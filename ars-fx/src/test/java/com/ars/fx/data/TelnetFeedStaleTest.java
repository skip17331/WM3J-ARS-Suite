package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** A cluster link that dies without the server closing it is noticed and reconnected; a quiet but live one is left alone. */
class TelnetFeedStaleTest {
    private static final class Feed extends TelnetFeed {
        final List<String> lines = new CopyOnWriteArrayList<>();
        Feed(int port) { super("teststale", "test-feed", "127.0.0.1", port, "WM3J", true); }
        @Override protected void onLine(String line) { lines.add(line); }
        @Override protected void onEndpointChanged() { }
        @Override protected void fire() { }
    }

    /** A cluster stand-in: login prompt, one spot, then silence. When {@code answersBlank}, it answers blank lines with its prompt. */
    private static Thread serve(ServerSocket ss, AtomicInteger connections, AtomicInteger blanks, boolean answersBlank) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    Socket c = ss.accept();
                    connections.incrementAndGet();
                    new Thread(() -> {
                        try (c) {
                            OutputStream out = c.getOutputStream(); InputStream in = c.getInputStream();
                            out.write("login: ".getBytes(StandardCharsets.ISO_8859_1)); out.flush();
                            StringBuilder cur = new StringBuilder();
                            boolean spotSent = false;
                            int b;
                            while ((b = in.read()) >= 0) {
                                if (b != '\n') { if (b != '\r') cur.append((char) b); continue; }
                                String line = cur.toString(); cur.setLength(0);
                                if (!spotSent) {                                                  // the call: welcome and one spot
                                    out.write("Hello WM3J\r\nDX de K1ABC:   14025.0  K2XYZ  test  1730Z\r\n".getBytes(StandardCharsets.ISO_8859_1)); out.flush();
                                    spotSent = true;
                                } else if (line.isEmpty()) {
                                    blanks.incrementAndGet();
                                    if (answersBlank) { out.write("WM3J de TEST >\r\n".getBytes(StandardCharsets.ISO_8859_1)); out.flush(); }
                                }
                            }
                        } catch (Exception ignored) { }
                    }).start();
                }
            } catch (Exception ignored) { }
        });
        t.setDaemon(true); t.start();
        return t;
    }

    @Test
    void aSilentDeadLinkIsProbedThenReconnected() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            AtomicInteger connections = new AtomicInteger(), blanks = new AtomicInteger();
            serve(ss, connections, blanks, false);                     // never answers a blank line: as a dead link would not
            Feed f = new Feed(ss.getLocalPort());
            f.setIdleLimits(700, 1800);
            f.start();
            assertTrue(waitFor(() -> f.isConnected() && !f.lines.isEmpty()), "connected and got the spot");
            assertEquals(1, connections.get());
            assertTrue(waitFor(() -> blanks.get() >= 1), "a blank line was sent as a probe once it went quiet");
            assertTrue(waitFor(() -> connections.get() >= 2, 15_000), "gave up on the silent link and connected again");
            assertTrue(f.rawSince(0).stream().anyMatch(l -> l.text().contains("looks dead")), "the console log says why it reconnected");
        }
    }

    @Test
    void aQuietButAnsweringServerIsLeftConnected() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            AtomicInteger connections = new AtomicInteger(), blanks = new AtomicInteger();
            serve(ss, connections, blanks, true);                      // answers each probe with its prompt
            Feed f = new Feed(ss.getLocalPort());
            f.setIdleLimits(500, 1500);
            f.start();
            assertTrue(waitFor(() -> f.isConnected() && !f.lines.isEmpty()), "connected");
            Thread.sleep(6000);                                         // several probe cycles
            assertTrue(blanks.get() >= 2, "it kept probing: " + blanks.get());
            assertEquals(1, connections.get(), "never reconnected a live link");
            assertTrue(f.isConnected());
        }
    }

    private static boolean waitFor(java.util.function.BooleanSupplier c) throws InterruptedException { return waitFor(c, 8000); }
    private static boolean waitFor(java.util.function.BooleanSupplier c, long ms) throws InterruptedException {
        for (long t = 0; t < ms; t += 100) { if (c.getAsBoolean()) return true; Thread.sleep(100); }
        return false;
    }
}
