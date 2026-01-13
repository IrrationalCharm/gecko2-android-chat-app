package com.dominik.Gecko2Chat.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

public class WebSocketManager {
    private static WebSocketManager instance;
    private StompClient stompClient;
    private final String WS_URL = "ws://10.0.2.2:8081/ws";
    private CompositeDisposable compositeDisposable;

    private final PublishSubject<String> messageSubject = PublishSubject.create();

    private WebSocketManager() {
        compositeDisposable = new CompositeDisposable();
    }

    public static synchronized WebSocketManager getInstance() {
        if (instance == null) instance = new WebSocketManager();
        return instance;
    }

    public void connect(String jwtToken) {
        if (stompClient != null && stompClient.isConnected()) return;

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL);

        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("Authorization", "Bearer " + jwtToken));

        stompClient.connect(headers);

        Disposable d = stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            Log.d("WS", "Stomp connection opened");
                            subscribeToPrivateMessages();
                            break;

                        case ERROR: Log.e("WS", "Error", lifecycleEvent.getException()); break;
                        case CLOSED: Log.d("WS", "Stomp connection closed"); break;
                    }
                });
        compositeDisposable.add(d);
    }

    public Observable<String> getMessageStream() {
        return messageSubject;
    }

    public void subscribeToPrivateMessages() {
        Disposable d = stompClient.topic("/user/private")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    Log.d("WS", "Message Arrived: " + topicMessage.getPayload());
                    messageSubject.onNext(topicMessage.getPayload()); // Emit the message to the "radio"
                }, throwable -> Log.e("WS", "Subscribe error", throwable));
        compositeDisposable.add(d);
    }

    public void sendMessage(String jsonPayload) {
        //Maps to @MessageMapping("/chat") in ChatController
        Disposable d = stompClient.send("/app/chat", jsonPayload)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> Log.d("WS", "Message Sent"), t -> Log.e("WS", "Send error", t));
        compositeDisposable.add(d);
    }

    public void disconnect() {
        if (stompClient != null) stompClient.disconnect();
        if (compositeDisposable != null) compositeDisposable.clear();
    }
}