package com.dominik.Gecko2Chat.model.websocket.incoming;

import com.dominik.Gecko2Chat.enums.MessageType;

public record MessageSentEvent(
        MessageType type,
        String messageId,
        String timestamp
) implements ServerMessage {

}
