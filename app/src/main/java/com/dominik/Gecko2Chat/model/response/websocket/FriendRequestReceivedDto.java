package com.dominik.Gecko2Chat.model.response.websocket;

import com.dominik.Gecko2Chat.enums.PrivateMessageType;


public record FriendRequestReceivedDto(
        PrivateMessageType type,
        String friendRequestId,
        String senderId,
        String senderUsername,
        String senderDisplayName,
        String senderProfileImageUrl,
        long createdAt) implements PrivateMessage {
}
