package com.ars.fx.data;

import com.jlog.bandplan.BandplanLoader;

import java.util.Locale;

/**
 * Band lookups for the whole app, from the suite's shared band plan ({@link BandplanLoader} in j-log-common) — the one
 * place band edges live. The log entry, J-Log-Quick, the cluster feeds, the rig panel and the digital modules all ask
 * here, so a frequency has the same band everywhere. The operator's IARU region (J-Hub ▸ Station) selects the plan.
 */
public final class Bands {
    private Bands() {}

    private static BandplanLoader plan() { return BandplanLoader.getInstance(); }

    /** Point the shared plan at the region chosen in J-Hub ▸ Station ("R1" / "R2" / "R3"). Call at start-up and on change. */
    public static void applyRegion() {
        String r = HubConfig.get("station.iaru", "R2").trim().toUpperCase(Locale.ROOT);
        BandplanLoader.setActiveRegion(r.startsWith("IARU") ? r : "IARU-" + r);
    }

    /** "20m" — or "" outside every amateur band. */
    public static String of(long hz) { String b = plan().bandName(hz); return b == null ? "" : b; }

    /** Same, for a frequency typed in MHz ("14.074" or "14,074"); null if it cannot be read or is out of band. */
    public static String ofMhz(String mhz) {
        String n = normaliseMhz(mhz);
        if (n == null) return null;
        try {
            String b = plan().bandName(Math.round(Double.parseDouble(n) * 1_000_000.0));
            return b == null || b.isEmpty() ? null : b;
        } catch (Exception e) { return null; }
    }

    /**
     * A typed or stored frequency as plain MHz ("14.025"), the unit the log and ADIF use. Accepts what operators type:
     * <ul>
     *   <li>kHz, as contest loggers take it: "14025" is 14.025 MHz. It used to be read as 14,025 MHz, so the QSO got no band
     *       and was exported as {@code <FREQ:5>14025}, which LoTW and Club Log reject;</li>
     *   <li>a decimal comma: "14,025" (it used to lose the comma and become 14,025 MHz);</li>
     *   <li>the spot-list style "14.025.0".</li>
     * </ul>
     * A number of 1000 or more is taken as kHz when that lands in a band, except at 10 GHz and up where MHz is what microwave
     * operators type (10368). Anything else is returned trimmed and unchanged; null and blank stay as they are.
     */
    public static String normaliseMhz(String s) {
        if (s == null) return null;
        String t = s.replace(" ", "").trim();
        if (t.isEmpty()) return t;
        if (t.matches("\\d+\\.\\d{3}\\.\\d+")) t = t.substring(0, t.lastIndexOf('.')) + t.substring(t.lastIndexOf('.') + 1);
        if (t.contains(",")) t = t.contains(".") ? t.replace(",", "") : t.replace(',', '.');
        double v;
        try { v = Double.parseDouble(t); } catch (NumberFormatException e) { return s.trim(); }
        if (v <= 0) return s.trim();
        if (v >= 1000 && v < 10_000_000 && inBand(v / 1000) && (v < 10_000 || !inBand(v))) return plain(v / 1000);
        return plain(v);
    }

    private static boolean inBand(double mhz) {
        String b = plan().bandName(Math.round(mhz * 1_000_000.0));
        return b != null && !b.isEmpty();
    }

    private static String plain(double mhz) {
        return new java.math.BigDecimal(mhz).setScale(6, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** "20" for "20m", "70" for "70cm" … for panels that label bands by number only; null out of band. */
    public static String number(long hz) {
        String b = of(hz);
        return b.isEmpty() ? null : b.replaceAll("[^0-9.]", "");
    }

    /** The band plan's activity at this frequency ("CW", "DATA", "PHONE"...), or "" when it is not in a band or the plan is silent. */
    public static String activityAt(long hz) {
        BandplanLoader.Description d = plan().describe(hz);
        return d == null || d.activity == null ? "" : d.activity.name();
    }

    /** Which band plan the answer for this frequency came from, in words: "US FCC Part 97" or "IARU Region 2" ("" when not in a band). */
    public static String planName(long hz) {
        BandplanLoader.Description d = plan().describe(hz);
        if (d == null || d.sourceId.isBlank()) return "";
        if ("US".equalsIgnoreCase(d.sourceId)) return "US FCC Part 97";
        if (d.sourceId.toUpperCase(Locale.ROOT).startsWith("IARU-R")) return "IARU Region " + d.sourceId.substring(6);
        return d.sourceId;
    }

    /** {@link #describe} plus the band plan it came from, e.g. "20m · CW / data only · US FCC Part 97 band plan". */
    public static String describeWithPlan(long hz) {
        String d = describe(hz), p = planName(hz);
        return d.isEmpty() || p.isEmpty() ? d : d + " · " + p + " band plan";
    }

    /** What the band plan says is at this frequency, e.g. "20m · CW segment" — "" when it is not in a band. */
    public static String describe(long hz) {
        BandplanLoader.Description d = plan().describe(hz);
        if (d == null) return "";
        return d.band + " · " + (d.label == null || d.label.isBlank() ? d.activity.toString() : d.label);
    }
}
