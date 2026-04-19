module com.jlog.engine {
    requires transitive java.sql;
    requires java.prefs;
    requires org.xerial.sqlitejdbc;
    requires transitive com.fasterxml.jackson.databind;
    requires jssc;
    requires org.java_websocket;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;

    opens com.jlog.model  to com.fasterxml.jackson.databind;
    opens com.jlog.plugin to com.fasterxml.jackson.databind;

    exports com.jlog.db;
    exports com.jlog.civ;
    exports com.jlog.cluster;
    exports com.jlog.plugin;
    exports com.jlog.export;
    exports com.jlog.util;
    exports com.jlog.model;
    exports com.jlog.i18n;
}
