package com.ars.fx.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the Hamlib daemons {@link DaemonManager} started healthy without the operator touching anything.
 *
 * <p>The failure this exists for: a radio's USB link drops and comes back (cable knocked, radio power-cycled, a USB hub reset).
 * The kernel makes a new device node, but the {@code rigctld} we started still holds the old, dead one, so the rig stays offline
 * until someone restarts things by hand. Every few seconds this checks each daemon we own:
 * <ul>
 *   <li>its serial device is gone: stop the daemon (it holds a dead handle) and wait;</li>
 *   <li>the device is back and the daemon is not running (or we stopped it): start it;</li>
 *   <li>the device node is a different one from the one the daemon opened: restart it;</li>
 *   <li>the daemon is alive but has not answered three probes in a row: restart it.</li>
 * </ul>
 * A daemon that keeps failing is restarted at most {@value #MAX_RESTARTS} times in ten minutes, then left alone until the settings change.
 * Clients such as {@code RigClient} already drop their socket and reconnect on their own, so they pick the new daemon up by themselves.
 */
public final class DaemonWatchdog {
    private DaemonWatchdog() {}

    public enum Action { NONE, STOP, START, RESTART }

    static final int MAX_RESTARTS = 5;
    static final long WINDOW_MS = 10 * 60_000L;
    static final long GRACE_MS = 15_000L;       // a new daemon needs a moment before it listens
    static final int FAILED_PROBES = 3;

    /**
     * What to do about one daemon. Pure, so the rules can be tested without hardware.
     *
     * @param procAlive      the daemon we started is running
     * @param deviceApplies  its serial setting is a device path we can watch (a /dev/... node), not a COM port or a network address
     * @param deviceNow      identity of the device node now, or null when it is absent
     * @param deviceAtSpawn  identity of the node when the daemon was started, or null
     * @param sinceSpawnMs   how long the daemon has been running
     * @param failedProbes   consecutive probes it has not answered
     */
    public static Action decide(boolean procAlive, boolean deviceApplies, String deviceNow, String deviceAtSpawn,
                                long sinceSpawnMs, int failedProbes) {
        if (deviceApplies) {
            if (deviceNow == null) return procAlive ? Action.STOP : Action.NONE;          // gone: no stale handle left running
            if (!procAlive) return Action.START;                                         // back (or the daemon died)
            if (deviceAtSpawn != null && !deviceNow.equals(deviceAtSpawn)) return Action.RESTART;   // re-plugged under us
        } else if (!procAlive) {
            return Action.START;                                                         // died
        }
        if (procAlive && sinceSpawnMs > GRACE_MS && failedProbes >= FAILED_PROBES) return Action.RESTART;
        return Action.NONE;
    }

    /** Identity of the device node at {@code serial}: where it really points, its inode and change time. Null when absent or not a path. */
    public static String deviceId(String serial) {
        if (serial == null || !serial.startsWith("/")) return null;
        try {
            Path p = Paths.get(serial);
            if (!Files.exists(p)) return null;
            Path real = p.toRealPath();
            Object ino = Files.getAttribute(real, "unix:ino");
            Object ctime = Files.getAttribute(real, "unix:ctime");
            return real + "#" + ino + "#" + ctime;
        } catch (Exception e) {
            try { return Files.exists(Paths.get(serial)) ? serial : null; } catch (Exception ex) { return null; }   // no unix attributes: presence only
        }
    }

    /** True when {@code serial} is a /dev-style path we can watch. */
    public static boolean watchable(String serial) { return serial != null && serial.startsWith("/"); }

    // ---- the loop ----------------------------------------------------------

    private static Thread thread;
    private static final Map<String, Integer> FAILS = new ConcurrentHashMap<>();
    private static final Map<String, ArrayDeque<Long>> RESTARTS = new ConcurrentHashMap<>();
    private static final Map<String, String> LAST = new ConcurrentHashMap<>();

    /** Start the watchdog thread once (the hub calls this after the daemons are first started). */
    public static synchronized void start() {
        if (thread != null) return;
        thread = new Thread(() -> {
            while (true) {
                try { Thread.sleep(4000); } catch (InterruptedException ie) { return; }
                try { for (String kind : DaemonManager.kinds()) check(kind); }
                catch (Exception e) { System.err.println("[hub] daemon watchdog: " + e); }
            }
        }, "daemon-watchdog");
        thread.setDaemon(true);
        thread.start();
    }

    /** The last thing the watchdog did for {@code kind}, for a status line; empty when it has done nothing. */
    public static String last(String kind) { return LAST.getOrDefault(kind, ""); }

    /** One check of one daemon. */
    static void check(String kind) {
        if (!DaemonManager.wantedNow(kind)) { FAILS.remove(kind); return; }
        String serial = DaemonManager.serialOf(kind);
        boolean watch = watchable(serial);
        boolean alive = DaemonManager.running(kind);
        String now = watch ? deviceId(serial) : null;
        if (alive && watch && now != null) {
            if (probe(kind)) FAILS.put(kind, 0); else FAILS.merge(kind, 1, Integer::sum);
        } else FAILS.put(kind, 0);
        long since = alive ? System.currentTimeMillis() - DaemonManager.spawnedAt(kind) : 0;
        Action a = decide(alive, watch, now, DaemonManager.spawnDeviceId(kind), since, FAILS.getOrDefault(kind, 0));
        switch (a) {
            case NONE -> { }
            case STOP -> { DaemonManager.stop(kind); say(kind, "device " + serial + " is gone: stopped the daemon, waiting for it to come back"); }
            case START -> {
                if (DaemonManager.listening(kind)) break;                      // one we did not start (run by hand) is never touched
                if (allowed(kind)) {
                    DaemonManager.ensure(kind); FAILS.put(kind, 0);
                    if (DaemonManager.running(kind)) say(kind, "device back: started the daemon");
                }
            }
            case RESTART -> { if (allowed(kind)) { DaemonManager.restart(kind); FAILS.put(kind, 0); say(kind, "daemon was stale (device re-plugged or not answering): restarted"); } }
        }
    }

    /** At most MAX_RESTARTS restarts per ten minutes; a config change (DaemonManager.applyAll) clears the count. */
    private static boolean allowed(String kind) {
        ArrayDeque<Long> q = RESTARTS.computeIfAbsent(kind, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) q.pollFirst();
            if (q.size() >= MAX_RESTARTS) {
                if (!"gave up".equals(LAST.get(kind + ".state"))) { LAST.put(kind + ".state", "gave up"); say(kind, "restarted " + MAX_RESTARTS + " times in 10 minutes: leaving it alone until the settings change"); }
                return false;
            }
            q.addLast(now);
            LAST.remove(kind + ".state");
            return true;
        }
    }
    static void resetLimits() { RESTARTS.clear(); LAST.entrySet().removeIf(e -> e.getKey().endsWith(".state")); }

    private static void say(String kind, String msg) {
        String line = kind + " daemon: " + msg;
        LAST.put(kind, line);
        System.out.println("[hub] " + line);
    }

    /** Ask the daemon for something harmless; any reply (even an error) means it is alive. */
    static boolean probe(String kind) {
        String host = DaemonManager.hostOf(kind);
        int port = DaemonManager.portOf(kind);
        String cmd = kind.startsWith("rotor") ? "p\n" : "f\n";
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 1500);
            s.setSoTimeout(2500);
            s.getOutputStream().write(cmd.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            s.getOutputStream().flush();
            return new BufferedReader(new InputStreamReader(s.getInputStream())).readLine() != null;
        } catch (Exception e) { return false; }
    }
}
