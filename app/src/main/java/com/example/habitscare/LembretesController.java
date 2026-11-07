package com.example.habitscare;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.habitscare.databinding.FragmentPerfilBinding;

import java.util.Locale;

/**
 * Cuida do cartão "Lembretes" da tela de Perfil: interruptores, escolha de horário/intervalo
 * e todo o tratamento de permissões (POST_NOTIFICATIONS e alarme exato).
 *
 * Regras de permissão:
 * - Ao LIGAR um lembrete, pede a permissão de notificações (Android 13+) se ainda não tiver.
 *   Se negar: o interruptor volta para desligado, o app avisa de forma amigável e nada quebra.
 *   Se negou "para sempre": oferece abrir as configurações do app.
 * - Hora de dormir e Resumo usam alarme exato (Android 12+). Sem a permissão, o app explica
 *   e oferece abrir a tela do sistema; se o usuário recusar, os lembretes continuam
 *   funcionando de forma aproximada (podem atrasar alguns minutos).
 */
public class LembretesController {

    private final Fragment fragment;
    private FragmentPerfilBinding b;
    private boolean atualizando = false;   // evita disparar os listeners quando a tela só se atualiza

    // O que fazer depois que o usuário responder ao pedido de permissão
    private Runnable aoConceder;
    private Runnable aoNegar;

    private final ActivityResultLauncher<String> pedirPermissaoNotificacoes;

    /** Crie como CAMPO do Fragment (antes de a tela iniciar). */
    public LembretesController(Fragment fragment) {
        this.fragment = fragment;

        pedirPermissaoNotificacoes = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), concedida -> {
                    Runnable ok = aoConceder;
                    Runnable nao = aoNegar;
                    aoConceder = null;
                    aoNegar = null;
                    if (b == null) return;   // a tela foi fechada enquanto o diálogo estava aberto

                    if (concedida) {
                        if (ok != null) ok.run();
                    } else {
                        if (nao != null) nao.run();
                        // Depois de uma negação, se o sistema não deixa mais perguntar,
                        // o único caminho é as configurações do app
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                && !fragment.shouldShowRequestPermissionRationale(
                                Manifest.permission.POST_NOTIFICATIONS)) {
                            oferecerAbrirConfiguracoes();
                        }
                    }
                });
    }

    // ---------- Ciclo de vida (chamado pelo PerfilFragment) ----------

    public void vincular(FragmentPerfilBinding binding) {
        this.b = binding;

        b.swSono.setOnCheckedChangeListener((v, ligado) -> aoAlternar(AlarmScheduler.TIPO_SONO, ligado));
        b.swAgua.setOnCheckedChangeListener((v, ligado) -> aoAlternar(AlarmScheduler.TIPO_AGUA, ligado));
        b.swResumo.setOnCheckedChangeListener((v, ligado) -> aoAlternar(AlarmScheduler.TIPO_RESUMO, ligado));

        b.btnHoraSono.setOnClickListener(v -> escolherHorarioSono());
        b.btnIntervaloAgua.setOnClickListener(v -> escolherIntervaloAgua());
        b.tvAvisoLembretes.setOnClickListener(v -> abrirPermissaoAlarmeExato());

        atualizarUi();
    }

    /** Ao voltar para a tela (ex.: depois das configurações do sistema): reagenda e atualiza. */
    public void aoRetomar() {
        if (b == null) return;
        AlarmScheduler.reagendarTudo(ctx());
        atualizarUi();
    }

    public void desvincular() {
        b = null;
    }

    // ---------- Interruptores ----------

    private void aoAlternar(String tipo, boolean ligado) {
        if (atualizando || b == null) return;
        final Context ctx = ctx();

        if (!ligado) {
            definirAtivo(ctx, tipo, false);
            AlarmScheduler.reagendarTudo(ctx);
            atualizarUi();
            return;
        }

        garantirPermissaoNotificacoes(
                () -> {   // permissão OK
                    definirAtivo(ctx, tipo, true);
                    AlarmScheduler.reagendarTudo(ctx);
                    atualizarUi();
                    boolean usaExato = !AlarmScheduler.TIPO_AGUA.equals(tipo);
                    if (usaExato && !AlarmScheduler.podeAgendarExato(ctx)) {
                        explicarAlarmeExato();
                    }
                },
                () -> {   // permissão NEGADA: desliga esse lembrete, sem erro
                    definirAtivo(ctx, tipo, false);
                    AlarmScheduler.reagendarTudo(ctx);
                    atualizarUi();
                    aviso("Sem permissão de notificações, este lembrete ficou desativado. "
                            + "Você pode ligar de novo quando quiser.");
                });
    }

    private void definirAtivo(Context ctx, String tipo, boolean valor) {
        switch (tipo) {
            case AlarmScheduler.TIPO_SONO:   LembretesPrefs.setSonoAtivo(ctx, valor); break;
            case AlarmScheduler.TIPO_AGUA:   LembretesPrefs.setAguaAtiva(ctx, valor); break;
            case AlarmScheduler.TIPO_RESUMO: LembretesPrefs.setResumoAtivo(ctx, valor); break;
            default: break;
        }
    }

    // ---------- Permissão de notificações (Android 13+) ----------

    private void garantirPermissaoNotificacoes(Runnable ok, Runnable negado) {
        if (NotificationHelper.notificacoesPermitidas(ctx())) {
            ok.run();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            aoConceder = ok;
            aoNegar = negado;
            pedirPermissaoNotificacoes.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            // Antes do Android 13 não há pedido em tempo de execução:
            // as notificações só estão desligadas se o usuário bloqueou o app nas configurações
            negado.run();
            oferecerAbrirConfiguracoes();
        }
    }

    private void oferecerAbrirConfiguracoes() {
        if (b == null) return;
        new AlertDialog.Builder(ctx())
                .setTitle("Notificações desativadas")
                .setMessage("Para receber os lembretes, ative as notificações do HabitsCare nas configurações do aparelho.")
                .setNegativeButton("Agora não", null)
                .setPositiveButton("Abrir configurações", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, ctx().getPackageName());
                    abrirTela(intent);
                })
                .show();
    }

    // ---------- Permissão de alarme exato (Android 12+) ----------

    private void explicarAlarmeExato() {
        if (b == null) return;
        new AlertDialog.Builder(ctx())
                .setTitle("Alarmes no horário exato")
                .setMessage("Para avisar exatamente na hora marcada, permita \"Alarmes e lembretes\" para o HabitsCare. "
                        + "Sem isso os lembretes continuam funcionando, mas podem atrasar alguns minutos.")
                .setNegativeButton("Agora não", null)
                .setPositiveButton("Permitir", (d, w) -> abrirPermissaoAlarmeExato())
                .show();
    }

    private void abrirPermissaoAlarmeExato() {
        abrirTela(AlarmScheduler.intentPermissaoAlarmeExato(ctx()));
    }

    private void abrirTela(Intent intent) {
        try {
            fragment.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Alguns aparelhos não têm essa tela: abre os detalhes gerais do app
            try {
                fragment.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + ctx().getPackageName())));
            } catch (ActivityNotFoundException ignorada) {
                aviso("Não foi possível abrir as configurações.");
            }
        }
    }

    // ---------- Horário de dormir e intervalo da água ----------

    private void escolherHorarioSono() {
        final Context ctx = ctx();
        new TimePickerDialog(ctx, (view, hora, minuto) -> {
            LembretesPrefs.setSonoHorario(ctx, hora, minuto);
            if (LembretesPrefs.isSonoAtivo(ctx)) AlarmScheduler.agendarSono(ctx);
            atualizarUi();
        }, LembretesPrefs.getSonoHora(ctx), LembretesPrefs.getSonoMinuto(ctx), true).show();
    }

    private void escolherIntervaloAgua() {
        final Context ctx = ctx();
        final String[] opcoes = {"A cada 1 hora", "A cada 2 horas", "A cada 3 horas"};
        int selecionado = Math.max(0, Math.min(2, LembretesPrefs.getAguaIntervaloH(ctx) - 1));

        new AlertDialog.Builder(ctx)
                .setTitle("Lembrar de beber água")
                .setSingleChoiceItems(opcoes, selecionado, (dialog, which) -> {
                    LembretesPrefs.setAguaIntervaloH(ctx, which + 1);
                    if (LembretesPrefs.isAguaAtiva(ctx)) AlarmScheduler.agendarAgua(ctx);
                    atualizarUi();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---------- Tela ----------

    private void atualizarUi() {
        if (b == null) return;
        Context ctx = ctx();

        atualizando = true;

        boolean sono = LembretesPrefs.isSonoAtivo(ctx);
        boolean agua = LembretesPrefs.isAguaAtiva(ctx);
        boolean resumo = LembretesPrefs.isResumoAtivo(ctx);

        b.swSono.setChecked(sono);
        b.swAgua.setChecked(agua);
        b.swResumo.setChecked(resumo);

        b.btnHoraSono.setText(String.format(Locale.getDefault(), "Horário: %02d:%02d",
                LembretesPrefs.getSonoHora(ctx), LembretesPrefs.getSonoMinuto(ctx)));

        int horas = LembretesPrefs.getAguaIntervaloH(ctx);
        b.btnIntervaloAgua.setText("A cada " + horas + (horas == 1 ? " hora" : " horas")
                + " · das 8h às 22h");

        boolean faltaAlarmeExato = (sono || resumo) && !AlarmScheduler.podeAgendarExato(ctx);
        b.tvAvisoLembretes.setVisibility(faltaAlarmeExato ? View.VISIBLE : View.GONE);

        atualizando = false;
    }

    // ---------- Utilidades ----------

    private Context ctx() {
        return fragment.requireContext();
    }

    private void aviso(String mensagem) {
        if (fragment.getContext() != null) {
            Toast.makeText(fragment.getContext(), mensagem, Toast.LENGTH_LONG).show();
        }
    }
}