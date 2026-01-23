package com.dominik.Gecko2Chat.utils.mapper;

import com.dominik.Gecko2Chat.model.User;
import com.dominik.Gecko2Chat.model.response.UserDto;

public final class UserMapper {

    public static UserDto mapUserToDto(User user) {
        return new UserDto(
                user.internalId(),
                user.providerId(),
                user.username(),
                user.displayName(),
                user.email(),
                user.mobileNumber(),
                user.profileBio(),
                user.profileImageUrl()
        );
    }

    public static User mapDtoToUser(UserDto dto, boolean isOnboarded) {
        return new User(
                dto.internalId(),
                dto.providerId(),
                dto.username(),
                dto.displayName(),
                dto.email(),
                dto.mobileNumber(),
                dto.profileBio(),
                dto.profileImageUrl(),
                isOnboarded
        );
    }
}
