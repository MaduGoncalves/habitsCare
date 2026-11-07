package com.example.habitscare;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.habitscare.model.Sleep;
import com.example.habitscare.model.Walk;
import com.example.habitscare.model.Water;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * Junta água, caminhada e sono do usuário logado em uma lista de dias
 * (do mais recente para o mais antigo). Só entram dias que tiveram algum registro.
 */
public class HistoricoViewModel extends AndroidViewModel {

    private final Repository repository;
    private final LiveData<List<HistoricoDia>> historico;

    public HistoricoViewModel(@NonNull Application application) {
        super(application);
        repository = new Repository(application);

        // Só muda quando o usuário muda (não quando ele edita nome/foto)
        LiveData<Integer> idUsuario = Transformations.distinctUntilChanged(
                Transformations.map(repository.getUsuarioAtivo(), user -> user == null ? -1 : user.id));

        historico = Transformations.switchMap(idUsuario, id -> {
            if (id == null || id == -1) {
                return new MutableLiveData<List<HistoricoDia>>(Collections.<HistoricoDia>emptyList());
            }
            return montar(id);
        });
    }

    public LiveData<List<HistoricoDia>> getHistorico() {
        return historico;
    }

    // ---------- Montagem ----------

    private static class Estado {
        List<Water> aguas;
        List<Walk> caminhadas;
        List<Sleep> sonos;
    }

    private static class Acumulador {
        double litros;
        double km;
        String dormir;
        String acordar;
    }

    private LiveData<List<HistoricoDia>> montar(int idUsuario) {
        final MediatorLiveData<List<HistoricoDia>> resultado = new MediatorLiveData<>();
        final Estado estado = new Estado();

        resultado.addSource(repository.getWaterByUsers(idUsuario), lista -> {
            estado.aguas = lista;
            combinar(resultado, estado);
        });
        resultado.addSource(repository.getWalkByUsers(idUsuario), lista -> {
            estado.caminhadas = lista;
            combinar(resultado, estado);
        });
        resultado.addSource(repository.getSleepByUsers(idUsuario), lista -> {
            estado.sonos = lista;
            combinar(resultado, estado);
        });
        return resultado;
    }

    private void combinar(MediatorLiveData<List<HistoricoDia>> destino, Estado e) {
        // Espera as três consultas responderem pela primeira vez
        if (e.aguas == null || e.caminhadas == null || e.sonos == null) return;

        // Chave "yyyy-MM-dd": ordem alfabética = ordem cronológica. Invertida = mais recente primeiro.
        TreeMap<String, Acumulador> mapa = new TreeMap<>(Collections.reverseOrder());

        for (Water w : e.aguas) {
            if (w.getDia() != null && w.getQuantidadeL() > 0) {
                acumulador(mapa, w.getDia()).litros += w.getQuantidadeL();
            }
        }
        for (Walk w : e.caminhadas) {
            if (w.getDia() != null && w.getKm() > 0) {
                acumulador(mapa, w.getDia()).km += w.getKm();
            }
        }
        for (Sleep s : e.sonos) {
            if (s.getDia() != null && s.getHorarioDormir() != null && s.getHorarioAcordar() != null) {
                Acumulador a = acumulador(mapa, s.getDia());
                a.dormir = s.getHorarioDormir();
                a.acordar = s.getHorarioAcordar();
            }
        }

        List<HistoricoDia> lista = new ArrayList<>();
        for (java.util.Map.Entry<String, Acumulador> entrada : mapa.entrySet()) {
            Acumulador a = entrada.getValue();
            lista.add(new HistoricoDia(entrada.getKey(), a.litros, a.km, a.dormir, a.acordar));
        }
        destino.setValue(lista);
    }

    private static Acumulador acumulador(TreeMap<String, Acumulador> mapa, String dia) {
        Acumulador a = mapa.get(dia);
        if (a == null) {
            a = new Acumulador();
            mapa.put(dia, a);
        }
        return a;
    }
}