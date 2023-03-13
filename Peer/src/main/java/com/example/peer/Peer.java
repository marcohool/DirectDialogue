package com.example.peer;

import Connections.ConnectionHandler;
import Controllers.HomeController;
import Messages.Message;
import Messages.MessageDescriptor;
import Messages.Query;
import Messages.QueryDescriptor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Peer extends Nodes.Node {
    private final ArrayList<UUID> seenMessageUUIDs = new ArrayList<>();
    private final HashMap<String, ArrayList<StoredMessage>> chatHistory = new HashMap<>();
    private ActionEvent lastEvent;
    private HomeController homeController;
    private final int minNoOfConnections = 3;
    private final InetSocketAddress serverAddress = new InetSocketAddress("192.168.68.63", 1926);

    private static final InetSocketAddress[] initialPeers = new InetSocketAddress[]{
            new InetSocketAddress("127.0.0.1", 1),
            new InetSocketAddress("127.0.0.1", 2),
            new InetSocketAddress("127.0.0.1", 3),
            new InetSocketAddress("127.0.0.1", 4),
            new InetSocketAddress("127.0.0.1", 5),
    };

    public Peer(String username) {
        super(username);
        setServerAddress(serverAddress);
    }

    public Peer() {
        super(null);
        setServerAddress(serverAddress);
    }

    public Peer(String username, InetSocketAddress address) {
        super(username, address);
    }

    public static void main(String[] args) {

////      --------------------- MANUAL TESTING ---------------------
//      Peer6 to join the network and request 2 neighbour peers from initial peer
//        Peer peer6 = new Peer("lautahool");
//        peer6.sendMessage(MessageDescriptor.PING, null, 1, initialPeers[0]);
//        wait(1);
//        System.out.println(peer6.getName() +" -> " + peer6.activeConnections);
//        peer6.sendMessage(MessageDescriptor.QUERY, new Query(QueryDescriptor.NEIGHBOURHOOD, "2").toString(), 1, peer6.getActiveConnections().get("peer0"));
//
//        wait(1);
//
//        // Peer7 to join the network and request for "lautahool" from initial peer
//        Peer peer7 = new Peer("blol");
//        peer7.sendMessage(MessageDescriptor.PING, null, 1, initialPeers[1]);
//        wait(1);
//        System.out.println(peer7.getName() +" -> " + peer7.activeConnections);
//        peer7.sendMessage(MessageDescriptor.QUERY, new Query(QueryDescriptor.USER, "lautahool").toString(), 3, peer7.getActiveConnections().get("peer1"));
//
//        // Wait and print connections
//        wait(1);
//
//        System.out.println(peer6.getName() +" -> " + peer6.activeConnections);
//        System.out.println(peer7.getName() +" -> " + peer7.activeConnections);
//
//        wait(1);
//        -----------------------------------------------------------

        PeerUI ui = new PeerUI();
        ui.start();

    }

    public void handleMessage(Message message, ConnectionHandler connectionHandler) {
        System.out.println(this.getName() + " RECEIVED MESSAGE : '" + message + "'");

        // Only handle message if it has not been received before & ttl > 0
        if (!this.seenMessageUUIDs.contains(message.getUuid())) {
            this.seenMessageUUIDs.add(message.getUuid());
            this.lastReceivedMessage = message;

            // If message is coming from server
            if (new InetSocketAddress(message.getSourceSocketAddress(), message.getSourcePort()).equals(this.serverAddress)) {
                switch (message.getMessageDescriptor()) {
                    case LOGIN_SUCCESS:
                        this.setName(message.getMessageContent());
                        pingRandomInitialPeer();
                        changeScene(this.lastEvent, "home.fxml");
                        break;

                    case LOGIN_ERROR:
                        displayAlert("Login unsuccessful");
                        break;

                    case SIGNUP_ERROR:
                        displayAlert(message.getMessageContent());
                        break;

                    case SEARCH:
                        try {
                            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("home.fxml"));
                            fxmlLoader.load();
                            this.homeController.updateUserSearchLV(message.getMessageContent().split(" "));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }

            // If message is coming from peer
            else {
                switch (message.getMessageDescriptor()) {
                    case PING:
                        // Register connection with user
                        this.activeConnections.put(message.getSourceUsername(), connectionHandler);

                        // Respond with PONG
                        this.sendMessage(MessageDescriptor.PONG, null, 1, connectionHandler);
                        break;

                    case PONG:
                        // Register connection with user
                        this.activeConnections.put(message.getSourceUsername(), connectionHandler);

                        // If this peer needs more connection, send QUERY
                        if (this.activeConnections.size() < minNoOfConnections) {
                            this.sendMessage(MessageDescriptor.QUERY, new Query(QueryDescriptor.NEIGHBOURHOOD, "1").toString(), 1, connectionHandler);
                        }

                        // Check queue for any messages to be sent to new connection
                        for (Map.Entry<String, String> queuedMessaged : this.messageQueue.entrySet()) {
                            // If there is a queued message, send it
                            if (queuedMessaged.getKey().equals(message.getSourceUsername())) {
                                this.sendMessage(MessageDescriptor.MESSAGE, queuedMessaged.getValue(), 1, queuedMessaged.getKey());
                            }
                        }
                        break;

                    case QUERY:
                        // Get query type
                        Query query = new Query(message.getMessageContent());

                        // If query is asking for connected peers respond with IPs of X neighbours
                        if (query.getQueryDescriptor().equals(QueryDescriptor.NEIGHBOURHOOD)) {
                            String returnedAddresses = getRandomAddresses(message.getSourceUsername(), Integer.parseInt(query.getQueryContent()));
                            if (!returnedAddresses.equals("")) {
                                this.sendMessage(MessageDescriptor.QUERYHIT, returnedAddresses, 1, connectionHandler);
                            }
                        }

                        // If query is looking for username
                        if (query.getQueryDescriptor().equals(QueryDescriptor.USER)) {

                            // If this peer is connected to the user
                            if (this.activeConnections.containsKey(query.getQueryContent())) {
                                this.sendMessage(MessageDescriptor.QUERYHIT, String.valueOf(this.activeConnections.get(query.getQueryContent()).getRecipientAddress()).replace("/", ""), 1, connectionHandler);
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
                            this.sendMessage(MessageDescriptor.PING, null, 1, new InetSocketAddress(ipSplit[0], Integer.parseInt(ipSplit[1])));
                        }
                        break;

                    case MESSAGE:
                        this.homeController.displayMessage(message.getMessageContent(), message.getSourceUsername(), false);
                        System.out.println("message all good -> " + message);

                }
            }
        }
    }

    public HashMap<String, ArrayList<StoredMessage>> getChatHistory() {
        return chatHistory;
    }


    public void addMessageHistory(String recipient, StoredMessage message) {
        if (this.chatHistory.containsKey(recipient)) {
            this.chatHistory.get(recipient).add(message);
        } else {
            this.chatHistory.put(recipient, new ArrayList<>(List.of(message)));
        }
    }

    public void changeScene(ActionEvent event, String fxmlFile) {
        Platform.runLater(
                () -> {
                    try {
                        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFile));
                        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                        stage.setTitle("DirectDialogue");

                        if (fxmlFile.equals("home.fxml")) {
                            stage.setScene(new Scene(fxmlLoader.load(), 1400, 820));
                        } else {
                            stage.setScene(new Scene(fxmlLoader.load(), 600, 400));
                        }

                        // Center stage
                        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
                        stage.setX((primScreenBounds.getWidth() - stage.getWidth()) / 2);
                        stage.setY((primScreenBounds.getHeight() - stage.getHeight()) / 2);

                        // Set peer in scenes controller
                        if (fxmlLoader.getController() instanceof HomeController) {
                            this.homeController = fxmlLoader.getController();
                        }

                        // Show stage
                        stage.show();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    public void displayAlert(String message){
        Platform.runLater(
                () -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(message);
                    alert.show();
                }
        );
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

    private void pingRandomInitialPeer() {
        Random random = new Random();

        InetSocketAddress peerToPing = initialPeers[random.nextInt(initialPeers.length)];

        // Ping random initial peer
        this.sendMessage(MessageDescriptor.PING, null, 1, peerToPing);
    }

    // Util method to wait for connections to establish
    public static void wait(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setLastEvent(ActionEvent lastEvent) {
        this.lastEvent = lastEvent;
    }

}
