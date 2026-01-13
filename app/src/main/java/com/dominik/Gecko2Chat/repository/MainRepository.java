package com.dominik.Gecko2Chat.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.api.UserApi;
import com.dominik.Gecko2Chat.model.response.StartupDto;
import com.dominik.Gecko2Chat.rest.RestClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainRepository {

    private static MainRepository instance;
    private UserApi userApi;


    private MainRepository(Context context) {
        userApi = RestClient.getInstance(context).getUserApi();
    }

    public static MainRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MainRepository(context);
        }
        return instance;
    }

    public void getStartupData(MutableLiveData<StartupDto> data, MutableLiveData<String> error) {
        userApi.getStartup().enqueue(new Callback<ApiResponse<StartupDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<StartupDto>> call, Response<ApiResponse<StartupDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Success: Pass the inner data to the LiveData
                    data.postValue(response.body().data());
                } else {
                    error.postValue("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StartupDto>> call, Throwable t) {
                error.postValue("Network Failure: " + t.getMessage());
            }
        });
    }
}
