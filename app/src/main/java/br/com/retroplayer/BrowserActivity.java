package br.com.retroplayer;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import java.util.*;

public class BrowserActivity extends Activity {
  BrowserSession session;
  WebView web;
  EditText address;
  TextView status;
  ProgressBar progress;
  FrameLayout holder;
  ScrollView home;
  ImageView star;
  View fullscreen;
  WebChromeClient.CustomViewCallback fullscreenCallback;
  boolean resumed;
  int dp(int n) {
    return Ui.dp(this, n);
  }
  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    getWindow().setStatusBarColor(Ui.BG);
    getWindow().setNavigationBarColor(Ui.BG);
    PlayerService s = PlayerService.instance;
    if (s == null) {
      startActivity(new Intent(this, MainActivity.class));
      finish();
      return;
    }
    session = s.browser();
    web = session.web;
    LinearLayout root = Ui.column(this);
    root.setBackgroundColor(Ui.BG);
    setContentView(root);
    LinearLayout header = Ui.row(this);
    header.setPadding(dp(4), dp(6), dp(8), dp(6));
    ImageView close = Ui.iconButton(this, "back", Ui.TEXT, this::finish);
    close.setContentDescription("Voltar ao RetroPlayer");
    header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
    TextView title = Ui.text(this, "Explorar", 21, Ui.TEXT);
    title.setTypeface(Ui.display(this));
    header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    ImageView bookmarks = Ui.iconButton(this, "library", Ui.LIME, this::favorites);
    bookmarks.setContentDescription("Sites favoritos");
    header.addView(bookmarks, new LinearLayout.LayoutParams(dp(48), dp(48)));
    root.addView(header);
    LinearLayout bar = Ui.row(this);
    bar.setBackground(Ui.shape(this, Ui.PANEL, 16));
    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(50));
    bp.setMargins(dp(16), 0, dp(16), dp(8));
    root.addView(bar, bp);
    ImageView search = Ui.icon(this, "search", Ui.MUTED, 21);
    bar.addView(search, new LinearLayout.LayoutParams(dp(44), dp(44)));
    address = new EditText(this);
    address.setTextColor(Ui.TEXT);
    address.setTextSize(14);
    address.setTypeface(Ui.mono(this));
    address.setHintTextColor(Ui.MUTED);
    address.setHint("Pesquisar ou digitar endereço");
    address.setSingleLine();
    address.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    address.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
    address.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
    address.setSelectAllOnFocus(true);
    address.setOnEditorActionListener((v, a, e) -> {
      navigate();
      return true;
    });
    bar.addView(address, new LinearLayout.LayoutParams(0, -1, 1));
    ImageView go = Ui.iconButton(this, "forward", Ui.LIME, this::navigate);
    go.setContentDescription("Abrir endereço");
    bar.addView(go, new LinearLayout.LayoutParams(dp(44), dp(44)));
    progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.LIME));
    progress.setVisibility(View.GONE);
    root.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));
    status = Ui.text(this, "Áudio em segundo plano ativado", 11, Ui.MUTED);
    status.setPadding(dp(18), dp(4), dp(18), dp(6));
    root.addView(status);
    holder = new FrameLayout(this);
    root.addView(holder, new LinearLayout.LayoutParams(-1, 0, 1));
    session.attach(this, holder);
    home = new ScrollView(this);
    home.setBackgroundColor(Ui.BG);
    holder.addView(home, new FrameLayout.LayoutParams(-1, -1));
    renderHome();
    if (session.pageUrl.isEmpty())
      showHome();
    else {
      home.setVisibility(View.GONE);
      web.setVisibility(View.VISIBLE);
    }
    LinearLayout tools = Ui.row(this);
    tools.setBackgroundColor(Ui.PANEL);
    Object[][] entries = {
        {"back", "Voltar", (Runnable) () -> {if (web.canGoBack()){home.setVisibility(View.GONE);
    web.goBack();
  }
  else showHome();
}
}
, {"forward", "Avançar", (Runnable) () -> {if (web.canGoForward()){home.setVisibility(View.GONE);
web.goForward();
}
}
}
, {"star", "Salvar site nos favoritos", (Runnable) this::saveFavorite},
    {"headphones", "Ouvir no player", (Runnable) () -> direct(address.getText().toString())}, {
  "more", "Opções do navegador", (Runnable) this::options
}
}
;
for (Object[] e : entries) {
  ImageView icon = Ui.iconButton(this, (String) e[0], Ui.TEXT, (Runnable) e[2]);
  icon.setContentDescription((String) e[1]);
  tools.addView(icon, new LinearLayout.LayoutParams(0, dp(56), 1));
  if (e[0].equals("star"))
    star = icon;
}
root.addView(tools);
pageChanged();
}
List<BookmarkStore.Bookmark> bookmarks() {
  return BookmarkStore.read(getSharedPreferences("browser", 0).getString("bookmarks", "[]"));
}
void persist(List<BookmarkStore.Bookmark> list) {
  getSharedPreferences("browser", 0)
      .edit()
      .putString("bookmarks", BookmarkStore.write(list))
      .apply();
}
void renderHome() {
  LinearLayout content = Ui.column(this);
  content.setPadding(dp(24), dp(14), dp(24), dp(24));
  Ui.SpriteView sprite = new Ui.SpriteView(this);
  content.addView(sprite, new LinearLayout.LayoutParams(-1, dp(170)));
  TextView title = Ui.text(this, "A internet no seu ritmo.", 26, Ui.TEXT);
  title.setGravity(Gravity.CENTER);
  title.setTypeface(Ui.display(this));
  content.addView(title);
  Ui.gap(this, content, 10);
  TextView intro = Ui.text(this,
      "Encontre músicas, rádios e vídeos.\nSalve seus sites para voltar com um toque.", 14,
      Ui.MUTED);
  intro.setGravity(Gravity.CENTER);
  intro.setLineSpacing(dp(4), 1);
  content.addView(intro);
  Ui.gap(this, content, 24);
  content.addView(Ui.button(this, "Pesquisar na internet", true, () -> {
    address.requestFocus();
    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
        .showSoftInput(address, InputMethodManager.SHOW_IMPLICIT);
  }));
  Ui.gap(this, content, 26);
  content.addView(Ui.label(this, "SEUS FAVORITOS"));
  Ui.gap(this, content, 12);
  List<BookmarkStore.Bookmark> favorites = bookmarks();
  if (favorites.isEmpty()) {
    TextView hint = Ui.text(this, "Abra um site e toque na estrela para salvar.", 13, Ui.MUTED);
    content.addView(hint);
  } else
    for (BookmarkStore.Bookmark b : favorites) {
      LinearLayout r = favoriteRow(b);
      r.setOnClickListener(v -> open(b.url));
      r.setOnLongClickListener(v -> {
        removeFavorite(b);
        return true;
      });
      content.addView(r);
    }
  Ui.gap(this, content, 22);
  TextView note = Ui.text(this,
      "Links diretos de áudio ou vídeo podem tocar no player. Alguns sites protegidos controlam a "
          + "reprodução em segundo plano.",
      12, Ui.MUTED);
  note.setLineSpacing(dp(3), 1);
  content.addView(note);
  home.removeAllViews();
  home.addView(content);
}
LinearLayout favoriteRow(BookmarkStore.Bookmark b) {
  LinearLayout row = Ui.row(this);
  row.setPadding(dp(8), dp(12), dp(8), dp(12));
  row.setBackground(Ui.ripple(this, Ui.BG, 12));
  ImageView icon = Ui.icon(this, "web", Ui.LIME, 23);
  row.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
  LinearLayout labels = Ui.column(this);
  TextView t = Ui.text(this, b.title, 15, Ui.TEXT);
  t.setMaxLines(1);
  t.setEllipsize(android.text.TextUtils.TruncateAt.END);
  labels.addView(t);
  TextView u = Ui.text(this, Uri.parse(b.url).getHost(), 12, Ui.MUTED);
  labels.addView(u);
  row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
  return row;
}
void showHome() {
  web.setVisibility(View.INVISIBLE);
  home.setVisibility(View.VISIBLE);
  address.setText("");
  renderHome();
}
void open(String url) {
  if (!BookmarkStore.valid(url)) {
    toast("Endereço inválido.");
    return;
  }
  home.setVisibility(View.GONE);
  web.setVisibility(View.VISIBLE);
  session.navigate(url);
  address.clearFocus();
  ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
      .hideSoftInputFromWindow(address.getWindowToken(), 0);
}
void navigate() {
  String value = address.getText().toString().trim();
  if (value.isEmpty())
    return;
  if (!value.startsWith("http://") && !value.startsWith("https://"))
    value = value.contains(".") && !value.contains(" ")
        ? "https://" + value
        : "https://www.google.com/search?q=" + Uri.encode(value);
  open(value);
}
void pageChanged() {
  if (session == null || address == null)
    return;
  if (!address.hasFocus() && home != null && home.getVisibility() != View.VISIBLE)
    address.setText(session.pageUrl);
  if (star != null) {
    boolean saved = false;
    for (BookmarkStore.Bookmark b : bookmarks())
      if (b.url.equals(session.pageUrl))
        saved = true;
    star.setImageDrawable(new Ui.Glyph(saved ? "starfill" : "star", saved ? Ui.LIME : Ui.TEXT));
  }
  setStatus(session.backgroundEnabled ? "Áudio em segundo plano ativado"
                                      : "Áudio em segundo plano desativado");
}
void loading(int value) {
  progress.setVisibility(value == 100 ? View.GONE : View.VISIBLE);
  progress.setProgress(value);
}
void setStatus(String value) {
  status.setText(value);
}
void saveFavorite() {
  String url = session.pageUrl;
  if (!BookmarkStore.valid(url) || home.getVisibility() == View.VISIBLE) {
    toast("Abra um site para salvar nos favoritos.");
    return;
  }
  EditText name = new EditText(this);
  name.setText(session.pageTitle);
  new AlertDialog.Builder(this)
      .setTitle("Salvar nos favoritos")
      .setView(name)
      .setNegativeButton("Cancelar", null)
      .setPositiveButton("Salvar",
          (d, w) -> {
            List<BookmarkStore.Bookmark> list = bookmarks();
            BookmarkStore.save(list, name.getText().toString().trim(), url);
            persist(list);
            pageChanged();
            toast("Site salvo nos favoritos.");
          })
      .show();
}
void removeFavorite(BookmarkStore.Bookmark bookmark) {
  new AlertDialog.Builder(this)
      .setTitle("Remover favorito?")
      .setMessage(bookmark.title)
      .setNegativeButton("Cancelar", null)
      .setPositiveButton("Remover",
          (d, w) -> {
            List<BookmarkStore.Bookmark> list = bookmarks();
            list.removeIf(b -> b.url.equals(bookmark.url));
            persist(list);
            renderHome();
            pageChanged();
          })
      .show();
}
void favorites() {
  List<BookmarkStore.Bookmark> list = bookmarks();
  if (list.isEmpty()) {
    toast("Seus sites favoritos aparecerão aqui.");
    showHome();
    return;
  }
  ListView view = new ListView(this);
  view.setAdapter(new BaseAdapter() {
    public int getCount() {
      return list.size();
    }
    public Object getItem(int n) {
      return list.get(n);
    }
    public long getItemId(int n) {
      return n;
    }
    public View getView(int n, View old, ViewGroup parent) {
      return favoriteRow(list.get(n));
    }
  });
  AlertDialog dialog = new AlertDialog.Builder(this)
                           .setTitle("Sites favoritos")
                           .setView(view)
                           .setNegativeButton("Fechar", null)
                           .create();
  view.setOnItemClickListener((p, v, n, id) -> {
    open(list.get(n).url);
    dialog.dismiss();
  });
  view.setOnItemLongClickListener((p, v, n, id) -> {
    dialog.dismiss();
    removeFavorite(list.get(n));
    return true;
  });
  dialog.show();
}
void options() {
  new AlertDialog.Builder(this)
      .setTitle("Navegador")
      .setItems(new String[] {"Página inicial", "Sites favoritos", "Recarregar página",
                    "Abrir link de mídia no player",
                    session.backgroundEnabled ? "Desativar áudio em segundo plano"
                                              : "Ativar áudio em segundo plano",
                    "Parar áudio do site"},
          (d, n) -> {
            switch (n) {
              case 0:
                showHome();
                break;
              case 1:
                favorites();
                break;
              case 2:
                web.reload();
                break;
              case 3:
                direct(address.getText().toString());
                break;
              case 4:
                session.setBackground(!session.backgroundEnabled);
                pageChanged();
                break;
              case 5:
                session.pauseMedia();
                break;
            }
          })
      .show();
}
void direct(String initial) {
  EditText input = new EditText(this);
  input.setSingleLine();
  input.setText(initial);
  input.setHint("https://site.com/musica.mp3");
  input.setInputType(
      android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
  new AlertDialog.Builder(this)
      .setTitle("Ouvir no player")
      .setMessage("Cole um link HTTPS direto de áudio ou vídeo. O player mantém o som com a tela "
          + "apagada e pode compartilhar a faixa na sala.")
      .setView(input)
      .setNegativeButton("Cancelar", null)
      .setPositiveButton("Tocar",
          (d, w) -> {
            String url = input.getText().toString().trim();
            Uri uri = Uri.parse(url);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null) {
              toast("Use um link HTTPS direto válido.");
              return;
            }
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", web.getSettings().getUserAgentString());
            if (session.pageUrl.startsWith("https://"))
              headers.put("Referer", session.pageUrl);
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null)
              headers.put("Cookie", cookie);
            session.handoffPending = true;
            session.service.addWebMedia(url, null, headers, 0);
            finish();
          })
      .show();
}
void showFullscreen(View view, WebChromeClient.CustomViewCallback callback) {
  if (fullscreen != null) {
    callback.onCustomViewHidden();
    return;
  }
  fullscreen = view;
  fullscreenCallback = callback;
  holder.addView(view, new FrameLayout.LayoutParams(-1, -1));
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
}
void hideFullscreen() {
  if (fullscreen == null)
    return;
  holder.removeView(fullscreen);
  fullscreen = null;
  if (fullscreenCallback != null)
    fullscreenCallback.onCustomViewHidden();
  fullscreenCallback = null;
  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
}
void toast(String s) {
  Toast.makeText(this, s, Toast.LENGTH_LONG).show();
}
@Override
public void onBackPressed() {
  if (fullscreen != null)
    hideFullscreen();
  else if (home.getVisibility() != View.VISIBLE && web.canGoBack())
    web.goBack();
  else
    finish();
}
@Override
protected void onResume() {
  super.onResume();
  resumed = true;
  if (web != null)
    web.onResume();
}
@Override
protected void onPause() {
  resumed = false;
  super.onPause();
}
@Override
protected void onStop() {
  super.onStop();
  if (session != null)
    session.background();
}
@Override
protected void onDestroy() {
  hideFullscreen();
  if (session != null)
    session.detach(this);
  super.onDestroy();
}
}
