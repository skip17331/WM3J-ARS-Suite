package com.ars.fx.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

/**
 * Station-side WebSocket server that lets every J-Hub client — the loose module
 * windows and the docked app on this machine, plus a solo J-Map / J-Sat on a
 * second computer or a shack Raspberry Pi — share this station's live state.
 * Runs inside the background {@link com.ars.fx.HubServer} process (the single
 * owner of the Hamlib daemons + feeds); client windows never start it.
 *
 * <p>Server → client (broadcast ~1 s, plus once on connect): a {@code state}
 * snapshot of DX-cluster spots, "heard by" RBN spots, rig + rotor state, and the
 * station QTH/call. Client → server: {@code rig.freq} / {@code rig.mode} /
 * {@code rotor.move} commands, which are applied to this station's real rig and
 * rotor via the local {@link RigClient} / {@link RotorClient}.
 *
 * <p>Uses {@code org.java-websocket}, the same library the rest of the suite's
 * hub speaks. Bound on {@code remote.port} (default 8090); disable with
 * {@code remote.serverEnabled=false}.
 */
public final class RemoteServer extends WebSocketServer {

    private static volatile RemoteServer INSTANCE;
    private static final Gson GSON = new Gson();

    /** Hub-side hooks, set by {@link com.ars.fx.HubServer} before {@link #start()}: client-count changes
     *  drive the refcount/linger shutdown; a {@code hub.reconfig} nudge from a client triggers a reconcile. */
    public static volatile IntConsumer onClientCount;
    public static volatile Runnable onReconfig;

    private final Map<WebSocket, String> clients = new ConcurrentHashMap<>();   // conn → module label (from "hello")
    private final String boundHost;
    private final int boundPort;

    /** Start the hub's sharing server (idempotent). The hub always runs it so local loose/docked client
     *  windows can attach over loopback; {@code remote.serverEnabled} only widens the bind to the LAN
     *  (0.0.0.0) so a second PC / shack Pi can reach it. Called by the hub, never by a client window. */
    public static synchronized void startHub() {
        if (INSTANCE != null) return;
        int port = port();
        String host = bindHost();
        RemoteServer s = new RemoteServer(host, port);
        s.setReuseAddr(true);
        try {
            s.start();                 // non-blocking; spins its own accept thread
            INSTANCE = s;
            s.beginBroadcast();
            System.out.println("[remote] J-Hub sharing server listening on " + host + ":" + port
                    + (lanShared() ? " (LAN-visible)" : " (local only)"));
        } catch (Exception e) {
            System.err.println("[remote] server failed to start on " + host + ":" + port + " — " + e.getMessage());
        }
    }
    public static boolean isRunning() { return INSTANCE != null; }
    /** Live count of attached clients (loose windows + docked app + any LAN peers). Drives the hub refcount. */
    public static int clientCount() { RemoteServer s = INSTANCE; return s == null ? 0 : s.clients.size(); }
    public static int port() {
        try { return Integer.parseInt(HubConfig.get("remote.port", "8090").trim()); } catch (Exception e) { return 8090; }
    }
    private static boolean lanShared() { return HubConfig.getBool("remote.serverEnabled", true); }
    private static String bindHost() { return lanShared() ? "0.0.0.0" : "127.0.0.1"; }

    /** Stop the sharing server (idempotent). The broadcast loop exits when it no longer is INSTANCE. */
    public static synchronized void shutdown() {
        RemoteServer s = INSTANCE;
        INSTANCE = null;
        if (s != null) try { s.stop(1000); } catch (Exception ignored) {}   // inherited WebSocketServer.stop(timeout)
    }

    /** Re-bind only if the configured port / LAN-visibility actually changed — avoids dropping every client
     *  on an unrelated reconfig (e.g. a daemon toggle). Loopback clients briefly drop and auto-reconnect; a
     *  changed port needs the clients relaunched (they still target the old one). Returns true if it rebound. */
    public static synchronized boolean applyIfBindChanged() {
        RemoteServer s = INSTANCE;
        if (s == null) { startHub(); return true; }
        if (s.boundPort == port() && s.boundHost.equals(bindHost())) return false;
        Thread t = new Thread(() -> { shutdown(); startHub(); }, "remote-apply");
        t.setDaemon(true);
        t.start();
        return true;
    }

    private RemoteServer(String host, int port) { super(new InetSocketAddress(host, port)); this.boundHost = host; this.boundPort = port; }

    @Override public void onStart() { setConnectionLostTimeout(30); }
    @Override public void onOpen(WebSocket conn, ClientHandshake h) {
        clients.put(conn, "?");
        fireCount();
        trySend(conn, snapshot());
    }
    @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        fireCount();
    }
    @Override public void onError(WebSocket conn, Exception ex) {}

    @Override public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject o = JsonParser.parseString(message).getAsJsonObject();
            String t = o.has("t") ? o.get("t").getAsString() : "";
            switch (t) {
                case "rig.freq"     -> RigClient.getInstance().setFreqHz(o.get("hz").getAsLong());
                case "rig.mode"     -> RigClient.getInstance().setMode(o.get("mode").getAsString());
                case "rotor.move"   -> RotorClient.getInstance().moveTo(o.get("az").getAsDouble());
                case "hello"        -> {                                   // client identifies itself
                    clients.put(conn, o.has("module") ? o.get("module").getAsString() : "?");
                    System.out.println("[hub] " + clients.get(conn) + " attached (" + clients.size()
                            + " client" + (clients.size() == 1 ? "" : "s") + ")");
                }
                case "hub.reconfig" -> { Runnable r = onReconfig; if (r != null) r.run(); }   // re-read config + reconcile
                default             -> { /* ignore unknown */ }
            }
        } catch (Exception ignored) { /* malformed command */ }
    }

    private void fireCount() {
        IntConsumer cb = onClientCount;
        if (cb != null) try { cb.accept(clients.size()); } catch (Exception ignored) {}
    }

    private void beginBroadcast() {
        Thread t = new Thread(() -> {
            while (INSTANCE == this) {                 // exits once stopped/replaced
                try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
                if (INSTANCE != this) return;
                try { String s = snapshot(); if (!getConnections().isEmpty()) broadcast(s); } catch (Exception ignored) {}
            }
        }, "remote-broadcast");
        t.setDaemon(true);
        t.start();
    }

    private static void trySend(WebSocket c, String s) { try { c.send(s); } catch (Exception ignored) {} }

    /** Current station state as a {@code state} message. */
    private String snapshot() {
        JsonObject o = new JsonObject();
        o.addProperty("t", "state");
        o.add("spots", GSON.toJsonTree(ClusterClient.getInstance().spots()));
        o.add("heard", GSON.toJsonTree(HeardByClient.getInstance().spots()));
        o.add("rig",   GSON.toJsonTree(RigClient.getInstance().last()));
        o.add("rotor", GSON.toJsonTree(RotorClient.getInstance().last()));
        JsonObject st = new JsonObject();
        st.addProperty("call", HubConfig.call());
        st.addProperty("grid", HubConfig.grid());
        st.addProperty("lat",  HubConfig.get("station.lat", "39.7456"));
        st.addProperty("lon",  HubConfig.get("station.lon", "-76.96"));
        o.add("station", st);
        return o.toString();
    }
}
