package com.example.habitscare;

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

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_login,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        editEmail = view.findViewById(R.id.editEmail);
        editSenha = view.findViewById(R.id.editSenha);
        buttonLogin = view.findViewById(R.id.buttonLogin);
        buttonCadastro = view.findViewById(R.id.buttonCadastro);

        db = AppDatabase.get(requireContext());

        // Botão de cadastro
        buttonCadastro.setOnClickListener(v -> {

            Intent intent = new Intent(
                    requireContext(),
                    CadastroActivity.class
            );

            startActivity(intent);
        });

        // Botão de login
        buttonLogin.setOnClickListener(v -> realizarLogin());
    }

    private void realizarLogin() {

        String email = editEmail.getText()
                .toString()
                .trim();

        String senhaDigitada = editSenha.getText()
                .toString()
                .trim();

        // Validação dos campos
        if (email.isEmpty() || senhaDigitada.isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "Preencha todos os campos",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Criptografa a senha digitada para comparar
        // com a senha armazenada no banco
        String senhaCriptografada =
                SecurityUtils.hashSenha(senhaDigitada);

        // Operação do banco em uma thread separada
        executor.execute(() -> {

            // Busca o usuário pelo e-mail
            User user = db.userDao()
                    .getUserByEmail(email);

            // Usuário não encontrado
            if (user == null) {

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(
                                requireContext(),
                                "E-mail ou senha incorretos",
                                Toast.LENGTH_SHORT
                        ).show()
                );

                return;
            }

            // Senha incorreta
            if (user.password == null ||
                    !user.password.equals(senhaCriptografada)) {

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(
                                requireContext(),
                                "E-mail ou senha incorretos",
                                Toast.LENGTH_SHORT
                        ).show()
                );

                return;
            }

            // ============================================================
            // LOGIN VALIDADO
            // ============================================================

            // Remove qualquer sessão anterior
            db.userDao().logoutTodos();

            // Define o usuário atual como logado
            user.isLogged = true;

            // Atualiza o usuário no banco
            db.userDao().atualizar(user);

            // Informa o resultado na interface
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(
                            requireContext(),
                            "Login realizado com sucesso!",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        });
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
