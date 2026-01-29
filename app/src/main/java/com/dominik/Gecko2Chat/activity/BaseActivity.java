package com.dominik.Gecko2Chat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dominik.Gecko2Chat.database.AppDatabase;
import com.dominik.Gecko2Chat.model.api.KeycloakApi;
import com.dominik.Gecko2Chat.repository.MainRepository;
import com.dominik.Gecko2Chat.repository.MessageRepository;
import com.dominik.Gecko2Chat.utils.AuthStateManager;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.WebSocketManager;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.connectivity.ConnectionBuilder;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public abstract class BaseActivity extends AppCompatActivity {

    private AuthorizationService authService;
    protected AuthStateManager authStateManager;
    protected UserManager userManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authStateManager = new AuthStateManager(getApplicationContext());
        userManager = UserManager.getInstance(this);
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

        //We need fresh ID Tokens if we just created the account.
        if(getIntent().getBooleanExtra("justOnboarded", false)) {
            state.setNeedsTokenRefresh(true);
        }

        state.performActionWithFreshTokens(authService, (accessToken, idToken, ex) -> {
            if (ex != null) {
                // This block runs if the Refresh Token is expired or revoked.
                Log.e("BaseActivity", "Session expired (Refresh token failed). Logging out.", ex);
                performLogout();
                return;
            }
            userManager.saveUserFromIdToken(idToken);

            //Connect to websocket
            WebSocketManager.getInstance().connect(getApplicationContext());
        });
    }


    //Helper to allow non-secure HTTP connections
    private AppAuthConfiguration createAuthConfig() {
        ConnectionBuilder connectionBuilder = uri -> {
            URL url = new URL(uri.toString());
            return (HttpURLConnection) url.openConnection();
        };
        return new AppAuthConfiguration.Builder()
                .setConnectionBuilder(connectionBuilder)
                .build();
    }


    public void performLogout() {
        WebSocketManager.getInstance().disconnect();
        revokeToken();

        userManager.clearUser();
        authStateManager.clearAuthState();

        try( ExecutorService executor = Executors.newSingleThreadExecutor() ) {
            executor.execute(() -> AppDatabase.getInstance(this).clearAllTables());
        }

        Intent intent = new Intent(this, LoginActivity.class);
        //Avoid user pressing back
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void revokeToken() {
        var retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.134:8080") //Keycloak IP, to be changed later
                .build();
        KeycloakApi keycloakApi = retrofit.create(KeycloakApi.class);


        AuthState state = authStateManager.getAuthState();
        String refreshToken = state.getRefreshToken();

        if (refreshToken == null) return;

        keycloakApi.revokeToken("gecko2-android-client", refreshToken, "refresh_token").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("BaseActivity", "Successfully revoked token");
                    return;
                }

                Log.e("BaseActivity", "Failed to revoke token");
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("BaseActivity", "Failed to revoke token", t);
            }
        });
    }

}
