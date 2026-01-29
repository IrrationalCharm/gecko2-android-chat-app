package com.dominik.Gecko2Chat.model.websocket.incoming;

import com.dominik.Gecko2Chat.enums.MessageType;

//Just filler, need to confirm now
public record MessageReadEvent(
        MessageType type,
        String messageId,
        String senderOfMessage,
        String recipientOfMessage,
        String timestamp
) implements ServerMessage {
}
