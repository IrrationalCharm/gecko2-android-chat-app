package com.dominik.Gecko2Chat.repository;

import android.content.Context;
import android.util.Log;

import com.dominik.Gecko2Chat.model.MessageModel;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.dominik.Gecko2Chat.model.api.MessageApi;
import com.dominik.Gecko2Chat.model.response.MessageDto;
import com.dominik.Gecko2Chat.model.response.MessageHistoryDto;
import com.dominik.Gecko2Chat.rest.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageRepository {

    private static MessageRepository instance;
    private MessageApi messageApi;


    private MessageRepository(Context context) {
        messageApi = RestClient.getInstance(context).getMessagesApi();
    }

    public static MessageRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MessageRepository(context);
        }
        return instance;
    }


    public interface MessageHistoryCallback {
        void onSuccess(List<MessageModel> messages);
        void onError(String errorMessage);
    }

    public void getConversationHistory(String friendId, int page, int size, MessageHistoryCallback callback) {
        messageApi.getConversation(friendId, page, size).enqueue(new Callback<ApiResponse<MessageHistoryDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageHistoryDto>> call, Response<ApiResponse<MessageHistoryDto>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    List<MessageDto> messagesDto = response.body().data().messages();
                    List<MessageModel> messages = messagesDto.stream()
                                    .map(messageDto -> {
                                        String receiverId = messageDto.conversationId().split(":")[0].equals(messageDto.senderId()) ? messageDto.conversationId().split(":")[1] : messageDto.conversationId().split(":")[0];
                                        return new MessageModel(messageDto.clientMsgId(), messageDto.senderId(), receiverId, messageDto.content(), messageDto.timestamp(), messageDto.textType());})
                                    .collect(Collectors.toList());

                    Collections.reverse(messages);
                    callback.onSuccess(messages);
                } else {
                    Log.e("MessageRepository", "Error fetching history: " + response.code());
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageHistoryDto>> call, Throwable t) {
                Log.e("MessageRepository", "Network Failure: " + t.getMessage());
                callback.onError("Error: " + t.getMessage());
            }
        });
    }
}