package Controllers;

import com.example.peer.Chat;
import com.example.peer.Peer;
import com.example.peer.PeerUI;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;

public class GroupChatController implements Initializable {
    private final Peer peer = PeerUI.peer;

    @FXML
    private ListView<String> lv_contacts;

    @FXML
    private Button btn_create_group;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        List<String> selectedUsers = new ArrayList<>();
        Set<String> peerContacts = new HashSet<>();

        for (Chat chat : peer.getActiveChats()) {
            peerContacts.addAll(chat.getChatParticipants());
        }
        lv_contacts.getItems().addAll(peerContacts);

        lv_contacts.setCellFactory(CheckBoxListCell.forListView(item -> {
            BooleanProperty observable = new SimpleBooleanProperty();
            observable.addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    selectedUsers.add(item);
                } else {
                    selectedUsers.remove(item);
                }
            });
            return observable;
        }));

        btn_create_group.setOnAction(event -> {
            if (selectedUsers.size() <= 1) {
                peer.displayAlert("Please select 2 or more contacts");
            } else {
                peer.createGroup(peerContacts);
                Stage stage = (Stage) btn_create_group.getScene().getWindow();
                stage.close();
            }
        });



    }
}
