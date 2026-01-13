package com.dominik.Gecko2Chat.model;

import com.dominik.Gecko2Chat.enums.TextType;

import java.time.LocalDateTime;

public record MessageModel(
        String id,
        String senderId,
        String receiverId,
        String content,
        LocalDateTime timestamp,
        TextType type
) {
}
