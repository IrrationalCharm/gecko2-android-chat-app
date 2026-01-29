package com.dominik.Gecko2Chat.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.dominik.Gecko2Chat.enums.MessageStatus;

import java.time.Instant;

@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey
    @NonNull
    public String messageId; // UUID created by client sender.

    public String conversationId;    // friend's ID (grouping key)
    public String senderId;
    public String recipientId;
    public String content;
    public MessageStatus status;    // Store Enum as String
    public String type;  // Store Enum as String
    public Instant timestamp; // Store as epoch time

}
