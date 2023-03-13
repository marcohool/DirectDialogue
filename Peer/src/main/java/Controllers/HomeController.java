package Controllers;

import Messages.Message;
import Messages.MessageDescriptor;
import com.example.peer.Peer;
import com.example.peer.PeerUI;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.ArrayList;
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

        // Listener for new conversation listview
        lv_new_convo_results.getSelectionModel().selectedItemProperty().addListener((observableValue, s, selected) -> {
            if (selected != null) {
                openChat(selected);
            }
        });


        tf_chat_message.setOnAction(actionEvent -> {
            String messageToSend = tf_chat_message.getText();
            if (!messageToSend.isEmpty()) {
                // Send message
                peer.sendMessage(MessageDescriptor.MESSAGE, tf_chat_message.getText(), 1, lbl_chat_name.getText());

                System.out.println(peer.getName() +" -> " + peer.activeConnections);
                displayMessage(messageToSend, true);
            }
        });

        vbox_messages.heightProperty().addListener((observableValue, number, t1) -> System.out.println("?"));

        btn_new_convo.setOnAction(actionEvent -> toggleNewConvo());

        btn_new_convo_back.setOnAction(actionEvent -> toggleNewConvo());

    }

    public void updateUserSearchLV(String[] users) {
        Platform.runLater(() -> lv_new_convo_results.setItems(FXCollections.observableArrayList(users)));
    }

    private void openChat(String username) {
        tf_chat_message.setVisible(true);
        lbl_chat_name.setText(username);
        vbox_messages.getChildren().clear();

        ArrayList<Message> messages = peer.getChatHistory().get(username);

        if (messages != null) {
            for (Message message : messages) {
                displayMessage(message.getMessageContent(), !message.getSourceUsername().equals(username));
            }
        }
    }

    private void displayMessage(String message, boolean sent) {
        // Display message
        HBox hBox = new HBox();
        hBox.setPadding(new Insets(1, 1, 1, 1));

        Text text = new Text(message);
        text.setStyle("-fx-font: 14 Berlin-Sans-FB");
        TextFlow textFlow = new TextFlow(text);
        textFlow.setPadding(new Insets(5, 10, 5, 10));

        if (sent) {
            hBox.setAlignment(Pos.CENTER_RIGHT);
            text.setFill(Color.WHITE);
            textFlow.setStyle("-fx-background-radius: 50 50 50 50; -fx-background-color: rgb(15, 125, 242); ");
        } else {
            hBox.setAlignment(Pos.CENTER_LEFT);
            textFlow.setStyle("-fx-background-radius: 50 50 50 50; -fx-background-color: rgb(232, 232, 235);");
        }


        hBox.getChildren().add(textFlow);
        vbox_messages.getChildren().add(hBox);
        tf_chat_message.clear();
    }

    private void toggleNewConvo() {
        // Toggle new convo elements
        if (lbl_new_convo.isVisible()) {
            btn_new_convo.setVisible(true);
            lbl_new_convo.setVisible(false);
            btn_new_convo_back.setVisible(false);
            lv_new_convo_results.setVisible(false);
            lv_recent_contacts.setVisible(true);
            tf_searchbar.setPromptText("Search");
        } else {
            lbl_new_convo.setVisible(true);
            btn_new_convo_back.setVisible(true);
            lv_recent_contacts.setVisible(false);
            lv_new_convo_results.setVisible(true);
            tf_searchbar.setPromptText("Search by username");
            btn_new_convo.setVisible(false);
        }
    }

}
