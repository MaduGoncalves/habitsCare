package com.example.habitscare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * O Android apaga todos os alarmes quando o celular reinicia (e em algumas outras situações).
 * Este receiver reagenda tudo quando:
 * - o celular liga;
 * - o app é atualizado;
 * - o fuso horário muda;
 * - a permissão de alarme exato é concedida ou revogada (Android 12+).
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String ACAO_PERMISSAO_ALARME_EXATO =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        switch (intent.getAction()) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
            case Intent.ACTION_TIMEZONE_CHANGED:
            case ACAO_PERMISSAO_ALARME_EXATO:
                AlarmScheduler.reagendarTudo(context.getApplicationContext());
                break;
            default:
                break;
        }
    }
}