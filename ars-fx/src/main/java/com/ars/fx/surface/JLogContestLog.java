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
import static com.ars.fx.surface.JLogContestScore.*;

/** J-Log contest page: the recent-log table, the all-QSOs window and the Cabrillo export. Split out of {@link JLogContest}. */
final class JLogContestLog {
    private JLogContestLog() {}

    // ---- recent contest log table -----------------------------------------
    static void fillLog(VBox tbl) { fillLog(tbl, 10); }
    static void fillLog(VBox tbl, int limit) {
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
    static Node logHeader() {
        HBox h = new HBox(9,
                col("UTC", 54), col("CALL", 90), col("BAND", 48), col("MODE", 46),
                col("SN", 44), col("EXCHANGE", -1), col("PTS", 46), col("", 138));
        h.getStyleClass().add("jl-loghd"); h.setAlignment(Pos.CENTER_LEFT); return h;
    }
    static Node logRow(QsoRecord q) {
        String utc = q.getDateTimeUtc() == null ? "" : q.getDateTimeUtc().format(HMS).substring(0, 5);
        String exch = String.join(" ", strip(q.getSerialReceived(), q.getContestField1(), q.getContestField2(),
                q.getContestField3(), q.getContestField4(), q.getContestField5()));
        Label pts = cell(q.isDupe() ? "DUPE" : String.valueOf(q.getPoints()), 46);
        if (q.isDupe()) pts.setStyle("-fx-text-fill:-ars-err;-fx-font-weight:bold;");      // a dupe in the log is called out, not just dimmed
        Label edit = lbl("Edit", "jl-clearbtn"); edit.setStyle("-fx-cursor:hand;");
        Label erase = lbl("Erase", "jl-clearbtn"); erase.setStyle("-fx-cursor:hand;-fx-text-fill:-ars-err;");
        Label labelBtn = lbl("Label", "jl-clearbtn"); labelBtn.setStyle("-fx-cursor:hand;");
        com.ars.fx.shell.Tips.tip(labelBtn, "Print the QSL label for this QSO (printer and size: Setup ▸ Label printers)");
        edit.setOnMouseClicked(e -> QsoEditDialog.show(edit, q));  // the edit form: a pop-up with every field of the QSO
        erase.setOnMouseClicked(e -> eraseQso(erase, q));
        labelBtn.setOnMouseClicked(e -> {
            setB4("⋯ printing label for " + q.getCallsign() + "…", "new");
            com.ars.fx.data.QslLabel.printAsync(com.ars.fx.data.QslLabel.fromContest(q), r -> Platform.runLater(() ->
                    setB4((r.ok() ? "✓ " : "✗ ") + r.message(), r.ok() ? "new" : "dupe")));
        });
        HBox acts = new HBox(10, labelBtn, edit, erase); acts.setMinWidth(138); acts.setPrefWidth(138); acts.setAlignment(Pos.CENTER_RIGHT);
        HBox r = new HBox(9,
                cell(utc, 54), cell(q.getCallsign(), 90), cell(z(q.getBand()), 48), cell(z(q.getMode()), 46),
                cell(z(q.getSerialSent()), 44), cell(exch, -1), pts, acts);
        r.getStyleClass().add("jl-logrow"); r.setAlignment(Pos.CENTER_LEFT);
        if (q.isDupe()) r.setOpacity(0.75);
        return r;
    }
    static String[] strip(String... parts) {
        List<String> out = new ArrayList<>();
        for (String p : parts) if (p != null && !p.isBlank()) out.add(p);
        return out.toArray(new String[0]);
    }

    /** Every QSO of the contest in a window of its own, each with Edit / Erase - for correcting something older than the last ten. */
    static void openAllQsos() {
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
    static void exportCabrillo(ContestPlugin plugin) {
        try {
            ContestConfigBridge.syncStationToEngine();
            java.nio.file.Path dir = java.nio.file.Paths.get(System.getProperty("user.home"), ".j-log", "exports");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path out = dir.resolve(plugin.getContestId() + "_" + HubConfig.call() + ".cbr");
            com.ars.fx.data.ContestState.recompute();                // the score as the contest page shows it, freshly worked out
            com.jlog.scoring.ContestScore sc = com.ars.fx.data.ContestState.score();
            com.jlog.export.CabrilloExporter.export(plugin, out, new com.jlog.export.CabrilloExporter.Options(
                    "WM3J ARS Suite " + com.ars.fx.AppVersion.current(), sc == null ? null : sc.score()));
            if (com.ars.fx.data.LogExport.asciiOnly()) {            // J-Hub ▸ Data ▸ charset: ASCII
                String txt = java.nio.file.Files.readString(out, java.nio.charset.StandardCharsets.UTF_8);
                java.nio.file.Files.writeString(out, com.ars.fx.data.LogExport.forExport(txt), java.nio.charset.StandardCharsets.US_ASCII);
            }
            setB4("Exported → " + out, "new");
        } catch (Exception e) {
            setB4("Export failed: " + e.getMessage(), "dupe");
        }
    }

}
