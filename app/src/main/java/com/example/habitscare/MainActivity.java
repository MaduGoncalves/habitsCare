package com.example.habitscare;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.habitscare.databinding.ActivityMainBinding;

/**
 * Tela única do app: mostra o Login quando não há usuário logado
 * e a tela Hoje (com a barra inferior) quando há.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Ajusta as margens para não sobrepor a barra de status ou de navegação do celular
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        configurarBarraInferior();

        // Decide a tela pelo usuário logado. Quando o login é feito (ou o usuário sai),
        // o banco avisa e a tela troca sozinha.
        AppDatabase.get(this).userDao().getUsuarioAtivo().observe(this, user -> {
            if (user != null) {
                mostrarHoje();
            } else {
                mostrarLogin();
            }
        });
    }

    private void mostrarLogin() {
        // Sem login: sem barra de navegação nem toolbar. Só Login, Cadastro e Sobre.
        binding.bottomNav.setVisibility(View.GONE);
        binding.appBarLayout.setVisibility(View.GONE);

        Fragment atual = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (atual instanceof LoginFragment) return;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new LoginFragment())
                .commit();
    }

    private void mostrarHoje() {
        binding.bottomNav.setVisibility(View.VISIBLE);
        binding.appBarLayout.setVisibility(View.VISIBLE);

        Menu menu = binding.bottomNav.getMenu();
        if (menu.size() > 0) {
            menu.getItem(0).setChecked(true);
        }

        Fragment atual = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (atual instanceof HojeFragment) return;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new HojeFragment())
                .commit();
    }

    private void configurarBarraInferior() {
        Menu menu = binding.bottomNav.getMenu();

        binding.bottomNav.setOnItemSelectedListener(item -> {
            // Descobre a posição do item no menu (1º item = Hoje, 2º item = Sobre)
            int posicao = -1;
            for (int i = 0; i < menu.size(); i++) {
                if (menu.getItem(i).getItemId() == item.getItemId()) {
                    posicao = i;
                    break;
                }
            }

            if (posicao == 0) {
                mostrarHoje();
                return true;
            }
            if (posicao == 1) {
                startActivity(new Intent(this, AboutProjectActivity.class));
                return false; // "Sobre" abre outra tela, então não fica marcado aqui
            }
            return false;
        });
    }
}