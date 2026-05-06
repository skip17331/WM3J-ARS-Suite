package com.hamradio.jhub;

import com.fazecast.jSerialComm.SerialPort;
import com.hamradio.jhub.model.AntennaStatus;
import com.hamradio.jhub.model.JHubConfig.AntennaRule;
import com.hamradio.jhub.model.JHubConfig.AntennaSection;
import com.hamradio.jhub.model.JHubConfig.AntennaSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AntennaController — drives a serial-port-attached antenna switch (or several)
 * from band / mode / rotor-heading inputs. Pure Java via jSerialComm.
 *
 * <p>Invariants:
 * <ul>
 *   <li>All serial I/O runs on a single-threaded executor — no overlapping
 *       writes (most cheap relay boxes handle one command at a time).
 *   <li>Rule order is significant — the first matching rule per switch wins,
 *       so operators put more-specific rules above less-specific ones.
 *   <li>A {@code setOverride} on a switch suppresses rule evaluation for that
 *       switch until {@code clearOverride} fires. Other switches stay
 *       rule-driven.
 *   <li>When {@code lockoutOnPtt=true} and {@link #setPttOn(boolean)} reports
 *       on, evaluation is paused; the moment PTT clears we re-evaluate so any
 *       deferred change is applied immediately.
 * </ul>
 */
public class AntennaController {

    /** Dedicated SLF4J logger so operators can route antenna events to a
     *  separate appender (e.g. a contest-night audit log). */
    private static final Logger antennaLog = LoggerFactory.getLogger("antenna");
    private static final Logger log        = LoggerFactory.getLogger(AntennaController.class);

    private static final AntennaController INSTANCE = new AntennaController();
    public  static AntennaController getInstance() { return INSTANCE; }
    private AntennaController() {}

    private MessageRouter router;

    // Config snapshot — replaced atomically on restart()
    private volatile AntennaSection cfg = new AntennaSection();

    // Hardware
    private SerialPort port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean faulted = new AtomicBoolean(false);

    // Live inputs
    private volatile String currentBand    = "";
    private volatile String currentMode    = "";
    private volatile double currentHeading = -1;
    private final AtomicBoolean pttOn      = new AtomicBoolean(false);

    // State per switch
    /** id → currently-selected antenna (0 = unset). */
    private final Map<String, Integer> currentAntenna = new HashMap<>();
    /** id → operator-forced antenna; absent means "rule-driven". */
    private final Map<String, Integer> overrides = new HashMap<>();

    // Single-threaded executor — same pattern as the Hamlib controllers,
    // serializes serial writes and rule evaluation.
    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "antenna-controller"); t.setDaemon(true); return t;
    });

    // ── Lifecycle ────────────────────────────────────────────────────

    public void setRouter(MessageRouter r) { this.router = r; }

    public void start(AntennaSection s) {
        this.cfg = s != null ? s : new AntennaSection();
        if (!cfg.enabled) {
            log.info("AntennaController disabled — not opening port");
            return;
        }
        exec.submit(() -> openPort(cfg));
        running.set(true);
        log.info("AntennaController starting — port={} baud={} switches={} rules={}",
                cfg.comPort, cfg.baud, cfg.switches.size(), cfg.rules.size());
    }

    public void stop() {
        running.set(false);
        exec.submit(this::closePort);
        log.info("AntennaController stopped");
    }

    public void restart(AntennaSection s) {
        stop();
        start(s);
    }

    public boolean isRunning() { return running.get(); }
    public boolean isFaulted() { return faulted.get(); }

    // ── Inputs from MessageRouter ────────────────────────────────────

    /** Called from MessageRouter on every RIG_STATUS — kicks off rule evaluation
     *  if anything actually changed. */
    public void onBandMode(String band, String mode) {
        if (band != null) currentBand = band;
        if (mode != null) currentMode = mode;
        evaluate("rig change");
    }

    /** Called from MessageRouter on every ROTOR_STATUS. */
    public void updateHeading(double heading) {
        currentHeading = heading;
        evaluate("rotor change");
    }

    /** Tracks the rig's PTT state for the lockout-on-transmit safety. */
    public void setPttOn(boolean on) {
        boolean was = pttOn.getAndSet(on);
        if (was && !on) {
            // PTT just dropped — re-run rules so a deferred change applies now.
            evaluate("ptt cleared");
        }
    }

    // ── Manual override ──────────────────────────────────────────────

    public void setOverride(String switchId, int antenna) {
        overrides.put(switchId, antenna);
        evaluate("override set " + switchId + "=" + antenna);
    }

    public void clearOverride(String switchId) {
        overrides.remove(switchId);
        evaluate("override cleared " + switchId);
    }

    public void clearAllOverrides() {
        overrides.clear();
        evaluate("all overrides cleared");
    }

    // ── Rule evaluation ──────────────────────────────────────────────

    /** Evaluate every switch against the current state and emit any needed
     *  serial commands. Runs on the controller's executor. */
    private void evaluate(String reason) {
        if (!running.get()) return;
        exec.submit(() -> {
            if (cfg.lockoutOnPtt && pttOn.get()) {
                publishStatus(true, "PTT active — switching locked out");
                return;
            }
            for (AntennaSwitch sw : cfg.switches) {
                Integer overridden = overrides.get(sw.id);
                int target;
                String why;
                if (overridden != null) {
                    target = overridden;
                    why    = "OVERRIDE";
                } else {
                    AntennaRule m = matchRule(sw.id);
                    if (m == null) continue;     // no rule matched this switch — leave alone
                    target = m.antenna;
                    why    = "rule band=" + m.band
                            + (m.mode.isEmpty() ? "" : "/mode=" + m.mode)
                            + (m.headingMin >= 0 ? "/hdg=" + (int)m.headingMin + "-" + (int)m.headingMax : "");
                }
                Integer cur = currentAntenna.get(sw.id);
                if (cur != null && cur == target) continue;   // already correct, skip
                if (sendSelect(sw, target, m_template(sw.id, target))) {
                    currentAntenna.put(sw.id, target);
                    antennaLog.info("[{}] {} → ant {} ({}; reason: {})",
                            Instant.now(), sw.name, target, why, reason);
                }
            }
            publishStatus(false, reason);
        });
    }

    /** Look up the command template for the given switch — pulls from the
     *  matching rule (so different switches can use different protocols).
     *  Falls back to a sensible default when no rule has a template yet. */
    private String m_template(String switchId, int antenna) {
        for (AntennaRule r : cfg.rules) {
            if (switchId.equals(r.switchId) && r.commandTemplate != null && !r.commandTemplate.isEmpty()) {
                return r.commandTemplate;
            }
        }
        return "SW{switch}={antenna}\\r";
    }

    /** First rule whose conditions match the current state for this switch. */
    AntennaRule matchRule(String switchId) {
        for (AntennaRule r : cfg.rules) {
            if (!switchId.equals(r.switchId)) continue;
            if (r.band != null && !r.band.isEmpty() && !r.band.equalsIgnoreCase(currentBand)) continue;
            if (r.mode != null && !r.mode.isEmpty() && !r.mode.equalsIgnoreCase(currentMode)) continue;
            if (r.headingMin >= 0 && r.headingMax >= 0) {
                if (currentHeading < 0) continue;   // need a heading to match a heading rule
                if (!headingInRange(currentHeading, r.headingMin, r.headingMax)) continue;
            }
            return r;
        }
        return null;
    }

    /** Heading window match — supports wrap-around (e.g. 350°–10° spans north). */
    static boolean headingInRange(double hdg, double lo, double hi) {
        hdg = ((hdg % 360) + 360) % 360;
        lo  = ((lo  % 360) + 360) % 360;
        hi  = ((hi  % 360) + 360) % 360;
        if (lo <= hi) return hdg >= lo && hdg <= hi;
        return hdg >= lo || hdg <= hi;   // wrapped window
    }

    // ── Serial I/O ───────────────────────────────────────────────────

    private void openPort(AntennaSection s) {
        try {
            port = SerialPort.getCommPort(s.comPort);
            port.setComPortParameters(s.baud, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            port.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 1000);
            if (!port.openPort()) {
                throw new RuntimeException("openPort returned false for " + s.comPort);
            }
            faulted.set(false);
            log.info("Antenna serial port opened: {}", s.comPort);
        } catch (Exception e) {
            log.warn("Failed to open antenna serial port {}: {}", s.comPort, e.getMessage());
            faulted.set(true);
            port = null;
        }
    }

    private void closePort() {
        if (port != null) {
            try { port.closePort(); } catch (Exception ignored) {}
            port = null;
        }
    }

    /** Format the command and write it to the serial port. Returns false on error. */
    private boolean sendSelect(AntennaSwitch sw, int antenna, String template) {
        if (port == null) {
            log.debug("Antenna serial port not open — skipping {} → {}", sw.name, antenna);
            return false;
        }
        String cmd = template
                .replace("{switch}",  sw.id)
                .replace("{antenna}", Integer.toString(antenna));
        // Honor literal escapes in the user-supplied template
        cmd = cmd.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t");
        byte[] bytes = cmd.getBytes(StandardCharsets.US_ASCII);
        int written = port.writeBytes(bytes, bytes.length);
        if (written != bytes.length) {
            log.warn("Antenna serial write short ({}/{} bytes) for {} → {}",
                    written, bytes.length, sw.name, antenna);
            faulted.set(true);
            return false;
        }
        faulted.set(false);
        return true;
    }

    // ── Status broadcast ─────────────────────────────────────────────

    private void publishStatus(boolean locked, String reason) {
        if (router == null) return;
        AntennaStatus s = new AntennaStatus();
        s.locked    = locked;
        s.faulted   = faulted.get();
        s.reason    = reason == null ? "" : reason;
        s.timestamp = Instant.now().toString();
        for (AntennaSwitch sw : cfg.switches) {
            int    cur = currentAntenna.getOrDefault(sw.id, 0);
            boolean ov = overrides.containsKey(sw.id);
            s.switches.put(sw.id, new AntennaStatus.SwitchState(
                    sw.name, cur, ov,
                    ov ? "OVERRIDE" : (locked ? "LOCKED" : reason)));
        }
        router.publishAntennaStatus(s);
    }

    /** Snapshot of the current state for the REST status endpoint. */
    public AntennaStatus snapshotStatus() {
        AntennaStatus s = new AntennaStatus();
        s.locked    = cfg.lockoutOnPtt && pttOn.get();
        s.faulted   = faulted.get();
        s.timestamp = Instant.now().toString();
        for (AntennaSwitch sw : cfg.switches) {
            int    cur = currentAntenna.getOrDefault(sw.id, 0);
            boolean ov = overrides.containsKey(sw.id);
            s.switches.put(sw.id, new AntennaStatus.SwitchState(
                    sw.name, cur, ov, ov ? "OVERRIDE" : ""));
        }
        return s;
    }

    /** Visible for testing — exposes the current rule-matching inputs. */
    void __setStateForTest(String band, String mode, double heading) {
        currentBand = band; currentMode = mode; currentHeading = heading;
    }

    /** Visible for testing — replaces config without opening a port. */
    void __setConfigForTest(AntennaSection s) {
        this.cfg = s;
    }
}
