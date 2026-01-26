package com.dominik.Gecko2Chat.utils;

import android.util.Log;

import com.dominik.Gecko2Chat.enums.ErrorCode;
import com.dominik.Gecko2Chat.model.api.ApiResponse;
import com.google.gson.Gson;

import retrofit2.Response;

public class ErrorUtils {

    public static ErrorCode parseError(Response<?> response) {
        try {
            if (response.errorBody() == null) {
                return ErrorCode.RESOURCE_NOT_FOUND;
            }

            // Parse the error body into your ApiResponse wrapper
            Gson gson = new Gson();
            ApiResponse<?> errorResponse = gson.fromJson(
                    response.errorBody().charStream(),
                    ApiResponse.class
            );

            if (errorResponse != null && errorResponse.code() != null) {
                return ErrorCode.valueOf(errorResponse.code());
            }

        } catch (Exception e) {
            Log.e("ErrorUtils", "Error parsing response", e);
        }

        // Default fallback if parsing fails
        return ErrorCode.RESOURCE_NOT_FOUND;
    }
}