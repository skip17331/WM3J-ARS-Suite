package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig;
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
 * rigctld protocol note (line-based, persistent TCP connection):
 *
 *   We send every command with the Hamlib EXTENDED-response prefix '+'. This is
 *   load-bearing: in the *default* protocol, get-commands return only their bare
 *   value with NO "RPRT" terminator —
 *       f  →  "145000000\n"          (no RPRT)
 *       m  →  "USB\n3000\n"          (no RPRT)
 *   — so a reader that blocks until it sees "RPRT " (as sendCommand does) hangs
 *   until the socket read times out, reconnects, and never reports "connected".
 *   The '+' prefix makes rigctld label every field and append "RPRT x" to *both*
 *   get and set replies, e.g.:
 *       +f  →  "get_freq:\nFrequency: 145000000\nRPRT 0\n"
 *       +m  →  "get_mode:\nMode: USB\nPassband: 3000\nRPRT 0\n"
 *       +F <hz>  / +M <mode> 0 / +T 0|1 / +L KEYSPD <n> / +b <text> / +\stop_morse
 *                →  "<echo>\nRPRT 0\n"
 *   parseFrequency()/parseMode() read the labeled lines; a non-zero RPRT throws.
 *
 * If the rig/backend doesn't support the CW commands, sendCw/stopCw/setKeyerSpeed
 * latch a one-shot {@code cwUnsupported} flag and notify MessageRouter so J-Log
 * can fall back to its CI-V CW path and display a single notice to the operator.
 */
public class HamlibRigController implements RigController {

    private static final Logger log = LoggerFactory.getLogger(HamlibRigController.class);

    private static final HamlibRigController INSTANCE = new HamlibRigController();
    public static HamlibRigController getInstance() { return INSTANCE; }
    private HamlibRigController() {}

    // Wired by JHubMain
    private MessageRouter router;

    // Config (updated by restart())
    private volatile String  host       = "127.0.0.1";
    private volatile int     port       = 4532;
    private volatile int     pollMs     = 500;
    private volatile boolean pttEnabled = false;
    // True when this start() came up with manageRigctld=true. Needed in
    // noteFailure() to attribute connect-refused errors correctly: when we own
    // the daemon, the manager's getLastError() / getDaemonComplaint() is the
    // truth, not the generic "is rigctld running?" message.
    private volatile boolean managed    = false;

    // Status
    private volatile boolean running        = false;
    private volatile boolean connected      = false;
    private volatile long    lastFreq       = 0;
    private volatile String  lastMode       = "";
    // Latches true if the rig/backend rejects any CW command. One-shot per session;
    // cleared only by restart() since "unsupported" is a property of the configured rig.
    private volatile boolean cwUnsupported  = false;

    // Last human-readable failure reason; "" when healthy. Surfaced via
    // /api/rig/status so the Rig Control panel can explain *why* it won't connect
    // instead of sitting on "Connecting…" forever with nothing in the INFO log.
    private volatile String  lastError      = "";
    // Failure-log throttle. The poll loop retries every ~3s; without throttling a
    // down/silent rig spams the log. Log the first failure (and any clean->fail
    // transition) at WARN, then re-log at WARN at most once a minute (DEBUG between).
    private long failCount     = 0;   // scheduler-thread only
    private long lastFailLogMs = 0;   // scheduler-thread only
    private static final long FAIL_RELOG_MS = 60_000;

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
        this.host       = JHubConfig.normalizeHost(cfg.hamlibHost);
        this.port       = cfg.hamlibPort > 0 ? cfg.hamlibPort : 4532;
        this.pollMs     = cfg.pollRateMs > 0 ? cfg.pollRateMs : 500;
        this.pttEnabled = cfg.enablePtt;
        this.managed    = cfg.manageRigctld;
        this.running    = true;
        this.lastError  = "";
        this.failCount  = 0;
        this.lastFailLogMs = 0;
        // When manageRigctld is true, j-hub owns the daemon. Bring it up
        // before the poll loop starts so the first TCP connect finds a
        // listener instead of failing with "Connection refused" until the
        // operator wonders what's wrong. A failure here is surfaced via
        // lastError so the Rig Control panel can show the rigctld stderr
        // instead of a generic "Connecting..." badge.
        if (cfg.manageRigctld) {
            String err = RigctldManager.getInstance().ensureRunning(cfg);
            if (err != null) lastError = err;
        }
        log.info("HamlibRigController starting — {}:{} poll={}ms managed={}",
                host, port, pollMs, cfg.manageRigctld);
        schedulePoll();
    }

    /** Stop polling and close the connection. */
    public void stop() {
        running = false;
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
        scheduler.execute(this::closeSocket);
        // Always stop the managed daemon on full controller stop. The "is it
        // ours to stop?" question is answered inside the manager — if we never
        // spawned one this is a cheap no-op.
        RigctldManager.getInstance().stop();
        connected = false;
        lastError = "";   // disabled is not an error state
        log.info("HamlibRigController stopped");
    }

    /** Apply new config — stop current poll, start fresh. */
    public void restart(RigSection cfg) {
        if (cfg == null) { running = false; return; }
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
        scheduler.execute(this::closeSocket);
        connected     = false;
        cwUnsupported = false; // re-probe CW support against the new rig
        if ("HAMLIB".equals(cfg.backend)) {
            start(cfg);
        } else {
            // Backend turned off — also tear down any rigctld we spawned.
            RigctldManager.getInstance().stop();
            running   = false;
            lastError = "";
        }
    }

    /** Force-close the TCP connection so the next poll tick reconnects. */
    public void reconnect() {
        if (!running) return;
        log.info("Hamlib reconnect requested");
        scheduler.execute(this::closeSocket);
    }

    // ── Status accessors ─────────────────────────────────────────────

    public boolean isRunning()       { return running; }
    public boolean isConnected()     { return connected; }
    public long    getLastFreq()     { return lastFreq; }
    public String  getLastMode()     { return lastMode; }
    public boolean isCwUnsupported() { return cwUnsupported; }
    /** Last failure reason for the UI; "" when connected or disabled. */
    public String  getLastError()    { return lastError; }

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

    /** Exchange VFOs A and B (vfo_op XCHG in Hamlib parlance). The
     *  scheduler-thread send mirrors tune()'s pattern; on success the next
     *  500ms poll cycle will broadcast a fresh RIG_STATUS so all clients
     *  see the new active VFO's freq/mode without a manual refresh. */
    @Override
    public void swapVfo() {
        if (!running) return;
        scheduler.execute(() -> {
            try {
                sendCommand("g XCHG");
            } catch (IOException e) {
                log.warn("Hamlib VFO swap failed: {}", e.getMessage());
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
        // Notify the antenna controller immediately (before the rigctld round-trip)
        // so the lockout-during-transmit safety blocks any in-flight switch.
        AntennaController.getInstance().setPttOn(on);
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

    // ── CW (Morse) commands ──────────────────────────────────────────
    // Backed by Hamlib level KEYSPD plus the send_morse / stop_morse commands.
    // Values are clamped to a sane WPM range; rejection by the rig latches the
    // cwUnsupported flag so subsequent CW calls become no-ops for this session.

    private static int clampWpm(int wpm) {
        if (wpm < 5)  return 5;
        if (wpm > 60) return 60;
        return wpm;
    }

    /** Set the rig's CW keyer speed in WPM (Hamlib level KEYSPD). */
    public void setKeyerSpeed(int wpm) {
        if (!running || cwUnsupported) return;
        final int speed = clampWpm(wpm);
        scheduler.execute(() -> {
            try {
                sendCommand("L KEYSPD " + speed);
            } catch (IOException e) {
                handleCwError("setKeyerSpeed", e);
            }
        });
    }

    /**
     * Send the supplied text as CW via the rig's internal keyer.
     * Sets KEYSPD to the requested WPM first, then dispatches the morse text.
     * The rigctld {@code b} command returns immediately; the morse plays out on the rig.
     */
    public void sendCw(String text, int wpm) {
        if (!running || cwUnsupported) return;
        if (text == null || text.isEmpty()) return;
        final int    speed = clampWpm(wpm);
        final String msg   = text;
        scheduler.execute(() -> {
            try {
                sendCommand("L KEYSPD " + speed);
                sendCommand("b " + msg);
                log.debug("CW sent ({} WPM): {}", speed, msg);
            } catch (IOException e) {
                handleCwError("sendCw", e);
            }
        });
    }

    /** Abort any in-flight CW transmission via Hamlib's stop_morse. */
    public void stopCw() {
        if (!running || cwUnsupported) return;
        scheduler.execute(() -> {
            try {
                sendCommand("\\stop_morse");
            } catch (IOException e) {
                handleCwError("stopCw", e);
            }
        });
    }

    // RPRT failures are protocol-level rejections — connection stays good, command failed.
    // Any RPRT on a CW command means this rig/backend can't do Hamlib CW; latch and notify once.
    // Real I/O failures still close the socket so the next poll reconnects.
    private void handleCwError(String op, IOException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("RPRT ")) {
            if (!cwUnsupported) {
                cwUnsupported = true;
                log.warn("Hamlib CW unsupported by this rig ({}): {} — disabling CW path", op, msg);
                if (router != null) router.publishCwUnsupported();
            }
        } else {
            log.warn("Hamlib {} I/O error: {}", op, msg);
            closeSocket();
        }
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
                connected     = true;
                lastError     = "";
                failCount     = 0;
                lastFailLogMs = 0;
                log.info("Hamlib connected — {}:{}", host, port);
            }

            if (freq != lastFreq || !mode.equals(lastMode)) {
                lastFreq = freq;
                lastMode = mode;
                publishRigStatus(freq, mode);
            }

        } catch (IOException e) {
            noteFailure(e);
            closeSocket();
        }
    }

    /**
     * Record a poll/command failure: build an operator-readable explanation,
     * stash it for the Rig Control panel, and log it (throttled).
     *
     * Three failure modes look identical in the UI ("not connected") but have
     * different fixes, so they must be told apart:
     *   • managed rigctld refused to spawn  → manager.getLastError() is the truth
     *                                         ("Serial port not set", "rigctld
     *                                         not found", "exited immediately…")
     *   • managed rigctld up but won't bind → rigctld can't open the serial /
     *                                         can't talk to the rig. It logs the
     *                                         cause to stdout (rig_open: error =
     *                                         …) which the manager scrapes into
     *                                         getDaemonComplaint().
     *   • external rigctld unreachable      → ConnectException → operator must
     *                                         start rigctld themselves.
     * Operators almost always misfile a managed-mode failure as a j-hub bug, so
     * never tell them to "start rigctld" when we're the ones who tried to.
     */
    private void noteFailure(IOException e) {
        boolean wasConnected = connected;
        connected = false;

        String where = host + ":" + port;
        String detail;
        if (e instanceof java.net.ConnectException) {
            if (managed) {
                RigctldManager mgr = RigctldManager.getInstance();
                String mgrErr = mgr.getLastError();
                String complaint = mgr.getDaemonComplaint();
                if (mgrErr != null && !mgrErr.isBlank()) {
                    detail = mgrErr;
                } else if (mgr.isRunning() && complaint != null && !complaint.isBlank()) {
                    detail = "rigctld is running but couldn't open the rig (" + complaint
                           + ") — check Model ID, serial port and baud, and that"
                           + " the radio is on with CAT enabled.";
                } else if (mgr.isRunning()) {
                    detail = "rigctld is running but isn't accepting connections —"
                           + " usually means it couldn't open the serial device."
                           + " Check Model ID, serial port and baud, and that the"
                           + " radio is on with CAT enabled.";
                } else {
                    detail = "rigctld failed to start (no process running). Check"
                           + " Model ID, serial port and baud in Rig Control.";
                }
            } else {
                detail = "Can't reach rigctld at " + where
                       + " — is rigctld running and listening on that port?";
            }
        } else if (e instanceof java.net.UnknownHostException) {
            detail = "Unknown host '" + host + "' — check the Hamlib host setting.";
        } else if (e instanceof java.net.SocketTimeoutException) {
            detail = "Reached rigctld at " + where + " but got no reply within 3s"
                   + " — rigctld is up, the rig isn't answering. Check rigctld's"
                   + " model (-m), serial port (-r) and baud (-s), and that the"
                   + " radio is powered on with CAT enabled.";
        } else {
            String m = e.getMessage();
            if (m != null && m.contains("RPRT ")) {
                detail = "rigctld is running but the rig didn't answer (" + m + ")"
                       + " — check rigctld's -m model, -r port and -s baud, and"
                       + " that the radio is on with CAT enabled.";
            } else {
                detail = "rigctld I/O error at " + where + ": "
                       + (m == null ? e.toString() : m);
            }
        }
        lastError = detail;

        failCount++;
        long now = System.currentTimeMillis();
        boolean relog = wasConnected || lastFailLogMs == 0
                     || (now - lastFailLogMs) >= FAIL_RELOG_MS;
        if (relog) {
            lastFailLogMs = now;
            if (wasConnected) log.warn("Hamlib connection lost — {}", detail);
            else              log.warn("Hamlib not connecting (attempt {}) — {}",
                                       failCount, detail);
        } else {
            log.debug("Hamlib still failing (attempt {}) — {}", failCount, detail);
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
        // Forward the new frequency to the amp so band-switched amps follow
        // the rig automatically. The amp controller filters same-band repeats
        // and respects its own bandFollow flag, so this is a cheap call.
        HamlibAmpController.getInstance().followBand(freq);
    }

    // ── rigctld socket I/O ───────────────────────────────────────────
    // All methods below are called only from the scheduler thread.

    private void ensureConnected() throws IOException {
        if (socket != null && !socket.isClosed() && socket.isConnected()) return;
        Socket s = new Socket();
        try {
            // Bounded connect so an unreachable host fails in 3s, not the OS
            // default (~minutes). A connect-phase timeout is reported as a
            // ConnectException so noteFailure() can distinguish "can't reach
            // rigctld" from "rigctld up but the rig is mute" (a read timeout).
            s.connect(new java.net.InetSocketAddress(host, port), 3000);
        } catch (java.net.SocketTimeoutException te) {
            try { s.close(); } catch (IOException ignored) {}
            throw new java.net.ConnectException(
                    "timed out connecting to " + host + ":" + port);
        }
        s.setSoTimeout(3000);
        socket = s;
        reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        out = socket.getOutputStream();
        log.debug("rigctld socket opened — {}:{}", host, port);
    }

    private String sendCommand(String cmd) throws IOException {
        ensureConnected();
        // '+' = Hamlib extended response: every reply (get OR set) is field-
        // labelled and terminated with "RPRT x". Without it, plain get-commands
        // (f, m) return a bare value and no RPRT, so the loop below would block
        // until the socket times out. See the class header for the wire formats.
        out.write(("+" + cmd + "\n").getBytes(StandardCharsets.US_ASCII));
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

    /**
     * Extended protocol returns "get_mode:\nMode: USB\nPassband: 3000" — pick the
     * "Mode:" line, not the first line. Falls back to the first bare,
     * non-label token so a plain-protocol "USB\n3000" reply still parses.
     */
    private static String parseMode(String response) {
        for (String raw : response.split("\n")) {
            String s = raw.trim();
            if (s.regionMatches(true, 0, "Mode:", 0, 5)) {
                return s.substring(5).trim();
            }
        }
        for (String raw : response.split("\n")) {
            String s = raw.trim();
            if (!s.isEmpty() && !s.endsWith(":")) return s;
        }
        return "";
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
