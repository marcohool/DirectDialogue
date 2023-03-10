package Nodes;

import Connections.ConnectionHandler;
import Connections.Listener;
import Messages.Message;
import Messages.MessageDescriptor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

public abstract class Node implements INode {
    private InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
    public final HashMap<String, ConnectionHandler> activeConnections = new HashMap<>();
    private final String name;

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

    public void sendMessage(MessageDescriptor messageDescriptor, String messageContent, int ttl, InetSocketAddress address, ConnectionHandler connection) {
        try {
            if (connection == null) {
                // Check if there is an existing connection with recipient
                connection = searchForEstablishedConnection(address);

                // If there is no established connection create a new one
                if (connection == null) {
                    connection = new ConnectionHandler(address, this);

                    // Listen to incoming messages on this channel
                    connection.start();
                }

            }

            Message message = new Message(this.name, this.address.getAddress(), this.address.getPort(), ttl, messageDescriptor, messageContent);

            // Send message across the connection
            connection.sendMessage(message);

        } catch (IOException e) {
            System.out.println("-- Connection refused to " + address + " --");
            e.printStackTrace();
        }


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

    public void setAddress(InetSocketAddress address) {
        this.address = address;
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
}
