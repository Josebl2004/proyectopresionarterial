package com.example.proyectopresionarterial;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddRecordViewModel extends AndroidViewModel {

    private static final String TAG = "AddRecordVM";

    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> infoMessage = new MutableLiveData<>();
    private final MutableLiveData<BloodPressureRecord> record = new MutableLiveData<>();

    public AddRecordViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
    }

    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public LiveData<String> getInfoMessage() { return infoMessage; }
    public LiveData<BloodPressureRecord> getRecord() { return record; }

    public void saveRecord(String systolicStr, String diastolicStr, String heartRateStr, String condition) {
        // Validaciones básicas de vacío
        if (isEmpty(systolicStr) || isEmpty(diastolicStr) || isEmpty(heartRateStr) || isEmpty(condition)) {
            errorMessage.postValue("Completa todos los campos obligatorios");
            return;
        }

        int systolic, diastolic, heartRate;
        try {
            systolic = Integer.parseInt(systolicStr);
            diastolic = Integer.parseInt(diastolicStr);
            heartRate = Integer.parseInt(heartRateStr);
        } catch (NumberFormatException e) {
            errorMessage.postValue("Ingresa valores numéricos válidos");
            return;
        }

        // Rangos
        if (systolic < 50 || systolic > 250) {
            errorMessage.postValue("Sistólica debe estar entre 50 y 250");
            return;
        }
        if (diastolic < 30 || diastolic > 150) {
            errorMessage.postValue("Diastólica debe estar entre 30 y 150");
            return;
        }
        if (heartRate < 30 || heartRate > 200) {
            errorMessage.postValue("Frecuencia cardíaca debe estar entre 30 y 200");
            return;
        }

        // Construir el registro con fecha/hora actuales
        final BloodPressureRecord newRecord = new BloodPressureRecord();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String time = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
        newRecord.setDate(date);
        newRecord.setTime(time);
        newRecord.setSystolic(systolic);
        newRecord.setDiastolic(diastolic);
        newRecord.setHeartRate(heartRate);
        newRecord.setCondition(condition.toLowerCase(Locale.ROOT));
        // Clasificación local inmediata
        newRecord.setClassification(ClassificationHelper.classify(systolic, diastolic));
        newRecord.setSynced(false);

        executor.execute(() -> {
            try {
                long id = db.bloodPressureDao().insert(newRecord);
                newRecord.setId(id);

                if (!hasInternet()) {
                    infoMessage.postValue("Guardado localmente. Se sincronizará cuando haya conexión");
                }

                saveSuccess.postValue(true);

                // Webhook n8n: evento de creación (con recomendación si es posible)
                Log.d(TAG, "Webhook: encolando evento 'created' para id=" + newRecord.getId());
                postToWebhookWithRecommendation(newRecord, "created");

                // Intentar sincronizar con la API si hay internet (no bloquea la UI)
                syncRecord(newRecord);
            } catch (Exception e) {
                errorMessage.postValue("Error al guardar: " + e.getMessage());
                saveSuccess.postValue(false);
            }
        });
    }

    // Método sobrecargado para guardar con fecha y hora específicas
    public void saveRecord(String systolicStr, String diastolicStr, String heartRateStr,
                         String condition, String time, String date, String notes) {
        // Validaciones básicas de vacío
        if (isEmpty(systolicStr) || isEmpty(diastolicStr) || isEmpty(heartRateStr) || isEmpty(condition)) {
            errorMessage.postValue("Completa todos los campos obligatorios");
            return;
        }

        int systolic, diastolic, heartRate;
        try {
            systolic = Integer.parseInt(systolicStr);
            diastolic = Integer.parseInt(diastolicStr);
            heartRate = Integer.parseInt(heartRateStr);
        } catch (NumberFormatException e) {
            errorMessage.postValue("Ingresa valores numéricos válidos");
            return;
        }

        // Rangos
        if (systolic < 50 || systolic > 250) {
            errorMessage.postValue("Sistólica debe estar entre 50 y 250");
            return;
        }
        if (diastolic < 30 || diastolic > 150) {
            errorMessage.postValue("Diastólica debe estar entre 30 y 150");
            return;
        }
        if (heartRate < 30 || heartRate > 200) {
            errorMessage.postValue("Frecuencia cardíaca debe estar entre 30 y 200");
            return;
        }

        // Construir el registro con la fecha y hora proporcionadas
        final BloodPressureRecord newRecord = new BloodPressureRecord();
        newRecord.setDate(date);
        newRecord.setTime(time);
        newRecord.setSystolic(systolic);
        newRecord.setDiastolic(diastolic);
        newRecord.setHeartRate(heartRate);
        newRecord.setCondition(condition.toLowerCase(Locale.ROOT));
        // Nota: El parámetro notes se ignora porque BloodPressureRecord no tiene ese campo
        // Clasificación local inmediata
        newRecord.setClassification(ClassificationHelper.classify(systolic, diastolic));
        newRecord.setSynced(false);

        executor.execute(() -> {
            try {
                long id = db.bloodPressureDao().insert(newRecord);
                newRecord.setId(id);

                if (!hasInternet()) {
                    infoMessage.postValue("Guardado localmente. Se sincronizará cuando haya conexión");
                }

                saveSuccess.postValue(true);

                // Webhook n8n: evento de creación (con recomendación si es posible)
                Log.d(TAG, "Webhook: encolando evento 'created' (custom datetime) para id=" + newRecord.getId());
                postToWebhookWithRecommendation(newRecord, "created");

                // Intentar sincronizar con la API si hay internet (no bloquea la UI)
                syncRecord(newRecord);
            } catch (Exception e) {
                errorMessage.postValue("Error al guardar: " + e.getMessage());
                saveSuccess.postValue(false);
            }
        });
    }

    // Método para cargar un registro existente
    public void loadRecord(long recordId) {
        if (recordId <= 0) {
            errorMessage.postValue("ID de registro inválido");
            return;
        }

        executor.execute(() -> {
            try {
                BloodPressureRecord loadedRecord = db.bloodPressureDao().getById(recordId);
                if (loadedRecord != null) {
                    record.postValue(loadedRecord);
                } else {
                    errorMessage.postValue("No se encontró el registro");
                }
            } catch (Exception e) {
                errorMessage.postValue("Error al cargar: " + e.getMessage());
            }
        });
    }

    // Método para actualizar un registro existente
    public void updateRecord(long recordId, String systolicStr, String diastolicStr, String heartRateStr,
                           String condition, String time, String date, String notes) {
        // Validaciones básicas de vacío
        if (isEmpty(systolicStr) || isEmpty(diastolicStr) || isEmpty(heartRateStr) || isEmpty(condition)) {
            errorMessage.postValue("Completa todos los campos obligatorios");
            return;
        }

        int systolic, diastolic, heartRate;
        try {
            systolic = Integer.parseInt(systolicStr);
            diastolic = Integer.parseInt(diastolicStr);
            heartRate = Integer.parseInt(heartRateStr);
        } catch (NumberFormatException e) {
            errorMessage.postValue("Ingresa valores numéricos válidos");
            return;
        }

        // Rangos
        if (systolic < 50 || systolic > 250) {
            errorMessage.postValue("Sistólica debe estar entre 50 y 250");
            return;
        }
        if (diastolic < 30 || diastolic > 150) {
            errorMessage.postValue("Diastólica debe estar entre 30 y 150");
            return;
        }
        if (heartRate < 30 || heartRate > 200) {
            errorMessage.postValue("Frecuencia cardíaca debe estar entre 30 y 200");
            return;
        }

        executor.execute(() -> {
            try {
                BloodPressureRecord existingRecord = db.bloodPressureDao().getById(recordId);
                if (existingRecord != null) {
                    existingRecord.setSystolic(systolic);
                    existingRecord.setDiastolic(diastolic);
                    existingRecord.setHeartRate(heartRate);
                    existingRecord.setCondition(condition.toLowerCase(Locale.ROOT));
                    existingRecord.setTime(time);
                    existingRecord.setDate(date);
                    existingRecord.setClassification(ClassificationHelper.classify(systolic, diastolic));
                    existingRecord.setSynced(false);

                    db.bloodPressureDao().update(existingRecord);

                    if (!hasInternet()) {
                        infoMessage.postValue("Actualizado localmente. Se sincronizará cuando haya conexión");
                    }

                    saveSuccess.postValue(true);

                    // Webhook n8n: evento de actualización (con recomendación si es posible)
                    Log.d(TAG, "Webhook: encolando evento 'updated' para id=" + existingRecord.getId());
                    postToWebhookWithRecommendation(existingRecord, "updated");

                    // Intentar sincronizar con la API si hay internet
                    syncRecord(existingRecord);
                } else {
                    errorMessage.postValue("No se encontró el registro a actualizar");
                    saveSuccess.postValue(false);
                }
            } catch (Exception e) {
                errorMessage.postValue("Error al actualizar: " + e.getMessage());
                saveSuccess.postValue(false);
            }
        });
    }

    // Método para eliminar un registro
    public void deleteRecord(long recordId) {
        if (recordId <= 0) {
            errorMessage.postValue("ID de registro inválido");
            return;
        }

        executor.execute(() -> {
            try {
                BloodPressureRecord recordToDelete = db.bloodPressureDao().getById(recordId);
                if (recordToDelete != null) {
                    db.bloodPressureDao().delete(recordToDelete);
                    saveSuccess.postValue(true);

                    // Webhook n8n: evento de eliminación (sin recomendación)
                    Log.d(TAG, "Webhook: encolando evento 'deleted' para id=" + recordToDelete.getId());
                    postToWebhook(recordToDelete, "deleted");

                    // Si el registro estaba sincronizado, notificar a la API
                    if (recordToDelete.isSynced() && hasInternet()) {
                        // Aquí iría la llamada a la API para eliminar el registro en el servidor
                    }
                } else {
                    errorMessage.postValue("No se encontró el registro a eliminar");
                    saveSuccess.postValue(false);
                }
            } catch (Exception e) {
                errorMessage.postValue("Error al eliminar: " + e.getMessage());
                saveSuccess.postValue(false);
            }
        });
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean hasInternet() {
        ConnectivityManager cm = (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null &&
               (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    private void syncRecord(BloodPressureRecord record) {
        // Aquí iría la implementación de sincronización con la API
    }

    // Enviar al webhook si hay internet y está configurado (sin recomendación explícita)
    private void postToWebhook(BloodPressureRecord r, String event) {
        try {
            if (r == null) return;
            if (hasInternet()) {
                Log.d(TAG, "Webhook: enviando sin recomendación, event=" + event + ", id=" + r.getId());
                N8nWebhookClient.sendRecordAsync(getApplication(), r, event);
            } else {
                Log.w(TAG, "Webhook: sin internet, no se envía. event=" + event + ", id=" + r.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Webhook: error postToWebhook: " + e.getMessage());
        }
    }

    // Enviar al webhook, intentando primero obtener una recomendación corta de la API
    private void postToWebhookWithRecommendation(BloodPressureRecord r, String event) {
        if (r == null) return;
        if (!hasInternet()) {
            // Sin internet: generar recomendación local y enviar
            String localRec = RecommendationLocal.generate(r);
            Log.w(TAG, "Webhook: sin internet, envío con recomendación local. event=" + event + ", id=" + (r != null ? r.getId() : -1));
            try {
                N8nWebhookClient.sendRecordAsync(getApplication(), r, event, localRec);
            } catch (Exception e) {
                Log.e(TAG, "Webhook: error al enviar recomendación local sin internet: " + e.getMessage());
            }
            return;
        }
        try {
            RecommendationFetcher.fetchForRecord(getApplication(), r, new RecommendationFetcher.RecCallback() {
                @Override
                public void onSuccess(String recommendation) {
                    try {
                        Log.d(TAG, "Webhook: enviando con recomendación API. event=" + event + ", id=" + r.getId());
                        N8nWebhookClient.sendRecordAsync(getApplication(), r, event, recommendation);
                    } catch (Exception e) {
                        Log.e(TAG, "Webhook: error al enviar con recomendación API: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String message) {
                    // Fallback: generar y enviar recomendación local personalizada
                    String localRec = RecommendationLocal.generate(r);
                    Log.w(TAG, "Webhook: fallo recomendación API ('" + message + "'), envío con recomendación local. event=" + event + ", id=" + r.getId());
                    try {
                        N8nWebhookClient.sendRecordAsync(getApplication(), r, event, localRec);
                    } catch (Exception e) {
                        Log.e(TAG, "Webhook: error al enviar con recomendación local: " + e.getMessage());
                        // Último fallback: envío básico sin recomendación
                        postToWebhook(r, event);
                    }
                }
            });
        } catch (Exception e) {
            // Fallback: generar y enviar recomendación local si algo falla
            String localRec = RecommendationLocal.generate(r);
            Log.e(TAG, "Webhook: excepción al obtener recomendación API, envío con recomendación local. event=" + event + ", id=" + r.getId() + ". Error: " + e.getMessage());
            try {
                N8nWebhookClient.sendRecordAsync(getApplication(), r, event, localRec);
            } catch (Exception ex) {
                Log.e(TAG, "Webhook: error al enviar con recomendación local tras excepción: " + ex.getMessage());
                postToWebhook(r, event);
            }
        }
    }

    public void syncPendingRecords() {
        // Método para sincronizar todos los registros pendientes
        if (!hasInternet()) {
            return; // No hay internet, no intentar sincronizar
        }

        executor.execute(() -> {
            try {
                // Obtener todos los registros no sincronizados
                // List<BloodPressureRecord> pendingRecords = db.bloodPressureDao().getUnsyncedRecords();
                // for (BloodPressureRecord record : pendingRecords) {
                //     syncRecord(record);
                // }
                // Por ahora, este método no hace nada porque no tenemos la implementación completa de la API
            } catch (Exception e) {
                // Log del error si es necesario
                Log.e(TAG, "syncPendingRecords error: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
