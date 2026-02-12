package com.dominik.Gecko2Chat.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

public class AuthStateManager {

    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_STATE = "auth_state";
    private static final String AUTH_ENDPOINT = "http://192.168.1.134:8080/realms/gecko2-realm/protocol/openid-connect/auth";
    private static final String TOKEN_ENDPOINT = "http://192.168.1.134:8080/realms/gecko2-realm/protocol/openid-connect/token";
    private final SharedPreferences prefs;

    private final CryptoManager cryptoManager;

    public AuthStateManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            cryptoManager = new CryptoManager(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public AuthState getAuthState() {
        String encrypted = prefs.getString(KEY_STATE, null);
        AuthorizationServiceConfiguration config = createConfig();

        if (encrypted == null) {
            return new AuthState(config);
        }

        String json = null;
        try {
            json = cryptoManager.decrypt(encrypted);
        } catch (Exception e) {
            // If decryption fails, return a fresh empty state
            return new AuthState(config);
        }

        try {
            AuthState parsedState = AuthState.jsonDeserialize(json);
            
            // Always recreate the state with the fresh configuration to ensure endpoints are correct.
            // This fixes issues where the saved state might have missing or old configuration.
            AuthState fixedState = new AuthState(config);

            if (parsedState.getLastAuthorizationResponse() != null) {
                fixedState.update(parsedState.getLastAuthorizationResponse(), parsedState.getAuthorizationException());
            }
            if (parsedState.getLastTokenResponse() != null) {
                fixedState.update(parsedState.getLastTokenResponse(), parsedState.getAuthorizationException());
            }
            
            return fixedState;

        } catch (Exception e) {
            // If deserialization fails, return a fresh empty state
            return new AuthState(config);
        }
    }

    // Helper to create the configuration object
    private AuthorizationServiceConfiguration createConfig() {
        return new AuthorizationServiceConfiguration(
                Uri.parse(AUTH_ENDPOINT),
                Uri.parse(TOKEN_ENDPOINT)
        );
    }

    public void updateAfterAuthorization(AuthorizationResponse response, AuthorizationException ex) {
        AuthState currentState = getAuthState();
        // This updates the state with the authorization code and CONFIGURATION
        currentState.update(response, ex);
        updateAuthState(currentState);
    }

    public void updateAfterTokenResponse(TokenResponse response, AuthorizationException ex) {
        AuthState currentState = getAuthState();
        // This method updates the state with the new token or the error
        currentState.update(response, ex);
        updateAuthState(currentState);
    }


    public void updateAuthState(AuthState state) {
        try {
            String encrypted = cryptoManager.encrypt(state.jsonSerializeString());
            prefs.edit().putString(KEY_STATE, encrypted).apply();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void clearAuthState() {
        prefs.edit().remove(KEY_STATE).apply();
    }


    public String getAccessToken() {
        return getAuthState().getAccessToken();
    }

}
