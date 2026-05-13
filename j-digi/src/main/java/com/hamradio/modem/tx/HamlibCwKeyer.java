package com.hamradio.modem.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Sends CW by handing the text to Hamlib's {@code rigctld} via the
 * {@code b <text>\n} (send_morse) command. The rig's own keyer plays
 * the Morse — j-digi never touches the audio device for this path.
 *
 * <p>Why a separate keyer instead of an extension to {@link AudioTxEngine}:
 * the audio-based flow opens a {@code SourceDataLine}, generates raised-cosine
 * dits/dahs, and PTT-toggles around the audio. The CAT-based flow does none
 * of that — the rig handles timing, sidetone, and PTT internally. Two
 * codepaths, similar callback shape.
 *
 * <h2>Timing</h2>
 * {@code rigctld} returns {@code RPRT 0} immediately after queueing the
 * Morse — long before the rig has finished sending it. We estimate the
 * total transmission duration from text length + WPM (PARIS formula) and
 * sleep for that long before firing {@code onCompleted}, so the status
 * panel reflects reality. A future polish would poll {@code \get_ptt}
 * to detect actual end-of-transmission, but the estimate is close
 * enough for normal use.
 *
 * <h2>Cancel</h2>
 * Issues {@code \stop_morse\n} (rigctld's abort command) and unblocks
 * the estimated-duration wait.
 */
public final class HamlibCwKeyer {

    private static final Logger log = LoggerFactory.getLogger(HamlibCwKeyer.class);

    public static final int    DEFAULT_WPM         = 20;
    private static final int   CONNECT_TIMEOUT_MS  = 2000;
    private static final int   READ_TIMEOUT_MS     = 2000;

    private final String host;
    private final int    port;
    private volatile int wpm;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private volatile Thread waitThread;
    private volatile boolean transmitting;

    public HamlibCwKeyer(String host, int port, int wpm) {
        this.host = host == null || host.isBlank() ? HamlibRigControl.DEFAULT_HOST : host.trim();
        this.port = port > 0 ? port : HamlibRigControl.DEFAULT_PORT;
        this.wpm  = wpm > 0 ? wpm : DEFAULT_WPM;
    }

    public String host() { return host; }
    public int    port() { return port; }
    public int    wpm()  { return wpm; }
    public void   setWpm(int wpm) { this.wpm = wpm > 0 ? wpm : DEFAULT_WPM; }
    public boolean isTransmitting() { return transmitting; }

    /**
     * Send {@code text} as Morse via the rig's keyer.
     *
     * @param onStarted   fired right after rigctld accepts the command
     * @param onCompleted fired after the estimated transmit duration elapses
     * @param onCancelled fired if {@link #cancel()} was called before completion
     * @param onError     fired if rigctld rejects the command or the
     *                    connection fails
     */
    public synchronized void transmit(String text,
                                      Runnable onStarted,
                                      Runnable onCompleted,
                                      Runnable onCancelled,
                                      Consumer<String> onError) {
        Objects.requireNonNull(text, "text");
        if (transmitting) {
            throw new IllegalStateException("CW transmit already in progress");
        }
        cancelled.set(false);
        transmitting = true;

        Thread t = new Thread(() -> runTransmit(text, onStarted, onCompleted, onCancelled, onError),
                              "hamlib-cw");
        t.setDaemon(true);
        waitThread = t;
        t.start();
    }

    /** Abort an in-flight CW transmission. */
    public synchronized void cancel() {
        if (!transmitting) return;
        cancelled.set(true);
        try { sendCommand("\\stop_morse\n"); }
        catch (IOException e) { log.warn("stop_morse failed: {}", e.getMessage()); }
        Thread t = waitThread;
        if (t != null) t.interrupt();
    }

    private void runTransmit(String text,
                             Runnable onStarted,
                             Runnable onCompleted,
                             Runnable onCancelled,
                             Consumer<String> onError) {
        try {
            String reply = sendCommand("b " + text + "\n");
            HamlibRigControl.parseRprt(reply, "b " + text);
            if (onStarted != null) onStarted.run();

            long durationMs = estimateMorseMs(text, wpm);
            try { Thread.sleep(durationMs); }
            catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

            if (cancelled.get()) {
                if (onCancelled != null) onCancelled.run();
            } else {
                if (onCompleted != null) onCompleted.run();
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Hamlib CW transmit failed: {}", msg);
            if (onError != null) onError.accept(msg);
        } finally {
            transmitting = false;
            waitThread = null;
        }
    }

    /** Sends a raw rigctld command, returns first reply line. Visible for tests. */
    String sendCommand(String command) throws IOException {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            s.setSoTimeout(READ_TIMEOUT_MS);
            OutputStream out = s.getOutputStream();
            Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            w.write(command);
            w.flush();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String reply = in.readLine();
            return reply == null ? "" : reply;
        }
    }

    /**
     * PARIS timing — the word PARIS is 50 dit-units including the trailing
     * inter-word space. So one character averages 50/5 = 10 dits, and 1
     * dit at N WPM is {@code 1200/N} ms. Visible for tests.
     */
    static long estimateMorseMs(String text, int wpm) {
        if (text == null || text.isEmpty()) return 0L;
        int chars = text.length();
        double ditMs = 1200.0 / Math.max(1, wpm);
        // 10 dits per character is a good average across English; bump
        // by 200 ms baseline to cover rig keyer ramp-up.
        return Math.round(chars * 10 * ditMs) + 200L;
    }
}
