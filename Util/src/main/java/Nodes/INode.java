package Nodes;

import Connections.ConnectionHandler;
import Messages.Message;

public interface INode {

    void handleMessage(Message message, ConnectionHandler connectionHandler);

}
