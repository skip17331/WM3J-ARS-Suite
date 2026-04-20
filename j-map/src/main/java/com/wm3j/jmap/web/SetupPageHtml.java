package com.wm3j.jmap.web;

import com.wm3j.jmap.service.config.Settings;

/**
 * Generates the complete HTML for the web-based Setup Page.
 * Mobile-friendly, single-page with live save via fetch API.
 * This page is ONLY accessible via the web server - never shown in the JavaFX UI.
 */
public class SetupPageHtml {

    public static String generate(Settings s) {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>J-Map Setup — """ + s.getCallsign() + """
</title>
<style>
  :root {
    --bg:       #0a0b12;
    --surface:  #0f1120;
    --surface2: #14182a;
    --border:   #1e2d50;
    --accent:   #2a7fff;
    --gold:     #ffd700;
    --green:    #00cc66;
    --red:      #ff4455;
    --text:     #ccd6f6;
    --muted:    #4a5580;
    --font:     'Liberation Mono', monospace;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: var(--bg);
    color: var(--text);
    font-family: var(--font);
    font-size: 14px;
    min-height: 100vh;
  }
  header {
    background: var(--surface);
    border-bottom: 1px solid var(--border);
    padding: 16px 24px;
    display: flex;
    align-items: center;
    gap: 16px;
    position: sticky; top: 0; z-index: 100;
  }
  header .logo { font-size: 24px; }
  header h1 { font-size: 18px; color: var(--gold); letter-spacing: 2px; }
  header .subtitle { font-size: 11px; color: var(--muted); margin-top: 2px; }
  header .badge {
    margin-left: auto;
    background: var(--green);
    color: #000;
    padding: 3px 10px;
    border-radius: 12px;
    font-size: 11px;
    font-weight: bold;
  }
  .container { max-width: 720px; margin: 0 auto; padding: 24px 16px 80px; }
  .section {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 8px;
    margin-bottom: 20px;
    overflow: hidden;
  }
  .section-header {
    background: var(--surface2);
    padding: 12px 16px;
    font-size: 11px;
    font-weight: bold;
    color: var(--muted);
    letter-spacing: 2px;
    border-bottom: 1px solid var(--border);
    text-transform: uppercase;
  }
  .field {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid var(--border);
    gap: 16px;
  }
  .field:last-child { border-bottom: none; }
  .field-label {
    flex: 1;
    font-size: 13px;
    color: var(--text);
    min-width: 140px;
  }
  .field-label .desc {
    font-size: 10px;
    color: var(--muted);
    margin-top: 2px;
    font-weight: normal;
  }
  /* Toggle switch */
  .toggle { position: relative; width: 48px; height: 26px; flex-shrink: 0; }
  .toggle input { opacity: 0; width: 0; height: 0; }
  .toggle-track {
    position: absolute; inset: 0;
    background: #1a2040;
    border-radius: 13px;
    cursor: pointer;
    border: 1px solid var(--border);
    transition: background 0.2s;
  }
  .toggle-track::after {
    content: '';
    position: absolute;
    left: 3px; top: 3px;
    width: 18px; height: 18px;
    background: var(--muted);
    border-radius: 50%;
    transition: transform 0.2s, background 0.2s;
  }
  .toggle input:checked + .toggle-track { background: #0d4a2a; border-color: var(--green); }
  .toggle input:checked + .toggle-track::after {
    transform: translateX(22px);
    background: var(--green);
  }
  /* Text inputs */
  input[type="text"], input[type="number"], select {
    background: #0a0c1a;
    border: 1px solid var(--border);
    border-radius: 4px;
    color: var(--text);
    font-family: var(--font);
    font-size: 13px;
    padding: 6px 10px;
    outline: none;
    transition: border-color 0.2s;
    width: 180px;
  }
  input[type="text"]:focus, input[type="number"]:focus, select:focus {
    border-color: var(--accent);
  }
  input[type="number"] { width: 100px; }
  input[type="range"] {
    width: 140px;
    accent-color: var(--accent);
  }
  select { cursor: pointer; }

  /* Save button */
  .save-bar {
    position: fixed; bottom: 0; left: 0; right: 0;
    background: var(--surface);
    border-top: 1px solid var(--border);
    padding: 16px 24px;
    display: flex;
    align-items: center;
    gap: 16px;
    z-index: 100;
  }
  .btn-save {
    background: var(--accent);
    color: white;
    border: none;
    border-radius: 6px;
    padding: 12px 32px;
    font-family: var(--font);
    font-size: 14px;
    font-weight: bold;
    cursor: pointer;
    letter-spacing: 1px;
    transition: background 0.15s, transform 0.1s;
  }
  .btn-save:hover { background: #3a8fff; }
  .btn-save:active { transform: scale(0.97); }
  .status-msg { font-size: 13px; color: var(--muted); }
  .status-msg.ok  { color: var(--green); }
  .status-msg.err { color: var(--red); }

  /* Status cards */
  .status-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 8px;
    padding: 16px;
  }
  .card {
    background: var(--surface2);
    border: 1px solid var(--border);
    border-radius: 6px;
    padding: 10px 14px;
  }
  .card .card-key   { font-size: 9px; color: var(--muted); letter-spacing: 1px; text-transform: uppercase; }
  .card .card-value { font-size: 16px; color: var(--gold); font-weight: bold; margin-top: 4px; }

  @media (max-width: 480px) {
    header h1 { font-size: 15px; }
    .field { flex-wrap: wrap; }
    input[type="text"] { width: 100%; }
  }
</style>
</head>
<body>

<header>
  <span class="logo">📻</span>
  <div>
    <h1>J-MAP SETUP</h1>
    <div class="subtitle">Configuration &amp; Preferences — """ + s.getCallsign() + """
</div>
  </div>
  <span class="badge">LIVE</span>
</header>

<div class="container">

  <!-- Live Status -->
  <div class="section">
    <div class="section-header">⚡ System Status</div>
    <div class="status-cards" id="statusCards">
      <div class="card"><div class="card-key">Callsign</div><div class="card-value" id="sc-call">""" + s.getCallsign() + """
</div></div>
      <div class="card"><div class="card-key">Data Mode</div><div class="card-value" id="sc-mode">""" + (s.isUseMockData() ? "MOCK" : "LIVE") + """
</div></div>
      <div class="card"><div class="card-key">Rotor</div><div class="card-value" id="sc-rotor">""" + (s.isRotorEnabled() ? "ENABLED" : "OFF") + """
</div></div>
    </div>
  </div>

  <!-- Operator Identity -->
  <div class="section">
    <div class="section-header">👤 Operator Identity</div>
    <div class="field">
      <div class="field-label">Callsign<div class="desc">Your amateur radio callsign</div></div>
      <input type="text" id="callsign" value=\"""" + s.getCallsign() + """
\" maxlength="12">
    </div>
    <div class="field">
      <div class="field-label">QTH Latitude<div class="desc">Decimal degrees (-90 to +90)</div></div>
      <input type="number" id="qthLat" value=\"""" + s.getQthLat() + """
\" step="0.001" min="-90" max="90">
    </div>
    <div class="field">
      <div class="field-label">QTH Longitude<div class="desc">Decimal degrees (-180 to +180)</div></div>
      <input type="number" id="qthLon" value=\"""" + s.getQthLon() + """
\" step="0.001" min="-180" max="180">
    </div>
    <div class="field">
      <div class="field-label">Grid Square<div class="desc">Maidenhead locator (e.g. FN31pr)</div></div>
      <input type="text" id="qthGrid" value=\"""" + s.getQthGrid() + """
\" maxlength="8">
    </div>
  </div>

  <!-- Data Sources -->
  <div class="section">
    <div class="section-header">🔌 Data Sources</div>
    <div class="field">
      <div class="field-label">Use Mock Data<div class="desc">Synthetic data (no internet required)</div></div>
      <label class="toggle"><input type="checkbox" id="useMockData" """ + (s.isUseMockData() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">NOAA API Key<div class="desc">Optional — live solar &amp; geomag data</div></div>
      <input type="text" id="noaaApiKey" value=\"""" + s.getNoaaApiKey() + """
\" placeholder="optional">
    </div>
    <div class="field">
      <div class="field-label">OpenWeather API Key<div class="desc">Required for live weather overlay</div></div>
      <input type="text" id="openWeatherApiKey" value=\"""" + s.getOpenWeatherApiKey() + """
\" placeholder="optional">
    </div>
  </div>

  <!-- Base Map -->
  <div class="section">
    <div class="section-header">🗺️ Base Map</div>
    <div class="field">
      <div class="field-label">World Map<div class="desc">Equirectangular flat projection</div></div>
      <label class="toggle"><input type="checkbox" id="showWorldMap" """ + (s.isShowWorldMap() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Grayline / Terminator<div class="desc">Day/night boundary overlay</div></div>
      <label class="toggle"><input type="checkbox" id="showGrayline" """ + (s.isShowGrayline() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Grayline Opacity</div>
      <input type="range" id="graylineOpacity" min="0.1" max="1.0" step="0.05" value=\"""" + s.getGraylineOpacity() + """
\">
    </div>
    <div class="field">
      <div class="field-label">DX Spots<div class="desc">Live spots from DX cluster / HTTP feed</div></div>
      <label class="toggle"><input type="checkbox" id="showDxSpots" """ + (s.isShowDxSpots() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">DX Band Filter</div>
      <select id="dxBandFilter">
        <option value="ALL" """ + (s.getDxBandFilter().equals("ALL") ? "selected" : "") + """
>All Bands</option>
        <option value="160m" """ + (s.getDxBandFilter().equals("160m") ? "selected" : "") + """
>160m</option>
        <option value="80m" """ + (s.getDxBandFilter().equals("80m") ? "selected" : "") + """
>80m</option>
        <option value="40m" """ + (s.getDxBandFilter().equals("40m") ? "selected" : "") + """
>40m</option>
        <option value="20m" """ + (s.getDxBandFilter().equals("20m") ? "selected" : "") + """
>20m</option>
        <option value="15m" """ + (s.getDxBandFilter().equals("15m") ? "selected" : "") + """
>15m</option>
        <option value="10m" """ + (s.getDxBandFilter().equals("10m") ? "selected" : "") + """
>10m</option>
        <option value="6m" """ + (s.getDxBandFilter().equals("6m") ? "selected" : "") + """
>6m</option>
      </select>
    </div>
    <div class="field">
      <div class="field-label">DX Max Age (min)<div class="desc">Hide spots older than this</div></div>
      <input type="number" id="dxMaxAgeMinutes" value=\"""" + s.getDxMaxAgeMinutes() + """
\" min="5" max="120">
    </div>
    <div class="field">
      <div class="field-label">Show Callsigns on Map</div>
      <label class="toggle"><input type="checkbox" id="dxShowCallsigns" """ + (s.isDxShowCallsigns() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
  </div>

  <!-- Space Weather -->
  <div class="section">
    <div class="section-header">🌌 Space Weather</div>
    <div class="field">
      <div class="field-label">Aurora Overlay<div class="desc">NOAA Ovation auroral oval</div></div>
      <label class="toggle"><input type="checkbox" id="showAuroraOverlay" """ + (s.isShowAuroraOverlay() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Geomagnetic Alerts<div class="desc">NOAA SWPC Kp alert rings on map</div></div>
      <label class="toggle"><input type="checkbox" id="showGeomagneticAlerts" """ + (s.isShowGeomagneticAlerts() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
  </div>

  <!-- Terrestrial Weather -->
  <div class="section">
    <div class="section-header">🌦️ Terrestrial Weather</div>
    <div class="field">
      <div class="field-label">Weather Overlay<div class="desc">Cloud cover / precipitation</div></div>
      <label class="toggle"><input type="checkbox" id="showWeatherOverlay" """ + (s.isShowWeatherOverlay() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Tropo Ducting Overlay<div class="desc">Hepburn VHF/UHF forecast</div></div>
      <label class="toggle"><input type="checkbox" id="showTropoOverlay" """ + (s.isShowTropoOverlay() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Radar Overlay<div class="desc">NOAA weather radar composite</div></div>
      <label class="toggle"><input type="checkbox" id="showRadarOverlay" """ + (s.isShowRadarOverlay() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Lightning Overlay<div class="desc">Real-time lightning strike density</div></div>
      <label class="toggle"><input type="checkbox" id="showLightningOverlay" """ + (s.isShowLightningOverlay() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Surface Conditions<div class="desc">Surface temperature / pressure map</div></div>
      <label class="toggle"><input type="checkbox" id="showSurfaceConditions" """ + (s.isShowSurfaceConditions() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
  </div>

  <!-- Amateur Radio -->
  <div class="section">
    <div class="section-header">📡 Amateur Radio Overlays</div>
    <div class="field">
      <div class="field-label">CQ Zones<div class="desc">CQ Magazine zone boundaries on map</div></div>
      <label class="toggle"><input type="checkbox" id="showCqZones" """ + (s.isShowCqZones() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">ITU Zones<div class="desc">International Telecommunication Union zones</div></div>
      <label class="toggle"><input type="checkbox" id="showItuZones" """ + (s.isShowItuZones() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Maidenhead Grid Squares<div class="desc">4-character field/square grid overlay</div></div>
      <label class="toggle"><input type="checkbox" id="showGridSquares" """ + (s.isShowGridSquares() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Satellite Tracking<div class="desc">Amateur satellite ground tracks (TLE)</div></div>
      <label class="toggle"><input type="checkbox" id="showSatelliteTracking" """ + (s.isShowSatelliteTracking() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
  </div>

  <!-- Movable Windows -->
  <div class="section">
    <div class="section-header">🪟 Movable Windows</div>
    <div class="field">
      <div class="field-label">Countdown Timer<div class="desc">10-min repeating timer, flashes at zero</div></div>
      <label class="toggle"><input type="checkbox" id="showCountdownTimer" """ + (s.isShowCountdownTimer() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Contest List<div class="desc">Upcoming contests from WA7BNM</div></div>
      <label class="toggle"><input type="checkbox" id="showContestList" """ + (s.isShowContestList() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
  </div>

  <!-- DE Window -->
  <div class="section">
    <div class="section-header">🏠 DE Window (Your Station)</div>
    <div class="field">
      <div class="field-label">Show DE Window<div class="desc">Floating panel showing your station info</div></div>
      <label class="toggle"><input type="checkbox" id="showDeWindow" """ + (s.isShowDeWindow() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field" style="flex-direction:column;align-items:flex-start;gap:4px;padding:12px 16px">
      <div style="font-size:10px;color:var(--muted);text-transform:uppercase;letter-spacing:1px">Displays when enabled:</div>
      <div style="font-size:12px;color:var(--text);margin-top:6px;line-height:2">
        Local Time &nbsp;·&nbsp; Lat / Lon &nbsp;·&nbsp; ITU Zone &nbsp;·&nbsp; CQ Zone &nbsp;·&nbsp; ARRL Section &nbsp;·&nbsp; Grid Square
      </div>
    </div>
  </div>

  <!-- DX Window -->
  <div class="section">
    <div class="section-header">🌍 DX Window</div>
    <div class="field">
      <div class="field-label">Show DX Window<div class="desc">Floating panel — click a spot on map to populate</div></div>
      <label class="toggle"><input type="checkbox" id="showDxWindow" """ + (s.isShowDxWindow() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field" style="flex-direction:column;align-items:flex-start;gap:4px;padding:12px 16px">
      <div style="font-size:10px;color:var(--muted);text-transform:uppercase;letter-spacing:1px">Displays when a DX spot is selected:</div>
      <div style="font-size:12px;color:var(--text);margin-top:6px;line-height:2">
        Callsign &nbsp;·&nbsp; DX Local Time &nbsp;·&nbsp; Lat / Lon &nbsp;·&nbsp; ITU Zone &nbsp;·&nbsp; CQ Zone &nbsp;·&nbsp; ARRL Section &nbsp;·&nbsp; Grid Square
      </div>
    </div>
  </div>

  <!-- Rotor Map -->
  <div class="section">
    <div class="section-header">🔄 Rotor Map</div>
    <div class="field">
      <div class="field-label">Show Rotor Map<div class="desc">Great-circle map in lower-right</div></div>
      <label class="toggle"><input type="checkbox" id="showRotorMap" """ + (s.isShowRotorMap() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Enable Arduino Rotor<div class="desc">Connect to IP-based rotor controller</div></div>
      <label class="toggle"><input type="checkbox" id="rotorEnabled" """ + (s.isRotorEnabled() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Arduino IP Address</div>
      <input type="text" id="arduinoIp" value=\"""" + s.getArduinoIp() + """
\" placeholder="192.168.1.100">
    </div>
    <div class="field">
      <div class="field-label">Arduino Port</div>
      <input type="number" id="arduinoPort" value=\"""" + s.getArduinoPort() + """
\" min="1" max="65535">
    </div>
    <div class="field">
      <div class="field-label">Protocol</div>
      <select id="arduinoProtocol">
        <option value="HTTP" """ + (s.getArduinoProtocol().equals("HTTP") ? "selected" : "") + """
>HTTP</option>
        <option value="UDP" """ + (s.getArduinoProtocol().equals("UDP") ? "selected" : "") + """
>UDP</option>
        <option value="WEBSOCKET" """ + (s.getArduinoProtocol().equals("WEBSOCKET") ? "selected" : "") + """
>WebSocket</option>
      </select>
    </div>
    <div class="field">
      <div class="field-label">Show Beam Width Arc</div>
      <label class="toggle"><input type="checkbox" id="showBeamWidthArc" """ + (s.isShowBeamWidthArc() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Beam Width (°)</div>
      <input type="number" id="beamWidthDegrees" value=\"""" + s.getBeamWidthDegrees() + """
\" min="5" max="180">
    </div>
    <div class="field">
      <div class="field-label">Show Long Path<div class="desc">Red dashed 180° bearing line</div></div>
      <label class="toggle"><input type="checkbox" id="showLongPath" """ + (s.isShowLongPath() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
  </div>

  <!-- Time Displays -->
  <div class="section">
    <div class="section-header">🕐 Time Displays</div>
    <div class="field">
      <div class="field-label">Show Local Time</div>
      <label class="toggle"><input type="checkbox" id="showLocalTime" """ + (s.isShowLocalTime() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Show UTC Time</div>
      <label class="toggle"><input type="checkbox" id="showUtcTime" """ + (s.isShowUtcTime() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Timezone<div class="desc">e.g. America/New_York, Europe/London</div></div>
      <input type="text" id="timezone" value=\"""" + s.getTimezone() + """
\" placeholder="UTC">
    </div>
  </div>

  <!-- Display -->
  <div class="section">
    <div class="section-header">🖥️ Display</div>
    <div class="field">
      <div class="field-label">Font Size<div class="desc">Base size for all text (10–22 px)</div></div>
      <div style="display:flex;align-items:center;gap:10px">
        <input type="range" id="fontSize" min="10" max="22" step="1" value=\"""" + s.getFontSize() + """
\" oninput="previewFontSize(this.value)">
        <span id="fontSizeVal" style="color:var(--gold);font-size:12px;min-width:32px">""" + s.getFontSize() + """
px</span>
      </div>
    </div>
  </div>

  <!-- Solar & Propagation -->
  <div class="section">
    <div class="section-header">☀️ Solar &amp; Propagation</div>
    <div class="field">
      <div class="field-label">Show Solar Data<div class="desc">SFI, Kp, A-index, X-ray</div></div>
      <label class="toggle"><input type="checkbox" id="showSolarData" """ + (s.isShowSolarData() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Show Sunspot Graphic<div class="desc">Visual sunspot activity chart</div></div>
      <label class="toggle"><input type="checkbox" id="showSunspotGraphic" """ + (s.isShowSunspotGraphic() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Show Propagation Data<div class="desc">FOT, MUF, LUF values</div></div>
      <label class="toggle"><input type="checkbox" id="showPropagationData" """ + (s.isShowPropagationData() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
    <div class="field">
      <div class="field-label">Show Band Conditions<div class="desc">Color-coded 80m–6m conditions</div></div>
      <label class="toggle"><input type="checkbox" id="showBandConditions" """ + (s.isShowBandConditions() ? "checked" : "") + """
><span class="toggle-track"></span></label>
    </div>
  </div>

</div><!-- /container -->

<!-- Save Bar -->
<div class="save-bar">
  <button class="btn-save" onclick="saveSettings()">💾  SAVE &amp; APPLY</button>
  <span class="status-msg" id="statusMsg">Changes will apply instantly to the main display.</span>
</div>

<script>
function collectSettings() {
  const g = (id) => document.getElementById(id);
  const chk = (id) => g(id) ? g(id).checked : false;
  const val = (id) => g(id) ? g(id).value : '';
  const num = (id) => g(id) ? parseFloat(g(id).value) : 0;
  const int = (id) => g(id) ? parseInt(g(id).value) : 0;

  return {
    callsign:               val('callsign'),
    qthLat:                 num('qthLat'),
    qthLon:                 num('qthLon'),
    qthGrid:                val('qthGrid'),
    timezone:               val('timezone'),
    useMockData:            chk('useMockData'),
    noaaApiKey:             val('noaaApiKey'),
    openWeatherApiKey:      val('openWeatherApiKey'),
    // Base Map
    showWorldMap:           chk('showWorldMap'),
    showGrayline:           chk('showGrayline'),
    graylineOpacity:        num('graylineOpacity'),
    showDxSpots:            chk('showDxSpots'),
    dxBandFilter:           val('dxBandFilter'),
    dxMaxAgeMinutes:        int('dxMaxAgeMinutes'),
    dxShowCallsigns:        chk('dxShowCallsigns'),
    // Space Weather
    showAuroraOverlay:      chk('showAuroraOverlay'),
    showGeomagneticAlerts:  chk('showGeomagneticAlerts'),
    // Terrestrial Weather
    showWeatherOverlay:     chk('showWeatherOverlay'),
    showTropoOverlay:       chk('showTropoOverlay'),
    showRadarOverlay:       chk('showRadarOverlay'),
    showLightningOverlay:   chk('showLightningOverlay'),
    showSurfaceConditions:  chk('showSurfaceConditions'),
    // Amateur Radio
    showCqZones:            chk('showCqZones'),
    showItuZones:           chk('showItuZones'),
    showGridSquares:        chk('showGridSquares'),
    showSatelliteTracking:  chk('showSatelliteTracking'),
    // Movable Windows
    showCountdownTimer:     chk('showCountdownTimer'),
    showContestList:        chk('showContestList'),
    // DE / DX Windows
    showDeWindow:           chk('showDeWindow'),
    showDxWindow:           chk('showDxWindow'),
    // Rotor
    showRotorMap:           chk('showRotorMap'),
    rotorEnabled:           chk('rotorEnabled'),
    arduinoIp:              val('arduinoIp'),
    arduinoPort:            int('arduinoPort'),
    arduinoProtocol:        val('arduinoProtocol'),
    showBeamWidthArc:       chk('showBeamWidthArc'),
    beamWidthDegrees:       num('beamWidthDegrees'),
    showLongPath:           chk('showLongPath'),
    // Display
    fontSize:               int('fontSize') || 13,
    // Time
    showLocalTime:          chk('showLocalTime'),
    showUtcTime:            chk('showUtcTime'),
    // Solar & Propagation
    showSolarData:          chk('showSolarData'),
    showSunspotGraphic:     chk('showSunspotGraphic'),
    showPropagationData:    chk('showPropagationData'),
    showBandConditions:     chk('showBandConditions'),
  };
}

async function saveSettings() {
  const msg = document.getElementById('statusMsg');
  msg.textContent = 'Saving...';
  msg.className = 'status-msg';

  try {
    const settings = collectSettings();
    const res = await fetch('/api/settings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(settings)
    });

    const data = await res.json();
    if (res.ok) {
      msg.textContent = '✓ Saved and applied to main display';
      msg.className = 'status-msg ok';
      document.getElementById('sc-call').textContent = settings.callsign;
      document.getElementById('sc-mode').textContent = settings.useMockData ? 'MOCK' : 'LIVE';
      document.getElementById('sc-rotor').textContent = settings.rotorEnabled ? 'ENABLED' : 'OFF';
    } else {
      throw new Error(data.message || 'Unknown error');
    }
  } catch (err) {
    msg.textContent = '✗ Error: ' + err.message;
    msg.className = 'status-msg err';
  }

  setTimeout(() => {
    msg.textContent = 'Changes will apply instantly to the main display.';
    msg.className = 'status-msg';
  }, 4000);
}

// Keyboard shortcut: Ctrl+S to save
document.addEventListener('keydown', (e) => {
  if (e.ctrlKey && e.key === 's') { e.preventDefault(); saveSettings(); }
});

// Live font-size preview while dragging the slider (debounced)
let fontSizeDebounce;
function previewFontSize(val) {
  document.getElementById('fontSizeVal').textContent = val + 'px';
  clearTimeout(fontSizeDebounce);
  fontSizeDebounce = setTimeout(() => autoApplyFontSize(parseInt(val)), 400);
}
async function autoApplyFontSize(size) {
  const settings = collectSettings();
  settings.fontSize = size;
  try {
    await fetch('/api/settings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(settings)
    });
  } catch (e) {}
}

</script>
</body>
</html>
""";
    }
}
