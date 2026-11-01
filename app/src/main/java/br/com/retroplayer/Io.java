package br.com.retroplayer;
import java.io.*;
final class Io {
  static byte[] all(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    copy(in, out);
    return out.toByteArray();
  }
  static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] b = new byte[32768];
    int n;
    while ((n = in.read(b)) != -1) out.write(b, 0, n);
  }
  static byte[] bytes(InputStream in, int size) throws IOException {
    byte[] b = new byte[size];
    int pos = 0;
    while (pos < size) {
      int n = in.read(b, pos, size - pos);
      if (n < 0)
        throw new EOFException();
      pos += n;
    }
    return b;
  }
  static void skip(InputStream in, long n) throws IOException {
    while (n > 0) {
      long k = in.skip(n);
      if (k == 0) {
        if (in.read() == -1)
          throw new EOFException();
        k = 1;
      }
      n -= k;
    }
  }
}
