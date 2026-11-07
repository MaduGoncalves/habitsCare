package com.example.habitscare;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Guarda as preferências dos lembretes do aplicativo.
 *
 * Armazena:
 * - Estado da sessão;
 * - Ativação e horário do lembrete de sono;
 * - Ativação e intervalo do lembrete de água;
 * - Ativação e horário do lembrete de caminhada.
 *
 * Utiliza SharedPreferences para que o AlarmeReceiver
 * consiga consultar as informações rapidamente,
 * sem precisar acessar o banco de dados.
 */
public final class LembretesPrefs {

    private static final String ARQUIVO = "lembretes_prefs";

    private static final String K_SESSAO = "sessao_ativa";

    private static final String K_SONO_ATIVO = "sono_ativo";
    private static final String K_SONO_HORA = "sono_hora";
    private static final String K_SONO_MIN = "sono_min";

    private static final String K_AGUA_ATIVO = "agua_ativo";
    private static final String K_AGUA_INTERVALO = "agua_intervalo_h";

    private static final String K_CAMINHADA_ATIVA = "caminhada_ativa";
    private static final String K_CAMINHADA_HORA = "caminhada_hora";
    private static final String K_CAMINHADA_MIN = "caminhada_min";

    // Chave antiga utilizada pelo lembrete de resumo do dia.
    // É mantida apenas para preservar configurações anteriores.
    private static final String K_RESUMO_ANTIGO = "resumo_ativo";

    // Impede a criação de objetos dessa classe.
    private LembretesPrefs() {
    }

    /**
     * Retorna o arquivo de preferências do aplicativo.
     */
    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext()
                .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    // =============================================
    // SESSÃO
    // =============================================

    /**
     * Verifica se existe uma sessão ativa.
     */
    public static boolean isSessaoAtiva(Context c) {
        return sp(c).getBoolean(K_SESSAO, false);
    }

    /**
     * Define se existe uma sessão ativa.
     */
    public static void setSessaoAtiva(Context c, boolean v) {
        sp(c).edit()
                .putBoolean(K_SESSAO, v)
                .apply();
    }

    // =============================================
    // LEMBRETE DE SONO
    // =============================================

    /**
     * Verifica se o lembrete de sono está ativado.
     */
    public static boolean isSonoAtivo(Context c) {
        return sp(c).getBoolean(K_SONO_ATIVO, false);
    }

    /**
     * Ativa ou desativa o lembrete de sono.
     */
    public static void setSonoAtivo(Context c, boolean v) {
        sp(c).edit()
                .putBoolean(K_SONO_ATIVO, v)
                .apply();
    }

    /**
     * Retorna a hora configurada para dormir.
     * O horário padrão é 22h.
     */
    public static int getSonoHora(Context c) {
        return sp(c).getInt(K_SONO_HORA, 22);
    }

    /**
     * Retorna os minutos configurados para dormir.
     */
    public static int getSonoMinuto(Context c) {
        return sp(c).getInt(K_SONO_MIN, 0);
    }

    /**
     * Salva o horário configurado para dormir.
     */
    public static void setSonoHorario(Context c, int hora, int minuto) {
        sp(c).edit()
                .putInt(K_SONO_HORA, hora)
                .putInt(K_SONO_MIN, minuto)
                .apply();
    }

    // =============================================
    // LEMBRETE DE ÁGUA
    // =============================================

    /**
     * Verifica se o lembrete de água está ativado.
     */
    public static boolean isAguaAtiva(Context c) {
        return sp(c).getBoolean(K_AGUA_ATIVO, false);
    }

    /**
     * Ativa ou desativa o lembrete de água.
     */
    public static void setAguaAtiva(Context c, boolean v) {
        sp(c).edit()
                .putBoolean(K_AGUA_ATIVO, v)
                .apply();
    }

    /**
     * Retorna o intervalo configurado entre os lembretes.
     * O intervalo padrão é de 2 horas.
     */
    public static int getAguaIntervaloH(Context c) {
        return sp(c).getInt(K_AGUA_INTERVALO, 2);
    }

    /**
     * Salva o intervalo entre os lembretes de água.
     */
    public static void setAguaIntervaloH(Context c, int horas) {
        sp(c).edit()
                .putInt(K_AGUA_INTERVALO, horas)
                .apply();
    }

    // =============================================
    // LEMBRETE DE CAMINHADA
    // =============================================

    /**
     * Verifica se o lembrete de caminhada está ativado.
     *
     * Caso ainda não exista a nova preferência,
     * consulta a chave antiga "resumo_ativo".
     * Isso preserva a ativação salva em versões anteriores.
     */
    public static boolean isCaminhadaAtiva(Context c) {

        SharedPreferences prefs = sp(c);

        if (prefs.contains(K_CAMINHADA_ATIVA)) {
            return prefs.getBoolean(K_CAMINHADA_ATIVA, false);
        }

        return prefs.getBoolean(K_RESUMO_ANTIGO, false);
    }

    /**
     * Ativa ou desativa o lembrete de caminhada.
     *
     * Ao salvar, remove a chave antiga.
     */
    public static void setCaminhadaAtiva(Context c, boolean v) {

        sp(c).edit()
                .putBoolean(K_CAMINHADA_ATIVA, v)
                .remove(K_RESUMO_ANTIGO)
                .apply();
    }

    /**
     * Retorna a hora configurada para a caminhada.
     * O horário padrão é 20h.
     */
    public static int getCaminhadaHora(Context c) {
        return sp(c).getInt(K_CAMINHADA_HORA, 20);
    }

    /**
     * Retorna os minutos configurados para a caminhada.
     */
    public static int getCaminhadaMinuto(Context c) {
        return sp(c).getInt(K_CAMINHADA_MIN, 0);
    }

    /**
     * Salva o horário configurado para a caminhada.
     */
    public static void setCaminhadaHorario(
            Context c,
            int hora,
            int minuto
    ) {

        sp(c).edit()
                .putInt(K_CAMINHADA_HORA, hora)
                .putInt(K_CAMINHADA_MIN, minuto)
                .apply();
    }

    // =============================================
    // VERIFICAÇÃO DOS LEMBRETES
    // =============================================

    /**
     * Verifica se pelo menos um lembrete está ativado.
     */
    public static boolean algumAtivo(Context c) {

        return isSonoAtivo(c)
                || isAguaAtiva(c)
                || isCaminhadaAtiva(c);
    }

    /**
     * Desativa todos os lembretes.
     *
     * Utilizado quando necessário desativar
     * todas as preferências de lembretes.
     */
    public static void desativarTudo(Context c) {

        sp(c).edit()
                .putBoolean(K_SONO_ATIVO, false)
                .putBoolean(K_AGUA_ATIVO, false)
                .putBoolean(K_CAMINHADA_ATIVA, false)
                .remove(K_RESUMO_ANTIGO)
                .apply();
    }
}