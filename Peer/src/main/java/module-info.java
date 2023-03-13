module com.example.peer {
    requires javafx.controls;
    requires javafx.fxml;
    requires Util;

    opens com.example.peer to javafx.fxml;
    exports com.example.peer;

    opens Controllers to javafx.fxml;
    exports Controllers;

}
