module org.poltanov.server {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.poltanov.server to javafx.fxml;
    exports org.poltanov.server;
}