package Connections;

import Nodes.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class ConnectionHandler extends Thread {
    private final Socket socket;
    private final Node parentNode;
    private final InetSocketAddress recipientAddress;

    // When initiating a new connection
    public ConnectionHandler(InetSocketAddress address, Node parentNode) throws IOException {
        this.recipientAddress = address;
        this.socket = new Socket(this.recipientAddress.getAddress(), this.recipientAddress.getPort());
        this.parentNode = parentNode;
    }

    // When receiving connection from serverSocket.accept()
    public ConnectionHandler(Socket socket, Node parentNode) {
        this.socket = socket;
        this.parentNode = parentNode;
        this.recipientAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        //this.recipientPort = socket.getPort();
    }

    @Override
    public void run() {
        // Register active connection
        parentNode.activeConnections.add(this);

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            // Read all incoming messages from the socket until connection is closed
            while (true) {
                String message = reader.readLine();

                // Assign message to be handled by the node
                parentNode.handleMessage(message, this);
            }

        } catch (Exception e) {
            // Connection closed - remove connection
            e.printStackTrace();
            parentNode.activeConnections.remove(this);
            System.out.println("Connection closed");
        }
    }

    public void sendMessage(String message) {
        try {
            System.out.println("sending message '" + message);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(message);
        } catch (IOException e) {
            System.out.println("Failed to send message " + message + "\n" + e);
        }
    }

    public InetSocketAddress getRecipientAddress() {
        return this.recipientAddress;
    }
}
