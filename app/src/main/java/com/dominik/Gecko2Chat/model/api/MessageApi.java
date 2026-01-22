package com.dominik.Gecko2Chat.model.api;


import com.dominik.Gecko2Chat.model.response.MessageHistoryDto;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MessageApi {

    @GET("/message-persistence-service/chat/conversation/{friendId}")
    Call<ApiResponse<MessageHistoryDto>> getConversation(@Path("friendId") String friendId,
                                                         @Query("page") int page,
                                                         @Query("size") int size);

    @GET("/message-persistence-service/chat/conversation/{friendId}")
    Call<ApiResponse<MessageHistoryDto>> getConversation(
            @Path("friendId") String friendId,
            @Query("before") long timestamp, // epoch
            @Query("size") int size
    );

}
