package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.dominik.Gecko2Chat.model.MessageModel;
import com.dominik.Gecko2Chat.repository.MessageRepository;
import com.dominik.Gecko2Chat.utils.ConversationUtils;
import com.dominik.Gecko2Chat.utils.UserManager;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class MessageViewModel extends AndroidViewModel {

    private final MessageRepository repository;

    private final CompositeDisposable disposable = new CompositeDisposable();

    private final MutableLiveData<Integer> messageLimit = new MutableLiveData<>(20);
    private LiveData<List<MessageModel>> messageList = new MutableLiveData<>(new ArrayList<>());
    //private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private String myId;
    private UserManager userManager;

    private String currentFriendId;

    private int currentPage = 0;
    private boolean isLastPage = false;


    public MessageViewModel(@NonNull Application application) {
        super(application);
        userManager = new UserManager(application.getApplicationContext());
        repository = MessageRepository.getInstance(application);


    }

    //Has to be called by the activity/fragment
    public void initChat(String friendId) {
        currentFriendId = friendId;
        myId = userManager.getUser().internalId();
        String conversationId = ConversationUtils.getConversationId(currentFriendId, myId);

        messageList = Transformations.map(repository.getMessagesForChat(conversationId),
                entities ->
                    entities.stream().map(ConversationUtils::mapEntityToMessageModel).toList()
        );


    }


    public void loadNextPage(String friendId) {
        List<MessageModel> currentList = messageList.getValue();

        if (currentList == null || currentList.isEmpty()) return;

        String oldestTimestamp = currentList.get(0).timestamp().toString();

        disposable.add(
                repository.loadMoreHistory(friendId, oldestTimestamp)
                        .observeOn(AndroidSchedulers.mainThread()))
                        .subscribe( () -> {
                            int newLimit = messageLimit.getValue() + 20;
                            messageLimit.setValue(newLimit);
                            //isLoading = false;
                        },
                        throwable -> {
                            Log.e("MessageViewModel", "Error loading more history", throwable);
                            //isLoading = false;
                        }

        );
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


    }
}
