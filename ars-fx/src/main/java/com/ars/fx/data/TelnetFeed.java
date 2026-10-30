package com.ars.fx.data;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A line-oriented Telnet feed (DX cluster, RBN) with ONE status model for every module:
 * {@link Status#DISCONNECTED} (nobody wants a connection), {@link Status#CONNECTING} (a connection is wanted and
 * being established — including login and reconnect back-off) and {@link Status#CONNECTED} (logged in and the
 * server is talking).
 *
 * <p>The hub process owns the socket. A window attached to the hub ({@link RemoteLink}) runs no socket at all:
 * it mirrors the hub's status and raw log, and {@link #connect()}, {@link #disconnect()},
 * {@link #setEndpoint(String, int, String)} and {@link #sendCommand(String)} are forwarded to the hub.
 *
 * <p>Everything sent and received is kept in a bounded raw log (for the console popup).
 */
public abstract class TelnetFeed {

    public enum Status {
        DISCONNECTED("Disconnected"), CONNECTING("Connecting…"), CONNECTED("Connected");
        private final String label;
        Status(String label) { this.label = label; }
        public String label() { return label; }
    }

    /** One raw log line; {@code seq} counts up for the life of the owning process. */
    public record RawLine(long seq, String text) {}

    private static final int RAW_MAX = 600;
    private static final int MAX_BACKOFF = 60;
    private static final int PROMPT_WAIT_MS = 2500, SILENT_LIMIT_MS = 25_000, READ_TICK_MS = 500;

    /** Hub-process start time, so a client can tell the hub restarted (its seq numbers start over). */
    private static final long EPOCH = System.currentTimeMillis();

    private final String key, threadName;
    protected volatile String host;
    protected volatile int port;
    protected volatile String call;

    private volatile boolean paused;                 // true = the operator does not want a connection
    private volatile Status status = Status.DISCONNECTED;
    private volatile String detail = "";
    private volatile boolean started;
    private int backoff = 5;
    private final Object wake = new Object();
    private int attempts;                            // failed attempts since the operator last pressed Connect (or a success)
    private volatile boolean kick;                   // set by connect()/setEndpoint(): skip any back-off and retry now

    private volatile Socket socket;
    private volatile OutputStream out;

    private final Deque<RawLine> raw = new ArrayDeque<>();
    private long rawSeq;
    private long rawEpoch = EPOCH;                   // hub's epoch as last seen (client side)
    private volatile int rawResets;                  // bumps when the raw log is cleared, so a console can start over

    protected TelnetFeed(String key, String threadName, String host, int port, String call, boolean autoConnect) {
        this.key = key; this.threadName = threadName;
        this.host = host; this.port = port; this.call = call;
        this.paused = !autoConnect;
        this.status = Status.DISCONNECTED;
    }

    // ── what subclasses supply ──────────────────────────────────────────────
    /** A complete line from the server (never empty), after login. */
    protected abstract void onLine(String line);
    /** The endpoint changed — drop spots belonging to the old one. */
    protected abstract void onEndpointChanged();
    /** Tell the UI (on the FX thread) that status or data changed. */
    protected abstract void fire();

    // ── status ──────────────────────────────────────────────────────────────
    public Status status() { return status; }
    public boolean isConnected() { return status == Status.CONNECTED; }
    /** True when a connection is wanted (CONNECTING or CONNECTED), false when DISCONNECTED by choice. */
    public boolean wantConnected() { return !paused; }
    /** Short human detail: "login sent…", "connection refused — retrying in 10 s", … */
    public String detail() { return detail; }
    public String endpoint() { return host; }
    public int port() { return port; }
    public String loginCall() { return call; }
    public String key() { return key; }

    /** Most failed attempts before giving up, from J-Hub ▸ Data ({@code data.<key>MaxTries}); 0 = never give up. */
    public int maxTries() {
        try { return Math.max(0, Integer.parseInt(HubConfig.get("data." + key + "MaxTries", "5").trim())); }
        catch (Exception e) { return 5; }
    }

    private void setStatus(Status s, String d) {
        boolean changed = status != s || !d.equals(detail);
        status = s; detail = d;
        if (changed) fire();
    }

    // ── lifecycle ───────────────────────────────────────────────────────────
    /** Start the connection loop (hub / standalone). A window attached to the hub mirrors instead. */
    public synchronized void start() {
        if (started) return;
        started = true;
        if (RemoteLink.isActive()) return;
        Thread t = new Thread(this::loop, threadName);
        t.setDaemon(true);
        t.start();
    }

    /** A window can only drive the feed through the hub; say so plainly when the hub link is down. */
    private boolean hubReachable() {
        if (RemoteLink.get() != null && RemoteLink.get().isConnected()) return true;
        paused = true; setStatus(Status.DISCONNECTED, "not connected to the hub — nothing to " + key + " can be done");
        return false;
    }

    /** Operator Connect. */
    public void connect() {
        if (RemoteLink.isActive()) {
            if (!hubReachable()) return;
            RemoteLink.get().sendFeed(key, "connect", null);
            paused = false; setStatus(Status.CONNECTING, "asking the hub to connect…");
            return;
        }
        backoff = 5; attempts = 0; paused = false; kick = true;
        setStatus(Status.CONNECTING, "connecting to " + host + ":" + port + "…");
        wakeUp();
    }

    /** Operator Disconnect — stop reading and don't auto-reconnect. */
    public void disconnect() {
        if (RemoteLink.isActive()) {
            if (!hubReachable()) return;
            RemoteLink.get().sendFeed(key, "disconnect", null);
            paused = true; setStatus(Status.DISCONNECTED, "");
            return;
        }
        paused = true;
        closeSocket();
        setStatus(Status.DISCONNECTED, "");
        wakeUp();
    }

    /** Point at a new server/login. Reconnects only if the endpoint really changed and a connection is wanted. */
    public void setEndpoint(String h, int p, String c) {
        String nh = (h == null || h.isBlank()) ? host : h.trim();
        int np = p > 0 ? p : port;
        String nc = (c == null || c.isBlank()) ? call : c.trim().toUpperCase();
        if (RemoteLink.isActive()) {
            JsonObject o = new JsonObject(); o.addProperty("host", nh); o.addProperty("port", np); o.addProperty("call", nc);
            if (hubReachable()) RemoteLink.get().sendFeed(key, "endpoint", o);
            return;
        }
        if (nh.equals(host) && np == port && nc.equals(call)) return;
        host = nh; port = np; call = nc;
        onEndpointChanged();
        backoff = 5; attempts = 0;
        addRaw("* endpoint now " + host + ":" + port + " as " + call);
        kick = true;
        closeSocket();                       // the loop reconnects if a connection is wanted
        if (!paused) setStatus(Status.CONNECTING, "connecting to " + host + ":" + port + "…");
        wakeUp();
    }

    /** Send a line to the server (the console's command line). */
    public void sendCommand(String text) {
        if (text == null || text.isBlank()) return;
        if (RemoteLink.isActive()) {
            JsonObject o = new JsonObject(); o.addProperty("text", text);
            if (hubReachable()) RemoteLink.get().sendFeed(key, "cmd", o);
            return;
        }
        OutputStream o = out;
        if (o == null || status == Status.DISCONNECTED) { addRaw("* not connected — \"" + text + "\" not sent"); return; }
        try { o.write((text + "\r\n").getBytes(StandardCharsets.ISO_8859_1)); o.flush(); addRaw("> " + text); }
        catch (IOException e) { addRaw("* send failed: " + e.getMessage()); }
    }

    // ── connection loop ─────────────────────────────────────────────────────
    private void loop() {
        while (true) {
            if (paused) { setStatus(Status.DISCONNECTED, ""); waitMs(300); continue; }
            String why;
            try {
                setStatus(Status.CONNECTING, "connecting to " + host + ":" + port + "…");
                addRaw("* connecting to " + host + ":" + port);
                session();
                why = "the server closed the connection";
            } catch (Exception e) {
                why = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                if (e instanceof java.net.ConnectException) why = "connection refused";
                else if (e instanceof java.net.UnknownHostException) why = "unknown host " + host;
                else if (e instanceof SocketTimeoutException && e.getMessage() != null && e.getMessage().contains("connect")) why = "connection timed out";
            }
            closeSocket();
            if (paused) continue;
            attempts++;
            int max = maxTries();
            if (max > 0 && attempts >= max) {                    // out of tries: stop, and say so plainly
                paused = true;
                String msg = "gave up after " + attempts + " tries (" + why + ") — press Connect to try again";
                addRaw("* " + msg);
                setStatus(Status.DISCONNECTED, msg);
                attempts = 0;
                continue;
            }
            int wait = backoff;
            String of = max > 0 ? " (try " + attempts + " of " + max + ")" : " (try " + attempts + ")";
            addRaw("* " + why + " — retrying in " + wait + " s" + of);
            for (int left = wait; left > 0 && !paused && !kick; left--) {
                setStatus(Status.CONNECTING, why + " — retrying in " + left + " s" + of);
                waitMs(1000);
            }
            kick = false;
            backoff = Math.min(backoff * 2, MAX_BACKOFF);
        }
    }

    /** One connection: connect, wait for the login prompt, log in, then read until closed or paused. */
    private void session() throws IOException {
        Socket s = new Socket();
        socket = s;                         // published BEFORE connecting, so Stop/Disconnect can abort an attempt in flight
        s.connect(new InetSocketAddress(host, port), 8000);
        if (paused || socket != s) { try { s.close(); } catch (Exception ignored) {} return; }
        s.setSoTimeout(READ_TICK_MS);
        out = s.getOutputStream();
        InputStream in = s.getInputStream();
        addRaw("* connected to " + host + ":" + port + " — waiting for the login prompt");
        setStatus(Status.CONNECTING, "logging in as " + call + "…");

        StringBuilder cur = new StringBuilder();
        byte[] buf = new byte[2048];
        long t0 = System.currentTimeMillis();
        int logins = 0;                 // how many times we have answered a login prompt (some servers ask twice)
        boolean answered = false;       // the server has sent something since our (last) login
        long loginAt = 0;

        while (!paused && socket == s) {
            int n;
            try { n = in.read(buf); }
            catch (SocketTimeoutException tick) {
                long now = System.currentTimeMillis();
                if (cur.length() > 0) {                              // a prompt has no newline: treat what we have as a line
                    String p = clean(cur); cur.setLength(0);
                    if (!p.isEmpty()) {
                        addRaw(p);
                        if (looksLikePrompt(p) && logins < 3) { sendLogin(); logins++; loginAt = now; answered = false; }
                        else if (logins > 0) { if (!answered) { answered = true; backoff = 5; attempts = 0; setStatus(Status.CONNECTED, ""); } onLine(p.trim()); }
                    }
                }
                if (logins == 0 && now - t0 > PROMPT_WAIT_MS) { sendLogin(); logins++; loginAt = now; answered = false; }   // silent server: send the call anyway
                // Connected only means the server has answered after our login. A server that stays silent is not connected.
                if (logins > 0 && !answered && now - loginAt > SILENT_LIMIT_MS) throw new IOException("the server did not answer the login");
                continue;
            }
            if (n < 0) return;                                    // server closed
            for (int i = 0; i < n; i++) {
                int b = buf[i] & 0xFF;
                if (b == 0xFF) { i = telnetOption(buf, i, n, s.getOutputStream()); continue; }   // IAC negotiation — skip (and decline)
                if (b == '\n') {
                    String line = clean(cur); cur.setLength(0);
                    if (line.isEmpty()) continue;
                    addRaw(line);
                    if (line.regionMatches(true, 0, "DX de", 0, 5)) {                      // a spot: we are certainly logged in and live
                        answered = true; if (logins == 0) logins = 1;
                        if (status != Status.CONNECTED) { backoff = 5; attempts = 0; setStatus(Status.CONNECTED, ""); }
                        onLine(line.trim());
                    } else if (looksLikePrompt(line) && logins < 3) { sendLogin(); logins++; loginAt = System.currentTimeMillis(); answered = false; }
                    else if (logins > 0) { if (!answered) { answered = true; backoff = 5; attempts = 0; setStatus(Status.CONNECTED, ""); } onLine(line.trim()); }
                } else if (b != '\r') cur.append((char) b);
            }
        }
    }

    private void sendLogin() throws IOException {
        OutputStream o = out;
        if (o == null) return;
        o.write((call + "\r\n").getBytes(StandardCharsets.ISO_8859_1)); o.flush();
        addRaw("> " + call);
        setStatus(Status.CONNECTING, "login sent as " + call + " — waiting for the server…");
    }

    private static boolean looksLikePrompt(String s) {
        if (s.length() > 80 || s.regionMatches(true, 0, "DX de", 0, 5)) return false;   // spots and long text are never prompts
        String l = s.toLowerCase();
        return l.contains("login:") || l.contains("call:") || l.contains("callsign:") || l.contains("enter your call") || l.endsWith("login");
    }

    /** Skip a Telnet option sequence starting at an IAC byte; decline WILL/DO. Returns the index of its last byte. */
    private static int telnetOption(byte[] b, int i, int n, OutputStream o) {
        try {
            if (i + 1 >= n) return i;
            int cmd = b[i + 1] & 0xFF;
            if (cmd >= 251 && cmd <= 254) {                       // WILL / WONT / DO / DONT <opt>
                if (i + 2 >= n) return i + 1;
                int opt = b[i + 2] & 0xFF;
                if (cmd == 253) o.write(new byte[]{(byte) 0xFF, (byte) 252, (byte) opt});      // DO   → WONT
                else if (cmd == 251) o.write(new byte[]{(byte) 0xFF, (byte) 254, (byte) opt}); // WILL → DONT
                o.flush();
                return i + 2;
            }
            if (cmd == 250) { int j = i + 2; while (j + 1 < n && !((b[j] & 0xFF) == 0xFF && (b[j + 1] & 0xFF) == 240)) j++; return Math.min(j + 1, n - 1); }
            return i + 1;
        } catch (IOException e) { return i + 1; }
    }

    private static String clean(CharSequence cs) {
        StringBuilder sb = new StringBuilder(cs.length());
        for (int i = 0; i < cs.length(); i++) { char c = cs.charAt(i); if (c == '\t' || (c >= 32 && c != 127)) sb.append(c); }
        return sb.toString();
    }

    private void closeSocket() {
        Socket s = socket;
        socket = null; out = null;
        try { if (s != null) s.close(); } catch (Exception ignored) {}   // close only the socket; it unblocks the reader
    }

    private void waitMs(long ms) {
        synchronized (wake) { try { wake.wait(ms); } catch (InterruptedException ignored) {} }
    }
    private void wakeUp() { synchronized (wake) { wake.notifyAll(); } }

    // ── raw log ─────────────────────────────────────────────────────────────
    private synchronized void addRaw(String text) {
        raw.addLast(new RawLine(++rawSeq, text));
        while (raw.size() > RAW_MAX) raw.removeFirst();
    }
    /** Lines with {@code seq} greater than the one given (0 = everything held). */
    public synchronized List<RawLine> rawSince(long seq) {
        List<RawLine> out = new ArrayList<>();
        for (RawLine r : raw) if (r.seq() > seq) out.add(r);
        return out;
    }
    public synchronized long rawSeq() { return rawSeq; }
    /** Bumps when the raw log was cleared/replaced; a console that sees it change starts over. */
    public int rawResets() { return rawResets; }
    public synchronized void clearRaw() { raw.clear(); rawResets++; }

    // ── hub ⇄ window mirroring ──────────────────────────────────────────────
    /** Hub side: this feed's status for the broadcast snapshot. */
    public JsonObject statusJson() {
        JsonObject o = new JsonObject();
        o.addProperty("status", status.name());
        o.addProperty("want", !paused);
        o.addProperty("host", host); o.addProperty("port", port); o.addProperty("call", call);
        o.addProperty("detail", detail);
        return o;
    }

    /** Window side: adopt the hub's status. */
    public void applyRemoteStatus(JsonObject o) {
        try {
            Status s = Status.valueOf(o.get("status").getAsString());
            boolean want = o.get("want").getAsBoolean();
            String d = o.has("detail") ? o.get("detail").getAsString() : "";
            String h = o.get("host").getAsString(); int p = o.get("port").getAsInt(); String c = o.get("call").getAsString();
            boolean changed = s != status || want == paused || !d.equals(detail) || !h.equals(host) || p != port || !c.equals(call);
            host = h; port = p; call = c; paused = !want; status = s; detail = d;
            if (changed) fire();
        } catch (Exception ignored) { /* malformed */ }
    }

    /** Window side: the link to the hub dropped, so the feed's real state is unknown. */
    public void linkLost() { paused = true; setStatus(Status.DISCONNECTED, "not connected to the hub"); }

    /** Window side: raw lines pushed by the hub; {@code lastSeq} is the seq of the final line. */
    public synchronized void injectRaw(long epoch, long lastSeq, List<String> lines) {
        if (epoch != rawEpoch) { raw.clear(); rawSeq = 0; rawEpoch = epoch; rawResets++; }
        long first = lastSeq - lines.size() + 1;
        for (int i = 0; i < lines.size(); i++) {
            long seq = first + i;
            if (seq <= rawSeq) continue;
            raw.addLast(new RawLine(seq, lines.get(i))); rawSeq = seq;
        }
        while (raw.size() > RAW_MAX) raw.removeFirst();
    }

    /** Hub side: epoch to tag raw pushes with. */
    public static long epoch() { return EPOCH; }

    /** Hub side: apply a command a window sent. */
    public void applyCommand(String op, JsonObject o) {
        switch (op) {
            case "connect"    -> connect();
            case "disconnect" -> disconnect();
            case "endpoint"   -> setEndpoint(o.get("host").getAsString(), o.get("port").getAsInt(), o.get("call").getAsString());
            case "cmd"        -> sendCommand(o.get("text").getAsString());
            default           -> { }
        }
    }

    /** Hub side: resolve a feed by its key. */
    public static TelnetFeed byKey(String key) {
        return switch (key) { case "cluster" -> ClusterClient.getInstance(); case "rbn" -> HeardByClient.getInstance(); default -> null; };
    }
}
