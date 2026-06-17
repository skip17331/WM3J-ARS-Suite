package com.ars.fx.shell;

import com.ars.fx.data.HubConfig;
import com.ars.fx.data.RotorClient;
import com.ars.fx.data.SolarData;
import com.ars.fx.data.SunImageService;
import com.ars.fx.data.WeatherData;
import com.ars.fx.mock.Mock;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared shell builders — 1:1 with suite-shell.jsx (SuiteDock, SuiteDrawer,
 * SuiteInstruments, top bar). Pure view code over mock data.
 */
public final class Shell {
    private Shell() {}

    /** Navigation + theme hooks, set by the host (Launcher). Dock items and the
     *  J-Hub nav call navigate(id) to switch surface; toggleTheme() flips light/dark. */
    public static java.util.function.Consumer<String> onNavigate;
    /** Host re-applies the effective scheme to the scene root after a theme change. */
    public static Runnable onThemeChanged;
    /** Id of the surface currently on screen (set by the host on every show); spot-click prefill checks it. */
    public static volatile String currentSurface = "hub";
    /** Solo mode: a single module launched on its own (J-Map/J-Sat) — no module dock, no cross-nav. */
    public static volatile boolean solo = false;
    public static void navigate(String id) { if (onNavigate != null) onNavigate.accept(id); }

    /** Dashboard theme button: advance the suite-wide scheme (clears per-module overrides). */
    public static com.ars.fx.Themes.Scheme cycleGlobalTheme() {
        com.ars.fx.Themes.Scheme s = com.ars.fx.Themes.cycleGlobal();
        if (onThemeChanged != null) onThemeChanged.run();
        return s;
    }
    /** Module theme button: advance the current module's own scheme (remembered until changed). */
    public static com.ars.fx.Themes.Scheme cycleModuleTheme() {
        com.ars.fx.Themes.Scheme s = com.ars.fx.Themes.cycleModule(currentSurface);
        if (onThemeChanged != null) onThemeChanged.run();
        return s;
    }

    /** Module hue name -> Color (glyph strokes are set in Java; CSS handles tile bg). */
    public static final Map<String,Color> HUE = Map.ofEntries(
        Map.entry("log",   Color.web("#ebb353")), Map.entry("map",    Color.web("#26c0cf")),
        Map.entry("digi",  Color.web("#b191ea")), Map.entry("bridge", Color.web("#6fa7ee")),
        Map.entry("sat",   Color.web("#6fc884")), Map.entry("vault",  Color.web("#dba475")),
        Map.entry("learn", Color.web("#ef7d83")), Map.entry("hub",    Color.web("#a2c4d0")),
        Map.entry("accent",Color.web("#31bdda")), Map.entry("t2",     Color.web("#97a4b2")));

    // ---- small helpers -----------------------------------------------------
    public static Label lbl(String text, String... classes) {
        Label l = new Label(text); l.getStyleClass().addAll(classes); return l;
    }
    static Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); VBox.setVgrow(r, Priority.ALWAYS); return r; }
    static Region hsp()    { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }

    /** A rounded hue-tinted icon tile holding a module glyph. */
    public static StackPane iconTile(String hue, String glyphId, double glyphSize, String... tileClasses) {
        Color c = HUE.getOrDefault(hue, HUE.get("t2"));
        StackPane tile = new StackPane(Glyphs.glyph(glyphId, glyphSize, c));
        tile.getStyleClass().addAll(tileClasses);
        if (!hue.equals("gear")) tile.getStyleClass().add(hue);
        return tile;
    }

    // ---- shell frame assembly: dock | (top + [center | rail] + bottom) ----
    public static Region frame(Region dock, HBox top, Region center, Region rail) {
        return frame(dock, top, center, rail, null);
    }
    public static Region frame(Region dock, HBox top, Region center, Region rail, Region bottom) {
        HBox.setHgrow(center, Priority.ALWAYS);
        Region body = (rail == null) ? center : new HBox(center, rail);
        if (body instanceof HBox hb) hb.setFillHeight(true);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox modwin = new VBox(top, body); modwin.getStyleClass().add("sx-modwin");
        if (bottom != null) modwin.getChildren().add(bottom);
        HBox.setHgrow(modwin, Priority.ALWAYS); VBox.setVgrow(modwin, Priority.ALWAYS);
        // solo mode (J-Map/J-Sat on their own) builds without the module dock
        HBox root = (dock == null) ? new HBox(modwin) : new HBox(dock, modwin);
        root.getStyleClass().add("sx-shell"); root.setFillHeight(true);
        return root;
    }

    // ---- module dock (collapsed icon rail, hover-expands) ------------------
    public static Region dock(String activeId) {
        if (solo) return null;   // solo module windows have no dock
        VBox dock = new VBox(); dock.getStyleClass().add("sx-dock");
        dock.setMinWidth(58); dock.setPrefWidth(58); dock.setMaxWidth(58);
        List<Node> expandOnly = new ArrayList<>();   // names + section labels + dots

        // J-Hub at top
        dock.getChildren().add(dockItem("hub", "J-Hub", "hub", true, activeId, expandOnly));
        Label sec = lbl("MODULES", "sx-dock-sec"); sec.setManaged(false); sec.setVisible(false); expandOnly.add(sec);
        dock.getChildren().add(sec);
        for (Mock.Mod m : com.ars.fx.data.ModuleConfig.enabledModules())
            dock.getChildren().add(dockItem(m.id(), m.name(), m.hue(), m.running(), activeId, expandOnly));
        dock.getChildren().add(spacer());
        // Station settings foot
        VBox foot = new VBox(dockItemRaw("gear", "Station settings", "gear", false, activeId, expandOnly));
        foot.getStyleClass().add("sx-dock-foot");
        dock.getChildren().add(foot);

        dock.setOnMouseEntered(e -> { setExpanded(dock, expandOnly, true); });
        dock.setOnMouseExited(e -> { setExpanded(dock, expandOnly, false); });
        return dock;
    }
    private static void setExpanded(VBox dock, List<Node> nodes, boolean exp) {
        double w = exp ? 216 : 58;
        dock.setMinWidth(w); dock.setPrefWidth(w); dock.setMaxWidth(w);
        for (Node n : nodes) { n.setManaged(exp); n.setVisible(exp); }
    }
    private static HBox dockItem(String id, String name, String hue, boolean running, String active, List<Node> expandOnly) {
        return dockItemRaw(id, name, hue, running, active, expandOnly);
    }
    private static HBox dockItemRaw(String id, String name, String hue, boolean running, String active, List<Node> expandOnly) {
        StackPane ic = iconTile(hue, hue.equals("gear") ? "gear" : id, 20, "sx-dock-ic");
        if (!running && !id.equals(active)) ic.setOpacity(0.62);
        Label nm = lbl(name, "sx-dock-name"); nm.setManaged(false); nm.setVisible(false); HBox.setHgrow(nm, Priority.ALWAYS); nm.setMaxWidth(Double.MAX_VALUE);
        Region dot = new Region(); dot.getStyleClass().addAll("sx-dock-dot"); if (running) dot.getStyleClass().add("on");
        dot.setManaged(false); dot.setVisible(false);
        expandOnly.add(nm); if (!hue.equals("gear")) expandOnly.add(dot);
        HBox row = new HBox(12, ic, nm, dot); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("sx-dock-item");
        if (!hue.equals("gear")) row.getStyleClass().add(hue);
        if (id.equals(active)) row.getStyleClass().add("active");
        VBox.setMargin(row, new Insets(2, 9, 2, 9));
        if (!hue.equals("gear")) row.setOnMouseClicked(e -> navigate(id));   // dock = navigation
        else { row.setStyle("-fx-cursor:hand;"); row.setOnMouseClicked(e -> navigate("station")); }   // gear → Station settings
        return row;
    }

    // ---- top bar -----------------------------------------------------------
    public static HBox topBar(String hue, String glyphId, String name, String subtitle,
                              String[][] stats, String clock) {
        HBox top = new HBox(); top.getStyleClass().add("sx-top"); top.setAlignment(Pos.CENTER_LEFT);
        StackPane bic = iconTile(hue, glyphId, 18, "sx-brand-ic");
        VBox bt = new VBox(1, lbl(name, "sx-brand-nm"));
        if (subtitle != null && !subtitle.isBlank()) bt.getChildren().add(lbl(subtitle, "sx-brand-ct"));
        bt.setAlignment(Pos.CENTER_LEFT);
        HBox brand = new HBox(10, bic, bt); brand.setAlignment(Pos.CENTER_LEFT); brand.setPadding(new Insets(0, 20, 0, 0));
        top.getChildren().add(brand);
        for (String[] s : stats) {   // {key, value, optional class}
            boolean hasCls = s.length > 2 && s[2] != null && !s[2].isBlank();
            VBox cell = new VBox(2, lbl(s[0], "sx-stat-k"),
                    lbl(s[1], hasCls ? new String[]{"sx-stat-v", s[2]} : new String[]{"sx-stat-v"}));
            cell.getStyleClass().add("sx-stat"); cell.setAlignment(Pos.CENTER_LEFT);
            top.getChildren().add(cell);
        }
        top.getChildren().add(hsp());
        // canonical rig panel: drops down from this top-right button (replaces the rail rig drawer)
        Label rig = rigButton(); HBox.setMargin(rig, new Insets(0, 14, 0, 0));
        top.getChildren().add(rig);
        // per-module theme: shows the module's current scheme, cycles just this module on click
        Label theme = lbl("◑ " + com.ars.fx.Themes.effective(hue).label(), "sx-theme"); theme.setStyle("-fx-cursor:hand;");
        theme.setOnMouseClicked(e -> theme.setText("◑ " + cycleModuleTheme().label()));
        HBox.setMargin(theme, new Insets(0, 16, 0, 0));
        top.getChildren().add(theme);
        if (clock != null) {
            VBox c = new VBox(1, lbl("UTC", "sx-clock-k"), lbl(clock, "sx-clock-v"));
            c.setAlignment(Pos.CENTER_RIGHT); top.getChildren().add(c);
        }
        return top;
    }

    // ---- collapsible drawer ------------------------------------------------
    // Remembered open/closed state by title, so a drawer the operator opened stays
    // open when its surface rebuilds (e.g. J-Sat recreates its rail every ~2 s).
    private static final Map<String, Boolean> DRAWER_STATE = new java.util.HashMap<>();

    public static VBox drawer(String title, String hue, String glyphId, String summary, boolean open, Node body) {
        return drawer(title, hue, glyphId, summary, open, body, null);
    }
    /** As above, with an optional {@code onToggle}. Open/closed state is remembered by title across rebuilds. */
    public static VBox drawer(String title, String hue, String glyphId, String summary, boolean open, Node body,
                              java.util.function.Consumer<Boolean> onToggle) {
        boolean initOpen = DRAWER_STATE.getOrDefault(title, open);
        VBox dw = new VBox(); dw.getStyleClass().add("sx-dw"); if (initOpen) dw.getStyleClass().add("open");
        StackPane ic = iconTile(hue, glyphId, 14, "sx-dw-ic");
        Label t = lbl(title, "sx-dw-t"); HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE);
        Label sum = lbl(summary == null ? "" : summary, "sx-dw-sum");
        Label cv = lbl("›", "sx-dw-cv");   // ›
        HBox head = new HBox(9, ic, t, sum, cv); head.setAlignment(Pos.CENTER_LEFT); head.getStyleClass().add("sx-dw-head");
        VBox bodyBox = new VBox(); bodyBox.getStyleClass().add("sx-dw-body"); bodyBox.getChildren().add(body);
        cv.setRotate(initOpen ? 90 : 0); sum.setVisible(!initOpen); sum.setManaged(!initOpen);
        bodyBox.setManaged(initOpen); bodyBox.setVisible(initOpen);
        head.setOnMouseClicked(e -> {
            boolean nowOpen = !dw.getStyleClass().contains("open");
            dw.getStyleClass().remove("open"); if (nowOpen) dw.getStyleClass().add("open");
            bodyBox.setManaged(nowOpen); bodyBox.setVisible(nowOpen);
            sum.setVisible(!nowOpen); sum.setManaged(!nowOpen); cv.setRotate(nowOpen ? 90 : 0);
            DRAWER_STATE.put(title, nowOpen);
            if (onToggle != null) onToggle.accept(nowOpen);
        });
        dw.getChildren().addAll(head, bodyBox);
        return dw;
    }

    /** Right-rail container that scrolls. */
    public static ScrollPane rail(Node... drawers) {
        VBox box = new VBox(9, drawers); box.getStyleClass().add("sx-rail");
        ScrollPane sp = new ScrollPane(box); sp.setFitToWidth(true); sp.getStyleClass().add("sx-rail-scroll");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setMinWidth(322); sp.setPrefWidth(322); sp.setMaxWidth(322);
        return sp;
    }

    // ---- standard instrument drawers --------------------------------------
    public static List<Node> instruments(int az) {
        // Weather (live Open-Meteo at the station location)
        WeatherData.getInstance().start();
        WeatherData.Snapshot w = WeatherData.getInstance().snapshot();
        String grid = HubConfig.grid();
        HBox wxBig; VBox wxBody; String wxSum;
        if (w == null) {
            wxBig = new HBox(8, lbl("—°","sx-ro-head"), lbl("loading…","sx-ro-dir")); wxBig.setAlignment(Pos.BOTTOM_LEFT);
            wxBody = new VBox(wxBig); wxSum = "loading…";
        } else {
            wxBig = new HBox(8, lbl(Math.round(w.tempC())+"°","sx-ro-head"), lbl(w.desc()+" · feels "+Math.round(w.feelsC())+"°","sx-ro-dir")); wxBig.setAlignment(Pos.BOTTOM_LEFT);
            wxBody = new VBox(wxBig, kv("Wind", w.windCompass()+" "+Math.round(w.windKmh())+" · g"+Math.round(w.gustKmh()), false),
                    kv("Humidity", w.humidity()+"%", false), kv("Pressure", Math.round(w.pressure())+" hPa", false));
            wxSum = Math.round(w.tempC())+"° "+w.windCompass()+Math.round(w.windKmh());
        }

        return List.of(
            propagationDrawer(),
            solarSpaceWeatherDrawer(),
            bandConditionsDrawer(),
            drawer("Weather · " + grid,"bridge","bridge", wxSum, false, wxBody));
    }

    /** Station-ID timer drawer — the first standard drawer on every operating module. */
    public static Node idDrawer() { return com.ars.fx.shell.IdTimer.drawer(); }

    /** Lead drawers shown first on every operating module: Station ID, Rotor control.
     *  Rig control is no longer a rail drawer — it drops down from the top-bar RIG button
     *  ({@link #rigButton()} / {@link RigPanel}). */
    public static List<Node> leadDrawers(int az) { return List.of(idDrawer(), rotorControlDrawer(az)); }

    /** Top-right RIG button that toggles the canonical {@link RigPanel} drop-down. Shared by
     *  the module top bar ({@link #topBar}) and the J-Hub dashboard bar. */
    public static Label rigButton() {
        Label b = lbl("⏚ RIG", "sx-rigbtn"); b.setStyle("-fx-cursor:hand;");
        b.setOnMouseClicked(e -> toggleRigOverlay(b));
        return b;
    }

    private static final String RIG_OVERLAY_ID = "rig-overlay";
    /** Show (or hide, if already open) the rig panel as a top-right drop-down over the current surface. */
    public static void toggleRigOverlay(Node source) {
        javafx.scene.Scene sc = source == null ? null : source.getScene();
        if (sc == null || !(sc.getRoot() instanceof Pane root)) return;
        for (Node n : new ArrayList<>(root.getChildrenUnmodifiable()))
            if (RIG_OVERLAY_ID.equals(n.getId())) { root.getChildren().remove(n); return; }   // toggle off

        Region panel = RigPanel.build(() -> root.getChildren().removeIf(n -> RIG_OVERLAY_ID.equals(n.getId())));
        ScrollPane sp = new ScrollPane(panel); sp.getStyleClass().add("rig-overlay-scroll");
        sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setMinWidth(RigPanel.WIDTH + 2); sp.setMaxWidth(RigPanel.WIDTH + 2);
        sp.maxHeightProperty().bind(root.heightProperty().subtract(72));

        StackPane veil = new StackPane(sp); veil.setId(RIG_OVERLAY_ID);
        veil.setStyle("-fx-background-color: transparent;"); veil.setPickOnBounds(true);
        veil.prefWidthProperty().bind(root.widthProperty()); veil.prefHeightProperty().bind(root.heightProperty());
        StackPane.setAlignment(sp, Pos.TOP_RIGHT); StackPane.setMargin(sp, new Insets(58, 12, 0, 0));
        veil.setOnMouseClicked(e -> { if (e.getTarget() == veil) root.getChildren().remove(veil); });   // click-outside dismiss
        root.getChildren().add(veil);
    }

    /** Live rotor-control drawer: compass, CCW/Stop/CW, presets. Shared across modules. */
    public static Node rotorControlDrawer(int az) {
        String dir = new String[]{"N","NE","E","SE","S","SW","W","NW"}[(int)Math.round(((az%360+360)%360)/45.0)%8];
        String az3 = String.format("%03d", (az%360+360)%360);
        StackPane roHolder = new StackPane(); roHolder.setAlignment(Pos.TOP_LEFT);
        java.util.function.Consumer<RotorClient.State> renderRo = st -> {
            boolean live = st != null && st.connected();
            int curAz = live ? (int) Math.round(((st.az() % 360) + 360) % 360) : az;
            String d = new String[]{"N","NE","E","SE","S","SW","W","NW"}[(int) Math.round((curAz % 360) / 45.0) % 8];
            VBox roInfo = new VBox(0); roInfo.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(roInfo, Priority.ALWAYS);
            HBox head = new HBox(2, lbl(String.format("%03d", curAz), "sx-ro-head"), lbl("°", "sx-ro-dir")); head.setAlignment(Pos.BOTTOM_LEFT);
            Label ccw = btn("◀ CCW","sx-ro-turn-btn"), stp = btn("Stop","sx-ro-turn-btn","stop"), cw = btn("CW ▶","sx-ro-turn-btn");
            ccw.setStyle("-fx-cursor:hand;"); stp.setStyle("-fx-cursor:hand;"); cw.setStyle("-fx-cursor:hand;");
            ccw.setOnMouseClicked(e -> RotorClient.getInstance().nudge(-15));
            stp.setOnMouseClicked(e -> RotorClient.getInstance().stop());
            cw.setOnMouseClicked(e -> RotorClient.getInstance().nudge(15));
            HBox turn = new HBox(5, ccw, stp, cw); for (Node n : turn.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
            roInfo.getChildren().addAll(head, lbl(d + (live ? " · live" : " · rotctld offline"), "sx-ro-dir"), turn);
            VBox.setMargin(turn, new Insets(10,0,0,0));
            HBox roTop = new HBox(13, compass(curAz, 88), roInfo); roTop.setAlignment(Pos.CENTER_LEFT);
            GridPane presets = new GridPane(); presets.setHgap(5); presets.setVgap(5);
            int i = 0; for (Object[] p : Mock.ROTOR_PRESETS) {
                final int paz = ((Number) p[1]).intValue();
                Label b = btn(p[0]+" "+paz+"°", "sx-ro-preset"); GridPane.setHgrow(b, Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE);
                b.setStyle("-fx-cursor:hand;"); b.setOnMouseClicked(e -> RotorClient.getInstance().moveTo(paz));
                presets.add(b, i%3, i/3); i++;
            }
            for (int col=0; col<3; col++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(100/3.0); presets.getColumnConstraints().add(cc); }
            roHolder.getChildren().setAll(new VBox(11, roTop, presets));
        };
        renderRo.accept(null);
        RotorClient.getInstance().setListener(st -> javafx.application.Platform.runLater(() -> renderRo.accept(st)));
        RotorClient.getInstance().start();
        return drawer("Rotor control","sat","sat", az3+"° "+dir, false, new VBox(roHolder));
    }
    /** "Propagation": best band, MUF / FOT, geomagnetic, aurora, solar wind (shared so same-named drawers match). */
    public static Node propagationDrawer() {
        SolarData.getInstance().start();
        SolarData.Snapshot sd = SolarData.getInstance().snapshot();
        return drawer("Propagation", "map", "map", sd == null ? "loading…" : bestBand(sd), false, propagationBody(sd));
    }
    private static VBox propagationBody(SolarData.Snapshot sd) {
        if (sd == null) return new VBox(kv("Solar / space weather", "loading HamQSL…", false));
        double muf = SolarData.mufMhz(sd), fot = SolarData.fotMhz(sd);
        String mufStr = Double.isNaN(muf) ? "unavailable" : String.format("%.1f MHz", muf);
        String fotStr = Double.isNaN(fot) ? "—" : String.format("%.1f MHz", fot);
        return new VBox(kv("Best band (day)", bestBand(sd), true),
                   kv("MUF (3000 km)", mufStr, !Double.isNaN(muf)),
                   kv("FOT", fotStr, !Double.isNaN(fot)),
                   kv("Geomagnetic", SolarData.geomag(sd.kIndex()) + " · K" + nz(sd.kIndex()), "quiet".equals(SolarData.geomag(sd.kIndex()))),
                   kv("Aurora", SolarData.auroraDesc(sd.aurora()), "quiet".equals(SolarData.auroraDesc(sd.aurora()))),
                   kv("Solar wind", nz(sd.solarWind()) + " km/s", false));
    }

    /** Highest band group rated Good for daytime, from the HamQSL conditions. */
    private static String bestBand(SolarData.Snapshot s) {
        if (s == null || s.bands().isEmpty()) return "—";
        for (int i = s.bands().size() - 1; i >= 0; i--) {            // higher bands listed last
            SolarData.Band b = s.bands().get(i);
            if ("Good".equalsIgnoreCase(b.day())) return b.group() + " good";
        }
        return s.bands().get(0).group() + " " + s.bands().get(0).day().toLowerCase();
    }
    private static String nz(String s) { return (s == null || s.isBlank()) ? "—" : s.trim(); }

    // ---- canonical space-weather drawers (shared so same-named drawers match) ----
    /** "Solar &amp; space weather": real SDO sun disk + solar cell grid + source stamp. */
    public static Node solarSpaceWeatherDrawer() {
        SolarData.getInstance().start();
        SolarData.Snapshot s = SolarData.getInstance().snapshot();
        String sum = s == null ? "loading…" : "SFI " + nz(s.sfi()) + " · K" + nz(s.kIndex());
        return drawer("Solar & space weather", "digi", "digi", sum, false, solarSpaceWeatherBody());
    }
    public static VBox solarSpaceWeatherBody() {
        SolarData.getInstance().start();
        SolarData.Snapshot s = SolarData.getInstance().snapshot();
        String stamp = s != null ? "HamQSL · N0NBH · " + nz(s.updated()) : "HamQSL · N0NBH · loading…";
        return new VBox(9, sunspotDisk(), solarGrid(), lbl(stamp, "sx-dw-stamp"));
    }
    /** "Band conditions": band · day · night (good / fair / poor) table from HamQSL. */
    public static Node bandConditionsDrawer() {
        SolarData.getInstance().start();
        SolarData.Snapshot s = SolarData.getInstance().snapshot();
        return drawer("Band conditions", "map", "map", s == null ? "loading…" : bestBand(s), false, bandConditionsBody());
    }
    public static VBox bandConditionsBody() {
        SolarData.getInstance().start();
        Label hb = lbl("BAND", "sx-bc-bn"); HBox.setHgrow(hb, Priority.ALWAYS); hb.setMaxWidth(Double.MAX_VALUE);
        Label hd = lbl("DAY", "sx-swx-k"); hd.setMinWidth(60); hd.setAlignment(Pos.CENTER);
        Label hn = lbl("NIGHT", "sx-swx-k"); hn.setMinWidth(60); hn.setAlignment(Pos.CENTER);
        VBox box = new VBox(6, new HBox(7, hb, hd, hn));
        SolarData.Snapshot s = SolarData.getInstance().snapshot();
        if (s == null || s.bands().isEmpty()) box.getChildren().add(lbl("loading HamQSL band conditions…", "sx-swx-k"));
        else for (SolarData.Band b : s.bands()) box.getChildren().add(bandRow(b.group(), b.day(), b.night()));
        box.getChildren().add(bandHeatMap());
        return box;
    }

    /** DX-cluster activity heat map per band: cell colour scales with spot count.
     *  Marked unavailable when the cluster isn't loaded (we don't auto-connect). */
    public static Node bandHeatMap() {
        String[] bands = {"160m","80m","40m","30m","20m","17m","15m","12m","10m","6m"};
        com.ars.fx.data.ClusterClient cc = com.ars.fx.data.ClusterClient.getInstance();
        List<com.ars.fx.data.ClusterClient.Spot> spots = cc.spots();
        boolean available = cc.isConnected() || !spots.isEmpty();

        VBox sec = new VBox(6); VBox.setMargin(sec, new Insets(4, 0, 0, 0));
        sec.getChildren().add(lbl("DX ACTIVITY · " + (available ? spots.size() + " spots" : "cluster off"), "sx-bc-hh"));
        if (!available) {
            sec.getChildren().add(lbl("Connect the DX cluster for live band activity", "sx-bc-na"));
            return sec;
        }
        int[] counts = new int[bands.length]; int max = 0;
        for (com.ars.fx.data.ClusterClient.Spot sp : spots)
            for (int i = 0; i < bands.length; i++)
                if (bands[i].equals(sp.band())) { counts[i]++; if (counts[i] > max) max = counts[i]; }
        HBox r1 = new HBox(4), r2 = new HBox(4);
        for (int i = 0; i < bands.length; i++) (i < 5 ? r1 : r2).getChildren().add(heatCell(bands[i], counts[i], max));
        sec.getChildren().addAll(r1, r2);
        return sec;
    }
    private static Node heatCell(String band, int count, int max) {
        VBox cell = new VBox(1); cell.setAlignment(Pos.CENTER); cell.getStyleClass().add("sx-heat");
        HBox.setHgrow(cell, Priority.ALWAYS); cell.setMaxWidth(Double.MAX_VALUE);
        double frac = max <= 0 ? 0 : (double) count / max;
        cell.setStyle("-fx-background-color:" + heatColor(frac, count) + "; -fx-background-radius:4; -fx-padding:4 2 4 2;");
        Label bl = lbl(band.replace("m", ""), "sx-heat-b");
        Label cl = lbl(count > 0 ? String.valueOf(count) : "·", count > 0 ? "sx-heat-c" : "sx-heat-c0");
        cell.getChildren().addAll(bl, cl);
        return cell;
    }
    private static String heatColor(double frac, int count) {
        if (count <= 0) return "-ars-surface-2";
        double hue = (1.0 - frac) * 110.0;             // 110° green (low) … 0° red (busiest)
        Color c = Color.hsb(hue, 0.70, 0.58, 0.92);
        return String.format("rgba(%d,%d,%d,%.2f)", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255), c.getOpacity());
    }
    private static GridPane solarGrid() {
        GridPane swx = new GridPane(); swx.setHgap(7); swx.setVgap(7);
        SolarData.Snapshot s = SolarData.getInstance().snapshot();
        String[][] cells = s == null
            ? new String[][]{{"SFI","—"},{"SSN","—"},{"A-idx","—"},{"K-idx","—"},{"X-ray","—"},{"Aurora","—"},{"Sol wind","—"},{"Mag","—"}}
            : new String[][]{{"SFI",nz(s.sfi())},{"SSN",nz(s.sunspots())},{"A-idx",nz(s.aIndex())},{"K-idx",nz(s.kIndex())},
                             {"X-ray",nz(s.xray())},{"Aurora",nz(s.aurora())},{"Sol wind",nz(s.solarWind())},{"Mag",nz(s.magField())}};
        int j=0; for (String[] c2 : cells){ VBox c=new VBox(2,lbl(c2[0],"sx-swx-k"),lbl(c2[1],"sx-swx-v")); c.getStyleClass().add("sx-swx-c"); GridPane.setHgrow(c,Priority.ALWAYS); c.setMaxWidth(Double.MAX_VALUE); swx.add(c,j%4,j/4); j++; }
        for (int col=0; col<4; col++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(25); swx.getColumnConstraints().add(cc); }
        return swx;
    }
    /** Real SDO/HMI continuum solar disk; synthetic disk while loading / offline. */
    private static Region sunspotDisk() {
        StackPane host = new StackPane(); host.setMaxWidth(Double.MAX_VALUE); host.setMinHeight(158); host.setMaxHeight(158);
        Runnable render = () -> {
            Image img = SunImageService.getInstance().image();
            host.getChildren().setAll(img != null && !img.isError() ? sunReal(img) : syntheticDisk());
        };
        SunImageService.getInstance().setListener(render);
        SunImageService.getInstance().start();
        render.run();
        return host;
    }
    private static Region sunReal(Image img) {
        ImageView iv = new ImageView(img); iv.setPreserveRatio(true); iv.setFitHeight(150); iv.setSmooth(true);
        Label src = lbl("SDO · HMI continuum", "sx-dw-stamp"); StackPane.setAlignment(src, Pos.BOTTOM_LEFT); StackPane.setMargin(src, new Insets(0, 0, 5, 7));
        Label cr = lbl("NASA/SDO/HMI", "sx-dw-stamp"); StackPane.setAlignment(cr, Pos.BOTTOM_RIGHT); StackPane.setMargin(cr, new Insets(0, 7, 5, 0));
        StackPane sp = new StackPane(iv, src, cr); sp.setStyle("-fx-background-color:#06090d; -fx-background-radius:8;");
        sp.setMinHeight(158); sp.setMaxHeight(158); sp.setMaxWidth(Double.MAX_VALUE);
        return sp;
    }
    private static Region syntheticDisk() {
        double W = 290, H = 150;
        Canvas cv = new Canvas(W, H);
        GraphicsContext g = cv.getGraphicsContext2D();
        double cx = W / 2, cy = H / 2, R = 62;
        g.setFill(Color.web("#0b0f14")); g.fillRect(0, 0, W, H);
        RadialGradient rg = new RadialGradient(0, 0, cx, cy, R, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffe9ad")), new Stop(0.55, Color.web("#ffc15a")),
                new Stop(0.85, Color.web("#f59331")), new Stop(1, Color.web("#c9641f")));
        g.setFill(rg); g.fillOval(cx - R, cy - R, 2 * R, 2 * R);
        g.setStroke(Color.web("#ffd488", 0.5)); g.setLineWidth(1.5); g.strokeOval(cx - R, cy - R, 2 * R, 2 * R);
        double[][] spots = {{-20, -8, 9}, {-9, 2, 5}, {12, 16, 7}, {28, -18, 5}, {-30, 20, 4}, {18, -3, 3}};
        for (double[] s : spots) {
            if (Math.hypot(s[0], s[1]) + s[2] > R - 3) continue;
            double sx = cx + s[0], sy = cy + s[1], r = s[2];
            g.setFill(Color.web("#9c5a1e", 0.55)); g.fillOval(sx - r * 1.7, sy - r * 1.4, r * 3.4, r * 2.8);
            g.setFill(Color.web("#2a1808")); g.fillOval(sx - r, sy - r * 0.8, r * 2, r * 1.6);
        }
        g.setFill(Color.web("#8b98a6")); g.setFont(Font.font("JetBrains Mono", 9));
        g.setTextAlign(TextAlignment.LEFT);  g.fillText("SDO · HMI continuum", 6, H - 7);
        g.setTextAlign(TextAlignment.RIGHT); g.fillText("SN 142 · 6 groups", W - 6, H - 7);
        StackPane wrap = new StackPane(cv); wrap.setMaxWidth(Double.MAX_VALUE);
        return wrap;
    }

    public static HBox kv(String k, String v, boolean ok) {
        Label kl = lbl(k); kl.getStyleClass().add("k"); HBox.setHgrow(kl, Priority.ALWAYS); kl.setMaxWidth(Double.MAX_VALUE);
        Label vl = lbl(v); vl.getStyleClass().addAll("ars-mono"); vl.getStyleClass().add("v");
        if (ok) vl.setStyle("-fx-text-fill: -ars-ok;");
        HBox row = new HBox(kl, vl); row.getStyleClass().add("sx-kv"); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    static HBox bandRow(String band, String day, String night) {
        Label b = lbl(band, "sx-bc-bn"); HBox.setHgrow(b, Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE);
        Label d = lbl(day, "sx-bc-c", rate(day)); d.setMinWidth(60); d.setAlignment(Pos.CENTER);
        Label n = lbl(night, "sx-bc-c", rate(night)); n.setMinWidth(60); n.setAlignment(Pos.CENTER);
        HBox row = new HBox(7, b, d, n); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    // HamQSL ratings are "Good"/"Fair"/"Poor"; the CSS classes are lowercase.
    private static String rate(String r) { return r == null ? "" : r.trim().toLowerCase(); }
    static Label btn(String text, String... classes) { Label l = lbl(text, classes); l.setAlignment(Pos.CENTER); return l; }

    // ---- rotor compass dial (port of ARSCompass) --------------------------
    public static Canvas compass(double az, double size) {
        Canvas cv = new Canvas(size, size);
        paintCompass(cv, az);
        return cv;
    }

    /** Repaint an existing compass canvas at a new azimuth (for live rotor data). */
    public static void paintCompass(Canvas cv, double az) {
        double size = cv.getWidth();
        GraphicsContext g = cv.getGraphicsContext2D();
        g.clearRect(0, 0, size, size);
        double r = size/2 - 8, cx = size/2, cy = size/2;
        for (int i=0;i<36;i++){
            double a = Math.toRadians(i*10 - 90); boolean major = i%9==0;
            double r2 = r - (major?7:3.5);
            g.setStroke(Color.web("#4d5663")); g.setLineWidth(major?1.6:1);
            g.strokeLine(cx+Math.cos(a)*r, cy+Math.sin(a)*r, cx+Math.cos(a)*r2, cy+Math.sin(a)*r2);
        }
        g.setStroke(Color.web("#38434f")); g.setLineWidth(1.5); g.strokeOval(cx-r, cy-r, r*2, r*2);
        double rad = Math.toRadians(az-90), nx = cx+Math.cos(rad)*(r-6), ny = cy+Math.sin(rad)*(r-6);
        g.setStroke(Color.web("#6fc884")); g.setLineWidth(2.5); g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.strokeLine(cx, cy, nx, ny);
        g.setFill(Color.web("#6fc884")); g.fillOval(nx-3, ny-3, 6, 6);
        g.setFill(Color.web("#6a7684")); g.fillOval(cx-2.5, cy-2.5, 5, 5);
    }
}
