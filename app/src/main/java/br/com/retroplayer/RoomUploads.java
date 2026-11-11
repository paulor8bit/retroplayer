package br.com.retroplayer;
import java.io.*;
import java.util.*;

/** Streaming upload reservations with per-file and total disk bounds. */
final class RoomUploads {
  static final long MAX_FILE = 100L * 1024 * 1024, MAX_TOTAL = 300L * 1024 * 1024;
  final File directory;
  long reserved;
  int active;
  RoomUploads(File directory) {
    this.directory = directory; directory.mkdirs();
    File[] stale = directory.listFiles();
    if(stale!=null)for(File file:stale)if(file.getName().endsWith(".part"))file.delete();
  }
  static String name(String value) {
    String n = value.replace('\\', '/');
    n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
    if (n.isEmpty() || n.length() > 160 || !n.toLowerCase(Locale.ROOT).matches(".*\\.(mp3|m4a|aac|flac|ogg|opus|wav|mp4|m4v|webm|mkv|3gp)$")) return "";
    return n;
  }
  synchronized File reserve(long size, String name) throws IOException {
    if (size < 1 || size > MAX_FILE || active >= 2) throw new IOException("Até 100 MB por arquivo; aguarde os envios em andamento.");
    long used = 0;
    File[] files = directory.listFiles();
    if (files != null) for (File f : files) used += f.length();
    if (used + reserved + size > MAX_TOTAL || directory.getUsableSpace() < size + 32L * 1024 * 1024)
      throw new IOException("Espaço para arquivos da sala esgotado. Peça ao anfitrião para remover faixas.");
    if (name(name).isEmpty()) throw new IOException("Escolha um arquivo de áudio ou vídeo compatível.");
    reserved += size; active++;
    return new File(directory, UUID.randomUUID() + name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT) + ".part");
  }
  synchronized void release(long size) { reserved -= size; active--; }
  boolean owns(File file) { return directory.equals(file.getParentFile()); }
}
