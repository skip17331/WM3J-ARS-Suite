package com.ars.fx.data;

import com.hamradio.modem.ModemService;

/**
 * Process-wide holder for the real J-Digi modem engine ({@link ModemService}).
 * Constructed lazily and started once on a background thread; the surface binds
 * its decode / spectrum / status listeners. On a host without an audio device
 * (or with no hub) {@code startupSafe()} fails gracefully and posts to the decode
 * log, so the UI still works — it just won't decode off-air.
 */
public final class DigiEngine {
    private DigiEngine() {}

    private static ModemService svc;
    private static boolean triedCreate, started;

    /** The shared modem service, or null if it couldn't be constructed in this environment. */
    public static synchronized ModemService get() {
        if (!triedCreate) {
            triedCreate = true;
            try { svc = new ModemService(); }
            catch (Throwable t) { svc = null; }
        }
        return svc;
    }

    /** Start audio capture + hub connection once (off the UI thread). */
    public static synchronized void start() {
        ModemService s = get();
        if (s == null || started) return;
        started = true;
        Thread t = new Thread(() -> { try { s.startupSafe(); } catch (Throwable ignored) { } }, "digi-modem");
        t.setDaemon(true);
        t.start();
    }

    public static boolean available() { return get() != null; }
}
