package com.example.peer;

import java.time.LocalDateTime;
import java.util.UUID;

public class StoredMessage {

    private final UUID uuid;
    private final String chatUsername;
    private final String sender;
    private final String messageContent;
    private final LocalDateTime dateTime;
    private boolean delivered;
    private boolean failed;

    public StoredMessage(UUID uuid, String chatUsername, String sender, String messageContent, LocalDateTime dateTime) {
        this.uuid = uuid;
        this.chatUsername = chatUsername;
        this.sender = sender;
        this.messageContent = messageContent;
        this.dateTime = dateTime;
        this.delivered = false;
        this.failed = false;
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

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public UUID getUuid() {
        return uuid;
    }
}
