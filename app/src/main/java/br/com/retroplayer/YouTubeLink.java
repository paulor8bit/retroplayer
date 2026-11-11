package br.com.retroplayer;

import java.net.*;
/** Only canonical YouTube hosts and eleven-character video IDs are accepted. */
final class YouTubeLink {
  static String id(String value) {
    try {
      URI u = new URI(value);
      if (!"https".equalsIgnoreCase(u.getScheme()) || u.getUserInfo() != null) return "";
      String host = u.getHost(), path = u.getPath(), id = "";
      if (host == null) return "";
      host = host.toLowerCase(java.util.Locale.ROOT);
      if (host.equals("youtu.be")) id = path.substring(1).split("/", 2)[0];
      else if (host.equals("youtube.com") || host.equals("www.youtube.com") || host.equals("m.youtube.com") || host.equals("music.youtube.com")) {
        if (path.equals("/watch") && u.getRawQuery() != null) {
          for (String p : u.getRawQuery().split("&")) if (p.startsWith("v=")) id = URLDecoder.decode(p.substring(2), "UTF-8");
        } else if (path.startsWith("/shorts/") || path.startsWith("/embed/") || path.startsWith("/live/")) id = path.substring(path.indexOf('/', 1) + 1).split("/", 2)[0];
      }
      return id.matches("[A-Za-z0-9_-]{11}") ? id : "";
    } catch (Exception ignored) { return ""; }
  }
}
