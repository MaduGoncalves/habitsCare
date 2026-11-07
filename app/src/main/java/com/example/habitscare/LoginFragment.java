package com.example.habitscare;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.habitscare.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginFragment extends Fragment {

    private EditText editEmail;
    private EditText editSenha;
    private Button buttonLogin;
    private Button buttonCadastro;

    private AppDatabase db;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editEmail = view.findViewById(R.id.editEmail);
        editSenha = view.findViewById(R.id.editSenha);
        buttonLogin = view.findViewById(R.id.buttonLogin);
        buttonCadastro = view.findViewById(R.id.buttonCadastro);

        db = AppDatabase.get(requireContext());

        // Botão de cadastro
        buttonCadastro.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CadastroActivity.class)));

        // Botão de login
        buttonLogin.setOnClickListener(v -> realizarLogin());

        // "Seguir sem login": sem estar logado, só a tela "Sobre o projeto" fica disponível
        view.findViewById(R.id.textSeguirSemLogin).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AboutProjectActivity.class)));
    }

    private void realizarLogin() {
        String email = editEmail.getText().toString().trim();
        String senhaDigitada = editSenha.getText().toString().trim();

        // Validação dos campos
        if (email.isEmpty() || senhaDigitada.isEmpty()) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Operação do banco em uma thread separada
        executor.execute(() -> {

            // Busca o usuário pelo e-mail
            User user = db.userDao().getUserByEmail(email);

            // Usuário não encontrado ou senha incorreta
            // BCrypt gera um hash diferente a cada vez, então a senha é conferida com verificarSenha
            if (user == null || !SecurityUtils.verificarSenha(senhaDigitada, user.password)) {
                mostrarToast("E-mail ou senha incorretos");
                return;
            }

            // ============================================================
            // LOGIN VALIDADO
            // ============================================================

            // Remove qualquer sessão anterior
            db.userDao().logoutTodos();

            // Define o usuário atual como logado e salva
            user.isLogged = true;
            db.userDao().atualizar(user);

            // Não precisa abrir outra tela: a MainActivity observa o usuário logado
            // e troca o Login pela tela Hoje automaticamente.
            mostrarToast("Login realizado com sucesso!");
        });
    }

    // Mostra um Toast na thread principal, sem quebrar se o fragment já foi fechado
    private void mostrarToast(String mensagem) {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() ->
                Toast.makeText(activity, mensagem, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        editEmail = null;
        editSenha = null;
        buttonLogin = null;
        buttonCadastro = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}