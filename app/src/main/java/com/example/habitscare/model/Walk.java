package com.example.habitscare.model;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "walk",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "idUsuario",
                onDelete = ForeignKey.CASCADE
        ))
public class Walk {
    @PrimaryKey(autoGenerate = true)
    public int id;
    private int idUsuario;
    private double km;
    private String dia;
}
