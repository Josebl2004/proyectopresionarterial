package com.example.proyectopresionarterial;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class SummaryViewModel extends AndroidViewModel {

    private final BloodPressureDao dao;

    public SummaryViewModel(@NonNull Application application) {
        super(application);
        dao = AppDatabase.getInstance(application).bloodPressureDao();
    }

    public LiveData<BloodPressureRecord> getLatest() {
        return dao.observeLatest();
    }
}

