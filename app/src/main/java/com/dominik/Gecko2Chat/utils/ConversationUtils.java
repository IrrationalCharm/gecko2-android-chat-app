package com.dominik.Gecko2Chat.utils;

import com.dominik.Gecko2Chat.database.entities.MessageEntity;
import com.dominik.Gecko2Chat.enums.MessageStatus;
import com.dominik.Gecko2Chat.enums.TextType;
import com.dominik.Gecko2Chat.model.MessageModel;
import com.dominik.Gecko2Chat.model.response.MessageDto;
import com.dominik.Gecko2Chat.model.websocket.incoming.ChatMessageEvent;
import com.dominik.Gecko2Chat.model.websocket.outgoing.SendMessageRequest;

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
        TextType type = TextType.valueOf(entity.type);

        return new MessageModel(
                entity.messageId,
                entity.senderId,
                entity.recipientId,
                entity.content,
                entity.status,
                entity.timestamp,
                type);
    }


    public static MessageEntity mapChatMessageEventToMessageEntity(ChatMessageEvent event, MessageStatus status) {
        var mEntity = new MessageEntity();
        mEntity.messageId = event.clientMsgId();
        mEntity.conversationId = getConversationId(event.senderId(), event.recipientId());
        mEntity.senderId = event.senderId();
        mEntity.recipientId = event.recipientId();
        mEntity.content = event.content();
        mEntity.status = status;
        mEntity.timestamp = Instant.parse(event.timestamp());
        mEntity.type = event.textType().toString();
        return mEntity;
    }


    public static MessageEntity mapChatMessageDtoToMessageEntity(SendMessageRequest dto) {
        var entity = new MessageEntity();
        entity.messageId = dto.clientMsgId();
        entity.conversationId = getConversationId(dto.senderId(), dto.recipientId());
        entity.senderId = dto.senderId();
        entity.recipientId = dto.recipientId();
        entity.content = dto.content();
        entity.timestamp = Instant.parse(dto.timestamp());
        entity.status = MessageStatus.SENDING;
        entity.type = dto.textType().toString();
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
        entity.status = dto.status();
        entity.type = dto.type().toString();

        return entity;
    }
}