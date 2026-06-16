package com.ars.fx.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Client side of the station link, used when a solo J-Map / J-Sat is launched
 * with a {@code "remote"} URL in its {@link SoloConfig}. Connects to the
 * station's {@link RemoteServer}, feeds incoming state into the same data
 * singletons the in-dock build uses (so the UI is unchanged), and forwards this
 * window's rig-tune / rotor-move commands back to the station.
 *
 * <p>While a link is active, {@link ClusterClient}/{@link HeardByClient}/
 * {@link RigClient}/{@link RotorClient} skip their local daemons and instead
 * mirror what the server pushes; their setters become remote commands.
 */
public final class RemoteLink {

    private static volatile RemoteLink INSTANCE;
    private static final Gson GSON = new Gson();

    public static boolean isActive() { return INSTANCE != null; }
    public static RemoteLink get() { return INSTANCE; }

    /** Begin the link to a station J-Hub (idempotent). Bad URLs fail quietly to local behaviour. */
    public static synchronized void connect(String url) {
        if (INSTANCE != null) return;
        try { RemoteLink l = new RemoteLink(new URI(url)); INSTANCE = l; l.open(); }
        catch (Exception e) { System.err.println("[remote] bad station url \"" + url + "\": " + e.getMessage()); INSTANCE = null; }
    }

    private final URI uri;
    private volatile WebSocketClient ws;
    private volatile boolean connected = false;

    private RemoteLink(URI uri) { this.uri = uri; }
    public boolean isConnected() { return connected; }
    public String endpoint() { return uri.toString(); }

    private void open() {
        ws = new WebSocketClient(uri) {
            @Override public void onOpen(ServerHandshake h) { connected = true; }
            @Override public void onMessage(String message) { handle(message); }
            @Override public void onClose(int code, String reason, boolean remote) { connected = false; scheduleReconnect(); }
            @Override public void onError(Exception ex) { /* reconnect handles it */ }
        };
        ws.setConnectionLostTimeout(30);
        try { ws.connect(); } catch (Exception e) { scheduleReconnect(); }
    }

    private void scheduleReconnect() {
        Thread t = new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) { return; }
            WebSocketClient c = ws;
            if (c != null && !connected) { try { c.reconnect(); } catch (Exception ignored) {} }
        }, "remote-reconnect");
        t.setDaemon(true);
        t.start();
    }

    // ── incoming station state → local singletons ─────────────────────────────
    private void handle(String message) {
        try {
            JsonObject o = JsonParser.parseString(message).getAsJsonObject();
            if (!"state".equals(o.has("t") ? o.get("t").getAsString() : "")) return;

            ClusterClient.getInstance().injectSpots(listOf(o.get("spots"), ClusterClient.Spot.class));
            HeardByClient.getInstance().injectSpots(listOf(o.get("heard"), HeardByClient.HeardSpot.class));

            RigClient.State rig = GSON.fromJson(o.get("rig"), RigClient.State.class);
            if (rig != null) RigClient.getInstance().applyRemote(rig);
            RotorClient.State rot = GSON.fromJson(o.get("rotor"), RotorClient.State.class);
            if (rot != null) RotorClient.getInstance().applyRemote(rot);

            if (o.has("station") && o.get("station").isJsonObject()) applyStation(o.getAsJsonObject("station"));
        } catch (Exception ignored) { /* malformed snapshot */ }
    }

    private <T> List<T> listOf(JsonElement arr, Class<T> cls) {
        List<T> out = new ArrayList<>();
        if (arr != null && arr.isJsonArray()) { JsonArray a = arr.getAsJsonArray(); for (JsonElement e : a) out.add(GSON.fromJson(e, cls)); }
        return out;
    }
    private void applyStation(JsonObject st) {
        setIfChanged("station.call", str(st, "call"));
        setIfChanged("station.grid", str(st, "grid"));
        setIfChanged("station.lat",  str(st, "lat"));
        setIfChanged("station.lon",  str(st, "lon"));
    }
    private static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null; }
    private static void setIfChanged(String key, String val) {
        if (val == null || val.isBlank()) return;
        if (!val.equals(HubConfig.get(key, ""))) HubConfig.set(key, val);   // avoid disk thrash on every snapshot
    }

    // ── outgoing commands from this window's setters ──────────────────────────
    private void send(JsonObject o) {
        WebSocketClient c = ws;
        if (c != null && connected) { try { c.send(o.toString()); } catch (Exception ignored) {} }
    }
    public void sendRigFreq(long hz)  { JsonObject o = new JsonObject(); o.addProperty("t", "rig.freq"); o.addProperty("hz", hz);   send(o); }
    public void sendRigMode(String m) { JsonObject o = new JsonObject(); o.addProperty("t", "rig.mode"); o.addProperty("mode", m);  send(o); }
    public void sendRotorMove(double az) { JsonObject o = new JsonObject(); o.addProperty("t", "rotor.move"); o.addProperty("az", az); send(o); }
}
