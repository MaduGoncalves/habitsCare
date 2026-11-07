package com.example.habitscare;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habitscare.databinding.ItemHistoricoBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class HistoricoAdapter extends ListAdapter<HistoricoDia, HistoricoAdapter.ViewHolder> {

    private static final Locale PT_BR = new Locale("pt", "BR");

    public HistoricoAdapter() {
        super(new DiffUtil.ItemCallback<HistoricoDia>() {
            @Override
            public boolean areItemsTheSame(@NonNull HistoricoDia a, @NonNull HistoricoDia b) {
                return a.dia.equals(b.dia);
            }

            @Override
            public boolean areContentsTheSame(@NonNull HistoricoDia a, @NonNull HistoricoDia b) {
                return a.litros == b.litros
                        && a.km == b.km
                        && Objects.equals(a.horarioDormir, b.horarioDormir)
                        && Objects.equals(a.horarioAcordar, b.horarioAcordar);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoricoBinding b = ItemHistoricoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoricoBinding b;

        ViewHolder(ItemHistoricoBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(HistoricoDia d) {
            b.tvData.setText(formatarData(d.dia));

            b.chipAgua.setText(String.format(Locale.getDefault(), "💧 %.2f L", d.litros));
            b.chipWalk.setText(String.format(Locale.getDefault(), "🚶 %.1f km", d.km));

            if (d.temSono()) {
                float horas = Repository.calcularHorasSono(d.horarioDormir, d.horarioAcordar);
                b.chipSono.setText(String.format(Locale.getDefault(), "🌙 %s → %s · %.1f h",
                        d.horarioDormir, d.horarioAcordar, horas));
            } else {
                b.chipSono.setText("🌙 — → —");
            }
        }

        /** "2026-11-07" -> "Sábado, 07 de nov." */
        private static String formatarData(String dia) {
            try {
                Date data = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dia);
                String texto = new SimpleDateFormat("EEEE, dd 'de' MMM", PT_BR).format(data);
                return texto.substring(0, 1).toUpperCase(PT_BR) + texto.substring(1);
            } catch (Exception e) {
                return dia;
            }
        }
    }
}