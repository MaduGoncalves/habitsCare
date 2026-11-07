package com.example.habitscare;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.habitscare.dados_dao.UserDao;
import com.example.habitscare.databinding.FragmentPerfilBinding;
import com.example.habitscare.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding binding;
    private UserDao userDao;
    private User usuarioAtual;              // usuário logado, como está salvo no banco
    private boolean camposPreenchidos = false;
    private byte[] novaFoto = null;         // foto escolhida mas ainda não salva

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Lembretes (notificações e alarmes) + permissões. Também precisa ser campo da classe.
    private final LembretesController lembretes = new LembretesController(this);

    // Câmera ou galeria (precisa ser criado como campo, antes de a tela iniciar)
    private final FotoPicker fotoPicker = new FotoPicker(this, bytes -> {
        if (binding == null) return;
        novaFoto = bytes;
        FotoUtils.carregar(binding.ivFoto, bytes);   // prévia imediata
        atualizarBotaoSalvar();
    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userDao = AppDatabase.get(requireContext()).userDao();

        // Salvar começa desabilitado: só habilita quando algo for editado
        binding.btnSalvar.setEnabled(false);

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (binding == null) return;
                binding.tilNome.setError(null);
                binding.tilEmail.setError(null);
                atualizarBotaoSalvar();
            }
        };
        binding.etNome.addTextChangedListener(watcher);
        binding.etEmail.addTextChangedListener(watcher);
        binding.etSenha.addTextChangedListener(watcher);

        // Carrega o usuário logado
        userDao.getUsuarioAtivo().observe(getViewLifecycleOwner(), user -> {
            if (user == null || binding == null) return;
            usuarioAtual = user;

            binding.tvNomeTitulo.setText(user.getUser_name());
            binding.tvEmailSubtitulo.setText(user.getEmail());

            // Se há foto escolhida e ainda não salva, ela continua sendo exibida
            if (novaFoto == null) {
                FotoUtils.carregar(binding.ivFoto, user.getPhoto());
            }

            // Preenche os campos só na primeira vez, para não sobrescrever o que a pessoa está digitando
            if (!camposPreenchidos) {
                camposPreenchidos = true;
                binding.etNome.setText(user.getUser_name());
                binding.etEmail.setText(user.getEmail());
            }
            atualizarBotaoSalvar();
        });

        // Tocar na foto ou em "Alterar foto": escolher entre câmera e galeria
        binding.ivFoto.setOnClickListener(v -> fotoPicker.mostrarOpcoes());
        binding.btnAlterarFoto.setOnClickListener(v -> fotoPicker.mostrarOpcoes());

        binding.btnSalvar.setOnClickListener(v -> salvar());
        binding.btnSair.setOnClickListener(v -> confirmarSaida());

        lembretes.vincular(binding);
    }

    @Override
    public void onResume() {
        super.onResume();
        lembretes.aoRetomar();   // reagenda e atualiza depois de voltar das configurações do sistema
    }

    // ---------- Habilitar / desabilitar Salvar ----------

    private String nomeDigitado()  { return binding.etNome.getText() == null ? "" : binding.etNome.getText().toString().trim(); }
    private String emailDigitado() { return binding.etEmail.getText() == null ? "" : binding.etEmail.getText().toString().trim(); }
    private String senhaDigitada() { return binding.etSenha.getText() == null ? "" : binding.etSenha.getText().toString(); }

    private boolean houveAlteracao() {
        if (usuarioAtual == null) return false;

        String nomeSalvo = usuarioAtual.getUser_name() == null ? "" : usuarioAtual.getUser_name();
        String emailSalvo = usuarioAtual.getEmail() == null ? "" : usuarioAtual.getEmail();

        boolean nomeMudou = !nomeDigitado().equals(nomeSalvo);
        boolean emailMudou = !emailDigitado().equals(emailSalvo);
        boolean senhaMudou = !senhaDigitada().isEmpty();   // vazio = manter a senha atual
        boolean fotoMudou = novaFoto != null;

        return nomeMudou || emailMudou || senhaMudou || fotoMudou;
    }

    private void atualizarBotaoSalvar() {
        if (binding == null) return;
        binding.btnSalvar.setEnabled(houveAlteracao());
    }

    // ---------- Salvar ----------

    private boolean validar(String nome, String email) {
        boolean ok = true;

        if (nome.isEmpty()) {
            binding.tilNome.setError("Informe o nome");
            ok = false;
        }
        if (email.isEmpty()) {
            binding.tilEmail.setError("Informe o e-mail");
            ok = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("E-mail inválido");
            ok = false;
        }
        // Senha: sem tamanho mínimo (igual ao cadastro). Campo vazio = manter a atual.
        return ok;
    }

    private void salvar() {
        if (usuarioAtual == null) return;

        final String nome = nomeDigitado();
        final String email = emailDigitado();
        final String senha = senhaDigitada();
        final byte[] fotoEscolhida = novaFoto;

        if (!validar(nome, email)) return;

        final Activity activity = requireActivity();
        final User base = usuarioAtual;

        binding.btnSalvar.setEnabled(false);

        executor.execute(() -> {
            // O e-mail é único: não pode ser de outro usuário
            User existente = userDao.getUserByEmail(email);
            if (existente != null && existente.getId() != base.getId()) {
                activity.runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.tilEmail.setError("Este e-mail já está em uso");
                    atualizarBotaoSalvar();
                });
                return;
            }

            // Senha nova: gera um novo hash (BCrypt com salt). A senha digitada nunca é gravada.
            // Campo vazio: mantém o hash que já estava salvo.
            String senhaFinal = senha.isEmpty() ? base.getPassword() : SecurityUtils.hashSenha(senha);
            byte[] fotoFinal = (fotoEscolhida != null) ? fotoEscolhida : base.getPhoto();

            User atualizado = new User(nome, senhaFinal, email, fotoFinal, base.isLogged());
            atualizado.setId(base.getId());
            userDao.atualizar(atualizado);

            activity.runOnUiThread(() -> {
                novaFoto = null;
                if (binding != null) {
                    binding.etSenha.setText("");   // limpa o campo
                    binding.etSenha.clearFocus();
                    atualizarBotaoSalvar();
                }
                Toast.makeText(activity, "Perfil atualizado", Toast.LENGTH_SHORT).show();
                // O LiveData emite o usuário novo (o cabeçalho da MainActivity também atualiza a foto)
            });
        });
    }

    // ---------- Sair ----------

    private void confirmarSaida() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Sair")
                .setMessage("Deseja sair da sua conta?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sair", (d, w) -> sair())
                .show();
    }

    private void sair() {
        // Basta deslogar: a MainActivity observa o usuário ativo e troca sozinha para o Login
        executor.execute(() -> userDao.logoutTodos());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        lembretes.desvincular();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}