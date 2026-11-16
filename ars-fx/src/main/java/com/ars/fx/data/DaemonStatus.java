package com.ars.fx.data;

import com.google.gson.JsonObject;

/**
 * What the hub's Hamlib daemons ({@code rigctld}, {@code rotctld}, {@code ampctld}) are doing, for the status line under each
 * "Start ... for me" switch in J-Hub's settings.
 *
 * <p>The daemons and the {@link DaemonWatchdog} live in the hub process; the docked app is a client of it. The hub puts
 * {@link #snapshot()} in its once-a-second state message and the client keeps it with {@link #apply}, so the screen shows what the
 * hub is really doing. A process that owns the daemons itself (no hub link) reads them directly.
 */
public final class DaemonStatus {
    private DaemonStatus() {}

    /** How a status line should look: "ok" (running), "warn" (wanted but not running), "off" (the switch is off, or no news yet). */
    public record Line(String level, String text) {}

    private static volatile JsonObject remote;

    /** The state of every daemon the hub manages, for the hub's state message. Cheap: it reads what the watchdog already recorded. */
    public static JsonObject snapshot() {
        JsonObject o = new JsonObject();
        for (String k : DaemonManager.kinds()) {
            JsonObject d = new JsonObject();
            d.addProperty("wanted", DaemonManager.wantedNow(k));
            d.addProperty("running", DaemonManager.running(k));
            d.addProperty("device", DaemonWatchdog.deviceState(k));
            d.addProperty("last", DaemonWatchdog.last(k));
            o.add(k, d);
        }
        return o;
    }

    /** A client keeps the hub's latest daemon state. */
    public static void apply(JsonObject daemons) { remote = daemons; }

    /** The status line for {@code kind} ("rig", "rotor", "rotor2" or "amp"). */
    public static Line line(String kind) {
        if (RemoteLink.isActive()) {
            JsonObject all = remote, d = all != null && all.has(kind) && all.get(kind).isJsonObject() ? all.getAsJsonObject(kind) : null;
            if (d == null) return new Line("off", "waiting for the hub…");
            return describe(bool(d, "wanted"), bool(d, "running"), str(d, "device"), str(d, "last"));
        }
        return describe(DaemonManager.wantedNow(kind), DaemonManager.running(kind), DaemonWatchdog.deviceState(kind), DaemonWatchdog.last(kind));
    }

    /**
     * The words for one daemon. Pure, so they can be tested.
     *
     * @param wanted  its "Start ... for me" switch is on
     * @param running the daemon J-Hub started is running
     * @param device  "ok", "gone" or "" (the serial device the watchdog last saw)
     * @param last    the watchdog's last action for it, or ""
     */
    public static Line describe(boolean wanted, boolean running, String device, String last) {
        if (!wanted) return new Line("off", "Not started by J-Hub (the switch is off)");
        String tail = last == null || last.isBlank() ? "" : "  ·  last: " + last.replaceFirst("^\\w+ daemon: ", "");
        if (running) return new Line("ok", "● Running" + tail);
        if ("gone".equals(device)) return new Line("warn", "○ Waiting for the device to come back; it starts by itself" + tail);
        return new Line("warn", "○ Not running" + tail);
    }

    private static boolean bool(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean(); }
    private static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
}
