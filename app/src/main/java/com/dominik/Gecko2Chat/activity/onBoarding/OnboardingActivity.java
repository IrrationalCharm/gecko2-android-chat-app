package com.dominik.Gecko2Chat.activity.onBoarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.main_activity.MainActivity;
import com.dominik.Gecko2Chat.activity.onBoarding.fragments.DisplayNameFragment;
import com.dominik.Gecko2Chat.activity.onBoarding.fragments.OnBoardingAdapter;
import com.dominik.Gecko2Chat.activity.onBoarding.fragments.OnboardingStep;
import com.dominik.Gecko2Chat.activity.onBoarding.fragments.ProfilePictureFragment;
import com.dominik.Gecko2Chat.activity.onBoarding.fragments.UsernameFragment;
import com.dominik.Gecko2Chat.model.OnBoardingRequestDto;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.api.RegistrationApi;
import com.dominik.Gecko2Chat.model.response.UserDto;
import com.dominik.Gecko2Chat.rest.RestClient;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MaterialButton btnNext;
    private LinearLayout dotsContainer;
    private OnBoardingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onboarding_activity);

        initViews();
        setupViewPager();
        setupListeners();

        // Initialize the first dot as active
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
    }

    private void setupListeners() {
        // NEXT Button
        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            OnboardingStep fragment = adapter.getFragment(current);

            // Check if Fragment data is valid
            if (fragment != null && fragment.isDataValid()) {
                if (current < adapter.getItemCount() - 1) {
                    viewPager.setCurrentItem(current + 1);
                } else {
                    finishOnboarding();
                }
            }
        });

        // PAGE CHANGE LISTENER (Updates Dots & Buttons)
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                updateButtons(position);
            }
        });
    }

    // --- HELPER TO UPDATE THE MANUAL DOTS ---
    private void updateIndicators(int position) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);

            dot.setSelected(i == position);

            float scale = (i == position) ? 1.2f : 1.0f;
            dot.animate().scaleX(scale).scaleY(scale).setDuration(200).start();
        }
    }


    private void updateButtons(int position) {
        // Next Button Text
        if (position == adapter.getItemCount() - 1) {
            btnNext.setText("Finish");
            //btnNext.setIconResource(R.drawable.ic_check);
        } else {
            btnNext.setText("Next");
            //btnNext.setIconResource(R.drawable.ic_arrow_forward);
        }
    }

    private void finishOnboarding() {
        showLoading(true);
        String username = null;
        String displayName = null;
        String profileImageUri = null;


        //Loop through all fragments and check if data is valid
        for (int i = 0; i < adapter.getItemCount(); i++) {
            OnboardingStep fragment = adapter.getFragment(i);

            switch(fragment) { //Pattern matching Java 21
                case UsernameFragment usernameFragment:
                    if (!usernameFragment.isDataValid()) {
                        handleValidationError(i, "Username is not valid");
                        return;
                    }
                    username = usernameFragment.getData();
                    break;

                case DisplayNameFragment displayNameFragment:
                    if (!displayNameFragment.isDataValid()) {
                        handleValidationError(i, "Display name is not valid");
                        return;
                    }
                    displayName = displayNameFragment.getData();
                    break;

                case ProfilePictureFragment profilePictureFragment:
                    profileImageUri = profilePictureFragment.getData();
                    break;

                default:
                    throw new IllegalStateException("Unexpected value: " + fragment);
            }
        }

        // All data is valid at this point, sending request.
        RegistrationApi registrationApi = RestClient.getInstance(this).getRegistrationApi();

        var OnBoardingRequestDto = new OnBoardingRequestDto(username, displayName, null, null, null);
        String finalProfileImageUri = profileImageUri; // To use inside call

        registrationApi.registerUser(OnBoardingRequestDto).enqueue(new Callback<ApiResponse<UserDto>>() {
           @Override
           public void onResponse(Call<ApiResponse<UserDto>> call, Response<ApiResponse<UserDto>> response) {
               if (response.isSuccessful() && response.body() != null) {
                   if (finalProfileImageUri != null && finalProfileImageUri.isEmpty())
                       uploadProfilePicture(finalProfileImageUri);

                   Toast.makeText(OnboardingActivity.this, "Setup Complete!", Toast.LENGTH_SHORT).show();
                   goToMainActivity();
               } else {
                   Log.e("OnboardingActivity", "Onboarding failed, response body is null");
                   Toast.makeText(OnboardingActivity.this, "Onboarding failed, please try again", Toast.LENGTH_SHORT).show();
               }
               showLoading(false);
           }

           @Override
           public void onFailure(Call<ApiResponse<UserDto>> call, Throwable t) {
                showLoading(false);
                Log.e("OnboardingActivity", "Onboarding failed, network error", t);
                Toast.makeText(OnboardingActivity.this, "Onboarding failed, please try again", Toast.LENGTH_SHORT).show();
           }
        });



        // startActivity(new Intent(this, MainActivity.class));
        // finish();
    }

    private void goToMainActivity() {
        showLoading(false);
        startActivity(new Intent(this, MainActivity.class).putExtra("justOnboarded", true));
        finish();
    }

    private void uploadProfilePicture(String finalProfileImageUri) {
    }

    private void handleValidationError(int index, String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        showLoading(false);
        viewPager.setCurrentItem(index);
    }

    private void showLoading(boolean isLoading) {
        btnNext.setEnabled(!isLoading);
        btnNext.setText(isLoading ? "Loading..." : "Finish");
    }
}