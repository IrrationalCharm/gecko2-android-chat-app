package com.dominik.Gecko2Chat.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.dominik.Gecko2Chat.database.AppDatabase;
import com.dominik.Gecko2Chat.database.dao.ConversationDao;
import com.dominik.Gecko2Chat.database.dao.FriendDao;
import com.dominik.Gecko2Chat.database.dao.FriendRequestDao;
import com.dominik.Gecko2Chat.database.entities.ConversationEntity;
import com.dominik.Gecko2Chat.database.entities.FriendEntity;
import com.dominik.Gecko2Chat.database.dao.MessageDao;
import com.dominik.Gecko2Chat.database.entities.FriendRequestEntity;
import com.dominik.Gecko2Chat.database.entities.MessageEntity;
import com.dominik.Gecko2Chat.enums.MessageStatus;
import com.dominik.Gecko2Chat.enums.MessageType;
import com.dominik.Gecko2Chat.model.User;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.api.UserApi;
import com.dominik.Gecko2Chat.model.response.MessageHistoryDto;
import com.dominik.Gecko2Chat.model.response.StartupDto;
import com.dominik.Gecko2Chat.model.websocket.FriendRequestDto;
import com.dominik.Gecko2Chat.model.websocket.outgoing.ClientMessage;
import com.dominik.Gecko2Chat.model.websocket.outgoing.SendDeliveredReceiptRequest;
import com.dominik.Gecko2Chat.rest.RestClient;
import com.dominik.Gecko2Chat.utils.ConversationUtils;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.dominik.Gecko2Chat.utils.mapper.FriendMapper;
import com.dominik.Gecko2Chat.utils.mapper.FriendRequestMapper;
import com.dominik.Gecko2Chat.utils.mapper.UserMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import retrofit2.Response;


//Im aware that this class is messy and needs refactoring...
public class MainRepository {

    private static MainRepository instance;
    private final UserManager userManager;
    private final UserApi userApi;
    private final MessageDao messageDao;
    private final FriendDao friendDao;
    private final ConversationDao conversationDao;
    private final FriendRequestDao friendRequestDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final CompositeDisposable compositeDisposable;


    private MainRepository(Context context) {
        userApi = RestClient.getInstance(context).getUserApi();
        AppDatabase db = AppDatabase.getInstance(context);
        userManager = UserManager.getInstance(context);
        compositeDisposable = new CompositeDisposable();
        messageDao = db.messageDao();
        friendDao = db.friendDao();
        conversationDao = db.conversationDao();
        friendRequestDao = db.friendRequestDao();

        monitorConnectionStatus();
    }

    public static synchronized MainRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MainRepository(context);
        }
        return instance;
    }


    /**
     * Makes a request to the server to refresh the startup data. Includes:
     *      - the list of friends
     *      - Logged-in User data
     *      - Conversations
     *      - Friend requests
     */
    public void refreshStartupData() {
        executor.execute(() -> {
            Instant lastMessageTimestamp = messageDao.getLatestTimestamp();
            long timestampEpoch = lastMessageTimestamp == null ? 0 : lastMessageTimestamp.toEpochMilli();

            try {
                Response<ApiResponse<StartupDto>> response = userApi.getStartupSync(timestampEpoch).execute();

                if(!response.isSuccessful() || response.body() == null) {
                    Log.e("MainRepository", "Error refreshing startup data: " + response.code());
                    return;
                }

                StartupDto data = response.body().data();

                //Map and save friends
                if (data.friendsList() != null) {
                    List<FriendEntity> friends = data.friendsList().stream()
                            .map(FriendMapper::mapFriendDtoToEntity)
                            .toList();
                    friendDao.insertAll(friends);

                } else Log.e("MainRepository", "Friends list is null");


                //Save logged-in user data into SharedPreferences
                if (data.userDto() != null) {
                    User user = UserMapper.mapDtoToUser(data.userDto(), true);
                    userManager.saveUser(user);
                } else Log.e("MainRepository", "User data is null");


                //Sync conversations
                String myId = userManager.getUser().internalId();
                if(data.conversationSummary() != null) {
                    List<MessageHistoryDto> conversations = data.conversationSummary();

                    for (MessageHistoryDto conv : conversations) {
                        List<MessageEntity> messageEntities = conv.messages().stream()
                                .map(ConversationUtils::mapMessageDtoToMessageEntity)
                                .collect(Collectors.toList());

                        if (!messageEntities.isEmpty()) {
                            //Update messages that current user received but are marked as sent. and change them to delivered
                            for(MessageEntity msg : messageEntities) {
                                if(msg.recipientId.equals(myId) && msg.status == MessageStatus.SENT) {
                                    msg.status = MessageStatus.DELIVERED;
                                }
                            }
                            messageDao.insertAll(messageEntities);

                            Optional<MessageEntity> lastMessageOpt = messageEntities.stream()
                                    .max(Comparator.comparing(m -> m.timestamp));//Deliver it to the server, so that the other user can see (if online) that it was deliveredTimestamp. Otherwise it just updates the Conversation Table in message-persistence-service

                            if(lastMessageOpt.isPresent()) {
                                MessageEntity lastMessage = lastMessageOpt.get();
                                String friendId = lastMessage.senderId.equals(myId) ? lastMessage.recipientId : lastMessage.senderId;

                                conversationDao.insertOrUpdate(new ConversationEntity(
                                        conv.conversationId(),
                                        friendId,
                                        lastMessage.content,
                                        lastMessage.timestamp,
                                        conv.unreadCount(),
                                        lastMessage.senderId
                                        ));
                            }

                            Optional<MessageEntity> lastReceivedMessageOpt = messageEntities.stream()
                                    .filter(msg -> msg.recipientId.equals(myId))
                                    .max(Comparator.comparing(m -> m.timestamp));

                            lastReceivedMessageOpt.ifPresent(this::sendDeliveryReceipt);
                        }

                        messageDao.markMessagesAsDelivered(conv.conversationId(), myId, conv.lastDeliveredMessage(), MessageStatus.DELIVERED);

                    }
                } else Log.e("MainRepository", "Conversation summary is null");


                //Sync friend requests
                if(data.pendingRequests() != null) {
                    List<FriendRequestDto> friendRequests = data.pendingRequests();

                    List<FriendRequestEntity> friendRequestEntities = friendRequests.stream()
                            .map(FriendRequestMapper::mapFriendRequestDtoToEntity)
                            .toList();

                    friendRequestDao.insertAll(friendRequestEntities);
                }
                Log.d("MainRepository", "Startup data refreshed");

            } catch (IOException e) {
                Log.e("MainRepository", "Error refreshing startup data", e);

            }
        });
    }


    private void sendDeliveryReceipt(MessageEntity msg) {
        ClientMessage delivered = new SendDeliveredReceiptRequest(
                MessageType.DELIVERY_RECEIPT_CLIENT,
                msg.recipientId, //Im the recipient of the incoming message but the sender of the deliveredTimestamp receipt
                msg.senderId,
                msg.messageId,
                msg.conversationId,
                msg.timestamp.toString()
        );

        WebSocketManager.getInstance().send(delivered);
    }


    private void monitorConnectionStatus() {
        // 4. Update the LiveData
        Disposable d = ( WebSocketManager.getInstance().getConnectionStatus()
                        .filter(status -> status == WebSocketManager.ConnectionStatus.CONNECTED)
                        // Avoid rapid firing if status flutters
                        .subscribe(status -> {
                            Log.d("MainRepository", "Connection restored, refreshing data...");
                            refreshStartupData();
                        }, Throwable::printStackTrace)
        );
        compositeDisposable.add(d);
    }



    public LiveData<List<FriendEntity>> getFriends() {
        return friendDao.getAllFriends();
    }
}
