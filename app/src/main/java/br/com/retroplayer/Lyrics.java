package br.com.retroplayer;

import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Independent LRC reader and LRCLIB client. All network work runs off the UI thread. */
final class Lyrics {
  static final class Line {
    final long time;
    final String text;
    Line(long time, String text) { this.time = time; this.text = text; }
  }
  final List<Line> lines;
  final String plain;
  Lyrics(String source) {
    ArrayList<Line> parsed = new ArrayList<>();
    Pattern tag = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\]");
    Matcher offsetTag = Pattern.compile("\\[offset:\\s*([+-]?\\d+)\\]", Pattern.CASE_INSENSITIVE).matcher(source);
    long offset = 0;
    if (offsetTag.find()) {
      try { offset = Math.max(-3600000, Math.min(3600000, Long.parseLong(offsetTag.group(1)))); }
      catch (NumberFormatException ignored) {}
    }
    for (String row : source.split("\\r?\\n")) {
      Matcher m = tag.matcher(row.trim());
      ArrayList<Long> times = new ArrayList<>();
      int end = 0;
      while (m.find() && m.start() == end) {
        String f = m.group(3);
        long fraction = f == null ? 0 : Long.parseLong((f + "000").substring(0, 3));
        times.add(Math.max(0, Long.parseLong(m.group(1)) * 60000 + Long.parseLong(m.group(2)) * 1000 + fraction - offset));
        end = m.end();
      }
      String body = row.trim().substring(end).replaceAll("<\\d{1,3}:\\d{1,2}(?:[.:]\\d{1,3})?>", "").trim();
      for (long t : times) parsed.add(new Line(t, body));
    }
    parsed.sort(Comparator.comparingLong(l -> l.time));
    lines = Collections.unmodifiableList(parsed);
    plain = parsed.isEmpty() ? source.trim() : "";
  }
  int at(long position) {
    int lo = 0, hi = lines.size() - 1, found = -1;
    while (lo <= hi) { int mid = (lo + hi) >>> 1; if (lines.get(mid).time <= position) { found = mid; lo = mid + 1; } else hi = mid - 1; }
    return found;
  }
  static String clean(String title) {
    return title.replaceFirst("(?i)\\s*[-|]\\s*YouTube(?: Music)?$", "")
        .replaceFirst("(?i)\\.(mp3|flac|m4a|ogg|opus|wav|mp4|webm)$", "")
        .replaceAll("(?i)\\s*[\\[(][^\\])]*(official|lyrics?|audio|video)[^\\])]*[\\])]", "").trim();
  }
  static String fetch(String title, String artist, double duration) throws Exception {
    String url = "https://lrclib.net/api/get?track_name=" + URLEncoder.encode(title, "UTF-8")
        + "&artist_name=" + URLEncoder.encode(artist, "UTF-8") + "&duration=" + Math.round(duration);
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    c.setConnectTimeout(10000); c.setReadTimeout(10000);
    c.setRequestProperty("User-Agent", "RetroPlayer/1.3 (Android music player)");
    try {
      if (c.getResponseCode() == 404) return "";
      if (c.getResponseCode() != 200) throw new IOException("LRCLIB HTTP " + c.getResponseCode());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try (InputStream in = c.getInputStream()) {
        byte[] b = new byte[4096]; int n;
        while ((n = in.read(b)) != -1) { if (out.size() + n > 262144) throw new IOException("Letra muito grande"); out.write(b, 0, n); }
      }
      JSONObject j = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
      if (j.optBoolean("instrumental")) return "♪ Instrumental";
      String synced = j.optString("syncedLyrics", "");
      return synced.isEmpty() || synced.equals("null") ? j.optString("plainLyrics", "").replaceFirst("^null$", "") : synced;
    } finally { c.disconnect(); }
  }
}
