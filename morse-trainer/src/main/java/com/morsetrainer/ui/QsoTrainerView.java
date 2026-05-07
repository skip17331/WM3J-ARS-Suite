package com.morsetrainer.ui;

import com.morsetrainer.audio.MorsePlayer;
import com.morsetrainer.audio.ToneGenerator;
import com.morsetrainer.core.AppConfig;
import com.morsetrainer.trainer.qso.QsoGenerator;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * QSO Simulator UI. Generates a QSO at the chosen difficulty, plays it, then
 * scores the user's transcription against the original. The reveal panel is
 * hidden until the user clicks "Score" to avoid cheating.
 */
public class QsoTrainerView {

    private final Stage owner;
    private final MorsePlayer player;
    private final ToneGenerator tone;
    private final QsoGenerator generator = new QsoGenerator();

    private final TextArea reference = new TextArea();
    private final TextArea userCopy = new TextArea();
    private final Label score = new Label("");
    private final ComboBox<QsoGenerator.Difficulty> difficulty = new ComboBox<>();
    private final Spinner<Integer> wpmSpin = new Spinner<>(8, 50, AppConfig.get().wpm);

    private String currentQso = "";

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

        HBox controls = new HBox(10,
                new Label("Difficulty:"), difficulty,
                new Label("WPM:"), wpmSpin,
                btn("Generate & Play", this::generateAndPlay),
                btn("Reveal & Score", this::revealAndScore),
                btn("Back", () -> HomeView.show(owner)));

        VBox top = new VBox(10, title, controls);
        root.setTop(top);

        userCopy.setPromptText("Type the QSO as you copy it…");
        userCopy.setFont(Font.font("Monospaced", 14));
        reference.setEditable(false);
        reference.setStyle("-fx-control-inner-background: #f3f4f6;");
        reference.setFont(Font.font("Monospaced", 14));
        reference.setPromptText("(Reference QSO appears here when you reveal)");

        VBox center = new VBox(8,
                new Label("Your copy:"), userCopy,
                new Label("Reference:"), reference,
                score);
        userCopy.setPrefRowCount(8);
        reference.setPrefRowCount(8);
        score.setFont(Font.font("System", FontWeight.BOLD, 14));
        root.setCenter(center);

        Scene scene = new Scene(root, 920, 640);
        var css = getClass().getResource("/css/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        owner.setScene(scene);
        owner.setTitle("QSO Simulator");
    }

    private Button btn(String t, Runnable r) { Button b = new Button(t); b.setOnAction(e -> r.run()); return b; }

    private void generateAndPlay() {
        AppConfig.get().wpm = wpmSpin.getValue();
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
