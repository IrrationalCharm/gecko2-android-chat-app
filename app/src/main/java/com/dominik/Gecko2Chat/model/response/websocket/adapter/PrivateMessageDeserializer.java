package com.dominik.Gecko2Chat.model.response.websocket.adapter;

import com.dominik.Gecko2Chat.enums.PrivateMessageType;
import com.dominik.Gecko2Chat.model.response.websocket.ChatMessageDto;
import com.dominik.Gecko2Chat.model.response.websocket.FriendRequestReceivedDto;
import com.dominik.Gecko2Chat.model.response.websocket.MessageReceivedDto;
import com.dominik.Gecko2Chat.model.response.websocket.PrivateMessage;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

// Helps deserialize json to PrivateMessage
public class PrivateMessageDeserializer implements JsonDeserializer<PrivateMessage> {

    @Override
    public PrivateMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        String typeString = jsonObject.get("type").getAsString();

        var type = PrivateMessageType.valueOf(typeString);

        return switch (type) {
            case CHAT_MESSAGE -> context.deserialize(json, ChatMessageDto.class);
            case MESSAGE_RECEIVED -> context.deserialize(json, MessageReceivedDto.class);
            case FRIEND_REQUEST_RECEIVED -> context.deserialize(json, FriendRequestReceivedDto.class);

        };

    }
}
