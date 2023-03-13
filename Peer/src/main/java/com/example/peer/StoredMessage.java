package com.example.peer;

import java.time.LocalDateTime;

public class StoredMessage {

    private final String chatUsername;
    private final String sourceUsername;
    private final String messageContent;
    private final LocalDateTime currentDateTime;


    public StoredMessage(String chatUsername, String sourceUsername, String messageContent) {
        this.chatUsername = chatUsername;
        this.sourceUsername = sourceUsername;
        this.messageContent = messageContent;
        this.currentDateTime = LocalDateTime.now();
    }

    public String getSourceUsername() {
        return chatUsername;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public String getChatUsername() {
        return chatUsername;
    }

    public LocalDateTime getCurrentDateTime() {
        return currentDateTime;
    }
}
