package com.dominik.Gecko2Chat.model;

public record OnBoardingRequestDto(
        String username,
        String displayName,
        String mobileNumber,
        String profileBio,
        String profileImageUrl
) {
}