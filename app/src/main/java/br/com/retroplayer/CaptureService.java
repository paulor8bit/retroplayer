package br.com.retroplayer;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import java.util.Arrays;

/** Captures only playback permitted by Android's source-app policy. Never uses the microphone. */
public final class CaptureService extends Service {
  MediaProjection projection;
  AudioRecord recorder;
  volatile boolean recording;
  Thread worker;
  PcmBroadcast broadcast;
  PlayerService owner;
  final Handler main = new Handler(Looper.getMainLooper());
  public IBinder onBind(Intent intent) { return null; }
  @Override public int onStartCommand(Intent intent, int flags, int id) {
    if (intent == null || "stop".equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
    if (recording) return START_NOT_STICKY;
    if (Build.VERSION.SDK_INT < 29) { stopSelf(); return START_NOT_STICKY; }
    try {
      NotificationManager manager = getSystemService(NotificationManager.class);
      manager.createNotificationChannel(new NotificationChannel("capture", "Som de outros apps", NotificationManager.IMPORTANCE_LOW));
      PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
      PendingIntent stop = PendingIntent.getService(this, 10, new Intent(this, CaptureService.class).setAction("stop"), PendingIntent.FLAG_IMMUTABLE);
      startForeground(2, new Notification.Builder(this, "capture").setSmallIcon(R.drawable.icon)
          .setContentTitle("Transmitindo som de outros apps").setContentText("Abra o app de origem. Toque em Parar para encerrar.")
          .setContentIntent(open).setOngoing(true).addAction(android.R.drawable.ic_media_pause, "Parar", stop).build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
      owner = PlayerService.instance;
      if (owner == null || !owner.room) throw new IllegalStateException("Abra uma sala antes de transmitir.");
      Intent grant = intent.getParcelableExtra("grant");
      if (grant == null) throw new IllegalArgumentException("Autorização não recebida");
      projection = getSystemService(MediaProjectionManager.class).getMediaProjection(Activity.RESULT_OK, grant);
      projection.registerCallback(new MediaProjection.Callback() {
        @Override public void onStop() { stopSelf(); }
      }, main);
      if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("Permissão de áudio não concedida");
      AudioFormat format = new AudioFormat.Builder().setSampleRate(PcmBroadcast.SAMPLE_RATE)
          .setChannelMask(AudioFormat.CHANNEL_IN_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build();
      AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(projection)
          .addMatchingUsage(AudioAttributes.USAGE_MEDIA).addMatchingUsage(AudioAttributes.USAGE_GAME)
          .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN).excludeUid(android.os.Process.myUid()).build();
      int min = AudioRecord.getMinBufferSize(PcmBroadcast.SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
      if (min <= 0) throw new IllegalStateException("Formato de captura indisponível neste aparelho");
      recorder = new AudioRecord.Builder().setAudioFormat(format).setBufferSizeInBytes(Math.max(min * 2, 16384))
          .setAudioPlaybackCaptureConfig(config).build();
      if (recorder.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("Captura não inicializada");
      owner.controlPause();
      broadcast = new PcmBroadcast();
      recorder.startRecording();
      if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) throw new IllegalStateException("Captura não autorizada");
      recording = true;
      owner.capture = broadcast;
      owner.captureStatus = "Aguardando áudio. Abra o app de origem e dê play.";
      owner.snapshot(); owner.notifyNow();
      worker = new Thread(this::readAudio, "playback-capture"); worker.start();
    } catch (Exception e) {
      if (PlayerService.instance != null) PlayerService.instance.error = "Não foi possível capturar: " + e.getMessage();
      stopSelf();
    }
    return START_NOT_STICKY;
  }
  void readAudio() {
    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
    byte[] buffer = new byte[4096];
    long lastSignal = SystemClock.elapsedRealtime();
    try {
      while (recording) {
        int count = recorder.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
        if (count < 0) throw new IllegalStateException("Leitura de áudio interrompida (" + count + ")");
        if (count == 0) continue;
        // Stereo PCM frames must remain intact across network chunks.
        count -= count % 4;
        boolean signal = false;
        for (int i = 0; i + 1 < count; i += 2) if (Math.abs((short)((buffer[i] & 255) | (buffer[i + 1] << 8))) > 80) { signal = true; break; }
        if (signal) lastSignal = SystemClock.elapsedRealtime();
        owner.captureStatus = SystemClock.elapsedRealtime() - lastSignal > 6000
            ? "Sem áudio recebido. O app pode bloquear a captura ou estar pausado."
            : "Transmitindo som do dispositivo • estéreo";
        broadcast.publish(Arrays.copyOf(buffer, count));
      }
    } catch (Exception e) {
      if (recording && owner != null) owner.error = "Captura interrompida: " + e.getMessage();
      main.post(this::stopSelf);
    }
  }
  @Override public void onDestroy() {
    recording = false;
    if (broadcast != null) broadcast.close();
    if (recorder != null) { try { recorder.stop(); } catch (Exception ignored) {} }
    if (worker != null) { try { worker.join(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    if (recorder != null) recorder.release();
    if (projection != null) { try { projection.stop(); } catch (Exception ignored) {} }
    if (owner != null && PlayerService.instance == owner && owner.capture == broadcast) { owner.capture = null; owner.captureStatus = "Captura encerrada"; owner.snapshot(); owner.notifyNow(); }
    stopForeground(STOP_FOREGROUND_REMOVE);
    super.onDestroy();
  }
}
