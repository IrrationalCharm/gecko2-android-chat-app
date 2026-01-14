package com.dominik.Gecko2Chat.model.response.websocket;

import com.dominik.Gecko2Chat.enums.PrivateMessageType;
import com.dominik.Gecko2Chat.enums.TextType;

//This is to send a message to messaging-service, it doesn't have a message Id yet.
public record ChatMessageDto(
        String clientMsgId,
        String senderId,
        String recipientId,
        TextType textType,
        PrivateMessageType type,
        String content,
        String timestamp
) implements PrivateMessage {
}