package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Club Log's dated DXCC exceptions (cty.xml). */
class ClubLogCtyTest {
    // the shape of Club Log's cty.xml: entities, then exceptions, then sections this class does not read
    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <clublog date="2026-11-17T00:00:00+00:00">
             <entities><entity><adif>1</adif><name>CANADA</name><prefix>VE</prefix></entity></entities>
             <exceptions record="3">
              <exception record="1"><call>VP8XYZ</call><entity>SOUTH SHETLAND ISLANDS</entity><adif>241</adif><cqz>13</cqz>
               <cont>SA</cont><long>-58.40</long><lat>-62.08</lat>
               <start>2026-01-10T00:00:00+00:00</start><end>2026-02-20T23:59:59+00:00</end></exception>
              <exception record="2"><call>KH8/K1ABC</call><entity>AMERICAN SAMOA</entity><adif>9</adif><cqz>32</cqz><cont>OC</cont>
               <long>-170.7</long><lat>-14.3</lat><start>2025-06-01T00:00:00+00:00</start></exception>
              <exception record="3"><call>BAD</call><entity>NOWHERE</entity><adif></adif></exception>
             </exceptions>
             <prefixes><prefix record="1"><call>VE</call><adif>1</adif></prefix></prefixes>
            </clublog>
            """;

    @AfterEach void clear() { ClubLogCty.setForTest(Map.of()); }

    private static byte[] gzip(String s) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (GZIPOutputStream z = new GZIPOutputStream(b)) { z.write(s.getBytes(StandardCharsets.UTF_8)); }
        return b.toByteArray();
    }

    @Test
    void readsExceptionsPlainOrGzippedAndSkipsBrokenOnes() throws Exception {
        for (byte[] body : List.of(XML.getBytes(StandardCharsets.UTF_8), gzip(XML))) {
            Map<String, List<ClubLogCty.CallException>> m = ClubLogCty.parse(body);
            assertEquals(2, m.size(), "the record without an entity number is skipped");
            ClubLogCty.CallException e = m.get("VP8XYZ").get(0);
            assertEquals(241, e.adif());
            assertEquals("SA", e.cont());
            assertEquals(-58.40, e.lon(), 1e-9);
            assertEquals(Instant.parse("2026-02-20T23:59:59Z").getEpochSecond(), e.end());
            assertEquals(Long.MAX_VALUE, m.get("KH8/K1ABC").get(0).end(), "no end date: still current");
        }
    }

    @Test
    void appliesOnlyInsideItsDatesInclusive() throws Exception {
        ClubLogCty.setForTest(ClubLogCty.parse(XML.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(ClubLogCty.at("vp8xyz", Instant.parse("2026-01-10T00:00:00Z")), "first second, any case");
        assertNotNull(ClubLogCty.at("VP8XYZ", Instant.parse("2026-02-20T23:59:59Z")), "last second");
        assertNull(ClubLogCty.at("VP8XYZ", Instant.parse("2026-02-21T00:00:00Z")));
        assertNull(ClubLogCty.at("VP8XYZ", Instant.parse("2025-12-31T00:00:00Z")));
        assertNotNull(ClubLogCty.at("K1ABC/KH8", Instant.parse("2026-11-17T00:00:00Z")), "A/B also tried as B/A");
    }

    @Test
    void dxccLookupUsesTheExceptionAtTheQsoTime() throws Exception {
        ClubLogCty.setForTest(ClubLogCty.parse(XML.getBytes(StandardCharsets.UTF_8)));
        DxccInfo.Info during = DxccInfo.of("VP8XYZ", Instant.parse("2026-02-01T12:00:00Z"));
        assertEquals(241, during.dxcc());
        assertEquals("SA", during.continent());
        DxccInfo.Info after = DxccInfo.of("VP8XYZ", Instant.parse("2026-06-01T12:00:00Z"));
        assertNotEquals(241, after.dxcc(), "outside its dates the prefix decides again (VP8 = Falklands)");
        assertEquals(Instant.parse("2026-02-01T12:00:00Z"), DxccInfo.utc("2026-02-01 12:00:00"));
        assertNull(DxccInfo.utc("garbage"));
    }

    @Test
    void aShortOrBrokenDownloadIsNotKept() throws Exception {
        assertTrue(ClubLogCty.save(XML.getBytes(StandardCharsets.UTF_8)).contains("kept the copy"), "2 exceptions is not Club Log's file");
        assertTrue(ClubLogCty.save("<html>Forbidden</html>".getBytes(StandardCharsets.UTF_8)).contains("not the country file")
                || ClubLogCty.save("<html>Forbidden</html>".getBytes(StandardCharsets.UTF_8)).contains("kept the copy"));
        assertEquals(0, ClubLogCty.size());
    }

    @Test
    void titleCasesClubLogNames() {
        assertEquals("South Shetland Islands", DxccInfo.titleCase("SOUTH SHETLAND ISLANDS"));
        assertEquals("Isle of Man", DxccInfo.titleCase("ISLE OF MAN"));
    }
}
