package com.dominik.Gecko2Chat.model.websocket.outgoing;

import com.dominik.Gecko2Chat.enums.MessageType;

public sealed interface ClientMessage permits SendMessageRequest, SendReadReceiptRequest, SendDeliveredReceiptRequest, SendTypingStatusRequest {
    MessageType type();
}
