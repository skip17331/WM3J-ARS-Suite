package com.ars.fx.shell;

import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.RigClient;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The hero bar every module screen opens with (it is what {@link Shell#topBar} builds): the module's name and what it is looking at on the
 * left, its own figures as large readouts beside it, and on the right what every screen wants to see at a glance: the rig's frequency and
 * mode, the DX cluster feed, the transmit light, the station-ID timer and the UTC clock. It is the same banner the J-Hub dashboard
 * opens with, so the suite reads as one.
 *
 * <p>The right-hand readouts and the clock keep themselves current once a second, and stop when the bar leaves the screen. Surfaces that rebuild
 * often (J-Sat) get a fresh bar each time, so the ticker is cheap and tied to the bar's own life.
 */
final class HeroBar {
    private HeroBar() {}

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss"), DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy");
    /** Last scale: a rebuilt bar starts here rather than flash full size (J-Sat rebuilds every ~2 s). */
    private static double scale = 1;
    private static final double MIN_SCALE = 0.55;

    static HBox build(String hue, String glyphId, String name, String subtitle, String[][] stats, String clock) {
        HBox top = new HBox(); top.getStyleClass().addAll("sx-top", "hero-bar"); top.setAlignment(Pos.CENTER_LEFT);
        if (hue != null && !hue.isBlank()) top.getStyleClass().add("hue-" + hue);   // the bar's lower edge takes its module's colour

        // identity
        StackPane ic = Shell.iconTile(hue, glyphId, 22, "hero-ic");
        VBox id = new VBox(1, lbl(name, "hero-name"));
        if (subtitle != null && !subtitle.isBlank()) id.getChildren().add(lbl(subtitle, "hero-sub"));
        id.setAlignment(Pos.CENTER_LEFT);
        HBox brand = new HBox(14, ic, id); brand.setAlignment(Pos.CENTER_LEFT); brand.setPadding(new Insets(0, 26, 0, 0));
        top.getChildren().add(brand);

        List<VBox> cells = new ArrayList<>();       // everything that scales with the bar's width

        // the module's own figures
        for (String[] s : stats) {                   // {key, value, optional class}
            boolean hasCls = s.length > 2 && s[2] != null && !s[2].isBlank();
            VBox cell = new VBox(2, lbl(s[0], "hero-k"), lbl(s[1], hasCls ? new String[]{"hero-val", s[2]} : new String[]{"hero-val"}));
            cell.getStyleClass().add("hero-stat"); cell.setAlignment(Pos.CENTER_LEFT);
            cells.add(cell); top.getChildren().add(cell);
        }
        Region sp = new Region(); HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS); sp.setMinWidth(8);
        top.getChildren().add(sp);

        // solo windows have no dock, so the gear that reaches J-Hub / Station settings lives in the bar
        if (Shell.solo) {
            Label gear = lbl("⚙", "sx-theme"); gear.setStyle("-fx-cursor:hand;");
            javafx.scene.control.Tooltip.install(gear, new javafx.scene.control.Tooltip("J-Hub · Station settings"));
            Tips.tip(gear, "Open J-Hub (dashboard and settings)");
            gear.setOnMouseClicked(e -> Shell.navigate("hub"));
            HBox.setMargin(gear, new Insets(0, 14, 0, 0));
            top.getChildren().add(gear);
        }

        // what every screen shows: the rig, the cluster, the transmit light, the ID timer, the clock
        Label rigF = lbl("—", "hero-val", "rig"), rigM = lbl("rig offline", "hero-rig-sub");
        VBox rig = new VBox(2, lbl("RIG", "hero-k"), rigF, rigM); rig.getStyleClass().add("hero-stat");
        Region dot = new Region(); dot.getStyleClass().add("jhub-mini-dot"); dot.setMinSize(9, 9); dot.setPrefSize(9, 9); dot.setMaxSize(9, 9);
        Label cluTxt = lbl("", "hero-sub");
        HBox cluRow = new HBox(7, dot, cluTxt); cluRow.setAlignment(Pos.CENTER_LEFT);
        VBox clu = new VBox(5, lbl("DX CLUSTER", "hero-k"), cluRow); clu.getStyleClass().add("hero-stat");
        Label clk = lbl(CLOCK.format(LocalTime.now(ZoneOffset.UTC)), "hero-clk"), date = lbl("", "hero-sub");
        VBox clockBox = new VBox(1, lbl("UTC", "hero-k"), clk, date); clockBox.setAlignment(Pos.TOP_RIGHT); clockBox.getStyleClass().add("hero-stat");
        HeroDevices.Set devices = HeroDevices.build("hero-val");          // rotor / satellite rotor / amp, each only if switched on on its Setup page
        if (HeroDevices.showRig()) { cells.add(rig); top.getChildren().add(rig); }
        cells.addAll(devices.cells()); top.getChildren().addAll(devices.cells());
        cells.add(clu); cells.add(clockBox);
        top.getChildren().add(clu);
        top.getChildren().add(TxIndicator.build());
        Label idTimer = IdTimer.button(); HBox.setMargin(idTimer, new Insets(0, 10, 0, 34));
        top.getChildren().add(idTimer);
        top.getChildren().add(clockBox);
        for (Node n : cells) HBox.setMargin(n, new Insets(0, 0, 0, 0));

        Runnable rigNow = () -> {
            RigClient.State st = RigClient.getInstance().last();
            boolean on = st != null && st.connected() && st.freqHz() > 0;
            rigF.setText(on ? RigClient.fmtFreq(st.freqHz()) : "—");
            String mode = on && st.mode() != null ? st.mode().trim() : "";
            rigM.setText(on ? (mode.isEmpty() ? "MHz" : "MHz  ·  " + mode) : "rig offline");
        };
        java.util.function.Consumer<RigClient.State> rigLive = st -> rigNow.run();          // the readout follows the rig as it changes, not once a second
        rig.sceneProperty().addListener((o, was, now) -> {
            RigClient.getInstance().removeListener(rigLive);
            if (now != null) RigClient.getInstance().addListener(rigLive);
        });
        Runnable tick = () -> {
            clk.setText(CLOCK.format(LocalTime.now(ZoneOffset.UTC)));
            date.setText(DATE.format(LocalDate.now(ZoneOffset.UTC)));
            rigNow.run();
            ClusterClient cc = ClusterClient.getInstance();
            FeedUi.paint(dot, cc.status());
            cluTxt.setText(cc.status().label() + "  ·  " + cc.spots().size() + " spots");
            devices.update().run();
        };
        tick.run();
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick.run()));
        tl.setCycleCount(Timeline.INDEFINITE); tl.play();
        top.sceneProperty().addListener((o, was, sc) -> { if (sc == null) tl.stop(); else tl.play(); });

        fitToWidth(top, cells);
        return top;
    }

    /** Shrink the readouts (and their gaps) so the row fits the bar, instead of cutting values to "…". */
    private static void fitToWidth(HBox top, List<VBox> cells) {
        final double gap = 30;
        Runnable apply = () -> {
            double s = scale;
            for (VBox c : cells) {
                c.setMinWidth(Region.USE_PREF_SIZE);        // a value is never cut; the subtitle and the spacer give way first
                c.setMaxHeight(Region.USE_PREF_SIZE);
                HBox.setMargin(c, new Insets(0, 0, 0, gap * s));
                for (Node n : c.getChildren()) {
                    if (!(n instanceof Label l)) continue;
                    double full = l.getStyleClass().contains("hero-clk") ? 30 : l.getStyleClass().contains("hero-val") ? 24 : 0;
                    l.setStyle(full > 0 ? String.format("-fx-font-size:%.1fpx;", full * s) : "");
                }
            }
        };
        top.widthProperty().addListener((o, a, b) -> {
            double row = 0, others = top.getInsets().getLeft() + top.getInsets().getRight() + 70;
            for (VBox c : cells) row += c.prefWidth(-1) + gap * scale;
            for (Node n : top.getChildren()) if (!cells.contains(n) && !(n instanceof Region r && r.getMaxWidth() == Double.MAX_VALUE)) others += Math.min(n.prefWidth(-1), n == top.getChildren().get(0) ? 150 : 400);
            double s = Math.max(MIN_SCALE, Math.min(1, (b.doubleValue() - others) / (row / scale * 1.03)));
            if (b.doubleValue() <= 1 || Math.abs(s - scale) < 0.01) return;
            scale = s; apply.run();
        });
        apply.run();
    }
}
