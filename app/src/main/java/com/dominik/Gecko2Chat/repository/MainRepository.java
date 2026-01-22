package com.dominik.Gecko2Chat.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dominik.Gecko2Chat.database.AppDatabase;
import com.dominik.Gecko2Chat.database.FriendDao;
import com.dominik.Gecko2Chat.database.FriendEntity;
import com.dominik.Gecko2Chat.database.MessageDao;
import com.dominik.Gecko2Chat.database.MessageEntity;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.api.UserApi;
import com.dominik.Gecko2Chat.model.response.ConversationSummaryDto;
import com.dominik.Gecko2Chat.model.response.StartupDto;
import com.dominik.Gecko2Chat.rest.RestClient;
import com.dominik.Gecko2Chat.utils.ConversationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainRepository {

    private static MainRepository instance;
    private UserApi userApi;
    private MessageDao messageDao;
    private FriendDao friendDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();



    private MainRepository(Context context) {
        userApi = RestClient.getInstance(context).getUserApi();
        AppDatabase db = AppDatabase.getInstance(context);
        messageDao = db.messageDao();
        friendDao = db.friendDao();
    }

    public static MainRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MainRepository(context);
        }
        return instance;
    }

    public void refreshStartupData() {
        userApi.getStartup().enqueue(new Callback<ApiResponse<StartupDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<StartupDto>> call, Response<ApiResponse<StartupDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StartupDto data = response.body().data();

                    executor.execute(() -> {
                        List<FriendEntity> friends = data.friendsList().stream()
                                .map(friend -> new FriendEntity(friend.internalId(), friend.username(), friend.displayName(), friend.profileBio(), friend.profileImageUrl()))
                                .toList();
                        friendDao.insertAll(friends);


                        List<MessageEntity> messages = data.conversationSummary().stream()
                                .map(conv -> ConversationUtils.mapMessageDtoToMessageEntity(conv.lastMessage()))
                                .toList();
                        messageDao.insertAll(messages);

                        Log.d("MainRepository", "Startup data refreshed");
                    });

                } else
                    Log.e("MainRepository", "Startup data refresh failed");
            }

            @Override
            public void onFailure(Call<ApiResponse<StartupDto>> call, Throwable t) {
                // Handle error (e.g. post to a separate Error LiveData)
            }
        });
    }

    public LiveData<List<FriendEntity>> getFriends() {
        return friendDao.getAllFriends();
    }
}
