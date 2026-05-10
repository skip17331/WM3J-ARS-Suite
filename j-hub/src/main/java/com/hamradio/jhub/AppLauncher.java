package com.hamradio.jhub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AppLauncher — launches and tracks external ARS Suite applications
 * (j-map, j-log) as child processes.
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
        kill(name); // stop stale instance if any (tracked in our process map)
        killOrphansMatching(command); // catch survivors from a previous j-hub run
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
            p.descendants().forEach(ProcessHandle::destroyForcibly);
            p.destroyForcibly();
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

    // ---------------------------------------------------------------
    // Orphan sweep — survivors from a previous j-hub session get
    // re-parented to init/cinnamon and become invisible to our process
    // map. Without this sweep, the next Launch fails on port-busy.
    // ---------------------------------------------------------------

    private static final Pattern SCRIPT_LAUNCH =
        Pattern.compile("(/\\S+?)/(?:start\\.sh|run\\.sh|j-\\S+?\\.sh)\\b");
    private static final Pattern JAR_LAUNCH =
        Pattern.compile("(/\\S+/target/)\\S+\\.jar\\b");
    private static final Pattern MVN_LAUNCH =
        Pattern.compile("cd\\s+(/\\S+?)\\s+&&\\s+mvn\\b");

    /**
     * Derive a unique substring of the target jar path from a launch command
     * so we can find a re-parented previous instance running the same jar.
     * Returns null if no usable signature can be extracted.
     */
    static String orphanMarker(String command) {
        Matcher m;
        if ((m = JAR_LAUNCH.matcher(command)).find())    return m.group(1);
        if ((m = SCRIPT_LAUNCH.matcher(command)).find()) return m.group(1) + "/target/";
        if ((m = MVN_LAUNCH.matcher(command)).find())    return m.group(1) + "/target/";
        return null;
    }

    private void killOrphansMatching(String command) {
        String marker = orphanMarker(command);
        if (marker == null) return;
        long myPid = ProcessHandle.current().pid();
        ProcessHandle.allProcesses()
            .filter(ph -> ph.pid() != myPid)
            .filter(ph -> ph.info().commandLine().map(c -> c.contains(marker)).orElse(false))
            .forEach(ph -> {
                log.info("Killing orphan process pid={} matching {}", ph.pid(), marker);
                ph.descendants().forEach(ProcessHandle::destroyForcibly);
                ph.destroyForcibly();
            });
    }

    /** Kill all managed processes (called from shutdown hook). */
    public void stopAll() {
        processes.forEach((name, p) -> {
            if (p.isAlive()) {
                p.descendants().forEach(ProcessHandle::destroyForcibly);
                p.destroyForcibly();
                log.info("Stopped '{}' during shutdown", name);
            }
        });
        processes.clear();
    }
}
