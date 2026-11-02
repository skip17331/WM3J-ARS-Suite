package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RotorClientHeadingTest {

    private final RotorClient rotor = RotorClient.getInstance();

    @AfterEach
    void offline() { rotor.applyRemote(RotorClient.State.OFFLINE); }

    @Test
    void noRotorMeansNoHeading() {
        rotor.applyRemote(RotorClient.State.OFFLINE);
        assertTrue(Double.isNaN(rotor.headingDeg()));
    }

    @Test
    void aLiveRotorGivesANormalisedHeading() {
        rotor.applyRemote(new RotorClient.State(true, 410, 0));
        assertEquals(50, rotor.headingDeg(), 0.001);
        assertEquals("live", rotor.statusText());
    }
}
