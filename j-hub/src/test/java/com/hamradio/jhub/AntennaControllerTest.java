package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig.AntennaRule;
import com.hamradio.jhub.model.JHubConfig.AntennaSection;
import com.hamradio.jhub.model.JHubConfig.AntennaSwitch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-Java tests for the AntennaController rule evaluator. The serial-port
 * side and the WebSocket broadcast are skipped — those live behind a real
 * SerialPort instance that we don't want to open in CI.
 */
class AntennaControllerTest {

    private AntennaController ctl;
    private AntennaSection cfg;

    @BeforeEach
    void setUp() {
        ctl = AntennaController.getInstance();
        cfg = new AntennaSection();
        AntennaSwitch sw = new AntennaSwitch();
        sw.id = "main"; sw.name = "Main"; sw.antennaCount = 4;
        cfg.switches.add(sw);
        ctl.__setConfigForTest(cfg);
    }

    @Test
    void simpleBandRuleMatches() {
        cfg.rules.add(rule("20m", "", -1, -1, "main", 2));
        ctl.__setStateForTest("20m", "CW", -1);
        AntennaRule r = ctl.matchRule("main");
        assertNotNull(r);
        assertEquals(2, r.antenna);
    }

    @Test
    void noMatchReturnsNull() {
        cfg.rules.add(rule("20m", "", -1, -1, "main", 2));
        ctl.__setStateForTest("40m", "CW", -1);
        assertNull(ctl.matchRule("main"));
    }

    @Test
    void modeSpecificRuleWinsOverBandOnlyWhenListedFirst() {
        // Operators put more-specific rules first; first match wins.
        cfg.rules.add(rule("20m", "CW",  -1, -1, "main", 3));
        cfg.rules.add(rule("20m", "",    -1, -1, "main", 2));
        ctl.__setStateForTest("20m", "CW", -1);
        assertEquals(3, ctl.matchRule("main").antenna);
        ctl.__setStateForTest("20m", "SSB", -1);
        assertEquals(2, ctl.matchRule("main").antenna);
    }

    @Test
    void headingRuleNeedsHeadingPresent() {
        cfg.rules.add(rule("20m", "", 0, 90, "main", 3));
        ctl.__setStateForTest("20m", "CW", -1);   // no rotor heading yet
        assertNull(ctl.matchRule("main"), "heading rule must not fire without a heading");
    }

    @Test
    void headingInRangeMatches() {
        cfg.rules.add(rule("20m", "", 0, 90, "main", 3));
        ctl.__setStateForTest("20m", "CW", 45);
        assertEquals(3, ctl.matchRule("main").antenna);
    }

    @Test
    void headingOutOfRangeDoesNotMatch() {
        cfg.rules.add(rule("20m", "", 0, 90, "main", 3));
        ctl.__setStateForTest("20m", "CW", 180);
        assertNull(ctl.matchRule("main"));
    }

    @Test
    void headingWindowWrapsAroundNorth() {
        // 350°–10° spans north — bearings of 5 and 355 should both match.
        assertTrue (AntennaController.headingInRange(  5, 350, 10));
        assertTrue (AntennaController.headingInRange(355, 350, 10));
        assertTrue (AntennaController.headingInRange(  0, 350, 10));
        assertFalse(AntennaController.headingInRange( 90, 350, 10));
        assertFalse(AntennaController.headingInRange(180, 350, 10));
    }

    @Test
    void headingNormalizesToZeroToThreeSixty() {
        // Negative or >360 inputs should be normalized — protects against
        // rotors that report -180..180 instead of 0..360.
        assertTrue(AntennaController.headingInRange(-5, 350, 10));
        assertTrue(AntennaController.headingInRange(365, 0, 10));
    }

    @Test
    void rulesAreScopedPerSwitch() {
        AntennaSwitch sw2 = new AntennaSwitch();
        sw2.id = "aux"; sw2.name = "Aux"; sw2.antennaCount = 2;
        cfg.switches.add(sw2);
        cfg.rules.add(rule("20m", "", -1, -1, "main", 2));
        cfg.rules.add(rule("20m", "", -1, -1, "aux",  1));
        ctl.__setStateForTest("20m", "CW", -1);
        assertEquals(2, ctl.matchRule("main").antenna);
        assertEquals(1, ctl.matchRule("aux") .antenna);
    }

    @Test
    void emptyBandIsTreatedAsAny() {
        // A wildcard rule (no band specified) should match any band.
        cfg.rules.add(rule("", "", -1, -1, "main", 4));
        ctl.__setStateForTest("40m", "SSB", -1);
        assertEquals(4, ctl.matchRule("main").antenna);
    }

    private static AntennaRule rule(String band, String mode,
                                    double hdgMin, double hdgMax,
                                    String switchId, int antenna) {
        AntennaRule r = new AntennaRule();
        r.band = band; r.mode = mode;
        r.headingMin = hdgMin; r.headingMax = hdgMax;
        r.switchId = switchId; r.antenna = antenna;
        return r;
    }
}
