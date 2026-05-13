package com.jlog.controller;

import com.jlog.db.PriorityCallsignDao;
import com.jlog.model.DxSpot;
import com.jlog.model.PriorityCallsign;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * PrioritySpotAlertService — matches inbound DX spots against the
 * operator's priority watch list. On a match, it fires registered
 * listeners (the banner overlay) and plays an audible alert
 * (PC speakers) unless the entry is muted or its audible flag is off.
 *
 * <p>60-second per-callsign debounce so the same target spotted
 * repeatedly by the cluster doesn't continuously re-alert. The cache
 * is in-memory only — relaunching j-log resets it intentionally.
 *
 * <p>The watch-list snapshot is reloaded on demand via
 * {@link #reload()} — the management window calls this after every
 * add / remove / edit so changes take effect immediately.
 */
public final class PrioritySpotAlertService {

    private static final Logger log = LoggerFactory.getLogger(PrioritySpotAlertService.class);
    private static final PrioritySpotAlertService INSTANCE = new PrioritySpotAlertService();
    public static PrioritySpotAlertService getInstance() { return INSTANCE; }
    private PrioritySpotAlertService() {}

    /** Per-callsign last-alert timestamp to suppress flap. */
    private final Map<String, Long> lastAlertMs = new HashMap<>();
    private static final long DEBOUNCE_MS = 60_000L;

    /** Cached upper-cased callsign set for fast match check. */
    private volatile Set<String> activeSet = java.util.Collections.emptySet();

    private final CopyOnWriteArrayList<Consumer<Alert>> listeners = new CopyOnWriteArrayList<>();

    /** Re-read the watch list from the DB. Safe to call from any thread. */
    public void reload() {
        try {
            activeSet = PriorityCallsignDao.getInstance().getActiveCallsigns();
        } catch (Exception ex) {
            log.warn("Priority list reload failed: {}", ex.getMessage());
        }
    }

    /** Banner overlays register here so they can pop on match. */
    public void addListener(Consumer<Alert> l) { listeners.add(l); }
    public void removeListener(Consumer<Alert> l) { listeners.remove(l); }

    /**
     * Hand off a spot to the service. Returns true if it matched the
     * watch list (caller doesn't need the verdict — listeners and the
     * sound alert have already fired by then).
     */
    public boolean onSpot(DxSpot spot) {
        if (spot == null || spot.getDxCallsign() == null) return false;
        String key = baseCallsign(spot.getDxCallsign()).toUpperCase(Locale.ROOT);
        if (!activeSet.contains(key)) return false;

        long now = System.currentTimeMillis();
        Long last = lastAlertMs.get(key);
        if (last != null && now - last < DEBOUNCE_MS) return true; // debounced
        lastAlertMs.put(key, now);

        // Pull the full row to honour audible / banner flags
        PriorityCallsign entry = PriorityCallsignDao.getInstance().findByCallsign(key);
        if (entry == null) return false; // raced with a delete

        Alert alert = new Alert(spot, entry);
        if (entry.isBanner()) {
            // Listeners must marshal onto the FX thread themselves; we
            // don't assume they're FX-only.
            for (Consumer<Alert> l : listeners) {
                try { l.accept(alert); } catch (Exception ex) {
                    log.debug("Listener threw on alert: {}", ex.getMessage());
                }
            }
        }
        if (entry.isAudible()) {
            playAudible();
        }
        log.info("PRIORITY SPOT: {} on {} @ {} kHz",
                 spot.getDxCallsign(), spot.getBand(), spot.getFrequencyKHz());
        return true;
    }

    /** Strip /P, /M, /R, /QRP, etc. for the match. */
    private static String baseCallsign(String c) {
        if (c == null) return "";
        String t = c.trim();
        int slash = t.indexOf('/');
        if (slash < 0) return t;
        // Common operator suffixes — drop. If the slash is the
        // *prefix* indicator (e.g. K1ABC/VE), keep that root.
        String left  = t.substring(0, slash);
        String right = t.substring(slash + 1);
        // The "root" of /P, /M, /R, /QRP, /MM, /AM is the left side.
        // The "root" of /VE is the right side (less common but real).
        if (right.length() <= 3 && right.matches("[A-Za-z0-9]+")) return left;
        return left;
    }

    /**
     * Play a short two-tone alert from the default speakers. Generated
     * in-process so we don't need a bundled WAV resource — works on any
     * platform Java Sound supports.
     */
    private void playAudible() {
        Thread t = new Thread(() -> {
            try {
                AudioFormat fmt = new AudioFormat(44_100f, 16, 1, true, false);
                int totalSamples = (int) (44_100 * 0.35); // 350 ms total
                byte[] buf = new byte[totalSamples * 2];
                for (int i = 0; i < totalSamples; i++) {
                    double phase = (i < totalSamples / 2)
                        ? 2 * Math.PI * 800 * i / 44_100.0
                        : 2 * Math.PI * 600 * i / 44_100.0;
                    double env = envelope(i, totalSamples);
                    short s = (short) (Math.sin(phase) * 16000 * env);
                    buf[2*i]     = (byte) (s & 0xff);
                    buf[2*i + 1] = (byte) ((s >> 8) & 0xff);
                }
                try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
                    line.open(fmt, buf.length);
                    line.start();
                    line.write(buf, 0, buf.length);
                    line.drain();
                }
            } catch (Exception ex) {
                log.debug("Audible alert failed: {}", ex.getMessage());
            }
        }, "priority-alert-tone");
        t.setDaemon(true);
        t.start();
    }

    /** Short cosine fade-in/out to avoid clicks. */
    private static double envelope(int i, int total) {
        int ramp = Math.min(total / 8, 1500);
        if (i < ramp)             return 0.5 - 0.5 * Math.cos(Math.PI * i / ramp);
        if (i > total - ramp)     return 0.5 - 0.5 * Math.cos(Math.PI * (total - i) / ramp);
        return 1.0;
    }

    /** Container delivered to banner listeners. */
    public static final class Alert {
        public final DxSpot          spot;
        public final PriorityCallsign entry;
        public Alert(DxSpot s, PriorityCallsign e) { this.spot = s; this.entry = e; }
    }
}
