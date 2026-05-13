package com.jlog.bandplan;

/** What an amateur band segment is allocated to. Single dimension —
 *  for finer detail (e.g. "narrow data" vs "wider data") use the
 *  {@link BandSegment#label()} on the segment itself. */
public enum Activity {
    /** CW (continuous wave / Morse code). */
    CW,
    /** Narrow- or wide-bandwidth digital modes (RTTY, PSK31, FT8, etc.). */
    DATA,
    /** Voice — SSB, AM, FM. */
    PHONE,
    /** Image — SSTV, FSTV. */
    IMAGE,
    /** Beacons and propagation indicators (NCDXF, WSPR, etc.). */
    BEACON,
    /** Satellite uplink/downlink subbands. */
    SAT,
    /** Mixed-mode / all-mode segment with no single dominant activity. */
    MIXED;
}
