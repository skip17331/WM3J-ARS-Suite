package com.ars.fx.data;

import java.util.List;

/**
 * The arithmetic of an ARRL Field Day entry. ARRL does not score Field Day from the log: the entrant declares a power multiplier
 * and bonus points on the summary sheet, and the score is the QSO points times the multiplier plus the bonus points (bonus points are
 * added after the multiplier). So this is a worksheet, not a scorer - it adds up what the operator claims. The bonus values and caps
 * are those of the 2026 ARRL Field Day rules (revised 1 March 2026, section 7.3); check them each year, as ARRL changes them.
 */
public final class FieldDaySheet {
    private FieldDaySheet() {}

    /**
     * One bonus on the summary sheet. The operator enters a count (1 to claim a flat bonus, or how many for a per-unit one); the points
     * are the count (capped at {@code maxUnits}, 0 = no cap) times {@code unitPoints}. {@code classes} says which entry classes may claim it.
     */
    public record Category(String key, String name, int unitPoints, int maxUnits, String classes, String unit) {
        /** The points for a claim of {@code count}: capped, never negative. */
        public int points(int count) {
            int n = Math.max(0, count);
            if (maxUnits > 0) n = Math.min(n, maxUnits);
            return n * unitPoints;
        }
    }

    public static final int EMERGENCY_POWER_PER_TX = 100, EMERGENCY_POWER_MAX_TX = 20;

    /** The power multipliers (rule 7.2): 5 for 5 W or less on batteries / solar, 2 for 5 W on mains or a generator or up to 100 W, 1 above 100 W. */
    public static final int[] POWER_MULTIPLIERS = {5, 2, 1};

    private static Category flat(String key, String name, int pts, String classes) { return new Category(key, name, pts, 1, classes, ""); }

    public static final List<Category> CATEGORIES = List.of(
            flat("media", "Media publicity", 100, "all"),
            flat("location", "Public location", 100, "A, B, F"),
            flat("booth", "Public information table", 100, "A, B, F"),
            flat("sm", "Message to the Section Manager / SEC", 100, "all"),
            new Category("nts", "Messages handled (NTS / ICS-213)", 10, 10, "all", "messages"),
            flat("satellite", "Satellite QSO", 100, "A, B, F"),
            flat("natural", "Alternate power (5+ QSOs)", 100, "A, B, E, F"),
            flat("w1aw", "W1AW bulletin copied", 100, "all"),
            flat("education", "Educational activity", 100, "A, F; D, E with 3+"),
            flat("official", "Elected official site visit", 100, "all"),
            flat("agency", "Served-agency site visit", 100, "all"),
            new Category("gota", "GOTA station contacts", 5, 0, "A, F", "contacts"),
            flat("coach", "GOTA coach (10+ contacts)", 100, "A, F"),
            flat("web", "Submitted through the web app", 50, "all"),
            new Category("youth", "Youth: participants 18 or under", 20, 5, "A, C, D, E, F (Class B: 40 max)", "participants"),
            flat("social", "Social media", 100, "all"),
            flat("safety", "Safety officer", 100, "A only"),
            flat("site", "Site responsibilities", 50, "B, C, D, E, F"));

    /** 100 points for each emergency-power transmitter, up to 20 of them. */
    public static int emergencyPower(int transmitters) {
        return Math.max(0, Math.min(transmitters, EMERGENCY_POWER_MAX_TX)) * EMERGENCY_POWER_PER_TX;
    }

    /** QSO points times the power multiplier, plus the bonus points. A multiplier below 1 counts as 1. */
    public static long total(int qsoPoints, int powerMultiplier, long bonusPoints) {
        return (long) Math.max(0, qsoPoints) * Math.max(1, powerMultiplier) + Math.max(0, bonusPoints);
    }

    /** A typed number, or 0 for blank and rubbish. */
    public static int number(String s) {
        try { return s == null || s.isBlank() ? 0 : Math.max(0, Integer.parseInt(s.trim())); } catch (NumberFormatException e) { return 0; }
    }
}
