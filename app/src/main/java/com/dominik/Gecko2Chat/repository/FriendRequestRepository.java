package com.dominik.Gecko2Chat.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.dominik.Gecko2Chat.database.AppDatabase;
import com.dominik.Gecko2Chat.database.FriendRequestDao;
import com.dominik.Gecko2Chat.database.FriendRequestEntity;
import com.dominik.Gecko2Chat.utils.UserManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FriendRequestRepository {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final FriendRequestDao friendRequestDao;
    private static FriendRequestRepository instance;
    private UserManager userManager;

    private FriendRequestRepository(Context context) {
        userManager = UserManager.getInstance(context);
        friendRequestDao = AppDatabase.getInstance(context).friendRequestDao();
    }

    public FriendRequestRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FriendRequestRepository(context);
        }
        return instance;
    }

    public LiveData<List<FriendRequestEntity>> getFriendRequests() {
        return friendRequestDao.getAllFriendRequests();
    }

}
