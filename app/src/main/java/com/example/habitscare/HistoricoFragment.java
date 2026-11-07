package com.example.habitscare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.habitscare.databinding.FragmentHistoricoBinding;

public class HistoricoFragment extends Fragment {

    private FragmentHistoricoBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoricoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        HistoricoAdapter adapter = new HistoricoAdapter();
        binding.rvHistorico.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistorico.setAdapter(adapter);

        HistoricoViewModel viewModel = new ViewModelProvider(this).get(HistoricoViewModel.class);
        viewModel.getHistorico().observe(getViewLifecycleOwner(), lista -> {
            if (binding == null) return;
            adapter.submitList(lista);
            boolean vazio = (lista == null || lista.isEmpty());
            binding.tvVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}