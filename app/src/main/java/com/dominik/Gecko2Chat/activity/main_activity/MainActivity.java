package com.dominik.Gecko2Chat.activity.main_activity;


import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.BaseActivity;
import com.dominik.Gecko2Chat.activity.main_activity.fragments.ChatsFragment;
import com.dominik.Gecko2Chat.activity.main_activity.fragments.ContactsFragment;
import com.dominik.Gecko2Chat.activity.main_activity.fragments.ProfileFragment;
import com.dominik.Gecko2Chat.viewmodel.MainViewModel;



public class MainActivity extends BaseActivity {

    private Fragment chatsFragment;
    private Fragment contactFragment;
    private final Fragment profileFragment = new ProfileFragment();
    private TextView tvProfileText, tvChats, tvContacts;
    private FrameLayout navContacts, navChats, navProfile;
    private ImageView imgProfileAvatar, iconChats, iconContacts;

    private MainViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initViews();
        initListeners();

        chatsFragment = new ChatsFragment();
        contactFragment = new ContactsFragment();

        loadFragment(chatsFragment);

    }


    private void updateMenuUI(Tabs activeTab) {
        int colorGreen = ContextCompat.getColor(this, R.color.green_accent);
        int colorGrey = ContextCompat.getColor(this, R.color.text_secondary);

        //reset text
        resetTextView(tvProfileText, colorGrey);
        resetTextView(tvChats, colorGrey);
        resetTextView(tvContacts, colorGrey);

        //reset icons
        iconChats.setColorFilter(colorGrey);
        iconContacts.setColorFilter(colorGrey);

        iconChats.setAlpha(0.5f);
        iconContacts.setAlpha(0.5f);
        imgProfileAvatar.setAlpha(0.5f);

        switch (activeTab) {
            case CHATS:
                activateTextView(tvChats, colorGreen);
                iconChats.setAlpha(1f);
                iconChats.setColorFilter(colorGreen);
                break;

            case CONTACTS:
                activateTextView(tvContacts, colorGreen);
                iconContacts.setAlpha(1f);
                iconContacts.setColorFilter(colorGreen);
                break;

            case PROFILE:
                imgProfileAvatar.setAlpha(1f);
                activateTextView(tvProfileText, colorGreen);
                break;
        }
    }

    private void resetTextView(TextView view, int color) {
        view.setTextColor(color);
        view.setBackground(null);
        if (view.getCompoundDrawablesRelative()[1] != null) {
            view.getCompoundDrawablesRelative()[1].setTint(color);
        }
    }

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

        switch (fragment) {
            case ChatsFragment ignored -> updateMenuUI(Tabs.CHATS);
            case ContactsFragment ignored -> updateMenuUI(Tabs.CONTACTS);
            case ProfileFragment ignored -> updateMenuUI(Tabs.PROFILE);
            default -> {
            }
        }
    }

    private void initViews() {
        navContacts = findViewById(R.id.navContacts);
        navChats = findViewById(R.id.navChats);
        navProfile = findViewById(R.id.navProfile);

        tvProfileText = findViewById(R.id.tvProfileText);
        tvChats = findViewById(R.id.tvChats);
        tvContacts = findViewById(R.id.tvContacts);

        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        iconChats = findViewById(R.id.iconChats);
        iconContacts = findViewById(R.id.iconContacts);

        viewModel.getCurrentUser().observe(this, user -> {
            String avatarUrl = user.profileImageUrl();
            Glide.with(this)
                    .load(avatarUrl.contains("null") ? R.drawable.person_icon : avatarUrl)
                    .placeholder(R.drawable.person_icon)
                    .error(R.drawable.person_icon)
                    .circleCrop()
                    .into(imgProfileAvatar);
        });


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

        navProfile.setOnClickListener(v -> {
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
