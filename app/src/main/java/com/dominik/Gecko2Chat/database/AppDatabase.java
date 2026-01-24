package com.dominik.Gecko2Chat.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {MessageEntity.class, FriendEntity.class}, version = 1)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
    public abstract FriendDao friendDao();
    public abstract FriendRequestDao friendRequestDao();


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
