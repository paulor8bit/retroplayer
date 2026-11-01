package br.com.retroplayer;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.media.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
  final int bg = Color.rgb(14, 16, 23), panel = Color.rgb(25, 29, 40),
            lime = Color.rgb(201, 255, 102), muted = Color.rgb(151, 161, 179);
  Handler handler = new Handler();
  PlayerService service;
  LinearLayout root, queue, chatRows;
  TextView title, time, meta, status, link, queueTitle;
  Button play, roomButton, repeat, shuffle;
  SeekBar progress;
  SurfaceView video;
  TextView artwork;
  Wave wave;
  EditText message;
  String nickname = "Anfitrião";
  boolean dragging = false;
  int lastQueue = -1, lastIndex = -2, lastChat = -1;
  String lastError = "";
  boolean active = false;
  int dp(float n) {
    return (int) (n * getResources().getDisplayMetrics().density + .5f);
  }
  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setStatusBarColor(bg);
    getWindow().setNavigationBarColor(bg);
    nickname = getPreferences(0).getString("nickname", "Anfitrião");
    startForegroundService(new Intent(this, PlayerService.class));
    build();
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
            != PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, 4);
  }
  TextView text(String v, int size, int color) {
    TextView t = new TextView(this);
    t.setText(v);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setTypeface(Typeface.MONOSPACE);
    return t;
  }
  GradientDrawable shape(int color, int stroke) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(12));
    d.setStroke(dp(1), stroke);
    return d;
  }
  LinearLayout vertical() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }
  LinearLayout row() {
    LinearLayout l = new LinearLayout(this);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }
  void gap(LinearLayout parent, int height) {
    View v = new View(this);
    parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
  }
  Button button(String label, boolean primary, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setTextSize(12);
    b.setAllCaps(false);
    b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    b.setTextColor(primary ? bg : Color.WHITE);
    b.setBackground(
        shape(primary ? lime : Color.rgb(41, 46, 61), primary ? lime : Color.rgb(65, 71, 89)));
    b.setMinHeight(dp(45));
    b.setPadding(dp(8), 0, dp(8), 0);
    b.setOnClickListener(v -> action.run());
    return b;
  }
  void weight(LinearLayout l, View v) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
    p.setMargins(dp(3), 0, dp(3), 0);
    l.addView(v, p);
  }
  LinearLayout card(String heading) {
    LinearLayout l = vertical();
    l.setPadding(dp(14), dp(14), dp(14), dp(14));
    l.setBackground(shape(panel, Color.rgb(53, 59, 76)));
    if (heading != null) {
      l.addView(text(heading, 12, muted));
      gap(l, 12);
    }
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(14);
    root.addView(l, p);
    return l;
  }
  void build() {
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(bg);
    root = vertical();
    root.setPadding(dp(16), dp(20), dp(16), dp(24));
    scroll.addView(root);
    setContentView(scroll);
    LinearLayout header = row();
    TextView logo = text("ϟ RETROPLAYER", 25, lime);
    logo.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    header.addView(logo, new LinearLayout.LayoutParams(0, -2, 1));
    header.addView(text("TOGETHER\nv1.0 / STEREO", 10, muted));
    root.addView(header);
    gap(root, 6);
    root.addView(text("Seu som. Sua turma. A mesma frequência.", 11, muted));
    gap(root, 22);
    LinearLayout p = card("01 / NOW PLAYING");
    title = text("Abra sua primeira faixa", 19, Color.WHITE);
    title.setMaxLines(2);
    p.addView(title);
    gap(p, 6);
    meta = text("MÚSICA + VÍDEO  •  LOCAL PLAYER", 10, lime);
    p.addView(meta);
    gap(p, 14);
    FrameLayout visual = new FrameLayout(this);
    visual.setBackground(shape(Color.rgb(8, 14, 13), Color.rgb(47, 67, 44)));
    wave = new Wave();
    visual.addView(wave, new FrameLayout.LayoutParams(-1, -1));
    artwork = text("ϟ", 62, lime);
    artwork.setGravity(Gravity.CENTER);
    visual.addView(artwork, new FrameLayout.LayoutParams(-1, -1));
    video = new SurfaceView(this);
    video.setVisibility(View.GONE);
    FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
    visual.addView(video, vp);
    video.getHolder().addCallback(new SurfaceHolder.Callback() {
      public void surfaceCreated(SurfaceHolder h) {
        if (service != null)
          service.attach(h.getSurface());
      }
      public void surfaceChanged(SurfaceHolder h, int f, int w, int ht) {
        if (service != null)
          service.attach(h.getSurface());
      }
      public void surfaceDestroyed(SurfaceHolder h) {
        if (service != null)
          service.attach(null);
      }
    });
    p.addView(visual, new LinearLayout.LayoutParams(-1, dp(175)));
    gap(p, 12);
    time = text("00:00 / 00:00", 28, lime);
    p.addView(time);
    progress = new SeekBar(this);
    progress.setProgressTintList(android.content.res.ColorStateList.valueOf(lime));
    progress.setThumbTintList(android.content.res.ColorStateList.valueOf(lime));
    progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar b, int n, boolean user) {}
      public void onStartTrackingTouch(SeekBar b) {
        dragging = true;
      }
      public void onStopTrackingTouch(SeekBar b) {
        dragging = false;
        if (service != null)
          service.seek(b.getProgress());
      }
    });
    p.addView(progress);
    gap(p, 10);
    LinearLayout controls = row();
    weight(controls, button("|◀", false, () -> {
      if (service != null)
        service.next(-1);
    }));
    play = button("▶ PLAY", true, () -> {
      if (service != null)
        service.toggle();
    });
    weight(controls, play);
    weight(controls, button("▶|", false, () -> {
      if (service != null)
        service.next(1);
    }));
    p.addView(controls);
    gap(p, 10);
    LinearLayout extras = row();
    shuffle = button("Aleatório", false, () -> {
      if (service != null) {
        service.shuffle = !service.shuffle;
        shuffle.setTextColor(service.shuffle ? lime : Color.WHITE);
      }
    });
    repeat = button("Repetir", false, () -> {
      if (service != null) {
        service.repeat = !service.repeat;
        repeat.setTextColor(service.repeat ? lime : Color.WHITE);
      }
    });
    weight(extras, shuffle);
    weight(extras, repeat);
    weight(extras, button("EQ", false, this::equalizer));
    p.addView(extras);
    gap(p, 12);
    LinearLayout vol = row();
    vol.addView(text("VOL", 10, muted));
    SeekBar volume = new SeekBar(this);
    AudioManager audio = getSystemService(AudioManager.class);
    volume.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
    volume.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
    volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar b, int n, boolean user) {
        if (user)
          audio.setStreamVolume(AudioManager.STREAM_MUSIC, n, 0);
      }
      public void onStartTrackingTouch(SeekBar b) {}
      public void onStopTrackingTouch(SeekBar b) {}
    });
    vol.addView(volume, new LinearLayout.LayoutParams(0, -2, 1));
    p.addView(vol);
    LinearLayout files = card(null);
    LinearLayout fr = row();
    weight(fr, button("+ Abrir arquivos", true, this::pick));
    weight(fr, button("Entrar por link", false, this::join));
    files.addView(fr);
    gap(files, 10);
    files.addView(button("◎ Navegar na internet", false, () -> {
      if (service != null)
        service.pause();
      startActivity(new Intent(this, BrowserActivity.class));
    }));
    gap(files, 14);
    queueTitle = text("02 / PLAYLIST  •  0 FAIXAS", 12, muted);
    files.addView(queueTitle);
    gap(files, 10);
    queue = vertical();
    files.addView(queue);
    LinearLayout room = card("03 / LISTEN TOGETHER");
    room.addView(text("Aperte o play. Chame alguém.", 18, Color.WHITE));
    gap(room, 8);
    room.addView(
        text("Compartilhe música ou vídeo em tempo real.\nO convidado entra pelo navegador.", 12,
            muted));
    gap(room, 12);
    status = text("○ Sala fechada", 12, lime);
    room.addView(status);
    link = text("O link aparece aqui ao abrir a sala.", 11, muted);
    link.setTextIsSelectable(true);
    gap(room, 8);
    room.addView(link);
    gap(room, 12);
    LinearLayout rr = row();
    roomButton = button("Criar sala", true, () -> {
      if (service != null) {
        if (service.room)
          service.closeRoom();
        else
          service.openRoom();
      }
    });
    weight(rr, roomButton);
    weight(rr, button("Compartilhar", false, this::share));
    room.addView(rr);
    gap(room, 8);
    room.addView(button("Copiar link", false, () -> {
      if (service != null && !service.url.isEmpty()
          && service.tunnelStatus.startsWith("Sala online")) {
        ((android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
            .setPrimaryClip(ClipData.newPlainText("RetroPlayer", service.url));
        toast("Link copiado");
      } else
        toast("Aguarde a sala ficar online.");
    }));
    gap(room, 10);
    room.addView(text("Você controla a reprodução. A sincronia é\naproximada e depende da "
                      + "conexão.\nEncerre a sala para invalidar o link.",
        10, muted));
    LinearLayout chat = card("04 / ROOM CHAT");
    LinearLayout cr = row();
    cr.addView(
        text("Conversa na mesma vibe", 15, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1));
    cr.addView(button("Nome", false, this::name));
    chat.addView(cr);
    gap(chat, 12);
    ScrollView cs = new ScrollView(this);
    chatRows = vertical();
    cs.addView(chatRows);
    chat.addView(cs, new LinearLayout.LayoutParams(-1, dp(220)));
    gap(chat, 12);
    LinearLayout compose = row();
    message = new EditText(this);
    message.setTextColor(Color.WHITE);
    message.setHintTextColor(muted);
    message.setTextSize(14);
    message.setHint("Escreva uma mensagem…");
    message.setMaxLines(3);
    compose.addView(message, new LinearLayout.LayoutParams(0, -2, 1));
    compose.addView(button("Enviar", true, () -> {
      if (service == null || !service.room) {
        toast("Crie uma sala para conversar.");
        return;
      }
      String m = message.getText().toString().trim();
      if (!m.isEmpty()) {
        service.chat(nickname, m);
        message.setText("");
      }
    }));
    chat.addView(compose);
    root.addView(text("RETRO SOUL / CONNECTED HEART\nFeito para ouvir junto.", 10, muted));
  }
  void pick() {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.setType("*/*");
    i.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"audio/*", "video/*"});
    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    startActivityForResult(i, 7);
  }
  protected void onActivityResult(int r, int c, Intent data) {
    super.onActivityResult(r, c, data);
    if (r == 7 && c == RESULT_OK && data != null) {
      if (service == null)
        service = PlayerService.instance;
      if (service == null)
        return;
      if (data.getClipData() != null) {
        for (int i = 0; i < data.getClipData().getItemCount(); i++)
          add(data.getClipData().getItemAt(i).getUri());
      } else if (data.getData() != null)
        add(data.getData());
    }
  }
  void add(Uri u) {
    try {
      getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (Exception ignored) {
    }
    service.add(u);
  }
  void share() {
    if (service == null || service.url.isEmpty()
        || !service.tunnelStatus.startsWith("Sala online")) {
      toast("Aguarde a sala ficar online.");
      return;
    }
    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType("text/plain");
    i.putExtra(Intent.EXTRA_TEXT, "Ouça comigo no RetroPlayer! Música, vídeo e chat: " + service.url);
    startActivity(Intent.createChooser(i, "Convidar para a sala"));
  }
  void join() {
    EditText e = new EditText(this);
    e.setHint("https://…trycloudflare.com/…/");
    e.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
    new AlertDialog.Builder(this)
        .setTitle("Entrar em uma sala")
        .setMessage("Cole o link recebido. A sala abre no navegador.")
        .setView(e)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Entrar",
            (d, w) -> {
              String url = e.getText().toString().trim();
              Uri u = Uri.parse(url);
              if (!"https".equals(u.getScheme()) || u.getHost() == null) {
                toast("Use um link HTTPS válido.");
                return;
              }
              try {
                startActivity(new Intent(Intent.ACTION_VIEW, u));
              } catch (Exception ex) {
                toast("Nenhum navegador disponível.");
              }
            })
        .show();
  }
  void name() {
    EditText e = new EditText(this);
    e.setText(nickname);
    new AlertDialog.Builder(this)
        .setTitle("Seu nome no chat")
        .setView(e)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Salvar",
            (d, w) -> {
              String n = e.getText().toString().trim();
              if (!n.isEmpty()) {
                nickname = n.substring(0, Math.min(24, n.length()));
                getPreferences(0).edit().putString("nickname", nickname).apply();
              }
            })
        .show();
  }
  void equalizer() {
    if (service == null || !service.ready) {
      toast("Abra uma faixa primeiro.");
      return;
    }
    try {
      service.ensureEq();
      android.media.audiofx.Equalizer eq = service.eq;
      LinearLayout l = vertical();
      l.setPadding(dp(20), 0, dp(20), 0);
      Switch enabled = new Switch(this);
      enabled.setText("Equalizador ativo");
      enabled.setChecked(eq.getEnabled());
      enabled.setOnCheckedChangeListener((b, on) -> eq.setEnabled(on));
      l.addView(enabled);
      short[] range = eq.getBandLevelRange();
      for (short band = 0; band < eq.getNumberOfBands(); band++) {
        final short bn = band;
        TextView label = text((eq.getCenterFreq(band) / 1000) + " Hz", 12, lime);
        l.addView(label);
        SeekBar b = new SeekBar(this);
        b.setMax(range[1] - range[0]);
        b.setProgress(eq.getBandLevel(band) - range[0]);
        b.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar b, int n, boolean user) {
            if (user)
              try {
                eq.setBandLevel(bn, (short) (n + range[0]));
              } catch (Exception ignored) {
              }
          }
          public void onStartTrackingTouch(SeekBar b) {}
          public void onStopTrackingTouch(SeekBar b) {}
        });
        l.addView(b);
      }
      new AlertDialog.Builder(this)
          .setTitle("Equalizador • saída local")
          .setView(l)
          .setPositiveButton("Pronto", null)
          .show();
    } catch (Exception e) {
      toast("Equalizador indisponível neste aparelho.");
    }
  }
  void toast(String t) {
    Toast.makeText(this, t, Toast.LENGTH_LONG).show();
  }
  String clock(int seconds) {
    return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
  }
  Runnable refresh = new Runnable() {
    public void run() {
      if (!active)
        return;
      service = PlayerService.instance;
      if (service != null) {
        try {
          JSONObject s = new JSONObject(service.state);
          title.setText(s.optString("title"));
          int pos = (int) (s.optDouble("position") * 1000),
              dur = (int) (s.optDouble("duration") * 1000);
          time.setText(clock(pos / 1000) + " / " + clock(dur / 1000));
          if (!dragging) {
            progress.setMax(dur);
            progress.setProgress(pos);
          }
          boolean playing = s.optBoolean("playing");
          play.setText(playing ? "Ⅱ PAUSA" : "▶ PLAY");
          wave.playing = playing;
          wave.invalidate();
          boolean isVideo =
              service.index >= 0 && service.tracks.get(service.index).mime.startsWith("video/");
          video.setVisibility(isVideo ? View.VISIBLE : View.GONE);
          artwork.setVisibility(isVideo ? View.GONE : View.VISIBLE);
          meta.setText(isVideo ? "VIDEO  •  LOCAL + TOGETHER" : "STEREO  •  LOCAL + TOGETHER");
          status.setText((service.room ? "● " : "○ ") + service.tunnelStatus);
          link.setText(
              service.url.isEmpty() ? "O link aparece aqui ao abrir a sala." : service.url);
          roomButton.setText(service.room ? "Encerrar sala" : "Criar sala");
          if (lastQueue != service.tracks.size() || lastIndex != service.index) {
            queue.removeAllViews();
            lastQueue = service.tracks.size();
            lastIndex = service.index;
            queueTitle.setText("02 / PLAYLIST  •  " + lastQueue + " FAIXAS");
            if (lastQueue == 0)
              queue.addView(
                  text("Sua coleção começa aqui.\nAdicione arquivos do seu celular.", 12, muted));
            for (int i = 0; i < lastQueue; i++) {
              final int n = i;
              TextView t =
                  text(String.format(Locale.US, "%02d  %s", i + 1, service.tracks.get(i).name), 13,
                      i == lastIndex ? lime : Color.WHITE);
              t.setPadding(dp(8), dp(12), dp(8), dp(12));
              t.setMaxLines(2);
              t.setBackground(shape(i == lastIndex ? Color.rgb(39, 50, 35) : panel, panel));
              t.setOnClickListener(v -> service.select(n));
              queue.addView(t);
            }
          }
          JSONArray chats = new JSONArray(service.chatJson());
          int id = chats.length() > 0 ? chats.getJSONObject(chats.length() - 1).getInt("id") : 0;
          if (id != lastChat) {
            lastChat = id;
            chatRows.removeAllViews();
            if (chats.length() == 0)
              chatRows.addView(
                  text("O chat aparece aqui.\nCrie uma sala e convide alguém!", 12, muted));
            for (int i = Math.max(0, chats.length() - 40); i < chats.length(); i++) {
              JSONObject j = chats.getJSONObject(i);
              TextView t = text(j.getString("name") + "  •  "
                      + new java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                          .format(new Date(j.getLong("time")))
                      + "\n" + j.getString("text"),
                  13, Color.WHITE);
              t.setPadding(dp(8), dp(8), dp(8), dp(8));
              chatRows.addView(t);
            }
            chatRows.post(() -> ((ScrollView) chatRows.getParent()).fullScroll(View.FOCUS_DOWN));
          }
          if (!service.error.isEmpty() && !service.error.equals(lastError)) {
            lastError = service.error;
            toast(lastError);
          }
        } catch (Exception ignored) {
        }
      }
      handler.postDelayed(this, 400);
    }
  };
  @Override
  protected void onResume() {
    super.onResume();
    active = true;
    handler.post(refresh);
    handler.postDelayed(() -> {
      if (PlayerService.instance != null) {
        service = PlayerService.instance;
        service.restore();
        if (video.getHolder().getSurface().isValid())
          service.attach(video.getHolder().getSurface());
      }
    }, 500);
  }
  @Override
  protected void onPause() {
    active = false;
    handler.removeCallbacks(refresh);
    super.onPause();
  }
  @Override
  protected void onDestroy() {
    if (service != null)
      service.attach(null);
    handler.removeCallbacksAndMessages(null);
    super.onDestroy();
  }
  class Wave extends View {
    Paint paint = new Paint(3);
    boolean playing;
    Wave() {
      super(MainActivity.this);
    }
    protected void onDraw(Canvas c) {
      int w = getWidth(), h = getHeight();
      paint.setColor(Color.rgb(25, 44, 29));
      for (int x = 0; x < w; x += dp(16)) c.drawLine(x, 0, x, h, paint);
      for (int y = 0; y < h; y += dp(16)) c.drawLine(0, y, w, y, paint);
      paint.setColor(Color.rgb(104, 155, 62));
      for (int i = 0; i < 36; i++) {
        float x = (i + .2f) * w / 36f;
        float a = playing
            ? (float) (.15
                  + .75
                      * Math.abs(Math.sin(i * 1.37 + System.currentTimeMillis() / 380.0)
                          * Math.cos(i * .22 + System.currentTimeMillis() / 1700.0)))
            : .08f;
        c.drawRect(x, h - a * h * .8f, x + w / 52f, h, paint);
      }
    }
  }
}
