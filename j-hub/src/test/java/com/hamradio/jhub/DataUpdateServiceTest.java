package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig.AutoUpdateSection;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for DataUpdateService — exercises the validators directly and runs an
 * end-to-end fetch against an in-process HttpServer so CI never touches the
 * real internet.
 */
class DataUpdateServiceTest {

    private HttpServer server;
    private int        port;
    private final byte[] validCty = ("United States of America:                  05:  08:  NA:   37.701:    91.629:     5.0:  K:\n" +
            "    AA0AA;\n").getBytes(StandardCharsets.US_ASCII);
    private final byte[] validScp = ("# MASTER.SCP — Super Check Partial database\n" +
            "K1ABC\nW1XYZ\nDL2ABC\n").repeat(2000).getBytes(StandardCharsets.US_ASCII);

    @BeforeEach
    void up() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void down() { if (server != null) server.stop(0); }

    // ── Validators ──────────────────────────────────────────────────

    @Test
    void validateCtyAcceptsRealishCty() {
        assertTrue(DataUpdateService.validateCty(validCty));
    }

    @Test
    void validateCtyRejectsHtmlErrorPage() {
        // A 404 page or HTML error from a misconfigured server has no colon
        // in the first line — the most common "I got bytes but they're junk"
        // failure mode.
        byte[] html = "<!DOCTYPE html>\n<html><body>Not Found</body></html>".getBytes();
        assertFalse(DataUpdateService.validateCty(html));
    }

    @Test
    void validateScpAcceptsCallsignList() {
        assertTrue(DataUpdateService.validateScp(validScp));
    }

    @Test
    void validateScpRejectsHtmlErrorPage() {
        byte[] html = "<!DOCTYPE html><html><body>oops</body></html>".getBytes();
        assertFalse(DataUpdateService.validateScp(html));
    }

    @Test
    void validateScpAcceptsCommentBeforeCalls() {
        byte[] body = ("# header comment\n# another\nK1ABC\nW1XYZ\n").getBytes();
        assertTrue(DataUpdateService.validateScp(body));
    }

    // ── End-to-end against in-process HttpServer ────────────────────

    @Test
    void successfulCycleWritesBothFiles(@TempDir Path tmp) throws Exception {
        serve("/cty.dat",     200, validCty);
        serve("/MASTER.SCP",  200, validScp);

        DataUpdateService svc = newSvcAt(tmp);
        svc.updateNow();
        // updateNow runs on the executor; give it a moment.
        waitForFile(tmp.resolve("cty.dat"));
        waitForFile(tmp.resolve("MASTER.SCP"));

        assertEquals(validCty.length, Files.size(tmp.resolve("cty.dat")));
        assertEquals(validScp.length, Files.size(tmp.resolve("MASTER.SCP")));
        assertTrue(svc.getStatus().cty.validated);
        assertTrue(svc.getStatus().scp.validated);
        svc.stop();
    }

    @Test
    void httpErrorLeavesPreviousFileIntact(@TempDir Path tmp) throws Exception {
        // First seed an existing "good" file so we can verify it isn't lost.
        Files.write(tmp.resolve("cty.dat"), "previous-good-data".getBytes());
        long preSize = Files.size(tmp.resolve("cty.dat"));

        serve("/cty.dat",    503, "down for maintenance".getBytes());
        serve("/MASTER.SCP", 200, validScp);

        DataUpdateService svc = newSvcAt(tmp);
        svc.updateNow();
        waitForFile(tmp.resolve("MASTER.SCP"));
        Thread.sleep(200); // let the cty failure record itself

        // CTY untouched; status records the HTTP failure
        assertEquals(preSize, Files.size(tmp.resolve("cty.dat")));
        assertEquals(503, svc.getStatus().cty.lastHttpStatus);
        assertFalse(svc.getStatus().cty.validated);
        assertTrue(svc.getStatus().cty.lastError.contains("HTTP 503"));
        svc.stop();
    }

    @Test
    void undersizedResponseFailsValidationAndKeepsExistingFile(@TempDir Path tmp) throws Exception {
        Files.write(tmp.resolve("cty.dat"), "previous-good-data".getBytes());
        long preSize = Files.size(tmp.resolve("cty.dat"));

        // Truncated cty (well below the 50 KB minimum) — common partial-fetch case.
        serve("/cty.dat",    200, "tiny:".getBytes());
        serve("/MASTER.SCP", 200, validScp);

        DataUpdateService svc = newSvcAt(tmp);
        svc.updateNow();
        waitForFile(tmp.resolve("MASTER.SCP"));
        Thread.sleep(200);

        assertEquals(preSize, Files.size(tmp.resolve("cty.dat")));
        assertFalse(svc.getStatus().cty.validated);
        assertTrue(svc.getStatus().cty.lastError.contains("size"));
        svc.stop();
    }

    @Test
    void backupRotationKeepsPreviousAsBak(@TempDir Path tmp) throws Exception {
        Files.write(tmp.resolve("cty.dat"), "previous-good-data".getBytes());

        serve("/cty.dat",    200, validCty);
        serve("/MASTER.SCP", 200, validScp);

        DataUpdateService svc = newSvcAt(tmp);
        svc.updateNow();
        waitForFile(tmp.resolve("cty.dat.bak"));

        assertEquals("previous-good-data",
                new String(Files.readAllBytes(tmp.resolve("cty.dat.bak"))));
        assertEquals(validCty.length, Files.size(tmp.resolve("cty.dat")));
        svc.stop();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private void serve(String path, int status, byte[] body) {
        server.createContext(path, ex -> {
            ex.sendResponseHeaders(status, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
    }

    private DataUpdateService newSvcAt(Path dir) {
        // Each test gets a fresh instance state; the singleton field just gives
        // us one configured controller — start() reseeds everything.
        DataUpdateService svc = DataUpdateService.getInstance();
        AutoUpdateSection cfg = new AutoUpdateSection();
        cfg.enabled      = true;
        cfg.intervalDays = 365;       // effectively disable the automatic re-tick
        cfg.ctyUrl       = "http://127.0.0.1:" + port + "/cty.dat";
        cfg.scpUrl       = "http://127.0.0.1:" + port + "/MASTER.SCP";
        cfg.dataDir      = dir.toString();
        cfg.minCtySize   = 50;        // small thresholds for test fixtures
        cfg.minScpSize   = 50_000;
        svc.start(cfg);
        return svc;
    }

    private static void waitForFile(Path p) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (Files.exists(p)) return;
            Thread.sleep(40);
        }
        throw new AssertionError("file did not appear: " + p);
    }
}
