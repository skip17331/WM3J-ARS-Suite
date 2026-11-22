package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SpotCommentTest {
    @Test
    void sweepstakesSpotsCarryTheSection() {
        assertEquals("SS ENY", SpotComment.of("ARRL_SS_CW", " eny "));
        assertEquals("SS ENY", SpotComment.of("ARRL_SWEEPSTAKES_SSB", "ENY"));
    }

    @Test
    void anUnknownContestGivesJustTheMultiplier() {
        assertEquals("WA", SpotComment.of("WA_QSO_PARTY", "WA"));
        assertEquals("", SpotComment.of("ARRL_SS_CW", null));
    }
}
