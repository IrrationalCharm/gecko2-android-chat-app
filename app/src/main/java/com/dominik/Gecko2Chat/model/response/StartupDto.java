package com.dominik.Gecko2Chat.model.response;

import com.dominik.Gecko2Chat.model.response.websocket.FriendRequestDto;

import java.util.List;
import java.util.Set;

public record StartupDto(
        UserDto userDto, //user-service/api/v1/users
        Set<FriendDto> friendsList, //user-service/api/v1/friends
        List<MessageHistoryDto> conversationSummary, //message-persistence-service/sync
        List<FriendRequestDto> pendingRequests
) {
}

