package com.dominik.Gecko2Chat.utils.mapper;

import com.dominik.Gecko2Chat.database.entities.FriendEntity;
import com.dominik.Gecko2Chat.model.response.FriendDto;

public abstract class FriendMapper {

    public static FriendEntity mapFriendDtoToEntity(FriendDto friend) {
        return new FriendEntity(
                friend.internalId(),
                friend.username(),
                friend.displayName(),
                friend.profileBio(),
                friend.profileImageUrl());
    }

    public static FriendDto mapFriendEntityToDto(FriendEntity friend) {
        return new FriendDto(
                friend.internalId,
                friend.username,
                friend.displayName,
                friend.profileBio,
                friend.profileImageUrl);
    }
}
