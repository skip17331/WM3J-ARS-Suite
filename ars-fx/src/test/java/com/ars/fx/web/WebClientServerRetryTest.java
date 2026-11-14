package com.ars.fx.web;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.HubConfig;
import java.net.ServerSocket;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The phone web client comes up by itself when its port was busy at start-up, and its requests are time-limited. */
class WebClientServerRetryTest {
    @Test
    void aBusyPortIsRetriedUntilItIsFree(@TempDir Path t) throws Exception {
        String oldHome = System.getProperty("user.home");
        System.setProperty("user.home", t.toString()); HubConfig.reload();
        WebClientServer.setUidFileForTest(t.resolve("uids.txt"));
        ServerSocket busy = new ServerSocket(0);
        try {
            int p = busy.getLocalPort();
            HubConfig.setBool(WebClientServer.KEY_ENABLED, true);
            HubConfig.set(WebClientServer.KEY_PORT, String.valueOf(p));
            WebClientServer.apply();                                         // the old hub still holds the port
            assertEquals(-1, WebClientServer.boundPort(), "could not bind while the port is busy");
            WebClientServer.retryIfDown();
            assertEquals(-1, WebClientServer.boundPort(), "still busy: still down, and no crash");
            busy.close();                                                    // the old process lets go
            WebClientServer.retryIfDown();
            assertEquals(p, WebClientServer.boundPort(), "came up on the retry");
            assertEquals("30", System.getProperty("sun.net.httpserver.maxRspTime"), "a stalled phone cannot hold a worker thread for ever");
        } finally {
            try { busy.close(); } catch (Exception ignored) { }
            WebClientServer.stop(); WebClientServer.setUidFileForTest(null);
            System.setProperty("user.home", oldHome); HubConfig.reload();
        }
    }
}
