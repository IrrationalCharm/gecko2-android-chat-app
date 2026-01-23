package com.dominik.Gecko2Chat.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.dominik.Gecko2Chat.database.AppDatabase;
import com.dominik.Gecko2Chat.database.DateConverter;
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

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
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
                case MessageReceivedDto messageReceivedDto -> Log.i("MessageRepository", "Message acknowledged by server received: " + messageReceivedDto.uuid());

                case ChatMessageDto messageDto -> {
                    Log.i("MessageRepository", "Message received: " + messageDto.content());
                    var mEntity = new MessageEntity();
                    mEntity.messageId = messageDto.clientMsgId();
                    mEntity.conversationId = ConversationUtils.getConversationId(messageDto.senderId(), messageDto.recipientId());
                    mEntity.senderId = messageDto.senderId();
                    mEntity.recipientId = messageDto.recipientId();
                    mEntity.content = messageDto.content();
                    mEntity.timestamp = Instant.parse(messageDto.timestamp());
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


    public void performDeltaSync() {

        executor.execute(() -> { //Room cannot be executed in main thread
            Instant latestTimestamp = messageDao.getLatestTimestamp();
            long timestampEpoch = latestTimestamp == null ? 0 : latestTimestamp.toEpochMilli();
            Log.i("MessageRepository", "Performing delta sync with timestamp: " + timestampEpoch);

            try {
                //Cannot use .enqueue() because the callback is executed in the main thread, and we need to execute it in a background thread to store the result in Room
                Response<ApiResponse<List<MessageHistoryDto>>> response = messageApi.getSyncConversation(timestampEpoch).execute();

                if(!response.isSuccessful() || response.body() == null) {
                    Log.e("MessageRepository", "Error performing delta sync: " + response.code());
                    return;
                }

                List<MessageHistoryDto> messages = response.body().data();

                for (MessageHistoryDto dto : messages) {

                    List<MessageEntity> messageEntities = dto.messages().stream()
                            .map(ConversationUtils::mapMessageDtoToMessageEntity)
                            .collect(Collectors.toList());
                    messageDao.insertAll(messageEntities);
                }

                Log.i("MessageRepository", "Delta sync successful");

            } catch (IOException e) {
                Log.e("MessageRepository", "Error performing delta sync", e);
                throw new RuntimeException(e);
            }

        });

    }


    public void sendMessage(String myId, String currentFriendId, String content) {
        var dto = new ChatMessageDto(
                UUID.randomUUID().toString(),
                myId,
                currentFriendId,
                TextType.TEXT,
                PrivateMessageType.CHAT_MESSAGE,
                content,
                Instant.now().toString()
        );

        String json = gson.toJson(dto);
        Log.i("Chat", "Sending message: " + json);
        WebSocketManager.getInstance().sendMessage(json);

        executor.execute(() -> {
            MessageEntity messageEntity = ConversationUtils.mapChatMessageDtoToMessageEntity(dto);
            messageDao.insertMessage(messageEntity);
        });

    }

    public void loadMoreHistory(String friendId, Instant oldestTimestamp) {
        executor.execute(() -> {
            boolean hasLocalHistory = messageDao.hasMessagesBefore(currentConversationId, oldestTimestamp);

            if (hasLocalHistory) {
                Log.i("MessageRepository", "Loading local history");
                return;
            }

            Log.i("MessageRepository", "Loading remote history");
            fetchAndInsertMessages(friendId, oldestTimestamp);
        });
    }

    private void fetchAndInsertMessages(String friendId, Instant beforeTime) {
        long epoch = DateConverter.dateToTimestamp(beforeTime);

        messageApi.getConversation(friendId, epoch, 20).enqueue(new Callback<ApiResponse<MessageHistoryDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageHistoryDto>> call, Response<ApiResponse<MessageHistoryDto>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    List<MessageDto> messagesDto = response.body().data().messages();

                    if (messagesDto == null || messagesDto.isEmpty()) return;

                    executor.execute(() ->{
                        List<MessageEntity> messages = messagesDto.stream()
                                .map(ConversationUtils::mapMessageDtoToMessageEntity)
                                .collect(Collectors.toList());
                        messageDao.insertAll(messages);
                    });

                } else {
                    Log.e("MessageRepository", "Error fetching history: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageHistoryDto>> call, Throwable t) {
                Log.e("MessageRepository", "Network Failure: " + t.getMessage());
            }
        });
    }

    public LiveData<List<MessageEntity>> getMessagesForChat(String conversationId, int limit) {
        return messageDao.getMessagesForChat(conversationId, limit);
    }

    public LiveData<List<MessageEntity>> getRecentChats() {
        return messageDao.getRecentChats();
    }

    public void setCurrentConversationId(String currentConversationId) {
        this.currentConversationId = currentConversationId;
    }

    public void clearCurrentConversationId() {
        this.currentConversationId = null;
    }


}