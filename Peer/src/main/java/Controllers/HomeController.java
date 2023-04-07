package Controllers;

import Messages.Message;
import Messages.MessageDescriptor;
import com.example.peer.Chat;
import com.example.peer.Peer;
import com.example.peer.PeerUI;
import com.example.peer.StoredMessage;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

public class HomeController implements Initializable {
    private final Peer peer = PeerUI.peer;
    private Chat currentlyOpenChat;

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

        lv_new_convo_results.setFocusTraversable(false);
        lv_recent_contacts.setFocusTraversable(false);

        vbox_messages.prefWidthProperty().bind(sp_chat.widthProperty().subtract(12));
        vbox_messages.prefHeightProperty().bind(sp_chat.heightProperty().subtract(10));

        sp_chat.vvalueProperty().bind(vbox_messages.heightProperty());

        // Search bar text listener
        tf_searchbar.setOnAction(actionEvent -> {
            if (!tf_searchbar.getText().equals("")) {
                peer.setLastEvent(actionEvent);
                peer.sendMessage(MessageDescriptor.SEARCH, tf_searchbar.getText().toLowerCase(), 1, null, null, peer.getServerAddress());
            }
        });

        lv_new_convo_results.setCellFactory(lv -> setCells());
        lv_recent_contacts.setCellFactory(lv -> setCells());

        // Sending messages
        tf_chat_message.setOnAction(actionEvent -> {
            String messageToSend = tf_chat_message.getText();
            if (!messageToSend.isEmpty()) {
                // Send message
                sendMessage(messageToSend);
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

        btn_new_convo.setOnAction(actionEvent -> toggleNewConvo());
        btn_new_convo_back.setOnAction(actionEvent -> toggleNewConvo());

        mi_active_connections.setOnAction(actionEvent -> System.out.println(peer.getName() +" -> " + peer.activeConnections));
        mi_message_queue.setOnAction(actionEvent -> System.out.println(peer.getName() +" -> " + peer.getMessageQueue()));

    }

    public void setFailedSend(StoredMessage message) {
        refreshChat(message.getChatUUID());
    }


    public void updateRecentChats(Chat chat) {
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
            chats[i] = new Chat(new HashSet<>(Collections.singleton(users[i])), peer.getName());
        }
        Platform.runLater(() -> lv_new_convo_results.setItems(FXCollections.observableArrayList(chats)));
    }

    private void openChat(UUID chatUUID) {
        openChat(peer.getActiveChat(chatUUID));
    }

    private void openChat(Chat chat) {
        Platform.runLater(() -> {
            tf_chat_message.setVisible(true);
            lbl_chat_name.setText(chat.getChatName());
            setActivity(chat.getChatUUID());
            vbox_messages.getChildren().clear();
            currentlyOpenChat = chat;
            for (StoredMessage message : chat.getMessageHistory()) {
                displayMessage(message);
            }
        });
    }

    public void setActivity(UUID chatUUID) {
        Platform.runLater(() -> {
            if (currentlyOpenChat != null && currentlyOpenChat.getChatUUID() != null && currentlyOpenChat.getChatUUID().equals(chatUUID)) {
                vbox_activity_status.getChildren().clear();

                TextFlow activityStatus = new TextFlow();
                activityStatus.setTextAlignment(TextAlignment.RIGHT);
                activityStatus.setPadding(new Insets(0, 20, 0, 0));

                Text activityText = new Text();
                Circle circle = new Circle(6);

                if (currentlyOpenChat.getChatParticipants().size() == 1) {
                    if (peer.getActiveConnections().containsKey(currentlyOpenChat.getChatParticipants().iterator().next())) {
                        activityText.setText("Online  ");
                        circle.setFill(Color.GREEN);
                    } else {
                        activityText.setText("Status Unknown  ");
                        circle.setFill(Color.GREY);
                    }

                    activityStatus.getChildren().add(activityText);
                    activityStatus.getChildren().add(circle);
                    vbox_activity_status.getChildren().add(activityStatus);
                }
            }
        });

    }

    public void refreshChat(UUID chatUUID) {
        if (currentlyOpenChat.getChatUUID().equals(chatUUID)) {
            openChat(chatUUID);
        }
    }

    private void sendMessage(String messageToSend) {
        UUID uuid = UUID.randomUUID();

        if (currentlyOpenChat.getChatParticipants().size() == 1) {
            peer.sendMessage(MessageDescriptor.MESSAGE, messageToSend, null, uuid, 1, currentlyOpenChat.getChatParticipants().iterator().next());
            peer.sendMessage(MessageDescriptor.MESSAGE, messageToSend, null, uuid, 1, peer.getName());
        }

        // Send message to each participant
        else {
            for (String participant : currentlyOpenChat.getAllChatParticipants()) {
                peer.sendMessage(MessageDescriptor.MESSAGE, messageToSend, currentlyOpenChat.getChatUUID(), uuid, 1, participant.stripLeading());
            }
        }

//      Add message to message history
        StoredMessage storedMessage = new StoredMessage(uuid, currentlyOpenChat.getChatUUID(), peer.getName(), messageToSend, LocalDateTime.now());
        peer.addChatHistory(currentlyOpenChat, storedMessage);
        updateRecentChats(currentlyOpenChat);

        // Display message on GUI
        displayMessage(storedMessage);
    }

    private boolean messageBelongsToChat(StoredMessage message, Chat chat) {
        if (message.getChatUUID() != null && message.getChatUUID().equals(chat.getChatUUID())) {
            return true;
        } else {
            return message.getChatUUID() == null && (message.getSender().equals(chat.getChatParticipants().iterator().next()) || message.getSender().equals(peer.getName()) ) && chat.getChatParticipants().size() == 1;
        }
    }

    private boolean messageIsEcho(StoredMessage message, Chat chat) {
        for (StoredMessage storedMessage : chat.getMessageHistory()) {
            if (storedMessage.getUuid().equals(message.getUuid()) && !storedMessage.getSender().equals(message.getSender())) {
                return true;
            }
        }
        return false;
    }

    public void displayMessage(StoredMessage message) {
        if (currentlyOpenChat != null) {
            if (messageBelongsToChat(message, currentlyOpenChat) && !messageIsEcho(message, currentlyOpenChat)) {
                // Display message
                HBox hBox = new HBox();
                hBox.setPadding(new Insets(1, 4, 1, 10));

                // Set TextFlows
                Text messageText = new Text(message.getMessageContent());
                messageText.setStyle("-fx-font: 14 Berlin-Sans-FB");
                Text messageTime = new Text("   " + message.getDateTime().getHour() + ":" + (message.getDateTime().getMinute() < 10 ? "0" : "") + message.getDateTime().getMinute());

                TextFlow textFlow = new TextFlow();
                textFlow.setPadding(new Insets(5, 10, 5, 10));

                // If sent or received
                if (message.getSender().equals("SYSTEM")) {
                    hBox.setAlignment(Pos.CENTER);
                    messageText.setStyle("-fx-text-fill: rgb(134,134,134);");
                    messageTime.setStyle("-fx-font: 11 Berlin-Sans-FB;");
                } else {
                    messageTime.setStyle("-fx-font: 10 Berlin-Sans-FB;");
                    if (message.getSender().equals(peer.getName())) {
                        hBox.setAlignment(Pos.CENTER_RIGHT);
                        messageText.setFill(Color.WHITE);
                        messageTime.setFill(Color.WHITE);

                        // If message failed sending
                        if (message.isFailed()) {
                            textFlow.setStyle("-fx-background-radius: 50 50 50 50; -fx-background-color: rgb(255, 59, 47); -fx-cursor: hand");
                            textFlow.setOnMouseClicked(mouseEvent -> displayResendAlert(message));
                        } else {
                            textFlow.setStyle("-fx-background-radius: 50 50 50 50; -fx-background-color: rgb(15, 125, 242); ");
                        }
                    } else {
                        hBox.setAlignment(Pos.CENTER_LEFT);
                        textFlow.setStyle("-fx-background-radius: 50 50 50 50; -fx-background-color: rgb(232, 232, 235);");
                    }
                }

                if (!message.getSender().equals("SYSTEM") && !message.getSender().equals(peer.getName())) {
                    if (currentlyOpenChat.getChatParticipants().size() > 1) {
                        Text usernameText = new Text(message.getSender() + "\n");
                        usernameText.setStyle("-fx-font-size: 13; -fx-fill: rgb(195,62,34);");
                        textFlow.getChildren().addAll(usernameText, messageText, messageTime);
                    } else {
                        textFlow.getChildren().addAll(messageText, messageTime);
                    }
                } else {
                    textFlow.getChildren().addAll(messageText, messageTime);
                }

                hBox.getChildren().add(textFlow);


                Platform.runLater(
                        () -> vbox_messages.getChildren().add(hBox)
                );

            }

            Platform.runLater(() -> {
                tf_chat_message.clear();
                lv_new_convo_results.setCellFactory(lv -> setCells());
                lv_recent_contacts.setCellFactory(lv -> setCells());
            });

        }
    }

    private void displayResendAlert(StoredMessage message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Send Failed");
        alert.setHeaderText("This message failed to send");
        alert.setContentText("Choose an option:");

        ButtonType buttonTypeDelete = new ButtonType("Delete");
        ButtonType buttonTypeResend = new ButtonType("Resend");

        alert.getButtonTypes().setAll(buttonTypeDelete, buttonTypeResend);

        alert.showAndWait().ifPresent(response -> {
            if (response == buttonTypeDelete) {
                peer.getActiveChat(message.getChatUUID()).getMessageHistory().remove(message);
                openChat(message.getChatUUID());
            } else if (response == buttonTypeResend) {
                sendMessage(message.getMessageContent());
                peer.getActiveChat(message.getChatUUID()).getMessageHistory().remove(message);
                openChat(message.getChatUUID());
            }
        });
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

                    ArrayList<StoredMessage> chatHistory = item.getMessageHistory();
                    if (chatHistory.size() > 0) {
                        Text lastMessage = new Text(chatHistory.get(chatHistory.size() - 1).getMessageContent());
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
