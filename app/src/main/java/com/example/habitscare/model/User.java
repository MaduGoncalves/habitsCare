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
    // Construtor Vazio (Exigido pelo Room)
    public User() {
    }

    // Construtor com Parâmetros
    public User(String user_name, String password, String email, byte[] photo, boolean isLogged) {
        this.user_name = user_name;
        this.password = password;
        this.email = email;
        this.photo = photo;
        this.isLogged = isLogged;
    }

    // Getters e Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public boolean isLogged() {
        return isLogged;
    }

    public void setLogged(boolean logged) {
        isLogged = logged;
    }
}
