package com.dominik.Gecko2Chat.model.websocket.incoming;

import com.dominik.Gecko2Chat.enums.MessageType;
import com.dominik.Gecko2Chat.model.response.FriendDto;

public record FriendRequestAcceptedEvent(
        MessageType type,
        FriendDto newFriend,
        long createdAt
) implements ServerMessage {
}
