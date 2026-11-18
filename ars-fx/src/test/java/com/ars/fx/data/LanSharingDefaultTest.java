package com.ars.fx.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The hub is reachable from this computer only until the operator switches LAN sharing on (Setup > Remote sharing). */
class LanSharingDefaultTest {

    private String oldHome;

    @BeforeEach void home(@TempDir Path tmp) {
        oldHome = System.getProperty("user.home");
        System.setProperty("user.home", tmp.toString());
        HubConfig.reload();
    }

    @AfterEach void restore() { System.setProperty("user.home", oldHome); HubConfig.reload(); }

    private static String bindHost() throws Exception {
        Method m = RemoteServer.class.getDeclaredMethod("bindHost");
        m.setAccessible(true);
        return (String) m.invoke(null);
    }

    @Test void aFreshInstallListensOnThisComputerOnly() throws Exception {
        assertFalse(RemoteServer.LAN_SHARED_DEFAULT);
        assertEquals("127.0.0.1", bindHost(), "used to be 0.0.0.0: any device on the LAN could tune and rotate");
    }

    @Test void switchingItOnOpensItToTheLan() throws Exception {
        HubConfig.setBool("remote.serverEnabled", true);
        assertEquals("0.0.0.0", bindHost());
        HubConfig.setBool("remote.serverEnabled", false);
        assertEquals("127.0.0.1", bindHost());
    }
}
