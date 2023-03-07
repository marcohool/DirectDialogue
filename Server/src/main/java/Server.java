import Connections.ConnectionHandler;
import Nodes.Node;

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
    }

}
