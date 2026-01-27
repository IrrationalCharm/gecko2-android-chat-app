package com.dominik.Gecko2Chat.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.main_activity.MainActivity;
import com.dominik.Gecko2Chat.activity.onBoarding.OnboardingActivity;
import com.dominik.Gecko2Chat.repository.MainRepository;
import com.dominik.Gecko2Chat.utils.AuthStateManager;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.google.android.material.button.MaterialButton;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.connectivity.ConnectionBuilder;

import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private static final String AUTH_ENDPOINT = "http://192.168.1.134:8080/realms/gecko2-realm/protocol/openid-connect/auth";
    private static final String TOKEN_ENDPOINT = "http://192.168.1.134:8080/realms/gecko2-realm/protocol/openid-connect/token";
    private static final String REDIRECT_URI = "com.dominik.gecko2chat://oauth2callback";
    private static final String CLIENT_ID = "gecko2-android-client";
    private static final String SCOPE = "email profile openid";
    private static final int RC_AUTH = 100;
    private AuthorizationService authService;
    private AuthStateManager authStateManager;


    private Button loginBtn;
    private MaterialButton googleLoginBtn;
    private LinearLayout buttonsContainer;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        init();
        initListeners();
    }

    //Handles the result of an authorization code request
    private final ActivityResultLauncher<Intent> authLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Handle the result of the authorization request
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // SHOW LOADING UI IMMEDIATELY
                    showLoading(true);

                    Intent data = result.getData();
                    AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
                    AuthorizationException ex = AuthorizationException.fromIntent(data);

                    authStateManager.updateAfterAuthorization(resp, ex);

                    if (resp != null) {
                        performTokenRequest(resp);
                    } else if (ex != null) {
                        Toast.makeText(this, "Login Failed: " + ex.error, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showLoading(false);
                }
            }
    );

    private void performTokenRequest(AuthorizationResponse resp) {
        authService.performTokenRequest(resp.createTokenExchangeRequest(),
                (response, ex) -> {
                    authStateManager.updateAfterTokenResponse(response, ex);

                    if (ex != null) {
                        // Network/Exchange failed, show buttons again
                        showLoading(false);
                        Toast.makeText(this, "Login Failed during exchange", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                    var userManager = UserManager.getInstance(this);
                    userManager.saveUserFromIdToken(authStateManager.getAuthState().getIdToken());

                    if (userManager.getUser().isOnboarded()) {
                        MainRepository.getInstance(this).refreshStartupData();

                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }

                    if (!userManager.getUser().isOnboarded()) {
                        startActivity(new Intent(this, OnboardingActivity.class));
                        finish();
                    }
                });
    }

    private void login(View view) {
        showLoading(true);
        AuthorizationServiceConfiguration serviceConfig = new AuthorizationServiceConfiguration(
                Uri.parse(AUTH_ENDPOINT),
                Uri.parse(TOKEN_ENDPOINT));

        AuthorizationRequest authRequest =
                new AuthorizationRequest.Builder(
                        serviceConfig, // the authorization service configuration
                        CLIENT_ID, // the client ID, typically pre-registered and static
                        ResponseTypeValues.CODE, // the response_type value: we want a code
                        Uri.parse(REDIRECT_URI))
                        .setScope(SCOPE)
                        .setPrompt("login") //Forces user to login even if browser has valid token.
                        .build();

        // --- NEW: Custom Tab Styling ---
        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();

        // Set the toolbar color to match your app's background/primary color
        int color = ContextCompat.getColor(this, R.color.soft_green_start); // Use your specific green hex here
        intentBuilder.setDefaultColorSchemeParams(new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(color)
                .build());

        // Hide the URL bar title to make it cleaner (URL still visible for security)
        intentBuilder.setShowTitle(false);

        // Optional: Add an exit animation to make it slide out smoothly
        intentBuilder.setExitAnimations(this, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

        CustomTabsIntent customTabsIntent = intentBuilder.build();
        // -------------------------------

        // Pass the customTabsIntent to the getAuthorizationRequestIntent method
        Intent authIntent = authService.getAuthorizationRequestIntent(authRequest, customTabsIntent);
        authLauncher.launch(authIntent);
    }

    private void googleLogin(View view) {
    }


    private void initListeners() {
        loginBtn.setOnClickListener(this::login);
        googleLoginBtn.setOnClickListener(this::googleLogin);
    }


    private void init() {
        loginBtn = findViewById(R.id.btn_sign_in);
        googleLoginBtn = findViewById(R.id.btn_google_sign_in);
        buttonsContainer = findViewById(R.id.login_buttons_container);
        progressBar = findViewById(R.id.login_progress);

        //Allows opening a connection to http:// addresses
        ConnectionBuilder connectionBuilder = uri -> {
            URL url = new URL(uri.toString());
            // This allows opening a connection to http:// addresses
            return (HttpURLConnection) url.openConnection();
        };

        AppAuthConfiguration authConfig = new AppAuthConfiguration.Builder()
                .setConnectionBuilder(connectionBuilder) // Inject the HTTP-friendly builder
                .build();
        // end

        authService = new AuthorizationService(this, authConfig);
        authStateManager = new AuthStateManager(this);
    }


    // 2. Add Helper Method
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            buttonsContainer.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            buttonsContainer.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }
}
