import Connections.ConnectionHandler;
import Nodes.Node;

import java.sql.*;
import java.util.Arrays;

public class Server extends Node {

    public Server(String address, int port)  {
        super(address, port);
    }

    public static void main(String[] args) {

        // Start server on specified IP and port
        Server server = new Server("192.168.68.63", 1926);

    }

    public void handleMessage(String message, ConnectionHandler connectionHandler) {
        System.out.println(message + " received from " + connectionHandler.getRecipientAddress());

        String[] split = message.split("\\s+");

        switch (split[0]){
            // login [username] [password]
            case "login":
                if (split.length < 3) {
                    connectionHandler.sendMessage("error Please enter a valid username & password");
                } else {
                    String response = Database.loginUser(split[1], split[2]);
                    connectionHandler.sendMessage(response);
                }
                break;
            // signup [username] [password]
            case "signup":
                if (split.length < 3) {
                    connectionHandler.sendMessage("error Please enter a valid username & password");
                } else {
                    String response = Database.registerUser(split[1], split[2]);
                    connectionHandler.sendMessage(response);
                }
                break;
            // search [username query]
            case "search":
                if (split.length > 1) {
                    String response = Database.searchUser(String.join(" ", Arrays.copyOfRange(split, 1, split.length)));
                    connectionHandler.sendMessage(response);
                }
                break;
        }

    }
}


class Database {

    static String searchUser(String username) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuilder returnMessage = new StringBuilder("error SQLException");

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/p2p-chat", "root", "123123");
            ps = conn.prepareStatement("SELECT username FROM users WHERE username LIKE ? LIMIT 10");
            ps.setString(1, "%" + username + "%");
            rs = ps.executeQuery();

            returnMessage = new StringBuilder("results");
            while (rs.next()) {
                returnMessage.append(" ").append(rs.getString("username"));
            }

        } catch (SQLException e) {
            System.out.println("Error searching for user: " + e.getMessage());
        } finally {
            closeResources(conn, new PreparedStatement[]{ps}, rs);
        }

        return returnMessage.toString();
    }


    static String loginUser(String username, String password) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String returnMessage = "error SQLException";

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/p2p-chat", "root", "123123");
            ps = conn.prepareStatement("SELECT password FROM users WHERE username = ?");
            ps.setString(1, username);
            rs = ps.executeQuery();

            if (!rs.isBeforeFirst()) {
                returnMessage = "error Invalid username/password";
            } else {
                while (rs.next()) {
                    String retrievedPassword = rs.getString("password");
                    if (retrievedPassword.equals(password)) {
                        returnMessage = "login success";
                    } else {
                        returnMessage = "error Invalid username/password";
                    }
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
        String returnMessage = "error SQLException";

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/p2p-chat", "root", "123123");

            // Check if username already exists
            psCheck = conn.prepareStatement("SELECT * FROM users WHERE USERNAME = ?");
            psCheck.setString(1, username);
            rs = psCheck.executeQuery();

            if (rs.isBeforeFirst()) {
                returnMessage = "Username taken";
            }

            // Else register user
            else {
                if (!username.equals("") && !password.equals("")) {

                    psInsert = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?) ");
                    psInsert.setString(1, username);
                    psInsert.setString(2, password);
                    psInsert.executeUpdate();

                    returnMessage =  "signup success";
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
                System.out.println("Error closing resultset: " + e.getMessage());
            }
        }
        for (PreparedStatement preparedStatement : ps) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                System.out.println("Error closing prepared statement: " + e.getMessage());
            }
        }
    }

}