package com.dominik.Gecko2Chat.activity.add_friend_activity;

public sealed class UiState permits UiState.Idle, UiState.Loading, UiState.Success, UiState.Error {

    public static final class Idle extends UiState {
        public static final Idle INSTANCE = new Idle();
    }

    public static final class Loading extends UiState {
        public static final Loading INSTANCE = new Loading();
    }


    public static final class Success extends UiState {
        public static final Success INSTANCE = new Success();
    }

    public static final class Error extends UiState {
        public final String message;

        public Error(String message) {
            this.message = message;
        }
    }
}
