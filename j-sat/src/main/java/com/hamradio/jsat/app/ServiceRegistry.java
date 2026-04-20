package com.hamradio.jsat.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.hamradio.jsat.hub.JHubClient;
import com.hamradio.jsat.model.SatelliteState;
import com.hamradio.jsat.service.config.JsatSettings;
import com.hamradio.jsat.service.registry.SatelliteRegistry;
import com.hamradio.jsat.service.spaceweather.SpaceWeatherService;
import com.hamradio.jsat.service.tle.TleManager;
import com.hamradio.jsat.service.tracking.SatelliteTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Central service registry: wires together all J-Sat services and
 * manages the main scheduling loop.
 *
 * Rig Doppler and rotor control are delegated to J-Hub via SAT_DOPPLER
 * and SAT_ROTOR_CMD WebSocket messages — no direct Hamlib connections here.
 */
public class ServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistry.class);

    public final SatelliteRegistry   satRegistry;
    public final TleManager          tleManager;
    public final SatelliteTracker    tracker;
    public final SpaceWeatherService spaceWeather;
    public final JHubClient          hubClient;

    /** Last rotor position received from J-Hub (via ROTOR_STATUS). */
    public volatile double hubRotorAz = 0;
    public volatile double hubRotorEl = 0;

    /** Last RIG_STATUS frequency received from J-Hub. */
    public volatile long   hubRigFreqHz = 0;

    private volatile JsatSettings settings;
    private ScheduledExecutorService scheduler;
    private Runnable settingsChangedCallback;

    public ServiceRegistry(JsatSettings settings) {
        this.settings   = settings;
        satRegistry  = new SatelliteRegistry();
        tleManager   = new TleManager();
        tracker      = new SatelliteTracker(tleManager, satRegistry, settings);
        spaceWeather = new SpaceWeatherService();
        hubClient    = new JHubClient(settings.hubHost, settings.hubWsPort);

        hubClient.setOnRigStatus(this::onRigStatus);
        hubClient.setOnRotorStatus(this::onRotorStatus);
    }

    public void start() {
        log.info("Starting J-Sat services...");
        tleManager.initialize();
        tracker.predictPassesAsync();
        hubClient.start();

        scheduler = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "jsat-scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::tick,            0, 1,     TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::fetchSpaceWeather, 10, 300, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(tracker::predictPassesAsync, 30, 600, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::updateTles,      60, 43200, TimeUnit.SECONDS);

        log.info("J-Sat scheduler started");
    }

    public void stop() {
        log.info("Stopping J-Sat services...");
        if (scheduler != null) scheduler.shutdownNow();
        hubClient.disconnect();
    }

    public void onSettingsChanged(JsatSettings newSettings) {
        this.settings = newSettings;
        tracker.applySettings(newSettings);
        if (settingsChangedCallback != null) settingsChangedCallback.run();
        tracker.predictPassesAsync();
        log.info("Settings applied");
    }

    public JsatSettings getSettings() { return settings; }

    public void setSettingsChangedCallback(Runnable cb) { this.settingsChangedCallback = cb; }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void tick() {
        try {
            tracker.tick();
            SatelliteState sel = tracker.getSelectedState();
            if (sel != null) {
                // Route Doppler correction to J-Hub if enabled
                if (settings.rigControlEnabled && sel.isVisible()) {
                    hubClient.publishDopplerFreqs(
                        sel.correctedDownlinkHz,
                        sel.correctedUplinkHz,
                        null);
                }
                // Route rotor tracking to J-Hub if enabled
                if (settings.rotorControlEnabled && sel.isVisible()) {
                    hubClient.publishRotorCmd(sel.azimuthDeg, sel.elevationDeg);
                }
                // Publish full satellite state to J-Hub
                hubClient.publishSatState(sel);
            }
        } catch (Exception e) {
            log.debug("Tick error: {}", e.getMessage());
        }
    }

    private void fetchSpaceWeather() {
        try { spaceWeather.fetch(); }
        catch (Exception e) { log.warn("Space weather fetch failed: {}", e.getMessage()); }
    }

    private void updateTles() {
        try {
            tleManager.updateNow();
            tracker.reinitPropagators();
            tracker.predictPassesAsync();
        } catch (Exception e) {
            log.warn("TLE update failed: {}", e.getMessage());
        }
    }

    private void onRigStatus(JsonNode msg) {
        hubRigFreqHz = msg.path("frequency").asLong(0);
    }

    private void onRotorStatus(JsonNode msg) {
        hubRotorAz = msg.path("bearing").asDouble(0);
        hubRotorEl = msg.path("elevation").asDouble(0);
    }
}
