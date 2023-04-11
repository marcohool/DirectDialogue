package com.example.peer;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Chat {

    private final String chatName;
    private final Set<String> chatParticipants;
    private final String peerName;
    private final ArrayList<StoredMessage> messageHistory;
    private final UUID chatUUID;

    public Chat(UUID chatUUID, Set<String> chatParticipants, String peerName) {
        chatParticipants.add(peerName);

        this.chatName = chatParticipants.stream().filter(s -> !s.trim().equals(peerName)).collect(Collectors.joining(", ")).stripLeading();
        this.chatParticipants = chatParticipants.stream().map(String::trim).collect(Collectors.toSet());
        this.peerName = peerName;
        this.chatUUID = chatUUID;
        this.messageHistory = new ArrayList<>();
    }

    public Chat(Set<String> chatParticipants, String peerName) {
        chatParticipants.add(peerName);

        this.chatName = chatParticipants.stream().filter(s -> !s.equals(peerName)).collect(Collectors.joining(", ")).stripLeading();
        this.chatParticipants = chatParticipants.stream().map(String::trim).collect(Collectors.toSet());
        this.peerName = peerName;
        this.chatUUID = null;
        this.messageHistory = new ArrayList<>();
    }

    public String getChatName() {
        return chatName;
    }

    public Set<String> getChatParticipants() {
        Set<String> returnedParticipants = new java.util.HashSet<>(Set.copyOf(this.chatParticipants));
        returnedParticipants.remove(peerName);
        return returnedParticipants;
    }

    public Set<String> getAllChatParticipants() {
        return chatParticipants;
    }

    public String getPeerName() {
        return peerName;
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
