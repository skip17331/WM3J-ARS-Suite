package com.ars.fx.data;

import com.ars.fx.sat.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * Real satellite tracking for the J-Sat surface. Drives the SGP4 orbital core
 * (copied from j-sat) with:
 *   • observer QTH from <code>~/.j-sat/settings.json</code> (fallback: the J-Hub station location),
 *   • fresh TLEs from <code>~/.j-sat/tles.txt</code> (the cache j-sat refreshes
 *     daily; fallback to a bundled snapshot so it still works standalone),
 *   • amateur frequencies/type from the bundled satellite-registry.json.
 *
 * <p>A background thread recomputes live az/el/range/Doppler every {@value
 * #LIVE_MS} ms and full pass predictions every {@value #PASS_MS} ms, pushing a
 * {@link Snapshot} to the registered listener on the JavaFX thread.
 */
public final class SatService {

    private static final SatService INSTANCE = new SatService();
    public static SatService getInstance() { return INSTANCE; }
    private SatService() {}

    private static final long LIVE_MS = 2000;
    private static final long PASS_MS = 60_000;

    // ── public data shapes ────────────────────────────────────────────────────
    /** One satellite's current geometry + radio + next pass. */
    public record SatInfo(String name, String type, String mode,
                          long downlinkHz, long uplinkHz,
                          boolean inPass, double azDeg, double elDeg, double rangeKm, double rangeRateKmS,
                          long secsToAos, double nextMaxElDeg, long nextDurSec,
                          double aosAzDeg, double maxElAzDeg, double losAzDeg,
                          long downlinkCorrHz, long uplinkCorrHz,
                          double subLat, double subLon, double altKm,
                          double apogeeKm, double perigeeKm, long aosEpoch, long losEpoch,
                          double[][] track) {
        public String statusPill() {
            if (inPass) return "AOS";
            if (secsToAos < 0) return "LOS";
            return "+" + compact(secsToAos);
        }
        public String statusClass() { return inPass ? "aos" : (secsToAos < 0 ? "los" : ""); }
        /** Angular radius of the satellite footprint in degrees (horizon-to-horizon visibility). */
        public double footprintDeg() { return Math.toDegrees(Math.acos(6378.137 / (6378.137 + Math.max(1, altKm)))); }
    }

    public record Snapshot(boolean ready, List<SatInfo> sats, SatInfo tracked, String error) {
        public static final Snapshot EMPTY = new Snapshot(false, List.of(), null, "");
    }

    // ── registry definition ───────────────────────────────────────────────────
    private record Def(int norad, String name, String type, long downlinkHz, long uplinkHz, String mode) {}

    private volatile double[] obs = {0, 0, 0};              // {lat, lon, altKm}: re-read every pass computation so edits in J-Hub apply at once
    private volatile Snapshot snapshot = Snapshot.EMPTY;
    private volatile Consumer<SatService> listener = s -> {};
    private volatile boolean started = false;

    private volatile Map<Integer, TleSet> tles;     // by NORAD id
    private volatile List<Def> defs;
    private long lastPassMs = 0;
    private final Map<Integer, SatellitePass> nextPass = new HashMap<>();

    public Snapshot snapshot() { return snapshot; }
    public double obsLat() { return obs[0]; }
    public double obsLon() { return obs[1]; }
    public void setListener(Consumer<SatService> l) { this.listener = l == null ? s -> {} : l; fire(); }

    public boolean isRunning() { return started; }
    public synchronized void start() {
        if (started) return;
        started = true;
        Thread t = new Thread(this::loop, "sat-track");
        t.setDaemon(true);
        t.start();
    }

    private void loop() {
        try { tles = TleStore.current(); defs = buildDefs(); }
        catch (Exception e) { snapshot = new Snapshot(false, List.of(), null, "TLE/registry load failed: " + e.getMessage()); fire(); return; }
        TleStore.addListener(this::reload);        // new elements arrived → re-resolve the tracked set
        TleStore.autoRefreshIfDue();               // daily, in the background, if enabled in J-Hub ▸ Satellites
        while (true) {
            double[] o = loadObserver();
            if (o == null) {
                snapshot = new Snapshot(false, List.of(), null, HubConfig.LOCATION_HINT);
                fire();
                try { Thread.sleep(LIVE_MS); } catch (InterruptedException ie) { return; }
                continue;
            }
            obs = o;
            try { recompute(); } catch (Exception e) { /* keep last good snapshot */ }
            try { Thread.sleep(LIVE_MS); } catch (InterruptedException ie) { return; }
        }
    }

    /** What the operator picked in J-Hub ▸ Satellites, resolved against the elements we actually hold. A pick with
     *  no elements can't be tracked; it is remembered in {@link #missingNames()} instead of vanishing silently. */
    private List<Def> buildDefs() {
        Map<Integer, SatRegistry.Entry> reg = SatRegistry.byNorad();
        List<Def> list = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (int id : SatSelection.selected()) {
            SatRegistry.Entry e = reg.get(id);
            TleSet tle = tles.get(id);
            if (tle == null) { missing.add(e != null ? e.name() : "NORAD " + id); continue; }
            if (e != null) list.add(new Def(id, e.name(), e.type(), e.downlinkHz(), e.uplinkHz(), e.mode()));
            else list.add(new Def(id, tle.name, "No radio data", 0, 0, ""));
        }
        missingNames = List.copyOf(missing);
        return list;
    }

    /** Selected satellites that have no orbital elements loaded. */
    public List<String> missingNames() { return missingNames; }
    private volatile List<String> missingNames = List.of();

    /** Re-read the selection and elements and recompute now (off the caller's thread). */
    public void reload() {
        Thread t = new Thread(() -> {
            synchronized (this) {
                tles = TleStore.current(); defs = buildDefs(); nextPass.clear(); lastPassMs = 0;
            }
            if (started) { try { recompute(); } catch (Exception ignored) {} }
        }, "sat-reload");
        t.setDaemon(true); t.start();
    }

    private synchronized void recompute() {
        if (defs == null || tles == null) return;
        Instant now = Instant.now();
        boolean refreshPasses = (System.currentTimeMillis() - lastPassMs) > PASS_MS || nextPass.isEmpty();
        Sgp4Propagator prop = new Sgp4Propagator();
        PassPredictor predictor = new PassPredictor();
        List<SatInfo> out = new ArrayList<>();

        for (Def d : defs) {
            TleSet tle = tles.get(d.norad());
            if (tle == null) continue;
            double az = 0, el = 0, range = 0, rate = 0, subLat = 0, subLon = 0, altKm = 0;
            try {
                prop.init(tle);
                double[] pv = prop.propagate(now);
                if (pv == null) continue;
                double[] aer = CoordTransform.satAzElRange(new double[]{pv[0], pv[1], pv[2]},
                        new double[]{pv[3], pv[4], pv[5]}, obs[0], obs[1], obs[2], now);
                az = aer[0]; el = aer[1]; range = aer[2]; rate = aer[3];
                double[] geo = CoordTransform.eciToGeodetic(new double[]{pv[0], pv[1], pv[2]}, now);
                subLat = geo[0]; subLon = geo[1]; altKm = geo[2];
            } catch (Exception e) { continue; }

            // apogee / perigee from the TLE mean motion + eccentricity
            double nMo = tle.meanMotion * 2 * Math.PI / 86400.0;          // rad/s
            double sma = Math.cbrt(398600.4418 / (nMo * nMo));            // semi-major axis (km)
            double apogeeKm = sma * (1 + tle.eccentricity) - 6378.137;
            double perigeeKm = sma * (1 - tle.eccentricity) - 6378.137;

            // ground track: sub-satellite point over one orbital period, centred on now
            double periodMin = tle.meanMotion > 0 ? 1440.0 / tle.meanMotion : 95.0;
            int steps = 90;
            List<double[]> tr = new ArrayList<>();
            for (int i = 0; i <= steps; i++) {
                Instant when = now.plusMillis((long) ((-periodMin * 0.5 + periodMin * i / steps) * 60000));
                try {
                    double[] pvk = prop.propagate(when);
                    if (pvk == null) continue;
                    double[] geo = CoordTransform.eciToGeodetic(new double[]{pvk[0], pvk[1], pvk[2]}, when);
                    tr.add(new double[]{geo[0], geo[1]});
                } catch (Exception ignored) {}
            }
            double[][] track = tr.toArray(new double[0][]);

            if (refreshPasses) {
                try {
                    List<SatellitePass> ps = predictor.predict(tle, obs[0], obs[1], obs[2], now);
                    if (!ps.isEmpty()) nextPass.put(d.norad(), ps.get(0)); else nextPass.remove(d.norad());
                } catch (Exception ignored) {}
            }
            SatellitePass np = nextPass.get(d.norad());
            boolean inPass = el > 0;
            long secsToAos = np != null ? np.secondsUntilAos(now) : -1;
            double maxEl = np != null ? np.maxElDeg : 0;
            long durSec = np != null ? np.durationSeconds() : 0;
            double aosAz = np != null ? np.aosAzDeg : 0, maxElAz = np != null ? np.maxElAzDeg : 0, losAz = np != null ? np.losAzDeg : 0;
            long aosEpoch = np != null ? np.aos.getEpochSecond() : -1;
            long losEpoch = np != null ? np.los.getEpochSecond() : -1;

            long dnCorr = DopplerCalculator.correctedFrequency(d.downlinkHz(), rate);
            long upCorr = DopplerCalculator.invertingUplinkCorrection(d.uplinkHz(), rate);

            out.add(new SatInfo(d.name(), d.type(), d.mode(), d.downlinkHz(), d.uplinkHz(),
                    inPass, az, el, range, rate, secsToAos, maxEl, durSec, aosAz, maxElAz, losAz, dnCorr, upCorr,
                    subLat, subLon, altKm, apogeeKm, perigeeKm, aosEpoch, losEpoch, track));
        }
        if (refreshPasses) lastPassMs = System.currentTimeMillis();

        // Sort: in-pass first (highest elevation), then by soonest AOS.
        out.sort((a, b) -> {
            if (a.inPass() != b.inPass()) return a.inPass() ? -1 : 1;
            if (a.inPass()) return Double.compare(b.elDeg(), a.elDeg());
            long sa = a.secsToAos() < 0 ? Long.MAX_VALUE : a.secsToAos();
            long sb = b.secsToAos() < 0 ? Long.MAX_VALUE : b.secsToAos();
            return Long.compare(sa, sb);
        });
        SatInfo tracked = out.isEmpty() ? null : out.get(0);
        snapshot = new Snapshot(true, out, tracked, "");
        fire();
    }

    private void fire() {
        Consumer<SatService> l = listener;
        try { javafx.application.Platform.runLater(() -> l.accept(this)); }
        catch (IllegalStateException noFx) { /* toolkit not up (tests) */ }
    }

    // ── loaders ───────────────────────────────────────────────────────────────
    private static double[] loadObserver() {
        Path p = Paths.get(System.getProperty("user.home"), ".j-sat", "settings.json");
        try (Reader r = Files.newBufferedReader(p)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            return new double[]{ o.get("qthLat").getAsDouble(), o.get("qthLon").getAsDouble(),
                    o.has("qthAltKm") ? o.get("qthAltKm").getAsDouble() : 0.0 };
        } catch (Exception e) {
            // no J-Sat file: use the J-Hub station location (lat/lon or grid), height from Station ▸ Elevation
            double[] here = HubConfig.location();
            return here == null ? null : new double[]{ here[0], here[1], elevKm() };
        }
    }
    /** Station elevation in km (the setting is stored in the chosen height unit). */
    private static double elevKm() {
        try { return Units.heightToMeters(Double.parseDouble(HubConfig.get("station.elev", "0").trim())) / 1000.0; } catch (Exception e) { return 0; }
    }
    private static double dbl(String s, double def) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; } }

    // ── display helpers ───────────────────────────────────────────────────────
    /** seconds → "18M" / "1H 26M". */
    public static String compact(long secs) {
        long m = secs / 60;
        if (m < 60) return m + "M";
        return (m / 60) + "H " + (m % 60) + "M";
    }
    /** seconds → "+18m" / "+1h 26m" (lower-case, for the pass list). */
    public static String compactLower(long secs) {
        if (secs < 0) return "—";
        long m = secs / 60;
        if (m < 60) return "+" + m + "m";
        return "+" + (m / 60) + "h " + (m % 60) + "m";
    }
    /** Hz → "145.960" MHz string (3 dp). */
    public static String mhz(long hz) { return hz <= 0 ? "—" : String.format("%.3f", hz / 1_000_000.0); }
    /** Hz → "145.9612" MHz string (4 dp, for Doppler-corrected). */
    public static String mhz4(long hz) { return hz <= 0 ? "—" : String.format("%.4f", hz / 1_000_000.0); }
    /** signed kHz shift, e.g. "+1.2 kHz". */
    public static String shiftKHz(long corrHz, long nomHz) {
        if (nomHz <= 0) return "—";
        double k = (corrHz - nomHz) / 1000.0;
        return String.format("%+.1f kHz", k);
    }
    /** Epoch seconds → "HH:mm:ss" UTC ("—" if absent). */
    public static String hhmmUtc(long epoch) {
        if (epoch < 0) return "—";
        return java.time.LocalTime.ofInstant(java.time.Instant.ofEpochSecond(epoch), java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
