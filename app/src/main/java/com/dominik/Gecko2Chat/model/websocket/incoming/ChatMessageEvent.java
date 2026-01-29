package com.dominik.Gecko2Chat.model.websocket.incoming;

import com.dominik.Gecko2Chat.enums.MessageType;
import com.dominik.Gecko2Chat.enums.TextType;

//This is to send a message to messaging-service, it doesn't have a message Id yet.
public record ChatMessageEvent(
        MessageType type,
        String clientMsgId,
        String senderId,
        String recipientId,
        TextType textType,
        String content,
        String timestamp
) implements ServerMessage {
}