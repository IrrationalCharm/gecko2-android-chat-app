package com.dominik.Gecko2Chat.utils;


import android.content.Context;

import com.dominik.Gecko2Chat.model.websocket.incoming.ChatMessageEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.FriendRequestReceivedEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.MessageDeliveredEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.MessageReadEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.MessageSentEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.ServerMessage;
import com.dominik.Gecko2Chat.model.websocket.adapter.ServerMessageDeserializer;
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
                .registerTypeAdapter(ServerMessage.class, new ServerMessageDeserializer())
                .create();

        subscribeToWebsocket();
    }

    public static synchronized WebSocketEventRouter getInstance(Context context) {
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
        ServerMessage message = gson.fromJson(json, ServerMessage.class);

        switch (message) {
            case ChatMessageEvent event -> messageRepository.incomingMessage(event);
            case MessageSentEvent event -> messageRepository.incomingMessageSent(event);
            case MessageDeliveredEvent event -> messageRepository.incomingMessageDelivered(event);
            case MessageReadEvent messageReadEvent -> messageRepository.incomingMessageRead(messageReadEvent);
            case FriendRequestReceivedEvent event -> friendRepository.incomingFriendRequest(event);
        }
    }

}
