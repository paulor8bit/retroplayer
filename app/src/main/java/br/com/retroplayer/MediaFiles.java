package br.com.retroplayer;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Provider-independent, cancellable traversal used by the Android folder picker. */
final class MediaFiles {
  static final class Entry {
    final String id, uri, name, mime;
    final long size;
    final boolean directory;
    Entry(String id, String uri, String name, String mime, long size, boolean directory) {
      this.id = id;
      this.uri = uri;
      this.name = name;
      this.mime = mime;
      this.size = size;
      this.directory = directory;
    }
  }
  interface Tree {
    List<Entry> children(String id) throws Exception;
  }
  interface Progress {
    void update(int files, int folders);
  }
  static final class Result {
    final List<Entry> files = new ArrayList<>();
    int folders, unreadable;
    boolean cancelled;
  }
  static boolean isMedia(String name, String mime) {
    if (mime != null && (mime.startsWith("audio/") || mime.startsWith("video/")))
      return true;
    return name != null
        && name.toLowerCase(Locale.ROOT)
               .matches(
                   ".*\\.(mp3|mp4|m4a|m4v|aac|ogg|oga|opus|wav|flac|webm|mkv|3gp|3gpp|ts|m3u8)$");
  }
  static Result scan(Tree tree, String root, AtomicBoolean cancel, Progress progress) {
    Result r = new Result();
    Set<String> visited = new HashSet<>();
    Set<String> uris = new HashSet<>();
    Deque<String> pending = new ArrayDeque<>();
    pending.add(root);
    while (!pending.isEmpty() && !cancel.get()) {
      String id = pending.removeFirst();
      if (!visited.add(id))
        continue;
      r.folders++;
      try {
        List<Entry> entries = tree.children(id);
        entries.sort(Comparator.comparing(
            (Entry e) -> e.name == null ? "" : e.name, String.CASE_INSENSITIVE_ORDER));
        for (Entry e : entries) {
          if (cancel.get())
            break;
          if (e.directory)
            pending.addLast(e.id);
          else if (isMedia(e.name, e.mime) && uris.add(e.uri))
            r.files.add(e);
        }
      } catch (Exception e) {
        r.unreadable++;
      }
      if (progress != null)
        progress.update(r.files.size(), r.folders);
    }
    r.cancelled = cancel.get();
    return r;
  }
}
