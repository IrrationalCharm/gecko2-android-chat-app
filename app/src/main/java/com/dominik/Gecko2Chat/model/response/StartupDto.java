package com.dominik.Gecko2Chat.model.response;

import java.util.List;
import java.util.Set;

public record StartupDto(
        UserDto userDto, //user-service/api/v1/users
        Set<PublicUserResponseDto> friendsList, //user-service/api/v1/friends
        List<ConversationSummaryDto> conversationSummary //message-persistence-service/last-messages
) {
}

