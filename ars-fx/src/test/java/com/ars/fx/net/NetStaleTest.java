package com.ars.fx.net;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Sockets that the pings keep alive but that are stuck in the handshake do not hang a station for ever. */
class NetStaleTest {
    final List<NetNode> nodes = new ArrayList<>();
    @AfterEach void stopAll() { for (NetNode n : nodes) n.stop(); }

    static boolean await(BooleanSupplier c, long ms) throws Exception {
        for (long t = 0; t < ms; t += 50) { if (c.getAsBoolean()) return true; Thread.sleep(50); }
        return false;
    }

    @Test void aHostThatNeverSendsItsChallengeIsGivenUpOnAndTriedAgain() throws Exception {
        AtomicInteger opens = new AtomicInteger();
        WebSocketServer silent = new WebSocketServer(new InetSocketAddress("127.0.0.1", 0)) {     // accepts the socket and says nothing
            @Override public void onOpen(WebSocket c, ClientHandshake h) { opens.incrementAndGet(); }
            @Override public void onClose(WebSocket c, int code, String r, boolean remote) { }
            @Override public void onMessage(WebSocket c, String m) { }
            @Override public void onError(WebSocket c, Exception e) { }
            @Override public void onStart() { }
        };
        silent.start();
        try {
            assertTrue(await(() -> silent.getPort() > 0, 3000));
            MemStore s = new MemStore("CLI", "CQ_WW_CW");
            NetNode n = new NetNode("CLI", "OP", "contest-key", s, () -> new NetStatus("CLI", "OP", "CQ_WW_CW", "20m", "CW", 14_025_000L, 0));
            nodes.add(n);
            n.setHandshakeLimitsForTest(800, 15_000);
            n.startJoin("127.0.0.1", silent.getPort());
            assertTrue(await(() -> opens.get() >= 1, 5000), "first connection");
            assertTrue(await(() -> opens.get() >= 2, 12_000), "the stuck connection was dropped and a new one made: " + opens.get());
            assertFalse(n.online());
            assertTrue(n.problem().contains("handshake"), n.problem());
        } finally { silent.stop(100); }
    }

    @Test void aConnectionThatNeverSaysHelloIsClosedByTheHost() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW");
        NetNode h = new NetNode("HOST", "OP", "contest-key", hs, () -> new NetStatus("HOST", "OP", "CQ_WW_CW", "20m", "CW", 14_025_000L, 0));
        nodes.add(h);
        h.setHandshakeLimitsForTest(15_000, 700);
        h.startHost(0);
        WebSocketClient idle = new WebSocketClient(new URI("ws://127.0.0.1:" + h.port())) {      // connects and never proves the key
            @Override public void onOpen(ServerHandshake x) { }
            @Override public void onMessage(String m) { }
            @Override public void onClose(int c, String r, boolean remote) { }
            @Override public void onError(Exception e) { }
        };
        idle.connectBlocking();
        assertTrue(idle.isOpen());
        assertTrue(await(idle::isClosed, 14_000), "the host closed the connection that never authenticated");
    }
}
