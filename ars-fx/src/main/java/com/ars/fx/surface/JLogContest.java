package com.ars.fx.surface;

import com.ars.fx.data.ContestConfigBridge;
import com.ars.fx.data.ContestLayout;
import com.ars.fx.data.ContestLogs;
import com.ars.fx.data.ContestState;
import com.ars.fx.data.ContestValidators;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.MultCheck;
import com.ars.fx.data.QsoPartySide;
import com.ars.fx.shell.Shell;
import com.jlog.db.ContestQsoDao;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.ContestPlugin.FieldDef;
import com.jlog.scoring.ContestScore;
import com.jlog.scoring.ContestScorer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ars.fx.surface.JLogContestSend.*;
import static com.ars.fx.surface.JLogContestScore.*;
import static com.ars.fx.surface.JLogContestLog.*;
import static com.ars.fx.shell.Shell.lbl;

/**
 * J-Log — Contest mode. Pick a contest (plugin), log QSOs with the plugin's
 * exchange, live dupe + scoring via the j-log-engine, recent-log table, and
 * Cabrillo export. Multiplier trackers + maps come in later phases.
 */
public final class JLogContest {
    private JLogContest() {}

    static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    // value controls for the active plugin's entry fields, keyed by field id
    static Map<String, Control> fieldControls;
    static TextField bandF, modeF;
    static Label serialV;
    static Label sendV;                          // "what to send" line
    static QsoRecord editingRec;                 // the logged QSO being corrected (null = logging a new one)
    static Label editL;                          // "editing ..." banner
    static boolean callGivesMult;                // this contest's multiplier can be told from the call alone
    static Label memoryL;                        // "exchange filled from your earlier QSO"
    static final java.util.Map<String, String> spotMemo = new java.util.concurrent.ConcurrentHashMap<>();
    static volatile List<QsoRecord> logSnap;     // the contest log as of the last change, for spot tags
    static Label problemL;                       // what is wrong with the entry (invalid section, missing field...)
    static String problemOwner = "";             // field the message is about
    static boolean softArmed;                    // a "not recognised" warning was shown: the next Enter logs anyway
    static Label similarL;                       // possible-duplicate callsigns
    static com.ars.fx.surface.contest.ScpBox scpBox;   // Super Check Partial strip under the call
    static final java.util.Set<String> keepIds = new java.util.HashSet<>();   // fields that survive Clear (sent + persistent)
    static final java.util.Set<String> askedSent = new java.util.HashSet<>();  // contests whose missing exchange was asked this session
    static int serial = 1;
    static Label b4;                 // worked-before / dupe indicator
    static StackPane host;

    public static Region build() {
        ContestConfigBridge.bootstrap();
        ContestState.ensureLoaded();
        ContestPlugin plugin = ContestState.plugin();
        if (plugin == null) return ContestPicker.build();   // no contest chosen yet → chooser

        ContestState.clearListeners();   // before building panes that self-register as listeners
        spotMemo.clear(); logSnap = null;
        ContestState.addListener(() -> { spotMemo.clear(); logSnap = null; });     // the log changed: spot tags are stale
        callGivesMult = MultCheck.multFromCallAlone(plugin, ContestState.ctx());
        // cluster spots in contest mode: NEW MULT where the multiplier can be told from the call; no DXCC flags (they mean nothing in a contest)
        JHubDashboard.contestSpotTag = spot -> spotTag(plugin, spot);
        JHubDashboard.contestSpotDupe = spot -> spotDupe(plugin, spot);
        com.ars.fx.data.N1mmBroadcast.appInfo(plugin);                                   // tell listening programs which contest this is

        Region dock = Shell.dock("log");
        HBox top = Shell.topBar("log", "log", "J-Log", "Contest · " + plugin.getContestName(), scoreStats(), nowUtc());

        // One-screen operating position: entry + last QSOs on the left, DX cluster over the contest panes on the right.
        VBox left = new VBox(8, toolbar(plugin), new com.ars.fx.surface.contest.NetStrip(), entryPane(plugin), recentLogPane());
        left.setStyle("-fx-background-color:-ars-bg;");
        ScrollPane sp = new ScrollPane(left); sp.setFitToWidth(true); sp.setFitToHeight(true);   // scrolls only if the window is too short
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;");
        sp.setMinWidth(620); sp.setPrefWidth(780); HBox.setHgrow(sp, Priority.NEVER);

        Region cluster = JHubDashboard.contestClusterCol();
        cluster.setMinWidth(380); cluster.setMaxWidth(Double.MAX_VALUE);
        cluster.setMinHeight(300); cluster.setPrefHeight(620); cluster.setMaxHeight(Double.MAX_VALUE);   // twice the old 310 px; the spot list scrolls inside it, and it gives way when the window is short
        VBox.setVgrow(cluster, Priority.ALWAYS);
        VBox right = new VBox(10, cluster);
        TabPane tabs = contestTabs(plugin);
        if (tabs != null) { tabs.setMaxHeight(Double.MAX_VALUE); VBox.setVgrow(tabs, Priority.ALWAYS); right.getChildren().add(tabs); }   // the freed height goes to the sections / statistics
        right.setMinWidth(380); HBox.setHgrow(right, Priority.ALWAYS);
        HBox main = new HBox(14, sp, right); main.setFillHeight(true); main.setPadding(new Insets(0, 14, 10, 0));
        main.setStyle("-fx-background-color:-ars-bg;");

        // the right-hand rail keeps the station controls (rig, rotor); the contest panes live in the tabs
        List<Node> railDrawers = new ArrayList<>(Shell.leadDrawers());
        if (JHubDashboard.contestControlsDrawer != null) railDrawers.add(0, JHubDashboard.contestControlsDrawer);   // the cluster's controls: a drawer here, so the spot list gets the room
        Region rail = Shell.rail(railDrawers.toArray(new Node[0]));
        HBox withTools = new HBox(toolsColumn(plugin), main); withTools.setFillHeight(true);
        HBox.setHgrow(main, Priority.ALWAYS);
        host = new StackPane((Region) Shell.frame(dock, top, withTools, rail));

        ContestState.addListener(() -> Platform.runLater(JLogContestScore::refreshScoreStats));
        ContestState.recompute();
        startTicker();

        // DX spot click → prefill the contest call/band/mode (live + queued)
        SpotAction.setContestTarget(JLogContest::prefillSpot);
        SpotAction.Prefill queued = SpotAction.takePending();
        if (queued != null) prefillSpot(queued);

        maybeChooseDb(plugin);   // each time contest mode is entered: pick which database this contest logs to
        if (ContestLogs.confirmed()) askSentIfMissing(plugin);   // then anything of "what I send" that Setup does not know
        return host;
    }

    /** On entering contest mode (or switching contest) ask which database to log to, unless already chosen this session.
     *  Cancelling leaves contest mode — it cannot run without a database. */
    static void maybeChooseDb(ContestPlugin plugin) {
        if (ContestLogs.confirmed()) return;
        Platform.runLater(() -> ContestDbDialog.show(host, plugin, () -> Shell.navigate("logc"),
                () -> { ContestLogs.resetConfirmation(); Shell.navigate("hub"); }));
    }

    /** Switch database from inside contest mode (the "Switch database" link). */
    static void switchDatabase(Node source) {
        ContestState.ensureLoaded();
        ContestPlugin p = ContestState.plugin();
        if (p != null) ContestDbDialog.show(source, p, () -> Shell.navigate("logc"), null);
    }

    /** Entering contest mode from the normal log: offer the contest chosen last time (or let the operator pick another).
     *  Which database it logs to is asked next, when the contest screen opens. */
    public static void promptEnter(Node source) {
        ContestState.ensureLoaded();
        String where = "Your normal log is " + com.ars.fx.data.LogDbInfo.pretty(com.ars.fx.data.LogDbInfo.normalPath())
                + " (table qso). Contest QSOs go to a contest database of their own — you pick which one next — so contest mode never changes your normal log. ";
        Confirm.Choice cancel = new Confirm.Choice("Cancel", false, null);
        Runnable choose = () -> { ContestLogs.resetConfirmation(); ContestState.setActive(null); Shell.navigate("logc"); };
        String lastId = ContestState.lastId();
        ContestPlugin last = null;
        try { if (!lastId.isBlank()) last = com.jlog.plugin.PluginLoader.getInstance().getById(lastId); } catch (Exception ignored) {}
        if (last == null) {
            Confirm.show(source, "Switch to contest mode", where + "No contest has been chosen yet — pick one to continue.",
                    cancel, new Confirm.Choice("Choose a contest", true, choose));
            return;
        }
        String name = last.getContestName();
        Confirm.show(source, "Switch to contest mode", where + "Last contest: " + name + ".",
                cancel, new Confirm.Choice("Choose another contest", false, choose),
                new Confirm.Choice("Continue " + name, true, () -> { ContestState.setActive(lastId); ContestLogs.resetConfirmation(); Shell.navigate("logc"); }));
    }

    /** Fill the contest entry from a clicked DX spot (callsign + band/mode), then focus the call + run dupe. */
    public static void prefillSpot(SpotAction.Prefill p) {
        Runnable r = () -> {
            if (fieldControls == null || p == null) return;
            if (editingRec == null) clearForm();   // a new spot is a new contact: nothing of the last one (precedence, check, section ...) stays
            Control call = fieldControls.get("callsign");
            if (call instanceof TextField t && p.call() != null) { t.setText(p.call()); t.positionCaret(t.getText().length()); }
            if (p.band() != null && !p.band().isBlank() && bandF != null) bandF.setText(p.band());
            if (p.mode() != null && !p.mode().isBlank() && modeF != null) modeF.setText(p.mode());
            if (call != null) call.requestFocus();
            if (ContestState.plugin() != null) updateDupe(ContestState.plugin());
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    // ---- contest panes as tabs (multiplier trackers, maps, statistics) ------
    static TabPane contestTabs(ContestPlugin plugin) {
        List<Map.Entry<String, Region>> panes = com.ars.fx.surface.contest.TrackerFactory.titledPanes(plugin);
        if (panes.isEmpty()) return null;
        TabPane tp = new TabPane(); tp.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        ContestLayout lay = ContestLayout.resolve(plugin);                   // order and hiding, from the contest's layout
        List<Map.Entry<String, Region>> ordered = new ArrayList<>();
        List<String> titles = new ArrayList<>(lay.orderTabs(panes.stream().map(Map.Entry::getKey).toList()));
        QsoPartySide.Side sd = QsoPartySide.isParty(plugin) ? QsoPartySide.side(plugin) : null;
        if (sd != null && lay.tabs().isEmpty()) {             // counties first from outside the state, states and provinces first from inside
            boolean inState = sd == QsoPartySide.Side.IN_STATE;
            titles.sort(java.util.Comparator.comparingInt((String t) -> {
                String x = t.toLowerCase(java.util.Locale.ROOT);
                boolean county = x.contains("count"), statey = x.contains("state") || x.contains("provinc") || x.contains("map");
                return inState ? (statey ? 0 : county ? 1 : 2) : (county ? 0 : statey ? 2 : 1);
            }));
        }
        else if (lay.tabs().isEmpty())                        // every other contest: the multiplier trackers and maps before the statistics
            titles.sort(java.util.Comparator.comparingInt((String t) -> t.toLowerCase(java.util.Locale.ROOT).contains("statist") ? 1 : 0));
        for (String title : titles)
            for (Map.Entry<String, Region> e : panes) if (e.getKey().equals(title)) { ordered.add(e); break; }
        if (ordered.isEmpty()) return null;
        for (Map.Entry<String, Region> e : ordered) {
            ScrollPane c = new ScrollPane(e.getValue()); c.setFitToWidth(true);
            c.setStyle("-fx-background-color:transparent;");
            tp.getTabs().add(new Tab(e.getKey(), c));
        }
        tp.setMinHeight(220); tp.setPrefHeight(300); tp.setMaxHeight(340);
        return tp;
    }

    /** {QSOs/h over the last 10 minutes, QSOs in the last 60 minutes} - dupes don't count. */
    static int[] rates() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int c10 = 0, c60 = 0;
        for (QsoRecord q : ContestState.qsos()) {
            if (q.isDupe() || q.getDateTimeUtc() == null) continue;
            long m = java.time.temporal.ChronoUnit.MINUTES.between(q.getDateTimeUtc(), now);
            if (m < 10) c10++;
            if (m < 60) c60++;
        }
        return new int[]{c10 * 6, c60};
    }


    // ---- recent contest-log pane (refreshed on every score recompute) -----
    static VBox recentLogPane() {
        Label lh = lbl("☰  Last 10 QSOs"); lh.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:-ars-t3;");
        VBox tbl = new VBox(); tbl.getStyleClass().add("jl-logtbl");
        fillLog(tbl);
        ContestState.addListener(() -> Platform.runLater(() -> fillLog(tbl)));
        Label all = lbl("All QSOs \u2192", "jl-clearbtn"); all.setStyle("-fx-cursor:hand;");
        all.setOnMouseClicked(e -> openAllQsos());
        Region hgap = new Region(); HBox.setHgrow(hgap, Priority.ALWAYS);
        HBox head = new HBox(10, lh, hgap, all); head.setAlignment(Pos.CENTER_LEFT);
        VBox v = new VBox(6, head, tbl); v.setPadding(new Insets(0, 18, 12, 18));
        return v;
    }

    // ---- top toolbar: contest name + pick / normal / export ----------------
    static Region toolbar(ContestPlugin plugin) {
        Node ic = Shell.iconTile("log", "log", 16, "sx-dw-ic");
        Label name = lbl(plugin.getContestName(), "jhub-card-title");
        Label fmt = lbl(plugin.getExchangeFormat() == null ? "" : plugin.getExchangeFormat(), "jl-emeta");
        VBox ttl = new VBox(1, name, fmt);
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);

        Node chip = sideChip(plugin);
        HBox bar = new HBox(11, ic, ttl, gap);
        if (chip != null) bar.getChildren().add(2, chip);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(14, 18, 6, 18));
        return bar;
    }

    // ---- LOG TOOLS column: the contest actions that used to crowd the title bar -----------------------------
    /** The collapsing column beside the dock, as on the J-Hub dashboard: contest actions, translator, awards, database tools. */
    /** The "print a label for each new contest QSO" switch (its own setting, off by default). */
    static Shell.QslToggle printQsl;

    static Region toolsColumn(ContestPlugin plugin) {
        Shell.openDrawerByDefault("Contest");
        Label pick = lbl("Change contest", "jl-clearbtn"); pick.setStyle("-fx-cursor:hand;");
        pick.setOnMouseClicked(e -> Confirm.show(pick, "Change contest?",
                "Pick a different contest? The current contest's QSOs stay logged.",
                new Confirm.Choice("Cancel", false, null),
                new Confirm.Choice("Change", true, () -> { ContestLogs.resetConfirmation(); ContestState.setActive(null); Shell.navigate("logc"); })));
        Label normal = lbl("Leave contest", "jl-clearbtn"); normal.setStyle("-fx-cursor:hand;");
        normal.setOnMouseClicked(e -> Confirm.show(normal, "Leave contest mode?",
                "Go back to the J-Hub dashboard for normal logging? Your contest log is saved in " + com.ars.fx.data.LogDbInfo.pretty(com.ars.fx.data.LogDbInfo.contestPath())
                        + ". Normal QSOs go to " + com.ars.fx.data.LogDbInfo.pretty(com.ars.fx.data.LogDbInfo.normalPath()) + ".",
                new Confirm.Choice("Stay", false, null),
                new Confirm.Choice("Leave", true, () -> { ContestLogs.resetConfirmation(); Shell.navigate("hub"); })));
        Label export = lbl("Export Cabrillo ⤓", "jl-logbtn"); export.setStyle("-fx-cursor:hand;");
        export.setOnMouseClicked(e -> exportCabrillo(plugin));

        Label times = lbl("Contest times\u2026", "jl-clearbtn"); times.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(times, "Set when this contest starts and ends (UTC) to get a clock with time left and off time");
        times.setOnMouseClicked(e -> editTimes(times, plugin));
        Label layoutBtn = lbl("Layout\u2026", "jl-clearbtn"); layoutBtn.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(layoutBtn, "Open this contest's layout file: a reminder note, the order of the tabs and of the exchange fields");
        layoutBtn.setOnMouseClicked(e -> openLayoutFile(plugin));
        printQsl = Shell.contestQslToggle();
        VBox contestBody = new VBox(8, export, times, layoutBtn, printQsl.node, pick, normal);
        Node contestDrawer = Shell.drawer("Contest", "log", "log", plugin.getContestName(), true, contestBody);
        Node[] more = JLogCockpit.contestSideDrawers(() -> {
            Control cf = fieldControls == null ? null : fieldControls.get("callsign");
            return cf instanceof TextField t ? t.getText() : "";
        });
        List<Node> all = new ArrayList<>(); all.add(contestDrawer); all.add(LogDbBar.drawer(true)); all.addAll(List.of(more));
        return Shell.collapsibleLeftPanel("jlog", "LOG TOOLS", 322, all.toArray(new Node[0]));
    }

    private static java.util.function.Consumer<com.ars.fx.data.RigClient.State> freqListener;

    /** What the next QSO will be logged on: the rig's dial frequency, read-only, above the exchange row. Blank when it would not be stored. */
    private static Node freqRow() {
        Label v = lbl("", "jhub-rig-fq");
        Label note = lbl("", "jl-emeta");
        Runnable upd = () -> {
            String khz = bandF == null ? null : com.ars.fx.data.ContestFreq.rigKhz(bandF.getText());
            if (khz != null) { v.setText(com.ars.fx.data.ContestFreq.toMhz(khz) + " MHz"); note.setText("saved with the QSO"); }
            else { v.setText("\u2014"); note.setText("not saved: no rig, or the rig is on another band than BAND"); }
        };
        com.ars.fx.data.RigClient rc = com.ars.fx.data.RigClient.getInstance();
        if (freqListener != null) rc.removeListener(freqListener);
        freqListener = st -> javafx.application.Platform.runLater(upd);
        rc.addListener(freqListener);
        if (bandF != null) bandF.textProperty().addListener((o, a, b) -> upd.run());
        upd.run();
        HBox h = new HBox(10, lbl("FREQ", "jl-eyebrow"), v, note);
        h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return h;
    }

    // ---- entry pane: band/mode + plugin fields + log table -----------------
    static VBox entryPane(ContestPlugin plugin) {
        fieldControls = new LinkedHashMap<>();
        keepIds.clear();
        serial = ContestState.nextSerial();

        VBox body = new VBox(9); body.setPadding(new Insets(4, 18, 8, 18));

        // BAND + MODE (seeded from the rig; locked when the plugin pins them)
        com.ars.fx.data.RigClient.State rs = com.ars.fx.data.RigClient.getInstance().last();
        String band0 = plugin.getLockedBand() != null ? plugin.getLockedBand()
                : (rs.connected() && rs.freqHz() > 0 ? bandFor(rs.freqHz()) : "20m");
        String mode0 = defaultMode(plugin, rs);
        bandF = tf("", band0, 80); modeF = tf("", mode0, 80);
        bandF.textProperty().addListener((o, a, b) -> refreshScoreStats());
        if (plugin.getLockedBand() != null) bandF.setDisable(true);
        if (plugin.getLockedMode() != null) modeF.setDisable(true);

        // received row (entryRow 0) and sent row (entryRow 1) from the plugin
        List<Node> rcvd = new ArrayList<>();
        List<Node> sent = new ArrayList<>();
        rcvd.add(g("BAND", bandF));
        rcvd.add(g("MODE", modeF));
        ContestLayout layout = ContestLayout.resolve(plugin);
        List<FieldDef> fieldsInOrder = new ArrayList<>();
        for (String id : layout.orderFields(plugin.getEntryFields().stream().map(FieldDef::getId).toList()))
            for (FieldDef f : plugin.getEntryFields()) if (f.getId().equals(id)) { fieldsInOrder.add(f); break; }
        if (!layout.note().isEmpty()) {                                                // a reminder for this contest
            Label noteL = lbl(layout.note(), "jl-emeta"); noteL.setWrapText(true); noteL.setMaxWidth(Double.MAX_VALUE);
            noteL.setStyle("-fx-font-size:13px;");
            body.getChildren().add(noteL);
        }
        for (FieldDef fd : fieldsInOrder) {
            if ("band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;   // dedicated selectors above
            Control c = controlFor(fd);
            if (c instanceof TextField tfc && QsoPartySide.isParty(plugin) && fd.getId().equals("state_prov_rcvd")) {
                QsoPartySide.Side sd0 = QsoPartySide.side(plugin);
                if (sd0 != null) tfc.setPromptText(sd0 == QsoPartySide.Side.IN_STATE ? "county / state / DX" : (QsoPartySide.partyState(plugin) + " county"));
            }
            fieldControls.put(fd.getId(), c);
            if ("callsign".equals(fd.getId()) && c instanceof TextField cf) {
                cf.setMinWidth(210); cf.setPrefWidth(210); cf.setStyle(CALL_STYLE);   // the call is what you look at
                cf.textProperty().addListener((o, a, b) -> TranslatorWindow.follow(b));     // the translator (if open) follows the country
            }
            if (fd.getEntryRow() == 1) {                       // what WE send: filled from Setup / earlier answers, shown in the SEND line
                keepIds.add(fd.getId());
                setControl(c, SentExchange.value(plugin, fd, modeF.getText()));
            } else {
                if (fd.isPersistent() && !"callsign".equals(fd.getId())) keepIds.add(fd.getId());
                rcvd.add(g(fd.getLabel(), c));
            }
        }
        serialV = lbl(String.valueOf(serial), "jhub-rig-fq");   // kept for clearForm; the number is shown in the SEND line

        editL = lbl("", "jl-emeta"); editL.setWrapText(true); editL.setMaxWidth(Double.MAX_VALUE);
        editL.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:-ars-accent;");
        editL.managedProperty().bind(editL.textProperty().isNotEmpty());
        editL.visibleProperty().bind(editL.textProperty().isNotEmpty());
        body.getChildren().add(editL);
        body.getChildren().add(freqRow());
        body.getChildren().add(wrapRow("RECEIVED", rcvd));
        body.getChildren().add(sendRow(plugin));

        // worked-before / dupe indicator + actions
        b4 = lbl("New", "jl-eyebrow"); b4.setStyle("-fx-font-size:20px;-fx-font-weight:bold;");   // DUPE / NEW / NEW BAND, readable from across the desk
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        Label clear = lbl("Clear", "jl-clearbtn");   // a real target: the plain text link was too small to hit in a hurry
        clear.setStyle("-fx-cursor:hand;-fx-font-size:16px;-fx-font-weight:bold;-fx-padding:12 26 12 26;-fx-border-color:-ars-err;-fx-border-radius:8;-fx-background-radius:8;");
        Label logBtn = lbl("Log QSO  ⏎", "jl-logbtn"); logBtn.setStyle("-fx-cursor:hand;");
        Label spotBtn = lbl("Log + Spot", "jl-logbtn"); spotBtn.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(spotBtn, "Save this QSO and post a spot for the station to the DX cluster, on the rig's frequency");
        HBox actions = new HBox(12, b4, gap, clear, spotBtn, logBtn); actions.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().add(actions);
        problemL = lbl("", "jl-emeta"); problemL.setWrapText(true); problemL.setMaxWidth(Double.MAX_VALUE);
        problemL.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:-ars-err;");
        problemL.managedProperty().bind(problemL.textProperty().isNotEmpty());
        problemL.visibleProperty().bind(problemL.textProperty().isNotEmpty());
        body.getChildren().add(problemL);
        memoryL = lbl("", "jl-emeta"); memoryL.setWrapText(true); memoryL.setMaxWidth(Double.MAX_VALUE);
        memoryL.setStyle("-fx-font-size:14px;");
        memoryL.managedProperty().bind(memoryL.textProperty().isNotEmpty()); memoryL.visibleProperty().bind(memoryL.textProperty().isNotEmpty());
        similarL = lbl("", "jl-emeta"); similarL.setWrapText(true);   // possible duplicates: calls already logged that resemble the typed one
        similarL.setMaxWidth(Double.MAX_VALUE);
        similarL.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:-ars-err;-fx-font-family:'JetBrains Mono';"
                + "-fx-background-color:-ars-surface-2;-fx-border-color:-ars-err;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 12 8 12;");
        similarL.managedProperty().bind(similarL.textProperty().isNotEmpty());   // no empty box while there is nothing to warn about
        similarL.visibleProperty().bind(similarL.textProperty().isNotEmpty());
        body.getChildren().add(similarL);
        scpBox = new com.ars.fx.surface.contest.ScpBox(c -> {
            if (fieldControls.get("callsign") instanceof TextField cf) { cf.setText(c); cf.requestFocus(); cf.positionCaret(c.length()); }
        }, workedInContest(), com.ars.fx.data.ScpStore.listFor(plugin));
        body.getChildren().add(scpBox);
        body.getChildren().add(memoryL);

        // F1–F8 station macros (shared with normal log + dashboard)
        body.getChildren().add(runRow());
        body.getChildren().add(MacroBar.sendAsRow(modeF));
        List<com.ars.fx.data.MacroConfig.Macro> ownMacros = layout.contestMacros();     // the contest's own F1-F8, if its layout has them
        List<com.ars.fx.data.MacroConfig.Macro> macroSet = ownMacros.isEmpty() ? null : ownMacros;
        java.util.function.IntConsumer fireMacro = MacroBar.fire(() -> modeF.getText(), () -> value("callsign"), msg -> setB4("▶ " + msg, "new"),
                macroSet, () -> macroTags(plugin));
        body.getChildren().add(MacroBar.row(fireMacro, macroSet));
        installFKeys(body, fireMacro, macroSet == null ? com.ars.fx.data.MacroConfig.count() : macroSet.size());

        Runnable doLog = () -> { if (logQso(plugin)) finishLog(); };   // recompute → listeners refresh log + trackers
        logBtn.setOnMouseClicked(e -> { spotAfter = false; doLog.run(); });
        spotBtn.setOnMouseClicked(e -> {
            if (editingRec != null) { setB4("Spot is for new QSOs, not edits", "new"); return; }
            spotAfter = true; doLog.run();
        });
        clear.setOnMouseClicked(e -> clearForm());

        // keyboard flow: Enter in any field logs (a missing required field takes the focus instead), Esc clears
        for (Control c : fieldControls.values()) {
            if (c instanceof TextField t) t.setOnAction(e -> { spotAfter = false; doLog.run(); });
            c.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) clearForm(); });
        }
        // Space = next field (call, then the exchange in the order shown); the space itself is not typed
        List<Control> spaceOrder = new ArrayList<>();
        for (FieldDef fd : fieldsInOrder) {
            if (fd.getEntryRow() == 1 || "band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;
            Control c = fieldControls.get(fd.getId());
            if (c instanceof TextField) spaceOrder.add(c);
        }
        for (int i = 0; i < spaceOrder.size(); i++) {
            final Control cur = spaceOrder.get(i), next = i + 1 < spaceOrder.size() ? spaceOrder.get(i + 1) : null;
            cur.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, e -> { if (" ".equals(e.getCharacter())) e.consume(); });
            cur.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() != javafx.scene.input.KeyCode.SPACE) return;
                e.consume();
                if (cur == fieldControls.get("callsign")) com.ars.fx.data.N1mmBroadcast.lookup(plugin, candidate(plugin));   // Space after the call: a lookup, before the QSO is logged
                if (next != null) { next.requestFocus(); if (next instanceof TextField t) t.selectAll(); }
            });
        }
        // Tab / Shift+Tab stay in the entry fields (call and the exchange, in order, wrapping round): they never wander off to the send-as CW / Voice picks
        // or the macro buttons. Band and mode come from the rig, so Tab from them goes back to the call.
        if (!spaceOrder.isEmpty()) {
            List<Control> tabRing = spaceOrder;
            java.util.function.Consumer<javafx.scene.input.KeyEvent> tabMove = e -> {
                if (e.getCode() != javafx.scene.input.KeyCode.TAB) return;
                e.consume();
                int at = tabRing.indexOf((Control) e.getSource());
                int to = at < 0 ? 0 : e.isShiftDown() ? (at - 1 + tabRing.size()) % tabRing.size() : (at + 1) % tabRing.size();
                Control next = tabRing.get(to);
                next.requestFocus(); if (next instanceof TextField t) t.selectAll();
            };
            for (Control c : tabRing) c.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, tabMove::accept);
            bandF.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, tabMove::accept);
            modeF.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, tabMove::accept);
            for (Control c : fieldControls.values()) if (!tabRing.contains(c)) c.setFocusTraversable(false);       // a sent-exchange or other non-entry field
        }
        bandF.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) clearForm(); });
        modeF.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) clearForm(); });

        // live validation: red border the moment a value can never be valid, amber if merely unrecognised; the full check on leaving the field
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1 || "band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;
            if (fieldControls.get(fd.getId()) instanceof TextField t) {
                t.textProperty().addListener((o, a, b) -> { softArmed = false; liveCheck(plugin, fd, t, false); });
                t.focusedProperty().addListener((o, was, now) -> { if (!now) liveCheck(plugin, fd, t, true); });
                if (!PickList.universe(plugin, fd).isEmpty()) PickList.attach(t, () -> PickList.universe(plugin, fd));   // type-ahead for a fixed list
            }
        }

        // live dupe check as the call is typed (short pause after the last key), and when band/mode change
        javafx.animation.PauseTransition dupeDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));
        dupeDelay.setOnFinished(e -> updateDupe(plugin, true));
        javafx.animation.PauseTransition exchDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        exchDelay.setOnFinished(e -> updateDupe(plugin, false));
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1 || "callsign".equals(fd.getId()) || "band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;
            if (fieldControls.get(fd.getId()) instanceof TextField t) t.textProperty().addListener((o, a, b) -> exchDelay.playFromStart());
        }
        Control callC = fieldControls.get("callsign");
        if (callC instanceof TextField cf) cf.textProperty().addListener((o, a, b) -> {
            dupeDelay.playFromStart(); if (scpBox != null) scpBox.update(b);
            // the call was deleted: whatever the dupe / call-history lookup filled in (precedence, check, section ...) goes with it
            if ((b == null || b.isEmpty()) && a != null && !a.isEmpty() && editingRec == null) Platform.runLater(() -> { if (cf.getText() == null || cf.getText().isEmpty()) clearForm(); });   // still empty: a spot click may have set a call since
        });
        bandF.textProperty().addListener((o, a, b) -> dupeDelay.playFromStart());
        modeF.textProperty().addListener((o, a, b) -> dupeDelay.playFromStart());

        // the multiplier field (the section): once it is typed, blink that section in the grid so it can be found at a glance
        if (plugin.getMultiplierModel() != null && plugin.getMultiplierModel().getField() != null
                && fieldControls.get(plugin.getMultiplierModel().getField()) instanceof TextField mf) {
            javafx.animation.PauseTransition blinkDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
            blinkDelay.setOnFinished(e -> com.ars.fx.surface.contest.Lightbox.flash(mf.getText()));
            mf.textProperty().addListener((o, a, b) -> {
                if (b != null && !b.isBlank()) blinkDelay.playFromStart();
                else { blinkDelay.stop(); com.ars.fx.surface.contest.Lightbox.flash(null); }      // cleared: the blink stops
            });
        }

        // CQ WPX: the prefix follows from the call (K1ABC -> K1), so fill it as the call is typed; one the operator has typed over is left alone
        if (plugin.isAutoFillWpxPrefix() && fieldControls.get("prefix_rcvd") instanceof TextField pf && callC instanceof TextField cf) {
            String[] last = {""};
            cf.textProperty().addListener((o, a, b) -> {
                String auto = b == null || b.isBlank() ? "" : com.jlog.util.CallsignRegion.wpxPrefix(b);
                if (pf.getText() == null || pf.getText().isEmpty() || pf.getText().equals(last[0])) pf.setText(auto);
                last[0] = auto;
            });
        }

        return body;
    }

    /** The mode a contest opens in: the one it pins, else the one its name says (SSB / CW / RTTY / digital), else the rig's, else CW. */
    static String defaultMode(ContestPlugin plugin, com.ars.fx.data.RigClient.State rs) {
        String m = SentExchange.contestMode(plugin);
        if (m != null) return m;
        String own = ContestLayout.resolve(plugin).modeOrNull();                           // the contest's own layout
        if (own != null) return own;
        String pref = HubConfig.get("contest.defaultMode", "Auto");                    // Setup: Auto / SSB / CW
        if (pref.equals("SSB") || pref.equals("CW")) return pref;
        return rs.connected() && rs.mode() != null && !rs.mode().isBlank() ? rs.mode() : "SSB";   // Auto: follow the rig, else SSB
    }

    // ---- save -------------------------------------------------------------
    /** "NEW MULT" for a spot that would add a multiplier, else "". Remembered until the log changes. */
    static String spotTag(ContestPlugin plugin, com.ars.fx.data.ClusterClient.Spot spot) {
        String key = spot.call() + "|" + spot.band() + "|" + spot.mode();
        return spotMemo.computeIfAbsent(key, k -> {
            List<QsoRecord> snap = logSnap;
            if (snap == null) { snap = ContestState.qsos(); logSnap = snap; }
            QsoRecord cand = MultCheck.candidateOf(plugin, spot.call(), spot.band(), spot.mode());
            if (callGivesMult) return MultCheck.isNewMult(plugin, snap, cand, ContestState.ctx()) ? "NEW MULT" : "";
            // the multiplier is in the exchange: use what the call history says this station sends, if the contest accepts it
            return fromHistory(plugin, cand) && MultCheck.isNewMult(plugin, snap, cand, ContestState.ctx()) ? "NEW MULT?" : "";
        });
    }
    private static final java.util.Map<String, Boolean> dupeMemo = new java.util.HashMap<>();
    private static long dupeMemoSize = -1;
    /** Is the spotted station a dupe in this contest, by the contest's own rule? Worked out from the log in memory, remembered until the log changes. */
    static boolean spotDupe(ContestPlugin plugin, com.ars.fx.data.ClusterClient.Spot spot) {
        List<QsoRecord> all = ContestState.qsos();
        if (all.size() != dupeMemoSize) { dupeMemo.clear(); dupeMemoSize = all.size(); }
        String call = spot.call() == null ? "" : spot.call().toUpperCase(java.util.Locale.ROOT);
        return dupeMemo.computeIfAbsent(call + "|" + spot.band() + "|" + spot.mode(), k -> {
            List<QsoRecord> prior = new ArrayList<>();
            for (QsoRecord q : all) if (q.getCallsign() != null && q.getCallsign().equalsIgnoreCase(call)) prior.add(q);
            if (prior.isEmpty()) return false;
            try { return ContestScorer.isDupe(plugin, MultCheck.candidateOf(plugin, call, spot.band(), spot.mode()), prior); } catch (Exception e) { return false; }
        });
    }
    /** Fill {@code cand}'s multiplier field from the call history. False when there is nothing usable (then no tag is better than a wrong one). */
    static boolean fromHistory(ContestPlugin plugin, QsoRecord cand) {
        var mm = plugin.getMultiplierModel();
        if (mm == null || mm.getField() == null) return false;
        String slot = plugin.fieldSlotColumn(mm.getField());
        if (slot == null) return false;
        java.util.Map<String, String> hist = com.ars.fx.data.CallHistory.get(cand.getCallsign());
        for (FieldDef fd : plugin.getEntryFields()) {
            if (!mm.getField().equals(fd.getId())) continue;
            for (String key : com.ars.fx.data.CallHistory.keysForField(plugin, fd.getId())) {
                String v = hist.get(key);
                if (v != null && acceptable(plugin, fd, v)) { setSlot(cand, slot, v); return true; }
            }
        }
        return false;
    }

    /** F1-F8 key the macros, for as long as this page is on screen (and never after: the keys are handed back when the page leaves the scene). */
    static void installFKeys(Node anchor, java.util.function.IntConsumer fire, int count) {
        javafx.scene.input.KeyCode[] fk = {javafx.scene.input.KeyCode.F1, javafx.scene.input.KeyCode.F2, javafx.scene.input.KeyCode.F3, javafx.scene.input.KeyCode.F4,
                javafx.scene.input.KeyCode.F5, javafx.scene.input.KeyCode.F6, javafx.scene.input.KeyCode.F7, javafx.scene.input.KeyCode.F8};
        List<javafx.scene.input.KeyCodeCombination> mine = new ArrayList<>();
        anchor.sceneProperty().addListener((o, oldS, s) -> {
            if (oldS != null) for (var k : mine) oldS.getAccelerators().remove(k);
            mine.clear();
            if (s == null) return;
            for (int i = 0; i < count && i < fk.length; i++) {
                final int idx = i;
                var k = new javafx.scene.input.KeyCodeCombination(fk[i]);
                mine.add(k);
                s.getAccelerators().put(k, () -> fire.accept(idx));
            }
        });
    }

    /** Is this call already in the open contest's log (on any band)? Rebuilt only when the log changes size. */
    static java.util.function.Predicate<String> workedInContest() {
        java.util.Set<String>[] calls = new java.util.Set[]{java.util.Set.of()};
        int[] n = {-1};
        return c -> {
            List<QsoRecord> q = ContestState.qsos();
            if (q.size() != n[0]) {
                java.util.Set<String> s = new java.util.HashSet<>();
                for (QsoRecord r : q) if (r.getCallsign() != null) s.add(r.getCallsign().trim().toUpperCase());
                calls[0] = s; n[0] = q.size();
            }
            return calls[0].contains(c.toUpperCase());
        };
    }

    /** Copy what is on the form (call, band, mode and every exchange field) into a record. */
    static void fillFromForm(ContestPlugin plugin, QsoRecord r) {
        r.setCallsign(value("callsign").toUpperCase());
        r.setBand(normBand(bandF.getText()));
        r.setMode(modeF.getText().trim().toUpperCase());
        for (FieldDef fd : plugin.getEntryFields()) {
            String v = value(fd.getId());
            switch (fd.getId()) {
                case "callsign", "band", "mode" -> {}   // band/mode come from the dedicated selectors
                case "rst_rcvd", "rst_received" -> r.setRstReceived(v);
                case "rst_sent" -> r.setRstSent(v);
                case "serial_rcvd", "serial_received" -> r.setSerialReceived(v);
                default -> {
                    String slot = plugin.fieldSlotColumn(fd.getId());
                    if (slot != null) setSlot(r, slot, v.toUpperCase());
                }
            }
        }
    }
    /** A record of the QSO being typed, not yet logged. */
    static QsoRecord candidate(ContestPlugin plugin) {
        QsoRecord r = new QsoRecord();
        r.setContestId(plugin.getContestId());
        r.setDateTimeUtc(LocalDateTime.now(ZoneOffset.UTC));
        fillFromForm(plugin, r);
        return r;
    }
    /** Enough typed to ask "is it a new multiplier?": the multiplier field filled in and valid, or (when the call gives it) a call. */
    static boolean readyForMult(ContestPlugin plugin) {
        if (value("callsign").length() < 3 || bandF.getText().isBlank() || modeF.getText().isBlank()) return false;
        var mm = plugin.getMultiplierModel();
        if (mm == null || mm.getField() == null) return true;
        FieldDef fd = null;
        for (FieldDef f : plugin.getEntryFields()) if (mm.getField().equals(f.getId())) fd = f;
        if (fd == null || value(fd.getId()).isBlank()) return callGivesMult;   // no section yet: only a contest whose call alone gives the multiplier can say
        // a section the list does not recognise (SOFT) is not a multiplier to claim either
        return ContestValidation.check(plugin, fd, value(fd.getId()), true).level() == ContestValidation.Level.OK;
    }

    static boolean spotAfter;        // "Log + Spot" pressed: post a spot once the QSO is saved
    static String spotNote = "";     // what happened to it, shown after the form clears

    /** Post the spot for a QSO about to be logged, on the rig's dial frequency; returns a short status. */
    static String spotNow(QsoRecord r) {
        com.ars.fx.data.RigClient.State rs = com.ars.fx.data.RigClient.getInstance().last();
        if (rs == null || !rs.connected() || rs.freqHz() <= 0) return "✗ spot not sent — rig not connected, no frequency";
        ContestPlugin pl = ContestState.plugin();
        String mult = pl != null && pl.getMultiplierModel() != null && pl.getMultiplierModel().getField() != null ? value(pl.getMultiplierModel().getField()) : "";
        // the contest tag and the section the station sent ("SS ENY"); without a mode, so the short cluster comment keeps it
        return JLogCockpit.postSpot(r.getCallsign(), String.valueOf(rs.freqHz() / 1e6), "", com.ars.fx.data.SpotComment.of(pl == null ? "" : pl.getContestId(), mult));
    }
    /** After a successful log: clear the form, then show the spot result if there is one. */
    static void finishLog() {
        String note = spotNote; spotNote = ""; spotAfter = false;
        clearForm();
        if (!note.isEmpty()) setB4(note, "new");
    }

    static boolean logQso(ContestPlugin plugin) { return logQso(plugin, false); }

    /** @param dupeApproved the operator has already said "log this dupe anyway" */
    static boolean logQso(ContestPlugin plugin, boolean dupeApproved) {
        String call = value("callsign");
        if (call == null || call.isBlank()) { focus("callsign"); return false; }
        // required-field check (band/mode come from the dedicated selectors)
        for (FieldDef fd : plugin.getEntryFields()) {
            if ("band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;
            if (fd.isRequired() && fd.getEntryRow() == 0 && value(fd.getId()).isBlank()) {
                showProblem(fd.getId(), "Missing: " + (fd.getLabel() == null ? fd.getId() : fd.getLabel())); focus(fd.getId()); return false;
            }
        }
        if (!validateEntry(plugin)) return false;      // an invalid value never reaches the log
        try {
            String cid = plugin.getContestId();
            boolean editing = editingRec != null;
            QsoRecord r = editing ? editingRec : new QsoRecord();      // an edit keeps the QSO's time, serial and everything not on the form
            final String oldCall = editing ? r.getCallsign() : null;    // an edit tells other programs which call it replaces
            r.setContestId(cid);
            if (!editing) {
                r.setDateTimeUtc(LocalDateTime.now(ZoneOffset.UTC));
                r.setSerialSent(String.valueOf(serial));
                String khz = com.ars.fx.data.ContestFreq.rigKhz(bandF.getText());   // Cabrillo's frequency column; none if no rig or it is on another band
                if (khz != null) r.setFrequency(khz);
            }
            fillFromForm(plugin, r);
            if (!editing) {
                // a county line ("ADA/CAN"): one QSO per county, or per pair of counties when both stations are on lines. Those that are
                // not dupes are logged at once; if every one is a dupe, the first goes through the usual dupe question below.
                List<QsoRecord> lines = com.ars.fx.data.CountyLine.expand(plugin, r);
                if (lines.size() > 1) {
                    List<QsoRecord> seen = new ArrayList<>(ContestQsoDao.getInstance().findByCallsign(cid, call));
                    List<QsoRecord> keep = new ArrayList<>();
                    for (QsoRecord c : lines) if (!ContestScorer.isDupe(plugin, c, seen)) { keep.add(c); seen.add(c); }
                    if (!keep.isEmpty()) return logCountyLine(plugin, keep);
                    QsoRecord first = lines.get(0);
                    for (String id : com.ars.fx.data.CountyLine.FIELDS) {     // carry on with the first county only
                        String slot = plugin.fieldSlotColumn(id);
                        if (slot != null) setSlot(r, slot, slotValue(first, slot));
                    }
                }
            }
            List<QsoRecord> prior = new ArrayList<>(ContestQsoDao.getInstance().findByCallsign(cid, call));
            if (editing) prior.removeIf(x -> x.getId() == r.getId());
            boolean dupe = ContestScorer.isDupe(plugin, r, prior);
            if (dupe && !dupeApproved) {   // a dupe is never logged without the operator saying so
                setB4("\u26d4 DUPE \u2014 " + call.toUpperCase(), "dupe");
                Confirm.show(b4, "Dupe \u2014 " + call.toUpperCase(),
                        call.toUpperCase() + " is already in the log for " + normBand(bandF.getText()) + " " + modeF.getText().trim().toUpperCase()
                                + ". A dupe scores nothing. Log it anyway?",
                        new Confirm.Choice("Don't log it", true, () -> focus("callsign")),
                        new Confirm.Choice("Log the dupe", false, () -> { if (logQso(plugin, true)) finishLog(); }));
                return false;
            }
            r.setDupe(dupe);
            r.setPoints(ContestScorer.points(plugin, r, ContestState.ctx()));
            // a new multiplier is worked out before the QSO is in the log (only when something is listening: it scores the log twice)
            boolean newMult = !editing && !dupe && com.ars.fx.data.N1mmBroadcast.enabled() && MultCheck.isNewMult(plugin, ContestState.qsos(), r, ContestState.ctx());
            if (editing) { ContestQsoDao.getInstance().update(r); editingRec = null; reflagDupes(plugin); com.ars.fx.data.N1mmBroadcast.contactEdited(plugin, r, oldCall); }
            else { if (spotAfter) spotNote = spotNow(r); ContestQsoDao.getInstance().insert(r); serial++; com.ars.fx.data.N1mmBroadcast.contactAdded(plugin, r, newMult); com.ars.fx.shell.IdTimer.reset(); com.ars.fx.data.QsoRecorder.logged(r.getCallsign(), r.getBand(), r.getMode()); com.ars.fx.data.SessionQsos.logged(r.getCallsign(), null, r.getBand(), r.getMode()); }
            ContestState.recompute();
            if (!editing && !dupe && printQsl != null) printQsl.afterSave(com.ars.fx.data.QslLabel.fromContest(r));   // new, non-dupe QSOs only
            final QsoRecord logged = r;
            Thread lt = new Thread(() -> com.ars.fx.data.CallHistory.learn(plugin, logged), "call-history-learn");
            lt.setDaemon(true); lt.start();
            return true;
        } catch (Exception e) {
            System.err.println("[contest] log failed: " + e.getMessage());
            return false;
        }
    }

    /** Log the QSOs of one county-line contact (none of them a dupe): one serial number, one QSL label, every line in the log. */
    private static boolean logCountyLine(ContestPlugin plugin, List<QsoRecord> qsos) throws Exception {
        for (QsoRecord c : qsos) {
            c.setDupe(false);
            c.setPoints(ContestScorer.points(plugin, c, ContestState.ctx()));
            ContestQsoDao.getInstance().insert(c);
            com.ars.fx.data.N1mmBroadcast.contactAdded(plugin, c, false);
        }
        QsoRecord first = qsos.get(0);
        if (spotAfter) spotNote = spotNow(first);
        serial++;
        com.ars.fx.data.SessionQsos.logged(first.getCallsign(), null, first.getBand(), first.getMode());
        com.ars.fx.shell.IdTimer.reset(); com.ars.fx.data.QsoRecorder.logged(first.getCallsign(), first.getBand(), first.getMode());   // one recording for the contact
        ContestState.recompute();
        if (printQsl != null) printQsl.afterSave(com.ars.fx.data.QslLabel.fromContest(first));
        Thread lt = new Thread(() -> com.ars.fx.data.CallHistory.learn(plugin, first), "call-history-learn");
        lt.setDaemon(true); lt.start();
        setB4("Logged " + qsos.size() + " QSOs: county line", "new");
        return true;
    }

    // ---- edit / erase a logged QSO ------------------------------------------------
    static String slotValue(QsoRecord q, String slot) {
        return switch (slot) {
            case "field1" -> q.getContestField1();
            case "field2" -> q.getContestField2();
            case "field3" -> q.getContestField3();
            case "field4" -> q.getContestField4();
            case "field5" -> q.getContestField5();
            default -> null;
        };
    }
    /** Load a logged QSO into the entry form; Enter then saves it back in place, Esc drops the edit. */
    static void loadForEdit(QsoRecord q) {
        ContestPlugin plugin = ContestState.plugin();
        if (plugin == null || fieldControls == null) return;
        clearForm();
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1 || "band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;
            Control c = fieldControls.get(fd.getId());
            if (c == null) continue;
            String v = switch (fd.getId()) {
                case "callsign" -> q.getCallsign();
                case "rst_rcvd", "rst_received" -> q.getRstReceived();
                case "serial_rcvd", "serial_received" -> q.getSerialReceived();
                default -> { String slot = plugin.fieldSlotColumn(fd.getId()); yield slot == null ? null : slotValue(q, slot); }
            };
            setControl(c, v == null ? "" : v);
        }
        if (q.getBand() != null) bandF.setText(q.getBand());
        if (q.getMode() != null) modeF.setText(q.getMode());
        editingRec = q;
        if (editL != null) editL.setText("\u270e EDITING " + q.getCallsign() + (q.getDateTimeUtc() == null ? "" : "  (" + q.getDateTimeUtc().format(HMS).substring(0, 5) + "Z)")
                + " \u2014 Enter saves the change, Esc cancels");
        focus("callsign");
    }
    /** True when {@code contestId} is the contest on the contest page now: its QSOs can be edited and erased from the log browser. */
    static boolean isActiveContest(String contestId) {
        ContestPlugin p = ContestState.plugin();
        return p != null && contestId != null && contestId.equals(p.getContestId());
    }
    /** The log browser's Edit: load {@code q} into the contest page's entry form. False when the contest page has not been opened yet. */
    static boolean editFromBrowser(QsoRecord q) {
        if (fieldControls == null || !isActiveContest(q.getContestId())) return false;
        loadForEdit(q);
        return true;
    }
    /** The log browser's Delete: same confirmation, dupe re-check, N1MM and network notices as Erase on the contest page. */
    static void eraseFromBrowser(Node source, QsoRecord q, Runnable after) { eraseQso(source, q, after); }
    static void eraseQso(Node source, QsoRecord q) { eraseQso(source, q, null); }
    static void eraseQso(Node source, QsoRecord q, Runnable after) {
        Confirm.show(source, "Erase this QSO?",
                q.getCallsign() + "  " + z(q.getBand()) + " " + z(q.getMode())
                        + (q.getDateTimeUtc() == null ? "" : "  " + q.getDateTimeUtc().format(HMS).substring(0, 5) + "Z")
                        + "\nIt is removed from the contest log and the score. This cannot be undone.",
                new Confirm.Choice("Keep it", true, null),
                new Confirm.Choice("Erase", false, () -> {
                    try {
                        ContestQsoDao.getInstance().delete(q.getId());
                        com.ars.fx.data.N1mmBroadcast.contactDeleted(ContestState.plugin(), q);
                        if (editingRec != null && editingRec.getId() == q.getId()) clearForm();
                        ContestPlugin p = ContestState.plugin();
                        if (p != null) reflagDupes(p);      // erasing the first contact can turn a later "dupe" into a valid QSO
                        ContestState.recompute();
                        if (after != null) after.run();
                    } catch (Exception e) { setB4("Erase failed: " + e.getMessage(), "dupe"); }
                }));
    }
    /** Re-decide which QSOs are dupes, oldest first, after one was edited or erased. */
    static void reflagDupes(ContestPlugin plugin) {
        try {
            List<QsoRecord> all = new ArrayList<>(ContestQsoDao.getInstance().fetchByContest(plugin.getContestId()));
            for (QsoRecord q : com.ars.fx.data.DupeFlags.recompute(plugin, all)) ContestQsoDao.getInstance().updateDerived(q.getId(), q.getPoints(), q.isDupe());
        } catch (Exception e) { System.err.println("[contest] dupe re-check failed: " + e.getMessage()); }
    }

    static void setSlot(QsoRecord r, String slot, String v) {
        switch (slot) {
            case "field1" -> r.setContestField1(v);
            case "field2" -> r.setContestField2(v);
            case "field3" -> r.setContestField3(v);
            case "field4" -> r.setContestField4(v);
            case "field5" -> r.setContestField5(v);
            default -> {}
        }
    }

    static void updateDupe(ContestPlugin plugin) { updateDupe(plugin, true); }

    /** @param fromCall true when the call, band or mode changed (may refill the exchange from an earlier QSO); false for an exchange edit */
    static void updateDupe(ContestPlugin plugin, boolean fromCall) {
        String call = value("callsign");
        if (call == null || call.length() < 2) { setB4("New", ""); if (similarL != null) similarL.setText(""); if (memoryL != null) memoryL.setText(""); return; }
        final String typed = call.toUpperCase();
        final QsoRecord cand = candidate(plugin);                      // read the form here, on the FX thread
        final boolean ready = readyForMult(plugin);
        Thread t = new Thread(() -> {
            try {
                List<QsoRecord> prior = ContestQsoDao.getInstance().findByCallsign(plugin.getContestId(), typed);
                boolean dupe = ContestScorer.isDupe(plugin, cand, prior);
                String similar = similarCalls(plugin, typed);
                boolean newMult = !dupe && ready && MultCheck.isNewMult(plugin, ContestState.qsos(), cand, ContestState.ctx());
                QsoRecord last = null;
                for (QsoRecord q : prior) if (last == null || (q.getDateTimeUtc() != null && last.getDateTimeUtc() != null && q.getDateTimeUtc().isAfter(last.getDateTimeUtc()))) last = q;
                final QsoRecord earlier = last;
                final java.util.Map<String, String> hist = earlier == null && fromCall ? com.ars.fx.data.CallHistory.get(typed) : java.util.Map.of();
                Platform.runLater(() -> {
                    String star = newMult ? "   \u2605 NEW MULT" : "";
                    if (dupe) setB4("\u26d4 DUPE \u2014 " + typed, "dupe");
                    else if (!prior.isEmpty()) setB4("\u25d0 NEW BAND/MODE \u2014 " + typed + " (worked " + prior.size() + "\u00d7 elsewhere)" + star, newMult ? "new" : "b4");
                    else if (!similar.isEmpty()) setB4("\u25d0 POSSIBLE DUPE \u2014 " + typed + star, newMult ? "new" : "b4");
                    else if (typed.length() >= 3) setB4("\u2713 NEW \u2014 " + typed + star, "new");
                    else setB4("New", "");
                    if (similarL != null) similarL.setText(similar.isEmpty() ? "" : "POSSIBLE DUPES:  " + similar);
                    if (fromCall && earlier != null && editingRec == null) prefillFromEarlier(plugin, earlier);
                    else if (fromCall && !hist.isEmpty() && editingRec == null && prefillFromHistory(plugin, typed, hist)) { /* filled */ }
                    else if (fromCall && memoryL != null) memoryL.setText("");
                });
            } catch (Exception ignored) {}
        }, "contest-dupe");
        t.setDaemon(true); t.start();
    }

    /** Exchange memory: a station you have worked before usually sends the same exchange (SS precedence / check / section, a state, a zone), so fill
     *  in the blank received fields from that QSO. Serial numbers and reports are never copied. Nothing is overwritten. */
    static void prefillFromEarlier(ContestPlugin plugin, QsoRecord earlier) {
        boolean anyTyped = false;
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1 || isNotExchange(fd.getId())) continue;
            if (!value(fd.getId()).isBlank()) anyTyped = true;
        }
        if (anyTyped) return;                                          // the operator already started typing: leave it alone
        int filled = 0;
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1 || isNotExchange(fd.getId())) continue;
            String slot = plugin.fieldSlotColumn(fd.getId());
            String v = slot == null ? null : slotValue(earlier, slot);
            Control c = fieldControls.get(fd.getId());
            if (v != null && !v.isBlank() && c != null) { setControl(c, v); filled++; }
        }
        if (memoryL != null) memoryL.setText(filled == 0 ? "" : "Exchange filled in from your earlier QSO with " + earlier.getCallsign() + " \u2014 check it before you log.");
    }
    /**
     * Call history: a station never worked in this contest usually sends what it sent before (its state, section, zone, name).
     * Blank received fields are filled from the history, but only with a value the contest accepts for that field, and only if the
     * operator has not started typing. @return true when at least one field was filled
     */
    static boolean prefillFromHistory(ContestPlugin plugin, String call, java.util.Map<String, String> hist) {
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1 || isNotExchange(fd.getId())) continue;
            if (!value(fd.getId()).isBlank()) return false;
        }
        int filled = 0;
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1 || fd.isConstant() || isNotExchange(fd.getId())) continue;
            Control c = fieldControls.get(fd.getId());
            if (c == null) continue;
            for (String key : com.ars.fx.data.CallHistory.keysForField(plugin, fd.getId())) {         // a QSO party county before the state
                String v = hist.get(key);
                if (v == null || !acceptable(plugin, fd, v)) continue;
                setControl(c, v); filled++; break;
            }
        }
        if (memoryL != null && filled > 0) memoryL.setText("Exchange filled in from the call history for " + call + " \u2014 check it before you log.");
        return filled > 0;
    }
    /** Would the contest take this value in this field? Anything short of a clean pass is not used. */
    static boolean acceptable(ContestPlugin plugin, FieldDef fd, String v) {
        return ContestValidation.check(plugin, fd, v, true).level() == ContestValidation.Level.OK;
    }
    static boolean isNotExchange(String id) {
        return id.equals("callsign") || id.equals("band") || id.equals("mode") || id.startsWith("rst") || id.startsWith("serial");
    }

    /** Logged calls that look like what is being typed: they share any three contiguous characters with it (KC8RHC finds KC8, C8R, 8RH and RHC),
     *  best match first. Other than an exact match; at most 8, with band and mode. */
    static String similarCalls(ContestPlugin plugin, String typed) {
        if (typed.length() < 3) return "";
        java.util.Map<String, String> found = new java.util.HashMap<>();
        java.util.Map<String, Integer> score = new java.util.HashMap<>();
        try {
            for (QsoRecord q : ContestQsoDao.getInstance().fetchByContest(plugin.getContestId())) {
                String c = q.getCallsign() == null ? "" : q.getCallsign().toUpperCase();
                if (c.isEmpty() || c.equals(typed) || q.isDupe()) continue;
                int sc = com.ars.fx.data.CallSimilarity.score(typed, c);
                if (sc > 0) { found.merge(c, (z(q.getBand()) + " " + z(q.getMode())).trim(), (a, b) -> a); score.put(c, sc); }
            }
        } catch (Exception ignored) {}
        List<String> calls = new ArrayList<>(found.keySet());
        calls.sort(java.util.Comparator.<String>comparingInt(c -> -score.get(c)).thenComparing(java.util.Comparator.naturalOrder()));
        List<String> out = new ArrayList<>();
        for (String c : calls) {
            if (out.size() >= 8) break;
            String bm = found.get(c);
            out.add(bm.isEmpty() ? c : c + " (" + bm + ")");
        }
        return String.join(",  ", out);
    }
    static final String CALL_STYLE = "-fx-font-size:20px;-fx-font-weight:bold;";
    static javafx.animation.Timeline dupeBlink;
    /** A dupe turns the callsign red and blinking; anything else puts it back. */
    static void blinkCall(boolean dupe) {
        if (dupeBlink != null) { dupeBlink.stop(); dupeBlink = null; }
        Control c = fieldControls == null ? null : fieldControls.get("callsign");
        if (c == null) return;
        c.setStyle(CALL_STYLE);
        if (!dupe) return;
        final String red = CALL_STYLE + "-fx-text-fill:#ff3b3b;";
        c.setStyle(red);
        dupeBlink = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(0), e -> c.setStyle(red)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(450), e -> c.setStyle(CALL_STYLE + "-fx-text-fill:transparent;")),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(900)));
        dupeBlink.setCycleCount(javafx.animation.Animation.INDEFINITE);
        dupeBlink.play();
    }
    static void setB4(String text, String cls) {
        if (b4 == null) return;
        if (!text.startsWith("\u25b6")) blinkCall("dupe".equals(cls) && text.startsWith("\u26d4 DUPE"));   // a sent macro leaves it as it was
        b4.setText(text);
        b4.getStyleClass().removeAll("dupe", "b4", "new");
        if (!cls.isEmpty()) b4.getStyleClass().add(cls);
    }

    // ---- validation ------------------------------------------------------------
    static void showProblem(String owner, String msg) { problemOwner = owner; if (problemL != null) problemL.setText(msg); }
    static void clearProblem(String owner) { if (owner.equals(problemOwner)) showProblem("", ""); }
    static void mark(Control c, ContestValidation.Result r) {
        c.getStyleClass().removeAll("invalid", "warnfield");
        if (r.level() == ContestValidation.Level.HARD) c.getStyleClass().add("invalid");
        else if (r.level() == ContestValidation.Level.SOFT) c.getStyleClass().add("warnfield");
    }
    static void liveCheck(ContestPlugin plugin, FieldDef fd, Control c, boolean complete) {
        ContestValidation.Result r = ContestValidation.check(plugin, fd, value(fd.getId()), complete);
        mark(c, r);
        if (r.level() == ContestValidation.Level.HARD || (complete && r.level() == ContestValidation.Level.SOFT)) showProblem(fd.getId(), r.message());
        else clearProblem(fd.getId());
    }
    /** Hard problems stop the QSO; "not recognised" ones need a second Enter. */
    static boolean validateEntry(ContestPlugin plugin) {
        List<String> soft = new ArrayList<>();
        for (FieldDef fd : plugin.getEntryFields()) {
            Control c = fieldControls.get(fd.getId());
            if (c == null) continue;
            ContestValidation.Result r = ContestValidation.check(plugin, fd, value(fd.getId()), true);
            mark(c, r);
            if (r.blocks()) { showProblem(fd.getId(), r.message()); c.requestFocus(); return false; }
            if (r.level() == ContestValidation.Level.SOFT) soft.add(r.message());
        }
        if (!soft.isEmpty() && !softArmed) {
            softArmed = true;
            showProblem("soft", String.join(".  ", soft) + ".  Press Enter again to log it anyway.");
            return false;
        }
        return true;
    }

    static void clearForm() {
        for (Map.Entry<String, Control> en : fieldControls.entrySet()) {
            if (keepIds.contains(en.getKey())) continue;
            Control c = en.getValue();
            if (c instanceof TextField t) t.clear();
            else if (c instanceof ComboBox<?> cb) cb.getSelectionModel().clearSelection();
        }
        serial = ContestState.nextSerial();
        if (serialV != null) serialV.setText(String.valueOf(serial));
        if (ContestState.plugin() != null) refreshSend(ContestState.plugin());
        if (similarL != null) similarL.setText("");
        if (scpBox != null) scpBox.update("");
        if (memoryL != null) memoryL.setText("");
        for (Control c : fieldControls.values()) c.getStyleClass().removeAll("invalid", "warnfield");
        editingRec = null; if (editL != null) editL.setText("");
        softArmed = false; showProblem("", "");
        setB4("New", "");
        focus("callsign");
    }

    // ---- ticker -----------------------------------------------------------
    static boolean tickerStarted = false;
    static void startTicker() {
        if (tickerStarted) return;
        tickerStarted = true;
        javafx.animation.Timeline tl = new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(3), e -> ContestState.recomputeIfChanged()));
        tl.setCycleCount(javafx.animation.Timeline.INDEFINITE); tl.play();
    }

    // ---- small helpers ----------------------------------------------------
    static Control controlFor(FieldDef fd) {
        if ("combo".equalsIgnoreCase(fd.getType()) && fd.getOptions() != null && !fd.getOptions().isEmpty()) {
            ComboBox<String> cb = new ComboBox<>(); cb.getItems().addAll(fd.getOptions());
            cb.getStyleClass().add("jhub-mini-field");
            double w = fd.getWidth() > 0 ? fd.getWidth() : 110; cb.setMinWidth(w); cb.setPrefWidth(w);
            return cb;
        }
        TextField t = new TextField(); if (fd.getLabel() != null) t.setPromptText(fd.getLabel());
        t.getStyleClass().add("jhub-mini-field"); if ("callsign".equals(fd.getId())) t.getStyleClass().add("call");
        double w = fd.getWidth() > 0 ? fd.getWidth() : 96; t.setMinWidth(w); t.setPrefWidth(w);
        return Shell.caps(t);   // exchange fields are upper case (stored upper case in the DB)
    }
    static String value(String id) {
        Control c = fieldControls.get(id);
        if (c instanceof TextField t) return t.getText() == null ? "" : t.getText().trim();
        if (c instanceof ComboBox<?> cb) { Object v = cb.getValue(); return v == null ? "" : v.toString().trim(); }
        return "";
    }
    static void focus(String id) { Control c = fieldControls.get(id); if (c != null) c.requestFocus(); }

    /** Fill an exchange field from a clicked map region, then focus the call field. Called by ContestMapPane. */
    @SuppressWarnings("unchecked")
    public static void fillField(String fieldId, String value) {
        if (fieldControls == null || fieldId == null) return;
        Control c = fieldControls.get(fieldId);
        if (c instanceof TextField t) t.setText(value);
        else if (c instanceof ComboBox<?> cb) ((ComboBox<String>) cb).setValue(value);
        Control call = fieldControls.get("callsign"); if (call != null) call.requestFocus();
    }

    static TextField tf(String prompt, String init, double w) {
        TextField t = new TextField(init == null ? "" : init);
        if (prompt != null && !prompt.isEmpty()) t.setPromptText(prompt);
        t.getStyleClass().add("jhub-mini-field");
        if (w > 0) { t.setMinWidth(w); t.setPrefWidth(w); }
        return Shell.caps(t);
    }
    static VBox g(String label, Node field) {
        VBox v = new VBox(5, lbl(label, "jl-flabel"), field); v.setAlignment(Pos.TOP_LEFT);
        return v;
    }
    static Label col(String s, double w) { Label l = lbl(s, "jl-logc-h"); if (w > 0) { l.setMinWidth(w); l.setPrefWidth(w); } else { HBox.setHgrow(l, Priority.ALWAYS); l.setMaxWidth(Double.MAX_VALUE); } return l; }
    static Label cell(String s, double w) { Label l = lbl(s == null ? "" : s, "jl-logc"); if (w > 0) { l.setMinWidth(w); l.setPrefWidth(w); } else { HBox.setHgrow(l, Priority.ALWAYS); l.setMaxWidth(Double.MAX_VALUE); } return l; }

    static String z(String s) { return s == null ? "" : s; }
    static String nowUtc() { return java.time.LocalTime.now(ZoneOffset.UTC).format(HMS); }
    static String normBand(String b) { if (b == null) return ""; b = b.trim().toLowerCase(); return b.matches("\\d+") ? b + "m" : b; }
    static String bandFor(long hz) { String b = com.ars.fx.data.JLogDb.bandFor(String.format(java.util.Locale.US, "%.3f", hz / 1e6)); return b == null ? "20m" : b; }
}
