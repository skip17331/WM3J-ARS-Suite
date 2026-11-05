package com.ars.fx.data;

import java.util.List;

/**
 * The arithmetic of an ARRL Field Day entry. ARRL does not score Field Day from the log: the entrant declares a power multiplier
 * and bonus points on the summary sheet, and the score is the QSO points times the multiplier plus the bonus points. So this is a
 * worksheet, not a scorer - it adds up what the operator types, and it does not decide what a bonus is worth: those values change
 * from year to year, so only the two the sheet states plainly are filled in (100 points per emergency-power transmitter up to 20, and
 * the web-submission bonus); every other value is entered from the current year's rules.
 */
public final class FieldDaySheet {
    private FieldDaySheet() {}

    /** The bonus categories on the summary sheet (names as on the 2024 sheet). {@code points} is a suggested value, or -1 to enter it yourself. */
    public record Category(String key, String name, int points) {}

    public static final int EMERGENCY_POWER_PER_TX = 100, EMERGENCY_POWER_MAX_TX = 20;

    public static final List<Category> CATEGORIES = List.of(
            new Category("media", "Media publicity", -1),
            new Category("location", "Public location", -1),
            new Category("booth", "Public information booth", -1),
            new Category("sm", "Message to the ARRL Section Manager / SEC", -1),
            new Category("w1aw", "W1AW Field Day message", -1),
            new Category("nts", "Messages handled (NTS / ICS-213)", -1),
            new Category("satellite", "Satellite QSO", -1),
            new Category("natural", "Natural power QSOs", -1),
            new Category("official", "Elected official site visit", -1),
            new Category("agency", "Served-agency site visit", -1),
            new Category("education", "Educational activity", -1),
            new Category("youth", "Youth element", -1),
            new Category("gota", "GOTA station", -1),
            new Category("web", "Submitted through the web app", 50),
            new Category("safety", "Safety officer", -1),
            new Category("site", "Site responsibilities", -1),
            new Category("social", "Social media", -1));

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
