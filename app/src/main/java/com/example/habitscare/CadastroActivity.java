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

    /*
     * Foto armazenada em memória como byte[].
     */
    private byte[] fotoEmBytes;

    /*
     * Usuário que está sendo editado.
     *
     * null = novo cadastro
     * diferente de null = edição
     */
    private User usuarioEditando;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    /*
     * Solicita permissão para utilizar a câmera.
     */
    private final ActivityResultLauncher<String>
            requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {

                        if (granted) {

                            abrirCamera();

                        } else {

                            Toast.makeText(
                                    this,
                                    "Permissão da câmera negada",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    /*
     * Abre a câmera externa do sistema Android.
     */
    private final ActivityResultLauncher<Uri>
            takePictureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {

                        if (success) {

                            carregarImagem();

                        } else {

                            Toast.makeText(
                                    this,
                                    "Não foi possível tirar a foto",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cadastro);

        /*
         * Componentes da tela.
         */
        imageView = findViewById(R.id.imageView);

        buttonCamera = findViewById(
                R.id.buttonCamera
        );

        buttonSalvar = findViewById(
                R.id.buttonSalvar
        );

        editNome = findViewById(
                R.id.editNome
        );

        editEmail = findViewById(
                R.id.editEmail
        );

        editSenha = findViewById(
                R.id.editSenha
        );

        textLabelSenha = findViewById(
                R.id.textLabelSenha
        );

        layoutCampoSenha = findViewById(
                R.id.layoutCampoSenha
        );

        textTitulo = findViewById(
                R.id.textTitulo
        );

        /*
         * Banco de dados.
         */
        db = AppDatabase.get(this);

        /*
         * Verifica se a Activity foi aberta
         * para edição de um usuário.
         */
        int usuarioId = getIntent().getIntExtra(
                "USUARIO_ID",
                -1
        );

        if (usuarioId != -1) {
            if (textLabelSenha != null) textLabelSenha.setVisibility(View.GONE);
            if (layoutCampoSenha != null) layoutCampoSenha.setVisibility(View.GONE);
            if (textTitulo != null) textTitulo.setText(R.string.titulo_editar);
            buttonSalvar.setText(R.string.btn_salvar_alteracoes);

            carregarUsuario(usuarioId);
        }

        /*
         * Botão para tirar foto.
         */
        buttonCamera.setOnClickListener(
                v -> verificarPermissaoCamera()
        );

        /*
         * Botão salvar.
         */
        buttonSalvar.setOnClickListener(
                v -> salvarCadastro()
        );
    }

    /*
     * Verifica se o aplicativo possui
     * permissão para usar a câmera.
     */
    private void verificarPermissaoCamera() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            abrirCamera();

        } else {

            requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
            );
        }
    }

    /*
     * Abre a câmera externa do Android.
     */
    private void abrirCamera() {

        try {

            photoFile = criarArquivoImagem();

            photoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName()
                            + ".fileprovider",
                    photoFile
            );

            /*
             * Abre a câmera do sistema.
             */
            takePictureLauncher.launch(photoUri);

        } catch (IOException e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Erro ao criar arquivo da foto",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /*
     * Cria o arquivo temporário onde a câmera
     * irá salvar a fotografia.
     */
    private File criarArquivoImagem()
            throws IOException {

        String timeStamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                ).format(new Date());

        String nomeArquivo =
                "JPEG_" + timeStamp + "_";

        File pasta =
                getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES
                );

        return File.createTempFile(
                nomeArquivo,
                ".jpg",
                pasta
        );
    }

    /*
     * Mostra a foto tirada no ImageView.
     */
    private void carregarImagem() {

        if (photoFile != null
                && photoFile.exists()) {

            Bitmap bitmap =
                    BitmapFactory.decodeFile(
                            photoFile.getAbsolutePath()
                    );

            if (bitmap != null) {
                imageView.setPadding(0, 0, 0, 0);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageBitmap(bitmap);
            }

            /*
             * Converte a foto para byte[].
             */
            fotoEmBytes =
                    obterBytesDaFoto();
        }
    }

    /*
     * Converte a foto para byte[].
     *
     * A imagem é reduzida e comprimida antes
     * de ser armazenada no banco.
     */
    private byte[] obterBytesDaFoto() {

        /*
         * Se não existe uma nova foto,
         * mantém a foto que já estava salva.
         */
        if (photoFile == null
                || !photoFile.exists()) {

            return fotoEmBytes;
        }

        try {

            /*
             * Carrega a imagem original.
             */
            Bitmap bitmap =
                    BitmapFactory.decodeFile(
                            photoFile.getAbsolutePath()
                    );

            if (bitmap == null) {

                return null;
            }

            /*
             * Tamanho máximo da imagem.
             */
            int larguraMaxima = 800;
            int alturaMaxima = 800;

            /*
             * Calcula a escala necessária
             * para manter a proporção da imagem.
             */
            float escala = Math.min(
                    (float) larguraMaxima
                            / bitmap.getWidth(),

                    (float) alturaMaxima
                            / bitmap.getHeight()
            );

            /*
             * Só reduz a imagem se ela for
             * maior que 800x800.
             */
            if (escala < 1.0f) {

                int novaLargura =
                        Math.round(
                                bitmap.getWidth()
                                        * escala
                        );

                int novaAltura =
                        Math.round(
                                bitmap.getHeight()
                                        * escala
                        );

                Bitmap bitmapReduzido =
                        Bitmap.createScaledBitmap(
                                bitmap,
                                novaLargura,
                                novaAltura,
                                true
                        );

                /*
                 * Libera a imagem original.
                 */
                bitmap.recycle();

                bitmap = bitmapReduzido;
            }

            /*
             * Cria o array de bytes.
             */
            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            /*
             * Comprime a imagem como JPEG.
             *
             * Qualidade 60 reduz bastante o tamanho
             * do BLOB sem deixar a imagem ilegível.
             */
            bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    60,
                    outputStream
            );

            /*
             * Libera a memória do Bitmap.
             */
            bitmap.recycle();

            /*
             * Retorna a imagem como byte[].
             */
            return outputStream.toByteArray();

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    /*
     * Carrega os dados do usuário para edição.
     */
    private void carregarUsuario(int usuarioId) {

        executor.execute(() -> {

            User usuario =
                    db.userDao().buscarPorId(
                            usuarioId
                    );

            runOnUiThread(() -> {

                if (usuario == null) {

                    Toast.makeText(
                            this,
                            "Usuário não encontrado",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();

                    return;
                }

                usuarioEditando = usuario;

                /*
                 * Preenche o nome.
                 */
                editNome.setText(
                        usuario.user_name
                );

                /*
                 * Preenche o e-mail.
                 */
                editEmail.setText(
                        usuario.email
                );

                /*
                 * Não colocamos a senha antiga
                 * no campo.
                 */
                editSenha.setText("");

                /*
                 * Carrega a foto armazenada no banco.
                 */
                if (usuario.photo != null
                        && usuario.photo.length > 0) {

                    fotoEmBytes =
                            usuario.photo;

                    Bitmap bitmap =
                            BitmapFactory.decodeByteArray(
                                    usuario.photo,
                                    0,
                                    usuario.photo.length
                            );

                    if (bitmap != null) {

                        imageView.setImageBitmap(bitmap
                        );
                    }
                }
            });
        });
    }

    /*
     * Salva um novo usuário ou atualiza
     * um usuário existente.
     */
    private void salvarCadastro() {

        String nome =
                editNome.getText()
                        .toString()
                        .trim();

        String email =
                editEmail.getText()
                        .toString()
                        .trim();

        String senha =
                editSenha.getText()
                        .toString()
                        .trim();

        /*
         * No cadastro a senha é obrigatória.
         *
         * Na edição a senha pode ficar vazia,
         * mantendo a senha anterior.
         */
        if (nome.isEmpty()
                || email.isEmpty()
                || (usuarioEditando == null
                && senha.isEmpty())) {

            Toast.makeText(
                    this,
                    "Preencha todos os campos obrigatórios",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * Obtém a foto em byte[].
         */
        byte[] fotoFinal =
                obterBytesDaFoto();

        /*
         * Foto obrigatória.
         */
        if (fotoFinal == null
                || fotoFinal.length == 0) {

            Toast.makeText(
                    this,
                    "Tire uma foto de perfil",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        executor.execute(() -> {

            /*
             * Verifica se já existe um usuário
             * com esse nome.
             */
            User usuarioComMesmoNome =
                    db.userDao()
                            .getUserByUsername(nome);

            /*
             * =================================
             * NOVO CADASTRO
             * =================================
             */
            if (usuarioEditando == null) {

                if (usuarioComMesmoNome != null) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    CadastroActivity.this,
                                    "Nome de usuário já cadastrado!",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                    return;
                }

                User novoUsuario =
                        new User();

                novoUsuario.user_name =
                        nome;

                novoUsuario.email =
                        email;

                /*
                 * Criptografa a senha.
                 */
                novoUsuario.password =
                        SecurityUtils.hashSenha(
                                senha
                        );

                /*
                 * Salva a foto como byte[].
                 */
                novoUsuario.photo =
                        fotoFinal;

                /*
                 * Novo usuário começa
                 * deslogado.
                 */
                novoUsuario.isLogged =
                        false;

                /*
                 * Insere no banco.
                 */
                db.userDao().insert(
                        novoUsuario
                );

                runOnUiThread(() -> {

                    Toast.makeText(
                            CadastroActivity.this,
                            "Usuário cadastrado com sucesso!",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                });

            }

            /*
             * =================================
             * EDIÇÃO
             * =================================
             */
            else {

                /*
                 * Verifica se o nome já pertence
                 * a outro usuário.
                 */
                if (usuarioComMesmoNome != null
                        && usuarioComMesmoNome.id
                        != usuarioEditando.id) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    CadastroActivity.this,
                                    "Nome de usuário já cadastrado!",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                    return;
                }

                /*
                 * Atualiza nome.
                 */
                usuarioEditando.user_name =
                        nome;

                /*
                 * Atualiza e-mail.
                 */
                usuarioEditando.email =
                        email;

                /*
                 * Se uma nova senha foi digitada,
                 * criptografa e salva.
                 */
                if (!senha.isEmpty()) {

                    usuarioEditando.password =
                            SecurityUtils.hashSenha(
                                    senha
                            );
                }

                /*
                 * Atualiza a foto.
                 */
                usuarioEditando.photo =
                        fotoFinal;

                /*
                 * Atualiza no banco.
                 */
                db.userDao().atualizar(
                        usuarioEditando
                );

                runOnUiThread(() -> {

                    Toast.makeText(
                            CadastroActivity.this,
                            "Perfil atualizado com sucesso!",
                            Toast.LENGTH_SHORT
                    ).show();

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