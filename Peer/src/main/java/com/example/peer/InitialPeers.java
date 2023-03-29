package com.example.peer;

import Messages.MessageDescriptor;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class InitialPeers {

    // Set initial peer nodes and connect them to each other
    public static ArrayList<Peer> initialPeers;

    public static void main(String[] args) {

        initialPeers = setInitialPeers(new InetSocketAddress[]{
                new InetSocketAddress("127.0.0.1", 1),
                new InetSocketAddress("127.0.0.1", 2),
                new InetSocketAddress("127.0.0.1", 3),
                new InetSocketAddress("127.0.0.1", 4),
                new InetSocketAddress("127.0.0.1", 5),
        });

        Peer.wait(1);

        // Print active connections of preconfigured nodes
        for (Peer peer : initialPeers) {
            System.out.print("\n" + peer.getName() + " (" + peer.getAddress() + ") is connected to:");
            peer.getActiveConnections().forEach((key, value) -> System.out.print(" " + key + " (" + value.getRecipientAddress() + ") | "));
        }

    }

    public static ArrayList<Peer> setInitialPeers(InetSocketAddress[] addresses) {
        ArrayList<Peer> peers = new ArrayList<>();

        // Declare peers
        for (int i = 0; i < addresses.length; i++) {
            peers.add(new Peer("peer" + i ,addresses[i]));
        }

        // Connect all peers together
        for (Peer peer1 : peers) {
            for (Peer peer2: peers) {
                if (peer1 != peer2) {
                    peer1.sendMessage(MessageDescriptor.PING, null, 1, null, null, peer2.getAddress());
                }
            }
        }

        return peers;
    }

}
