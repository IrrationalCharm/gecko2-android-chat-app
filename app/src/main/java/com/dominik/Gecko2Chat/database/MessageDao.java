package com.dominik.Gecko2Chat.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.time.LocalDateTime;
import java.util.List;

import io.reactivex.Single;

@Dao
public interface MessageDao {

    // 1. Get all messages for a chat
    @Query("SELECT * FROM messages WHERE conversationId = :friendId ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<MessageEntity>> getMessagesForChat(String friendId, int limit);

    //Returns true if there are ANY messages older than the given timestamp
    @Query("SELECT COUNT(*) > 0 FROM messages WHERE conversationId = :conversationId AND timestamp < :timestamp")
    boolean hasMessagesBefore(String conversationId, LocalDateTime timestamp);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(MessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MessageEntity> messages);

    // 4. Get most recent message for every chat (For MainActivity list)
    // This is a simplified query; usually, you group by chatId.
    @Query("SELECT * FROM messages GROUP BY conversationId ORDER BY timestamp DESC")
    LiveData<List<MessageEntity>> getRecentChats();
}
