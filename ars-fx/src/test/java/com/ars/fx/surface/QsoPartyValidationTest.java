package com.ars.fx.surface;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.HubConfig;
import com.ars.fx.data.QsoPartySide;
import com.ars.fx.surface.ContestValidation.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.ContestPlugin.FieldDef;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QsoPartyValidationTest {
    static ContestPlugin vt;
    static FieldDef rcvd;
    private String oldHome;

    @BeforeAll static void load() throws Exception {
        vt = new ObjectMapper().readValue(Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins", "vt_qso_party.json").toFile(), ContestPlugin.class);
        rcvd = vt.getEntryFields().stream().filter(f -> "state_prov_rcvd".equals(f.getId())).findFirst().orElseThrow();
    }
    @BeforeEach void home(@TempDir Path tmp) { oldHome = System.getProperty("user.home"); System.setProperty("user.home", tmp.toString()); HubConfig.reload(); }
    @AfterEach void restore() { System.setProperty("user.home", oldHome); HubConfig.reload(); }

    static String county() { return vt.getScoringRules().getQsoParty().getInStateCounties().get(0); }
    static ContestValidation.Result check(String v) { return ContestValidation.check(vt, rcvd, v, true); }

    @Test void inStateAnyStateProvinceDxOrCountyIsFine() {
        QsoPartySide.saveSentQth(vt, county());
        assertEquals(Level.OK, check("MA").level());
        assertEquals(Level.OK, check("ON").level());
        assertEquals(Level.OK, check("DX").level());
        assertEquals(Level.OK, check(county()).level());
        assertEquals(Level.HARD, check("QQQ").level(), "VT has all 14 counties listed, so a code outside them is refused");
    }

    @Test void outOfStateOnlyTheStateCountiesAreWorkable() {
        QsoPartySide.saveSentQth(vt, "MA");
        assertEquals(Level.OK, check(county()).level());
        ContestValidation.Result r = check("NH");
        assertEquals(Level.HARD, r.level());
        assertTrue(r.message().contains("VT county"), r.message());
        assertEquals(Level.HARD, check("DX").level(), "DX is not workable from outside the state either");
    }

    @Test void withNothingSetTheOldLooseCheckApplies() {
        assertEquals(Level.OK, check("MA").level());
        assertEquals(Level.SOFT, check("QQQ").level(), "no side known: the loose check, a warning only");
    }

    @Test void everyListedPartyStateReallyHasThatManyCounties() throws Exception {
        for (String f : new String[]{"az", "ca", "de", "fl", "ga", "me", "mi", "mdc", "nc", "nh", "nm", "ns", "ny", "oh", "on", "bc", "qc", "la", "pa", "sc", "tn", "tx", "vt", "wa"}) {
            ContestPlugin p = new ObjectMapper().readValue(Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins", f + "_qso_party.json").toFile(), ContestPlugin.class);
            assertTrue(QsoPartySide.countyListDefinitive(p), f + " county list is not the length of the state's county count");
        }
    }

    @Test void partiesWithoutACheckedListOnlyWarn() throws Exception {
        for (String f : new String[]{"new_england"}) {
            ContestPlugin p = new ObjectMapper().readValue(Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins", f + "_qso_party.json").toFile(), ContestPlugin.class);
            assertFalse(QsoPartySide.countyListDefinitive(p), f + " is not a list we can check");
        }
    }
}
