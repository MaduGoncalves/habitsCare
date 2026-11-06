package com.example.habitscare;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.habitscare.model.User;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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

    private File photoFile;
    private Uri photoUri;

    private byte[] fotoEmBytes;
    private User usuarioEditando;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            abrirCamera();
                        } else {
                            Toast.makeText(this, "Permissão da câmera negada", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (success) {
                            carregarImagem();
                        } else {
                            Toast.makeText(this, "Não foi possível tirar a foto", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

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

        buttonCamera.setOnClickListener(v -> verificarPermissaoCamera());
        buttonSalvar.setOnClickListener(v -> salvarCadastro());
    }

    private void verificarPermissaoCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamera() {
        try {
            photoFile = criarArquivoImagem();
            photoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureLauncher.launch(photoUri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao criar arquivo da foto", Toast.LENGTH_SHORT).show();
        }
    }

    private File criarArquivoImagem() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nomeArquivo = "JPEG_" + timeStamp + "_";
        File pasta = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(nomeArquivo, ".jpg", pasta);
    }

    private void carregarImagem() {
        if (photoFile != null && photoFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            if (bitmap != null) {
                imageView.setPadding(0, 0, 0, 0);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageBitmap(bitmap);
            }
            fotoEmBytes = obterBytesDaFoto();
        }
    }

    private byte[] obterBytesDaFoto() {
        if (photoFile == null || !photoFile.exists()) {
            return fotoEmBytes;
        }

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            if (bitmap == null) return null;

            int larguraMaxima = 800;
            int alturaMaxima = 800;

            float escala = Math.min(
                    (float) larguraMaxima / bitmap.getWidth(),
                    (float) alturaMaxima / bitmap.getHeight()
            );

            if (escala < 1.0f) {
                int novaLargura = Math.round(bitmap.getWidth() * escala);
                int novaAltura = Math.round(bitmap.getHeight() * escala);

                Bitmap bitmapReduzido = Bitmap.createScaledBitmap(bitmap, novaLargura, novaAltura, true);
                bitmap.recycle();
                bitmap = bitmapReduzido;
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
            bitmap.recycle();

            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

        byte[] fotoFinal = obterBytesDaFoto();
        if (fotoFinal == null || fotoFinal.length == 0) {
            Toast.makeText(this, "Tire uma foto de perfil", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            User usuarioComMesmoNome = db.userDao().getUserByUsername(nome);

            if (usuarioEditando == null) {
                if (usuarioComMesmoNome != null) {
                    runOnUiThread(() -> Toast.makeText(CadastroActivity.this, "Nome de usuário já cadastrado!", Toast.LENGTH_SHORT).show());
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
                if (usuarioComMesmoNome != null && usuarioComMesmoNome.getId() != usuarioEditando.getId()) {
                    runOnUiThread(() -> Toast.makeText(CadastroActivity.this, "Nome de usuário já cadastrado!", Toast.LENGTH_SHORT).show());
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