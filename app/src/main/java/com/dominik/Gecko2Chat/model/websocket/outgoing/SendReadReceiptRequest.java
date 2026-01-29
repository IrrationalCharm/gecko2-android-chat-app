package com.dominik.Gecko2Chat.model.websocket.outgoing;

import com.dominik.Gecko2Chat.enums.MessageType;

public record SendReadReceiptRequest(
        MessageType type

) implements ClientMessage {
}
