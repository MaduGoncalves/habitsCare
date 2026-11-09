package com.example.habitscare;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.util.Calendar;

public class AlarmScheduler {

    private static final boolean MODO_TESTE = true;
    private static final int INTERVALO_TESTE_MIN = 2;

    // Ação utilizada para identificar os alarmes do aplicativo.
    public static final String ACAO_ALARME =
            "com.example.habitscare.ACAO_ALARME";

    // Chave utilizada para identificar o tipo de alarme.
    public static final String EXTRA_TIPO = "tipo";

    // Tipos de alarme.
    public static final String TIPO_SONO = "sono";
    public static final String TIPO_AGUA = "agua";
    public static final String TIPO_CAMINHADA = "caminhada";

    // Horários permitidos para os lembretes de água.
    private static final int AGUA_INICIO = 8;
    private static final int AGUA_FIM = 22;

    /**
     * Verifica se o aplicativo possui permissão
     * para agendar alarmes exatos.
     */
    public static boolean podeAgendarExato(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            AlarmManager am =
                    (AlarmManager) context.getSystemService(
                            Context.ALARM_SERVICE
                    );

            return am != null && am.canScheduleExactAlarms();
        }

        return true;
    }

    /**
     * Retorna a Intent para solicitar a permissão
     * de alarmes exatos nas configurações do Android.
     */
    public static Intent intentPermissaoAlarmeExato(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            return new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + context.getPackageName())
            );
        }

        return new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + context.getPackageName())
        );
    }

    // =============================================
    // AGENDAMENTO DOS LEMBRETES
    // =============================================

    /**
     * Agenda o lembrete de sono utilizando o horário
     * configurado nas preferências do aplicativo.
     */
    public static void agendarSono(Context context) {

        if (!LembretesPrefs.isSonoAtivo(context)) {
            cancelar(context, TIPO_SONO);
            return;
        }

        int hora = LembretesPrefs.getSonoHora(context);
        int minuto = LembretesPrefs.getSonoMinuto(context);

        Calendar calendario = proximaOcorrencia(hora, minuto);

        agendar(
                context,
                TIPO_SONO,
                calendario.getTimeInMillis()
        );
    }

    /**
     * Agenda o lembrete de água utilizando o intervalo
     * configurado nas preferências do aplicativo.
     */
    public static void agendarAgua(Context context) {

        if (!LembretesPrefs.isAguaAtiva(context)) {
            cancelar(context, TIPO_AGUA);
            return;
        }

        int intervalo = LembretesPrefs.getAguaIntervaloH(context);

        Calendar calendario = proximaAgua(intervalo);

        agendar(
                context,
                TIPO_AGUA,
                calendario.getTimeInMillis()
        );
    }

    /**
     * Agenda o lembrete de caminhada utilizando
     * o horário escolhido pelo usuário.
     *
     * O horário padrão é 20h, mas pode ser alterado
     * pelo botão de configuração na tela de Perfil.
     */
    public static void agendarCaminhada(Context context) {

        if (!LembretesPrefs.isCaminhadaAtiva(context)) {
            cancelar(context, TIPO_CAMINHADA);
            return;
        }

        // Recupera o horário escolhido pelo usuário.
        int hora = LembretesPrefs.getCaminhadaHora(context);
        int minuto = LembretesPrefs.getCaminhadaMinuto(context);

        // Calcula a próxima ocorrência desse horário.
        Calendar calendario = proximaOcorrencia(hora, minuto);

        agendar(
                context,
                TIPO_CAMINHADA,
                calendario.getTimeInMillis()
        );
    }

    // =============================================
    // CANCELAMENTO DOS ALARMES
    // =============================================

    /**
     * Cancela um alarme específico.
     */
    public static void cancelar(Context context, String tipo) {

        AlarmManager am =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (am == null) return;

        PendingIntent pi = criarPendingIntent(context, tipo);

        am.cancel(pi);
        pi.cancel();
    }

    /**
     * Cancela todos os alarmes do aplicativo.
     */
    public static void cancelarTudo(Context context) {

        cancelar(context, TIPO_SONO);
        cancelar(context, TIPO_AGUA);
        cancelar(context, TIPO_CAMINHADA);
    }

    // =============================================
    // REAGENDAMENTO
    // =============================================

    /**
     * Reagenda todos os lembretes ativos.
     *
     * Esse método é utilizado quando o usuário inicia
     * uma sessão ou quando o celular é reiniciado.
     */
    public static void reagendarTudo(Context context) {

        Context app = context.getApplicationContext();

        // Verifica se existe uma sessão ativa.
        if (!LembretesPrefs.isSessaoAtiva(app)) {
            cancelarTudo(app);
            return;
        }

        // Reagenda o lembrete de água.
        if (LembretesPrefs.isAguaAtiva(app)) {

            agendarAgua(app);

        } else {

            cancelar(app, TIPO_AGUA);
        }

        // Reagenda o lembrete de sono.
        if (LembretesPrefs.isSonoAtivo(app)
                && NotificationHelper.notificacoesPermitidas(app)) {

            agendarSono(app);

        } else {

            cancelar(app, TIPO_SONO);
        }

        // Reagenda o lembrete de caminhada.
        if (LembretesPrefs.isCaminhadaAtiva(app)
                && NotificationHelper.notificacoesPermitidas(app)) {

            agendarCaminhada(app);

        } else {

            cancelar(app, TIPO_CAMINHADA);
        }
    }

    // =============================================
    // ALARME MANAGER
    // =============================================

    /**
     * Agenda um alarme no AlarmManager.
     *
     * Tenta utilizar um alarme exato. Caso não tenha
     * permissão, utiliza um alarme não exato.
     */
    private static void agendar(
            Context context,
            String tipo,
            long horario
    ) {

        AlarmManager am =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (am == null) return;

        PendingIntent pi = criarPendingIntent(context, tipo);

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                if (podeAgendarExato(context)) {

                    am.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            horario,
                            pi
                    );

                } else {

                    am.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            horario,
                            pi
                    );
                }

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                am.setExact(
                        AlarmManager.RTC_WAKEUP,
                        horario,
                        pi
                );

            } else {

                am.set(
                        AlarmManager.RTC_WAKEUP,
                        horario,
                        pi
                );
            }

        } catch (SecurityException e) {

            // Caso não seja possível agendar um alarme exato,
            // tenta utilizar um alarme não exato.
            try {

                am.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        horario,
                        pi
                );

            } catch (Exception ex) {

                ex.printStackTrace();
            }
        }
    }

    // =============================================
    // PENDING INTENT
    // =============================================

    /**
     * Cria o PendingIntent responsável por chamar
     * o AlarmeReceiver quando o horário chegar.
     */
    private static PendingIntent criarPendingIntent(
            Context context,
            String tipo
    ) {

        Intent intent = new Intent(context, AlarmeReceiver.class);

        intent.setAction(ACAO_ALARME);
        intent.putExtra(EXTRA_TIPO, tipo);

        return PendingIntent.getBroadcast(
                context,
                codigo(tipo),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Define um código único para cada tipo de alarme.
     */
    private static int codigo(String tipo) {

        switch (tipo) {

            case TIPO_SONO:
                return 1001;

            case TIPO_AGUA:
                return 1002;

            case TIPO_CAMINHADA:
                return 1003;

            default:
                return 1000;
        }
    }

    // =============================================
    // CÁLCULO DOS HORÁRIOS
    // =============================================

    /**
     * Calcula a próxima ocorrência de um horário diário.
     */
    private static Calendar proximaOcorrencia(
            int hora,
            int minuto
    ) {

        Calendar calendario = Calendar.getInstance();

        calendario.set(Calendar.HOUR_OF_DAY, hora);
        calendario.set(Calendar.MINUTE, minuto);
        calendario.set(Calendar.SECOND, 0);
        calendario.set(Calendar.MILLISECOND, 0);

        // Se o horário já passou hoje, agenda para amanhã.
        if (calendario.getTimeInMillis() <= System.currentTimeMillis()) {
            calendario.add(Calendar.DAY_OF_YEAR, 1);
        }

        return calendario;
    }

    /**
     * Calcula o próximo horário do lembrete de água.
     *
     * Os lembretes acontecem entre 08h e 22h,
     * respeitando o intervalo configurado pelo usuário.
     */
    private static Calendar proximaAgua(int intervalo) {

        // ===== MODO DE TESTE: ignora o intervalo e a janela 08h-22h =====
        if (MODO_TESTE) {
            Calendar teste = Calendar.getInstance();
            teste.add(Calendar.MINUTE, INTERVALO_TESTE_MIN);
            teste.set(Calendar.SECOND, 0);
            teste.set(Calendar.MILLISECOND, 0);
            return teste;
        }

        Calendar agora = Calendar.getInstance();

        Calendar proxima = (Calendar) agora.clone();

        // Adiciona o intervalo configurado.
        proxima.add(Calendar.HOUR_OF_DAY, intervalo);

        proxima.set(Calendar.SECOND, 0);
        proxima.set(Calendar.MILLISECOND, 0);

        // Se ultrapassar 22h, agenda para as 08h do dia seguinte.
        if (proxima.get(Calendar.HOUR_OF_DAY) >= AGUA_FIM) {

            proxima.add(Calendar.DAY_OF_YEAR, 1);
            proxima.set(Calendar.HOUR_OF_DAY, AGUA_INICIO);
            proxima.set(Calendar.MINUTE, 0);
        }

        // Garante que o horário não seja anterior às 08h.
        if (proxima.get(Calendar.HOUR_OF_DAY) < AGUA_INICIO) {

            proxima.set(Calendar.HOUR_OF_DAY, AGUA_INICIO);
            proxima.set(Calendar.MINUTE, 0);
        }

        return proxima;
    }
}