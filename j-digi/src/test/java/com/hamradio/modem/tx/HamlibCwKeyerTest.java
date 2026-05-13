package com.hamradio.modem.tx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** In-process fake-rigctld covers the CW-via-CAT path the same way
 *  HamlibRigControlTest covers PTT. */
class HamlibCwKeyerTest {

    private ServerSocket server;
    private Thread       acceptThread;
    private final List<String> received = new ArrayList<>();
    private final AtomicReference<String> replyLine = new AtomicReference<>("RPRT 0");

    @BeforeEach
    void start() throws IOException {
        server = new ServerSocket(0);
        acceptThread = new Thread(this::accept, "fake-rigctld-cw");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    @AfterEach
    void stop() throws IOException {
        if (server != null) server.close();
    }

    private void accept() {
        while (!server.isClosed()) {
            try (Socket s = server.accept();
                 BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                 Writer out = new OutputStreamWriter(
                     s.getOutputStream(), StandardCharsets.UTF_8)) {
                String line = in.readLine();
                if (line == null) continue;
                synchronized (received) { received.add(line); }
                out.write(replyLine.get() + "\n");
                out.flush();
            } catch (IOException stop) { return; }
        }
    }

    @Test
    void sendsBPrefixedCommand() throws Exception {
        // 60 WPM × "HI" = ~400 ms estimated transmit; well under the await window.
        HamlibCwKeyer k = new HamlibCwKeyer("127.0.0.1", server.getLocalPort(), 60);
        CountDownLatch done = new CountDownLatch(1);
        k.transmit("HI", null, done::countDown, null, e -> done.countDown());
        assertTrue(done.await(3, TimeUnit.SECONDS), "completion callback must fire");

        synchronized (received) {
            assertEquals(1, received.size());
            assertEquals("b HI", received.get(0),
                "must send the exact 'b <text>' command");
        }
    }

    @Test
    void completedFiresAfterEstimatedDuration() throws Exception {
        HamlibCwKeyer k = new HamlibCwKeyer("127.0.0.1", server.getLocalPort(), 60);
        long expected = HamlibCwKeyer.estimateMorseMs("HI", 60);  // very short
        long t0 = System.currentTimeMillis();
        CountDownLatch done = new CountDownLatch(1);
        k.transmit("HI", null, done::countDown, null, e -> fail("error: " + e));
        assertTrue(done.await(3, TimeUnit.SECONDS));
        long elapsed = System.currentTimeMillis() - t0;
        assertTrue(elapsed >= expected - 50,
            "completed too early: elapsed=" + elapsed + " expected≥" + expected);
    }

    @Test
    void cancelFiresCancelledCallback() throws Exception {
        // 5 WPM → very slow; gives us time to cancel before the wait expires.
        HamlibCwKeyer k = new HamlibCwKeyer("127.0.0.1", server.getLocalPort(), 5);
        CountDownLatch cancelled = new CountDownLatch(1);
        CountDownLatch started   = new CountDownLatch(1);
        k.transmit("HELLO WORLD",
            started::countDown,
            () -> fail("completed should not fire after cancel"),
            cancelled::countDown,
            e -> fail("error: " + e));
        assertTrue(started.await(2, TimeUnit.SECONDS), "started must fire first");
        k.cancel();
        assertTrue(cancelled.await(3, TimeUnit.SECONDS),
            "cancelled callback must fire after cancel()");

        synchronized (received) {
            // First request is "b HELLO WORLD", second is "\stop_morse"
            assertEquals(2, received.size(), "expected b + stop_morse commands");
            assertEquals("b HELLO WORLD", received.get(0));
            assertEquals("\\stop_morse",  received.get(1));
        }
    }

    @Test
    void rigctldErrorRouteToOnError() throws Exception {
        replyLine.set("RPRT -11");
        HamlibCwKeyer k = new HamlibCwKeyer("127.0.0.1", server.getLocalPort(), 30);
        AtomicReference<String> errMsg = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        k.transmit("E", null, () -> fail("should not complete"), null, e -> {
            errMsg.set(e); done.countDown();
        });
        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertNotNull(errMsg.get());
        assertTrue(errMsg.get().contains("-11"),
            "error must mention RPRT code: " + errMsg.get());
    }

    @Test
    void doubleTransmitRejected() throws Exception {
        HamlibCwKeyer k = new HamlibCwKeyer("127.0.0.1", server.getLocalPort(), 5);
        CountDownLatch started = new CountDownLatch(1);
        k.transmit("LONG TEXT FOR TESTING", started::countDown, null, null, null);
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertThrows(IllegalStateException.class,
            () -> k.transmit("X", null, null, null, null));
        k.cancel();   // clean up
    }

    @Test
    void estimateScalesWithLengthAndWpm() {
        long short_ = HamlibCwKeyer.estimateMorseMs("HI",  20);
        long long_  = HamlibCwKeyer.estimateMorseMs("HI HI HI HI HI", 20);
        assertTrue(long_ > short_, "longer text → longer estimate");

        long slow  = HamlibCwKeyer.estimateMorseMs("HELLO", 10);
        long fast  = HamlibCwKeyer.estimateMorseMs("HELLO", 40);
        assertTrue(slow > fast, "slower WPM → longer estimate");
        // 10 vs 40 WPM = 4× difference in dit duration. Accounting for the
        // 200 ms baseline, slow should be roughly 4× of (fast - 200) + 200.
        long fastWithoutBase = fast - 200;
        assertTrue(Math.abs((slow - 200) - 4 * fastWithoutBase) < 100,
            "WPM scaling should be ~linear: slow=" + slow + " fast=" + fast);
    }

    @Test
    void emptyTextEstimateIsZero() {
        assertEquals(0L, HamlibCwKeyer.estimateMorseMs("", 20));
        assertEquals(0L, HamlibCwKeyer.estimateMorseMs(null, 20));
    }
}
