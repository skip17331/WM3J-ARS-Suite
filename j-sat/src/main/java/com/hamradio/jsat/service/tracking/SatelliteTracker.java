package com.hamradio.jsat.service.tracking;

import com.hamradio.jsat.model.*;
import com.hamradio.jsat.service.config.JsatSettings;
import com.hamradio.jsat.service.orbital.*;
import com.hamradio.jsat.service.registry.SatelliteRegistry;
import com.hamradio.jsat.service.tle.TleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Real-time satellite tracking engine.
 *
 * Maintains live state for all enabled, TLE-matched satellites.
 * Propagates positions, computes AZ/EL, Doppler, and footprints.
 * Runs pass prediction asynchronously.
 */
public class SatelliteTracker {

    private static final Logger log = LoggerFactory.getLogger(SatelliteTracker.class);

    private final TleManager        tleManager;
    private final SatelliteRegistry registry;
    private JsatSettings            settings;

    // Active tracking state, keyed by satellite name
    private final Map<String, SatelliteState> currentStates = new ConcurrentHashMap<>();
    // Pass predictions per satellite name
    private final Map<String, List<SatellitePass>> passPredictions = new ConcurrentHashMap<>();
    // Per-satellite propagators (reuse to avoid re-init every second)
    private final Map<String, Sgp4Propagator> propagators = new ConcurrentHashMap<>();

    // Currently selected satellite for Doppler/rotor tracking
    private volatile String selectedSatName;

    private final ExecutorService passPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "jsat-pass-predict");
        t.setDaemon(true);
        return t;
    });

    private static final int GROUND_TRACK_STEPS = 90; // 90 × 60s = 90 min

    public SatelliteTracker(TleManager tleManager, SatelliteRegistry registry, JsatSettings settings) {
        this.tleManager = tleManager;
        this.registry   = registry;
        this.settings   = settings;
    }

    /**
     * Update all tracked satellite states. Called every second by the scheduler.
     */
    public void tick() {
        Instant now = Instant.now();
        double obsLat = settings.qthLat;
        double obsLon = settings.qthLon;
        double obsAlt = settings.qthAltKm;

        for (SatelliteDefinition def : getActiveSatellites()) {
            final TleSet tle = resolveTle(def);
            if (tle == null) continue;

            Sgp4Propagator prop = propagators.computeIfAbsent(def.name, k -> {
                Sgp4Propagator p = new Sgp4Propagator();
                p.init(tle);
                return p;
            });

            double[] eci = prop.propagate(now);
            if (eci == null) continue;

            double[] geo = CoordTransform.eciToGeodetic(
                new double[]{eci[0], eci[1], eci[2]}, now);
            double[] azel = CoordTransform.satAzElRange(
                new double[]{eci[0], eci[1], eci[2]},
                new double[]{eci[3], eci[4], eci[5]},
                obsLat, obsLon, obsAlt, now);

            double rangeRate = azel[3];
            boolean sunlit   = CoordTransform.inSunlight(
                new double[]{eci[0], eci[1], eci[2]}, now);

            long dopplerUp   = DopplerCalculator.dopplerShiftHz(def.uplinkHz, rangeRate);
            long dopplerDown = DopplerCalculator.dopplerShiftHz(def.downlinkHz, rangeRate);
            long corrUp      = def.isLinearTransponder()
                               ? DopplerCalculator.invertingUplinkCorrection(def.uplinkHz, rangeRate)
                               : DopplerCalculator.correctedFrequency(def.uplinkHz, rangeRate);
            long corrDown    = DopplerCalculator.correctedFrequency(def.downlinkHz, rangeRate);

            double footprint = CoordTransform.footprintRadiusDeg(geo[2]);
            double[][] track = computeGroundTrack(prop, now, obsLat);

            double nRad  = tle.meanMotion * 2 * Math.PI / 86400.0;
            double sma   = Math.cbrt(398600.4418 / (nRad * nRad));
            double peri  = sma * (1 - tle.eccentricity) - 6371.0;
            double apo   = sma * (1 + tle.eccentricity) - 6371.0;

            SatelliteState state = new SatelliteState(
                def.name, tle.noradId,
                geo[0], geo[1], geo[2],
                azel[1], azel[0], azel[2], rangeRate,
                dopplerUp, dopplerDown, corrUp, corrDown,
                sunlit, track, footprint, peri, apo);

            currentStates.put(def.name, state);
        }
    }

    /**
     * Re-initialize all propagators (called after TLE update).
     */
    public void reinitPropagators() {
        propagators.clear();
        log.debug("Propagators cleared for re-initialization");
    }

    /**
     * Run pass prediction for all enabled satellites in background.
     */
    public void predictPassesAsync() {
        passPool.submit(() -> {
            Instant now = Instant.now();
            double lat = settings.qthLat;
            double lon = settings.qthLon;
            double alt = settings.qthAltKm;

            PassPredictor predictor = new PassPredictor();
            for (SatelliteDefinition def : getActiveSatellites()) {
                TleSet tle = resolveTle(def);
                if (tle == null) continue;
                try {
                    List<SatellitePass> passes = predictor.predict(tle, lat, lon, alt, now);
                    passPredictions.put(def.name, passes);
                } catch (Exception e) {
                    log.debug("Pass prediction failed for {}: {}", def.name, e.getMessage());
                }
            }
            log.debug("Pass prediction complete for {} satellites", passPredictions.size());
        });
    }

    /** Returns all current satellite states (snapshot). */
    public Map<String, SatelliteState> getCurrentStates() {
        return Collections.unmodifiableMap(new HashMap<>(currentStates));
    }

    /** Returns all upcoming passes across all satellites, sorted by AOS. */
    public List<SatellitePass> getAllPasses() {
        List<SatellitePass> all = new ArrayList<>();
        passPredictions.values().forEach(all::addAll);
        Collections.sort(all);
        return all;
    }

    public List<SatellitePass> getPassesFor(String satName) {
        return passPredictions.getOrDefault(satName, List.of());
    }

    public SatelliteState getState(String satName) {
        return currentStates.get(satName);
    }

    public void setSelectedSatellite(String name) { this.selectedSatName = name; }
    public String getSelectedSatellite()          { return selectedSatName; }

    public SatelliteState getSelectedState() {
        return selectedSatName != null ? currentStates.get(selectedSatName) : null;
    }

    public void applySettings(JsatSettings settings) {
        this.settings = settings;
        propagators.clear();
    }

    private TleSet resolveTle(SatelliteDefinition def) {
        if (def.noradId > 0) {
            TleSet t = tleManager.findByNorad(def.noradId);
            if (t != null) return t;
        }
        return tleManager.findByName(def.name);
    }

    /**
     * Returns the satellites to track.
     * null enabledSatellites  → not yet configured; use registry JSON defaults.
     * empty enabledSatellites → user explicitly cleared; track nothing.
     * non-empty               → track exactly these named satellites.
     */
    private List<SatelliteDefinition> getActiveSatellites() {
        if (settings.enabledSatellites == null) return registry.getEnabled();
        if (settings.enabledSatellites.isEmpty()) return List.of();
        Set<String> sel = settings.enabledSatellites;
        return registry.getAll().stream().filter(d -> sel.contains(d.name)).toList();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private double[][] computeGroundTrack(Sgp4Propagator prop, Instant now, double obsLat) {
        double[][] track = new double[GROUND_TRACK_STEPS][2];
        for (int i = 0; i < GROUND_TRACK_STEPS; i++) {
            Instant t = now.plusSeconds(i * 60L);
            double[] eci = prop.propagate(t);
            if (eci != null) {
                double[] geo = CoordTransform.eciToGeodetic(
                    new double[]{eci[0], eci[1], eci[2]}, t);
                track[i][0] = geo[0]; // lat
                track[i][1] = geo[1]; // lon
            }
        }
        return track;
    }
}
