package com.ars.fx.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

/**
 * Station-side WebSocket server that lets a solo J-Map / J-Sat — on a second
 * computer or a shack Raspberry Pi — share this station's live state. Started by
 * the J-Hub dock app.
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

    /** Start the server if enabled in config (idempotent). Called by the dock app. */
    public static synchronized void startIfEnabled() {
        if (INSTANCE != null) return;
        if (!HubConfig.getBool("remote.serverEnabled", true)) return;
        int port = port();
        RemoteServer s = new RemoteServer(port);
        s.setReuseAddr(true);
        try {
            s.start();                 // non-blocking; spins its own accept thread
            INSTANCE = s;
            s.beginBroadcast();
            System.out.println("[remote] J-Hub sharing server listening on :" + port);
        } catch (Exception e) {
            System.err.println("[remote] server failed to start on :" + port + " — " + e.getMessage());
        }
    }
    public static boolean isRunning() { return INSTANCE != null; }
    public static int port() {
        try { return Integer.parseInt(HubConfig.get("remote.port", "8090").trim()); } catch (Exception e) { return 8090; }
    }

    private RemoteServer(int port) { super(new InetSocketAddress(port)); }

    @Override public void onStart() { setConnectionLostTimeout(30); }
    @Override public void onOpen(WebSocket conn, ClientHandshake h) { trySend(conn, snapshot()); }
    @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {}
    @Override public void onError(WebSocket conn, Exception ex) {}

    @Override public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject o = JsonParser.parseString(message).getAsJsonObject();
            String t = o.has("t") ? o.get("t").getAsString() : "";
            switch (t) {
                case "rig.freq"   -> RigClient.getInstance().setFreqHz(o.get("hz").getAsLong());
                case "rig.mode"   -> RigClient.getInstance().setMode(o.get("mode").getAsString());
                case "rotor.move" -> RotorClient.getInstance().moveTo(o.get("az").getAsDouble());
                default           -> { /* ignore unknown */ }
            }
        } catch (Exception ignored) { /* malformed command */ }
    }

    private void beginBroadcast() {
        Thread t = new Thread(() -> {
            while (true) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
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
