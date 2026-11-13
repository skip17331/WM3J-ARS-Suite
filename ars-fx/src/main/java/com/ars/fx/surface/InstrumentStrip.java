package com.ars.fx.surface;

import com.ars.fx.data.ClusterClient;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.List;

/**
 * The Instrument panel dashboard's gauge row: four round needle dials (QSOs today, QSOs in the log, spots on air, needed spots) and a bank of
 * LED-segment meters for band activity. It replaces the Cockpit's stat tiles in that look only, and is kept current by {@link #update}.
 *
 * <p>Everything is drawn on canvases with fixed colours, because a real panel is dark whatever the room lighting: a bezel, a black face, white
 * block numerals, and an orange needle that swings to the new reading when it changes. A dial's scale grows by steps (10, 20, 50, 100, ...)
 * so the needle always has somewhere to go.
 */
final class InstrumentStrip extends HBox {
    private static final String[] BANDS = {"160","80","60","40","30","20","17","15","12","10","6","2","70c"};
    private static final String[] NAMES = {"QSOS TODAY", "IN LOG", "SPOTS ON AIR", "NEW ONES"};
    private static final Color[] TONES = {Color.web("#ffb038"), Color.web("#5cc8ff"), Color.web("#67d283"), Color.web("#b48cff")};
    private static final double[] STEPS = {10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000};
    private static final String FONT = "Bahnschrift";                  // squared, stencil-like; falls back to the system bold face where it is missing

    private final Dial[] dials = new Dial[4];
    private final BandMeters meters = new BandMeters();
    private long countsAt; private int today, log;

    InstrumentStrip() {
        super(14);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("ins-gauges");
        for (int i = 0; i < 4; i++) { dials[i] = new Dial(NAMES[i], TONES[i]); getChildren().add(dials[i]); }
        HBox.setHgrow(meters, Priority.ALWAYS); meters.setMaxWidth(Double.MAX_VALUE);
        getChildren().add(meters);
        setMinHeight(USE_PREF_SIZE); setMinWidth(0);
    }

    void update(List<ClusterClient.Spot> spots, int needed) {
        long now = System.currentTimeMillis();
        if (now - countsAt > 5000) {                         // the log counts change only when a QSO is saved
            countsAt = now;
            try { today = com.ars.fx.data.JLogDb.countToday(); log = com.ars.fx.data.JLogDb.count(); } catch (Exception ignored) {}
        }
        int[] v = {today, log, spots.size(), needed};
        for (int i = 0; i < 4; i++) dials[i].set(v[i]);
        int[] n = new int[BANDS.length];
        for (ClusterClient.Spot s : spots) {
            String b = s.band() == null ? "" : s.band().trim().toLowerCase().replace("m", "");
            if (b.equals("70c")) b = "70c";
            for (int i = 0; i < BANDS.length; i++) if (BANDS[i].equals(b)) { n[i]++; break; }
        }
        meters.set(n);
    }

    private static Font font(double size) { return Font.font(FONT, FontWeight.BOLD, size); }

    // ---- a round dial ---------------------------------------------------------
    private static final class Dial extends Pane {
        private static final double D = 158;
        private final Canvas cv = new Canvas(D, D);
        private final String name; private final Color tone;
        private final SimpleDoubleProperty shown = new SimpleDoubleProperty(0);
        private double value, scale = STEPS[0];
        private Timeline swing;

        Dial(String name, Color tone) {
            this.name = name; this.tone = tone;
            getChildren().add(cv);
            setMinSize(D, D); setPrefSize(D, D); setMaxSize(D, D);
            shown.addListener((o, a, b) -> draw());
            draw();
        }

        void set(double v) {
            if (v == value && scale >= v) return;
            value = v;
            for (double s : STEPS) { scale = s; if (v <= s * 0.95) break; }
            if (com.ars.fx.shell.Fx.reduced()) { shown.set(v); return; }
            if (swing != null) swing.stop();
            swing = new Timeline(new KeyFrame(Duration.millis(700), new KeyValue(shown, v, Interpolator.EASE_BOTH)));
            swing.play();
        }

        private void draw() {
            GraphicsContext g = cv.getGraphicsContext2D();
            g.clearRect(0, 0, D, D);
            double c = D / 2, R = D / 2 - 3;
            // bezel: a brushed-metal ring, then the black well the face sits in
            g.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#e4e7ec")), new Stop(0.45, Color.web("#7b8089")), new Stop(1, Color.web("#2b2e33"))));
            g.fillOval(c - R, c - R, 2 * R, 2 * R);
            double r1 = R - 7;
            g.setFill(Color.web("#050506")); g.fillOval(c - r1, c - r1, 2 * r1, 2 * r1);
            double r = r1 - 2;
            g.setFill(new RadialGradient(0, 0, c, c - r * 0.3, r * 1.3, false, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#23262b")), new Stop(1, Color.web("#0a0b0d"))));
            g.fillOval(c - r, c - r, 2 * r, 2 * r);

            double a0 = 135, sweep = 270, frac = Math.max(0, Math.min(1, shown.get() / scale));
            // lit arc up to the reading, in the dial's colour
            double ar = r - 6;
            g.setLineCap(StrokeLineCap.BUTT);
            g.setStroke(Color.web("#2b2f35")); g.setLineWidth(4); g.strokeArc(c - ar, c - ar, 2 * ar, 2 * ar, -a0, -sweep, javafx.scene.shape.ArcType.OPEN);
            g.setStroke(tone); g.setLineWidth(4); g.strokeArc(c - ar, c - ar, 2 * ar, 2 * ar, -a0, -sweep * frac, javafx.scene.shape.ArcType.OPEN);

            // ticks and numerals
            g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(javafx.geometry.VPos.CENTER);
            for (int i = 0; i <= 50; i++) {
                boolean major = i % 10 == 0, mid = i % 5 == 0;
                double ang = Math.toRadians(a0 + sweep * i / 50.0), cs = Math.cos(ang), sn = Math.sin(ang);
                double ro = ar - 5, ri = ro - (major ? 9 : mid ? 6 : 3.5);
                g.setStroke(Color.WHITE); g.setLineWidth(major ? 2 : 1);
                g.strokeLine(c + ro * cs, c + ro * sn, c + ri * cs, c + ri * sn);
                if (major) {
                    double nr = ri - 9;
                    double val = scale * i / 50.0;
                    g.setFill(Color.WHITE); g.setFont(font(10));
                    g.fillText(val >= 1000 ? (int) (val / 1000) + "k" : Integer.toString((int) val), c + nr * cs, c + nr * sn);
                }
            }
            // legend and the digital reading, white block letters
            g.setFill(Color.WHITE); g.setFont(font(7.5));
            g.fillText(name, c + 3, c + r * 0.42);                     // a little right of centre: the needle rests down-left at zero
            double bw = 50, bh = 16, by = c + r * 0.54;
            g.setFill(Color.web("#000000")); g.fillRoundRect(c - bw / 2, by, bw, bh, 4, 4);
            g.setStroke(Color.web("#4a4f57")); g.setLineWidth(1); g.strokeRoundRect(c - bw / 2, by, bw, bh, 4, 4);
            g.setFill(tone); g.setFont(font(13));
            g.fillText(Integer.toString((int) Math.round(value)), c, by + bh / 2 + 0.5);

            // needle: tapered, with a short tail, swung from the hub
            double ang = Math.toRadians(a0 + sweep * frac), cs = Math.cos(ang), sn = Math.sin(ang), px = -sn, py = cs;
            double len = r - 14, tail = 13, w = 3.2;
            g.save();
            g.setEffect(new javafx.scene.effect.DropShadow(5, 2, 3, Color.rgb(0, 0, 0, 0.75)));
            g.setFill(Color.web("#ff5a1f"));
            g.fillPolygon(new double[]{c + len * cs, c + w * px - tail * cs, c - w * px - tail * cs},
                          new double[]{c + len * sn, c + w * py - tail * sn, c - w * py - tail * sn}, 3);
            g.restore();
            g.setFill(new RadialGradient(0, 0, c - 2, c - 2, 9, false, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#9aa0a8")), new Stop(1, Color.web("#25282c"))));
            g.fillOval(c - 7, c - 7, 14, 14);
            g.setFill(Color.web("#0b0c0e")); g.fillOval(c - 2, c - 2, 4, 4);

            // glass: a soft highlight across the top of the lens
            g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.rgb(255, 255, 255, 0.13)), new Stop(1, Color.rgb(255, 255, 255, 0))));
            g.fillOval(c - r * 0.8, c - r * 0.97, r * 1.6, r * 0.9);
        }
    }

    // ---- band activity: banks of LED segments ----------------------------------
    private static final class BandMeters extends Pane {
        private final Canvas cv = new Canvas();
        private int[] n = new int[BANDS.length];

        BandMeters() {
            getChildren().add(cv);
            setMinSize(0, 158); setPrefSize(430, 158); setMaxHeight(158);
            widthProperty().addListener((o, a, b) -> draw());
        }
        void set(int[] counts) { n = counts; draw(); }

        @Override protected void layoutChildren() { cv.setWidth(getWidth()); cv.setHeight(getHeight()); draw(); }

        private void draw() {
            double w = getWidth(), h = getHeight();
            if (w < 40 || h < 40) return;
            GraphicsContext g = cv.getGraphicsContext2D();
            g.clearRect(0, 0, w, h);
            g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#15171a")), new Stop(1, Color.web("#0a0b0d"))));
            g.fillRoundRect(0, 0, w, h, 14, 14);
            g.setStroke(Color.web("#3a3e45")); g.setLineWidth(1.5); g.strokeRoundRect(0.75, 0.75, w - 1.5, h - 1.5, 14, 14);
            g.setTextAlign(TextAlignment.LEFT); g.setTextBaseline(javafx.geometry.VPos.TOP);
            g.setFill(Color.WHITE); g.setFont(font(10));
            g.fillText("BAND ACTIVITY", 16, 11);
            int max = 1; for (int v : n) max = Math.max(max, v);
            int cols = BANDS.length, segs = 9;
            double left = 16, right = 16, top = 34, bottom = 24, cw = (w - left - right) / cols, sh = (h - top - bottom) / segs;
            g.setTextAlign(TextAlignment.CENTER); g.setTextBaseline(javafx.geometry.VPos.CENTER);
            for (int i = 0; i < cols; i++) {
                double x = left + i * cw, sw = Math.max(6, cw * 0.56), sx = x + (cw - sw) / 2;
                int lit = n[i] == 0 ? 0 : Math.max(1, (int) Math.round(segs * (double) n[i] / max));
                for (int s = 0; s < segs; s++) {
                    boolean on = s < lit;
                    Color base = s < 5 ? Color.web("#67d283") : s < 7 ? Color.web("#ffb038") : Color.web("#ff5a4a");
                    double y = top + (segs - 1 - s) * sh;
                    g.setFill(on ? base : Color.web("#1d2024"));
                    g.fillRoundRect(sx, y + 1, sw, sh - 2.5, 2, 2);
                }
                g.setFill(Color.WHITE); g.setFont(font(9.5));
                g.fillText(BANDS[i], x + cw / 2, h - 12);
            }
        }
    }
}
