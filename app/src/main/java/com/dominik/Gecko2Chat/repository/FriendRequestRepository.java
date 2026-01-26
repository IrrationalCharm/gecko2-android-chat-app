package com.dominik.Gecko2Chat.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.dominik.Gecko2Chat.database.AppDatabase;
import com.dominik.Gecko2Chat.database.dao.FriendRequestDao;
import com.dominik.Gecko2Chat.database.entities.FriendRequestEntity;
import com.dominik.Gecko2Chat.enums.ErrorCode;
import com.dominik.Gecko2Chat.enums.FriendRequestAction;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.api.FriendshipApi;
import com.dominik.Gecko2Chat.model.request.UpdateFriendRequestDto;
import com.dominik.Gecko2Chat.model.response.websocket.FriendRequestDto;
import com.dominik.Gecko2Chat.model.response.websocket.FriendRequestReceivedDto;
import com.dominik.Gecko2Chat.rest.RestClient;
import com.dominik.Gecko2Chat.utils.UserManager;

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
        void onSuccess(T data);
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
        friendshipApi.pendingFriendRequests().enqueue(new Callback<ApiResponse<List<FriendRequestDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<FriendRequestDto>>> call, Response<ApiResponse<List<FriendRequestDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FriendRequestDto> friendRequests = response.body().data();
                    executor.execute(() -> {
                        List<FriendRequestEntity> friendRequestEntities = friendRequests.stream()
                                .map(friendRequest -> new FriendRequestEntity(
                                                                                String.valueOf(friendRequest.id()),
                                                                                "INCOMING",
                                                                                friendRequest.initiatorId().toString(),
                                                                                friendRequest.receiverId().toString(),
                                                                                friendRequest.initiatorUsername(),
                                                                                friendRequest.initiatorDisplayName(),
                                                                                friendRequest.initiatorUrlProfileImage(),
                                                                                Instant.ofEpochSecond(friendRequest.createdAt())
                                                                        ))
                                .toList();

                        friendRequestDao.insertAll(friendRequestEntities);
                    });
                    Log.i("FriendRequestRepository", "Friend requests synced");
                } else {
                    Log.e("FriendRequestRepository", "Failed to sync friend requests: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<FriendRequestDto>>> call, Throwable t) {
                Log.e("FriendRequestRepository", "Failed to sync friend requests", t);
            }
        });
    }


    public LiveData<Integer> getFriendRequestsCount() {
        return friendRequestDao.getFriendRequestsCount();
    }


    public void incomingFriendRequest(FriendRequestReceivedDto dto) {
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


    public LiveData<List<FriendRequestEntity>> getFriendRequests() {
        return friendRequestDao.getFriendRequests();
    }


    public void acceptRequest(String requestId) {
        var dto = new UpdateFriendRequestDto(FriendRequestAction.ACCEPT_REQUEST);

        friendshipApi.updateFriendRequest(requestId, dto).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if(response.isSuccessful()) {
                    Log.i("FriendRequestRepository", "Friend request accepted");
                    executor.execute(() -> friendRequestDao.removeRequestById(requestId));

                } else {
                    Log.e("FriendRequestRepository", "Failed to accept friend request: " + response.message());

                }

            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("FriendRequestRepository", "Failed to accept friend request", t);
            }
        });
    }


    public void declineRequest(String requestId) {
        var dto = new UpdateFriendRequestDto(FriendRequestAction.DECLINE_REQUEST);

        friendshipApi.updateFriendRequest(requestId, dto).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if(response.isSuccessful()) {
                    Log.i("FriendRequestRepository", "Friend request denied");
                    executor.execute(() -> friendRequestDao.removeRequestById(requestId));

                } else {
                    Log.e("FriendRequestRepository", "Failed to deny friend request: " + response.message());
                }

            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("FriendRequestRepository", "Failed to deny friend request", t);
            }
        });
    }

    public void sendFriendRequest(String username, RepositoryCallback<Void> callback) {
        friendshipApi.sendFriendRequest(username).enqueue(new Callback<ApiResponse<Void>>() {

            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if(response.isSuccessful()) {
                    Log.i("FriendRequestRepository", "Friend request sent");
                    callback.onSuccess(null);
                } else {
                    //callback.onError(response.);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {

            }
        });
    }
}
