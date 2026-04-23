package com.jlog.ui.contest;

import com.jlog.model.QsoRecord;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Shows whether the callsign currently being entered has been worked before in
 * this contest, and on which modes. Per ARRL 10M rules the station is workable
 * once per mode, so a prior contact on the same mode is a dupe; a prior contact
 * on the other mode is still a valid new QSO.
 */
public class WorkedBeforePane extends VBox {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm 'UTC'");

    private final Label statusLine = new Label("—");
    private final Label detailLine = new Label("");

    public WorkedBeforePane() {
        getStyleClass().addAll("pane-content", "worked-before");
        statusLine.getStyleClass().add("worked-before");
        detailLine.getStyleClass().add("worked-before");
        setSpacing(2);
        getChildren().addAll(statusLine, detailLine);
    }

    /** Update the pane for a callsign given the list of prior QSOs and the current mode. */
    public void update(String callsign, String currentMode, List<QsoRecord> prior) {
        update(callsign, currentMode, prior, List.of());
    }

    /** Update with both exact-match QSOs ({@code prior}) and prefix matches
     *  ({@code partials}). When {@code prior} is empty but {@code partials} isn't,
     *  the pane shows "possible dupe" + the matching callsigns so the operator
     *  can verify before logging. */
    public void update(String callsign, String currentMode, List<QsoRecord> prior, List<String> partials) {
        if (callsign == null || callsign.isBlank()) { clear(); return; }
        String tag = isRoverCall(callsign) ? "🚗 ROVER " : "";
        if (prior == null || prior.isEmpty()) {
            statusLine.getStyleClass().removeAll("worked-before-new", "worked-before-dupe");
            if (partials != null && !partials.isEmpty()) {
                statusLine.setText(tag + "⚠ possible dupe — " + callsign);
                statusLine.getStyleClass().add("worked-before-dupe");
                StringBuilder sb = new StringBuilder("matches: ");
                for (int i = 0; i < partials.size() && i < 6; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(partials.get(i));
                }
                if (partials.size() > 6) sb.append(", …");
                detailLine.setText(sb.toString());
            } else {
                statusLine.setText(tag + "✓ NEW — " + callsign);
                statusLine.getStyleClass().add("worked-before-new");
                detailLine.setText("");
            }
            return;
        }

        Map<String, Integer> byMode = new LinkedHashMap<>();
        for (QsoRecord q : prior) {
            if (q.isDupe()) continue;
            byMode.merge(q.getMode() == null ? "" : q.getMode(), 1, Integer::sum);
        }
        boolean dupe = currentMode != null && byMode.containsKey(currentMode);
        statusLine.getStyleClass().removeAll("worked-before-new", "worked-before-dupe");
        if (dupe) {
            statusLine.setText(tag + "⛔ DUPE on " + currentMode + " — " + callsign);
            statusLine.getStyleClass().add("worked-before-dupe");
        } else {
            statusLine.setText(tag + "⚠ Worked before — " + callsign);
            statusLine.getStyleClass().add("worked-before-new");
        }

        StringBuilder sb = new StringBuilder();
        byMode.forEach((m, count) -> {
            if (sb.length() > 0) sb.append("  ");
            sb.append(count).append("× ").append(m.isBlank() ? "?" : m);
        });
        QsoRecord last = prior.get(prior.size() - 1);
        if (last.getDateTimeUtc() != null) sb.append("   last ").append(last.getDateTimeUtc().format(FMT));
        detailLine.setText(sb.toString());
    }

    public void clear() {
        statusLine.setText("—");
        statusLine.getStyleClass().removeAll("worked-before-new", "worked-before-dupe");
        detailLine.setText("");
    }

    private static boolean isRoverCall(String call) {
        if (call == null) return false;
        String c = call.trim().toUpperCase();
        return c.endsWith("/R") || c.endsWith("/ROVER");
    }
}
