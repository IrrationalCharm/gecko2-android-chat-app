package com.dominik.Gecko2Chat.model.api;

import com.dominik.Gecko2Chat.model.response.StartupDto;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.GET;
import retrofit2.http.Query;


public interface UserApi {


    @Multipart
    @POST("/user-service/api/v1/users/profile-image")
    Call<ApiResponse<String>> uploadAvatar(@Part MultipartBody.Part image);


    /**
     * @param sinceTimestamp used for messages, if provided, it returns all messages from this timestamp on
     *                       otherwise it will return the last 20 messages
     * @return Returns user details, friends list, friend requests and chat messages
     */
    @GET("/mobile-bff/api/v1/startup")
    Call<ApiResponse<StartupDto>> getStartupSync(@Query("sinceTimestamp") long sinceTimestamp);

}
