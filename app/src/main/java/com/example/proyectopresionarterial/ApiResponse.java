package com.example.proyectopresionarterial;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    @SerializedName("classification")
    private String classification;

    @SerializedName("recommendation")
    private String recommendation;

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}

