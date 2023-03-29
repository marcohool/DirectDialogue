package com.example.peer;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class Chat {

    private final String chatName;
    private final Set<String> chatParticipants;
    private final ArrayList<StoredMessage> messageHistory;
    private final UUID chatUUID;

    public Chat(Set<String> chatParticipants) {
        this.chatName = String.join(", ", chatParticipants);
        this.chatParticipants = chatParticipants;
        this.chatUUID = null;
        this.messageHistory = new ArrayList<>();
    }

    public String getChatName() {
        return chatName;
    }

    public Set<String> getChatParticipants() {
        return chatParticipants;
    }

    public UUID getChatUUID() {
        return chatUUID;
    }

    public void addMessageHistory(StoredMessage messageHistory) {
        this.messageHistory.add(messageHistory);
    }

    public ArrayList<StoredMessage> getMessageHistory() {
        return messageHistory;
    }
}
