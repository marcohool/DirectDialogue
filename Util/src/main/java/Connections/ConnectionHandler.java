package Connections;

import Nodes.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ConnectionHandler extends Thread {
    private final Socket socket;
    private final Node parentNode;
    private InetSocketAddress recipientAddress;

    // When initiating a new connection
    public ConnectionHandler(InetSocketAddress address, Node parentNode) throws IOException {
        this.recipientAddress = address;
        this.socket = new Socket(address.getAddress(), this.recipientAddress.getPort());
        this.parentNode = parentNode;
    }

    // When receiving connection from serverSocket.accept()
    public ConnectionHandler(Socket socket, Node parentNode) {
        this.socket = socket;
        this.parentNode = parentNode;
        this.recipientAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    @Override
    public void run() {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            // Read all incoming messages from the socket until connection is closed
            String message;

            do {
                message = reader.readLine();
                String[] messageSplit = message.split(" ");

                // Register connection with user
                String username = messageSplit[0];
                parentNode.activeConnections.put(username, this);

                // Set recipient address as serverIP of incoming message
                String[] address = messageSplit[1].split(":");
                this.recipientAddress = new InetSocketAddress(address[0], Integer.parseInt(address[1]));

                // Assign message to be handled by the node
                parentNode.handleMessage(message, this);

            } while (true);

        } catch (Exception e) {
            // Connection closed - remove connection
            e.printStackTrace();
            parentNode.activeConnections.values().remove(this);

            // Close socket
            try {
                this.socket.close();
                System.out.println("Connection closed");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        try {
            System.out.println("SENDING MESSAGE : '" + message + "' - FROM " + this.parentNode.getAddress());
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
