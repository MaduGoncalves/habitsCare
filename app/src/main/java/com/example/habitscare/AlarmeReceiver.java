
package com.example.habitscare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmeReceiver extends BroadcastReceiver {

    // IDs das notificações
    private static final int ID_SONO = 2001;
    private static final int ID_CAMINHADA = 2002;
    private static final int ID_AGUA = 2003;

    @Override
    public void onReceive(Context context, Intent intent) {

        // Verifica se o Intent recebido é válido.
        if (intent == null || intent.getAction() == null) {
            return;
        }

        // Verifica se a ação corresponde ao alarme do aplicativo.
        if (!AlarmScheduler.ACAO_ALARME.equals(intent.getAction())) {
            return;
        }

        // Utiliza o contexto da aplicação.
        Context app = context.getApplicationContext();

        // Recupera o tipo de alarme recebido.
        String tipo = intent.getStringExtra(AlarmScheduler.EXTRA_TIPO);

        if (tipo == null) {
            return;
        }

        // Verifica se existe uma sessão ativa no aplicativo.
        if (!LembretesPrefs.isSessaoAtiva(app)) {
            return;
        }

        switch (tipo) {

            // =============================================
            // ALARME DO SONO
            // =============================================

            case AlarmScheduler.TIPO_SONO:

                // Verifica se o lembrete de sono está ativado.
                if (!LembretesPrefs.isSonoAtivo(app)) {
                    break;
                }

                // Agenda novamente o próximo lembrete de sono.
                AlarmScheduler.agendarSono(app);

                // Verifica se as notificações estão permitidas.
                if (!NotificationHelper.notificacoesPermitidas(app)) {
                    break;
                }

                // Exibe a notificação de sono.
                NotificationHelper.mostrar(
                        app,
                        NotificationHelper.CANAL_SONO,
                        ID_SONO,
                        "Hora de dormir! 🌙",
                        "Está chegando a hora de descansar. Prepare-se para uma boa noite de sono!"
                );

                break;

            // =============================================
            // ALARME DA ÁGUA
            // =============================================

            case AlarmScheduler.TIPO_AGUA:

                // Verifica se o lembrete de água está ativado.
                if (!LembretesPrefs.isAguaAtiva(app)) {
                    break;
                }

                // Agenda o próximo lembrete de água.
                AlarmScheduler.agendarAgua(app);

                // Verifica se as notificações estão permitidas.
                if (!NotificationHelper.notificacoesPermitidas(app)) {
                    break;
                }

                /*
                 * Exibe a notificação de água.
                 * O som personalizado será reproduzido
                 * pelo canal CANAL_AGUA.
                 */
                NotificationHelper.mostrar(
                        app,
                        NotificationHelper.CANAL_AGUA,
                        ID_AGUA,
                        "Hora de beber água! 💧",
                        "Não se esqueça de se hidratar. Beba um copo de água!"
                );

                break;

            // =============================================
            // ALARME DA CAMINHADA
            // =============================================

            case AlarmScheduler.TIPO_CAMINHADA:

                // Verifica se o lembrete de caminhada está ativado.
                if (!LembretesPrefs.isCaminhadaAtiva(app)) {
                    break;
                }

                // Agenda novamente a próxima caminhada,
                // respeitando o horário escolhido pelo usuário.
                AlarmScheduler.agendarCaminhada(app);

                // Verifica se as notificações estão permitidas.
                if (!NotificationHelper.notificacoesPermitidas(app)) {
                    break;
                }

                // Exibe a notificação da caminhada.
                NotificationHelper.mostrar(
                        app,
                        NotificationHelper.CANAL_CAMINHADA,
                        ID_CAMINHADA,
                        "Hora de caminhar! 🚶‍♀️",
                        "Que tal fazer uma caminhada? Reserve um momento para movimentar o corpo e cuidar da sua saúde!"
                );

                break;

            default:

                // Não faz nada caso o tipo de alarme seja desconhecido.
                break;
        }
    }
}
