package com.example.peer;

import Messages.Message;
import Messages.MessageDescriptor;
import Messages.VectorClock;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class Chat {

    private final String chatName;
    private UUID chatUUID;
    private final Set<String> chatParticipants;
    private int lsn;
    private final ArrayList<Message> pending; // crb pending set
    private final ConcurrentLinkedDeque<Message> crbDeliveredMessages;
    private final VectorClock vectorClock;

    public Chat(UUID chatUUID, Set<String> chatParticipants, String peer) {
        this.chatName = String.join(", ", chatParticipants).stripLeading();
        this.chatUUID = chatUUID;
        this.chatParticipants = chatParticipants;
        this.lsn = 0;
        this.pending = new ArrayList<>();
        this.crbDeliveredMessages = new ConcurrentLinkedDeque<>();
        this.vectorClock = new VectorClock(chatParticipants, peer);
    }

    public UUID getChatUUID() {
        return chatUUID;
    }

    public String getChatName() {
        return chatName;
    }


    public ArrayList<Message> getPending() {
        return pending;
    }

    public ConcurrentLinkedDeque<Message> getCrbDeliveredMessages() {
        return crbDeliveredMessages;
    }

    public Set<String> getChatParticipants() {
        return chatParticipants;
    }

    public VectorClock getVectorClockAndIncrement(String peerName) {
        VectorClock clockToSend = new VectorClock(this.vectorClock.toString());
        this.vectorClock.getClock().put(peerName, lsn);
        this.lsn += 1;
        return clockToSend;
    }

    public void addPending(Message message) {
        this.pending.add(message);

        // Don't deal with message if it is not a Message
        if (message.getMessageDescriptor().equals(MessageDescriptor.CREATE_GROUP)) {
            this.pending.remove(message);
            this.crbDeliveredMessages.add(message);
            return;
        }

        // Loop through pending in reverse order
        boolean loopAgain;
        do {
            loopAgain = false;
            ListIterator<Message> li = this.pending.listIterator();
            while (li.hasNext()) {
                li.next();
            }
            while (li.hasPrevious()) {
                Message pendingMessage = li.previous();
                boolean deliverMessage = true;
                VectorClock pendingMessageClock = new VectorClock(pendingMessage.getMessageContent().split("\\|", 2)[0]);

                // Loop through each vector clock entry and make sure all are <= current vector
                for (Map.Entry<String, Integer> entry : pendingMessageClock.getClock().entrySet()) {
                    if (entry.getValue() > this.vectorClock.getClock().get(entry.getKey())) {
                        deliverMessage = false;
                        break;
                    }
                }

                if (deliverMessage) {
                    loopAgain = true;
                    // Deliver message
                    System.out.println("crb-delivering: " + pendingMessage.getMessageContent() + "    vectorclock: " + this.vectorClock);
                    this.vectorClock.increment(message.getOriginalSender());
                    this.pending.remove(pendingMessage);
                    this.crbDeliveredMessages.add(pendingMessage);
                    break;
                }

            }
        } while (loopAgain);



//        boolean shouldDeliver = true;
//        // Loop through all pending messages and check if this new message is <= than all of them
//        do {
//            for (Message pendingMessage : this.pending) {
//                VectorClock vectorClock = new VectorClock(pendingMessage.getMessageContent().split("\\|", 2)[0]);
//
//                for (Map.Entry<String, Integer> entry : vectorClock.getClock().entrySet()) {
//                    String participant = entry.getKey();
//                    int messageClockValue = entry.getValue();
//                    int currentClockValue = messageVectorClock.getClock().getOrDefault(participant, 0);
//                    if (messageClockValue > currentClockValue) {
//                        shouldDeliver = false;
//                        break;
//                    }
//                }
//            }
//
//            if (shouldDeliver) {
//                this.vectorClock.getClock().put(message.getOriginalSender(), this.lsn);
//                this.lsn += 1;
//                this.crbDeliveredMessages.add(message);
//                this.pending.remove(message);
//            }
//        } while (shouldDeliver);

    }

    public void setLsn(int lsn) {
        this.lsn = lsn;
    }

    public void setChatUUID(UUID chatUUID) {
        this.chatUUID = chatUUID;
    }

}
