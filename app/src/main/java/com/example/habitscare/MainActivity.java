package com.example.habitscare;

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
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import android.view.MenuItem;

/**
 * Tela única do app: mostra o Login quando não há usuário logado
 * e as telas Hoje / Perfil (com a barra inferior) quando há.
 */
public class MainActivity extends AppCompatActivity {

    // Posição de cada item no menu da barra inferior (res/menu).
    // Ajuste se a ordem do seu menu for diferente.
    private static final int POS_HOJE = 0;
    private static final int POS_HISTORICO = 1;
    private static final int POS_PERFIL = 2;

    private ActivityMainBinding binding;

    // Para reagir só quando o login/logout muda (e não a cada edição do perfil)
    private Boolean sessaoAnterior = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Isso garante que os botões fiquem colados no limite seguro do celular, sem sobrepor
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        configurarBarraInferior();
        configurarToolbar();

        // Canais de notificação (Android 8+): criar é seguro mesmo se já existirem
        NotificationHelper.criarCanais(this);

        // A foto redonda do cabeçalho leva para o Perfil
        binding.ivFotoHeader.setOnClickListener(v -> mostrarPerfil());

        // Decide a tela pelo usuário logado. Quando o login é feito (ou o usuário sai),
        // o banco avisa e a tela troca sozinha.
        AppDatabase.get(this).userDao().getUsuarioAtivo().observe(this, user -> {
            Fragment atual = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);

            // Login: liga os lembretes salvos. Logout: cancela todos os alarmes.
            boolean logado = (user != null);
            if (sessaoAnterior == null || sessaoAnterior != logado) {
                sessaoAnterior = logado;
                LembretesPrefs.setSessaoAtiva(this, logado);
                if (logado) {
                    AlarmScheduler.reagendarTudo(this);
                } else {
                    AlarmScheduler.cancelarTudo(this);
                }
            }

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

    private void mostrarHistorico() {
        mostrarBarras();
        marcarItem(POS_HISTORICO);

        Fragment atual = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (atual instanceof HistoricoFragment) return;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new HistoricoFragment())
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
            if (posicao == POS_HISTORICO) {
                mostrarHistorico();
                return true;
            }
            return false;
        });
    }

    private void configurarToolbar() {
        // 1. Verifica qual tema está ativo ao abrir a tela
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            binding.ivThemeToggle.setImageResource(R.drawable.sun); // Escuro = Sol
        } else {
            binding.ivThemeToggle.setImageResource(R.drawable.moon); // Claro = Lua
        }

        // 2. Ação de clique na imagem
        binding.ivThemeToggle.setOnClickListener(v -> {
            int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (mode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Muda pro claro
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // Muda pro escuro
            }
        });
    }
}