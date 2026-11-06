package com.ars.fx.surface;

import com.ars.fx.data.JLogDb;
import com.ars.fx.shell.FeedUi;
import com.ars.fx.shell.Shell;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
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
    private static TextField callRef, freqRef, bandRef, modeRef;   // spot-click prefill targets
    private static Runnable clearRef;                              // the Clear button's action: a spot click starts from a blank form
    private static Runnable lookupRef;                            // worked-before + callbook lookup for the current call

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
        // the DX Cluster window from the J-Hub dashboard, beside the entry form
        Region cluster = JHubDashboard.clusterCol();
        cluster.setMinWidth(380); cluster.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cluster, Priority.ALWAYS);
        sp.setMinWidth(600); sp.setPrefWidth(800); HBox.setHgrow(sp, Priority.NEVER);   // the entry form keeps its natural width; the cluster takes the rest
        HBox main = new HBox(14, sp, cluster); main.setFillHeight(true); main.setPadding(new Insets(0, 14, 0, 0));
        main.setStyle("-fx-background-color:-ars-bg;");
        host = new StackPane((Region) Shell.frame(dock, top, main, rail));
        startTicker();
        // spot-click prefill: register this form as the live log + apply any spot picked elsewhere
        SpotAction.setLogTarget(JLogCockpit::prefillSpot);
        SpotAction.Prefill queued = SpotAction.takePending();
        if (queued != null) prefillSpot(queued);
        return host;
    }

    /** Fill the new-QSO form from a clicked DX spot (callsign + freq/band/mode), then focus the call field. */
    static void prefillSpot(SpotAction.Prefill p) {
        Runnable r = () -> {
            if (callRef == null || p == null) return;
            if (clearRef != null) clearRef.run();     // new spot = new contact: clear the previous one's name/QTH/grid, and forget the last lookup
            if (p.call() != null) callRef.setText(p.call());
            if (p.freqMhz() != null) freqRef.setText(p.freqMhz());
            if (p.band() != null && !p.band().isBlank()) bandRef.setText(p.band());
            if (p.mode() != null && !p.mode().isBlank()) modeRef.setText(p.mode());
            callRef.requestFocus();
            callRef.positionCaret(callRef.getText() == null ? 0 : callRef.getText().length());
            if (lookupRef != null) lookupRef.run();   // worked-before + callbook autofill for the spotted call
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    // ---- complete QSO entry pane + recent log ------------------------------
    /** True while the J-Hub dashboard builds the entry form: the RTTY/CW drawer is then handed to the dashboard's side column. */
    private static boolean embedded;
    private static Node embeddedTx;

    /** The normal-log entry form + recent log, for the J-Hub dashboard (the merged normal-log page). Spot clicks fill it while it is on screen. */
    static VBox embeddedEntry() {
        embedded = true; embeddedTx = null;
        VBox pane;
        try { pane = entryPane(); } finally { embedded = false; }
        startTicker();
        pane.sceneProperty().addListener((o, was, sc) -> {
            if (sc != null) SpotAction.setQuickTarget(JLogCockpit::prefillSpot); else SpotAction.clearQuickTarget(JLogCockpit::prefillSpot);
        });
        return pane;
    }
    /** Log mode: normal log (current) and the way into contest mode. */
    private static Node logModeDrawer() {
        Label cur = lbl("\u25cf Normal log", "jl-skim-meta"); cur.setStyle("-fx-font-weight:bold;-fx-text-fill:-ars-ok;");
        Label where = lbl("Logging to " + com.ars.fx.data.LogDbInfo.normalLine(), "jl-emeta"); where.setWrapText(true);
        Label contest = lbl("\u21c4 Contest mode\u2026", "jl-clearbtn"); contest.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(contest, "Switch to the contest page. Contest QSOs go to a contest database of their own.");
        contest.setOnMouseClicked(e -> JLogContest.promptEnter(contest));
        return Shell.drawer("Log mode", "log", "log", "Normal", true, new VBox(8, cur, where, contest));
    }
    /** Translator: QSO phrases in other languages, following the call being logged. */
    private static Node translatorDrawer() {
        Label open = lbl("Open translator", "jl-clearbtn"); open.setStyle("-fx-cursor:hand;");
        open.setOnMouseClicked(e -> TranslatorWindow.show(open, callRef == null ? "" : callRef.getText()));
        Label hint = lbl("QSO phrases in other languages; follows the country of the callsign you are logging.", "jl-emeta"); hint.setWrapText(true);
        return Shell.drawer("Translator", "learn", "learn", "", false, new VBox(8, hint, open));
    }
    /** Database tools: which database is in use, and the way to J-Hub Setup > Data. */
    private static Node databaseDrawer() {
        Label where = lbl("Log in use: " + com.ars.fx.data.LogDbInfo.normalLine(), "jl-emeta"); where.setWrapText(true);
        Label tools = lbl("Database tools \u203a", "jl-clearbtn"); tools.setStyle("-fx-cursor:hand;");
        tools.setOnMouseClicked(e -> Shell.navigate("setup:logdb"));   // J-Hub > Data: database info, export, upload
        return Shell.drawer("Database tools", "vault", "vault", "", false, new VBox(8, where, tools));
    }

    /** The J-Log-only drawers (Log mode, RTTY/CW TX & RX, Heard By, Awards, Skimmer, Translator, Database tools) for the dashboard's collapsing side column. Call after {@link #embeddedEntry()}. */
    static Node[] sideDrawers() {
        List<Node> out = new ArrayList<>();
        out.add(logModeDrawer());
        if (embeddedTx != null) out.add(embeddedTx);
        out.add(heardByDrawer()); out.add(awardsDrawer()); out.add(skimmerDrawer());
        out.add(translatorDrawer()); out.add(databaseDrawer());
        return out.toArray(new Node[0]);
    }

    /** The drawer the contest page's LOG TOOLS column carries besides its own Contest drawer: the Translator, following the call being logged. (Awards and Database tools read the normal log, so they stay off the contest page.) */
    static Node[] contestSideDrawers(java.util.function.Supplier<String> call) {
        Label open = lbl("Open translator", "jl-clearbtn"); open.setStyle("-fx-cursor:hand;");
        open.setOnMouseClicked(e -> TranslatorWindow.show(open, call.get()));
        Label hint = lbl("QSO phrases in other languages; follows the country of the callsign you are logging.", "jl-emeta"); hint.setWrapText(true);
        Node translator = Shell.drawer("Translator", "learn", "learn", "", false, new VBox(8, hint, open));
        return new Node[]{translator};
    }

    /** Is this call already in the normal log? The set of calls is re-read at most every ten seconds. */
    private static java.util.function.Predicate<String> normalWorked() {
        java.util.Set<String>[] calls = new java.util.Set[]{java.util.Set.of()};
        long[] at = {0};
        return c -> {
            long now = System.currentTimeMillis();
            if (now - at[0] > 10_000) {
                java.util.Set<String> s = new java.util.HashSet<>();
                for (String k : com.ars.fx.data.JLogDb.workedCallBands()) s.add(k.substring(0, k.indexOf('|')));
                calls[0] = s; at[0] = now;
            }
            return calls[0].contains(c.toUpperCase());
        };
    }

    private static VBox entryPane() {
        // header
        Node ic = Shell.iconTile("log", "log", 16, "sx-dw-ic");
        Label sub = lbl(JLogDb.count() + " in log · " + JLogDb.countToday() + " today", "jl-emeta");
        Label titleLbl = lbl("New QSO", "jhub-card-title");
        VBox ttl = new VBox(1, titleLbl, sub);
        Region hsp = new Region(); HBox.setHgrow(hsp, Priority.ALWAYS);
        Label contestChip = lbl("⇄ Contest mode", "jl-clearbtn"); contestChip.setStyle("-fx-cursor:hand;");
        contestChip.setOnMouseClicked(e -> JLogContest.promptEnter(contestChip));
        final String idleB4 = embedded ? "" : "Normal log";    // merged page: the header shows only the worked-before / dupe status
        Label b4 = lbl(idleB4, "jl-eyebrow");   // worked-before / dupe / status
        Label translatorChip = lbl("Translator", "jl-clearbtn"); translatorChip.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(translatorChip, "QSO phrases in other languages — follows the country of the callsign you are logging");
        HBox head = embedded ? new HBox(11, ic, ttl, hsp, b4) : new HBox(11, ic, ttl, hsp, translatorChip, contestChip, b4);
        head.setAlignment(Pos.CENTER_LEFT);

        TextField call = tf("", "", 210, true);
        call.textProperty().addListener((o, a, b) -> TranslatorWindow.follow(b));        // the translator (if open) follows the call's country
        translatorChip.setOnMouseClicked(e -> TranslatorWindow.show(translatorChip, call.getText()));
        TextField freq = tf("MHz", "14.074", 110, false);
        TextField band = tf("auto", "20m", 80, false);
        TextField mode = tf("", "USB", 80, false);
        callRef = call; freqRef = freq; bandRef = band; modeRef = mode;   // spot-click prefill
        TextField rsnt = tf("", "59", 70, false);
        TextField rrcv = tf("", "59", 70, false);
        TextField name = tf("name", "", 180, false);
        TextField grid = tf("FN31", "", 110, false);
        TextField qth  = tf("city / DXCC", "", 220, false);
        TextField stat = tf("ST", "", 70, false);
        TextField cnty = tf("county", "", 200, false);
        TextField pwr  = tf("W", com.ars.fx.data.HubConfig.get("station.txPower", "100").trim(), 80, false);
        TextField cmt  = tf("notes, QSL info…", "", -1, false);
        TextField iota = tf("NA-005", "", 110, false);       // award references: island / summit / park
        TextField sota = tf("W7W/LC-001", "", 150, false);
        TextField pota = tf("US-0001", "", 110, false);
        CheckBox cQsl = new CheckBox("QSL"), cLotw = new CheckBox("LoTW"), cEqsl = new CheckBox("eQSL");
        com.ars.fx.shell.Tips.tip(cQsl, "Paper QSL card received"); com.ars.fx.shell.Tips.tip(cLotw, "Confirmed on LoTW"); com.ars.fx.shell.Tips.tip(cEqsl, "Confirmed on eQSL");
        HBox confBox = new HBox(10, cQsl, cLotw, cEqsl); confBox.setAlignment(Pos.CENTER_LEFT);
        com.ars.fx.data.JLogDb.Refs[] baseRefs = {com.ars.fx.data.JLogDb.Refs.NONE};   // CQ / ITU zone of the QSO being edited (kept as is)

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
            if (MacroBar.usePhone(mode.getText())) {
                if (m.audio() == null || m.audio().isBlank())
                    rx.appendText("▶ voice " + m.label() + " — no audio file set (Dashboard ▸ Data ▸ Macros)\n");
                else
                    com.ars.fx.data.TxController.macroVoice(m.audio(), msg -> rx.appendText(msg + "\n"));   // keys the rig, plays the file, unkeys
            } else {
                Consumer<String> say = msg -> rx.appendText(msg + "\n");
                String text = MacroBar.textFor(m.text(), call.getText(), say);      // fills {CALL} and your station data; null = a field is empty
                if (text != null) com.ars.fx.data.TxController.macroText(mode.getText(), text, say);   // CW via the rig keyer
            }
        };

        VBox dtCell1 = g("DATE (UTC)", dateF, false), dtCell2 = g("TIME (UTC)", timeF, false);
        Region dtsp = new Region(); HBox.setHgrow(dtsp, Priority.ALWAYS);
        HBox dtRow = new HBox(12, dtCell1, dtCell2, dtsp, nowBtn); dtRow.setAlignment(Pos.BOTTOM_LEFT);

        VBox body = new VBox(13); body.setPadding(new Insets(14, 18, 16, 18));
        body.getChildren().add(lbl("Enter the full QSO — callsign, signal report, name/QTH, grid, comment.", "jl-cp-empty"));
        body.getChildren().add(row(g("CALLSIGN", call, false), g("FREQ (MHZ)", freq, false), g("BAND", band, false), g("MODE", mode, false)));
        body.getChildren().add(Shell.bandCaption(freq));        // "20m · CW segment" from the shared band plan
        // Super Check Partial under the call, as in contest mode; a call already in the log is ticked
        com.ars.fx.surface.contest.ScpBox scp = new com.ars.fx.surface.contest.ScpBox(c -> { call.setText(c); call.requestFocus(); call.positionCaret(c.length()); }, normalWorked());
        call.textProperty().addListener((o, a, b) -> scp.update(b));
        body.getChildren().add(scp);
        body.getChildren().add(row(g("RST SENT", rsnt, false), g("RST RCVD", rrcv, false), g("NAME", name, false), g("GRID", grid, false)));
        body.getChildren().add(row(g("QTH", qth, false), g("STATE", stat, false), g("COUNTY", cnty, false), g("POWER (W)", pwr, false)));
        body.getChildren().add(dtRow);
        body.getChildren().add(row(g("IOTA", iota, false), g("SOTA SUMMIT", sota, false), g("POTA PARK", pota, false), g("CONFIRMED", confBox, false)));
        body.getChildren().add(RefHints.attach(call, grid, iota, sota, pota));

        VBox cmtF = g("COMMENT", cmt, true);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Label clear = lbl("Clear", "jl-clearbtn"); clear.setStyle("-fx-cursor:hand;");
        Label cancel = lbl("Cancel edit", "jl-clearbtn"); cancel.setStyle("-fx-cursor:hand;"); cancel.setVisible(false); cancel.setManaged(false);
        Label logBtn = lbl("Log QSO  ⏎", "jl-logbtn"); logBtn.setStyle("-fx-cursor:hand;");
        Shell.QslToggle printQsl = Shell.qslToggle();
        HBox cmtRow = new HBox(12, cmtF, sp2, printQsl.node, cancel, clear, logBtn); cmtRow.setAlignment(Pos.BOTTOM_LEFT);
        body.getChildren().add(cmtRow);
        body.getChildren().add(MacroBar.sendAsRow(mode));
        body.getChildren().add(MacroBar.row(fireMacro));

        // recent log table
        Label lh = lbl("☰  Recent log"); lh.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:-ars-t3;");
        Label ln = lbl(""); ln.setStyle("-fx-font-size:11px;-fx-text-fill:-ars-t4;");
        Region lsp = new Region(); HBox.setHgrow(lsp, Priority.ALWAYS);
        Label browse = lbl("Browse full log ⤢", "jl-clearbtn"); browse.setStyle("-fx-cursor:hand;");
        HBox lbar = new HBox(8, lh, ln, lsp, browse); lbar.getStyleClass().add("jl-loghd"); lbar.setAlignment(Pos.CENTER_LEFT);
        VBox tbl = new VBox();

        TextField[] all = {call, name, grid, qth, stat, cnty, cmt, iota, sota, pota};
        long[] editId = {-1};
        String[] lastLk = {""};
        Runnable[] refresh = new Runnable[1];

        Runnable exitEdit = () -> {
            editId[0] = -1; titleLbl.setText("New QSO"); logBtn.setText("Log QSO  ⏎");
            cancel.setVisible(false); cancel.setManaged(false);
            manualTime[0] = false; dateF.setText(nowDate()); timeF.setText(nowTime());
            cQsl.setSelected(false); cLotw.setSelected(false); cEqsl.setSelected(false); baseRefs[0] = com.ars.fx.data.JLogDb.Refs.NONE;
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
            iota.setText(z(f.refs().iota())); sota.setText(z(f.refs().sota())); pota.setText(z(f.refs().pota()));
            cQsl.setSelected(f.refs().qsl()); cLotw.setSelected(f.refs().lotw()); cEqsl.setSelected(f.refs().eqsl()); baseRefs[0] = f.refs();
            String dt = f.datetimeUtc();
            dateF.setText(dt != null && dt.length() >= 10 ? dt.substring(0, 10) : nowDate());
            timeF.setText(dt != null && dt.length() >= 19 ? dt.substring(11, 19) : nowTime());
            manualTime[0] = true;
            titleLbl.setText("Edit QSO #" + id); logBtn.setText("Update  ⏎");
            cancel.setVisible(true); cancel.setManaged(true);
            call.requestFocus();
        };
        Consumer<Long> onDelete = id -> {
            JLogDb.QsoFull gone = JLogDb.byId(id);
            if (JLogDb.delete(id)) {
                if (gone != null) com.ars.fx.data.N1mmBroadcast.normalDeleted(com.ars.fx.data.N1mmBroadcast.of(gone));
                if (editId[0] == id) exitEdit.run();
                refresh[0].run();
                sub.setText(JLogDb.count() + " in log · " + JLogDb.countToday() + " today");
            }
        };
        refresh[0] = () -> { fillLog(tbl, ln, onEdit, onDelete); LogBrowser.refreshIfOpen(); };
        refresh[0].run();
        browse.setOnMouseClicked(e -> LogBrowser.show(browse, onEdit, () -> {
            if (editId[0] >= 0 && JLogDb.byId(editId[0]) == null) exitEdit.run();   // the QSO being edited was deleted
            refresh[0].run();
            sub.setText(JLogDb.count() + " in log · " + JLogDb.countToday() + " today");
        }));

        Runnable doLog = () -> {
            if (call.getText() == null || call.getText().isBlank()) { call.requestFocus(); return; }
            int p = 0; try { p = Integer.parseInt(pwr.getText().trim()); } catch (Exception ignored) {}
            String bandVal = (band.getText() == null || band.getText().isBlank()) ? JLogDb.bandFor(freq.getText()) : band.getText();
            String dt = manualTime[0] ? composeDt(dateF.getText(), timeF.getText()) : null;
            com.ars.fx.data.JLogDb.Refs refs = new com.ars.fx.data.JLogDb.Refs(iota.getText(), sota.getText(), pota.getText(),
                    baseRefs[0].cqz(), baseRefs[0].ituz(), cQsl.isSelected(), cLotw.isSelected(), cEqsl.isSelected());
            JLogDb.QsoFull before = editId[0] >= 0 ? JLogDb.byId(editId[0]) : null;
            boolean ok = (editId[0] >= 0)
                    ? JLogDb.update(editId[0], dt, call.getText(), freq.getText(), bandVal, mode.getText(),
                            rsnt.getText(), rrcv.getText(), name.getText(), qth.getText(), grid.getText(), cmt.getText(), stat.getText(), cnty.getText(), p, refs)
                    : JLogDb.insertAt(dt, call.getText(), freq.getText(), bandVal, mode.getText(),
                            rsnt.getText(), rrcv.getText(), name.getText(), qth.getText(), grid.getText(), cmt.getText(), stat.getText(), cnty.getText(), p, refs);
            if (ok) {
                long rowId = editId[0] >= 0 ? editId[0] : JLogDb.maxId();
                JLogDb.QsoFull saved = JLogDb.byId(rowId);
                if (saved != null) {
                    if (editId[0] >= 0) com.ars.fx.data.N1mmBroadcast.normalEdited(com.ars.fx.data.N1mmBroadcast.of(saved), before == null ? saved.call() : before.call());
                    else com.ars.fx.data.N1mmBroadcast.normalAdded(com.ars.fx.data.N1mmBroadcast.of(saved));
                }
                if (editId[0] < 0) printQsl.afterSave(new com.ars.fx.data.QslLabel.Qso(call.getText(), dt, freq.getText(), bandVal,
                        mode.getText(), rsnt.getText(), rrcv.getText(), name.getText(), qth.getText()));
                exitEdit.run();
                for (TextField t : all) t.clear();
                b4.setText(idleB4); b4.setStyle(""); lastLk[0] = "";
                call.requestFocus();
                refresh[0].run();
                sub.setText(JLogDb.count() + " in log · " + JLogDb.countToday() + " today");
            }
        };
        logBtn.setOnMouseClicked(e -> doLog.run());
        call.setOnAction(e -> doLog.run());
        cmt.setOnAction(e -> doLog.run());
        Runnable clearForm = () -> { exitEdit.run(); for (TextField t : all) t.clear(); b4.setText(idleB4); b4.setStyle(""); lastLk[0] = ""; };
        clearRef = clearForm;
        clear.setOnMouseClicked(e -> { clearForm.run(); call.requestFocus(); });
        cancel.setOnMouseClicked(e -> { exitEdit.run(); for (TextField t : all) t.clear(); call.requestFocus(); });

        // Worked-before/dupe indicator + callbook autofill for the current call (both off-thread).
        // Shared by the call-field exit AND the spot-click prefill.
        Runnable doCallLookup = () -> {
            String c = call.getText() == null ? "" : call.getText().trim().toUpperCase();
            if (c.length() < 3) { b4.setText(idleB4); b4.setStyle(""); return; }
            if (c.equals(lastLk[0])) return;
            lastLk[0] = c;
            String curBand = band.getText() == null ? "" : band.getText().trim();
            boolean lookupOn = com.ars.fx.data.LookupConfig.anyEnabled();
            Thread t = new Thread(() -> {
                JLogDb.Prior prior = JLogDb.priorWith(c);
                com.ars.fx.data.CallsignLookup.Result r = lookupOn ? com.ars.fx.data.CallsignLookup.lookup(c) : null;
                Platform.runLater(() -> {
                    updateB4(b4, prior, curBand);
                    boolean sameCall = c.equals(call.getText() == null ? "" : call.getText().trim().toUpperCase());   // a slow reply must not land on the next spot
                    if (r != null && sameCall) { fillIfEmpty(name, r.name()); fillIfEmpty(grid, r.grid()); fillIfEmpty(qth, r.city());
                        fillIfEmpty(stat, r.state()); fillIfEmpty(cnty, r.county()); }
                });
            }, "callsign-lookup");
            t.setDaemon(true); t.start();
        };
        lookupRef = doCallLookup;
        call.focusedProperty().addListener((o, was, now) -> { if (!now) doCallLookup.run(); });

        VBox entry = embedded ? new VBox(0, head, body) : new VBox(0, head, LogDbBar.build(false), body); entry.getStyleClass().add("jl-entry"); entry.setMaxWidth(940);
        // F1–F8 keyboard shortcuts for the macros (registered once the entry is on a scene).
        entry.sceneProperty().addListener((o, oldS, s) -> {
            if (s == null) return;
            KeyCode[] fkeys = {KeyCode.F1, KeyCode.F2, KeyCode.F3, KeyCode.F4, KeyCode.F5, KeyCode.F6, KeyCode.F7, KeyCode.F8};
            for (int i = 0; i < com.ars.fx.data.MacroConfig.count() && i < fkeys.length; i++) {
                final int idx = i;
                s.getAccelerators().put(new KeyCodeCombination(fkeys[i]), () -> fireMacro.accept(idx));
            }
        });
        Node txrx = rttyCwDrawer(rx, transmit);
        if (embedded) {   // merged page: the TX drawer lives in the dashboard's side column, the form fills its own width
            embeddedTx = txrx;
            VBox col = new VBox(entry, lbar, tbl); col.setStyle("-fx-background-color:-ars-bg;");
            return col;
        }
        ((Region) txrx).setMaxWidth(940); VBox txWrap = new VBox(txrx); txWrap.setMaxWidth(940); txWrap.setPadding(new Insets(11, 14, 0, 14));
        lbar.setMaxWidth(940); tbl.setMaxWidth(940);
        VBox col = new VBox(entry, txWrap, lbar, tbl); col.setStyle("-fx-background-color:-ars-bg;");
        return col;
    }
    // F-key macros are defined in J-Hub ▸ Data (MacroConfig); J-Log just renders + fires them.
    /** Expand {CALL} in a macro from the current callsign field (blank → "OM"). */
    /** Phone modes play the voice keyer; everything else sends macro text. */
    private static boolean isPhone(String mode) {
        if (mode == null) return false;
        String m = mode.trim().toUpperCase();
        return m.equals("USB") || m.equals("LSB") || m.contains("SSB") || m.equals("AM") || m.equals("FM")
                || m.contains("PHONE") || m.contains("VOICE");
    }
    // ---- RTTY / CW transmit & receive drawer (between entry pane and log) ---
    // digital TX mode chips: {label, rate descriptor}
    private static String cwRate() { return com.ars.fx.data.HubConfig.get("tx.cwWpm", "25").trim() + " wpm"; }
    private static final String[][] TX_MODES = {{"CW", null},{"RTTY","45 baud · 170 Hz"},{"PSK31","31 baud"}};
    private static String txRate(int i) { return TX_MODES[i][1] == null ? cwRate() : TX_MODES[i][1]; }
    private static TextArea txRxArea() {
        TextArea rx = new TextArea(); rx.setPromptText("What you send from here and the macro keyer shows up here.");
        rx.setEditable(false); rx.setWrapText(true); rx.setPrefRowCount(5);
        rx.setStyle("-fx-control-inner-background:#0b0f14; -fx-background-color:#0b0f14; -fx-text-fill:-ars-ok; -fx-font-family:'JetBrains Mono'; -fx-font-size:12px; -fx-border-color:-ars-border; -fx-border-radius:6; -fx-background-radius:6;");
        return rx;
    }
    private static Node rttyCwDrawer(TextArea rx, Consumer<String> transmit) {
        Label[] chips = new Label[TX_MODES.length];
        Label rate = lbl(txRate(0), "jl-skim-meta");
        HBox modeRow = new HBox(8); modeRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < TX_MODES.length; i++) {
            final int idx = i;
            Label chip = lbl(TX_MODES[i][0], "jl-chip"); if (i == 0) chip.getStyleClass().add("accent");
            chip.setStyle("-fx-cursor:hand;");
            chip.setOnMouseClicked(e -> {
                for (Label c : chips) c.getStyleClass().remove("accent");
                chip.getStyleClass().add("accent");
                rate.setText(txRate(idx));
            });
            chips[i] = chip; modeRow.getChildren().add(chip);
        }
        Region msp = new Region(); HBox.setHgrow(msp, Priority.ALWAYS);
        modeRow.getChildren().addAll(msp, rate);

        TextField tx = new TextField(); Shell.caps(tx); tx.setPromptText("type to transmit…"); tx.getStyleClass().add("jhub-mini-field"); HBox.setHgrow(tx, Priority.ALWAYS); tx.setMaxWidth(Double.MAX_VALUE);
        Label stop = lbl("Stop", "jl-clearbtn"); stop.setStyle("-fx-cursor:hand;");
        Label send = lbl("Send ⏎", "jl-logbtn"); send.setStyle("-fx-cursor:hand;");
        Runnable sendTx = () -> { if (!tx.getText().isBlank()) { transmit.accept(tx.getText()); tx.clear(); } };
        send.setOnMouseClicked(e -> sendTx.run());
        tx.setOnAction(e -> sendTx.run());
        stop.setOnMouseClicked(e -> { tx.clear(); rx.appendText("■ TX stopped\n"); });
        HBox txRow = new HBox(10, tx, stop, send); txRow.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(9, modeRow, rx, txRow); body.setPadding(new Insets(4, 2, 4, 2));
        return Shell.drawer("RTTY / CW · TX & RX", "digi", "digi", "CW · " + cwRate(), false, body);
    }
    private static void fillIfEmpty(TextField f, String v) {
        if (v != null && !v.isBlank() && (f.getText() == null || f.getText().isBlank())) f.setText(v);
    }
    private static TextField tf(String prompt, String init, double w, boolean call) {
        TextField t = new TextField(init == null ? "" : init);
        if (prompt != null && !prompt.isEmpty()) t.setPromptText(prompt);
        t.getStyleClass().add("jhub-mini-field"); if (call) t.getStyleClass().add("call");
        if (w > 0) { t.setMinWidth(w); t.setPrefWidth(w); t.setMaxWidth(w); } else t.setMaxWidth(Double.MAX_VALUE);
        return Shell.caps(t);   // all log entry is upper case
    }
    private static VBox g(String label, Node field, boolean grow) {
        VBox v = new VBox(5, lbl(label, "jl-flabel"), field); v.setAlignment(Pos.TOP_LEFT);
        if (grow) { HBox.setHgrow(v, Priority.ALWAYS); v.setMaxWidth(Double.MAX_VALUE); }
        return v;
    }
    private static HBox row(VBox... cells) { HBox r = new HBox(12, cells); r.setAlignment(Pos.BOTTOM_LEFT); return r; }

    // columns: DATE, UTC, CALL, FREQ, BAND, MODE, RST, NAME, [edit/del actions]
    private static final double[] LOGC = {88, 52, 112, 84, 52, 68, 50, -1, 58};   // only NAME flexes, so header and rows share one grid
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
        if (LOGC[i] < 0) { HBox.setHgrow(cell, Priority.ALWAYS); cell.setMaxWidth(Double.MAX_VALUE); cell.setMinWidth(0); cell.setPrefWidth(0); }
        else { cell.setMinWidth(LOGC[i]); cell.setPrefWidth(LOGC[i]); cell.setMaxWidth(LOGC[i]); }
        r.getChildren().add(cell);
    }

    // ---- right rail --------------------------------------------------------
    private static Node[] railDrawers() {
        List<Node> rail = new ArrayList<>();
        rail.addAll(Shell.leadDrawers());     // Station ID, Rig control, Rotor control
        rail.add(heardByDrawer());
        rail.add(awardsDrawer());
        rail.add(skimmerDrawer());
        rail.addAll(Shell.instruments(50));      // propagation, solar, band conditions, weather
        return rail.toArray(new Node[0]);
    }
    private static Region spotRow(com.ars.fx.data.ClusterClient.Spot s) {
        Label fq = lbl(com.ars.fx.data.ClusterClient.fmtFreq(s.freqHz()), "jl-skim-fq"); fq.setMinWidth(58);
        Label cl = lbl(s.call(), "jl-skim-cl"); HBox.setHgrow(cl, Priority.ALWAYS); cl.setMaxWidth(Double.MAX_VALUE);
        Label md = lbl(s.mode(), "jl-mode");
        Label age = lbl(com.ars.fx.data.ClusterClient.age(s.arrivalMs()), "jl-skim-meta");
        HBox top = new HBox(9, fq, cl, md, age); top.setAlignment(Pos.CENTER_LEFT);
        Node pc = com.ars.fx.data.PathNow.shown() ? com.ars.fx.shell.PathChip.of(com.ars.fx.data.PathNow.forSpot(s.call(), s.freqHz())) : null;
        if (pc != null) top.getChildren().add(3, pc);
        String tail = "de " + s.de() + (s.comment().isEmpty() ? "" : " · " + s.comment());
        HBox bot = new HBox(9, lbl(s.band(), "jl-skim-meta"), lbl(tail, "jl-skim-meta")); bot.setAlignment(Pos.CENTER_LEFT); HBox.setMargin(bot, new Insets(2, 0, 0, 0));
        VBox row = new VBox(0, top, bot); row.setPadding(new Insets(7, 0, 7, 0));
        row.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(row, "Click to use this spot — fills the log entry, tunes the rig, and offers to turn the beam");
        row.setOnMouseClicked(e -> SpotAction.onSpot(row, s.call(), s.freqHz(), s.mode(), s.band()));
        return row;
    }

    /** "Heard By" — reverse-beacon spots of our own call, pulled from RBN. */
    private static Node heardByDrawer() {
        com.ars.fx.data.HeardByClient hb = com.ars.fx.data.HeardByClient.getInstance();
        Region cdot = new Region(); cdot.getStyleClass().add("jhub-mini-dot"); cdot.setMinSize(8, 8); cdot.setMaxSize(8, 8);
        cdot.setStyle("-fx-background-color:-ars-t4;-fx-background-radius:99;");
        Region ssp = new Region(); HBox.setHgrow(ssp, Priority.ALWAYS);
        Label act = lbl(FeedUi.action(hb), "jl-clearbtn"); act.setStyle("-fx-cursor:hand;");
        act.setOnMouseClicked(e -> { FeedUi.toggle(hb); act.setText(FeedUi.action(hb)); });
        Label state = lbl(FeedUi.line(hb), "jl-skim-meta");
        HBox src = new HBox(6, cdot, state, ssp, act); src.setAlignment(Pos.CENTER_LEFT);

        VBox rows = new VBox();
        Label empty = lbl(FeedUi.emptyList(hb, "RBN"), "jl-skim-meta"); empty.setWrapText(true);
        empty.setPadding(new Insets(8, 2, 8, 2));
        rows.getChildren().add(empty);
        VBox body = new VBox(8, src, rows); body.setPadding(new Insets(2, 2, 4, 2));

        hb.setListener(c -> Shell.keepScroll(rows, () -> {
            cdot.setStyle(FeedUi.dot(c.status()));
            state.setText(FeedUi.line(c)); act.setText(FeedUi.action(c));
            rows.getChildren().clear();
            List<com.ars.fx.data.HeardByClient.HeardSpot> list = c.spots();
            if (list.isEmpty()) {
                empty.setText(c.isConnected() ? "Connected to RBN — waiting to be heard (" + c.myCall() + ")…" : FeedUi.emptyList(c, "RBN"));
                rows.getChildren().add(empty);
            } else {
                for (com.ars.fx.data.HeardByClient.HeardSpot s : list) rows.getChildren().add(heardRow(s));
            }
        }));
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
        row.setStyle("-fx-border-color: transparent transparent -ars-border transparent; -fx-border-width: 0 0 1 0;-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(row, "Click to use this skimmer's frequency — tunes the rig and offers to turn the beam");
        row.setOnMouseClicked(e -> SpotAction.onSpot(row, s.spotter(), s.freqHz(), s.mode(), s.band()));
        return row;
    }
    /** Config key: comma-separated award codes the drawer shows. Unset = the default view (DXCC/WAS/WAC always,
     *  the others once something is logged); set = exactly those, even with nothing logged yet. */
    private static final String AWARDS_KEY = "awards.drawer";

    /** The codes the operator picked for the drawer, or null for the default view. */
    private static java.util.Set<String> pickedAwards() {
        String v = com.ars.fx.data.HubConfig.get(AWARDS_KEY, null);
        if (v == null) return null;
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        for (String c : v.split(",")) if (!c.isBlank()) out.add(c.trim());
        return out;
    }

    /** Awards drawer - a line per tracked award (worked / confirmed of target) from the real log, a button for the full
     *  awards screen, and "Customize" to choose which awards are tracked here. */
    private static Node awardsDrawer() {
        VBox body = new VBox(7);
        body.setPadding(new Insets(2, 2, 4, 2));
        Node dw = Shell.drawer("Awards", "vault", "vault", "", true, body);
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            body.getChildren().setAll(lbl("Working out awards…", "jl-skim-meta"));
            Thread t = new Thread(() -> {
                List<com.ars.fx.data.JLogDb.QsoExport> logRows = com.ars.fx.data.JLogDb.allForExport();
                com.ars.fx.data.Awards.Snapshot snap = com.ars.fx.data.Awards.compute(logRows);
                // the operator's own award sheets (My awards) sit beside the built-in awards
                List<com.ars.fx.data.Awards.Progress> all = new ArrayList<>(snap.all());
                java.util.Set<String> defaultOn = new java.util.HashSet<>(List.of(snap.dxcc.code(), snap.was.code(), snap.wac.code()));
                for (com.ars.fx.data.Awards.Progress p : snap.all()) if (p.worked() > 0) defaultOn.add(p.code());
                for (com.ars.fx.data.CustomAwards.Def d : com.ars.fx.data.CustomAwards.list()) {
                    com.ars.fx.data.CustomAwards.Result res = com.ars.fx.data.CustomAwards.compute(d, logRows);
                    com.ars.fx.data.Awards.Progress p = new com.ars.fx.data.Awards.Progress("my:" + d.id, d.name, res.worked, res.confirmed, res.total(),
                            java.util.Set.of(), java.util.Set.of(), null);
                    all.add(p); defaultOn.add(p.code());
                }
                Platform.runLater(() -> {
                    body.getChildren().clear();
                    java.util.Set<String> picked = pickedAwards();
                    for (com.ars.fx.data.Awards.Progress p : all) {
                        if (picked != null) { if (!picked.contains(p.code())) continue; }
                        else if (!defaultOn.contains(p.code())) continue;   // default: hide awards with nothing logged yet
                        body.getChildren().add(awardLine(p));
                    }
                    if (body.getChildren().isEmpty()) body.getChildren().add(lbl("No awards tracked - use Customize.", "jl-skim-meta"));
                    Label open = lbl("Open awards ⤢", "jl-clearbtn"); open.setStyle("-fx-cursor:hand;");
                    open.setOnMouseClicked(e -> AwardsWindow.show(open));
                    Label cust = lbl("Customize…", "jl-clearbtn"); cust.setStyle("-fx-cursor:hand;");
                    cust.setOnMouseClicked(e -> customizeAwards(cust, all, defaultOn, refresh[0]));
                    HBox links = new HBox(14, open, cust); links.setAlignment(Pos.CENTER_LEFT);
                    body.getChildren().add(links);
                    if (dw.lookup(".sx-dw-sum") instanceof Label sum) sum.setText(snap.dxcc.worked() + " DXCC · " + snap.was.worked() + "/" + snap.was.target() + " WAS");
                });
            }, "awards-drawer");
            t.setDaemon(true); t.start();
        };
        refresh[0].run();
        return dw;
    }

    /** Pop-up with one tick per award: choose which ones the Awards drawer tracks. */
    private static void customizeAwards(Node owner, List<com.ars.fx.data.Awards.Progress> all, java.util.Set<String> defaultOn, Runnable onSaved) {
        java.util.Set<String> picked = pickedAwards();
        VBox box = new VBox(8); box.setPadding(new Insets(14));
        box.getChildren().add(lbl("Show these awards in the drawer", "jhub-card-title"));
        List<CheckBox> boxes = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        for (com.ars.fx.data.Awards.Progress p : all) {
            boolean on = picked != null ? picked.contains(p.code()) : defaultOn.contains(p.code());
            CheckBox cb = new CheckBox(p.code().startsWith("my:") ? p.name() + " (my award)" : p.code() + " — " + p.name()); cb.setSelected(on);
            boxes.add(cb); codes.add(p.code()); box.getChildren().add(cb);
        }
        Label reset = lbl("Reset to default", "jl-clearbtn"); reset.setStyle("-fx-cursor:hand;");
        Label save  = lbl("Save", "jl-clearbtn");            save.setStyle("-fx-cursor:hand;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox btns = new HBox(14, reset, sp, save); btns.setAlignment(Pos.CENTER_LEFT); btns.setPadding(new Insets(8, 0, 0, 0));
        box.getChildren().add(btns);
        javafx.scene.Scene scene = new javafx.scene.Scene(box);
        com.ars.fx.Theme.apply(scene, false); com.ars.fx.shell.Tips.install(scene);
        com.ars.fx.Themes.applyTo(box, "log");
        javafx.stage.Stage st = new javafx.stage.Stage(); st.setTitle("Awards — customize"); st.setScene(scene);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) st.initOwner(owner.getScene().getWindow());
        reset.setOnMouseClicked(e -> { com.ars.fx.data.HubConfig.remove(AWARDS_KEY); st.close(); onSaved.run(); });
        save.setOnMouseClicked(e -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < boxes.size(); i++) if (boxes.get(i).isSelected()) sb.append(sb.length() > 0 ? "," : "").append(codes.get(i));
            com.ars.fx.data.HubConfig.set(AWARDS_KEY, sb.toString());
            st.close(); onSaved.run();
        });
        st.show();
    }
    private static Node awardLine(com.ars.fx.data.Awards.Progress p) {
        StackPane bar = new StackPane(); bar.setMinHeight(6); bar.setMaxHeight(6); bar.setMaxWidth(Double.MAX_VALUE);
        Region track = new Region(); track.getStyleClass().add("jl-award-bar-track"); track.setMinHeight(6); track.setMaxHeight(6); track.setMaxWidth(Double.MAX_VALUE);
        Region fill = new Region(); fill.getStyleClass().add("jl-award-bar-fill"); fill.setMinHeight(6); fill.setMaxHeight(6);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT); bar.getChildren().addAll(track, fill);
        fill.maxWidthProperty().bind(bar.widthProperty().multiply(p.fraction()));
        boolean mine = p.code().startsWith("my:");
        Label name = lbl(mine ? p.name() : p.code(), "jl-skim-meta"); name.setMinWidth(mine ? 96 : 40); name.setMaxWidth(mine ? 130 : 60);
        Label cnt = lbl(p.worked() + " / " + p.confirmed() + "✓ of " + p.target(), "jl-award-pct");
        HBox top = new HBox(8, name, bar, cnt); HBox.setHgrow(bar, Priority.ALWAYS); top.setAlignment(Pos.CENTER_LEFT);
        com.ars.fx.shell.Tips.tip(top, p.name() + " — " + p.worked() + " worked, " + p.confirmed() + " confirmed");
        return top;
    }

    private static Runnable skimmerRefresh;
    /** Skimmer drawer — the callsigns the local CW skimmer has decoded. They arrive as spots from "<mycall>-SKM" in the shared
     *  list, so this is that list filtered to the skimmer; it fills in as spots come in. */
    private static Node skimmerDrawer() {
        VBox rows = new VBox();
        VBox dw = Shell.drawer("Skimmer", "digi", "digi", "", true, rows);
        Runnable fill = () -> {
            boolean on = com.ars.fx.data.HubConfig.getBool("digi.localSkimmer", false);
            List<com.ars.fx.data.ClusterClient.Spot> heard = new ArrayList<>();
            for (com.ars.fx.data.ClusterClient.Spot s : com.ars.fx.data.ClusterClient.getInstance().spots())
                if (s.de() != null && s.de().toUpperCase().endsWith("-SKM") && heard.size() < 12) heard.add(s);
            Shell.keepScroll(rows, () -> {
                rows.getChildren().clear();
                if (heard.isEmpty()) {
                    Label none = lbl(on ? "Listening — no callsigns decoded yet." : "The local CW skimmer is off. Switch it on in J-Hub ▸ Data ▸ WSJT-X / Digital.", "jl-skim-meta");
                    none.setWrapText(true); none.setPadding(new Insets(8, 2, 8, 2));
                    rows.getChildren().add(none);
                } else for (com.ars.fx.data.ClusterClient.Spot s : heard) rows.getChildren().add(spotRow(s));
            });
            if (dw.lookup(".sx-dw-sum") instanceof Label sum) sum.setText(!on ? "off" : heard.size() + " decodes");
        };
        skimmerRefresh = fill;
        com.ars.fx.data.ClusterClient.getInstance().addListener("jlog.skimmer", c -> fill.run());   // its callsigns arrive through the same feed
        fill.run();
        return dw;
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
