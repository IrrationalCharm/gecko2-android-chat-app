package com.dominik.Gecko2Chat.model.websocket.outgoing;

import com.dominik.Gecko2Chat.enums.MessageType;

public record SendDeliveredReceiptRequest(
        MessageType type,
        String senderId,
        String recipientId,
        String messageId,
        String conversationId,
        String timestamp

) implements ClientMessage {
}
