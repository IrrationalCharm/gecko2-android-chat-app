package com.dominik.Gecko2Chat.model.response;

public record UserDto(
        String internalId,
        String providerId,
        String username,
        String displayName,
        String email,
        String mobileNumber,
        String profileBio,
        String profileImageUrl) {
}