package com.morsetrainer.ui;

import com.morsetrainer.audio.MorsePlayer;
import com.morsetrainer.audio.ToneGenerator;
import com.morsetrainer.core.AppConfig;
import com.morsetrainer.decoder.TimingDecoder;
import com.morsetrainer.hardware.KeyEventSource;
import com.morsetrainer.hardware.KeyboardKeyer;
import com.morsetrainer.hardware.arduino.ArduinoKeyer;
import com.morsetrainer.hardware.pizero.PiZeroKeyer;
import com.morsetrainer.trainer.qso.QsoGenerator;
import com.morsetrainer.trainer.sendpractice.SendPracticeSession;
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

import java.util.ArrayList;
import java.util.List;

/**
 * QSO Simulator UI with two modes:
 * <ul>
 *   <li><b>Receive only</b> — plays the whole QSO end-to-end and grades the
 *       user's typed transcription against the original.</li>
 *   <li><b>Send & Receive</b> — alternates DX and user turns. DX turns are
 *       played as audio and the user types what they hear; user turns prompt
 *       the user to key their reply, which is decoded live and graded.</li>
 * </ul>
 */
public class QsoTrainerView {

    private final Stage owner;
    private final MorsePlayer player;
    private final ToneGenerator tone;
    private final QsoGenerator generator = new QsoGenerator();

    // --- shared controls ----------------------------------------------------
    private final ComboBox<QsoGenerator.Difficulty> difficulty = new ComboBox<>();
    private final ComboBox<String> modeBox = new ComboBox<>();
    private final ComboBox<String> inputBox = new ComboBox<>();
    private final Spinner<Integer> wpmSpin = new Spinner<>(8, 50, AppConfig.get().wpm);
    private final TextField callsignField = new TextField();
    private final TextField nameField     = new TextField();
    private final TextField qthField      = new TextField();

    // --- receive-only mode state -------------------------------------------
    private final TextArea reference = new TextArea();
    private final TextArea userCopy  = new TextArea();
    private final Label score = new Label("");
    private String currentQso = "";

    // --- send-and-receive mode state ---------------------------------------
    private final Label turnLabel    = new Label("");
    private final Label turnPrompt   = new Label("");
    private final TextArea liveCopy  = new TextArea();
    private final TextArea liveSendDecoded = new TextArea();
    private final TextArea transcript = new TextArea();
    private final Button   nextButton = new Button("Next turn");

    private List<QsoGenerator.Turn> turns = List.of();
    private int turnIndex = 0;
    private SendPracticeSession sendSession;
    private KeyEventSource source;
    private final TimingDecoder decoder = new TimingDecoder();
    private int receiveCorrect = 0;
    private int receiveTotal   = 0;
    private int sendCorrect    = 0;
    private int sendTotal      = 0;

    public QsoTrainerView(Stage owner, MorsePlayer player, ToneGenerator tone) {
        this.owner = owner; this.player = player; this.tone = tone;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        Label title = new Label("QSO Simulator");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        difficulty.getItems().addAll(QsoGenerator.Difficulty.values());
        difficulty.setValue(QsoGenerator.Difficulty.TRAINING);

        modeBox.getItems().addAll("Send & Receive", "Receive only");
        modeBox.setValue("Send & Receive");
        modeBox.setOnAction(e -> refreshModeUi());

        inputBox.getItems().addAll("Keyboard (Space)", "Arduino USB", "Pi Zero Wireless");
        inputBox.setValue("Keyboard (Space)");

        AppConfig cfg = AppConfig.get();
        callsignField.setText(blank(cfg.userCallsign, "WM3J"));
        callsignField.setPrefColumnCount(8);
        nameField.setText(blank(cfg.userName, "OP"));
        nameField.setPrefColumnCount(8);
        qthField.setText(blank(cfg.userQth, "USA"));
        qthField.setPrefColumnCount(10);

        HBox row1 = new HBox(10,
                new Label("Mode:"), modeBox,
                new Label("Difficulty:"), difficulty,
                new Label("WPM:"), wpmSpin,
                new Label("Input:"), inputBox);
        HBox row2 = new HBox(10,
                new Label("Your call:"), callsignField,
                new Label("Name:"), nameField,
                new Label("QTH:"), qthField,
                btn("Start QSO", this::startQso),
                btn("Reveal & Score", this::revealAndScore),
                nextButton,
                btn("Back", () -> { stopAnyInput(); HomeView.show(owner); }));
        nextButton.setOnAction(e -> advanceTurn());

        VBox top = new VBox(10, title, row1, row2);
        root.setTop(top);

        // Receive-only widgets
        userCopy.setPromptText("Type the QSO as you copy it…");
        userCopy.setFont(Font.font("Monospaced", 14));
        reference.setEditable(false);
        reference.setStyle("-fx-control-inner-background: #f3f4f6;");
        reference.setFont(Font.font("Monospaced", 14));
        reference.setPromptText("(Reference QSO appears here when you reveal)");
        userCopy.setPrefRowCount(7);
        reference.setPrefRowCount(7);
        score.setFont(Font.font("System", FontWeight.BOLD, 14));

        VBox receiveOnlyPane = new VBox(8,
                new Label("Your copy:"), userCopy,
                new Label("Reference:"), reference,
                score);

        // Send-and-receive widgets
        turnLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        turnPrompt.setFont(Font.font("Monospaced", 16));
        turnPrompt.setWrapText(true);
        turnPrompt.setStyle("-fx-text-fill: #1d4ed8;");
        liveCopy.setPromptText("(DX turn) Type what you copy…");
        liveCopy.setFont(Font.font("Monospaced", 14));
        liveCopy.setPrefRowCount(3);
        liveSendDecoded.setEditable(false);
        liveSendDecoded.setFont(Font.font("Monospaced", 14));
        liveSendDecoded.setPromptText("(User turn) Decoded keying appears here…");
        liveSendDecoded.setPrefRowCount(3);
        transcript.setEditable(false);
        transcript.setFont(Font.font("Monospaced", 12));
        transcript.setPrefRowCount(8);

        VBox liveScorePane = new VBox(8,
                turnLabel,
                turnPrompt,
                new Label("Your copy (DX turns):"), liveCopy,
                new Label("Your sending (USER turns):"), liveSendDecoded,
                new Label("Transcript:"), transcript,
                score);

        // Stash both panes — refreshModeUi() picks the right one.
        root.setCenter(liveScorePane);
        root.getProperties().put("receivePane", receiveOnlyPane);
        root.getProperties().put("livePane",    liveScorePane);

        Scene scene = new Scene(root, 980, 760);
        var css = getClass().getResource("/css/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        owner.setScene(scene);
        owner.setTitle("QSO Simulator");
        refreshModeUi();
    }

    private void refreshModeUi() {
        boolean live = "Send & Receive".equals(modeBox.getValue());
        BorderPane root = (BorderPane) owner.getScene().getRoot();
        root.setCenter(live ? (VBox) root.getProperties().get("livePane")
                            : (VBox) root.getProperties().get("receivePane"));
        // Inputs only relevant in live mode.
        inputBox.setDisable(!live);
        callsignField.setDisable(!live);
        nameField.setDisable(!live);
        qthField.setDisable(!live);
        nextButton.setVisible(live);
        nextButton.setManaged(live);
    }

    private Button btn(String t, Runnable r) { Button b = new Button(t); b.setOnAction(e -> r.run()); return b; }

    // ------------------------------------------------------------------
    // Receive-only mode (legacy single-string playback + transcription score)
    // ------------------------------------------------------------------

    private void startQso() {
        AppConfig.get().wpm = wpmSpin.getValue();
        persistUserStation();
        if ("Receive only".equals(modeBox.getValue())) {
            startReceiveOnly();
        } else {
            startLive();
        }
    }

    private void startReceiveOnly() {
        currentQso = generator.generate(difficulty.getValue());
        userCopy.clear();
        reference.clear();
        score.setText("Playing… type as you go.");
        userCopy.requestFocus();

        new Thread(() -> {
            try { player.playBlocking(currentQso); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }, "qso-play").start();
    }

    private void revealAndScore() {
        if ("Receive only".equals(modeBox.getValue())) {
            if (currentQso.isEmpty()) return;
            reference.setText(currentQso);
            String user = userCopy.getText().trim().toUpperCase().replaceAll("\\s+", " ");
            String ref  = currentQso.trim().toUpperCase().replaceAll("\\s+", " ");
            double charAcc = charAccuracy(ref, user);
            double wordAcc = wordAccuracy(ref, user);
            double timing  = completenessRatio(ref, user);
            double overall = (charAcc * 0.5 + wordAcc * 0.3 + timing * 0.2) * 100;
            score.setText(String.format(
                    "Overall: %.0f%%   Char Acc: %.0f%%   Word Acc: %.0f%%   Completeness: %.0f%%",
                    overall, charAcc * 100, wordAcc * 100, timing * 100));
        } else {
            // In live mode "Reveal" finishes whatever turn is open and shows the running total.
            finalizeCurrentTurn();
            renderLiveScore();
        }
    }

    // ------------------------------------------------------------------
    // Send-and-receive mode
    // ------------------------------------------------------------------

    private void startLive() {
        stopAnyInput();
        QsoGenerator.UserStation user = new QsoGenerator.UserStation(
                upper(callsignField.getText(), "WM3J"),
                upper(nameField.getText(),     "OP"),
                upper(qthField.getText(),      "USA"));
        turns = generator.generateTurns(difficulty.getValue(), user);
        turnIndex = 0;
        receiveCorrect = receiveTotal = sendCorrect = sendTotal = 0;
        liveCopy.clear();
        liveSendDecoded.clear();
        transcript.clear();
        score.setText("");
        beginTurn();
    }

    private void beginTurn() {
        if (turnIndex >= turns.size()) {
            renderLiveScore();
            turnLabel.setText("QSO complete.");
            turnPrompt.setText("");
            stopAnyInput();
            return;
        }
        QsoGenerator.Turn t = turns.get(turnIndex);
        turnLabel.setText("Turn " + (turnIndex + 1) + " of " + turns.size()
                + " — " + (t.speaker() == QsoGenerator.Speaker.DX ? "DX is sending" : "Your turn — send"));
        if (t.speaker() == QsoGenerator.Speaker.DX) {
            turnPrompt.setText("Listen and copy:");
            liveCopy.clear();
            liveCopy.setDisable(false);
            liveCopy.requestFocus();
            stopAnyInput();
            new Thread(() -> {
                try { player.playBlocking(t.text()); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }, "qso-dx-turn").start();
        } else {
            turnPrompt.setText("Send: " + t.text());
            liveCopy.setDisable(true);
            liveSendDecoded.clear();
            startSendCapture();
        }
    }

    private void advanceTurn() {
        finalizeCurrentTurn();
        turnIndex++;
        beginTurn();
    }

    private void finalizeCurrentTurn() {
        if (turns.isEmpty() || turnIndex >= turns.size()) return;
        QsoGenerator.Turn t = turns.get(turnIndex);
        if (t.speaker() == QsoGenerator.Speaker.DX) {
            String typed = normalize(liveCopy.getText());
            String want  = normalize(t.text());
            int matched = matchedChars(want, typed);
            receiveCorrect += matched;
            receiveTotal   += want.length();
            transcript.appendText("DX:    " + t.text() + "\n");
            transcript.appendText("Copy:  " + typed + "    "
                    + score(matched, want.length()) + "\n\n");
        } else {
            String decoded = sendSession == null ? "" : normalize(sendSession.decoded());
            String want    = normalize(t.text());
            int matched = matchedChars(want, decoded);
            sendCorrect += matched;
            sendTotal   += want.length();
            transcript.appendText("YOU:   " + t.text() + "\n");
            transcript.appendText("Sent:  " + decoded + "    "
                    + score(matched, want.length()) + "\n\n");
            stopAnyInput();
        }
    }

    private void renderLiveScore() {
        double rxPct = receiveTotal == 0 ? 0 : 100.0 * receiveCorrect / receiveTotal;
        double txPct = sendTotal    == 0 ? 0 : 100.0 * sendCorrect    / sendTotal;
        double overall = (receiveTotal + sendTotal) == 0 ? 0
                : 100.0 * (receiveCorrect + sendCorrect) / (receiveTotal + sendTotal);
        score.setText(String.format(
                "Overall: %.0f%%   Receive: %.0f%% (%d/%d)   Send: %.0f%% (%d/%d)",
                overall, rxPct, receiveCorrect, receiveTotal, txPct, sendCorrect, sendTotal));
    }

    private void startSendCapture() {
        decoder.reset();
        sendSession = new SendPracticeSession(SendPracticeSession.Mode.FREE, "", decoder);
        sendSession.onCharacter(c -> Platform.runLater(() ->
                liveSendDecoded.appendText(String.valueOf(c))));
        source = createSource();
        if (source != null) {
            source.start(ev -> {
                if (sendSession != null) sendSession.accept(ev);
            });
        }
        // Drive decoder.tick periodically so word boundaries finalize without
        // requiring an explicit AnimationTimer per turn.
        Thread tick = new Thread(() -> {
            try {
                while (sendSession != null) {
                    Thread.sleep(50);
                    if (sendSession != null) sendSession.tick(System.currentTimeMillis());
                }
            } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }, "qso-send-tick");
        tick.setDaemon(true);
        tick.start();
    }

    private KeyEventSource createSource() {
        return switch (inputBox.getValue()) {
            case "Arduino USB"       -> new ArduinoKeyer();
            case "Pi Zero Wireless"  -> new PiZeroKeyer();
            default -> {
                KeyboardKeyer k = new KeyboardKeyer(owner.getScene(), KeyCode.SPACE);
                k.start(ev -> {
                    if (ev.type() == com.morsetrainer.decoder.KeyEvent.Type.DOWN) tone.keyDown();
                    else tone.keyUp();
                    if (sendSession != null) sendSession.accept(ev);
                });
                yield k;
            }
        };
    }

    private void stopAnyInput() {
        if (source != null) { source.stop(); source = null; }
        sendSession = null;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void persistUserStation() {
        AppConfig cfg = AppConfig.get();
        cfg.userCallsign = upper(callsignField.getText(), "");
        cfg.userName     = upper(nameField.getText(),     "");
        cfg.userQth      = upper(qthField.getText(),      "");
    }

    private static String blank(String v, String d) { return v == null || v.isBlank() ? d : v; }
    private static String upper(String v, String d) {
        if (v == null || v.isBlank()) return d;
        return v.trim().toUpperCase();
    }
    private static String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase().replaceAll("\\s+", " ");
    }
    private static int matchedChars(String want, String got) {
        int n = Math.min(want.length(), got.length());
        int m = 0;
        for (int i = 0; i < n; i++) if (want.charAt(i) == got.charAt(i)) m++;
        return m;
    }
    private static String score(int matched, int total) {
        if (total == 0) return "(empty)";
        return String.format("[%d/%d  %.0f%%]", matched, total, 100.0 * matched / total);
    }

    private static double charAccuracy(String ref, String user) {
        int n = Math.max(ref.length(), 1);
        int correct = 0;
        for (int i = 0; i < Math.min(ref.length(), user.length()); i++)
            if (ref.charAt(i) == user.charAt(i)) correct++;
        return (double) correct / n;
    }
    private static double wordAccuracy(String ref, String user) {
        String[] r = ref.split(" ");
        String[] u = user.split(" ");
        int matches = 0;
        for (int i = 0; i < Math.min(r.length, u.length); i++) if (r[i].equals(u[i])) matches++;
        return r.length == 0 ? 0 : (double) matches / r.length;
    }
    private static double completenessRatio(String ref, String user) {
        if (ref.isEmpty()) return 0;
        return Math.min(1.0, (double) user.length() / ref.length());
    }
}
