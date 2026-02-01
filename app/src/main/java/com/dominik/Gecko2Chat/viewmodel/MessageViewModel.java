package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.dominik.Gecko2Chat.model.MessageModel;
import com.dominik.Gecko2Chat.model.User;
import com.dominik.Gecko2Chat.repository.MessageRepository;
import com.dominik.Gecko2Chat.utils.ConversationUtils;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.WebSocketManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MessageViewModel extends AndroidViewModel {

    private static class ChatQuery {
        final String conversationId;
        final int limit;
        ChatQuery(String id, int l) { conversationId = id; limit = l; }
    }

    private final MessageRepository repository;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final MutableLiveData<WebSocketManager.ConnectionStatus> connectionStatus = new MutableLiveData<>();
    private final LiveData<List<MessageModel>> messageList;
    private final MutableLiveData<ChatQuery> chatQuery = new MutableLiveData<>();

    private final User currentUser;
    private String currentFriendId;


    public MessageViewModel(@NonNull Application application) {
        super(application);
        currentUser = UserManager.getInstance(application.getApplicationContext()).getUser();
        repository = MessageRepository.getInstance(application);


        messageList = Transformations.switchMap(chatQuery, query -> {
            if (query == null || query.conversationId == null) {
                return new MutableLiveData<>(new ArrayList<>());
            }

            // Transform the DB entity list to UI models
            return Transformations.map(
                    repository.getMessagesForChat(query.conversationId, query.limit),
                    entities -> entities.stream()
                            .map(ConversationUtils::mapEntityToMessageModel)
                            .toList()
            );
        });
    }


    //Has to be called by the activity/fragment
    public void initChat(String friendId) {
        if (currentFriendId != null && currentFriendId.equals(friendId)) { // Prevent re-initialization on rotation
            return;
        }

        currentFriendId = friendId;
        String conversationId = ConversationUtils.getConversationId(currentFriendId, currentUser.internalId());

        repository.setCurrentConversationId(conversationId);
        monitorConnectionStatus();

        chatQuery.setValue(new ChatQuery(conversationId, 20));

        repository.markConversationAsRead(currentUser.internalId(), friendId); //We just opened the chat, determine if it is marked as read and notify friend.
    }


    //Monitor connection status for chat
    private void monitorConnectionStatus() {
        Disposable d = WebSocketManager.getInstance().getConnectionStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        connectionStatus::setValue,
                        throwable -> Log.e("ChatViewModel", "Error observing connection status", throwable)
                );

        compositeDisposable.add(d);
    }


    public void loadNextPage(String friendId) {
        ChatQuery query = chatQuery.getValue();
        if (query == null) return;

        List<MessageModel> list = messageList.getValue();
        Instant oldest = (list != null && !list.isEmpty()) ? list.get(0).timestamp() : Instant.now();
        repository.loadMoreHistory(friendId, oldest);

        chatQuery.setValue(new ChatQuery(query.conversationId, query.limit + 20));

    }

    public LiveData<List<MessageModel>> getMessageList() {return messageList;}
    public LiveData<WebSocketManager.ConnectionStatus> getConnectionStatus() {return connectionStatus;}
    public void addNewMessage(String content) {
        repository.sendMessage(currentUser.internalId(), currentFriendId, content);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.clearCurrentConversationId();
        compositeDisposable.clear();
    }
}
