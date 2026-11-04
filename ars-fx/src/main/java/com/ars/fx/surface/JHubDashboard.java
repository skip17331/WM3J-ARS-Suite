package com.ars.fx.surface;

import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.JLogDb;
import com.ars.fx.data.RigClient;
import com.ars.fx.data.RotorClient;
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

    /** The content of one J-Hub setup section, for {@link JHubSetup} (one screen, section menu on the left). */
    static Region setupPage(String id) {
        return switch (id) {
            case "station"  -> stationCenter();
            case "rig"      -> configCenter();
            case "rotor"    -> rotorCenter();
            case "amp"      -> amplifierCenter();
            case "antsw"    -> antennaSwitchCenter();
            case "modules"  -> modulesCenter();
            case "sat"      -> JHubSatellites.center();
            default         -> dataPage(id);     // tx, cluster, digital, lookup, logdb, upload, export, share, learn
        };
    }

    private static Region page(String activeConf, Region centerContent) {
        // Same icon dock as every operating module; the J-Hub config links ride along as an
        // expand-only tail so they stay reachable without changing the collapsed rail. In a solo
        // window there is no dock, so the same links become a fixed left nav column instead.
        Region nav = Shell.solo ? soloNav(activeConf) : Shell.dock("hub", hubConfigSection(activeConf));

        ScrollPane sp = new ScrollPane(centerContent); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(nav, sp); row.setFillHeight(true); VBox.setVgrow(row, Priority.ALWAYS);
        row.setMinHeight(0);
        VBox root = new VBox(workspaceBar(), row); root.setStyle("-fx-background-color:-ars-bg;"); root.setMinHeight(0);
        return root;
    }

    /** Solo settings nav: the dock's config links as a static left column, topped by a link back to the
     *  operating module this window hosts (there is no dock to navigate back through). */
    private static Region soloNav(String activeConf) {
        VBox col = new VBox(4); col.getStyleClass().add("sx-dock");
        col.setMinWidth(208); col.setPrefWidth(208); col.setMaxWidth(208);
        col.setPadding(new Insets(10, 8, 10, 8));
        String back = Shell.soloModule;
        Label backLink = lbl("← Back to " + com.ars.fx.data.SoloConfig.name(back), "jhub-nav-sub");
        backLink.setStyle("-fx-cursor:hand;");
        backLink.setOnMouseClicked(e -> Shell.navigate(back));
        VBox.setMargin(backLink, new Insets(2, 9, 10, 9));
        col.getChildren().add(backLink);
        col.getChildren().addAll(hubConfigSection(activeConf));
        return col;
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
    private static String brandCallText() { String c = com.ars.fx.data.HubConfig.call(); return c.isEmpty() ? "" : "· " + c.toUpperCase(); }
    static Region workspaceBar() {
        Region dot = chip("jhub-bar-dot", 9);
        Label brandCall = lbl(brandCallText(), "jhub-bar-sub");
        com.ars.fx.data.HubConfig.addChangeListener(k -> { if ("station.call".equals(k) || "*".equals(k)) javafx.application.Platform.runLater(() -> brandCall.setText(brandCallText())); });
        HBox brand = new HBox(8, dot, lbl("J-Hub", "jhub-bar-nm"), brandCall);
        brand.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label idTimer = com.ars.fx.shell.IdTimer.button();
        HBox bar = new HBox(brand, sp, lbl("ARS Suite · up " + uptime(), "jhub-bar-sub", "jhub-uptime"), idTimer);
        bar.setSpacing(14); bar.getStyleClass().add("jhub-bar"); bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ---- J-Hub config links (ride along in the dock as an expand-only tail) -----
    /** The dock's J-Hub tail: just the one Setup screen (its sections are in Setup's own left menu); the J-Hub entry at the top of the dock is the dashboard. */
    static java.util.List<Node> hubConfigSection(String activeConf) {
        java.util.List<Node> out = new java.util.ArrayList<>();
        out.add(lbl("J-HUB", "jhub-nav-sec"));
        Region setup = confItem("⚙", "Setup", "›", !activeConf.equals("dashboard"));
        setup.setOnMouseClicked(e -> Shell.navigate("station")); out.add(setup);
        return out;
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
        int enabledMods = com.ars.fx.data.ModuleConfig.enabledCount();
        VBox run = new VBox(0, lbl(enabledMods + "/" + com.ars.fx.data.ModuleConfig.all().size() + " modules", "jhub-modrun"), lbl("enabled", "jhub-modrun")); run.setAlignment(Pos.CENTER_RIGHT);
        HBox crumbRow = new HBox(crumb, sp, run); crumbRow.setAlignment(Pos.CENTER_LEFT);

        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        ClusterClient cc = ClusterClient.getInstance();
        Region cdot = chip("jhub-mini-dot", 8); cdot.setStyle(com.ars.fx.shell.FeedUi.dot(cc.status()));
        Label cluTxt = lbl(cc.status().label() + " · " + cc.spots().size() + " spots", "jhub-bar-sub");
        headerDot = cdot; headerTxt = cluTxt;      // kept live by the cluster card's render
        HBox clu = new HBox(7, lbl("Cluster", "jhub-crumb"), cdot, cluTxt,
                lbl("UTC", "jhub-crumb"), lbl(nowUtc(), "sx-clock-v")); clu.setAlignment(Pos.CENTER_LEFT);
        HBox profRow = new HBox(sp2, clu); profRow.setAlignment(Pos.CENTER_LEFT);
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
    private static final String[] CLU_BANDS = {"All","HF","VHF+"}, CLU_MODES = {"All","CW","Phone","Digi"};
    /** The cluster window's band / mode filter is the "Band filter" / "Mode filter" setting on J-Hub ▸ Data, so what you
     *  pick here is what it starts with next time (and the Data page shows). 0 = All. */
    private static int cluSel(String key, String[] opts) {
        String cur = com.ars.fx.data.HubConfig.get(key, opts[0]);
        for (int i = 0; i < opts.length; i++) if (opts[i].equals(cur)) return i;
        return 0;
    }
    private static boolean cluNeededOnly = false;
    private static java.util.Set<String> workedCache = java.util.Set.of();
    private static long workedAt = 0;
    private static java.util.Set<String> workedSet() {
        long now = System.currentTimeMillis();
        if (now - workedAt > 5000) { workedCache = JLogDb.workedCallBands(); workedAt = now; }
        return workedCache;
    }
    private static boolean bandOk(ClusterClient.Spot s) {
        int cluBandSel = cluSel("data.clusterBand", CLU_BANDS);
        if (cluBandSel == 0) return true;
        boolean hf = HF_BANDS.contains(s.band());
        return cluBandSel == 1 ? hf : !hf;
    }
    private static boolean modeOk(ClusterClient.Spot s) {
        switch (cluSel("data.clusterMode", CLU_MODES)) {
            case 1: return "CW".equals(s.mode());
            case 2: return "SSB".equals(s.mode());
            case 3: return java.util.Set.of("FT8","FT4","RTTY","PSK31","JS8").contains(s.mode());
            default: return true;
        }
    }
    private static boolean isNeeded(ClusterClient.Spot s, java.util.Set<String> worked) {
        return !worked.contains(s.call() + "|" + s.band());
    }

    private static Region headerDot; private static Label headerTxt; private static Label headActRef;

    /** Middle column: the DX spot list with a functional cluster-controls drawer beneath it. */
    /** The DX Cluster window (spot list + cluster controls). Also used on J-Log, so the two always look and behave alike. */
    static Region clusterCol() {
        ClusterClient cc = ClusterClient.getInstance();

        // ---- DX spot panel ----
        Node ic = Shell.iconTile("hub", "hub", 16, "sx-dw-ic");
        Region hsp = new Region(); HBox.setHgrow(hsp, Priority.ALWAYS);
        Region cdot = chip("jhub-mini-dot", 8); cdot.setStyle(com.ars.fx.shell.FeedUi.dot(cc.status()));
        Label cluState = lbl(com.ars.fx.shell.FeedUi.line(cc), "jhub-clu-de");
        HBox src = new HBox(6, cdot, cluState); src.setAlignment(Pos.CENTER_LEFT);
        Label consoleBtn = lbl("Console", "jhub-clearbtn"); consoleBtn.setStyle("-fx-cursor:hand;");   // raw feed + command line
        consoleBtn.setOnMouseClicked(e -> ClusterConsole.show(consoleBtn));
        Label headAct = lbl(com.ars.fx.shell.FeedUi.action(cc), "jhub-clearbtn"); headAct.setStyle("-fx-cursor:hand;");   // Connect / Stop / Disconnect, always visible
        headAct.setOnMouseClicked(e -> com.ars.fx.shell.FeedUi.toggle(cc));
        headActRef = headAct;
        Label cluTitle = lbl("DX Cluster", "jhub-card-title");
        for (Label l : new Label[]{cluTitle, headAct, consoleBtn}) l.setMinWidth(Region.USE_PREF_SIZE);   // never truncated; the server text gives way instead
        cluState.setMinWidth(0); src.setMinWidth(0); HBox.setHgrow(src, Priority.SOMETIMES);
        HBox head = new HBox(11, ic, cluTitle, hsp, src, headAct, consoleBtn); head.getStyleClass().add("jhub-card-head"); head.setAlignment(Pos.CENTER_LEFT);

        Label fltSummary = lbl("", "jhub-clu-flt");
        HBox flt = new HBox(8, fltSummary);
        Region fsp = new Region(); HBox.setHgrow(fsp, Priority.ALWAYS); flt.getChildren().addAll(fsp, lbl("+ Spot", "jhub-clu-flt"));
        flt.setAlignment(Pos.CENTER_LEFT); flt.setPadding(new Insets(12, 16, 12, 16));

        VBox rows = new VBox();
        Label empty = lbl(com.ars.fx.shell.FeedUi.emptyList(cc, "cluster")); empty.setStyle("-fx-font-size:12px;-fx-text-fill:-ars-t4;-fx-padding:18 16 18 16;"); empty.setWrapText(true);
        rows.getChildren().add(empty);
        ScrollPane rsp = new ScrollPane(rows); rsp.setFitToWidth(true); rsp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); rsp.setStyle("-fx-background-color:transparent;"); VBox.setVgrow(rsp, Priority.ALWAYS);
        VBox card = new VBox(head, flt, rsp); card.getStyleClass().add("jhub-card"); VBox.setVgrow(card, Priority.ALWAYS);

        // ---- controls drawer ----
        TextField login  = new TextField(cc.loginCall()); login.getStyleClass().add("jhub-mini-field"); login.setMinWidth(96); login.setMaxWidth(96);
        ComboBox<String> server = clusterPicker(login::getText); server.setMaxWidth(Double.MAX_VALUE);
        VBox serverF = labeled("CLUSTER SERVER", server); HBox.setHgrow(serverF, Priority.ALWAYS); serverF.setMaxWidth(Double.MAX_VALUE);
        HBox row1 = new HBox(12, serverF, labeled("LOGIN CALL", login)); row1.setAlignment(Pos.BOTTOM_LEFT);

        Label statusLbl = lbl("", "jhub-conn");
        Label act = lbl("", "jhub-clearbtn");

        // single render: applies filters to the live feed + refreshes status everywhere
        Runnable render = () -> Shell.keepScroll(rsp, () -> {
            boolean conn = cc.isConnected();
            cdot.setStyle(com.ars.fx.shell.FeedUi.dot(cc.status()));
            cluState.setText(com.ars.fx.shell.FeedUi.line(cc));
            if (headActRef != null) headActRef.setText(com.ars.fx.shell.FeedUi.action(cc));
            if (headerDot != null) headerDot.setStyle(com.ars.fx.shell.FeedUi.dot(cc.status()));
            if (headerTxt != null) headerTxt.setText(cc.status().label() + " · " + cc.spots().size() + " spots");
            java.util.Set<String> worked = workedSet();
            java.util.List<ClusterClient.Spot> all = cc.spots();
            java.util.List<ClusterClient.Spot> shown = new java.util.ArrayList<>();
            for (ClusterClient.Spot s : all)
                if (bandOk(s) && modeOk(s) && (!cluNeededOnly || isNeeded(s, worked))) shown.add(s);
            rows.getChildren().clear();
            if (shown.isEmpty()) {
                empty.setText(conn && !all.isEmpty() ? "No spots match the current filter" : com.ars.fx.shell.FeedUi.emptyList(cc, "cluster"));
                rows.getChildren().add(empty);
            } else {
                for (ClusterClient.Spot s : shown) {
                    Region row = cluRow(new String[]{
                            ClusterClient.fmtFreq(s.freqHz()), s.call(), isNeeded(s, worked) ? "y" : "", s.mode(), s.band(),
                            s.de(), s.comment(), ClusterClient.age(s.arrivalMs()) });
                    row.setStyle("-fx-cursor:hand;");
                    com.ars.fx.shell.Tips.tip(row, "Click to use this spot — fills the log entry, tunes the rig, and offers to turn the beam");
                    row.setOnMouseClicked(e -> SpotAction.onSpot(row, s.call(), s.freqHz(), s.mode(), s.band()));
                    rows.getChildren().add(row);
                }
            }
            fltSummary.setText(filterSummary() + "  ·  " + shown.size() + (cluNeededOnly ? " needed" : " shown"));
            boolean want = cc.wantConnected();
            statusLbl.getStyleClass().remove("live");
            statusLbl.setText(com.ars.fx.shell.FeedUi.symbolLine(cc) + (cc.status() == com.ars.fx.data.TelnetFeed.Status.CONNECTING && !cc.detail().isEmpty() ? " — " + cc.detail() : ""));
            if (conn) statusLbl.getStyleClass().add("live");
            act.setText(com.ars.fx.shell.FeedUi.action(cc));
        });

        VBox bandsF = labeled("BANDS", segSel(CLU_BANDS, cluSel("data.clusterBand", CLU_BANDS), i -> { com.ars.fx.data.HubConfig.set("data.clusterBand", CLU_BANDS[i]); render.run(); }));
        VBox modesF = labeled("MODES", segSel(CLU_MODES, cluSel("data.clusterMode", CLU_MODES), i -> { com.ars.fx.data.HubConfig.set("data.clusterMode", CLU_MODES[i]); render.run(); }));
        HBox row2 = new HBox(12, bandsF, modesF); row2.setAlignment(Pos.BOTTOM_LEFT);

        Label nkl = lbl("Needed (ATNO/band-fill) only"); nkl.getStyleClass().add("k"); HBox.setHgrow(nkl, Priority.ALWAYS); nkl.setMaxWidth(Double.MAX_VALUE);
        HBox needed = new HBox(nkl, switchToggle(cluNeededOnly, v -> { cluNeededOnly = v; render.run(); }));
        needed.getStyleClass().add("sx-kv"); needed.setAlignment(Pos.CENTER_LEFT);

        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        act.setStyle("-fx-cursor:hand;");
        act.setOnMouseClicked(e -> { com.ars.fx.shell.FeedUi.toggle(cc); render.run(); });
        Label apply = lbl("Apply login", "jhub-clearbtn"); apply.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(apply, "Use the login call typed above and reconnect");
        apply.setOnMouseClicked(e -> {
            com.ars.fx.data.HubConfig.set("data.clusterLogin", login.getText().trim());
            cc.setEndpoint(null, -1, login.getText());
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
        return b[cluSel("data.clusterBand", CLU_BANDS)] + " · " + m[cluSel("data.clusterMode", CLU_MODES)];
    }
    /** Interactive segmented control: highlights the chosen option and reports its index. */
    static Region segSel(String[] opts, int on, java.util.function.IntConsumer onSelect) {
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
    static Region switchToggle(boolean initial, java.util.function.Consumer<Boolean> onChange) {
        boolean[] on = {initial};
        Region knob = new Region(); knob.getStyleClass().add("jm-switch-knob");
        StackPane sw = new StackPane(knob); sw.getStyleClass().add("jm-switch"); sw.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(sw, "Click to switch on or off");
        sw.setMinSize(36, 20); sw.setPrefSize(36, 20); sw.setMaxSize(36, 20);   // the row would otherwise stretch it into a circle
        StackPane.setMargin(knob, new Insets(0, 3, 0, 3));
        Runnable paint = () -> {
            StackPane.setAlignment(knob, on[0] ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
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
        VBox ttl = new VBox(1, lbl("J-Log-Quick", "jhub-card-title"), sub);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label open = lbl("Open J-Log →", "jhub-open");
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
        body.getChildren().add(Shell.bandCaption(tFreq));       // "20m · CW segment" from the shared band plan
        body.getChildren().add(frow(labeled("RST ↑", tUp), false, labeled("RST ↓", tDn), false, labeled("NAME", tName), true));
        body.getChildren().add(frow(labeled("QTH", tQth), true, labeled("GRID", tGrid), false));

        VBox cmt = labeled("COMMENT", tCmt); HBox.setHgrow(cmt, Priority.ALWAYS); cmt.setMaxWidth(Double.MAX_VALUE);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Label clear = lbl("Clear", "jhub-clearbtn");
        Label logBtn = lbl("Log QSO ⏎", "jl-logbtn");
        Label printQsl = Shell.placeholderButton("Print QSL label", "jhub-clearbtn", "Print a QSL label for this QSO — coming soon");   // not hooked up yet
        HBox cmtRow = new HBox(12, cmt, sp2, printQsl, clear, logBtn);
        cmtRow.setAlignment(Pos.BOTTOM_LEFT);
        body.getChildren().add(cmtRow);

        // F1–F8 station macros (shared with J-Log normal + contest)
        Label txEcho = lbl("", "jhub-mini-hint");
        body.getChildren().add(MacroBar.sendAsRow(tMode));
        body.getChildren().add(MacroBar.row(MacroBar.fire(() -> tMode.getText(), () -> tCall.getText(), msg -> txEcho.setText("▶ " + msg))));
        body.getChildren().add(txEcho);

        // today's log — read back from the DB
        Label tlh = lbl("☰  Recent log"); tlh.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:-ars-t3;");
        Label tln = lbl(""); tln.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-t4;");
        HBox tlbar = new HBox(8, tlh, tln); tlbar.getStyleClass().add("jhub-card-head"); tlbar.setAlignment(Pos.CENTER_LEFT);
        VBox tbl = new VBox();
        fillLog(tbl, tln);

        // wiring
        String[] lastLk = {""};
        Runnable clearForm = () -> { tCall.clear(); tName.clear(); tQth.clear(); tGrid.clear(); tCmt.clear(); lastLk[0] = ""; };
        Runnable doLog = () -> {
            String call = tCall.getText();
            if (call == null || call.isBlank()) { tCall.requestFocus(); return; }
            String freq = tFreq.getText();
            boolean ok = JLogDb.insert(call, freq, JLogDb.bandFor(freq), tMode.getText(),
                    tUp.getText(), tDn.getText(), tName.getText(), tQth.getText(), tGrid.getText(), tCmt.getText());
            if (ok) {
                clearForm.run();
                tCall.requestFocus();
                fillLog(tbl, tln);
                sub.setText(JLogDb.count() + " in log · live");
            }
        };
        logBtn.setOnMouseClicked(e -> doLog.run());
        tCall.setOnAction(e -> doLog.run());
        tCmt.setOnAction(e -> doLog.run());
        clear.setOnMouseClicked(e -> { clearForm.run(); tCall.requestFocus(); });

        // Callbook lookup for the call field: on leaving it, and on every spot click (same as the full J-Log form).
        Runnable doLookup = () -> {
            String c = tCall.getText() == null ? "" : tCall.getText().trim().toUpperCase();
            if (c.length() < 3 || c.equals(lastLk[0])) return;
            lastLk[0] = c;
            if (!com.ars.fx.data.LookupConfig.anyEnabled()) return;
            Thread t = new Thread(() -> {
                com.ars.fx.data.CallsignLookup.Result r = com.ars.fx.data.CallsignLookup.lookup(c);
                if (r == null) return;
                javafx.application.Platform.runLater(() -> {
                    if (!c.equals(tCall.getText() == null ? "" : tCall.getText().trim().toUpperCase())) return;   // the operator moved on
                    quickFill(tName, r.name()); quickFill(tGrid, r.grid()); quickFill(tQth, r.city());
                });
            }, "quick-lookup");
            t.setDaemon(true); t.start();
        };
        tCall.focusedProperty().addListener((o, was, now) -> { if (!now) doLookup.run(); });

        // A spot click anywhere (cluster list, map...) starts a fresh contact here: clear, apply the call, look it up.
        java.util.function.Consumer<SpotAction.Prefill> onSpot = p -> {
            Runnable r = () -> {
                clearForm.run();
                if (p.call() != null) tCall.setText(p.call());
                if (p.freqMhz() != null) tFreq.setText(p.freqMhz());
                if (p.mode() != null && !p.mode().isBlank()) tMode.setText(p.mode());
                tCall.requestFocus(); tCall.positionCaret(tCall.getText().length());
                doLookup.run();
            };
            if (javafx.application.Platform.isFxApplicationThread()) r.run(); else javafx.application.Platform.runLater(r);
        };

        VBox card = new VBox(head, body, tlbar, tbl); card.getStyleClass().add("jhub-card");
        card.sceneProperty().addListener((o, was, sc) -> {   // only a target while this card is actually on screen
            if (sc != null) SpotAction.setQuickTarget(onSpot); else SpotAction.clearQuickTarget(onSpot);
        });
        HBox.setHgrow(card, Priority.ALWAYS); card.setMinWidth(420);
        return card;
    }

    private static void quickFill(TextField t, String v) {
        if (v != null && !v.isBlank() && t.getText().isBlank()) t.setText(v.trim());
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
        return com.ars.fx.shell.Shell.caps(t);   // J-Log-Quick entry is upper case
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
        VBox col = new VBox(11, com.ars.fx.shell.RigPanel.build(),
                Shell.rotorControlDrawer(),
                Shell.drawer("Amplifier", "bridge", "bridge", "", false, ampBody()),
                Shell.propagationDrawer(),
                Shell.solarSpaceWeatherDrawer());
        col.setMinWidth(322); col.setPrefWidth(322); col.setMaxWidth(322);
        ScrollPane sp = new ScrollPane(col);                  // floating, it can't lean on the page to scroll
        sp.getStyleClass().add("sx-dock-scroll"); sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return Shell.collapsibleRail(sp);
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

    // ---- config page (shot 09) --------------------------------------------
    private static Region configCenter() {
        HBox crumb = new HBox(7, lbl("J-Hub","jhub-crumb"), lbl("›","jhub-crumb"), lbl("Hardware","jhub-crumb"),
                lbl("›","jhub-crumb"), lbl("Rig control","cfg-title")); crumb.setAlignment(Pos.BOTTOM_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label back = lbl("← Dashboard","cfg-link"); back.setStyle("-fx-cursor:hand;"); back.setOnMouseClicked(e -> Shell.navigate("hub"));
        HBox top = new HBox(crumb, sp, back); top.setAlignment(Pos.BOTTOM_LEFT);
        Label desc = lbl("CAT control for your transceiver — connection, PTT, and how J-Hub follows the radio.","cfg-desc");

        com.ars.fx.data.RigClient.getInstance().start();
        HBox cells = liveStrip(new String[]{"STATUS", "FREQUENCY", "MODE", "RIGCTLD"}, () -> {
            com.ars.fx.data.RigClient rc = com.ars.fx.data.RigClient.getInstance();
            com.ars.fx.data.RigClient.State st = rc.last();
            boolean on = st != null && st.connected();
            return new String[][]{
                {on ? "● CONNECTED" : "○ OFFLINE", "", on ? "1" : "0"},
                {on && st.freqHz() > 0 ? com.ars.fx.data.RigClient.fmtFreq(st.freqHz()) : "—", on && st.freqHz() > 0 ? "MHz" : "", "0"},
                {on && st.mode() != null && !st.mode().isBlank() ? st.mode() : "—", "", "0"},
                {rc.endpoint(), "", "0"}};
        });

        VBox conn = section("CONNECTION",
            cfgRow("Transceiver model", null, cInputPrompt("rig.model", "e.g. Icom IC-7610")),
            cfgRow("Hamlib model #", "rigctld -m (run: rigctl --list)", cInput("rig.hamlibModel", "")),
            cfgRow("Interface", null, cSeg("rig.interface", new String[]{"USB","Serial","Network"}, 0)),
            cfgRow("Port", null, cInput("rig.port", "/dev/ttyUSB0")),
            cfgRow("Baud rate", null, cSelect("rig.baud", "9600", BAUDS)),
            cfgRow("CI-V address", "Icom civ address (hex)", cInputPrompt("rig.civ", "e.g. 0x98")),
            cfgRow("Poll interval", "how often J-Hub asks rigctld for frequency, mode and meters (250–5000 ms)", cInputApply("rig.poll", "800", () -> {})),
            cfgRow("rigctld host", "Hamlib rig daemon J-Hub connects to", cInputApply("rig.rigctldHost", "127.0.0.1", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("rigctld port", null, cInputApply("rig.rigctldPort", "4532", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())));
        VBox ptt = section("PTT & KEYING",
            cfgRow("PTT method", "CAT keys over the CAT link. RTS / DTR key that line of the serial port (needs Start rigctld for me). VOX keys nothing — the radio or interface senses the audio",
                    cSegApply("rig.ptt", new String[]{"CAT","RTS","DTR","VOX"}, 0, () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("CW keyer", null, cInputPrompt("rig.keyer", "e.g. Winkeyer USB")));
        VBox beh = section("BEHAVIOR",
            cfgRow("Auto band-follow", "not applied yet", cToggle("rig.autoBandFollow", true)),
            cfgRow("Start rigctld for me", "spawn the daemon with the model/serial above", cToggleApply("rig.startDaemon", false, on -> com.ars.fx.data.RemoteLink.nudgeReconfig())));

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

        VBox callCell = statCell("CALLSIGN", "", "", true), gridCell = statCell("GRID", "", "", false),
             zoneCell = statCell("ITU / CQ", "", "", false), dxccCell = statCell("DXCC", "", "", false);
        stationStrip = new VBox[]{callCell, gridCell, zoneCell, dxccCell};
        refreshStationStrip();
        startStationStripListener();
        HBox cells = new HBox(0, callCell, gridCell, zoneCell, dxccCell);
        cells.getStyleClass().add("cfg-status"); cells.setAlignment(Pos.CENTER_LEFT);

        VBox id = section("IDENTITY",
            cfgRow("Callsign", null, cInputApply("station.call", "", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Operator name", null, cInputPrompt("station.name", "your first name")),
            cfgRow("QTH", "city, state", cInputPrompt("station.qth", "e.g. Baltimore MD")),
            cfgRow("Grid square", "6-char Maidenhead locator", cInput("station.grid", "")));
        VBox loc = section("LOCATION",
            cfgRow("Latitude", "decimal degrees (N+)", cInputPrompt("station.lat", "decimal degrees, N+")),
            cfgRow("Longitude", "decimal degrees (E+)", cInputPrompt("station.lon", "decimal degrees, E+")),
            cfgRow("Elevation", "height above sea level — shown in the unit chosen under Units", elevationField()));
        VBox zones = section("ZONES & AWARDS",
            cfgRow("ITU zone", null, cInputPrompt("station.ituZone", "your ITU zone")),
            cfgRow("CQ zone", null, cInputPrompt("station.cqZone", "your CQ zone")),
            cfgRow("IARU region", null, cSegApply("station.iaru", new String[]{"R1","R2","R3"}, 1, com.ars.fx.data.Bands::applyRegion)),
            cfgRow("Default TX power", "pre-fills the power on a new QSO in J-Log", cUnit("station.txPower", "100", "W")));

        VBox units = section("UNITS",
            cfgRow("Units", "Metric: km · °C · km/h · hPa · m      Standard (US): mi · °F · mph · inHg · ft", unitSystemSeg()));
        Label theme = lbl("◑ " + com.ars.fx.Themes.byKey(com.ars.fx.Themes.global()).label(), "sx-theme");
        theme.setStyle("-fx-cursor:hand;");
        theme.setOnMouseClicked(e -> theme.setText("◑ " + Shell.cycleGlobalTheme().label()));
        VBox appearance = section("APPEARANCE",
            cfgRow("Colour scheme", "click to switch the scheme for the whole suite", theme));
        Label checkMsg = lbl("", "cfg-lh");
        Label checkBtn = lbl("Check now", "sx-theme"); checkBtn.setStyle("-fx-cursor:hand;");
        checkBtn.setOnMouseClicked(e -> {
            checkMsg.setText("Checking…");
            Thread t = new Thread(() -> {
                String msg;
                try {
                    var r = com.ars.fx.data.UpdateChecker.check(true);
                    msg = r == null ? "You're up to date (v" + com.ars.fx.AppVersion.current() + ")." : r.tag() + " is available.";
                } catch (Exception ex) { msg = "Couldn't reach GitHub: " + ex.getMessage(); }
                String m = msg;
                javafx.application.Platform.runLater(() -> checkMsg.setText(m));
            }, "update-check-manual");
            t.setDaemon(true); t.start();
        });
        HBox checkRow = new HBox(12, checkBtn, checkMsg); checkRow.setAlignment(Pos.CENTER_LEFT);
        VBox updates = section("UPDATES",
            cfgRow("Version", "this build", lbl("v" + com.ars.fx.AppVersion.current(), "cfg-sv")),
            cfgRow("Check for updates", "once a day, from GitHub — nothing about you is sent", cToggle("update.check", true)),
            cfgRow("Check now", null, checkRow));
        VBox v = new VBox(20, top, desc, cells, id, loc, units, appearance, updates, zones);
        v.setPadding(new Insets(18, 28, 40, 28)); v.setMaxWidth(1080); v.setStyle("-fx-background-color:-ars-bg;");
        return v;
    }

    /** A status strip whose values are re-read from {@code vals} once a second while it is on screen: one {value, unit, "1" = ok}
     *  per key. The strips on the hardware pages show what the rig / rotor / amp / feeds are doing now, not what they were
     *  when the page was opened. */
    private static HBox liveStrip(String[] keys, java.util.function.Supplier<String[][]> vals) { return liveStrip(keys, vals, 150); }
    private static HBox liveStrip(String[] keys, java.util.function.Supplier<String[][]> vals, double cellWidth) {
        VBox[] cells = new VBox[keys.length];
        for (int i = 0; i < keys.length; i++) { cells[i] = statCell(keys[i], "", "", false); cells[i].setMinWidth(cellWidth); }
        HBox row = new HBox(0, cells); row.getStyleClass().add("cfg-status"); row.setAlignment(Pos.CENTER_LEFT);
        Runnable paint = () -> {
            String[][] v;
            try { v = vals.get(); } catch (RuntimeException ex) { return; }
            for (int i = 0; i < cells.length && i < v.length; i++) {
                Label val = stripValue(cells[i]);
                val.setText(v[i][0]);
                val.getStyleClass().remove("ok");
                if ("1".equals(v[i][2])) val.getStyleClass().add("ok");
                HBox vu = (HBox) cells[i].getChildren().get(1);
                if (v[i][1].isEmpty()) { if (vu.getChildren().size() > 1) vu.getChildren().remove(1); }
                else if (vu.getChildren().size() > 1) ((Label) vu.getChildren().get(1)).setText(v[i][1]);
                else vu.getChildren().add(lbl(v[i][1], "cfg-sval-u"));
            }
        };
        paint.run();
        Timeline t = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> paint.run()));
        t.setCycleCount(Timeline.INDEFINITE);
        row.sceneProperty().addListener((o, was, sc) -> { if (sc == null) t.stop(); else { paint.run(); t.play(); } });
        return row;
    }

    // ---- Station page status strip: follows the saved settings as they are edited ----------------------------
    private static VBox[] stationStrip;
    private static boolean stationStripListening;
    private static Label stripValue(VBox cell) { return (Label) ((HBox) cell.getChildren().get(1)).getChildren().get(0); }
    private static Label stripUnit(VBox cell) { HBox vu = (HBox) cell.getChildren().get(1); return vu.getChildren().size() > 1 ? (Label) vu.getChildren().get(1) : null; }
    private static String orDash(String s) { return s == null || s.isBlank() ? "—" : s.trim(); }
    private static void refreshStationStrip() {
        VBox[] c = stationStrip;
        if (c == null) return;
        String call = com.ars.fx.data.HubConfig.call();
        stripValue(c[0]).setText(orDash(call));
        stripValue(c[1]).setText(orDash(com.ars.fx.data.HubConfig.grid()).toUpperCase());
        stripValue(c[2]).setText(orDash(com.ars.fx.data.HubConfig.get("station.ituZone", "")) + " / " + orDash(com.ars.fx.data.HubConfig.get("station.cqZone", "")));
        // the DXCC entity follows the callsign: number and country, or a dash when the call can't be resolved
        com.ars.fx.data.DxccInfo.Info info = call == null || call.isBlank() ? null : com.ars.fx.data.DxccInfo.of(call);
        stripValue(c[3]).setText(info == null || info.dxcc() <= 0 ? "—" : String.valueOf(info.dxcc()));
        Label u = stripUnit(c[3]);
        if (u == null) { u = lbl("", "cfg-sval-u"); ((HBox) c[3].getChildren().get(1)).getChildren().add(u); }
        u.setText(info == null || info.country() == null ? "" : info.country());
    }
    private static void startStationStripListener() {
        if (stationStripListening) return;
        stationStripListening = true;
        com.ars.fx.data.HubConfig.addChangeListener(key -> {
            if (key == null || !(key.startsWith("station.") || key.equals("*"))) return;
            javafx.application.Platform.runLater(JHubDashboard::refreshStationStrip);
        });
    }

    /** Metric / Standard switch. Rebuilds the page so the elevation field and other labels follow at once. */
    private static Region unitSystemSeg() {
        int on = com.ars.fx.data.Units.standard() ? 1 : 0;
        return segSel(new String[]{"Metric", "Standard (US)"}, on, i -> {
            com.ars.fx.data.Units.System next = i == 1 ? com.ars.fx.data.Units.System.STANDARD : com.ars.fx.data.Units.System.METRIC;
            if (next == com.ars.fx.data.Units.system()) return;
            double m = com.ars.fx.data.Units.heightToMeters(parseD(com.ars.fx.data.HubConfig.get("station.elev", "0")));   // keep the same real height
            com.ars.fx.data.Units.set(next);
            com.ars.fx.data.HubConfig.set("station.elev", String.valueOf(Math.round(com.ars.fx.data.Units.heightFromMeters(m))));
            com.ars.fx.data.RemoteLink.nudgeReconfig();
            Shell.navigate("station");
        });
    }
    private static double parseD(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; } }

    /** Station elevation, stored and shown in the chosen height unit (ft or m). */
    private static Region elevationField() { return cUnit("station.elev", "0", com.ars.fx.data.Units.heightUnit() + " AMSL"); }
    // ---- shared config-page chrome ----------------------------------------
    static HBox cfgTop(String... crumbs) {
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
    static Region cfgPage(Node... rows) {
        VBox v = new VBox(20, rows); v.setPadding(new Insets(18, 28, 40, 28)); v.setMaxWidth(1080); v.setStyle("-fx-background-color:-ars-bg;");
        return v;
    }

    private static Region rotorCenter() {
        com.ars.fx.data.RotorClient.getInstance().start();
        HBox cells = liveStrip(new String[]{"STATUS", "AZIMUTH", "ELEVATION", "ROTCTLD"}, () -> {
            com.ars.fx.data.RotorClient ro = com.ars.fx.data.RotorClient.getInstance();
            boolean on = ro.last().connected();
            return new String[][]{
                {on ? "● CONNECTED" : ro.isConfigured() ? "○ ROTCTLD OFFLINE" : "○ NOT SET UP", "", on ? "1" : "0"},
                {on ? String.format("%03.0f", ro.headingDeg()) : "—", on ? "°" : "", "0"},
                {on ? String.format("%.0f", ro.last().el()) : "—", on ? "°" : "", "0"},
                {ro.endpoint(), "", "0"}};
        });
        VBox conn = section("CONNECTION",
            cfgRow("Rotor model", null, cInputPrompt("rotor.model", "e.g. Yaesu G-1000DXA")),
            cfgRow("Hamlib model #", "rotctld -m (run: rotctl --list)", cInput("rotor.hamlibModel", "")),
            cfgRow("Interface", null, cSeg("rotor.interface", new String[]{"USB","Serial","Network"}, 1)),
            cfgRow("Serial port", null, cInput("rotor.port", "/dev/ttyUSB0")),
            cfgRow("Baud rate", null, cSelect("rotor.baud", "9600", BAUDS)),
            cfgRow("rotctld host", "Hamlib rotor daemon", cInputApply("rotor.host", "127.0.0.1", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("rotctld port", null, cInputApply("rotor.rotctldPort", "4533", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Start rotctld for me", null, cToggleApply("rotor.startDaemon", false, on -> com.ars.fx.data.RemoteLink.nudgeReconfig())));
        VBox beh = section("BEHAVIOR",
            cfgRow("Overlap / wrap", "where the rotor crosses — not applied yet", cSeg("rotor.wrap", new String[]{"North","South"}, 0)),
            cfgRow("Park position", "the Park button on the rotor panel", cUnit("rotor.park", "0", "° az")),
            cfgRow("Dead-band", "satellite tracking skips moves smaller than this", cUnit("rotor.deadband", "2", "°")),
            cfgRow("Auto-track satellites (J-Sat)", "same switch as Track with rotor in J-Sat", cToggle("rotor.autoTrack", false)));
        VBox presets = section("BEAM PRESETS", presetRows());
        return cfgPage(cfgTop("J-Hub","Hardware","Rotor control"),
            lbl("Az/el rotor control via Hamlib rotctld — connection, presets and park position.","cfg-desc"),
            cells, conn, beh, presets);
    }
    private static Region amplifierCenter() {
        com.ars.fx.data.AmpClient.getInstance().start();
        HBox cells = liveStrip(new String[]{"STATUS", "BAND", "SWR", "AMPCTLD"}, () -> {
            com.ars.fx.data.AmpClient am = com.ars.fx.data.AmpClient.getInstance();
            com.ars.fx.data.AmpClient.State st = am.last();
            boolean on = st != null && st.connected();
            com.ars.fx.data.RigClient.State rig = com.ars.fx.data.RigClient.getInstance().last();
            String band = rig != null && rig.connected() && rig.freqHz() > 0 ? com.ars.fx.data.Bands.of(rig.freqHz()) : "";
            return new String[][]{
                {on ? "● " + st.mode().toUpperCase() : "○ OFFLINE", "", on ? "1" : "0"},
                {band.isEmpty() ? "—" : band, "", "0"},
                {on && st.swr() > 0 ? String.format("%.1f:1", st.swr()) : "—", "", "0"},
                {am.endpoint(), "", "0"}};
        });
        VBox conn = section("CONNECTION",
            cfgRow("Amplifier model", null, cInputPrompt("amp.model", "e.g. Elecraft KPA-1500")),
            cfgRow("Hamlib model #", "ampctld -m (run: ampctl --list)", cInput("amp.hamlibModel", "")),
            cfgRow("Interface", null, cSeg("amp.interface", new String[]{"USB","Serial","Network"}, 0)),
            cfgRow("Serial port", null, cInput("amp.port", "/dev/ttyACM0")),
            cfgRow("Baud rate", null, cSelect("amp.baud", "38400", BAUDS)),
            cfgRow("ampctld host", "Hamlib amp daemon", cInputApply("amp.host", "127.0.0.1", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("ampctld port", null, cInputApply("amp.ampctldPort", "4531", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Start ampctld for me", null, cToggleApply("amp.startDaemon", false, on -> com.ars.fx.data.RemoteLink.nudgeReconfig())));
        VBox beh = section("BEHAVIOR",
            cfgRow("Follow rig band", "auto band-switch with the radio — not applied yet", cToggle("amp.followBand", true)),
            cfgRow("Max power", "for your reference and macros — not applied yet", cUnit("amp.maxPower", "1000", "W")),
            cfgRow("Standby on TX inhibit", "not applied yet", cToggle("amp.standbyOnInhibit", true)),
            cfgRow("Operate / Standby", "not applied yet — set it on the amplifier", cSeg("amp.opStandby", new String[]{"Operate","Standby"}, 1)));
        return cfgPage(cfgTop("J-Hub","Hardware","Amplifier"),
            lbl("Linear amplifier control via Hamlib ampctld — band-follow, power limit and standby.","cfg-desc"),
            cells, conn, beh);
    }
    private static Region antennaSwitchCenter() {
        HBox cells = liveStrip(new String[]{"STATUS", "ACTIVE", "ANTENNA", "PORTS"}, () -> {
            int named = 0;
            for (int i = 1; i <= 8; i++) if (!com.ars.fx.data.HubConfig.get("ant.port" + i, "").isBlank()) named++;
            return new String[][]{
                {"○ NO CONTROL", "", "0"}, {"—", "", "0"}, {"—", "", "0"},
                {named == 0 ? "—" : String.valueOf(named), named == 0 ? "" : "named", "0"}};
        });
        VBox conn = section("CONNECTION",
            cfgRow("Switch model", null, cInputPrompt("ant.model", "e.g. 4O3A Antenna Genius")),
            cfgRow("Interface", null, cSeg("ant.interface", new String[]{"USB","Serial","Network"}, 2)),
            cfgRow("Host / IP", null, cInputPrompt("ant.host", "e.g. 192.168.1.50")),
            cfgRow("Port", null, cInput("ant.port", "9007")),
            cfgRow("Follow rig band", "select antenna by band", cToggle("ant.followBand", true)));
        String[][] ports = {{"1","160m dipole","160m"},{"2","80m vertical","80m"},{"3","20m beam","20–10m"},
                {"4","40m delta loop","40m"},{"5","6m yagi","6m"},{"6","RX loop","RX only"}};
        Node[] rows = new Node[ports.length];
        for (int i = 0; i < ports.length; i++) rows[i] = cfgRow("Port " + ports[i][0], "bands " + ports[i][2], cInput("ant.port" + ports[i][0], ports[i][1]));
        VBox ant = section("ANTENNA PORTS", rows);
        return cfgPage(cfgTop("J-Hub","Hardware","Antenna switch"),
            lbl("Port names for your antenna switch, saved for reference and macros. Switch control is not built in yet.","cfg-desc"),
            cells, conn, ant);
    }
    /** One part of what used to be the single long Data page; each now has its own entry in the Setup menu. */
    private static Region dataPage(String which) {
        HBox cells = liveStrip(new String[]{"CLUSTER", "RBN", "WSJT-X", "LOG DB"}, () -> {
            com.ars.fx.data.ClusterClient cc = com.ars.fx.data.ClusterClient.getInstance();
            com.ars.fx.data.HeardByClient hb = com.ars.fx.data.HeardByClient.getInstance();
            boolean wsjtx = com.ars.fx.data.BridgeService.connected();
            return new String[][]{
                {cc.status().label(), cc.endpoint(), cc.isConnected() ? "1" : "0"},
                {hb.status().label(), hb.endpoint(), hb.isConnected() ? "1" : "0"},
                {wsjtx ? "● linked" : "○ waiting", "UDP " + com.ars.fx.data.BridgeService.port(), wsjtx ? "1" : "0"},
                {com.ars.fx.data.LogDbInfo.pretty(com.ars.fx.data.LogDbInfo.dataDir()), "", "0"}};
        }, 260);
        Runnable applyCluster = () -> {
            String srv = com.ars.fx.data.HubConfig.get("data.clusterServer", "dxc.ve7cc.net:23").trim();
            String login = com.ars.fx.data.HubConfig.get("data.clusterLogin", com.ars.fx.data.HubConfig.call());
            String host = srv; int port = 23; int c = srv.lastIndexOf(':');
            if (c > 0) { host = srv.substring(0, c); try { port = Integer.parseInt(srv.substring(c + 1).trim()); } catch (Exception ignored) {} }
            ClusterClient.getInstance().setEndpoint(host, port, login);
        };
        VBox cluster = section("DX CLUSTER",
            cfgRow("Cluster server", "pick one, or type host:port and press Enter; one runs at a time, and a live connection moves to the new server",
                    tagged(clusterPicker(() -> com.ars.fx.data.HubConfig.get("data.clusterLogin", com.ars.fx.data.HubConfig.call())), "data.clusterServer")),
            cfgRow("Login callsign", null, cInputApply("data.clusterLogin", com.ars.fx.data.HubConfig.call(), applyCluster)),
            cfgRow("Auto-connect on start", "off by default — connect from the cluster panel when you want spots", cToggle("data.clusterAutoConnect", false)),
            cfgRow("Connection tries", "give up after this many failed attempts (0 = keep trying); Connect starts a fresh count",
                    cInputApply("data.clusterMaxTries", "5", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("Band filter", null, cSeg("data.clusterBand", new String[]{"All","HF","VHF+"}, 0)),
            cfgRow("Mode filter", null, cSeg("data.clusterMode", new String[]{"All","CW","Phone","Digi"}, 0)),
            cfgRow("RBN connection tries", "give up after this many failed attempts (0 = keep trying)",
                    cInputApply("data.rbnMaxTries", "5", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("RBN auto-connect on start", "off by default — \"who's hearing me\" skimmer spots; Connect from the Heard By panel",
                    cToggle("data.rbnAutoConnect", false)));
        VBox wsjt = section("WSJT-X / DIGITAL",
            cfgRow("WSJT-X UDP port", "set the same port in WSJT-X ▸ Settings ▸ Reporting; applied as soon as you leave the field", cInputApply("data.wsjtxPort", "2237", com.ars.fx.data.BridgeService::restart)),
            cfgRow("Local CW skimmer", "J-Digi listens to the whole audio passband for CW, picks out callsigns it is confident of and adds them to the spot list as \"-SKM\" spots. Needs the rig connected (Rig control) and uses some CPU",
                    cToggle("digi.localSkimmer", false)),
            cfgRow("Auto-log FT8/FT4 QSOs", "adds each QSO WSJT-X logs to the J-Log database (skips one already there)", cToggle("data.autoLogFt8", false)),
            cfgRow("PSK Reporter upload", "not applied yet", cToggle("data.pskReporter", false)));
        VBox logbook = section("LOGBOOK & EXPORT",
            cfgRow("Cabrillo / CSV charset", "ASCII drops accents and replaces other characters with ?",  cSeg("data.charset", new String[]{"UTF-8","ASCII"}, 0)),
            cfgRow("LoTW / eQSL auto-upload", "not applied yet — use the Upload section below", cToggle("data.lotwAutoUpload", false)));
        VBox remote = section("REMOTE SHARING (SOLO J-MAP / J-SAT)",
            cfgRow("Share with solo modules on the LAN", "lets a standalone J-Map / J-Sat (2nd PC, shack Pi) use this station's spots, rig + rotor",
                    cToggleApply("remote.serverEnabled", true, on -> com.ars.fx.data.RemoteLink.nudgeReconfig())),
            cfgRow("WebSocket port", "the solo module connects to ws://<this-pc>:<port>",
                    cInputApply("remote.port", "8090", () -> com.ars.fx.data.RemoteLink.nudgeReconfig())));
        VBox contest = section("CONTEST / CABRILLO",
            cfgRow("Operator category", null, cSelect("contest.cab.operator", "SINGLE-OP", new String[]{"SINGLE-OP","MULTI-OP","CHECKLOG"})),
            cfgRow("Power", null, cSelect("contest.cab.power", "HIGH", new String[]{"HIGH","LOW","QRP"})),
            cfgRow("Assisted", null, cSelect("contest.cab.assisted", "NON-ASSISTED", new String[]{"NON-ASSISTED","ASSISTED"})),
            cfgRow("Band category", null, cInput("contest.cab.band", "ALL")),
            cfgRow("Mode category", null, cInput("contest.cab.mode", "MIXED")),
            cfgRow("Station", null, cInput("contest.cab.station", "FIXED")),
            cfgRow("Club", null, cInput("contest.cab.club", "")),
            cfgRow("Sweepstakes precedence", "your sent SS precedence (A/B/U/M/Q/S)", cInput("contest.ss.precedence", "")),
            cfgRow("Sweepstakes check", "year first licensed (2 digits)", cInput("contest.ss.check", "")),
            cfgRow("Sweepstakes section", "your ARRL/RAC section", cInput("contest.ss.section", "")));
        return switch (which) {
            case "tx" -> cfgPage(cfgTop("J-Hub", "Hardware", "Transmit & macros"),
                lbl("CW speed, keyer, PTT delay and voice output, and the F-key macros used by J-Log and J-Digi.", "cfg-desc"),
                txSection(), macroSection());
            case "cluster" -> cfgPage(cfgTop("J-Hub", "Data & spotting", "DX cluster & RBN"),
                lbl("Where spots come from: the DX cluster server and login, and the Reverse Beacon Network (\"who is hearing me\").", "cfg-desc"),
                cells, cluster);
            case "digital" -> cfgPage(cfgTop("J-Hub", "Data & spotting", "WSJT-X / digital"),
                lbl("The WSJT-X / JTDX link, the local CW skimmer and automatic logging of digital QSOs.", "cfg-desc"),
                cells, wsjt);
            case "lookup" -> cfgPage(cfgTop("J-Hub", "Data & spotting", "Callsign lookup"),
                lbl("Callbook providers J-Log and J-Map ask for a station's name, QTH and grid, tried top to bottom.", "cfg-desc"),
                lookupSection());
            case "logdb" -> cfgPage(cfgTop("J-Hub", "Logbook", "Log database & backup"),
                lbl("Which database files J-Log writes to, the contest databases, and ADIF / CSV backup.", "cfg-desc"),
                cells, logDatabaseSection(), JHubContestDbs.build(), backupSection());
            case "upload" -> cfgPage(cfgTop("J-Hub", "Logbook", "Online upload"),
                lbl("Push your log to LoTW, eQSL, QRZ Logbook and Club Log.", "cfg-desc"),
                uploadSection());
            case "export" -> cfgPage(cfgTop("J-Hub", "Logbook", "Export & Cabrillo"),
                lbl("Export character set and the entry details written into Cabrillo contest logs.", "cfg-desc"),
                logbook, contest);
            case "share" -> cfgPage(cfgTop("J-Hub", "System", "Remote sharing"),
                lbl("Let a solo J-Map / J-Sat on another computer use this station's spots, rig and rotor.", "cfg-desc"),
                remote);
            case "learn" -> cfgPage(cfgTop("J-Hub", "System", "J-Learn content"),
                lbl("Update the J-Learn chapters separately from the program.", "cfg-desc"),
                JHubLearnContent.build());
            default -> cfgPage(cfgTop("J-Hub", "Setup"), lbl("Pick a section from the menu on the left.", "cfg-desc"));
        };
    }

    /** Which database files J-Log writes to, with their sizes and a way to open the folder. The export and
     *  upload tools follow directly below on this page. */
    private static VBox logDatabaseSection() {
        com.ars.fx.data.ContestState.ensureLoaded();   // brings the contest engine up so "in use" is the real file
        java.nio.file.Path normal = com.ars.fx.data.LogDbInfo.normalPath(), contest = com.ars.fx.data.LogDbInfo.contestPath();
        Label nCount = lbl(JLogDb.count() + " QSOs · " + com.ars.fx.data.LogDbInfo.size(normal), "cfg-lt");
        com.ars.fx.data.ContestState.ensureLoaded();
        var plug = com.ars.fx.data.ContestState.plugin();
        Label cCount = lbl((plug == null ? "no contest selected" : plug.getContestName() + " · " + com.ars.fx.data.ContestState.qsos().size() + " QSOs")
                + " · " + com.ars.fx.data.LogDbInfo.size(contest), "cfg-lt");
        Label status = lbl("", "cfg-lh"); status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        Label open = cfgBtn("Open folder");
        open.setOnMouseClicked(e -> status.setText(com.ars.fx.data.LogDbInfo.openFolder(com.ars.fx.data.LogDbInfo.dataDir())
                ? "Opened " + com.ars.fx.data.LogDbInfo.pretty(com.ars.fx.data.LogDbInfo.dataDir())
                : "Could not open the folder — it is " + com.ars.fx.data.LogDbInfo.dataDir()));
        Label toLog = cfgBtn("Open J-Log"); toLog.setOnMouseClicked(e -> Shell.navigate("log"));
        HBox btns = new HBox(10, open, toLog); btns.setAlignment(Pos.CENTER_LEFT);
        Region bsp = new Region(); HBox.setHgrow(bsp, Priority.ALWAYS);
        HBox actions = new HBox(20, new VBox(2, lbl("Database folder", "cfg-lt"),
                lbl("Back up, export and upload are in the sections below", "cfg-lh")), bsp, btns);
        actions.getStyleClass().add("cfg-row"); actions.setAlignment(Pos.CENTER_LEFT);
        return section("LOG DATABASE",
            cfgRow("Normal log", com.ars.fx.data.LogDbInfo.normalLine(), nCount),
            cfgRow("Contest log (in use)", com.ars.fx.data.LogDbInfo.contestLine(), cCount),
            actions,
            new HBox(status) {{ getStyleClass().add("cfg-row"); }});
    }

    /** J-Hub ▸ Modules — turn each module on or off (reflected live in the dock + nav). */
    private static Region modulesCenter() {
        java.util.List<Node> rows = new java.util.ArrayList<>();
        for (com.ars.fx.data.ModuleConfig.Mod m : com.ars.fx.data.ModuleConfig.all()) {
            Node tile = Shell.iconTile(m.hue(), m.id(), 13, "sx-dw-ic");
            Label nm = lbl(m.name(), "k"); HBox.setHgrow(nm, Priority.ALWAYS); nm.setMaxWidth(Double.MAX_VALUE);
            VBox txt = new VBox(1, nm, lbl(com.ars.fx.data.ModuleConfig.blurb(m.id()), "jhub-card-sub"));
            HBox.setHgrow(txt, Priority.ALWAYS); txt.setMaxWidth(Double.MAX_VALUE);
            HBox left = new HBox(11, tile, txt); left.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(left, Priority.ALWAYS); left.setMaxWidth(Double.MAX_VALUE);
            HBox row = new HBox(12, left,
                    cToggleApply("module." + m.id() + ".enabled", true, on -> Shell.navigate("setup:modules")));
            row.getStyleClass().add("sx-kv"); row.setAlignment(Pos.CENTER_LEFT);
            rows.add(row);
        }
        VBox list = section("MODULES", rows.toArray(new Node[0]));
        return cfgPage(cfgTop("J-Hub", "Modules"),
                lbl("Turn modules on or off. Disabled modules are hidden from the dock and the J-Hub nav — J-Hub itself is always on.", "cfg-desc"),
                list);
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

        Label imp = cfgBtn("Import ADIF…");
        boolean[] busy = {false};
        imp.setOnMouseClicked(e -> {
            if (busy[0]) return;
            FileChooser fc = new FileChooser(); fc.setTitle("Import ADIF into the normal log");
            fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("ADIF files", "*.adi", "*.adif", "*.ADI", "*.ADIF"), new FileChooser.ExtensionFilter("All files", "*.*"));
            java.io.File f = fc.showOpenDialog(imp.getScene().getWindow());
            if (f == null) return;
            busy[0] = true; status.setText("Importing " + f.getName() + " …");
            Thread t = new Thread(() -> {
                com.ars.fx.data.LogImport.Result r = com.ars.fx.data.LogImport.importAdif(f.toPath());
                javafx.application.Platform.runLater(() -> { busy[0] = false;
                    status.setText(!r.ok() ? "✗ Import failed: " + r.error()
                            : "✓ Imported " + r.imported() + " QSO" + (r.imported() == 1 ? "" : "s") + " from " + f.getName()
                              + (r.duplicates() > 0 ? " · " + r.duplicates() + " already in the log, skipped" : "")
                              + (r.failed() > 0 ? " · " + r.failed() + " records unreadable" : "")); });
            }, "adif-import");
            t.setDaemon(true); t.start();
        });
        com.ars.fx.shell.Tips.tip(imp, "Add the QSOs from an ADIF file to the normal log; ones already logged are skipped");

        VBox card = new VBox(); card.getStyleClass().add("cfg-card");
        card.getChildren().add(cfgRow("Backup folder", "timestamped j-log-YYYYMMDD-HHMMSS files", dir));
        HBox exportRow = new HBox(20, new VBox(2, lbl("Export now", "cfg-lt"), lbl("ADIF for LoTW / eQSL / QRZ / Club Log · CSV for spreadsheets", "cfg-lh")));
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS); exportRow.getChildren().addAll(esp, btns);
        exportRow.getStyleClass().add("cfg-row"); exportRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(exportRow);
        HBox importRow = new HBox(20, new VBox(2, lbl("Import", "cfg-lt"), lbl("ADIF from another logger or a backup → the normal log (duplicates are skipped)", "cfg-lh")));
        Region isp = new Region(); HBox.setHgrow(isp, Priority.ALWAYS); importRow.getChildren().addAll(isp, imp);
        importRow.getStyleClass().add("cfg-row"); importRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(importRow);
        HBox statusRow = new HBox(status); statusRow.getStyleClass().add("cfg-row"); statusRow.setStyle("-fx-border-width:0;");
        card.getChildren().add(statusRow);
        return new VBox(6, lbl("BACKUP, EXPORT & IMPORT", "cfg-sec"), card);
    }
    static Label cfgBtn(String text) {
        Label b = lbl(text, "cfg-select"); b.setStyle("-fx-cursor:hand; -fx-padding:6 14 6 14; -fx-background-color:-ars-accent; -fx-text-fill:#06121a; -fx-font-weight:bold; -fx-background-radius:7;");
        return b;
    }
    /** F1–F8 station macros used by J-Log: CW/RTTY/digital send the text, phone plays the audio file. */
    /** What the F-key macros need to key the radio: CW speed, and the sound output voice macros play to. */
    private static VBox txSection() {
        javafx.scene.control.ComboBox<String> dev = new javafx.scene.control.ComboBox<>();
        dev.getItems().add("System default"); dev.getItems().addAll(com.ars.fx.data.TxController.outputDevices());
        String cur = com.ars.fx.data.HubConfig.get("tx.voiceDevice", "").trim();
        dev.setValue(cur.isEmpty() || !dev.getItems().contains(cur) ? "System default" : cur);
        dev.setMinWidth(260); dev.setMaxWidth(320);
        dev.valueProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set("tx.voiceDevice", b == null || b.equals("System default") ? "" : b));
        return section("TRANSMIT (F-KEY MACROS)",
            cfgRow("CW speed", "F-key macros go to the rig's own keyer (Hamlib send_morse) — the rig must support it. J-Digi uses this speed for CW as well", cUnit("tx.cwWpm", "25", "wpm")),
            cfgRow("J-Digi CW keyer", "Audio: J-Digi makes the tone and keys the rig with the PTT method (Rig control). Rig keyer: the text goes to the rig's own keyer over Hamlib — no audio path needed",
                    cSeg("tx.cwKeyer", new String[]{"Audio","Rig keyer"}, 0)),
            cfgRow("PTT delay", "seconds between keying the rig and the audio starting, and again between the audio ending and dropping the key (0 – 2, e.g. 0.3)",
                    cUnit("tx.pttDelay", "0.2", "s")),
            cfgRow("Voice audio output", "where voice macros play — choose the sound card that feeds the radio. PTT is keyed through the rig (Rig control)", dev));
    }

    private static VBox macroSection() {
        TextField[] lastMacroText = {null};
        VBox card = new VBox(); card.getStyleClass().add("cfg-card");
        // column header
        Label hk = lbl("KEY", "cfg-lh"); hk.setMinWidth(34);
        Label hl = lbl("LABEL", "cfg-lh"); hl.setMinWidth(110);
        Label ht = lbl("CW / RTTY TEXT  ·  fields are filled in when sent", "cfg-lh"); HBox.setHgrow(ht, Priority.ALWAYS); ht.setMaxWidth(Double.MAX_VALUE);
        Label ha = lbl("SSB VOICE FILE", "cfg-lh"); ha.setMinWidth(210);
        HBox header = new HBox(10, hk, hl, ht, ha, spacer(82)); header.getStyleClass().add("cfg-row"); header.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(header);

        for (int i = 0; i < com.ars.fx.data.MacroConfig.count(); i++) {
            final int idx = i;
            com.ars.fx.data.MacroConfig.Macro m = com.ars.fx.data.MacroConfig.get(i);
            Label fk = lbl("F" + (i + 1), "cfg-lt"); fk.setMinWidth(34);
            TextField labelF = macroField(m.label(), 110);
            TextField textF  = com.ars.fx.shell.Shell.caps(macroField(m.text(), -1));   // macro text is sent in upper case
            textF.focusedProperty().addListener((o, was, now) -> { if (now) lastMacroText[0] = textF; });   // where a clicked tag is inserted
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
        // the tags: every setting on the Station / Rig / Rotor / Amplifier / Antenna pages, with what it is worth right now
        Label hintLbl = lbl("Click a tag to put it into the macro text box you are editing. {CALL} is the station you are working.", "cfg-lh");
        hintLbl.setWrapText(true); hintLbl.setMaxWidth(Double.MAX_VALUE);
        VBox palette = new VBox(10, hintLbl);
        String curGroup = null; javafx.scene.layout.FlowPane flow = null;
        for (com.ars.fx.data.MacroConfig.Tag g : com.ars.fx.data.MacroConfig.TAGS) {
            if (!g.group().equals(curGroup)) {
                curGroup = g.group();
                flow = new javafx.scene.layout.FlowPane(6, 6);
                palette.getChildren().addAll(lbl(curGroup, "cfg-lh"), flow);
            }
            String v = com.ars.fx.data.MacroConfig.value(g);
            Label chip = lbl(g.tag() + (v.isEmpty() ? "  ⚠ not set" : "  " + v.toUpperCase()), "jl-clearbtn");
            chip.setStyle("-fx-cursor:hand;-fx-padding:3 8;-fx-background-color:-ars-surface-2;-fx-background-radius:6;"
                    + (v.isEmpty() ? "-fx-text-fill:-ars-warn;" : ""));
            com.ars.fx.shell.Tips.tip(chip, g.tag() + " — " + g.desc() + "\n" + (v.isEmpty() ? "Not set yet: a macro using it will not be sent until you fill it in." : "Currently: " + v.toUpperCase()));
            chip.setOnMouseClicked(e -> {
                TextField tf = lastMacroText[0];
                if (tf == null) { hintLbl.setText("Click into a macro's text box first, then click the tag."); return; }
                tf.insertText(tf.getCaretPosition(), g.tag()); tf.requestFocus();
            });
            flow.getChildren().add(chip);
        }
        VBox tagsCard = new VBox(palette); tagsCard.getStyleClass().add("cfg-card"); tagsCard.setPadding(new Insets(12, 16, 12, 16));
        Label rebuild = cfgBtn("Rebuild from station data");
        rebuild.setOnMouseClicked(e -> Confirm.show(rebuild, "Rebuild the macros?",
                "Puts the standard macro texts back on F1–F8, written with {MYCALL}, {CALL} and the other fields so they use your Station data. "
                        + "Your voice-file choices are kept; any text you edited is replaced.",
                new Confirm.Choice("Cancel", false, null),
                new Confirm.Choice("Rebuild", true, () -> { com.ars.fx.data.MacroConfig.resetToDefaults(); Shell.navigate("setup:tx"); })));
        HBox tools = new HBox(14, new Region(), rebuild); tools.setAlignment(Pos.CENTER_RIGHT); HBox.setHgrow(tools.getChildren().get(0), Priority.ALWAYS);
        Label macroDesc = lbl("Shared with J-Log. CW/RTTY/digital modes send the text message; SSB and other phone modes play the voice file. "
                + "A field is filled from J-Hub ▸ Station (and Rig control) when the macro is sent — a macro that needs one you have not filled in is not sent.", "cfg-lh");
        macroDesc.setWrapText(true); macroDesc.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, lbl("STATION MACROS · F1–F8", "cfg-sec"), macroDesc, tools, card,
                lbl("MACRO TAGS", "cfg-sec"), tagsCard);
    }
    private static TextField macroField(String val, double w) {
        TextField t = new TextField(val == null ? "" : val);
        t.getStyleClass().add("jhub-mini-field");
        if (w > 0) { t.setMinWidth(w); t.setMaxWidth(w); } else { HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE); }
        return t;
    }
    private static Region spacer(double w) { Region r = new Region(); r.setMinWidth(w); r.setMaxWidth(w); return r; }
    private static Node[] presetRows() {
        java.util.List<com.ars.fx.data.RotorClient.Preset> ps = com.ars.fx.data.RotorClient.PRESETS;
        Node[] rows = new Node[ps.size()];
        for (int i = 0; i < rows.length; i++)
            rows[i] = cfgRow(ps.get(i).name(), "shown as " + ps.get(i).shortName() + " on the rotor panel", cUnit("rotor.preset." + ps.get(i).key(), String.valueOf(ps.get(i).defaultAz()), "° az"));
        return rows;
    }

    static VBox statCell(String k, String val, String unit, boolean ok) {
        Label v = lbl(val, "cfg-sv"); if (ok) v.getStyleClass().add("ok");
        HBox vu = new HBox(4, v); vu.setAlignment(Pos.BOTTOM_LEFT); if (!unit.isEmpty()) vu.getChildren().add(lbl(unit, "cfg-sval-u"));
        VBox cell = new VBox(4, lbl(k, "cfg-sk"), vu); cell.setMinWidth(150); return cell;
    }
    static VBox section(String name, Node... rows) {
        VBox card = new VBox(rows); card.getStyleClass().add("cfg-card");
        if (rows.length > 0) rows[rows.length-1].setStyle("-fx-border-width:0;");
        return new VBox(6, lbl(name, "cfg-sec"), card);
    }
    static HBox cfgRow(String title, String hint, Node control) {
        Object tag = control.getProperties().get("macroTag");
        if (tag != null) hint = (hint == null ? "" : hint + "  ·  ") + "macro tag " + tag;      // every setting a macro can use says so
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
        return tagged(t, key);
    }
    /** Marks a settings control with the macro tag its value is available as: the row shows it, and the control's
     *  tooltip names it. A no-op for settings that have no tag. */
    private static <T extends Node> T tagged(T n, String key) {
        com.ars.fx.data.MacroConfig.Tag g = com.ars.fx.data.MacroConfig.tagForKey(key);
        if (g == null) return n;
        n.getProperties().put("macroTag", g.tag());
        Object prev = n.getProperties().get("tip");
        com.ars.fx.shell.Tips.tip(n, (prev instanceof String ps && !ps.isBlank() ? ps + "\n" : "") + "Macro tag " + g.tag() + " — put it in a macro to send this value");
        return n;
    }
    /** Free-text field bound to a HubConfig key whose example is shown as grey prompt text — the field is genuinely
     *  empty until the operator types, so what is displayed is what is stored (and what macros will send). */
    private static TextField cInputPrompt(String key, String example) {
        TextField t = new TextField(com.ars.fx.data.HubConfig.get(key, "")); t.getStyleClass().add("jhub-mini-field");
        t.setPromptText(example);
        t.setMinWidth(220); t.setMaxWidth(260); t.setAlignment(Pos.CENTER_RIGHT);
        t.textProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set(key, b));
        return tagged(t, key);
    }
    /** Free-text field bound to a HubConfig key that also runs {@code apply} on commit (focus-lost / Enter). */
    private static TextField cInputApply(String key, String def, Runnable apply) {
        TextField t = cInput(key, def);
        t.focusedProperty().addListener((o, was, now) -> { if (!now) apply.run(); });
        t.setOnAction(e -> apply.run());
        return tagged(t, key);
    }
    /** Numeric/short field + unit, bound to a HubConfig key. */
    private static Region cUnit(String key, String def, String unit) {
        TextField t = new TextField(com.ars.fx.data.HubConfig.get(key, def)); t.getStyleClass().add("jhub-mini-field");
        t.setMinWidth(120); t.setMaxWidth(120); t.setAlignment(Pos.CENTER_RIGHT);
        t.textProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set(key, b));
        HBox h = new HBox(8, t, lbl(unit, "cfg-sval-u")); h.setAlignment(Pos.CENTER_LEFT); return tagged(h, key);
    }
    /** Dropdown bound to a HubConfig key (current value injected if not in the list). */
    private static Region cSelect(String key, String def, String... opts) {
        ComboBox<String> c = new ComboBox<>(); c.getItems().addAll(opts);
        String cur = com.ars.fx.data.HubConfig.get(key, def);
        if (!c.getItems().contains(cur)) c.getItems().add(0, cur);
        c.setValue(cur); c.setMinWidth(220); c.setMaxWidth(260);
        c.valueProperty().addListener((o, a, b) -> com.ars.fx.data.HubConfig.set(key, b));
        return tagged(c, key);
    }
    /** Pick list of DX clusters. Choosing one saves it and moves the single cluster connection there. */
    private static ComboBox<String> clusterPicker(java.util.function.Supplier<String> login) {
        ComboBox<String> c = new ComboBox<>(); c.setEditable(true);
        for (com.ars.fx.data.ClusterServers.Server s : com.ars.fx.data.ClusterServers.ALL) c.getItems().add(s.label());
        String cur = com.ars.fx.data.ClusterServers.label(com.ars.fx.data.HubConfig.get("data.clusterServer", com.ars.fx.data.ClusterServers.DEFAULT_ADDRESS).trim());
        if (!c.getItems().contains(cur)) c.getItems().add(0, cur);
        c.setValue(cur); c.setMinWidth(220);
        com.ars.fx.shell.Tips.tip(c, "Choose a DX cluster, or type host:port and press Enter");
        c.valueProperty().addListener((o, a, b) -> {
            String address = com.ars.fx.data.ClusterServers.addressOf(b);
            if (address.isEmpty() || address.equals(com.ars.fx.data.ClusterServers.addressOf(a))) return;
            com.ars.fx.data.HubConfig.set("data.clusterServer", address);
            ClusterClient.getInstance().setEndpoint(com.ars.fx.data.ClusterServers.host(address), com.ars.fx.data.ClusterServers.port(address), login.get());
        });
        return c;
    }
    /** Segmented control bound to a HubConfig key (stores the option string). */
    private static Region cSeg(String key, String[] opts, int def) {
        String cur = com.ars.fx.data.HubConfig.get(key, opts[def]);
        int on = 0; for (int i = 0; i < opts.length; i++) if (opts[i].equals(cur)) on = i;
        return tagged(segSel(opts, on, i -> com.ars.fx.data.HubConfig.set(key, opts[i])), key);
    }
    /** Segmented control bound to a HubConfig key that also runs {@code apply} after each change. */
    private static Region cSegApply(String key, String[] opts, int def, Runnable apply) {
        String cur = com.ars.fx.data.HubConfig.get(key, opts[def]);
        int on = 0; for (int i = 0; i < opts.length; i++) if (opts[i].equals(cur)) on = i;
        return tagged(segSel(opts, on, i -> { com.ars.fx.data.HubConfig.set(key, opts[i]); apply.run(); }), key);
    }
    /** Toggle bound to a HubConfig boolean key. */
    private static Region cToggle(String key, boolean def) {
        return tagged(switchToggle(com.ars.fx.data.HubConfig.getBool(key, def), v -> com.ars.fx.data.HubConfig.setBool(key, v)), key);
    }
    /** Toggle bound to a HubConfig boolean key that also runs {@code apply} with the new state. */
    private static Region cToggleApply(String key, boolean def, java.util.function.Consumer<Boolean> apply) {
        return tagged(switchToggle(com.ars.fx.data.HubConfig.getBool(key, def), v -> { com.ars.fx.data.HubConfig.setBool(key, v); apply.accept(v); }), key);
    }

    // ---- small helpers -----------------------------------------------------
    private static Region chip(String cls, double sz){ Region r = new Region(); r.getStyleClass().add(cls); r.setMinSize(sz,sz); r.setPrefSize(sz,sz); r.setMaxSize(sz,sz); return r; }
    private static Label btn(String t, String... c){ Label l = lbl(t, c); l.setAlignment(Pos.CENTER); return l; }
    private static VBox labeled(String label, Region inp){ VBox v = new VBox(5, lbl(label,"jl-flabel"), inp); v.setAlignment(Pos.TOP_LEFT); return v; }
}
