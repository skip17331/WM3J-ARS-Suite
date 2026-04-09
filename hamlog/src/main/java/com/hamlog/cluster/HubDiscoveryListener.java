package com.hamlog.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Listens for UDP broadcast beacons from ham-radio-hub on port 9999.
 *
 * When a HUB_BEACON packet arrives, calls the provided callback with the
 * WebSocket URL (e.g. "ws://192.168.1.5:8080") so the caller can connect.
 * Only fires the callback when not already connected.
 */
public class HubDiscoveryListener {

    private static final Logger log = LoggerFactory.getLogger(HubDiscoveryListener.class);
    private static final int DISCOVERY_PORT = 9999;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "hub-discovery-listener");
        t.setDaemon(true);
        return t;
    });

    private DatagramSocket socket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Consumer<String> onHubFound; // called with ws URL

    public void setOnHubFound(Consumer<String> callback) {
        this.onHubFound = callback;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        executor.submit(this::listenLoop);
        log.info("Listening for hub beacons on UDP port {}", DISCOVERY_PORT);
    }

    public void stop() {
        running.set(false);
        if (socket != null) socket.close();
        executor.shutdownNow();
    }

    private void listenLoop() {
        try {
            socket = new DatagramSocket(DISCOVERY_PORT);
            socket.setSoTimeout(2000); // 2s timeout so we can check running flag
            byte[] buf = new byte[512];

            while (running.get()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String json = new String(packet.getData(), 0, packet.getLength());
                    handleBeacon(json, packet.getAddress().getHostAddress());
                } catch (java.net.SocketTimeoutException ignored) {
                    // normal — loop and check running flag
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                log.warn("Discovery listener error: {}", e.getMessage());
                // Retry after a delay
                executor.schedule(this::listenLoop, 10, TimeUnit.SECONDS);
            }
        }
    }

    private void handleBeacon(String json, String senderIp) {
        try {
            JsonNode node = MAPPER.readTree(json);
            if (!"HUB_BEACON".equals(node.path("type").asText())) return;

            int wsPort = node.path("wsPort").asInt(8080);
            String wsUrl = "ws://" + senderIp + ":" + wsPort;

            log.debug("Hub beacon received from {}: {}", senderIp, json);

            if (onHubFound != null) {
                onHubFound.accept(wsUrl);
            }
        } catch (Exception e) {
            log.warn("Failed to parse hub beacon: {}", json);
        }
    }
}
