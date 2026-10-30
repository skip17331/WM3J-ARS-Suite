package com.ars.fx.data;

import com.hamradio.modem.ModemService;

/**
 * Process-wide holder for the real J-Digi modem engine ({@link ModemService}).
 * Constructed lazily and started once on a background thread; the surface binds
 * its decode / spectrum / status listeners. On a host without an audio device
 * the capture start fails gracefully and posts to the decode log, so the UI still
 * works — it just won't decode off-air.
 *
 * <p>The modem's PTT follows the J-Hub rig settings: {@code rig.ptt} (VOX keys nothing; CAT, RTS and DTR all go
 * through Hamlib, whose daemon does the keying) and the {@code rig.rigctldHost} / {@code rig.rigctldPort} daemon
 * endpoint. It is applied when the modem is created and again whenever one of those settings changes. The rig's
 * dial frequency and mode ({@link RigClient}) are passed on too, with 0 Hz while the rig is not connected.
 */
public final class DigiEngine {
    private DigiEngine() {}

    private static volatile ModemService svc;
    private static boolean triedCreate, started;

    /** The shared modem service, or null if it couldn't be constructed in this environment. */
    public static synchronized ModemService get() {
        if (!triedCreate) {
            triedCreate = true;
            try { svc = new ModemService(); }
            catch (Throwable t) { svc = null; }
            if (svc != null) {
                syncPtt();
                HubConfig.addChangeListener(key -> { if ("*".equals(key) || key.startsWith("rig.")) syncPtt(); });
                // the modem needs the dial frequency and mode (band-plan caption, "Rig freq" readout, decoders)
                RigClient.getInstance().addListener(st -> {
                    ModemService m = svc;
                    if (m != null) m.setRigState(st.connected() ? st.freqHz() : 0L, st.mode());
                });
            }
        }
        return svc;
    }

    /** Push the J-Hub PTT method and rigctld endpoint into the modem. Not synchronized: it runs from
     *  {@link HubConfig}'s change callback, which holds the config lock, and {@link #get()} holds this class's. */
    static void syncPtt() {
        ModemService svc = DigiEngine.svc;
        if (svc == null) return;
        String ptt = HubConfig.get("rig.ptt", "CAT").trim();
        String method = ptt.equalsIgnoreCase("VOX") ? "VOX" : "HAMLIB";
        int port = 0;
        try { port = Integer.parseInt(HubConfig.get("rig.rigctldPort", "4532").trim()); } catch (NumberFormatException ignored) { }
        svc.applyPttSettings(method, HubConfig.get("rig.rigctldHost", "127.0.0.1"), port);
    }

    /** Start audio capture once (off the UI thread). ars-fx is itself the hub, so the modem's legacy hub link is skipped. */
    public static synchronized void start() {
        ModemService s = get();
        if (s == null || started) return;
        started = true;
        Thread t = new Thread(() -> { try { s.startupEmbedded(); } catch (Throwable ignored) { } }, "digi-modem");
        t.setDaemon(true);
        t.start();
    }

    public static boolean available() { return get() != null; }
}
