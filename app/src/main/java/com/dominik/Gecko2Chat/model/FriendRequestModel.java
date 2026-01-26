package com.dominik.Gecko2Chat.model;

import java.time.Instant;

public record FriendRequestModel(String id,
                                 String displayName,
                                 String username,
                                 String profileImageUrl,
                                 Instant createdAt)
{
}
