package com.dominik.Gecko2Chat.model.response;

import java.io.Serializable;
import java.util.UUID;

public record PublicUserResponseDto(
        String internalId,
        String username,
        String displayName,
        String profileBio,
        String profileImageUrl) implements Serializable {
}
