package com.example.habitscare.dados_dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.habitscare.model.User;

import java.util.List;

@Dao
public interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User buscarPorId(int id);

    @Query("SELECT * FROM users WHERE isLogged = 1 LIMIT 1")
    LiveData<User> getUsuarioAtivo();

    @Query("SELECT * FROM users WHERE user_name = :userName LIMIT 1")
    User getUserByUsername(String userName);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("UPDATE users SET isLogged = 0")
    void logoutTodos();

    @Query("SELECT * FROM users")
    LiveData<List<User>> getAll();

    @Insert
    void insert(User u);

    @Update
    void atualizar(User u);

    @Delete
    void apagar(User u);
}
