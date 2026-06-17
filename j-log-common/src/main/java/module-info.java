/**
 * j-Log Common — DB-free shared primitives split out of {@code com.jlog.engine}
 * so non-logging consumers (e.g. j-bridge, which only needs the bandplan) don't
 * have to embed the whole engine (SQLite, scoring, plugins, PDFBox, …).
 */
module com.jlog.common {
    requires transitive com.fasterxml.jackson.databind;
    requires jssc;
    requires org.slf4j;
    requires java.desktop;   // IssueReporter → java.awt.Desktop (browser launch)

    // jackson reflectively binds the QSO/spot model POJOs
    opens com.jlog.model to com.fasterxml.jackson.databind;

    exports com.jlog.model;
    exports com.jlog.bandplan;
    exports com.jlog.civ;
    exports com.jlog.i18n;
    exports com.jlog.support;
}
