package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MapTracksTest {
    String oldHome;
    @BeforeEach void home(@TempDir Path tmp) { oldHome = System.getProperty("user.home"); System.setProperty("user.home", tmp.toString()); HubConfig.reload(); MapTracks.clear(); }
    @AfterEach void restore() { MapTracks.clear(); System.setProperty("user.home", oldHome); HubConfig.reload(); }

    @Test void aCallAPrefixAndAGridAllPlaceAStation() {
        double[] call = MapTracks.resolve("W1AW"), prefix = MapTracks.resolve("ZS"), grid4 = MapTracks.resolve("fn31"), grid6 = MapTracks.resolve("FN31pr");
        assertNotNull(call); assertNotNull(prefix);
        assertTrue(prefix[0] < 0, "South Africa is in the southern hemisphere");
        assertEquals(41.5, grid4[0], 0.6); assertEquals(-73, grid4[1], 1.1);
        assertEquals(41.7, grid6[0], 0.1); assertEquals(-72.7, grid6[1], 0.2);
        assertNotNull(MapTracks.resolve("W3LPL-#"), "a skimmer's suffix is ignored");
    }

    @Test void whatCannotBePlacedIsRefused() {
        assertNull(MapTracks.resolve(null));
        assertNull(MapTracks.resolve("   "));
        assertNull(MapTracks.resolve("not a station at all"));
        assertNull(MapTracks.resolve("x".repeat(40)));
        assertNull(MapTracks.make("W1AW", "nowhere!!"));
        assertNull(MapTracks.make("W1AW", "W1AW"), "a track from a place to itself");
        assertNull(MapTracks.make("FN31", "FN31"));
    }

    @Test void aTrackHasTheRightDistanceAndBearing() {
        MapTracks.Track t = MapTracks.make("fn31", "io91");                     // Connecticut to southern England
        assertEquals("FN31", t.from()); assertEquals("IO91", t.to());
        assertEquals(5500, t.km(), 150, "about 5,500 km");
        assertEquals(51, t.bearing(), 6, "north-east");
        assertEquals(0, MapTracks.bearing(0, 0, 10, 0), 1e-6, "due north");
        assertEquals(90, MapTracks.bearing(0, 0, 0, 10), 1e-6, "due east");
        assertEquals(180, MapTracks.bearing(10, 0, 0, 0), 1e-6, "due south");
        assertEquals(270, MapTracks.bearing(0, 10, 0, 0), 1e-6, "due west");
        assertTrue(t.label().startsWith("FN31 → IO91 · "), t.label());
        assertTrue(t.label().contains(" · ") && t.label().endsWith("°") && (t.label().contains(" km") || t.label().contains(" mi")), t.label());
        assertTrue(t.shortLabel().endsWith("°"));
    }

    @Test void theListKeepsTheNewestAndReplacesTheSamePair() {
        assertNotNull(MapTracks.add("W1AW", "ZS"));
        assertNotNull(MapTracks.add("W1AW", "ZS"));
        assertEquals(1, MapTracks.all().size(), "the same pair again replaces it");
        String[] to = {"JA", "VK", "ZL", "DL", "G", "F", "I", "EA", "OH", "SM"};
        for (String t : to) MapTracks.add("W1AW", t);
        assertEquals(MapTracks.MAX, MapTracks.all().size());
        assertEquals("OH", MapTracks.all().get(MapTracks.MAX - 2).to(), "the newest are kept");
        assertNull(MapTracks.add("W1AW", "!!"), "a bad one adds nothing");
        assertEquals(MapTracks.MAX, MapTracks.all().size());
        MapTracks.clear();
        assertTrue(MapTracks.all().isEmpty());
    }
}
