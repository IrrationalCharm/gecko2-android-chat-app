package com.dominik.Gecko2Chat.model.websocket;

import java.util.UUID;

public record FriendRequestDto(
        long id,
        UUID initiatorId,
        UUID receiverId,

        String initiatorUsername,
        String initiatorDisplayName,
        String initiatorUrlProfileImage,

        long createdAt
) {
}
