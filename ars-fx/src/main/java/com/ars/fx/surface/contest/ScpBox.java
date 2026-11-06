package com.ars.fx.surface.contest;

import com.ars.fx.data.ScpData;
import com.ars.fx.data.ScpStore;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The Super Check Partial strip under the call field: as a call is typed, the known calls that contain it (click one to use it); and when what
 * is typed is not a known call but is one keystroke from some, those, as the likely right call. A one-line status says which release is in
 * use, or that the database has not been downloaded (and how to get it).
 */
public final class ScpBox extends VBox {
    private static final int SHOWN = 14, NEAR_SHOWN = 6;
    private static final java.util.Set<ScpStore.Kind> checked = java.util.concurrent.ConcurrentHashMap.newKeySet();      // one network check per list per run of the app; ScpStore also rations itself to once a day

    private final Label status = lbl("", "ct-note");
    private final FlowPane partial = new FlowPane(8, 4), near = new FlowPane(8, 4);
    private final Label nearHead = lbl("", "ct-note");
    private final Consumer<String> pick;
    private final java.util.function.Predicate<String> worked;     // is this call already in the log? Marked on its chip.
    private final ScpStore.Kind kind;
    private ScpData data = ScpData.EMPTY;
    private String lastTyped = "";

    public ScpBox(Consumer<String> pick) { this(pick, c -> false); }

    /** @param worked says whether a call is already in the log being worked in; its chip is dimmed and ticked. */
    public ScpBox(Consumer<String> pick, java.util.function.Predicate<String> worked) { this(pick, worked, ScpStore.Kind.MASTER); }

    /** @param kind which of the site's lists to check against (see {@link ScpStore#listFor}). */
    public ScpBox(Consumer<String> pick, java.util.function.Predicate<String> worked, ScpStore.Kind kind) {
        super(4);
        this.kind = kind == null ? ScpStore.Kind.MASTER : kind;
        this.pick = pick;
        this.worked = worked == null ? c -> false : worked;
        nearHead.setStyle("-fx-text-fill:-ars-warn;-fx-font-weight:bold;");
        for (FlowPane p : List.of(partial, near)) { p.managedProperty().bind(p.visibleProperty()); }
        nearHead.managedProperty().bind(nearHead.visibleProperty());
        getChildren().addAll(status, partial, nearHead, near);
        partial.setVisible(false); near.setVisible(false); nearHead.setVisible(false);
        load();
    }

    /** Reads the database off the UI thread, and once per run asks the site whether there is a newer one. */
    private void load() {
        Thread t = new Thread(() -> {
            ScpData d = ScpStore.current(kind);
            Platform.runLater(() -> { data = d; update(lastTyped); });
            if (checked.add(kind)) {
                ScpStore.Outcome o = ScpStore.refresh(kind, Instant.now(), false);
                if (o.changed()) Platform.runLater(() -> { data = ScpStore.current(kind); update(lastTyped); });
            }
        }, "scp-load");
        t.setDaemon(true); t.start();
    }

    /** Shows what the database says about the call as typed so far (call from the UI thread). */
    public void update(String typed) {
        lastTyped = typed == null ? "" : typed;
        partial.getChildren().clear(); near.getChildren().clear();
        partial.setVisible(false); near.setVisible(false); nearHead.setVisible(false);
        if (data.isEmpty()) {
            status.setText(ScpStore.autoUpdate() ? "Super Check Partial: downloading the call list… (needs a network connection)"
                    : "Super Check Partial is off — turn it on in Setup ▸ Data");
            return;
        }
        String core = ScpData.core(lastTyped);
        String head = "SUPER CHECK PARTIAL" + (kind == ScpStore.Kind.MASTER ? "" : " (" + kind.about + ")") + " · " + String.format("%,d", data.size()) + " calls" + (data.release().isEmpty() ? "" : " · release " + data.release());
        if (core.length() < 2) { status.setText(head); return; }
        ScpData.Result r = data.partial(core, SHOWN);
        boolean known = data.contains(core);
        status.setText(known ? "✓ " + core + " is in the Super Check Partial list" : head);
        for (String c : r.calls()) partial.getChildren().add(chip(c, c.equals(core)));
        if (r.total() > r.calls().size()) partial.getChildren().add(lbl("+" + (r.total() - r.calls().size()) + " more", "ct-note"));
        partial.setVisible(!partial.getChildren().isEmpty());
        if (!known && core.length() >= 3) {
            ScpData.Result n = data.near(core, NEAR_SHOWN);
            if (!n.calls().isEmpty()) {
                nearHead.setText(core + " is not in the list — did you mean:");
                for (String c : n.calls()) near.getChildren().add(chip(c, false));
                nearHead.setVisible(true); near.setVisible(true);
            } else if (r.total() == 0) {
                nearHead.setText(core + " is not in the Super Check Partial list — check the copy");
                nearHead.setVisible(true);
            }
        }
    }

    private Label chip(String call, boolean exact) {
        boolean did = !exact && worked.test(call);
        Label l = lbl(did ? call + " \u2713" : call, "ct-note");
        if (did) com.ars.fx.shell.Tips.tip(l, call + " is already in the log");
        l.setStyle("-fx-font-family:'JetBrains Mono';-fx-font-size:15px;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:1 6 1 6;-fx-background-radius:4;"
                + "-fx-background-color:" + (exact ? "-ars-ok-soft" : "-ars-surface-3") + ";-fx-text-fill:" + (exact ? "-ars-ok" : did ? "-ars-t3" : "-ars-t1") + ";" + (did ? "-fx-opacity:0.7;" : ""));
        l.setOnMouseClicked(e -> pick.accept(call));
        return l;
    }
}
