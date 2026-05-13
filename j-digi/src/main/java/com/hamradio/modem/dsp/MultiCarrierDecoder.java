package com.hamradio.modem.dsp;

import com.hamradio.modem.mode.CwMode;
import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.SignalSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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

    private final double sampleRate;
    private final List<ActiveChannel> channels = new ArrayList<>();

    /** Optional listener fired on every decoded text fragment. */
    private volatile Consumer<DecodedFragment> listener = f -> {};

    public MultiCarrierDecoder(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    /** Register an optional consumer for decoded fragments. Useful for
     *  the j-hub bridge (Phase C) or the j-digi UI debug pane. */
    public void setListener(Consumer<DecodedFragment> l) {
        this.listener = (l != null) ? l : f -> {};
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
     *  one or more characters. Phase B / C will consume these. */
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
}
