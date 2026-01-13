package com.dominik.Gecko2Chat.activity.onBoarding.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.enums.ErrorCode;
import com.dominik.Gecko2Chat.enums.SuccessfulCode;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.api.RegistrationApi;
import com.dominik.Gecko2Chat.rest.RestClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.logging.ErrorManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsernameFragment extends Fragment implements OnboardingStep {
    private TextInputLayout tilUsername;
    private final static String USERNAME_REGEX = "^[a-zA-Z0-9_-]{3,20}$";
    private final Handler debouncingHandler = new Handler(Looper.getMainLooper());
    private Runnable checkUsernameRunnable;
    private TextInputEditText etUsername;
    private boolean isUsernameAvailable = false; // Flag to track API result

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_username, container, false);

        tilUsername = view.findViewById(R.id.til_username);
        etUsername = view.findViewById(R.id.et_username);

        initListeners();

        return view;
    }

    private void initListeners() {

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                isUsernameAvailable = false;
                tilUsername.setError(null);

                if (charSequence.length() == 0) { //On android isEmpty() is not implemented??
                    tilUsername.setHelperText("");
                } else
                    tilUsername.setHelperText("Checking...");

                tilUsername.setHelperTextColor(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.helper_text_color)));

                // Debouncing
                if (checkUsernameRunnable != null)
                    debouncingHandler.removeCallbacks(checkUsernameRunnable);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkUsernameRunnable = () -> isDataValid();

                if(!editable.toString().isEmpty())
                    debouncingHandler.postDelayed(checkUsernameRunnable, 500);
            }
        });
    }

    // --- INTERFACE METHODS ---

    @Override
    public boolean isDataValid() {
        String input = etUsername.getText().toString().trim();

        if (input.isEmpty()) {
            tilUsername.setError("Username cannot be empty");
            return false;
        }

        if (input.length() < 3) {
            tilUsername.setError("Too short (min 3 chars)");
            return false;
        }

        if (input.length() > 20) {
            tilUsername.setError("Too long (max 20 chars)");
            return false;
        }

        if (!input.matches(USERNAME_REGEX)) {
            tilUsername.setError("Not a valid username");
            return false;
        }


        RegistrationApi registrationApi = RestClient.getInstance(getContext()).getRegistrationApi();
        registrationApi.checkUsernameAvailability(input).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                ApiResponse<String> apiResponse = response.body();

                if (!response.isSuccessful()) {
                    handleResponseError(apiResponse);
                    return;
                }

                if (apiResponse != null) {
                    handleSuccessfulResponse(apiResponse);
                    return;
                }

                Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_LONG).show();
            }
        });

        // If we get here, everything is good!
        tilUsername.setError(null);
        return isUsernameAvailable;
    }

    private void handleSuccessfulResponse(ApiResponse<String> apiResponse) {
        var code = SuccessfulCode.valueOf(apiResponse.code());

        switch (code) {
            case USERNAME_AVAILABLE:
                isUsernameAvailable = true;
                tilUsername.setError(null);
                tilUsername.setHelperText("Username available!");
                break;
            case USERNAME_TAKEN:
                isUsernameAvailable = false;
                tilUsername.setError("Username taken!");
                break;
            default:
                Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_LONG).show();

        }
    }

    // Handle API errors
    private void handleResponseError(ApiResponse<String> apiResponse) {
        if (apiResponse == null) {
            Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_LONG).show();
            return;
        }

        var code = ErrorCode.valueOf(apiResponse.code());

        if (code == ErrorCode.VALIDATION_ERROR) {
            isUsernameAvailable = false;
            tilUsername.setError(apiResponse.detail());
            return;
        }

        Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_LONG).show();
    }

    @Override
    public String getData() {
        return etUsername.getText().toString().trim();
    }

    // Call this when your API returns success
    public void setUsernameAvailable(boolean available) {
        this.isUsernameAvailable = available;
    }
}
