package com.dominik.Gecko2Chat.model.response;

import java.util.List;


/**
 * this is the messages stored and provided by messaging-persistence-service
 * @param conversationId
 * @param messages
 * @param pageNumber
 * @param totalPages
 * @param isLastPage
 */
public record MessageHistoryDto(
        String conversationId,
        List<MessageDto> messages,

        int pageNumber,
        int totalPages,
        boolean isLastPage
) {
}