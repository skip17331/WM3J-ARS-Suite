package com.ars.fx.data;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Which NCDXF / IARU beacons you have heard, on which band, and when. Marked by a click on the map or the "I hear it" button, or
 * automatically when the rig's S-meter holds a signal on the beacon frequency while that beacon is on. Kept between sessions (in the
 * settings), shown as "7 of 18 heard on 20m", and written out as a CSV log on request. Also decides, from the clock alone, which band a
 * hopping beacon watch should be on.
 */
public final class BeaconHeard {
    private BeaconHeard() {}

    public static final String KEY = "map.beaconsHeard";
    /** S-meter reading, in dB relative to S9, that counts as a signal: S3. A stronger one is needed than the noise floor of a quiet band. */
    public static final int AUTO_DB = -36;
    /** Consecutive one-second readings above {@link #AUTO_DB} before a beacon is marked, so a click or a burst of noise is not a beacon. */
    public static final int AUTO_SAMPLES = 3;
    /** One beacon is on each band for ten seconds of a 180-second cycle. */
    public static final int CYCLE_SECONDS = 180;

    public record Entry(String call, String band, long epochSec) {}

    private static final Map<String, Entry> HEARD = new TreeMap<>();
    private static boolean loaded;
    private static String autoKey = "";
    private static int autoCount;

    private static String key(String call, String band) { return call.trim().toUpperCase(Locale.ROOT) + "@" + band.trim().toLowerCase(Locale.ROOT); }

    // ---- marking -----------------------------------------------------------------------------------------------------

    /** Marks a beacon heard on a band at {@code epochSec} (a second mark only moves the time forward). */
    public static synchronized void mark(String call, String band, long epochSec) {
        if (call == null || call.isBlank() || band == null || band.isBlank()) return;
        load();
        HEARD.merge(key(call, band), new Entry(call.trim().toUpperCase(Locale.ROOT), band.trim().toLowerCase(Locale.ROOT), epochSec),
                (old, nu) -> nu.epochSec() >= old.epochSec() ? nu : old);
        save();
    }

    /** Takes a mark back; returns whether there was one. */
    public static synchronized boolean unmark(String call, String band) {
        load();
        boolean had = HEARD.remove(key(call, band)) != null;
        if (had) save();
        return had;
    }

    /** Marks it if it is not marked, takes it back if it is. Returns whether it is marked now. */
    public static synchronized boolean toggle(String call, String band, long epochSec) {
        if (isHeard(call, band)) { unmark(call, band); return false; }
        mark(call, band, epochSec);
        return true;
    }

    public static synchronized boolean isHeard(String call, String band) { load(); return HEARD.containsKey(key(call, band)); }

    public static synchronized int count(String band) {
        load();
        int n = 0;
        for (Entry e : HEARD.values()) if (e.band().equalsIgnoreCase(band)) n++;
        return n;
    }

    public static synchronized void clear() { HEARD.clear(); loaded = true; autoKey = ""; autoCount = 0; HubConfig.set(KEY, ""); }

    // ---- by the S-meter ----------------------------------------------------------------------------------------------

    /**
     * One reading, once a second, while {@code call} is the beacon on the air on {@code band}: after {@link #AUTO_SAMPLES} readings in a row at or
     * above {@link #AUTO_DB}, in the middle of the beacon's ten-second slot, it is marked. {@code sMeterDb} is Integer.MIN_VALUE when the rig does
     * not say. Returns true on the reading that marks it.
     */
    public static synchronized boolean sample(String call, String band, int sMeterDb, int secondInSlot, long epochSec) {
        String k = key(call, band);
        boolean strong = sMeterDb != Integer.MIN_VALUE && sMeterDb >= AUTO_DB && secondInSlot >= 1 && secondInSlot <= 9;
        if (!strong) { if (!k.equals(autoKey) || sMeterDb == Integer.MIN_VALUE || sMeterDb < AUTO_DB) autoCount = 0; autoKey = k; return false; }
        if (!k.equals(autoKey)) { autoKey = k; autoCount = 0; }
        if (++autoCount >= AUTO_SAMPLES && !isHeard(call, band)) { mark(call, band, epochSec); autoCount = 0; return true; }
        return false;
    }

    // ---- showing and keeping -----------------------------------------------------------------------------------------

    /** "7 of 18 heard on 20m". */
    public static String summary(String band, int total) { return count(band) + " of " + total + " heard on " + band; }

    /** "20m 7/18 · 17m 3/18 ..." for the bands given. */
    public static String allBands(List<String> bands, int total) {
        StringBuilder sb = new StringBuilder();
        for (String b : bands) sb.append(sb.length() == 0 ? "" : "  ·  ").append(b).append(' ').append(count(b)).append('/').append(total);
        return sb.toString();
    }

    /** The log, oldest first, as CSV with a header: call, band, UTC time. */
    public static synchronized String csv() {
        load();
        List<Entry> l = new ArrayList<>(HEARD.values());
        l.sort(Comparator.comparingLong(Entry::epochSec).thenComparing(Entry::call));
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
        StringBuilder sb = new StringBuilder("call,band,utc\n");
        for (Entry e : l) sb.append(e.call()).append(',').append(e.band()).append(',').append(f.format(Instant.ofEpochSecond(e.epochSec()))).append('\n');
        return sb.toString();
    }

    // ---- hopping -------------------------------------------------------------------------------------------------------

    /** Which of {@code bands} bands a hopping watch is on at this moment: the next one every 180 seconds, so each band gets one whole cycle. */
    public static int bandAt(long epochSec, int bands) { return bands <= 0 ? 0 : (int) ((epochSec / CYCLE_SECONDS) % bands); }

    // ---- keeping between sessions --------------------------------------------------------------------------------------

    static synchronized String encode() {
        StringBuilder sb = new StringBuilder();
        for (Entry e : HEARD.values()) sb.append(sb.length() == 0 ? "" : ";").append(e.call()).append('@').append(e.band()).append('=').append(e.epochSec());
        return sb.toString();
    }

    static synchronized void decode(String s) {
        HEARD.clear(); loaded = true;
        if (s == null) return;
        for (String part : s.split(";")) {
            int at = part.indexOf('@'), eq = part.indexOf('=');
            if (at < 1 || eq < at + 2) continue;
            try { String call = part.substring(0, at), band = part.substring(at + 1, eq); long t = Long.parseLong(part.substring(eq + 1).trim());
                  HEARD.put(key(call, band), new Entry(call.toUpperCase(Locale.ROOT), band.toLowerCase(Locale.ROOT), t)); }
            catch (NumberFormatException ignored) { /* a bad entry is skipped */ }
        }
    }

    private static void load() { if (!loaded) decode(HubConfig.get(KEY, "")); }
    private static void save() { HubConfig.set(KEY, encode()); }

    /** Tests: forget what was loaded so the next read starts from the settings again. */
    static synchronized void forgetForTest() { HEARD.clear(); loaded = false; autoKey = ""; autoCount = 0; }
}
