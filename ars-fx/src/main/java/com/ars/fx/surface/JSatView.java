package com.ars.fx.surface;

import com.ars.fx.data.SatService;
import com.ars.fx.data.SatService.SatInfo;
import com.ars.fx.data.SatService.Snapshot;
import com.ars.fx.shell.Shell;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/** J-Sat satellite tracking (handoff shot 06) — real SGP4 from {@link SatService}. */
public final class JSatView {
    private JSatView() {}

    // ── view state (persists across the 2 s snapshot rebuilds) ────────────────
    private static String selectedSat = null;   // null = follow the auto-tracked sat
    private static int    viewMode    = 0;      // 0 = polar sky plot, 1 = blue marble
    private static boolean trackRotor = false;  // feed focus az to the rotor while in pass
    private static boolean dopplerTune = false; // feed Doppler-corrected downlink to the rig

    /** Each rebuild (~2 s): point the rotor and tune the rig at the focus satellite when enabled. */
    private static void autoTrack(SatInfo f) {
        if (f == null) return;
        if (trackRotor && f.inPass() && f.elDeg() > 0) com.ars.fx.data.RotorClient.getInstance().moveTo(f.azDeg());
        if (dopplerTune && f.downlinkCorrHz() > 0) com.ars.fx.data.RigClient.getInstance().setFreqHz(f.downlinkCorrHz());
    }

    private static boolean aosAlarm = false;                              // chirp on AOS / LOS transitions
    private static final java.util.Set<String> prevInPass = new java.util.HashSet<>();
    private static boolean alarmInited = false;
    /** Detect satellites crossing the horizon since the last snapshot and chirp if armed. */
    private static void checkAlarms(Snapshot snap) {
        java.util.Set<String> now = new java.util.HashSet<>();
        for (SatInfo s : snap.sats()) if (s.inPass()) now.add(s.name());
        if (alarmInited && aosAlarm) {
            for (String n : now) if (!prevInPass.contains(n)) beep(true);      // AOS (rising chirp)
            for (String n : prevInPass) if (!now.contains(n)) beep(false);     // LOS (falling chirp)
        }
        prevInPass.clear(); prevInPass.addAll(now); alarmInited = true;
    }
    /** Two-note chirp (rising = AOS, falling = LOS) via javax.sound; silent if no audio device. */
    private static void beep(boolean rising) {
        Thread t = new Thread(() -> {
            try {
                int rate = 44100; double dur = 0.16;
                float[] freqs = rising ? new float[]{784, 1175} : new float[]{784, 523};
                javax.sound.sampled.AudioFormat fmt = new javax.sound.sampled.AudioFormat(rate, 8, 1, true, false);
                javax.sound.sampled.SourceDataLine line = javax.sound.sampled.AudioSystem.getSourceDataLine(fmt);
                line.open(fmt); line.start();
                for (float fr : freqs) {
                    byte[] buf = new byte[(int) (rate * dur)];
                    for (int i = 0; i < buf.length; i++) buf[i] = (byte) (Math.sin(2 * Math.PI * fr * i / rate) * 78);
                    line.write(buf, 0, buf.length);
                }
                line.drain(); line.close();
            } catch (Throwable ignored) { }
        }, "sat-alarm");
        t.setDaemon(true); t.start();
    }
    private static boolean satCollapsed = false;
    private static StackPane host;
    private static boolean clockStarted = false;
    private static final java.time.format.DateTimeFormatter HMS = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

    public static Region build() {
        host = new StackPane();
        host.getChildren().add(placeholder("Acquiring TLEs & computing passes…"));
        SatService.getInstance().setListener(s -> rebuild());
        SatService.getInstance().start();
        // tick the UTC clock once a second (updates just the clock label, not the whole frame)
        if (!clockStarted) {
            clockStarted = true;
            Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                if (host != null && host.lookup(".sx-clock-v") instanceof Label l)
                    l.setText(java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(HMS));
            }));
            clock.setCycleCount(Timeline.INDEFINITE); clock.play();
        }
        return host;
    }
    private static void rebuild() {
        if (host != null) host.getChildren().setAll(frame(SatService.getInstance().snapshot()));
    }

    // distinct per-satellite colours (assigned by name so each sat keeps its colour)
    private static final Color[] PALETTE = {
            Color.web("#6fc884"), Color.web("#31bdda"), Color.web("#eebb58"), Color.web("#b191ea"),
            Color.web("#ef7d83"), Color.web("#54d0c8"), Color.web("#f0a35a"), Color.web("#7f9cff"),
            Color.web("#d98cc8"), Color.web("#9ad04f"), Color.web("#ff8f6b"), Color.web("#62c0e8")
    };
    private static java.util.Map<String, Color> colorMap(Snapshot snap) {
        java.util.List<String> names = new ArrayList<>();
        for (SatInfo s : snap.sats()) names.add(s.name());
        java.util.Collections.sort(names);
        java.util.Map<String, Color> m = new java.util.HashMap<>();
        for (int i = 0; i < names.size(); i++) m.put(names.get(i), PALETTE[i % PALETTE.length]);
        return m;
    }
    private static Color alpha(Color c, double a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }
    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    private static SatInfo focus(Snapshot snap) {
        if (selectedSat != null)
            for (SatInfo s : snap.sats()) if (s.name().equals(selectedSat)) return s;
        return snap.tracked();
    }

    private static Region frame(Snapshot snap) {
        Region dock = Shell.dock("sat");
        SatInfo f = focus(snap);
        autoTrack(f);
        checkAlarms(snap);
        String name = f != null ? f.name() : "—";
        String[][] stats = {
                {"TRACKING", name, "accent"},
                {"AZ / EL", f != null ? String.format("%03.0f° / %.0f°", f.azDeg(), f.elDeg()) : "—"},
                {"AOS", f != null ? SatService.hhmmUtc(f.aosEpoch()) : "—"},
                {"LOS", f != null ? SatService.hhmmUtc(f.losEpoch()) : "—"},
                {"APOGEE", f != null ? Math.round(f.apogeeKm()) + " km" : "—"},
                {"PERIGEE", f != null ? Math.round(f.perigeeKm()) + " km" : "—"}};
        String utc = java.time.LocalTime.now(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        HBox top = Shell.topBar("sat", "sat", "J-Sat", "Satellite tracking · " + name, stats, utc);

        Region center = snap.ready() && f != null
                ? new HBox(satColumn(snap, f), plotCol(snap, f))
                : placeholder(snap.error().isEmpty() ? "Computing…" : snap.error());
        if (center instanceof HBox hb) hb.setFillHeight(true);
        Region rail = Shell.rail(railDrawers(snap, f));
        return (Region) Shell.frame(dock, top, center, rail);
    }

    private static Region placeholder(String msg) {
        Label l = lbl(msg); l.setStyle("-fx-font-size:13px;-fx-text-fill:-ars-t3;");
        StackPane sp = new StackPane(l); sp.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ---- left satellite list (collapsible drawer) --------------------------
    private static Region satColumn(Snapshot snap, SatInfo f) {
        if (satCollapsed) {
            VBox p = new VBox(0); p.getStyleClass().add("js-panel"); p.setMinWidth(40); p.setPrefWidth(40); p.setMaxWidth(40);
            p.setAlignment(Pos.TOP_CENTER); p.setPadding(new Insets(14, 0, 14, 0));
            Label expand = lbl("›", "jm-collapse"); expand.setStyle("-fx-cursor:hand;");
            expand.setOnMouseClicked(e -> { satCollapsed = false; rebuild(); });
            Label v = lbl("SATELLITES", "js-sec"); v.setRotate(90);
            StackPane vwrap = new StackPane(v); vwrap.setMinWidth(36); VBox.setMargin(vwrap, new Insets(28, 0, 0, 0)); vwrap.setMinHeight(130);
            p.getChildren().addAll(expand, vwrap);
            return p;
        }
        VBox p = new VBox(9); p.getStyleClass().add("js-panel"); p.setMinWidth(310); p.setPrefWidth(310); p.setMaxWidth(310);
        Label title = lbl("SATELLITES · " + snap.sats().size(), "js-sec"); HBox.setHgrow(title, Priority.ALWAYS); title.setMaxWidth(Double.MAX_VALUE);
        Label collapse = lbl("‹", "jm-collapse"); collapse.setStyle("-fx-cursor:hand;");
        collapse.setOnMouseClicked(e -> { satCollapsed = true; rebuild(); });
        p.getChildren().add(new HBox(8, title, collapse));
        java.util.Map<String, Color> cm = colorMap(snap);
        for (SatInfo s : snap.sats()) p.getChildren().add(satCard(s, s == f, cm.get(s.name())));
        return p;
    }
    private static Region satCard(SatInfo s, boolean focused, Color color) {
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label pill = lbl(s.statusPill(), "js-pill"); if (!s.statusClass().isEmpty()) pill.getStyleClass().add(s.statusClass());
        Region swatch = new Region(); swatch.setMinSize(10, 10); swatch.setMaxSize(10, 10);
        swatch.setStyle("-fx-background-color:" + toHex(color) + "; -fx-background-radius:3;");
        Label nm = lbl(s.name(), "js-sat-nm"); nm.setMinWidth(Region.USE_PREF_SIZE);   // never truncate the callsign
        Label tp = lbl(s.type(), "js-sat-type"); tp.setMinWidth(0);                     // type ellipsizes if tight
        HBox top = new HBox(8, swatch, nm, tp, sp, pill); top.setAlignment(Pos.CENTER_LEFT);
        String dur = s.nextDurSec() > 0 ? Math.round(s.nextDurSec() / 60.0) + "m" : "—";
        HBox stats = new HBox(18, stat("Max", "el " + Math.round(s.nextMaxElDeg()) + "°"), stat("Dur", dur),
                stat("Dn", SatService.mhz(s.downlinkHz()))); stats.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(stats, new Insets(9, 0, 0, 0));
        VBox card = new VBox(top, stats); card.getStyleClass().add("js-sat");
        if (focused) card.getStyleClass().add("sel");
        card.setStyle("-fx-cursor:hand;");
        card.setOnMouseClicked(e -> { selectedSat = s.name().equals(selectedSat) ? null : s.name(); rebuild(); });
        return card;
    }
    private static VBox stat(String k, String v){ return new VBox(2, lbl(k, "js-sk"), lbl(v, "js-sv")); }

    // ---- center plot + telemetry ------------------------------------------
    private static Region plotCol(Snapshot snap, SatInfo f) {
        String sub = String.format("%s · max el %d°  ·  %d sats", f.type(), Math.round(f.nextMaxElDeg()), snap.sats().size());
        HBox titleL = new HBox(8, lbl(f.name(), "js-title"), lbl(sub, "js-title-sub")); titleL.setAlignment(Pos.CENTER_LEFT);
        Region tsp = new Region(); HBox.setHgrow(tsp, Priority.ALWAYS);
        Region toggle = viewToggle();
        HBox title = new HBox(10, titleL, tsp, toggle); title.setAlignment(Pos.CENTER_LEFT); title.setPadding(new Insets(14, 4, 8, 0));

        boolean dim = selectedSat != null;
        // the plot canvas fills the available area and repaints on resize
        Canvas cv = new Canvas();
        StackPane sky = new StackPane(cv); sky.setStyle("-fx-background-color:-ars-bg;"); VBox.setVgrow(sky, Priority.ALWAYS); sky.setMinSize(0, 0);
        Runnable repaint = () -> {
            double aw = sky.getWidth(), ah = sky.getHeight();
            if (aw <= 1 || ah <= 1) return;
            cv.setWidth(aw); cv.setHeight(ah);
            if (viewMode == 1) drawMarble(cv.getGraphicsContext2D(), aw, ah, snap, f, dim, cv);
            else drawPolar(cv.getGraphicsContext2D(), aw, ah, snap, f, dim, cv);
        };
        sky.widthProperty().addListener((o, a, b) -> repaint.run());
        sky.heightProperty().addListener((o, a, b) -> repaint.run());
        repaint.run();

        long dopHz = f.downlinkCorrHz() - f.downlinkHz();
        String[][] cells = {
                {"AZIMUTH", String.format("%03.0f", f.azDeg()), "°", ""},
                {"ELEVATION", String.format("%.0f", f.elDeg()), "°", f.elDeg() < 0 ? "y" : ""},
                {"ALTITUDE", String.format("%,d", Math.round(f.altKm())), "km", ""},
                {"RANGE", String.format("%,d", Math.round(f.rangeKm())), "km", ""},
                {"RANGE RATE", String.format("%+.1f", f.rangeRateKmS()), "km/s", f.rangeRateKmS() < 0 ? "y" : ""},
                {"DOPPLER", String.format("%+.1f", dopHz / 1000.0), "kHz", dopHz < 0 ? "y" : ""}};
        HBox tel = new HBox(12); tel.setAlignment(Pos.CENTER); tel.setPadding(new Insets(0, 0, 22, 0));
        for (String[] c : cells) {
            Label v = lbl(c[1], "js-tel-v"); if (c[3].equals("y")) v.getStyleClass().add("neg");
            HBox vu = new HBox(3, v, lbl(c[2], "js-tel-u")); vu.setAlignment(Pos.BOTTOM_LEFT);
            VBox cell = new VBox(3, lbl(c[0], "js-tel-k"), vu); cell.getStyleClass().add("js-tel"); cell.setMinWidth(108);
            tel.getChildren().add(cell);
        }
        VBox col = new VBox(0, title, sky, tel); col.setAlignment(Pos.TOP_CENTER); col.setStyle("-fx-background-color:-ars-bg;");
        HBox.setHgrow(col, Priority.ALWAYS); col.setPadding(new Insets(0, 20, 0, 20));
        return col;
    }
    private static Region viewToggle() {
        HBox h = new HBox(2); h.getStyleClass().add("jm-seg");
        String[] opts = {"Polar", "Blue marble"};
        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            Label b = lbl(opts[i], "jm-seg-btn"); if (i == viewMode) b.getStyleClass().add("on");
            b.setMinWidth(96); b.setStyle("-fx-cursor:hand;");
            b.setOnMouseClicked(e -> { viewMode = idx; rebuild(); });
            h.getChildren().add(b);
        }
        return h;
    }

    // ---- polar sky plot (all sats above horizon) --------------------------
    private static void drawPolar(GraphicsContext g, double w, double h, Snapshot snap, SatInfo f, boolean dim, Canvas cv) {
        g.clearRect(0, 0, w, h);
        double cx = w/2, cy = h/2, R = Math.max(60, Math.min(w,h)/2 - 30);
        Color border = Color.web("#28313d"), t3 = Color.web("#6a7684"), sat = Color.web("#6fc884");

        g.setStroke(border); g.setLineWidth(1);
        g.strokeOval(cx-R, cy-R, R*2, R*2);
        double r30 = R*(60/90.0), r60 = R*(30/90.0);
        g.strokeOval(cx-r30, cy-r30, r30*2, r30*2);
        g.strokeOval(cx-r60, cy-r60, r60*2, r60*2);
        for (int i=0;i<8;i++){ double a=Math.toRadians(i*45-90); g.setStroke(Color.web("#28313d",0.5)); g.strokeLine(cx,cy,cx+Math.cos(a)*R,cy+Math.sin(a)*R); }
        g.setFill(t3); g.setFont(Font.font("JetBrains Mono", 11)); g.setTextAlign(TextAlignment.CENTER);
        g.fillText("zenith", cx, cy+4); g.fillText("30°", cx, cy-r30+13); g.fillText("60°", cx, cy-r60+13);
        g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12));
        g.fillText("N", cx, cy-R-12); g.fillText("S", cx, cy+R+18);
        g.setTextAlign(TextAlignment.LEFT);  g.fillText("E", cx+R+8, cy+4);
        g.setTextAlign(TextAlignment.RIGHT); g.fillText("W", cx-R-8, cy+4);

        java.util.Map<String, Color> cm = colorMap(snap);
        // focus pass track in the focus sat's colour
        double maxEl = f.nextMaxElDeg();
        if (maxEl > 0) {
            g.setStroke(alpha(cm.get(f.name()), dim ? 0.5 : 0.9)); g.setLineWidth(2); g.setLineDashes(5,5); g.beginPath();
            for (int i=0;i<=60;i++){
                double tt=i/60.0, el=Math.sin(tt*Math.PI)*maxEl;
                double az = tt < 0.5 ? lerpAz(f.aosAzDeg(), f.maxElAzDeg(), tt/0.5) : lerpAz(f.maxElAzDeg(), f.losAzDeg(), (tt-0.5)/0.5);
                double r=R*((90-el)/90.0), a=Math.toRadians(az-90);
                if (i==0) g.moveTo(cx+Math.cos(a)*r, cy+Math.sin(a)*r); else g.lineTo(cx+Math.cos(a)*r, cy+Math.sin(a)*r);
            }
            g.stroke(); g.setLineDashes(null);
        }

        // all sats above the horizon, coloured per satellite
        List<String> names = new ArrayList<>(); List<double[]> pos = new ArrayList<>();
        int below = 0;
        for (SatInfo s : snap.sats()) {
            if (s.elDeg() <= 0) { below++; continue; }
            boolean isF = s == f;
            double r=R*((90-s.elDeg())/90.0), a=Math.toRadians(s.azDeg()-90);
            double x=cx+Math.cos(a)*r, y=cy+Math.sin(a)*r;
            double al = isF ? 1.0 : (dim ? 0.28 : 0.9);
            if (isF) { g.setStroke(Color.web("#ffffff",0.55)); g.setLineWidth(2.5); g.strokeOval(x-8,y-8,16,16); }
            g.setStroke(Color.web("#0b0f14",0.8)); g.setLineWidth(2); g.strokeOval(x-5,y-5,10,10);
            g.setFill(alpha(cm.get(s.name()), al)); g.fillOval(x-5,y-5,10,10);
            g.setFill(Color.web("#e8eef4", al)); g.setFont(Font.font("JetBrains Mono", isF?FontWeight.BOLD:FontWeight.NORMAL, isF?12:11));
            g.setTextAlign(TextAlignment.LEFT); g.fillText(s.name(), x+9, y+4);
            names.add(s.name()); pos.add(new double[]{x,y});
        }
        if (below > 0) {
            g.setFill(t3); g.setFont(Font.font("JetBrains Mono", 11)); g.setTextAlign(TextAlignment.CENTER);
            g.fillText(below + " of " + snap.sats().size() + " sats below horizon", cx, cy+R+34);
        }
        attachPick(cv, names, pos);
    }

    // ---- blue marble plot (all sats, sub-satellite points + ground tracks) ----
    private static Image marble;
    private static Image marble() {
        if (marble == null) marble = new Image(JSatView.class.getResourceAsStream("/com/ars/fx/map/bluemarble.jpg"));
        return marble;
    }
    private static void drawMarble(GraphicsContext g, double w, double h, Snapshot snap, SatInfo f, boolean dim, Canvas cv) {
        g.clearRect(0, 0, w, h);
        double imgW = Math.min(w, h * 2), imgH = imgW / 2;
        double RMX = (w - imgW) / 2, RMY = (h - imgH) / 2, RW = imgW, RH = imgH;
        g.setFill(Color.web("#0b0f14")); g.fillRect(0, 0, w, h);
        Image m = marble();
        if (m != null && !m.isError()) g.drawImage(m, RMX, RMY, RW, RH);
        g.setStroke(Color.web("#28313d")); g.setLineWidth(1); g.strokeRect(RMX, RMY, RW, RH);
        for (int lon=-150; lon<=150; lon+=30){ double x=lon2x(lon,RMX,RW); g.setStroke(Color.web("#28313d",lon==0?0.8:0.4)); g.strokeLine(x,RMY,x,RMY+RH); }
        for (int lat=-60; lat<=60; lat+=30){ double y=lat2y(lat,RMY,RH); g.setStroke(Color.web("#28313d",lat==0?0.8:0.4)); g.strokeLine(RMX,y,RMX+RW,y); }

        // ground tracks (one orbital period) for ALL sats in view, each its own colour;
        // non-focus tracks dim along with their dots when a satellite is selected.
        java.util.Map<String, Color> cm = colorMap(snap);
        for (SatInfo s : snap.sats()) {
            if (s == f) continue;
            drawTrack(g, s.track(), RMX, RMY, RW, RH, alpha(cm.get(s.name()), dim ? 0.28 : 0.55), 1.1);
        }
        drawTrack(g, f.track(), RMX, RMY, RW, RH, alpha(cm.get(f.name()), 0.95), 1.8);

        // focus footprint (visibility circle, approximated as an ellipse) in its colour
        if (f.altKm() > 1) {
            Color fc = cm.get(f.name());
            double fp = f.footprintDeg();
            double fx = lon2x(f.subLon(), RMX, RW), fy = lat2y(f.subLat(), RMY, RH);
            double rxDeg = Math.min(170, fp / Math.max(0.25, Math.cos(Math.toRadians(f.subLat()))));
            double rx = (rxDeg / 360.0) * RW, ry = (fp / 180.0) * RH;
            g.setStroke(alpha(fc, 0.6)); g.setLineWidth(1.3);
            g.setFill(alpha(fc, 0.10));
            g.fillOval(fx - rx, fy - ry, rx * 2, ry * 2);
            g.strokeOval(fx - rx, fy - ry, rx * 2, ry * 2);
        }

        // QTH marker
        double qx = lon2x(SatService.getInstance().obsLon(), RMX, RW), qy = lat2y(SatService.getInstance().obsLat(), RMY, RH);
        g.setFill(Color.web("#ffffff")); g.fillOval(qx-4, qy-4, 8, 8);
        g.setStroke(Color.web("#ffffff",0.5)); g.setLineWidth(1.2); g.strokeOval(qx-7, qy-7, 14, 14);

        // all sub-satellite points, coloured per satellite (light ring = visible now)
        List<String> names = new ArrayList<>(); List<double[]> pos = new ArrayList<>();
        for (SatInfo s : snap.sats()) {
            boolean isF = s == f;
            double x = lon2x(s.subLon(), RMX, RW), y = lat2y(s.subLat(), RMY, RH);
            double a = isF ? 1.0 : (dim ? 0.3 : 0.95);
            Color base = cm.get(s.name());
            if (isF) { g.setStroke(Color.web("#ffffff",0.55)); g.setLineWidth(2.5); g.strokeOval(x-8,y-8,16,16); }
            g.setStroke(Color.web("#0b0f14",0.8)); g.setLineWidth(2); g.strokeOval(x-4.5,y-4.5,9,9);
            g.setFill(alpha(base, a)); g.fillOval(x-4.5,y-4.5,9,9);
            if (s.elDeg() > 0) { g.setStroke(alpha(Color.web("#e8eef4"), a)); g.setLineWidth(1.4); g.strokeOval(x-6.5,y-6.5,13,13); }
            g.setFill(Color.web("#e8eef4", a)); g.setFont(Font.font("JetBrains Mono", isF?FontWeight.BOLD:FontWeight.NORMAL, isF?12:10));
            g.setTextAlign(TextAlignment.LEFT); g.fillText(s.name(), x+8, y+4);
            names.add(s.name()); pos.add(new double[]{x,y});
        }
        attachPick(cv, names, pos);
    }
    /** Draw a ground-track polyline, breaking the line where it wraps across the date line. */
    private static void drawTrack(GraphicsContext g, double[][] track, double rmx, double rmy, double rw, double rh, Color color, double width) {
        if (track == null || track.length < 2) return;
        g.setStroke(color); g.setLineWidth(width);
        g.beginPath();
        double prevX = Double.NaN; boolean started = false;
        for (double[] pt : track) {
            double x = lon2x(pt[1], rmx, rw), y = lat2y(pt[0], rmy, rh);
            if (started && Math.abs(x - prevX) > rw * 0.5) { g.stroke(); g.beginPath(); started = false; }
            if (!started) { g.moveTo(x, y); started = true; } else g.lineTo(x, y);
            prevX = x;
        }
        g.stroke();
    }
    private static double lon2x(double lon, double rmx, double rw) { return rmx + ((lon + 180) / 360.0) * rw; }
    private static double lat2y(double lat, double rmy, double rh) { return rmy + ((90 - lat) / 180.0) * rh; }

    /** Click-to-select nearest satellite dot. */
    private static void attachPick(Canvas cv, List<String> names, List<double[]> pos) {
        cv.setOnMouseClicked((MouseEvent e) -> {
            double best = 1e9; String pick = null;
            for (int i = 0; i < pos.size(); i++) {
                double d = Math.hypot(e.getX() - pos.get(i)[0], e.getY() - pos.get(i)[1]);
                if (d < best) { best = d; pick = names.get(i); }
            }
            if (pick != null && best < 20) { selectedSat = pick.equals(selectedSat) ? null : pick; rebuild(); }
        });
    }
    private static double lerpAz(double a, double b, double t) {
        double d = ((b - a + 540) % 360) - 180;
        return (a + d * t + 360) % 360;
    }

    // ---- right rail --------------------------------------------------------
    private static Node[] railDrawers(Snapshot snap, SatInfo f) {
        VBox doppler;
        if (f != null) {
            doppler = new VBox(9,
                    radioBox("DOWNLINK", SatService.mhz(f.downlinkHz()) + "  " + SatService.shiftKHz(f.downlinkCorrHz(), f.downlinkHz()), SatService.mhz4(f.downlinkCorrHz())),
                    radioBox("UPLINK", SatService.mhz(f.uplinkHz()) + "  " + SatService.shiftKHz(f.uplinkCorrHz(), f.uplinkHz()), SatService.mhz4(f.uplinkCorrHz())),
                    Shell.kv("Mode", f.mode(), false),
                    satToggleRow("Track with rotor", trackRotor, () -> { trackRotor = !trackRotor; if (trackRotor && f.inPass() && f.elDeg() > 0) com.ars.fx.data.RotorClient.getInstance().moveTo(f.azDeg()); rebuild(); }),
                    satToggleRow("Doppler-tune rig", dopplerTune, () -> { dopplerTune = !dopplerTune; if (dopplerTune && f.downlinkCorrHz() > 0) com.ars.fx.data.RigClient.getInstance().setFreqHz(f.downlinkCorrHz()); rebuild(); }));
        } else {
            doppler = new VBox(9, lbl("No satellite selected", "js-radio-sub"));
        }
        VBox np = new VBox();
        np.getChildren().add(satToggleRow("AOS / LOS alarm", aosAlarm, () -> { aosAlarm = !aosAlarm; rebuild(); }));
        int shown = 0;
        for (SatInfo s : snap.sats()) {
            if (shown++ >= 6) break;
            Label nm = lbl(s.name(), "js-np-nm"); Label el = lbl("el " + Math.round(s.nextMaxElDeg()) + "°", "js-np-el");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            String when = s.inPass() ? "now" : SatService.compactLower(s.secsToAos());
            HBox row = new HBox(9, nm, el, sp, lbl(when, "js-np-t")); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("js-np");
            row.setStyle("-fx-cursor:hand;"); row.setOnMouseClicked(e -> { selectedSat = s.name(); rebuild(); });
            np.getChildren().add(row);
        }
        String dopSummary = f != null ? SatService.mhz4(f.downlinkCorrHz()) : "—";
        String passSummary = !snap.sats().isEmpty() ? snap.sats().get(0).name() + " " +
                (snap.sats().get(0).inPass() ? "now" : SatService.compactLower(snap.sats().get(0).secsToAos())) : "—";
        List<Node> rail = new ArrayList<>();
        rail.addAll(Shell.leadDrawers(f != null ? (int) Math.round(f.azDeg()) : 0));   // Station ID, Rig control, Rotor control
        rail.add(drawer("Radio · Doppler", "bridge", "bridge", dopSummary, true, doppler));
        rail.add(drawer("Next passes", "sat", "sat", passSummary, true, np));
        rail.addAll(Shell.instruments(0));   // propagation, solar, band conditions, weather
        return rail.toArray(new Node[0]);
    }
    /** Drawer whose open/closed state survives the 2 s frame rebuilds (keyed by title). */
    private static final java.util.Map<String, Boolean> drawerOpen = new java.util.HashMap<>();
    private static Node drawer(String title, String hue, String glyph, String summary, boolean def, Node body) {
        boolean open = drawerOpen.getOrDefault(title, def);
        return Shell.drawer(title, hue, glyph, summary, open, body, o -> drawerOpen.put(title, o));
    }
    private static Region radioBox(String k, String sub, String v) {
        VBox left = new VBox(2, lbl(k, "js-radio-k"), lbl(sub, "js-radio-sub"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox box = new HBox(left, sp, lbl(v, "js-radio-v")); box.getStyleClass().add("js-radio"); box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
    /** kv-style row whose value is a clickable ON/OFF toggle. */
    private static HBox satToggleRow(String k, boolean on, Runnable onClick) {
        Label kl = lbl(k); kl.getStyleClass().add("k"); HBox.setHgrow(kl, Priority.ALWAYS); kl.setMaxWidth(Double.MAX_VALUE);
        Label vl = lbl(on ? "ON" : "OFF"); vl.getStyleClass().addAll("ars-mono", "v");
        vl.setStyle("-fx-cursor:hand;" + (on ? "-fx-text-fill:-ars-ok;-fx-font-weight:bold;" : "-fx-text-fill:-ars-t4;"));
        vl.setOnMouseClicked(e -> onClick.run());
        HBox row = new HBox(kl, vl); row.getStyleClass().add("sx-kv"); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
