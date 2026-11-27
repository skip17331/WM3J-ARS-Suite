package com.ars.fx;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The background hub and the loose modules a window starts run on that window's own home folder. */
class HubServerLaunchTest {
    @Test void theChildGetsThisJvmsHomeFolder() {
        String old = System.getProperty("user.home");
        try {
            System.setProperty("user.home", "/tmp/some-scratch-home");
            List<String> cmd = HubServer.javaCmd("com.ars.fx.HubServer");
            assertTrue(cmd.contains("-Duser.home=/tmp/some-scratch-home"), cmd.toString());
        } finally { System.setProperty("user.home", old); }
    }

    @Test void theHomeFlagComesBeforeTheMainClassAndTheMainClassStaysLast() {
        List<String> cmd = HubServer.javaCmd("com.ars.fx.HubServer");
        int home = cmd.indexOf("-Duser.home=" + System.getProperty("user.home"));
        assertTrue(home > 0 && home < cmd.indexOf("com.ars.fx.HubServer"), cmd.toString());
        assertEquals("com.ars.fx.HubServer", cmd.get(cmd.size() - 1), "ensureRunning() inserts its flag just before the last element");
    }

    @Test void appArgumentsFollowTheMainClass() {
        List<String> cmd = HubServer.javaCmd("com.ars.fx.Main", "--module", "map");
        assertEquals(List.of("com.ars.fx.Main", "--module", "map"), cmd.subList(cmd.size() - 3, cmd.size()));
    }
}
