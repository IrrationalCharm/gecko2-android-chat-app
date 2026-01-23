package com.dominik.Gecko2Chat.model.api;


import com.dominik.Gecko2Chat.model.response.MessageHistoryDto;


import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MessageApi {

    @GET("/message-persistence-service/chat/conversation/{friendId}")
    Call<ApiResponse<MessageHistoryDto>> getConversation(@Path("friendId") String friendId,
                                                         @Query("page") int page,
                                                         @Query("size") int size);

    @GET("/message-persistence-service/v2/chat/conversation/{friendId}")
    Call<ApiResponse<MessageHistoryDto>> getConversation(
            @Path("friendId") String friendId,
            @Query("before") long timestamp, // epoch
            @Query("size") int size
    );

    /**
     * @param page page of the conversations
     * @param size number of conversations per page
     * @return This returns a list of recent conversations, 20 messages per conversation
     */
    @GET("/message-persistence-service/chat/hydrated")
    Call<ApiResponse<List<MessageHistoryDto>>> getHydratedConversation(@Query("page") int page, @Query("size") int size);

    @GET("/message-persistence-service/v2/chat/sync")
    Call<ApiResponse<List<MessageHistoryDto>>> getSyncConversation(@Query("sinceTimestamp") long sinceTimestamp);
}
