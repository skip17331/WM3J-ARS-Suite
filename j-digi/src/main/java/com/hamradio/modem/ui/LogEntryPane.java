package com.hamradio.modem.ui;

import com.hamradio.modem.ModemService;
import com.hamradio.modem.model.HubSpot;
import com.jlog.db.QsoDao;
import com.jlog.model.QsoRecord;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;

/**
 * Log entry form in the right panel.
 * Saves directly via j-log-engine (QsoDao) when db is available,
 * otherwise sends a LOG_ENTRY_DRAFT via j-hub.
 */
public class LogEntryPane extends VBox {

    private final TextField        tfCallsign = new TextField();
    private final TextField        tfName     = new TextField();
    private final TextField        tfFreq     = new TextField();
    private final TextField        tfBand     = new TextField();
    private final ComboBox<String> cbMode     = new ComboBox<>();
    private final TextField        tfRstSent  = new TextField("599");
    private final TextField        tfRstRcvd  = new TextField("599");
    private final TextArea         taNotes    = new TextArea();
    private final Label            lblStatus  = new Label();

    private final ModemService service;
    private boolean dbReady = false;
    private String stationCallsign = "NOCALL";

    public LogEntryPane(ModemService service) {
        this.service = service;
        setSpacing(8);
        setPadding(new Insets(8));
        getStyleClass().add("jd-entry-pane");

        try {
            QsoDao.getInstance();
            dbReady = true;
        } catch (Exception ignored) {}

        cbMode.getItems().addAll(
            "RTTY", "PSK31", "PSK63", "PSK125",
            "OLIVIA", "MFSK16", "DOMINOEX", "CW",
            "SSB", "FT8", "FT4", "JS8", "AM", "FM"
        );
        cbMode.setValue("RTTY");

        tfFreq.setEditable(false);
        tfFreq.setPrefWidth(130);
        tfBand.setEditable(false);
        tfBand.setPrefWidth(60);
        taNotes.setPrefRowCount(3);
        taNotes.setWrapText(true);

        service.setStatusListener(st ->
            javafx.application.Platform.runLater(() -> {
                long hz = st.getRigFrequencyHz();
                if (hz > 0) {
                    tfFreq.setText("%.3f".formatted(hz / 1_000_000.0));
                    tfBand.setText(bandFromHz(hz));
                }
                String rigMode = st.getRigMode();
                if (rigMode != null && !rigMode.isBlank()
                        && !cbMode.getItems().contains(cbMode.getValue())) {
                    cbMode.setValue(rigMode);
                }
            }));

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);

        int r = 0;
        addRow(grid, r++, "Callsign", tfCallsign);
        addRow(grid, r++, "Name",     tfName);
        addRow(grid, r++, "Mode",     cbMode);
        addRow(grid, r++, "RST Sent", tfRstSent);
        addRow(grid, r++, "RST Rcvd", tfRstRcvd);
        HBox fb = new HBox(6, tfFreq, tfBand);
        fb.setAlignment(Pos.CENTER_LEFT);
        addRow(grid, r++, "Freq/Band", fb);
        addRow(grid, r,   "Notes",     taNotes);

        Button saveBtn  = new Button("Save QSO");
        Button draftBtn = new Button("Send Draft");
        Button clearBtn = new Button("Clear");

        saveBtn.getStyleClass().add("primary-button");
        draftBtn.getStyleClass().add("secondary-button");

        saveBtn.setOnAction(e  -> saveQso());
        draftBtn.setOnAction(e -> sendDraft());
        clearBtn.setOnAction(e -> clear());

        HBox buttons = new HBox(6, saveBtn, draftBtn, clearBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        lblStatus.getStyleClass().add("jd-status-label");

        getChildren().addAll(
            sectionLabel("Log Entry"),
            grid,
            buttons,
            lblStatus
        );
    }

    // ---------------------------------------------------------------
    // Public API — called by MacroBar and DxSpotPane
    // ---------------------------------------------------------------

    public String getCallsign() {
        return tfCallsign.getText().trim().toUpperCase();
    }

    public String getRst() {
        String r = tfRstSent.getText().trim();
        return r.isBlank() ? "599" : r;
    }

    public void prefillFromSpot(HubSpot spot) {
        if (spot == null) return;
        if (spot.spotted != null) tfCallsign.setText(spot.spotted.toUpperCase());
        if (spot.band    != null) tfBand.setText(spot.band);
        if (spot.mode    != null && !spot.mode.isBlank()) cbMode.setValue(spot.mode);
        if (spot.frequency > 0)
            tfFreq.setText("%.3f".formatted(spot.frequency / 1_000_000.0));
    }

    public void prefillCallsign(String call) {
        if (call != null && !call.isBlank())
            tfCallsign.setText(call.trim().toUpperCase());
    }

    /** Called by MainWindow when j-hub delivers station identity. */
    public void setStationCallsign(String call) {
        if (call != null && !call.isBlank())
            stationCallsign = call.trim().toUpperCase();
    }

    /** The operator's own callsign (for logging metadata, not the DX callsign field). */
    public String getStationCallsign() { return stationCallsign; }

    // ---------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------

    private void saveQso() {
        String call = getCallsign();
        if (call.isBlank()) { setStatus("Callsign required"); return; }

        if (!dbReady) {
            sendDraft();
            setStatus("DB unavailable \u2014 sent as draft");
            return;
        }

        try {
            QsoRecord q = new QsoRecord();
            q.setCallsign(call);
            q.setDateTimeUtc(LocalDateTime.now());
            q.setBand(tfBand.getText().trim());
            q.setMode(cbMode.getValue());
            q.setRstSent(tfRstSent.getText().trim());
            q.setRstReceived(tfRstRcvd.getText().trim());
            q.setNotes(taNotes.getText().trim());
            QsoDao.getInstance().insert(q);
            setStatus("Saved \u2014 " + call);
            clear();
        } catch (Exception ex) {
            setStatus("Save failed: " + ex.getMessage());
        }
    }

    private void sendDraft() {
        long hz = service.getStatus().getRigFrequencyHz();
        service.sendLogDraft(
            getCallsign(),
            cbMode.getValue(),
            tfBand.getText().trim(),
            hz,
            tfRstSent.getText().trim(),
            tfRstRcvd.getText().trim(),
            taNotes.getText().trim(),
            "",
            0.9
        );
        setStatus("Draft sent \u2014 " + (getCallsign().isBlank() ? "(no call)" : getCallsign()));
    }

    private void clear() {
        tfCallsign.clear();
        tfName.clear();
        taNotes.clear();
        tfRstSent.setText("599");
        tfRstRcvd.setText("599");
        lblStatus.setText("");
    }

    private void setStatus(String msg) { lblStatus.setText(msg); }

    // ---------------------------------------------------------------
    // Layout helpers
    // ---------------------------------------------------------------

    private void addRow(GridPane grid, int row, String label, javafx.scene.Node field) {
        Label lbl = new Label(label + ":");
        lbl.setMinWidth(70);
        lbl.getStyleClass().add("entry-label");
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("jd-section-label");
        return l;
    }

    private String bandFromHz(long hz) {
        long k = hz / 1000;
        if (k >= 1800   && k <= 2000)   return "160m";
        if (k >= 3500   && k <= 4000)   return "80m";
        if (k >= 7000   && k <= 7300)   return "40m";
        if (k >= 10100  && k <= 10150)  return "30m";
        if (k >= 14000  && k <= 14350)  return "20m";
        if (k >= 18068  && k <= 18168)  return "17m";
        if (k >= 21000  && k <= 21450)  return "15m";
        if (k >= 24890  && k <= 24990)  return "12m";
        if (k >= 28000  && k <= 29700)  return "10m";
        if (k >= 50000  && k <= 54000)  return "6m";
        if (k >= 144000 && k <= 148000) return "2m";
        if (k >= 420000 && k <= 450000) return "70cm";
        return "";
    }
}
