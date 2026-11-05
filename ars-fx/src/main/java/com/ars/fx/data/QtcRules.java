package com.ars.fx.data;

import com.jlog.model.QtcRecord;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * The checks on WAE DX Contest QTC traffic. A QTC is a report of a QSO you already made - its time, the call and the serial number -
 * passed on to a station on the other side of the Europe / non-Europe line, one point for each QTC for both stations.
 *
 * <p>What is checked: at most 10 QTCs in a series; at most 10 QTCs in all with the same station; a QTC passes between a European and
 * a non-European station only; a QSO is reported once; and not back to the station it was made with. These are the rules as the
 * engine's WAE definition cites them (claimed / running - the contest committee re-adjudicates), so check them against the current
 * WAEDC rules before relying on them.
 */
public final class QtcRules {
    private QtcRules() {}

    public static final int MAX_SERIES = 10, MAX_PER_PARTNER = 10;
    public static final String SENT = "SENT", RECEIVED = "RECEIVED";

    /** One reported QSO: HHMM, call, serial. */
    public record Line(String time, String call, String serial) {}

    /** The first thing wrong with a series about to be saved, or null when it is fine. */
    public static String validate(String ownContinent, String partnerCall, String partnerContinent, String direction,
                                  List<Line> lines, List<QtcRecord> existing) {
        String partner = norm(partnerCall);
        if (partner.isEmpty()) return "Enter the station the QTCs are with.";
        if (lines == null || lines.isEmpty()) return "Enter at least one QTC.";
        if (lines.size() > MAX_SERIES) return "A series holds at most " + MAX_SERIES + " QTCs.";
        if (ownContinent != null && partnerContinent != null && !ownContinent.isBlank() && !partnerContinent.isBlank()
                && ownContinent.equalsIgnoreCase("EU") == partnerContinent.equalsIgnoreCase("EU"))
            return "QTCs pass only between a European and a non-European station (you are " + ownContinent.toUpperCase(Locale.ROOT)
                    + ", " + partner + " is " + partnerContinent.toUpperCase(Locale.ROOT) + ").";

        int already = 0;
        Set<String> reported = new HashSet<>();
        for (QtcRecord r : existing) {
            if (!direction.equalsIgnoreCase(r.getDirection())) continue;
            if (partner.equals(norm(r.getPartnerCall()))) already++;
            reported.add(key(r.getQsoTime(), r.getQsoCall(), r.getQsoSerial()));
        }
        if (already + lines.size() > MAX_PER_PARTNER)
            return "At most " + MAX_PER_PARTNER + " QTCs in all with " + partner + " (" + already + " already logged).";

        int n = 0;
        for (Line l : lines) {
            n++;
            if (l.time() == null || !l.time().matches("([01]\\d|2[0-3])[0-5]\\d")) return "QTC " + n + ": the time must be four digits, HHMM.";
            String c = norm(l.call());
            if (!c.matches("[A-Z0-9/]{3,14}") || !c.matches(".*\\d.*") || !c.matches(".*[A-Z].*")) return "QTC " + n + ": '" + l.call() + "' is not a callsign.";
            if (l.serial() == null || !l.serial().matches("\\d{1,4}")) return "QTC " + n + ": the serial number is digits only.";
            if (c.equals(partner)) return "QTC " + n + ": a QTC cannot report the QSO with the station you are sending it to.";
            if (!reported.add(key(l.time(), c, l.serial()))) return "QTC " + n + ": that QSO has already been reported" + (n > 1 ? " (or appears twice here)." : ".");
        }
        return null;
    }

    /** The next series number for a direction: one more than the highest used so far. */
    public static int nextSeries(List<QtcRecord> existing, String direction) {
        int max = 0;
        for (QtcRecord r : existing) if (direction.equalsIgnoreCase(r.getDirection())) max = Math.max(max, r.getSeriesNo());
        return max + 1;
    }

    /** Which side a station is on by default: a European sends QTCs, a non-European receives them. */
    public static String defaultDirection(String ownContinent) { return "EU".equalsIgnoreCase(ownContinent) ? SENT : RECEIVED; }

    static String norm(String s) { return s == null ? "" : s.trim().toUpperCase(Locale.ROOT); }
    private static String key(String time, String call, String serial) { return norm(time) + "|" + norm(call) + "|" + (serial == null ? "" : serial.trim().replaceFirst("^0+(?!$)", "")); }

    /** Convenience for callers: continent of a call, or null. */
    public static String continentOf(String call, Function<String, DxccInfo.Info> resolve) {
        DxccInfo.Info i = resolve.apply(norm(call));
        return i == null ? null : i.continent();
    }
}
