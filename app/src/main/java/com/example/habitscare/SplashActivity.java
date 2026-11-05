package com.example.habitscare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class SplashActivity extends AppCompatActivity {

    private boolean isDataReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Instala a Splash Screen oficial
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // 2. Mantém a Splash Screen visível na tela até que 'isDataReady' seja true
        splashScreen.setKeepOnScreenCondition(() -> !isDataReady);

        // 3. Simula a verificação ou carregamento de dados (ex: 2 segundos)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Verifica a sessão do utilizador
            SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);
            boolean isLoggedIn = preferences.getBoolean("is_logged_in", false);

            Intent intent;
            if (isLoggedIn) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish();

            // Liberta a Splash Screen para concluir a transição
            isDataReady = true;
        }, 500); // 2000 milissegundos = 2 segundos
    }
}