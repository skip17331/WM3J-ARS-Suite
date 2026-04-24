package com.hamradio.modem.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hamradio.modem.ModemService;
import com.jlog.db.ContestQsoDao;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Contest-aware QSO entry pane for J-Digi.
 *
 * Activated when J-Log broadcasts CONTEST_ACTIVE via J-Hub.
 * Builds its form dynamically from the contest plugin's field schema.
 * Dupe/mult checks AND QSO persistence run against the shared contest DB
 * in j-log-engine, so a RTTY contest can be logged without j-log running.
 * A QSO_SAVED broadcast lets j-log's contest view refresh if it's open.
 * Scoring (points/serial allocation) still runs in j-log when present.
 */
public class ContestEntryPane extends VBox {

    private final ModemService service;

    private String  contestId          = "";
    private String  multiplierDbColumn = "field1";
    private boolean contestWideDupe    = false;
    private boolean roverAwareDupe     = false;

    // Ordered list of non-special field ids in the plugin's schema, in the
    // same slot order used for contestField1..5 persistence. Built during
    // activate() and read at save time so direct DB writes land in the
    // correct columns without requiring a plugin reference.
    private final List<String> dbColOrder = new ArrayList<>();

    // Dynamic rcvd fields: fieldId → TextField
    private final Map<String, TextField> rcvdFields = new LinkedHashMap<>();

    // Fixed controls always present
    private final TextField tfCallsign = new TextField();
    private final TextField tfRstRcvd  = new TextField("599");
    private final Label     lblDupe    = new Label();
    private final Label     lblMult    = new Label();
    private final Label     lblStatus  = new Label();
    private final Label     lblContest = new Label("No contest active");
    private final Label     lblHint    = new Label();

    // Dynamic field area rebuilt per contest
    private final VBox fieldArea = new VBox(6);

    public ContestEntryPane(ModemService service) {
        this.service = service;
        setSpacing(8);
        setPadding(new Insets(8));
        getStyleClass().add("jd-entry-pane");

        lblContest.getStyleClass().add("jd-section-label");
        lblHint.getStyleClass().add("exchange-hint");
        lblDupe.getStyleClass().add("dupe-status");
        lblMult.getStyleClass().add("dupe-status");
        lblStatus.getStyleClass().add("jd-status-label");

        tfCallsign.setPromptText("Callsign");
        tfCallsign.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-font-family: monospace;");
        tfCallsign.textProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                String up = nv.toUpperCase().replaceAll("\\s+", "");
                if (!up.equals(nv)) { tfCallsign.setText(up); return; }
                if (up.length() >= 3) checkDupe(up);
                else { lblDupe.setText(""); lblDupe.getStyleClass().removeAll("dupe-status-warn","dupe-status-ok"); }
            }
        });

        getChildren().addAll(
            lblContest,
            lblHint,
            new Label("Callsign:"),
            tfCallsign,
            lblDupe,
            fieldArea,
            lblMult,
            makeSendButton(),
            lblStatus
        );
    }

    // ---------------------------------------------------------------
    // Activate / deactivate
    // ---------------------------------------------------------------

    /** Called when CONTEST_ACTIVE arrives. Rebuilds the form. */
    public void activate(JsonObject schema) {
        contestId          = getString(schema, "contestId", "");
        multiplierDbColumn = getString(schema, "multiplierDbColumn", "field1");
        contestWideDupe    = schema.has("contestWideDupe")  && schema.get("contestWideDupe").getAsBoolean();
        roverAwareDupe     = schema.has("roverAwareDupe")   && schema.get("roverAwareDupe").getAsBoolean();
        String name        = getString(schema, "contestName", "Contest");
        String hint        = getString(schema, "exchangeFormat", "");

        Platform.runLater(() -> {
            lblContest.setText(name);
            lblHint.setText(hint);
            rebuildFields(schema);
        });
    }

    /** Called when CONTEST_INACTIVE arrives. Shows idle state. */
    public void deactivate() {
        Platform.runLater(() -> {
            contestId = "";
            contestWideDupe = false;
            roverAwareDupe  = false;
            lblContest.setText("No contest active");
            lblHint.setText("");
            fieldArea.getChildren().clear();
            rcvdFields.clear();
            lblDupe.setText("");
            lblMult.setText("");
            lblStatus.setText("");
            tfCallsign.clear();
        });
    }

    // ---------------------------------------------------------------
    // Dynamic form builder
    // ---------------------------------------------------------------

    private void rebuildFields(JsonObject schema) {
        fieldArea.getChildren().clear();
        rcvdFields.clear();
        dbColOrder.clear();

        if (!schema.has("entryFields")) return;
        JsonArray fields = schema.getAsJsonArray("entryFields");

        // Mirror ContestPlugin.computeMultiplierDbColumn slot logic: every
        // non-special field id gets slot 0..4, mapping to contestField1..5.
        for (JsonElement el : fields) {
            JsonObject fd = el.getAsJsonObject();
            String id = getString(fd, "id", "");
            if (!isSpecialId(id) && !id.isBlank()) dbColOrder.add(id);
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        int row = 0;

        // RST rcvd is always present in first position
        addGridRow(grid, row++, "RST R:", tfRstRcvd);

        for (JsonElement el : fields) {
            JsonObject fd = el.getAsJsonObject();
            String id       = getString(fd, "id", "");
            String label    = getString(fd, "label", id);
            int    entryRow = fd.has("entryRow") ? fd.get("entryRow").getAsInt() : 0;

            // Skip: sent-side fields, special handled fields, band, mode (from rig)
            if (entryRow == 1) continue;
            if (isSpecialId(id)) continue;

            TextField tf = new TextField();
            tf.setPrefWidth(fd.has("width") ? fd.get("width").getAsInt() : 100);
            tf.textProperty().addListener((obs, ov, nv) -> {
                if (nv != null) {
                    String up = nv.toUpperCase().replaceAll("\\s+", "");
                    if (!up.equals(nv)) tf.setText(up);
                }
                checkMult();
            });
            rcvdFields.put(id, tf);
            addGridRow(grid, row++, label + ":", tf);
        }

        fieldArea.getChildren().add(grid);
    }

    private static boolean isSpecialId(String id) {
        return switch (id) {
            case "callsign","band","mode","rst_sent","rst_rcvd",
                 "serial_sent","serial_rcvd","prec_sent","check_sent","sect_sent" -> true;
            default -> false;
        };
    }

    private void addGridRow(GridPane grid, int row, String label, javafx.scene.Node field) {
        Label lbl = new Label(label);
        lbl.setMinWidth(65);
        lbl.getStyleClass().add("entry-label");
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    // ---------------------------------------------------------------
    // Dupe / mult checks (async, direct DB access via j-log-engine)
    // ---------------------------------------------------------------

    private void checkDupe(String callsign) {
        if (contestId.isBlank()) return;
        String band = service.bandFromRigHz();
        String mode = service.getStatus().getRigMode() != null ? service.getStatus().getRigMode() : "";
        CompletableFuture.runAsync(() -> {
            try {
                boolean dupe;
                String  scope;
                if (contestWideDupe) {
                    // Sweepstakes / Rookie Roundup: one QSO per callsign, entire contest.
                    dupe  = ContestQsoDao.getInstance().isDuplicateContestWide(contestId, callsign);
                    scope = "contest";
                } else if (roverAwareDupe && isRoverCall(callsign)) {
                    // VHF rovers: (callsign, band, grid) — new grid = new QSO.
                    String grid = firstRcvdText("grid_rcvd", "gridsquare_rcvd");
                    dupe  = ContestQsoDao.getInstance().isDuplicateBandGrid(
                                contestId, callsign, band, multiplierDbColumn, grid);
                    scope = band.isBlank() ? "band" : band;
                } else if (roverAwareDupe) {
                    dupe  = ContestQsoDao.getInstance().isDuplicatePerBand(contestId, callsign, band);
                    scope = band.isBlank() ? "band" : band;
                } else {
                    dupe  = ContestQsoDao.getInstance().isDuplicate(contestId, callsign, band, mode);
                    scope = band.isBlank() ? "?" : band;
                }
                final boolean d = dupe;
                final String  s = scope;
                Platform.runLater(() -> {
                    lblDupe.getStyleClass().removeAll("dupe-status-warn","dupe-status-ok");
                    if (d) {
                        lblDupe.setText("DUPE on " + s);
                        lblDupe.getStyleClass().add("dupe-status-warn");
                    } else {
                        lblDupe.setText("New");
                        lblDupe.getStyleClass().add("dupe-status-ok");
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    private static boolean isRoverCall(String call) {
        if (call == null) return false;
        String c = call.trim().toUpperCase();
        return c.endsWith("/R") || c.endsWith("/ROVER");
    }

    private String firstRcvdText(String... ids) {
        for (String id : ids) {
            TextField tf = rcvdFields.get(id);
            if (tf != null && tf.getText() != null && !tf.getText().isBlank()) return tf.getText().trim();
        }
        return "";
    }

    private void checkMult() {
        if (contestId.isBlank() || rcvdFields.isEmpty()) return;
        // Use the first non-blank rcvd field value as the mult candidate
        String candidate = rcvdFields.values().stream()
            .map(TextField::getText).filter(s -> s != null && !s.isBlank())
            .findFirst().orElse(null);
        if (candidate == null) { lblMult.setText(""); return; }
        final String val = candidate.trim().toUpperCase();
        CompletableFuture.runAsync(() -> {
            try {
                List<String> worked = ContestQsoDao.getInstance()
                    .distinctFieldByColumn(contestId, multiplierDbColumn);
                boolean isNew = !worked.contains(val);
                Platform.runLater(() -> {
                    lblMult.getStyleClass().removeAll("dupe-status-warn","dupe-status-ok");
                    if (isNew) {
                        lblMult.setText("NEW MULT: " + val);
                        lblMult.getStyleClass().add("dupe-status-ok");
                    } else {
                        lblMult.setText("Mult worked: " + val);
                        lblMult.getStyleClass().remove("dupe-status-ok");
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    // ---------------------------------------------------------------
    // Send draft to J-Log
    // ---------------------------------------------------------------

    private Button makeSendButton() {
        Button btn = new Button("Log QSO");
        btn.getStyleClass().add("primary-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> sendDraft());
        return btn;
    }

    private void sendDraft() {
        String callsign = tfCallsign.getText().trim().toUpperCase();
        if (callsign.isBlank()) { lblStatus.setText("Callsign required"); return; }

        // Build exchange string from all rcvd fields (tokenized by ContestLogController)
        StringBuilder exch = new StringBuilder();
        rcvdFields.values().forEach(tf -> {
            String v = tf.getText().trim();
            if (!v.isBlank()) { if (exch.length() > 0) exch.append(" "); exch.append(v); }
        });

        long hz   = service.getStatus().getRigFrequencyHz();
        String band = service.bandFromRigHz();
        String mode = service.getStatus().getRigMode() != null ? service.getStatus().getRigMode() : "";
        String rstRcvd = tfRstRcvd.getText().trim();

        // No contest active — fall back to draft-send so j-log's Normal Log
        // can pick it up.
        if (contestId.isBlank()) {
            service.sendLogDraft(callsign, mode, band, hz, "599", rstRcvd, exch.toString(), "", 0.9);
            lblStatus.setText("Sent to J-Log: " + callsign);
            clearEntry();
            return;
        }

        // Contest active — write the QSO directly to the shared DB, then
        // broadcast QSO_SAVED so peer apps refresh. This removes the hard
        // requirement that j-log be running to log a digital-mode contest QSO.
        com.jlog.model.QsoRecord q = new com.jlog.model.QsoRecord();
        q.setContestId(contestId);
        q.setCallsign(callsign);
        q.setDateTimeUtc(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        q.setBand(band);
        q.setMode(mode);
        q.setFrequency(hz > 0 ? Long.toString(hz) : "");
        q.setOperator(com.jlog.util.AppConfig.getInstance().getStationCallsign());
        q.setRstSent("599");
        q.setRstReceived(rstRcvd);
        q.setExchange(exch.toString());

        // Map rcvd fields to contestField1..5 by plugin schema order.
        for (int slot = 0; slot < dbColOrder.size() && slot < 5; slot++) {
            TextField tf = rcvdFields.get(dbColOrder.get(slot));
            String v = tf != null && tf.getText() != null ? tf.getText().trim() : "";
            switch (slot) {
                case 0 -> q.setContestField1(v);
                case 1 -> q.setContestField2(v);
                case 2 -> q.setContestField3(v);
                case 3 -> q.setContestField4(v);
                case 4 -> q.setContestField5(v);
            }
        }

        CompletableFuture.runAsync(() -> {
            try {
                ContestQsoDao.getInstance().upsertByNaturalKey(q);
                service.sendContestQsoSaved(q);
                Platform.runLater(() -> {
                    lblStatus.setText("Logged: " + callsign);
                    clearEntry();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> lblStatus.setText("Log failed: " + ex.getMessage()));
            }
        });
    }

    private void clearEntry() {
        tfCallsign.clear();
        tfRstRcvd.setText("599");
        rcvdFields.values().forEach(TextField::clear);
        lblDupe.setText("");
        lblMult.setText("");
        tfCallsign.requestFocus();
    }

    // ---------------------------------------------------------------
    // Fill from waterfall decode — called when operator clicks a decode line
    // ---------------------------------------------------------------

    /** Pre-fill the form by parsing a raw decoded text line. */
    public void fillFromDecode(String text, String mode) {
        if (text == null || text.isBlank()) return;
        // Simple tokenizer: find callsign-like token and exchange tokens
        String[] tokens = text.trim().split("\\s+");
        for (String tok : tokens) {
            if (tok.matches("[A-Z0-9]{3,}[0-9][A-Z]+") || tok.matches("[A-Z][A-Z0-9]?[0-9][A-Z]+")) {
                if (tfCallsign.getText().isBlank()) {
                    tfCallsign.setText(tok);
                }
            }
        }
        // Fill first open rcvd field with the last token that looks like a state/exchange
        for (int i = tokens.length - 1; i >= 0; i--) {
            String tok = tokens[i];
            if (tok.matches("[A-Z]{2,3}") || tok.equals("DX")) {
                rcvdFields.values().stream()
                    .filter(tf -> tf.getText().isBlank())
                    .findFirst().ifPresent(tf -> tf.setText(tok));
                break;
            }
        }
    }

    public String getContestId() { return contestId; }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static String getString(JsonObject obj, String key, String def) {
        return obj.has(key) ? obj.get(key).getAsString() : def;
    }
}
