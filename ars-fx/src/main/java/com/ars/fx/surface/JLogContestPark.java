package com.ars.fx.surface;

import com.ars.fx.data.ContestState;
import com.ars.fx.data.RigClient;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ars.fx.shell.Shell.lbl;
import static com.ars.fx.surface.JLogContest.*;

/**
 * Parked contacts: two stations that are hard to work (one in a pile-up), and you want to try the other and come back. Park saves everything on
 * the entry form with the radio's frequency and mode, and clears the form for the next station; clicking the parked chip puts it all back and
 * tunes the radio to where it was. What is on the form when you restore is parked first, so two stations can be swapped between. Nothing here
 * reads or switches the radio's VFOs, so the radio is never cycled: the frequency is read once when you park, and set once when you come back.
 */
final class JLogContestPark {
    private JLogContestPark() {}

    /** One parked contact. {@code freqHz} is 0 when the radio was not connected, and then coming back tunes nothing. */
    record Parked(String call, Map<String, String> values, String band, String mode, long freqHz, String rigMode) {}

    static final int MAX = 8;
    private static final List<Parked> LIST = new ArrayList<>();
    private static String forContest = "";
    private static final FlowPane BOX = new FlowPane(8, 6);
    static { BOX.managedProperty().bind(javafx.beans.binding.Bindings.isNotEmpty(BOX.getChildren())); BOX.visibleProperty().bind(BOX.managedProperty()); }

    /** The strip of parked chips, for the entry pane; empty (and hidden) until something is parked. A different contest starts a fresh list. */
    static FlowPane strip(String contestId) {
        if (!contestId.equals(forContest)) { LIST.clear(); forContest = contestId; }
        refresh();
        return BOX;
    }

    /** Ctrl+P parks, Ctrl+1 ... Ctrl+8 bring a parked contact back, from any entry field. */
    static void installKeys(Control c) {
        c.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!e.isControlDown() || e.isAltDown() || e.isShiftDown()) return;
            if (e.getCode() == KeyCode.P) { park(); e.consume(); return; }
            int n = e.getCode().isDigitKey() ? e.getCode().ordinal() - KeyCode.DIGIT0.ordinal() : -1;
            if (n >= 1 && n <= LIST.size()) { restore(LIST.get(n - 1)); e.consume(); }
        });
    }

    static boolean park() {
        if (editingRec != null) { setB4("Finish or cancel the edit before parking", "new"); return false; }
        String call = value("callsign");
        if (call.isBlank()) { setB4("Type the call first, then park it", "new"); focus("callsign"); return false; }
        Map<String, String> vals = new LinkedHashMap<>();
        for (Map.Entry<String, Control> en : fieldControls.entrySet()) {
            if (keepIds.contains(en.getKey())) continue;          // what we send, and anything that stays between contacts, is not part of the contact
            vals.put(en.getKey(), value(en.getKey()));
        }
        RigClient.State rs = RigClient.getInstance().last();
        boolean rig = rs != null && rs.connected() && rs.freqHz() > 0;
        Parked p = new Parked(call.toUpperCase(Locale.ROOT), vals, bandF.getText(), modeF.getText(), rig ? rs.freqHz() : 0, rig ? rs.mode() : "");
        LIST.add(0, p);
        while (LIST.size() > MAX) LIST.remove(LIST.size() - 1);
        clearForm();
        refresh();
        setB4("⏸ Parked " + p.call() + (p.freqHz() > 0 ? "  " + mhz(p.freqHz()) : ""), "new");
        return true;
    }

    static void restore(Parked p) {
        if (!LIST.contains(p)) return;
        if (editingRec != null) { setB4("Finish or cancel the edit first", "new"); return; }
        if (!value("callsign").isBlank() && !park()) return;       // what is on the form goes to the strip first: a swap loses nothing
        LIST.remove(p);
        clearForm();
        for (Map.Entry<String, String> en : p.values().entrySet()) {
            Control c = fieldControls.get(en.getKey());
            if (c != null) JLogContestSend.setControl(c, en.getValue());
        }
        if (p.band() != null) bandF.setText(p.band());
        if (p.mode() != null) modeF.setText(p.mode());
        if (p.freqHz() > 0) {                                      // back to where it was: set once, never polled
            RigClient rig = RigClient.getInstance();
            RigClient.State rs = rig.last();
            if (rs != null && rs.connected()) {
                rig.setFreqHz(p.freqHz());
                if (p.rigMode() != null && !p.rigMode().isBlank()) rig.setMode(p.rigMode());
            }
        }
        refresh();
        focus("callsign");
        if (ContestState.plugin() != null) updateDupe(ContestState.plugin());
    }

    static void drop(Parked p) { LIST.remove(p); refresh(); }

    private static void refresh() {
        BOX.getChildren().clear();
        int n = 1;
        for (Parked p : LIST) {
            Label chip = lbl("⏸ " + p.call() + (p.freqHz() > 0 ? "  " + mhz(p.freqHz()) : ""), "jl-clearbtn");
            chip.setStyle("-fx-cursor:hand;-fx-font-size:14px;-fx-font-weight:bold;-fx-padding:6 12;-fx-border-color:-ars-accent;-fx-border-radius:8;-fx-background-radius:8;");
            StringBuilder tip = new StringBuilder("Click to come back to " + p.call() + " (Ctrl+" + n + ")");
            if (p.freqHz() > 0) tip.append(": the radio is tuned back to ").append(mhz(p.freqHz())).append(' ').append(p.rigMode() == null ? "" : p.rigMode());
            for (Map.Entry<String, String> en : p.values().entrySet())
                if (!"callsign".equals(en.getKey()) && !en.getValue().isBlank()) tip.append('\n').append(en.getKey()).append(": ").append(en.getValue());
            com.ars.fx.shell.Tips.tip(chip, tip.toString());
            chip.setOnMouseClicked(e -> { if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) drop(p); else restore(p); });
            BOX.getChildren().add(chip);
            n++;
        }
        if (!LIST.isEmpty()) {
            Label hint = lbl("click to return · right-click to drop", "jl-emeta");
            BOX.getChildren().add(hint);
        }
    }

    static String mhz(long hz) { return String.format(Locale.ROOT, "%.3f", hz / 1e6); }
}
