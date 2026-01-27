package com.dominik.Gecko2Chat.utils.mapper;

import com.dominik.Gecko2Chat.database.entities.FriendRequestEntity;
import com.dominik.Gecko2Chat.model.FriendRequestModel;
import com.dominik.Gecko2Chat.model.response.websocket.FriendRequestDto;

import java.time.Instant;

public final class FriendRequestMapper {

    public static FriendRequestEntity mapFriendRequestDtoToEntity(FriendRequestDto dto) {
        return new FriendRequestEntity(
                String.valueOf(dto.id()),
                "INCOMING",
                dto.initiatorId().toString(),
                dto.receiverId().toString(),
                dto.initiatorUsername(),
                dto.initiatorDisplayName(),
                dto.initiatorUrlProfileImage(),
                Instant.ofEpochSecond(dto.createdAt())
        );
    }

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
