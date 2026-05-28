package com.hamradio.jhub;

import com.hamradio.jhub.model.AmpStatus;
import com.hamradio.jhub.model.JHubConfig;
import com.hamradio.jhub.model.JHubConfig.AmpSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * HamlibAmpController — TCP client for the Hamlib ampctld daemon.
 *
 * <p>When amp backend = "HAMLIB" in j-hub.json, this controller:
 * <ul>
 *   <li>Connects to ampctld at {@code tcpHost:tcpPort} (default {@code localhost:4531})
 *   <li>Polls frequency, powerstat, SWR, and forward power at {@code pollRateMs} intervals
 *   <li>Publishes {@link AmpStatus} via {@link MessageRouter#publishAmpStatus} on every change
 *   <li>Latches a per-level "unsupported" flag the first time a level returns RPRT — many amps
 *       only expose a subset of levels, and we don't want to spam the log on every poll
 *   <li>Reports a fault when SWR exceeds the configured {@code swrFault} threshold
 *   <li>Accepts {@link #followBand(long)} from {@link HamlibRigController} to forward the
 *       rig's frequency to the amp (only when {@code bandFollow=true})
 * </ul>
 *
 * <p>ampctld command protocol (line-based, persistent TCP connection):
 * <pre>
 *   f                  → get frequency      → "&lt;hz&gt;\nRPRT 0"
 *   F &lt;hz&gt;             → set frequency      → "RPRT 0"
 *   \get_powerstat     → operate / standby  → "0|1\nRPRT 0"
 *   \get_level SWR     → SWR                 → "&lt;float&gt;\nRPRT 0"
 *   \get_level PWR     → forward power      → "&lt;float&gt;\nRPRT 0"
 * </pre>
 */
public class HamlibAmpController {

    private static final Logger log = LoggerFactory.getLogger(HamlibAmpController.class);

    private static final HamlibAmpController INSTANCE = new HamlibAmpController();
    public  static HamlibAmpController getInstance() { return INSTANCE; }
    private HamlibAmpController() {}

    private MessageRouter router;

    // Config
    private volatile String  host       = "127.0.0.1";
    private volatile int     port       = 4531;
    private volatile int     pollMs     = 1000;
    private volatile boolean bandFollow = true;
    private volatile double  swrFault   = 3.0;

    // Status
    private volatile boolean running   = false;
    private volatile boolean connected = false;
    // Per-level "unsupported" flags — once true, we stop polling that level.
    private volatile boolean swrUnavailable       = false;
    private volatile boolean pwrUnavailable       = false;
    private volatile boolean powerstatUnavailable = false;

    // Last published state — used to suppress no-change broadcasts.
    private volatile AmpStatus lastStatus = new AmpStatus();

    // Socket — only touched from the scheduler thread
    private Socket         socket;
    private BufferedReader reader;
    private OutputStream   out;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "hamlib-amp");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> pollFuture;

    // ── Lifecycle ────────────────────────────────────────────────────

    public void setRouter(MessageRouter r) { this.router = r; }

    public void start(AmpSection cfg) {
        this.host       = JHubConfig.normalizeHost(cfg.tcpHost);
        this.port       = cfg.tcpPort > 0 ? cfg.tcpPort : 4531;
        this.pollMs     = cfg.pollRateMs > 0 ? cfg.pollRateMs : 1000;
        this.bandFollow = cfg.bandFollow;
        this.swrFault   = cfg.swrFault > 0 ? cfg.swrFault : 3.0;
        this.running    = true;
        // Re-probe levels on each (re)start — replacing the amp may add/remove support.
        this.swrUnavailable = this.pwrUnavailable = this.powerstatUnavailable = false;
        // Spawn ampctld ourselves if asked. Same shape as the rig + rotor
        // managed daemons; failures land in the j-hub log.
        if (cfg.manageAmpctld) {
            AmpctldManager.getInstance().ensureRunning(cfg);
        }
        log.info("HamlibAmpController starting — {}:{} poll={}ms bandFollow={} managed={}",
                host, port, pollMs, bandFollow, cfg.manageAmpctld);
        schedulePoll();
    }

    public void stop() {
        running = false;
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
        scheduler.execute(this::closeSocket);
        AmpctldManager.getInstance().stop();
        connected = false;
        log.info("HamlibAmpController stopped");
    }

    public void restart(AmpSection cfg) {
        if (cfg == null) { running = false; return; }
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
        scheduler.execute(this::closeSocket);
        connected = false;
        if ("HAMLIB".equals(cfg.backend)) {
            start(cfg);
        } else {
            AmpctldManager.getInstance().stop();
            running = false;
        }
    }

    // ── Status accessors ─────────────────────────────────────────────

    public boolean isRunning()        { return running; }
    public boolean isConnected()      { return connected; }
    public boolean isBandFollowing()  { return bandFollow; }
    public AmpStatus getLastStatus()  { return lastStatus; }

    // ── Commands (dispatched onto the scheduler thread) ───────────────

    /**
     * Forward the rig's current frequency to the amp so band-switched amps
     * follow automatically. Called by {@link HamlibRigController} on every
     * frequency change. No-op when {@code bandFollow=false} or the controller
     * isn't running. Same-band repeats are filtered to avoid bouncing the amp
     * relays on small VFO moves within a band.
     */
    public void followBand(long freqHz) {
        if (!running || !bandFollow || freqHz <= 0) return;
        // If the amp's last reported frequency is in the same band, don't bother.
        String currentBand = frequencyToBand(lastStatus.frequency);
        String newBand     = frequencyToBand(freqHz);
        if (!currentBand.isEmpty() && currentBand.equals(newBand)) return;
        scheduler.execute(() -> {
            try {
                sendCommand("F " + freqHz);
                log.debug("Amp band follow → {} Hz ({})", freqHz, newBand);
            } catch (IOException e) {
                log.warn("Amp band-follow command failed: {}", e.getMessage());
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
        AmpStatus s = new AmpStatus();
        s.timestamp = Instant.now().toString();

        try {
            // Frequency — every amp supports this (it's how they band-switch).
            String freqResp = sendCommand("f");
            s.frequency = parseLong(freqResp);
            s.band = frequencyToBand(s.frequency);

            if (!connected) {
                connected = true;
                log.info("ampctld connected — {}:{}", host, port);
            }

            s.swr       = readLevel("SWR", swrUnavailable, () -> swrUnavailable = true);
            s.fwdPower  = readLevel("PWR", pwrUnavailable, () -> pwrUnavailable = true);
            s.powerstat = readPowerstat();

            // Fault evaluation — SWR > threshold flags a fault. Amps that
            // expose a dedicated fault level can be added here later.
            if (!Double.isNaN(s.swr) && s.swr > swrFault) {
                s.faulted     = true;
                s.faultReason = String.format("SWR %.1f > %.1f", s.swr, swrFault);
            }

            if (statusChanged(lastStatus, s)) {
                lastStatus = s;
                if (router != null) router.publishAmpStatus(s);
            }

        } catch (IOException e) {
            if (connected) {
                log.warn("ampctld connection lost: {}", e.getMessage());
                connected = false;
            }
            closeSocket();
        }
    }

    /** Read a Hamlib level. On RPRT failure marks the level unavailable so we stop asking. */
    private double readLevel(String token, boolean alreadyUnavailable, Runnable markUnavailable) {
        if (alreadyUnavailable) return Double.NaN;
        try {
            String resp = sendCommand("\\get_level " + token);
            return parseDouble(resp);
        } catch (IOException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("RPRT ")) {
                log.info("Amp doesn't support level {} ({}); will stop polling it", token, msg);
                markUnavailable.run();
                return Double.NaN;
            }
            throw new RuntimeException(e); // surface real I/O failures to poll()'s catch
        }
    }

    /** Read the powerstat (0=standby, 1=operate). */
    private String readPowerstat() {
        if (powerstatUnavailable) return "UNKNOWN";
        try {
            String resp = sendCommand("\\get_powerstat");
            int n = (int) parseLong(resp);
            return n == 1 ? "OPERATE" : (n == 0 ? "STANDBY" : "UNKNOWN");
        } catch (IOException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("RPRT ")) {
                log.info("Amp doesn't support powerstat ({}); will stop polling it", msg);
                powerstatUnavailable = true;
                return "UNKNOWN";
            }
            throw new RuntimeException(e);
        }
    }

    private static boolean statusChanged(AmpStatus a, AmpStatus b) {
        if (a.frequency != b.frequency) return true;
        if (!nearlyEqual(a.swr,      b.swr))      return true;
        if (!nearlyEqual(a.fwdPower, b.fwdPower)) return true;
        if (!nearlyEqual(a.refPower, b.refPower)) return true;
        if (!a.powerstat.equals(b.powerstat))     return true;
        if (a.faulted != b.faulted)               return true;
        return false;
    }

    /** Treats two NaNs as equal; otherwise allows a small absolute tolerance. */
    private static boolean nearlyEqual(double a, double b) {
        if (Double.isNaN(a) && Double.isNaN(b)) return true;
        if (Double.isNaN(a) || Double.isNaN(b)) return false;
        return Math.abs(a - b) < 0.05;
    }

    // ── ampctld socket I/O ───────────────────────────────────────────

    private void ensureConnected() throws IOException {
        if (socket != null && !socket.isClosed() && socket.isConnected()) return;
        socket = new Socket(host, port);
        socket.setSoTimeout(3000);
        reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        out = socket.getOutputStream();
        log.debug("ampctld socket opened — {}:{}", host, port);
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
                if (code != 0) throw new IOException("ampctld RPRT " + code + " for: " + cmd);
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

    // ── Parsers ──────────────────────────────────────────────────────

    private static long parseLong(String response) {
        for (String line : response.split("\n")) {
            String s = line.contains(":") ? line.substring(line.indexOf(':') + 1).trim() : line.trim();
            if (!s.isEmpty()) {
                try { return (long) Double.parseDouble(s); }
                catch (NumberFormatException ignored) {}
            }
        }
        return 0L;
    }

    private static double parseDouble(String response) {
        for (String line : response.split("\n")) {
            String s = line.contains(":") ? line.substring(line.indexOf(':') + 1).trim() : line.trim();
            if (!s.isEmpty()) {
                try { return Double.parseDouble(s); }
                catch (NumberFormatException ignored) {}
            }
        }
        return Double.NaN;
    }

    // ── Frequency → band ─────────────────────────────────────────────

    static String frequencyToBand(long hz) {
        long khz = hz / 1000;
        if (khz >=   1800 && khz <=   2000) return "160m";
        if (khz >=   3500 && khz <=   4000) return "80m";
        if (khz >=   5300 && khz <=   5500) return "60m";
        if (khz >=   7000 && khz <=   7300) return "40m";
        if (khz >=  10100 && khz <=  10150) return "30m";
        if (khz >=  14000 && khz <=  14350) return "20m";
        if (khz >=  18068 && khz <=  18168) return "17m";
        if (khz >=  21000 && khz <=  21450) return "15m";
        if (khz >=  24890 && khz <=  24990) return "12m";
        if (khz >=  28000 && khz <=  29700) return "10m";
        if (khz >=  50000 && khz <=  54000) return "6m";
        if (khz >= 144000 && khz <= 148000) return "2m";
        return "";
    }
}
