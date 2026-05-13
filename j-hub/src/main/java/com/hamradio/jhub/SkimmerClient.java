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
 * SkimmerClient — parallel telnet client for a local CW Skimmer Server
 * (or any AR-Cluster-format source running on the operator's LAN).
 *
 * VE7CC's CW Skimmer Server, RTTY Skimmer Server, and similar SDR-fed
 * decoders all emit spot lines in the same format the public RBN
 * backbone uses. The only thing that varies in practice is which server
 * the spots come from (a local PC vs telnet.reversebeacon.net). Re-using
 * the RBN line parser lets us treat a local skimmer as just "RBN, but
 * tagged differently" — the spots flow through the same broadcast path
 * to every connected module, and downstream consumers (J-Map's spot
 * colouring, J-Log's spot pane) can render `source = "SKIMMER"` spots
 * distinctly from upstream RBN if they choose to.
 *
 * Why bother when RBN is free?
 * <ul>
 *   <li>You can run your own skimmer on bands or modes the public RBN
 *       doesn't cover well (60m, 1.25m, niche digital modes).</li>
 *   <li>Latency drops from seconds to milliseconds for spots on signals
 *       you can hear locally — useful for chasing.</li>
 *   <li>You become eligible to feed the public RBN backbone if you want
 *       to — the same line format you ingest is the format you'd emit.</li>
 * </ul>
 */
public class SkimmerClient {

    private static final Logger log = LoggerFactory.getLogger(SkimmerClient.class);

    private static final SkimmerClient INSTANCE = new SkimmerClient();
    public static SkimmerClient getInstance() { return INSTANCE; }
    private SkimmerClient() {}

    // Same line format as RBN — see RbnClient.RBN_PATTERN. Duplicated rather
    // than shared so the two ingest paths can evolve independently if a
    // skimmer flavour starts adding fields that public RBN doesn't.
    private static final Pattern SKIMMER_PATTERN = Pattern.compile(
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
            Thread t = new Thread(r, "skimmer-reconnect"); t.setDaemon(true); return t;
        });
    private final ExecutorService readerThread =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "skimmer-reader"); t.setDaemon(true); return t;
        });

    private int backoffSeconds = 5;
    private static final int MAX_BACKOFF = 300; // shorter than RBN — local server should come back fast

    public void setRouter(MessageRouter router) { this.router = router; }

    public boolean isConnected() { return connected.get(); }
    public boolean isRunning()   { return running.get(); }

    public void start() {
        JHubConfig.SkimmerSection cfg = ConfigManager.getInstance().getConfig().skimmer;
        if (cfg == null || !cfg.enabled) {
            log.info("Skimmer feed disabled in config — not starting");
            return;
        }
        if (running.compareAndSet(false, true)) {
            scheduleConnect(0);
        }
    }

    public void restart() {
        backoffSeconds = 5;
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
        JHubConfig.SkimmerSection cfg = ConfigManager.getInstance().getConfig().skimmer;
        if (cfg == null || !cfg.enabled) {
            running.set(false);
            return;
        }

        log.info("Connecting to local skimmer {}:{} …", cfg.server, cfg.port);
        try {
            socket = new Socket(cfg.server, cfg.port);
            socket.setSoTimeout(180_000); // 3-minute idle timeout
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            connected.set(true);
            backoffSeconds = 5;
            log.info("Skimmer connected");

            String loginCall = (cfg.loginCallsign != null && !cfg.loginCallsign.isBlank())
                ? cfg.loginCallsign
                : ConfigManager.getInstance().getStation().callsign;
            writer.println(loginCall);

            readerThread.submit(this::readLoop);
        } catch (Exception e) {
            connected.set(false);
            log.warn("Skimmer connect failed: {} — retrying in {}s", e.getMessage(), backoffSeconds);
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
            if (running.get()) log.warn("Skimmer read error: {} — reconnecting in {}s", e.getMessage(), backoffSeconds);
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
        Matcher m = SKIMMER_PATTERN.matcher(line);
        if (!m.find()) {
            log.trace("Skimmer non-spot: {}", line);
            return;
        }
        try {
            Spot spot = new Spot();
            spot.type      = "SPOT";
            spot.source    = "SKIMMER";
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

            // Filter on SNR + band/mode same as RBN
            JHubConfig.SkimmerSection cfg = ConfigManager.getInstance().getConfig().skimmer;
            if (spot.snrDb != null && spot.snrDb < cfg.minSnrDb) return;
            if (cfg.bands != null && !cfg.bands.isEmpty() && spot.band != null && !cfg.bands.contains(spot.band)) return;
            if (cfg.modes != null && !cfg.modes.isEmpty() && spot.mode != null && !cfg.modes.contains(spot.mode)) return;

            SpotEnricher.getInstance().enrich(spot);
            if (router != null) router.publishSpot(spot);
            log.debug("Skimmer spot: {} {} @ {}kHz  {}dB  {}WPM",
                spot.spotter, spot.spotted, spot.frequency / 1000, spot.snrDb, spot.wpm);
        } catch (Exception e) {
            log.debug("Skimmer parse error '{}': {}", line, e.getMessage());
        }
    }

    /** Same band map ClusterManager / RbnClient use — kept local to avoid coupling. */
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
