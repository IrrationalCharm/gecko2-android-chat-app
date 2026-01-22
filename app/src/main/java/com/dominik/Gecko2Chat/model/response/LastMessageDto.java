package com.dominik.Gecko2Chat.model.response;

import java.time.Instant;

public record LastMessageDto(
        String senderId,
        String content,
        Instant timestamp
) {
}