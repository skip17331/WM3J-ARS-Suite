package com.ars.fx.data;

import com.ars.fx.sat.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * Real satellite tracking for the J-Sat surface. Drives the SGP4 orbital core
 * (copied from j-sat) with:
 *   • observer QTH from <code>~/.j-sat/settings.json</code> (fallback WM3J grid),
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

    private final double[] obs = loadObserver();           // {lat, lon, altKm}
    private volatile Snapshot snapshot = Snapshot.EMPTY;
    private volatile Consumer<SatService> listener = s -> {};
    private volatile boolean started = false;

    private Map<Integer, TleSet> tles;     // by NORAD id
    private List<Def> defs;
    private long lastPassMs = 0;
    private final Map<Integer, SatellitePass> nextPass = new HashMap<>();

    public Snapshot snapshot() { return snapshot; }
    public double obsLat() { return obs[0]; }
    public double obsLon() { return obs[1]; }
    public void setListener(Consumer<SatService> l) { this.listener = l == null ? s -> {} : l; fire(); }

    public synchronized void start() {
        if (started) return;
        started = true;
        Thread t = new Thread(this::loop, "sat-track");
        t.setDaemon(true);
        t.start();
    }

    private void loop() {
        try { tles = loadTles(); defs = loadRegistry(); }
        catch (Exception e) { snapshot = new Snapshot(false, List.of(), null, "TLE/registry load failed: " + e.getMessage()); fire(); return; }
        while (true) {
            try { recompute(); } catch (Exception e) { /* keep last good snapshot */ }
            try { Thread.sleep(LIVE_MS); } catch (InterruptedException ie) { return; }
        }
    }

    private void recompute() {
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
            return new double[]{ 39.745583, -76.959714, 0.0 }; // WM3J / FM19
        }
    }

    private static Map<Integer, TleSet> loadTles() throws Exception {
        List<String> lines;
        Path cache = Paths.get(System.getProperty("user.home"), ".j-sat", "tles.txt");
        if (Files.isReadable(cache)) lines = Files.readAllLines(cache, StandardCharsets.UTF_8);
        else lines = readResourceLines("/com/ars/fx/sat/tles-fallback.txt");
        Map<Integer, TleSet> map = new HashMap<>();
        int i = 0;
        while (i + 2 < lines.size()) {
            String name = lines.get(i).trim();
            String l1 = lines.get(i + 1).trim(), l2 = lines.get(i + 2).trim();
            if (l1.startsWith("1 ") && l2.startsWith("2 ")) {
                try {
                    TleSet t = new TleSet(name, l1, l2);
                    map.put(Integer.parseInt(t.noradId), t);
                } catch (Exception ignored) {}
                i += 3;
            } else { i += 1; }  // resync if a stray/blank line appears
        }
        return map;
    }

    private static List<Def> loadRegistry() throws Exception {
        try (InputStream in = SatService.class.getResourceAsStream("/com/ars/fx/sat/satellite-registry.json");
             Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            JsonArray arr = root.isJsonArray() ? root.getAsJsonArray()
                    : root.getAsJsonObject().getAsJsonArray(root.getAsJsonObject().keySet().iterator().next());
            // "Selected in J-Hub" = ~/.j-sat settings.json enabledSatellites (falls back to the
            // registry's own enabled flag when no selection is present).
            java.util.Set<String> enabled = loadEnabled();
            List<Def> list = new ArrayList<>();
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                String nm = o.get("name").getAsString();
                boolean keep = enabled.isEmpty() ? (!o.has("enabled") || o.get("enabled").getAsBoolean())
                                                 : enabled.contains(nm);
                if (!keep) continue;
                long dn = o.has("downlinkHz") ? o.get("downlinkHz").getAsLong() : 0;
                long up = o.has("uplinkHz") ? o.get("uplinkHz").getAsLong() : 0;
                String mode = "FM";
                if (o.has("modes") && o.getAsJsonArray("modes").size() > 0) mode = o.getAsJsonArray("modes").get(0).getAsString();
                list.add(new Def(o.get("noradId").getAsInt(), o.get("name").getAsString(),
                        o.has("type") ? o.get("type").getAsString() : "", dn, up, mode));
            }
            return list;
        }
    }

    /** Operator-selected satellite names from ~/.j-sat/settings.json (j-hub selection). */
    private static java.util.Set<String> loadEnabled() {
        Path p = Paths.get(System.getProperty("user.home"), ".j-sat", "settings.json");
        try (Reader r = Files.newBufferedReader(p)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            if (o.has("enabledSatellites")) {
                java.util.Set<String> s = new java.util.HashSet<>();
                for (JsonElement e : o.getAsJsonArray("enabledSatellites")) s.add(e.getAsString());
                return s;
            }
        } catch (Exception ignored) {}
        return java.util.Set.of();
    }

    private static List<String> readResourceLines(String res) throws Exception {
        try (InputStream in = SatService.class.getResourceAsStream(res)) {
            if (in == null) return List.of();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

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
    public static String mhz(long hz) { return String.format("%.3f", hz / 1_000_000.0); }
    /** Hz → "145.9612" MHz string (4 dp, for Doppler-corrected). */
    public static String mhz4(long hz) { return String.format("%.4f", hz / 1_000_000.0); }
    /** signed kHz shift, e.g. "+1.2 kHz". */
    public static String shiftKHz(long corrHz, long nomHz) {
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
