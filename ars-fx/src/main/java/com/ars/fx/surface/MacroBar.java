package com.ars.fx.surface;

import com.ars.fx.data.MacroConfig;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Shared F1–F8 station-macro button row, used by J-Log normal, J-Log contest,
 * and the J-Hub dashboard mini-log. Macros are defined in J-Hub ▸ Data.
 */
public final class MacroBar {
    private MacroBar() {}

    /** The F1–F<n> button row; {@code fire.accept(index)} runs that macro. */
    public static javafx.scene.layout.FlowPane row(IntConsumer fire) {
        // wraps onto a second line in a narrow column — the buttons keep their natural size instead of collapsing to "…"
        javafx.scene.layout.FlowPane m = new javafx.scene.layout.FlowPane(6, 6); m.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < MacroConfig.count(); i++) {
            final int idx = i;
            VBox mb = new VBox(2, lbl("F" + (i + 1), "jl-macro-fk"), lbl(MacroConfig.get(i).label(), "jl-macro-ml"));
            mb.getStyleClass().add("jl-macro"); mb.setStyle("-fx-cursor:hand;");
            mb.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            for (javafx.scene.Node n : mb.getChildren()) if (n instanceof javafx.scene.control.Label l) l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            com.ars.fx.shell.Tips.tip(mb, "F" + (idx + 1) + " — " + MacroConfig.get(idx).label() + "  (click to send)");
            mb.setOnMouseClicked(e -> fire.accept(idx));
            m.getChildren().add(mb);
        }
        return m;
    }

    /** Standard fire: phone modes play the voice file; CW/RTTY/digital send the {CALL}-expanded text. */
    public static IntConsumer fire(Supplier<String> modeSup, Supplier<String> callSup, Consumer<String> sendText) {
        return i -> {
            MacroConfig.Macro m = MacroConfig.get(i);
            if (isPhone(modeSup.get())) {
                if (m.audio() != null && !m.audio().isBlank()) {
                    boolean played = MacroConfig.playVoice(m.audio());
                    sendText.accept("▶ voice " + m.label() + (played ? "" : " (file not found)"));
                } else {
                    sendText.accept("▶ voice " + m.label() + " — no audio set (J-Hub ▸ Data ▸ Macros)");
                }
            } else {
                sendText.accept(expand(m.text(), callSup.get()));
            }
        };
    }

    public static boolean isPhone(String mode) {
        if (mode == null) return false;
        String m = mode.trim().toUpperCase();
        return m.equals("USB") || m.equals("LSB") || m.contains("SSB") || m.equals("AM") || m.equals("FM")
                || m.contains("PHONE") || m.contains("VOICE");
    }
    public static String expand(String msg, String call) {
        String c = (call == null || call.isBlank()) ? "OM" : call.trim().toUpperCase();
        return msg == null ? "" : msg.replace("{CALL}", c);
    }
}
