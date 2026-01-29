package com.dominik.Gecko2Chat.model.response;

import com.dominik.Gecko2Chat.enums.MessageStatus;
import com.dominik.Gecko2Chat.enums.TextType;

import java.time.Instant;

//These are messages retrieved from messaging-persistence-service via REST (history messages) not to be confused with websocket messages.
public record MessageDto(

        String clientMsgId,
        String conversationId,
        String senderId,
        String content,
        MessageStatus status,
        Instant timestamp,
        TextType type
) {
}

