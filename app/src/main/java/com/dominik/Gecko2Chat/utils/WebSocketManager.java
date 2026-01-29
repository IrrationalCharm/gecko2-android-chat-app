package com.dominik.Gecko2Chat.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dominik.Gecko2Chat.activity.LoginActivity;
import com.dominik.Gecko2Chat.model.websocket.incoming.ServerMessage;
import com.dominik.Gecko2Chat.model.websocket.adapter.ServerMessageDeserializer;
import com.dominik.Gecko2Chat.model.websocket.outgoing.ClientMessage;
import com.dominik.Gecko2Chat.model.websocket.outgoing.SendDeliveredReceiptRequest;
import com.dominik.Gecko2Chat.model.websocket.outgoing.SendMessageRequest;
import com.dominik.Gecko2Chat.model.websocket.outgoing.SendReadReceiptRequest;
import com.dominik.Gecko2Chat.model.websocket.outgoing.SendTypingStatusRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;

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
    private static final String PING_PAYLOAD = "PING";

    private static final String PING_SERVER = "/app/ping";
    private static final String CHAT_MESSAGE_CLIENT = "/app/chat";
    private static final String READ_RECEIPT_CLIENT = "/app/read-receipt";
    private static final String DELIVERY_RECEIPT_CLIENT = "/app/delivered-receipt";
    private static final String TYPING_STATUS_CLIENT = "/app/typing";

    private AuthorizationService authService;
    private AuthStateManager authStateManager;

    private static WebSocketManager instance;
    private StompClient stompClient;
    private final String WS_URL = "ws://10.0.2.2:8081/ws";
    private CompositeDisposable compositeDisposable;
    private Disposable lifecycleDisposable; //Specific disposable for the connection lifecycle to prevent duplicates
    private Disposable reconnectDisposable; //Specific disposable for the reconnection timer

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ServerMessage.class, new ServerMessageDeserializer())
            .create();

    private boolean isIntentionalDisconnect = false;

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


    private void initAuth(Context context) {
        if (authService == null) {
            authService = new AuthorizationService(context.getApplicationContext());
        }
        if (authStateManager == null) {
            authStateManager = new AuthStateManager(context.getApplicationContext());
        }
    }


    public void connect(Context context) {
        initAuth(context);

        if (stompClient != null && stompClient.isConnected()) {
            return;
        }

        statusSubject.onNext(ConnectionStatus.CONNECTING);
        isIntentionalDisconnect = false;

        AuthState state = authStateManager.getAuthState();
        state.performActionWithFreshTokens(authService, (accessToken, idToken, ex) -> {
            if (ex != null) {
                Log.e("WS", "Token refresh failed: " + ex.getMessage());
                statusSubject.onNext(ConnectionStatus.ERROR);

                Intent intent = new Intent(context, LoginActivity.class);
                context.startActivity(intent);
                return;
            }

            if (accessToken != null) {
                Log.d("WS", "Token refresh successful");
                authStateManager.updateAuthState(state);
                startStompConnection(accessToken, context);
            }
        });
    }


    private void startStompConnection(String accessToken, Context context) {
        if(lifecycleDisposable != null) lifecycleDisposable.dispose();
        if(reconnectDisposable != null) reconnectDisposable.dispose();

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL);

        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("Authorization", "Bearer " + accessToken));

        stompClient.connect(headers);

        lifecycleDisposable = stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            Log.d("WS", "Stomp connection opened");
                            subscribeToPrivateMessages();
                            Disposable pingDisposable = Observable.timer(500, TimeUnit.MILLISECONDS)
                                    .observeOn(Schedulers.io())
                                    .subscribe(aLong -> sendMessage(PING_SERVER, gson.toJson(PING_PAYLOAD)));
                            compositeDisposable.add(pingDisposable);
                            break;

                        case ERROR:
                            Log.e("WS", "Error", lifecycleEvent.getException());
                            statusSubject.onNext(ConnectionStatus.ERROR);
                            if(!isIntentionalDisconnect) {
                                scheduleReconnect(context);
                            }
                            break;
                        case CLOSED:
                            Log.d("WS", "Stomp connection closed");
                            statusSubject.onNext(ConnectionStatus.DISCONNECTED);
                            if(!isIntentionalDisconnect) {
                                scheduleReconnect(context);
                            }
                            break;
                    }
                });

    }


    private void scheduleReconnect(Context context) {
        if(reconnectDisposable != null && !reconnectDisposable.isDisposed()) { //Avoid duplicate reconnect attempts
            return;
        }

        reconnectDisposable = Observable.timer(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    Log.d("WS", "Attempting Reconnect...");
                    connect(context);
                });
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
                    String payload = topicMessage.getPayload();
                    Log.d("WS", "Message Arrived: " + payload);

                    if (payload.contains("PONG") || payload.contains("CONNECTION_ESTABLISHED")) {
                        Log.d("WS", "Handshake successful - Status CONNECTED");
                        statusSubject.onNext(ConnectionStatus.CONNECTED);
                        return; // Consume this message, don't pass it to the EventRouter
                    }

                    messageSubject.onNext(topicMessage.getPayload()); // Emit the message to the "radio"
                }, throwable -> Log.e("WS", "Subscribe error", throwable));
        compositeDisposable.add(d);
    }


    private void sendMessage(String destination, String jsonPayload) {
        Disposable d = stompClient.send(destination, jsonPayload)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> Log.d("WS", "Message Sent"), t -> Log.e("WS", "Send error", t));
        compositeDisposable.add(d);
    }


    public void send(ClientMessage message) {
        String json = gson.toJson(message);

        String destination = switch (message) {
            case SendDeliveredReceiptRequest ignored -> DELIVERY_RECEIPT_CLIENT;
            case SendMessageRequest ignored -> CHAT_MESSAGE_CLIENT;
            case SendReadReceiptRequest ignored -> READ_RECEIPT_CLIENT;
            case SendTypingStatusRequest ignored -> TYPING_STATUS_CLIENT;
        };

        sendMessage(destination, json);
    }


    public void disconnect() {
        isIntentionalDisconnect = true; //Force stops the reconnection

        if (stompClient != null) stompClient.disconnect();
        if (lifecycleDisposable != null) lifecycleDisposable.dispose();
        if (reconnectDisposable != null) reconnectDisposable.dispose();
        if (authService != null) authService.dispose();
        if (compositeDisposable != null) compositeDisposable.clear();
    }


    public enum ConnectionStatus {
        CONNECTED, CONNECTING, DISCONNECTED, ERROR
    }
}