package com.dominik.Gecko2Chat.activity.onBoarding.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dominik.Gecko2Chat.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class DisplayNameFragment extends Fragment implements OnboardingStep {

    private TextInputEditText etDisplayName;
    private TextInputLayout tilDisplayName;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_displayname, container, false);

        etDisplayName = view.findViewById(R.id.et_display_name);
        tilDisplayName = view.findViewById(R.id.til_display_name);


        etDisplayName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                if (!editable.toString().isEmpty())
                    isDataValid();
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                tilDisplayName.setError(null);

            }
        });


        return view;
    }

    @Override
    public boolean isDataValid() {
        String input = etDisplayName.getText().toString().trim();

        //Display name is allowed to be emptyD
        if (input.isEmpty()) {
            return true;
        }

        if (input.length() <3) {
            tilDisplayName.setError("Too short (min 3 chars)");
            return false;
        }


        if (input.length() > 20) {
            tilDisplayName.setError("Too long (max 20 chars)");
            return false;
        }


        if (!input.matches("^[a-zA-Z0-9À-ÿ _]{3,20}$")) {
            tilDisplayName.setError("Not a valid character combination");
            return false;
        }


        return true;
    }

    @Override
    public String getData() {
        return etDisplayName.getText().toString().trim();
    }
}
