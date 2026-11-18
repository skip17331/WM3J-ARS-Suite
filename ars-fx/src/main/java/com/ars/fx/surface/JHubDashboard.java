package com.ars.fx.surface;

import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.SpotNeeds;
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
import static com.ars.fx.surface.JHubSetupPages.*;

/** J-Hub unified workspace — cockpit dashboard (handoff shot 01), mock data. */
public final class JHubDashboard {
    private JHubDashboard() {}

    static final long START = System.currentTimeMillis();
    static Region dashRoot;
    static boolean dashTickerStarted;

    // the LOG TOOLS tray lives in threeCol(), beside the cards, so the hero and the insight strip run the full width
    public static Region build()        { Region center = dashboardCenter(); dashRoot = page("dashboard", center, null, false); com.ars.fx.UiStyle.applyDashboard(dashRoot); startDashTicker(); return dashRoot; }

    static Region page(String activeConf, Region centerContent) { return page(activeConf, centerContent, null); }
    /** @param leftPanel optional collapsing column placed right of the dock (the dashboard's log tools) */
    static Region page(String activeConf, Region centerContent, Region leftPanel) { return page(activeConf, centerContent, leftPanel, true); }
    /** @param scrolls false when {@code centerContent} brings its own scrolling (the dashboard keeps its header fixed and scrolls only the cards) */
    static Region page(String activeConf, Region centerContent, Region leftPanel, boolean scrolls) {
        // Same icon dock as every operating module; the J-Hub config links ride along as an
        // expand-only tail so they stay reachable without changing the collapsed rail. In a solo
        // window there is no dock, so the same links become a fixed left nav column instead.
        Region nav = Shell.solo ? soloNav(activeConf) : Shell.dock("hub", hubConfigSection(activeConf));

        Region centre;
        if (scrolls) {
            ScrollPane sp = new ScrollPane(centerContent); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setStyle("-fx-background-color:-ars-bg;"); centre = sp;
        } else centre = centerContent;
        centre.setMinWidth(0); HBox.setHgrow(centre, Priority.ALWAYS);
        HBox row = leftPanel == null ? new HBox(nav, centre) : new HBox(nav, leftPanel, centre); row.setFillHeight(true); VBox.setVgrow(row, Priority.ALWAYS);
        row.setMinHeight(0);
        VBox root = new VBox(workspaceBar(), row); root.getStyleClass().add("jhub-page"); root.setMinHeight(0);   // background from the stylesheet, so a style pack can replace it
        if (com.ars.fx.UiStyle.isModern()) {          // space age: the page sits over a starfield
            root.getStyleClass().add("jhub-page-clear");
            StackPane sky = new StackPane(new Starfield(), root); sky.setMinHeight(0);
            return sky;
        }
        return root;
    }

    /** Solo settings nav: the dock's config links as a static left column, topped by a link back to the
     *  operating module this window hosts (there is no dock to navigate back through). */
    static Region soloNav(String activeConf) {
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

    /** The hero banner and the insight strip stay fixed at the top; only the cards underneath scroll. */
    static Region dashboardCenter() {
        insightStrip = new InsightStrip(com.ars.fx.UiStyle.isInstrument());     // every look has the collapsible, pinnable strip; Instrument shows gauges when it is open
        HBox cards = threeCol();                       // after the strip exists: the cluster card feeds it on its first render
        VBox cardsBox = new VBox(14, cards); cardsBox.setPadding(new Insets(12, 24, 24, 24));
        if (fullWidthLog != null) { VBox.setVgrow(cards, Priority.NEVER); cardsBox.getChildren().add(fullWidthLog); }   // the log spans the whole width under the cards
        ScrollPane sp = new ScrollPane(cardsBox); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().add("dash-scroll"); VBox.setVgrow(sp, Priority.ALWAYS); sp.setMinHeight(0);
        // the entry form takes keyboard focus as the page opens and the scroll pane nudges the cards up to show it, which hid the card headers
        // under the fixed strip; start from the top (a later scroll by the user is left alone)
        sp.sceneProperty().addListener((o, was, sc) -> { if (sc != null) javafx.application.Platform.runLater(() -> javafx.application.Platform.runLater(() -> sp.setVvalue(0))); });

        VBox head = new VBox(14, header(), insightStrip);
        head.setPadding(new Insets(10, 24, 0, 24));
        head.setMinHeight(Region.USE_PREF_SIZE);       // the scroll area takes what is left; the header is never squeezed to make room for it
        head.setViewOrder(-1);                         // the strip floats open over the cards, so the whole header must draw above them

        VBox center = new VBox(head, sp);
        center.getStyleClass().add("dash-center"); center.setMinHeight(0);
        return center;
    }

    // ---- top workspace bar -------------------------------------------------
    static String uptime() {
        long m = (System.currentTimeMillis() - START) / 60000;
        return m < 60 ? m + "m" : (m / 60) + "h " + (m % 60) + "m";
    }
    /** The status bar's right-hand readout: uptime, or in the space age style the mission elapsed time. */
    static String barUptime() {
        if (!com.ars.fx.UiStyle.isModern()) return "ARS Suite · up " + uptime();
        long s = (System.currentTimeMillis() - START) / 1000;
        return String.format("MET  T+ %02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
    }
    /** A thin orbit line with a glowing dot that circles along it; the dot sits still when Reduce effects is on. */
    static Region orbitTrack() {
        Pane track = new Pane(); track.setMinHeight(30); track.setPrefHeight(30); track.setMouseTransparent(true);
        HBox.setHgrow(track, Priority.ALWAYS); track.setMinWidth(0);
        Region line = new Region(); line.setPrefHeight(1); line.setMinHeight(1); line.setMaxHeight(1);
        line.setStyle("-fx-background-color: linear-gradient(to right, transparent, -ars-accent-line 50%, transparent);");   // scheme colours, so it follows the theme button
        Region dot = new Region(); dot.setPrefSize(7, 7); dot.setMinSize(7, 7); dot.setMaxSize(7, 7);
        dot.setStyle("-fx-background-color: -ars-t1; -fx-background-radius: 99; -fx-effect: dropshadow(gaussian, -ars-accent, 10, 0.6, 0, 0);");
        track.getChildren().addAll(line, dot);
        javafx.beans.property.SimpleDoubleProperty phase = new javafx.beans.property.SimpleDoubleProperty(0.5);
        Runnable place = () -> {
            double w = track.getWidth(), h = track.getHeight();
            line.resizeRelocate(0, h / 2, w, 1);
            dot.relocate(Math.max(0, w - 7) * phase.get(), h / 2 - 3);
        };
        track.widthProperty().addListener((o, a, b) -> place.run());
        track.heightProperty().addListener((o, a, b) -> place.run());
        phase.addListener((o, a, b) -> place.run());
        Timeline orbit = new Timeline(new KeyFrame(javafx.util.Duration.ZERO, new javafx.animation.KeyValue(phase, 0.0)),
                new KeyFrame(javafx.util.Duration.seconds(16), new javafx.animation.KeyValue(phase, 1.0, javafx.animation.Interpolator.LINEAR)));
        orbit.setCycleCount(Timeline.INDEFINITE);
        track.sceneProperty().addListener((o, was, sc) -> { if (sc == null) orbit.stop(); else if (!com.ars.fx.shell.Fx.reduced()) orbit.play(); });
        return track;
    }
    static String nowUtc() { return java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")); }
    static void startDashTicker() {
        if (dashTickerStarted) return;
        dashTickerStarted = true;
        com.ars.fx.data.RigClient.getInstance().addListener(st -> {      // the hero's rig readout follows the rig, not the one-second tick
            if (dashRoot != null && dashRoot.lookup(".hero-rig") instanceof Label f && dashRoot.lookup(".hero-rig-sub") instanceof Label m) heroRig(f, m);
        });
        com.ars.fx.shell.Fx.everySecond(() -> {
            if (dashRoot == null) return;
            heroDevicesTick.run();
            if (dashRoot.lookup(".sx-clock-v") instanceof Label l) l.setText(nowUtc());
            if (dashRoot.lookup(".jhub-uptime") instanceof Label l) l.setText(barUptime());
            if (dashRoot.lookup(".hero-rig") instanceof Label f && dashRoot.lookup(".hero-rig-sub") instanceof Label m) heroRig(f, m);
        });
    }
    static String brandCallText() { String c = com.ars.fx.data.HubConfig.call(); return c.isEmpty() ? "" : "· " + c.toUpperCase(); }
    static Region workspaceBar() {
        Region dot = chip("jhub-bar-dot", 9);
        Label brandCall = lbl(brandCallText(), "jhub-bar-sub");
        com.ars.fx.data.HubConfig.addChangeListener(k -> { if ("station.call".equals(k) || "*".equals(k)) javafx.application.Platform.runLater(() -> brandCall.setText(brandCallText())); });
        HBox brand = new HBox(8, dot, lbl("Dashboard", "jhub-bar-nm"), brandCall);
        brand.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        if (com.ars.fx.UiStyle.isModern()) sp = orbitTrack();      // space age: a satellite crawls along a thin orbit line between the two ends
        HBox bar = new HBox(brand, sp, lbl(barUptime(), "jhub-bar-sub", "jhub-uptime"));
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
    static Region confItem(String icon, String name, String caret, boolean active) {
        Label ic = lbl(icon, "jhub-nav-cic");
        Label nm = lbl(name, "jhub-nav-cnm"); HBox.setHgrow(nm, Priority.ALWAYS); nm.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(11, ic, nm); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("jhub-nav-conf");
        if (caret != null) row.getChildren().add(lbl(caret, "jhub-nav-caret"));
        if (active) row.getStyleClass().add("active");
        VBox.setMargin(row, new Insets(1, 9, 1, 9));
        return row;
    }

    // ---- center header -----------------------------------------------------
    static Region header() { return heroHeader(); }

    /** The dashboard header, in every style: a hero banner. Station identity, the rig's live frequency, the cluster feed and the UTC clock.
     *  Its clock keeps the sx-clock-v class and its rig readout the hero-rig class, so the dashboard's one-second ticker keeps both current. */
    static Region heroHeader() {
        String call = com.ars.fx.data.HubConfig.call().toUpperCase();
        java.util.List<String> bits = new java.util.ArrayList<>();
        String nm = com.ars.fx.data.HubConfig.get("station.name", "").trim(), qth = com.ars.fx.data.HubConfig.get("station.qth", "").trim();
        String grid = com.ars.fx.data.HubConfig.grid().toUpperCase();
        if (!nm.isEmpty()) bits.add(nm); if (!qth.isEmpty()) bits.add(qth); if (!grid.isEmpty()) bits.add(grid);
        VBox id = new VBox(1, lbl("STATION", "hero-k"), lbl(call.isEmpty() ? "SET CALLSIGN" : call, "hero-call"),
                lbl(bits.isEmpty() ? "Setup › Station" : String.join("  ·  ", bits), "hero-sub"));

        Label rigF = lbl("—", "hero-rig"), rigM = lbl("rig offline", "hero-rig-sub");
        heroRig(rigF, rigM);
        VBox rig = new VBox(1, lbl("RIG", "hero-k"), rigF, rigM);

        ClusterClient cc = ClusterClient.getInstance();
        Region cdot = chip("jhub-mini-dot", 9); com.ars.fx.shell.FeedUi.paint(cdot, cc.status());
        Label cluTxt = lbl(cc.status().label() + " · " + cc.spots().size() + " spots", "hero-sub");
        headerDot = cdot; headerTxt = cluTxt;      // kept live by the cluster card's render
        HBox cluRow = new HBox(7, cdot, cluTxt); cluRow.setAlignment(Pos.CENTER_LEFT);
        VBox clu = new VBox(5, lbl("DX CLUSTER", "hero-k"), cluRow);

        VBox clock = new VBox(1, lbl("UTC", "hero-k"), lbl(nowUtc(), "sx-clock-v", "hero-clk"),
                lbl(java.time.LocalDate.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM yyyy")), "hero-sub"));
        clock.setAlignment(Pos.TOP_RIGHT);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label idTimer = com.ars.fx.shell.IdTimer.button();
        com.ars.fx.shell.HeroDevices.Set devices = com.ars.fx.shell.HeroDevices.build("hero-rig");   // rotor / satellite rotor / amp, if switched on in Setup
        heroDevicesTick = devices.update(); heroDevicesTick.run();
        HBox row = new HBox(40, id);
        if (com.ars.fx.shell.HeroDevices.showRig()) row.getChildren().add(rig);
        row.getChildren().addAll(devices.cells());
        row.getChildren().addAll(sp, clu, idTimer, clock); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("hero");
        return row;
    }
    static void heroRig(Label f, Label m) {
        com.ars.fx.data.RigClient.State st = com.ars.fx.data.RigClient.getInstance().last();
        boolean on = st != null && st.connected() && st.freqHz() > 0;
        f.setText(on ? com.ars.fx.data.RigClient.fmtFreq(st.freqHz()) : "—");
        String mode = on && st.mode() != null ? st.mode().trim() : "";
        m.setText(on ? (mode.isEmpty() ? "MHz" : "MHz  ·  " + mode) : "rig offline");
    }

    // the insight strip under the hero, in every look (live stats + band activity, collapsible and pinnable): see InsightStrip
    static InsightStrip insightStrip;
    /** Brings the hero banner's extra device readouts (rotor, amp) up to date; run by the dashboard's one-second ticker. */
    static Runnable heroDevicesTick = () -> {};

    // ---- 3-column area -----------------------------------------------------
    static HBox threeColRef = new HBox();
    /** The Recent log, lifted out of the card column so it can run the full width of the dashboard. */
    static VBox fullWidthLog;
    static HBox threeCol() {
        fullWidthLog = null;
        // the full normal-log entry form + recent log (same as J-Log) beside the DX cluster
        VBox entry = JLogCockpit.embeddedEntry();
        boolean clusterRight = clusterInRail();
        VBox left;
        Region rail;
        if (clusterRight) {
            // Setup > Appearance: the DX Cluster and its controls head the PANELS column, and the Recent log stays under the New QSO form
            // (the entry column as J-Log builds it), in the place the cluster had
            left = new VBox(11, entry);
            rail = railCol(clusterCol(true));
        } else {
            WindowsRow windows = new WindowsRow(entry, clusterCol());   // side by side when there is room, stacked when there is not
            left = new VBox(11, windows);
            rail = railCol(null);
        }
        HBox.setHgrow(left, Priority.ALWAYS);
        // LOG TOOLS (left tray) starts at the same top and height as PANELS (right tray); sideDrawers() has to come after embeddedEntry()
        Region logTools = Shell.collapsibleLeftPanel("jlog", "LOG TOOLS", 322, JLogCockpit.sideDrawers());
        HBox box = new HBox(14, logTools, left, rail);
        box.setFillHeight(true); VBox.setVgrow(box, Priority.ALWAYS);
        threeColRef = box;
        return box;
    }
    /**
     * The New QSO form and the DX Cluster window. The form's fields do not shrink: it needs its computed minimum width (about 685 px) for every field and the
     * Log QSO button to show, and likes 798 (760 plus 1 cm). Both used to be forced lower than that (a minimum of 560), so when the row got narrow, as it does with the
     * PANELS column pinned, the form was squeezed below what its fields need and the cluster window lay over the part that spilled out. Now, when the row is
     * too narrow for the form's minimum plus a cluster window of at least 300 px, the cluster drops below the form instead (300, not more, so a maximised
     * 1920 px window still has them side by side with the PANELS column pinned; the cluster header gives up its server text first); with room, the form takes its
     * natural width (never less than it needs) and the cluster takes the rest, as before. (All sizes are logical pixels: a 125% display shows 1.25 times these.)
     */
    static final class WindowsRow extends Region {
        private static final double FORM_PREF = 798, GAP = 20, CLUSTER_MIN = 300, STACKED_CLUSTER_H = 460;
        private final Region entry, cluster;
        /** Cards in rows (the default; Setup > Appearance): New QSO, then DX Cluster, then the Recent log, each its own row. Off = side by side
         *  when there is room. */
        private final boolean rows = com.ars.fx.data.HubConfig.getBool("ui.dashboard.rows", true);
        /** Rows mode: what sits under the New QSO card in the entry column (the Recent log), lifted out so the cluster can go between them. */
        private final VBox rest;
        WindowsRow(Region entry, Region cluster) {
            this.entry = entry; this.cluster = cluster;
            VBox r = null;
            if (entry instanceof VBox col && col.getChildren().size() > 1) {
                java.util.List<Node> tail = new java.util.ArrayList<>(col.getChildren().subList(1, col.getChildren().size()));
                col.getChildren().removeAll(tail);
                r = new VBox(tail.toArray(new Node[0]));
            }
            fullWidthLog = r;                  // the Recent log goes under the whole row (tray, cards and rail), see dashboardCenter()
            rest = null;
            getChildren().addAll(entry, cluster);
            if (rest != null) getChildren().add(rest);
            setMinSize(0, 0);
        }
        /** What the form needs for everything on it to show. */
        private double need() { return entry.minWidth(-1); }
        /** What it takes when there is room. */
        private double liked() { return Math.max(need(), FORM_PREF); }
        private boolean stacked(double w) { return rows || w < need() + GAP + CLUSTER_MIN; }
        private double entryWidth(double w) {
            if (rows) return w;                                  // a row: the New QSO card takes the whole width
            return stacked(w) ? Math.max(need(), Math.min(liked(), w)) : Math.max(need(), Math.min(liked(), w - GAP - CLUSTER_MIN));
        }
        @Override protected double computeMinWidth(double h) { return 0; }
        @Override protected double computePrefWidth(double h) { return liked() + GAP + CLUSTER_MIN; }
        @Override protected double computePrefHeight(double w) {
            double width = w > 0 ? w : getWidth() > 0 ? getWidth() : liked() + GAP + CLUSTER_MIN;
            double ew = entryWidth(width), eh = entry.prefHeight(ew);
            // side by side the cluster is as tall as the New QSO card (see formHeight) and its spot list scrolls inside it; it used to ask for
            // its content height, so a long spot list stretched the whole row far past the QSO pane
            if (stacked(width)) return eh + GAP + STACKED_CLUSTER_H + (rest != null ? GAP + rest.prefHeight(width) : 0);
            return eh;
        }
        @Override protected void layoutChildren() {
            double w = getWidth(), h = getHeight(), ew = entryWidth(w);
            if (stacked(w)) {
                double eh = entry.prefHeight(ew);
                entry.resizeRelocate(0, 0, ew, eh);
                double ch = rest != null ? STACKED_CLUSTER_H : Math.max(STACKED_CLUSTER_H, h - eh - GAP);
                cluster.resizeRelocate(0, eh + GAP, w, ch);
                if (rest != null) rest.resizeRelocate(0, eh + GAP + ch + GAP, w, rest.prefHeight(w));
            } else {
                entry.resizeRelocate(0, 0, ew, h);
                cluster.resizeRelocate(ew + GAP, 0, w - ew - GAP, formHeight(ew, h));
            }
        }
        /** The height of the New QSO card alone: the entry column also carries the Recent log under it, which the cluster must not follow. */
        private double formHeight(double ew, double fallback) {
            if (entry instanceof javafx.scene.layout.Pane p && !p.getChildren().isEmpty() && p.getChildren().get(0) instanceof Region card)
                return Math.min(fallback, card.prefHeight(ew));
            return fallback;
        }
    }

    /** Mini "Saved QSOs" drawer under the New-QSO / DX-cluster windows — from the real ~/.j-log DB. */
        // ---- DX cluster filter state (shared by panel + controls drawer) -------
    static final java.util.Set<String> HF_BANDS = java.util.Set.of(
            "160m","80m","60m","40m","30m","20m","17m","15m","12m","10m");
    static final String[] CLU_BANDS = {"All","HF","VHF+"}, CLU_MODES = {"All","CW","Phone","Digi"};
    /** The cluster window's band / mode filter is the "Band filter" / "Mode filter" setting on J-Hub ▸ Data, so what you
     *  pick here is what it starts with next time (and the Data page shows). 0 = All. */
    static int cluSel(String key, String[] opts) {
        String cur = com.ars.fx.data.HubConfig.get(key, opts[0]);
        for (int i = 0; i < opts.length; i++) if (opts[i].equals(cur)) return i;
        return 0;
    }
    static boolean cluNeededOnly = false;
    static SpotNeeds.Index needCache = new SpotNeeds.Index();
    static long needAt = 0;
    /** The log's worked slots as an index for the NEED flags; rebuilt every few seconds, not per spot. */
    static SpotNeeds.Index needIndex() {
        long now = System.currentTimeMillis();
        if (now - needAt > 5000) { needCache = SpotNeeds.build(JLogDb.workedSlots(), SpotNeeds.resolver()); needAt = now; }
        return needCache;
    }
    static boolean bandOk(ClusterClient.Spot s) {
        int cluBandSel = cluSel("data.clusterBand", CLU_BANDS);
        if (cluBandSel == 0) return true;
        boolean hf = HF_BANDS.contains(s.band());
        return cluBandSel == 1 ? hf : !hf;
    }
    static boolean modeOk(ClusterClient.Spot s) {
        switch (cluSel("data.clusterMode", CLU_MODES)) {
            case 1: return "CW".equals(s.mode());
            case 2: return "SSB".equals(s.mode());
            case 3: return java.util.Set.of("FT8","FT4","RTTY","PSK31","JS8","DIGI").contains(s.mode());
            default: return true;
        }
    }
    /** Set by the contest page: what to tag a spot with while it is on screen (empty = no tag). */
    static volatile java.util.function.Function<ClusterClient.Spot, String> contestSpotTag;

    /** The tag on a spot: in contest mode the contest's own (NEW MULT), otherwise the new-entity / band / mode flag. */
    static String tag(ClusterClient.Spot s, SpotNeeds.Index ix) {
        java.util.function.Function<ClusterClient.Spot, String> h = contestSpotTag;
        if (h != null && "logc".equals(Shell.currentSurface)) return h.apply(s);
        return need(s, ix).label;
    }

    /** The spots as band-map marks: needed (tagged), already worked (this call on this band; in a contest, in this contest's log), or ordinary. */
    static java.util.List<com.ars.fx.data.BandMapModel.Mark> bandMarks(java.util.List<ClusterClient.Spot> spots, SpotNeeds.Index ix) {
        var plugin = com.ars.fx.data.ContestState.plugin();
        boolean contest = "logc".equals(Shell.currentSurface) && plugin != null;
        boolean wide = contest && plugin.isContestWideDupe();
        java.util.Set<String> done = contest ? contestWorked() : java.util.Set.of();
        java.util.List<com.ars.fx.data.BandMapModel.Mark> out = new java.util.ArrayList<>(spots.size());
        for (ClusterClient.Spot s : spots) {
            com.ars.fx.data.BandMapModel.Kind k;
            if (!tag(s, ix).isEmpty()) k = com.ars.fx.data.BandMapModel.Kind.NEEDED;
            else if (contest ? done.contains(wide ? SpotNeeds.up(s.call()) : SpotNeeds.up(s.call()) + "|" + SpotNeeds.up(s.band())) : ix.worked(s.call(), s.band()))
                k = com.ars.fx.data.BandMapModel.Kind.WORKED;
            else k = com.ars.fx.data.BandMapModel.Kind.NORMAL;
            boolean skimmer = s.de() != null && s.de().toUpperCase(java.util.Locale.ROOT).endsWith("-SKM");     // the local CW skimmer's spots
            out.add(new com.ars.fx.data.BandMapModel.Mark(s.call(), s.freqHz(), s.mode(), s.band(), s.arrivalMs(), k, skimmer));
        }
        return out;
    }
    static java.util.Set<String> cwSet = java.util.Set.of();
    static int cwCount = -1; private static String cwContest = "";
    /** The calls (and call + band) in the open contest's log, rebuilt only when the log has changed size. */
    static java.util.Set<String> contestWorked() {
        java.util.List<com.jlog.model.QsoRecord> q = com.ars.fx.data.ContestState.qsos();
        String id = com.ars.fx.data.ContestState.plugin().getContestId();
        if (q.size() != cwCount || !id.equals(cwContest)) {
            java.util.Set<String> s = new java.util.HashSet<>();
            for (com.jlog.model.QsoRecord r : q) { String c = SpotNeeds.up(r.getCallsign()); s.add(c); s.add(c + "|" + SpotNeeds.up(r.getBand())); }
            cwSet = s; cwCount = q.size(); cwContest = id;
        }
        return cwSet;
    }

    /** Setup: the Club Log token (kept with the upload logins), Update now, and what the list holds. */
    static Region mostWantedControls() {
        javafx.scene.control.PasswordField tok = new javafx.scene.control.PasswordField();
        tok.setText(com.ars.fx.data.MostWanted.token()); tok.setPromptText("cl_..."); tok.getStyleClass().add("jhub-mini-field"); tok.setPrefWidth(170);
        JHubSetupPages.saveWhenSettled(tok, () -> { String v = tok.getText(); return () -> com.ars.fx.data.MostWanted.setToken(v); });
        int n = com.ars.fx.data.MostWanted.size();
        Label st = lbl(n > 0 ? n + " entities ranked" : "no list yet", "jhub-conn");
        Label upd = lbl("Update now", "jhub-clearbtn"); upd.setStyle("-fx-cursor:hand;");
        upd.setOnMouseClicked(e -> {
            st.setText("⋯ asking Club Log…");
            JHubSetupPages.flushSaves();
            Thread t = new Thread(() -> { JHubSetupPages.waitForSaves(); String m = com.ars.fx.data.MostWanted.refresh(); javafx.application.Platform.runLater(() -> st.setText(m)); }, "most-wanted");
            t.setDaemon(true); t.start();
        });
        HBox h = new HBox(8, tok, upd, st); h.setAlignment(Pos.CENTER_RIGHT);
        return h;
    }

    /** Setup: Update now for Club Log's DXCC exceptions, and how many calls the file holds. */
    static Region clubLogCtyControls() {
        int n = com.ars.fx.data.ClubLogCty.size();
        Label st = lbl(n > 0 ? n + " calls" : com.ars.fx.data.ClubLogCty.apiKey().isEmpty() ? "no API key set" : "no file yet", "jhub-conn");
        Label upd = lbl("Update now", "jhub-clearbtn"); upd.setStyle("-fx-cursor:hand;");
        upd.setOnMouseClicked(e -> {
            st.setText("⋯ asking Club Log…");
            Thread t = new Thread(() -> { String m = com.ars.fx.data.ClubLogCty.refresh(); javafx.application.Platform.runLater(() -> st.setText(m)); }, "clublog-cty");
            t.setDaemon(true); t.start();
        });
        HBox h = new HBox(8, upd, st); h.setAlignment(Pos.CENTER_RIGHT);
        return h;
    }

    /** The tag a spot shows in the spot list ("NEW ONE", "NEW BAND", a contest's NEW MULT ..., or "" when not needed), for spot alerts. */
    static String needLabel(ClusterClient.Spot s) { return tag(s, needIndex()); }

    static SpotNeeds.Need need(ClusterClient.Spot s, SpotNeeds.Index ix) {
        return SpotNeeds.classify(ix, s.call(), s.band(), s.mode(), SpotNeeds.resolver());
    }

    static Region headerDot; private static Label headerTxt; private static Label headActRef;

    /** Middle column: the DX spot list with a functional cluster-controls drawer beneath it. */
    /** The DX Cluster window (spot list + cluster controls). Also used on J-Log, so the two always look and behave alike. */
    static Region clusterCol() { return clusterCol(false); }
    /** The compact cluster card's preferred height in the PANELS column (it gets less when the panels under it need the room, see RailSplit). */
    static final double RAIL_CLUSTER_H = 460;
    /**
     * @param compact true for the 322 px PANELS column: a spot is one line (frequency, call, any NEW tag, mode) with the spotter, comment, band
     *                and age in its tooltip, the card takes the height the panels under it leave, and the controls drawer stacks its band / mode pickers
     */
    static Region clusterCol(boolean compact) {
        ClusterClient cc = ClusterClient.getInstance();

        // ---- DX spot panel ----
        Node ic = Shell.iconTile("hub", "hub", 16, "sx-dw-ic");
        Region hsp = new Region(); HBox.setHgrow(hsp, Priority.ALWAYS);
        Region cdot = chip("jhub-mini-dot", 8); com.ars.fx.shell.FeedUi.paint(cdot, cc.status());
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
        Region fsp = new Region(); HBox.setHgrow(fsp, Priority.ALWAYS); 
        // the spots as a list, or laid out on a band map (same spots, same filters)
        BandMapView map = new BandMapView((node, mk) -> SpotAction.onSpot(node, mk.call(), mk.freqHz(), mk.mode(), mk.band()));
        VBox.setVgrow(map, Priority.ALWAYS);
        boolean[] mapOn = {"map".equals(com.ars.fx.data.HubConfig.get("data.clusterView", "list"))};
        Label viewBtn = lbl(mapOn[0] ? "List" : "Map", "jhub-clu-flt"); viewBtn.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(viewBtn, "Switch between the spot list and the band map");
        Label spotBtn = lbl("+ Spot", "jhub-clu-flt"); spotBtn.setStyle("-fx-cursor:hand;");
        flt.getChildren().addAll(fsp, viewBtn, spotBtn);
        flt.setAlignment(Pos.CENTER_LEFT); flt.setPadding(new Insets(12, 16, 12, 16));
        SpotEntry spot = spotEntry();                                  // opens under the filter line; "+ Spot" shows and hides it
        Region spotRow = spot.view();
        spotRow.setVisible(false); spotRow.setManaged(false);
        com.ars.fx.shell.Tips.tip(spotBtn, "Send a spot to the cluster");
        spotBtn.setOnMouseClicked(e -> {
            boolean show = !spotRow.isVisible();
            spotRow.setVisible(show); spotRow.setManaged(show);
            if (show) spot.onOpen().run();
        });
        VBox fltBox = new VBox(flt, spotRow);

        VBox rows = new VBox();
        Label empty = lbl(com.ars.fx.shell.FeedUi.emptyList(cc, "cluster")); empty.setStyle("-fx-font-size:12px;-fx-text-fill:-ars-t4;-fx-padding:18 16 18 16;"); empty.setWrapText(true);
        rows.getChildren().add(empty);
        ScrollPane rsp = new ScrollPane(rows); rsp.setFitToWidth(true); rsp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); rsp.setStyle("-fx-background-color:transparent;"); VBox.setVgrow(rsp, Priority.ALWAYS);
        VBox card = new VBox(head, fltBox, mapOn[0] ? map : rsp); card.getStyleClass().add("jhub-card"); VBox.setVgrow(card, Priority.ALWAYS);
        if (compact) { card.setMinHeight(200); card.setPrefHeight(RAIL_CLUSTER_H); }   // RailSplit sizes it: whatever the panels under it leave

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
            com.ars.fx.shell.FeedUi.paint(cdot, cc.status());
            cluState.setText(com.ars.fx.shell.FeedUi.line(cc));
            if (headActRef != null) headActRef.setText(com.ars.fx.shell.FeedUi.action(cc));
            if (headerDot != null) com.ars.fx.shell.FeedUi.paint(headerDot, cc.status());
            if (headerTxt != null) headerTxt.setText(cc.status().label() + " · " + cc.spots().size() + " spots");
            SpotNeeds.Index worked = needIndex();
            java.util.List<ClusterClient.Spot> all = cc.spots();
            if (insightStrip != null) {
                int needed = 0; for (ClusterClient.Spot s : all) if (!tag(s, worked).isEmpty()) needed++;
                insightStrip.update(all, needed);
            }
            java.util.List<ClusterClient.Spot> shown = new java.util.ArrayList<>();
            for (ClusterClient.Spot s : all)
                if (bandOk(s) && modeOk(s) && (!cluNeededOnly || !tag(s, worked).isEmpty())) shown.add(s);
            rows.getChildren().clear();
            if (shown.isEmpty()) {
                empty.setText(conn && !all.isEmpty() ? "No spots match the current filter" : com.ars.fx.shell.FeedUi.emptyList(cc, "cluster"));
                rows.getChildren().add(empty);
            } else {
                for (ClusterClient.Spot s : shown) {
                    String[] f = { ClusterClient.fmtFreq(s.freqHz()), s.call(), tag(s, worked), s.mode(), s.band(),
                            s.de(), s.comment(), ClusterClient.age(s.arrivalMs()) };
                    Region row = compact ? cluRowCompact(f) : cluRow(f,
                            com.ars.fx.data.PathNow.shown() ? com.ars.fx.shell.PathChip.of(com.ars.fx.data.PathNow.forSpot(s.call(), s.freqHz())) : null);
                    row.setStyle("-fx-cursor:hand;");
                    int mw = com.ars.fx.data.MostWanted.rank(s.call());
                    String hint = (mw > 0 ? "Club Log Most Wanted #" + mw + "\n" : "")
                            + "Click to use this spot — fills the log entry, tunes the rig, and offers to turn the beam";
                    com.ars.fx.shell.Tips.tip(row, compact ? spotDetails(f) + "\n" + hint : hint);
                    row.setOnMouseClicked(e -> SpotAction.onSpot(row, s.call(), s.freqHz(), s.mode(), s.band()));
                    com.ars.fx.shell.Fx.fresh(row, s.arrivalMs());     // a new spot slides in and glows
                    rows.getChildren().add(row);
                }
            }
            fltSummary.setText(filterSummary() + "  ·  " + shown.size() + (cluNeededOnly ? " needed" : " shown"));
            if (mapOn[0]) map.update(bandMarks(shown, worked));
            boolean want = cc.wantConnected();
            statusLbl.getStyleClass().remove("live");
            statusLbl.setText(com.ars.fx.shell.FeedUi.symbolLine(cc) + (cc.status() == com.ars.fx.data.TelnetFeed.Status.CONNECTING && !cc.detail().isEmpty() ? " — " + cc.detail() : ""));
            if (conn) statusLbl.getStyleClass().add("live");
            act.setText(com.ars.fx.shell.FeedUi.action(cc));
        });

        VBox bandsF = labeled("BANDS", segSel(CLU_BANDS, cluSel("data.clusterBand", CLU_BANDS), i -> { com.ars.fx.data.HubConfig.set("data.clusterBand", CLU_BANDS[i]); render.run(); }));
        VBox modesF = labeled("MODES", segSel(CLU_MODES, cluSel("data.clusterMode", CLU_MODES), i -> { com.ars.fx.data.HubConfig.set("data.clusterMode", CLU_MODES[i]); render.run(); }));
        Region row2;
        if (compact) row2 = new VBox(10, bandsF, modesF);      // side by side they are wider than the PANELS column
        else { HBox h = new HBox(12, bandsF, modesF); h.setAlignment(Pos.BOTTOM_LEFT); row2 = h; }

        Label nkl = lbl("Needed only (new entity / band / mode; new multiplier in a contest)"); nkl.getStyleClass().add("k"); HBox.setHgrow(nkl, Priority.ALWAYS); nkl.setMaxWidth(Double.MAX_VALUE);
        if (compact) nkl.setWrapText(true);
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

        viewBtn.setOnMouseClicked(e -> {
            mapOn[0] = !mapOn[0];
            com.ars.fx.data.HubConfig.set("data.clusterView", mapOn[0] ? "map" : "list");
            viewBtn.setText(mapOn[0] ? "List" : "Map");
            card.getChildren().setAll(head, fltBox, mapOn[0] ? map : rsp);
            render.run();
        });
        cc.setListener(c -> render.run());
        cc.start();
        render.run();

        VBox col = new VBox(11, card, controls);
        HBox.setHgrow(col, Priority.ALWAYS); col.setMinWidth(compact ? 0 : 360);
        return col;
    }
    /** The "+ Spot" form and what to run each time it is opened. */
    private record SpotEntry(Region view, Runnable onOpen) {}
    /**
     * The cluster card's "+ Spot" form: call, frequency in MHz (taken from the rig each time the form opens, when the rig is connected and
     * the field is empty) and a comment, sent as {@code DX <call> <kHz> <comment>} through {@link JLogCockpit#postSpot}, the same path as
     * Log + Spot. The fields wrap, so it also fits the 322 px PANELS column.
     */
    static SpotEntry spotEntry() {
        TextField call = new TextField(); call.setPromptText("Call"); call.getStyleClass().add("jhub-mini-field"); call.setPrefColumnCount(8);
        TextField freq = new TextField(); freq.setPromptText("MHz");  freq.getStyleClass().add("jhub-mini-field"); freq.setPrefColumnCount(7);
        TextField cmt  = new TextField(); cmt.setPromptText("Comment (mode, report)"); cmt.getStyleClass().add("jhub-mini-field"); cmt.setPrefColumnCount(14);
        Label send = lbl("Send", "jhub-clearbtn"); send.setStyle("-fx-cursor:hand;");
        Label status = lbl("", "jhub-conn"); status.setWrapText(true);
        Runnable go = () -> {
            if (call.getText() == null || call.getText().isBlank()) { status.setText("✗ type the call to spot"); return; }
            String r = JLogCockpit.postSpot(call.getText(), freq.getText() == null ? "" : freq.getText(), "", cmt.getText());
            status.setText(r);
            if (r.startsWith("✓")) { call.clear(); cmt.clear(); }
        };
        send.setOnMouseClicked(e -> go.run());
        call.setOnAction(e -> go.run()); freq.setOnAction(e -> go.run()); cmt.setOnAction(e -> go.run());
        javafx.scene.layout.FlowPane fields = new javafx.scene.layout.FlowPane(8, 8, call, freq, cmt, send);
        fields.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(6, fields, status); box.setPadding(new Insets(0, 16, 12, 16));
        Runnable onOpen = () -> {
            status.setText("");
            com.ars.fx.data.RigClient.State s = com.ars.fx.data.RigClient.getInstance().last();
            if ((freq.getText() == null || freq.getText().isBlank()) && s != null && s.connected() && s.freqHz() > 0)
                freq.setText(String.format(java.util.Locale.ROOT, "%.4f", s.freqHz() / 1e6));
            call.requestFocus();
        };
        return new SpotEntry(box, onOpen);
    }
    /** A spot as one line for the narrow PANELS column: frequency, call, any NEW tag, then the mode at the right. The call is never cut short. */
    static Region cluRowCompact(String[] s) {
        Label fq = lbl(s[0], "jhub-clu-fq"); fq.setMinWidth(64);
        Label call = lbl(s[1], "jhub-clu-call"); call.setMinWidth(Region.USE_PREF_SIZE);
        HBox top = new HBox(8, fq, call);
        if (!s[2].isEmpty()) { Label need = lbl(s[2], "jhub-need"); need.setMinWidth(0); top.getChildren().add(need); }
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label mode = lbl(s[3], "jhub-clu-mode"); mode.setMinWidth(Region.USE_PREF_SIZE);
        top.getChildren().addAll(sp, mode);
        top.setAlignment(Pos.CENTER_LEFT);
        VBox v = new VBox(top); v.getStyleClass().add("jhub-clu");
        if (s[4] != null && !s[4].isBlank()) v.getStyleClass().add("band-" + s[4].trim().toLowerCase());
        return v;
    }
    /** What the compact row leaves out, for its tooltip: band, spotter, comment and age. */
    static String spotDetails(String[] s) {
        StringBuilder b = new StringBuilder(s[1]).append("  ").append(s[0]);
        if (s[4] != null && !s[4].isBlank()) b.append("  ").append(s[4]);
        b.append("\nde ").append(s[5]);
        if (s[6] != null && !s[6].isBlank()) b.append("  · ").append(s[6]);
        b.append("\n").append(s[7]);
        return b.toString();
    }
    static String filterSummary() {
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

    
    static void quickFill(TextField t, String v) {
        if (v != null && !v.isBlank() && t.getText().isBlank()) t.setText(v.trim());
    }

    /** A labelled-field row: alternating (VBox field, boolean grow) pairs. */
    static HBox frow(Object... pairs) {
        HBox row = new HBox(12); row.setAlignment(Pos.BOTTOM_LEFT);
        for (int i = 0; i < pairs.length; i += 2) {
            VBox f = (VBox) pairs[i];
            boolean grow = (Boolean) pairs[i + 1];
            if (grow) { HBox.setHgrow(f, Priority.ALWAYS); f.setMaxWidth(Double.MAX_VALUE); }
            row.getChildren().add(f);
        }
        return row;
    }

    static TextField tf(String prompt, String init, double w, boolean call) {
        TextField t = new TextField(init == null ? "" : init);
        if (prompt != null && !prompt.isEmpty()) t.setPromptText(prompt);
        t.getStyleClass().add("jhub-mini-field");
        if (call) t.getStyleClass().add("call");
        if (w > 0) { t.setMinWidth(w); t.setPrefWidth(w); t.setMaxWidth(w); }
        else { t.setMaxWidth(Double.MAX_VALUE); }
        return com.ars.fx.shell.Shell.caps(t);   // J-Log-Quick entry is upper case
    }

    static void fillLog(VBox tbl, Label countLbl) {
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
    static String nv(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    static final double[] LOGC = {52,-1,86,48,52,46};
    static HBox logRow(String[] c, boolean header) {
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
    static void wrapCol(HBox r, Node n, int i){
        HBox cell = new HBox(n); cell.setAlignment(Pos.CENTER_LEFT);
        if (LOGC[i]<0){ HBox.setHgrow(cell, Priority.ALWAYS); cell.setMaxWidth(Double.MAX_VALUE);} else { cell.setMinWidth(LOGC[i]); cell.setPrefWidth(LOGC[i]); cell.setMaxWidth(LOGC[i]); }
        r.getChildren().add(cell);
    }

    static Region cluRow(String[] s) { return cluRow(s, null); }
    static Region cluRow(String[] s, Node pathChip) {
        Label fq = lbl(s[0], "jhub-clu-fq"); fq.setMinWidth(64);
        HBox top = new HBox(9, fq, lbl(s[1], "jhub-clu-call"));
        if (!s[2].isEmpty()) top.getChildren().add(lbl(s[2], "jhub-need"));     // NEW ONE / NEW BAND / NEW MODE
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS); top.getChildren().add(sp);
        if (pathChip != null) top.getChildren().add(pathChip);
        top.getChildren().add(lbl(s[7], "jhub-clu-age"));
        top.setAlignment(Pos.CENTER_LEFT);
        String tail = "de " + s[5] + (s[6].isEmpty() ? "" : "  · " + s[6]);
        HBox bot = new HBox(9, lbl(s[3], "jhub-clu-mode"), lbl(s[4], "jhub-clu-de"), lbl(tail, "jhub-clu-de"));
        bot.setAlignment(Pos.CENTER_LEFT); HBox.setMargin(bot, new Insets(3,0,0,0));
        VBox v = new VBox(0, top, bot); v.getStyleClass().add("jhub-clu");
        if (s[4] != null && !s[4].isBlank()) v.getStyleClass().add("band-" + s[4].trim().toLowerCase());     // a band-coloured stripe in styles that ask for one
        return v;
    }

    // ---- drawer rail (rig + rotor + space wx) ------------------------------
    /** Setup > Appearance > "DX Cluster in the right column": the cluster heads the PANELS column instead of sitting under / beside New QSO. */
    static final String CLUSTER_RIGHT_KEY = "ui.dashboard.clusterRight";
    static boolean clusterInRail() { return com.ars.fx.data.HubConfig.getBool(CLUSTER_RIGHT_KEY, false); }

    /** @param cluster the compact DX Cluster (card + controls) to put at the top of the column, or null for the panels alone */
    static Region railCol(Region cluster) {
        VBox col = new VBox(11, com.ars.fx.shell.RigPanel.build(),
                Shell.rotorControlDrawer(),
                Shell.drawer("Amplifier", "bridge", "bridge", "", false, ampBody()),
                Shell.propagationDrawer(),
                Shell.solarSpaceWeatherDrawer());
        if (cluster != null) {
            // the cluster must never push the panels out of sight: they keep the height they need (scrolling, with a bar, if they need more
            // than there is) and the cluster takes what is left
            col.setMinWidth(0);                                   // with a scroll bar showing, the panels fit beside it instead of under it
            ScrollPane sp = new ScrollPane(col);
            sp.getStyleClass().add("sx-dock-scroll"); sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            return Shell.collapsibleRail(new RailSplit(cluster, sp));
        }
        col.setMinWidth(322); col.setPrefWidth(322); col.setMaxWidth(322);
        ScrollPane sp = new ScrollPane(col);                  // floating, it can't lean on the page to scroll
        sp.getStyleClass().add("sx-dock-scroll"); sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return Shell.collapsibleRail(sp);
    }
    /**
     * The PANELS column with the DX Cluster on top. The panels below (rig, rotor, amplifier, propagation, solar) get the height they ask for, so
     * every one of them stays in view; the cluster gets the rest, but never less than {@link #CLUSTER_MIN}. When the open panels need more than is
     * left after that, their own scroll pane shows a bar. (The plain column could let its content run off the bottom, because it had no cluster
     * above it to crowd the panels out.)
     */
    static final class RailSplit extends Region {
        private static final double GAP = 11, CLUSTER_MIN = 300, PANELS_MIN = 120;
        private final Region cluster, panels;
        RailSplit(Region cluster, Region panels) {
            this.cluster = cluster; this.panels = panels;
            getChildren().addAll(cluster, panels);
        }
        @Override protected double computePrefWidth(double h)  { return 322; }
        @Override protected double computeMinHeight(double w) { return CLUSTER_MIN + GAP + PANELS_MIN; }
        @Override protected double computePrefHeight(double w) { return cluster.prefHeight(w) + GAP + panels.prefHeight(w); }
        @Override protected void layoutChildren() {
            double w = getWidth(), h = getHeight();
            double ch = Math.max(CLUSTER_MIN, h - GAP - panels.prefHeight(w));   // the cluster grows into whatever the panels do not need
            double ph = Math.max(0, h - ch - GAP);                               // the panels scroll if they need more than this
            cluster.resizeRelocate(0, 0, w, ch);
            panels.resizeRelocate(0, ch + GAP, w, ph);
        }
    }
    static Region ampBody() {
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
            boolean over = AmpClient.getInstance().overLimit();
            pwrV.setText(c && s.pwr() > 0 ? (over ? "⚠ " : "") + Math.round(s.pwr()) + " W" : "—");
            pwrV.setStyle(over ? "-fx-text-fill:-ars-warn;" : "");
        });
        AmpClient.getInstance().start();
        return new VBox(12, stateRow, meters, conn);
    }

}
