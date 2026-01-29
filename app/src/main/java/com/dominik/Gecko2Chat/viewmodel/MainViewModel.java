package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.dominik.Gecko2Chat.database.entities.FriendEntity;
import com.dominik.Gecko2Chat.database.entities.MessageEntity;
import com.dominik.Gecko2Chat.model.ChatModel;
import com.dominik.Gecko2Chat.model.ContactModel;
import com.dominik.Gecko2Chat.model.User;
import com.dominik.Gecko2Chat.repository.FriendRequestRepository;
import com.dominik.Gecko2Chat.repository.MainRepository;
import com.dominik.Gecko2Chat.repository.MessageRepository;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.WebSocketManager;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainViewModel extends AndroidViewModel {

    private final FriendRequestRepository friendRequestRepository;
    private final MessageRepository messageRepository;
    private final MainRepository mainRepository;

    private final UserManager userManager;

    private SharedPreferences.OnSharedPreferenceChangeListener userPrefsListener;

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MediatorLiveData<List<ChatModel>> chatList = new MediatorLiveData<>();
    private LiveData<List<ContactModel >> contactList = new MutableLiveData<>();
    private final LiveData<Integer> friendRequestsCount;

    //Monitor connection status
    private final MutableLiveData<WebSocketManager.ConnectionStatus> connectionStatus = new MutableLiveData<>();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();


    public MainViewModel(@NonNull Application application) {
        super(application);
        // Initialize Repository with Application Context
        mainRepository = MainRepository.getInstance(application);
        messageRepository = MessageRepository.getInstance(application);
        friendRequestRepository = FriendRequestRepository.getInstance(application);
        userManager = UserManager.getInstance(application);
        friendRequestsCount = friendRequestRepository.getFriendRequestsCount();

        monitorConnectionStatus();
        monitorUserPreferences();

        LiveData<List<FriendEntity>> friendsList = mainRepository.getFriends();
        LiveData<List<MessageEntity>> recentMessageEntities = messageRepository.getRecentChats();

        //Listens to changes in friends and messages db, and runs updateChatList if any change event is triggered
        chatList.addSource(friendsList, friends -> updateChatList(recentMessageEntities.getValue(), friendsList.getValue()));
        chatList.addSource(recentMessageEntities, chats -> updateChatList(recentMessageEntities.getValue(), friendsList.getValue()));

        contactList = Transformations.map(friendsList, entities -> entities.stream()
                .map(entity -> new ContactModel(entity.internalId, entity.username, entity.displayName, "Recently online", entity.profileImageUrl))
                        .toList());

        loadCurrentUser();
    }

    private void monitorUserPreferences() {
        // Listen to changes in SharedPreferences related to the User object
        userPrefsListener = (sharedPreferences, key) -> {
            //Check if the changed key is relevant to the User object
            if (userManager.isUserKey(key)) {
                loadCurrentUser();
            }
        };
        userManager.registerOnSharedPreferenceChangeListener(userPrefsListener);
    }


    private void loadCurrentUser() {
        User user = userManager.getUser();
        currentUser.setValue(user);
    }

    private void updateChatList(List<MessageEntity> chats, List<FriendEntity> friends) {
        if (chats == null || chats.isEmpty()) return;
        if (friends == null || friends.isEmpty()) return;

        List<ChatModel> currentList = chatList.getValue();
        if (currentList == null) currentList = new ArrayList<>();

        // Create a copy to modify (Good practice for DiffUtil later)
        List<ChatModel> newUiList = new ArrayList<>(currentList);

        String currentUserId = userManager.getUser().internalId();

        for (MessageEntity msg : chats) {
            String otherUserId = msg.senderId.equals(currentUserId) ? msg.recipientId : msg.senderId;

            // Find friend details from the cached list
            String name = "Unknown";
            String avatar = null;

            for (FriendEntity f : friends) {
                if (f.internalId.equals(otherUserId)) {
                    name = f.displayName;
                    avatar = f.profileImageUrl;
                    break;

                }
            }

            newUiList.removeIf(chatModel -> chatModel.friendId().equals(otherUserId));

            newUiList.add(new ChatModel(name, otherUserId, msg.content, msg.timestamp.toString(), avatar));
        }

        chatList.setValue(newUiList);
    }


    private void monitorConnectionStatus() {
        // 4. Update the LiveData
        Disposable d = WebSocketManager.getInstance().getConnectionStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // Ensure we are on UI thread for LiveData
                .subscribe(
                        connectionStatus::setValue,
                        throwable -> {
                            // Handle error if needed, usually just log it
                            Log.e("ChatViewModel", "Error observing connection status", throwable);
                        }
                );

        compositeDisposable.add(d);
    }


    public LiveData<List<ChatModel>> getChatList() { return chatList; }
    public LiveData<Integer> getFriendRequestsCount() {return friendRequestsCount; }
    public LiveData<List<ContactModel>> getContactList() { return contactList; }
    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<WebSocketManager.ConnectionStatus> getConnectionStatus() {return connectionStatus;}

    @Override
    protected void onCleared() {
        super.onCleared();
        userManager.unregisterOnSharedPreferenceChangeListener(userPrefsListener);
        compositeDisposable.clear();
    }
}