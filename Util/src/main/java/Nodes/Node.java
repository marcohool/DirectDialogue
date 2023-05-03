package Nodes;

import Connections.ConnectionHandler;
import Connections.Listener;
import Messages.Message;
import Messages.MessageDescriptor;
import Messages.Query;
import Messages.QueryDescriptor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Node implements INode {
    private String name;
    protected InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
    public final Queue<ConnectionHandler> activeConnections = new UniqueRecipientQueue<>();

    // Queues
    protected final ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> messageQueue = new ConcurrentHashMap<>();

    // Starting node with a specified address
    public Node(String name, InetSocketAddress address) {
        this.name = name;
        this.address = address;
        startListener();
    }

    // Starting a node with a randomly generated address
    public Node(String name) {
        this.name = name;
        startListener();

    }

    private void startListener() {
        // Start listener thread
        try {
            Thread thread = new Thread(new Listener(this.address, this));
            thread.start();

        } catch (IOException e) {
            System.err.println("Error listening for incoming connections: " + e.getMessage());
        }
    }

    public void sendMessageToUsername(Message message, String username) {
        // Check if connection with this user already exists
        ConnectionHandler connection = searchForEstablishedConnectionByUsername(username);

        // If there is no established connection with this user, send query to find them
        if (connection == null) {
            // Add message to queue
            addMessageToQueue(username, message);

            // Query for user
            Message queryMessage = signMessage(MessageDescriptor.QUERY, new Query(QueryDescriptor.USER, username).toString());
            queryMessage.setTtl(5);
            for (ConnectionHandler activeConnection : this.activeConnections) {
                activeConnection.sendMessage(queryMessage);
            }

        } else {
            // Else send message to the user from the already established connection
            connection.sendMessage(message);
        }

    }

    public void sendMessageToAddress(Message message, InetSocketAddress destinationAddress) {
        try {
            // Check if connection with this address already exists
            ConnectionHandler connection = searchForEstablishedConnectionByAddress(destinationAddress);

            // If there is no established connection create a new one
            if (connection == null) {
                connection = new ConnectionHandler(destinationAddress, this);
            }

            // Send message
            connection.sendMessage(message);

        } catch (IOException e) {
            System.out.println("-- Connection refused to " + destinationAddress + " --");
            e.printStackTrace();
        }
    }

    private synchronized ConnectionHandler searchForEstablishedConnectionByAddress(InetSocketAddress address) {
        try {
            for (ConnectionHandler conn : this.activeConnections) {
                if (conn.getRecipientAddress().equals(address)) {
                    return conn;
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("java.util.ConcurrentModificationException");
            searchForEstablishedConnectionByAddress(address);
        }
        return null;
    }

    private synchronized ConnectionHandler searchForEstablishedConnectionByUsername(String username) {
        try {
            for (ConnectionHandler conn : this.activeConnections) {
                if (conn.getRecipientName().equals(username)) {
                    return conn;
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("java.util.ConcurrentModificationException");
            searchForEstablishedConnectionByUsername(username);
        }
        return null;
    }

    protected void addMessageToQueue(String recipient, Message message) {
        if (this.messageQueue.containsKey(recipient)) {
            this.messageQueue.get(recipient).add(message);
        } else {
            this.messageQueue.put(recipient, new ConcurrentLinkedQueue<>(Collections.singleton(message)));
        }
    }

    public Message signMessage(MessageDescriptor messageDescriptor, String messageContent) {
        return new Message(this.getName(), this.address.getAddress(), this.address.getPort(), null, null, 3, messageDescriptor, this.getName(), messageContent);
    }

    public String getName() {
        return name;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public Queue<ConnectionHandler> getActiveConnections() {
        return activeConnections;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }
}


class UniqueRecipientQueue<E extends ConnectionHandler> extends ConcurrentLinkedQueue<E> {
    @Override
    public boolean add(E e) {
        for (E element : this) {
            if (element.getRecipientName().equals(e.getRecipientName())) {
                return false;
            }
        }
        return super.add(e);
    }
}
