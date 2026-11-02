package com.example.habitscare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etSenha;
    private Button btnLogin, btnContinueWithoutLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etSenha = findViewById(R.id.etSenha);
        btnLogin = findViewById(R.id.btnLogin);
        btnContinueWithoutLogin = findViewById(R.id.btnContinueWithoutLogin);

        // Acesso restrito via Login
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String senha = etSenha.getText().toString().trim();

            if (!email.isEmpty() && !senha.isEmpty()) {
                // Salva a sessão ativa
                SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);
                preferences.edit().putBoolean("is_logged_in", true).apply();

                // Redireciona para a área restrita
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Preencha e-mail e senha para entrar", Toast.LENGTH_SHORT).show();
            }
        });

        // Acesso público: explicação da aplicação
        btnContinueWithoutLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, AboutActivity.class);
            startActivity(intent);
        });
    }
}