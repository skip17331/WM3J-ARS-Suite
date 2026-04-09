package com.hamradio.hub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HubDiscovery — broadcasts a UDP beacon every 5 seconds so that Ham Radio
 * apps (e.g. HamLog) can auto-discover the hub without manual configuration.
 *
 * Beacon format (JSON, sent to 255.255.255.255:9999):
 *   {"type":"HUB_BEACON","wsPort":8080,"webPort":8081,"callsign":"W3ABC"}
 *
 * Receiving apps should connect to ws://[source-ip]:[wsPort] and send
 * APP_CONNECTED to register.
 */
public class HubDiscovery {

    private static final Logger log = LoggerFactory.getLogger(HubDiscovery.class);

    /** UDP port apps listen on for discovery beacons. */
    public static final int DISCOVERY_PORT = 9999;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "hub-discovery");
        t.setDaemon(true);
        return t;
    });

    private DatagramSocket udpSocket;

    public void start() {
        try {
            udpSocket = new DatagramSocket(0); // ephemeral port — sender doesn't need 9999
            udpSocket.setBroadcast(true);
            scheduler.scheduleAtFixedRate(this::sendBeacon, 0, 5, TimeUnit.SECONDS);
            log.info("Hub discovery beacon started on UDP port {}", DISCOVERY_PORT);
        } catch (Exception e) {
            log.warn("Could not start discovery beacon: {}", e.getMessage());
        }
    }

    public void stop() {
        scheduler.shutdownNow();
        if (udpSocket != null) udpSocket.close();
    }

    private void sendBeacon() {
        try {
            var cfg = ConfigManager.getInstance().getConfig();
            if (cfg == null) return;

            String callsign = cfg.station != null ? cfg.station.callsign : "NOCALL";
            String json = String.format(
                "{\"type\":\"HUB_BEACON\",\"wsPort\":%d,\"webPort\":%d,\"callsign\":\"%s\"}",
                cfg.hub.websocketPort, cfg.hub.webConfigPort, callsign
            );

            byte[] data = json.getBytes();
            DatagramPacket packet = new DatagramPacket(
                data, data.length,
                InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT
            );
            udpSocket.send(packet);
        } catch (Exception e) {
            log.trace("Beacon send failed: {}", e.getMessage());
        }
    }
}
