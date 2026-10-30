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

    private static final String SEND_AS = "macro.sendAs";   // "auto" (from the mode field) | "cw" | "voice"

    /** Should the F-keys play the voice file (true) or send text (false)? The operator's "send as" choice wins;
     *  with neither box ticked it follows the mode field (phone modes → voice). */
    public static boolean usePhone(String mode) {
        return switch (com.ars.fx.data.HubConfig.get(SEND_AS, "auto")) {
            case "voice" -> true;
            case "cw" -> false;
            default -> isPhone(mode);
        };
    }

    /** "Macros send as: ☐ CW / data ☐ Voice" — two boxes that act as a choice. Neither ticked = automatic from the mode
     *  field; ticking one forces it; ticking the other switches; unticking returns to automatic. The choice is shared
     *  by every J-Log screen. {@code modeField} is the screen's MODE field, so the hint can name what a press will do. */
    public static javafx.scene.layout.FlowPane sendAsRow(javafx.scene.control.TextField modeField) {
        javafx.scene.control.CheckBox cw = new javafx.scene.control.CheckBox("CW / data"), voice = new javafx.scene.control.CheckBox("Voice");
        for (javafx.scene.control.CheckBox cb : new javafx.scene.control.CheckBox[]{cw, voice})
            cb.setStyle("-fx-text-fill:-ars-t1;-fx-font-size:12px;-fx-cursor:hand;");   // the default text colour is invisible on this theme
        String cur = com.ars.fx.data.HubConfig.get(SEND_AS, "auto");
        cw.setSelected(cur.equals("cw")); voice.setSelected(cur.equals("voice"));
        javafx.scene.control.Label hint = lbl("", "jl-skim-meta");
        Runnable paint = () -> {
            String mode = modeField == null ? "" : modeField.getText();
            String how = com.ars.fx.data.HubConfig.get(SEND_AS, "auto");
            boolean phone = usePhone(mode);
            hint.setText(how.equals("auto") ? "auto → " + (phone ? "voice" : "text") + (mode == null || mode.isBlank() ? "" : " (" + mode.trim().toUpperCase() + ")")
                                            : "→ " + (phone ? "voice file" : "text"));
        };
        boolean[] busy = {false};
        cw.selectedProperty().addListener((o, a, on) -> {
            if (busy[0]) return; busy[0] = true;
            if (on) voice.setSelected(false);
            com.ars.fx.data.HubConfig.set(SEND_AS, on ? "cw" : (voice.isSelected() ? "voice" : "auto")); busy[0] = false; paint.run();
        });
        voice.selectedProperty().addListener((o, a, on) -> {
            if (busy[0]) return; busy[0] = true;
            if (on) cw.setSelected(false);
            com.ars.fx.data.HubConfig.set(SEND_AS, on ? "voice" : (cw.isSelected() ? "cw" : "auto")); busy[0] = false; paint.run();
        });
        if (modeField != null) modeField.textProperty().addListener((o, a, b) -> paint.run());
        com.ars.fx.shell.Tips.tip(cw, "Send the macro text (CW or data modes) — whatever the mode field says");
        com.ars.fx.shell.Tips.tip(voice, "Play the macro's voice file — whatever the mode field says");
        com.ars.fx.shell.Tips.tip(hint, "Leave both unticked and the F-keys follow the mode: SSB / USB / LSB / AM / FM play the voice file, anything else sends the text");
        paint.run();
        javafx.scene.layout.FlowPane row = new javafx.scene.layout.FlowPane(12, 4); row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(lbl("Macros send as", "jl-skim-meta"), cw, voice, hint);
        return row;
    }

    /** Standard fire: voice file or text per {@link #usePhone}; text is {CALL}-expanded. */
    public static IntConsumer fire(Supplier<String> modeSup, Supplier<String> callSup, Consumer<String> sendText) {
        return i -> {
            MacroConfig.Macro m = MacroConfig.get(i);
            if (usePhone(modeSup.get())) {
                if (m.audio() != null && !m.audio().isBlank()) {
                    com.ars.fx.data.TxController.macroVoice(m.audio(), sendText);      // keys the rig, plays the file, unkeys
                } else {
                    sendText.accept("▶ voice " + m.label() + " — no audio set (J-Hub ▸ Data ▸ Macros)");
                }
            } else {
                String text = textFor(m.text(), callSup.get(), sendText);          // null = a needed station field is empty (already reported)
                if (text != null) com.ars.fx.data.TxController.macroText(modeSup.get(), text, sendText);   // CW via the rig keyer
            }
        };
    }

    public static boolean isPhone(String mode) {
        if (mode == null) return false;
        String m = mode.trim().toUpperCase();
        return m.equals("USB") || m.equals("LSB") || m.contains("SSB") || m.equals("AM") || m.equals("FM")
                || m.contains("PHONE") || m.contains("VOICE");
    }
    public static String expand(String msg, String call) { return MacroConfig.expand(msg, call).text(); }

    /** The macro text with {CALL} and the station placeholders filled in. When a field it needs is empty, nothing is sent:
     *  this reports which one to {@code report} and returns null. */
    public static String textFor(String template, String workedCall, Consumer<String> report) {
        MacroConfig.Expanded x = MacroConfig.expand(template, workedCall);
        if (x.ok()) return x.text();
        if (report != null) report.accept("✗ not sent — this macro needs " + String.join("; ", x.missing()) + ". Set it in J-Hub.");
        return null;
    }
}
