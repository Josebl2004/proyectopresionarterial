package com.example.proyectopresionarterial;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    private final List<BloodPressureRecord> items = new ArrayList<>();
    private OnRecordClickListener listener;

    // Interfaz para manejar clics en los elementos
    public interface OnRecordClickListener {
        void onRecordClick(BloodPressureRecord record);
    }

    // Constructor sin parámetros para uso en HistoryFragment
    public HistoryAdapter() {
        this.listener = null;
    }

    // Constructor con listener para uso en HistoryActivity
    public HistoryAdapter(OnRecordClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<BloodPressureRecord> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BloodPressureRecord r = items.get(position);
        String cls = r.getClassification();
        if (cls == null || cls.isEmpty()) {
            cls = ClassificationHelper.classify(r.getSystolic(), r.getDiastolic());
        }
        h.tvClassification.setText(ucFirst(cls));
        h.tvValues.setText(r.getSystolic() + "/" + r.getDiastolic() + " mmHg \u2022 " + r.getHeartRate() + " bpm");

        String dateLabel = r.getDate();
        try {
            SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outFmt = new SimpleDateFormat("dd/MM", Locale.getDefault());
            dateLabel = outFmt.format(inFmt.parse(r.getDate()));
        } catch (ParseException ignored) {}
        h.tvMeta.setText(dateLabel + " " + r.getTime() + " \u2022 " + r.getCondition());

        // Configurar chip y color de card según clasificación
        applyStatusChip(h.chipStatus, cls);
        applyCardColor(h.card, cls);

        // Configurar el click listener
        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecordClick(r);
            }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvClassification;
        final TextView tvValues;
        final TextView tvMeta;
        final MaterialButton chipStatus;
        VH(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvClassification = itemView.findViewById(R.id.tvClassification);
            tvValues = itemView.findViewById(R.id.tvValues);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }
    }

    private void applyStatusChip(MaterialButton chip, String classification) {
        if (chip == null) return;
        String c = classification != null ? classification.toLowerCase(Locale.ROOT) : "";
        int bg;
        int textColor;
        String text;
        if (c.contains("hipertensión") || c.contains("hipertension")) {
            bg = ContextCompat.getColor(chip.getContext(), R.color.bp_red);
            textColor = ContextCompat.getColor(chip.getContext(), R.color.onSecondary);
            text = "Hipertensión";
        } else if (c.contains("elevada")) {
            bg = ContextCompat.getColor(chip.getContext(), R.color.bp_yellow);
            textColor = ContextCompat.getColor(chip.getContext(), android.R.color.black);
            text = "Elevada";
        } else {
            bg = ContextCompat.getColor(chip.getContext(), R.color.bp_green);
            textColor = ContextCompat.getColor(chip.getContext(), R.color.onSecondary);
            text = "Normal";
        }
        chip.setText(text);
        chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bg));
        chip.setTextColor(textColor);
    }

    private void applyCardColor(MaterialCardView card, String classification) {
        String c = classification.toLowerCase(Locale.ROOT);
        int color;
        if (c.contains("hipertensión") || c.contains("hipertension")) {
            color = ContextCompat.getColor(card.getContext(), R.color.bp_red);
        } else if (c.contains("elevada")) {
            color = ContextCompat.getColor(card.getContext(), R.color.bp_yellow);
        } else {
            color = ContextCompat.getColor(card.getContext(), R.color.bp_green);
        }
        card.setStrokeColor(color);
        card.setCardBackgroundColor(adjustAlpha(color, 0.08f));
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(android.graphics.Color.alpha(color) * factor);
        int red = android.graphics.Color.red(color);
        int green = android.graphics.Color.green(color);
        int blue = android.graphics.Color.blue(color);
        return android.graphics.Color.argb(alpha, red, green, blue);
    }

    private String ucFirst(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0,1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }
}
