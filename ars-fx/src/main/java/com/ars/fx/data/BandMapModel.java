package com.ars.fx.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The arithmetic of the band map: which spots to show for a band or a stretch of it, and where each label goes so that a crowded
 * stretch stays readable. Pure - no UI. The map is a frequency ruler running top (low) to bottom (high); every spot has a true position
 * on the ruler and a label position, and where labels would overlap they are pushed apart (a leader line joins the two).
 */
public final class BandMapModel {
    private BandMapModel() {}

    /** NEEDED: a new entity / band / mode, or a new multiplier in a contest. WORKED: this call is already in the log on this band. */
    public enum Kind { NEEDED, NORMAL, WORKED }

    /** One spot to place. */
    public record Mark(String call, long freqHz, String mode, String band, long arrivalMs, Kind kind) {}

    /** A spot with its place on the ruler (pixels from the top) and how faded it is (1 fresh ... {@link #MIN_ALPHA} old). */
    public record Placed(Mark mark, double trueY, double labelY, double alpha) {}

    public static final long FRESH_MS = 10 * 60_000L, MAX_AGE_MS = 60 * 60_000L;
    public static final double MIN_ALPHA = 0.35;

    /** The ends of the amateur band containing {@code seedHz}, as {lowHz, highHz}, found by asking the shared band plan; null if it is not in a band. */
    public static long[] edges(long seedHz) {
        String band = Bands.of(seedHz);
        if (band.isEmpty()) return null;
        final long step = 1_000, limit = 8_000_000;
        long lo = seedHz, hi = seedHz;
        while (seedHz - lo < limit && Bands.of(lo - step).equals(band)) lo -= step;
        while (hi - seedHz < limit && Bands.of(hi + step).equals(band)) hi += step;
        return new long[]{lo, hi};
    }

    /** 1.0 for a spot under ten minutes old, falling in a line to {@link #MIN_ALPHA} at an hour. */
    public static double alpha(long ageMs) {
        if (ageMs <= FRESH_MS) return 1.0;
        if (ageMs >= MAX_AGE_MS) return MIN_ALPHA;
        return 1.0 - (1.0 - MIN_ALPHA) * (ageMs - FRESH_MS) / (double) (MAX_AGE_MS - FRESH_MS);
    }

    /**
     * The spots to draw for [{@code loHz}, {@code hiHz}]: inside the range, not older than an hour, one per call (the latest - a skimmer
     * network spots the same station again and again), and at most {@code cap} of them - the needed ones first, then the newest.
     */
    public static List<Mark> select(List<Mark> in, long loHz, long hiHz, long nowMs, int cap) {
        Map<String, Mark> latest = new LinkedHashMap<>();
        for (Mark m : in) {
            if (m.freqHz() < loHz || m.freqHz() > hiHz || nowMs - m.arrivalMs() > MAX_AGE_MS) continue;
            String key = m.call() == null ? "" : m.call().toUpperCase(Locale.ROOT);
            Mark old = latest.get(key);
            if (old == null || m.arrivalMs() >= old.arrivalMs()) latest.put(key, m);
        }
        List<Mark> out = new ArrayList<>(latest.values());
        if (out.size() > cap) {
            out.sort(Comparator.<Mark>comparingInt(m -> m.kind() == Kind.NEEDED ? 0 : 1).thenComparing(Comparator.comparingLong(Mark::arrivalMs).reversed()));
            out = new ArrayList<>(out.subList(0, Math.max(0, cap)));
        }
        return out;
    }

    /**
     * Where each label goes on a ruler {@code height} pixels tall: at the spot's own frequency, pushed down where it would come within
     * {@code gap} pixels of the one above, and, if that runs off the bottom, pulled back up. Returned in order of frequency.
     */
    public static List<Placed> layout(List<Mark> marks, long loHz, long hiHz, double height, double gap, long nowMs) {
        List<Mark> sorted = new ArrayList<>(marks);
        sorted.sort(Comparator.comparingLong(Mark::freqHz).thenComparing(Mark::call));
        int n = sorted.size();
        double span = Math.max(1, hiHz - loHz);
        double[] trueY = new double[n], y = new double[n];
        for (int i = 0; i < n; i++) {
            trueY[i] = (sorted.get(i).freqHz() - loHz) / span * height;
            y[i] = trueY[i];
            if (i > 0 && y[i] < y[i - 1] + gap) y[i] = y[i - 1] + gap;
        }
        double bottom = Math.max(0, height - gap / 2);
        for (int i = n - 1; i >= 0; i--) {
            double limit = i == n - 1 ? bottom : y[i + 1] - gap;
            if (y[i] > limit) y[i] = limit;
        }
        List<Placed> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Mark m = sorted.get(i);
            out.add(new Placed(m, trueY[i], Math.max(0, y[i]), alpha(nowMs - m.arrivalMs())));
        }
        return out;
    }

    /** A tick spacing (Hz) that keeps ticks at least {@code minPx} apart on a ruler {@code height} pixels tall: 1, 5, 10, 25, 50, 100 or 500 kHz. */
    public static long tickStepHz(long loHz, long hiHz, double height, double minPx) {
        long[] steps = {1_000, 5_000, 10_000, 25_000, 50_000, 100_000, 500_000};
        double pxPerHz = height / Math.max(1, hiHz - loHz);
        for (long s : steps) if (s * pxPerHz >= minPx) return s;
        return steps[steps.length - 1];
    }

    /** The first tick at or above {@code loHz} for a spacing of {@code stepHz}. */
    public static long firstTick(long loHz, long stepHz) { return ((loHz + stepHz - 1) / stepHz) * stepHz; }

    /** The band, as the plan names it, that has the most spots in it - where to look when the rig is not telling us a band. Null if there are none. */
    public static String busiestBand(List<Mark> marks) {
        Map<String, Integer> n = new HashMap<>();
        for (Mark m : marks) { String b = Bands.of(m.freqHz()); if (!b.isEmpty()) n.merge(b, 1, Integer::sum); }
        return n.entrySet().stream().max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey())).map(Map.Entry::getKey).orElse(null);
    }
}
