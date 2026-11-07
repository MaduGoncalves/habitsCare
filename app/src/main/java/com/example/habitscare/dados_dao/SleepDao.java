package com.example.habitscare.dados_dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.habitscare.model.Sleep;

import java.util.List;

@Dao
public interface SleepDao {
    @Query("SELECT * FROM sleep")
    LiveData<List<Sleep>> getAll();

    @Query("SELECT * FROM sleep WHERE idUsuario = :idUsuario")
    LiveData<List<Sleep>> getByIdUser(int idUsuario);

    // Observado pela tela "Hoje"
    @Query("SELECT * FROM sleep WHERE idUsuario = :idUsuario AND dia = :dataHoje LIMIT 1")
    LiveData<Sleep> getSleepDoDia(int idUsuario, String dataHoje);

    // Leitura síncrona, usada dentro do executor do Repository
    @Query("SELECT * FROM sleep WHERE idUsuario = :idUsuario AND dia = :dia LIMIT 1")
    Sleep getSleepDoDiaSync(int idUsuario, String dia);

    @Insert
    void insert(Sleep s);

    @Update
    void atualizar(Sleep s);

    @Delete
    void apagar(Sleep s);
}