package com.example.proyectopresionarterial;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "blood_pressure_records")
public class BloodPressureRecord {

    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    @Expose(serialize = false)
    private long id;

    // Fecha en formato ISO 8601: yyyy-MM-dd
    @SerializedName("date")
    @Expose(serialize = false)
    private String date;

    // Hora en formato HH:mm
    @SerializedName("time")
    @Expose(serialize = false)
    private String time;

    // Presión sistólica en mmHg
    @SerializedName("systolic")
    @Expose
    private int systolic;

    // Presión diastólica en mmHg
    @SerializedName("diastolic")
    @Expose
    private int diastolic;

    // Frecuencia cardíaca en bpm
    @SerializedName("heartRate")
    @Expose
    private int heartRate;

    // Condición: "reposo", "ejercicio", "estrés", etc.
    @SerializedName("condition")
    @Expose
    private String condition;

    // Clasificación: "normal", "elevada", "hipertensión" (opcional)
    @SerializedName("classification")
    @Expose(serialize = false)
    private String classification; // puede ser null inicialmente

    // Indica si ya se envió a la API
    @SerializedName("synced")
    @Expose(serialize = false)
    private boolean synced;

    // Constructor vacío requerido por Room y Gson
    public BloodPressureRecord() {
    }

    // Getters y Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getSystolic() {
        return systolic;
    }

    public void setSystolic(int systolic) {
        this.systolic = systolic;
    }

    public int getDiastolic() {
        return diastolic;
    }

    public void setDiastolic(int diastolic) {
        this.diastolic = diastolic;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }
}
