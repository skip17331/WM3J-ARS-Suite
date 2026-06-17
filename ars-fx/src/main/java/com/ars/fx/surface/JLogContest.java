package com.ars.fx.surface;

import com.ars.fx.data.ContestConfigBridge;
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

        VBox center = new VBox(12, toolbar(plugin), entryPane(plugin));
        Region strip = trackerStrip(plugin);
        if (strip != null) center.getChildren().add(strip);
        center.getChildren().add(recentLogPane());
        ScrollPane sp = new ScrollPane(center); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(sp, Priority.ALWAYS);

        List<Node> railNodes = new ArrayList<>(Shell.leadDrawers(0));
        railNodes.addAll(com.ars.fx.surface.contest.TrackerFactory.columnPanes(plugin));
        Region rail = Shell.rail(railNodes.toArray(new Node[0]));
        host = new StackPane((Region) Shell.frame(dock, top, sp, rail));

        ContestState.addListener(() -> Platform.runLater(JLogContest::refreshScoreStats));
        ContestState.recompute();
        startTicker();
        return host;
    }

    // ---- tracker strip (row-placement multiplier panes) -------------------
    private static Region trackerStrip(ContestPlugin plugin) {
        List<Region> panes = com.ars.fx.surface.contest.TrackerFactory.rowPanes(plugin);
        if (panes.isEmpty()) return null;
        HBox strip = new HBox(12); strip.setAlignment(Pos.TOP_LEFT); strip.getChildren().addAll(panes);
        strip.setPadding(new Insets(0, 18, 4, 18));
        ScrollPane sp = new ScrollPane(strip); sp.setFitToHeight(true);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setStyle("-fx-background-color:transparent;");
        sp.setMinHeight(140); sp.setPrefHeight(280); sp.setMaxHeight(300);
        return sp;
    }

    // ---- recent contest-log pane (refreshed on every score recompute) -----
    private static VBox recentLogPane() {
        Label lh = lbl("☰  Recent contest log"); lh.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:-ars-t3;");
        VBox tbl = new VBox(); tbl.getStyleClass().add("jl-logtbl");
        fillLog(tbl);
        ContestState.addListener(() -> Platform.runLater(() -> fillLog(tbl)));
        VBox v = new VBox(6, lh, tbl); v.setMaxWidth(1100); v.setPadding(new Insets(0, 18, 18, 18));
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
        pick.setOnMouseClicked(e -> { ContestState.setActive(null); Shell.navigate("logc"); });
        Label normal = lbl("Normal log", "jl-clearbtn"); normal.setStyle("-fx-cursor:hand;");
        normal.setOnMouseClicked(e -> Shell.navigate("log"));
        Label export = lbl("Export Cabrillo ⤓", "jl-logbtn"); export.setStyle("-fx-cursor:hand;");
        export.setOnMouseClicked(e -> exportCabrillo(plugin));

        HBox bar = new HBox(11, ic, ttl, gap, normal, pick, export);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(14, 18, 6, 18));
        return bar;
    }

    // ---- entry pane: band/mode + plugin fields + log table -----------------
    private static VBox entryPane(ContestPlugin plugin) {
        fieldControls = new LinkedHashMap<>();
        serial = ContestState.nextSerial();

        VBox body = new VBox(13); body.setPadding(new Insets(8, 18, 16, 18));

        // BAND + MODE (seeded from the rig; locked when the plugin pins them)
        com.ars.fx.data.RigClient.State rs = com.ars.fx.data.RigClient.getInstance().last();
        String band0 = plugin.getLockedBand() != null ? plugin.getLockedBand()
                : (rs.connected() && rs.freqHz() > 0 ? bandFor(rs.freqHz()) : "20m");
        String mode0 = plugin.getLockedMode() != null ? plugin.getLockedMode()
                : (rs.connected() && rs.mode() != null && !rs.mode().isBlank() ? rs.mode() : "CW");
        bandF = tf("", band0, 80); modeF = tf("", mode0, 80);
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
            (fd.getEntryRow() == 1 ? sent : rcvd).add(g(fd.getLabel(), c));
        }
        // running serial display in the sent row
        serialV = lbl(String.valueOf(serial), "jhub-rig-fq");
        sent.add(0, g("SERIAL", serialV));

        body.getChildren().add(lbl("Log the exchange — band/mode + " + (plugin.getExchangeFormat() == null ? "exchange" : plugin.getExchangeFormat()) + ".", "jl-cp-empty"));
        body.getChildren().add(wrapRow("RECEIVED", rcvd));
        body.getChildren().add(wrapRow("SENT", sent));

        // worked-before / dupe indicator + actions
        b4 = lbl("New", "jl-eyebrow");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        Label clear = lbl("Clear", "jl-clearbtn"); clear.setStyle("-fx-cursor:hand;");
        Label logBtn = lbl("Log QSO  ⏎", "jl-logbtn"); logBtn.setStyle("-fx-cursor:hand;");
        HBox actions = new HBox(12, b4, gap, clear, logBtn); actions.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().add(actions);

        Runnable doLog = () -> { if (logQso(plugin)) clearForm(); };   // recompute → listeners refresh log + trackers
        logBtn.setOnMouseClicked(e -> doLog.run());
        clear.setOnMouseClicked(e -> clearForm());

        // dupe check on callsign blur
        Control callC = fieldControls.get("callsign");
        if (callC instanceof TextField cf) {
            cf.setOnAction(e -> doLog.run());
            cf.focusedProperty().addListener((o, was, now) -> { if (!now) updateDupe(plugin); });
        }

        body.setMaxWidth(1100);
        return body;
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
        if (call == null || call.length() < 3) { setB4("New", ""); return; }
        Thread t = new Thread(() -> {
            try {
                List<QsoRecord> prior = ContestQsoDao.getInstance().findByCallsign(plugin.getContestId(), call);
                QsoRecord cand = new QsoRecord();
                cand.setCallsign(call); cand.setBand(normBand(bandF.getText())); cand.setMode(modeF.getText());
                boolean dupe = ContestScorer.isDupe(plugin, cand, prior);
                Platform.runLater(() -> {
                    if (dupe) setB4("⛔ DUPE — " + call.toUpperCase(), "dupe");
                    else if (!prior.isEmpty()) setB4("● B4 " + prior.size() + "× — " + call.toUpperCase(), "b4");
                    else setB4("✓ NEW — " + call.toUpperCase(), "new");
                });
            } catch (Exception ignored) {}
        }, "contest-dupe");
        t.setDaemon(true); t.start();
    }
    private static void setB4(String text, String cls) {
        if (b4 == null) return;
        b4.setText(text);
        b4.getStyleClass().removeAll("dupe", "b4", "new");
        if (!cls.isEmpty()) b4.getStyleClass().add(cls);
    }

    private static void clearForm() {
        for (Control c : fieldControls.values()) {
            if (c instanceof TextField t) t.clear();
            else if (c instanceof ComboBox<?> cb) cb.getSelectionModel().clearSelection();
        }
        serial = ContestState.nextSerial();
        if (serialV != null) serialV.setText(String.valueOf(serial));
        setB4("New", "");
        focus("callsign");
    }

    // ---- score readout ----------------------------------------------------
    private static String[][] scoreStats() {
        ContestScore s = ContestState.score();
        return new String[][]{
                {"QSOs", String.valueOf(s.qsoCount())},
                {"MULTS", String.valueOf(s.mults())},
                {"PTS", String.valueOf(s.points())},
                {"SCORE", String.format("%,d", s.score()), "accent"}};
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
        for (QsoRecord q : rows) { if (n++ >= 15) break; tbl.getChildren().add(logRow(q)); }
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
        // force uppercase for exchange fields (stored uppercase in the DB)
        t.textProperty().addListener((o, a, b) -> { if (b != null && !b.equals(b.toUpperCase())) t.setText(b.toUpperCase()); });
        return t;
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
        return t;
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
