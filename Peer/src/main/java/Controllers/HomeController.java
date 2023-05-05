package Controllers;

import Connections.ConnectionHandler;
import Messages.Message;
import Messages.MessageDescriptor;
import Messages.VectorClock;
import com.example.peer.Chat;
import com.example.peer.Peer;
import com.example.peer.PeerUI;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class HomeController implements Initializable {
    private final Peer peer = PeerUI.peer;
    private Chat currentlyOpenChat = null;

    @FXML
    private TextField tf_searchbar;

    @FXML
    private TextField tf_chat_message;

    @FXML
    private ScrollPane sp_chat;

    @FXML
    private VBox vbox_messages;

    @FXML
    private Button btn_new_convo;

    @FXML
    private Button btn_new_group;

    @FXML
    private Button btn_new_convo_back;

    @FXML
    private Label lbl_new_convo;

    @FXML
    private Label lbl_chat_name;

    @FXML
    private ListView<Chat> lv_new_convo_results;

    @FXML
    private ListView<Chat> lv_recent_contacts;

    @FXML
    private MenuItem mi_active_connections;

    @FXML
    private MenuItem mi_message_queue;

    @FXML
    private ImageView iv_group;

    @FXML
    private VBox vbox_activity_status;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Display configurations
        lv_new_convo_results.setFocusTraversable(false);
        lv_recent_contacts.setFocusTraversable(false);
        vbox_messages.prefWidthProperty().bind(sp_chat.widthProperty().subtract(12));
        vbox_messages.prefHeightProperty().bind(sp_chat.heightProperty().subtract(10));
        sp_chat.vvalueProperty().bind(vbox_messages.heightProperty());

        // Set content of listviews
        lv_new_convo_results.setCellFactory(lv -> setCells());
        lv_recent_contacts.setCellFactory(lv -> setCells());

        // Toggles
        btn_new_convo.setOnAction(actionEvent -> toggleNewConvo());
        btn_new_convo_back.setOnAction(actionEvent -> toggleNewConvo());
        mi_active_connections.setOnAction(actionEvent -> System.out.println(peer.getName() + " - " + peer.activeConnections.stream().map(ConnectionHandler::getRecipientName).collect(Collectors.joining(" "))));
        //mi_message_queue.setOnAction(actionEvent -> System.out.println(peer.getName() +" -> " + peer.getMessageQueue()));


        // Search bar text listener
        tf_searchbar.setOnAction(actionEvent -> {
            if (!tf_searchbar.getText().equals("")) {
                peer.setLastEvent(actionEvent);
                peer.sendMessageToAddress(peer.signMessage(MessageDescriptor.SEARCH, tf_searchbar.getText().toLowerCase()), peer.getServerAddress());
            }
        });

        // New group chat
        btn_new_group.setOnAction(actionEvent -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("choose_members.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 418, 400);
                Stage popupStage = new Stage();
                popupStage.initModality(Modality.APPLICATION_MODAL);

                popupStage.setScene(scene);
                popupStage.showAndWait();
                toggleNewConvo();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Sending messages
        tf_chat_message.setOnAction(actionEvent -> {
            String messageToSend = tf_chat_message.getText();
            if (!messageToSend.isEmpty()) {
                // Send message
                sendMessage(messageToSend, currentlyOpenChat);
            }
        });

    }

    private void sendMessage(String messageContent, Chat openChat) {
        Message messageToSend = peer.signMessage(MessageDescriptor.MESSAGE, openChat.getVectorClockAndIncrement(peer.getName()) + "|" + messageContent);
        messageToSend.setOriginalSender(peer.getName());
        messageToSend.setTtl(5);

        // Set chatUUID to null if it is a direct message
        if (openChat.getChatParticipants().size() == 1) {
            messageToSend.setChatUUID(null);
        }
        else {
            messageToSend.setChatUUID(openChat.getChatUUID());
        }

        // Deliver message
        peer.deliverMessage(messageToSend, openChat);

        // Add message to chat message history
        updateRecentChats(openChat);

    }

    private void openChat(Chat chat) {
        Platform.runLater(() -> {
            currentlyOpenChat = chat;
            tf_chat_message.setVisible(true);
            lbl_chat_name.setText(chat.getChatName());

            Platform.runLater(() -> {
                vbox_messages.getChildren().clear();
                Message previousMessage = null;
                for (Message message : chat.getCrbDeliveredMessages()) {
                    displayMessage(message, (previousMessage != null && previousMessage.getOriginalSender().equals(message.getOriginalSender())));
                    previousMessage = message;
                }
            });

        });
    }

    private void displayMessage(Message message, boolean sameSender) {
        // Display message
        HBox hBox = new HBox();
        hBox.setPadding(new Insets(1, 4, 1, 10));

        // Set TextFlows
        //Text messageText = new Text(message.getMessageContent().split("\\|", 2)[1]);
        Text messageText = new Text(message.getMessageContent());
        messageText.setStyle("-fx-font: 14 Berlin-Sans-FB");
        Text messageTime = new Text("   " + message.getDateTime().getHour() + ":" + (message.getDateTime().getMinute() < 10 ? "0" : "") + message.getDateTime().getMinute());

        TextFlow textFlow = new TextFlow();
        textFlow.setPadding(new Insets(5, 10, 5, 10));


        if (message.getSourceUsername().equals(peer.getName())) {
            hBox.setAlignment(Pos.CENTER_RIGHT);
            messageText.setFill(Color.WHITE);
            messageTime.setFill(Color.WHITE);
            textFlow.setStyle("-fx-background-radius: 50 50 50 50; -fx-background-color: rgb(15, 125, 242); ");

            // Delivered participants tooltip
            textFlow.setOnMouseEntered(e -> {
                StringBuilder tooltipText = new StringBuilder("Message delivered to: ");

                // Append all delivered to recipients to tooltip text
                for (Map.Entry<String, Boolean> recipient : message.getDelivered().entrySet()) {
                    if (recipient.getValue()) {
                        tooltipText.append(" ").append(recipient.getKey());
                    }
                }

                // Install tooltip
                Tooltip tooltip = new Tooltip(tooltipText.toString());
                Tooltip.install(textFlow, tooltip);
            });

        }
        // Else if message is from SYSTEM
        else {
            if (message.getSourceUsername().equals("SYSTEM")) {
                if (message.getMessageDescriptor().equals(MessageDescriptor.CREATE_GROUP)) {
                    // Set chat creation message
                    messageText.setText(message.getMessageContent().split("\\|", 2)[1].split(",")[0] + " has created the chat");
                }
                hBox.setAlignment(Pos.CENTER);
                messageText.setStyle("-fx-text-fill: rgb(134,134,134); -fx-font: 11 Berlin-Sans-FB;");
            }
            // Else message is from non-sender peer
            else {
                // If chat is group chat, show username of sender
                if (currentlyOpenChat.getChatParticipants().size() > 1) {
                    if (!sameSender) {
                        Text usernameText = new Text(message.getOriginalSender() + "\n");
                        usernameText.setStyle("-fx-font-size: 13; -fx-fill: rgb(195,62,34);");
                        hBox.setPadding(new Insets(10, 4, 1, 10));
                        textFlow.getChildren().add(usernameText);
                    }
                }
                hBox.setAlignment(Pos.CENTER_LEFT);
                textFlow.setStyle("-fx-background-radius: 50 50 50 50; -fx-background-color: rgb(232, 232, 235);");
            }
        }

        // Set components
        textFlow.getChildren().addAll(messageText, messageTime);
        hBox.getChildren().add(textFlow);

        Platform.runLater(
                () -> {
                    tf_chat_message.clear();
                    vbox_messages.getChildren().add(hBox);
                    lv_new_convo_results.setCellFactory(lv -> setCells());
                    lv_recent_contacts.setCellFactory(lv -> setCells());
                });

    }

    public void updateRecentChats(Chat chat) {
        // Refresh chat if currently open chat is the same
        if (this.currentlyOpenChat != null && this.currentlyOpenChat.equals(chat)) {
            openChat(chat);
        }

        // Move chat to top of listview
        ObservableList<Chat> items = lv_recent_contacts.getItems();
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (chat.getChatUUID() == null) {
                if (chat.getChatParticipants().equals(items.get(i).getChatParticipants())) {
                    index = i;
                    break;
                }
            }
            else if (items.get(i).getChatUUID() != null && items.get(i).getChatUUID().equals(chat.getChatUUID())) {
                index = i;
                break;
            }
        }

        if (index > 0) {
            Chat item = items.remove(index);
            items.add(0, item);
        }

        if (index == -1) {
            items.add(0, chat);
        }

        Platform.runLater(() -> lv_recent_contacts.scrollTo(0));

    }

    public void updateUserSearchLV(String[] users) {
        Chat[] chats = new Chat[users.length];

        for (int i = 0; i < users.length; i++) {
            boolean chatExists = false;

            // Set chat listview to existing chat if it already exists with that user
            for (Chat activeChat : peer.getActiveChats()) {
                if (activeChat.getChatParticipants().equals(new HashSet<>(Collections.singleton(users[i])))) {
                    chats[i] = activeChat;
                    chatExists = true;
                    break;
                }
            }

            if (!chatExists) {
                chats[i] = new Chat(null, new HashSet<>(Collections.singleton(users[i])), peer.getName());
            }

        }
        Platform.runLater(() -> lv_new_convo_results.setItems(FXCollections.observableArrayList(chats)));
    }

    private ListCell<Chat> setCells() {
        ListCell<Chat> cell = new ListCell<>() {
            @Override
            protected void updateItem(Chat item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    TextFlow textFlow = new TextFlow();
                    textFlow.setStyle("-fx-font: 16 Berlin-Sans-FB;");

                    Text chatName = new Text(item.getChatName());
                    chatName.setStyle("-fx-font-weight: bold;");
                    textFlow.getChildren().add(chatName);

                    ConcurrentLinkedDeque<Message> chatHistory = item.getCrbDeliveredMessages();
                    if (chatHistory.size() > 0) {
                        Text lastMessage = new Text(chatHistory.getLast().getMessageContent().split("\\|", 2)[1]);
                        lastMessage.setStyle("-fx-font-weight: lighter; -fx-font-size: 12;");
                        textFlow.getChildren().add(new Text(System.lineSeparator()));
                        textFlow.getChildren().add(lastMessage);
                    }
                    Platform.runLater(() -> setGraphic(textFlow));
                }
            }
        };

        cell.setOnMouseClicked(event -> {
            if (!cell.isEmpty()) {
                Chat item = cell.getItem();
                openChat(item);
            }
        });

        return cell;
    }

    private void toggleNewConvo() {
        // Toggle new convo elements
        if (lbl_new_convo.isVisible()) {
            btn_new_convo.setVisible(true);
            lbl_new_convo.setVisible(false);
            btn_new_convo_back.setVisible(false);
            btn_new_group.setVisible(false);
            lv_new_convo_results.setVisible(false);
            lv_new_convo_results.getItems().clear();
            lv_recent_contacts.setVisible(true);
            iv_group.setVisible(false);
            tf_searchbar.setPromptText("Search");
            tf_searchbar.clear();
        } else {
            lbl_new_convo.setVisible(true);
            btn_new_convo_back.setVisible(true);
            lv_recent_contacts.setVisible(false);
            lv_new_convo_results.setVisible(true);
            iv_group.setVisible(true);
            tf_searchbar.setPromptText("Search by username");
            btn_new_convo.setVisible(false);
            btn_new_group.setVisible(true);
            tf_searchbar.clear();
        }
    }
}
