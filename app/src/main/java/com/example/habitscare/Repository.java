package com.example.habitscare;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.habitscare.dados_dao.SleepDao;
import com.example.habitscare.dados_dao.UserDao;
import com.example.habitscare.dados_dao.WalkDao;
import com.example.habitscare.dados_dao.WaterDao;
import com.example.habitscare.model.Sleep;
import com.example.habitscare.model.User;
import com.example.habitscare.model.Walk;
import com.example.habitscare.model.Water;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Repository {
    private final UserDao userDao;
    private final WaterDao waterDao;
    private final SleepDao sleepDao;
    private final WalkDao walkDao;
    private final ExecutorService executor;
    public Repository(Application application) {
        AppDatabase db = AppDatabase.get(application);

        this.userDao = db.userDao();
        this.waterDao = db.waterDao();
        this.sleepDao = db.sleepDao();
        this.walkDao = db.walkDao();

        this.executor = Executors.newSingleThreadExecutor();
    }

    //USERS
    public LiveData<User> getUsuarioAtivo() {
        return userDao.getUsuarioAtivo();
    }
    public void logout(User user) {
        executor.execute(() -> {
            if (user != null) {
                user.isLogged = false; // Altera a flag para deslogado
                userDao.atualizar(user); // Salva no banco
            }
        });
    }

    //WATER
    public LiveData<List<Water>> getAllWater() {
        return waterDao.getAll();
    }

    public LiveData<List<Water>> getWaterByUsers(int idUser) {
        return waterDao.getByIdUser(idUser);
    }

    public void insertWater(Water water) {
        executor.execute(() -> waterDao.insert(water));
    }

    public void updateWater(Water water) {
        executor.execute(() -> waterDao.atualizar(water));
    }

    public void deleteWater(Water water) {
        executor.execute(() -> waterDao.apagar(water));
    }

    //SLEEP
    public LiveData<List<Sleep>> getAllSleep() {
        return sleepDao.getAll();
    }
    public LiveData<List<Sleep>> getSleepByUsers(int idUser) {
        return sleepDao.getByIdUser(idUser);
    }
    public void insertSleep(Sleep sleep) {
        executor.execute(() -> sleepDao.insert(sleep));
    }
    public void updateSleep(Sleep sleep) {
        executor.execute(() -> sleepDao.atualizar(sleep));
    }
    public void deleteSleep(Sleep sleep) {
        executor.execute(() -> sleepDao.apagar(sleep));
    }

    //WALK
    public LiveData<List<Walk>> getAllWalk() {
        return walkDao.getAll();
    }
    public LiveData<List<Walk>> getWalkByUsers(int idUser) {
        return walkDao.getByIdUser(idUser);
    }
    public void insertWalk(Walk walk) {
        executor.execute(() -> walkDao.insert(walk));
    }
    public void updateWalk(Walk walk) {
        executor.execute(() -> walkDao.atualizar(walk));
    }
    public void deleteWalk(Walk walk) {
        executor.execute(() -> walkDao.apagar(walk));
    }
}
