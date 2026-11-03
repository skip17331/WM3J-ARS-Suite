package com.ars.fx.data;

import java.util.*;
import java.util.function.Function;

/**
 * Antenna Workshop calculators — antenna designs, trap / loading-coil components and the J-Learn formula cards.
 * Pure functions: each {@link Calc} turns its input map (id → text) into labelled output rows. No UI here.
 * Ported from the original browser-side Antenna Workshop; the maths and wording are unchanged.
 */
public final class AntennaCalcs {
    private AntennaCalcs() {}

    public enum Kind { ANTENNA, COMPONENT, FORMULA }
    public record In(String id, String label, String def, String[][] choices, String hint) {
        public boolean select() { return choices != null; }
    }
    public record Calc(String id, String name, String section, Kind kind, List<In> inputs,
                       Function<Map<String, String>, List<String[]>> compute, String diagram, List<String> notes) {
        public Map<String, String> defaults() {
            Map<String, String> m = new LinkedHashMap<>();
            for (In i : inputs) m.put(i.id(), i.def());
            return m;
        }
    }

    private static final Map<String, Calc> CALCS = new LinkedHashMap<>();
    public static Calc get(String id) { return CALCS.get(id); }
    public static Collection<Calc> all() { return CALCS.values(); }
    public static List<Calc> of(Kind k) { return CALCS.values().stream().filter(c -> c.kind() == k).toList(); }
    /** The calculator whose J-Learn section id (e.g. "09-02") matches, or null. */
    public static Calc forSection(String section) {
        for (Calc c : CALCS.values()) if (c.section().equals(section)) return c;
        return null;
    }

    // ---- small helpers ----------------------------------------------------------------------------------
    private static In num(String id, String label, double def) { return new In(id, label, num(def), null, null); }
    private static In num(String id, String label, double def, String hint) { return new In(id, label, num(def), null, hint); }
    private static In sel(String id, String label, String def, String... cv) {
        String[][] ch = new String[cv.length / 2][2];
        for (int i = 0; i < ch.length; i++) { ch[i][0] = cv[2 * i]; ch[i][1] = cv[2 * i + 1]; }
        return new In(id, label, def, ch, null);
    }
    /** JS-style number to string: 14 → "14", 14.15 → "14.15". */
    static String num(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return String.valueOf((long) d);
        return String.valueOf(d);
    }
    private static String fx(double d, int n) { return String.format(Locale.ROOT, "%." + n + "f", d); }
    /** JS parseFloat: NaN when unparseable. */
    static double pf(String s) {
        if (s == null) return Double.NaN;
        var m = java.util.regex.Pattern.compile("^\\s*([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)").matcher(s);
        return m.find() ? Double.parseDouble(m.group(1)) : Double.NaN;
    }
    private static double pf(Map<String, String> v, String k) { return pf(v.get(k)); }
    /** JS `parseFloat(x) || d`. */
    private static double pfd(Map<String, String> v, String k, double d) { double x = pf(v, k); return (Double.isNaN(x) || x == 0) ? d : x; }
    private static int pi(Map<String, String> v, String k, int d) { double x = pf(v, k); return (Double.isNaN(x) || (int) x == 0) ? d : (int) x; }
    private static String str(Map<String, String> v, String k) { String s = v.get(k); return s == null ? "" : s; }
    private static void row(List<String[]> r, String k, String val) { r.add(new String[]{k, val}); }
    private static List<String[]> rows(String[]... rs) { return new ArrayList<>(Arrays.asList(rs)); }
    private static String[] r(String k, String v) { return new String[]{k, v}; }

    public static String hamBandFor(double mhz) {
        if (mhz >= 1.8 && mhz <= 2.0) return "160m";
        if (mhz >= 3.5 && mhz <= 4.0) return "80m";
        if (mhz >= 7.0 && mhz <= 7.3) return "40m";
        if (mhz >= 10.1 && mhz <= 10.15) return "30m";
        if (mhz >= 14.0 && mhz <= 14.35) return "20m";
        if (mhz >= 18.06 && mhz <= 18.17) return "17m";
        if (mhz >= 21.0 && mhz <= 21.45) return "15m";
        if (mhz >= 24.89 && mhz <= 24.99) return "12m";
        if (mhz >= 28.0 && mhz <= 29.7) return "10m";
        if (mhz >= 50 && mhz <= 54) return "6m";
        if (mhz >= 144 && mhz <= 148) return "2m";
        if (mhz >= 420 && mhz <= 450) return "70cm";
        return "out of band";
    }

    private static void add(String id, String name, String section, Kind kind, List<In> inputs,
                            Function<Map<String, String>, List<String[]>> compute, String diagram, String... notes) {
        CALCS.put(id, new Calc(id, name, section, kind, inputs, compute, diagram, List.of(notes)));
    }

    static {
        // ===================================================================================== ANTENNAS
        add("flat-dipole", "Flat Dipole", "09-02", Kind.ANTENNA, List.of(
                num("freq", "Frequency (MHz)", 14.150),
                new In("k", "Length factor (k)", "468", null, "Imperial: 468 (thin wire). Use 462 for thick aluminum, 471 for very thin wire."),
                sel("units", "Display units", "ft", "ft", "feet (and inches)", "m", "meters")),
            v -> {
                double f = pfd(v, "freq", 14.150), k = pfd(v, "k", 468);
                double ft = k / f, m = ft * 0.3048, half = ft / 2;
                boolean mt = "m".equals(v.get("units"));
                return rows(
                    r("Total length", mt ? fx(m, 2) + " m" : fx(ft, 2) + " ft  (" + fx(ft * 12, 1) + " in)"),
                    r("Each leg (half)", mt ? fx(m / 2, 2) + " m" : fx(half, 2) + " ft  (" + fx(half * 12, 1) + " in)"),
                    r("Free-space λ/2", mt ? fx(150 / f, 2) + " m" : fx(492 / f, 2) + " ft  (no end-effect)"),
                    r("Expected feed Z (free space)", "~73 Ω resistive at resonance"),
                    r("Practical feed Z", "~50–73 Ω depending on height; lower height drops below 50 Ω"));
            },
            """
               support ─────────────────────●─────────────────────── support
                                            │
                                          feed
                                            │
                                        coax to rig
                   ←───────────  half-wave  ───────────→""",
            "Trim shorter to raise resonant frequency, longer to lower it. ~1 cm = ~10 kHz on 20m.",
            "Mount at least λ/4 above ground for usable patterns; λ/2 or higher for DX.",
            "1:1 current balun at the feedpoint suppresses common-mode current on the coax shield.");

        add("inverted-v", "Inverted-V Dipole", "09-03", Kind.ANTENNA, List.of(
                num("freq", "Frequency (MHz)", 7.150), num("apex", "Apex height (ft)", 35),
                num("angle", "Droop angle from horizontal (deg)", 30)),
            v -> {
                double f = pfd(v, "freq", 7.150), angleDeg = pfd(v, "angle", 30);
                double corr = 1 - 0.03 * (angleDeg / 45);
                double invLen = (468 / f) * corr, legSlope = invLen / 2;
                double apex = pfd(v, "apex", 35);
                double tip = apex - legSlope * Math.cos((90 - angleDeg) * Math.PI / 180);
                return rows(
                    r("Total length (corrected)", fx(invLen, 2) + " ft  (" + fx(invLen * 0.3048, 2) + " m)"),
                    r("Each leg (sloped)", fx(legSlope, 2) + " ft along the wire"),
                    r("Tip height above ground", fx(Math.max(0, tip), 1) + " ft (keep ≥ 8 ft for safety)"),
                    r("Expected feed Z", "~50 Ω at typical droop angles (lower than flat dipole)"),
                    r("Match to 50 Ω coax", "1:1 balun at apex"));
            },
            """
                                       ●  ← apex (mast / tree)
                                      /│\\
                                     / │ \\
                                    /  │  \\
                                   /  feed  \\
                                  /    │    \\
                                 /  coax to rig  \\
                                /                 \\
                          stake ●─── ground ───● stake""",
            "Droop 30–45° is typical; steeper makes the antenna more vertical (lower feed Z, higher angle).",
            "~3% length correction at 45° droop accounts for end-coupling and ground proximity at the tips.",
            "Apex must be at least λ/4 high for effective NVIS / regional coverage.");

        add("efhw-no-traps", "EFHW (No Traps)", "09-07", Kind.ANTENNA, List.of(
                num("freq", "Lowest band (MHz)", 7.150, "EFHW is half-wave at this frequency, harmonic-resonant on 2×, 3×, 4× of it."),
                sel("unun", "Unun ratio", "49", "49", "49:1 — most common", "64", "64:1 — slightly higher Z", "56", "56:1 — alternative")),
            v -> {
                double f = pfd(v, "freq", 7.150);
                double lenFt = 478 / f;
                List<String> harm = new ArrayList<>();
                for (int h : new int[]{1, 2, 3, 4}) {
                    double fh = f * h;
                    if (fh < 30) harm.add(h + "× (" + fx(fh, 2) + " MHz → " + hamBandFor(fh) + ")");
                }
                int unun = pi(v, "unun", 49);
                return rows(
                    r("Total wire length", fx(lenFt, 1) + " ft  (" + fx(lenFt * 0.3048, 2) + " m)"),
                    r("Counterpoise length", "~" + fx(lenFt * 0.05, 1) + " ft  (~5% of wire — soldered to unun ground side)"),
                    r("Expected end Z", unun == 49 ? "~2450 Ω" : unun == 64 ? "~3200 Ω" : "~2800 Ω"),
                    r("Unun primary:secondary", unun == 49 ? "2:14 turns" : unun == 64 ? "2:16 turns" : "2:14.7 turns (typical)"),
                    r("Harmonic resonance", String.join(";  ", harm)));
            },
            """
                   ┌──── 49:1 unun ────┐
                   │                   │
               ────┴──●═════════════════════════════ wire (length above) → end insulator
               coax   ↑
                      5% counterpoise tail""",
            "Harmonic resonance: a 40m EFHW (~67 ft) is also resonant on 20m, 15m, 10m.",
            "Counterpoise (~5% of wire length) is critical — without it, coax shield carries common-mode current.",
            "Run the wire in a straight line for cleanest impedance; gentle slope is fine.");

        add("j-pole", "J-Pole (VHF/UHF)", "09-09", Kind.ANTENNA, List.of(
                num("freq", "Center frequency (MHz)", 146.000),
                sel("mat", "Material", "copper", "copper", "½\" copper pipe — VF ≈ 0.96", "aluminum", "Aluminum tubing — VF ≈ 0.96",
                        "450ohm", "450Ω ladder line — VF ≈ 0.91")),
            v -> {
                double f = pfd(v, "freq", 146.0);
                double vf = "450ohm".equals(v.get("mat")) ? 0.91 : 0.96;
                double halfIn = (5905.5 * vf) / f, quarterIn = halfIn / 2, feedIn = quarterIn * 0.10;
                return rows(
                    r("Half-wave radiator (long element)", fx(halfIn, 2) + " in  (" + fx(halfIn * 2.54, 1) + " cm)"),
                    r("Quarter-wave matching stub", fx(quarterIn, 2) + " in  (" + fx(quarterIn * 2.54, 1) + " cm)"),
                    r("Stub spacing", "450ohm".equals(v.get("mat")) ? "450Ω ladder line spacing" : "About 1.5 inches center-to-center"),
                    r("Feed point from bottom", fx(feedIn, 2) + " in  — adjust ±¼\" for SWR minimum"),
                    r("Bottom of stub", "Connected (shorted) — tied to coax shield via short jumper"));
            },
            """
                 ▲  ← top of half-wave radiator (free end)
                 │
                 │
                 │   ← long element (½λ)
                 │
                 │   ┌──── 1.5" spacing ───┐
                 │   │                      │
                 │   │ ← short stub (¼λ)    │
                 │   │                      │
                 ●───●─── feed point  (slide to tune)
                          ↑
                        coax to rig""",
            "Tune by sliding the feedpoint up/down the stub for SWR minimum at the operating frequency.",
            "Vertical mounting; vertical polarization. Omnidirectional.",
            "No radials needed — stub provides the matching network.");

        add("vertical", "Vertical Antenna (¼λ)", "09-11", Kind.ANTENNA, List.of(
                num("freq", "Frequency (MHz)", 14.150), num("radials", "Radials installed", 16)),
            v -> {
                double f = pfd(v, "freq", 14.150);
                double ft = 234 / f;
                int radials = pi(v, "radials", 16);
                double total = radials * ft;
                String loss = radials >= 32 ? "< 0.5 dB" : radials >= 16 ? "~1 dB" : radials >= 8 ? "~2 dB" : radials >= 4 ? "~3 dB" : "~5 dB";
                return rows(
                    r("Vertical element length", fx(ft, 2) + " ft  (" + fx(ft * 12, 1) + " in)"),
                    r("Each radial length", fx(ft, 2) + " ft  (¼λ; trim to fit your yard)"),
                    r("Number of radials", radials + "  (32+ ideal, 16 acceptable, 4 minimum)"),
                    r("Total radial wire needed", fx(total, 0) + " ft"),
                    r("Estimated ground-system loss", loss),
                    r("Expected feed Z", "~36 Ω over a perfect ground; raised radials → ~50 Ω"),
                    r("Match to 50 Ω coax", radials >= 32 ? "Direct or 1:1 balun" : "1:1 balun + tuner if needed"));
            },
            """
                              ▲  ← tip of vertical (¼λ)
                              │
                              │
                              │
                              │   feed at base
                              ●═════════════════════ ground level
                            ╱─┼─╲
                          ╱   │   ╲
                        ╱   coax    ╲   ← buried radials (¼λ each, 16-32 ideal)
                      ╱       to       ╲
                    ╱         rig         ╲""",
            "¼λ vertical needs a radial system — verticals over poor ground can be 6 dB worse than horizontal dipoles.",
            "Radials don't need to be exactly ¼λ — any length helps; longer is better.",
            "Salt-marsh QTH: 4 radials may be enough. Sand or rocks: 32+ radials minimum for usable performance.");

        add("fan-dipole", "Fan Dipole", "09-04", Kind.ANTENNA, List.of(
                num("b1", "Band 1 (MHz)", 7.150), num("b2", "Band 2 (MHz)", 14.150),
                num("b3", "Band 3 (MHz, optional)", 21.200), num("b4", "Band 4 (MHz, optional)", 28.500),
                num("sep", "End-to-end separation (in)", 4, "Gap between adjacent dipole tips. 4-6\" is typical; closer = more interaction = more shortening.")),
            v -> {
                double sep = pfd(v, "sep", 4);
                double couple = Math.max(0.92, Math.min(0.98, 0.97 - 0.012 * (6 - sep)));
                List<String[]> out = new ArrayList<>();
                row(out, "Coupling correction", "×" + fx(couple, 3) + "  (~" + fx((1 - couple) * 100, 1) + "% shortening)");
                row(out, "Tip separation", fx(sep, 1) + " in between adjacent element tips");
                row(out, "Feed point", "All elements connected at one center insulator + 1:1 balun");
                for (String k : new String[]{"b1", "b2", "b3", "b4"}) {
                    double f = pf(v, k);
                    if (Double.isNaN(f) || f < 1) continue;
                    double corrected = (468 / f) * couple;
                    row(out, fx(f, 2) + " MHz (" + hamBandFor(f) + ")", fx(corrected, 2) + " ft total · " + fx(corrected / 2, 2) + " ft each leg");
                }
                row(out, "Match", "50 Ω coax + 1:1 current balun");
                return out;
            },
            """
                                   feed (1:1 balun)
                                          │
                                          │
                   ●─── 20m ─────────●─────────────── 20m ───●
                   ●─── 40m ────────────●───────────────── 40m ───●
                   ●─── 80m ────────────────●───────────────────── 80m ───●
                                          │
                                        coax to rig

                  All elements meet at one feedpoint. Tips ~4-6" apart.""",
            "Trim each band independently from longest to shortest — start with the lowest-frequency element.",
            "Each band is resonant on its own; shorter elements look like high-impedance opens at lower bands.",
            "Mutual coupling between elements means you must build all of them before final trim.",
            "Wider tip-spacing reduces coupling but takes more lateral space. 4-6\" is a good compromise.");

        add("trapped-dipole", "Trapped Dipole", "09-05", Kind.ANTENNA, trappedInputs(
                "Available total length (ft, tip-to-tip)", "0 = no constraint (full-size design). Otherwise the design will be shortened to fit.", false),
            v -> trapped(v, false),
            """
                          feed (1:1 balun)
                                 │
                                 ▼
              ●═══════════[Trap1]═══════════[Trap2]═══════════● outer end
              ←─inner─→  ←──outer 1──→  ←─── outer 2 ────→
               20m         opens at        opens at
               leg         14 MHz          7 MHz

              On 20m: traps look like high-Z opens, only inner radiates
              On 40m: trap 2 opens; inner + outer-1 radiate
              On 80m: both traps open; full antenna radiates

             SHORTY VARIANT (shorten > 0):
              Outer segments are physically shorter than standard. Traps are
              re-spec'd with HIGHER inductance (lower C) so their below-resonance
              reactance does more loading work. Same f_r, bigger coil, smaller cap.""",
            "Length-driven design: set Available Length to your space limit (tip-to-tip) and the calc fits a shortened design; leave it at 0 for full-size.",
            "Trim from highest band to lowest. Inner segment first (traps shorted), then add each outer band one at a time.",
            "Higher-frequency traps go closest to the feedpoint; lower-frequency traps farther out (one set per leg, mirrored).",
            "When the antenna is shortened, traps are re-spec'd with higher L and lower C (same f_r) so their below-resonance reactance does more loading work.",
            "Real bandwidth on the lowest band is typically 60–80% of the calculated estimate due to trap and coil losses; widen capacitive-hat additions if you need more.",
            "Feed via 50 Ω coax with a 1:1 current balun; a rig-end tuner helps on shortened designs.");

        add("ocf-dipole", "OCF Dipole (Windom)", "09-06", Kind.ANTENNA, List.of(
                num("freq", "Lowest band (MHz)", 7.150),
                num("offset", "Feed offset from center (%)", 33, "33% gives ~200Ω feed → 4:1 balun. 25% gives ~300Ω → 6:1 balun. Classic Windom: 36%."),
                sel("balun", "Balun ratio", "4", "4", "4:1 — 200Ω target (standard OCF)", "6", "6:1 — 300Ω target (Carolina Windom)", "9", "9:1 — 450Ω target")),
            v -> {
                double f = pfd(v, "freq", 7.150), offset = pfd(v, "offset", 33);
                double total = 468 / f, shortLeg = total * (offset / 100), longLeg = total - shortLeg;
                String b = str(v, "balun");
                String z = b.equals("4") ? "~200 Ω" : b.equals("6") ? "~300 Ω" : "~450 Ω";
                String ratio = b.equals("4") ? "4:1" : b.equals("6") ? "6:1" : "9:1";
                List<String> harm = new ArrayList<>();
                for (int h : new int[]{1, 2, 4, 8}) { double fh = f * h; if (fh < 30) harm.add(fx(fh, 2) + " MHz → " + hamBandFor(fh)); }
                return rows(
                    r("Total length", fx(total, 2) + " ft  (" + fx(total * 0.3048, 2) + " m)"),
                    r("Short leg", fx(shortLeg, 2) + " ft  (" + num(offset) + "% from center)"),
                    r("Long leg", fx(longLeg, 2) + " ft  (" + num(100 - offset) + "% from center)"),
                    r("Feed-point Z (target)", z),
                    r("Balun ratio", ratio + " current balun at the feedpoint"),
                    r("Bands covered (harmonics)", String.join("; ", harm)),
                    r("Common-mode choke", "Add a 1:1 current choke at the rig end of the coax"));
            },
            """
              ●──── short leg ────●─────── long leg ───────●
                                   │
                                 [Balun 4:1]
                                   │
                                 coax to rig

              Feed point is OFF-CENTER — typically 36% from one end (Carolina)
              or 33% (classic Windom). The off-center feed presents a 200-300 Ω
              impedance that's roughly band-independent across HF.""",
            "OCF dipoles are known harmonically — an 80m OCF feeds 80/40/20/15/10 with usable SWR.",
            "The off-center feed picks an impedance plateau across the harmonic bands.",
            "A 1:1 common-mode choke at the rig end is essential — OCF feedlines radiate without it.",
            "WARC bands (30/17/12) typically need a tuner; harmonic plateau doesn't cover them.",
            "Total length varies between vendors — measure and trim with an analyzer for best SWR.");

        add("efhw-trapped", "EFHW (Trapped)", "09-08", Kind.ANTENNA, trappedInputs(
                "Available total wire length (ft)", "0 = no constraint (full-size). Otherwise the wire will be shortened to fit.", true),
            v -> trapped(v, true),
            """
              unun
               │
               ●═══ seg 1 ═══[Trap1]═══ seg 2 ═══[Trap2]═══ seg 3 ═══●
               ↑    20m       opens     additional             additional
             5% c/p  half     14 MHz    for 40m                for 80m
             tail    wave

              On 20m: traps act as opens — only seg 1 radiates
              On 40m: trap 2 opens — seg 1 + seg 2 radiate
              On 80m: both traps open — full wire radiates

             SHORTY VARIANT (shorten > 0):
              Segments are physically shorter; traps re-spec'd with higher L
              (lower C) to provide extra inductive loading at lower bands.
              At seg-1 shortening > 10% an inline coil is added inside seg 1.""",
            "Length-driven design: set Available Length to your wire-length budget; leave it at 0 for full-size.",
            "Build & trim from the unun outward, one band at a time. Higher-frequency traps live closer to the unun.",
            "When shortened, traps get re-spec'd (higher L, lower C, same f_r) and a seg-1 inline coil may be added.",
            "Trap voltage rating is the dominant constraint — EFHW traps see roughly 2.5× the voltage of dipole traps at the same power. Vacuum caps are common for shorty designs > 100 W.",
            "Counterpoise tail (~5% of innermost segment) on the unun ground side is essential. A 1:1 common-mode choke at the rig end is mandatory.",
            "For shortened designs, expect to use a tuner at the rig for full-band SWR coverage.");

        add("yagi", "Yagi-Uda", "09-10", Kind.ANTENNA, List.of(
                num("freq", "Center frequency (MHz)", 14.175),
                sel("elements", "Total elements (incl. DE + reflector)", "3", "2", "2 (DE + reflector)", "3", "3 (DE + R + 1 director)",
                        "4", "4 (DE + R + 2 directors)", "5", "5 (DE + R + 3 directors)", "6", "6 (DE + R + 4 directors)")),
            v -> {
                double f = pfd(v, "freq", 14.175);
                int n = pi(v, "elements", 3);
                double lambdaFt = 984 / f;
                double deLen = (468 / f) * 0.97, reflLen = deLen * 1.05;
                double[] dirShort = {0.95, 0.93, 0.92, 0.91};
                List<String[]> dirs = new ArrayList<>();
                for (int i = 0; i < n - 2; i++) {
                    double len = deLen * dirShort[Math.min(i, dirShort.length - 1)];
                    dirs.add(r("Director " + (i + 1) + " length", fx(len, 2) + " ft  (" + fx(len * 12, 1) + " in)"));
                }
                double spaceRefl = lambdaFt * 0.18, spaceD1 = lambdaFt * 0.12, spaceDir = lambdaFt * 0.20;
                List<String[]> sp = new ArrayList<>();
                sp.add(r("Reflector → DE spacing", fx(spaceRefl, 2) + " ft"));
                if (n >= 3) sp.add(r("DE → Director 1 spacing", fx(spaceD1, 2) + " ft"));
                for (int i = 2; i < n - 1; i++) sp.add(r("Director " + (i - 1) + " → Director " + i + " spacing", fx(spaceDir, 2) + " ft"));
                double boom = spaceRefl + (n >= 3 ? spaceD1 : 0) + (n - 3) * spaceDir;
                Map<Integer, Double> gain = Map.of(2, 5.5, 3, 7.5, 4, 9.0, 5, 10.5, 6, 11.5);
                Map<Integer, Integer> fb = Map.of(2, 12, 3, 20, 4, 22, 5, 25, 6, 25);
                double g = gain.getOrDefault(n, 12.0);
                List<String[]> out = new ArrayList<>();
                row(out, "Reflector length", fx(reflLen, 2) + " ft  (" + fx(reflLen * 12, 1) + " in)");
                row(out, "Driven element length", fx(deLen, 2) + " ft  (" + fx(deLen * 12, 1) + " in)");
                out.addAll(dirs);
                row(out, "Boom length (total)", fx(Math.max(0, boom), 2) + " ft");
                out.addAll(sp);
                row(out, "Expected forward gain", "~" + num(g) + " dBi  (~" + fx(g - 2.15, 1) + " dBd)");
                row(out, "Expected F/B ratio", "~" + fb.getOrDefault(n, 25) + " dB");
                row(out, "Expected feed Z", "~25-35 Ω (gamma match or hairpin to 50 Ω)");
                row(out, "Match", "Gamma match, beta/hairpin match, or 4:1 transformer to 50 Ω coax");
                return out;
            },
            """
                refl  DE     D1   D2   D3
                 │    │      │    │    │
                 │    │      │    │    │
                 │    │      │    │    │     →  forward direction (toward DX)
                 │    │      │    │    │
                 │    │      │    │    │
                 ●────●──────●────●────●  boom

                     feed
                      │
                    coax to rig

              Reflector is the longest element; directors get progressively shorter.
              Driven element is shortest of the three classic elements.""",
            "Element lengths are empirical — verify with NEC-2 / 4nec2 modeling for production builds.",
            "Spacing affects gain, F/B, and feed Z — close-spaced gives narrow band but slightly more gain.",
            "Driven-element length given includes ~3% shortening typical with a gamma match. Adjust for your match type.",
            "Boom must be insulated from elements (or all-aluminum with elements through the boom — both OK).",
            "Yagis typically need a balun (4:1 hairpin or matching transformer) to bring 25-35 Ω feed Z to 50 Ω.");

        add("mag-loop", "Magnetic Loop", "09-14", Kind.ANTENNA, List.of(
                num("freq", "Operating frequency (MHz)", 14.175),
                num("diam", "Loop diameter (ft)", 3.0, "Larger loop = higher efficiency but lower Q. 0.05λ - 0.25λ is typical (50% range)"),
                num("cond", "Conductor diameter (in)", 0.5, "½\" copper pipe is typical. Larger pipe = lower loss = higher efficiency."),
                num("power", "Transmit power (W)", 25)),
            v -> {
                double f = pfd(v, "freq", 14.175), dFt = pfd(v, "diam", 3), condIn = pfd(v, "cond", 0.5), power = pfd(v, "power", 25);
                double dM = dFt * 0.3048, condM = condIn * 0.0254;
                double circum = Math.PI * dM;
                double a = condM / 2, b = dM / 2;
                double indH = 4e-7 * Math.PI * b * (Math.log(8 * b / a) - 2);
                double reqPF = 1 / (4 * Math.PI * Math.PI * Math.pow(f * 1e6, 2) * indH) * 1e12;
                double xl = 2 * Math.PI * f * 1e6 * indH;
                double skinR = 0.0826 * Math.sqrt(f) / (condIn / 0.039);
                double rLoss = skinR * (circum / 0.3048);
                double q = Math.min(800, xl / Math.max(rLoss, 0.01));
                double vcap = Math.sqrt(power * q * xl);
                double bw = (f * 1000) / q;
                double cFrac = circum / (300 / f);
                String eff = cFrac < 0.1 ? "poor" : cFrac < 0.2 ? "good" : cFrac < 0.3 ? "excellent" : "over-size";
                return rows(
                    r("Loop circumference", fx(circum, 2) + " m  (" + fx(circum / 0.3048, 2) + " ft)"),
                    r("Circumference / wavelength", fx(cFrac, 3) + "  (" + eff + ")"),
                    r("Loop inductance", fx(indH * 1e6, 2) + " µH"),
                    r("Tuning capacitance needed", fx(reqPF, 1) + " pF"),
                    r("Estimated Q", fx(q, 0)),
                    r("3-dB bandwidth", fx(bw, 1) + " kHz"),
                    r("Inductive reactance X_L", fx(xl, 0) + " Ω"),
                    r("Voltage across capacitor", fx(vcap, 0) + " V peak  (at " + num(power) + " W)"),
                    r("Capacitor voltage rating", "≥ " + fx(vcap * 1.5, 0) + " V (safety margin) → vacuum cap recommended"));
            },
            """
                     ╭─────────────╮
                    ╱               ╲
                   ╱                 ╲
                  │                   │
                  │                   │
                  │     [tuning C]    │  ← high-V variable capacitor (vacuum)
                  │      ┌───┐        │
                   ╲    ─┤   ├─      ╱
                    ╲   ─┤   ├─     ╱
                     ╰───┴───┴─────╯
                          ↑
                     coupling loop
                     (~⅕ of main loop dia.)
                          │
                        coax to rig

              Tune by varying the capacitor — each band needs a different C value.
              Coupling loop adjusts feed Z to ~50 Ω.""",
            "Mag loops are extremely narrow-banded — Q of 200-500 means 3-dB BW around 30-70 kHz on 20m.",
            "Voltages across the tuning cap exceed several kV at moderate power — vacuum capacitor required for >50 W.",
            "Efficiency drops sharply below 0.1λ circumference; aim for 0.15-0.25λ for best results.",
            "Coupling loop ~⅕ the main-loop diameter is a good starting point; trim for SWR.",
            "Loop should be 6-10 ft above ground for cleanest pattern.");

        // ===================================================================================== COMPONENTS
        add("trap-design", "Trap Design", "09-13", Kind.COMPONENT, List.of(
                num("freq", "Trap resonant frequency (MHz)", 14.150, "Set this to the highest band the trap should isolate (e.g. 14 MHz for an 80/40/20 trap dipole)."),
                num("capPF", "Capacitance choice (pF)", 100, "Pick a doorknob or vacuum cap on hand. 50-200 pF is typical. Smaller C → larger L (more turns)."),
                num("power", "Operating power (W PEP)", 100),
                num("formIn", "Coil form diameter (in)", 1.0, "Clear PVC pipe is common. 1\" - 2\" diameter, close-wound."),
                sel("wireGauge", "Wire gauge (AWG)", "14", "12", "#12 (heavy, low loss)", "14", "#14 (standard)", "16", "#16 (lighter)")),
            v -> {
                double f = pfd(v, "freq", 14.150), capPF = pfd(v, "capPF", 100), power = pfd(v, "power", 100), formIn = pfd(v, "formIn", 1.0);
                int awg = pi(v, "wireGauge", 14);
                double indH = 1 / (4 * Math.PI * Math.PI * Math.pow(f * 1e6, 2) * capPF * 1e-12);
                double indUH = indH * 1e6;
                double xl = 2 * Math.PI * f * 1e6 * indH;
                double trapQ = 100;
                double vcap = Math.sqrt(power * trapQ * xl);
                double rad = formIn / 2;
                double wireDia = awg == 12 ? 0.0808 : awg == 14 ? 0.0641 : 0.0508;
                double turns = Math.ceil(wheelerTurns(indUH, rad, wireDia, 5) * 10) / 10;
                double coilLen = turns * wireDia;
                double wireFt = (turns * Math.PI * formIn) / 12;
                return rows(
                    r("Required inductance", fx(indUH, 2) + " µH"),
                    r("Reactance at resonance (X_L = X_C)", fx(xl, 0) + " Ω"),
                    r("Capacitor voltage rating needed", "≥ " + fx(vcap * 1.5, 0) + " V peak  (at " + num(power) + " W PEP, Q≈" + num(trapQ) + ")"),
                    r("Capacitor type recommendation", vcap > 2000 ? "Vacuum or doorknob ceramic (5 kV+)" : vcap > 500 ? "Doorknob ceramic (3 kV+)" : "Mica or ceramic disc (1 kV)"),
                    r("Coil turns", fx(turns, 1) + " turns of #" + awg + " on " + num(formIn) + "\" form"),
                    r("Coil length", fx(coilLen, 2) + " in close-wound"),
                    r("Wire length", fx(wireFt, 1) + " ft  (plus 6\" leads)"),
                    r("Trap Q (typical)", "~" + num(trapQ)));
            },
            """
                      C (capacitor)
                        ┌─┤├─┐
                        │    │
                        │    │
                        │    │
               wire ────┴────┴──── wire
                            │
                          L (coil)

               Schematic — at resonance (f₀ = 1/(2π√LC)) the trap is a high-Z open.
               Above f₀, looks capacitive (X_C dominates).
               Below f₀, looks inductive (X_L dominates).""",
            "Pick a capacitor first (high voltage rating matters more than precision); compute coil to match.",
            "For legal-limit operation, capacitor voltage can exceed 5 kV — use vacuum capacitors.",
            "Wind the coil close-wound (turns touching) for predictable inductance from the formula.",
            "Trap loss (typically 0.3-1 dB per trap) reduces antenna efficiency — count traps in your loss budget.",
            "Verify trap resonance with a NanoVNA or dip meter BEFORE installing — much easier on the bench.");

        add("loading-coil", "Loading Coil", "09-12", Kind.COMPONENT, List.of(
                num("freq", "Operating frequency (MHz)", 7.150),
                num("physLen", "Antenna physical length (ft)", 33, "Length of the (shortened) antenna. For a dipole, this is total tip-to-tip; for a vertical, full length."),
                sel("antType", "Antenna type", "dipole", "dipole", "Dipole (half-wave)", "vertical", "Vertical (quarter-wave)"),
                sel("position", "Coil position", "center", "base", "Base loaded (lossiest, easiest)", "center", "Center loaded (best efficiency)",
                        "top", "Top loaded (highest efficiency, hardest physically)")),
            v -> {
                double f = pfd(v, "freq", 7.150), phys = pfd(v, "physLen", 33);
                boolean dip = "dipole".equals(v.get("antType"));
                double full = dip ? 468 / f : 234 / f;
                double frac = phys / full;
                if (frac >= 0.97) return rows(r("Result", "Antenna is " + fx(frac * 100, 0) + "% of full length — no significant loading needed."));
                double z0 = dip ? 50 : 36;
                double xRatio = Math.tan((Math.PI / 2) * (1 - frac));
                double pos = 1.0; String eff = "good", loss = "~1 dB";
                String position = str(v, "position");
                if (position.equals("base")) { pos = 3.0; eff = "poor"; loss = "~3-5 dB"; }
                if (position.equals("top")) { pos = 0.5; eff = "excellent"; loss = "<0.5 dB"; }
                double xReq = z0 * xRatio * pos;
                double indUH = xReq / (2 * Math.PI * f * 1e6) * 1e6;
                double perSide = dip ? indUH / 2 : indUH;
                double wireDia = 0.0641;
                double turns = Math.ceil(wheelerTurns(perSide, 1.0, wireDia, 10) * 10) / 10;
                double coilLen = turns * wireDia;
                return rows(
                    r("Full-size length (no loading)", fx(full, 1) + " ft"),
                    r("Physical length (input)", fx(phys, 1) + " ft  (" + fx(frac * 100, 0) + "%)"),
                    r("Reactance to add", fx(xReq, 0) + " Ω total"),
                    r("Total loading inductance", fx(indUH, 2) + " µH"),
                    r("Inductance per side", fx(perSide, 2) + " µH " + (dip ? "(× 2 — one in each leg)" : "(single coil)")),
                    r("Suggested coil", fx(turns, 1) + " turns of #14 on 2\" form, close-wound (~" + fx(coilLen, 2) + "\" long)"),
                    r("Position", position + " loaded — efficiency: " + eff + ", est. loss: " + loss),
                    r("Practical Q", position.equals("base") ? "50-100 (lossy in mobile mounts)" : "150-300 (air-core)"));
            },
            """
              Center-loaded vertical:                    Base-loaded vertical:

                   │                                    │
                   │  ← upper element                   │  ← full element
                   │                                    │
                   ●  ← loading coil                    │
                   │                                    │
                   │  ← lower element                   ●  ← loading coil at base
                   │                                    │
               ────●────────                         ───●────────
               ground                                 ground

              Center loading is more efficient (more current flows in the
              full upper section before the loading coil reduces it).
              Base loading is easier mechanically but lossier.""",
            "Loading restores resonance by adding reactance equal to what was lost from shortening.",
            "Position trade-off: base = simple but lossy; center = best efficiency; top = best efficiency, mechanical challenge.",
            "For a dipole, put a coil in EACH leg (half the inductance per side, both sides).",
            "Loading coil Q matters — air-core ~150-300 vs. iron core ~50-100. Higher Q = less loss.",
            "Bandwidth narrows in proportion to shortening — a 50% shortened antenna has ~1/4 the bandwidth.");

        formulas();
    }

    /** Turns for a single-layer air-core coil (Wheeler): L(µH) = r²N² / (9r + 10ℓ), ℓ = N × wire diameter — iterated. */
    private static double wheelerTurns(double uH, double radiusIn, double wireDia, double start) {
        double n = start, prev = 0;
        for (int i = 0; i < 30 && Math.abs(n - prev) > 0.01; i++) {
            prev = n;
            n = Math.sqrt(uH * (9 * radiusIn + 10 * n * wireDia) / (radiusIn * radiusIn));
        }
        return n;
    }

    // ===================================================================================== TRAPPED DESIGNS
    private static List<In> trappedInputs(String lenLabel, String lenHint, boolean efhw) {
        List<In> l = new ArrayList<>(List.of(
                sel("nbands", "Number of bands", "3", "2", "2", "3", "3", "4", "4"),
                num("b1", "Band 1 — highest (MHz)", 14.150), num("b2", "Band 2 (MHz)", 7.150),
                num("b3", "Band 3 (MHz)", 3.700), num("b4", "Band 4 (MHz, if 4-band)", 1.900),
                num("L_avail", lenLabel, 0, lenHint),
                sel("cap_pf", "Trap capacitor (pF)", "100", "50", "50", "75", "75", "100", "100", "150", "150", "220", "220", "330", "330"),
                num("power", "Power (W PEP)", 100)));
        if (efhw) l.add(sel("unun", "Unun ratio", "49", "49", "49:1", "64", "64:1"));
        return l;
    }

    private record Trap(double lUH, double cPF, double vPeak, double xAtFi, double fr, double fi) {}

    /** Length-driven trapped dipole (efhw=false) / trapped EFHW (efhw=true) design — nine labelled sections. */
    private static List<String[]> trapped(Map<String, String> v, boolean efhw) {
        int n = pi(v, "nbands", 3);
        List<Double> fl = new ArrayList<>();
        for (int i = 1; i <= n; i++) { double fi = pf(v, "b" + i); if (!Double.isNaN(fi) && fi > 0) fl.add(fi); }
        if (fl.size() != n) return rows(r("Error", "Need " + n + " valid band frequencies (got " + fl.size() + ")."));
        fl.sort(Comparator.reverseOrder());
        double[] f = fl.stream().mapToDouble(Double::doubleValue).toArray();
        double lAvail = pfd(v, "L_avail", 0), C = pfd(v, "cap_pf", 100), P = pfd(v, "power", 100);
        final double z0 = 600, qTrap = 100, qCoil = 200, vMul = efhw ? 2.5 : 1.0;
        String unun = efhw ? str(v, "unun") : "";

        // full-size segment lengths
        double[] segFull = new double[n];
        double cum = 0;
        for (int i = 0; i < n; i++) {
            double fullForBand = efhw ? 478 / f[i] : (468 / f[i]) / 2;
            segFull[i] = fullForBand - cum;
            cum = fullForBand;
        }
        double fullTotal = efhw ? cum : 2 * cum;

        double s = 1.0; boolean shortened = false;
        if (lAvail > 0 && lAvail < fullTotal) { s = lAvail / fullTotal; shortened = true; }
        double[] seg = new double[n];
        for (int i = 0; i < n; i++) seg[i] = segFull[i] * s;

        // traps
        List<Trap> traps = new ArrayList<>();
        double[] xTot = new double[n];
        for (int i = 1; i < n; i++) {
            double lambdaB = 984 / f[i], dSeg = segFull[i] - seg[i], fr = f[i - 1];
            double lStd = 1 / (4 * Math.PI * Math.PI * Math.pow(fr * 1e6, 2) * C * 1e-12);
            double xStd = 2 * Math.PI * (f[i] * 1e6) * lStd / (1 - Math.pow(f[i] / fr, 2));
            double xt, lh, cp;
            if (shortened && dSeg > 0.05) {
                double xExtra = z0 * Math.tan(2 * Math.PI * dSeg / lambdaB);
                xt = xStd + Math.max(0, xExtra);
                double frHz = fr * 1e6, fa = f[i] * 1e6;
                lh = xt * (1 - Math.pow(fa / frHz, 2)) / (2 * Math.PI * fa);
                cp = 1 / (4 * Math.PI * Math.PI * frHz * frHz * lh) * 1e12;
            } else {
                xt = xStd;
                lh = lStd; cp = C;
            }
            double xlr = 2 * Math.PI * (fr * 1e6) * lh;
            traps.add(new Trap(lh * 1e6, cp, Math.sqrt(P * qTrap * xlr) * vMul, xt, fr, f[i]));
            xTot[i] = xt;
        }

        // inner (dipole) / seg-1 (EFHW) coil
        double coilUH = 0;
        double coilThresh = efhw ? Math.max(0.05, segFull[0] * 0.10) : 0.05;
        if (shortened && segFull[0] - seg[0] > coilThresh) {
            double x = z0 * Math.tan(2 * Math.PI * (segFull[0] - seg[0]) / (984 / f[0]));
            coilUH = x / (2 * Math.PI * (f[0] * 1e6)) * 1e6;
            xTot[0] = x;
        } else xTot[0] = efhw ? 50 : 30;

        // radiation resistance + bandwidth
        double[] rr = new double[n], bw = new double[n];
        double base = efhw ? 36 : 73;
        for (int i = 0; i < n; i++) {
            double fullAt = efhw ? 478 / f[i] : (468 / f[i]) / 2;
            double built = 0; for (int j = 0; j <= i; j++) built += seg[j];
            double ratio = Math.min(1, built / fullAt);
            rr[i] = Math.max(2, base * ratio * ratio);
        }
        for (int i = 0; i < n; i++) {
            double x = Math.max(10, xTot[i]);
            double qe = (i == 0 && coilUH > 0.1) ? qCoil : qTrap;
            double bwHz = (f[i] * 1e6) * (rr[i] + x / qe) / x;
            bw[i] = efhw ? bwHz / 1000 : Math.min(bwHz / 1000, (f[i] * 1000) / 15);
        }
        double totalPerSide = 0; for (double x : seg) totalPerSide += x;

        List<String[]> out = new ArrayList<>();
        String kind = efhw ? "EFHW" : "dipole";
        // 1. Overview
        row(out, "── 1. Antenna Overview ──", "");
        row(out, "Design type", (shortened ? "Shortened trapped " : "Full-size trapped ") + (efhw ? "EFHW" : "dipole"));
        StringBuilder bl = new StringBuilder();
        for (int i = 0; i < n; i++) bl.append(i > 0 ? ", " : "").append(fx(f[i], 3)).append(" MHz");
        row(out, "Bands", n + " bands: " + bl);
        if (shortened) {
            row(out, "Why shortened", "Available " + (efhw ? "wire " : "") + "length " + fx(lAvail, 1) + " ft is less than the full-size requirement of " + fx(fullTotal, 1) + " ft. "
                    + (efhw ? "Applying uniform shortening s = " + fx(s * 100, 1) + "% to all segments and re-specing traps to provide extra inductive loading."
                            : "Applying uniform shortening factor s = " + fx(s * 100, 1) + "% to all segments and re-specing traps to provide the missing inductive loading."));
        } else if (lAvail > 0) {
            row(out, "Note", "Available length " + fx(lAvail, 1) + " ft " + (efhw ? "≥ full-size requirement " : "meets/exceeds full-size requirement ") + fx(fullTotal, 1) + " ft; building full size.");
        } else row(out, "Note", "No length constraint specified — designing at full size.");

        // 2. Segment plan
        row(out, "", ""); row(out, "── 2. Segment Length Plan ──", "");
        row(out, efhw ? "BUILT total wire length" : "BUILT tip-to-tip",
                efhw ? fx(totalPerSide, 2) + " ft" : fx(2 * totalPerSide, 2) + " ft  (" + fx(totalPerSide, 2) + " ft per side)");
        row(out, "Shortening applied", shortened ? "s = " + fx(s * 100, 1) + "% of full-size  (full-size would be " + fx(fullTotal, 2) + " ft)"
                : efhw ? "none — full-size design" : "none — full-size design (no length constraint, or constraint exceeds full-size)");
        row(out, efhw ? "Seg 1 (unun → trap 1, λ/2 at " + fx(f[0], 2) + " MHz)" : "Inner segment, each side (λ/2 at " + fx(f[0], 2) + " MHz)",
                fx(seg[0], 2) + " ft" + (shortened && segFull[0] - seg[0] > 0.05 ? "  (ideal " + fx(segFull[0], 2) + " ft  ← shortened)" : "  (full-size)"));
        for (int i = 1; i < n; i++) {
            String lab;
            if (efhw) lab = "Seg " + (i + 1) + " (trap " + i + " → " + (i == n - 1 ? "end" : "trap " + (i + 1)) + ", extends to " + fx(f[i], 2) + " MHz)";
            else lab = "Outer " + i + ", each side (extends to " + fx(f[i], 2) + " MHz)";
            row(out, lab, fx(seg[i], 2) + " ft" + (shortened && segFull[i] - seg[i] > 0.05 ? "  (ideal " + fx(segFull[i], 2) + " ft  ← shortened)" : "  (full-size)"));
        }

        // 3. Loading components
        row(out, "", ""); row(out, "── 3. Loading Components ──", "");
        if (!shortened) row(out, "Loading needed", efhw ? "None beyond natural trap inductance below resonance."
                : "None beyond the natural inductive loading provided by the standard traps below their resonant frequency.");
        for (int i = 0; i < traps.size(); i++) {
            Trap t = traps.get(i);
            row(out, "Trap " + (i + 1) + " — resonant at " + fx(t.fr(), 2) + " MHz", "L = " + fx(t.lUH(), 2) + " µH  ·  C = " + fx(t.cPF(), 0) + " pF");
            row(out, "  Effect", efhw
                    ? "Open-Z at " + fx(t.fr(), 2) + " MHz;  ~" + fx(t.xAtFi(), 0) + " Ω inductive at " + fx(t.fi(), 2) + " MHz (loads the shortened next segment)."
                    : "Open-Z (isolates " + fx(t.fr(), 2) + " MHz);  ~" + fx(t.xAtFi(), 0) + " Ω inductive at " + fx(t.fi(), 2) + " MHz (loads the shortened outer segment).");
            row(out, "  Cap voltage @ " + num(P) + " W" + (efhw ? " (EFHW × 2.5)" : ""), fx(t.vPeak(), 0) + " V peak  →  " + capClass(t.vPeak(), efhw));
        }
        if (coilUH > 0.1) {
            row(out, efhw ? "Seg-1 inline loading coil" : "Center loading coil per leg",
                    efhw ? fx(coilUH, 2) + " µH placed ~⅓ to ½ along seg 1 — replaces missing length on " + fx(f[0], 2) + " MHz."
                         : fx(coilUH, 2) + " µH (one in each leg at the feedpoint) — replaces missing inner-segment length on " + fx(f[0], 2) + " MHz.");
        }
        boolean hats = shortened && s < 0.55;
        row(out, "Capacitive hats", efhw
                ? (hats ? "Recommended at the wire end to broaden lowest-band BW and reduce required trap reactance." : "Optional; can be added at the wire end to slightly broaden bandwidth.")
                : (hats ? "Recommended at wire tips to reduce required coil/trap reactance and lift radiation resistance. Each hat adds the equivalent of ~5–10% of physical length per side."
                        : "Not required for this design; can be added at tips to slightly broaden bandwidth."));

        // 4. Placement
        row(out, "", ""); row(out, "── 4. Trap / Coil Placement ──", "");
        row(out, "Rule", efhw ? "Higher-frequency trap closest to the unun; each lower-frequency trap farther out (single wire, one trap per band boundary)."
                : "Higher-frequency trap closest to feed; each lower-frequency trap farther out. Traps mirror across the feedpoint (one set per leg).");
        StringBuilder lay = new StringBuilder();
        if (efhw) {
            lay.append("unun ⇒ ").append(fx(seg[0], 1)).append(" ft");
            if (coilUH > 0.1) lay.append(" (incl. ").append(fx(coilUH, 1)).append(" µH inline coil)");
            for (int i = 1; i < n; i++) lay.append("  → [Trap ").append(i).append(": ").append(fx(f[i - 1], 2)).append(" MHz]  → ")
                    .append(fx(seg[i], 1)).append(" ft").append(i == n - 1 ? " ⇒ end" : "");
        } else {
            lay.append("feed");
            if (coilUH > 0.1) lay.append(" → [").append(fx(coilUH, 1)).append(" µH coil]");
            lay.append(" → ").append(fx(seg[0], 1)).append(" ft wire");
            for (int i = 1; i < n; i++) lay.append("  → [Trap ").append(i).append(": ").append(fx(f[i - 1], 2)).append(" MHz]  → ").append(fx(seg[i], 1)).append(" ft wire");
        }
        row(out, efhw ? "Layout (unun → end)" : "Per-side layout (feed → tip)", lay.toString());

        // 5. Feedpoint
        row(out, "", ""); row(out, "── 5. Feedpoint Impedance & Matching ──", "");
        for (int i = 0; i < n; i++) {
            if (efhw) {
                double zEnd = rr[i] * (unun.equals("49") ? 49 : 64);
                row(out, "Estimated end Z on " + fx(f[i], 2) + " MHz", "~" + fx(rr[i], 0) + " Ω at the input of the unun (full = 36 Ω);  ~" + fx(zEnd, 0) + " Ω at unun output");
            } else row(out, "Estimated Z on " + fx(f[i], 2) + " MHz", "~" + fx(rr[i], 0) + " Ω (full-size dipole = 73 Ω; loaded antennas drop with shortening)");
        }
        if (efhw) {
            row(out, "Recommended match", unun + ":1 unun (primary 2 turns, secondary " + (unun.equals("49") ? "14" : "16") + " turns on FT240-43 toroid). "
                    + (shortened ? "Add a 1:1 common-mode choke at the rig end of the coax; tuner recommended on shortened designs." : "A 1:1 common-mode choke at the rig end is essential."));
            row(out, "Counterpoise", "~" + fx(segFull[0] * 0.05, 1) + " ft (5% of full-size seg 1) — connect to unun ground side");
        } else row(out, "Recommended match", shortened ? "50 Ω coax + 1:1 current balun. Use an antenna tuner at the rig for SWR > 2:1 outside band centers."
                : "50 Ω coax + 1:1 current balun directly. Tuner needed only at band edges.");

        // 6. SWR
        row(out, "", ""); row(out, "── 6. Expected SWR Behavior ──", "");
        String curve;
        if (shortened && s < 0.55) curve = efhw ? "Very sharp V-shaped minima — narrow operating window, steep edges, may require tuner for full band coverage."
                : "Very sharp V-shaped minima at each band center — narrow tuning window, steep edges.";
        else if (shortened && s < 0.75) curve = efhw ? "Sharper minima than full-size; usable < 2:1 over ~50–70% of each band center."
                : "Pronounced V-shaped minima; SWR < 2:1 over a smaller fraction of each band than a full-size trapped dipole.";
        else if (shortened) curve = efhw ? "Mild narrowing relative to full-size trapped EFHW; SWR < 2:1 across most of each band."
                : "Standard trapped-dipole V shape, slightly steeper sides than full size.";
        else curve = efhw ? "Standard trapped EFHW: SWR ≈ 1.5–2:1 across each band, broader than a trapped dipole."
                : "Typical trapped-dipole behavior: SWR ≈ 1.5:1 at center, rising to ~2:1 at band edges.";
        row(out, "Curve shape", curve);
        row(out, "Narrowest band", fx(f[n - 1], 2) + " MHz (lowest band) — " + (efhw ? "highest loading reactance and lowest Rr" : "highest loading reactance = sharpest tuning"));

        // 7. Bandwidth
        row(out, "", ""); row(out, "── 7. Expected 2:1 SWR Bandwidth ──", "");
        row(out, "Formula used", "BW ≈ f · (Rr + X_load/Q) / X_load (loaded-antenna approximation; Q ≈ 100 " + (efhw ? "traps, 200 air-core coils)" : "for traps, 200 for air-core coils)"));
        for (int i = 0; i < n; i++) {
            double x = Math.max(10, xTot[i]);
            String tag = (i == 0 && coilUH > 0.1) ? "coil-loaded" : (i == 0) ? "unloaded" : "trap-loaded";
            row(out, fx(f[i], 2) + " MHz  (" + tag + ")", "BW ≈ " + (bw[i] < 10 ? fx(bw[i], 1) : fx(bw[i], 0)) + " kHz    [Rr ≈ " + fx(rr[i], 0) + " Ω,  X_load ≈ " + fx(x, 0) + " Ω]");
        }
        for (int i = 0; i < n; i++) {
            if (bw[i] < 20 && f[i] < 5) row(out, "  ⚠ " + fx(f[i], 2) + " MHz BW",
                    (efhw ? "extremely narrow — practical only within ±" : "extremely narrow — operating practical only within ±") + fx(bw[i] / 2, 1) + " kHz of design frequency.");
        }

        // 8. Performance
        row(out, "", ""); row(out, "── 8. Performance & Efficiency Opinion ──", "");
        String verdict;
        if (!shortened) verdict = efhw ? "Full-size trapped EFHW. Solid performer; expect ~0.5–1.5 dB total trap loss across bands. Excellent for portable / fixed multi-band."
                : "Full-size trapped dipole. Expect ~0.5–1.0 dB total trap loss across bands; otherwise a strong performer.";
        else if (s >= 0.85) verdict = efhw ? "Mild shortening (≤15%). Practical, near-full performance. Reasonable trap voltages."
                : "Mild shortening (≤15%). Practical, near-full performance. Slight bandwidth narrowing; trap voltage manageable.";
        else if (s >= 0.70) verdict = efhw ? "Moderate shortening (15–30%). Practical for SOTA/POTA — wire bag drops by 1/3. Plan on doorknob+ caps for the innermost trap."
                : "Moderate shortening (15–30%). Real-world good — popular space-saving QTH antenna. Plan on doorknob caps and noticeably narrower lowest-band BW.";
        else if (s >= 0.55) verdict = efhw ? "Heavy shortening (30–45%). Marginal: efficiency suffers (~1.5–3 dB on lowest band), tight BW, vacuum caps likely required at full power."
                : "Heavy shortening (30–45%). Marginal: efficiency suffers (~1.5–2.5 dB), bandwidth tight. Doorknob or vacuum caps required.";
        else if (s >= 0.40) verdict = efhw ? "Extreme shortening (45–60%). Poor — very narrow BW, vacuum caps required, traps approach physical limits."
                : "Extreme shortening (45–60%). Poor efficiency, very narrow BW. Build only if no alternative; expect to retune frequently within bands.";
        else verdict = efhw ? "Below 40% — not viable as an EFHW. Consider a different topology (vertical, loop, etc.)."
                : "Below 40% — not recommended as a trapped dipole; the loading reactance is so dominant that radiation resistance becomes < 5 Ω. Pick a different topology.";
        row(out, "Verdict", verdict);
        String plural = traps.size() > 1 ? "s" : "";
        if (!efhw) row(out, "Radiation resistance trend", "Rr drops from " + fx(rr[0], 0) + " Ω on " + fx(f[0], 2) + " MHz to " + fx(rr[n - 1], 0) + " Ω on " + fx(f[n - 1], 2) + " MHz.");
        row(out, "Trap insertion loss", traps.size() + " trap" + plural + (efhw ? " × ~0.5–1.5 dB each on the bands below their resonance. Cumulative on lowest band: ~" + fx(traps.size() * 1.0, 1) + " dB."
                : " × ~0.5–1 dB each on the bands below their resonance. Cumulative on the lowest band: ~" + fx(traps.size() * 0.75, 1) + " dB."));
        if (efhw) row(out, "Voltage warning", "EFHW traps see ~2.5× the voltage of dipole traps at the same power. Plan capacitor type accordingly.");

        // 9. Warnings
        row(out, "", ""); row(out, "── 9. Warnings & Practical Notes ──", "");
        List<String> w = new ArrayList<>();
        if (efhw) {
            if (lAvail > 0 && lAvail < segFull[0] * 0.4) w.add("⚠ Available length " + fx(lAvail, 1) + " ft is well below the highest-band ideal " + fx(segFull[0], 1) + " ft — design infeasible as an EFHW.");
        } else if (lAvail > 0 && lAvail < segFull[0] * 2 * 0.4) w.add("⚠ Available length " + fx(lAvail, 1) + " ft is well below the highest-band ideal " + fx(segFull[0] * 2, 1) + " ft — design infeasible as a dipole.");
        if (shortened && s < 0.5) {
            if (efhw) {
                w.add("⚠ At s = " + fx(s * 100, 0) + "% the wire is severely shortened. Alternatives:");
                w.add("   • Half-size loaded vertical with elevated radials");
                w.add("   • Magnetic loop antenna (very narrow BW but efficient)");
                w.add("   • Drop the lowest band; design for higher bands at fuller size");
                w.add("   • Add capacitive end-hat to reduce required trap reactance");
            } else {
                w.add("⚠ At s = " + fx(s * 100, 0) + "% the antenna is severely shortened. Alternatives to consider:");
                w.add("   • Loaded vertical with elevated radials (uses ~30 ft of vertical real estate, similar efficiency)");
                w.add("   • Magnetic loop antenna (5–10 ft diameter, very narrow BW but efficient on " + fx(f[n - 1], 1) + " MHz)");
                w.add("   • Drop the lowest band (" + fx(f[n - 1], 2) + " MHz) and build a fuller-size design for the higher bands");
                w.add("   • Add capacitive hats at the tips to reduce required loading reactance");
            }
        }
        for (int i = 0; i < traps.size(); i++) {
            double vp = traps.get(i).vPeak();
            if (efhw) { if (vp > 3000 && P < 500) w.add("⚠ Trap " + (i + 1) + " needs vacuum capacitor at " + num(P) + " W (EFHW voltage is severe)"); }
            else if (vp > 2000 && P < 500) w.add("⚠ Trap " + (i + 1) + " needs doorknob/vacuum caps even at " + num(P) + " W power level");
        }
        if (bw[n - 1] < 20 && f[n - 1] < 5) w.add(efhw ? "⚠ Lowest-band BW < 20 kHz — practical operation limited to a narrow window."
                : "⚠ Lowest-band BW < 20 kHz — you may not cover the whole CW or phone segment without retuning.");
        if (w.isEmpty()) w.add(efhw ? "No specific warnings — design is within practical limits." : "No specific warnings — sound design within practical limits.");
        for (String x : w) row(out, "", x);
        return out;
    }

    private static String capClass(double vp, boolean efhw) {
        if (efhw) {
            if (vp < 1000) return "silvered mica 1 kV";
            if (vp < 3000) return "doorknob ceramic 3 kV";
            if (vp < 7000) return "vacuum capacitor 5–10 kV";
            return "vacuum capacitor 10 kV+";
        }
        if (vp < 700) return "silvered mica / NPO ceramic";
        if (vp < 2000) return "doorknob ceramic 1–3 kV";
        if (vp < 6000) return "vacuum capacitor 5 kV+";
        return "vacuum capacitor 10 kV+";
    }

    // ===================================================================================== FORMULAS
    /** Γ = (Z − Z₀)/(Z + Z₀) for Z = R + jX → {re, im, |Γ|, ∠Γ°}. */
    private static double[] gamma(double R, double X, double Z0) {
        double nr = R - Z0, ni = X, dr = R + Z0, di = X, d2 = dr * dr + di * di;
        double gr = (nr * dr + ni * di) / d2, gi = (ni * dr - nr * di) / d2;
        return new double[]{gr, gi, Math.sqrt(gr * gr + gi * gi), Math.atan2(gi, gr) * 180 / Math.PI};
    }
    private static double swrOf(double g) { return g >= 1 ? Double.POSITIVE_INFINITY : (1 + g) / (1 - g); }
    private static boolean fin(double d) { return !Double.isInfinite(d) && !Double.isNaN(d); }

    private static void formulas() {
        add("ohms-law", "Ohm's Law", "17-01", Kind.FORMULA, List.of(
                sel("mode", "Solve for", "V", "V", "Voltage (given I, R)", "I", "Current (given V, R)", "R", "Resistance (given V, I)"),
                num("V", "Voltage V (volts)", 13.8), num("I", "Current I (amps)", 22), num("R", "Resistance R (ohms)", 0.016)),
            v -> {
                double V = pf(v, "V"), I = pf(v, "I"), R = pf(v, "R");
                String m = str(v, "mode");
                double res; String formula;
                if (m.equals("V")) { res = I * R; formula = "V = I × R"; }
                else if (m.equals("I")) { res = V / R; formula = "I = V / R"; }
                else { res = V / I; formula = "R = V / I"; }
                return rows(r("Formula", formula),
                    r("Result", m.equals("V") ? "V = " + fx(res, 3) + " volts" : m.equals("I") ? "I = " + fx(res, 3) + " amps" : "R = " + fx(res, 3) + " ohms"),
                    r("Power (V × I)", fx(V * I, 2) + " W  (using current input values)"),
                    r("Power (I²R)", fx(I * I * R, 2) + " W"),
                    r("Power (V²/R)", fx(V * V / R, 2) + " W"));
            }, null,
            "Use this with all three forms: V=IR, I=V/R, R=V/I. Power forms (P=VI=I²R=V²/R) are shown as cross-checks.",
            "Voltage drop in mobile DC cables is the classic ham use — pick I and R, solve for V_drop.",
            "For AC with reactance, use Impedance (§15-04) instead.");

        add("power-law", "Power Law", "17-02", Kind.FORMULA, List.of(
                sel("mode", "Solve for", "P_VI", "P_VI", "Power from V and I", "P_IR", "Power from I and R", "P_VR", "Power from V and R",
                        "V_PR", "Voltage from P and R", "I_PR", "Current from P and R"),
                num("V", "Voltage V (volts)", 70.7), num("I", "Current I (amps)", 1.41), num("R", "Resistance R (ohms)", 50), num("P", "Power P (watts)", 100)),
            v -> {
                double V = pf(v, "V"), I = pf(v, "I"), R = pf(v, "R"), P = pf(v, "P");
                String m = str(v, "mode");
                double res; String formula, label;
                switch (m) {
                    case "P_VI" -> { res = V * I; formula = "P = V × I"; label = "P (W)"; }
                    case "P_IR" -> { res = I * I * R; formula = "P = I² × R"; label = "P (W)"; }
                    case "P_VR" -> { res = V * V / R; formula = "P = V² / R"; label = "P (W)"; }
                    case "V_PR" -> { res = Math.sqrt(P * R); formula = "V = √(P · R)"; label = "V (V)"; }
                    default -> { res = Math.sqrt(P / R); formula = "I = √(P / R)"; label = "I (A)"; }
                }
                return rows(r("Formula", formula), r("Result", label + ": " + fx(res, 3)),
                    r("Cross-check (P = V·I)", fx(V * I, 2) + " W"), r("Cross-check (P = I²R)", fx(I * I * R, 2) + " W"),
                    r("Cross-check (P = V²/R)", fx(V * V / R, 2) + " W"));
            }, null,
            "Use RMS for AC. RMS = Peak / √2 for sine waves.",
            "For SSB, peak power (PEP) is the maximum envelope; average is much lower (~25-40 W avg for 100 W PEP voice).",
            "Resistor wattage rating: derate to 50%. A 100 W resistor running at 100 W is failing.");

        add("reactance", "Reactance (X_L, X_C)", "17-03", Kind.FORMULA, List.of(
                sel("mode", "Type", "L", "L", "Inductive (X_L = 2πfL)", "C", "Capacitive (X_C = 1/(2πfC))"),
                num("f", "Frequency f (MHz)", 14.150), num("L", "Inductance L (µH)", 5), num("C", "Capacitance C (pF)", 50)),
            v -> {
                double f = pf(v, "f") * 1e6, L = pf(v, "L") * 1e-6, C = pf(v, "C") * 1e-12;
                double xl = 2 * Math.PI * f * L, xc = 1 / (2 * Math.PI * f * C);
                boolean ind = "L".equals(v.get("mode"));
                double x = ind ? xl : xc;
                return rows(r("Formula", ind ? "X_L = 2π · f · L" : "X_C = 1 / (2π · f · C)"),
                    r("Result", (ind ? "X_L" : "X_C") + " = " + fx(x, 2) + " Ω"),
                    r("X_L (cross)", fx(xl, 2) + " Ω"), r("X_C (cross)", fx(xc, 2) + " Ω"),
                    r("X_L − X_C (net X)", fx(xl - xc, 2) + " Ω  (" + (xl > xc ? "inductive" : xl < xc ? "capacitive" : "resonant") + ")"));
            }, null,
            "Convert MHz → Hz, µH → H, pF → F before plugging in. Calculator handles this for you.",
            "Reactance does NOT dissipate power — only resistance does.",
            "When X_L = X_C the circuit is resonant (see §15-05).");

        add("impedance", "Impedance |Z| / phase", "17-04", Kind.FORMULA, List.of(
                num("R", "Resistance R (Ω)", 65), num("X", "Reactance X (Ω, signed: + = ind, − = cap)", 28), num("Z0", "Reference Z₀ (Ω)", 50)),
            v -> {
                double R = pf(v, "R"), X = pf(v, "X"), Z0 = pf(v, "Z0");
                double[] g = gamma(R, X, Z0);
                double swr = swrOf(g[2]);
                return rows(r("Z = R + jX", fx(R, 2) + " + j" + fx(X, 2) + " Ω"),
                    r("|Z|", fx(Math.sqrt(R * R + X * X), 2) + " Ω"),
                    r("Phase ∠Z", fx(Math.atan2(X, R) * 180 / Math.PI, 2) + "°  (" + (X > 0 ? "inductive" : X < 0 ? "capacitive" : "resistive") + ")"),
                    r("Reflection |Γ|", fx(g[2], 4) + "  ∠ " + fx(g[3], 1) + "°"),
                    r("SWR (vs Z₀)", fin(swr) ? fx(swr, 2) + ":1" : "∞ (open / short)"));
            }, null,
            "X is signed: + for inductive, − for capacitive. The display tells you which.",
            "Pure resistance (X=0) gives SWR = R/Z₀ when R > Z₀, or Z₀/R when R < Z₀.",
            "See §15-13 for the Smith chart calc using these same Γ values.");

        add("resonance", "Resonant Frequency", "17-05", Kind.FORMULA, List.of(
                sel("mode", "Solve for", "f", "f", "f from L and C", "L", "L from f and C", "C", "C from f and L"),
                num("L", "Inductance L (µH)", 5), num("C", "Capacitance C (pF)", 50), num("f", "Frequency f (MHz)", 10.07)),
            v -> {
                double L = pf(v, "L") * 1e-6, C = pf(v, "C") * 1e-12, f = pf(v, "f") * 1e6;
                String m = str(v, "mode");
                double res; String formula, label;
                if (m.equals("f")) { res = 1 / (2 * Math.PI * Math.sqrt(L * C)) / 1e6; formula = "f = 1 / (2π · √(L · C))"; label = "f (MHz)"; }
                else if (m.equals("L")) { res = 1 / (4 * Math.PI * Math.PI * f * f * C) * 1e6; formula = "L = 1 / (4π² · f² · C)"; label = "L (µH)"; }
                else { res = 1 / (4 * Math.PI * Math.PI * f * f * L) * 1e12; formula = "C = 1 / (4π² · f² · L)"; label = "C (pF)"; }
                return rows(r("Formula", formula), r("Result", label + ": " + fx(res, 3)),
                    r("Quick form (MHz · µH · pF)", "f(MHz) = 159.15 / √(L(µH)·C(pF))"));
            }, null,
            "At resonance X_L = X_C — they cancel and the circuit is purely resistive.",
            "High Q narrows the resonance peak; low Q broadens it (see §15-11).",
            "Use this to size traps (§07-13) and tank circuits.");

        add("wavelength", "Wavelength", "17-06", Kind.FORMULA, List.of(
                num("f", "Frequency f (MHz)", 14.150),
                num("vf", "Velocity factor (1.0 = free space)", 1.0, "For coax, use the cable's VF (RG-58 ≈ 0.66, LMR-400 ≈ 0.85, ladder line ≈ 0.91).")),
            v -> {
                double f = pf(v, "f"), vf = pf(v, "vf");
                double lm = (300 / f) * vf, lft = lm / 0.3048, half = 468 / f;
                return rows(r("Wavelength λ", fx(lm, 3) + " m  (" + fx(lft, 2) + " ft)"),
                    r("λ/2", fx(lm / 2, 3) + " m  (" + fx(lft / 2, 2) + " ft)"),
                    r("λ/4", fx(lm / 4, 3) + " m  (" + fx(lft / 4, 2) + " ft)"),
                    r("½-wave dipole (in air, end-effect)", fx(half, 2) + " ft  (k=468)"),
                    r("¼-wave vertical (in air)", fx(half / 2, 2) + " ft  (k=234)"));
            }, null,
            "Free-space λ(m) = 300 / f(MHz). For coax / ladder line, multiply by VF.",
            "Half-wave dipole length = 468 / f(MHz) ft accounts for ~5% end-effect on thin wire.",
            "Quarter-wave vertical = 234 / f(MHz) ft (same correction, halved).");

        add("swr", "SWR", "17-07", Kind.FORMULA, List.of(
                sel("mode", "Compute from", "pwr", "pwr", "Forward / reflected power", "z", "Load impedance Z_L vs Z₀"),
                num("Pf", "Forward power P_f (W)", 100), num("Pr", "Reflected power P_r (W)", 4),
                num("R", "Load R (Ω)", 75), num("X", "Load X (Ω, signed)", 0), num("Z0", "Z₀ (Ω)", 50)),
            v -> {
                double gm; String formula;
                if ("pwr".equals(v.get("mode"))) { gm = Math.sqrt(pf(v, "Pr") / pf(v, "Pf")); formula = "|Γ| = √(P_r / P_f);  SWR = (1 + |Γ|) / (1 − |Γ|)"; }
                else { gm = gamma(pf(v, "R"), pf(v, "X"), pf(v, "Z0"))[2]; formula = "Γ = (Z_L − Z₀)/(Z_L + Z₀);  SWR = (1 + |Γ|)/(1 − |Γ|)"; }
                double swr = swrOf(gm);
                double loss = gm >= 1 ? Double.POSITIVE_INFINITY : -10 * Math.log10(1 - gm * gm);
                return rows(r("Formula", formula), r("Reflection |Γ|", fx(gm, 4)),
                    r("SWR", fin(swr) ? fx(swr, 2) + ":1" : "∞:1 (full reflection)"),
                    r("Reflected power", fx(100 * gm * gm, 1) + "%"),
                    r("Mismatch loss", fin(loss) ? fx(loss, 2) + " dB" : "∞"));
            }, null,
            "SWR ≤ 1.5 is excellent; ≤ 2 is good; ≤ 3 is acceptable for most rigs.",
            "Mismatch loss column: power lost vs. power available with perfect match.",
            "On a lossy feedline, SWR at the rig reads lower than at the antenna — the cable hides mismatch.");

        add("erp", "ERP / EIRP", "17-08", Kind.FORMULA, List.of(
                num("P_TX", "Transmitter power P_TX (W)", 100), num("gain", "Antenna gain", 6),
                sel("unit", "Gain reference", "dBd", "dBd", "dBd (vs dipole)", "dBi", "dBi (vs isotropic)", "lin", "linear ratio"),
                num("loss", "Feedline loss (dB)", 1)),
            v -> {
                double P = pf(v, "P_TX"), g = pf(v, "gain"), lossDb = pf(v, "loss");
                String u = str(v, "unit");
                double gDbi = u.equals("dBd") ? g + 2.15 : u.equals("dBi") ? g : 10 * Math.log10(g);
                double gLin = Math.pow(10, gDbi / 10), lLin = Math.pow(10, -lossDb / 10);
                double eirp = P * gLin * lLin, erp = eirp / Math.pow(10, 2.15 / 10);
                return rows(r("Antenna gain", fx(gDbi, 2) + " dBi  (" + fx(gDbi - 2.15, 2) + " dBd, ×" + fx(gLin, 2) + ")"),
                    r("Feedline factor", "×" + fx(lLin, 3) + "  (lose " + fx(100 * (1 - lLin), 1) + "%)"),
                    r("EIRP", fx(eirp, 1) + " W  (" + fx(10 * Math.log10(eirp), 1) + " dBW, " + fx(10 * Math.log10(eirp * 1000), 1) + " dBm)"),
                    r("ERP (vs dipole)", fx(erp, 1) + " W"));
            }, null,
            "EIRP uses dBi (isotropic) reference; ERP uses dBd (dipole). Differ by 2.15 dB.",
            "For RF-safety / FCC compliance use EIRP — see §15-14 RF Exposure.",
            "PEP is the peak; for averaged exposure (FCC), use average power ≈ PEP × duty cycle × mode factor.");

        add("feedline-loss", "Feedline Loss", "17-09", Kind.FORMULA, List.of(
                sel("cable", "Cable type", "lmr400", "rg58", "RG-58 (foam)", "rg58a", "RG-58A (PVC)", "rg8x", "RG-8X", "rg213", "RG-213 (foam)",
                        "lmr400", "LMR-400", "lmr600", "LMR-600", "heliax", "7/8\" Heliax (LDF5-50A)"),
                num("len_ft", "Length (ft)", 100), num("f", "Frequency (MHz)", 14.150), num("P_in", "Power into line (W)", 100)),
            v -> {
                double[] fq = {1.8, 7, 14, 28, 50, 144, 432, 1300};
                Map<String, double[]> tbl = Map.of(
                    "rg58",   new double[]{0.4, 0.9, 1.3, 1.9, 2.5, 4.6, 8.4, 16.5},
                    "rg58a",  new double[]{0.5, 1.1, 1.6, 2.4, 3.3, 6.0, 11.4, 22.0},
                    "rg8x",   new double[]{0.3, 0.6, 0.9, 1.3, 1.7, 3.1, 5.7, 11.0},
                    "rg213",  new double[]{0.2, 0.4, 0.6, 0.8, 1.1, 1.9, 3.4, 6.8},
                    "lmr400", new double[]{0.1, 0.3, 0.4, 0.6, 0.8, 1.4, 2.6, 4.8},
                    "lmr600", new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.9, 1.7, 3.2},
                    "heliax", new double[]{0.05, 0.1, 0.15, 0.2, 0.3, 0.5, 1.0, 1.9});
                double[] row = tbl.getOrDefault(str(v, "cable"), tbl.get("lmr400"));
                double f = pf(v, "f");
                double db = Double.NaN;
                if (f <= fq[0]) db = row[0];
                else if (f >= fq[fq.length - 1]) db = row[row.length - 1];
                else for (int i = 0; i < fq.length - 1; i++) {
                    if (f >= fq[i] && f <= fq[i + 1]) {
                        double t = (Math.log10(f) - Math.log10(fq[i])) / (Math.log10(fq[i + 1]) - Math.log10(fq[i]));
                        db = row[i] + t * (row[i + 1] - row[i]);
                        break;
                    }
                }
                double loss = db * pf(v, "len_ft") / 100, factor = Math.pow(10, -loss / 10);
                double pin = pf(v, "P_in"), pout = pin * factor;
                return rows(r("Cable loss spec", fx(db, 2) + " dB / 100 ft @ " + fx(f, 2) + " MHz"),
                    r("Total loss", fx(loss, 2) + " dB"),
                    r("Power factor", "×" + fx(factor, 3) + "  (" + fx(factor * 100, 1) + "% delivered)"),
                    r("Power delivered", fx(pout, 1) + " W"),
                    r("Power lost", fx(pin - pout, 1) + " W (heats the cable)"));
            }, null,
            "Loss roughly doubles when frequency quadruples — VHF needs better cable than HF.",
            "Mismatch on a lossy line ADDS to matched loss; see §15-07 SWR for the penalty calc.",
            "Cable specs vary slightly by manufacturer; this calculator uses canonical values.");

        add("decibels", "Decibels", "17-10", Kind.FORMULA, List.of(
                sel("mode", "Convert", "lin2db", "lin2db", "Linear ratio → dB (power)", "db2lin", "dB → linear ratio (power)", "lin2db_v", "Voltage ratio → dB",
                        "watt2dbm", "Watts → dBm", "dbm2watt", "dBm → Watts"),
                num("val", "Input value", 100)),
            v -> {
                double x = pf(v, "val"), res; String formula, unit;
                switch (str(v, "mode")) {
                    case "lin2db" -> { res = 10 * Math.log10(x); formula = "dB = 10 · log₁₀(P₂/P₁)"; unit = "dB"; }
                    case "db2lin" -> { res = Math.pow(10, x / 10); formula = "ratio = 10^(dB/10)"; unit = "×"; }
                    case "lin2db_v" -> { res = 20 * Math.log10(x); formula = "dB = 20 · log₁₀(V₂/V₁)"; unit = "dB"; }
                    case "watt2dbm" -> { res = 10 * Math.log10(x * 1000); formula = "dBm = 10 · log₁₀(W·1000)"; unit = "dBm"; }
                    default -> { res = Math.pow(10, x / 10) / 1000; formula = "W = 10^(dBm/10) / 1000"; unit = "W"; }
                }
                return rows(r("Formula", formula), r("Result", fx(res, 3) + " " + unit),
                    r("Reference", "0 dBm = 1 mW; 30 dBm = 1 W; 60 dBm = 1 kW; +3 dB ≈ ×2; +10 dB = ×10"));
            }, null,
            "Power: 10·log; voltage / current at same impedance: 20·log.",
            "dBm reference is 1 mW; dBW reference is 1 W. Differ by 30 dB.",
            "Quick mental: 3 dB = ×2; 10 dB = ×10; combine these for any dB.");

        add("q-factor", "Q Factor", "17-11", Kind.FORMULA, List.of(
                sel("mode", "Compute from", "XR", "XR", "X / R at resonance", "fbw", "f / Δf₃dB"),
                num("X", "Reactance X (Ω at resonance)", 314), num("R", "Loss resistance R (Ω)", 1),
                num("f", "Center frequency f (MHz)", 14.150), num("bw", "3-dB bandwidth Δf (kHz)", 35)),
            v -> {
                double q; String formula;
                if ("XR".equals(v.get("mode"))) { q = pf(v, "X") / pf(v, "R"); formula = "Q = X / R"; }
                else { q = (pf(v, "f") * 1000) / pf(v, "bw"); formula = "Q = f / Δf₃dB"; }
                double f = pf(v, "f");
                return rows(r("Formula", formula), r("Q", fx(q, 1)),
                    r("Implied 3-dB bandwidth", fx((f * 1000) / q, 1) + " kHz @ " + fx(f, 2) + " MHz"),
                    r("Voltage rise factor at resonance (V_C / V_in)", "Q = " + fx(q, 1) + "× (in series LC)"));
            }, null,
            "High Q (>200) = sharp resonance, narrow bandwidth, low loss.",
            "Q ≈ 100 typical for L/C tank circuits; ≈ 400-500 for mag loops; ≈ 10,000 for crystals.",
            "Voltage across the cap at resonance is Q × input voltage — sizes the cap's voltage rating.");

        add("bandwidth", "Bandwidth", "17-12", Kind.FORMULA, List.of(
                num("f", "Center frequency f (MHz)", 14.150), num("Q", "Q factor", 200)),
            v -> {
                double f = pf(v, "f"), q = pf(v, "Q"), bw = (f * 1000) / q;
                return rows(r("Formula", "BW = f / Q"),
                    r("3-dB bandwidth", fx(bw, 2) + " kHz  (" + fx(100 / q, 2) + "% of f)"),
                    r("Approximate SWR-2:1 BW (single-tuned antenna)", fx(bw * 1.41, 1) + " kHz"));
            }, null,
            "Q = 100 antenna at 14 MHz → ~140 kHz BW; Q = 400 mag loop → ~35 kHz BW.",
            "SWR-2:1 antenna BW is approximately √2 × the 3-dB BW.",
            "CW filter BW typically 250-500 Hz; SSB filter 2.4-3 kHz; AM 6 kHz; FM 12.5-25 kHz.");

        add("smith-chart", "Smith Chart (Γ ↔ Z)", "17-13", Kind.FORMULA, List.of(
                sel("mode", "Direction", "z2g", "z2g", "Z → Γ", "g2z", "Γ → Z"),
                num("R", "R (Ω)", 75), num("X", "X (Ω, signed)", 0), num("Z0", "Z₀ (Ω)", 50),
                num("Gmag", "|Γ|", 0.2), num("Gphase", "∠Γ (deg)", 0)),
            v -> {
                double z0 = pf(v, "Z0");
                if ("z2g".equals(v.get("mode"))) {
                    double R = pf(v, "R"), X = pf(v, "X");
                    double[] g = gamma(R, X, z0);
                    double swr = swrOf(g[2]);
                    return rows(r("Formula", "Γ = (Z − Z₀) / (Z + Z₀)"),
                        r("z (normalized Z)", fx(R / z0, 3) + " + j" + fx(X / z0, 3)),
                        r("|Γ|", fx(g[2], 4)), r("∠Γ", fx(g[3], 1) + "°"),
                        r("Γ (rect)", fx(g[0], 4) + " + j" + fx(g[1], 4)),
                        r("SWR", fin(swr) ? fx(swr, 2) + ":1" : "∞:1"));
                }
                double gm = pf(v, "Gmag"), gp = pf(v, "Gphase") * Math.PI / 180;
                double gr = gm * Math.cos(gp), gi = gm * Math.sin(gp);
                double nr = 1 + gr, ni = gi, dr = 1 - gr, di = -gi, d2 = dr * dr + di * di;
                double zr = (nr * dr + ni * di) / d2, zi = (ni * dr - nr * di) / d2;
                double swr = swrOf(gm);
                return rows(r("Formula", "z = (1 + Γ) / (1 − Γ);  Z = z × Z₀"),
                    r("z (normalized)", fx(zr, 3) + " + j" + fx(zi, 3)),
                    r("Z", fx(zr * z0, 2) + " + j" + fx(zi * z0, 2) + " Ω"),
                    r("SWR", fin(swr) ? fx(swr, 2) + ":1" : "∞:1"));
            }, null,
            "Smith chart is a polar plot of Γ. Center = perfect match (Γ=0).",
            "Normalized impedance z = Z/Z₀. Plot at the intersection of constant-r and constant-x circles.",
            "Half-wavelength of lossless line rotates 360° around the chart; quarter-wave rotates 180°.");

        add("rf-exposure", "RF Exposure", "17-14", Kind.FORMULA, List.of(
                num("P_PEP", "Transmitter PEP (W)", 100),
                sel("mode", "Mode", "ssb", "ssb", "SSB voice (k_avg ≈ 0.20)", "am", "AM voice (k_avg ≈ 0.50)", "fm", "FM voice (k_avg = 1.0)",
                        "cw", "CW (k_avg ≈ 0.40)", "ft8", "FT8/FT4/RTTY (k_avg = 1.0)", "psk31", "PSK31 (k_avg ≈ 0.5)"),
                num("D", "TX duty cycle (0..1)", 0.5, "Fraction of TX time during a 6-min uncontrolled / 30-min controlled window."),
                num("L_dB", "Feedline loss (dB)", 1), num("G_dBi", "Antenna gain (dBi)", 8.15),
                num("d_m", "Distance to evaluation point (m)", 5), num("f", "Frequency (MHz)", 14.15),
                sel("env", "Environment", "uncontrolled", "uncontrolled", "Uncontrolled (public, neighbors)", "controlled", "Controlled (operator and family)")),
            v -> {
                double pep = pf(v, "P_PEP"), D = pf(v, "D"), lDb = pf(v, "L_dB"), gDbi = pf(v, "G_dBi"), d = pf(v, "d_m"), f = pf(v, "f");
                double k = switch (str(v, "mode")) { case "ssb" -> 0.20; case "am" -> 0.50; case "cw" -> 0.40; case "psk31" -> 0.5; default -> 1.0; };
                double pavg = pep * k * D, pant = pavg * Math.pow(10, -lDb / 10), eirp = pant * Math.pow(10, gDbi / 10);
                double sWorst = eirp / (Math.PI * d * d);
                boolean unc = "uncontrolled".equals(v.get("env"));
                double mpe;
                if (unc) {
                    if (f >= 0.3 && f <= 3.0) mpe = Math.min(100, 100 / (f * f));
                    else if (f > 3.0 && f <= 30) mpe = 180 / (f * f);
                    else if (f > 30 && f <= 300) mpe = 0.2;
                    else if (f > 300 && f <= 1500) mpe = 0.0067 * f;
                    else mpe = 1.0;
                } else {
                    if (f >= 0.3 && f <= 3.0) mpe = 100;
                    else if (f > 3.0 && f <= 30) mpe = 900 / (f * f);
                    else if (f > 30 && f <= 300) mpe = 1.0;
                    else if (f > 300 && f <= 1500) mpe = 0.0335 * f;
                    else mpe = 5.0;
                }
                double margin = mpe / sWorst;
                return rows(r("Average power factor (mode × duty)", fx(k * D, 3) + " × " + num(pep) + " W"),
                    r("Avg power into feedline", fx(pavg, 1) + " W"),
                    r("Power at antenna (after feedline)", fx(pant, 1) + " W"),
                    r("EIRP", fx(eirp, 1) + " W (" + fx(10 * Math.log10(eirp), 1) + " dBW)"),
                    r("Power density at d (4× ground reflection)", fx(sWorst, 3) + " W/m²"),
                    r("MPE limit (" + str(v, "env") + ")", fx(mpe, 3) + " W/m²"),
                    r("Compliance", sWorst <= mpe ? "✅ COMPLIANT (margin " + fx(margin, 2) + "×)" : "❌ EXCEEDS MPE (over by " + fx(1 / margin, 2) + "×)"));
            }, null,
            "Average power = PEP × mode-factor × duty. SSB voice averages ~10–20% of PEP.",
            "EIRP not ERP. Convert dBd → dBi by adding 2.15 if your antenna spec is in dBd.",
            "FCC averages over 6 min uncontrolled / 30 min controlled.",
            "Failed scenarios: reduce power, reduce duty cycle, increase distance, or aim antenna away.");
    }
}
