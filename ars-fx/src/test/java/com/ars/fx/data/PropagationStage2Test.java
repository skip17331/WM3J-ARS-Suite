package com.ars.fx.data;

import com.ars.fx.data.Propagation.Point;
import com.ars.fx.data.Propagation.Verdict;
import com.ars.fx.data.Propagation.Weather;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Propagation stage 2 (punch list 5-5): the winter and equatorial anomalies, and the saved observations a model change can be replayed
 * against. The first check (November 2026, about 400 open paths) found 96% of 10 m paths over 1,000 km called closed.
 */
class PropagationStage2Test {
    static final Weather ACTIVE = new Weather(160, 120, 1), QUIET = new Weather(70, 10, 1);
    static final int DEC_21 = 355, JUN_21 = 172, MAR_21 = 80;
    static final Point BOSTON = new Point(42.36, -71.06), HOUSTON = new Point(29.76, -95.37), NYC = new Point(40.71, -74.01), LONDON = new Point(51.51, -0.13);

    String oldHome;
    @BeforeEach void home(@TempDir Path tmp) { oldHome = System.getProperty("user.home"); System.setProperty("user.home", tmp.toString()); HubConfig.reload(); PropagationCheck.forgetForTest(); }
    @AfterEach void restore() { PropagationCheck.forgetForTest(); System.setProperty("user.home", oldHome); HubConfig.reload(); }

    @Test void winterAnomalyRaisesMidLatitudeDaytimeFoF2InLocalWinter() {
        double dec = Propagation.foF2(0.5, 45, ACTIVE, DEC_21), mar = Propagation.foF2(0.5, 45, ACTIVE, MAR_21), jun = Propagation.foF2(0.5, 45, ACTIVE, JUN_21);
        assertTrue(dec > mar * 1.15 && mar > jun * 1.15, "December > March > June at 45N: " + dec + " " + mar + " " + jun);
        assertTrue(Propagation.foF2(0.5, -45, ACTIVE, JUN_21) > Propagation.foF2(0.5, -45, ACTIVE, DEC_21), "reversed in the south");
        assertEquals(Propagation.foF2(-0.5, 45, ACTIVE), Propagation.foF2(-0.5, 45, ACTIVE, DEC_21), 1e-9, "not at night");
        assertEquals(Propagation.foF2(0.5, 45, QUIET), Propagation.foF2(0.5, 45, QUIET, DEC_21), 1e-9, "not with a quiet sun");
        assertEquals(1.0, Propagation.winterAnomaly(0.5, 80, DEC_21, 160), 1e-9, "not near the pole");
    }

    @Test void equatorialAnomalyRaisesTheCrestsByDayAndEvening() {
        assertTrue(Propagation.equatorialAnomaly(0.5, 15, 160) > 1.2, "a crest by day");
        assertTrue(Propagation.equatorialAnomaly(-0.1, -15, 160) > 1.1, "into the evening");
        assertEquals(1.0, Propagation.equatorialAnomaly(-0.8, 15, 160), 1e-9, "not in the small hours");
        assertEquals(1.0, Propagation.equatorialAnomaly(0.5, 45, 160), 1e-9, "not at mid-latitude");
        assertEquals(1.0, Propagation.equatorialAnomaly(0.5, 15, 70), 1e-9, "not with a quiet sun");
    }

    @Test void tenMetresAtWinterMiddayWithAnActiveSunIsNoLongerClosed() {
        // Boston - Houston (2,600 km), 18:30 UTC (midday on the path), 21 December, flux 160
        LocalDateTime t = LocalDateTime.of(2026, 12, 21, 18, 30);
        Verdict v = Propagation.verdictNow(BOSTON, HOUSTON, ACTIVE, t, 28.074);
        assertNotEquals(Verdict.CLOSED, v, "stage 1 called this marginal at best; a winter noon on 10 m at flux 160 is usually open");
    }

    @Test void observationsAreSavedAndCanBeReplayed() {
        LocalDateTime noon = LocalDateTime.of(2026, 11, 20, 13, 0);
        PropagationCheck.observe(NYC, LONDON, 14_025_000L, noon, ACTIVE);
        PropagationCheck.observe(NYC, LONDON, 28_074_000L, noon, ACTIVE);
        PropagationCheck.observe(NYC, LONDON, 7_025_000L, noon.withHour(3), ACTIVE);
        PropagationCheck.flush();
        List<PropagationCheck.Obs> obs = PropagationCheck.observations();
        assertEquals(3, obs.size());
        assertEquals(28.074, obs.get(1).fMhz(), 1e-6);
        Map<String, PropagationCheck.Tally> replay = PropagationCheck.replay(obs);
        assertEquals(PropagationCheck.byBand(), replay, "the same model gives the same verdicts on replay");
    }

    @Test void theNewModelHasItsOwnTally() {
        assertEquals("propcheck.tally.v2", PropagationCheck.KEY, "stage 1's tally stays under propcheck.tally for comparison");
    }
}
