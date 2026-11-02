package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClusterServersTest {

    @Test
    void readsAddressFromAListEntryOrTypedText() {
        assertEquals("dxc.w3lpl.net:7373", ClusterServers.addressOf("W3LPL · dxc.w3lpl.net:7373"));
        assertEquals("my.cluster.org:8000", ClusterServers.addressOf("  my.cluster.org:8000 "));
    }

    @Test
    void labelsKnownServersAndLeavesOthersAlone() {
        assertEquals("VE7CC · dxc.ve7cc.net:23", ClusterServers.label("DXC.VE7CC.NET:23"));
        assertEquals("my.cluster.org:8000", ClusterServers.label("my.cluster.org:8000"));
    }

    @Test
    void splitsHostAndPort() {
        assertEquals("dxc.w1nr.net", ClusterServers.host("dxc.w1nr.net:7300"));
        assertEquals(7300, ClusterServers.port("dxc.w1nr.net:7300"));
        assertEquals(-1, ClusterServers.port("dxc.w1nr.net"));
        assertEquals(-1, ClusterServers.port("dxc.w1nr.net:abc"));
    }
}
