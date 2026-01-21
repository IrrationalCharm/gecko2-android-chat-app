package com.dominik.Gecko2Chat.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.dominik.Gecko2Chat.database.AppDatabase;
import com.dominik.Gecko2Chat.database.MessageDao;
import com.dominik.Gecko2Chat.database.MessageEntity;
import com.dominik.Gecko2Chat.enums.PrivateMessageType;
import com.dominik.Gecko2Chat.enums.TextType;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.api.MessageApi;
import com.dominik.Gecko2Chat.model.response.MessageDto;
import com.dominik.Gecko2Chat.model.response.MessageHistoryDto;
import com.dominik.Gecko2Chat.model.response.websocket.ChatMessageDto;
import com.dominik.Gecko2Chat.model.response.websocket.MessageReceivedDto;
import com.dominik.Gecko2Chat.model.response.websocket.PrivateMessage;
import com.dominik.Gecko2Chat.model.response.websocket.adapter.PrivateMessageDeserializer;
import com.dominik.Gecko2Chat.rest.RestClient;
import com.dominik.Gecko2Chat.utils.ConversationUtils;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageRepository {

    private static MessageRepository instance;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(PrivateMessage.class, new PrivateMessageDeserializer())
            .create();
    private MessageApi messageApi;
    private MessageDao messageDao;
    private String currentConversationId = null;


    private MessageRepository(Context context) {
        messageDao = AppDatabase.getInstance(context).messageDao();
        messageApi = RestClient.getInstance(context).getMessagesApi();
        subscribeToWebsocket();
    }

    public static synchronized MessageRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MessageRepository(context);
        }
        return instance;
    }


    private void subscribeToWebsocket() {
        Disposable d = WebSocketManager.getInstance().getMessageStream()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleIncomingMessage, Throwable::printStackTrace);

        compositeDisposable.add(d);
    }


    private void handleIncomingMessage(String json) {
        try {
            PrivateMessage dto = gson.fromJson(json, PrivateMessage.class);

            switch(dto) {
                case MessageReceivedDto messageReceivedDto -> {
                    Log.i("MessageRepository", "Message acknowledged by server received: " + messageReceivedDto.uuid());

                }

                case ChatMessageDto messageDto -> {
                    Log.i("MessageRepository", "Message received: " + messageDto.content());
                    var mEntity = new MessageEntity();
                    mEntity.messageId = messageDto.clientMsgId();
                    mEntity.conversationId = ConversationUtils.getConversationId(messageDto.senderId(), messageDto.recipientId());
                    mEntity.senderId = messageDto.senderId();
                    mEntity.recipientId = messageDto.recipientId();
                    mEntity.content = messageDto.content();
                    //mEntity.timestamp = LocalDateTime.parse(messageDto.timestamp()).toInstant();
                    mEntity.textType = messageDto.textType().toString();

                    executor.execute(() -> messageDao.insertMessage(mEntity));

                    // Emit notification if its not the current conversation
                    if (currentConversationId != null && !currentConversationId.equals(mEntity.conversationId)) {
                        Log.i("MessageRepository", "Message from another conversation received");
                        //TODO emit notification
                        //NotificationUtils.showNewMessageNotification(context, mEntity);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Chat", "Error parsing message", e);
        }
    }

    public LiveData<List<MessageEntity>> getMessagesForChat(String conversationId) {
        return messageDao.getMessagesForChat(conversationId);
    }

    public void sendMessage(String myId, String currentFriendId, String content) {
        var dto = new ChatMessageDto(
                UUID.randomUUID().toString(),
                myId,
                currentFriendId,
                TextType.TEXT,
                PrivateMessageType.CHAT_MESSAGE,
                content,
                LocalDateTime.now().toString()
        );

        String json = gson.toJson(dto);
        Log.i("Chat", "Sending message: " + json);
        WebSocketManager.getInstance().sendMessage(json);

        executor.execute(() -> {
            MessageEntity messageEntity = ConversationUtils.mapChatMessageDtoToMessageEntity(dto);
            messageDao.insertMessage(messageEntity);
        });

    }

    public Completable loadMoreHistory(long oldestTimestamp) {
        //Asks first if local DB has messages before date
        return messageDao.hasMessagesBefore(currentConversationId, oldestTimestamp)
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(hasMessages -> {
                    if(hasMessages)
                        return Completable.complete();
                     else
                        return fetchAndInsertMessages(oldestTimestamp); //If not, fetches from api and inserts

                });
    }

    private Completable fetchAndInsertMessages(long beforeTime) {
        return messageApi.getConversationHistory(currentConversationId, beforeTime, 20)
                .flatMapCompletable(response -> {
                    if (response.body() != null && response.body().data() != null) {
                        List<MessageEntity> entities = response.body().data().messages().stream()
                                .map(ConversationUtils::mapDtoToEntity) // Your mapper
                                .collect(Collectors.toList());

                        if (entities.isEmpty()) {
                            return Completable.error(new Exception("End of history"));
                        }
                        // Insert and wait for DB to finish
                        return messageDao.insertAll(entities);
                    } else {
                        return Completable.error(new Exception("Empty response"));
                    }
                });
    }


    public void loadOlderMessages(String friendId, int page, int size) {
        messageApi.getConversation(friendId, page, size).enqueue(new Callback<ApiResponse<MessageHistoryDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageHistoryDto>> call, Response<ApiResponse<MessageHistoryDto>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    List<MessageDto> messagesDto = response.body().data().messages();
                    List<MessageEntity> messages = messagesDto.stream()
                                    .map(ConversationUtils::mapMessageDtoToMessageEntity)
                                    .collect(Collectors.toList());

                    messageDao.insertAll(messages);
                } else {
                    Log.e("MessageRepository", "Error fetching history: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageHistoryDto>> call, Throwable t) {
                Log.e("MessageRepository", "Network Failure: " + t.getMessage());
                callback.onError("Error: " + t.getMessage());
            }
        });
    }

    public void setCurrentConversationId(String currentConversationId) {
        this.currentConversationId = currentConversationId;
    }

    public void clearCurrentConversationId() {
        this.currentConversationId = null;
    }

}