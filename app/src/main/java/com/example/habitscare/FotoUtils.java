package com.example.habitscare;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Utilitários da foto de perfil.
 * A foto é reduzida (máx. 512 px) e comprimida em JPEG antes de ir para o banco (campo BLOB),
 * para a linha do usuário continuar pequena.
 */
public final class FotoUtils {

    private static final int MAX_LADO = 512;
    private static final int QUALIDADE_JPEG = 85;

    private FotoUtils() { }

    /** Lê a imagem escolhida na galeria e devolve bytes JPEG reduzidos. Chame fora da thread principal. */
    public static byte[] uriParaBytes(Context context, Uri uri) {
        try {
            ContentResolver cr = context.getContentResolver();

            // 1) Descobre o tamanho original sem carregar a imagem
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = cr.openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            // 2) Carrega já reduzida (potência de 2) para economizar memória
            int maior = Math.max(bounds.outWidth, bounds.outHeight);
            int sample = 1;
            while (maior / (sample * 2) >= MAX_LADO) sample *= 2;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bmp;
            try (InputStream in = cr.openInputStream(uri)) {
                bmp = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bmp == null) return null;

            // 3) Ajusta a rotação (fotos de celular costumam vir "deitadas")
            int graus = lerRotacao(cr, uri);
            if (graus != 0) {
                Matrix m = new Matrix();
                m.postRotate(graus);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            }

            // 4) Ajuste fino do tamanho para no máximo 512 px
            int lado = Math.max(bmp.getWidth(), bmp.getHeight());
            if (lado > MAX_LADO) {
                float escala = (float) MAX_LADO / lado;
                bmp = Bitmap.createScaledBitmap(bmp,
                        Math.round(bmp.getWidth() * escala),
                        Math.round(bmp.getHeight() * escala), true);
            }

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida);
            return saida.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /** Converte um Bitmap (ex.: foto tirada na câmera) em bytes JPEG reduzidos. */
    public static byte[] bitmapParaBytes(Bitmap bmp) {
        if (bmp == null) return null;
        int lado = Math.max(bmp.getWidth(), bmp.getHeight());
        if (lado > MAX_LADO) {
            float escala = (float) MAX_LADO / lado;
            bmp = Bitmap.createScaledBitmap(bmp,
                    Math.round(bmp.getWidth() * escala),
                    Math.round(bmp.getHeight() * escala), true);
        }
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida);
        return saida.toByteArray();
    }

    private static int lerRotacao(ContentResolver cr, Uri uri) {
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) return 0;
            int o = new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (o) {
                case ExifInterface.ORIENTATION_ROTATE_90:  return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /** Mostra a foto no ImageView; se não houver foto, mostra o ícone padrão de pessoa. */
    public static void carregar(ImageView imageView, byte[] foto) {
        if (foto != null && foto.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(foto, 0, foto.length);
            if (bmp != null) {
                imageView.setImageBitmap(bmp);
                return;
            }
        }
        imageView.setImageResource(R.drawable.ic_pessoa);
    }
}