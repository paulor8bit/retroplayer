package br.com.retroplayer;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
  final int bg = Ui.BG, panel = Ui.PANEL, lime = Ui.LIME, muted = Ui.MUTED;
  final Handler handler = new Handler(Looper.getMainLooper());
  PlayerService service;
  FrameLayout pages, artFrame;
  LinearLayout shell, root, chatRows, mini, roomPage, webPage;
  ScrollView playerPage;
  View libraryPage;
  TextView title, artist, elapsed, total, meta, status, link, queueTitle, importStatus, miniTitle,
      miniSubtitle, chatHint, headerSubtitle, webSummary;
  ImageView play, miniPlay, shuffle, repeat, artwork;
  SurfaceView video;
  SeekBar progress;
  Ui.SpriteView mascot;
  Ui.ProgressTrace trace;
  TextView playbackState;
  LinearLayout recentRows;
  EditText message, search;
  ListView queue;
  QueueAdapter adapter;
  String nickname = "Anfitrião", lastError = "", query = "";
  long lastVersion = -1;
  int lastIndex = -2, lastChat = -1, tab = 0;
  boolean active, dragging;
  final ArrayList<Integer> visibleTracks = new ArrayList<>();
  final ArrayList<View> pageViews = new ArrayList<>();
  final ArrayList<LinearLayout> navItems = new ArrayList<>();
  int dp(float n) {
    return Ui.dp(this, n);
  }
  TextView text(String s, int size, int color) {
    return Ui.text(this, s, size, color);
  }
  LinearLayout vertical() {
    return Ui.column(this);
  }
  LinearLayout row() {
    return Ui.row(this);
  }
  void gap(LinearLayout l, int n) {
    Ui.gap(this, l, n);
  }
  Button button(String s, boolean primary, Runnable r) {
    return Ui.button(this, s, primary, r);
  }
  GradientDrawable shape(int color, int stroke) {
    GradientDrawable d = Ui.shape(this, color, 8);
    d.setStroke(dp(1), stroke);
    return d;
  }
  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    getWindow().setStatusBarColor(bg);
    getWindow().setNavigationBarColor(panel);
    nickname = getPreferences(0).getString("nickname", "Anfitrião");
    startForegroundService(new Intent(this, PlayerService.class));
    build();
    if (saved != null)
      switchTab(saved.getInt("tab", 0));
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
            != PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, 4);
  }
  void weight(LinearLayout l, View v) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
    p.setMargins(dp(4), 0, dp(4), 0);
    l.addView(v, p);
  }
  LinearLayout padded() {
    LinearLayout l = vertical();
    l.setPadding(dp(20), dp(8), dp(20), dp(22));
    return l;
  }
  ScrollView scroller(LinearLayout content) {
    ScrollView s = new ScrollView(this);
    s.setFillViewport(true);
    s.setClipToPadding(false);
    s.setVerticalScrollBarEnabled(false);
    s.addView(content);
    return s;
  }
  void section(LinearLayout parent, String label) {
    TextView t = Ui.label(this, label);
    parent.addView(t);
    gap(parent, 12);
  }
  void icon(LinearLayout parent, String kind, int color, Runnable action, int size) {
    ImageView v = Ui.iconButton(this, kind, color, action);
    parent.addView(v, new LinearLayout.LayoutParams(dp(size), dp(size)));
  }
  void build() {
    shell = vertical();
    shell.setBackgroundColor(bg);
    setContentView(shell);
    Ui.StatusStrip strip = new Ui.StatusStrip(this);
    LinearLayout.LayoutParams stripParams = new LinearLayout.LayoutParams(-1, dp(28));
    stripParams.setMargins(dp(18), 0, dp(18), 0);
    shell.addView(strip, stripParams);
    LinearLayout header = row();
    header.setBackground(shape(bg, lime));
    header.setPadding(dp(16), dp(8), dp(12), dp(8));
    ImageView mark = Ui.icon(this, "headphones", lime, 25);
    header.addView(mark, new LinearLayout.LayoutParams(dp(38), dp(48)));
    LinearLayout heading = vertical();
    TextView brand = text("RETROPLAYER", 24, Ui.TEXT);
    brand.setTypeface(Ui.display(this));
    brand.setSingleLine(true);
    heading.addView(brand);
    headerSubtitle = Ui.label(this, "CYBRDECK // OPERADOR LOCAL");
    headerSubtitle.setTextSize(9);
    heading.addView(headerSubtitle);
    header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
    icon(header, "plus", Ui.TEXT, this::addMenu, 44);
    icon(header, "more", Ui.TEXT, this::menu, 40);
    LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, dp(72));
    headerParams.setMargins(dp(14), dp(4), dp(14), dp(12));
    shell.addView(header, headerParams);
    pages = new FrameLayout(this);
    shell.addView(pages, new LinearLayout.LayoutParams(-1, 0, 1));
    buildPlayer();
    buildLibrary();
    buildRoom();
    buildWeb();
    for (View view : pageViews) pages.addView(view, new FrameLayout.LayoutParams(-1, -1));
    mini = row();
    mini.setPadding(dp(12), dp(6), dp(10), dp(6));
    mini.setBackground(shape(Ui.PANEL, Ui.LINE));
    LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, dp(68));
    mp.setMargins(dp(12), dp(4), dp(12), dp(6));
    shell.addView(mini, mp);
    ImageView cover = new ImageView(this);
    cover.setImageResource(R.drawable.deck_cover);
    cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
    cover.setBackground(Ui.shape(this, bg, 9));
    cover.setClipToOutline(true);
    mini.addView(cover, new LinearLayout.LayoutParams(dp(48), dp(48)));
    LinearLayout ml = vertical();
    ml.setPadding(dp(12), 0, dp(6), 0);
    miniTitle = text("Sua música está aqui", 13, Ui.TEXT);
    miniTitle.setMaxLines(1);
    miniTitle.setEllipsize(TextUtils.TruncateAt.END);
    ml.addView(miniTitle);
    miniSubtitle = text("Escolha um arquivo para começar", 11, muted);
    ml.addView(miniSubtitle);
    mini.addView(ml, new LinearLayout.LayoutParams(0, -2, 1));
    miniPlay = Ui.iconButton(this, "play", lime, () -> {
      if (service != null)
        service.controlToggle();
    });
    miniPlay.setContentDescription("Reproduzir ou pausar");
    mini.addView(miniPlay, new LinearLayout.LayoutParams(dp(46), dp(46)));
    icon(mini, "next", Ui.TEXT, () -> {
      if (service != null)
        service.next(1);
    }, 42);
    mini.setOnClickListener(v -> switchTab(0));
    LinearLayout nav = row();
    nav.setPadding(dp(8), dp(2), dp(8), dp(4));
    nav.setBackgroundColor(bg);
    String[] names = {"PLAYER", "ARQUIVOS", "SALA", "WEB"},
             icons = {"headphones", "library", "room", "web"};
    for (int i = 0; i < 4; i++) {
      final int n = i;
      LinearLayout item = vertical();
      item.setGravity(Gravity.CENTER);
      item.setBackground(Ui.ripple(this, Color.TRANSPARENT, 16));
      ImageView image = Ui.icon(this, icons[i], muted, 21);
      item.addView(image, new LinearLayout.LayoutParams(dp(38), dp(34)));
      TextView label = text(names[i], 11, muted);
      item.addView(label);
      item.setOnClickListener(v -> switchTab(n));
      item.setContentDescription(names[i]);
      navItems.add(item);
      nav.addView(item, new LinearLayout.LayoutParams(0, dp(60), 1));
    }
    shell.addView(nav);
    switchTab(0);
  }
  void buildPlayer() {
    root = padded();
    LinearLayout channel = row();
    TextView label = Ui.label(this, "ACTIVE_MEDIA // PLAYER");
    channel.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
    meta = Ui.label(this, "ESTÉREO");
    meta.setTextColor(lime);
    channel.addView(meta);
    root.addView(channel);
    gap(root, 14);
    LinearLayout activeMedia = row();
    activeMedia.setGravity(Gravity.TOP);
    activeMedia.setPadding(dp(12), dp(14), dp(12), dp(14));
    activeMedia.setBackground(shape(bg, lime));
    artwork = new ImageView(this);
    artwork.setImageResource(R.drawable.deck_cover);
    artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
    artwork.setBackground(shape(panel, lime));
    artwork.setClipToOutline(true);
    activeMedia.addView(artwork, new LinearLayout.LayoutParams(dp(94), dp(132)));
    LinearLayout trackInfo = vertical();
    trackInfo.setPadding(dp(14), 0, 0, 0);
    trackInfo.addView(Ui.label(this, "ARQUIVO ATIVO"));
    gap(trackInfo, 5);
    title = text("PRONTO PARA O PLAY", 27, Ui.TEXT);
    title.setTypeface(Ui.display(this));
    title.setAllCaps(true);
    title.setMaxLines(3);
    title.setEllipsize(TextUtils.TruncateAt.END);
    trackInfo.addView(title);
    gap(trackInfo, 7);
    artist = text("Selecione um arquivo ou uma pasta", 12, muted);
    trackInfo.addView(artist);
    gap(trackInfo, 12);
    Ui.BarcodeView barcode = new Ui.BarcodeView(this);
    trackInfo.addView(barcode, new LinearLayout.LayoutParams(dp(86), dp(16)));
    activeMedia.addView(trackInfo, new LinearLayout.LayoutParams(0, -2, 1));
    root.addView(activeMedia);
    gap(root, 14);
    artFrame = new FrameLayout(this);
    artFrame.setBackground(shape(bg, Ui.LINE));
    artFrame.setClipToOutline(true);
    video = new SurfaceView(this);
    video.setVisibility(View.GONE);
    artFrame.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
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
    artFrame.setVisibility(View.GONE);
    root.addView(artFrame, new LinearLayout.LayoutParams(-1, dp(200)));
    trace = new Ui.ProgressTrace(this);
    root.addView(trace, new LinearLayout.LayoutParams(-1, dp(54)));
    playbackState = Ui.label(this, "AGUARDANDO // 0% REPRODUZIDO");
    playbackState.setTextColor(lime);
    root.addView(playbackState);
    gap(root, 10);
    progress = new SeekBar(this);
    progress.setPadding(dp(2), 0, dp(2), 0);
    progress.setProgressTintList(android.content.res.ColorStateList.valueOf(lime));
    progress.setThumbTintList(android.content.res.ColorStateList.valueOf(lime));
    progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar b, int value, boolean user) {
        if (user)
          elapsed.setText(clock(value / 1000));
      }
      public void onStartTrackingTouch(SeekBar b) {
        dragging = true;
      }
      public void onStopTrackingTouch(SeekBar b) {
        dragging = false;
        if (service != null)
          service.seek(b.getProgress());
      }
    });
    root.addView(progress, new LinearLayout.LayoutParams(-1, dp(28)));
    LinearLayout times = row();
    elapsed = text("0:00", 11, muted);
    elapsed.setTypeface(Typeface.MONOSPACE);
    total = text("0:00", 11, muted);
    total.setTypeface(Typeface.MONOSPACE);
    times.addView(elapsed, new LinearLayout.LayoutParams(0, -2, 1));
    times.addView(total);
    root.addView(times);
    gap(root, 10);
    LinearLayout controls = row();
    controls.setGravity(Gravity.CENTER);
    shuffle = Ui.iconButton(this, "shuffle", muted, () -> {
      if (service != null) {
        service.shuffle = !service.shuffle;
        updateModeIcons();
      }
    });
    controls.addView(shuffle, new LinearLayout.LayoutParams(0, dp(48), 1));
    icon(controls, "previous", Ui.TEXT, () -> {
      if (service != null)
        service.next(-1);
    }, 52);
    play = Ui.iconButton(this, "play", bg, () -> {
      if (service != null)
        service.toggle();
    });
    play.setContentDescription("Reproduzir ou pausar");
    play.setBackground(Ui.ripple(this, Ui.ORANGE, 8));
    play.setPadding(dp(20), dp(20), dp(20), dp(20));
    LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(70), dp(70));
    pp.setMargins(dp(6), 0, dp(6), 0);
    controls.addView(play, pp);
    icon(controls, "next", Ui.TEXT, () -> {
      if (service != null)
        service.next(1);
    }, 52);
    repeat = Ui.iconButton(this, "repeat", muted, () -> {
      if (service != null) {
        service.repeat = !service.repeat;
        updateModeIcons();
      }
    });
    controls.addView(repeat, new LinearLayout.LayoutParams(0, dp(48), 1));
    root.addView(controls);
    gap(root, 14);
    LinearLayout actions = row();
    weight(actions, button("Adicionar mídia", false, this::addMenu));
    weight(actions, button("Ouvir com alguém", false, () -> switchTab(2)));
    root.addView(actions);
    gap(root, 16);
    LinearLayout extras = row();
    ImageView eq = Ui.iconButton(this, "equalizer", muted, this::equalizer);
    eq.setContentDescription("Equalizador");
    extras.addView(eq, new LinearLayout.LayoutParams(dp(46), dp(46)));
    extras.addView(text("Volume", 11, muted));
    SeekBar vol = new SeekBar(this);
    AudioManager audio = getSystemService(AudioManager.class);
    vol.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
    vol.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
    vol.setProgressTintList(android.content.res.ColorStateList.valueOf(lime));
    vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar b, int n, boolean user) {
        if (user)
          audio.setStreamVolume(AudioManager.STREAM_MUSIC, n, 0);
      }
      public void onStartTrackingTouch(SeekBar b) {}
      public void onStopTrackingTouch(SeekBar b) {}
    });
    extras.addView(vol, new LinearLayout.LayoutParams(0, dp(38), 1));
    root.addView(extras);
    gap(root, 12);
    LinearLayout memo = row();
    memo.setPadding(dp(14), dp(12), dp(14), dp(12));
    memo.setBackground(shape(bg, lime));
    ImageView memoIcon = Ui.icon(this, "headphones", Ui.ORANGE, 24);
    memo.addView(memoIcon, new LinearLayout.LayoutParams(dp(40), dp(44)));
    LinearLayout memoLabels = vertical();
    memoLabels.addView(Ui.label(this, "CANAL DE ÁUDIO // LOCAL"));
    gap(memoLabels, 5);
    memoLabels.addView(text("Seu som continua em segundo plano.", 12, lime));
    memo.addView(memoLabels, new LinearLayout.LayoutParams(0, -2, 1));
    root.addView(memo);
    gap(root, 20);
    section(root, "ÚLTIMOS_ARQUIVOS");
    recentRows = vertical();
    root.addView(recentRows);
    gap(root, 14);
    LinearLayout footer = row();
    Ui.BarcodeView code = new Ui.BarcodeView(this);
    footer.addView(code, new LinearLayout.LayoutParams(dp(60), dp(19)));
    TextView signature = Ui.label(this, "  RETROPLAYER // CYBR 1.2");
    footer.addView(signature);
    root.addView(footer);
    playerPage = scroller(root);
    pageViews.add(playerPage);
  }
  void updateRecent() {
    if (recentRows == null)
      return;
    recentRows.removeAllViews();
    if (service == null || service.tracks.isEmpty()) {
      recentRows.addView(text("Nenhum arquivo na fila.\nUse + para abrir sua coleção.", 13, muted));
      return;
    }
    for (int i = service.tracks.size() - 1; i >= Math.max(0, service.tracks.size() - 3); i--) {
      final int at = i;
      PlayerService.Track track = service.tracks.get(i);
      LinearLayout item = row();
      item.setPadding(dp(6), dp(8), dp(6), dp(8));
      ImageView cover = new ImageView(this);
      cover.setImageResource(R.drawable.deck_cover);
      cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
      item.addView(cover, new LinearLayout.LayoutParams(dp(40), dp(50)));
      LinearLayout labels = vertical();
      labels.setPadding(dp(12), 0, 0, 0);
      TextView name = text(track.name, 17, lime);
      name.setTypeface(Ui.display(this));
      name.setMaxLines(1);
      name.setEllipsize(TextUtils.TruncateAt.END);
      labels.addView(name);
      labels.addView(Ui.label(this,
          ("https".equals(track.uri.getScheme()) ? "INTERNET" : "ARQUIVO LOCAL") + " // "
              + (i + 1)));
      item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
      item.setBackground(Ui.ripple(this, bg, 6));
      item.setOnClickListener(v -> service.select(at));
      recentRows.addView(item);
      View line = new View(this);
      line.setBackgroundColor(Ui.LINE);
      recentRows.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }
  }
  void buildLibrary() {
    LinearLayout l = padded();
    LinearLayout heading = row();
    TextView t = text("Sua biblioteca", 25, Ui.TEXT);
    t.setTypeface(Ui.display(this));
    heading.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
    icon(heading, "more", muted,
        ()
            -> new AlertDialog.Builder(this)
                .setItems(new String[] {"Adicionar um arquivo", "Adicionar vários arquivos",
                              "Adicionar uma pasta", "Limpar lista de reprodução"},
                    (d, n) -> {
                      if (n == 0)
                        pick(false);
                      if (n == 1)
                        pick(true);
                      if (n == 2)
                        pickFolder();
                      if (n == 3)
                        clearQueue();
                    })
                .show(),
        40);
    l.addView(heading);
    gap(l, 6);
    queueTitle = text("Todas as suas músicas e vídeos", 13, muted);
    l.addView(queueTitle);
    gap(l, 17);
    LinearLayout add = row();
    weight(add, button("Um arquivo", false, () -> pick(false)));
    Button folder = button("Escolher pasta", true, this::pickFolder);
    weight(add, folder);
    l.addView(add);
    gap(l, 14);
    LinearLayout find = row();
    find.setBackground(Ui.shape(this, panel, 13));
    find.addView(Ui.icon(this, "search", muted, 20), new LinearLayout.LayoutParams(dp(43), dp(44)));
    search = new EditText(this);
    search.setSingleLine();
    search.setTextSize(14);
    search.setHint("Buscar na lista de reprodução");
    search.setHintTextColor(muted);
    search.setTextColor(Ui.TEXT);
    search.setBackgroundColor(Color.TRANSPARENT);
    find.addView(search, new LinearLayout.LayoutParams(0, dp(46), 1));
    l.addView(find);
    search.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int a, int c, int f) {}
      public void onTextChanged(CharSequence s, int a, int b, int c) {
        query = s.toString().toLowerCase(Locale.ROOT);
        filterQueue();
      }
      public void afterTextChanged(Editable e) {}
    });
    gap(l, 8);
    LinearLayout importRow = row();
    importStatus = text("", 12, lime);
    importRow.addView(importStatus, new LinearLayout.LayoutParams(0, -2, 1));
    ImageView cancel = Ui.iconButton(this, "close", muted, () -> {
      if (service != null && service.importing)
        service.cancelImport.set(true);
    });
    cancel.setContentDescription("Cancelar importação de pasta");
    importRow.addView(cancel, new LinearLayout.LayoutParams(dp(36), dp(32)));
    l.addView(importRow);
    FrameLayout listHolder = new FrameLayout(this);
    l.addView(listHolder, new LinearLayout.LayoutParams(-1, 0, 1));
    queue = new ListView(this);
    queue.setDivider(null);
    queue.setCacheColorHint(bg);
    queue.setSelector(Ui.ripple(this, Color.TRANSPARENT, 12));
    adapter = new QueueAdapter();
    queue.setAdapter(adapter);
    listHolder.addView(queue, new FrameLayout.LayoutParams(-1, -1));
    LinearLayout empty = vertical();
    empty.setGravity(Gravity.CENTER);
    mascot = new Ui.SpriteView(this);
    empty.addView(mascot, new LinearLayout.LayoutParams(dp(155), dp(155)));
    TextView emptyTitle = text("Sua próxima faixa mora aqui.", 18, Ui.TEXT);
    emptyTitle.setGravity(Gravity.CENTER);
    empty.addView(emptyTitle);
    gap(empty, 8);
    TextView hint =
        text("Escolha um arquivo ou uma pasta.\nMúsicas e vídeos entram na mesma fila.", 13, muted);
    hint.setGravity(Gravity.CENTER);
    empty.addView(hint);
    listHolder.addView(empty, new FrameLayout.LayoutParams(-1, -1));
    queue.setEmptyView(empty);
    queue.setOnItemClickListener((p, v, n, id) -> {
      if (service != null) {
        service.select(visibleTracks.get(n));
        switchTab(0);
      }
    });
    queue.setOnItemLongClickListener((p, v, n, id) -> {
      if (service == null)
        return true;
      int at = visibleTracks.get(n);
      new AlertDialog.Builder(this)
          .setTitle(service.tracks.get(at).name)
          .setItems(new String[] {"Reproduzir agora", "Remover da lista"},
              (d, w) -> {
                if (service == null)
                  return;
                if (w == 0)
                  service.select(at);
                else
                  service.removeTrack(at);
              })
          .show();
      return true;
    });
    libraryPage = l;
    pageViews.add(l);
  }
  void buildRoom() {
    roomPage = padded();
    section(roomPage, "OUVIR JUNTO");
    LinearLayout heading = row();
    TextView t = text("A mesma frequência.", 24, Ui.TEXT);
    t.setTypeface(Ui.display(this));
    heading.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
    Ui.SpriteView little = new Ui.SpriteView(this);
    heading.addView(little, new LinearLayout.LayoutParams(dp(76), dp(76)));
    roomPage.addView(heading);
    TextView desc =
        text("Crie uma sala e mande o link. Quem entrar pode ouvir, assistir e conversar com você.",
            13, muted);
    desc.setLineSpacing(dp(3), 1);
    roomPage.addView(desc);
    gap(roomPage, 18);
    LinearLayout roomCard = vertical();
    roomCard.setPadding(dp(16), dp(14), dp(16), dp(14));
    roomCard.setBackground(shape(panel, Ui.LINE));
    status = text("Sala fechada", 13, lime);
    roomCard.addView(status);
    gap(roomCard, 8);
    link = text("Seu link aparece aqui quando a sala ficar online.", 12, muted);
    link.setTextIsSelectable(true);
    roomCard.addView(link);
    gap(roomCard, 14);
    LinearLayout actions = row();
    weight(actions, button("Criar / encerrar", true, () -> {
      if (service != null) {
        if (service.room)
          service.closeRoom();
        else
          service.openRoom();
      }
    }));
    weight(actions, button("Convidar", false, this::share));
    roomCard.addView(actions);
    gap(roomCard, 8);
    LinearLayout more = row();
    weight(more, button("Copiar link", false, this::copyLink));
    weight(more, button("Entrar por link", false, this::join));
    roomCard.addView(more);
    roomPage.addView(roomCard);
    gap(roomPage, 18);
    LinearLayout chatTitle = row();
    chatTitle.addView(Ui.label(this, "CONVERSA DA SALA"), new LinearLayout.LayoutParams(0, -2, 1));
    chatTitle.addView(
        button("Seu nome", false, this::name), new LinearLayout.LayoutParams(-2, dp(36)));
    roomPage.addView(chatTitle);
    gap(roomPage, 10);
    ScrollView chatScroll = new ScrollView(this);
    chatRows = vertical();
    chatScroll.addView(chatRows);
    roomPage.addView(chatScroll, new LinearLayout.LayoutParams(-1, dp(240)));
    gap(roomPage, 8);
    LinearLayout compose = row();
    compose.setPadding(dp(12), dp(4), dp(4), dp(4));
    compose.setBackground(Ui.shape(this, panel, 18));
    message = new EditText(this);
    message.setTextColor(Ui.TEXT);
    message.setHintTextColor(muted);
    message.setHint("Enviar uma mensagem…");
    message.setTextSize(14);
    message.setMaxLines(3);
    message.setBackgroundColor(Color.TRANSPARENT);
    compose.addView(message, new LinearLayout.LayoutParams(0, -2, 1));
    ImageView send = Ui.iconButton(this, "forward", lime, () -> {
      if (service == null || !service.room) {
        toast("Crie uma sala para conversar.");
        return;
      }
      String value = message.getText().toString().trim();
      if (!value.isEmpty()) {
        service.chat(nickname, value);
        message.setText("");
      }
    });
    send.setContentDescription("Enviar mensagem");
    compose.addView(send, new LinearLayout.LayoutParams(dp(44), dp(44)));
    roomPage.addView(compose);
    pageViews.add(scroller(roomPage));
  }
  void buildWeb() {
    webPage = padded();
    section(webPage, "EXPLORAR");
    Ui.SpriteView sprite = new Ui.SpriteView(this);
    webPage.addView(sprite, new LinearLayout.LayoutParams(-1, dp(205)));
    TextView t = text("O mundo também dá play.", 27, Ui.TEXT);
    t.setTypeface(Ui.display(this));
    webPage.addView(t);
    gap(webPage, 10);
    TextView desc = text(
        "Abra sites, descubra rádios e guarde seus favoritos. Tudo no RetroPlayer.", 15, muted);
    desc.setLineSpacing(dp(5), 1);
    webPage.addView(desc);
    gap(webPage, 24);
    webPage.addView(button("Abrir navegador", true, this::openBrowser));
    gap(webPage, 12);
    webPage.addView(button("Abrir um link de áudio ou vídeo", false, this::remote));
    gap(webPage, 24);
    webSummary = text("Seu áudio continua quando você minimiza o player.", 13, lime);
    webSummary.setLineSpacing(dp(4), 1);
    webPage.addView(webSummary);
    pageViews.add(scroller(webPage));
  }
  void switchTab(int n) {
    tab = n;
    for (int i = 0; i < pageViews.size(); i++)
      pageViews.get(i).setVisibility(i == n ? View.VISIBLE : View.GONE);
    if (mini != null)
      mini.setVisibility(n == 0 ? View.GONE : View.VISIBLE);
    String[] icons = {"headphones", "library", "room", "web"};
    for (int i = 0; i < navItems.size(); i++) {
      LinearLayout item = navItems.get(i);
      ((ImageView) item.getChildAt(0))
          .setImageDrawable(new Ui.Glyph(icons[i], i == n ? lime : muted));
      ((TextView) item.getChildAt(1)).setTextColor(i == n ? lime : muted);
    }
    if (n == 1)
      filterQueue();
  }
  void updateModeIcons() {
    if (service == null)
      return;
    shuffle.setImageDrawable(new Ui.Glyph("shuffle", service.shuffle ? lime : muted));
    repeat.setImageDrawable(new Ui.Glyph("repeat", service.repeat ? lime : muted));
  }
  void addMenu() {
    new AlertDialog.Builder(this)
        .setTitle("Adicionar à reprodução")
        .setItems(new String[] {"Selecionar um arquivo", "Selecionar vários arquivos",
                      "Escolher uma pasta e subpastas", "Abrir link de mídia"},
            (d, n) -> {
              if (n == 0)
                pick(false);
              if (n == 1)
                pick(true);
              if (n == 2)
                pickFolder();
              if (n == 3)
                remote();
            })
        .show();
  }
  void menu() {
    new AlertDialog.Builder(this)
        .setTitle("RetroPlayer")
        .setItems(new String[] {"Adicionar mídia", "Equalizador", "Navegador e favoritos",
                      "Limpar lista de reprodução", "Sobre o RetroPlayer"},
            (d, n) -> {
              if (n == 0)
                addMenu();
              if (n == 1)
                equalizer();
              if (n == 2)
                openBrowser();
              if (n == 3)
                clearQueue();
              if (n == 4)
                new AlertDialog.Builder(this)
                    .setTitle("RetroPlayer 1.2")
                    .setMessage(
                        "Música, vídeo e companhia.\n\nPlayer nativo para Android com pastas, "
                        + "favoritos, áudio em segundo plano, salas por link e chat.\n\nA "
                        + "reprodução de sites protegidos depende das regras de cada site.")
                    .setPositiveButton("Pronto", null)
                    .show();
            })
        .show();
  }
  void pick(boolean multiple) {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.setType("*/*");
    i.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"audio/*", "video/*"});
    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    startActivityForResult(i, 7);
  }
  void pickFolder() {
    if (service != null && service.importing) {
      toast("Uma pasta já está sendo importada.");
      switchTab(1);
      return;
    }
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
    startActivityForResult(i, 8);
  }
  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null)
      return;
    service = PlayerService.instance;
    if (service == null) {
      toast("O player está reiniciando. Tente novamente.");
      return;
    }
    if (request == 8 && data.getData() != null) {
      Uri tree = data.getData();
      persist(tree);
      service.importFolder(tree);
      switchTab(1);
    }
    if (request == 7) {
      if (data.getClipData() != null) {
        for (int i = 0; i < data.getClipData().getItemCount(); i++) {
          Uri uri = data.getClipData().getItemAt(i).getUri();
          persist(uri);
          service.add(uri);
        }
      } else if (data.getData() != null) {
        persist(data.getData());
        service.add(data.getData());
      }
      switchTab(1);
    }
  }
  void persist(Uri uri) {
    try {
      getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (SecurityException e) {
      toast("O acesso a este arquivo pode precisar ser autorizado novamente.");
    }
  }
  void clearQueue() {
    new AlertDialog.Builder(this)
        .setTitle("Limpar lista?")
        .setMessage("Os arquivos continuam no celular.")
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Limpar",
            (d, w) -> {
              if (service != null)
                service.clearQueue();
            })
        .show();
  }
  void copyLink() {
    if (service != null && !service.url.isEmpty()
        && service.tunnelStatus.startsWith("Sala online")) {
      ((android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
          .setPrimaryClip(ClipData.newPlainText("RetroPlayer", service.url));
      toast("Link copiado.");
    } else
      toast("Aguarde a sala ficar online.");
  }
  void openBrowser() {
    startActivity(new Intent(this, BrowserActivity.class));
  }
  void remote() {
    EditText input = new EditText(this);
    input.setHint("https://site.com/musica.mp3");
    input.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
    new AlertDialog.Builder(this)
        .setTitle("Abrir mídia da internet")
        .setMessage("Cole um link HTTPS direto de áudio ou vídeo.")
        .setView(input)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Tocar",
            (d, w) -> {
              Uri u = Uri.parse(input.getText().toString().trim());
              if (!"https".equals(u.getScheme()) || u.getHost() == null
                  || u.getUserInfo() != null) {
                toast("Use um link HTTPS direto válido.");
                return;
              }
              if (service != null) {
                service.addRemote(u.toString());
                switchTab(0);
              }
            })
        .show();
  }
  void filterQueue() {
    visibleTracks.clear();
    if (service != null)
      for (int i = 0; i < service.tracks.size(); i++)
        if (query.isEmpty() || service.tracks.get(i).name.toLowerCase(Locale.ROOT).contains(query))
          visibleTracks.add(i);
    if (adapter != null)
      adapter.notifyDataSetChanged();
  }
  class QueueAdapter extends BaseAdapter {
    public int getCount() {
      return visibleTracks.size();
    }
    public Object getItem(int n) {
      return service.tracks.get(visibleTracks.get(n));
    }
    public long getItemId(int n) {
      return visibleTracks.get(n);
    }
    public View getView(int n, View convert, ViewGroup parent) {
      LinearLayout item;
      if (convert == null) {
        item = row();
        item.setPadding(dp(8), dp(9), dp(8), dp(9));
        item.addView(Ui.icon(MainActivity.this, "music", muted, 22),
            new LinearLayout.LayoutParams(dp(46), dp(48)));
        LinearLayout labels = vertical();
        TextView name = text("", 14, Ui.TEXT);
        name.setMaxLines(1);
        name.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(name);
        gap(labels, 4);
        labels.addView(text("", 11, muted));
        item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView more = Ui.icon(MainActivity.this, "more", muted, 18);
        item.addView(more, new LinearLayout.LayoutParams(dp(32), dp(40)));
      } else
        item = (LinearLayout) convert;
      int at = visibleTracks.get(n);
      PlayerService.Track track = service.tracks.get(at);
      boolean current = at == service.index;
      LinearLayout labels = (LinearLayout) item.getChildAt(1);
      TextView name = (TextView) labels.getChildAt(0);
      name.setText(track.name);
      name.setTextColor(current ? lime : Ui.TEXT);
      TextView info = (TextView) labels.getChildAt(2);
      String origin = "https".equals(track.uri.getScheme()) ? "Internet"
          : track.mime.startsWith("video/")                   ? "Vídeo"
                                                              : "Áudio";
      info.setText(origin + "  •  " + String.format(Locale.getDefault(), "%02d", at + 1));
      item.setBackground(Ui.shape(MainActivity.this, current ? Ui.PANEL : bg, 8));
      ((ImageView) item.getChildAt(0))
          .setImageDrawable(new Ui.Glyph(current    ? "headphones"
                  : track.mime.startsWith("video/") ? "play"
                                                    : "music",
              current ? lime : muted));
      item.getChildAt(2).setOnClickListener(v
          -> new AlertDialog.Builder(MainActivity.this)
              .setTitle(track.name)
              .setItems(new String[] {"Reproduzir", "Remover da lista"},
                  (d, w) -> {
                    if (w == 0) {
                      service.select(at);
                      switchTab(0);
                    } else
                      service.removeTrack(at);
                  })
              .show());
      return item;
    }
  }
  void share() {
    if (service == null || service.url.isEmpty()
        || !service.tunnelStatus.startsWith("Sala online")) {
      toast("Aguarde a sala ficar online.");
      return;
    }
    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType("text/plain");
    i.putExtra(
        Intent.EXTRA_TEXT, "Ouça comigo no RetroPlayer! Música, vídeo e chat: " + service.url);
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
  final Runnable refresh = new Runnable() {
    public void run() {
      if (!active)
        return;
      service = PlayerService.instance;
      if (service != null) {
        try {
          JSONObject s = new JSONObject(service.state);
          boolean has = service.index >= 0;
          String name = has ? service.tracks.get(service.index).name : "PRONTO PARA O PLAY";
          title.setText(name);
          artist.setText(has ? ("https".equals(service.tracks.get(service.index).uri.getScheme())
                                       ? "Streaming da internet"
                                       : "Sua biblioteca • " + (service.index + 1) + " de "
                                           + service.tracks.size())
                             : "Adicione uma música, um vídeo ou uma pasta");
          boolean playing = s.optBoolean("playing");
          headerSubtitle.setText(
              service.room ? "CYBRDECK // SALA ABERTA" : "CYBRDECK // OPERADOR LOCAL");
          int pos = (int) (s.optDouble("position") * 1000),
              duration = (int) (s.optDouble("duration") * 1000);
          if (!dragging) {
            elapsed.setText(clock(pos / 1000));
            progress.setMax(duration);
            progress.setProgress(pos);
          }
          trace.update(duration > 0 ? (float) pos / duration : 0);
          playbackState.setText((playing      ? "REPRODUZINDO"
                                        : has ? "PAUSADO"
                                              : "AGUARDANDO")
              + " // "
              + (duration > 0 ? Math.min(100, (int) (100L * pos / duration)) + "% REPRODUZIDO"
                              : "CANAL DE ÁUDIO"));
          progress.setEnabled(duration > 0 && service.ready);
          total.setText(duration > 0 ? clock(duration / 1000) : has ? "AO VIVO" : "0:00");
          play.setImageDrawable(new Ui.Glyph(playing ? "pause" : "play", bg));
          boolean isVideo = has
              && (service.tracks.get(service.index).mime.startsWith("video/")
                  || (service.ready && service.player.getVideoHeight() > 0));
          video.setVisibility(isVideo ? View.VISIBLE : View.GONE);
          artwork.setVisibility(View.VISIBLE);
          artFrame.setVisibility(isVideo ? View.VISIBLE : View.GONE);
          meta.setText(isVideo ? "VÍDEO" : "ESTÉREO");
          if (isVideo && service.ready) {
            int vw = service.player.getVideoWidth(), vh = service.player.getVideoHeight();
            if (vw > 0 && vh > 0) {
              int aw = artFrame.getWidth(), ah = artFrame.getHeight();
              float scale = Math.min((float) aw / vw, (float) ah / vh);
              FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) video.getLayoutParams();
              int w = (int) (vw * scale), h = (int) (vh * scale);
              if (lp.width != w || lp.height != h) {
                lp.width = w;
                lp.height = h;
                lp.gravity = Gravity.CENTER;
                video.setLayoutParams(lp);
              }
            }
          }
          boolean webActive = service.browser != null && service.browser.active;
          miniTitle.setText(webActive ? service.browser.pageTitle
                  : has               ? name
                                      : "Sua música está aqui");
          miniSubtitle.setText(webActive ? "Navegador • segundo plano"
                  : has                  ? "Toque para abrir o player"
                                         : "Escolha um arquivo ou uma pasta");
          miniPlay.setImageDrawable(new Ui.Glyph(
              (webActive ? service.browser.playing : playing) ? "pause" : "play", lime));
          status.setText((service.room ? "●  " : "○  ") + service.tunnelStatus);
          link.setText(service.url.isEmpty() ? "Seu link aparece aqui quando a sala ficar online."
                                             : service.url);
          queueTitle.setText(service.tracks.size() + " arquivos na lista de reprodução");
          importStatus.setText(service.importStatus);
          if (lastVersion != service.tracksVersion || lastIndex != service.index) {
            lastVersion = service.tracksVersion;
            lastIndex = service.index;
            filterQueue();
            updateRecent();
          }
          updateModeIcons();
          JSONArray chats = new JSONArray(service.chatJson());
          int id = chats.length() > 0 ? chats.getJSONObject(chats.length() - 1).getInt("id") : 0;
          if (id != lastChat) {
            lastChat = id;
            chatRows.removeAllViews();
            if (chats.length() == 0) {
              TextView hint =
                  text("Sua conversa aparece aqui.\nCrie uma sala para começar.", 13, muted);
              hint.setPadding(dp(6), dp(20), dp(6), 0);
              chatRows.addView(hint);
            }
            for (int i = Math.max(0, chats.length() - 50); i < chats.length(); i++) {
              JSONObject j = chats.getJSONObject(i);
              LinearLayout bubble = vertical();
              bubble.setPadding(dp(12), dp(10), dp(12), dp(10));
              bubble.setBackground(Ui.shape(
                  MainActivity.this, j.getString("name").equals(nickname) ? Ui.RAISED : panel, 14));
              TextView author = text(j.getString("name") + "  ·  "
                      + new java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                          .format(new Date(j.getLong("time"))),
                  11, lime);
              bubble.addView(author);
              gap(bubble, 5);
              bubble.addView(text(j.getString("text"), 14, Ui.TEXT));
              LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
              p.bottomMargin = dp(8);
              chatRows.addView(bubble, p);
            }
            chatRows.post(() -> ((ScrollView) chatRows.getParent()).fullScroll(View.FOCUS_DOWN));
          }
          if (!service.error.isEmpty() && !service.error.equals(lastError)) {
            lastError = service.error;
            toast(lastError);
          } else if (service.error.isEmpty())
            lastError = "";
        } catch (Exception ignored) {
        }
      }
      handler.postDelayed(this, 450);
    }
  };
  @Override
  protected void onResume() {
    super.onResume();
    active = true;
    handler.post(refresh);
    handler.postDelayed(() -> {
      service = PlayerService.instance;
      if (service != null) {
        service.restore();
        if (video.getHolder().getSurface().isValid())
          service.attach(video.getHolder().getSurface());
      }
    }, 300);
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
  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putInt("tab", tab);
    super.onSaveInstanceState(out);
  }
  @Override
  public void onBackPressed() {
    if (tab != 0)
      switchTab(0);
    else
      super.onBackPressed();
  }
}
