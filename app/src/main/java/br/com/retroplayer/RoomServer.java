package br.com.retroplayer;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import android.content.res.AssetFileDescriptor;
import org.json.*;

public class RoomServer {
  final PlayerService s;
  final ServerSocket listener;
  public final int port;
  volatile boolean running = true;
  final ThreadPoolExecutor pool =
      new ThreadPoolExecutor(12, 12, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(24));
  long chatWindow = 0;
  int chatCount = 0;
  RoomServer(PlayerService service) throws IOException {
    s = service;
    listener = new ServerSocket(0, 32, InetAddress.getByName("127.0.0.1"));
    port = listener.getLocalPort();
  }
  void start() {
    new Thread(() -> {
      while (running) try {
          Socket c = listener.accept();
          try {
            pool.execute(() -> handle(c));
          } catch (RejectedExecutionException e) {
            c.close();
          }
        } catch (IOException e) {
          if (!running)
            break;
        }
    }, "room-http").start();
  }
  void close() {
    running = false;
    try {
      listener.close();
    } catch (Exception ignored) {
    }
    pool.shutdownNow();
  }
  String line(InputStream in) throws IOException {
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    for (int i = 0; i < 8192; i++) {
      int v = in.read();
      if (v == -1)
        return b.size() == 0 ? null : b.toString("UTF-8");
      if (v == 10)
        return b.toString("UTF-8").replace("\r", "");
      b.write(v);
    }
    throw new IOException("Header too large");
  }
  void handle(Socket sock) {
    try (sock) {
      sock.setSoTimeout(15000);
      InputStream in = new BufferedInputStream(sock.getInputStream());
      OutputStream out = new BufferedOutputStream(sock.getOutputStream());
      String first = line(in);
      if (first == null)
        return;
      String[] req = first.split(" ");
      if (req.length < 3)
        return;
      Map<String, String> headers = new HashMap<>();
      String l;
      int total = 0;
      while ((l = line(in)) != null && !l.isEmpty()) {
        total += l.length();
        if (total > 16384)
          return;
        int a = l.indexOf(':');
        if (a > 0)
          headers.put(l.substring(0, a).toLowerCase(Locale.ROOT), l.substring(a + 1).trim());
      }
      String path = req[1].split("\\?", 2)[0], prefix = "/" + s.token + "/";
      if (!s.room || !path.startsWith(prefix)) {
        reply(out, 404, "text/plain", "Sala encerrada ou link inválido.");
        return;
      }
      String endpoint = path.substring(prefix.length());
      if (!req[0].equals("GET") && !req[0].equals("HEAD") && !req[0].equals("POST")) {
        reply(out, 405, "text/plain", "Método inválido");
        return;
      }
      if (req[0].equals("POST")) {
        if (!endpoint.equals("chat")
            || !headers.getOrDefault("content-type", "").startsWith("application/json")) {
          reply(out, 400, "text/plain", "JSON necessário");
          return;
        }
        String origin = headers.get("origin");
        if (origin != null
            && !Objects.equals(URI.create(origin).getRawAuthority(), headers.get("host"))) {
          reply(out, 403, "text/plain", "Origem inválida");
          return;
        }
        int size = Integer.parseInt(headers.getOrDefault("content-length", "0"));
        if (size < 1 || size > 8192) {
          reply(out, 413, "text/plain", "Mensagem grande");
          return;
        }
        synchronized (this) {
          long now = System.currentTimeMillis();
          if (now - chatWindow > 1000) {
            chatWindow = now;
            chatCount = 0;
          }
          if (++chatCount > 10) {
            reply(out, 429, "text/plain", "Aguarde um instante");
            return;
          }
        }
        byte[] data = Io.bytes(in, size);
        if (data.length != size)
          return;
        JSONObject j = new JSONObject(new String(data, StandardCharsets.UTF_8));
        s.chat(j.optString("name", "Visitante"), j.optString("text", ""));
        reply(out, 200, "application/json", "{\"ok\":true}");
        return;
      }
      if (endpoint.equals("state")) {
        reply(out, 200, "application/json", s.state);
        return;
      }
      if (endpoint.equals("chat")) {
        reply(out, 200, "application/json", s.chatJson());
        return;
      }
      if (endpoint.equals("")) {
        try (InputStream page = s.getAssets().open("guest.html")) {
          byte[] b = Io.all(page);
          head(out, 200, "text/html; charset=utf-8", b.length, "");
          if (!req[0].equals("HEAD"))
            out.write(b);
          out.flush();
        }
        return;
      }
      if (endpoint.startsWith("media/")) {
        String id = endpoint.substring(6);
        if (!id.equals(String.valueOf(s.generation)) || s.index < 0) {
          reply(out, 404, "text/plain", "Faixa mudou");
          return;
        }
        PlayerService.Track t = s.tracks.get(s.index);
        if ("https".equals(t.uri.getScheme())) {
          head(out, 302, "text/plain", 0,
              "Location: " + t.uri.toString().replace("\r", "").replace("\n", "") + "\r\n");
          out.flush();
          return;
        }
        try (AssetFileDescriptor fd = s.getContentResolver().openAssetFileDescriptor(t.uri, "r")) {
          if (fd == null)
            throw new IOException();
          long length = fd.getLength();
          if (length < 0)
            length = t.size;
          long start = 0, end = length - 1;
          String range = headers.get("range");
          int status = 200;
          if (range != null && length > 0) {
            if (!range.matches("bytes=[0-9]*-[0-9]*")) {
              head(out, 416, "text/plain", 0, "Content-Range: bytes */" + length + "\r\n");
              out.flush();
              return;
            }
            String[] p = range.substring(6).split("-", -1);
            if (p[0].isEmpty()) {
              long suffix = Long.parseLong(p[1]);
              start = Math.max(0, length - suffix);
            } else {
              start = Long.parseLong(p[0]);
              if (!p[1].isEmpty())
                end = Math.min(end, Long.parseLong(p[1]));
            }
            if (start >= length || start > end) {
              head(out, 416, "text/plain", 0, "Content-Range: bytes */" + length + "\r\n");
              out.flush();
              return;
            }
            status = 206;
          }
          long count = length >= 0 ? end - start + 1 : -1;
          head(out, status, t.mime, count,
              "Accept-Ranges: bytes\r\n"
                  + (status == 206
                          ? "Content-Range: bytes " + start + "-" + end + "/" + length + "\r\n"
                          : ""));
          if (!req[0].equals("HEAD"))
            try (InputStream media = fd.createInputStream()) {
              Io.skip(media, start);
              byte[] buffer = new byte[65536];
              long remaining = count;
              while (running && s.room && prefix.equals("/" + s.token + "/")
                  && (remaining > 0 || count < 0)) {
                int n = media.read(buffer, 0,
                    (int) (count < 0 ? buffer.length : Math.min(buffer.length, remaining)));
                if (n < 0)
                  break;
                out.write(buffer, 0, n);
                if (count >= 0)
                  remaining -= n;
              }
            }
          out.flush();
        }
        return;
      }
      reply(out, 404, "text/plain", "Não encontrado");
    } catch (Exception ignored) {
    }
  }
  void head(OutputStream out, int status, String mime, long size, String extra) throws IOException {
    String statusText = status == 200 ? "OK"
        : status == 206               ? "Partial Content"
        : status == 404               ? "Not Found"
        : status == 416               ? "Range Not Satisfiable"
                                      : "Error";
    String h = "HTTP/1.1 " + status + " " + statusText + "\r\nContent-Type: " + mime + "\r\n"
        + (size >= 0 ? "Content-Length: " + size + "\r\n" : "")
        + ("Connection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: "
           + "nosniff\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src "
           + "'self'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; media-src 'self' "
           + "https:; connect-src 'self'; frame-ancestors 'none'\r\n")
        + extra + "\r\n";
    out.write(h.getBytes(StandardCharsets.UTF_8));
  }
  void reply(OutputStream out, int status, String type, String body) throws IOException {
    byte[] b = body.getBytes(StandardCharsets.UTF_8);
    head(out, status, type + "; charset=utf-8", b.length, "");
    out.write(b);
    out.flush();
  }
}
