package com.example.habitscare.dados_dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.habitscare.model.Water;

import java.util.List;

@Dao
public interface WaterDao {
    @Query("SELECT * FROM water")
    LiveData<List<Water>> getAll();

    @Query("SELECT * FROM water WHERE idUsuario = :idUsuario")
    LiveData<List<Water>> getByIdUser(int idUsuario);

    // Este é o método que busca a água do dia para o utilizadoo
    // Observado pela tela "Hoje"
    @Query("SELECT * FROM water WHERE idUsuario = :idUsuario AND dia = :dataHoje LIMIT 1")
    LiveData<Water> getAguaDoDia(int idUsuario, String dataHoje);
    @Query("SELECT * FROM water WHERE idUsuario = :idUsuario AND dia = :dia LIMIT 1")
    Water getAguaDoDiaSync(int idUsuario, String dia);

    @Insert
    void insert(Water w);

    @Update
    void atualizar(Water w);

    @Delete
    void apagar(Water w);
}