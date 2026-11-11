package br.com.retroplayer;
import java.net.*;
final class RoomLink {
  static String parse(String raw) {
    try {
      if(raw==null||raw.length()>4096)return "";
      URI uri = new URI(raw.trim());
      if ("retroplayer".equals(uri.getScheme()) && "room".equals(uri.getHost())) {
        for (String p : uri.getRawQuery().split("&")) if (p.startsWith("url=")) return parse(URLDecoder.decode(p.substring(4), "UTF-8"));
        return "";
      }
      if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
          || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getRawQuery() != null || uri.getRawFragment() != null
          || !uri.getPath().matches("/[a-fA-F0-9]{32}/?")) return "";
      return "https://" + uri.getRawAuthority() + uri.getPath().replaceFirst("/?$", "/");
    } catch (Exception ignored) { return ""; }
  }
  static String media(String value) {
    try {
      URI u = new URI(value.trim());
      if (!"https".equalsIgnoreCase(u.getScheme()) || u.getHost() == null || u.getUserInfo() != null || (u.getPort() != -1 && u.getPort() != 443)) return "";
      String host = u.getHost().toLowerCase(java.util.Locale.ROOT);
      if (host.equals("localhost") || host.endsWith(".local") || host.startsWith("127.") || host.startsWith("10.") || host.startsWith("192.168.") || host.contains(":")) return "";
      if (host.matches("172\\.(1[6-9]|2[0-9]|3[01])\\..*") || host.startsWith("169.254.") || host.startsWith("0.")) return "";
      if (!YouTubeLink.id(value).isEmpty()) return "https://music.youtube.com/watch?v=" + YouTubeLink.id(value);
      return MediaFiles.isMedia(u.getPath(), null) ? u.toASCIIString() : "";
    } catch (Exception ignored) { return ""; }
  }
}
