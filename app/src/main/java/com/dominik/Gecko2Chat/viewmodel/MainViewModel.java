package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dominik.Gecko2Chat.model.ChatModel;
import com.dominik.Gecko2Chat.model.ContactModel;
import com.dominik.Gecko2Chat.model.response.ConversationSummaryDto;
import com.dominik.Gecko2Chat.model.response.PublicUserResponseDto;
import com.dominik.Gecko2Chat.model.response.StartupDto;
import com.dominik.Gecko2Chat.model.response.websocket.ChatMessageDto;
import com.dominik.Gecko2Chat.model.response.websocket.PrivateMessage;
import com.dominik.Gecko2Chat.model.response.websocket.adapter.PrivateMessageDeserializer;
import com.dominik.Gecko2Chat.repository.MainRepository;
import com.dominik.Gecko2Chat.repository.MessageRepository;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import java.util.Optional;
import java.util.Set;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainViewModel extends AndroidViewModel {

    //private final MainRepository repository;
    private final MessageRepository messageRepository;


    private final MutableLiveData<List<ChatModel>> chatList = new MutableLiveData<>();
    // Using PublicUserResponseDto for contacts, or map to a ContactModel if you prefer
    private final MutableLiveData<List<ContactModel >> contactList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(PrivateMessage.class, new PrivateMessageDeserializer())
            .create();


    public MainViewModel(@NonNull Application application) {
        super(application);
        // Initialize Repository with Application Context
        //repository = MainRepository.getInstance(application);
        messageRepository = MessageRepository.getInstance(application);


    }




    private void handleIncomingMessage(String json) {
        try {
            PrivateMessage message = gson.fromJson(json, PrivateMessage.class);
            updateChatListWithNewMessage(message);
        } catch (Exception e) {
            Log.e("MainViewModel", "Error parsing message", e);
        }
    }


    private void updateChatListWithNewMessage(PrivateMessage privateMessage) {
        if (privateMessage instanceof ChatMessageDto message) {
            List<ChatModel> currentList = chatList.getValue();
            if (currentList == null) currentList = new ArrayList<>();

            // Create a copy to modify (Good practice for DiffUtil later)
            List<ChatModel> newList = new ArrayList<>(currentList);

            // find the chat that corresponds to the sender
            Optional<ChatModel> existingChat = newList.stream()
                    .filter(chat -> chat.friendId().equals(message.senderId()))
                    .findFirst();

            if (existingChat.isPresent()) {
                ChatModel oldChat = existingChat.get();

                // 2. Create updated ChatModel (Records are immutable)
                ChatModel updatedChat = new ChatModel(
                        oldChat.name(),
                        oldChat.friendId(),
                        message.content(),
                        message.timestamp(),
                        oldChat.avatar()
                );

                // 3. Move to TOP of the list
                newList.remove(oldChat);
                newList.add(0, updatedChat);

                // ui updates automatically here
                chatList.setValue(newList);
            } else {
                // TODO: Handle case where it's a new unknown sender
                Log.e("MainViewModel", "Unknown sender: " + message.senderId());
            }
        }


    }


    public void fetchStartupData() {
        isLoading.setValue(true);

        MutableLiveData<StartupDto> tempObserver = new MutableLiveData<>();
        tempObserver.observeForever(dto -> {
            isLoading.setValue(false);
            if (dto != null) {
                mapDataToUi(dto);
            }
        });

        //repository.getStartupData(tempObserver, errorMessage);
    }

    private void mapDataToUi(StartupDto dto) {
        List<ChatModel> uiChats = new ArrayList<>();
        List<ContactModel> uiContacts = new ArrayList<>();

        // Recent Chats
        if (dto.conversationSummary() != null) {
            for (ConversationSummaryDto conv : dto.conversationSummary()) {
                var lastMessage = conv.lastMessage();

                // Gets the friend userId from the participants list
                String friendUserId = conv.participants().stream().filter(
                        p -> !p.equals(dto.userDto().internalId())).findFirst().orElse(null);


                PublicUserResponseDto friend = findFriendById(dto.friendsList(), friendUserId);
                if (friend == null) continue;

                uiChats.add(new ChatModel(
                        friend.displayName(),
                        friendUserId,
                        lastMessage.content(),
                        null,
                        null// Avatar
                ));
            }
        }

        //Contacts
        for (PublicUserResponseDto friend : dto.friendsList()) {
            uiContacts.add( new ContactModel(
                    friend.displayName(),
                    "Last seen recently",
                    null // Avatar
                )
            );
        }


        chatList.setValue(uiChats);
        contactList.setValue(uiContacts);
    }

    private PublicUserResponseDto findFriendById(Set<PublicUserResponseDto> publicUserResponseDtos, String userId) {
        return publicUserResponseDtos.stream()
                .filter(dto -> dto.internalId().equals(userId))
                .findFirst()
                .orElse(null);
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }

    public LiveData<List<ChatModel>> getChatList() { return chatList; }
    public LiveData<List<ContactModel>> getContactList() { return contactList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
}