package com.jlog.ui.contest;

import com.jlog.db.ContestQtcDao;
import com.jlog.model.QtcRecord;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * WAE-DC QTC traffic entry (Rule §7). The operator records a QTC series
 * exchanged with a partner: pick direction (RX = received from partner,
 * TX = sent to partner), enter band/mode/QRG, then one QTC per line as
 * <code>HHMM CALL SERIAL</code>. On save each line becomes a
 * {@link QtcRecord} (= 1 point); the ≤10-per-pair cap (Rule §7/§12) is
 * enforced. The series number auto-increments per partner+direction.
 */
public class QtcPane extends VBox {

    private static final Logger log = LoggerFactory.getLogger(QtcPane.class);

    private final String contestId;
    private final TextField partner = new TextField();
    private final ComboBox<String> direction = new ComboBox<>();
    private final TextField band = new TextField();
    private final TextField mode = new TextField();
    private final TextField qrg  = new TextField();
    private final TextArea  lines = new TextArea();
    private final Label status = new Label("0 QTCs logged");

    public QtcPane(String contestId) {
        this.contestId = contestId;
        getStyleClass().addAll("pane-content", "qtc-pane");
        setSpacing(4);
        setPadding(new Insets(6));

        partner.setPromptText("Partner call");
        direction.getItems().addAll("RX", "TX");
        direction.setValue("RX");
        band.setPromptText("band e.g. 20m");
        mode.setPromptText("mode");
        qrg.setPromptText("QRG kHz");
        lines.setPromptText("One QTC per line:  HHMM CALL SERIAL\n1332 S59XYZ 112");
        lines.setPrefRowCount(6);

        Button save = new Button("Save QTC series");
        save.setOnAction(e -> save());

        HBox row1 = new HBox(4, new Label("Pair:"), partner, direction);
        HBox row2 = new HBox(4, band, mode, qrg);
        getChildren().addAll(row1, row2, lines, save, status);
        refreshStatus();
    }

    private void save() {
        String p = partner.getText() == null ? "" : partner.getText().trim().toUpperCase();
        if (p.isEmpty()) { status.setText("Enter a partner callsign."); return; }
        String dir = direction.getValue();
        String[] raw = lines.getText() == null ? new String[0] : lines.getText().split("\\r?\\n");

        java.util.List<String[]> parsed = new java.util.ArrayList<>();
        for (String ln : raw) {
            String s = ln.trim();
            if (s.isEmpty()) continue;
            String[] t = s.split("\\s+");
            if (t.length < 3) { status.setText("Bad line (need HHMM CALL SERIAL): " + s); return; }
            parsed.add(new String[]{ t[0], t[1].toUpperCase(), t[2] });
        }
        if (parsed.isEmpty()) { status.setText("No QTC lines entered."); return; }
        if (parsed.size() > 10) { status.setText("A QTC series is max 10 (Rule §7)."); return; }

        try {
            int already = ContestQtcDao.getInstance().countByPair(contestId, p);
            if (already + parsed.size() > 10) {
                status.setText("Pair cap: " + already + " already with " + p
                        + " (max 10). Can add " + (10 - already) + ".");
                return;
            }
            int seriesNo = already / 10 + 1;   // simple progressive series id
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            for (String[] t : parsed) {
                QtcRecord r = new QtcRecord();
                r.setContestId(contestId);
                r.setPartnerCall(p);
                r.setDirection(dir);
                r.setSeriesNo(seriesNo);
                r.setSeriesSize(parsed.size());
                r.setQtcQrg(qrg.getText());
                r.setQtcBand(band.getText());
                r.setQtcMode(mode.getText());
                r.setQtcDateTimeUtc(now);
                r.setQsoTime(t[0]);
                r.setQsoCall(t[1]);
                r.setQsoSerial(t[2]);
                ContestQtcDao.getInstance().insert(r);
            }
            lines.clear();
            status.setText("Saved " + parsed.size() + " QTC(s) " + dir + " " + p + ".");
            refreshStatus();
        } catch (Exception ex) {
            log.warn("QTC save failed", ex);
            status.setText("Save failed: " + ex.getMessage());
        }
    }

    /** Re-read the running QTC total (called by the stats poller too). */
    public void refreshStatus() {
        try {
            int n = ContestQtcDao.getInstance().totalQtcPointsByContest(contestId);
            Platform.runLater(() -> status.setText(n + " QTCs logged (= " + n + " pts)"));
        } catch (Exception ignore) {}
    }
}
