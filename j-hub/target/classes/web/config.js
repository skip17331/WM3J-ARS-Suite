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
  rig:         null,   // last RIG_STATUS
  rotor:       null,   // last ROTOR_STATUS { bearing, elevation }
  clusterConn: false,
  spm:         0,
  wsState:     'CLOSED',
  appearance:  { theme: 'dark', fontSize: 13, waterfallColor: 'viridis', mapTheme: 'dark' },
};

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

  // Rig tab live readout
  setVal('rig-live-freq', hz > 0 ? (hz / 1e6).toFixed(3) : '');
  setVal('rig-live-mode', mode);
  setVal('rig-live-band', band);
  setVal('rig-live-pwr',  rig.power != null ? rig.power : '');

  // Intel pane
  setText('i-freq', mhz);
  setText('i-mode', mode);
  setText('i-band', band !== '' ? band : '—');
  setText('i-pwr',  pwr);
}

// ── Rotor UI ───────────────────────────────────────────────
function updateRotorUI(rot) {
  const hdg = rot.bearing != null ? Math.round(rot.bearing) : null;
  const txt = hdg != null ? hdg + '°' : '---°';

  setText('d-heading',     txt);
  setText('rot-heading-big', txt);
  setText('i-heading', txt);

  // Rotate compass needles
  ['compass-needle', 'rot-needle'].forEach(id => {
    const el = document.getElementById(id);
    if (el && hdg != null) el.setAttribute('transform', `rotate(${hdg}, 50, 50)`);
  });

  // Dashboard rotor backend
  const rb = (state.config.rotor && state.config.rotor.backend) || '—';
  setText('d-rotor-backend', rb);
  setText('i-rot-backend', rb);
}

// ── Dashboard update ───────────────────────────────────────
function updateDashboard(status) {
  const cc = status.clusterConnected;
  const cfg = state.config;

  // Cluster card
  setDot('d-clus-dot', cc ? 'green' : 'red');
  setText('d-clus-txt', cc ? 'Connected' : 'Disconnected');
  setText('d-spm', status.spotsPerMinute || 0);
  setText('d-total-spots', status.totalSpots || 0);
  if (cfg.cluster) setText('d-clus-srv', cfg.cluster.server || '—');

  // Cluster tab
  setDot('cl-dot', cc ? 'green' : 'red');
  setText('cl-status-txt', cc ? 'Connected' : 'Disconnected');
  setText('cl-spm', status.spotsPerMinute || 0);
  setText('cl-total', status.totalSpots || 0);
  if (cfg.cluster) setText('cl-srv-live', cfg.cluster.server || '—');

  // Module launch buttons from appsRunning
  const ar = status.appsRunning || {};
  updateModuleDot('jmap',    ar['jMap'],     'J-Map');
  updateModuleDot('jlog',    ar['j-log'],    'J-Log');
  updateModuleDot('jdigi',   ar['j-digi'],   'J-Digi');
  updateModuleDot('jbridge', ar['j-bridge'], 'J-Bridge');
}

function updateModuleDot(key, running, label) {
  const dot = document.getElementById('dot-' + key);
  if (dot) setDot('dot-' + key, running ? 'green' : 'gray');
  const meta = document.getElementById('meta-' + key);
  if (meta) meta.textContent = running ? 'Running' : 'Not running';
}

// ── Intel pane ─────────────────────────────────────────────
function applyStationIntel(st) {
  setText('i-callsign', st.callsign || '—');
  setText('i-grid', st.gridSquare  || '—');
  // Populate station fields in logging tab
  setVal('st-call', st.callsign  || '');
  setVal('st-grid', st.gridSquare || '');
  setVal('st-lat',  st.lat  != null ? st.lat  : '');
  setVal('st-lon',  st.lon  != null ? st.lon  : '');
  setVal('st-tz',   st.timezone  || '');
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
  const keys = { 'j-log': 'jlog', 'j-digi': 'jdigi', 'j-bridge': 'jbridge', 'jMap': 'jmap', 'webconfig': null };

  Object.entries(keys).forEach(([appName, key]) => {
    if (!key) return;
    const connected = apps.some(a => a.appName === appName);
    const connAt   = connected ? apps.find(a => a.appName === appName).connectedAt : null;

    setDot('i-dot-' + key, connected ? 'green' : 'gray');
    setText('i-' + key, connected ? 'Online' : 'Offline');

    setDot('dot-' + key, connected ? 'green' : 'gray');
    const meta = document.getElementById('meta-' + key);
    if (meta) meta.textContent = connected
      ? 'Connected ' + (connAt ? new Date(connAt).toLocaleTimeString() : '') : 'Not connected';
  });

  // Module tab session table
  renderSessionTable(apps);
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
  fetch('/api/status').then(r => r.json()).then(d => {
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
  setVal('st-call', st.callsign  || '');
  setVal('st-grid', st.gridSquare || '');
  setVal('st-lat',  st.lat  != null ? st.lat  : '');
  setVal('st-lon',  st.lon  != null ? st.lon  : '');
  setVal('st-tz',   st.timezone  || '');
  applyStationIntel(st);

  // J-Hub ports
  const jh = cfg.jHub || {};
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

  // Logging
  const lg = cfg.logger || {};
  setVal('log-db-path', (lg.normalLog && lg.normalLog.dbPath) || '');
  setVal('log-mode', lg.mode || 'normal');

  // Band/mode filters
  const filters = (cl.filters) || {};
  buildFilterChips('band-filters', ['160m','80m','60m','40m','30m','20m','17m','15m','12m','10m','6m','2m','70cm'], filters.bands || []);
  buildFilterChips('mode-filters', ['CW','SSB','FT8','FT4','RTTY','PSK31','JS8','OLIVIA','MFSK16'], filters.modes || []);

  // Appearance
  const ap = cfg.appearance || {};
  state.appearance = ap;
  applyAppearanceUI(ap);

  // Module cards
  buildModuleCards(cfg.apps || {});
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
  // keep the Appearance tab seg control in sync
  document.querySelectorAll('#theme-seg .seg-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.val === next);
  });
}

// ── Appearance controls ────────────────────────────────────
function setTheme(val, btn) {
  document.querySelectorAll('#theme-seg .seg-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  state.appearance.theme = val;
  applyTheme(val);
}

function setWfColor(val, el) {
  document.querySelectorAll('#wf-swatches .swatch').forEach(s => s.classList.remove('sel'));
  el.classList.add('sel');
  state.appearance.waterfallColor = val;
  setText('wf-color-label', 'Selected: ' + val);
}

function setMapTheme(val, el) {
  document.querySelectorAll('#map-swatches .swatch').forEach(s => s.classList.remove('sel'));
  el.classList.add('sel');
  state.appearance.mapTheme = val;
  setText('map-theme-label', 'Selected: ' + val);
}

function applyAppearanceUI(ap) {
  // Theme — server value overrides localStorage only if explicitly set
  const theme = ap.theme || localStorage.getItem('jhub-theme') || 'dark';
  applyTheme(theme);
  state.appearance.theme = theme;
  document.querySelectorAll('#theme-seg .seg-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.val === theme);
  });

  // Font size
  const fsRange = document.getElementById('font-size-range');
  if (fsRange) fsRange.value = ap.fontSize || 13;
  setText('font-size-val', ap.fontSize || 13);

  // Waterfall
  document.querySelectorAll('#wf-swatches .swatch').forEach(s => {
    s.classList.toggle('sel', s.dataset.val === ap.waterfallColor);
  });
  setText('wf-color-label', 'Selected: ' + (ap.waterfallColor || 'viridis'));

  // Map theme
  document.querySelectorAll('#map-swatches .swatch').forEach(s => {
    s.classList.toggle('sel', s.dataset.val === ap.mapTheme);
  });
  setText('map-theme-label', 'Selected: ' + (ap.mapTheme || 'dark'));
}

// ── Module cards (Modules tab) ─────────────────────────────
function buildModuleCards(appsSection) {
  const modules = [
    { key: 'jLog',    id: 'j-log',    label: 'J-Log',    desc: 'Ham radio logging application' },
    { key: 'jDigi',   id: 'j-digi',   label: 'J-Digi',   desc: 'Digital modem / RTTY / PSK' },
    { key: 'jBridge', id: 'j-bridge', label: 'J-Bridge', desc: 'WSJT-X / FT8 integration bridge' },
    { key: 'jMap',    id: 'jMap',     label: 'J-Map',    desc: 'Real-time grayline + DX map' },
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
function loadMacros() {
  fetch('/api/macros')
    .then(r => r.json())
    .then(list => renderMacroTable(list))
    .catch(() => {});
}

function renderMacroTable(list) {
  const tbody = document.getElementById('macro-tbody');
  if (!tbody) return;
  tbody.innerHTML = list.map((m, i) => `
    <tr data-idx="${i}">
      <td><input type="text" value="${esc(m.key||'')}" data-field="key" ${m.type==='FIXED'?'readonly style="color:var(--subtext0)"':''}></td>
      <td><input type="text" value="${esc(m.label||'')}" data-field="label"></td>
      <td><span class="${m.type==='FIXED'?'badge-fixed':'badge-prog'}">${m.type||'FIXED'}</span><input type="hidden" data-field="type" value="${m.type||'FIXED'}"></td>
      <td><input type="text" value="${esc(m.text||'')}" data-field="text"></td>
      <td>${m.type==='PROGRAMMABLE'?`<button class="btn btn-red btn-sm" onclick="removeMacroRow(this)">✕</button>`:'&nbsp;'}</td>
    </tr>`
  ).join('');
}

function collectMacros() {
  const rows = document.querySelectorAll('#macro-tbody tr');
  return Array.from(rows).map(tr => ({
    key:   tr.querySelector('[data-field=key]').value.trim(),
    label: tr.querySelector('[data-field=label]').value.trim(),
    type:  tr.querySelector('[data-field=type]').value,
    text:  tr.querySelector('[data-field=text]').value.trim(),
  })).filter(m => m.key !== '');
}

function addMacro() {
  const idx = document.querySelectorAll('#macro-tbody tr').length;
  const tr = document.createElement('tr');
  tr.dataset.idx = idx;
  tr.innerHTML = `
    <td><input type="text" value="F${idx}" data-field="key"></td>
    <td><input type="text" value="F${idx}" data-field="label"></td>
    <td><span class="badge-prog">PROGRAMMABLE</span><input type="hidden" data-field="type" value="PROGRAMMABLE"></td>
    <td><input type="text" value="" data-field="text" placeholder="Your template here"></td>
    <td><button class="btn btn-red btn-sm" onclick="removeMacroRow(this)">✕</button></td>`;
  document.getElementById('macro-tbody').appendChild(tr);
}

function removeMacroRow(btn) {
  btn.closest('tr').remove();
}

// ── Save functions ─────────────────────────────────────────
function saveStation() {
  const body = {
    station: {
      callsign:   (document.getElementById('st-call').value||'').toUpperCase().trim(),
      gridSquare: (document.getElementById('st-grid').value||'').toUpperCase().trim(),
      lat:   parseFloat(document.getElementById('st-lat').value)||0,
      lon:   parseFloat(document.getElementById('st-lon').value)||0,
      timezone:   document.getElementById('st-tz').value.trim()||'UTC',
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
      websocketPort: parseInt(document.getElementById('ws-port').value)||8080,
      webConfigPort: parseInt(document.getElementById('web-port').value)||8081,
    }
  };
  postPartialConfig(body, 'ports-msg', 'Ports saved — restart required');
}

function saveAppearance() {
  const ap = {
    theme:          activeSegVal('#theme-seg'),
    fontSize:       parseInt(document.getElementById('font-size-range').value)||13,
    waterfallColor: state.appearance.waterfallColor || 'viridis',
    mapTheme:       state.appearance.mapTheme       || 'dark',
  };
  fetch('/api/appearance', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(ap) })
    .then(r => r.json())
    .then(() => { flashMsg('ap-msg', 'Saved'); state.appearance = ap; })
    .catch(() => flashMsg('ap-msg', 'Error', true));
}

function saveMacros() {
  const list = collectMacros();
  fetch('/api/macros', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(list) })
    .then(r => r.json())
    .then(() => flashMsg('macro-msg', 'Macros saved'))
    .catch(() => flashMsg('macro-msg', 'Error', true));
}

function saveModuleCmd(key, id) {
  const cmd  = document.getElementById('cmd-' + key).value.trim();
  const auto = document.getElementById('auto-' + key).checked;
  const appsUpdate = {};
  appsUpdate[key] = { command: cmd, autoLaunch: auto };
  postPartialConfig({ apps: appsUpdate }, 'mod-msg-' + key, 'Saved');
}

// Merge-patch style partial config update
function postPartialConfig(patch, msgId, okText) {
  const merged = deepMerge(state.config, patch);
  fetch('/api/config', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(merged) })
    .then(r => r.json())
    .then(() => { state.config = merged; flashMsg(msgId, okText); })
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

// ── Cluster actions ────────────────────────────────────────
function clusterConnect() {
  const host  = document.getElementById('cl-host').value.trim();
  const port  = parseInt(document.getElementById('cl-port').value)||7373;
  const login = document.getElementById('cl-login').value.trim();
  fetch('/api/cluster/connect', { method: 'POST', headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ server: host, port, loginCallsign: login }) })
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

// ── PTT test (key TX for 1 second, then release) ──────────
function testPtt() {
  const msg = document.getElementById('rig-save-msg');
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

// ── Rig connection status (polled for the Rig tab) ─────────
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
  fetch('/api/status').then(r => r.json()).then(status => {
    const diag = { timestamp: new Date().toISOString(), status, config: state.config };
    const blob = new Blob([JSON.stringify(diag, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'j-hub-diagnostics.json';
    a.click();
  });
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

// ── Boot ────────────────────────────────────────────────────
// Restore theme before first paint to avoid flash
(function () {
  const saved = localStorage.getItem('jhub-theme') || 'dark';
  applyTheme(saved);
  state.appearance.theme = saved;
})();

loadConfig();
loadMacros();
connectWs();
pollStatus();
pollSpots();
updateRigConnStatus();
setInterval(updateRigConnStatus, 3000);

setInterval(tickClock, 1000);
setInterval(pollStatus, 2000);
setInterval(pollSpots, 10000);
