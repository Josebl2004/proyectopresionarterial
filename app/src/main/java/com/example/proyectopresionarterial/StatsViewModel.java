package com.example.proyectopresionarterial;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsViewModel extends AndroidViewModel {

    private final BloodPressureDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<BloodPressureRecord>> statsData = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> averages = new MutableLiveData<>();
    private final MutableLiveData<Integer> trend = new MutableLiveData<>();

    // Constantes para tendencias
    public static final int TREND_IMPROVING = 1;
    public static final int TREND_STABLE = 0;
    public static final int TREND_WORSENING = -1;

    public StatsViewModel(@NonNull Application application) {
        super(application);
        dao = AppDatabase.getInstance(application).bloodPressureDao();
    }

    public LiveData<List<BloodPressureRecord>> getStatsData() {
        return statsData;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Map<String, Integer>> getAverages() {
        return averages;
    }

    public LiveData<Integer> getTrend() {
        return trend;
    }

    public LiveData<List<BloodPressureRecord>> getLast7Days() {
        String end = formatDate(new Date());
        String start = formatDate(daysAgo(6));
        return dao.observeBetweenDates(start, end);
    }

    public LiveData<List<BloodPressureRecord>> getWeekAverages() {
        return getLast7Days();
    }

    public void loadWeekData() {
        executor.execute(() -> {
            try {
                String end = formatDate(new Date());
                String start = formatDate(daysAgo(6));
                List<BloodPressureRecord> records = dao.getBetweenDates(start, end);
                statsData.postValue(records);
                calculateAverages(records);
                calculateTrend(records);
            } catch (Exception e) {
                error.postValue("Error al cargar datos semanales: " + e.getMessage());
            }
        });
    }

    public void loadMonthData() {
        executor.execute(() -> {
            try {
                String end = formatDate(new Date());
                String start = formatDate(daysAgo(29));
                List<BloodPressureRecord> records = dao.getBetweenDates(start, end);
                statsData.postValue(records);
                calculateAverages(records);
                calculateTrend(records);
            } catch (Exception e) {
                error.postValue("Error al cargar datos mensuales: " + e.getMessage());
            }
        });
    }

    public void loadYearData() {
        executor.execute(() -> {
            try {
                String end = formatDate(new Date());
                String start = formatDate(daysAgo(364));
                List<BloodPressureRecord> records = dao.getBetweenDates(start, end);
                statsData.postValue(records);
                calculateAverages(records);
                calculateTrend(records);
            } catch (Exception e) {
                error.postValue("Error al cargar datos anuales: " + e.getMessage());
            }
        });
    }

    public void loadAllData() {
        executor.execute(() -> {
            try {
                List<BloodPressureRecord> records = dao.getAll();
                statsData.postValue(records);
                calculateAverages(records);
                calculateTrend(records);
            } catch (Exception e) {
                error.postValue("Error al cargar todos los datos: " + e.getMessage());
            }
        });
    }

    /**
     * Método para refrescar los datos de la última semana
     * Este método se usa para forzar la recarga de datos cuando el usuario
     * presiona el botón "Ver más en Tendencia últimos 7 días"
     */
    public void refreshWeekData() {
        executor.execute(() -> {
            try {
                String end = formatDate(new Date());
                String start = formatDate(daysAgo(6));
                List<BloodPressureRecord> records = dao.getBetweenDates(start, end);
                statsData.postValue(records);
                calculateAverages(records);
                calculateTrend(records);
            } catch (Exception e) {
                error.postValue("Error al actualizar datos semanales: " + e.getMessage());
            }
        });
    }

    /**
     * Refresca todos los datos estadísticos
     * Se usa principalmente en onResume() para actualizar la UI
     */
    public void refreshData() {
        // Recargar los datos más recientes
        loadWeekData();
    }

    private void calculateAverages(List<BloodPressureRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        int totalSystolic = 0;
        int totalDiastolic = 0;
        int totalHeartRate = 0;

        for (BloodPressureRecord record : records) {
            totalSystolic += record.getSystolic();
            totalDiastolic += record.getDiastolic();
            totalHeartRate += record.getHeartRate();
        }

        Map<String, Integer> avgMap = new HashMap<>();
        avgMap.put("systolic", totalSystolic / records.size());
        avgMap.put("diastolic", totalDiastolic / records.size());
        avgMap.put("heartRate", totalHeartRate / records.size());

        averages.postValue(avgMap);
    }

    private void calculateTrend(List<BloodPressureRecord> records) {
        if (records == null || records.size() < 2) {
            trend.postValue(TREND_STABLE);
            return;
        }

        // Comparar primera mitad con segunda mitad
        int halfSize = records.size() / 2;

        int firstHalfAvg = 0;
        int secondHalfAvg = 0;

        for (int i = 0; i < halfSize; i++) {
            firstHalfAvg += records.get(i).getSystolic();
        }
        firstHalfAvg /= halfSize;

        for (int i = halfSize; i < records.size(); i++) {
            secondHalfAvg += records.get(i).getSystolic();
        }
        secondHalfAvg /= (records.size() - halfSize);

        // Determinar tendencia
        int difference = secondHalfAvg - firstHalfAvg;

        if (difference > 5) {
            trend.postValue(TREND_WORSENING);
        } else if (difference < -5) {
            trend.postValue(TREND_IMPROVING);
        } else {
            trend.postValue(TREND_STABLE);
        }
    }

    public void shareStatsAsImage(Context context, View chartView) {
        // Implementación básica - se puede mejorar
        error.postValue("Función de compartir como imagen en desarrollo");
    }

    public void shareStatsAsText(Context context) {
        executor.execute(() -> {
            try {
                List<BloodPressureRecord> records = statsData.getValue();
                if (records == null || records.isEmpty()) {
                    error.postValue("No hay datos para compartir");
                    return;
                }

                StringBuilder text = new StringBuilder();
                text.append("Estadísticas de Presión Arterial\n\n");

                Map<String, Integer> avg = averages.getValue();
                if (avg != null) {
                    text.append("Promedios:\n");
                    text.append(String.format("Sistólica: %d mmHg\n", avg.get("systolic")));
                    text.append(String.format("Diastólica: %d mmHg\n", avg.get("diastolic")));
                    text.append(String.format("Frecuencia cardíaca: %d lpm\n\n", avg.get("heartRate")));
                }

                text.append(String.format("Total de registros: %d\n", records.size()));

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, text.toString());
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(Intent.createChooser(shareIntent, "Compartir estadísticas"));
            } catch (Exception e) {
                error.postValue("Error al compartir: " + e.getMessage());
            }
        });
    }

    public void shareStatsAsPdf(Context context) {
        // Implementación básica - se puede mejorar
        error.postValue("Función de compartir como PDF en desarrollo");
    }

    private Date daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        return cal.getTime();
    }

    private String formatDate(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
