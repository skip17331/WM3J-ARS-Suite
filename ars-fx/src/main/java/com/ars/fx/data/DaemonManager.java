package com.ars.fx.data;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawns the Hamlib daemons (rigctld / rotctld / ampctld) on the operator's
 * behalf when "Start … for me" is enabled in the J-Hub config, using the
 * configured model #, serial device, baud and TCP endpoint. Detect-and-advise:
 * if the binary isn't found (or the model # is blank) it quietly does nothing —
 * RigClient/RotorClient/AmpClient then just connect to whatever is running.
 */
public final class DaemonManager {
    private DaemonManager() {}

    /** {kind, daemon, modelKey, serialKey, baudKey, hostKey, portKey, defaultPort, startKey} */
    private record Spec(String kind, String daemon, String modelKey, String serialKey, String baudKey,
                        String hostKey, String portKey, String defaultPort, String startKey) {}

    private static final List<Spec> SPECS = List.of(
            new Spec("rig",   "rigctld", "rig.hamlibModel",   "rig.port",   "rig.baud",
                    "rig.rigctldHost", "rig.rigctldPort", "4532", "rig.startDaemon"),
            new Spec("rotor", "rotctld", "rotor.hamlibModel", "rotor.port", "rotor.baud",
                    "rotor.host", "rotor.rotctldPort", "4533", "rotor.startDaemon"),
            new Spec("rotor2", "rotctld", "rotor2.hamlibModel", "rotor2.port", "rotor2.baud",   // the satellite rotor's own rotctld, on its own port
                    "rotor2.host", "rotor2.rotctldPort", "4534", "rotor2.startDaemon"),
            new Spec("amp",   "ampctld", "amp.hamlibModel",   "amp.port",   "amp.baud",
                    "amp.host", "amp.ampctldPort", "4531", "amp.startDaemon"));

    private static final Map<String, Process> PROCS = new ConcurrentHashMap<>();

    /** Spawn any enabled daemon not already running (e.g. at app start). */
    public static void ensureAll() {
        for (Spec s : SPECS) ensure(s.kind());
        DaemonWatchdog.start();                    // then keep them healthy: stale after a USB drop, not answering ...
    }
    /** Kill every daemon we spawned (called on app shutdown so none are left orphaned). */
    public static void stopAll() { for (Spec s : SPECS) stop(s.kind()); }
    /** Reconcile every daemon to its current config toggle: spawn the enabled ones, stop the disabled ones.
     *  The hub calls this after re-loading config on a client's {@code hub.reconfig} nudge. */
    public static void applyAll() { DaemonWatchdog.resetLimits(); for (Spec s : SPECS) setEnabled(s.kind(), wanted(s)); }

    /** Whether {@code s}'s daemon should be running: its "start for me" switch is on, and for the satellite rotor, the rotor itself is switched on. */
    private static boolean wanted(Spec s) {
        return HubConfig.getBool(s.startKey(), false) && (!s.kind().equals("rotor2") || HubConfig.getBool("rotor2.enabled", false));
    }

    /** The command line we started each daemon with, so a changed setting can be told from "already running". */
    private static final Map<String, List<String>> ARGV = new ConcurrentHashMap<>();
    /** The serial device's identity and the time when each daemon was started, so DaemonWatchdog can tell a device that was re-plugged since. */
    private static final Map<String, String> DEVID = new ConcurrentHashMap<>();
    private static final Map<String, Long> SPAWNED = new ConcurrentHashMap<>();

    /**
     * Spawn the daemon for {@code kind} if its toggle is on and the binary exists. A daemon we started earlier is left
     * alone while its command line is unchanged, and restarted when a setting that goes on it (model, port, baud, PTT
     * type …) has changed. One we did not start — anything already listening on the endpoint — is never touched.
     */
    public static synchronized void ensure(String kind) {
        Spec s = spec(kind);
        if (s == null || !wanted(s)) return;
        String bin = resolveBinary(s.daemon());
        List<String> argv = bin == null ? null : buildArgv(s, bin);
        Process p = PROCS.get(kind);
        if (p != null && p.isAlive()) {
            if (argv == null || argv.equals(ARGV.get(kind))) return;          // unchanged (or can't be rebuilt) — keep it
            stop(kind);                                                       // settings changed: restart with the new ones
            try { p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        } else {
            // Cross-process backstop: if something is already listening on the daemon's endpoint (our own daemon
            // owned by another JVM, or an externally-run daemon), never spawn a second one to fight over the port.
            String host = HubConfig.get(s.hostKey(), "127.0.0.1").trim();
            if (portOpen(host, parsePort(HubConfig.get(s.portKey(), s.defaultPort()), s.defaultPort()))) return;
        }
        if (argv == null) return;                                 // not installed, or no Hamlib model # (needed for -m)
        String serial = HubConfig.get(s.serialKey(), "").trim();
        String devNow = DaemonWatchdog.deviceId(serial);
        if (DaemonWatchdog.watchable(serial) && devNow == null) return;   // its device is not there (radio off / cable out): DaemonWatchdog starts it when it returns
        try {
            Process np = new ProcessBuilder(argv).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            PROCS.put(kind, np);
            ARGV.put(kind, argv);
            DEVID.put(kind, devNow == null ? "" : devNow);
            SPAWNED.put(kind, System.currentTimeMillis());
        } catch (Exception ignored) { /* spawn failed — operator runs it manually */ }
    }

    /** The only /dev/ttyUSB* or /dev/ttyACM* node present, else "" (none or ambiguous — the operator picks one). */
    private static String soleSerialPort() {
        java.io.File[] f = new java.io.File("/dev").listFiles((d, n) -> n.startsWith("ttyUSB") || n.startsWith("ttyACM"));
        return f != null && f.length == 1 ? f[0].getPath() : "";
    }

    /** The command line for {@code s}'s daemon from the current settings, or null when there is no Hamlib model number. */
    private static List<String> buildArgv(Spec s, String bin) {
        String model = HubConfig.get(s.modelKey(), "").trim();
        if (model.isBlank()) return null;
        List<String> argv = new ArrayList<>();
        argv.add(bin); argv.add("-m"); argv.add(model);
        String serial = HubConfig.get(s.serialKey(), "").trim();
        if (serial.isBlank() && s.kind().equals("rig")) serial = soleSerialPort();   // fresh install: the one USB serial device present
        if (!serial.isBlank()) { argv.add("-r"); argv.add(serial); }
        String baud = HubConfig.get(s.baudKey(), "").trim();
        if (!baud.isBlank()) { argv.add("-s"); argv.add(baud); }
        argv.add("-t"); argv.add(HubConfig.get(s.portKey(), s.defaultPort()).trim());
        argv.add("-T"); argv.add(HubConfig.get(s.hostKey(), "127.0.0.1").trim());
        if (s.kind().equals("rig")) {
            // J-Hub's PTT method: CAT keys over the CAT link (the default); RTS / DTR key a control line of the serial
            // port, which rigctld only does when told the PTT type and device. VOX needs no keying from the daemon.
            String ptt = HubConfig.get("rig.ptt", "CAT").trim().toUpperCase();
            if ((ptt.equals("RTS") || ptt.equals("DTR")) && !serial.isBlank()) {
                argv.add("-P"); argv.add(ptt);
                argv.add("-p"); argv.add(serial);
            }
        }
        return argv;
    }

    /** The command line {@code kind}'s daemon would be started with now (as a path-less program name), or null. For tests. */
    static List<String> argvFor(String kind) {
        Spec s = spec(kind);
        return s == null ? null : buildArgv(s, s.daemon());
    }

    /** Stop the daemon we spawned for {@code kind} (toggle turned off / app exit). */
    public static synchronized void stop(String kind) {
        Process p = PROCS.remove(kind);
        ARGV.remove(kind); DEVID.remove(kind); SPAWNED.remove(kind);
        if (p != null) p.destroy();
    }

    /** Stop and start {@code kind}'s daemon again (the watchdog's repair). */
    public static synchronized void restart(String kind) {
        Process p = PROCS.get(kind);
        stop(kind);
        if (p != null) try { p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        ensure(kind);
    }

    // ---- what DaemonWatchdog needs to see ------------------------------------
    static java.util.List<String> kinds() { java.util.List<String> k = new ArrayList<>(); for (Spec s : SPECS) k.add(s.kind()); return k; }
    static boolean wantedNow(String kind) { Spec s = spec(kind); return s != null && wanted(s); }
    static String serialOf(String kind) { Spec s = spec(kind); return s == null ? "" : HubConfig.get(s.serialKey(), "").trim(); }
    static String hostOf(String kind) { Spec s = spec(kind); return s == null ? "127.0.0.1" : HubConfig.get(s.hostKey(), "127.0.0.1").trim(); }
    static int portOf(String kind) { Spec s = spec(kind); return s == null ? 0 : parsePort(HubConfig.get(s.portKey(), s.defaultPort()), s.defaultPort()); }
    /** Something is already answering on the daemon's endpoint (ours, or one started by hand). */
    static boolean listening(String kind) { Spec s = spec(kind); return s != null && portOpen(hostOf(kind), portOf(kind)); }
    static String spawnDeviceId(String kind) { String d = DEVID.get(kind); return d == null || d.isEmpty() ? null : d; }
    static long spawnedAt(String kind) { return SPAWNED.getOrDefault(kind, System.currentTimeMillis()); }

    public static boolean running(String kind) { Process p = PROCS.get(kind); return p != null && p.isAlive(); }

    /** Toggle handler: spawn when enabled, stop when disabled. */
    public static void setEnabled(String kind, boolean on) { if (on) ensure(kind); else stop(kind); }

    private static Spec spec(String kind) { for (Spec s : SPECS) if (s.kind().equals(kind)) return s; return null; }

    private static int parsePort(String v, String def) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return Integer.parseInt(def); }
    }
    private static boolean portOpen(String host, int port) {
        try (Socket s = new Socket()) { s.connect(new InetSocketAddress(host, port), 200); return true; }
        catch (Exception e) { return false; }
    }

    /** Locate a hamlib daemon: explicit override, common install dirs, then PATH. */
    static String resolveBinary(String daemon) {
        String override = System.getProperty(daemon + ".bin", HubConfig.get(daemon + ".bin", ""));
        if (!override.isBlank() && Files.isExecutable(Paths.get(override))) return override;
        for (String cand : new String[]{"/usr/bin/" + daemon, "/usr/local/bin/" + daemon, "/opt/homebrew/bin/" + daemon,
                "C:\\Program Files\\Hamlib\\bin\\" + daemon + ".exe"})
            if (Files.isExecutable(Paths.get(cand))) return cand;
        try {
            boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
            Process p = new ProcessBuilder(win ? "where" : "which", daemon).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            String first = out.lines().findFirst().orElse("").trim();
            if (!first.isBlank() && Files.isExecutable(Paths.get(first))) return first;
        } catch (Exception ignored) { }
        return null;
    }
}
