package com.dominik.Gecko2Chat.model;

import com.dominik.Gecko2Chat.enums.MessageStatus;
import com.dominik.Gecko2Chat.enums.TextType;

import java.time.Instant;

public record MessageModel(
        String id,
        String senderId,
        String receiverId,
        String content,
        MessageStatus status,
        Instant timestamp,
        TextType type
) {
}
