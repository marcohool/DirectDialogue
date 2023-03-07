package Nodes;

import Connections.ConnectionHandler;

public interface INode {

    void handleMessage(String message, ConnectionHandler connectionHandler);

}
