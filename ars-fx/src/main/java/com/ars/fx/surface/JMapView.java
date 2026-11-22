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

import static com.ars.fx.surface.JMapControls.*;
import static com.ars.fx.surface.JMapCanvases.*;
import static com.ars.fx.shell.Shell.lbl;

/** J-Map azimuthal propagation map (handoff shot 03), mock data. */
public final class JMapView {
    private JMapView() {}

    // ── NCDXF/IARU International Beacon Project ────────────────────────────────
    // 18 beacons in transmit-slot order; each transmits 10 s per band on a 3-min cycle.
    static final String[][] BEACONS = {
            {"4U1UN","40.75","-73.97"}, {"VE8AT","79.99","-85.95"}, {"W6WX","37.37","-121.92"},
            {"KH6RS","20.79","-156.46"}, {"ZL6B","-41.05","175.46"}, {"VK6RBP","-32.10","116.05"},
            {"JA2IGY","34.46","136.79"}, {"RR9O","54.98","82.89"}, {"VR2B","22.27","114.16"},
            {"4S7B","6.91","79.87"}, {"ZS6DN","-25.91","28.20"}, {"5Z4B","-1.38","36.68"},
            {"4X6TU","32.11","34.80"}, {"OH2B","60.19","24.96"}, {"CS3B","32.65","-16.91"},
            {"LU4AA","-34.61","-58.37"}, {"OA4B","-12.07","-77.08"}, {"YV5B","10.42","-66.98"}};
    static final String[] BEACON_BANDS = {"20m","17m","15m","12m","10m"};
    /** The one frequency each band's beacons share (kHz as MHz text); the 18 stations take turns on it, so a rig tuned here hears them all. */
    static final long[] BEACON_HZ = {14_100_000L, 18_110_000L, 21_150_000L, 24_930_000L, 28_200_000L};
    static boolean showBeacons = false;
    static int beaconBand = 0;
    static Label beaconInfo;                         // live "QSY to ..." line in the drawer
    static HBox beaconSeg;                           // the band buttons, so a hopping watch can move the highlight
    static Label beaconHeardInfo;                    // "7 of 18 heard on 20m"
    record BeaconDot(double x, double y, String call) {}
    static final java.util.List<BeaconDot> paintedBeacons = new java.util.ArrayList<>();
    static boolean beaconHop() { return com.ars.fx.data.HubConfig.getBool("map.beaconHop", false); }
    static boolean beaconAuto() { return com.ars.fx.data.HubConfig.getBool("map.beaconAuto", false); }
    static void selectBeaconSeg(int idx) {
        if (beaconSeg == null) return;
        for (int i = 0; i < beaconSeg.getChildren().size(); i++) {
            Node n = beaconSeg.getChildren().get(i);
            if (i == idx) { if (!n.getStyleClass().contains("on")) n.getStyleClass().add("on"); } else n.getStyleClass().remove("on");
        }
    }
    static void refreshBeaconHeard() {
        if (beaconHeardInfo != null) beaconHeardInfo.setText(com.ars.fx.data.BeaconHeard.summary(BEACON_BANDS[beaconBand], BEACONS.length)
                + "   \u00b7   " + com.ars.fx.data.BeaconHeard.allBands(java.util.Arrays.asList(BEACON_BANDS), BEACONS.length));
    }
    /** Once a second while the cycle is on: hop to the next band every cycle if asked, and mark the beacon on the air if the S-meter holds a signal on its frequency. */
    static void beaconWatch() {
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
    static Runnable currentMapRepaint;
    static boolean beaconTicker;
    /** Beacon index transmitting on band b right now (slot = UTC%180 /10; band offsets by one slot each). */
    static int activeBeacon(int band) {
        int slot = (int) ((java.time.Instant.now().getEpochSecond() % 180) / 10);
        return ((slot - band) % 18 + 18) % 18;
    }
    static String mhz(long hz) { return String.format(java.util.Locale.ROOT, "%.3f", hz / 1e6); }

    /** What to tell the operator: where to tune, who is on now and who is next. */
    static String beaconLine() {
        long now = java.time.Instant.now().getEpochSecond();
        int a = activeBeacon(beaconBand);
        // the station on this band moves one place round the list every 10 s
        int nxt = activeBeaconAt(beaconBand, now + 10);
        return "QSY to " + mhz(BEACON_HZ[beaconBand]) + " MHz CW  \u00b7  now " + BEACONS[a][0] + "  \u00b7  next " + BEACONS[nxt][0] + " in " + (10 - now % 10) + " s";
    }
    static int activeBeaconAt(int band, long epochSec) {
        int slot = (int) ((epochSec % 180) / 10);
        return ((slot - band) % 18 + 18) % 18;
    }

    /** Auto QSY: put the rig on the selected band's beacon frequency in CW. Does nothing without a connected rig, and never while it transmits. */
    static void qsyToBeacon() {
        if (!showBeacons || !com.ars.fx.data.HubConfig.getBool("map.beaconQsy", false)) return;
        com.ars.fx.data.RigClient rig = com.ars.fx.data.RigClient.getInstance();
        com.ars.fx.data.RigClient.State st = rig.last();
        if (!st.connected() || st.ptt()) return;
        rig.setMode("CW");
        rig.setFreqHz(BEACON_HZ[beaconBand]);
    }

    static void repaintMap() { paintedBeam = Math.round(beamDeg()); if (currentMapRepaint != null) currentMapRepaint.run(); }
    /** Dim the regular DX spots while a propagation overlay (beacons / who's-hearing-me) is active. */
    static boolean dimSpots() { return showBeacons || showHeardBy; }
    /** Draw each shown cluster spot's path, from the station spotted to the station that spotted it. */
    static java.util.List<MapSpot> frameSpots;               // the spots the globe is painting this frame, so the layers share one lookup
    static boolean showSpotPaths = false;
    static long spotPathsSince = 0;                         // Clear sets this to now: only spots that arrive after it get a line
    static final long SPOT_PATH_LIFE_MS = 10 * 60_000L;     // a spot's line fades out over ten minutes, then goes

    /** The antenna's heading, or NaN with no rotor: the map never shows a beam the antenna isn't on. */
    static double beamDeg() { return com.ars.fx.data.RotorClient.getInstance().headingDeg(); }
    static String beamText() { double b = beamDeg(); return Double.isNaN(b) ? "—" : String.format("%03.0f°", b); }
    static long paintedBeam = Long.MIN_VALUE;     // what the map last drew, so a turning rotor repaints it

    // ── clickable map dots: each paint records the on-screen spot positions so a
    //    click can be hit-tested back to the spot that was drawn there. ──────────
    record Dot(double x, double y, MapSpot s) {}
    static final java.util.List<Dot> paintedDots = new java.util.ArrayList<>();
    static void handleMapClick(Node source, double x, double y) {
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
    static void selectSpot(Node source, MapSpot s) {
        selectSpotCall(s.call());
        DxInfoCard.show(source, s, spotWorked(s));
        SpotAction.onSpot(source, s.call(), s.freqHz(), s.mode(), s.band(), s.lat(), s.lon());
    }

    // ── overlay toggles (left panel) ──────────────────────────────────────────
    static boolean ovGrayline = true, ovBasemap = true, ovBeam = true, ovRings = true, ovCallsigns = true, ovNeededOnly = false, ovBurst = true, ovFade = true, ovCountries = true, ovSections = false, ovStates = false;
    // J-Map carries a 270px controls panel on top of the dock+rail, so a narrow / high-DPI window
    // pushes the side edges off screen. Below this logical width auto-collapse the controls to the
    // icon strip (same idea as J-Sat's satellite list).
    static final double NARROW_W = 1240;
    static boolean narrow() { return mapFrame != null && mapFrame.getWidth() > 1 && mapFrame.getWidth() < NARROW_W; }
    static boolean overlayFlag(int i) {
        return switch (i) { case 0 -> ovGrayline; case 1 -> ovBasemap; case 2 -> ovBeam; case 3 -> ovRings; case 4 -> ovCallsigns; case 5 -> ovNeededOnly; case 6 -> ovBurst; case 7 -> ovFade; case 9 -> ovSections; case 10 -> ovStates; default -> ovCountries; };
    }
    static void applyOverlay(int i, boolean v) {
        switch (i) { case 0 -> ovGrayline = v; case 1 -> ovBasemap = v; case 2 -> ovBeam = v; case 3 -> ovRings = v; case 4 -> ovCallsigns = v; case 5 -> ovNeededOnly = v; case 6 -> ovBurst = v; case 7 -> ovFade = v; case 9 -> ovSections = v; case 10 -> ovStates = v; default -> ovCountries = v; }
    }
    static void setOverlayFlag(int i, boolean v) {
        applyOverlay(i, v);
        com.ars.fx.data.HubConfig.setBool("map.ov." + i, v);            // kept for next time
    }
    static { for (int i = 0; i < 11; i++) applyOverlay(i, com.ars.fx.data.HubConfig.getBool("map.ov." + i, overlayFlag(i))); }
    static java.util.Set<String> workedCallsCache = java.util.Set.of();
    static long workedCallsAt = 0;
    static java.util.Set<String> workedCalls() {
        long n = System.currentTimeMillis();
        if (n - workedCallsAt > 5000) { workedCallsCache = new java.util.HashSet<>(com.ars.fx.data.JLogDb.distinctCalls()); workedCallsAt = n; }
        return workedCallsCache;
    }
    private static final java.util.Map<String, Boolean> contestDupeMemo = new java.util.concurrent.ConcurrentHashMap<>();
    private static long contestDupeAt = 0;
    /**
     * Is this spot already worked? With a contest open: a dupe by that contest's own rules (same call on the same band and mode, or whatever the contest
     * says), as the dashboard's cluster list judges it; before, J-Map asked the normal log whether the call had ever been worked, so a station worked in
     * the contest still showed as needed. With no contest: the call is in the normal log. The contest answers are kept for 5 seconds (the repaint asks
     * for every spot), and re-read from the database because an undocked J-Map is another process from the one logging.
     */
    static boolean spotWorked(MapSpot s) {
        String call = s.call().toUpperCase();
        if (com.ars.fx.data.ContestState.activeId().isBlank()) return workedCalls().contains(call);
        com.ars.fx.data.ContestState.ensureLoaded();
        com.jlog.plugin.ContestPlugin plugin = com.ars.fx.data.ContestState.plugin();
        if (plugin == null) return workedCalls().contains(call);
        long n = System.currentTimeMillis();
        if (n - contestDupeAt > 5000) { contestDupeMemo.clear(); contestDupeAt = n; }
        return contestDupeMemo.computeIfAbsent(call + "|" + s.band() + "|" + s.mode(), k -> {
            try {
                com.jlog.model.QsoRecord cand = com.ars.fx.data.MultCheck.candidateOf(plugin, call, s.band(), s.mode());
                return com.jlog.scoring.ContestScorer.isDupe(plugin, cand, com.jlog.db.ContestQsoDao.getInstance().findByCallsign(plugin.getContestId(), call));
            } catch (Exception e) { return false; }
        });
    }
    /** "Needed only" — hide spots that are already worked. */
    static boolean spotHidden(MapSpot s) { return ovNeededOnly && spotWorked(s); }

    // ---- live DX-cluster spots on the map ---------------------------------
    static int spotMode = Math.max(0, Math.min(3, com.ars.fx.data.HubConfig.getInt("map.spotMode", 0)));   // 0 All · 1 CW · 2 Phone · 3 Digi
    static final java.util.Set<String> spotBands = new java.util.HashSet<>();   // empty = all bands
    static { for (String b : com.ars.fx.data.HubConfig.get("map.bands", "").split(",")) if (!b.isBlank()) spotBands.add(b.trim()); }
    static int spotAgeMax = Math.max(1, Math.min(60, com.ars.fx.data.HubConfig.getInt("map.spotAge", 20)));   // minutes
    static Runnable currentSpotsRefresh;

    /**
     * A spot's dot: a soft glow in its band's colour, fading as the spot ages so what is current stands out, and a ring on the very newest. With Path now
     * on, a band our estimate says is probably closed fades further, and one it says is likely open gets a green ring.
     */
    static void spotDot(javafx.scene.canvas.GraphicsContext g, MapSpot s, double x, double y, double spotA) {
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
    static void drawSpotLabels(GraphicsContext g, double spotA, double w, double h, double meX, double meY) {
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
    static boolean labelsFading = false;
    static final Text MEASURE = new Text();
    static double textWidth(String str, Font f) { MEASURE.setFont(f); MEASURE.setText(str); return MEASURE.getLayoutBounds().getWidth(); }

    /** A positioned cluster spot (geo-located via {@link com.ars.fx.data.Geo}). */
    public record MapSpot(String call, double lat, double lon, String band, String mode, long arrivalMs, long freqHz, Color color,
                          String spotter, String comment) {}

    /** Geo-locate + filter the given raw cluster spots (skips calls with no known location). */
    public static java.util.List<MapSpot> spotsFrom(java.util.List<com.ars.fx.data.ClusterClient.Spot> raw) {
        java.util.List<MapSpot> out = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();
        for (com.ars.fx.data.ClusterClient.Spot s : raw) {
            double[] ll = com.ars.fx.data.Geo.near(s.call());
            if (ll == null) continue;
            if (!modePass(s.mode()) || !bandPass(s.band()) || (now - s.arrivalMs()) / 60000 > spotAgeMax) continue;
            out.add(new MapSpot(s.call(), ll[0], ll[1], s.band(), s.mode(), s.arrivalMs(), s.freqHz(), bandColor(s.band()), s.de(), s.comment()));
        }
        return out;
    }
    static java.util.List<MapSpot> liveSpots() { return spotsFrom(com.ars.fx.data.ClusterClient.getInstance().spots()); }
    static boolean modePass(String m) { return modePass(spotMode, m); }
    /** @param mode 0 All · 1 CW · 2 Phone · 3 Digi (everything else) */
    static boolean modePass(int mode, String m) {
        if (mode == 0) return true;
        String u = m == null ? "" : m.toUpperCase();
        boolean cw = u.equals("CW");
        boolean phone = u.equals("SSB") || u.equals("USB") || u.equals("LSB") || u.equals("FM") || u.equals("AM") || u.contains("PHONE");
        return mode == 1 ? cw : mode == 2 ? phone : (!cw && !phone);
    }
    static boolean bandPass(String b) { return spotBands.isEmpty() || spotBands.contains(b); }
    static void refreshSpots() { repaintMap(); if (currentSpotsRefresh != null) currentSpotsRefresh.run(); }

    static void startBeaconTicker() {
        if (beaconTicker) return;
        beaconTicker = true;
        // 1 Hz tick keeps the top bar live. The map itself is costly to repaint and barely changes: beacons step every
        // 10 s, the sun about a quarter of a degree a minute. New spots repaint it on their own.
        com.ars.fx.shell.Fx.everySecond(() -> {
            updateMapTopbar();                                 // ticking clock + live SPOTS/SHOWN/GRAY-LINE/BEAM
            if (beaconInfo != null) beaconInfo.setText(showBeacons ? beaconLine() : "");
            if (showBeacons) beaconWatch();
            long second = java.time.Instant.now().getEpochSecond();
            boolean beamMoved = Math.round(beamDeg()) != paintedBeam;
            if (beamMoved || (showSpotPaths && second % 15 == 0) || (showBeacons ? second % 10 == 0 : ovGrayline && second % 60 == 0) || (labelsFading && ovCallsigns && ovFade && second % 10 == 0)) repaintMap();
        });
    }
    /** Projects (lat,lon) to canvas {x,y}, or null when off-map (azimuthal beyond the disk). */
    interface Proj { double[] at(double lat, double lon); }
    static void drawBeacons(GraphicsContext g, Proj proj) {
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

    static void drawTracks(GraphicsContext g, Proj proj) {
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

    static final java.util.Map<String, double[]> SPOTTER_AT = new java.util.HashMap<>();   // the same few skimmers spot all day; the lookup scans a prefix table
    static {   // a station's real place comes from the callbook a moment after it is first seen: forget the call-area guesses and repaint
        com.ars.fx.data.PlaceLookup.onChange = () -> javafx.application.Platform.runLater(() -> { SPOTTER_AT.clear(); repaintMap(); });
    }
    static final double[] UNKNOWN = new double[0];
    static double[] spotterAt(String call) {
        if (call == null) return null;
        if (SPOTTER_AT.size() > 4000) SPOTTER_AT.clear();
        double[] ll = SPOTTER_AT.computeIfAbsent(call, c -> { double[] g = com.ars.fx.data.Geo.near(c); return g == null ? UNKNOWN : g; });
        return ll == UNKNOWN ? null : ll;
    }

    /** The spot last clicked, by call: all its paths, one for each station that spotted it, are drawn in red on top of the rest. */
    static String selectedSpotCall;
    static void selectSpotCall(String call) {
        String c = call == null ? null : call.toUpperCase();
        if (java.util.Objects.equals(c, selectedSpotCall)) return;
        selectedSpotCall = c; repaintMap();
    }

    /**
     * One thin line per shown spot, spotted station to spotter, in the band's colour, with a small dot at the spotter's end. The clicked spot's lines are
     * red, heavier and on top, whether or not the Spot paths switch is on.
     */
    static void drawSpotPaths(GraphicsContext g, Proj proj) {
        if (!showSpotPaths && selectedSpotCall == null) return;
        long now = System.currentTimeMillis();
        java.util.List<MapSpot> spots = frameSpots != null ? frameSpots : liveSpots();
        if (showSpotPaths) for (MapSpot s : spots) {
            if (spotHidden(s) || s.arrivalMs() < spotPathsSince || s.call().equalsIgnoreCase(selectedSpotCall)) continue;
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
                if (!s.call().equalsIgnoreCase(selectedSpotCall) || spotHidden(s)) continue;
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
    static boolean showHeardBy = false;
    /** Session QSOs: an arc from the station to each contact logged since ARS Suite started (data/SessionQsos); a saved on/off switch. */
    static boolean showSession = com.ars.fx.data.HubConfig.getBool("map.sessionQsos", false);
    static Label sessionInfo;                          // "3 QSOs this session" line in the controls
    static String sessionText() {
        int n = com.ars.fx.data.SessionQsos.count(), placed = com.ars.fx.data.SessionQsos.placedCount();
        return n == 0 ? "No QSOs logged yet this session." : n + (n == 1 ? " QSO" : " QSOs") + " this session" + (placed < n ? " (" + (n - placed) + " not placed)" : "");
    }
    /** The session's arcs: station QTH to each placed contact, in the band's colour, a dot and the callsign at the far end. The newest is heavier. */
    static void drawSessionQsos(GraphicsContext g, Proj proj) {
        if (!showSession || !com.ars.fx.data.HubConfig.hasLocation()) return;
        java.util.List<com.ars.fx.data.SessionQsos.Qso> list = com.ars.fx.data.SessionQsos.all();
        double oldW = gcWidth;
        g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 10)); g.setTextAlign(TextAlignment.LEFT);
        for (int i = 0; i < list.size(); i++) {
            com.ars.fx.data.SessionQsos.Qso q = list.get(i);
            if (!q.placed()) continue;
            Color col = bandColor(q.band());
            gcWidth = i == list.size() - 1 ? 2.6 : 1.8;
            drawGcPath(g, proj, Q_LAT, Q_LON, q.lat(), q.lon(), Color.color(col.getRed(), col.getGreen(), col.getBlue(), 0.92));
            double[] p = proj.at(q.lat(), q.lon());
            if (p == null) continue;
            g.setStroke(Color.web("#0b0f14", 0.85)); g.setLineWidth(2); g.strokeOval(p[0] - 4, p[1] - 4, 8, 8);
            g.setFill(col); g.fillOval(p[0] - 4, p[1] - 4, 8, 8);
            g.setFill(Color.web("#0b0f14", 0.72)); g.fillRoundRect(p[0] + 5, p[1] - 16, q.call().length() * 6.2 + 8, 15, 6, 6);     // a backing, so it reads over land and sea alike
            g.setFill(Color.web("#eef3f8", 0.97)); g.fillText(q.call(), p[0] + 9, p[1] - 5);
        }
        gcWidth = oldW;
    }
    static void drawHeardBy(GraphicsContext g, Proj proj) {
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
    static String toHex(Color c) {
        return String.format("#%02x%02x%02x", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }
    static Color bandColor(String band) {
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
    static double gcJump = 350;
    static double gcWidth = 1.4;                            // line width of a path; a selected spot's paths are drawn heavier
    /** Set while the globe is painting: paths then arch off the surface instead of lying flat on it. */
    static com.ars.fx.data.GlobeProjection gcGlobe;
    /** How high the middle of a path rises on the globe, in km: a tenth of its length, up to 1,500 km. The ends stay on the ground. */
    static double gcLiftKm(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double d = 2 * Math.asin(Math.min(1, Math.sqrt(Math.pow(Math.sin((p2 - p1) / 2), 2) + Math.cos(p1) * Math.cos(p2) * Math.pow(Math.sin(Math.toRadians(lon2 - lon1) / 2), 2))));
        return Math.min(1500, 0.1 * d * 6371.0);
    }
    static void drawGcPath(GraphicsContext g, Proj proj, double lat1, double lon1, double lat2, double lon2, Color col) {
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
    static double[] gcPoint(double lat1, double lon1, double lat2, double lon2, double f) {
        double p1 = Math.toRadians(lat1), l1 = Math.toRadians(lon1), p2 = Math.toRadians(lat2), l2 = Math.toRadians(lon2);
        double d = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((p2 - p1) / 2), 2) + Math.cos(p1) * Math.cos(p2) * Math.pow(Math.sin((l2 - l1) / 2), 2)));
        if (d == 0) return new double[]{lat1, lon1};
        double A = Math.sin((1 - f) * d) / Math.sin(d), B = Math.sin(f * d) / Math.sin(d);
        double x = A * Math.cos(p1) * Math.cos(l1) + B * Math.cos(p2) * Math.cos(l2);
        double y = A * Math.cos(p1) * Math.sin(l1) + B * Math.cos(p2) * Math.sin(l2);
        double z = A * Math.sin(p1) + B * Math.sin(p2);
        return new double[]{Math.toDegrees(Math.atan2(z, Math.sqrt(x * x + y * y))), Math.toDegrees(Math.atan2(y, x))};
    }

    /** Called when the theme button is pressed: repaint the map in the new colours. */
    public static void themeChanged() { JMapCanvases.themeChanged(); }

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
        com.ars.fx.UiStyle.applyDashboard(mapFrame);          // the dashboard look (Setup > Appearance) carries over to J-Map
        // re-collapse / expand the controls when the window crosses the narrow threshold
        mapFrame.widthProperty().addListener((o, a, b) -> { if ((a.doubleValue() < NARROW_W) != (b.doubleValue() < NARROW_W)) rebuildLeft[0].run(); });
        return mapFrame;
    }

    static Region mapFrame;

    // ---- starburst: a short pop on the map where a spot has just arrived ----
    static final long BURST_MS = 2600;
    static Canvas burstCanvas;
    static javafx.animation.AnimationTimer burstTimer;

    static void setUpBursts(Canvas c) {
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
                    for (LimbDot d : limbDots) {
                        long age = now - d.s().arrivalMs();
                        if (age < 0 || age >= BURST_MS) continue;
                        any = true;
                        drawLimbBurst(g, d.x(), d.y(), d.theta(), age / (double) BURST_MS, d.s().color());
                    }
                }
                if (!any) stop();                                   // nothing new on the map: no more frames until the next spot
            }
        };
    }

    /** Called after each map paint: if a spot on the map is brand new, play its starburst. The timer runs only while there is one to play. */
    static void kickBursts() {
        if (!ovBurst || burstTimer == null || com.ars.fx.shell.Fx.reduced()) return;
        long now = System.currentTimeMillis();
        for (Dot d : paintedDots) { long age = now - d.s().arrivalMs(); if (age >= 0 && age < BURST_MS) { burstTimer.start(); return; } }
        for (LimbDot d : limbDots) { long age = now - d.s().arrivalMs(); if (age >= 0 && age < BURST_MS) { burstTimer.start(); return; } }
    }

    /** A new spot on the far side of the globe, shown as a burst on the globe's edge: {x, y} is the rim point in line with it, {@code theta} the way out from the centre. */
    record LimbDot(double x, double y, double theta, MapSpot s) {}
    static final List<LimbDot> limbDots = new ArrayList<>();

    /** Called for a spot the globe cannot see. Only brand-new ones are kept: the rim point is where the line from the globe's centre through the hidden spot's projected position meets the edge. */
    static void noteLimbSpot(com.ars.fx.data.GlobeProjection pr, MapSpot s, double w, double h) {
        long age = System.currentTimeMillis() - s.arrivalMs();
        if (age < 0 || age >= BURST_MS) return;
        com.ars.fx.data.GlobeProjection.Pt p = pr.project(s.lat(), s.lon(), 0);
        double dx = p.x() - w / 2, dy = p.y() - h / 2, rim = pr.silhouetteRadius();
        double theta = Math.hypot(dx, dy) < 1 ? -Math.PI / 2 : Math.atan2(dy, dx);              // dead behind the centre: say it is straight up
        limbDots.add(new LimbDot(w / 2 + Math.cos(theta) * rim, h / 2 + Math.sin(theta) * rim, theta, s));
    }

    /**
     * The far-side version of {@link #drawBurst}, quieter and half a burst: a glow on the globe's edge, a half ring on the outer side and five rays fanning
     * outward, as if the spot were peeking over the horizon. Same timing and the same dark line under everything.
     */
    static void drawLimbBurst(GraphicsContext g, double x, double y, double theta, double t, Color band) {
        double a = t < 0.35 ? 1.0 : Math.pow(1 - (t - 0.35) / 0.65, 1.2);
        double grow = 1 - Math.pow(1 - Math.min(1.0, t / 0.6), 2.2);
        Color col = band.interpolate(Color.WHITE, 0.15).deriveColor(0, 1.2, 1.15, a), under = Color.web("#0b0f14", 0.5 * a);
        g.setLineCap(StrokeLineCap.ROUND);
        double gr = 16 + 8 * grow;
        g.setFill(new javafx.scene.paint.RadialGradient(0, 0, x, y, gr, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, band.deriveColor(0, 1.2, 1.2, 0.5 * a)), new javafx.scene.paint.Stop(1, band.deriveColor(0, 1.2, 1.2, 0))));
        g.fillOval(x - gr, y - gr, gr * 2, gr * 2);
        double start = -Math.toDegrees(theta) - 90;                     // JavaFX arcs run counter-clockwise, screen angles clockwise
        double r = 5 + 17 * grow, w = 2.4 * (1 - 0.7 * t) + 0.5;
        g.setLineWidth(w + 2.0); g.setStroke(under); g.strokeArc(x - r, y - r, r * 2, r * 2, start, 180, javafx.scene.shape.ArcType.OPEN);
        g.setLineWidth(w); g.setStroke(col); g.strokeArc(x - r, y - r, r * 2, r * 2, start, 180, javafx.scene.shape.ArcType.OPEN);
        double r0 = 4 + 5 * grow, r1 = 9 + 20 * grow, rayW = 2.2 * (1 - 0.55 * t) + 0.7;
        for (int pass = 0; pass < 2; pass++) {
            g.setLineWidth(pass == 0 ? rayW + 2.0 : rayW); g.setStroke(pass == 0 ? under : col);
            for (int i = -2; i <= 2; i++) {
                double ang = theta + Math.toRadians(i * 38), far = (Math.abs(i) == 1) ? r1 * 0.7 : r1;
                g.strokeLine(x + Math.cos(ang) * r0, y + Math.sin(ang) * r0, x + Math.cos(ang) * far, y + Math.sin(ang) * far);
            }
        }
        double core = t < 0.25 ? 1.0 - t / 0.25 : 0.0;
        if (core > 0) { g.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.9 * core)); g.fillOval(x - 4.5, y - 4.5, 9, 9); }
    }

    /**
     * One frame of a burst, {@code t} from 0 to 1 over {@link #BURST_MS}. It has to catch the eye on a busy map without shouting: a flash
     * and a glow round the dot, two rings (the second a beat behind, and fainter) that spread, and eight rays (long and short alternately) that shoot out and
     * turn a little. It holds full strength for the first 35% and then fades. Everything is drawn twice, a dark line under the band-coloured one, so it shows on
     * a pale map as well as a dark one.
     */
    static void drawBurst(GraphicsContext g, double x, double y, double t, Color band) {
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
    static Label[] projBtns = new Label[0];
    /** Light the button for {@code sel}; the region chips call this too, because picking a region switches the map to Rectangular. */
    static void highlightProjection(int sel) {
        for (int i = 0; i < projBtns.length; i++) { projBtns[i].getStyleClass().remove("on"); if (i == sel) projBtns[i].getStyleClass().add("on"); }
    }
    static Region projectionToggle(java.util.function.IntConsumer onProjection) {
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
    static int projection = Math.max(0, Math.min(2, com.ars.fx.data.HubConfig.getInt("map.projection", 0)));   // 0 azimuthal · 1 rectangular · 2 globe
    static String nowUtc() { return java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")); }
    static String grayLineStat() {
        if (!com.ars.fx.data.HubConfig.hasLocation()) return "set location";
        double[] rs = com.ars.fx.data.SolarPosition.riseSet(Q_LAT, Q_LON);
        return Double.isNaN(rs[0]) ? (rs[2] > 0 ? "sun up all day" : "sun down all day") : "SR " + com.ars.fx.data.SolarPosition.hhmm(rs[0]) + " · SS " + com.ars.fx.data.SolarPosition.hhmm(rs[1]);
    }
    /** Live topbar: ticking UTC clock + SPOTS / SHOWN / GRAY-LINE / BEAM. */
    static void updateMapTopbar() {
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
    static void collectStatValues(Node n, java.util.List<Label> out) {
        if (n instanceof Label l && (l.getStyleClass().contains("hero-stat-v") || l.getStyleClass().contains("sx-stat-v"))) out.add(l);
        if (n instanceof javafx.scene.Parent p) for (Node c : p.getChildrenUnmodifiable()) collectStatValues(c, out);
    }

    // ---- bottom alert scroller (port of original J-Map DxScrollerBar) ------
    static Region tickerBar() {
        double H = 42, pageSeconds = 7;                       // how long each page of the ticker stays up
        Pane pane = new Pane();
        pane.setMinHeight(H); pane.setPrefHeight(H); pane.setMaxHeight(H);
        pane.setStyle("-fx-background-color:#07090c;-fx-border-color:#2c3540 transparent transparent transparent;-fx-border-width:1 0 0 0;");
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(pane.widthProperty()); clip.setHeight(H);
        pane.setClip(clip);

        // Pages, not a scroll. A scrolling bar forced a new frame 30 to 60 times a second for as long as the map was open, and on this class of
        // laptop GPU every frame of a 4K window costs about the same (the render thread sits in the driver's makeCurrent / swap), so the
        // ticker alone cost about 60 to 90 points of CPU. A page changes every few seconds and the screen is otherwise still.
        Font font = Font.font("JetBrains Mono", 12.5);
        Text line = new Text(); line.setFont(font); line.setFill(Color.web("#c3ccd6"));
        line.setTextOrigin(javafx.geometry.VPos.CENTER); line.setLayoutY(H / 2);
        pane.getChildren().add(line);

        java.util.List<String> pages = new java.util.ArrayList<>();
        int[] shown = { 0 };
        Runnable show = () -> {
            if (pages.isEmpty()) { line.setText(""); return; }
            line.setText(pages.get(shown[0] % pages.size()));
            line.setLayoutX(Math.max(12, (pane.getWidth() - line.getLayoutBounds().getWidth()) / 2));   // centred
        };
        Runnable rebuild = () -> {                             // pack as many items as fit the bar onto each page
            double w = pane.getWidth() - 40;
            if (w <= 0) return;
            String all = com.ars.fx.data.SolarData.tickerText(com.ars.fx.data.SolarData.getInstance().snapshot(), grayLineStat());
            pages.clear();
            String sep = "   ●   ", cur = "";
            for (String item : all.split("●")) {
                item = item.strip();
                if (item.isEmpty()) continue;
                String tryText = cur.isEmpty() ? item : cur + sep + item;
                Text meas = new Text(tryText); meas.setFont(font);
                if (!cur.isEmpty() && meas.getLayoutBounds().getWidth() > w) { pages.add(cur); cur = item; } else cur = tryText;
            }
            if (!cur.isEmpty()) pages.add(cur);
            show.run();
        };
        rebuild.run();

        // a short fade between pages (a third of a second of frames every few seconds)
        javafx.animation.Timeline pager = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(pageSeconds), e -> {
            if (pages.size() < 2 || pane.getScene() == null) return;
            javafx.animation.FadeTransition out = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), line);
            out.setToValue(0);
            out.setOnFinished(f -> {
                shown[0] = (shown[0] + 1) % pages.size();
                show.run();
                javafx.animation.FadeTransition in = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), line);
                in.setToValue(1); in.play();
            });
            out.play();
        }));
        pager.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        pager.play();
        javafx.beans.value.ChangeListener<Number> onWidth = (o, a, b) -> rebuild.run();
        pane.widthProperty().addListener(onWidth);
        Runnable onData = rebuild;                             // new figures as soon as they arrive
        com.ars.fx.data.SolarData.getInstance().addListener(onData);
        pane.sceneProperty().addListener((o, old, sc) -> {
            if (sc == null && old != null) { pager.stop(); com.ars.fx.data.SolarData.getInstance().removeListener(onData); }
        });
        return pane;
    }

    // ---- right rail --------------------------------------------------------
    /** Rebuild the live DX-spots list (cluster + filters). */
    static void fillSpots(VBox spots) {
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
            String need = spotWorked(s) ? "n" : "y";
            VBox txt = new VBox(1, hcall(s.call(), need),
                    lbl(s.band() + " · " + s.mode() + " · " + com.ars.fx.data.ClusterClient.age(s.arrivalMs()), "jm-spot-meta")); HBox.setHgrow(txt, Priority.ALWAYS);
            HBox row = new HBox(11, dot, fq, txt); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("jm-spot-row");
            if (s.band() != null && !s.band().isBlank()) row.getStyleClass().add("band-" + s.band().trim().toLowerCase());   // a band stripe in the looks that ask for one
            Node pc = com.ars.fx.data.PathNow.shown() ? com.ars.fx.shell.PathChip.of(com.ars.fx.data.PathNow.forPoint(s.lat(), s.lon(), s.freqHz())) : null;
            if (pc != null) row.getChildren().add(pc);
            row.setStyle("-fx-cursor:hand;");
            com.ars.fx.shell.Tips.tip(row, "Click to use this spot — fills the log entry, tunes the rig, and offers to turn the beam");
            row.setOnMouseClicked(e -> selectSpot(row, s));
            com.ars.fx.shell.Fx.fresh(row, s.arrivalMs());          // a new spot slides in and glows
            spots.getChildren().add(row);
        }
    }
    static Region spotsDrawer;                    // its summary follows the live count
    static Node[] railDrawers(VBox spots) {
        com.ars.fx.data.ClusterClient cc = com.ars.fx.data.ClusterClient.getInstance();
        Label connect = lbl(com.ars.fx.shell.FeedUi.action(cc), "jl-logbtn"); connect.setStyle("-fx-cursor:hand;-fx-padding:6 14;");
        connect.setOnMouseClicked(e -> { com.ars.fx.shell.FeedUi.toggle(cc); connect.setText(com.ars.fx.shell.FeedUi.action(cc)); });
        // with the other one-second clocks, and only while the button is on screen (this timeline was never stopped, so each rebuild of the
        // drawer left another one running behind it)
        com.ars.fx.shell.Fx.everySecondWhileShown(connect, () -> connect.setText(com.ars.fx.shell.FeedUi.action(cc)));
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
    static Node hcall(String call, String need){
        HBox h = new HBox(7, lbl(call, "jm-spot-call")); h.setAlignment(Pos.CENTER_LEFT);
        if (need.equals("y")) h.getChildren().add(lbl("NEED","jhub-need"));
        return h;
    }
}
