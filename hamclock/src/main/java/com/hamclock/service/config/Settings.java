package com.hamclock.service.config;

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

    // Data source flags
    private boolean useMockData = true;
    private String noaaApiKey = "";
    private String openWeatherApiKey = "";

    // === MAP & OVERLAYS ===
    private boolean showWorldMap = true;
    private boolean showGrayline = true;
    private boolean showDxSpots = true;
    private double graylineOpacity = 0.6;

    // === SPACE WEATHER OVERLAYS ===
    private boolean showAuroraOverlay = true;
    private boolean showGeomagneticAlerts = false;

    // === TERRESTRIAL WEATHER OVERLAYS ===
    private boolean showWeatherOverlay = false;
    private boolean showTropoOverlay = false;
    private boolean showRadarOverlay = false;
    private boolean showLightningOverlay = false;
    private boolean showSurfaceConditions = false;

    // === AMATEUR RADIO OVERLAYS ===
    private boolean showCqZones = false;
    private boolean showItuZones = false;
    private boolean showGridSquares = false;
    private boolean showSatelliteTracking = false;

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

    // UI preferences
    private boolean darkTheme = true;
    private double uiScale = 1.0;
    private int fontSize = 13;      // base font size in px (range 10–22)
    private int webServerPort = 8082;

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

    public int getWebServerPort() { return webServerPort; }
    public void setWebServerPort(int webServerPort) { this.webServerPort = webServerPort; }
}
