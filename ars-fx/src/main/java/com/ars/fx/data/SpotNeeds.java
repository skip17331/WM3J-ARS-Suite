package com.ars.fx.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * What a DX spot would add to the log, judged the way the awards judge it: by DXCC entity, not by callsign.
 * <ul>
 *   <li><b>New entity</b> - the entity has never been worked (an all-time new one);</li>
 *   <li><b>New band</b> - the entity is worked, but not on this band;</li>
 *   <li><b>New mode</b> - worked on this band, but never in this mode group (CW / phone / digital).</li>
 * </ul>
 * A call whose entity cannot be worked out falls back to "this call on this band". The index is built from the log's
 * distinct (call, band, mode) slots, so rebuilding it is cheap.
 */
public final class SpotNeeds {
    private SpotNeeds() {}

    public enum Need {
        NONE(""), NEW_ENTITY("NEW ONE"), NEW_BAND("NEW BAND"), NEW_MODE("NEW MODE");
        public final String label;
        Need(String label) { this.label = label; }
    }

    /** One distinct slot already in the log. */
    public record Slot(String call, String band, String mode) {}

    public static final class Index {
        private final Map<String, Set<String>> bandsByEntity = new HashMap<>();
        private final Map<String, Set<String>> modesByEntity = new HashMap<>();
        private final Set<String> callBands = new HashSet<>();
        public int size() { return callBands.size(); }
        /** True if this call is already in the log on this band (any mode). */
        public boolean worked(String call, String band) { return callBands.contains(up(call) + "|" + up(band)); }
    }

    private static final Map<String, Optional<DxccInfo.Info>> RESOLVED = new ConcurrentHashMap<>();
    /** DXCC resolution with a memory: the same calls turn up again and again in a spot list. */
    public static Function<String, DxccInfo.Info> resolver() {
        return call -> RESOLVED.computeIfAbsent(call, c -> Optional.ofNullable(DxccInfo.of(c))).orElse(null);
    }

    public static String up(String s) { return s == null ? "" : s.trim().toUpperCase(Locale.ROOT); }

    public static Index build(List<Slot> slots, Function<String, DxccInfo.Info> resolve) {
        Index ix = new Index();
        for (Slot s : slots) {
            String call = up(s.call()), band = up(s.band());
            if (call.isEmpty()) continue;
            ix.callBands.add(call + "|" + band);
            DxccInfo.Info info = resolve.apply(call);
            if (info == null) continue;
            String ent = Awards.entityKey(info);
            ix.bandsByEntity.computeIfAbsent(ent, k -> new HashSet<>()).add(band);
            ix.modesByEntity.computeIfAbsent(ent, k -> new HashSet<>()).add(Awards.modeGroup(s.mode()));
        }
        return ix;
    }

    public static Need classify(Index ix, String call, String band, String mode, Function<String, DxccInfo.Info> resolve) {
        String c = up(call), b = up(band);
        if (c.isEmpty()) return Need.NONE;
        DxccInfo.Info info = resolve.apply(c);
        if (info == null) return ix.callBands.contains(c + "|" + b) ? Need.NONE : Need.NEW_BAND;      // entity unknown: call + band only
        String ent = Awards.entityKey(info);
        Set<String> bands = ix.bandsByEntity.get(ent);
        if (bands == null) return Need.NEW_ENTITY;
        if (!b.isEmpty() && !bands.contains(b)) return Need.NEW_BAND;
        Set<String> modes = ix.modesByEntity.get(ent);
        if (mode != null && !mode.isBlank() && modes != null && !modes.contains(Awards.modeGroup(mode))) return Need.NEW_MODE;
        return Need.NONE;
    }
}
