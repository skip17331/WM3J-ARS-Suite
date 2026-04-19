package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig.RigSection;
import com.hamradio.jhub.model.RigStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.*;

/**
 * HamlibRigController — TCP client for the Hamlib rigctld daemon.
 *
 * When backend = "HAMLIB" in j-hub.json, this controller:
 *   • Connects to rigctld at hamlibHost:hamlibPort (default localhost:4532)
 *   • Polls frequency + mode at pollRateMs intervals
 *   • Publishes RIG_STATUS via MessageRouter on every change
 *   • Accepts tune(freq, mode) calls from SPOT_SELECTED handling
 *   • Accepts setPtt(on) calls from the REST API (if enablePtt=true)
 *
 * rigctld command protocol (line-based, persistent TCP connection):
 *   f         → get frequency   → "<hz>\nRPRT 0"
 *   m         → get mode        → "<mode>\n<passband>\nRPRT 0"
 *   F <hz>    → set frequency   → "RPRT 0"
 *   M <mode> 0 → set mode       → "RPRT 0"
 *   T 0|1     → set PTT off/on  → "RPRT 0"
 */
public class HamlibRigController {

    private static final Logger log = LoggerFactory.getLogger(HamlibRigController.class);

    private static final HamlibRigController INSTANCE = new HamlibRigController();
    public static HamlibRigController getInstance() { return INSTANCE; }
    private HamlibRigController() {}

    // Wired by JHubMain
    private MessageRouter router;

    // Config (updated by restart())
    private volatile String  host       = "localhost";
    private volatile int     port       = 4532;
    private volatile int     pollMs     = 500;
    private volatile boolean pttEnabled = false;

    // Status
    private volatile boolean running   = false;
    private volatile boolean connected = false;
    private volatile long    lastFreq  = 0;
    private volatile String  lastMode  = "";

    // Socket — only touched from the scheduler thread
    private Socket         socket;
    private BufferedReader reader;
    private OutputStream   out;

    // Single-threaded scheduler for polling + command serialisation
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "hamlib-poll");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> pollFuture;

    // ── Lifecycle ────────────────────────────────────────────────────

    public void setRouter(MessageRouter r) { this.router = r; }

    /** Start polling with the supplied rig configuration. */
    public void start(RigSection cfg) {
        this.host       = cfg.hamlibHost != null ? cfg.hamlibHost : "localhost";
        this.port       = cfg.hamlibPort > 0 ? cfg.hamlibPort : 4532;
        this.pollMs     = cfg.pollRateMs > 0 ? cfg.pollRateMs : 500;
        this.pttEnabled = cfg.enablePtt;
        this.running    = true;
        log.info("HamlibRigController starting — {}:{} poll={}ms", host, port, pollMs);
        schedulePoll();
    }

    /** Stop polling and close the connection. */
    public void stop() {
        running = false;
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
        scheduler.execute(this::closeSocket);
        connected = false;
        log.info("HamlibRigController stopped");
    }

    /** Apply new config — stop current poll, start fresh. */
    public void restart(RigSection cfg) {
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
        scheduler.execute(this::closeSocket);
        connected = false;
        if ("HAMLIB".equals(cfg.backend)) {
            start(cfg);
        } else {
            running = false;
        }
    }

    /** Force-close the TCP connection so the next poll tick reconnects. */
    public void reconnect() {
        if (!running) return;
        log.info("Hamlib reconnect requested");
        scheduler.execute(this::closeSocket);
    }

    // ── Status accessors ─────────────────────────────────────────────

    public boolean isRunning()   { return running; }
    public boolean isConnected() { return connected; }
    public long    getLastFreq() { return lastFreq; }
    public String  getLastMode() { return lastMode; }

    // ── Commands (dispatched onto the scheduler thread) ───────────────

    /**
     * Tune the rig to the given frequency and mode.
     * Called by MessageRouter on SPOT_SELECTED.
     */
    public void tune(long freq, String mode) {
        if (!running) return;
        scheduler.execute(() -> {
            try {
                if (freq > 0) {
                    sendCommand("F " + freq);
                    lastFreq = freq;
                }
                if (mode != null && !mode.isBlank()) {
                    sendCommand("M " + mode + " 0");
                    lastMode = mode;
                }
                // Publish immediately so the UI updates without waiting for the next poll
                publishRigStatus(lastFreq, lastMode);
            } catch (IOException e) {
                log.warn("Hamlib tune failed: {}", e.getMessage());
                closeSocket();
            }
        });
    }

    /**
     * Key or un-key the transmitter.
     * Only takes effect when enablePtt=true in config.
     */
    public void setPtt(boolean on) {
        if (!running) { log.warn("setPtt called but controller is not running"); return; }
        if (!pttEnabled) { log.warn("PTT is disabled in rig config"); return; }
        scheduler.execute(() -> {
            try {
                sendCommand("T " + (on ? "1" : "0"));
                log.info("PTT {}", on ? "ON" : "OFF");
            } catch (IOException e) {
                log.warn("PTT command failed: {}", e.getMessage());
                closeSocket();
            }
        });
    }

    // ── Poll loop ────────────────────────────────────────────────────

    private void schedulePoll() {
        if (pollFuture != null) pollFuture.cancel(false);
        pollFuture = scheduler.scheduleAtFixedRate(this::poll, 0, pollMs, TimeUnit.MILLISECONDS);
    }

    private void poll() {
        if (!running) return;
        try {
            String freqResp = sendCommand("f");
            String modeResp = sendCommand("m");

            long   freq = parseFrequency(freqResp);
            String mode = parseMode(modeResp);

            if (!connected) {
                connected = true;
                log.info("Hamlib connected — {}:{}", host, port);
            }

            if (freq != lastFreq || !mode.equals(lastMode)) {
                lastFreq = freq;
                lastMode = mode;
                publishRigStatus(freq, mode);
            }

        } catch (IOException e) {
            if (connected) {
                log.warn("Hamlib connection lost: {}", e.getMessage());
                connected = false;
            }
            closeSocket();
        }
    }

    private void publishRigStatus(long freq, String mode) {
        if (router == null) return;
        RigStatus rig  = new RigStatus();
        rig.source     = "HAMLIB";
        rig.frequency  = freq;
        rig.mode       = mode;
        rig.band       = frequencyToBand(freq);
        rig.timestamp  = Instant.now().toString();
        StateCache.getInstance().setLastRigStatus(rig);
        router.publishRigStatus(rig);
    }

    // ── rigctld socket I/O ───────────────────────────────────────────
    // All methods below are called only from the scheduler thread.

    private void ensureConnected() throws IOException {
        if (socket != null && !socket.isClosed() && socket.isConnected()) return;
        socket = new Socket(host, port);
        socket.setSoTimeout(3000);
        reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        out = socket.getOutputStream();
        log.debug("rigctld socket opened — {}:{}", host, port);
    }

    private String sendCommand(String cmd) throws IOException {
        ensureConnected();
        out.write((cmd + "\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("RPRT ")) {
                int code;
                try { code = Integer.parseInt(line.substring(5).trim()); }
                catch (NumberFormatException e) { code = -1; }
                if (code != 0) throw new IOException("rigctld RPRT " + code + " for: " + cmd);
                break;
            }
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        return sb.toString().trim();
    }

    private void closeSocket() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null; reader = null; out = null;
    }

    // ── Response parsers ─────────────────────────────────────────────

    /** Handles both "14225000" and "Frequency: 14225000" response formats. */
    private static long parseFrequency(String response) {
        for (String line : response.split("\n")) {
            String s = line.contains(":") ? line.substring(line.indexOf(':') + 1).trim() : line.trim();
            if (!s.isEmpty()) {
                try { return (long) Double.parseDouble(s); }
                catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    /** Takes the first line, strips "Mode: " prefix if present. */
    private static String parseMode(String response) {
        String first = response.split("\n")[0].trim();
        return first.replaceFirst("(?i)^Mode:\\s*", "");
    }

    // ── Frequency → band conversion ──────────────────────────────────

    private static String frequencyToBand(long hz) {
        long khz = hz / 1000;
        if (khz >=  1800 && khz <=  2000) return "160m";
        if (khz >=  3500 && khz <=  4000) return "80m";
        if (khz >=  5300 && khz <=  5500) return "60m";
        if (khz >=  7000 && khz <=  7300) return "40m";
        if (khz >= 10100 && khz <= 10150) return "30m";
        if (khz >= 14000 && khz <= 14350) return "20m";
        if (khz >= 18068 && khz <= 18168) return "17m";
        if (khz >= 21000 && khz <= 21450) return "15m";
        if (khz >= 24890 && khz <= 24990) return "12m";
        if (khz >= 28000 && khz <= 29700) return "10m";
        if (khz >= 50000 && khz <= 54000) return "6m";
        if (khz >= 144000 && khz <= 148000) return "2m";
        return "";
    }
}
