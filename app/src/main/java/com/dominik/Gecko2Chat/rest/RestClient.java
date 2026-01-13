package com.dominik.Gecko2Chat.rest;

import android.content.Context;

import com.dominik.Gecko2Chat.model.api.MessageApi;
import com.dominik.Gecko2Chat.model.api.RegistrationApi;
import com.dominik.Gecko2Chat.model.api.UserApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestClient {

    private static final String BASE_URL = "http://192.168.1.134:8081";
    private static RestClient instance;
    private final Retrofit retrofit;


    private RestClient(Context context) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(context))
                .build();

        //Helps convert to LocalDateTime
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, jsonDeserializationContext) -> LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, c) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .create();

        this.retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    public static synchronized RestClient getInstance(Context context) {
        if (instance == null) {
            instance = new RestClient(context.getApplicationContext());
        }
        return instance;
    }


    public RegistrationApi getRegistrationApi() {
        return retrofit.create(RegistrationApi.class);
    }

    public UserApi getUserApi() {
        return retrofit.create(UserApi.class);
    }

    public MessageApi getMessagesApi() {
        return retrofit.create(MessageApi.class);
    }

}
