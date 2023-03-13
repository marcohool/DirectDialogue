package Connections;

import Messages.Message;
import Nodes.Node;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

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
            Message message;

            do {
                String readMessage = reader.readLine();

                if (readMessage != null) {
                    message = new Message(readMessage);

                    // Set recipient address as serverIP of incoming message
                    this.recipientAddress = new InetSocketAddress(message.getSourceSocketAddress(), message.getSourcePort());

                    // Add to received messages
                    parentNode.addMessageHistory(message.getSourceUsername(), message);

                    // Assign message to be handled by the node
                    parentNode.handleMessage(message, this);
                }

            } while (true);

        } catch (Exception e) {
            // Connection closed - remove connection
            e.printStackTrace();
            parentNode.activeConnections.values().remove(this);

            // Close socket
            try {
                this.socket.close();
                System.out.println("Connection closed with " + this.getRecipientAddress());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendMessage(Message message) {
        message.decrementTtl();

        // Only send message if ttl is >= 0
        if (message.getTtl() >= 0) {
            try {
                //System.out.println("SENDING MESSAGE : '" + message + "' - TO " + this.recipientAddress);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println(message);
            } catch (IOException e) {
                System.out.println("Failed to send message " + message + "\n" + e);
            }
        }
    }

    public InetSocketAddress getRecipientAddress() {
        return this.recipientAddress;
    }

}
