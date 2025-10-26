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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private StatsViewModel viewModel;
    private LineChart chart;
    private TextView tvEmpty;
    private TextView tvAvgPressure, tvAvgPulse, tvBasedOn, tvTrend;
    private TextView tvDistNormal, tvDistElevada, tvDistHip;
    private MaterialButton chipNormal, chipElevada, chipHip;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        chart = view.findViewById(R.id.chartTrend);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvAvgPressure = view.findViewById(R.id.tvAvgPressure);
        tvAvgPulse = view.findViewById(R.id.tvAvgPulse);
        tvBasedOn = view.findViewById(R.id.tvBasedOn);
        tvTrend = view.findViewById(R.id.tvTrend);
        tvDistNormal = view.findViewById(R.id.tvDistNormal);
        tvDistElevada = view.findViewById(R.id.tvDistElevada);
        tvDistHip = view.findViewById(R.id.tvDistHip);
        chipNormal = view.findViewById(R.id.chipNormal);
        chipElevada = view.findViewById(R.id.chipElevada);
        chipHip = view.findViewById(R.id.chipHip);

        setupChart(chart);
        StatsMarkerView marker = new StatsMarkerView(requireContext(), R.layout.marker_stats);
        chart.setMarker(marker);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);
        viewModel.getLast7Days().observe(getViewLifecycleOwner(), this::render);
    }

    private void setupChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("No hay registros aún");
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.getAxisRight().setEnabled(false);

        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(12f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
    }

    private void render(List<BloodPressureRecord> records) {
        if (records == null || records.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            chart.clear();
            return;
        } else {
            tvEmpty.setVisibility(View.GONE);
        }

        // Promedios
        int sumSys = 0, sumDia = 0, sumHr = 0;
        int n = 0;
        int countNormal = 0, countElevada = 0, countHip = 0;

        for (BloodPressureRecord r : records) {
            sumSys += r.getSystolic();
            sumDia += r.getDiastolic();
            sumHr += r.getHeartRate();
            n++;
            String c = r.getClassification();
            if (c != null) {
                c = c.toLowerCase(Locale.ROOT);
                if (c.contains("hipertensión") || c.contains("hipertension")) countHip++;
                else if (c.contains("elevada")) countElevada++;
                else if (c.contains("normal")) countNormal++;
            }
        }

        int avgSys = Math.round(sumSys / (float) n);
        int avgDia = Math.round(sumDia / (float) n);
        int avgHr = Math.round(sumHr / (float) n);
        tvAvgPressure.setText(avgSys + "/" + avgDia);
        tvAvgPulse.setText(String.valueOf(avgHr));
        tvBasedOn.setText("Basado en " + n + (n == 1 ? " medición" : " mediciones"));

        // Tendencia simple: comparar últimos 2 puntos de sistólica
        String trendText;
        int colorTrend;
        if (records.size() >= 2) {
            int last = records.get(records.size() - 1).getSystolic();
            int prev = records.get(records.size() - 2).getSystolic();
            if (last < prev) { trendText = "↘ Bajando"; colorTrend = ContextCompat.getColor(requireContext(), R.color.bp_green);}
            else if (last > prev) { trendText = "↗ Subiendo"; colorTrend = ContextCompat.getColor(requireContext(), R.color.bp_red);}
            else { trendText = "→ Estable"; colorTrend = ContextCompat.getColor(requireContext(), R.color.bp_yellow);}
        } else { trendText = "→ Estable"; colorTrend = ContextCompat.getColor(requireContext(), R.color.bp_yellow);}
        tvTrend.setText(trendText);
        tvTrend.setTextColor(colorTrend);

        // Distribución con porcentajes
        tvDistNormal.setText(formatCountPct(countNormal, n));
        tvDistElevada.setText(formatCountPct(countElevada, n));
        tvDistHip.setText(formatCountPct(countHip, n));

        // Tendencia (chart único con dos líneas)
        List<Entry> sysEntries = new ArrayList<>();
        List<Entry> diaEntries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat outFmt = new SimpleDateFormat("dd/MM", Locale.getDefault());
        for (int i = 0; i < records.size(); i++) {
            BloodPressureRecord r = records.get(i);
            sysEntries.add(new Entry(i, r.getSystolic()));
            diaEntries.add(new Entry(i, r.getDiastolic()));
            String label;
            try { label = outFmt.format(inFmt.parse(r.getDate())); }
            catch (ParseException e) { label = r.getDate(); }
            xLabels.add(label);
        }
        int red = ContextCompat.getColor(requireContext(), R.color.bp_red);
        int primary = ContextCompat.getColor(requireContext(), R.color.primaryColor);
        LineDataSet sysSet = new LineDataSet(sysEntries, "Sistólica");
        sysSet.setColor(red); sysSet.setCircleColor(red); sysSet.setLineWidth(2f); sysSet.setCircleRadius(3.5f); sysSet.setDrawValues(false);
        LineDataSet diaSet = new LineDataSet(diaEntries, "Diastólica");
        diaSet.setColor(primary); diaSet.setCircleColor(primary); diaSet.setLineWidth(2f); diaSet.setCircleRadius(3.5f); diaSet.setDrawValues(false);
        chart.setData(new LineData(sysSet, diaSet));
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                int idx = (int) value; return (idx >= 0 && idx < xLabels.size()) ? xLabels.get(idx) : "";
            }
        });
        chart.invalidate();
    }

    private String formatCountPct(int count, int total) {
        if (total <= 0) return "0 (0%)";
        int pct = Math.round((count * 100f) / total);
        return count + " (" + pct + "%)";
    }
}
