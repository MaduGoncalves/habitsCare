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
        public Water() {
        }

        public Water(int idUsuario, double quantidadeL, String dia) {
                this.idUsuario = idUsuario;
                this.quantidadeL = quantidadeL;
                this.dia = dia;
        }

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

        public double getQuantidadeL() {
                return quantidadeL;
        }

        public void setQuantidadeL(double quantidadeL) {
                this.quantidadeL = quantidadeL;
        }

        public String getDia() {
                return dia;
        }

        public void setDia(String dia) {
                this.dia = dia;
        }
}
