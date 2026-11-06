package com.example.habitscare.model;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.time.LocalTime;

@Entity(tableName = "sleep",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "idUsuario",
                onDelete = ForeignKey.CASCADE
        ))
public class Sleep {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int idUsuario;
    private String horarioDormir;
    private String horarioAcordar;
    private String dia;
    // Construtor Vazio (Exigido pelo Room)
    public Sleep() {
    }

    // Construtor com Parâmetros
    public Sleep(int idUsuario, String horarioDormir, String horarioAcordar, String dia) {
        this.idUsuario = idUsuario;
        this.horarioDormir = horarioDormir;
        this.horarioAcordar = horarioAcordar;
        this.dia = dia;
    }

    // Getters e Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getHorarioDormir() {
        return horarioDormir;
    }

    public void setHorarioDormir(String horarioDormir) {
        this.horarioDormir = horarioDormir;
    }

    public String getHorarioAcordar() {
        return horarioAcordar;
    }

    public void setHorarioAcordar(String horarioAcordar) {
        this.horarioAcordar = horarioAcordar;
    }

    public String getDia() {
        return dia;
    }

    public void setDia(String dia) {
        this.dia = dia;
    }
}
