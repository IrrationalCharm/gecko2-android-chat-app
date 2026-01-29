package com.dominik.Gecko2Chat.model.websocket.incoming;

import com.dominik.Gecko2Chat.enums.MessageType;

public record MessageDeliveredEvent(
        MessageType type,
        String messageId,
        String senderOfMessage,
        String recipientOfMessage,
        String timestamp
) implements ServerMessage {

}
