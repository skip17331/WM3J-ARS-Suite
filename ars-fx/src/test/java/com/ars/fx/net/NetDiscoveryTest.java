package com.ars.fx.net;

import static org.junit.jupiter.api.Assertions.*;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class NetDiscoveryTest {
    static final InetAddress LAN = addr("192.168.1.20");
    static InetAddress addr(String s) { try { return InetAddress.getByName(s); } catch (Exception e) { throw new IllegalStateException(e); } }

    @Test void aHostAnnouncementIsReadAndTakesItsAddressFromWhereItCameFrom() {
        NetDiscovery.Found f = NetDiscovery.parse(NetDiscovery.beacon("W1AW-1", 12070, "CQ_WW_CW"), LAN);
        assertEquals(new NetDiscovery.Found("W1AW-1", "192.168.1.20", 12070, "CQ_WW_CW"), f);
        assertTrue(f.label().contains("W1AW-1") && f.label().contains("192.168.1.20:12070") && f.label().contains("CQ_WW_CW"));
        assertEquals("", NetDiscovery.parse(NetDiscovery.beacon("HOST", 12070, null), LAN).contest(), "no contest open yet");
        assertEquals("A B", NetDiscovery.parse(NetDiscovery.beacon("A|B", 12070, ""), LAN).station(), "the separator cannot be smuggled into a name");
    }

    @Test void anythingElseIsIgnored() {
        assertNull(NetDiscovery.parse("hello", LAN));
        assertNull(NetDiscovery.parse("OTHER|HOST|12070|X", LAN));
        assertNull(NetDiscovery.parse("ARS-NET1|HOST|0|X", LAN), "port 0");
        assertNull(NetDiscovery.parse("ARS-NET1|HOST|70000|X", LAN), "port too high");
        assertNull(NetDiscovery.parse("ARS-NET1|HOST|abc|X", LAN));
        assertNull(NetDiscovery.parse("ARS-NET1||12070|X", LAN), "no name");
        assertNull(NetDiscovery.parse("ARS-NET1|HOST|12070", LAN), "a field short");
        assertNull(NetDiscovery.parse("ARS-NET1|HOST|12070|" + "x".repeat(300), LAN), "too long");
        assertNull(NetDiscovery.parse(null, LAN));
        assertNull(NetDiscovery.parse(NetDiscovery.beacon("HOST", 12070, ""), null));
    }

    @Test void aScanHearsAnAnnouncingHostOnce() throws Exception {
        int port;
        try (DatagramSocket s = new DatagramSocket(0)) { port = s.getLocalPort(); }
        CompletableFuture<List<NetDiscovery.Found>> scan = CompletableFuture.supplyAsync(() -> NetDiscovery.scan(2500, port));
        Thread.sleep(300);
        try (NetDiscovery.Announcer a = NetDiscovery.announceTo(() -> "HOSTSTN", () -> 12070, () -> "CQ_WW_CW", List.of(new InetSocketAddress(InetAddress.getLoopbackAddress(), port)))) {
            List<NetDiscovery.Found> found = scan.get();
            assertEquals(1, found.size(), "announced twice in the window, listed once: " + found);
            assertEquals("HOSTSTN", found.get(0).station());
            assertEquals(12070, found.get(0).port());
            assertEquals("CQ_WW_CW", found.get(0).contest());
            assertEquals("127.0.0.1", found.get(0).host());
        }
    }

    @Test void aScanOfAQuietNetworkFindsNothing() {
        int port;
        try (DatagramSocket s = new DatagramSocket(0)) { port = s.getLocalPort(); } catch (Exception e) { throw new IllegalStateException(e); }
        assertTrue(NetDiscovery.scan(300, port).isEmpty());
    }
}
