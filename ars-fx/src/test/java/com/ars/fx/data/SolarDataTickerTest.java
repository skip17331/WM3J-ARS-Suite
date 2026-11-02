package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SolarDataTickerTest {

    private static SolarData.Snapshot snap(String muf) {
        return new SolarData.Snapshot("94", "7", "1", "60", "B1", "2", "380", "1.2", "today", List.of(), muf, "5.0");
    }

    @Test
    void showsTheLiveFigures() {
        String t = SolarData.tickerText(snap("21.3"), "SR 12:02 · SS 22:14");
        assertTrue(t.contains("SFI 94 · SN 60 · A 7 · K 1 · X-ray B1"), t);
        assertTrue(t.contains("Aurora 2"), t);
        assertTrue(t.contains("Solar wind 380 km/s"), t);
        assertTrue(t.contains("Gray line: SR 12:02 · SS 22:14"), t);
        assertTrue(t.contains("MUF 21.3 MHz"), t);
    }

    @Test
    void leavesOutAnIonosondeThatIsNotReporting() {
        assertFalse(SolarData.tickerText(snap("NoRpt"), "SR 12:02").contains("MUF"));
    }

    @Test
    void saysSoWhileTheDataIsLoading() {
        assertEquals("   Space weather loading…   ●   Gray line: SR 12:02   ●", SolarData.tickerText(null, "SR 12:02"));
    }
}
