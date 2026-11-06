package com.ars.fx.data;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whether a spot's frequency is open from your station right now, and when it is worth trying, from the suite's own propagation
 * estimate ({@link Propagation}) and the space weather already on hand. Everything is null (or empty) without a station location or
 * space-weather data. A verdict is kept for three minutes per place and frequency, so a map or a spot list of a few hundred spots
 * does not redo the sums on every repaint.
 */
public final class PathNow {
    private PathNow() {}

    private record Val(long minute, Propagation.Verdict verdict) {}
    private static final ConcurrentHashMap<String, Val> CACHE = new ConcurrentHashMap<>();
    private static final long KEEP_MINUTES = 3;

    /** The verdict for a spot of this call at this frequency, or null when its place or the data is unknown. */
    public static Propagation.Verdict forSpot(String call, long freqHz) {
        double[] ll = call == null ? null : Geo.of(call);
        return ll == null ? null : forPoint(ll[0], ll[1], freqHz);
    }

    /** The verdict for the far end at {@code lat, lon}, or null. */
    public static Propagation.Verdict forPoint(double lat, double lon, long freqHz) {
        try {
            if (freqHz <= 0 || !HubConfig.hasLocation()) return null;
            Propagation.Weather w = weather();
            if (w == null) return null;
            long minute = System.currentTimeMillis() / 60000;
            String key = Math.round(HubConfig.lat() * 10) + "," + Math.round(HubConfig.lon() * 10) + ">" + Math.round(lat * 10) + "," + Math.round(lon * 10) + "@" + Math.round(freqHz / 1e5);
            Val v = CACHE.get(key);
            if (v != null && minute - v.minute() < KEEP_MINUTES) return v.verdict();
            Propagation.Verdict r = Propagation.verdictNow(home(), new Propagation.Point(lat, lon), w, LocalDateTime.now(ZoneOffset.UTC), freqHz / 1e6);
            if (CACHE.size() > 4000) CACHE.clear();
            CACHE.put(key, new Val(minute, r));
            return r;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** "open now until 1800 UTC" / "next opening 1400-1800 UTC" ... or "" when it cannot be worked out. */
    public static String bestTimes(double lat, double lon, long freqHz) {
        try {
            if (freqHz <= 0 || !HubConfig.hasLocation()) return "";
            Propagation.Weather w = weather();
            if (w == null) return "";
            return Propagation.bestTimes(home(), new Propagation.Point(lat, lon), w, LocalDateTime.now(ZoneOffset.UTC), freqHz / 1e6);
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static Propagation.Point home() { return new Propagation.Point(HubConfig.lat(), HubConfig.lon()); }

    private static Propagation.Weather weather() {
        SolarData.Snapshot sd = SolarData.getInstance().snapshot();
        return sd == null ? null : Propagation.Weather.of(sd.sfi(), sd.sunspots(), sd.kIndex());
    }

    /** Whether the map and the spot lists show the verdict (Setting: J-Map ▸ Filters). On by default. */
    public static boolean shown() { return HubConfig.getBool("map.pathNow", true); }
}
