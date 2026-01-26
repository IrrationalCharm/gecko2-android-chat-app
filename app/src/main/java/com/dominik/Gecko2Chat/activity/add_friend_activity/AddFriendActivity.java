package com.dominik.Gecko2Chat.activity.add_friend_activity;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.BaseActivity;
import com.dominik.Gecko2Chat.viewmodel.AddFriendViewModel;
import com.google.android.material.button.MaterialButton;

public class AddFriendActivity extends BaseActivity {


    private AddFriendViewModel viewModel;
    private ImageView btnBack;
    private EditText etUsername;
    private MaterialButton btnSendRequest;

    private boolean isSent = false;
    private ValueAnimator borderAnimator;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        viewModel = new ViewModelProvider(this).get(AddFriendViewModel.class);

        initViews();
        initListeners();


        setInputBorderColor(ContextCompat.getColor(this, R.color.text_secondary));
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etUsername = findViewById(R.id.etUsername);
        btnSendRequest = findViewById(R.id.btnSendRequest);
    }

    private void initListeners() {
        viewModel.getUiState().observe(this, uiState -> {
            switch (uiState) {
                case UiState.Idle ignored -> {}
                case UiState.Loading ignored -> {} //TODO add loading animation
                case UiState.Success ignored -> friendRequestSent();
                case UiState.Error e -> errorSendingRequest(e.message);

                default -> throw new IllegalStateException("Unexpected value: " + uiState);
            }
        });

        btnBack.setOnClickListener(v -> finish());

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // If user starts typing, stop any running success animation immediately
                if (borderAnimator != null && borderAnimator.isRunning()) {
                    borderAnimator.cancel();
                }

                if (isSent) {
                    isSent = false;
                    return;
                }
                viewModel.getUiState().setValue(new UiState.Idle());
                setInputBorderColor(ContextCompat.getColor(AddFriendActivity.this, R.color.text_secondary));
            }
        });

        btnSendRequest.setOnClickListener(v -> {
            String input = etUsername.getText().toString().trim();
            viewModel.onSendClick(input);
        });
    }

    private void errorSendingRequest(String message) {
        isSent = false;
        etUsername.setError(message);
        setInputBorderColor(ContextCompat.getColor(this, R.color.soft_red));
    }

    private void friendRequestSent() {
        isSent = true;
        etUsername.setText("");
        etUsername.setError(null);

        animateSuccessBorder();

        Toast.makeText(this, "Friend Request Sent", Toast.LENGTH_SHORT).show();
    }

    private void animateSuccessBorder() {
        int defaultColor = ContextCompat.getColor(this, R.color.text_secondary);
        int successColor = ContextCompat.getColor(this, R.color.green_accent);

        // Stroke widths in pixels
        int defaultStroke = 3;
        int targetStroke = 8; // Made bigger as requested

        // Cancel previous animation if exists
        if (borderAnimator != null) {
            borderAnimator.cancel();
        }

        borderAnimator = ValueAnimator.ofFloat(0f, 1f);
        borderAnimator.setDuration(3500); // 2 seconds total

        GradientDrawable background = (GradientDrawable) etUsername.getBackground().mutate();
        ArgbEvaluator colorEvaluator = new ArgbEvaluator();

        borderAnimator.addUpdateListener(animator -> {
            float fraction = animator.getAnimatedFraction();
            int color;
            int stroke;

            if (fraction < 0.1f) {
                // PHASE 1: Fade In (0% -> 20%)
                float localFraction = fraction / 0.2f; // map 0.0-0.2 to 0.0-1.0
                color = (int) colorEvaluator.evaluate(localFraction, defaultColor, successColor);
                stroke = (int) (defaultStroke + (targetStroke - defaultStroke) * localFraction);

            } else if (fraction < 0.9f) {
                // PHASE 2: Hold Green (20% -> 80%)
                color = successColor;
                stroke = targetStroke;

            } else {
                // PHASE 3: Fade Out (80% -> 100%)
                float localFraction = (fraction - 0.8f) / 0.2f; // map 0.8-1.0 to 0.0-1.0
                color = (int) colorEvaluator.evaluate(localFraction, successColor, defaultColor);
                stroke = (int) (targetStroke - (targetStroke - defaultStroke) * localFraction);
            }

            background.setStroke(stroke, color);
        });

        borderAnimator.start();
    }

    private void setInputBorderColor(int color) {
        GradientDrawable background = (GradientDrawable) etUsername.getBackground();
        background.mutate();
        background.setStroke(3, color);
    }
}