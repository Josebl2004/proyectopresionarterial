package com.example.proyectopresionarterial;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

/**
 * Cliente minimalista para enviar eventos al webhook de n8n.
 * Configurar en res/values/strings.xml el valor de n8n_webhook_url con el link del webhook.
 * Alternativamente, definir N8N_WEBHOOK_URL en gradle.properties (BuildConfig) para sobrescribir.
 */
public final class N8nWebhookClient {

    private static final String TAG = "N8nWebhook";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build();

    private static final Gson gson = new Gson();

    private N8nWebhookClient() {}

    /**
     * Compatibilidad hacia atrás: envía sin recomendación.
     */
    public static void sendRecordAsync(Application app, BloodPressureRecord record, String eventType) {
        sendRecordAsync(app, record, eventType, null);
    }

    /**
     * Envía el registro al webhook de n8n de forma asíncrona. "eventType" puede ser
     * "created", "updated" o "deleted". Si se proporciona recommendation, se incluirá en el payload.
     */
    public static void sendRecordAsync(Application app, BloodPressureRecord record, String eventType, String recommendation) {
        if (app == null || record == null) return;

        String url = null;
        try {
            // 1) Preferir URL desde BuildConfig (gradle.properties)
            String cfg = BuildConfig.N8N_WEBHOOK_URL;
            if (!TextUtils.isEmpty(cfg)) {
                url = cfg.trim();
            }
            // 2) Fallback a resources si BuildConfig está vacío
            if (TextUtils.isEmpty(url)) {
                url = app.getString(R.string.n8n_webhook_url);
                if (!TextUtils.isEmpty(url)) url = url.trim();
            }
        } catch (Exception ignored) {}

        if (TextUtils.isEmpty(url)) {
            Log.w(TAG, "n8n_webhook_url vacío: no se enviará el evento");
            return;
        }
        if (!NetworkUtils.isNetworkAvailable(app)) {
            Log.w(TAG, "Sin red: no se enviará el evento al webhook");
            return;
        }

        // Construir payload
        Payload payload = new Payload();
        payload.event = eventType == null ? "created" : eventType.toLowerCase(Locale.getDefault());
        payload.id = record.getId();
        payload.date = record.getDate();
        payload.time = record.getTime();
        payload.systolic = record.getSystolic();
        payload.diastolic = record.getDiastolic();
        payload.heartRate = record.getHeartRate();
        payload.classification = record.getClassification() != null ? record.getClassification() :
                ClassificationHelper.classify(record.getSystolic(), record.getDiastolic());
        payload.condition = record.getCondition();
        payload.recommendation = (recommendation == null ? null : recommendation.trim());
        try { payload.userName = SessionManager.getUserName(app); } catch (Exception ignored) {}

        String body = gson.toJson(payload);
        RequestBody requestBody = RequestBody.create(body, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Log.d(TAG, "Enviando evento '" + payload.event + "' al webhook: " + url + " (id=" + record.getId() + ")");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error enviando a n8n: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response r = response) {
                    if (!r.isSuccessful()) {
                        Log.e(TAG, "Webhook n8n HTTP " + r.code() + ": " + (r.body() != null ? r.body().string() : ""));
                    } else {
                        Log.d(TAG, "Evento enviado a n8n OK: " + payload.event + " (#" + record.getId() + ")");
                    }
                }
            }
        });
    }

    // POJO de payload
    static class Payload {
        @SerializedName("event") String event;
        @SerializedName("id") long id;
        @SerializedName("date") String date;
        @SerializedName("time") String time;
        @SerializedName("systolic") int systolic;
        @SerializedName("diastolic") int diastolic;
        @SerializedName("heartRate") int heartRate;
        @SerializedName("classification") String classification;
        @SerializedName("condition") String condition;
        @SerializedName("userName") String userName;
        @SerializedName("recommendation") String recommendation;
    }
}
