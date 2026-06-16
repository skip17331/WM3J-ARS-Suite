package com.ars.fx.data;

import java.util.*;

/**
 * Award progress computed from the real ~/.j-log log: WAS (worked states),
 * WAC (worked continents), DXCC (worked entities). Continent/entity come from a
 * compact callsign-prefix table — good enough for the common DXCC the typical
 * operator works; unresolved prefixes fall back to their leading letters.
 */
public final class Awards {
    private Awards() {}

    /** WAS — 50 states + DC. */
    public static final String[] STATES = {
        "AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD",
        "MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC",
        "SD","TN","TX","UT","VT","VA","WA","WV","WI","WY","DC" };

    public static final String[] CONTINENTS = {"NA","SA","EU","AF","AS","OC"};

    // prefix → {entity, continent}; matched longest-prefix-first.
    private static final String[][] PFX = {
        {"K","United States","NA"},{"W","United States","NA"},{"N","United States","NA"},{"AA","United States","NA"},
        {"KH6","Hawaii","OC"},{"KL","Alaska","NA"},{"KP4","Puerto Rico","NA"},
        {"VE","Canada","NA"},{"VA","Canada","NA"},{"VO","Canada","NA"},{"VY","Canada","NA"},
        {"XE","Mexico","NA"},{"CO","Cuba","NA"},{"CM","Cuba","NA"},{"HI","Dominican Rep","NA"},{"TI","Costa Rica","NA"},
        {"PY","Brazil","SA"},{"LU","Argentina","SA"},{"CE","Chile","SA"},{"CX","Uruguay","SA"},{"HK","Colombia","SA"},
        {"YV","Venezuela","SA"},{"OA","Peru","SA"},{"HC","Ecuador","SA"},{"ZP","Paraguay","SA"},
        {"G","England","EU"},{"M","England","EU"},{"2E","England","EU"},{"GM","Scotland","EU"},{"GW","Wales","EU"},
        {"EI","Ireland","EU"},{"DL","Germany","EU"},{"DK","Germany","EU"},{"DJ","Germany","EU"},{"DF","Germany","EU"},
        {"F","France","EU"},{"I","Italy","EU"},{"EA","Spain","EU"},{"CT","Portugal","EU"},{"ON","Belgium","EU"},
        {"PA","Netherlands","EU"},{"OE","Austria","EU"},{"HB","Switzerland","EU"},{"OZ","Denmark","EU"},
        {"SM","Sweden","EU"},{"LA","Norway","EU"},{"OH","Finland","EU"},{"SP","Poland","EU"},{"OK","Czechia","EU"},
        {"S5","Slovenia","EU"},{"9A","Croatia","EU"},{"YO","Romania","EU"},{"LZ","Bulgaria","EU"},{"SV","Greece","EU"},
        {"UA","Russia","EU"},{"UR","Ukraine","EU"},{"EU","Belarus","EU"},{"ES","Estonia","EU"},{"YL","Latvia","EU"},
        {"LY","Lithuania","EU"},{"TA","Turkey","AS"},{"4X","Israel","AS"},{"JA","Japan","AS"},{"JE","Japan","AS"},
        {"JF","Japan","AS"},{"JH","Japan","AS"},{"JR","Japan","AS"},{"HL","South Korea","AS"},{"DS","South Korea","AS"},
        {"BY","China","AS"},{"BG","China","AS"},{"BA","China","AS"},{"VU","India","AS"},{"9M","Malaysia","AS"},
        {"HS","Thailand","AS"},{"YB","Indonesia","AS"},{"DU","Philippines","AS"},{"UN","Kazakhstan","AS"},
        {"VK","Australia","OC"},{"ZL","New Zealand","OC"},{"KH2","Guam","OC"},{"FK","New Caledonia","OC"},{"3D2","Fiji","OC"},
        {"ZS","South Africa","AF"},{"CN","Morocco","AF"},{"SU","Egypt","AF"},{"EA8","Canary Is","AF"},{"CT3","Madeira","AF"},
        {"5N","Nigeria","AF"},{"5Z","Kenya","AF"},{"7Q","Malawi","AF"},{"V5","Namibia","AF"},{"9J","Zambia","AF"},
    };

    public record Progress(String code, String name, int worked, int target, Set<String> workedSet, List<String> all) {}

    /** {entity, continent} for a callsign, or leading-letters fallback. */
    private static String[] resolve(String call) {
        if (call == null || call.isEmpty()) return null;
        String c = call.toUpperCase();
        // strip a portable prefix like "F/K1ABC" → use the part after the last '/' that looks like a base call,
        // but a leading "EA8/" should win — keep it simple: take the longest of the two sides.
        String[] best = null; int bestLen = 0;
        for (String[] e : PFX) {
            if (c.startsWith(e[0]) && e[0].length() > bestLen) { best = e; bestLen = e[0].length(); }
        }
        if (best != null) return new String[]{best[1], best[2]};
        return null;
    }

    public static Progress was() {
        Set<String> worked = JLogDb.workedStates();
        Set<String> hit = new HashSet<>();
        for (String s : STATES) if (worked.contains(s)) hit.add(s);
        return new Progress("WAS", "Worked All States", hit.size(), STATES.length, hit, Arrays.asList(STATES));
    }
    public static Progress wac() {
        Set<String> hit = new HashSet<>();
        for (String call : JLogDb.distinctCalls()) {
            String[] r = resolve(call);
            if (r != null) hit.add(r[1]);
        }
        return new Progress("WAC", "Worked All Continents", hit.size(), CONTINENTS.length, hit, Arrays.asList(CONTINENTS));
    }
    public static Progress dxcc() {
        Set<String> ent = new HashSet<>();
        for (String call : JLogDb.distinctCalls()) {
            String[] r = resolve(call);
            ent.add(r != null ? r[0] : leadLetters(call));
        }
        return new Progress("DXCC", "DX Century Club", ent.size(), 100, ent, null);
    }
    private static String leadLetters(String c) {
        StringBuilder sb = new StringBuilder();
        for (char ch : c.toCharArray()) { if (Character.isLetter(ch)) sb.append(ch); else if (sb.length() > 0) break; }
        return sb.length() == 0 ? c : sb.toString();
    }
}
