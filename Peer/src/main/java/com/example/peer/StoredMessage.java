package com.example.peer;

import java.time.LocalDateTime;

public class StoredMessage {

    private final String chatUsername;
    private final String sender;
    private final String messageContent;
    private final LocalDateTime dateTime;


    public StoredMessage(String chatUsername, String sender, String messageContent, LocalDateTime dateTime) {
        this.chatUsername = chatUsername;
        this.sender = sender;
        this.messageContent = messageContent;
        this.dateTime = dateTime;
    }

    public String getSender() {
        return sender;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public String getChatUsername() {
        return chatUsername;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
}
