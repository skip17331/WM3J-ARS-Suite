package com.hamradio.digitalbridge;

import com.hamradio.digitalbridge.model.WsjtxDecode;
import com.hamradio.digitalbridge.model.WsjtxQsoLogged;
import com.hamradio.digitalbridge.model.WsjtxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * WsjtxUdpListener — listens on a UDP port for WSJT-X broadcast datagrams.
 *
 * Runs on a dedicated daemon thread.  If WSJT-X is not running the listener
 * simply blocks; no errors are thrown.  When WSJT-X starts, packets will
 * automatically begin arriving.
 *
 * All callbacks are invoked on the listener thread.  Callers must wrap
 * any JavaFX node mutations in Platform.runLater().
 */
public class WsjtxUdpListener {

    private static final Logger log = LoggerFactory.getLogger(WsjtxUdpListener.class);

    private static final int BUFFER_SIZE    = 4096;
    private static final int RETRY_DELAY_MS = 5_000;

    private final int    port;
    private final String bindAddress;

    private volatile boolean running = false;
    private DatagramSocket  socket;

    private final WsjtxProtocolDecoder decoder = new WsjtxProtocolDecoder();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "wsjtx-udp-listener");
        t.setDaemon(true);
        return t;
    });

    // ── Callbacks ─────────────────────────────────────────────────────────────

    private Consumer<WsjtxDecode>          onDecode;
    private Consumer<WsjtxStatus>          onStatus;
    private Consumer<WsjtxQsoLogged>       onQsoLogged;
    private BiConsumer<Boolean, String>    onConnectionChange; // (connected, version)
    private Runnable                       onClear;

    public WsjtxUdpListener(int port, String bindAddress) {
        this.port        = port;
        this.bindAddress = bindAddress;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    public void start() {
        if (running) return;
        running = true;
        executor.submit(this::listenLoop);
        log.info("WSJT-X UDP listener starting on {}:{}", bindAddress, port);
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) socket.close();
        executor.shutdown();
        log.info("WSJT-X UDP listener stopped");
    }

    public boolean isRunning() { return running; }
    public int     getPort()   { return port; }

    // ── Internal loop ─────────────────────────────────────────────────────────

    private void listenLoop() {
        while (running) {
            try {
                InetAddress addr = InetAddress.getByName(bindAddress);
                socket = new DatagramSocket(port, addr);
                socket.setSoTimeout(0); // block indefinitely

                log.info("UDP listener ready on port {}", port);
                byte[]         buf    = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                while (running) {
                    socket.receive(packet);
                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                    dispatch(data);
                    packet.setLength(buf.length); // reset for next receive
                }

            } catch (Exception e) {
                if (running) {
                    log.warn("UDP error: {} — retrying in {}ms", e.getMessage(), RETRY_DELAY_MS);
                    sleep(RETRY_DELAY_MS);
                }
            } finally {
                if (socket != null && !socket.isClosed()) socket.close();
            }
        }
    }

    private void dispatch(byte[] data) {
        WsjtxProtocolDecoder.DecodedMessage msg = decoder.decode(data);
        if (msg == null) return;

        switch (msg.messageType) {
            case WsjtxProtocolDecoder.TYPE_HEARTBEAT:
                log.debug("WSJT-X heartbeat v{}", msg.heartbeat != null ? msg.heartbeat.version : "?");
                if (onConnectionChange != null)
                    onConnectionChange.accept(true, msg.heartbeat != null ? msg.heartbeat.version : "");
                break;

            case WsjtxProtocolDecoder.TYPE_STATUS:
                if (msg.status != null && onStatus != null) onStatus.accept(msg.status);
                break;

            case WsjtxProtocolDecoder.TYPE_DECODE:
                if (msg.decode != null && onDecode != null) onDecode.accept(msg.decode);
                break;

            case WsjtxProtocolDecoder.TYPE_CLEAR:
                if (onClear != null) onClear.run();
                break;

            case WsjtxProtocolDecoder.TYPE_QSO_LOGGED:
                if (msg.qsoLogged != null && onQsoLogged != null) onQsoLogged.accept(msg.qsoLogged);
                break;

            case WsjtxProtocolDecoder.TYPE_CLOSE:
                log.info("WSJT-X sent Close");
                if (onConnectionChange != null) onConnectionChange.accept(false, null);
                break;

            default:
                log.trace("Ignored WSJT-X type {}", msg.messageType);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ── Callback setters ──────────────────────────────────────────────────────

    public void setOnDecode(Consumer<WsjtxDecode> cb)               { this.onDecode = cb; }
    public void setOnStatus(Consumer<WsjtxStatus> cb)               { this.onStatus = cb; }
    public void setOnQsoLogged(Consumer<WsjtxQsoLogged> cb)         { this.onQsoLogged = cb; }
    public void setOnConnectionChange(BiConsumer<Boolean,String> cb){ this.onConnectionChange = cb; }
    public void setOnClear(Runnable cb)                             { this.onClear = cb; }
}
