package com.example.proyectopresionarterial;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

public class App extends Application {
    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        // StrictMode solo en debug para detectar posibles ANRs o accesos indebidos.
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
        // Handler global de excepciones NO fatales: loguea y deja que el sistema cierre si es crítico.
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.e(TAG, "Excepción no capturada en hilo " + t.getName(), e);
        });
    }
}

