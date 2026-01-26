package com.dominik.Gecko2Chat.utils.mapper;

import com.dominik.Gecko2Chat.database.entities.FriendRequestEntity;
import com.dominik.Gecko2Chat.model.FriendRequestModel;

public final class FriendRequestMapper {

    public static FriendRequestModel mapEntityToFriendRequestModel(FriendRequestEntity entity) {
        return new FriendRequestModel(
                entity.id,
                entity.initiatorDisplayName,
                entity.initiatorUsername,
                entity.initiatorUserProfileImage,
                entity.createdAt
        );
    }
}
