package com.ars.fx.surface;

import com.ars.fx.data.ContestState;
import com.jlog.db.ContestQsoDao;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.ContestPlugin.FieldDef;
import com.jlog.scoring.ContestScorer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The contact edit form: a pop-up over the contest page with everything about one logged QSO (date and time UTC, call, band, mode, the serial
 * we sent, and every exchange field the contest has) in one place. Save checks the values the way the entry form does, re-decides dupes and
 * points for the whole log, and tells other programs about the change.
 */
final class QsoEditDialog {
    private QsoEditDialog() {}

    static void show(Node source, QsoRecord q) {
        ContestPlugin plugin = ContestState.plugin();
        Scene sc = source == null ? null : source.getScene();
        if (plugin == null || sc == null || !(sc.getRoot() instanceof Pane root)) { JLogContest.loadForEdit(q); return; }   // no room for a pop-up: the entry form will do

        LocalDateTime when = q.getDateTimeUtc() == null ? LocalDateTime.now(java.time.ZoneOffset.UTC) : q.getDateTimeUtc();
        TextField date = field(when.toLocalDate().toString(), 110), time = field(when.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")), 90);
        TextField call = field(JLogContest.z(q.getCallsign()), 130), band = field(JLogContest.z(q.getBand()), 70), mode = field(JLogContest.z(q.getMode()), 70);
        TextField sentNr = field(JLogContest.z(q.getSerialSent()), 70);
        call.setStyle("-fx-font-size:16px;-fx-font-weight:bold;");

        Map<FieldDef, Control> fields = new LinkedHashMap<>();
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1 || "callsign".equals(fd.getId()) || "band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;
            String v = switch (fd.getId()) {
                case "rst_rcvd", "rst_received" -> q.getRstReceived();
                case "rst_sent" -> q.getRstSent();
                case "serial_rcvd", "serial_received" -> q.getSerialReceived();
                default -> { String slot = plugin.fieldSlotColumn(fd.getId()); yield slot == null ? null : JLogContest.slotValue(q, slot); }
            };
            Control c;
            if ("combo".equalsIgnoreCase(fd.getType()) && fd.getOptions() != null && !fd.getOptions().isEmpty()) {
                ComboBox<String> cb = new ComboBox<>(); cb.getItems().addAll(fd.getOptions()); cb.setEditable(true);
                cb.setValue(v == null ? "" : v); cb.getStyleClass().add("jhub-mini-field"); cb.setPrefWidth(110); c = cb;
            } else c = field(v == null ? "" : v, fd.getWidth() > 0 ? Math.max(70, fd.getWidth()) : 90);
            fields.put(fd, c);
        }

        GridPane top = new GridPane(); top.setHgap(12); top.setVgap(8);
        addCell(top, 0, "DATE (UTC)", date); addCell(top, 1, "TIME (UTC)", time); addCell(top, 2, "CALL", call);
        addCell(top, 3, "BAND", band); addCell(top, 4, "MODE", mode); addCell(top, 5, "WE SENT #", sentNr);
        javafx.scene.layout.FlowPane exch = new javafx.scene.layout.FlowPane(12, 8);
        for (Map.Entry<FieldDef, Control> e : fields.entrySet())
            exch.getChildren().add(labeled(e.getKey().getLabel() == null || e.getKey().getLabel().isBlank() ? e.getKey().getId() : e.getKey().getLabel(), e.getValue()));
        exch.setPrefWrapLength(560);

        Label err = lbl("", "jl-emeta"); err.setWrapText(true); err.setMaxWidth(560);
        err.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:-ars-err;");
        err.managedProperty().bind(err.textProperty().isNotEmpty()); err.visibleProperty().bind(err.textProperty().isNotEmpty());

        Label title = lbl("Edit QSO — " + JLogContest.z(q.getCallsign()), "jhub-card-title");
        Label save = lbl("Save", "jl-logbtn"), cancel = lbl("Cancel", "jl-clearbtn"), erase = lbl("Erase", "jl-clearbtn");
        save.setStyle("-fx-cursor:hand;-fx-padding:7 18;"); cancel.setStyle("-fx-cursor:hand;-fx-padding:7 16;");
        erase.setStyle("-fx-cursor:hand;-fx-padding:7 16;-fx-text-fill:-ars-err;");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox btns = new HBox(10, erase, gap, cancel, save); btns.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12, title, top, exch, err, btns);
        card.setPadding(new Insets(20, 22, 18, 22)); card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setStyle("-fx-background-color:-ars-surface-1;-fx-background-radius:12;-fx-border-color:-ars-border;-fx-border-width:1;-fx-border-radius:12;");
        StackPane veil = new StackPane(card); veil.setMinSize(0, 0);
        veil.setStyle("-fx-background-color:rgba(6,9,13,0.55);");
        veil.prefWidthProperty().bind(root.widthProperty()); veil.prefHeightProperty().bind(root.heightProperty());
        Runnable close = () -> root.getChildren().remove(veil);

        Runnable doSave = () -> {
            String msg = apply(plugin, q, date.getText(), time.getText(), call.getText(), band.getText(), mode.getText(), sentNr.getText(), fields);
            if (msg != null) { err.setText(msg); return; }
            close.run();
        };
        save.setOnMouseClicked(e -> doSave.run());
        cancel.setOnMouseClicked(e -> close.run());
        erase.setOnMouseClicked(e -> { close.run(); JLogContest.eraseQso(source, q); });
        card.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) { close.run(); e.consume(); }
            else if (e.getCode() == javafx.scene.input.KeyCode.ENTER) { doSave.run(); e.consume(); }
        });
        root.getChildren().add(veil);
        call.requestFocus(); call.selectAll();
    }

    /**
     * Check the typed values and, if they are all acceptable, write them into {@code q} and the log.
     * @return null when saved, else what is wrong (nothing has been changed then)
     */
    static String apply(ContestPlugin plugin, QsoRecord q, String date, String time, String call, String band, String mode, String sentNr,
                        Map<FieldDef, Control> fields) {
        LocalDateTime when;
        try { when = LocalDateTime.of(LocalDate.parse(date.trim()), LocalTime.parse(time.trim().length() == 5 ? time.trim() + ":00" : time.trim())); }
        catch (Exception e) { return "The date is yyyy-mm-dd and the time hh:mm:ss, both UTC."; }
        String c = call == null ? "" : call.trim().toUpperCase(java.util.Locale.ROOT);
        if (c.isEmpty()) return "A call is needed.";
        Map<FieldDef, String> vals = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (Map.Entry<FieldDef, Control> e : fields.entrySet()) {
            String v = valueOf(e.getValue());
            vals.put(e.getKey(), v);
            if (e.getKey().isRequired() && v.isBlank()) { problems.add("Missing: " + labelOf(e.getKey())); continue; }
            ContestValidation.Result r = ContestValidation.check(plugin, e.getKey(), v, true);
            if (r.level() == ContestValidation.Level.HARD) problems.add(labelOf(e.getKey()) + ": " + r.message());
        }
        if (!problems.isEmpty()) return String.join("  ", problems);

        String oldCall = q.getCallsign();
        q.setDateTimeUtc(when);
        q.setCallsign(c);
        q.setBand(JLogContest.normBand(band));
        q.setMode(mode == null ? "" : mode.trim().toUpperCase(java.util.Locale.ROOT));
        q.setSerialSent(sentNr == null ? "" : sentNr.trim());
        for (Map.Entry<FieldDef, String> e : vals.entrySet()) {
            String v = e.getValue();
            switch (e.getKey().getId()) {
                case "rst_rcvd", "rst_received" -> q.setRstReceived(v);
                case "rst_sent" -> q.setRstSent(v);
                case "serial_rcvd", "serial_received" -> q.setSerialReceived(v);
                default -> { String slot = plugin.fieldSlotColumn(e.getKey().getId()); if (slot != null) JLogContest.setSlot(q, slot, v.toUpperCase(java.util.Locale.ROOT)); }
            }
        }
        try {
            List<QsoRecord> prior = new ArrayList<>(ContestQsoDao.getInstance().findByCallsign(plugin.getContestId(), c));
            prior.removeIf(x -> x.getId() == q.getId());
            boolean dupe = ContestScorer.isDupe(plugin, q, prior);
            q.setDupe(dupe);
            q.setPoints(ContestScorer.points(plugin, q, ContestState.ctx()));
            ContestQsoDao.getInstance().update(q);
            JLogContest.reflagDupes(plugin);                      // changing a call, band or time can turn another QSO into (or out of) a dupe
            com.ars.fx.data.N1mmBroadcast.contactEdited(plugin, q, oldCall);
            ContestState.recompute();
            Thread lt = new Thread(() -> com.ars.fx.data.CallHistory.learn(plugin, q), "call-history-learn");
            lt.setDaemon(true); lt.start();
        } catch (Exception ex) { return "Could not save: " + ex.getMessage(); }
        return null;
    }

    private static String labelOf(FieldDef fd) { return fd.getLabel() == null || fd.getLabel().isBlank() ? fd.getId() : fd.getLabel(); }
    private static String valueOf(Control c) {
        if (c instanceof TextField t) return t.getText() == null ? "" : t.getText().trim();
        if (c instanceof ComboBox<?> cb) { Object v = cb.isEditable() && cb.getEditor() != null ? cb.getEditor().getText() : cb.getValue(); return v == null ? "" : v.toString().trim(); }
        return "";
    }
    private static TextField field(String text, double w) {
        TextField t = new TextField(text); t.getStyleClass().add("jhub-mini-field"); t.setMinWidth(w); t.setPrefWidth(w); return t;
    }
    private static VBox labeled(String label, Node n) { VBox v = new VBox(3, lbl(label.toUpperCase(java.util.Locale.ROOT), "jm-sec"), n); return v; }
    private static void addCell(GridPane g, int col, String label, Node n) { g.add(labeled(label, n), col % 3, col / 3); }
}
