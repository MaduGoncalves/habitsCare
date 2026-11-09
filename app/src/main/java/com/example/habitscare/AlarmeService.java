package com.example.habitscare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;


public class AlarmeService extends Service {

    public static final String ACAO_PARAR = "com.example.habitscare.PARAR_ALARME";
    private static final String CANAL = "alarme_tocando_v1";
    private static final int NOTIF_ID = 2010;
    private static final long DURACAO_MS = 30_000;   // toca no máximo 30 s

    private MediaPlayer mp;
    private Vibrator vibrator;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && ACAO_PARAR.equals(intent.getAction())) {
            pararTudo();
            return START_NOT_STICKY;
        }

        criarCanal();

        Intent abrir = new Intent(this, MainActivity.class);
        abrir.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent piAbrir = PendingIntent.getActivity(
                this, 0, abrir,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent parar = new Intent(this, AlarmeService.class).setAction(ACAO_PARAR);
        PendingIntent piParar = PendingIntent.getService(
                this, 1, parar,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(this, CANAL)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle("Hora de beber água! 💧")
                .setContentText("Toque em Parar para silenciar")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setContentIntent(piAbrir)
                .addAction(0, "Parar", piParar)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, n);
        }

        tocar();
        handler.postDelayed(this::pararTudo, DURACAO_MS);

        return START_NOT_STICKY;
    }

    private void tocar() {
        try {
            android.util.Log.d("ALARME", "tocar() chamado");

            mp = MediaPlayer.create(this, R.raw.agua,
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(), 0);

            android.util.Log.d("ALARME", "MediaPlayer = " + mp);

            if (mp != null) {
                mp.setLooping(true);
                mp.start();
            }

            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                long[] padrao = {0, 600, 400, 600, 400};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(padrao, 0));
                } else {
                    vibrator.vibrate(padrao, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void criarCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(
                    CANAL, "Alarme tocando", NotificationManager.IMPORTANCE_HIGH);
            c.setSound(null, null);       // o som é tocado pelo serviço
            c.enableVibration(false);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private void pararTudo() {
        handler.removeCallbacksAndMessages(null);
        try {
            if (mp != null) {
                if (mp.isPlaying()) mp.stop();
                mp.release();
                mp = null;
            }
        } catch (Exception ignored) {}
        if (vibrator != null) vibrator.cancel();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        pararTudo();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}