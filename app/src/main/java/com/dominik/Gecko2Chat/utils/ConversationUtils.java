package com.dominik.Gecko2Chat.utils;

import com.dominik.Gecko2Chat.database.entities.MessageEntity;
import com.dominik.Gecko2Chat.enums.TextType;
import com.dominik.Gecko2Chat.model.MessageModel;
import com.dominik.Gecko2Chat.model.response.MessageDto;
import com.dominik.Gecko2Chat.model.response.websocket.ChatMessageDto;

import java.time.Instant;

public final class ConversationUtils {

    // Creates a conversation id from two users ordered by string compareTo
    public static String getConversationId(String userA, String userB) {
        if(userA.compareTo(userB) > 0) {
            return String.format("%s:%s", userA, userB);
        } else
            return String.format("%s:%s", userB, userA);

    }

    //Message model is for frontend UI.
    public static MessageModel mapEntityToMessageModel(MessageEntity entity) {
        TextType type = TextType.valueOf(entity.textType);

        return new MessageModel(
                entity.messageId,
                entity.senderId,
                entity.recipientId,
                entity.content,
                entity.timestamp,
                type);
    }

    //ChatMessageDto is from real time chat
    public static MessageEntity mapChatMessageDtoToMessageEntity(ChatMessageDto dto) {
        var entity = new MessageEntity();
        entity.messageId = dto.clientMsgId();
        entity.conversationId = getConversationId(dto.senderId(), dto.recipientId());
        entity.senderId = dto.senderId();
        entity.recipientId = dto.recipientId();
        entity.content = dto.content();
        entity.timestamp = Instant.parse(dto.timestamp());
        entity.status = "STATUS_SENDING";
        entity.textType = dto.textType().name();
        return entity;
    }

    public static MessageEntity mapMessageDtoToMessageEntity(MessageDto dto) {
        var entity = new MessageEntity();
        entity.messageId = dto.clientMsgId();
        entity.conversationId = dto.conversationId();
        entity.senderId = dto.senderId();

        //Annoying code to get recipient because MessageDto doesn't contain recipient.
        entity.recipientId = dto.conversationId().split(":")[0].equals(dto.senderId()) ? dto.conversationId().split(":")[1] : dto.conversationId().split(":")[0];
        entity.content = dto.content();
        entity.timestamp = dto.timestamp();
        entity.status = "SENT";
        entity.textType = dto.textType().toString();

        return entity;
    }
}