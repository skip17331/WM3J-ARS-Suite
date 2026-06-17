module com.jlog.engine {
    requires transitive java.sql;
    requires java.prefs;
    requires java.desktop;
    requires org.xerial.sqlitejdbc;
    requires transitive com.fasterxml.jackson.databind;
    // DB-free primitives (model/bandplan/civ/i18n) now live in j-log-common.
    // transitive so engine consumers keep seeing com.jlog.model etc. through us.
    requires transitive com.jlog.common;
    requires org.java_websocket;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires org.apache.pdfbox;

    opens com.jlog.plugin to com.fasterxml.jackson.databind;
    opens com.jlog.award  to com.fasterxml.jackson.databind;

    exports com.jlog.db;
    exports com.jlog.cluster;
    exports com.jlog.plugin;
    exports com.jlog.award;
    exports com.jlog.scoring;
    exports com.jlog.export;
    exports com.jlog.util;
}
