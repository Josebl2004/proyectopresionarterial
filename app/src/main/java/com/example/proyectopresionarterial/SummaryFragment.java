package com.example.proyectopresionarterial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class SummaryFragment extends Fragment {

    private SummaryViewModel viewModel;
    private MaterialCardView cardToday;
    private TextView tvStatusToday, tvValuesToday, tvMetaToday, tvEmptySummary, tvLastPulse;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_summary, container, false);
        tvEmptySummary = v.findViewById(R.id.tvEmptySummary);
        cardToday = v.findViewById(R.id.cardToday);
        tvStatusToday = v.findViewById(R.id.tvStatusToday);
        tvValuesToday = v.findViewById(R.id.tvValuesToday);
        tvMetaToday = v.findViewById(R.id.tvMetaToday);
        tvLastPulse = v.findViewById(R.id.tvLastPulse);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SummaryViewModel.class);
        viewModel.getLatest().observe(getViewLifecycleOwner(), record -> {
            if (record == null) {
                tvEmptySummary.setVisibility(View.VISIBLE);
                cardToday.setVisibility(View.GONE);
                return;
            }
            tvEmptySummary.setVisibility(View.GONE);
            cardToday.setVisibility(View.VISIBLE);

            String classification = record.getClassification();
            if (classification == null || classification.isEmpty()) {
                classification = ClassificationHelper.classify(record.getSystolic(), record.getDiastolic());
            }
            tvStatusToday.setText(ucFirst(classification));
            tvValuesToday.setText(getString(R.string.values_today, record.getSystolic(), record.getDiastolic(), record.getHeartRate()));
            if (tvLastPulse != null) tvLastPulse.setVisibility(View.GONE);

            String dateLabel = record.getDate();
            try {
                SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat outFmt = new SimpleDateFormat("dd/MM", Locale.getDefault());
                dateLabel = outFmt.format(inFmt.parse(record.getDate()));
            } catch (ParseException ignored) {}
            tvMetaToday.setText(getString(R.string.meta_today, dateLabel, record.getTime(), record.getCondition()));

            applyCardColor(classification);
        });
    }

    private void applyCardColor(String classification) {
        if (getContext() == null) return;
        String c = classification.toLowerCase(Locale.ROOT);
        int color;
        if (c.contains("hipertensión") || c.contains("hipertension")) {
            color = ContextCompat.getColor(requireContext(), R.color.bp_red);
        } else if (c.contains("elevada")) {
            color = ContextCompat.getColor(requireContext(), R.color.bp_yellow);
        } else {
            color = ContextCompat.getColor(requireContext(), R.color.bp_green);
        }
        cardToday.setStrokeColor(color);
        cardToday.setCardBackgroundColor(adjustAlpha(color, 0.08f));
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
