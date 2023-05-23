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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class Peer extends Nodes.Node {
    private InetSocketAddress serverAddress;

    private final ArrayList<Chat> activeChats = new ArrayList<>();

    private final LinkedHashSet<Message> deliveredMessages = new LinkedHashSet<>();

    // Store QUERY requests that have been echoed and waiting for QUERYHIT - <User sending query, queried username>
    protected final ConcurrentHashMap<String, HashSet<String>> receivedQueryRequests = new ConcurrentHashMap <>();

    // Store messages meant for this peer for group chats that have not yet been initialized
    private final ArrayList<Message> groupMessageQueue = new ArrayList<>();


    // JavaFX variables
    private ActionEvent lastEvent;
    private HomeController homeController;
    private final Object homeControllerLock = new Object();

    // Hard-coded initial peers
    private final InetSocketAddress[] initialPeers = new InetSocketAddress[5];

    // Constructors
    public Peer(String name) {
        super(name);
        setInitialAddresses();
    }

    // Used for constructing initial peers
    public Peer(String username, InetSocketAddress address) {
        super(username, address);
    }

    // Main
    public static void main(String[] args) {
        PeerUI ui = new PeerUI();
        ui.start();
    }

    private void setInitialAddresses() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nPlease enter the IP address of the initial peers: ");
        String address = scanner.next();

        for (int i = 0; i<initialPeers.length; i++) {
            initialPeers[i] = new InetSocketAddress(address, i+1);
        }

        System.out.println("\nPlease enter the IP address of the server: ");
        address = scanner.next();
        this.serverAddress = new InetSocketAddress(address, 1926);
    }

    @Override
    public void handleMessage(Message message, ConnectionHandler connectionHandler) {

        if (!this.deliveredMessages.contains(message)) {
            //System.out.println(this.getName() + " RECEIVED MESSAGE (" + message.getSourceSocketAddress() + "/" + message.getSourcePort() + ") : " + message  + " " + connectionHandler.getName());

            // If message is coming from server
            if (new InetSocketAddress(message.getSourceSocketAddress(), message.getSourcePort()).equals(this.serverAddress)) {
                switch (message.getMessageDescriptor()) {
                    case LOGIN_SUCCESS:
                    case SIGNUP_SUCCESS:
                        // Set username of this peer
                        this.setName(message.getMessageContent());

                        // Ping initial peer
                        Random random = new Random();
                        InetSocketAddress peerToPing = initialPeers[random.nextInt(initialPeers.length)];
                        try {
                            this.sendMessageToAddress(signMessage(MessageDescriptor.PING, null), peerToPing);
                        } catch (IOException e) {
                            System.out.println("Connection to initial peer failed");
                            e.printStackTrace();
                        }

                        // Change scene to home scene
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
                int minNoOfConnections = 1;

                // If this peer needs more connection, send QUERY
//            if (this.activeConnections.size() < minNoOfConnections) {
//                connectionHandler.sendMessage(signMessage(MessageDescriptor.QUERY, new Query(QueryDescriptor.NEIGHBOURHOOD, "1").toString()));
//            }

                switch (message.getMessageDescriptor()) {
                    case PING:
                        // Register connection with user
                        this.activeConnections.add(connectionHandler);

                        // Send any queued messages
                        sendQueuedMessages(message.getSourceUsername(), connectionHandler);

                        // Send any queued query requests
                        sendQueryQueue(message.getSourceUsername(), connectionHandler.getRecipientAddress());

                        // Respond with PONG
                        connectionHandler.sendMessage(signMessage(MessageDescriptor.PONG, null));
                        break;

                    case PONG:
                        // Register connection with user
                        this.activeConnections.add(connectionHandler);

                        // Send any queued messages
                        sendQueuedMessages(message.getSourceUsername(), connectionHandler);

                        // Send any queued query requests
                        sendQueryQueue(message.getSourceUsername(), connectionHandler.getRecipientAddress());

                        if (homeController != null) {
                            //homeController.setActivity(message.getChatUUID());
                        }
                        break;

                    case QUERY:
                        this.deliveredMessages.add(message);

                        // Get query type
                        Query query = new Query(message.getMessageContent());

                        switch (query.getQueryDescriptor()) {
                            // Peer is requesting any neighbourhood peers to connect to
                            case NEIGHBOURHOOD -> {
                                String returnedAddresses = getRandomAddresses(message.getSourceUsername(), Integer.parseInt(query.getQueryContent()));
                                if (!returnedAddresses.equals("")) {
                                    connectionHandler.sendMessage(signMessage(MessageDescriptor.QUERYHIT, returnedAddresses));
                                }
                            }

                            // Peer is requesting for a specific user
                            case USER -> {

                                // If this peer is connected to the user
                                for (ConnectionHandler activeConnection : this.activeConnections) {
                                    if (activeConnection.getRecipientName().equals(query.getQueryContent())) {
                                        connectionHandler.sendMessage(signMessage(MessageDescriptor.QUERYHIT, activeConnection.getRecipientName() + ":" + activeConnection.getRecipientAddress()));
                                        return;
                                    }
                                }

                                // Else echo message
                                Message messageToEcho = signMessage(MessageDescriptor.QUERY, message.getMessageContent());
                                messageToEcho.setMessageUUID(message.getMessageUUID());
                                for (ConnectionHandler activeConnection : this.activeConnections) {
                                    if (!activeConnection.equals(connectionHandler) && !activeConnection.getRecipientName().equals(message.getSourceUsername())) {
                                        activeConnection.sendMessage(messageToEcho);
                                    }
                                }

                                // Log echoed query to be able to direct QUERYHIT to original requester
                                if (this.receivedQueryRequests.containsKey(message.getSourceUsername())) {
                                    this.receivedQueryRequests.get(message.getSourceUsername()).add(message.getMessageContent().split(" ")[1]);
                                } else {
                                    this.receivedQueryRequests.put(message.getSourceUsername(), new HashSet<>(Collections.singleton(message.getMessageContent().split(" ")[1])));
                                }
                            }
                        }
                        break;

                    case QUERYHIT:
                        // For returned address in QUERYHIT
                        for (String ip : message.getMessageContent().split(" ")) {
                            String[] ipSplit = ip.split(":");

                            InetSocketAddress returnedIP;
                            if (ipSplit.length == 2) {
                                returnedIP = new InetSocketAddress(ipSplit[0], Integer.parseInt(ipSplit[1]));
                            }
                            else {
                                returnedIP = new InetSocketAddress(ipSplit[1].replace("/", ""), Integer.parseInt(ipSplit[2]));
                            }


                            // Only handle if the returned address is not already connected to this peer
                            boolean connectionExists = false;
                            for (ConnectionHandler connection : this.activeConnections) {
                                if (connection.getRecipientAddress().equals(returnedIP)) {
                                    connectionExists = true;
                                    break;
                                }
                            }

                            if (!connectionExists) {
                                // If QUERYHIT is simply a returned IP (NEIGHBOURHOOD query) -> ping it
                                if (ipSplit.length == 2) {
                                    try {
                                        sendMessageToAddress(signMessage(MessageDescriptor.PING, null), returnedIP);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                }

                                // Else is a USER QUERYHIT
                                String returnedUsername = ipSplit[0];

                                // Check message queue for any messages this peer wants to send to the returned peer IP
                                for (String user : this.messageQueue.keySet()) {
                                    // If this peer has a message for the returned QUERYHIT - send PING
                                    if (user.equals(returnedUsername)) {
                                        try {
                                            sendMessageToAddress(signMessage(MessageDescriptor.PING, null), returnedIP);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                // Check query queue for peers requesting this user
                                sendQueryQueue(returnedUsername, returnedIP);
                            }

                        }
                        break;

                    case MESSAGE:
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

                        // Split vector clock from message content
                        VectorClock vectorClock = new VectorClock(message.getMessageContent().split("\\|", 2)[0]);

                        // If chatUUID is null - message is a personal message
                        boolean chatExists = false;
                        if (message.getChatUUID() == null) {

                            // If chat exists already - add message to it
                            for (Chat activeChat : this.getActiveChats()) {
                                if (activeChat.getChatUUID() == null && activeChat.getChatParticipants().equals(new HashSet<>(Collections.singleton(message.getSourceUsername())))) {
                                    chatExists = true;
                                    deliverMessage(message, activeChat);
                                    this.homeController.updateRecentChats(activeChat);
                                    break;
                                }
                            }

                            // Add new chat if chat doesn't exist
                            if (!chatExists) {
                                Chat newChat = new Chat(null,  new HashSet<>(Collections.singleton(message.getSourceUsername())), this.getName());
                                deliverMessage(message, newChat);
                                this.activeChats.add(newChat);
                                this.homeController.updateRecentChats(newChat);
                            }

                        }

                        // Else message is for a group chat
                        else {
                            // If group exists - add message
                            for (Chat activeChats : this.getActiveChats()) {
                                if (activeChats.getChatUUID() != null && activeChats.getChatUUID().equals(message.getChatUUID())) {
                                    chatExists = true;
                                    deliverMessage(message, activeChats);
                                    this.homeController.updateRecentChats(activeChats);
                                    break;
                                }
                            }

                            // Don't deliver message if not in chat
                            if (!chatExists) {
                                return;
                            }
                        }

//                        // Deliver message
//                        deliverMessage(message);

                        break;

                    case CREATE_GROUP:
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

                        ArrayList<String> participants = new ArrayList<>(Arrays.asList(message.getMessageContent().split("\\|", 2)[1].split(", ")));

                        // Remove this peer from participants
                        participants.remove(this.getName());

                        Chat newGroupChat = new Chat(message.getChatUUID(), new HashSet<>(participants), this.getName());
                        message.setSourceUsername("SYSTEM");

                        deliverMessage(message, newGroupChat);

                        this.activeChats.add(newGroupChat);
                        this.homeController.updateRecentChats(newGroupChat);

                }
            }
        }

        // If message has been delivered - acknowledge if it is ACK
        else {
            if (message.getMessageDescriptor().equals(MessageDescriptor.MESSAGE) || message.getMessageDescriptor().equals(MessageDescriptor.CREATE_GROUP)) {

                // Get delivered and mark it delivered message as seen by receiver
                for (Message deliveredMessage : this.deliveredMessages) {
                    if (deliveredMessage.equals(message)) {
                        deliveredMessage.registerACK(message.getSourceUsername());
                        break;
                    }
                }


            }
        }

    }

    public void broadcastMessage(Message message, Set<String> participants) {
        // Set messageToBroadcast UUIDs
        Message messageToBroadcast = signMessage(message.getMessageDescriptor(), message.getMessageContent());
        messageToBroadcast.setChatUUID(message.getChatUUID());
        messageToBroadcast.setMessageUUID(message.getMessageUUID());
        messageToBroadcast.setOriginalSender(message.getSourceUsername());

        // If message is direct message
        if (message.getChatUUID() == null) {
            // If participants are defined (message is being broadcast from this peer) - send to them
            if (participants != null) {

                // Set message participants
                message.setParticipants(participants);

                for (String participant : participants) {
                    this.sendMessageToUsername(messageToBroadcast, participant);
                }
            }

            else {
                // Set message participants
                message.setParticipants(Collections.singleton(message.getSourceUsername()));

                // Send to user who sent the message
                this.sendMessageToUsername(messageToBroadcast, message.getSourceUsername());
            }
        }

        else {
            // Else broadcast to all participants
            for (Chat chat : this.activeChats) {
                if (chat.getChatUUID() != null && chat.getChatUUID().equals(message.getChatUUID())) {

                    // Set message participants
                    message.setParticipants(chat.getChatParticipants());

                    for (String user : chat.getChatParticipants()) {
                        this.sendMessageToUsername(messageToBroadcast, user);
                    }
                    break;
                }
            }
        }

    }

    public void deliverMessage(Message message, Chat chat) {
        if (!this.deliveredMessages.contains(message)) { // if m ∈ delivered then
            this.deliveredMessages.add(message); // delivered := delivered U {m};

            // Add chat if it is new
            if (!this.activeChats.contains(chat)) {
                this.activeChats.add(chat);
            }

            chat.addPending(message); // trigger <rb, Deliver | s, m>;
            broadcastMessage(message, chat.getChatParticipants()); // "Echo" m via BEB to ensure all correct processes get it
        }
    }

    public void createGroup(Set<String> participants) {
        Chat newChat = new Chat(UUID.randomUUID(), participants, this.getName());
        Message chatCreationMessage = signMessage(MessageDescriptor.CREATE_GROUP, new VectorClock(participants, this.getName()) + "|" + this.getName() + ", " + participants.toString().replace("[", "").replace("]", ""));
        chatCreationMessage.setSourceUsername("SYSTEM");
        chatCreationMessage.setChatUUID(newChat.getChatUUID());
        chatCreationMessage.setOriginalSender(this.getName());

        deliverMessage(chatCreationMessage, newChat);
        homeController.updateRecentChats(newChat);

    }


    private void sendQueuedMessages(String connectionUsername, ConnectionHandler newConnectionHandler) {
        // Check queue for any messages to be sent to new connection
        for (Map.Entry<String, ConcurrentLinkedQueue<Message>> queuedMessages : this.messageQueue.entrySet()) {
            // If there is a queued message for this user, send it
            if (queuedMessages.getKey().equals(connectionUsername)) {

                // Stop ConcurrentModificationException
                Iterator<Message> iterator = queuedMessages.getValue().iterator();
                while (iterator.hasNext()) {
                    Message message = iterator.next();
                    newConnectionHandler.sendMessage(message);

                    // Remove from queue
                    iterator.remove();
                }
            }
        }
    }

    private void sendQueryQueue(String username, InetSocketAddress recipientAddress) {
        // Check queue for QUERY requests
        Iterator<Map.Entry<String, HashSet<String>>> it = this.receivedQueryRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, HashSet<String>> queryRequest = it.next();
            // If there is a request for this user
            if (queryRequest.getValue().contains(username)) {
                sendMessageToUsername(signMessage(MessageDescriptor.QUERYHIT, username + ":" + recipientAddress), queryRequest.getKey());
                queryRequest.getValue().remove(username);

                if (queryRequest.getValue().isEmpty()) {
                    it.remove(); // remove the current entry using the iterator
                }
            }
        }
    }

    public void displayAlert(String message) {
        Platform.runLater(
                () -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(message);
                    alert.show();
                }
        );
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


    // Util method to get random address
    private String getRandomAddresses(String requester, int x) {
        StringBuilder connectedIPs = new StringBuilder();
        Random random = new Random();
        Set<Integer> generatedIndices = new HashSet<>();

        // Remove requester from connected IPs
        ConcurrentLinkedQueue<ConnectionHandler> activeConnectionsCopy = new ConcurrentLinkedQueue<>(this.activeConnections);
        activeConnectionsCopy.removeIf(connection -> connection.getRecipientName().equals(requester));

        if (activeConnectionsCopy.size() < x) {
            x = activeConnectionsCopy.size();
        }

        for (int i = 0; i < x; i++) {
            int randomIndex;

            // Make sure the same address is not selected twice
            do {
                randomIndex = random.nextInt(x);
            } while (generatedIndices.contains(randomIndex));
            generatedIndices.add(randomIndex);

            // Get address at index
            ConnectionHandler address = (ConnectionHandler) activeConnectionsCopy.toArray()[randomIndex];
            connectedIPs.append(address.getRecipientAddress().toString().replace("/", "")).append(" ");
        }

        return connectedIPs.toString();
    }

    // Util method to wait for connections to establish
    public static void wait(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }

    public ActionEvent getLastEvent() {
        return lastEvent;
    }

    public HomeController getHomeController() {
        return homeController;
    }

    public ArrayList<Chat> getActiveChats() {
        return activeChats;
    }

    public void setLastEvent(ActionEvent lastEvent) {
        this.lastEvent = lastEvent;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = new InetSocketAddress(serverAddress, 1926);
    }
}
