package com.dominik.Gecko2Chat.model.response;

import java.time.LocalDateTime;
import java.util.Set;

public record ConversationSummaryDto(
        String conversationId,
        Set<String> participants,
        LastMessageDto lastMessage,
        LocalDateTime updatedAt
) {
}
