package com.example.peer;

import Messages.Message;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Chat {

    private final String chatName;
    private UUID chatUUID;
    private final Set<String> chatParticipants;
    private final ArrayList<Message> messageHistory;

    public Chat(UUID chatUUID, Set<String> chatParticipants) {
        this.chatName = String.join(", ", chatParticipants).stripLeading();
        this.chatUUID = chatUUID;
        this.chatParticipants = chatParticipants;
        this.messageHistory = new ArrayList<>();
    }

    public UUID getChatUUID() {
        return chatUUID;
    }

    public String getChatName() {
        return chatName;
    }

    public ArrayList<Message> getMessageHistory() {
        return messageHistory;
    }

    public Set<String> getChatParticipants() {
        return chatParticipants;
    }

    public void addMessageHistory(Message message) {
        this.messageHistory.add(message);
    }

    public void setChatUUID(UUID chatUUID) {
        this.chatUUID = chatUUID;
    }

}
