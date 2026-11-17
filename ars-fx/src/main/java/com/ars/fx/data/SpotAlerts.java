package com.ars.fx.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Spot alerts (J-Hub ▸ Setup ▸ Data ▸ DX Cluster): tell the operator when a spot arrives for a call on their watch list, or one the
 * log still needs (the NEW ONE / NEW BAND / NEW MODE tag the cluster card shows). Off by default.
 *
 * <p>{@link #check} is pure: given the spot list, how each spot is tagged, the settings and the clock, it returns the spots to alert on
 * and records them in {@code seen}, so the same call on the same band alerts at most once every {@link #REPEAT_MS} however often the
 * cluster repeats the spot. Spots older than {@link #FRESH_MS} (the list a reconnect hands back) never alert.
 */
public final class SpotAlerts {
    private SpotAlerts() {}

    public static final String KEY_ON = "alerts.enabled", KEY_NEEDED = "alerts.needed", KEY_WATCH = "alerts.watch", KEY_SOUND = "alerts.sound";
    /** The same call + band alerts again only after this long. */
    static final long REPEAT_MS = 30 * 60_000L;
    /** Only spots that arrived this recently alert. */
    static final long FRESH_MS = 10 * 60_000L;

    /** What to alert on: the watch list (exact calls, or prefixes ending in '*') and whether needed spots alert. */
    public record Settings(boolean enabled, boolean needed, List<String> watch) {
        public static Settings fromConfig() {
            return new Settings(HubConfig.getBool(KEY_ON, false), HubConfig.getBool(KEY_NEEDED, true), parse(HubConfig.get(KEY_WATCH, "")));
        }
    }

    /** One alert: the spot and why ("watch list", or the need tag such as "NEW ONE"). */
    public record Alert(ClusterClient.Spot spot, String reason) {}

    /** "VP8*, 3y0j ; K1ABC" → [VP8*, 3Y0J, K1ABC]. */
    public static List<String> parse(String watch) {
        List<String> out = new ArrayList<>();
        if (watch == null) return out;
        for (String w : watch.split("[,;\\s]+")) {
            String t = w.trim().toUpperCase(Locale.ROOT);
            if (!t.isEmpty() && !t.equals("*")) out.add(t);
        }
        return out;
    }

    /** True if {@code call} is on the watch list: an exact call (portable suffixes ignored), or a prefix pattern "VP8*". */
    public static boolean watched(String call, List<String> watch) {
        if (call == null || watch == null || watch.isEmpty()) return false;
        String c = call.trim().toUpperCase(Locale.ROOT);
        String base = c.contains("/") ? baseOf(c) : c;
        for (String w : watch) {
            if (w.endsWith("*")) { String p = w.substring(0, w.length() - 1); if (!p.isEmpty() && (c.startsWith(p) || base.startsWith(p))) return true; }
            else if (w.equals(c) || w.equals(base)) return true;
        }
        return false;
    }

    /** The longest '/'-separated part, which is the base call in K1ABC/P, VP2E/K1ABC and the like. */
    private static String baseOf(String c) {
        String best = "";
        for (String p : c.split("/")) if (p.length() > best.length()) best = p;
        return best;
    }

    /**
     * The spots in {@code spots} to alert on now. {@code needTag} gives a spot's need tag ("" when not needed). Records each alerted
     * call + band in {@code seen} (forgetting entries older than {@link #REPEAT_MS}).
     */
    public static List<Alert> check(List<ClusterClient.Spot> spots, Function<ClusterClient.Spot, String> needTag, Settings s,
                                    long now, Map<String, Long> seen) {
        List<Alert> out = new ArrayList<>();
        if (s == null || !s.enabled() || spots == null) return out;
        seen.values().removeIf(t -> now - t > REPEAT_MS);
        for (ClusterClient.Spot sp : spots) {
            if (sp == null || sp.call() == null) continue;
            if (now - sp.arrivalMs() > FRESH_MS) continue;
            String key = sp.call().trim().toUpperCase(Locale.ROOT) + "|" + (sp.band() == null ? "" : sp.band());
            if (seen.containsKey(key)) continue;
            String reason = null;
            if (watched(sp.call(), s.watch())) reason = "watch list";
            else if (s.needed()) {
                String t = needTag == null ? "" : needTag.apply(sp);
                if (t != null && !t.isBlank()) reason = t;
            }
            if (reason == null) continue;
            seen.put(key, now);
            out.add(new Alert(sp, reason));
        }
        return out;
    }

    /** The app's running state: what has already alerted. */
    private static final Map<String, Long> SEEN = new HashMap<>();
    /** {@link #check} against the app's settings, clock and remembered alerts. Call on the FX thread (the cluster listener's thread). */
    public static List<Alert> checkNow(List<ClusterClient.Spot> spots, Function<ClusterClient.Spot, String> needTag) {
        return check(spots, needTag, Settings.fromConfig(), System.currentTimeMillis(), SEEN);
    }

    /** A short two-tone chime on a background thread (never on the FX thread); silently does nothing without an audio device. */
    public static void chime() {
        if (!HubConfig.getBool(KEY_SOUND, true)) return;
        Thread t = new Thread(() -> {
            try {
                float rate = 22050f;
                javax.sound.sampled.AudioFormat fmt = new javax.sound.sampled.AudioFormat(rate, 16, 1, true, false);
                byte[] pcm = tone(rate, new double[]{880, 1320}, 0.12);
                try (javax.sound.sampled.SourceDataLine line = javax.sound.sampled.AudioSystem.getSourceDataLine(fmt)) {
                    line.open(fmt); line.start(); line.write(pcm, 0, pcm.length); line.drain();
                }
            } catch (Exception | LinkageError ignored) { /* no sound device: the card still shows */ }
        }, "spot-alert-chime");
        t.setDaemon(true); t.start();
    }

    /** 16-bit mono PCM: each frequency for {@code seconds}, with a short fade in and out so it does not click. */
    static byte[] tone(float rate, double[] freqs, double seconds) {
        int n = (int) (rate * seconds);
        byte[] out = new byte[freqs.length * n * 2];
        int fade = Math.max(1, n / 10), o = 0;
        for (double f : freqs) for (int i = 0; i < n; i++) {
            double env = Math.min(1, Math.min(i, n - 1 - i) / (double) fade);
            short v = (short) (Math.sin(2 * Math.PI * f * i / rate) * 0.3 * env * Short.MAX_VALUE);
            out[o++] = (byte) v; out[o++] = (byte) (v >> 8);
        }
        return out;
    }
}
