package com.dominik.Gecko2Chat.activity.main_activity.fragments;

import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.BaseActivity;
import com.dominik.Gecko2Chat.viewmodel.MainViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;


public class ProfileFragment extends Fragment {

    private ShapeableImageView iv_profile_avatar;
    private ImageView btn_upload;
    private MaterialCardView cvBtnLogout;
    private MainViewModel viewModel;
    private TextView tvMyDisplayName;
    private TextView tvMyUsername;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                // Image selected, trigger upload
                viewModel.uploadProfileImage(requireContext(), uri);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        cvBtnLogout = view.findViewById(R.id.cvLogout);
        tvMyDisplayName = view.findViewById(R.id.tvMyDisplayName);
        tvMyUsername = view.findViewById(R.id.tvMyUsername);
        btn_upload = view.findViewById(R.id.btn_upload);
        iv_profile_avatar = view.findViewById(R.id.iv_profile_avatar);

        cvBtnLogout.setOnClickListener(view1 -> {
            ((BaseActivity) requireActivity()).performLogout();
        });

        btn_upload.setOnClickListener(view1 -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            String username = "@" + user.username();

            // Update UI"
            tvMyDisplayName.setText(user.displayName());
            tvMyUsername.setText(username);

            if(user.profileImageUrl() != null && !user.profileImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(user.profileImageUrl().contains("null") ? R.drawable.person_icon : user.profileImageUrl())
                        .placeholder(R.drawable.person_icon)
                        .error(R.drawable.person_icon)
                        .circleCrop()
                        .into(iv_profile_avatar);
            }
        });


    }
}