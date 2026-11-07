package com.example.habitscare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class SplashActivity extends AppCompatActivity {

    private boolean isDataReady = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Instala a Splash Screen oficial
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // 2. Mantém a Splash visível até a flag ser verdadeira
        splashScreen.setKeepOnScreenCondition(() -> !isDataReady);

        // 3. Sempre segue para a MainActivity: ela decide se mostra o Login ou a tela Hoje
        runnable = () -> {
            isDataReady = true; // Define como true PRIMEIRO

            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        };

        // 4. Executa após o tempo especificado
        handler.postDelayed(runnable, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove callbacks pendentes se a Activity for destruída antes do tempo
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}