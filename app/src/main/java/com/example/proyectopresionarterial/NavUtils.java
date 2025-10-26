package com.example.proyectopresionarterial;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

/** Utilidades para lanzar Activities de forma segura evitando ActivityNotFoundException */
public final class NavUtils {
    private NavUtils() {}

    public static void safeStart(Activity host, Class<?> target) {
        if (host == null || target == null) return;
        Intent intent = new Intent(host, target);
        PackageManager pm = host.getPackageManager();
        if (intent.resolveActivity(pm) != null) {
            host.startActivity(intent);
        } else {
            View root = host.findViewById(android.R.id.content);
            if (root != null) {
                Snackbar.make(root, host.getString(R.string.navigation_activity_missing), Snackbar.LENGTH_LONG).show();
            }
        }
    }
}

