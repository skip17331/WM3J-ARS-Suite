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
import static com.ars.fx.surface.JLogContestScore.*;
import static com.ars.fx.surface.JLogContestLog.*;

/** J-Log contest page: the "SEND" line: what to say or key, the sent exchange and the macro tags. Split out of {@link JLogContest}. */
final class JLogContestSend {
    private JLogContestSend() {}

    // ---- "SEND" line: exactly what to say / key, built from Setup ----------------
    static String sendText(ContestPlugin plugin) {
        List<String> parts = new ArrayList<>();
        if (fieldControls.containsKey("serial_rcvd")) parts.add(String.valueOf(serial));      // contests that exchange a serial number
        for (FieldDef fd : SentExchange.sentFields(plugin)) {
            String v = value(fd.getId());
            parts.add(v.isEmpty() ? "?" : v);
            if (fd.getId().equals("prec_sent")) parts.add(HubConfig.call().toUpperCase());    // Sweepstakes: NR PREC CALL CHK SECT
        }
        return parts.isEmpty() ? "-" : String.join("   ", parts);
    }
    /** "RUN" or "S&P": whether you are calling CQ or hunting. Passed on in the N1MM+ messages (RadioInfo IsRunning, contact IsRunQSO). */
    static Region runRow() {
        Label tag = lbl("Operating", "jl-skim-meta");
        Label chip = lbl(ContestState.isRun() ? "RUN" : "S&P", "jl-clearbtn");
        chip.setStyle("-fx-cursor:hand;-fx-font-weight:bold;");
        com.ars.fx.shell.Tips.tip(chip, "Click to switch between running (calling CQ) and search-and-pounce. Other programs listening on the N1MM+ broadcast are told.");
        chip.setOnMouseClicked(e -> {
            boolean run = !ContestState.isRun();
            ContestState.setRun(run);
            chip.setText(run ? "RUN" : "S&P");
            com.ars.fx.data.N1mmBroadcast.runChanged();
        });
        HBox row = new HBox(12, tag, chip); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** What you send, in the contest's own (Cabrillo) order, e.g. "5NN 05" or "5NN 001" or "1D ORG"; the report is cut (599 = 5NN). */
    static String exchangeText(ContestPlugin plugin) {
        List<String> parts = new ArrayList<>();
        boolean resolved = plugin.getCabrilloSent() != null && !plugin.getCabrilloSent().isEmpty();
        if (resolved) for (String id : plugin.getCabrilloSent()) {
            if (id.equals("serial_sent")) { parts.add(String.valueOf(serial)); continue; }
            FieldDef fd = plugin.getEntryFields().stream().filter(f -> f.getId().equals(id)).findFirst().orElse(null);
            if (fd == null) { resolved = false; break; }
            parts.add(value(id));
        }
        if (!resolved) {                                              // the Cabrillo names are not field ids: fall back to the SEND line's order
            parts.clear();
            if (fieldControls.containsKey("serial_rcvd")) parts.add(String.valueOf(serial));
            for (FieldDef fd : SentExchange.sentFields(plugin)) parts.add(value(fd.getId()));
        }
        parts.removeIf(String::isEmpty);
        return com.ars.fx.data.ContestMacros.cut(String.join(" ", parts));
    }
    /** The placeholders only a contest page knows. */
    static Map<String, String> macroTags(ContestPlugin plugin) {
        return Map.of("{EXCH}", exchangeText(plugin), "{NR}", String.valueOf(serial));
    }

    static Region sendRow(ContestPlugin plugin) {
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
    static void reloadSent(ContestPlugin plugin) {
        for (FieldDef fd : SentExchange.sentFields(plugin)) {
            Control c = fieldControls.get(fd.getId());
            if (c != null) setControl(c, SentExchange.value(plugin, fd, modeF.getText()));
        }
        refreshSend(plugin);
    }
    static void refreshSend(ContestPlugin plugin) { if (sendV != null) sendV.setText(sendText(plugin)); }
    @SuppressWarnings("unchecked")
    static void setControl(Control c, String v) {
        if (c instanceof TextField t) t.setText(v == null ? "" : v);
        else if (c instanceof ComboBox<?> cb) ((ComboBox<String>) cb).setValue(v == null || v.isEmpty() ? null : v);
    }
    /** Ask for any part of the exchange Setup does not know, once per contest per session. */
    static void askSentIfMissing(ContestPlugin plugin) {
        List<FieldDef> miss = SentExchange.missing(plugin, modeF.getText());
        if (miss.isEmpty() || !askedSent.add(plugin.getContestId())) return;
        Platform.runLater(() -> SentExchange.ask(host, plugin, miss, modeF.getText(), () -> reloadSent(plugin)));
    }

    static HBox wrapRow(String tag, List<Node> cells) {
        Label t = lbl(tag, "jl-flabel"); t.setMinWidth(70);
        HBox fields = new HBox(12); fields.getChildren().addAll(cells); fields.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setHgrow(fields, Priority.ALWAYS);
        HBox row = new HBox(10, t, fields); row.setAlignment(Pos.BOTTOM_LEFT);
        return row;
    }

}
