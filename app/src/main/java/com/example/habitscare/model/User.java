package com.example.habitscare.model;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = "user_name", unique = true)})
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String user_name;
    public String password;
    public String email;
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] photo;

    // Campo que controla se este usuário é o ativo na sessão
    public boolean isLogged;
}
