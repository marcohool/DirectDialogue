import Connections.ConnectionHandler;
import Messages.Message;
import Messages.MessageDescriptor;
import Nodes.Node;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public class Server extends Node {

    public Server(String name, InetSocketAddress address) {
        super(name, address);
    }

    public static void main(String[] args) throws UnknownHostException {

        // Start server on specified IP and port
        Server server = new Server("server1", new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), 1926));

    }

    public void handleMessage(Message message, ConnectionHandler connectionHandler) {
        // If message TTL is > 0
        if (message.getTtl() > 0) {
            message.decrementTtl();
            String[] messageSplit = message.getMessageContent().split(" ");

            switch (message.getMessageDescriptor()) {
                case LOGIN:

                    // If login message is invalid
                    if (messageSplit.length < 2) {
                        connectionHandler.sendMessage(signMessage(MessageDescriptor.LOGIN_ERROR, "Please enter a valid username & password"));
                    }
                    // Login user
                    else {
                        MessageDescriptor response = Database.loginUser(messageSplit[0], messageSplit[1]);
                        if (response.equals(MessageDescriptor.LOGIN_SUCCESS)) {
                            connectionHandler.sendMessage(signMessage(MessageDescriptor.LOGIN_SUCCESS, messageSplit[0]));
                        }
                        else {
                            connectionHandler.sendMessage(signMessage(MessageDescriptor.LOGIN_ERROR, null));
                        }
                    }
                    break;

                case SIGNUP:
                    if (messageSplit.length < 2) {
                        connectionHandler.sendMessage(signMessage(MessageDescriptor.SIGNUP_ERROR, "No username/password"));
                    } else {
                        String response = Database.registerUser(messageSplit[0], messageSplit[1]);
                        if (response.equals("success")) {
                            connectionHandler.sendMessage(signMessage(MessageDescriptor.SIGNUP_SUCCESS, messageSplit[0]));
                        } else {
                            connectionHandler.sendMessage(signMessage(MessageDescriptor.SIGNUP_ERROR, response));
                        }
                    }

                case SEARCH:
                    if (messageSplit.length > 0) {
                        ArrayList<String> responses = Database.searchUser(message.getMessageContent());
                        responses.remove(message.getSourceUsername());
                        connectionHandler.sendMessage(signMessage(MessageDescriptor.SEARCH, String.join(" ", responses)));
                    }
                    break;
            }
        }
    }

}

class Database {

    static ArrayList<String> searchUser(String username) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        ArrayList<String> searchResults = new ArrayList<>();

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/p2p-chat", "root", "123123");
            ps = conn.prepareStatement("SELECT username FROM users WHERE username LIKE ? LIMIT 10");
            ps.setString(1, "%" + username + "%");
            rs = ps.executeQuery();

            while (rs.next()) {
                searchResults.add(rs.getString("username"));
            }

        } catch (SQLException e) {
            System.out.println("Error searching for user: " + e.getMessage());
        } finally {
            closeResources(conn, new PreparedStatement[]{ps}, rs);
        }

        return searchResults;
    }


    static MessageDescriptor loginUser(String username, String password) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        MessageDescriptor returnMessage = MessageDescriptor.LOGIN_ERROR;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/p2p-chat", "root", "123123");
            ps = conn.prepareStatement("SELECT password FROM users WHERE username = ?");
            ps.setString(1, username);
            rs = ps.executeQuery();

            if (rs.isBeforeFirst()) {
                rs.next();
                if (rs.getString("password").equals(password)) {
                    returnMessage = MessageDescriptor.LOGIN_SUCCESS;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, new PreparedStatement[]{ps}, rs);
        }

        return returnMessage;

    }

    static String registerUser(String username, String password) {
        Connection conn = null;
        PreparedStatement psInsert = null;
        PreparedStatement psCheck = null;
        ResultSet rs = null;
        String returnMessage = "SQLException";

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/p2p-chat", "root", "123123");

            // Check if username already exists
            psCheck = conn.prepareStatement("SELECT * FROM users WHERE USERNAME = ?");
            psCheck.setString(1, username);
            rs = psCheck.executeQuery();

            if (rs.isBeforeFirst()) {
                returnMessage = "Username taken";
            } else {
                if (!username.equals("") && !password.equals("")) {

                    psInsert = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?) ");
                    psInsert.setString(1, username);
                    psInsert.setString(2, password);
                    psInsert.executeUpdate();

                    returnMessage = "success";
                } else {
                    returnMessage = "You must enter a valid username and password";
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, new PreparedStatement[]{psInsert, psCheck}, rs);
        }

        return returnMessage;
    }

    private static void closeResources(Connection conn, PreparedStatement[] ps, ResultSet rs) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                System.out.println("Error closing ResultSet: " + e.getMessage());
            }
        }
        for (PreparedStatement preparedStatement : ps) {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                System.out.println("Error closing prepared statement: " + e.getMessage());
            }
        }
    }
}
