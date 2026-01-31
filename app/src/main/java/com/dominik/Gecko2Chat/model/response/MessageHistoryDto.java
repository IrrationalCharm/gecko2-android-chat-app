package com.dominik.Gecko2Chat.model.response;

import java.time.Instant;
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

        Instant lastDeliveredMessage,  //The other user of the requester
        Instant lastReadMessage,        //The other user of the requester

        int pageNumber,
        int totalPages,
        boolean isLastPage
) {
}