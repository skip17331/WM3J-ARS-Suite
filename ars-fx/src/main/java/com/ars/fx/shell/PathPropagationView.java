package com.ars.fx.shell;

import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.Geo;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.Propagation;
import com.ars.fx.data.Propagation.Hour;
import com.ars.fx.data.Propagation.Point;
import com.ars.fx.data.Propagation.Weather;
import com.ars.fx.data.RigClient;
import com.ars.fx.data.SolarData;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Band openings for one path: from this station to a call or prefix, for each band and each UTC hour of today, coloured by how likely the band
 * is to be open (green likely, red unlikely, grey shut). It is an estimate computed here ({@link Propagation}) from the solar flux, sunspot
 * number and K index the app already pulls, the sun's position along the path and the time of year - and it says so on the panel. The target
 * follows the last spot you clicked, or type one.
 */
public final class PathPropagationView extends VBox {
    public static final String KEY_TARGET = "prop.target";
    private static final double CELL_W = 11, CELL_H = 16;

    private final TextField target = new TextField(HubConfig.get(KEY_TARGET, ""));
    private final VBox out = new VBox(6);
    private final Label check = lbl("", "sx-dw-stamp");
    private String signature = "";

    /** The spot you clicked becomes the target of the estimate. */
    public static void setTarget(String call) { if (call != null && !call.isBlank()) HubConfig.set(KEY_TARGET, call.trim().toUpperCase(Locale.ROOT)); }

    public PathPropagationView() {
        super(8);
        SolarData.getInstance().start();
        target.setPromptText("call or prefix, e.g. JA1ABC or ZS"); target.getStyleClass().add("jhub-mini-field"); target.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(target, Priority.ALWAYS);
        PauseTransition typed = new PauseTransition(Duration.millis(350));
        typed.setOnFinished(e -> { HubConfig.set(KEY_TARGET, target.getText().trim().toUpperCase(Locale.ROOT)); refresh(); });
        target.textProperty().addListener((o, a, b) -> typed.playFromStart());
        HBox row = new HBox(8, lbl("PATH ESTIMATE TO", "sx-bc-hh"), target); row.setAlignment(Pos.CENTER_LEFT);
        check.setWrapText(true); check.setMaxWidth(330);
        Tips.tip(check, "How often our estimate agreed with what the DX cluster hears: every spot of 1,000 km or more is a path that was open, so a 'probably closed' answer for it is a miss. It cannot show false alarms. Kept between sessions. Click to start the count again.");
        check.setStyle("-fx-cursor:hand;");
        check.setOnMouseClicked(e -> { com.ars.fx.data.PropagationCheck.reset(); check.setText(com.ars.fx.data.PropagationCheck.summary()); });
        getChildren().addAll(row, out, check);
        check.setText(com.ars.fx.data.PropagationCheck.summary());
        refresh();
        // follows a spot clicked elsewhere, moves the "now" column and picks up fresh space-weather data; stops when the panel is taken off screen
        Timeline tick = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            String t = HubConfig.get(KEY_TARGET, "");
            if (!target.isFocused() && !t.equals(target.getText().trim())) target.setText(t);
            check.setText(com.ars.fx.data.PropagationCheck.summary());
            refresh();
        }));
        tick.setCycleCount(Animation.INDEFINITE);
        sceneProperty().addListener((o, was, now) -> { if (now == null) tick.stop(); else tick.play(); });
    }

    /** Redraws only if something that changes the picture has changed. */
    private void refresh() {
        SolarData.Snapshot sd = SolarData.getInstance().snapshot();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String sig = HubConfig.get(KEY_TARGET, "") + "|" + System.identityHashCode(sd) + "|" + now.getHour() + "|" + HubConfig.hasLocation() + "|" + HubConfig.grid();
        if (sig.equals(signature)) return;
        signature = sig;
        out.getChildren().setAll(build(sd, now));
    }

    private List<javafx.scene.Node> build(SolarData.Snapshot sd, ZonedDateTime now) {
        java.util.ArrayList<javafx.scene.Node> nodes = new java.util.ArrayList<>();
        String t = HubConfig.get(KEY_TARGET, "").trim();
        if (!HubConfig.hasLocation()) { nodes.add(note("Set your location in J-Hub ▸ Station to see an estimate from your station.")); return nodes; }
        double[] ll = t.isEmpty() ? null : Geo.of(t);
        if (t.isEmpty()) { nodes.add(note("Type a call or prefix, or click a spot, to see which bands should be open to it.")); return nodes; }
        if (ll == null) { nodes.add(note("\"" + t + "\" is not a call or prefix I can place.")); return nodes; }
        Weather w = sd == null ? null : Weather.of(sd.sfi(), sd.sunspots(), sd.kIndex());
        if (w == null) { nodes.add(note(sd == null ? "Waiting for space-weather data…" : "No solar flux or sunspot number reported just now.")); return nodes; }

        Point tx = new Point(HubConfig.lat(), HubConfig.lon()), rx = new Point(ll[0], ll[1]);
        double km = Propagation.distanceKm(tx, rx);
        List<Hour> day = Propagation.day(tx, rx, w, LocalDate.now(ZoneOffset.UTC));
        int hr = now.getHour();
        Hour cur = day.get(hr);

        String best = null; double bestR = 0;
        for (String b : ClusterClient.HEAT_BANDS) {
            Double mhz = freq(b); if (mhz == null) continue;
            double r = Propagation.reliability(mhz, cur);
            if (r > bestR) { bestR = r; best = b; }
        }
        Label head = lbl(String.format(Locale.US, "%s · %,.0f km · %d hop%s  ·  now: MUF %.1f, LUF %.1f MHz%s", t, km, cur.hops(), cur.hops() == 1 ? "" : "s",
                cur.mufMhz(), cur.lufMhz(), best == null || bestR < 0.2 ? "  ·  no band looks open" : "  ·  best " + best + " " + Math.round(bestR * 100) + "%"), "sx-dw-stamp");
        head.setWrapText(true); head.setMaxWidth(330);
        nodes.add(head);

        GridPane g = new GridPane(); g.setHgap(1); g.setVgap(1);
        for (int h = 0; h < 24; h++) {                                         // hour axis, every three hours; the current hour marked
            Label hl = lbl(h == hr ? "▼" : (h % 3 == 0 ? String.format("%02d", h) : ""), "sx-bc-hh");
            hl.setStyle("-fx-font-size:9px;" + (h == hr ? "-fx-text-fill:-ars-accent;" : ""));
            hl.setMinWidth(CELL_W); hl.setPrefWidth(CELL_W); hl.setAlignment(Pos.CENTER); hl.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            g.add(hl, h + 1, 0);
        }
        int row = 1;
        for (String b : ClusterClient.HEAT_BANDS) {
            Double mhz = freq(b); if (mhz == null) continue;
            Label bn = lbl(b, "sx-bc-bn"); bn.setMinWidth(34); g.add(bn, 0, row);
            for (int h = 0; h < 24; h++) {
                double r = Propagation.reliability(mhz, day.get(h));
                Region cell = new Region(); cell.setMinSize(CELL_W, CELL_H); cell.setPrefSize(CELL_W, CELL_H);
                cell.setStyle("-fx-background-color:" + color(r) + ";-fx-background-radius:2;" + (h == hr ? "-fx-border-color:-ars-accent;-fx-border-radius:2;" : ""));
                Tips.tip(cell, String.format(Locale.US, "%s at %02d:00Z — %d%% likely\nMUF %.1f MHz, LUF %.1f MHz", b, h, Math.round(r * 100), day.get(h).mufMhz(), day.get(h).lufMhz()));
                g.add(cell, h + 1, row);
            }
            row++;
        }
        nodes.add(g);
        nodes.add(lbl(String.format(Locale.US, "SFI %.0f · SSN %.0f · K %.0f · UTC hours · green = likely open, red = unlikely, grey = shut", w.sfi(), w.ssn(), w.k()), "sx-dw-stamp"));
        Label why = note("An estimate, not a forecast: worked out here from the solar flux, sunspot number and K index, a simple model of the ionosphere and the great-circle path. "
                + "It is not VOACAP, and it leaves out sporadic E, the winter anomaly, polar paths, noise and your power and antennas.");
        nodes.add(why);
        return nodes;
    }

    private static Double freq(String band) {
        Long hz = RigClient.bandHz(band.replace("m", ""));
        return hz == null ? null : hz / 1e6;
    }

    private static Label note(String text) {
        Label l = lbl(text, "sx-bc-na"); l.setWrapText(true); l.setMaxWidth(330); l.setPadding(new Insets(2, 0, 0, 0));
        return l;
    }

    /** Red (unlikely) through yellow to green (likely); nearly nothing is a dim grey. */
    static String color(double r) {
        if (r < 0.03) return "rgba(110,120,135,0.18)";
        Color c = Color.hsb(r * 120.0, 0.72, 0.62, 0.55 + 0.4 * r);
        return String.format(Locale.US, "rgba(%d,%d,%d,%.2f)", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255), c.getOpacity());
    }
}
