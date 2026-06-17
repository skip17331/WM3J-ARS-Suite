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

    /** Immutable rig snapshot. Unknown numerics use sentinel values; {@code levels}/{@code funcs}
     *  hold whatever the rig reported this poll (RFPOWER/AF/RF/MICGAIN/SQL/NR/PREAMP/ATT/AGC and
     *  NB/ANF/NR/VOX/LOCK/TUNER/COMP) — absent keys mean the rig didn't answer. */
    public record State(boolean connected, long freqHz, String mode, String vfo, long vfoBHz, boolean split, boolean ptt,
                        long passbandHz, int sMeterDb, double swr, double alc, double rfPowerFrac,
                        java.util.Map<String,Double> levels, java.util.Map<String,Boolean> funcs, String error) {
        public static final State OFFLINE = new State(false, 0, "", "", 0, false, false, 0,
                Integer.MIN_VALUE, -1, -1, -1, java.util.Map.of(), java.util.Map.of(), "");
        /** A reported level (0..1, or rig units for AGC/PREAMP/ATT), or NaN if unknown. */
        public double level(String k) { Double v = levels == null ? null : levels.get(k); return v == null ? Double.NaN : v; }
        /** A reported function on/off, false if unknown. */
        public boolean func(String k) { return funcs != null && Boolean.TRUE.equals(funcs.get(k)); }
        public boolean has(String k) { return funcs != null && funcs.containsKey(k); }
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

    private final java.util.List<Consumer<State>> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile State last = State.OFFLINE;
    private volatile boolean started = false;

    private Socket socket; private BufferedReader reader; private OutputStream out;

    public State last() { return last; }
    public String endpoint() { return host + ":" + port; }

    /** Subscribe to rig state — the panel, the top-bar RIG readout and the dashboard card
     *  can all listen at once. The last state is delivered immediately. Pair with
     *  {@link #removeListener} (e.g. when the node leaves the scene) to avoid leaks. */
    public void addListener(Consumer<State> l) { if (l != null) { listeners.add(l); l.accept(last); } }
    public void removeListener(Consumer<State> l) { listeners.remove(l); }

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
                emit(new State(false, 0, "", "", 0, false, false, 0, Integer.MIN_VALUE, -1, -1, -1,
                        java.util.Map.of(), java.util.Map.of(), e.getMessage() == null ? "offline" : e.getMessage()));
            }
            try { Thread.sleep(POLL_MS); } catch (InterruptedException ie) { return; }
        }
    }

    // levels/funcs read on the slow cadence (every SLOW_EVERY-th poll) — they change rarely
    private static final String[] LEVEL_NAMES = {"AF","RF","RFPOWER","MICGAIN","SQL","NR","PREAMP","ATT","AGC","COMP"};
    private static final String[] FUNC_NAMES  = {"NB","ANF","NR","VOX","LOCK","TUNER","COMP"};
    private static final int SLOW_EVERY = 4;
    private int cycle = 0;
    private volatile java.util.Map<String,Double> slowLevels = java.util.Map.of();
    private volatile java.util.Map<String,Boolean> slowFuncs = java.util.Map.of();
    private volatile boolean slowSplit = false;

    private void pollOnce() throws IOException {
        long freq, pb = 0, vfoB = 0; String mode = "", vfo = "";
        // Structural read — get_rig_info gives freq+mode+vfo+width (+VFO-B freq) in one round-trip;
        // fall back to f/m on older Hamlib that rejects it.
        try {
            String info = send("\\get_rig_info");
            long[] fm = new long[]{0, 0, 0};
            String[] mv = parseRigInfo(info, fm);
            freq = fm[0]; pb = fm[1]; vfoB = fm[2]; mode = mv[0]; vfo = mv[1];
        } catch (IOException rpt) {
            if (!isRprt(rpt)) throw rpt;
            freq = parseFreq(send("f"));
            mode = parseMode(send("m"));
        }
        // every poll: the fast-moving meters + TX state
        int sMeter = optInt("\\get_level STRENGTH");
        double swr = optDouble("\\get_level SWR");
        double pwr = optDouble("\\get_level RFPOWER");
        double alc = optDouble("\\get_level ALC");
        boolean ptt = optBool("t");

        // slow cadence: the level/func banks + split (rarely change; keep last between refreshes)
        if (cycle % SLOW_EVERY == 0) {
            java.util.Map<String,Double> lv = new java.util.LinkedHashMap<>();
            for (String n : LEVEL_NAMES) { double v = optDouble("\\get_level " + n); if (v >= 0) lv.put(n, v); }
            java.util.Map<String,Boolean> fn = new java.util.LinkedHashMap<>();
            for (String n : FUNC_NAMES) { Boolean b = optBoolOrNull("\\get_func " + n); if (b != null) fn.put(n, b); }
            slowLevels = lv; slowFuncs = fn; slowSplit = Boolean.TRUE.equals(parseBool(sendOpt("\\get_split_vfo")));
        }
        cycle++;
        java.util.Map<String,Double> levels = new java.util.LinkedHashMap<>(slowLevels);
        if (pwr >= 0) levels.put("RFPOWER", pwr);   // freshen the fast-read power
        emit(new State(true, freq, mode, vfo, vfoB, slowSplit, ptt, pb, sMeter, swr, alc, pwr, levels, slowFuncs, ""));
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

    /** Step the dial by {@code deltaHz} from the last known frequency (fast/slow tuning, hold-to-repeat).
     *  Routes through {@link #setFreqHz} so it is forwarded in solo-remote mode too. */
    public void nudgeFreq(long deltaHz) {
        long base = last.freqHz();
        if (base <= 0) return;
        setFreqHz(Math.max(0, base + deltaHz));
    }

    // ── Best-effort CAT for the rig panel's extra controls. These have no
    //    solo-remote forwarder yet, so they no-op over a RemoteLink; locally they
    //    use the Hamlib short verbs (V/S/T/U/L). An unsupported verb just RPRTs. ──
    /** Select the active VFO (Hamlib {@code V}: "VFOA" / "VFOB"). */
    public void setVfo(String vfo)        { if (RemoteLink.isActive()) return; runCmd("V " + vfo); }
    /** A momentary VFO operation (Hamlib {@code \\vfo_op}: "CPY" = A=B, "XCHG" = A/B swap, …). */
    public void vfoOp(String op)          { if (RemoteLink.isActive()) return; runCmd("\\vfo_op " + op); }
    /** Enable/disable split (Hamlib {@code S}: on → TX on VFOB, off → VFOA). */
    public void setSplit(boolean on)      { if (RemoteLink.isActive()) return; runCmd(on ? "S 1 VFOB" : "S 0 VFOA"); }
    /** Key/unkey the transmitter (Hamlib {@code T}). */
    public void setPtt(boolean on)        { if (RemoteLink.isActive()) return; runCmd(on ? "T 1" : "T 0"); }
    /** Toggle a named function — NB, NR, ANF, COMP, … (Hamlib {@code U <name> 0|1}). */
    public void setFunc(String name, boolean on) { if (RemoteLink.isActive()) return; runCmd("U " + name + " " + (on ? 1 : 0)); }
    /** Set a named level — RFPOWER, AF, RF, SQL, MICGAIN, … 0..1 (Hamlib {@code L <name> <val>}). */
    public void setLevel(String name, double val) { if (RemoteLink.isActive()) return; runCmd("L " + name + " " + val); }
    /** Trigger the antenna tuner / start a tune cycle (Hamlib {@code U TUNER 1}). */
    public void antTune()                 { if (RemoteLink.isActive()) return; runCmd("U TUNER 1"); }

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
    /** A get_ptt / get-style boolean — true iff the rig answered "1"; false on any error. */
    private boolean optBool(String cmd) {
        try { Boolean b = parseBool(send(cmd)); return Boolean.TRUE.equals(b); }
        catch (IOException e) { return false; }
    }
    /** A get_func boolean — null when the rig doesn't support it (RPRT), so it isn't recorded. */
    private Boolean optBoolOrNull(String cmd) {
        try { return parseBool(send(cmd)); }
        catch (IOException e) { return null; }
    }
    /** Send, returning "" on any error (for optional structural reads like get_split_vfo). */
    private String sendOpt(String cmd) { try { return send(cmd); } catch (IOException e) { return ""; } }

    private void emit(State s) {
        last = s;
        for (Consumer<State> l : listeners) {
            try { javafx.application.Platform.runLater(() -> l.accept(s)); }
            catch (IllegalStateException noFx) { /* toolkit not up (tests) */ }
        }
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
    /** Fills freqOut[0]=RX freq, [1]=passband width (Hz), [2]=VFO-B freq; returns {mode, vfo}. */
    private static String[] parseRigInfo(String body, long[] freqOut) {
        String mode = "", vfo = "";
        for (String raw : body.split("\n")) {
            String line = raw.trim();
            if (line.startsWith("VFO=")) {
                Map<String,String> kv = new HashMap<>();
                for (String tok : line.split("\\s+")) { int eq = tok.indexOf('='); if (eq > 0) kv.put(tok.substring(0, eq), tok.substring(eq + 1)); }
                if (freqOut.length > 2 && "VFOB".equalsIgnoreCase(kv.get("VFO")))
                    try { freqOut[2] = (long) Double.parseDouble(kv.getOrDefault("Freq", "0")); } catch (NumberFormatException ig) {}
                if ("1".equals(kv.get("RX"))) {
                    try { freqOut[0] = (long) Double.parseDouble(kv.getOrDefault("Freq", "0")); } catch (NumberFormatException ig) {}
                    if (freqOut.length > 1) try { freqOut[1] = (long) Double.parseDouble(kv.getOrDefault("Width", "0")); } catch (NumberFormatException ig) {}
                    mode = kv.getOrDefault("Mode", "");
                    vfo = kv.getOrDefault("VFO", "");
                }
            }
        }
        return new String[]{mode, vfo};
    }
    /** A 0/1 status from a get_func / get_ptt / get_split_vfo reply ("1", "PTT: 1", "Split: 1");
     *  null when no 0/1 token is present. */
    private static Boolean parseBool(String body) {
        Boolean result = null;
        for (String raw : body.split("\n")) {
            String s = raw.trim();
            if (s.isEmpty() || s.regionMatches(true, 0, "get_", 0, 4)) continue;
            if (s.contains(":")) s = s.substring(s.indexOf(':') + 1).trim();
            String tok = s.isEmpty() ? "" : s.split("\\s+")[0];
            if (tok.equals("1")) result = Boolean.TRUE;
            else if (tok.equals("0")) result = Boolean.FALSE;
        }
        return result;
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
    /** 14074000 → "14.074.000" (MHz.kHz.Hz) — the rig panel's full direct-entry form. */
    public static String fmtFreqFull(long hz) {
        return String.format("%d.%03d.%03d", hz / 1_000_000, (hz / 1000) % 1000, hz % 1000);
    }
    /** 14074000 → "14.074" (MHz.kHz) — the compact top-bar RIG readout. */
    public static String fmtFreqShort(long hz) {
        return String.format("%d.%03d", hz / 1_000_000, (hz / 1000) % 1000);
    }
    /** Segments lit (0..16) for a STRENGTH reading in dB relative to S9 (S0≈-54, S9+40≈+40). */
    public static int sMeterSegments(int db) {
        if (db == Integer.MIN_VALUE) return 0;
        double f = (db + 54.0) / 94.0;
        return (int) Math.max(0, Math.min(16, Math.round(f * 16)));
    }
    /** STRENGTH dB (relative to S9) → "S7" / "S9" / "S9+20" reading; "—" when unknown. */
    public static String sMeterText(int db) {
        if (db == Integer.MIN_VALUE) return "—";
        if (db >= 0) return db < 3 ? "S9" : "S9+" + (int) (Math.round(db / 10.0) * 10);
        int s = (int) Math.max(0, Math.min(9, 9 + Math.floor(db / 6.0)));   // 6 dB per S-unit below S9
        return "S" + s;
    }
}
