package com.dominik.Gecko2Chat.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dominik.Gecko2Chat.model.ChatModel;
import com.dominik.Gecko2Chat.model.ContactModel;
import com.dominik.Gecko2Chat.model.response.ConversationSummaryDto;
import com.dominik.Gecko2Chat.model.response.LastMessageDto;
import com.dominik.Gecko2Chat.model.response.PublicUserResponseDto;
import com.dominik.Gecko2Chat.model.response.StartupDto;
import com.dominik.Gecko2Chat.repository.MainRepository;

import java.util.ArrayList;
import java.util.List;

import java.util.Set;

public class MainViewModel extends AndroidViewModel {

    private final MainRepository repository;

    private final MutableLiveData<List<ChatModel>> chatList = new MutableLiveData<>();
    // Using PublicUserResponseDto for contacts, or map to a ContactModel if you prefer
    private final MutableLiveData<List<ContactModel >> contactList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        // Initialize Repository with Application Context
        repository = MainRepository.getInstance(application);
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


        repository.getStartupData(tempObserver, errorMessage);
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

    public LiveData<List<ChatModel>> getChatList() { return chatList; }
    public LiveData<List<ContactModel>> getContactList() { return contactList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
}