package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.add_friend_activity.UiState;
import com.dominik.Gecko2Chat.enums.ErrorCode;
import com.dominik.Gecko2Chat.repository.FriendRequestRepository;

public class AddFriendViewModel extends AndroidViewModel {

    private final static String USERNAME_REGEX = "^[a-zA-Z0-9_-]{3,20}$";
    private final FriendRequestRepository repository;
    private MutableLiveData<UiState> uiState = new MutableLiveData<>();


    public AddFriendViewModel(@NonNull Application application) {
        super(application);

        repository = FriendRequestRepository.getInstance(application);
    }

    public void onSendClick(String rawInput) {
        validateInput(rawInput);
        if (uiState.getValue() instanceof UiState.Error) return;

        uiState.setValue(new UiState.Loading());

        repository.sendFriendRequest(rawInput, new FriendRequestRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                uiState.setValue(new UiState.Success());
            }

            @Override
            public void onError(ErrorCode message) {

            }
        });

    }

    private void validateInput(String rawInput) {
        if (rawInput.isEmpty()) return;

        if (rawInput.length() < 3) {
            uiState.setValue(new UiState.Error("Too short (min 3 chars)"));
            return;
        }

        if (rawInput.length() > 20) {
            uiState.setValue(new UiState.Error("Too long (max 20 chars)"));
            return;
        }

        if (!rawInput.matches(USERNAME_REGEX)) {
            uiState.setValue(new UiState.Error("Not a valid username"));
        }
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }
}

