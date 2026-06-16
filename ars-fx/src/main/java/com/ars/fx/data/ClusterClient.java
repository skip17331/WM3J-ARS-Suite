package com.ars.fx.data;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Live DX-cluster Telnet feed for the J-Hub dashboard cluster panel. Standalone
 * re-implementation of j-hub's ClusterManager protocol — connect, log in with
 * the station callsign, parse "DX de" spot lines, keep a bounded recent list.
 *
 * <p>Defaults: host {@code dxc.ve7cc.net}, port {@code 23}, login {@code WM3J}
 * (WM3J's call). Override with {@code -Dcluster.host/.port/.call}.
 *
 * <p>One background daemon thread owns the socket; on every new spot (and on
 * connect/disconnect) the registered listener fires on the JavaFX thread. Auto-
 * reconnects with exponential back-off so a dropped cluster heals itself.
 */
public final class ClusterClient {

    private static final ClusterClient INSTANCE = new ClusterClient();
    public static ClusterClient getInstance() { return INSTANCE; }
    private ClusterClient() {}

    // DX de <spotter>:  <freqKHz>  <spotted>  <comment>  <hhmm>Z
    private static final Pattern SPOT = Pattern.compile(
            "DX de\\s+([A-Z0-9/\\-]+)[:\\s]+([\\d.]+)\\s+([A-Z0-9/\\-]+)\\s+(.*?)\\s+(\\d{4}Z?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final int MAX_SPOTS = 40;
    private static final int MAX_BACKOFF = 60;

    /** One cluster spot. {@code arrivalMs} is local receive time, for the age column. */
    public record Spot(long freqHz, String call, String mode, String band, String de, String comment, long arrivalMs) {}

    private volatile String host  = System.getProperty("cluster.host", "dxc.ve7cc.net");
    private volatile int    port  = Integer.getInteger("cluster.port", 23);
    private volatile String call  = System.getProperty("cluster.call", HubConfig.call());

    private final Deque<Spot> spots = new ArrayDeque<>();
    private volatile Consumer<ClusterClient> listener = c -> {};
    private volatile boolean connected = false;
    private volatile boolean started = false;
    private volatile boolean paused = false;   // operator pressed Disconnect
    private int backoff = 5;

    private Socket socket; private BufferedReader reader; private PrintWriter writer;

    public boolean isConnected() { return connected; }
    /** True when the operator wants a connection (i.e. not manually disconnected). */
    public boolean wantConnected() { return !paused; }
    public String endpoint() { return host; }
    public String loginCall() { return call; }

    /** Snapshot of recent spots, newest first. */
    public synchronized List<Spot> spots() { return new ArrayList<>(spots); }

    public void setListener(Consumer<ClusterClient> l) { this.listener = l == null ? c -> {} : l; fire(); }

    public synchronized void start() {
        if (started) return;
        started = true;
        Thread t = new Thread(this::loop, "cluster");
        t.setDaemon(true);
        t.start();
    }

    /** Operator Disconnect — stop reading and don't auto-reconnect. */
    public void disconnect() { paused = true; closeSocket(); fire(); }
    /** Operator Connect — resume; the loop reconnects within ~300ms. */
    public void connect() { backoff = 5; paused = false; }
    /** Point at a new server/login and reconnect (clears the current feed). */
    public void setEndpoint(String h, int p, String c) {
        this.host = (h == null || h.isBlank()) ? this.host : h.trim();
        if (p > 0) this.port = p;
        this.call = (c == null || c.isBlank()) ? this.call : c.trim().toUpperCase();
        synchronized (this) { spots.clear(); }
        backoff = 5; paused = false; closeSocket();
    }

    private void loop() {
        while (true) {
            if (paused) {
                if (connected) { connected = false; closeSocket(); fire(); }
                sleepMs(300);
                continue;
            }
            try {
                openAndLogin();
                connected = true;
                backoff = 5;
                fire();
                String line;
                while (!paused && (line = reader.readLine()) != null) processLine(line.trim());
            } catch (Exception e) {
                /* connect/read failure → back off and retry */
            }
            connected = false;
            closeSocket();
            fire();
            if (paused) continue;
            sleepMs(backoff * 1000L);
            backoff = Math.min(backoff * 2, MAX_BACKOFF);
        }
    }
    private static void sleepMs(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    private void openAndLogin() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 8000);
        s.setSoTimeout(0); // cluster streams indefinitely; no read timeout
        socket = s;
        reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
        writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.US_ASCII), true);
        writer.println(call); // most DXSpider/CC clusters accept the bare call as login
    }

    private void processLine(String line) {
        if (line.isEmpty()) return;
        Matcher m = SPOT.matcher(line);
        if (!m.find()) return;
        Spot spot = parse(m);
        if (spot == null) return;
        synchronized (this) {
            spots.addFirst(spot);
            while (spots.size() > MAX_SPOTS) spots.removeLast();
        }
        fire();
    }

    private static Spot parse(Matcher m) {
        long freqHz;
        try { freqHz = (long) (Double.parseDouble(m.group(2)) * 1000); }
        catch (NumberFormatException e) { return null; }
        String de = m.group(1).toUpperCase();
        String call = m.group(3).toUpperCase();
        String comment = m.group(4).trim();
        String band = freqToBand(freqHz);
        String mode = detectMode(comment, freqHz);
        return new Spot(freqHz, call, mode, band, de, comment, System.currentTimeMillis());
    }

    private void fire() {
        Consumer<ClusterClient> l = listener;
        try { javafx.application.Platform.runLater(() -> l.accept(this)); }
        catch (IllegalStateException noFx) { /* toolkit not up (tests) */ }
    }

    private void closeSocket() {
        // Close ONLY the socket. When disconnect() runs on another thread while the
        // loop thread is blocked in readLine(), closing the reader/writer first would
        // deadlock (their close() waits on the lock the blocked read holds). Closing
        // the socket tears down its streams and asynchronously unblocks the read.
        Socket s = socket;
        socket = null; reader = null; writer = null;
        try { if (s != null) s.close(); } catch (Exception ignored) {}
    }

    // ── helpers (mirror j-hub ClusterManager) ─────────────────────────────────
    static String freqToBand(long hz) {
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

    static String detectMode(String comment, long freqHz) {
        if (comment != null) {
            String uc = comment.toUpperCase();
            if (uc.contains("FT8"))  return "FT8";
            if (uc.contains("FT4"))  return "FT4";
            if (uc.contains("RTTY")) return "RTTY";
            if (uc.contains("PSK"))  return "PSK31";
            if (uc.contains("JS8"))  return "JS8";
            if (uc.contains("CW"))   return "CW";
            if (uc.contains("SSB") || uc.contains("LSB") || uc.contains("USB")) return "SSB";
        }
        long khz = freqHz / 1000;
        if (khz >= 14074 && khz <= 14075) return "FT8";
        if (khz >= 7074  && khz <= 7075)  return "FT8";
        if (khz >= 21074 && khz <= 21075) return "FT8";
        return "SSB";
    }

    /** 14022000 → "14.022.0" (MHz.kHz.100Hz). */
    public static String fmtFreq(long hz) {
        long mhz = hz / 1_000_000, khz = (hz / 1000) % 1000, tenth = (hz / 100) % 10;
        return String.format("%d.%03d.%d", mhz, khz, tenth);
    }

    /** Compact "now / Nm / Nh" age from a receive time. */
    public static String age(long arrivalMs) {
        long secs = Math.max(0, (System.currentTimeMillis() - arrivalMs) / 1000);
        if (secs < 60) return "now";
        if (secs < 3600) return (secs / 60) + "m";
        return (secs / 3600) + "h";
    }
}
