package com.dominik.Gecko2Chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.dominik.Gecko2Chat.repository.MainRepository;
import com.dominik.Gecko2Chat.utils.AuthStateManager;
import com.dominik.Gecko2Chat.utils.WebSocketEventRouter;

public class GeckoApplication extends Application implements DefaultLifecycleObserver {

    @Override
    public void onCreate() {
        super.onCreate();
        //Listens to whole apps lifecycle
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        //This runs only when the app comes to the foreground

        AuthStateManager authStateManager = new AuthStateManager(this);

        if(authStateManager.getAuthState().isAuthorized()) {


            MainRepository.getInstance(this).refreshStartupData();

            WebSocketEventRouter.getInstance(this);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // App went to background
    }
}
