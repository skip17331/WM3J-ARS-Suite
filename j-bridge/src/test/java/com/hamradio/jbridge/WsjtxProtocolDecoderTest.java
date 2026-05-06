package com.hamradio.jbridge;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static com.hamradio.jbridge.WsjtxProtocolDecoder.SRC_JTDX;
import static com.hamradio.jbridge.WsjtxProtocolDecoder.SRC_UNKNOWN;
import static com.hamradio.jbridge.WsjtxProtocolDecoder.SRC_WSJTX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WsjtxProtocolDecoderTest {

    // ── Source app detection ─────────────────────────────────────────────────

    @Test
    void detectsWsjtx() {
        assertEquals(SRC_WSJTX, WsjtxProtocolDecoder.detectSourceApp("WSJT-X"));
    }

    @Test
    void detectsWsjtxAlternateSpelling() {
        // Some WSJT-X distributions report "WSJTX" without the dash.
        assertEquals(SRC_WSJTX, WsjtxProtocolDecoder.detectSourceApp("WSJTX"));
    }

    @Test
    void detectsJtdx() {
        assertEquals(SRC_JTDX, WsjtxProtocolDecoder.detectSourceApp("JTDX"));
    }

    @Test
    void detectsJtdxMultiInstance() {
        // Multi-instance setups suffix the name (e.g. -North) — match prefix.
        assertEquals(SRC_JTDX, WsjtxProtocolDecoder.detectSourceApp("JTDX-North"));
    }

    @Test
    void detectsWsjtxMultiInstance() {
        assertEquals(SRC_WSJTX, WsjtxProtocolDecoder.detectSourceApp("WSJT-X-Main"));
    }

    @Test
    void caseInsensitive() {
        assertEquals(SRC_JTDX,  WsjtxProtocolDecoder.detectSourceApp("jtdx"));
        assertEquals(SRC_WSJTX, WsjtxProtocolDecoder.detectSourceApp("wsjt-x"));
    }

    @Test
    void unknownIdReportedAsUnknown() {
        // MSHV is intentionally not in the supported list (no native macOS).
        assertEquals(SRC_UNKNOWN, WsjtxProtocolDecoder.detectSourceApp("MSHV"));
        assertEquals(SRC_UNKNOWN, WsjtxProtocolDecoder.detectSourceApp("Some Other App"));
    }

    @Test
    void nullAndEmptyIdReturnUnknown() {
        assertEquals(SRC_UNKNOWN, WsjtxProtocolDecoder.detectSourceApp(null));
        assertEquals(SRC_UNKNOWN, WsjtxProtocolDecoder.detectSourceApp(""));
        assertEquals(SRC_UNKNOWN, WsjtxProtocolDecoder.detectSourceApp("   "));
    }

    // ── Forward-compatibility: heartbeat with extra trailing bytes ───────────

    @Test
    void heartbeatWithExtraTrailingBytesParses() {
        // Build a valid WSJT-X heartbeat then append 64 bytes of garbage —
        // the decoder reads only the well-known fields and must still succeed.
        byte[] base = buildHeartbeat("JTDX", 3, "2.2.0+rc207", "abc123");
        byte[] padded = new byte[base.length + 64];
        System.arraycopy(base, 0, padded, 0, base.length);
        // remaining bytes default to zero — that's fine, decoder ignores them

        WsjtxProtocolDecoder dec = new WsjtxProtocolDecoder();
        WsjtxProtocolDecoder.DecodedMessage msg = dec.decode(padded);
        assertNotNull(msg, "decoder should accept WSJT-X-family packets with trailing fields");
        assertEquals(WsjtxProtocolDecoder.TYPE_HEARTBEAT, msg.messageType);
        assertEquals("JTDX", msg.id);
        assertNotNull(msg.heartbeat);
        assertEquals("2.2.0+rc207", msg.heartbeat.version);
    }

    @Test
    void heartbeatFromWsjtxAndJtdxParseIdentically() {
        WsjtxProtocolDecoder dec = new WsjtxProtocolDecoder();
        var w = dec.decode(buildHeartbeat("WSJT-X", 3, "2.7.0", ""));
        var j = dec.decode(buildHeartbeat("JTDX",   3, "2.2.0", ""));
        assertNotNull(w); assertNotNull(j);
        assertEquals(WsjtxProtocolDecoder.TYPE_HEARTBEAT, w.messageType);
        assertEquals(WsjtxProtocolDecoder.TYPE_HEARTBEAT, j.messageType);
        assertEquals("WSJT-X", w.id);
        assertEquals("JTDX",   j.id);
    }

    @Test
    void badMagicReturnsNull() {
        ByteBuffer buf = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
        buf.putInt(0x12345678); // not the magic
        buf.putInt(3);
        buf.putInt(0);
        WsjtxProtocolDecoder dec = new WsjtxProtocolDecoder();
        assertEquals(null, dec.decode(buf.array()));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Build a synthetic WSJT-X-protocol heartbeat datagram for the test. */
    private static byte[] buildHeartbeat(String id, int maxSchema, String version, String revision) {
        byte[] idB  = id.getBytes(StandardCharsets.UTF_8);
        byte[] verB = version.getBytes(StandardCharsets.UTF_8);
        byte[] revB = revision.getBytes(StandardCharsets.UTF_8);
        int size = 4 + 4 + 4                              // magic + schema + type
                 + 4 + idB.length                         // id (length-prefixed)
                 + 4                                      // maxSchema
                 + 4 + verB.length                        // version
                 + 4 + revB.length;                       // revision
        ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
        buf.putInt(WsjtxProtocolDecoder.MAGIC);
        buf.putInt(3);                                    // schema
        buf.putInt(WsjtxProtocolDecoder.TYPE_HEARTBEAT);  // type
        buf.putInt(idB.length);  buf.put(idB);
        buf.putInt(maxSchema);
        buf.putInt(verB.length); buf.put(verB);
        buf.putInt(revB.length); buf.put(revB);
        return buf.array();
    }
}
