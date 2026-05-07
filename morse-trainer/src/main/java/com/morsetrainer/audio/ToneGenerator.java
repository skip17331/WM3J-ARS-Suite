package com.morsetrainer.audio;

import com.morsetrainer.core.AppConfig;
import com.morsetrainer.core.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sidetone generator using Java Sound. Produces a clean sine wave with a short
 * attack/release envelope to avoid clicks at element boundaries.
 *
 * The class plays bursts on a background dispatcher; multiple play requests are
 * coalesced — calls return immediately, so the WPM engine controls timing via
 * scheduled execution rather than blocking inside this class.
 */
public class ToneGenerator implements AutoCloseable {

    private final AppConfig cfg;
    private SourceDataLine line;
    private final AtomicBoolean keyDown = new AtomicBoolean(false);
    private Thread renderThread;
    private volatile boolean running = false;

    public ToneGenerator(AppConfig cfg) { this.cfg = cfg; open(); }
    public ToneGenerator() { this(AppConfig.get()); }

    private void open() {
        try {
            AudioFormat fmt = new AudioFormat(cfg.sampleRateHz, 16, 1, true, false);
            line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt, cfg.sampleRateHz / 4);
            line.start();

            running = true;
            renderThread = new Thread(this::renderLoop, "morse-tone-renderer");
            renderThread.setDaemon(true);
            renderThread.start();
        } catch (LineUnavailableException e) {
            Logger.warn("Audio line unavailable: %s", e.getMessage());
            line = null;
        }
    }

    /** Begin emitting tone immediately. Idempotent. */
    public void keyDown() { keyDown.set(true); }
    /** Stop emitting tone immediately. Idempotent. */
    public void keyUp()   { keyDown.set(false); }

    /** Synchronous tone burst of the given duration (ms). Convenience for fixed sequences. */
    public void burst(long millis) {
        keyDown();
        try { Thread.sleep(millis); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        keyUp();
    }

    private void renderLoop() {
        // Render in 10ms blocks. Cheap enough at 44.1 kHz.
        final int blockMs = 10;
        final int blockSamples = cfg.sampleRateHz * blockMs / 1000;
        final byte[] buffer = new byte[blockSamples * 2];
        final double twoPiF = 2.0 * Math.PI * cfg.toneFrequencyHz;
        final double sr = cfg.sampleRateHz;
        final int rampSamples = (int) (cfg.rampMillis / 1000.0 * sr);

        double phase = 0.0;
        boolean wasOn = false;
        int rampPos = 0;

        while (running) {
            boolean on = keyDown.get();
            for (int i = 0; i < blockSamples; i++) {
                double envelope;
                if (on && !wasOn) { rampPos = 0; wasOn = true; }
                if (!on && wasOn) { rampPos = 0; wasOn = false; }

                if (on) {
                    envelope = rampPos < rampSamples ? (double) rampPos / rampSamples : 1.0;
                } else {
                    envelope = rampPos < rampSamples ? 1.0 - (double) rampPos / rampSamples : 0.0;
                }
                rampPos++;

                double s = Math.sin(phase) * envelope * cfg.toneVolume;
                phase += twoPiF / sr;
                if (phase > 2 * Math.PI) phase -= 2 * Math.PI;

                short pcm = (short) (s * Short.MAX_VALUE);
                buffer[i * 2]     = (byte) (pcm & 0xff);
                buffer[i * 2 + 1] = (byte) ((pcm >> 8) & 0xff);
            }
            if (line != null) line.write(buffer, 0, buffer.length);
        }
    }

    @Override
    public void close() {
        running = false;
        if (renderThread != null) renderThread.interrupt();
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
        }
    }
}
