package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.jlog.scoring.DxccResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CtyDataTest {
    static final String SAMPLE = """
            Germany:                  14:  28:  EU:   51.00:    -10.00:    -1.0:  DL:
                DA,DB,DC,DD,DF,DG,DH,DJ,DK,DL,DM,DN,DO,DP,DQ,DR,Y2,Y3,Y4,Y5,Y6,Y7,Y8,Y9;
            United States:            05:  08:  NA:   37.53:    91.67:     5.0:  K:
                AA,AB,K,N,W,=AA1BB(4)[7],W6(3)[6];
            Fiji:                     32:  56:  OC:  -17.78:  -177.92:   -12.0:  3D2:
                3D2;
            Conway Reef:              32:  56:  OC:  -22.00:  -175.00:   -12.0:  3D2/c:
                =3D2CCC;
            Canada:                   05:  09:  NA:   44.35:    78.75:     5.0:  VE:
                VA,VE,VO(2)[9]{NA},VY,
                CF,CG;
            """;

    @Test void parsesEntitiesPrefixesAndOverrides() {
        CtyData c = CtyData.parse(SAMPLE);
        assertEquals(5, c.size());
        assertEquals("Germany", c.lookup("DL1ABC").entity().name());
        assertEquals(14, c.lookup("DL1ABC").cq());
        assertEquals(28, c.lookup("DL1ABC").itu());
        assertEquals(10.0, c.entities().get(0).lonEast(), 1e-9, "the file has west positive; we hold east positive");
    }

    @Test void longestPrefixWinsAndOverridesApply() {
        CtyData c = CtyData.parse(SAMPLE);
        assertEquals(5, c.lookup("W1AW").cq());
        assertEquals(3, c.lookup("W6XYZ").cq(), "W6 is in CQ zone 3, W is zone 5");
        assertEquals(6, c.lookup("W6XYZ").itu());
        assertEquals(2, c.lookup("VO1AA").cq());
        assertEquals(9, c.lookup("VO1AA").itu());
    }

    @Test void anExactCallBeatsAnyPrefix() {
        CtyData c = CtyData.parse(SAMPLE);
        assertEquals("Conway Reef", c.lookup("3D2CCC").entity().name());
        assertEquals("Fiji", c.lookup("3D2AB").entity().name());
        assertEquals(4, c.lookup("AA1BB").cq());
        assertEquals(7, c.lookup("AA1BB").itu());
        assertEquals("United States", c.lookup("AA1BC").entity().name(), "the next call is just a plain AA prefix");
    }

    @Test void aliasListsContinueOnTheNextLine() {
        assertEquals("Canada", CtyData.parse(SAMPLE).lookup("CF2ABC").entity().name());
    }

    @Test void portableCallsResolveByTheRightPart() {
        CtyData c = CtyData.parse(SAMPLE);
        assertEquals("Germany", c.lookup("DL1ABC/P").entity().name());
        assertEquals("United States", c.lookup("W1AW/4").entity().name());
        assertEquals("Canada", c.lookup("VE3/W1AW").entity().name(), "prefix first: the operating entity");
        assertEquals("Germany", c.lookup("W1AW/DL").entity().name(), "prefix last");
        assertNull(c.lookup("W1AW/MM"), "maritime mobile has no entity");
    }

    @Test void unknownAndBlankGiveNull() {
        CtyData c = CtyData.parse(SAMPLE);
        assertNull(c.lookup("ZZ9ZZ"));
        assertNull(c.lookup(""));
        assertNull(c.lookup(null));
    }

    @Test void theBundledFileHasTheWholeWorld() {
        CtyData c = CtyData.bundled();
        assertTrue(c.size() >= 330, "expected ~340 entities, got " + c.size());
        assertTrue(c.lookup("DL1ABC").entity().name().contains("Germany"), c.lookup("DL1ABC").entity().name());
        assertNotNull(c.lookup("JA1XYZ"));
        assertEquals("AS", c.lookup("JA1XYZ").continent());
        assertEquals(25, c.lookup("JA1XYZ").cq());
        assertEquals(5, c.lookup("W1AW").cq());
        assertEquals(3, c.lookup("AA0BY").cq(), "a listed call carries its own zone");
        assertEquals(5, c.lookup("W6AAA").cq(), "the US default zone applies to every other call");
    }

    /**
     * The engine's DXCC table and cty.dat must agree: every prefix of a cty entity resolves, in the engine, to that entity's
     * DXCC number, and two different cty entities never share one. A few cty entities are deliberately folded into a parent
     * (DXCC counts Sicily and African Italy as Italy, the Shetlands as Scotland, Asiatic and European Turkey as Turkey).
     * Also writes target/cty-audit.txt listing every disagreement.
     */
    @Test void engineTableAgreesWithCountryFiles() throws Exception {
        CtyData cty = CtyData.bundled();
        DxccResolver res = DxccResolver.getInstance();
        java.util.Set<String> noDxcc = java.util.Set.of("Vienna Intl Ctr");     // not a DXCC entity
        Map<String, Map<String, Integer>> numbersOf = new TreeMap<>();          // entity name -> engine number -> prefixes
        Map<String, java.util.Set<String>> entitiesOf = new TreeMap<>();        // engine number -> entity names
        List<String> unresolved = new ArrayList<>();
        Map<String, List<String>> detail = new TreeMap<>();
        int total = 0;
        for (CtyData.Entity e : cty.entities()) {
            if (noDxcc.contains(e.name())) continue;
            for (String p : cty.aliasPrefixes(e)) {
                String call = p + (Character.isDigit(p.charAt(p.length() - 1)) ? "AA" : "1AA");
                CtyData.Match m = cty.lookup(call);
                if (m == null || m.entity() != e) continue;                      // a longer prefix owns this call
                total++;
                DxccResolver.Entity r;
                try { r = res.resolve(call); } catch (RuntimeException ex) { r = null; }
                if (r == null) { unresolved.add(e.name() + "  " + p); continue; }
                numbersOf.computeIfAbsent(e.name(), k -> new TreeMap<>()).merge(r.id(), 1, Integer::sum);
                detail.computeIfAbsent(e.name() + " " + r.id(), k -> new ArrayList<>()).add(p);
                entitiesOf.computeIfAbsent(r.id(), k -> new java.util.TreeSet<>()).add(e.name());
            }
        }
        StringBuilder sb = new StringBuilder("cty.dat prefixes tested: " + total + "\nunresolved: " + unresolved.size() + "\n");
        unresolved.forEach(x -> sb.append("  ").append(x).append('\n'));
        List<String> split = new ArrayList<>(), shared = new ArrayList<>();
        numbersOf.forEach((name, nums) -> { if (nums.size() > 1) split.add(name + " -> " + nums); });
        entitiesOf.forEach((num, names) -> { if (names.size() > 1) shared.add(num + " <- " + names); });
        sb.append("entities resolving to more than one number:\n"); split.forEach(x -> sb.append("  ").append(x).append('\n'));
        sb.append("numbers shared by several entities:\n"); shared.forEach(x -> sb.append("  ").append(x).append('\n'));
        sb.append("detail for the split entities:\n");
        for (String line : split) { String n = line.substring(0, line.indexOf(" -> ")); detail.forEach((k, v) -> { if (k.startsWith(n + " ")) sb.append("  ").append(k).append(": ").append(v.size() > 12 ? v.subList(0, 12) + "..." : v).append('\n'); }); }
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "cty-audit.txt"), sb.toString());

        assertTrue(total > 1000);
        assertEquals(List.of(), unresolved, "cty.dat prefixes the engine table cannot place (see target/cty-audit.txt)");
        assertEquals(List.of(), split, "a cty entity must map to one DXCC number");
        // the folds that are intended
        java.util.Set<String> folded = java.util.Set.of("[Italy, Sicily]", "[African Italy, Italy, Sicily]", "[African Italy, Italy]", "[African Italy, Sicily]",
                "[Asiatic Turkey, European Turkey]", "[Scotland, Shetland Islands]");
        List<String> bad = shared.stream().filter(x -> !folded.contains(x.substring(x.indexOf("<- ") + 3))).toList();
        assertEquals(List.of(), bad, "two different cty entities share a DXCC number");
    }
}
