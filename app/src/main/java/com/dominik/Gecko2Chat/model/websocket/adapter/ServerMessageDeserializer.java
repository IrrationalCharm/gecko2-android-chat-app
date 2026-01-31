package com.dominik.Gecko2Chat.model.websocket.adapter;

import com.dominik.Gecko2Chat.enums.MessageType;
import com.dominik.Gecko2Chat.model.websocket.incoming.ChatMessageEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.FriendRequestReceivedEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.MessageDeliveredEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.MessageReadEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.MessageSentEvent;
import com.dominik.Gecko2Chat.model.websocket.incoming.ServerMessage;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

// Helps deserialize json to PrivateMessage
public class ServerMessageDeserializer implements JsonDeserializer<ServerMessage> {

    @Override
    public ServerMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        String typeString = jsonObject.get("type").getAsString();

        var type = MessageType.valueOf(typeString);

        return switch (type) {
            case CHAT_MESSAGE_SERVER -> context.deserialize(json, ChatMessageEvent.class);
            case MESSAGE_SENT_SERVER -> context.deserialize(json, MessageSentEvent.class);
            case MESSAGE_DELIVERED_SERVER -> context.deserialize(json, MessageDeliveredEvent.class);
            case MESSAGE_READ_SERVER -> context.deserialize(json, MessageReadEvent.class);
            case FRIEND_REQUEST_SERVER -> context.deserialize(json, FriendRequestReceivedEvent.class);

            default -> throw new IllegalStateException("Unexpected value: " + type);
        };

    }
}
