package com.example.habitscare;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Seletor de foto reutilizável: o usuário escolhe entre TIRAR FOTO (câmera) ou ESCOLHER DA GALERIA.
 * Funciona em Activity (Cadastro) e em Fragment (Perfil).
 *
 * IMPORTANTE: crie o objeto como CAMPO da classe, nunca dentro de um clique:
 *
 *     private final FotoPicker fotoPicker = new FotoPicker(this, bytes -> {
 *         // já está na thread principal; 'bytes' é um JPEG reduzido, pronto para user.photo
 *     });
 *
 * E no clique da foto:  fotoPicker.mostrarOpcoes();
 *
 * A câmera usa o mesmo FileProvider do cadastro: authority = "<pacote>.fileprovider".
 */
public class FotoPicker {

    public interface Callback {
        /** Chamado na thread principal com a foto em JPEG (pronta para gravar no campo photo). */
        void aoEscolher(byte[] foto);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    private final Fragment fragment;        // preenchido quando usado em Fragment
    private final Activity activity;        // preenchido quando usado em Activity
    private final Callback callback;

    private final ActivityResultLauncher<PickVisualMediaRequest> galeria;
    private final ActivityResultLauncher<Uri> camera;
    private final ActivityResultLauncher<String> permissaoCamera;

    // Arquivo temporário da foto que está sendo tirada
    private File arquivoCamera;
    private Uri uriCamera;

    /** Uso em Fragment (tela de Perfil). */
    public FotoPicker(Fragment fragment, Callback callback) {
        this(fragment, fragment, null, callback);
    }

    /** Uso em Activity (tela de Cadastro). */
    public FotoPicker(ComponentActivity activity, Callback callback) {
        this(activity, null, activity, callback);
    }

    private FotoPicker(ActivityResultCaller caller, Fragment fragment, Activity activity, Callback callback) {
        this.fragment = fragment;
        this.activity = activity;
        this.callback = callback;

        // Galeria (seletor do sistema, não precisa de permissão)
        galeria = caller.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) processar(uri, null);
                });

        // Câmera (foto em tamanho real, gravada num arquivo temporário via FileProvider)
        camera = caller.registerForActivityResult(
                new ActivityResultContracts.TakePicture(), sucesso -> {
                    if (sucesso && uriCamera != null) {
                        processar(uriCamera, arquivoCamera);
                    } else {
                        if (arquivoCamera != null) arquivoCamera.delete();
                        aviso("Não foi possível tirar a foto");
                    }
                });

        permissaoCamera = caller.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), concedida -> {
                    if (concedida) lancarCamera();
                    else aviso("Permissão da câmera negada");
                });
    }

    /** Mostra o diálogo com as duas opções. */
    public void mostrarOpcoes() {
        new AlertDialog.Builder(contexto())
                .setTitle("Foto de perfil")
                .setItems(new String[]{"Tirar foto", "Escolher da galeria"}, (d, which) -> {
                    if (which == 0) abrirCamera();
                    else abrirGaleria();
                })
                .show();
    }

    private void abrirGaleria() {
        galeria.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void abrirCamera() {
        if (ContextCompat.checkSelfPermission(contexto(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            lancarCamera();
        } else {
            permissaoCamera.launch(Manifest.permission.CAMERA);
        }
    }

    private void lancarCamera() {
        Context ctx = contexto();
        try {
            String data = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File pasta = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            arquivoCamera = File.createTempFile("JPEG_" + data + "_", ".jpg", pasta);
            uriCamera = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", arquivoCamera);
            camera.launch(uriCamera);
        } catch (IOException e) {
            aviso("Erro ao criar arquivo da foto");
        } catch (ActivityNotFoundException e) {
            aviso("Nenhum aplicativo de câmera encontrado");
        }
    }

    // ---------- Processamento (fora da thread principal) ----------

    private void processar(Uri uri, File arquivoTemporario) {
        final Context app = contexto().getApplicationContext();
        EXECUTOR.execute(() -> {
            byte[] bytes = FotoUtils.uriParaBytes(app, uri);
            if (arquivoTemporario != null) arquivoTemporario.delete();   // não deixa lixo no aparelho
            principal.post(() -> {
                if (bytes == null) aviso("Não foi possível carregar a imagem");
                else callback.aoEscolher(bytes);
            });
        });
    }

    private Context contexto() {
        return fragment != null ? fragment.requireContext() : activity;
    }

    private void aviso(String msg) {
        Context ctx = (fragment != null) ? fragment.getContext() : activity;
        if (ctx != null) Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }
}