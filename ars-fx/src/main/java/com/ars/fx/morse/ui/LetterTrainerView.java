package com.ars.fx.morse.ui;

import com.ars.fx.morse.analytics.ScoringEngine;
import com.ars.fx.morse.audio.MorsePlayer;
import com.ars.fx.morse.audio.ToneGenerator;
import com.ars.fx.morse.core.AppConfig;
import com.ars.fx.morse.trainer.letters.LetterSession;
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

import java.util.Map;

/**
 * UI for the Letter Trainer. Plays a character, waits for the user's keyboard
 * response, gives instant feedback, advances. Per-character stats are shown in
 * a heatmap-style table at the bottom and updated live.
 */
public class LetterTrainerView {

    private final Runnable onBack;
    private javafx.scene.layout.Region rootPane;      // this view's root, once built (its scene is reached through it)
    private final MorsePlayer player;
    private final ToneGenerator tone;

    private LetterSession session;
    private final ScoringEngine scoring = new ScoringEngine();
    private char currentChar;
    private boolean awaitingResponse = false;

    private final Label statusLabel = new Label("Press Start to begin.");
    private final Label promptLabel = new Label(" ");        // training-wheels char
    private final CheckBox showCharBox = new CheckBox("Show character");
    private final Label feedbackLabel = new Label(" ");
    private final Label progressLabel = new Label("");
    private final TextField input = new TextField();
    private final TextArea heatmap = new TextArea();
    private final Spinner<Integer> levelSpinner = new Spinner<>(2, 40, AppConfig.get().defaultKochLevel);
    private final Spinner<Integer> wpmSpinner   = new Spinner<>(5, 50, AppConfig.get().wpm);
    private final Spinner<Integer> farnsSpinner = new Spinner<>(5, 50, AppConfig.get().farnsworthWpm);

    public LetterTrainerView(Runnable onBack, MorsePlayer player, ToneGenerator tone) {
        this.onBack = onBack;
        this.player = player;
        this.tone = tone;
    }

    public javafx.scene.layout.Region build() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        Label title = new Label("Letter Trainer");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        // Two rows — one HBox with all of these gets squished narrow
        // (labels and buttons collapse to "..."). Settings on top,
        // actions below.
        HBox settingsRow = new HBox(12,
                new Label("Koch level:"), levelSpinner,
                new Label("WPM:"), wpmSpinner,
                new Label("Farnsworth WPM:"), farnsSpinner);
        settingsRow.setAlignment(Pos.CENTER_LEFT);

        HBox actionRow = new HBox(12,
                showCharBox,
                button("Start", this::start),
                button("Stop",  this::stop),
                button("Back",  () -> onBack.run()));
        actionRow.setAlignment(Pos.CENTER_LEFT);
        // Training wheels: on = show the letter while it's sent (learning);
        // off = code only (testing). Defaults on for new learners.
        showCharBox.setSelected(true);

        VBox top = new VBox(12, title, settingsRow, actionRow);
        root.setTop(top);

        promptLabel.setFont(Font.font("System", FontWeight.BOLD, 64));
        promptLabel.setStyle("-fx-text-fill: -ars-accent;");
        feedbackLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        progressLabel.setStyle("-fx-text-fill: -ars-t3;");
        input.setMaxWidth(120);
        input.setFont(Font.font("Monospaced", 24));
        input.setOnAction(ev -> submitTyped());
        input.textProperty().addListener((obs, old, val) -> {
            if (awaitingResponse && !val.isEmpty()) submitTyped();
        });

        VBox center = new VBox(14, statusLabel, promptLabel, feedbackLabel, input, progressLabel);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40));
        root.setCenter(center);

        heatmap.setEditable(false);
        heatmap.setPrefRowCount(8);
        heatmap.setFont(Font.font("Monospaced", 12));
        VBox bottom = new VBox(8, new Label("Per-character stats"), heatmap);
        root.setBottom(bottom);

        rootPane = root;
        return root;
    }

    private Button button(String label, Runnable r) {
        Button b = new Button(label);
        // Never let the HBox shrink a button down to an unreadable "...".
        b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        b.setOnAction(ev -> r.run());
        return b;
    }

    private void start() {
        AppConfig cfg = AppConfig.get();
        cfg.wpm = wpmSpinner.getValue();
        cfg.farnsworthWpm = farnsSpinner.getValue();
        session = LetterSession.kochLevel(levelSpinner.getValue(), cfg.letterSessionCount);
        statusLabel.setText("Listen and type each character.");
        nextCharacter();
        input.requestFocus();
    }

    private void stop() {
        awaitingResponse = false;
        promptLabel.setText(" ");
        statusLabel.setText(String.format("Stopped. Accuracy %.0f%%, WPM %.1f",
                scoring.overallAccuracy() * 100, scoring.achievedWpm()));
        if (AppConfig.get().autoExportSessions) scoring.exportJson();
        renderHeatmap();
    }

    private void nextCharacter() {
        if (session == null || !session.hasNext()) {
            stop();
            return;
        }
        currentChar = session.next();
        scoring.presented(currentChar);
        progressLabel.setText("Remaining: " + session.remaining());
        promptLabel.setText(showCharBox.isSelected()
                ? String.valueOf(Character.toUpperCase(currentChar)) : "");
        awaitingResponse = true;
        Platform.runLater(input::clear);
        new Thread(() -> {
            try { player.playBlocking(String.valueOf(currentChar)); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }, "letter-play").start();
    }

    private void submitTyped() {
        if (!awaitingResponse) return;
        String s = input.getText().trim();
        if (s.isEmpty()) return;
        char typed = Character.toUpperCase(s.charAt(0));
        scoring.typed(currentChar, typed);
        if (typed == Character.toUpperCase(currentChar)) {
            feedbackLabel.setText("✓ " + currentChar);
            feedbackLabel.setStyle("-fx-text-fill: -ars-ok;");
        } else {
            feedbackLabel.setText("✗ " + currentChar + " (you: " + typed + ")");
            feedbackLabel.setStyle("-fx-text-fill: -ars-err;");
        }
        renderHeatmap();
        awaitingResponse = false;
        new Thread(() -> {
            try { Thread.sleep(800); } catch (InterruptedException ie) {}
            Platform.runLater(this::nextCharacter);
        }).start();
    }

    private void renderHeatmap() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total: %d | Errors: %d | Accuracy: %.1f%% | WPM: %.1f%n",
                scoring.totalCharacters(), scoring.totalErrors(),
                scoring.overallAccuracy() * 100, scoring.achievedWpm()));
        sb.append(String.format("%n%-6s %-10s %-9s %-7s %-12s%n",
                "Char", "Presented", "Errors", "Acc%", "AvgRT(ms)"));
        for (Map.Entry<Character, com.ars.fx.morse.analytics.CharStats> e :
                new java.util.TreeMap<>(scoring.perCharacter()).entrySet()) {
            var s = e.getValue();
            sb.append(String.format("%-6s %-10d %-9d %-7.1f %-12.0f%n",
                    s.character, s.presented, s.errors, s.accuracy() * 100, s.avgResponseMillis()));
        }
        if (!scoring.recommendedDrills(5).isEmpty()) {
            sb.append("\nRecommended drill chars: ").append(scoring.recommendedDrills(5));
        }
        heatmap.setText(sb.toString());
    }
}
