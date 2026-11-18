package com.jlog.bandplan;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Band and segment edges: both ends belong to the band (14.000 and 14.350 MHz are in 20 m). */
class BandEdgesTest {
    private static final BandSegment CW = new BandSegment(14_000_000, 14_150_000, Activity.CW, " CW only "),
            PHONE = new BandSegment(14_150_000, 14_350_000, Activity.PHONE, null);
    private static final Bandplan PLAN = new Bandplan("T", null, List.of(
            new BandRange("20m", 14_000_000, 14_350_000, List.of(CW, PHONE)),
            new BandRange("17m", 18_068_000, 18_168_000, List.of())));

    @Test
    void edgesAreInsideTheBand() {
        assertEquals("20m", PLAN.bandAt(14_000_000).name());
        assertEquals("20m", PLAN.bandAt(14_350_000).name());
        assertNull(PLAN.bandAt(13_999_999));
        assertNull(PLAN.bandAt(14_350_001));
    }

    @Test
    void aSharedSegmentEdgeGoesToTheFirstSegment() {
        assertSame(CW, PLAN.segmentAt(14_150_000));
        assertSame(PHONE, PLAN.segmentAt(14_150_001));
        assertNull(PLAN.segmentAt(18_100_000), "a band with no segments has no segment");
    }

    @Test
    void segmentLabelsAndPlanNames() {
        assertEquals("CW only", CW.label(), "trimmed");
        assertEquals(Activity.PHONE.name(), PHONE.label(), "no label: the activity's name");
        assertEquals("T", PLAN.displayName(), "no display name: the id");
        assertEquals(List.of("20m", "17m"), PLAN.bands().stream().map(BandRange::name).toList(), "kept in the order given");
        assertThrows(UnsupportedOperationException.class, () -> PLAN.band("20m").segments().add(CW));
    }
}
