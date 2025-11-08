package com.example.resqnet_app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSessionManager {
    private static final String PREF_NAME = "UserPrefs"; // ✅ must match LoginActivity
    private static final String KEY_USERNAME = "username";
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public UserSessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveUsername(String username) {
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public String getUsername() {
        // Default “Unknown” only if truly not stored
        return pref.getString(KEY_USERNAME, "Unknown");
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
