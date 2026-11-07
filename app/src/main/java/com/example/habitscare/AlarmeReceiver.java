package com.example.habitscare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * Recebe o disparo dos alarmes (hora de dormir, água e resumo do dia),
 * agenda o próximo e mostra a notificação correspondente.
 */
public class AlarmeReceiver extends BroadcastReceiver {

    private static final int ID_SONO = 2001;
    private static final int ID_AGUA = 2002;
    private static final int ID_RESUMO = 2003;

    private static final String[] FRASES_AGUA = {
            "Que tal um copo de água agora? Seu corpo agradece.",
            "Hidratação em dia! Beba um copo de água.",
            "Pausa rápida: um copo de água ajuda no foco e na energia."
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String tipo = intent.getStringExtra(AlarmScheduler.EXTRA_TIPO);
        if (tipo == null) return;

        Context app = context.getApplicationContext();

        // Sem usuário logado: não notifica
        if (!LembretesPrefs.isSessaoAtiva(app)) return;

        // Permissão de notificações perdida: desliga tudo sem erro
        if (!NotificationHelper.notificacoesPermitidas(app)) {
            LembretesPrefs.desativarTudo(app);
            AlarmScheduler.cancelarTudo(app);
            return;
        }

        switch (tipo) {
            case AlarmScheduler.TIPO_SONO:
                if (!LembretesPrefs.isSonoAtivo(app)) return;
                AlarmScheduler.agendarSono(app);   // agenda o de amanhã antes de notificar
                NotificationHelper.mostrar(app, NotificationHelper.CANAL_SONO, ID_SONO,
                        "Hora de dormir 🌙",
                        "Está na hora de se preparar para ir para a cama. Boa noite!");
                break;

            case AlarmScheduler.TIPO_AGUA:
                if (!LembretesPrefs.isAguaAtiva(app)) return;
                AlarmScheduler.agendarAgua(app);   // agenda o próximo lembrete
                int indice = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) % FRASES_AGUA.length;
                NotificationHelper.mostrar(app, NotificationHelper.CANAL_AGUA, ID_AGUA,
                        "Hora de beber água 💧", FRASES_AGUA[indice]);
                break;

            case AlarmScheduler.TIPO_RESUMO:
                if (!LembretesPrefs.isResumoAtivo(app)) return;
                AlarmScheduler.agendarResumo(app); // agenda o de amanhã
                NotificationHelper.mostrar(app, NotificationHelper.CANAL_RESUMO, ID_RESUMO,
                        "Resumo do dia 📋",
                        "Confira e preencha os hábitos de hoje: água, caminhada e sono.");
                break;

            default:
                break;
        }
    }
}