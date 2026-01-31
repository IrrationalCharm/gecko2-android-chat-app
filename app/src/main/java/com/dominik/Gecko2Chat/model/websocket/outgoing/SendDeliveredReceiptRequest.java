package com.dominik.Gecko2Chat.model.websocket.outgoing;

import com.dominik.Gecko2Chat.enums.MessageType;

public record SendDeliveredReceiptRequest(
        MessageType type,
        String senderId, //the user that confirmed the message has been sent, so the sender of this request
        String recipientId, //who will receive the notification that the message is deliveredTimestamp
        String messageId,
        String conversationId,
        String deliveredTimestamp

) implements ClientMessage {
}
