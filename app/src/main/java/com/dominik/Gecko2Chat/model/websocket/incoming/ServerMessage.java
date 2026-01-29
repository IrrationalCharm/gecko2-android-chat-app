package com.dominik.Gecko2Chat.model.websocket.incoming;

import com.dominik.Gecko2Chat.enums.MessageType;

public sealed interface ServerMessage permits ChatMessageEvent, MessageSentEvent, MessageDeliveredEvent, MessageReadEvent, FriendRequestReceivedEvent {
    MessageType type();
}

