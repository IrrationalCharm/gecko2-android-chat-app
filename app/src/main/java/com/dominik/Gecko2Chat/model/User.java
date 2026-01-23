package com.dominik.Gecko2Chat.model;


public record User(String internalId,
                   String providerId,
                   String username,
                   String displayName,
                   String email,
                   String mobileNumber,
                   String profileBio,
                   String profileImageUrl,
                   boolean isOnboarded) {

}
