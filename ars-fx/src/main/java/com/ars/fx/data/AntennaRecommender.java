package com.ars.fx.data;

import java.util.*;
import java.util.function.Function;

/**
 * Antenna Workshop recommender — a five-step questionnaire and per-antenna fit scoring
 * (J-Learn §09-01). Each antenna's fit returns 0..10; 0 disqualifies it. The top results within
 * 70% of the best score are returned, ranked. No UI here.
 */
public final class AntennaRecommender {
    private AntennaRecommender() {}

    public enum Type { SINGLE, MULTI, NUMBER }
    public record Q(String id, String text, Type type, String[][] choices, String placeholder) {}
    public record Step(String title, List<Q> questions) {}
    public record Result(Antenna ant, double score, List<String> reasons) {}
    public record Score(double score, List<String> reasons) {}

    /** Answers: single/number → String, multi → List&lt;String&gt;. */
    public static final class Answers extends HashMap<String, Object> {
        String s(String k) { Object o = get(k); return o instanceof String x ? x : null; }
        boolean is(String k, String... vals) { String x = s(k); return x != null && Arrays.asList(vals).contains(x); }
        @SuppressWarnings("unchecked") List<String> list(String k) { Object o = get(k); return o instanceof List<?> l ? (List<String>) l : List.of(); }
    }

    public record Antenna(String id, String name, String section, String summary, Function<Answers, Score> fit) {}

    private static Q single(String id, String text, String... cv) {
        String[][] ch = new String[cv.length / 2][2];
        for (int i = 0; i < ch.length; i++) { ch[i][0] = cv[2 * i]; ch[i][1] = cv[2 * i + 1]; }
        return new Q(id, text, Type.SINGLE, ch, null);
    }
    private static Q multi(String id, String text, String... cv) {
        String[][] ch = new String[cv.length / 2][2];
        for (int i = 0; i < ch.length; i++) { ch[i][0] = cv[2 * i]; ch[i][1] = cv[2 * i + 1]; }
        return new Q(id, text, Type.MULTI, ch, null);
    }
    private static Q number(String id, String text, String ph) { return new Q(id, text, Type.NUMBER, null, ph); }

    public static final List<Step> STEPS = List.of(
        new Step("A — Your Dwelling", List.of(
            single("A1", "Building type", "sf-detached", "Single-family detached", "sf-attached", "Single-family attached (row house, townhouse)",
                "mf-low", "Multi-family low-rise (duplex, triplex, walk-up)", "mf-high", "Multi-family high-rise (apartment 3+ stories)",
                "mobile", "Mobile home / manufactured", "rv", "RV / camper", "none", "No fixed residence"),
            single("A2", "Ownership", "own", "Own", "rent", "Rent", "condo-hoa", "Condo with HOA", "hoa-sf", "HOA single-family"),
            single("A3", "HOA / antenna restrictions", "none", "None", "light", "Some restrictions (no visible antennas above roofline)",
                "strict", "Strict (nothing visible from outside)", "unknown", "Don't know / haven't checked"),
            single("A4", "Indoor / attic option", "attic-clear", "Yes — usable attic, non-metal roof",
                "attic-metal", "Yes — but attic is metal-clad / has foil insulation", "no", "No attic option", "unknown", "Don't know"))),
        new Step("B — Your Lot", List.of(
            number("B1L", "Lot length (ft)", "e.g. 120"), number("B1W", "Lot width (ft)", "e.g. 80"),
            single("B2", "Lot orientation (long axis)", "ns", "North-South", "ew", "East-West", "square", "Roughly square / doesn't matter", "irregular", "Irregular"),
            single("B3N", "How many trees usable as antenna supports?", "0", "None", "1", "1 tree", "2", "2 trees", "3+", "3 or more"),
            number("B3H", "Tallest tree height (ft)", "e.g. 60"),
            single("B4", "Existing tower", "none", "None", "pushup", "Push-up mast (~30 ft)", "self40", "Self-supporting 40-50 ft", "guyed50", "Guyed 50+ ft", "crankup", "Crank-up"),
            single("B5", "Could you put up a tower if you wanted?", "approved", "Already approved / no obstacles", "possible", "Could be approved but not yet",
                "forbidden", "Forbidden (HOA, lease, code)", "no-interest", "Not interested"),
            single("B6", "Roof access", "flat", "Yes — flat roof", "pitched", "Yes — pitched roof", "no", "No / can't go up there"),
            single("B7", "Soil for buried radials", "lawn", "Lawn (good)", "drive", "Driveway / patio", "rocks", "Rocks / shallow soil",
                "salt", "Salt marsh / coastal (excellent)", "unknown", "Don't know"))),
        new Step("C — Your Goals", List.of(
            multi("C1", "Bands wanted (check all that apply)", "160", "160m", "80", "80m", "40", "40m", "30", "30m", "20", "20m", "17", "17m",
                "15", "15m", "12", "12m", "10", "10m", "6", "6m", "2", "2m", "70cm", "70cm"),
            single("C2", "Multi-band priority", "single", "One band done well", "few", "A few specific bands done well", "all", "All HF bands acceptably (with tuner)"),
            multi("C3", "Operating modes (check all that apply)", "ssb", "SSB voice", "cw", "CW", "ft8", "FT8 / FT4", "rtty", "RTTY",
                "digital", "Other digital (PSK31, JS8Call)", "fm", "FM voice (repeaters)", "dv", "Digital voice (DMR/Fusion/D-STAR)", "packet", "Packet / APRS"),
            single("C4", "Power level", "qrp", "QRP (≤ 10 W)", "100", "100 W", "500", "500 W", "legal", "Legal limit (1500 W)"),
            multi("C5", "Operating goals (check all that apply)", "rag", "Local rag-chew", "nvis", "Regional NVIS", "dx", "DX hunting", "contest", "Contesting",
                "emcomm", "Emergency communications", "portable", "Portable (POTA / SOTA)", "fd", "Field Day"))),
        new Step("D — Constraints", List.of(
            single("D1", "Budget", "lt50", "Under $50", "50-200", "$50 – $200", "200-500", "$200 – $500", "500-1500", "$500 – $1500", "gt1500", "Over $1500", "no-limit", "No firm limit"),
            single("D2", "Time investment", "hours", "A few hours", "weekend", "A weekend", "weekends", "Multiple weekends", "long", "A long-term project"),
            single("D3", "Skill level", "first", "First antenna", "couple", "Built one or two", "exp", "Experienced builder", "eng", "Antenna-engineering background"),
            single("D4", "Climbing comfort", "tower", "I'll climb a tower", "roof", "Pitched roof OK", "ladder", "Step ladder only", "ground", "No heights — keep it on the ground"),
            single("D5", "Stealth required", "none", "Visible OK", "subtle", "Subtle preferred", "hidden", "Must be invisible from neighbors", "stealth", "Must be invisible always (attic / flagpole only)"))),
        new Step("E — Existing Infrastructure", List.of(
            single("E2", "Coax in place", "none", "None — buying new", "rg58", "RG-58", "rg8x", "RG-8X", "rg213", "RG-213", "lmr400", "LMR-400",
                "hardline", "Hardline (Heliax, etc.)", "ladder", "Window line / ladder line", "unknown", "Don't know"),
            single("E3", "Tuner available", "none", "None", "internal", "Internal (rig only)", "ext100", "External 100 W", "ext1500", "External 1500 W", "remote", "Auto + remote"),
            single("E4", "Grounding system", "none", "None", "rod", "Single ground rod", "array", "Multi-rod bonded array", "counter", "Dedicated counterpoise wire"))));

    // ---- answer helpers -----------------------------------------------------------------------------------
    private static boolean band(Answers a, String b) { return a.list("C1").contains(b); }
    private static boolean anyBand(Answers a, String... bs) { for (String b : bs) if (band(a, b)) return true; return false; }
    private static boolean goal(Answers a, String g) { return a.list("C5").contains(g); }
    private static boolean modes(Answers a, String... ms) { for (String m : ms) if (a.list("C3").contains(m)) return true; return false; }
    private static Double d(String s) { double x = AntennaCalcs.pf(s); return Double.isNaN(x) ? null : x; }
    /** Longest lot dimension in feet, or null. */
    private static Double lot(Answers a) {
        Double l = d(a.s("B1L")), w = d(a.s("B1W"));
        if (l == null) return w;
        return w == null ? l : Math.max(l, w);
    }
    private static int trees(Answers a) {
        String n = a.s("B3N");
        if (n == null) return 0;
        if (n.equals("3+")) return 3;
        try { return Integer.parseInt(n); } catch (NumberFormatException e) { return 0; }
    }
    private static boolean outdoor(Answers a) { return !"strict".equals(a.s("A3")); }
    private static boolean canTower(Answers a) {
        String t = a.s("B4");
        if (t != null && !t.equals("none")) return true;
        return a.is("B5", "approved", "possible");
    }
    private static double bandMHz(int b) {
        return switch (b) { case 160 -> 1.9; case 80 -> 3.7; case 40 -> 7.15; case 30 -> 10.13; case 20 -> 14.15; case 17 -> 18.12; case 15 -> 21.2;
            case 12 -> 24.94; case 10 -> 28.5; case 6 -> 50.15; case 2 -> 146; default -> 14.15; };
    }
    /** Largest numeric band (metres) chosen, 0 when none numeric (JS Math.max over parseInt || 0). */
    private static int longestBand(Answers a) {
        int m = 0;
        for (String b : a.list("C1")) { try { m = Math.max(m, Integer.parseInt(b.replaceAll("\\D.*$", ""))); } catch (NumberFormatException ignored) {} }
        return m;
    }
    private static Score sc(double s, List<String> r) { return new Score(Math.min(10, Math.max(0, s)), r); }
    private static Score none() { return new Score(0, List.of()); }
    private static Score fixed(double s, String... reasons) { return new Score(s, List.of(reasons)); }

    public static final List<Antenna> ANTENNAS = List.of(
        new Antenna("flat-dipole", "Flat Dipole", "09-02", "Half-wave dipole strung horizontally between two supports. Single-band, simplest, cheapest.", a -> {
            List<String> r = new ArrayList<>();
            if (!outdoor(a) && a.is("A4", "no")) return none();
            Double lot = lot(a);
            if (a.list("C1").isEmpty()) return fixed(4, "no bands selected — generic suggestion");
            int lb = longestBand(a);
            long half = lb > 0 ? Math.round(468 / bandMHz(lb)) : 65;
            if (lot != null && lot < half + 5) return fixed(1, "lot " + AntennaCalcs.num(lot) + " ft is too short for a " + lb + "m dipole (~" + half + " ft)");
            double s = 7;
            if (trees(a) >= 2) { s += 1.5; r.add("two supports available (" + trees(a) + " trees)"); }
            if (a.is("A3", "none", "light")) s += 0.3;
            if (a.is("D3", "first", "couple")) { s += 0.5; r.add("beginner-friendly build"); }
            if (a.is("D1", "lt50", "50-200")) { s += 0.3; r.add("inexpensive (wire + insulators + balun)"); }
            if (a.is("C2", "single")) { s += 0.5; r.add("matches single-band priority"); }
            if (a.is("C2", "all")) { s -= 1.0; r.add("⚠ single-band only — consider trapped or fan variant"); }
            if (a.is("D5", "stealth")) { s -= 2; r.add("⚠ visible installation"); }
            return sc(s, r);
        }),
        new Antenna("inverted-v", "Inverted-V Dipole", "09-03", "Dipole with apex at center support, ends sloping down to stakes. Needs only one tall support.", a -> {
            List<String> r = new ArrayList<>();
            if (!outdoor(a) && a.is("A4", "no")) return none();
            if (a.list("C1").isEmpty()) return fixed(4);
            int lb = longestBand(a);
            long half = Math.round(468 / bandMHz(lb));
            Double lot = lot(a);
            if (lot != null && lot < half * 0.8) return fixed(1, "lot " + AntennaCalcs.num(lot) + " ft still too short for " + lb + "m inverted-V");
            double s = 7.5;
            if (trees(a) >= 1) { s += 1.5; r.add("one tall support sufficient — " + trees(a) + " tree(s) available"); }
            Double th = d(a.s("B3H"));
            if (th != null && th >= 30) { s += 0.5; r.add("apex height " + a.s("B3H") + " ft is good"); }
            if (a.is("D3", "first", "couple")) { s += 0.5; r.add("beginner-friendly"); }
            if (a.is("D1", "lt50", "50-200")) { s += 0.3; r.add("inexpensive build"); }
            if (a.is("D5", "stealth")) { s -= 1.5; r.add("⚠ apex still visible"); }
            return sc(s, r);
        }),
        new Antenna("fan-dipole", "Fan Dipole", "09-04", "Multiple parallel dipoles fed from one feedpoint. Resonant on each band without traps.", a -> {
            List<String> r = new ArrayList<>();
            if (!outdoor(a) && a.is("A4", "no")) return none();
            long bands = a.list("C1").stream().filter(b -> { try { return Integer.parseInt(b.replaceAll("\\D.*$", "")) <= 80; } catch (NumberFormatException e) { return false; } }).count();
            if (bands < 2) return fixed(2, "fan only useful for 2+ bands");
            double s = 7;
            if (a.is("C2", "few")) { s += 1.5; r.add("fits \"" + bands + " bands done well\" goal"); }
            if (a.is("D3", "couple", "exp")) { s += 0.5; r.add("moderate build skill needed"); }
            if (a.is("D3", "first")) { s -= 0.5; r.add("⚠ harder first build than single dipole"); }
            if (trees(a) >= 2) { s += 1; r.add("two supports available"); }
            if (a.is("D5", "stealth")) { s -= 2.5; r.add("⚠ multiple wires visible"); }
            return sc(s, r);
        }),
        new Antenna("trapped-dipole", "Trapped Dipole", "09-05", "Single wire with traps that isolate band segments. Multi-band in less space than fan.", a -> {
            List<String> r = new ArrayList<>();
            if (!outdoor(a) && a.is("A4", "no")) return none();
            if (a.list("C1").size() < 2) return fixed(2, "traps only useful for 2+ bands");
            double s = 7;
            if (a.is("C2", "few")) { s += 1; r.add("matches few-bands-well goal"); }
            if (a.is("D2", "weekends", "long")) { s += 0.5; r.add("time available for trap construction"); }
            if (a.is("D2", "hours")) { s -= 1; r.add("⚠ trap building takes a weekend+"); }
            if (a.is("D3", "first")) { s -= 1.5; r.add("⚠ requires intermediate skill"); }
            Double lot = lot(a);
            if (lot != null && lot < 65) { s -= 1; r.add("⚠ short lot may not fit even trapped 40m"); }
            if (trees(a) >= 2) { s += 0.5; r.add("two supports available"); }
            if (a.is("C4", "legal")) { s -= 0.5; r.add("⚠ legal-limit traps need careful design"); }
            return sc(s, r);
        }),
        new Antenna("ocf-dipole", "OCF Dipole (Windom)", "09-06", "Dipole fed off-center (~⅓ from one end) with 4:1 or 6:1 balun. Multi-band without traps or tuner.", a -> {
            List<String> r = new ArrayList<>();
            if (!outdoor(a) && a.is("A4", "no")) return none();
            if (a.list("C1").isEmpty()) return fixed(4);
            double s = 7;
            if (a.is("C2", "few", "all")) { s += 1; r.add("OCF covers 80/40/20/15/10 with one wire"); }
            if (a.is("E3", "none", "internal")) { s += 0.5; r.add("works without external tuner"); }
            if (a.is("D5", "stealth")) s -= 1.5;
            if (trees(a) >= 2) { s += 0.5; r.add("two supports available"); }
            if (a.is("D3", "first")) { s -= 0.5; r.add("⚠ needs balanced feed-line discipline"); }
            return sc(s, r);
        }),
        new Antenna("efhw-no-traps", "EFHW (No Traps)", "09-07", "Half-wave wire fed at one end via 49:1 or 64:1 unun. Resonant on harmonics — 80m EFHW covers 40/20/15/10.", a -> {
            List<String> r = new ArrayList<>();
            if (!outdoor(a) && a.is("A4", "no")) return none();
            double s = 8;
            if (trees(a) >= 1) { s += 1; r.add("only one support needed — " + trees(a) + " tree(s) available"); }
            if (a.is("C2", "few", "all")) { s += 1; r.add("harmonic coverage on 4 bands"); }
            if (a.is("A4", "attic-clear") && !outdoor(a)) { s += 1; r.add("attic install possible"); }
            if (!a.is("D3", "first")) s += 0.3;
            if (a.is("D5", "stealth")) { s -= 1; r.add("⚠ wire visible if installed outdoors"); }
            if (a.is("E3", "none")) { s -= 0.3; r.add("⚠ tuner helps for FT8/SSB on same band"); }
            Double lot = lot(a);
            if (lot != null && lot < 70) { s -= 1; r.add("⚠ short lot — 80m EFHW needs ~134 ft, can be sloped"); }
            return sc(s, r);
        }),
        new Antenna("efhw-trapped", "EFHW (Trapped)", "09-08", "Trapped end-fed half-wave for portable / restricted-length installations. Each band cuts the wire shorter.", a -> {
            List<String> r = new ArrayList<>();
            if (!outdoor(a) && a.is("A4", "no")) return none();
            double s = 6;
            if (goal(a, "portable")) { s += 2; r.add("excellent for POTA / SOTA portable"); }
            Double lot = lot(a);
            if (lot != null && lot < 70) { s += 1.5; r.add("fits short lot via traps"); }
            if (a.is("D3", "first")) { s -= 2; r.add("⚠ trap construction is intermediate"); }
            if (a.is("D2", "hours")) { s -= 1.5; r.add("⚠ need a weekend to wind traps"); }
            return sc(s, r);
        }),
        new Antenna("j-pole", "J-Pole (VHF/UHF)", "09-09", "Half-wave VHF/UHF radiator with quarter-wave matching stub. Easy build, omni, vertical polarization.", a -> {
            List<String> r = new ArrayList<>();
            if (!anyBand(a, "2", "70cm", "6")) return fixed(0, "J-pole is for VHF/UHF only");
            double s = 7.5;
            if (modes(a, "fm", "dv")) { s += 1; r.add("matches FM repeater operating"); }
            if (goal(a, "rag")) { s += 0.5; r.add("great for local FM rag-chew"); }
            if (a.is("D3", "first")) { s += 0.5; r.add("beginner-friendly — copper pipe + solder"); }
            if (a.is("D1", "lt50")) { s += 0.5; r.add("under $20 in copper"); }
            if (a.is("A1", "mf-high")) { s -= 1; r.add("⚠ apartment installation challenging"); }
            if (a.is("D5", "stealth")) { s -= 1.5; r.add("⚠ vertical pipe is visible"); }
            return sc(s, r);
        }),
        new Antenna("yagi", "Yagi-Uda", "09-10", "Directional beam with reflector and directors. High gain, narrow beamwidth — best for DX and contesting.", a -> {
            List<String> r = new ArrayList<>();
            if (!canTower(a)) return fixed(0, "Yagi requires a tower");
            if (a.is("B5", "forbidden")) return fixed(0, "tower forbidden");
            double s = 6;
            if (goal(a, "dx") || goal(a, "contest")) { s += 2.5; r.add("directional gain + F/B ratio for DX/contest"); }
            String tw = a.s("B4");
            if (tw != null && !tw.equals("none")) { s += 1.5; r.add("existing tower"); }
            else if (a.is("B5", "approved")) { s += 0.5; r.add("tower could be added"); }
            if (a.is("D1", "no-limit", "gt1500")) { s += 0.5; r.add("budget supports commercial Yagi"); }
            if (a.is("D1", "lt50", "50-200")) { s -= 2; r.add("⚠ Yagi is expensive"); }
            if (a.is("D3", "first")) { s -= 1.5; r.add("⚠ Yagi is not a first antenna"); }
            if (a.is("D4", "ground", "ladder")) { s -= 1; r.add("⚠ tower work requires climbing or hire-out"); }
            if (anyBand(a, "20", "15", "10")) { s += 0.5; r.add("20/15/10 are classic Yagi bands"); }
            if (anyBand(a, "80", "40") && !anyBand(a, "20", "15", "10")) { s -= 1.5; r.add("⚠ 80/40 Yagis are huge and rare"); }
            return sc(s, r);
        }),
        new Antenna("vertical", "Vertical Antenna", "09-11", "Quarter-wave / 5/8-wave / half-wave vertical with radial system. Low takeoff angle, omnidirectional.", a -> {
            List<String> r = new ArrayList<>();
            if (a.is("A3", "strict") && a.is("A4", "no")) return none();
            double s = 7;
            if (goal(a, "dx")) { s += 1; r.add("low takeoff angle good for DX"); }
            if (a.is("B7", "lawn", "salt")) { s += 1; r.add("good radial soil (" + a.s("B7") + ")"); }
            if (a.is("B7", "rocks", "drive")) { s -= 1.5; r.add("⚠ radials hard to install in your soil"); }
            if (a.is("D5", "subtle", "hidden")) { s += 0.5; r.add("lower visual profile than horizontal wire"); }
            if (a.is("D5", "stealth")) { s -= 1; r.add("⚠ still visible — flagpole hide-job needed"); }
            Double lot = lot(a);
            if (lot != null && lot >= 30) { s += 0.3; r.add("lot supports radial field"); }
            if (lot != null && lot < 30) { s -= 1; r.add("⚠ too small for full radial field"); }
            if (goal(a, "nvis")) { s -= 1.5; r.add("⚠ vertical bad for NVIS (needs high angle)"); }
            return sc(s, r);
        }),
        new Antenna("mag-loop", "Magnetic Loop", "09-14", "Small loop, ~⅛ wavelength, with high-Q tuning capacitor. Stealth-friendly; narrow bandwidth.", a -> {
            List<String> r = new ArrayList<>();
            double s = 5;
            if (a.is("D5", "stealth", "hidden")) { s += 3; r.add("excellent stealth — tiny footprint"); }
            if (a.is("A1", "mf-high", "mf-low")) { s += 2; r.add("apartment-friendly indoor option"); }
            if (a.is("A4", "attic-clear")) { s += 1.5; r.add("attic install practical"); }
            if (goal(a, "portable")) { s += 1; r.add("compact for portable"); }
            if (a.is("C4", "legal", "500")) { s -= 1; r.add("⚠ high-power mag loop needs vacuum capacitor"); }
            if (a.is("D1", "lt50")) { s -= 1; r.add("⚠ tuning capacitor often $50-100"); }
            if (a.is("D3", "first")) { s -= 1.5; r.add("⚠ tricky tuning, narrow BW"); }
            return sc(s, r);
        }));

    /** Rank every antenna that scores above zero, keep those within 70% of the best, at most five. */
    public static List<Result> rank(Answers raw) {
        Answers a = new Answers();
        for (var e : raw.entrySet()) {
            Object v = e.getValue();
            if ("__skip".equals(v) || v == null || (v instanceof String s && s.isBlank()) || (v instanceof List<?> l && l.isEmpty())) continue;
            a.put(e.getKey(), v);
        }
        List<Result> ranked = new ArrayList<>();
        for (Antenna ant : ANTENNAS) {
            Score s = ant.fit().apply(a);
            if (s != null && s.score() > 0) ranked.add(new Result(ant, s.score(), s.reasons()));
        }
        ranked.sort((x, y) -> Double.compare(y.score(), x.score()));
        double cutoff = ranked.isEmpty() ? 0 : ranked.get(0).score() * 0.7;
        return ranked.stream().filter(r -> r.score() >= cutoff).limit(5).toList();
    }
}
