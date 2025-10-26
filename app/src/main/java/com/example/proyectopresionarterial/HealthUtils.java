package com.example.proyectopresionarterial;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class HealthUtils {
    private HealthUtils() {}

    public static boolean isProfileComplete(UserProfile p) {
        return p != null && p.weightLbs != null && p.heightCm != null &&
                p.dateOfBirth != null && !p.dateOfBirth.trim().isEmpty() &&
                p.gender != null && !p.gender.trim().isEmpty();
    }

    public static Float calculateBmi(Float weightLbs, Integer heightCm) {
        if (weightLbs == null || heightCm == null || heightCm == 0) return null;
        float kg = weightLbs * 0.45359237f;
        float m = heightCm / 100.0f;
        if (m <= 0f) return null;
        return kg / (m * m);
    }

    public static String getBmiCategory(float bmi) {
        if (bmi < 18.5f) {
            return "Bajo peso";
        } else if (bmi < 25.0f) {
            return "Normal";
        } else if (bmi < 30.0f) {
            return "Sobrepeso";
        } else {
            return "Obesidad";
        }
    }

    public static Integer calculateAge(String dateOfBirth) {
        if (dateOfBirth == null || dateOfBirth.trim().isEmpty()) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date dob = sdf.parse(dateOfBirth);
            if (dob == null) return null;
            Calendar birth = Calendar.getInstance();
            birth.setTime(dob);
            Calendar today = Calendar.getInstance();
            if (dob.after(today.getTime())) return null;
            int age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return Math.max(age, 0);
        } catch (ParseException e) {
            return null;
        }
    }
}
