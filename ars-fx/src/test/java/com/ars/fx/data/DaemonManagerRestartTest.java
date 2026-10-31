package com.ars.fx.data;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A running daemon is restarted when a setting on its command line changes, and left alone otherwise. */
class DaemonManagerRestartTest {
    @Test
    void restartsOnlyWhenTheCommandLineChanges() throws Exception {
        Assumptions.assumeFalse(System.getProperty("os.name", "").toLowerCase().contains("win"), "uses a shell script");
        String oldHome = System.getProperty("user.home");
        Path dir = Files.createTempDirectory("daemon-restart");
        System.setProperty("user.home", dir.toString());
        try {
            // a stand-in rigctld: log how it was started, then stay alive until killed
            Path log = dir.resolve("starts.log");
            Path bin = dir.resolve("fake-rigctld");
            Files.writeString(bin, "#!/bin/sh\necho \"$@\" >> '" + log + "'\nexec sleep 300\n");
            Files.setPosixFilePermissions(bin, PosixFilePermissions.fromString("rwxr-xr-x"));

            HubConfig.reload();
            HubConfig.set("rigctld.bin", bin.toString());
            HubConfig.set("rig.hamlibModel", "1035"); HubConfig.set("rig.port", "/dev/ttyUSB9");
            HubConfig.set("rig.rigctldPort", "45329");                       // a port nothing listens on
            HubConfig.setBool("rig.startDaemon", true);

            DaemonManager.ensure("rig");
            assertTrue(waitFor(() -> startsIn(log).size() == 1), "first start");
            assertFalse(startsIn(log).get(0).contains("-P"));

            DaemonManager.ensure("rig");                                     // same settings: no second start
            Thread.sleep(500);
            assertEquals(1, startsIn(log).size());

            HubConfig.set("rig.ptt", "RTS");                                 // PTT type changes the command line
            DaemonManager.ensure("rig");
            assertTrue(waitFor(() -> startsIn(log).size() == 2), "restart after the PTT change");
            assertTrue(startsIn(log).get(1).contains("-P RTS"), startsIn(log).get(1));
            assertTrue(DaemonManager.running("rig"));
        } finally {
            DaemonManager.stop("rig");
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
