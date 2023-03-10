package Messages;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.UUID;

public class Message {

    // Message header
    private final String sourceUsername;
    private final InetAddress sourceSocketAddress;
    private final int sourcePort;
    private final UUID uuid;
    private final int ttl;
    private final MessageDescriptor messageDescriptor;
    private final String messageContent;

    public Message(String message) throws UnknownHostException {
        String[] messageSplit = message.split(" ");

        this.sourceUsername = messageSplit[0];
        this.sourceSocketAddress = InetAddress.getByName(messageSplit[1].replace("/", ""));
        this.sourcePort = Integer.parseInt(messageSplit[2]);
        this.uuid = UUID.fromString(messageSplit[3]);
        this.ttl = Integer.parseInt(messageSplit[4]);
        this.messageDescriptor = MessageDescriptor.valueOf(messageSplit[5]);
        this.messageContent = String.join(" ", Arrays.copyOfRange(messageSplit, 6, messageSplit.length));

    }

    public Message(String sourceUsername, InetAddress sourceSocketAddress, int sourcePort, int ttl, MessageDescriptor messageDescriptor, String messageContent) {
        this.sourceUsername = sourceUsername;
        this.sourceSocketAddress = sourceSocketAddress;
        this.sourcePort = sourcePort;
        this.uuid = UUID.randomUUID();
        this.ttl = ttl;
        this.messageDescriptor = messageDescriptor;
        this.messageContent = messageContent;
    }

    @Override
    public String toString() {
        return this.sourceUsername + " " + this.sourceSocketAddress + " " + this.sourcePort + " " + this.uuid + " " + this.ttl + " " + this.messageDescriptor + " " + this.messageContent;
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

    public int getTtl() {
        return ttl;
    }

    public MessageDescriptor getMessageDescriptor() {
        return messageDescriptor;
    }

    public String getMessageContent() {
        return messageContent;
    }
}
