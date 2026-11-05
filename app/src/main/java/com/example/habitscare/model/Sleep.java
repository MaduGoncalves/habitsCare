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
}
