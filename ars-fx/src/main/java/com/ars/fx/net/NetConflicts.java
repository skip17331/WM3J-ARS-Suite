package com.ars.fx.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** What this station should be told about the others: a station in another contest, or on the band and mode this one is using. Pure. */
public final class NetConflicts {
    private NetConflicts() {}

    public static List<String> warnings(NetStatus me, Collection<NetStatus> others) {
        List<String> out = new ArrayList<>();
        if (me == null) return out;
        for (NetStatus o : others) {
            if (o.station().equals(me.station())) continue;
            if (me.contest() != null && o.contest() != null && !me.contest().equals(o.contest())) {
                out.add(o.label() + " is logging a different contest (" + o.contest() + "): its QSOs are not shared");
                continue;
            }
            if (!blank(me.band()) && !blank(me.mode()) && me.band().equalsIgnoreCase(o.band()) && me.mode().equalsIgnoreCase(o.mode()))
                out.add(o.label() + " is also on " + me.band() + " " + me.mode());
        }
        return out;
    }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
