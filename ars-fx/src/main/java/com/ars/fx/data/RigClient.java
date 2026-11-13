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
 * <p>One background daemon thread polls every J-Hub ▸ Rig control ▸ Poll interval (800 ms by default) and pushes a
 * {@link State} to the registered listener on the JavaFX thread. When the daemon
 * is absent the connect fails fast (3 s) and a disconnected State is emitted, so
 * the drawer shows an honest "offline" rather than stale mock values.
 */
public final class RigClient {

    private static final RigClient INSTANCE = new RigClient();
    public static RigClient getInstance() { return INSTANCE; }
    private RigClient() {}

    private static final long DEFAULT_POLL_MS = 800;
    /** How often to poll rigctld, from J-Hub ▸ Rig control ▸ Poll interval (250–5000 ms; 800 when empty or not a number). */
    private static long pollMs() {
        try { return Math.max(250, Math.min(5000, Long.parseLong(HubConfig.get("rig.poll", "800").trim()))); }
        catch (NumberFormatException e) { return DEFAULT_POLL_MS; }
    }

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
    public void applyRemote(State s) { if (s != null) emit(guard(s)); }

    /** Frequency, mode and PTT are read every {@code fastMs()}; the meters and level banks on every METER_EVERY-th of those. */
    private static final int METER_EVERY = 4;
    private static long fastMs() { return Math.max(100, pollMs() / 6); }
    /** Released after every command so the poller reads the rig straight away instead of at the end of its sleep. */
    private final java.util.concurrent.Semaphore wake = new java.util.concurrent.Semaphore(0);

    private void loop() {
        while (true) {
            try { pollOnce(); }
            catch (Exception e) {
                closeSocket();
                emit(new State(false, 0, "", "", 0, false, false, 0, Integer.MIN_VALUE, -1, -1, -1,
                        java.util.Map.of(), java.util.Map.of(), e.getMessage() == null ? "offline" : e.getMessage()));
            }
            try { wake.tryAcquire(fastMs(), java.util.concurrent.TimeUnit.MILLISECONDS); wake.drainPermits(); } catch (InterruptedException ie) { return; }
        }
    }

    // levels/funcs read on the slow cadence (every SLOW_EVERY-th poll) — they change rarely
    private static final String[] LEVEL_NAMES = {"AF","RF","RFPOWER","MICGAIN","SQL","NR","NB","PREAMP","ATT","AGC","COMP"};
    private static final String[] FUNC_NAMES  = {"NB","ANF","NR","VOX","LOCK","TUNER","COMP","RIT","XIT"};
    private static final String[] SLOW_ITEMS = slowItems();
    private static String[] slowItems() {
        java.util.List<String> l = new java.util.ArrayList<>();
        for (String n : LEVEL_NAMES) l.add("L:" + n);
        for (String n : FUNC_NAMES) l.add("F:" + n);
        l.add("SPLIT"); l.add("SPLITFREQ"); l.add("RIT_HZ"); l.add("XIT_HZ");
        return l.toArray(new String[0]);
    }
    private int slowIdx = 0;
    // written by the poll thread and (for an immediate on-screen answer) by command callers, so concurrent maps
    private final java.util.Map<String,Double> lvAcc = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String,Boolean> fnAcc = new java.util.concurrent.ConcurrentHashMap<>();
    /** Items to read back at once, ahead of the round-robin: what a command just changed. */
    private final java.util.concurrent.ConcurrentLinkedQueue<String> prio = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private volatile long splitHz = 0;
    private int cycle = 0;
    private volatile java.util.Map<String,Double> slowLevels = java.util.Map.of();
    private volatile java.util.Map<String,Boolean> slowFuncs = java.util.Map.of();
    private volatile boolean slowSplit = false;
    private volatile String lastVfoName = "";
    private volatile int mSmeter = Integer.MIN_VALUE; private volatile double mSwr = -1, mAlc = -1, mPwr = -1;

    private void readItem(String item) {
        if (item.startsWith("L:")) { String n = item.substring(2); double v = optDouble("\\get_level " + n); if (v >= 0) lvAcc.put(n, v); else lvAcc.remove(n); }
        else if (item.startsWith("F:")) { String n = item.substring(2); Boolean f = optBoolOrNull("\\get_func " + n); if (f != null) fnAcc.put(n, f); else fnAcc.remove(n); }
        else if (item.equals("SPLIT")) slowSplit = Boolean.TRUE.equals(parseBool(sendOpt("\\get_split_vfo")));
        else if (item.equals("SPLITFREQ")) { if (slowSplit) { long hz = parseFreq(sendOpt("\\get_split_freq")); if (hz > 0) splitHz = hz; } }
        else if (item.equals("RIT_HZ") || item.equals("XIT_HZ")) {
            String body = sendOpt(item.equals("RIT_HZ") ? "\\get_rit" : "\\get_xit");
            Double v = parseSigned(body);
            if (v != null) lvAcc.put(item, v);
        }
    }
    private void publishBanks() { slowLevels = new java.util.LinkedHashMap<>(lvAcc); slowFuncs = new java.util.LinkedHashMap<>(fnAcc); }
    /** The number in a reply such as "RIT: -120" or "Frequency: 7074000" (sign kept); null when there is none. */
    private static Double parseSigned(String r) {
        for (String raw : r.split("\n")) {
            String t = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1).trim() : raw.trim();
            try { return Double.parseDouble(t); } catch (NumberFormatException ig) { /* next line */ }
        }
        return null;
    }
    private void pollOnce() throws IOException {
        long freq, pb = 0, vfoB = 0; String mode = "", vfo = "";
        // Structural read. By default only the ACTIVE VFO is read (f / m / get_vfo): get_rig_info also reads VFO B, and on some Icoms
        // (the IC-7300 among them) Hamlib does that by swapping the radio to VFO B and back every poll, so the radio flips between A and B
        // and its relays click. Turn "rig.readBothVfos" on only for a rig that reads the other VFO without swapping.
        if (HubConfig.getBool("rig.readBothVfos", false)) {
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
        } else {
            freq = parseFreq(send("f"));
            String m = send("m");
            mode = parseMode(m);
            pb = parsePassband(m);
            if (cycle % METER_EVERY == 0) lastVfoName = parseVfoName(sendOpt("\\get_vfo"));
            vfo = lastVfoName;
        }
        boolean ptt = optBool("t");
        // the meters are slower than the dial: read them on every METER_EVERY-th poll and keep the last values between
        if (cycle % METER_EVERY == 0) {
            mSmeter = optInt("\\get_level STRENGTH");
            mSwr = optDouble("\\get_level SWR");
            mPwr = optDouble("\\get_level RFPOWER");
            mAlc = optDouble("\\get_level ALC");
        }
        int sMeter = mSmeter; double swr = mSwr, pwr = mPwr, alc = mAlc;

        // the level / func banks and split change rarely: read ONE per poll, round and round, so no poll is ever held up by the bank;
        // what a command just changed is read back first, so the panel shows the radio's own answer within a poll or two
        String item;
        int guard = 0;
        while ((item = prio.poll()) != null && guard++ < 6) readItem(item);
        readItem(SLOW_ITEMS[slowIdx++ % SLOW_ITEMS.length]);
        publishBanks();
        cycle++;
        java.util.Map<String,Double> levels = new java.util.LinkedHashMap<>(slowLevels);
        if (pwr >= 0) levels.put("RFPOWER", pwr);   // freshen the fast-read power
        if (slowSplit && splitHz > 0) vfoB = splitHz;      // with split on, "VFO B" is the transmit frequency
        emit(guard(new State(true, freq, mode, vfo, vfoB, slowSplit, ptt, pb, sMeter, swr, alc, pwr, levels, slowFuncs, "")));
    }

    // ── Commands (run on the poll thread to serialise socket access) ──────────
    // The dial the operator just asked for. Shown at once, and kept for a moment against a poll that was already in flight (it would
    // bring the old value back for one frame), and the base for the next nudge so hold-to-repeat steps from where it is going, not from where
    // the last poll saw it.
    private volatile long cmdHz = 0; private volatile long cmdAt = 0;
    private static final long CMD_HOLD_MS = 600;
    private static State withFreq(State s, long hz) {
        return new State(s.connected(), hz, s.mode(), s.vfo(), s.vfoBHz(), s.split(), s.ptt(), s.passbandHz(), s.sMeterDb(), s.swr(), s.alc(),
                s.rfPowerFrac(), s.levels(), s.funcs(), s.error());
    }
    private State guard(State s) {
        long hz = cmdHz;
        if (hz > 0 && s.connected() && s.freqHz() != hz && System.currentTimeMillis() - cmdAt < CMD_HOLD_MS) return withFreq(s, hz);
        return s;
    }
    public void setFreqHz(long hz) {
        cmdHz = hz; cmdAt = System.currentTimeMillis();
        State s = last;
        if (s.connected() && s.freqHz() != hz) emit(withFreq(s, hz));
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
        long base = (cmdHz > 0 && System.currentTimeMillis() - cmdAt < 1500) ? cmdHz : last.freqHz();
        if (base <= 0) return;
        setFreqHz(Math.max(0, base + deltaHz));
    }

    // ── Best-effort CAT for the rig panel's extra controls. These have no
    //    solo-remote forwarder yet, so they no-op over a RemoteLink; locally they
    //    use the Hamlib short verbs (V/S/T/U/L). An unsupported verb just RPRTs. ──
    /** Select the active VFO (Hamlib {@code V}: "VFOA" / "VFOB"). */
    public void setVfo(String vfo)        { cmd("V " + vfo); }
    /** A momentary VFO operation (Hamlib {@code \\vfo_op}: "CPY" = A=B, "XCHG" = A/B swap, …). */
    public void vfoOp(String op)          { cmd("\\vfo_op " + op); }
    /** Enable/disable split (Hamlib {@code S}: on → TX on VFOB, off → VFOA). */
    /** Split on copies VFO A to B first, so the transmit frequency starts equal to the receive frequency (never on some other band). */
    public void setSplit(boolean on) {
        long rx = last.freqHz();
        if (on) { cmd("\\vfo_op CPY"); }
        cmd(on ? "S 1 VFOB" : "S 0 VFOA");
        slowSplit = on; if (on && rx > 0) splitHz = rx;
        republish(java.util.Map.of(), java.util.Map.of(), on, on && rx > 0 ? rx : null);
    }
    /** Set the transmit frequency while split is on. */
    public void setSplitFreqHz(long hz) {
        if (hz <= 0) return;
        splitHz = hz; cmd("I " + hz);
        republish(java.util.Map.of(), java.util.Map.of(), null, hz);
    }
    /** The transmit frequency to step from while split is on (the last one asked for or read). */
    public long splitFreqHz() { long b = last.vfoBHz(); return b > 0 ? b : splitHz; }
    /** RIT / XIT offset in Hz (Hamlib J / Z). */
    public void setRitHz(long hz) { hz = Math.max(-9999, Math.min(9999, hz)); cmd("J " + hz); republish(java.util.Map.of("RIT_HZ", (double) hz), java.util.Map.of(), null, null); }
    public void setXitHz(long hz) { hz = Math.max(-9999, Math.min(9999, hz)); cmd("Z " + hz); republish(java.util.Map.of("XIT_HZ", (double) hz), java.util.Map.of(), null, null); }
    /** Show a changed setting at once, laid over the last state (the radio's own answer follows within a poll and replaces it). */
    private void republish(java.util.Map<String,Double> lvOver, java.util.Map<String,Boolean> fnOver, Boolean splitOver, Long txOver) {
        lvAcc.putAll(lvOver); fnAcc.putAll(fnOver); publishBanks();      // the hub's own bank, read by the next poll anyway
        State s = last;
        if (!s.connected()) return;
        java.util.Map<String,Double> lv = new java.util.LinkedHashMap<>(s.levels() == null ? java.util.Map.of() : s.levels()); lv.putAll(lvOver);
        java.util.Map<String,Boolean> fn = new java.util.LinkedHashMap<>(s.funcs() == null ? java.util.Map.of() : s.funcs()); fn.putAll(fnOver);
        emit(new State(true, s.freqHz(), s.mode(), s.vfo(), txOver != null ? txOver : s.vfoBHz(), splitOver != null ? splitOver : s.split(), s.ptt(),
                s.passbandHz(), s.sMeterDb(), s.swr(), s.alc(), s.rfPowerFrac(), lv, fn, s.error()));
    }
    /** Key/unkey the transmitter (Hamlib {@code T}). */
    public void setPtt(boolean on)        { TxController.manualPtt(on); }   // through the transmit controller: a time limit, and released if the window goes
    /** Toggle a named function — NB, NR, ANF, COMP, … (Hamlib {@code U <name> 0|1}). */
    public void setFunc(String name, boolean on) { cmd("U " + name + " " + (on ? 1 : 0)); republish(java.util.Map.of(), java.util.Map.of(name, on), null, null); }
    /** Set a named level — RFPOWER, AF, RF, SQL, MICGAIN, … 0..1 (Hamlib {@code L <name> <val>}). */
    public void setLevel(String name, double val) { cmd("L " + name + " " + (val == Math.rint(val) ? String.valueOf((long) val) : String.format(java.util.Locale.ROOT, "%.4f", val))); republish(java.util.Map.of(name, val), java.util.Map.of(), null, null); }
    /** Trigger the antenna tuner / start a tune cycle (Hamlib {@code U TUNER 1}). */
    public void antTune()                 { cmd("\\vfo_op TUNE"); }   // starts the tuning cycle; "U TUNER 1" only switches the tuner on

    /** A raw Hamlib command, waiting for the rig's reply (throws if it refuses). Only the process that owns the rig
     *  can use it — a window attached to the hub asks the hub instead (see {@link TxController}). */
    public String command(String cmd) throws IOException {
        if (RemoteLink.isActive()) throw new IOException("this window does not own the rig");
        return send(cmd);
    }

    /** One thread, in order: a held tuning arrow sends a command every 90 ms, and separate threads could land them out of order. */
    private final java.util.concurrent.ExecutorService cmdQueue = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "rig-cmd"); t.setDaemon(true); return t; });
    /** A control command: sent to the station's hub when this window is attached to one, else straight to rigctld. */
    private void cmd(String c) {
        if (RemoteLink.isActive()) { RemoteLink.get().sendRigCmd(c); return; }
        runCmd(c);
    }
    private static final java.util.regex.Pattern ALLOWED_CMD = java.util.regex.Pattern.compile(
            "V VFO[AB]|S [01] VFO[AB]|I [0-9]{4,10}|[JZ] -?[0-9]{1,5}|U [A-Z0-9_]{1,12} [01]|L [A-Z0-9_]{1,12} -?[0-9]{1,6}(\\.[0-9]{1,6})?|\\\\vfo_op [A-Z_]{1,12}");
    /** The hub runs a control command a window asked for, but only the kinds the rig panel sends. */
    public void runForwarded(String c) {
        if (c == null || !ALLOWED_CMD.matcher(c).matches() || !last.connected()) return;
        runCmd(c);
    }
    /** The poll items a command may have changed. */
    private static java.util.List<String> itemsFor(String c) {
        String[] t = c.split("\\s+");
        return switch (t[0]) {
            case "U" -> t.length > 1 ? java.util.List.of("F:" + t[1]) : java.util.List.of();
            case "L" -> t.length > 1 ? java.util.List.of("L:" + t[1]) : java.util.List.of();
            case "S", "V" -> java.util.List.of("SPLIT", "SPLITFREQ");
            case "I" -> java.util.List.of("SPLITFREQ");
            case "J" -> java.util.List.of("RIT_HZ", "F:RIT");
            case "Z" -> java.util.List.of("XIT_HZ", "F:XIT");
            default -> java.util.List.of();
        };
    }
    private void runCmd(String cmd) {
        cmdQueue.execute(() -> {
            try { send(cmd); } catch (IOException ignored) {}
            for (String it : itemsFor(cmd)) prio.add(it);       // read back what this changed
            wake.release();
        });
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

    /** Called on the poll thread with every change, with no JavaFX involved: the hub (which has no UI) pushes it to the windows. */
    private final java.util.List<Consumer<State>> directListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    public void addDirectListener(Consumer<State> l) { if (l != null) directListeners.add(l); }

    private void emit(State s) {
        if (s.equals(last)) return;                 // nothing changed: no repaint, no message
        last = s;
        for (Consumer<State> l : directListeners) { try { l.accept(s); } catch (Exception ignored) {} }
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
    /** The passband width (Hz) from a "m" reply ("Mode: LSB" / "Passband: 2400", or bare "LSB" / "2400"); 0 when absent. */
    private static long parsePassband(String r) {
        long v = 0;
        for (String raw : r.split("\n")) {
            String s = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1).trim() : raw.trim();
            if (s.isEmpty() || !s.chars().allMatch(Character::isDigit)) continue;
            try { v = Long.parseLong(s); } catch (NumberFormatException ig) {}
        }
        return v;
    }
    /** The VFO name from a get_vfo reply ("VFO: VFOA" or "VFOA"); "" when absent. */
    private static String parseVfoName(String r) {
        for (String raw : r.split("\n")) {
            String s = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1).trim() : raw.trim();
            if (s.toUpperCase().startsWith("VFO") || s.equalsIgnoreCase("MAIN") || s.equalsIgnoreCase("SUB")) return s;
        }
        return "";
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
    /** STRENGTH dB (relative to S9) → "S7" / "S9" / "S9+20" reading; "—" when unknown. */
    public static String sMeterText(int db) {
        if (db == Integer.MIN_VALUE) return "—";
        if (db >= 0) return db < 3 ? "S9" : "S9+" + (int) (Math.round(db / 10.0) * 10);
        int s = (int) Math.max(0, Math.min(9, 9 + Math.floor(db / 6.0)));   // 6 dB per S-unit below S9
        return "S" + s;
    }
}
