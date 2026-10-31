package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DaemonManagerTest {
    private static void withConfig(Runnable body) throws Exception {
        String old = System.getProperty("user.home");
        System.setProperty("user.home", Files.createTempDirectory("daemon-args").toString());     // not the user's real config
        try { HubConfig.reload(); body.run(); }
        finally { System.setProperty("user.home", old); HubConfig.reload(); }
    }

    @Test
    void noHamlibModelMeansNothingToStart() throws Exception {
        withConfig(() -> assertNull(DaemonManager.argvFor("rig")));
    }

    @Test
    void catPttAddsNoPttFlags() throws Exception {
        withConfig(() -> {
            HubConfig.set("rig.hamlibModel", "1035"); HubConfig.set("rig.port", "/dev/ttyUSB0"); HubConfig.set("rig.baud", "19200");
            List<String> a = DaemonManager.argvFor("rig");
            assertEquals(List.of("rigctld", "-m", "1035", "-r", "/dev/ttyUSB0", "-s", "19200", "-t", "4532", "-T", "127.0.0.1"), a);
            HubConfig.set("rig.ptt", "CAT");
            assertEquals(a, DaemonManager.argvFor("rig"));
            HubConfig.set("rig.ptt", "VOX");
            assertEquals(a, DaemonManager.argvFor("rig"));
        });
    }

    @Test
    void rtsAndDtrTellRigctldThePttTypeAndDevice() throws Exception {
        withConfig(() -> {
            HubConfig.set("rig.hamlibModel", "1035"); HubConfig.set("rig.port", "/dev/ttyUSB0");
            HubConfig.set("rig.ptt", "RTS");
            List<String> rts = DaemonManager.argvFor("rig");
            int i = rts.indexOf("-P");
            assertTrue(i > 0);
            assertEquals("RTS", rts.get(i + 1));
            assertEquals("-p", rts.get(i + 2));
            assertEquals("/dev/ttyUSB0", rts.get(i + 3));

            HubConfig.set("rig.ptt", "DTR");
            assertEquals("DTR", DaemonManager.argvFor("rig").get(DaemonManager.argvFor("rig").indexOf("-P") + 1));
            assertNotEquals(rts, DaemonManager.argvFor("rig"));      // a PTT change alters the command line → the daemon is restarted
        });
    }

    @Test
    void rtsWithoutASerialPortIsLeftAtTheDefault() throws Exception {
        withConfig(() -> {
            HubConfig.set("rig.hamlibModel", "1035"); HubConfig.set("rig.port", ""); HubConfig.set("rig.ptt", "RTS");
            assertFalse(DaemonManager.argvFor("rig").contains("-P"));
        });
    }

    @Test
    void rotorAndAmpNeverGetPttFlags() throws Exception {
        withConfig(() -> {
            HubConfig.set("rig.ptt", "RTS");
            HubConfig.set("rotor.hamlibModel", "603"); HubConfig.set("rotor.port", "/dev/ttyUSB1");
            HubConfig.set("amp.hamlibModel", "201"); HubConfig.set("amp.port", "/dev/ttyUSB2");
            assertFalse(DaemonManager.argvFor("rotor").contains("-P"));
            assertFalse(DaemonManager.argvFor("amp").contains("-P"));
        });
    }
}
