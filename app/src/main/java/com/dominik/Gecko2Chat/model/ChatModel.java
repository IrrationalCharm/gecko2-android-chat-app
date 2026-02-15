package com.dominik.Gecko2Chat.model;

import java.time.Instant;

public record ChatModel(String name, String friendId, String lastMessage, long unreadCount, Instant timestamp, String avatar) {
}
