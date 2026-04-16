package com.hamradio.modem.mode;

import com.hamradio.modem.dsp.Goertzel;
import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.SignalSnapshot;

import java.util.Optional;

/**
 * RTTY decoder
 *
 * Sample-clocked asynchronous Baudot receiver:
 * - per-sample 2-tone quadrature discriminator
 * - hysteretic MARK/SPACE slicer
 * - MARK->SPACE start-bit detect
 * - exact UART-style bit-center sampling
 * - LSB-first 5-bit Baudot decode
 *
 * This replaces coarse step/window timing with sample-accurate character timing.
 */
public class RttyMode implements DigitalMode {

    private static final double DEFAULT_SAMPLE_RATE = 8000.0;

    private static final double BAUD = 45.45;
    private static final int DATA_BITS = 5;

    private static final double MARK_HZ = 2125.0;
    private static final double SPACE_HZ = 2295.0;

    private static final double CENTER_HZ = (MARK_HZ + SPACE_HZ) / 2.0;
    private static final double BANDWIDTH_HZ = 300.0;

    private static final double MIN_RMS = 0.004;
    private static final double MIN_DOMINANCE = 1.08;

    private static final double DISC_SMOOTH_ALPHA = 0.16;
    private static final double SLICER_MARK_THRESHOLD = 0.10;
    private static final double SLICER_SPACE_THRESHOLD = -0.10;

    private static final double MIN_IDLE_MARK_BITS = 0.90;
    private static final double START_CONFIRM_BITS = 0.35;

    private static final int BAD_CHARS_BEFORE_UNLOCK = 5;
    private static final int MAX_BUFFER_BEFORE_EMIT = 32;

    private static final boolean DEBUG = true;
    private static final int POWER_LOG_EVERY_FRAMES = 12;

    private boolean reverse = false;

    private boolean initialized = false;
    private double sampleRate = DEFAULT_SAMPLE_RATE;
    private double samplesPerBit = DEFAULT_SAMPLE_RATE / BAUD;

    private final DcBlocker dcBlocker = new DcBlocker();
    private final SimpleAgc agc = new SimpleAgc();
    private BiquadBandpass bandpass =
            new BiquadBandpass(DEFAULT_SAMPLE_RATE, CENTER_HZ, BANDWIDTH_HZ);

    // Per-sample discriminator oscillators
    private double markCos = 1.0;
    private double markSin = 0.0;
    private double spaceCos = 1.0;
    private double spaceSin = 0.0;

    private double markRotCos = 1.0;
    private double markRotSin = 0.0;
    private double spaceRotCos = 1.0;
    private double spaceRotSin = 0.0;

    // One-pole low-pass I/Q accumulators
    private double markI = 0.0;
    private double markQ = 0.0;
    private double spaceI = 0.0;
    private double spaceQ = 0.0;
    private double iqAlpha = 0.12;

    private double smoothedDiscriminator = 0.0;
    private boolean slicedMark = true;
    private boolean previousSlicedMark = true;

    private double currentRunSamples = 0.0;
    private double previousMarkRunSamples = 0.0;

    // UART-style character timing
    private boolean inCharacter = false;
    private int symbolIndex = 0; // 0..4 data bits, 5 stop bit
    private int currentCode = 0;
    private double sampleClock = 0.0;
    private double nextBitCenter = 0.0;

    private int consecutiveBadChars = 0;

    private boolean lettersShift = true;
    private final StringBuilder pendingText = new StringBuilder();

    private double lastMarkPower = 0.0;
    private double lastSpacePower = 0.0;
    private double lastDominance = 0.0;
    private double lastDiscriminator = 0.0;

    private long frameCounter = 0;

    @Override
    public String getName() {
        return reverse ? "RTTY (REV) [LSB]" : "RTTY [LSB]";
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        if (this.reverse != reverse) {
            this.reverse = reverse;
            resetDecoderState();
            debug("reverse=" + reverse);
        } else {
            debug("reverse unchanged=" + reverse);
        }
    }

    public double getLastMarkPower() {
        return lastMarkPower;
    }

    public double getLastSpacePower() {
        return lastSpacePower;
    }

    public double getLastDominance() {
        return lastDominance;
    }

    @Override
    public Optional<DecodeMessage> process(SignalSnapshot snapshot, long rigFrequencyHz) {
        if (snapshot == null
                || snapshot.getSamples() == null
                || snapshot.getSamples().length == 0) {
            return Optional.empty();
        }

        frameCounter++;
        initializeIfNeeded(snapshot);

        float[] samples = preprocess(snapshot.getSamples());
        double processedRms = computeRms(samples);

        if (processedRms < MIN_RMS) {
            if (DEBUG && frameCounter % POWER_LOG_EVERY_FRAMES == 0) {
                debug(String.format("low rms=%.5f peak=%.1f", processedRms, snapshot.getPeakFrequencyHz()));
            }
            return maybeEmitBufferedText(
                    rigFrequencyHz,
                    snapshot.getPeakFrequencyHz(),
                    0.0,
                    0.40
            );
        }

        // Keep Goertzel-based reporting/SNR estimation for UI consistency.
        int reportWindow = Math.max(64, (int) Math.round(samplesPerBit * 0.50));
        double totalMark = 0.0;
        double totalSpace = 0.0;
        int validWindows = 0;

        for (int start = 0; start + reportWindow <= samples.length; start += reportWindow / 2) {
            float[] window = new float[reportWindow];
            System.arraycopy(samples, start, window, 0, reportWindow);

            double markPower = Goertzel.power(window, (float) sampleRate, MARK_HZ);
            double spacePower = Goertzel.power(window, (float) sampleRate, SPACE_HZ);

            lastMarkPower = markPower;
            lastSpacePower = spacePower;

            totalMark += markPower;
            totalSpace += spacePower;

            double stronger = Math.max(markPower, spacePower);
            double weaker = Math.max(Math.min(markPower, spacePower), 1e-12);
            double dominance = stronger / weaker;
            lastDominance = dominance;

            if (dominance >= MIN_DOMINANCE) {
                validWindows++;
            }
        }

        // Sample-accurate decode core
        for (float s : samples) {
            processAudioSample(s);
        }

        if (DEBUG && frameCounter % POWER_LOG_EVERY_FRAMES == 0) {
            debug(String.format(
                    "reverse=%s rms=%.5f peak=%.1f mark=%.5f space=%.5f dom=%.3f disc=%.3f sliced=%s inCharacter=%s sampleClock=%.1f nextCenter=%.1f symbolIndex=%d",
                    reverse,
                    processedRms,
                    snapshot.getPeakFrequencyHz(),
                    lastMarkPower,
                    lastSpacePower,
                    lastDominance,
                    lastDiscriminator,
                    slicedMark ? "MARK" : "SPACE",
                    inCharacter,
                    sampleClock,
                    nextBitCenter,
                    symbolIndex
            ));
        }

        double snr = estimateSnr(totalMark, totalSpace);
        double confidence = estimateConfidence(totalMark, totalSpace, processedRms, validWindows);

        if (pendingText.length() >= MAX_BUFFER_BEFORE_EMIT
                || pendingText.indexOf("\n") >= 0
                || pendingText.indexOf("\r") >= 0) {
            return maybeEmitBufferedText(
                    rigFrequencyHz,
                    snapshot.getPeakFrequencyHz(),
                    snr,
                    confidence
            );
        }

        return Optional.empty();
    }

    private void initializeIfNeeded(SignalSnapshot snapshot) {
        if (!initialized || Math.abs(snapshot.getSampleRate() - sampleRate) > 0.01) {
            sampleRate = snapshot.getSampleRate();
            samplesPerBit = sampleRate / BAUD;
            iqAlpha = Math.min(0.25, 2.0 / Math.max(8.0, samplesPerBit * 0.5));

            initialized = true;

            resetDecoderState();
            dcBlocker.reset();
            agc.reset();
            bandpass = new BiquadBandpass(sampleRate, CENTER_HZ, BANDWIDTH_HZ);

            double markStep = 2.0 * Math.PI * MARK_HZ / sampleRate;
            double spaceStep = 2.0 * Math.PI * SPACE_HZ / sampleRate;

            markRotCos = Math.cos(markStep);
            markRotSin = Math.sin(markStep);
            spaceRotCos = Math.cos(spaceStep);
            spaceRotSin = Math.sin(spaceStep);

            debug(String.format(
                    "init sampleRate=%.1f samplesPerBit=%.3f",
                    sampleRate,
                    samplesPerBit
            ));
        }
    }

    private void resetDecoderState() {
        markCos = 1.0;
        markSin = 0.0;
        spaceCos = 1.0;
        spaceSin = 0.0;

        markI = 0.0;
        markQ = 0.0;
        spaceI = 0.0;
        spaceQ = 0.0;

        smoothedDiscriminator = 0.0;
        slicedMark = true;
        previousSlicedMark = true;

        currentRunSamples = 0.0;
        previousMarkRunSamples = 0.0;

        inCharacter = false;
        symbolIndex = 0;
        currentCode = 0;
        sampleClock = 0.0;
        nextBitCenter = 0.0;

        consecutiveBadChars = 0;

        lettersShift = true;
        pendingText.setLength(0);
    }

    private float[] preprocess(float[] input) {
        float[] out = new float[input.length];

        for (int i = 0; i < input.length; i++) {
            double x = input[i];
            x = dcBlocker.process(x);
            x = agc.process(x);
            x = bandpass.process(x);
            out[i] = (float) x;
        }

        return out;
    }

    private void processAudioSample(float sample) {
        double x = sample;

        // Complex mix to mark frequency
        double mixedMarkI = x * markCos;
        double mixedMarkQ = -x * markSin;

        // Complex mix to space frequency
        double mixedSpaceI = x * spaceCos;
        double mixedSpaceQ = -x * spaceSin;

        markI += iqAlpha * (mixedMarkI - markI);
        markQ += iqAlpha * (mixedMarkQ - markQ);
        spaceI += iqAlpha * (mixedSpaceI - spaceI);
        spaceQ += iqAlpha * (mixedSpaceQ - spaceQ);

        double markPower = (markI * markI) + (markQ * markQ);
        double spacePower = (spaceI * spaceI) + (spaceQ * spaceQ);

        double total = markPower + spacePower + 1e-12;
        double disc = (markPower - spacePower) / total;
        if (reverse) {
            disc = -disc;
        }

        lastDiscriminator = disc;
        smoothedDiscriminator += DISC_SMOOTH_ALPHA * (disc - smoothedDiscriminator);

        previousSlicedMark = slicedMark;
        if (smoothedDiscriminator >= SLICER_MARK_THRESHOLD) {
            slicedMark = true;
        } else if (smoothedDiscriminator <= SLICER_SPACE_THRESHOLD) {
            slicedMark = false;
        }

        // Run-length tracking for start qualification
        if (slicedMark == previousSlicedMark) {
            currentRunSamples += 1.0;
        } else {
            if (previousSlicedMark) {
                previousMarkRunSamples = currentRunSamples;
            }

            // MARK -> SPACE transition = candidate start bit
            if (previousSlicedMark && !slicedMark) {
                boolean enoughIdle =
                        previousMarkRunSamples >= samplesPerBit * MIN_IDLE_MARK_BITS;

                if (!inCharacter && enoughIdle) {
                    beginCharacter();
                    debug(String.format(
                            "START detected (markRun=%.1f bits=%.2f)",
                            previousMarkRunSamples,
                            previousMarkRunSamples / samplesPerBit
                    ));
                }
            }

            currentRunSamples = 1.0;
        }

        // Character timing
        if (inCharacter) {
            if (sampleClock >= nextBitCenter) {
                boolean bit = sampleBitCenter();
                onBitSample(bit);
                nextBitCenter += samplesPerBit;
            }
        }

        stepOscillators();
        sampleClock += 1.0;
    }

    private void beginCharacter() {
        inCharacter = true;
        symbolIndex = 0;
        currentCode = 0;

        // Confirm we stay SPACE briefly after the edge
        double confirm = samplesPerBit * START_CONFIRM_BITS;

        // First data bit center = 1.5 bit times after start edge,
        // plus a tiny empirical early offset.
        nextBitCenter = sampleClock + confirm + (samplesPerBit * 1.15);
    }

    private boolean sampleBitCenter() {
        // At this point slicedMark is already the current per-sample state.
        // Using the instantaneous sliced state at the center works much better
        // than coarse multi-window voting in this modem.
        return slicedMark;
    }

    private void onBitSample(boolean votedMark) {
        debug(String.format(
                "BIT vote=%s symbolIndex=%d sampleClock=%.1f nextCenter=%.1f",
                votedMark ? "MARK(1)" : "SPACE(0)",
                symbolIndex,
                sampleClock,
                nextBitCenter
        ));

        if (symbolIndex < DATA_BITS) {
            int bit = votedMark ? 1 : 0;
            currentCode |= (bit << symbolIndex);
            symbolIndex++;
            return;
        }

        boolean stopValid = votedMark;

        debug(String.format(
                "CHAR codeLSB=%d (0x%02X) stopValid=%s",
                currentCode & 0x1F,
                currentCode & 0x1F,
                stopValid
        ));

        finalizeCharacter(currentCode & 0x1F, stopValid);

        inCharacter = false;
        symbolIndex = 0;
        currentCode = 0;
        nextBitCenter = 0.0;
    }

    private void finalizeCharacter(int baudotCode, boolean stopValid) {
        if (!stopValid) {
            consecutiveBadChars++;
            debug("REJECT stop bit invalid");

            if (consecutiveBadChars >= BAD_CHARS_BEFORE_UNLOCK) {
                consecutiveBadChars = 0;
            }
            return;
        }

        consecutiveBadChars = 0;

        char decoded = decodeBaudot(baudotCode);

        if (decoded == 0) {
            debug(String.format(
                    "SHIFT code=0x%02X letters=%s",
                    baudotCode,
                    lettersShift
            ));
            return;
        }

        debug(String.format(
                "ACCEPT code=0x%02X char=%s",
                baudotCode,
                printable(decoded)
        ));

        pendingText.append(decoded);
    }

    private void stepOscillators() {
        double nMarkCos = (markCos * markRotCos) - (markSin * markRotSin);
        double nMarkSin = (markSin * markRotCos) + (markCos * markRotSin);
        markCos = nMarkCos;
        markSin = nMarkSin;

        double nSpaceCos = (spaceCos * spaceRotCos) - (spaceSin * spaceRotSin);
        double nSpaceSin = (spaceSin * spaceRotCos) + (spaceCos * spaceRotSin);
        spaceCos = nSpaceCos;
        spaceSin = nSpaceSin;
    }

    private Optional<DecodeMessage> maybeEmitBufferedText(long rigFrequencyHz,
                                                          double offsetHz,
                                                          double snr,
                                                          double confidence) {
        if (pendingText.length() == 0) {
            return Optional.empty();
        }

        String text = pendingText.toString();
        pendingText.setLength(0);

        debug("EMIT text=" + printable(text));

        return Optional.of(new DecodeMessage(
                ModeType.RTTY,
                text,
                rigFrequencyHz,
                offsetHz,
                snr,
                confidence
        ));
    }

    private double estimateSnr(double mark, double space) {
        double signal = Math.max(mark, space) + 1e-12;
        double noise = Math.min(mark, space) + 1e-12;
        return 10.0 * Math.log10(signal / noise);
    }

    private double estimateConfidence(double mark,
                                      double space,
                                      double rms,
                                      int validWindows) {
        double total = mark + space + 1e-12;
        double dominanceWeight = Math.max(mark, space) / total;
        double rmsWeight = Math.min(1.0, rms / 0.08);
        double windowWeight = Math.min(1.0, validWindows / 8.0);

        double value =
                (dominanceWeight * 0.55)
                        + (rmsWeight * 0.20)
                        + (windowWeight * 0.24);

        return Math.max(0.0, Math.min(0.99, value));
    }

    private double computeRms(float[] samples) {
        double sumSq = 0.0;
        for (float s : samples) {
            sumSq += s * s;
        }
        return Math.sqrt(sumSq / Math.max(1, samples.length));
    }

    private char decodeBaudot(int code) {
        if (code == 0x1F) {
            lettersShift = true;
            return 0;
        }

        if (code == 0x1B) {
            lettersShift = false;
            return 0;
        }

        return lettersShift ? LETTERS[code] : FIGURES[code];
    }

    private static void debug(String msg) {
        if (DEBUG) {
            System.out.println("[RTTY] " + msg);
        }
    }

    private static String printable(char c) {
        if (c == '\n') return "\\n";
        if (c == '\r') return "\\r";
        if (c == ' ') return "' '";
        return Character.toString(c);
    }

    private static String printable(String s) {
        return s.replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static final char[] LETTERS = new char[32];
    private static final char[] FIGURES = new char[32];

    static {
        for (int i = 0; i < 32; i++) {
            LETTERS[i] = 0;
            FIGURES[i] = 0;
        }

        LETTERS[0x01] = 'E';
        LETTERS[0x02] = '\n';
        LETTERS[0x03] = 'A';
        LETTERS[0x04] = ' ';
        LETTERS[0x05] = 'S';
        LETTERS[0x06] = 'I';
        LETTERS[0x07] = 'U';
        LETTERS[0x08] = '\r';
        LETTERS[0x09] = 'D';
        LETTERS[0x0A] = 'R';
        LETTERS[0x0B] = 'J';
        LETTERS[0x0C] = 'N';
        LETTERS[0x0D] = 'F';
        LETTERS[0x0E] = 'C';
        LETTERS[0x0F] = 'K';
        LETTERS[0x10] = 'T';
        LETTERS[0x11] = 'Z';
        LETTERS[0x12] = 'L';
        LETTERS[0x13] = 'W';
        LETTERS[0x14] = 'H';
        LETTERS[0x15] = 'Y';
        LETTERS[0x16] = 'P';
        LETTERS[0x17] = 'Q';
        LETTERS[0x18] = 'O';
        LETTERS[0x19] = 'B';
        LETTERS[0x1A] = 'G';
        LETTERS[0x1C] = 'M';
        LETTERS[0x1D] = 'X';
        LETTERS[0x1E] = 'V';

        FIGURES[0x01] = '3';
        FIGURES[0x02] = '\n';
        FIGURES[0x03] = '-';
        FIGURES[0x04] = ' ';
        FIGURES[0x05] = '\'';
        FIGURES[0x06] = '8';
        FIGURES[0x07] = '7';
        FIGURES[0x08] = '\r';
        FIGURES[0x09] = '$';
        FIGURES[0x0A] = '4';
        FIGURES[0x0B] = '\'';
        FIGURES[0x0C] = ',';
        FIGURES[0x0D] = '!';
        FIGURES[0x0E] = ':';
        FIGURES[0x0F] = '(';
        FIGURES[0x10] = '5';
        FIGURES[0x11] = '"';
        FIGURES[0x12] = ')';
        FIGURES[0x13] = '2';
        FIGURES[0x14] = '#';
        FIGURES[0x15] = '6';
        FIGURES[0x16] = '0';
        FIGURES[0x17] = '1';
        FIGURES[0x18] = '9';
        FIGURES[0x19] = '?';
        FIGURES[0x1A] = '&';
        FIGURES[0x1C] = '.';
        FIGURES[0x1D] = '/';
        FIGURES[0x1E] = ';';
    }

    private static final class DcBlocker {
        private static final double R = 0.995;
        private double x1 = 0.0;
        private double y1 = 0.0;

        double process(double x) {
            double y = x - x1 + (R * y1);
            x1 = x;
            y1 = y;
            return y;
        }

        void reset() {
            x1 = 0.0;
            y1 = 0.0;
        }
    }

    private static final class SimpleAgc {
        private static final double TARGET = 0.30;
        private static final double ATTACK = 0.08;
        private static final double DECAY = 0.001;
        private static final double MIN_ENV = 1e-4;
        private static final double MAX_GAIN = 25.0;

        private double env = 0.05;

        double process(double sample) {
            double abs = Math.abs(sample);

            if (abs > env) {
                env += ATTACK * (abs - env);
            } else {
                env += DECAY * (abs - env);
            }

            double gain = TARGET / Math.max(env, MIN_ENV);
            if (gain > MAX_GAIN) {
                gain = MAX_GAIN;
            }

            return sample * gain;
        }

        void reset() {
            env = 0.05;
        }
    }

    private static final class BiquadBandpass {

        private final double b0;
        private final double b1;
        private final double b2;
        private final double a1;
        private final double a2;

        private double x1 = 0.0;
        private double x2 = 0.0;
        private double y1 = 0.0;
        private double y2 = 0.0;

        BiquadBandpass(double sampleRate, double centerHz, double bandwidthHz) {
            double q = Math.max(0.1, centerHz / Math.max(1.0, bandwidthHz));
            double w0 = 2.0 * Math.PI * centerHz / sampleRate;
            double alpha = Math.sin(w0) / (2.0 * q);
            double cos = Math.cos(w0);

            double bb0 = alpha;
            double bb1 = 0.0;
            double bb2 = -alpha;

            double aa0 = 1.0 + alpha;
            double aa1 = -2.0 * cos;
            double aa2 = 1.0 - alpha;

            b0 = bb0 / aa0;
            b1 = bb1 / aa0;
            b2 = bb2 / aa0;
            a1 = aa1 / aa0;
            a2 = aa2 / aa0;
        }

        double process(double x) {
            double y =
                    b0 * x +
                    b1 * x1 +
                    b2 * x2 -
                    a1 * y1 -
                    a2 * y2;

            x2 = x1;
            x1 = x;

            y2 = y1;
            y1 = y;

            return y;
        }
    }
}
