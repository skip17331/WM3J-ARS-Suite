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
            d.addProperty("lastAt", DaemonWatchdog.lastAt(k));
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
            return describe(bool(d, "wanted"), bool(d, "running"), str(d, "device"), str(d, "last"), ago(num(d, "lastAt")));
        }
        return describe(DaemonManager.wantedNow(kind), DaemonManager.running(kind), DaemonWatchdog.deviceState(kind),
                DaemonWatchdog.last(kind), ago(DaemonWatchdog.lastAt(kind)));
    }

    /** Milliseconds since {@code atMs}, or -1 when it is unknown (0). */
    private static long ago(long atMs) { return atMs <= 0 ? -1 : Math.max(0, System.currentTimeMillis() - atMs); }

    /** "just now", "4 min ago", "2 h 5 min ago", "3 d ago"; "" when unknown. Pure, so it can be tested. */
    public static String agoText(long agoMs) {
        if (agoMs < 0) return "";
        long min = agoMs / 60_000;
        if (min < 1) return "just now";
        if (min < 60) return min + " min ago";
        long h = min / 60;
        if (h < 24) return h + " h" + (min % 60 == 0 ? "" : " " + (min % 60) + " min") + " ago";
        return (h / 24) + " d ago";
    }

    /** As {@link #describe(boolean, boolean, String, String, long)} with no time for the last action. */
    public static Line describe(boolean wanted, boolean running, String device, String last) {
        return describe(wanted, running, device, last, -1);
    }

    /**
     * The words for one daemon. Pure, so they can be tested.
     *
     * @param wanted  its "Start ... for me" switch is on
     * @param running the daemon J-Hub started is running
     * @param device  "ok", "gone", "noaccess" or "" (the serial device the watchdog last saw)
     * @param last    the watchdog's last action for it, or ""
     * @param agoMs   how long ago that action was, or -1 when unknown
     */
    public static Line describe(boolean wanted, boolean running, String device, String last, long agoMs) {
        if (!wanted) return new Line("off", "Not started by J-Hub (the switch is off)");
        String when = agoText(agoMs);
        String tail = last == null || last.isBlank() ? ""
                : "  ·  last" + (when.isEmpty() ? "" : " (" + when + ")") + ": " + last.replaceFirst("^\\w+ daemon: ", "");
        if ("noaccess".equals(device)) return new Line("warn", "✖ No permission to open the serial port. Add your user to the dialout (or uucp) group, then log out and back in" + tail);
        if (running) return new Line("ok", "● Running" + tail);
        if ("gone".equals(device)) return new Line("warn", "○ Waiting for the device to come back; it starts by itself" + tail);
        return new Line("warn", "○ Not running" + tail);
    }

    private static boolean bool(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean(); }
    private static long num(JsonObject o, String k) { try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsLong() : 0; } catch (Exception e) { return 0; } }
    private static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
}
