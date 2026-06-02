package com.jlog.scoring;

import java.util.Locale;
import java.util.Set;

/**
 * Shared helpers for the reusable {@code multiplierType:"qso_party"}
 * scoring engine. The contest-specific parameters live in the plugin's
 * {@code scoringRules.qsoParty} block; this class only provides the
 * mode-class normalisation and the in-state / grid classification that
 * every state &amp; province QSO party shares. Claimed/running — each
 * sponsor's log checker re-adjudicates.
 */
public final class QsoParty {

    private QsoParty() {}

    /** Multiplier / point mode class. CW is its own; all voice modes are
     *  "PH"; RTTY is the legacy "RY" class (kept distinct because some
     *  parties — e.g. VTQP — score and multiply RTTY differently from the
     *  WSJT group); every other data mode (FT8/FT4/JS8/MFSK/PSK/…) is the
     *  WSJT digital class "DG". */
    public static String modeClass(String mode) {
        if (mode == null) return "DG";
        String m = mode.trim().toUpperCase(Locale.ROOT);
        if (m.equals("CW")) return "CW";
        if (m.equals("SSB") || m.equals("USB") || m.equals("LSB")
                || m.equals("FM") || m.equals("AM") || m.equals("PHONE")
                || m.equals("DV") || m.equals("VOICE")) return "PH";
        if (m.equals("RTTY") || m.equals("RY")) return "RY";
        return "DG";
    }

    /** Mode class with optional RTTY→digital merge: when {@code mergeRtty}
     *  the legacy "RY" class folds into "DG" (parties that treat RTTY and
     *  the WSJT modes as one — e.g. SCQP: an FT8 and an RTTY QSO with the
     *  same station on a band are a dupe). */
    public static String modeClass(String mode, boolean mergeRtty) {
        String c = modeClass(mode);
        return (mergeRtty && "RY".equals(c)) ? "DG" : c;
    }

    /** Mode class with optional RTTY→digital merge AND optional
     *  CW+digital merge: when {@code mergeCw} CW, RTTY and the WSJT
     *  digital class all fold into one "CD" class (parties that score
     *  and multiply CW &amp; digital together, e.g. LAQP), Phone stays
     *  separate. {@code mergeCw} takes precedence over {@code mergeRtty}. */
    public static String modeClass(String mode, boolean mergeRtty, boolean mergeCw) {
        String c = modeClass(mode, mergeRtty);
        if (mergeCw && ("CW".equals(c) || "RY".equals(c) || "DG".equals(c)))
            return "CD";
        return c;
    }

    /** Base callsign with any portable/county suffix stripped
     *  (WW4SF/CHAR → WW4SF) for bonus-station identity. */
    public static String baseCall(String call) {
        if (call == null) return "";
        String c = call.trim().toUpperCase(Locale.ROOT);
        int i = c.indexOf('/');
        return i > 0 ? c.substring(0, i) : c;
    }

    /** True if the received QTH code is one of the contest's in-state
     *  county codes (case-insensitive). */
    public static boolean isInStateQth(String qth, Set<String> inStateCounties) {
        if (qth == null || inStateCounties == null) return false;
        return inStateCounties.contains(qth.trim().toUpperCase(Locale.ROOT));
    }

    /** County test with an optional by-exclusion rule: {@code byExcl}
     *  treats any non-blank QTH that is not the literal "DX" and not
     *  exactly 2 characters (every US state &amp; Canadian prov/territory
     *  code is 2 chars) as an in-state county — for parties whose county
     *  codes are mixed length (GAQP/MiQP). Else delegates to the
     *  set/length rule. */
    public static boolean isCounty(String qth, Set<String> inStateCounties,
                                   int len, boolean byExcl) {
        if (qth == null) return false;
        if (!byExcl) return isCounty(qth, inStateCounties, len);
        String q = qth.trim().toUpperCase(Locale.ROOT);
        return !q.isEmpty() && !q.equals("DX") && q.length() != 2;
    }

    /** County test honouring an optional length rule. When {@code len > 0}
     *  any non-blank QTH of exactly that length other than the literal
     *  "DX" is treated as an in-state county (for parties whose official
     *  3-letter code list isn't bundled — county codes are N chars,
     *  W/VE state/prov 2, DX the literal "DX"). Otherwise falls back to
     *  explicit-set membership. */
    public static boolean isCounty(String qth, Set<String> inStateCounties, int len) {
        if (qth == null) return false;
        String q = qth.trim().toUpperCase(Locale.ROOT);
        if (q.isEmpty() || q.equals("DX")) return false;
        if (len > 0) return q.length() == len;
        return inStateCounties != null && inStateCounties.contains(q);
    }

    /** 4-character Maidenhead field+square, upper-cased (FN34km → FN34);
     *  null if fewer than 4 characters. */
    public static String grid4(String grid) {
        if (grid == null) return null;
        String g = grid.trim().toUpperCase(Locale.ROOT);
        return g.length() >= 4 ? g.substring(0, 4) : null;
    }

    /** Case-insensitive membership test for special-callsign sets
     *  (bonus / club calls), tolerant of surrounding whitespace. */
    public static boolean callIn(String call, Set<String> set) {
        if (call == null || set == null) return false;
        return set.contains(call.trim().toUpperCase(Locale.ROOT));
    }
}
