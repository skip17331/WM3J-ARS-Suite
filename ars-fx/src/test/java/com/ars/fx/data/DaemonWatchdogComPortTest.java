package com.ars.fx.data;

import com.ars.fx.data.DaemonWatchdog.Action;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** The watchdog also watches Windows COM ports (listed in the registry), and never acts on a guess. */
class DaemonWatchdogComPortTest {
    private boolean oldWindows;
    private Supplier<Map<String, String>> oldLister;
    private String oldHome;

    @BeforeEach
    void save() {
        oldWindows = DaemonWatchdog.windows;
        oldLister = DaemonWatchdog.comLister;
        oldHome = System.getProperty("user.home");
        DaemonWatchdog.resetComCache();
    }

    @AfterEach
    void restore() {
        DaemonWatchdog.windows = oldWindows;
        DaemonWatchdog.comLister = oldLister;
        DaemonWatchdog.resetComCache();
        System.setProperty("user.home", oldHome);
        HubConfig.reload();
    }

    private static Map<String, String> ports(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put(pairs[i], pairs[i + 1]);
        return m;
    }

    @Test
    void parsesTheRegistryListing() {
        String out = "\r\nHKEY_LOCAL_MACHINE\\HARDWARE\\DEVICEMAP\\SERIALCOMM\r\n"
                + "    \\Device\\BthModem1    REG_SZ    COM3\r\n"
                + "    \\Device\\USBSER000    REG_SZ    com7\r\n"
                + "    \\Device\\ftvspc4294967294    REG_SZ    COM99\r\n\r\n";
        Map<String, String> m = DaemonWatchdog.parseSerialComm(out);
        assertEquals(3, m.size());
        assertEquals("\\Device\\BthModem1", m.get("COM3"));
        assertEquals("\\Device\\USBSER000", m.get("COM7"), "names are upper-cased");
        assertEquals("\\Device\\ftvspc4294967294", m.get("COM99"));
        assertTrue(DaemonWatchdog.parseSerialComm("").isEmpty());
        assertTrue(DaemonWatchdog.parseSerialComm(null).isEmpty());
        assertTrue(DaemonWatchdog.parseSerialComm("ERROR: The system was unable to find the specified registry key").isEmpty());
    }

    @Test
    void recognisesComNames() {
        assertEquals("COM3", DaemonWatchdog.comName("COM3"));
        assertEquals("COM3", DaemonWatchdog.comName("com3"));
        assertEquals("COM12", DaemonWatchdog.comName(" COM12 "));
        assertEquals("COM3", DaemonWatchdog.comName("\\\\.\\COM3"));
        assertNull(DaemonWatchdog.comName("/dev/ttyUSB0"));
        assertNull(DaemonWatchdog.comName("COM"));
        assertNull(DaemonWatchdog.comName("COM3:"));
        assertNull(DaemonWatchdog.comName("192.168.1.50"));
        assertNull(DaemonWatchdog.comName(""));
        assertNull(DaemonWatchdog.comName(null));
    }

    @Test
    void aComPortIsWatchedWhileThePortListIsReadable() {
        DaemonWatchdog.windows = true;
        DaemonWatchdog.comLister = () -> ports("COM3", "\\Device\\USBSER000");
        assertTrue(DaemonWatchdog.watchable("COM3"));
        assertTrue(DaemonWatchdog.watchable("com3"));
        assertTrue(DaemonWatchdog.watchable("\\\\.\\COM3"));
        assertEquals("COM3#\\Device\\USBSER000", DaemonWatchdog.deviceId("COM3"));
        assertEquals(DaemonWatchdog.deviceId("COM3"), DaemonWatchdog.deviceId("\\\\.\\com3"), "same port whatever the spelling");
        assertNull(DaemonWatchdog.deviceId("COM4"), "a port that is not listed is absent");
        assertTrue(DaemonWatchdog.watchable("COM4"), "and is still a port to watch: its daemon is stopped, then started when it returns");
        assertTrue(DaemonWatchdog.watchable("/dev/ttyUSB0"), "a /dev node is watched as before");
        assertFalse(DaemonWatchdog.watchable("192.168.1.50"));
        assertFalse(DaemonWatchdog.watchable(""));
        assertFalse(DaemonWatchdog.watchable(null));
    }

    @Test
    void anUnreadableListNeverStopsAnything() {
        DaemonWatchdog.windows = true;
        DaemonWatchdog.comLister = () -> null;                  // reg.exe missing, slow or failing
        assertFalse(DaemonWatchdog.watchable("COM3"), "not watched: only a dead daemon is started again, as before");
        assertNull(DaemonWatchdog.deviceId("COM3"));
    }

    @Test
    void aFailedReadKeepsTheLastGoodList() throws Exception {
        DaemonWatchdog.windows = true;
        int[] calls = {0};
        DaemonWatchdog.comLister = () -> calls[0]++ == 0 ? ports("COM3", "\\Device\\A") : null;
        assertEquals("COM3#\\Device\\A", DaemonWatchdog.deviceId("COM3"));
        Thread.sleep(2100);                                     // past the 2 s cache: the next read fails
        assertEquals("COM3#\\Device\\A", DaemonWatchdog.deviceId("COM3"), "one failed read is not a missing radio");
        assertTrue(DaemonWatchdog.watchable("COM3"));
        assertTrue(calls[0] >= 2, "it did read again");
    }

    @Test
    void anEmptyListMeansEveryPortIsGone() {
        DaemonWatchdog.windows = true;
        DaemonWatchdog.comLister = Map::of;                     // the key is absent: no serial ports at all
        assertTrue(DaemonWatchdog.watchable("COM3"));
        assertNull(DaemonWatchdog.deviceId("COM3"));
        assertEquals(Action.STOP, DaemonWatchdog.decide(true, true, DaemonWatchdog.deviceId("COM3"), "COM3#\\Device\\A", 60_000, 0));
    }

    @Test
    void comPortsAreNotWatchedOffWindows() {
        DaemonWatchdog.windows = false;
        DaemonWatchdog.comLister = () -> ports("COM3", "\\Device\\A");
        assertFalse(DaemonWatchdog.watchable("COM3"));
        assertNull(DaemonWatchdog.deviceId("COM3"));
    }

    @Test
    void theSettingTurnsComWatchingOff() throws Exception {
        DaemonWatchdog.windows = true;
        DaemonWatchdog.comLister = () -> ports("COM3", "\\Device\\A");
        System.setProperty("user.home", Files.createTempDirectory("wd-com").toString());   // never the user's real config
        HubConfig.reload();
        assertTrue(DaemonWatchdog.watchable("COM3"), "on by default");
        HubConfig.setBool("daemon.watchComPorts", false);
        assertFalse(DaemonWatchdog.watchable("COM3"));
    }

    @Test
    void windowsReadsItsRealPortList() {
        Assumptions.assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"), "reads the Windows registry");
        Map<String, String> m = DaemonWatchdog.queryComPorts();
        assertNotNull(m, "reg.exe answered");
        for (Map.Entry<String, String> e : m.entrySet()) {
            assertTrue(e.getKey().matches("COM[0-9]+"), e.getKey());
            assertFalse(e.getValue().isBlank());
        }
    }
}
