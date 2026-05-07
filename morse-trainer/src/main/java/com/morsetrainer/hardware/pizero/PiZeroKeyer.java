package com.morsetrainer.hardware.pizero;

import com.morsetrainer.core.AppConfig;
import com.morsetrainer.core.Logger;
import com.morsetrainer.decoder.KeyEvent;
import com.morsetrainer.hardware.KeyEventSource;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wireless keyer source — listens on a UDP port for packets sent by a
 * Raspberry Pi Zero W (or any other Wi-Fi keyer).
 *
 * Packet format (text, one event per packet, terminated by newline optional):
 *   Straight key:    DOWN|<piMillis>|<batteryPct?>
 *                    UP|<piMillis>|<batteryPct?>
 *   Iambic paddle:   ELEM|DIT|<piMillis>|<batteryPct?>
 *                    ELEM|DAH|<piMillis>|<batteryPct?>
 *   Heartbeat:       PING|<piMillis>|<batteryPct?>   (no event, battery only)
 *
 * For BLE deployments, a separate adapter (e.g. using Bluez via JNI, or a
 * gattlib-based bridge) can call {@code injectEvent} directly with parsed
 * KeyEvents. The class supports both transports through that hook.
 */
public class PiZeroKeyer implements KeyEventSource {

    private final int port;
    private DatagramSocket socket;
    private Thread reader;
    private volatile boolean running = false;
    private Consumer<KeyEvent> sink;
    private volatile int lastBatteryPct = -1;

    public PiZeroKeyer() { this(AppConfig.get().pizeroUdpPort); }
    public PiZeroKeyer(int port) { this.port = port; }

    @Override
    public void start(Consumer<KeyEvent> consumer) {
        if (running) return;
        this.sink = consumer;
        try {
            socket = new DatagramSocket(port);
            running = true;
            reader = new Thread(this::readLoop, "pizero-keyer");
            reader.setDaemon(true);
            reader.start();
            Logger.info("Pi Zero keyer listening on UDP %d", port);
        } catch (Exception e) {
            Logger.warn("Pi Zero keyer: cannot bind UDP %d: %s", port, e.getMessage());
        }
    }

    private void readLoop() {
        byte[] buf = new byte[256];
        while (running) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);
                String msg = new String(pkt.getData(), 0, pkt.getLength(), StandardCharsets.UTF_8).trim();
                for (KeyEvent ev : parse(msg)) {
                    if (sink != null) sink.accept(ev);
                }
            } catch (Exception e) {
                if (running) Logger.warn("Pi Zero read error: %s", e.getMessage());
            }
        }
    }

    /** Allow BLE / external bridges to push events directly. */
    public void injectEvent(KeyEvent ev) {
        if (sink != null) sink.accept(ev);
    }

    /**
     * Parse one UDP datagram body into 0..N KeyEvents. Battery field, if
     * present, updates {@link #lastBatteryPct} as a side-effect.
     */
    List<KeyEvent> parse(String msg) {
        if (msg.isEmpty()) return Collections.emptyList();
        String[] parts = msg.split("\\|");
        if (parts.length < 1) return Collections.emptyList();

        switch (parts[0]) {
            case "DOWN": {
                long t = (parts.length >= 2) ? safeLong(parts[1]) : System.currentTimeMillis();
                if (parts.length >= 3) lastBatteryPct = (int) safeLong(parts[2]);
                return List.of(KeyEvent.down(t));
            }
            case "UP": {
                long t = (parts.length >= 2) ? safeLong(parts[1]) : System.currentTimeMillis();
                if (parts.length >= 3) lastBatteryPct = (int) safeLong(parts[2]);
                return List.of(KeyEvent.up(t));
            }
            case "ELEM": {
                if (parts.length < 3) return Collections.emptyList();
                long t = safeLong(parts[2]);
                if (parts.length >= 4) lastBatteryPct = (int) safeLong(parts[3]);
                int wpm = AppConfig.get().wpm;
                int ditMs = Math.max(1, 1200 / Math.max(1, wpm));
                int durMs = "DAH".equals(parts[1]) ? ditMs * 3 : ditMs;
                return List.of(KeyEvent.down(t), KeyEvent.up(t + durMs));
            }
            case "PING": {
                if (parts.length >= 3) lastBatteryPct = (int) safeLong(parts[2]);
                return Collections.emptyList();
            }
            default:
                return Collections.emptyList();
        }
    }

    private static long safeLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return System.currentTimeMillis(); }
    }

    public int lastBatteryPct() { return lastBatteryPct; }

    @Override
    public void stop() {
        running = false;
        if (socket != null) socket.close();
        if (reader != null) reader.interrupt();
    }

    @Override public String name() { return "Pi Zero UDP:" + port; }
    @Override public boolean isConnected() { return socket != null && !socket.isClosed(); }
}
