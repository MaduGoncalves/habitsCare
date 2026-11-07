package com.example.habitscare;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.util.Calendar;

/**
 * Agenda e cancela os alarmes no AlarmManager.
 *
 * - Hora de dormir e Resumo do dia: alarme EXATO, que se repete todo dia
 *   (o AlarmeReceiver agenda o do dia seguinte assim que o de hoje dispara).
 * - Beber água: alarme aproximado, a cada N horas entre 8h e 22h (não precisa ser exato).
 * - Se a permissão de alarme exato não estiver concedida, cai para alarme aproximado
 *   (o lembrete pode atrasar alguns minutos, mas continua funcionando e o app não quebra).
 */
public final class AlarmScheduler {

    public static final String ACAO_ALARME = "com.example.habitscare.ACAO_ALARME";
    public static final String EXTRA_TIPO = "tipo";

    public static final String TIPO_SONO = "sono";
    public static final String TIPO_AGUA = "agua";
    public static final String TIPO_RESUMO = "resumo";

    private static final int HORA_RESUMO = 20;   // 20h
    private static final int AGUA_INICIO = 8;    // primeiro lembrete a partir das 8h
    private static final int AGUA_FIM = 22;      // sem lembretes a partir das 22h

    private AlarmScheduler() { }

    // ---------- Permissão de alarme exato (Android 12+) ----------

    /** true se pode agendar alarmes exatos (sempre true antes do Android 12). */
    public static boolean podeAgendarExato(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    /** Tela do sistema onde o usuário libera "Alarmes e lembretes" para o app. */
    public static Intent intentPermissaoAlarmeExato(Context context) {
        Uri pacote = Uri.parse("package:" + context.getPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pacote);
        }
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pacote);
    }

    // ---------- Agendamentos ----------

    /** Agenda a próxima "hora de dormir" (hoje, se ainda não passou; senão amanhã). */
    public static void agendarSono(Context context) {
        long quando = proximaOcorrencia(
                LembretesPrefs.getSonoHora(context), LembretesPrefs.getSonoMinuto(context));
        agendar(context, TIPO_SONO, quando, true);
    }

    /** Agenda o próximo lembrete de água, respeitando o intervalo e a janela das 8h às 22h. */
    public static void agendarAgua(Context context) {
        long quando = proximaAgua(LembretesPrefs.getAguaIntervaloH(context));
        agendar(context, TIPO_AGUA, quando, false);
    }

    /** Agenda o próximo resumo do dia, às 20h. */
    public static void agendarResumo(Context context) {
        agendar(context, TIPO_RESUMO, proximaOcorrencia(HORA_RESUMO, 0), true);
    }

    public static void cancelar(Context context, String tipo) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pendingIntent(context, tipo));
    }

    public static void cancelarTudo(Context context) {
        cancelar(context, TIPO_SONO);
        cancelar(context, TIPO_AGUA);
        cancelar(context, TIPO_RESUMO);
    }

    /**
     * Põe os alarmes de acordo com as preferências salvas. É seguro chamar sempre
     * (login, boot, voltar para a tela de Perfil...).
     * - Sem usuário logado: cancela tudo.
     * - Sem permissão de notificações: desativa os lembretes sem erro e cancela os alarmes.
     */
    public static void reagendarTudo(Context context) {
        Context app = context.getApplicationContext();

        if (!LembretesPrefs.isSessaoAtiva(app)) {
            cancelarTudo(app);
            return;
        }
        if (LembretesPrefs.algumAtivo(app) && !NotificationHelper.notificacoesPermitidas(app)) {
            LembretesPrefs.desativarTudo(app);
            cancelarTudo(app);
            return;
        }

        if (LembretesPrefs.isSonoAtivo(app)) agendarSono(app); else cancelar(app, TIPO_SONO);
        if (LembretesPrefs.isAguaAtiva(app)) agendarAgua(app); else cancelar(app, TIPO_AGUA);
        if (LembretesPrefs.isResumoAtivo(app)) agendarResumo(app); else cancelar(app, TIPO_RESUMO);
    }

    // ---------- Internos ----------

    private static void agendar(Context context, String tipo, long quandoMillis, boolean preferirExato) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = pendingIntent(context, tipo);

        if (preferirExato && podeAgendarExato(context)) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quandoMillis, pi);
                return;
            } catch (SecurityException e) {
                // Permissão revogada entre a checagem e o agendamento: usa o aproximado abaixo
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quandoMillis, pi);
    }

    private static PendingIntent pendingIntent(Context context, String tipo) {
        Intent intent = new Intent(context, AlarmeReceiver.class);
        intent.setAction(ACAO_ALARME);
        intent.putExtra(EXTRA_TIPO, tipo);
        return PendingIntent.getBroadcast(
                context, codigo(tipo), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Cada tipo tem seu código, para um alarme não substituir o outro. */
    public static int codigo(String tipo) {
        switch (tipo) {
            case TIPO_SONO:   return 1001;
            case TIPO_AGUA:   return 1002;
            default:          return 1003;
        }
    }

    /** Próximo horário "hh:mm": hoje se ainda não passou, senão amanhã. */
    private static long proximaOcorrencia(int hora, int minuto) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hora);
        c.set(Calendar.MINUTE, minuto);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return c.getTimeInMillis();
    }

    /** Agora + intervalo; se cair fora das 8h–22h, vai para as 8h seguintes. */
    private static long proximaAgua(int intervaloHoras) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR_OF_DAY, Math.max(1, intervaloHoras));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int hora = c.get(Calendar.HOUR_OF_DAY);
        if (hora >= AGUA_FIM) {                 // noite: amanhã de manhã
            c.add(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, AGUA_INICIO);
            c.set(Calendar.MINUTE, 0);
        } else if (hora < AGUA_INICIO) {        // madrugada: hoje de manhã
            c.set(Calendar.HOUR_OF_DAY, AGUA_INICIO);
            c.set(Calendar.MINUTE, 0);
        }
        return c.getTimeInMillis();
    }
}