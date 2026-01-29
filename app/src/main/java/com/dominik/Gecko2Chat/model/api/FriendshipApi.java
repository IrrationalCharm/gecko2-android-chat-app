package com.dominik.Gecko2Chat.model.api;

import com.dominik.Gecko2Chat.model.request.UpdateFriendRequestDto;
import com.dominik.Gecko2Chat.model.response.FriendDto;
import com.dominik.Gecko2Chat.model.websocket.FriendRequestDto;

import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Everything related to user friends, friend requests, blocked...
 */
public interface FriendshipApi {

    @GET("/user-service/api/v1/friends")
    Call<ApiResponse<Set<FriendDto>>> getFriendsList();

    @GET("/user-service/api/v1/friends/requests")
    Call<ApiResponse<List<FriendRequestDto>>> pendingFriendRequests();

    @PATCH("/user-service/api/v1/friends/requests/{requestId}")
    Call<ApiResponse<Void>> updateFriendRequest(@Path("requestId") String requestId, @Body UpdateFriendRequestDto dto);

    @POST("/user-service/api/v1/friends/requests/{username}")
    Call<ApiResponse<Void>> sendFriendRequest(@Path("username") String username);
}
