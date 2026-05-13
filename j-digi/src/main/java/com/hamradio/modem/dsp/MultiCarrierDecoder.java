package com.hamradio.modem.dsp;

import com.hamradio.modem.mode.CwMode;
import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.SignalSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Per-channel CW decoder fleet driven by {@link LocalSkimmer} detections.
 *
 * <p>For each carrier the skimmer reports, this class owns:
 * <ul>
 *   <li>A {@link CwMode} instance pinned to that carrier (AFC off — see
 *       {@link CwMode#setLockedCarrier(double)}).</li>
 *   <li>A rolling text buffer of recently-decoded characters.</li>
 *   <li>A last-seen timestamp used to reap channels that drop off.</li>
 * </ul>
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Skimmer snapshot arrives → {@link #processFrame} matches every
 *       peak to an existing channel within {@link #CHANNEL_MERGE_HZ},
 *       refreshing its {@code lastSeenMs}; unmatched peaks spawn a new
 *       channel up to {@link #MAX_CHANNELS}.</li>
 *   <li>The current audio frame is then handed to every active
 *       channel's decoder — each one bandpass-filters around its own
 *       carrier, so up to {@code MAX_CHANNELS} CW QSOs can be decoded
 *       in parallel.</li>
 *   <li>Channels with no peak hit for {@link #CHANNEL_REAP_MS} are
 *       removed.</li>
 * </ol>
 *
 * <p>Phase A scope (this commit): decode runs and decoded text is
 * emitted to a per-channel buffer plus an optional listener. Callsign
 * extraction, confidence scoring, and {@code SPOT} publishing are
 * follow-ups (Phase B / C).
 */
public class MultiCarrierDecoder {

    private static final Logger log = LoggerFactory.getLogger(MultiCarrierDecoder.class);

    // ── Tunables ──────────────────────────────────────────────────────
    /** Peak frequencies within this distance of an existing channel are
     *  treated as the same channel rather than spawning a duplicate.
     *  Matches {@code LocalSkimmer.peakSeparationHz}. */
    static final double CHANNEL_MERGE_HZ = 80.0;
    /** Channels with no peak hit for this many milliseconds get reaped. */
    static final long   CHANNEL_REAP_MS  = 5_000;
    /** Hard cap so a wild-noise frame can't spawn an unbounded fleet of
     *  decoders. Matches {@code LocalSkimmer.maxPeaks}. */
    static final int    MAX_CHANNELS     = 16;
    /** Per-channel rolling text buffer cap. */
    private static final int TEXT_BUFFER_LIMIT = 256;

    /** Phase B: confidence threshold for a scored callsign to be
     *  promoted to a {@link ScoredCallsign} listener event. Bare
     *  matches with no repetition or context score 0.40, so 0.65
     *  keeps the bar at "needs at least repetition OR context". */
    static final double SCORE_EMIT_THRESHOLD = 0.65;

    private final double sampleRate;
    private final List<ActiveChannel> channels = new ArrayList<>();

    /** Optional listener fired on every decoded text fragment. */
    private volatile Consumer<DecodedFragment> listener = f -> {};
    /** Phase B listener — fires when a scored callsign clears the
     *  threshold and hasn't been emitted on this channel before. */
    private volatile Consumer<ScoredCallsign> callsignListener = c -> {};

    public MultiCarrierDecoder(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    /** Register an optional consumer for decoded fragments. Useful for
     *  the j-hub bridge (Phase C) or the j-digi UI debug pane. */
    public void setListener(Consumer<DecodedFragment> l) {
        this.listener = (l != null) ? l : f -> {};
    }

    /** Register an optional consumer for scored callsign promotions.
     *  Phase C wires this to a SPOT-publishing path with 5-minute
     *  dedup; until then it's null-safe. */
    public void setCallsignListener(Consumer<ScoredCallsign> l) {
        this.callsignListener = (l != null) ? l : c -> {};
    }

    /** Snapshot of currently-tracked channels (for tests / UI). */
    public List<ChannelView> getChannels() {
        List<ChannelView> out = new ArrayList<>(channels.size());
        for (ActiveChannel ch : channels) {
            out.add(new ChannelView(ch.centerHz, ch.lastSnrDb, ch.lastSeenMs,
                                    ch.recentText.toString()));
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Drive one audio frame through every active channel.
     *
     * @param samples raw audio for this frame
     * @param snap    most recent skimmer snapshot (may be null when the
     *                skimmer hasn't produced one yet — channels keep
     *                decoding off their pinned carriers until reaped)
     * @param rigHz   current rig frequency, forwarded to CwMode's
     *                emitted DecodeMessages for ADIF-style spot tagging
     */
    public void processFrame(float[] samples, LocalSkimmer.Snapshot snap, long rigHz) {
        long now = (snap != null) ? snap.timestampMs : System.currentTimeMillis();

        // 1. Match peaks → spawn / refresh channels
        if (snap != null) {
            for (LocalSkimmer.Peak peak : snap.peaks) {
                ActiveChannel matched = findChannel(peak.freqHz);
                if (matched != null) {
                    matched.lastSeenMs = now;
                    matched.lastSnrDb  = peak.snrDb;
                } else if (channels.size() < MAX_CHANNELS) {
                    ActiveChannel ch = new ActiveChannel(peak.freqHz, peak.snrDb, now);
                    channels.add(ch);
                    log.debug("Skimmer: spawn channel @ {} Hz (SNR {} dB)",
                              Math.round(peak.freqHz), Math.round(peak.snrDb));
                }
            }
        }

        // 2. Decode this frame on every active channel.
        if (samples != null && samples.length > 0 && !channels.isEmpty()) {
            double rms = computeRms(samples);
            for (ActiveChannel ch : channels) {
                // Per-channel SignalSnapshot: AFC is off in the decoder
                // so peakFrequencyHz isn't consulted, but we pass the
                // pinned carrier anyway so any future code that reads
                // it sees the channel's own frequency.
                SignalSnapshot perChannel = new SignalSnapshot(
                    samples, /*magnitudes=*/null, rms, ch.centerHz, sampleRate);
                Optional<DecodeMessage> decoded = ch.decoder.process(perChannel, rigHz);
                if (decoded.isPresent()) {
                    String text = decoded.get().getText();
                    if (text == null || text.isEmpty()) continue;
                    appendTextCapped(ch.recentText, text);
                    log.info("Skimmer ch={}Hz: {}", Math.round(ch.centerHz), text.trim());
                    listener.accept(new DecodedFragment(ch.centerHz, ch.lastSnrDb, text, now));
                    scoreAndEmit(ch, now);
                }
            }
        }

        // 3. Reap stale channels.
        Iterator<ActiveChannel> it = channels.iterator();
        while (it.hasNext()) {
            ActiveChannel ch = it.next();
            if (now - ch.lastSeenMs > CHANNEL_REAP_MS) {
                log.debug("Skimmer: reap channel @ {} Hz (idle {} ms)",
                          Math.round(ch.centerHz), now - ch.lastSeenMs);
                it.remove();
            }
        }
    }

    /** Phase B: run the scorer over a channel's rolling text and
     *  promote any candidate clearing {@link #SCORE_EMIT_THRESHOLD}
     *  that we haven't already emitted on this channel. */
    private void scoreAndEmit(ActiveChannel ch, long now) {
        List<CallsignScorer.Candidate> candidates =
            CallsignScorer.score(ch.recentText.toString());
        for (CallsignScorer.Candidate cand : candidates) {
            if (cand.confidence < SCORE_EMIT_THRESHOLD) continue;
            if (!ch.emittedCalls.add(cand.callsign)) continue;
            log.info("Skimmer scored {} @ {} Hz conf={} ctx={} reps={}",
                     cand.callsign, Math.round(ch.centerHz),
                     String.format("%.2f", cand.confidence),
                     cand.context, cand.repetitions);
            callsignListener.accept(new ScoredCallsign(
                ch.centerHz, ch.lastSnrDb,
                cand.callsign, cand.confidence,
                cand.context, cand.repetitions, now));
        }
    }

    /** Find the existing channel within CHANNEL_MERGE_HZ, or null. */
    private ActiveChannel findChannel(double freqHz) {
        ActiveChannel best = null;
        double bestDist = CHANNEL_MERGE_HZ;
        for (ActiveChannel ch : channels) {
            double dist = Math.abs(ch.centerHz - freqHz);
            if (dist < bestDist) { bestDist = dist; best = ch; }
        }
        return best;
    }

    private static void appendTextCapped(StringBuilder sb, String s) {
        sb.append(s);
        if (sb.length() > TEXT_BUFFER_LIMIT) {
            sb.delete(0, sb.length() - TEXT_BUFFER_LIMIT);
        }
    }

    private static double computeRms(float[] samples) {
        double sum = 0.0;
        for (float s : samples) sum += (double) s * s;
        return Math.sqrt(sum / samples.length);
    }

    // ─────────────────────────────────────────────────────────────────
    // Internal per-channel state
    // ─────────────────────────────────────────────────────────────────

    private static final class ActiveChannel {
        final double centerHz;
        final CwMode decoder;
        long   lastSeenMs;
        double lastSnrDb;
        final StringBuilder recentText = new StringBuilder();
        /** Callsigns this channel has already promoted via the
         *  callsignListener. Prevents the scorer from re-emitting on
         *  every subsequent decode chunk that keeps the same call in
         *  the rolling text buffer. */
        final Set<String> emittedCalls = new HashSet<>();

        ActiveChannel(double centerHz, double snrDb, long now) {
            this.centerHz   = centerHz;
            this.lastSeenMs = now;
            this.lastSnrDb  = snrDb;
            this.decoder    = new CwMode();
            this.decoder.setLockedCarrier(centerHz);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Public read-only views
    // ─────────────────────────────────────────────────────────────────

    /** Immutable view of one channel's current state (for tests / UI). */
    public static final class ChannelView {
        public final double centerHz;
        public final double snrDb;
        public final long   lastSeenMs;
        public final String recentText;
        public ChannelView(double centerHz, double snrDb, long lastSeenMs, String recentText) {
            this.centerHz   = centerHz;
            this.snrDb      = snrDb;
            this.lastSeenMs = lastSeenMs;
            this.recentText = recentText;
        }
    }

    /** Emitted to the optional listener every time a channel decodes
     *  one or more characters. Phase C will consume these via the
     *  callsign listener instead — this one stays for debug panes. */
    public static final class DecodedFragment {
        public final double centerHz;
        public final double snrDb;
        public final String text;
        public final long   timestampMs;
        public DecodedFragment(double centerHz, double snrDb, String text, long timestampMs) {
            this.centerHz    = centerHz;
            this.snrDb       = snrDb;
            this.text        = text;
            this.timestampMs = timestampMs;
        }
    }

    /**
     * Phase B promotion event — a callsign was extracted from a
     * channel's decoded text and scored above the emit threshold.
     * Phase C will subscribe to this and translate each one into a
     * {@code SPOT} message published to j-hub with {@code
     * source:"LOCAL_SKIMMER"} (with a time-based dedup window so the
     * same call doesn't get spotted twice in five minutes).
     */
    public static final class ScoredCallsign {
        public final double centerHz;
        public final double snrDb;
        public final String callsign;
        public final double confidence;
        /** Why the scorer promoted: "CQ", "DE", "TU", "RST", "QRZ",
         *  "RPT" (repeated), or "BARE" (only when threshold tolerates it). */
        public final String context;
        public final int    repetitions;
        public final long   timestampMs;

        public ScoredCallsign(double centerHz, double snrDb,
                              String callsign, double confidence,
                              String context, int repetitions, long timestampMs) {
            this.centerHz    = centerHz;
            this.snrDb       = snrDb;
            this.callsign    = callsign;
            this.confidence  = confidence;
            this.context     = context;
            this.repetitions = repetitions;
            this.timestampMs = timestampMs;
        }
    }
}
