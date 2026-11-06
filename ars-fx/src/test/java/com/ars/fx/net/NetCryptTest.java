package com.ars.fx.net;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.jlog.model.QsoRecord;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NetCryptTest {
    static final String KEY = "contest-key";
    final List<NetNode> nodes = new ArrayList<>();
    @AfterEach void stopAll() { for (NetNode n : nodes) n.stop(); }

    // ---- the sealing ------------------------------------------------------------------------------------------
    @Test void aSealedMessageOpensWithTheSameKeyAndNoOther() {
        byte[] k = NetCrypt.sessionKey(KEY, "nonce-1");
        String msg = "{\"t\":\"qso\",\"call\":\"K1SECRET\"}";
        String frame = NetCrypt.seal(k, msg);
        assertFalse(frame.contains("K1SECRET"), "the call sign is not in the frame");
        assertEquals("enc", NetProto.type(NetProto.parse(frame)));
        assertEquals(msg, NetCrypt.open(k, frame));
        assertNull(NetCrypt.open(NetCrypt.sessionKey("other-key", "nonce-1"), frame), "the wrong shared key");
        assertNull(NetCrypt.open(NetCrypt.sessionKey(KEY, "nonce-2"), frame), "another connection's key");
    }

    @Test void eachConnectionHasItsOwnKeyAndEachFrameItsOwnIv() {
        assertFalse(java.util.Arrays.equals(NetCrypt.sessionKey(KEY, "a"), NetCrypt.sessionKey(KEY, "b")));
        assertArrayEquals(NetCrypt.sessionKey(KEY, "a"), NetCrypt.sessionKey(" " + KEY + " ", "a"), "stray spaces round the key do not matter, as in the handshake");
        byte[] k = NetCrypt.sessionKey(KEY, "a");
        assertNotEquals(NetCrypt.seal(k, "same"), NetCrypt.seal(k, "same"), "a new random IV each time");
    }

    @Test void anAlteredOrForeignFrameIsRefused() {
        byte[] k = NetCrypt.sessionKey(KEY, "n");
        String frame = NetCrypt.seal(k, "hello");
        JsonObject o = NetProto.parse(frame);
        String d = o.get("d").getAsString();
        char flip = d.charAt(30) == 'A' ? 'B' : 'A';
        o.addProperty("d", d.substring(0, 30) + flip + d.substring(31));
        assertNull(NetCrypt.open(k, o.toString()), "one changed character breaks the tag");
        assertNull(NetCrypt.open(k, "{\"t\":\"qso\",\"call\":\"K1ABC\"}"), "a message sent in the clear");
        assertNull(NetCrypt.open(k, "not json"));
        assertNull(NetCrypt.open(k, "{\"t\":\"enc\",\"d\":\"AAAA\"}"), "too short to hold an IV and tag");
        assertNull(NetCrypt.open(k, "{\"t\":\"enc\",\"d\":\"%%%\"}"), "not base64");
    }

    // ---- on the wire ------------------------------------------------------------------------------------------
    /** A bare client that does the handshake by hand and records every frame it is sent. */
    static final class Raw extends WebSocketClient {
        final BlockingQueue<String> in = new LinkedBlockingQueue<>();
        Raw(URI u) { super(u); }
        @Override public void onOpen(ServerHandshake h) { }
        @Override public void onMessage(String s) { in.add(s); }
        @Override public void onClose(int c, String r, boolean remote) { }
        @Override public void onError(Exception e) { }
        String next() throws InterruptedException { return in.poll(5, TimeUnit.SECONDS); }
    }

    @Test void afterTheWelcomeNothingCrossesTheWireInTheClear() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW");
        hs.log("K1SECRET", "20m");
        NetNode h = new NetNode("HOST", "OP", KEY, hs, () -> new NetStatus("HOST", "OP", "CQ_WW_CW", "20m", "CW", 14_025_000L, 0));
        nodes.add(h); h.startHost(0);

        Raw c = new Raw(new URI("ws://127.0.0.1:" + h.port()));
        assertTrue(c.connectBlocking(5, TimeUnit.SECONDS));
        try {
            JsonObject ch = NetProto.parse(c.next());
            assertEquals("challenge", NetProto.type(ch));
            String nonce = NetProto.str(ch, "nonce");
            c.send(NetProto.hello("RAW", "OP", NetAuth.proof(KEY, nonce, "RAW")));
            assertEquals("welcome", NetProto.type(NetProto.parse(c.next())), "the welcome is the last plain frame");

            byte[] sk = NetCrypt.sessionKey(KEY, nonce);
            String have = c.next();
            assertNotNull(have);
            assertEquals("enc", NetProto.type(NetProto.parse(have)), "the host's list of QSOs is sealed");
            assertFalse(have.contains("K1SECRET"));
            assertEquals("have", NetProto.type(NetProto.parse(NetCrypt.open(sk, have))), "and opens with the connection's key");

            // a QSO sent in the clear after the handshake is ignored; the same QSO sealed is taken
            QsoRecord q = new QsoRecord();
            q.setUid("raw-1"); q.setContestId("CQ_WW_CW"); q.setCallsign("K9RAW"); q.setBand("20m"); q.setMode("CW");
            q.setDateTimeUtc(java.time.LocalDateTime.of(2026, 11, 5, 16, 0)); q.setOrigin("RAW"); q.setUpdatedMs(System.currentTimeMillis() + 10_000);
            c.send(NetProto.qso(q));
            Thread.sleep(500);
            assertNull(hs.byCall("K9RAW"), "a frame in the clear after the handshake is dropped");
            c.send(NetCrypt.seal(sk, NetProto.qso(q)));
            long end = System.currentTimeMillis() + 5000;
            while (hs.byCall("K9RAW") == null && System.currentTimeMillis() < end) Thread.sleep(25);
            assertNotNull(hs.byCall("K9RAW"), "the sealed one is applied");
        } finally { c.closeBlocking(); }
    }

    @Test void aStationOnTheOldProtocolIsToldToUpdateNotLeftToTalkGibberish() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW");
        NetNode h = new NetNode("HOST", "OP", KEY, hs, () -> null);
        nodes.add(h); h.startHost(0);
        Raw c = new Raw(new URI("ws://127.0.0.1:" + h.port()));
        assertTrue(c.connectBlocking(5, TimeUnit.SECONDS));
        try {
            String nonce = NetProto.str(NetProto.parse(c.next()), "nonce");
            JsonObject old = NetProto.parse(NetProto.hello("OLD", "OP", NetAuth.proof(KEY, nonce, "OLD")));
            old.addProperty("v", 1);
            c.send(old.toString());
            JsonObject r = NetProto.parse(c.next());
            assertEquals("reject", NetProto.type(r));
            assertTrue(NetProto.str(r, "reason").contains("older version"), NetProto.str(r, "reason"));
        } finally { c.closeBlocking(); }
    }
}
