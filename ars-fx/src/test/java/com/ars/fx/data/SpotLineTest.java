package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** "DX de" lines as different cluster software and the RBN send them. Four of these used to be dropped. */
class SpotLineTest {

    @Test void plainSpot() {
        SpotLine.Parsed p = SpotLine.parse("DX de K1TTT:     14025.1  3Y0J         CW 599 up 2                    1230Z");
        assertNotNull(p);
        assertEquals("K1TTT", p.spotter());
        assertEquals(14_025_100L, p.freqHz());
        assertEquals("3Y0J", p.call());
        assertEquals("CW 599 up 2", p.comment());
    }

    @Test void theSpottersGridOrStateAfterTheTime() {
        SpotLine.Parsed grid = SpotLine.parse("DX de W3LPL:     14025.0  3Y0J         CW 599                         1230Z FN20");
        assertNotNull(grid, "CC Cluster / AR-Cluster with the spotter's locator shown: was dropped");
        assertEquals("CW 599", grid.comment());
        assertNotNull(SpotLine.parse("DX de K1TTT:     14025.0  3Y0J         CW 599                         1230Z NY"), "state: was dropped");
        assertNotNull(SpotLine.parse("DX de K1TTT:     14025.0  3Y0J         CW 599                         1230Z ENY"), "section");
    }

    @Test void bellAndOtherControlCharacters() {
        SpotLine.Parsed p = SpotLine.parse("DX de DL1ABC:    7074.0  JA1XYZ       FT8 -12 dB                     0815Z\u0007\u0007");
        assertNotNull(p, "DXSpider's bell: was dropped");
        assertEquals("FT8 -12 dB", p.comment());
    }

    @Test void noCommentAtAll() {
        SpotLine.Parsed p = SpotLine.parse("DX de K1TTT:     14025.0  3Y0J 1230Z");
        assertNotNull(p, "was dropped");
        assertEquals("", p.comment());
        assertEquals("3Y0J", SpotLine.parse("DX de K1TTT:     14025.0  3Y0J      1230Z").call());
    }

    @Test void aNumberAtTheEndOfTheCommentIsNotTheTime() {
        SpotLine.Parsed p = SpotLine.parse("DX de K1TTT:  14025.0  3Y0J   CW QSX 1500        1230Z");
        assertEquals("CW QSX 1500", p.comment());
    }

    @Test void rbnSkimmerLines() {
        SpotLine.Parsed p = SpotLine.parse("DX de W3OA-#:    14021.9  K1ABC          CW    19 dB  25 WPM  CQ      1231Z");
        assertNotNull(p);
        assertEquals("W3OA-#", p.spotter());
        assertEquals(14_021_900L, p.freqHz(), "rounded, not truncated");
    }

    @Test void notSpots() {
        assertNull(SpotLine.parse("Hello K1ABC, this is DXSpider"));
        assertNull(SpotLine.parse("WWV de W0MU <18>:   SFI=150, A=5, K=1"));
        assertNull(SpotLine.parse(null));
        assertNull(SpotLine.parse("DX de K1TTT:  abc  3Y0J  1230Z"));
    }

    @Test void theClusterClientUsesIt() {
        ClusterClient.Spot s = ClusterClient.parse("DX de W3LPL:     14025.0  3Y0J         CW 599     1230Z FN20");
        assertNotNull(s);
        assertEquals("20m", s.band());
        assertEquals("CW", s.mode());
    }
}
