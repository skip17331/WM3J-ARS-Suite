package com.ars.fx.data;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Live azimuth/elevation from a Hamlib <code>rotctld</code> daemon (default
 * 127.0.0.1:4533). Standalone re-implementation of the j-hub
 * HamlibRotorController protocol for the J-Hub dashboard Rotor-control drawer.
 *
 * <p>rotctld uses the PLAIN line protocol (no <code>+</code> prefix):
 * <code>p</code> → "az\nel\nRPRT 0", <code>P az el</code> slews, <code>S</code>
 * stops. Host defaults to <code>127.0.0.1</code> (never "localhost" — IPv6/IPv4
 * loopback mismatch on Windows).
 */
public final class RotorClient {

    private static final RotorClient INSTANCE = new RotorClient();
    public static RotorClient getInstance() { return INSTANCE; }
    private RotorClient() {}

    private static final long POLL_MS = 700;

    public record State(boolean connected, double az, double el) {
        public static final State OFFLINE = new State(false, 0, 0);
    }

    // rotctld endpoint: -Drotor.host/-Drotor.port override, else HubConfig, else default
    private volatile String host = resolveHost();
    private volatile int    port = resolvePort();
    private static String resolveHost() { return System.getProperty("rotor.host", HubConfig.get("rotor.host", "127.0.0.1")); }
    private static int resolvePort() {
        String p = System.getProperty("rotor.port");
        try { return p != null ? Integer.parseInt(p.trim()) : Integer.parseInt(HubConfig.get("rotor.rotctldPort", "4533").trim()); }
        catch (Exception e) { return 4533; }
    }
    /** Re-read the endpoint from config and drop the socket so the poll loop reconnects. */
    public void reconnect() { host = resolveHost(); port = resolvePort(); closeSocket(); }

    private volatile Consumer<State> listener = s -> {};
    private volatile State last = State.OFFLINE;
    private volatile double targetAz = -1;
    private volatile boolean started = false;

    private Socket socket; private BufferedReader reader; private OutputStream out;

    public State last() { return last; }

    /** True if a rotor appears to be set up: a Hamlib model is configured, or a rotor is actually answering. */
    public boolean isConfigured() {
        String m = HubConfig.get("rotor.hamlibModel", "");
        return (m != null && !m.isBlank()) || last.connected();
    }
    public String endpoint() { return host + ":" + port; }

    /** The antenna's heading, 0 to 360, or NaN when no rotor is answering. Every screen shows this, never its own number. */
    public double headingDeg() { State s = last; return s.connected() ? ((s.az() % 360) + 360) % 360 : Double.NaN; }

    /** Why there is no heading, or that it is live. */
    public String statusText() { return last.connected() ? "live" : isConfigured() ? "rotctld offline" : "no rotor set up"; }
    public void setListener(Consumer<State> l) { this.listener = l == null ? s -> {} : l; emit(last); }

    public synchronized void start() {
        if (started) return;
        started = true;
        if (RemoteLink.isActive()) return;   // solo-remote: az/el arrives via applyRemote(), no local rotctld
        Thread t = new Thread(this::loop, "rotor-poll");
        t.setDaemon(true);
        t.start();
    }

    /** Push a rotor state received from the station (solo-remote mode). */
    public void applyRemote(State s) { if (s != null) emit(s); }

    private void loop() {
        while (true) {
            try {
                double[] pos = parsePos(send("p"));
                emit(new State(true, pos[0], pos[1]));
            } catch (Exception e) {
                closeSocket();
                emit(State.OFFLINE);
            }
            try { Thread.sleep(POLL_MS); } catch (InterruptedException ie) { return; }
        }
    }

    // ── Commands ──────────────────────────────────────────────────────────────
    /** Slew to azimuth (elevation held at current reading). */
    public void moveTo(double az) {
        targetAz = ((az % 360) + 360) % 360;
        if (RemoteLink.isActive()) { RemoteLink.get().sendRotorMove(targetAz); return; }   // forward to the station
        double el = Math.max(0, last.el());
        runCmd(String.format("P %.1f %.1f", targetAz, el));
    }
    /** A beam preset: shown in full on J-Hub ▸ Hardware ▸ Rotor control (where it is edited) and by {@code shortName} on the rotor drawer. */
    public record Preset(String name, String shortName, String key, int defaultAz) {
        /** The azimuth in J-Hub's settings, or the default when it is empty or not a number. */
        public int az() {
            try { return (int) Math.round(((Double.parseDouble(HubConfig.get("rotor.preset." + key, String.valueOf(defaultAz)).trim()) % 360) + 360) % 360); }
            catch (NumberFormatException e) { return defaultAz; }
        }
    }
    public static final java.util.List<Preset> PRESETS = java.util.List.of(
            new Preset("Europe", "EU", "Europe", 50), new Preset("Africa", "AF", "Africa", 95),
            new Preset("S. America", "SA", "SAmerica", 155), new Preset("Caribbean", "Carib", "Caribbean", 135),
            new Preset("Asia (LP)", "AS", "AsiaLP", 330), new Preset("Oceania", "OC", "Oceania", 250));

    /** The park azimuth from J-Hub ▸ Rotor control (0 when not set). */
    public static int parkAz() {
        try { return (int) Math.round(((Double.parseDouble(HubConfig.get("rotor.park", "0").trim()) % 360) + 360) % 360); }
        catch (NumberFormatException e) { return 0; }
    }
    /** The dead-band in degrees from J-Hub ▸ Rotor control: automatic tracking does not bother the rotor for less than this. */
    public static double deadbandDeg() {
        try { return Math.max(0, Double.parseDouble(HubConfig.get("rotor.deadband", "2").trim())); }
        catch (NumberFormatException e) { return 2; }
    }
    /** Automatic tracking (a satellite pass): slew only when the beam is further than the dead-band from {@code az}. */
    public void trackTo(double az) {
        State s = last;
        if (s.connected()) {
            double d = Math.abs((((az - s.az()) % 360) + 540) % 360 - 180);
            if (d < deadbandDeg()) return;
        }
        moveTo(az);
    }

    /** Nudge the current bearing by delta degrees (CCW / CW buttons). */
    public void nudge(double delta) {
        double base = targetAz >= 0 ? targetAz : last.az();
        moveTo(base + delta);
    }
    public void stop() { targetAz = -1; runCmd("S"); }
    public double targetAz() { return targetAz; }

    private void runCmd(String cmd) {
        Thread t = new Thread(() -> { try { send(cmd); } catch (IOException ignored) {} }, "rotor-cmd");
        t.setDaemon(true); t.start();
    }

    private void emit(State s) {
        last = s;
        Consumer<State> l = listener;
        try { javafx.application.Platform.runLater(() -> l.accept(s)); }
        catch (IllegalStateException noFx) { /* toolkit not up */ }
    }

    // ── Socket I/O ────────────────────────────────────────────────────────────
    private void ensureConnected() throws IOException {
        if (socket != null && !socket.isClosed() && socket.isConnected()) return;
        Socket s = new Socket();
        try { s.connect(new InetSocketAddress(host, port), 3000); }
        catch (java.net.SocketTimeoutException te) { try { s.close(); } catch (IOException ig) {} throw new java.net.ConnectException("timed out " + host + ":" + port); }
        s.setSoTimeout(3000);
        socket = s;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        out = socket.getOutputStream();
    }

    private synchronized String send(String cmd) throws IOException {
        ensureConnected();
        while (reader.ready()) reader.readLine();             // a report left over from the last command
        out.write((cmd + "\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
        StringBuilder sb = new StringBuilder();
        String line;
        int values = 0;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("RPRT ")) {
                int code; try { code = Integer.parseInt(line.substring(5).trim()); } catch (NumberFormatException e) { code = -1; }
                if (code != 0) throw new IOException("RPRT " + code + " for: " + cmd);
                break;
            }
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
            // A position reply is two values, with or without a closing RPRT: don't wait for one that may not come.
            if ("p".equals(cmd) && ++values == 2) break;
        }
        return sb.toString().trim();
    }

    private synchronized void closeSocket() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null; reader = null; out = null;
    }

    /** "az\nel" or "Azimuth: az\nElevation: el" → {az, el}. */
    private static double[] parsePos(String response) {
        double az = 0, el = 0; int i = 0;
        for (String raw : response.split("\n")) {
            String s = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1).trim() : raw.trim();
            if (s.isEmpty()) continue;
            try { double v = Double.parseDouble(s); if (i == 0) az = v; else if (i == 1) el = v; i++; } catch (NumberFormatException ignored) {}
        }
        return new double[]{az, el};
    }

    /** Compass bearing → 8-point cardinal label. */
    public static String cardinal(double az) {
        String[] pts = {"N","NE","E","SE","S","SW","W","NW"};
        return pts[(int) Math.round(((az % 360) + 360) % 360 / 45.0) % 8];
    }
}
