package com.dominik.Gecko2Chat.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.Instant;

@Entity(tableName = "friend_requests")
public class FriendRequestEntity {

    @PrimaryKey
    @NonNull
    public String id; // The API's ID for this request

    // "INCOMING" or "OUTGOING"
    public String requestType;

    // IDs for logic/API calls
    public String senderId;
    public String receiverId;

    //EMBEDDED UI DATA (The "Other Person")
    public String otherUserId;
    public String otherUserUsername;
    public String otherUserDisplayName;
    public String otherUserProfileImage;

    public Instant createdAt;
}