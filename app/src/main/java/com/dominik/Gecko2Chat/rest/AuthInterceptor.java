package com.dominik.Gecko2Chat.rest;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.dominik.Gecko2Chat.utils.AuthStateManager;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.GrantTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.connectivity.ConnectionBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final AuthStateManager authStateManager;
    private final AuthorizationService authService;

    private static final String AUTH_ENDPOINT = "http://192.168.1.134:8080/realms/gecko2-realm/protocol/openid-connect/auth";
    private static final String TOKEN_ENDPOINT = "http://192.168.1.134:8080/realms/gecko2-realm/protocol/openid-connect/token";
    private static final String CLIENT_ID = "gecko2-android-client";

    public AuthInterceptor(Context context) {
        authStateManager = new AuthStateManager(context.getApplicationContext());
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

        authService = new AuthorizationService(context.getApplicationContext(), authConfig);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        AuthState state = authStateManager.getAuthState();

        //If no token exist
        if (state.getAccessToken() == null) {
            return chain.proceed(originalRequest);
        }

        // 2. CHECK IF EXPIRED (or close to expiring)
        if (state.getNeedsTokenRefresh()) {
            // We need to refresh the token SYNCHRONOUSLY before this request proceeds
            String freshToken = refreshAccessTokenSync(state);

            if (freshToken != null) {
                // Refresh success! Use new token
                return chain.proceed(addAuthorizationHeader(originalRequest, freshToken));
            } else {
                // Refresh failed. Proceed with old token (it will fail)
                return chain.proceed(originalRequest);
            }
        }

        // 3. Token is fine, proceed normally
        Request request = addAuthorizationHeader(originalRequest, state.getAccessToken());

        return chain.proceed(request);
    }

    /**
     * Helper to bridge Async AppAuth to Sync OkHttp
     */
    private String refreshAccessTokenSync(AuthState state) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> newToken = new AtomicReference<>(null);

        // 1. Check if we have a refresh token
        if (state.getRefreshToken() == null) {
            return null; // Cannot refresh
        }

        // 2. Prepare the Request
        AuthorizationServiceConfiguration config = state.getAuthorizationServiceConfiguration();
        if (config == null) {
            config = new AuthorizationServiceConfiguration(
                    Uri.parse(AUTH_ENDPOINT),
                    Uri.parse(TOKEN_ENDPOINT));
        }

        TokenRequest refreshRequest = new TokenRequest.Builder(config, CLIENT_ID)
                .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(state.getRefreshToken())
                .build();

        // 3. Execute Sync
        authService.performTokenRequest(refreshRequest, (response, ex) -> {
            if (response != null) {
                // Success: Update state & save it
                state.update(response, ex);
                authStateManager.updateAuthState(state);
                newToken.set(response.accessToken);
            }
            latch.countDown();
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return newToken.get();
    }

    private Request addAuthorizationHeader(Request originalRequest, String token) {
        return originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
    }
}
