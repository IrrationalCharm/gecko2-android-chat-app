package com.dominik.Gecko2Chat.enums;

//Websocket messages
public enum MessageType {
    //Outgoing messages
    CHAT_MESSAGE_CLIENT, //Outgoing message to server
    READ_RECEIPT_CLIENT, //Client read a message
    DELIVERY_RECEIPT_CLIENT, //Confirmation that client received a message
    TYPING_STATUS_CLIENT, //Client is typing


    //Incoming messages
    CHAT_MESSAGE_SERVER, //Incoming message from server
    FRIEND_REQUEST_SERVER, //Incoming friend request from server
    MESSAGE_DELIVERED_SERVER, //Acknowledgment that recipient received message
    MESSAGE_READ_SERVER, //Acknowledgment that recipient read message
    MESSAGE_SENT_SERVER //Acknowledgment that server received message
}
