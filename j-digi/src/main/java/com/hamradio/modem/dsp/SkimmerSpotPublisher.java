package com.hamradio.modem.dsp;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Phase C: turns scored-callsign promotions from
 * {@link MultiCarrierDecoder} into broker-format {@code SPOT}
 * messages, with a per-(callsign, kHz) dedup window so the same
 * call doesn't keep re-spotting every time it cycles through the
 * scorer's emit threshold.
 *
 * <p>Wire shape (matches {@code com.hamradio.jhub.model.Spot}):
 * <pre>
 *   {
 *     "type":      "SPOT",
 *     "spotter":   "&lt;myCall&gt; (skimmer)",
 *     "spotted":   "W1AW",
 *     "frequency": 14025500,
 *     "mode":      "CW",
 *     "source":    "LOCAL_SKIMMER",
 *     "snrDb":     18,
 *     "comment":   "Local skimmer · CQ · reps=2 · conf=0.95",
 *     "timestamp": "2026-05-13T12:34:56.789Z"
 *   }
 * </pre>
 *
 * <p>j-hub's {@code MessageRouter} caches the spot and re-broadcasts;
 * J-Map / J-Log render it the same way they render cluster spots,
 * with the {@code source} field letting the UI colour skimmer spots
 * distinctly. DXCC / bearing / country enrichment is handled on the
 * j-hub side (see the inbound-SPOT branch in {@code MessageRouter}).
 *
 * <p>The class is fully testable: the publish sink, rig-frequency
 * supplier, and station-callsign supplier are all injected so the
 * unit tests can fake them.
 */
public class SkimmerSpotPublisher {

    private static final Logger log = LoggerFactory.getLogger(SkimmerSpotPublisher.class);

    /** No same-(callsign, kHz) re-spot within this window. */
    static final long DEDUP_WINDOW_MS  = 5 * 60 * 1000L;
    /** How often to sweep stale entries out of the dedup map (cheap;
     *  bounded by callsign turnover within DEDUP_WINDOW_MS). */
    static final long PRUNE_INTERVAL_MS = 60 * 1000L;

    private final Consumer<JsonObject> publish;
    private final LongSupplier         rigHzSupplier;
    private final Supplier<String>     myCallSupplier;

    private final Map<String, Long> lastSpotMs = new HashMap<>();
    private long lastPruneMs = 0L;

    /** Spots emitted; useful for the j-digi status pane and tests. */
    private long spotsEmittedTotal = 0L;

    public SkimmerSpotPublisher(Consumer<JsonObject> publish,
                                LongSupplier rigHzSupplier,
                                Supplier<String> myCallSupplier) {
        this.publish        = publish;
        this.rigHzSupplier  = rigHzSupplier;
        this.myCallSupplier = myCallSupplier;
    }

    /** Total number of SPOTs this publisher has emitted since startup. */
    public long getSpotsEmittedTotal() { return spotsEmittedTotal; }

    /** Size of the live dedup map (for tests / diagnostics). */
    int dedupTableSize() { return lastSpotMs.size(); }

    /**
     * MultiCarrierDecoder callsign-listener entry point. Builds a SPOT
     * JSON, dedups, and publishes if the dedup check passes.
     */
    public void onScoredCallsign(MultiCarrierDecoder.ScoredCallsign sc) {
        if (sc == null || sc.callsign == null || sc.callsign.isEmpty()) return;

        long rigHz = rigHzSupplier.getAsLong();
        if (rigHz <= 0) {
            log.debug("Skimmer SPOT skipped — no rig frequency yet ({}@{}Hz)",
                      sc.callsign, Math.round(sc.centerHz));
            return;
        }

        // USB convention: rig dial = LSB carrier, audio offset adds.
        // Same math the existing publishLocalSkimmer uses.
        long rfHz = rigHz + Math.round(sc.centerHz);

        // Dedup key: callsign + kHz bucket. Round to nearest kHz so
        // tiny audio-offset wiggle doesn't break the dedup.
        long khz = rfHz / 1000L;
        String dedupKey = sc.callsign + "@" + khz;
        long now = sc.timestampMs;

        Long prev = lastSpotMs.get(dedupKey);
        if (prev != null && now - prev < DEDUP_WINDOW_MS) {
            log.debug("Skimmer SPOT deduped — {} @ {} kHz last spotted {} s ago",
                      sc.callsign, khz, (now - prev) / 1000);
            return;
        }
        lastSpotMs.put(dedupKey, now);

        JsonObject msg = new JsonObject();
        msg.addProperty("type",      "SPOT");
        msg.addProperty("spotter",   spotterTag());
        msg.addProperty("spotted",   sc.callsign);
        msg.addProperty("frequency", rfHz);
        msg.addProperty("mode",      "CW");
        msg.addProperty("source",    "LOCAL_SKIMMER");
        msg.addProperty("snrDb",     (int) Math.round(sc.snrDb));
        msg.addProperty("comment",   String.format(
            "Local skimmer · %s · reps=%d · conf=%.2f",
            sc.context, sc.repetitions, sc.confidence));
        msg.addProperty("timestamp", Instant.ofEpochMilli(now).toString());

        publish.accept(msg);
        spotsEmittedTotal++;
        log.info("Skimmer SPOT {} @ {} kHz ({} dB, ctx={})",
                 sc.callsign, khz, (int) Math.round(sc.snrDb), sc.context);

        maybePrune(now);
    }

    private String spotterTag() {
        String my = myCallSupplier.get();
        if (my == null || my.isBlank()) my = "LOCAL";
        return my + " (skimmer)";
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
        if (removed > 0) log.debug("Skimmer SPOT dedup pruned {} stale entries", removed);
    }
}
