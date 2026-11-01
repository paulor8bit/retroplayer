package br.com.retroplayer;
import java.util.*;
import org.json.*;
final class BookmarkStore {
  static final class Bookmark {
    final String title, url;
    Bookmark(String t, String u) {
      title = t;
      url = u;
    }
  }
  static List<Bookmark> read(String raw) {
    List<Bookmark> result = new ArrayList<>();
    try {
      JSONArray a = new JSONArray(raw);
      Set<String> seen = new HashSet<>();
      for (int i = 0; i < a.length(); i++) {
        JSONObject j = a.optJSONObject(i);
        if (j == null)
          continue;
        String url = j.optString("url");
        if (valid(url) && seen.add(url))
          result.add(new Bookmark(j.optString("title", url), url));
      }
    } catch (Exception ignored) {
    }
    return result;
  }
  static boolean valid(String url) {
    try {
      java.net.URI u = new java.net.URI(url);
      return ("https".equals(u.getScheme()) || "http".equals(u.getScheme()))
          && u.getHost() != null && u.getUserInfo() == null;
    } catch (Exception e) {
      return false;
    }
  }
  static String write(List<Bookmark> list) {
    JSONArray a = new JSONArray();
    for (Bookmark b : list) try {
        a.put(new JSONObject().put("title", b.title).put("url", b.url));
      } catch (Exception ignored) {
      }
    return a.toString();
  }
  static void save(List<Bookmark> list, String title, String url) {
    if (!valid(url))
      return;
    list.removeIf(b -> b.url.equals(url));
    list.add(0, new Bookmark(title == null || title.trim().isEmpty() ? url : title, url));
  }
}
