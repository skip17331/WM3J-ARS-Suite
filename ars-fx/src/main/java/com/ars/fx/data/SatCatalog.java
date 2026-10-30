package com.ars.fx.data;

import com.ars.fx.sat.PassPredictor;
import com.ars.fx.sat.SatellitePass;
import com.ars.fx.sat.TleSet;

import java.time.Instant;
import java.util.*;

/**
 * Every satellite we can offer for selection: the union of the radio registry and whatever orbital elements
 * are held. A satellite may have radio data only (no elements: cannot be tracked), elements only (tracked for
 * position and passes, no Doppler), or both.
 */
public final class SatCatalog {
    private SatCatalog() {}

    public record Row(int norad, String name, String type, String modes, String status,
                      long downlinkHz, long uplinkHz, boolean inRegistry, boolean hasElements, double ageDays) {
        public boolean hasRadio() { return downlinkHz > 0 || uplinkHz > 0; }
    }

    /** Best pass in the next 24 h over the station, per NORAD id. */
    public record PassInfo(int passes, double maxElDeg, long nextAosEpoch) {}

    public static List<Row> rows() {
        Map<Integer, TleSet> tles = TleStore.current();
        Map<Integer, SatRegistry.Entry> reg = SatRegistry.byNorad();
        List<Row> out = new ArrayList<>();
        for (SatRegistry.Entry e : reg.values()) {
            TleSet t = tles.get(e.norad());
            out.add(new Row(e.norad(), e.name(), e.type(), String.join(", ", e.modes()), e.status(),
                    e.downlinkHz(), e.uplinkHz(), true, t != null, t != null ? t.ageInDays() : -1));
        }
        for (TleSet t : tles.values()) {
            int id = Integer.parseInt(t.noradId);
            if (reg.containsKey(id)) continue;
            out.add(new Row(id, t.name, "", "", "", 0, 0, false, true, t.ageInDays()));
        }
        out.sort(Comparator.comparing(r -> r.name().toUpperCase()));
        return out;
    }

    /** Predict the next 24 h for every satellite with elements (blocking — run off the FX thread). */
    public static Map<Integer, PassInfo> passes24h(double lat, double lon) {
        Map<Integer, PassInfo> out = new HashMap<>();
        PassPredictor pp = new PassPredictor();
        Instant now = Instant.now();
        for (Map.Entry<Integer, TleSet> e : TleStore.current().entrySet()) {
            try {
                List<SatellitePass> ps = pp.predict(e.getValue(), lat, lon, 0, now);
                double max = 0; long first = -1;
                for (SatellitePass p : ps) { max = Math.max(max, p.maxElDeg); if (first < 0 || p.aos.getEpochSecond() < first) first = p.aos.getEpochSecond(); }
                out.put(e.getKey(), new PassInfo(ps.size(), max, first));
            } catch (Exception ignored) {}
        }
        return out;
    }
}
