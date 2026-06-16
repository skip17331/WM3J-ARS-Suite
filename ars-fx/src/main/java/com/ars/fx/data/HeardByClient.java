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
 * "Heard By" — reverse-beacon feed of spots OF the operator's own callsign
 * (who heard WM3J), from the Reverse Beacon Network. Connects via Telnet to
 * {@code telnet.reversebeacon.net:7000}, logs in with the station call, parses
 * the {@code DX de} skimmer lines, and keeps only those whose SPOTTED call is
 * the operator's — i.e. the skimmers that heard us.
 *
 * <p>Defaults overridable via {@code -Dheardby.host/.port/.call}. Background
 * daemon thread + auto-reconnect, same as {@link ClusterClient}. RBN/internet
 * is unreachable from sandboxes, so the drawer simply shows "listening…".
 */
public final class HeardByClient {

    private static final HeardByClient INSTANCE = new HeardByClient();
    public static HeardByClient getInstance() { return INSTANCE; }
    private HeardByClient() {}

    // DX de <skimmer>:  <freqKHz>  <spotted>  <comment incl. mode/snr/wpm>  <hhmm>Z
    // RBN skimmer calls carry a trailing "-#", so '#' is allowed in the spotter group.
    private static final Pattern SPOT = Pattern.compile(
            "DX de\\s+([A-Z0-9/#\\-]+)[:\\s]+([\\d.]+)\\s+([A-Z0-9/\\-]+)\\s+(.*?)\\s+(\\d{4}Z?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final int MAX_SPOTS = 40;
    private static final int MAX_BACKOFF = 60;

    /** One reverse spot: {@code spotter} is the skimmer that heard us. */
    public record HeardSpot(long freqHz, String spotter, String mode, String band, String report, long arrivalMs) {}

    private volatile String host = System.getProperty("heardby.host", "telnet.reversebeacon.net");
    private volatile int    port = Integer.getInteger("heardby.port", 7000);
    private volatile String call = System.getProperty("heardby.call", HubConfig.call());

    private final Deque<HeardSpot> spots = new ArrayDeque<>();
    private volatile Consumer<HeardByClient> listener = c -> {};
    private volatile boolean connected = false;
    private volatile boolean started = false;
    private int backoff = 5;

    private Socket socket; private BufferedReader reader; private PrintWriter writer;

    public boolean isConnected() { return connected; }
    public String endpoint() { return host; }
    public String myCall() { return call; }
    public synchronized List<HeardSpot> spots() { return new ArrayList<>(spots); }
    public void setListener(Consumer<HeardByClient> l) { this.listener = l == null ? c -> {} : l; fire(); }

    public synchronized void start() {
        if (started) return;
        started = true;
        Thread t = new Thread(this::loop, "heardby");
        t.setDaemon(true);
        t.start();
    }

    private void loop() {
        while (true) {
            try {
                openAndLogin();
                connected = true; backoff = 5; fire();
                String line;
                while ((line = reader.readLine()) != null) processLine(line.trim());
            } catch (Exception e) { /* connect/read failure → back off */ }
            connected = false;
            closeSocket();
            fire();
            sleepMs(backoff * 1000L);
            backoff = Math.min(backoff * 2, MAX_BACKOFF);
        }
    }
    private static void sleepMs(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    private void openAndLogin() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 8000);
        s.setSoTimeout(0);
        socket = s;
        reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
        writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.US_ASCII), true);
        writer.println(call); // RBN prompts "Please enter your call:" — the bare call works
    }

    private void processLine(String line) {
        if (line.isEmpty()) return;
        Matcher m = SPOT.matcher(line);
        if (!m.find()) return;
        String spotted = m.group(3).toUpperCase();
        if (!spotted.equalsIgnoreCase(call)) return;     // keep only spots OF us
        long freqHz;
        try { freqHz = (long) (Double.parseDouble(m.group(2)) * 1000); }
        catch (NumberFormatException e) { return; }
        String spotter = m.group(1).toUpperCase();
        String comment = m.group(4).trim();
        HeardSpot hs = new HeardSpot(freqHz, spotter, ClusterClient.detectMode(comment, freqHz),
                ClusterClient.freqToBand(freqHz), comment, System.currentTimeMillis());
        synchronized (this) {
            spots.addFirst(hs);
            while (spots.size() > MAX_SPOTS) spots.removeLast();
        }
        fire();
    }

    private void fire() {
        Consumer<HeardByClient> l = listener;
        try { javafx.application.Platform.runLater(() -> l.accept(this)); }
        catch (IllegalStateException noFx) { /* toolkit not up */ }
    }

    private void closeSocket() {
        Socket s = socket;
        socket = null; reader = null; writer = null;
        try { if (s != null) s.close(); } catch (Exception ignored) {}
    }
}
