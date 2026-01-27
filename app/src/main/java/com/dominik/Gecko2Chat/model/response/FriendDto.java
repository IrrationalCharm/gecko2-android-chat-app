package com.dominik.Gecko2Chat.model.response;

import java.io.Serializable;

public record FriendDto(
        String internalId,
        String username,
        String displayName,
        String profileBio,
        String profileImageUrl) implements Serializable {
}
