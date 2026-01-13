package com.dominik.Gecko2Chat.model.response;

//This is to send a message to messaging-service, it doesn't have a message Id yet.
public record ChatMessageDto(
        String senderId,
        String recipientId,
        String content,
        String timestamp
) {
}