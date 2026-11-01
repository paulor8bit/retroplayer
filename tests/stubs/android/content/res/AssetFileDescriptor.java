package android.content.res;
import java.io.*;
public class AssetFileDescriptor implements AutoCloseable {
  private final File f;
  public AssetFileDescriptor(File file) {
    f = file;
  }
  public long getLength() {
    return f.length();
  }
  public InputStream createInputStream() throws IOException {
    return new FileInputStream(f);
  }
  public void close() {}
}
