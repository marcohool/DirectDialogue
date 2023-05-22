package Controllers;

import Messages.MessageDescriptor;
import com.example.peer.Peer;
import com.example.peer.PeerUI;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Optional;
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
            try {
                peer.sendMessageToAddress(peer.signMessage(MessageDescriptor.LOGIN, tf_username.getText() + " " + pf_password.getText()), peer.getServerAddress());
            } catch (IOException e) {
                e.printStackTrace();

                TextInputDialog dialog = new TextInputDialog("");

                dialog.setTitle("Server Error");
                dialog.setHeaderText("There was an error connecting to the server, this may be due to an configuration error. \nPlease enter below the IP address the server is running on");
                dialog.setContentText("IP Address (e.g. 192.168.56.1)");

                Optional<String> result = dialog.showAndWait();

                result.ifPresent(name -> {
                    System.out.println(result.get());
                    peer.setServerAddress(result.get());
                });
            }
        });



    }
}
