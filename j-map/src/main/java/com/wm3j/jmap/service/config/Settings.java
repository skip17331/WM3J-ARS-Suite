package com.wm3j.jmap.service.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Application settings - persisted as JSON.
 * All UI configuration is driven by these settings, which are
 * modified exclusively through the web-based Setup Page.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Settings {

    // Operator identity
    private String callsign = "W1AW";
    private double qthLat = 41.7148;
    private double qthLon = -72.7271;
    private String qthGrid = "FN31pr";
    private String timezone = "UTC";
    private String arrlSection = "";
    private int    cqZone     = 0;
    private int    ituZone    = 0;

    // Data source flags
    private boolean useMockData = true;
    private String noaaApiKey = "";
    private String openWeatherApiKey = "";

    // === MAP & OVERLAYS ===
    private boolean showWorldMap = true;
    private boolean showGrayline = true;
    private boolean showDxSpots = true;
    private boolean showDxPaths = true;
    private double  graylineOpacity = 0.6;
    private boolean showSunPosition = true;

    // === SPACE WEATHER OVERLAYS ===
    private boolean showAuroraOverlay = true;
    private boolean showGeomagneticAlerts = false;

    // === TERRESTRIAL WEATHER OVERLAYS ===
    private boolean showWeatherOverlay = false;
    private boolean showTropoOverlay = false;
    private boolean showRadarOverlay = false;
    private boolean showLightningOverlay = false;
    private boolean showFrontsOverlay = false;
    private boolean showSurfaceConditions = false;

    // === AMATEUR RADIO OVERLAYS ===
    private boolean showCqZones = false;
    private boolean showItuZones = false;
    private boolean showGridSquares = false;
    private boolean showSatelliteTracking = false;

    // === PROPAGATION MODEL ===
    private boolean showPropagationOverlay = false;
    private boolean showPropagationModelWindow = false;
    private double propagationModelWindowX = 10;
    private double propagationModelWindowY = 100;

    // === LUNAR / PLANETARY ===
    private boolean showLunarPlanetaryWindow = false;
    private boolean showMoonMarker = true;
    private double lunarWindowX = 10;
    private double lunarWindowY = 300;

    // === PSK REPORTER ===
    private boolean showPskOverlay = false;
    private String  pskCallsign = "";
    private String  pskBandFilter = "ALL";
    private int     pskMaxAgeMinutes = 30;

    // === MOVABLE WINDOWS ===
    private boolean showCountdownTimer = false;
    private boolean showContestList = false;

    // === DE WINDOW ===
    private boolean showDeWindow = true;

    // === DX WINDOW ===
    private boolean showDxWindow = false;
    private String dxWindowCallsign = "";

    // === FLOATING WINDOW POSITIONS ===
    private double countdownTimerX = 10;  private double countdownTimerY = 10;
    private double contestListX    = 10;  private double contestListY    = 220;
    private double deWindowX       = 10;  private double deWindowY       = 460;
    private double dxWindowX       = 250; private double dxWindowY       = 10;


    // DX spot filters
    private String dxBandFilter = "ALL";   // ALL, 160m, 80m, 40m, 20m, 15m, 10m, 6m
    private int dxMaxAgeMinutes = 30;
    private boolean dxShowCallsigns = true;

    // === ROTOR MAP ===
    private boolean showRotorMap = true;
    private boolean rotorEnabled = false;
    private String arduinoIp = "192.168.1.100";
    private int arduinoPort = 4533;
    private String arduinoProtocol = "HTTP";  // HTTP, UDP, WEBSOCKET
    private boolean showBeamWidthArc = true;
    private double beamWidthDegrees = 30.0;
    private boolean showLongPath = true;

    // === TIME DISPLAYS ===
    private boolean showLocalTime = true;
    private boolean showUtcTime = true;
    private String secondaryTimezone = "";

    // === SOLAR & PROPAGATION ===
    private boolean showSolarData = true;
    private boolean showSunspotGraphic = true;
    private boolean showPropagationData = true;
    private boolean showBandConditions = true;

    // Propagation settings
    private double fot = 14.0;   // MHz
    private double muf = 28.0;   // MHz

    // Map source and data
    private String mapStyle       = "BLUE_MARBLE";
    private String mapView        = "WORLD";
    private String tileProvider   = "FLAT";
    private int    mapZoom        = 2;
    private double mapCenterLat   = 0.0;
    private double mapCenterLon   = 0.0;
    private String tleSource      = "";
    private String jSatApiUrl     = "http://localhost:4540";
    private int    refreshSeconds = 30;

    // === J-Hub bootstrap (where to find the upstream broker) ===
    // These are read locally before we contact J-Hub — so they are NOT
    // overwritten when JMAP_CONFIG arrives. Persisting them here lets
    // J-Map run on a second machine pointed at the shack PC's J-Hub.
    private String jhubHost    = "localhost";
    private int    jhubWsPort  = 8080;
    private int    jhubWebPort = 8081;

    // UI preferences
    private boolean darkTheme = true;
    private double uiScale = 1.0;
    private int fontSize = 13;

    // Per-floating-window font sizes (override global, 0 = inherit)
    private int deInfoFontSize      = 0;
    private int dxInfoFontSize      = 0;
    private int contestListFontSize = 0;
    private int countdownFontSize   = 0;
    private int propagationFontSize = 0;
    private int lunarFontSize       = 0;

    // -------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------

    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign; }

    public double getQthLat() { return qthLat; }
    public void setQthLat(double qthLat) { this.qthLat = qthLat; }

    public double getQthLon() { return qthLon; }
    public void setQthLon(double qthLon) { this.qthLon = qthLon; }

    public String getQthGrid() { return qthGrid; }
    public void setQthGrid(String qthGrid) { this.qthGrid = qthGrid; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getArrlSection() { return arrlSection != null ? arrlSection : ""; }
    public void setArrlSection(String arrlSection) { this.arrlSection = arrlSection; }

    public int getCqZone() { return cqZone; }
    public void setCqZone(int cqZone) { this.cqZone = cqZone; }

    public int getItuZone() { return ituZone; }
    public void setItuZone(int ituZone) { this.ituZone = ituZone; }

    public boolean isUseMockData() { return useMockData; }
    public void setUseMockData(boolean useMockData) { this.useMockData = useMockData; }

    public String getNoaaApiKey() { return noaaApiKey != null ? noaaApiKey : ""; }
    public void setNoaaApiKey(String noaaApiKey) { this.noaaApiKey = noaaApiKey; }

    public String getOpenWeatherApiKey() { return openWeatherApiKey != null ? openWeatherApiKey : ""; }
    public void setOpenWeatherApiKey(String openWeatherApiKey) { this.openWeatherApiKey = openWeatherApiKey; }

    public boolean isShowWorldMap() { return showWorldMap; }
    public void setShowWorldMap(boolean showWorldMap) { this.showWorldMap = showWorldMap; }

    public boolean isShowGrayline() { return showGrayline; }
    public void setShowGrayline(boolean showGrayline) { this.showGrayline = showGrayline; }

    public boolean isShowDxSpots() { return showDxSpots; }
    public void setShowDxSpots(boolean showDxSpots) { this.showDxSpots = showDxSpots; }

    public boolean isShowAuroraOverlay() { return showAuroraOverlay; }
    public void setShowAuroraOverlay(boolean showAuroraOverlay) { this.showAuroraOverlay = showAuroraOverlay; }

    public boolean isShowGeomagneticAlerts() { return showGeomagneticAlerts; }
    public void setShowGeomagneticAlerts(boolean showGeomagneticAlerts) { this.showGeomagneticAlerts = showGeomagneticAlerts; }

    public boolean isShowWeatherOverlay() { return showWeatherOverlay; }
    public void setShowWeatherOverlay(boolean showWeatherOverlay) { this.showWeatherOverlay = showWeatherOverlay; }

    public boolean isShowTropoOverlay() { return showTropoOverlay; }
    public void setShowTropoOverlay(boolean showTropoOverlay) { this.showTropoOverlay = showTropoOverlay; }

    public boolean isShowRadarOverlay() { return showRadarOverlay; }
    public void setShowRadarOverlay(boolean showRadarOverlay) { this.showRadarOverlay = showRadarOverlay; }

    public boolean isShowLightningOverlay() { return showLightningOverlay; }
    public void setShowLightningOverlay(boolean showLightningOverlay) { this.showLightningOverlay = showLightningOverlay; }

    public boolean isShowFrontsOverlay() { return showFrontsOverlay; }
    public void setShowFrontsOverlay(boolean showFrontsOverlay) { this.showFrontsOverlay = showFrontsOverlay; }

    public boolean isShowSurfaceConditions() { return showSurfaceConditions; }
    public void setShowSurfaceConditions(boolean showSurfaceConditions) { this.showSurfaceConditions = showSurfaceConditions; }

    public boolean isShowCqZones() { return showCqZones; }
    public void setShowCqZones(boolean showCqZones) { this.showCqZones = showCqZones; }

    public boolean isShowItuZones() { return showItuZones; }
    public void setShowItuZones(boolean showItuZones) { this.showItuZones = showItuZones; }

    public boolean isShowGridSquares() { return showGridSquares; }
    public void setShowGridSquares(boolean showGridSquares) { this.showGridSquares = showGridSquares; }

    public boolean isShowSatelliteTracking() { return showSatelliteTracking; }
    public void setShowSatelliteTracking(boolean showSatelliteTracking) { this.showSatelliteTracking = showSatelliteTracking; }

    public boolean isShowCountdownTimer() { return showCountdownTimer; }
    public void setShowCountdownTimer(boolean showCountdownTimer) { this.showCountdownTimer = showCountdownTimer; }

    public boolean isShowContestList() { return showContestList; }
    public void setShowContestList(boolean showContestList) { this.showContestList = showContestList; }

    public boolean isShowDeWindow() { return showDeWindow; }
    public void setShowDeWindow(boolean showDeWindow) { this.showDeWindow = showDeWindow; }

    public boolean isShowDxWindow() { return showDxWindow; }
    public void setShowDxWindow(boolean showDxWindow) { this.showDxWindow = showDxWindow; }

    public boolean isShowPropagationOverlay() { return showPropagationOverlay; }
    public void setShowPropagationOverlay(boolean showPropagationOverlay) { this.showPropagationOverlay = showPropagationOverlay; }

    public boolean isShowPropagationModelWindow() { return showPropagationModelWindow; }
    public void setShowPropagationModelWindow(boolean showPropagationModelWindow) { this.showPropagationModelWindow = showPropagationModelWindow; }

    public double getPropagationModelWindowX() { return propagationModelWindowX; }
    public void setPropagationModelWindowX(double v) { propagationModelWindowX = v; }

    public double getPropagationModelWindowY() { return propagationModelWindowY; }
    public void setPropagationModelWindowY(double v) { propagationModelWindowY = v; }

    public boolean isShowLunarPlanetaryWindow() { return showLunarPlanetaryWindow; }
    public void setShowLunarPlanetaryWindow(boolean showLunarPlanetaryWindow) { this.showLunarPlanetaryWindow = showLunarPlanetaryWindow; }

    public boolean isShowMoonMarker() { return showMoonMarker; }
    public void setShowMoonMarker(boolean showMoonMarker) { this.showMoonMarker = showMoonMarker; }

    public double getLunarWindowX() { return lunarWindowX; }
    public void setLunarWindowX(double v) { lunarWindowX = v; }

    public double getLunarWindowY() { return lunarWindowY; }
    public void setLunarWindowY(double v) { lunarWindowY = v; }

    public boolean isShowPskOverlay() { return showPskOverlay; }
    public void setShowPskOverlay(boolean showPskOverlay) { this.showPskOverlay = showPskOverlay; }

    public String getPskCallsign() { return pskCallsign != null ? pskCallsign : ""; }
    public void setPskCallsign(String pskCallsign) { this.pskCallsign = pskCallsign; }

    public String getPskBandFilter() { return pskBandFilter != null ? pskBandFilter : "ALL"; }
    public void setPskBandFilter(String pskBandFilter) { this.pskBandFilter = pskBandFilter; }

    public int getPskMaxAgeMinutes() { return pskMaxAgeMinutes; }
    public void setPskMaxAgeMinutes(int pskMaxAgeMinutes) { this.pskMaxAgeMinutes = pskMaxAgeMinutes; }

    public double getCountdownTimerX() { return countdownTimerX; } public void setCountdownTimerX(double v) { countdownTimerX = v; }
    public double getCountdownTimerY() { return countdownTimerY; } public void setCountdownTimerY(double v) { countdownTimerY = v; }
    public double getContestListX()    { return contestListX; }    public void setContestListX(double v)    { contestListX = v; }
    public double getContestListY()    { return contestListY; }    public void setContestListY(double v)    { contestListY = v; }
    public double getDeWindowX()       { return deWindowX; }       public void setDeWindowX(double v)       { deWindowX = v; }
    public double getDeWindowY()       { return deWindowY; }       public void setDeWindowY(double v)       { deWindowY = v; }
    public double getDxWindowX()       { return dxWindowX; }       public void setDxWindowX(double v)       { dxWindowX = v; }
    public double getDxWindowY()       { return dxWindowY; }       public void setDxWindowY(double v)       { dxWindowY = v; }

    public String getDxWindowCallsign() { return dxWindowCallsign != null ? dxWindowCallsign : ""; }
    public void setDxWindowCallsign(String dxWindowCallsign) { this.dxWindowCallsign = dxWindowCallsign; }

    public double getGraylineOpacity() { return graylineOpacity; }
    public void setGraylineOpacity(double graylineOpacity) { this.graylineOpacity = graylineOpacity; }

    public boolean isShowSunPosition() { return showSunPosition; }
    public void setShowSunPosition(boolean showSunPosition) { this.showSunPosition = showSunPosition; }

    public String getDxBandFilter() { return dxBandFilter; }
    public void setDxBandFilter(String dxBandFilter) { this.dxBandFilter = dxBandFilter; }

    public int getDxMaxAgeMinutes() { return dxMaxAgeMinutes; }
    public void setDxMaxAgeMinutes(int dxMaxAgeMinutes) { this.dxMaxAgeMinutes = dxMaxAgeMinutes; }

    public boolean isDxShowCallsigns() { return dxShowCallsigns; }
    public void setDxShowCallsigns(boolean dxShowCallsigns) { this.dxShowCallsigns = dxShowCallsigns; }

    public boolean isShowRotorMap() { return showRotorMap; }
    public void setShowRotorMap(boolean showRotorMap) { this.showRotorMap = showRotorMap; }

    public boolean isRotorEnabled() { return rotorEnabled; }
    public void setRotorEnabled(boolean rotorEnabled) { this.rotorEnabled = rotorEnabled; }

    public String getArduinoIp() { return arduinoIp; }
    public void setArduinoIp(String arduinoIp) { this.arduinoIp = arduinoIp; }

    public int getArduinoPort() { return arduinoPort; }
    public void setArduinoPort(int arduinoPort) { this.arduinoPort = arduinoPort; }

    public String getArduinoProtocol() { return arduinoProtocol; }
    public void setArduinoProtocol(String arduinoProtocol) { this.arduinoProtocol = arduinoProtocol; }

    public boolean isShowBeamWidthArc() { return showBeamWidthArc; }
    public void setShowBeamWidthArc(boolean showBeamWidthArc) { this.showBeamWidthArc = showBeamWidthArc; }

    public double getBeamWidthDegrees() { return beamWidthDegrees; }
    public void setBeamWidthDegrees(double beamWidthDegrees) { this.beamWidthDegrees = beamWidthDegrees; }

    public boolean isShowLongPath() { return showLongPath; }
    public void setShowLongPath(boolean showLongPath) { this.showLongPath = showLongPath; }

    public boolean isShowLocalTime() { return showLocalTime; }
    public void setShowLocalTime(boolean showLocalTime) { this.showLocalTime = showLocalTime; }

    public boolean isShowUtcTime() { return showUtcTime; }
    public void setShowUtcTime(boolean showUtcTime) { this.showUtcTime = showUtcTime; }

    public String getSecondaryTimezone() { return secondaryTimezone != null ? secondaryTimezone : ""; }
    public void setSecondaryTimezone(String secondaryTimezone) { this.secondaryTimezone = secondaryTimezone; }

    public boolean isShowSolarData() { return showSolarData; }
    public void setShowSolarData(boolean showSolarData) { this.showSolarData = showSolarData; }

    public boolean isShowSunspotGraphic() { return showSunspotGraphic; }
    public void setShowSunspotGraphic(boolean showSunspotGraphic) { this.showSunspotGraphic = showSunspotGraphic; }

    public boolean isShowPropagationData() { return showPropagationData; }
    public void setShowPropagationData(boolean showPropagationData) { this.showPropagationData = showPropagationData; }

    public boolean isShowBandConditions() { return showBandConditions; }
    public void setShowBandConditions(boolean showBandConditions) { this.showBandConditions = showBandConditions; }

    public double getFot() { return fot; }
    public void setFot(double fot) { this.fot = fot; }

    public double getMuf() { return muf; }
    public void setMuf(double muf) { this.muf = muf; }

    public boolean isDarkTheme() { return darkTheme; }
    public void setDarkTheme(boolean darkTheme) { this.darkTheme = darkTheme; }

    public double getUiScale() { return uiScale; }
    public void setUiScale(double uiScale) { this.uiScale = uiScale; }

    public int getFontSize() { return (fontSize >= 10 && fontSize <= 22) ? fontSize : 13; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }

    public String getMapStyle() { return mapStyle != null && !mapStyle.isBlank() ? mapStyle : "BLUE_MARBLE"; }
    public void setMapStyle(String mapStyle) { this.mapStyle = mapStyle; }

    public String getMapView() { return mapView != null && !mapView.isBlank() ? mapView : "WORLD"; }
    public void setMapView(String mapView) { this.mapView = mapView; }

    public boolean isShowDxPaths() { return showDxPaths; }
    public void setShowDxPaths(boolean showDxPaths) { this.showDxPaths = showDxPaths; }

    public String getTileProvider() { return tileProvider != null ? tileProvider : "FLAT"; }
    public void setTileProvider(String tileProvider) { this.tileProvider = tileProvider; }

    public int getMapZoom() { return mapZoom > 0 ? mapZoom : 2; }
    public void setMapZoom(int mapZoom) { this.mapZoom = mapZoom; }

    public double getMapCenterLat() { return mapCenterLat; }
    public void setMapCenterLat(double mapCenterLat) { this.mapCenterLat = mapCenterLat; }

    public double getMapCenterLon() { return mapCenterLon; }
    public void setMapCenterLon(double mapCenterLon) { this.mapCenterLon = mapCenterLon; }

    public String getTleSource() { return tleSource != null ? tleSource : ""; }
    public void setTleSource(String tleSource) { this.tleSource = tleSource; }

    public String getJSatApiUrl() { return jSatApiUrl != null && !jSatApiUrl.isBlank() ? jSatApiUrl : "http://localhost:4540"; }
    public void setJSatApiUrl(String jSatApiUrl) { this.jSatApiUrl = jSatApiUrl; }

    public int getRefreshSeconds() { return refreshSeconds > 0 ? refreshSeconds : 30; }
    public void setRefreshSeconds(int refreshSeconds) { this.refreshSeconds = refreshSeconds; }

    public int getDeInfoFontSize()      { return deInfoFontSize; }
    public void setDeInfoFontSize(int v){ this.deInfoFontSize = v; }
    public int getDxInfoFontSize()      { return dxInfoFontSize; }
    public void setDxInfoFontSize(int v){ this.dxInfoFontSize = v; }
    public int getContestListFontSize()     { return contestListFontSize; }
    public void setContestListFontSize(int v){ this.contestListFontSize = v; }
    public int getCountdownFontSize()     { return countdownFontSize; }
    public void setCountdownFontSize(int v){ this.countdownFontSize = v; }
    public int getPropagationFontSize()     { return propagationFontSize; }
    public void setPropagationFontSize(int v){ this.propagationFontSize = v; }
    public int getLunarFontSize()     { return lunarFontSize; }
    public void setLunarFontSize(int v){ this.lunarFontSize = v; }

    public String getJhubHost()        { return jhubHost == null || jhubHost.isBlank() ? "localhost" : jhubHost; }
    public void   setJhubHost(String v){ this.jhubHost = v; }
    public int    getJhubWsPort()      { return jhubWsPort > 0 ? jhubWsPort : 8080; }
    public void   setJhubWsPort(int v) { this.jhubWsPort = v; }
    public int    getJhubWebPort()     { return jhubWebPort > 0 ? jhubWebPort : 8081; }
    public void   setJhubWebPort(int v){ this.jhubWebPort = v; }
}
