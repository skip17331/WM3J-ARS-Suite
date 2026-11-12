package com.ars.fx.web;

import com.ars.fx.data.Bands;
import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.JLogDb;
import com.ars.fx.data.PathNow;
import com.ars.fx.data.Propagation;
import com.ars.fx.data.RigClient;
import com.ars.fx.data.RotorClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * A phone-friendly page served by the hub, for logging from a field (POTA / SOTA) or watching the shack from a phone without a native app.
 * Off until turned on in J-Hub &#9656; Data. It serves one responsive page (no login screen: the page asks for the access token once and keeps it)
 * and a small JSON interface that needs that token: station and rig state, the DX spots with our own band-open estimate, the recent log, and
 * posting a QSO. The page keeps QSOs it could not send in the phone's own storage and sends them, with their original times, when the network
 * returns; each carries an id so a retry is never logged twice. Tuning the rig from the phone is a separate switch, off by default.
 *
 * <p>It speaks plain HTTP, so use it on your own network only; reaching it from outside is a job for a VPN, not for this server.
 */
public final class WebClientServer {
    private WebClientServer() {}

    public static final String KEY_ENABLED = "web.enabled", KEY_PORT = "web.port", KEY_TOKEN = "web.token", KEY_CONTROL = "web.rigControl";
    public static final int DEFAULT_PORT = 8095;
    private static final int MAX_BODY = 16 * 1024, MAX_UIDS = 4000;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern CALL = Pattern.compile("[A-Z0-9/]{3,14}");
    private static final SecureRandom RNG = new SecureRandom();

    private static HttpServer server;
    private static int boundPort;
    private static Path uidFileOverride;
    private static final Set<String> SEEN = Collections.synchronizedSet(new LinkedHashSet<>());
    private static boolean uidsLoaded;

    // ---- life cycle --------------------------------------------------------------------------------------------------

    public static boolean enabled() { return HubConfig.getBool(KEY_ENABLED, false); }
    public static int port() {
        try { int p = Integer.parseInt(HubConfig.get(KEY_PORT, String.valueOf(DEFAULT_PORT)).trim()); if (p >= 1024 && p <= 65535) return p; }
        catch (NumberFormatException ignored) { /* the default */ }
        return DEFAULT_PORT;
    }

    /** The access token; made (24 random characters) the first time it is needed. */
    public static synchronized String token() {
        String t = HubConfig.get(KEY_TOKEN, "").trim();
        if (t.length() < 12) {
            byte[] b = new byte[16]; RNG.nextBytes(b);
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(Character.forDigit((x >> 4) & 0xf, 16)).append(Character.forDigit(x & 0xf, 16));
            t = sb.substring(0, 24);
            HubConfig.set(KEY_TOKEN, t);
        }
        return t;
    }

    /** Starts or stops the server to match the settings; call whenever one changes. */
    public static synchronized void apply() {
        if (!enabled()) { stop(); return; }
        int p = port();
        if (server != null && boundPort == p) return;
        stop();
        try { start(p); } catch (IOException e) { System.err.println("[web] could not start on port " + p + ": " + e.getMessage()); }
    }

    /** Registers for setting changes and applies the current ones. Call once, in the hub. */
    public static void init() {
        HubConfig.addChangeListener(k -> { if (k != null && (k.startsWith("web.") || k.equals("*"))) apply(); });
        apply();
    }

    static synchronized int start(int port) throws IOException {
        stop();
        HttpServer s = HttpServer.create(new InetSocketAddress(port), 16);
        s.createContext("/", WebClientServer::route);
        s.setExecutor(Executors.newFixedThreadPool(4, r -> { Thread t = new Thread(r, "web-client"); t.setDaemon(true); return t; }));
        s.start();
        server = s; boundPort = s.getAddress().getPort();
        System.out.println("[web] phone client on port " + boundPort);
        return boundPort;
    }

    public static synchronized void stop() {
        if (server != null) { server.stop(0); server = null; boundPort = 0; }
    }
    static synchronized int boundPort() { return server == null ? -1 : boundPort; }

    /** The addresses to open on the phone: this computer's LAN addresses with the port and token. */
    public static List<String> urls() {
        List<String> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    if (a instanceof java.net.Inet4Address && a.isSiteLocalAddress()) out.add("http://" + a.getHostAddress() + ":" + port() + "/?t=" + token());
            }
        } catch (Exception ignored) { /* none */ }
        return out;
    }

    // ---- routing -----------------------------------------------------------------------------------------------------

    private static void route(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (method.equals("GET") && (path.equals("/") || path.equals("/index.html"))) { page(ex, "index.html", "text/html; charset=utf-8"); return; }
            if (method.equals("GET") && path.equals("/manifest.webmanifest")) { page(ex, "manifest.webmanifest", "application/manifest+json"); return; }
            if (!path.startsWith("/api/")) { reply(ex, 404, error("not found")); return; }
            if (!authorised(ex)) { reply(ex, 401, error("the access token is missing or wrong")); return; }
            switch (path) {
                case "/api/state" -> { if (method.equals("GET")) reply(ex, 200, state()); else reply(ex, 405, error("GET only")); }
                case "/api/spots" -> { if (method.equals("GET")) reply(ex, 200, spots(intParam(ex, "limit", 40, 1, 100))); else reply(ex, 405, error("GET only")); }
                case "/api/recent" -> { if (method.equals("GET")) reply(ex, 200, recent(intParam(ex, "limit", 20, 1, 100))); else reply(ex, 405, error("GET only")); }
                case "/api/qso" -> { if (method.equals("POST")) reply(ex, 200, postQso(body(ex))); else reply(ex, 405, error("POST only")); }
                case "/api/qsy" -> { if (method.equals("POST")) { JsonObject r = qsy(body(ex)); reply(ex, r.has("error") ? 403 : 200, r); } else reply(ex, 405, error("POST only")); }
                default -> reply(ex, 404, error("not found"));
            }
        } catch (IllegalArgumentException e) {
            reply(ex, 400, error(e.getMessage()));
        } catch (Throwable t) {
            reply(ex, 500, error("server error"));
        } finally { ex.close(); }
    }

    private static boolean authorised(HttpExchange ex) {
        String given = ex.getRequestHeaders().getFirst("X-Token");
        if (given == null) {
            String a = ex.getRequestHeaders().getFirst("Authorization");
            if (a != null && a.startsWith("Bearer ")) given = a.substring(7).trim();
        }
        if (given == null) return false;
        return MessageDigest.isEqual(given.trim().getBytes(StandardCharsets.UTF_8), token().getBytes(StandardCharsets.UTF_8));
    }

    private static void page(HttpExchange ex, String name, String type) throws IOException {
        try (InputStream in = WebClientServer.class.getResourceAsStream("/com/ars/fx/web/" + name)) {
            if (in == null) { reply(ex, 404, error("not found")); return; }
            byte[] b = in.readAllBytes();
            ex.getResponseHeaders().set("Content-Type", type);
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
        }
    }

    private static void reply(HttpExchange ex, int status, JsonElement body) throws IOException {
        byte[] b = body.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, b.length);
        ex.getResponseBody().write(b);
    }
    private static JsonObject error(String why) { JsonObject o = new JsonObject(); o.addProperty("error", why); return o; }

    private static JsonObject body(HttpExchange ex) throws IOException {
        byte[] b = ex.getRequestBody().readNBytes(MAX_BODY + 1);
        if (b.length > MAX_BODY) throw new IllegalArgumentException("the request is too large");
        try {
            JsonElement e = JsonParser.parseString(new String(b, StandardCharsets.UTF_8));
            if (!e.isJsonObject()) throw new IllegalArgumentException("expected a JSON object");
            return e.getAsJsonObject();
        } catch (com.google.gson.JsonParseException e) { throw new IllegalArgumentException("not valid JSON"); }
    }

    private static int intParam(HttpExchange ex, String name, int def, int min, int max) {
        String q = ex.getRequestURI().getRawQuery();
        if (q == null) return def;
        for (String kv : q.split("&")) {
            String[] p = kv.split("=", 2);
            if (p.length == 2 && p[0].equals(name)) { try { return Math.max(min, Math.min(max, Integer.parseInt(p[1]))); } catch (NumberFormatException e) { return def; } }
        }
        return def;
    }

    // ---- what the page reads -----------------------------------------------------------------------------------------

    static JsonObject state() {
        JsonObject o = new JsonObject();
        o.addProperty("call", HubConfig.call());
        o.addProperty("grid", HubConfig.grid());
        o.addProperty("utc", LocalDateTime.now(ZoneOffset.UTC).format(DT));
        o.addProperty("rigControl", HubConfig.getBool(KEY_CONTROL, false));
        RigClient.State r = RigClient.getInstance().last();
        JsonObject rig = new JsonObject();
        boolean on = r != null && r.connected() && r.freqHz() > 0;
        rig.addProperty("connected", on);
        if (on) {
            rig.addProperty("freqHz", r.freqHz()); rig.addProperty("mode", r.mode() == null ? "" : r.mode());
            rig.addProperty("band", Bands.of(r.freqHz())); rig.addProperty("ptt", r.ptt());
        }
        o.add("rig", rig);
        RotorClient.State ro = RotorClient.getInstance().last();
        JsonObject rotor = new JsonObject();
        rotor.addProperty("connected", ro != null && ro.connected());
        if (ro != null && ro.connected()) rotor.addProperty("az", Math.round(((ro.az() % 360) + 360) % 360));
        o.add("rotor", rotor);
        return o;
    }

    static JsonObject spots(int limit) {
        JsonArray a = new JsonArray();
        long now = System.currentTimeMillis();
        for (ClusterClient.Spot s : ClusterClient.getInstance().spots()) {
            if (a.size() >= limit) break;
            JsonObject o = new JsonObject();
            o.addProperty("call", s.call()); o.addProperty("freqHz", s.freqHz()); o.addProperty("mode", s.mode() == null ? "" : s.mode());
            o.addProperty("band", s.band() == null ? "" : s.band()); o.addProperty("de", s.de() == null ? "" : s.de());
            o.addProperty("comment", s.comment() == null ? "" : s.comment()); o.addProperty("ageSec", Math.max(0, (now - s.arrivalMs()) / 1000));
            Propagation.Verdict v = PathNow.forSpot(s.call(), s.freqHz());
            if (v != null) o.addProperty("path", v.name().toLowerCase(Locale.ROOT));
            a.add(o);
        }
        JsonObject o = new JsonObject(); o.add("spots", a); return o;
    }

    static JsonObject recent(int limit) {
        JsonArray a = new JsonArray();
        for (JLogDb.Qso q : JLogDb.recent(limit)) {
            JsonObject o = new JsonObject();
            o.addProperty("utc", q.utc()); o.addProperty("call", q.callsign()); o.addProperty("freq", q.freq()); o.addProperty("band", q.band());
            o.addProperty("mode", q.mode()); o.addProperty("rst", q.rstRcvd() == null ? "" : q.rstRcvd()); o.addProperty("name", q.name() == null ? "" : q.name());
            a.add(o);
        }
        JsonObject o = new JsonObject(); o.add("qsos", a); return o;
    }

    // ---- what the page does ------------------------------------------------------------------------------------------

    /** Logs a QSO from the phone. A repeat of an id already logged is acknowledged and ignored, so the page can retry freely. */
    static JsonObject postQso(JsonObject b) {
        String uid = str(b, "uid", 64);
        if (!uid.matches("[A-Za-z0-9_\\-]{8,64}")) throw new IllegalArgumentException("a QSO needs an id of 8 to 64 letters, digits, - or _");
        String call = str(b, "call", 20).toUpperCase(Locale.ROOT);
        if (!CALL.matcher(call).matches() || !call.matches(".*\\d.*") || !call.matches(".*[A-Z].*")) throw new IllegalArgumentException("that is not a callsign");
        String mode = str(b, "mode", 12).toUpperCase(Locale.ROOT);
        if (mode.isEmpty()) throw new IllegalArgumentException("the mode is missing");
        String freq = str(b, "freq", 12);                                   // MHz
        double mhz;
        try { mhz = freq.isEmpty() ? 0 : Double.parseDouble(freq); } catch (NumberFormatException e) { throw new IllegalArgumentException("the frequency is not a number"); }
        if (mhz != 0 && (mhz < 0.1 || mhz > 450)) throw new IllegalArgumentException("the frequency is out of range");
        String band = str(b, "band", 8);
        if (band.isEmpty() && mhz > 0) band = JLogDb.bandFor(freq);
        if (band.isEmpty()) throw new IllegalArgumentException("the band or frequency is missing");

        synchronized (SEEN) {
            loadUids();
            if (SEEN.contains(uid)) { JsonObject d = new JsonObject(); d.addProperty("ok", true); d.addProperty("dup", true); return d; }
        }
        String utc = str(b, "utc", 19);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC), when = now;
        if (!utc.isEmpty()) {
            try { when = LocalDateTime.parse(utc, DT); } catch (RuntimeException e) { throw new IllegalArgumentException("the time must be yyyy-MM-dd HH:mm:ss (UTC)"); }
            if (when.isAfter(now.plusMinutes(10))) when = now;                // a phone clock that is ahead does not put a QSO in the future
        }
        JLogDb.Refs refs = new JLogDb.Refs("", str(b, "sota", 16), str(b, "pota", 16), 0, 0, false, false, false);
        boolean ok = JLogDb.insertAt(when.format(DT), call, freq, band, mode, str(b, "rstSent", 8), str(b, "rstRcvd", 8), str(b, "name", 40), str(b, "qth", 60),
                str(b, "grid", 8), str(b, "comment", 200), str(b, "state", 4), str(b, "county", 30), 0, refs);
        if (!ok) throw new IllegalArgumentException("the log could not be written");
        com.ars.fx.data.SessionQsos.logged(call, str(b, "grid", 8), band, mode);
        remember(uid);
        JsonObject d = new JsonObject(); d.addProperty("ok", true); d.addProperty("dup", false); return d;
    }

    /** Tunes the rig, if the operator allowed it: connected, not transmitting, and inside an amateur band. */
    static JsonObject qsy(JsonObject b) {
        if (!HubConfig.getBool(KEY_CONTROL, false)) return error("rig control from the phone is off (J-Hub > Data)");
        RigClient.State r = RigClient.getInstance().last();
        if (r == null || !r.connected()) return error("the rig is not connected");
        if (r.ptt()) return error("the rig is transmitting");
        long hz;
        try { hz = Math.round(Double.parseDouble(str(b, "freqKhz", 14)) * 1000.0); } catch (NumberFormatException e) { return error("the frequency is not a number"); }
        if (Bands.of(hz).isEmpty()) return error("that frequency is outside the amateur bands");
        RigClient.getInstance().setFreqHz(hz);
        String mode = str(b, "mode", 8).toUpperCase(Locale.ROOT);
        if (List.of("CW", "USB", "LSB", "FM", "AM", "RTTY").contains(mode)) RigClient.getInstance().setMode(mode);
        JsonObject d = new JsonObject(); d.addProperty("ok", true); return d;
    }

    private static String str(JsonObject b, String key, int max) {
        JsonElement e = b.get(key);
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) return "";
        String s = e.getAsString().trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    // ---- ids already logged ------------------------------------------------------------------------------------------

    static void setUidFileForTest(Path p) { uidFileOverride = p; synchronized (SEEN) { SEEN.clear(); uidsLoaded = false; } }
    private static Path uidFile() { return uidFileOverride != null ? uidFileOverride : Paths.get(System.getProperty("user.home"), ".j-log", "web-uids.txt"); }

    private static void loadUids() {
        if (uidsLoaded) return;
        uidsLoaded = true;
        try { if (Files.exists(uidFile())) for (String l : Files.readAllLines(uidFile(), StandardCharsets.UTF_8)) if (!l.isBlank()) SEEN.add(l.trim()); }
        catch (IOException ignored) { /* start empty */ }
    }

    private static void remember(String uid) {
        synchronized (SEEN) {
            SEEN.add(uid);
            try {
                Files.createDirectories(uidFile().getParent());
                if (SEEN.size() > MAX_UIDS) {                                  // keep the newest
                    List<String> all = new ArrayList<>(SEEN);
                    SEEN.clear(); SEEN.addAll(all.subList(all.size() - MAX_UIDS / 2, all.size()));
                    Files.write(uidFile(), SEEN, StandardCharsets.UTF_8);
                } else Files.writeString(uidFile(), uid + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException ignored) { /* the id stays in memory for this run */ }
        }
    }
}
