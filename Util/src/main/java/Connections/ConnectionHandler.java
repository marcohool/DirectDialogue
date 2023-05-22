package Connections;

import Messages.Message;
import Nodes.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

public class ConnectionHandler extends Thread {
    private String recipientName;
    private InetSocketAddress recipientAddress;
    private final Socket socket;
    private final Node parentNode;

    // When initiating a new connection
    public ConnectionHandler(InetSocketAddress address, Node parentNode) throws IOException {
        this.recipientAddress = address;
        this.socket = new Socket(address.getAddress(), this.recipientAddress.getPort());
        this.parentNode = parentNode;
        this.start();
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

                    // Set recipient address & recipient name
                    this.recipientName = message.getSourceUsername();
                    this.recipientAddress = new InetSocketAddress(message.getSourceSocketAddress(), message.getSourcePort());
                    System.out.println(parentNode.getName() + " RECEIVED MESSAGE (" + this.recipientAddress + ") : " + message);

                    // Assign message to be handled by the node
                    parentNode.handleMessage(message, this);
                }

            } while (true);

        } catch (Exception e) {
            // Connection closed - remove connection
            e.printStackTrace();
            parentNode.activeConnections.remove(this);

            // Close socket
            try {
                this.socket.close();
                System.out.println("Connection closed with " + this.recipientAddress + " by " + this.parentNode.getName());
                System.out.println(parentNode.getName() + " -> " + parentNode.activeConnections);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendMessage(Message message) {

        // Simulate network delays
//        Random rand = new Random();
//        int chance = rand.nextInt(10);
//
//        if (chance < 3) {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }


        try {
            System.out.println(parentNode.getName() + " SENDING MESSAGE : '" + message + "' - TO " + this.recipientAddress);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(message);
        } catch (IOException e) {
            System.out.println("Failed to send message " + message + "\n" + e);
            }
    }

    public String getRecipientName() {
        return recipientName;
    }

    public InetSocketAddress getRecipientAddress() {
        return recipientAddress;
    }
}
