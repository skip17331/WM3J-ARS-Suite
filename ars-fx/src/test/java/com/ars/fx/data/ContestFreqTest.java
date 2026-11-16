package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Contest QSOs store their frequency in kHz (Cabrillo's unit), and only when it agrees with the band on the form. */
class ContestFreqTest {
    @Test
    void rigOnTheFormsBandGivesKilohertz() {
        assertEquals("14025", ContestFreq.khz(14_025_000L, true, "20m"));
        assertEquals("7040",  ContestFreq.khz(7_040_000L, true, "40m"));
        assertEquals("14025", ContestFreq.khz(14_025_000L, true, "20"), "20 and 20m are the same band");
        assertEquals("14025", ContestFreq.khz(14_025_000L, true, " 20M "));
        assertEquals("14026", ContestFreq.khz(14_025_600L, true, "20m"), "rounded to the nearest kHz");
        assertEquals("144200", ContestFreq.khz(144_200_000L, true, "2m"));
    }

    @Test
    void nothingIsStoredOnAGuess() {
        assertNull(ContestFreq.khz(14_025_000L, false, "20m"), "rig not connected");
        assertNull(ContestFreq.khz(0L, true, "20m"), "no reading");
        assertNull(ContestFreq.khz(-1L, true, "20m"));
        assertNull(ContestFreq.khz(14_025_000L, true, "40m"), "the rig is on 20m but the form says 40m: the operator's band wins");
        assertNull(ContestFreq.khz(14_025_000L, true, ""), "no band on the form");
        assertNull(ContestFreq.khz(14_025_000L, true, null));
        assertNull(ContestFreq.khz(1_000_000L, true, "20m"), "outside every amateur band");
    }

    @Test
    void labelsShowMegahertz() {
        assertEquals("14.025", ContestFreq.toMhz("14025"));
        assertEquals("144.200", ContestFreq.toMhz("144200"));
        assertEquals("14.074", ContestFreq.toMhz("14.074"), "a value already in MHz is left alone");
        assertEquals("14.025", ContestFreq.toMhz(" 14025 "));
        assertEquals("", ContestFreq.toMhz(""));
        assertNull(ContestFreq.toMhz(null));
        assertEquals("abc", ContestFreq.toMhz("abc"), "unreadable text is passed through, not lost");
    }
}
