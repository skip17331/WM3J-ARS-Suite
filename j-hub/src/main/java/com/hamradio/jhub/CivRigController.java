package com.hamradio.jhub;

import com.fazecast.jSerialComm.SerialPort;
import com.hamradio.jhub.civ.CivProtocol;
import com.hamradio.jhub.model.JHubConfig.RigSection;
import com.hamradio.jhub.model.RigStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * CivRigController — direct Icom CI-V serial backend.
 *
 * <p>This is the "no Hamlib needed" path for Icom owners. Where
 * {@link HamlibRigController} hands everything off to a {@code rigctld}
 * TCP daemon, this controller talks CI-V binary frames directly over
 * the rig's serial port. Faster (no localhost socket hop), one less
 * dependency to install, and matches the way a lot of operators have
 * always run their Icoms.
 *
 * <p>Limits: CI-V is Icom-only; mode coverage is the standard
 * LSB/USB/AM/CW/RTTY/FM set plus DV/DD on modern rigs. CW keying via
 * CI-V isn't reliably supported across the line so we surface
 * "CW unsupported" once and let j-log/j-digi fall back to their audio
 * keyer path — the same flag {@link HamlibRigController} uses when a
 * given rig rejects Hamlib CW.
 *
 * <p>Threading is the same single-thread scheduler pattern the Hamlib
 * controller uses: poll loop runs at {@code pollRateMs}, all commands
 * serialise onto that thread so the serial port is never accessed
 * from two threads at once.
 */
public class CivRigController implements RigController {

    private static final Logger log = LoggerFactory.getLogger(CivRigController.class);

    private static final CivRigController INSTANCE = new CivRigController();
    public static CivRigController getInstance() { return INSTANCE; }
    private CivRigController() {}

    // ── Wired by JHubMain ────────────────────────────────────────────
    private MessageRouter router;

    // ── Config (re-read on each restart) ─────────────────────────────
    private volatile String  portName   = "";
    private volatile int     baud       = 9600;
    private volatile int     rigAddr    = CivProtocol.TO_RIG_DEFAULT;
    private volatile int     pollMs     = 500;
    private volatile boolean pttEnabled = false;

    // ── Status ───────────────────────────────────────────────────────
    private volatile boolean running       = false;
    private volatile boolean connected     = false;
    private volatile long    lastFreq      = 0;
    private volatile String  lastMode      = "";
    private volatile String  lastError     = "";
    // CI-V keying isn't a universal capability and j-log/j-digi already
    // have an audio fallback path. Latch unsupported up-front rather than
    // attempting cmd 0x17 (send CW char) and risking a NAK loop.
    private static final boolean CW_SUPPORTED = false;

    // Failure-log throttle (same idea as HamlibRigController)
    private long failCount     = 0;   // scheduler thread only
    private long lastFailLogMs = 0;   // scheduler thread only
    private static final long FAIL_RELOG_MS = 60_000;

    // ── Serial port — touched only from the scheduler thread ─────────
    private SerialPort port;

    // ── Scheduler (single thread) ────────────────────────────────────
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "civ-poll");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> pollFuture;

    // ── Lifecycle ────────────────────────────────────────────────────

    @Override public void setRouter(MessageRouter r) { this.router = r; }

    @Override public void start(RigSection cfg) {
        this.portName   = cfg.civPort    != null ? cfg.civPort : "";
        this.baud       = cfg.civBaud    > 0     ? cfg.civBaud : 9600;
        this.rigAddr    = CivProtocol.parseAddress(cfg.civAddress);
        this.pollMs     = cfg.pollRateMs > 0     ? cfg.pollRateMs : 500;
        this.pttEnabled = cfg.enablePtt;
        this.running    = true;
        this.lastError  = "";
        this.failCount  = 0;
        this.lastFailLogMs = 0;
        log.info("CivRigController starting — port={} baud={} addr=0x{} poll={}ms",
                portName, baud, Integer.toHexString(rigAddr), pollMs);
        schedulePoll();
    }

    @Override public void stop() {
        running = false;
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
        scheduler.execute(this::closePort);
        connected = false;
        lastError = "";
        log.info("CivRigController stopped");
    }

    @Override public void restart(RigSection cfg) {
        if (cfg == null) { running = false; return; }
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
        scheduler.execute(this::closePort);
        connected = false;
        if ("CI_V".equals(cfg.backend)) {
            start(cfg);
        } else {
            running = false;
            lastError = "";
        }
    }

    @Override public void reconnect() {
        if (!running) return;
        log.info("CI-V reconnect requested");
        scheduler.execute(this::closePort);
    }

    // ── Status accessors ─────────────────────────────────────────────

    @Override public boolean isRunning()       { return running; }
    @Override public boolean isConnected()     { return connected; }
    @Override public long    getLastFreq()     { return lastFreq; }
    @Override public String  getLastMode()     { return lastMode; }
    @Override public String  getLastError()    { return lastError; }
    @Override public boolean isCwUnsupported() { return !CW_SUPPORTED; }

    // ── Commands ─────────────────────────────────────────────────────

    @Override public void tune(long freq, String mode) {
        if (!running) return;
        scheduler.execute(() -> {
            try {
                if (freq > 0) {
                    sendFrame(CivProtocol.CMD_FREQ_SET, CivProtocol.freqToBcd(freq));
                    lastFreq = freq;
                }
                if (mode != null && !mode.isBlank()) {
                    byte m = CivProtocol.modeNameToByte(mode);
                    sendFrame(CivProtocol.CMD_MODE_SET, new byte[] { m });
                    lastMode = CivProtocol.modeByteToName(m);
                }
                publishRigStatus(lastFreq, lastMode);
            } catch (IOException e) {
                log.warn("CI-V tune failed: {}", e.getMessage());
                closePort();
            }
        });
    }

    @Override public void setPtt(boolean on) {
        if (!running) { log.warn("setPtt called but CivRigController is not running"); return; }
        if (!pttEnabled) { log.warn("PTT is disabled in rig config"); return; }
        AntennaController.getInstance().setPttOn(on);
        scheduler.execute(() -> {
            try {
                byte[] data = new byte[] { (byte) CivProtocol.SUBCMD_PTT, (byte) (on ? 0x01 : 0x00) };
                sendFrame(CivProtocol.CMD_PTT, data);
                log.info("PTT {} (CI-V)", on ? "ON" : "OFF");
            } catch (IOException e) {
                log.warn("CI-V PTT command failed: {}", e.getMessage());
                closePort();
            }
        });
    }

    // CW path: not supported by the universal CI-V command set. We notify
    // the router once (so j-log shows the same fallback notice the user
    // would see on a Hamlib rig that rejects keying) and accept calls as
    // no-ops thereafter.
    @Override public void setKeyerSpeed(int wpm) { notifyCwUnsupportedOnce("setKeyerSpeed"); }
    @Override public void sendCw(String t, int w) { notifyCwUnsupportedOnce("sendCw"); }
    @Override public void stopCw()              { notifyCwUnsupportedOnce("stopCw"); }

    private boolean cwNotified = false;
    private synchronized void notifyCwUnsupportedOnce(String op) {
        if (cwNotified) return;
        cwNotified = true;
        log.info("CI-V backend does not support keying ({}); j-log/j-digi will fall back to audio CW", op);
        if (router != null) router.publishCwUnsupported();
    }

    // ── Poll loop ────────────────────────────────────────────────────

    private void schedulePoll() {
        if (pollFuture != null) pollFuture.cancel(false);
        pollFuture = scheduler.scheduleAtFixedRate(this::poll, 0, pollMs, TimeUnit.MILLISECONDS);
    }

    private void poll() {
        if (!running) return;
        try {
            byte[] freqResp = sendFrame(CivProtocol.CMD_FREQ_GET, null);
            byte[] modeResp = sendFrame(CivProtocol.CMD_MODE_GET, null);

            long   freq = parseFreqResponse(freqResp);
            String mode = parseModeResponse(modeResp);

            if (!connected) {
                connected = true;
                lastError = "";
                failCount = 0;
                lastFailLogMs = 0;
                log.info("CI-V connected — {} @ {} baud (addr 0x{})",
                        portName, baud, Integer.toHexString(rigAddr));
            }

            if (freq != lastFreq || !mode.equals(lastMode)) {
                lastFreq = freq;
                lastMode = mode;
                publishRigStatus(freq, mode);
            }
        } catch (IOException e) {
            noteFailure(e);
            closePort();
        }
    }

    // Distinguish failure modes the same way the Hamlib controller does so
    // operators get an actionable detail line in the Rig Control panel:
    //   • port won't open    → wrong device / no permission / cable absent
    //   • opened but mute    → rig powered off / wrong CI-V address / wrong baud
    private void noteFailure(IOException e) {
        boolean wasConnected = connected;
        connected = false;

        String detail;
        String m = e.getMessage() == null ? e.toString() : e.getMessage();
        if (m.contains("openPort returned false") || m.contains("not open")) {
            detail = "Can't open " + portName
                   + " — check the device name, that the cable is plugged in,"
                   + " and that you have permission (dialout group on Linux).";
        } else if (m.contains("timeout") || m.contains("Timeout")) {
            detail = "Opened " + portName + " but the rig isn't answering at "
                   + baud + " baud / addr 0x" + Integer.toHexString(rigAddr)
                   + " — check the rig is powered on, CI-V is enabled, and the"
                   + " address+baud match the rig's menu settings.";
        } else {
            detail = "CI-V I/O error on " + portName + ": " + m;
        }
        lastError = detail;

        failCount++;
        long now = System.currentTimeMillis();
        boolean relog = wasConnected || lastFailLogMs == 0
                     || (now - lastFailLogMs) >= FAIL_RELOG_MS;
        if (relog) {
            lastFailLogMs = now;
            if (wasConnected) log.warn("CI-V connection lost — {}", detail);
            else              log.warn("CI-V not connecting (attempt {}) — {}", failCount, detail);
        } else {
            log.debug("CI-V still failing (attempt {}) — {}", failCount, detail);
        }
    }

    private void publishRigStatus(long freq, String mode) {
        if (router == null) return;
        RigStatus rig = new RigStatus();
        rig.source    = "CI_V";
        rig.frequency = freq;
        rig.mode      = mode;
        rig.band      = frequencyToBand(freq);
        rig.timestamp = Instant.now().toString();
        StateCache.getInstance().setLastRigStatus(rig);
        router.publishRigStatus(rig);
        HamlibAmpController.getInstance().followBand(freq);
    }

    // ── Serial I/O ───────────────────────────────────────────────────

    private void ensureOpen() throws IOException {
        if (port != null && port.isOpen()) return;
        if (portName == null || portName.isBlank()) {
            throw new IOException("CI-V port not configured");
        }
        SerialPort sp = SerialPort.getCommPort(portName);
        sp.setComPortParameters(baud, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        // Read timeout 1s: real Icoms answer in <100ms. 1s is plenty without
        // making the poll loop sluggish when the rig is silent.
        sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING
                            | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 1000);
        if (!sp.openPort()) {
            throw new IOException("openPort returned false for " + portName);
        }
        port = sp;
        log.debug("CI-V serial port opened — {} @ {}", portName, baud);
    }

    private void closePort() {
        if (port != null) {
            try { port.closePort(); } catch (Exception ignored) {}
            port = null;
        }
    }

    /**
     * Send a CI-V frame and read back the first reply addressed to us
     * (skipping any echo of our own TX that the CI-V cable may loop back).
     * Returns the full frame including FE FE … FD, or throws on timeout
     * or NAK. Caller is on the scheduler thread.
     */
    private byte[] sendFrame(int cmd, byte[] data) throws IOException {
        ensureOpen();
        byte[] tx = CivProtocol.frame(rigAddr, cmd, data);
        int wrote = port.writeBytes(tx, tx.length);
        if (wrote != tx.length) {
            throw new IOException("short write " + wrote + "/" + tx.length);
        }

        // Read frames until we see one FROM the rig TO us. A common CI-V
        // wiring echoes our own TX back on RX; we silently drop those.
        long deadline = System.currentTimeMillis() + 1200;
        while (System.currentTimeMillis() < deadline) {
            byte[] frame = readOneFrame(deadline);
            if (frame.length < 5) continue;
            int to   = frame[2] & 0xFF;
            int from = frame[3] & 0xFF;
            // Skip echoes (from == us, to == rig)
            if (from == CivProtocol.FROM_CONTROLLER && to == rigAddr) continue;
            // Reject if not addressed to us
            if (to != CivProtocol.FROM_CONTROLLER) continue;
            // NAK
            if ((frame[4] & 0xFF) == (CivProtocol.NAK & 0xFF)) {
                throw new IOException("CI-V NAK on cmd 0x" + Integer.toHexString(cmd));
            }
            return frame;
        }
        throw new IOException("timeout waiting for CI-V reply to cmd 0x" + Integer.toHexString(cmd));
    }

    /** Read one full CI-V frame (FE FE ... FD) from the port; bounded by deadline. */
    private byte[] readOneFrame(long deadlineMs) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int preamble = 0;   // count consecutive 0xFE bytes seen so far
        byte[] one = new byte[1];
        while (System.currentTimeMillis() < deadlineMs) {
            int n = port.readBytes(one, 1);
            if (n <= 0) continue;
            int b = one[0] & 0xFF;
            if (preamble < 2) {
                if (b == 0xFE) {
                    preamble++;
                    if (preamble == 2) {
                        buf.reset();
                        buf.write(0xFE); buf.write(0xFE);
                    }
                } else {
                    preamble = 0;
                }
                continue;
            }
            buf.write(b);
            if (b == 0xFD) return buf.toByteArray();
            // Guard: a runaway frame just resyncs at the next FE FE
            if (buf.size() > 64) {
                preamble = 0;
                buf.reset();
            }
        }
        return new byte[0];
    }

    private long parseFreqResponse(byte[] frame) {
        // Frame: FE FE E0 <rigAddr> 03 <5 BCD> FD
        if (frame.length < 11) return lastFreq;
        if ((frame[4] & 0xFF) != CivProtocol.CMD_FREQ_GET) return lastFreq;
        return CivProtocol.bcdToFreq(frame, 5);
    }

    private String parseModeResponse(byte[] frame) {
        // Frame: FE FE E0 <rigAddr> 04 <mode> [<filter>] FD
        if (frame.length < 7) return lastMode;
        if ((frame[4] & 0xFF) != CivProtocol.CMD_MODE_GET) return lastMode;
        return CivProtocol.modeByteToName(frame[5] & 0xFF);
    }

    // ── Frequency → band (identical to HamlibRigController) ──────────

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

    // Suppress unused-warning on Arrays import — used during early debugging dumps.
    @SuppressWarnings("unused")
    private static String hexDump(byte[] b) { return Arrays.toString(b); }
}
