package com.dominik.Gecko2Chat.model.api;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;

public record ApiResponse<T>(

        @SerializedName("code")
        String code,
        @SerializedName("status")
        int status,
        @SerializedName("detail")
        String detail,
        @SerializedName("data")
        T data,
        @SerializedName("instance")
        String instance,
        @SerializedName("timestamp")
        String timestamp
) {
}
