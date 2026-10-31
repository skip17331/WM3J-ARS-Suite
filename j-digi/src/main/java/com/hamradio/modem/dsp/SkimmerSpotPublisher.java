package com.hamradio.modem.dsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Turns the callsigns {@link MultiCarrierDecoder} promotes into {@link LocalSpot}s for the host application, with a
 * per-(callsign, kHz) dedup window so the same call does not keep re-spotting every time it cycles through the scorer's
 * emit threshold. A spot needs the rig's dial frequency to turn the audio offset into a radio frequency, so nothing is
 * produced until the host has told the modem where the rig is tuned.
 *
 * <p>The sink and the rig-frequency supplier are injected, so the class is tested without audio or a rig.
 */
public class SkimmerSpotPublisher {

    private static final Logger log = LoggerFactory.getLogger(SkimmerSpotPublisher.class);

    /** A callsign heard by the local skimmer. {@code frequencyHz} is the radio frequency (dial + audio offset). */
    public record LocalSpot(String callsign, long frequencyHz, int snrDb, String context, int repetitions,
                            double confidence, long timestampMs) {}

    /** No same-(callsign, kHz) re-spot within this window. */
    static final long DEDUP_WINDOW_MS  = 5 * 60 * 1000L;
    /** How often to sweep stale entries out of the dedup map (cheap; bounded by callsign turnover). */
    static final long PRUNE_INTERVAL_MS = 60 * 1000L;

    private final Consumer<LocalSpot> sink;
    private final LongSupplier rigHzSupplier;

    private final Map<String, Long> lastSpotMs = new HashMap<>();
    private long lastPruneMs = 0L;
    private long spotsEmittedTotal = 0L;

    public SkimmerSpotPublisher(Consumer<LocalSpot> sink, LongSupplier rigHzSupplier) {
        this.sink = sink;
        this.rigHzSupplier = rigHzSupplier;
    }

    /** Total number of spots emitted since startup. */
    public long getSpotsEmittedTotal() { return spotsEmittedTotal; }

    /** Size of the live dedup map (for tests / diagnostics). */
    int dedupTableSize() { return lastSpotMs.size(); }

    /** {@link MultiCarrierDecoder} callsign-listener entry point: dedup, then hand a spot to the sink. */
    public synchronized void onScoredCallsign(MultiCarrierDecoder.ScoredCallsign sc) {
        if (sc == null || sc.callsign == null || sc.callsign.isEmpty()) return;

        long rigHz = rigHzSupplier.getAsLong();
        if (rigHz <= 0) {
            log.debug("Skimmer spot skipped — no rig frequency yet ({}@{}Hz)", sc.callsign, Math.round(sc.centerHz));
            return;
        }

        // USB convention: the dial is the suppressed carrier, the audio offset adds.
        long rfHz = rigHz + Math.round(sc.centerHz);

        // Dedup key: callsign + kHz bucket, so a few Hz of audio-offset wiggle does not break the dedup.
        long khz = rfHz / 1000L;
        String dedupKey = sc.callsign + "@" + khz;
        long now = sc.timestampMs;

        Long prev = lastSpotMs.get(dedupKey);
        if (prev != null && now - prev < DEDUP_WINDOW_MS) {
            log.debug("Skimmer spot deduped — {} @ {} kHz last spotted {} s ago", sc.callsign, khz, (now - prev) / 1000);
            return;
        }
        lastSpotMs.put(dedupKey, now);

        sink.accept(new LocalSpot(sc.callsign, rfHz, (int) Math.round(sc.snrDb), sc.context, sc.repetitions,
                sc.confidence, now));
        spotsEmittedTotal++;
        log.info("Skimmer spot {} @ {} kHz ({} dB, ctx={})", sc.callsign, khz, (int) Math.round(sc.snrDb), sc.context);

        maybePrune(now);
    }

    private void maybePrune(long now) {
        if (now - lastPruneMs < PRUNE_INTERVAL_MS) return;
        lastPruneMs = now;
        Iterator<Map.Entry<String, Long>> it = lastSpotMs.entrySet().iterator();
        int removed = 0;
        while (it.hasNext()) {
            if (now - it.next().getValue() > DEDUP_WINDOW_MS) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) log.debug("Skimmer spot dedup pruned {} stale entries", removed);
    }
}
