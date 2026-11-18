package com.ars.fx.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** QSO audio recording (punch list 5-2): the ring of received audio, the WAV it writes, and its file names. */
class QsoRecorderTest {

    @AfterEach void reset() { QsoRecorder.clear(); }

    @Test void theWavIsReadableAsEightKilohertzSixteenBitMono() throws Exception {
        float[] s = new float[8000];
        for (int i = 0; i < s.length; i++) s[i] = (float) Math.sin(2 * Math.PI * 1000 * i / 8000.0) * 0.5f;
        byte[] wav = QsoRecorder.wav(s, 8000);
        assertEquals(44 + 16000, wav.length);
        try (AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
            AudioFormat f = in.getFormat();
            assertEquals(8000f, f.getSampleRate());
            assertEquals(16, f.getSampleSizeInBits());
            assertEquals(1, f.getChannels());
            assertEquals(8000, in.getFrameLength(), "one second");
        }
    }

    @Test void loudSamplesAreClippedNotWrapped() {
        byte[] wav = QsoRecorder.wav(new float[]{2f, -2f}, 8000);
        short a = (short) ((wav[44] & 0xff) | (wav[45] << 8)), b = (short) ((wav[46] & 0xff) | (wav[47] << 8));
        assertEquals(32767, a);
        assertEquals(-32767, b);
    }

    @Test void theRingKeepsTheLatestAudioOldestFirst() {
        for (int f = 0; f < 3; f++) {
            float[] frame = new float[1000];
            java.util.Arrays.fill(frame, f + 1);
            QsoRecorder.append(frame);
        }
        float[] last = QsoRecorder.last(1500);
        assertEquals(1500, last.length);
        assertEquals(2f, last[0], "from the middle of the second frame");
        assertEquals(3f, last[1499]);
        assertEquals(3000, QsoRecorder.last(10_000).length, "only what has been heard");
    }

    @Test void theRingWrapsAfterTwoMinutes() {
        int cap = QsoRecorder.RATE * QsoRecorder.MAX_SECONDS;
        float[] frame = new float[8000];
        for (int i = 0; i < cap / 8000 + 3; i++) { java.util.Arrays.fill(frame, i); QsoRecorder.append(frame); }
        float[] all = QsoRecorder.last(cap * 2);
        assertEquals(cap, all.length, "never more than the ring holds");
        assertEquals(cap / 8000 + 2, all[all.length - 1], 0f, "the newest frame last");
        assertEquals(3, all[0], 0f, "the three oldest frames were overwritten");
    }

    @Test void fileNames() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 28, 12, 34, 56);
        assertEquals("2026-11-28/123456_DL1ABC_20m_CW.wav", QsoRecorder.fileName(t, "DL1ABC", "20m", "CW"));
        assertEquals("2026-11-28/123456_W1AW-P_40m_SSB.wav", QsoRecorder.fileName(t, "W1AW/P", "40m", "SSB"), "a / cannot be in a file name");
        assertEquals("2026-11-28/123456_K1ABC_-_-.wav", QsoRecorder.fileName(t, "K1ABC", null, ""));
    }
}
