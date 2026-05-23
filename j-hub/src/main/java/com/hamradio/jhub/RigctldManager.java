package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig.RigSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RigctldManager — spawns and supervises a local {@code rigctld} child process
 * on behalf of {@link HamlibRigController}.
 *
 * <p>The suite has always been able to <em>connect</em> to rigctld over TCP,
 * but it expected the operator to have started rigctld themselves in another
 * shell — a step that was invisible from the UI and a recurring source of
 * "won't connect" reports. When {@code rig.manageRigctld} is true, this class
 * owns the daemon: it launches rigctld with the rig's model id, serial port,
 * baud rate, and the suite's chosen TCP listen port, captures its output to
 * the j-hub log, and tears it down on suite shutdown / config restart.
 *
 * <p>When {@code rig.manageRigctld} is false the manager is a no-op and the
 * controller behaves exactly as it used to — i.e. it connects to whatever
 * rigctld is already listening on {@code hamlibHost:hamlibPort}, whether
 * started by hand on the same box or running on another machine.
 */
public class RigctldManager {

    private static final Logger log = LoggerFactory.getLogger(RigctldManager.class);

    private static final RigctldManager INSTANCE = new RigctldManager();
    public static RigctldManager getInstance() { return INSTANCE; }
    private RigctldManager() {}

    // Live process and the args we started it with, kept so a restart() that
    // doesn't actually change anything is a no-op instead of a kill-then-respawn.
    private Process     process;
    private String      currentSignature = "";
    private volatile String lastError    = "";
    // Last rig_open / serial_open complaint scraped from rigctld's stdout. When
    // rigctld is given a bad model/port/baud, it neither exits nor binds the TCP
    // port — it sits alive and writes its complaint to stdout. The controller
    // surfaces this to the operator instead of the generic "Can't reach rigctld"
    // message that would otherwise blame the operator for our spawn's failure.
    private volatile String daemonComplaint = "";

    /**
     * Ensure rigctld is running with the requested configuration. If a process
     * is already running with the same args, leave it alone; otherwise kill
     * the old one and spawn a fresh one. Returns null on success, or a short
     * human-readable error string on failure.
     */
    public synchronized String ensureRunning(RigSection cfg) {
        if (cfg == null || !cfg.manageRigctld) { stop(); return null; }

        String sig = signature(cfg);
        if (process != null && process.isAlive() && sig.equals(currentSignature)) {
            return null; // already up with the right args
        }
        stop();
        daemonComplaint = "";

        String rigctld = DependencyChecker.findRigctld();
        if (rigctld == null) {
            String hint = "rigctld not found — install Hamlib ("
                        + (Platform.isWindows() ? "https://hamlib.github.io/"
                          : Platform.isMac() ? "brew install hamlib"
                                             : "sudo apt install libhamlib-utils") + ")";
            lastError = hint;
            log.warn(hint);
            return hint;
        }
        if (cfg.rigModel <= 0) {
            lastError = "Hamlib Model ID not set — pick a rig in Rig Control";
            return lastError;
        }
        if (cfg.serialPort == null || cfg.serialPort.isBlank()) {
            lastError = "Serial port not set — fill in Rig Control";
            return lastError;
        }

        List<String> argv = new ArrayList<>();
        argv.add(rigctld);
        argv.add("-m"); argv.add(Integer.toString(cfg.rigModel));
        argv.add("-r"); argv.add(cfg.serialPort);
        if (cfg.baudRate > 0) { argv.add("-s"); argv.add(Integer.toString(cfg.baudRate)); }
        argv.add("-t"); argv.add(Integer.toString(cfg.hamlibPort > 0 ? cfg.hamlibPort : 4532));
        // Bind to localhost only; remote daemons should be configured manually.
        argv.add("-T"); argv.add(cfg.hamlibHost != null && !cfg.hamlibHost.isBlank()
                                  ? cfg.hamlibHost : "127.0.0.1");

        try {
            ProcessBuilder pb = new ProcessBuilder(argv).redirectErrorStream(true);
            process = pb.start();
            currentSignature = sig;
            lastError = "";
            log.info("rigctld started — pid {} args {}", process.pid(), String.join(" ", argv));
            startOutputPump(process);
            // Give rigctld ~250 ms to bind the port (typical) before the
            // controller's first TCP connect; if the spawn failed immediately
            // surface that as the error so the UI doesn't show a misleading
            // "Connecting..." until the read times out.
            Thread.sleep(250);
            if (!process.isAlive()) {
                int code = process.exitValue();
                lastError = "rigctld exited immediately (code " + code
                          + ") — check Model ID, serial port and baud";
                log.warn(lastError);
                process = null;
                currentSignature = "";
                return lastError;
            }
            return null;
        } catch (Exception e) {
            lastError = "Failed to start rigctld: " + e.getMessage();
            log.warn(lastError);
            process = null;
            currentSignature = "";
            return lastError;
        }
    }

    /** Tear down the managed rigctld if we started one. Safe to call repeatedly. */
    public synchronized void stop() {
        if (process != null) {
            log.info("Stopping managed rigctld (pid {})", process.pid());
            try { process.descendants().forEach(ProcessHandle::destroyForcibly); } catch (Exception ignored) {}
            process.destroyForcibly();
            process = null;
        }
        currentSignature = "";
    }

    public boolean isRunning() { return process != null && process.isAlive(); }
    public String  getLastError() { return lastError; }
    /** Last rig_open/serial_open complaint scraped from rigctld's stdout, or "" if none. */
    public String  getDaemonComplaint() { return daemonComplaint; }

    // ── internals ────────────────────────────────────────────────────

    private static String signature(RigSection cfg) {
        return cfg.rigModel + "|" + cfg.serialPort + "|" + cfg.baudRate
                + "|" + cfg.hamlibHost + "|" + cfg.hamlibPort;
    }

    // rigctld writes a banner + per-connection chatter; pipe it to the logger
    // at DEBUG so it doesn't drown the console but is available when troubleshooting.
    // Also watch for the specific "rig_open: error = ..." and "serial_open: ..."
    // lines hamlib emits when it can't talk to the radio — those are the real
    // cause behind a "rigctld up but won't accept connections" failure, and the
    // controller needs them to give the operator an honest error message.
    private void startOutputPump(Process p) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    log.debug("[rigctld] {}", line);
                    String trimmed = line.trim();
                    if (trimmed.startsWith("rig_open: error")
                            || trimmed.startsWith("serial_open:")
                            || trimmed.contains("Unable to open")) {
                        daemonComplaint = trimmed;
                    }
                }
            } catch (Exception ignored) {}
        }, "rigctld-stdout");
        t.setDaemon(true);
        t.start();
    }
}
