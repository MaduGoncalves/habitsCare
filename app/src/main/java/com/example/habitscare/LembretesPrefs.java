package com.example.habitscare;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Guarda as preferências dos lembretes (ligado/desligado, horário de dormir, intervalo da água).
 * Fica em SharedPreferences porque o AlarmeReceiver precisa ler isso rápido, sem abrir o banco.
 */
public final class LembretesPrefs {

    private static final String ARQUIVO = "lembretes_prefs";

    private static final String K_SESSAO = "sessao_ativa";
    private static final String K_SONO_ATIVO = "sono_ativo";
    private static final String K_SONO_HORA = "sono_hora";
    private static final String K_SONO_MIN = "sono_min";
    private static final String K_AGUA_ATIVO = "agua_ativo";
    private static final String K_AGUA_INTERVALO = "agua_intervalo_h";
    private static final String K_RESUMO_ATIVO = "resumo_ativo";

    private LembretesPrefs() { }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    // ---- Sessão: só notifica enquanto há um usuário logado ----
    public static boolean isSessaoAtiva(Context c) { return sp(c).getBoolean(K_SESSAO, false); }
    public static void setSessaoAtiva(Context c, boolean v) { sp(c).edit().putBoolean(K_SESSAO, v).apply(); }

    // ---- Hora de dormir ----
    public static boolean isSonoAtivo(Context c) { return sp(c).getBoolean(K_SONO_ATIVO, false); }
    public static void setSonoAtivo(Context c, boolean v) { sp(c).edit().putBoolean(K_SONO_ATIVO, v).apply(); }
    public static int getSonoHora(Context c) { return sp(c).getInt(K_SONO_HORA, 22); }
    public static int getSonoMinuto(Context c) { return sp(c).getInt(K_SONO_MIN, 0); }
    public static void setSonoHorario(Context c, int hora, int minuto) {
        sp(c).edit().putInt(K_SONO_HORA, hora).putInt(K_SONO_MIN, minuto).apply();
    }

    // ---- Beber água ----
    public static boolean isAguaAtiva(Context c) { return sp(c).getBoolean(K_AGUA_ATIVO, false); }
    public static void setAguaAtiva(Context c, boolean v) { sp(c).edit().putBoolean(K_AGUA_ATIVO, v).apply(); }
    public static int getAguaIntervaloH(Context c) { return sp(c).getInt(K_AGUA_INTERVALO, 2); }
    public static void setAguaIntervaloH(Context c, int horas) { sp(c).edit().putInt(K_AGUA_INTERVALO, horas).apply(); }

    // ---- Resumo do dia (20h) ----
    public static boolean isResumoAtivo(Context c) { return sp(c).getBoolean(K_RESUMO_ATIVO, false); }
    public static void setResumoAtivo(Context c, boolean v) { sp(c).edit().putBoolean(K_RESUMO_ATIVO, v).apply(); }

    public static boolean algumAtivo(Context c) {
        return isSonoAtivo(c) || isAguaAtiva(c) || isResumoAtivo(c);
    }

    /** Usado quando a permissão de notificações é negada ou revogada: desliga tudo sem erro. */
    public static void desativarTudo(Context c) {
        sp(c).edit()
                .putBoolean(K_SONO_ATIVO, false)
                .putBoolean(K_AGUA_ATIVO, false)
                .putBoolean(K_RESUMO_ATIVO, false)
                .apply();
    }
}