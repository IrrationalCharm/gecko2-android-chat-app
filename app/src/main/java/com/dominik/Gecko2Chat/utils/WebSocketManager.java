package com.dominik.Gecko2Chat.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

/**
 * Keeps the connection alive and provides a raw stream of data.
 */
public class WebSocketManager {
    private static WebSocketManager instance;
    private StompClient stompClient;
    private final String WS_URL = "ws://10.0.2.2:8081/ws";
    private CompositeDisposable compositeDisposable;

    //A "live pipe" that emits data only to listeners watching right now
    private final PublishSubject<String> messageSubject = PublishSubject.create();
    //A pipe that remembers the most recent value.
    private final BehaviorSubject<ConnectionStatus> statusSubject = BehaviorSubject.createDefault(ConnectionStatus.DISCONNECTED);

    private WebSocketManager() {
        compositeDisposable = new CompositeDisposable();
    }

    public static synchronized WebSocketManager getInstance() {
        if (instance == null) instance = new WebSocketManager();
        return instance;
    }

    public void connect(String jwtToken) {
        if (stompClient != null && stompClient.isConnected()) return;

        statusSubject.onNext(ConnectionStatus.CONNECTING);

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
                            statusSubject.onNext(ConnectionStatus.CONNECTED);
                            subscribeToPrivateMessages();
                            break;

                        case ERROR:
                            Log.e("WS", "Error", lifecycleEvent.getException());
                            statusSubject.onNext(ConnectionStatus.ERROR);
                            scheduleReconnect(jwtToken);
                            break;
                        case CLOSED:
                            Log.d("WS", "Stomp connection closed");
                            statusSubject.onNext(ConnectionStatus.DISCONNECTED);
                            break;
                    }
                });
        compositeDisposable.add(d);
    }

    private void scheduleReconnect(String jwtToken) {
        Disposable d = Observable.timer(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    Log.d("WS", "Attempting Reconnect...");
                    connect(jwtToken);
                });

        compositeDisposable.add(d);
    }

    public Observable<String> getMessageStream() {
        return messageSubject;
    }

    public Observable<ConnectionStatus> getConnectionStatus() {
        return statusSubject;
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

    public enum ConnectionStatus {
        CONNECTED, CONNECTING, DISCONNECTED, ERROR
    }
}