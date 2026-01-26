package com.dominik.Gecko2Chat.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.dominik.Gecko2Chat.database.entities.FriendRequestEntity;

import java.util.List;

@Dao
public interface FriendRequestDao {

    @Query("SELECT COUNT(*) FROM friend_requests")
    LiveData<Integer> getFriendRequestsCount();


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFriendRequest(FriendRequestEntity friendRequest);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FriendRequestEntity> friendRequests);

    @Query("SELECT * FROM friend_requests")
    LiveData<List<FriendRequestEntity>> getFriendRequests();
}
