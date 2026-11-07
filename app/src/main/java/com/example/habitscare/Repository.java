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

    // Metas e limites
    public static final double META_AGUA_L = 2.0;
    public static final double MAX_AGUA_L = 10.0;
    public static final double META_KM = 5.0;
    public static final double MAX_KM = 100.0;
    public static final float META_HORAS_SONO = 8.0f;

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

    public UserDao getUserDao() {
        return userDao;
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

    public LiveData<Water> getAguaDoDia(int idUsuario, String dia) {
        return waterDao.getAguaDoDia(idUsuario, dia);
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

    /** Soma (ou subtrai) litros de água no registro do dia. Cria o registro se ainda não existir. */
    public void alterarAgua(int idUsuario, String dia, double deltaLitros) {
        executor.execute(() -> {
            Water w = waterDao.getAguaDoDiaSync(idUsuario, dia);
            if (w == null) {
                if (deltaLitros <= 0) return;
                w = new Water(idUsuario, limitar(deltaLitros, MAX_AGUA_L), dia);
                waterDao.insert(w);
            } else {
                w.setQuantidadeL(limitar(w.getQuantidadeL() + deltaLitros, MAX_AGUA_L));
                waterDao.atualizar(w);
            }
        });
    }

    //SLEEP
    public LiveData<List<Sleep>> getAllSleep() {
        return sleepDao.getAll();
    }

    public LiveData<List<Sleep>> getSleepByUsers(int idUser) {
        return sleepDao.getByIdUser(idUser);
    }

    public LiveData<Sleep> getSleepDoDia(int idUsuario, String dia) {
        return sleepDao.getSleepDoDia(idUsuario, dia);
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

    /** Salva os horários de sono do dia (cria o registro ou atualiza o existente). */
    public void salvarSono(int idUsuario, String dia, String horarioDormir, String horarioAcordar) {
        executor.execute(() -> {
            Sleep s = sleepDao.getSleepDoDiaSync(idUsuario, dia);
            if (s == null) {
                sleepDao.insert(new Sleep(idUsuario, horarioDormir, horarioAcordar, dia));
            } else {
                s.setHorarioDormir(horarioDormir);
                s.setHorarioAcordar(horarioAcordar);
                sleepDao.atualizar(s);
            }
        });
    }

    /** Apaga o registro de sono do dia, se existir. */
    public void limparSono(int idUsuario, String dia) {
        executor.execute(() -> {
            Sleep s = sleepDao.getSleepDoDiaSync(idUsuario, dia);
            if (s != null) sleepDao.apagar(s);
        });
    }

    /** Calcula as horas dormidas a partir de "HH:mm" (considera virada de meia-noite). */
    public static float calcularHorasSono(String horarioDormir, String horarioAcordar) {
        try {
            int dormir = paraMinutos(horarioDormir);
            int acordar = paraMinutos(horarioAcordar);
            int diff = acordar - dormir;
            if (diff <= 0) diff += 24 * 60;
            return diff / 60f;
        } catch (Exception e) {
            return 0f;
        }
    }

    private static int paraMinutos(String hhmm) {
        String[] p = hhmm.split(":");
        return Integer.parseInt(p[0].trim()) * 60 + Integer.parseInt(p[1].trim());
    }

    //WALK
    public LiveData<List<Walk>> getAllWalk() {
        return walkDao.getAll();
    }

    public LiveData<List<Walk>> getWalkByUsers(int idUser) {
        return walkDao.getByIdUser(idUser);
    }

    public LiveData<Walk> getWalkDoDia(int idUsuario, String dia) {
        return walkDao.getWalkDoDia(idUsuario, dia);
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

    /** Soma (ou subtrai) quilômetros no registro do dia. Cria o registro se ainda não existir. */
    public void alterarWalk(int idUsuario, String dia, double deltaKm) {
        executor.execute(() -> {
            Walk w = walkDao.getWalkDoDiaSync(idUsuario, dia);
            if (w == null) {
                if (deltaKm <= 0) return;
                w = new Walk(idUsuario, limitar(deltaKm, MAX_KM), dia);
                walkDao.insert(w);
            } else {
                w.setKm(limitar(w.getKm() + deltaKm, MAX_KM));
                walkDao.atualizar(w);
            }
        });
    }

    // Mantém o valor entre 0 e o máximo, com 2 casas decimais (evita 0.30000000000000004)
    private static double limitar(double valor, double max) {
        double v = Math.max(0.0, Math.min(max, valor));
        return Math.round(v * 100.0) / 100.0;
    }
}