package com.ars.fx.data;

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
public final class HeardByClient extends TelnetFeed {

    private static final HeardByClient INSTANCE = new HeardByClient();
    public static HeardByClient getInstance() { return INSTANCE; }

    private HeardByClient() {
        super("rbn", "heardby", System.getProperty("heardby.host", "telnet.reversebeacon.net"),
                Integer.getInteger("heardby.port", 7000), System.getProperty("heardby.call", HubConfig.call()),
                HubConfig.getBool("data.rbnAutoConnect", false));
    }

    // DX de <skimmer>:  <freqKHz>  <spotted>  <comment incl. mode/snr/wpm>  <hhmm>Z
    // RBN skimmer calls carry a trailing "-#", so '#' is allowed in the spotter group.
    private static final Pattern SPOT = Pattern.compile(
            "DX de\\s+([A-Z0-9/#\\-]+)[:\\s]+([\\d.]+)\\s+([A-Z0-9/\\-]+)\\s+(.*?)\\s+(\\d{4}Z?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final int MAX_SPOTS = 40;

    /** One reverse spot: {@code spotter} is the skimmer that heard us. */
    public record HeardSpot(long freqHz, String spotter, String mode, String band, String report, long arrivalMs) {}

    private final Deque<HeardSpot> spots = new ArrayDeque<>();
    private volatile Consumer<HeardByClient> listener = c -> {};

    public String myCall() { return call; }
    /** Update the watched call (e.g. from station config) and reconnect to RBN if it changed. */
    public void setCall(String c) { if (c != null && !c.isBlank()) setEndpoint(null, 0, c); }
    public synchronized List<HeardSpot> spots() { return new ArrayList<>(spots); }
    public void setListener(Consumer<HeardByClient> l) { this.listener = l == null ? c -> {} : l; fire(); }

    /** Replace the feed with "heard by" spots pushed from the hub (window side). Status arrives separately. */
    public void injectSpots(List<HeardSpot> incoming) {
        synchronized (this) {
            spots.clear();
            if (incoming != null) for (HeardSpot s : incoming) { spots.addLast(s); if (spots.size() >= MAX_SPOTS) break; }
        }
        fire();
    }

    @Override protected void onEndpointChanged() { synchronized (this) { spots.clear(); } }

    @Override protected void onLine(String line) {
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

    @Override protected void fire() {
        Consumer<HeardByClient> l = listener;
        try { javafx.application.Platform.runLater(() -> l.accept(this)); }
        catch (IllegalStateException noFx) { /* toolkit not up */ }
    }
}
