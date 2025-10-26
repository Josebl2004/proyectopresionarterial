package com.example.proyectopresionarterial;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserProfileViewModel extends AndroidViewModel {
    private final AppDatabase db;
    private final UserProfileDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> saveError = new MutableLiveData<>();

    public UserProfileViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        dao = db.userProfileDao();
    }

    public LiveData<UserProfile> getProfile() {
        return dao.getProfile();
    }

    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public LiveData<String> getSaveError() { return saveError; }

    public void updateProfile(UserProfile profile) {
        executor.execute(() -> {
            try {
                dao.insert(profile);
                saveSuccess.postValue(true);
            } catch (Exception e) {
                saveError.postValue(e.getMessage());
                saveSuccess.postValue(false);
            }
        });
    }

    public void saveProfile(String weight, String height, String dob, String gender) {
        try {
            UserProfile profile = new UserProfile();
            profile.weightLbs = weight.isEmpty() ? null : Float.parseFloat(weight);
            profile.heightCm = height.isEmpty() ? null : Integer.parseInt(height);
            profile.dateOfBirth = dob;
            profile.gender = gender;
            updateProfile(profile);
        } catch (NumberFormatException ex) {
            saveError.postValue("Valores numéricos inválidos");
            saveSuccess.postValue(false);
        }
    }

    /**
     * Refresca los datos del perfil desde la base de datos
     * Se usa principalmente en onResume() para actualizar la UI
     */
    public void refreshProfile() {
        // No necesitamos hacer nada especial aquí ya que estamos usando LiveData,
        // que automáticamente notifica a los observadores cuando los datos cambian.
        // Sin embargo, podríamos forzar una actualización en implementaciones futuras si es necesario.
    }
}
