package com.example.proyectopresionarterial;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recommendation_cache")
public class RecommendationCache {
    @PrimaryKey
    public int id = 1; // singleton
    public String recommendation;
    public long updatedAt; // epoch millis

    public RecommendationCache() {}
    public RecommendationCache(@NonNull String recommendation, long updatedAt) {
        this.recommendation = recommendation;
        this.updatedAt = updatedAt;
    }
}

