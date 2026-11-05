package com.example.habitscare.dados_dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.habitscare.model.Sleep;
import com.example.habitscare.model.Water;

import java.util.List;

@Dao
public interface WaterDao {
    @Query("SELECT * FROM water")
    LiveData<List<Water>> getAll();

    @Query("SELECT * FROM water WHERE idUsuario = :idUsuario")
    LiveData<List<Water>> getByIdUser(int idUsuario);

    @Insert
    void insert(Water w);

    @Update
    void atualizar(Water w);

    @Delete
    void apagar(Water w);
}
