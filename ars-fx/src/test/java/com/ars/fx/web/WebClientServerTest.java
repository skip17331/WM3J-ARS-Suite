package com.ars.fx.web;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.HubConfig;
import com.ars.fx.data.JLogDb;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WebClientServerTest {
    static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String oldHome; Path tmp; int port; String token;
    final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach void up(@TempDir Path t) throws Exception {
        tmp = t;
        oldHome = System.getProperty("user.home"); System.setProperty("user.home", t.toString()); HubConfig.reload();
        WebClientServer.setUidFileForTest(t.resolve("uids.txt"));
        token = WebClientServer.token();
        port = WebClientServer.start(0);
    }
    @AfterEach void down() { WebClientServer.stop(); WebClientServer.setUidFileForTest(null); System.setProperty("user.home", oldHome); HubConfig.reload(); }

    record Res(int status, String body) { JsonObject json() { return JsonParser.parseString(body).getAsJsonObject(); } }

    Res get(String path, String tokenHeader) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (tokenHeader != null) b.header("X-Token", tokenHeader);
        HttpResponse<String> r = http.send(b.GET().build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body());
    }
    Res post(String path, String json) throws Exception {
        HttpRequest rq = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).header("X-Token", token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> r = http.send(rq, HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body());
    }
    static String qso(String uid, String call, String extra) {
        return "{\"uid\":\"" + uid + "\",\"call\":\"" + call + "\",\"freq\":\"14.0600\",\"mode\":\"CW\",\"rstSent\":\"599\",\"rstRcvd\":\"579\",\"name\":\"BOB\"" + extra + "}";
    }

    // ---- who may see what ------------------------------------------------------------------------------------
    @Test void thePageIsOpenButTheDataNeedsTheToken() throws Exception {
        Res page = get("/", null);
        assertEquals(200, page.status());
        assertTrue(page.body().contains("<title>ARS Suite</title>") && page.body().contains("/api/state"));
        assertEquals(200, get("/manifest.webmanifest", null).status());
        assertEquals(404, get("/nothing", null).status());
        assertEquals(401, get("/api/state", null).status(), "no token");
        assertEquals(401, get("/api/state", "wrong-token-123456").status());
        assertEquals(401, get("/api/recent", "").status());
        assertEquals(200, get("/api/state", token).status());
        HttpRequest bearer = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/state")).header("Authorization", "Bearer " + token).GET().build();
        assertEquals(200, http.send(bearer, HttpResponse.BodyHandlers.ofString()).statusCode(), "the Bearer form works too");
    }

    @Test void theTokenIsMadeOnceAndStable() {
        assertEquals(24, token.length());
        assertTrue(token.matches("[0-9a-f]{24}"));
        assertEquals(token, WebClientServer.token());
        HubConfig.set(WebClientServer.KEY_TOKEN, "my-own-token-1234");
        assertEquals("my-own-token-1234", WebClientServer.token(), "the operator's own is kept");
        HubConfig.set(WebClientServer.KEY_TOKEN, "short");
        assertTrue(WebClientServer.token().length() >= 12, "a token too short to be safe is replaced");
    }

    @Test void theStateHasTheStationAndSaysWhenThereIsNoRigOrRotor() throws Exception {
        JsonObject s = get("/api/state", token).json();
        assertTrue(s.has("call") && s.has("utc"));
        assertFalse(s.getAsJsonObject("rig").get("connected").getAsBoolean());
        assertFalse(s.getAsJsonObject("rotor").get("connected").getAsBoolean());
        assertFalse(s.get("rigControl").getAsBoolean());
    }

    @Test void wrongMethodsAreRefused() throws Exception {
        assertEquals(405, get("/api/qso", token).status());
        Res r = post("/api/state", "{}");
        assertEquals(405, r.status());
    }

    // ---- logging a QSO ---------------------------------------------------------------------------------------
    @Test void aQsoIsLoggedOnceEvenIfThePhoneSendsItTwice() throws Exception {
        Res a = post("/api/qso", qso("phone-uid-0001", "k1abc", ",\"pota\":\"k-1234\",\"comment\":\"hi\""));
        assertEquals(200, a.status(), a.body());
        assertTrue(a.json().get("ok").getAsBoolean()); assertFalse(a.json().get("dup").getAsBoolean());
        Res b = post("/api/qso", qso("phone-uid-0001", "K1ABC", ""));
        assertEquals(200, b.status());
        assertTrue(b.json().get("dup").getAsBoolean(), "the same id again is acknowledged and ignored");
        assertEquals(1, JLogDb.recent(10).size());
        JLogDb.Qso q = JLogDb.recent(1).get(0);
        assertEquals("K1ABC", q.callsign());
        assertEquals("20m", q.band(), "the band is worked out from the frequency");
        assertEquals("CW", q.mode());
        assertEquals("K-1234", JLogDb.byId(q.id()).refs().pota(), "the park reference is kept and tidied");
        JsonObject recent = get("/api/recent?limit=5", token).json();
        assertEquals(1, recent.getAsJsonArray("qsos").size());
        assertEquals("K1ABC", recent.getAsJsonArray("qsos").get(0).getAsJsonObject().get("call").getAsString());
    }

    @Test void theIdsSurviveARestart() throws Exception {
        assertTrue(post("/api/qso", qso("phone-uid-0002", "K1ABC", "")).status() == 200);
        WebClientServer.setUidFileForTest(tmp.resolve("uids.txt"));          // forget memory; the file remains, as after a restart
        Res again = post("/api/qso", qso("phone-uid-0002", "K1ABC", ""));
        assertTrue(again.json().get("dup").getAsBoolean());
        assertEquals(1, JLogDb.recent(10).size());
    }

    @Test void aQsoTypedEarlierKeepsItsTimeAndAClockThatIsAheadIsPulledBack() throws Exception {
        LocalDateTime twoHoursAgo = LocalDateTime.now(ZoneOffset.UTC).minusHours(2).withNano(0);
        assertEquals(200, post("/api/qso", qso("phone-uid-0003", "K2DEF", ",\"utc\":\"" + twoHoursAgo.format(DT) + "\"")).status());
        assertEquals(twoHoursAgo.format(DT), JLogDb.recent(1).get(0).utc(), "queued on the phone, logged later, still dated when it was made");
        LocalDateTime tomorrow = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
        assertEquals(200, post("/api/qso", qso("phone-uid-0004", "K3GHI", ",\"utc\":\"" + tomorrow.format(DT) + "\"")).status());
        LocalDateTime stored = LocalDateTime.parse(JLogDb.recent(1).get(0).utc(), DT);
        assertTrue(stored.isBefore(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(11)), "a time in the future is not kept: " + stored);
    }

    @Test void badQsosAreRefusedWithAReason() throws Exception {
        assertEquals(400, post("/api/qso", qso("phone-uid-0005", "NOCALL", "")).status());
        assertEquals(400, post("/api/qso", qso("phone-uid-0005", "K1 ABC", "")).status());
        assertEquals(400, post("/api/qso", "{\"uid\":\"phone-uid-0005\",\"call\":\"K1ABC\",\"freq\":\"14.06\"}").status(), "no mode");
        assertEquals(400, post("/api/qso", "{\"uid\":\"x\",\"call\":\"K1ABC\",\"freq\":\"14.06\",\"mode\":\"CW\"}").status(), "id too short");
        assertEquals(400, post("/api/qso", qso("phone-uid-0006", "K1ABC", "").replace("14.0600", "abc")).status(), "frequency");
        assertEquals(400, post("/api/qso", qso("phone-uid-0006", "K1ABC", "").replace("14.0600", "9999")).status(), "frequency out of range");
        assertEquals(400, post("/api/qso", qso("phone-uid-0006", "K1ABC", ",\"utc\":\"yesterday\"")).status(), "time");
        assertEquals(400, post("/api/qso", "not json").status());
        assertEquals(400, post("/api/qso", "[1,2]").status());
        assertEquals(400, post("/api/qso", "{\"uid\":\"phone-uid-0007\",\"call\":\"K1ABC\",\"mode\":\"CW\"}").status(), "neither band nor frequency");
        assertEquals(400, post("/api/qso", qso("phone-uid-0008", "K1ABC", ",\"comment\":\"" + "x".repeat(20000) + "\"")).status(), "oversized");
        assertEquals(0, JLogDb.recent(10).size(), "none of them reached the log");
        assertTrue(post("/api/qso", "{\"uid\":\"phone-uid-0009\",\"call\":\"K1ABC\"}").json().get("error").getAsString().length() > 5);
    }

    // ---- tuning the rig ---------------------------------------------------------------------------------------
    @Test void tuningIsOffUntilAllowedAndNeedsASafeRig() throws Exception {
        Res off = post("/api/qsy", "{\"freqKhz\":\"14060\"}");
        assertEquals(403, off.status());
        assertTrue(off.json().get("error").getAsString().contains("off"));
        HubConfig.set(WebClientServer.KEY_CONTROL, "true");
        Res noRig = post("/api/qsy", "{\"freqKhz\":\"14060\"}");
        assertEquals(403, noRig.status());
        assertTrue(noRig.json().get("error").getAsString().contains("not connected"), noRig.body());
    }

    @Test void spotsComeBackAsAListWithTheFieldsThePageReads() throws Exception {
        JsonObject s = get("/api/spots?limit=3", token).json();      // whatever the shared cluster client holds in this run, up to the limit
        assertTrue(s.get("spots").isJsonArray());
        assertTrue(s.getAsJsonArray("spots").size() <= 3);
        for (var e : s.getAsJsonArray("spots")) {
            JsonObject o = e.getAsJsonObject();
            for (String k : new String[]{"call", "freqHz", "mode", "band", "de", "comment", "ageSec"}) assertTrue(o.has(k), "a spot has " + k);
        }
        assertTrue(get("/api/spots?limit=abc", token).json().get("spots").isJsonArray(), "an unreadable limit means the default");
    }

    // ---- starting and stopping with the settings -------------------------------------------------------------
    @Test void theServerFollowsTheSettings() throws Exception {
        WebClientServer.stop();
        HubConfig.set(WebClientServer.KEY_ENABLED, "false");
        WebClientServer.apply();
        assertEquals(-1, WebClientServer.boundPort());
        assertFalse(WebClientServer.enabled());
        HubConfig.set(WebClientServer.KEY_ENABLED, "true");
        HubConfig.set(WebClientServer.KEY_PORT, "not a port");
        assertEquals(WebClientServer.DEFAULT_PORT, WebClientServer.port(), "an unreadable port means the default");
        HubConfig.set(WebClientServer.KEY_PORT, "80");
        assertEquals(WebClientServer.DEFAULT_PORT, WebClientServer.port(), "and so does one below 1024");
    }
}
