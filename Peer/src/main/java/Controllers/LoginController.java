package Controllers;

import Messages.MessageDescriptor;
import com.example.peer.Peer;
import com.example.peer.PeerUI;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    private final Peer peer = PeerUI.peer;

    @FXML
    private Button bt_signup;

    @FXML
    private Button bt_login;

    @FXML
    private TextField tf_username;

    @FXML
    private PasswordField pf_password;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        bt_signup.setOnAction(actionEvent -> {
            peer.setLastEvent(actionEvent);
            peer.changeScene(actionEvent, "signup.fxml");
        });

        bt_login.setOnAction(actionEvent -> {
            peer.setLastEvent(actionEvent);
            peer.sendMessage(MessageDescriptor.LOGIN, tf_username.getText() + " " + pf_password.getText(), 1, peer.getServerAddress());
        });

    }

}
