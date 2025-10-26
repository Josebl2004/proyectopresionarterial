package com.example.proyectopresionarterial;

import android.app.Application;
import android.text.TextUtils;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Obtiene una recomendación detallada considerando hasta 3 mediciones recientes. */
public final class DetailedRecommendationFetcher {

    public interface CallbackRec {
        void onSuccess(String recommendation);
        void onError(String message);
    }

    private DetailedRecommendationFetcher() {}

    public static void fetch(Application app, List<BloodPressureRecord> lastRecords, CallbackRec cb) {
        if (app == null) { if (cb != null) cb.onError("app null"); return; }
        if (lastRecords == null || lastRecords.isEmpty()) { if (cb != null) cb.onError("sin registros"); return; }
        if (!NetworkUtils.isNetworkAvailable(app)) { if (cb != null) cb.onError("sin red"); return; }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Analiza las últimas mediciones de presión arterial del paciente y ofrece recomendaciones detalladas en español. ");
        prompt.append("Incluye consejos de estilo de vida, señales de alerta y seguimiento sugerido. Máximo 8-10 frases, claras y accionables. ");
        prompt.append("Mediciones (más reciente primero):\n");
        int idx = 1;
        for (BloodPressureRecord r : lastRecords) {
            String cls = r.getClassification();
            if (TextUtils.isEmpty(cls)) cls = ClassificationHelper.classify(r.getSystolic(), r.getDiastolic());
            prompt.append(idx++).append(") ")
                  .append(r.getDate()).append(" ").append(r.getTime()).append(" — ")
                  .append(r.getSystolic()).append('/').append(r.getDiastolic()).append(" mmHg, FC ")
                  .append(r.getHeartRate()).append(" bpm, Condición: ")
                  .append(r.getCondition()).append(", Clasificación: ")
                  .append(cls).append("\n");
        }
        prompt.append("No contradigas las clasificaciones locales: para 'normal' o 'elevada' evita diagnosticar hipertensión o sugerir medicación. Si hay hipertensión, recomienda consulta médica y seguimiento sin alarmismo salvo crisis.");

        // Más tokens y menor temperatura para un texto estable y completo
        OpenAIRequest req = new OpenAIRequest(prompt.toString(), 0.5, 700,
                "Eres un profesional de la salud. Redacta recomendaciones personalizadas y responsables en español, basadas en varias mediciones de presión arterial.");

        BloodPressureApiService service = RetrofitProvider.getService();
        service.classify(req).enqueue(new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(Call<OpenAIResponse> call, Response<OpenAIResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (cb != null) cb.onError("HTTP " + response.code());
                    return;
                }
                String rec = response.body().toApiResponse().getRecommendation();
                if (rec == null) rec = "No se pudo generar una recomendación detallada.";
                if (cb != null) cb.onSuccess(rec.trim());
            }

            @Override
            public void onFailure(Call<OpenAIResponse> call, Throwable t) {
                if (cb != null) cb.onError(t.getMessage() == null ? "error" : t.getMessage());
            }
        });
    }
}

