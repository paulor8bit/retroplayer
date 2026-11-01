package br.com.retroplayer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class LibraryIntegration {
  static int checks;
  static void check(boolean v, String label) {
    checks++;
    if (!v)
      throw new AssertionError(label);
  }
  static MediaFiles.Entry entry(String id, String name, String mime, boolean directory) {
    return new MediaFiles.Entry(id, "content://tree/" + id, name, mime, 10, directory);
  }
  public static void main(String[] args) {
    Map<String, List<MediaFiles.Entry>> tree = new HashMap<>();
    tree.put("root",
        Arrays.asList(entry("a", "Zeta.MP3", "application/octet-stream", false),
            entry("folder", "Album", "vnd.android.document/directory", true),
            entry("pic", "foto.jpg", "image/jpeg", false),
            entry("radio", "Radio", "audio/aac", false), entry("locked", "Sem acesso", "", true)));
    tree.put("folder",
        Arrays.asList(entry("v", "Video.mp4", "video/mp4", false),
            entry("a", "Zeta.MP3", "audio/mpeg", false), entry("root", "Loop", "", true),
            entry("notes", "texto.txt", "text/plain", false)));
    MediaFiles.Result r = MediaFiles.scan(id -> {
      if (id.equals("locked"))
        throw new SecurityException();
      return new ArrayList<>(tree.get(id));
    }, "root", new AtomicBoolean(), null);
    check(r.files.size() == 3, "include media in subfolders, exclude images and deduplicate");
    check(r.folders == 3, "cycle does not cause infinite traversal");
    check(r.unreadable == 1, "report inaccessible subfolder without losing playable files");
    check(!r.cancelled, "normal scan completes");
    AtomicBoolean cancel = new AtomicBoolean();
    MediaFiles.Result stopped = MediaFiles.scan(
        id -> new ArrayList<>(tree.get(id)), "root", cancel, (files, folders) -> cancel.set(true));
    check(stopped.cancelled && stopped.folders == 1, "folder import can be cancelled");
    check(MediaFiles.isMedia("Faixa.FLAC", null), "extension fallback is case independent");
    check(MediaFiles.isMedia("stream", "audio/ogg"), "extensionless audio by MIME");
    check(!MediaFiles.isMedia("document.pdf", "application/pdf"), "reject documents");
    List<BookmarkStore.Bookmark> list = new ArrayList<>();
    BookmarkStore.save(list, "Rádio", "https://example.com/radio?a=1&b=2");
    BookmarkStore.save(list, "Vídeos", "https://video.example/");
    BookmarkStore.save(list, "Rádio atualizada", "https://example.com/radio?a=1&b=2");
    check(list.size() == 2 && list.get(0).title.equals("Rádio atualizada"),
        "saving existing favorite updates it without duplicates");
    List<BookmarkStore.Bookmark> read = BookmarkStore.read(BookmarkStore.write(list));
    check(read.size() == 2 && read.get(0).url.equals(list.get(0).url),
        "bookmarks survive serialization preserving query strings and names");
    BookmarkStore.save(list, "Inseguro", "javascript:alert(1)");
    check(list.size() == 2, "non-web scheme cannot be favorited");
    check(BookmarkStore.read("invalid JSON").isEmpty(), "corrupt favorites recover gracefully");
    check(!BookmarkStore.valid("https://user:secret@example.com"),
        "avoid saving credentials in favorites");
    List<BookmarkStore.Bookmark> bad =
        BookmarkStore.read("[{\"title\":\"Site\",\"url\":\"https://example.com\"},{\"url\":\"file:/"
                           + "//private\"},{\"url\":\"https://example.com\"}]");
    check(bad.size() == 1, "corrupt and duplicate bookmark entries are filtered");
    System.out.println("PASS: " + checks + " folder import, cancellation and favorites checks");
  }
}
