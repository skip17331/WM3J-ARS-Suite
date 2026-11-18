package com.ars.fx.surface;

import com.ars.fx.data.CountyLine;
import com.ars.fx.surface.ContestValidation.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** County-line contacts in a QSO party (punch list 4b-5): "ADA/CAN" is one QSO per county. Checked with the real Idaho definition. */
class CountyLineTest {
    static ContestPlugin idaho, cqww;

    @BeforeAll
    static void load() throws Exception {
        Path dir = Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins");
        ObjectMapper m = new ObjectMapper();
        idaho = m.readValue(dir.resolve("id_qso_party.json").toFile(), ContestPlugin.class);
        cqww = m.readValue(dir.resolve("cq_ww_cw.json").toFile(), ContestPlugin.class);
    }

    private static QsoRecord qso(ContestPlugin p, String rcvd, String sent) {
        QsoRecord r = new QsoRecord();
        r.setCallsign("K7ABC"); r.setBand("40m"); r.setMode("CW"); r.setContestId(p.getContestId());
        r.setId(42); r.setUid("u-1");
        CountyLine.set(r, p.fieldSlotColumn("state_prov_rcvd"), rcvd);
        if (p.fieldSlotColumn("state_prov_sent") != null) CountyLine.set(r, p.fieldSlotColumn("state_prov_sent"), sent);
        return r;
    }

    private static String rcvd(QsoRecord q) { return CountyLine.get(q, idaho.fieldSlotColumn("state_prov_rcvd")); }
    private static String sent(QsoRecord q) { return CountyLine.get(q, idaho.fieldSlotColumn("state_prov_sent")); }

    @Test void parts() {
        assertEquals(List.of("ADA", "CAN"), CountyLine.parts("ada / Can"));
        assertEquals(List.of("ADA"), CountyLine.parts("ADA"));
        assertEquals(List.of("ADA"), CountyLine.parts("ADA/"), "while typing");
        assertEquals(List.of("ADA", "CAN"), CountyLine.parts("ADA/CAN/ADA"), "a repeat is dropped");
        assertEquals(List.of(), CountyLine.parts("  "));
    }

    @Test void aCountyLineStationIsOneQsoPerCounty() {
        List<QsoRecord> l = CountyLine.expand(idaho, qso(idaho, "ADA/CAN", "BAN"));
        assertEquals(2, l.size());
        assertEquals(List.of("ADA", "CAN"), l.stream().map(CountyLineTest::rcvd).toList());
        for (QsoRecord q : l) {
            assertEquals("K7ABC", q.getCallsign()); assertEquals("40m", q.getBand());
            assertEquals("BAN", sent(q));
            assertEquals(0, q.getId(), "saved as a QSO of its own");
            assertNull(q.getUid());
        }
    }

    @Test void bothStationsOnLinesIsOneQsoPerPair() {
        List<QsoRecord> l = CountyLine.expand(idaho, qso(idaho, "ADA/CAN", "BAN/BEA"));
        assertEquals(4, l.size());
        assertEquals(List.of("ADA|BAN", "ADA|BEA", "CAN|BAN", "CAN|BEA"), l.stream().map(q -> rcvd(q) + "|" + sent(q)).sorted().toList());
    }

    @Test void anOrdinaryEntryIsUnchanged() {
        QsoRecord r = qso(idaho, "ADA", "BAN");
        List<QsoRecord> l = CountyLine.expand(idaho, r);
        assertEquals(1, l.size());
        assertSame(r, l.get(0));
        assertEquals(1, CountyLine.expand(cqww, qso(cqww, "14", "5")).size(), "not a QSO party: never split");
    }

    @Test void eachCountyIsValidatedOnItsOwn() {
        var f = idaho.getEntryFields().stream().filter(x -> "state_prov_rcvd".equals(x.getId())).findFirst().orElseThrow();
        assertEquals(Level.OK, ContestValidation.check(idaho, f, "ADA/CAN", true).level());
        Level bad = ContestValidation.check(idaho, f, "ZZZ", true).level();
        assertNotEquals(Level.OK, bad, "ZZZ is neither an Idaho county nor a state or province");
        assertEquals(bad, ContestValidation.check(idaho, f, "ADA/ZZZ", true).level(), "the bad half decides, as if it had been entered alone");
        assertEquals(bad, ContestValidation.check(idaho, f, "ZZZ/ADA", true).level());
        assertNotEquals(Level.HARD, ContestValidation.check(idaho, f, "ADA/", false).level(), "fine while typing");
    }
}
