package com.ars.fx.data;

import com.ars.fx.data.SpotNeeds.Need;
import com.ars.fx.data.SpotNeeds.Slot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class SpotNeedsTest {
    // two entities: Japan (dxcc 339) with JA1AAA / JH1BBB, Germany (dxcc 230) with DL1XYZ; ZZ9 does not resolve
    static final Map<String, DxccInfo.Info> TABLE = Map.of(
            "JA1AAA", new DxccInfo.Info("Japan", "AS", 339, 36, 140),
            "JH1BBB", new DxccInfo.Info("Japan", "AS", 339, 36, 140),
            "DL1XYZ", new DxccInfo.Info("Germany", "EU", 230, 51, 10));
    static final Function<String, DxccInfo.Info> RESOLVE = TABLE::get;

    static SpotNeeds.Index log(Slot... s) { return SpotNeeds.build(List.of(s), RESOLVE); }

    @Test
    void anUnworkedEntityIsANewOneEvenForAnUnknownCall() {
        var ix = log(new Slot("DL1XYZ", "20m", "CW"));
        assertEquals(Need.NEW_ENTITY, SpotNeeds.classify(ix, "JA1AAA", "20m", "CW", RESOLVE));
    }

    @Test
    void aNewCallInAWorkedEntityIsNotNeededOnTheSameBandAndMode() {
        // the old check said NEED here: this call was not in the log. The entity has been worked on 20m CW, so it is not needed.
        var ix = log(new Slot("JA1AAA", "20m", "CW"));
        assertEquals(Need.NONE, SpotNeeds.classify(ix, "JH1BBB", "20m", "CW", RESOLVE));
    }

    @Test
    void aWorkedEntityOnANewBandIsNewBand() {
        var ix = log(new Slot("JA1AAA", "20m", "CW"));
        assertEquals(Need.NEW_BAND, SpotNeeds.classify(ix, "JH1BBB", "40m", "CW", RESOLVE));
    }

    @Test
    void aNewModeGroupOnAWorkedBandIsNewMode() {
        var ix = log(new Slot("JA1AAA", "20m", "CW"));
        assertEquals(Need.NEW_MODE, SpotNeeds.classify(ix, "JA1AAA", "20m", "SSB", RESOLVE));
        assertEquals(Need.NEW_MODE, SpotNeeds.classify(ix, "JA1AAA", "20m", "FT8", RESOLVE));
    }

    @Test
    void modeGroupsAreCwPhoneDigitalNotRawModeNames() {
        var ix = log(new Slot("JA1AAA", "20m", "USB"));
        assertEquals(Need.NONE, SpotNeeds.classify(ix, "JH1BBB", "20m", "SSB", RESOLVE), "USB and SSB are both phone");
        assertEquals(Need.NEW_MODE, SpotNeeds.classify(ix, "JH1BBB", "20m", "RTTY", RESOLVE));
    }

    @Test
    void bandAndCaseAreNormalised() {
        var ix = log(new Slot("ja1aaa", " 20M ", "cw"));
        assertEquals(Need.NONE, SpotNeeds.classify(ix, "JH1BBB", "20m", "CW", RESOLVE));
    }

    @Test
    void anUnresolvableCallFallsBackToCallAndBand() {
        var ix = log(new Slot("ZZ9AA", "20m", "CW"));
        assertEquals(Need.NONE, SpotNeeds.classify(ix, "ZZ9AA", "20m", "CW", RESOLVE));
        assertEquals(Need.NEW_BAND, SpotNeeds.classify(ix, "ZZ9AA", "15m", "CW", RESOLVE));
        assertEquals(Need.NEW_BAND, SpotNeeds.classify(ix, "ZZ9BB", "20m", "CW", RESOLVE));
    }

    @Test
    void anEmptyLogNeedsEverything() {
        var ix = log();
        assertEquals(Need.NEW_ENTITY, SpotNeeds.classify(ix, "DL1XYZ", "20m", "CW", RESOLVE));
        assertEquals("NEW ONE", Need.NEW_ENTITY.label);
        assertEquals("", Need.NONE.label);
    }

    @Test
    void blankSpotCallIsIgnored() {
        assertEquals(Need.NONE, SpotNeeds.classify(log(), "  ", "20m", "CW", RESOLVE));
    }
}
