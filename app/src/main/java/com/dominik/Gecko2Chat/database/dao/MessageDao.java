package com.dominik.Gecko2Chat.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.dominik.Gecko2Chat.database.entities.MessageEntity;
import com.dominik.Gecko2Chat.enums.MessageStatus;

import java.time.Instant;
import java.util.List;

@Dao
public interface MessageDao {

    //Get all messages for a chat
    @Query("SELECT * FROM (SELECT * FROM messages WHERE conversationId = :friendId ORDER BY timestamp DESC LIMIT :limit) ORDER BY timestamp ASC")
    LiveData<List<MessageEntity>> getMessagesForChat(String friendId, int limit);

    //Returns true if there are ANY messages older than the given timestamp
    @Query("SELECT COUNT(*) > 0 FROM messages WHERE conversationId = :conversationId AND timestamp < :timestamp")
    boolean hasMessagesBefore(String conversationId, Instant timestamp);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(MessageEntity message);

    @Query("UPDATE messages SET status = :newStatus WHERE conversationId = :conversationId AND senderId = :myId AND timestamp <= :timestamp AND status = 'SENT'")
    void markMessagesAsDelivered(String conversationId, String myId, Instant timestamp, MessageStatus newStatus);

    @Query("UPDATE messages SET status = 'READ' WHERE conversationId = :conversationId AND senderId = :senderUserId AND status = 'DELIVERED'")
    void markAllMessagesAsRead(String conversationId, String senderUserId);

    @Query("UPDATE messages SET status = :newStatus WHERE conversationId = :conversationId AND senderId = :myId AND timestamp <= :timestamp AND (status = 'SENT' OR status = 'DELIVERED')")
    void markMessagesAsRead(String conversationId, String myId, Instant timestamp, MessageStatus newStatus);
    @Query("UPDATE messages SET status = :status, timestamp = :timestamp WHERE messageId = :messageId")
    void updateStatusAndTimestamp(String messageId, MessageStatus status, Instant timestamp);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MessageEntity> messages);

    //Get the last message of each chat
    @Query("""
            SELECT m.* FROM messages m
            INNER JOIN (SELECT conversationId, MAX(timestamp) as max_ts FROM messages GROUP BY conversationId) latest
            ON m.conversationId = latest.conversationId AND m.timestamp = latest.max_ts
            ORDER BY m.timestamp DESC
            """)
    LiveData<List<MessageEntity>> getRecentChats();

    //Get last message stored in Room
    @Query("SELECT MAX(timestamp) FROM messages")
    Instant getLatestTimestamp();

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND senderId = :senderUserId AND status = 'DELIVERED'")
    int numberOfMessagesDelivered(String conversationId, String senderUserId);
}
