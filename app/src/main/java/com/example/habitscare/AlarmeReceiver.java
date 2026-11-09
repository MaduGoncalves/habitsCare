package com.example.habitscare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

public class AlarmeReceiver extends BroadcastReceiver {

    // IDs das notificações
    private static final int ID_SONO = 2001;
    private static final int ID_CAMINHADA = 2002;
    private static final int ID_AGUA = 2003;

    @Override
    public void onReceive(Context context, Intent intent) {

        android.util.Log.d("ALARME", "onReceive: "
                + (intent == null ? "null" : intent.getAction()));

        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (!AlarmScheduler.ACAO_ALARME.equals(intent.getAction())) {
            return;
        }

        Context app = context.getApplicationContext();

        String tipo = intent.getStringExtra(AlarmScheduler.EXTRA_TIPO);

        if (tipo == null) {
            return;
        }

        if (!LembretesPrefs.isSessaoAtiva(app)) {
            android.util.Log.d("ALARME", "sessao inativa, ignorando");
            return;
        }

        switch (tipo) {

            // ---------------- SONO ----------------
            case AlarmScheduler.TIPO_SONO:

                if (!LembretesPrefs.isSonoAtivo(app)) {
                    break;
                }

                AlarmScheduler.agendarSono(app);

                if (!NotificationHelper.notificacoesPermitidas(app)) {
                    break;
                }

                NotificationHelper.mostrar(
                        app,
                        NotificationHelper.CANAL_SONO,
                        ID_SONO,
                        "Hora de dormir! 🌙",
                        "Está chegando a hora de descansar. Prepare-se para uma boa noite de sono!"
                );

                break;

            // ---------------- ÁGUA (serviço + plano B) ----------------
            case AlarmScheduler.TIPO_AGUA:

                if (!LembretesPrefs.isAguaAtiva(app)) {
                    android.util.Log.d("ALARME", "agua desativada");
                    break;
                }

                AlarmScheduler.agendarAgua(app);

                try {
                    android.util.Log.d("ALARME", "iniciando AlarmeService");
                    ContextCompat.startForegroundService(
                            app, new Intent(app, AlarmeService.class));
                } catch (Exception e) {
                    // Plano B: se o serviço falhar, pelo menos mostra a notificação
                    android.util.Log.e("ALARME", "falha ao iniciar servico: " + e);
                    NotificationHelper.mostrar(
                            app,
                            NotificationHelper.CANAL_AGUA,
                            ID_AGUA,
                            "Hora de beber água! 💧",
                            "Não se esqueça de se hidratar. Beba um copo de água!"
                    );
                }

                break;

            // ---------------- CAMINHADA ----------------
            case AlarmScheduler.TIPO_CAMINHADA:

                if (!LembretesPrefs.isCaminhadaAtiva(app)) {
                    break;
                }

                AlarmScheduler.agendarCaminhada(app);

                if (!NotificationHelper.notificacoesPermitidas(app)) {
                    break;
                }

                NotificationHelper.mostrar(
                        app,
                        NotificationHelper.CANAL_CAMINHADA,
                        ID_CAMINHADA,
                        "Hora de caminhar! 🚶‍♀️",
                        "Que tal fazer uma caminhada? Reserve um momento para movimentar o corpo e cuidar da sua saúde!"
                );

                break;

            default:
                break;
        }
    }
}