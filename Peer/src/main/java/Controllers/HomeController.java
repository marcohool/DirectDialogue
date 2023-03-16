package Controllers;

import Messages.Message;
import Messages.MessageDescriptor;
import com.example.peer.Peer;
import com.example.peer.PeerUI;
import com.example.peer.StoredMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

public class HomeController implements Initializable {
    private final Peer peer = PeerUI.peer;

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
    private Button btn_new_convo_back;

    @FXML
    private Label lbl_new_convo;

    @FXML
    private Label lbl_chat_name;

    @FXML
    private ListView<String> lv_new_convo_results;

    @FXML
    private ListView<String> lv_recent_contacts;

    @FXML
    private MenuItem mi_active_connections;

    @FXML
    private MenuItem mi_message_queue;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        lv_new_convo_results.setFocusTraversable(false);
        lv_recent_contacts.setFocusTraversable(false);

        vbox_messages.prefWidthProperty().bind(sp_chat.widthProperty().subtract(10));
        vbox_messages.prefHeightProperty().bind(sp_chat.heightProperty().subtract(10));

        // Search bar text listener
        tf_searchbar.setOnAction(actionEvent -> {
            if (!tf_searchbar.getText().equals("")) {
                peer.setLastEvent(actionEvent);
                peer.sendMessage(MessageDescriptor.SEARCH, tf_searchbar.getText().toLowerCase(), 1, peer.getServerAddress());
            }
        });

        lv_new_convo_results.setCellFactory(lv -> setCells());
        lv_recent_contacts.setCellFactory(lv -> setCells());

        // Sending messages
        tf_chat_message.setOnAction(actionEvent -> {
            String messageToSend = tf_chat_message.getText();
            if (!messageToSend.isEmpty()) {
                // Send message
                Message sentMessage = peer.sendMessage(MessageDescriptor.MESSAGE, tf_chat_message.getText(), 1, lbl_chat_name.getText());

                // Add message to message history
                StoredMessage storedMessage = new StoredMessage(sentMessage.getUuid(), lbl_chat_name.getText(), peer.getName(), messageToSend, LocalDateTime.now());
                peer.addChatHistory(lbl_chat_name.getText(), storedMessage);

                updateRecentChats(lbl_chat_name.getText());

                // Display message on GUI
                displayMessage(storedMessage);

            }
        });

        btn_new_convo.setOnAction(actionEvent -> toggleNewConvo());

        btn_new_convo_back.setOnAction(actionEvent -> toggleNewConvo());

        mi_active_connections.setOnAction(actionEvent -> System.out.println(peer.getName() +" -> " + peer.activeConnections));
        mi_message_queue.setOnAction(actionEvent -> System.out.println(peer.getName() +" -> " + peer.getMessageQueue()));

    }

    public void setFailedSend(StoredMessage message) {
        if (message.getChatUsername().equals(lbl_chat_name.getText())) {
            openChat(message.getChatUsername());
        }
    }

    public void updateRecentChats(String chatToTop) {
        // Move chat to top of listview
        ObservableList<String> items = lv_recent_contacts.getItems();
        int index = lv_recent_contacts.getItems().indexOf(chatToTop);

        if (index > 0) {
            String item = items.remove(index);
            items.add(0, item);
        }

        if (index == -1) {
            items.add(0, chatToTop);
        }

        Platform.runLater(() -> lv_recent_contacts.scrollTo(0));

    }

    public void updateUserSearchLV(String[] users) {
        Platform.runLater(() -> lv_new_convo_results.setItems(FXCollections.observableArrayList(users)));
    }

    private void openChat(String username) {
        Platform.runLater(() -> {
            tf_chat_message.setVisible(true);
            lbl_chat_name.setText(username);
            vbox_messages.getChildren().clear();

            ArrayList<StoredMessage> messages = peer.getChatHistory().get(username);
            if (messages != null) {
                for (StoredMessage message : messages) {
                    displayMessage(message);
                }
            }
        });
    }

    public void displayMessage(StoredMessage message) {
        if (message.getChatUsername().equals(lbl_chat_name.getText())) {
            // Display message
            HBox hBox = new HBox();
            hBox.setPadding(new Insets(1, 4, 1, 10));

            // Set TextFlows
            Text messageText = new Text(message.getMessageContent());
            messageText.setStyle("-fx-font: 14 Berlin-Sans-FB");
            Text messageTime = new Text("   " + message.getDateTime().getHour() + ":" + (message.getDateTime().getMinute() < 10 ? "0" : "") + message.getDateTime().getMinute());
            messageTime.setStyle("-fx-font: 10 Berlin-Sans-FB;");

            TextFlow textFlow = new TextFlow();
            textFlow.getChildren().addAll(messageText, messageTime);
            textFlow.setPadding(new Insets(5, 10, 5, 10));

            // If sent or received
            if (message.getSender().equals(peer.getName())) {
                hBox.setAlignment(Pos.CENTER_RIGHT);
                messageText.setFill(Color.WHITE);
                messageTime.setFill(Color.WHITE);

                // If message failed sending
                if (message.isFailed()) {
                    textFlow.setStyle("-fx-background-radius: 50 50 50 50; -fx-background-color: rgb(255, 59, 47); -fx-cursor: hand");
                    textFlow.setOnMouseClicked(mouseEvent -> displayResendAlert());
                } else {
                    textFlow.setStyle("-fx-background-radius: 50 50 50 50; -fx-background-color: rgb(15, 125, 242); ");
                }
            } else {
                hBox.setAlignment(Pos.CENTER_LEFT);
                textFlow.setStyle("-fx-background-radius: 50 50 50 50; -fx-background-color: rgb(232, 232, 235);");
            }

            hBox.getChildren().add(textFlow);

            Platform.runLater(
                    () -> {
                        vbox_messages.getChildren().add(hBox);
                        tf_chat_message.clear();
                        lv_new_convo_results.setCellFactory(lv -> setCells());
                        lv_recent_contacts.setCellFactory(lv -> setCells());
                    }
            );

        }
    }

    private void displayResendAlert() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Send Failed");
        alert.setHeaderText("This message failed to send");
        alert.setContentText("Choose an option:");

        ButtonType buttonTypeDelete = new ButtonType("Delete");
        ButtonType buttonTypeResend = new ButtonType("Resend");

        alert.getButtonTypes().setAll(buttonTypeDelete, buttonTypeResend);

        alert.showAndWait().ifPresent(response -> {
            if (response == buttonTypeDelete) {
                System.out.println("delte");
            } else if (response == buttonTypeResend) {
                System.out.println("resend");
            }
        });
    }

    private ListCell<String> setCells() {
        ListCell<String> cell = new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    TextFlow textFlow = new TextFlow();
                    textFlow.setStyle("-fx-font: 16 Berlin-Sans-FB;");

                    Text username = new Text(item);
                    username.setStyle("-fx-font-weight: bold;");

                    textFlow.getChildren().add(username);

                    ArrayList<StoredMessage> chatHistory = peer.getChatHistory().get(item);
                    if (chatHistory != null) {
                        Text lastMessage = new Text(chatHistory.get(chatHistory.size()-1).getMessageContent());
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
                String item = cell.getItem();
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
            lv_new_convo_results.setVisible(false);
            lv_new_convo_results.getItems().clear();
            lv_recent_contacts.setVisible(true);
            tf_searchbar.setPromptText("Search");
            tf_searchbar.clear();
        } else {
            lbl_new_convo.setVisible(true);
            btn_new_convo_back.setVisible(true);
            lv_recent_contacts.setVisible(false);
            lv_new_convo_results.setVisible(true);
            tf_searchbar.setPromptText("Search by username");
            btn_new_convo.setVisible(false);
            tf_searchbar.clear();
        }
    }

}
