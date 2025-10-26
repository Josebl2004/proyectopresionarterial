package com.example.proyectopresionarterial;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {

    private final BloodPressureDao dao;
    private final MutableLiveData<String> healthImageUrl = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MainViewModel(@NonNull Application application) {
        super(application);
        dao = AppDatabase.getInstance(application).bloodPressureDao();
    }

    public LiveData<BloodPressureRecord> getLatestForToday() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        return dao.observeLatestForDate(today);
    }

    public LiveData<BloodPressureRecord> getLatest() {
        return dao.observeLatest();
    }

    // Nuevo: LiveData que prioriza hoy y cae a última general
    public LiveData<BloodPressureRecord> getLatestPreferred() {
        MediatorLiveData<BloodPressureRecord> result = new MediatorLiveData<>();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        LiveData<BloodPressureRecord> todayLive = dao.observeLatestForDate(today);
        LiveData<BloodPressureRecord> latestLive = dao.observeLatest();

        result.addSource(todayLive, rec -> {
            if (rec != null) {
                result.setValue(rec);
            } else {
                // si no hay medición hoy, usar la última global
                BloodPressureRecord current = result.getValue();
                if (current == null) {
                    BloodPressureRecord latest = latestLive.getValue();
                    if (latest != null) result.setValue(latest);
                }
            }
        });

        result.addSource(latestLive, rec -> {
            // Solo aplicar si aún no tenemos una de hoy
            BloodPressureRecord todayRec = todayLive.getValue();
            if (todayRec == null) {
                result.setValue(rec);
            }
        });

        return result;
    }

    public LiveData<String> getHealthImageUrl() {
        return healthImageUrl;
    }

    public void fetchHealthImage() {
        ImageHelper.fetchImage("blood pressure", new ImageHelper.ImageCallback() {
            @Override
            public void onImageUrl(String url) {
                healthImageUrl.postValue(url);
            }

            @Override
            public void onError() {
                healthImageUrl.postValue(null); // Fallback local en la vista
            }
        });
    }

    public void deleteRecord(BloodPressureRecord record) {
        executor.execute(() -> {
            if (record != null) {
                dao.delete(record);
            }
        });
    }

    public void refreshData() {
        // Este método fuerza una actualización de los datos
        // Como usamos LiveData con observe, los datos se actualizan automáticamente
        fetchHealthImage();
    }
}
