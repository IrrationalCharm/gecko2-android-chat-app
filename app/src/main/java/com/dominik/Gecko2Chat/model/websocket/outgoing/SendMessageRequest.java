package com.dominik.Gecko2Chat.model.websocket.outgoing;

import com.dominik.Gecko2Chat.enums.MessageType;
import com.dominik.Gecko2Chat.enums.TextType;

public record SendMessageRequest(
        MessageType type,
        String clientMsgId,
        String senderId,
        String recipientId,
        TextType textType,
        String content,
        String timestamp

) implements ClientMessage {

}
