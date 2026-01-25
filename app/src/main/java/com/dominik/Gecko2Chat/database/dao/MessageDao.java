package com.dominik.Gecko2Chat.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.dominik.Gecko2Chat.database.entities.MessageEntity;

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
}
