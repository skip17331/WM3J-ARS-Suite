package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.Propagation.Point;
import com.ars.fx.data.Propagation.Verdict;
import com.ars.fx.data.Propagation.Weather;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PropagationCheckTest {
    static final Point NYC = new Point(40.71, -74.01), LONDON = new Point(51.51, -0.13), BOSTON = new Point(42.36, -71.06);
    static final Weather HIGH = new Weather(200, 150, 1), LOW = new Weather(70, 10, 1);
    static final LocalDateTime NOON_UTC = LocalDateTime.of(2026, 3, 21, 12, 30), NIGHT_UTC = LocalDateTime.of(2026, 3, 21, 3, 30);
    String oldHome;

    @BeforeEach void home(@TempDir Path tmp) { oldHome = System.getProperty("user.home"); System.setProperty("user.home", tmp.toString()); HubConfig.reload(); PropagationCheck.forgetForTest(); }
    @AfterEach void restore() { PropagationCheck.forgetForTest(); System.setProperty("user.home", oldHome); HubConfig.reload(); }

    @Test void aHeardPathIsAskedAboutAndTalliedPerBand() {
        Verdict open = PropagationCheck.observe(NYC, LONDON, 14_025_000L, NOON_UTC, HIGH);
        assertNotNull(open);
        Verdict miss = PropagationCheck.observe(NYC, LONDON, 28_500_000L, NIGHT_UTC, LOW);       // 10 m at night at the bottom of the cycle: the estimate says closed
        assertEquals(Verdict.CLOSED, miss, "a heard path the estimate calls closed is a miss");
        assertEquals(2, PropagationCheck.total().n());
        assertEquals(1, PropagationCheck.byBand().get("20m").n());
        assertEquals(1, PropagationCheck.byBand().get("10m").closed());
    }

    @Test void shortPathsAndMissingDataAreNotCounted() {
        assertNull(PropagationCheck.observe(NYC, BOSTON, 14_025_000L, NOON_UTC, HIGH), "300 km is ground wave");
        assertNull(PropagationCheck.observe(NYC, LONDON, 14_025_000L, NOON_UTC, null));
        assertNull(PropagationCheck.observe(null, LONDON, 14_025_000L, NOON_UTC, HIGH));
        assertNull(PropagationCheck.observe(NYC, LONDON, 0, NOON_UTC, HIGH));
        assertNull(PropagationCheck.observe(NYC, LONDON, 300_000_000L, NOON_UTC, HIGH), "not on an amateur band");
        assertEquals(0, PropagationCheck.total().n());
    }

    @Test void theSummaryGivesPercentagesAndAnEmptyOneSaysWhatItIsWaitingFor() {
        assertTrue(PropagationCheck.summary().contains("waiting for DX cluster spots"));
        for (int i = 0; i < 3; i++) PropagationCheck.observe(NYC, LONDON, 14_025_000L, NOON_UTC, HIGH);
        PropagationCheck.observe(NYC, LONDON, 28_500_000L, NIGHT_UTC, LOW);
        String s = PropagationCheck.summary();
        assertTrue(s.contains("4 heard paths"), s);
        assertTrue(s.matches(".*\\d+% rated likely open, \\d+% marginal, \\d+% probably closed.*"), s);
    }

    @Test void theTalliesSurviveARestartAndCanBeReset() {
        for (int i = 0; i < 25; i++) PropagationCheck.observe(NYC, LONDON, 14_025_000L, NOON_UTC, HIGH);      // the 25th count saves
        assertEquals(25, PropagationCheck.total().n());
        PropagationCheck.forgetForTest();                                                                       // a new session
        assertEquals(25, PropagationCheck.total().n(), "read back from the config");
        PropagationCheck.reset();
        assertEquals(0, PropagationCheck.total().n());
        PropagationCheck.forgetForTest();
        assertEquals(0, PropagationCheck.total().n(), "and the reset is kept too");
    }

    @Test void theEncodingRoundTripsAndABadEntryIsSkipped() {
        PropagationCheck.decode("20m:3,2,1;10m:x,y,z;bad;40m:5,0,0");
        assertEquals(6, PropagationCheck.byBand().get("20m").n());
        assertEquals(5, PropagationCheck.byBand().get("40m").open());
        assertNull(PropagationCheck.byBand().get("10m"));
        assertEquals("20m:3,2,1;40m:5,0,0", PropagationCheck.encode());
    }

    @Test void theWeakestBandsNeedEnoughPathsAndAreOrderedByMisses() {
        assertTrue(PropagationCheck.weakest().startsWith("Not enough"));
        PropagationCheck.decode("20m:50,5,5;10m:10,5,25;15m:3,1,1");
        String w = PropagationCheck.weakest();
        assertTrue(w.startsWith("Weakest bands: 10m: 63% misses of 40"), w);
        assertFalse(w.contains("15m"), "five paths is too few to judge a band");
    }

    @Test void aSpotterCallLosesItsSkimmerSuffix() {
        assertEquals("K2PO/7", PropagationCheck.spotterBase("K2PO/7-#"));
        assertEquals("W1ABC", PropagationCheck.spotterBase("W1ABC-2"));
        assertEquals("", PropagationCheck.spotterBase(null));
    }
}
