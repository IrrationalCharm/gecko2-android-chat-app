package com.dominik.Gecko2Chat.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.dominik.Gecko2Chat.database.dao.ConversationDao;
import com.dominik.Gecko2Chat.database.dao.FriendDao;
import com.dominik.Gecko2Chat.database.dao.FriendRequestDao;
import com.dominik.Gecko2Chat.database.dao.MessageDao;
import com.dominik.Gecko2Chat.database.entities.ConversationEntity;
import com.dominik.Gecko2Chat.database.entities.FriendEntity;
import com.dominik.Gecko2Chat.database.entities.FriendRequestEntity;
import com.dominik.Gecko2Chat.database.entities.MessageEntity;

@Database(entities = {MessageEntity.class, FriendEntity.class, FriendRequestEntity.class, ConversationEntity.class}, version = 5)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
    public abstract FriendDao friendDao();
    public abstract FriendRequestDao friendRequestDao();
    public abstract ConversationDao conversationDao();



    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "gecko_chat_db")
                            .fallbackToDestructiveMigration(true)
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
