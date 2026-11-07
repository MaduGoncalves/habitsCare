package com.example.habitscare;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/** Canais de notificação, checagem de permissão e exibição das notificações. */
public final class NotificationHelper {

    public static final String CANAL_SONO = "lembrete_sono";
    public static final String CANAL_AGUA = "lembrete_agua";
    public static final String CANAL_RESUMO = "resumo_dia";

    private NotificationHelper() { }

    /**
     * Cria os canais (Android 8+). Pode ser chamado quantas vezes quiser: se já existem, nada muda.
     * Chamamos na MainActivity e também antes de cada notificação.
     */
    public static void criarCanais(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel sono = new NotificationChannel(
                CANAL_SONO, "Hora de dormir", NotificationManager.IMPORTANCE_HIGH);
        sono.setDescription("Lembrete diário para se preparar para dormir");

        NotificationChannel agua = new NotificationChannel(
                CANAL_AGUA, "Beber água", NotificationManager.IMPORTANCE_DEFAULT);
        agua.setDescription("Lembretes de hidratação ao longo do dia");

        // IMPORTANCE_LOW = notificação "passiva": aparece na barra, mas sem som nem vibração
        NotificationChannel resumo = new NotificationChannel(
                CANAL_RESUMO, "Resumo do dia", NotificationManager.IMPORTANCE_LOW);
        resumo.setDescription("Convite às 20h para preencher os hábitos do dia");

        nm.createNotificationChannel(sono);
        nm.createNotificationChannel(agua);
        nm.createNotificationChannel(resumo);
    }

    /**
     * true se o app PODE notificar agora. Cobre os dois casos:
     * - Android 13+: permissão POST_NOTIFICATIONS concedida;
     * - qualquer versão: usuário não bloqueou as notificações do app nas configurações.
     */
    public static boolean notificacoesPermitidas(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** Mostra a notificação. Devolve false (sem erro) se não houver permissão. */
    @SuppressLint("MissingPermission") // a permissão é verificada logo abaixo
    public static boolean mostrar(Context context, String canal, int id, String titulo, String texto) {
        if (!notificacoesPermitidas(context)) return false;

        criarCanais(context);

        // Tocar na notificação abre o app
        Intent abrir = new Intent(context, MainActivity.class);
        abrir.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent toque = PendingIntent.getActivity(
                context, id, abrir, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int prioridade = CANAL_SONO.equals(canal) ? NotificationCompat.PRIORITY_HIGH
                : CANAL_RESUMO.equals(canal) ? NotificationCompat.PRIORITY_LOW
                  : NotificationCompat.PRIORITY_DEFAULT;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, canal)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(prioridade)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(toque)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
            return true;
        } catch (SecurityException e) {
            return false; // permissão revogada no meio do caminho: ignora sem crashar
        }
    }
}