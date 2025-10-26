package com.example.proyectopresionarterial;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class BloodPressureRequest {
    @SerializedName("systolic")
    @Expose
    public int systolic;

    @SerializedName("diastolic")
    @Expose
    public int diastolic;

    @SerializedName("heartRate")
    @Expose
    public int heartRate;

    @SerializedName("condition")
    @Expose
    public String condition;

    // Opcionales
    @SerializedName("bmi")
    @Expose
    public Float bmi; // null si no disponible

    @SerializedName("age")
    @Expose
    public Integer age; // null si no disponible

    @SerializedName("gender")
    @Expose
    public String gender; // null si no disponible
}

