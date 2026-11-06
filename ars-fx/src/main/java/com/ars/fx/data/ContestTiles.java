package com.ars.fx.data;

import com.jlog.model.QsoRecord;
import com.jlog.scoring.ContestScore;
import com.jlog.scoring.DxccResolver;
import com.jlog.util.CallsignRegion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The extra score tiles a contest's layout asks for ({@code tiles} in {@link ContestLayout}), worked out from the contest log and
 * the score. Each tile is {@code {label, value}}. A tile that does not apply to the log yet shows "-", so the number of tiles never
 * changes and the page can update them in place.
 *
 * <p>Kinds: {@code qsosByBand}, {@code multsByBand}, {@code countries} (DXCC entities), {@code zones} (of 40), {@code prefixes}
 * (WPX), {@code states} (states / provinces worked), {@code sections}, {@code mults} (everything that counts as a multiplier),
 * {@code dupes}.
 */
public final class ContestTiles {
    private ContestTiles() {}

    public static final List<String> KINDS = List.of("qsosByBand", "multsByBand", "countries", "zones", "prefixes", "states", "sections", "mults", "dupes");
    private static final List<String> BANDS = List.of("160m", "80m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m", "2m", "70cm");

    /** One tile per requested kind, in the order asked. */
    public static List<String[]> compute(List<String> kinds, List<QsoRecord> qsos, ContestScore score, Set<String> workedMults) {
        List<String[]> out = new ArrayList<>();
        if (kinds == null) return out;
        for (String k : kinds) out.add(tile(k == null ? "" : k.trim(), qsos == null ? List.of() : qsos, score, workedMults == null ? Set.of() : workedMults));
        return out;
    }

    private static String[] tile(String kind, List<QsoRecord> qsos, ContestScore score, Set<String> worked) {
        switch (kind) {
            case "qsosByBand": {
                Map<String, Integer> n = new LinkedHashMap<>();
                for (QsoRecord q : qsos) if (!q.isDupe()) n.merge(band(q.getBand()), 1, Integer::sum);
                return new String[]{"QSOS BY BAND", byBand(n)};
            }
            case "multsByBand": {
                Map<String, Integer> n = new LinkedHashMap<>();
                if (score != null) for (Map.Entry<String, List<String>> e : score.workedByBand().entrySet()) if (!e.getValue().isEmpty()) n.put(band(e.getKey()), e.getValue().size());
                return new String[]{"MULTS BY BAND", byBand(n)};
            }
            case "countries": {
                Set<String> s = new TreeSet<>();
                for (QsoRecord q : qsos) if (!q.isDupe() && q.getCallsign() != null) {
                    String e = DxccResolver.getInstance().entityOf(q.getCallsign().trim().toUpperCase(Locale.ROOT));
                    if (e != null && !e.isBlank()) s.add(e);
                }
                return new String[]{"COUNTRIES", count(s.size())};
            }
            case "zones":
                return new String[]{"ZONES", score == null || score.zonesWorked().isEmpty() ? "-" : score.zonesWorked().size() + " / 40"};
            case "prefixes": {
                Set<String> s = new TreeSet<>();
                for (QsoRecord q : qsos) if (!q.isDupe() && q.getCallsign() != null) {
                    String p = CallsignRegion.wpxPrefix(q.getCallsign().trim().toUpperCase(Locale.ROOT));
                    if (p != null && !p.isBlank()) s.add(p);
                }
                return new String[]{"PREFIXES", count(s.size())};
            }
            case "states":    return new String[]{"STATES / PROV", count(worked.size())};
            case "sections":  return new String[]{"SECTIONS", count(worked.size())};
            case "mults":     return new String[]{"MULTS WORKED", count(worked.size())};
            case "dupes": {
                long d = qsos.stream().filter(QsoRecord::isDupe).count();
                return new String[]{"DUPES", String.valueOf(d)};
            }
            default:          return new String[]{kind.toUpperCase(Locale.ROOT), "?"};
        }
    }

    private static String count(int n) { return n == 0 ? "-" : String.valueOf(n); }

    private static String band(String b) { return b == null ? "" : b.trim().toLowerCase(Locale.ROOT); }

    /** "20:130  40:55  80:12", in band order, without the "m"; "-" when empty. */
    private static String byBand(Map<String, Integer> n) {
        if (n.isEmpty()) return "-";
        List<String> keys = new ArrayList<>();
        for (String b : BANDS) if (n.containsKey(b)) keys.add(b);
        for (String b : n.keySet()) if (!keys.contains(b)) keys.add(b);
        StringBuilder sb = new StringBuilder();
        for (String b : keys) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(b.endsWith("m") && !b.endsWith("cm") ? b.substring(0, b.length() - 1) : b).append(':').append(n.get(b));
        }
        return sb.toString();
    }
}
