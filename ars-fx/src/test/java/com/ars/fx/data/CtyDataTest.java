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

    /** Not a pass/fail check: writes target/cty-audit.txt listing where the engine's prefix table and cty.dat disagree. */
    @Test void auditAgainstTheEnginePrefixTable() throws Exception {
        CtyData cty = CtyData.bundled();
        DxccResolver res = DxccResolver.getInstance();
        List<String> missing = new ArrayList<>();       // cty knows it, the engine does not
        Map<String, Integer> byEntity = new TreeMap<>();
        int total = 0, resolved = 0;
        for (CtyData.Entity e : cty.entities()) {
            for (String p : cty.aliasPrefixes(e)) {
                String call = p + (Character.isDigit(p.charAt(p.length() - 1)) ? "AA" : "1AA");
                CtyData.Match m = cty.lookup(call);
                if (m == null || m.entity() != e) continue;          // a longer prefix owns this call; test that one instead
                total++;
                DxccResolver.Entity r;
                try { r = res.resolve(call); } catch (RuntimeException ex) { r = null; }
                if (r != null) { resolved++; continue; }
                missing.add(e.name() + "  " + p);
                byEntity.merge(e.name(), 1, Integer::sum);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("cty.dat prefixes tested: ").append(total).append("\nresolved by the engine table: ").append(resolved)
          .append("\nnot resolved: ").append(missing.size()).append("\nentities with at least one unresolved prefix: ").append(byEntity.size()).append("\n\n");
        byEntity.forEach((k, v) -> sb.append(String.format("%3d  %s%n", v, k)));
        sb.append("\n--- unresolved prefixes ---\n");
        missing.forEach(x -> sb.append(x).append('\n'));
        Files.writeString(Path.of("target", "cty-audit.txt"), sb.toString());
        assertTrue(total > 1000);
    }
}
