package com.dominik.Gecko2Chat.model.response.websocket;

import com.dominik.Gecko2Chat.enums.PrivateMessageType;

public record MessageReceivedDto(
        PrivateMessageType type,
        String uuid) implements PrivateMessage {

}
