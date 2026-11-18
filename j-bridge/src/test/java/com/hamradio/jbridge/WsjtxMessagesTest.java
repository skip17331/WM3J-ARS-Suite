package com.hamradio.jbridge;

import static org.junit.jupiter.api.Assertions.*;

import com.hamradio.jbridge.model.WsjtxDecode;
import com.hamradio.jbridge.model.WsjtxQsoLogged;
import com.hamradio.jbridge.model.WsjtxStatus;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** The WSJT-X messages J-Bridge acts on, as WSJT-X sends them. */
class WsjtxMessagesTest {
    private final WsjtxProtocolDecoder dec = new WsjtxProtocolDecoder();

    static byte[] status(long dialHz, String mode, String dxCall) {
        return WsjtxPackets.of(WsjtxProtocolDecoder.TYPE_STATUS, "WSJT-X")
                .i64(dialHz).str(mode).str(dxCall).str("-12").str("FT8")
                .bool(true).bool(false).bool(true)                 // tx enabled, transmitting, decoding
                .i32(1500).i32(1234).str("FN42").bool(false)       // rx df, tx df, dx grid, tx watchdog
                .str("").bool(false).bytes();                      // sub-mode, fast mode
    }

    static byte[] decode(int msOfDay, int snr, double dt, int df, String message) {
        return WsjtxPackets.of(WsjtxProtocolDecoder.TYPE_DECODE, "WSJT-X")
                .bool(true).i32(msOfDay).i32(snr).f64(dt).i32(df).str("~").str(message).bool(true).bool(false).bytes();
    }

    @Test
    void statusCarriesDialModeAndBand() {
        WsjtxStatus s = dec.decode(status(14_074_000L, "FT8", "K1ABC")).status;
        assertEquals(14_074_000L, s.getDialFrequency());
        assertEquals("FT8", s.getMode());
        assertEquals("K1ABC", s.getDxCall());
        assertEquals("FN42", s.getDxGrid());
        assertEquals(1234, s.getTxDf());
        assertTrue(s.isTxEnabled());
        assertFalse(s.isTransmitting());
        assertEquals(BandUtils.frequencyToBand(14_074_000L), s.getBand());
        assertNotEquals("OOB", s.getBand(), "14.074 MHz is in the 20 m band");
    }

    @Test
    void decodeCarriesSnrTimingAndText() {
        WsjtxDecode d = dec.decode(decode(12 * 3_600_000 + 15_000, -14, 0.3, 1520, "CQ K1ABC FN42")).decode;
        assertEquals(-14, d.getSnr());
        assertEquals(0.3, d.getDeltaTime(), 1e-9);
        assertEquals(1520, d.getDeltaFrequency());
        assertEquals("CQ K1ABC FN42", d.getMessage());
        assertTrue(d.isLowConfidence());
        assertFalse(d.isOffAir());
        assertEquals(12 * 3600 + 15, d.getTimestamp().getEpochSecond() % 86_400, "time of day from the packet");
    }

    @Test
    void loggedQsoCarriesEveryField() {
        Instant on = Instant.parse("2026-11-17T21:02:15Z"), off = Instant.parse("2026-11-17T21:03:45Z");
        byte[] p = WsjtxPackets.of(WsjtxProtocolDecoder.TYPE_QSO_LOGGED, "WSJT-X")
                .time(off).str("JA1XYZ").str("PM95").i64(7_074_000L).str("FT8").str("-10").str("-08").str("100")
                .str("tnx").str("Taro").time(on).str("WM3J").str("WM3J").str("FM19").str("").str("").str("").bytes();
        WsjtxQsoLogged q = dec.decode(p).qsoLogged;
        assertEquals("JA1XYZ", q.getDxCall());
        assertEquals("PM95", q.getDxGrid());
        assertEquals(off, q.getTimeOff());
        assertEquals(on, q.getTimeOn());
        assertEquals("-10", q.getRstSent());
        assertEquals("-08", q.getRstReceived());
        assertEquals("Taro", q.getName());
        assertEquals("FM19", q.getMyGrid());
        assertNull(q.getExchangeSent(), "empty strings come through as null");
        assertEquals(BandUtils.frequencyToBand(7_074_000L), q.getBand());
    }

    @Test
    void clearAndCloseAreRecognised() {
        assertEquals(WsjtxProtocolDecoder.TYPE_CLEAR, dec.decode(WsjtxPackets.of(WsjtxProtocolDecoder.TYPE_CLEAR, "WSJT-X").bytes()).messageType);
        assertEquals(WsjtxProtocolDecoder.TYPE_CLOSE, dec.decode(WsjtxPackets.of(WsjtxProtocolDecoder.TYPE_CLOSE, "JTDX").bytes()).messageType);
    }

    @Test
    void truncatedOrUnknownPacketsAreDroppedNotThrown() {
        byte[] full = status(14_074_000L, "FT8", "K1ABC");
        assertNull(dec.decode(Arrays.copyOf(full, 30)), "cut off mid-field");
        assertNull(dec.decode(new byte[5]));
        assertNull(dec.decode(WsjtxPackets.of(WsjtxProtocolDecoder.TYPE_REPLAY, "WSJT-X").bytes()), "a type J-Bridge does not handle");
    }

    @Test
    void outOfBandFrequencyIsMarked() {
        assertEquals("OOB", BandUtils.frequencyToBand(1_000L));
    }

    @Test
    void listenerDeliversPacketsToItsCallbacks() throws Exception {
        int port;
        try (DatagramSocket probe = new DatagramSocket(0, InetAddress.getLoopbackAddress())) { port = probe.getLocalPort(); }
        WsjtxUdpListener l = new WsjtxUdpListener(port, "127.0.0.1");
        CountDownLatch got = new CountDownLatch(3);
        AtomicReference<String> msg = new AtomicReference<>(), app = new AtomicReference<>();
        AtomicReference<Boolean> closed = new AtomicReference<>();
        l.setOnDecode(d -> { msg.set(d.getMessage()); got.countDown(); });
        l.setOnConnectionChange((connected, src, ver) -> {
            if (connected) { app.set(src + " " + ver); } else { closed.set(true); }
            got.countDown();
        });
        l.start();
        try (DatagramSocket tx = new DatagramSocket()) {
            byte[] hb = WsjtxPackets.of(WsjtxProtocolDecoder.TYPE_HEARTBEAT, "WSJT-X").i32(3).str("2.7.0").str("").bytes();
            byte[][] packets = {hb, decode(1000, -5, 0.1, 800, "K1ABC WM3J FM19"),
                    WsjtxPackets.of(WsjtxProtocolDecoder.TYPE_CLOSE, "WSJT-X").bytes()};
            // the listener binds on its own thread: resend until all three arrive (UDP to a port not yet open is lost)
            for (int tries = 0; tries < 50 && got.getCount() > 0; tries++) {
                if (got.getCount() == 3) for (byte[] p : packets) tx.send(new DatagramPacket(p, p.length, InetAddress.getLoopbackAddress(), port));
                got.await(100, TimeUnit.MILLISECONDS);
            }
        } finally {
            l.stop();
        }
        assertEquals(0, got.getCount(), "heartbeat, decode and close all arrived");
        assertEquals("K1ABC WM3J FM19", msg.get());
        assertEquals("WSJT-X 2.7.0", app.get());
        assertTrue(closed.get());
    }
}
