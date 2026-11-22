package com.ars.fx.data;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Where a US station really is, for the map. A call's digit only says its call area (every W7 is on the Oregon / Idaho line), so a station
 * that is not in the call history is looked up in the operator's callbook ({@link CallsignLookup}: Callook by default, or QRZ / HamQTH if
 * set up), one call at a time in the background, and the answer (its grid, else its state) is kept for the run. Until it comes the map uses
 * the call area, and {@link #onChange} runs when a new answer arrives so the map can repaint.
 */
public final class PlaceLookup {
    private PlaceLookup() {}

    private static final double[] NONE = new double[0];
    private static final Map<String, double[]> PLACED = new ConcurrentHashMap<>();   // call -> {lat, lon}, or NONE when the callbook did not know it
    private static final int QUEUE = 300;
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE), r -> {
        Thread t = new Thread(r, "place-lookup"); t.setDaemon(true); return t;
    }, new ThreadPoolExecutor.DiscardPolicy());     // a flood of new calls: the ones past the queue are asked about on a later repaint
    private static final java.util.Set<String> ASKED = ConcurrentHashMap.newKeySet();
    private static volatile long lastNotify;

    /** Run (from any thread) when a new place has been found; the map repaints. */
    public static volatile Runnable onChange;

    /** The looked-up place of a call, or null when it is not known (yet). Asks the callbook in the background the first time. */
    public static double[] get(String call) {
        if (call == null || call.isBlank()) return null;
        String c = CallHistory.baseCall(call);
        double[] hit = PLACED.get(c);
        if (hit != null) return hit == NONE ? null : new double[]{hit[0], hit[1]};
        if (Geo.isUs(c) && ASKED.add(c)) {
            try { WORKER.execute(() -> find(c)); } catch (RuntimeException e) { ASKED.remove(c); }
            if (WORKER.getQueue().remainingCapacity() == 0) ASKED.remove(c);          // dropped by the full queue: try again later
        }
        return null;
    }

    private static void find(String c) {
        double[] p = NONE;
        try {
            if (LookupConfig.anyEnabled()) {
                CallsignLookup.Result r = CallsignLookup.lookup(c);
                double[] q = r == null ? null : pointOf(r.grid(), r.state());
                if (q != null) p = q;
            }
            Thread.sleep(120);                                                          // gently on the callbook
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (Exception ignored) { /* no answer: the call area stays */ }
        PLACED.put(c, p);
        Runnable r = onChange; long now = System.currentTimeMillis();
        if (p != NONE && r != null && now - lastNotify > 800) { lastNotify = now; r.run(); }   // not a repaint per call
    }

    /** The point for a callbook answer: the centre of its grid square when it has one, else the centre of its state. Null when neither is usable. */
    public static double[] pointOf(String grid, String state) {
        if (grid != null && grid.trim().length() >= 4) {
            try { double[] g = com.jlog.util.Maidenhead.center(grid.trim()); if (g != null) return g; } catch (RuntimeException ignored) { /* bad grid */ }
        }
        return Geo.stateCentre(state == null ? null : state.trim().toUpperCase(Locale.ROOT));
    }
}
