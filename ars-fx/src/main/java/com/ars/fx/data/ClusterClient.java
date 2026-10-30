package com.ars.fx.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Live DX-cluster Telnet feed (dxc.ve7cc.net by default): log in with the station callsign, parse "DX de" spot
 * lines, keep a bounded recent list. The connection, status model ({@link Status}: disconnected / connecting /
 * connected), raw log and hub mirroring all live in {@link TelnetFeed}; this class adds the spot parsing.
 *
 * <p>The hub process owns the socket; windows attached to the hub mirror it. On every change the registered
 * listener fires on the JavaFX thread.
 */
public final class ClusterClient extends TelnetFeed {

    private static final ClusterClient INSTANCE = new ClusterClient();
    public static ClusterClient getInstance() { return INSTANCE; }

    private ClusterClient() {
        super("cluster", "cluster", System.getProperty("cluster.host", "dxc.ve7cc.net"),
                Integer.getInteger("cluster.port", 23), System.getProperty("cluster.call", HubConfig.call()),
                HubConfig.getBool("data.clusterAutoConnect", false));   // not on by default: the operator opts in (J-Hub ▸ Data ▸ DX Cluster)
    }

    // DX de <spotter>:  <freqKHz>  <spotted>  <comment>  <hhmm>Z
    private static final Pattern SPOT = Pattern.compile(
            "DX de\\s+([A-Z0-9/\\-]+)[:\\s]+([\\d.]+)\\s+([A-Z0-9/\\-]+)\\s+(.*?)\\s+(\\d{4}Z?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final int MAX_SPOTS = 40;

    /** One cluster spot. {@code arrivalMs} is local receive time, for the age column. */
    public record Spot(long freqHz, String call, String mode, String band, String de, String comment, long arrivalMs) {}

    /** Bands and mode groups of the activity heat map (see {@link #activity(long)}). */
    public static final String[] HEAT_BANDS = {"160m", "80m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m"};
    public static final String[] HEAT_MODES = {"CW", "Phone", "Digi"};
    private static final int ACTIVITY_KEEP_MS = 60 * 60_000, ACTIVITY_MAX = 6000;

    private final Deque<Spot> spots = new ArrayDeque<>();
    /** Every spot heard in the last hour as {arrivalMs, bandIndex, modeGroup} — longer memory than the 40-spot list. */
    private final Deque<long[]> activityLog = new ArrayDeque<>();
    private volatile int[][] remoteActivity;           // window side: what the hub last sent
    private volatile Consumer<ClusterClient> listener = c -> {};

    /** Snapshot of recent spots, newest first. */
    public synchronized List<Spot> spots() { return new ArrayList<>(spots); }

    public void setListener(Consumer<ClusterClient> l) { this.listener = l == null ? c -> {} : l; fire(); }

    /** Hub side: take the server/login from J-Hub's config ({@code data.clusterServer}, {@code data.clusterLogin}). */
    public void applyConfig() {
        if (RemoteLink.isActive()) return;
        String srv = HubConfig.get("data.clusterServer", "").trim();
        String login = HubConfig.get("data.clusterLogin", "").trim();
        String h = host; int p = port;
        if (!srv.isEmpty()) {
            int c = srv.lastIndexOf(':');
            if (c > 0) { h = srv.substring(0, c); try { p = Integer.parseInt(srv.substring(c + 1).trim()); } catch (Exception ignored) {} }
            else h = srv;
        }
        setEndpoint(h, p, login.isEmpty() ? HubConfig.call() : login);
    }

    /** Spot counts per band (rows, {@link #HEAT_BANDS}) and mode group (columns, {@link #HEAT_MODES}) over the last
     *  {@code windowMs}. A window attached to the hub shows what the hub counted (it always uses the hub's 30 min). */
    public int[][] activity(long windowMs) {
        int[][] remote = remoteActivity;
        if (RemoteLink.isActive()) return remote != null ? remote : new int[HEAT_BANDS.length][HEAT_MODES.length];
        int[][] out = new int[HEAT_BANDS.length][HEAT_MODES.length];
        long cutoff = System.currentTimeMillis() - windowMs;
        synchronized (this) { for (long[] a : activityLog) if (a[0] >= cutoff) out[(int) a[1]][(int) a[2]]++; }
        return out;
    }
    /** Hub side: the 30-minute counts flattened row by row, for the snapshot sent to windows. */
    public int[] activityFlat() {
        int[][] a = activity(30 * 60_000L); int[] f = new int[HEAT_BANDS.length * HEAT_MODES.length];
        for (int i = 0; i < a.length; i++) for (int j = 0; j < a[i].length; j++) f[i * HEAT_MODES.length + j] = a[i][j];
        return f;
    }
    /** Window side: adopt the hub's counts. */
    public void applyRemoteActivity(int[] flat) {
        if (flat == null || flat.length != HEAT_BANDS.length * HEAT_MODES.length) return;
        int[][] a = new int[HEAT_BANDS.length][HEAT_MODES.length];
        for (int i = 0; i < flat.length; i++) a[i / HEAT_MODES.length][i % HEAT_MODES.length] = flat[i];
        remoteActivity = a;
    }
    private static int modeGroup(String mode) {
        if (mode == null) return 1;
        return switch (mode.toUpperCase()) { case "CW" -> 0; case "SSB", "USB", "LSB", "FM", "AM" -> 1; default -> 2; };
    }
    private synchronized void recordActivity(Spot s) {
        int b = -1; for (int i = 0; i < HEAT_BANDS.length; i++) if (HEAT_BANDS[i].equals(s.band())) { b = i; break; }
        if (b < 0) return;
        activityLog.addLast(new long[]{s.arrivalMs(), b, modeGroup(s.mode())});
        long cutoff = s.arrivalMs() - ACTIVITY_KEEP_MS;
        while (!activityLog.isEmpty() && (activityLog.peekFirst()[0] < cutoff || activityLog.size() > ACTIVITY_MAX)) activityLog.removeFirst();
    }

    /** Replace the feed with spots pushed from the hub (window side). Status arrives separately. */
    public void injectSpots(List<Spot> incoming) {
        synchronized (this) {
            spots.clear();
            if (incoming != null) for (Spot s : incoming) { spots.addLast(s); if (spots.size() >= MAX_SPOTS) break; }
        }
        fire();
    }

    @Override protected void onEndpointChanged() { synchronized (this) { spots.clear(); activityLog.clear(); } }

    @Override protected void onLine(String line) {
        Matcher m = SPOT.matcher(line);
        if (!m.find()) return;
        Spot spot = parse(m);
        if (spot == null) return;
        recordActivity(spot);
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

    @Override protected void fire() {
        Consumer<ClusterClient> l = listener;
        try { javafx.application.Platform.runLater(() -> l.accept(this)); }
        catch (IllegalStateException noFx) { /* toolkit not up (tests) */ }
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
