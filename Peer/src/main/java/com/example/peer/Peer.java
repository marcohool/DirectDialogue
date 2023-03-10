package com.example.peer;

import Connections.ConnectionHandler;
import Messages.Message;
import Messages.MessageDescriptor;
import Messages.Query;
import Messages.QueryDescriptor;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Peer extends Nodes.Node {
    private final ArrayList<UUID> seenMessageUUIDs = new ArrayList<>();

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
            System.out.print("\n" + peer.getName() + " (" + peer.getAddress() + ") is connected to:");
            peer.getActiveConnections().forEach((key, value) -> System.out.print(" " + key + " (" + value.getRecipientAddress() + ") | "));
        }

        wait(1);

        // Peer6 to join the network and request 2 neighbour peers from initial peer
        Peer peer6 = new Peer("lautahool");
        peer6.sendMessage(MessageDescriptor.PING, null, 1, initialPeers.get(0).getAddress(), null);
        wait(1);
        System.out.println(peer6.getActiveConnections());
        peer6.sendMessage(MessageDescriptor.QUERY, new Query(QueryDescriptor.NEIGHBOURHOOD, "2").toString(), 1, peer6.getActiveConnections().get(initialPeers.get(0).getName()).getRecipientAddress(), null);

        wait(1);

        // Peer7 to join the network and request for "lautahool" from initial peer
        Peer peer7 = new Peer("blol");
        peer7.sendMessage(MessageDescriptor.PING, null, 1, initialPeers.get(1).getAddress(), null);
        wait(1);
        peer7.sendMessage(MessageDescriptor.QUERY, new Query(QueryDescriptor.USER, "lautahool").toString(), 3,  peer7.getActiveConnections().get(initialPeers.get(1).getName()).getRecipientAddress(), null);


        // Wait and print connections
        wait(1);

        System.out.println(peer6.getName() +" -> " + peer6.activeConnections);
        System.out.println(peer7.getName() +" -> " + peer7.activeConnections);

        wait(1);

    }

    public void handleMessage(Message message, ConnectionHandler connectionHandler) {
        System.out.println(this.getName() + " RECEIVED MESSAGE : '" + message + "'");

        // Only handle message if it has not been received before & ttl > 0
        if (!this.seenMessageUUIDs.contains(message.getUuid())) {
            this.seenMessageUUIDs.add(message.getUuid());

            switch (message.getMessageDescriptor()) {
                case PING:
                    // Register connection with user
                    this.activeConnections.put(message.getSourceUsername(), connectionHandler);

                    // Respond with PONG
                    this.sendMessage(MessageDescriptor.PONG, null, 1, null, connectionHandler);
                    break;

                case PONG:
                    // Register connection with user
                    this.activeConnections.put(message.getSourceUsername(), connectionHandler);
                    break;

                case QUERY:
                    // Get query type
                    Query query = new Query(message.getMessageContent());

                    // If query is asking for connected peers respond with IPs of X neighbours
                    if (query.getQueryDescriptor().equals(QueryDescriptor.NEIGHBOURHOOD)) {
                        this.sendMessage(MessageDescriptor.QUERYHIT, getRandomAddresses(message.getSourceUsername(), Integer.parseInt(query.getQueryContent())), 1, null, connectionHandler);
                    }

                    // If query is looking for username
                    if (query.getQueryDescriptor().equals(QueryDescriptor.USER)) {

                        // If this peer is connected to the user
                        if (this.activeConnections.containsKey(query.getQueryContent())) {
                            this.sendMessage(MessageDescriptor.QUERYHIT, String.valueOf(this.activeConnections.get(query.getQueryContent()).getRecipientAddress()).replace("/", ""), 1, new InetSocketAddress(message.getSourceSocketAddress(), message.getSourcePort()), null);
                        }

                        // Else echo message
                        else {
                            for (ConnectionHandler connection : this.activeConnections.values()) {
                                message.decrementTtl();
                                connection.sendMessage(message);
                            }
                        }
                    }
                    break;

                case QUERYHIT:

                    // Send PING to all returned IPs
                    for (String ip : message.getMessageContent().split(" ")) {
                        String[] ipSplit = ip.split(":");
                        this.sendMessage(MessageDescriptor.PING, null, 1, new InetSocketAddress(ipSplit[0], Integer.parseInt(ipSplit[1])), null);
                    }
                    break;

                case MESSAGE:
                    System.out.println("message all good -> " + message);
            }

        }
    }

    private String getRandomAddresses(String requester, int x) {
        StringBuilder connectedIPs = new StringBuilder();
        Random random = new Random();
        Set<Integer> generatedIndices = new HashSet<>();

        // Remove requester from connected IPs
        HashMap<String, ConnectionHandler> activeConnectionsCopy = new HashMap<>(this.activeConnections);
        activeConnectionsCopy.remove(requester);
        Object[] values = activeConnectionsCopy.values().toArray();

        if (values.length < x) {
            x = values.length;
        }

        for (int i = 0; i < x; i++) {
            int randomIndex;

            // Make sure the same address is not selected twice
            do {
                randomIndex = random.nextInt(values.length);
            } while (generatedIndices.contains(randomIndex));
            generatedIndices.add(randomIndex);

            // Get address at index
            ConnectionHandler address = (ConnectionHandler) values[randomIndex];
            connectedIPs.append(address.getRecipientAddress().toString().replace("/", "")).append(" ");
        }

        return connectedIPs.toString();
    }

    // Util method to wait for connections to establish
    private static void wait(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
                    peer1.sendMessage(MessageDescriptor.PING, null, 1, peer2.getAddress(), null);
                }
            }
        }

        return peers;
    }

}
