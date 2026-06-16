package com.ars.fx.surface;

import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.JLogDb;
import com.ars.fx.data.RigClient;
import com.ars.fx.data.RotorClient;
import com.ars.fx.mock.Mock;
import com.ars.fx.data.AmpClient;
import com.ars.fx.shell.Shell;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import static com.ars.fx.shell.Shell.lbl;

/** J-Hub unified workspace — cockpit dashboard (handoff shot 01), mock data. */
public final class JHubDashboard {
    private JHubDashboard() {}

    private static final long START = System.currentTimeMillis();
    private static Region dashRoot;
    private static boolean dashTickerStarted;

    public static Region build()        { dashRoot = page("dashboard", dashboardCenter()); startDashTicker(); return dashRoot; }
    public static Region buildConfig()  { return page("rig", configCenter()); }
    public static Region buildStation() { return page("station", stationCenter()); }
    public static Region buildRotor()   { return page("rotor", rotorCenter()); }
    public static Region buildAmp()     { return page("amp", amplifierCenter()); }
    public static Region buildAntSw()   { return page("antsw", antennaSwitchCenter()); }
    public static Region buildData()    { return page("data", dataCenter()); }

    private static Region page(String activeConf, Region centerContent) {
        // Two independent nav behaviours:
        //   • workspace-bar ☰ fully closes the drawer (toggle the holder),
        //   • nav-head ☰ collapses the drawer to an icon rail (rebuild nav).
        boolean[] collapsed = {false};
        StackPane navHolder = new StackPane();
        Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> navHolder.getChildren().setAll(
                hubNav(activeConf, collapsed[0], () -> { collapsed[0] = !collapsed[0]; rebuild[0].run(); }));
        rebuild[0].run();
        Runnable close = () -> { boolean vis = !navHolder.isVisible(); navHolder.setManaged(vis); navHolder.setVisible(vis); };

        ScrollPane sp = new ScrollPane(centerContent); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(navHolder, sp); row.setFillHeight(true); VBox.setVgrow(row, Priority.ALWAYS);
        VBox root = new VBox(workspaceBar(close), row); root.setStyle("-fx-background-color:-ars-bg;");
        return root;
    }

    private static Region dashboardCenter() {
        VBox center = new VBox(20, header(), threeCol());
        center.setPadding(new Insets(18, 24, 24, 24));
        center.setStyle("-fx-background-color:-ars-bg;");
        return center;
    }

    // ---- top workspace bar -------------------------------------------------
    private static String uptime() {
        long m = (System.currentTimeMillis() - START) / 60000;
        return m < 60 ? m + "m" : (m / 60) + "h " + (m % 60) + "m";
    }
    private static String nowUtc() { return java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")); }
    private static void startDashTicker() {
        if (dashTickerStarted) return;
        dashTickerStarted = true;
        Timeline t = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            if (dashRoot == null) return;
            if (dashRoot.lookup(".sx-clock-v") instanceof Label l) l.setText(nowUtc());
            if (dashRoot.lookup(".jhub-uptime") instanceof Label l) l.setText("ARS Suite · up " + uptime());
        }));
        t.setCycleCount(Timeline.INDEFINITE); t.play();
    }
    private static Region workspaceBar(Runnable onMenu) {
        Label ham = lbl("☰", "jhub-bar-ham"); ham.setStyle("-fx-cursor:hand;"); ham.setOnMouseClicked(e -> onMenu.run());
        Region dot = chip("jhub-bar-dot", 9);
        HBox brand = new HBox(8, ham, dot, lbl("J-Hub", "jhub-bar-nm"), lbl("· WM3J", "jhub-bar-sub"));
        brand.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label theme = lbl("◑ Theme", "jhub-theme"); theme.setStyle("-fx-cursor:hand;"); theme.setOnMouseClicked(e -> Shell.toggleTheme());
        HBox bar = new HBox(brand, sp, lbl("ARS Suite · up " + uptime(), "jhub-bar-sub", "jhub-uptime"), theme);
        bar.setSpacing(14); bar.getStyleClass().add("jhub-bar"); bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ---- left nav ----------------------------------------------------------
    private static Region hubNav(String activeConf, boolean collapsed, Runnable onCollapse) {
        VBox nav = new VBox(); nav.getStyleClass().add("jhub-nav");
        double w = collapsed ? 66 : 256;
        nav.setMinWidth(w); nav.setPrefWidth(w); nav.setMaxWidth(w);

        boolean onHardware = activeConf.equals("rig") || activeConf.equals("rotor")
                || activeConf.equals("amp") || activeConf.equals("antsw");
        if (collapsed) {
            Label ham = lbl("☰", "jhub-bar-ham"); ham.setStyle("-fx-cursor:hand;"); ham.setOnMouseClicked(e -> onCollapse.run());
            HBox head = new HBox(ham); head.setAlignment(Pos.CENTER); head.getStyleClass().add("jhub-nav-head");
            nav.getChildren().add(head);
            for (Mock.Mod m : Mock.MODULES) {
                Node ic = Shell.iconTile(m.hue(), m.id(), 18, "sx-dock-ic");
                HBox row = new HBox(ic); row.setAlignment(Pos.CENTER); row.getStyleClass().add("jhub-nav-item");
                // modules are separate surfaces; none is the active item while on a J-Hub page
                row.setStyle("-fx-cursor:hand;"); VBox.setMargin(row, new Insets(1, 9, 1, 9));
                row.setOnMouseClicked(e -> Shell.navigate(m.id()));
                nav.getChildren().add(row);
            }
            nav.getChildren().addAll(
                    collapsedConf("◎", activeConf.equals("dashboard"), () -> Shell.navigate("hub")),
                    collapsedConf("▣", activeConf.equals("station"), () -> Shell.navigate("station")),
                    collapsedConf("◉", onHardware, () -> Shell.navigate("hubcfg")),
                    collapsedConf("▤", activeConf.equals("data"), () -> Shell.navigate("hubdata")));
            return nav;
        }

        Label ham = lbl("☰", "jhub-bar-ham"); ham.setStyle("-fx-cursor:hand;"); ham.setOnMouseClicked(e -> onCollapse.run());
        HBox head = new HBox(10, ham, lbl("J-Hub", "jhub-nav-headnm")); head.setAlignment(Pos.CENTER_LEFT); head.getStyleClass().add("jhub-nav-head");
        nav.getChildren().addAll(head, lbl("OPERATE", "jhub-nav-sec"));
        for (Mock.Mod m : Mock.MODULES) nav.getChildren().add(navItem(m, false));
        nav.getChildren().add(lbl("J-HUB", "jhub-nav-sec"));
        Region dash = confItem("◎", "Dashboard", null, activeConf.equals("dashboard"));
        dash.setOnMouseClicked(e -> Shell.navigate("hub"));
        nav.getChildren().add(dash);
        Region station = confItem("▣", "Station", "›", activeConf.equals("station"));
        station.setOnMouseClicked(e -> Shell.navigate("station"));
        nav.getChildren().add(station);
        nav.getChildren().add(confItem("◉", "Hardware", "⌄", false));
        String[][] hw = {{"Rig control","hubcfg","rig"},{"Rotor control","hubrotor","rotor"},
                {"Amplifier","hubamp","amp"},{"Antenna switch","hubant","antsw"},{"Antenna workshop","",""}};
        for (String[] s : hw) {
            Label l = lbl(s[0], "jhub-nav-sub");
            if (!s[2].isEmpty() && activeConf.equals(s[2])) l.getStyleClass().add("active");
            if (!s[1].isEmpty()) { l.setStyle("-fx-cursor:hand;"); l.setOnMouseClicked(e -> Shell.navigate(s[1])); }
            VBox.setMargin(l, new Insets(0, 9, 0, 9));
            nav.getChildren().add(l);
        }
        Region data = confItem("▤", "Data", "›", activeConf.equals("data"));
        data.setOnMouseClicked(e -> Shell.navigate("hubdata"));
        nav.getChildren().add(data);
        return nav;
    }
    private static Region collapsedConf(String glyph, boolean active, Runnable onClick) {
        Label ic = lbl(glyph, "jhub-nav-cic");
        HBox row = new HBox(ic); row.setAlignment(Pos.CENTER); row.getStyleClass().add("jhub-nav-conf");
        if (active) row.getStyleClass().add("active");
        row.setStyle("-fx-cursor:hand;"); VBox.setMargin(row, new Insets(1, 9, 1, 9));
        row.setOnMouseClicked(e -> onClick.run());
        return row;
    }
    private static Region navItem(Mock.Mod m, boolean active) {
        Node ic = Shell.iconTile(m.hue(), m.id(), 18, "sx-dock-ic");
        Label nm = lbl(m.name(), "jhub-nav-nm");
        Label st = lbl(m.running() ? "running" : m.sub(), "jhub-nav-st"); if (m.running()) st.getStyleClass().add("run");
        VBox txt = new VBox(1, nm, st);
        HBox row = new HBox(11, ic, txt); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("jhub-nav-item");
        if (active && m.running()) row.getStyleClass().addAll("active","run");
        VBox.setMargin(row, new Insets(1, 9, 1, 9));
        row.setOnMouseClicked(e -> Shell.navigate(m.id()));
        return row;
    }
    private static Region confItem(String icon, String name, String caret, boolean active) {
        Label ic = lbl(icon, "jhub-nav-cic");
        Label nm = lbl(name, "jhub-nav-cnm"); HBox.setHgrow(nm, Priority.ALWAYS); nm.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(11, ic, nm); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("jhub-nav-conf");
        if (caret != null) row.getChildren().add(lbl(caret, "jhub-nav-caret"));
        if (active) row.getStyleClass().add("active");
        VBox.setMargin(row, new Insets(1, 9, 1, 9));
        return row;
    }

    // ---- center header -----------------------------------------------------
    private static Region header() {
        HBox crumb = new HBox(7, lbl("J-Hub", "jhub-crumb"), lbl("›", "jhub-crumb"), lbl("Dashboard", "jhub-crumb-b"));
        crumb.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        long running = Mock.MODULES.stream().filter(Mock.Mod::running).count();
        VBox run = new VBox(0, lbl(running + "/" + Mock.MODULES.size() + " modules", "jhub-modrun"), lbl("running", "jhub-modrun")); run.setAlignment(Pos.CENTER_RIGHT);
        HBox crumbRow = new HBox(crumb, sp, run); crumbRow.setAlignment(Pos.CENTER_LEFT);

        HBox profs = new HBox(12); profs.setAlignment(Pos.CENTER_LEFT);
        String activeProfile = com.ars.fx.data.ProfileConfig.active();
        for (com.ars.fx.data.ProfileConfig.Profile p : com.ars.fx.data.ProfileConfig.profiles()) {
            boolean on = p.name().equals(activeProfile);
            Region d = chip("jhub-mini-dot", 8); d.setStyle("-fx-background-color:-ars-" + p.color() + ";");
            HBox chip = new HBox(8, d, lbl(p.name(), "jhub-prof-nm")); chip.getStyleClass().add("jhub-prof"); chip.setAlignment(Pos.CENTER_LEFT);
            chip.setStyle("-fx-cursor:hand;" + (on ? "-fx-border-color:-ars-" + p.color() + ";-fx-border-width:1;-fx-border-radius:99;-fx-padding:3 9 3 7;" : ""));
            chip.setOnMouseClicked(e -> { com.ars.fx.data.ProfileConfig.activate(p.name()); Shell.navigate("hub"); });
            profs.getChildren().add(chip);
        }
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        ClusterClient cc = ClusterClient.getInstance();
        Region cdot = chip("jhub-mini-dot", 8); cdot.setStyle("-fx-background-color:-ars-" + (cc.isConnected() ? "ok" : "t4") + ";");
        HBox clu = new HBox(7, lbl("Cluster", "jhub-crumb"), cdot, lbl(cc.spots().size() + " spots", "jhub-bar-sub"),
                lbl("UTC", "jhub-crumb"), lbl(nowUtc(), "sx-clock-v")); clu.setAlignment(Pos.CENTER_LEFT);
        HBox profRow = new HBox(profs, sp2, clu); profRow.setAlignment(Pos.CENTER_LEFT);
        return new VBox(14, crumbRow, profRow);
    }

    // ---- 3-column area -----------------------------------------------------
    private static HBox threeColRef = new HBox();
    private static HBox threeCol() {
        HBox windows = new HBox(20, miniLog(), clusterCol());
        windows.setFillHeight(true);
        VBox left = new VBox(11, windows, savedQsoDrawer());
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox box = new HBox(20, left, railCol());
        box.setFillHeight(true); VBox.setVgrow(box, Priority.ALWAYS);
        threeColRef = box;
        return box;
    }
    /** Mini "Saved QSOs" drawer under the New-QSO / DX-cluster windows — from the real ~/.j-log DB. */
    private static Region savedQsoDrawer() {
        VBox rows = new VBox();
        rows.getChildren().add(logRow(new String[]{"UTC","CALL","FREQ","BAND","MODE","RST"}, true));
        java.util.List<JLogDb.Qso> recent = JLogDb.recent(8);
        if (recent.isEmpty()) {
            Label none = lbl("No saved QSOs yet — log one above"); none.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-t4;-fx-padding:12 16 12 16;");
            rows.getChildren().add(none);
        } else {
            for (JLogDb.Qso q : recent) rows.getChildren().add(logRow(new String[]{
                    JLogDb.hhmm(q.utc()), nv(q.callsign()), nv(q.freq()), nv(q.band()), nv(q.mode()), nv(q.rstRcvd()) }, false));
        }
        return Shell.drawer("Saved QSOs", "log", "log", recent.size() + " recent", true, rows);
    }
    // ---- DX cluster filter state (shared by panel + controls drawer) -------
    private static final java.util.Set<String> HF_BANDS = java.util.Set.of(
            "160m","80m","60m","40m","30m","20m","17m","15m","12m","10m");
    private static int cluBandSel = 0, cluModeSel = 0;     // 0 = All
    private static boolean cluNeededOnly = false;
    private static java.util.Set<String> workedCache = java.util.Set.of();
    private static long workedAt = 0;
    private static java.util.Set<String> workedSet() {
        long now = System.currentTimeMillis();
        if (now - workedAt > 5000) { workedCache = JLogDb.workedCallBands(); workedAt = now; }
        return workedCache;
    }
    private static boolean bandOk(ClusterClient.Spot s) {
        if (cluBandSel == 0) return true;
        boolean hf = HF_BANDS.contains(s.band());
        return cluBandSel == 1 ? hf : !hf;
    }
    private static boolean modeOk(ClusterClient.Spot s) {
        switch (cluModeSel) {
            case 1: return "CW".equals(s.mode());
            case 2: return "SSB".equals(s.mode());
            case 3: return java.util.Set.of("FT8","FT4","RTTY","PSK31","JS8").contains(s.mode());
            default: return true;
        }
    }
    private static boolean isNeeded(ClusterClient.Spot s, java.util.Set<String> worked) {
        return !worked.contains(s.call() + "|" + s.band());
    }

    /** Middle column: the DX spot list with a functional cluster-controls drawer beneath it. */
    private static Region clusterCol() {
        ClusterClient cc = ClusterClient.getInstance();

        // ---- DX spot panel ----
        Node ic = Shell.iconTile("hub", "hub", 16, "sx-dw-ic");
        Region hsp = new Region(); HBox.setHgrow(hsp, Priority.ALWAYS);
        Region cdot = chip("jhub-mini-dot", 8); cdot.setStyle("-fx-background-color:-ars-t4;");
        HBox src = new HBox(6, cdot, lbl(cc.endpoint(), "jhub-clu-de")); src.setAlignment(Pos.CENTER_LEFT);
        HBox head = new HBox(11, ic, lbl("DX Cluster", "jhub-card-title"), hsp, src); head.getStyleClass().add("jhub-card-head"); head.setAlignment(Pos.CENTER_LEFT);

        Label fltSummary = lbl("", "jhub-clu-flt");
        HBox flt = new HBox(8, fltSummary);
        Region fsp = new Region(); HBox.setHgrow(fsp, Priority.ALWAYS); flt.getChildren().addAll(fsp, lbl("+ Spot", "jhub-clu-flt"));
        flt.setAlignment(Pos.CENTER_LEFT); flt.setPadding(new Insets(12, 16, 12, 16));

        VBox rows = new VBox();
        Label empty = lbl("Connecting to cluster…"); empty.setStyle("-fx-font-size:12px;-fx-text-fill:-ars-t4;-fx-padding:18 16 18 16;");
        rows.getChildren().add(empty);
        ScrollPane rsp = new ScrollPane(rows); rsp.setFitToWidth(true); rsp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); rsp.setStyle("-fx-background-color:transparent;"); VBox.setVgrow(rsp, Priority.ALWAYS);
        VBox card = new VBox(head, flt, rsp); card.getStyleClass().add("jhub-card"); VBox.setVgrow(card, Priority.ALWAYS);

        // ---- controls drawer ----
        TextField server = new TextField(cc.endpoint()); server.getStyleClass().add("jhub-mini-field"); server.setMaxWidth(Double.MAX_VALUE);
        TextField login  = new TextField(cc.loginCall()); login.getStyleClass().add("jhub-mini-field"); login.setMinWidth(96); login.setMaxWidth(96);
        VBox serverF = labeled("CLUSTER SERVER", server); HBox.setHgrow(serverF, Priority.ALWAYS); serverF.setMaxWidth(Double.MAX_VALUE);
        HBox row1 = new HBox(12, serverF, labeled("LOGIN CALL", login)); row1.setAlignment(Pos.BOTTOM_LEFT);

        Label statusLbl = lbl("", "jhub-conn");
        Label act = lbl("", "jhub-clearbtn");

        // single render: applies filters to the live feed + refreshes status everywhere
        Runnable render = () -> {
            boolean conn = cc.isConnected();
            cdot.setStyle("-fx-background-color:" + (conn ? "-ars-ok" : "-ars-t4") + ";");
            java.util.Set<String> worked = workedSet();
            java.util.List<ClusterClient.Spot> all = cc.spots();
            java.util.List<ClusterClient.Spot> shown = new java.util.ArrayList<>();
            for (ClusterClient.Spot s : all)
                if (bandOk(s) && modeOk(s) && (!cluNeededOnly || isNeeded(s, worked))) shown.add(s);
            rows.getChildren().clear();
            if (shown.isEmpty()) {
                empty.setText(!cc.wantConnected() ? "Disconnected"
                        : conn ? (all.isEmpty() ? "Connected — waiting for spots…" : "No spots match the current filter")
                        : "Cluster offline — reconnecting…");
                rows.getChildren().add(empty);
            } else {
                for (ClusterClient.Spot s : shown) rows.getChildren().add(cluRow(new String[]{
                        ClusterClient.fmtFreq(s.freqHz()), s.call(), isNeeded(s, worked) ? "y" : "", s.mode(), s.band(),
                        s.de(), s.comment(), ClusterClient.age(s.arrivalMs()) }));
            }
            fltSummary.setText(filterSummary() + "  ·  " + shown.size() + (cluNeededOnly ? " needed" : " shown"));
            boolean want = cc.wantConnected();
            statusLbl.getStyleClass().remove("live");
            if (conn) { statusLbl.getStyleClass().add("live"); statusLbl.setText("● connected · " + cc.endpoint()); }
            else statusLbl.setText((want ? "○ connecting · " : "○ disconnected · ") + cc.endpoint());
            act.setText(want ? "Disconnect" : "Connect");
        };

        VBox bandsF = labeled("BANDS", segSel(new String[]{"All","HF","VHF+"}, cluBandSel, i -> { cluBandSel = i; render.run(); }));
        VBox modesF = labeled("MODES", segSel(new String[]{"All","CW","Phone","Digi"}, cluModeSel, i -> { cluModeSel = i; render.run(); }));
        HBox row2 = new HBox(12, bandsF, modesF); row2.setAlignment(Pos.BOTTOM_LEFT);

        Label nkl = lbl("Needed (ATNO/band-fill) only"); nkl.getStyleClass().add("k"); HBox.setHgrow(nkl, Priority.ALWAYS); nkl.setMaxWidth(Double.MAX_VALUE);
        HBox needed = new HBox(nkl, switchToggle(cluNeededOnly, v -> { cluNeededOnly = v; render.run(); }));
        needed.getStyleClass().add("sx-kv"); needed.setAlignment(Pos.CENTER_LEFT);

        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        act.setStyle("-fx-cursor:hand;");
        act.setOnMouseClicked(e -> { if (cc.wantConnected()) cc.disconnect(); else cc.connect(); render.run(); });
        Label apply = lbl("Apply server", "jhub-clearbtn"); apply.setStyle("-fx-cursor:hand;");
        apply.setOnMouseClicked(e -> {
            String txt = server.getText().trim(); String h = txt; int p = -1;
            int colon = txt.lastIndexOf(':');
            if (colon > 0) { try { p = Integer.parseInt(txt.substring(colon + 1).trim()); h = txt.substring(0, colon); } catch (NumberFormatException ignored) {} }
            cc.setEndpoint(h, p, login.getText());
        });
        HBox actRow = new HBox(10, statusLbl, gap, apply, act); actRow.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(12, row1, row2, needed, actRow); body.setPadding(new Insets(4, 2, 4, 2));
        Region controls = Shell.drawer("Cluster controls", "hub", "hub", cc.endpoint(), false, body);

        cc.setListener(c -> render.run());
        cc.start();
        render.run();

        VBox col = new VBox(11, card, controls);
        HBox.setHgrow(col, Priority.ALWAYS); col.setMinWidth(360);
        return col;
    }
    private static String filterSummary() {
        String[] b = {"All bands","HF","VHF+"}, m = {"All modes","CW","Phone","Digi"};
        return b[cluBandSel] + " · " + m[cluModeSel];
    }
    /** Interactive segmented control: highlights the chosen option and reports its index. */
    private static Region segSel(String[] opts, int on, java.util.function.IntConsumer onSelect) {
        HBox h = new HBox(2); h.getStyleClass().add("cfg-seg");
        Label[] btns = new Label[opts.length];
        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            Label b = lbl(opts[i], "cfg-seg-btn"); if (i == on) b.getStyleClass().add("on");
            b.setStyle("-fx-cursor:hand;");
            b.setOnMouseClicked(e -> { for (Label x : btns) x.getStyleClass().remove("on"); b.getStyleClass().add("on"); onSelect.accept(idx); });
            btns[i] = b; h.getChildren().add(b);
        }
        return h;
    }
    /** Interactive toggle switch that reports its new state. */
    private static Region switchToggle(boolean initial, java.util.function.Consumer<Boolean> onChange) {
        boolean[] on = {initial};
        Region knob = new Region(); knob.getStyleClass().add("jm-switch-knob");
        StackPane sw = new StackPane(knob); sw.getStyleClass().add("jm-switch"); sw.setStyle("-fx-cursor:hand;");
        Runnable paint = () -> {
            sw.getStyleClass().remove("on"); if (on[0]) sw.getStyleClass().add("on");
            sw.setStyle("-fx-cursor:hand;" + (on[0] ? "-fx-background-color:-ars-accent;-fx-border-color:-ars-accent;" : ""));
        };
        paint.run();
        sw.setOnMouseClicked(e -> { on[0] = !on[0]; paint.run(); onChange.accept(on[0]); });
        return sw;
    }

    private static Region miniLog() {
        // header — live count from the real ~/.j-log/j-log.db
        Node ic = Shell.iconTile("log", "log", 16, "sx-dw-ic");
        Label sub = lbl(JLogDb.count() + " in log · live", "jhub-card-sub");
        VBox ttl = new VBox(1, lbl("J-Log · new QSO", "jhub-card-title"), sub);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label open = lbl("Open full J-Log →", "jhub-open");
        open.setOnMouseClicked(e -> Shell.navigate("log"));
        HBox head = new HBox(11, ic, ttl, sp, open); head.getStyleClass().add("jhub-card-head"); head.setAlignment(Pos.CENTER_LEFT);

        // editable fields
        TextField tCall = tf("", "", -1, true);
        TextField tFreq = tf("MHz", "14.074", 120, false);
        TextField tMode = tf("", "USB", 90, false);
        TextField tUp   = tf("", "59", 70, false);
        TextField tDn   = tf("", "59", 70, false);
        TextField tName = tf("name", "", -1, false);
        TextField tQth  = tf("DXCC / QTH", "", -1, false);
        TextField tGrid = tf("FN31", "", 120, false);
        TextField tCmt  = tf("notes, QSL info…", "", -1, false);

        VBox body = new VBox(13);
        body.setPadding(new Insets(14, 16, 16, 16));
        body.getChildren().add(lbl("Enter a callsign for DXCC, beam heading & worked-before…", "jhub-mini-hint"));
        body.getChildren().add(frow(labeled("CALLSIGN", tCall), true, labeled("FREQ (MHZ)", tFreq), false, labeled("MODE", tMode), false));
        body.getChildren().add(frow(labeled("RST ↑", tUp), false, labeled("RST ↓", tDn), false, labeled("NAME", tName), true));
        body.getChildren().add(frow(labeled("QTH", tQth), true, labeled("GRID", tGrid), false));

        VBox cmt = labeled("COMMENT", tCmt); HBox.setHgrow(cmt, Priority.ALWAYS); cmt.setMaxWidth(Double.MAX_VALUE);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Label clear = lbl("Clear", "jhub-clearbtn");
        Label logBtn = lbl("Log QSO ⏎", "jl-logbtn");
        HBox cmtRow = new HBox(12, cmt, sp2, clear, logBtn);
        cmtRow.setAlignment(Pos.BOTTOM_LEFT);
        body.getChildren().add(cmtRow);

        // today's log — read back from the DB
        Label tlh = lbl("☰  Recent log"); tlh.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:-ars-t3;");
        Label tln = lbl(""); tln.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-t4;");
        HBox tlbar = new HBox(8, tlh, tln); tlbar.getStyleClass().add("jhub-card-head"); tlbar.setAlignment(Pos.CENTER_LEFT);
        VBox tbl = new VBox();
        fillLog(tbl, tln);

        // wiring
        Runnable doLog = () -> {
            String call = tCall.getText();
            if (call == null || call.isBlank()) { tCall.requestFocus(); return; }
            String freq = tFreq.getText();
            boolean ok = JLogDb.insert(call, freq, JLogDb.bandFor(freq), tMode.getText(),
                    tUp.getText(), tDn.getText(), tName.getText(), tQth.getText(), tGrid.getText(), tCmt.getText());
            if (ok) {
                tCall.clear(); tName.clear(); tQth.clear(); tGrid.clear(); tCmt.clear();
                tCall.requestFocus();
                fillLog(tbl, tln);
                sub.setText(JLogDb.count() + " in log · live");
            }
        };
        logBtn.setOnMouseClicked(e -> doLog.run());
        tCall.setOnAction(e -> doLog.run());
        tCmt.setOnAction(e -> doLog.run());
        clear.setOnMouseClicked(e -> { tCall.clear(); tName.clear(); tQth.clear(); tGrid.clear(); tCmt.clear(); tCall.requestFocus(); });

        VBox card = new VBox(head, body, tlbar, tbl); card.getStyleClass().add("jhub-card");
        HBox.setHgrow(card, Priority.ALWAYS); card.setMinWidth(420);
        return card;
    }

    /** A labelled-field row: alternating (VBox field, boolean grow) pairs. */
    private static HBox frow(Object... pairs) {
        HBox row = new HBox(12); row.setAlignment(Pos.BOTTOM_LEFT);
        for (int i = 0; i < pairs.length; i += 2) {
            VBox f = (VBox) pairs[i];
            boolean grow = (Boolean) pairs[i + 1];
            if (grow) { HBox.setHgrow(f, Priority.ALWAYS); f.setMaxWidth(Double.MAX_VALUE); }
            row.getChildren().add(f);
        }
        return row;
    }

    private static TextField tf(String prompt, String init, double w, boolean call) {
        TextField t = new TextField(init == null ? "" : init);
        if (prompt != null && !prompt.isEmpty()) t.setPromptText(prompt);
        t.getStyleClass().add("jhub-mini-field");
        if (call) t.getStyleClass().add("call");
        if (w > 0) { t.setMinWidth(w); t.setPrefWidth(w); t.setMaxWidth(w); }
        else { t.setMaxWidth(Double.MAX_VALUE); }
        return t;
    }

    private static void fillLog(VBox tbl, Label countLbl) {
        tbl.getChildren().clear();
        tbl.getChildren().add(logRow(new String[]{"UTC","CALL","FREQ","BAND","MODE","RST"}, true));
        java.util.List<JLogDb.Qso> rows = JLogDb.recent(7);
        if (rows.isEmpty()) {
            Label none = lbl("No QSOs yet — log one above"); none.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-t4;-fx-padding:14 16 14 16;");
            tbl.getChildren().add(none);
            countLbl.setText("");
        } else {
            for (JLogDb.Qso q : rows)
                tbl.getChildren().add(logRow(new String[]{ JLogDb.hhmm(q.utc()), nv(q.callsign()),
                        nv(q.freq()), nv(q.band()), nv(q.mode()), nv(q.rstRcvd()) }, false));
            countLbl.setText("· " + rows.size() + " shown");
        }
    }
    private static String nv(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private static final double[] LOGC = {52,-1,86,48,52,46};
    private static HBox logRow(String[] c, boolean header) {
        HBox r = new HBox(10); r.setAlignment(Pos.CENTER_LEFT); r.setPadding(new Insets(header?9:8, 16, header?9:8, 16));
        r.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;");
        for (int i=0;i<c.length;i++){
            Label l;
            if (header) { l = lbl(c[i]); l.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:-ars-t3;"); }
            else if (i==1) l = lbl(c[i], "jl-tr-call");
            else if (i==4) { Label m = lbl(c[i], "jl-mode"); wrapCol(r,m,i); continue; }
            else l = lbl(c[i], "jl-tr-mono");
            wrapCol(r,l,i);
        }
        return r;
    }
    private static void wrapCol(HBox r, Node n, int i){
        HBox cell = new HBox(n); cell.setAlignment(Pos.CENTER_LEFT);
        if (LOGC[i]<0){ HBox.setHgrow(cell, Priority.ALWAYS); cell.setMaxWidth(Double.MAX_VALUE);} else { cell.setMinWidth(LOGC[i]); cell.setPrefWidth(LOGC[i]); cell.setMaxWidth(LOGC[i]); }
        r.getChildren().add(cell);
    }

    private static Region cluRow(String[] s) {
        Label fq = lbl(s[0], "jhub-clu-fq"); fq.setMinWidth(64);
        HBox top = new HBox(9, fq, lbl(s[1], "jhub-clu-call"));
        if (s[2].equals("y")) top.getChildren().add(lbl("NEED", "jhub-need"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS); top.getChildren().addAll(sp, lbl(s[7], "jhub-clu-age"));
        top.setAlignment(Pos.CENTER_LEFT);
        String tail = "de " + s[5] + (s[6].isEmpty() ? "" : "  · " + s[6]);
        HBox bot = new HBox(9, lbl(s[3], "jhub-clu-mode"), lbl(s[4], "jhub-clu-de"), lbl(tail, "jhub-clu-de"));
        bot.setAlignment(Pos.CENTER_LEFT); HBox.setMargin(bot, new Insets(3,0,0,0));
        VBox v = new VBox(0, top, bot); v.getStyleClass().add("jhub-clu");
        return v;
    }

    // ---- drawer rail (rig + rotor + space wx) ------------------------------
    private static Region railCol() {
        VBox col = new VBox(11, com.ars.fx.shell.IdTimer.drawer(),
                Shell.drawer("Rig control", "bridge", "bridge", "", true, rigBody()),
                Shell.drawer("Rotor control", "bridge", "bridge", "", true, rotorBody()),
                Shell.drawer("Amplifier", "bridge", "bridge", "", false, ampBody()),
                Shell.solarSpaceWeatherDrawer(),
                Shell.bandConditionsDrawer());
        col.setMinWidth(322); col.setPrefWidth(322); col.setMaxWidth(322);
        return col;
    }
    private static Region rigBody() {
        Label freq = lbl("—.—.—", "jhub-rig-fq");
        HBox fq = new HBox(6, freq, lbl("MHz", "jhub-rig-u")); fq.setAlignment(Pos.BOTTOM_LEFT);
        Label modeChip = lbl("—", "jhub-pill");
        Label bandChip = lbl("—", "jhub-pill", "grey");
        Label vfoChip  = lbl("VFO A", "jhub-pill", "grey");
        HBox chips = new HBox(8, modeChip, bandChip, vfoChip);
        HBox steps = new HBox(0); String[] st = {"‹","10","100","1k","5k","›"};
        for (int i=0;i<st.length;i++){ Label b = lbl(st[i], "jhub-step"); if (st[i].equals("1k")) b.getStyleClass().add("on"); HBox.setHgrow(b,Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE); HBox.setMargin(b,new Insets(0,2,0,2)); steps.getChildren().add(b); }
        // s-meter
        Region[] segs = new Region[16];
        HBox seg = new HBox(2); seg.setAlignment(Pos.CENTER_LEFT);
        for (int i=0;i<16;i++){ Region s = new Region(); s.getStyleClass().addAll("jhub-sm-seg", "off"); HBox.setHgrow(s,Priority.ALWAYS); s.setMaxWidth(Double.MAX_VALUE); segs[i]=s; seg.getChildren().add(s); }
        HBox scale = new HBox(); for (String k : new String[]{"S1","","","55","","59","","+20","","+40"}){ Label l=lbl(k,"jhub-sm-k"); HBox.setHgrow(l,Priority.ALWAYS); l.setMaxWidth(Double.MAX_VALUE); scale.getChildren().add(l);}
        VBox sm = new VBox(3, seg, scale);
        Label pwV = lbl("—","jhub-pw-v");
        VBox pw = new VBox(2, lbl("POWER","jhub-pw-k"), pwV);
        Region psp = new Region(); HBox.setHgrow(psp, Priority.ALWAYS);
        Label swrV = lbl("—","jhub-pw-v");
        VBox swr = new VBox(2, lbl("SWR","jhub-pw-k"), swrV); swr.setAlignment(Pos.CENTER_RIGHT);
        HBox power = new HBox(pw, psp, swr);
        GridPane bands = new GridPane(); bands.setHgap(5); bands.setVgap(5);
        Label[] bandBtns = new Label[Mock.RIG_BANDS.length];
        for (int i=0;i<Mock.RIG_BANDS.length;i++){
            final String band = Mock.RIG_BANDS[i];
            Label b = lbl(band, "jhub-band"); GridPane.setHgrow(b,Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE);
            b.setOnMouseClicked(e -> RigClient.getInstance().setBand(band));
            bandBtns[i]=b; bands.add(b, i%5, i/5);
        }
        for (int c=0;c<5;c++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(20); bands.getColumnConstraints().add(cc); }

        Label conn = lbl("○ rigctld " + RigClient.getInstance().endpoint() + " · offline", "jhub-conn");

        // live updater
        RigClient.getInstance().setListener(s -> {
            boolean c = s.connected();
            conn.getStyleClass().remove("live");
            if (c) { conn.getStyleClass().add("live"); conn.setText("● rigctld " + RigClient.getInstance().endpoint()); }
            else conn.setText("○ rigctld " + RigClient.getInstance().endpoint() + " · offline");
            freq.setText(c && s.freqHz() > 0 ? RigClient.fmtFreq(s.freqHz()) : "—.—.—");
            modeChip.setText(c && !s.mode().isEmpty() ? s.mode() : "—");
            String band = c && s.freqHz() > 0 ? JLogDb.bandFor(String.valueOf(s.freqHz()/1_000_000.0)) : null;
            bandChip.setText(band != null ? band : "—");
            if (c && s.vfo() != null && !s.vfo().isEmpty()) vfoChip.setText(s.vfo());
            int lit = c ? RigClient.sMeterSegments(s.sMeterDb()) : 0;
            for (int i=0;i<segs.length;i++){ segs[i].getStyleClass().removeAll("on","off"); segs[i].getStyleClass().add(i<lit?"on":"off"); }
            pwV.setText(c && s.rfPowerFrac() >= 0 ? Math.round(s.rfPowerFrac()*100) + "%" : "—");
            swrV.setText(c && s.swr() >= 0 ? String.format("%.1f:1", s.swr()) : "—");
            for (Label b : bandBtns){ b.getStyleClass().remove("on"); if ((b.getText()+"m").equals(band)) b.getStyleClass().add("on"); }
        });
        RigClient.getInstance().start();
        return new VBox(12, fq, chips, steps, sm, power, bands, conn);
    }
    private static Region ampBody() {
        Label stateV = lbl("—", "jhub-pill", "grey");
        HBox stateRow = new HBox(8, lbl("STATE", "jhub-pw-k"), stateV); stateRow.setAlignment(Pos.CENTER_LEFT);
        Label swrV = lbl("—", "jhub-pw-v");
        VBox swr = new VBox(2, lbl("SWR", "jhub-pw-k"), swrV);
        Region psp = new Region(); HBox.setHgrow(psp, Priority.ALWAYS);
        Label pwrV = lbl("—", "jhub-pw-v");
        VBox pwr = new VBox(2, lbl("FWD POWER", "jhub-pw-k"), pwrV); pwr.setAlignment(Pos.CENTER_RIGHT);
        HBox meters = new HBox(swr, psp, pwr);
        Label conn = lbl("○ ampctld " + AmpClient.getInstance().endpoint() + " · offline", "jhub-conn");
        AmpClient.getInstance().setListener(s -> {
            boolean c = s.connected();
            conn.getStyleClass().remove("live");
            if (c) { conn.getStyleClass().add("live"); conn.setText("● ampctld " + AmpClient.getInstance().endpoint()); }
            else conn.setText("○ ampctld " + AmpClient.getInstance().endpoint() + " · offline");
            stateV.setText(c ? s.mode() : "—");
            swrV.setText(c && s.swr() > 0 ? String.format("%.1f:1", s.swr()) : "—");
            pwrV.setText(c && s.pwr() > 0 ? Math.round(s.pwr()) + " W" : "—");
        });
        AmpClient.getInstance().start();
        return new VBox(12, stateRow, meters, conn);
    }
    private static Region rotorBody() {
        javafx.scene.canvas.Canvas cv = Shell.compass(0, 88);
        VBox info = new VBox(0); HBox.setHgrow(info, Priority.ALWAYS);
        Label head = lbl("—","sx-ro-head");
        HBox h = new HBox(2, head, lbl("°","sx-ro-dir")); h.setAlignment(Pos.BOTTOM_LEFT);
        Label dir = lbl("offline","sx-ro-dir");
        HBox turn = new HBox(5,
                btn("◀ CCW","sx-ro-turn-btn"), btn("Stop","sx-ro-turn-btn","stop"), btn("CW ▶","sx-ro-turn-btn"));
        ((Label) turn.getChildren().get(0)).setOnMouseClicked(e -> RotorClient.getInstance().nudge(-15));
        ((Label) turn.getChildren().get(1)).setOnMouseClicked(e -> RotorClient.getInstance().stop());
        ((Label) turn.getChildren().get(2)).setOnMouseClicked(e -> RotorClient.getInstance().nudge(15));
        for (Node n : turn.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        info.getChildren().addAll(h, dir, turn); VBox.setMargin(turn, new Insets(10,0,0,0));
        HBox top = new HBox(13, cv, info); top.setAlignment(Pos.CENTER_LEFT);
        Object[][] pr = {{"Europe",50},{"Africa",95},{"S. Am",155},{"Carib",135},{"Asia",330},{"Oceania",250}};
        GridPane g = new GridPane(); g.setHgap(5); g.setVgap(5);
        for (int i=0;i<pr.length;i++){
            final double az = (Integer) pr[i][1];
            Label b = btn(pr[i][0]+" "+(int)az+"°", "sx-ro-preset"); GridPane.setHgrow(b,Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE);
            b.setOnMouseClicked(e -> RotorClient.getInstance().moveTo(az));
            g.add(b,i%3,i/3);
        }
        for (int c=0;c<3;c++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(100/3.0); g.getColumnConstraints().add(cc); }

        RotorClient.getInstance().setListener(s -> {
            if (s.connected()) {
                Shell.paintCompass(cv, s.az());
                head.setText(String.format("%03d", Math.round(((s.az()%360)+360)%360)));
                dir.setText(RotorClient.cardinal(s.az()) + " · short path");
            } else {
                Shell.paintCompass(cv, 0);
                head.setText("—"); dir.setText("offline");
            }
        });
        RotorClient.getInstance().start();
        return new VBox(11, top, g);
    }

    // ---- config page (shot 09) --------------------------------------------
    private static Region configCenter() {
        HBox crumb = new HBox(7, lbl("J-Hub","jhub-crumb"), lbl("›","jhub-crumb"), lbl("Hardware","jhub-crumb"),
                lbl("›","jhub-crumb"), lbl("Rig control","cfg-title")); crumb.setAlignment(Pos.BOTTOM_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label back = lbl("← Dashboard","cfg-link"); back.setStyle("-fx-cursor:hand;"); back.setOnMouseClicked(e -> Shell.navigate("hub"));
        HBox top = new HBox(crumb, sp, back); top.setAlignment(Pos.BOTTOM_LEFT);
        Label desc = lbl("CAT control for your transceiver — connection, PTT, and how J-Hub follows the radio.","cfg-desc");

        HBox cells = new HBox(0,
            statCell("STATUS","● CONNECTED","",true), statCell("FREQUENCY","14.074.00","MHz",false),
            statCell("MODE","USB","",false), statCell("POLL","12","ms",false));
        cells.getStyleClass().add("cfg-status"); cells.setAlignment(Pos.CENTER_LEFT);

        VBox conn = section("CONNECTION",
            cfgRow("Transceiver model", null, cInput("rig.model", "Icom IC-7610")),
            cfgRow("Hamlib model #", "rigctld -m (run: rigctl --list)", cInput("rig.hamlibModel", "")),
            cfgRow("Interface", null, cSeg("rig.interface", new String[]{"USB","Serial","Network"}, 0)),
            cfgRow("Port", null, cInput("rig.port", "/dev/ttyUSB0")),
            cfgRow("Baud rate", null, cSelect("rig.baud", "9600", BAUDS)),
            cfgRow("CI-V address", "Icom civ address (hex)", cInput("rig.civ", "0x98")),
            cfgRow("Poll interval", null, cUnit("rig.poll", "12", "ms")),
            cfgRow("rigctld host", "Hamlib rig daemon J-Hub connects to", cInputApply("rig.rigctldHost", "127.0.0.1", () -> com.ars.fx.data.RigClient.getInstance().reconnect())),
            cfgRow("rigctld port", null, cInputApply("rig.rigctldPort", "4532", () -> com.ars.fx.data.RigClient.getInstance().reconnect())));
        VBox ptt = section("PTT & KEYING",
            cfgRow("PTT method", null, cSeg("rig.ptt", new String[]{"CAT","RTS","DTR","VOX"}, 0)),
            cfgRow("CW keyer", null, cInput("rig.keyer", "Winkeyer USB")),
            cfgRow("TX delay", null, cUnit("rig.txDelay", "30", "ms")));
        VBox beh = section("BEHAVIOR",
            cfgRow("Auto band-follow", null, cToggle("rig.autoBandFollow", true)),
            cfgRow("Start rigctld for me", "spawn the daemon with the model/serial above", cToggleApply("rig.startDaemon", false, on -> com.ars.fx.data.DaemonManager.setEnabled("rig", on))));

        VBox v = new VBox(20, top, desc, cells, conn, ptt, beh);
        v.setPadding(new Insets(18, 28, 40, 28)); v.setMaxWidth(1080); v.setStyle("-fx-background-color:-ars-bg;");
        return v;
    }
    private static Region stationCenter() {
        HBox crumb = new HBox(7, lbl("J-Hub","jhub-crumb"), lbl("›","jhub-crumb"), lbl("Station","cfg-title"));
        crumb.setAlignment(Pos.BOTTOM_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label back = lbl("← Dashboard","cfg-link"); back.setStyle("-fx-cursor:hand;"); back.setOnMouseClicked(e -> Shell.navigate("hub"));
        HBox top = new HBox(crumb, sp, back); top.setAlignment(Pos.BOTTOM_LEFT);
        Label desc = lbl("Your station identity & location — shared with every module for grids, beam headings and award tracking.","cfg-desc");

        HBox cells = new HBox(0,
            statCell("CALLSIGN", com.ars.fx.data.HubConfig.call(), "", true), statCell("GRID", com.ars.fx.data.HubConfig.grid(), "", false),
            statCell("ITU / CQ", com.ars.fx.data.HubConfig.get("station.ituZone","8") + " / " + com.ars.fx.data.HubConfig.get("station.cqZone","5"), "", false),
            statCell("DXCC","291","",false));
        cells.getStyleClass().add("cfg-status"); cells.setAlignment(Pos.CENTER_LEFT);

        VBox id = section("IDENTITY",
            cfgRow("Callsign", null, cInputApply("station.call", "WM3J", () -> com.ars.fx.data.HeardByClient.getInstance().setCall(com.ars.fx.data.HubConfig.call()))),
            cfgRow("Operator name", null, cInput("station.name", "Mike")),
            cfgRow("Grid square", "6-char Maidenhead locator", cInput("station.grid", "FM19")));
        VBox loc = section("LOCATION",
            cfgRow("Latitude", "decimal degrees (N+)", cInput("station.lat", "39.745583")),
            cfgRow("Longitude", "decimal degrees (E+)", cInput("station.lon", "-76.959714")),
            cfgRow("Elevation", null, cUnit("station.elev", "0", "m AMSL")));
        VBox zones = section("ZONES & AWARDS",
            cfgRow("ITU zone", null, cInput("station.ituZone", "8")),
            cfgRow("CQ zone", null, cInput("station.cqZone", "5")),
            cfgRow("IARU region", null, cSeg("station.iaru", new String[]{"R1","R2","R3"}, 1)),
            cfgRow("Default TX power", null, cUnit("station.txPower", "100", "W")));

        VBox v = new VBox(20, top, desc, cells, id, loc, zones);
        v.setPadding(new Insets(18, 28, 40, 28)); v.setMaxWidth(1080); v.setStyle("-fx-background-color:-ars-bg;");
        return v;
    }
    // ---- shared config-page chrome ----------------------------------------
    private static HBox cfgTop(String... crumbs) {
        HBox crumb = new HBox(7); crumb.setAlignment(Pos.BOTTOM_LEFT);
        for (int i = 0; i < crumbs.length; i++) {
            crumb.getChildren().add(lbl(crumbs[i], i == crumbs.length - 1 ? "cfg-title" : "jhub-crumb"));
            if (i < crumbs.length - 1) crumb.getChildren().add(lbl("›", "jhub-crumb"));
        }
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label back = lbl("← Dashboard", "cfg-link"); back.setStyle("-fx-cursor:hand;"); back.setOnMouseClicked(e -> Shell.navigate("hub"));
        HBox top = new HBox(crumb, sp, back); top.setAlignment(Pos.BOTTOM_LEFT);
        return top;
    }
    private static Region cfgPage(Node... rows) {
        VBox v = new VBox(20, rows); v.setPadding(new Insets(18, 28, 40, 28)); v.setMaxWidth(1080); v.setStyle("-fx-background-color:-ars-bg;");
        return v;
    }

    private static Region rotorCenter() {
        HBox cells = new HBox(0, statCell("STATUS","● CONNECTED","",true), statCell("AZIMUTH","045","°",false),
                statCell("ELEVATION","12","°",false), statCell("ROTCTLD","127.0.0.1:4533","",false));
        cells.getStyleClass().add("cfg-status"); cells.setAlignment(Pos.CENTER_LEFT);
        VBox conn = section("CONNECTION",
            cfgRow("Rotor model", null, cInput("rotor.model", "Yaesu G-1000DXA")),
            cfgRow("Hamlib model #", "rotctld -m (run: rotctl --list)", cInput("rotor.hamlibModel", "")),
            cfgRow("Interface", null, cSeg("rotor.interface", new String[]{"USB","Serial","Network"}, 1)),
            cfgRow("Serial port", null, cInput("rotor.port", "/dev/ttyUSB0")),
            cfgRow("Baud rate", null, cSelect("rotor.baud", "9600", BAUDS)),
            cfgRow("rotctld host", "Hamlib rotor daemon", cInputApply("rotor.host", "127.0.0.1", () -> com.ars.fx.data.RotorClient.getInstance().reconnect())),
            cfgRow("rotctld port", null, cInputApply("rotor.rotctldPort", "4533", () -> com.ars.fx.data.RotorClient.getInstance().reconnect())),
            cfgRow("Start rotctld for me", null, cToggleApply("rotor.startDaemon", false, on -> com.ars.fx.data.DaemonManager.setEnabled("rotor", on))));
        VBox beh = section("BEHAVIOR",
            cfgRow("Overlap / wrap", "where the rotor crosses", cSeg("rotor.wrap", new String[]{"North","South"}, 0)),
            cfgRow("Park position", null, cUnit("rotor.park", "0", "° az")),
            cfgRow("Dead-band", null, cUnit("rotor.deadband", "2", "°")),
            cfgRow("Auto-track satellites (J-Sat)", null, cToggle("rotor.autoTrack", false)));
        VBox presets = section("BEAM PRESETS", presetRows());
        return cfgPage(cfgTop("J-Hub","Hardware","Rotor control"),
            lbl("Az/el rotor control via Hamlib rotctld — connection, presets and park position.","cfg-desc"),
            cells, conn, beh, presets);
    }
    private static Region amplifierCenter() {
        HBox cells = new HBox(0, statCell("STATUS","● STANDBY","",true), statCell("BAND","20m","",false),
                statCell("SWR","1.2:1","",false), statCell("AMPCTLD","127.0.0.1:4534","",false));
        cells.getStyleClass().add("cfg-status"); cells.setAlignment(Pos.CENTER_LEFT);
        VBox conn = section("CONNECTION",
            cfgRow("Amplifier model", null, cInput("amp.model", "Elecraft KPA-1500")),
            cfgRow("Hamlib model #", "ampctld -m (run: ampctl --list)", cInput("amp.hamlibModel", "")),
            cfgRow("Interface", null, cSeg("amp.interface", new String[]{"USB","Serial","Network"}, 0)),
            cfgRow("Serial port", null, cInput("amp.port", "/dev/ttyACM0")),
            cfgRow("Baud rate", null, cSelect("amp.baud", "38400", BAUDS)),
            cfgRow("ampctld host", "Hamlib amp daemon", cInputApply("amp.host", "127.0.0.1", () -> AmpClient.getInstance().reconnect())),
            cfgRow("ampctld port", null, cInputApply("amp.ampctldPort", "4531", () -> AmpClient.getInstance().reconnect())),
            cfgRow("Start ampctld for me", null, cToggleApply("amp.startDaemon", false, on -> com.ars.fx.data.DaemonManager.setEnabled("amp", on))));
        VBox beh = section("BEHAVIOR",
            cfgRow("Follow rig band", "auto band-switch with the radio", cToggle("amp.followBand", true)),
            cfgRow("Max power", null, cUnit("amp.maxPower", "1000", "W")),
            cfgRow("Standby on TX inhibit", null, cToggle("amp.standbyOnInhibit", true)),
            cfgRow("Operate / Standby", null, cSeg("amp.opStandby", new String[]{"Operate","Standby"}, 1)));
        return cfgPage(cfgTop("J-Hub","Hardware","Amplifier"),
            lbl("Linear amplifier control via Hamlib ampctld — band-follow, power limit and standby.","cfg-desc"),
            cells, conn, beh);
    }
    private static Region antennaSwitchCenter() {
        HBox cells = new HBox(0, statCell("STATUS","● ONLINE","",true), statCell("ACTIVE","Port 3","",false),
                statCell("ANTENNA","20m beam","",false), statCell("PORTS","6","",false));
        cells.getStyleClass().add("cfg-status"); cells.setAlignment(Pos.CENTER_LEFT);
        VBox conn = section("CONNECTION",
            cfgRow("Switch model", null, cInput("ant.model", "4O3A Antenna Genius")),
            cfgRow("Interface", null, cSeg("ant.interface", new String[]{"USB","Serial","Network"}, 2)),
            cfgRow("Host / IP", null, cInput("ant.host", "192.168.1.50")),
            cfgRow("Port", null, cInput("ant.port", "9007")),
            cfgRow("Follow rig band", "select antenna by band", cToggle("ant.followBand", true)));
        String[][] ports = {{"1","160m dipole","160m"},{"2","80m vertical","80m"},{"3","20m beam","20–10m"},
                {"4","40m delta loop","40m"},{"5","6m yagi","6m"},{"6","RX loop","RX only"}};
        Node[] rows = new Node[ports.length];
        for (int i = 0; i < ports.length; i++) rows[i] = cfgRow("Port " + ports[i][0], "bands " + ports[i][2], cInput("ant.port" + ports[i][0], ports[i][1]));
        VBox ant = section("ANTENNA PORTS", rows);
        return cfgPage(cfgTop("J-Hub","Hardware","Antenna switch"),
            lbl("Remote antenna switch — port-to-antenna mapping and band-follow selection.","cfg-desc"),
            cells, conn, ant);
    }
    private static Region dataCenter() {
        HBox cells = new HBox(0, statCell("CLUSTER","● dxc.ve7cc.net","",true), statCell("RBN","● connected","",true),
                statCell("WSJT-X","UDP 2237","",false), statCell("LOG DB","~/.j-log","",false));
        cells.getStyleClass().add("cfg-status"); cells.setAlignment(Pos.CENTER_LEFT);
        Runnable applyCluster = () -> {
            String srv = com.ars.fx.data.HubConfig.get("data.clusterServer", "dxc.ve7cc.net:23").trim();
            String login = com.ars.fx.data.HubConfig.get("data.clusterLogin", com.ars.fx.data.HubConfig.call());
            String host = srv; int port = 23; int c = srv.lastIndexOf(':');
            if (c > 0) { host = srv.substring(0, c); try { port = Integer.parseInt(srv.substring(c + 1).trim()); } catch (Exception ignored) {} }
            ClusterClient.getInstance().setEndpoint(host, port, login);
        };
        VBox cluster = section("DX CLUSTER",
            cfgRow("Cluster server", null, cInputApply("data.clusterServer", "dxc.ve7cc.net:23", applyCluster)),
            cfgRow("Login callsign", null, cInputApply("data.clusterLogin", "WM3J", applyCluster)),
            cfgRow("Band filter", null, cSeg("data.clusterBand", new String[]{"All","HF","VHF+"}, 0)),
            cfgRow("Mode filter", null, cSeg("data.clusterMode", new String[]{"All","CW","Phone","Digi"}, 0)),
            cfgRow("Reverse Beacon (RBN)", "spots of your own call", cToggle("data.rbn", true)));
        VBox wsjt = section("WSJT-X / DIGITAL",
            cfgRow("WSJT-X UDP port", null, cInput("data.wsjtxPort", "2237")),
            cfgRow("Auto-log FT8/FT4 QSOs", null, cToggle("data.autoLogFt8", true)),
            cfgRow("PSK Reporter upload", null, cToggle("data.pskReporter", false)));
        VBox logbook = section("LOGBOOK & EXPORT",
            cfgRow("Log database", null, cInput("data.logDb", "~/.j-log/j-log.db")),
            cfgRow("Cabrillo / CSV charset", null, cSeg("data.charset", new String[]{"UTF-8","ASCII"}, 0)),
            cfgRow("LoTW / eQSL auto-upload", null, cToggle("data.lotwAutoUpload", false)));
        return cfgPage(cfgTop("J-Hub","Data"),
            lbl("Spotting, digital-mode and logbook data sources — cluster, RBN, WSJT-X, ADIF/Cabrillo export.","cfg-desc"),
            cells, cluster, wsjt, lookupSection(), logbook, backupSection(), uploadSection(), macroSection());
    }
    /** Online log upload — LoTW (TQSL-signed), eQSL, QRZ Logbook, Club Log. */
    private static VBox uploadSection() {
        Label status = lbl("", "cfg-lh"); status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        java.util.function.Consumer<com.ars.fx.data.LogUpload.Result> report = r ->
                status.setText((r.ok() ? "✓ " : "✗ ") + r.message());

        VBox card = new VBox(); card.getStyleClass().add("cfg-card");
        // TQSL location (global, for LoTW)
        String detected = com.ars.fx.data.LogUpload.resolveTqsl();
        TextField tqsl = macroField(com.ars.fx.data.UploadConfig.tqslPath(), 290);
        tqsl.setPromptText(detected != null ? "auto-detected: " + detected : "path to tqsl (TrustedQSL not found on PATH)");
        tqsl.textProperty().addListener((o, a, b) -> com.ars.fx.data.UploadConfig.setTqslPath(tqsl.getText()));
        card.getChildren().add(cfgRow("TrustedQSL (tqsl) path", detected != null ? "detected — override only if needed" : "required for LoTW — install from arrl.org/lotw", tqsl));

        for (com.ars.fx.data.UploadConfig.Service s : com.ars.fx.data.UploadConfig.SERVICES) {
            CheckBox en = new CheckBox(); en.setSelected(com.ars.fx.data.UploadConfig.enabled(s.id()));
            en.selectedProperty().addListener((o, a, b) -> com.ars.fx.data.UploadConfig.setEnabled(s.id(), en.isSelected()));
            HBox fields = new HBox(8); fields.setAlignment(Pos.CENTER_RIGHT);
            for (com.ars.fx.data.UploadConfig.Field f : s.fields()) {
                TextField tf = new TextField(com.ars.fx.data.UploadConfig.field(s.id(), f.key()));
                tf.getStyleClass().add("jhub-mini-field"); tf.setPromptText(f.label()); tf.setMinWidth(120); tf.setMaxWidth(150);
                tf.textProperty().addListener((o, a, b) -> com.ars.fx.data.UploadConfig.setField(s.id(), f.key(), tf.getText()));
                fields.getChildren().add(tf);
            }
            Label up = cfgBtn("Upload"); up.setOnMouseClicked(e -> {
                status.setText("⋯ uploading to " + s.label() + "…");
                final String id = s.id();
                Thread t = new Thread(() -> {
                    com.ars.fx.data.LogUpload.Result r = switch (id) {
                        case "lotw" -> com.ars.fx.data.LogUpload.lotw();
                        case "eqsl" -> com.ars.fx.data.LogUpload.eqsl();
                        case "qrz_logbook" -> com.ars.fx.data.LogUpload.qrzLogbook();
                        case "clublog" -> com.ars.fx.data.LogUpload.clublog();
                        default -> new com.ars.fx.data.LogUpload.Result(false, 0, "unknown service");
                    };
                    javafx.application.Platform.runLater(() -> report.accept(r));
                }, "log-upload");
                t.setDaemon(true); t.start();
            });
            fields.getChildren().addAll(up, en);
            card.getChildren().add(cfgRow(s.label(), s.hint(), fields));
        }
        if (!card.getChildren().isEmpty()) card.getChildren().get(card.getChildren().size() - 1).setStyle("-fx-border-width:0;");
        HBox statusRow = new HBox(status); statusRow.getStyleClass().add("cfg-row"); statusRow.setStyle("-fx-border-width:0;");
        return new VBox(6, lbl("ONLINE LOG UPLOAD", "cfg-sec"),
                lbl("Push your log to the online confirmation services. LoTW is signed with your TrustedQSL certificate.", "cfg-lh"),
                card, statusRow);
    }
    /** Callsign-lookup providers (QRZ / HamCall / Callook / HamQTH / QRZCQ), tried top-to-bottom. */
    private static VBox lookupSection() {
        VBox card = new VBox(); card.getStyleClass().add("cfg-card");
        for (com.ars.fx.data.LookupConfig.Prov p : com.ars.fx.data.LookupConfig.PROVIDERS) {
            com.ars.fx.data.LookupConfig.Setting cur = com.ars.fx.data.LookupConfig.get(p.id());
            CheckBox en = new CheckBox(); en.setSelected(cur.enabled());
            TextField user = null, secret = null, path = null;
            HBox controls = new HBox(8); controls.setAlignment(Pos.CENTER_RIGHT);
            if (p.creds()) {
                user = macroField(cur.user(), 130); user.setPromptText("user / call");
                secret = new TextField(cur.secret()); secret.getStyleClass().add("jhub-mini-field"); secret.setPromptText("password / key"); secret.setMinWidth(150); secret.setMaxWidth(150);
                controls.getChildren().addAll(user, secret);
            } else if (p.path()) {
                path = macroField(cur.extra(), 290); path.setPromptText("~/callbook.csv  (call,name,city,state,county,country,grid)");
                controls.getChildren().add(path);
            }
            controls.getChildren().add(en);
            final TextField fu = user, fs = secret, fp = path;
            Runnable persist = () -> com.ars.fx.data.LookupConfig.set(p.id(), en.isSelected(),
                    fu == null ? "" : fu.getText(), fs == null ? "" : fs.getText(), fp == null ? "" : fp.getText());
            en.selectedProperty().addListener((o, a, b) -> persist.run());
            if (fu != null) fu.textProperty().addListener((o, a, b) -> persist.run());
            if (fs != null) fs.textProperty().addListener((o, a, b) -> persist.run());
            if (fp != null) fp.textProperty().addListener((o, a, b) -> persist.run());
            card.getChildren().add(cfgRow(p.label(), p.hint(), controls));
        }
        if (!card.getChildren().isEmpty()) card.getChildren().get(card.getChildren().size() - 1).setStyle("-fx-border-width:0;");
        return new VBox(6, lbl("CALLSIGN LOOKUP", "cfg-sec"),
                lbl("Tried top-to-bottom; first hit wins. J-Log autofills name / QTH / grid / state / county from the CALLSIGN field.", "cfg-lh"),
                card);
    }
    /** Real ADIF / CSV backup export of the J-Log database. */
    private static VBox backupSection() {
        TextField dir = new TextField(com.ars.fx.data.LogExport.defaultBackupDir().toString());
        dir.getStyleClass().add("jhub-mini-field"); dir.setMinWidth(280); dir.setMaxWidth(Double.MAX_VALUE);
        Label status = lbl("", "cfg-lh"); status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        java.util.function.Consumer<com.ars.fx.data.LogExport.Result> report = r ->
                status.setText(r.ok() ? "✓ Exported " + r.count() + " QSO" + (r.count() == 1 ? "" : "s") + " → " + r.path()
                                      : "✗ Export failed: " + r.error());
        Label adi = cfgBtn("Export ADIF"); adi.setOnMouseClicked(e -> report.accept(com.ars.fx.data.LogExport.backup(dir.getText(), "adi")));
        Label csv = cfgBtn("Export CSV");  csv.setOnMouseClicked(e -> report.accept(com.ars.fx.data.LogExport.backup(dir.getText(), "csv")));
        HBox btns = new HBox(10, adi, csv); btns.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(); card.getStyleClass().add("cfg-card");
        card.getChildren().add(cfgRow("Backup folder", "timestamped j-log-YYYYMMDD-HHMMSS files", dir));
        HBox exportRow = new HBox(20, new VBox(2, lbl("Export now", "cfg-lt"), lbl("ADIF for LoTW / eQSL / QRZ / Club Log · CSV for spreadsheets", "cfg-lh")));
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS); exportRow.getChildren().addAll(esp, btns);
        exportRow.getStyleClass().add("cfg-row"); exportRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(exportRow);
        HBox statusRow = new HBox(status); statusRow.getStyleClass().add("cfg-row"); statusRow.setStyle("-fx-border-width:0;");
        card.getChildren().add(statusRow);
        return new VBox(6, lbl("BACKUP & EXPORT", "cfg-sec"), card);
    }
    private static Label cfgBtn(String text) {
        Label b = lbl(text, "cfg-select"); b.setStyle("-fx-cursor:hand; -fx-padding:6 14 6 14; -fx-background-color:-ars-accent; -fx-text-fill:#06121a; -fx-font-weight:bold; -fx-background-radius:7;");
        return b;
    }
    /** F1–F8 station macros used by J-Log: CW/RTTY/digital send the text, phone plays the audio file. */
    private static VBox macroSection() {
        VBox card = new VBox(); card.getStyleClass().add("cfg-card");
        // column header
        Label hk = lbl("KEY", "cfg-lh"); hk.setMinWidth(34);
        Label hl = lbl("LABEL", "cfg-lh"); hl.setMinWidth(110);
        Label ht = lbl("CW / RTTY TEXT  ·  {CALL} = worked station", "cfg-lh"); HBox.setHgrow(ht, Priority.ALWAYS); ht.setMaxWidth(Double.MAX_VALUE);
        Label ha = lbl("SSB VOICE FILE", "cfg-lh"); ha.setMinWidth(210);
        HBox header = new HBox(10, hk, hl, ht, ha, spacer(82)); header.getStyleClass().add("cfg-row"); header.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(header);

        for (int i = 0; i < com.ars.fx.data.MacroConfig.count(); i++) {
            final int idx = i;
            com.ars.fx.data.MacroConfig.Macro m = com.ars.fx.data.MacroConfig.get(i);
            Label fk = lbl("F" + (i + 1), "cfg-lt"); fk.setMinWidth(34);
            TextField labelF = macroField(m.label(), 110);
            TextField textF  = macroField(m.text(), -1);
            TextField audioF = macroField(m.audio(), 210); audioF.setPromptText("none — text only");
            Label browse = lbl("Browse…", "cfg-select"); browse.setMinWidth(82); browse.setStyle("-fx-cursor:hand;");

            Runnable persist = () -> com.ars.fx.data.MacroConfig.set(idx, labelF.getText(), textF.getText(), audioF.getText());
            labelF.textProperty().addListener((o, a, b) -> persist.run());
            textF.textProperty().addListener((o, a, b) -> persist.run());
            audioF.textProperty().addListener((o, a, b) -> persist.run());
            browse.setOnMouseClicked(e -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Voice-keyer audio for F" + (idx + 1));
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio (WAV / AIFF / AU)", "*.wav", "*.aif", "*.aiff", "*.au"));
                java.io.File cur = com.ars.fx.data.MacroConfig.resolve(audioF.getText());
                if (cur != null && cur.getParentFile() != null && cur.getParentFile().isDirectory()) fc.setInitialDirectory(cur.getParentFile());
                java.io.File f = fc.showOpenDialog(browse.getScene() != null ? browse.getScene().getWindow() : null);
                if (f != null) audioF.setText(f.getAbsolutePath());
            });
            HBox row = new HBox(10, fk, labelF, textF, audioF, browse); row.getStyleClass().add("cfg-row"); row.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().add(row);
        }
        if (!card.getChildren().isEmpty()) card.getChildren().get(card.getChildren().size() - 1).setStyle("-fx-border-width:0;");
        return new VBox(6, lbl("STATION MACROS · F1–F8", "cfg-sec"),
                lbl("Shared with J-Log. CW/RTTY/digital modes send the text message; SSB and other phone modes play the voice file.", "cfg-lh"),
                card);
    }
    private static TextField macroField(String val, double w) {
        TextField t = new TextField(val == null ? "" : val);
        t.getStyleClass().add("jhub-mini-field");
        if (w > 0) { t.setMinWidth(w); t.setMaxWidth(w); } else { HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE); }
        return t;
    }
    private static Region spacer(double w) { Region r = new Region(); r.setMinWidth(w); r.setMaxWidth(w); return r; }
    private static Node[] presetRows() {
        String[][] pr = {{"Europe","50"},{"Africa","95"},{"S. America","155"},{"Caribbean","135"},{"Asia (LP)","330"},{"Oceania","250"}};
        Node[] rows = new Node[pr.length];
        for (int i = 0; i < pr.length; i++) rows[i] = cfgRow(pr[i][0], null, cUnit("rotor.preset." + pr[i][0].replaceAll("[^A-Za-z]", ""), pr[i][1], "° az"));
        return rows;
    }

    private static VBox statCell(String k, String val, String unit, boolean ok) {
        Label v = lbl(val, "cfg-sv"); if (ok) v.getStyleClass().add("ok");
        HBox vu = new HBox(4, v); vu.setAlignment(Pos.BOTTOM_LEFT); if (!unit.isEmpty()) vu.getChildren().add(lbl(unit, "cfg-sval-u"));
        VBox cell = new VBox(4, lbl(k, "cfg-sk"), vu); cell.setMinWidth(150); return cell;
    }
    private static VBox section(String name, Node... rows) {
        VBox card = new VBox(rows); card.getStyleClass().add("cfg-card");
        if (rows.length > 0) rows[rows.length-1].setStyle("-fx-border-width:0;");
        return new VBox(6, lbl(name, "cfg-sec"), card);
    }
    private static HBox cfgRow(String title, String hint, Node control) {
        VBox lblBox = new VBox(2, lbl(title, "cfg-lt")); if (hint != null) lblBox.getChildren().add(lbl(hint, "cfg-lh"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(20, lblBox, sp, control); row.getStyleClass().add("cfg-row"); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    // ---- config-bound interactive controls (persist to HubConfig) ----------
    private static final String[] BAUDS = {"1200","2400","4800","9600","19200","38400","57600","115200"};
    /** Free-text field bound to a HubConfig key. */
    private static TextField cInput(String key, String def) {
        TextField t = new TextField(com.ars.fx.data.HubConfig.get(key, def)); t.getStyleClass().add("jhub-mini-field");
        t.setMinWidth(220); t.setMaxWidth(260); t.setAlignment(Pos.CENTER_RIGHT);
        t.textProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set(key, b));
        return t;
    }
    /** Free-text field bound to a HubConfig key that also runs {@code apply} on commit (focus-lost / Enter). */
    private static TextField cInputApply(String key, String def, Runnable apply) {
        TextField t = cInput(key, def);
        t.focusedProperty().addListener((o, was, now) -> { if (!now) apply.run(); });
        t.setOnAction(e -> apply.run());
        return t;
    }
    /** Numeric/short field + unit, bound to a HubConfig key. */
    private static Region cUnit(String key, String def, String unit) {
        TextField t = new TextField(com.ars.fx.data.HubConfig.get(key, def)); t.getStyleClass().add("jhub-mini-field");
        t.setMinWidth(120); t.setMaxWidth(120); t.setAlignment(Pos.CENTER_RIGHT);
        t.textProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set(key, b));
        HBox h = new HBox(8, t, lbl(unit, "cfg-sval-u")); h.setAlignment(Pos.CENTER_LEFT); return h;
    }
    /** Dropdown bound to a HubConfig key (current value injected if not in the list). */
    private static Region cSelect(String key, String def, String... opts) {
        ComboBox<String> c = new ComboBox<>(); c.getItems().addAll(opts);
        String cur = com.ars.fx.data.HubConfig.get(key, def);
        if (!c.getItems().contains(cur)) c.getItems().add(0, cur);
        c.setValue(cur); c.setMinWidth(220); c.setMaxWidth(260);
        c.valueProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set(key, b));
        return c;
    }
    /** Segmented control bound to a HubConfig key (stores the option string). */
    private static Region cSeg(String key, String[] opts, int def) {
        String cur = com.ars.fx.data.HubConfig.get(key, opts[def]);
        int on = 0; for (int i = 0; i < opts.length; i++) if (opts[i].equals(cur)) on = i;
        return segSel(opts, on, i -> com.ars.fx.data.HubConfig.set(key, opts[i]));
    }
    /** Toggle bound to a HubConfig boolean key. */
    private static Region cToggle(String key, boolean def) {
        return switchToggle(com.ars.fx.data.HubConfig.getBool(key, def), v -> com.ars.fx.data.HubConfig.setBool(key, v));
    }
    /** Toggle bound to a HubConfig boolean key that also runs {@code apply} with the new state. */
    private static Region cToggleApply(String key, boolean def, java.util.function.Consumer<Boolean> apply) {
        return switchToggle(com.ars.fx.data.HubConfig.getBool(key, def), v -> { com.ars.fx.data.HubConfig.setBool(key, v); apply.accept(v); });
    }

    // ---- small helpers -----------------------------------------------------
    private static Region chip(String cls, double sz){ Region r = new Region(); r.getStyleClass().add(cls); r.setMinSize(sz,sz); r.setPrefSize(sz,sz); r.setMaxSize(sz,sz); return r; }
    private static Label btn(String t, String... c){ Label l = lbl(t, c); l.setAlignment(Pos.CENTER); return l; }
    private static VBox labeled(String label, Region inp){ VBox v = new VBox(5, lbl(label,"jl-flabel"), inp); v.setAlignment(Pos.TOP_LEFT); return v; }
}
