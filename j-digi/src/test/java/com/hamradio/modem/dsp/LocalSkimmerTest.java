package com.hamradio.modem.dsp;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional tests for the multi-channel CW detector. Each test builds
 * a synthetic magnitude spectrum (no FFT, no audio engine) and asserts
 * the skimmer picks the expected number / locations of peaks.
 */
class LocalSkimmerTest {

    private static final int   FFT_SIZE     = 1024;
    private static final float SAMPLE_RATE  = 8000f;
    private static final double BIN_HZ      = SAMPLE_RATE / FFT_SIZE;

    /** Add a CW-shaped narrow peak into the magnitude array, centred
     *  at the given frequency with the given peak magnitude. Width is
     *  ~2 bins, matching how a real CW carrier sits in the FFT. */
    private static void addCwPeak(double[] mag, double freqHz, double amplitude) {
        int centreBin = (int) Math.round(freqHz / BIN_HZ);
        for (int b = -3; b <= 3; b++) {
            int idx = centreBin + b;
            if (idx < 0 || idx >= mag.length) continue;
            // Bell curve, ~1.5-bin half-width
            double w = Math.exp(-(b * b) / 2.0);
            mag[idx] += amplitude * w;
        }
    }

    /** Returns the latest snapshot the skimmer emits after N frames. */
    private static LocalSkimmer.Snapshot runFrames(LocalSkimmer s, double[] mag, int frames) {
        AtomicReference<LocalSkimmer.Snapshot> last = new AtomicReference<>();
        s.setListener(last::set);
        for (int i = 0; i < frames; i++) s.process(mag.clone());
        return last.get();
    }

    @Test
    void disabledByDefault() {
        LocalSkimmer s = new LocalSkimmer(FFT_SIZE, SAMPLE_RATE);
        assertFalse(s.isEnabled());
        AtomicReference<LocalSkimmer.Snapshot> seen = new AtomicReference<>();
        s.setListener(seen::set);
        s.process(new double[FFT_SIZE / 2]);
        assertNull(seen.get(), "skimmer must emit nothing while disabled");
    }

    @Test
    void emitsEmptySnapshotWhenBandIsQuiet() {
        LocalSkimmer s = new LocalSkimmer(FFT_SIZE, SAMPLE_RATE);
        s.setEnabled(true);
        // Quiet band — uniform low noise
        double[] mag = new double[FFT_SIZE / 2];
        for (int i = 0; i < mag.length; i++) mag[i] = 0.001;
        LocalSkimmer.Snapshot snap = runFrames(s, mag, 50);
        assertNotNull(snap);
        assertTrue(snap.peaks.isEmpty(), "quiet band should yield zero peaks");
    }

    @Test
    void detectsSingleCwSignal() {
        LocalSkimmer s = new LocalSkimmer(FFT_SIZE, SAMPLE_RATE);
        s.setEnabled(true);

        double[] mag = new double[FFT_SIZE / 2];
        for (int i = 0; i < mag.length; i++) mag[i] = 0.001; // floor
        addCwPeak(mag, 700.0, 0.5);

        // Run enough frames for the noise floor to settle below the peak
        LocalSkimmer.Snapshot snap = runFrames(s, mag, 100);
        assertNotNull(snap);
        assertEquals(1, snap.peaks.size(), "should detect exactly one carrier");
        assertEquals(700.0, snap.peaks.get(0).freqHz, BIN_HZ * 2,
                "detected frequency off");
        assertTrue(snap.peaks.get(0).snrDb > 12.0,
                "detected SNR should clear threshold");
    }

    @Test
    void detectsMultipleCwSignalsRankedBySnr() {
        LocalSkimmer s = new LocalSkimmer(FFT_SIZE, SAMPLE_RATE);
        s.setEnabled(true);

        double[] mag = new double[FFT_SIZE / 2];
        for (int i = 0; i < mag.length; i++) mag[i] = 0.001;
        addCwPeak(mag, 500.0, 0.20);   // weakest
        addCwPeak(mag, 800.0, 0.80);   // strongest
        addCwPeak(mag, 1100.0, 0.40);  // middle

        LocalSkimmer.Snapshot snap = runFrames(s, mag, 100);
        assertNotNull(snap);
        assertEquals(3, snap.peaks.size(), "should detect 3 carriers");
        // Highest SNR first
        assertEquals(800.0, snap.peaks.get(0).freqHz, BIN_HZ * 2);
        assertEquals(1100.0, snap.peaks.get(1).freqHz, BIN_HZ * 2);
        assertEquals(500.0,  snap.peaks.get(2).freqHz, BIN_HZ * 2);
    }

    @Test
    void rejectsWidebandInterference() {
        LocalSkimmer s = new LocalSkimmer(FFT_SIZE, SAMPLE_RATE);
        s.setEnabled(true);

        double[] mag = new double[FFT_SIZE / 2];
        for (int i = 0; i < mag.length; i++) mag[i] = 0.001;
        // 250-Hz-wide blob centred at 900 Hz — too wide for CW
        int centre = (int) (900.0 / BIN_HZ);
        int halfWidth = (int) (125.0 / BIN_HZ);
        for (int b = -halfWidth; b <= halfWidth; b++) mag[centre + b] += 0.4;

        LocalSkimmer.Snapshot snap = runFrames(s, mag, 100);
        assertNotNull(snap);
        assertTrue(snap.peaks.isEmpty(),
                "wideband signal must not be treated as CW; got " + snap.peaks.size());
    }

    @Test
    void dedupesCloselySpacedPeaks() {
        LocalSkimmer s = new LocalSkimmer(FFT_SIZE, SAMPLE_RATE);
        s.setEnabled(true);

        double[] mag = new double[FFT_SIZE / 2];
        for (int i = 0; i < mag.length; i++) mag[i] = 0.001;
        // Two CW peaks 30 Hz apart — should collapse to one
        addCwPeak(mag, 700.0, 0.50);
        addCwPeak(mag, 730.0, 0.45);

        LocalSkimmer.Snapshot snap = runFrames(s, mag, 100);
        assertNotNull(snap);
        assertEquals(1, snap.peaks.size(),
                "two carriers within 80 Hz must dedupe to one");
    }

    @Test
    void respectsScanBandLimits() {
        LocalSkimmer s = new LocalSkimmer(FFT_SIZE, SAMPLE_RATE);
        s.setEnabled(true);
        s.setScanBand(600.0, 900.0);   // narrow to 600–900 Hz

        double[] mag = new double[FFT_SIZE / 2];
        for (int i = 0; i < mag.length; i++) mag[i] = 0.001;
        addCwPeak(mag, 500.0, 0.5);    // below band — ignored
        addCwPeak(mag, 700.0, 0.5);    // in band — kept
        addCwPeak(mag, 1100.0, 0.5);   // above band — ignored

        LocalSkimmer.Snapshot snap = runFrames(s, mag, 100);
        assertEquals(1, snap.peaks.size());
        assertEquals(700.0, snap.peaks.get(0).freqHz, BIN_HZ * 2);
    }

    @Test
    void caps_at_maxPeaks() {
        LocalSkimmer s = new LocalSkimmer(FFT_SIZE, SAMPLE_RATE);
        s.setEnabled(true);
        s.setMaxPeaks(3);

        double[] mag = new double[FFT_SIZE / 2];
        for (int i = 0; i < mag.length; i++) mag[i] = 0.001;
        // 5 carriers in band, all above threshold
        double[] freqs = { 500, 700, 900, 1100, 1300 };
        double[] amps  = { 0.20, 0.50, 0.30, 0.40, 0.60 };
        for (int i = 0; i < freqs.length; i++) addCwPeak(mag, freqs[i], amps[i]);

        LocalSkimmer.Snapshot snap = runFrames(s, mag, 100);
        assertEquals(3, snap.peaks.size());
    }
}
