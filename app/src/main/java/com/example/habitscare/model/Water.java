package com.example.habitscare.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "water",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "idUsuario",
                onDelete = ForeignKey.CASCADE
        ))

public class Water {
        @PrimaryKey(autoGenerate = true)
        private int id;
        private int idUsuario;
        private double quantidadeL;
        private String dia;
}
