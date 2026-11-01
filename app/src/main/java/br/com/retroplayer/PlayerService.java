package br.com.retroplayer;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import android.net.Uri;
import android.os.*;
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
  BrowserSession browser;
  public BrowserSession browser() {
    if (browser == null)
      browser = new BrowserSession(this);
    return browser;
  }
  public void controlToggle() {
    if (browser != null && browser.active) {
      if (browser.playing)
        browser.pauseMedia();
      else
        browser.playMedia();
      notifyNow();
    } else
      toggle();
  }
  public void controlPlay() {
    if (browser != null && browser.active)
      browser.playMedia();
    else
      play();
  }
  public void controlPause() {
    if (browser != null && browser.active)
      browser.pauseMedia();
    pause();
  }
  public volatile boolean importing = false;
  public volatile String importStatus = "";
  public final java.util.concurrent.atomic.AtomicBoolean cancelImport =
      new java.util.concurrent.atomic.AtomicBoolean();
  final java.util.concurrent.ExecutorService library =
      java.util.concurrent.Executors.newSingleThreadExecutor();
  public long tracksVersion = 0;
  boolean restoring = false, restored = false;
  android.content.BroadcastReceiver noisy;
  MediaPlayer player;
  RoomServer server;
  java.lang.Process tunnel;
  final Handler main = new Handler(Looper.getMainLooper());
  android.os.PowerManager.WakeLock wake;
  MediaSession session;
  String metadataTitle = "";
  Surface surface;
  long generation = 0;
  boolean repeat = false, shuffle = false;
  int chatId = 0;
  int tunnelGeneration = 0;
  public static class Track {
    public Uri uri;
    public String name, mime;
    public long size;
    Map<String, String> headers;
    int startPosition = 0;
    Track(Uri u, String n, String m, long z) {
      uri = u;
      name = n;
      mime = m;
      size = z;
    }
  }
  private volatile byte[] themeCover;
  private final Object themeLock = new Object();
  InputStream openThemeAsset(String name) throws IOException {
    if (name.equals("cover.png")) {
      if (themeCover == null)
        synchronized (themeLock) {
          if (themeCover == null) {
            android.graphics.BitmapFactory.Options options =
                new android.graphics.BitmapFactory.Options();
            options.inSampleSize = 2;
            options.inScaled = false;
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeResource(
                getResources(), R.drawable.deck_cover, options);
            if (bitmap == null)
              throw new IOException("Capa indisponível");
            try {
              ByteArrayOutputStream encoded = new ByteArrayOutputStream();
              if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, encoded))
                throw new IOException("Capa indisponível");
              themeCover = encoded.toByteArray();
            } finally {
              bitmap.recycle();
            }
          }
        }
      return new ByteArrayInputStream(themeCover);
    }
    if (name.equals("mono.ttf") || name.equals("display.ttf"))
      return getAssets().open("theme/" + name);
    throw new FileNotFoundException(name);
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
        controlPlay();
      }
      public void onPause() {
        controlPause();
      }
      public void onSkipToNext() {
        next(1);
      }
      public void onSkipToPrevious() {
        next(-1);
      }
      public void onSeekTo(long p) {
        if (browser != null && browser.active)
          browser.seekMedia((int) p);
        else
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
    noisy = new BroadcastReceiver() {
      public void onReceive(Context c, Intent i) {
        controlPause();
      }
    };
    if (Build.VERSION.SDK_INT >= 33)
      registerReceiver(noisy, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
          Context.RECEIVER_NOT_EXPORTED);
    else
      registerReceiver(noisy, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    main.post(tick);
  }
  Notification notification() {
    Intent open = new Intent(this, MainActivity.class);
    PendingIntent pi = PendingIntent.getActivity(
        this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    Notification.Builder n =
        new Notification.Builder(this, "player")
            .setSmallIcon(br.com.retroplayer.R.drawable.icon)
            .setContentTitle(browser != null && browser.active
                    ? browser.pageTitle
                    : (index >= 0 ? tracks.get(index).name : "RetroPlayer"))
            .setContentText(browser != null && browser.active
                    ? "Web • áudio em segundo plano"
                    : (room ? tunnelStatus : "Sua música continua em segundo plano"))
            .setContentIntent(pi)
            .setOngoing(true)
            .setStyle(new Notification.MediaStyle()
                    .setMediaSession(session.getSessionToken())
                    .setShowActionsInCompactView(0, 1));
    for (String a : new String[] {"toggle", "next", "stop"}) {
      PendingIntent p = PendingIntent.getService(this, a.hashCode(),
          new Intent(this, PlayerService.class).setAction(a), PendingIntent.FLAG_IMMUTABLE);
      n.addAction(a.equals("toggle")
              ? ((browser != null && browser.active ? browser.playing : ready && player.isPlaying())
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play)
              : a.equals("next") ? android.R.drawable.ic_media_next
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
        controlToggle();
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
      if (browser != null && browser.active) {
        playing = browser.playing;
        pos = (int) (browser.position * 1000);
        duration = (int) (browser.duration * 1000);
      }
      String currentTitle = browser != null && browser.active ? browser.pageTitle
          : index >= 0                                        ? tracks.get(index).name
                                                              : "RetroPlayer";
      if (!currentTitle.equals(metadataTitle)) {
        metadataTitle = currentTitle;
        session.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST,
                    browser != null && browser.active ? "Navegador" : "RetroPlayer")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, Math.max(0, duration))
                .build());
      }
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
  Track describe(Uri uri) throws Exception {
    String name = "Mídia";
    long size = -1;
    try (android.database.Cursor c = getContentResolver().query(uri,
             new String[] {android.provider.OpenableColumns.DISPLAY_NAME,
                 android.provider.OpenableColumns.SIZE},
             null, null, null)) {
      if (c != null && c.moveToFirst()) {
        name = c.isNull(0) ? name : c.getString(0);
        size = c.isNull(1) ? -1 : c.getLong(1);
      }
    }
    String mime = getContentResolver().getType(uri);
    return new Track(uri, name, mime == null ? guessMime(name) : mime, size);
  }
  static String guessMime(String name) {
    String n = name.toLowerCase(Locale.ROOT);
    if (n.endsWith(".mp4") || n.endsWith(".m4v"))
      return "video/mp4";
    if (n.endsWith(".webm"))
      return "video/webm";
    if (n.endsWith(".mkv"))
      return "video/x-matroska";
    if (n.endsWith(".m3u8"))
      return "application/vnd.apple.mpegurl";
    if (n.endsWith(".flac"))
      return "audio/flac";
    if (n.endsWith(".ogg") || n.endsWith(".opus"))
      return "audio/ogg";
    if (n.endsWith(".wav"))
      return "audio/wav";
    if (n.endsWith(".m4a"))
      return "audio/mp4";
    return "audio/mpeg";
  }
  public void add(Uri uri) {
    if ("https".equals(uri.getScheme())) {
      addRemote(uri.toString());
      return;
    }
    library.execute(() -> {
      try {
        Track t = describe(uri);
        main.post(() -> {
          if (instance == this)
            appendTracks(java.util.Collections.singletonList(t), true);
        });
      } catch (Exception e) {
        error = "Arquivo indisponível: " + e.getMessage();
      }
    });
  }
  int appendTracks(List<Track> added, boolean autoplay) {
    Set<String> existing = new HashSet<>();
    for (Track t : tracks) existing.add(t.uri.toString());
    int count = 0;
    for (Track t : added)
      if (existing.add(t.uri.toString())) {
        tracks.add(t);
        count++;
      }
    if (count > 0) {
      tracksVersion++;
      saveQueue();
      if (index < 0)
        select(0, autoplay);
    }
    return count;
  }
  public void importFolder(Uri tree) {
    if (importing)
      return;
    importing = true;
    importStatus = "Lendo a pasta…";
    cancelImport.set(false);
    library.execute(() -> {
      MediaFiles.Result r = FolderImporter.scan(getContentResolver(), tree, cancelImport,
          (files,
              folders) -> importStatus = files + " mídias • " + folders + " pastas verificadas");
      ArrayList<Track> added = new ArrayList<>();
      for (MediaFiles.Entry e : r.files)
        added.add(new Track(Uri.parse(e.uri), e.name,
            e.mime == null || (!e.mime.startsWith("audio/") && !e.mime.startsWith("video/"))
                ? guessMime(e.name)
                : e.mime,
            e.size));
      main.post(() -> {
        if (instance != this)
          return;
        int count = r.cancelled ? 0 : appendTracks(added, true);
        importing = false;
        importStatus = r.cancelled ? "Importação cancelada"
                                   : count + " arquivos adicionados"
                + (r.unreadable > 0 ? " • " + r.unreadable + " pastas sem acesso" : "");
      });
    });
  }
  public void addRemote(String link) {
    addWebMedia(link, null, null, 0);
  }
  public void addWebMedia(String link, String title, Map<String, String> headers, int position) {
    try {
      Uri uri = Uri.parse(link);
      if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
        throw new IllegalArgumentException("Use HTTPS");
      String name = title;
      if (name == null || name.isEmpty())
        name = uri.getLastPathSegment();
      if (name == null || name.isEmpty())
        name = uri.getHost();
      Track t = new Track(uri, name, guessMime(uri.getPath() == null ? name : uri.getPath()), -1);
      t.headers = headers;
      t.startPosition = position;
      int at = -1;
      for (int i = 0; i < tracks.size(); i++)
        if (tracks.get(i).uri.equals(uri)) {
          at = i;
          break;
        }
      if (at < 0) {
        tracks.add(t);
        at = tracks.size() - 1;
      } else
        tracks.set(at, t);
      tracksVersion++;
      saveQueue();
      select(at);
    } catch (Exception e) {
      error = "Link não suportado: " + e.getMessage();
      if (browser != null)
        browser.handoffPending = false;
    }
  }
  void saveQueue() {
    try {
      JSONArray items = new JSONArray();
      for (Track t : tracks)
        items.put(new JSONObject()
                .put("uri", t.uri.toString())
                .put("name", t.name)
                .put("mime", t.mime)
                .put("size", t.size));
      getSharedPreferences("queue", 0)
          .edit()
          .putString("items", items.toString())
          .remove("uris")
          .apply();
    } catch (Exception ignored) {
    }
  }
  public void restore() {
    if (restored || restoring || !tracks.isEmpty())
      return;
    restoring = true;
    library.execute(() -> {
      ArrayList<Track> result = new ArrayList<>();
      try {
        android.content.SharedPreferences prefs = getSharedPreferences("queue", 0);
        if (prefs.contains("items")) {
          JSONArray a = new JSONArray(prefs.getString("items", "[]"));
          for (int i = 0; i < a.length(); i++) {
            JSONObject j = a.getJSONObject(i);
            result.add(new Track(Uri.parse(j.getString("uri")), j.getString("name"),
                j.getString("mime"), j.optLong("size", -1)));
          }
        } else {
          JSONArray a = new JSONArray(prefs.getString("uris", "[]"));
          for (int i = 0; i < a.length(); i++) {
            Uri u = Uri.parse(a.getString(i));
            if ("https".equals(u.getScheme()))
              result.add(new Track(u,
                  u.getLastPathSegment() == null ? u.getHost() : u.getLastPathSegment(),
                  guessMime(u.getPath() == null ? "" : u.getPath()), -1));
            else
              try {
                result.add(describe(u));
              } catch (Exception ignored) {
              }
          }
        }
      } catch (Exception ignored) {
      }
      main.post(() -> {
        if (instance != this)
          return;
        restoring = false;
        restored = true;
        appendTracks(result, false);
      });
    });
  }
  public void removeTrack(int at) {
    if (at < 0 || at >= tracks.size())
      return;
    if (tracks.size() == 1) {
      clearQueue();
      return;
    }
    boolean current = at == index;
    tracks.remove(at);
    tracksVersion++;
    if (at < index)
      index--;
    if (current)
      select(Math.min(at, tracks.size() - 1));
    saveQueue();
  }
  public void clearQueue() {
    if (importing)
      cancelImport.set(true);
    ready = false;
    if (eq != null) {
      eq.release();
      eq = null;
    }
    if (player != null) {
      player.release();
      player = null;
    }
    tracks.clear();
    index = -1;
    generation++;
    tracksVersion++;
    saveQueue();
    snapshot();
    notifyNow();
  }
  public void select(int n) {
    select(n, true);
  }
  public void select(int n, boolean autoplay) {
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
      Track chosen = tracks.get(n);
      if (chosen.headers != null)
        player.setDataSource(this, chosen.uri, chosen.headers);
      else
        player.setDataSource(this, chosen.uri);
      if (surface != null)
        player.setSurface(surface);
      player.setOnPreparedListener(p -> {
        if (player != p)
          return;
        ready = true;
        if (chosen.startPosition > 0 && p.getDuration() > 0) {
          p.seekTo(chosen.startPosition, MediaPlayer.SEEK_CLOSEST);
          chosen.startPosition = 0;
        }
        if (autoplay)
          play();
        session.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, chosen.name)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "RetroPlayer")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, Math.max(0, p.getDuration()))
                .build());
        snapshot();
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
        if (browser != null)
          browser.handoffPending = false;
        error = "Formato não suportado ou arquivo indisponível (" + w + ").";
        return true;
      });
      player.prepareAsync();
      notifyNow();
    } catch (Exception e) {
      if (browser != null)
        browser.handoffPending = false;
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
                        if (player.isPlaying())
                          player.pause();
                        resumeOnFocus = false;
                      } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        resumeOnFocus = player.isPlaying();
                        if (resumeOnFocus)
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
        if (browser != null) {
          browser.pauseMedia();
          browser.active = false;
          browser.handoffPending = false;
        }
        player.start();
        snapshot();
        notifyNow();
      }
    }
  }
  public void pause() {
    if (ready) {
      resumeOnFocus = false;
      if (player.isPlaying())
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
    ready = false;
    cancelImport.set(true);
    library.shutdownNow();
    if (noisy != null)
      unregisterReceiver(noisy);
    if (browser != null)
      browser.destroy();
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
