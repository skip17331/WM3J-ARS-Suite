package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelFadeTest {
    @Test void aQuietMapKeepsNamesLongAndABusyOneShortens() {
        assertEquals(480, LabelFade.lifeSec(0, 20), 1e-9);
        assertEquals(480, LabelFade.lifeSec(10, 20), 1e-9);
        assertEquals(75, LabelFade.lifeSec(80, 20), 1e-9);
        assertEquals(75, LabelFade.lifeSec(500, 20), 1e-9);
    }

    @Test void lifetimeFallsSmoothlyAsTheMapFillsUp() {
        double prev = Double.MAX_VALUE;
        for (int n = 10; n <= 80; n += 5) {
            double life = LabelFade.lifeSec(n, 20);
            assertTrue(life <= prev, "never longer with more spots: n=" + n);
            prev = life;
        }
        // evenly on a log scale: the geometric middle of 10 and 80 (about 28 spots) lands on the geometric middle of 480 and 75
        assertEquals(Math.sqrt(480 * 75.0), LabelFade.lifeSec((int) Math.round(Math.sqrt(10 * 80.0)), 20), 4.0);
    }

    @Test void aNameNeverOutlivesItsSpot() {
        assertEquals(180, LabelFade.lifeSec(3, 3), 1e-9);     // spots only stay 3 minutes: names go by then
    }

    @Test void aNameIsFullForHalfItsLifeThenFades() {
        assertEquals(1.0, LabelFade.alpha(0, 100), 1e-9);
        assertEquals(1.0, LabelFade.alpha(50, 100), 1e-9);
        assertEquals(0.5, LabelFade.alpha(75, 100), 1e-9);
        assertEquals(0.0, LabelFade.alpha(100, 100), 1e-9);
        assertEquals(0.0, LabelFade.alpha(400, 100), 1e-9);
    }
}
