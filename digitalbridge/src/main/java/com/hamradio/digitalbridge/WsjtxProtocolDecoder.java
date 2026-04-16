package com.hamradio.digitalbridge;

import com.hamradio.digitalbridge.model.WsjtxDecode;
import com.hamradio.digitalbridge.model.WsjtxQsoLogged;
import com.hamradio.digitalbridge.model.WsjtxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.JulianFields;

/**
 * WsjtxProtocolDecoder — decodes the WSJT-X binary UDP protocol.
 *
 * Wire format per Network/NetworkMessage.hpp in the WSJT-X source tree:
 *
 *   Offset  Size  Field
 *   ------  ----  -----
 *      0     4    Magic  0xADBCCBDA  (big-endian)
 *      4     4    Schema version (quint32)
 *      8     4    Message type  (quint32)
 *     12     4    Id — length-prefixed UTF-8 string (instance name)
 *    ...    ...   Type-specific payload
 *
 * String encoding: quint32 length + UTF-8 bytes; 0xFFFFFFFF = null/empty.
 * QDateTime: quint64 Julian day + quint32 ms-since-midnight + quint8 time-spec.
 * All integers big-endian.
 *
 * This class is stateless and thread-safe; one instance can be shared.
 */
public class WsjtxProtocolDecoder {

    private static final Logger log = LoggerFactory.getLogger(WsjtxProtocolDecoder.class);

    public static final int MAGIC = 0xADBCCBDA;

    public static final int TYPE_HEARTBEAT   = 0;
    public static final int TYPE_STATUS      = 1;
    public static final int TYPE_DECODE      = 2;
    public static final int TYPE_CLEAR       = 3;
    public static final int TYPE_REPLY       = 4;
    public static final int TYPE_QSO_LOGGED  = 5;
    public static final int TYPE_CLOSE       = 6;
    public static final int TYPE_REPLAY      = 7;
    public static final int TYPE_HALT_TX     = 8;
    public static final int TYPE_FREE_TEXT   = 9;
    public static final int TYPE_WSPR_DECODE = 10;
    public static final int TYPE_LOCATION    = 11;

    // ── Result holder ─────────────────────────────────────────────────────────

    public static class DecodedMessage {
        public final int messageType;
        public final String id;           // WSJT-X instance name
        public final int schemaVersion;

        // At most one of these is non-null
        public final HeartbeatInfo  heartbeat;
        public final WsjtxStatus    status;
        public final WsjtxDecode    decode;
        public final WsjtxQsoLogged qsoLogged;

        DecodedMessage(int type, String id, int schema,
                       HeartbeatInfo hb, WsjtxStatus st,
                       WsjtxDecode dc, WsjtxQsoLogged qso) {
            this.messageType   = type;
            this.id            = id;
            this.schemaVersion = schema;
            this.heartbeat     = hb;
            this.status        = st;
            this.decode        = dc;
            this.qsoLogged     = qso;
        }
    }

    public static class HeartbeatInfo {
        public final int    maxSchema;
        public final String version;
        public final String revision;
        HeartbeatInfo(int max, String ver, String rev) {
            this.maxSchema = max; this.version = ver; this.revision = rev;
        }
    }

    // ── Main decode entry ─────────────────────────────────────────────────────

    /**
     * Decode a raw UDP datagram from WSJT-X.
     *
     * @param data bytes from DatagramPacket (offset-adjusted copy)
     * @return decoded message, or null if packet is not a valid WSJT-X datagram
     */
    public DecodedMessage decode(byte[] data) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
            if (buf.remaining() < 12) return null;

            int magic = buf.getInt();
            if (magic != MAGIC) { log.trace("Not WSJT-X (bad magic)"); return null; }

            int schema = buf.getInt();
            int type   = buf.getInt();
            String id  = readUtf8(buf);

            return switch (type) {
                case TYPE_HEARTBEAT  -> decodeHeartbeat(buf, id, schema);
                case TYPE_STATUS     -> decodeStatus(buf, id, schema);
                case TYPE_DECODE     -> decodeDecode(buf, id, schema);
                case TYPE_CLEAR      -> new DecodedMessage(TYPE_CLEAR, id, schema, null, null, null, null);
                case TYPE_QSO_LOGGED -> decodeQsoLogged(buf, id, schema);
                case TYPE_CLOSE      -> new DecodedMessage(TYPE_CLOSE, id, schema, null, null, null, null);
                default -> { log.trace("Unhandled WSJT-X type {}", type); yield null; }
            };

        } catch (Exception e) {
            log.warn("Failed to decode WSJT-X packet: {}", e.getMessage());
            return null;
        }
    }

    // ── Type decoders ─────────────────────────────────────────────────────────

    private DecodedMessage decodeHeartbeat(ByteBuffer buf, String id, int schema) {
        int    maxSchema = buf.getInt();
        String version   = readUtf8(buf);
        String revision  = readUtf8(buf);
        return new DecodedMessage(TYPE_HEARTBEAT, id, schema,
                new HeartbeatInfo(maxSchema, version, revision),
                null, null, null);
    }

    private DecodedMessage decodeStatus(ByteBuffer buf, String id, int schema) {
        WsjtxStatus s = new WsjtxStatus();
        s.setDialFrequency(buf.getLong());         // quint64 Hz
        s.setMode(nullIfEmpty(readUtf8(buf)));
        s.setDxCall(nullIfEmpty(readUtf8(buf)));
        s.setReport(nullIfEmpty(readUtf8(buf)));
        String txMode = readUtf8(buf);             // consumed but not exposed
        s.setTxEnabled(buf.get() != 0);
        s.setTransmitting(buf.get() != 0);
        s.setDecoding(buf.get() != 0);
        buf.getInt();                              // RX DF (not needed here)
        s.setTxDf(buf.getInt());
        s.setDxGrid(nullIfEmpty(readUtf8(buf)));
        s.setTxWatchdog(buf.get() != 0);
        // subMode, fastMode consumed without storing
        if (buf.hasRemaining()) readUtf8(buf);     // subMode
        if (buf.hasRemaining()) buf.get();         // fastMode
        s.setBand(BandUtils.frequencyToBand(s.getDialFrequency()));
        return new DecodedMessage(TYPE_STATUS, id, schema, null, s, null, null);
    }

    private DecodedMessage decodeDecode(ByteBuffer buf, String id, int schema) {
        WsjtxDecode d = new WsjtxDecode();
        buf.get();                                 // isNew bool
        int msOfDay     = buf.getInt();            // quint32 ms since midnight UTC
        d.setSnr(buf.getInt());                    // qint32
        d.setDeltaTime(buf.getDouble());           // double
        d.setDeltaFrequency(buf.getInt());         // quint32 audio Hz
        readUtf8(buf);                             // mode string from packet (we use Status mode)
        d.setMessage(nullIfEmpty(readUtf8(buf)));
        d.setLowConfidence(buf.hasRemaining() && buf.get() != 0);
        d.setOffAir(buf.hasRemaining() && buf.get() != 0);

        // Reconstruct timestamp from today + ms-of-day
        Instant ts = LocalDate.now(ZoneOffset.UTC)
                              .atStartOfDay(ZoneOffset.UTC)
                              .toInstant()
                              .plusMillis(msOfDay);
        d.setTimestamp(ts);
        return new DecodedMessage(TYPE_DECODE, id, schema, null, null, d, null);
    }

    private DecodedMessage decodeQsoLogged(ByteBuffer buf, String id, int schema) {
        WsjtxQsoLogged q = new WsjtxQsoLogged();
        q.setTimeOff(readQDateTime(buf));
        q.setDxCall(nullIfEmpty(readUtf8(buf)));
        q.setDxGrid(nullIfEmpty(readUtf8(buf)));
        q.setDialFrequency(buf.getLong());         // quint64 Hz
        q.setMode(nullIfEmpty(readUtf8(buf)));
        q.setRstSent(nullIfEmpty(readUtf8(buf)));
        q.setRstReceived(nullIfEmpty(readUtf8(buf)));
        q.setTxPower(nullIfEmpty(readUtf8(buf)));
        q.setComments(nullIfEmpty(readUtf8(buf)));
        q.setName(nullIfEmpty(readUtf8(buf)));
        q.setTimeOn(readQDateTime(buf));
        q.setOperatorCall(nullIfEmpty(readUtf8(buf)));
        q.setMyCall(nullIfEmpty(readUtf8(buf)));
        q.setMyGrid(nullIfEmpty(readUtf8(buf)));
        q.setExchangeSent(nullIfEmpty(readUtf8(buf)));
        q.setExchangeReceived(nullIfEmpty(readUtf8(buf)));
        if (buf.hasRemaining()) q.setPropagationMode(nullIfEmpty(readUtf8(buf)));

        q.setTimestamp(q.getTimeOff() != null ? q.getTimeOff() : Instant.now());
        q.setBand(BandUtils.frequencyToBand(q.getDialFrequency()));
        return new DecodedMessage(TYPE_QSO_LOGGED, id, schema, null, null, null, q);
    }

    // ── Wire type helpers ─────────────────────────────────────────────────────

    /**
     * Read a WSJT-X length-prefixed UTF-8 string.
     * Returns "" for the null sentinel 0xFFFFFFFF.
     */
    private static String readUtf8(ByteBuffer buf) {
        if (buf.remaining() < 4) return "";
        int len = buf.getInt();
        if (len == 0xFFFFFFFF || len < 0) return "";
        if (len == 0) return "";
        if (len > buf.remaining()) return "";
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Read a Qt QDateTime:
     *   quint64  Julian day number
     *   quint32  milliseconds since midnight
     *   quint8   time-spec (0=local,1=UTC,2=OffsetFromUTC,3=TimeZone)
     */
    private static Instant readQDateTime(ByteBuffer buf) {
        try {
            long julianDay = buf.getLong();
            int  msOfDay   = buf.getInt();
            buf.get(); // time-spec byte
            LocalDate date = LocalDate.MIN.with(JulianFields.JULIAN_DAY, julianDay);
            long epochSec  = date.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            return Instant.ofEpochMilli(epochSec * 1000L + msOfDay);
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private static String nullIfEmpty(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
