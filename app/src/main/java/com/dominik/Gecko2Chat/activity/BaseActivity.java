package com.dominik.Gecko2Chat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dominik.Gecko2Chat.utils.AuthStateManager;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.WebSocketManager;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.connectivity.ConnectionBuilder;

import java.net.HttpURLConnection;
import java.net.URL;

public class BaseActivity extends AppCompatActivity {

    private AuthorizationService authService;
    protected AuthStateManager authStateManager;
    protected UserManager userManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authStateManager = new AuthStateManager(getApplicationContext());
        userManager = new UserManager(getApplicationContext());
        authService = new AuthorizationService(this, createAuthConfig());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthAndConnect();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (authService != null) {
            authService.dispose();
        }
    }


    private void checkAuthAndConnect() {
        AuthState state = authStateManager.getAuthState();

        if(!state.isAuthorized()) {
            Log.e("BaseActivity", "No valid auth state found. Redirecting to login");
            performLogout();
            return;
        }

        // 2. "Smart" Check: Checks expiration -> Refreshes if needed -> Returns Token
        state.performActionWithFreshTokens(authService, (accessToken, idToken, ex) -> {
            if (ex != null) {
                // This block runs if the Refresh Token is expired or revoked.
                Log.e("BaseActivity", "Session expired (Refresh token failed). Logging out.", ex);
                performLogout();
                return;
            }

            authStateManager.updateAuthState(state);

            // 4. NOW it is safe to connect the WebSocket
            if (accessToken != null) {
                WebSocketManager.getInstance().connect(accessToken);
            }
        });
    }


    // Helper to allow HTTP connections
    private AppAuthConfiguration createAuthConfig() {
        ConnectionBuilder connectionBuilder = uri -> {
            URL url = new URL(uri.toString());
            return (HttpURLConnection) url.openConnection();
        };
        return new AppAuthConfiguration.Builder()
                .setConnectionBuilder(connectionBuilder)
                .build();
    }


    private void performLogout() {
        WebSocketManager.getInstance().disconnect();
        userManager.clearUser();
        authStateManager.clearAuthState();

        Intent intent = new Intent(this, LoginActivity.class);
        //Avoid user pressing back
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}
