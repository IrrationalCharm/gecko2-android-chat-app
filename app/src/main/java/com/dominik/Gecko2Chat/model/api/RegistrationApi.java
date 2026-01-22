package com.dominik.Gecko2Chat.model.api;

import com.dominik.Gecko2Chat.model.OnBoardingRequestDto;
import com.dominik.Gecko2Chat.model.response.UserDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RegistrationApi {

    @POST("/user-service/api/register/onboard")
    Call<ApiResponse<UserDto>> registerUser(@Body OnBoardingRequestDto request);

    @GET("/user-service/api/register/username-availability")
    Call<ApiResponse<String>> checkUsernameAvailability(@Query("username") String username);


}
