package com.ars.fx.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The QSOs logged since ARS Suite started (this session), kept in memory so J-Map can draw an arc from the station to each one while its
 * "Session QSOs" switch is on. Nothing is saved: the list starts empty on every launch, and <b>Clear</b> on J-Map empties it.
 *
 * <p>Each QSO is placed from the grid square it was logged with when there is one, otherwise from its callsign's prefix ({@link MapTracks#resolve}); one
 * that cannot be placed stays in the count but gets no arc. A QSO imported from a file does not count: only contacts logged as they happen
 * (the log forms, the J-Map form, WSJT-X auto-log, the phone client and the contest page).
 */
public final class SessionQsos {
    private SessionQsos() {}

    /** One logged contact: who, on what band and mode, when, and where (NaN when it could not be placed). */
    public record Qso(String call, String band, String mode, long atMs, double lat, double lon) {
        public boolean placed() { return !Double.isNaN(lat) && !Double.isNaN(lon); }
    }

    private static final List<Qso> LIST = new CopyOnWriteArrayList<>();
    private static volatile Runnable listener;

    /** A QSO has just been logged. Safe to call from any thread. */
    public static void logged(String call, String grid, String band, String mode) {
        if (call == null || call.isBlank()) return;
        String c = call.trim().toUpperCase(Locale.ROOT);
        double[] ll = null;
        if (grid != null && !grid.isBlank()) ll = MapTracks.resolve(grid);          // the grid it was logged with is exact; the prefix is the country's centre
        if (ll == null) ll = MapTracks.resolve(c);
        LIST.add(new Qso(c, band == null ? "" : band.trim(), mode == null ? "" : mode.trim(), System.currentTimeMillis(),
                ll == null ? Double.NaN : ll[0], ll == null ? Double.NaN : ll[1]));
        notifyChanged();
    }

    public static List<Qso> all() { return new ArrayList<>(LIST); }
    public static int count() { return LIST.size(); }
    public static int placedCount() { int n = 0; for (Qso q : LIST) if (q.placed()) n++; return n; }
    public static void clear() { LIST.clear(); notifyChanged(); }

    /** Told whenever the list changes (on whatever thread changed it); one listener, the newest J-Map's. */
    public static void setListener(Runnable r) { listener = r; }
    private static void notifyChanged() { Runnable r = listener; if (r != null) r.run(); }
}
