package com.example.peer;

import Connections.ConnectionHandler;
import Controllers.HomeController;
import Messages.*;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class Peer extends Nodes.Node {
    private final ArrayList<Chat> activeChats = new ArrayList<>();
    private ActionEvent lastEvent;
    private HomeController homeController;
    private final Object homeControllerLock = new Object();
    private final int minNoOfConnections = 1;
    private final InetSocketAddress serverAddress = new InetSocketAddress("192.168.68.63", 1926);
    private final ConcurrentHashMap<UUID, ConcurrentLinkedQueue<StoredMessage>> groupChatMessageQueue = new ConcurrentHashMap<>();

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
//        System.out.print("\n" + this.getName() + " (" + this.getAddress() + ") is connected to:");
//        this.getActiveConnections().forEach((key, value) -> System.out.print(" " + key + " (" + value.getRecipientAddress() + ") | "));
//        System.out.println();
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
                    case SIGNUP_SUCCESS:
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
                            if (!message.getMessageContent().equals("")) {
                                this.homeController.updateUserSearchLV(message.getMessageContent().split(" "));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }

            // If message is coming from peer
            else {

                // If this peer needs more connection, send QUERY
                if (this.activeConnections.size() < minNoOfConnections) {
                    this.sendMessage(MessageDescriptor.QUERY, new Query(QueryDescriptor.NEIGHBOURHOOD, "1").toString(), 1, null, null, connectionHandler);
                }

                switch (message.getMessageDescriptor()) {
                    case PING:
                        // Register connection with user
                        this.activeConnections.put(message.getSourceUsername(), connectionHandler);

                        // Respond with PONG
                        this.sendMessage(MessageDescriptor.PONG, null, 1, null, null, connectionHandler);

                        // Send any queued messages
                        checkQueuedMessages(message.getSourceUsername(), connectionHandler);

                        if (homeController != null) {
                            homeController.setActivity(message.getChatUUID());
                        }
                        break;

                    case PONG:
                        // Register connection with user
                        this.activeConnections.put(message.getSourceUsername(), connectionHandler);

                        // Send any queued messages
                        checkQueuedMessages(message.getSourceUsername(), connectionHandler);

                        if (homeController != null) {
                            homeController.setActivity(message.getChatUUID());
                        }

                        break;

                    case QUERY:
                        // Get query type
                        Query query = new Query(message.getMessageContent());

                        // If query is asking for connected peers respond with IPs of X neighbours
                        if (query.getQueryDescriptor().equals(QueryDescriptor.NEIGHBOURHOOD)) {

                            String returnedAddresses = getRandomAddresses(message.getSourceUsername(), Integer.parseInt(query.getQueryContent()));
                            if (!returnedAddresses.equals("")) {
                                this.sendMessage(MessageDescriptor.QUERYHIT, returnedAddresses, 5, null, null, connectionHandler);
                            }

                        }

                        // If query is looking for username
                        if (query.getQueryDescriptor().equals(QueryDescriptor.USER)) {

                            // If this peer is connected to the user
                            if (this.activeConnections.containsKey(query.getQueryContent())) {
                                this.sendMessage(MessageDescriptor.QUERYHIT, query.getQueryContent() + ":" + this.activeConnections.get(query.getQueryContent()).getRecipientAddress(), 5, UUID.randomUUID(), null, connectionHandler);
                            }

                            // Else echo message
                            else {
                                for (ConnectionHandler connection : this.activeConnections.values()) {
                                    // Don't echo to sender of this message
                                    if (!connection.equals(connectionHandler)) {
                                        this.sendMessage(MessageDescriptor.QUERY, message.getMessageContent(), message.getTtl(), message.getUuid(), null, connection);
                                    }
                                }

                                // Log echo
                                if (this.receivedQueryRequests.containsKey(message.getSourceUsername())) {
                                    this.receivedQueryRequests.get(message.getSourceUsername()).add(message.getMessageContent());
                                } else {
                                    this.receivedQueryRequests.put(message.getSourceUsername(), new ArrayList<>(Collections.singleton(message.getMessageContent().split(" ")[1])));
                                }

                            }
                        }
                        break;

                    case QUERYHIT:
                        for (String ip : message.getMessageContent().split(" ")) {
                            String[] ipSplit = ip.split(":");

                            // If QUERYHIT is simply a returned IP -> ping it
                            if (ipSplit.length == 2) {
                                this.sendMessage(MessageDescriptor.PING, null, 1, null, null, new InetSocketAddress(ipSplit[0], Integer.parseInt(ipSplit[1])));
                                break;
                            }

                            String returnedUsername = ipSplit[0];
                            InetSocketAddress returnedIP = new InetSocketAddress(ipSplit[1].replace("/", ""), Integer.parseInt(ipSplit[2]));

                            // Check query queues
                            for (Map.Entry<String, ArrayList<String>> entry : this.receivedQueryRequests.entrySet()) {
                                // If entry contains a query for said user send QUERYHIT
                                if (entry.getValue().contains(returnedUsername)) {
                                    this.sendMessage(MessageDescriptor.QUERYHIT, returnedUsername + ":" + returnedIP, null, null, 3, entry.getKey());
                                    entry.getValue().remove(returnedUsername);
                                }
                            }

                            // Check own query queue
                            for (String entry : this.messageQueue.keySet()) {
                                if (entry.equals(returnedUsername)) {
                                    // Ping IP
                                    this.sendMessage(MessageDescriptor.PING, null, 1, null, null, returnedIP);
                                }
                            }
                        }
                        break;

                    case MESSAGE:
                        StoredMessage storedMessage = new StoredMessage(message.getUuid(), message.getChatUUID(), message.getSourceUsername(), message.getMessageContent(), message.getDateTime());
                        storedMessage.setDelivered(true);
                        Chat addedChat = addReceivedChatHistory(message.getChatUUID(), storedMessage);

                        // Wait for homeController to load if it is null
                        synchronized (homeControllerLock) {
                            while (homeController == null) {
                                try {
                                    homeControllerLock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (addedChat != null) {
                            this.homeController.updateRecentChats(addedChat);
                        }
                        this.homeController.displayMessage(storedMessage);
                        break;

                    case CREATE_GROUP:
                        String[] participants = message.getMessageContent().replace("[", "").replace("]", "").split(",");

                        for (int i = 0; i < participants.length; i++) {
                            participants[i] = participants[i].trim();

                            // Ping participant if connection is not established already
                            this.sendMessage(MessageDescriptor.PING, null, null, null, 1, participants[i]);
                        }

                        Chat newChat = new Chat(message.getChatUUID(), new HashSet<>(Arrays.asList(participants)), this.getName());
                        newChat.addMessageHistory(new StoredMessage(UUID.randomUUID(), newChat.getChatUUID(), "SYSTEM", message.getSourceUsername() + " has created the chat", message.getDateTime()));

                        this.activeChats.add(newChat);
                        //checkGroupQueuedMessages(newChat.getChatUUID());
                        this.homeController.updateRecentChats(newChat);


                        break;
                }
            }
        }
    }

    private void checkQueuedMessages(String username, ConnectionHandler connectionHandler) {
        // Check queue for any messages to be sent to new connection
        for (Map.Entry<String, ConcurrentLinkedQueue<Message>> queuedMessages : this.messageQueue.entrySet()) {
            // If there is a queued message, send it
            if (queuedMessages.getKey().equals(username)) {
                for (Message message : queuedMessages.getValue()) {
                    connectionHandler.sendMessage(message);

                    // Remove from queue
                    queuedMessages.getValue().remove(message);

                    // Set in history as sent
                    for (Chat chat : this.activeChats) {
                        if (chat.getChatName().equals(username)) {
                            for (StoredMessage storedMessage : chat.getMessageHistory()) {
                                storedMessage.setDelivered(true);
                            }
                        }
                    }
                }
            }
        }

    }

    private void checkGroupQueuedMessages(UUID chatUUID) {
        for (Map.Entry<UUID, ConcurrentLinkedQueue<StoredMessage>> queuedMessage : this.groupChatMessageQueue.entrySet()) {
            if (queuedMessage.getKey().equals(chatUUID)) {
                for (StoredMessage message : queuedMessage.getValue()) {
                    addReceivedChatHistory(chatUUID, message);
                    queuedMessage.getValue().remove(message);
                }
            }
        }
    }

    public Chat addReceivedChatHistory(UUID chatUUID, StoredMessage message) {

        // If UUID is null, chat is a direct message
        if (chatUUID == null) {
            //boolean chatExists = false;

            // Find if chat exists and add message to chat message history
            for (Chat activeChat : this.activeChats) {
                if (activeChat.getChatName().equals(message.getSender())) {
                    activeChat.addMessageHistory(message);
                    return activeChat;
                }
            }

            // If chat is from new peer, create a chat and add it
            Chat newChat = new Chat(new HashSet<>(Collections.singleton(message.getSender())), this.getName());
            this.activeChats.add(newChat);
            newChat.addMessageHistory(message);
            return newChat;

        }

        // Else - message is to a group chat
        else {
            // If group chat exists, add message
            for (Chat activeChat : this.activeChats) {
                if (activeChat.getChatUUID() != null && activeChat.getChatUUID().equals(chatUUID)) {
                    activeChat.addMessageHistory(message);

                    Message messageToEcho = new Message(this.getName(), this.address.getAddress(), this.address.getPort(), message.getUuid(), message.getChatUUID(), 1, MessageDescriptor.MESSAGE, message.getMessageContent());
                    // Echo message to all participants
                    for (String participant : activeChat.getAllChatParticipants()) {
                        this.sendMessage(messageToEcho, participant);
                    }

                    return activeChat;
                }
            }

            // Else add to message queue
            addMessageToGroupQueue(chatUUID, message);
//            String[] participants = message.getMessageContent().replace("[", "").replace("]", "").split(",");
//            Chat newChat = new Chat(chatUUID, new HashSet<>(Arrays.asList(participants)), this.getName());
//            this.activeChats.add(newChat);
            return null;
        }

    }

    private void addMessageToGroupQueue(UUID chatUUID, StoredMessage message) {
        if (this.groupChatMessageQueue.containsKey(chatUUID)) {
            this.groupChatMessageQueue.get(chatUUID).add(message);
        } else {
            this.groupChatMessageQueue.put(chatUUID, new ConcurrentLinkedQueue<>(Collections.singleton(message)));
        }
    }

    // Add message history to existing chat
    public void addChatHistory(Chat chat, StoredMessage message) {

        chat.addMessageHistory(message);

        if (!this.activeChats.contains(chat)) {
            this.activeChats.add(chat);
        }

//        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
//        executor.schedule(() -> this.messageQueue.forEach((username, queuedMessages) -> {
//            if (username.equals(chatName)) {
//                for (Message message1 : queuedMessages) {
//                    if (message1.getUuid().equals(message.getUuid())) {
//                        queuedMessages.remove(message1);
//                        message.setFailed(true);
//                        this.homeController.setFailedSend(message);
//                    }
//                }
//            }
//        }), 10, TimeUnit.SECONDS);

    }

    public void createGroup(Set<String> participants) {
        participants.add(this.getName());

        Chat chat = new Chat(UUID.randomUUID(), participants, this.getName());
        chat.addMessageHistory(new StoredMessage(null, chat.getChatUUID(), "SYSTEM", this.getName() + " has created the chat", LocalDateTime.now()));
        this.activeChats.add(chat);
        this.homeController.updateRecentChats(chat);

        for (String participant : participants) {
            this.sendMessage(MessageDescriptor.CREATE_GROUP, chat.getAllChatParticipants().toString(), chat.getChatUUID(), null, 1, participant);
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
                            synchronized (homeControllerLock) {
                                this.homeController = fxmlLoader.getController();
                                homeControllerLock.notifyAll();
                            }
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
        this.sendMessage(MessageDescriptor.PING, null, 1, null, null, peerToPing);
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

    public Chat getActiveChat(UUID chatUUID) {
        for (Chat chat : this.activeChats) {
            if (chat.getChatUUID().equals(chatUUID)) {
                return chat;
            }
        }
        return null;
    }

    public ArrayList<Chat> getActiveChats() {
        return activeChats;
    }

}
