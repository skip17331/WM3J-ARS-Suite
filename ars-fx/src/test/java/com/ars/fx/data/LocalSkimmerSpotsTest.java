package com.ars.fx.data;

import com.hamradio.modem.dsp.SkimmerSpotPublisher.LocalSpot;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Callsigns the local CW skimmer hears become spots in the shared list. */
class LocalSkimmerSpotsTest {
    private static LocalSpot heard(String call, long hz) { return new LocalSpot(call, hz, 18, "CQ", 2, 0.85, 1_000L); }

    private static void withConfig(java.util.concurrent.Callable<Void> body) throws Exception {
        String old = System.getProperty("user.home");
        System.setProperty("user.home", Files.createTempDirectory("skim-spots").toString());     // not the user's real config
        try { HubConfig.reload(); body.call(); }
        finally { System.setProperty("user.home", old); HubConfig.reload(); }
    }

    @Test
    void aHeardCallBecomesACwSpotTaggedAsTheLocalSkimmer() {
        ClusterClient.Spot s = DigiEngine.skimmerSpot(heard("W1AW", 14_025_700L), "WM3J", 5_000L);
        assertEquals("W1AW", s.call());
        assertEquals(14_025_700L, s.freqHz());
        assertEquals("CW", s.mode());
        assertEquals("20m", s.band());
        assertEquals("WM3J-SKM", s.de());
        assertEquals(5_000L, s.arrivalMs());
        assertTrue(s.comment().contains("reps=2") && s.comment().contains("18 dB"), s.comment());
        assertEquals("LOCAL-SKM", DigiEngine.skimmerSpot(heard("W1AW", 14_025_700L), " ", 0).de());
    }

    @Test
    void localSpotsGoToTheTopOfTheListAndTheListStaysBounded() {
        ClusterClient c = ClusterClient.getInstance();
        for (int i = 0; i < 60; i++) c.addLocalSpot(DigiEngine.skimmerSpot(heard("K" + i + "AA", 14_025_000L + i), "WM3J", i));
        List<ClusterClient.Spot> spots = c.spots();
        assertEquals("K59AA", spots.get(0).call());               // newest first
        assertEquals(40, spots.size());                           // the list's existing cap
    }

    /** Runs {@code body} with a hub listening on a spare local port, then shuts it down. */
    private static void withHub(int port, java.util.concurrent.Callable<Void> body) throws Exception {
        withConfig(() -> {
            HubConfig.set("remote.port", String.valueOf(port));
            HubConfig.setBool("remote.serverEnabled", false);        // loopback only
            RemoteServer.startHub();
            try { return body.call(); } finally { RemoteLink.disconnect(); RemoteServer.shutdown(); }
        });
    }

    private static boolean has(String call) { return ClusterClient.getInstance().spots().stream().anyMatch(s -> s.call().equals(call)); }

    private static boolean await(java.util.function.BooleanSupplier c, long ms) throws InterruptedException {
        for (long end = System.currentTimeMillis() + ms; System.currentTimeMillis() < end; Thread.sleep(50)) if (c.getAsBoolean()) return true;
        return c.getAsBoolean();
    }

    @Test
    void aWindowSendsItsSkimmerSpotAsASpotAddMessage() throws Exception {
        // A stand-in hub that just records what a window sends it (the real hub's own handler is covered below).
        java.util.concurrent.BlockingQueue<String> got = new java.util.concurrent.LinkedBlockingQueue<>();
        org.java_websocket.server.WebSocketServer hub = new org.java_websocket.server.WebSocketServer(new java.net.InetSocketAddress("127.0.0.1", 45393)) {
            @Override public void onOpen(org.java_websocket.WebSocket c, org.java_websocket.handshake.ClientHandshake h) {}
            @Override public void onClose(org.java_websocket.WebSocket c, int code, String reason, boolean remote) {}
            @Override public void onMessage(org.java_websocket.WebSocket c, String m) { got.add(m); }
            @Override public void onError(org.java_websocket.WebSocket c, Exception e) {}
            @Override public void onStart() {}
        };
        hub.setReuseAddr(true);
        hub.start();
        try {
            RemoteLink.connect("ws://127.0.0.1:45393", "test");
            RemoteLink link = RemoteLink.get();
            assertTrue(await(link::isConnected, 8000), "the window did not attach");
            link.sendSpot(DigiEngine.skimmerSpot(heard("VK3ABC", 14_030_100L), "WM3J", 1L));
            com.google.gson.JsonObject sent = null;
            for (long end = System.currentTimeMillis() + 5000; sent == null && System.currentTimeMillis() < end; ) {
                String m = got.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (m != null && m.contains("spot.add")) sent = com.google.gson.JsonParser.parseString(m).getAsJsonObject();
            }
            assertNotNull(sent, "no spot.add message arrived");
            assertEquals("VK3ABC", sent.get("call").getAsString());
            assertEquals(14_030_100L, sent.get("hz").getAsLong());
            assertEquals("CW", sent.get("mode").getAsString());
            assertEquals("20m", sent.get("band").getAsString());
            assertEquals("WM3J-SKM", sent.get("de").getAsString());
            assertTrue(sent.get("comment").getAsString().contains("Local skimmer"));
        } finally {
            RemoteLink.disconnect();
            hub.stop(1000);
        }
    }

    @Test
    void aSpotAddedToTheHubIsKeptThroughItsRebroadcasts() throws Exception {
        // A plain socket client, so no window link is injecting the hub's list back over itself (in one process the
        // hub and a window would share a single list; in the real app they are separate processes).
        withHub(45392, () -> {
            java.util.concurrent.atomic.AtomicBoolean open = new java.util.concurrent.atomic.AtomicBoolean();
            org.java_websocket.client.WebSocketClient c = new org.java_websocket.client.WebSocketClient(new java.net.URI("ws://127.0.0.1:45392")) {
                @Override public void onOpen(org.java_websocket.handshake.ServerHandshake h) { open.set(true); }
                @Override public void onMessage(String m) {}
                @Override public void onClose(int code, String reason, boolean remote) {}
                @Override public void onError(Exception e) {}
            };
            c.connect();
            assertTrue(await(open::get, 8000), "could not connect to the hub");
            ClusterClient.Spot sp = DigiEngine.skimmerSpot(heard("ZL2XYZ", 14_035_000L), "WM3J", 1L);
            c.send("{\"t\":\"spot.add\",\"hz\":" + sp.freqHz() + ",\"call\":\"ZL2XYZ\",\"mode\":\"CW\",\"band\":\"20m\",\"de\":\"WM3J-SKM\",\"comment\":\"x\"}");
            assertTrue(await(() -> has("ZL2XYZ"), 5000), "the hub did not add the spot");
            Thread.sleep(2500);                                       // two or more hub re-broadcasts
            assertTrue(has("ZL2XYZ"), "the spot was lost");
            c.close();
            return null;
        });
    }
}
