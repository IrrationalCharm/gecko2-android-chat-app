package com.dominik.Gecko2Chat.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.dominik.Gecko2Chat.database.entities.ConversationEntity;

import java.time.Instant;
import java.util.List;

@Dao
public interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    LiveData<List<ConversationEntity>> getConversations();

    // Call this when a new message arrives
    @Query("""
        INSERT INTO conversations (
            conversationId,
            otherUserId,
            lastMessageContent,
            lastMessageTimestamp,
            lastMessageSenderId,
            unreadCount
        )
        VALUES (:conversationId, :otherUserId, :message, :timestamp, :senderId, :increment)
        ON CONFLICT(conversationId) DO UPDATE SET
            lastMessageContent = :message,
            lastMessageTimestamp = :timestamp,
            lastMessageSenderId = :senderId,
            unreadCount = conversations.unreadCount + :increment
    """)
    void upsertConversation(
            String conversationId,
            String otherUserId, // <--- ADDED THIS
            String message,
            Instant timestamp,
            String senderId,
            int increment
    );

    @Query("UPDATE conversations SET unreadCount = 0 WHERE conversationId = :conversationId")
    void markConversationAsRead(String conversationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ConversationEntity conversation);
}
