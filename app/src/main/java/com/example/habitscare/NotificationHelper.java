package com.example.habitscare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    public static final String CANAL_SONO = "lembrete_sono_v5";
    public static final String CANAL_CAMINHADA = "lembrete_caminhada";
    public static final String CANAL_AGUA = "lembrete_agua_alarme_v2";   // ID NOVO

    public static void criarCanais(Context context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }

        // ---------------- SONO ----------------
        NotificationChannel sono = new NotificationChannel(
                CANAL_SONO, "Lembrete de sono", NotificationManager.IMPORTANCE_HIGH);
        sono.setDescription("Lembretes relacionados ao horário de dormir");
        sono.enableVibration(true);
        sono.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});

        // ---------------- CAMINHADA ----------------
        NotificationChannel caminhada = new NotificationChannel(
                CANAL_CAMINHADA, "Lembrete de caminhada", NotificationManager.IMPORTANCE_DEFAULT);
        caminhada.setDescription("Lembrete diário para fazer uma caminhada");
        caminhada.enableVibration(false);

        // ---------------- ÁGUA (alarme) ----------------
        Uri somAgua = Uri.parse(
                "android.resource://" + context.getPackageName() + "/" + R.raw.agua);

        AudioAttributes atributosSom = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel agua = new NotificationChannel(
                CANAL_AGUA, "Lembrete de água", NotificationManager.IMPORTANCE_HIGH);
        agua.setDescription("Lembretes para beber água");
        agua.enableVibration(true);
        agua.setVibrationPattern(new long[]{0, 500, 300, 500, 300, 500});
        agua.setSound(somAgua, atributosSom);
        agua.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        // Apaga o canal ANTIGO da água (ID diferente do novo)
        nm.deleteNotificationChannel("lembrete_agua_personalizado");

        nm.createNotificationChannel(sono);
        nm.createNotificationChannel(caminhada);
        nm.createNotificationChannel(agua);
    }

    public static boolean notificacoesPermitidas(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void mostrar(Context context, String canal, int id,
                               String titulo, String texto) {

        if (!notificacoesPermitidas(context)) {
            return;
        }

        if (!CANAL_SONO.equals(canal)
                && !CANAL_CAMINHADA.equals(canal)
                && !CANAL_AGUA.equals(canal)) {
            return;
        }

        criarCanais(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean agua = CANAL_AGUA.equals(canal);

        int prioridade;
        if (agua) {
            prioridade = NotificationCompat.PRIORITY_MAX;
        } else if (CANAL_SONO.equals(canal)) {
            prioridade = NotificationCompat.PRIORITY_HIGH;
        } else {
            prioridade = NotificationCompat.PRIORITY_DEFAULT;
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, canal)
                        .setSmallIcon(R.drawable.ic_notificacao)
                        .setContentTitle(titulo)
                        .setContentText(texto)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                        .setCategory(agua
                                ? NotificationCompat.CATEGORY_ALARM
                                : NotificationCompat.CATEGORY_REMINDER)
                        .setPriority(prioridade)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        // Antes do Android 8.0 não existem canais
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (agua) {
                Uri somAgua = Uri.parse(
                        "android.resource://" + context.getPackageName() + "/" + R.raw.agua);
                builder.setSound(somAgua);
            } else if (CANAL_CAMINHADA.equals(canal)) {
                builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
            } else if (CANAL_SONO.equals(canal)) {
                builder.setSound(null);
            }
        }

        try {
            Notification notificacao = builder.build();

            if (agua) {
                // Repete o som até o usuário tocar ou dispensar
                notificacao.flags |= Notification.FLAG_INSISTENT;
            }

            NotificationManagerCompat.from(context).notify(id, notificacao);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}