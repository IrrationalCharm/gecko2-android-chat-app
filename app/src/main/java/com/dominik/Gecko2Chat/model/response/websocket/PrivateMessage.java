package com.dominik.Gecko2Chat.model.response.websocket;

import com.dominik.Gecko2Chat.enums.PrivateMessageType;

public sealed interface PrivateMessage permits ChatMessageDto, MessageReceivedDto, FriendRequestReceivedDto {
    PrivateMessageType type();
}

