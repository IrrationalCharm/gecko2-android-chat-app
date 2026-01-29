package com.dominik.Gecko2Chat.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.dominik.Gecko2Chat.database.AppDatabase;
import com.dominik.Gecko2Chat.database.dao.FriendDao;
import com.dominik.Gecko2Chat.database.dao.FriendRequestDao;
import com.dominik.Gecko2Chat.database.entities.FriendEntity;
import com.dominik.Gecko2Chat.database.dao.MessageDao;
import com.dominik.Gecko2Chat.database.entities.FriendRequestEntity;
import com.dominik.Gecko2Chat.database.entities.MessageEntity;
import com.dominik.Gecko2Chat.model.User;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.api.UserApi;
import com.dominik.Gecko2Chat.model.response.MessageHistoryDto;
import com.dominik.Gecko2Chat.model.response.StartupDto;
import com.dominik.Gecko2Chat.model.websocket.FriendRequestDto;
import com.dominik.Gecko2Chat.rest.RestClient;
import com.dominik.Gecko2Chat.utils.ConversationUtils;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.mapper.FriendMapper;
import com.dominik.Gecko2Chat.utils.mapper.FriendRequestMapper;
import com.dominik.Gecko2Chat.utils.mapper.UserMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import retrofit2.Response;

public class MainRepository {

    private static MainRepository instance;
    private final UserManager userManager;
    private final UserApi userApi;
    private final MessageDao messageDao;
    private final FriendDao friendDao;
    private final FriendRequestDao friendRequestDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();



    private MainRepository(Context context) {
        userApi = RestClient.getInstance(context).getUserApi();
        AppDatabase db = AppDatabase.getInstance(context);
        userManager = UserManager.getInstance(context);

        messageDao = db.messageDao();
        friendDao = db.friendDao();
        friendRequestDao = db.friendRequestDao();
    }

    public static MainRepository getInstance(Context context) {
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
                if(data.conversationSummary() != null) {
                    List<MessageHistoryDto> messages = data.conversationSummary();

                    for (MessageHistoryDto dto : messages) {
                        List<MessageEntity> messageEntities = dto.messages().stream()
                                .map(ConversationUtils::mapMessageDtoToMessageEntity)
                                .collect(Collectors.toList());
                        messageDao.insertAll(messageEntities);
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
                throw new RuntimeException(e);
            }
        });


    }

    public LiveData<List<FriendEntity>> getFriends() {
        return friendDao.getAllFriends();
    }
}
