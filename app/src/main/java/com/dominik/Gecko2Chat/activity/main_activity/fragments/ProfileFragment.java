package com.dominik.Gecko2Chat.activity.main_activity.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.BaseActivity;
import com.google.android.material.card.MaterialCardView;


public class ProfileFragment extends Fragment {

    private MaterialCardView cvBtnLogout;
    private TextView tvMyDisplayName;
    private TextView tvMyUsername;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        cvBtnLogout = view.findViewById(R.id.cvLogout);
        tvMyDisplayName = view.findViewById(R.id.tvMyDisplayName);
        tvMyUsername = view.findViewById(R.id.tvMyUsername);

        cvBtnLogout.setOnClickListener(view1 -> {
            ((BaseActivity) requireActivity()).performLogout();
        });

        return view;
    }
}