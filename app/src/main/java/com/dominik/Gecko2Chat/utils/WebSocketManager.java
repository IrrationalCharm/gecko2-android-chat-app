package com.dominik.Gecko2Chat.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.connectivity.ConnectionBuilder;

import java.net.HttpURLConnection;
import java.net.URL;
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
import okhttp3.OkHttpClient;
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
    private final String WS_URL = "ws://10.0.2.2:8888/ws"; //"ws://10.0.2.2:8081/ws"

    // Disposables bound to the specific connection session (topics, pings)
    private final CompositeDisposable connectionDisposable = new CompositeDisposable();

    private Disposable lifecycleDisposable; // Specific disposable for the connection lifecycle
    private Disposable reconnectDisposable; // Specific disposable for the reconnection timer

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ServerMessage.class, new ServerMessageDeserializer())
            .create();

    private boolean isIntentionalDisconnect = false;

    // A "live pipe" that emits data only to listeners watching right now
    private final PublishSubject<String> messageSubject = PublishSubject.create();
    // A pipe that remembers the most recent value.
    private final BehaviorSubject<ConnectionStatus> statusSubject = BehaviorSubject.createDefault(ConnectionStatus.DISCONNECTED);

    private WebSocketManager() {
    }

    public static synchronized WebSocketManager getInstance() {
        if (instance == null) instance = new WebSocketManager();
        return instance;
    }


    public void connect(Context context) {
        if (stompClient != null && stompClient.isConnected())
            return;

        initAuth(context);

        statusSubject.onNext(ConnectionStatus.CONNECTING);
        isIntentionalDisconnect = false;

        AuthState state = authStateManager.getAuthState();
        state.performActionWithFreshTokens(authService, (accessToken, idToken, ex) -> {
            if (ex != null) {
                Log.e("WS", "Token refresh failed: " + ex.getMessage());
                statusSubject.onNext(ConnectionStatus.ERROR);

                isIntentionalDisconnect = true;
                disconnect();
                statusSubject.onNext(ConnectionStatus.AUTH_ERROR);
                return;
            }

            if (accessToken != null) {
                Log.d("WS", "Token refresh successful");
                authStateManager.updateAuthState(state);
                startStompConnection(accessToken, context);
            }
        });
    }


    private void initAuth(Context context) {
        //TODO remove this!
        ConnectionBuilder connectionBuilder = uri -> {
            URL url = new URL(uri.toString());
            return (HttpURLConnection) url.openConnection();
        };

        AppAuthConfiguration authConfig = new AppAuthConfiguration.Builder()
                .setConnectionBuilder(connectionBuilder)
                .build();

        if (authService == null) {
            authService = new AuthorizationService(context.getApplicationContext(), authConfig);
        }
        if (authStateManager == null) {
            authStateManager = new AuthStateManager(context.getApplicationContext());
        }
    }


    private void startStompConnection(String accessToken, Context context) {

        if(stompClient != null && stompClient.isConnected())
            return;

        resetConnection();

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
                            // Send manual PING after 500ms
                            Disposable pingDisposable = Observable.timer(500, TimeUnit.MILLISECONDS)
                                    .observeOn(Schedulers.io())
                                    .subscribe(aLong -> sendMessage(PING_SERVER, gson.toJson(PING_PAYLOAD)));
                            connectionDisposable.add(pingDisposable);
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



    /**
     * Cleans up previous connection artifacts to ensure a fresh start.
     */
    private void resetConnection() {
        if (stompClient != null) {
            Log.d("WS", "Resetting connection");
            stompClient.disconnect();
            stompClient = null;
        }
        connectionDisposable.clear(); // Clear old topic subscriptions and pings
        if (lifecycleDisposable != null) {
            lifecycleDisposable.dispose();
            lifecycleDisposable = null;
        }
        if(reconnectDisposable != null) {
            reconnectDisposable.dispose();
            reconnectDisposable = null;
        }
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

    public void disconnect() {
        isIntentionalDisconnect = true; //Force stops the reconnection

        resetConnection(); // Uses the shared reset logic

        if (reconnectDisposable != null) reconnectDisposable.dispose();
        if (authService != null) authService.dispose();
        authService = null;
    }



    public Observable<String> getMessageStream() {return messageSubject;}
    public Observable<ConnectionStatus> getConnectionStatus() {return statusSubject;}

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

        connectionDisposable.add(d);
    }

    private void sendMessage(String destination, String jsonPayload) {

        Disposable d = stompClient.send(destination, jsonPayload)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> Log.d("WS", "Message Sent"), t -> Log.e("WS", "Send error", t));
        connectionDisposable.add(d);
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




    public enum ConnectionStatus {
        CONNECTED, CONNECTING, DISCONNECTED, ERROR, AUTH_ERROR
    }
}