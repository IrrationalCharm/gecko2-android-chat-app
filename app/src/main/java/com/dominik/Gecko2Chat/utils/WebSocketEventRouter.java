package com.dominik.Gecko2Chat.utils;


import android.content.Context;

import com.dominik.Gecko2Chat.model.response.websocket.ChatMessageDto;
import com.dominik.Gecko2Chat.model.response.websocket.FriendRequestReceivedDto;
import com.dominik.Gecko2Chat.model.response.websocket.MessageReceivedDto;
import com.dominik.Gecko2Chat.model.response.websocket.PrivateMessage;
import com.dominik.Gecko2Chat.model.response.websocket.adapter.PrivateMessageDeserializer;
import com.dominik.Gecko2Chat.repository.FriendRequestRepository;
import com.dominik.Gecko2Chat.repository.MessageRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * A generic class that subscribes to the manager, parses the JSON, and "routes" the object to the correct Repository.
 */
public class WebSocketEventRouter {

    private static WebSocketEventRouter instance;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final MessageRepository messageRepository;
    private final FriendRequestRepository friendRepository;

    private final Gson gson;

    private WebSocketEventRouter(Context context) {
        this.messageRepository = MessageRepository.getInstance(context);
        this.friendRepository = FriendRequestRepository.getInstance(context);

        this.gson = new GsonBuilder()
                .registerTypeAdapter(PrivateMessage.class, new PrivateMessageDeserializer())
                .create();

        subscribeToWebsocket();
    }

    public static WebSocketEventRouter getInstance(Context context) {
        if(instance == null) {
            instance = new WebSocketEventRouter(context);
            return instance;
        }
        return instance;
    }


    private void subscribeToWebsocket() {
        Disposable d = WebSocketManager.getInstance().getMessageStream()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::routeMessages, Throwable::printStackTrace);

        compositeDisposable.add(d);
    }

    private void routeMessages(String json) {
        PrivateMessage message = gson.fromJson(json, PrivateMessage.class);

        switch (message) {
            case MessageReceivedDto messageReceivedDto -> messageRepository.messageReceived(messageReceivedDto);
            case ChatMessageDto messageDto -> messageRepository.incomingMessage(messageDto);
            case FriendRequestReceivedDto friendRequestReceivedDto -> friendRepository.incomingFriendRequest(friendRequestReceivedDto);



            default -> throw new IllegalStateException("Unexpected value: " + message);
        }
    }

}
