package com.morsetrainer.ui;

import com.morsetrainer.analytics.ScoringEngine;
import com.morsetrainer.audio.MorsePlayer;
import com.morsetrainer.audio.ToneGenerator;
import com.morsetrainer.core.AppConfig;
import com.morsetrainer.trainer.groups.GroupSession;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Group Trainer UI: plays a random group, the user types it, accuracy is
 * scored character-by-character. Per-character stats roll up into the same
 * scoring engine used by the letter trainer.
 */
public class GroupTrainerView {

    private final Stage owner;
    private final MorsePlayer player;
    private final ToneGenerator tone;

    private final ScoringEngine scoring = new ScoringEngine();
    private GroupSession session;
    private String currentGroup = "";

    private final Label statusLabel = new Label("Press Start to begin.");
    private final TextField input = new TextField();
    private final Label feedback = new Label(" ");
    private final TextArea heatmap = new TextArea();
    private final Spinner<Integer> minSpin = new Spinner<>(2, 7, AppConfig.get().groupMinSize);
    private final Spinner<Integer> maxSpin = new Spinner<>(2, 7, AppConfig.get().groupMaxSize);
    private final Spinner<Integer> wpmSpin = new Spinner<>(5, 50, AppConfig.get().wpm);
    private final TextField alphabetField = new TextField("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");

    public GroupTrainerView(Stage owner, MorsePlayer player, ToneGenerator tone) {
        this.owner = owner; this.player = player; this.tone = tone;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        Label title = new Label("Group Trainer");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        HBox controls = new HBox(10,
                new Label("Min:"), minSpin,
                new Label("Max:"), maxSpin,
                new Label("WPM:"), wpmSpin,
                btn("Start", this::start),
                btn("Stop", this::stop),
                btn("Back", () -> HomeView.show(owner)));

        HBox alpha = new HBox(8, new Label("Character set:"), alphabetField);
        alphabetField.setPrefColumnCount(40);

        VBox top = new VBox(10, title, controls, alpha);
        root.setTop(top);

        feedback.setFont(Font.font("Monospaced", 22));
        input.setPrefColumnCount(20);
        input.setFont(Font.font("Monospaced", 20));
        input.setOnAction(ev -> submit());

        VBox center = new VBox(14, statusLabel, feedback, input);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40));
        root.setCenter(center);

        heatmap.setEditable(false);
        heatmap.setPrefRowCount(8);
        heatmap.setFont(Font.font("Monospaced", 12));
        root.setBottom(new VBox(8, new Label("Stats"), heatmap));

        Scene scene = new Scene(root, 920, 640);
        var css = getClass().getResource("/css/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        owner.setScene(scene);
        owner.setTitle("Group Trainer");
    }

    private Button btn(String t, Runnable r) { Button b = new Button(t); b.setOnAction(e -> r.run()); return b; }

    private void start() {
        AppConfig.get().wpm = wpmSpin.getValue();
        List<Character> chars = new ArrayList<>();
        for (char c : alphabetField.getText().toUpperCase().toCharArray()) chars.add(c);
        if (chars.isEmpty()) { statusLabel.setText("Empty character set"); return; }
        session = new GroupSession(chars, minSpin.getValue(), maxSpin.getValue(), 20, new java.util.Random());
        statusLabel.setText("Type each group as you hear it.");
        nextGroup();
        input.requestFocus();
    }

    private void stop() {
        statusLabel.setText(String.format("Stopped — Accuracy %.0f%%", scoring.overallAccuracy() * 100));
        if (AppConfig.get().autoExportSessions) scoring.exportJson();
        renderHeatmap();
    }

    private void nextGroup() {
        if (session == null || !session.hasNext()) { stop(); return; }
        currentGroup = session.next();
        for (char c : currentGroup.toCharArray()) scoring.presented(c);
        Platform.runLater(input::clear);
        new Thread(() -> {
            try { player.playBlocking(currentGroup); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }, "group-play").start();
    }

    private void submit() {
        if (currentGroup.isEmpty()) return;
        String typed = input.getText().toUpperCase();
        int n = Math.min(typed.length(), currentGroup.length());
        for (int i = 0; i < currentGroup.length(); i++) {
            char expected = currentGroup.charAt(i);
            char got = i < typed.length() ? typed.charAt(i) : ' ';
            scoring.typed(expected, got);
        }
        scoring.wordBoundary();

        StringBuilder sb = new StringBuilder("Expected: ").append(currentGroup)
                .append("  You: ").append(typed);
        feedback.setText(sb.toString());
        renderHeatmap();
        new Thread(() -> {
            try { Thread.sleep(900); } catch (InterruptedException ie) {}
            Platform.runLater(this::nextGroup);
        }).start();
    }

    private void renderHeatmap() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total: %d | Errors: %d | Accuracy: %.1f%% | Word Acc: %.1f%%%n%n",
                scoring.totalCharacters(), scoring.totalErrors(),
                scoring.overallAccuracy() * 100, scoring.wordAccuracy() * 100));
        sb.append(String.format("%-6s %-10s %-9s %-7s%n", "Char", "Presented", "Errors", "Acc%"));
        for (var s : new java.util.TreeMap<>(scoring.perCharacter()).values()) {
            sb.append(String.format("%-6s %-10d %-9d %-7.1f%n",
                    s.character, s.presented, s.errors, s.accuracy() * 100));
        }
        if (!scoring.recommendedDrills(5).isEmpty()) {
            sb.append("\nRecommended drill chars: ").append(scoring.recommendedDrills(5));
        }
        heatmap.setText(sb.toString());
    }
}
