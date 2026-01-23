package com.dominik.Gecko2Chat.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;


import androidx.appcompat.app.AppCompatActivity;

import com.dominik.Gecko2Chat.activity.main_activity.MainActivity;
import com.dominik.Gecko2Chat.activity.onBoarding.OnboardingActivity;
import com.dominik.Gecko2Chat.utils.AuthStateManager;
import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.utils.UserManager;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.connectivity.ConnectionBuilder;

import java.net.HttpURLConnection;
import java.net.URL;


@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private AuthorizationService authService;
    private AuthStateManager authStateManager;
    private UserManager userManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runnable = this::checkAuthAndNavigate;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        //SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        //prefs.edit().clear().apply();

        init();

        handler.postDelayed(runnable, 1500);
    }

    private void init() {
        //Allows opening a connection to http:// addresses TODO: remove this when https added.
        ConnectionBuilder connectionBuilder = uri -> {
            URL url = new URL(uri.toString());
            // This allows opening a connection to http:// addresses
            return (HttpURLConnection) url.openConnection();
        };

        AppAuthConfiguration authConfig = new AppAuthConfiguration.Builder()
                .setConnectionBuilder(connectionBuilder) // Inject the HTTP-friendly builder
                .build();
        // end

        authService = new AuthorizationService(this, authConfig);
        authStateManager = new AuthStateManager(this);
        userManager = UserManager.getInstance(this);
    }

    private void checkAuthAndNavigate() {
        AuthState state = authStateManager.getAuthState();

        // 1. If not even logged in, go to Login
        if (!state.isAuthorized()) {
            navigateToLogin();
            return;
        }

        state.setNeedsTokenRefresh(true);

        // 2. Try to get a FRESH token (Auto-refreshes if needed)
        state.performActionWithFreshTokens(authService, (accessToken, idToken, ex) -> {
            if (ex != null) {
                // Refresh failed
                navigateToLogin();
                return;
            }
            // Success! Token is valid/refreshed.
            authStateManager.updateAuthState(state);
            userManager.saveUserFromIdToken(idToken);

            // 3. Now check Onboarding status
            if (userManager.getUser().isOnboarded()) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, OnboardingActivity.class));
            }
            finish();
        });
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacks(runnable);
    }
}
