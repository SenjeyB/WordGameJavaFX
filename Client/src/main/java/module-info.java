module org.poltanov.client {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.poltanov.client to javafx.fxml;
    exports org.poltanov.client;
}