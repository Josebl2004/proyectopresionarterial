package com.example.proyectopresionarterial;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {
    @PrimaryKey
    public int id = 1; // único perfil local

    public Float weightLbs; // puede ser null si no completado
    public Integer heightCm; // puede ser null
    public String dateOfBirth; // yyyy-MM-dd, puede ser null
    public String gender; // "masculino", "femenino", "otro", puede ser null
    public String lastUpdated; // yyyy-MM-dd HH:mm, puede ser null

    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Float getWeightLbs() { return weightLbs; }
    public void setWeightLbs(Float weightLbs) { this.weightLbs = weightLbs; }

    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
}

