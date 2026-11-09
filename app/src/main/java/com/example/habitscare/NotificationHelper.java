package com.example.habitscare;

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

    // Identificadores dos canais de notificação
    public static final String CANAL_SONO = "lembrete_sono_v4";
    public static final String CANAL_CAMINHADA = "lembrete_caminhada";
    public static final String CANAL_AGUA = "lembrete_agua_personalizado";

    /**
     * Cria os canais de notificação do aplicativo
     *
     * No Android 8.0 ou superior, cada canal possui suas próprias
     * configurações de som, vibração e importância
     */
    public static void criarCanais(Context context) {

        // Os canais só existem a partir do Android 8.0 (API 26)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager nm =
                context.getSystemService(NotificationManager.class);

        if (nm == null) {
            return;
        }

        // -------------------------------------------------
        // CANAL DO SONO
        // -------------------------------------------------

        NotificationChannel sono = new NotificationChannel(
                CANAL_SONO,
                "Lembrete de sono",
                NotificationManager.IMPORTANCE_HIGH
        );

        sono.setDescription("Lembretes relacionados ao horário de dormir");

        // Ativa a vibração.
        sono.enableVibration(true);
        sono.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});

        // -------------------------------------------------
        // CANAL DA CAMINHADA
        // -------------------------------------------------

        NotificationChannel caminhada = new NotificationChannel(
                CANAL_CAMINHADA,
                "Lembrete de caminhada",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        caminhada.setDescription("Lembrete diário para fazer uma caminhada");

        // Não utiliza vibração.
        caminhada.enableVibration(false);

        // -------------------------------------------------
        // CANAL DA ÁGUA
        // -------------------------------------------------

        // Localiza o arquivo som_agua.mp3 dentro de res/raw
        Uri somAgua = Uri.parse(
                "android.resource://" +
                        context.getPackageName() +
                        "/" +
                        R.raw.agua
        );

        // Define como o Android deve reproduzir o áudio
        AudioAttributes atributosSom = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel agua = new NotificationChannel(
                CANAL_AGUA,
                "Lembrete de água",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        agua.setDescription("Lembretes para beber água");

        // Não utiliza vibração
        agua.enableVibration(false);

        // Configura o áudio personalizado
        agua.setSound(somAgua, atributosSom);

        // Registra os três canais no Android
        nm.createNotificationChannel(sono);
        nm.createNotificationChannel(caminhada);
        nm.createNotificationChannel(agua);
    }

    /**
     * Verifica se o aplicativo pode exibir notificações.
     */
    public static boolean notificacoesPermitidas(Context context) {
        return NotificationManagerCompat
                .from(context)
                .areNotificationsEnabled();
    }

    /**
     * Exibe uma notificação utilizando o canal informado.
     *
     * @param context Contexto do aplicativo.
     * @param canal Identificador do canal.
     * @param id Identificador único da notificação.
     * @param titulo Título da notificação.
     * @param texto Mensagem da notificação.
     */
    public static void mostrar(
            Context context,
            String canal,
            int id,
            String titulo,
            String texto
    ) {

        // Verifica se o aplicativo pode enviar notificações.
        if (!notificacoesPermitidas(context)) {
            return;
        }

        // Aceita somente os canais definidos nesta classe.
        if (!CANAL_SONO.equals(canal)
                && !CANAL_CAMINHADA.equals(canal)
                && !CANAL_AGUA.equals(canal)) {
            return;
        }

        // Garante que os canais estejam criados.
        criarCanais(context);

        // Ao tocar na notificação, abre a MainActivity.
        Intent intent = new Intent(context, MainActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
        );

        // Define a prioridade conforme o tipo de notificação.
        int prioridade = CANAL_SONO.equals(canal)
                ? NotificationCompat.PRIORITY_HIGH
                : NotificationCompat.PRIORITY_DEFAULT;

        // Monta a notificação.
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, canal)
                        .setSmallIcon(R.drawable.ic_notificacao)
                        .setContentTitle(titulo)
                        .setContentText(texto)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(texto)
                        )
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setPriority(prioridade)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        /*
         * Antes do Android 8.0, não existem canais de notificação.
         * Por isso, configuramos o som diretamente na notificação
         * nessas versões.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

            if (CANAL_AGUA.equals(canal)) {

                // Reproduz o áudio personalizado da água.
                Uri somAgua = Uri.parse(
                        "android.resource://" +
                                context.getPackageName() +
                                "/" +
                                R.raw.agua
                );

                builder.setSound(somAgua);

            } else if (CANAL_CAMINHADA.equals(canal)) {

                // Utiliza o som padrão de notificação.
                builder.setDefaults(NotificationCompat.DEFAULT_SOUND);

            } else if (CANAL_SONO.equals(canal)) {

                // Mantém o lembrete de sono sem som.
                builder.setSound(null);
            }
        }

        // Exibe a notificação.
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}