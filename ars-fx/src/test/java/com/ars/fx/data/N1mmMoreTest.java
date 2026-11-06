package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.jlog.model.QsoRecord;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class N1mmMoreTest {
    String oldHome;
    final List<String[]> sent = new ArrayList<>();                      // {kind, xml}

    @BeforeEach void home(@TempDir Path tmp) {
        oldHome = System.getProperty("user.home"); System.setProperty("user.home", tmp.toString()); HubConfig.reload();
        HubConfig.set(N1mmBroadcast.KEY_ENABLED, "true");
        N1mmBroadcast.setKindSenderForTest((k, x) -> sent.add(new String[]{k, x}));
    }
    @AfterEach void restore() {
        N1mmBroadcast.setKindSenderForTest(null);
        N1mmListener.stop();
        System.setProperty("user.home", oldHome); HubConfig.reload();
    }

    static QsoRecord q(long id, String call, String band, String mode) {
        QsoRecord r = new QsoRecord();
        r.setId(id); r.setCallsign(call); r.setBand(band); r.setMode(mode); r.setDateTimeUtc(LocalDateTime.of(2026, 11, 6, 14, 5, 9));
        r.setRstSent("599"); r.setRstReceived("599");
        return r;
    }
    static N1mmBroadcast.NormalQso normal(long id, String call) {
        return new N1mmBroadcast.NormalQso(id, "2026-11-06 14:05:09", call, "14.025", "20m", "CW", "599", "579", "BOB", "tnx");
    }
    static ClusterClient.Spot spot(String call, long arrival) { return new ClusterClient.Spot(14_025_000L, call, "CW", "20m", "K1TTT", "up 1", arrival); }

    // ---- the spot message ---------------------------------------------------------------------------------
    @Test void aSpotIsWellFormedAndCarriesTheDocumentedElements() throws Exception {
        String xml = N1mm.spot(new N1mm.SpotMsg("dl1abc", 7_061_200L, "k2po", "CW 9 DB", "CW", LocalDateTime.of(2026, 11, 6, 17, 19, 37), "add", "single mult"),
                new N1mm.Ctx("W1AW", "SHACK", "W1AW", 0, false, null, 1));
        String[] root = new String[1];
        Map<String, String> f = N1mmTest.parse(xml, root);
        assertEquals("spot", root[0]);
        assertEquals("DL1ABC", f.get("dxcall"));
        assertEquals("7061.2", f.get("frequency"));
        assertEquals("K2PO", f.get("spottercall"));
        assertEquals("2026-11-06 17:19:37", f.get("timestamp"));
        assertEquals("add", f.get("action"));
        assertEquals("CW", f.get("mode"));
        assertEquals("single mult", f.get("status"));
        assertEquals("SHACK", f.get("StationName"));
        assertEquals("delete", N1mmTest.parse(N1mm.spot(new N1mm.SpotMsg("A", 1000, "B", "", "CW", null, "DELETE", ""), new N1mm.Ctx("W1AW", "S", "W1AW", 0, false, null, 1)), root).get("action"));
    }

    @Test void spotsGoOutOnlyWhenSwitchedOnAndOnlyTheNewOnes() {
        long t = System.currentTimeMillis() + 10_000;
        List<ClusterClient.Spot> all = List.of(spot("AA1AA", t + 1), spot("BB1BB", t + 2));
        assertEquals(0, N1mmBroadcast.spotsHeard(all), "off by default");
        assertTrue(sent.isEmpty());

        HubConfig.set(N1mmBroadcast.KEY_SPOTS, "true");
        assertEquals(0, N1mmBroadcast.spotsHeard(all), "turning it on does not send the backlog");
        List<ClusterClient.Spot> more = new ArrayList<>(all);
        more.add(spot("DD1DD", t + 4)); more.add(spot("CC1CC", t + 3));
        assertEquals(2, N1mmBroadcast.spotsHeard(more), "only the two that arrived since");
        assertEquals("spots", sent.get(0)[0]);
        assertTrue(sent.get(0)[1].contains("CC1CC") && sent.get(1)[1].contains("DD1DD"), "oldest first");
        assertEquals(0, N1mmBroadcast.spotsHeard(more), "and not again");
    }

    @Test void aBurstOfSpotsIsCutAtForty() {
        HubConfig.set(N1mmBroadcast.KEY_SPOTS, "true");
        long t = System.currentTimeMillis();
        N1mmBroadcast.spotsHeard(List.of());
        List<ClusterClient.Spot> burst = new ArrayList<>();
        for (int i = 1; i <= 100; i++) burst.add(spot("K" + i + "AAA", t + 1000 + i));
        assertEquals(40, N1mmBroadcast.spotsHeard(burst));
    }

    // ---- an address for each kind -----------------------------------------------------------------------------
    @Test void eachKindUsesItsOwnAddressOrTheMainOne() {
        HubConfig.set(N1mmBroadcast.KEY_DEST, "127.0.0.1:12060");
        assertEquals(12060, N1mmBroadcast.destinationsFor("spots").get(0).getPort(), "no setting of its own: the main address");
        HubConfig.set(N1mmBroadcast.KEY_DEST + ".spots", "127.0.0.1:12061 127.0.0.1:12062");
        assertEquals(2, N1mmBroadcast.destinationsFor("spots").size());
        assertEquals(12061, N1mmBroadcast.destinationsFor("spots").get(0).getPort());
        assertEquals(12060, N1mmBroadcast.destinationsFor("contacts").get(0).getPort(), "the others are unchanged");
        HubConfig.set(N1mmBroadcast.KEY_DEST + ".radio", "not an address!!");
        assertEquals(12060, N1mmBroadcast.destinationsFor("radio").get(0).getPort(), "an unreadable one falls back");
    }

    // ---- the normal log ----------------------------------------------------------------------------------
    @Test void aNormalLogQsoIsAContactWithNoContestAndKeepsItsIdThroughEditAndDelete() throws Exception {
        N1mmBroadcast.normalAdded(normal(42, "dl1abc"));
        N1mmBroadcast.normalEdited(normal(42, "DL1ABD"), "DL1ABC");
        N1mmBroadcast.normalDeleted(normal(42, "DL1ABD"));
        assertEquals(3, sent.size());
        String[] root = new String[1];
        Map<String, String> add = N1mmTest.parse(sent.get(0)[1], root);
        assertEquals("contactinfo", root[0]);
        assertEquals("DL1ABC", add.get("call"));
        assertEquals("", add.get("contestname"));
        assertEquals("1402500", add.get("rxfreq"), "the QSO's own frequency (14.025 MHz), in 10 Hz units");
        Map<String, String> rep = N1mmTest.parse(sent.get(1)[1], root);
        assertEquals("contactreplace", root[0]);
        assertEquals("DL1ABC", rep.get("oldcall"));
        Map<String, String> del = N1mmTest.parse(sent.get(2)[1], root);
        assertEquals("contactdelete", root[0]);
        assertEquals(add.get("ID"), rep.get("ID"));
        assertEquals(add.get("ID"), del.get("ID"));
        assertEquals("contacts", sent.get(0)[0]);
    }

    @Test void theNormalLogCanBeSwitchedOffAndNothingGoesWhenBroadcastingIsOff() {
        HubConfig.set(N1mmBroadcast.KEY_NORMAL, "false");
        N1mmBroadcast.normalAdded(normal(1, "K1ABC"));
        assertTrue(sent.isEmpty());
        HubConfig.set(N1mmBroadcast.KEY_NORMAL, "true");
        HubConfig.set(N1mmBroadcast.KEY_ENABLED, "false");
        N1mmBroadcast.normalAdded(normal(1, "K1ABC"));
        assertTrue(sent.isEmpty());
    }

    // ---- all computers ----------------------------------------------------------------------------------------
    @Test void aForwardedContactSaysItIsNotTheOriginalAndOnlyWhenAsked() throws Exception {
        QsoRecord r = q(7, "K1ABC", "20m", "CW");
        N1mmBroadcast.forwarded(null, r);
        assertTrue(sent.isEmpty(), "off by default");
        HubConfig.set(N1mmBroadcast.KEY_ALLCOMP, "true");
        N1mmBroadcast.forwarded(null, r);
        assertEquals(1, sent.size());
        assertEquals("False", N1mmTest.parse(sent.get(0)[1], new String[1]).get("IsOriginal"));
        assertEquals("True", N1mmTest.parse(N1mm.contactInfo(null, r, new N1mm.Ctx("W1AW", "S", "W1AW", 0, false, null, 1)), new String[1]).get("IsOriginal"), "an ordinary contact is original");
    }

    // ---- the set-frequency request ------------------------------------------------------------------------------
    @Test void aSetFrequencyRequestIsReadAndAnythingElseIsIgnored() {
        N1mm.SetFrequency f = N1mm.parseSetFrequency("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<radio_setfrequency>\n<app>WaterfallBandmap</app>\n<radionr>1</radionr>\n<frequency>14053.284</frequency>\n<mousebutton>Left</mousebutton>\n</radio_setfrequency>");
        assertEquals(1, f.radioNr());
        assertEquals(14_053_284L, f.freqHz());
        assertEquals(2, N1mm.parseSetFrequency("<Radio_SetFrequency><radionr>2</radionr><frequency>7010</frequency></Radio_SetFrequency>").radioNr());
        assertEquals(1, N1mm.parseSetFrequency("<radio_setfrequency><frequency>7010</frequency></radio_setfrequency>").radioNr(), "no radio number: radio 1");
        assertNull(N1mm.parseSetFrequency("<radio_setfrequency><frequency>50</frequency></radio_setfrequency>"), "below 100 kHz");
        assertNull(N1mm.parseSetFrequency("<radio_setfrequency><frequency>999999999</frequency></radio_setfrequency>"), "above 450 MHz");
        assertNull(N1mm.parseSetFrequency("<radio_setfrequency><frequency>abc</frequency></radio_setfrequency>"));
        assertNull(N1mm.parseSetFrequency("<radio_setfrequency><radionr>1</radionr></radio_setfrequency>"), "no frequency");
        assertNull(N1mm.parseSetFrequency("<contactinfo><frequency>7010</frequency></contactinfo>"), "another message");
        assertNull(N1mm.parseSetFrequency("<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><radio_setfrequency><frequency>7010</frequency></radio_setfrequency>"), "a DOCTYPE is refused");
        assertNull(N1mm.parseSetFrequency(null));
        assertNull(N1mm.parseSetFrequency("x".repeat(5000)));
    }

    @Test void theListenerHearsARequestOnLoopbackAndIgnoresJunk() throws Exception {
        AtomicReference<N1mm.SetFrequency> got = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        assertTrue(N1mmListener.start(0, f -> { got.set(f); latch.countDown(); }));
        int port = N1mmListener.boundPort();
        assertTrue(port > 0);
        try (DatagramSocket s = new DatagramSocket()) {
            byte[] junk = "hello".getBytes(StandardCharsets.UTF_8);
            s.send(new DatagramPacket(junk, junk.length, InetAddress.getLoopbackAddress(), port));
            byte[] ok = "<radio_setfrequency><radionr>1</radionr><frequency>21025.5</frequency></radio_setfrequency>".getBytes(StandardCharsets.UTF_8);
            s.send(new DatagramPacket(ok, ok.length, InetAddress.getLoopbackAddress(), port));
        }
        assertTrue(latch.await(3, TimeUnit.SECONDS), "the request arrived");
        assertEquals(21_025_500L, got.get().freqHz());
        N1mmListener.stop();
        assertEquals(-1, N1mmListener.boundPort());
    }

    @Test void aRequestIsNotActedOnWithoutASafeRig() {
        assertFalse(N1mmListener.qsy(null));
        assertFalse(N1mmListener.qsy(new N1mm.SetFrequency(2, 14_025_000L)), "there is one radio");
        assertFalse(N1mmListener.qsy(new N1mm.SetFrequency(1, 14_025_000L)), "no rig connected in a test");
    }
}
