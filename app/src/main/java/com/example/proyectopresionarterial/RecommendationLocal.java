package com.example.proyectopresionarterial;

import android.text.TextUtils;

import java.util.Locale;

/** Genera una recomendación breve (2-3 frases) según la medición actual. */
public final class RecommendationLocal {

    private RecommendationLocal() {}

    public static String generate(BloodPressureRecord r) {
        if (r == null) return null;
        String classification;
        try {
            classification = r.getClassification();
            if (TextUtils.isEmpty(classification)) {
                classification = ClassificationHelper.classify(r.getSystolic(), r.getDiastolic());
            }
        } catch (Exception e) {
            classification = "normal";
        }
        return generate(r.getSystolic(), r.getDiastolic(), r.getHeartRate(), r.getCondition(), classification);
    }

    public static String generate(int systolic, int diastolic, int heartRate, String condition, String classification) {
        String cls = classification == null ? "" : classification.toLowerCase(Locale.getDefault());
        String cond = condition == null ? "" : condition.toLowerCase(Locale.getDefault());

        boolean crisis = systolic >= 180 || diastolic >= 120;
        boolean tachy = heartRate > 100;
        boolean brady = heartRate > 0 && heartRate < 50;

        StringBuilder sb = new StringBuilder();

        if (crisis) {
            sb.append("Tus valores (" + systolic + "/" + diastolic + " mmHg) sugieren una urgencia. Busca atención médica inmediata si presentas síntomas como dolor de cabeza intenso, dolor torácico, dificultad para respirar o visión borrosa.");
            sb.append(" En lo posible, repite la medición tras 5 minutos de reposo y evita esfuerzos.");
            return sb.toString();
        }

        if (cls.contains("normal")) {
            sb.append("Tus valores (" + systolic + "/" + diastolic + " mmHg) están dentro de lo normal. Mantén hábitos saludables: limita la sal, realiza actividad física regular y duerme bien.");
            sb.append(" Controla tu presión periódicamente para dar seguimiento.");
        } else if (cls.contains("elevad")) { // 'elevada'
            sb.append("Tu presión (" + systolic + "/" + diastolic + " mmHg) está ligeramente elevada. Refuerza cambios de estilo de vida: reduce sal, cuida el peso y aumenta la actividad física.");
            sb.append(" Repite mediciones en días distintos; si persiste por 2-3 semanas, consulta con tu médico.");
        } else if (cls.contains("estadio 1") || cls.contains("hipertensión estadio 1") || cls.contains("hipertension 1")) {
            sb.append("Tus valores sugieren hipertensión estadio 1. Prioriza cambios de estilo de vida (menos sal, ejercicio, control de peso) y monitoriza 3-4 veces por semana.");
            sb.append(" Agenda una consulta médica para evaluación y plan de seguimiento.");
        } else if (cls.contains("estadio 2") || cls.contains("hipertensión estadio 2") || cls.contains("hipertension 2")) {
            sb.append("Tus valores indican hipertensión estadio 2. Es recomendable consultar con tu médico pronto para ajustar el plan de tratamiento.");
            sb.append(" Mientras tanto, reduce la sal, evita alcohol en exceso y monitoriza con mayor frecuencia.");
        } else {
            // Desconocido: recomendar prudencia
            sb.append("Con estos valores (" + systolic + "/" + diastolic + " mmHg) se sugiere reforzar hábitos saludables y repetir la medición en reposo.");
            sb.append(" Si se mantienen elevados, consulta con tu médico.");
        }

        if (!cond.isEmpty()) {
            if (cond.contains("ejercicio") || cond.contains("actividad")) {
                sb.append(" Como fue tras ejercicio, repite la medición en reposo para comparar.");
            } else if (cond.contains("estrés") || cond.contains("estres")) {
                sb.append(" Considera técnicas de manejo de estrés (respiración, pausas activas).");
            }
        }

        if (tachy) {
            sb.append(" Nota: la frecuencia cardíaca es alta (" + heartRate + " bpm). Si persiste, consulta.");
        } else if (brady) {
            sb.append(" Nota: la frecuencia cardíaca es baja (" + heartRate + " bpm). Si presentas mareos o malestar, consulta.");
        }

        return sb.toString();
    }
}

