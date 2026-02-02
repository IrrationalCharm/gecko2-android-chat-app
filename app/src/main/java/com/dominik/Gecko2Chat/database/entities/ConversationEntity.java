package com.dominik.Gecko2Chat.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.Instant;

@Entity(tableName = "conversations")
public class ConversationEntity {

    @PrimaryKey
    @NonNull
    public String conversationId; //userA:userB

    public String otherUserId;    //FriendId
    public String lastMessageContent;
    public Instant lastMessageTimestamp;
    public long unreadCount;

    public String lastMessageSenderId;

    public ConversationEntity(@NonNull String conversationId, String otherUserId, String lastMessageContent, Instant lastMessageTimestamp, long unreadCount, String lastMessageSenderId) {
        this.conversationId = conversationId;
        this.otherUserId = otherUserId;
        this.lastMessageContent = lastMessageContent;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
        this.lastMessageSenderId = lastMessageSenderId;
    }
}
