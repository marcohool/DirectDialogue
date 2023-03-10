package com.example.peer;

import Connections.ConnectionHandler;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Peer extends Nodes.Node {

    public Peer(String username) {
        super(username);
    }

    public Peer(String username, InetSocketAddress address) {
        super(username, address);
    }

    public static void main(String[] args) {

        // Configure server address
        String serverIPAddress = "192.168.68.63";
        int serverPort = 1926;
        InetSocketAddress serverAddress = new InetSocketAddress(serverIPAddress, serverPort);

        // Set initial peer nodes and connect them to each other
        ArrayList<Peer> initialPeers = setInitialPeers(new InetSocketAddress[]{
                new InetSocketAddress("127.0.0.1", 1),
                new InetSocketAddress("127.0.0.1", 2),
                new InetSocketAddress("127.0.0.1", 3),
                new InetSocketAddress("127.0.0.1", 4),
                new InetSocketAddress("127.0.0.1", 5),
        });

        // Print active connections of preconfigured nodes
        for (Peer peer : initialPeers) {
            System.out.print(peer.getName() + " (" + peer.getAddress() + ") is connected to:");
            peer.getActiveConnections().forEach((key, value) -> System.out.print(" " + key + " (" + value.getRecipientAddress() + ") | "));
            System.out.print("\n");
        }

        // New node to join the network
//        Peer peer1 = new Peer("lautahool");
//        peer1.sendMessage("query peer2", initialPeers.get(0).getAddress());

    }

    public void handleMessage(String message, ConnectionHandler connectionHandler) {
        System.out.println("MESSAGE RECEIVED : '" + message + "' - FROM " + connectionHandler.getRecipientAddress());

        String[] messageSplit = message.split(" ");
        String[] address = messageSplit[0].split(":");

        switch (messageSplit[2]) {
            case "query":
                if (searchForEstablishedConnection(messageSplit[3]) != null) {
                    System.out.println("Connection is there");
                }

            case "hit":

        }

    }

    private void flood(String message) {

    }

    private static ArrayList<Peer> setInitialPeers(InetSocketAddress[] addresses) {
        ArrayList<Peer> peers = new ArrayList<>();

        // Declare peers
        for (int i = 0; i < addresses.length; i++) {
            peers.add(new Peer("peer" + i ,addresses[i]));
        }

        // Connect all peers together
        for (Peer peer1 : peers) {
            for (Peer peer2: peers) {
                if (peer1 != peer2) {
                    peer1.sendMessage("hello", peer2.getAddress());
                }
            }
        }

        return peers;
    }

}
