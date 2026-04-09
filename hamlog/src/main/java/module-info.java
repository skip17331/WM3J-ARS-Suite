module com.hamlog.app {
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

    opens com.hamlog.app        to javafx.fxml;
    opens com.hamlog.controller to javafx.fxml;
    opens com.hamlog.model      to javafx.base, com.fasterxml.jackson.databind;
    opens com.hamlog.plugin     to com.fasterxml.jackson.databind;

    exports com.hamlog.app;
    exports com.hamlog.controller;
    exports com.hamlog.model;
    exports com.hamlog.db;
    exports com.hamlog.civ;
    exports com.hamlog.cluster;
    exports com.hamlog.macro;
    exports com.hamlog.plugin;
    exports com.hamlog.export;
    exports com.hamlog.util;
    exports com.hamlog.i18n;
}
