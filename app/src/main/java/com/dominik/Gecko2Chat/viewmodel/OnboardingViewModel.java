package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dominik.Gecko2Chat.activity.add_friend_activity.UiState;
import com.dominik.Gecko2Chat.model.OnBoardingRequestDto;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.response.UserDto;
import com.dominik.Gecko2Chat.rest.RestClient;
import com.dominik.Gecko2Chat.utils.AuthStateManager;
import com.dominik.Gecko2Chat.utils.ImageUploadUtils;
import com.dominik.Gecko2Chat.utils.UserManager;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.connectivity.ConnectionBuilder;

import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnboardingViewModel extends AndroidViewModel {

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState.Idle());
    private final AuthStateManager authStateManager;

    public OnboardingViewModel(@NonNull Application application) {
        super(application);
        this.authStateManager = new AuthStateManager(application);
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void submitOnboardingData(String username, String displayName, String profileImageUri) {
        uiState.setValue(new UiState.Loading());

        OnBoardingRequestDto requestDto = new OnBoardingRequestDto(username, displayName, null, null, null);

        // 1. Call Registration API
        RestClient.getInstance(getApplication()).getRegistrationApi().registerUser(requestDto)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<UserDto>> call, @NonNull Response<ApiResponse<UserDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // Registration success -> Refresh Token to get new claims (internal_id)
                            refreshTokenAndProceed(profileImageUri);
                        } else {
                            uiState.setValue(new UiState.Error("Onboarding failed: " + response.message()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<UserDto>> call, @NonNull Throwable t) {
                        uiState.setValue(new UiState.Error("Network error: " + t.getMessage()));
                    }
                });
    }

    private void refreshTokenAndProceed(String imageUri) {
        // Setup Auth Service (HTTP fix included)
        ConnectionBuilder connectionBuilder = uri -> (HttpURLConnection) new URL(uri.toString()).openConnection();
        AppAuthConfiguration authConfig = new AppAuthConfiguration.Builder()
                .setConnectionBuilder(connectionBuilder)
                .build();
        AuthorizationService authService = new AuthorizationService(getApplication(), authConfig);

        AuthState state = authStateManager.getAuthState();
        TokenRequest refreshRequest = state.createTokenRefreshRequest();

        // 2. Perform Token Refresh
        authService.performTokenRequest(refreshRequest, (response, ex) -> {
            authService.dispose(); // Clean up immediately

            if (response != null) {
                state.update(response, ex);
                authStateManager.updateAuthState(state);

                //Update Local User Manager with new ID Token
                if (response.idToken != null) {
                    UserManager.getInstance(getApplication()).saveUserFromIdToken(response.idToken);
                }

                //Upload Image or Finish
                if (imageUri != null && !imageUri.isEmpty()) {
                    uploadProfilePicture(Uri.parse(imageUri));
                } else {
                    uiState.setValue(new UiState.Success());
                }
            } else {
                uiState.setValue(new UiState.Error("Failed to refresh session. Please login again."));
            }
        });
    }

    private void uploadProfilePicture(Uri fileUri) {
        MultipartBody.Part body = ImageUploadUtils.prepareImagePart(getApplication(), "image", fileUri);

        if (body == null) {
            // Failed to prepare image, but registration was success, so we finish anyway
            Log.e("OnboardingVM", "Failed to process image file");
            uiState.setValue(new UiState.Success());
            return;
        }

        RestClient.getInstance(getApplication()).getUserApi()
                .uploadAvatar(body)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                        // Regardless of image success/fail, we finish onboarding
                        uiState.setValue(new UiState.Success());
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                        Log.e("OnboardingVM", "Image upload failed", t);
                        uiState.setValue(new UiState.Success());
                    }
                });
    }
}