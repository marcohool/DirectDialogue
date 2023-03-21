package com.example.peer;

import java.util.ArrayList;
import java.util.UUID;

public class Chat {

    private final String chatName;
    private final ArrayList<String> chatParticipants;
    private final ArrayList<StoredMessage> messageHistory;
    private final UUID chatUUID;

    public Chat(String chatName, ArrayList<String> chatParticipants) {
        this.chatName = chatName;
        this.chatParticipants = chatParticipants;
        this.chatUUID = UUID.randomUUID();
        this.messageHistory = new ArrayList<>();
    }

    public String getChatName() {
        return chatName;
    }

    public ArrayList<String> getChatParticipants() {
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
