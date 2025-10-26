package com.example.proyectopresionarterial;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryViewModel extends AndroidViewModel {

    // Constantes para filtros temporales
    public static final int FILTER_ALL = 0;
    public static final int FILTER_WEEK = 1;
    public static final int FILTER_MONTH = 2;
    public static final int FILTER_YEAR = 3;

    private final BloodPressureDao dao;
    private final MutableLiveData<List<BloodPressureRecord>> records = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private String searchQuery = "";
    private boolean filterNormal = true;
    private boolean filterElevated = true;
    private boolean filterHypertension = true;
    private boolean orderByDateAsc = false;
    private boolean orderByDateDesc = true;  // por defecto, más reciente primero
    private boolean orderByValueAsc = false;
    private boolean orderByValueDesc = false;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        dao = AppDatabase.getInstance(application).bloodPressureDao();
    }

    public LiveData<List<BloodPressureRecord>> getAllOrdered() {
        return dao.observeAllOrdered();
    }

    public LiveData<List<BloodPressureRecord>> getRecords() {
        return records;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query;
        // Recargar los datos con el nuevo filtro
        loadRecords(FILTER_ALL, new ArrayList<>());
    }

    public void loadRecords(int timeframe, List<String> conditions) {
        new Thread(() -> {
            try {
                // Cargar registros desde la base de datos según filtros
                List<BloodPressureRecord> allRecords = dao.getAll();

                // Aplicar filtros de tiempo
                List<BloodPressureRecord> filtered = filterByTimeframe(allRecords, timeframe);

                // Aplicar filtros de condición
                if (conditions != null && !conditions.isEmpty()) {
                    filtered = filterByConditions(filtered, conditions);
                }

                // Aplicar filtro de búsqueda
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    filtered = filterBySearch(filtered, searchQuery);
                }

                // Ordenar según las preferencias
                filtered = applyOrdering(filtered);

                // Actualizar LiveData en el hilo principal
                records.postValue(filtered);

            } catch (Exception e) {
                Log.e("HistoryViewModel", "Error loading records", e);
                error.postValue("Error cargando registros: " + e.getMessage());
            }
        }).start();
    }

    private List<BloodPressureRecord> filterByTimeframe(List<BloodPressureRecord> records, int timeframe) {
        if (timeframe == FILTER_ALL) return records;

        List<BloodPressureRecord> filtered = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();

        // Establecer fecha límite según el filtro
        cal.add(Calendar.DAY_OF_YEAR, -7); // una semana atrás
        Date weekAgo = timeframe == FILTER_WEEK ? cal.getTime() : null;

        cal.setTime(today);
        cal.add(Calendar.MONTH, -1); // un mes atrás
        Date monthAgo = timeframe == FILTER_MONTH ? cal.getTime() : null;

        cal.setTime(today);
        cal.add(Calendar.YEAR, -1); // un año atrás
        Date yearAgo = timeframe == FILTER_YEAR ? cal.getTime() : null;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (BloodPressureRecord record : records) {
            try {
                Date recordDate = sdf.parse(record.getDate());
                if (recordDate != null) {
                    if ((timeframe == FILTER_WEEK && recordDate.after(weekAgo)) ||
                        (timeframe == FILTER_MONTH && recordDate.after(monthAgo)) ||
                        (timeframe == FILTER_YEAR && recordDate.after(yearAgo))) {
                        filtered.add(record);
                    }
                }
            } catch (Exception e) {
                // Error parsing date, skip record
            }
        }

        return filtered;
    }

    private List<BloodPressureRecord> filterByConditions(List<BloodPressureRecord> records, List<String> conditions) {
        List<BloodPressureRecord> filtered = new ArrayList<>();

        for (BloodPressureRecord record : records) {
            if (record.getCondition() != null) {
                for (String condition : conditions) {
                    if (record.getCondition().toLowerCase().contains(condition.toLowerCase())) {
                        filtered.add(record);
                        break;
                    }
                }
            }
        }

        return filtered;
    }

    private List<BloodPressureRecord> filterBySearch(List<BloodPressureRecord> records, String query) {
        if (query == null || query.isEmpty()) return records;

        List<BloodPressureRecord> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (BloodPressureRecord record : records) {
            // Buscar en fecha, hora, condición o clasificación
            if ((record.getDate() != null && record.getDate().toLowerCase().contains(lowerQuery)) ||
                (record.getTime() != null && record.getTime().toLowerCase().contains(lowerQuery)) ||
                (record.getCondition() != null && record.getCondition().toLowerCase().contains(lowerQuery)) ||
                (record.getClassification() != null && record.getClassification().toLowerCase().contains(lowerQuery))) {
                filtered.add(record);
            }
        }

        return filtered;
    }

    private List<BloodPressureRecord> applyOrdering(List<BloodPressureRecord> records) {
        // Implementar ordenamiento según flags
        // Por simplicidad, aquí solo se mantiene la lista original
        return records;
    }

    public void deleteRecord(BloodPressureRecord record) {
        new Thread(() -> {
            try {
                dao.delete(record);
                // Recargar datos después de eliminar
                loadRecords(FILTER_ALL, new ArrayList<>());
            } catch (Exception e) {
                error.postValue("Error eliminando registro: " + e.getMessage());
            }
        }).start();
    }

    public void exportAsCsv(Context context) {
        // Implementación para exportar como CSV
        error.postValue("Función próximamente disponible");
    }

    public void exportAsPdf(Context context) {
        // Implementación para exportar como PDF
        error.postValue("Función próximamente disponible");
    }

    public void shareCurrentView(Context context) {
        // Implementación para compartir la vista actual
        error.postValue("Función próximamente disponible");
    }

    public void setFilters(boolean normal, boolean elevated, boolean hypertension,
                          boolean dateAsc, boolean dateDesc, boolean valueAsc, boolean valueDesc) {
        this.filterNormal = normal;
        this.filterElevated = elevated;
        this.filterHypertension = hypertension;
        this.orderByDateAsc = dateAsc;
        this.orderByDateDesc = dateDesc;
        this.orderByValueAsc = valueAsc;
        this.orderByValueDesc = valueDesc;
    }

    public void resetFilters() {
        filterNormal = true;
        filterElevated = true;
        filterHypertension = true;
        orderByDateAsc = false;
        orderByDateDesc = true;
        orderByValueAsc = false;
        orderByValueDesc = false;
    }

    // Getters para los estados de filtro
    public boolean isFilterNormal() { return filterNormal; }
    public boolean isFilterElevated() { return filterElevated; }
    public boolean isFilterHypertension() { return filterHypertension; }
    public boolean isOrderByDateAsc() { return orderByDateAsc; }
    public boolean isOrderByDateDesc() { return orderByDateDesc; }
    public boolean isOrderByValueAsc() { return orderByValueAsc; }
    public boolean isOrderByValueDesc() { return orderByValueDesc; }

    /**
     * Refresca los datos de los registros históricos
     * Se usa principalmente en onResume() para actualizar la UI
     */
    public void refreshData() {
        // Recargar los registros con los filtros actuales
        loadRecords(FILTER_ALL, new ArrayList<>());
    }
}
