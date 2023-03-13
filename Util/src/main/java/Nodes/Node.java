package Nodes;

import Connections.ConnectionHandler;
import Connections.Listener;
import Messages.Message;
import Messages.MessageDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Node implements INode {
    private InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
    private InetSocketAddress serverAddress;
    public final HashMap<String, ConnectionHandler> activeConnections = new HashMap<>();
    private final HashMap<String, ArrayList<Message>> messageHistory = new HashMap<>();
    protected final HashMap<String, String> messageQueue = new HashMap<>();
    private String name;

    // Starting a server node
    public Node(String name, InetSocketAddress address) {
        this.name = name;
        this.address = address;
        startListener();
    }

    // Starting a client node
    public Node(String name) {
        this.name = name;
        startListener();

    }

    private void startListener() {
        // Start listener thread
        try {
            Thread thread = new Thread(new Listener(this.address, this));
            thread.start();
        } catch (IOException e) {
            System.err.println("Error listening for incoming connections: " + e.getMessage());
        }
    }

    public void sendMessage(MessageDescriptor messageDescriptor, String messageContent, int ttl, String destinationUsername) {

        // Check if connection with user exists
        ConnectionHandler connection = this.activeConnections.get(destinationUsername);

        // If not, QUERY local neighbours for username IP address
        if (connection == null) {
            for (ConnectionHandler connections : this.activeConnections.values()) {

                // Add message to queue
                this.messageQueue.put(destinationUsername, messageContent);

                // Don't send QUERY to server
                if (!connections.getRecipientAddress().equals(this.serverAddress)) {
                    sendMessage(MessageDescriptor.QUERY, destinationUsername, 3, connections);
                }
            }

        } else {
            sendMessage(messageDescriptor, messageContent, ttl, connection);
        }
    }

    public void sendMessage(MessageDescriptor messageDescriptor, String messageContent, int ttl, InetSocketAddress destinationAddress) {
        try {
            // Check if there is an existing connection with recipient
            ConnectionHandler connection = searchForEstablishedConnection(destinationAddress);

            // If there is no established connection create a new one
            if (connection == null) {
                connection = new ConnectionHandler(destinationAddress, this);

                // Listen to incoming messages on this channel
                connection.start();

                sendMessage(messageDescriptor, messageContent, ttl, connection);

            }
        } catch (IOException e) {
            System.out.println("-- Connection refused to " + destinationAddress + " --");
            e.printStackTrace();
        }
    }

    public void sendMessage(MessageDescriptor messageDescriptor, String messageContent, int ttl, ConnectionHandler connectionHandler) {
        // Set message details
        Message message = new Message(this.name, this.address.getAddress(), this.address.getPort(), ttl, messageDescriptor, messageContent);

        // Send message across the connection
        connectionHandler.sendMessage(message);

        // Register sent message
        addMessageHistory(getUsernameFromAddress(this.activeConnections, connectionHandler).toString(), message);
    }

    private static <String, ConnectionHandler> Set<String> getUsernameFromAddress(Map<String, ConnectionHandler> map, ConnectionHandler value) {
        return map.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), value))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    protected ConnectionHandler searchForEstablishedConnection(InetSocketAddress address) {

        try {
            for (ConnectionHandler conn : this.activeConnections.values()) {
                if (conn.getRecipientAddress().equals(address)) {
                    return conn;
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("java.util.ConcurrentModificationException");
            searchForEstablishedConnection(address);
        }
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public void setServerAddress(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public HashMap<String, ConnectionHandler> getActiveConnections() {
        return activeConnections;
    }

    public String getName() {
        return name;
    }

    public void addMessageHistory(String recipient, Message message) {
        if (this.messageHistory.containsKey(recipient)) {
            this.messageHistory.get(recipient).add(message);
        } else {
            this.messageHistory.put(recipient, new ArrayList<>(List.of(message)));
        }
    }

}
