package com.hamradio.modem.mode;

import com.hamradio.modem.dsp.Goertzel;
import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.SignalSnapshot;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AX.25 packet receiver.
 *
 * Bell 202 AFSK on the wire:
 *   - Mark  = 1200 Hz, Space = 2200 Hz, 1200 baud
 *   - NRZI encoding (0 = transition, 1 = no transition)
 *   - HDLC framing: 0x7E flag, bit stuffing after 5 ones, 7+ ones = abort
 *   - FCS = CRC-16-CCITT, init 0xFFFF, poly 0x1021 (reversed 0x8408),
 *     final XOR 0xFFFF, transmitted low-byte first
 *
 * Each successfully FCS-checked frame is emitted as one DecodeMessage in
 * APRS-ish text form: SRC>DEST[,DIGI*,...] [ctrl=XX pid=XX] :info
 */
public class Ax25Mode implements DigitalMode {

    private static final double DEFAULT_SAMPLE_RATE = 8000.0;

    private static final double MARK_HZ  = 1200.0;
    private static final double SPACE_HZ = 2200.0;
    private static final double BAUD     = 1200.0;

    private static final double CENTER_HZ    = (MARK_HZ + SPACE_HZ) / 2.0;
    private static final double BANDWIDTH_HZ = 1400.0;

    private static final double MIN_RMS = 0.004;

    private static final double DISC_SMOOTH_ALPHA  = 0.45;
    private static final double SLICER_HIGH        = 0.05;
    private static final double SLICER_LOW         = -0.05;
    private static final double PHASE_NUDGE        = 0.30;

    private static final int    MAX_FRAME_BYTES    = 330;
    private static final int    MIN_FRAME_BYTES    = 17;   // 7+7+1+2
    private static final int    MAX_PENDING_CHARS  = 4096;

    private static final boolean DEBUG = false;

    private boolean initialized = false;
    private double sampleRate = DEFAULT_SAMPLE_RATE;
    private double samplesPerBit = DEFAULT_SAMPLE_RATE / BAUD;

    private final DcBlocker dcBlocker = new DcBlocker();
    private final SimpleAgc agc = new SimpleAgc();
    private BiquadBandpass bandpass =
            new BiquadBandpass(DEFAULT_SAMPLE_RATE, CENTER_HZ, BANDWIDTH_HZ);

    private double markCos = 1.0, markSin = 0.0;
    private double spaceCos = 1.0, spaceSin = 0.0;
    private double markRotCos = 1.0, markRotSin = 0.0;
    private double spaceRotCos = 1.0, spaceRotSin = 0.0;

    private double markI = 0.0, markQ = 0.0;
    private double spaceI = 0.0, spaceQ = 0.0;
    private double iqAlpha = 0.3;

    private double smoothedDisc = 0.0;
    private boolean slicedMark = true;
    private boolean prevSlicedMark = true;

    private double bitPhase = 0.0;

    private boolean lastNrziLevel = true;

    private int hdlcShift = 0;
    private int onesCount = 0;
    private boolean inFrame = false;
    private int byteAccum = 0;
    private int bitInByte = 0;
    private final ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();

    private final StringBuilder pendingText = new StringBuilder();

    private double lastMarkPower = 0.0;
    private double lastSpacePower = 0.0;
    private int framesDecoded = 0;
    private int framesFcsFailed = 0;

    @Override
    public String getName() {
        return "AX.25";
    }

    public int getFramesDecoded() { return framesDecoded; }
    public int getFramesFcsFailed() { return framesFcsFailed; }

    @Override
    public Optional<DecodeMessage> process(SignalSnapshot snapshot, long rigFrequencyHz) {
        if (snapshot == null
                || snapshot.getSamples() == null
                || snapshot.getSamples().length == 0) {
            return Optional.empty();
        }

        initializeIfNeeded(snapshot);

        float[] samples = preprocess(snapshot.getSamples());
        double rms = computeRms(samples);

        // Keep mark/space power for UI reporting even when squelched.
        lastMarkPower  = Goertzel.power(samples, (float) sampleRate, MARK_HZ);
        lastSpacePower = Goertzel.power(samples, (float) sampleRate, SPACE_HZ);

        if (rms < MIN_RMS) {
            return maybeEmit(rigFrequencyHz, snapshot.getPeakFrequencyHz());
        }

        for (float s : samples) {
            processSample(s);
        }

        return maybeEmit(rigFrequencyHz, snapshot.getPeakFrequencyHz());
    }

    private void initializeIfNeeded(SignalSnapshot snapshot) {
        if (initialized && Math.abs(snapshot.getSampleRate() - sampleRate) < 0.01) {
            return;
        }
        sampleRate = snapshot.getSampleRate();
        samplesPerBit = sampleRate / BAUD;
        iqAlpha = Math.min(0.5, 4.0 / Math.max(8.0, samplesPerBit));

        dcBlocker.reset();
        agc.reset();
        bandpass = new BiquadBandpass(sampleRate, CENTER_HZ, BANDWIDTH_HZ);

        double markStep  = 2.0 * Math.PI * MARK_HZ  / sampleRate;
        double spaceStep = 2.0 * Math.PI * SPACE_HZ / sampleRate;
        markRotCos  = Math.cos(markStep);
        markRotSin  = Math.sin(markStep);
        spaceRotCos = Math.cos(spaceStep);
        spaceRotSin = Math.sin(spaceStep);

        markCos = 1.0; markSin = 0.0;
        spaceCos = 1.0; spaceSin = 0.0;
        markI = markQ = spaceI = spaceQ = 0.0;
        smoothedDisc = 0.0;
        slicedMark = true;
        prevSlicedMark = true;
        bitPhase = 0.0;
        lastNrziLevel = true;
        resetFrameState();

        initialized = true;
        debug(String.format("init sampleRate=%.1f samplesPerBit=%.3f", sampleRate, samplesPerBit));
    }

    private float[] preprocess(float[] input) {
        float[] out = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            double x = dcBlocker.process(input[i]);
            x = agc.process(x);
            x = bandpass.process(x);
            out[i] = (float) x;
        }
        return out;
    }

    private void processSample(float sample) {
        double x = sample;

        double mixI_m = x * markCos;
        double mixQ_m = -x * markSin;
        double mixI_s = x * spaceCos;
        double mixQ_s = -x * spaceSin;

        markI  += iqAlpha * (mixI_m - markI);
        markQ  += iqAlpha * (mixQ_m - markQ);
        spaceI += iqAlpha * (mixI_s - spaceI);
        spaceQ += iqAlpha * (mixQ_s - spaceQ);

        double mp = markI*markI + markQ*markQ;
        double sp = spaceI*spaceI + spaceQ*spaceQ;
        double total = mp + sp + 1e-12;
        double disc = (mp - sp) / total;
        smoothedDisc += DISC_SMOOTH_ALPHA * (disc - smoothedDisc);

        prevSlicedMark = slicedMark;
        if (smoothedDisc > SLICER_HIGH) slicedMark = true;
        else if (smoothedDisc < SLICER_LOW) slicedMark = false;

        // Phase ticks at one symbol per samplesPerBit samples. When the slicer
        // transitions, snap phase toward 0 so the next firing lands one half-bit
        // after the edge — i.e. mid-symbol.
        if (slicedMark != prevSlicedMark) {
            double half = samplesPerBit / 2.0;
            double err = bitPhase - half;
            if (err > half) err -= samplesPerBit;
            if (err < -half) err += samplesPerBit;
            bitPhase -= PHASE_NUDGE * err;
        }

        bitPhase += 1.0;
        if (bitPhase >= samplesPerBit) {
            bitPhase -= samplesPerBit;
            sampleBit();
        }

        stepOscillators();
    }

    private void sampleBit() {
        boolean level = slicedMark;
        int dataBit = (level == lastNrziLevel) ? 1 : 0;
        lastNrziLevel = level;
        processHdlcBit(dataBit);
    }

    private void processHdlcBit(int bit) {
        // HDLC is LSB-first on the wire. Track the most recent 8 bits with
        // LSB = oldest, bit 7 = newest, so the bit stream that just produced
        // a flag matches 0x7E directly.
        hdlcShift = ((hdlcShift >> 1) | (bit << 7)) & 0xFF;

        if (hdlcShift == 0x7E) {
            // Flag boundary. Try to close any in-progress frame, then start
            // a new collection window. The 0x7E byte itself is never added to
            // the frame buffer because the destuff/byte machinery is bypassed
            // when we hit the flag pattern.
            if (inFrame && frameBuffer.size() > 0) {
                tryFinalizeFrame();
            }
            inFrame = true;
            frameBuffer.reset();
            byteAccum = 0;
            bitInByte = 0;
            onesCount = 0;
            return;
        }

        if (!inFrame) {
            return;
        }

        if (bit == 1) {
            onesCount++;
            if (onesCount >= 7) {
                // Seven consecutive ones inside a frame = abort.
                resetFrameState();
                return;
            }
        } else {
            if (onesCount == 5) {
                // Stuffed zero following five ones — drop it.
                onesCount = 0;
                return;
            }
            onesCount = 0;
        }

        byteAccum |= (bit << bitInByte);
        bitInByte++;
        if (bitInByte == 8) {
            frameBuffer.write(byteAccum);
            byteAccum = 0;
            bitInByte = 0;
            if (frameBuffer.size() > MAX_FRAME_BYTES) {
                resetFrameState();
            }
        }
    }

    private void resetFrameState() {
        inFrame = false;
        frameBuffer.reset();
        byteAccum = 0;
        bitInByte = 0;
        onesCount = 0;
    }

    private void tryFinalizeFrame() {
        // The partial byteAccum/bitInByte state at flag time holds the leading
        // bits of the flag pattern itself — those are not data and are simply
        // discarded. Only completed bytes (already written to the buffer)
        // belong to the frame; CRC will catch any misalignment.
        byte[] data = frameBuffer.toByteArray();
        if (data.length < MIN_FRAME_BYTES) {
            return;
        }

        int receivedFcs = (data[data.length - 2] & 0xFF)
                        | ((data[data.length - 1] & 0xFF) << 8);
        int computedFcs = computeFcs(data, 0, data.length - 2);

        if (computedFcs != receivedFcs) {
            framesFcsFailed++;
            debug(String.format("FCS fail len=%d got=%04X want=%04X",
                    data.length, receivedFcs, computedFcs));
            return;
        }

        String parsed = parseAx25(data, data.length - 2);
        if (parsed == null) {
            return;
        }

        framesDecoded++;
        if (pendingText.length() + parsed.length() + 1 > MAX_PENDING_CHARS) {
            pendingText.setLength(0);
        }
        pendingText.append(parsed).append('\n');
        debug("DECODED " + parsed);
    }

    private static int computeFcs(byte[] data, int off, int len) {
        int crc = 0xFFFF;
        int end = off + len;
        for (int i = off; i < end; i++) {
            int b = data[i] & 0xFF;
            for (int j = 0; j < 8; j++) {
                int bit = (b >> j) & 1;
                int lsb = (crc & 1) ^ bit;
                crc >>= 1;
                if (lsb != 0) {
                    crc ^= 0x8408;
                }
            }
        }
        return crc ^ 0xFFFF;
    }

    private static String parseAx25(byte[] data, int len) {
        int idx = 0;
        List<String> addresses = new ArrayList<>();
        List<Boolean> hBits = new ArrayList<>();
        boolean lastAddr = false;

        for (int i = 0; i < 10; i++) {
            if (idx + 7 > len) return null;
            StringBuilder call = new StringBuilder();
            for (int c = 0; c < 6; c++) {
                int ch = (data[idx + c] & 0xFF) >> 1;
                if (ch >= 0x20 && ch <= 0x7E && ch != ' ') {
                    call.append((char) ch);
                }
            }
            int ssidByte = data[idx + 6] & 0xFF;
            int ssid = (ssidByte >> 1) & 0x0F;
            lastAddr = (ssidByte & 0x01) != 0;
            boolean h = (ssidByte & 0x80) != 0;  // C/R on dest/src, "has-been-repeated" on digis
            String full = call.toString();
            if (full.isEmpty()) return null;
            if (ssid != 0) full = full + "-" + ssid;
            addresses.add(full);
            hBits.add(h);
            idx += 7;
            if (lastAddr) break;
        }
        if (!lastAddr) return null;
        if (idx >= len) return null;
        if (addresses.size() < 2) return null;

        int control = data[idx++] & 0xFF;

        StringBuilder result = new StringBuilder();
        // Source > Destination, then comma-joined digipeater path with "*" marking
        // the last hop that has already repeated.
        result.append(addresses.get(1)).append('>').append(addresses.get(0));
        for (int i = 2; i < addresses.size(); i++) {
            result.append(',').append(addresses.get(i));
            if (hBits.get(i)) result.append('*');
        }
        result.append(String.format(" [ctrl=%02X", control));

        // UI frame (control low 2 bits = 11) carries a PID byte + info.
        // I-frame (low bit = 0) and S-frame (low 2 bits = 01) also carry PID/info
        // in some encodings; for v2.0 only U/I carry PID. Be permissive.
        boolean isU = (control & 0x03) == 0x03;
        boolean isI = (control & 0x01) == 0x00;
        if ((isU || isI) && idx < len) {
            int pid = data[idx++] & 0xFF;
            result.append(String.format(" pid=%02X]", pid));
            if (idx < len) {
                byte[] infoBytes = new byte[len - idx];
                System.arraycopy(data, idx, infoBytes, 0, infoBytes.length);
                String info = new String(infoBytes, StandardCharsets.ISO_8859_1);
                // Drop trailing CR/LF/NUL only; preserve interior printable bytes.
                int end = info.length();
                while (end > 0) {
                    char c = info.charAt(end - 1);
                    if (c == '\r' || c == '\n' || c == 0) end--;
                    else break;
                }
                if (end > 0) {
                    result.append(' ').append(':').append(info, 0, end);
                }
            }
        } else {
            result.append(']');
        }
        return result.toString();
    }

    private Optional<DecodeMessage> maybeEmit(long rigFrequencyHz, double offsetHz) {
        if (pendingText.length() == 0) {
            return Optional.empty();
        }
        String text = pendingText.toString();
        pendingText.setLength(0);
        double snr = estimateSnr();
        return Optional.of(new DecodeMessage(
                ModeType.AX25,
                text,
                rigFrequencyHz,
                offsetHz,
                snr,
                0.95));
    }

    private double estimateSnr() {
        double s = Math.max(lastMarkPower, lastSpacePower) + 1e-12;
        double n = Math.min(lastMarkPower, lastSpacePower) + 1e-12;
        return 10.0 * Math.log10(s / n);
    }

    private static double computeRms(float[] samples) {
        double sumSq = 0.0;
        for (float s : samples) sumSq += s * s;
        return Math.sqrt(sumSq / Math.max(1, samples.length));
    }

    private void stepOscillators() {
        double nMc = markCos * markRotCos - markSin * markRotSin;
        double nMs = markSin * markRotCos + markCos * markRotSin;
        markCos = nMc; markSin = nMs;

        double nSc = spaceCos * spaceRotCos - spaceSin * spaceRotSin;
        double nSs = spaceSin * spaceRotCos + spaceCos * spaceRotSin;
        spaceCos = nSc; spaceSin = nSs;
    }

    private static void debug(String msg) {
        if (DEBUG) System.out.println("[AX25] " + msg);
    }

    private static final class DcBlocker {
        private static final double R = 0.995;
        private double x1 = 0.0, y1 = 0.0;
        double process(double x) {
            double y = x - x1 + R * y1;
            x1 = x; y1 = y;
            return y;
        }
        void reset() { x1 = 0; y1 = 0; }
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
            if (abs > env) env += ATTACK * (abs - env);
            else env += DECAY * (abs - env);
            double gain = TARGET / Math.max(env, MIN_ENV);
            if (gain > MAX_GAIN) gain = MAX_GAIN;
            return sample * gain;
        }
        void reset() { env = 0.05; }
    }

    private static final class BiquadBandpass {
        private final double b0, b1, b2, a1, a2;
        private double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        BiquadBandpass(double sampleRate, double centerHz, double bandwidthHz) {
            double q = Math.max(0.1, centerHz / Math.max(1.0, bandwidthHz));
            double w0 = 2.0 * Math.PI * centerHz / sampleRate;
            double alpha = Math.sin(w0) / (2.0 * q);
            double cos = Math.cos(w0);
            double bb0 = alpha, bb1 = 0.0, bb2 = -alpha;
            double aa0 = 1.0 + alpha;
            double aa1 = -2.0 * cos;
            double aa2 = 1.0 - alpha;
            b0 = bb0 / aa0; b1 = bb1 / aa0; b2 = bb2 / aa0;
            a1 = aa1 / aa0; a2 = aa2 / aa0;
        }
        double process(double x) {
            double y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1; x1 = x;
            y2 = y1; y1 = y;
            return y;
        }
    }
}
