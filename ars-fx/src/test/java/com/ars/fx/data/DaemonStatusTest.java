package com.ars.fx.data;

import com.ars.fx.data.DaemonStatus.Line;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The status line under each "Start ... for me" switch. */
class DaemonStatusTest {
    @Test
    void switchedOffIsNotAProblem() {
        Line l = DaemonStatus.describe(false, false, "", "");
        assertEquals("off", l.level());
        assertTrue(l.text().contains("switch is off"));
    }

    @Test
    void runningIsOkAndShowsTheLastRepair() {
        assertEquals(new Line("ok", "● Running"), DaemonStatus.describe(true, true, "ok", ""));
        Line l = DaemonStatus.describe(true, true, "ok", "rig daemon: device back: started the daemon");
        assertEquals("ok", l.level());
        assertTrue(l.text().endsWith("last: device back: started the daemon"), l.text());
        assertFalse(l.text().contains("rig daemon:"), "the kind prefix is dropped");
    }

    @Test
    void theLastRepairSaysWhenItHappened() {
        Line l = DaemonStatus.describe(true, true, "ok", "rig daemon: device back: started the daemon", 4 * 60_000L + 5_000);
        assertTrue(l.text().endsWith("last (4 min ago): device back: started the daemon"), l.text());
        assertTrue(DaemonStatus.describe(true, true, "ok", "rig daemon: x", -1).text().endsWith("last: x"), "no time known: no brackets");
        assertEquals("just now", DaemonStatus.agoText(20_000));
        assertEquals("2 h ago", DaemonStatus.agoText(120 * 60_000L));
        assertEquals("2 h 5 min ago", DaemonStatus.agoText(125 * 60_000L));
        assertEquals("3 d ago", DaemonStatus.agoText(3 * 24 * 3_600_000L + 1));
        assertEquals("", DaemonStatus.agoText(-1));
    }

    @Test
    void unreadableSerialPortSaysWhyAndHowToFixIt() {
        Line l = DaemonStatus.describe(true, false, "noaccess", "");
        assertEquals("warn", l.level());
        assertTrue(l.text().contains("dialout"), l.text());
        assertEquals("warn", DaemonStatus.describe(true, true, "noaccess", "").level(), "even if a daemon is up, no access is flagged");
    }

    @Test
    void noAccessIsFalseForMissingDevicesAndNonPaths() {
        assertFalse(DaemonWatchdog.noAccess("/dev/does-not-exist"));
        assertFalse(DaemonWatchdog.noAccess("COM3"));
        assertFalse(DaemonWatchdog.noAccess(null));
    }

    @Test
    void wantedButNotRunningIsAWarning() {
        Line gone = DaemonStatus.describe(true, false, "gone", "rig daemon: device COM3 is gone: stopped the daemon, waiting for it to come back");
        assertEquals("warn", gone.level());
        assertTrue(gone.text().contains("Waiting for the device"), gone.text());
        assertTrue(gone.text().contains("last: device COM3 is gone"), gone.text());

        Line dead = DaemonStatus.describe(true, false, "ok", "");
        assertEquals("warn", dead.level());
        assertEquals("○ Not running", dead.text());
        assertEquals("warn", DaemonStatus.describe(true, false, "", null).level(), "no device news and no last action");
    }

    @Test
    void snapshotCoversEveryDaemonAndRoundTrips() {
        JsonObject s = DaemonStatus.snapshot();
        for (String k : new String[]{"rig", "rotor", "rotor2", "amp"}) {
            assertTrue(s.has(k), k);
            JsonObject d = s.getAsJsonObject(k);
            assertTrue(d.has("wanted") && d.has("running") && d.has("device") && d.has("last"), k);
        }
        DaemonStatus.apply(s);          // what a client does with the hub's message; must not throw
    }
}
