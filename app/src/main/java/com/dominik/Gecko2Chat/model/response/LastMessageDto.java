package com.dominik.Gecko2Chat.model.response;

import java.time.LocalDateTime;

public record LastMessageDto(
        String senderId,
        String content,
        LocalDateTime timestamp
) {
}