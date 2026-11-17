package com.jlog.scoring;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The bundled QSO-party definitions switch the entrant-category rules on as their rules say (values in each definition's formula
 * note): checked by scoring small logs with the real plugins, so a typo in a definition's JSON or a missing key shows up here.
 */
class QsoPartyBundledRulesTest {

    private static final List<String> NO_BANDS = List.of();

    /** The bundled definition, read from the classpath as the loader reads it ("PA_QSO_PARTY" is pa_qso_party.json). */
    private static ContestPlugin plugin(String id) {
        String res = "/com/jlog/plugins/" + id.toLowerCase(java.util.Locale.ROOT) + ".json";
        try (var in = QsoPartyBundledRulesTest.class.getResourceAsStream(res)) {
            assertNotNull(in, res + " is bundled");
            ContestPlugin p = new ObjectMapper().readValue(in, ContestPlugin.class);
            assertEquals(id, p.getContestId());
            return p;
        } catch (java.io.IOException e) { throw new AssertionError(res, e); }
    }
    private static ContestPlugin.QsoPartyConfig cfg(String id) { return plugin(id).getScoringRules().getQsoParty(); }

    /** A QSO with received county {@code rcvd} sent from {@code sent}, points worked out by the plugin as the contest page does. */
    private static QsoRecord q(ContestPlugin p, StationContext ctx, String call, String band, String mode, String rcvd, String sent) {
        QsoRecord r = new QsoRecord();
        r.setCallsign(call); r.setBand(band); r.setMode(mode);
        r.setContestField1(rcvd);
        String col = p.fieldSlotColumn("state_prov_sent");
        if ("field2".equals(col)) r.setContestField2(sent); else if ("field3".equals(col)) r.setContestField3(sent);
        r.setDateTimeUtc(LocalDateTime.of(2027, 2, 28, 16, 0));
        r.setPoints(ContestScorer.points(p, r, ctx));
        return r;
    }

    @Test void everyEditedPartyLoads() {
        for (String id : List.of("DE_QSO_PARTY", "KY_QSO_PARTY", "NM_QSO_PARTY", "VT_QSO_PARTY", "WI_QSO_PARTY", "FL_QSO_PARTY",
                "NE_QSO_PARTY", "MDC_QSO_PARTY", "PA_QSO_PARTY", "ID_QSO_PARTY", "AR_QSO_PARTY", "CO_QSO_PARTY", "OK_QSO_PARTY",
                "TN_QSO_PARTY", "TX_QSO_PARTY", "WV_QSO_PARTY", "NS_QSO_PARTY", "ON_QSO_PARTY", "QC_QSO_PARTY", "MO_QSO_PARTY",
                "NC_QSO_PARTY")) {
            assertNotNull(plugin(id).getScoringRules().getQsoParty(), id);
        }
    }

    @Test void powerMultipliersFromTheDefinitions() {
        assertEquals(3.0, cfg("DE_QSO_PARTY").getPowerMultiplier().get("QRP"));
        assertEquals(5.0, cfg("NM_QSO_PARTY").getPowerMultiplier().get("QRP"));
        assertEquals(1.5, cfg("WI_QSO_PARTY").getPowerMultiplier().get("LOW"));
        assertEquals(1.5, cfg("VT_QSO_PARTY").getPowerMultiplier().get("LOW"));
        assertEquals(4.0, cfg("MDC_QSO_PARTY").getStationMultiplier().get("ROVER"));
        assertEquals(5, cfg("ID_QSO_PARTY").getPointsByPower().get("QRP"));
    }

    @Test void pennsylvaniaRoverActivatesACountyAt10Qsos() {
        ContestPlugin pa = plugin("PA_QSO_PARTY");
        StationContext rover = StationContext.of("W3XX", "FN10", "CHE", "LOW", "ROVER");
        List<QsoRecord> l = new ArrayList<>();
        for (int i = 0; i < 10; i++) l.add(q(pa, rover, "K3A" + (char) ('A' + i), "40m", "CW", "BUC", "CHE"));
        ContestScore s = ContestScorer.score(pa, l, rover, pa.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(s.points() * s.mults() + 500, s.score(), "10 QSOs from CHE: +500");
        ContestScore fixed = ContestScorer.score(pa, l, StationContext.of("W3XX", "FN10", "CHE", "LOW", "FIXED"), pa.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(fixed.points() * fixed.mults(), fixed.score(), "a fixed station gets no rover bonus");
    }

    @Test void northCarolina2027() {
        ContestPlugin nc = plugin("NC_QSO_PARTY");
        StationContext mobile = StationContext.of("W4XX", "EM95", "WAK", "LOW", "MOBILE");
        List<QsoRecord> l = new ArrayList<>(List.of(
                q(nc, mobile, "K4AA", "40m", "CW", "DUR", "WAK"),
                q(nc, mobile, "K4AB", "40m", "CW", "ORA", "CHA")));
        ContestScore s = ContestScorer.score(nc, l, mobile, nc.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(6, s.points(), "CW 3 each; no rarest-county ×10 any more");
        assertEquals(4, s.mults(), "DUR, ORA worked + WAK, CHA operated from");
        assertEquals(6 * 4 + 2 * 100, s.score(), "+100 per county activated");
        for (String c : List.of("N4H", "N4I", "N4K", "N4E", "N4N", "N4C")) l.add(q(nc, mobile, c, "20m", "SSB", "WAK", "WAK"));
        ContestScore all = ContestScorer.score(nc, l, mobile, nc.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(all.points() * all.mults() + 2 * 100 + 6 * 50 + 200, all.score(), "HIKENC: 50 each + 200 for all six");
    }

    @Test void missouriWindowElogAndMobileCountyMultiplier() {
        ContestPlugin mo = plugin("MO_QSO_PARTY");
        StationContext fixed = StationContext.of("W0XX", "EM48", "SLC", "LOW", "FIXED");
        QsoRecord a = q(mo, fixed, "K0AA", "40m", "CW", "JAC", "SLC");
        a.setDateTimeUtc(LocalDateTime.of(2027, 4, 3, 15, 0));                       // Saturday 15:00 UTC: in the window
        a.setPoints(ContestScorer.points(mo, a, fixed));
        ContestScore s = ContestScorer.score(mo, List.of(a), fixed, mo.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(s.points() * s.mults() + 100 + 1, s.score(), "+100 e-log, +1 time window");
    }

    @Test void ontarioRoverReworksFromANewArea() {
        ContestPlugin on = plugin("ON_QSO_PARTY");
        assertTrue(cfg("ON_QSO_PARTY").isOwnCountyDupe());
        StationContext rover = StationContext.of("VE3XX", "FN03", "TOR", "LOW", "ROVER");
        QsoRecord first = q(on, rover, "VE3AA", "40m", "CW", "OTT", "TOR");
        assertFalse(ContestScorer.isDupe(on, q(on, rover, "VE3AA", "40m", "CW", "OTT", "YRK"), List.of(first)));
        assertTrue(ContestScorer.isDupe(on, q(on, rover, "VE3AA", "40m", "CW", "OTT", "TOR"), List.of(first)));
    }
}
