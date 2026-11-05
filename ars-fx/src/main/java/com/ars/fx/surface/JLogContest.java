package com.ars.fx.surface;

import com.ars.fx.data.ContestConfigBridge;
import com.ars.fx.data.ContestLogs;
import com.ars.fx.data.ContestState;
import com.ars.fx.data.ContestValidators;
import com.ars.fx.data.HubConfig;
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
    private static Label similarL;                       // possible-duplicate callsigns
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

        Region dock = Shell.dock("log");
        HBox top = Shell.topBar("log", "log", "J-Log", "Contest · " + plugin.getContestName(), scoreStats(), nowUtc());

        // One-screen operating position: entry + last QSOs on the left, DX cluster over the contest panes on the right.
        VBox left = new VBox(8, toolbar(plugin), LogDbBar.build(true), entryPane(plugin), recentLogPane());
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
        for (Map.Entry<String, Region> e : panes) {
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
        VBox v = new VBox(6, lh, tbl); v.setPadding(new Insets(0, 18, 12, 18));
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
        HBox bar = new HBox(11, ic, ttl, gap, translator, normal, pick, export);
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
        for (FieldDef fd : plugin.getEntryFields()) {
            if ("band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;   // dedicated selectors above
            Control c = controlFor(fd);
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

        body.getChildren().add(wrapRow("RECEIVED", rcvd));
        body.getChildren().add(sendRow(plugin));

        // worked-before / dupe indicator + actions
        b4 = lbl("New", "jl-eyebrow"); b4.setStyle("-fx-font-size:20px;-fx-font-weight:bold;");   // DUPE / NEW / NEW BAND, readable from across the desk
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        Label clear = lbl("Clear", "jl-clearbtn"); clear.setStyle("-fx-cursor:hand;");
        Label logBtn = lbl("Log QSO  ⏎", "jl-logbtn"); logBtn.setStyle("-fx-cursor:hand;");
        HBox actions = new HBox(12, b4, gap, clear, logBtn); actions.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().add(actions);
        similarL = lbl("", "jl-emeta"); similarL.setWrapText(true);   // possible duplicates: calls already logged that resemble the typed one
        similarL.setMaxWidth(Double.MAX_VALUE);
        similarL.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:-ars-warn;-fx-font-family:'JetBrains Mono';"
                + "-fx-background-color:-ars-surface-2;-fx-border-color:-ars-warn;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 12 8 12;");
        similarL.managedProperty().bind(similarL.textProperty().isNotEmpty());   // no empty box while there is nothing to warn about
        similarL.visibleProperty().bind(similarL.textProperty().isNotEmpty());
        body.getChildren().add(similarL);

        // F1–F8 station macros (shared with normal log + dashboard)
        body.getChildren().add(MacroBar.sendAsRow(modeF));
        body.getChildren().add(MacroBar.row(MacroBar.fire(
                () -> modeF.getText(), () -> value("callsign"), msg -> setB4("▶ " + msg, "new"))));

        Runnable doLog = () -> { if (logQso(plugin)) clearForm(); };   // recompute → listeners refresh log + trackers
        logBtn.setOnMouseClicked(e -> doLog.run());
        clear.setOnMouseClicked(e -> clearForm());

        // keyboard flow: Enter in any field logs (a missing required field takes the focus instead), Esc clears
        for (Control c : fieldControls.values()) {
            if (c instanceof TextField t) t.setOnAction(e -> doLog.run());
            c.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) clearForm(); });
        }
        bandF.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) clearForm(); });
        modeF.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) clearForm(); });

        // live dupe check as the call is typed (short pause after the last key), and when band/mode change
        javafx.animation.PauseTransition dupeDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));
        dupeDelay.setOnFinished(e -> updateDupe(plugin));
        Control callC = fieldControls.get("callsign");
        if (callC instanceof TextField cf) cf.textProperty().addListener((o, a, b) -> dupeDelay.playFromStart());
        bandF.textProperty().addListener((o, a, b) -> dupeDelay.playFromStart());
        modeF.textProperty().addListener((o, a, b) -> dupeDelay.playFromStart());

        return body;
    }

    /** The mode a contest opens in: the one it pins, else the one its name says (SSB / CW / RTTY / digital), else the rig's, else CW. */
    private static String defaultMode(ContestPlugin plugin, com.ars.fx.data.RigClient.State rs) {
        String m = SentExchange.contestMode(plugin);
        if (m != null) return m;
        return rs.connected() && rs.mode() != null && !rs.mode().isBlank() ? rs.mode() : "SSB";   // mixed-mode contest: follow the rig, else SSB
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
    private static boolean logQso(ContestPlugin plugin) {
        String call = value("callsign");
        if (call == null || call.isBlank()) { focus("callsign"); return false; }
        // required-field check (band/mode come from the dedicated selectors)
        for (FieldDef fd : plugin.getEntryFields()) {
            if ("band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;
            if (fd.isRequired() && fd.getEntryRow() == 0 && value(fd.getId()).isBlank()) { focus(fd.getId()); return false; }
            if (!ContestValidators.valid(fd.getValidator(), value(fd.getId()))) { focus(fd.getId()); return false; }
        }
        try {
            String cid = plugin.getContestId();
            QsoRecord r = new QsoRecord();
            r.setContestId(cid);
            r.setCallsign(call.toUpperCase());
            r.setBand(normBand(bandF.getText()));
            r.setMode(modeF.getText().trim().toUpperCase());
            r.setDateTimeUtc(LocalDateTime.now(ZoneOffset.UTC));
            r.setSerialSent(String.valueOf(serial));
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
            List<QsoRecord> prior = ContestQsoDao.getInstance().findByCallsign(cid, call);
            r.setDupe(ContestScorer.isDupe(plugin, r, prior));
            r.setPoints(ContestScorer.points(plugin, r, ContestState.ctx()));
            ContestQsoDao.getInstance().insert(r);
            serial++;
            ContestState.recompute();
            return true;
        } catch (Exception e) {
            System.err.println("[contest] log failed: " + e.getMessage());
            return false;
        }
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

    private static void updateDupe(ContestPlugin plugin) {
        String call = value("callsign");
        if (call == null || call.length() < 2) { setB4("New", ""); if (similarL != null) similarL.setText(""); return; }
        final String typed = call.toUpperCase();
        Thread t = new Thread(() -> {
            try {
                List<QsoRecord> prior = ContestQsoDao.getInstance().findByCallsign(plugin.getContestId(), typed);
                QsoRecord cand = new QsoRecord();
                cand.setCallsign(typed); cand.setBand(normBand(bandF.getText())); cand.setMode(modeF.getText());
                boolean dupe = ContestScorer.isDupe(plugin, cand, prior);
                String similar = similarCalls(plugin, typed);
                Platform.runLater(() -> {
                    if (dupe) setB4("⛔ DUPE — " + typed, "dupe");
                    else if (!prior.isEmpty()) setB4("◐ NEW BAND/MODE — " + typed + " (worked " + prior.size() + "× elsewhere)", "b4");
                    else if (!similar.isEmpty()) setB4("◐ POSSIBLE DUPE — " + typed, "b4");
                    else if (typed.length() >= 3) setB4("✓ NEW — " + typed, "new");
                    else setB4("New", "");
                    if (similarL != null) similarL.setText(similar.isEmpty() ? "" : "POSSIBLE DUPES:  " + similar);
                });
            } catch (Exception ignored) {}
        }, "contest-dupe");
        t.setDaemon(true); t.start();
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
        String[] left = leftTile(p);
        if (left != null) {
            String[][] more = new String[tiles.length + 1][];
            System.arraycopy(tiles, 0, more, 0, tiles.length);
            more[tiles.length] = left;
            return more;
        }
        return tiles;
    }

    /** Label of the "multipliers still to work" tile, or null when the contest has no fixed set of multipliers (prefixes, DXCC). */
    private static String leftLabel(ContestPlugin p) {
        if (p == null || com.ars.fx.surface.contest.TrackerFactory.multiplierUniverse(p) == null) return null;
        if (p.getContestId() != null && p.getContestId().toUpperCase().startsWith("ARRL_SS")) return "TO CLEAN SWEEP";
        String t = p.getScoringRules() == null || p.getScoringRules().getMultiplierType() == null ? "" : p.getScoringRules().getMultiplierType();
        return switch (t) {
            case "sections" -> "SECTIONS LEFT";
            case "states" -> "STATES LEFT";
            case "zone_country", "zone_country_state" -> "ZONES LEFT";
            default -> "MULTS LEFT";
        };
    }
    private static String[] leftTile(ContestPlugin p) {
        String label = leftLabel(p);
        return label == null ? null : new String[]{label, leftText(p)};
    }
    /** "12 sections" / "CLEAN SWEEP!" - for contests that count multipliers per band, the ones left on the band in use. */
    private static String leftText(ContestPlugin p) {
        List<String> all = com.ars.fx.surface.contest.TrackerFactory.multiplierUniverse(p);
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
    }
    private static void collectStatV(Node n, List<Label> out) {
        if (n instanceof Label l && l.getStyleClass().contains("sx-stat-v")) out.add(l);
        if (n instanceof Parent p) for (Node c : p.getChildrenUnmodifiable()) collectStatV(c, out);
    }

    // ---- recent contest log table -----------------------------------------
    private static void fillLog(VBox tbl) {
        tbl.getChildren().clear();
        tbl.getChildren().add(logHeader());
        List<QsoRecord> rows = ContestState.qsos();
        if (rows.isEmpty()) {
            Label none = lbl("No QSOs yet — log one above"); none.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-t4;-fx-padding:12 8;");
            tbl.getChildren().add(none); return;
        }
        int n = 0;
        for (QsoRecord q : rows) { if (n++ >= 10) break; tbl.getChildren().add(logRow(q)); }
    }
    private static Node logHeader() {
        HBox h = new HBox(9,
                col("UTC", 54), col("CALL", 90), col("BAND", 48), col("MODE", 46),
                col("SN", 44), col("EXCHANGE", -1), col("PTS", 36));
        h.getStyleClass().add("jl-loghd"); h.setAlignment(Pos.CENTER_LEFT); return h;
    }
    private static Node logRow(QsoRecord q) {
        String utc = q.getDateTimeUtc() == null ? "" : q.getDateTimeUtc().format(HMS).substring(0, 5);
        String exch = String.join(" ", strip(q.getSerialReceived(), q.getContestField1(), q.getContestField2(),
                q.getContestField3(), q.getContestField4(), q.getContestField5()));
        HBox r = new HBox(9,
                cell(utc, 54), cell(q.getCallsign(), 90), cell(z(q.getBand()), 48), cell(z(q.getMode()), 46),
                cell(z(q.getSerialSent()), 44), cell(exch, -1), cell(q.isDupe() ? "dup" : String.valueOf(q.getPoints()), 36));
        r.getStyleClass().add("jl-logrow"); r.setAlignment(Pos.CENTER_LEFT);
        if (q.isDupe()) r.setOpacity(0.5);
        return r;
    }
    private static String[] strip(String... parts) {
        List<String> out = new ArrayList<>();
        for (String p : parts) if (p != null && !p.isBlank()) out.add(p);
        return out.toArray(new String[0]);
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
