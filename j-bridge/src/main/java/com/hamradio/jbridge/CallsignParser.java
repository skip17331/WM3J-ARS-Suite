package com.hamradio.jbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * CallsignParser — extracts the DX callsign from a WSJT-X decoded message.
 *
 * WSJT-X message formats handled:
 *   CQ W3ABC FM19           → W3ABC
 *   CQ DX W3ABC FM19        → W3ABC
 *   CQ NA W3ABC FM19        → W3ABC  (directional CQ)
 *   W3ABC K3XYZ -10         → W3ABC  (first call is DX)
 *   W3ABC K3XYZ RR73        → W3ABC
 *   TU; K3XYZ W3ABC -08     → W3ABC  (DX is 2nd call after "TU;")
 *   K3XYZ W3ABC R-08        → W3ABC  (R-report; DX is 2nd call)
 *   DE W3ABC FM19           → W3ABC
 *
 * Returns null rather than guessing when the callsign cannot be determined
 * with confidence.
 */
public class CallsignParser {

    private static final Logger log = LoggerFactory.getLogger(CallsignParser.class);

    /**
     * Matches standard amateur callsigns including portable/prefix variants:
     *   W3ABC, VK3ABC, OH2BH, 5B4AIF, OZ/DL3YM, W3ABC/P
     */
    private static final Pattern CALLSIGN_RE = Pattern.compile(
        "\\b([A-Z0-9]{1,3}/)?([A-Z]{1,2}[0-9][A-Z]{1,4})(/[A-Z0-9]+)?\\b"
    );

    /** Tokens that are never callsigns in WSJT-X messages */
    private static final Set<String> RESERVED = new HashSet<>(Arrays.asList(
        "CQ","DE","DX","NA","EU","AS","AF","OC","SA","ANT",
        "RR73","73","RRR","TU","ACK","R","RR"
    ));

    private CallsignParser() {}

    /**
     * Extract the DX callsign from a WSJT-X decoded message string.
     *
     * @param message raw message text (may be null)
     * @return callsign in upper-case, or null if not determined
     */
    public static String extractDxCallsign(String message) {
        if (message == null || message.isBlank()) return null;

        String msg = message.trim().toUpperCase();

        // ── CQ [<modifier>] <CALL> [<GRID>] ──────────────────────────────────
        if (msg.startsWith("CQ ")) {
            for (String token : msg.substring(3).split("\\s+")) {
                if (!RESERVED.contains(token) && isCallsign(token)) {
                    return stripPortableSuffix(token);
                }
            }
            return null;
        }

        // ── DE <CALL> [<GRID>] ────────────────────────────────────────────────
        if (msg.startsWith("DE ")) {
            String[] parts = msg.split("\\s+");
            if (parts.length >= 2 && isCallsign(parts[1])) {
                return stripPortableSuffix(parts[1]);
            }
            return null;
        }

        // ── TU; <OUR_CALL> <DX_CALL> <REPORT> ────────────────────────────────
        if (msg.startsWith("TU;")) {
            String[] parts = msg.split("\\s+");
            if (parts.length >= 3 && isCallsign(parts[2])) {
                return stripPortableSuffix(parts[2]);
            }
            return null;
        }

        // ── Two-callsign exchange ─────────────────────────────────────────────
        String[] tokens = msg.split("\\s+");
        java.util.List<String> calls = new java.util.ArrayList<>();
        for (String t : tokens) {
            if (!RESERVED.contains(t) && isCallsign(t)) calls.add(stripPortableSuffix(t));
        }

        if (calls.size() == 1) return calls.get(0);

        if (calls.size() >= 2) {
            // If third token is an R-report: format is <OUR> <DX> R<REPORT> → DX is second
            if (tokens.length >= 3 && tokens[2].matches("R[+-]?\\d+")) {
                return calls.get(1);
            }
            // Default: first callsign is DX station
            return calls.get(0);
        }

        log.trace("No callsign extracted from: {}", message);
        return null;
    }

    /**
     * Returns true if the token looks like a valid amateur callsign.
     * Rejects grid squares (FM19), signal reports (-10), and reserved words.
     */
    public static boolean isCallsign(String token) {
        if (token == null || token.length() < 3 || token.length() > 14) return false;
        if (RESERVED.contains(token)) return false;
        // Reject Maidenhead grid locators: 2 letters + 2 digits [+ 2 letters]
        if (token.matches("[A-R]{2}[0-9]{2}([A-X]{2})?")) return false;
        // Reject signal reports
        if (token.matches("[R]?[+-]?\\d+")) return false;
        return CALLSIGN_RE.matcher(token).matches();
    }

    /**
     * Strip a trailing portable/mobile suffix from a callsign.
     * "W3ABC/P" → "W3ABC"; preserves prefix separators like "OZ/DL3YM".
     */
    private static String stripPortableSuffix(String call) {
        int slash = call.lastIndexOf('/');
        if (slash > 0) {
            String suffix = call.substring(slash + 1);
            // Suffix of 1-2 chars is a portable/mobile designator
            if (suffix.length() <= 2 && suffix.matches("[A-Z0-9]+")) {
                return call.substring(0, slash);
            }
        }
        return call;
    }
}
