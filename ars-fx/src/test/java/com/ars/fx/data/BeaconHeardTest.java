package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BeaconHeardTest {
    String oldHome;
    @BeforeEach void home(@TempDir Path tmp) { oldHome = System.getProperty("user.home"); System.setProperty("user.home", tmp.toString()); HubConfig.reload(); BeaconHeard.forgetForTest(); }
    @AfterEach void restore() { BeaconHeard.forgetForTest(); System.setProperty("user.home", oldHome); HubConfig.reload(); }

    static final long T = 1_794_000_000L;                      // some moment in November 2026

    @Test void aBeaconIsMarkedPerBandAndCounted() {
        BeaconHeard.mark("4u1un", "20m", T);
        BeaconHeard.mark("VE8AT", "20m", T + 10);
        BeaconHeard.mark("4U1UN", "15m", T + 20);
        assertTrue(BeaconHeard.isHeard("4U1UN", "20m"));
        assertTrue(BeaconHeard.isHeard("4u1un", "20M"), "case does not matter");
        assertFalse(BeaconHeard.isHeard("W6WX", "20m"));
        assertEquals(2, BeaconHeard.count("20m"));
        assertEquals(1, BeaconHeard.count("15m"));
        assertEquals(0, BeaconHeard.count("10m"));
        assertEquals("2 of 18 heard on 20m", BeaconHeard.summary("20m", 18));
        assertEquals("20m 2/18  \u00b7  17m 0/18  \u00b7  15m 1/18", BeaconHeard.allBands(List.of("20m", "17m", "15m"), 18));
    }

    @Test void clickingTwiceTakesTheMarkBackAndNothingBlankIsMarked() {
        assertTrue(BeaconHeard.toggle("W6WX", "20m", T));
        assertFalse(BeaconHeard.toggle("W6WX", "20m", T + 5));
        assertFalse(BeaconHeard.isHeard("W6WX", "20m"));
        assertFalse(BeaconHeard.unmark("W6WX", "20m"), "nothing left to take back");
        BeaconHeard.mark("", "20m", T); BeaconHeard.mark("W6WX", " ", T); BeaconHeard.mark(null, "20m", T);
        assertEquals(0, BeaconHeard.count("20m"));
    }

    @Test void theLogIsCsvOldestFirstAndASecondMarkMovesTheTimeForwardOnly() {
        BeaconHeard.mark("KH6RS", "20m", T + 100);
        BeaconHeard.mark("4U1UN", "20m", T);
        BeaconHeard.mark("KH6RS", "20m", T + 200);              // heard again later
        BeaconHeard.mark("4U1UN", "20m", T - 500);              // an older mark does not pull it back
        String[] lines = BeaconHeard.csv().split("\n");
        assertEquals("call,band,utc", lines[0]);
        assertEquals(3, lines.length);
        assertTrue(lines[1].startsWith("4U1UN,20m,2026-"), lines[1]);
        assertTrue(lines[2].startsWith("KH6RS,20m,2026-"), lines[2]);
        assertTrue(lines[1].matches("4U1UN,20m,\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d"), lines[1]);
        assertNotEquals(lines[1].substring(lines[1].length() - 8), lines[2].substring(lines[2].length() - 8));
    }

    @Test void theMarksSurviveARestartAndAClear() {
        BeaconHeard.mark("OH2B", "17m", T); BeaconHeard.mark("OH2B", "10m", T + 1);
        BeaconHeard.forgetForTest();
        assertTrue(BeaconHeard.isHeard("OH2B", "17m"), "read back from the settings");
        assertEquals(1, BeaconHeard.count("10m"));
        BeaconHeard.clear();
        BeaconHeard.forgetForTest();
        assertEquals(0, BeaconHeard.count("17m"), "and the clear is kept too");
    }

    @Test void aBadSavedEntryIsSkipped() {
        BeaconHeard.decode("OH2B@17m=1794000000;broken;X@y=abc;@20m=5;CS3B@15m=1794000100");
        assertEquals(1, BeaconHeard.count("17m"));
        assertEquals(1, BeaconHeard.count("15m"));
        assertEquals("CS3B@15m=1794000100;OH2B@17m=1794000000".split(";").length, BeaconHeard.encode().split(";").length);
    }

    // ---- by the S-meter ----------------------------------------------------------------------------------------------
    @Test void aSteadySignalMarksTheBeaconOnTheThirdReading() {
        assertFalse(BeaconHeard.sample("W6WX", "20m", -30, 3, T));
        assertFalse(BeaconHeard.sample("W6WX", "20m", -30, 4, T + 1));
        assertTrue(BeaconHeard.sample("W6WX", "20m", -30, 5, T + 2), "the third in a row");
        assertTrue(BeaconHeard.isHeard("W6WX", "20m"));
        assertFalse(BeaconHeard.sample("W6WX", "20m", -30, 6, T + 3), "already marked: not again");
    }

    @Test void noiseGapsAndTheEdgesOfASlotDoNotMark() {
        BeaconHeard.sample("ZL6B", "20m", -30, 3, T); BeaconHeard.sample("ZL6B", "20m", -30, 4, T + 1);
        assertFalse(BeaconHeard.sample("ZL6B", "20m", -60, 5, T + 2), "a dropout resets the count");
        assertFalse(BeaconHeard.sample("ZL6B", "20m", -30, 6, T + 3));
        assertFalse(BeaconHeard.sample("ZL6B", "20m", -30, 7, T + 4));
        assertFalse(BeaconHeard.isHeard("ZL6B", "20m"), "two readings after the dropout are not enough");
        assertFalse(BeaconHeard.sample("VK6RBP", "20m", Integer.MIN_VALUE, 3, T), "a rig that does not report an S-meter");
        assertFalse(BeaconHeard.sample("VK6RBP", "20m", -30, 0, T));
        assertFalse(BeaconHeard.sample("VK6RBP", "20m", -30, 0, T));
        assertFalse(BeaconHeard.sample("VK6RBP", "20m", -30, 0, T));
        assertFalse(BeaconHeard.isHeard("VK6RBP", "20m"), "the first second of a slot is when the previous beacon is still dying away");
        BeaconHeard.sample("JA2IGY", "20m", -30, 5, T); BeaconHeard.sample("JA2IGY", "20m", -30, 6, T + 1);
        assertFalse(BeaconHeard.sample("RR9O", "20m", -30, 7, T + 2), "a different beacon starts the count again");
        assertFalse(BeaconHeard.isHeard("JA2IGY", "20m"));
    }

    // ---- hopping -------------------------------------------------------------------------------------------------------
    @Test void aHoppingWatchMovesToTheNextBandEachCycle() {
        assertEquals(0, BeaconHeard.bandAt(0, 5));
        assertEquals(0, BeaconHeard.bandAt(179, 5));
        assertEquals(1, BeaconHeard.bandAt(180, 5));
        assertEquals(4, BeaconHeard.bandAt(4 * 180 + 90, 5));
        assertEquals(0, BeaconHeard.bandAt(5 * 180, 5), "round to the first again");
        assertEquals(0, BeaconHeard.bandAt(T, 0), "no bands: the first");
        int seen = 0, last = -1;
        for (long t = 0; t < 15 * 60; t += 30) { int b = BeaconHeard.bandAt(t, 5); if (b != last) { seen++; last = b; } }
        assertEquals(5, seen, "all five bands in fifteen minutes");
    }
}
