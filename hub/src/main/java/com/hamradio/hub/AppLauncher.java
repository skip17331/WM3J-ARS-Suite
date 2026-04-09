package com.hamradio.hub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AppLauncher — launches and tracks external ham radio applications
 * (hamclock, hamlog) as child processes.
 *
 * Commands are split on whitespace, so paths with spaces must be
 * quoted by the OS shell — advise users to use wrapper scripts if needed.
 */
public class AppLauncher {

    private static final Logger log = LoggerFactory.getLogger(AppLauncher.class);
    private static final AppLauncher INSTANCE = new AppLauncher();

    private final Map<String, Process> processes = new ConcurrentHashMap<>();

    private AppLauncher() {}

    public static AppLauncher getInstance() { return INSTANCE; }

    // ---------------------------------------------------------------
    // Launch / kill
    // ---------------------------------------------------------------

    /**
     * Launch a named app using the given command string.
     * Kills any previously running instance of the same name first.
     *
     * @return null on success, or an error message string on failure
     */
    public String launch(String name, String command) {
        if (command == null || command.isBlank()) {
            return "No command configured for '" + name + "'";
        }
        kill(name); // stop stale instance if any
        try {
            // Run via bash so the command can use shell features (cd, &&, pipes, etc.)
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.inheritIO();
            Process p = pb.start();
            processes.put(name, p);
            log.info("Launched '{}' (pid {}) — {}", name, p.pid(), command);
            return null; // success
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Failed to launch '{}': {}", name, msg);
            return msg;
        }
    }

    /**
     * Kill a running app by name. No-op if not running.
     *
     * @return true if a process was actually killed
     */
    public boolean kill(String name) {
        Process p = processes.remove(name);
        if (p != null && p.isAlive()) {
            p.destroy();
            log.info("Stopped '{}'", name);
            return true;
        }
        return false;
    }

    /** @return true if the named app has a live child process */
    public boolean isRunning(String name) {
        Process p = processes.get(name);
        return p != null && p.isAlive();
    }

    /** Kill all managed processes (called from shutdown hook). */
    public void stopAll() {
        processes.forEach((name, p) -> {
            if (p.isAlive()) {
                p.destroy();
                log.info("Stopped '{}' during shutdown", name);
            }
        });
        processes.clear();
    }
}
