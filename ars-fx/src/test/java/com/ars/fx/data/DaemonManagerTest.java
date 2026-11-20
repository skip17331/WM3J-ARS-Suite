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
    void theSatelliteRotorGetsItsOwnRotctldOnItsOwnPort() throws Exception {
        withConfig(() -> {
            assertNull(DaemonManager.argvFor("rotor2"), "no model number, nothing to start");
            HubConfig.set("rotor.hamlibModel", "202"); HubConfig.set("rotor.port", "/dev/ttyUSB0");
            HubConfig.set("rotor2.hamlibModel", "204"); HubConfig.set("rotor2.port", "/dev/ttyUSB1"); HubConfig.set("rotor2.baud", "19200");
            assertEquals(List.of("rotctld", "-m", "202", "-r", "/dev/ttyUSB0", "-t", "4533", "-T", "127.0.0.1"), DaemonManager.argvFor("rotor"));
            assertEquals(List.of("rotctld", "-m", "204", "-r", "/dev/ttyUSB1", "-s", "19200", "-t", "4534", "-T", "127.0.0.1"), DaemonManager.argvFor("rotor2"));
        });
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

    /** Run {@code body} as if the machine had exactly these USB serial devices ("" = none), whatever is really plugged in. */
    private static void withSerialPort(String found, Runnable body) {
        java.util.function.Supplier<String> old = DaemonManager.serialPortFinder;
        DaemonManager.serialPortFinder = () -> found;
        try { body.run(); } finally { DaemonManager.serialPortFinder = old; }
    }

    @Test
    void rtsWithoutASerialPortIsLeftAtTheDefault() throws Exception {
        withConfig(() -> withSerialPort("", () -> {          // no device on the machine, so nothing to name after -p
            HubConfig.set("rig.hamlibModel", "1035"); HubConfig.set("rig.port", ""); HubConfig.set("rig.ptt", "RTS");
            assertFalse(DaemonManager.argvFor("rig").contains("-P"));
        }));
    }

    @Test
    void aFreshInstallUsesTheOnlySerialDeviceItFinds() throws Exception {
        withConfig(() -> withSerialPort("/dev/ttyUSB7", () -> {
            HubConfig.set("rig.hamlibModel", "1035"); HubConfig.set("rig.port", ""); HubConfig.set("rig.ptt", "RTS");
            List<String> a = DaemonManager.argvFor("rig");
            assertEquals("/dev/ttyUSB7", a.get(a.indexOf("-r") + 1), "the one device found is used for CAT");
            assertEquals("/dev/ttyUSB7", a.get(a.indexOf("-p") + 1), "and for the RTS line");
            HubConfig.set("rig.port", "/dev/ttyUSB0");
            assertEquals("/dev/ttyUSB0", DaemonManager.argvFor("rig").get(DaemonManager.argvFor("rig").indexOf("-r") + 1), "a port the operator chose wins");
        }));
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
