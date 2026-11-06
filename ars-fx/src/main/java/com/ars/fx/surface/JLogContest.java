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

import static com.ars.fx.shell.Shell.lbl;

/**
 * J-Log — Contest mode. Pick a contest (plugin), log QSOs with the plugin's
 * exchange, live dupe + scoring via the j-log-engine, recent-log table, and
 * Cabrillo export. Multiplier trackers + maps come in later phases.
 */
public final class JLogContest {
    private JLogContest() {}

    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    // value controls for the active plugin's entry fields, keyed by field id
    private static Map<String, Control> fieldControls;
    private static TextField bandF, modeF;
    private static Label serialV;
    private static Label sendV;                          // "what to send" line
    private static QsoRecord editingRec;                 // the logged QSO being corrected (null = logging a new one)
    private static Label editL;                          // "editing ..." banner
    private static boolean callGivesMult;                // this contest's multiplier can be told from the call alone
    private static Label memoryL;                        // "exchange filled from your earlier QSO"
    private static final java.util.Map<String, String> spotMemo = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile List<QsoRecord> logSnap;     // the contest log as of the last change, for spot tags
    private static Label problemL;                       // what is wrong with the entry (invalid section, missing field...)
    private static String problemOwner = "";             // field the message is about
    private static boolean softArmed;                    // a "not recognised" warning was shown: the next Enter logs anyway
    private static Label similarL;                       // possible-duplicate callsigns
    private static com.ars.fx.surface.contest.ScpBox scpBox;   // Super Check Partial strip under the call
    private static final java.util.Set<String> keepIds = new java.util.HashSet<>();   // fields that survive Clear (sent + persistent)
    private static final java.util.Set<String> askedSent = new java.util.HashSet<>();  // contests whose missing exchange was asked this session
    private static int serial = 1;
    private static Label b4;                 // worked-before / dupe indicator
    private static StackPane host;

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
        com.ars.fx.data.N1mmBroadcast.appInfo(plugin);                                   // tell listening programs which contest this is

        Region dock = Shell.dock("log");
        HBox top = Shell.topBar("log", "log", "J-Log", "Contest · " + plugin.getContestName(), scoreStats(), nowUtc());

        // One-screen operating position: entry + last QSOs on the left, DX cluster over the contest panes on the right.
        VBox left = new VBox(8, toolbar(plugin), new com.ars.fx.surface.contest.NetStrip(), LogDbBar.build(true), entryPane(plugin), recentLogPane());
        left.setStyle("-fx-background-color:-ars-bg;");
        ScrollPane sp = new ScrollPane(left); sp.setFitToWidth(true); sp.setFitToHeight(true);   // scrolls only if the window is too short
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;");
        sp.setMinWidth(620); sp.setPrefWidth(780); HBox.setHgrow(sp, Priority.NEVER);

        Region cluster = JHubDashboard.clusterCol();
        cluster.setMinWidth(380); cluster.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(cluster, Priority.ALWAYS);
        VBox right = new VBox(10, cluster);
        TabPane tabs = contestTabs(plugin);
        if (tabs != null) right.getChildren().add(tabs);
        right.setMinWidth(380); HBox.setHgrow(right, Priority.ALWAYS);
        HBox main = new HBox(14, sp, right); main.setFillHeight(true); main.setPadding(new Insets(0, 14, 10, 0));
        main.setStyle("-fx-background-color:-ars-bg;");

        // the right-hand rail keeps the station controls (rig, rotor); the contest panes live in the tabs
        Region rail = Shell.rail(Shell.leadDrawers().toArray(new Node[0]));
        host = new StackPane((Region) Shell.frame(dock, top, main, rail));

        ContestState.addListener(() -> Platform.runLater(JLogContest::refreshScoreStats));
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
    private static void maybeChooseDb(ContestPlugin plugin) {
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
    private static TabPane contestTabs(ContestPlugin plugin) {
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
    private static int[] rates() {
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
    private static VBox recentLogPane() {
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
    private static Region toolbar(ContestPlugin plugin) {
        Node ic = Shell.iconTile("log", "log", 16, "sx-dw-ic");
        Label name = lbl(plugin.getContestName(), "jhub-card-title");
        Label fmt = lbl(plugin.getExchangeFormat() == null ? "" : plugin.getExchangeFormat(), "jl-emeta");
        VBox ttl = new VBox(1, name, fmt);
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);

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

        Label translator = lbl("Translator", "jl-clearbtn"); translator.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(translator, "QSO phrases in other languages — follows the country of the callsign you are logging");
        translator.setOnMouseClicked(e -> {
            Control cf = fieldControls == null ? null : fieldControls.get("callsign");
            TranslatorWindow.show(translator, cf instanceof TextField t ? t.getText() : "");
        });
        Label times = lbl("Contest times\u2026", "jl-clearbtn"); times.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(times, "Set when this contest starts and ends (UTC) to get a clock with time left and off time");
        times.setOnMouseClicked(e -> editTimes(times, plugin));
        Label layoutBtn = lbl("Layout\u2026", "jl-clearbtn"); layoutBtn.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(layoutBtn, "Open this contest's layout file: a reminder note, the order of the tabs and of the exchange fields");
        layoutBtn.setOnMouseClicked(e -> openLayoutFile(plugin));
        Node chip = sideChip(plugin);
        HBox bar = new HBox(11, ic, ttl, gap, translator, normal, pick, times, layoutBtn, export);
        if (chip != null) bar.getChildren().add(2, chip);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(14, 18, 6, 18));
        return bar;
    }

    // ---- entry pane: band/mode + plugin fields + log table -----------------
    private static VBox entryPane(ContestPlugin plugin) {
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
                cf.setMinWidth(210); cf.setPrefWidth(210); cf.setStyle("-fx-font-size:20px;-fx-font-weight:bold;");   // the call is what you look at
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
        body.getChildren().add(wrapRow("RECEIVED", rcvd));
        body.getChildren().add(sendRow(plugin));

        // worked-before / dupe indicator + actions
        b4 = lbl("New", "jl-eyebrow"); b4.setStyle("-fx-font-size:20px;-fx-font-weight:bold;");   // DUPE / NEW / NEW BAND, readable from across the desk
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        Label clear = lbl("Clear", "jl-clearbtn"); clear.setStyle("-fx-cursor:hand;");
        Label logBtn = lbl("Log QSO  ⏎", "jl-logbtn"); logBtn.setStyle("-fx-cursor:hand;");
        HBox actions = new HBox(12, b4, gap, clear, logBtn); actions.setAlignment(Pos.CENTER_LEFT);
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
        similarL.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:-ars-warn;-fx-font-family:'JetBrains Mono';"
                + "-fx-background-color:-ars-surface-2;-fx-border-color:-ars-warn;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 12 8 12;");
        similarL.managedProperty().bind(similarL.textProperty().isNotEmpty());   // no empty box while there is nothing to warn about
        similarL.visibleProperty().bind(similarL.textProperty().isNotEmpty());
        body.getChildren().add(similarL);
        scpBox = new com.ars.fx.surface.contest.ScpBox(c -> {
            if (fieldControls.get("callsign") instanceof TextField cf) { cf.setText(c); cf.requestFocus(); cf.positionCaret(c.length()); }
        });
        body.getChildren().add(scpBox);
        body.getChildren().add(memoryL);

        // F1–F8 station macros (shared with normal log + dashboard)
        body.getChildren().add(MacroBar.sendAsRow(modeF));
        java.util.function.IntConsumer fireMacro = MacroBar.fire(() -> modeF.getText(), () -> value("callsign"), msg -> setB4("▶ " + msg, "new"));
        body.getChildren().add(MacroBar.row(fireMacro));
        installFKeys(body, fireMacro);

        Runnable doLog = () -> { if (logQso(plugin)) clearForm(); };   // recompute → listeners refresh log + trackers
        logBtn.setOnMouseClicked(e -> doLog.run());
        clear.setOnMouseClicked(e -> clearForm());

        // keyboard flow: Enter in any field logs (a missing required field takes the focus instead), Esc clears
        for (Control c : fieldControls.values()) {
            if (c instanceof TextField t) t.setOnAction(e -> doLog.run());
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
        bandF.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) clearForm(); });
        modeF.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) clearForm(); });

        // live validation: red border the moment a value can never be valid, amber if merely unrecognised; the full check on leaving the field
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1 || "band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;
            if (fieldControls.get(fd.getId()) instanceof TextField t) {
                t.textProperty().addListener((o, a, b) -> { softArmed = false; liveCheck(plugin, fd, t, false); });
                t.focusedProperty().addListener((o, was, now) -> { if (!now) liveCheck(plugin, fd, t, true); });
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
        if (callC instanceof TextField cf) cf.textProperty().addListener((o, a, b) -> { dupeDelay.playFromStart(); if (scpBox != null) scpBox.update(b); });
        bandF.textProperty().addListener((o, a, b) -> dupeDelay.playFromStart());
        modeF.textProperty().addListener((o, a, b) -> dupeDelay.playFromStart());

        return body;
    }

    /** The mode a contest opens in: the one it pins, else the one its name says (SSB / CW / RTTY / digital), else the rig's, else CW. */
    private static String defaultMode(ContestPlugin plugin, com.ars.fx.data.RigClient.State rs) {
        String m = SentExchange.contestMode(plugin);
        if (m != null) return m;
        String own = ContestLayout.resolve(plugin).modeOrNull();                           // the contest's own layout
        if (own != null) return own;
        String pref = HubConfig.get("contest.defaultMode", "Auto");                    // Setup: Auto / SSB / CW
        if (pref.equals("SSB") || pref.equals("CW")) return pref;
        return rs.connected() && rs.mode() != null && !rs.mode().isBlank() ? rs.mode() : "SSB";   // Auto: follow the rig, else SSB
    }

    // ---- "SEND" line: exactly what to say / key, built from Setup ----------------
    private static String sendText(ContestPlugin plugin) {
        List<String> parts = new ArrayList<>();
        if (fieldControls.containsKey("serial_rcvd")) parts.add(String.valueOf(serial));      // contests that exchange a serial number
        for (FieldDef fd : SentExchange.sentFields(plugin)) {
            String v = value(fd.getId());
            parts.add(v.isEmpty() ? "?" : v);
            if (fd.getId().equals("prec_sent")) parts.add(HubConfig.call().toUpperCase());    // Sweepstakes: NR PREC CALL CHK SECT
        }
        return parts.isEmpty() ? "-" : String.join("   ", parts);
    }
    private static Region sendRow(ContestPlugin plugin) {
        Label tag = lbl("SEND", "jl-flabel"); tag.setMinWidth(70);
        sendV = lbl(sendText(plugin), "jhub-rig-fq");
        sendV.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:-ars-ok;");
        Label edit = lbl("Edit", "jl-clearbtn"); edit.setStyle("-fx-cursor:hand;");
        edit.setOnMouseClicked(e -> SentExchange.ask(edit, plugin, SentExchange.sentFields(plugin), modeF.getText(), () -> reloadSent(plugin)));
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox row = new HBox(10, tag, sendV, gap, edit); row.setAlignment(Pos.CENTER_LEFT);
        // the report follows the mode (59 on phone, 599 otherwise) and the line follows the serial / any edit
        modeF.textProperty().addListener((o, a, b) -> reloadSent(plugin));
        return row;
    }
    /** Re-read every sent field (Setup, saved answers, mode-dependent report) into its control and redraw the SEND line. */
    private static void reloadSent(ContestPlugin plugin) {
        for (FieldDef fd : SentExchange.sentFields(plugin)) {
            Control c = fieldControls.get(fd.getId());
            if (c != null) setControl(c, SentExchange.value(plugin, fd, modeF.getText()));
        }
        refreshSend(plugin);
    }
    private static void refreshSend(ContestPlugin plugin) { if (sendV != null) sendV.setText(sendText(plugin)); }
    @SuppressWarnings("unchecked")
    private static void setControl(Control c, String v) {
        if (c instanceof TextField t) t.setText(v == null ? "" : v);
        else if (c instanceof ComboBox<?> cb) ((ComboBox<String>) cb).setValue(v == null || v.isEmpty() ? null : v);
    }
    /** Ask for any part of the exchange Setup does not know, once per contest per session. */
    private static void askSentIfMissing(ContestPlugin plugin) {
        List<FieldDef> miss = SentExchange.missing(plugin, modeF.getText());
        if (miss.isEmpty() || !askedSent.add(plugin.getContestId())) return;
        Platform.runLater(() -> SentExchange.ask(host, plugin, miss, modeF.getText(), () -> reloadSent(plugin)));
    }

    private static HBox wrapRow(String tag, List<Node> cells) {
        Label t = lbl(tag, "jl-flabel"); t.setMinWidth(70);
        HBox fields = new HBox(12); fields.getChildren().addAll(cells); fields.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setHgrow(fields, Priority.ALWAYS);
        HBox row = new HBox(10, t, fields); row.setAlignment(Pos.BOTTOM_LEFT);
        return row;
    }

    // ---- save -------------------------------------------------------------
    /** "NEW MULT" for a spot that would add a multiplier, else "". Remembered until the log changes. */
    private static String spotTag(ContestPlugin plugin, com.ars.fx.data.ClusterClient.Spot spot) {
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
    /** Fill {@code cand}'s multiplier field from the call history. False when there is nothing usable (then no tag is better than a wrong one). */
    private static boolean fromHistory(ContestPlugin plugin, QsoRecord cand) {
        var mm = plugin.getMultiplierModel();
        if (mm == null || mm.getField() == null) return false;
        String key = com.ars.fx.data.CallHistory.keyForField(mm.getField());
        String v = key == null ? null : com.ars.fx.data.CallHistory.get(cand.getCallsign()).get(key);
        String slot = plugin.fieldSlotColumn(mm.getField());
        if (v == null || slot == null) return false;
        for (FieldDef fd : plugin.getEntryFields())
            if (mm.getField().equals(fd.getId())) { if (!acceptable(plugin, fd, v)) return false; setSlot(cand, slot, v); return true; }
        return false;
    }

    /** F1-F8 key the macros, for as long as this page is on screen (and never after: the keys are handed back when the page leaves the scene). */
    private static void installFKeys(Node anchor, java.util.function.IntConsumer fire) {
        javafx.scene.input.KeyCode[] fk = {javafx.scene.input.KeyCode.F1, javafx.scene.input.KeyCode.F2, javafx.scene.input.KeyCode.F3, javafx.scene.input.KeyCode.F4,
                javafx.scene.input.KeyCode.F5, javafx.scene.input.KeyCode.F6, javafx.scene.input.KeyCode.F7, javafx.scene.input.KeyCode.F8};
        List<javafx.scene.input.KeyCodeCombination> mine = new ArrayList<>();
        anchor.sceneProperty().addListener((o, oldS, s) -> {
            if (oldS != null) for (var k : mine) oldS.getAccelerators().remove(k);
            mine.clear();
            if (s == null) return;
            for (int i = 0; i < com.ars.fx.data.MacroConfig.count() && i < fk.length; i++) {
                final int idx = i;
                var k = new javafx.scene.input.KeyCodeCombination(fk[i]);
                mine.add(k);
                s.getAccelerators().put(k, () -> fire.accept(idx));
            }
        });
    }

    /** Copy what is on the form (call, band, mode and every exchange field) into a record. */
    private static void fillFromForm(ContestPlugin plugin, QsoRecord r) {
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
    private static QsoRecord candidate(ContestPlugin plugin) {
        QsoRecord r = new QsoRecord();
        r.setContestId(plugin.getContestId());
        r.setDateTimeUtc(LocalDateTime.now(ZoneOffset.UTC));
        fillFromForm(plugin, r);
        return r;
    }
    /** Enough typed to ask "is it a new multiplier?": the multiplier field filled in and valid, or (when the call gives it) a call. */
    private static boolean readyForMult(ContestPlugin plugin) {
        if (value("callsign").length() < 3 || bandF.getText().isBlank() || modeF.getText().isBlank()) return false;
        var mm = plugin.getMultiplierModel();
        if (mm == null || mm.getField() == null) return true;
        FieldDef fd = null;
        for (FieldDef f : plugin.getEntryFields()) if (mm.getField().equals(f.getId())) fd = f;
        if (fd == null || value(fd.getId()).isBlank()) return !callGivesMult;
        var lv = ContestValidation.check(plugin, fd, value(fd.getId()), true).level();
        return lv == ContestValidation.Level.OK || lv == ContestValidation.Level.SOFT;
    }

    private static boolean logQso(ContestPlugin plugin) { return logQso(plugin, false); }

    /** @param dupeApproved the operator has already said "log this dupe anyway" */
    private static boolean logQso(ContestPlugin plugin, boolean dupeApproved) {
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
            }
            fillFromForm(plugin, r);
            List<QsoRecord> prior = new ArrayList<>(ContestQsoDao.getInstance().findByCallsign(cid, call));
            if (editing) prior.removeIf(x -> x.getId() == r.getId());
            boolean dupe = ContestScorer.isDupe(plugin, r, prior);
            if (dupe && !dupeApproved) {   // a dupe is never logged without the operator saying so
                setB4("\u26d4 DUPE \u2014 " + call.toUpperCase(), "dupe");
                Confirm.show(b4, "Dupe \u2014 " + call.toUpperCase(),
                        call.toUpperCase() + " is already in the log for " + normBand(bandF.getText()) + " " + modeF.getText().trim().toUpperCase()
                                + ". A dupe scores nothing. Log it anyway?",
                        new Confirm.Choice("Don't log it", true, () -> focus("callsign")),
                        new Confirm.Choice("Log the dupe", false, () -> { if (logQso(plugin, true)) clearForm(); }));
                return false;
            }
            r.setDupe(dupe);
            r.setPoints(ContestScorer.points(plugin, r, ContestState.ctx()));
            // a new multiplier is worked out before the QSO is in the log (only when something is listening: it scores the log twice)
            boolean newMult = !editing && !dupe && com.ars.fx.data.N1mmBroadcast.enabled() && MultCheck.isNewMult(plugin, ContestState.qsos(), r, ContestState.ctx());
            if (editing) { ContestQsoDao.getInstance().update(r); editingRec = null; reflagDupes(plugin); com.ars.fx.data.N1mmBroadcast.contactEdited(plugin, r, oldCall); }
            else { ContestQsoDao.getInstance().insert(r); serial++; com.ars.fx.data.N1mmBroadcast.contactAdded(plugin, r, newMult); }
            ContestState.recompute();
            final QsoRecord logged = r;
            Thread lt = new Thread(() -> com.ars.fx.data.CallHistory.learn(plugin, logged), "call-history-learn");
            lt.setDaemon(true); lt.start();
            return true;
        } catch (Exception e) {
            System.err.println("[contest] log failed: " + e.getMessage());
            return false;
        }
    }

    // ---- edit / erase a logged QSO ------------------------------------------------
    private static String slotValue(QsoRecord q, String slot) {
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
    private static void loadForEdit(QsoRecord q) {
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
    private static void eraseQso(Node source, QsoRecord q) {
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
                    } catch (Exception e) { setB4("Erase failed: " + e.getMessage(), "dupe"); }
                }));
    }
    /** Re-decide which QSOs are dupes, oldest first, after one was edited or erased. */
    private static void reflagDupes(ContestPlugin plugin) {
        try {
            List<QsoRecord> all = new ArrayList<>(ContestQsoDao.getInstance().fetchByContest(plugin.getContestId()));
            for (QsoRecord q : com.ars.fx.data.DupeFlags.recompute(plugin, all)) ContestQsoDao.getInstance().updateDerived(q.getId(), q.getPoints(), q.isDupe());
        } catch (Exception e) { System.err.println("[contest] dupe re-check failed: " + e.getMessage()); }
    }

    private static void setSlot(QsoRecord r, String slot, String v) {
        switch (slot) {
            case "field1" -> r.setContestField1(v);
            case "field2" -> r.setContestField2(v);
            case "field3" -> r.setContestField3(v);
            case "field4" -> r.setContestField4(v);
            case "field5" -> r.setContestField5(v);
            default -> {}
        }
    }

    private static void updateDupe(ContestPlugin plugin) { updateDupe(plugin, true); }

    /** @param fromCall true when the call, band or mode changed (may refill the exchange from an earlier QSO); false for an exchange edit */
    private static void updateDupe(ContestPlugin plugin, boolean fromCall) {
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
    private static void prefillFromEarlier(ContestPlugin plugin, QsoRecord earlier) {
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
    private static boolean prefillFromHistory(ContestPlugin plugin, String call, java.util.Map<String, String> hist) {
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1 || isNotExchange(fd.getId())) continue;
            if (!value(fd.getId()).isBlank()) return false;
        }
        int filled = 0;
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1 || fd.isConstant() || isNotExchange(fd.getId())) continue;
            String key = com.ars.fx.data.CallHistory.keyForField(fd.getId());
            String v = key == null ? null : hist.get(key);
            Control c = fieldControls.get(fd.getId());
            if (v == null || c == null || !acceptable(plugin, fd, v)) continue;
            setControl(c, v); filled++;
        }
        if (memoryL != null && filled > 0) memoryL.setText("Exchange filled in from the call history for " + call + " \u2014 check it before you log.");
        return filled > 0;
    }
    /** Would the contest take this value in this field? Anything short of a clean pass is not used. */
    private static boolean acceptable(ContestPlugin plugin, FieldDef fd, String v) {
        return ContestValidation.check(plugin, fd, v, true).level() == ContestValidation.Level.OK;
    }
    private static boolean isNotExchange(String id) {
        return id.equals("callsign") || id.equals("band") || id.equals("mode") || id.startsWith("rst") || id.startsWith("serial");
    }

    /** Logged calls that look like what is being typed: they start with it, contain it, or are contained in it
     *  (a partial call, or one letter too many). Other than an exact match; at most 8, with band and mode. */
    private static String similarCalls(ContestPlugin plugin, String typed) {
        if (typed.length() < 3) return "";
        java.util.Map<String, String> found = new java.util.TreeMap<>();
        try {
            for (QsoRecord q : ContestQsoDao.getInstance().fetchByContest(plugin.getContestId())) {
                String c = q.getCallsign() == null ? "" : q.getCallsign().toUpperCase();
                if (c.isEmpty() || c.equals(typed) || q.isDupe()) continue;
                if (c.contains(typed) || (c.length() >= 3 && typed.contains(c)))
                    found.merge(c, (z(q.getBand()) + " " + z(q.getMode())).trim(), (a, b) -> a);
            }
        } catch (Exception ignored) {}
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> e : found.entrySet()) {
            if (out.size() >= 8) break;
            out.add(e.getValue().isEmpty() ? e.getKey() : e.getKey() + " (" + e.getValue() + ")");
        }
        return String.join(",  ", out);
    }
    private static void setB4(String text, String cls) {
        if (b4 == null) return;
        b4.setText(text);
        b4.getStyleClass().removeAll("dupe", "b4", "new");
        if (!cls.isEmpty()) b4.getStyleClass().add(cls);
    }

    // ---- validation ------------------------------------------------------------
    private static void showProblem(String owner, String msg) { problemOwner = owner; if (problemL != null) problemL.setText(msg); }
    private static void clearProblem(String owner) { if (owner.equals(problemOwner)) showProblem("", ""); }
    private static void mark(Control c, ContestValidation.Result r) {
        c.getStyleClass().removeAll("invalid", "warnfield");
        if (r.level() == ContestValidation.Level.HARD) c.getStyleClass().add("invalid");
        else if (r.level() == ContestValidation.Level.SOFT) c.getStyleClass().add("warnfield");
    }
    private static void liveCheck(ContestPlugin plugin, FieldDef fd, Control c, boolean complete) {
        ContestValidation.Result r = ContestValidation.check(plugin, fd, value(fd.getId()), complete);
        mark(c, r);
        if (r.level() == ContestValidation.Level.HARD || (complete && r.level() == ContestValidation.Level.SOFT)) showProblem(fd.getId(), r.message());
        else clearProblem(fd.getId());
    }
    /** Hard problems stop the QSO; "not recognised" ones need a second Enter. */
    private static boolean validateEntry(ContestPlugin plugin) {
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

    private static void clearForm() {
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

    // ---- score readout ----------------------------------------------------
    private static String[][] scoreStats() {
        ContestScore s = ContestState.score();
        String[][] tiles = new String[][]{
                {"QSOs", String.valueOf(s.qsoCount())},
                {"MULTS", String.valueOf(s.mults())},
                {"PTS", String.valueOf(s.points())},
                {"SCORE", String.format("%,d", s.score()), "accent"},
                {"10-MIN RATE", rates()[0] + "/h"},
                {"60-MIN RATE", rates()[1] + "/h"}};
        ContestPlugin p = ContestState.plugin();
        List<String[]> out = new ArrayList<>(java.util.Arrays.asList(tiles));
        String[] left = leftTile(p);
        if (left != null) out.add(left);
        String[] clock = clockTile(p);
        if (clock != null) out.add(clock);
        return out.toArray(new String[0][]);
    }

    /** "CONTEST CLOCK": time to the start / left, and off time so far; only once the operator has set the contest's times. */
    private static String[] clockTile(ContestPlugin p) {
        if (p == null) return null;
        com.ars.fx.data.ContestClock.Window w = com.ars.fx.data.ContestClock.window(p.getContestId());
        if (!w.valid()) return null;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        com.ars.fx.data.ContestClock.Status st = com.ars.fx.data.ContestClock.status(w, now);
        String text = st.text();
        if (st.phase() != com.ars.fx.data.ContestClock.Phase.BEFORE) {
            List<LocalDateTime> times = new ArrayList<>();
            for (QsoRecord q : ContestState.qsos()) if (!q.isDupe()) times.add(q.getDateTimeUtc());
            java.time.Duration off = com.ars.fx.data.ContestClock.offTime(times, w, now, java.time.Duration.ofMinutes(30));
            if (!off.isZero()) text += "  \u00b7  off " + com.ars.fx.data.ContestClock.span(off);
        }
        return new String[]{"CONTEST CLOCK", text};
    }

    /** The side of a state QSO party this operator is on - in-state (sends a county) or out-of-state (sends a state / province) - or a prompt to say. */
    private static Node sideChip(ContestPlugin plugin) {
        if (!QsoPartySide.isParty(plugin)) return null;
        String qth = QsoPartySide.sentQth(plugin), st = QsoPartySide.partyState(plugin);
        QsoPartySide.Side sd = QsoPartySide.side(plugin, qth);
        Label l = lbl(sd == null ? "Set your QTH \u203a" : QsoPartySide.label(sd) + (st.isEmpty() ? "" : " (" + st + ")") + " \u00b7 sending " + qth, "jl-eyebrow");
        String color = sd == null ? "-ars-warn" : sd == QsoPartySide.Side.IN_STATE ? "-ars-ok" : "-ars-accent";
        l.setStyle("-fx-cursor:hand;-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + color + ";-fx-border-color:" + color
                + ";-fx-border-radius:10;-fx-padding:3 10 3 10;");
        String tip = sd == null ? "Tell the app where you are: your county if you are in " + st + ", otherwise your state or province."
                : sd == QsoPartySide.Side.IN_STATE
                ? "You are in " + st + ": you send your county and work everyone - counties, other states and provinces, and DX. Click to change."
                : "You are outside " + st + ": you send your state or province and work only " + st + " stations (counties). Click to change.";
        com.ars.fx.shell.Tips.tip(l, tip);
        l.setOnMouseClicked(e -> SentExchange.ask(l, plugin, SentExchange.sentFields(plugin), modeF.getText(), () -> Shell.navigate("logc")));
        return l;
    }

    /** Make (if need be) and open the operator's layout file for this contest; the page picks it up the next time it is opened. */
    private static void openLayoutFile(ContestPlugin plugin) {
        try {
            List<String> tabs = new ArrayList<>();
            if (plugin.getRow2Panes() != null) for (var pd : plugin.getRow2Panes()) {
                String type = pd.getPaneType() == null ? "" : pd.getPaneType();
                if (type.equals("dupe_checker") || type.equals("worked_before")) continue;
                tabs.add(pd.getTitle() == null || pd.getTitle().isBlank() ? type : pd.getTitle());
            }
            List<String> ids = plugin.getEntryFields().stream().map(FieldDef::getId).filter(i -> !i.equals("band") && !i.equals("mode")).toList();
            java.nio.file.Path f = ContestLayout.ensureUserFile(plugin, tabs, ids);
            com.ars.fx.data.LinkOpener.openFile(f, () -> setB4("Layout file: " + f, "new"));
        } catch (Exception e) { setB4("Could not make the layout file: " + e.getMessage(), "dupe"); }
    }

    private static void editTimes(Node owner, ContestPlugin plugin) {
        com.ars.fx.data.ContestClock.Window w = com.ars.fx.data.ContestClock.window(plugin.getContestId());
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        TextField start = new TextField(w.start() == null ? "" : f.format(w.start())), end = new TextField(w.end() == null ? "" : f.format(w.end()));
        start.setPromptText("2026-11-28 00:00"); end.setPromptText("2026-11-30 00:00");
        Label msg = lbl("", "jl-emeta"); msg.setStyle("-fx-text-fill:-ars-err;");
        Label hint = lbl("Times are UTC. They are kept for this contest only, so enter them each year. Off time counts gaps of 30 minutes or more.", "jl-emeta");
        hint.setWrapText(true); hint.setMaxWidth(380);
        Label cancel = lbl("Cancel", "jl-clearbtn"), save = lbl("Save", "jl-logbtn"), clear = lbl("Clear", "jl-clearbtn");
        for (Label l : new Label[]{cancel, save, clear}) l.setStyle("-fx-cursor:hand;");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox btns = new HBox(14, clear, msg, gap, cancel, save); btns.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(8, lbl("Contest times \u2014 " + plugin.getContestName(), "jhub-card-title"), lbl("START (UTC)", "jl-flabel"), start,
                lbl("END (UTC)", "jl-flabel"), end, hint, btns);
        box.setPadding(new Insets(16)); box.setPrefWidth(420);
        javafx.scene.Scene scene = new javafx.scene.Scene(box);
        com.ars.fx.Theme.apply(scene, false); com.ars.fx.shell.Tips.install(scene);
        com.ars.fx.Themes.applyTo(box, "log");
        javafx.stage.Stage st = new javafx.stage.Stage(); st.setTitle("Contest times"); st.setScene(scene);
        st.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) st.initOwner(owner.getScene().getWindow());
        cancel.setOnMouseClicked(e -> st.close());
        clear.setOnMouseClicked(e -> { com.ars.fx.data.ContestClock.setWindow(plugin.getContestId(), null); st.close(); Shell.navigate("logc"); });
        save.setOnMouseClicked(e -> {
            LocalDateTime a = com.ars.fx.data.ContestClock.parse(start.getText()), b = com.ars.fx.data.ContestClock.parse(end.getText());
            if (a == null || b == null) { msg.setText("Use yyyy-MM-dd HH:mm"); return; }
            if (!b.isAfter(a)) { msg.setText("The end must be after the start"); return; }
            com.ars.fx.data.ContestClock.setWindow(plugin.getContestId(), new com.ars.fx.data.ContestClock.Window(a, b));
            st.close(); Shell.navigate("logc");                      // rebuild so the clock tile appears
        });
        st.show();
    }

    /** Label of the "multipliers still to work" tile, or null when the contest has no fixed set of multipliers (prefixes, DXCC). */
    private static String leftLabel(ContestPlugin p) {
        if (p == null || universe(p) == null) return null;
        if (p.getContestId() != null && p.getContestId().toUpperCase().startsWith("ARRL_SS")) return "TO CLEAN SWEEP";
        if (QsoPartySide.isParty(p) && QsoPartySide.side(p) != null) return QsoPartySide.side(p) == QsoPartySide.Side.IN_STATE ? "MULTS LEFT" : "COUNTIES LEFT";
        String t = p.getScoringRules() == null || p.getScoringRules().getMultiplierType() == null ? "" : p.getScoringRules().getMultiplierType();
        return switch (t) {
            case "sections" -> "SECTIONS LEFT";
            case "states" -> "STATES LEFT";
            case "zone_country", "zone_country_state" -> "ZONES LEFT";
            default -> "MULTS LEFT";
        };
    }
    /** The multipliers there are to collect: for a state QSO party, what this side of the party can work; else the contest's own list. */
    private static List<String> universe(ContestPlugin p) {
        if (QsoPartySide.isParty(p)) {
            QsoPartySide.Side sd = QsoPartySide.side(p);
            if (sd != null) { List<String> u = QsoPartySide.multUniverse(p, sd); if (u != null) return u; }
        }
        return com.ars.fx.surface.contest.TrackerFactory.multiplierUniverse(p);
    }
    private static String[] leftTile(ContestPlugin p) {
        String label = leftLabel(p);
        return label == null ? null : new String[]{label, leftText(p)};
    }
    /** "12 sections" / "CLEAN SWEEP!" - for contests that count multipliers per band, the ones left on the band in use. */
    private static String leftText(ContestPlugin p) {
        List<String> all = universe(p);
        if (all == null) return "";
        java.util.Set<String> worked = new java.util.HashSet<>();
        boolean perBand = p.getMultiplierModel() != null && p.getMultiplierModel().isPerBand();
        String band = bandF == null ? "" : normBand(bandF.getText());
        if (perBand && !band.isEmpty()) {
            for (Map.Entry<String, List<String>> e : ContestState.score().workedByBand().entrySet())
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(band)) for (String v : e.getValue()) worked.add(v.toUpperCase());
        } else {
            for (String v : ContestState.workedMults()) worked.add(v.toUpperCase());
        }
        int left = 0;
        for (String v : all) if (!worked.contains(v.toUpperCase())) left++;
        boolean sweep = p.getContestId() != null && p.getContestId().toUpperCase().startsWith("ARRL_SS");
        if (left == 0) return sweep ? "CLEAN SWEEP!" : "ALL WORKED";
        return left + (perBand && !band.isEmpty() ? " on " + band : "");
    }
    private static void refreshScoreStats() {
        if (host == null) return;
        Region frame = (Region) host.getChildren().get(0);
        List<Label> vs = new ArrayList<>(); collectStatV(frame, vs);
        ContestScore s = ContestState.score();
        if (vs.size() >= 4) {
            vs.get(0).setText(String.valueOf(s.qsoCount()));
            vs.get(1).setText(String.valueOf(s.mults()));
            vs.get(2).setText(String.valueOf(s.points()));
            vs.get(3).setText(String.format("%,d", s.score()));
        }
        if (vs.size() >= 6) { int[] r = rates(); vs.get(4).setText(r[0] + "/h"); vs.get(5).setText(r[1] + "/h"); }
        if (vs.size() >= 7 && leftLabel(ContestState.plugin()) != null) vs.get(6).setText(leftText(ContestState.plugin()));
        int clockAt = 6 + (leftLabel(ContestState.plugin()) != null ? 1 : 0);
        String[] clock = clockTile(ContestState.plugin());
        if (clock != null && vs.size() > clockAt) vs.get(clockAt).setText(clock[1]);
    }
    private static void collectStatV(Node n, List<Label> out) {
        if (n instanceof Label l && l.getStyleClass().contains("sx-stat-v")) out.add(l);
        if (n instanceof Parent p) for (Node c : p.getChildrenUnmodifiable()) collectStatV(c, out);
    }

    // ---- recent contest log table -----------------------------------------
    private static void fillLog(VBox tbl) { fillLog(tbl, 10); }
    private static void fillLog(VBox tbl, int limit) {
        tbl.getChildren().clear();
        tbl.getChildren().add(logHeader());
        List<QsoRecord> rows = ContestState.qsos();
        if (rows.isEmpty()) {
            Label none = lbl("No QSOs yet — log one above"); none.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-t4;-fx-padding:12 8;");
            tbl.getChildren().add(none); return;
        }
        int n = 0;
        for (QsoRecord q : rows) { if (n++ >= limit) break; tbl.getChildren().add(logRow(q)); }
    }
    private static Node logHeader() {
        HBox h = new HBox(9,
                col("UTC", 54), col("CALL", 90), col("BAND", 48), col("MODE", 46),
                col("SN", 44), col("EXCHANGE", -1), col("PTS", 46), col("", 92));
        h.getStyleClass().add("jl-loghd"); h.setAlignment(Pos.CENTER_LEFT); return h;
    }
    private static Node logRow(QsoRecord q) {
        String utc = q.getDateTimeUtc() == null ? "" : q.getDateTimeUtc().format(HMS).substring(0, 5);
        String exch = String.join(" ", strip(q.getSerialReceived(), q.getContestField1(), q.getContestField2(),
                q.getContestField3(), q.getContestField4(), q.getContestField5()));
        Label pts = cell(q.isDupe() ? "DUPE" : String.valueOf(q.getPoints()), 46);
        if (q.isDupe()) pts.setStyle("-fx-text-fill:-ars-err;-fx-font-weight:bold;");      // a dupe in the log is called out, not just dimmed
        Label edit = lbl("Edit", "jl-clearbtn"); edit.setStyle("-fx-cursor:hand;");
        Label erase = lbl("Erase", "jl-clearbtn"); erase.setStyle("-fx-cursor:hand;-fx-text-fill:-ars-err;");
        edit.setOnMouseClicked(e -> loadForEdit(q));
        erase.setOnMouseClicked(e -> eraseQso(erase, q));
        HBox acts = new HBox(10, edit, erase); acts.setMinWidth(92); acts.setPrefWidth(92); acts.setAlignment(Pos.CENTER_RIGHT);
        HBox r = new HBox(9,
                cell(utc, 54), cell(q.getCallsign(), 90), cell(z(q.getBand()), 48), cell(z(q.getMode()), 46),
                cell(z(q.getSerialSent()), 44), cell(exch, -1), pts, acts);
        r.getStyleClass().add("jl-logrow"); r.setAlignment(Pos.CENTER_LEFT);
        if (q.isDupe()) r.setOpacity(0.75);
        return r;
    }
    private static String[] strip(String... parts) {
        List<String> out = new ArrayList<>();
        for (String p : parts) if (p != null && !p.isBlank()) out.add(p);
        return out.toArray(new String[0]);
    }

    /** Every QSO of the contest in a window of its own, each with Edit / Erase - for correcting something older than the last ten. */
    private static void openAllQsos() {
        ContestPlugin plugin = ContestState.plugin();
        if (plugin == null) return;
        VBox tbl = new VBox(); tbl.getStyleClass().add("jl-logtbl");
        Runnable fill = () -> fillLog(tbl, Integer.MAX_VALUE);
        fill.run();
        ContestState.addListener(() -> Platform.runLater(fill));
        ScrollPane sp = new ScrollPane(tbl); sp.setFitToWidth(true); sp.setStyle("-fx-background-color:-ars-bg;");
        VBox root = new VBox(sp); VBox.setVgrow(sp, Priority.ALWAYS); root.setStyle("-fx-background-color:-ars-bg;");
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 620);
        com.ars.fx.Theme.apply(scene, false); com.ars.fx.shell.Tips.install(scene);
        com.ars.fx.Themes.applyTo(root, "log");
        javafx.stage.Stage st = new javafx.stage.Stage();
        st.setTitle(plugin.getContestName() + " \u2014 all QSOs"); st.setScene(scene);
        st.show();
    }

    // ---- Cabrillo export --------------------------------------------------
    private static void exportCabrillo(ContestPlugin plugin) {
        try {
            ContestConfigBridge.syncStationToEngine();
            java.nio.file.Path dir = java.nio.file.Paths.get(System.getProperty("user.home"), ".j-log", "exports");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path out = dir.resolve(plugin.getContestId() + "_" + HubConfig.call() + ".cbr");
            com.jlog.export.CabrilloExporter.export(plugin, out);
            if (com.ars.fx.data.LogExport.asciiOnly()) {            // J-Hub ▸ Data ▸ charset: ASCII
                String txt = java.nio.file.Files.readString(out, java.nio.charset.StandardCharsets.UTF_8);
                java.nio.file.Files.writeString(out, com.ars.fx.data.LogExport.forExport(txt), java.nio.charset.StandardCharsets.US_ASCII);
            }
            setB4("Exported → " + out, "new");
        } catch (Exception e) {
            setB4("Export failed: " + e.getMessage(), "dupe");
        }
    }

    // ---- ticker -----------------------------------------------------------
    private static boolean tickerStarted = false;
    private static void startTicker() {
        if (tickerStarted) return;
        tickerStarted = true;
        javafx.animation.Timeline tl = new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(3), e -> ContestState.recompute()));
        tl.setCycleCount(javafx.animation.Timeline.INDEFINITE); tl.play();
    }

    // ---- small helpers ----------------------------------------------------
    private static Control controlFor(FieldDef fd) {
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
    private static String value(String id) {
        Control c = fieldControls.get(id);
        if (c instanceof TextField t) return t.getText() == null ? "" : t.getText().trim();
        if (c instanceof ComboBox<?> cb) { Object v = cb.getValue(); return v == null ? "" : v.toString().trim(); }
        return "";
    }
    private static void focus(String id) { Control c = fieldControls.get(id); if (c != null) c.requestFocus(); }

    /** Fill an exchange field from a clicked map region, then focus the call field. Called by ContestMapPane. */
    @SuppressWarnings("unchecked")
    public static void fillField(String fieldId, String value) {
        if (fieldControls == null || fieldId == null) return;
        Control c = fieldControls.get(fieldId);
        if (c instanceof TextField t) t.setText(value);
        else if (c instanceof ComboBox<?> cb) ((ComboBox<String>) cb).setValue(value);
        Control call = fieldControls.get("callsign"); if (call != null) call.requestFocus();
    }

    private static TextField tf(String prompt, String init, double w) {
        TextField t = new TextField(init == null ? "" : init);
        if (prompt != null && !prompt.isEmpty()) t.setPromptText(prompt);
        t.getStyleClass().add("jhub-mini-field");
        if (w > 0) { t.setMinWidth(w); t.setPrefWidth(w); }
        return Shell.caps(t);
    }
    private static VBox g(String label, Node field) {
        VBox v = new VBox(5, lbl(label, "jl-flabel"), field); v.setAlignment(Pos.TOP_LEFT);
        return v;
    }
    private static Label col(String s, double w) { Label l = lbl(s, "jl-logc-h"); if (w > 0) { l.setMinWidth(w); l.setPrefWidth(w); } else { HBox.setHgrow(l, Priority.ALWAYS); l.setMaxWidth(Double.MAX_VALUE); } return l; }
    private static Label cell(String s, double w) { Label l = lbl(s == null ? "" : s, "jl-logc"); if (w > 0) { l.setMinWidth(w); l.setPrefWidth(w); } else { HBox.setHgrow(l, Priority.ALWAYS); l.setMaxWidth(Double.MAX_VALUE); } return l; }

    private static String z(String s) { return s == null ? "" : s; }
    private static String nowUtc() { return java.time.LocalTime.now(ZoneOffset.UTC).format(HMS); }
    private static String normBand(String b) { if (b == null) return ""; b = b.trim().toLowerCase(); return b.matches("\\d+") ? b + "m" : b; }
    private static String bandFor(long hz) { String b = com.ars.fx.data.JLogDb.bandFor(String.format(java.util.Locale.US, "%.3f", hz / 1e6)); return b == null ? "20m" : b; }
}
