package com.example.habitscare;

/** Resumo de um dia no histórico: água, caminhada e sono. (Não é tabela do banco.) */
public class HistoricoDia {
    public final String dia;             // "yyyy-MM-dd"
    public final double litros;          // água do dia (0 se não houve)
    public final double km;              // caminhada do dia (0 se não houve)
    public final String horarioDormir;   // "HH:mm" ou null
    public final String horarioAcordar;  // "HH:mm" ou null

    public HistoricoDia(String dia, double litros, double km,
                        String horarioDormir, String horarioAcordar) {
        this.dia = dia;
        this.litros = litros;
        this.km = km;
        this.horarioDormir = horarioDormir;
        this.horarioAcordar = horarioAcordar;
    }

    public boolean temSono() {
        return horarioDormir != null && horarioAcordar != null;
    }
}