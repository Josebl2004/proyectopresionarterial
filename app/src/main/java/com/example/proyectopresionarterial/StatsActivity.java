package com.example.proyectopresionarterial;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    private StatsViewModel viewModel;
    private LineChart lineChartTrend;
    private BarChart barChartWeekly;
    private PieChart pieChartDistribution;
    private CombinedChart combinedChartAll;
    private TextView tvNoData;
    private TextView tvAvgSystolic;
    private TextView tvAvgDiastolic;
    private TextView tvAvgHeartRate;
    private TextView tvTrend;
    private TextView tvAverageTitle;
    private MaterialButtonToggleGroup toggleTimeRange;
    private TabLayout tabLayoutChartType;
    private MaterialCardView cardSummary;
    private MaterialCardView cardChart;

    // Constantes para filtros de tiempo
    private static final int TIME_WEEK = 0;
    private static final int TIME_MONTH = 1;
    private static final int TIME_YEAR = 2;
    private static final int TIME_ALL = 3;

    // Constantes para tipos de gráficos
    private static final int CHART_TREND = 0;
    private static final int CHART_WEEKLY = 1;
    private static final int CHART_DISTRIBUTION = 2;
    private static final int CHART_COMBINED = 3;

    private int currentTimeRange = TIME_WEEK; // Por defecto
    private int currentChartType = CHART_TREND; // Por defecto

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bloquear acceso si no hay sesión
        if (!SessionManager.isLoggedIn(this)) {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_stats);

        // Configurar Toolbar con botón de volver
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
            toolbar.setNavigationOnClickListener(v -> finish());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.menu_stats_title));
            }
        }

        // Inicializar referencias a vistas
        initializeViews();

        // Configurar ViewModel
        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        // Configurar controles de UI
        setupUIControls();

        // Configurar gráficos
        setupCharts();

        // Observar cambios en datos
        observeViewModel();

        // Cargar datos iniciales
        loadData();
    }

    private void initializeViews() {
        lineChartTrend = findViewById(R.id.lineChartTrend);
        barChartWeekly = findViewById(R.id.barChartWeekly);
        pieChartDistribution = findViewById(R.id.pieChartDistribution);
        combinedChartAll = findViewById(R.id.combinedChartAll);
        tvNoData = findViewById(R.id.tvNoStatsData);
        tvAvgSystolic = findViewById(R.id.tvAvgSystolic);
        tvAvgDiastolic = findViewById(R.id.tvAvgDiastolic);
        tvAvgHeartRate = findViewById(R.id.tvAvgHeartRate);
        tvTrend = findViewById(R.id.tvTrend);
        tvAverageTitle = findViewById(R.id.tvAverageTitle);
        toggleTimeRange = findViewById(R.id.toggleTimeRange);
        tabLayoutChartType = findViewById(R.id.tabLayoutChartType);
        cardSummary = findViewById(R.id.cardSummary);
        cardChart = findViewById(R.id.cardChart);
    }

    private void setupUIControls() {
        // Configurar toggles de rango de tiempo
        toggleTimeRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnWeek) {
                    currentTimeRange = TIME_WEEK;
                } else if (checkedId == R.id.btnMonth) {
                    currentTimeRange = TIME_MONTH;
                } else if (checkedId == R.id.btnYear) {
                    currentTimeRange = TIME_YEAR;
                } else if (checkedId == R.id.btnAll) {
                    currentTimeRange = TIME_ALL;
                }
                loadData();
            }
        });

        // Configurar tabs para tipos de gráficos (comportamiento original)
        tabLayoutChartType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentChartType = tab.getPosition();
                updateChartVisibility();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    private void setupCharts() {
        // Configurar gráfico de líneas (tendencias)
        setupLineChart();

        // Configurar gráfico de barras (semanal)
        setupBarChart();

        // Configurar gráfico de pastel (distribución)
        setupPieChart();

        // Configurar gráfico combinado
        setupCombinedChart();

        // Mostrar solo el gráfico actual
        updateChartVisibility();
    }

    private void setupLineChart() {
        lineChartTrend.setDrawGridBackground(false);
        lineChartTrend.getDescription().setEnabled(false);
        lineChartTrend.setTouchEnabled(true);
        lineChartTrend.setDragEnabled(true);
        lineChartTrend.setScaleEnabled(true);
        lineChartTrend.setPinchZoom(true);
        lineChartTrend.setNoDataText(getString(R.string.no_data_available));

        // Configurar ejes
        XAxis xAxis = lineChartTrend.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = lineChartTrend.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(40f); // Valor mínimo razonable para presión

        YAxis rightAxis = lineChartTrend.getAxisRight();
        rightAxis.setEnabled(false);

        // Configurar leyenda
        Legend legend = lineChartTrend.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);

        // Configurar marcador personalizado
        StatsMarkerView mv = new StatsMarkerView(this, R.layout.marker_view_stats);
        mv.setChartView(lineChartTrend);
        lineChartTrend.setMarker(mv);

        // Agregar animación
        lineChartTrend.animateX(1000);
    }

    private void setupBarChart() {
        barChartWeekly.setDrawGridBackground(false);
        barChartWeekly.getDescription().setEnabled(false);
        barChartWeekly.setTouchEnabled(true);
        barChartWeekly.setDragEnabled(true);
        barChartWeekly.setScaleEnabled(true);
        barChartWeekly.setPinchZoom(false);
        barChartWeekly.setDoubleTapToZoomEnabled(false);
        barChartWeekly.setNoDataText(getString(R.string.no_data_available));

        // Configurar ejes
        XAxis xAxis = barChartWeekly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = barChartWeekly.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = barChartWeekly.getAxisRight();
        rightAxis.setEnabled(false);

        // Configurar leyenda
        Legend legend = barChartWeekly.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        // Agregar animación
        barChartWeekly.animateY(1000);
    }

    private void setupPieChart() {
        pieChartDistribution.setUsePercentValues(true);
        pieChartDistribution.getDescription().setEnabled(false);
        pieChartDistribution.setExtraOffsets(5, 10, 5, 5);
        pieChartDistribution.setDragDecelerationFrictionCoef(0.95f);
        pieChartDistribution.setCenterText(getString(R.string.classification_distribution));
        pieChartDistribution.setCenterTextSize(14f);
        pieChartDistribution.setDrawHoleEnabled(true);
        pieChartDistribution.setHoleColor(Color.WHITE);
        pieChartDistribution.setTransparentCircleRadius(61f);
        pieChartDistribution.setNoDataText(getString(R.string.no_data_available));

        // Configurar leyenda
        Legend legend = pieChartDistribution.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);

        // Agregar animación
        pieChartDistribution.animateY(1000);
    }

    private void setupCombinedChart() {
        combinedChartAll.getDescription().setEnabled(false);
        combinedChartAll.setBackgroundColor(Color.WHITE);
        combinedChartAll.setDrawGridBackground(false);
        combinedChartAll.setDrawBarShadow(false);
        combinedChartAll.setHighlightFullBarEnabled(false);
        combinedChartAll.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR,
                CombinedChart.DrawOrder.LINE
        });
        combinedChartAll.setNoDataText(getString(R.string.no_data_available));

        // Configurar ejes
        XAxis xAxis = combinedChartAll.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = combinedChartAll.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(40f);

        YAxis rightAxis = combinedChartAll.getAxisRight();
        rightAxis.setEnabled(false);

        // Configurar leyenda
        Legend legend = combinedChartAll.getLegend();
        legend.setWordWrapEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);

        // Agregar animación
        combinedChartAll.animateXY(1000, 1000);
    }

    private void updateChartVisibility() {
        lineChartTrend.setVisibility(currentChartType == CHART_TREND ? View.VISIBLE : View.GONE);
        barChartWeekly.setVisibility(currentChartType == CHART_WEEKLY ? View.VISIBLE : View.GONE);
        pieChartDistribution.setVisibility(currentChartType == CHART_DISTRIBUTION ? View.VISIBLE : View.GONE);
        combinedChartAll.setVisibility(currentChartType == CHART_COMBINED ? View.VISIBLE : View.GONE);
    }

    private void observeViewModel() {
        // Observar datos para gráficos
        viewModel.getStatsData().observe(this, data -> {
            if (data == null || data.isEmpty()) {
                showNoDataMessage();
            } else {
                hideNoDataMessage();
                updateCharts(data);
                updateSummaryStats(data);
            }
        });

        // Observar errores
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(findViewById(android.R.id.content),
                        error, Snackbar.LENGTH_LONG).show();
            }
        });

        // Observar promedios
        viewModel.getAverages().observe(this, averages -> {
            if (averages != null) {
                updateAveragesDisplay(averages);
            }
        });

        // Observar tendencia
        viewModel.getTrend().observe(this, trend -> {
            if (trend != null) {
                updateTrendDisplay(trend);
            }
        });
    }

    private void loadData() {
        // Actualizar título de promedios según rango seleccionado
        updateAverageTitleByTimeRange();

        // Cargar datos según el rango de tiempo seleccionado
        switch (currentTimeRange) {
            case TIME_WEEK:
                viewModel.loadWeekData();
                break;
            case TIME_MONTH:
                viewModel.loadMonthData();
                break;
            case TIME_YEAR:
                viewModel.loadYearData();
                break;
            case TIME_ALL:
                viewModel.loadAllData();
                break;
        }
    }

    private void updateAverageTitleByTimeRange() {
        String titleFormat = getString(R.string.average_title_format);
        String periodText;

        switch (currentTimeRange) {
            case TIME_WEEK:
                periodText = getString(R.string.period_week);
                break;
            case TIME_MONTH:
                periodText = getString(R.string.period_month);
                break;
            case TIME_YEAR:
                periodText = getString(R.string.period_year);
                break;
            default:
                periodText = getString(R.string.period_all);
                break;
        }

        tvAverageTitle.setText(String.format(titleFormat, periodText));
    }

    private void showNoDataMessage() {
        tvNoData.setVisibility(View.VISIBLE);
        cardSummary.setVisibility(View.GONE);
        cardChart.setVisibility(View.GONE);
    }

    private void hideNoDataMessage() {
        tvNoData.setVisibility(View.GONE);
        cardSummary.setVisibility(View.VISIBLE);
        cardChart.setVisibility(View.VISIBLE);
    }

    private void updateCharts(List<BloodPressureRecord> data) {
        switch (currentChartType) {
            case CHART_TREND:
                updateLineChart(data);
                break;
            case CHART_WEEKLY:
                updateBarChart(data);
                break;
            case CHART_DISTRIBUTION:
                updatePieChart(data);
                break;
            case CHART_COMBINED:
                updateCombinedChart(data);
                break;
        }
    }

    private void updateLineChart(List<BloodPressureRecord> data) {
        ArrayList<Entry> entriesSystolic = new ArrayList<>();
        ArrayList<Entry> entriesDiastolic = new ArrayList<>();
        ArrayList<Entry> entriesHeartRate = new ArrayList<>();
        ArrayList<String> xLabels = new ArrayList<>();

        // Lógica original: construir etiquetas dd/MM a partir de split de fecha
        for (int i = 0; i < data.size(); i++) {
            BloodPressureRecord record = data.get(i);
            entriesSystolic.add(new Entry(i, record.getSystolic()));
            entriesDiastolic.add(new Entry(i, record.getDiastolic()));
            entriesHeartRate.add(new Entry(i, record.getHeartRate()));

            try {
                String[] dateParts = record.getDate().split("/");
                String shortDate = dateParts[0] + "/" + dateParts[1];
                xLabels.add(shortDate);
            } catch (Exception e) {
                xLabels.add(record.getDate());
            }
        }

        LineDataSet systolicSet = new LineDataSet(entriesSystolic, getString(R.string.systolic));
        systolicSet.setColor(ContextCompat.getColor(this, R.color.systolic_color));
        systolicSet.setCircleColor(ContextCompat.getColor(this, R.color.systolic_color));
        systolicSet.setLineWidth(2f);
        systolicSet.setCircleRadius(3f);
        systolicSet.setDrawCircleHole(false);
        systolicSet.setValueTextSize(9f);
        systolicSet.setDrawFilled(true);
        systolicSet.setFillAlpha(65);
        systolicSet.setFillColor(ContextCompat.getColor(this, R.color.systolic_color_transparent));
        systolicSet.setDrawValues(false);

        LineDataSet diastolicSet = new LineDataSet(entriesDiastolic, getString(R.string.diastolic));
        diastolicSet.setColor(ContextCompat.getColor(this, R.color.diastolic_color));
        diastolicSet.setCircleColor(ContextCompat.getColor(this, R.color.diastolic_color));
        diastolicSet.setLineWidth(2f);
        diastolicSet.setCircleRadius(3f);
        diastolicSet.setDrawCircleHole(false);
        diastolicSet.setValueTextSize(9f);
        diastolicSet.setDrawFilled(true);
        diastolicSet.setFillAlpha(65);
        diastolicSet.setFillColor(ContextCompat.getColor(this, R.color.diastolic_color_transparent));
        diastolicSet.setDrawValues(false);

        LineDataSet heartRateSet = new LineDataSet(entriesHeartRate, getString(R.string.heart_rate));
        heartRateSet.setColor(ContextCompat.getColor(this, R.color.heart_rate_color));
        heartRateSet.setCircleColor(ContextCompat.getColor(this, R.color.heart_rate_color));
        heartRateSet.setLineWidth(2f);
        heartRateSet.setCircleRadius(3f);
        heartRateSet.setDrawCircleHole(false);
        heartRateSet.setValueTextSize(9f);
        heartRateSet.setDrawValues(false);

        XAxis xAxis = lineChartTrend.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));

        LineData lineData = new LineData(systolicSet, diastolicSet, heartRateSet);
        lineChartTrend.setData(lineData);
        lineChartTrend.invalidate();
    }

    private void updateBarChart(List<BloodPressureRecord> data) {
        ArrayList<BarEntry> entriesSystolic = new ArrayList<>();
        ArrayList<BarEntry> entriesDiastolic = new ArrayList<>();
        ArrayList<String> xLabels = new ArrayList<>();

        Map<String, List<BloodPressureRecord>> recordsByDay = groupByDayOfWeek(data);
        String[] daysOfWeek = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

        for (int i = 0; i < daysOfWeek.length; i++) {
            String day = daysOfWeek[i];
            List<BloodPressureRecord> dayRecords = recordsByDay.getOrDefault(day, new ArrayList<>());

            float avgSystolic = 0f;
            float avgDiastolic = 0f;
            if (!dayRecords.isEmpty()) {
                for (BloodPressureRecord record : dayRecords) {
                    avgSystolic += record.getSystolic();
                    avgDiastolic += record.getDiastolic();
                }
                avgSystolic /= dayRecords.size();
                avgDiastolic /= dayRecords.size();
            }

            entriesSystolic.add(new BarEntry(i, avgSystolic));
            entriesDiastolic.add(new BarEntry(i, avgDiastolic));
            xLabels.add(day);
        }

        BarDataSet systolicSet = new BarDataSet(entriesSystolic, getString(R.string.systolic));
        systolicSet.setColor(ContextCompat.getColor(this, R.color.systolic_color));
        systolicSet.setValueTextSize(10f);
        systolicSet.setDrawValues(true);

        BarDataSet diastolicSet = new BarDataSet(entriesDiastolic, getString(R.string.diastolic));
        diastolicSet.setColor(ContextCompat.getColor(this, R.color.diastolic_color));
        diastolicSet.setValueTextSize(10f);
        diastolicSet.setDrawValues(true);

        float groupSpace = 0.1f;
        float barSpace = 0.03f;
        float barWidth = 0.42f;

        XAxis xAxis = barChartWeekly.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setLabelCount(xLabels.size());

        BarData barData = new BarData(systolicSet, diastolicSet);
        barData.setBarWidth(barWidth);
        barChartWeekly.setData(barData);
        barChartWeekly.groupBars(0f, groupSpace, barSpace);
        barChartWeekly.invalidate();
    }

    private Map<String, List<BloodPressureRecord>> groupByDayOfWeek(List<BloodPressureRecord> data) {
        Map<String, List<BloodPressureRecord>> result = new HashMap<>();
        String[] daysOfWeek = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

        // Inicializar mapa
        for (String day : daysOfWeek) {
            result.put(day, new ArrayList<>());
        }

        // Lógica original: parsear dd/MM/yyyy
        for (BloodPressureRecord record : data) {
            try {
                String[] dateParts = record.getDate().split("/");
                int dayOfMonth = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]) - 1;
                int year = Integer.parseInt(dateParts[2]);

                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);

                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                int spanishDayOfWeek = (dayOfWeek + 5) % 7;

                String dayName = daysOfWeek[spanishDayOfWeek];
                result.get(dayName).add(record);
            } catch (Exception e) {
                // Ignorar en caso de error
            }
        }

        return result;
    }

    private void updatePieChart(List<BloodPressureRecord> data) {
        // Contar registros por clasificación
        Map<String, Integer> countByClassification = new HashMap<>();
        countByClassification.put(getString(R.string.category_normal), 0);
        countByClassification.put(getString(R.string.category_elevada), 0);
        countByClassification.put(getString(R.string.category_hipertension1), 0);
        countByClassification.put(getString(R.string.category_hipertension2), 0);

        for (BloodPressureRecord record : data) {
            String classification = record.getClassification();
            if (classification == null || classification.isEmpty()) {
                classification = ClassificationHelper.classify(record.getSystolic(), record.getDiastolic());
            }

            String key;
            if (classification.toLowerCase().contains("normal")) {
                key = getString(R.string.category_normal);
            } else if (classification.toLowerCase().contains("elevada")) {
                key = getString(R.string.category_elevada);
            } else if (classification.toLowerCase().contains("hipertensión 1") ||
                       classification.toLowerCase().contains("hipertension 1")) {
                key = getString(R.string.category_hipertension1);
            } else {
                key = getString(R.string.category_hipertension2);
            }

            countByClassification.put(key, countByClassification.get(key) + 1);
        }

        // Crear entradas para el gráfico de pastel
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countByClassification.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Configurar colores según clasificación
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(this, R.color.bp_green));
        colors.add(ContextCompat.getColor(this, R.color.bp_yellow));
        colors.add(ContextCompat.getColor(this, R.color.bp_orange));
        colors.add(ContextCompat.getColor(this, R.color.bp_red));
        dataSet.setColors(colors);

        // Configurar formato de valor
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f%%", value);
            }
        });

        PieData pieData = new PieData(dataSet);
        pieData.setValueTextSize(11f);
        pieData.setValueTextColor(Color.WHITE);

        pieChartDistribution.setData(pieData);
        pieChartDistribution.highlightValues(null);
        pieChartDistribution.invalidate();
    }

    private void updateCombinedChart(List<BloodPressureRecord> data) {
        ArrayList<Entry> entriesSystolic = new ArrayList<>();
        ArrayList<Entry> entriesDiastolic = new ArrayList<>();
        ArrayList<BarEntry> entriesHeartRate = new ArrayList<>();
        ArrayList<String> xLabels = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            BloodPressureRecord record = data.get(i);
            entriesSystolic.add(new Entry(i, record.getSystolic()));
            entriesDiastolic.add(new Entry(i, record.getDiastolic()));
            entriesHeartRate.add(new BarEntry(i, record.getHeartRate()));

            try {
                String[] dateParts = record.getDate().split("/");
                String shortDate = dateParts[0] + "/" + dateParts[1];
                xLabels.add(shortDate);
            } catch (Exception e) {
                xLabels.add(record.getDate());
            }
        }

        LineDataSet systolicSet = new LineDataSet(entriesSystolic, getString(R.string.systolic));
        systolicSet.setColor(ContextCompat.getColor(this, R.color.systolic_color));
        systolicSet.setCircleColor(ContextCompat.getColor(this, R.color.systolic_color));
        systolicSet.setLineWidth(2f);
        systolicSet.setCircleRadius(3f);
        systolicSet.setDrawCircleHole(false);
        systolicSet.setDrawValues(false);

        LineDataSet diastolicSet = new LineDataSet(entriesDiastolic, getString(R.string.diastolic));
        diastolicSet.setColor(ContextCompat.getColor(this, R.color.diastolic_color));
        diastolicSet.setCircleColor(ContextCompat.getColor(this, R.color.diastolic_color));
        diastolicSet.setLineWidth(2f);
        diastolicSet.setCircleRadius(3f);
        diastolicSet.setDrawCircleHole(false);
        diastolicSet.setDrawValues(false);

        BarDataSet heartRateSet = new BarDataSet(entriesHeartRate, getString(R.string.heart_rate));
        heartRateSet.setColor(ContextCompat.getColor(this, R.color.heart_rate_color));
        heartRateSet.setDrawValues(false);

        XAxis xAxis = combinedChartAll.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setLabelCount(Math.min(7, xLabels.size()));

        LineData lineData = new LineData(systolicSet, diastolicSet);
        BarData barData = new BarData(heartRateSet);
        barData.setBarWidth(0.6f);

        CombinedData combinedData = new CombinedData();
        combinedData.setData(lineData);
        combinedData.setData(barData);

        combinedChartAll.setData(combinedData);
        combinedChartAll.invalidate();
    }

    private void updateSummaryStats(List<BloodPressureRecord> data) {
        // Calcular promedios
        int totalSystolic = 0, totalDiastolic = 0, totalHeartRate = 0;

        for (BloodPressureRecord record : data) {
            totalSystolic += record.getSystolic();
            totalDiastolic += record.getDiastolic();
            totalHeartRate += record.getHeartRate();
        }

        int avgSystolic = totalSystolic / data.size();
        int avgDiastolic = totalDiastolic / data.size();
        int avgHeartRate = totalHeartRate / data.size();

        tvAvgSystolic.setText(String.valueOf(avgSystolic));
        tvAvgDiastolic.setText(String.valueOf(avgDiastolic));
        tvAvgHeartRate.setText(String.valueOf(avgHeartRate));
    }

    private void updateAveragesDisplay(Map<String, Integer> averages) {
        if (averages.containsKey("systolic")) {
            tvAvgSystolic.setText(String.valueOf(averages.get("systolic")));
        }

        if (averages.containsKey("diastolic")) {
            tvAvgDiastolic.setText(String.valueOf(averages.get("diastolic")));
        }

        if (averages.containsKey("heartRate")) {
            tvAvgHeartRate.setText(String.valueOf(averages.get("heartRate")));
        }
    }

    private void updateTrendDisplay(int trend) {
        String trendText;
        int trendColor;

        switch (trend) {
            case StatsViewModel.TREND_IMPROVING:
                trendText = getString(R.string.trend_improving);
                trendColor = ContextCompat.getColor(this, R.color.bp_green);
                break;
            case StatsViewModel.TREND_WORSENING:
                trendText = getString(R.string.trend_worsening);
                trendColor = ContextCompat.getColor(this, R.color.bp_red);
                break;
            default:
                trendText = getString(R.string.trend_stable);
                trendColor = ContextCompat.getColor(this, R.color.bp_yellow);
                break;
        }

        tvTrend.setText(trendText);
        tvTrend.setTextColor(trendColor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stats, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            loadData();
            return true;
        } else if (item.getItemId() == R.id.action_share) {
            shareStats();
            return true;
        } else if (item.getItemId() == R.id.action_help) {
            showHelp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareStats() {
        // Compartir estadísticas actuales
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.share_stats_title)
            .setItems(new String[]{
                    getString(R.string.share_stats_image),
                    getString(R.string.share_stats_text),
                    getString(R.string.share_stats_pdf)
            }, (dialog, which) -> {
                switch (which) {
                    case 0:
                        viewModel.shareStatsAsImage(this, getCurrentChart());
                        break;
                    case 1:
                        viewModel.shareStatsAsText(this);
                        break;
                    case 2:
                        viewModel.shareStatsAsPdf(this);
                        break;
                }
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    private View getCurrentChart() {
        switch (currentChartType) {
            case CHART_TREND:
                return lineChartTrend;
            case CHART_WEEKLY:
                return barChartWeekly;
            case CHART_DISTRIBUTION:
                return pieChartDistribution;
            case CHART_COMBINED:
                return combinedChartAll;
            default:
                return lineChartTrend;
        }
    }

    private void showHelp() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.stats_help_title)
            .setMessage(R.string.stats_help_message)
            .setPositiveButton(R.string.btn_close, null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Recargar los datos cuando la actividad vuelve a primer plano
        viewModel.refreshData();

        // Actualizar la visualización según el rango de tiempo y tipo de gráfico actual
        switch (currentTimeRange) {
            case TIME_WEEK:
                viewModel.loadWeekData();
                break;
            case TIME_MONTH:
                viewModel.loadMonthData();
                break;
            case TIME_YEAR:
                viewModel.loadYearData();
                break;
            case TIME_ALL:
                viewModel.loadAllData();
                break;
        }

        // Actualizar la interfaz con los nuevos datos
        updateChartDisplay();
    }

    private void updateChartDisplay() {
        // Método intencionalmente vacío (comportamiento original)
    }
}
