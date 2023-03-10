package Connections;

import Nodes.Node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Random;

public class Listener implements Runnable {
    private final ServerSocket serverSocket;
    private final Node parentNode;

    public Listener(InetSocketAddress address, Node node) throws UnknownHostException {
        this.serverSocket = setServerSocket(address, address.getPort());
        this.parentNode = node;
        node.setAddress((InetSocketAddress) serverSocket.getLocalSocketAddress());
        System.out.println("ServerSocket set successfully to " + serverSocket.getLocalSocketAddress());
    }

    private ServerSocket setServerSocket(InetSocketAddress address, int port) throws UnknownHostException {

        // If port is not specified, randomly generate port number
        if (port == 0) {
            port = getRandPort();
        }

        // Check if port is available
        try {
            return new ServerSocket(port, 0, address.getAddress());
        } catch (IOException e) {
            // Choose new port and try again
            System.out.println("Port " + port + " not free, finding new port");
            return setServerSocket(address, getRandPort());
        }
    }

    private int getRandPort() {
        Random rand = new Random();
        return rand.nextInt(65536);
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Start thread to handle each new connection
                ConnectionHandler connectionHandler = new ConnectionHandler(serverSocket.accept(), parentNode);
                connectionHandler.start();
            }
        } catch (IOException e) {
            System.err.println("Error handling incoming connection: " + e.getMessage());
        }
    }
}
