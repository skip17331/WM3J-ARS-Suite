package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig;
import com.hamradio.jhub.model.Spot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RbnClient — parallel telnet client for the Reverse Beacon Network
 * (telnet.reversebeacon.net:7000).
 *
 * RBN spots are skimmer-decoded, not operator-spotted, and arrive in a
 * format that's almost identical to a standard cluster spot but with
 * extra fields after the spotted callsign:
 *
 *   DX de KM3T-2:    14025.5  K1ABC          CW    21 dB  22 WPM  CQ      1234Z
 *
 * We parse the same way as {@link ClusterManager} but capture SNR and
 * WPM into the optional Spot.snrDb / Spot.wpm fields so the UI can show
 * skimmer info alongside the regular comment text.
 *
 * Spots that pass the SNR floor and band/mode filter are published via
 * {@link MessageRouter#publishSpot(Spot)} with {@code source = "RBN"}
 * — same broadcast path as cluster spots, so J-Map and J-Log pick them
 * up with no further wiring.
 */
public class RbnClient {

    private static final Logger log = LoggerFactory.getLogger(RbnClient.class);

    private static final RbnClient INSTANCE = new RbnClient();
    public static RbnClient getInstance() { return INSTANCE; }
    private RbnClient() {}

    // RBN format: "DX de <spotter>:    <freq>   <spotted>   <mode>   <snr> dB   <wpm> WPM   <comment>   <time>Z"
    // The mode word and SNR/WPM blocks are positional but optional for non-CW spots
    // (FT8 lines from the FT8 skimmer omit the WPM field, etc.). We use a forgiving
    // regex that captures spotter / freq / spotted as required, then optional
    // mode, snr, wpm, comment, time.
    private static final Pattern RBN_PATTERN = Pattern.compile(
        "DX de\\s+([A-Z0-9/\\-]+)[:\\s]+([\\d.]+)\\s+([A-Z0-9/\\-]+)" +
        "(?:\\s+([A-Z0-9]+))?" +                          // mode
        "(?:\\s+(-?\\d+)\\s*dB)?" +                       // SNR
        "(?:\\s+(\\d+)\\s*WPM)?" +                        // WPM (CW only)
        "(?:\\s+(.*?))?" +                                // comment
        "\\s+(\\d{4})Z?\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private MessageRouter router;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final AtomicBoolean running   = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rbn-reconnect"); t.setDaemon(true); return t;
        });
    private final ExecutorService readerThread =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rbn-reader"); t.setDaemon(true); return t;
        });

    private int backoffSeconds = 10;
    private static final int MAX_BACKOFF = 600;

    public void setRouter(MessageRouter router) { this.router = router; }

    public boolean isConnected() { return connected.get(); }
    public boolean isRunning()   { return running.get(); }

    public void start() {
        JHubConfig.RbnSection cfg = ConfigManager.getInstance().getConfig().rbn;
        if (cfg == null || !cfg.enabled) {
            log.info("RBN feed disabled in config — not starting");
            return;
        }
        if (running.compareAndSet(false, true)) {
            scheduleConnect(0);
        }
    }

    public void restart() {
        backoffSeconds = 10;
        running.set(false);
        connected.set(false);
        closeSocket();
        start();
    }

    public synchronized void stop() {
        running.set(false);
        connected.set(false);
        closeSocket();
    }

    private void scheduleConnect(int delaySec) {
        if (!running.get()) return;
        scheduler.schedule(this::doConnect, delaySec, TimeUnit.SECONDS);
    }

    private void doConnect() {
        if (!running.get() || connected.get()) return;
        JHubConfig.RbnSection cfg = ConfigManager.getInstance().getConfig().rbn;
        if (cfg == null || !cfg.enabled) {
            running.set(false);
            return;
        }

        log.info("Connecting to RBN {}:{} …", cfg.server, cfg.port);
        try {
            socket = new Socket(cfg.server, cfg.port);
            socket.setSoTimeout(180_000); // 3-minute idle timeout — RBN is busy
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            connected.set(true);
            backoffSeconds = 10;
            log.info("RBN connected");

            String loginCall = (cfg.loginCallsign != null && !cfg.loginCallsign.isBlank())
                ? cfg.loginCallsign
                : ConfigManager.getInstance().getStation().callsign;
            writer.println(loginCall);
            // Default RBN behavior is to send everything; users who want filtering
            // can change it via /api/rbn or the cluster tab. We do the band/mode
            // filtering client-side so RBN's own filter language stays untouched.

            readerThread.submit(this::readLoop);
        } catch (Exception e) {
            connected.set(false);
            log.warn("RBN connect failed: {} — retrying in {}s", e.getMessage(), backoffSeconds);
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
            if (running.get()) log.warn("RBN read error: {} — reconnecting in {}s", e.getMessage(), backoffSeconds);
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

    private void processLine(String line) {
        if (line.isEmpty()) return;
        Matcher m = RBN_PATTERN.matcher(line);
        if (!m.find()) {
            log.trace("RBN non-spot: {}", line);
            return;
        }
        try {
            Spot spot = new Spot();
            spot.type      = "SPOT";
            spot.source    = "RBN";
            spot.spotter   = m.group(1).toUpperCase();
            spot.spotted   = m.group(3).toUpperCase();
            spot.timestamp = Instant.now().toString();

            try {
                double freqKhz = Double.parseDouble(m.group(2));
                spot.frequency = (long)(freqKhz * 1000);
                spot.band      = freqToBand(spot.frequency);
            } catch (NumberFormatException e) { return; }

            String modeWord = m.group(4);
            if (modeWord != null && !modeWord.isBlank()) spot.mode = modeWord.toUpperCase();

            String snrStr = m.group(5);
            if (snrStr != null) {
                try { spot.snrDb = Integer.parseInt(snrStr); } catch (NumberFormatException ignored) {}
            }
            String wpmStr = m.group(6);
            if (wpmStr != null) {
                try { spot.wpm = Integer.parseInt(wpmStr); } catch (NumberFormatException ignored) {}
            }
            spot.comment = (m.group(7) != null ? m.group(7).trim() : "");

            // Apply RBN-specific filters: SNR floor + band/mode
            JHubConfig.RbnSection cfg = ConfigManager.getInstance().getConfig().rbn;
            if (spot.snrDb != null && spot.snrDb < cfg.minSnrDb) return;
            if (cfg.bands != null && !cfg.bands.isEmpty() && spot.band != null && !cfg.bands.contains(spot.band)) return;
            if (cfg.modes != null && !cfg.modes.isEmpty() && spot.mode != null && !cfg.modes.contains(spot.mode)) return;

            SpotEnricher.getInstance().enrich(spot);
            if (router != null) router.publishSpot(spot);
            log.debug("RBN spot: {} {} @ {}kHz  {}dB  {}WPM",
                spot.spotter, spot.spotted, spot.frequency / 1000, spot.snrDb, spot.wpm);
        } catch (Exception e) {
            log.debug("RBN parse error '{}': {}", line, e.getMessage());
        }
    }

    /** Same band map ClusterManager uses — kept local to avoid coupling. */
    private static String freqToBand(long hz) {
        long khz = hz / 1000;
        if (khz >=   1800 && khz <=   2000) return "160m";
        if (khz >=   3500 && khz <=   4000) return "80m";
        if (khz >=   5300 && khz <=   5500) return "60m";
        if (khz >=   7000 && khz <=   7300) return "40m";
        if (khz >=  10100 && khz <=  10150) return "30m";
        if (khz >=  14000 && khz <=  14350) return "20m";
        if (khz >=  18068 && khz <=  18168) return "17m";
        if (khz >=  21000 && khz <=  21450) return "15m";
        if (khz >=  24890 && khz <=  24990) return "12m";
        if (khz >=  28000 && khz <=  29700) return "10m";
        if (khz >=  50000 && khz <=  54000) return "6m";
        if (khz >= 144000 && khz <= 148000) return "2m";
        if (khz >= 222000 && khz <= 225000) return "1.25m";
        if (khz >= 420000 && khz <= 450000) return "70cm";
        return "unknown";
    }
}
