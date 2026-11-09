package com.example.habitscare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Patterns;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habitscare.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CadastroActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button buttonCamera;
    private Button buttonSalvar;

    private EditText editNome;
    private EditText editEmail;
    private EditText editSenha;
    private TextView textLabelSenha;
    private LinearLayout layoutCampoSenha;
    private TextView textTitulo;

    private AppDatabase db;

    private byte[] fotoEmBytes;
    private User usuarioEditando;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Câmera OU galeria (precisa ser criado como campo, antes de a tela iniciar)
    private final FotoPicker fotoPicker = new FotoPicker(this, bytes -> {
        fotoEmBytes = bytes;
        imageView.setPadding(0, 0, 0, 0);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        FotoUtils.carregar(imageView, bytes);
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        imageView = findViewById(R.id.imageView);
        buttonCamera = findViewById(R.id.buttonCamera);
        buttonSalvar = findViewById(R.id.buttonSalvar);
        editNome = findViewById(R.id.editNome);
        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);
        textLabelSenha = findViewById(R.id.textLabelSenha);
        layoutCampoSenha = findViewById(R.id.layoutCampoSenha);
        textTitulo = findViewById(R.id.textTitulo);

        db = AppDatabase.get(this);

        int usuarioId = getIntent().getIntExtra("USUARIO_ID", -1);

        if (usuarioId != -1) {
            if (textLabelSenha != null) textLabelSenha.setVisibility(View.GONE);
            if (layoutCampoSenha != null) layoutCampoSenha.setVisibility(View.GONE);
            if (textTitulo != null) textTitulo.setText(R.string.titulo_editar);
            buttonSalvar.setText(R.string.btn_salvar_alteracoes);

            carregarUsuario(usuarioId);
        }

        // Botão e a própria foto abrem a escolha entre câmera e galeria
        buttonCamera.setOnClickListener(v -> fotoPicker.mostrarOpcoes());
        imageView.setOnClickListener(v -> fotoPicker.mostrarOpcoes());
        buttonSalvar.setOnClickListener(v -> salvarCadastro());
    }

    private void carregarUsuario(int usuarioId) {
        executor.execute(() -> {
            User usuario = db.userDao().buscarPorId(usuarioId);

            runOnUiThread(() -> {
                if (usuario == null) {
                    Toast.makeText(this, "Usuário não encontrado", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                usuarioEditando = usuario;
                editNome.setText(usuario.getUser_name());
                editEmail.setText(usuario.getEmail());
                editSenha.setText("");

                if (usuario.getPhoto() != null && usuario.getPhoto().length > 0) {
                    fotoEmBytes = usuario.getPhoto();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(
                            usuario.getPhoto(), 0, usuario.getPhoto().length
                    );
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            });
        });
    }

    private void salvarCadastro() {
        String nome = editNome.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String senha = editSenha.getText().toString().trim();

        if (nome.isEmpty() || email.isEmpty() || (usuarioEditando == null && senha.isEmpty())) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== NOVO: validação do formato do e-mail =====
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Digite um e-mail válido");
            editEmail.requestFocus();
            return;
        }

        final byte[] fotoFinal = fotoEmBytes;
        if (fotoFinal == null || fotoFinal.length == 0) {
            Toast.makeText(this, "Escolha uma foto de perfil", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            // O nome pode se repetir; só o e-mail precisa ser único
            User usuarioComMesmoEmail = db.userDao().getUserByEmail(email);

            if (usuarioEditando == null) {
                if (usuarioComMesmoEmail != null) {
                    runOnUiThread(() -> Toast.makeText(CadastroActivity.this, "E-mail já cadastrado!", Toast.LENGTH_SHORT).show());
                    return;
                }

                User novoUsuario = new User();
                novoUsuario.setUser_name(nome);
                novoUsuario.setEmail(email);
                novoUsuario.setPassword(SecurityUtils.hashSenha(senha));
                novoUsuario.setPhoto(fotoFinal);
                novoUsuario.setLogged(false);

                db.userDao().insert(novoUsuario);

                runOnUiThread(() -> {
                    Toast.makeText(CadastroActivity.this, "Usuário cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                if (usuarioComMesmoEmail != null && usuarioComMesmoEmail.getId() != usuarioEditando.getId()) {
                    runOnUiThread(() -> Toast.makeText(CadastroActivity.this, "E-mail já cadastrado!", Toast.LENGTH_SHORT).show());
                    return;
                }

                usuarioEditando.setUser_name(nome);
                usuarioEditando.setEmail(email);
                if (!senha.isEmpty()) {
                    usuarioEditando.setPassword(SecurityUtils.hashSenha(senha));
                }
                usuarioEditando.setPhoto(fotoFinal);

                db.userDao().atualizar(usuarioEditando);

                runOnUiThread(() -> {
                    Toast.makeText(CadastroActivity.this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}