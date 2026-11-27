package com.ars.fx.data;

import com.hamradio.modem.ModemService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * QSO audio recording (punch list 5-2). While it is on, the receive audio J-Digi captures (the input chosen in J-Digi, 8 kHz mono)
 * goes into a ring of the last two minutes. When a QSO is logged on the log form or the map's QSO card, the last
 * {@code rec.seconds} of it is written as a WAV file: {@code ~/.j-log/recordings/<date>/<time>_<call>_<band>_<mode>.wav} (UTC).
 * Off until switched on in Setup ▸ Log database & backup. Turning it on starts J-Digi's audio capture if it is not already running.
 */
public final class QsoRecorder {
    private QsoRecorder() {}

    public static final String KEY_ON = "rec.qso", KEY_SECONDS = "rec.seconds";
    public static final int RATE = 8000, MAX_SECONDS = 120, DEFAULT_SECONDS = 60;

    private static final float[] ring = new float[RATE * MAX_SECONDS];
    private static long written;                                 // samples ever put in the ring
    private static final Consumer<float[]> LISTENER = QsoRecorder::append;
    private static ModemService attachedTo;
    private static boolean started;

    /** Call once at start-up: follows the Setup switch from then on. */
    public static synchronized void init() {
        if (started) return;
        started = true;
        HubConfig.addChangeListener(k -> { if (k == null || k.equals("*") || k.startsWith("rec.")) sync(); });
        sync();
    }

    public static boolean enabled() { return HubConfig.getBool(KEY_ON, false); }

    /** Seconds kept per QSO: 10 to 120, default 60. */
    public static int seconds() {
        int s = HubConfig.getInt(KEY_SECONDS, DEFAULT_SECONDS);
        return Math.max(10, Math.min(MAX_SECONDS, s));
    }

    public static Path folder() { return Paths.get(System.getProperty("user.home"), ".j-log", "recordings"); }

    /** Attach to or detach from J-Digi's audio as the switch says. */
    private static synchronized void sync() {
        ModemService svc = enabled() ? DigiEngine.get() : null;
        if (svc == attachedTo) return;
        if (attachedTo != null) attachedTo.removeAudioListener(LISTENER);
        attachedTo = svc;
        if (svc != null) {
            svc.addAudioListener(LISTENER);
            DigiEngine.start();                                  // capture runs only once something needs it
        }
        synchronized (ring) { written = 0; }
    }

    /** The capture thread's frames, into the ring. */
    static void append(float[] frame) {
        if (frame == null) return;
        synchronized (ring) {
            for (float v : frame) { ring[(int) (written % ring.length)] = v; written++; }
        }
    }

    /** The last {@code n} samples heard (fewer if less has been heard), oldest first. */
    static float[] last(int n) {
        synchronized (ring) {
            int k = (int) Math.min(Math.min(n, ring.length), written);
            float[] out = new float[k];
            long start = written - k;
            for (int i = 0; i < k; i++) out[i] = ring[(int) ((start + i) % ring.length)];
            return out;
        }
    }

    /** Test hook: forget what was heard. */
    static void clear() { synchronized (ring) { written = 0; } }

    /**
     * A QSO was logged: write its recording in the background. Returns the file it will be written to, or null when recording is
     * off or nothing has been heard yet.
     */
    public static Path logged(String call, String band, String mode) {
        if (!enabled() || attachedTo == null) return null;
        float[] audio = last(seconds() * RATE);
        if (audio.length == 0) return null;
        Path file = folder().resolve(fileName(LocalDateTime.now(ZoneOffset.UTC), call, band, mode));
        Thread t = new Thread(() -> {
            try {
                Files.createDirectories(file.getParent());
                Files.write(file, wav(audio, RATE));
            } catch (IOException e) {
                System.err.println("[recorder] could not write " + file + ": " + e.getMessage());
            }
        }, "qso-recording");
        t.setDaemon(true);
        t.start();
        return file;
    }

    /** {@code 2026-11-28/123456_DL1ABC_20m_CW.wav}; characters a file name cannot hold (the / of a portable call) become '-'. */
    static String fileName(LocalDateTime utc, String call, String band, String mode) {
        String day = utc.format(DateTimeFormatter.ISO_LOCAL_DATE), time = utc.format(DateTimeFormatter.ofPattern("HHmmss"));
        return day + "/" + time + "_" + safe(call) + "_" + safe(band) + "_" + safe(mode) + ".wav";
    }

    private static String safe(String s) {
        String v = s == null ? "" : s.trim().replaceAll("[^A-Za-z0-9.-]", "-");
        return v.isEmpty() ? "-" : v;
    }

    /** 16-bit PCM mono WAV of {@code samples} (-1..1, clipped). */
    static byte[] wav(float[] samples, int rate) {
        int data = samples.length * 2;
        ByteBuffer b = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()).putInt(36 + data).put("WAVE".getBytes());
        b.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1)
                .putInt(rate).putInt(rate * 2).putShort((short) 2).putShort((short) 16);
        b.put("data".getBytes()).putInt(data);
        for (float v : samples) {
            float c = Math.max(-1f, Math.min(1f, v));
            b.putShort((short) Math.round(c * 32767));
        }
        return b.array();
    }
}
