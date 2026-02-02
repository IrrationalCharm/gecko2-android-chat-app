package com.dominik.Gecko2Chat.model;

public record ChatModel(String name, String friendId, String lastMessage, long unreadCount, String date, String avatar) {
}
