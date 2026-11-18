package com.ars.fx.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Punch list 5-9: Cloudlog (Wavelog's API) and HRDLog.net (one QSO per request, only what has not gone before). */
class CloudlogHrdlogUploadTest {

    private static JLogDb.QsoExport qso(String call) {
        return new JLogDb.QsoExport(call, "2026-11-18 15:30:00", "20m", "SSB", "14.250", 100, "59", "57", null, null, null, null, null,
                "FN20", JLogDb.Refs.NONE);
    }

    @Test void cloudlogGetsWavelogsRequestAndItsReplyIsRead() throws Exception {
        List<String> got = new CopyOnWriteArrayList<>();
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/index.php/api/qso", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            got.add(body);
            JsonObject in = JsonParser.parseString(body).getAsJsonObject();
            // Cloudlog's reply (Api.php): 201, status created, the ADIF echoed back, imported_count
            byte[] b = ("{\"status\":\"created\",\"type\":\"adif\",\"string\":" + in.get("string") + ",\"imported_count\":2,\"messages\":[]}")
                    .getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(201, b.length); ex.getResponseBody().write(b); ex.close();
        });
        s.start();
        try {
            LogUpload.Result r = LogUpload.cloudlog(List.of(qso("W1AW"), qso("K1JT")), "http://127.0.0.1:" + s.getAddress().getPort(), "cl-key", "2");
            assertTrue(r.ok(), r.message());
            assertEquals(2, r.count());
            assertTrue(r.message().startsWith("Cloudlog: sent 2 QSOs"), r.message());
            assertTrue(r.message().contains("2 new"), r.message());
            assertFalse(r.message().contains("<eor>"), "the echoed ADIF is not shown: " + r.message());
            JsonObject sent = JsonParser.parseString(got.get(0)).getAsJsonObject();
            assertEquals("2", sent.get("station_profile_id").getAsString());
        } finally { s.stop(0); }
    }

    @Test void cloudlogsFailureReasonIsShown() throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/index.php/api/qso", ex -> {
            byte[] b = "{\"status\":\"failed\",\"reason\":\"station id does not belong to the API key owner.\"}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(401, b.length); ex.getResponseBody().write(b); ex.close();
        });
        s.start();
        try {
            LogUpload.Result r = LogUpload.cloudlog(List.of(qso("W1AW")), "http://127.0.0.1:" + s.getAddress().getPort(), "k", "9");
            assertFalse(r.ok());
            assertEquals("Cloudlog: stopped after 0 QSOs (HTTP 401): station id does not belong to the API key owner.", r.message());
            assertTrue(LogUpload.cloudlog(List.of(qso("W1AW")), "", "k", "9").message().startsWith("Cloudlog address"));
        } finally { s.stop(0); }
    }

    static List<JLogDb.Pending> pending(String... calls) {
        List<JLogDb.Pending> l = new ArrayList<>();
        for (int i = 0; i < calls.length; i++) l.add(new JLogDb.Pending(100 + i, qso(calls[i])));
        return l;
    }

    @Test void hrdlogSendsEachQsoAndMarksWhatArrived() {
        List<String> forms = new ArrayList<>();
        List<Long> marked = new ArrayList<>();
        LogUpload.Result r = LogUpload.hrdlog(pending("W1AW", "K1JT", "N0CALL"), "wm3j", "abc123", (url, form) -> {
            assertEquals("https://robot.hrdlog.net/newentry.aspx", url);
            forms.add(URLDecoder.decode(form, StandardCharsets.UTF_8));
            return switch (forms.size()) {
                case 1 -> "<?xml version=\"1.0\"?><insert>1</insert>";
                case 2 -> "<?xml version=\"1.0\"?><insert>0</insert>";
                default -> "<?xml version=\"1.0\"?><error>Bad ADIF</error>";
            };
        }, marked::addAll);
        assertEquals(3, forms.size(), "one request per QSO");
        assertTrue(forms.get(0).startsWith("Callsign=WM3J&Code=abc123&ADIFData=<"), forms.get(0));
        assertTrue(forms.get(0).toLowerCase().contains("<call:4>w1aw") && forms.get(0).toLowerCase().contains("<eor>"), forms.get(0));
        assertEquals(List.of(100L, 101L), marked, "added and already-there are marked sent; the failed one is tried again next time");
        assertFalse(r.ok());
        assertEquals(1, r.count());
        assertTrue(r.message().startsWith("HRDLog: 1 added, 1 already there, 1 failed"), r.message());
    }

    @Test void hrdlogStopsOnAWrongLoginAndKeepsWhatWentBefore() {
        List<Long> marked = new ArrayList<>();
        int[] n = {0};
        LogUpload.Result r = LogUpload.hrdlog(pending("W1AW", "K1JT", "N0CALL"), "WM3J", "wrong", (url, form) ->
                ++n[0] == 1 ? "<insert>1</insert>" : "<error>Invalid token</error>", marked::addAll);
        assertEquals(2, n[0], "stopped at the refusal");
        assertEquals(List.of(100L), marked);
        assertTrue(r.message().startsWith("HRDLog refused the callsign or upload code"), r.message());
    }

    @Test void hrdlogStopsWhenTheSiteIsDown() {
        int[] n = {0};
        List<JLogDb.Pending> many = pending("A1A", "A2A", "A3A", "A4A", "A5A", "A6A", "A7A", "A8A");
        LogUpload.Result r = LogUpload.hrdlog(many, "WM3J", "c", (url, form) -> { n[0]++; throw new java.io.IOException("connection refused"); }, ids -> {});
        assertEquals(LogUpload.HRDLOG_MAX_ERRORS, n[0]);
        assertTrue(r.message().contains("errors in a row") && r.message().contains("connection refused"), r.message());
    }

    @Test void hrdlogNeedsBothSettingsAndSaysWhenNothingIsNew() {
        assertTrue(LogUpload.hrdlog(pending("W1AW"), "", "c", (u, f) -> fail("no request"), ids -> {}).message().contains("not both set"));
        LogUpload.Result none = LogUpload.hrdlog(List.of(), "WM3J", "c", (u, f) -> fail("no request"), ids -> {});
        assertTrue(none.ok());
        assertTrue(none.message().contains("nothing new"));
    }

    @Test void hrdlogsUploadQueueStartsWithTheWholeLog() throws Exception {
        // unlike LoTW / eQSL auto-upload, whose markers began with every older QSO marked done
        String home = System.getProperty("user.home");
        java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("hrdlogq");
        System.setProperty("user.home", tmp.toString());
        try {
            assertTrue(JLogDb.insert("W1AW", "14.025", "20m", "CW", "599", "599", "", "", "", ""));
            assertEquals(1, JLogDb.pendingUpload("hrdlog").size());
            assertEquals(1, JLogDb.pendingUpload("lotw").size(), "a QSO logged after the markers exist is pending for LoTW too");
            JLogDb.markUploaded("hrdlog", List.of(JLogDb.pendingUpload("hrdlog").get(0).id()));
            assertEquals(0, JLogDb.pendingUpload("hrdlog").size());
        } finally {
            System.setProperty("user.home", home);
        }
    }
}
