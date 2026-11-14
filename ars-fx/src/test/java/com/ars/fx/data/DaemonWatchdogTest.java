package com.ars.fx.data;

import com.ars.fx.data.DaemonWatchdog.Action;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The watchdog repairs a daemon whose radio dropped off USB and came back, without anyone restarting it by hand. */
class DaemonWatchdogTest {
    @Test
    void rulesForOneDaemon() {
        long old = 60_000;
        // the device vanished while the daemon runs: stop it, it holds a dead handle
        assertEquals(Action.STOP, DaemonWatchdog.decide(true, true, null, "dev#1", old, 0));
        // vanished and nothing running: nothing to do, wait
        assertEquals(Action.NONE, DaemonWatchdog.decide(false, true, null, null, 0, 0));
        // it came back and no daemon is running: start
        assertEquals(Action.START, DaemonWatchdog.decide(false, true, "dev#2", null, 0, 0));
        // it came back as a different node while the daemon still held the old one: restart
        assertEquals(Action.RESTART, DaemonWatchdog.decide(true, true, "dev#2", "dev#1", old, 0));
        // healthy: same device, answering
        assertEquals(Action.NONE, DaemonWatchdog.decide(true, true, "dev#1", "dev#1", old, 0));
        // alive, same device, but not answering three probes in a row: restart; not within the start-up grace
        assertEquals(Action.RESTART, DaemonWatchdog.decide(true, true, "dev#1", "dev#1", old, 3));
        assertEquals(Action.NONE, DaemonWatchdog.decide(true, true, "dev#1", "dev#1", 2_000, 5));
        assertEquals(Action.NONE, DaemonWatchdog.decide(true, true, "dev#1", "dev#1", old, 2));
        // a setting that is not a /dev path (COM port, network): only a dead daemon is started again
        assertEquals(Action.START, DaemonWatchdog.decide(false, false, null, null, 0, 0));
        assertEquals(Action.NONE, DaemonWatchdog.decide(true, false, null, null, old, 0));
    }

    @Test
    void deviceIdentityChangesWhenTheNodeIsRecreated() throws Exception {
        Path dir = Files.createTempDirectory("wd-dev");
        Path dev = dir.resolve("ttyUSB0");
        assertNull(DaemonWatchdog.deviceId(dev.toString()), "absent");
        assertNull(DaemonWatchdog.deviceId("COM3"), "not a path");
        assertNull(DaemonWatchdog.deviceId(""), "blank");
        Files.writeString(dev, "x");
        String a = DaemonWatchdog.deviceId(dev.toString());
        assertNotNull(a);
        assertEquals(a, DaemonWatchdog.deviceId(dev.toString()), "stable while untouched");
        Files.delete(dev); Thread.sleep(20); Files.writeString(dev, "x");
        assertNotEquals(a, DaemonWatchdog.deviceId(dev.toString()), "a recreated node is a different one");
        Path link = dir.resolve("by-id");
        Files.createSymbolicLink(link, dev);
        assertNotNull(DaemonWatchdog.deviceId(link.toString()), "a symlink to the node (the /dev/serial/by-id style)");
    }

    @Test
    void repairsADaemonAfterTheDeviceDropsAndReturns() throws Exception {
        Assumptions.assumeFalse(System.getProperty("os.name", "").toLowerCase().contains("win"), "uses a shell script");
        String oldHome = System.getProperty("user.home");
        Path dir = Files.createTempDirectory("wd-repair");
        System.setProperty("user.home", dir.toString());
        try {
            Path log = dir.resolve("starts.log");
            Path bin = dir.resolve("fake-rigctld");
            Files.writeString(bin, "#!/bin/sh\necho \"$@\" >> '" + log + "'\nexec sleep 300\n");
            Files.setPosixFilePermissions(bin, PosixFilePermissions.fromString("rwxr-xr-x"));
            Path dev = dir.resolve("ttyUSB0");

            HubConfig.reload();
            HubConfig.set("rigctld.bin", bin.toString());
            HubConfig.set("rig.hamlibModel", "3073"); HubConfig.set("rig.port", dev.toString());
            HubConfig.set("rig.rigctldPort", "45331");
            HubConfig.setBool("rig.startDaemon", true);
            DaemonWatchdog.resetLimits();

            // the radio is off at start-up: no device, so no daemon is started (it would only exit at once)
            DaemonManager.ensure("rig");
            Thread.sleep(300);
            assertFalse(DaemonManager.running("rig"));
            assertEquals(0, startsIn(log).size());

            // it appears: the watchdog starts the daemon
            Files.writeString(dev, "x");
            DaemonWatchdog.check("rig");
            assertTrue(waitFor(() -> startsIn(log).size() == 1), "started when the device appeared");
            assertTrue(DaemonManager.running("rig"));

            DaemonWatchdog.check("rig");                         // nothing changed: left alone
            Thread.sleep(300);
            assertEquals(1, startsIn(log).size());

            // the USB link drops: the daemon is stopped, not left holding a dead handle
            Files.delete(dev);
            DaemonWatchdog.check("rig");
            assertFalse(DaemonManager.running("rig"), "stopped while the device is gone");
            assertTrue(DaemonWatchdog.last("rig").contains("gone"), DaemonWatchdog.last("rig"));

            // and returns: started again
            Files.writeString(dev, "x");
            DaemonWatchdog.check("rig");
            assertTrue(waitFor(() -> startsIn(log).size() == 2), "started again when it returned");

            // it is re-plugged between two checks (the node is recreated, never seen missing): restarted
            Files.delete(dev); Thread.sleep(20); Files.writeString(dev, "x");
            DaemonWatchdog.check("rig");
            assertTrue(waitFor(() -> startsIn(log).size() == 3), "restarted for the new node");
            assertTrue(DaemonManager.running("rig"));
        } finally {
            DaemonManager.stop("rig");
            DaemonWatchdog.resetLimits();
            System.setProperty("user.home", oldHome);
            HubConfig.reload();
        }
    }

    @Test
    void aDaemonStartedByHandIsLeftAlone() throws Exception {
        Assumptions.assumeFalse(System.getProperty("os.name", "").toLowerCase().contains("win"), "uses a shell script");
        String oldHome = System.getProperty("user.home");
        Path dir = Files.createTempDirectory("wd-external");
        System.setProperty("user.home", dir.toString());
        try (java.net.ServerSocket external = new java.net.ServerSocket(0)) {
            Path log = dir.resolve("starts.log");
            Path bin = dir.resolve("fake-rigctld");
            Files.writeString(bin, "#!/bin/sh\necho \"$@\" >> '" + log + "'\nexec sleep 300\n");
            Files.setPosixFilePermissions(bin, PosixFilePermissions.fromString("rwxr-xr-x"));
            Path dev = dir.resolve("ttyUSB0"); Files.writeString(dev, "x");
            HubConfig.reload();
            HubConfig.set("rigctld.bin", bin.toString());
            HubConfig.set("rig.hamlibModel", "3073"); HubConfig.set("rig.port", dev.toString());
            HubConfig.set("rig.rigctldPort", String.valueOf(external.getLocalPort()));   // someone else's daemon is listening here
            HubConfig.setBool("rig.startDaemon", true);
            DaemonWatchdog.resetLimits();
            for (int i = 0; i < 8; i++) DaemonWatchdog.check("rig");
            Thread.sleep(300);
            assertEquals(0, startsIn(log).size(), "never starts a second one over a daemon it did not start");
            assertEquals("", DaemonWatchdog.last("rig"));
        } finally {
            DaemonManager.stop("rig");
            DaemonWatchdog.resetLimits();
            System.setProperty("user.home", oldHome);
            HubConfig.reload();
        }
    }

    private static List<String> startsIn(Path log) {
        try { return Files.exists(log) ? Files.readAllLines(log) : List.of(); } catch (Exception e) { return List.of(); }
    }

    private static boolean waitFor(java.util.function.BooleanSupplier c) throws InterruptedException {
        for (int i = 0; i < 100; i++) { if (c.getAsBoolean()) return true; Thread.sleep(100); }
        return false;
    }
}
