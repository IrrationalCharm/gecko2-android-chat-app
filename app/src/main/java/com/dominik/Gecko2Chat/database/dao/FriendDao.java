package com.dominik.Gecko2Chat.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.dominik.Gecko2Chat.database.entities.FriendEntity;

import java.util.List;

@Dao
public interface FriendDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FriendEntity> friends);

    @Query("SELECT * FROM friends")
    LiveData<List<FriendEntity>> getAllFriends();
}
