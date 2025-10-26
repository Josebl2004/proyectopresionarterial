package com.example.proyectopresionarterial;

import android.app.Application;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel ligero para obtener recomendaciones desde la API
 * en base al último registro y (si hay) datos de perfil.
 */
public class RecommendationViewModel extends AndroidViewModel {

    private static final String TAG = "RecViewModel";
    private final AppDatabase db;
    private final RecommendationCacheDao cacheDao;
    private final MutableLiveData<String> recommendation = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private static final long CACHE_TTL_MS = BuildConfig.REC_CACHE_TTL_MS; // configurable via build config

    // Preferencias locales para firmar el contenido de caché sin migrar Room
    private static final String PREFS = "rec_cache_prefs";
    private static final String KEY_SIG = "cache_signature";
    private static final String KEY_UPDATED = "cache_updated_at";

    public RecommendationViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        cacheDao = db.recommendationCacheDao();
    }

    public LiveData<String> getRecommendation() { return recommendation; }
    public LiveData<String> getError() { return error; }

    public void fetchForRecord(BloodPressureRecord record) {
        if (record == null) { recommendation.postValue(null); return; }
        error.postValue(null);
        Application app = getApplication();

        Log.d(TAG, "Fetching recommendation for record: " + record.getId());

        // Calcular clasificación local para orientar prompt y validar caché
        final String classification = safeClassification(record.getSystolic(), record.getDiastolic());
        final String signature = buildSignature(record);

        // 1) Intentar mostrar caché válida (firma coincide y no expirada)
        io.execute(() -> {
            SharedPreferences sp = app.getSharedPreferences(PREFS, 0);
            String cachedSig = sp.getString(KEY_SIG, null);
            long cachedAt = sp.getLong(KEY_UPDATED, 0L);
            RecommendationCache cache = cacheDao.getNow();
            long age = System.currentTimeMillis() - cachedAt;

            if (cache != null && cache.recommendation != null && !cache.recommendation.trim().isEmpty()
                && cachedSig != null && cachedSig.equals(signature) && age <= CACHE_TTL_MS) {
                recommendation.postValue(cache.recommendation);
                Log.d(TAG, "Using cached recommendation for signature=" + signature + ", age=" + (age/1000) + "s");
            } else {
                Log.d(TAG, "Cache not used (missing/expired/mismatched signature)");
            }
        });

        try {
            if (!NetworkUtils.isNetworkAvailable(app)) {
                // Sin red: si la caché no coincidía, error
                io.execute(() -> {
                    SharedPreferences sp = app.getSharedPreferences(PREFS, 0);
                    String cachedSig = sp.getString(KEY_SIG, null);
                    if (cachedSig == null || !cachedSig.equals(signature)) {
                        String msg = app.getString(R.string.error_no_network);
                        error.postValue(msg);
                        Log.e(TAG, "Network error and no matching cache for signature " + signature);
                    }
                });
                return;
            }
        } catch (Exception ex) {
            String msg = app.getString(R.string.api_recommendations_error) + ": " + ex.getMessage();
            error.postValue(msg);
            Log.e(TAG, "Exception in API setup: ", ex);
            return;
        }

        // 2) Construir prompt dirigido y consistente con la clasificación actual
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Genera una recomendación breve y clara para el paciente basada en su última medición de presión arterial. ");
        promptBuilder.append("Usa un tono adecuado a la categoría y evita alarmismo innecesario. ");
        promptBuilder.append("Datos: Sistólica=").append(record.getSystolic()).append(" mmHg, ");
        promptBuilder.append("Diastólica=").append(record.getDiastolic()).append(" mmHg, ");
        promptBuilder.append("Frecuencia cardíaca=").append(record.getHeartRate()).append(" bpm");
        if (!TextUtils.isEmpty(record.getCondition())) {
            promptBuilder.append(", Condición=").append(record.getCondition());
        }
        promptBuilder.append(". Clasificación actual (según guías): ").append(classification).append(". ");
        promptBuilder.append("Instrucciones: ");
        promptBuilder.append("• Si la clasificación es 'normal', ofrece refuerzo positivo y consejos de mantenimiento (sueño, dieta baja en sal, actividad física). No uses términos de 'hipertensión' ni sugieras medicación. ");
        promptBuilder.append("• Si es 'elevada', sugiere cambios de estilo de vida y seguimiento; evita mencionar medicación o diagnosticar 'hipertensión' salvo que sea estrictamente necesario según datos. ");
        promptBuilder.append("• Si es 'hipertensión', recomienda consultar con su médico y control regular. Evita emergencia salvo que PAS≥180 o PAD≥120 o haya síntomas. ");
        promptBuilder.append("• No contradigas la clasificación indicada ni inventes datos. Responde en 3-4 frases como máximo.");

        io.execute(() -> {
            try {
                // Añadir perfil si está completo
                UserProfile profile = db.userProfileDao().getProfileNow();
                if (HealthUtils.isProfileComplete(profile)) {
                    float bmi = HealthUtils.calculateBmi(profile.weightLbs, profile.heightCm);
                    int age = HealthUtils.calculateAge(profile.dateOfBirth);
                    String gender = profile.gender;
                    promptBuilder.append(" Datos adicionales: IMC=")
                        .append(String.format(Locale.getDefault(), "%.1f", bmi))
                        .append(", Edad=").append(age)
                        .append(", Género=").append(gender).append('.');
                }

                BloodPressureApiService service = RetrofitProvider.getService();
                Log.d(TAG, "Calling OpenAI API for signature=" + signature + ", BP=" + record.getSystolic() + "/" + record.getDiastolic());

                OpenAIRequest openAIRequest = new OpenAIRequest(promptBuilder.toString());

                service.classify(openAIRequest).enqueue(new Callback<OpenAIResponse>() {
                    @Override
                    public void onResponse(Call<OpenAIResponse> call, Response<OpenAIResponse> response) {
                        Log.d(TAG, "API response code: " + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse apiResponse = response.body().toApiResponse();
                            String rec = apiResponse.getRecommendation();

                            // Sanitizar salida según clasificación local
                            rec = sanitizeRecommendation(rec, classification, app);

                            if (rec != null && !rec.trim().isEmpty()) {
                                final String finalRec = rec.trim();
                                recommendation.postValue(finalRec);
                                Log.d(TAG, "Got recommendation (trimmed), len=" + finalRec.length());

                                // Guardar en Room y firmar en SharedPreferences
                                io.execute(() -> {
                                    cacheDao.upsert(new RecommendationCache(finalRec, System.currentTimeMillis()));
                                    SharedPreferences sp = app.getSharedPreferences(PREFS, 0);
                                    sp.edit()
                                        .putString(KEY_SIG, signature)
                                        .putLong(KEY_UPDATED, System.currentTimeMillis())
                                        .apply();
                                });
                            } else {
                                String msg = app.getString(R.string.api_recommendations_empty);
                                error.postValue(msg);
                                Log.e(TAG, "API returned empty recommendation");
                            }
                        } else {
                            String errorBodyText = "No error body";
                            try (ResponseBody eb = response.errorBody()) {
                                if (eb != null) errorBodyText = eb.string();
                            } catch (Exception ignored) {}
                            String msg = "HTTP " + response.code() + ": " + errorBodyText;
                            error.postValue(msg);
                            Log.e(TAG, "API error: " + msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenAIResponse> call, Throwable t) {
                        Log.e(TAG, "API call failed", t);
                        io.execute(() -> {
                            // Usar caché solo si la firma coincide
                            SharedPreferences sp = app.getSharedPreferences(PREFS, 0);
                            String cachedSig = sp.getString(KEY_SIG, null);
                            RecommendationCache cache = cacheDao.getNow();
                            if (cachedSig != null && cachedSig.equals(signature) && cache != null && cache.recommendation != null && !cache.recommendation.trim().isEmpty()) {
                                recommendation.postValue(cache.recommendation + " (offline)");
                            } else {
                                String msg = "Error de API: " + (t.getMessage() == null ? "Error desconocido" : t.getMessage());
                                error.postValue(msg);
                            }
                        });
                    }
                });
            } catch (Exception e) {
                String msg = "Excepción: " + e.getMessage();
                error.postValue(msg);
                Log.e(TAG, "Exception during API call", e);
            }
        });
    }

    private String safeClassification(int systolic, int diastolic) {
        try {
            String c = ClassificationHelper.classify(systolic, diastolic);
            return c == null ? "normal" : c.toLowerCase(Locale.getDefault());
        } catch (Exception e) {
            return "normal";
        }
    }

    private String buildSignature(BloodPressureRecord r) {
        String cond = r.getCondition() == null ? "" : r.getCondition();
        return r.getSystolic() + "|" + r.getDiastolic() + "|" + r.getHeartRate() + "|" + cond;
    }

    // Asegura coherencia de salida respecto a la clasificación local
    private String sanitizeRecommendation(String rec, String classification, Application app) {
        if (rec == null) return null;
        String out = rec.trim();
        String cls = classification == null ? "" : classification.toLowerCase(Locale.getDefault());
        String lower = out.toLowerCase(Locale.getDefault());

        if (cls.contains("normal")) {
            if (lower.contains("hipertens")) {
                return "Tus valores actuales están dentro de rangos normales. Mantén hábitos saludables: limita la sal, realiza actividad física moderada y duerme bien. Continúa midiendo tu presión periódicamente.";
            }
            // Evitar sugerir medicación sin indicación
            if (lower.contains("medicac") || lower.contains("fárm") || lower.contains("trat")) {
                return "Tus valores son normales. No se requiere medicación. Mantén una dieta equilibrada, reduce la sal, haz actividad física y controla tu presión regularmente.";
            }
            return out;
        }

        if (cls.contains("elevada")) {
            boolean mentionsHypertension = lower.contains("hipertens");
            boolean mentionsDrugs = lower.contains("medicac") || lower.contains("fárm") || lower.contains("trat");
            if (mentionsHypertension || mentionsDrugs) {
                return "Tu presión está ligeramente elevada. Enfócate en cambios de estilo de vida: reduce la sal, aumenta actividad física, controla el peso y duerme bien. Repite la medición en distintos días y da seguimiento con tu médico si persiste.";
            }
            return out;
        }

        // Para hipertensión mantenemos la guía del modelo pero evitar alarmismo si no es crisis
        if (cls.contains("hipertens")) {
            // Si no hay crisis, evitar mensaje de emergencia
            return out;
        }

        // Fallback
        return out;
    }
}
