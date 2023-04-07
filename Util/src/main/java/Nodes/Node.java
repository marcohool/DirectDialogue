package Nodes;

import Connections.ConnectionHandler;
import Connections.Listener;
import Messages.Message;
import Messages.MessageDescriptor;
import Messages.Query;
import Messages.QueryDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

public abstract class Node implements INode {
    protected InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
    private InetSocketAddress serverAddress;
    public final ConcurrentHashMap<String, ConnectionHandler> activeConnections = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> messageQueue = new ConcurrentHashMap<>();
    protected final ArrayList<UUID> seenMessageUUIDs = new ArrayList<>();
    protected Message lastReceivedMessage = null;
    private String name;

    // Store QUERY requests that have been echoed and waiting for QUERYHIT
    protected final HashMap<String, ArrayList<String>> receivedQueryRequests = new HashMap<>();

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

    public void sendMessage(Message message, String destinationUsername) {
        if (this.activeConnections.containsKey(destinationUsername)) {
            this.activeConnections.get(destinationUsername).sendMessage(message);
        } else {
            System.out.println("connection dont exist matey");
        }
    }

    public void sendMessage(MessageDescriptor messageDescriptor, String messageContent, UUID chatUUID, UUID messageUUID, int ttl, String destinationUsername) {
        Message sentMessage;

        // Check if connection with user exists
        ConnectionHandler connection = this.activeConnections.get(destinationUsername);

        // If not, QUERY local neighbours for username IP address
        if (connection == null) {

            // Add message to queue
            sentMessage = new Message(this.name, this.address.getAddress(), this.address.getPort(), messageUUID, chatUUID, ttl, messageDescriptor, messageContent);
            if (this.messageQueue.containsKey(destinationUsername)) {
                this.messageQueue.get(destinationUsername).add(sentMessage);
            } else {
                this.messageQueue.put(destinationUsername, new ConcurrentLinkedQueue<>(Collections.singleton(sentMessage)));
            }

            for (ConnectionHandler connections : this.activeConnections.values()) {
                // Don't send QUERY to server
                if (!connections.getRecipientAddress().equals(this.serverAddress)) {
                    sendMessage(MessageDescriptor.QUERY, new Query(QueryDescriptor.USER, destinationUsername).toString(), 3, null, null, connections);
                }
            }

        } else {
            sendMessage(messageDescriptor, messageContent, ttl, messageUUID, chatUUID, connection);
        }

    }

    public void sendMessage(MessageDescriptor messageDescriptor, String messageContent, int ttl, UUID messageUUID, UUID chatUUID,  InetSocketAddress destinationAddress) {

        try {
            // Check if there is an existing connection with recipient
            ConnectionHandler connection = searchForEstablishedConnection(destinationAddress);

            // If there is no established connection create a new one
            if (connection == null) {
                connection = new ConnectionHandler(destinationAddress, this);

                // Listen to incoming messages on this channel
                connection.start();

                sendMessage(messageDescriptor, messageContent, ttl, messageUUID, chatUUID, connection);

            }
        } catch (IOException e) {
            System.out.println("-- Connection refused to " + destinationAddress + " --");
            e.printStackTrace();
        }
    }

    public void sendMessage(MessageDescriptor messageDescriptor, String messageContent, int ttl, UUID messageUUID, UUID chatUUID, ConnectionHandler connectionHandler) {
        // Set message details
        Message message = new Message(this.name, this.address.getAddress(), this.address.getPort(), messageUUID, chatUUID, ttl, messageDescriptor, messageContent);

        // Send message across the connection
        connectionHandler.sendMessage(message);

    }

    protected synchronized ConnectionHandler searchForEstablishedConnection(InetSocketAddress address) {

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

    public void handleDisconnect(ConnectionHandler connectionHandler) {
        String username = null;

        for ( Map.Entry<String, ConnectionHandler> entry : this.activeConnections.entrySet()) {
            if (entry.getValue().equals(connectionHandler)) {
                username = entry.getKey();
                break;
            }
        }

        if (username == null) {
            return;
        }

        this.messageQueue.remove(username);
        this.receivedQueryRequests.remove(username);
        activeConnections.values().remove(connectionHandler);
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

    public ConcurrentHashMap<String, ConnectionHandler> getActiveConnections() {
        return activeConnections;
    }

    public String getName() {
        return name;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> getMessageQueue() {
        return messageQueue;
    }

    public ArrayList<UUID> getSeenMessageUUIDs() {
        return seenMessageUUIDs;
    }
}
