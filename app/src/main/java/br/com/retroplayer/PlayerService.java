package br.com.retroplayer;

import android.app.*;
import android.media.session.*;
import android.content.*;
import android.os.*;
import android.media.*;
import android.net.Uri;
import android.view.Surface;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class PlayerService extends Service {
  public static PlayerService instance;
  public final ArrayList<Track> tracks = new ArrayList<>();
  public final ArrayList<JSONObject> messages = new ArrayList<>();
  public volatile String state = "{}", url = "", tunnelStatus = "Sala fechada", error = "";
  public volatile boolean room = false, ready = false;
  public volatile int index = -1;
  public volatile String token = UUID.randomUUID().toString().replace("-", "");
  android.media.audiofx.Equalizer eq;
  AudioFocusRequest focus;
  boolean resumeOnFocus = false;
  public void ensureEq() {
    if (eq == null) {
      eq = new android.media.audiofx.Equalizer(0, player.getAudioSessionId());
      eq.setEnabled(true);
    }
  }
  MediaPlayer player;
  RoomServer server;
  java.lang.Process tunnel;
  final Handler main = new Handler(Looper.getMainLooper());
  android.os.PowerManager.WakeLock wake;
  MediaSession session;
  Surface surface;
  long generation = 0;
  boolean repeat = false, shuffle = false;
  int chatId = 0;
  int tunnelGeneration = 0;
  public static class Track {
    public Uri uri;
    public String name, mime;
    public long size;
    Track(Uri u, String n, String m, long z) {
      uri = u;
      name = n;
      mime = m;
      size = z;
    }
  }
  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(
        new NotificationChannel("player", "Reprodução e sala", NotificationManager.IMPORTANCE_LOW));
    session = new MediaSession(this, "RetroPlayer");
    session.setCallback(new MediaSession.Callback() {
      public void onPlay() {
        play();
      }
      public void onPause() {
        pause();
      }
      public void onSkipToNext() {
        next(1);
      }
      public void onSkipToPrevious() {
        next(-1);
      }
      public void onSeekTo(long p) {
        seek((int) p);
      }
    });
    session.setFlags(
        MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
    session.setActive(true);
    startForeground(1, notification());
    wake = ((PowerManager) getSystemService(POWER_SERVICE))
               .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "retroplayer:room");
    try {
      server = new RoomServer(this);
      server.start();
    } catch (Exception e) {
      error = e.toString();
    }
    main.post(tick);
  }
  Notification notification() {
    Intent open = new Intent(this, MainActivity.class);
    PendingIntent pi = PendingIntent.getActivity(
        this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    Notification.Builder n =
        new Notification.Builder(this, "player")
            .setSmallIcon(br.com.retroplayer.R.drawable.icon)
            .setContentTitle(index >= 0 ? tracks.get(index).name : "RetroPlayer • Together")
            .setContentText(room ? tunnelStatus : "Música, vídeo e boas companhias")
            .setContentIntent(pi)
            .setOngoing(true)
            .setStyle(new Notification.MediaStyle()
                    .setMediaSession(session.getSessionToken())
                    .setShowActionsInCompactView(0, 1));
    for (String a : new String[] {"toggle", "next", "stop"}) {
      PendingIntent p = PendingIntent.getService(this, a.hashCode(),
          new Intent(this, PlayerService.class).setAction(a), PendingIntent.FLAG_IMMUTABLE);
      n.addAction(a.equals("toggle") ? android.R.drawable.ic_media_play
              : a.equals("next")     ? android.R.drawable.ic_media_next
                                     : android.R.drawable.ic_menu_close_clear_cancel,
          a.equals("toggle")     ? "Play / pausa"
              : a.equals("next") ? "Próxima"
                                 : "Encerrar",
          p);
    }
    return n.build();
  }
  void notifyNow() {
    getSystemService(NotificationManager.class).notify(1, notification());
  }
  public int onStartCommand(Intent i, int f, int id) {
    if (i != null) {
      String a = i.getAction();
      if ("toggle".equals(a))
        toggle();
      if ("next".equals(a))
        next(1);
      if ("stop".equals(a)) {
        stopSelf();
      }
    }
    return START_NOT_STICKY;
  }
  public IBinder onBind(Intent i) {
    return null;
  }
  Runnable tick = new Runnable() {
    public void run() {
      snapshot();
      main.postDelayed(this, 250);
    }
  };
  void snapshot() {
    try {
      boolean playing = ready && player != null && player.isPlaying();
      int pos = ready ? player.getCurrentPosition() : 0;
      int duration = ready ? player.getDuration() : 0;
      JSONObject s =
          new JSONObject()
              .put("title", index >= 0 ? tracks.get(index).name : "Selecione sua música ou vídeo")
              .put("id", generation)
              .put("playing", playing)
              .put("position", pos / 1000.0)
              .put("duration", Math.max(0, duration) / 1000.0)
              .put("serverTime", System.currentTimeMillis())
              .put("hasMedia", index >= 0 && ready)
              .put("mime", index >= 0 ? tracks.get(index).mime : "")
              .put("room", room);
      state = s.toString();
      session.setPlaybackState(new PlaybackState.Builder()
              .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                  | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT
                  | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO)
              .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, pos,
                  playing ? 1 : 0)
              .build());
    } catch (Exception ignored) {
    }
  }
  public void add(Uri uri) {
    if ("https".equals(uri.getScheme())) {
      addRemote(uri.toString());
      return;
    }
    try {
      String name = "Mídia";
      long size = -1;
      try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
        if (c != null && c.moveToFirst()) {
          int ni = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME),
              si = c.getColumnIndex(android.provider.OpenableColumns.SIZE);
          if (ni >= 0)
            name = c.getString(ni);
          if (si >= 0 && !c.isNull(si))
            size = c.getLong(si);
        }
      }
      String mime = getContentResolver().getType(uri);
      if (mime == null)
        mime = name.toLowerCase(Locale.ROOT).endsWith("mp4") ? "video/mp4" : "audio/mpeg";
      tracks.add(new Track(uri, name, mime, size));
      saveQueue();
      if (index < 0)
        select(0);
    } catch (Exception e) {
      error = "Não foi possível abrir o arquivo: " + e.getMessage();
    }
  }
  public void addRemote(String link) {
    try {
      Uri uri = Uri.parse(link);
      if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
        throw new IllegalArgumentException("Link inválido");
      String name = uri.getLastPathSegment();
      if (name == null || name.isEmpty())
        name = uri.getHost();
      String path = name.toLowerCase(Locale.ROOT);
      String mime = path.endsWith(".mp4") ? "video/mp4"
          : path.endsWith(".webm")        ? "video/webm"
          : path.endsWith(".m3u8")        ? "application/vnd.apple.mpegurl"
          : path.endsWith(".ogg")         ? "audio/ogg"
          : path.endsWith(".wav")         ? "audio/wav"
          : path.endsWith(".m4a")         ? "audio/mp4"
                                          : "audio/mpeg";
      tracks.add(new Track(uri, name, mime, -1));
      saveQueue();
      select(tracks.size() - 1);
    } catch (Exception e) {
      error = "Link não suportado: " + e.getMessage();
    }
  }
  void saveQueue() {
    try {
      JSONArray a = new JSONArray();
      for (Track t : tracks) a.put(t.uri.toString());
      getSharedPreferences("queue", 0).edit().putString("uris", a.toString()).apply();
    } catch (Exception ignored) {
    }
  }
  public void restore() {
    if (!tracks.isEmpty())
      return;
    try {
      JSONArray a = new JSONArray(getSharedPreferences("queue", 0).getString("uris", "[]"));
      for (int i = 0; i < a.length(); i++) add(Uri.parse(a.getString(i)));
    } catch (Exception ignored) {
    }
  }
  public void select(int n) {
    if (n < 0 || n >= tracks.size())
      return;
    ready = false;
    index = n;
    generation++;
    error = "";
    if (eq != null) {
      eq.release();
      eq = null;
    }
    if (player != null)
      player.release();
    player = new MediaPlayer();
    try {
      player.setAudioAttributes(new AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build());
      player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
      player.setDataSource(this, tracks.get(n).uri);
      if (surface != null)
        player.setSurface(surface);
      player.setOnPreparedListener(p -> {
        ready = true;
        play();
        notifyNow();
      });
      player.setOnCompletionListener(p -> {
        if (repeat) {
          seek(0);
          play();
        } else if (index < tracks.size() - 1 || shuffle)
          next(1);
        else {
          snapshot();
          notifyNow();
        }
      });
      player.setOnErrorListener((p, w, e) -> {
        ready = false;
        error = "Formato não suportado ou arquivo indisponível (" + w + ").";
        return true;
      });
      player.prepareAsync();
      notifyNow();
    } catch (Exception e) {
      error = e.getMessage();
    }
  }
  public void attach(Surface s) {
    surface = s;
    try {
      if (player != null)
        player.setSurface(s);
    } catch (Exception ignored) {
    }
  }
  public void toggle() {
    if (ready) {
      if (player.isPlaying())
        pause();
      else
        play();
    }
  }
  public void play() {
    if (ready) {
      AudioManager am = getSystemService(AudioManager.class);
      if (focus == null)
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                    .setOnAudioFocusChangeListener(change -> {
                      if (!ready)
                        return;
                      if (change == AudioManager.AUDIOFOCUS_LOSS) {
                        player.pause();
                        resumeOnFocus = false;
                      } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        resumeOnFocus = player.isPlaying();
                        player.pause();
                      } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
                        player.setVolume(.2f, .2f);
                      else if (change == AudioManager.AUDIOFOCUS_GAIN) {
                        player.setVolume(1, 1);
                        if (resumeOnFocus) {
                          player.start();
                          resumeOnFocus = false;
                        }
                      }
                      snapshot();
                    })
                    .build();
      if (am.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        player.start();
        snapshot();
        notifyNow();
      }
    }
  }
  public void pause() {
    if (ready) {
      resumeOnFocus = false;
      player.pause();
      snapshot();
      notifyNow();
    }
  }
  public void seek(int ms) {
    if (ready) {
      player.seekTo(Math.max(0, Math.min(ms, player.getDuration())), MediaPlayer.SEEK_CLOSEST);
      snapshot();
    }
  }
  public void next(int d) {
    if (!tracks.isEmpty())
      select(shuffle ? new Random().nextInt(tracks.size())
                     : (index + d + tracks.size()) % tracks.size());
  }
  public synchronized void chat(String name, String text) {
    text = text.trim();
    if (text.isEmpty())
      return;
    try {
      JSONObject j = new JSONObject()
                         .put("id", ++chatId)
                         .put("name", name.substring(0, Math.min(24, name.length())))
                         .put("text", text.substring(0, Math.min(1000, text.length())))
                         .put("time", System.currentTimeMillis());
      messages.add(j);
      while (messages.size() > 150) messages.remove(0);
    } catch (Exception ignored) {
    }
  }
  public synchronized String chatJson() {
    return new JSONArray(messages).toString();
  }
  public void openRoom() {
    if (room)
      return;
    if (server == null) {
      error = "Servidor local indisponível.";
      return;
    }
    room = true;
    url = "";
    token = UUID.randomUUID().toString().replace("-", "");
    tunnelStatus = "Conectando ao TryCloudflare…";
    int run = ++tunnelGeneration;
    if (!wake.isHeld())
      wake.acquire();
    notifyNow();
    new Thread(() -> {
      java.lang.Process proc = null;
      try {
        File dns = new File(getFilesDir(), "dns");
        dns.mkdirs();
        try (FileWriter w = new FileWriter(new File(dns, "resolv.cnf"))) {
          w.write("nameserver 1.1.1.1\nnameserver 8.8.8.8\n");
        }
        File ca = new File(getFilesDir(), "ca.pem");
        try (InputStream in = getAssets().open("ca.pem");
            OutputStream out = new FileOutputStream(ca)) {
          Io.copy(in, out);
        }
        ProcessBuilder b =
            new ProcessBuilder(getApplicationInfo().nativeLibraryDir + "/libcloudflared.so",
                "tunnel", "--no-autoupdate", "--url", "http://127.0.0.1:" + server.port,
                "--protocol", "http2", "--edge-ip-version", "4");
        b.directory(getFilesDir());
        b.redirectErrorStream(true);
        b.environment().put("SSL_CERT_FILE", ca.toString());
        b.environment().put("TUNNEL_DNS_RESOLVER_ADDRS", "1.1.1.1:53");
        b.environment().put("HOME", getFilesDir().toString());
        proc = b.start();
        synchronized (this) {
          if (!room || run != tunnelGeneration) {
            proc.destroy();
            return;
          }
          tunnel = proc;
        }
        Pattern pattern = Pattern.compile("https://[a-z0-9-]+\\.trycloudflare\\.com");
        boolean registered = false;
        try (BufferedReader reader =
                 new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            if (run != tunnelGeneration)
              break;
            Matcher m = pattern.matcher(line);
            if (m.find())
              url = m.group() + "/" + token + "/";
            if (line.contains("Registered tunnel connection")) {
              registered = true;
              tunnelStatus = "Sala online • link pronto";
              main.post(this::notifyNow);
            }
            if (line.contains("ERR") && !registered)
              tunnelStatus = "Conectando… verifique sua internet";
          }
        }
        if (run == tunnelGeneration && room) {
          tunnelStatus = "Túnel desconectado. Encerre e abra a sala novamente.";
          url = "";
          main.post(this::notifyNow);
        }
      } catch (Exception e) {
        if (run == tunnelGeneration) {
          tunnelStatus = "Falha no túnel: " + e.getMessage();
          url = "";
          main.post(this::notifyNow);
        }
      } finally {
        if (proc != null)
          proc.destroy();
      }
    }, "tunnel").start();
    main.postDelayed(() -> {
      if (run == tunnelGeneration && room && !tunnelStatus.startsWith("Sala online")) {
        closeRoom();
        error = "Não foi possível conectar ao TryCloudflare em 60 segundos. Tente outra rede e "
                + "abra a sala novamente.";
      }
    }, 60000);
  }
  public synchronized void closeRoom() {
    room = false;
    tunnelGeneration++;
    if (tunnel != null) {
      tunnel.destroy();
      tunnel = null;
    }
    url = "";
    token = UUID.randomUUID().toString().replace("-", "");
    tunnelStatus = "Sala fechada";
    synchronized (this) {
      messages.clear();
    }
    if (wake.isHeld())
      wake.release();
    notifyNow();
  }
  @Override
  public void onDestroy() {
    main.removeCallbacksAndMessages(null);
    closeRoom();
    if (server != null)
      server.close();
    if (eq != null)
      eq.release();
    if (player != null)
      player.release();
    if (focus != null)
      getSystemService(AudioManager.class).abandonAudioFocusRequest(focus);
    session.release();
    instance = null;
    stopForeground(STOP_FOREGROUND_REMOVE);
    super.onDestroy();
  }
}
