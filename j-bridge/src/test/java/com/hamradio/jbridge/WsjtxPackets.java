package com.hamradio.jbridge;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.JulianFields;

/** Builds WSJT-X UDP datagrams the way WSJT-X writes them (Qt QDataStream, big-endian), for tests. */
final class WsjtxPackets {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final DataOutputStream out = new DataOutputStream(bytes);

    static WsjtxPackets of(int type, String id) { return new WsjtxPackets().i32(WsjtxProtocolDecoder.MAGIC).i32(3).i32(type).str(id); }

    WsjtxPackets i32(int v)     { try { out.writeInt(v); } catch (IOException e) { throw new UncheckedIOException(e); } return this; }
    WsjtxPackets i64(long v)    { try { out.writeLong(v); } catch (IOException e) { throw new UncheckedIOException(e); } return this; }
    WsjtxPackets f64(double v)  { try { out.writeDouble(v); } catch (IOException e) { throw new UncheckedIOException(e); } return this; }
    WsjtxPackets bool(boolean v){ try { out.writeByte(v ? 1 : 0); } catch (IOException e) { throw new UncheckedIOException(e); } return this; }
    /** QByteArray / utf8 string: length then bytes; null is written as length 0xFFFFFFFF. */
    WsjtxPackets str(String s) {
        if (s == null) return i32(0xFFFFFFFF);
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        i32(b.length);
        try { out.write(b); } catch (IOException e) { throw new UncheckedIOException(e); }
        return this;
    }
    /** QDateTime: Julian day (qint64), ms of day (quint32), time spec (1 = UTC). */
    WsjtxPackets time(Instant t) {
        LocalDate d = t.atZone(ZoneOffset.UTC).toLocalDate();
        long ms = t.toEpochMilli() - d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        i64(d.getLong(JulianFields.JULIAN_DAY)); i32((int) ms);
        try { out.writeByte(1); } catch (IOException e) { throw new UncheckedIOException(e); }
        return this;
    }
    byte[] bytes() { return bytes.toByteArray(); }
}
