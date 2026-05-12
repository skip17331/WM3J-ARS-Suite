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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Uses an in-process ServerSocket that pretends to be {@code rigctld} —
 * captures the exact bytes a {@link HamlibRigControl} sends and replies
 * with a configurable {@code RPRT N} line. No real rig or daemon needed.
 */
class HamlibRigControlTest {

    private ServerSocket server;
    private Thread       acceptThread;
    private final List<String> received = new ArrayList<>();
    private final AtomicReference<String> replyLine = new AtomicReference<>("RPRT 0");

    @BeforeEach
    void startFakeRigctld() throws IOException {
        server = new ServerSocket(0); // ephemeral port
        acceptThread = new Thread(this::accept, "fake-rigctld");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    @AfterEach
    void stopFakeRigctld() throws IOException {
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
            } catch (IOException stop) {
                return; // server closed
            }
        }
    }

    @Test
    void pttOnSendsTSpaceOne() throws Exception {
        HamlibRigControl r = new HamlibRigControl("127.0.0.1", server.getLocalPort());
        r.setPtt(true);
        synchronized (received) {
            assertEquals(List.of("T 1"), received,
                "PTT on should send exactly 'T 1\\n'");
        }
    }

    @Test
    void pttOffSendsTSpaceZero() throws Exception {
        HamlibRigControl r = new HamlibRigControl("127.0.0.1", server.getLocalPort());
        r.setPtt(false);
        synchronized (received) {
            assertEquals(List.of("T 0"), received);
        }
    }

    @Test
    void rprtNonZeroThrows() {
        replyLine.set("RPRT -5");
        HamlibRigControl r = new HamlibRigControl("127.0.0.1", server.getLocalPort());
        Exception ex = assertThrows(IOException.class, () -> r.setPtt(true));
        assertTrue(ex.getMessage().contains("-5"),
            "error message should include the RPRT code: " + ex.getMessage());
    }

    @Test
    void malformedReplyThrows() {
        replyLine.set("WAT 0");
        HamlibRigControl r = new HamlibRigControl("127.0.0.1", server.getLocalPort());
        assertThrows(IOException.class, () -> r.setPtt(true));
    }

    @Test
    void connectionRefusedSurfaces() {
        // Use a port we just closed → connection refused, fast.
        int closedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            closedPort = s.getLocalPort();
        } catch (IOException e) { fail(e.getMessage()); return; }

        HamlibRigControl r = new HamlibRigControl("127.0.0.1", closedPort);
        assertThrows(IOException.class, () -> r.setPtt(true));
    }

    @Test
    void parseRprtAcceptsZero() throws IOException {
        HamlibRigControl.parseRprt("RPRT 0", "T 1\n");   // no throw
    }

    @Test
    void parseRprtRejectsNullAndJunk() {
        assertThrows(IOException.class, () -> HamlibRigControl.parseRprt(null, "T 1\n"));
        assertThrows(IOException.class, () -> HamlibRigControl.parseRprt("",   "T 1\n"));
        assertThrows(IOException.class, () -> HamlibRigControl.parseRprt("RPRT abc", "T 1\n"));
    }

    @Test
    void defaultsAreLocalhost4532() {
        HamlibRigControl r = new HamlibRigControl();
        assertEquals("127.0.0.1", r.host());
        assertEquals(4532,        r.port());
    }
}
