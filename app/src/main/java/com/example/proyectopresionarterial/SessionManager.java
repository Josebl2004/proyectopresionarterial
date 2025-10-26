package com.example.proyectopresionarterial;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionManager {
    private static final String PREFS_NAME = "session_prefs";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_NAME = "user_name";

    private SessionManager() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveLoginState(Context context, boolean isLoggedIn) {
        prefs(context).edit().putBoolean(KEY_LOGGED_IN, isLoggedIn).apply();
    }

    public static void saveUserName(Context context, String userName) {
        prefs(context).edit().putString(KEY_USER_NAME, userName).apply();
    }

    public static String getUserName(Context context) {
        return prefs(context).getString(KEY_USER_NAME, "Usuario");
    }

    public static boolean isLoggedIn(Context context) {
        return prefs(context).getBoolean(KEY_LOGGED_IN, false);
    }

    public static void clearSession(Context context) {
        prefs(context).edit().clear().apply();
    }
}
