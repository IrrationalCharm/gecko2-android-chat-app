package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.dominik.Gecko2Chat.activity.add_friend_activity.UiState;
import com.dominik.Gecko2Chat.enums.ErrorCode;
import com.dominik.Gecko2Chat.repository.FriendRequestRepository;

public class AddFriendViewModel extends AndroidViewModel {

    private final static String USERNAME_REGEX = "^[a-zA-Z0-9_-]{3,20}$";
    private final FriendRequestRepository repository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>();


    public AddFriendViewModel(@NonNull Application application) {
        super(application);

        repository = FriendRequestRepository.getInstance(application);
    }


    public void onSendClick(String rawInput) {
        validateInput(rawInput);
        if (uiState.getValue() instanceof UiState.Error) return;

        uiState.setValue(new UiState.Loading());

        repository.sendFriendRequest(rawInput, new FriendRequestRepository.RepositoryCallback<>() {
            @Override
            public void onSuccess() {
                uiState.setValue(new UiState.Success());
            }

            @Override
            public void onError(ErrorCode errorCode) {
                switch (errorCode) {
                    case FRIEND_REQUEST_BLOCKED_BY_USER -> uiState.setValue(new UiState.Error("Could not find this user"));
                    case FRIEND_REQUEST_EXISTS -> uiState.setValue(new UiState.Error("You already sent a friend request to this user"));
                    case USERNAME_NOT_FOUND -> uiState.setValue(new UiState.Error("Hm, that didn't work, Double-check that the username is correct."));
                    case FRIEND_REQUEST_SELF -> uiState.setValue(new UiState.Error("You can't send a friend request to yourself!!"));
                    case FRIEND_REQUEST_ALREADY_FRIENDS -> uiState.setValue(new UiState.Error("You are already friends with this user"));

                    default -> uiState.setValue(new UiState.Error("hm, something went wrong!"));
                }
            }
        });

    }

    private void validateInput(@NonNull String rawInput) {
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

    public MutableLiveData<UiState> getUiState() {
        return uiState;
    }
}

