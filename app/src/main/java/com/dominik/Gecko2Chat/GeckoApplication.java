package com.dominik.Gecko2Chat;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.dominik.Gecko2Chat.model.User;
import com.dominik.Gecko2Chat.repository.MainRepository;
import com.dominik.Gecko2Chat.utils.AuthStateManager;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.WebSocketEventRouter;
import com.dominik.Gecko2Chat.utils.WebSocketManager;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.connectivity.ConnectionBuilder;

import java.net.HttpURLConnection;
import java.net.URL;

public class GeckoApplication extends Application implements DefaultLifecycleObserver {

    @Override
    public void onCreate() {
        super.onCreate();
        //Listens to whole apps lifecycle
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    //This runs only when the app comes to the foreground
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {

        AuthStateManager authStateManager = new AuthStateManager(this);
        AuthState state = authStateManager.getAuthState();

        if(state.isAuthorized()) {
            //TODO: remove this when https added.
            ConnectionBuilder connectionBuilder = uri -> {
                URL url = new URL(uri.toString());
                // This allows opening a connection to http:// addresses
                return (HttpURLConnection) url.openConnection();
            };

            AppAuthConfiguration authConfig = new AppAuthConfiguration.Builder()
                    .setConnectionBuilder(connectionBuilder) // Inject the HTTP-friendly builder
                    .build();
            // end

            AuthorizationService authService = new AuthorizationService(getApplicationContext(), authConfig);

            //state.setNeedsTokenRefresh(true);
            state.performActionWithFreshTokens(authService, (accessToken, idToken, ex) -> {
                if (ex != null) {
                    return;
                }

                if (idToken != null && !idToken.isEmpty()) {
                    UserManager.getInstance(getApplicationContext()).saveUserFromIdToken(idToken);
                    User user = UserManager.getInstance(getApplicationContext()).getUser();

                    if (user.isOnboarded()) {
                        WebSocketEventRouter.getInstance(this);
                        MainRepository.getInstance(this);
                        WebSocketManager.getInstance().connect(getApplicationContext());
                    }
                }


            });


        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // App went to background
    }
}
