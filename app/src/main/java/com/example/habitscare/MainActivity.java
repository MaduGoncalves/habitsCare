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
 * e as telas Hoje / Perfil (com a barra inferior) quando há.
 */
public class MainActivity extends AppCompatActivity {

    // Posição de cada item no menu da barra inferior (res/menu).
    // Ajuste se a ordem do seu menu for diferente.
    private static final int POS_HOJE = 0;
    private static final int POS_SOBRE = 1;
    private static final int POS_PERFIL = 2;

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

        // A foto redonda do cabeçalho leva para o Perfil
        binding.ivFotoHeader.setOnClickListener(v -> mostrarPerfil());

        // Decide a tela pelo usuário logado. Quando o login é feito (ou o usuário sai),
        // o banco avisa e a tela troca sozinha.
        AppDatabase.get(this).userDao().getUsuarioAtivo().observe(this, user -> {
            Fragment atual = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);

            if (user != null) {
                // Foto do usuário no cabeçalho (atualiza sozinha quando ele troca a foto no Perfil)
                FotoUtils.carregar(binding.ivFotoHeader, user.getPhoto());

                // IMPORTANTE: este observer também dispara quando o usuário edita nome/e-mail/senha/foto.
                // Só vai para o Hoje se ainda não estiver numa tela logada (Hoje ou Perfil),
                // senão salvar o perfil jogaria a pessoa de volta para o Hoje.
                if (atual == null || atual instanceof LoginFragment) {
                    mostrarHoje();
                }
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
        mostrarBarras();
        marcarItem(POS_HOJE);

        Fragment atual = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (atual instanceof HojeFragment) return;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new HojeFragment())
                .commit();
    }

    private void mostrarPerfil() {
        mostrarBarras();
        marcarItem(POS_PERFIL);

        Fragment atual = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (atual instanceof PerfilFragment) return;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new PerfilFragment())
                .commit();
    }

    private void mostrarBarras() {
        binding.bottomNav.setVisibility(View.VISIBLE);
        binding.appBarLayout.setVisibility(View.VISIBLE);
    }

    private void marcarItem(int posicao) {
        Menu menu = binding.bottomNav.getMenu();
        if (posicao < menu.size()) {
            menu.getItem(posicao).setChecked(true);
        }
    }

    private void configurarBarraInferior() {
        Menu menu = binding.bottomNav.getMenu();

        binding.bottomNav.setOnItemSelectedListener(item -> {
            // Descobre a posição do item no menu
            int posicao = -1;
            for (int i = 0; i < menu.size(); i++) {
                if (menu.getItem(i).getItemId() == item.getItemId()) {
                    posicao = i;
                    break;
                }
            }

            if (posicao == POS_HOJE) {
                mostrarHoje();
                return true;
            }
            if (posicao == POS_PERFIL) {
                mostrarPerfil();
                return true;
            }
            if (posicao == POS_SOBRE) {
                startActivity(new Intent(this, AboutProjectActivity.class));
                return false; // "Sobre" abre outra tela, então não fica marcado aqui
            }
            return false;
        });
    }
}