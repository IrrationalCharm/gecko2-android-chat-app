package com.dominik.Gecko2Chat.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "friends")
public class FriendEntity {

    @NonNull
    @PrimaryKey
    public String internalId;
    public String username;
    public String displayName;
    public String profileBio;
    public String profileImageUrl;


    // Constructor to map from your API DTO
    public FriendEntity(String internalId, String username, String displayName, String profileBio, String profileImageUrl ) {
        this.internalId = internalId;
        this.username = username;
        this.displayName = displayName;
        this.profileBio = profileBio;
        this.profileImageUrl = profileImageUrl;

    }
}