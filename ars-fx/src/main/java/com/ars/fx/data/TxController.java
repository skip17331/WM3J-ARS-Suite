package com.ars.fx.data;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.Mixer;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Keys the radio for the F-key macros, from any module.
 *
 * <ul>
 *   <li><b>CW</b> — the text goes to the rig's own keyer ({@code send_morse}, Hamlib {@code b}); speed from
 *       {@code tx.cwWpm}. The rig does the timing.</li>
 *   <li><b>Voice</b> — PTT on ({@code T 1}), the macro's audio file plays on the chosen output
 *       ({@code tx.voiceDevice}), PTT off ({@code T 0}).</li>
 *   <li><b>Data</b> (RTTY, PSK31) — handed to the built-in J-Digi modem, which keys with its own settings.</li>
 * </ul>
 *
 * The hub process owns the rig. A window attached to the hub sends {@code tx} requests to it and plays any voice
 * audio itself; the hub releases PTT by itself if that window stops talking to it.
 *
 * <p>Safety: nothing is sent unless the rig is connected; PTT is always released on stop, on error and on a time
 * limit (the file's length plus a margin); a press while already transmitting is ignored; Esc / the red light stop it.
 */
public final class TxController {
    private TxController() {}

    private static final int PTT_MARGIN_MS = 3000, MAX_VOICE_MS = 120_000;

    /** Seconds between keying the rig and the audio starting, and between the audio ending and releasing the key.
     *  One number for both ({@code tx.pttDelay}, J-Hub ▸ Data ▸ Transmit), clamped to 0–2 s. Default 0.2 s. */
    public static double pttDelaySec() {
        try { return Math.max(0.0, Math.min(2.0, Double.parseDouble(HubConfig.get("tx.pttDelay", "0.2").trim()))); }
        catch (Exception e) { return 0.2; }
    }

    // ── the "transmitting" state the red light follows ───────────────────────
    private static final ReadOnlyBooleanWrapper TX = new ReadOnlyBooleanWrapper(false);
    /** True while we are keying the rig (CW, voice or data), or the rig itself reports PTT. Changes on the FX thread. */
    public static ReadOnlyBooleanProperty transmittingProperty() { ensureWatch(); return TX.getReadOnlyProperty(); }

    private static volatile boolean localBusy;         // our own CW/voice send in progress (this process)
    private static volatile boolean remoteBusy;        // the hub says it is sending for a window
    private static volatile boolean hubBusy;           // hub side: a CW send in progress here
    private static volatile boolean rigPtt;            // the rig's own PTT, polled
    private static volatile boolean watching;

    public static boolean busy() { return localBusy || remoteBusy || hubBusy; }
    /** Hub side: for the state snapshot sent to windows. */
    public static boolean hubTransmitting() { return hubBusy || pttHeld; }

    private static synchronized void ensureWatch() {
        if (watching) return;
        watching = true;
        RigClient.getInstance().addListener(s -> { rigPtt = s.ptt(); refresh(); });
        Thread t = new Thread(() -> {
            while (true) {
                try { Thread.sleep(300); } catch (InterruptedException e) { return; }
                refresh();
            }
        }, "tx-watch");
        t.setDaemon(true); t.start();
    }

    private static void refresh() {
        boolean digi = false;
        try { var m = DigiEngine.get(); digi = m != null && m.isTransmitting(); } catch (Throwable ignored) {}
        boolean now = localBusy || remoteBusy || hubBusy || rigPtt || digi;
        try { Platform.runLater(() -> { if (TX.get() != now) TX.set(now); }); } catch (IllegalStateException ignored) {}
    }

    /** Window side: the hub's own "sending" flag, from its state snapshot. */
    public static void applyRemoteTx(boolean on) { remoteBusy = on; refresh(); }

    // ── macros ──────────────────────────────────────────────────────────────
    /** Send a text macro: CW through the rig keyer, RTTY / PSK31 through the J-Digi modem. {@code report} gets
     *  plain-language progress and errors (any thread). */
    public static void macroText(String mode, String text, Consumer<String> report) {
        ensureWatch();
        String m = mode == null ? "" : mode.trim().toUpperCase(Locale.ROOT);
        if (text == null || text.isBlank()) { post(report, "✗ nothing to send — this macro has no text (J-Hub ▸ Data ▸ Macros)"); return; }
        switch (m) {
            case "RTTY", "PSK31" -> data(m, text, report);
            case "FT8", "FT4", "JT65", "JT9", "FST4", "MSK144" ->
                    post(report, "✗ " + m + " is sent by WSJT-X, not from here — nothing was transmitted");
            default -> cw(text, report);       // CW, and any other mode when the operator forced "CW / data"
        }
    }

    /** Send a voice macro: PTT, play the audio file, release PTT. */
    public static void macroVoice(String path, Consumer<String> report) {
        ensureWatch();
        if (busy()) { post(report, "✗ already transmitting — press Esc to stop it first"); return; }
        if (!RigClient.getInstance().last().connected()) { post(report, "✗ not sent — the rig is not connected (J-Hub ▸ Rig control)"); return; }
        File f = MacroConfig.resolve(path);
        if (f == null || !f.isFile()) { post(report, "✗ not sent — audio file not found: " + (path == null ? "" : path)); return; }
        localBusy = true; refresh();
        Thread t = new Thread(() -> runVoice(f, report), "tx-voice");
        t.setDaemon(true); worker = t; t.start();
    }

    /** Stop whatever we are sending and release PTT. Safe to call any time. */
    public static void abort() {
        aborted = true;
        Thread w = worker; if (w != null) w.interrupt();
        Clip c = playing; if (c != null) try { c.stop(); } catch (Exception ignored) {}
        if (RemoteLink.isActive()) { RemoteLink l = RemoteLink.get(); if (l != null) l.sendTx("stop", null); }
        else new Thread(TxController::releaseLocal, "tx-abort").start();
        localBusy = false; remoteBusy = false; refresh();
    }

    // ── CW ──────────────────────────────────────────────────────────────────
    private static void cw(String text, Consumer<String> report) {
        if (busy()) { post(report, "✗ already transmitting — press Esc to stop it first"); return; }
        if (!RigClient.getInstance().last().connected()) { post(report, "✗ not sent — the rig is not connected (J-Hub ▸ Rig control)"); return; }
        int wpm = wpm();
        long ms = estimateMs(text, wpm);
        localBusy = true; aborted = false; refresh();
        if (RemoteLink.isActive()) {
            JsonObject o = new JsonObject(); o.addProperty("text", text); o.addProperty("wpm", wpm);
            RemoteLink.get().sendTx("cw", o);
            post(report, "▶ CW " + wpm + " wpm — sent to the rig's keyer: " + text);
            SCHED.schedule(() -> { localBusy = false; refresh(); }, ms + 400, TimeUnit.MILLISECONDS);
            return;
        }
        Thread t = new Thread(() -> {
            try {
                cwLocal(text, wpm);
                post(report, "▶ CW " + wpm + " wpm — sent to the rig's keyer: " + text);
                sleepInterruptibly(ms + 400);
            } catch (IOException e) {
                post(report, "✗ the rig did not accept the CW text (" + e.getMessage() + ") — it may not support the Hamlib keyer command");
            } finally { localBusy = false; refresh(); }
        }, "tx-cw");
        t.setDaemon(true); worker = t; t.start();
    }

    /** Hub / standalone: set the keyer speed (best effort) and hand the text to the rig's keyer. */
    private static void cwLocal(String text, int wpm) throws IOException {
        try { RigClient.getInstance().command("L KEYSPD " + wpm); } catch (IOException ignored) { /* many rigs have no settable speed */ }
        RigClient.getInstance().command("b " + text.replace("\n", " "));
    }

    private static int wpm() {
        try { return Math.max(5, Math.min(60, Integer.parseInt(HubConfig.get("tx.cwWpm", "25").trim()))); } catch (Exception e) { return 25; }
    }
    /** Same estimate the J-Digi keyer uses (PARIS): the rig has no "done" signal over Hamlib. */
    private static long estimateMs(String text, int wpm) { return (long) text.length() * 10L * 1200L / wpm + 200; }

    // ── data (RTTY / PSK31) ─────────────────────────────────────────────────
    private static void data(String mode, String text, Consumer<String> report) {
        var modem = DigiEngine.get();
        if (modem == null) { post(report, "✗ the J-Digi modem is not available in this environment — nothing was transmitted"); return; }
        if (modem.isTransmitting()) { post(report, "✗ already transmitting — press Esc to stop it first"); return; }
        try {
            modem.setMode(com.hamradio.modem.model.ModeType.valueOf(mode));
            modem.transmitText(text);
            post(report, "▶ " + mode + " — sent to the J-Digi modem (it keys with J-Digi's own PTT / audio settings): " + text);
        } catch (Exception e) { post(report, "✗ " + mode + " send failed: " + e.getMessage()); }
        refresh();
    }

    // ── voice ───────────────────────────────────────────────────────────────
    private static volatile Thread worker;
    private static volatile boolean aborted;
    private static volatile Clip playing;

    private static void runVoice(File f, Consumer<String> report) {
        aborted = false;
        boolean keyed = false;
        try (AudioInputStream in = AudioSystem.getAudioInputStream(f)) {
            Clip clip = openClip(in);
            long fileMs = Math.min(MAX_VOICE_MS, clip.getMicrosecondLength() / 1000);
            playing = clip;
            long delayMs = Math.round(pttDelaySec() * 1000);
            pttOn(fileMs + 2 * delayMs + PTT_MARGIN_MS); keyed = true;
            post(report, "▶ voice — keyed, " + String.format(Locale.US, "%.2f", delayMs / 1000.0) + " s, then playing " + f.getName()
                    + " (" + String.format(Locale.US, "%.1f", fileMs / 1000.0) + " s)");
            sleepInterruptibly(delayMs);                          // key → pause → audio
            if (!aborted) {
                CountDownLatch done = new CountDownLatch(1);
                clip.addLineListener(ev -> { if (ev.getType() == LineEvent.Type.STOP) done.countDown(); });
                clip.start();
                done.await(fileMs + PTT_MARGIN_MS, TimeUnit.MILLISECONDS);
            }
            clip.stop(); clip.close();
            if (!aborted) sleepInterruptibly(delayMs);            // audio → pause → drop the key
            post(report, aborted ? "■ voice stopped" : "✓ voice sent — back to receive");
        } catch (InterruptedException e) {
            post(report, "■ voice stopped");
        } catch (Exception e) {
            post(report, "✗ voice not sent: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        } finally {
            playing = null;
            if (keyed) pttOffSafely();            // ALWAYS release, whatever happened above
            localBusy = false; refresh();
        }
    }

    private static Clip openClip(AudioInputStream in) throws Exception {
        String dev = HubConfig.get("tx.voiceDevice", "").trim();
        Clip clip = null;
        if (!dev.isEmpty()) {
            for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
                if (!mi.getName().equals(dev)) continue;
                try { clip = AudioSystem.getClip(mi); } catch (Exception ignored) { /* not an output: keep looking */ }
                if (clip != null) break;
            }
        }
        if (clip == null) clip = AudioSystem.getClip();      // system default output
        clip.open(in);
        return clip;
    }

    /** Names of the sound outputs a voice macro can play to (for the J-Hub setting). */
    public static java.util.List<String> outputDevices() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            try { if (AudioSystem.getMixer(mi).isLineSupported(new javax.sound.sampled.DataLine.Info(Clip.class, null)) || AudioSystem.getMixer(mi).getSourceLineInfo().length > 0) out.add(mi.getName()); }
            catch (Exception ignored) {}
        }
        return out;
    }

    /** The rig panel's PTT button: key or unkey, with a time limit so a forgotten key is released. Runs off the FX thread. */
    public static void manualPtt(boolean on) {
        PTT_QUEUE.execute(() -> {                 // one thread: key and unkey must happen in the order they were asked for
            try {
                if (on) { if (RigClient.getInstance().last().connected()) pttOn(MANUAL_PTT_MS); }
                else pttOffSafely();
            } catch (Exception e) { System.err.println("[tx] ptt: " + e.getMessage()); }
        });
    }
    private static final java.util.concurrent.ExecutorService PTT_QUEUE = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "tx-manual-ptt"); t.setDaemon(true); return t; });
    private static final long MANUAL_PTT_MS = 60_000;

    // ── PTT (local to whoever owns the rig; forwarded from a window) ────────
    private static void pttOn(long maxMs) throws IOException {
        if (RemoteLink.isActive()) {
            JsonObject o = new JsonObject(); o.addProperty("on", true); o.addProperty("maxMs", maxMs);
            RemoteLink.get().sendTx("ptt", o);
        } else { pttLocal(true, maxMs); }
    }
    private static void pttOffSafely() {
        for (int i = 0; i < 3; i++) {
            try {
                if (RemoteLink.isActive()) { JsonObject o = new JsonObject(); o.addProperty("on", false); RemoteLink.get().sendTx("ptt", o); }
                else pttLocal(false, 0);
                return;
            } catch (IOException e) { try { Thread.sleep(150); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); } }
        }
    }

    private static final ScheduledExecutorService SCHED = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "tx-sched"); t.setDaemon(true); return t; });
    private static volatile ScheduledFuture<?> watchdog;
    private static volatile boolean pttHeld;
    private static volatile Object pttHolder;

    /** Owner side: key / unkey, with a watchdog that releases PTT if nothing does. */
    private static synchronized void pttLocal(boolean on, long maxMs) throws IOException {
        ScheduledFuture<?> w = watchdog; if (w != null) { w.cancel(false); watchdog = null; }
        if (on) {
            RigClient.getInstance().command("T 1");
            pttHeld = true;
            watchdog = SCHED.schedule(() -> { try { pttLocal(false, 0); } catch (IOException ignored) {} }, Math.max(1000, Math.min(maxMs, MAX_VOICE_MS + PTT_MARGIN_MS)), TimeUnit.MILLISECONDS);
        } else {
            try { RigClient.getInstance().command("T 0"); } finally { pttHeld = false; pttHolder = null; }
        }
    }
    private static void releaseLocal() {
        try { RigClient.getInstance().command("\\stop_morse"); } catch (IOException ignored) {}
        try { pttLocal(false, 0); } catch (IOException ignored) {}
        hubBusy = false; refresh();
    }

    // ── hub side: requests from windows ─────────────────────────────────────
    /** A window asked the hub to transmit. {@code from} identifies the connection holding PTT. */
    public static void hubCommand(String op, JsonObject o, Object from) {
        new Thread(() -> {
            try {
                switch (op) {
                    case "cw" -> {
                        if (!RigClient.getInstance().last().connected()) return;
                        String text = o.get("text").getAsString(); int wpm = o.get("wpm").getAsInt();
                        hubBusy = true; pttHolder = from; refresh();      // if that window vanishes, hubClientGone stops the keyer
                        try { cwLocal(text, wpm); } finally { SCHED.schedule(() -> { hubBusy = false; refresh(); }, estimateMs(text, wpm) + 400, TimeUnit.MILLISECONDS); }
                    }
                    case "ptt" -> {
                        if (o.get("on").getAsBoolean()) {
                            if (!RigClient.getInstance().last().connected()) return;
                            pttHolder = from; pttLocal(true, o.has("maxMs") ? o.get("maxMs").getAsLong() : 10_000);
                        } else pttLocal(false, 0);
                        refresh();
                    }
                    case "stop" -> releaseLocal();
                    default -> { }
                }
            } catch (Exception e) { System.err.println("[tx] " + op + ": " + e.getMessage()); }
        }, "tx-hub-" + op).start();
    }
    /** A window disconnected: if it was holding PTT, release it. */
    public static void hubClientGone(Object conn) {
        if (conn != null && conn == pttHolder) new Thread(TxController::releaseLocal, "tx-release").start();
    }

    /** Window exiting / hub quitting: never leave the transmitter keyed. */
    public static void shutdown() {
        if (!busy() && !pttHeld) return;
        abort();
        try { Thread.sleep(250); } catch (InterruptedException ignored) {}
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private static void sleepInterruptibly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    private static void post(Consumer<String> report, String msg) {
        if (report == null) return;
        try { Platform.runLater(() -> report.accept(msg)); } catch (IllegalStateException noFx) { report.accept(msg); }
    }
}
