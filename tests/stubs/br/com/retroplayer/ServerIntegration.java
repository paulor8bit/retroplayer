package br.com.retroplayer;
import java.net.*;
import java.net.http.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class ServerIntegration {
  static int checks = 0;
  static void check(boolean b, String m) {
    checks++;
    if (!b)
      throw new AssertionError(m);
  }
  static HttpClient client = HttpClient.newBuilder().build();
  static HttpResponse<byte[]> get(String base, String path, String range) throws Exception {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
    if (range != null)
      b.header("Range", range);
    return client.send(b.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
  }
  public static void main(String[] args) throws Exception {
    File fixture = new File("tests/fixture.bin");
    byte[] data = new byte[100000];
    new Random(42).nextBytes(data);
    Files.write(fixture.toPath(), data);
    PlayerService s = new PlayerService(fixture, new File("app/src/main/assets"));
    RoomServer server = new RoomServer(s);
    server.start();
    String base = "http://127.0.0.1:" + server.port + "/", room = "secret-session/";
    try {
      check(get(base, "wrong/state", null).statusCode() == 404, "secret token required");
      check(get(base, room + "state", null).statusCode() == 200, "state accessible");
      check(new String(get(base, room, null).body()).contains("ROOM CHAT"), "guest page served");
      HttpResponse<byte[]> full = get(base, room + "media/1", null);
      check(Arrays.equals(full.body(), data), "full media bytes");
      HttpResponse<byte[]> part = get(base, room + "media/1", "bytes=10-39");
      check(
          part.statusCode() == 206 && Arrays.equals(part.body(), Arrays.copyOfRange(data, 10, 40)),
          "byte range");
      check(part.headers().firstValue("Content-Range").orElse("").equals("bytes 10-39/100000"),
          "range header");
      check(Arrays.equals(get(base, room + "media/1", "bytes=-20").body(),
                Arrays.copyOfRange(data, data.length - 20, data.length)),
          "suffix range");
      check(
          get(base, room + "media/1", "bytes=100000-").statusCode() == 416, "reject invalid range");
      check(get(base, room + "media/0", null).statusCode() == 404, "stale track rejected");
      HttpResponse<byte[]> post =
          client.send(HttpRequest.newBuilder(URI.create(base + room + "chat"))
                          .header("Content-Type", "application/json")
                          .POST(HttpRequest.BodyPublishers.ofString(
                              "{\"name\":\"Amigo\",\"text\":\"Olá <script>!\"}"))
                          .build(),
              HttpResponse.BodyHandlers.ofByteArray());
      check(post.statusCode() == 200, "chat post");
      check(
          new String(get(base, room + "chat", null).body(), java.nio.charset.StandardCharsets.UTF_8)
              .contains("Olá"),
          "chat received");
      HttpResponse<byte[]> csrf =
          client.send(HttpRequest.newBuilder(URI.create(base + room + "chat"))
                          .header("Content-Type", "application/json")
                          .header("Origin", "https://other.example")
                          .POST(HttpRequest.BodyPublishers.ofString("{}"))
                          .build(),
              HttpResponse.BodyHandlers.ofByteArray());
      check(csrf.statusCode() == 403, "cross-origin post rejected");
      s.tracks.set(0,
          new PlayerService.Track(new PlayerService.MediaUri("https://example.com/music.mp3"),
              "remote", "audio/mpeg", -1));
      HttpResponse<byte[]> redirect = get(base, room + "media/1", null);
      check(redirect.statusCode() == 302
              && redirect.headers()
                  .firstValue("Location")
                  .orElse("")
                  .equals("https://example.com/music.mp3"),
          "remote media redirect");
      s.room = false;
      check(get(base, room + "state", null).statusCode() == 404, "closed room revoked");
      System.out.println("PASS: " + checks + " HTTP, media, chat and access checks");
    } finally {
      server.close();
      fixture.delete();
    }
  }
}
