package com.dominik.Gecko2Chat.model.response;

import com.dominik.Gecko2Chat.enums.TextType;

import java.time.Instant;

public record LastMessageDto(
        String clientMsgId,
        String conversationId,
        String senderId,
        String content,
        Instant timestamp,
        TextType textType
) {
}