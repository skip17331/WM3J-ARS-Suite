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
import static com.ars.fx.surface.JLogContest.*;
import static com.ars.fx.surface.JLogContestSend.*;
import static com.ars.fx.surface.JLogContestLog.*;

/** J-Log contest page: the score readout: tiles, the clock, the QSO-party side chip, times, and what is left to work. Split out of {@link JLogContest}. */
final class JLogContestScore {
    private JLogContestScore() {}

    // ---- score readout ----------------------------------------------------
    static String[][] scoreStats() {
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
        out.addAll(layoutTiles(p));
        String[] gaps = gapTexts();                                     // the time between contacts: always the last two tiles
        out.add(new String[]{"SINCE LAST QSO", gaps[0]});
        out.add(new String[]{"LAST GAP", gaps[1]});
        return out.toArray(new String[0][]);
    }

    /** The extra tiles the contest's layout asks for ({@code tiles}). */
    static List<String[]> layoutTiles(ContestPlugin p) {
        if (p == null) return List.of();
        List<String> kinds = ContestLayout.resolve(p).tiles();
        if (kinds.isEmpty()) return List.of();
        return com.ars.fx.data.ContestTiles.compute(kinds, ContestState.qsos(), ContestState.score(), ContestState.workedMults());
    }

    /** "CONTEST CLOCK": time to the start / left, and off time so far; only once the operator has set the contest's times. */
    static String[] clockTile(ContestPlugin p) {
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
    static Node sideChip(ContestPlugin plugin) {
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
    static void openLayoutFile(ContestPlugin plugin) {
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

    static void editTimes(Node owner, ContestPlugin plugin) {
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
    static String leftLabel(ContestPlugin p) {
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
    static List<String> universe(ContestPlugin p) {
        if (QsoPartySide.isParty(p)) {
            QsoPartySide.Side sd = QsoPartySide.side(p);
            if (sd != null) { List<String> u = QsoPartySide.multUniverse(p, sd); if (u != null) return u; }
        }
        return com.ars.fx.surface.contest.TrackerFactory.multiplierUniverse(p);
    }
    static String[] leftTile(ContestPlugin p) {
        String label = leftLabel(p);
        return label == null ? null : new String[]{label, leftText(p)};
    }
    /** "12 sections" / "CLEAN SWEEP!" - for contests that count multipliers per band, the ones left on the band in use. */
    static String leftText(ContestPlugin p) {
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
    static void refreshScoreStats() {
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
        int extraAt = clockAt + (clock != null ? 1 : 0);
        List<String[]> extra = layoutTiles(ContestState.plugin());
        for (int i = 0; i < extra.size() && extraAt + i < vs.size(); i++) vs.get(extraAt + i).setText(extra.get(i)[1]);
        int gapAt = extraAt + extra.size();
        if (vs.size() > gapAt + 1) { String[] g = gapTexts(); vs.get(gapAt).setText(g[0]); vs.get(gapAt + 1).setText(g[1]); }
    }
    /** {since the last QSO, the gap before it} as clock text, from the contest log (a dupe is still a contact). */
    static String[] gapTexts() {
        List<LocalDateTime> times = new ArrayList<>();
        for (QsoRecord q : ContestState.qsos()) times.add(q.getDateTimeUtc());
        return new String[]{com.ars.fx.data.QsoGaps.fmt(com.ars.fx.data.QsoGaps.sinceLast(times, LocalDateTime.now(ZoneOffset.UTC))),
                com.ars.fx.data.QsoGaps.fmt(com.ars.fx.data.QsoGaps.lastGap(times))};
    }
    static void collectStatV(Node n, List<Label> out) {
        if (n instanceof Label l && (l.getStyleClass().contains("hero-stat-v") || l.getStyleClass().contains("sx-stat-v"))) out.add(l);
        if (n instanceof Parent p) for (Node c : p.getChildrenUnmodifiable()) collectStatV(c, out);
    }

}
