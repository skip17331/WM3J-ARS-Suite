package com.jlog.scoring;

/**
 * The operator/station facts a scorer needs, gathered once by the caller so
 * scoring never reaches into AppConfig / the UI. {@code ownCallsign} should
 * already be resolved (station call, falling back to the contest "sent" call);
 * {@code sentQth} is the operator's own sent QTH (for qso_party in/out logic).
 */
public record StationContext(String ownCallsign, String ownGrid, String sentQth) {
    public static StationContext of(String ownCallsign, String ownGrid, String sentQth) {
        return new StationContext(
            ownCallsign == null ? "" : ownCallsign,
            ownGrid == null ? "" : ownGrid,
            sentQth == null ? "" : sentQth);
    }
}
