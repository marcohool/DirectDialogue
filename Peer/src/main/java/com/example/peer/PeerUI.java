package com.example.peer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PeerUI extends Application {
    public static Peer peer = new Peer();

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("login.fxml"));

        // Set scene
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("DirectDialogue");
        stage.setScene(scene);

        // Show scene
        stage.show();

    }

    public void start() {
        launch();
    }


}
