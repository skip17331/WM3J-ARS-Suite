package com.ars.fx.data;

import com.hamradio.jbridge.BandUtils;
import com.hamradio.jbridge.CallsignParser;
import com.hamradio.jbridge.WsjtxUdpListener;
import com.hamradio.jbridge.model.WsjtxDecode;
import com.hamradio.jbridge.model.WsjtxStatus;

import com.jlog.util.Maidenhead;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Process-wide holder for the real J-Bridge WSJT-X UDP listener
 * (com.hamradio:j-bridge). Binds the listener's decode/status/connection
 * callbacks once, keeps a rolling buffer of recent decodes + the last status,
 * and fans out a single refresh signal to the surface. Receive-only: it listens
 * for WSJT-X broadcasts on UDP 2237 (override with -Dwsjtx.port).
 */
public final class BridgeService {
    private BridgeService() {}

    private static WsjtxUdpListener listener;
    private static boolean started;
    private static final Deque<WsjtxDecode> decodes = new ArrayDeque<>();
    private static volatile WsjtxStatus status;
    private static volatile boolean connected;
    private static volatile String sourceApp = "", version = "";
    private static volatile Runnable sink = () -> {};

    public static synchronized void start() {
        if (started) return;
        started = true;
        try {
            listener = new WsjtxUdpListener(port(), "0.0.0.0");
            listener.setOnStatus(s -> { status = s; fire(); });
            listener.setOnDecode(d -> {
                try { enrich(d, status, stationLatLon()); } catch (RuntimeException ignored) { /* show the decode un-enriched */ }
                synchronized (decodes) { decodes.addFirst(d); while (decodes.size() > 60) decodes.removeLast(); }
                fire();
            });
            listener.setOnConnectionChange((c, app, ver) -> { connected = c; sourceApp = app == null ? "" : app; version = ver == null ? "" : ver; fire(); });
            listener.setOnClear(() -> { synchronized (decodes) { decodes.clear(); } fire(); });
            listener.setOnQsoLogged(BridgeService::autoLog);
            listener.start();
        } catch (Throwable t) {
            listener = null;
        }
    }

    /**
     * Fill in what the raw WSJT-X decode does not carry: the DX callsign, the band / mode / dial frequency from the
     * latest WSJT-X status, and its DXCC country, continent and a distance and bearing from the station. The country and
     * distance describe the <em>transmitting</em> station — on a directed message ({@code W3ABC K3XYZ -10}) that is the
     * second callsign, not the one being called. Its position is the 4-character grid at the end of the message when
     * there is one, otherwise the middle of its country. Without a station position distance and bearing stay at zero.
     *
     * @param station {lat, lon} of this station, or null if not set
     */
    static void enrich(WsjtxDecode d, WsjtxStatus st, double[] station) {
        String msg = d.getMessage();
        String call = CallsignParser.extractDxCallsign(msg);
        d.setCallsign(call);
        String sender = CallsignParser.extractSenderCallsign(msg);
        if (st != null) {
            d.setFrequency(st.getDialFrequency());
            d.setMode(st.getMode());
            String b = BandUtils.frequencyToBand(st.getDialFrequency());
            d.setBand("OOB".equals(b) ? null : b);
        }
        if (sender == null) return;
        DxccInfo.Info info = DxccInfo.of(sender);
        if (info == null) return;
        d.setCountry(info.country());
        d.setContinent(info.continent());
        d.setDxcc(info.dxcc());

        double[] far = gridInMessage(msg);
        if (far == null && !Double.isNaN(info.lat()) && !Double.isNaN(info.lon())) far = new double[]{info.lat(), info.lon()};
        if (far == null) return;
        d.setLat(far[0]);
        d.setLon(far[1]);
        if (station == null) return;
        double km = DxccInfo.distanceKm(station[0], station[1], far[0], far[1]);
        d.setDistanceKm(km);
        d.setDistanceMi(km / 1.609344);
        d.setBearing(DxccInfo.bearingDeg(station[0], station[1], far[0], far[1]));
    }

    /** The centre of the 4-character grid that ends a message ("CQ DX W3ABC FM19"), or null. "RR73" is a reply, not a grid. */
    private static double[] gridInMessage(String msg) {
        if (msg == null) return null;
        String[] t = msg.trim().toUpperCase().split("\\s+");
        String last = t.length == 0 ? "" : t[t.length - 1];
        return !"RR73".equals(last) && Maidenhead.isValid4(last) ? Maidenhead.center(last) : null;
    }

    /** This station's {lat, lon} from J-Hub ▸ Station (latitude / longitude, else the grid square); null when unset. */
    private static double[] stationLatLon() { return HubConfig.location(); }

    /** UI refresh callback — wrap your Platform.runLater(refresh) here. Replaces any previous sink. */
    public static void setSink(Runnable r) { sink = r != null ? r : () -> {}; }

    public static boolean running()    { return listener != null && listener.isRunning(); }
    public static boolean connected()  { return connected; }
    public static String sourceApp()   { return sourceApp; }
    public static String version()     { return version; }
    public static WsjtxStatus status() { return status; }
    /** UDP port WSJT-X sends to: {@code -Dwsjtx.port}, else J-Hub ▸ Data ▸ WSJT-X UDP port, else 2237. */
    public static int port() {
        Integer sys = Integer.getInteger("wsjtx.port");
        if (sys != null) return sys;
        try { int p = Integer.parseInt(HubConfig.get("data.wsjtxPort", "2237").trim()); if (p >= 1 && p <= 65535) return p; }
        catch (NumberFormatException ignored) { /* use the default */ }
        return 2237;
    }

    /** Listen on the port in the settings now (after it was changed in J-Hub ▸ Data). */
    public static synchronized void restart() {
        if (listener != null) { try { listener.stop(); } catch (RuntimeException ignored) { } listener = null; }
        connected = false; status = null;
        started = false;
        start();
        fire();
    }

    /** A QSO WSJT-X reports as logged goes into the J-Log database when J-Hub ▸ Data ▸ "Auto-log FT8/FT4 QSOs" is on. */
    private static void autoLog(com.hamradio.jbridge.model.WsjtxQsoLogged q) {
        if (q == null || !HubConfig.getBool("data.autoLogFt8", false)) return;
        String call = q.getDxCall();
        if (call == null || call.isBlank()) return;
        java.time.Instant when = q.getTimeOn() != null ? q.getTimeOn() : q.getTimestamp() != null ? q.getTimestamp() : java.time.Instant.now();
        String utc = java.time.LocalDateTime.ofInstant(when, java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String freq = q.getDialFrequency() > 0 ? String.format(java.util.Locale.ROOT, "%.6f", q.getDialFrequency() / 1e6) : null;
        String band = q.getBand() != null && !q.getBand().isBlank() ? q.getBand() : freq == null ? null : JLogDb.bandFor(freq);
        String mode = q.getMode();
        if (JLogDb.hasQso(call, band, mode, utc)) return;                    // already there
        int power = 0;
        try { power = (int) Math.round(Double.parseDouble(q.getTxPower().trim())); } catch (Exception ignored) { }
        JLogDb.insertAt(utc, call, freq, band, mode, q.getRstSent(), q.getRstReceived(), q.getName(), null,
                q.getDxGrid(), q.getComments(), null, null, power);
        SessionQsos.logged(call, q.getDxGrid(), band, mode);
        fire();
    }
    public static List<WsjtxDecode> decodes() { synchronized (decodes) { return new ArrayList<>(decodes); } }

    private static void fire() { Runnable r = sink; if (r != null) r.run(); }
}
