package com.example.habitscare;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Instala a Splash Screen oficial
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Verifica se o utilizador já está autenticado
        SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("is_logged_in", false);

        Intent intent;
        if (isLoggedIn) {
            // Se já fez login, vai direto para a área restrita
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // Se não fez login, vai para a tela de autenticação
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish(); // Remove a SplashActivity da pilha de navegação
    }
}