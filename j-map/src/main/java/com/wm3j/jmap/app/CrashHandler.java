package com.wm3j.jmap.app;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Global uncaught-exception handler. Logs every stray exception via SLF4J
 * (so it lands in {@code logs/<app>.log}) and — when JavaFX is up — shows a
 * modal dialog with the stack trace and a "Copy to Clipboard" button.
 */
public final class CrashHandler {

    private static final Logger log = LoggerFactory.getLogger(CrashHandler.class);
    private static volatile boolean dialogOpen = false;

    private CrashHandler() {}

    public static void install(String appName) {
        Thread.setDefaultUncaughtExceptionHandler((t, ex) -> handle(appName, t, ex));
    }

    private static void handle(String appName, Thread t, Throwable ex) {
        String stack = stackToString(ex);
        log.error("Uncaught exception in thread '{}':\n{}", t.getName(), stack);
        if (dialogOpen) return;
        try {
            if (Platform.isFxApplicationThread()) showDialog(appName, stack);
            else Platform.runLater(() -> showDialog(appName, stack));
        } catch (IllegalStateException fxNotReady) {
            // JavaFX toolkit not initialized — the log entry is our only record.
        }
    }

    private static void showDialog(String appName, String stack) {
        if (dialogOpen) return;
        dialogOpen = true;
        try {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(appName + " — Unexpected Error");
            a.setHeaderText("Something went wrong. Save your work before continuing.");
            TextArea area = new TextArea(stack);
            area.setEditable(false); area.setWrapText(false); area.setPrefSize(640, 320);
            Button copy = new Button("Copy to Clipboard");
            copy.setOnAction(e -> {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(stack);
                Clipboard.getSystemClipboard().setContent(cc);
                copy.setText("Copied ✓");
            });
            a.getDialogPane().setContent(new VBox(8, area, copy));
            a.showAndWait();
        } finally { dialogOpen = false; }
    }

    private static String stackToString(Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
