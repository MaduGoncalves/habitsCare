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

    // A variável fixa COPO_L foi removida para dar lugar à variável dinâmica
    private double quantidadeLitrosSelecionada = 0.25; // Começa valendo 1 copo (250 ml = 0.25 L)
    private static final double INCREMENTO_KM = 0.5; // cada clique = 0,5 km

    private FragmentHojeBinding binding;
    private Repository repository;
    private int idUsuarioLogado = -1;
    private String dataHoje;

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

        repository.getUsuarioAtivo().observe(getViewLifecycleOwner(), user -> {
            if (user == null || binding == null) return;

            binding.tvSaudacao.setText("Olá, " + user.user_name + "!");

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
        String hoje = calcularDataHoje();
        if (dataHoje != null && !hoje.equals(dataHoje)) {
            dataHoje = hoje;
            carregarHabitosDoDia();
        }
    }

    private String calcularDataHoje() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private void carregarHabitosDoDia() {
        if (idUsuarioLogado == -1 || binding == null) return;

        if (aguaLive != null) aguaLive.removeObservers(getViewLifecycleOwner());
        if (walkLive != null) walkLive.removeObservers(getViewLifecycleOwner());
        if (sleepLive != null) sleepLive.removeObservers(getViewLifecycleOwner());

        // Água
        aguaLive = repository.getAguaDoDia(idUsuarioLogado, dataHoje);
        aguaLive.observe(getViewLifecycleOwner(), water -> {
            double litros = (water != null) ? water.getQuantidadeL() : 0.0;
            if (binding != null) {
                binding.tvProgressoAgua.setText(String.format(Locale.getDefault(),
                        "%.2f/%.1f L", litros, Repository.META_AGUA_L));
            }
        });

        // Caminhada
        walkLive = repository.getWalkDoDia(idUsuarioLogado, dataHoje);
        walkLive.observe(getViewLifecycleOwner(), walk -> {
            double km = (walk != null) ? walk.getKm() : 0.0;
            if (binding != null) {
                binding.tvProgressoWalk.setText(String.format(Locale.getDefault(),
                        "%.1f/%.1f km", km, Repository.META_KM));
            }
        });

        // Sono
        // Sono
        sleepLive = repository.getSleepDoDia(idUsuarioLogado, dataHoje);
        sleepLive.observe(getViewLifecycleOwner(), sleep -> {
            if (binding == null) return;
            if (sleep == null || sleep.getHorarioDormir() == null || sleep.getHorarioAcordar() == null) {
                binding.tvProgressoSleep.setText(String.format(Locale.getDefault(),
                        "0.0/%.1f hrs", Repository.META_HORAS_SONO));
                // Zera os relógios
                binding.tvHoraDormir.setText("00:00");
                binding.tvHoraAcordar.setText("00:00");
            } else {
                float horas = Repository.calcularHorasSono(sleep.getHorarioDormir(), sleep.getHorarioAcordar());
                binding.tvProgressoSleep.setText(String.format(Locale.getDefault(),
                        "%.1f/%.1f hrs", horas, Repository.META_HORAS_SONO));
                // Atualiza os relógios com as horas salvas
                binding.tvHoraDormir.setText(sleep.getHorarioDormir());
                binding.tvHoraAcordar.setText(sleep.getHorarioAcordar());
            }
        });
    }

    private void configurarBotoes() {
        // === NOVA LÓGICA DOS COPOS ===
        // Configura o clique em cada ícone de copo
        binding.cbCopo1.setOnClickListener(v -> selecionarCopos(1));
        binding.cbCopo2.setOnClickListener(v -> selecionarCopos(2));
        binding.cbCopo3.setOnClickListener(v -> selecionarCopos(3));
        binding.cbCopo4.setOnClickListener(v -> selecionarCopos(4));
        binding.cbCopo5.setOnClickListener(v -> selecionarCopos(5));

        // Inicializa a tela com 1 copo selecionado por padrão
        selecionarCopos(1);

        // BOTÕES DE ADICIONAR E REMOVER ÁGUA USANDO A NOVA VARIÁVEL
        binding.btnAdicionarAgua.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) {
                repository.alterarAgua(idUsuarioLogado, dataHoje, +quantidadeLitrosSelecionada);
            }
        });
        binding.btnRemoverAgua.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) {
                repository.alterarAgua(idUsuarioLogado, dataHoje, -quantidadeLitrosSelecionada);
            }
        });

        // CAMINHADA
        binding.btnAdicionarWalk.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) repository.alterarWalk(idUsuarioLogado, dataHoje, +INCREMENTO_KM);
        });
        binding.btnRemoverWalk.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) repository.alterarWalk(idUsuarioLogado, dataHoje, -INCREMENTO_KM);
        });

        // === SONO ===
        // Ao clicar no texto "00:00" de Dormir
        binding.tvHoraDormir.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hora, minuto) -> {
                binding.tvHoraDormir.setText(formatarHorario(hora, minuto));
            }, 22, 0, true).show(); // Começa às 22:00 por padrão
        });

        // Ao clicar no texto "00:00" de Acordar
        binding.tvHoraAcordar.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hora, minuto) -> {
                binding.tvHoraAcordar.setText(formatarHorario(hora, minuto));
            }, 7, 0, true).show(); // Começa às 07:00 por padrão
        });

        // Botão Salvar
        binding.btnSalvarSono.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) {
                String dormir = binding.tvHoraDormir.getText().toString();
                String acordar = binding.tvHoraAcordar.getText().toString();
                repository.salvarSono(idUsuarioLogado, dataHoje, dormir, acordar);
            }
        });

        // Botão Apagar
        binding.btnRemoverSleep.setOnClickListener(v -> {
            if (idUsuarioLogado != -1) repository.limparSono(idUsuarioLogado, dataHoje);
        });
    }

    // Método que faz a mágica de acender/apagar os copos e calcular os litros
    private void selecionarCopos(int quantidade) {
        // Multiplica a quantidade de copos selecionados por 0.25L (250ml)
        quantidadeLitrosSelecionada = quantidade * 0.25;

        // Acende ou apaga os CheckBoxes dependendo de qual foi clicado
        binding.cbCopo1.setChecked(quantidade >= 1);
        binding.cbCopo2.setChecked(quantidade >= 2);
        binding.cbCopo3.setChecked(quantidade >= 3);
        binding.cbCopo4.setChecked(quantidade >= 4);
        binding.cbCopo5.setChecked(quantidade >= 5);
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