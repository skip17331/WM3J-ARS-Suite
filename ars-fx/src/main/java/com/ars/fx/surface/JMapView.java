package com.ars.fx.surface;

import com.ars.fx.mock.Mock;
import com.ars.fx.shell.Shell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/** J-Map azimuthal propagation map (handoff shot 03), mock data. */
public final class JMapView {
    private JMapView() {}

    // ── NCDXF/IARU International Beacon Project ────────────────────────────────
    // 18 beacons in transmit-slot order; each transmits 10 s per band on a 3-min cycle.
    private static final String[][] BEACONS = {
            {"4U1UN","40.75","-73.97"}, {"VE8AT","79.99","-85.95"}, {"W6WX","37.37","-121.92"},
            {"KH6RS","20.79","-156.46"}, {"ZL6B","-41.05","175.46"}, {"VK6RBP","-32.10","116.05"},
            {"JA2IGY","34.46","136.79"}, {"RR9O","54.98","82.89"}, {"VR2B","22.27","114.16"},
            {"4S7B","6.91","79.87"}, {"ZS6DN","-25.91","28.20"}, {"5Z4B","-1.38","36.68"},
            {"4X6TU","32.11","34.80"}, {"OH2B","60.19","24.96"}, {"CS3B","32.65","-16.91"},
            {"LU4AA","-34.61","-58.37"}, {"OA4B","-12.07","-77.08"}, {"YV5B","10.42","-66.98"}};
    private static final String[] BEACON_BANDS = {"20m","17m","15m","12m","10m"};
    private static boolean showBeacons = false;
    private static int beaconBand = 0;
    private static Runnable currentMapRepaint;
    private static javafx.animation.Timeline beaconTicker;
    /** Beacon index transmitting on band b right now (slot = UTC%180 /10; band offsets by one slot each). */
    private static int activeBeacon(int band) {
        int slot = (int) ((java.time.Instant.now().getEpochSecond() % 180) / 10);
        return ((slot - band) % 18 + 18) % 18;
    }
    private static void repaintMap() { if (currentMapRepaint != null) currentMapRepaint.run(); }
    /** Dim the regular DX spots while a propagation overlay (beacons / who's-hearing-me) is active. */
    private static boolean dimSpots() { return showBeacons || showHeardBy; }

    /** Point the beam wedge at a heading (e.g. after rotating to a clicked spot). */
    public static void setBeam(double deg) { beamDeg = ((deg % 360) + 360) % 360; repaintMap(); updateMapTopbar(); }

    // ── clickable map dots: each paint records the on-screen spot positions so a
    //    click can be hit-tested back to the spot that was drawn there. ──────────
    private record Dot(double x, double y, MapSpot s) {}
    private static final java.util.List<Dot> paintedDots = new java.util.ArrayList<>();
    private static void handleMapClick(Node source, double x, double y) {
        Dot best = null; double bestD = Double.MAX_VALUE;
        for (Dot d : paintedDots) { double dd = Math.hypot(d.x() - x, d.y() - y); if (dd < bestD) { bestD = dd; best = d; } }
        if (best != null && bestD <= 11) {
            MapSpot s = best.s();
            SpotAction.onSpot(source, s.call(), s.freqHz(), s.mode(), s.band(), s.lat(), s.lon());
        }
    }

    // ── overlay toggles (left panel) ──────────────────────────────────────────
    private static boolean ovGrayline = true, ovBasemap = true, ovBeam = true, ovRings = true, ovCallsigns = true, ovNeededOnly = false;
    private static double beamDeg = 50;
    private static boolean overlayFlag(int i) {
        return switch (i) { case 0 -> ovGrayline; case 1 -> ovBasemap; case 2 -> ovBeam; case 3 -> ovRings; case 4 -> ovCallsigns; default -> ovNeededOnly; };
    }
    private static void setOverlayFlag(int i, boolean v) {
        switch (i) { case 0 -> ovGrayline = v; case 1 -> ovBasemap = v; case 2 -> ovBeam = v; case 3 -> ovRings = v; case 4 -> ovCallsigns = v; default -> ovNeededOnly = v; }
    }
    private static java.util.Set<String> workedCallsCache = java.util.Set.of();
    private static long workedCallsAt = 0;
    private static java.util.Set<String> workedCalls() {
        long n = System.currentTimeMillis();
        if (n - workedCallsAt > 5000) { workedCallsCache = new java.util.HashSet<>(com.ars.fx.data.JLogDb.distinctCalls()); workedCallsAt = n; }
        return workedCallsCache;
    }
    /** "Needed only" — hide spots whose call is already in the log. */
    private static boolean spotHidden(String call) { return ovNeededOnly && workedCalls().contains(call.toUpperCase()); }

    // ---- live DX-cluster spots on the map ---------------------------------
    private static int spotMode = 0;                                              // 0 All · 1 CW · 2 Phone · 3 Digi
    private static final java.util.Set<String> spotBands = new java.util.HashSet<>();   // empty = all bands
    private static int spotAgeMax = 20;                                           // minutes
    private static Runnable currentSpotsRefresh;

    /** A positioned cluster spot (geo-located via {@link com.ars.fx.data.Geo}). */
    public record MapSpot(String call, double lat, double lon, String band, String mode, long arrivalMs, long freqHz, Color color) {}

    /** Geo-locate + filter the given raw cluster spots (skips calls with no known location). */
    public static java.util.List<MapSpot> spotsFrom(java.util.List<com.ars.fx.data.ClusterClient.Spot> raw) {
        java.util.List<MapSpot> out = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();
        for (com.ars.fx.data.ClusterClient.Spot s : raw) {
            double[] ll = com.ars.fx.data.Geo.of(s.call());
            if (ll == null) continue;
            if (!modePass(s.mode()) || !bandPass(s.band()) || (now - s.arrivalMs()) / 60000 > spotAgeMax) continue;
            out.add(new MapSpot(s.call(), ll[0], ll[1], s.band(), s.mode(), s.arrivalMs(), s.freqHz(), bandColor(s.band())));
        }
        return out;
    }
    private static java.util.List<MapSpot> liveSpots() { return spotsFrom(com.ars.fx.data.ClusterClient.getInstance().spots()); }
    private static boolean modePass(String m) {
        if (spotMode == 0) return true;
        String u = m == null ? "" : m.toUpperCase();
        boolean cw = u.equals("CW");
        boolean phone = u.equals("SSB") || u.equals("USB") || u.equals("LSB") || u.equals("FM") || u.equals("AM") || u.contains("PHONE");
        return spotMode == 1 ? cw : spotMode == 2 ? phone : (!cw && !phone);   // Digi = everything else
    }
    private static boolean bandPass(String b) { return spotBands.isEmpty() || spotBands.contains(b); }
    private static void refreshSpots() { repaintMap(); if (currentSpotsRefresh != null) currentSpotsRefresh.run(); }

    private static void startBeaconTicker() {
        if (beaconTicker != null) return;
        // 1 Hz tick drives the beacon cycle and the live gray line (sun position,
        // az/el and SR/SS in the solar readout track the wall clock).
        beaconTicker = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
            updateMapTopbar();                                 // ticking clock + live SPOTS/SHOWN/GRAY-LINE/BEAM
            if (showBeacons || ovGrayline) repaintMap();
        }));
        beaconTicker.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        beaconTicker.play();
    }
    /** Projects (lat,lon) to canvas {x,y}, or null when off-map (azimuthal beyond the disk). */
    private interface Proj { double[] at(double lat, double lon); }
    private static void drawBeacons(GraphicsContext g, Proj proj) {
        if (!showBeacons) return;
        int active = activeBeacon(beaconBand);
        double[] q = proj.at(Q_LAT, Q_LON);
        double[] ab = proj.at(Double.parseDouble(BEACONS[active][1]), Double.parseDouble(BEACONS[active][2]));
        if (q != null && ab != null) {
            g.setStroke(Color.web("#ffd84a", 0.7)); g.setLineWidth(1.6); g.setLineDashes(6, 5);
            g.strokeLine(q[0], q[1], ab[0], ab[1]); g.setLineDashes(null);
        }
        for (int i = 0; i < BEACONS.length; i++) {
            double[] p = proj.at(Double.parseDouble(BEACONS[i][1]), Double.parseDouble(BEACONS[i][2]));
            if (p == null) continue;
            double x = p[0], y = p[1];
            if (i == active) {
                g.setFill(Color.web("#ffd33d", 0.18)); g.fillOval(x - 14, y - 14, 28, 28);
                g.setFill(Color.web("#fff3b0")); g.fillOval(x - 6, y - 6, 12, 12);
                g.setStroke(Color.web("#ffcf3a")); g.setLineWidth(1.6); g.strokeOval(x - 6, y - 6, 12, 12);
                g.setFill(Color.web("#fff3b0")); g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12)); g.setTextAlign(TextAlignment.LEFT);
                g.fillText(BEACONS[i][0] + " · " + BEACON_BANDS[beaconBand], x + 10, y + 4);
            } else {
                g.setStroke(Color.web("#0b0f14", 0.7)); g.setLineWidth(1.5); g.strokeOval(x - 3.2, y - 3.2, 6.4, 6.4);
                g.setFill(Color.web("#cdd6e0", 0.55)); g.fillOval(x - 3, y - 3, 6, 6);
                g.setFill(Color.web("#9aa7b4", 0.7)); g.setFont(Font.font("JetBrains Mono", 9)); g.setTextAlign(TextAlignment.LEFT);
                g.fillText(BEACONS[i][0], x + 7, y + 3);
            }
        }
    }

    // ── "Who's hearing me" — RBN skimmers that spotted our call ───────────────
    private static boolean showHeardBy = false;
    private static void drawHeardBy(GraphicsContext g, Proj proj) {
        if (!showHeardBy) return;
        java.util.List<com.ars.fx.data.HeardByClient.HeardSpot> spots = com.ars.fx.data.HeardByClient.getInstance().spots();
        for (com.ars.fx.data.HeardByClient.HeardSpot s : spots) {
            double[] ll = com.ars.fx.data.Geo.of(s.spotter());
            if (ll == null) continue;
            Color col = bandColor(s.band());
            drawGcPath(g, proj, Q_LAT, Q_LON, ll[0], ll[1], Color.web(toHex(col), 0.7));
            double[] p = proj.at(ll[0], ll[1]);
            if (p == null) continue;
            g.setStroke(Color.web("#0b0f14", 0.8)); g.setLineWidth(2); g.strokeOval(p[0] - 4, p[1] - 4, 8, 8);
            g.setFill(col); g.fillOval(p[0] - 4, p[1] - 4, 8, 8);
            g.setFill(Color.web("#e8eef4", 0.85)); g.setFont(Font.font("JetBrains Mono", 9)); g.setTextAlign(TextAlignment.LEFT);
            g.fillText(s.spotter() + " " + s.band(), p[0] + 7, p[1] + 3);
        }
    }
    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }
    private static Color bandColor(String band) {
        switch (band == null ? "" : band) {
            case "160m": return Color.web("#9ad04f"); case "80m": return Color.web("#62c0e8");
            case "40m": return Color.web("#6fc884");  case "30m": return Color.web("#54d0c8");
            case "20m": return Color.web("#31bdda");  case "17m": return Color.web("#b191ea");
            case "15m": return Color.web("#eebb58");  case "12m": return Color.web("#f0a35a");
            case "10m": return Color.web("#ef7d83");  default: return Color.web("#9aa7b4");
        }
    }
    /** Great-circle path between two geographic points, projected + clipped to the map. */
    private static void drawGcPath(GraphicsContext g, Proj proj, double lat1, double lon1, double lat2, double lon2, Color col) {
        g.setStroke(col); g.setLineWidth(1.4);
        int N = 24; double prevX = 0, prevY = 0; boolean have = false;
        for (int i = 0; i <= N; i++) {
            double[] pt = gcPoint(lat1, lon1, lat2, lon2, (double) i / N);
            double[] xy = proj.at(pt[0], pt[1]);
            if (xy == null) { have = false; continue; }
            if (have && Math.abs(xy[0] - prevX) < 350) g.strokeLine(prevX, prevY, xy[0], xy[1]);
            prevX = xy[0]; prevY = xy[1]; have = true;
        }
    }
    private static double[] gcPoint(double lat1, double lon1, double lat2, double lon2, double f) {
        double p1 = Math.toRadians(lat1), l1 = Math.toRadians(lon1), p2 = Math.toRadians(lat2), l2 = Math.toRadians(lon2);
        double d = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((p2 - p1) / 2), 2) + Math.cos(p1) * Math.cos(p2) * Math.pow(Math.sin((l2 - l1) / 2), 2)));
        if (d == 0) return new double[]{lat1, lon1};
        double A = Math.sin((1 - f) * d) / Math.sin(d), B = Math.sin(f * d) / Math.sin(d);
        double x = A * Math.cos(p1) * Math.cos(l1) + B * Math.cos(p2) * Math.cos(l2);
        double y = A * Math.cos(p1) * Math.sin(l1) + B * Math.cos(p2) * Math.sin(l2);
        double z = A * Math.sin(p1) + B * Math.sin(p2);
        return new double[]{Math.toDegrees(Math.atan2(z, Math.sqrt(x * x + y * y))), Math.toDegrees(Math.atan2(y, x))};
    }

    public static Region build() {
        Region dock = Shell.dock("map");
        // station QTH + call from the J-Hub config
        Q_LAT = qdbl(com.ars.fx.data.HubConfig.get("station.lat", "39.7456"), 39.7456);
        Q_LON = qdbl(com.ars.fx.data.HubConfig.get("station.lon", "-76.96"), -76.96);
        myCall = com.ars.fx.data.HubConfig.call();
        String[][] stats = {{"SPOTS", String.valueOf(com.ars.fx.data.ClusterClient.getInstance().spots().size())},
                {"SHOWN", String.valueOf(liveSpots().size())},
                {"GRAY LINE", grayLineStat()},
                {"BEAM", String.format("%03d°", (int) beamDeg), "accent"}};
        HBox top = Shell.topBar("map","map","J-Map","Propagation & spots · " + com.ars.fx.data.HubConfig.grid(), stats, nowUtc());

        // projection toggle: azimuthal canvas ⇄ rectangular blue-marble map
        StackPane mapHolder = new StackPane(); HBox.setHgrow(mapHolder, Priority.ALWAYS);
        int[] proj = {0};
        Runnable renderMap = () -> mapHolder.getChildren().setAll(proj[0] == 1 ? rectMap() : aziArea());
        renderMap.run();
        startBeaconTicker();
        com.ars.fx.data.HeardByClient.getInstance().setListener(c -> { if (showHeardBy) repaintMap(); });
        com.ars.fx.data.HeardByClient.getInstance().start();

        // live DX-cluster spots feed the map + the rail spot list
        VBox spotsBox = new VBox();
        fillSpots(spotsBox);
        currentSpotsRefresh = () -> fillSpots(spotsBox);
        com.ars.fx.data.ClusterClient cc = com.ars.fx.data.ClusterClient.getInstance();
        cc.setListener(c -> javafx.application.Platform.runLater(JMapView::refreshSpots));
        cc.start();

        // collapsible left controls column
        StackPane leftHolder = new StackPane(); leftHolder.setAlignment(Pos.TOP_LEFT);
        int[] leftCollapsed = {0};
        java.util.function.IntConsumer onProj = i -> { proj[0] = i; renderMap.run(); };
        Runnable[] rebuildLeft = new Runnable[1];
        rebuildLeft[0] = () -> leftHolder.getChildren().setAll(leftCollapsed[0] == 1
                ? leftRail(() -> { leftCollapsed[0] = 0; rebuildLeft[0].run(); })
                : leftPanel(onProj, () -> { leftCollapsed[0] = 1; rebuildLeft[0].run(); }));
        rebuildLeft[0].run();

        HBox center = new HBox(leftHolder, mapHolder); center.setFillHeight(true);
        Region rail = Shell.rail(railDrawers(spotsBox));
        mapFrame = (Region) Shell.frame(dock, top, center, rail, tickerBar());
        return mapFrame;
    }

    private static Region mapFrame;
    private static String nowUtc() { return java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")); }
    private static String grayLineStat() {
        double[] rs = solarRiseSet();
        return Double.isNaN(rs[0]) ? (rs[2] > 0 ? "sun up all day" : "sun down all day") : "SR " + hhmm(rs[0]) + " · SS " + hhmm(rs[1]);
    }
    /** Live topbar: ticking UTC clock + SPOTS / SHOWN / GRAY-LINE / BEAM. */
    private static void updateMapTopbar() {
        if (mapFrame == null) return;
        if (mapFrame.lookup(".sx-clock-v") instanceof Label l) l.setText(nowUtc());
        java.util.List<Label> vs = new java.util.ArrayList<>(); collectStatValues(mapFrame, vs);
        if (vs.size() >= 4) {
            vs.get(0).setText(String.valueOf(com.ars.fx.data.ClusterClient.getInstance().spots().size()));
            vs.get(1).setText(String.valueOf(liveSpots().size()));
            vs.get(2).setText(grayLineStat());
            vs.get(3).setText(String.format("%03d°", (int) beamDeg));
        }
    }
    private static void collectStatValues(Node n, java.util.List<Label> out) {
        if (n instanceof Label l && l.getStyleClass().contains("sx-stat-v")) out.add(l);
        if (n instanceof javafx.scene.Parent p) for (Node c : p.getChildrenUnmodifiable()) collectStatValues(c, out);
    }

    // ---- bottom alert scroller (port of original J-Map DxScrollerBar) ------
    private static Region tickerBar() {
        double H = 42;
        Canvas canvas = new Canvas();
        Pane pane = new Pane(canvas);
        pane.setMinHeight(H); pane.setPrefHeight(H); pane.setMaxHeight(H);
        pane.setStyle("-fx-background-color:#11161d;");
        canvas.widthProperty().bind(pane.widthProperty());
        canvas.setHeight(H);

        // Quiet default (no live geomag/lightning feed yet) — mirrors the original's idle banner.
        String text = "   Space weather quiet   ●   SFI 168 · SN 142 · A 7 · K 2 · X-ray B1   ●   "
                + "Aurora quiet — no visible activity   ●   No active geomagnetic alerts   ●   "
                + "No lightning activity detected   ●   Gray line: SR 11:02 · SS 22:48   ●   MUF(3000) 28.4 MHz   ●";
        String color = "#5b6675";
        Font font = Font.font("JetBrains Mono", 12.5);
        Text meas = new Text(text); meas.setFont(font);
        double singleW = meas.getLayoutBounds().getWidth();

        double speed = 70, tickMs = 30;
        double[] x = { -1 };
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(tickMs), e -> {
            double w = canvas.getWidth(); if (w <= 0) return;
            if (x[0] < 0) x[0] = w;
            x[0] -= speed * (tickMs / 1000.0);
            if (x[0] <= -singleW) x[0] += singleW;
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(Color.web("#11161d")); g.fillRect(0, 0, w, H);
            g.setStroke(Color.web("#2c3540")); g.setLineWidth(1); g.strokeLine(0, 0, w, 0);
            g.setFont(font); g.setFill(Color.web(color));
            for (double px = x[0]; px < w; px += singleW) g.fillText(text, px, H - 15);
        }));
        tl.setCycleCount(Timeline.INDEFINITE); tl.play();
        return pane;
    }

    // ---- left controls panel ----------------------------------------------
    private static Region leftPanel(java.util.function.IntConsumer onProjection, Runnable onCollapse) {
        VBox p = new VBox(0); p.getStyleClass().add("jm-panel"); p.setMinWidth(270); p.setPrefWidth(270); p.setMaxWidth(270);
        Label title = lbl("MAP CONTROLS", "jm-sec"); title.setStyle("-fx-padding:0 0 0 0;"); HBox.setHgrow(title, Priority.ALWAYS); title.setMaxWidth(Double.MAX_VALUE);
        Label collapse = lbl("‹", "jm-collapse"); collapse.setStyle("-fx-cursor:hand;"); collapse.setOnMouseClicked(e -> onCollapse.run());
        HBox header = new HBox(8, title, collapse); header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(0, 0, 2, 0));
        p.getChildren().add(header);
        // PROJECTION — inlined (not segSel) so the region picker can flip it to Rectangular and keep the highlight in sync.
        p.getChildren().add(lbl("PROJECTION","jm-sec"));
        HBox projSeg = new HBox(2); projSeg.getStyleClass().add("jm-seg");
        String[] projOpts = {"Azimuthal","Rectangular"};
        Label[] projBtns = new Label[2];
        java.util.function.IntConsumer selectProj = sel -> {
            for (int i = 0; i < projBtns.length; i++) { projBtns[i].getStyleClass().remove("on"); if (i == sel) projBtns[i].getStyleClass().add("on"); }
        };
        for (int i = 0; i < projOpts.length; i++) {
            final int idx = i;
            Label b = lbl(projOpts[i], "jm-seg-btn"); if (i == 0) b.getStyleClass().add("on");
            HBox.setHgrow(b, Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE);
            b.setOnMouseClicked(e -> { selectProj.accept(idx); onProjection.accept(idx); });
            projBtns[i] = b; projSeg.getChildren().add(b);
        }
        p.getChildren().add(projSeg);

        // fixed-zoom region picker — single click. Picking a region snaps the blue
        // marble to that window AND switches the projection to Rectangular, so it
        // takes effect immediately even from the azimuthal view.
        p.getChildren().add(lbl("REGION · BLUE MARBLE","jm-sec"));
        FlowPane regions = new FlowPane(6, 6);
        Label[] rchips = new Label[REGION_NAMES.length];
        Runnable paintChips = () -> {
            for (int i = 0; i < rchips.length; i++) {
                boolean on = i == mapRegion;
                rchips[i].setStyle("-fx-cursor:hand; -fx-padding:4 10 4 10; -fx-background-radius:6; -fx-font-size:11px;"
                        + (on ? "-fx-background-color:-ars-map; -fx-text-fill:#06121a; -fx-font-weight:bold;"
                              : "-fx-background-color:-ars-surface-4; -fx-text-fill:-ars-text-2;"));
            }
        };
        for (int i = 0; i < REGION_NAMES.length; i++) {
            final int idx = i;
            Label chip = new Label(REGION_NAMES[i]);
            rchips[i] = chip;
            chip.setOnMouseClicked(e -> { mapRegion = idx; paintChips.run(); selectProj.accept(1); onProjection.accept(1); });
            regions.getChildren().add(chip);
        }
        paintChips.run();
        regions.setPadding(new Insets(2, 0, 0, 0));
        p.getChildren().add(regions);
        // bands
        HBox bandsHead = new HBox(lbl("BANDS","jm-sec")); Region bsp = new Region(); HBox.setHgrow(bsp, Priority.ALWAYS);
        bandsHead.getChildren().addAll(bsp, lbl("ALL","jm-sec-all")); bandsHead.setAlignment(Pos.CENTER_LEFT);
        p.getChildren().add(bandsHead);
        GridPane bands = new GridPane(); bands.setHgap(6); bands.setVgap(6);
        java.util.List<HBox> bandChips = new java.util.ArrayList<>(); java.util.List<String> bandKeys = new java.util.ArrayList<>();
        Runnable restyleBands = () -> {
            boolean any = !spotBands.isEmpty();
            for (int i = 0; i < bandChips.size(); i++) {
                boolean sel = spotBands.contains(bandKeys.get(i));
                bandChips.get(i).setOpacity(!any || sel ? 1.0 : 0.4);
            }
        };
        for (int i=0;i<Mock.MAP_BANDS.length;i++){
            String[] b = Mock.MAP_BANDS[i];
            String key = b[0] + "m";                          // Mock bands are bare ("20"); cluster bands are "20m"
            Region dot = new Region(); dot.getStyleClass().add("jm-band-dot"); dot.setStyle("-fx-background-color:"+b[1]+";");
            HBox chip = new HBox(7, dot, lbl(b[0],"jm-band-n")); chip.getStyleClass().add("jm-band"); chip.setAlignment(Pos.CENTER_LEFT);
            GridPane.setHgrow(chip, Priority.ALWAYS); chip.setMaxWidth(Double.MAX_VALUE); chip.setStyle("-fx-cursor:hand;");
            chip.setOnMouseClicked(e -> { if (!spotBands.remove(key)) spotBands.add(key); restyleBands.run(); refreshSpots(); });
            bandChips.add(chip); bandKeys.add(key);
            bands.add(chip, i%3, i/3);
        }
        for (int c=0;c<3;c++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(100/3.0); bands.getColumnConstraints().add(cc); }
        restyleBands.run();
        p.getChildren().add(bands);
        p.getChildren().add(lbl("MODE","jm-sec"));
        p.getChildren().add(segSel(new String[]{"All","CW","Phone","Digi"}, spotMode, i -> { spotMode = i; refreshSpots(); }));
        Label ageLbl = lbl("SPOT AGE ≤ " + spotAgeMax + " MIN","jm-sec");
        p.getChildren().add(ageLbl);
        p.getChildren().add(sliderI((spotAgeMax - 1) / 59.0, f -> {
            spotAgeMax = (int) Math.max(1, Math.round(1 + f * 59));
            ageLbl.setText("SPOT AGE ≤ " + spotAgeMax + " MIN"); refreshSpots();
        }));
        p.getChildren().add(lbl("OVERLAYS","jm-sec"));
        for (int oi = 0; oi < Mock.OVERLAYS.length; oi++) {
            final int idx = oi;
            Label t = lbl((String) Mock.OVERLAYS[oi][0], "jm-tog-label"); HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE);
            HBox row = new HBox(t, toggleSwitch(overlayFlag(idx), v -> { setOverlayFlag(idx, v); repaintMap(); }));
            row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(7, 0, 7, 0));
            p.getChildren().add(row);
        }
        // NCDXF/IARU International Beacon Project cycle
        p.getChildren().add(lbl("NCDXF BEACONS","jm-sec"));
        Label bt = lbl("Show beacon cycle","jm-tog-label"); HBox.setHgrow(bt, Priority.ALWAYS); bt.setMaxWidth(Double.MAX_VALUE);
        HBox btr = new HBox(bt, toggleSwitch(showBeacons, v -> { showBeacons = v; repaintMap(); }));
        btr.setAlignment(Pos.CENTER_LEFT); btr.setPadding(new Insets(7, 0, 4, 0));
        p.getChildren().add(btr);
        p.getChildren().add(segSel(BEACON_BANDS, beaconBand, i -> { beaconBand = i; repaintMap(); }));

        // RBN "who's hearing me" overlay
        p.getChildren().add(lbl("WHO'S HEARING ME · RBN","jm-sec"));
        Label ht = lbl("Show RBN spots of WM3J","jm-tog-label"); HBox.setHgrow(ht, Priority.ALWAYS); ht.setMaxWidth(Double.MAX_VALUE);
        HBox htr = new HBox(ht, toggleSwitch(showHeardBy, v -> { showHeardBy = v; repaintMap(); }));
        htr.setAlignment(Pos.CENTER_LEFT); htr.setPadding(new Insets(7, 0, 4, 0));
        p.getChildren().add(htr);

        Label beamLbl = lbl("BEAM HEADING · " + String.format("%03d°", (int) beamDeg), "jm-sec");
        p.getChildren().add(beamLbl);
        p.getChildren().add(sliderI(beamDeg / 359.0, frac -> {
            beamDeg = frac * 359; beamLbl.setText("BEAM HEADING · " + String.format("%03d°", (int) beamDeg)); repaintMap();
        }));
        return p;
    }
    /** Interactive slider — click/drag sets the value and reports the fraction. */
    private static Region sliderI(double frac, java.util.function.DoubleConsumer onChange) {
        double[] f = {frac};
        Region track = new Region(); track.setMinHeight(5); track.setMaxHeight(5); track.setStyle("-fx-background-color:-ars-surface-4; -fx-background-radius:99;");
        Region fill = new Region(); fill.setMinHeight(5); fill.setMaxHeight(5); fill.setStyle("-fx-background-color:-ars-map; -fx-background-radius:99;");
        Region knob = new Region(); knob.setMinSize(15, 15); knob.setMaxSize(15, 15); knob.setStyle("-fx-background-color:-ars-map; -fx-background-radius:99; -fx-border-color:-ars-surface-1; -fx-border-width:2; -fx-border-radius:99;");
        Pane bar = new Pane(track, fill, knob); bar.setMinHeight(16); bar.setPrefHeight(16); bar.setStyle("-fx-cursor:hand;");
        Runnable layout = () -> { double w = bar.getWidth(); track.resizeRelocate(0, 6, w, 5); fill.resizeRelocate(0, 6, w * f[0], 5); knob.relocate(Math.max(0, w * f[0] - 7), 1); };
        bar.widthProperty().addListener((o, a, b) -> layout.run());
        java.util.function.DoubleConsumer setX = px -> { double w = bar.getWidth(); if (w <= 0) return; f[0] = Math.max(0, Math.min(1, px / w)); layout.run(); onChange.accept(f[0]); };
        bar.setOnMousePressed(e -> setX.accept(e.getX()));
        bar.setOnMouseDragged(e -> setX.accept(e.getX()));
        VBox.setMargin(bar, new Insets(8, 0, 4, 0));
        return bar;
    }
    private static Region toggleSwitch(boolean initial, java.util.function.Consumer<Boolean> onChange) {
        boolean[] on = {initial};
        Region knob = new Region(); knob.getStyleClass().add("jm-switch-knob");
        StackPane sw = new StackPane(knob); sw.getStyleClass().add("jm-switch"); sw.setMaxSize(36, 20); sw.setPadding(new Insets(0, 3, 0, 3));
        Runnable paint = () -> { sw.getStyleClass().remove("on"); if (on[0]) sw.getStyleClass().add("on"); StackPane.setAlignment(knob, on[0] ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); };
        paint.run(); sw.setStyle("-fx-cursor:hand;");
        sw.setOnMouseClicked(e -> { on[0] = !on[0]; paint.run(); onChange.accept(on[0]); });
        return sw;
    }
    /** Collapsed left column — a thin rail with a re-open control. */
    private static Region leftRail(Runnable onExpand) {
        VBox p = new VBox(0); p.getStyleClass().add("jm-panel"); p.setMinWidth(40); p.setPrefWidth(40); p.setMaxWidth(40);
        p.setAlignment(Pos.TOP_CENTER); p.setPadding(new Insets(16, 0, 16, 0));
        Label expand = lbl("›", "jm-collapse"); expand.setStyle("-fx-cursor:hand;"); expand.setOnMouseClicked(e -> onExpand.run());
        Label vlabel = lbl("CONTROLS", "jm-sec"); vlabel.setRotate(90);
        VBox vbox = new VBox(vlabel); vbox.setAlignment(Pos.CENTER); VBox.setMargin(vbox, new Insets(26, 0, 0, 0)); vbox.setMinWidth(120);
        StackPane vwrap = new StackPane(vbox); vwrap.setMinWidth(36); vwrap.setMaxWidth(36);
        p.getChildren().addAll(expand, vwrap);
        return p;
    }
    /** Interactive segmented control reporting the chosen index. */
    private static Region segSel(String[] opts, int on, java.util.function.IntConsumer onSelect) {
        HBox h = new HBox(2); h.getStyleClass().add("jm-seg");
        Label[] btns = new Label[opts.length];
        for (int i=0;i<opts.length;i++){
            final int idx = i;
            Label b = lbl(opts[i],"jm-seg-btn"); if(i==on) b.getStyleClass().add("on"); HBox.setHgrow(b,Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE);
            b.setOnMouseClicked(e -> { for (Label x : btns) x.getStyleClass().remove("on"); b.getStyleClass().add("on"); onSelect.accept(idx); });
            btns[i] = b; h.getChildren().add(b);
        }
        return h;
    }
    // ---- azimuthal map -----------------------------------------------------
    private static Region aziArea() {
        Canvas cv = new Canvas();
        // top-right legend
        VBox tr = new VBox(2,
            rt("22 spots · ≤"), rt("20m"), rt("rings ="), rt("distance"), rt("spokes ="), rt("bearing"));
        tr.setAlignment(Pos.TOP_RIGHT); tr.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(tr, Pos.TOP_RIGHT); StackPane.setMargin(tr, new Insets(14,18,0,0));
        // bottom-left legend card
        VBox card = new VBox(5, lbl("Azimuthal · centered FN20","jm-legend-b"),
            legRow("#26c0cf","your station  / beam"), legRow("ring","needed DX"), legRow("#eebb58","sunlit (gray line)"));
        card.getStyleClass().add("jm-legend-card");
        card.setMaxSize(220, Region.USE_PREF_SIZE);
        StackPane.setAlignment(card, Pos.BOTTOM_LEFT); StackPane.setMargin(card, new Insets(0,0,20,16));
        StackPane area = new StackPane(cv, tr, card); area.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(area, Priority.ALWAYS);
        area.setMinSize(0, 0);

        Runnable redraw = () -> {
            double aw = area.getWidth(), ah = area.getHeight();
            if (aw <= 1 || ah <= 1) return;
            cv.setWidth(aw); cv.setHeight(ah);
            paintAzimuthal(cv.getGraphicsContext2D(), aw, ah);
        };
        area.widthProperty().addListener((o, a, b) -> redraw.run());
        area.heightProperty().addListener((o, a, b) -> redraw.run());
        area.setOnMouseClicked(e -> handleMapClick(area, e.getX(), e.getY()));
        currentMapRepaint = redraw;
        redraw.run();
        return area;
    }
    private static Label rt(String s){ return lbl(s, "jm-legend"); }
    private static HBox legRow(String color, String text){
        Region d = new Region(); d.setMinSize(9,9); d.setMaxSize(9,9);
        if (color.equals("ring")) d.setStyle("-fx-background-radius:99; -fx-border-color:-ars-warn; -fx-border-width:1.5; -fx-border-radius:99;");
        else d.setStyle("-fx-background-color:"+color+"; -fx-background-radius:99;");
        HBox r = new HBox(8, d, lbl(text,"jm-legend-row")); r.setAlignment(Pos.CENTER_LEFT); return r;
    }

    private static void paintAzimuthal(GraphicsContext g, double w, double h) {
        g.clearRect(0, 0, w, h);
        double cx = w/2, cy = h/2 + 4, R = Math.max(70, Math.min(w, h)/2 - 70);
        Color border = Color.web("#28313d"), faint = Color.web("#28313d"), t3 = Color.web("#6a7684"), t4 = Color.web("#4d5663");

        // light-gray azimuthal world basemap (great-circle map centred on QTH), clipped to the disk.
        // gcm-gray spans 0–180° (edge = antipode); our R = 15000 km ≈ 135°, so the full image
        // disk must be drawn larger (×180/135) and clipped so our outer ring sits at 135°.
        Image base = gcmGray();
        if (ovBasemap && base != null && !base.isError()) {
            g.save();
            g.beginPath(); g.arc(cx, cy, R, R, 0, 360); g.closePath(); g.clip();
            double d = R * 2.667;
            g.setGlobalAlpha(0.55);
            g.drawImage(base, cx - d / 2, cy - d / 2, d, d);
            g.setGlobalAlpha(1.0);
            g.restore();
        }

        // real day/night terminator (gray line) projected into the azimuthal map
        if (ovGrayline) drawAzimuthalGrayLine(g, cx, cy, R);

        // range rings
        if (ovRings) {
            double[] rr = {0.34, 0.67, 1.0}; String[] rl = {"5k km","10k km","15k km"};
            g.setStroke(border); g.setLineWidth(1);
            for (int i=0;i<3;i++){ double r=R*rr[i]; g.strokeOval(cx-r, cy-r, r*2, r*2); }
            g.setFont(Font.font("JetBrains Mono", 9.5)); g.setFill(t4); g.setTextAlign(TextAlignment.LEFT);
            for (int i=0;i<3;i++){ g.fillText(rl[i], cx+4, cy-R*rr[i]+12); }
        }

        // spokes + compass labels
        String[] comp = {"N","30","60","E","120","150","S","210","240","W","300","330"};
        g.setLineWidth(1);
        for (int i=0;i<12;i++){
            double a = Math.toRadians(i*30 - 90);
            g.setStroke(Color.web("#28313d", 0.55));
            g.strokeLine(cx, cy, cx+Math.cos(a)*R, cy+Math.sin(a)*R);
            g.setFill(t3); g.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, 11)); g.setTextAlign(TextAlignment.CENTER);
            g.fillText(comp[i], cx+Math.cos(a)*(R+18), cy+Math.sin(a)*(R+18)+4);
        }

        // beam wedge toward the selected heading
        if (ovBeam) {
            double beam = Math.toRadians(beamDeg - 90), spread = Math.toRadians(11);
            g.setFill(Color.web("#31bdda", 0.16));
            g.beginPath(); g.moveTo(cx, cy);
            g.lineTo(cx+Math.cos(beam-spread)*R, cy+Math.sin(beam-spread)*R);
            g.lineTo(cx+Math.cos(beam+spread)*R, cy+Math.sin(beam+spread)*R);
            g.closePath(); g.fill();
        }

        // live DX-cluster spots
        double spotA = dimSpots() ? 0.28 : 1.0;
        paintedDots.clear();
        for (MapSpot s : liveSpots()){
            if (spotHidden(s.call())) continue;
            double[] pp = azProject(s.lat(), s.lon(), cx, cy, R);
            if (pp[2] > 1.0) continue;                       // off the visible disk
            double x = pp[0], y = pp[1];
            paintedDots.add(new Dot(x, y, s));
            g.setFill(s.color().deriveColor(0,1,1,spotA)); g.fillOval(x-4.5, y-4.5, 9, 9);
            if (ovCallsigns) { g.setFill(Color.web("#e8eef4", spotA)); g.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, 11)); g.setTextAlign(TextAlignment.LEFT); g.fillText(s.call(), x+8, y+4); }
        }
        // the Sun — sub-solar point projected into the azimuthal map (if visible)
        if (ovGrayline) {
            double[] ss = subSolar();
            double[] sp = azProject(ss[0], ss[1], cx, cy, R);
            if (sp[2] <= 1.0) { drawSun(g, sp[0], sp[1]); drawSunLabel(g, sp[0], sp[1]); }
        }

        // me (center)
        g.setFill(Color.web("#31bdda")); g.fillOval(cx-5, cy-5, 10, 10);
        g.setStroke(Color.web("#31bdda", 0.35)); g.setLineWidth(3); g.strokeOval(cx-5, cy-5, 10, 10);
        g.setFill(Color.web("#31bdda")); g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12)); g.fillText(myCall, cx+9, cy+4);

        // NCDXF beacon + "who's hearing me" overlays (azimuthal-equidistant projection)
        Proj azi = (lat, lon) -> { double[] pp = azProject(lat, lon, cx, cy, R); return pp[2] <= 1.0 ? new double[]{pp[0], pp[1]} : null; };
        drawHeardBy(g, azi);
        drawBeacons(g, azi);
    }
    /** Azimuthal-equidistant night shading + terminator (gray line) centred on QTH. */
    private static void drawAzimuthalGrayLine(GraphicsContext g, double cx, double cy, double R) {
        double[] ss = subSolar(); double decl = ss[0], ssLon = ss[1];
        double declR = Math.toRadians(decl), sinDecl = Math.sin(declR), cosDecl = Math.cos(declR), ssLonR = Math.toRadians(ssLon);
        double qLatR = Math.toRadians(Q_LAT), qLonR = Math.toRadians(Q_LON), sinQ = Math.sin(qLatR), cosQ = Math.cos(qLatR);

        // night region — inverse-project each (downsampled) pixel, darken where the sun is below the horizon
        g.save();
        g.beginPath(); g.arc(cx, cy, R, R, 0, 360); g.closePath(); g.clip();
        g.setFill(Color.web("#000000", 0.42));
        int step = 3;
        for (double py = cy - R; py <= cy + R; py += step) {
            for (double px = cx - R; px <= cx + R; px += step) {
                double dx = px - cx, dy = py - cy, rp = Math.sqrt(dx * dx + dy * dy);
                if (rp > R) continue;
                double delta = (rp / R) * 15000.0 / 6371.0;       // angular distance (rad)
                double brg = Math.atan2(dy, dx) + Math.PI / 2;     // bearing from north
                double sinDel = Math.sin(delta), cosDel = Math.cos(delta);
                double latR = Math.asin(sinQ * cosDel + cosQ * sinDel * Math.cos(brg));
                double lonR = qLonR + Math.atan2(Math.sin(brg) * sinDel * cosQ, cosDel - sinQ * Math.sin(latR));
                double dot = Math.sin(latR) * sinDecl + Math.cos(latR) * cosDecl * Math.cos(lonR - ssLonR);
                if (dot < 0) g.fillRect(px, py, step, step);
            }
        }
        g.restore();

        // terminator line — the great circle 90° from the sub-solar point, clipped to the disk
        g.setStroke(Color.web("#eebb58", 0.85)); g.setLineWidth(1.6); g.setLineDashes(6, 5);
        double prevX = 0, prevY = 0; boolean have = false;
        for (int d = 0; d <= 360; d += 3) {
            double[] t = dest(decl, ssLon, d, 90.0 * 111.195);
            double[] p = azProject(t[0], t[1], cx, cy, R);
            if (p[2] <= 1.0) { if (have) g.strokeLine(prevX, prevY, p[0], p[1]); prevX = p[0]; prevY = p[1]; have = true; }
            else have = false;
        }
        g.setLineDashes(null);
    }
    /** Project a geographic point into the azimuthal-equidistant map (centred on QTH). Returns {x,y,distNorm}. */
    private static double[] azProject(double lat, double lon, double cx, double cy, double R) {
        double qLatR = Math.toRadians(Q_LAT), latR = Math.toRadians(lat), dLon = Math.toRadians(lon - Q_LON);
        double cosD = Math.sin(qLatR) * Math.sin(latR) + Math.cos(qLatR) * Math.cos(latR) * Math.cos(dLon);
        double delta = Math.acos(Math.max(-1, Math.min(1, cosD)));
        double distNorm = (Math.toDegrees(delta) * 111.195) / 15000.0;
        double brg = Math.atan2(Math.sin(dLon) * Math.cos(latR),
                Math.cos(qLatR) * Math.sin(latR) - Math.sin(qLatR) * Math.cos(latR) * Math.cos(dLon));
        double a = brg - Math.PI / 2, r = distNorm * R;
        return new double[]{ cx + Math.cos(a) * r, cy + Math.sin(a) * r, distNorm };
    }

    // ---- rectangular (equirectangular) blue-marble map ---------------------
    private static double Q_LAT = 39.7456, Q_LON = -76.96;  // station QTH (from HubConfig)
    private static String myCall = "WM3J";
    private static double qdbl(String s, double def) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; } }

    // Fixed-zoom regions for the blue marble: {minLat, maxLat, minLon, maxLon}.
    // Index 0 is the whole world; the rest snap to a continent-sized window.
    private static final String[] REGION_NAMES = {"World","NA","SA","EU","AF","W.As","E.As","Aus","Ant"};
    private static final double[][] REGIONS = {
        {-90,  90, -180, 180},   // World
        {  7,  72, -168, -52},   // North America
        {-56,  13,  -82, -34},   // South America
        { 34,  71,  -12,  40},   // Europe
        {-36,  38,  -18,  52},   // Africa
        { 12,  50,   26,  82},   // West Asia
        {  5,  55,  100, 150},   // East Asia
        {-47,  -8,  112, 179},   // Australia / Oceania
        {-90, -58, -180, 180},   // Antarctica
    };
    private static int mapRegion = 0;
    // Live equirectangular viewport the rect projection + overlays map into.
    private static double vMinLon = -180, vMaxLon = 180, vMinLat = -90, vMaxLat = 90;
    private static Image marble;
    private static Image marble() {
        if (marble == null) marble = new Image(JMapView.class.getResourceAsStream("/com/ars/fx/map/bluemarble.jpg"));
        return marble;
    }
    private static Image gcmGray;
    private static Image gcmGray() {
        if (gcmGray == null) gcmGray = new Image(JMapView.class.getResourceAsStream("/com/ars/fx/map/gcm-gray.jpg"));
        return gcmGray;
    }
    private static Region rectMap() {
        ImageView iv = new ImageView(marble());
        iv.setPreserveRatio(false); iv.setSmooth(true);
        Canvas cv = new Canvas();

        VBox tr = new VBox(2, rt("22 spots · ≤"), rt("20m"), rt("grid ="), rt("30° lat/lon"), rt("dash ="), rt("great-circle"));
        tr.setAlignment(Pos.TOP_RIGHT); tr.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(tr, Pos.TOP_RIGHT); StackPane.setMargin(tr, new Insets(14, 18, 0, 0));
        VBox card = new VBox(5, lbl("Rectangular · lat / lon", "jm-legend-b"),
                legRow("#26c0cf", "your station  / beam"), legRow("ring", "needed DX"), legRow("#eebb58", "sunlit (gray line)"));
        card.getStyleClass().add("jm-legend-card"); card.setMaxSize(220, Region.USE_PREF_SIZE);
        StackPane.setAlignment(card, Pos.BOTTOM_LEFT); StackPane.setMargin(card, new Insets(0, 0, 20, 16));

        StackPane area = new StackPane(iv, cv, tr, card); area.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(area, Priority.ALWAYS);
        area.setMinSize(0, 0);

        // Size the blue-marble image + overlay to the live area for the currently
        // selected fixed-zoom region, and repaint on every resize. Equirectangular
        // has equal px/degree on both axes, so the display aspect == the window's
        // lon-span : lat-span, and the lat/lon window maps linearly to source pixels.
        Runnable redraw = () -> {
            double aw = area.getWidth(), ah = area.getHeight();
            if (aw <= 1 || ah <= 1) return;
            double[] rg = REGIONS[mapRegion];
            vMinLat = rg[0]; vMaxLat = rg[1]; vMinLon = rg[2]; vMaxLon = rg[3];
            double lonSpan = vMaxLon - vMinLon, latSpan = vMaxLat - vMinLat;
            double aspect = lonSpan / latSpan;
            double imgW = Math.min(aw, ah * aspect), imgH = imgW / aspect;
            Image src = marble();
            double sw = src.getWidth(), sh = src.getHeight();
            iv.setViewport(new Rectangle2D((vMinLon + 180) / 360.0 * sw, (90 - vMaxLat) / 180.0 * sh,
                                           lonSpan / 360.0 * sw, latSpan / 180.0 * sh));
            iv.setFitWidth(imgW); iv.setFitHeight(imgH);
            iv.setVisible(ovBasemap);
            cv.setWidth(aw); cv.setHeight(ah);
            paintRect(cv.getGraphicsContext2D(), aw, ah, imgW, imgH);
        };
        area.widthProperty().addListener((o, a, b) -> redraw.run());
        area.heightProperty().addListener((o, a, b) -> redraw.run());
        area.setOnMouseClicked(e -> handleMapClick(area, e.getX(), e.getY()));
        currentMapRepaint = redraw;
        redraw.run();
        return area;
    }

    /** Draw the equirectangular overlay (graticule, terminator, spots, QTH) for the given area + centred image rect. */
    private static void paintRect(GraphicsContext g, double aw, double ah, double imgW, double imgH) {
        double RMX = (aw - imgW) / 2.0, RMY = (ah - imgH) / 2.0, RW = imgW, RH = imgH;
        g.clearRect(0, 0, aw, ah);
        g.setStroke(Color.web("#28313d")); g.setLineWidth(1); g.strokeRect(RMX, RMY, RW, RH);

        // clip every overlay to the map rect so a zoomed region doesn't spill markers/labels
        g.save();
        g.beginPath(); g.rect(RMX, RMY, RW, RH); g.closePath(); g.clip();

        for (int lon = -150; lon <= 150; lon += 30) {
            double x = lon2x(lon, RMX, RW); boolean prime = lon == 0;
            g.setStroke(Color.web("#28313d", prime ? 0.9 : 0.45)); g.setLineWidth(prime ? 1.1 : 0.6);
            g.strokeLine(x, RMY, x, RMY + RH);
        }
        for (int lat = -60; lat <= 60; lat += 30) {
            double y = lat2y(lat, RMY, RH); boolean eq = lat == 0;
            g.setStroke(Color.web("#28313d", eq ? 0.9 : 0.45)); g.setLineWidth(eq ? 1.1 : 0.6);
            g.strokeLine(RMX, y, RMX + RW, y);
        }
        if (ovGrayline) drawTerminator(g, RMX, RMY, RW, RH);

        g.setFill(Color.web("#cdd6e0", 0.65)); g.setFont(Font.font("IBM Plex Sans", FontWeight.NORMAL, 12)); g.setTextAlign(TextAlignment.CENTER);
        double[][] regions = {{52,13},{39,-98},{-14,-52},{8,20},{55,40},{36,138},{-25,134}};
        String[] rn = {"EUROPE","N. AMERICA","S. AMERICA","AFRICA","RUSSIA","ASIA","OCEANIA"};
        for (int i = 0; i < rn.length; i++) g.fillText(rn[i], lon2x(regions[i][1], RMX, RW), lat2y(regions[i][0], RMY, RH));

        double spotA = dimSpots() ? 0.28 : 1.0;
        paintedDots.clear();
        for (MapSpot s : liveSpots()) {
            if (spotHidden(s.call())) continue;
            double x = lon2x(s.lon(), RMX, RW), y = lat2y(s.lat(), RMY, RH);
            paintedDots.add(new Dot(x, y, s));
            g.setStroke(Color.web("#0b0f14", 0.8 * spotA)); g.setLineWidth(2); g.strokeOval(x - 4.5, y - 4.5, 9, 9);
            g.setFill(s.color().deriveColor(0,1,1,spotA)); g.fillOval(x - 4.5, y - 4.5, 9, 9);
            if (ovCallsigns) { g.setFill(Color.web("#e8eef4", spotA)); g.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, 11)); g.setTextAlign(TextAlignment.LEFT); g.fillText(s.call(), x + 8, y + 4); }
        }

        double qx = lon2x(Q_LON, RMX, RW), qy = lat2y(Q_LAT, RMY, RH);
        g.setStroke(Color.web("#31bdda", 0.5)); g.setLineWidth(1.3); g.strokeOval(qx - 8, qy - 8, 16, 16);
        g.setFill(Color.web("#31bdda")); g.fillOval(qx - 4.5, qy - 4.5, 9, 9);
        g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12)); g.setTextAlign(TextAlignment.CENTER);
        g.fillText(myCall, qx, qy - 13);

        // NCDXF beacon + "who's hearing me" overlays (equirectangular)
        Proj rect = (lat, lon) -> new double[]{lon2x(lon, RMX, RW), lat2y(lat, RMY, RH)};
        drawHeardBy(g, rect);
        drawBeacons(g, rect);
        g.restore();
    }
    private static double lon2x(double lon, double rmx, double rw) { return rmx + ((lon - vMinLon) / (vMaxLon - vMinLon)) * rw; }
    private static double lat2y(double lat, double rmy, double rh) { return rmy + ((vMaxLat - lat) / (vMaxLat - vMinLat)) * rh; }
    /** Great-circle destination point from (lat1,lon1) along bearing for distKm. */
    private static double[] dest(double lat1, double lon1, double brgDeg, double distKm) {
        double R = 6371.0, d = distKm / R;
        double b = Math.toRadians(brgDeg), p1 = Math.toRadians(lat1), l1 = Math.toRadians(lon1);
        double p2 = Math.asin(Math.sin(p1) * Math.cos(d) + Math.cos(p1) * Math.sin(d) * Math.cos(b));
        double l2 = l1 + Math.atan2(Math.sin(b) * Math.sin(d) * Math.cos(p1), Math.cos(d) - Math.sin(p1) * Math.sin(p2));
        double lon = ((Math.toDegrees(l2) + 540) % 360) - 180;
        return new double[]{Math.toDegrees(p2), lon};
    }
    /** Current sub-solar point {declinationDeg, subSolarLonDeg} — the geometry behind the gray line. */
    private static double[] subSolar() {
        java.time.Instant now = java.time.Instant.now();
        double hoursUtc = (now.getEpochSecond() % 86400) / 3600.0;
        double ssLon = ((-15.0 * (hoursUtc - 12.0) + 540) % 360) - 180;
        int doy = java.time.LocalDate.now(java.time.ZoneOffset.UTC).getDayOfYear();
        double decl = 23.44 * Math.sin(Math.toRadians(360.0 / 365.0 * (doy - 81)));
        return new double[]{ decl, ssLon };
    }
    private static void drawTerminator(GraphicsContext g, double rmx, double rmy, double rw, double rh) {
        double[] ss = subSolar(); double decl = ss[0], ssLon = ss[1];
        double td = Math.tan(Math.toRadians(decl == 0 ? 0.001 : decl));

        // terminator polyline points (y clamped to the map rect)
        int n = 0; int N = (360 / 3) + 1;
        double[] xs = new double[N], ys = new double[N];
        for (int lon = -180; lon <= 180; lon += 3) {
            double termLat = Math.toDegrees(Math.atan(-Math.cos(Math.toRadians(lon - ssLon)) / td));
            xs[n] = lon2x(lon, rmx, rw);
            ys[n] = Math.max(rmy, Math.min(rmy + rh, lat2y(termLat, rmy, rh)));
            n++;
        }
        // Darken the NIGHT side (inside the gray line). Sun north (decl≥0) → night is
        // the southern hemisphere (below the curve); else the northern.
        boolean nightBelow = decl >= 0;
        g.setFill(Color.web("#04060a", 0.5));
        g.beginPath(); g.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) g.lineTo(xs[i], ys[i]);
        if (nightBelow) { g.lineTo(rmx + rw, rmy + rh); g.lineTo(rmx, rmy + rh); }
        else            { g.lineTo(rmx + rw, rmy);      g.lineTo(rmx, rmy); }
        g.closePath(); g.fill();

        // the gray line itself
        g.setStroke(Color.web("#eebb58", 0.85)); g.setLineWidth(1.6); g.setLineDashes(6, 5);
        g.beginPath(); g.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) g.lineTo(xs[i], ys[i]);
        g.stroke(); g.setLineDashes(null);

        // the Sun — its sub-solar point (directly overhead: lat = declination, lon = sub-solar)
        double sx = lon2x(ssLon, rmx, rw), sy = lat2y(decl, rmy, rh);
        drawSun(g, sx, sy); drawSunLabel(g, sx, sy);
    }
    /** Sun's azimuth/elevation as seen from the QTH (degrees), from the sub-solar point. */
    private static double[] solarAzEl() {
        double[] ss = subSolar();
        double decl = Math.toRadians(ss[0]), ssLon = ss[1];
        double lat = Math.toRadians(Q_LAT), dlon = Math.toRadians(ssLon - Q_LON);
        double cosC = Math.sin(lat) * Math.sin(decl) + Math.cos(lat) * Math.cos(decl) * Math.cos(dlon);
        double el = 90 - Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosC))));
        double y = Math.sin(dlon) * Math.cos(decl);
        double x = Math.cos(lat) * Math.sin(decl) - Math.sin(lat) * Math.cos(decl) * Math.cos(dlon);
        double az = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
        return new double[]{az, el};
    }
    /** Sunrise/sunset at the QTH today as {srUtcHours, ssUtcHours}; NaN if sun never rises/sets. */
    private static double[] solarRiseSet() {
        double decl = Math.toRadians(subSolar()[0]), lat = Math.toRadians(Q_LAT);
        double cosH = -Math.tan(lat) * Math.tan(decl);
        if (cosH < -1) return new double[]{Double.NaN, Double.NaN, 1};   // sun up all day
        if (cosH > 1)  return new double[]{Double.NaN, Double.NaN, -1};  // sun down all day
        double H = Math.toDegrees(Math.acos(cosH));     // half-day arc, degrees
        // solar noon in UTC at this longitude (when the sub-solar point crosses Q_LON)
        double noonUtc = 12.0 - Q_LON / 15.0;
        double sr = ((noonUtc - H / 15.0) % 24 + 24) % 24;
        double sUtc = ((noonUtc + H / 15.0) % 24 + 24) % 24;
        return new double[]{sr, sUtc, 0};
    }
    private static String hhmm(double h) {
        int m = (int) Math.round(h * 60) % 1440; if (m < 0) m += 1440;
        return String.format("%02d:%02d", m / 60, m % 60);
    }
    /** Solar-position readout beside the sun glyph: sub-solar lat/lon + az/el + SR/SS from QTH. */
    private static void drawSunLabel(GraphicsContext g, double x, double y) {
        double[] ss = subSolar(); double decl = ss[0], ssLon = ss[1];
        double[] ae = solarAzEl();
        double[] rs = solarRiseSet();
        String pos = String.format("☉ %.1f°%s %.1f°%s",
                Math.abs(decl), decl >= 0 ? "N" : "S", Math.abs(ssLon), ssLon >= 0 ? "E" : "W");
        String azel = String.format("az %03.0f°  el %+.0f°", ae[0], ae[1]);
        String srss = Double.isNaN(rs[0])
                ? (rs[2] > 0 ? "sun up all day" : "sun down all day")
                : String.format("SR %s  SS %s Z", hhmm(rs[0]), hhmm(rs[1]));
        g.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, 10.5));
        g.setTextAlign(TextAlignment.LEFT);
        g.setFill(Color.web("#05080c", 0.55)); g.fillText(pos, x + 16.5, y + 5.5); g.fillText(azel, x + 16.5, y + 18.5); g.fillText(srss, x + 16.5, y + 31.5);
        g.setFill(Color.web("#ffd84a", 0.92)); g.fillText(pos, x + 16, y + 5);
        g.setFill(Color.web("#eebb58", 0.88)); g.fillText(azel, x + 16, y + 18);
        g.setFill(Color.web("#d7c089", 0.85)); g.fillText(srss, x + 16, y + 31);
    }
    /** A small sun glyph (glow + disk + rays) at (x,y). */
    private static void drawSun(GraphicsContext g, double x, double y) {
        g.setFill(Color.web("#ffd33d", 0.16)); g.fillOval(x - 17, y - 17, 34, 34);
        g.setFill(Color.web("#ffe27a", 0.28)); g.fillOval(x - 10, y - 10, 20, 20);
        g.setStroke(Color.web("#ffd84a", 0.9)); g.setLineWidth(1.4);
        for (int a = 0; a < 8; a++) {
            double ang = Math.toRadians(a * 45);
            g.strokeLine(x + Math.cos(ang) * 8.5, y + Math.sin(ang) * 8.5, x + Math.cos(ang) * 12.5, y + Math.sin(ang) * 12.5);
        }
        g.setFill(Color.web("#fff3b0")); g.fillOval(x - 6, y - 6, 12, 12);
        g.setStroke(Color.web("#ffcf3a")); g.setLineWidth(1.3); g.strokeOval(x - 6, y - 6, 12, 12);
    }

    // ---- right rail --------------------------------------------------------
    /** Rebuild the live DX-spots list (cluster + filters). */
    private static void fillSpots(VBox spots) {
        spots.getChildren().clear();
        java.util.List<MapSpot> list = liveSpots();
        if (list.isEmpty()) {
            com.ars.fx.data.ClusterClient ccc = com.ars.fx.data.ClusterClient.getInstance();
            Label e = lbl(ccc.isConnected() ? "No spots match the filters." : ccc.wantConnected() ? "Connecting to cluster…" : "Cluster off — Connect in J-Hub", "jm-spot-meta");
            e.setPadding(new Insets(10, 2, 10, 2)); spots.getChildren().add(e); return;
        }
        int n = 0;
        for (MapSpot s : list) {
            if (n++ >= 40) break;
            Region dot = new Region(); dot.getStyleClass().add("jm-spot-dot"); dot.setStyle("-fx-background-color:" + toHex(s.color()) + ";");
            Label fq = lbl(com.ars.fx.data.ClusterClient.fmtFreq(s.freqHz()), "jm-spot-fq"); fq.setMinWidth(54);
            String need = workedCalls().contains(s.call().toUpperCase()) ? "n" : "y";
            VBox txt = new VBox(1, hcall(s.call(), need),
                    lbl(s.band() + " · " + s.mode() + " · " + com.ars.fx.data.ClusterClient.age(s.arrivalMs()), "jm-spot-meta")); HBox.setHgrow(txt, Priority.ALWAYS);
            HBox row = new HBox(11, dot, fq, txt); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("jm-spot-row");
            row.setStyle("-fx-cursor:hand;");
            row.setOnMouseClicked(e -> SpotAction.onSpot(row, s.call(), s.freqHz(), s.mode(), s.band(), s.lat(), s.lon()));
            spots.getChildren().add(row);
        }
    }
    private static Node[] railDrawers(VBox spots) {
        ScrollPane spotScroll = new ScrollPane(spots); spotScroll.setFitToWidth(true);
        spotScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); spotScroll.setPrefViewportHeight(300);
        spotScroll.setStyle("-fx-background-color:transparent;");

        List<Node> inst = Shell.instruments(50);
        List<Node> rail = new ArrayList<>();
        rail.add(Shell.drawer("DX Spots","map","map", liveSpots().size() + " spots", true, spotScroll));
        rail.add(Shell.drawer("DX station info","accent","hub","", false, lbl("Select a spot to see DXCC, beam heading & distance.","jm-spot-meta")));
        rail.add(inst.get(0));   // Antenna · Rotor
        rail.add(inst.get(1));   // Propagation
        rail.add(inst.get(2));   // Solar & space weather (shared)
        rail.add(inst.get(3));   // Band conditions (shared)
        return rail.toArray(new Node[0]);
    }
    private static Node hcall(String call, String need){
        HBox h = new HBox(7, lbl(call, "jm-spot-call")); h.setAlignment(Pos.CENTER_LEFT);
        if (need.equals("y")) h.getChildren().add(lbl("NEED","jhub-need"));
        return h;
    }
}
