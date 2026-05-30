package com.hamradio.jhub;

import com.hamradio.jhub.model.RigCapabilities;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hamlib {@code \dump_caps} and {@code \dump_state} text into a
 * {@link RigCapabilities}. Both blobs come back over the rigctld extended
 * ({@code +}) protocol as the body of a single reply terminated by
 * {@code RPRT 0}; {@code HamlibRigController.sendCommand} returns everything
 * before the RPRT line, so this parser is fed that raw body.
 *
 * <p>Formats verified against live rigctld 4.5.5 (dummy backend, model 1):
 *
 * <pre>
 * dump_caps:
 *   Caps dump for model: 1
 *   Model name:\tDummy
 *   Mfg name:\tHamlib
 *   Set functions: ... RIT ... XIT ...
 *   Get level: ... STRENGTH(..) ... RFPOWER(..) ...
 *   Set level: ... RFPOWER(..) ...
 *   Mode list: AM CW USB LSB RTTY FM WFM CWR RTTYR
 *   VFO list: VFOA VFOB ... Main MEM currVFO
 *   Can set Frequency:\tY
 *   Can set Split Freq:\tY
 *   Can set RIT:\tY
 *   ...
 *
 * dump_state:
 *   1            (protocol/version preamble — ignored)
 *   1
 *   0
 *   &lt;rx range lines&gt; low high modes lo_pwr hi_pwr vfo ant
 *   0 0 0 0 0 0 0          (RX terminator)
 *   &lt;tx range lines&gt;
 *   0 0 0 0 0 0 0          (TX terminator)
 *   &lt;tuning steps, filters, masks ...&gt;
 * </pre>
 *
 * Range lines mix decimal freqs with hex mode masks (e.g.
 * {@code 150000.000000 1500000000.000000 0x1ff 5000 100000 0x77e00007 0xf}),
 * so a "range line" is identified by: ≥7 whitespace tokens whose first two
 * parse as doubles. The all-zero 7-token line separates the RX block (first)
 * from the TX block (second); we keep only the TX ranges (band-keypad gating
 * is about where the rig can transmit).
 */
public final class RigCapsParser {

    private RigCapsParser() {}

    // RFPOWER must not match RFPOWER_METER / RFPOWER_METER_WATTS — anchor on the
    // trailing '(' that opens each level's "(min..max/step)" descriptor.
    private static final Pattern RFPOWER_TOK  = Pattern.compile("\\bRFPOWER\\(");
    private static final Pattern STRENGTH_TOK = Pattern.compile("\\bSTRENGTH\\(");

    /**
     * Parse a dump_caps body and (optionally) a dump_state body into a fresh
     * {@link RigCapabilities}. Either argument may be null/blank; whatever is
     * present is applied. {@link RigCapabilities#known} is set true if the
     * dump_caps body yielded a model line, a mode list, or any "Can set" flag.
     */
    public static RigCapabilities parse(String dumpCaps, String dumpState) {
        RigCapabilities c = new RigCapabilities();
        if (dumpCaps != null && !dumpCaps.isBlank())  parseCaps(c, dumpCaps);
        if (dumpState != null && !dumpState.isBlank()) parseTxRanges(c, dumpState);
        return c;
    }

    private static void parseCaps(RigCapabilities c, String body) {
        boolean sawSomething = false;
        for (String raw : body.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("Caps dump for model:")) {
                c.model = intAfterColon(line);
                sawSomething = true;
            } else if (startsWithIgnoreCase(line, "Model name:")) {
                c.modelName = afterColon(line);
                sawSomething = true;
            } else if (startsWithIgnoreCase(line, "Mfg name:")) {
                c.mfgName = afterColon(line);
            } else if (startsWithIgnoreCase(line, "Mode list:")) {
                c.modes = tokensAfterColon(line);
                sawSomething = true;
            } else if (startsWithIgnoreCase(line, "VFO list:")) {
                c.vfoList = tokensAfterColon(line);
            } else if (startsWithIgnoreCase(line, "Set functions:")) {
                java.util.List<String> fns = tokensAfterColon(line);
                c.ritFunc = fns.contains("RIT");
                c.xitFunc = fns.contains("XIT");
            } else if (startsWithIgnoreCase(line, "Get level:")) {
                c.getStrength = STRENGTH_TOK.matcher(line).find();
                c.getRfPower  = RFPOWER_TOK.matcher(line).find();
            } else if (startsWithIgnoreCase(line, "Set level:")) {
                c.setRfPower  = RFPOWER_TOK.matcher(line).find();
            } else if (startsWithIgnoreCase(line, "Can ")) {
                applyCanFlag(c, line);
                sawSomething = true;
            }
        }
        if (sawSomething) c.known = true;
    }

    /** Map a "Can set/get X:\tY" line onto the matching capability boolean. */
    private static void applyCanFlag(RigCapabilities c, String line) {
        boolean yes = endsWithYes(line);
        // Compare on the label between "Can " and the colon.
        int colon = line.indexOf(':');
        if (colon < 0) return;
        String label = line.substring(0, colon).trim();   // e.g. "Can set Split Freq"
        switch (label) {
            case "Can set Frequency": c.setFreq   = yes; break;
            case "Can get Frequency": c.getFreq   = yes; break;
            case "Can set Mode":      c.setMode   = yes; break;
            case "Can set VFO":       c.setVfo    = yes; break;
            case "Can get VFO":       c.getVfo    = yes; break;
            case "Can set PTT":       c.setPtt    = yes; break;
            case "Can set Split VFO": c.splitVfo  = yes; break;
            case "Can set Split Freq":c.splitFreq = yes; break;
            case "Can set Split Mode":c.splitMode = yes; break;
            case "Can set RIT":       c.ritOffset = yes; break;
            case "Can set XIT":       c.xitOffset = yes; break;
            default: /* not a flag we gate on */ break;
        }
    }

    /** Extract the second freq-range block (TX) from a dump_state body. */
    private static void parseTxRanges(RigCapabilities c, String body) {
        int block = 0;   // 0 = RX block, 1 = TX block, 2 = done
        for (String raw : body.split("\\R")) {
            String[] t = raw.trim().split("\\s+");
            if (t.length < 7) continue;
            if (!isDouble(t[0]) || !isDouble(t[1])) continue;   // not a range line
            if (allZero(t)) {                                   // block terminator
                block++;
                if (block >= 2) break;
                continue;
            }
            if (block == 1) {
                long lo = (long) Double.parseDouble(t[0]);
                long hi = (long) Double.parseDouble(t[1]);
                if (hi > lo) c.txRanges.add(new long[]{lo, hi});
            }
        }
    }

    // ── helpers ──────────────────────────────────────────────────────

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /** Text after the first ':' , trimmed (handles the tab in "Model name:\tX"). */
    private static String afterColon(String line) {
        int i = line.indexOf(':');
        return i < 0 ? "" : line.substring(i + 1).trim();
    }

    private static int intAfterColon(String line) {
        String s = afterColon(line);
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static java.util.List<String> tokensAfterColon(String line) {
        String rest = afterColon(line);
        if (rest.isEmpty()) return new java.util.ArrayList<>();
        return new java.util.ArrayList<>(Arrays.asList(rest.split("\\s+")));
    }

    /** True if the trailing token (after the last whitespace/tab) is "Y". */
    private static boolean endsWithYes(String line) {
        String v = afterColon(line);
        return v.equalsIgnoreCase("Y") || v.equalsIgnoreCase("Yes");
    }

    private static boolean isDouble(String s) {
        if (s.isEmpty()) return false;
        try { Double.parseDouble(s); return true; }
        catch (NumberFormatException e) { return false; }
    }

    private static boolean allZero(String[] tokens) {
        for (String t : tokens) {
            if (!isDouble(t) || Double.parseDouble(t) != 0.0) return false;
        }
        return true;
    }
}
