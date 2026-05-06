package com.jlog.util;

/**
 * MacroVariableEngine — substitutes the standard ARS Suite macro variables
 * inside a template string. Used by both the J-Log macro engine (CW + voice +
 * digital macros) and the J-Digi macro bar.
 *
 * <p>Supported placeholders:
 * <pre>
 *   {MYCALL}  — operator's own callsign
 *   {CALL}    — DX (worked) callsign
 *   {RST}     — RST sent (legacy alias for {RST_S})
 *   {RST_S}   — RST sent
 *   {RST_R}   — RST received
 *   {NAME}    — DX operator name (defaults to "OM" if blank)
 *   {EXCH}    — contest exchange (notes/exchange field)
 *   {SERIAL}  — zero-padded serial number, three digits (e.g. "007")
 *   {NR}      — bare serial number (e.g. "7")
 *   {FREQ}    — rig frequency in MHz, three decimals (e.g. "14.225")
 *   {BAND}    — derived band tag (e.g. "20m")
 *   {MODE}    — current operating mode
 * </pre>
 *
 * <p>Unknown placeholders are left untouched so future variables don't break
 * existing templates. Pure Java; no JavaFX or platform dependencies.
 */
public final class MacroVariableEngine {

    private MacroVariableEngine() {}

    /** Mutable bag of QSO + rig state used to expand a template. */
    public static final class Context {
        public String myCall       = "";
        public String dxCall       = "";
        public String rstSent      = "";
        public String rstReceived  = "";
        public String name         = "";
        public String exchange     = "";
        public int    serialNumber = 0;
        public long   frequencyHz  = 0L;
        public String mode         = "";
    }

    /**
     * Expand the variables in {@code template} using values from {@code ctx}.
     * Returns the original string unchanged when {@code template} or {@code ctx}
     * is null or empty.
     */
    public static String substitute(String template, Context ctx) {
        if (template == null || template.isEmpty() || ctx == null) return template;

        String myCall = nz(ctx.myCall);
        String dxCall = nz(ctx.dxCall).trim();
        String rstS   = blankToDefault(ctx.rstSent,     "599");
        String rstR   = blankToDefault(ctx.rstReceived, "599");
        String name   = blankToDefault(ctx.name,        "OM");
        String exch   = nz(ctx.exchange);
        String mode   = nz(ctx.mode);

        String freq = ctx.frequencyHz > 0
                ? String.format("%.3f", ctx.frequencyHz / 1_000_000.0)
                : "--";
        String band = ctx.frequencyHz > 0 ? bandFromHz(ctx.frequencyHz) : "--";

        String serialPadded = ctx.serialNumber > 0
                ? String.format("%03d", ctx.serialNumber) : "";
        String serialBare   = ctx.serialNumber > 0
                ? Integer.toString(ctx.serialNumber)     : "";

        return template
                .replace("{MYCALL}", myCall)
                .replace("{CALL}",   dxCall)
                .replace("{RST_S}",  rstS)
                .replace("{RST_R}",  rstR)
                .replace("{RST}",    rstS)
                .replace("{NAME}",   name)
                .replace("{EXCH}",   exch)
                .replace("{SERIAL}", serialPadded)
                .replace("{NR}",     serialBare)
                .replace("{FREQ}",   freq)
                .replace("{BAND}",   band)
                .replace("{MODE}",   mode);
    }

    /** Map a frequency in Hz to its band tag, or "--" if outside known ham bands. */
    public static String bandFromHz(long hz) {
        long k = hz / 1000;
        if (k >=   1800 && k <=   2000) return "160m";
        if (k >=   3500 && k <=   4000) return "80m";
        if (k >=   5300 && k <=   5500) return "60m";
        if (k >=   7000 && k <=   7300) return "40m";
        if (k >=  10100 && k <=  10150) return "30m";
        if (k >=  14000 && k <=  14350) return "20m";
        if (k >=  18068 && k <=  18168) return "17m";
        if (k >=  21000 && k <=  21450) return "15m";
        if (k >=  24890 && k <=  24990) return "12m";
        if (k >=  28000 && k <=  29700) return "10m";
        if (k >=  50000 && k <=  54000) return "6m";
        if (k >= 144000 && k <= 148000) return "2m";
        if (k >= 222000 && k <= 225000) return "1.25m";
        if (k >= 420000 && k <= 450000) return "70cm";
        return "--";
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String blankToDefault(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }
}
