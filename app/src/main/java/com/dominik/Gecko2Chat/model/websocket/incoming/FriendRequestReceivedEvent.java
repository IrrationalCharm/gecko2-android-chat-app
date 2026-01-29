package com.dominik.Gecko2Chat.model.websocket.incoming;

import com.dominik.Gecko2Chat.enums.MessageType;


public record FriendRequestReceivedEvent(
        MessageType type,
        String friendRequestId,
        String senderId,
        String senderUsername,
        String senderDisplayName,
        String senderProfileImageUrl,
        long createdAt) implements ServerMessage {
}
