package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig;
import com.hamradio.jhub.model.JHubConfig.RotorSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RotctldManager — spawns and supervises a local {@code rotctld} child process
 * on behalf of {@link HamlibRotorController}. Same shape as {@link RigctldManager}.
 *
 * <p>Until 2026-05-20, the suite expected the operator to start {@code rotctld}
 * themselves in another shell with the rotor model, serial port, and baud rate
 * — invisible from the UI and the second-most-common "won't connect" report
 * after the matching rigctld case. When {@code rotor.manageRotctld} is true,
 * this class owns the daemon: spawns it, pipes its output into the log, and
 * tears it down on suite shutdown / config restart.
 *
 * <p>When {@code rotor.manageRotctld} is false the manager is a no-op and the
 * controller behaves as before — connects to whatever rotctld is already
 * listening on {@code tcpHost:tcpPort}.
 */
public class RotctldManager {

    private static final Logger log = LoggerFactory.getLogger(RotctldManager.class);

    private static final RotctldManager INSTANCE = new RotctldManager();
    public static RotctldManager getInstance() { return INSTANCE; }
    private RotctldManager() {}

    private Process     process;
    private String      currentSignature = "";
    private volatile String lastError    = "";

    /**
     * Ensure rotctld is running with the requested configuration. Returns
     * null on success, or a short error string. Idempotent against
     * already-correct state — a save that doesn't change rotor model / port /
     * baud is a no-op instead of a kill-and-respawn.
     */
    public synchronized String ensureRunning(RotorSection cfg) {
        if (cfg == null || !cfg.manageRotctld) { stop(); return null; }

        String sig = signature(cfg);
        if (process != null && process.isAlive() && sig.equals(currentSignature)) {
            return null;
        }
        stop();

        String rotctld = DependencyChecker.findRotctld();
        if (rotctld == null) {
            String hint = "rotctld not found — install Hamlib ("
                        + (Platform.isWindows() ? "https://hamlib.github.io/"
                          : Platform.isMac() ? "brew install hamlib"
                                             : "sudo apt install libhamlib-utils") + ")";
            lastError = hint;
            log.warn(hint);
            return hint;
        }
        if (cfg.rotorModel <= 0) {
            lastError = "Hamlib Rotor Model ID not set — pick a rotor in Rotor Control";
            return lastError;
        }
        if (cfg.serialPort == null || cfg.serialPort.isBlank()) {
            lastError = "Serial port not set — fill in Rotor Control";
            return lastError;
        }

        List<String> argv = new ArrayList<>();
        argv.add(rotctld);
        argv.add("-m"); argv.add(Integer.toString(cfg.rotorModel));
        argv.add("-r"); argv.add(cfg.serialPort);
        if (cfg.baudRate > 0) { argv.add("-s"); argv.add(Integer.toString(cfg.baudRate)); }
        argv.add("-t"); argv.add(Integer.toString(cfg.tcpPort > 0 ? cfg.tcpPort : 4533));
        argv.add("-T"); argv.add(JHubConfig.normalizeHost(cfg.tcpHost));

        try {
            ProcessBuilder pb = new ProcessBuilder(argv).redirectErrorStream(true);
            process = pb.start();
            currentSignature = sig;
            lastError = "";
            log.info("rotctld started — pid {} args {}", process.pid(), String.join(" ", argv));
            startOutputPump(process);
            // Same 250 ms grace period the rigctld manager uses — if the
            // spawn fails immediately we want to surface the exit code now,
            // not after the controller's first TCP connect times out.
            Thread.sleep(250);
            if (!process.isAlive()) {
                int code = process.exitValue();
                lastError = "rotctld exited immediately (code " + code
                          + ") — check Rotor Model ID, serial port and baud";
                log.warn(lastError);
                process = null;
                currentSignature = "";
                return lastError;
            }
            return null;
        } catch (Exception e) {
            lastError = "Failed to start rotctld: " + e.getMessage();
            log.warn(lastError);
            process = null;
            currentSignature = "";
            return lastError;
        }
    }

    /** Tear down the managed rotctld if we started one. Safe to call repeatedly. */
    public synchronized void stop() {
        if (process != null) {
            log.info("Stopping managed rotctld (pid {})", process.pid());
            try { process.descendants().forEach(ProcessHandle::destroyForcibly); } catch (Exception ignored) {}
            process.destroyForcibly();
            process = null;
        }
        currentSignature = "";
    }

    public boolean isRunning()     { return process != null && process.isAlive(); }
    public String  getLastError()  { return lastError; }

    private static String signature(RotorSection cfg) {
        return cfg.rotorModel + "|" + cfg.serialPort + "|" + cfg.baudRate
                + "|" + cfg.tcpHost + "|" + cfg.tcpPort;
    }

    private static void startOutputPump(Process p) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    log.debug("[rotctld] {}", line);
                }
            } catch (Exception ignored) {}
        }, "rotctld-stdout");
        t.setDaemon(true);
        t.start();
    }
}
