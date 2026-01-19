package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dominik.Gecko2Chat.enums.PrivateMessageType;
import com.dominik.Gecko2Chat.enums.TextType;
import com.dominik.Gecko2Chat.model.MessageModel;
import com.dominik.Gecko2Chat.model.response.websocket.ChatMessageDto;
import com.dominik.Gecko2Chat.model.response.websocket.MessageReceivedDto;
import com.dominik.Gecko2Chat.model.response.websocket.PrivateMessage;
import com.dominik.Gecko2Chat.model.response.websocket.adapter.PrivateMessageDeserializer;
import com.dominik.Gecko2Chat.repository.MessageRepository;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MessageViewModel extends AndroidViewModel {

    private final MessageRepository repository;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final MutableLiveData<List<MessageModel>> messageList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private String myId;
    private UserManager userManager;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(PrivateMessage.class, new PrivateMessageDeserializer())
            .create();
    private String currentFriendId;

    private int currentPage = 0;
    private boolean isLastPage = false;


    public MessageViewModel(@NonNull Application application) {
        super(application);
        userManager = new UserManager(application.getApplicationContext());
        repository = MessageRepository.getInstance(application);
    }

    public void initChat(String friendId) {
        currentFriendId = friendId;
        myId = userManager.getUser().internalId();
        subscribeToWebsocket();
        loadNextPage(currentFriendId);

    }

    private void subscribeToWebsocket() {
        Disposable d = WebSocketManager.getInstance().getMessageStream()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleIncomingMessage, Throwable::printStackTrace);

        compositeDisposable.add(d);
    }

    private void handleIncomingMessage(String message) {
        try {
            PrivateMessage dto = gson.fromJson(message, PrivateMessage.class);

            switch(dto) {
                case MessageReceivedDto messageReceivedDto -> {
                    Log.i("Chat", "Message received: " + messageReceivedDto.uuid());

                }

                case ChatMessageDto messageDto -> {
                    // FILTER: Only show messages from the friend we are currently chatting with
                    if (messageDto.senderId().equals(currentFriendId)) {
                        var msg = new MessageModel(
                                messageDto.clientMsgId(),
                                messageDto.senderId(),
                                myId,
                                messageDto.content(),
                                LocalDateTime.parse(messageDto.timestamp()),
                                messageDto.textType()
                        );
                        addNewMessage(msg);

                    }
                }
            }


        } catch (Exception e) {
            Log.e("Chat", "Error parsing message", e);
        }
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

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();

    }

    public void sendMessage(String content) {
        var dto = new ChatMessageDto(
                UUID.randomUUID().toString(),
                myId, // You need to ensure the ViewModel has access to myId and friendId
                currentFriendId,
                TextType.TEXT,
                PrivateMessageType.CHAT_MESSAGE,
                content,
                LocalDateTime.now().toString()
        );

        String json = gson.toJson(dto);
        Log.i("Chat", "Sending message: " + json);
        WebSocketManager.getInstance().sendMessage(json);

        MessageModel uiMessage = new MessageModel(
                dto.clientMsgId(),
                dto.senderId(),
                dto.recipientId(),
                dto.content(),
                LocalDateTime.parse(dto.timestamp()),
                dto.textType()
        );

        addNewMessage(uiMessage);
    }
}
