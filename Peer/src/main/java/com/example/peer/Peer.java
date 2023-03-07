package com.example.peer;

import Connections.ConnectionHandler;
import java.net.InetSocketAddress;

public class Peer extends Nodes.Node {

    public Peer() {
        super();
    }

    public static void main(String[] args) {

        // Configure server address
        String serverIPAddress = "192.168.68.63";
        int serverPort = 1926;
        InetSocketAddress serverAddress = new InetSocketAddress(serverIPAddress, serverPort);

        // Start peer
        Peer peer1 = new Peer();
        peer1.sendMessage("test1", serverAddress);

    }

    public void handleMessage(String message, ConnectionHandler connectionHandler) {
        System.out.println(message);
    }

}
