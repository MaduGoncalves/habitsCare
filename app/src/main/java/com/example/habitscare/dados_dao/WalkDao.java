package com.example.habitscare.dados_dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.habitscare.model.Walk;

import java.util.List;

@Dao
public interface WalkDao {
    @Query("SELECT * FROM walk")
    LiveData<List<Walk>> getAll();

    @Query("SELECT * FROM walk WHERE idUsuario = :idUsuario")
    LiveData<List<Walk>> getByIdUser(int idUsuario);

    // Método para o Dashboard "Hoje": busca os passos do dia atual do usuário
    @Query("SELECT * FROM walk WHERE idUsuario = :idUsuario AND dia = :dataHoje LIMIT 1")
    LiveData<Walk> getWalkDoDia(int idUsuario, String dataHoje);

    @Query("SELECT * FROM walk WHERE idUsuario = :idUsuario AND dia = :dia LIMIT 1")
    Walk getWalkDoDiaSync(int idUsuario, String dia);

    @Insert
    void insert(Walk w);

    @Update
    void atualizar(Walk w);

    @Delete
    void apagar(Walk w);
}