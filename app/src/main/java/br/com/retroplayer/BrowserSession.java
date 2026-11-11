package br.com.retroplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import java.util.*;
import org.json.*;

/** The foreground playback service owns the WebView, rather than a disposable Activity. */
final class BrowserSession {
  final PlayerService service;
  final MutableContextWrapper context;
  final WebView web;
  BrowserActivity host;
  String pageTitle = "Navegador", pageUrl = "";
  boolean active = false, playing = false, backgroundEnabled = true, handoffPending = false,
          destroyed = false;
  double position = 0, duration = 0;
  long ignoreUntil = 0, queuePlayUntil = 0;
  String queueId = "", guestUrl = "";
  boolean queueWasPlaying;
  final Handler handler = new Handler(Looper.getMainLooper());
  @SuppressLint("SetJavaScriptEnabled")
  BrowserSession(PlayerService s) {
    service = s;
    context = new MutableContextWrapper(s);
    web = new WebView(context);
    web.setBackgroundColor(Ui.BG);
    backgroundEnabled = s.getSharedPreferences("browser", 0).getBoolean("background", true);
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
      public void onPageStarted(WebView w, String url, android.graphics.Bitmap icon) {
        pageUrl = url;
        active = false;
        playing = false;
        if (host != null)
          host.pageChanged();
      }
      public void onPageFinished(WebView w, String url) {
        pageUrl = url;
        CookieManager.getInstance().flush();
        if (host != null)
          host.pageChanged();
      }
      public boolean shouldOverrideUrlLoading(WebView w, WebResourceRequest request) {
        Uri u = request.getUrl();
        String scheme = u.getScheme();
        if (!"https".equals(scheme) && !"http".equals(scheme)) {
          if (host != null)
            host.toast("Este link precisa de um aplicativo externo.");
          return true;
        }
        String p = u.getPath();
        if (request.isForMainFrame() && p != null && MediaFiles.isMedia(p, null)) {
          if (host != null)
            host.direct(u.toString());
          return true;
        }
        return false;
      }
      public void onReceivedError(WebView w, WebResourceRequest r, WebResourceError e) {
        if (r.isForMainFrame() && host != null)
          host.setStatus("Não foi possível abrir. Confira o endereço e sua conexão.");
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      public void onReceivedTitle(WebView w, String title) {
        pageTitle = title == null ? "Navegador" : title;
        if (host != null)
          host.pageChanged();
      }
      public void onProgressChanged(WebView w, int value) {
        if (host != null)
          host.loading(value);
      }
      public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        return host != null && host.chooseFile(callback);
      }
      public void onShowCustomView(View view, CustomViewCallback callback) {
        if (host != null)
          host.showFullscreen(view, callback);
        else
          callback.onCustomViewHidden();
      }
      public void onHideCustomView() {
        if (host != null)
          host.hideFullscreen();
      }
    });
    web.setDownloadListener((url, agent, disposition, mime, length) -> {
      if (host != null) {
        if (MediaFiles.isMedia(url, mime))
          host.direct(url);
        else
          host.toast("Este arquivo não é uma mídia compatível.");
      }
    });
    handler.post(poll);
  }
  void attach(BrowserActivity activity, android.widget.FrameLayout container) {
    host = activity;
    context.setBaseContext(activity);
    if (web.getParent() != null)
      ((ViewGroup) web.getParent()).removeView(web);
    container.addView(web, new android.widget.FrameLayout.LayoutParams(-1, -1));
    web.onResume();
    if (!pageUrl.isEmpty())
      container.setVisibility(View.VISIBLE);
  }
  void detach(BrowserActivity activity) {
    if (host != activity)
      return;
    if (web.getParent() != null)
      ((ViewGroup) web.getParent()).removeView(web);
    host = null;
    context.setBaseContext(service);
  }
  void playQueued(String url) {
    queueId = YouTubeLink.id(url);
    queueWasPlaying = false;
    web.getSettings().setAllowContentAccess(false);
    queuePlayUntil = SystemClock.uptimeMillis() + 20000;
    web.getSettings().setMediaPlaybackRequiresUserGesture(false);
    navigate(url);
  }
  void navigate(String url) {
    handoffPending = false;
    web.onResume();
    String agent = WebSettings.getDefaultUserAgent(service);
    if ("music.youtube.com".equalsIgnoreCase(Uri.parse(url).getHost()))
      agent = agent.replaceFirst("\\([^)]*\\)", "(X11; Linux x86_64)")
          .replace(" Mobile ", " ").replace("Version/4.0 ", "");
    web.getSettings().setUserAgentString(agent);
    web.loadUrl(url);
  }
  void setBackground(boolean enabled) {
    backgroundEnabled = enabled;
    service.getSharedPreferences("browser", 0).edit().putBoolean("background", enabled).apply();
  }
  void evaluateMedia(android.webkit.ValueCallback<JSONObject> callback) {
    if (destroyed)
      return;
    String js = "(function(){if(window.retroRoomState)return JSON.stringify(window.retroRoomState);var a=Array.from(document.querySelectorAll('video,audio'));var "
        + "m=a.find(x=>!x.paused&&!x.ended)||a.find(x=>x.currentSrc);if(!m)return "
        + "JSON.stringify({active:false});return "
        + "JSON.stringify({active:true,playing:!m.paused&&!m.ended,position:m.currentTime||"
        + "0,duration:Number.isFinite(m.duration)?m.duration:0,src:m.currentSrc||m.src||'',"
        + "title:document.title,url:location.href,ended:m.ended});})()";
    web.evaluateJavascript(js, raw -> {
      try {
        Object decoded = new JSONTokener(raw).nextValue();
        callback.onReceiveValue(new JSONObject(String.valueOf(decoded)));
      } catch (Exception ignored) {
        callback.onReceiveValue(new JSONObject());
      }
    });
  }
  final Runnable poll = new Runnable() {
    public void run() {
      if (destroyed)
        return;
      evaluateMedia(j -> {
        if (destroyed || SystemClock.uptimeMillis() < ignoreUntil)
          return;
        pageUrl = j.optString("url", pageUrl);
        pageTitle = j.optString("title", pageTitle);
        boolean was = active;
        boolean wasPlaying = playing;
        playing = j.optBoolean("playing");
        active = playing || (active && j.optBoolean("active"));
        position = j.optDouble("position");
        duration = j.optDouble("duration");
        if (!queueId.isEmpty() && queueId.equals(YouTubeLink.id(pageUrl))) {
          if (playing) { queueWasPlaying = true; queuePlayUntil = 0; }
          else if (j.optBoolean("active") && SystemClock.uptimeMillis() < queuePlayUntil) playMedia();
          if(!playing&&queuePlayUntil>0&&SystemClock.uptimeMillis()>=queuePlayUntil){queuePlayUntil=0;service.error="YouTube não iniciou. Abra o YouTube Music no app e toque em reproduzir.";}
          if (playing && service.index >= 0 && service.tracks.get(service.index).mime.equals("video/youtube")) {
            PlayerService.Track track = service.tracks.get(service.index);
            if (!track.name.equals(pageTitle)) { track.name = pageTitle; service.tracksVersion++; service.saveQueue(); }
          }
          if (queueWasPlaying && j.optBoolean("ended")) {
            queueWasPlaying = false;
            if (service.repeat) { seekMedia(0); playMedia(); }
            else if (service.index < service.tracks.size() - 1 || service.shuffle) service.next(1);
          }
        }
        if (playing && service.ready && service.player.isPlaying())
          service.pause();
        if ((active != was || playing != wasPlaying) && !destroyed)
          service.notifyNow();
      });
      handler.postDelayed(this, 1200);
    }
  };
  void background() {
    if (!backgroundEnabled) {
      pauseMedia();
      web.onPause();
      return;
    }
    web.onResume();
    if(host!=null&&host.roomMode)return;
    evaluateMedia(j -> {
      if (destroyed || handoffPending || SystemClock.uptimeMillis() < ignoreUntil
          || !j.optBoolean("playing"))
        return;
      if (!YouTubeLink.id(pageUrl).isEmpty()) return;
      String src = j.optString("src");
      Uri uri = Uri.parse(src);
      // Direct, unprotected HTML media can move to the native service for screen-off playback.
      if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
        return;
      handoffPending = true;
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", web.getSettings().getUserAgentString());
      if (pageUrl.startsWith("https://"))
        headers.put("Referer", pageUrl);
      String cookies = CookieManager.getInstance().getCookie(src);
      if (cookies != null)
        headers.put("Cookie", cookies);
      service.addWebMedia(src, pageTitle, headers, (int) (j.optDouble("position") * 1000));
    });
  }
  void pauseMedia() {
    if (destroyed)
      return;
    ignoreUntil = SystemClock.uptimeMillis() + 1800;
    playing = false;
    web.evaluateJavascript(
        "if(typeof roomPauseAudio==='function')roomPauseAudio();document.querySelectorAll('video,audio').forEach(function(m){m.pause()})", null);
  }
  void playMedia() {
    if (destroyed)
      return;
    web.onResume();
    web.evaluateJavascript("(function(){var "
            + "m=document.querySelector('video,audio');if(typeof roomResumeAudio==='function'){roomResumeAudio();return;}if(m)m.play().catch(function(){})})()",
        null);
  }
  void seekMedia(int position) {
    if (destroyed)
      return;
    double seconds = Math.max(0, position) / 1000.0;
    web.evaluateJavascript("(function(){var "
            + "m=document.querySelector('video,audio');if(m&&Number.isFinite(m."
            + "duration))m.currentTime=Math.min(" + seconds + ",m.duration)})()",
        null);
  }
  void destroy() {
    pauseMedia();
    destroyed = true;
    handler.removeCallbacksAndMessages(null);
    if (web.getParent() != null)
      ((ViewGroup) web.getParent()).removeView(web);
    web.destroy();
    host = null;
    context.setBaseContext(service);
  }
}
