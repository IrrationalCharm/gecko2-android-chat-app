package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dominik.Gecko2Chat.model.MessageModel;
import com.dominik.Gecko2Chat.repository.MessageRepository;

import java.util.ArrayList;
import java.util.List;

public class MessageViewModel extends AndroidViewModel {

    private final MessageRepository repository;

    private final MutableLiveData<List<MessageModel>> messageList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private int currentPage = 0;
    private boolean isLastPage = false;


    public MessageViewModel(@NonNull Application application) {
        super(application);
        repository = MessageRepository.getInstance(application);
    }


    public void loadNextPage(String friendId) {
        if (isLastPage) return;

        repository.getConversationHistory(friendId, currentPage, 20, new MessageRepository.MessageHistoryCallback() {

            @Override
            public void onSuccess(List<MessageModel> messages) {
                if (messages.isEmpty()) {
                    isLastPage = true;
                    return;
                }
                List<MessageModel> currentList = new ArrayList<>(messageList.getValue());
                currentList.addAll(0, messages);
                messageList.setValue(currentList);
                currentPage++;
                isLoading.setValue(false);

            }

            @Override
            public void onError(String errorMessage) {
                isLoading.setValue(false);
                //TODO implement error handling
            }
        });
    }


    public LiveData<List<MessageModel>> getMessageList() {
        return messageList;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }


    //Called when websocket receives a new message
    public void addNewMessage(MessageModel msg) {
        List<MessageModel> currentList = new ArrayList<>(messageList.getValue());

        boolean exists = currentList.stream().anyMatch(m -> m.id().equals(msg.id()));

        if (!exists) {
            currentList.add(msg);
            messageList.setValue(currentList);
        }

    }
}
