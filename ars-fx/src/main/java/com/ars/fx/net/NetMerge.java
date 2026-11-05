package com.ars.fx.net;

import com.jlog.db.ContestQsoDao.Meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How two stations' logs are made one. A QSO's version is the time it last changed; when two stations disagree about a QSO the later change
 * wins, and a deletion is a change like any other (so an erase on one station is not undone by a stale copy on another). Pure.
 */
public final class NetMerge {
    private NetMerge() {}

    /** The state of one QSO at one station: when it last changed, whether that change was its deletion, and who made it. */
    public record Version(long ts, boolean deleted, String origin) {
        public static Version of(Meta m) { return m == null ? null : new Version(m.ts(), m.deleted(), m.origin() == null ? "" : m.origin()); }
    }

    /** Orders versions: later change first; at the same instant a deletion beats an edit; then the station name, so every station agrees. */
    public static int compare(Version a, Version b) {
        if (a.ts() != b.ts()) return Long.compare(a.ts(), b.ts());
        if (a.deleted() != b.deleted()) return a.deleted() ? 1 : -1;
        return a.origin().compareTo(b.origin());
    }

    /** True if a QSO arriving in {@code incoming} version should replace what this station holds ({@code local}, null if it holds nothing). */
    public static boolean incomingWins(Version local, Version incoming) {
        return local == null || compare(incoming, local) > 0;
    }

    /** Of the QSOs and deletions in {@code mine}, those the other station lacks or holds an older version of: what to send it. */
    public static List<Meta> toSend(List<Meta> mine, List<Meta> theirs) {
        Map<String, Version> have = new HashMap<>();
        for (Meta t : theirs) have.put(t.uid(), Version.of(t));
        List<Meta> out = new ArrayList<>();
        for (Meta m : mine) {
            Version t = have.get(m.uid());
            if (t == null || compare(Version.of(m), t) > 0) out.add(m);
        }
        return out;
    }
}
