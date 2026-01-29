package com.dominik.Gecko2Chat.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.dominik.Gecko2Chat.database.AppDatabase;
import com.dominik.Gecko2Chat.database.dao.MessageDao;
import com.dominik.Gecko2Chat.database.entities.MessageEntity;
import com.dominik.Gecko2Chat.enums.MessageStatus;
import com.dominik.Gecko2Chat.enums.MessageType;
import com.dominik.Gecko2Chat.enums.TextType;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.api.MessageApi;
import com.dominik.Gecko2Chat.model.response.MessageDto;
import com.dominik.Gecko2Chat.model.response.MessageHistoryDto;
import com.dominik.Gecko2Chat.model.websocket.incoming.ChatMessageEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.MessageDeliveredEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.MessageReadEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.MessageSentEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.ServerMessage;
import com.dominik.Gecko2Chat.model.websocket.adapter.ServerMessageDeserializer;
import com.dominik.Gecko2Chat.model.websocket.outgoing.ClientMessage;
import com.dominik.Gecko2Chat.model.websocket.outgoing.SendDeliveredReceiptRequest;
import com.dominik.Gecko2Chat.model.websocket.outgoing.SendMessageRequest;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageRepository {

    private static MessageRepository instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ServerMessage.class, new ServerMessageDeserializer())
            .create();
    private final MessageApi messageApi;
    private final MessageDao messageDao;
    private String currentConversationId = null;


    private MessageRepository(Context context) {
        messageDao = AppDatabase.getInstance(context).messageDao();
        messageApi = RestClient.getInstance(context).getMessagesApi();
    }

    public static synchronized MessageRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MessageRepository(context);
        }
        return instance;
    }


    public void incomingMessage(ChatMessageEvent dto) {
        Log.i("MessageRepository", "Message received: " + dto.content());
        var mEntity = new MessageEntity();
        mEntity.messageId = dto.clientMsgId();
        mEntity.conversationId = ConversationUtils.getConversationId(dto.senderId(), dto.recipientId());
        mEntity.senderId = dto.senderId();
        mEntity.recipientId = dto.recipientId();
        mEntity.content = dto.content();
        mEntity.timestamp = Instant.parse(dto.timestamp());
        mEntity.type = dto.textType().toString();

        executor.execute(() -> {
            messageDao.insertMessage(mEntity);

            ClientMessage delivered = new SendDeliveredReceiptRequest(
                    MessageType.DELIVERY_RECEIPT_CLIENT,
                    mEntity.recipientId, //Im the recipient of the incoming message but the sender of the delivered receipt
                    mEntity.senderId,
                    mEntity.messageId,
                    mEntity.conversationId,
                    mEntity.timestamp.toString()
                    );

            WebSocketManager.getInstance().send(delivered);
        });

        // Emit notification if its not the current conversation
        if (currentConversationId != null && !currentConversationId.equals(mEntity.conversationId)) {
            Log.i("MessageRepository", "Message from another conversation received");
            //TODO emit notification
            //NotificationUtils.showNewMessageNotification(context, mEntity);
        }
    }


    //Confirmation by server that message was received
    public void incomingMessageSent(MessageSentEvent event) {
        Log.i("MessageRepository", "Message acknowledged by server received: " + event.messageId());
        executor.execute(() -> messageDao.updateStatusAndTimestamp(event.messageId(), MessageStatus.SENT, Instant.parse(event.timestamp())));
    }

    //Confirmation by server that message was received by recipient
    public void incomingMessageDelivered(MessageDeliveredEvent event) {
        Log.i("MessageRepository", "Message delivered by server received: " + event.messageId());
        executor.execute(() -> messageDao.updateStatus(event.messageId(), MessageStatus.DELIVERED));
    }

    //Confirmation by server that message was read by recipient
    public void incomingMessageRead(MessageReadEvent event) {
        Log.i("MessageRepository", "Message read by server received: " + event.messageId());
        executor.execute(() -> messageDao.updateStatus(event.messageId(), MessageStatus.READ));
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
        var message = new SendMessageRequest(
                MessageType.CHAT_MESSAGE_CLIENT,
                UUID.randomUUID().toString(),
                myId,
                currentFriendId,
                TextType.TEXT,
                content,
                Instant.now().toString() //To be overwritten by server
        );

        Log.i("Chat", "Sending message: " + message);
        WebSocketManager.getInstance().send(message);

        executor.execute(() -> {
            MessageEntity messageEntity = ConversationUtils.mapChatMessageDtoToMessageEntity(message);
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
        long epoch = beforeTime.toEpochMilli();

        messageApi.getConversation(friendId, epoch, 20).enqueue(new Callback<ApiResponse<MessageHistoryDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MessageHistoryDto>> call, @NonNull Response<ApiResponse<MessageHistoryDto>> response) {
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
            public void onFailure(@NonNull Call<ApiResponse<MessageHistoryDto>> call, @NonNull Throwable t) {
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