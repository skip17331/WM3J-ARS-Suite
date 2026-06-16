package com.ars.fx.data;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Live rig state from a Hamlib <code>rigctld</code> daemon (default
 * 127.0.0.1:4532). Standalone re-implementation of the j-hub HamlibRigController
 * protocol — the parts the J-Hub dashboard's Rig-control drawer needs.
 *
 * <p>Protocol notes carried over from j-hub (load-bearing):
 * <ul>
 *   <li>Every command is sent with the Hamlib EXTENDED-response prefix
 *       <code>+</code> so both get and set replies are field-labelled and
 *       terminated with <code>RPRT x</code>. Without it, plain get-commands
 *       (f, m) return a bare value and no RPRT and the reader blocks.</li>
 *   <li>Host defaults to <code>127.0.0.1</code>, never "localhost": on Windows
 *       Hamlib binds <code>[::1]</code> for "localhost" while Java connects IPv4.</li>
 * </ul>
 *
 * <p>One background daemon thread polls ~every {@value #POLL_MS} ms and pushes a
 * {@link State} to the registered listener on the JavaFX thread. When the daemon
 * is absent the connect fails fast (3 s) and a disconnected State is emitted, so
 * the drawer shows an honest "offline" rather than stale mock values.
 */
public final class RigClient {

    private static final RigClient INSTANCE = new RigClient();
    public static RigClient getInstance() { return INSTANCE; }
    private RigClient() {}

    private static final long POLL_MS = 800;

    /** Default band entry frequencies (Hz) for the band keypad. */
    private static final Map<String,Long> BAND_HZ = new HashMap<>();
    static {
        BAND_HZ.put("160", 1_840_000L);  BAND_HZ.put("80", 3_573_000L);
        BAND_HZ.put("40", 7_074_000L);   BAND_HZ.put("30", 10_136_000L);
        BAND_HZ.put("20", 14_074_000L);  BAND_HZ.put("17", 18_100_000L);
        BAND_HZ.put("15", 21_074_000L);  BAND_HZ.put("12", 24_915_000L);
        BAND_HZ.put("10", 28_074_000L);  BAND_HZ.put("6", 50_313_000L);
    }
    public static Long bandHz(String band) { return BAND_HZ.get(band); }

    /** Immutable rig snapshot. Unknown numerics use sentinel values. */
    public record State(boolean connected, long freqHz, String mode, String vfo,
                        int sMeterDb, double swr, double rfPowerFrac, String error) {
        public static final State OFFLINE = new State(false, 0, "", "", Integer.MIN_VALUE, -1, -1, "");
    }

    // rigctld endpoint: -Drig.host/-Drig.port override, else HubConfig (rig.port is the *serial* device), else default
    private volatile String host = resolveHost();
    private volatile int    port = resolvePort();
    private static String resolveHost() { return System.getProperty("rig.host", HubConfig.get("rig.rigctldHost", "127.0.0.1")); }
    private static int resolvePort() {
        String p = System.getProperty("rig.port");
        try { return p != null ? Integer.parseInt(p.trim()) : Integer.parseInt(HubConfig.get("rig.rigctldPort", "4532").trim()); }
        catch (Exception e) { return 4532; }
    }
    /** Re-read the endpoint from config and drop the socket so the poll loop reconnects. */
    public void reconnect() { host = resolveHost(); port = resolvePort(); closeSocket(); }

    private volatile Consumer<State> listener = s -> {};
    private volatile State last = State.OFFLINE;
    private volatile boolean started = false;

    private Socket socket; private BufferedReader reader; private OutputStream out;

    public State last() { return last; }
    public String endpoint() { return host + ":" + port; }

    /** Register the drawer's callback. Replaces any previous one (drawer rebuilds). */
    public void setListener(Consumer<State> l) { this.listener = l == null ? s -> {} : l; emit(last); }

    /** Start the poll thread (idempotent). */
    public synchronized void start() {
        if (started) return;
        started = true;
        if (RemoteLink.isActive()) return;   // solo-remote: state arrives via applyRemote(), no local rigctld
        Thread t = new Thread(this::loop, "rig-poll");
        t.setDaemon(true);
        t.start();
    }

    /** Push a rig state received from the station (solo-remote mode). */
    public void applyRemote(State s) { if (s != null) emit(s); }

    private void loop() {
        while (true) {
            try { pollOnce(); }
            catch (Exception e) {
                closeSocket();
                emit(new State(false, 0, "", "", Integer.MIN_VALUE, -1, -1, e.getMessage() == null ? "offline" : e.getMessage()));
            }
            try { Thread.sleep(POLL_MS); } catch (InterruptedException ie) { return; }
        }
    }

    private void pollOnce() throws IOException {
        long freq; String mode = ""; String vfo = "";
        // Structural read — get_rig_info gives freq+mode+vfo in one round-trip;
        // fall back to f/m on older Hamlib that rejects it.
        try {
            String info = send("\\get_rig_info");
            long[] fm = new long[]{0};
            String[] mv = parseRigInfo(info, fm);
            freq = fm[0]; mode = mv[0]; vfo = mv[1];
        } catch (IOException rpt) {
            if (!isRprt(rpt)) throw rpt;
            freq = parseFreq(send("f"));
            mode = parseMode(send("m"));
        }
        int sMeter = optInt("\\get_level STRENGTH");
        double swr = optDouble("\\get_level SWR");
        double pwr = optDouble("\\get_level RFPOWER");
        emit(new State(true, freq, mode, vfo, sMeter, swr, pwr, ""));
    }

    // ── Commands (run on the poll thread to serialise socket access) ──────────
    public void setFreqHz(long hz) {
        if (RemoteLink.isActive()) { RemoteLink.get().sendRigFreq(hz); return; }   // forward to the station
        runCmd("F " + hz);
    }
    public void setBand(String band) { Long hz = bandHz(band); if (hz != null) setFreqHz(hz); }
    public void setMode(String mode) {
        if (RemoteLink.isActive()) { RemoteLink.get().sendRigMode(mode); return; }
        runCmd("M " + mode + " 0");
    }

    private void runCmd(String cmd) {
        Thread t = new Thread(() -> { try { send(cmd); } catch (IOException ignored) {} }, "rig-cmd");
        t.setDaemon(true); t.start();
    }

    // ── Optional level reads — an RPRT (unsupported) yields sentinel, not a drop ─
    private int optInt(String cmd) {
        try { double v = parseLevel(send(cmd)); return Double.isNaN(v) ? Integer.MIN_VALUE : (int) Math.round(v); }
        catch (IOException e) { if (isRprt(e)) return Integer.MIN_VALUE; return Integer.MIN_VALUE; }
    }
    private double optDouble(String cmd) {
        try { return parseLevel(send(cmd)); }
        catch (IOException e) { return -1; }
    }

    private void emit(State s) {
        last = s;
        Consumer<State> l = listener;
        try { javafx.application.Platform.runLater(() -> l.accept(s)); }
        catch (IllegalStateException noFx) { /* toolkit not up (tests) */ }
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
        out.write(("+" + cmd + "\n").getBytes(StandardCharsets.US_ASCII));
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

    private static boolean isRprt(IOException e) { return e.getMessage() != null && e.getMessage().startsWith("RPRT"); }

    // ── Parsers (mirror j-hub HamlibRigController) ────────────────────────────
    private static long parseFreq(String r) {
        for (String line : r.split("\n")) {
            String s = line.contains(":") ? line.substring(line.indexOf(':') + 1).trim() : line.trim();
            if (!s.isEmpty()) { try { return (long) Double.parseDouble(s); } catch (NumberFormatException ig) {} }
        }
        return 0;
    }
    private static String parseMode(String r) {
        for (String raw : r.split("\n")) { String s = raw.trim(); if (s.regionMatches(true, 0, "Mode:", 0, 5)) return s.substring(5).trim(); }
        for (String raw : r.split("\n")) { String s = raw.trim(); if (!s.isEmpty() && !s.endsWith(":")) return s; }
        return "";
    }
    /** Fills freqOut[0]; returns {mode, vfo}. */
    private static String[] parseRigInfo(String body, long[] freqOut) {
        String mode = "", vfo = "";
        for (String raw : body.split("\n")) {
            String line = raw.trim();
            if (line.startsWith("VFO=")) {
                Map<String,String> kv = new HashMap<>();
                for (String tok : line.split("\\s+")) { int eq = tok.indexOf('='); if (eq > 0) kv.put(tok.substring(0, eq), tok.substring(eq + 1)); }
                if ("1".equals(kv.get("RX"))) {
                    try { freqOut[0] = (long) Double.parseDouble(kv.getOrDefault("Freq", "0")); } catch (NumberFormatException ig) {}
                    mode = kv.getOrDefault("Mode", "");
                    vfo = kv.getOrDefault("VFO", "");
                }
            }
        }
        return new String[]{mode, vfo};
    }
    private static double parseLevel(String body) {
        double val = Double.NaN;
        for (String raw : body.split("\n")) {
            String s = raw.trim();
            if (s.isEmpty() || s.regionMatches(true, 0, "get_level", 0, 9)) continue;
            try { val = Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return val;
    }

    // ── Display helpers ───────────────────────────────────────────────────────
    /** 14074000 → "14.074.00" (MHz.kHz.10Hz), matching the dashboard readout. */
    public static String fmtFreq(long hz) {
        long mhz = hz / 1_000_000, khz = (hz / 1000) % 1000, tens = (hz / 10) % 100;
        return String.format("%d.%03d.%02d", mhz, khz, tens);
    }
    /** Segments lit (0..16) for a STRENGTH reading in dB relative to S9 (S0≈-54, S9+40≈+40). */
    public static int sMeterSegments(int db) {
        if (db == Integer.MIN_VALUE) return 0;
        double f = (db + 54.0) / 94.0;
        return (int) Math.max(0, Math.min(16, Math.round(f * 16)));
    }
}
