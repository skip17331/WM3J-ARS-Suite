package com.ars.fx.surface.contest;

import com.ars.fx.data.ScpData;
import com.ars.fx.data.ScpStore;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The Super Check Partial strip under the call field: as a call is typed, the known calls that contain it (click one to use it); and when what
 * is typed is not a known call but is one keystroke from some, those, as the likely right call. A one-line status says which release is in
 * use, or that the database has not been downloaded (and how to get it).
 */
public final class ScpBox extends VBox {
    private static final int SHOWN = 14, NEAR_SHOWN = 6;
    private static final AtomicBoolean checked = new AtomicBoolean();      // one network check per run of the app; ScpStore also rations itself to once a day

    private final Label status = lbl("", "ct-note");
    private final FlowPane partial = new FlowPane(8, 4), near = new FlowPane(8, 4);
    private final Label nearHead = lbl("", "ct-note");
    private final Consumer<String> pick;
    private ScpData data = ScpData.EMPTY;
    private String lastTyped = "";

    public ScpBox(Consumer<String> pick) {
        super(4);
        this.pick = pick;
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
            ScpData d = ScpStore.current();
            Platform.runLater(() -> { data = d; update(lastTyped); });
            if (checked.compareAndSet(false, true)) {
                ScpStore.Outcome o = ScpStore.refresh(Instant.now(), false);
                if (o.changed()) Platform.runLater(() -> { data = ScpStore.current(); update(lastTyped); });
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
        String head = "SUPER CHECK PARTIAL · " + String.format("%,d", data.size()) + " calls" + (data.release().isEmpty() ? "" : " · release " + data.release());
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
        Label l = lbl(call, "ct-note");
        l.setStyle("-fx-font-family:'JetBrains Mono';-fx-font-size:15px;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:1 6 1 6;-fx-background-radius:4;"
                + "-fx-background-color:" + (exact ? "-ars-ok-soft" : "-ars-surface-3") + ";-fx-text-fill:" + (exact ? "-ars-ok" : "-ars-t1") + ";");
        l.setOnMouseClicked(e -> pick.accept(call));
        return l;
    }
}
