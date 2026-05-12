package com.hamradio.modem.mode;

import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.SignalSnapshot;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drive Ax25Mode end-to-end against the WAVs produced by
 * generate_ax25_test_suite.py — read the WAV, chop into snapshot-sized
 * frames, push through process(), and assert the decoded packets match.
 */
class Ax25ModeTest {

    private static final int FRAME_SIZE = 1024;

    @Test
    void decodesCleanFrame() throws Exception {
        List<String> out = decodeWav("ax25_test_suite/ax25_clean.wav");
        // Filter to non-empty lines and split per-frame newline-terminated payload.
        List<String> packets = flatten(out);
        assertEquals(1, packets.size(), "expected one packet, got: " + packets);
        assertTrue(packets.get(0).startsWith("WM3J-7>APRS,WIDE1-1,WIDE2-2"),
                "addresses wrong: " + packets.get(0));
        assertTrue(packets.get(0).contains("ctrl=03"), "missing UI ctrl: " + packets.get(0));
        assertTrue(packets.get(0).contains("pid=F0"), "missing pid: " + packets.get(0));
        assertTrue(packets.get(0).endsWith(":>Test packet from j-digi AX.25 decoder!"),
                "payload wrong: " + packets.get(0));
    }

    @Test
    void decodesBurstOfThreeFrames() throws Exception {
        List<String> packets = flatten(decodeWav("ax25_test_suite/ax25_burst.wav"));
        assertEquals(3, packets.size(), "expected three packets, got: " + packets);

        assertTrue(packets.get(0).startsWith("WM3J-7>APRS,WIDE1-1,WIDE2-2"));
        assertTrue(packets.get(0).endsWith(":>Test packet from j-digi AX.25 decoder!"));

        assertTrue(packets.get(1).startsWith("WM3J>CQ"));
        assertTrue(packets.get(1).endsWith(":Hello AX.25 world"));

        assertTrue(packets.get(2).startsWith("N0CALL-9>BEACON,WIDE1-1*"),
                "h-bit marker missing: " + packets.get(2));
        assertTrue(packets.get(2).endsWith(":!4044.00N/07400.00W>Beacon test"));
    }

    @Test
    void decodesNoisyFrame() throws Exception {
        List<String> packets = flatten(decodeWav("ax25_test_suite/ax25_noisy.wav"));
        assertEquals(1, packets.size(),
                "modest gaussian noise should not stop decode, got: " + packets);
        assertTrue(packets.get(0).contains("WM3J-7>APRS"));
        assertTrue(packets.get(0).endsWith(":>Test packet from j-digi AX.25 decoder!"));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static List<String> decodeWav(String relativePath) throws Exception {
        File wav = locate(relativePath);
        float[] samples = readMonoFloat(wav);
        double sampleRate = readSampleRate(wav);
        Ax25Mode decoder = new Ax25Mode();
        List<String> out = new ArrayList<>();
        for (int off = 0; off < samples.length; off += FRAME_SIZE) {
            int len = Math.min(FRAME_SIZE, samples.length - off);
            float[] frame = new float[len];
            System.arraycopy(samples, off, frame, 0, len);
            double rms = rms(frame);
            SignalSnapshot snap = new SignalSnapshot(frame, new double[0], rms, 0.0, sampleRate);
            Optional<DecodeMessage> msg = decoder.process(snap, 144_390_000L);
            msg.ifPresent(m -> out.add(m.getText()));
        }
        return out;
    }

    private static List<String> flatten(List<String> emitted) {
        List<String> out = new ArrayList<>();
        for (String chunk : emitted) {
            for (String line : chunk.split("\n")) {
                if (!line.isBlank()) out.add(line);
            }
        }
        return out;
    }

    private static File locate(String relativePath) {
        // Tests run from the module root under Maven; the WAVs sit there.
        File primary = new File(relativePath);
        if (primary.isFile()) return primary;
        File fallback = new File("j-digi", relativePath);
        if (fallback.isFile()) return fallback;
        throw new IllegalStateException("Cannot find " + relativePath
                + " (cwd=" + new File(".").getAbsolutePath() + ")");
    }

    private static double readSampleRate(File wav) throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wav)) {
            return ais.getFormat().getSampleRate();
        }
    }

    private static float[] readMonoFloat(File wav) throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wav)) {
            AudioFormat fmt = ais.getFormat();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (InputStream in = ais) {
                byte[] tmp = new byte[4096];
                int n;
                while ((n = in.read(tmp)) > 0) buf.write(tmp, 0, n);
            }
            byte[] raw = buf.toByteArray();
            int sampleSize = fmt.getSampleSizeInBits() / 8;
            int channels = fmt.getChannels();
            int frames = raw.length / (sampleSize * channels);
            ByteOrder order = fmt.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            ByteBuffer bb = ByteBuffer.wrap(raw).order(order);
            float[] out = new float[frames];
            for (int i = 0; i < frames; i++) {
                int sum = 0;
                for (int c = 0; c < channels; c++) {
                    sum += bb.getShort();
                }
                out[i] = (sum / (float) channels) / 32768f;
            }
            return out;
        }
    }

    private static double rms(float[] s) {
        double sum = 0.0;
        for (float v : s) sum += v * v;
        return Math.sqrt(sum / Math.max(1, s.length));
    }
}
