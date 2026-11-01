package br.com.retroplayer;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class FolderImporter {
  static MediaFiles.Result scan(
      ContentResolver resolver, Uri tree, AtomicBoolean cancel, MediaFiles.Progress progress) {
    return MediaFiles.scan(id -> {
      List<MediaFiles.Entry> rows = new ArrayList<>();
      Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id);
      String[] columns = {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
          DocumentsContract.Document.COLUMN_DISPLAY_NAME,
          DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE};
      try (Cursor c = resolver.query(children, columns, null, null, null)) {
        if (c == null)
          throw new IllegalStateException("Pasta indisponível");
        while (!cancel.get() && c.moveToNext()) {
          String child = c.getString(0), name = c.getString(1), mime = c.getString(2);
          long size = c.isNull(3) ? -1 : c.getLong(3);
          rows.add(new MediaFiles.Entry(child,
              DocumentsContract.buildDocumentUriUsingTree(tree, child).toString(),
              name == null ? "Arquivo" : name, mime, size,
              DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)));
        }
      }
      return rows;
    }, DocumentsContract.getTreeDocumentId(tree), cancel, progress);
  }
}
