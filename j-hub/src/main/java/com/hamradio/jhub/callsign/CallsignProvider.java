package com.hamradio.jhub.callsign;

/** Provider interface for callsign data sources. */
public interface CallsignProvider {

    /** Short identifier used in config and logs: "qrz", "hamqth", "hamdb", "callook". */
    String name();

    /** True if this provider has credentials/availability to attempt a lookup. */
    boolean isAvailable();

    /**
     * Looks up the callsign. Returns a result with found=false if the call is unknown.
     * Throws only on infrastructure errors (network failure, parse error, etc.).
     */
    CallsignResult lookup(String callsign) throws Exception;
}
