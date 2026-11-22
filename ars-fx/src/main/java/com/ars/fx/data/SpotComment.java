package com.ars.fx.data;

import java.util.Locale;

/**
 * The comment on a spot sent from the contest page's Log + Spot: the contest's short tag and the multiplier the other station sent, so
 * "SS ENY" tells everyone in Sweepstakes the section they need. No JavaFX, so it can be tested.
 */
public final class SpotComment {
    private SpotComment() {}

    /** The tag the contest is known by on the cluster, or "" when it has none this knows. */
    static String tag(String contestId) {
        String id = contestId == null ? "" : contestId.toLowerCase(Locale.ROOT);
        if (id.startsWith("arrl_ss") || id.startsWith("arrl_sweepstakes")) return "SS";
        if (id.startsWith("arrl_field_day")) return "FD";
        if (id.startsWith("arrl_dx")) return "ARRLDX";
        if (id.startsWith("cq_wpx")) return "WPX";
        if (id.startsWith("cq_ww")) return "CQWW";
        if (id.startsWith("cq_160")) return "CQ160";
        if (id.equals("arrl_160m")) return "160M";
        if (id.equals("arrl_10m")) return "10M";
        return "";
    }

    /** "SS ENY": the tag and the section / multiplier, either of which may be missing. */
    public static String of(String contestId, String multiplier) {
        String m = multiplier == null ? "" : multiplier.trim().toUpperCase(Locale.ROOT);
        return (tag(contestId) + " " + m).trim();
    }
}
