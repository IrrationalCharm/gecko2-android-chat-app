package com.dominik.Gecko2Chat.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.dominik.Gecko2Chat.database.AppDatabase;
import com.dominik.Gecko2Chat.database.dao.ConversationDao;
import com.dominik.Gecko2Chat.database.dao.MessageDao;
import com.dominik.Gecko2Chat.database.entities.ConversationEntity;
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
import com.dominik.Gecko2Chat.model.websocket.outgoing.SendDeliveredReceiptRequest;
import com.dominik.Gecko2Chat.model.websocket.outgoing.SendMessageRequest;
import com.dominik.Gecko2Chat.model.websocket.outgoing.SendReadReceiptRequest;
import com.dominik.Gecko2Chat.rest.RestClient;
import com.dominik.Gecko2Chat.utils.ConversationUtils;
import com.dominik.Gecko2Chat.utils.WebSocketManager;

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
    private final MessageApi messageApi;
    private final MessageDao messageDao;
    private final ConversationDao conversationDao;
    private String currentConversationId = null;


    private MessageRepository(Context context) {
        messageDao = AppDatabase.getInstance(context).messageDao();
        conversationDao = AppDatabase.getInstance(context).conversationDao();
        messageApi = RestClient.getInstance(context).getMessagesApi();

    }

    public static synchronized MessageRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MessageRepository(context);
        }
        return instance;
    }


    //Incoming messages from websocket
    public void incomingMessage(ChatMessageEvent event) {
        String conversationId = ConversationUtils.getConversationId(event.senderId(), event.recipientId());

        boolean isChatOpen = conversationId.equals(this.currentConversationId);
        MessageStatus status = isChatOpen ? MessageStatus.READ : MessageStatus.DELIVERED;

        MessageEntity mEntity = ConversationUtils.mapChatMessageEventToMessageEntity(event, status);

        executor.execute(() -> {
            messageDao.insertMessage(mEntity);
            int increment = isChatOpen ? 0 : 1;
            conversationDao.upsertConversation(mEntity.conversationId, mEntity.senderId, mEntity.content, mEntity.timestamp, mEntity.senderId, increment);

            //If the user is in the same chat, mark message status as read, otherwise mark as delivered
            if (status == MessageStatus.READ) {
                sendReadReceipt(mEntity.recipientId, mEntity.senderId);
            } else {
                sendDeliveredReceipt(mEntity.recipientId, mEntity.senderId);
            }
        });
    }


    //Confirmation by server that message was received
    public void incomingMessageSent(MessageSentEvent event) {
        Log.d("MessageRepository", "Message acknowledged by server received: " + event.messageId());
        executor.execute(() -> messageDao.updateStatusAndTimestamp(event.messageId(), MessageStatus.SENT, Instant.parse(event.timestamp())));
    }

    //Confirmation by server that message was deliveredTimestamp to recipient
    public void incomingMessageDelivered(MessageDeliveredEvent event) {
        Log.d("MessageRepository", "Message deliveredTimestamp by server received");
        String conversationId = ConversationUtils.getConversationId(event.senderOfMessage(), event.recipientOfMessage());
        executor.execute(() -> messageDao.markMessagesAsDelivered(conversationId, event.recipientOfMessage(), Instant.parse(event.timestamp()), MessageStatus.DELIVERED));
    }

    //Confirmation by server that message was read by recipient
    public void incomingMessageRead(MessageReadEvent event) {
        Log.d("MessageRepository", "Message read by server received");
        String conversationId = ConversationUtils.getConversationId(event.senderOfMessage(), event.recipientOfMessage());
        executor.execute(() -> messageDao.markMessagesAsRead(conversationId, event.recipientOfMessage(), Instant.parse(event.timestamp()), MessageStatus.READ));
    }


    /**
     *When user just opened chat, determine if it is marked as read and notify friend
     */
    public void markConversationAsRead(String currentUserId, String friendId) {
        executor.execute(()-> {
            int numberOfMessagesOnDelivered = messageDao.numberOfMessagesDelivered(currentConversationId, friendId);

            //if(numberOfMessagesOnDelivered == 0) return;

            conversationDao.markConversationAsRead(currentConversationId);
            messageDao.markAllMessagesAsRead(currentConversationId, friendId);

            var request = new SendReadReceiptRequest(
                    MessageType.READ_RECEIPT_CLIENT,
                    currentUserId, //The current user who just read the new message
                    friendId, //who will receive the event that the message is read
                    currentConversationId,
                    Instant.now().toString());

            WebSocketManager.getInstance().send(request);
        });
    }


    //Send read receipt through Websocket
    public void sendReadReceipt(String currentUserId, String friendId) {
        var readReceiptRequest = new SendReadReceiptRequest(
                MessageType.READ_RECEIPT_CLIENT,
                currentUserId, //The current user who just read the new message
                friendId, //who will receive the event that the message is read
                ConversationUtils.getConversationId(currentUserId, friendId),
                Instant.now().toString()); //Ignored by server for now

        WebSocketManager.getInstance().send(readReceiptRequest);
    }


    public void sendDeliveredReceipt(String currentUserId, String friendId) {
        var deliveredReceiptRequest = new SendDeliveredReceiptRequest(
                MessageType.DELIVERY_RECEIPT_CLIENT,
                currentUserId, //Im the recipient of the incoming message but the sender of the deliveredTimestamp receipt
                friendId, //who will receive the notification that the message is deliveredTimestamp
                "nothing", //messageId ignored by server, to be removed.
                ConversationUtils.getConversationId(currentUserId, friendId),
                Instant.now().toString());

        WebSocketManager.getInstance().send(deliveredReceiptRequest);
    }


    //Send a message through Websocket
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
            conversationDao.upsertConversation(messageEntity.conversationId, messageEntity.recipientId, messageEntity.content, messageEntity.timestamp, messageEntity.senderId, 1);
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

        messageApi.getConversation(friendId, epoch, 20).enqueue(new Callback<>() {
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

    public LiveData<List<ConversationEntity>> getConversations() {
        return conversationDao.getConversations();
    }

    public void setCurrentConversationId(String currentConversationId) {
        this.currentConversationId = currentConversationId;
    }

    public void clearCurrentConversationId() {
        this.currentConversationId = null;
    }


}