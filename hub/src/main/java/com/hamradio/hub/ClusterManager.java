package com.hamradio.hub;

import com.hamradio.hub.model.HubConfig;
import com.hamradio.hub.model.Spot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ClusterManager — owns the single DX cluster telnet connection.
 *
 * Responsibilities:
 *   • Connect to configurable cluster server:port
 *   • Login with station callsign
 *   • Parse DX spot lines into Spot objects
 *   • Enrich spots via SpotEnricher (DXCC, bearing, distance, local time)
 *   • Apply band and mode filters
 *   • Publish enriched spots through MessageRouter
 *   • Reconnect automatically on disconnect with exponential back-off
 *
 * Spot line format (standard DX Spider / AR-cluster):
 *   DX de W3ABC:     14225.0  DX0ABC       599 both ways                2026-04-07 1200Z
 */
public class ClusterManager {

    private static final Logger log = LoggerFactory.getLogger(ClusterManager.class);

    // Singleton
    private static final ClusterManager INSTANCE = new ClusterManager();
    public static ClusterManager getInstance() { return INSTANCE; }
    private ClusterManager() {}

    // Regex pattern for standard DX spot lines
    // DX de <spotter>:    <freq>   <spotted>    <comment>          <time>
    private static final Pattern SPOT_PATTERN = Pattern.compile(
        "DX de\\s+([A-Z0-9/\\-]+)[:\\s]+([\\d.]+)\\s+([A-Z0-9/\\-]+)\\s+(.*?)\\s+(\\d{4}Z?)\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private MessageRouter router;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final AtomicBoolean running    = new AtomicBoolean(false);
    private final AtomicBoolean connected  = new AtomicBoolean(false);

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cluster-reconnect");
            t.setDaemon(true);
            return t;
        });

    private final ExecutorService readerThread =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "cluster-reader");
            t.setDaemon(true);
            return t;
        });

    // Reconnect backoff state
    private int backoffSeconds = 5;
    private static final int MAX_BACKOFF = 300; // 5 minutes

    public void setRouter(MessageRouter router) { this.router = router; }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Initiate the cluster connection (non-blocking — actual I/O runs on daemon threads).
     */
    public void connect() {
        if (running.compareAndSet(false, true)) {
            scheduleConnect(0);
        }
    }

    /**
     * Soft reconnect after a config change — closes the current socket so the
     * read loop reconnects with the new settings. Does NOT shut down executors.
     */
    public void reconnect() {
        backoffSeconds = 5;
        closeSocket();
        connected.set(false);
        if (!running.get()) {
            // Was never started — start now
            connect();
        } else {
            // readLoop's finally will handle reconnect; also schedule here in
            // case we were between retries with no active readLoop.
            scheduleConnect(1);
        }
    }

    /**
     * Soft disconnect — closes the current socket and stops reconnect attempts,
     * but leaves the executor threads alive so Connect can be used again.
     * Called from the web UI disconnect button.
     */
    public void softDisconnect() {
        running.set(false);
        connected.set(false);
        closeSocket();
        log.info("Cluster disconnected (executors kept alive — reconnect available)");
    }

    /**
     * Permanently disconnect and stop reconnect attempts (shutdown hook only).
     */
    public void disconnect() {
        running.set(false);
        closeSocket();
        scheduler.shutdownNow();
        readerThread.shutdownNow();
        log.info("ClusterManager disconnected and stopped");
    }

    public boolean isConnected() { return connected.get(); }

    // ---------------------------------------------------------------
    // Connection lifecycle
    // ---------------------------------------------------------------

    private void scheduleConnect(int delaySeconds) {
        if (!running.get()) return;
        scheduler.schedule(this::doConnect, delaySeconds, TimeUnit.SECONDS);
    }

    private void doConnect() {
        if (!running.get()) return;
        if (connected.get()) return; // already connected, skip duplicate schedule

        HubConfig.ClusterSection cfg = ConfigManager.getInstance().getCluster();
        if (cfg == null || cfg.server == null || cfg.server.isBlank()) {
            log.warn("Cluster server not configured — skipping connection");
            return;
        }

        log.info("Connecting to DX cluster {}:{} …", cfg.server, cfg.port);
        try {
            socket = new Socket(cfg.server, cfg.port);
            socket.setSoTimeout(120_000); // 2-minute read timeout
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            connected.set(true);
            backoffSeconds = 5; // reset backoff on successful connect
            log.info("Connected to cluster {}:{}", cfg.server, cfg.port);

            // Login
            String loginCall = cfg.loginCallsign != null ? cfg.loginCallsign :
                               ConfigManager.getInstance().getStation().callsign;
            writer.println(loginCall);

            // Start reading on daemon thread
            readerThread.submit(this::readLoop);

        } catch (Exception e) {
            connected.set(false);
            log.warn("Cluster connection failed: {} — retrying in {}s", e.getMessage(), backoffSeconds);
            scheduleConnect(backoffSeconds);
            backoffSeconds = Math.min(backoffSeconds * 2, MAX_BACKOFF);
        }
    }

    private void readLoop() {
        try {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                processLine(line.trim());
            }
        } catch (IOException e) {
            if (running.get()) {
                log.warn("Cluster read error: {} — reconnecting in {}s", e.getMessage(), backoffSeconds);
            }
        } finally {
            connected.set(false);
            closeSocket();
            if (running.get()) {
                scheduleConnect(backoffSeconds);
                backoffSeconds = Math.min(backoffSeconds * 2, MAX_BACKOFF);
            }
        }
    }

    private void closeSocket() {
        try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        try { if (writer != null) writer.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------
    // Line parsing
    // ---------------------------------------------------------------

    private void processLine(String line) {
        if (line.isEmpty()) return;

        // Broadcast every raw line so the web UI telnet feed can display it
        if (router != null) {
            router.publishRawLine(line);
        }

        Matcher m = SPOT_PATTERN.matcher(line);
        if (!m.find()) {
            // Not a spot line (could be login prompt, announcements, etc.)
            log.trace("Cluster non-spot: {}", line);
            return;
        }

        try {
            Spot spot = parseSpot(m);
            if (spot == null) return;

            // Apply filters
            if (!passesFilter(spot)) {
                log.trace("Spot filtered: {} on {}", spot.spotted, spot.band);
                return;
            }

            // Enrich
            SpotEnricher.getInstance().enrich(spot);

            // Publish to all apps
            if (router != null) {
                router.publishSpot(spot);
            }
            log.debug("Spot: {} {} {}Hz", spot.spotter, spot.spotted, spot.frequency);

        } catch (Exception e) {
            log.warn("Failed to parse spot line '{}': {}", line, e.getMessage());
        }
    }

    private Spot parseSpot(Matcher m) {
        Spot spot = new Spot();
        spot.type     = "SPOT";
        spot.spotter  = m.group(1).toUpperCase();
        spot.spotted  = m.group(3).toUpperCase();
        spot.comment  = m.group(4).trim();
        spot.timestamp = Instant.now().toString();

        // Frequency in kHz → Hz
        try {
            double freqKhz = Double.parseDouble(m.group(2));
            spot.frequency = (long)(freqKhz * 1000);
            spot.band      = freqToBand(spot.frequency);
        } catch (NumberFormatException e) {
            return null; // malformed frequency
        }

        // Attempt to determine mode from comment or frequency
        spot.mode = detectMode(spot.comment, spot.frequency);

        return spot;
    }

    // ---------------------------------------------------------------
    // Band and mode helpers
    // ---------------------------------------------------------------

    private String freqToBand(long hz) {
        long khz = hz / 1000;
        if (khz >= 1800  && khz <= 2000)  return "160m";
        if (khz >= 3500  && khz <= 4000)  return "80m";
        if (khz >= 7000  && khz <= 7300)  return "40m";
        if (khz >= 10100 && khz <= 10150) return "30m";
        if (khz >= 14000 && khz <= 14350) return "20m";
        if (khz >= 18068 && khz <= 18168) return "17m";
        if (khz >= 21000 && khz <= 21450) return "15m";
        if (khz >= 24890 && khz <= 24990) return "12m";
        if (khz >= 28000 && khz <= 29700) return "10m";
        if (khz >= 50000 && khz <= 54000) return "6m";
        return "OOB";
    }

    private String detectMode(String comment, long freqHz) {
        if (comment == null) return "SSB";
        String uc = comment.toUpperCase();
        if (uc.contains("FT8"))  return "FT8";
        if (uc.contains("FT4"))  return "FT4";
        if (uc.contains("RTTY")) return "RTTY";
        if (uc.contains("PSK"))  return "PSK31";
        if (uc.contains("JS8"))  return "JS8";
        if (uc.contains("CW"))   return "CW";
        if (uc.contains("SSB") || uc.contains("LSB") || uc.contains("USB")) return "SSB";
        // Frequency-based guesses
        long khz = freqHz / 1000;
        if (khz >= 14074 && khz <= 14075) return "FT8";
        if (khz >= 7074  && khz <= 7075)  return "FT8";
        if (khz >= 21074 && khz <= 21075) return "FT8";
        return "SSB"; // default
    }

    private boolean passesFilter(Spot spot) {
        HubConfig.ClusterSection cfg = ConfigManager.getInstance().getCluster();
        if (cfg.filters == null) return true;

        // Band filter
        Set<String> bands = cfg.filters.bands;
        if (bands != null && !bands.isEmpty() && !bands.contains(spot.band)) {
            return false;
        }

        // Mode filter
        Set<String> modes = cfg.filters.modes;
        if (modes != null && !modes.isEmpty() && !modes.contains(spot.mode)) {
            return false;
        }

        return true;
    }
}
