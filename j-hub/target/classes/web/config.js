'use strict';
/* ============================================================
   J-Hub Cockpit UI — config.js
   ============================================================ */

// ── State ──────────────────────────────────────────────────
const state = {
  config:      {},
  status:      {},
  spots:       [],
  connectedApps: [],
  rig:         null,
  rotor:       null,
  clusterConn: false,
  spm:         0,
  wsState:     'CLOSED',
  appearance:  { theme: 'dark', fontSize: 13, waterfallColor: 'viridis', mapTheme: 'dark' },
};

// TLE freshness data keyed by satellite name, populated from J-Sat TLE API
let jsatTleStatus = {};

// ── Tab navigation ─────────────────────────────────────────
document.querySelectorAll('.nav-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
  });
});

// ── WebSocket (live telemetry) ─────────────────────────────
let ws = null;
let wsReconnectTimer = null;

function connectWs() {
  const port = window.location.port || '8081';
  const wsPort = parseInt(port) - 1; // 8080
  try {
    ws = new WebSocket('ws://' + window.location.hostname + ':' + wsPort);
  } catch(e) {
    scheduleWsReconnect(); return;
  }

  ws.onopen = () => {
    ws.send(JSON.stringify({ type: 'APP_CONNECTED', appName: 'webconfig', version: '1.0.0' }));
    setWsState('OPEN');
    if (wsReconnectTimer) { clearTimeout(wsReconnectTimer); wsReconnectTimer = null; }
  };

  ws.onclose = () => {
    setWsState('CLOSED');
    scheduleWsReconnect();
  };

  ws.onerror = () => { setWsState('ERROR'); };

  ws.onmessage = e => {
    try { handleWsMsg(JSON.parse(e.data)); } catch(_) {}
  };
}

function scheduleWsReconnect() {
  if (!wsReconnectTimer) wsReconnectTimer = setTimeout(connectWs, 3000);
}

function setWsState(s) {
  state.wsState = s;
  const el = document.getElementById('sb-ws');
  const navEl = document.getElementById('nav-ws-state');
  if (s === 'OPEN') {
    el.textContent = 'Connected'; el.className = 'sb-v ok';
    if (navEl) { navEl.textContent = 'Connected'; navEl.style.color = 'var(--green)'; }
  } else if (s === 'ERROR') {
    el.textContent = 'Error'; el.className = 'sb-v err';
    if (navEl) { navEl.textContent = 'Error'; navEl.style.color = 'var(--red)'; }
  } else {
    el.textContent = 'Disconnected'; el.className = 'sb-v err';
    if (navEl) { navEl.textContent = 'Offline'; navEl.style.color = 'var(--red)'; }
  }
}

function handleWsMsg(msg) {
  switch (msg.type) {
    case 'JHUB_WELCOME':
      if (msg.station) applyStationIntel(msg.station);
      break;
    case 'RIG_STATUS':
      state.rig = msg;
      updateRigUI(msg);
      break;
    case 'ROTOR_STATUS':
      state.rotor = msg;
      updateRotorUI(msg);
      break;
    case 'APP_LIST':
      state.connectedApps = msg.apps || [];
      updateModulesUI();
      break;
    case 'SPOT':
      state.spots.unshift(msg);
      if (state.spots.length > 200) state.spots.pop();
      renderSpotTable();
      break;
    case 'CLUSTER_RAW':
      appendRawFeed(msg.line || '');
      break;
    case 'SAT_STATE':
      updateJSatLive(msg);
      break;
  }
}

// ── Periodic polling ───────────────────────────────────────
function pollStatus() {
  fetch('/api/status')
    .then(r => r.json())
    .then(d => {
      state.status = d;
      state.clusterConn = d.clusterConnected;
      state.spm = d.spotsPerMinute || 0;
      updateStatusBar(d);
      updateDashboard(d);
      updateIntelPane(d);
      // Sync connected apps from HTTP status — reliable fallback when WS APP_LIST is missed
      if (Array.isArray(d.connectedApps)) {
        state.connectedApps = d.connectedApps;
        updateModulesUI();
      }
    })
    .catch(() => {});
}

function pollSpots() {
  fetch('/api/spots')
    .then(r => r.json())
    .then(spots => {
      state.spots = spots;
      renderSpotTable();
    })
    .catch(() => {});
}

// ── Rig status UI ──────────────────────────────────────────
function updateRigUI(rig) {
  const hz = rig.frequency || rig.rigFrequencyHz || 0;
  const mhz = hz > 0 ? (hz / 1e6).toFixed(3) + ' MHz' : '— — —';
  const mode = rig.mode || '—';
  const band = rig.band || '';
  const pwr  = rig.power != null ? rig.power + ' W' : '—';

  setText('d-freq', mhz);
  setText('d-mode', mode);
  setText('d-pwr',  rig.power != null ? rig.power : '—');
  setText('d-src',  rig.source || '—');
  setText('d-band', band);
  setText('d-rig-ts', rig.timestamp ? new Date(rig.timestamp).toLocaleTimeString() : '—');

  setVal('rig-live-freq', hz > 0 ? (hz / 1e6).toFixed(3) : '');
  setVal('rig-live-mode', mode);
  setVal('rig-live-band', band);
  setVal('rig-live-pwr',  rig.power != null ? rig.power : '');

  setText('i-freq', mhz);
  setText('i-mode', mode);
  setText('i-band', band !== '' ? band : '—');
  setText('i-pwr',  pwr);
}

// ── Rotor UI ───────────────────────────────────────────────
function updateRotorUI(rot) {
  const hdg = rot.bearing   != null ? Math.round(rot.bearing)   : null;
  const elv = rot.elevation != null ? Math.round(rot.elevation) : null;

  const hdgTxt = hdg != null ? hdg + '°' : '---°';
  const elvTxt = elv != null ? elv + '°' : '---°';

  setText('d-heading',       hdgTxt);
  setText('rot-heading-big', hdgTxt);
  setText('i-heading',       hdgTxt);
  setText('d-elevation',     elvTxt);
  setText('rot-elev-big',    elvTxt);

  ['compass-needle', 'rot-needle'].forEach(id => {
    const el = document.getElementById(id);
    if (el && hdg != null) el.setAttribute('transform', `rotate(${hdg}, 50, 50)`);
  });

  ['elev-needle', 'rot-elev-needle'].forEach(id => {
    const el = document.getElementById(id);
    if (el && elv != null) {
      const clamped = Math.min(90, Math.max(0, elv));
      el.setAttribute('transform', `rotate(${180 - clamped * 2}, 50, 50)`);
    }
  });

  const rb = (state.config.rotor && state.config.rotor.backend) || '—';
  setText('d-rotor-backend', rb);
  setText('i-rot-backend',   rb);
}

// ── Dashboard update ───────────────────────────────────────
function updateDashboard(status) {
  const cc = status.clusterConnected;
  const cfg = state.config;

  setDot('d-clus-dot', cc ? 'green' : 'red');
  setText('d-clus-txt', cc ? 'Connected' : 'Disconnected');
  setText('d-spm', status.spotsPerMinute || 0);
  setText('d-total-spots', status.totalSpots || 0);
  if (cfg.cluster) setText('d-clus-srv', cfg.cluster.server || '—');

  setDot('cl-dot', cc ? 'green' : 'red');
  setText('cl-status-txt', cc ? 'Connected' : 'Disconnected');
  setText('cl-spm', status.spotsPerMinute || 0);
  setText('cl-total', status.totalSpots || 0);
  if (cfg.cluster) setText('cl-srv-live', cfg.cluster.server || '—');

  // Module running state from appsRunning
  const ar = status.appsRunning || {};
  updateModuleDot('jmap',    ar['jMap'],     'J-Map');
  updateModuleDot('jlog',    ar['j-log'],    'J-Log');
  updateModuleDot('jdigi',   ar['j-digi'],   'J-Digi');
  updateModuleDot('jbridge', ar['j-bridge'], 'J-Bridge');
  updateModuleDot('jsat',    ar['j-sat'],    'J-Sat');
}

const MODULE_APP_NAMES = { jmap: 'j-map', jlog: 'j-log', jdigi: 'j-digi', jbridge: 'j-bridge', jsat: 'j-sat' };
function updateModuleDot(key, running, label) {
  // WebSocket connectedApps is authoritative; appsRunning is a fallback for process-started-but-not-yet-connected
  const appName = MODULE_APP_NAMES[key];
  const wsConnected = appName && state.connectedApps.some(a => a.appName === appName);
  const isUp = wsConnected || running;
  setDot('dot-' + key, isUp ? 'green' : 'gray');
  const meta = document.getElementById('meta-' + key);
  if (meta) meta.textContent = wsConnected ? 'Connected' : (running ? 'Running' : 'Not running');
}

// ── Intel pane ─────────────────────────────────────────────
function applyStationIntel(st) {
  setText('i-callsign', st.callsign || '—');
  setText('i-grid', st.gridSquare  || '—');
  setVal('st-call',      st.callsign    || '');
  setVal('st-name',      st.name        || '');
  setVal('st-grid',      st.gridSquare  || '');
  setVal('st-lat',       st.lat  != null ? st.lat  : '');
  setVal('st-lon',       st.lon  != null ? st.lon  : '');
  setVal('st-tz',        st.timezone    || '');
  setVal('st-cqzone',    st.cqZone      || '');
  setVal('st-arrl',      st.arrlSection || '');
  setVal('st-ituzone',   st.ituZone     || '');
  setVal('st-rig-alias', st.rigAlias    || '');

  // Show rig alias in intel pane, dashboard, and rig control header
  const aliasRow = document.getElementById('i-rig-alias-row');
  const alias = st.rigAlias || '';
  if (aliasRow) aliasRow.style.display = alias ? '' : 'none';
  setText('i-rig-alias',     alias || '—');
  setText('i-rig-model',     alias || (st.callsign ? st.callsign + ' Rig' : '—'));
  setText('d-rig-alias',     alias);
  setText('rig-header-alias', alias ? '— ' + alias : '');
}

function updateIntelPane(status) {
  const cc = status.clusterConnected;
  const cls = document.getElementById('i-clus-status');
  if (cls) {
    cls.textContent = cc ? 'Connected' : 'Disconnected';
    cls.className = 'intel-v ' + (cc ? 'live' : 'err');
  }
  setText('i-spm', status.spotsPerMinute || 0);
  updateAlerts(status);
}

function updateModulesUI() {
  const apps = state.connectedApps;
  const ar   = (state.status && state.status.appsRunning) || {};
  // Maps module key → appsRunning property name returned by /api/status
  const RUNNING_KEY = { jmap: 'jMap', jlog: 'j-log', jdigi: 'j-digi', jbridge: 'j-bridge', jsat: 'j-sat' };
  const keys = { 'j-log': 'jlog', 'j-digi': 'jdigi', 'j-bridge': 'jbridge', 'j-map': 'jmap', 'j-sat': 'jsat', 'logging-engine': null, 'webconfig': null };

  Object.entries(keys).forEach(([appName, key]) => {
    if (!key) return;
    const connected = apps.some(a => a.appName === appName);
    const connAt    = connected ? apps.find(a => a.appName === appName).connectedAt : null;
    const running   = !!ar[RUNNING_KEY[key]];

    // Side panel (Operator Intel)
    setDot('i-dot-' + key, connected ? 'green' : (running ? 'yellow' : 'gray'));
    setText('i-' + key, connected ? 'Online' : (running ? 'Starting' : 'Offline'));

    // Center panel (Module Connections)
    setDot('dot-' + key, connected ? 'green' : (running ? 'yellow' : 'gray'));
    const meta = document.getElementById('meta-' + key);
    if (meta) meta.textContent = connected
      ? 'Connected ' + (connAt ? new Date(connAt).toLocaleTimeString() : '')
      : (running ? 'Starting...' : 'Not connected');
  });

  renderSessionTable(apps);
  updateJSatConnStatus();
}

function updateAlerts(status) {
  const alerts = [];
  if (!status.clusterConnected && state.config.cluster && state.config.cluster.server) {
    alerts.push({ icon: '⚠️', text: 'DX Cluster disconnected', sub: state.config.cluster.server });
  }
  if (!state.rig && state.config.rig && state.config.rig.backend !== 'NONE') {
    alerts.push({ icon: '⚠️', text: 'Rig not reporting', sub: 'No RIG_STATUS received' });
  }

  const el = document.getElementById('alerts-list');
  if (!el) return;
  if (alerts.length === 0) {
    el.innerHTML = '<div class="no-alerts">No active alerts</div>';
  } else {
    el.innerHTML = alerts.map(a =>
      `<div class="alert-item">
        <span class="alert-icon">${a.icon}</span>
        <div><div class="alert-text">${a.text}</div>${a.sub ? `<div style="font-size:11px;color:var(--overlay0)">${a.sub}</div>` : ''}</div>
      </div>`
    ).join('');
  }
}

// ── J-Sat ─────────────────────────────────────────────────

let jsatSatellites = [];

function updateJSatLive(msg) {
  setText('jsat-tracking', msg.satName || '—');
  const az  = msg.azDeg  != null ? msg.azDeg.toFixed(1)  + '°' : '—';
  const el  = msg.elDeg  != null ? msg.elDeg.toFixed(1)  + '°' : '—';
  setText('jsat-azel', az + ' / ' + el);
  const dl  = msg.downlinkHz > 0 ? (msg.downlinkHz / 1e6).toFixed(3) + ' MHz' : '—';
  setText('jsat-dl', dl);
}

function loadJSatSatelliteRegistry() {
  fetch('/data/satellite-registry.json')
    .then(r => r.json())
    .then(data => {
      jsatSatellites = data.satellites || [];
      renderJSatSatList(null);
    })
    .catch(() => {
      const el = document.getElementById('jsat-sat-list');
      if (el) el.innerHTML = '<div style="color:var(--red);font-size:12px">Failed to load satellite list</div>';
    });
}

function renderJSatSatList(enabledNames) {
  const container = document.getElementById('jsat-sat-list');
  if (!container) return;
  if (!jsatSatellites.length) { container.innerHTML = '<div style="color:var(--overlay0);font-size:12px">No satellites</div>'; return; }

  const groups = {};
  jsatSatellites.forEach(sat => {
    const g = sat.type || 'Other';
    if (!groups[g]) groups[g] = [];
    groups[g].push(sat);
  });

  let html = '';
  Object.entries(groups).sort().forEach(([grp, sats]) => {
    html += `<div style="font-size:11px;font-weight:bold;color:var(--overlay0);padding:4px 0 2px;border-top:1px solid var(--surface1);margin-top:4px">${grp}</div>`;
    sats.forEach(sat => {
      const checked = enabledNames ? enabledNames.includes(sat.name) : sat.enabled;
      const norad = sat.noradId > 0 ? `<span style="color:var(--overlay0);font-size:10px;margin-left:6px">#${sat.noradId}</span>` : '';
      const satStatus = sat.status ? `<span style="color:var(--overlay0);font-size:10px;margin-left:6px">${sat.status}</span>` : '';
      const tleInfo = jsatTleStatus[sat.name];
      let staleBadge = '';
      if (tleInfo) {
        if (tleInfo.freshness === 'RED') {
          const age = tleInfo.ageHours != null ? tleInfo.ageHours.toFixed(1) + 'h old' : 'stale';
          staleBadge = `<span style="font-size:9px;background:var(--red);color:#fff;border-radius:3px;padding:1px 4px;margin-left:4px;flex-shrink:0" title="${age}">STALE</span>`;
        } else if (tleInfo.freshness === 'YELLOW' || tleInfo.stale) {
          const age = tleInfo.ageHours != null ? tleInfo.ageHours.toFixed(1) + 'h old' : 'aging';
          staleBadge = `<span style="font-size:9px;background:var(--yellow,#f9e2af);color:#1e1e2e;border-radius:3px;padding:1px 4px;margin-left:4px;flex-shrink:0" title="${age}">AGING</span>`;
        }
      }
      html += `<label style="display:flex;align-items:center;gap:6px;padding:2px 0;cursor:pointer">
        <input type="checkbox" class="jsat-sat-cb" data-name="${sat.name}" data-type="${sat.type || ''}" ${checked ? 'checked' : ''}>
        <span style="font-size:12px">${sat.name}</span>${norad}${satStatus}${staleBadge}
      </label>`;
    });
  });
  container.innerHTML = html;
}

function jsatGetCheckedNames() {
  return Array.from(document.querySelectorAll('.jsat-sat-cb:checked')).map(cb => cb.dataset.name);
}

function jsatSelectByType(typeKeyword) {
  document.querySelectorAll('.jsat-sat-cb').forEach(cb => {
    cb.checked = cb.dataset.type && cb.dataset.type.toLowerCase().includes(typeKeyword.toLowerCase());
  });
}

function jsatSelectAll()  { document.querySelectorAll('.jsat-sat-cb').forEach(cb => cb.checked = true);  }
function jsatSelectNone() { document.querySelectorAll('.jsat-sat-cb').forEach(cb => cb.checked = false); }

function populateJSatTab(cfg) {
  const s  = cfg.jSatSettings || {};
  const ap = cfg.apps && cfg.apps.jSat ? cfg.apps.jSat : {};

  const fsEl  = document.getElementById('jsat-font-size');
  const fsLbl = document.getElementById('jsat-font-size-val');
  if (fsEl)  fsEl.value = s.fontSize || 15;
  if (fsLbl) fsLbl.textContent = s.fontSize || 15;

  const setFontSlider = (id, valId, v) => {
    const el  = document.getElementById(id);
    const lbl = document.getElementById(valId);
    const n   = (typeof v === 'number' ? v : 0);
    if (el)  el.value = n;
    if (lbl) lbl.textContent = (n === 0 ? 'auto' : n);
  };
  setFontSlider('jsat-font-topbar',   'jsat-font-topbar-val',   s.topBarFontSize);
  setFontSlider('jsat-font-livepass', 'jsat-font-livepass-val', s.livePassFontSize);
  setFontSlider('jsat-font-passlist', 'jsat-font-passlist-val', s.passListFontSize);
  setFontSlider('jsat-font-weather',  'jsat-font-weather-val',  s.spaceWeatherFontSize);
  setFontSlider('jsat-font-rigrotor', 'jsat-font-rigrotor-val', s.rigRotorFontSize);

  setChk('jsat-doppler-enable',    !!s.rigControlEnabled);
  setChk('jsat-rotor-enable',      !!s.rotorControlEnabled);
  setChk('jsat-show-track',        s.showGroundTrack  !== false);
  setChk('jsat-show-footprint',    s.showFootprint    !== false);
  setChk('jsat-show-spaceweather', s.showSpaceWeather !== false);
  setVal('jsat-callsign',  s.callsign  || '');
  setVal('jsat-lat',       s.qthLat    != null ? s.qthLat   : '');
  setVal('jsat-lon',       s.qthLon    != null ? s.qthLon   : '');
  setVal('jsat-alt',       s.qthAltKm  != null ? s.qthAltKm : '');
  setVal('jsat-min-el',    s.minPassElevationDeg != null ? s.minPassElevationDeg : '');
  setVal('jsat-lookahead', s.passLookAheadHours  != null ? s.passLookAheadHours  : '');

  const rig   = cfg.rig   || {};
  const rotor = cfg.rotor || {};
  setText('jsat-rig-backend',   rig.backend   || 'NONE');
  setText('jsat-rotor-backend', rotor.backend || 'NONE');

  const enabledNames = Array.isArray(s.enabledSatellites) && s.enabledSatellites.length
    ? s.enabledSatellites : null;
  if (jsatSatellites.length) renderJSatSatList(enabledNames);

  setVal('jsat-tle-threshold', s.tleStaleThresholdHours != null ? s.tleStaleThresholdHours : 48);
  setVal('jsat-tle-port',      s.tleApiPort            != null ? s.tleApiPort             : 4540);
}

function saveJSatSettings() {
  const intOf = id => parseInt(document.getElementById(id).value, 10) || 0;
  const settings = {
    fontSize:             intOf('jsat-font-size') || 15,
    topBarFontSize:       intOf('jsat-font-topbar'),
    livePassFontSize:     intOf('jsat-font-livepass'),
    passListFontSize:     intOf('jsat-font-passlist'),
    spaceWeatherFontSize: intOf('jsat-font-weather'),
    rigRotorFontSize:     intOf('jsat-font-rigrotor'),
    callsign:             document.getElementById('jsat-callsign').value.trim() || undefined,
    qthLat:               parseFloat(document.getElementById('jsat-lat').value)  || undefined,
    qthLon:               parseFloat(document.getElementById('jsat-lon').value)  || undefined,
    qthAltKm:             parseFloat(document.getElementById('jsat-alt').value)  || undefined,
    minPassElevationDeg:  parseFloat(document.getElementById('jsat-min-el').value)    || undefined,
    passLookAheadHours:   parseInt(document.getElementById('jsat-lookahead').value)   || undefined,
    rigControlEnabled:   document.getElementById('jsat-doppler-enable').checked,
    rotorControlEnabled: document.getElementById('jsat-rotor-enable').checked,
    showGroundTrack:      document.getElementById('jsat-show-track').checked,
    showFootprint:        document.getElementById('jsat-show-footprint').checked,
    showSpaceWeather:     document.getElementById('jsat-show-spaceweather').checked,
    enabledSatellites:    jsatGetCheckedNames(),
    tleStaleThresholdHours: parseInt(document.getElementById('jsat-tle-threshold').value) || 48,
    tleApiPort:             parseInt(document.getElementById('jsat-tle-port').value)      || 4540,
  };

  fetch('/api/jsat', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify(settings)
  })
  .then(r => r.json())
  .then(() => flashMsg('jsat-msg', 'J-Sat settings saved'))
  .catch(() => flashMsg('jsat-msg', 'Error', true));
}

function updateJSatConnStatus() {
  const apps  = state.connectedApps || [];
  const jsat  = apps.find(a => a.appName === 'j-sat');
  const dot   = document.getElementById('jsat-conn-dot');
  const meta  = document.getElementById('jsat-conn-meta');
  if (dot)  setDot('jsat-conn-dot', jsat ? 'green' : 'gray');
  if (meta) meta.textContent = jsat
    ? 'Connected ' + (jsat.connectedAt ? new Date(jsat.connectedAt).toLocaleTimeString() : '')
    : 'Not connected';

  if (state.rotor) {
    const az = state.rotor.bearing   != null ? state.rotor.bearing.toFixed(1)   + '°' : '—';
    const el = state.rotor.elevation != null ? state.rotor.elevation.toFixed(1) + '°' : '—';
    setText('jsat-rotor-pos', az + ' / ' + el);
  }

  if (state.rig) {
    const hz  = state.rig.frequency || 0;
    const mhz = hz > 0 ? (hz / 1e6).toFixed(3) + ' MHz' : 'Unknown';
    setText('jsat-rig-status', mhz);
  }
}

// ── J-Sat TLE status ───────────────────────────────────────

function loadJSatTleStatus() {
  fetch('/api/jsat/tle-status')
    .then(r => r.json())
    .then(data => {
      jsatTleStatus = {};
      (data || []).forEach(t => { jsatTleStatus[t.name] = t; });
      // Re-render satellite list to show/hide stale badges
      if (jsatSatellites.length) {
        const s = (state.config.jSatSettings) || {};
        const enabledNames = Array.isArray(s.enabledSatellites) && s.enabledSatellites.length
          ? s.enabledSatellites : null;
        renderJSatSatList(enabledNames);
      }
    })
    .catch(() => {});
}

function refreshJSatTleStatus() {
  loadJSatTleStatus();
  flashMsg('jsat-tle-refresh-msg', 'TLE status refreshed');
}

// ── Status bar ─────────────────────────────────────────────
function updateStatusBar(status) {
  const cc = status.clusterConnected;
  const sbClus = document.getElementById('sb-cluster');
  if (sbClus) {
    sbClus.textContent = cc ? (status.spotsPerMinute || 0) + ' spm' : 'Offline';
    sbClus.className = 'sb-v ' + (cc ? 'ok' : 'err');
  }
  const sbRig = document.getElementById('sb-rig-poll');
  if (sbRig) {
    const hasRig = state.rig != null;
    const backend = (state.config.rig && state.config.rig.backend) || 'NONE';
    sbRig.textContent = backend === 'NONE' ? 'None' : (hasRig ? 'OK' : 'No data');
    sbRig.className = 'sb-v ' + (backend === 'NONE' ? '' : (hasRig ? 'ok' : 'warn'));
  }
  if (status.uptimeSeconds != null) {
    const u = status.uptimeSeconds;
    const h = Math.floor(u / 3600);
    const m = Math.floor((u % 3600) / 60);
    const s = u % 60;
    setText('sb-uptime', `${h}h ${m}m ${s}s`);
  }
}

function tickClock() {
  const now = new Date();
  const utc = now.toISOString().substr(11, 8);
  const loc = now.toLocaleTimeString();
  setText('sb-utc',   utc);
  setText('sb-local', loc);
}

// ── Spot table ─────────────────────────────────────────────
function renderSpotTable() {
  const tbody = document.getElementById('spot-tbody');
  if (!tbody) return;
  const rows = state.spots.slice(0, 50).map(s => {
    const freq = s.frequency ? (s.frequency / 1000).toFixed(1) : '—';
    const dist = s.distanceMi ? Math.round(s.distanceMi) + ' mi' : (s.distanceKm ? Math.round(s.distanceKm) + ' km' : '—');
    const brg  = s.bearing != null ? Math.round(s.bearing) + '°' : '—';
    const time = s.timestamp ? new Date(s.timestamp).toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'}) : '—';
    return `<tr>
      <td class="sp-dx">${esc(s.spotted||'')}</td>
      <td class="sp-freq">${freq}</td>
      <td class="sp-mode">${esc(s.mode||'')}</td>
      <td class="sp-ctry">${esc(s.country||'')}</td>
      <td>${dist}</td>
      <td>${brg}</td>
      <td class="sp-time">${time}</td>
    </tr>`;
  }).join('');
  tbody.innerHTML = rows || '<tr><td colspan="7" style="color:var(--overlay0);text-align:center;padding:16px">No spots yet</td></tr>';
}

// ── Raw telnet feed ─────────────────────────────────────────
function appendRawFeed(line) {
  const el = document.getElementById('raw-feed');
  if (!el) return;
  const cls = line.includes('DX de') ? 'rf-spot' : 'rf-sys';
  const div = document.createElement('div');
  div.className = cls;
  div.textContent = line;
  el.appendChild(div);
  el.scrollTop = el.scrollHeight;
  while (el.childNodes.length > 200) el.removeChild(el.firstChild);
}

function sendTelnetCommand() {
  const input = document.getElementById('cl-send-input');
  const cmd = input ? input.value.trim() : '';
  if (!cmd) return;
  fetch('/api/cluster/send', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ command: cmd })
  })
  .then(r => r.json())
  .then(() => {
    appendRawFeed('> ' + cmd);
    if (input) input.value = '';
  })
  .catch(() => flashMsg('cl-msg', 'Send error', true));
}

// ── Session table ───────────────────────────────────────────
function renderSessionTable(apps) {
  const tbody = document.getElementById('ws-sessions-tbody');
  if (!tbody) return;
  const visible = (apps || []).filter(a => a.appName !== 'webconfig');
  tbody.innerHTML = visible.length === 0
    ? '<tr><td colspan="3" style="color:var(--overlay0);padding:12px">No modules connected</td></tr>'
    : visible.map(a => `<tr>
        <td>${esc(a.appName)}</td>
        <td style="color:var(--subtext0)">${esc(a.version||'')}</td>
        <td style="color:var(--overlay0)">${a.connectedAt ? new Date(a.connectedAt).toLocaleString() : '—'}</td>
      </tr>`).join('');
}

function refreshSessions() {
  fetch('/api/status').then(r => r.json()).then(() => {
    renderSessionTable(state.connectedApps);
  });
}

// ── Load config & populate all forms ──────────────────────
function loadConfig() {
  fetch('/api/config')
    .then(r => r.json())
    .then(cfg => {
      state.config = cfg;
      populateForms(cfg);
    })
    .catch(() => {});
}

function populateForms(cfg) {
  // Station
  const st = cfg.station || {};
  setVal('st-call',    st.callsign    || '');
  setVal('st-name',    st.name        || '');
  setVal('st-qth',     st.qth         || '');
  setVal('st-grid',    st.gridSquare  || '');
  setVal('st-lat',     st.lat  != null ? st.lat  : '');
  setVal('st-lon',     st.lon  != null ? st.lon  : '');
  setVal('st-cqzone',  st.cqZone      || '');
  setVal('st-arrl',    st.arrlSection || '');
  setVal('st-ituzone', st.ituZone     || '');
  setSelectVal('st-tz',   st.timezone || 'UTC');
  setSelectVal('st-lang', st.language || 'en');
  applyStationIntel(st);

  // J-Hub ports + IP
  const jh = cfg.jHub || {};
  setVal('jhub-ip',  jh.ip          || 'localhost');
  setVal('ws-port',  jh.websocketPort || 8080);
  setVal('web-port', jh.webConfigPort || 8081);

  // Rig
  const rig = cfg.rig || {};
  setRigBackendUI(rig.backend || 'NONE');
  setVal('civ-port',   rig.civPort    || '');
  setVal('civ-baud',   rig.civBaud    || 9600);
  setVal('civ-addr',   rig.civAddress || '94');
  setVal('rig-hamlib-host', rig.hamlibHost || 'localhost');
  setVal('rig-hamlib-port', rig.hamlibPort || 4532);
  setVal('rig-poll-ms', rig.pollRateMs || 500);
  setChk('rig-ptt', !!rig.enablePtt);
  document.getElementById('ptt-test-btn').disabled = rig.backend === 'NONE';

  // Rotor
  const rot = cfg.rotor || {};
  setRotorBackendUI(rot.backend || 'NONE');
  setVal('rot-model',       rot.model        || '');
  setVal('rot-com',         rot.comPort      || '');
  setVal('rot-hamlib-host', rot.tcpHost      || 'localhost');
  setVal('rot-hamlib-port', rot.tcpPort      || 4533);
  setVal('rot-short-offset', rot.shortPathOffset != null ? rot.shortPathOffset : 0);
  setVal('rot-custom',      rot.customPreset != null ? rot.customPreset : 0);
  setText('i-rot-backend', rot.backend || 'NONE');

  // Cluster
  const cl = cfg.cluster || {};
  setVal('cl-host',  cl.server        || '');
  setVal('cl-port',  cl.port          || 7373);
  setVal('cl-login', cl.loginCallsign || '');
  setChk('cl-auto',  !!cl.autoConnect);
  if (cfg.cluster) setText('d-clus-srv', cl.server || '—');
  loadNetworks();

  // Logging
  const lg = cfg.logger || {};
  setVal('log-db-path', (lg.normalLog && lg.normalLog.dbPath) || '');
  setVal('log-mode', lg.mode || 'normal');

  // Band/mode filters
  const filters = (cl.filters) || {};
  buildFilterChips('band-filters', ['160m','80m','60m','40m','30m','20m','17m','15m','12m','10m','6m','2m','70cm'], filters.bands || []);
  buildFilterChips('mode-filters', ['CW','SSB','FT8','FT4','RTTY','PSK31','JS8','OLIVIA','MFSK16'], filters.modes || []);

  // Appearance (theme only — tab removed, but still apply saved theme)
  const ap = cfg.appearance || {};
  state.appearance = ap;
  const theme = ap.theme || localStorage.getItem('jhub-theme') || 'dark';
  applyTheme(theme);
  state.appearance.theme = theme;

  // Module cards
  buildModuleCards(cfg.apps || {});

  // J-Sat tab
  populateJSatTab(cfg);

  // Callsign tab
  populateCallsignTab(cfg);

  // J-Log tab
  const jl = cfg.jLogSettings || {};
  populateJLogForm(jl);

  // J-Digi tab
  const jd = cfg.jDigiSettings || {};
  populateJDigiForm(jd);
}

// ── Rig backend segmented control ─────────────────────────
function setRigBackend(val, btn) {
  document.querySelectorAll('#rig-backend-seg .seg-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  showCond('civ-block',    val === 'CI_V');
  showCond('hamlib-block', val === 'HAMLIB');
  document.getElementById('ptt-test-btn').disabled = val === 'NONE';
}

function setRigBackendUI(val) {
  document.querySelectorAll('#rig-backend-seg .seg-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.val === val);
  });
  showCond('civ-block',    val === 'CI_V');
  showCond('hamlib-block', val === 'HAMLIB');
}

// ── Rotor backend segmented control ───────────────────────
function setRotorBackend(val, btn) {
  document.querySelectorAll('#rot-backend-seg .seg-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  showCond('rot-internal-block', val === 'INTERNAL');
  showCond('rot-hamlib-block',   val === 'HAMLIB');
}

function setRotorBackendUI(val) {
  document.querySelectorAll('#rot-backend-seg .seg-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.val === val);
  });
  showCond('rot-internal-block', val === 'INTERNAL');
  showCond('rot-hamlib-block',   val === 'HAMLIB');
}

// ── Theme ─────────────────────────────────────────────────
function applyTheme(val) {
  document.body.classList.toggle('light', val === 'light');
  const btn = document.getElementById('theme-toggle-btn');
  if (btn) btn.textContent = val === 'light' ? '☀' : '☾';
  localStorage.setItem('jhub-theme', val);
}

function toggleThemeBtn() {
  const current = document.body.classList.contains('light') ? 'light' : 'dark';
  const next = current === 'light' ? 'dark' : 'light';
  applyTheme(next);
  state.appearance.theme = next;
}

// ── Module cards (Modules tab) ─────────────────────────────
function buildModuleCards(appsSection) {
  const modules = [
    { key: 'jLog',    id: 'j-log',    label: 'J-Log',    desc: 'Ham radio logging application' },
    { key: 'jDigi',   id: 'j-digi',   label: 'J-Digi',   desc: 'Digital modem / RTTY / PSK' },
    { key: 'jBridge', id: 'j-bridge', label: 'J-Bridge', desc: 'WSJT-X / FT8 integration bridge' },
    { key: 'jMap',    id: 'jMap',     label: 'J-Map',    desc: 'Real-time grayline + DX map' },
    { key: 'jSat',    id: 'j-sat',    label: 'J-Sat',    desc: 'Satellite tracking and Doppler control' },
  ];

  const container = document.getElementById('module-cards');
  if (!container) return;
  container.innerHTML = modules.map(m => {
    const entry = appsSection[m.key] || {};
    return `<div class="card">
      <div class="card-title">${m.label}</div>
      <div style="font-size:11px;color:var(--overlay0);margin-bottom:10px">${m.desc}</div>
      <div class="field-row cols-auto">
        <div class="field" style="grid-column:1/-1">
          <input type="text" id="cmd-${m.key}" placeholder="bash /home/user/ars/${m.id}/run.sh" value="${esc(entry.command||'')}">
          <label>Launch Command</label>
        </div>
      </div>
      <div class="field-row cols-2" style="margin-top:6px">
        <div class="field">
          <input type="text" id="ip-${m.key}" placeholder="localhost" value="${esc(entry.ip||'localhost')}">
          <label>IP Address</label>
        </div>
      </div>
      <div class="toggle-row">
        <div>
          <div class="toggle-label">Auto-Launch</div>
          <div class="toggle-desc">Start automatically when J-Hub starts</div>
        </div>
        <label class="toggle">
          <input type="checkbox" id="auto-${m.key}" ${entry.autoLaunch ? 'checked' : ''}>
          <span class="toggle-slider"></span>
        </label>
      </div>
      <div class="btn-row" style="margin-top:10px">
        <button class="btn btn-green btn-sm" onclick="launchApp('${m.id}')">Launch</button>
        <button class="btn btn-red btn-sm"   onclick="killApp('${m.id}')">Stop</button>
        <button class="btn btn-ghost btn-sm" onclick="saveModuleCmd('${m.key}','${m.id}')">Save</button>
        <span id="mod-msg-${m.key}" style="font-size:11px;color:var(--overlay0)"></span>
      </div>
    </div>`;
  }).join('');
}

// ── Filter chips ───────────────────────────────────────────
function buildFilterChips(containerId, allItems, enabled) {
  const el = document.getElementById(containerId);
  if (!el) return;
  const set = new Set(Array.isArray(enabled) ? enabled : []);
  el.innerHTML = allItems.map(item =>
    `<div class="filter-chip ${set.has(item) ? 'on' : ''}" data-val="${item}" onclick="toggleChip(this)">${item}</div>`
  ).join('');
}

function toggleChip(el) {
  el.classList.toggle('on');
}

function selectAllBands() {
  document.querySelectorAll('#band-filters .filter-chip').forEach(c => c.classList.add('on'));
}

function clearAllBands() {
  document.querySelectorAll('#band-filters .filter-chip').forEach(c => c.classList.remove('on'));
}

function getCheckedChips(containerId) {
  return Array.from(document.querySelectorAll('#' + containerId + ' .filter-chip.on'))
              .map(c => c.dataset.val);
}

// ── Macro table ────────────────────────────────────────────
// ── Macros (Digital + Voice) ──────────────────────────────
state.macros = [];
state.macroKind = 'CW';

function loadMacros() {
  fetch('/api/macros')
    .then(r => r.json())
    .then(list => {
      state.macros = (list || []).map(m => ({
        key: m.key || '',
        label: m.label || '',
        type: m.type || 'PROGRAMMABLE',
        text: m.text || '',
        kind: m.kind || 'CW',
        wavPath: m.wavPath || ''
      }));
      renderMacroList();
    })
    .catch(() => {});
}

function setMacroKind(kind, btn) {
  state.macroKind = kind;
  document.querySelectorAll('#macro-kind-seg .seg-btn').forEach(b => b.classList.toggle('active', b === btn));
  document.getElementById('macro-kind-title').textContent = kind === 'VOICE' ? 'Voice Macros' : 'Digital / CW Macros';
  document.getElementById('macro-kind-help').innerHTML = kind === 'VOICE'
    ? 'Hold the Record button to capture audio from your default mic. Files are stored under <code>~/.j-hub/voice/</code>.'
    : 'Substitutions: <code>{MYCALL} {CALL} {RST} {NAME} {BAND} {FREQ} {MODE}</code>';
  renderMacroList();
}

function renderMacroList() {
  const wrap = document.getElementById('macros-list');
  if (!wrap) return;
  const kind = state.macroKind;
  const visible = state.macros.map((m, i) => ({ m, i })).filter(x => (x.m.kind || 'CW') === kind);
  wrap.innerHTML = visible.map(({ m, i }) => kind === 'VOICE'
    ? `<div class="row-card macro-row" data-idx="${i}">
         <div class="row-fields">
           <div><label>Key (F1–F12)</label><input type="text" value="${esc(m.key)}" data-field="key" placeholder="F1"></div>
           <div><label>Label</label><input type="text" value="${esc(m.label)}" data-field="label" placeholder="CQ Voice"></div>
           <div style="grid-column:1/-1">
             <label>WAV file</label>
             <input type="text" value="${esc(m.wavPath)}" data-field="wavPath" placeholder="(record below or paste path)" style="font-size:11px">
             <div style="margin-top:6px;display:flex;gap:8px;align-items:center;flex-wrap:wrap">
               <button class="rec-btn" type="button" onmousedown="startVoiceRecord(${i},this)" onmouseup="stopVoiceRecord(${i},this)" onmouseleave="stopVoiceRecord(${i},this)">● Hold to Record</button>
               <button class="rec-btn" type="button" style="background:var(--surface1);color:var(--text)" onclick="playVoicePreview(${i})">▶ Play</button>
               <span class="wav-info" id="wav-info-${i}">${m.wavPath ? '✓ ' + m.wavPath.split('/').pop() : ''}</span>
             </div>
           </div>
         </div>
         <div class="row-actions">
           <button onclick="triggerMacro(${i})" title="Trigger now">▶</button>
           <button onclick="removeMacroRow(${i})" title="Delete">✕</button>
         </div>
       </div>`
    : `<div class="row-card macro-row" data-idx="${i}">
         <div class="row-fields">
           <div><label>Key</label><input type="text" value="${esc(m.key)}" data-field="key" ${m.type==='FIXED'?'readonly style="color:var(--subtext0)"':''}></div>
           <div><label>Label</label><input type="text" value="${esc(m.label)}" data-field="label"></div>
           <div style="grid-column:1/-1">
             <label>Text Template</label>
             <textarea class="macro-text" data-field="text" placeholder="CQ CQ DE {MYCALL} {MYCALL} K">${esc(m.text)}</textarea>
           </div>
         </div>
         <div class="row-actions">
           <button onclick="triggerMacro(${i})" title="Trigger now">▶</button>
           ${m.type === 'FIXED' ? '' : `<button onclick="removeMacroRow(${i})" title="Delete">✕</button>`}
         </div>
       </div>`
  ).join('') || '<div style="font-size:13px;color:var(--subtext0);padding:10px">No '+kind+' macros yet. Click "+ Add Macro" below.</div>';

  // Wire up live-edit binding so saveMacros() picks up unsaved edits.
  wrap.querySelectorAll('.row-card').forEach(card => {
    const idx = parseInt(card.dataset.idx);
    card.querySelectorAll('[data-field]').forEach(input => {
      input.addEventListener('input', () => {
        const f = input.dataset.field;
        state.macros[idx][f] = input.value;
      });
    });
  });
}

function addMacro() {
  const kind = state.macroKind;
  const defaultKey = 'F' + (state.macros.filter(m => (m.kind||'CW') === kind).length + 1);
  state.macros.push({
    key: defaultKey, label: defaultKey, type: 'PROGRAMMABLE',
    text: '', kind: kind, wavPath: ''
  });
  renderMacroList();
}

function removeMacroRow(idx) {
  state.macros.splice(idx, 1);
  renderMacroList();
}

function saveMacros() {
  const body = state.macros
    .filter(m => (m.key || '').trim() !== '')
    .map(m => ({
      key: m.key.trim(), label: m.label || m.key, type: m.type || 'PROGRAMMABLE',
      text: m.text || '', kind: m.kind || 'CW', wavPath: m.wavPath || ''
    }));
  fetch('/api/macros', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => flashMsg('macros-save-msg', 'Saved'))
    .catch(() => flashMsg('macros-save-msg', 'Error', true));
}

function triggerMacro(idx) {
  const m = state.macros[idx];
  if (!m) return;
  fetch('/api/macros/trigger', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ key: m.key })
  }).catch(() => {});
}

// ── Voice recording (browser MediaRecorder + inline WAV encoder) ──
state.recorder = null;
state.recorderChunks = [];
state.recorderStream = null;

async function startVoiceRecord(idx, btn) {
  if (state.recorder) return;
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    state.recorderStream = stream;
    state.recorderChunks = [];
    // Use WebAudio to capture raw PCM so we can encode WAV (Java's AudioSystem doesn't read WebM/Opus).
    const ctx = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 22050 });
    const src = ctx.createMediaStreamSource(stream);
    const proc = ctx.createScriptProcessor(4096, 1, 1);
    proc.onaudioprocess = e => {
      const ch = e.inputBuffer.getChannelData(0);
      state.recorderChunks.push(new Float32Array(ch));
    };
    src.connect(proc); proc.connect(ctx.destination);
    state.recorder = { ctx, proc, src, idx };
    btn.classList.add('recording');
    btn.textContent = '● Recording…';
  } catch (e) {
    alert('Microphone access denied: ' + e.message);
  }
}

async function stopVoiceRecord(idx, btn) {
  if (!state.recorder || state.recorder.idx !== idx) return;
  const { ctx, proc, src } = state.recorder;
  proc.disconnect(); src.disconnect();
  state.recorderStream.getTracks().forEach(t => t.stop());
  const sampleRate = ctx.sampleRate;
  await ctx.close();
  state.recorder = null;
  btn.classList.remove('recording');
  btn.textContent = '● Hold to Record';

  if (state.recorderChunks.length === 0) return;
  const wav = encodeWav(state.recorderChunks, sampleRate);
  const m = state.macros[idx];
  const safeName = (m.key || ('macro_' + Date.now())).replace(/[^A-Za-z0-9._-]/g, '_');
  const fd = new FormData();
  fd.append('name', safeName);
  fd.append('file', new Blob([wav], { type: 'audio/wav' }), safeName + '.wav');
  try {
    const res = await fetch('/api/voice/upload', { method: 'POST', body: fd });
    const json = await res.json();
    if (json.path) {
      m.wavPath = json.path;
      renderMacroList();
    }
  } catch (e) { alert('Upload failed: ' + e.message); }
  state.recorderChunks = [];
}

function encodeWav(chunks, sampleRate) {
  let len = 0; chunks.forEach(c => len += c.length);
  const buf = new ArrayBuffer(44 + len * 2);
  const dv = new DataView(buf);
  const w = (o, s) => { for (let i=0;i<s.length;i++) dv.setUint8(o+i, s.charCodeAt(i)); };
  w(0,'RIFF'); dv.setUint32(4, 36+len*2, true); w(8,'WAVE'); w(12,'fmt ');
  dv.setUint32(16, 16, true); dv.setUint16(20, 1, true); dv.setUint16(22, 1, true);
  dv.setUint32(24, sampleRate, true); dv.setUint32(28, sampleRate*2, true);
  dv.setUint16(32, 2, true); dv.setUint16(34, 16, true);
  w(36,'data'); dv.setUint32(40, len*2, true);
  let off = 44;
  for (const c of chunks) {
    for (let i=0; i<c.length; i++) {
      const s = Math.max(-1, Math.min(1, c[i]));
      dv.setInt16(off, s < 0 ? s*0x8000 : s*0x7FFF, true);
      off += 2;
    }
  }
  return buf;
}

function playVoicePreview(idx) {
  const m = state.macros[idx];
  if (!m || !m.wavPath) { alert('No WAV file recorded yet'); return; }
  const url = '/api/voice/file?path=' + encodeURIComponent(m.wavPath);
  new Audio(url).play().catch(e => alert('Playback failed: ' + e.message));
}

// ── Save functions ─────────────────────────────────────────
function saveStation() {
  const body = {
    station: {
      callsign:    (document.getElementById('st-call').value||'').toUpperCase().trim(),
      name:        document.getElementById('st-name').value.trim(),
      qth:         document.getElementById('st-qth').value.trim(),
      gridSquare:  (document.getElementById('st-grid').value||'').toUpperCase().trim(),
      lat:         parseFloat(document.getElementById('st-lat').value)||0,
      lon:         parseFloat(document.getElementById('st-lon').value)||0,
      timezone:    document.getElementById('st-tz').value || 'UTC',
      language:    document.getElementById('st-lang').value || 'en',
      cqZone:      parseInt(document.getElementById('st-cqzone').value)||0,
      arrlSection: (document.getElementById('st-arrl').value||'').toUpperCase().trim(),
      ituZone:     parseInt(document.getElementById('st-ituzone').value)||0,
      rigAlias:    document.getElementById('st-rig-alias').value.trim(),
    }
  };
  postPartialConfig(body, 'st-msg', 'Station saved');
}

function saveRig() {
  const backend = activeSegVal('#rig-backend-seg');
  const body = {
    backend,
    civPort:    document.getElementById('civ-port').value.trim(),
    civBaud:    parseInt(document.getElementById('civ-baud').value)||9600,
    civAddress: document.getElementById('civ-addr').value.trim()||'94',
    hamlibHost: document.getElementById('rig-hamlib-host').value.trim()||'localhost',
    hamlibPort: parseInt(document.getElementById('rig-hamlib-port').value)||4532,
    pollRateMs: parseInt(document.getElementById('rig-poll-ms').value)||500,
    enablePtt:  document.getElementById('rig-ptt').checked,
  };
  fetch('/api/rig', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => { flashMsg('rig-save-msg', 'Saved'); state.config.rig = body; })
    .catch(() => flashMsg('rig-save-msg', 'Error', true));
}

function saveRotor() {
  const backend = activeSegVal('#rot-backend-seg');
  const body = {
    backend,
    model:   document.getElementById('rot-model').value.trim(),
    comPort: document.getElementById('rot-com').value.trim(),
    tcpHost: document.getElementById('rot-hamlib-host').value.trim()||'localhost',
    tcpPort: parseInt(document.getElementById('rot-hamlib-port').value)||4533,
    shortPathOffset: parseFloat(document.getElementById('rot-short-offset').value)||0,
    customPreset:    parseFloat(document.getElementById('rot-custom').value)||0,
  };
  fetch('/api/rotor', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => { flashMsg('rot-save-msg', 'Saved'); state.config.rotor = body; setText('i-rot-backend', backend); })
    .catch(() => flashMsg('rot-save-msg', 'Error', true));
}

// ── Amp Control ────────────────────────────────────────────
function setAmpBackend(val, btn) {
  document.querySelectorAll('#amp-backend-seg .seg-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  showCond('amp-hamlib-block', val === 'HAMLIB');
}
function setAmpBackendUI(val) {
  document.querySelectorAll('#amp-backend-seg .seg-btn').forEach(b => b.classList.toggle('active', b.dataset.val === val));
  showCond('amp-hamlib-block', val === 'HAMLIB');
}
function loadAmp() {
  fetch('/api/amp').then(r => r.json()).then(amp => {
    setAmpBackendUI(amp.backend || 'NONE');
    setVal('amp-tcp-host',  amp.tcpHost   || 'localhost');
    setVal('amp-tcp-port',  amp.tcpPort   || 4531);
    setVal('amp-model',     amp.model     || '');
    setVal('amp-poll',      amp.pollRateMs|| 1000);
    setVal('amp-swr-fault', amp.swrFault != null ? amp.swrFault : 3.0);
    setChk('amp-band-follow', amp.bandFollow !== false);
    setChk('amp-fault-alert', amp.faultAlert !== false);
  }).catch(() => {});
}
function saveAmp() {
  const backend = activeSegVal('#amp-backend-seg');
  const body = {
    backend,
    tcpHost:    document.getElementById('amp-tcp-host').value.trim()||'localhost',
    tcpPort:    parseInt(document.getElementById('amp-tcp-port').value)||4531,
    model:      document.getElementById('amp-model').value.trim(),
    pollRateMs: parseInt(document.getElementById('amp-poll').value)||1000,
    swrFault:   parseFloat(document.getElementById('amp-swr-fault').value)||3.0,
    bandFollow: document.getElementById('amp-band-follow').checked,
    faultAlert: document.getElementById('amp-fault-alert').checked,
  };
  fetch('/api/amp', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => flashMsg('amp-save-msg', 'Saved'))
    .catch(() => flashMsg('amp-save-msg', 'Error', true));
}

// ── Antenna Switching ─────────────────────────────────────
state.antenna = { switches: [], rules: [] };

function loadAntenna() {
  fetch('/api/antenna').then(r => r.json()).then(ant => {
    setChk('ant-enabled', !!ant.enabled);
    setVal('ant-com',     ant.comPort || '');
    setVal('ant-baud',    ant.baud    || 9600);
    setChk('ant-lockout', ant.lockoutOnPtt !== false);
    state.antenna.switches = ant.switches || [];
    state.antenna.rules    = ant.rules    || [];
    renderAntennaSwitches();
    renderAntennaRules();
  }).catch(() => {});
}

function renderAntennaSwitches() {
  const wrap = document.getElementById('ant-switches-list');
  if (!wrap) return;
  wrap.innerHTML = state.antenna.switches.map((s, i) => `
    <div class="row-card" data-idx="${i}">
      <div class="row-fields">
        <div><label>ID</label><input type="text" value="${esc(s.id||'')}" data-field="id"></div>
        <div><label>Name</label><input type="text" value="${esc(s.name||'')}" data-field="name"></div>
        <div><label>Antenna Count</label><input type="number" min="1" max="16" value="${s.antennaCount||4}" data-field="antennaCount"></div>
      </div>
      <div class="row-actions"><button onclick="removeAntennaSwitch(${i})">✕</button></div>
    </div>`).join('') || '<div style="font-size:12px;color:var(--subtext0)">No switches defined yet.</div>';
  wrap.querySelectorAll('.row-card').forEach(card => {
    const i = parseInt(card.dataset.idx);
    card.querySelectorAll('[data-field]').forEach(inp => {
      inp.addEventListener('input', () => {
        const f = inp.dataset.field;
        state.antenna.switches[i][f] = (f === 'antennaCount') ? parseInt(inp.value)||1 : inp.value;
      });
    });
  });
}

function addAntennaSwitch() {
  state.antenna.switches.push({ id: 'sw' + (state.antenna.switches.length + 1), name: 'Switch', antennaCount: 4 });
  renderAntennaSwitches();
}
function removeAntennaSwitch(i) { state.antenna.switches.splice(i,1); renderAntennaSwitches(); }

function renderAntennaRules() {
  const wrap = document.getElementById('ant-rules-list');
  if (!wrap) return;
  const switchOpts = state.antenna.switches.map(s => `<option value="${esc(s.id)}">${esc(s.id)}</option>`).join('');
  wrap.innerHTML = state.antenna.rules.map((r, i) => `
    <div class="row-card" data-idx="${i}">
      <div class="row-fields">
        <div><label>Band</label>
          <select data-field="band">
            ${['','160m','80m','60m','40m','30m','20m','17m','15m','12m','10m','6m','2m','70cm']
              .map(b => `<option value="${b}" ${r.band===b?'selected':''}>${b||'(any)'}</option>`).join('')}
          </select>
        </div>
        <div><label>Mode</label>
          <select data-field="mode">
            ${['','CW','SSB','FT8','FT4','RTTY','PSK31','JS8','OLIVIA','MFSK16']
              .map(m => `<option value="${m}" ${r.mode===m?'selected':''}>${m||'(any)'}</option>`).join('')}
          </select>
        </div>
        <div><label>Heading min (°)</label><input type="number" value="${r.headingMin!=null && r.headingMin>=0?r.headingMin:''}" data-field="headingMin" placeholder="(any)"></div>
        <div><label>Heading max (°)</label><input type="number" value="${r.headingMax!=null && r.headingMax>=0?r.headingMax:''}" data-field="headingMax" placeholder="(any)"></div>
        <div><label>Switch</label><select data-field="switchId">${switchOpts || '<option value="">(none)</option>'}</select></div>
        <div><label>Antenna #</label><input type="number" min="1" value="${r.antenna||1}" data-field="antenna"></div>
        <div style="grid-column:1/-1"><label>Command Template</label>
          <input type="text" value="${esc(r.commandTemplate||'SW{switch}={antenna}\\\\r')}" data-field="commandTemplate"></div>
      </div>
      <div class="row-actions">
        <button onclick="moveAntennaRule(${i},-1)" title="Up">↑</button>
        <button onclick="moveAntennaRule(${i},1)" title="Down">↓</button>
        <button onclick="removeAntennaRule(${i})">✕</button>
      </div>
    </div>`).join('') || '<div style="font-size:12px;color:var(--subtext0)">No rules defined yet.</div>';
  // Restore selected switchId after innerHTML render (selects lose value otherwise)
  wrap.querySelectorAll('.row-card').forEach(card => {
    const i = parseInt(card.dataset.idx);
    const sel = card.querySelector('[data-field=switchId]');
    if (sel && state.antenna.rules[i].switchId) sel.value = state.antenna.rules[i].switchId;
    card.querySelectorAll('[data-field]').forEach(inp => {
      inp.addEventListener('input', () => {
        const f = inp.dataset.field;
        let v = inp.value;
        if (f === 'antenna') v = parseInt(v)||1;
        else if (f === 'headingMin' || f === 'headingMax') v = v === '' ? -1 : parseFloat(v);
        state.antenna.rules[i][f] = v;
      });
    });
  });
}

function addAntennaRule() {
  const sw = state.antenna.switches[0];
  state.antenna.rules.push({
    band: '20m', mode: '', headingMin: -1, headingMax: -1,
    switchId: sw ? sw.id : 'main', antenna: 1, commandTemplate: 'SW{switch}={antenna}\\r'
  });
  renderAntennaRules();
}
function removeAntennaRule(i) { state.antenna.rules.splice(i,1); renderAntennaRules(); }
function moveAntennaRule(i, dir) {
  const j = i + dir;
  if (j < 0 || j >= state.antenna.rules.length) return;
  const tmp = state.antenna.rules[i]; state.antenna.rules[i] = state.antenna.rules[j]; state.antenna.rules[j] = tmp;
  renderAntennaRules();
}

function saveAntenna() {
  const body = {
    enabled:      document.getElementById('ant-enabled').checked,
    comPort:      document.getElementById('ant-com').value.trim(),
    baud:         parseInt(document.getElementById('ant-baud').value)||9600,
    lockoutOnPtt: document.getElementById('ant-lockout').checked,
    switches:     state.antenna.switches,
    rules:        state.antenna.rules,
  };
  fetch('/api/antenna', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => flashMsg('ant-save-msg', 'Saved'))
    .catch(() => flashMsg('ant-save-msg', 'Error', true));
}
function saveAntennaRules() { saveAntenna(); flashMsg('ant-rules-msg', 'Saved'); }

function pollAntennaStatus() {
  fetch('/api/antenna/status').then(r => r.json()).then(s => {
    setText('ant-st-band',    s.band || '—');
    setText('ant-st-mode',    s.mode || '—');
    setText('ant-st-heading', (s.heading != null && s.heading >= 0) ? s.heading.toFixed(0) + '°' : '—°');
    setText('ant-st-active',  s.activeAntenna != null ? '#' + s.activeAntenna : '—');
    setText('ant-st-rule',    s.matchedRule || 'No matching rule');
  }).catch(() => {});
}

// ── J-Learn ───────────────────────────────────────────────
state.jlearn = { manifest: [], byId: {}, currentId: null };

function loadLearn() {
  fetch('/api/jlearn/manifest').then(r => r.json()).then(list => {
    state.jlearn.manifest = list || [];
    state.jlearn.byId = {};
    for (const e of state.jlearn.manifest) state.jlearn.byId[e.id] = e;
    renderLearnToc();
    // Restore last-opened section if any.
    const last = localStorage.getItem('jl-last');
    if (last && state.jlearn.byId[last]) openLearnSection(last);
  }).catch(() => {});
}

function renderLearnToc() {
  const wrap = document.getElementById('jl-toc');
  if (!wrap) return;
  const filter   = (document.getElementById('jl-search')?.value || '').toLowerCase();
  const advanced = document.getElementById('jl-advanced')?.checked;

  const visible = state.jlearn.manifest.filter(e => {
    if (!advanced && e.level === 'advanced') return false;
    if (!filter) return true;
    return e.title.toLowerCase().includes(filter) || e.id.includes(filter);
  });

  // Group by chapter for the rendered tree.
  const byChapter = {};
  for (const e of visible) {
    (byChapter[e.chapter] = byChapter[e.chapter] || []).push(e);
  }
  const chapters = Object.keys(byChapter).sort();
  wrap.innerHTML = chapters.map(ch => {
    const overview = byChapter[ch].find(e => e.section === '00');
    const sections = byChapter[ch].filter(e => e.section !== '00');
    const chapterTitle = overview ? overview.title.replace(/ — Overview$/, '') : 'Chapter ' + ch;
    return `<div style="margin-bottom:8px">
      <div onclick="openLearnSection('${overview ? overview.id : (sections[0] && sections[0].id) || ''}')"
           style="font-weight:600;font-size:13px;cursor:pointer;padding:3px 4px;border-radius:3px"
           onmouseover="this.style.background='var(--surface1)'"
           onmouseout="this.style.background=''">${ch} · ${esc(chapterTitle)}</div>
      ${sections.map(s => `<div onclick="openLearnSection('${s.id}')"
        style="cursor:pointer;font-size:12px;padding:2px 4px 2px 16px;border-radius:3px;color:var(--subtext0)"
        onmouseover="this.style.background='var(--surface1)';this.style.color='var(--text)'"
        onmouseout="this.style.background='';this.style.color='var(--subtext0)'">
        ${s.section} · ${esc(s.title)}${s.level === 'advanced' ? ' <span style="font-size:10px;color:var(--peach)">⚙️</span>' : ''}</div>`).join('')}
    </div>`;
  }).join('');
}

function filterLearnToc() { renderLearnToc(); }

function openLearnSection(id) {
  if (!id) return;
  state.jlearn.currentId = id;
  localStorage.setItem('jl-last', id);
  fetch('/api/jlearn/content?id=' + encodeURIComponent(id))
    .then(r => r.text())
    .then(md => renderLearnContent(md))
    .catch(e => {
      document.getElementById('jl-viewer').innerHTML =
        '<div style="color:var(--red)">Failed to load section: ' + esc(e.message) + '</div>';
    });
}

function renderLearnContent(md) {
  // Re-fetch when called from the Advanced toggle (no md argument).
  if (md == null) {
    if (state.jlearn.currentId) openLearnSection(state.jlearn.currentId);
    return;
  }
  const advanced = document.getElementById('jl-advanced')?.checked;
  const banner = renderLearnBanner(state.jlearn.currentId);
  document.getElementById('jl-viewer').innerHTML = banner + mdToHtml(stripFrontMatter(md), advanced);
  document.getElementById('jl-viewer').scrollTop = 0;
}

// Per-chapter banner shown above the rendered markdown. Currently used
// only by chapter 03 (Morse code) to surface the standalone trainer app.
function renderLearnBanner(id) {
  if (!id) return '';
  if (id.startsWith('03-')) {
    return '<div style="margin:0 0 14px 0;padding:10px 14px;border-left:3px solid var(--mauve);'
         + 'background:rgba(203,166,247,0.08);border-radius:4px;display:flex;align-items:center;'
         + 'gap:12px;font-size:13px">'
         + '<span style="font-size:18px">🎧</span>'
         + '<div style="flex:1">'
         + '<div style="font-weight:600;color:var(--text)">Morse Code Trainer</div>'
         + '<div style="color:var(--subtext0);font-size:12px">Standalone JavaFX practice app: letter/group/QSO drills, real-time decoder, optional Arduino or Pi Zero keyer.</div>'
         + '</div>'
         + '<button class="action-btn primary" onclick="launchMorseTrainer(this)">▶ Launch Trainer</button>'
         + '</div>';
  }
  return '';
}

function launchMorseTrainer(btn) {
  const original = btn ? btn.textContent : null;
  if (btn) { btn.disabled = true; btn.textContent = 'Launching…'; }
  fetch('/api/morsetrainer/launch', { method: 'POST' })
    .then(r => r.json().then(j => ({ ok: r.ok, body: j })))
    .then(({ ok, body }) => {
      if (btn) {
        btn.textContent = ok ? '✓ Launched' : '✗ Failed';
        if (!ok && body && body.error) console.error('morse-trainer launch:', body.error);
        setTimeout(() => { btn.disabled = false; btn.textContent = original; }, 2500);
      }
    })
    .catch(e => {
      if (btn) {
        btn.textContent = '✗ Failed';
        console.error('morse-trainer launch:', e);
        setTimeout(() => { btn.disabled = false; btn.textContent = original; }, 2500);
      }
    });
}

function stripFrontMatter(md) {
  // Drop a leading YAML block delimited by --- on its own lines.
  if (!md.startsWith('---')) return md;
  const end = md.indexOf('\n---', 3);
  if (end < 0) return md;
  return md.substring(end + 4).replace(/^\s*\n/, '');
}

// Tiny markdown renderer — covers what J-Learn actually uses:
// headings, paragraphs, lists, blockquotes (with the Advanced callout
// marker recognised), code blocks, inline code, bold, italic, links.
// Deliberately no third-party dep — keeps the suite offline-clean.
function mdToHtml(md, includeAdvanced) {
  const lines = md.split(/\r?\n/);
  const out = [];
  let i = 0;
  const escapeHtml = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  const inline = s => escapeHtml(s)
    .replace(/`([^`]+)`/g,           '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g,     '<strong>$1</strong>')
    .replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '<em>$1</em>')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>');

  while (i < lines.length) {
    const line = lines[i];
    // Code fence
    if (line.startsWith('```')) {
      const buf = [];
      i++;
      while (i < lines.length && !lines[i].startsWith('```')) { buf.push(escapeHtml(lines[i])); i++; }
      i++;
      out.push('<pre style="background:var(--mantle);padding:10px;border-radius:6px;overflow-x:auto;font-family:Consolas,monospace;font-size:12px"><code>' + buf.join('\n') + '</code></pre>');
      continue;
    }
    // Headings
    let m;
    if ((m = line.match(/^(#{1,4})\s+(.*)$/))) {
      const level = m[1].length;
      const sizes = { 1: '22px', 2: '18px', 3: '15px', 4: '13px' };
      out.push(`<h${level} style="font-size:${sizes[level]};margin:14px 0 6px 0;color:var(--text)">${inline(m[2])}</h${level}>`);
      i++; continue;
    }
    // Blockquote (with Advanced callout detection)
    if (line.startsWith('>')) {
      const buf = [];
      while (i < lines.length && lines[i].startsWith('>')) {
        buf.push(lines[i].replace(/^>\s?/, ''));
        i++;
      }
      const text = buf.join(' ');
      const isAdvanced = /^⚙️\s+\*\*Advanced\s*—/.test(text.trim());
      if (isAdvanced && !includeAdvanced) continue;  // hide in simple mode
      const style = isAdvanced
        ? 'border-left:3px solid var(--peach);background:rgba(250,179,135,0.06);padding:10px 14px;margin:10px 0;font-size:13px'
        : 'border-left:3px solid var(--blue);background:rgba(137,180,250,0.06);padding:10px 14px;margin:10px 0;font-size:13px';
      out.push('<blockquote style="' + style + '">' + inline(text) + '</blockquote>');
      continue;
    }
    // Unordered list
    if (/^\s*[-*]\s+/.test(line)) {
      const items = [];
      while (i < lines.length && /^\s*[-*]\s+/.test(lines[i])) {
        items.push('<li style="margin:2px 0">' + inline(lines[i].replace(/^\s*[-*]\s+/, '')) + '</li>');
        i++;
      }
      out.push('<ul style="padding-left:22px;margin:8px 0">' + items.join('') + '</ul>');
      continue;
    }
    // Blank line
    if (line.trim() === '') { i++; continue; }
    // Paragraph (gather consecutive non-special lines)
    const para = [];
    while (i < lines.length && lines[i].trim() !== ''
        && !lines[i].startsWith('#')
        && !lines[i].startsWith('>')
        && !lines[i].startsWith('```')
        && !/^\s*[-*]\s+/.test(lines[i])) {
      para.push(lines[i]);
      i++;
    }
    if (para.length) {
      // TODO markers come through as plain HTML comments — show them as a placeholder.
      let html = inline(para.join(' '));
      html = html.replace(/&lt;!--\s*TODO:?\s*content\s*--&gt;/g,
        '<span style="font-style:italic;color:var(--overlay0);font-size:12px">(content not yet written)</span>');
      out.push('<p style="margin:8px 0;line-height:1.5;font-size:14px">' + html + '</p>');
    }
  }
  return out.join('\n');
}

// ── Cloud backup ──────────────────────────────────────────
function loadBackup() {
  fetch('/api/backup').then(r => r.json()).then(b => {
    setChk('bk-enabled',     !!b.enabled);
    setBackupModeUI(b.mode || 'FOLDER');
    setVal('bk-folder',      b.folderPath || '');
    setVal('bk-webdav-url',  b.webdavUrl || '');
    setVal('bk-hours',       b.scheduleHours != null ? b.scheduleHours : 24);
    setVal('bk-retain',      b.retain != null ? b.retain : 14);
    setChk('bk-inc-jhub',    b.includeJHub !== false);
    setChk('bk-inc-jlog',    b.includeJLog !== false);
    setChk('bk-inc-jmap',    b.includeJMap !== false);
    setChk('bk-inc-jsat',    b.includeJSat !== false);
    setChk('bk-inc-jdigi',   !!b.includeJDigi);
    setChk('bk-inc-jbridge', !!b.includeJBridge);
  }).catch(() => {});
  fetch('/api/backup/status').then(r => r.json()).then(s => {
    if (!s) return;
    const sb = document.getElementById('bk-status');
    if (!sb) return;
    sb.textContent = 'Last run: ' +
      (s.lastRunAt ? new Date(s.lastRunAt).toLocaleString() : 'never') +
      ' — ' + (s.lastRunSucceeded ? 'OK' : 'FAILED') +
      ' — ' + (s.lastRunMessage || '') +
      (s.lastBytes > 0 ? ' (' + Math.round(s.lastBytes/1024) + ' KB)' : '');
  }).catch(() => {});
}

function setBackupMode(val, btn) {
  document.querySelectorAll('#bk-mode-seg .seg-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  showCond('bk-folder-block', val === 'FOLDER');
  showCond('bk-webdav-block', val === 'WEBDAV');
}
function setBackupModeUI(val) {
  document.querySelectorAll('#bk-mode-seg .seg-btn').forEach(b => b.classList.toggle('active', b.dataset.val === val));
  showCond('bk-folder-block', val === 'FOLDER');
  showCond('bk-webdav-block', val === 'WEBDAV');
}

function saveBackup() {
  const body = {
    enabled:        document.getElementById('bk-enabled').checked,
    mode:           activeSegVal('#bk-mode-seg') || 'FOLDER',
    folderPath:     document.getElementById('bk-folder').value.trim(),
    webdavUrl:      document.getElementById('bk-webdav-url').value.trim(),
    scheduleHours:  parseInt(document.getElementById('bk-hours').value)  || 0,
    retain:         parseInt(document.getElementById('bk-retain').value) || 14,
    includeJHub:    document.getElementById('bk-inc-jhub').checked,
    includeJLog:    document.getElementById('bk-inc-jlog').checked,
    includeJMap:    document.getElementById('bk-inc-jmap').checked,
    includeJSat:    document.getElementById('bk-inc-jsat').checked,
    includeJDigi:   document.getElementById('bk-inc-jdigi').checked,
    includeJBridge: document.getElementById('bk-inc-jbridge').checked,
  };
  fetch('/api/backup', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => flashMsg('bk-msg', 'Saved'))
    .catch(() => flashMsg('bk-msg', 'Error', true));
}

function runBackupNow() {
  flashMsg('bk-msg', 'Running…');
  fetch('/api/backup/run', { method: 'POST' })
    .then(r => r.json())
    .then(r => {
      flashMsg('bk-msg', r.success ? 'Backup OK' : ('Failed: ' + r.message), !r.success);
      loadBackup();
    })
    .catch(e => flashMsg('bk-msg', 'Error: ' + e.message, true));
}

function saveWebdavCreds() {
  const user = document.getElementById('bk-webdav-user').value;
  const pass = document.getElementById('bk-webdav-pass').value;
  if (!user || !pass) { flashMsg('bk-cred-msg', 'Enter user + password', true); return; }
  fetch('/api/credentials/webdav', { method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ user, pass }) })
    .then(r => r.json())
    .then(() => {
      flashMsg('bk-cred-msg', 'Saved (encrypted)');
      document.getElementById('bk-webdav-pass').value = '';
    })
    .catch(() => flashMsg('bk-cred-msg', 'Error', true));
}

// ── Log uploaders ─────────────────────────────────────────
function loadUploaders() {
  fetch('/api/uploaders').then(r => r.json()).then(list => renderUploaders(list)).catch(() => {});
}

function renderUploaders(list) {
  const wrap = document.getElementById('uploaders-list');
  if (!wrap) return;
  wrap.innerHTML = (list || []).map(u => {
    const fields = (u.fields || []).map(f => `
      <div class="field" style="margin-right:8px;min-width:140px">
        <input type="${f === 'pass' || f === 'apiKey' || f === 'uploadCode' ? 'password' : 'text'}"
               id="cred-${u.serviceId}-${f}" autocomplete="off">
        <label>${f}</label>
      </div>`).join('');
    return `<div class="row-card" data-svc="${u.serviceId}">
      <div class="row-fields" style="grid-template-columns:1fr">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div>
            <div style="font-weight:600">${esc(u.displayName)}</div>
            <div style="font-size:12px;color:var(--subtext0)">
              ${u.configured ? '✓ credentials saved' : '<span style="color:var(--peach)">not configured</span>'}
              · ${u.uploaded} uploaded · ${u.pending < 0 ? '?' : u.pending} pending
              ${u.totalQsos < 0 ? ' (j-log.db not found)' : ''}
            </div>
          </div>
          <div>
            <button class="btn btn-primary btn-sm" onclick="uploadNow('${u.serviceId}')">Upload pending</button>
          </div>
        </div>
        <div style="display:flex;flex-wrap:wrap;align-items:flex-end;margin-top:8px">
          ${fields}
          <button class="btn btn-ghost btn-sm" onclick="saveUploaderCreds('${u.serviceId}',[${
            (u.fields || []).map(f => `'${f}'`).join(',')}])">Save credentials</button>
          <span id="cred-msg-${u.serviceId}" style="font-size:12px;color:var(--overlay0);margin-left:8px"></span>
        </div>
      </div>
    </div>`;
  }).join('') || '<div style="color:var(--subtext0)">No uploaders registered.</div>';
}

function saveUploaderCreds(serviceId, fields) {
  const body = {};
  let any = false;
  for (const f of fields) {
    const el = document.getElementById('cred-' + serviceId + '-' + f);
    if (el && el.value) { body[f] = el.value; any = true; }
  }
  if (!any) { flashMsg('cred-msg-' + serviceId, 'Enter all fields first', true); return; }
  fetch('/api/credentials/' + serviceId, { method: 'POST',
    headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => {
      flashMsg('cred-msg-' + serviceId, 'Saved (encrypted)');
      // Clear password fields immediately so they don't sit in the DOM
      for (const f of fields) {
        const el = document.getElementById('cred-' + serviceId + '-' + f);
        if (el && el.type === 'password') el.value = '';
      }
      setTimeout(loadUploaders, 1000);
    })
    .catch(() => flashMsg('cred-msg-' + serviceId, 'Error', true));
}

function uploadNow(serviceId) {
  flashMsg('cred-msg-' + serviceId, 'Uploading…');
  fetch('/api/uploaders/upload/' + serviceId, { method: 'POST' })
    .then(r => r.json())
    .then(r => {
      flashMsg('cred-msg-' + serviceId,
        r.success ? (r.qsosUploaded + ' uploaded') : ('Failed: ' + r.message),
        !r.success);
      setTimeout(loadUploaders, 1500);
    })
    .catch(e => flashMsg('cred-msg-' + serviceId, 'Error: ' + e.message, true));
}

// ── Reverse Beacon Network feed ───────────────────────────
function loadRbn() {
  fetch('/api/rbn').then(r => r.json()).then(d => {
    const cfg = d.config || {};
    setChk('rbn-enabled', !!cfg.enabled);
    setVal('rbn-server',  cfg.server || 'telnet.reversebeacon.net');
    setVal('rbn-port',    cfg.port   || 7000);
    setVal('rbn-login',   cfg.loginCallsign || '');
    setVal('rbn-snr',     cfg.minSnrDb != null ? cfg.minSnrDb : 5);
    buildFilterChips('rbn-band-filters',
      ['160m','80m','60m','40m','30m','20m','17m','15m','12m','10m','6m','2m','70cm'],
      Array.from(cfg.bands || []));
    buildFilterChips('rbn-mode-filters',
      ['CW','SSB','FT8','FT4','RTTY','PSK31','JS8'],
      Array.from(cfg.modes || []));
    const dot = document.getElementById('rbn-dot');
    const txt = document.getElementById('rbn-status-txt');
    if (dot) dot.className = 'dot ' + (d.connected ? 'green' : (d.running ? 'yellow' : 'gray'));
    if (txt) txt.textContent = d.connected ? 'connected'
                            : d.running   ? 'reconnecting…'
                            :               (cfg.enabled ? 'enabled, not connected' : 'disabled');
  }).catch(() => {});
}

function saveRbn() {
  const body = {
    enabled:       document.getElementById('rbn-enabled').checked,
    server:        document.getElementById('rbn-server').value.trim() || 'telnet.reversebeacon.net',
    port:          parseInt(document.getElementById('rbn-port').value) || 7000,
    loginCallsign: document.getElementById('rbn-login').value.toUpperCase().trim(),
    minSnrDb:      parseInt(document.getElementById('rbn-snr').value),
    bands:         getCheckedChips('rbn-band-filters'),
    modes:         getCheckedChips('rbn-mode-filters'),
  };
  if (isNaN(body.minSnrDb)) body.minSnrDb = 5;
  fetch('/api/rbn', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => { flashMsg('rbn-msg', 'Saved'); setTimeout(loadRbn, 1500); })
    .catch(() => flashMsg('rbn-msg', 'Error', true));
}

function saveCluster() {
  const host  = document.getElementById('cl-host').value.trim();
  const port  = parseInt(document.getElementById('cl-port').value)||7373;
  const login = document.getElementById('cl-login').value.toUpperCase().trim();
  const auto  = document.getElementById('cl-auto').checked;
  const bands = getCheckedChips('band-filters');
  const modes = getCheckedChips('mode-filters');

  const body = {
    cluster: { server: host, port, loginCallsign: login, autoConnect: auto,
               filters: { bands, modes } }
  };
  postPartialConfig(body, 'cl-msg', 'Cluster settings saved');
}

function saveFilters() {
  const bands = getCheckedChips('band-filters');
  const modes = getCheckedChips('mode-filters');
  const body = { cluster: { filters: { bands, modes } } };
  postPartialConfig(body, 'cl-msg', 'Filters saved');
}

function saveLogging() {
  const body = {
    logger: {
      mode: document.getElementById('log-mode').value,
      normalLog: { dbPath: document.getElementById('log-db-path').value.trim() },
    }
  };
  postPartialConfig(body, 'log-msg', 'Logging settings saved');
}

function savePorts() {
  const body = {
    jHub: {
      ip:            document.getElementById('jhub-ip').value.trim() || 'localhost',
      websocketPort: parseInt(document.getElementById('ws-port').value)||8080,
      webConfigPort: parseInt(document.getElementById('web-port').value)||8081,
    }
  };
  postPartialConfig(body, 'ports-msg', 'Ports saved — restart required');
}

function saveMacros() {
  const list = collectMacros();
  fetch('/api/macros', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(list) })
    .then(r => r.json())
    .then(() => flashMsg('macro-msg', 'Macros saved'))
    .catch(() => flashMsg('macro-msg', 'Error', true));
}

function saveModuleCmd(key, id) {
  const cmd  = (document.getElementById('cmd-' + key) || {}).value || '';
  const auto = (document.getElementById('auto-' + key) || {}).checked || false;
  const ip   = (document.getElementById('ip-' + key)   || {}).value || 'localhost';
  const appsUpdate = {};
  appsUpdate[key] = { command: cmd.trim(), autoLaunch: auto, ip: ip.trim() };
  postPartialConfig({ apps: appsUpdate }, 'mod-msg-' + key, 'Saved');
}

// Merge-patch style partial config update
function postPartialConfig(patch, msgId, okText) {
  const merged = deepMerge(state.config, patch);
  fetch('/api/config', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(merged) })
    .then(r => r.json().then(data => ({ ok: r.ok, data })))
    .then(({ ok, data }) => {
      if (!ok) { flashMsg(msgId, data.error || 'Error', true); return; }
      state.config = merged;
      flashMsg(msgId, okText);
    })
    .catch(() => flashMsg(msgId, 'Error', true));
}

function deepMerge(target, source) {
  const out = Object.assign({}, target);
  for (const k of Object.keys(source)) {
    if (source[k] && typeof source[k] === 'object' && !Array.isArray(source[k])) {
      out[k] = deepMerge(out[k] || {}, source[k]);
    } else {
      out[k] = source[k];
    }
  }
  return out;
}

// ── App launch / kill ──────────────────────────────────────
function launchApp(name) {
  const cmdEl = document.getElementById('cmd-' + name) || document.getElementById('cmd-j' + name);
  const command = cmdEl ? cmdEl.value.trim() : null;
  const body = command ? JSON.stringify({ command }) : '{}';
  fetch('/api/apps/launch/' + name, { method: 'POST', headers: {'Content-Type':'application/json'}, body })
    .then(r => r.json())
    .then(d => { if (d.error) alert('Launch error: ' + d.error); })
    .catch(() => {});
}

function killApp(name) {
  fetch('/api/apps/kill/' + name, { method: 'POST' })
    .then(r => r.json())
    .catch(() => {});
}

// ── Saved DX networks ──────────────────────────────────────
function loadNetworks() {
  fetch('/api/cluster/networks')
    .then(r => r.json())
    .then(nets => {
      const sel = document.getElementById('cl-network-select');
      if (!sel) return;
      const prev = sel.value;
      sel.innerHTML = '<option value="">— manual entry —</option>';
      (nets || []).forEach(n => {
        const opt = document.createElement('option');
        opt.value = n.name;
        opt.textContent = n.name + '  (' + n.server + ':' + n.port + ')';
        sel.appendChild(opt);
      });
      if (prev) sel.value = prev;
      document.getElementById('cl-del-btn').style.display = sel.value ? 'inline-block' : 'none';
      state.dxNetworks = nets || [];
    })
    .catch(() => {});
}

function onNetworkSelect() {
  const sel   = document.getElementById('cl-network-select');
  const name  = sel.value;
  const delBtn = document.getElementById('cl-del-btn');
  delBtn.style.display = name ? 'inline-block' : 'none';
  if (!name) return;
  const net = (state.dxNetworks || []).find(n => n.name === name);
  if (!net) return;
  setVal('cl-host',    net.server        || '');
  setVal('cl-port',    net.port          || 7373);
  setVal('cl-login',   net.loginCallsign || '');
  setVal('cl-net-name', net.name);
}

function saveNetwork() {
  const name  = (document.getElementById('cl-net-name').value || '').trim();
  const host  = document.getElementById('cl-host').value.trim();
  const port  = parseInt(document.getElementById('cl-port').value) || 7373;
  const login = document.getElementById('cl-login').value.toUpperCase().trim();
  if (!name)  { flashMsg('cl-net-msg', 'Enter a network name', true); return; }
  if (!host)  { flashMsg('cl-net-msg', 'Enter a host', true);         return; }
  fetch('/api/cluster/networks', { method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ name, server: host, port, loginCallsign: login }) })
    .then(r => r.json())
    .then(() => {
      flashMsg('cl-net-msg', '\u2713 Saved');
      loadNetworks();
      setTimeout(() => {
        const sel = document.getElementById('cl-network-select');
        if (sel) { sel.value = name; onNetworkSelect(); }
      }, 200);
    })
    .catch(() => flashMsg('cl-net-msg', 'Error saving', true));
}

function deleteNetwork() {
  const sel  = document.getElementById('cl-network-select');
  const name = sel && sel.value;
  if (!name) return;
  if (!confirm('Delete network "' + name + '"?')) return;
  fetch('/api/cluster/networks', { method: 'DELETE',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ name }) })
    .then(r => r.json())
    .then(() => {
      flashMsg('cl-net-msg', 'Deleted');
      loadNetworks();
    })
    .catch(() => flashMsg('cl-net-msg', 'Error', true));
}

// ── Cluster actions ────────────────────────────────────────
function clusterConnect() {
  const sel  = document.getElementById('cl-network-select');
  const name = sel && sel.value;
  let body;
  if (name) {
    body = { networkName: name };
  } else {
    body = {
      server:        document.getElementById('cl-host').value.trim(),
      port:          parseInt(document.getElementById('cl-port').value) || 7373,
      loginCallsign: document.getElementById('cl-login').value.trim()
    };
  }
  fetch('/api/cluster/connect', { method: 'POST', headers: {'Content-Type':'application/json'},
    body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => flashMsg('cl-msg', 'Connecting…'))
    .catch(() => flashMsg('cl-msg', 'Error', true));
}

function clusterDisconnect() {
  fetch('/api/cluster/disconnect', { method: 'POST' })
    .then(() => flashMsg('cl-msg', 'Disconnected'));
}

function refreshSpots() {
  pollSpots();
}

// ── Quick actions ──────────────────────────────────────────
function reconnectRig() {
  fetch('/api/rig/reconnect', { method: 'POST' })
    .then(r => r.json())
    .then(() => { flashMsg('sb-rig-poll', 'Reconnecting\u2026', false); updateRigConnStatus(); })
    .catch(() => flashMsg('sb-rig-poll', 'Err', true));
}
function reconnectCluster() { clusterConnect(); }
function reloadConfig()     { loadConfig(); loadMacros(); }
function restartWs() {
  if (ws) ws.close();
}

// ── PTT test ───────────────────────────────────────────────
function testPtt() {
  fetch('/api/rig/ptt', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ptt: true })
  })
  .then(r => r.json())
  .then(() => {
    flashMsg('rig-save-msg', 'TX keyed\u2026', false);
    setTimeout(() => {
      fetch('/api/rig/ptt', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ptt: false })
      })
      .then(() => flashMsg('rig-save-msg', 'PTT released'))
      .catch(() => flashMsg('rig-save-msg', 'Release error', true));
    }, 1000);
  })
  .catch(() => flashMsg('rig-save-msg', 'PTT error', true));
}

// ── Rig connection status ──────────────────────────────────
function updateRigConnStatus() {
  fetch('/api/rig/status')
    .then(r => r.json())
    .then(s => {
      const el = document.getElementById('rig-conn-status');
      if (!el) return;
      if (!s.running) {
        el.textContent = 'Disabled'; el.className = 'rig-conn-badge off';
      } else if (s.connected) {
        el.textContent = 'Connected'; el.className = 'rig-conn-badge ok';
      } else {
        el.textContent = 'Connecting\u2026'; el.className = 'rig-conn-badge warn';
      }
    })
    .catch(() => {});
}

// ── Rotor manual ───────────────────────────────────────────
const BEARING_MAP = { N:0, NE:45, E:90, SE:135, S:180, SW:225, W:270, NW:315 };
function rotorCmd(dir) {
  const hdg = BEARING_MAP[dir];
  if (hdg == null) return;
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'ROTOR_CMD', heading: hdg }));
  }
}

function rotorPreset(type) {
  const rot = state.config.rotor || {};
  let hdg = 0;
  if (type === 'short') hdg = rot.shortPathOffset || 0;
  else if (type === 'long') hdg = ((rot.shortPathOffset || 0) + 180) % 360;
  else if (type === 'custom') hdg = rot.customPreset || 0;
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'ROTOR_CMD', heading: hdg }));
  }
}

function rotorElevJog(delta) {
  const currentEl = state.rotor && state.rotor.elevation != null ? state.rotor.elevation : 0;
  const newEl = Math.min(90, Math.max(0, currentEl + delta));
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'ROTOR_CMD', elevation: newEl }));
  }
}

function rotorElevPark() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'ROTOR_CMD', elevation: 0 }));
  }
}

function rotorStop() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'ROTOR_CMD', stop: true }));
  }
}

// ── Config export / import ─────────────────────────────────
function exportConfig() {
  fetch('/api/config').then(r => r.json()).then(cfg => {
    const blob = new Blob([JSON.stringify(cfg, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'j-hub-config.json';
    a.click();
  });
}

function importConfig() {
  document.getElementById('import-file').click();
}

function handleImport(input) {
  const file = input.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = e => {
    try {
      const cfg = JSON.parse(e.target.result);
      fetch('/api/config', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(cfg) })
        .then(() => { loadConfig(); loadMacros(); alert('Config imported successfully'); })
        .catch(() => alert('Import failed'));
    } catch(ex) { alert('Invalid JSON: ' + ex.message); }
  };
  reader.readAsText(file);
}

function exportDiag() {
  // Pulls a zip from j-hub containing every module's logs/*.log + a config
  // snapshot, current sessions, deps check, and environment info. Attach the
  // resulting file to bug reports.
  fetch('/api/diagnostics/bundle')
    .then(r => {
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.blob();
    })
    .then(blob => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      const stamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
      a.download = 'ars-diag-' + stamp + '.zip';
      a.click();
    })
    .catch(() => alert('Diagnostics bundle failed — check j-hub logs'));
}

// ── Utilities ──────────────────────────────────────────────
function setText(id, val) {
  const el = document.getElementById(id);
  if (el) el.textContent = val;
}

function setVal(id, val) {
  const el = document.getElementById(id);
  if (el && el.value !== undefined) el.value = val;
}

function setChk(id, val) {
  const el = document.getElementById(id);
  if (el) el.checked = !!val;
}

function setSelectVal(id, val) {
  const el = document.getElementById(id);
  if (!el) return;
  const opt = el.querySelector(`option[value="${val}"]`);
  if (opt) opt.selected = true;
}

function populateTimezones() {
  const sel = document.getElementById('st-tz');
  if (!sel) return;
  let zones;
  try {
    zones = Intl.supportedValuesOf('timeZone');
  } catch(_) {
    zones = [
      'UTC',
      'America/Anchorage','America/Chicago','America/Denver','America/Los_Angeles',
      'America/New_York','America/Phoenix','America/Sao_Paulo','America/Toronto',
      'America/Vancouver',
      'Europe/Amsterdam','Europe/Athens','Europe/Berlin','Europe/Brussels',
      'Europe/Budapest','Europe/Copenhagen','Europe/Dublin','Europe/Helsinki',
      'Europe/Lisbon','Europe/London','Europe/Madrid','Europe/Moscow',
      'Europe/Oslo','Europe/Paris','Europe/Prague','Europe/Rome',
      'Europe/Stockholm','Europe/Vienna','Europe/Warsaw','Europe/Zurich',
      'Asia/Bangkok','Asia/Colombo','Asia/Dubai','Asia/Hong_Kong',
      'Asia/Istanbul','Asia/Jakarta','Asia/Jerusalem','Asia/Karachi',
      'Asia/Kolkata','Asia/Kuala_Lumpur','Asia/Manila','Asia/Seoul',
      'Asia/Shanghai','Asia/Singapore','Asia/Taipei','Asia/Tokyo',
      'Australia/Adelaide','Australia/Brisbane','Australia/Melbourne',
      'Australia/Perth','Australia/Sydney',
      'Pacific/Auckland','Pacific/Honolulu',
      'Africa/Cairo','Africa/Johannesburg','Africa/Lagos','Africa/Nairobi',
    ];
  }
  sel.innerHTML = zones.map(z => `<option value="${z}">${z.replace(/_/g,' ')}</option>`).join('');
}

function setDot(id, color) {
  const el = document.getElementById(id);
  if (!el) return;
  el.className = 'dot ' + color;
}

function showCond(id, visible) {
  const el = document.getElementById(id);
  if (el) el.classList.toggle('show', visible);
}

function activeSegVal(selector) {
  const btn = document.querySelector(selector + ' .seg-btn.active');
  return btn ? btn.dataset.val : '';
}

function flashMsg(id, text, isError) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = text;
  el.style.color = isError ? 'var(--red)' : 'var(--green)';
  setTimeout(() => { if (el.textContent === text) { el.textContent = ''; el.style.color = ''; } }, 3000);
}

function esc(s) {
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

// ── J-Map settings ────────────────────────────────────────
async function uploadJMapImage(fileInputId, endpoint, msgId) {
  const input = document.getElementById(fileInputId);
  const msg   = document.getElementById(msgId);
  if (!input || !input.files.length) { if (msg) msg.textContent = 'Choose a file first'; return; }
  const fd = new FormData();
  fd.append('image', input.files[0]);
  if (msg) { msg.style.color = ''; msg.textContent = 'Uploading…'; }
  try {
    const res  = await fetch(endpoint, { method: 'POST', body: fd });
    const data = await res.json();
    if (res.ok) {
      if (msg) { msg.style.color = 'var(--green)'; msg.textContent = '✓ Applied'; }
    } else {
      throw new Error(data.error || 'Upload failed');
    }
  } catch (e) {
    if (msg) { msg.style.color = 'var(--red)'; msg.textContent = '✗ ' + e.message; }
  }
  setTimeout(() => { if (msg) { msg.textContent = ''; msg.style.color = ''; } }, 5000);
}

function loadJMapSettings() {
  fetch('/api/jmap')
    .then(r => r.json())
    .then(s => populateJMapForm(s))
    .catch(() => {});
  // Replace the placeholder hostname in the "remote J-Map" snippet with
  // whatever the browser used to reach J-Hub. Works for IP, .local, FQDN.
  const hint = document.getElementById('jmap-hub-host-hint');
  if (hint) hint.textContent = location.hostname || 'your-shack-pc.local';
}

function populateJMapForm(s) {
  const fsEl  = document.getElementById('jm-font-size');
  const fsLbl = document.getElementById('jm-font-size-val');
  if (fsEl)  fsEl.value = s.fontSize || 13;
  if (fsLbl) fsLbl.textContent = s.fontSize || 13;

  const setFontSlider = (id, valId, v) => {
    const el  = document.getElementById(id);
    const lbl = document.getElementById(valId);
    const n   = (typeof v === 'number' ? v : 0);
    if (el)  el.value = n;
    if (lbl) lbl.textContent = (n === 0 ? 'auto' : n);
  };
  setFontSlider('jm-font-de',      'jm-font-de-val',      s.deInfoFontSize);
  setFontSlider('jm-font-dx',      'jm-font-dx-val',      s.dxInfoFontSize);
  setFontSlider('jm-font-contest', 'jm-font-contest-val', s.contestListFontSize);
  setFontSlider('jm-font-prop',    'jm-font-prop-val',    s.propagationFontSize);
  setFontSlider('jm-font-lunar',   'jm-font-lunar-val',   s.lunarFontSize);

  setChk('jm-mock',      !!s.useMockData);
  setVal('jm-noaa-key',  s.noaaApiKey         || '');
  setVal('jm-owm-key',   s.openWeatherApiKey  || '');

  setChk('jm-worldmap',  s.showWorldMap  !== false);
  setChk('jm-grayline',     s.showGrayline    !== false);
  setChk('jm-dxspots',      s.showDxSpots     !== false);
  setChk('jm-sunposition',  s.showSunPosition !== false);
  const opEl = document.getElementById('jm-grayline-opacity');
  const opLbl = document.getElementById('jm-grayline-opacity-val');
  if (opEl) { opEl.value = s.graylineOpacity != null ? s.graylineOpacity : 0.6; }
  if (opLbl) opLbl.textContent = parseFloat(opEl ? opEl.value : 0.6).toFixed(2);

  setChk('jm-aurora',    !!s.showAuroraOverlay);
  setChk('jm-geomag',    !!s.showGeomagneticAlerts);
  setChk('jm-satellite', !!s.showSatelliteTracking);

  setChk('jm-weather',   !!s.showWeatherOverlay);
  setChk('jm-tropo',     !!s.showTropoOverlay);
  setChk('jm-radar',     !!s.showRadarOverlay);
  setChk('jm-lightning', !!s.showLightningOverlay);
  setChk('jm-fronts',    !!s.showFrontsOverlay);
  setChk('jm-surface',   !!s.showSurfaceConditions);

  setChk('jm-cqzones',   !!s.showCqZones);
  setChk('jm-ituzones',  !!s.showItuZones);
  setChk('jm-gridsq',    !!s.showGridSquares);
  setChk('jm-rotormap',  s.showRotorMap !== false);

  setChk('jm-dewindow',  s.showDeWindow !== false);
  setChk('jm-dxwindow',  !!s.showDxWindow);
  setChk('jm-countdown', !!s.showCountdownTimer);
  setChk('jm-contests',  !!s.showContestList);

  setSelectVal('jm-dx-band', s.dxBandFilter || 'ALL');
  setVal('jm-dx-maxage',   s.dxMaxAgeMinutes != null ? s.dxMaxAgeMinutes : 30);
  setChk('jm-dx-callsigns', s.dxShowCallsigns !== false);

  setChk('jm-localtime', s.showLocalTime !== false);
  setChk('jm-utctime',   s.showUtcTime   !== false);
  setVal('jm-tz2',       s.secondaryTimezone || '');

  setChk('jm-solar',    s.showSolarData       !== false);
  setChk('jm-sunspot',  s.showSunspotGraphic  !== false);
  setChk('jm-prop',     s.showPropagationData !== false);
  setChk('jm-bandcond', s.showBandConditions  !== false);

  setSelectVal('jm-map-style',     s.mapStyle     || 'BLUE_MARBLE');
  setSelectVal('jm-map-view',      s.mapView      || 'WORLD');
  setSelectVal('jm-tile-provider', s.tileProvider || 'FLAT');
  setChk('jm-dxpaths',             s.showDxPaths !== false);
  setVal('jm-zoom',       s.mapZoom       != null ? s.mapZoom       : 2);
  setVal('jm-center-lat', s.mapCenterLat  != null ? s.mapCenterLat  : 0);
  setVal('jm-center-lon', s.mapCenterLon  != null ? s.mapCenterLon  : 0);
  setVal('jm-refresh',    s.refreshSeconds != null ? s.refreshSeconds : 30);
}

function saveJMapSettings() {
  const chk  = id => document.getElementById(id) && document.getElementById(id).checked;
  const val  = id => { const el = document.getElementById(id); return el ? el.value : ''; };
  const flt  = id => { const el = document.getElementById(id); return el ? parseFloat(el.value) : 0; };
  const intn = id => { const el = document.getElementById(id); return el ? parseInt(el.value) || 0 : 0; };

  const settings = {
    fontSize:               intn('jm-font-size') || 13,
    deInfoFontSize:         intn('jm-font-de'),
    dxInfoFontSize:         intn('jm-font-dx'),
    contestListFontSize:    intn('jm-font-contest'),
    propagationFontSize:    intn('jm-font-prop'),
    lunarFontSize:          intn('jm-font-lunar'),
    useMockData:            chk('jm-mock'),
    noaaApiKey:             val('jm-noaa-key').trim(),
    openWeatherApiKey:      val('jm-owm-key').trim(),
    showWorldMap:           chk('jm-worldmap'),
    showGrayline:           chk('jm-grayline'),
    showDxSpots:            chk('jm-dxspots'),
    showSunPosition:        chk('jm-sunposition'),
    graylineOpacity:        flt('jm-grayline-opacity'),
    showAuroraOverlay:      chk('jm-aurora'),
    showGeomagneticAlerts:  chk('jm-geomag'),
    showSatelliteTracking:  chk('jm-satellite'),
    showWeatherOverlay:     chk('jm-weather'),
    showTropoOverlay:       chk('jm-tropo'),
    showRadarOverlay:       chk('jm-radar'),
    showLightningOverlay:   chk('jm-lightning'),
    showFrontsOverlay:      chk('jm-fronts'),
    showSurfaceConditions:  chk('jm-surface'),
    showCqZones:            chk('jm-cqzones'),
    showItuZones:           chk('jm-ituzones'),
    showGridSquares:        chk('jm-gridsq'),
    showRotorMap:           chk('jm-rotormap'),
    showDeWindow:           chk('jm-dewindow'),
    showDxWindow:           chk('jm-dxwindow'),
    showCountdownTimer:     chk('jm-countdown'),
    showContestList:        chk('jm-contests'),
    dxBandFilter:           val('jm-dx-band'),
    dxMaxAgeMinutes:        intn('jm-dx-maxage') || 30,
    dxShowCallsigns:        chk('jm-dx-callsigns'),
    showLocalTime:          chk('jm-localtime'),
    showUtcTime:            chk('jm-utctime'),
    secondaryTimezone:      val('jm-tz2').trim(),
    showSolarData:          chk('jm-solar'),
    showSunspotGraphic:     chk('jm-sunspot'),
    showPropagationData:    chk('jm-prop'),
    showBandConditions:     chk('jm-bandcond'),
    mapStyle:               val('jm-map-style') || 'BLUE_MARBLE',
    mapView:                val('jm-map-view') || 'WORLD',
    showDxPaths:            chk('jm-dxpaths'),
    tileProvider:           val('jm-tile-provider') || 'FLAT',
    mapZoom:                intn('jm-zoom') || 2,
    mapCenterLat:           flt('jm-center-lat'),
    mapCenterLon:           flt('jm-center-lon'),
    refreshSeconds:         intn('jm-refresh') || 30,
  };

  fetch('/api/jmap', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(settings) })
    .then(r => r.json().then(data => ({ ok: r.ok, data })))
    .then(({ ok, data }) => {
      if (!ok) { flashMsg('jmap-msg', data.error || 'Error', true); return; }
      flashMsg('jmap-msg', 'Saved — applied to J-Map via WebSocket');
    })
    .catch(() => flashMsg('jmap-msg', 'Error', true));
}

// ── J-Log settings (font size only — launch config removed) ──
function loadJLogSettings() {
  fetch('/api/jlog')
    .then(r => r.json())
    .then(s => populateJLogForm(s))
    .catch(() => {});
}

function populateJLogForm(s) {
  const setSlider = (id, valId, v, dflt) => {
    const el  = document.getElementById(id);
    const lbl = document.getElementById(valId);
    const n   = (typeof v === 'number') ? v : dflt;
    if (el)  el.value = n;
    if (lbl) lbl.textContent = n;
  };
  setSlider('jlog-font-size',      'jlog-font-size-val',      s.fontSize, 13);
  const f = s.fonts || {};
  setSlider('jlog-font-statusbar', 'jlog-font-statusbar-val', f.statusBar, 12);
  setSlider('jlog-font-entry',     'jlog-font-entry-val',     f.entry,     13);
  setSlider('jlog-font-table',     'jlog-font-table-val',     f.table,     12);
  setSlider('jlog-font-info',      'jlog-font-info-val',      f.info,      12);
  setSlider('jlog-font-spots',     'jlog-font-spots-val',     f.spots,     12);

  const wx = document.getElementById('jlog-show-spacewx');
  if (wx) wx.checked = (s.showSpaceWeather !== false);   // default true
}

function saveJLogSettings() {
  const intOf = id => parseInt(document.getElementById(id).value, 10);
  const wxEl  = document.getElementById('jlog-show-spacewx');
  const payload = {
    fontSize: intOf('jlog-font-size') || 13,
    showSpaceWeather: wxEl ? wxEl.checked : true,
    fonts: {
      statusBar: intOf('jlog-font-statusbar') || 12,
      entry:     intOf('jlog-font-entry')     || 13,
      table:     intOf('jlog-font-table')     || 12,
      info:      intOf('jlog-font-info')      || 12,
      spots:     intOf('jlog-font-spots')     || 12,
    }
  };
  fetch('/api/jlog', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(payload)
  })
  .then(r => r.json())
  .then(() => flashMsg('jlog-msg', 'J-Log settings saved — restart J-Log for per-pane overrides'))
  .catch(() => flashMsg('jlog-msg', 'Error', true));
}

function restartJLog() {
  // Best-effort: save current settings first, then ask j-hub to shut down +
  // re-launch the j-log process. The UI's stop() hook flushes SQLite writes
  // and closes the WebSocket cleanly before exit.
  saveJLogSettings();
  fetch('/api/jlog/restart', { method: 'POST' })
    .then(r => r.json())
    .then(() => flashMsg('jlog-msg', 'J-Log restart requested — it will reappear in a few seconds'))
    .catch(() => flashMsg('jlog-msg', 'Restart failed', true));
}

/** Save-then-restart helper for j-map / j-digi / j-bridge / j-sat. */
function _saveAndRestart(saveFn, endpoint, msgEl) {
  try { saveFn(); } catch (ignored) {}
  fetch(endpoint, { method: 'POST' })
    .then(r => r.json())
    .then(() => flashMsg(msgEl, 'Restart requested — the app will reappear in a few seconds'))
    .catch(() => flashMsg(msgEl, 'Restart failed', true));
}

function restartJMap()    { _saveAndRestart(saveJMapSettings,    '/api/jmap/restart',    'jmap-msg');    }
function restartJDigi()   { _saveAndRestart(saveJDigiSettings,   '/api/jdigi/restart',   'jdigi-msg');   }
function restartJBridge() { _saveAndRestart(saveJBridgeSettings, '/api/jbridge/restart', 'jbridge-msg'); }
function restartJSat()    { _saveAndRestart(saveJSatSettings,    '/api/jsat/restart',    'jsat-msg');    }

// ── J-Digi settings (font size only — launch config removed) ─
function loadJDigiSettings() {
  fetch('/api/jdigi')
    .then(r => r.json())
    .then(s => populateJDigiForm(s))
    .catch(() => {});
}

function populateJDigiForm(s) {
  const fsEl  = document.getElementById('jdigi-font-size');
  const fsLbl = document.getElementById('jdigi-font-size-val');
  if (fsEl)  fsEl.value = s.fontSize || 13;
  if (fsLbl) fsLbl.textContent = s.fontSize || 13;

  const setFontSlider = (id, valId, v) => {
    const el  = document.getElementById(id);
    const lbl = document.getElementById(valId);
    const n   = (typeof v === 'number' ? v : 0);
    if (el)  el.value = n;
    if (lbl) lbl.textContent = (n === 0 ? 'auto' : n);
  };
  const f = s.fonts || {};
  setFontSlider('jdigi-font-rxtx',      'jdigi-font-rxtx-val',      f.rxTx);
  setFontSlider('jdigi-font-freq',      'jdigi-font-freq-val',      f.freq);
  setFontSlider('jdigi-font-statusbar', 'jdigi-font-statusbar-val', f.statusBar);
  setFontSlider('jdigi-font-toolbar',   'jdigi-font-toolbar-val',   f.toolbar);
  setFontSlider('jdigi-font-entry',     'jdigi-font-entry-val',     f.entry);
}

function saveJDigiSettings() {
  const intOf = id => parseInt(document.getElementById(id).value, 10) || 0;
  const payload = {
    fontSize: intOf('jdigi-font-size') || 13,
    fonts: {
      rxTx:      intOf('jdigi-font-rxtx'),
      freq:      intOf('jdigi-font-freq'),
      statusBar: intOf('jdigi-font-statusbar'),
      toolbar:   intOf('jdigi-font-toolbar'),
      entry:     intOf('jdigi-font-entry'),
    }
  };
  fetch('/api/jdigi', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(payload)
  })
  .then(r => r.json())
  .then(() => flashMsg('jdigi-msg', 'J-Digi settings saved'))
  .catch(() => flashMsg('jdigi-msg', 'Error', true));
}

// ── J-Bridge settings ─────────────────────────────────────
function loadJBridgeSettings() {
  fetch('/api/jbridge')
    .then(r => r.json())
    .then(s => populateJBridgeForm(s))
    .catch(() => {});
}

function populateJBridgeForm(s) {
  setVal('jbridge-wsjtx-path', s.wsjtxPath || '');

  const fsEl  = document.getElementById('jbridge-font-size');
  const fsLbl = document.getElementById('jbridge-font-size-val');
  if (fsEl)  fsEl.value = s.fontSize || 12;
  if (fsLbl) fsLbl.textContent = s.fontSize || 12;

  const setFontSlider = (id, valId, v) => {
    const el  = document.getElementById(id);
    const lbl = document.getElementById(valId);
    const n   = (typeof v === 'number' ? v : 0);
    if (el)  el.value = n;
    if (lbl) lbl.textContent = (n === 0 ? 'auto' : n);
  };
  const f = s.fonts || {};
  setFontSlider('jbridge-font-toolbar', 'jbridge-font-toolbar-val', f.toolbar);
  setFontSlider('jbridge-font-sidebar', 'jbridge-font-sidebar-val', f.sidebar);
  setFontSlider('jbridge-font-band',    'jbridge-font-band-val',    f.band);
  setFontSlider('jbridge-font-table',   'jbridge-font-table-val',   f.table);
}

function saveJBridgeSettings() {
  const intOf = id => parseInt(document.getElementById(id).value, 10) || 0;
  const payload = {
    wsjtxPath: document.getElementById('jbridge-wsjtx-path').value.trim(),
    fontSize:  intOf('jbridge-font-size') || 12,
    fonts: {
      toolbar: intOf('jbridge-font-toolbar'),
      sidebar: intOf('jbridge-font-sidebar'),
      band:    intOf('jbridge-font-band'),
      table:   intOf('jbridge-font-table'),
    }
  };
  fetch('/api/jbridge', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify(payload)
  })
  .then(r => r.json())
  .then(() => flashMsg('jbridge-msg', 'J-Bridge settings saved — restart J-Bridge to apply'))
  .catch(() => flashMsg('jbridge-msg', 'Error', true));
}

// ── Database Tools ─────────────────────────────────────────
function loadDbList() {
  fetch('/api/db/list')
    .then(r => r.json())
    .then(data => {
      const sel = document.getElementById('db-list-sel');
      if (!sel) return;
      const active = data.active || 'j-log.db';
      sel.innerHTML = (data.databases || []).map(d =>
        `<option value="${esc(d)}">${esc(d)}</option>`
      ).join('');
      sel.value = active;
      setText('db-active-name', active);
    })
    .catch(() => {});
}

function doAddDatabase() {
  const name = prompt('Enter a name for the new log database:');
  if (!name || !name.trim()) return;
  fetch('/api/db/create', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ name: name.trim() })
  })
  .then(r => r.json())
  .then(d => {
    if (d.error) { flashMsg('db-status-msg', d.error, true); return; }
    flashMsg('db-status-msg', 'Created: ' + (d.name || ''));
    loadDbList();
  })
  .catch(() => flashMsg('db-status-msg', 'Error creating database', true));
}

function doSelectDatabase() {
  const sel = document.getElementById('db-list-sel');
  const name = sel && sel.value;
  if (!name) { flashMsg('db-status-msg', 'No database selected', true); return; }
  fetch('/api/db/select', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ name })
  })
  .then(r => r.json())
  .then(d => {
    if (d.error) { flashMsg('db-status-msg', d.error, true); return; }
    flashMsg('db-status-msg', 'Active set to: ' + name + ' (restart J-Log to apply)');
    setText('db-active-name', name);
  })
  .catch(() => flashMsg('db-status-msg', 'Error', true));
}

function doDeleteDatabase() {
  const sel = document.getElementById('db-list-sel');
  const name = sel && sel.value;
  if (!name) { flashMsg('db-status-msg', 'No database selected', true); return; }
  if (!confirm('Delete "' + name + '"? This cannot be undone.')) return;
  fetch('/api/db/delete', {
    method: 'DELETE',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ name })
  })
  .then(r => r.json())
  .then(d => {
    if (d.error) { flashMsg('db-status-msg', d.error, true); return; }
    flashMsg('db-status-msg', 'Deleted: ' + name);
    loadDbList();
  })
  .catch(() => flashMsg('db-status-msg', 'Error deleting database', true));
}

function exportAdif() {
  flashMsg('db-export-msg', 'Exporting…');
  fetch('/api/db/export/adif', { method: 'POST' })
  .then(r => {
    if (!r.ok) return r.json().then(d => { flashMsg('db-export-msg', d.error || 'Export failed', true); });
    return r.blob().then(blob => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'log-export.adi';
      a.click();
      flashMsg('db-export-msg', 'ADIF exported');
    });
  })
  .catch(() => flashMsg('db-export-msg', 'Export error', true));
}

function exportCsv() {
  flashMsg('db-export-msg', 'Exporting…');
  fetch('/api/db/export/csv', { method: 'POST' })
  .then(r => {
    if (!r.ok) return r.json().then(d => { flashMsg('db-export-msg', d.error || 'Export failed', true); });
    return r.blob().then(blob => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'log-export.csv';
      a.click();
      flashMsg('db-export-msg', 'CSV exported');
    });
  })
  .catch(() => flashMsg('db-export-msg', 'Export error', true));
}

function importAdifFile(input) {
  const f = input && input.files && input.files[0];
  if (!f) return;
  const form = new FormData();
  form.append('adif', f);
  flashMsg('db-export-msg', 'Uploading ' + f.name + '…');
  fetch('/api/db/import/adif', { method: 'POST', body: form })
    .then(r => r.json().then(j => ({ ok: r.ok, j })))
    .then(({ ok, j }) => {
      if (!ok) {
        flashMsg('db-export-msg', j.error || 'Import failed', true);
      } else {
        flashMsg('db-export-msg', 'Import queued — J-Log is processing ' + f.name);
      }
    })
    .catch(() => flashMsg('db-export-msg', 'Import error', true))
    .finally(() => { input.value = ''; });
}

function backupActiveDb() {
  flashMsg('db-backup-msg', 'Backing up…');
  fetch('/api/db/backup', { method: 'POST' })
    .then(r => r.json().then(j => ({ ok: r.ok, j })))
    .then(({ ok, j }) => {
      if (!ok) flashMsg('db-backup-msg', j.error || 'Backup failed', true);
      else     flashMsg('db-backup-msg', 'Backup created: ' + j.backup);
    })
    .catch(() => flashMsg('db-backup-msg', 'Backup error', true));
}

// ── Weather tab ────────────────────────────────────────────
function fetchWeather() {
  fetch('/api/weather')
    .then(r => r.json())
    .then(d => updateWeatherUI(d))
    .catch(() => {});
}

function updateWeatherUI(d) {
  const sw = d.spaceWeather || {};
  const lw = d.localWeather;

  // Fetched timestamp
  if (d.fetchedAt) {
    const t = new Date(d.fetchedAt);
    setText('sw-fetched', 'Updated ' + t.toLocaleTimeString());
  }

  // Kp
  const kp = sw.kp != null ? sw.kp.toFixed(2) : '—';
  setText('sw-kp', kp);
  const cond = sw.kpCondition || '—';
  const kpEl = document.getElementById('sw-kp');
  if (kpEl && sw.kp != null) {
    const c = sw.kp >= 7 ? 'var(--red)' : sw.kp >= 5 ? 'var(--peach)' : sw.kp >= 3 ? 'var(--yellow)' : 'var(--green)';
    kpEl.style.color = c;
  }
  setText('sw-kp-cond', cond);

  // X-ray
  setText('sw-xray', sw.xrayClass || '—');
  const xrEl = document.getElementById('sw-xray');
  if (xrEl && sw.xrayClass) {
    const cls = sw.xrayClass[0];
    const c = cls === 'X' ? 'var(--red)' : cls === 'M' ? 'var(--peach)' : cls === 'C' ? 'var(--yellow)' : 'var(--green)';
    xrEl.style.color = c;
  }

  // IMF Bz / Bt
  const bz = sw.imfBz != null ? sw.imfBz.toFixed(1) + ' nT' : '— nT';
  setText('sw-bz', bz);
  const bzEl = document.getElementById('sw-bz');
  if (bzEl && sw.imfBz != null) {
    bzEl.style.color = sw.imfBz < -10 ? 'var(--red)' : sw.imfBz < 0 ? 'var(--peach)' : 'var(--green)';
  }
  setText('sw-bt', sw.imfBt != null ? 'Bt ' + sw.imfBt.toFixed(1) + ' nT' : 'Bt — nT');

  // Solar wind
  setText('sw-wind', sw.solarWindSpeed != null ? Math.round(sw.solarWindSpeed) + ' km/s' : '— km/s');
  setText('sw-dens', sw.solarWindDensity != null ? 'Density ' + sw.solarWindDensity.toFixed(1) + ' p/cc' : 'Density — p/cc');

  // Proton
  setText('sw-proton', sw.protonFlux != null ? sw.protonFlux.toFixed(2) : '—');

  // HF conditions note
  let hfNote = '';
  if (sw.xrayClass) {
    const cls = sw.xrayClass[0];
    if (cls === 'X') hfNote += 'Strong HF blackout possible. ';
    else if (cls === 'M') hfNote += 'HF degradation likely. ';
    else if (cls === 'C') hfNote += 'Minor HF fadeout possible. ';
    else hfNote += 'Solar flux OK. ';
  }
  if (sw.kp != null) {
    if (sw.kp >= 7) hfNote += 'Severe aurora/polar blackout.';
    else if (sw.kp >= 5) hfNote += 'Geomagnetic storm — polar paths degraded.';
    else if (sw.kp >= 3) hfNote += 'Slightly disturbed conditions.';
    else hfNote += 'Quiet geomagnetic conditions.';
  }
  setText('sw-hf-note', hfNote || '—');

  // Local weather
  const noKeyEl   = document.getElementById('local-wx-no-key');
  const dataEl    = document.getElementById('local-wx-data');
  const errEl     = document.getElementById('local-wx-err');

  if (lw === null || lw === undefined) {
    if (noKeyEl) noKeyEl.style.display = '';
    if (dataEl)  dataEl.style.display  = 'none';
    if (errEl)   errEl.style.display   = 'none';
  } else if (lw.cod != null && lw.cod !== 200 && lw.cod !== '200') {
    if (noKeyEl) noKeyEl.style.display = 'none';
    if (dataEl)  dataEl.style.display  = 'none';
    if (errEl) { errEl.style.display = ''; errEl.textContent = 'OpenWeather error: ' + (lw.message || lw.cod); }
  } else {
    if (noKeyEl) noKeyEl.style.display = 'none';
    if (errEl)   errEl.style.display   = 'none';
    if (dataEl)  dataEl.style.display  = '';

    const main  = lw.main  || {};
    const wind  = lw.wind  || {};
    const wDesc = (lw.weather && lw.weather[0]) ? lw.weather[0] : {};
    const deg   = (wind.deg != null) ? compassDir(wind.deg) : '';

    setText('wx-temp',     main.temp   != null ? Math.round(main.temp) + '°F' : '—');
    setText('wx-feels',    main.feels_like != null ? 'Feels like ' + Math.round(main.feels_like) + '°F' : '—');
    setText('wx-desc',     (wDesc.main || '—'));
    setText('wx-clouds',   wDesc.description ? cap(wDesc.description) : '—');
    setText('wx-wind',     wind.speed  != null ? Math.round(wind.speed) + ' mph' : '—');
    setText('wx-wind-dir', deg ? deg + (wind.gust ? ' · Gusts ' + Math.round(wind.gust) + ' mph' : '') : '—');
    setText('wx-humidity', main.humidity != null ? main.humidity + '%' : '—');
    setText('wx-pressure', main.pressure != null ? main.pressure + ' hPa' : '—');
    const vis = lw.visibility != null ? (lw.visibility / 1609.34).toFixed(1) + ' mi' : '—';
    setText('wx-vis',      vis);
    setText('wx-location', lw.name || '—');
  }
}

// ── Callsign tab ─────────────────────────────────────────────

function populateCallsignTab(cfg) {
  const cs = cfg.callsignLookup || {};
  setChk('cs-enabled',     cs.enabled !== false);
  setSelectVal('cs-provider',  cs.provider      || 'auto');
  setVal('cs-db-path-cfg', cs.localDbPath    || '');
  setVal('cs-cache-ttl',   cs.cacheTtlHours  != null ? cs.cacheTtlHours : 24);
  setVal('cs-qrz-user',    cs.qrzUsername    || '');
  setVal('cs-qrz-pass',    cs.qrzPassword    || '');
  setVal('cs-hamqth-user', cs.hamqthUsername || '');
  setVal('cs-hamqth-pass', cs.hamqthPassword || '');
  setVal('cs-fcc-url-cfg', cs.fccUlsUrl      || '');
  loadCallsignDbStatus();
}

function saveCallsignSettings() {
  const cfg = state.config || {};
  cfg.callsignLookup = {
    enabled:        document.getElementById('cs-enabled').checked,
    provider:       document.getElementById('cs-provider').value,
    localDbPath:    document.getElementById('cs-db-path-cfg').value.trim(),
    cacheTtlHours:  parseInt(document.getElementById('cs-cache-ttl').value) || 24,
    qrzUsername:    document.getElementById('cs-qrz-user').value.trim(),
    qrzPassword:    document.getElementById('cs-qrz-pass').value,
    hamqthUsername: document.getElementById('cs-hamqth-user').value.trim(),
    hamqthPassword: document.getElementById('cs-hamqth-pass').value,
    fccUlsUrl:      document.getElementById('cs-fcc-url-cfg').value.trim(),
  };
  fetch('/api/config', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cfg)
  })
    .then(r => r.json())
    .then(() => loadCallsignDbStatus())
    .catch(e => alert('Save failed: ' + e));
}

function loadCallsignDbStatus() {
  fetch('/api/callsign/db/status')
    .then(r => r.json())
    .then(d => {
      setText('cs-db-path',     d.dbPath || '(not configured)');
      setText('cs-db-records',  d.exists ? (d.records >= 0 ? Number(d.records).toLocaleString() : '—') : 'DB not found');
      setText('cs-db-size',     d.sizeBytes != null ? (d.sizeBytes / 1048576).toFixed(1) + ' MB' : '—');
      setText('cs-db-modified', d.lastModified ? new Date(d.lastModified).toLocaleString() : '—');
    })
    .catch(() => {});
}

function doCallsignLookup() {
  const call = (document.getElementById('cs-search').value || '').toUpperCase().trim();
  if (!call) return;
  document.getElementById('cs-result').style.display    = 'none';
  document.getElementById('cs-not-found').style.display = 'none';
  fetch('/api/callsign/' + encodeURIComponent(call))
    .then(r => r.json())
    .then(d => {
      if (!d.found) {
        const el = document.getElementById('cs-not-found');
        el.textContent = d.reason || 'Not found';
        el.style.display = '';
        return;
      }
      document.getElementById('cs-result').style.display = '';
      setText('cs-r-call',   d.callsign       || '—');
      setText('cs-r-name',   d.name           || '—');
      const addr = [d.addr1, d.city, d.state, d.zip].filter(Boolean).join(', ');
      setText('cs-r-addr',   addr             || '—');
      setText('cs-r-grid',   d.grid           || '—');
      setText('cs-r-class',  d.licenseClass   || '—');
      setText('cs-r-exp',    d.licenseExpires || '—');
      setText('cs-r-source', d.source         || '—');
    })
    .catch(e => {
      const el = document.getElementById('cs-not-found');
      el.textContent = 'Error: ' + e;
      el.style.display = '';
    });
}

let fccPollTimer = null;

function doFccDownload() {
  const btn = document.getElementById('cs-fcc-dl-btn');
  btn.disabled = true;
  btn.textContent = 'Starting…';
  document.getElementById('cs-fcc-dl-progress').style.display = '';
  fetch('/api/callsign/db/download/fcc', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: '{}'
  })
    .then(r => r.json())
    .then(d => {
      startFccPoll();
      if (!d.started) setText('cs-dl-msg', 'Pipeline already running — monitoring progress');
    })
    .catch(e => {
      btn.disabled = false;
      btn.textContent = 'Download from FCC';
      setText('cs-dl-msg', 'Error: ' + e);
    });
}

function startFccPoll() {
  if (fccPollTimer) return;
  pollFccProgress();
  fccPollTimer = setInterval(pollFccProgress, 1500);
}

function pollFccProgress() {
  fetch('/api/callsign/db/import/progress')
    .then(r => r.json())
    .then(d => {
      const phase = d.dlPhase || 'IDLE';
      setText('cs-dl-phase', phase);
      setText('cs-dl-msg',   d.dlMsg || '');

      const barWrap = document.getElementById('cs-dl-bar-wrap');
      const bar     = document.getElementById('cs-dl-bar');
      if (phase === 'DOWNLOADING' && d.dlBytesTotal > 0) {
        barWrap.style.display = '';
        bar.style.width = Math.min(100, (d.dlBytesReceived / d.dlBytesTotal) * 100).toFixed(1) + '%';
      } else {
        barWrap.style.display = 'none';
      }

      if (phase === 'DONE' || phase === 'FAILED') {
        clearInterval(fccPollTimer);
        fccPollTimer = null;
        const btn = document.getElementById('cs-fcc-dl-btn');
        btn.disabled = false;
        btn.textContent = 'Download from FCC';
        if (phase === 'DONE') loadCallsignDbStatus();
      }
    })
    .catch(() => {});
}

function doFccManualImport() {
  const ulsDir = document.getElementById('cs-uls-dir').value.trim();
  if (!ulsDir) { alert('Enter the path to the extracted ULS directory'); return; }
  const msgEl = document.getElementById('cs-fcc-import-msg');
  msgEl.textContent = 'Starting…';
  msgEl.style.color = '';
  fetch('/api/callsign/db/import/fcc', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ulsDir })
  })
    .then(r => r.json())
    .then(d => {
      if (d.started) {
        msgEl.textContent = 'Import started';
        msgEl.style.color = 'var(--green)';
      } else {
        msgEl.textContent = d.error || 'An import is already running';
        msgEl.style.color = 'var(--red)';
      }
    })
    .catch(e => { msgEl.textContent = 'Error: ' + e; msgEl.style.color = 'var(--red)'; });
}

function doCsvImport() {
  const csvPath = document.getElementById('cs-csv-path').value.trim();
  if (!csvPath) { alert('Enter the path to the CSV file'); return; }
  const msgEl = document.getElementById('cs-csv-import-msg');
  msgEl.textContent = 'Starting…';
  msgEl.style.color = '';
  fetch('/api/callsign/db/import/csv', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ csvPath })
  })
    .then(r => r.json())
    .then(d => {
      if (d.started) {
        msgEl.textContent = 'CSV import started';
        msgEl.style.color = 'var(--green)';
      } else {
        msgEl.textContent = d.error || 'An import is already running';
        msgEl.style.color = 'var(--red)';
      }
    })
    .catch(e => { msgEl.textContent = 'Error: ' + e; msgEl.style.color = 'var(--red)'; });
}

function compassDir(deg) {
  const dirs = ['N','NE','E','SE','S','SW','W','NW'];
  return dirs[Math.round(deg / 45) % 8];
}

function cap(s) { return s ? s.charAt(0).toUpperCase() + s.slice(1) : s; }

// ── Boot ────────────────────────────────────────────────────
// Restore theme before first paint to avoid flash
(function () {
  const saved = localStorage.getItem('jhub-theme') || 'dark';
  applyTheme(saved);
  state.appearance.theme = saved;
})();

populateTimezones();
loadConfig();
loadMacros();
loadAmp();
loadAntenna();
loadRbn();
loadBackup();
loadUploaders();
loadLearn();
setInterval(loadRbn, 10000);
setInterval(loadBackup, 30000);
loadJMapSettings();
loadJLogSettings();
loadJDigiSettings();
loadJBridgeSettings();
loadJSatSatelliteRegistry();
loadDbList();
connectWs();
pollStatus();
pollSpots();
pollAntennaStatus();
updateRigConnStatus();
loadJSatTleStatus();
setInterval(updateRigConnStatus, 3000);
setInterval(pollAntennaStatus, 3000);

// Deep-link: open browser at #macros / #amp / #antenna and the matching tab is selected
(function applyHashTab() {
  const h = (location.hash || '').replace('#', '').trim();
  if (!h) return;
  const btn = document.querySelector(`.nav-btn[data-tab="${h}"]`);
  if (btn) btn.click();
})();
window.addEventListener('hashchange', () => {
  const h = (location.hash || '').replace('#','').trim();
  const btn = h && document.querySelector(`.nav-btn[data-tab="${h}"]`);
  if (btn) btn.click();
});

fetchWeather();
setInterval(fetchWeather, 300000);   // refresh every 5 minutes

setInterval(tickClock, 1000);
setInterval(pollStatus, 2000);
setInterval(pollSpots, 10000);
setInterval(loadJSatTleStatus, 60000);
pollSessions();
setInterval(pollSessions, 5000);
pollDeps();

// ── Connected-apps dashboard card ─────────────────────────
function pollSessions() {
  fetch('/api/sessions')
    .then(r => r.json())
    .then(renderSessionList)
    .catch(() => {});
}

function renderSessionList(sessions) {
  const box   = document.getElementById('d-apps-list');
  const count = document.getElementById('d-apps-count');
  if (!box) return;
  count.textContent = sessions.length ? '(' + sessions.length + ')' : '';
  if (!sessions.length) {
    box.innerHTML = '<span style="color:var(--overlay0)">No apps connected</span>';
    return;
  }
  const fmtAge = sec => {
    if (sec == null || sec < 0) return '—';
    if (sec < 60)   return sec + 's';
    if (sec < 3600) return Math.floor(sec/60) + 'm';
    const h = Math.floor(sec/3600), m = Math.floor((sec % 3600)/60);
    return m === 0 ? h + 'h' : h + 'h ' + m + 'm';
  };
  const STALE_MSG = 60, STALE_HB = 45;
  box.innerHTML = sessions.map(s => {
    const msgStale = s.lastMessageAgeSeconds   > STALE_MSG;
    const hbStale  = s.lastHeartbeatAgeSeconds > STALE_HB;
    const dotColor = (msgStale && hbStale) ? '#f38ba8' : '#a6e3a1';
    const name = s.appName + (s.version ? ' v' + s.version : '');
    return '<div>' +
           '<span style="color:' + dotColor + '">●</span> <strong>' + name + '</strong>' +
           ' <span style="color:var(--overlay0)">' +
           'up ' + fmtAge(s.ageSeconds) +
           ' · msg ' + fmtAge(s.lastMessageAgeSeconds) +
           ' · hb ' + fmtAge(s.lastHeartbeatAgeSeconds) +
           '</span></div>';
  }).join('');
}

// ── System dependencies (Hamlib, WSJT-X) ───────────────────
function pollDeps() {
  const box = document.getElementById('d-deps-list');
  if (box) box.innerHTML = '<span style="color:var(--overlay0)">Checking…</span>';
  fetch('/api/deps')
    .then(r => r.json())
    .then(renderDeps)
    .catch(() => { if (box) box.innerHTML = '<span style="color:#f38ba8">Check failed</span>'; });
}

function renderDeps(d) {
  const box = document.getElementById('d-deps-list');
  if (!box) return;
  const rows = [];

  // Hamlib
  const h = d.hamlib || {};
  if (h.installed) {
    const details = [];
    if (h.rigctlVersion) details.push('rigctl: ' + h.rigctlVersion);
    if (h.rotctlVersion) details.push('rotctl: ' + h.rotctlVersion);
    rows.push('<div><span style="color:#a6e3a1">●</span> <strong>Hamlib</strong> ' +
              '<span style="color:var(--overlay0)">' +
              (details.join(' · ') || (h.rigctlPath || h.rotctlPath)) +
              '</span></div>');
  } else {
    rows.push('<div><span style="color:#f38ba8">●</span> <strong>Hamlib</strong> not found ' +
              '<span style="color:var(--overlay0)">— ' + (h.installHint || '') + '</span></div>');
  }

  // WSJT-X
  const w = d.wsjtx || {};
  if (w.installed) {
    rows.push('<div><span style="color:#a6e3a1">●</span> <strong>WSJT-X</strong> ' +
              '<span style="color:var(--overlay0)">' + (w.version || w.path) + '</span></div>');
  } else {
    rows.push('<div><span style="color:#f38ba8">●</span> <strong>WSJT-X</strong> not found ' +
              '<span style="color:var(--overlay0)">— ' + (w.installHint || '') + '</span></div>');
  }

  box.innerHTML = rows.join('');
}

// ───── Inventory tab ────────────────────────────────────────
state.inventory = { items: [], types: [], contacts: [] };

function loadInventoryAll() {
  Promise.all([
    fetch('/api/inventory/types').then(r => r.json()),
    fetch('/api/inventory/items').then(r => r.json()),
    fetch('/api/inventory/contacts').then(r => r.json()),
  ]).then(([types, items, contacts]) => {
    state.inventory.types = types || [];
    state.inventory.items = items || [];
    state.inventory.contacts = contacts || [];
    populateTypeSelectors();
    renderInventoryTable();
    renderContactsTable();
  }).catch(err => console.error('inventory load failed', err));
}

function populateTypeSelectors() {
  const typeFilter = document.getElementById('inv-type-filter');
  const typeForm   = document.getElementById('inv-item-type');
  if (!typeFilter || !typeForm) return;
  // Filter dropdown (preserves the "All Types" first option).
  const cur = typeFilter.value;
  typeFilter.innerHTML = '<option value="">All Types</option>'
    + state.inventory.types.map(t => `<option value="${t.id}">${escHtml(t.name)}</option>`).join('');
  typeFilter.value = cur;
  // Form dropdown.
  typeForm.innerHTML = state.inventory.types.map(t =>
    `<option value="${t.id}">${escHtml(t.name)}</option>`).join('');
}

function renderInventoryTable() {
  const tbody = document.getElementById('inv-tbody');
  if (!tbody) return;
  const search    = (document.getElementById('inv-search')?.value || '').toLowerCase();
  const typeFilter = document.getElementById('inv-type-filter')?.value || '';
  const dispFilter = document.getElementById('inv-disposition-filter')?.value || '';
  const instFilter = document.getElementById('inv-install-filter')?.value || '';

  const items = state.inventory.items.filter(it => {
    if (typeFilter && String(it.type_id) !== typeFilter) return false;
    if (dispFilter && it.disposition !== dispFilter) return false;
    if (instFilter && it.install_status !== instFilter) return false;
    if (search) {
      const hay = [it.manufacturer, it.model, it.serial_number, it.notes,
                   it.storage_location, it.type_name].filter(Boolean).join(' ').toLowerCase();
      if (!hay.includes(search)) return false;
    }
    return true;
  });

  // Stats
  const stats = document.getElementById('inv-stats');
  if (stats) {
    const totalValue = items.reduce((sum, it) =>
      sum + (Number(it.estimated_value) || 0), 0);
    const totalCost  = items.reduce((sum, it) =>
      sum + (Number(it.purchase_price)  || 0), 0);
    stats.textContent = `${items.length} item${items.length === 1 ? '' : 's'} `
      + ` ·  Estimated value: $${totalValue.toFixed(2)}`
      + ` ·  Original cost: $${totalCost.toFixed(2)}`;
  }

  if (items.length === 0) {
    tbody.innerHTML = '<tr><td colspan="10" style="text-align:center;color:var(--subtext0);padding:20px">'
      + (state.inventory.items.length === 0
         ? 'No items yet. Click <b>+ Add Item</b> to start your inventory.'
         : 'No items match the current filters.')
      + '</td></tr>';
    return;
  }

  tbody.innerHTML = items.map(it => {
    const valueText = it.estimated_value != null
      ? '$' + Number(it.estimated_value).toFixed(2) : '—';
    const dispLabel = ({
      working:        'Working',
      repairable:     'Repairable',
      not_repairable: 'Not Repairable',
    })[it.disposition] || it.disposition || '';
    const installLabel = it.install_status === 'storage' ? 'Storage' : 'Installed';
    return `<tr>
      <td>${escHtml(it.type_name || '')}</td>
      <td>${escHtml(it.manufacturer || '')}</td>
      <td>${escHtml(it.model || '')}</td>
      <td><code style="font-size:11px">${escHtml(it.serial_number || '')}</code></td>
      <td>${escHtml(it.date_acquired || '')}</td>
      <td style="text-align:right">${valueText}</td>
      <td><span class="inv-disp-${it.disposition || ''}">${escHtml(dispLabel)}</span></td>
      <td>${escHtml(installLabel)}</td>
      <td>${escHtml(it.storage_location || '')}</td>
      <td class="inv-actions">
        <button onclick="openItemModal(${it.id})">Edit</button>
        <button class="del" onclick="deleteItem(${it.id})">Del</button>
      </td>
    </tr>`;
  }).join('');
}

function renderContactsTable() {
  const tbody = document.getElementById('inv-contacts-tbody');
  if (!tbody) return;
  if (state.inventory.contacts.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--subtext0);padding:20px">'
      + 'No contacts yet. Click <b>+ Add Contact</b> to add a friend, club leader, or dealer.'
      + '</td></tr>';
    return;
  }
  tbody.innerHTML = state.inventory.contacts.map(c => `<tr>
    <td>${c.priority || ''}</td>
    <td>${escHtml(c.name || '')}</td>
    <td>${escHtml(c.callsign || '')}</td>
    <td>${escHtml(c.phone || '')}</td>
    <td>${escHtml(c.email || '')}</td>
    <td>${escHtml(c.relationship || '')}</td>
    <td style="max-width:300px;white-space:normal">${escHtml(c.items_wanted || '')}</td>
    <td class="inv-actions">
      <button onclick="openContactModal(${c.id})">Edit</button>
      <button class="del" onclick="deleteContact(${c.id})">Del</button>
    </td>
  </tr>`).join('');
}

// ----- Item modal -----
function openItemModal(id) {
  const modal = document.getElementById('inv-item-modal');
  const title = document.getElementById('inv-item-modal-title');
  document.getElementById('inv-item-form').reset();
  document.getElementById('inv-item-id').value = '';
  if (id) {
    const it = state.inventory.items.find(x => x.id === id);
    if (!it) return;
    title.textContent = 'Edit Item — ' + (it.manufacturer || '') + ' ' + (it.model || '');
    document.getElementById('inv-item-id').value          = it.id;
    document.getElementById('inv-item-type').value        = it.type_id;
    document.getElementById('inv-item-manufacturer').value= it.manufacturer || '';
    document.getElementById('inv-item-model').value       = it.model || '';
    document.getElementById('inv-item-serial').value      = it.serial_number || '';
    document.getElementById('inv-item-date').value        = it.date_acquired || '';
    document.getElementById('inv-item-price').value       = it.purchase_price != null ? it.purchase_price : '';
    document.getElementById('inv-item-value').value       = it.estimated_value != null ? it.estimated_value : '';
    document.getElementById('inv-item-disposition').value = it.disposition || 'working';
    document.getElementById('inv-item-install').value     = it.install_status || 'installed';
    document.getElementById('inv-item-storage').value     = it.storage_location || '';
    document.getElementById('inv-item-notes').value       = it.notes || '';
  } else {
    title.textContent = 'Add Item';
    // Default to "today" for the date field if blank.
    const d = new Date();
    document.getElementById('inv-item-date').value =
      d.toISOString().slice(0, 10);
  }
  toggleStorageLocation();
  modal.style.display = 'flex';
}

function closeItemModal() {
  document.getElementById('inv-item-modal').style.display = 'none';
}

function toggleStorageLocation() {
  const inst = document.getElementById('inv-item-install')?.value;
  const row = document.getElementById('inv-item-storage-row');
  if (row) row.style.display = inst === 'storage' ? '' : 'none';
}

function saveItem(ev) {
  ev.preventDefault();
  const id = document.getElementById('inv-item-id').value;
  const body = {
    type_id:          parseInt(document.getElementById('inv-item-type').value, 10),
    manufacturer:     document.getElementById('inv-item-manufacturer').value,
    model:            document.getElementById('inv-item-model').value,
    serial_number:    document.getElementById('inv-item-serial').value,
    date_acquired:    document.getElementById('inv-item-date').value,
    purchase_price:   document.getElementById('inv-item-price').value,
    estimated_value:  document.getElementById('inv-item-value').value,
    disposition:      document.getElementById('inv-item-disposition').value,
    install_status:   document.getElementById('inv-item-install').value,
    storage_location: document.getElementById('inv-item-storage').value,
    notes:            document.getElementById('inv-item-notes').value,
  };
  const url    = id ? '/api/inventory/items/' + id : '/api/inventory/items';
  const method = id ? 'PUT' : 'POST';
  fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then(r => r.json())
    .then(data => {
      if (data.error) { alert('Save failed: ' + data.error); return; }
      closeItemModal();
      loadInventoryAll();
    })
    .catch(err => alert('Save failed: ' + err));
}

function deleteItem(id) {
  const it = state.inventory.items.find(x => x.id === id);
  if (!it) return;
  const label = (it.manufacturer || '') + ' ' + (it.model || '');
  if (!confirm('Delete "' + label.trim() + '"? This cannot be undone.')) return;
  fetch('/api/inventory/items/' + id, { method: 'DELETE' })
    .then(r => r.json())
    .then(() => loadInventoryAll())
    .catch(err => alert('Delete failed: ' + err));
}

function exportInventoryCsv() {
  window.location.href = '/api/inventory/export.csv';
}

// ----- Contact modal -----
function openContactModal(id) {
  const modal = document.getElementById('inv-contact-modal');
  const title = document.getElementById('inv-contact-modal-title');
  document.getElementById('inv-contact-form').reset();
  document.getElementById('inv-contact-id').value = '';
  if (id) {
    const c = state.inventory.contacts.find(x => x.id === id);
    if (!c) return;
    title.textContent = 'Edit Contact — ' + (c.name || '');
    document.getElementById('inv-contact-id').value           = c.id;
    document.getElementById('inv-contact-name').value         = c.name || '';
    document.getElementById('inv-contact-callsign').value     = c.callsign || '';
    document.getElementById('inv-contact-phone').value        = c.phone || '';
    document.getElementById('inv-contact-email').value        = c.email || '';
    document.getElementById('inv-contact-relationship').value = c.relationship || '';
    document.getElementById('inv-contact-priority').value     = c.priority != null ? c.priority : 100;
    document.getElementById('inv-contact-items').value        = c.items_wanted || '';
    document.getElementById('inv-contact-notes').value        = c.notes || '';
  } else {
    title.textContent = 'Add Contact';
    document.getElementById('inv-contact-priority').value = 100;
  }
  modal.style.display = 'flex';
}

function closeContactModal() {
  document.getElementById('inv-contact-modal').style.display = 'none';
}

function saveContact(ev) {
  ev.preventDefault();
  const id = document.getElementById('inv-contact-id').value;
  const body = {
    name:         document.getElementById('inv-contact-name').value,
    callsign:     document.getElementById('inv-contact-callsign').value,
    phone:        document.getElementById('inv-contact-phone').value,
    email:        document.getElementById('inv-contact-email').value,
    relationship: document.getElementById('inv-contact-relationship').value,
    priority:     parseInt(document.getElementById('inv-contact-priority').value, 10) || 100,
    items_wanted: document.getElementById('inv-contact-items').value,
    notes:        document.getElementById('inv-contact-notes').value,
  };
  const url    = id ? '/api/inventory/contacts/' + id : '/api/inventory/contacts';
  const method = id ? 'PUT' : 'POST';
  fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then(r => r.json())
    .then(data => {
      if (data.error) { alert('Save failed: ' + data.error); return; }
      closeContactModal();
      loadInventoryAll();
    })
    .catch(err => alert('Save failed: ' + err));
}

function deleteContact(id) {
  const c = state.inventory.contacts.find(x => x.id === id);
  if (!c) return;
  if (!confirm('Delete contact "' + (c.name || '') + '"?')) return;
  fetch('/api/inventory/contacts/' + id, { method: 'DELETE' })
    .then(r => r.json())
    .then(() => loadInventoryAll())
    .catch(err => alert('Delete failed: ' + err));
}

function escHtml(s) {
  if (s == null) return '';
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

loadInventoryAll();
