module com.jlog.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires com.fasterxml.jackson.databind;
    requires jssc;
    requires org.java_websocket;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires java.prefs;

    opens com.jlog.app        to javafx.fxml;
    opens com.jlog.controller to javafx.fxml;
    opens com.jlog.model      to javafx.base, com.fasterxml.jackson.databind;
    opens com.jlog.plugin     to com.fasterxml.jackson.databind;

    exports com.jlog.app;
    exports com.jlog.controller;
    exports com.jlog.model;
    exports com.jlog.db;
    exports com.jlog.civ;
    exports com.jlog.cluster;
    exports com.jlog.macro;
    exports com.jlog.plugin;
    exports com.jlog.export;
    exports com.jlog.util;
    exports com.jlog.i18n;
}
