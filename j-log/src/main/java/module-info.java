module com.jlog.app {
    requires com.jlog.engine;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires org.slf4j;

    opens com.jlog.app        to javafx.fxml;
    opens com.jlog.controller to javafx.fxml;

    exports com.jlog.app;
    exports com.jlog.controller;
}
