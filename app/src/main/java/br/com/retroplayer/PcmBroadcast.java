package br.com.retroplayer;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded, nonblocking live fan-out. Slow listeners are disconnected rather than delaying capture. */
final class PcmBroadcast implements AutoCloseable {
  static final int SAMPLE_RATE = 44100, CHANNELS = 2, MAX_LISTENERS = 4;
  final String id = UUID.randomUUID().toString();
  private final Set<Listener> listeners = new HashSet<>();
  volatile boolean running = true;
  final class Listener implements AutoCloseable {
    final ArrayBlockingQueue<byte[]> chunks = new ArrayBlockingQueue<>(24);
    final AtomicBoolean open = new AtomicBoolean(true);
    final Runnable disconnected;
    Listener(Runnable disconnected) { this.disconnected = disconnected; }
    public void close() {
      if (!open.compareAndSet(true, false)) return;
      synchronized (PcmBroadcast.this) { listeners.remove(this); }
      chunks.clear();
      chunks.offer(new byte[0]);
      disconnected.run();
    }
    byte[] next() throws InterruptedException { return chunks.poll(12, TimeUnit.SECONDS); }
  }
  synchronized Listener subscribe(Runnable disconnected) {
    if (!running || listeners.size() >= MAX_LISTENERS) return null;
    Listener listener = new Listener(disconnected);
    listeners.add(listener);
    return listener;
  }
  void publish(byte[] bytes) {
    List<Listener> copy;
    synchronized (this) { if (!running) return; copy = new ArrayList<>(listeners); }
    for (Listener l : copy) if (l.open.get() && !l.chunks.offer(bytes)) l.close();
  }
  public void close() {
    List<Listener> copy;
    synchronized (this) { running = false; copy = new ArrayList<>(listeners); }
    for (Listener l : copy) l.close();
  }
}
