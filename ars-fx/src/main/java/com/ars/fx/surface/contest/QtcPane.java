package com.ars.fx.surface.contest;

import com.ars.fx.data.ContestState;
import com.ars.fx.data.DxccInfo;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.QtcRules;
import com.ars.fx.shell.Shell;
import com.ars.fx.surface.Confirm;
import com.jlog.db.ContestQtcDao;
import com.jlog.model.QsoRecord;
import com.jlog.model.QtcRecord;
import com.jlog.plugin.ContestPlugin;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The WAE QTC traffic pane: log a series of up to ten QTCs (the time, call and serial of QSOs already made) sent to or received from
 * a station on the other side of the Europe / non-Europe line. Each saved QTC is one point, added to the score. The checks are
 * {@link QtcRules}.
 */
public final class QtcPane {
    private QtcPane() {}

    private static final int ROWS = 10;

    public static Region build(ContestPlugin plugin) {
        String cid = plugin.getContestId();
        String own = ownContinent();
        ComboBox<String> dir = new ComboBox<>(); dir.getItems().addAll("Received", "Sent");
        dir.setValue(QtcRules.SENT.equals(QtcRules.defaultDirection(own)) ? "Sent" : "Received");
        TextField partner = field("station", 110), band = field("band", 60), mode = field("mode", 60);
        QsoRecord last = null;
        try { List<QsoRecord> all = ContestState.qsos(); if (!all.isEmpty()) last = all.get(0); } catch (RuntimeException ignored) {}
        if (last != null) { partner.setText(last.getCallsign()); band.setText(last.getBand() == null ? "" : last.getBand()); mode.setText(last.getMode() == null ? "" : last.getMode()); }
        else if (plugin.getLockedMode() != null) mode.setText(plugin.getLockedMode());

        Label seriesL = lbl("", "ct-note"), msg = lbl("", "ct-note"), totals = lbl("", "ct-note"); msg.setWrapText(true); msg.setMaxWidth(300);
        GridPane grid = new GridPane(); grid.setHgap(6); grid.setVgap(3);
        grid.add(lbl("TIME", "jl-flabel"), 0, 0); grid.add(lbl("CALL", "jl-flabel"), 1, 0); grid.add(lbl("SERIAL", "jl-flabel"), 2, 0);
        List<TextField[]> rows = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) {
            TextField t = field("HHMM", 54), c = field("call", 100), s = field("nr", 54);
            rows.add(new TextField[]{t, c, s});
            grid.add(t, 0, i + 1); grid.add(c, 1, i + 1); grid.add(s, 2, i + 1);
        }
        VBox history = new VBox(2);
        Runnable[] again = new Runnable[1];                    // refresh, for the handlers built inside it
        int[] editNo = {0};                                    // the series being corrected (0 = a new one)
        String[] editDir = {null};

        Runnable refresh = () -> {
            List<QtcRecord> ex = existing(cid);
            String d = dirKey(dir);
            seriesL.setText(editNo[0] > 0 ? "Editing QTC series " + editNo[0] : "QTC series " + QtcRules.nextSeries(ex, d));
            long sent = ex.stream().filter(r -> QtcRules.SENT.equalsIgnoreCase(r.getDirection())).count(), rcvd = ex.size() - sent;
            totals.setText(sent + " sent + " + rcvd + " received = " + ex.size() + " QTC point" + (ex.size() == 1 ? "" : "s"));
            history.getChildren().clear();
            Map<String, List<QtcRecord>> series = new LinkedHashMap<>();
            for (QtcRecord r : ex) series.computeIfAbsent(r.getDirection() + " " + r.getSeriesNo() + " " + r.getPartnerCall(), k -> new ArrayList<>()).add(r);
            List<String> keys = new ArrayList<>(series.keySet());
            for (int i = Math.max(0, keys.size() - 6); i < keys.size(); i++) {
                List<QtcRecord> g = series.get(keys.get(i)); QtcRecord f = g.get(0);
                Label row = lbl(("SENT".equalsIgnoreCase(f.getDirection()) ? "→ " : "← ") + f.getPartnerCall() + "  series " + f.getSeriesNo() + "  ·  " + g.size() + " QTC" + (g.size() == 1 ? "" : "s"), "ct-note");
                Label edit = lbl("Edit", "jl-clearbtn"), del = lbl("Delete", "jl-clearbtn");
                edit.setStyle("-fx-cursor:hand;"); del.setStyle("-fx-cursor:hand;");
                edit.setOnMouseClicked(e -> {                                    // load the series into the form; Save replaces it
                    dir.setValue("SENT".equalsIgnoreCase(f.getDirection()) ? "Sent" : "Received");
                    partner.setText(f.getPartnerCall()); band.setText(f.getQtcBand() == null ? "" : f.getQtcBand()); mode.setText(f.getQtcMode() == null ? "" : f.getQtcMode());
                    for (int r = 0; r < ROWS; r++) {
                        QtcRecord q = r < g.size() ? g.get(r) : null;
                        rows.get(r)[0].setText(q == null ? "" : nz(q.getQsoTime())); rows.get(r)[1].setText(q == null ? "" : nz(q.getQsoCall())); rows.get(r)[2].setText(q == null ? "" : nz(q.getQsoSerial()));
                    }
                    editNo[0] = f.getSeriesNo(); editDir[0] = f.getDirection().toUpperCase();
                    msg.setText("Editing series " + f.getSeriesNo() + " - Save replaces it, Clear drops the change."); msg.setStyle("");
                    again[0].run();
                });
                del.setOnMouseClicked(e -> Confirm.show(del, "Delete this QTC series?",
                        "Delete series " + f.getSeriesNo() + " with " + f.getPartnerCall() + " (" + g.size() + " QTC" + (g.size() == 1 ? "" : "s") + ")? Each is a point, so the score drops by " + g.size() + ".",
                        new Confirm.Choice("Keep", false, null),
                        new Confirm.Choice("Delete", true, () -> {
                            try { ContestQtcDao.getInstance().deleteSeries(cid, f.getDirection(), f.getSeriesNo()); }
                            catch (Exception ex3) { msg.setText("Could not delete: " + ex3.getMessage()); msg.setStyle("-fx-text-fill:-ars-err;"); return; }
                            if (editNo[0] == f.getSeriesNo() && f.getDirection().equalsIgnoreCase(editDir[0])) { editNo[0] = 0; editDir[0] = null; }
                            msg.setText("Deleted series " + f.getSeriesNo() + "."); msg.setStyle("");
                            again[0].run(); ContestState.recompute();
                        })));
                Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
                HBox line = new HBox(8, row, gap, edit, del); line.setAlignment(Pos.CENTER_LEFT);
                history.getChildren().add(line);
            }
        };
        again[0] = refresh;
        dir.setOnAction(e -> refresh.run());

        Label save = lbl("Save series", "jl-logbtn"), clear = lbl("Clear", "jl-clearbtn");
        save.setStyle("-fx-cursor:hand;"); clear.setStyle("-fx-cursor:hand;");
        clear.setOnMouseClicked(e -> { for (TextField[] r : rows) for (TextField t : r) t.clear(); msg.setText(""); editNo[0] = 0; editDir[0] = null; refresh.run(); });
        save.setOnMouseClicked(e -> {
            List<QtcRules.Line> lines = new ArrayList<>();
            for (TextField[] r : rows) if (!(r[0].getText().isBlank() && r[1].getText().isBlank() && r[2].getText().isBlank()))
                lines.add(new QtcRules.Line(r[0].getText().trim(), r[1].getText().trim(), r[2].getText().trim()));
            String d = dirKey(dir), p = partner.getText().trim().toUpperCase();
            boolean editing = editNo[0] > 0;
            List<QtcRecord> ex = existing(cid);
            if (editing) ex.removeIf(r -> r.getSeriesNo() == editNo[0] && editDir[0] != null && editDir[0].equalsIgnoreCase(r.getDirection()));   // the series being corrected does not count against itself
            String problem = QtcRules.validate(own, p, QtcRules.continentOf(p, DxccInfo::of), d, lines, ex);
            if (problem != null) { msg.setText(problem); msg.setStyle("-fx-text-fill:-ars-err;"); return; }
            int no = editing && d.equalsIgnoreCase(editDir[0]) ? editNo[0] : QtcRules.nextSeries(ex, d);
            List<QtcRecord> made = new ArrayList<>();
            for (QtcRules.Line l : lines) {
                QtcRecord r = new QtcRecord();
                r.setContestId(cid); r.setPartnerCall(p); r.setDirection(d); r.setSeriesNo(no); r.setSeriesSize(lines.size());
                r.setQtcBand(band.getText().trim().toLowerCase()); r.setQtcMode(mode.getText().trim().toUpperCase());
                r.setQtcDateTimeUtc(LocalDateTime.now(ZoneOffset.UTC));
                r.setQsoTime(l.time()); r.setQsoCall(l.call()); r.setQsoSerial(l.serial());
                made.add(r);
            }
            try {
                if (editing) ContestQtcDao.getInstance().replaceSeries(cid, editDir[0], editNo[0], made);
                else for (QtcRecord r : made) ContestQtcDao.getInstance().insert(r);
            } catch (Exception ex2) { msg.setText("Could not save: " + ex2.getMessage()); msg.setStyle("-fx-text-fill:-ars-err;"); return; }
            for (TextField[] r : rows) for (TextField t : r) t.clear();
            editNo[0] = 0; editDir[0] = null;
            msg.setText((editing ? "Updated" : "Saved") + " QTC series " + no + " (" + lines.size() + " QTC" + (lines.size() == 1 ? "" : "s") + ") " + ("SENT".equals(d) ? "to " : "from ") + p + ".");
            msg.setStyle("-fx-text-fill:-ars-ok;");
            refresh.run();
            ContestState.recompute();                                 // the QTC points join the score
        });
        refresh.run();

        HBox head = new HBox(6, dir, partner, band, mode); head.setAlignment(Pos.CENTER_LEFT);
        HBox buttons = new HBox(12, clear, save); buttons.setAlignment(Pos.CENTER_RIGHT);
        Label rule = lbl("Up to 10 QTCs a series, 10 in all with one station; a QSO is reported once. Edit or Delete a saved series from the list above. Check the current WAEDC rules.", "ct-note");
        rule.setWrapText(true); rule.setMaxWidth(300);
        VBox box = new VBox(6, head, seriesL, grid, msg, buttons, totals, history, rule);
        box.setPadding(new Insets(2));
        ContestState.addListener(() -> Platform.runLater(refresh));
        return box;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String dirKey(ComboBox<String> dir) { return "Sent".equals(dir.getValue()) ? QtcRules.SENT : QtcRules.RECEIVED; }

    private static List<QtcRecord> existing(String contestId) {
        try { return new ArrayList<>(ContestQtcDao.getInstance().fetchByContest(contestId)); } catch (Exception e) { return new ArrayList<>(); }
    }

    private static String ownContinent() {
        DxccInfo.Info i = DxccInfo.of(HubConfig.call());
        return i == null ? null : i.continent();
    }

    private static TextField field(String prompt, double w) {
        TextField t = new TextField(); t.setPromptText(prompt); t.getStyleClass().add("jhub-mini-field");
        t.setPrefWidth(w); t.setMinWidth(w);
        return Shell.caps(t);
    }
}
