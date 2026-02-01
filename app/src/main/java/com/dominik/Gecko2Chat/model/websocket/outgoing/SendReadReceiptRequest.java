package com.dominik.Gecko2Chat.model.websocket.outgoing;

import com.dominik.Gecko2Chat.enums.MessageType;

public record SendReadReceiptRequest(
        MessageType type,
        String senderId, //Sender of request
        String recipientId, //Recipient of request
        String conversationId,
        String readTimestamp

) implements ClientMessage {
}
