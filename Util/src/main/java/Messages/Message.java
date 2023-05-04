package Messages;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;

public class Message {

    // Message header
    private String sourceUsername;
    private final InetAddress sourceSocketAddress;
    private final int sourcePort;
    private UUID messageUUID;
    private UUID chatUUID;
    private int ttl;
    private LocalDateTime dateTime;
    private final MessageDescriptor messageDescriptor;
    private String originalSender;
    private String messageContent;

    // Log ACKs received for this message - <Username, delivered?>
    private final HashMap<String, Boolean> delivered;


    // Create message object from set data
    public Message(String sourceUsername, InetAddress sourceSocketAddress, int sourcePort, UUID messageUUID, UUID chatUUID, int ttl, MessageDescriptor messageDescriptor, String messageContent) {
        this.sourceUsername = sourceUsername;
        this.sourceSocketAddress = sourceSocketAddress;
        this.sourcePort = sourcePort;
        this.messageUUID = Objects.requireNonNullElseGet(messageUUID, UUID::randomUUID);
        this.chatUUID = chatUUID;
        this.ttl = ttl;
        this.dateTime = LocalDateTime.now();
        this.messageDescriptor = messageDescriptor;
        this.messageContent = messageContent;
        this.delivered = new HashMap<>();
    }

    // Create message object from incoming string
    public Message(String message) throws UnknownHostException {
        String[] messageSplit = message.split(" ");

        this.sourceUsername = messageSplit[0];
        this.sourceSocketAddress = InetAddress.getByName(messageSplit[1].replace("/", ""));
        this.sourcePort = Integer.parseInt(messageSplit[2]);
        this.messageUUID = UUID.fromString(messageSplit[3]);
        if (!Objects.equals(messageSplit[4], "null")) {
            this.chatUUID = UUID.fromString(messageSplit[4]);
        } else {
            this.chatUUID = null;
        }
        this.ttl = Integer.parseInt(messageSplit[5]);
        this.dateTime = LocalDateTime.parse(messageSplit[6]);
        this.messageDescriptor = MessageDescriptor.valueOf(messageSplit[7]);
        this.originalSender = messageSplit[8];
        this.messageContent = String.join(" ", Arrays.copyOfRange(messageSplit, 9, messageSplit.length));
        this.delivered = new HashMap<>();
    }

    public void registerACK(String user) {
        if (this.delivered.containsKey(user)) {
            this.delivered.put(user, true);
        } else {
            System.out.println("ERROR - Message does not contain recipient " + user);
        }
    }

    public void decrementTtl() {
        this.ttl = this.ttl - 1;
    }

    public String getSourceUsername() {
        return sourceUsername;
    }

    public InetAddress getSourceSocketAddress() {
        return sourceSocketAddress;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public UUID getMessageUUID() {
        return messageUUID;
    }

    public UUID getChatUUID() {
        return chatUUID;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public MessageDescriptor getMessageDescriptor() {
        return messageDescriptor;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public HashMap<String, Boolean> getDelivered() {
        return delivered;
    }

    public String getOriginalSender() {
        return originalSender;
    }

    public void setOriginalSender(String originalSender) {
        this.originalSender = originalSender;
    }

    public void setSourceUsername(String sourceUsername) {
        this.sourceUsername = sourceUsername;
    }

    public void setParticipants(Set<String> participants) {
        for (String participant : participants) {
            this.delivered.put(participant, false);
        }
    }

    public void setMessageUUID(UUID messageUUID) {
        this.messageUUID = messageUUID;
    }

    public void setChatUUID(UUID chatUUID) {
        this.chatUUID = chatUUID;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    @Override
    public String toString() {
        return this.sourceUsername + " " + this.sourceSocketAddress + " " + this.sourcePort + " " + this.messageUUID + " " + this.chatUUID + " " + this.ttl + " " + this.dateTime + " " + this.messageDescriptor + " " + this.originalSender + " " + this.messageContent;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Message other)) {
            return false;
        }
        return Objects.equals(this.messageUUID, other.messageUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageUUID);
    }

}
