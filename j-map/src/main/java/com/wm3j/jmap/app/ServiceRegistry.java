package com.wm3j.jmap.app;

import com.wm3j.jmap.service.astronomy.GraylineService;
import com.wm3j.jmap.service.astronomy.SolarPositionService;
import com.wm3j.jmap.service.astronomy.SunriseSunsetService;
import com.wm3j.jmap.service.aurora.AuroraProvider;
import com.wm3j.jmap.service.aurora.MockAuroraProvider;
import com.wm3j.jmap.service.aurora.NoaaOvationProvider;
import com.wm3j.jmap.service.config.Settings;
import com.wm3j.jmap.service.contest.ContestListProvider;
import com.wm3j.jmap.service.contest.MockContestListProvider;
import com.wm3j.jmap.service.contest.WaBnmContestListProvider;
import com.wm3j.jmap.service.dx.DxClusterClient;
import com.wm3j.jmap.service.dx.DxSpotProvider;
import com.wm3j.jmap.service.dx.HttpDxSpotProvider;
import com.wm3j.jmap.service.dx.MockDxSpotProvider;
import com.wm3j.jmap.service.geomag.GeomagneticAlertProvider;
import com.wm3j.jmap.service.geomag.MockGeomagneticAlertProvider;
import com.wm3j.jmap.service.geomag.NoaaGeomagneticAlertProvider;
import com.wm3j.jmap.service.lightning.LiveLightningProvider;
import com.wm3j.jmap.service.lightning.LightningProvider;
import com.wm3j.jmap.service.lightning.MockLightningProvider;
import com.wm3j.jmap.service.propagation.HamQslPropagationProvider;
import com.wm3j.jmap.service.propagation.MockPropagationProvider;
import com.wm3j.jmap.service.propagation.PropagationDataProvider;
import com.wm3j.jmap.service.radar.MockRadarProvider;
import com.wm3j.jmap.service.radar.NoaaRadarProvider;
import com.wm3j.jmap.service.radar.RadarProvider;
import com.wm3j.jmap.service.rotor.ArduinoRotorHttpProvider;
import com.wm3j.jmap.service.rotor.MockRotorProvider;
import com.wm3j.jmap.service.rotor.RotorProvider;
import com.wm3j.jmap.service.satellite.CelestrakSatelliteProvider;
import com.wm3j.jmap.service.satellite.MockSatelliteProvider;
import com.wm3j.jmap.service.satellite.SatelliteProvider;
import com.wm3j.jmap.service.solar.MockSolarDataProvider;
import com.wm3j.jmap.service.solar.NoaaSolarDataProvider;
import com.wm3j.jmap.service.solar.SolarDataProvider;
import com.wm3j.jmap.service.surface.MockSurfaceConditionsProvider;
import com.wm3j.jmap.service.surface.OpenMeteoSurfaceProvider;
import com.wm3j.jmap.service.surface.SurfaceConditionsProvider;
import com.wm3j.jmap.service.tropo.HepburnTropoProvider;
import com.wm3j.jmap.service.tropo.MockTropoProvider;
import com.wm3j.jmap.service.tropo.TropoProvider;
import com.wm3j.jmap.service.weather.MockWeatherProvider;
import com.wm3j.jmap.service.weather.OpenWeatherMapProvider;
import com.wm3j.jmap.service.weather.WeatherProvider;
import com.wm3j.jmap.service.zones.ZoneLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Central service registry that manages lifecycle and dependency injection
 * for all data providers and astronomy services.
 */
public class ServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistry.class);

    private volatile Settings settings;

    // Astronomy (no external calls)
    public final SolarPositionService solarPositionService;
    public final GraylineService graylineService;
    public final SunriseSunsetService sunriseSunsetService;

    // Zone lookup (pure calculation, no external calls)
    public final ZoneLookupService zoneLookupService;

    // Data providers (swappable: mock vs real)
    public volatile SolarDataProvider          solarDataProvider;
    public volatile PropagationDataProvider    propagationDataProvider;
    public volatile AuroraProvider             auroraProvider;
    public volatile GeomagneticAlertProvider   geomagneticAlertProvider;
    public volatile WeatherProvider            weatherProvider;
    public volatile TropoProvider              tropoProvider;
    public volatile RadarProvider              radarProvider;
    public volatile LightningProvider          lightningProvider;
    public volatile SurfaceConditionsProvider  surfaceConditionsProvider;
    public volatile DxSpotProvider             dxSpotProvider;
    public volatile RotorProvider              rotorProvider;
    public volatile SatelliteProvider          satelliteProvider;
    public volatile ContestListProvider        contestListProvider;

    public final DxClusterClient dxClusterClient = new DxClusterClient();

    private ScheduledExecutorService scheduler;

    private Runnable settingsChangedCallback;

    public ServiceRegistry(Settings settings) {
        this.settings = settings;

        this.solarPositionService = new SolarPositionService();
        this.graylineService      = new GraylineService(solarPositionService);
        this.sunriseSunsetService = new SunriseSunsetService(solarPositionService);
        this.zoneLookupService    = new ZoneLookupService();

        rebuildProviders();
    }

    public void start() {
        dxClusterClient.start(); // connect to Hub for spot delivery
        refreshRotor();
        refreshSolar();
        refreshDxSpots();

        scheduler = Executors.newScheduledThreadPool(6, r -> {
            Thread t = new Thread(r, "j-map-scheduler");
            t.setDaemon(true);
            return t;
        });

        // Solar / geomag — every 15 min
        scheduler.scheduleAtFixedRate(this::refreshSolar,          0, 15, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::refreshGeomagAlerts,   2, 15, TimeUnit.MINUTES);

        // Propagation — every 10 min
        scheduler.scheduleAtFixedRate(this::refreshPropagation,    5, 10, TimeUnit.MINUTES);

        // DX spots — every 2 min
        scheduler.scheduleAtFixedRate(this::refreshDxSpots,       10,  2, TimeUnit.MINUTES);

        // Space/weather overlays — every 30 min
        scheduler.scheduleAtFixedRate(this::refreshAurora,        15, 30, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::refreshRadar,         20, 10, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::refreshLightning,      5,  5, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::refreshSurface,       25, 30, TimeUnit.MINUTES);

        // Satellite — every 60 sec (positions change rapidly)
        scheduler.scheduleAtFixedRate(this::refreshSatellites,     0, 60, TimeUnit.SECONDS);

        // Contest list — every 4 hours
        scheduler.scheduleAtFixedRate(this::refreshContests,       0,  4, TimeUnit.HOURS);

        // Rotor — every 1 second
        scheduler.scheduleAtFixedRate(this::refreshRotor,          0,  1, TimeUnit.SECONDS);

        log.info("Service scheduler started");
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
        dxClusterClient.disconnect();
    }

    public void onSettingsChanged(Settings newSettings) {
        this.settings = newSettings;
        rebuildProviders();
        if (settingsChangedCallback != null) settingsChangedCallback.run();
        log.info("Settings applied, providers rebuilt");
        // Re-fetch key providers immediately so panels don't go blank after a settings save
        scheduler.submit(() -> { refreshSolar(); refreshPropagation(); refreshContests(); });
    }

    public void setSettingsChangedCallback(Runnable callback) {
        this.settingsChangedCallback = callback;
    }

    private void rebuildProviders() {
        boolean mock = settings.isUseMockData();
        boolean hasNoaa = !settings.getNoaaApiKey().isBlank();
        boolean hasOwm  = !settings.getOpenWeatherApiKey().isBlank();

        solarDataProvider = (mock || !hasNoaa)
            ? new MockSolarDataProvider() : new NoaaSolarDataProvider();

        // HamQSL endpoint times out in most environments; always use mock propagation data
        propagationDataProvider = new MockPropagationProvider();

        auroraProvider = mock
            ? new MockAuroraProvider() : new NoaaOvationProvider();

        geomagneticAlertProvider = (mock || !hasNoaa)
            ? new MockGeomagneticAlertProvider() : new NoaaGeomagneticAlertProvider();

        weatherProvider = (mock || !hasOwm)
            ? new MockWeatherProvider() : new OpenWeatherMapProvider(settings.getOpenWeatherApiKey());

        tropoProvider = mock
            ? new MockTropoProvider() : new HepburnTropoProvider();

        radarProvider = mock
            ? new MockRadarProvider() : new NoaaRadarProvider();

        lightningProvider = mock
            ? new MockLightningProvider() : new LiveLightningProvider();

        surfaceConditionsProvider = mock
            ? new MockSurfaceConditionsProvider() : new OpenMeteoSurfaceProvider();

        dxSpotProvider = mock
            ? new MockDxSpotProvider() : new HttpDxSpotProvider();

        satelliteProvider = mock
            ? new MockSatelliteProvider() : new CelestrakSatelliteProvider();

        // WaBnmContestListProvider scraper is unreliable; always use mock contest data
        contestListProvider = new MockContestListProvider();

        rotorProvider = (!settings.isRotorEnabled() || settings.getArduinoIp().isBlank())
            ? new MockRotorProvider()
            : new ArduinoRotorHttpProvider(settings.getArduinoIp(), settings.getArduinoPort());
    }

    // ── Refresh helpers ────────────────────────────────────────

    private void refreshSolar() {
        try { solarDataProvider.fetch(); }
        catch (Exception e) { log.warn("Solar refresh failed: {}", e.getMessage()); }
    }

    private void refreshGeomagAlerts() {
        try { geomagneticAlertProvider.fetch(); }
        catch (Exception e) { log.warn("Geomag refresh failed: {}", e.getMessage()); }
    }

    private void refreshPropagation() {
        try { propagationDataProvider.fetch(); }
        catch (Exception e) { log.warn("Propagation refresh failed: {}", e.getMessage()); }
    }

    private void refreshDxSpots() {
        try { dxSpotProvider.fetch(); }
        catch (Exception e) { log.warn("DX spots refresh failed: {}", e.getMessage()); }
    }

    private void refreshAurora() {
        try { auroraProvider.fetch(); }
        catch (Exception e) { log.warn("Aurora refresh failed: {}", e.getMessage()); }
    }

    private void refreshRadar() {
        try { radarProvider.fetch(); }
        catch (Exception e) { log.warn("Radar refresh failed: {}", e.getMessage()); }
    }

    private void refreshLightning() {
        try { lightningProvider.fetch(); }
        catch (Exception e) { log.warn("Lightning refresh failed: {}", e.getMessage()); }
    }

    private void refreshSurface() {
        try { surfaceConditionsProvider.fetch(); }
        catch (Exception e) { log.warn("Surface conditions refresh failed: {}", e.getMessage()); }
    }

    private void refreshSatellites() {
        try { satelliteProvider.fetch(); }
        catch (Exception e) { log.warn("Satellite refresh failed: {}", e.getMessage()); }
    }

    private void refreshContests() {
        try { contestListProvider.fetch(); }
        catch (Exception e) { log.warn("Contest list refresh failed: {}", e.getMessage()); }
    }

    private void refreshRotor() {
        try { rotorProvider.fetch(); }
        catch (Exception e) { /* Rotor offline - normal when not connected */ }
    }

    public Settings getSettings() { return settings; }
}
