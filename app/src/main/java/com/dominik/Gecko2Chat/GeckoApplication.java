package com.dominik.Gecko2Chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.dominik.Gecko2Chat.repository.MainRepository;
import com.dominik.Gecko2Chat.repository.MessageRepository;
import com.dominik.Gecko2Chat.utils.AuthStateManager;

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
            MessageRepository.getInstance(this).performDeltaSync();
            MainRepository.getInstance(this).refreshStartupData();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // App went to background
    }
}
