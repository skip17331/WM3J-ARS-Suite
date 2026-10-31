package com.hamradio.modem;

import com.hamradio.modem.audio.AudioEngine;
import com.hamradio.modem.dsp.SkimmerSpotPublisher.LocalSpot;
import com.hamradio.modem.tx.CwTransmitter;
import com.hamradio.modem.tx.SampleSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Audio in, spot out: synthesised CW through the modem with the local skimmer on. */
class ModemServiceSkimmerTest {

    private static final long DIAL_HZ = 14_025_000L;
    private static final double TONE_HZ = 700.0;
    /** 12 wpm: the channel decoder copies this synthetic signal cleanly here; from about 15 wpm up it runs letters together. */
    private static final int CW_WPM = 12;
    private static final String OVER = "CQ CQ CQ DE W1AW W1AW W1AW K ";

    /** Feed {@code text} sent as CW at {@code TONE_HZ} over band noise, a frame at a time, followed by a little silence.
     *  The noise matters: a perfectly clean synthetic signal has a noise floor of zero, so even its keying sidebands
     *  count as signals. */
    private static void feedCw(ModemService svc, String text, long frameGapMs) throws InterruptedException {
        SampleSource src = new CwTransmitter(AudioEngine.SAMPLE_RATE, CW_WPM, TONE_HZ, 0.5).createSampleSource(text);
        java.util.Random noise = new java.util.Random(42);
        float[] frame = new float[AudioEngine.FRAME_SIZE];
        int silent = 0;
        while (silent < 6) {
            java.util.Arrays.fill(frame, 0f);
            int n = src.isFinished() ? 0 : src.read(frame, 0, frame.length);
            if (n <= 0) silent++;
            for (int i = 0; i < frame.length; i++) frame[i] += (float) (noise.nextGaussian() * 0.03);
            svc.handleAudioFrame(frame.clone());
            if (frameGapMs > 0) Thread.sleep(frameGapMs);
        }
        src.close();
    }

    @Test
    void aCallsignInCwBecomesASpotAtDialPlusTone() throws Exception {
        ModemService svc = new ModemService();
        List<LocalSpot> spots = new CopyOnWriteArrayList<>();
        svc.setSpotListener(spots::add);
        svc.setRigState(DIAL_HZ, "USB");
        svc.setLocalSkimmerEnabled(true);
        try {
            feedCw(svc, OVER, 0);

            assertFalse(spots.isEmpty(), "the skimmer produced no spot for a clear CQ from W1AW");
            LocalSpot s = spots.stream().filter(x -> x.callsign().equals("W1AW")).findFirst().orElse(null);
            assertNotNull(s, "spots were " + spots);
            assertEquals(DIAL_HZ + TONE_HZ, s.frequencyHz(), 40.0, "spots were " + spots);   // dial plus the audio offset
        } finally {
            svc.setLocalSkimmerEnabled(false);
        }
    }

    @Test
    void nothingIsSpottedWhileTheSkimmerIsOff() throws Exception {
        ModemService svc = new ModemService();
        List<LocalSpot> spots = new CopyOnWriteArrayList<>();
        svc.setSpotListener(spots::add);
        svc.setRigState(DIAL_HZ, "USB");
        svc.setLocalSkimmerEnabled(false);
        feedCw(svc, OVER, 0);
        assertTrue(spots.isEmpty());
    }

    @Test
    void nothingIsSpottedWithoutARigFrequency() throws Exception {
        ModemService svc = new ModemService();
        List<LocalSpot> spots = new CopyOnWriteArrayList<>();
        svc.setSpotListener(spots::add);
        svc.setLocalSkimmerEnabled(true);                                   // no setRigState: the spot has nothing to anchor to
        try {
            feedCw(svc, OVER, 0);
            assertTrue(spots.isEmpty());
        } finally {
            svc.setLocalSkimmerEnabled(false);
        }
    }
}
