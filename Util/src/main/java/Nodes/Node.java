package Nodes;

import Connections.ConnectionHandler;
import Connections.Listener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public abstract class Node implements INode {
    private String address;
    private int port;
    public final ArrayList<ConnectionHandler> activeConnections = new ArrayList<>();

    // Starting a server node
    public Node(String address, int port) {
        this.address = address;
        this.port = port;
        startListener();
    }

    // Starting a client node
    public Node() {
        startListener();
    }

    private void startListener() {
        // Start listener thread
        try {
            Thread thread = new Thread(new Listener(this.address, this.port, this));
            thread.start();
            //onlinePorts.add(port);
        } catch (IOException e) {
            System.err.println("Error listening for incoming connections: " + e.getMessage());
        }
    }

    public void sendMessage(String message, InetSocketAddress address) {
        try {
            // Check if there is an existing connection with recipient
            ConnectionHandler connection = null;
            for (ConnectionHandler conn : this.activeConnections) {
                if (conn.getRecipientAddress() == address) {
                    connection = conn;
                }
            }

            // If there is no established connection create a new one
            if (connection == null) {
                connection = new ConnectionHandler(address, this);

                // Listen to incoming messages on this channel
                connection.start();
            }

            // Send message across the connection
            connection.sendMessage(message);

        } catch (IOException e) {
            System.out.println("-- Connection refused to " + address + " --");
            e.printStackTrace();
        }


    }


}
