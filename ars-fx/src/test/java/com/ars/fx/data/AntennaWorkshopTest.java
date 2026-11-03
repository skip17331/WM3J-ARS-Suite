package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AntennaWorkshopTest {
    private static String val(List<String[]> rows, String key) {
        for (String[] r : rows) if (r[0].equals(key)) return r[1];
        fail("no row '" + key + "' in " + rows.stream().map(r -> r[0]).toList());
        return null;
    }

    @Test void everyCalculatorRunsOnItsDefaults() {
        assertEquals(27, AntennaCalcs.all().size());
        for (AntennaCalcs.Calc c : AntennaCalcs.all()) {
            List<String[]> rows = c.compute().apply(c.defaults());
            assertFalse(rows.isEmpty(), c.id());
            for (String[] r : rows) assertFalse(r[1].contains("NaN"), c.id() + " → " + r[0] + " = " + r[1]);
        }
    }

    @Test void flatDipoleIs468OverF() {
        var c = AntennaCalcs.get("flat-dipole");
        assertTrue(val(c.compute().apply(c.defaults()), "Total length").startsWith("33.07 ft"));
    }

    @Test void trappedDipoleFullSizeThenShortened() {
        var c = AntennaCalcs.get("trapped-dipole");
        Map<String, String> v = c.defaults();
        var full = c.compute().apply(v);
        assertEquals("Full-size trapped dipole", val(full, "Design type"));
        v.put("L_avail", "40");
        var shorty = c.compute().apply(v);
        assertEquals("Shortened trapped dipole", val(shorty, "Design type"));
    }

    @Test void efhwTrappedShortenedAddsCoil() {
        var c = AntennaCalcs.get("efhw-trapped");
        Map<String, String> v = c.defaults();
        v.put("L_avail", "100");
        var rows = c.compute().apply(v);
        assertEquals("Shortened trapped EFHW", val(rows, "Design type"));
        assertNotNull(val(rows, "Seg-1 inline loading coil"));
    }

    @Test void ohmsLawAndSwr() {
        var ohm = AntennaCalcs.get("ohms-law");
        assertEquals("V = 0.352 volts", val(ohm.compute().apply(ohm.defaults()), "Result"));
        var swr = AntennaCalcs.get("swr");
        Map<String, String> v = swr.defaults();
        v.put("mode", "z");   // 75 Ω into 50 Ω
        assertEquals("1.50:1", val(swr.compute().apply(v), "SWR"));
    }

    @Test void everyCalculatorSectionExistsInTheLibrary() {
        assertEquals("flat-dipole", AntennaCalcs.forSection("09-02").id());
        assertEquals("rf-exposure", AntennaCalcs.forSection("17-14").id());
    }

    @Test void recommenderRanksAYagiForATowerDxer() {
        var a = new AntennaRecommender.Answers();
        a.put("B4", "guyed50"); a.put("C1", List.of("20", "15", "10")); a.put("C5", List.of("dx", "contest")); a.put("D1", "no-limit"); a.put("D3", "exp");
        var top = AntennaRecommender.rank(a);
        assertEquals("yagi", top.get(0).ant().id());
        assertTrue(top.size() <= 5);
    }

    @Test void recommenderDropsJPoleWithoutVhf() {
        var a = new AntennaRecommender.Answers();
        a.put("C1", List.of("20"));
        assertTrue(AntennaRecommender.rank(a).stream().noneMatch(r -> r.ant().id().equals("j-pole")));
    }
}
