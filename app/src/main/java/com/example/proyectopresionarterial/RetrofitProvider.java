package com.example.proyectopresionarterial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitProvider {
    private static volatile BloodPressureApiService SERVICE;

    // Ahora se usan las constantes generadas en BuildConfig (configurables desde gradle/local.properties)

    private RetrofitProvider() {}

    public static BloodPressureApiService getService() {
        if (SERVICE == null) {
            synchronized (RetrofitProvider.class) {
                if (SERVICE == null) {
                    SERVICE = build().create(BloodPressureApiService.class);
                }
            }
        }
        return SERVICE;
    }

    public static void reset() { SERVICE = null; }

    private static Retrofit build() {
        Gson gson = new GsonBuilder().create();

        // Usar la URL base configurada en BuildConfig (por defecto definida en app/build.gradle.kts)
        final String baseUrl = BuildConfig.API_BASE_URL;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();

                        // Agregar header Authorization solo si la API key está configurada
                        String apiKey = BuildConfig.API_KEY != null ? BuildConfig.API_KEY.trim() : "";
                        if (!apiKey.isEmpty()) {
                            builder.addHeader("Authorization", "Bearer " + apiKey);
                        }
                        builder.addHeader("Content-Type", "application/json");

                        return chain.proceed(builder.build());
                    }
                })
                .build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
}
