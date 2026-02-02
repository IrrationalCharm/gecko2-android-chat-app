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
        UPDATE conversations SET lastMessageContent =:message,
         lastMessageTimestamp = :timestamp,
         lastMessageSenderId = :senderId,
         unreadCount = unreadCount + :increment
        WHERE conversationId = :conversationId
    """)
    void updateConversation(String conversationId, String message, Instant timestamp, String senderId, int increment);

    @Query("UPDATE conversations SET unreadCount = 0 WHERE conversationId = :conversationId")
    void markConversationAsRead(String conversationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ConversationEntity conversation);
}
