package com.dominik.Gecko2Chat.repository;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.dominik.Gecko2Chat.database.AppDatabase;
import com.dominik.Gecko2Chat.database.dao.FriendRequestDao;
import com.dominik.Gecko2Chat.database.entities.FriendRequestEntity;
import com.dominik.Gecko2Chat.enums.ErrorCode;
import com.dominik.Gecko2Chat.enums.FriendRequestAction;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.api.FriendshipApi;
import com.dominik.Gecko2Chat.model.request.UpdateFriendRequestDto;
import com.dominik.Gecko2Chat.model.websocket.FriendRequestDto;
import com.dominik.Gecko2Chat.model.websocket.incoming.FriendRequestReceivedEvent;
import com.dominik.Gecko2Chat.rest.RestClient;
import com.dominik.Gecko2Chat.utils.ErrorUtils;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.mapper.FriendRequestMapper;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRequestRepository {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final FriendRequestDao friendRequestDao;
    private final FriendshipApi friendshipApi;
    private static FriendRequestRepository instance;
    private final UserManager userManager;

    public interface RepositoryCallback<T> {
        void onSuccess();
        void onError(ErrorCode message);
    }

    private FriendRequestRepository(Context context) {
        userManager = UserManager.getInstance(context);
        friendRequestDao = AppDatabase.getInstance(context).friendRequestDao();
        friendshipApi = RestClient.getInstance(context).getFriendshipApi();
    }

    public static FriendRequestRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FriendRequestRepository(context);
        }
        return instance;
    }


    public void syncFriendRequests() {
        friendshipApi.pendingFriendRequests().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<FriendRequestDto>>> call, @NonNull Response<ApiResponse<List<FriendRequestDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FriendRequestDto> friendRequests = response.body().data();
                    executor.execute(() -> {
                        List<FriendRequestEntity> friendRequestEntities = friendRequests.stream()
                                .map(FriendRequestMapper::mapFriendRequestDtoToEntity)
                                .toList();

                        friendRequestDao.insertAll(friendRequestEntities);
                    });
                    Log.i("FriendRequestRepository", "Friend requests synced");
                } else {
                    Log.e("FriendRequestRepository", "Failed to sync friend requests: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<FriendRequestDto>>> call, @NonNull Throwable t) {
                Log.e("FriendRequestRepository", "Failed to sync friend requests", t);
            }
        });
    }




    public void incomingFriendRequest(FriendRequestReceivedEvent dto) {
        Log.i("FriendRequestRepository", "Friend request received: " + dto);

        var friendRequest = new FriendRequestEntity(
                dto.friendRequestId(),
                "INCOMING",
                dto.senderId(),
                userManager.getUser().internalId(),
                dto.senderUsername(),
                dto.senderDisplayName(),
                dto.senderProfileImageUrl(),
                Instant.ofEpochMilli(dto.createdAt())

        );
        executor.execute(() -> friendRequestDao.insertFriendRequest(friendRequest));
    }


    public void acceptRequest(String requestId) {
        var dto = new UpdateFriendRequestDto(FriendRequestAction.ACCEPT_REQUEST);

        friendshipApi.updateFriendRequest(requestId, dto).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if(response.isSuccessful()) {
                    Log.i("FriendRequestRepository", "Friend request accepted");
                    executor.execute(() -> friendRequestDao.removeRequestById(requestId));

                } else {
                    Log.e("FriendRequestRepository", "Failed to accept friend request: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Log.e("FriendRequestRepository", "Failed to accept friend request", t);
            }
        });
    }


    public void declineRequest(String requestId) {
        var dto = new UpdateFriendRequestDto(FriendRequestAction.DECLINE_REQUEST);

        friendshipApi.updateFriendRequest(requestId, dto).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if(response.isSuccessful()) {
                    Log.i("FriendRequestRepository", "Friend request denied");
                    executor.execute(() -> friendRequestDao.removeRequestById(requestId));

                } else {
                    Log.e("FriendRequestRepository", "Failed to deny friend request: " + response.message());
                }

            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Log.e("FriendRequestRepository", "Failed to deny friend request", t);
            }
        });
    }


    public void sendFriendRequest(String username, RepositoryCallback<Void> callback) {
        friendshipApi.sendFriendRequest(username).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if(response.isSuccessful()) {
                    Log.i("FriendRequestRepository", "Friend request sent");
                    callback.onSuccess();
                } else {
                    Log.e("FriendRequestRepository", "Failed to send friend request: " + response.message());
                    callback.onError(ErrorUtils.parseError(response)); //Extracts from the returning error body the error code and parses it to ErrorCode
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Log.e("FriendRequestRepository", "Failed to send friend request", t);
                callback.onError(ErrorCode.UNKNOWN_ERROR);
            }
        });
    }


    public LiveData<Integer> getFriendRequestsCount() {
        return friendRequestDao.getFriendRequestsCount();
    }


    public LiveData<List<FriendRequestEntity>> getFriendRequests() {
        return friendRequestDao.getFriendRequests();
    }
}
