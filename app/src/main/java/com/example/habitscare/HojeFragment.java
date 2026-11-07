package com.example.habitscare;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import com.example.habitscare.databinding.FragmentHojeBinding;
import com.example.habitscare.model.Sleep;
import com.example.habitscare.model.Walk;
import com.example.habitscare.model.Water;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HojeFragment extends Fragment {

    private static final double COPO_L = 0.25;      // cada clique = 1 copo de 250 ml
    private static final double INCREMENTO_KM = 0.5; // cada clique = 0,5 km

    private FragmentHojeBinding binding;
    private Repository repository;
    private int idUsuarioLogado = -1;
    private String dataHoje;

    // LiveData guardados para remover os observers antes de observar de novo
    private LiveData<Water> aguaLive;
    private LiveData<Walk> walkLive;
    private LiveData<Sleep> sleepLive;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHojeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new Repository(requireActivity().getApplication());
        dataHoje = calcularDataHoje();

        // Usuário ativo logado
        repository.getUsuarioAtivo().observe(getViewLifecycleOwner(), user -> {
            if (user == null || binding == null) return;

            binding.tvSaudacao.setText("Olá, " + user.user_name + "! 👋");

            // Só recarrega os hábitos se o usuário realmente mudou
            if (user.id != idUsuarioLogado) {
                idUsuarioLogado = user.id;
                carregarHabitosDoDia();
            }
        });

        configurarBotoes();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Se virou o dia com o app em segundo plano, recarrega
        String hoje = calcularDataHoje();
        if (dataHoje != null && !hoje.equals(dataHoje)) {
            dataHoje = hoje;
            carregarHabitosDoDia();
        }
    }

    private String calcularDataHoje() {
        // Locale.US garante dígitos 0-9 na chave de data usada no banco
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private void carregarHabitosDoDia() {
        if (idUsuarioLogado == -1 || binding == null) return;

        // Remove observers antigos para não duplicar callbacks
        if (aguaLive != null) aguaLive.removeObservers(getViewLifecycleOwner());
        if (walkLive != null) walkLive.removeObservers(getViewLifecycleOwner());
        if (sleepLive != null) sleepLive.removeObservers(getViewLifecycleOwner());

        // Água (litros)
        aguaLive = repository.getAguaDoDia(idUsuarioLogado, dataHoje);
        aguaLive.observe(getViewLifecycleOwner(), water -> {
            double litros = (water != null) ? water.getQuantidadeL() : 0.0;
            if (binding != null) {
                binding.tvProgressoAgua.setText(String.format(Locale.getDefault(),
                        "%.2f/%.1f L", litros, Repository.META_AGUA_L));
            }
        });

        // Caminhada (km)
        walkLive = repository.getWalkDoDia(idUsuarioLogado, dataHoje);
        walkLive.observe(getViewLifecycleOwner(), walk -> {
            double km = (walk != null) ? walk.getKm() : 0.0;
            if (binding != null) {
                binding.tvProgressoWalk.setText(String.format(Locale.getDefault(),
                        "%.1f/%.1f km", km, Repository.META_KM));
            }
        });

        // Sono (horário de dormir e acordar)
        sleepLive = repository.getSleepDoDia(idUsuarioLogado, dataHoje);
        sleepLive.observe(getViewLifecycleOwner(), sleep -> {
            if (binding == null) return;
            if (sleep == null || sleep.getHorarioDormir() == null || sleep.getHorarioAcordar() == null) {
                binding.tvProgressoSleep.setText(String.format(Locale.getDefault(),
                        "0.0/%.1f hrs", Repository.META_HORAS_SONO));
            } else {
                float horas = Repository.calcularHorasSono(sleep.getHorarioDormir(), sleep.getHorarioAcordar());
                binding.tvProgressoSleep.setText(String.format(Locale.getDefault(),
                        "%s → %s (%.1f/%.1f hrs)",
                        sleep.getHorarioDormir(), sleep.getHorarioAcordar(),
                        horas, Repository.META_HORAS_SONO));
            }
        });
    }

    private void configurarBotoes() {
        // ÁGUA
        binding.btnAdicionarAgua.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) repository.alterarAgua(idUsuarioLogado, dataHoje, +COPO_L);
        });
        binding.btnRemoverAgua.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) repository.alterarAgua(idUsuarioLogado, dataHoje, -COPO_L);
        });

        // CAMINHADA
        binding.btnAdicionarWalk.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) repository.alterarWalk(idUsuarioLogado, dataHoje, +INCREMENTO_KM);
        });
        binding.btnRemoverWalk.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) repository.alterarWalk(idUsuarioLogado, dataHoje, -INCREMENTO_KM);
        });

        // SONO: "+" escolhe horário de dormir e acordar; "-" apaga o registro do dia
        binding.btnAdicionarSleep.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) escolherHorariosSono();
        });
        binding.btnRemoverSleep.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) repository.limparSono(idUsuarioLogado, dataHoje);
        });
    }

    private void escolherHorariosSono() {
        final int usuario = idUsuarioLogado;
        final String dia = dataHoje;

        new TimePickerDialog(requireContext(), (view1, horaDormir, minDormir) -> {
            final String dormir = formatarHorario(horaDormir, minDormir);

            new TimePickerDialog(requireContext(), (view2, horaAcordar, minAcordar) -> {
                String acordar = formatarHorario(horaAcordar, minAcordar);
                repository.salvarSono(usuario, dia, dormir, acordar);
            }, 7, 0, true).show();

        }, 23, 0, true).show();
    }

    private String formatarHorario(int hora, int minuto) {
        return String.format(Locale.US, "%02d:%02d", hora, minuto);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}