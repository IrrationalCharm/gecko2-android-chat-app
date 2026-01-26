package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.dominik.Gecko2Chat.database.entities.FriendRequestEntity;
import com.dominik.Gecko2Chat.model.FriendRequestModel;
import com.dominik.Gecko2Chat.repository.FriendRequestRepository;
import com.dominik.Gecko2Chat.utils.mapper.FriendRequestMapper;

import java.util.List;

public class FriendRequestViewModel extends AndroidViewModel {

    private final FriendRequestRepository repository;
    private final LiveData<List<FriendRequestModel>> requests;

    public FriendRequestViewModel(@NonNull Application application) {
        super(application);

        repository = FriendRequestRepository.getInstance(application);

        LiveData<List<FriendRequestEntity>> requestEntities = repository.getFriendRequests();
        //Everytime friend request repository is updated, this code is run and maps to friend request model
        requests = Transformations.map(requestEntities, entities ->
            entities.stream()
                    .map(FriendRequestMapper::mapEntityToFriendRequestModel)
                    .toList());
    }

    public void acceptRequest(String requestId) {
        repository.acceptRequest(requestId);
    }

    public void declineRequest(String requestId) {
        repository.declineRequest(requestId);
    }

    public LiveData<List<FriendRequestModel>> getFriendRequests() {
        return requests;
    }
}
