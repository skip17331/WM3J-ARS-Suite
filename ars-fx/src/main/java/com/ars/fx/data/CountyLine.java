package com.ars.fx.data;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * County-line contacts in a QSO party (punch list 4b-5). A station sitting on a county line sends both counties, "ADA/CAN", and
 * the contact counts once with each: two QSO lines in the log and the Cabrillo file. A mobile on a line can send its own county
 * the same way. The contest page used to need two separate entries for this; now one entry with "/" is logged as one QSO per
 * county (per pair of counties when both stations are on lines).
 */
public final class CountyLine {
    private CountyLine() {}

    /** The fields that hold counties in a QSO party: the county received and the county sent. */
    public static final List<String> FIELDS = List.of("state_prov_rcvd", "state_prov_sent");

    /** True when {@code fieldId} of {@code plugin} can hold a county-line value. */
    public static boolean applies(ContestPlugin plugin, String fieldId) {
        return plugin != null && QsoPartySide.isParty(plugin) && FIELDS.contains(fieldId);
    }

    /** "ada / Can" gives [ADA, CAN]; a value with no "/" gives itself (trimmed, upper case); repeats and empty parts are dropped. */
    public static List<String> parts(String value) {
        if (value == null) return List.of();
        String v = value.trim().toUpperCase(Locale.ROOT);
        if (!v.contains("/")) return v.isEmpty() ? List.of() : List.of(v);
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String p : v.split("/")) { String t = p.trim(); if (!t.isEmpty()) out.add(t); }
        return new ArrayList<>(out);
    }

    /**
     * One record per county combination of {@code r}: unchanged (a list of {@code r} itself) when no county field holds a "/".
     * Copies carry no id or uid, so each is saved as a QSO of its own.
     */
    public static List<QsoRecord> expand(ContestPlugin plugin, QsoRecord r) {
        List<QsoRecord> out = List.of(r);
        if (plugin == null || !QsoPartySide.isParty(plugin)) return out;
        for (String id : FIELDS) {
            String slot = plugin.fieldSlotColumn(id);
            if (slot == null) continue;
            List<QsoRecord> next = new ArrayList<>();
            for (QsoRecord q : out) {
                List<String> ps = parts(get(q, slot));
                if (ps.size() < 2) { next.add(q); continue; }
                for (String p : ps) {
                    QsoRecord c = q.copy();
                    c.setId(0); c.setUid(null);
                    set(c, slot, p);
                    next.add(c);
                }
            }
            out = next;
        }
        return out;
    }

    /** The value in a field1..field5 slot. */
    public static String get(QsoRecord q, String slot) {
        if (slot == null) return null;
        return switch (slot) {
            case "field1" -> q.getContestField1();
            case "field2" -> q.getContestField2();
            case "field3" -> q.getContestField3();
            case "field4" -> q.getContestField4();
            case "field5" -> q.getContestField5();
            default -> null;
        };
    }

    /** Set a field1..field5 slot. */
    public static void set(QsoRecord q, String slot, String v) {
        if (slot == null) return;
        switch (slot) {
            case "field1" -> q.setContestField1(v);
            case "field2" -> q.setContestField2(v);
            case "field3" -> q.setContestField3(v);
            case "field4" -> q.setContestField4(v);
            case "field5" -> q.setContestField5(v);
            default -> { }
        }
    }
}
