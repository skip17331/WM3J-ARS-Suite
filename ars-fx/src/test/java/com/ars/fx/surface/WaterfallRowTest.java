package com.ars.fx.surface;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** J-Digi's waterfall row: the image scrolls up one row and the new spectrum fills the bottom row (punch list 4a-4). */
class WaterfallRowTest {

    @Test void scrollsUpAndPaintsTheBottomRow() {
        int w = 4, h = 3;
        int[] p = new int[w * h];
        for (int i = 0; i < p.length; i++) p[i] = 0xff000000 | i;          // each pixel tagged with its index
        JDigiView.waterfallRow(p, w, h, new double[]{1e-9, 1.0});           // quiet left half, loud right half
        for (int i = 0; i < w * (h - 1); i++) assertEquals(0xff000000 | (i + w), p[i], "pixel " + i + " moved up a row");
        int quiet = JDigiView.waterfallColor(1e-9), loud = JDigiView.waterfallColor(1.0);
        assertArrayEquals(new int[]{quiet, quiet, loud, loud}, java.util.Arrays.copyOfRange(p, w * (h - 1), w * h));
    }

    @Test void coloursAreOpaqueFromQuietToLoud() {
        int floor = JDigiView.waterfallColor(0), top = JDigiView.waterfallColor(1.0);
        assertEquals(0xff, floor >>> 24, "opaque: the PixelBuffer is premultiplied, so only opaque colours keep their values");
        assertEquals(0xff, top >>> 24);
        assertEquals(0xff00003c, floor, "-90 dB and below: dark blue");
        assertEquals(0xffffff00, top, "-30 dB and above: yellow");
        assertNotEquals(floor, JDigiView.waterfallColor(0.001));            // -60 dB is in between
    }
}
