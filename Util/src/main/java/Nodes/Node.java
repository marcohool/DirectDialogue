package Nodes;

import Connections.ConnectionHandler;
import Connections.Listener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

public abstract class Node implements INode {
    private InetSocketAddress address = new InetSocketAddress("localhost", 0);
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
            //onlinePorts.add(port);
        } catch (IOException e) {
            System.err.println("Error listening for incoming connections: " + e.getMessage());
        }
    }

    public void sendMessage(String message, InetSocketAddress address) {
        try {
            // Check if there is an existing connection with recipient
            ConnectionHandler connection = searchForEstablishedConnection(address);

            // If there is no established connection create a new one
            if (connection == null) {
                connection = new ConnectionHandler(address, this);

                // Listen to incoming messages on this channel
                connection.start();
            }

            // Prepend ServerSocket address to beginning of message
            message = this.address.getAddress().getHostAddress() + ":" + this.address.getPort() + " " + message;

            // Prepend username to beginning of message
            message = this.name + " " + message;

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

    protected ConnectionHandler searchForEstablishedConnection(String username) {

        try {
            for (String name : this.activeConnections.keySet()) {
                if (name.equals(username)) {
                    return this.activeConnections.get(name);
                }
            }

        } catch (ConcurrentModificationException e) {
            System.out.println("java.util.ConcurrentModificationException");
            searchForEstablishedConnection(username);
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
