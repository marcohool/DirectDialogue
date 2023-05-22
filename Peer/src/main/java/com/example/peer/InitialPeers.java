package com.example.peer;

import Messages.MessageDescriptor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class InitialPeers {


    public static void main(String[] args) throws IOException {

        ArrayList<Peer> initialPeers = setInitialPeers(new InetSocketAddress[]{
                new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), 1),
                new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), 2),
                new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), 3),
//                new InetSocketAddress("127.0.0.1", 4),
//                new InetSocketAddress("127.0.0.1", 5),
        });

        Peer.wait(1);

        // Print active connections of preconfigured nodes
        for (Peer peer : initialPeers) {
            System.out.print("\n" + peer.getName() + " (" + peer.getAddress() + ") is connected to:");
            peer.getActiveConnections().forEach((connection) -> System.out.print(" " + connection.getRecipientName() + " (" + connection.getRecipientAddress() + ") | "));
        }
        System.out.println("\n");
    }

    public static ArrayList<Peer> setInitialPeers(InetSocketAddress[] addresses) throws IOException {
        ArrayList<Peer> peers = new ArrayList<>();

        // Declare peers
        for (int i = 0; i < addresses.length; i++) {
            peers.add(new Peer("peer" + i , addresses[i]));
        }

        // Connect all peers together
        for (Peer peer1 : peers) {
            for (Peer peer2: peers) {
                if (peer1 != peer2) {
                    peer1.sendMessageToAddress(peer1.signMessage(MessageDescriptor.PING, null), peer2.getAddress());

                }
            }
        }

        return peers;
    }

}
