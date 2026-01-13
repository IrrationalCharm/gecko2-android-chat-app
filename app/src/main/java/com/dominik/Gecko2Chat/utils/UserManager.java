package com.dominik.Gecko2Chat.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.dominik.Gecko2Chat.model.User;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class UserManager {

    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_MOBILE_NUMBER = "mobile_number";
    private static final String KEY_PROFILE_BIO = "profile_bio";
    private static final String KEY_PROFILE_IMAGE_URL = "profile_image_url";
    private static final String KEY_ONBOARDED = "onboarded";
    private final SharedPreferences prefs;

    // Keys for saving data
    private static final String KEY_PROVIDER_ID = "sub";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_INTERNAL_ID = "internal_id";
    private static final String KEY_USERNAME = "username";

    public UserManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Extracts details from the ID Token and saves them to SharedPreferences
     */
    public void saveUserFromIdToken(String idToken) {
        try {
            // Decode the token
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) return;
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(payload);

            // Extract fields (adjust keys based on your Keycloak settings)
            String providerId = json.optString("sub", "");
            String email = json.optString("email", "");
            String internalId = json.optString("internal_id", "");
            String usernameApp = json.optString("username_app", "");

           boolean isOnboarded = !(internalId.isEmpty() || usernameApp.isEmpty());

            // Save to SharedPreferences
            prefs.edit()
                    .putString(KEY_PROVIDER_ID, providerId)
                    .putString(KEY_EMAIL, email)
                    .putString(KEY_INTERNAL_ID, internalId)
                    .putString(KEY_USERNAME, usernameApp)
                    .putBoolean(KEY_ONBOARDED, isOnboarded)
                    .apply();

        } catch (Exception e) {
            Log.e("UserManager", "Failed to parse ID Token", e);
        }
    }

    /**
     * Returns the currently logged in User object
     */
    public User getUser() {
        return new User(
                prefs.getString(KEY_INTERNAL_ID, ""),
                prefs.getString(KEY_PROVIDER_ID, ""),
                prefs.getString(KEY_USERNAME, ""),
                prefs.getString(KEY_EMAIL, ""),
                prefs.getString(KEY_MOBILE_NUMBER, ""),
                prefs.getString(KEY_PROFILE_BIO, ""),
                prefs.getString(KEY_PROFILE_IMAGE_URL, ""),
                prefs.getBoolean(KEY_ONBOARDED, false)
        );
    }

    public void clearUser() {
        prefs.edit().clear().apply();
    }
}
