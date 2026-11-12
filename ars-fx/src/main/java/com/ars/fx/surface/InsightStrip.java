package com.ars.fx.surface;

import com.ars.fx.data.ClusterClient;
import com.ars.fx.shell.AutoPanel;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The Cockpit dashboard's insight strip: four live stat tiles and a band-activity chart, in two sizes.
 *
 * <ul>
 *   <li><b>Collapsed</b> (the default): one slim line of small type, with the bands as coloured chips that carry their label and count
 *       inside them. It takes little height.</li>
 *   <li><b>Open</b>: the tall version (big numbers, bars). Hovering the strip floats it open over the cards below it without moving
 *       anything; <b>Pin</b> keeps it open in the layout and remembers that (same Pin as the trays).</li>
 * </ul>
 * Both views are kept current by {@link #update}.
 */
final class InsightStrip extends Pane {
    static final String KEY = "insight";
    private static final String[] BANDS = {"160m","80m","60m","40m","30m","20m","17m","15m","12m","10m","6m","2m","70cm"};
    private static final String[][] STATS = {            // key (open), key (collapsed), tone
        {"QSOS TODAY", "TODAY", "amber"}, {"IN LOG", "LOG", "cyan"}, {"SPOTS ON AIR", "SPOTS", "green"}, {"NEW ONES", "NEW", "violet"}};
    private static final String[] SUBS = {"logged today", "total QSOs", "cluster feed", "needed spots"};

    private final Label[] bigVals = new Label[4], smallVals = new Label[4];
    private final Label[] barCnt = new Label[BANDS.length], chipCnt = new Label[BANDS.length];
    private final Region[] bars = new Region[BANDS.length];
    private final Region full, compact;
    private final DropShadow floatShadow = new DropShadow(20, Color.rgb(0, 0, 0, 0.55));
    private boolean hover;
    private final PauseTransition openT = new PauseTransition(Duration.millis(160)), closeT = new PauseTransition(Duration.millis(450));
    private long countsAt; private int today, log;

    InsightStrip() {
        full = buildFull(); compact = buildCompact();
        getChildren().addAll(compact, full);
        setMinWidth(0); setMinHeight(USE_PREF_SIZE);          // never squeezed: the cards scroll, this does not
        setViewOrder(-1);                                   // the floating open view must draw over the cards that follow it
        openT.setOnFinished(e -> { hover = true; relayout(); });
        closeT.setOnFinished(e -> { hover = false; relayout(); });
        setOnMouseEntered(e -> { closeT.stop(); if (!pinned() && !hover) openT.playFromStart(); });
        setOnMouseExited(e -> { openT.stop(); if (hover) closeT.playFromStart(); });
        relayout();
    }

    private static boolean pinned() { return AutoPanel.isPinned(KEY); }

    private void relayout() {
        boolean pin = pinned(), open = pin || hover;
        full.setVisible(open); compact.setVisible(!open);
        full.setEffect(pin ? null : floatShadow);
        full.setStyle(pin ? "" : "-fx-background-color:-ars-bg; -fx-padding:0 0 10 0;");   // opaque while floating, so the cards under it do not show through
        requestLayout();
        if (getParent() != null) getParent().requestLayout();
    }

    @Override protected double computePrefHeight(double w) {
        double width = w > 0 ? w : getWidth() > 0 ? getWidth() : 900;
        return pinned() ? full.prefHeight(width) : compact.prefHeight(width);
    }
    @Override protected double computePrefWidth(double h) { return 600; }
    @Override protected double computeMinWidth(double h) { return 0; }
    @Override protected void layoutChildren() {
        double w = getWidth();
        compact.resizeRelocate(0, 0, w, compact.prefHeight(w));
        full.resizeRelocate(0, 0, w, full.prefHeight(w));
    }

    // ---- open view -----------------------------------------------------------
    private Region buildFull() {
        HBox tiles = new HBox(12);
        for (int i = 0; i < 4; i++) {
            bigVals[i] = lbl("0", "stat-v");
            VBox t = new VBox(2, lbl(STATS[i][0], "stat-k"), bigVals[i], lbl(SUBS[i], "stat-sub"));
            t.getStyleClass().addAll("stat-tile", STATS[i][2]); t.setMinWidth(0);
            HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE);
            tiles.getChildren().add(t);
        }
        tiles.setMinWidth(0); HBox.setHgrow(tiles, Priority.ALWAYS);

        HBox chart = new HBox(8); chart.setAlignment(Pos.BOTTOM_LEFT); chart.setMinHeight(86); chart.setPrefHeight(86);
        for (int i = 0; i < BANDS.length; i++) {
            barCnt[i] = lbl("", "band-cnt"); bars[i] = new Region();
            bars[i].getStyleClass().addAll("bandbar", "band-" + BANDS[i]);
            VBox col = new VBox(3, barCnt[i], bars[i], lbl(BANDS[i].replace("m", ""), "band-nm")); col.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(col, Priority.ALWAYS); col.setMaxWidth(Double.MAX_VALUE); col.setMinWidth(0);
            chart.getChildren().add(col);
        }
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox head = new HBox(8, lbl("BAND ACTIVITY", "stat-k"), sp, AutoPanel.pinButton(KEY, this::relayout)); head.setAlignment(Pos.CENTER_LEFT);
        VBox bandCard = new VBox(6, head, chart); bandCard.getStyleClass().add("band-card"); bandCard.setMinWidth(0);
        HBox.setHgrow(bandCard, Priority.ALWAYS); bandCard.setMaxWidth(Double.MAX_VALUE); bandCard.setPrefWidth(430);

        HBox row = new HBox(14, tiles, bandCard); row.setMinWidth(0); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ---- collapsed view ------------------------------------------------------
    private Region buildCompact() {
        HBox stats = new HBox(16); stats.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 4; i++) {
            smallVals[i] = lbl("0", "cs-v", STATS[i][2]);
            HBox s = new HBox(5, lbl(STATS[i][1], "cs-k"), smallVals[i]); s.setAlignment(Pos.BASELINE_LEFT);
            stats.getChildren().add(s);
        }
        HBox chipRow = new HBox(5); chipRow.setAlignment(Pos.CENTER_RIGHT); chipRow.setMinWidth(0);
        for (int i = 0; i < BANDS.length; i++) {
            chipCnt[i] = lbl("", "bchip");                                   // the colour block: the spot count goes inside it
            chipCnt[i].getStyleClass().add("band-" + BANDS[i]);
            VBox c = new VBox(2, chipCnt[i], lbl(BANDS[i].replace("m", ""), "bchip-n")); c.setAlignment(Pos.CENTER);   // the band sits under it
            chipRow.getChildren().add(c);
        }
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(18, stats, sp, chipRow, AutoPanel.pinButton(KEY, this::relayout));
        row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("insight-compact"); row.setMinWidth(0);
        return row;
    }

    // ---- data ----------------------------------------------------------------
    void update(List<ClusterClient.Spot> spots, int needed) {
        long now = System.currentTimeMillis();
        if (now - countsAt > 5000) {                         // the log counts change only when a QSO is saved
            countsAt = now;
            try { today = com.ars.fx.data.JLogDb.countToday(); log = com.ars.fx.data.JLogDb.count(); } catch (Exception ignored) {}
        }
        String[] v = {Integer.toString(today), Integer.toString(log), Integer.toString(spots.size()), Integer.toString(needed)};
        for (int i = 0; i < 4; i++) { bigVals[i].setText(v[i]); smallVals[i].setText(v[i]); }
        int[] n = new int[BANDS.length]; int max = 1;
        for (ClusterClient.Spot s : spots) {
            String b = s.band() == null ? "" : s.band().trim().toLowerCase();
            for (int i = 0; i < BANDS.length; i++) if (BANDS[i].equals(b)) { n[i]++; max = Math.max(max, n[i]); break; }
        }
        for (int i = 0; i < n.length; i++) {
            double h = n[i] == 0 ? 3 : 8 + 50.0 * n[i] / max;
            bars[i].setMinHeight(h); bars[i].setPrefHeight(h); bars[i].setMaxHeight(h);
            bars[i].setOpacity(n[i] == 0 ? 0.25 : 1);
            barCnt[i].setText(n[i] == 0 ? "" : Integer.toString(n[i]));
            chipCnt[i].setText(n[i] == 0 ? "" : Integer.toString(n[i]));
            chipCnt[i].setOpacity(n[i] == 0 ? 0.3 : 1);
        }
    }
}
