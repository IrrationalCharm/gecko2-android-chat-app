package com.dominik.Gecko2Chat.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.Instant;
import java.util.UUID;

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
    public String initiatorUsername;
    public String initiatorDisplayName;
    public String initiatorUserProfileImage;

    public Instant createdAt;

    public FriendRequestEntity() {

    }

    public FriendRequestEntity(@NonNull String id, String requestType, String senderId, String receiverId, String initiatorUsername, String initiatorDisplayName, String initiatorUserProfileImage, Instant createdAt) {
        this.id = id;
        this.requestType = requestType;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.initiatorUsername = initiatorUsername;
        this.initiatorDisplayName = initiatorDisplayName;
        this.initiatorUserProfileImage = initiatorUserProfileImage;
        this.createdAt = createdAt;
    }
}