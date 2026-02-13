package com.dominik.Gecko2Chat.activity.onBoarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.add_friend_activity.UiState;
import com.dominik.Gecko2Chat.activity.main_activity.MainActivity;
import com.dominik.Gecko2Chat.activity.onBoarding.fragments.DisplayNameFragment;
import com.dominik.Gecko2Chat.activity.onBoarding.fragments.OnBoardingAdapter;
import com.dominik.Gecko2Chat.activity.onBoarding.fragments.OnboardingStep;
import com.dominik.Gecko2Chat.activity.onBoarding.fragments.ProfilePictureFragment;
import com.dominik.Gecko2Chat.activity.onBoarding.fragments.UsernameFragment;
import com.dominik.Gecko2Chat.viewmodel.OnboardingViewModel;
import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MaterialButton btnNext;
    private LinearLayout dotsContainer;
    private OnBoardingAdapter adapter;
    private OnboardingViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onboarding_activity);

        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

        initViews();
        setupViewPager();
        setupListeners();
        observeViewModel();

        updateIndicators(0);
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btn_next);
        dotsContainer = findViewById(R.id.dotsContainer);
    }

    private void setupViewPager() {
        adapter = new OnBoardingAdapter(this);
        viewPager.setAdapter(adapter);

        viewPager.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1f - absPos);
            page.setScaleY(0.85f + (0.15f * (1f - absPos)));
            page.setScaleX(0.85f + (0.15f * (1f - absPos)));
        });
    }

    private void setupListeners() {
        btnNext.setOnClickListener(v -> handleNextClick());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicators(position);
                btnNext.setText(position == adapter.getItemCount() - 1 ? "Finish" : "Next");
            }
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, state -> {
            if (state instanceof UiState.Loading) {
                setLoading(true);
            } else if (state instanceof UiState.Success) {
                setLoading(false);
                Toast.makeText(this, "Setup Complete!", Toast.LENGTH_SHORT).show();
                goToMainActivity();
            } else if (state instanceof UiState.Error) {
                setLoading(false);
                Toast.makeText(this, ((UiState.Error) state).message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleNextClick() {
        int current = viewPager.getCurrentItem();
        OnboardingStep currentFragment = adapter.getFragment(current);

        // 1. Validate current step
        if (currentFragment == null || !currentFragment.isDataValid()) {
            return; // Fragment handles showing its own error
        }

        // 2. Move next or Finish
        if (current < adapter.getItemCount() - 1) {
            viewPager.setCurrentItem(current + 1);
        } else {
            collectDataAndSubmit();
        }
    }

    private void collectDataAndSubmit() {
        String username = null;
        String displayName = null;
        String profileImageUri = null;

        // Extract data from all fragments
        for (int i = 0; i < adapter.getItemCount(); i++) {
            OnboardingStep fragment = adapter.getFragment(i);

            switch (fragment) {
                case UsernameFragment f -> username = f.getData();
                case DisplayNameFragment f -> displayName = f.getData();
                case ProfilePictureFragment f -> profileImageUri = f.getData();
                case null, default -> {}
            }
        }

        viewModel.submitOnboardingData(username, displayName, profileImageUri);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("justOnboarded", true);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        btnNext.setEnabled(!isLoading);
        btnNext.setText(isLoading ? "Loading..." : "Finish");
        viewPager.setUserInputEnabled(!isLoading); // Disable swipe while loading
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            dot.setSelected(i == position);
            float scale = (i == position) ? 1.2f : 1.0f;
            dot.animate().scaleX(scale).scaleY(scale).setDuration(200).start();
        }
    }
}