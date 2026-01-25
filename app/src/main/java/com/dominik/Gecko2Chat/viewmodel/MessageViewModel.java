package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.dominik.Gecko2Chat.model.MessageModel;
import com.dominik.Gecko2Chat.repository.MessageRepository;
import com.dominik.Gecko2Chat.utils.ConversationUtils;
import com.dominik.Gecko2Chat.utils.UserManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MessageViewModel extends AndroidViewModel {

    private final MessageRepository repository;
    private final MutableLiveData<Integer> messageLimit = new MutableLiveData<>(20);
    private LiveData<List<MessageModel>> messageList = new MutableLiveData<>(new ArrayList<>());
    private boolean isLoading = false;
    //private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private String myId;
    private UserManager userManager;

    private String currentFriendId;

    private int currentPage = 0;
    private boolean isLastPage = false;


    public MessageViewModel(@NonNull Application application) {
        super(application);
        userManager = UserManager.getInstance(application.getApplicationContext());
        repository = MessageRepository.getInstance(application);

    }

    //Has to be called by the activity/fragment
    public void initChat(String friendId) {
        currentFriendId = friendId;
        myId = userManager.getUser().internalId();
        String conversationId = ConversationUtils.getConversationId(currentFriendId, myId);

        repository.setCurrentConversationId(conversationId);


        //whenever "messageLimit" changes, this function runs again, which retrieve the messages from db and maps them to MessageModel
        messageList = Transformations.switchMap(messageLimit, limit ->
                Transformations.map(repository.getMessagesForChat(conversationId, limit),
                        entities -> entities.stream()
                                .map(ConversationUtils::mapEntityToMessageModel)
                                .toList()
                )
        );
    }


    public void loadNextPage(String friendId) {
        if (isLoading) return;

        List<MessageModel> currentList = messageList.getValue();
        if (currentList == null || currentList.isEmpty()) return;

        isLoading = true;

        Instant oldestTimestamp = currentList.get(0).timestamp();
        repository.loadMoreHistory(friendId, oldestTimestamp);

        Integer currentLimit = messageLimit.getValue();
        if (currentLimit != null) {
            messageLimit.setValue(currentLimit + 20);
        }

        isLoading = false;
    }


    public LiveData<List<MessageModel>> getMessageList() {
        return messageList;
    }


    public void addNewMessage(String content) {
        repository.sendMessage(myId, currentFriendId, content);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.clearCurrentConversationId();
    }
}
