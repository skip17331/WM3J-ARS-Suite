package com.ars.fx.data;

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
            new Spec("amp",   "ampctld", "amp.hamlibModel",   "amp.port",   "amp.baud",
                    "amp.host", "amp.ampctldPort", "4531", "amp.startDaemon"));

    private static final Map<String, Process> PROCS = new ConcurrentHashMap<>();

    /** Spawn any enabled daemon not already running (e.g. at app start). */
    public static void ensureAll() { for (Spec s : SPECS) ensure(s.kind()); }
    /** Kill every daemon we spawned (called on app shutdown so none are left orphaned). */
    public static void stopAll() { for (Spec s : SPECS) stop(s.kind()); }

    /** Spawn the daemon for {@code kind} if its toggle is on, the binary exists, and it isn't already running. */
    public static synchronized void ensure(String kind) {
        Spec s = spec(kind);
        if (s == null || !HubConfig.getBool(s.startKey(), false)) return;
        Process p = PROCS.get(kind);
        if (p != null && p.isAlive()) return;
        String bin = resolveBinary(s.daemon());
        if (bin == null) return;                                  // not installed — leave it to the operator
        String model = HubConfig.get(s.modelKey(), "").trim();
        if (model.isBlank()) return;                              // hamlib model # required for -m
        List<String> argv = new ArrayList<>();
        argv.add(bin); argv.add("-m"); argv.add(model);
        String serial = HubConfig.get(s.serialKey(), "").trim();
        if (!serial.isBlank()) { argv.add("-r"); argv.add(serial); }
        String baud = HubConfig.get(s.baudKey(), "").trim();
        if (!baud.isBlank()) { argv.add("-s"); argv.add(baud); }
        argv.add("-t"); argv.add(HubConfig.get(s.portKey(), s.defaultPort()).trim());
        argv.add("-T"); argv.add(HubConfig.get(s.hostKey(), "127.0.0.1").trim());
        try {
            Process np = new ProcessBuilder(argv).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            PROCS.put(kind, np);
        } catch (Exception ignored) { /* spawn failed — operator runs it manually */ }
    }

    /** Stop the daemon we spawned for {@code kind} (toggle turned off / app exit). */
    public static synchronized void stop(String kind) {
        Process p = PROCS.remove(kind);
        if (p != null) p.destroy();
    }

    public static boolean running(String kind) { Process p = PROCS.get(kind); return p != null && p.isAlive(); }

    /** Toggle handler: spawn when enabled, stop when disabled. */
    public static void setEnabled(String kind, boolean on) { if (on) ensure(kind); else stop(kind); }

    private static Spec spec(String kind) { for (Spec s : SPECS) if (s.kind().equals(kind)) return s; return null; }

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
