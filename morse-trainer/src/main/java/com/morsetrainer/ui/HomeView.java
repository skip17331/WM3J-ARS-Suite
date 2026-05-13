package com.morsetrainer.ui;

import com.morsetrainer.audio.MorsePlayer;
import com.morsetrainer.audio.ToneGenerator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Reusable home dashboard. Both the Application bootstrap and
 * "Back" buttons in trainer views call into this.
 */
public final class HomeView {

    private static ToneGenerator sharedTone;
    private static MorsePlayer sharedPlayer;

    public static void wireSharedAudio(ToneGenerator t, MorsePlayer p) {
        sharedTone = t; sharedPlayer = p;
    }

    public static ToneGenerator tone()   { return sharedTone; }
    public static MorsePlayer  player()  { return sharedPlayer; }

    private HomeView() {}

    public static void show(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24));

        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        Label title = new Label("Morse Code Trainer");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        Label subtitle = new Label("Audio · Decoder · Analytics — pick a mode below.");
        subtitle.setStyle("-fx-text-fill: #6b7280;");
        header.getChildren().addAll(title, subtitle);
        root.setTop(header);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(24));

        grid.add(card("Letter Trainer",
                "Random characters at your WPM. Koch progression supported.",
                () -> new LetterTrainerView(stage, sharedPlayer, sharedTone).show()), 0, 0);
        grid.add(card("Group Trainer",
                "Random 2–5 character groups. Track per-character accuracy.",
                () -> new GroupTrainerView(stage, sharedPlayer, sharedTone).show()), 1, 0);
        grid.add(card("QSO Simulator",
                "Realistic CW QSOs from training pace through contest tempo.",
                () -> new QsoTrainerView(stage, sharedPlayer, sharedTone).show()), 0, 1);
        grid.add(card("Sending Practice",
                "Send via keyboard, Arduino, or Pi Zero. Get timing diagnostics.",
                () -> new SendingTrainerView(stage, sharedTone).show()), 1, 1);
        Button settings = card("Settings",
                "WPM, Farnsworth, audio, hardware ports.",
                () -> new SettingsView(stage).show());
        grid.add(settings, 0, 2);

        Button report = card("🐛 Report Issue",
                "Found a bug? Open a pre-filled GitHub issue.",
                HomeView::openIssueReporter);
        grid.add(report, 1, 2);

        root.setCenter(grid);

        Scene scene = new Scene(root, 920, 640);
        var css = HomeView.class.getResource("/css/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Morse Code Trainer");
    }

    /**
     * Build a pre-filled GitHub issue URL and open it in the default
     * browser. Inlined here because morse-trainer is standalone and
     * doesn't depend on j-log-engine where the shared IssueReporter lives.
     */
    private static void openIssueReporter() {
        try {
            String title = "[morse-trainer v1.0.0] <one-line summary>";
            String body =
                "### Module\n- Name: morse-trainer\n- Version: 1.0.0\n\n" +
                "### Environment\n" +
                "- OS: " + System.getProperty("os.name", "") + " " +
                          System.getProperty("os.version", "") + "\n" +
                "- Java: " + System.getProperty("java.version", "") + "\n\n" +
                "### What I expected to happen\n\n\n" +
                "### What actually happened\n\n\n" +
                "### Steps to reproduce\n1. \n2. \n3. \n";
            String url = "https://github.com/skip17331/WM3J-ARS-Suite/issues/new"
                + "?labels=bug"
                + "&title=" + java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8)
                + "&body="  + java.net.URLEncoder.encode(body,  java.nio.charset.StandardCharsets.UTF_8);
            if (java.awt.Desktop.isDesktopSupported()
                && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception ignored) { /* best effort */ }
    }

    private static Button card(String title, String subtitle, Runnable onClick) {
        VBox content = new VBox(6);
        content.setPadding(new Insets(18));
        Label t = new Label(title);
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        Label s = new Label(subtitle);
        s.setStyle("-fx-text-fill: #6b7280;");
        s.setWrapText(true);
        content.getChildren().addAll(t, s);

        Button b = new Button();
        b.setGraphic(content);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPrefHeight(110);
        b.setOnAction(ev -> onClick.run());
        b.getStyleClass().add("mode-card");
        GridPane.setHgrow(b, Priority.ALWAYS);
        return b;
    }
}
