package com.dominik.Gecko2Chat.activity.main_activity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;


import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.BaseActivity;
import com.dominik.Gecko2Chat.activity.main_activity.fragments.ChatsFragment;
import com.dominik.Gecko2Chat.activity.main_activity.fragments.ContactsFragment;
import com.dominik.Gecko2Chat.activity.main_activity.fragments.ProfileFragment;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.dominik.Gecko2Chat.viewmodel.MainViewModel;
import com.google.android.material.card.MaterialCardView;



public class MainActivity extends BaseActivity {

    private final Fragment chatsFragment = new ChatsFragment();
    private final Fragment contactFragment = new ContactsFragment();
    private final Fragment profileFragment = new ProfileFragment();
    private TextView navContacts, navChats, tvProfileText;
    private MaterialCardView cvProfileImage;
    private MainViewModel viewModel;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        initViews();
        initListeners();

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        if (viewModel.getChatList().getValue() == null) {
            viewModel.fetchStartupData();
            viewModel.reloadCurrentUser();
        }

        loadFragment(chatsFragment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.reloadCurrentUser();
    }

    private static void updateConnectionBanner(WebSocketManager.ConnectionStatus status) {
        if (status == WebSocketManager.ConnectionStatus.CONNECTING) {
            // Show "Connecting..." banner/spinner
        } else if (status == WebSocketManager.ConnectionStatus.CONNECTED) {
            // Hide banner
        } else if (status == WebSocketManager.ConnectionStatus.ERROR) {
            // Show "Connection Failed" message
        }
    }

    private void updateMenuUI(Tabs activeTab) {
        int colorGreen = ContextCompat.getColor(this, R.color.green_accent);
        int colorGrey = ContextCompat.getColor(this, R.color.text_secondary);
        int colorTransparent = Color.TRANSPARENT; // Or use ContextCompat.getColor(this, android.R.color.transparent);

        // RESET ALL (Grey text, removed glow background, removed stroke)
        resetTextView(navChats, colorGrey);
        resetTextView(navContacts, colorGrey);
        // Reset Profile specifically
        tvProfileText.setTextColor(colorGrey);

        // RESET STROKE TO TRANSPARENT
        cvProfileImage.setStrokeColor(colorTransparent);


        // 2. SET ACTIVE (Green text, glowing background, AND GREEN STROKE for profile)
        switch (activeTab) {
            case CHATS:
                activateTextView(navChats, colorGreen);
                break;
            case CONTACTS:
                activateTextView(navContacts, colorGreen);
                break;
            case PROFILE:
                // SET STROKE TO GREEN
                cvProfileImage.setStrokeColor(colorGreen);
                break;
        }
    }

    // Helper for standard TextView tabs (unchanged)
    private void resetTextView(TextView view, int color) {
        view.setTextColor(color);
        view.setBackground(null);
        if (view.getCompoundDrawablesRelative()[1] != null) {
            view.getCompoundDrawablesRelative()[1].setTint(color);
        }
    }

    // Helper for standard TextView tabs (unchanged)
    private void activateTextView(TextView view, int color) {
        view.setTextColor(color);
        //view.setBackgroundResource(R.drawable.bg_nav_active);
        if (view.getCompoundDrawablesRelative()[1] != null) {
            view.getCompoundDrawablesRelative()[1].setTint(color);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void initViews() {
        navContacts = findViewById(R.id.navContacts);
        navChats = findViewById(R.id.navChats);
        tvProfileText = findViewById(R.id.tvProfileText);
        cvProfileImage = findViewById(R.id.cvProfileImage);
    }


    private void initListeners() {

        navChats.setOnClickListener(v -> {
            loadFragment(chatsFragment);
            updateMenuUI(Tabs.CHATS);
        });
        navContacts.setOnClickListener(v -> {
            loadFragment(contactFragment);
            updateMenuUI(Tabs.CONTACTS);
        });

        cvProfileImage.setOnClickListener(v -> {
            loadFragment(profileFragment);
            updateMenuUI(Tabs.PROFILE);
        });
    }


    private enum Tabs {
        CHATS,
        CONTACTS,
        PROFILE
    }
}
