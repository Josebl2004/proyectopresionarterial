package com.example.proyectopresionarterial;

public final class ClassificationHelper {
    private ClassificationHelper() {}

    public static String classify(int systolic, int diastolic) {
        // OMS criterios
        if (systolic >= 140 || diastolic >= 90) {
            return "hipertensión etapa 2";
        }
        if ((systolic >= 130 && systolic <= 139) || (diastolic >= 80 && diastolic <= 89)) {
            return "hipertensión etapa 1";
        }
        if (systolic >= 120 && systolic <= 129 && diastolic < 80) {
            return "elevada";
        }
        if (systolic < 120 && diastolic < 80) {
            return "normal";
        }
        // Fallback: si no encaja exactamente, usa la peor categoría aplicable
        if (systolic >= 130 || diastolic >= 80) {
            return "hipertensión etapa 1";
        }
        return "normal";
    }
}

