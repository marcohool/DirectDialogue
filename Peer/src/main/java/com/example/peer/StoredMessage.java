package com.example.peer;

import Messages.Message;

import java.time.LocalDateTime;
import java.util.UUID;

public class StoredMessage {

    private final UUID uuid;
    private final UUID chatUUID;
    private final String sender;
    private final String messageContent;
    private final LocalDateTime dateTime;
    private boolean delivered;
    private boolean failed;

    public StoredMessage(UUID uuid, UUID chatUUID, String sender, String messageContent, LocalDateTime dateTime) {
        this.uuid = uuid;
        this.chatUUID = chatUUID;
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

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public UUID getChatUUID() {
        return chatUUID;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof StoredMessage message)) {
            return false;
        }

        return this.uuid == message.getUuid();
    }
}
