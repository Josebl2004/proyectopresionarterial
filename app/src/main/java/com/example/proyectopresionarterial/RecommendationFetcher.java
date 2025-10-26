package com.example.proyectopresionarterial;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Helper ligero para obtener una recomendación de la API para un registro específico
 * y devolverla mediante callback. Incluye una sanitización mínima para mantener
 * coherencia con la clasificación local.
 */
public final class RecommendationFetcher {

    private static final String TAG = "RecFetcher";

    public interface RecCallback {
        void onSuccess(String recommendation);
        void onError(String message);
    }

    private RecommendationFetcher() {}

    public static void fetchForRecord(Application app, BloodPressureRecord record, RecCallback cb) {
        if (app == null || record == null) { if (cb != null) cb.onError("parámetros inválidos"); return; }
        if (!NetworkUtils.isNetworkAvailable(app)) { if (cb != null) cb.onError("sin red"); return; }

        String classification = safeClassification(record.getSystolic(), record.getDiastolic());

        StringBuilder prompt = new StringBuilder();
        prompt.append("Genera una recomendación breve (2-3 frases) para el paciente basada en su última medición de presión arterial. ");
        prompt.append("Datos: Sistólica=").append(record.getSystolic()).append(" mmHg, ");
        prompt.append("Diastólica=").append(record.getDiastolic()).append(" mmHg, ");
        prompt.append("Frecuencia cardíaca=").append(record.getHeartRate()).append(" bpm");
        if (!TextUtils.isEmpty(record.getCondition())) {
            prompt.append(", Condición=").append(record.getCondition());
        }
        prompt.append(". Clasificación local: ").append(classification).append(". ");
        prompt.append("No contradigas la clasificación: si es 'normal' o 'elevada', no menciones hipertensión ni medicación. Responde en español.");

        BloodPressureApiService service = RetrofitProvider.getService();
        OpenAIRequest req = new OpenAIRequest(prompt.toString());
        service.classify(req).enqueue(new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(Call<OpenAIResponse> call, Response<OpenAIResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (cb != null) cb.onError("HTTP " + response.code());
                    return;
                }
                String rec = response.body().toApiResponse().getRecommendation();
                String sanitized = sanitize(rec, classification);
                if (cb != null) cb.onSuccess(sanitized);
            }

            @Override
            public void onFailure(Call<OpenAIResponse> call, Throwable t) {
                if (cb != null) cb.onError(t.getMessage() == null ? "error" : t.getMessage());
            }
        });
    }

    private static String safeClassification(int s, int d) {
        try {
            String c = ClassificationHelper.classify(s, d);
            return c == null ? "normal" : c.toLowerCase(Locale.getDefault());
        } catch (Exception e) {
            return "normal";
        }
    }

    private static String sanitize(String rec, String cls) {
        if (rec == null) return null;
        String out = rec.trim();
        String lower = out.toLowerCase(Locale.getDefault());
        String c = cls == null ? "" : cls.toLowerCase(Locale.getDefault());
        if (c.contains("normal")) {
            if (lower.contains("hipertens") || lower.contains("medicac") || lower.contains("fárm") || lower.contains("trat")) {
                return "Tus valores son normales. Mantén hábitos saludables: limita la sal, haz actividad física moderada y duerme bien. Controla tu presión periódicamente.";
            }
        } else if (c.contains("elevada")) {
            if (lower.contains("hipertens") || lower.contains("medicac") || lower.contains("fárm") || lower.contains("trat")) {
                return "Tu presión está ligeramente elevada. Prioriza cambios de estilo de vida: reduce sal, aumenta actividad, cuida el peso y el descanso. Repite mediciones y da seguimiento si persiste.";
            }
        }
        return out;
    }
}

