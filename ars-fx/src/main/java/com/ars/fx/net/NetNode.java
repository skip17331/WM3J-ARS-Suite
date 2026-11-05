package com.ars.fx.net;

import com.google.gson.JsonObject;
import com.jlog.db.ContestQsoDao.Change;
import com.jlog.db.ContestQsoDao.Meta;
import com.jlog.model.QsoRecord;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * One station on a contest network. One station hosts and the others join it (a star: a joining station needs only the host's address), but
 * every station keeps a full copy of the log in its own database, so a station that loses the host carries on logging and the two catch up when
 * the connection returns. A station is let in only if it proves it knows the shared key ({@link NetAuth}). On joining, the two exchange lists
 * of the QSOs they hold ({@code have}) and each sends the other what is missing or older; after that every QSO added, edited or erased at
 * a station is sent on at once, and the host passes it to the others. Which version of a QSO wins is {@link NetMerge}'s rule.
 */
public final class NetNode {
    public enum Role { OFF, HOST, JOIN }

    private final String station, op, key;
    private final NetStore store;
    private final Supplier<NetStatus> myStatus;
    private final Object lock = new Object();                       // QSOs are applied one at a time
    private final Map<WebSocket, Conn> conns = new ConcurrentHashMap<>();
    private final Map<String, NetStatus> roster = new ConcurrentHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private volatile Role role = Role.OFF;
    private volatile boolean connected;                              // JOIN: the host has accepted us
    private volatile String lastProblem = "";
    private volatile int boundPort;
    private String joinHost; private int joinPort;
    private Srv server; private Cli client; private volatile boolean connecting;   // volatile: socket callbacks must never take this node's lock (stop() holds it while closing a socket)
    private ScheduledExecutorService timer;

    private static final class Conn { String nonce = NetAuth.nonce(); boolean authed; String station = ""; }
    private static final long STALE_MS = 20_000;

    public NetNode(String station, String op, String key, NetStore store, Supplier<NetStatus> myStatus) {
        this.station = station; this.op = op == null ? "" : op; this.key = key; this.store = store; this.myStatus = myStatus;
        store.onLocalChange(this::localChange);
    }

    // ---- life cycle --------------------------------------------------------------------------------------------------

    /** Starts hosting on {@code port} (0 for any free port; see {@link #port()}). */
    public synchronized void startHost(int port) throws Exception {
        stop();
        if (!NetAuth.keyOk(key)) throw new IllegalArgumentException("the shared key must be at least " + NetAuth.MIN_KEY + " characters");
        role = Role.HOST;
        CountDownLatch up = new CountDownLatch(1);
        server = new Srv(new InetSocketAddress(port), up);
        server.setReuseAddr(true);
        server.setConnectionLostTimeout(10);
        server.start();
        if (!up.await(5, TimeUnit.SECONDS)) { Srv s = server; server = null; role = Role.OFF; try { s.stop(100); } catch (Exception ignored) { } throw new IllegalStateException("could not listen on port " + port + (lastProblem.isEmpty() ? "" : ": " + lastProblem)); }
        boundPort = server.getPort();
        startTimer();
        fire();
    }

    /** Starts joining the host at {@code host}:{@code port}; keeps trying until it answers, and again if the connection drops. */
    public synchronized void startJoin(String host, int port) {
        stop();
        if (!NetAuth.keyOk(key)) { lastProblem = "the shared key must be at least " + NetAuth.MIN_KEY + " characters"; return; }
        role = Role.JOIN; joinHost = host; joinPort = port;
        startTimer();
        timer.execute(this::tryConnect);
        fire();
    }

    public synchronized void stop() {
        role = Role.OFF; connected = false;
        if (timer != null) { timer.shutdownNow(); timer = null; }
        Srv s = server; server = null;
        if (s != null) try { s.stop(200); } catch (Exception ignored) { }
        Cli c = client; client = null; connecting = false;
        if (c != null) try { c.close(); } catch (Exception ignored) { }
        conns.clear(); roster.clear(); boundPort = 0;
        fire();
    }

    private void startTimer() {
        timer = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "contest-net"); t.setDaemon(true); return t; });
        timer.scheduleWithFixedDelay(() -> { try { tick(); } catch (Throwable ignored) { } }, 1, 5, TimeUnit.SECONDS);
        if (role == Role.JOIN) timer.scheduleWithFixedDelay(() -> { try { tryConnect(); } catch (Throwable ignored) { } }, 3, 3, TimeUnit.SECONDS);
    }

    private synchronized void tryConnect() {
        if (role != Role.JOIN || connecting || (client != null && client.isOpen())) return;
        try {
            connecting = true;
            Cli c = new Cli(new URI("ws://" + joinHost + ":" + joinPort));
            c.setConnectionLostTimeout(10);
            client = c;
            c.connect();
        } catch (Exception e) { connecting = false; lastProblem = "cannot reach " + joinHost + ":" + joinPort; fire(); }
    }

    // ---- what the screen asks ----------------------------------------------------------------------------------------

    public Role role() { return role; }
    /** The port being listened on (HOST). */
    public int port() { return boundPort; }
    /** HOST: listening. JOIN: the host has accepted us. */
    public boolean online() { return role == Role.HOST || (role == Role.JOIN && connected); }
    public int connectedCount() { int n = 0; for (Conn c : conns.values()) if (c.authed) n++; return n; }
    public String problem() { return lastProblem; }
    public void addListener(Runnable r) { if (r != null) listeners.add(r); }

    /** The other stations, as last heard (not older than twenty seconds). */
    public List<NetStatus> roster() {
        long now = System.currentTimeMillis();
        List<NetStatus> out = new ArrayList<>();
        for (NetStatus s : roster.values()) if (now - s.seenMs() <= STALE_MS && !s.station().equals(station)) out.add(s);
        out.sort((a, b) -> a.station().compareToIgnoreCase(b.station()));
        return out;
    }

    /** A line for the screen: what this station is doing on the network. */
    public String summary() {
        return switch (role) {
            case OFF -> "";
            case HOST -> "Hosting on port " + boundPort + " · " + connectedCount() + (connectedCount() == 1 ? " station" : " stations") + " joined";
            case JOIN -> connected ? "Joined " + joinHost + ":" + joinPort : "Connecting to " + joinHost + ":" + joinPort + "…" + (lastProblem.isEmpty() ? "" : " (" + lastProblem + ")");
        };
    }

    private void fire() { for (Runnable r : listeners) { try { r.run(); } catch (RuntimeException ignored) { } } }

    // ---- every five seconds: say what this station is doing, forget the ones that went quiet ---------------------------

    private void tick() {
        long now = System.currentTimeMillis();
        roster.values().removeIf(s -> now - s.seenMs() > STALE_MS);
        publishStatus();
        fire();
    }

    /** Starts the exchange of QSOs again with every connected station: call it when the contest this station has open changes. */
    public void resync() {
        if (role == Role.HOST) {
            for (Map.Entry<WebSocket, Conn> e : conns.entrySet())
                if (e.getValue().authed && e.getKey().isOpen()) { sendHave(e.getKey()::send); e.getKey().send(NetProto.sync()); }
        } else if (role == Role.JOIN && connected) {
            Cli c = client;
            if (c != null && c.isOpen()) { sendHave(c::send); c.send(NetProto.sync()); }
        }
    }

    /** Tells the others what this station is doing now. */
    public void publishStatus() {
        NetStatus s = myStatus == null ? null : myStatus.get();
        if (s != null) broadcast(NetProto.status(s), null);
    }

    // ---- the server side ---------------------------------------------------------------------------------------------

    private final class Srv extends WebSocketServer {
        private final CountDownLatch started;
        Srv(InetSocketAddress a, CountDownLatch started) { super(a); this.started = started; }
        @Override public void onStart() { started.countDown(); }
        @Override public void onOpen(WebSocket ws, ClientHandshake h) { Conn c = new Conn(); conns.put(ws, c); ws.send(NetProto.challenge(c.nonce)); }
        @Override public void onClose(WebSocket ws, int code, String reason, boolean remote) {
            Conn c = conns.remove(ws);
            if (c != null && c.authed) roster.remove(c.station);
            fire();
        }
        @Override public void onError(WebSocket ws, Exception e) { lastProblem = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
        @Override public void onMessage(WebSocket ws, String text) {
            JsonObject m = NetProto.parse(text);
            Conn c = conns.get(ws);
            if (m == null || c == null) return;
            String t = NetProto.type(m);
            if (!c.authed) {
                if (!"hello".equals(t)) { ws.close(); return; }
                String st = NetProto.str(m, "station");
                if (st.isBlank() || st.equals(station) || stationInUse(st)) { ws.send(NetProto.reject("that station name is already in use")); ws.close(); return; }
                if (!NetAuth.verify(key, c.nonce, st, NetProto.str(m, "proof"))) { ws.send(NetProto.reject("wrong key")); ws.close(); return; }
                c.authed = true; c.station = st;
                ws.send(NetProto.welcome(station, store.contest()));
                sendHave(ws::send);
                fire();
                return;
            }
            handle(m, text, ws::send, ws);
        }
        private boolean stationInUse(String st) { for (Conn c : conns.values()) if (c.authed && c.station.equals(st)) return true; return false; }
    }

    // ---- the client side ---------------------------------------------------------------------------------------------

    private final class Cli extends WebSocketClient {
        Cli(URI u) { super(u); }
        @Override public void onOpen(ServerHandshake h) { /* the host speaks first: it sends a challenge */ }
        @Override public void onClose(int code, String reason, boolean remote) { connecting = false; connected = false; fire(); }
        @Override public void onError(Exception e) { lastProblem = "cannot reach " + joinHost + ":" + joinPort; connecting = false; fire(); }
        @Override public void onMessage(String text) {
            JsonObject m = NetProto.parse(text);
            if (m == null) return;
            switch (NetProto.type(m)) {
                case "challenge" -> send(NetProto.hello(station, op, NetAuth.proof(key, NetProto.str(m, "nonce"), station)));
                case "welcome" -> { connected = true; lastProblem = ""; connecting = false; sendHave(this::send); fire(); }
                case "reject" -> { lastProblem = NetProto.str(m, "reason"); fire(); }
                default -> handle(m, text, this::send, null);
            }
        }
    }

    // ---- messages both sides understand ------------------------------------------------------------------------------

    private void handle(JsonObject m, String raw, java.util.function.Consumer<String> reply, WebSocket from) {
        try {
            switch (NetProto.type(m)) {
                case "have" -> onHave(m, reply);
                case "sync" -> sendHave(reply);
                case "qso" -> { if (applyQso(m)) relay(raw, from); }
                case "del" -> { if (applyDelete(m)) relay(raw, from); }
                case "status" -> {
                    NetStatus s = NetProto.statusFrom(m);
                    if (s != null && !s.station().equals(station)) { roster.put(s.station(), s); fire(); }
                    relay(raw, from);
                }
                default -> { /* unknown messages from a newer version are ignored */ }
            }
        } catch (Exception e) { lastProblem = "could not apply a message: " + e.getMessage(); }
    }

    private void sendHave(java.util.function.Consumer<String> to) {
        try {
            String c = store.contest();
            if (c != null) to.accept(NetProto.have(c, store.inventory(c)));
        } catch (Exception e) { lastProblem = "could not list the log: " + e.getMessage(); }
    }

    /** The other station told us what it holds: send it what it lacks or has an older version of. */
    private void onHave(JsonObject m, java.util.function.Consumer<String> reply) throws Exception {
        String cur = store.contest();
        if (cur == null || !cur.equals(NetProto.str(m, "contest"))) return;
        for (Meta x : NetMerge.toSend(store.inventory(cur), NetProto.metasFrom(m))) {
            if (x.deleted()) reply.accept(NetProto.del(x.uid(), cur, x.ts()));
            else { QsoRecord q = store.find(x.uid()); if (q != null) reply.accept(NetProto.qso(q)); }
        }
    }

    private boolean applyQso(JsonObject m) throws Exception {
        QsoRecord q = NetProto.qsoFrom(m);
        if (q == null) return false;
        synchronized (lock) {
            String cur = store.contest();
            if (cur == null || !cur.equals(q.getContestId())) return false;                       // only the contest this station has open
            Meta local = store.metaOf(q.getUid());
            if (!NetMerge.incomingWins(NetMerge.Version.of(local), new NetMerge.Version(q.getUpdatedMs(), false, q.getOrigin() == null ? "" : q.getOrigin()))) return false;
            store.apply(q);
        }
        store.remoteChanged(q.getContestId());
        return true;
    }

    private boolean applyDelete(JsonObject m) throws Exception {
        String uid = NetProto.str(m, "uid"), contest = NetProto.str(m, "c"); long ts = NetProto.num(m, "ts");
        if (uid.isBlank() || contest.isBlank()) return false;
        synchronized (lock) {
            String cur = store.contest();
            if (cur == null || !cur.equals(contest)) return false;
            Meta local = store.metaOf(uid);
            if (!NetMerge.incomingWins(NetMerge.Version.of(local), new NetMerge.Version(ts, true, ""))) return false;
            store.applyDelete(uid, contest, ts);
        }
        store.remoteChanged(contest);
        return true;
    }

    // ---- sending -----------------------------------------------------------------------------------------------------

    /** The host passes what it received on to every other joined station. */
    private void relay(String raw, WebSocket except) { if (role == Role.HOST) broadcast(raw, except); }

    /** HOST: to every joined station (except one). JOIN: to the host, if connected. */
    private void broadcast(String msg, WebSocket except) {
        if (role == Role.HOST) {
            for (Map.Entry<WebSocket, Conn> e : conns.entrySet())
                if (e.getValue().authed && e.getKey() != except && e.getKey().isOpen()) { try { e.getKey().send(msg); } catch (Exception ignored) { } }
        } else if (role == Role.JOIN && connected) {
            Cli c = client;
            if (c != null && c.isOpen()) { try { c.send(msg); } catch (Exception ignored) { } }
        }
    }

    /** A change made at this station: send it on. (If we are not connected, the exchange on reconnecting carries it.) */
    private void localChange(Change ch) {
        if (role == Role.OFF) return;
        try {
            String cur = store.contest();
            if (cur == null || !cur.equals(ch.contestId())) return;
            if (ch.kind() == Change.Kind.DELETE) {
                Meta m = store.metaOf(ch.uid());
                if (m != null) broadcast(NetProto.del(ch.uid(), cur, m.ts()), null);
            } else {
                QsoRecord q = store.find(ch.uid());
                if (q != null) broadcast(NetProto.qso(q), null);
            }
        } catch (Exception e) { lastProblem = "could not send a change: " + e.getMessage(); }
    }
}
