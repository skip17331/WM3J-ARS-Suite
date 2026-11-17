package com.jlog.scoring;

import java.util.Locale;

/**
 * The operator/station facts a scorer needs, gathered once by the caller so
 * scoring never reaches into AppConfig / the UI. {@code ownCallsign} should
 * already be resolved (station call, falling back to the contest "sent" call);
 * {@code sentQth} is the operator's own sent QTH (for qso_party in/out logic).
 * {@code power} and {@code station} are the entry's Cabrillo CATEGORY-POWER
 * (QRP / LOW / HIGH) and CATEGORY-STATION (FIXED / MOBILE / PORTABLE / ROVER ...),
 * upper-cased; QSO parties use them for power and station-category multipliers
 * and for the mobile / rover county bonuses. Blank when unknown.
 */
public record StationContext(String ownCallsign, String ownGrid, String sentQth, String power, String station) {
    public static StationContext of(String ownCallsign, String ownGrid, String sentQth) {
        return of(ownCallsign, ownGrid, sentQth, "", "");
    }
    public static StationContext of(String ownCallsign, String ownGrid, String sentQth, String power, String station) {
        return new StationContext(
            ownCallsign == null ? "" : ownCallsign,
            ownGrid == null ? "" : ownGrid,
            sentQth == null ? "" : sentQth,
            up(power), up(station));
    }
    private static String up(String s) { return s == null ? "" : s.trim().toUpperCase(Locale.ROOT); }
}
