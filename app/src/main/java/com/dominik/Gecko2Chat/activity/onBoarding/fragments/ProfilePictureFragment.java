package com.dominik.Gecko2Chat.activity.onBoarding.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dominik.Gecko2Chat.R;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfilePictureFragment extends Fragment implements OnboardingStep {

    private ShapeableImageView ivProfilePicture;
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
        registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                ivProfilePicture.setImageURI(uri);
            }
        });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ivProfilePicture = view.findViewById(R.id.iv_profile_avatar);
        View btnUpload = view.findViewById(R.id.btn_upload);

        btnUpload.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_profile, container, false);

    }

    @Override
    public boolean isDataValid() {
        return true;
    }

    @Override
    public String getData() {
        if (selectedImageUri == null) return "";

        return selectedImageUri.toString();
    }
}