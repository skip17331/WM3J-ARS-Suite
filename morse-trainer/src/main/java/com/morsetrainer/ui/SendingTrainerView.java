package com.morsetrainer.ui;

import com.morsetrainer.analytics.SendingDiagnostics;
import com.morsetrainer.audio.ToneGenerator;
import com.morsetrainer.core.AppConfig;
import com.morsetrainer.decoder.TimingDecoder;
import com.morsetrainer.hardware.KeyEventSource;
import com.morsetrainer.hardware.KeyboardKeyer;
import com.morsetrainer.hardware.arduino.ArduinoKeyer;
import com.morsetrainer.hardware.pizero.PiZeroKeyer;
import com.morsetrainer.trainer.sendpractice.SendPracticeSession;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Sending Practice UI. The user picks an input source (keyboard, Arduino, or
 * Pi Zero), optionally enters a target string for guided mode, and starts the
 * session. Live diagnostics (WPM, dit/dah ratio, consistency) update at ~5Hz.
 */
public class SendingTrainerView {

    private final Stage owner;
    private final ToneGenerator tone;
    private final TimingDecoder decoder = new TimingDecoder();

    private SendPracticeSession session;
    private KeyEventSource source;
    private AnimationTimer ticker;

    private final ComboBox<String> inputBox = new ComboBox<>();
    private final ComboBox<String> modeBox  = new ComboBox<>();
    private final TextField target = new TextField();
    private final TextArea decoded = new TextArea();
    private final TextArea metrics = new TextArea();

    public SendingTrainerView(Stage owner, ToneGenerator tone) {
        this.owner = owner; this.tone = tone;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        Label title = new Label("Sending Practice");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        Label hint = new Label(
                "Keyboard uses Space. Arduino & Pi Zero auto-detect straight key vs iambic paddle "
              + "based on their firmware/launch mode (see hardware/README).");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #5d6b7c;");

        inputBox.getItems().addAll("Keyboard (Space)", "Arduino USB", "Pi Zero Wireless");
        inputBox.setValue("Keyboard (Space)");
        modeBox.getItems().addAll("Free", "Guided");
        modeBox.setValue("Free");

        target.setPromptText("Target text (guided mode)");
        target.setText("HELLO WORLD");

        HBox controls = new HBox(10,
                new Label("Input:"), inputBox,
                new Label("Mode:"), modeBox,
                btn("Start", this::startSession),
                btn("Stop", this::stopSession),
                btn("Back", () -> { stopSession(); HomeView.show(owner); }));

        VBox top = new VBox(10, title, hint, controls, target);
        root.setTop(top);

        decoded.setEditable(false);
        decoded.setFont(Font.font("Monospaced", 18));
        metrics.setEditable(false);
        metrics.setFont(Font.font("Monospaced", 12));
        metrics.setPrefRowCount(14);

        VBox center = new VBox(8,
                new Label("Decoded:"), decoded,
                new Label("Diagnostics:"), metrics);
        decoded.setPrefRowCount(4);
        root.setCenter(center);

        Scene scene = new Scene(root, 980, 680);
        var css = getClass().getResource("/css/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        owner.setScene(scene);
        owner.setTitle("Sending Practice");
        scene.getRoot().requestFocus();
    }

    private Button btn(String t, Runnable r) { Button b = new Button(t); b.setOnAction(e -> r.run()); return b; }

    private void startSession() {
        stopSession();
        decoder.reset();
        decoded.clear();
        metrics.clear();

        SendPracticeSession.Mode mode = "Guided".equals(modeBox.getValue())
                ? SendPracticeSession.Mode.GUIDED : SendPracticeSession.Mode.FREE;
        session = new SendPracticeSession(mode,
                mode == SendPracticeSession.Mode.GUIDED ? target.getText() : "",
                decoder);
        session.onCharacter(c -> Platform.runLater(() -> decoded.appendText(String.valueOf(c))));

        source = createSource();
        if (source != null) source.start(ev -> session.accept(ev));

        ticker = new AnimationTimer() {
            long lastUiUpdate = 0;
            @Override public void handle(long ns) {
                long nowMs = System.currentTimeMillis();
                if (session != null) session.tick(nowMs);
                if (nowMs - lastUiUpdate > 200) {
                    lastUiUpdate = nowMs;
                    refreshMetrics();
                }
            }
        };
        ticker.start();
    }

    private KeyEventSource createSource() {
        return switch (inputBox.getValue()) {
            case "Arduino USB" -> {
                ArduinoKeyer a = new ArduinoKeyer();
                yield a;
            }
            case "Pi Zero Wireless" -> {
                PiZeroKeyer p = new PiZeroKeyer();
                yield p;
            }
            default -> {
                Scene s = owner.getScene();
                KeyboardKeyer k = new KeyboardKeyer(s, KeyCode.SPACE);
                // Audible sidetone on keyboard input.
                k.start(ev -> {
                    if (ev.type() == com.morsetrainer.decoder.KeyEvent.Type.DOWN) tone.keyDown();
                    else tone.keyUp();
                    if (session != null) session.accept(ev);
                });
                yield k;
            }
        };
    }

    private void stopSession() {
        if (ticker != null) { ticker.stop(); ticker = null; }
        if (source != null) { source.stop(); source = null; }
        if (session != null) refreshMetrics();
    }

    private void refreshMetrics() {
        if (session == null) return;
        SendingDiagnostics.Result r = session.diagnostics();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Achieved WPM:    %.1f%n", r.achievedWpm));
        sb.append(String.format("Avg dit / dah:   %.1f / %.1f ms%n", r.avgDitMs, r.avgDahMs));
        sb.append(String.format("Dit / dah σ:     %.2f / %.2f ms%n", r.ditStdDevMs, r.dahStdDevMs));
        sb.append(String.format("Dit:Dah ratio:   %.2f (target 3.00)%n", r.ditDahRatio));
        sb.append(String.format("Avg intra/char/word gap: %.1f / %.1f / %.1f ms%n",
                r.avgIntraGapMs, r.avgCharGapMs, r.avgWordGapMs));
        sb.append(String.format("Consistency:     %.0f / 100%n", r.consistencyScore));
        sb.append(String.format("Smoothness:      %.0f / 100%n", r.smoothnessIndex));
        if (session.mode() == SendPracticeSession.Mode.GUIDED) {
            sb.append(String.format("Guided accuracy: %.0f%%%n", session.accuracy() * 100));
            var trouble = session.troubleReport();
            if (!trouble.worstByMiscode.isEmpty())
                sb.append("Hardest chars: ").append(trouble.worstByMiscode).append('\n');
            if (!trouble.worstByTiming.isEmpty())
                sb.append("Worst timing chars: ").append(trouble.worstByTiming).append('\n');
        }
        if (!r.issues.isEmpty()) {
            sb.append("\nIssues:\n");
            for (var i : r.issues) sb.append("• ").append(i.description()).append('\n');
        }
        Platform.runLater(() -> metrics.setText(sb.toString()));
    }
}
