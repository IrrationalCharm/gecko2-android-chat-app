package com.dominik.Gecko2Chat.database;

import androidx.lifecycle.LiveData;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

public interface FriendRequestDao {

    @Query("SELECT * FROM friend_requests")
    LiveData<List<FriendRequestEntity>> getAllFriendRequests();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFriendRequest(FriendRequestEntity friendRequest);

}
