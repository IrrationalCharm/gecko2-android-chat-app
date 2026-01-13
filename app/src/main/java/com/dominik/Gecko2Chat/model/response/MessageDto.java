package com.dominik.Gecko2Chat.model.response;


import com.dominik.Gecko2Chat.enums.TextType;

import java.time.LocalDateTime;

public record MessageDto(
        String id,
        String conversationId,
        String senderId,
        String content,
        String timestamp,
        TextType textType
) {
}
