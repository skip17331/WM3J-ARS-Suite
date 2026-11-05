package com.ars.fx.shell;

import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.RotorClient;
import com.ars.fx.data.SolarData;
import com.ars.fx.data.SunImageService;
import com.ars.fx.data.WeatherData;
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
    /** Last J-Log sub-surface shown (log vs logc), so the dock returns the operator to contest mode
     *  if that's where they were when they hopped to another module. Updated by the host on show. */
    public static volatile String lastLogSurface = "log";
    /** Solo mode: a single module launched on its own (J-Map/J-Sat) — no module dock, no cross-nav. */
    public static volatile boolean solo = false;
    /** In solo mode, the operating module this window hosts — so the J-Hub settings pages can offer a
     *  "← Back to <module>" link (there is no dock to navigate back through). Set by the host on startup. */
    public static volatile String soloModule = "map";
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
        Map.entry("info",  Color.web("#d98fd1")),
        Map.entry("accent",Color.web("#31bdda")), Map.entry("t2",     Color.web("#97a4b2")));

    // ---- small helpers -----------------------------------------------------
    /** Style classes that mark a label as a button. Such labels keep their full width: in a crowded row it is the
     *  spacer or the long text beside them that gives way, never the button (which would show as "…"). */
    private static final java.util.Set<String> BUTTON_CLASSES = java.util.Set.of(
            "jl-clearbtn", "jl-logbtn", "jhub-clearbtn", "jhub-open", "cfg-select", "cfg-seg-btn", "jm-seg-btn",
            "sx-ro-turn-btn", "rc-vfobtn", "dg-send", "jm-collapse", "sx-theme", "js-pill");
    /** A small caption for a frequency field (MHz) saying what the band plan has there — "20m · CW segment" — and nothing
     *  when the frequency is empty or outside every amateur band. Follows the field as it is typed or tuned. */
    public static Label bandCaption(javafx.scene.control.TextField freqMhz) {
        Label l = lbl("", "jl-skim-meta");
        Runnable paint = () -> {
            String t = "";
            try { t = com.ars.fx.data.Bands.describe(Math.round(Double.parseDouble(freqMhz.getText().replace(",", "").trim()) * 1_000_000.0)); }
            catch (Exception ignored) { /* empty or mid-typing */ }
            l.setText(t); l.setVisible(!t.isEmpty()); l.setManaged(!t.isEmpty());
        };
        freqMhz.textProperty().addListener((o, a, b) -> paint.run());
        paint.run();
        com.ars.fx.shell.Tips.tip(l, "From the suite's band plan (your IARU region is set in J-Hub ▸ Station)");
        return l;
    }

    /** The "Print QSL label" switch shared by J-Log and J-Log-Quick: a checkbox bound to the saved setting plus a one-line result. */
    public static final class QslToggle {
        public final javafx.scene.layout.VBox node;
        private final Label status = lbl("", "jl-emeta");
        QslToggle() {
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox("Print QSL label on save (beta)");
            cb.setSelected(com.ars.fx.data.QslLabel.printOnSave());
            cb.selectedProperty().addListener((o, a, b) -> com.ars.fx.data.QslLabel.setPrintOnSave(b));
            com.ars.fx.data.HubConfig.addChangeListener(k -> { if ("label.printOnSave".equals(k) || "*".equals(k))
                javafx.application.Platform.runLater(() -> cb.setSelected(com.ars.fx.data.QslLabel.printOnSave())); });
            Tips.tip(cb, "Print a QSL label whenever a QSO is saved — choose the printer in Setup ▸ Label printers");
            status.setWrapText(true); status.setMaxWidth(230);
            // Says where the label will go, so the tick box is never a mystery.
            Label dest = lbl("", "jl-emeta"); dest.setMaxWidth(230);
            Runnable showDest = () -> {
                com.ars.fx.data.QslLabel.Model m = com.ars.fx.data.QslLabel.currentModel();
                boolean zpl = "zpl".equals(com.ars.fx.data.QslLabel.mode(m));
                String where = (zpl ? com.ars.fx.data.HubConfig.get("label.host", "").trim() : "");
                if (where.isEmpty()) where = com.ars.fx.data.HubConfig.get("label.printer", "").trim();
                dest.setText(where.isEmpty() ? "No printer set — Setup ▸ Label printers"
                        : m.label() + " · " + com.ars.fx.data.QslLabel.currentSize(m).label().replaceAll(" — .*", ""));
            };
            showDest.run();
            com.ars.fx.data.HubConfig.addChangeListener(k -> { if (k != null && (k.startsWith("label.") || "*".equals(k)))
                javafx.application.Platform.runLater(showDest); });
            node = new javafx.scene.layout.VBox(3, cb, dest, status);
            node.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        }
        /** Called after a new QSO is saved: prints if the switch is on. */
        public void afterSave(com.ars.fx.data.QslLabel.Qso q) {
            if (!com.ars.fx.data.QslLabel.printOnSave()) return;
            status.setText("⋯ printing label…");
            com.ars.fx.data.QslLabel.printAsync(q, r -> javafx.application.Platform.runLater(() -> {
                status.setText((r.ok() ? "✓ " : "✗ ") + r.message());
                javafx.animation.PauseTransition t = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(r.ok() ? 6 : 15));
                t.setOnFinished(e -> status.setText("")); t.play();
            }));
        }
    }
    public static QslToggle qslToggle() { return new QslToggle(); }

    /** A button that is not wired up yet: shown greyed out, does nothing when clicked, and says so in its tooltip. */
    public static Label placeholderButton(String text, String styleClass, String tooltip) {
        Label b = lbl(text, styleClass);
        b.setOpacity(0.4); b.setStyle("-fx-cursor:default;");
        Tips.tip(b, tooltip);
        return b;
    }
    /** Force a text field to UPPER CASE as the operator types, pastes or sets text programmatically (caret stays put). */
    public static <T extends javafx.scene.control.TextInputControl> T caps(T field) {
        field.setTextFormatter(new javafx.scene.control.TextFormatter<String>(ch -> {
            if (ch.isContentChange()) ch.setText(ch.getText().toUpperCase(java.util.Locale.ROOT));
            return ch;
        }));
        return field;
    }

    public static Label lbl(String text, String... classes) {
        Label l = new Label(text); l.getStyleClass().addAll(classes);
        for (String c : classes) if (BUTTON_CLASSES.contains(c)) { l.setMinWidth(Region.USE_PREF_SIZE); break; }
        return l;
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
        Region body = (rail == null) ? center : new HBox(center, collapsibleRail(rail));
        if (body instanceof HBox hb) hb.setFillHeight(true);
        VBox.setVgrow(body, Priority.ALWAYS);
        body.setMinHeight(0); center.setMinHeight(0);   // a tall panel must scroll/clip, not push the top bar and ticker off-screen
        VBox modwin = new VBox(top, body); modwin.getStyleClass().add("sx-modwin");
        if (bottom != null) modwin.getChildren().add(bottom);
        HBox.setHgrow(modwin, Priority.ALWAYS); VBox.setVgrow(modwin, Priority.ALWAYS);
        // solo mode (J-Map/J-Sat on their own) builds without the module dock
        HBox root = (dock == null) ? new HBox(modwin) : new HBox(dock, modwin);
        root.getStyleClass().add("sx-shell"); root.setFillHeight(true);
        return root;
    }

    // ---- module dock (collapsed icon rail, hover-expands) ------------------
    public static Region dock(String activeId) { return dock(activeId, null); }

    /** @param tailExpandOnly extra rows (e.g. the J-Hub config links) shown only when the dock is
     *  expanded, appended after the modules and before the Station-settings foot. */
    public static Region dock(String activeId, List<Node> tailExpandOnly) {
        if (solo) return null;   // solo module windows have no dock
        VBox dock = new VBox(); dock.getStyleClass().add("sx-dock");
        dock.setMinWidth(58); dock.setPrefWidth(58); dock.setMaxWidth(58);
        List<Node> expandOnly = new ArrayList<>();   // names + section labels + dots

        com.ars.fx.data.ModuleConfig.markOpened(activeId);
        // J-Hub at top
        dock.getChildren().add(dockItem("hub", "J-Hub", "hub", true, activeId, expandOnly));
        Label sec = lbl("MODULES", "sx-dock-sec"); sec.setManaged(false); sec.setVisible(false); expandOnly.add(sec);
        dock.getChildren().add(sec);
        for (com.ars.fx.data.ModuleConfig.Mod m : com.ars.fx.data.ModuleConfig.enabledModules()) {
            if (m.id().equals("log")) continue;   // logging lives on the J-Hub dashboard; contest mode is reached from its Log tools
            dock.getChildren().add(dockItem(m.id(), m.name(), m.hue(), com.ars.fx.data.ModuleConfig.running(m.id()), activeId, expandOnly));
        }
        if (tailExpandOnly != null) for (Node n : tailExpandOnly) {
            n.setManaged(false); n.setVisible(false); expandOnly.add(n); dock.getChildren().add(n);
        }
        dock.getChildren().add(spacer());
        // Awards: opens the full awards screen from any module (same screen as J-Log's drawer "Open awards")
        dock.getChildren().add(dockAction("Awards", "vault", "Awards - DXCC, WAS, WAC, grids, IOTA, POTA, SOTA, zones",
                expandOnly, com.ars.fx.surface.AwardsWindow::open));
        // Settings foot: on every module's dock, not just the J-Hub screens (gear → J-Hub Setup)
        dock.getChildren().add(dockItem("station", "Settings", "gear", true, activeId, expandOnly));

        // Scrolls inside its own height: an expanded dock can be taller than the window, and a taller child would make
        // the root centre the whole page, pushing its top off screen. The wheel reaches anything below the fold.
        ScrollPane sp = new ScrollPane(dock);
        sp.getStyleClass().add("sx-dock-scroll");
        sp.setFitToWidth(true); sp.setFitToHeight(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setMinHeight(0);
        sp.minWidthProperty().bind(dock.minWidthProperty());
        sp.prefWidthProperty().bind(dock.prefWidthProperty());
        sp.maxWidthProperty().bind(dock.maxWidthProperty());
        sp.setOnMouseEntered(e -> setExpanded(dock, expandOnly, true));
        sp.setOnMouseExited(e -> setExpanded(dock, expandOnly, false));
        return sp;
    }
    private static void setExpanded(VBox dock, List<Node> nodes, boolean exp) {
        double w = exp ? 216 : 58;
        dock.setMinWidth(w); dock.setPrefWidth(w); dock.setMaxWidth(w);
        for (Node n : nodes) { n.setManaged(exp); n.setVisible(exp); }
    }
    /** A dock row that runs an action (instead of navigating to a module). */
    private static HBox dockAction(String name, String hue, String tip, List<Node> expandOnly, Runnable action) {
        StackPane ic = iconTile(hue, hue, 20, "sx-dock-ic");
        Label nm = lbl(name, "sx-dock-name"); nm.setManaged(false); nm.setVisible(false); HBox.setHgrow(nm, Priority.ALWAYS); nm.setMaxWidth(Double.MAX_VALUE);
        expandOnly.add(nm);
        HBox row = new HBox(12, ic, nm); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().addAll("sx-dock-item", hue);
        VBox.setMargin(row, new Insets(2, 9, 2, 9));
        Tips.tip(row, tip);
        row.setStyle("-fx-cursor:hand;");
        row.setOnMouseClicked(e -> action.run());
        return row;
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
        // dock = navigation; J-Log returns to whichever mode (normal/contest) the operator left it in
        Tips.tip(row, hue.equals("gear") ? "Station settings" : name + (running ? "" : " (not running)"));
        if (!hue.equals("gear")) row.setOnMouseClicked(e -> navigate(id.equals("log") ? "logc" : id)   /* J-Log = the contest page; normal logging is on the J-Hub dashboard */);
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
        List<VBox> tiles = new ArrayList<>();
        for (String[] s : stats) {   // {key, value, optional class}
            boolean hasCls = s.length > 2 && s[2] != null && !s[2].isBlank();
            VBox cell = new VBox(2, lbl(s[0], "sx-stat-k"),
                    lbl(s[1], hasCls ? new String[]{"sx-stat-v", s[2]} : new String[]{"sx-stat-v"}));
            cell.getStyleClass().add("sx-stat"); cell.setAlignment(Pos.CENTER_LEFT);
            tiles.add(cell);
            top.getChildren().add(cell);
        }
        top.getChildren().add(hsp());
        // solo windows have no dock, so the gear that reaches J-Hub / Station settings lives in the top bar
        if (solo) {
            Label gear = lbl("⚙", "sx-theme"); gear.setStyle("-fx-cursor:hand;");
            javafx.scene.control.Tooltip.install(gear, new javafx.scene.control.Tooltip("J-Hub · Station settings"));
            Tips.tip(gear, "Open J-Hub (dashboard and settings)");
            gear.setOnMouseClicked(e -> navigate("hub"));
            HBox.setMargin(gear, new Insets(0, 14, 0, 0));
            top.getChildren().add(gear);
        }
        top.getChildren().add(TxIndicator.build());
        Label id = IdTimer.button(); HBox.setMargin(id, new Insets(0, 16, 0, 0));
        top.getChildren().add(id);
        if (clock != null) {
            VBox c = new VBox(1, lbl("UTC", "sx-clock-k"), lbl(clock, "sx-clock-v"));
            c.getStyleClass().add("sx-stat"); c.setAlignment(Pos.CENTER_RIGHT);
            tiles.add(c); top.getChildren().add(c);
        }
        scaleWithWidth(top, tiles);
        return top;
    }

    /** Last top-bar scale: surfaces rebuild (J-Sat every ~2 s), so a new bar starts here rather than flash full size. */
    private static double topScale = 1;
    private static final double TOP_MIN_SCALE = 0.5, TOP_TILE_GAP = 8, TOP_BRAND_MIN = 120;

    /** Shrink the top-bar tiles and their fonts so the row fits the bar's width, instead of cutting values to "…". */
    private static void scaleWithWidth(HBox top, List<VBox> tiles) {
        Runnable apply = () -> {
            double s = topScale;
            String pad = String.format("-fx-padding:%.0f %.0f %.0f %.0f;", 5 * s, 12 * s, 5 * s, 12 * s);
            for (VBox t : tiles) {
                t.setStyle(pad);
                t.setMinWidth(Region.USE_PREF_SIZE);    // the subtitle gives way when the row is tight, never a value
                t.setMaxHeight(Region.USE_PREF_SIZE);
                HBox.setMargin(t, new Insets(0, 0, 0, TOP_TILE_GAP * s));
                for (Node n : t.getChildren()) {
                    if (!(n instanceof Label l)) continue;
                    double full = l.getStyleClass().contains("sx-stat-v") ? 16 : l.getStyleClass().contains("sx-clock-v") ? 18 : 0;
                    l.setStyle(String.format("-fx-font-size:%.1fpx;", full > 0 ? full * s : Math.max(8, 9 * s)));
                }
            }
        };
        top.widthProperty().addListener((o, a, b) -> {
            double row = 0, others = top.getInsets().getLeft() + top.getInsets().getRight() + 60;   // 60: margins around the (larger) ID timer
            for (VBox t : tiles) row += t.prefWidth(-1) + TOP_TILE_GAP * topScale;
            for (Node n : top.getChildren())
                if (!tiles.contains(n)) others += n == top.getChildren().get(0) ? Math.min(n.prefWidth(-1), TOP_BRAND_MIN) : n.prefWidth(-1);
            double s = Math.max(TOP_MIN_SCALE, Math.min(1, (b.doubleValue() - others) / (row / topScale * 1.03)));
            if (b.doubleValue() <= 1 || Math.abs(s - topScale) < 0.01) return;
            topScale = s; apply.run();
        });
        apply.run();
    }

    // ---- collapsible drawer ------------------------------------------------
    // Remembered open/closed state by title, so a drawer the operator opened stays
    // open when its surface rebuilds (e.g. J-Sat recreates its rail every ~2 s).
    // Drawers default closed (see drawer()); the few seeded here start open instead,
    // until the operator collapses them.
    private static final Map<String, Boolean> DRAWER_STATE = new java.util.HashMap<>();

    /** Have the drawer with this title open the next time it is built. */
    public static void openDrawer(String title) { DRAWER_STATE.put(title, true); }

    public static VBox drawer(String title, String hue, String glyphId, String summary, boolean open, Node body) {
        return drawer(title, hue, glyphId, summary, open, body, null);
    }
    /** As above, with an optional {@code onToggle}. Open/closed state is remembered by title across rebuilds. */
    public static VBox drawer(String title, String hue, String glyphId, String summary, boolean open, Node body,
                              java.util.function.Consumer<Boolean> onToggle) {
        // default closed on first sight; the operator's open/closed choice is then remembered by title
        boolean initOpen = DRAWER_STATE.getOrDefault(title, false);
        VBox dw = new VBox(); dw.getStyleClass().add("sx-dw"); if (initOpen) dw.getStyleClass().add("open");
        StackPane ic = iconTile(hue, glyphId, 14, "sx-dw-ic");
        Label t = lbl(title, "sx-dw-t"); HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE);
        Label sum = lbl(summary == null ? "" : summary, "sx-dw-sum");
        Label cv = lbl("›", "sx-dw-cv");   // ›
        HBox head = new HBox(9, ic, t, sum, cv); head.setAlignment(Pos.CENTER_LEFT); head.getStyleClass().add("sx-dw-head");
        VBox bodyBox = new VBox(); bodyBox.getStyleClass().add("sx-dw-body"); bodyBox.getChildren().add(body);
        cv.setRotate(initOpen ? 90 : 0); sum.setVisible(!initOpen); sum.setManaged(!initOpen);
        bodyBox.setManaged(initOpen); bodyBox.setVisible(initOpen);
        Tips.tip(head, "Click to show or hide " + title);
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

    /** Run a content rebuild without losing the user's scroll position. Rebuilding a list or a whole frame on a
     *  data tick otherwise snaps every ScrollPane under {@code scope} (and the ones enclosing it) back to the top,
     *  so a page-down "resets" immediately. Offsets are kept in pixels, since the content height changes. */
    public static void keepScroll(Node scope, Runnable rebuild) {
        List<ScrollPane> before = scrollPanes(scope);
        double[] px = new double[before.size()];
        for (int i = 0; i < px.length; i++) px[i] = scrollPx(before.get(i));
        rebuild.run();
        if (scope.getScene() == null) return;
        if (scope instanceof javafx.scene.Parent p) { p.applyCss(); p.layout(); }
        List<ScrollPane> after = scrollPanes(scope);
        if (after.size() != px.length) return;
        for (int i = 0; i < px.length; i++) {
            ScrollPane sp = after.get(i);
            if (px[i] <= 0 || sp.getContent() == null) continue;
            sp.layout();
            double extra = sp.getContent().getLayoutBounds().getHeight() - sp.getViewportBounds().getHeight();
            if (extra > 0) sp.setVvalue(Math.min(1.0, px[i] / extra));
        }
    }
    /** Page Up/Down, Home and End scroll the ScrollPane under the mouse pointer. JavaFX only routes those keys to a
     *  focused ScrollPane, and clicking a plain panel never focuses one, so in practice they did nothing. */
    public static void installPageKeys(javafx.scene.Scene scene) {
        Node[] hover = new Node[1];
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> hover[0] = e.getPickResult().getIntersectedNode());
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> hover[0] = e.getPickResult().getIntersectedNode());
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            javafx.scene.input.KeyCode k = e.getCode();
            if (k != javafx.scene.input.KeyCode.PAGE_DOWN && k != javafx.scene.input.KeyCode.PAGE_UP
                    && k != javafx.scene.input.KeyCode.HOME && k != javafx.scene.input.KeyCode.END) return;
            Node focus = scene.getFocusOwner();
            if (focus instanceof javafx.scene.control.TextInputControl || focus instanceof javafx.scene.control.ComboBoxBase) return;
            for (Node n = hover[0]; n != null; n = n.getParent()) {
                if (!(n instanceof ScrollPane sp) || sp.getContent() == null) continue;
                double extra = sp.getContent().getLayoutBounds().getHeight() - sp.getViewportBounds().getHeight();
                if (extra <= 0) continue;                         // nothing to scroll here; try the enclosing pane
                double page = sp.getViewportBounds().getHeight() * 0.9 / extra;
                double v = switch (k) {
                    case PAGE_DOWN -> sp.getVvalue() + page;
                    case PAGE_UP   -> sp.getVvalue() - page;
                    case HOME      -> 0.0;
                    default        -> 1.0;
                };
                sp.setVvalue(Math.max(0.0, Math.min(1.0, v)));
                e.consume();
                return;
            }
        });
    }
    private static List<ScrollPane> scrollPanes(Node scope) {
        List<ScrollPane> out = new ArrayList<>();
        List<ScrollPane> up = new ArrayList<>();
        for (Node n = scope.getParent(); n != null; n = n.getParent()) if (n instanceof ScrollPane sp) up.add(0, sp);
        out.addAll(up);
        for (Node n : scope.lookupAll(".scroll-pane")) if (n instanceof ScrollPane sp && !out.contains(sp)) out.add(sp);
        return out;
    }
    private static double scrollPx(ScrollPane sp) {
        if (sp.getContent() == null) return 0;
        double extra = sp.getContent().getLayoutBounds().getHeight() - sp.getViewportBounds().getHeight();
        return extra > 0 ? sp.getVvalue() * extra : 0;
    }

    /** Wrap a right column so it folds to a thin strip and opens on hover. It stays in the scene while folded, so its live drawers keep updating. */
    public static Region collapsibleRail(Region rail) {
        StackPane outer = new StackPane();
        outer.setMaxWidth(Region.USE_PREF_SIZE);
        Runnable[] rebuild = new Runnable[1];
        HBox header = new HBox(8, lbl("PANELS", "sx-rail-title"), hsp(), AutoPanel.pinButton("rail", () -> rebuild[0].run()));
        header.setAlignment(Pos.CENTER_LEFT); header.getStyleClass().add("sx-rail-head");
        VBox open = new VBox(header, rail);
        open.setMinWidth(322); open.setPrefWidth(322); open.setMaxWidth(322);
        VBox.setVgrow(rail, Priority.ALWAYS);

        Label title = lbl("PANELS", "sx-rail-title"); title.setRotate(90);
        VBox strip = new VBox(16, lbl("‹", "jm-collapse"), new javafx.scene.Group(title));
        strip.setAlignment(Pos.TOP_CENTER); strip.getStyleClass().add("sx-rail-strip");
        Tips.tip(strip, "Hover to open the panels; Pin keeps them open");

        rebuild[0] = () -> outer.getChildren().setAll(AutoPanel.wrap("rail", true, strip, open, 322, () -> false));
        rebuild[0].run();
        return outer;
    }

    /** A collapsing column that sits right of the module dock: a thin strip that floats the panel open on hover (Pin keeps it open).
     *  Mirror of {@link #collapsibleRail}; {@code key} names its pin state. */
    public static Region collapsibleLeftPanel(String key, String titleText, double width, Node... drawers) {
        VBox box = new VBox(9, drawers); box.getStyleClass().add("sx-rail");
        ScrollPane sp = new ScrollPane(box); sp.setFitToWidth(true); sp.getStyleClass().add("sx-rail-scroll");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        StackPane outer = new StackPane();
        outer.setMaxWidth(Region.USE_PREF_SIZE);
        Runnable[] rebuild = new Runnable[1];
        HBox header = new HBox(8, lbl(titleText, "sx-rail-title"), hsp(), AutoPanel.pinButton(key, () -> rebuild[0].run()));
        header.setAlignment(Pos.CENTER_LEFT); header.getStyleClass().add("sx-rail-head");
        VBox open = new VBox(header, sp);
        open.setMinWidth(width); open.setPrefWidth(width); open.setMaxWidth(width);
        VBox.setVgrow(sp, Priority.ALWAYS);
        Label title = lbl(titleText, "sx-rail-title"); title.setRotate(90);
        VBox strip = new VBox(16, lbl("›", "jm-collapse"), new javafx.scene.Group(title));
        strip.setAlignment(Pos.TOP_CENTER); strip.getStyleClass().add("sx-rail-strip");
        Tips.tip(strip, "Hover to open; Pin keeps it open");
        rebuild[0] = () -> outer.getChildren().setAll(AutoPanel.wrap(key, false, strip, open, width, () -> false));
        rebuild[0].run();
        return outer;
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
        String grid = HubConfig.grid().isEmpty() ? "—" : HubConfig.grid().toUpperCase();
        HBox wxBig; VBox wxBody; String wxSum;
        if (w == null) {
            String why = HubConfig.hasLocation() ? "loading…" : "set your location";
            wxBig = new HBox(8, lbl("—°","sx-ro-head"), lbl(why,"sx-ro-dir")); wxBig.setAlignment(Pos.BOTTOM_LEFT);
            wxBody = new VBox(wxBig); wxSum = why;
        } else {
            wxBig = new HBox(8, lbl(com.ars.fx.data.Units.tempText(w.tempC()),"sx-ro-head"), lbl(w.desc()+" · feels "+com.ars.fx.data.Units.tempText(w.feelsC()),"sx-ro-dir")); wxBig.setAlignment(Pos.BOTTOM_LEFT);
            wxBody = new VBox(wxBig, kv("Wind", w.windCompass()+" "+com.ars.fx.data.Units.wind(w.windKmh())+" · g"+com.ars.fx.data.Units.wind(w.gustKmh())+" "+com.ars.fx.data.Units.windUnit(), false),
                    kv("Humidity", w.humidity()+"%", false), kv("Pressure", com.ars.fx.data.Units.pressureText(w.pressure()), false));
            wxSum = com.ars.fx.data.Units.tempText(w.tempC())+" "+w.windCompass()+com.ars.fx.data.Units.wind(w.windKmh());
        }

        return List.of(
            propagationDrawer(),
            solarSpaceWeatherDrawer(),
            drawer("Weather · " + grid,"bridge","bridge", wxSum, false, wxBody));
    }

    /** Station-ID timer drawer — the first standard drawer on every operating module. */

    /** Lead drawers shown first on every operating module, in order: Station ID, Rig control
     *  ({@link RigPanel}), Rotor control. */
    public static List<Node> leadDrawers() { return List.of(RigPanel.build(), rotorControlDrawer()); }

    /** Live rotor-control drawer: compass, CCW/Stop/CW, presets. Shared across modules. */
    public static Node rotorControlDrawer() {
        RotorClient rotor = RotorClient.getInstance();
        StackPane roHolder = new StackPane(); roHolder.setAlignment(Pos.TOP_LEFT);
        VBox dw = drawer("Rotor control", "sat", "sat", rotorSummary(), false, new VBox(roHolder));
        String[] shown = {null};
        Runnable render = () -> {
            double az = rotor.headingDeg();
            String key = Double.isNaN(az) ? rotor.statusText() : String.valueOf(Math.round(az));
            if (key.equals(shown[0])) return;                 // the rotor reports twice a second; rebuilding would eat clicks
            shown[0] = key;
            if (dw.lookup(".sx-dw-sum") instanceof Label sum) sum.setText(rotorSummary());
            roHolder.getChildren().setAll(Double.isNaN(az) ? rotorOffline(rotor) : rotorLive(rotor, az));
        };
        render.run();
        rotor.setListener(st -> javafx.application.Platform.runLater(render));
        rotor.start();
        return dw;
    }
    private static String rotorSummary() {
        double az = RotorClient.getInstance().headingDeg();
        return Double.isNaN(az) ? RotorClient.getInstance().statusText()
                : String.format("%03.0f° %s", az, RotorClient.cardinal(az));
    }
    /** No heading to show, so no number, needle or controls: only why, and where to set it up. */
    private static Region rotorOffline(RotorClient rotor) {
        HBox head = new HBox(2, lbl("—", "sx-ro-head"), lbl("°", "sx-ro-dir")); head.setAlignment(Pos.BOTTOM_LEFT);
        VBox info = new VBox(0, head, lbl(rotor.statusText(), "sx-ro-dir")); info.setAlignment(Pos.CENTER_LEFT);
        Label hint = lbl("Set up the rotor in J-Hub ▸ Hardware ▸ Rotor control.", "sx-ro-dir"); hint.setWrapText(true);
        HBox top = new HBox(13, compass(Double.NaN, 88), info); top.setAlignment(Pos.CENTER_LEFT);
        return new VBox(11, top, hint);
    }
    private static Region rotorLive(RotorClient rotor, double az) {
        VBox roInfo = new VBox(0); roInfo.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(roInfo, Priority.ALWAYS);
        HBox head = new HBox(2, lbl(String.format("%03.0f", az), "sx-ro-head"), lbl("°", "sx-ro-dir")); head.setAlignment(Pos.BOTTOM_LEFT);
        Label ccw = btn("◀ CCW","sx-ro-turn-btn"), stp = btn("Stop","sx-ro-turn-btn","stop"), cw = btn("CW ▶","sx-ro-turn-btn");
        ccw.setStyle("-fx-cursor:hand;"); stp.setStyle("-fx-cursor:hand;"); cw.setStyle("-fx-cursor:hand;");
        ccw.setOnMouseClicked(e -> rotor.nudge(-15));
        stp.setOnMouseClicked(e -> rotor.stop());
        cw.setOnMouseClicked(e -> rotor.nudge(15));
        HBox turn = new HBox(5, ccw, stp, cw); for (Node n : turn.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        roInfo.getChildren().addAll(head, lbl(RotorClient.cardinal(az) + " · live", "sx-ro-dir"), turn);
        VBox.setMargin(turn, new Insets(10,0,0,0));
        HBox roTop = new HBox(13, compass(az, 88), roInfo); roTop.setAlignment(Pos.CENTER_LEFT);
        GridPane presets = new GridPane(); presets.setHgap(5); presets.setVgap(5);
        java.util.List<String[]> shownPresets = new java.util.ArrayList<>();
        for (RotorClient.Preset pr : RotorClient.PRESETS) shownPresets.add(new String[]{pr.shortName(), String.valueOf(pr.az())});   // edited under J-Hub ▸ Rotor control
        shownPresets.add(new String[]{"Park", String.valueOf(RotorClient.parkAz())});
        int i = 0; for (String[] p : shownPresets) {
            final int paz = Integer.parseInt(p[1]);
            Label b = btn(p[0]+" "+paz+"°", "sx-ro-preset"); GridPane.setHgrow(b, Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle("-fx-cursor:hand;"); b.setOnMouseClicked(e -> rotor.moveTo(paz));
            presets.add(b, i%3, i/3); i++;
        }
        for (int col=0; col<3; col++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(100/3.0); presets.getColumnConstraints().add(cc); }
        return new VBox(11, roTop, presets);
    }
    /** "Propagation" — one drawer for everything about what the bands are doing: MUF / FOT and geomagnetic state, then a
     *  single table of HF bands with HamQSL's day / night rating and live DX-cluster activity (heat map). The raw solar
     *  indices (SFI, K, aurora, solar wind …) are in "Solar &amp; space weather", not repeated here. */
    public static Node propagationDrawer() {
        SolarData.getInstance().start();
        ClusterClient.getInstance().start();
        VBox body = new VBox(7);
        String[] sig = {null};
        Runnable fill = () -> {
            String now = propagationSignature();
            if (now.equals(sig[0])) return;               // nothing changed: leave the rows (and the operator's scroll) alone
            sig[0] = now;
            body.getChildren().setAll(propagationRows());
        };
        fill.run();
        // keep the heat map live while the drawer is on screen; the timer stops once the drawer has been removed
        boolean[] attached = {false};
        javafx.animation.Timeline tick = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(4), e -> {
            if (body.getScene() != null) attached[0] = true;
            else if (attached[0]) { return; }
            fill.run();
        }));
        tick.setCycleCount(javafx.animation.Animation.INDEFINITE);
        body.sceneProperty().addListener((o, was, now) -> { if (now == null) tick.stop(); else tick.play(); });
        SolarData.Snapshot sd = SolarData.getInstance().snapshot();
        return drawer("Propagation", "map", "map", sd == null ? "loading…" : bestBand(sd), false, body);
    }

    private static String propagationSignature() {
        SolarData.Snapshot sd = SolarData.getInstance().snapshot();
        ClusterClient cc = ClusterClient.getInstance();
        return System.identityHashCode(sd) + "|" + cc.status() + "|" + java.util.Arrays.deepToString(cc.activity(30 * 60_000L))
                + "|" + com.ars.fx.data.Units.system();
    }

    private static List<Node> propagationRows() {
        SolarData.Snapshot sd = SolarData.getInstance().snapshot();
        List<Node> out = new ArrayList<>();
        if (sd == null) { out.add(kv("Propagation", "loading HamQSL…", false)); }
        else {
            double muf = SolarData.mufMhz(sd), fot = SolarData.fotMhz(sd);
            out.add(kv("MUF (3000 km)", Double.isNaN(muf) ? "not reported" : String.format("%.1f MHz", muf), !Double.isNaN(muf)));
            if (!Double.isNaN(fot)) out.add(kv("FOT", String.format("%.1f MHz", fot), true));
            out.add(kv("Geomagnetic", SolarData.geomag(sd.kIndex()), "quiet".equals(SolarData.geomag(sd.kIndex()))));
        }

        // ── one table: band · day · night · DX activity ──
        Label hb = lbl("BAND", "sx-bc-hh"); hb.setMinWidth(34);
        Label hd = lbl("DAY", "sx-bc-hh"); hd.setMinWidth(44); hd.setAlignment(Pos.CENTER);
        Label hn = lbl("NIGHT", "sx-bc-hh"); hn.setMinWidth(44); hn.setAlignment(Pos.CENTER);
        Label hx = lbl("DX SPOTS · 30 MIN", "sx-bc-hh"); HBox.setHgrow(hx, Priority.ALWAYS); hx.setMaxWidth(Double.MAX_VALUE); hx.setAlignment(Pos.CENTER);
        Tips.tip(hx, "DX spots heard on the cluster in the last 30 minutes. Green = few, red = the busiest band. Hover a row for the CW / Phone / Digi split.");
        Tips.tip(hd, "HamQSL daytime rating for this band's group"); Tips.tip(hn, "HamQSL night-time rating for this band's group");
        HBox head = new HBox(5, hb, hd, hn, hx); head.setAlignment(Pos.CENTER_LEFT); VBox.setMargin(head, new Insets(5, 0, 0, 0));
        out.add(head);

        ClusterClient cc = ClusterClient.getInstance();
        int[][] act = cc.activity(30 * 60_000L);
        int max = 0; for (int[] r : act) { int t = r[0] + r[1] + r[2]; if (t > max) max = t; }
        boolean live = cc.isConnected();
        for (int i = 0; i < ClusterClient.HEAT_BANDS.length; i++) {
            String band = ClusterClient.HEAT_BANDS[i];
            SolarData.Band grp = sd == null ? null : groupFor(sd, band);
            int total = act[i][0] + act[i][1] + act[i][2];
            Label bn = lbl(band, "sx-bc-bn"); bn.setMinWidth(34);
            Label d = rateChip(grp == null ? null : grp.day()), n = rateChip(grp == null ? null : grp.night());
            Label heat = lbl(total > 0 ? String.valueOf(total) : (live ? "·" : "—"), total > 0 ? "sx-heat-c" : "sx-heat-c0");
            heat.setMaxWidth(Double.MAX_VALUE); heat.setAlignment(Pos.CENTER); HBox.setHgrow(heat, Priority.ALWAYS);
            heat.setStyle("-fx-background-color:" + heatColor(max <= 0 ? 0 : (double) total / max, total) + ";-fx-background-radius:4;-fx-padding:3 2 3 2;");
            HBox row = new HBox(5, bn, d, n, heat); row.setAlignment(Pos.CENTER_LEFT);
            Tips.tip(row, band + (grp == null ? "" : " — " + grp.group() + " group: day " + grp.day().toLowerCase() + ", night " + grp.night().toLowerCase())
                    + (total > 0 ? "\n" + total + " DX spots in the last 30 min — CW " + act[i][0] + " · Phone " + act[i][1] + " · Digi " + act[i][2]
                                 : live ? "\nNo DX spots in the last 30 min" : "\nConnect the DX cluster for live activity"));
            out.add(row);
        }

        out.add(lbl("HamQSL ratings · cluster spots", "sx-dw-stamp"));
        if (!live) {
            Label go = lbl(cc.status() == com.ars.fx.data.TelnetFeed.Status.CONNECTING ? "Connecting to the DX cluster…" : "Connect the DX cluster for live activity ›",
                    cc.status() == com.ars.fx.data.TelnetFeed.Status.CONNECTING ? "sx-bc-na" : "jl-clearbtn");
            if (cc.status() != com.ars.fx.data.TelnetFeed.Status.CONNECTING) { go.setStyle("-fx-cursor:hand;"); go.setOnMouseClicked(e -> FeedUi.toggle(cc)); }
            out.add(go);
        }
        return out;
    }

    private static Label rateChip(String rating) {
        String text = rating == null || rating.isBlank() ? "—" : rating;
        Label c = lbl(text, "sx-bc-c", rate(text)); c.setMinWidth(44); c.setAlignment(Pos.CENTER);
        return c;
    }

    /** The HamQSL group ("80m-40m", "30m-20m", …) that covers an individual band, or null (160 m and 6 m have none). */
    private static SolarData.Band groupFor(SolarData.Snapshot sd, String band) {
        int w; try { w = Integer.parseInt(band.replace("m", "")); } catch (Exception e) { return null; }
        for (SolarData.Band g : sd.bands()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*m?\\s*-\\s*(\\d+)").matcher(g.group());
            if (m.find()) { int x = Integer.parseInt(m.group(1)), y = Integer.parseInt(m.group(2)); if (w >= Math.min(x, y) && w <= Math.max(x, y)) return g; }
        }
        return null;
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
        return new VBox(9, sunspotDisk(), solarGrid(), sunPositionGrid(), lbl(stamp, "sx-dw-stamp"));
    }
    /** Where the sun is now, from the station's position. Keeps itself current while it is on screen. */
    private static GridPane sunPositionGrid() {
        String[] keys = {"Sub-solar", "Sun az / el", "Sunrise", "Sunset"};
        Label[] vals = new Label[keys.length];
        GridPane grid = new GridPane(); grid.setHgap(7); grid.setVgap(7);
        for (int i = 0; i < keys.length; i++) {
            vals[i] = lbl("—", "sx-swx-v");
            VBox c = new VBox(2, lbl(keys[i], "sx-swx-k"), vals[i]); c.getStyleClass().add("sx-swx-c");
            GridPane.setHgrow(c, Priority.ALWAYS); c.setMaxWidth(Double.MAX_VALUE);
            grid.add(c, i % 2, i / 2);
        }
        Runnable paint = () -> {
            if (!HubConfig.hasLocation()) { for (Label v : vals) v.setText("—"); vals[0].setText("set location"); return; }
            double lat = HubConfig.lat(), lon = HubConfig.lon();
            double[] ae = com.ars.fx.data.SolarPosition.azEl(lat, lon), rs = com.ars.fx.data.SolarPosition.riseSet(lat, lon);
            String none = rs[2] > 0 ? "up all day" : "down all day";
            vals[0].setText(com.ars.fx.data.SolarPosition.subSolarText());
            vals[1].setText(String.format("%03.0f° / %+.0f°", ae[0], ae[1]));
            vals[2].setText(Double.isNaN(rs[0]) ? none : com.ars.fx.data.SolarPosition.hhmm(rs[0]) + " Z");
            vals[3].setText(Double.isNaN(rs[1]) ? none : com.ars.fx.data.SolarPosition.hhmm(rs[1]) + " Z");
        };
        paint.run();
        javafx.animation.Timeline tick = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(30), e -> paint.run()));
        tick.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        grid.sceneProperty().addListener((o, old, sc) -> { if (sc == null) tick.stop(); else tick.play(); });
        return grid;
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
        if (Double.isNaN(az)) return;                         // no heading, no needle
        double rad = Math.toRadians(az-90), nx = cx+Math.cos(rad)*(r-6), ny = cy+Math.sin(rad)*(r-6);
        g.setStroke(Color.web("#6fc884")); g.setLineWidth(2.5); g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.strokeLine(cx, cy, nx, ny);
        g.setFill(Color.web("#6fc884")); g.fillOval(nx-3, ny-3, 6, 6);
        g.setFill(Color.web("#6a7684")); g.fillOval(cx-2.5, cy-2.5, 5, 5);
    }
}
