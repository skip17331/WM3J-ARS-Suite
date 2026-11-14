package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** A hub link that is up but has gone silent is told from one that is healthy, being closed, or already down. */
class RemoteLinkQuietTest {
    @Test
    void onlyAnUpLinkThatWentSilentIsQuiet() {
        long now = 1_000_000;
        assertTrue(RemoteLink.hubQuiet(true, false, now, now - 25_000, 20_000), "up, nothing for 25 s: the hub's broadcast has stalled");
        assertFalse(RemoteLink.hubQuiet(true, false, now, now - 1_500, 20_000), "state arrives every second: healthy");
        assertFalse(RemoteLink.hubQuiet(true, false, now, now - 20_000, 20_000), "right at the limit is not over it");
        assertFalse(RemoteLink.hubQuiet(false, false, now, now - 60_000, 20_000), "already down: the reconnect loop has it");
        assertFalse(RemoteLink.hubQuiet(true, true, now, now - 60_000, 20_000), "the window is closing it on purpose");
    }
}
