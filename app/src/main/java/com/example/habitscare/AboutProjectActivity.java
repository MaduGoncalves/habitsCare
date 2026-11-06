package com.example.habitscare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class AboutProjectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_project);

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        MaterialButton btnCriarConta = findViewById(R.id.btnCriarConta);

        btnVoltar.setOnClickListener(v -> finish());

        btnCriarConta.setOnClickListener(v -> {
            Intent intent = new Intent(AboutProjectActivity.this, CadastroActivity  .class);
            startActivity(intent);
            finish();
        });
    }
}