/* ── Ham Radio Hub — Config UI Script ─────────────────────────────── */

// ---------------------------------------------------------------
// Globals
// ---------------------------------------------------------------

let config     = null;  // current config object
let ws         = null;  // live WebSocket connection to the hub

const BANDS = ["160m","80m","40m","30m","20m","17m","15m","12m","10m","6m"];
const MODES = ["SSB","CW","FT8","FT4","RTTY","PSK31","JS8","AM","FM"];

// ---------------------------------------------------------------
// Startup
// ---------------------------------------------------------------

document.addEventListener('DOMContentLoaded', () => {
  initTabs();
  buildCheckboxGroups();
  fetchConfig();
  fetchStatus();
  fetchSpots();
  connectWebSocket();
  fetchAppStatus();

  // Periodic refresh
  setInterval(fetchStatus,    2000);
  setInterval(fetchSpots,     5000);
  setInterval(fetchAppStatus, 3000);
});

// ---------------------------------------------------------------
// Tabs
// ---------------------------------------------------------------

function initTabs() {
  document.querySelectorAll('.tab').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(s => s.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
    });
  });
}

// ---------------------------------------------------------------
// Checkbox groups for bands and modes
// ---------------------------------------------------------------

function buildCheckboxGroups() {
  buildCheckGroup('bandFilter', BANDS);
  buildCheckGroup('modeFilter', MODES);
}

function buildCheckGroup(containerId, items) {
  const el = document.getElementById(containerId);
  el.innerHTML = '';
  items.forEach(item => {
    const label = document.createElement('label');
    const cb = document.createElement('input');
    cb.type = 'checkbox';
    cb.value = item;
    cb.id = containerId + '_' + item;
    label.appendChild(cb);
    label.appendChild(document.createTextNode(' ' + item));
    el.appendChild(label);
  });
}

function getChecked(containerId) {
  const checked = [];
  document.querySelectorAll(`#${containerId} input[type=checkbox]:checked`)
    .forEach(cb => checked.push(cb.value));
  return checked;
}

function setChecked(containerId, values) {
  const set = new Set(values || []);
  document.querySelectorAll(`#${containerId} input[type=checkbox]`)
    .forEach(cb => { cb.checked = set.has(cb.value); });
}

// ---------------------------------------------------------------
// Config load / save
// ---------------------------------------------------------------

async function fetchConfig() {
  try {
    const res = await fetch('/api/config');
    config = await res.json();
    populateForm(config);
  } catch (e) {
    showSaveMsg('Failed to load config: ' + e.message, true);
  }
}

function populateForm(cfg) {
  // Station
  val('callsign',       cfg.station?.callsign   || '');
  val('lat',            cfg.station?.lat        || '');
  val('lon',            cfg.station?.lon        || '');
  val('gridSquare',     cfg.station?.gridSquare || '');
  val('timezone',       cfg.station?.timezone   || 'UTC');
  val('wsPort',         cfg.hub?.websocketPort  || 8080);
  val('webPort',        cfg.hub?.webConfigPort  || 8081);

  // Cluster
  val('clusterServer',  cfg.cluster?.server         || '');
  val('clusterPort',    cfg.cluster?.port           || 7373);
  val('clusterLogin',   cfg.cluster?.loginCallsign  || '');
  setChecked('bandFilter', cfg.cluster?.filters?.bands || BANDS);
  setChecked('modeFilter', cfg.cluster?.filters?.modes || MODES);

  // Display
  val('mapStyle',       cfg.infoScreen?.mapStyle        || 'dark', true);
  check('showGreatCircle', cfg.infoScreen?.showGreatCircle ?? true);
  val('spotTimeout',    cfg.infoScreen?.spotTimeout     || 30);
  val('maxCachedSpots', cfg.infoScreen?.maxCachedSpots  || 50);

  // App Launcher
  val('launchCmdHamclock',  cfg.apps?.hamclock?.command    || '');
  check('launchAutoHamclock', cfg.apps?.hamclock?.autoLaunch ?? false);
  val('launchCmdHamlog',    cfg.apps?.hamlog?.command      || '');
  check('launchAutoHamlog',   cfg.apps?.hamlog?.autoLaunch  ?? false);
}

function collectConfig() {
  if (!config) config = {};

  config.hub = {
    websocketPort: parseInt(val('wsPort'))  || 8080,
    webConfigPort: parseInt(val('webPort')) || 8081,
  };

  config.station = {
    callsign:  val('callsign'),
    lat:       parseFloat(val('lat'))  || 0,
    lon:       parseFloat(val('lon'))  || 0,
    gridSquare: val('gridSquare'),
    timezone:  val('timezone') || 'UTC',
  };

  config.cluster = {
    server:        val('clusterServer'),
    port:          parseInt(val('clusterPort')) || 7373,
    loginCallsign: val('clusterLogin'),
    filters: {
      bands: getChecked('bandFilter'),
      modes: getChecked('modeFilter'),
    },
  };

  config.infoScreen = {
    mapStyle:       val('mapStyle'),
    showGreatCircle: document.getElementById('showGreatCircle').checked,
    spotTimeout:    parseInt(val('spotTimeout'))    || 30,
    maxCachedSpots: parseInt(val('maxCachedSpots')) || 50,
  };

  config.apps = {
    hamclock: {
      command:     val('launchCmdHamclock'),
      autoLaunch:  document.getElementById('launchAutoHamclock').checked,
    },
    hamlog: {
      command:     val('launchCmdHamlog'),
      autoLaunch:  document.getElementById('launchAutoHamlog').checked,
    },
  };

  return config;
}

async function saveConfig() {
  const cfg = collectConfig();
  try {
    const res = await fetch('/api/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(cfg),
    });
    const result = await res.json();
    if (result.status === 'saved') {
      showSaveMsg('✓ Configuration saved', false);
    } else {
      showSaveMsg('Error: ' + (result.error || 'unknown'), true);
    }
  } catch (e) {
    showSaveMsg('Save failed: ' + e.message, true);
  }
}

function showSaveMsg(msg, isErr) {
  const el = document.getElementById('saveMsg');
  el.textContent = msg;
  el.className = 'save-msg' + (isErr ? ' err' : '');
  setTimeout(() => { el.textContent = ''; }, 4000);
}

// ---------------------------------------------------------------
// Status bar
// ---------------------------------------------------------------

async function fetchStatus() {
  try {
    const s = await (await fetch('/api/status')).json();

    const clEl = document.getElementById('clusterStatus');
    if (s.clusterConnected) {
      clEl.textContent = '● Cluster: Connected';
      clEl.className = 'badge badge-ok';
    } else {
      clEl.textContent = '○ Cluster: Disconnected';
      clEl.className = 'badge badge-err';
    }

    document.getElementById('spotRate').textContent =
      (s.spotsPerMinute || 0).toFixed(0) + ' spots/min';

    if (s.rig) {
      const freq = (s.rig.frequency / 1e6).toFixed(3);
      document.getElementById('rigStatus').textContent =
        `${freq} MHz  ${s.rig.mode}  ${s.rig.power}W`;
    }
  } catch (_) {}
}

// ---------------------------------------------------------------
// Spots table
// ---------------------------------------------------------------

async function fetchSpots() {
  try {
    const spots = await (await fetch('/api/spots')).json();
    document.getElementById('spotCount').textContent = spots.length + ' spots';
    const tbody = document.getElementById('spotTableBody');
    tbody.innerHTML = '';
    // Show newest first
    [...spots].reverse().forEach(spot => {
      const tr = document.createElement('tr');
      const freqMhz = ((spot.frequency || 0) / 1e6).toFixed(3);
      const time = spot.localTimeAtSpot || spot.timestamp?.substring(11,16) || '—';
      tr.innerHTML = `
        <td class="callsign">${esc(spot.spotted)}</td>
        <td>${esc(spot.spotter)}</td>
        <td>${freqMhz}</td>
        <td>${esc(spot.mode || '—')}</td>
        <td class="country">${esc(spot.country || '—')}</td>
        <td>${spot.bearing != null ? spot.bearing.toFixed(0)+'°' : '—'}</td>
        <td>${spot.distanceKm != null ? Math.round(spot.distanceKm) : '—'}</td>
        <td>${esc(time)}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (_) {}
}

// ---------------------------------------------------------------
// WebSocket — live updates from hub
// ---------------------------------------------------------------

function connectWebSocket() {
  const wsPort = parseInt(document.location.port) - 1 || 8080; // guess
  // Attempt to connect to hub WS port (default 8080)
  try {
    ws = new WebSocket(`ws://localhost:${wsPort}`);
    ws.onopen = () => {
      ws.send(JSON.stringify({ type: 'APP_CONNECTED', appName: 'webconfig', version: '1.0' }));
    };
    ws.onmessage = e => handleWsMessage(JSON.parse(e.data));
    ws.onclose   = () => setTimeout(connectWebSocket, 5000);
  } catch (_) {}
}

function handleWsMessage(msg) {
  switch (msg.type) {
    case 'APP_LIST':
      renderAppList(msg.apps || []);
      break;
    case 'RIG_STATUS': {
      const freq = ((msg.frequency || 0) / 1e6).toFixed(3);
      document.getElementById('rigStatus').textContent =
        `${freq} MHz  ${msg.mode}  ${msg.power}W`;
      break;
    }
    case 'SPOT':
      prependSpot(msg);
      break;
    case 'CLUSTER_RAW':
      appendRawLine(msg.line);
      break;
  }
}

function renderAppList(apps) {
  const container = document.getElementById('appList');
  if (!apps.length) {
    container.innerHTML = '<p class="empty-msg">No apps connected</p>';
    return;
  }
  container.innerHTML = '';
  apps.forEach(app => {
    const card = document.createElement('div');
    card.className = 'app-card';
    const connTime = app.connectedAt ? new Date(app.connectedAt).toLocaleTimeString() : '—';
    card.innerHTML = `
      <div class="app-name">${esc(app.appName)}</div>
      <div class="app-meta">v${esc(app.version || '?')} · since ${connTime}</div>
    `;
    container.appendChild(card);
  });
}

function prependSpot(spot) {
  const tbody = document.getElementById('spotTableBody');
  const tr = document.createElement('tr');
  const freqMhz = ((spot.frequency || 0) / 1e6).toFixed(3);
  const time = spot.localTimeAtSpot || spot.timestamp?.substring(11,16) || '—';
  tr.innerHTML = `
    <td class="callsign">${esc(spot.spotted)}</td>
    <td>${esc(spot.spotter)}</td>
    <td>${freqMhz}</td>
    <td>${esc(spot.mode || '—')}</td>
    <td class="country">${esc(spot.country || '—')}</td>
    <td>${spot.bearing != null ? spot.bearing.toFixed(0)+'°' : '—'}</td>
    <td>${spot.distanceKm != null ? Math.round(spot.distanceKm) : '—'}</td>
    <td>${esc(time)}</td>
  `;
  tr.style.animation = 'fadeIn .4s';
  tbody.insertBefore(tr, tbody.firstChild);
  // Cap at 200 rows in the DOM
  while (tbody.children.length > 200) tbody.removeChild(tbody.lastChild);
  // Update count badge
  const cnt = document.getElementById('spotCount');
  cnt.textContent = tbody.children.length + ' spots';
}

// ---------------------------------------------------------------
// HamClock settings
// Settings are persisted in hub.json via /api/hamclock.
// When HamClock is running at localhost:8082, live settings are
// fetched from there first; on save they are pushed to both.
// ---------------------------------------------------------------

const HC_LIVE_API = 'http://localhost:8082/api/settings';
const HC_HUB_API  = '/api/hamclock';

async function fetchHamClock() {
  const badge = document.getElementById('hamclockStatusBadge');
  let s    = null;
  let live = false;

  // 1. Try the live HamClock app
  try {
    const res = await fetch(HC_LIVE_API, { signal: AbortSignal.timeout(2000) });
    if (res.ok) { s = await res.json(); live = true; }
  } catch (_) {}

  // 2. Fall back to hub-stored settings
  if (!s) {
    try {
      const res = await fetch(HC_HUB_API);
      if (res.ok) { const j = await res.json(); if (Object.keys(j).length) s = j; }
    } catch (_) {}
  }

  if (badge) {
    badge.textContent = live ? '● Live' : '○ Offline — showing saved settings';
    badge.className   = 'badge ' + (live ? 'badge-ok' : 'badge-warn');
  }

  if (s) populateHamClock(s);
}

function populateHamClock(s) {
  // Identity
  hcVal  ('hc-callsign',             s.callsign || '');
  hcVal  ('hc-qthLat',               s.qthLat);
  hcVal  ('hc-qthLon',               s.qthLon);
  hcVal  ('hc-qthGrid',              s.qthGrid || '');
  hcVal  ('hc-timezone',             s.timezone || '');
  // Data Sources
  hcCheck('hc-useMockData',          s.useMockData);
  hcVal  ('hc-noaaApiKey',           s.noaaApiKey || '');
  hcVal  ('hc-openWeatherApiKey',    s.openWeatherApiKey || '');
  // Base Map
  hcCheck('hc-showWorldMap',         s.showWorldMap);
  hcCheck('hc-showGrayline',         s.showGrayline);
  hcVal  ('hc-graylineOpacity',      s.graylineOpacity);
  hcCheck('hc-showDxSpots',          s.showDxSpots);
  hcVal  ('hc-dxBandFilter',         s.dxBandFilter);
  hcVal  ('hc-dxMaxAgeMinutes',      s.dxMaxAgeMinutes);
  hcCheck('hc-dxShowCallsigns',      s.dxShowCallsigns);
  // Space Weather
  hcCheck('hc-showAuroraOverlay',    s.showAuroraOverlay);
  hcCheck('hc-showGeomagneticAlerts',s.showGeomagneticAlerts);
  // Terrestrial Weather
  hcCheck('hc-showWeatherOverlay',   s.showWeatherOverlay);
  hcCheck('hc-showTropoOverlay',     s.showTropoOverlay);
  hcCheck('hc-showRadarOverlay',     s.showRadarOverlay);
  hcCheck('hc-showLightningOverlay', s.showLightningOverlay);
  hcCheck('hc-showSurfaceConditions',s.showSurfaceConditions);
  // Amateur Radio
  hcCheck('hc-showCqZones',          s.showCqZones);
  hcCheck('hc-showItuZones',         s.showItuZones);
  hcCheck('hc-showGridSquares',      s.showGridSquares);
  hcCheck('hc-showSatelliteTracking',s.showSatelliteTracking);
  // Movable Windows
  hcCheck('hc-showCountdownTimer',   s.showCountdownTimer);
  hcCheck('hc-showContestList',      s.showContestList);
  hcCheck('hc-showDeWindow',         s.showDeWindow);
  hcCheck('hc-showDxWindow',         s.showDxWindow);
  // Rotor
  hcCheck('hc-showRotorMap',         s.showRotorMap);
  hcCheck('hc-rotorEnabled',         s.rotorEnabled);
  hcVal  ('hc-arduinoIp',            s.arduinoIp || '');
  hcVal  ('hc-arduinoPort',          s.arduinoPort);
  hcVal  ('hc-arduinoProtocol',      s.arduinoProtocol || 'HTTP');
  hcCheck('hc-showBeamWidthArc',     s.showBeamWidthArc);
  hcVal  ('hc-beamWidthDegrees',     s.beamWidthDegrees);
  hcCheck('hc-showLongPath',         s.showLongPath);
  // Time & Display
  hcCheck('hc-showLocalTime',        s.showLocalTime);
  hcCheck('hc-showUtcTime',          s.showUtcTime);
  hcCheck('hc-darkTheme',            s.darkTheme);
  hcVal  ('hc-fontSize',             s.fontSize);
  // Solar & Propagation
  hcCheck('hc-showSolarData',        s.showSolarData);
  hcCheck('hc-showSunspotGraphic',   s.showSunspotGraphic);
  hcCheck('hc-showPropagationData',  s.showPropagationData);
  hcCheck('hc-showBandConditions',   s.showBandConditions);
}

function collectHamClock() {
  return {
    // Identity
    callsign:              hcVal('hc-callsign'),
    qthLat:                parseFloat(hcVal('hc-qthLat')) || 0,
    qthLon:                parseFloat(hcVal('hc-qthLon')) || 0,
    qthGrid:               hcVal('hc-qthGrid'),
    timezone:              hcVal('hc-timezone'),
    // Data Sources
    useMockData:           hcCheck('hc-useMockData'),
    noaaApiKey:            hcVal('hc-noaaApiKey'),
    openWeatherApiKey:     hcVal('hc-openWeatherApiKey'),
    // Base Map
    showWorldMap:          hcCheck('hc-showWorldMap'),
    showGrayline:          hcCheck('hc-showGrayline'),
    graylineOpacity:       parseFloat(hcVal('hc-graylineOpacity')),
    showDxSpots:           hcCheck('hc-showDxSpots'),
    dxBandFilter:          hcVal('hc-dxBandFilter'),
    dxMaxAgeMinutes:       parseInt(hcVal('hc-dxMaxAgeMinutes')),
    dxShowCallsigns:       hcCheck('hc-dxShowCallsigns'),
    // Space Weather
    showAuroraOverlay:     hcCheck('hc-showAuroraOverlay'),
    showGeomagneticAlerts: hcCheck('hc-showGeomagneticAlerts'),
    // Terrestrial Weather
    showWeatherOverlay:    hcCheck('hc-showWeatherOverlay'),
    showTropoOverlay:      hcCheck('hc-showTropoOverlay'),
    showRadarOverlay:      hcCheck('hc-showRadarOverlay'),
    showLightningOverlay:  hcCheck('hc-showLightningOverlay'),
    showSurfaceConditions: hcCheck('hc-showSurfaceConditions'),
    // Amateur Radio
    showCqZones:           hcCheck('hc-showCqZones'),
    showItuZones:          hcCheck('hc-showItuZones'),
    showGridSquares:       hcCheck('hc-showGridSquares'),
    showSatelliteTracking: hcCheck('hc-showSatelliteTracking'),
    // Movable Windows
    showCountdownTimer:    hcCheck('hc-showCountdownTimer'),
    showContestList:       hcCheck('hc-showContestList'),
    showDeWindow:          hcCheck('hc-showDeWindow'),
    showDxWindow:          hcCheck('hc-showDxWindow'),
    // Rotor
    showRotorMap:          hcCheck('hc-showRotorMap'),
    rotorEnabled:          hcCheck('hc-rotorEnabled'),
    arduinoIp:             hcVal('hc-arduinoIp'),
    arduinoPort:           parseInt(hcVal('hc-arduinoPort')),
    arduinoProtocol:       hcVal('hc-arduinoProtocol'),
    showBeamWidthArc:      hcCheck('hc-showBeamWidthArc'),
    beamWidthDegrees:      parseFloat(hcVal('hc-beamWidthDegrees')),
    showLongPath:          hcCheck('hc-showLongPath'),
    // Time & Display
    showLocalTime:         hcCheck('hc-showLocalTime'),
    showUtcTime:           hcCheck('hc-showUtcTime'),
    darkTheme:             hcCheck('hc-darkTheme'),
    fontSize:              parseInt(hcVal('hc-fontSize')) || 13,
    // Solar & Propagation
    showSolarData:         hcCheck('hc-showSolarData'),
    showSunspotGraphic:    hcCheck('hc-showSunspotGraphic'),
    showPropagationData:   hcCheck('hc-showPropagationData'),
    showBandConditions:    hcCheck('hc-showBandConditions'),
  };
}

async function saveHamClock() {
  const cfg = collectHamClock();
  const el  = document.getElementById('hamclockSaveMsg');
  let hubOk  = false;
  let liveOk = false;

  // Always persist to hub
  try {
    const r = await fetch(HC_HUB_API, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(cfg),
    });
    hubOk = (await r.json()).status === 'saved';
  } catch (_) {}

  // Also push to live HamClock if reachable
  try {
    const r = await fetch(HC_LIVE_API, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(cfg),
      signal: AbortSignal.timeout(2000),
    });
    liveOk = r.ok;
  } catch (_) {}

  if (hubOk || liveOk) {
    el.textContent = '✓ Saved' +
      (liveOk ? ' to HamClock' : '') +
      (hubOk  ? ' + Hub'       : '');
    el.className = 'save-msg';
  } else {
    el.textContent = 'Save failed';
    el.className   = 'save-msg err';
  }
  setTimeout(() => { el.textContent = ''; }, 4000);
}

// Fetch HamClock settings when the tab is clicked
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.tab[data-tab="hamclock"]').forEach(btn => {
    btn.addEventListener('click', fetchHamClock);
  });
});

function hcVal(id, v) {
  const el = document.getElementById(id);
  if (!el) return '';
  if (v === undefined) return el.value;
  el.value = v;
}

function hcCheck(id, v) {
  const el = document.getElementById(id);
  if (!el) return false;
  if (v === undefined) return el.checked;
  el.checked = !!v;
}

// ---------------------------------------------------------------
// Cluster connect button
// ---------------------------------------------------------------

async function clusterConnect() {
  const el = document.getElementById('clusterActionMsg');
  const clusterCfg = {
    server:        val('clusterServer'),
    port:          parseInt(val('clusterPort')) || 7373,
    loginCallsign: val('clusterLogin'),
  };
  try {
    const result = await (await fetch('/api/cluster/connect', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(clusterCfg),
    })).json();
    el.textContent = result.status === 'connecting' ? '⚡ Connecting…' : result.error || 'Error';
    el.className   = 'save-msg' + (result.error ? ' err' : '');
  } catch (e) {
    el.textContent = 'Error: ' + e.message;
    el.className   = 'save-msg err';
  }
  setTimeout(() => { el.textContent = ''; }, 4000);
}

async function clusterDisconnect() {
  const el = document.getElementById('clusterActionMsg');
  try {
    await fetch('/api/cluster/disconnect', { method: 'POST' });
    el.textContent = '■ Disconnected';
    el.className   = 'save-msg';
  } catch (e) {
    el.textContent = 'Error: ' + e.message;
    el.className   = 'save-msg err';
  }
  setTimeout(() => { el.textContent = ''; }, 4000);
}

// ---------------------------------------------------------------
// Raw Telnet Feed
// ---------------------------------------------------------------

const RAW_MAX_LINES = 500;

function appendRawLine(line) {
  if (document.getElementById('rawFeedPause')?.checked) return;
  const pre = document.getElementById('rawFeed');
  if (!pre) return;
  const atBottom = pre.scrollHeight - pre.scrollTop <= pre.clientHeight + 4;
  pre.textContent += line + '\n';
  // Trim to max lines
  const lines = pre.textContent.split('\n');
  if (lines.length > RAW_MAX_LINES + 1) {
    pre.textContent = lines.slice(lines.length - RAW_MAX_LINES).join('\n');
  }
  if (atBottom) pre.scrollTop = pre.scrollHeight;
}

function clearRawFeed() {
  const pre = document.getElementById('rawFeed');
  if (pre) pre.textContent = '';
}

// ---------------------------------------------------------------
// App Launcher
// ---------------------------------------------------------------

async function fetchAppStatus() {
  try {
    const s = await (await fetch('/api/apps/status')).json();
    setAppBadge('hamclock', s.hamclock);
    setAppBadge('hamlog',   s.hamlog);
  } catch (_) {}
}

function setAppBadge(name, running) {
  const el = document.getElementById(name + 'RunBadge');
  if (!el) return;
  el.textContent = running ? '● Running' : '○ Stopped';
  el.className   = 'badge ' + (running ? 'badge-ok' : 'badge-err');
}

async function appLaunch(name) {
  const msgEl  = document.getElementById(name + 'LaunchMsg');
  const cmdId  = 'launchCmd' + name.charAt(0).toUpperCase() + name.slice(1);
  const command = (document.getElementById(cmdId)?.value || '').trim();
  try {
    const res    = await fetch('/api/apps/launch/' + name, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command }),
    });
    const result = await res.json();
    if (result.status === 'launched') {
      msgEl.textContent = '⚡ Launching…';
      msgEl.className   = 'save-msg';
      setTimeout(fetchAppStatus, 800);
    } else {
      msgEl.textContent = 'Error: ' + (result.error || 'unknown error');
      msgEl.className   = 'save-msg err';
    }
  } catch (e) {
    msgEl.textContent = 'Error: ' + e.message;
    msgEl.className   = 'save-msg err';
  }
  setTimeout(() => { msgEl.textContent = ''; }, 4000);
}

async function appKill(name) {
  const msgEl = document.getElementById(name + 'LaunchMsg');
  try {
    await fetch('/api/apps/kill/' + name, { method: 'POST' });
    msgEl.textContent = '■ Stopped';
    msgEl.className   = 'save-msg';
    setTimeout(fetchAppStatus, 500);
  } catch (e) {
    msgEl.textContent = 'Error: ' + e.message;
    msgEl.className   = 'save-msg err';
  }
  setTimeout(() => { msgEl.textContent = ''; }, 3000);
}

// ---------------------------------------------------------------
// Utility helpers
// ---------------------------------------------------------------

/** Get or set an input value */
function val(id, v, isSelect) {
  const el = document.getElementById(id);
  if (!el) return '';
  if (v === undefined) return el.value;
  el.value = v;
}

function check(id, v) {
  const el = document.getElementById(id);
  if (el) el.checked = !!v;
}

function esc(s) {
  return String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}
