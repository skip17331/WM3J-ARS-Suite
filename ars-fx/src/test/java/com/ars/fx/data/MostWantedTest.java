package com.ars.fx.data;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MostWantedTest {

    @Test void theReplyIsPlaceToEntityAndIsTurnedRound() {
        Map<Integer, Integer> m = MostWanted.parse("{\"1\":\"344\",\"2\":123,\"3\":\"138\",\"x\":\"5\",\"4\":\"\",\"5\":\"344\"}");
        assertEquals(1, m.get(344), "the first place wins when an entity appears twice");
        assertEquals(2, m.get(123), "numbers or strings");
        assertEquals(3, m.get(138));
        assertEquals(3, m.size(), "bad keys and values are skipped");
        assertTrue(MostWanted.parse("[1,2]").isEmpty());
    }

    @Test void spotsAlertOnTheTopN() {
        Map<String, Long> seen = new HashMap<>();
        long now = 1_000_000L;
        List<ClusterClient.Spot> spots = List.of(new ClusterClient.Spot(14_025_000L, "P5AA", "CW", "20m", "K1TTT", "", now),
                new ClusterClient.Spot(14_026_000L, "DL1AA", "CW", "20m", "K1TTT", "", now));
        SpotAlerts.Settings top10 = new SpotAlerts.Settings(true, false, List.of(), 10);
        List<SpotAlerts.Alert> a = SpotAlerts.check(spots, sp -> "", sp -> sp.call().equals("P5AA") ? 1 : 250, top10, now, seen);
        assertEquals(1, a.size());
        assertEquals("Most Wanted #1", a.get(0).reason());
        assertTrue(SpotAlerts.check(spots, sp -> "", sp -> 1, new SpotAlerts.Settings(true, false, List.of(), 0), now, new HashMap<>()).isEmpty(),
                "top 0 = off");
    }

    @Test void refreshStoresTheListAndARefusedTokenKeepsIt() throws Exception {
        String old = System.getProperty("user.home");
        Path home = Files.createTempDirectory("mw-home");
        StringBuilder reply = new StringBuilder("{");
        for (int i = 1; i <= 340; i++) reply.append(i > 1 ? "," : "").append('"').append(i).append("\":\"").append(i + 100).append('"');
        reply.append('}');
        int[] status = {200};
        String[] auth = {""};
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/api/v1/most-wanted", ex -> {
            auth[0] = ex.getRequestHeaders().getFirst("Authorization");
            byte[] b = (status[0] == 200 ? reply.toString() : "{\"error\":\"bad token\"}").getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status[0], b.length); ex.getResponseBody().write(b); ex.close();
        });
        s.start();
        System.setProperty("user.home", home.toString());
        System.setProperty("clublog.mostwanted.url", "http://127.0.0.1:" + s.getAddress().getPort() + "/api/v1/most-wanted?mode=0&band=0&club=0");
        String oldTok = MostWanted.token();
        try {
            MostWanted.setToken("cl_test");
            assertTrue(MostWanted.stale(System.currentTimeMillis()), "no copy yet");
            assertTrue(MostWanted.refresh().contains("340 entities"));
            assertEquals("Bearer cl_test", auth[0]);
            assertEquals(1, MostWanted.rankOfEntity(101));
            assertFalse(MostWanted.stale(System.currentTimeMillis()), "a fresh copy is not refetched");
            assertTrue(MostWanted.stale(System.currentTimeMillis() + MostWanted.MAX_AGE_MS + 1), "a month later it is");
            status[0] = 401;
            assertTrue(MostWanted.refresh().contains("refused"));
            assertEquals(1, MostWanted.rankOfEntity(101), "a refused refresh keeps the old list");
        } finally {
            s.stop(0);
            MostWanted.setToken(oldTok);
            System.clearProperty("clublog.mostwanted.url");
            System.setProperty("user.home", old);
            MostWanted.setForTest(Map.of());
        }
    }
}
