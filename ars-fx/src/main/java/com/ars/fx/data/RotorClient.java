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
    public String endpoint() { return host + ":" + port; }
    public void setListener(Consumer<State> l) { this.listener = l == null ? s -> {} : l; emit(last); }

    public synchronized void start() {
        if (started) return;
        started = true;
        Thread t = new Thread(this::loop, "rotor-poll");
        t.setDaemon(true);
        t.start();
    }

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
        double el = Math.max(0, last.el());
        runCmd(String.format("P %.1f %.1f", targetAz, el));
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
        out.write((cmd + "\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("RPRT ")) {
                int code; try { code = Integer.parseInt(line.substring(5).trim()); } catch (NumberFormatException e) { code = -1; }
                if (code != 0) throw new IOException("RPRT " + code + " for: " + cmd);
                break;
            }
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
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
