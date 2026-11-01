package br.com.retroplayer;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;

public class BrowserActivity extends Activity {
  WebView web;
  EditText address;
  TextView status;
  ProgressBar progress;
  FrameLayout holder;
  View fullscreen;
  WebChromeClient.CustomViewCallback fullscreenCallback;
  int dark = Color.rgb(14, 16, 23), lime = Color.rgb(201, 255, 102);
  int dp(int n) {
    return (int) (n * getResources().getDisplayMetrics().density);
  }
  Button button(String label, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setAllCaps(false);
    b.setTextSize(11);
    b.setTextColor(lime);
    b.setOnClickListener(v -> action.run());
    return b;
  }
  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    getWindow().setStatusBarColor(dark);
    getWindow().setNavigationBarColor(dark);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(dark);
    setContentView(root);
    LinearLayout bar = new LinearLayout(this);
    bar.setPadding(dp(6), 0, dp(6), 0);
    bar.setGravity(Gravity.CENTER_VERTICAL);
    bar.addView(button("‹ App", this::finish));
    address = new EditText(this);
    address.setSingleLine();
    address.setTextSize(13);
    address.setTextColor(Color.WHITE);
    address.setHintTextColor(Color.GRAY);
    address.setHint("Site, pesquisa ou link de mídia");
    address.setInputType(17);
    address.setImeOptions(2);
    address.setSelectAllOnFocus(true);
    address.setOnEditorActionListener((v, a, event) -> {
      navigate();
      return true;
    });
    bar.addView(address, new LinearLayout.LayoutParams(0, dp(55), 1));
    bar.addView(button("Ir", this::navigate));
    root.addView(bar);
    progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));
    status = new TextView(this);
    status.setTextColor(lime);
    status.setTextSize(10);
    status.setPadding(dp(12), dp(4), dp(12), dp(4));
    status.setText("Navegador • o som de sites é local; links diretos podem ir para a sala.");
    root.addView(status);
    holder = new FrameLayout(this);
    root.addView(holder, new LinearLayout.LayoutParams(-1, 0, 1));
    web = new WebView(this);
    holder.addView(web, new FrameLayout.LayoutParams(-1, -1));
    web.setBackgroundColor(dark);
    WebSettings settings = web.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    settings.setMediaPlaybackRequiresUserGesture(true);
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);
    settings.setSafeBrowsingEnabled(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
    web.setWebViewClient(new WebViewClient() {
      public void onPageFinished(WebView v, String url) {
        address.setText(url);
      }
      public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        String scheme = r.getUrl().getScheme();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
          Toast
              .makeText(BrowserActivity.this, "Este link precisa de um aplicativo externo.",
                  Toast.LENGTH_SHORT)
              .show();
          return true;
        }
        String path = r.getUrl().getPath();
        if (r.isForMainFrame() && path != null
            && path.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(mp3|mp4|m4a|aac|ogg|wav|webm|m3u8)$")) {
          direct(r.getUrl().toString());
          return true;
        }
        return false;
      }
      public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
        if (r.isForMainFrame())
          status.setText("Não foi possível abrir o site. Verifique o endereço e sua conexão.");
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      public void onProgressChanged(WebView v, int n) {
        progress.setProgress(n);
        progress.setVisibility(n == 100 ? View.GONE : View.VISIBLE);
      }
      public void onShowCustomView(View view, CustomViewCallback callback) {
        if (fullscreen != null) {
          callback.onCustomViewHidden();
          return;
        }
        fullscreen = view;
        fullscreenCallback = callback;
        holder.addView(view, new FrameLayout.LayoutParams(-1, -1));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      }
      public void onHideCustomView() {
        hideFullscreen();
      }
    });
    web.setDownloadListener((url, ua, disposition, mime, length) -> {
      if (mime != null && (mime.startsWith("audio/") || mime.startsWith("video/")))
        direct(url);
      else
        Toast
            .makeText(
                this, "Downloads de sites não são suportados neste navegador.", Toast.LENGTH_LONG)
            .show();
    });
    LinearLayout tools = new LinearLayout(this);
    for (Button b : new Button[] {button("◀",
                                      () -> {
                                        if (web.canGoBack())
                                          web.goBack();
                                      }),
             button("▶",
                 () -> {
                   if (web.canGoForward())
                     web.goForward();
                 }),
             button("Recarregar", () -> web.reload()),
             button("Tocar link", () -> direct(address.getText().toString()))})
      tools.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1));
    root.addView(tools);
    if (saved != null)
      web.restoreState(saved);
    else
      home();
  }
  void home() {
    web.loadDataWithBaseURL("https://retroplayer.invalid/",
        "<!doctype html><meta name='viewport' content='width=device-width'><body "
        + "style='background:#0e1017;color:#c9ff66;font-family:monospace;padding:30px'><h1>ϟ WEB "
        + "RADIO</h1><p style='color:#a0aabc;line-height:1.8'>Digite um site ou faça uma busca na "
        + "barra acima.<br><br>Assista e escute pela internet.<br><br>Para ouvir com a turma, use "
        + "<b>Tocar link</b> com um endereço direto MP3, MP4 ou outro formato "
        + "compatível.<br><br>Vídeos dentro de sites tocam somente aqui. O conteúdo de cada site "
        + "depende da compatibilidade com o navegador.</p>",
        "text/html", "UTF-8", null);
  }
  void navigate() {
    String v = address.getText().toString().trim();
    if (v.isEmpty())
      return;
    if (!v.startsWith("https://") && !v.startsWith("http://"))
      v = v.contains(".") && !v.contains(" ") ? "https://" + v
                                              : "https://www.google.com/search?q=" + Uri.encode(v);
    Uri u = Uri.parse(v);
    if (u.getHost() == null)
      return;
    web.loadUrl(v);
    ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
        .hideSoftInputFromWindow(address.getWindowToken(), 0);
    web.requestFocus();
  }
  void direct(String initial) {
    EditText e = new EditText(this);
    e.setText(initial.startsWith("https://retroplayer.invalid") ? "" : initial);
    e.setHint("https://site.com/musica.mp3");
    e.setInputType(17);
    new AlertDialog.Builder(this)
        .setTitle("Abrir link no player")
        .setMessage("Use o endereço direto do áudio ou vídeo (HTTPS). Páginas de sites e conteúdo "
                    + "protegido não funcionam como uma faixa compartilhada.")
        .setView(e)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Tocar",
            (d, w) -> {
              String url = e.getText().toString().trim();
              Uri u = Uri.parse(url);
              if (!"https".equals(u.getScheme()) || u.getHost() == null
                  || u.getUserInfo() != null) {
                Toast.makeText(this, "Informe um link HTTPS direto válido.", Toast.LENGTH_LONG)
                    .show();
                return;
              }
              PlayerService s = PlayerService.instance;
              if (s != null) {
                web.loadUrl("about:blank");
                s.addRemote(url);
                finish();
              } else
                Toast.makeText(this, "Abra o player novamente.", Toast.LENGTH_LONG).show();
            })
        .show();
  }
  void hideFullscreen() {
    if (fullscreen != null) {
      holder.removeView(fullscreen);
      fullscreen = null;
      if (fullscreenCallback != null)
        fullscreenCallback.onCustomViewHidden();
      fullscreenCallback = null;
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
  }
  @Override
  public void onBackPressed() {
    if (fullscreen != null)
      hideFullscreen();
    else if (web.canGoBack())
      web.goBack();
    else
      super.onBackPressed();
  }
  @Override
  protected void onPause() {
    web.onPause();
    super.onPause();
  }
  @Override
  protected void onResume() {
    super.onResume();
    if (web != null)
      web.onResume();
  }
  @Override
  protected void onSaveInstanceState(Bundle b) {
    web.saveState(b);
    super.onSaveInstanceState(b);
  }
  @Override
  protected void onDestroy() {
    hideFullscreen();
    holder.removeView(web);
    web.destroy();
    super.onDestroy();
  }
}
