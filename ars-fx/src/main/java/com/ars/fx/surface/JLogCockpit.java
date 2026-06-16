package com.ars.fx.surface;

import com.ars.fx.data.JLogDb;
import com.ars.fx.shell.Shell;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.function.Consumer;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/** J-Log — Normal logging mode: full QSO entry pane, real ~/.j-log DB, skimmer + station-ID timer. */
public final class JLogCockpit {
    private JLogCockpit() {}

    private static boolean tickerStarted = false;
    private static StackPane host;               // for the 1 s clock / ID-timer ticker lookups
    private static TextField dateRef, timeRef;   // QSO date/time fields the ticker keeps "live"
    private static boolean[] manualRef;          // true once the operator overrides date/time

    // skimmer band-activity decodes: {freq, call, mode, wpm/snr}
    private static final String[][] SKIMMER = {
            {"14.0312", "W3LPL",  "CW",  "28 wpm · +21 dB"}, {"14.0285", "K1TTT",  "CW",  "30 wpm · +18 dB"},
            {"14.0410", "DL8WPX", "CW",  "26 wpm · +12 dB"}, {"14.0235", "VK9DX",  "CW",  "24 wpm · +08 dB"},
            {"14.0742", "JA1XYZ", "FT8", "-04 dB"},          {"14.0746", "EA5K",   "FT8", "-11 dB"},
            {"14.0188", "5U5R",   "CW",  "22 wpm · +06 dB"}, {"14.0359", "PY2NY",  "CW",  "29 wpm · +14 dB"},
    };

    public static Region build() {
        Region dock = Shell.dock("log");
        String[][] stats = {
                {"IN LOG", fmt(JLogDb.count())},
                {"TODAY", String.valueOf(JLogDb.countToday())},
                {"LAST", lastCall()}};
        String utc = java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        HBox top = Shell.topBar("log", "log", "J-Log", "Normal log · " + com.ars.fx.data.HubConfig.call(), stats, utc);

        VBox center = entryPane();
        ScrollPane sp = new ScrollPane(center); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(sp, Priority.ALWAYS);

        Region rail = Shell.rail(railDrawers());
        host = new StackPane((Region) Shell.frame(dock, top, sp, rail));
        startTicker();
        return host;
    }

    // ---- complete QSO entry pane + recent log ------------------------------
    private static VBox entryPane() {
        // header
        Node ic = Shell.iconTile("log", "log", 16, "sx-dw-ic");
        Label sub = lbl(JLogDb.count() + " in log · " + JLogDb.countToday() + " today", "jl-emeta");
        Label titleLbl = lbl("New QSO", "jhub-card-title");
        VBox ttl = new VBox(1, titleLbl, sub);
        Region hsp = new Region(); HBox.setHgrow(hsp, Priority.ALWAYS);
        Label b4 = lbl("Normal log", "jl-eyebrow");   // worked-before / dupe / status
        HBox head = new HBox(11, ic, ttl, hsp, b4); head.setAlignment(Pos.CENTER_LEFT);

        TextField call = tf("", "", 210, true);
        TextField freq = tf("MHz", "14.074", 110, false);
        TextField band = tf("auto", "20m", 80, false);
        TextField mode = tf("", "USB", 80, false);
        TextField rsnt = tf("", "59", 70, false);
        TextField rrcv = tf("", "59", 70, false);
        TextField name = tf("name", "", 180, false);
        TextField grid = tf("FN31", "", 110, false);
        TextField qth  = tf("city / DXCC", "", 220, false);
        TextField stat = tf("ST", "", 70, false);
        TextField cnty = tf("county", "", 200, false);
        TextField pwr  = tf("W", "100", 80, false);
        TextField cmt  = tf("notes, QSL info…", "", -1, false);

        // QSO date/time (UTC) — live "now" until you override; ⟳ snaps back to now.
        TextField dateF = tf("YYYY-MM-DD", nowDate(), 130, false);
        TextField timeF = tf("HH:MM:SS", nowTime(), 110, false);
        boolean[] manualTime = {false};
        dateF.setOnKeyTyped(e -> manualTime[0] = true);
        timeF.setOnKeyTyped(e -> manualTime[0] = true);
        Label nowBtn = lbl("⟳ now", "jl-clearbtn"); nowBtn.setStyle("-fx-cursor:hand;");
        nowBtn.setOnMouseClicked(e -> { manualTime[0] = false; dateF.setText(nowDate()); timeF.setText(nowTime()); });
        dateRef = dateF; timeRef = timeF; manualRef = manualTime;   // ticker keeps these live

        // BAND auto-derives from FREQ (still hand-editable to override).
        freq.textProperty().addListener((o, a, b) -> { String bd = JLogDb.bandFor(b); if (bd != null && !bd.isBlank()) band.setText(bd); });

        // Shared digital-mode TX path: macros and the TX field both transmit through this.
        TextArea rx = txRxArea();
        Consumer<String> transmit = msg -> { if (msg != null && !msg.isBlank()) rx.appendText("> " + msg.toUpperCase() + "\n"); };

        // Fire a station macro (configured in J-Hub ▸ Data). In phone modes it plays
        // the voice-keyer audio file; in CW/RTTY/digital it sends the text message.
        java.util.function.IntConsumer fireMacro = i -> {
            com.ars.fx.data.MacroConfig.Macro m = com.ars.fx.data.MacroConfig.get(i);
            if (isPhone(mode.getText())) {
                if (m.audio() == null || m.audio().isBlank())
                    rx.appendText("▶ voice " + m.label() + " — no audio file set (Dashboard ▸ Data ▸ Macros)\n");
                else {
                    boolean played = com.ars.fx.data.MacroConfig.playVoice(m.audio());
                    rx.appendText("▶ voice " + m.label() + " — " + shortName(m.audio()) + (played ? "" : " (file not found)") + "\n");
                }
            } else {
                transmit.accept(expandMacro(m.text(), call.getText()));
            }
        };

        VBox dtCell1 = g("DATE (UTC)", dateF, false), dtCell2 = g("TIME (UTC)", timeF, false);
        Region dtsp = new Region(); HBox.setHgrow(dtsp, Priority.ALWAYS);
        HBox dtRow = new HBox(12, dtCell1, dtCell2, dtsp, nowBtn); dtRow.setAlignment(Pos.BOTTOM_LEFT);

        VBox body = new VBox(13); body.setPadding(new Insets(14, 18, 16, 18));
        body.getChildren().add(lbl("Enter the full QSO — callsign, signal report, name/QTH, grid, comment.", "jl-cp-empty"));
        body.getChildren().add(row(g("CALLSIGN", call, false), g("FREQ (MHZ)", freq, false), g("BAND", band, false), g("MODE", mode, false)));
        body.getChildren().add(row(g("RST SENT", rsnt, false), g("RST RCVD", rrcv, false), g("NAME", name, false), g("GRID", grid, false)));
        body.getChildren().add(row(g("QTH", qth, false), g("STATE", stat, false), g("COUNTY", cnty, false), g("POWER (W)", pwr, false)));
        body.getChildren().add(dtRow);

        VBox cmtF = g("COMMENT", cmt, true);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Label clear = lbl("Clear", "jl-clearbtn"); clear.setStyle("-fx-cursor:hand;");
        Label cancel = lbl("Cancel edit", "jl-clearbtn"); cancel.setStyle("-fx-cursor:hand;"); cancel.setVisible(false); cancel.setManaged(false);
        Label logBtn = lbl("Log QSO  ⏎", "jl-logbtn"); logBtn.setStyle("-fx-cursor:hand;");
        HBox cmtRow = new HBox(12, cmtF, sp2, cancel, clear, logBtn); cmtRow.setAlignment(Pos.BOTTOM_LEFT);
        body.getChildren().add(cmtRow);
        body.getChildren().add(macroRow(fireMacro));

        // recent log table
        Label lh = lbl("☰  Recent log"); lh.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:-ars-t3;");
        Label ln = lbl(""); ln.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-t4;");
        HBox lbar = new HBox(8, lh, ln); lbar.getStyleClass().add("jl-loghd"); lbar.setAlignment(Pos.CENTER_LEFT);
        VBox tbl = new VBox();

        TextField[] all = {call, name, grid, qth, stat, cnty, cmt};
        long[] editId = {-1};
        String[] lastLk = {""};
        Runnable[] refresh = new Runnable[1];

        Runnable exitEdit = () -> {
            editId[0] = -1; titleLbl.setText("New QSO"); logBtn.setText("Log QSO  ⏎");
            cancel.setVisible(false); cancel.setManaged(false);
            manualTime[0] = false; dateF.setText(nowDate()); timeF.setText(nowTime());
        };
        // Load a logged QSO back into the form for editing.
        Consumer<Long> onEdit = id -> {
            JLogDb.QsoFull f = JLogDb.byId(id);
            if (f == null) return;
            editId[0] = id;
            call.setText(z(f.call())); freq.setText(z(f.freq())); band.setText(z(f.band())); mode.setText(z(f.mode()));
            rsnt.setText(z(f.rstSent())); rrcv.setText(z(f.rstRcvd())); name.setText(z(f.name())); grid.setText(z(f.grid()));
            qth.setText(z(f.qth())); stat.setText(z(f.state())); cnty.setText(z(f.county()));
            pwr.setText(f.powerW() > 0 ? String.valueOf(f.powerW()) : ""); cmt.setText(z(f.notes()));
            String dt = f.datetimeUtc();
            dateF.setText(dt != null && dt.length() >= 10 ? dt.substring(0, 10) : nowDate());
            timeF.setText(dt != null && dt.length() >= 19 ? dt.substring(11, 19) : nowTime());
            manualTime[0] = true;
            titleLbl.setText("Edit QSO #" + id); logBtn.setText("Update  ⏎");
            cancel.setVisible(true); cancel.setManaged(true);
            call.requestFocus();
        };
        Consumer<Long> onDelete = id -> {
            if (JLogDb.delete(id)) {
                if (editId[0] == id) exitEdit.run();
                refresh[0].run();
                sub.setText(JLogDb.count() + " in log · " + JLogDb.countToday() + " today");
            }
        };
        refresh[0] = () -> fillLog(tbl, ln, onEdit, onDelete);
        refresh[0].run();

        Runnable doLog = () -> {
            if (call.getText() == null || call.getText().isBlank()) { call.requestFocus(); return; }
            int p = 0; try { p = Integer.parseInt(pwr.getText().trim()); } catch (Exception ignored) {}
            String bandVal = (band.getText() == null || band.getText().isBlank()) ? JLogDb.bandFor(freq.getText()) : band.getText();
            String dt = manualTime[0] ? composeDt(dateF.getText(), timeF.getText()) : null;
            boolean ok = (editId[0] >= 0)
                    ? JLogDb.update(editId[0], dt, call.getText(), freq.getText(), bandVal, mode.getText(),
                            rsnt.getText(), rrcv.getText(), name.getText(), qth.getText(), grid.getText(), cmt.getText(), stat.getText(), cnty.getText(), p)
                    : JLogDb.insertAt(dt, call.getText(), freq.getText(), bandVal, mode.getText(),
                            rsnt.getText(), rrcv.getText(), name.getText(), qth.getText(), grid.getText(), cmt.getText(), stat.getText(), cnty.getText(), p);
            if (ok) {
                exitEdit.run();
                for (TextField t : all) t.clear();
                b4.setText("Normal log"); b4.setStyle(""); lastLk[0] = "";
                call.requestFocus();
                refresh[0].run();
                sub.setText(JLogDb.count() + " in log · " + JLogDb.countToday() + " today");
            }
        };
        logBtn.setOnMouseClicked(e -> doLog.run());
        call.setOnAction(e -> doLog.run());
        cmt.setOnAction(e -> doLog.run());
        clear.setOnMouseClicked(e -> { exitEdit.run(); for (TextField t : all) t.clear(); b4.setText("Normal log"); b4.setStyle(""); lastLk[0] = ""; call.requestFocus(); });
        cancel.setOnMouseClicked(e -> { exitEdit.run(); for (TextField t : all) t.clear(); call.requestFocus(); });

        // Callsign field exit: worked-before/dupe indicator + callbook autofill (both off-thread).
        call.focusedProperty().addListener((o, was, now) -> {
            if (now) return;
            String c = call.getText() == null ? "" : call.getText().trim().toUpperCase();
            if (c.length() < 3) { b4.setText("Normal log"); b4.setStyle(""); return; }
            if (c.equals(lastLk[0])) return;
            lastLk[0] = c;
            String curBand = band.getText() == null ? "" : band.getText().trim();
            boolean lookupOn = com.ars.fx.data.LookupConfig.anyEnabled();
            Thread t = new Thread(() -> {
                JLogDb.Prior prior = JLogDb.priorWith(c);
                com.ars.fx.data.CallsignLookup.Result r = lookupOn ? com.ars.fx.data.CallsignLookup.lookup(c) : null;
                Platform.runLater(() -> {
                    updateB4(b4, prior, curBand);
                    if (r != null) { fillIfEmpty(name, r.name()); fillIfEmpty(grid, r.grid()); fillIfEmpty(qth, r.city());
                        fillIfEmpty(stat, r.state()); fillIfEmpty(cnty, r.county()); }
                });
            }, "callsign-lookup");
            t.setDaemon(true); t.start();
        });

        VBox entry = new VBox(0, head, body); entry.getStyleClass().add("jl-entry"); entry.setMaxWidth(940);
        // F1–F8 keyboard shortcuts for the macros (registered once the entry is on a scene).
        entry.sceneProperty().addListener((o, oldS, s) -> {
            if (s == null) return;
            KeyCode[] fkeys = {KeyCode.F1, KeyCode.F2, KeyCode.F3, KeyCode.F4, KeyCode.F5, KeyCode.F6, KeyCode.F7, KeyCode.F8};
            for (int i = 0; i < com.ars.fx.data.MacroConfig.count() && i < fkeys.length; i++) {
                final int idx = i;
                s.getAccelerators().put(new KeyCodeCombination(fkeys[i]), () -> fireMacro.accept(idx));
            }
        });
        Node txrx = rttyCwDrawer(rx, transmit); ((Region) txrx).setMaxWidth(940); VBox txWrap = new VBox(txrx); txWrap.setMaxWidth(940); txWrap.setPadding(new Insets(11, 14, 0, 14));
        lbar.setMaxWidth(940); tbl.setMaxWidth(940);
        VBox col = new VBox(entry, txWrap, lbar, tbl); col.setStyle("-fx-background-color:-ars-bg;");
        return col;
    }
    // F-key macros are defined in J-Hub ▸ Data (MacroConfig); J-Log just renders + fires them.
    /** Expand {CALL} in a macro from the current callsign field (blank → "OM"). */
    private static String expandMacro(String msg, String call) {
        String c = (call == null || call.isBlank()) ? "OM" : call.trim().toUpperCase();
        return msg.replace("{CALL}", c);
    }
    /** Phone modes play the voice keyer; everything else sends macro text. */
    private static boolean isPhone(String mode) {
        if (mode == null) return false;
        String m = mode.trim().toUpperCase();
        return m.equals("USB") || m.equals("LSB") || m.contains("SSB") || m.equals("AM") || m.equals("FM")
                || m.contains("PHONE") || m.contains("VOICE");
    }
    private static String shortName(String path) {
        if (path == null) return "";
        int s = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return s >= 0 ? path.substring(s + 1) : path;
    }
    private static HBox macroRow(java.util.function.IntConsumer fire) {
        HBox m = new HBox(6); m.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < com.ars.fx.data.MacroConfig.count(); i++) {
            final int idx = i;
            VBox mb = new VBox(2, lbl("F" + (i + 1), "jl-macro-fk"), lbl(com.ars.fx.data.MacroConfig.get(i).label(), "jl-macro-ml"));
            mb.getStyleClass().add("jl-macro");
            mb.setStyle("-fx-cursor:hand;"); HBox.setHgrow(mb, Priority.ALWAYS); mb.setMaxWidth(Double.MAX_VALUE);
            mb.setOnMouseClicked(e -> fire.accept(idx));
            m.getChildren().add(mb);
        }
        return m;
    }
    // ---- RTTY / CW transmit & receive drawer (between entry pane and log) ---
    // digital TX mode chips: {label, rate descriptor}
    private static final String[][] TX_MODES = {{"CW","25 wpm"},{"RTTY","45 baud · 170 Hz"},{"PSK31","31 baud"}};
    private static TextArea txRxArea() {
        TextArea rx = new TextArea("CQ CQ CQ DE WM3J WM3J K\nWM3J DE DL8WPX DL8WPX  UR 599 599  NAME HANS HANS  QTH MUNICH  BK\n");
        rx.setEditable(false); rx.setWrapText(true); rx.setPrefRowCount(5);
        rx.setStyle("-fx-control-inner-background:#0b0f14; -fx-background-color:#0b0f14; -fx-text-fill:-ars-ok; -fx-font-family:'JetBrains Mono'; -fx-font-size:12px; -fx-border-color:-ars-border; -fx-border-radius:6; -fx-background-radius:6;");
        return rx;
    }
    private static Node rttyCwDrawer(TextArea rx, Consumer<String> transmit) {
        Label[] chips = new Label[TX_MODES.length];
        Label rate = lbl(TX_MODES[0][1], "jl-skim-meta");
        HBox modeRow = new HBox(8); modeRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < TX_MODES.length; i++) {
            final int idx = i;
            Label chip = lbl(TX_MODES[i][0], "jl-chip"); if (i == 0) chip.getStyleClass().add("accent");
            chip.setStyle("-fx-cursor:hand;");
            chip.setOnMouseClicked(e -> {
                for (Label c : chips) c.getStyleClass().remove("accent");
                chip.getStyleClass().add("accent");
                rate.setText(TX_MODES[idx][1]);
            });
            chips[i] = chip; modeRow.getChildren().add(chip);
        }
        Region msp = new Region(); HBox.setHgrow(msp, Priority.ALWAYS);
        modeRow.getChildren().addAll(msp, rate);

        TextField tx = new TextField(); tx.setPromptText("type to transmit…"); tx.getStyleClass().add("jhub-mini-field"); HBox.setHgrow(tx, Priority.ALWAYS); tx.setMaxWidth(Double.MAX_VALUE);
        Label stop = lbl("Stop", "jl-clearbtn"); stop.setStyle("-fx-cursor:hand;");
        Label send = lbl("Send ⏎", "jl-logbtn"); send.setStyle("-fx-cursor:hand;");
        Runnable sendTx = () -> { if (!tx.getText().isBlank()) { transmit.accept(tx.getText()); tx.clear(); } };
        send.setOnMouseClicked(e -> sendTx.run());
        tx.setOnAction(e -> sendTx.run());
        stop.setOnMouseClicked(e -> { tx.clear(); rx.appendText("■ TX stopped\n"); });
        HBox txRow = new HBox(10, tx, stop, send); txRow.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(9, modeRow, rx, txRow); body.setPadding(new Insets(4, 2, 4, 2));
        return Shell.drawer("RTTY / CW · TX & RX", "digi", "digi", "CW · 25 wpm", false, body);
    }
    private static void fillIfEmpty(TextField f, String v) {
        if (v != null && !v.isBlank() && (f.getText() == null || f.getText().isBlank())) f.setText(v);
    }
    private static TextField tf(String prompt, String init, double w, boolean call) {
        TextField t = new TextField(init == null ? "" : init);
        if (prompt != null && !prompt.isEmpty()) t.setPromptText(prompt);
        t.getStyleClass().add("jhub-mini-field"); if (call) t.getStyleClass().add("call");
        if (w > 0) { t.setMinWidth(w); t.setPrefWidth(w); t.setMaxWidth(w); } else t.setMaxWidth(Double.MAX_VALUE);
        return t;
    }
    private static VBox g(String label, TextField field, boolean grow) {
        VBox v = new VBox(5, lbl(label, "jl-flabel"), field); v.setAlignment(Pos.TOP_LEFT);
        if (grow) { HBox.setHgrow(v, Priority.ALWAYS); v.setMaxWidth(Double.MAX_VALUE); }
        return v;
    }
    private static HBox row(VBox... cells) { HBox r = new HBox(12, cells); r.setAlignment(Pos.BOTTOM_LEFT); return r; }

    // columns: DATE, UTC, CALL, FREQ, BAND, MODE, RST, NAME, [edit/del actions]
    private static final double[] LOGC = {84, 48, -1, 74, 46, 54, 48, -1, 58};
    private static void fillLog(VBox tbl, Label count, Consumer<Long> onEdit, Consumer<Long> onDelete) {
        tbl.getChildren().clear();
        tbl.getChildren().add(headerRow(new String[]{"DATE", "UTC", "CALL", "FREQ", "BAND", "MODE", "RST", "NAME", ""}));
        List<JLogDb.Qso> rows = JLogDb.recent(12);
        if (rows.isEmpty()) {
            Label none = lbl("No QSOs yet — log one above"); none.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-t4;-fx-padding:14 18 14 18;");
            tbl.getChildren().add(none); count.setText("");
        } else {
            for (JLogDb.Qso q : rows) tbl.getChildren().add(dataRow(q, onEdit, onDelete));
            count.setText("· " + rows.size() + " shown · click a row to edit");
        }
    }
    private static HBox headerRow(String[] c) {
        HBox r = new HBox(10); r.setAlignment(Pos.CENTER_LEFT); r.setPadding(new Insets(9, 18, 9, 18));
        r.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;");
        for (int i = 0; i < c.length; i++) { Label l = lbl(c[i]); l.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:-ars-t3;"); col(r, l, i); }
        return r;
    }
    private static HBox dataRow(JLogDb.Qso q, Consumer<Long> onEdit, Consumer<Long> onDelete) {
        HBox r = new HBox(10); r.setAlignment(Pos.CENTER_LEFT); r.setPadding(new Insets(8, 18, 8, 18));
        r.getStyleClass().add("jl-logrow");
        r.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0; -fx-cursor:hand;");
        col(r, lbl(JLogDb.dateOf(q.utc()), "jl-tr-mono"), 0);
        col(r, lbl(JLogDb.hhmm(q.utc()), "jl-tr-mono"), 1);
        col(r, lbl(nv(q.callsign()), "jl-tr-call"), 2);
        col(r, lbl(nv(q.freq()), "jl-tr-mono"), 3);
        col(r, lbl(nv(q.band()), "jl-tr-mono"), 4);
        col(r, lbl(nv(q.mode()), "jl-mode"), 5);
        col(r, lbl(nv(q.rstRcvd()), "jl-tr-mono"), 6);
        col(r, lbl(nv(q.name()), "jl-tr-mono"), 7);
        // edit (click row) + delete (two-step ✕)
        Label del = lbl("✕", "jl-tr-mono"); del.setStyle("-fx-cursor:hand;-fx-text-fill:-ars-t4;-fx-font-size:13px;-fx-padding:0 6 0 6;");
        boolean[] armed = {false};
        del.setOnMouseClicked(e -> {
            if (e != null) e.consume();
            if (!armed[0]) { armed[0] = true; del.setText("del?"); del.setStyle("-fx-cursor:hand;-fx-text-fill:-ars-err;-fx-font-weight:bold;-fx-font-size:11px;-fx-padding:0 4 0 4;"); }
            else if (onDelete != null) onDelete.accept(q.id());
        });
        col(r, del, 8);
        r.setOnMouseClicked(e -> { if (onEdit != null) onEdit.accept(q.id()); });
        return r;
    }
    private static void col(HBox r, Node n, int i) {
        HBox cell = new HBox(n); cell.setAlignment(Pos.CENTER_LEFT);
        if (LOGC[i] < 0) { HBox.setHgrow(cell, Priority.ALWAYS); cell.setMaxWidth(Double.MAX_VALUE); }
        else { cell.setMinWidth(LOGC[i]); cell.setPrefWidth(LOGC[i]); cell.setMaxWidth(LOGC[i]); }
        r.getChildren().add(cell);
    }

    // ---- right rail --------------------------------------------------------
    private static Node[] railDrawers() {
        List<Node> rail = new ArrayList<>();
        rail.add(com.ars.fx.shell.IdTimer.drawer());
        rail.add(spotterDrawer());
        rail.add(heardByDrawer());
        rail.add(awardsDrawer());
        rail.add(Shell.drawer("Skimmer", "digi", "digi", SKIMMER.length + " decodes", true, skimmerBody()));
        List<Node> inst = Shell.instruments(50);
        rail.add(inst.get(0)); rail.add(inst.get(1)); rail.add(inst.get(2)); rail.add(inst.get(3)); // rotor, prop, solar+spacewx, band
        return rail.toArray(new Node[0]);
    }
    /** Spotter / DX cluster drawer — live DX spots from the shared ClusterClient. */
    private static Node spotterDrawer() {
        com.ars.fx.data.ClusterClient cc = com.ars.fx.data.ClusterClient.getInstance();
        Region cdot = new Region(); cdot.getStyleClass().add("jhub-mini-dot"); cdot.setMinSize(8, 8); cdot.setMaxSize(8, 8);
        cdot.setStyle("-fx-background-color:-ars-t4;-fx-background-radius:99;");
        HBox src = new HBox(6, cdot, lbl(cc.endpoint(), "jl-skim-meta")); src.setAlignment(Pos.CENTER_LEFT);
        VBox rows = new VBox();
        Label empty = lbl("Connecting to cluster…", "jl-skim-meta"); empty.setPadding(new Insets(8, 2, 8, 2));
        rows.getChildren().add(empty);
        VBox body = new VBox(8, src, rows); body.setPadding(new Insets(2, 2, 4, 2));
        cc.setListener(c -> {
            cdot.setStyle("-fx-background-color:" + (c.isConnected() ? "-ars-ok" : "-ars-t4") + ";-fx-background-radius:99;");
            rows.getChildren().clear();
            List<com.ars.fx.data.ClusterClient.Spot> list = c.spots();
            if (list.isEmpty()) { empty.setText(c.isConnected() ? "Connected — waiting for spots…" : "Cluster offline — reconnecting…"); rows.getChildren().add(empty); }
            else { int n = 0; for (com.ars.fx.data.ClusterClient.Spot s : list) { if (n++ >= 12) break; rows.getChildren().add(spotRow(s)); } }
        });
        cc.start();
        return Shell.drawer("DX Cluster", "map", "map", cc.endpoint(), true, body);
    }
    private static Region spotRow(com.ars.fx.data.ClusterClient.Spot s) {
        Label fq = lbl(com.ars.fx.data.ClusterClient.fmtFreq(s.freqHz()), "jl-skim-fq"); fq.setMinWidth(58);
        Label cl = lbl(s.call(), "jl-skim-cl"); HBox.setHgrow(cl, Priority.ALWAYS); cl.setMaxWidth(Double.MAX_VALUE);
        Label md = lbl(s.mode(), "jl-mode");
        Label age = lbl(com.ars.fx.data.ClusterClient.age(s.arrivalMs()), "jl-skim-meta");
        HBox top = new HBox(9, fq, cl, md, age); top.setAlignment(Pos.CENTER_LEFT);
        String tail = "de " + s.de() + (s.comment().isEmpty() ? "" : " · " + s.comment());
        HBox bot = new HBox(9, lbl(s.band(), "jl-skim-meta"), lbl(tail, "jl-skim-meta")); bot.setAlignment(Pos.CENTER_LEFT); HBox.setMargin(bot, new Insets(2, 0, 0, 0));
        VBox row = new VBox(0, top, bot); row.setPadding(new Insets(7, 0, 7, 0));
        row.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;");
        return row;
    }

    /** "Heard By" — reverse-beacon spots of our own call, pulled from RBN. */
    private static Node heardByDrawer() {
        com.ars.fx.data.HeardByClient hb = com.ars.fx.data.HeardByClient.getInstance();
        Region cdot = new Region(); cdot.getStyleClass().add("jhub-mini-dot"); cdot.setMinSize(8, 8); cdot.setMaxSize(8, 8);
        cdot.setStyle("-fx-background-color:-ars-t4;-fx-background-radius:99;");
        HBox src = new HBox(6, cdot, lbl(hb.endpoint(), "jl-skim-meta")); src.setAlignment(Pos.CENTER_LEFT);

        VBox rows = new VBox();
        Label empty = lbl("Listening for skimmer spots of " + hb.myCall() + "…", "jl-skim-meta"); empty.setWrapText(true);
        empty.setPadding(new Insets(8, 2, 8, 2));
        rows.getChildren().add(empty);
        VBox body = new VBox(8, src, rows); body.setPadding(new Insets(2, 2, 4, 2));

        hb.setListener(c -> {
            cdot.setStyle("-fx-background-color:" + (c.isConnected() ? "-ars-ok" : "-ars-t4") + ";-fx-background-radius:99;");
            rows.getChildren().clear();
            List<com.ars.fx.data.HeardByClient.HeardSpot> list = c.spots();
            if (list.isEmpty()) {
                empty.setText(c.isConnected() ? "Connected to RBN — waiting to be heard…"
                        : "Listening for skimmer spots of " + c.myCall() + "…");
                rows.getChildren().add(empty);
            } else {
                for (com.ars.fx.data.HeardByClient.HeardSpot s : list) rows.getChildren().add(heardRow(s));
            }
        });
        hb.start();
        return Shell.drawer("Heard By", "map", "map", hb.myCall() + " · RBN", true, body);
    }
    private static Region heardRow(com.ars.fx.data.HeardByClient.HeardSpot s) {
        Label fq = lbl(com.ars.fx.data.ClusterClient.fmtFreq(s.freqHz()), "jl-skim-fq"); fq.setMinWidth(58);
        Label sp = lbl(s.spotter(), "jl-skim-cl"); HBox.setHgrow(sp, Priority.ALWAYS); sp.setMaxWidth(Double.MAX_VALUE);
        Label md = lbl(s.mode(), "jl-mode");
        Label age = lbl(com.ars.fx.data.ClusterClient.age(s.arrivalMs()), "jl-skim-meta");
        HBox top = new HBox(9, fq, sp, md, age); top.setAlignment(Pos.CENTER_LEFT);
        HBox bot = new HBox(9, lbl("heard you · " + s.band(), "jl-skim-meta"), lbl(s.report(), "jl-skim-snr")); bot.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(bot, new Insets(2, 0, 0, 0));
        VBox row = new VBox(0, top, bot); row.setPadding(new Insets(7, 0, 7, 0));
        row.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;");
        return row;
    }
    /** Awards drawer — collapsible mini-drawers for WAS / WAC / DXCC, computed from the real log. */
    private static Node awardsDrawer() {
        com.ars.fx.data.Awards.Progress was = com.ars.fx.data.Awards.was();
        com.ars.fx.data.Awards.Progress wac = com.ars.fx.data.Awards.wac();
        com.ars.fx.data.Awards.Progress dxcc = com.ars.fx.data.Awards.dxcc();
        VBox body = new VBox(8, awardMini(was, true), awardMini(wac, true), awardMini(dxcc, false));
        body.setPadding(new Insets(2, 2, 4, 2));
        String summary = was.worked() + "/" + was.target() + " WAS";
        return Shell.drawer("Awards", "vault", "vault", summary, true, body);
    }
    private static Node awardMini(com.ars.fx.data.Awards.Progress p, boolean grid) {
        double frac = p.target() > 0 ? Math.min(1.0, (double) p.worked() / p.target()) : 0;
        VBox content = new VBox(9);
        // progress bar + count
        StackPane bar = new StackPane(); bar.setMinHeight(8); bar.setMaxHeight(8); bar.setMaxWidth(Double.MAX_VALUE);
        Region track = new Region(); track.getStyleClass().add("jl-award-bar-track"); track.setMinHeight(8); track.setMaxHeight(8); track.setMaxWidth(Double.MAX_VALUE);
        Region fill = new Region(); fill.getStyleClass().add("jl-award-bar-fill"); fill.setMinHeight(8); fill.setMaxHeight(8);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT); bar.getChildren().addAll(track, fill);
        fill.maxWidthProperty().bind(bar.widthProperty().multiply(frac));
        Label cnt = lbl(p.worked() + " / " + p.target(), "jl-award-pct");
        Region csp = new Region(); HBox.setHgrow(csp, Priority.ALWAYS);
        HBox barRow = new HBox(10, bar, cnt); HBox.setHgrow(bar, Priority.ALWAYS); barRow.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().add(barRow);
        // grid of entities (worked highlighted)
        if (grid && p.all() != null) {
            FlowPane fp = new FlowPane(4, 4);
            for (String e : p.all()) {
                Label chip = lbl(e, "jl-award-chip"); if (p.workedSet().contains(e)) chip.getStyleClass().add("worked");
                fp.getChildren().add(chip);
            }
            content.getChildren().add(fp);
        } else {
            content.getChildren().add(lbl(p.worked() + " entities worked · target " + p.target(), "jl-skim-meta"));
        }
        return Shell.drawer(p.code(), "log", "vault", p.worked() + "/" + p.target(), false, content);
    }

    private static Region skimmerBody() {
        VBox v = new VBox();
        for (String[] s : SKIMMER) {
            Label fq = lbl(s[0], "jl-skim-fq"); fq.setMinWidth(58);
            Label cl = lbl(s[1], "jl-skim-cl"); HBox.setHgrow(cl, Priority.ALWAYS); cl.setMaxWidth(Double.MAX_VALUE);
            Label md = lbl(s[2], "jl-mode");
            Label snr = lbl(s[3], "jl-skim-snr");
            HBox top = new HBox(9, fq, cl, md); top.setAlignment(Pos.CENTER_LEFT);
            HBox bot = new HBox(9, lbl("de skimmer", "jl-skim-meta"), snr); bot.setAlignment(Pos.CENTER_LEFT); HBox.setMargin(bot, new Insets(2, 0, 0, 0));
            VBox row = new VBox(0, top, bot); row.setPadding(new Insets(7, 0, 7, 0));
            row.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;");
            v.getChildren().add(row);
        }
        return v;
    }

    // ---- UTC clock ticker (ID timer is the shared IdTimer component) -------
    private static void startTicker() {
        if (tickerStarted) return;
        tickerStarted = true;
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (host != null && host.lookup(".sx-clock-v") instanceof Label l)
                l.setText(java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            // keep the QSO date/time live until the operator overrides (and not while editing them)
            if (dateRef != null && manualRef != null && !manualRef[0] && !dateRef.isFocused() && !timeRef.isFocused()) {
                dateRef.setText(nowDate()); timeRef.setText(nowTime());
            }
        }));
        t.setCycleCount(Timeline.INDEFINITE); t.play();
    }

    // ---- helpers -----------------------------------------------------------
    private static String nowDate() { return java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString(); }
    private static String nowTime() { return java.time.LocalTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")); }
    /** Combine date + time fields into "yyyy-MM-dd HH:mm:ss", or null if not a valid timestamp (→ now). */
    private static String composeDt(String date, String time) {
        String d = date == null ? "" : date.trim(), t = time == null ? "" : time.trim();
        if (t.length() == 5) t = t + ":00";
        String dt = d + " " + t;
        return dt.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}") ? dt : null;
    }
    /** Update the worked-before / dupe indicator from prior-QSO data. */
    private static void updateB4(Label b4, JLogDb.Prior prior, String curBand) {
        if (prior == null || prior.count() == 0) {
            b4.setText("✓ NEW"); b4.setStyle("-fx-text-fill:-ars-ok;-fx-font-weight:bold;");
        } else if (curBand != null && !curBand.isBlank() && prior.bands().contains(curBand)) {
            b4.setText("⚠ DUPE " + curBand + " · " + prior.count() + "×"); b4.setStyle("-fx-text-fill:-ars-err;-fx-font-weight:bold;");
        } else {
            String bands = prior.bands().isEmpty() ? "" : " · " + String.join(",", prior.bands());
            b4.setText("● B4 " + prior.count() + "×" + bands); b4.setStyle("-fx-text-fill:-ars-warn;-fx-font-weight:bold;");
        }
    }
    private static String z(String s) { return s == null ? "" : s; }
    private static String nv(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private static String fmt(int n) { return String.format("%,d", n); }
    private static String lastCall() {
        List<JLogDb.Qso> r = JLogDb.recent(1);
        return r.isEmpty() ? "—" : r.get(0).callsign();
    }
}
