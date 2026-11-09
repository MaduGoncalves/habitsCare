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
 * Controla o cartão "Lembretes" da tela de Perfil.
 *
 * Responsabilidades:
 * - Controlar os interruptores dos lembretes;
 * - Permitir a escolha do horário de dormir;
 * - Permitir a escolha do intervalo de água;
 * - Permitir a escolha do horário de caminhada;
 * - Solicitar permissão de notificações;
 * - Solicitar permissão para alarmes exatos.
 */
public class LembretesController {

    private final Fragment fragment;
    private FragmentPerfilBinding b;

    // Evita disparar os listeners durante a atualização da tela.
    private boolean atualizando = false;

    // Ações executadas após a resposta da permissão.
    private Runnable aoConceder;
    private Runnable aoNegar;

    private final ActivityResultLauncher<String> pedirPermissaoNotificacoes;

    /**
     * Construtor da classe.
     *
     * Deve ser chamado como campo do Fragment,
     * antes de iniciar o ciclo de vida da tela.
     */
    public LembretesController(Fragment fragment) {

        this.fragment = fragment;

        pedirPermissaoNotificacoes = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                concedida -> {

                    Runnable ok = aoConceder;
                    Runnable nao = aoNegar;

                    aoConceder = null;
                    aoNegar = null;

                    // Verifica se a tela ainda está disponível.
                    if (b == null) return;

                    if (concedida) {

                        if (ok != null) {
                            ok.run();
                        }

                    } else {

                        if (nao != null) {
                            nao.run();
                        }

                        // Verifica se o sistema não permite
                        // solicitar novamente a permissão.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                && !fragment.shouldShowRequestPermissionRationale(
                                Manifest.permission.POST_NOTIFICATIONS)) {

                            oferecerAbrirConfiguracoes();
                        }
                    }
                }
        );
    }

    // =============================================
    // CICLO DE VIDA
    // =============================================

    /**
     * Vincula os componentes da tela aos listeners.
     */
    public void vincular(FragmentPerfilBinding binding) {

        this.b = binding;

        // Interruptor do lembrete de sono.
        b.swSono.setOnCheckedChangeListener(
                (v, ligado) -> aoAlternar(
                        AlarmScheduler.TIPO_SONO,
                        ligado
                )
        );

        // Interruptor do lembrete de água.
        b.swAgua.setOnCheckedChangeListener(
                (v, ligado) -> aoAlternar(
                        AlarmScheduler.TIPO_AGUA,
                        ligado
                )
        );

        // Interruptor do lembrete de caminhada.
        b.swCaminhada.setOnCheckedChangeListener(
                (v, ligado) -> aoAlternar(
                        AlarmScheduler.TIPO_CAMINHADA,
                        ligado
                )
        );

        // Botão para escolher o horário de dormir.
        b.btnHoraSono.setOnClickListener(
                v -> escolherHorarioSono()
        );

        // Botão para escolher o intervalo de água.
        b.btnIntervaloAgua.setOnClickListener(
                v -> escolherIntervaloAgua()
        );

        // Botão para escolher o horário da caminhada.
        b.btnHoraCaminhada.setOnClickListener(
                v -> escolherHorarioCaminhada()
        );

        // Texto para abrir as configurações de alarme exato.
        b.tvAvisoLembretes.setOnClickListener(
                v -> abrirPermissaoAlarmeExato()
        );

        atualizarUi();
    }

    /**
     * Chamado quando o usuário retorna à tela.
     *
     * Atualiza os alarmes e os componentes da interface.
     */
    public void aoRetomar() {

        if (b == null) return;

        AlarmScheduler.reagendarTudo(ctx());

        atualizarUi();
    }

    /**
     * Desvincula a tela quando o Fragment é destruído.
     */
    public void desvincular() {
        b = null;
    }

    // =============================================
    // CONTROLE DOS INTERRUPTORES
    // =============================================

    /**
     * Executado quando o usuário liga ou desliga
     * um dos lembretes.
     */
    private void aoAlternar(String tipo, boolean ligado) {

        if (atualizando || b == null) return;

        final Context ctx = ctx();

        // Caso o usuário desligue o lembrete.
        if (!ligado) {

            definirAtivo(ctx, tipo, false);

            AlarmScheduler.reagendarTudo(ctx);

            atualizarUi();

            return;
        }

        // Caso o usuário ligue o lembrete,
        // primeiro verifica a permissão de notificações.
        garantirPermissaoNotificacoes(

                // Permissão concedida.
                () -> {

                    definirAtivo(ctx, tipo, true);

                    AlarmScheduler.reagendarTudo(ctx);

                    atualizarUi();

                    // Água utiliza alarme não exato.
                    // Sono e caminhada utilizam alarme exato.
                    boolean usaExato =
                            !AlarmScheduler.TIPO_AGUA.equals(tipo);

                    if (usaExato
                            && !AlarmScheduler.podeAgendarExato(ctx)) {

                        explicarAlarmeExato();
                    }
                },

                // Permissão negada.
                () -> {

                    definirAtivo(ctx, tipo, false);

                    AlarmScheduler.reagendarTudo(ctx);

                    atualizarUi();

                    aviso(
                            "Sem permissão de notificações, este lembrete ficou desativado. "
                                    + "Você pode ligar de novo quando quiser."
                    );
                }
        );
    }

    /**
     * Define se determinado lembrete está ativado.
     */
    private void definirAtivo(
            Context ctx,
            String tipo,
            boolean valor
    ) {

        switch (tipo) {

            case AlarmScheduler.TIPO_SONO:

                LembretesPrefs.setSonoAtivo(ctx, valor);
                break;

            case AlarmScheduler.TIPO_AGUA:

                LembretesPrefs.setAguaAtiva(ctx, valor);
                break;

            case AlarmScheduler.TIPO_CAMINHADA:

                LembretesPrefs.setCaminhadaAtiva(ctx, valor);
                break;

            default:
                break;
        }
    }

    // =============================================
    // PERMISSÃO DE NOTIFICAÇÕES
    // =============================================

    /**
     * Verifica se o aplicativo pode enviar notificações.
     *
     * No Android 13 ou superior, solicita a permissão
     * POST_NOTIFICATIONS quando necessário.
     */
    private void garantirPermissaoNotificacoes(
            Runnable ok,
            Runnable negado
    ) {

        if (NotificationHelper.notificacoesPermitidas(ctx())) {

            ok.run();

            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            aoConceder = ok;
            aoNegar = negado;

            pedirPermissaoNotificacoes.launch(
                    Manifest.permission.POST_NOTIFICATIONS
            );

        } else {

            // Antes do Android 13, não existe solicitação
            // de permissão de notificações em tempo de execução.
            negado.run();

            oferecerAbrirConfiguracoes();
        }
    }

    /**
     * Oferece ao usuário a possibilidade de abrir
     * as configurações de notificações do aplicativo.
     */
    private void oferecerAbrirConfiguracoes() {

        if (b == null) return;

        new AlertDialog.Builder(ctx())
                .setTitle("Notificações desativadas")
                .setMessage(
                        "Para receber os lembretes, ative as notificações "
                                + "do HabitsCare nas configurações do aparelho."
                )
                .setNegativeButton("Agora não", null)
                .setPositiveButton(
                        "Abrir configurações",
                        (d, w) -> {

                            Intent intent = new Intent(
                                    Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            ).putExtra(
                                    Settings.EXTRA_APP_PACKAGE,
                                    ctx().getPackageName()
                            );

                            abrirTela(intent);
                        }
                )
                .show();
    }

    // =============================================
    // PERMISSÃO DE ALARME EXATO
    // =============================================

    /**
     * Explica ao usuário a necessidade da permissão
     * de alarmes exatos no Android 12 ou superior.
     */
    private void explicarAlarmeExato() {

        if (b == null) return;

        new AlertDialog.Builder(ctx())
                .setTitle("Alarmes no horário exato")
                .setMessage(
                        "Para avisar exatamente na hora marcada, permita "
                                + "\"Alarmes e lembretes\" para o HabitsCare. "
                                + "Sem isso os lembretes continuam funcionando, "
                                + "mas podem atrasar alguns minutos."
                )
                .setNegativeButton("Agora não", null)
                .setPositiveButton(
                        "Permitir",
                        (d, w) -> abrirPermissaoAlarmeExato()
                )
                .show();
    }

    /**
     * Abre as configurações de alarmes exatos.
     */
    private void abrirPermissaoAlarmeExato() {

        abrirTela(
                AlarmScheduler.intentPermissaoAlarmeExato(ctx())
        );
    }

    /**
     * Abre uma tela de configurações do Android.
     *
     * Caso a tela solicitada não esteja disponível,
     * tenta abrir as configurações gerais do aplicativo.
     */
    private void abrirTela(Intent intent) {

        try {

            fragment.startActivity(intent);

        } catch (ActivityNotFoundException e) {

            try {

                fragment.startActivity(
                        new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + ctx().getPackageName())
                        )
                );

            } catch (ActivityNotFoundException ignorada) {

                aviso("Não foi possível abrir as configurações.");
            }
        }
    }

    // =============================================
    // HORÁRIOS E INTERVALOS
    // =============================================

    /**
     * Permite ao usuário escolher o horário de dormir.
     */
    private void escolherHorarioSono() {

        final Context ctx = ctx();

        new TimePickerDialog(
                ctx,
                (view, hora, minuto) -> {

                    // Salva o horário escolhido.
                    LembretesPrefs.setSonoHorario(ctx, hora, minuto);

                    // Reagenda caso o lembrete esteja ativado.
                    if (LembretesPrefs.isSonoAtivo(ctx)) {

                        AlarmScheduler.agendarSono(ctx);
                    }

                    atualizarUi();
                },
                LembretesPrefs.getSonoHora(ctx),
                LembretesPrefs.getSonoMinuto(ctx),
                true
        ).show();
    }

    /**
     * Permite ao usuário escolher o intervalo
     * entre os lembretes de água.
     */
    private void escolherIntervaloAgua() {

        final Context ctx = ctx();

        final String[] opcoes = {
                "A cada 1 hora",
                "A cada 2 horas",
                "A cada 3 horas"
        };

        int selecionado = Math.max(
                0,
                Math.min(
                        2,
                        LembretesPrefs.getAguaIntervaloH(ctx) - 1
                )
        );

        new AlertDialog.Builder(ctx)
                .setTitle("Lembrar de beber água")
                .setSingleChoiceItems(
                        opcoes,
                        selecionado,
                        (dialog, which) -> {

                            // Salva o intervalo escolhido.
                            LembretesPrefs.setAguaIntervaloH(
                                    ctx,
                                    which + 1
                            );

                            // Reagenda caso o lembrete esteja ativado.
                            if (LembretesPrefs.isAguaAtiva(ctx)) {

                                AlarmScheduler.agendarAgua(ctx);
                            }

                            atualizarUi();

                            dialog.dismiss();
                        }
                )
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Permite ao usuário escolher o horário da caminhada.
     */
    private void escolherHorarioCaminhada() {

        final Context ctx = ctx();

        new TimePickerDialog(
                ctx,
                (view, hora, minuto) -> {

                    // Salva o horário escolhido.
                    LembretesPrefs.setCaminhadaHorario(
                            ctx,
                            hora,
                            minuto
                    );

                    // Reagenda caso o lembrete esteja ativado.
                    if (LembretesPrefs.isCaminhadaAtiva(ctx)) {

                        AlarmScheduler.agendarCaminhada(ctx);
                    }

                    atualizarUi();
                },
                LembretesPrefs.getCaminhadaHora(ctx),
                LembretesPrefs.getCaminhadaMinuto(ctx),
                true
        ).show();
    }

    // =============================================
    // ATUALIZAÇÃO DA INTERFACE
    // =============================================

    /**
     * Atualiza os interruptores, horários e avisos
     * exibidos na tela de Perfil.
     */
    private void atualizarUi() {

        if (b == null) return;

        Context ctx = ctx();

        atualizando = true;

        // Recupera o estado dos lembretes.
        boolean sono = LembretesPrefs.isSonoAtivo(ctx);
        boolean agua = LembretesPrefs.isAguaAtiva(ctx);
        boolean caminhada = LembretesPrefs.isCaminhadaAtiva(ctx);

        // Atualiza os interruptores.
        b.swSono.setChecked(sono);
        b.swAgua.setChecked(agua);
        b.swCaminhada.setChecked(caminhada);

        // Atualiza o horário de dormir.
        b.btnHoraSono.setText(
                String.format(
                        Locale.getDefault(),
                        "Horário: %02d:%02d",
                        LembretesPrefs.getSonoHora(ctx),
                        LembretesPrefs.getSonoMinuto(ctx)
                )
        );

        // Atualiza o intervalo de água.
        int horas = LembretesPrefs.getAguaIntervaloH(ctx);

        b.btnIntervaloAgua.setText(
                "A cada " + horas
                        + (horas == 1 ? " hora" : " horas")
                        + " · das 8h às 22h"
        );

        // Atualiza o horário da caminhada.
        b.btnHoraCaminhada.setText(
                String.format(
                        Locale.getDefault(),
                        "Horário: %02d:%02d",
                        LembretesPrefs.getCaminhadaHora(ctx),
                        LembretesPrefs.getCaminhadaMinuto(ctx)
                )
        );

        // Verifica se falta a permissão de alarme exato.
        boolean faltaAlarmeExato =
                (sono || caminhada)
                        && !AlarmScheduler.podeAgendarExato(ctx);

        b.tvAvisoLembretes.setVisibility(
                faltaAlarmeExato ? View.VISIBLE : View.GONE
        );

        atualizando = false;
    }

    // =============================================
    // UTILIDADES
    // =============================================

    /**
     * Retorna o contexto do Fragment.
     */
    private Context ctx() {
        return fragment.requireContext();
    }

    /**
     * Exibe uma mensagem ao usuário.
     */
    private void aviso(String mensagem) {

        if (fragment.getContext() != null) {

            Toast.makeText(
                    fragment.getContext(),
                    mensagem,
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}