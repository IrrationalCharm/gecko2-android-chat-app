package com.dominik.Gecko2Chat.model.response;

import java.time.Instant;
import java.util.Set;

public record ConversationSummaryDto(
        String conversationId,
        Set<String> participants,
        LastMessageDto lastMessage,
        Instant updatedAt
) {
}
