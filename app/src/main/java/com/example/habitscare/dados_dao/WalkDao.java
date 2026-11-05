package com.example.habitscare.dados_dao;

import androidx.lifecycle.LiveData;

import com.example.habitscare.model.Walk;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WalkDao {
    @Query("SELECT * FROM walk")
    LiveData<List<Walk>> getAll();

    @Query("SELECT * FROM walk WHERE idUsuario = :idUsuario")
    LiveData<List<Walk>> getByIdUser(int idUsuario);

    @Insert
    void insert(Walk w);

    @Update
    void atualizar(Walk w);

    @Delete
    void apagar(Walk w);
}
