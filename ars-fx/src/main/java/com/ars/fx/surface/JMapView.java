package com.ars.fx.surface;

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
    /** The one frequency each band's beacons share (kHz as MHz text); the 18 stations take turns on it, so a rig tuned here hears them all. */
    private static final long[] BEACON_HZ = {14_100_000L, 18_110_000L, 21_150_000L, 24_930_000L, 28_200_000L};
    private static boolean showBeacons = false;
    private static int beaconBand = 0;
    private static Label beaconInfo;                         // live "QSY to ..." line in the drawer
    private static HBox beaconSeg;                           // the band buttons, so a hopping watch can move the highlight
    private static Label beaconHeardInfo;                    // "7 of 18 heard on 20m"
    private record BeaconDot(double x, double y, String call) {}
    private static final java.util.List<BeaconDot> paintedBeacons = new java.util.ArrayList<>();
    private static boolean beaconHop() { return com.ars.fx.data.HubConfig.getBool("map.beaconHop", false); }
    private static boolean beaconAuto() { return com.ars.fx.data.HubConfig.getBool("map.beaconAuto", false); }
    private static void selectBeaconSeg(int idx) {
        if (beaconSeg == null) return;
        for (int i = 0; i < beaconSeg.getChildren().size(); i++) {
            Node n = beaconSeg.getChildren().get(i);
            if (i == idx) { if (!n.getStyleClass().contains("on")) n.getStyleClass().add("on"); } else n.getStyleClass().remove("on");
        }
    }
    private static void refreshBeaconHeard() {
        if (beaconHeardInfo != null) beaconHeardInfo.setText(com.ars.fx.data.BeaconHeard.summary(BEACON_BANDS[beaconBand], BEACONS.length)
                + "   \u00b7   " + com.ars.fx.data.BeaconHeard.allBands(java.util.Arrays.asList(BEACON_BANDS), BEACONS.length));
    }
    /** Once a second while the cycle is on: hop to the next band every cycle if asked, and mark the beacon on the air if the S-meter holds a signal on its frequency. */
    private static void beaconWatch() {
        long now = java.time.Instant.now().getEpochSecond();
        if (beaconHop()) {
            int b = com.ars.fx.data.BeaconHeard.bandAt(now, BEACON_BANDS.length);
            if (b != beaconBand) {
                beaconBand = b; selectBeaconSeg(b);
                if (beaconInfo != null) beaconInfo.setText(beaconLine());
                qsyToBeacon(); repaintMap(); refreshBeaconHeard();
            }
        }
        if (beaconAuto()) {
            com.ars.fx.data.RigClient.State st = com.ars.fx.data.RigClient.getInstance().last();
            boolean onFreq = st != null && st.connected() && Math.abs(st.freqHz() - BEACON_HZ[beaconBand]) <= 2000 && st.mode() != null && st.mode().toUpperCase().startsWith("CW");
            if (onFreq && com.ars.fx.data.BeaconHeard.sample(BEACONS[activeBeacon(beaconBand)][0], BEACON_BANDS[beaconBand], st.sMeterDb(), (int) (now % 10), now)) {
                repaintMap(); refreshBeaconHeard();
            }
        }
    }
    private static Runnable currentMapRepaint;
    private static javafx.animation.Timeline beaconTicker;
    /** Beacon index transmitting on band b right now (slot = UTC%180 /10; band offsets by one slot each). */
    private static int activeBeacon(int band) {
        int slot = (int) ((java.time.Instant.now().getEpochSecond() % 180) / 10);
        return ((slot - band) % 18 + 18) % 18;
    }
    private static String mhz(long hz) { return String.format(java.util.Locale.ROOT, "%.3f", hz / 1e6); }

    /** What to tell the operator: where to tune, who is on now and who is next. */
    private static String beaconLine() {
        long now = java.time.Instant.now().getEpochSecond();
        int a = activeBeacon(beaconBand);
        // the station on this band moves one place round the list every 10 s
        int nxt = activeBeaconAt(beaconBand, now + 10);
        return "QSY to " + mhz(BEACON_HZ[beaconBand]) + " MHz CW  \u00b7  now " + BEACONS[a][0] + "  \u00b7  next " + BEACONS[nxt][0] + " in " + (10 - now % 10) + " s";
    }
    private static int activeBeaconAt(int band, long epochSec) {
        int slot = (int) ((epochSec % 180) / 10);
        return ((slot - band) % 18 + 18) % 18;
    }

    /** Auto QSY: put the rig on the selected band's beacon frequency in CW. Does nothing without a connected rig, and never while it transmits. */
    private static void qsyToBeacon() {
        if (!showBeacons || !com.ars.fx.data.HubConfig.getBool("map.beaconQsy", false)) return;
        com.ars.fx.data.RigClient rig = com.ars.fx.data.RigClient.getInstance();
        com.ars.fx.data.RigClient.State st = rig.last();
        if (!st.connected() || st.ptt()) return;
        rig.setMode("CW");
        rig.setFreqHz(BEACON_HZ[beaconBand]);
    }

    private static void repaintMap() { paintedBeam = Math.round(beamDeg()); if (currentMapRepaint != null) currentMapRepaint.run(); }
    /** Dim the regular DX spots while a propagation overlay (beacons / who's-hearing-me) is active. */
    private static boolean dimSpots() { return showBeacons || showHeardBy; }
    /** Draw each shown cluster spot's path, from the station spotted to the station that spotted it. */
    private static java.util.List<MapSpot> frameSpots;               // the spots the globe is painting this frame, so the layers share one lookup
    private static boolean showSpotPaths = false;
    private static long spotPathsSince = 0;                         // Clear sets this to now: only spots that arrive after it get a line
    private static final long SPOT_PATH_LIFE_MS = 10 * 60_000L;     // a spot's line fades out over ten minutes, then goes

    /** The antenna's heading, or NaN with no rotor: the map never shows a beam the antenna isn't on. */
    private static double beamDeg() { return com.ars.fx.data.RotorClient.getInstance().headingDeg(); }
    private static String beamText() { double b = beamDeg(); return Double.isNaN(b) ? "—" : String.format("%03.0f°", b); }
    private static long paintedBeam = Long.MIN_VALUE;     // what the map last drew, so a turning rotor repaints it

    // ── clickable map dots: each paint records the on-screen spot positions so a
    //    click can be hit-tested back to the spot that was drawn there. ──────────
    private record Dot(double x, double y, MapSpot s) {}
    private static final java.util.List<Dot> paintedDots = new java.util.ArrayList<>();
    private static void handleMapClick(Node source, double x, double y) {
        Dot best = null; double bestD = Double.MAX_VALUE;
        for (Dot d : paintedDots) { double dd = Math.hypot(d.x() - x, d.y() - y); if (dd < bestD) { bestD = dd; best = d; } }
        if (showBeacons) {
            BeaconDot nb = null; double nd = Double.MAX_VALUE;
            for (BeaconDot b : paintedBeacons) { double dd = Math.hypot(b.x() - x, b.y() - y); if (dd < nd) { nd = dd; nb = b; } }
            if (nb != null && nd <= 10 && (best == null || nd <= bestD)) {
                com.ars.fx.data.BeaconHeard.toggle(nb.call(), BEACON_BANDS[beaconBand], java.time.Instant.now().getEpochSecond());
                refreshBeaconHeard(); repaintMap();
                return;
            }
        }
        if (best != null && bestD <= 11) {
            selectSpot(source, best.s());
        } else { selectSpotCall(null); DxInfoCard.hide(); }          // a click on empty map lets go of the selected spot and puts its card away
    }
    /** A spot was clicked, on the map or in the list: show who it is, then do the shared spot action. */
    private static void selectSpot(Node source, MapSpot s) {
        selectSpotCall(s.call());
        DxInfoCard.show(source, s, workedCalls().contains(s.call().toUpperCase()));
        SpotAction.onSpot(source, s.call(), s.freqHz(), s.mode(), s.band(), s.lat(), s.lon());
    }

    // ── overlay toggles (left panel) ──────────────────────────────────────────
    private static boolean ovGrayline = true, ovBasemap = true, ovBeam = true, ovRings = true, ovCallsigns = true, ovNeededOnly = false, ovBurst = true, ovFade = true;
    // J-Map carries a 270px controls panel on top of the dock+rail, so a narrow / high-DPI window
    // pushes the side edges off screen. Below this logical width auto-collapse the controls to the
    // icon strip (same idea as J-Sat's satellite list).
    private static final double NARROW_W = 1240;
    private static boolean narrow() { return mapFrame != null && mapFrame.getWidth() > 1 && mapFrame.getWidth() < NARROW_W; }
    private static boolean overlayFlag(int i) {
        return switch (i) { case 0 -> ovGrayline; case 1 -> ovBasemap; case 2 -> ovBeam; case 3 -> ovRings; case 4 -> ovCallsigns; case 5 -> ovNeededOnly; case 6 -> ovBurst; default -> ovFade; };
    }
    private static void applyOverlay(int i, boolean v) {
        switch (i) { case 0 -> ovGrayline = v; case 1 -> ovBasemap = v; case 2 -> ovBeam = v; case 3 -> ovRings = v; case 4 -> ovCallsigns = v; case 5 -> ovNeededOnly = v; case 6 -> ovBurst = v; default -> ovFade = v; }
    }
    private static void setOverlayFlag(int i, boolean v) {
        applyOverlay(i, v);
        com.ars.fx.data.HubConfig.setBool("map.ov." + i, v);            // kept for next time
    }
    static { for (int i = 0; i < 8; i++) applyOverlay(i, com.ars.fx.data.HubConfig.getBool("map.ov." + i, overlayFlag(i))); }
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
    private static int spotMode = Math.max(0, Math.min(3, com.ars.fx.data.HubConfig.getInt("map.spotMode", 0)));   // 0 All · 1 CW · 2 Phone · 3 Digi
    private static final java.util.Set<String> spotBands = new java.util.HashSet<>();   // empty = all bands
    static { for (String b : com.ars.fx.data.HubConfig.get("map.bands", "").split(",")) if (!b.isBlank()) spotBands.add(b.trim()); }
    private static int spotAgeMax = Math.max(1, Math.min(60, com.ars.fx.data.HubConfig.getInt("map.spotAge", 20)));   // minutes
    private static Runnable currentSpotsRefresh;

    /**
     * A spot's dot: a soft glow in its band's colour, fading as the spot ages so what is current stands out, and a ring on the very newest. With Path now
     * on, a band our estimate says is probably closed fades further, and one it says is likely open gets a green ring.
     */
    private static void spotDot(javafx.scene.canvas.GraphicsContext g, MapSpot s, double x, double y, double spotA) {
        com.ars.fx.data.Propagation.Verdict v = com.ars.fx.data.PathNow.shown() ? com.ars.fx.data.PathNow.forPoint(s.lat(), s.lon(), s.freqHz()) : null;
        double a = v == com.ars.fx.data.Propagation.Verdict.CLOSED ? spotA * 0.4 : spotA;
        double ageMin = Math.max(0, (System.currentTimeMillis() - s.arrivalMs()) / 60000.0);
        a *= 1.0 - 0.45 * Math.min(1.0, ageMin / Math.max(1, spotAgeMax));              // older spots recede
        Color c = s.color();
        g.setFill(new javafx.scene.paint.RadialGradient(0, 0, x, y, 13, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, c.deriveColor(0, 1, 1, 0.55 * a)), new javafx.scene.paint.Stop(1, c.deriveColor(0, 1, 1, 0))));
        g.fillOval(x - 13, y - 13, 26, 26);
        g.setFill(c.deriveColor(0, 1, 1, a)); g.fillOval(x - 4.5, y - 4.5, 9, 9);
        if (ageMin < 1.5) { g.setStroke(c.deriveColor(0, 1, 1, 0.85 * a)); g.setLineWidth(1.3); g.strokeOval(x - 8, y - 8, 16, 16); }   // heard in the last minute and a half
        if (v == com.ars.fx.data.Propagation.Verdict.OPEN) {
            g.setStroke(Color.web("#4ade80", 0.95 * spotA)); g.setLineWidth(1.6); g.strokeOval(x - 6.5, y - 6.5, 13, 13);
        }
    }

    /**
     * The callsigns, drawn after every dot as small pills so they stay readable over land, sea and each other. The selected spot goes first, then the
     * newest; each takes the first of four places round its dot (right, left, above, below) that is not on a label already placed or on the station's
     * own call. If none is free the label is left out and the dot keeps its glow, so a crowd of spots reads as dots with a few clear names, not a smear.
     * {@code meX} is NaN when the station is off the map.
     */
    private static void drawSpotLabels(GraphicsContext g, double spotA, double w, double h, double meX, double meY) {
        labelsFading = false;
        if (paintedDots.isEmpty()) return;
        List<Dot> order = new ArrayList<>(paintedDots);
        order.sort((p, q) -> {
            boolean ps = p.s().call().equalsIgnoreCase(selectedSpotCall), qs = q.s().call().equalsIgnoreCase(selectedSpotCall);
            if (ps != qs) return ps ? -1 : 1;
            return Long.compare(q.s().arrivalMs(), p.s().arrivalMs());
        });
        Font plain = Font.font("JetBrains Mono", FontWeight.NORMAL, 11), bold = Font.font("JetBrains Mono", FontWeight.BOLD, 11);
        List<double[]> placed = new ArrayList<>();                                          // {x, y, width, height}
        if (!Double.isNaN(meX)) placed.add(new double[]{meX - 10, meY - 16, 14 + 8 * Math.max(3, myCall.length()), 26});
        g.setTextAlign(TextAlignment.LEFT);
        long now = System.currentTimeMillis();
        double life = com.ars.fx.data.LabelFade.lifeSec(order.size(), spotAgeMax);           // the busier the map, the sooner a name goes
        boolean anyFading = false;
        for (Dot d : order) {
            MapSpot s = d.s();
            boolean sel = s.call().equalsIgnoreCase(selectedSpotCall);
            double fade = (!ovFade || sel) ? 1.0 : com.ars.fx.data.LabelFade.alpha((now - s.arrivalMs()) / 1000.0, life);
            if (fade < 0.06) continue;                                                      // gone: it no longer takes up room either
            if (fade < 1.0) anyFading = true;
            Font f = sel ? bold : plain;
            double pw = textWidth(s.call(), f) + 10, ph = 16;
            double[][] tries = {{d.x() + 9, d.y() - ph / 2}, {d.x() - 9 - pw, d.y() - ph / 2}, {d.x() - pw / 2, d.y() - ph - 8}, {d.x() - pw / 2, d.y() + 8}};
            double[] at = null;
            for (double[] c : tries) {
                if (c[0] < 2 || c[1] < 2 || c[0] + pw > w - 2 || c[1] + ph > h - 2) continue;
                boolean clash = false;
                for (double[] r : placed) if (c[0] < r[0] + r[2] && c[0] + pw > r[0] && c[1] < r[1] + r[3] && c[1] + ph > r[1]) { clash = true; break; }
                if (!clash) { at = c; break; }
            }
            if (at == null) { if (!sel) continue; at = tries[0]; }                          // the selected spot always gets its name
            placed.add(new double[]{at[0], at[1], pw, ph});
            double la = spotA * fade;
            g.setFill(Color.web("#0b0f14", (sel ? 0.86 : 0.66) * la));
            g.fillRoundRect(at[0], at[1], pw, ph, 7, 7);
            g.setStroke(sel ? Color.web("#ffffff", 0.85 * la) : s.color().deriveColor(0, 1, 1, 0.55 * la)); g.setLineWidth(sel ? 1.4 : 1.0);
            g.strokeRoundRect(at[0], at[1], pw, ph, 7, 7);
            g.setFill(Color.web("#e8eef4", la)); g.setFont(f);
            g.fillText(s.call(), at[0] + 5, at[1] + ph - 4.5);
        }
        labelsFading = anyFading;
    }
    /** Set by the last paint: some callsign is part-way through fading, so the map needs repainting now and then for the fade to carry on. */
    private static boolean labelsFading = false;
    private static final Text MEASURE = new Text();
    private static double textWidth(String str, Font f) { MEASURE.setFont(f); MEASURE.setText(str); return MEASURE.getLayoutBounds().getWidth(); }

    /** A positioned cluster spot (geo-located via {@link com.ars.fx.data.Geo}). */
    public record MapSpot(String call, double lat, double lon, String band, String mode, long arrivalMs, long freqHz, Color color,
                          String spotter, String comment) {}

    /** Geo-locate + filter the given raw cluster spots (skips calls with no known location). */
    public static java.util.List<MapSpot> spotsFrom(java.util.List<com.ars.fx.data.ClusterClient.Spot> raw) {
        java.util.List<MapSpot> out = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();
        for (com.ars.fx.data.ClusterClient.Spot s : raw) {
            double[] ll = com.ars.fx.data.Geo.of(s.call());
            if (ll == null) continue;
            if (!modePass(s.mode()) || !bandPass(s.band()) || (now - s.arrivalMs()) / 60000 > spotAgeMax) continue;
            out.add(new MapSpot(s.call(), ll[0], ll[1], s.band(), s.mode(), s.arrivalMs(), s.freqHz(), bandColor(s.band()), s.de(), s.comment()));
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
        // 1 Hz tick keeps the top bar live. The map itself is costly to repaint and barely changes: beacons step every
        // 10 s, the sun about a quarter of a degree a minute. New spots repaint it on their own.
        beaconTicker = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
            updateMapTopbar();                                 // ticking clock + live SPOTS/SHOWN/GRAY-LINE/BEAM
            if (beaconInfo != null) beaconInfo.setText(showBeacons ? beaconLine() : "");
            if (showBeacons) beaconWatch();
            long second = java.time.Instant.now().getEpochSecond();
            boolean beamMoved = Math.round(beamDeg()) != paintedBeam;
            if (beamMoved || (showSpotPaths && second % 15 == 0) || (showBeacons ? second % 10 == 0 : ovGrayline && second % 60 == 0) || (labelsFading && ovCallsigns && ovFade && second % 10 == 0)) repaintMap();
        }));
        beaconTicker.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        beaconTicker.play();
    }
    /** Projects (lat,lon) to canvas {x,y}, or null when off-map (azimuthal beyond the disk). */
    private interface Proj { double[] at(double lat, double lon); }
    private static void drawBeacons(GraphicsContext g, Proj proj) {
        if (!showBeacons) return;
        paintedBeacons.clear();
        int active = activeBeacon(beaconBand);
        g.setFill(Color.web("#0b0f14", 0.78)); g.fillRoundRect(12, 12, 360, 26, 8, 8);
        g.setFill(Color.web("#ffd84a")); g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 13)); g.setTextAlign(TextAlignment.LEFT);
        g.fillText("QSY " + mhz(BEACON_HZ[beaconBand]) + " MHz CW  \u00b7  " + BEACONS[active][0], 22, 30);
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
            paintedBeacons.add(new BeaconDot(x, y, BEACONS[i][0]));
            if (com.ars.fx.data.BeaconHeard.isHeard(BEACONS[i][0], BEACON_BANDS[beaconBand])) {
                g.setStroke(Color.web("#4ade80", 0.95)); g.setLineWidth(2); g.strokeOval(x - 8, y - 8, 16, 16);
            }
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

    // ── Ham-to-ham tracks: the great-circle path between two stations that are not this one ──
    /** Draws a path between two places for the map's track list, with a dot and the name at each end and the distance and bearing at the middle. */
    public static void showTrack(String from, String to) { com.ars.fx.data.MapTracks.add(from, to); repaintMap(); }

    private static void drawTracks(GraphicsContext g, Proj proj) {
        for (com.ars.fx.data.MapTracks.Track t : com.ars.fx.data.MapTracks.all()) {
            Color col = Color.web("#ff5a5a");
            drawGcPath(g, proj, t.lat1(), t.lon1(), t.lat2(), t.lon2(), Color.web("#ff5a5a", 0.9));
            g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 10)); g.setTextAlign(TextAlignment.LEFT);
            double[][] ends = {{t.lat1(), t.lon1()}, {t.lat2(), t.lon2()}};
            String[] names = {t.from(), t.to()};
            for (int i = 0; i < 2; i++) {
                double[] p = proj.at(ends[i][0], ends[i][1]);
                if (p == null) continue;
                g.setStroke(Color.web("#0b0f14", 0.85)); g.setLineWidth(2); g.strokeOval(p[0] - 4.5, p[1] - 4.5, 9, 9);
                g.setFill(col); g.fillOval(p[0] - 4.5, p[1] - 4.5, 9, 9);
                g.setFill(Color.web("#ffe3e3")); g.fillText(names[i], p[0] + 8, p[1] - 6);
            }
            double[] mid = gcPoint(t.lat1(), t.lon1(), t.lat2(), t.lon2(), 0.5);
            double[] mp;
            if (gcGlobe != null) {                                     // on the globe the label rides the top of the arch
                com.ars.fx.data.GlobeProjection.Pt pk = gcGlobe.project(mid[0], mid[1], gcLiftKm(t.lat1(), t.lon1(), t.lat2(), t.lon2()));
                mp = pk.visible() ? new double[]{pk.x(), pk.y()} : null;
            } else mp = proj.at(mid[0], mid[1]);
            if (mp != null) {
                String s = t.shortLabel();
                g.setFill(Color.web("#0b0f14", 0.78)); g.fillRoundRect(mp[0] - 4, mp[1] - 13, s.length() * 6.2 + 8, 16, 6, 6);
                g.setFill(Color.web("#ffc9c9")); g.fillText(s, mp[0], mp[1] - 1);
            }
        }
    }

    private static final java.util.Map<String, double[]> SPOTTER_AT = new java.util.HashMap<>();   // the same few skimmers spot all day; the lookup scans a prefix table
    private static final double[] UNKNOWN = new double[0];
    private static double[] spotterAt(String call) {
        if (call == null) return null;
        if (SPOTTER_AT.size() > 4000) SPOTTER_AT.clear();
        double[] ll = SPOTTER_AT.computeIfAbsent(call, c -> { double[] g = com.ars.fx.data.Geo.of(c); return g == null ? UNKNOWN : g; });
        return ll == UNKNOWN ? null : ll;
    }

    /** The spot last clicked, by call: all its paths, one for each station that spotted it, are drawn in red on top of the rest. */
    private static String selectedSpotCall;
    private static void selectSpotCall(String call) {
        String c = call == null ? null : call.toUpperCase();
        if (java.util.Objects.equals(c, selectedSpotCall)) return;
        selectedSpotCall = c; repaintMap();
    }

    /**
     * One thin line per shown spot, spotted station to spotter, in the band's colour, with a small dot at the spotter's end. The clicked spot's lines are
     * red, heavier and on top, whether or not the Spot paths switch is on.
     */
    private static void drawSpotPaths(GraphicsContext g, Proj proj) {
        if (!showSpotPaths && selectedSpotCall == null) return;
        long now = System.currentTimeMillis();
        java.util.List<MapSpot> spots = frameSpots != null ? frameSpots : liveSpots();
        if (showSpotPaths) for (MapSpot s : spots) {
            if (spotHidden(s.call()) || s.arrivalMs() < spotPathsSince || s.call().equalsIgnoreCase(selectedSpotCall)) continue;
            double age = (now - s.arrivalMs()) / (double) SPOT_PATH_LIFE_MS;      // 0 new, 1 gone
            if (age >= 1) continue;
            double fade = 1 - age * 0.9;                                            // full strength to 10 % just before it expires
            double[] ll = spotterAt(s.spotter());
            if (ll == null || (Math.abs(ll[0] - s.lat()) < 0.01 && Math.abs(ll[1] - s.lon()) < 0.01)) continue;
            Color col = s.color();
            drawGcPath(g, proj, s.lat(), s.lon(), ll[0], ll[1], col.deriveColor(0, 1, 1, 0.6 * fade));
            double[] p = proj.at(ll[0], ll[1]);
            if (p == null) continue;
            g.setFill(col.deriveColor(0, 1, 1, 0.9 * fade)); g.fillOval(p[0] - 2.5, p[1] - 2.5, 5, 5);
        }
        if (selectedSpotCall == null) return;
        Color red = Color.web("#ff5a5a");
        gcWidth = 2.2;
        try {
            for (MapSpot s : spots) {
                if (!s.call().equalsIgnoreCase(selectedSpotCall) || spotHidden(s.call())) continue;
                double[] ll = spotterAt(s.spotter());
                if (ll == null || (Math.abs(ll[0] - s.lat()) < 0.01 && Math.abs(ll[1] - s.lon()) < 0.01)) continue;
                drawGcPath(g, proj, s.lat(), s.lon(), ll[0], ll[1], red);
                double[] p = proj.at(ll[0], ll[1]);
                if (p == null) continue;
                g.setStroke(Color.web("#0b0f14", 0.85)); g.setLineWidth(1.5); g.strokeOval(p[0] - 4, p[1] - 4, 8, 8);
                g.setFill(red); g.fillOval(p[0] - 4, p[1] - 4, 8, 8);
            }
        } finally { gcWidth = 1.4; }
    }

    // ── "Who's hearing me" — RBN skimmers that spotted our call ───────────────
    private static boolean showHeardBy = false;
    private static void drawHeardBy(GraphicsContext g, Proj proj) {
        if (!showHeardBy) return;
        java.util.List<com.ars.fx.data.HeardByClient.HeardSpot> spots = com.ars.fx.data.HeardByClient.getInstance().spots();
        for (com.ars.fx.data.HeardByClient.HeardSpot s : spots) {
            double[] ll = spotterAt(s.spotter());
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
    /** The longest sideways step, in pixels, a path segment may take before it is taken to have wrapped across the flat map's date line. The globe has no such wrap. */
    private static double gcJump = 350;
    private static double gcWidth = 1.4;                            // line width of a path; a selected spot's paths are drawn heavier
    /** Set while the globe is painting: paths then arch off the surface instead of lying flat on it. */
    private static com.ars.fx.data.GlobeProjection gcGlobe;
    /** How high the middle of a path rises on the globe, in km: a tenth of its length, up to 1,500 km. The ends stay on the ground. */
    private static double gcLiftKm(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double d = 2 * Math.asin(Math.min(1, Math.sqrt(Math.pow(Math.sin((p2 - p1) / 2), 2) + Math.cos(p1) * Math.cos(p2) * Math.pow(Math.sin(Math.toRadians(lon2 - lon1) / 2), 2))));
        return Math.min(1500, 0.1 * d * 6371.0);
    }
    private static void drawGcPath(GraphicsContext g, Proj proj, double lat1, double lon1, double lat2, double lon2, Color col) {
        g.setStroke(col); g.setLineWidth(gcWidth);
        com.ars.fx.data.GlobeProjection globe = gcGlobe;
        int N = globe != null ? 48 : 24; double prevX = 0, prevY = 0; boolean have = false;
        double lift = globe != null ? gcLiftKm(lat1, lon1, lat2, lon2) : 0;
        for (int i = 0; i <= N; i++) {
            double[] pt = gcPoint(lat1, lon1, lat2, lon2, (double) i / N);
            double[] xy;
            if (globe != null) {
                com.ars.fx.data.GlobeProjection.Pt p = globe.project(pt[0], pt[1], lift * Math.sin(Math.PI * i / N));
                xy = p.visible() ? new double[]{p.x(), p.y()} : null;
            } else xy = proj.at(pt[0], pt[1]);
            if (xy == null) { have = false; continue; }
            if (have && Math.abs(xy[0] - prevX) < gcJump) g.strokeLine(prevX, prevY, xy[0], xy[1]);
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
        Q_LAT = com.ars.fx.data.HubConfig.lat();        // lat/lon, else the grid square; 0 until a location is set (the map says so)
        Q_LON = com.ars.fx.data.HubConfig.lon();
        myCall = com.ars.fx.data.HubConfig.call();
        String[][] stats = {{"SPOTS", String.valueOf(com.ars.fx.data.ClusterClient.getInstance().spots().size())},
                {"SHOWN", String.valueOf(liveSpots().size())},
                {"GRAY LINE", grayLineStat()},
                {"BEAM", beamText(), "accent"}};
        HBox top = Shell.topBar("map","map","J-Map","Propagation & spots · " + com.ars.fx.data.HubConfig.grid(), stats, nowUtc());

        // projection toggle: azimuthal canvas ⇄ rectangular blue-marble map
        StackPane mapHolder = new StackPane(); HBox.setHgrow(mapHolder, Priority.ALWAYS);
        if (projection == 2 && !GlobeView.supported()) projection = 0;       // no 3D here: the Globe button is not offered
        Runnable renderMap = () -> mapHolder.getChildren().setAll(projection == 2 ? globeArea() : projection == 1 ? rectMap() : aziArea());
        renderMap.run();
        startBeaconTicker();
        com.ars.fx.data.HeardByClient.getInstance().setListener(c -> { if (showHeardBy) repaintMap(); });
        com.ars.fx.data.HeardByClient.getInstance().start();

        // live DX-cluster spots feed the map + the rail spot list
        VBox spotsBox = new VBox();
        fillSpots(spotsBox);
        currentSpotsRefresh = () -> Shell.keepScroll(spotsBox, () -> fillSpots(spotsBox));
        com.ars.fx.data.ClusterClient cc = com.ars.fx.data.ClusterClient.getInstance();
        cc.setListener(c -> javafx.application.Platform.runLater(JMapView::refreshSpots));
        cc.start();
        // collapsible left controls column
        StackPane leftHolder = new StackPane(); leftHolder.setAlignment(Pos.TOP_LEFT);
        java.util.function.IntConsumer onProj = i -> { projection = i; com.ars.fx.data.HubConfig.setInt("map.projection", i); renderMap.run(); };
        Runnable[] rebuildLeft = new Runnable[1];
        rebuildLeft[0] = () -> leftHolder.getChildren().setAll(com.ars.fx.shell.AutoPanel.wrap("map", false, leftRail(),
                scrollLeft(leftPanel(projection, onProj, () -> rebuildLeft[0].run())), 270, JMapView::narrow));
        rebuildLeft[0].run();

        Canvas burst = new Canvas(); burst.setMouseTransparent(true);          // the starbursts: a light layer above the map, so the map itself is not redrawn every frame
        burst.widthProperty().bind(mapHolder.widthProperty()); burst.heightProperty().bind(mapHolder.heightProperty());
        StackPane.setAlignment(burst, Pos.TOP_LEFT);
        setUpBursts(burst);
        StackPane mapWithButton = new StackPane(mapHolder, burst, MapQsoCard.addButton()); HBox.setHgrow(mapWithButton, Priority.ALWAYS);
        Region projToggle = projectionToggle(onProj);                 // top right of the map, where J-Sat keeps its view buttons
        StackPane.setAlignment(projToggle, Pos.TOP_RIGHT); StackPane.setMargin(projToggle, new Insets(12, 18, 0, 0));
        mapWithButton.getChildren().add(projToggle);
        if (!com.ars.fx.data.HubConfig.hasLocation()) {
            Label warn = lbl("⚠ " + com.ars.fx.data.HubConfig.LOCATION_HINT + " Bearings, distances and the gray line need it.", "jm-tog-label");
            warn.setWrapText(true); warn.setMaxWidth(560);
            warn.setStyle("-fx-background-color:-ars-surface-2;-fx-border-color:-ars-border;-fx-background-radius:6;-fx-border-radius:6;-fx-padding:8 12 8 12;");
            StackPane.setAlignment(warn, Pos.TOP_CENTER); StackPane.setMargin(warn, new Insets(10, 0, 0, 0));
            mapWithButton.getChildren().add(warn);
        }
        mapWithButton.setMinSize(0, 0); mapHolder.setMinSize(0, 0);
        HBox center = new HBox(leftHolder, mapWithButton); center.setFillHeight(true);
        Region rail = Shell.rail(railDrawers(spotsBox));
        mapFrame = (Region) Shell.frame(dock, top, center, rail, tickerBar());
        // re-collapse / expand the controls when the window crosses the narrow threshold
        mapFrame.widthProperty().addListener((o, a, b) -> { if ((a.doubleValue() < NARROW_W) != (b.doubleValue() < NARROW_W)) rebuildLeft[0].run(); });
        return mapFrame;
    }

    private static Region mapFrame;

    // ---- starburst: a short pop on the map where a spot has just arrived ----
    private static final long BURST_MS = 2600;
    private static Canvas burstCanvas;
    private static javafx.animation.AnimationTimer burstTimer;

    private static void setUpBursts(Canvas c) {
        if (burstTimer != null) burstTimer.stop();
        burstCanvas = c;
        burstTimer = new javafx.animation.AnimationTimer() {
            @Override public void handle(long nowNs) {
                GraphicsContext g = burstCanvas.getGraphicsContext2D();
                g.clearRect(0, 0, burstCanvas.getWidth(), burstCanvas.getHeight());
                long now = System.currentTimeMillis();
                boolean any = false;
                if (ovBurst && !com.ars.fx.shell.Fx.reduced()) {
                    for (Dot d : paintedDots) {
                        long age = now - d.s().arrivalMs();
                        if (age < 0 || age >= BURST_MS) continue;
                        any = true;
                        drawBurst(g, d.x(), d.y(), age / (double) BURST_MS, d.s().color());
                    }
                }
                if (!any) stop();                                   // nothing new on the map: no more frames until the next spot
            }
        };
    }

    /** Called after each map paint: if a spot on the map is brand new, play its starburst. The timer runs only while there is one to play. */
    private static void kickBursts() {
        if (!ovBurst || burstTimer == null || com.ars.fx.shell.Fx.reduced()) return;
        long now = System.currentTimeMillis();
        for (Dot d : paintedDots) { long age = now - d.s().arrivalMs(); if (age >= 0 && age < BURST_MS) { burstTimer.start(); return; } }
    }

    /**
     * One frame of a burst, {@code t} from 0 to 1 over {@link #BURST_MS}. It has to catch the eye on a busy map without shouting: a flash
     * and a glow round the dot, two rings (the second a beat behind, and fainter) that spread, and eight rays (long and short alternately) that shoot out and
     * turn a little. It holds full strength for the first 35% and then fades. Everything is drawn twice, a dark line under the band-coloured one, so it shows on
     * a pale map as well as a dark one.
     */
    private static void drawBurst(GraphicsContext g, double x, double y, double t, Color band) {
        double a = t < 0.35 ? 1.0 : Math.pow(1 - (t - 0.35) / 0.65, 1.2);                 // hold, then fade
        double grow = 1 - Math.pow(1 - Math.min(1.0, t / 0.6), 2.2);                          // out fast, then settle
        Color col = band.interpolate(Color.WHITE, 0.15).deriveColor(0, 1.2, 1.15, a), under = Color.web("#0b0f14", 0.55 * a);
        g.setLineCap(StrokeLineCap.ROUND);

        // a soft glow round the dot for the whole burst
        double gr = 18 + 8 * grow;
        g.setFill(new javafx.scene.paint.RadialGradient(0, 0, x, y, gr, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, band.deriveColor(0, 1.2, 1.2, 0.42 * a)), new javafx.scene.paint.Stop(1, band.deriveColor(0, 1.2, 1.2, 0))));
        g.fillOval(x - gr, y - gr, gr * 2, gr * 2);

        // two rings, the second a beat behind
        for (int k = 0; k < 2; k++) {
            double tk = Math.max(0, Math.min(1.0, (t - 0.15 * k) / 0.6));
            if (tk <= 0) continue;
            double r = 6 + (26 - 6 * k) * (1 - Math.pow(1 - tk, 2.2)), w = (2.8 - 1.0 * k) * (1 - 0.7 * tk) + 0.5;
            double ak = a * (k == 0 ? 1.0 : 0.6);
            g.setLineWidth(w + 2.0); g.setStroke(Color.web("#0b0f14", 0.55 * ak)); g.strokeOval(x - r, y - r, r * 2, r * 2);
            g.setLineWidth(w); g.setStroke(col.deriveColor(0, 1, 1, k == 0 ? 1.0 : 0.6)); g.strokeOval(x - r, y - r, r * 2, r * 2);
        }

        // eight rays
        double r0 = 5 + 6 * grow, r1 = 11 + 26 * grow, turn = t * 0.7, rayW = 2.4 * (1 - 0.55 * t) + 0.7;
        for (int pass = 0; pass < 2; pass++) {
            g.setLineWidth(pass == 0 ? rayW + 2.0 : rayW); g.setStroke(pass == 0 ? under : col);
            for (int i = 0; i < 8; i++) {
                double ang = turn + i * Math.PI / 4, far = (i % 2 == 0) ? r1 : r1 * 0.62;
                g.strokeLine(x + Math.cos(ang) * r0, y + Math.sin(ang) * r0, x + Math.cos(ang) * far, y + Math.sin(ang) * far);
            }
        }

        // a white-hot flash at the centre that pulses once and dims
        double core = t < 0.25 ? 1.0 - t / 0.25 : 0.0;
        if (core > 0) {
            g.setFill(Color.web("#0b0f14", 0.4 * core)); g.fillOval(x - 8, y - 8, 16, 16);
            g.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.95 * core)); g.fillOval(x - 6, y - 6, 12, 12);
        }
    }

    // ---- projection toggle: Azimuthal / Rectangular / Globe (same look and place as J-Sat's Polar / Blue marble / Globe) ----
    private static Label[] projBtns = new Label[0];
    /** Light the button for {@code sel}; the region chips call this too, because picking a region switches the map to Rectangular. */
    private static void highlightProjection(int sel) {
        for (int i = 0; i < projBtns.length; i++) { projBtns[i].getStyleClass().remove("on"); if (i == sel) projBtns[i].getStyleClass().add("on"); }
    }
    private static Region projectionToggle(java.util.function.IntConsumer onProjection) {
        HBox h = new HBox(2); h.getStyleClass().add("jm-seg");
        String[] opts = GlobeView.supported() ? new String[]{"Azimuthal", "Rectangular", "Globe"} : new String[]{"Azimuthal", "Rectangular"};
        projBtns = new Label[opts.length];
        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            Label b = lbl(opts[i], "jm-seg-btn"); if (i == projection) b.getStyleClass().add("on");
            b.setMinWidth(96); b.setStyle("-fx-cursor:hand;");
            b.setOnMouseClicked(e -> { highlightProjection(idx); onProjection.accept(idx); });
            projBtns[i] = b; h.getChildren().add(b);
        }
        h.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return h;
    }
    // Kept here, not in build(): switching modules rebuilds J-Map, and these must survive that.
    private static int projection = Math.max(0, Math.min(2, com.ars.fx.data.HubConfig.getInt("map.projection", 0)));   // 0 azimuthal · 1 rectangular · 2 globe
    private static String nowUtc() { return java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")); }
    private static String grayLineStat() {
        if (!com.ars.fx.data.HubConfig.hasLocation()) return "set location";
        double[] rs = com.ars.fx.data.SolarPosition.riseSet(Q_LAT, Q_LON);
        return Double.isNaN(rs[0]) ? (rs[2] > 0 ? "sun up all day" : "sun down all day") : "SR " + com.ars.fx.data.SolarPosition.hhmm(rs[0]) + " · SS " + com.ars.fx.data.SolarPosition.hhmm(rs[1]);
    }
    /** Live topbar: ticking UTC clock + SPOTS / SHOWN / GRAY-LINE / BEAM. */
    private static void updateMapTopbar() {
        if (mapFrame == null) return;
        if (mapFrame.lookup(".sx-clock-v") instanceof Label l) l.setText(nowUtc());
        int shown = liveSpots().size();
        if (legendSpots != null) legendSpots.setText(legendSpotsText(shown));
        if (spotsDrawer != null && spotsDrawer.lookup(".sx-dw-sum") instanceof Label sum) sum.setText(shown + " spots");
        java.util.List<Label> vs = new java.util.ArrayList<>(); collectStatValues(mapFrame, vs);
        if (vs.size() >= 4) {
            com.ars.fx.shell.Fx.setAndFlash(vs.get(0), String.valueOf(com.ars.fx.data.ClusterClient.getInstance().spots().size()));   // a changed count pops
            com.ars.fx.shell.Fx.setAndFlash(vs.get(1), String.valueOf(shown));
            vs.get(2).setText(grayLineStat());
            vs.get(3).setText(beamText());
        }
    }
    private static void collectStatValues(Node n, java.util.List<Label> out) {
        if (n instanceof Label l && l.getStyleClass().contains("sx-stat-v")) out.add(l);
        if (n instanceof javafx.scene.Parent p) for (Node c : p.getChildrenUnmodifiable()) collectStatValues(c, out);
    }

    // ---- bottom alert scroller (port of original J-Map DxScrollerBar) ------
    private static Region tickerBar() {
        double H = 42, speed = 70;                            // px per second
        Pane pane = new Pane();
        pane.setMinHeight(H); pane.setPrefHeight(H); pane.setMaxHeight(H);
        pane.setStyle("-fx-background-color:#07090c;-fx-border-color:#2c3540 transparent transparent transparent;-fx-border-width:1 0 0 0;");
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(pane.widthProperty()); clip.setHeight(H);
        pane.setClip(clip);

        // One text node slid along by the GPU: redrawing a canvas on a timer steps unevenly against the screen refresh.
        Font font = Font.font("JetBrains Mono", 12.5);
        Text line = new Text(); line.setFont(font); line.setFill(Color.web("#c3ccd6"));
        line.setTextOrigin(javafx.geometry.VPos.CENTER); line.setLayoutY(H / 2);
        line.setCache(true); line.setCacheHint(javafx.scene.CacheHint.SPEED);
        pane.getChildren().add(line);

        double[] singleW = new double[1];
        Runnable refreshText = () -> {
            String one = com.ars.fx.data.SolarData.tickerText(com.ars.fx.data.SolarData.getInstance().snapshot(), grayLineStat()) + "   ";
            Text meas = new Text(one); meas.setFont(font);
            singleW[0] = meas.getLayoutBounds().getWidth();
            int copies = (int) Math.ceil(Math.max(pane.getWidth(), 1920) / singleW[0]) + 1;   // enough to fill the bar past the wrap
            line.setText(one.repeat(copies));
        };
        refreshText.run();

        double[] x = { Double.NaN };
        long[] last = { 0 };
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            @Override public void handle(long now) {
                double w = pane.getWidth();
                if (w <= 0) return;
                if (Double.isNaN(x[0])) x[0] = w;
                if (last[0] != 0) x[0] -= speed * (now - last[0]) / 1e9;   // by elapsed time, so a busy frame can't slow it
                last[0] = now;
                if (x[0] <= -singleW[0]) { refreshText.run(); x[0] = 0; }  // new figures only where the line repeats
                line.setTranslateX(x[0]);
            }
        };
        timer.start();
        Runnable onData = refreshText;                         // new figures as soon as they arrive, not only at the next wrap
        com.ars.fx.data.SolarData.getInstance().addListener(onData);
        pane.sceneProperty().addListener((o, old, sc) -> {
            if (sc == null && old != null) { timer.stop(); com.ars.fx.data.SolarData.getInstance().removeListener(onData); }
        });
        return pane;
    }

    // ---- left controls panel ----------------------------------------------
    /** The controls column is taller than a normal window; it must scroll rather than stretch the whole frame. */
    private static Region scrollLeft(Region panel) {
        ScrollPane sp = new ScrollPane(panel); sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setMinWidth(270); sp.setPrefWidth(270); sp.setMaxWidth(270);
        sp.setStyle("-fx-background-color:transparent;");
        return sp;
    }
    private static Region leftPanel(int projection, java.util.function.IntConsumer onProjection, Runnable rebuild) {
        VBox p = new VBox(0); p.getStyleClass().add("jm-panel");
        Label title = lbl("MAP CONTROLS", "jm-sec"); title.setStyle("-fx-padding:0 0 0 0;"); HBox.setHgrow(title, Priority.ALWAYS); title.setMaxWidth(Double.MAX_VALUE);
        HBox header = new HBox(8, title, com.ars.fx.shell.AutoPanel.pinButton("map", rebuild)); header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(0, 0, 2, 0));
        p.getChildren().add(header);
        // fixed-zoom region picker — single click. Picking a region snaps the blue
        // marble to that window AND switches the projection to Rectangular, so it
        // takes effect immediately even from the azimuthal view.
        p.getChildren().add(lbl("REGION","jm-sec"));
        FlowPane regions = new FlowPane(6, 6);
        Label[] rchips = new Label[REGION_NAMES.length];
        Runnable paintChips = () -> {
            for (int i = 0; i < rchips.length; i++) {
                boolean on = i == mapRegion;
                rchips[i].setStyle("-fx-cursor:hand; -fx-padding:4 10 4 10; -fx-background-radius:6; -fx-font-size:11px;"
                        + (on ? "-fx-background-color:-ars-map; -fx-text-fill:#06121a; -fx-font-weight:bold;"
                              : "-fx-background-color:-ars-surface-4; -fx-text-fill:-ars-t2;"));
            }
        };
        for (int i = 0; i < REGION_NAMES.length; i++) {
            final int idx = i;
            Label chip = new Label(REGION_NAMES[i]);
            rchips[i] = chip;
            chip.setOnMouseClicked(e -> { mapRegion = idx; com.ars.fx.data.HubConfig.setInt("map.region", idx); paintChips.run(); highlightProjection(1); onProjection.accept(1); });
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
        for (int i=0;i<MAP_BANDS.length;i++){
            String[] b = MAP_BANDS[i];
            String key = b[0] + "m";                          // Mock bands are bare ("20"); cluster bands are "20m"
            Region dot = new Region(); dot.getStyleClass().add("jm-band-dot"); dot.setStyle("-fx-background-color:"+b[1]+";");
            HBox chip = new HBox(7, dot, lbl(b[0],"jm-band-n")); chip.getStyleClass().add("jm-band"); chip.setAlignment(Pos.CENTER_LEFT);
            GridPane.setHgrow(chip, Priority.ALWAYS); chip.setMaxWidth(Double.MAX_VALUE); chip.setStyle("-fx-cursor:hand;");
            chip.setOnMouseClicked(e -> { if (!spotBands.remove(key)) spotBands.add(key); com.ars.fx.data.HubConfig.set("map.bands", String.join(",", spotBands)); restyleBands.run(); refreshSpots(); });
            bandChips.add(chip); bandKeys.add(key);
            bands.add(chip, i%3, i/3);
        }
        for (int c=0;c<3;c++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(100/3.0); bands.getColumnConstraints().add(cc); }
        restyleBands.run();
        p.getChildren().add(bands);
        p.getChildren().add(lbl("MODE","jm-sec"));
        p.getChildren().add(segSel(new String[]{"All","CW","Phone","Digi"}, spotMode, i -> { spotMode = i; com.ars.fx.data.HubConfig.setInt("map.spotMode", i); refreshSpots(); }));
        Label ageLbl = lbl("SPOT AGE ≤ " + spotAgeMax + " MIN","jm-sec");
        javafx.animation.PauseTransition saveSpotAge = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
        saveSpotAge.setOnFinished(e -> com.ars.fx.data.HubConfig.setInt("map.spotAge", spotAgeMax));
        p.getChildren().add(ageLbl);
        p.getChildren().add(sliderI((spotAgeMax - 1) / 59.0, f -> {
            spotAgeMax = (int) Math.max(1, Math.round(1 + f * 59));
            ageLbl.setText("SPOT AGE ≤ " + spotAgeMax + " MIN"); refreshSpots();
            saveSpotAge.playFromStart();                      // the slider fires on every pixel: save once it settles
        }));
        Label pn = lbl("Path now: fade closed bands, ring open ones", "jm-tog-label"); HBox.setHgrow(pn, Priority.ALWAYS); pn.setMaxWidth(Double.MAX_VALUE);
        com.ars.fx.shell.Tips.tip(pn, "Our own propagation estimate from the solar flux, sunspots and K index: a spot on a band that is probably closed from here fades, one that is likely open gets a green ring. An estimate, not a measurement. Needs your location set.");
        HBox pnr = new HBox(pn, toggleSwitch(com.ars.fx.data.PathNow.shown(), v -> { com.ars.fx.data.HubConfig.set("map.pathNow", String.valueOf(v)); repaintMap(); refreshSpots(); }));
        pnr.setAlignment(Pos.CENTER_LEFT); pnr.setPadding(new Insets(7, 0, 7, 0));
        p.getChildren().add(pnr);
        p.getChildren().add(lbl("OVERLAYS","jm-sec"));
        for (int oi = 0; oi < OVERLAYS.length; oi++) {
            final int idx = oi;
            Label t = lbl((String) OVERLAYS[oi][0], "jm-tog-label"); HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE);
            if (OVERLAYS[oi].length > 1) com.ars.fx.shell.Tips.tip(t, (String) OVERLAYS[oi][1]);
            HBox row = new HBox(t, toggleSwitch(overlayFlag(idx), v -> { setOverlayFlag(idx, v); repaintMap(); }));
            row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(7, 0, 7, 0));
            p.getChildren().add(row);
        }
        // ham-to-ham tracks
        p.getChildren().add(lbl("TRACKS (HAM TO HAM)","jm-sec"));
        javafx.scene.control.TextField trA = new javafx.scene.control.TextField(), trB = new javafx.scene.control.TextField();
        trA.setPromptText("from: call, prefix or grid"); trB.setPromptText("to: call, prefix or grid");
        for (javafx.scene.control.TextField f : new javafx.scene.control.TextField[]{trA, trB}) f.getStyleClass().add("jhub-mini-field");
        Label trMsg = lbl("", "jm-tog-label"); trMsg.setWrapText(true);
        Label trDraw = lbl("Draw", "jl-logbtn"), trClear = lbl("Clear", "jl-clearbtn");
        trDraw.setStyle("-fx-cursor:hand;"); trClear.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(trDraw, "Draw the great-circle path between two stations, with its distance and beam heading. Also from a spot: click a spot, then \"Show path\" to see where the spotter heard it from.");
        Runnable drawTrack = () -> {
            com.ars.fx.data.MapTracks.Track t = com.ars.fx.data.MapTracks.add(trA.getText(), trB.getText());
            trMsg.setText(t == null ? "Cannot place one of those (a call, a prefix such as ZS, or a grid such as FN31)." : t.label());
            repaintMap();
        };
        trDraw.setOnMouseClicked(e -> drawTrack.run());
        trA.setOnAction(e -> trB.requestFocus()); trB.setOnAction(e -> drawTrack.run());
        trClear.setOnMouseClicked(e -> { com.ars.fx.data.MapTracks.clear(); trMsg.setText(""); repaintMap(); });
        HBox trBtns = new HBox(14, trDraw, trClear); trBtns.setAlignment(Pos.CENTER_LEFT); trBtns.setPadding(new Insets(4, 0, 4, 0));
        p.getChildren().addAll(trA, trB, trBtns, trMsg);
        // NCDXF/IARU International Beacon Project cycle
        p.getChildren().add(lbl("NCDXF BEACONS","jm-sec"));
        Label bt = lbl("Show beacon cycle","jm-tog-label"); HBox.setHgrow(bt, Priority.ALWAYS); bt.setMaxWidth(Double.MAX_VALUE);
        beaconInfo = lbl(showBeacons ? beaconLine() : "", "jm-tog-label"); beaconInfo.setWrapText(true);
        beaconInfo.setStyle("-fx-text-fill:#ffd84a;-fx-font-weight:bold;");
        HBox btr = new HBox(bt, toggleSwitch(showBeacons, v -> { showBeacons = v; beaconInfo.setText(v ? beaconLine() : ""); repaintMap(); qsyToBeacon(); }));
        btr.setAlignment(Pos.CENTER_LEFT); btr.setPadding(new Insets(7, 0, 4, 0));
        p.getChildren().add(btr);
        beaconSeg = (HBox) segSel(BEACON_BANDS, beaconBand, i -> { beaconBand = i; beaconInfo.setText(showBeacons ? beaconLine() : ""); refreshBeaconHeard(); repaintMap(); qsyToBeacon(); });
        p.getChildren().add(beaconSeg);
        p.getChildren().add(beaconInfo);
        Label qt = lbl("Auto QSY the rig","jm-tog-label"); HBox.setHgrow(qt, Priority.ALWAYS); qt.setMaxWidth(Double.MAX_VALUE);
        com.ars.fx.shell.Tips.tip(qt, "Tune the rig to the beacon frequency (CW) when you pick a band or switch the cycle on. Needs the rig connected; never while transmitting.");
        HBox qtr = new HBox(qt, toggleSwitch(com.ars.fx.data.HubConfig.getBool("map.beaconQsy", false), v -> { com.ars.fx.data.HubConfig.set("map.beaconQsy", String.valueOf(v)); qsyToBeacon(); }));
        qtr.setAlignment(Pos.CENTER_LEFT); qtr.setPadding(new Insets(4, 0, 7, 0));
        p.getChildren().add(qtr);
        Label hp = lbl("Hop bands each 3 minutes", "jm-tog-label"); HBox.setHgrow(hp, Priority.ALWAYS); hp.setMaxWidth(Double.MAX_VALUE);
        com.ars.fx.shell.Tips.tip(hp, "Moves to the next band every 3-minute cycle (20, 17, 15, 12, 10 m), so one watch hears every beacon on every band in 15 minutes. With Auto QSY the rig follows.");
        HBox hpr = new HBox(hp, toggleSwitch(beaconHop(), v -> { com.ars.fx.data.HubConfig.set("map.beaconHop", String.valueOf(v)); if (v) beaconWatch(); }));
        hpr.setAlignment(Pos.CENTER_LEFT); hpr.setPadding(new Insets(4, 0, 4, 0));
        Label au = lbl("Mark heard by the S-meter", "jm-tog-label"); HBox.setHgrow(au, Priority.ALWAYS); au.setMaxWidth(Double.MAX_VALUE);
        com.ars.fx.shell.Tips.tip(au, "Marks the beacon on the air as heard when the rig is on the beacon frequency in CW and its S-meter holds S3 or more for three seconds. Needs a rig that reports its S-meter. You can always click a beacon on the map to mark or unmark it.");
        HBox aur = new HBox(au, toggleSwitch(beaconAuto(), v -> com.ars.fx.data.HubConfig.set("map.beaconAuto", String.valueOf(v))));
        aur.setAlignment(Pos.CENTER_LEFT); aur.setPadding(new Insets(4, 0, 4, 0));
        beaconHeardInfo = lbl("", "jm-tog-label"); beaconHeardInfo.setWrapText(true);
        refreshBeaconHeard();
        Label hear = lbl("I hear it", "jl-logbtn"), logBtn = lbl("Heard log", "jl-clearbtn"), clearHeard = lbl("Clear", "jl-clearbtn");
        for (Label l : new Label[]{hear, logBtn, clearHeard}) l.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(hear, "Marks the beacon that is on the air now, on the band shown, as heard.");
        com.ars.fx.shell.Tips.tip(logBtn, "Writes the beacons you have heard, with the UTC time, to a CSV file and opens it.");
        hear.setOnMouseClicked(e -> { com.ars.fx.data.BeaconHeard.mark(BEACONS[activeBeacon(beaconBand)][0], BEACON_BANDS[beaconBand], java.time.Instant.now().getEpochSecond()); refreshBeaconHeard(); repaintMap(); });
        logBtn.setOnMouseClicked(e -> {
            try {
                java.nio.file.Path f = java.nio.file.Paths.get(System.getProperty("user.home"), ".j-map", "beacons-heard.csv");
                java.nio.file.Files.createDirectories(f.getParent());
                java.nio.file.Files.writeString(f, com.ars.fx.data.BeaconHeard.csv(), java.nio.charset.StandardCharsets.UTF_8);
                com.ars.fx.data.LinkOpener.openFile(f, () -> { });
            } catch (java.io.IOException ex) { beaconHeardInfo.setText("Could not write the log: " + ex.getMessage()); }
        });
        clearHeard.setOnMouseClicked(e -> { com.ars.fx.data.BeaconHeard.clear(); refreshBeaconHeard(); repaintMap(); });
        HBox heardBtns = new HBox(14, hear, logBtn, clearHeard); heardBtns.setAlignment(Pos.CENTER_LEFT); heardBtns.setPadding(new Insets(4, 0, 6, 0));
        p.getChildren().addAll(hpr, aur, beaconHeardInfo, heardBtns);

        // DX-cluster spot paths: spotted station to spotter
        p.getChildren().add(lbl("SPOT PATHS · DX CLUSTER","jm-sec"));
        Label sp = lbl("Draw spotted \u2192 spotter for each shown spot","jm-tog-label"); HBox.setHgrow(sp, Priority.ALWAYS); sp.setMaxWidth(Double.MAX_VALUE);
        HBox spr = new HBox(sp, toggleSwitch(showSpotPaths, v -> { showSpotPaths = v; repaintMap(); }));
        spr.setAlignment(Pos.CENTER_LEFT); spr.setPadding(new Insets(7, 0, 4, 0));
        com.ars.fx.shell.Tips.tip(spr, "Draw a line from each spotted station to the station that spotted it, in the band's colour. It follows the mode and band filters, fades with the spot's age and goes after 10 minutes.");
        Label spClear = lbl("Clear", "jl-clearbtn"); spClear.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(spClear, "Wipe the spot paths and start again: only spots that arrive from now on get a line.");
        spClear.setOnMouseClicked(e -> { spotPathsSince = System.currentTimeMillis(); selectedSpotCall = null; repaintMap(); });
        HBox spBtns = new HBox(14, spClear); spBtns.setAlignment(Pos.CENTER_LEFT); spBtns.setPadding(new Insets(0, 0, 6, 0));
        p.getChildren().addAll(spr, spBtns);

        // RBN "who's hearing me" overlay
        p.getChildren().add(lbl("WHO'S HEARING ME · RBN","jm-sec"));
        Label ht = lbl("Show RBN spots of " + (myCall.isEmpty() ? "my call" : myCall.toUpperCase()),"jm-tog-label"); HBox.setHgrow(ht, Priority.ALWAYS); ht.setMaxWidth(Double.MAX_VALUE);
        HBox htr = new HBox(ht, toggleSwitch(showHeardBy, v -> { showHeardBy = v; repaintMap(); }));
        htr.setAlignment(Pos.CENTER_LEFT); htr.setPadding(new Insets(7, 0, 4, 0));
        p.getChildren().add(htr);

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
        StackPane sw = new StackPane(knob); sw.getStyleClass().add("jm-switch"); com.ars.fx.shell.Tips.tip(sw, "Click to switch on or off"); sw.setMaxSize(36, 20); sw.setPadding(new Insets(0, 3, 0, 3));
        Runnable paint = () -> { sw.getStyleClass().remove("on"); if (on[0]) sw.getStyleClass().add("on"); StackPane.setAlignment(knob, on[0] ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); };
        paint.run(); sw.setStyle("-fx-cursor:hand;");
        sw.setOnMouseClicked(e -> { on[0] = !on[0]; paint.run(); onChange.accept(on[0]); });
        return sw;
    }
    /** Collapsed left column — a thin rail with a re-open control. */
    private static Region leftRail() {
        VBox p = new VBox(0); p.getStyleClass().add("jm-panel"); p.setMinWidth(40); p.setPrefWidth(40); p.setMaxWidth(40);
        p.setAlignment(Pos.TOP_CENTER); p.setPadding(new Insets(16, 0, 16, 0));
        Label expand = lbl("›", "jm-collapse");
        Label vlabel = lbl("CONTROLS", "jm-sec"); vlabel.setRotate(90);
        javafx.scene.Group vgroup = new javafx.scene.Group(vlabel);   // sized to the rotated label, so it can't spill over the dock
        VBox.setMargin(vgroup, new Insets(26, 0, 0, 0));
        p.getChildren().addAll(expand, vgroup);
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
        legendSpots = rt(legendSpotsText(liveSpots().size()));
        VBox tr = new VBox(2, legendSpots,
            rt("rings = " + (com.ars.fx.data.Units.standard() ? "3,000 mi" : "5,000 km")), rt("spokes = 30°"));
        tr.setAlignment(Pos.TOP_RIGHT); tr.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(tr, Pos.TOP_RIGHT); StackPane.setMargin(tr, new Insets(58,18,0,0));
        StackPane area = new StackPane(cv, tr); area.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(area, Priority.ALWAYS);
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
    // ---- 3D globe ----------------------------------------------------------
    private static GlobeView mapGlobe;                     // one instance, so the way it was turned survives rebuilds

    private static Region globeArea() {
        if (mapGlobe == null) {
            mapGlobe = new GlobeView("map");
            mapGlobe.stationDot(false);                    // the painter draws the station with its call
            mapGlobe.painter(JMapView::paintGlobe);
            mapGlobe.onClick((x, y) -> handleMapClick(mapGlobe, x, y));
        }
        mapGlobe.station(Q_LAT, Q_LON);
        legendSpots = rt(legendSpotsText(liveSpots().size()));
        VBox tr = new VBox(2, legendSpots, rt("rings = " + (com.ars.fx.data.Units.standard() ? "3,000 mi" : "5,000 km")));
        tr.setAlignment(Pos.TOP_RIGHT); tr.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE); tr.setMouseTransparent(true);
        StackPane.setAlignment(tr, Pos.TOP_RIGHT); StackPane.setMargin(tr, new Insets(58, 18, 0, 0));
        StackPane area = new StackPane(mapGlobe, tr); area.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(area, Priority.ALWAYS);
        area.setMinSize(0, 0);
        currentMapRepaint = () -> { mapGlobe.night(ovGrayline); mapGlobe.refresh(); };
        currentMapRepaint.run();
        return area;
    }

    private static java.util.List<MapSpot> globeSpots = java.util.List.of();
    /** Everything J-Map draws on the flat and azimuthal maps, on the globe: range rings, the beam, spots, the sun, the station, and the track / path / beacon / heard-by layers. */
    private static void paintGlobe(GraphicsContext g, com.ars.fx.data.GlobeProjection pr, double w, double h, boolean fresh) {
        loadPalette();
        if (fresh) globeSpots = liveSpots();               // a turn or zoom reuses the last lookup; a filter change or a new spot asks for a fresh one
        frameSpots = globeSpots;
        Proj gp = (lat, lon) -> { com.ars.fx.data.GlobeProjection.Pt p = pr.project(lat, lon, 0); return p.visible() ? new double[]{p.x(), p.y()} : null; };
        gcJump = Double.MAX_VALUE; gcGlobe = pr;
        try {
            if (ovRings) {
                g.setStroke(ovBasemap ? P.line() : Color.web("#28313d")); g.setLineWidth(1); g.setLineDashes(4, 5);
                for (double km : new double[]{5000, 10000, 15000}) globeLine(g, pr, com.ars.fx.data.GlobeProjection.circle(Q_LAT, Q_LON, km / 111.195, 120));
                g.setLineDashes(null);
            }
            if (ovBeam && !Double.isNaN(beamDeg())) drawGlobeBeam(g, pr, beamDeg());

            double spotA = dimSpots() ? 0.28 : 1.0;
            paintedDots.clear();
            for (MapSpot s : globeSpots) {
                if (spotHidden(s.call())) continue;
                double[] xy = gp.at(s.lat(), s.lon());
                if (xy == null) continue;
                paintedDots.add(new Dot(xy[0], xy[1], s));
                g.setStroke(Color.web("#0b0f14", 0.8 * spotA)); g.setLineWidth(2); g.strokeOval(xy[0] - 4.5, xy[1] - 4.5, 9, 9);
                spotDot(g, s, xy[0], xy[1], spotA);
            }
            if (ovGrayline) drawGlobeSun(g, pr);
            double[] me = gp.at(Q_LAT, Q_LON);
            if (me != null) {
                g.setFill(Color.web("#31bdda")); g.fillOval(me[0] - 5, me[1] - 5, 10, 10);
                g.setStroke(Color.web("#31bdda", 0.35)); g.setLineWidth(3); g.strokeOval(me[0] - 5, me[1] - 5, 10, 10);
                g.setStroke(Color.web("#0b0f14", 0.7)); g.setLineWidth(3); g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12)); g.setTextAlign(TextAlignment.LEFT);
                g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND); g.strokeText(myCall, me[0] + 9, me[1] + 4); g.setLineJoin(javafx.scene.shape.StrokeLineJoin.MITER);
                g.setFill(Color.web("#31bdda")); g.fillText(myCall, me[0] + 9, me[1] + 4);
            }
            drawSpotPaths(g, gp);
            drawHeardBy(g, gp);
            drawBeacons(g, gp);
            drawTracks(g, gp);
            if (ovCallsigns) drawSpotLabels(g, spotA, w, h, me != null ? me[0] : Double.NaN, me != null ? me[1] : 0);
            kickBursts();
        } finally { gcJump = 350; gcGlobe = null; frameSpots = null; }
    }
    /**
     * The sun out in space: on the line of constant latitude it rides all day (today's declination), at an orbital height rather than flat on the
     * surface, with a faint line down to the point it is overhead. Round the back of the globe it is hidden by it.
     */
    private static void drawGlobeSun(GraphicsContext g, com.ars.fx.data.GlobeProjection pr) {
        double[] ss = com.ars.fx.data.SolarPosition.subSolar();
        double alt = pr.orbitAltKm(1.3);                  // near enough that it stays on the screen for most views of the globe
        double[][] track = new double[121][];
        for (int i = 0; i <= 120; i++) track[i] = new double[]{ss[0], -180 + 3.0 * i};
        g.setStroke(Color.web("#ffd84a", 0.45)); g.setLineWidth(1.2); g.setLineDashes(2, 5);
        globeLine(g, pr, track, alt);
        com.ars.fx.data.GlobeProjection.Pt up = pr.project(ss[0], ss[1], alt), down = pr.project(ss[0], ss[1], 0);
        if (up.visible() && down.visible()) { g.setLineDashes(3, 4); g.setStroke(Color.web("#ffd84a", 0.4)); g.strokeLine(down.x(), down.y(), up.x(), up.y()); }
        g.setLineDashes(null);
        if (up.visible()) drawSun(g, up.x(), up.y());
    }
    /** A polyline of {lat, lon} points on the globe, broken where a point is behind it. */
    private static void globeLine(GraphicsContext g, com.ars.fx.data.GlobeProjection pr, double[][] pts) { globeLine(g, pr, pts, 0); }
    private static void globeLine(GraphicsContext g, com.ars.fx.data.GlobeProjection pr, double[][] pts, double altKm) {
        com.ars.fx.data.GlobeProjection.Pt prev = null;
        for (double[] p : pts) {
            com.ars.fx.data.GlobeProjection.Pt cur = pr.project(p[0], p[1], altKm);
            if (prev != null && prev.visible() && cur.visible()) g.strokeLine(prev.x(), prev.y(), cur.x(), cur.y());
            prev = cur;
        }
    }
    /** The beam as a centre line and two edges 11 degrees either side, out to the same 15,000 km as the other maps; filled when all of it is on the near side. */
    private static void drawGlobeBeam(GraphicsContext g, com.ars.fx.data.GlobeProjection pr, double heading) {
        int steps = 40; double reach = 15000, spread = 11;
        double[][] left = new double[steps + 1][], right = new double[steps + 1][], mid = new double[steps + 1][];
        for (int i = 0; i <= steps; i++) {
            double km = reach * i / steps;
            left[i] = dest(Q_LAT, Q_LON, heading - spread, km); right[i] = dest(Q_LAT, Q_LON, heading + spread, km); mid[i] = dest(Q_LAT, Q_LON, heading, km);
        }
        double[] xs = new double[(steps + 1) * 2], ys = new double[(steps + 1) * 2];
        boolean all = true;
        for (int i = 0; i <= steps; i++) {
            com.ars.fx.data.GlobeProjection.Pt a = pr.project(left[i][0], left[i][1], 0), b = pr.project(right[i][0], right[i][1], 0);
            xs[i] = a.x(); ys[i] = a.y(); xs[xs.length - 1 - i] = b.x(); ys[ys.length - 1 - i] = b.y();
            all &= a.visible() && b.visible();
        }
        if (all) { g.setFill(Color.web("#31bdda", 0.2)); g.fillPolygon(xs, ys, xs.length); }
        g.setStroke(Color.web("#31bdda", 0.8)); g.setLineWidth(1.4);
        globeLine(g, pr, mid);
        if (!all) { g.setStroke(Color.web("#31bdda", 0.45)); globeLine(g, pr, left); globeLine(g, pr, right); }
    }

    private static Label rt(String s){ return lbl(s, "jm-legend"); }
    private static Label legendSpots;                     // live first line of the azimuthal note
    private static String legendSpotsText(int shown) { return shown + " spots · ≤ " + spotAgeMax + " min"; }

    private static void paintAzimuthal(GraphicsContext g, double w, double h) {
        g.clearRect(0, 0, w, h);
        double cx = w/2, cy = h/2 + 4, R = Math.max(70, Math.min(w, h)/2 - 70);
        loadPalette();
        Color border = ovBasemap ? P.line() : Color.web("#28313d"), faint = Color.web("#28313d"), t3 = ovBasemap ? P.lineText() : Color.web("#6a7684"), t4 = ovBasemap ? P.lineText() : Color.web("#4d5663");

        // vector world basemap (Natural Earth), azimuthal-equidistant about the station, clipped to the disk
        if (ovBasemap) {
            g.save();
            g.beginPath(); g.arc(cx, cy, R, R, 0, 360); g.closePath(); g.clip();
            g.setFill(P.ocean()); g.fillRect(cx - R, cy - R, R * 2, R * 2);
            drawVectorAzimuthal(g, cx, cy, R);
            g.restore();
        }

        // real day/night terminator (gray line) projected into the azimuthal map
        if (ovGrayline) drawAzimuthalGrayLine(g, cx, cy, R);

        // range rings
        if (ovRings) {
            double[] rr = {0.34, 0.67, 1.0}; String[] rl = com.ars.fx.data.Units.standard() ? new String[]{"3k mi","6k mi","9k mi"} : new String[]{"5k km","10k km","15k km"};
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
            g.setStroke(ovBasemap ? P.line().deriveColor(0, 1, 1, 0.55) : Color.web("#28313d", 0.55));
            g.strokeLine(cx, cy, cx+Math.cos(a)*R, cy+Math.sin(a)*R);
            g.setFill(t3); g.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, 11)); g.setTextAlign(TextAlignment.CENTER);
            g.fillText(comp[i], cx+Math.cos(a)*(R+18), cy+Math.sin(a)*(R+18)+4);
        }

        // beam wedge toward the selected heading
        if (ovBeam && !Double.isNaN(beamDeg())) {
            double beam = Math.toRadians(beamDeg() - 90), spread = Math.toRadians(11);
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
            spotDot(g, s, x, y, spotA);
        }
        // the Sun — sub-solar point projected into the azimuthal map (if visible)
        if (ovGrayline) {
            double[] ss = com.ars.fx.data.SolarPosition.subSolar();
            double[] sp = azProject(ss[0], ss[1], cx, cy, R);
            if (sp[2] <= 1.0) drawSun(g, sp[0], sp[1]);
        }

        // me (center)
        g.setFill(Color.web("#31bdda")); g.fillOval(cx-5, cy-5, 10, 10);
        g.setStroke(Color.web("#31bdda", 0.35)); g.setLineWidth(3); g.strokeOval(cx-5, cy-5, 10, 10);
        g.setFill(Color.web("#31bdda")); g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12)); g.fillText(myCall, cx+9, cy+4);

        // NCDXF beacon + "who's hearing me" overlays (azimuthal-equidistant projection)
        Proj azi = (lat, lon) -> { double[] pp = azProject(lat, lon, cx, cy, R); return pp[2] <= 1.0 ? new double[]{pp[0], pp[1]} : null; };
        drawSpotPaths(g, azi);
        drawHeardBy(g, azi);
        drawBeacons(g, azi);
        drawTracks(g, azi);
        if (ovCallsigns) drawSpotLabels(g, spotA, w, h, cx, cy);
        kickBursts();
    }
    /** Azimuthal-equidistant night shading + terminator (gray line) centred on QTH. */
    // One image, not thousands of small rectangles: those stalled the render thread for about 400 ms on every repaint.
    private static javafx.scene.image.WritableImage nightMask;
    private static String nightMaskKey = "";

    /** The dark side of the azimuthal disk, recalculated when the size, station or minute changes. */
    private static javafx.scene.image.Image nightMask(double R) {
        int size = (int) Math.round(R);                       // half the disk's pixels; scaling up softens the edge
        String key = size + "/" + Q_LAT + "/" + Q_LON + "/" + (System.currentTimeMillis() / 60_000) + "/" + P.night();
        if (key.equals(nightMaskKey)) return nightMask;

        double[] ss = com.ars.fx.data.SolarPosition.subSolar();
        double declR = Math.toRadians(ss[0]), sinDecl = Math.sin(declR), cosDecl = Math.cos(declR), ssLonR = Math.toRadians(ss[1]);
        double qLatR = Math.toRadians(Q_LAT), qLonR = Math.toRadians(Q_LON), sinQ = Math.sin(qLatR), cosQ = Math.cos(qLatR);
        int[] argb = new int[size * size];
        Color nc = ovBasemap ? P.night() : Color.web("#000000", 0.42);
        int night = ((int) Math.round(nc.getOpacity() * 255) << 24) | ((int) Math.round(nc.getRed() * 255) << 16) | ((int) Math.round(nc.getGreen() * 255) << 8) | (int) Math.round(nc.getBlue() * 255);
        double half = size / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x + 0.5 - half, dy = y + 0.5 - half, rp = Math.sqrt(dx * dx + dy * dy);
                if (rp > half) continue;
                double delta = (rp / half) * 15000.0 / 6371.0;     // angular distance (rad)
                double brg = Math.atan2(dy, dx) + Math.PI / 2;     // bearing from north
                double sinDel = Math.sin(delta), cosDel = Math.cos(delta);
                double latR = Math.asin(sinQ * cosDel + cosQ * sinDel * Math.cos(brg));
                double lonR = qLonR + Math.atan2(Math.sin(brg) * sinDel * cosQ, cosDel - sinQ * Math.sin(latR));
                if (Math.sin(latR) * sinDecl + Math.cos(latR) * cosDecl * Math.cos(lonR - ssLonR) < 0) argb[y * size + x] = night;
            }
        }
        if (nightMask == null || (int) nightMask.getWidth() != size) nightMask = new javafx.scene.image.WritableImage(size, size);
        nightMask.getPixelWriter().setPixels(0, 0, size, size,
                javafx.scene.image.PixelFormat.getIntArgbInstance(), argb, 0, size);
        nightMaskKey = key;
        return nightMask;
    }

    private static void drawAzimuthalGrayLine(GraphicsContext g, double cx, double cy, double R) {
        double[] ss = com.ars.fx.data.SolarPosition.subSolar(); double decl = ss[0], ssLon = ss[1];

        g.drawImage(nightMask(R), cx - R, cy - R, R * 2, R * 2);

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
    /** The map colours for the colour scheme in force; reloaded at the start of every paint so the theme button takes effect at once. */
    private static MapPalette P = MapPalette.forScheme("dark");
    private static void loadPalette() { P = MapPalette.forScheme(com.ars.fx.Themes.effectiveKey("map")); }
    /** Called when the theme button is pressed: repaint the map in the new colours. */
    public static void themeChanged() { if (currentMapRepaint != null) currentMapRepaint.run(); }
    /** Antarctica and Greenland are drawn as ice, everything else as land. */
    private static boolean isIce(com.ars.fx.data.VectorBasemap.Ring r) {
        return r.maxLat() < -60 || (r.minLon() > -75 && r.maxLon() < -10 && r.minLat() > 58 && r.points() > 200);
    }

    /** Land and borders in the azimuthal projection. Long edges are split so they bend like great circles; points past 140 degrees are held there (the disk clips at 135). */
    private static void drawVectorAzimuthal(GraphicsContext g, double cx, double cy, double R) {
        double maxD = 140.0 * 111.195 / 15000.0;                          // normalised distance cap
        g.setFill(P.land()); g.setStroke(P.coast()); g.setLineWidth(0.8);
        for (com.ars.fx.data.VectorBasemap.Ring ring : com.ars.fx.data.VectorBasemap.land().rings()) {
            boolean ice = isIce(ring);
            g.setFill(ice ? P.ice() : P.land()); g.setStroke(ice ? P.iceCoast() : P.coast());
            azPath(g, ring.ll(), cx, cy, R, maxD, true);
            g.fill(); g.stroke();
        }
        g.setStroke(P.border()); g.setLineWidth(0.7);
        for (com.ars.fx.data.VectorBasemap.Ring ring : com.ars.fx.data.VectorBasemap.borders().rings()) {
            azPath(g, ring.ll(), cx, cy, R, maxD, false);
            g.stroke();
        }
    }
    private static void azPath(GraphicsContext g, double[] ll, double cx, double cy, double R, double maxD, boolean close) {
        g.beginPath();
        int n = ll.length / 2;
        double plat = 0, plon = 0;
        for (int i = 0; i < n; i++) {
            double lat = ll[i * 2 + 1], lon = ll[i * 2];
            int sub = 1;
            if (i > 0) {
                double dd = Math.max(Math.abs(lat - plat), Math.abs(lon - plon));
                sub = Math.min(40, Math.max(1, (int) Math.ceil(dd / 1.0)));
            }
            for (int k = 1; k <= sub; k++) {
                double f = (double) k / sub, la = i == 0 ? lat : plat + (lat - plat) * f, lo = i == 0 ? lon : plon + (lon - plon) * f;
                double[] p = azProject(la, lo, cx, cy, R);
                double x = p[0], y = p[1];
                if (p[2] > maxD) { double sc = maxD / p[2]; x = cx + (x - cx) * sc; y = cy + (y - cy) * sc; }
                if (i == 0) g.moveTo(x, y); else g.lineTo(x, y);
            }
            plat = lat; plon = lon;
        }
        if (close) g.closePath();
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
    /** Band buttons on the left panel: {band number, colour}. */
    private static final String[][] MAP_BANDS = {
        {"160","#ec5b57"},{"80","#ec5b57"},{"40","#eebb58"},{"30","#67d283"},{"20","#26c0cf"},
        {"17","#26c0cf"},{"15","#b191ea"},{"12","#b191ea"},{"10","#ef7d83"},
    };
    /** Overlay toggle labels, in the order of {@code overlayFlag(i)}. */
    private static final Object[][] OVERLAYS = {
        {"Gray line / sun"},{"Basemap"},{"Beam heading"},{"Range rings"},{"Callsigns"},{"Needed only"},{"Starburst on new spots"},
        {"Fade old callsigns", "A callsign fades from the map after a while - sooner when the map is busy (about 75 seconds with 80 or more spots showing), later when it is quiet (up to 8 minutes with 10 or fewer). The dot stays, and the selected spot always keeps its name."},
    };
    private static double Q_LAT = 0, Q_LON = 0;  // station QTH (from HubConfig)
    private static String myCall = "";
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
    private static int mapRegion = Math.max(0, Math.min(REGION_NAMES.length - 1, com.ars.fx.data.HubConfig.getInt("map.region", 0)));
    // Live equirectangular viewport the rect projection + overlays map into.
    private static double vMinLon = -180, vMaxLon = 180, vMinLat = -90, vMaxLat = 90;
    private static int appliedRegion = -1;
    private static double lastRMX, lastRMY, lastRW = 1, lastRH = 1;     // the map rectangle of the last paint, for zoom and pan
    private static Region rectMap() {
        Canvas cv = new Canvas();
        StackPane area = new StackPane(cv); area.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(area, Priority.ALWAYS);
        area.setMinSize(0, 0);

        // The map window is {vMinLat..vMaxLat} x {vMinLon..vMaxLon}: a region chip sets it, the wheel zooms about the
        // pointer, a drag pans, a double click goes back to the region. Equirectangular has equal px/degree on both axes,
        // so the rectangle's aspect is the window's lon-span : lat-span.
        Runnable redraw = () -> {
            double aw = area.getWidth(), ah = area.getHeight();
            if (aw <= 1 || ah <= 1) return;
            if (appliedRegion != mapRegion) {
                double[] rg = REGIONS[mapRegion];
                vMinLat = rg[0]; vMaxLat = rg[1]; vMinLon = rg[2]; vMaxLon = rg[3];
                appliedRegion = mapRegion;
            }
            double aspect = (vMaxLon - vMinLon) / (vMaxLat - vMinLat);
            double imgW = Math.min(aw, ah * aspect), imgH = imgW / aspect;
            cv.setWidth(aw); cv.setHeight(ah);
            paintRect(cv.getGraphicsContext2D(), aw, ah, imgW, imgH);
        };
        area.widthProperty().addListener((o, a, b) -> redraw.run());
        area.heightProperty().addListener((o, a, b) -> redraw.run());

        double[] press = new double[3];                          // x, y, dragged
        area.setOnMousePressed(e -> { press[0] = e.getX(); press[1] = e.getY(); press[2] = 0; });
        area.setOnMouseDragged(e -> {
            double dx = e.getX() - press[0], dy = e.getY() - press[1];
            if (press[2] == 0 && Math.hypot(dx, dy) < 4) return;
            press[2] = 1; press[0] = e.getX(); press[1] = e.getY();
            double lonSpan = vMaxLon - vMinLon, latSpan = vMaxLat - vMinLat;
            setWindow(vMinLon - dx / lastRW * lonSpan, vMinLat + dy / lastRH * latSpan, lonSpan, latSpan);
            currentMapRepaint.run();
        });
        area.setOnScroll(e -> {
            if (e.getDeltaY() == 0) return;
            double lonSpan = vMaxLon - vMinLon, latSpan = vMaxLat - vMinLat;
            double fx = Math.max(0, Math.min(1, (e.getX() - lastRMX) / lastRW)), fy = Math.max(0, Math.min(1, (e.getY() - lastRMY) / lastRH));
            double lonAt = vMinLon + fx * lonSpan, latAt = vMaxLat - fy * latSpan;
            double f = Math.exp(e.getDeltaY() / 300.0);           // wheel up zooms in
            double ns = Math.max(Math.min(1.0 / f, Math.min(360 / lonSpan, 180 / latSpan)), 2.0 / latSpan);   // spans never exceed the world nor go under 2 degrees
            double nLon = lonSpan * ns, nLat = latSpan * ns;
            setWindow(lonAt - fx * nLon, latAt - (1 - fy) * nLat, nLon, nLat);
            currentMapRepaint.run();
        });
        area.setOnMouseClicked(e -> {
            if (press[2] == 1) { press[2] = 0; return; }          // the end of a drag is not a click
            if (e.getClickCount() == 2) { appliedRegion = -1; currentMapRepaint.run(); return; }
            handleMapClick(area, e.getX(), e.getY());
        });
        currentMapRepaint = redraw;
        redraw.run();
        return area;
    }
    /** Set the map window from its south-west corner and spans, kept inside the world. */
    private static void setWindow(double minLon, double minLat, double lonSpan, double latSpan) {
        minLon = Math.max(-180, Math.min(180 - lonSpan, minLon));
        minLat = Math.max(-90, Math.min(90 - latSpan, minLat));
        vMinLon = minLon; vMaxLon = minLon + lonSpan; vMinLat = minLat; vMaxLat = minLat + latSpan;
    }

    /** Land, coast and country borders on the flat map. Rings off the window are skipped, and points under a pixel apart are dropped. */
    private static void drawVectorRect(GraphicsContext g, double rmx, double rmy, double rw, double rh) {
        g.setFill(P.ocean()); g.fillRect(rmx, rmy, rw, rh);
        double pxPerDeg = rw / (vMaxLon - vMinLon);
        g.setFill(P.land()); g.setStroke(P.coast()); g.setLineWidth(0.8);
        for (com.ars.fx.data.VectorBasemap.Ring r : com.ars.fx.data.VectorBasemap.land().rings()) {
            if (!ringVisible(r)) continue;
            boolean ice = isIce(r);
            g.setFill(ice ? P.ice() : P.land()); g.setStroke(ice ? P.iceCoast() : P.coast());
            rectPath(g, r.ll(), rmx, rmy, rw, rh, pxPerDeg, true);
            g.fill(); g.stroke();
        }
        g.setStroke(P.border()); g.setLineWidth(0.7);
        for (com.ars.fx.data.VectorBasemap.Ring r : com.ars.fx.data.VectorBasemap.borders().rings()) {
            if (!ringVisible(r)) continue;
            rectPath(g, r.ll(), rmx, rmy, rw, rh, pxPerDeg, false);
            g.stroke();
        }
        if (vMaxLon - vMinLon <= 100) {                           // state and province lines only once zoomed in
            g.setStroke(P.state()); g.setLineWidth(0.6); g.setLineDashes(4, 3);
            for (com.ars.fx.data.VectorBasemap.Ring r : com.ars.fx.data.VectorBasemap.states().rings()) {
                if (!ringVisible(r)) continue;
                rectPath(g, r.ll(), rmx, rmy, rw, rh, pxPerDeg, false);
                g.stroke();
            }
            g.setLineDashes(null);
        }
    }
    /** Country names, most important first; a name that would overlap one already placed is skipped. */
    private static void drawCountryLabels(GraphicsContext g, double rmx, double rmy, double rw, double rh) {
        double z = com.ars.fx.data.MapLabels.zoomOf(vMaxLon - vMinLon);
        g.setFont(Font.font("IBM Plex Sans", FontWeight.NORMAL, 11)); g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(javafx.geometry.VPos.CENTER);
        g.setLineWidth(3); g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        java.util.List<double[]> placed = new java.util.ArrayList<>();
        for (com.ars.fx.data.MapLabels.Label l : com.ars.fx.data.MapLabels.countries()) {
            if (l.minZoom() > z + 0.3) continue;
            if (l.lon() < vMinLon || l.lon() > vMaxLon || l.lat() < vMinLat || l.lat() > vMaxLat) continue;
            double x = lon2x(l.lon(), rmx, rw), y = lat2y(l.lat(), rmy, rh), hw = l.name().length() * 3.3 + 3, hh = 8;
            boolean clash = false;
            for (double[] r : placed) if (Math.abs(x - r[0]) < hw + r[2] && Math.abs(y - r[1]) < hh + r[3]) { clash = true; break; }
            if (clash) continue;
            placed.add(new double[]{x, y, hw, hh});
            g.setStroke(P.halo()); g.strokeText(l.name(), x, y);
            g.setFill(P.text().deriveColor(0, 1, 1, 0.85)); g.fillText(l.name(), x, y);
        }
        g.setTextBaseline(javafx.geometry.VPos.BASELINE);
    }
    private static boolean ringVisible(com.ars.fx.data.VectorBasemap.Ring r) {
        return r.maxLon() >= vMinLon && r.minLon() <= vMaxLon && r.maxLat() >= vMinLat && r.minLat() <= vMaxLat;
    }
    private static void rectPath(GraphicsContext g, double[] ll, double rmx, double rmy, double rw, double rh, double pxPerDeg, boolean close) {
        g.beginPath();
        double minStep = 0.6 / pxPerDeg, lx = 0, ly = 0;
        int n = ll.length / 2;
        for (int i = 0; i < n; i++) {
            double lo = ll[i * 2], la = ll[i * 2 + 1];
            if (i > 0 && i < n - 1 && Math.abs(lo - lx) < minStep && Math.abs(la - ly) < minStep) continue;
            double x = lon2x(lo, rmx, rw), y = lat2y(la, rmy, rh);
            if (i == 0) g.moveTo(x, y); else g.lineTo(x, y);
            lx = lo; ly = la;
        }
        if (close) g.closePath();
    }

    /** The beam as great-circle paths out to the azimuthal map's 15,000 km edge, so the two views agree. */
    private static void drawRectBeam(GraphicsContext g, double rmx, double rmy, double rw, double rh, double heading) {
        double reach = 15000, spread = 11;
        int steps = 40;
        double[] left = rectPath(heading - spread, reach, steps), right = rectPath(heading + spread, reach, steps), mid = rectPath(heading, reach, steps);
        int n = steps + 1;
        double[] xs = new double[n * 2], ys = new double[n * 2];
        for (int i = 0; i < n; i++) {
            xs[i] = lon2x(left[i * 2 + 1], rmx, rw); ys[i] = lat2y(left[i * 2], rmy, rh);
            int back = n * 2 - 1 - i;
            xs[back] = lon2x(right[i * 2 + 1], rmx, rw); ys[back] = lat2y(right[i * 2], rmy, rh);
        }
        double[] mx = new double[n], my = new double[n];
        for (int i = 0; i < n; i++) { mx[i] = lon2x(mid[i * 2 + 1], rmx, rw); my[i] = lat2y(mid[i * 2], rmy, rh); }
        double world = rw * 360 / (vMaxLon - vMinLon);        // the map may be zoomed to a region
        for (double wrap : new double[]{-world, 0, world}) {  // a path past the date line carries on from the other edge
            double[] sx = new double[xs.length], sm = new double[n];
            for (int i = 0; i < sx.length; i++) sx[i] = xs[i] + wrap;
            for (int i = 0; i < n; i++) sm[i] = mx[i] + wrap;
            g.setFill(Color.web("#31bdda", 0.22)); g.fillPolygon(sx, ys, sx.length);
            g.setStroke(Color.web("#31bdda", 0.8)); g.setLineWidth(1.4); g.strokePolyline(sm, my, n);
        }
    }
    /** {lat, lon, lat, lon, ...} along a bearing from the station, longitude unbroken across the date line. */
    private static double[] rectPath(double bearing, double km, int steps) {
        double[] out = new double[(steps + 1) * 2];
        double prev = Q_LON;
        for (int i = 0; i <= steps; i++) {
            double[] p = dest(Q_LAT, Q_LON, bearing, km * i / steps);
            prev += (((p[1] - prev) % 360 + 540) % 360) - 180;
            out[i * 2] = p[0]; out[i * 2 + 1] = prev;
        }
        return out;
    }

    /** Draw the equirectangular overlay (graticule, terminator, spots, QTH) for the given area + centred image rect. */
    private static void paintRect(GraphicsContext g, double aw, double ah, double imgW, double imgH) {
        double RMX = (aw - imgW) / 2.0, RMY = (ah - imgH) / 2.0, RW = imgW, RH = imgH;
        loadPalette();
        lastRMX = RMX; lastRMY = RMY; lastRW = RW; lastRH = RH;
        g.clearRect(0, 0, aw, ah);
        g.save();
        g.beginPath(); g.rect(RMX, RMY, RW, RH); g.closePath(); g.clip();
        if (ovBasemap) drawVectorRect(g, RMX, RMY, RW, RH);
        g.restore();
        g.setStroke(Color.web("#28313d")); g.setLineWidth(1); g.strokeRect(RMX, RMY, RW, RH);

        // clip every overlay to the map rect so a zoomed region doesn't spill markers/labels
        g.save();
        g.beginPath(); g.rect(RMX, RMY, RW, RH); g.closePath(); g.clip();

        for (int lon = -150; lon <= 150; lon += 30) {
            double x = lon2x(lon, RMX, RW); boolean prime = lon == 0;
            g.setStroke(ovBasemap ? P.line().deriveColor(0, 1, 1, prime ? 1.2 : 0.6) : Color.web("#28313d", prime ? 0.9 : 0.45)); g.setLineWidth(prime ? 1.1 : 0.6);
            g.strokeLine(x, RMY, x, RMY + RH);
        }
        for (int lat = -60; lat <= 60; lat += 30) {
            double y = lat2y(lat, RMY, RH); boolean eq = lat == 0;
            g.setStroke(ovBasemap ? P.line().deriveColor(0, 1, 1, eq ? 1.2 : 0.6) : Color.web("#28313d", eq ? 0.9 : 0.45)); g.setLineWidth(eq ? 1.1 : 0.6);
            g.strokeLine(RMX, y, RMX + RW, y);
        }
        if (ovGrayline) drawTerminator(g, RMX, RMY, RW, RH);
        if (ovBeam && !Double.isNaN(beamDeg())) drawRectBeam(g, RMX, RMY, RW, RH, beamDeg());

        if (vMaxLon - vMinLon > 200) {                          // the whole world: the continents' names; zoomed in, the countries' names
            g.setFill(ovBasemap ? P.text().deriveColor(0, 1, 1, 0.65) : Color.web("#cdd6e0", 0.65)); g.setFont(Font.font("IBM Plex Sans", FontWeight.NORMAL, 12)); g.setTextAlign(TextAlignment.CENTER);
            double[][] regions = {{52,13},{39,-98},{-14,-52},{8,20},{55,40},{36,138},{-25,134}};
            String[] rn = {"EUROPE","N. AMERICA","S. AMERICA","AFRICA","RUSSIA","ASIA","OCEANIA"};
            for (int i = 0; i < rn.length; i++) g.fillText(rn[i], lon2x(regions[i][1], RMX, RW), lat2y(regions[i][0], RMY, RH));
        } else if (ovBasemap) drawCountryLabels(g, RMX, RMY, RW, RH);

        double spotA = dimSpots() ? 0.28 : 1.0;
        paintedDots.clear();
        for (MapSpot s : liveSpots()) {
            if (spotHidden(s.call())) continue;
            double x = lon2x(s.lon(), RMX, RW), y = lat2y(s.lat(), RMY, RH);
            paintedDots.add(new Dot(x, y, s));
            g.setStroke(Color.web("#0b0f14", 0.8 * spotA)); g.setLineWidth(2); g.strokeOval(x - 4.5, y - 4.5, 9, 9);
            spotDot(g, s, x, y, spotA);
        }

        double qx = lon2x(Q_LON, RMX, RW), qy = lat2y(Q_LAT, RMY, RH);
        g.setStroke(Color.web("#31bdda", 0.5)); g.setLineWidth(1.3); g.strokeOval(qx - 8, qy - 8, 16, 16);
        g.setFill(Color.web("#31bdda")); g.fillOval(qx - 4.5, qy - 4.5, 9, 9);
        g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12)); g.setTextAlign(TextAlignment.CENTER);
        g.fillText(myCall, qx, qy - 13);

        // NCDXF beacon + "who's hearing me" overlays (equirectangular)
        Proj rect = (lat, lon) -> new double[]{lon2x(lon, RMX, RW), lat2y(lat, RMY, RH)};
        drawSpotPaths(g, rect);
        drawHeardBy(g, rect);
        drawBeacons(g, rect);
        drawTracks(g, rect);
        if (ovCallsigns) drawSpotLabels(g, spotA, aw, ah, qx, qy);
        kickBursts();
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
    private static void drawTerminator(GraphicsContext g, double rmx, double rmy, double rw, double rh) {
        double[] ss = com.ars.fx.data.SolarPosition.subSolar(); double decl = ss[0], ssLon = ss[1];
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
        g.setFill(ovBasemap ? P.night() : Color.web("#04060a", 0.5));
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
        drawSun(g, sx, sy);
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
            Label e = lbl(ccc.isConnected() ? "Connected — no spots match the filters." : com.ars.fx.shell.FeedUi.emptyList(ccc, "cluster"), "jm-spot-meta");
            e.setWrapText(true);
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
            Node pc = com.ars.fx.data.PathNow.shown() ? com.ars.fx.shell.PathChip.of(com.ars.fx.data.PathNow.forPoint(s.lat(), s.lon(), s.freqHz())) : null;
            if (pc != null) row.getChildren().add(pc);
            row.setStyle("-fx-cursor:hand;");
            com.ars.fx.shell.Tips.tip(row, "Click to use this spot — fills the log entry, tunes the rig, and offers to turn the beam");
            row.setOnMouseClicked(e -> selectSpot(row, s));
            com.ars.fx.shell.Fx.fresh(row, s.arrivalMs());          // a new spot slides in and glows
            spots.getChildren().add(row);
        }
    }
    private static Region spotsDrawer;                    // its summary follows the live count
    private static Node[] railDrawers(VBox spots) {
        com.ars.fx.data.ClusterClient cc = com.ars.fx.data.ClusterClient.getInstance();
        Label connect = lbl(com.ars.fx.shell.FeedUi.action(cc), "jl-logbtn"); connect.setStyle("-fx-cursor:hand;-fx-padding:6 14;");
        connect.setOnMouseClicked(e -> { com.ars.fx.shell.FeedUi.toggle(cc); connect.setText(com.ars.fx.shell.FeedUi.action(cc)); });
        Timeline connectTick = new Timeline(new KeyFrame(Duration.millis(500), e -> connect.setText(com.ars.fx.shell.FeedUi.action(cc))));
        connectTick.setCycleCount(Timeline.INDEFINITE); connectTick.play();
        HBox connectRow = new HBox(connect); connectRow.setAlignment(Pos.CENTER_LEFT); connectRow.setPadding(new Insets(0, 0, 8, 0));
        ScrollPane spotScroll = new ScrollPane(spots); spotScroll.setFitToWidth(true);
        spotScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); spotScroll.setPrefViewportHeight(300);
        spotScroll.setStyle("-fx-background-color:transparent;");

        List<Node> rail = new ArrayList<>();
        rail.add(MapQsoCard.drawerSlot());                // New QSO, when pinned
        rail.addAll(Shell.leadDrawers());    // Rig control, Rotor control
        spotsDrawer = Shell.drawer("DX Spots","map","map", liveSpots().size() + " spots", true, new VBox(connectRow, spotScroll));
        rail.add(spotsDrawer);
        rail.add(Shell.drawer("DX station info","accent","hub","", false, DxInfoCard.drawerBody()));
        rail.addAll(Shell.instruments(50));               // propagation, solar, band conditions, weather
        return rail.toArray(new Node[0]);
    }
    private static Node hcall(String call, String need){
        HBox h = new HBox(7, lbl(call, "jm-spot-call")); h.setAlignment(Pos.CENTER_LEFT);
        if (need.equals("y")) h.getChildren().add(lbl("NEED","jhub-need"));
        return h;
    }
}
