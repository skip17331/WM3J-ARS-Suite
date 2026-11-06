package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class N1mmTest {
    String oldHome;

    @BeforeEach void home(@TempDir Path tmp) { oldHome = System.getProperty("user.home"); System.setProperty("user.home", tmp.toString()); HubConfig.reload(); }
    @AfterEach void restore() { N1mmBroadcast.setSenderForTest(null); System.setProperty("user.home", oldHome); HubConfig.reload(); }

    static ContestPlugin load(String f) throws Exception {
        return new ObjectMapper().readValue(Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins", f).toFile(), ContestPlugin.class);
    }
    /** Parses the message as XML (it must be well formed) and returns the root element's name and its child elements' text. */
    static Map<String, String> parse(String xml, String[] rootName) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document d = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        Element root = d.getDocumentElement(); if (rootName != null) rootName[0] = root.getTagName();
        Map<String, String> out = new LinkedHashMap<>();
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) if (n instanceof Element e) out.put(e.getTagName(), e.getTextContent());
        return out;
    }
    static Map<String, String> parse(String xml) throws Exception { return parse(xml, null); }

    static N1mm.Ctx ctx(boolean mult, String oldCall) { return new N1mm.Ctx("W1AW", "SHACK-PC", "K1TTT", 14_025_000L, mult, oldCall, 1); }
    static QsoRecord qso(String call, String band, String mode) {
        QsoRecord q = new QsoRecord(); q.setId(42); q.setCallsign(call); q.setBand(band); q.setMode(mode);
        q.setDateTimeUtc(LocalDateTime.of(2026, 11, 5, 16, 43, 38)); q.setRstSent("599"); q.setRstReceived("579"); q.setPoints(3);
        return q;
    }
    static void put(ContestPlugin p, QsoRecord q, String fieldId, String v) {
        switch (p.fieldSlotColumn(fieldId)) {
            case "field1" -> q.setContestField1(v); case "field2" -> q.setContestField2(v); case "field3" -> q.setContestField3(v);
            case "field4" -> q.setContestField4(v); case "field5" -> q.setContestField5(v); default -> fail("no slot for " + fieldId);
        }
    }

    @Test void aContactIsWellFormedXmlWithN1mmsElementsAndUnits() throws Exception {
        ContestPlugin p = load("cq_ww_cw.json");
        QsoRecord q = qso("K2DE", "20m", "CW"); put(p, q, "cq_zone", "5");
        String[] root = new String[1];
        Map<String, String> m = parse(N1mm.contactInfo(p, q, ctx(true, null)), root);
        assertEquals("contactinfo", root[0]);
        assertEquals("ARS-Suite", m.get("app"));
        assertEquals("2026-11-05 16:43:38", m.get("timestamp"));
        assertEquals("W1AW", m.get("mycall")); assertEquals("K2DE", m.get("call"));
        assertEquals("14", m.get("band"), "N1MM+ names 20 m by its lower MHz figure");
        assertEquals("1402500", m.get("rxfreq"), "frequencies are in units of 10 Hz: 14.025 MHz");
        assertEquals("1402500", m.get("txfreq"));
        assertEquals("CW", m.get("mode"));
        assertEquals("599", m.get("snt")); assertEquals("579", m.get("rcv"));
        assertEquals("5", m.get("zone"));
        assertEquals("K2", m.get("wpxprefix")); assertEquals("W1", m.get("stationprefix"));
        assertEquals("K", m.get("countryprefix"), "the entity's own prefix, not the WPX one");
        assertEquals("K1TTT", m.get("operator"), "no operator on the QSO: the station's operator");
        assertEquals("1", m.get("ismultiplier1")); assertEquals("3", m.get("points"));
        assertEquals("SHACK-PC", m.get("StationName")); assertEquals("True", m.get("IsOriginal"));
        for (String k : List.of("contestname", "contestnr", "sntnr", "rcvnr", "gridsquare", "exchange1", "section", "comment", "qth", "name", "power", "misctext", "prec", "ck",
                "ismultiplier2", "ismultiplier3", "radionr", "run1run2", "RoverLocation", "RadioInterfaced", "NetworkedCompNr", "NetBiosName", "IsRunQSO", "ID", "IsClaimedQso"))
            assertTrue(m.containsKey(k), "missing element " + k);
        assertEquals(p.getContestId(), m.get("contestname"), "the contest's own id");
    }

    @Test void theSweepstakesExchangeLandsInSectionPrecAndCheck() throws Exception {
        ContestPlugin p = load("arrl_sweepstakes_ssb.json");
        QsoRecord q = qso("W9XYZ", "40m", "SSB"); q.setSerialReceived("123");
        put(p, q, "precedence", "A"); put(p, q, "check", "89"); put(p, q, "section", "WI");
        Map<String, String> m = parse(N1mm.contactInfo(p, q, ctx(false, null)));
        assertEquals("A", m.get("prec")); assertEquals("89", m.get("ck")); assertEquals("WI", m.get("section"));
        assertEquals("123", m.get("rcvnr")); assertEquals("0", m.get("ismultiplier1"));
        assertEquals("7", m.get("band"));
    }

    @Test void aReplaceNamesTheOldCallAndADeleteCarriesTheSameId() throws Exception {
        ContestPlugin p = load("cq_ww_cw.json");
        QsoRecord q = qso("K2DE", "20m", "CW");
        String[] root = new String[1];
        Map<String, String> r = parse(N1mm.contactReplace(p, q, ctx(false, "K2DX")), root);
        assertEquals("contactreplace", root[0]);
        assertEquals("K2DX", r.get("oldcall")); assertEquals(r.get("timestamp"), r.get("oldtimestamp")); assertEquals("K2DE", r.get("call"));
        Map<String, String> info = parse(N1mm.contactInfo(p, q, ctx(false, null)));
        Map<String, String> del = parse(N1mm.contactDelete(p, q, ctx(false, null)), root);
        assertEquals("contactdelete", root[0]);
        assertEquals(List.of("app", "timestamp", "mycall", "band", "call", "contestnr", "StationName", "ID"), List.copyOf(del.keySet()), "exactly the elements N1MM+ documents");
        assertTrue(info.get("ID").matches("[0-9a-f]{32}"), info.get("ID"));
        assertEquals(info.get("ID"), del.get("ID"), "the same QSO has the same id in every message");
        assertEquals(info.get("ID"), r.get("ID"));
        QsoRecord other = qso("K2DE", "20m", "CW"); other.setId(43);
        assertNotEquals(info.get("ID"), parse(N1mm.contactInfo(p, other, ctx(false, null))).get("ID"));
    }

    @Test void textIsEscapedSoTheMessageStaysWellFormed() throws Exception {
        ContestPlugin p = load("cq_ww_cw.json");
        QsoRecord q = qso("K2DE", "20m", "CW"); q.setNotes("Tom & Jerry <test> \u0001ok");
        Map<String, String> m = parse(N1mm.contactInfo(p, q, ctx(false, null)));
        assertEquals("Tom & Jerry <test> ok", m.get("comment"));
        assertEquals("&amp;&lt;&gt;", N1mm.esc("&<>"));
    }

    @Test void bandModeAndPrefixConversions() {
        assertEquals("1.8", N1mm.band("160m")); assertEquals("3.5", N1mm.band("80m")); assertEquals("14", N1mm.band("20m")); assertEquals("50", N1mm.band("6m"));
        assertEquals("144", N1mm.band("2m")); assertEquals("", N1mm.band(null));
        assertEquals("SSB", N1mm.n1mmMode("USB")); assertEquals("CW", N1mm.n1mmMode("cw")); assertEquals("RTTY", N1mm.n1mmMode("RTTY"));
        assertEquals("USB", N1mm.radioMode("PKTUSB")); assertEquals("CW", N1mm.radioMode("CWR")); assertEquals("LSB", N1mm.radioMode("LSB"));
        assertEquals("K2", N1mm.wpxPrefix("K2DE")); assertEquals("VE3", N1mm.wpxPrefix("VE3XYZ")); assertEquals("9A1", N1mm.wpxPrefix("9A1A"));
        assertEquals("W1", N1mm.wpxPrefix("W1AW")); assertEquals("OH2", N1mm.wpxPrefix("OH2BH")); assertEquals("W1", N1mm.wpxPrefix("W1AW/4"));
        assertEquals("", N1mm.wpxPrefix(""));
    }

    @Test void radioInfoGivesTheFrequencyInTensOfHertz() throws Exception {
        String[] root = new String[1];
        Map<String, String> m = parse(N1mm.radioInfo(new N1mm.Radio(true, 14_074_000L, 14_077_500L, "PKTUSB", true, false, 1, "", "IC-7300"), ctx(false, null)), root);
        assertEquals("RadioInfo", root[0]);
        assertEquals("1407400", m.get("Freq")); assertEquals("1407750", m.get("TXFreq"));
        assertEquals("USB", m.get("Mode")); assertEquals("True", m.get("IsSplit")); assertEquals("False", m.get("IsTransmitting")); assertEquals("True", m.get("IsConnected"));
        assertEquals("W1AW", m.get("OpCall")); assertEquals("1", m.get("RadioNr")); assertEquals("IC-7300", m.get("RadioName"));
        assertEquals("-1", m.get("AuxAntSelected"));
    }

    @Test void appInfoNamesTheContest() throws Exception {
        Map<String, String> m = parse(N1mm.appInfo(load("cq_ww_cw.json"), ctx(false, null)));
        assertEquals("ARS-Suite", m.get("app")); assertEquals("W1AW", m.get("mycall")); assertEquals("1", m.get("contestnr")); assertFalse(m.get("contestname").isEmpty());
    }

    @Test void theScoreCountsNonDupesByBandAndModeClass() throws Exception {
        ContestPlugin p = load("cq_ww_cw.json");
        QsoRecord a = qso("K2DE", "20m", "CW"), b = qso("K3LR", "20m", "CW"), c = qso("DL1ABC", "40m", "SSB"), d = qso("K2DE", "20m", "CW"); d.setDupe(true);
        Map<String, String> cab = new LinkedHashMap<>(); cab.put("power", "LOW"); cab.put("assisted", "ASSISTED"); cab.put("operator", "SINGLE-OP"); cab.put("band", "ALL"); cab.put("mode", "CW"); cab.put("club", "A&B ARC");
        String xml = N1mm.score(p, List.of(a, b, c, d), 9, ctx(false, null), cab, "FN31pr12");
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertEquals("dynamicresults", doc.getDocumentElement().getTagName());
        Element cls = (Element) doc.getElementsByTagName("class").item(0);
        assertEquals("LOW", cls.getAttribute("power")); assertEquals("ASSISTED", cls.getAttribute("assisted")); assertEquals("ALL", cls.getAttribute("bands"));
        assertEquals("FN31PR", ((Element) doc.getElementsByTagName("qth").item(0)).getAttribute("grid6"));
        assertEquals("A&B ARC", doc.getElementsByTagName("club").item(0).getTextContent());
        assertEquals("9", doc.getElementsByTagName("score").item(0).getTextContent());
        Map<String, String> qsos = new LinkedHashMap<>(), points = new LinkedHashMap<>();
        for (int i = 0; i < doc.getElementsByTagName("qso").getLength(); i++) { Element e = (Element) doc.getElementsByTagName("qso").item(i); qsos.put(e.getAttribute("band") + "/" + e.getAttribute("mode"), e.getTextContent()); }
        for (int i = 0; i < doc.getElementsByTagName("point").getLength(); i++) { Element e = (Element) doc.getElementsByTagName("point").item(i); points.put(e.getAttribute("band") + "/" + e.getAttribute("mode"), e.getTextContent()); }
        assertEquals("3", qsos.get("total/ALL"), "the dupe is not counted");
        assertEquals("2", qsos.get("14/CW")); assertEquals("1", qsos.get("7/PH"));
        assertEquals("9", points.get("total/ALL")); assertEquals("6", points.get("14/CW"));
    }

    @Test void destinationsAreParsedFromThePairsTyped() {
        var d = N1mmBroadcast.destinations("127.0.0.1:12060 192.168.1.255:12061, 10.0.0.5");
        assertEquals(3, d.size());
        assertEquals(12060, d.get(0).getPort()); assertEquals(12061, d.get(1).getPort()); assertEquals(12060, d.get(2).getPort(), "a bare address gets the default port");
        assertEquals("192.168.1.255", d.get(1).getAddress().getHostAddress());
        assertTrue(N1mmBroadcast.destinations("nonsense:port 300.1.1.1:99999 :12060 127.0.0.1:0 ::1").isEmpty(), "unreadable entries are skipped");
        assertTrue(N1mmBroadcast.destinations("").isEmpty()); assertTrue(N1mmBroadcast.destinations(null).isEmpty());
    }

    @Test void nothingIsSentUntilTheOperatorTurnsItOn() throws Exception {
        List<String> sent = new java.util.ArrayList<>();
        N1mmBroadcast.setSenderForTest(sent::add);
        ContestPlugin p = load("cq_ww_cw.json");
        N1mmBroadcast.contactAdded(p, qso("K2DE", "20m", "CW"), false);
        assertTrue(sent.isEmpty(), "off by default");
        HubConfig.setBool(N1mmBroadcast.KEY_ENABLED, true);
        N1mmBroadcast.contactAdded(p, qso("K2DE", "20m", "CW"), true);
        N1mmBroadcast.contactEdited(p, qso("K2DE", "20m", "CW"), "K2DX");
        N1mmBroadcast.contactDeleted(p, qso("K2DE", "20m", "CW"));
        N1mmBroadcast.lookup(p, qso("K2DE", "20m", "CW"));
        N1mmBroadcast.appInfo(p);
        assertEquals(5, sent.size());
        assertTrue(sent.get(0).contains("<contactinfo>") && sent.get(1).contains("<contactreplace>") && sent.get(2).contains("<contactdelete>")
                && sent.get(3).contains("<lookupinfo>") && sent.get(4).contains("<AppInfo>"));
        sent.clear();
        HubConfig.setBool(N1mmBroadcast.KEY_CONTACTS, false); HubConfig.setBool(N1mmBroadcast.KEY_LOOKUP, false);
        N1mmBroadcast.contactAdded(p, qso("K2DE", "20m", "CW"), false); N1mmBroadcast.lookup(p, qso("K2DE", "20m", "CW"));
        assertTrue(sent.isEmpty(), "each kind of message has its own switch");
        N1mmBroadcast.score();
        assertTrue(sent.isEmpty(), "the score is off unless asked for");
    }

    @Test void aContactReallyArrivesOverUdp() throws Exception {
        try (DatagramSocket rx = new DatagramSocket(0, java.net.InetAddress.getLoopbackAddress())) {
            rx.setSoTimeout(3000);
            HubConfig.setBool(N1mmBroadcast.KEY_ENABLED, true);
            HubConfig.set(N1mmBroadcast.KEY_DEST, "127.0.0.1:" + rx.getLocalPort() + " 127.0.0.1:1");          // a second, dead destination must not matter
            ContestPlugin p = load("cq_ww_cw.json");
            N1mmBroadcast.contactAdded(p, qso("K2DE", "20m", "CW"), true);
            byte[] buf = new byte[8192];
            DatagramPacket pk = new DatagramPacket(buf, buf.length);
            rx.receive(pk);
            String xml = new String(pk.getData(), 0, pk.getLength(), StandardCharsets.UTF_8);
            Map<String, String> m = parse(xml);
            assertEquals("K2DE", m.get("call")); assertEquals("14", m.get("band"));
        }
    }

    @Test void theCountryPrefixIsTheEntitysOwnPrefix() {
        assertEquals("K", N1mm.countryPrefix("W1AW"));
        assertEquals("DL", N1mm.countryPrefix("DL1ABC"));
        assertEquals("KH6", N1mm.countryPrefix("AH6XYZ"), "Hawaii is its own country");
        assertEquals("", N1mm.countryPrefix("W1AW/MM"), "maritime mobile has no country");
        assertEquals("", N1mm.countryPrefix(""));
        assertEquals("", N1mm.countryPrefix(null));
    }
}
