package com.ars.fx.surface;

import com.ars.fx.data.BandMapModel;
import com.ars.fx.data.BandMapModel.Kind;
import com.ars.fx.data.BandMapModel.Mark;
import com.ars.fx.data.BandMapModel.Placed;
import com.ars.fx.data.Bands;
import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.RigClient;
import com.ars.fx.shell.Tips;
import com.jlog.bandplan.BandplanLoader;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The band map: spots laid out along a band by frequency, on a scaled ruler. Labels that would overlap are pushed apart with a leader line
 * back to the true frequency; new spots are bright and old ones fade; needed spots (new entity / band / mode, or a new multiplier in a
 * contest) are green and bold, ones you have already worked are dim; the rig's frequency is marked, and the map follows it from band to
 * band unless a band is picked. Click a spot to use it (fills the log entry, tunes the rig). The layout is {@link BandMapModel}.
 */
public final class BandMapView extends VBox {
    private static final String[] BANDS = {"Rig", "160m", "80m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m"};
    private static final double GAP = 17, RULER_X = 62, LABEL_X = RULER_X + 30, NEAR_HZ = 20_000;
    private static final Map<String, long[]> EDGES = new HashMap<>();

    private final BiConsumer<Node, Mark> onPick;
    private final Pane pane = new Pane();
    private final Label head = lbl("", "ct-note");
    private List<Mark> marks = List.of();
    private String choice = HubConfig.get("data.bandmapBand", "Rig");
    private boolean near = HubConfig.getBool("data.bandmapNear", false);
    private long drawnRigHz = -1;
    private final Consumer<RigClient.State> rigListener = s -> Platform.runLater(() -> {
        long f = s != null && s.connected() ? s.freqHz() : 0;
        if (Math.abs(f - drawnRigHz) >= 50) draw();                  // the rig moved: the marker, and maybe the band, change
    });

    public BandMapView(BiConsumer<Node, Mark> onPick) {
        super(6);
        this.onPick = onPick;
        // compact chips that wrap: the cluster column is narrow, and a row of ten full-size buttons would force it wider
        FlowPane bands = new FlowPane(4, 4);
        List<Label> chips = new java.util.ArrayList<>();
        for (String b : BANDS) {
            Label c = lbl(b, "ct-note");
            c.setOnMouseClicked(ev -> {
                choice = b; HubConfig.set("data.bandmapBand", choice);
                for (Label x : chips) styleChip(x, x.getText().equals(choice));
                draw();
            });
            styleChip(c, b.equals(choice));
            chips.add(c); bands.getChildren().add(c);
        }
        Tips.tip(chips.get(0), "Follow the rig: the map shows whichever band the rig is on");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        Label nearL = lbl("Near rig", "ct-note");
        Tips.tip(nearL, "Zoom to 20 kHz either side of the rig's frequency");
        HBox row = new HBox(8, head, gap, nearL, JHubDashboard.switchToggle(near, v -> { near = v; HubConfig.setBool("data.bandmapNear", v); draw(); }));
        row.setAlignment(Pos.CENTER_LEFT);
        pane.setMinHeight(140); VBox.setVgrow(pane, Priority.ALWAYS);
        Rectangle clip = new Rectangle(); clip.widthProperty().bind(pane.widthProperty()); clip.heightProperty().bind(pane.heightProperty());
        pane.setClip(clip);
        pane.widthProperty().addListener((o, a, b) -> draw());
        pane.heightProperty().addListener((o, a, b) -> draw());
        getChildren().addAll(bands, row, pane);
        // follow the rig only while on screen: no listener is left behind when the map is closed
        sceneProperty().addListener((o, was, now) -> {
            if (now != null) RigClient.getInstance().addListener(rigListener); else RigClient.getInstance().removeListener(rigListener);
        });
    }

    private static void styleChip(Label c, boolean on) {
        c.setStyle("-fx-font-size:12px;-fx-cursor:hand;-fx-padding:2 8 2 8;-fx-background-radius:10;"
                + (on ? "-fx-background-color:-ars-accent;-fx-text-fill:-ars-bg;-fx-font-weight:bold;"
                      : "-fx-background-color:-ars-surface-3;-fx-text-fill:-ars-t2;"));
    }

    /** New spot data (call on the UI thread). */
    public void update(List<Mark> all) { marks = all == null ? List.of() : all; draw(); }

    private void draw() {
        double w = pane.getWidth(), h = pane.getHeight();
        pane.getChildren().clear();
        RigClient.State rs = RigClient.getInstance().last();
        long rig = rs != null && rs.connected() && rs.freqHz() > 0 ? rs.freqHz() : 0;
        drawnRigHz = rig;
        long seed = seed(rig);
        long[] e = seed > 0 ? edges(seed) : null;
        if (e == null) { head.setText("Band map"); message(w, "No spots to map yet — connect the DX cluster, or pick a band"); return; }
        long lo = e[0], hi = e[1];
        String band = Bands.of(seed);
        boolean zoom = near && rig >= lo && rig <= hi;
        if (zoom) { lo = Math.max(lo, rig - (long) NEAR_HZ); hi = Math.min(hi, rig + (long) NEAR_HZ); }
        if (w < 120 || h < 60) return;

        long now = System.currentTimeMillis();
        List<Mark> chosen = BandMapModel.select(marks, lo, hi, now, Math.max(1, (int) (h / GAP)));
        List<Placed> placed = BandMapModel.layout(chosen, lo, hi, h - GAP, GAP, now);
        head.setText(band + " · " + mhz(lo) + "–" + mhz(hi) + " MHz · " + placed.size() + (placed.size() == 1 ? " spot" : " spots") + (zoom ? " · near rig" : ""));
        final double top = GAP / 2, usable = h - GAP, span = Math.max(1, hi - lo);

        Line axis = new Line(RULER_X, top, RULER_X, top + usable); axis.setStyle("-fx-stroke:-ars-border-2;"); pane.getChildren().add(axis);
        long step = BandMapModel.tickStepHz(lo, hi, usable, 38);
        for (long t = BandMapModel.firstTick(lo, step); t <= hi; t += step) {
            double y = top + (t - lo) / (double) span * usable;
            Line tick = new Line(RULER_X - 5, y, RULER_X, y); tick.setStyle("-fx-stroke:-ars-t3;");
            Label tl = lbl(mhz(t), "ct-note"); tl.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-t3;");
            tl.setLayoutX(0); tl.setLayoutY(y - 8); tl.setPrefWidth(RULER_X - 8); tl.setAlignment(Pos.CENTER_RIGHT);
            pane.getChildren().addAll(tick, tl);
        }
        for (Placed p : placed) {
            Mark m = p.mark();
            double ty = top + p.trueY(), ly = top + p.labelY();
            Line lead = new Line(RULER_X, ty, LABEL_X - 3, ly); lead.setStyle("-fx-stroke:-ars-t4;-fx-stroke-width:0.7;"); lead.setOpacity(p.alpha());
            String text = (m.kind() == Kind.NEEDED ? "★ " : "") + String.format("%.1f", m.freqHz() / 1000.0) + "  " + m.call();
            Label l = lbl(text, "ct-note");
            String color = m.kind() == Kind.NEEDED ? "-ars-ok" : m.kind() == Kind.WORKED ? "-ars-t3" : "-ars-t1";
            l.setStyle("-fx-font-family:'JetBrains Mono';-fx-font-size:13px;-fx-cursor:hand;-fx-text-fill:" + color + ";" + (m.kind() == Kind.NEEDED ? "-fx-font-weight:bold;" : ""));
            l.setOpacity(p.alpha());
            l.setLayoutX(LABEL_X); l.setLayoutY(ly - 9); l.setMaxWidth(Math.max(60, w - LABEL_X - 4));
            Tips.tip(l, m.call() + " · " + m.mode() + " · " + ClusterClient.fmtFreq(m.freqHz()) + " · " + ClusterClient.age(m.arrivalMs()) + " ago"
                    + (m.kind() == Kind.NEEDED ? " · needed" : m.kind() == Kind.WORKED ? " · already worked" : "") + " — click to use");
            l.setOnMouseClicked(ev -> onPick.accept(l, m));
            pane.getChildren().addAll(lead, l);
        }
        if (rig >= lo && rig <= hi) {                                      // where the rig is
            double y = top + (rig - lo) / (double) span * usable;
            Line rl = new Line(RULER_X - 8, y, w, y); rl.setStyle("-fx-stroke:-ars-accent;-fx-stroke-width:1.5;"); rl.setMouseTransparent(true);
            Label rt = lbl("▶ " + String.format("%.4f", rig / 1e6), "ct-note"); rt.setMouseTransparent(true);
            rt.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-accent;-fx-font-weight:bold;-fx-background-color:-ars-bg;");
            rt.setLayoutX(2); rt.setLayoutY(y - 9);
            pane.getChildren().addAll(rl, rt);
        }
    }

    /** A frequency inside the band to show: the rig's (when following it), the chosen band's, or the busiest band's; 0 if there is none. */
    private long seed(long rig) {
        if (!"Rig".equals(choice)) {
            Long hz = RigClient.bandHz(choice.replace("m", ""));
            return hz == null ? 0 : hz;
        }
        if (rig > 0 && !Bands.of(rig).isEmpty()) return rig;
        String busiest = BandMapModel.busiestBand(marks);
        if (busiest == null) return 0;
        for (Mark m : marks) if (Bands.of(m.freqHz()).equals(busiest)) return m.freqHz();
        return 0;
    }

    private static long[] edges(long seed) {
        String key = BandplanLoader.activeRegion() + "|" + Bands.of(seed);
        return EDGES.computeIfAbsent(key, k -> BandMapModel.edges(seed));
    }

    private void message(double w, String text) {
        Label l = lbl(text, "ct-note"); l.setWrapText(true); l.setMaxWidth(Math.max(120, w - 20)); l.setLayoutX(10); l.setLayoutY(10);
        pane.getChildren().add(l);
    }
    private static String mhz(long hz) { return String.format("%.3f", hz / 1e6); }
}
