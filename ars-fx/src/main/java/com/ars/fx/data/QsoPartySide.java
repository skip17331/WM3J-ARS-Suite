package com.ars.fx.data;

import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.MultiplierLists;
import com.jlog.scoring.QsoParty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * State and province QSO parties are played from two sides. An <b>in-state</b> station is in the party's state and sends its
 * county; it works everyone (counties, other states and provinces, DX). An <b>out-of-state</b> station sends its state or province
 * and works only the party state's counties. The scoring engine decides the side from the operator's <i>sent QTH</i> - this class
 * keeps that value per contest (a county that is right for one party is wrong for the next), decides the side the way the engine
 * does, and says what to expect and what is left to work on each side.
 */
public final class QsoPartySide {
    private QsoPartySide() {}

    public enum Side { IN_STATE, OUT_OF_STATE }

    public static final Set<String> US = Set.of("AL","AK","AZ","AR","CA","CO","CT","DE","DC","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME",
            "MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT",
            "VA","WA","WV","WI","WY");
    public static final Set<String> CA = Set.of("AB","BC","LB","MB","NB","NF","NL","NS","NT","NU","ON","PE","QC","SK","YT");

    public static boolean isParty(ContestPlugin p) {
        return p != null && p.getScoringRules() != null && "qso_party".equals(p.getScoringRules().getMultiplierType());
    }
    private static ContestPlugin.QsoPartyConfig cfg(ContestPlugin p) { return isParty(p) ? p.getScoringRules().getQsoParty() : null; }

    /** The party's own state or area ("VT", "7"), or "". */
    public static String partyState(ContestPlugin p) {
        ContestPlugin.QsoPartyConfig c = cfg(p);
        return c == null || c.getStateAbbr() == null ? "" : c.getStateAbbr().trim().toUpperCase(Locale.ROOT);
    }

    // ---- the operator's sent QTH -------------------------------------------------------------------------
    private static String key(ContestPlugin p) { return "contest.sent." + p.getContestId() + ".qth"; }

    /** What this operator sends in this party: the value saved for the contest, else the shared state / province answer, else "". */
    public static String sentQth(ContestPlugin p) {
        if (!isParty(p)) return "";
        String own = HubConfig.get(key(p), "").trim();
        if (!own.isEmpty()) return own.toUpperCase(Locale.ROOT);
        return HubConfig.get("contest.sent.state_prov_sent", "").trim().toUpperCase(Locale.ROOT);
    }
    public static void saveSentQth(ContestPlugin p, String v) { HubConfig.set(key(p), v == null ? "" : v.trim().toUpperCase(Locale.ROOT)); }

    // ---- which side ----------------------------------------------------------------------------------------
    private static Set<String> countySet(ContestPlugin.QsoPartyConfig c) {
        Set<String> out = new HashSet<>();
        if (c != null && c.getInStateCounties() != null) for (String s : c.getInStateCounties()) out.add(s.trim().toUpperCase(Locale.ROOT));
        return out;
    }

    /** The side the engine will score this sent QTH as; null when nothing is set yet (or the contest is not a state party). */
    public static Side side(ContestPlugin p, String qth) {
        ContestPlugin.QsoPartyConfig c = cfg(p);
        if (c == null && !isParty(p)) return null;
        if (qth == null || qth.isBlank()) return null;
        boolean in = QsoParty.isCounty(qth, countySet(c), c == null ? 0 : c.getCountyCodeLen(), c != null && c.isCountyByExclusion());
        return in ? Side.IN_STATE : Side.OUT_OF_STATE;
    }
    public static Side side(ContestPlugin p) { return side(p, sentQth(p)); }

    // ---- what each side works and needs ----------------------------------------------------------------------
    /** The party state's county codes (empty when the app has no list for it). */
    public static List<String> counties(ContestPlugin p) {
        List<String> l = new ArrayList<>(MultiplierLists.load(p == null ? null : p.getMultiplierList()));
        if (l.isEmpty()) l.addAll(countySet(cfg(p)));
        return l;
    }

    /** Everything an entrant on this side can receive: out of state, only the party's counties; in state, those and every state, province and DX. */
    public static Set<String> receivable(ContestPlugin p, Side side) {
        Set<String> out = new HashSet<>(counties(p));
        if (side == Side.IN_STATE) { out.addAll(US); out.addAll(CA); out.add("DX"); }
        return out;
    }

    /**
     * The multipliers an entrant on this side can collect - for "left to work". Out of state: the party's counties. In state: the
     * states and provinces (and DX) the party counts for in-state entrants, plus the party's own counties where it counts those.
     * Null when it cannot be listed (no county list, or DX counted per country).
     */
    public static List<String> multUniverse(ContestPlugin p, Side side) {
        if (side == null || !isParty(p)) return null;
        ContestPlugin.QsoPartyConfig c = cfg(p);
        List<String> counties = counties(p);
        if (side == Side.OUT_OF_STATE) return counties.isEmpty() ? null : counties;
        List<String> out = new ArrayList<>();
        boolean countsCounties = c == null || c.isInStateCountsCounties();
        boolean countsStates = c == null || c.isInStateCountsStates();
        if (countsCounties) out.addAll(counties);
        if (countsStates) { out.addAll(new java.util.TreeSet<>(US)); out.addAll(new java.util.TreeSet<>(CA)); }
        if (c != null && !c.isInStateNoDxMult() && !c.isInStateCountsDxccEach()) out.add("DX");
        String own = partyState(p);
        if (c == null || !c.isInStateSelfStateMult()) out.remove(own);   // you cannot work your own state from inside it, unless the party counts it (WVQP)
        return out.isEmpty() ? null : out;
    }

    /** "IN-STATE", "OUT-OF-STATE" or "" for a short label. */
    public static String label(Side s) { return s == null ? "" : s == Side.IN_STATE ? "IN-STATE" : "OUT-OF-STATE"; }
}
