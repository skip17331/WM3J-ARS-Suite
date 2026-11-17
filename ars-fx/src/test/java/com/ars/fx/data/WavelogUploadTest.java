package com.ars.fx.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** The Wavelog uploader against a stand-in server: the endpoint, the JSON it posts, batching, and how replies are read. */
class WavelogUploadTest {

    private static JLogDb.QsoExport qso(String call) {
        return new JLogDb.QsoExport(call, "2026-11-17 15:30:00", "20m", "CW", "14.025", 100, "599", "579", null, null, null, null, null,
                "FN20", JLogDb.Refs.NONE);
    }

    /** A stand-in Wavelog: records each posted body, answers with {@code status}/{@code body}. */
    private static HttpServer server(List<String> got, int status, String body) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/index.php/api/qso", ex -> {
            got.add(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        s.start();
        return s;
    }

    @Test void endpointFromWhateverAddressIsPasted() {
        assertEquals("https://log.example.com/index.php/api/qso", LogUpload.wavelogEndpoint("https://log.example.com"));
        assertEquals("https://log.example.com/index.php/api/qso", LogUpload.wavelogEndpoint("https://log.example.com/"));
        assertEquals("https://log.example.com/index.php/api/qso", LogUpload.wavelogEndpoint("log.example.com"));
        assertEquals("https://log.example.com/wl/index.php/api/qso", LogUpload.wavelogEndpoint("https://log.example.com/wl/index.php"));
        assertEquals("http://192.168.1.5/index.php/api/qso", LogUpload.wavelogEndpoint("http://192.168.1.5/index.php/api/qso"));
        assertNull(LogUpload.wavelogEndpoint("  "));
    }

    @Test void postsTheDocumentedJsonInBatches() throws Exception {
        List<String> got = new CopyOnWriteArrayList<>();
        HttpServer s = server(got, 201, "{\"status\":\"created\"}");
        try {
            List<JLogDb.QsoExport> rows = new ArrayList<>();
            for (int i = 0; i < LogUpload.WAVELOG_BATCH + 5; i++) rows.add(qso("K" + i + "AA"));
            LogUpload.Result r = LogUpload.wavelog(rows, "http://127.0.0.1:" + s.getAddress().getPort(), " wl-key ", "3");
            assertTrue(r.ok(), r.message());
            assertEquals(rows.size(), r.count());
            assertEquals(2, got.size(), "100 + 5 QSOs: two requests");
            JsonObject first = JsonParser.parseString(got.get(0)).getAsJsonObject();
            assertEquals("wl-key", first.get("key").getAsString());
            assertEquals("3", first.get("station_profile_id").getAsString());
            assertEquals("adif", first.get("type").getAsString());
            String adif = first.get("string").getAsString().toLowerCase();
            assertEquals(LogUpload.WAVELOG_BATCH, adif.split("<eor>", -1).length - 1, "one ADIF record per QSO");
            assertTrue(adif.contains("k0aa"));
        } finally { s.stop(0); }
    }

    @Test void aBadKeyIsReportedNotCountedAsSent() throws Exception {
        List<String> got = new CopyOnWriteArrayList<>();
        HttpServer s = server(got, 401, "{\"status\":\"error\",\"message\":\"Auth Error, invalid key\"}");
        try {
            LogUpload.Result r = LogUpload.wavelog(List.of(qso("W1AW")), "http://127.0.0.1:" + s.getAddress().getPort(), "bad", "1");
            assertFalse(r.ok());
            assertEquals(0, r.count());
            assertTrue(r.message().contains("invalid key"), r.message());
        } finally { s.stop(0); }
    }

    @Test void anErrorStatusWithHttp200IsStillAnError() throws Exception {
        HttpServer s = server(new CopyOnWriteArrayList<>(), 200, "{\"status\":\"abort\",\"reason\":\"station id\"}");
        try {
            assertFalse(LogUpload.wavelog(List.of(qso("W1AW")), "http://127.0.0.1:" + s.getAddress().getPort(), "k", "9").ok());
        } finally { s.stop(0); }
    }

    @Test void missingSettingsAreSaidPlainly() {
        assertFalse(LogUpload.wavelog(List.of(qso("W1AW")), "", "k", "1").ok());
        assertTrue(LogUpload.wavelog(List.of(qso("W1AW")), "https://x", "", "1").message().contains("not all set"));
        assertEquals("No QSOs to upload.", LogUpload.wavelog(List.of(), "https://x", "k", "1").message());
    }
}
