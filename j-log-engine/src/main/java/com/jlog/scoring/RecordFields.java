package com.jlog.scoring;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;

/**
 * Resolves an entry-field id to its value as stored on a {@link QsoRecord},
 * mirroring how the entry form maps fields onto the record — the same slot
 * mapping {@link ContestPlugin#fieldSlotColumn} (and the Cabrillo exporter) use.
 *
 * <p>This lets scoring read received-exchange values (year, grid, state/QTH)
 * from the record instead of live JavaFX controls, which is the prerequisite
 * for a UI-free scorer (scoring refactor, stage 1).
 */
public final class RecordFields {
    private RecordFields() {}

    /** The value an entry-field id holds on this record ("" if absent/unknown). */
    public static String value(ContestPlugin plugin, QsoRecord q, String id) {
        if (q == null || id == null || id.isBlank()) return "";
        if (plugin != null) {
            String col = plugin.fieldSlotColumn(id);     // field1..field5 for slot fields, null otherwise
            if (col != null) {
                return n(switch (col) {
                    case "field1" -> q.getContestField1();
                    case "field2" -> q.getContestField2();
                    case "field3" -> q.getContestField3();
                    case "field4" -> q.getContestField4();
                    case "field5" -> q.getContestField5();
                    default -> "";
                });
            }
        }
        // non-slot special fields stored in dedicated columns
        return switch (id) {
            case "callsign"    -> n(q.getCallsign());
            case "band"        -> n(q.getBand());
            case "mode"        -> n(q.getMode());
            case "rst_sent"    -> n(q.getRstSent());
            case "rst_rcvd"    -> n(q.getRstReceived());
            case "serial_rcvd" -> n(q.getSerialReceived());
            default            -> "";
        };
    }

    private static String n(String s) { return s == null ? "" : s; }
}
