package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig.AmpSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * AmpctldManager — spawns and supervises a local {@code ampctld} child
 * process on behalf of {@link HamlibAmpController}. Same shape as
 * {@link RigctldManager} / {@link RotctldManager}.
 *
 * <p>When {@code amp.manageAmpctld} is true, j-hub owns the daemon and
 * spawns it from {@code ampModel/serialPort/baudRate}. When false, the
 * controller connects to whatever ampctld is already listening on
 * {@code tcpHost:tcpPort} — useful for shared amp setups where one ampctld
 * serves multiple operators or sits on a separate machine.
 */
public class AmpctldManager {

    private static final Logger log = LoggerFactory.getLogger(AmpctldManager.class);

    private static final AmpctldManager INSTANCE = new AmpctldManager();
    public static AmpctldManager getInstance() { return INSTANCE; }
    private AmpctldManager() {}

    private Process     process;
    private String      currentSignature = "";
    private volatile String lastError    = "";

    /**
     * Ensure ampctld is running with the requested configuration. Returns
     * null on success, or a short error string. Idempotent against
     * already-correct state.
     */
    public synchronized String ensureRunning(AmpSection cfg) {
        if (cfg == null || !cfg.manageAmpctld) { stop(); return null; }

        String sig = signature(cfg);
        if (process != null && process.isAlive() && sig.equals(currentSignature)) {
            return null;
        }
        stop();

        String ampctld = DependencyChecker.findAmpctld();
        if (ampctld == null) {
            String hint = "ampctld not found — install Hamlib ("
                        + (Platform.isWindows() ? "https://hamlib.github.io/"
                          : Platform.isMac() ? "brew install hamlib"
                                             : "sudo apt install libhamlib-utils") + ")";
            lastError = hint;
            log.warn(hint);
            return hint;
        }
        if (cfg.ampModel <= 0) {
            lastError = "Hamlib Amp Model ID not set — pick an amp in Amp Control";
            return lastError;
        }
        if (cfg.serialPort == null || cfg.serialPort.isBlank()) {
            lastError = "Serial port not set — fill in Amp Control";
            return lastError;
        }

        List<String> argv = new ArrayList<>();
        argv.add(ampctld);
        argv.add("-m"); argv.add(Integer.toString(cfg.ampModel));
        argv.add("-r"); argv.add(cfg.serialPort);
        if (cfg.baudRate > 0) { argv.add("-s"); argv.add(Integer.toString(cfg.baudRate)); }
        argv.add("-t"); argv.add(Integer.toString(cfg.tcpPort > 0 ? cfg.tcpPort : 4531));
        argv.add("-T"); argv.add(cfg.tcpHost != null && !cfg.tcpHost.isBlank()
                                  ? cfg.tcpHost : "127.0.0.1");

        try {
            ProcessBuilder pb = new ProcessBuilder(argv).redirectErrorStream(true);
            process = pb.start();
            currentSignature = sig;
            lastError = "";
            log.info("ampctld started — pid {} args {}", process.pid(), String.join(" ", argv));
            startOutputPump(process);
            Thread.sleep(250);
            if (!process.isAlive()) {
                int code = process.exitValue();
                lastError = "ampctld exited immediately (code " + code
                          + ") — check Amp Model ID, serial port and baud";
                log.warn(lastError);
                process = null;
                currentSignature = "";
                return lastError;
            }
            return null;
        } catch (Exception e) {
            lastError = "Failed to start ampctld: " + e.getMessage();
            log.warn(lastError);
            process = null;
            currentSignature = "";
            return lastError;
        }
    }

    /** Tear down the managed ampctld if we started one. Safe to call repeatedly. */
    public synchronized void stop() {
        if (process != null) {
            log.info("Stopping managed ampctld (pid {})", process.pid());
            try { process.descendants().forEach(ProcessHandle::destroyForcibly); } catch (Exception ignored) {}
            process.destroyForcibly();
            process = null;
        }
        currentSignature = "";
    }

    public boolean isRunning()     { return process != null && process.isAlive(); }
    public String  getLastError()  { return lastError; }

    private static String signature(AmpSection cfg) {
        return cfg.ampModel + "|" + cfg.serialPort + "|" + cfg.baudRate
                + "|" + cfg.tcpHost + "|" + cfg.tcpPort;
    }

    private static void startOutputPump(Process p) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    log.debug("[ampctld] {}", line);
                }
            } catch (Exception ignored) {}
        }, "ampctld-stdout");
        t.setDaemon(true);
        t.start();
    }
}
