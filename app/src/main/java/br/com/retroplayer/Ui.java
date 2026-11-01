package br.com.retroplayer;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

final class Ui {
  static final int BG = 0xff201617, PANEL = 0xff3d2729, RAISED = 0xff492e30, LINE = 0xff6b494b,
                   TEXT = 0xff98e3e6, MUTED = 0xffc6a19c, LIME = 0xff98e3e6, ORANGE = 0xffe75c50;
  static Typeface monoFont, displayFont;
  static Typeface mono(Context c) {
    if (monoFont == null)
      monoFont = Typeface.createFromAsset(c.getApplicationContext().getAssets(), "theme/mono.ttf");
    return monoFont;
  }
  static Typeface display(Context c) {
    if (displayFont == null)
      displayFont =
          Typeface.createFromAsset(c.getApplicationContext().getAssets(), "theme/display.ttf");
    return displayFont;
  }
  static int dp(Context c, float n) {
    return (int) (n * c.getResources().getDisplayMetrics().density + .5f);
  }
  static GradientDrawable shape(Context c, int color, int radius) {
    GradientDrawable d = new DeckDrawable(c, color, Math.min(10, Math.max(4, radius / 2)));
    return d;
  }
  static android.graphics.drawable.Drawable ripple(Context c, int color, int radius) {
    return new RippleDrawable(
        android.content.res.ColorStateList.valueOf(0x33ffffff), shape(c, color, radius), null);
  }
  static TextView text(Context c, String s, int size, int color) {
    TextView t = new TextView(c);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setFontFeatureSettings("kern");
    t.setTypeface(size >= 18 ? display(c) : mono(c));
    return t;
  }
  static TextView label(Context c, String s) {
    TextView t = text(c, s, 11, MUTED);
    t.setLetterSpacing(.13f);
    t.setTypeface(mono(c));
    return t;
  }
  static LinearLayout column(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }
  static LinearLayout row(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }
  static void gap(Context c, LinearLayout l, int n) {
    l.addView(new View(c), new LinearLayout.LayoutParams(1, dp(c, n)));
  }
  static Button button(Context c, String s, boolean primary, Runnable action) {
    Button b = new Button(c);
    b.setAllCaps(true);
    b.setText(s);
    b.setTextSize(17);
    b.setTypeface(display(c));
    b.setLetterSpacing(.06f);
    b.setTextColor(primary ? BG : TEXT);
    b.setBackground(ripple(c, primary ? ORANGE : PANEL, 14));
    b.setMinHeight(dp(c, 48));
    b.setMinimumHeight(dp(c, 48));
    b.setPadding(dp(c, 14), 0, dp(c, 14), 0);
    b.setStateListAnimator(null);
    b.setOnClickListener(v -> action.run());
    return b;
  }
  static ImageView icon(Context c, String kind, int color, int size) {
    ImageView v = new ImageView(c);
    v.setImageDrawable(new Glyph(kind, color));
    int p = dp(c, (48 - size) / 2f);
    v.setPadding(p, p, p, p);
    return v;
  }
  static ImageView iconButton(Context c, String kind, int color, Runnable action) {
    ImageView v = icon(c, kind, color, 23);
    v.setBackground(ripple(c, Color.TRANSPARENT, 24));
    v.setOnClickListener(w -> action.run());
    v.setContentDescription(kind);
    return v;
  }
  static final class DeckDrawable extends GradientDrawable {
    final Paint edge = new Paint(3);
    final Path path = new Path();
    final int cut;
    int border;
    DeckDrawable(Context c, int fill, int cut) {
      this.cut = dp(c, cut);
      setColor(fill);
      border = fill == Color.TRANSPARENT || fill == BG ? Color.TRANSPARENT : LINE;
    }
    @Override
    public void setStroke(int width, int color) {
      border = color;
      invalidateSelf();
    }
    void outlinePath() {
      Rect r = getBounds();
      float k = Math.min(cut, Math.min(r.width(), r.height()) / 3f);
      path.reset();
      path.moveTo(r.left + k, r.top + 1);
      path.lineTo(r.right - 1, r.top + 1);
      path.lineTo(r.right - 1, r.bottom - k);
      path.lineTo(r.right - k, r.bottom - 1);
      path.lineTo(r.left + 1, r.bottom - 1);
      path.lineTo(r.left + 1, r.top + k);
      path.close();
    }
    @Override
    public void draw(Canvas c) {
      outlinePath();
      int save = c.save();
      c.clipPath(path);
      super.draw(c);
      c.restoreToCount(save);
      if (border != Color.TRANSPARENT) {
        edge.setColor(border);
        edge.setStrokeWidth(1.5f);
        edge.setStyle(Paint.Style.STROKE);
        c.drawPath(path, edge);
      }
    }
    @Override
    public void getOutline(Outline outline) {
      if (getBounds().isEmpty()) {
        outline.setEmpty();
        return;
      }
      outlinePath();
      outline.setConvexPath(path);
    }
  }
  static final class BarcodeView extends View {
    final Paint p = new Paint(3);
    BarcodeView(Context c) {
      super(c);
      setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override
    protected void onDraw(Canvas canvas) {
      p.setColor(LIME);
      float unit = getWidth() / 53f;
      int x = 0;
      for (int i = 0; i < 22; i++) {
        int bar = (i * 7 + 3) % 4 + 1;
        canvas.drawRect(x * unit, 0, (x + bar) * unit, getHeight(), p);
        x += bar + 1;
      }
    }
  }
  static final class StatusStrip extends View {
    final Paint p = new Paint(3);
    StatusStrip(Context c) {
      super(c);
      p.setTypeface(mono(c));
      setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override
    protected void onDraw(Canvas canvas) {
      p.setTextSize(dp(getContext(), 10));
      p.setColor(LIME);
      int battery =
          ((android.os.BatteryManager) getContext().getSystemService(Context.BATTERY_SERVICE))
              .getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
      String right = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                         .format(new java.util.Date())
          + (battery >= 0 && battery <= 100 ? "  BAT " + battery + "%" : "");
      canvas.drawText("RP//DECK", 0, getHeight() * .65f, p);
      float width = p.measureText(right);
      canvas.drawText(right, getWidth() - width, getHeight() * .65f, p);
      p.setColor(MUTED);
      float end = getWidth() - width - dp(getContext(), 14);
      for (float x = dp(getContext(), 85); x < end; x += dp(getContext(), 10))
        canvas.drawLine(x, getHeight() * .35f, x, getHeight() * .58f, p);
      if (isShown())
        postInvalidateDelayed(30000);
    }
  }
  static final class ProgressTrace extends View {
    final Paint p = new Paint(3);
    final Path wave = new Path();
    float fraction;
    ProgressTrace(Context c) {
      super(c);
      setContentDescription("Progresso da reprodução");
    }
    void update(float f) {
      fraction = Math.max(0, Math.min(1, f));
      invalidate();
    }
    @Override
    protected void onDraw(Canvas c) {
      float w = getWidth(), h = getHeight(), mid = h * .5f;
      wave.reset();
      wave.moveTo(0, mid);
      for (int x = 0; x <= getWidth(); x += 3) {
        float u = (x % 76) / 76f;
        float v = u > .37f && u < .44f ? -h * .26f
            : u > .44f && u < .49f     ? h * .32f
            : u > .49f && u < .56f     ? -h * .14f
                                       : 0;
        wave.lineTo(x, mid + v);
      }
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(dp(getContext(), 1));
      p.setColor(LINE);
      c.drawPath(wave, p);
      int save = c.save();
      c.clipRect(0, 0, w * fraction, h);
      p.setColor(ORANGE);
      p.setStrokeWidth(dp(getContext(), 2));
      c.drawPath(wave, p);
      c.restoreToCount(save);
      p.setColor(LIME);
      c.drawLine(w * fraction, 4, w * fraction, h - 4, p);
      p.setStyle(Paint.Style.FILL);
      c.drawRect(w * fraction - dp(getContext(), 2), mid - dp(getContext(), 5),
          w * fraction + dp(getContext(), 2), mid + dp(getContext(), 5), p);
    }
  }
  static final class Glyph extends Drawable {
    final String type;
    final Paint p = new Paint(3);
    Glyph(String type, int color) {
      this.type = type;
      p.setColor(color);
      p.setStrokeWidth(1.9f);
      p.setStrokeCap(Paint.Cap.ROUND);
      p.setStrokeJoin(Paint.Join.ROUND);
    }
    public void draw(Canvas canvas) {
      android.graphics.Rect r = getBounds();
      canvas.save();
      canvas.translate(r.left, r.top);
      canvas.scale(r.width() / 24f, r.height() / 24f);
      p.setStyle(Paint.Style.STROKE);
      Path path = new Path();
      switch (type) {
        case "play":
          p.setStyle(Paint.Style.FILL);
          path.moveTo(8, 5);
          path.lineTo(20, 12);
          path.lineTo(8, 19);
          path.close();
          canvas.drawPath(path, p);
          break;
        case "pause":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(6, 5, 10, 19, 1, 1, p);
          canvas.drawRoundRect(14, 5, 18, 19, 1, 1, p);
          break;
        case "next":
        case "previous":
          if (type.equals("previous")) {
            canvas.translate(24, 0);
            canvas.scale(-1, 1);
          }
          p.setStyle(Paint.Style.FILL);
          path.moveTo(5, 5);
          path.lineTo(16, 12);
          path.lineTo(5, 19);
          path.close();
          canvas.drawPath(path, p);
          canvas.drawRoundRect(17, 5, 20, 19, 1, 1, p);
          break;
        case "folder":
          path.moveTo(3, 7);
          path.lineTo(3, 19);
          path.lineTo(21, 19);
          path.lineTo(21, 7);
          path.lineTo(12, 7);
          path.lineTo(10, 4);
          path.lineTo(3, 4);
          path.close();
          canvas.drawPath(path, p);
          break;
        case "library":
          canvas.drawRoundRect(3, 4, 8, 20, 1, 1, p);
          canvas.drawLine(12, 4, 12, 20, p);
          canvas.drawLine(17, 4, 21, 20, p);
          break;
        case "web":
          canvas.drawCircle(12, 12, 9, p);
          canvas.drawOval(8, 3, 16, 21, p);
          canvas.drawLine(3, 12, 21, 12, p);
          break;
        case "room":
          canvas.drawRoundRect(3, 4, 21, 18, 4, 4, p);
          path.moveTo(7, 18);
          path.lineTo(6, 22);
          path.lineTo(12, 18);
          canvas.drawPath(path, p);
          canvas.drawCircle(8, 11, .5f, p);
          canvas.drawCircle(12, 11, .5f, p);
          canvas.drawCircle(16, 11, .5f, p);
          break;
        case "headphones":
          canvas.drawArc(4, 3, 20, 21, 180, 180, false, p);
          canvas.drawRoundRect(3, 11, 7, 20, 2, 2, p);
          canvas.drawRoundRect(17, 11, 21, 20, 2, 2, p);
          break;
        case "star":
        case "starfill":
          if (type.equals("starfill"))
            p.setStyle(Paint.Style.FILL);
          for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float radius = i % 2 == 0 ? 10 : 4.5f;
            float x = 12 + (float) Math.cos(a) * radius, y = 12 + (float) Math.sin(a) * radius;
            if (i == 0)
              path.moveTo(x, y);
            else
              path.lineTo(x, y);
          }
          path.close();
          canvas.drawPath(path, p);
          break;
        case "back":
          canvas.drawLine(15, 5, 8, 12, p);
          canvas.drawLine(8, 12, 15, 19, p);
          break;
        case "forward":
          canvas.drawLine(9, 5, 16, 12, p);
          canvas.drawLine(16, 12, 9, 19, p);
          break;
        case "plus":
          canvas.drawLine(12, 5, 12, 19, p);
          canvas.drawLine(5, 12, 19, 12, p);
          break;
        case "close":
          canvas.drawLine(6, 6, 18, 18, p);
          canvas.drawLine(6, 18, 18, 6, p);
          break;
        case "search":
          canvas.drawCircle(10, 10, 6, p);
          canvas.drawLine(15, 15, 21, 21, p);
          break;
        case "equalizer":
          for (int i = 0; i < 3; i++) {
            float x = 5 + i * 7;
            canvas.drawLine(x, 3, x, 21, p);
            canvas.drawRoundRect(x - 2, i == 1 ? 6 : 13, x + 2, i == 1 ? 10 : 17, 1, 1, p);
          }
          break;
        case "shuffle":
          path.moveTo(3, 6);
          path.cubicTo(10, 6, 14, 18, 21, 18);
          path.moveTo(3, 18);
          path.cubicTo(10, 18, 14, 6, 21, 6);
          path.moveTo(18, 3);
          path.lineTo(21, 6);
          path.lineTo(18, 9);
          path.moveTo(18, 15);
          path.lineTo(21, 18);
          path.lineTo(18, 21);
          canvas.drawPath(path, p);
          break;
        case "repeat":
          path.moveTo(18, 4);
          path.lineTo(21, 7);
          path.lineTo(18, 10);
          path.moveTo(21, 7);
          path.lineTo(7, 7);
          path.cubicTo(3, 7, 3, 12, 3, 12);
          path.moveTo(6, 20);
          path.lineTo(3, 17);
          path.lineTo(6, 14);
          path.moveTo(3, 17);
          path.lineTo(17, 17);
          path.cubicTo(21, 17, 21, 12, 21, 12);
          canvas.drawPath(path, p);
          break;
        case "more":
          p.setStyle(Paint.Style.FILL);
          for (int i = 0; i < 3; i++) canvas.drawCircle(12, 5 + i * 7, 1.6f, p);
          break;
        case "refresh":
          canvas.drawArc(4, 4, 20, 20, 35, 300, false, p);
          path.moveTo(21, 3);
          path.lineTo(21, 10);
          path.lineTo(14, 10);
          canvas.drawPath(path, p);
          break;
        case "music":
          canvas.drawLine(10, 17, 10, 5, p);
          canvas.drawLine(10, 5, 20, 3, p);
          canvas.drawLine(20, 3, 20, 15, p);
          canvas.drawOval(4, 16, 10, 20, p);
          canvas.drawOval(14, 14, 20, 18, p);
          break;
        default:
          canvas.drawCircle(12, 12, 8, p);
      }
      canvas.restore();
    }
    public void setAlpha(int a) {
      p.setAlpha(a);
    }
    public void setColorFilter(ColorFilter f) {
      p.setColorFilter(f);
    }
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }
  static final class SpriteView extends View {
    static Bitmap cachedSheet;
    final Bitmap sheet;
    final Rect src = new Rect();
    final RectF dst = new RectF();
    final Paint paint = new Paint(3);
    boolean animate = true;
    SpriteView(Context c) {
      super(c);
      BitmapFactory.Options opts = new BitmapFactory.Options();
      opts.inSampleSize = 2;
      if (cachedSheet == null)
        cachedSheet =
            BitmapFactory.decodeResource(getResources(), R.drawable.cassette_sprites, opts);
      sheet = cachedSheet;
      setContentDescription("Mascote fita cassete do RetroPlayer");
    }
    protected void onDraw(Canvas c) {
      if (sheet == null)
        return;
      int f = animate ? (int) (android.os.SystemClock.uptimeMillis() / 420 % 4) : 0;
      int w = sheet.getWidth() / 2, h = sheet.getHeight() / 2;
      float side = Math.min(getWidth(), getHeight());
      src.set((f % 2) * w, (f / 2) * h, (f % 2 + 1) * w, (f / 2 + 1) * h);
      dst.set((getWidth() - side) / 2, (getHeight() - side) / 2, (getWidth() + side) / 2,
          (getHeight() + side) / 2);
      c.drawBitmap(sheet, src, dst, paint);
      if (animate && isShown())
        postInvalidateDelayed(100);
    }
  }
}
