package com.ars.fx.morse.ui;

import com.ars.fx.morse.audio.MorsePlayer;
import com.ars.fx.morse.audio.ToneGenerator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The Morse Trainer inside J-Learn: a home screen with the four practice modes and the settings, each opening in the
 * reading area and returning here with Back. The tone generator and player are shared, created on first use.
 */
public final class MorseHome {
    private MorseHome() {}

    private static ToneGenerator tone;
    private static MorsePlayer player;

    private static synchronized void ensureAudio() {
        if (tone == null) { tone = new ToneGenerator(); player = new MorsePlayer(tone); }
    }

    /** Release the sound device (application exit). */
    public static synchronized void shutdown() {
        if (player != null) player.shutdown();
        if (tone != null) tone.close();
        player = null; tone = null;
    }

    /** @param show puts a pane into J-Learn's reading area */
    public static Region build(Consumer<Region> show) {
        Runnable home = () -> show.accept(build(show));
        VBox v = new VBox(14); v.setPadding(new Insets(28, 30, 36, 30)); v.setMaxWidth(900);
        v.getChildren().add(lbl("J-LEARN · MORSE TRAINER", "j2-content-eyebrow"));
        Label h = lbl("Morse Trainer", "j2-h1"); v.getChildren().add(h);
        Label intro = lbl("Learn and practise CW: copy characters, groups and whole QSOs, and send with a key or paddle and see how well "
                + "your timing is. Progress is scored per character so the drills can aim at your weak spots.", "j2-content-body");
        intro.setWrapText(true); intro.setMaxWidth(Double.MAX_VALUE); v.getChildren().add(intro);

        FlowPane cards = new FlowPane(14, 14);
        cards.getChildren().addAll(
            card("RECEIVING", "Letter trainer", "Single characters at your speed, Koch-method levels, instant feedback.",
                    () -> { ensureAudio(); show.accept(framed(new LetterTrainerView(home, player, tone).build())); }),
            card("RECEIVING", "Group trainer", "Random groups of 2–5 characters from any alphabet, with per-character accuracy.",
                    () -> { ensureAudio(); show.accept(framed(new GroupTrainerView(home, player, tone).build())); }),
            card("RECEIVING", "QSO simulator", "Realistic exchanges — CQ, callsigns, RST, name, QTH — at Training, Casual or Contest level.",
                    () -> { ensureAudio(); show.accept(framed(new QsoTrainerView(home, player, tone).build())); }),
            card("SENDING", "Sending practice", "Key it with the spacebar, an Arduino or a Pi Zero keyer; your timing is decoded and graded.",
                    () -> { ensureAudio(); show.accept(framed(new SendingTrainerView(home, tone).build())); }),
            card("SETUP", "Settings", "Tone, speed, Farnsworth spacing, keyer hardware, and your call / name / QTH for QSOs.",
                    () -> show.accept(framed(new SettingsView(home).build()))));
        v.getChildren().add(cards);
        return scroll(v);
    }

    private static Region card(String kind, String title, String text, Runnable open) {
        Label k = lbl(kind, "j2-content-eyebrow"), t = lbl(title, "jhub-card-title");
        Label d = lbl(text, "jl-emeta"); d.setWrapText(true); d.setMaxWidth(250);
        VBox c = new VBox(6, k, t, d); c.setPadding(new Insets(14, 16, 14, 16)); c.setPrefWidth(290); c.setMinHeight(130);
        c.setStyle("-fx-cursor:hand;-fx-background-color:-ars-surface-1;-fx-background-radius:10;-fx-border-color:-ars-border;-fx-border-radius:10;-fx-border-width:1;");
        c.setOnMouseClicked(e -> open.run());
        return c;
    }

    /** A trainer's pane inside the app's own colours (see the .mt-root rules in ars.css), scrollable in a small window. */
    private static Region framed(Region view) {
        VBox box = new VBox(view); box.getStyleClass().add("mt-root"); VBox.setVgrow(view, Priority.ALWAYS);
        return scroll(box);
    }

    private static ScrollPane scroll(Region content) {
        ScrollPane sp = new ScrollPane(content); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;-fx-background:-ars-bg;"); HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }
}
