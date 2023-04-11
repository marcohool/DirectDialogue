package Messages;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class Message {

    // Message header
    private final String sourceUsername;
    private final InetAddress sourceSocketAddress;
    private final int sourcePort;
    private final UUID uuid;
    private final UUID chatUUID;
    private int ttl;
    private LocalDateTime dateTime;
    private final MessageDescriptor messageDescriptor;
    private final String messageContent;

    public Message(String message) throws UnknownHostException {
        String[] messageSplit = message.split(" ");

        this.sourceUsername = messageSplit[0];
        this.sourceSocketAddress = InetAddress.getByName(messageSplit[1].replace("/", ""));
        this.sourcePort = Integer.parseInt(messageSplit[2]);
        this.uuid = UUID.fromString(messageSplit[3]);
        if (!Objects.equals(messageSplit[4], "null")) {
            this.chatUUID = UUID.fromString(messageSplit[4]);
        } else {
            this.chatUUID = null;
        }
        this.ttl = Integer.parseInt(messageSplit[5]);
        this.dateTime = LocalDateTime.parse(messageSplit[6]);
        this.messageDescriptor = MessageDescriptor.valueOf(messageSplit[7]);
        this.messageContent = String.join(" ", Arrays.copyOfRange(messageSplit, 8, messageSplit.length));

    }

    public Message(String sourceUsername, InetAddress sourceSocketAddress, int sourcePort, UUID uuid, UUID chatUUID, int ttl, MessageDescriptor messageDescriptor, String messageContent) {
        this.sourceUsername = sourceUsername;
        this.sourceSocketAddress = sourceSocketAddress;
        this.sourcePort = sourcePort;
        this.uuid = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
        this.chatUUID = chatUUID;
        this.ttl = ttl;
        this.dateTime = LocalDateTime.now();
        this.messageDescriptor = messageDescriptor;
        this.messageContent = messageContent;
    }

    @Override
    public String toString() {
        return this.sourceUsername + " " + this.sourceSocketAddress + " " + this.sourcePort + " " + this.uuid + " " + this.chatUUID + " " + this.ttl + " " + this.dateTime + " " + this.messageDescriptor + " " + this.messageContent;
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

    public UUID getUuid() {
        return uuid;
    }

    public UUID getChatUUID() {
        return chatUUID;
    }

    public int getTtl() {
        return ttl;
    }

    public MessageDescriptor getMessageDescriptor() {
        return messageDescriptor;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void decrementTtl() {
        this.ttl = this.ttl - 1;
    }

    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(this.sourceSocketAddress, sourcePort);
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime setDateTime) {
        this.dateTime = setDateTime;
    }

}
