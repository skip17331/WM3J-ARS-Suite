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

    /** Begin the link to a station J-Hub (idempotent). Bad URLs fail quietly to local behaviour.
     *  {@code identity} is the module label sent in the {@code hello} so the hub can list/track clients
     *  (e.g. "log", "map", or "dock" for the docked app). */
    public static synchronized void connect(String url, String identity) {
        if (INSTANCE != null) return;
        try { RemoteLink l = new RemoteLink(new URI(url), identity); INSTANCE = l; l.open(); }
        catch (Exception e) { System.err.println("[remote] bad station url \"" + url + "\": " + e.getMessage()); INSTANCE = null; }
    }

    /** Close the link (idempotent). The window is exiting, so suppress the reconnect; the hub sees the
     *  drop and decrements its client refcount. */
    public static synchronized void disconnect() {
        RemoteLink l = INSTANCE;
        INSTANCE = null;
        if (l != null) l.close();
    }

    /** Tell the hub to re-read config from disk and reconcile its backends (daemons, rig/rotor/amp links,
     *  LAN-share bind). Sent after a client edits config, since each JVM caches config independently. */
    public static void nudgeReconfig() {
        RemoteLink l = INSTANCE;
        if (l == null) return;
        JsonObject o = new JsonObject(); o.addProperty("t", "hub.reconfig"); l.send(o);
    }

    private final URI uri;
    private final String identity;
    private volatile WebSocketClient ws;
    private volatile boolean connected = false;
    private volatile boolean closing = false;

    private RemoteLink(URI uri, String identity) { this.uri = uri; this.identity = identity; }
    public boolean isConnected() { return connected; }
    public String endpoint() { return uri.toString(); }

    private void open() {
        ws = new WebSocketClient(uri) {
            @Override public void onOpen(ServerHandshake h) { connected = true; sendHello(); }
            @Override public void onMessage(String message) { handle(message); }
            @Override public void onClose(int code, String reason, boolean remote) {
                connected = false;
                TxController.applyRemoteTx(false);
                ClusterClient.getInstance().linkLost(); HeardByClient.getInstance().linkLost();   // their real state is unknown until we are back
                if (!closing) scheduleReconnect();
            }
            @Override public void onError(Exception ex) { /* reconnect handles it */ }
        };
        ws.setConnectionLostTimeout(30);
        try { ws.connect(); } catch (Exception e) { scheduleReconnect(); }
    }

    private void close() {
        closing = true;
        WebSocketClient c = ws;
        if (c != null) try { c.close(); } catch (Exception ignored) {}
    }

    private void sendHello() {
        JsonObject o = new JsonObject();
        o.addProperty("t", "hello");
        o.addProperty("module", identity == null || identity.isBlank() ? "?" : identity);
        send(o);
    }

    private void scheduleReconnect() {
        Thread t = new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) { return; }
            WebSocketClient c = ws;
            if (c != null && !connected && !closing) { try { c.reconnect(); } catch (Exception ignored) {} }
        }, "remote-reconnect");
        t.setDaemon(true);
        t.start();
    }

    // ── incoming station state → local singletons ─────────────────────────────
    private void handle(String message) {
        try {
            JsonObject o = JsonParser.parseString(message).getAsJsonObject();
            String type = o.has("t") ? o.get("t").getAsString() : "";
            if ("raw".equals(type)) {
                List<String> lines = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("lines")) lines.add(e.getAsString());
                ClusterClient.getInstance().injectRaw(o.get("epoch").getAsLong(), o.get("seq").getAsLong(), lines);
                return;
            }
            if (!"state".equals(type)) return;
            if (o.has("tx")) TxController.applyRemoteTx(o.get("tx").getAsBoolean());
            if (o.has("activity") && o.get("activity").isJsonArray()) {
                com.google.gson.JsonArray ja = o.getAsJsonArray("activity"); int[] flat = new int[ja.size()];
                for (int i = 0; i < flat.length; i++) flat[i] = ja.get(i).getAsInt();
                ClusterClient.getInstance().applyRemoteActivity(flat);
            }
            if (o.has("feeds") && o.get("feeds").isJsonObject()) {
                JsonObject f = o.getAsJsonObject("feeds");
                if (f.has("cluster")) ClusterClient.getInstance().applyRemoteStatus(f.getAsJsonObject("cluster"));
                if (f.has("rbn"))     HeardByClient.getInstance().applyRemoteStatus(f.getAsJsonObject("rbn"));
            }

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
    /** Ask the hub to act on a telnet feed: connect / disconnect / endpoint / cmd. */
    public void sendFeed(String key, String op, JsonObject extra) {
        JsonObject o = extra == null ? new JsonObject() : extra.deepCopy();
        o.addProperty("t", "feed"); o.addProperty("key", key); o.addProperty("op", op);
        send(o);
    }
    /** Ask the hub to transmit for us: cw / ptt / stop (see {@link TxController}). */
    public void sendTx(String op, JsonObject extra) {
        JsonObject o = extra == null ? new JsonObject() : extra.deepCopy();
        o.addProperty("t", "tx"); o.addProperty("op", op);
        send(o);
    }
    public void sendRigFreq(long hz)  { JsonObject o = new JsonObject(); o.addProperty("t", "rig.freq"); o.addProperty("hz", hz);   send(o); }
    public void sendRigMode(String m) { JsonObject o = new JsonObject(); o.addProperty("t", "rig.mode"); o.addProperty("mode", m);  send(o); }
    /** Ask the hub to add a spot from this window's local CW skimmer to the shared spot list. */
    public void sendSpot(ClusterClient.Spot sp) {
        JsonObject o = new JsonObject(); o.addProperty("t", "spot.add");
        o.addProperty("hz", sp.freqHz()); o.addProperty("call", sp.call()); o.addProperty("mode", sp.mode());
        o.addProperty("band", sp.band() == null ? "" : sp.band()); o.addProperty("de", sp.de()); o.addProperty("comment", sp.comment());
        send(o);
    }
    public void sendRotorMove(double az) { JsonObject o = new JsonObject(); o.addProperty("t", "rotor.move"); o.addProperty("az", az); send(o); }
    /** As above with an elevation too (a satellite pass on an az/el rotor); a station that only knows azimuth ignores the extra field. */
    public void sendRotorMove(double az, double el) { JsonObject o = new JsonObject(); o.addProperty("t", "rotor.move"); o.addProperty("az", az); o.addProperty("el", el); send(o); }
}
