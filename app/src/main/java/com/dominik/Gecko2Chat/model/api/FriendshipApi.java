package com.dominik.Gecko2Chat.model.api;

import com.dominik.Gecko2Chat.model.response.PublicUserResponseDto;
import com.dominik.Gecko2Chat.model.response.websocket.FriendRequestDto;

import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Everything related to user friends, friend requests, blocked...
 */
public interface FriendshipApi {

    @GET("/user-service/api/v1/friends")
    Call<ApiResponse<Set<PublicUserResponseDto>>> checkUsernameAvailability();

    @GET("/user-service/api/v1/friends/requests")
    Call<ApiResponse<List<FriendRequestDto>>> pendingFriendRequests();



}
