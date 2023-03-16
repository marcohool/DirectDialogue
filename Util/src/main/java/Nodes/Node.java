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
import java.util.stream.Collectors;

public abstract class Node implements INode {
    private InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
    private InetSocketAddress serverAddress;
    public final ConcurrentHashMap<String, ConnectionHandler> activeConnections = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> messageQueue = new ConcurrentHashMap<>();

    // Store QUERY requests that have been echoed and waiting for QUERYHIT
    protected final HashMap<String, ArrayList<String>> receivedQueryRequests = new HashMap<>();

    protected Message lastReceivedMessage = null;
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

    public Message sendMessage(MessageDescriptor messageDescriptor, String messageContent, int ttl, String destinationUsername) {
        Message sentMessage;

        // Check if connection with user exists
        ConnectionHandler connection = this.activeConnections.get(destinationUsername);

        // If not, QUERY local neighbours for username IP address
        if (connection == null) {

            // Add message to queue
            sentMessage = new Message(this.name, this.address.getAddress(), this.address.getPort(), ttl, messageDescriptor, messageContent);
            if (this.messageQueue.containsKey(destinationUsername)) {
                this.messageQueue.get(destinationUsername).add(sentMessage);
            } else {
                System.out.println("adding to queue");
                this.messageQueue.put(destinationUsername, new ConcurrentLinkedQueue<>(Collections.singleton(sentMessage)));
                System.out.println("added -> " + this.messageQueue);
            }

            for (ConnectionHandler connections : this.activeConnections.values()) {
                // Don't send QUERY to server
                if (!connections.getRecipientAddress().equals(this.serverAddress)) {
                    sendMessage(MessageDescriptor.QUERY, new Query(QueryDescriptor.USER, destinationUsername).toString(), 3, connections);
                }
            }

        } else {
            sentMessage = sendMessage(messageDescriptor, messageContent, ttl, connection);
        }

        return sentMessage;
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

    public Message sendMessage(MessageDescriptor messageDescriptor, String messageContent, int ttl, ConnectionHandler connectionHandler) {
        // Set message details
        Message message = new Message(this.name, this.address.getAddress(), this.address.getPort(), ttl, messageDescriptor, messageContent);

        // Send message across the connection
        connectionHandler.sendMessage(message);

        return message;

    }

    private synchronized static <String, ConnectionHandler> Set<String> getUsernameFromAddress(Map<String, ConnectionHandler> map, ConnectionHandler value) {
        return map.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), value))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
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
}
