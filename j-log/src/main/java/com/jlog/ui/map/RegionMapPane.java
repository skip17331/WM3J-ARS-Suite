package com.jlog.ui.map;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Clickable tile-grid map rendered as SVGPath nodes. Each region is a rounded-rectangle
 * {@link SVGPath} placed at integer (col,row) coordinates. The tile layout is a proper
 * cartographic convention ("tile grid map") that gives every US state / Canadian province
 * equal visual weight — appropriate for a contest log where a worked-or-not status matters
 * more than geographic accuracy.
 *
 * Regions carry four visual states via CSS classes:
 *   .region-tile-unworked   — base (gray)
 *   .region-tile-worked     — green
 *   .region-tile-current    — blue outline (currently-entered QSO)
 *   .region-tile-invalid    — red outline
 *
 * {@link #setOnRegionClicked(Consumer)} fires the region id when any tile is clicked.
 * {@link #setTooltipProvider(Function)} supplies hover text (name / first-worked / qso-count).
 */
public class RegionMapPane extends Pane {

    private static final int TILE_W = 44;
    private static final int TILE_H = 36;
    private static final int GAP    = 4;

    private final Map<String, SVGPath> shapes = new LinkedHashMap<>();
    private final Map<String, Label>   labels = new LinkedHashMap<>();
    private final Map<String, Tooltip> tips   = new LinkedHashMap<>();
    private final Set<String> worked = new HashSet<>();
    private String current;
    private String invalid;
    private Consumer<String>        onRegionClicked = id -> {};
    private Function<String, String> tooltipProvider = id -> id;

    public RegionMapPane(Map<String, int[]> layout) {
        getStyleClass().add("region-map-pane");
        setPickOnBounds(false);
        for (Map.Entry<String, int[]> e : layout.entrySet()) {
            String id  = e.getKey();
            int    col = e.getValue()[0];
            int    row = e.getValue()[1];
            int    x   = col * (TILE_W + GAP);
            int    y   = row * (TILE_H + GAP);

            SVGPath p = new SVGPath();
            p.setId(id);
            // Rounded rect: move, top edge, corner, right edge, corner, bottom edge, corner, left edge, corner.
            int r = 4;
            p.setContent(String.format(Locale.ROOT,
                "M %d,%d h %d a %d,%d 0 0 1 %d,%d v %d a %d,%d 0 0 1 -%d,%d h -%d a %d,%d 0 0 1 -%d,-%d v -%d a %d,%d 0 0 1 %d,-%d z",
                x + r, y, TILE_W - 2*r,
                r, r, r, r,
                TILE_H - 2*r,
                r, r, r, r,
                TILE_W - 2*r,
                r, r, r, r,
                TILE_H - 2*r,
                r, r, r, r));
            p.getStyleClass().addAll("region-tile", "region-tile-unworked");
            p.setCursor(Cursor.HAND);
            p.setOnMouseClicked(ev -> onRegionClicked.accept(id));

            Tooltip tip = new Tooltip(id);
            tip.setShowDelay(Duration.millis(150));
            tip.setShowDuration(Duration.seconds(10));
            p.setOnMouseEntered(ev -> tip.setText(tooltipProvider.apply(id)));
            Tooltip.install(p, tip);
            tips.put(id, tip);

            Label lbl = new Label(id);
            lbl.getStyleClass().add("region-tile-label");
            lbl.setMouseTransparent(true);
            lbl.setLayoutX(x);
            lbl.setLayoutY(y);
            lbl.setPrefWidth(TILE_W);
            lbl.setPrefHeight(TILE_H);
            lbl.setAlignment(Pos.CENTER);

            getChildren().addAll(p, lbl);
            shapes.put(id, p);
            labels.put(id, lbl);
        }
        int maxCol = layout.values().stream().mapToInt(v -> v[0]).max().orElse(0) + 1;
        int maxRow = layout.values().stream().mapToInt(v -> v[1]).max().orElse(0) + 1;
        setPrefSize(maxCol * (TILE_W + GAP), maxRow * (TILE_H + GAP));
    }

    public Set<String> regionIds() { return Collections.unmodifiableSet(shapes.keySet()); }

    public void setWorked(String id, boolean isWorked) {
        if (!shapes.containsKey(id)) return;
        if (isWorked) worked.add(id); else worked.remove(id);
        restyle(id);
    }

    /** Replace the worked-set in one call (idempotent). */
    public void setAllWorked(Collection<String> ids) {
        Set<String> target = new HashSet<>(ids);
        Set<String> union  = new HashSet<>(worked);
        union.addAll(target);
        for (String id : union) setWorked(id, target.contains(id));
    }

    public void setCurrent(String id) {
        String prev = this.current;
        this.current = id;
        if (prev != null) restyle(prev);
        if (id   != null) restyle(id);
    }

    public void setInvalid(String id) {
        String prev = this.invalid;
        this.invalid = id;
        if (prev != null) restyle(prev);
        if (id   != null) restyle(id);
    }

    public void clearWorked() {
        Set<String> previous = new HashSet<>(worked);
        worked.clear();
        previous.forEach(this::restyle);
    }

    public void setOnRegionClicked(Consumer<String> cb)        { this.onRegionClicked = cb != null ? cb : id -> {}; }
    public void setTooltipProvider(Function<String, String> p) { this.tooltipProvider = p != null ? p : id -> id; }

    private void restyle(String id) {
        SVGPath p = shapes.get(id);
        if (p == null) return;
        p.getStyleClass().removeAll(
            "region-tile-unworked", "region-tile-worked",
            "region-tile-current", "region-tile-invalid");
        if (id.equals(invalid))       p.getStyleClass().add("region-tile-invalid");
        else if (id.equals(current))  p.getStyleClass().add("region-tile-current");
        else if (worked.contains(id)) p.getStyleClass().add("region-tile-worked");
        else                          p.getStyleClass().add("region-tile-unworked");
    }

    // -----------------------------------------------------------------
    // Factories — US 50-state tile grid and Canadian 13-province grid.
    // Coordinates follow the NPR-style tile grid convention.
    // -----------------------------------------------------------------

    public static RegionMapPane usStates() {
        Map<String, int[]> m = new LinkedHashMap<>();
        put(m, "ME", 11, 0);
        put(m, "VT",  9, 1); put(m, "NH", 10, 1);
        put(m, "WA",  1, 2); put(m, "ID",  2, 2); put(m, "MT",  3, 2); put(m, "ND",  4, 2);
        put(m, "MN",  5, 2); put(m, "WI",  6, 2); put(m, "MI",  7, 2);
        put(m, "NY",  9, 2); put(m, "MA", 10, 2); put(m, "RI", 11, 2);
        put(m, "OR",  1, 3); put(m, "NV",  2, 3); put(m, "WY",  3, 3); put(m, "SD",  4, 3);
        put(m, "IA",  5, 3); put(m, "IL",  6, 3); put(m, "IN",  7, 3); put(m, "OH",  8, 3);
        put(m, "PA",  9, 3); put(m, "NJ", 10, 3); put(m, "CT", 11, 3);
        put(m, "CA",  1, 4); put(m, "UT",  2, 4); put(m, "CO",  3, 4); put(m, "NE",  4, 4);
        put(m, "MO",  5, 4); put(m, "KY",  6, 4); put(m, "WV",  7, 4); put(m, "VA",  8, 4);
        put(m, "MD",  9, 4); put(m, "DE", 10, 4);
        put(m, "AZ",  2, 5); put(m, "NM",  3, 5); put(m, "KS",  4, 5); put(m, "AR",  5, 5);
        put(m, "TN",  6, 5); put(m, "NC",  7, 5); put(m, "SC",  8, 5);
        put(m, "AK",  0, 6); put(m, "OK",  4, 6); put(m, "LA",  5, 6); put(m, "MS",  6, 6);
        put(m, "AL",  7, 6); put(m, "GA",  8, 6);
        put(m, "HI",  0, 7); put(m, "TX",  3, 7); put(m, "FL",  9, 7);
        return new RegionMapPane(m);
    }

    public static RegionMapPane canada() {
        Map<String, int[]> m = new LinkedHashMap<>();
        put(m, "NU", 5, 0);
        put(m, "YT", 1, 1); put(m, "NT", 2, 1); put(m, "NL", 7, 1);
        put(m, "BC", 1, 2); put(m, "AB", 2, 2); put(m, "SK", 3, 2); put(m, "MB", 4, 2);
        put(m, "ON", 5, 2); put(m, "QC", 6, 2); put(m, "NB", 7, 2);
        put(m, "NS", 7, 3); put(m, "PE", 8, 3);
        return new RegionMapPane(m);
    }

    /**
     * ARRL / RAC Sweepstakes section tile map. Sections arranged in columns by
     * FCC call area (W0..W9, VE, and non-contiguous US). Column order roughly
     * mirrors geographic layout (west → east for W-areas, Canada on the right).
     */
    public static RegionMapPane ssSections() {
        Map<String, int[]> m = new LinkedHashMap<>();
        // Column 0: non-contiguous US (HI is W6/KH6 territory; PR/VI are DXCC territories
        // that ARRL SS counts as sections).
        put(m, "HI", 0, 0); put(m, "PR", 0, 1); put(m, "VI", 0, 2);
        // Column 1: W6 (9)
        int c = 1;
        String[] w6 = {"EB","LAX","ORG","PAC","SB","SCV","SDG","SF","SJV"};
        for (int i = 0; i < w6.length; i++) put(m, w6[i], c, i);
        // Column 2: W7 (9)
        c = 2;
        String[] w7 = {"AK","AZ","EWA","ID","MT","NV","OR","UT","WWA"};
        for (int i = 0; i < w7.length; i++) put(m, w7[i], c, i);
        // Column 3: W0 (8)
        c = 3;
        String[] w0 = {"CO","IA","KS","MO","NE","ND","SD","WY"};
        for (int i = 0; i < w0.length; i++) put(m, w0[i], c, i);
        // Column 4: W5 (8)
        c = 4;
        String[] w5 = {"AR","LA","MS","NM","NTX","OK","STX","WTX"};
        for (int i = 0; i < w5.length; i++) put(m, w5[i], c, i);
        // Column 5: W9 (3)
        c = 5;
        String[] w9 = {"IL","IN","WI"};
        for (int i = 0; i < w9.length; i++) put(m, w9[i], c, i);
        // Column 6: W8 (3)
        c = 6;
        String[] w8 = {"MI","OH","WV"};
        for (int i = 0; i < w8.length; i++) put(m, w8[i], c, i);
        // Column 7: W4 (8)
        c = 7;
        String[] w4 = {"AL","FL","GA","KY","NC","SC","TN","VA"};
        for (int i = 0; i < w4.length; i++) put(m, w4[i], c, i);
        // Column 8: W3 (4)
        c = 8;
        String[] w3 = {"DE","EPA","MDC","WPA"};
        for (int i = 0; i < w3.length; i++) put(m, w3[i], c, i);
        // Column 9: W2 (5)
        c = 9;
        String[] w2 = {"ENY","NLI","NNJ","SNJ","WNY"};
        for (int i = 0; i < w2.length; i++) put(m, w2[i], c, i);
        // Column 10: W1 (7)
        c = 10;
        String[] w1 = {"CT","EMA","ME","NH","RI","VT","WMA"};
        for (int i = 0; i < w1.length; i++) put(m, w1[i], c, i);
        // Column 11-12: VE (15) split across 2 columns so one isn't 15 tall
        String[] ve = {"AB","BC","MB","NB","NL","NT","NS","NU","ONN","ONE","ONS","QC","SK","YT","MAR"};
        for (int i = 0; i < ve.length; i++) put(m, ve[i], 11 + (i / 8), i % 8);
        return new RegionMapPane(m);
    }

    private static void put(Map<String, int[]> m, String id, int col, int row) {
        m.put(id, new int[]{col, row});
    }
}
