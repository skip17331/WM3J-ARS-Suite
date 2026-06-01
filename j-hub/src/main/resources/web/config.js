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
  rigError:    '',
  rotor:       null,
  clusterConn: false,
  spm:         0,
  wsState:     'CLOSED',
  appearance:  { theme: 'dark', mapTheme: 'dark' },
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
    if (btn.dataset.tab === 'jvault')      ensureJVaultIframe();
    if (btn.dataset.tab === 'learn')       ensureJLearnIframe();
    // Workshop opens on the Calculators sub-tab by default, but the list
    // column is JS-rendered — populate it on tab activation so the user
    // doesn't see an empty column until they click Calculators again.
    if (btn.dataset.tab === 'antworkshop'
        && typeof awCalcRenderList === 'function'
        && !aw.calc.listRendered) awCalcRenderList();
    // Refresh live values in the Macro Variables Reference table whenever
    // the user opens the Macros tab — covers the case where MYCALL came in
    // before the user navigated here and the rig has since updated.
    if (btn.dataset.tab === 'macros'
        && typeof updateMacroLiveValues === 'function') updateMacroLiveValues();
  });
});

// ── J-Vault embedded iframe + zoom ─────────────────────────
function ensureJVaultIframe() {
  const f = document.getElementById('jvault-frame');
  if (!f) return;
  // Apply persisted zoom on first show.
  let z;
  try { z = localStorage.getItem('jhub.jvault.zoom'); } catch (e) {}
  if (z) {
    const slider = document.getElementById('jvault-text-size');
    if (slider) slider.value = z;
    applyJVaultZoom(z);
  }
  // Probe health BEFORE pointing the iframe at j-vault. If the server isn't
  // up, browsers latch onto the connection-refused page and the iframe stays
  // broken even after j-vault starts — only a hard Reload fixes it.
  if (f.src === 'about:blank' || f.src === '' || f.dataset.jvState === 'down') {
    fetch('http://localhost:8083/api/health', { cache: 'no-store' })
      .then(r => {
        if (!r.ok) throw new Error('bad health');
        f.dataset.jvState = 'up';
        f.src = 'http://localhost:8083/';
      })
      .catch(() => {
        f.dataset.jvState = 'down';
        f.src = JVAULT_NOT_RUNNING_PLACEHOLDER;
      });
  }
}

const JVAULT_NOT_RUNNING_PLACEHOLDER =
  'data:text/html;charset=utf-8,'
  + encodeURIComponent(
      '<!doctype html><html><body style="margin:0;height:100vh;display:flex;'
      + 'flex-direction:column;align-items:center;justify-content:center;gap:10px;'
      + 'font:13px system-ui,-apple-system,Segoe UI,sans-serif;background:#1e1e2e;color:#9399b2;text-align:center;padding:20px">'
      + '<div style="font-size:32px">🗂️</div>'
      + '<div style="font-size:14px">J-Vault isn\'t running on port 8083.</div>'
      + '<div>Click <strong style="color:#cdd6f4">Launch &amp; Attach</strong> above to start it and load it here, '
      + 'or visit <a href="http://localhost:8083/" target="_blank" style="color:#89b4fa">localhost:8083</a> directly.</div>'
      + '</body></html>');
function applyJVaultZoom(pct) {
  const f = document.getElementById('jvault-frame');
  const lbl = document.getElementById('jvault-text-size-val');
  if (!f) return;
  const v = parseInt(pct, 10) || 100;
  if (lbl) lbl.textContent = v;
  // CSS transform scales the iframe; expand width inversely so it fills the row.
  const scale = v / 100;
  f.style.transform = 'scale(' + scale + ')';
  f.style.width = (100 / scale) + '%';
  f.style.transformOrigin = '0 0';
  try { localStorage.setItem('jhub.jvault.zoom', String(v)); } catch (e) {}
}
function reloadJVaultIframe() {
  const f = document.getElementById('jvault-frame');
  if (!f) return;
  // Force a fresh health probe — if we're currently showing the "not running"
  // placeholder and the user has since launched j-vault by hand, this picks up.
  f.dataset.jvState = 'down';
  f.src = 'about:blank';
  ensureJVaultIframe();
}
function openJVaultExternal() {
  window.open('http://localhost:8083/', '_blank');
}

// Start j-vault if it isn't running, then poll /api/health until it comes up
// and reload the embedded iframe. Replaces the bare launchApp('jVault') that
// silently started the process without updating the iframe — leaving the user
// staring at a connection-refused page until they hit Reload by hand.
function launchAndAttachJVault() {
  const meta = document.getElementById('jvault-conn-meta');
  if (meta) meta.textContent = 'Starting…';
  launchApp('jVault');
  pollHealthAndReload({
    url:        'http://localhost:8083/api/health',
    timeoutMs:  15000,
    onReady:    () => { reloadJVaultIframe(); if (meta) meta.textContent = 'Inventory + Estate (port 8083)'; },
    onTimeout:  () => { if (meta) meta.textContent = 'Failed to start — check logs'; },
  });
}

// ── J-Learn embedded iframe ────────────────────────────────
//
// J-Learn moved out of j-hub in v1.1 — it's a separate process on port 8082.
// We iframe it here so the in-hub experience is preserved, but the iframe
// is also fully self-contained: visiting http://localhost:8082/ in a fresh
// browser tab works identically.
function ensureJLearnIframe() {
  const wrap = document.getElementById('jl-iframe-wrap');
  if (!wrap) return;
  if (wrap.querySelector('iframe')) return;
  const status = document.getElementById('jl-status');

  // Probe health BEFORE pointing the iframe at j-learn. If the server isn't
  // up, browsers latch onto the connection-refused page and the iframe stays
  // broken even after j-learn starts — only a hard Reload fixes it.
  fetch('http://localhost:8082/api/health', { cache: 'no-store' })
    .then(r => {
      if (!r.ok) throw new Error('bad health');
      attachJLearnIframe(wrap);
      if (status) status.textContent = 'running on :8082';
    })
    .catch(() => {
      renderJLearnNotRunning(wrap);
      if (status) status.textContent = 'not running — click Launch & Attach';
    });
}

function attachJLearnIframe(wrap) {
  wrap.innerHTML = '';
  const iframe = document.createElement('iframe');
  iframe.id = 'jl-frame';
  iframe.src = 'http://localhost:8082/';
  iframe.style.cssText = 'width:100%;height:100%;border:0;background:transparent';
  iframe.setAttribute('allow', 'fullscreen');
  wrap.appendChild(iframe);
}

function renderJLearnNotRunning(wrap) {
  wrap.innerHTML =
    '<div style="display:flex;flex-direction:column;align-items:center;justify-content:center;'
    + 'height:100%;color:var(--subtext0);gap:10px;text-align:center;padding:20px">'
    + '<div style="font-size:32px">📖</div>'
    + '<div style="font-size:14px">J-Learn isn\'t running on port 8082.</div>'
    + '<div style="font-size:12px">Click <strong>Launch &amp; Attach</strong> above '
    + 'to start it and load it here, or visit '
    + '<a href="http://localhost:8082/" target="_blank" rel="noopener">localhost:8082</a> directly.</div>'
    + '</div>';
}

function reloadJLearnIframe() {
  const wrap = document.getElementById('jl-iframe-wrap');
  if (!wrap) return;
  // Drop any iframe AND any "not running" placeholder, then re-probe health.
  // ensureJLearnIframe() guards against double-mount itself via the existing-
  // iframe early return, so explicit clearing here is needed for reuse.
  wrap.innerHTML = '';
  ensureJLearnIframe();
}

// Start j-learn if it isn't running, then poll /api/health until it comes up
// and reload the embedded iframe. The bare launchApp('j-learn') button used to
// silently spawn the process and never tell the iframe — leaving the user
// staring at "connection refused" forever.
function launchAndAttachJLearn() {
  const status = document.getElementById('jl-status');
  if (status) status.textContent = 'starting…';
  launchApp('j-learn');
  pollHealthAndReload({
    url:        'http://localhost:8082/api/health',
    timeoutMs:  15000,
    onReady:    () => { reloadJLearnIframe(); if (status) status.textContent = 'running on :8082'; },
    onTimeout:  () => { if (status) status.textContent = 'failed to start — check logs'; },
  });
}

// Generic helper: poll an /api/health endpoint until it responds OK or until
// timeoutMs elapses. Runs the matching callback exactly once. Used by both
// J-Learn and J-Vault launch buttons to bridge between "process spawned" and
// "iframe can attach". 400 ms cadence keeps the typical ~2 s startup tight.
function pollHealthAndReload({ url, timeoutMs, onReady, onTimeout }) {
  const start = Date.now();
  const tick = () => {
    fetch(url, { cache: 'no-store' })
      .then(r => {
        if (r.ok) { onReady(); return; }
        if (Date.now() - start > timeoutMs) { onTimeout(); return; }
        setTimeout(tick, 400);
      })
      .catch(() => {
        if (Date.now() - start > timeoutMs) { onTimeout(); return; }
        setTimeout(tick, 400);
      });
  };
  tick();
}

// Drive the iframe to a particular section. Used by Antenna Workshop /
// Formulas calculator cards' "Read about this in J-Learn" links.
function openLearnSection(id) {
  if (!id) return;
  const tabBtn = document.querySelector('[data-tab=learn]');
  if (tabBtn) tabBtn.click();
  ensureJLearnIframe();
  const f = document.getElementById('jl-frame');
  if (!f) return;
  // If iframe is brand new, wait for load before posting; otherwise send now.
  const post = () => f.contentWindow.postMessage({ type: 'jlearn-open', id }, '*');
  if (f.dataset.ready === '1') { post(); return; }
  // Reset src with the section query param so deep-links work even if the
  // iframe hasn't finished loading yet (postMessage might race).
  const targetSrc = 'http://localhost:8082/?section=' + encodeURIComponent(id);
  if (f.src.indexOf('?section=') < 0 && f.src.indexOf('localhost:8082/') >= 0) {
    f.src = targetSrc;
  } else {
    f.addEventListener('load', () => { f.dataset.ready = '1'; post(); }, { once: true });
  }
}

// Receive cross-module action requests from the j-learn iframe (e.g. the
// "Open in Workshop" buttons on §07 / §15 sections) and dispatch into
// j-hub's local handlers.
window.addEventListener('message', ev => {
  const m = ev.data || {};
  if (m.type !== 'jlearn-action') return;
  const action = m.action || '';
  if (action === 'launch-morse')         launchMorseTrainer(null);
  else if (action === 'open-workshop')   openAntennaWorkshop();
  else if (action.startsWith('open-calc:')) openAntennaCalc(action.substring(10));
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

  updateMacroLiveValues();
}

// Populate the "Current value" column in the Macros → Macro Variables Reference
// table. Pulls from state.config.station + state.rig; cells stay "—" when the
// source hasn't reported yet (no welcome, no RIG_STATUS).
function updateMacroLiveValues() {
  const st  = (state.config && state.config.station) || {};
  const rig = state.rig || {};
  const hz  = rig.frequency || rig.rigFrequencyHz || 0;
  const mhz = hz > 0 ? (hz / 1e6).toFixed(3) + ' MHz' : '—';

  setText('mv-mycall', st.callsign || '—');
  setText('mv-freq',   mhz);
  setText('mv-band',   rig.band || '—');
  setText('mv-mode',   rig.mode || '—');
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
  setVal('st-rig-model',    st.rigModel    || '');
  setVal('st-rig-alias',    st.rigAlias    || '');
  setVal('st-rig-operator', st.rigOperator || '');

  // Rig and Alias are independent: Rig = model (e.g. "IC-746pro"),
  // Alias = friendly nickname (e.g. "Backup"). Show both separately.
  const model = (st.rigModel || '').trim();
  const alias = (st.rigAlias || '').trim();
  const aliasRow = document.getElementById('i-rig-alias-row');
  if (aliasRow) aliasRow.style.display = alias ? '' : 'none';
  setText('i-rig-alias',     alias || '—');
  setText('i-rig-model',     model || (st.callsign ? st.callsign + ' Rig' : '—'));
  // Dashboard / rig-control header still surface the alias since that's
  // the short label most users prefer to see at a glance.
  setText('d-rig-alias',     alias);
  setText('rig-header-alias', alias ? '— ' + alias : '');

  // Rig operator: show explicit override if set, else fall back to operator name
  const rigOp = (st.rigOperator || '').trim() || (st.name || '').trim();
  setText('i-rig-operator', rigOp || '—');

  updateMacroLiveValues();
}

// Inline edit for the Operator field in the right intel pane.
function editRigOperatorInline() {
  const span = document.getElementById('i-rig-operator');
  if (!span) return;
  const current = (span.textContent === '—') ? '' : span.textContent;
  const input = document.createElement('input');
  input.type = 'text';
  input.value = current;
  input.style.cssText = 'width:100%;background:var(--surface0);border:1px solid var(--mauve);border-radius:3px;color:var(--text);padding:2px 4px;font-size:12px';
  const commit = () => {
    const v = input.value.trim();
    span.textContent = v || '—';
    saveRigOperator(v);
  };
  input.addEventListener('blur', commit);
  input.addEventListener('keydown', e => {
    if (e.key === 'Enter') { e.preventDefault(); input.blur(); }
    if (e.key === 'Escape') { e.preventDefault(); span.textContent = current || '—'; }
  });
  span.textContent = '';
  span.appendChild(input);
  input.focus();
  input.select();
}

function saveRigOperator(value) {
  fetch('/api/config').then(r => r.json()).then(cfg => {
    cfg.station = cfg.station || {};
    cfg.station.rigOperator = value;
    return fetch('/api/config', {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify(cfg),
    });
  }).then(() => {
    if (state && state.config && state.config.station) {
      state.config.station.rigOperator = value;
    }
  }).catch(err => console.error('saveRigOperator failed', err));
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
    alerts.push({ icon: '⚠️', text: 'Rig not reporting',
                  sub: state.rigError || 'No RIG_STATUS received' });
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

  setChk('jsat-eme-enabled',   !!s.showEmePanel);
  setVal('jsat-eme-freq',      s.emeFrequencyHz ? Math.round(s.emeFrequencyHz / 1_000_000) : 1296);
  setVal('jsat-eme-dx',        s.emeDxGrid || '');
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
    showEmePanel:           document.getElementById('jsat-eme-enabled').checked,
    emeFrequencyHz:         (parseInt(document.getElementById('jsat-eme-freq').value) || 1296) * 1_000_000,
    emeDxGrid:              document.getElementById('jsat-eme-dx').value.trim().toUpperCase(),
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
  setSelectVal('st-tz',           st.timezone   || 'UTC');
  setSelectVal('st-lang',         st.language   || 'en');
  setSelectVal('st-iaru-region',  st.iaruRegion || 'IARU-R2');
  setSelectVal('st-country',      st.country == null ? 'US' : st.country);
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
  // Hamlib bin folder override — top-level field, not under rig.
  setVal('rig-hamlib-bindir', cfg.hamlibBinDir || '');
  // Managed-rigctld fields. Default to "Start rigctld for me" ON for fresh
  // configs (rig.manageRigctld may be undefined on configs predating this
  // feature — !== false treats "missing" as "yes manage").
  setChk('rig-manage', rig.manageRigctld !== false);
  setRigModelUI(rig.rigModel || 0);
  setVal('rig-serial-port', rig.serialPort || '');
  setVal('rig-baud', rig.baudRate || 0);
  updateManageRigctldUI();
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
  // Managed-rotctld fields — mirror the rig pane. !== false treats missing
  // as "yes manage" so configs predating the feature still default ON.
  setChk('rot-manage', rot.manageRotctld !== false);
  setRotorModelUI(rot.rotorModel || 0);
  setVal('rot-serial-port', rot.serialPort || '');
  setVal('rot-baud', rot.baudRate || 0);
  updateManageRotctldUI();
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
  const density = ap.density || localStorage.getItem('jhub-density') || 'comfortable';
  applyDensity(density);
  state.appearance.density = density;
  const densitySel = document.getElementById('density-select');
  if (densitySel) densitySel.value = density;

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
function applyDensity(val) {
  const allowed = ['compact','comfortable','spacious'];
  if (!allowed.includes(val)) val = 'comfortable';
  document.body.classList.remove('density-compact','density-comfortable','density-spacious');
  document.body.classList.add('density-' + val);
  localStorage.setItem('jhub-density', val);
}

function saveDensity(val) {
  applyDensity(val);
  state.appearance = state.appearance || {};
  state.appearance.density = val;
  // POST the full appearance section so j-hub broadcasts CONFIG_UPDATE to every
  // connected module. Spread state.appearance so theme/fontSize aren't reset.
  fetch('/api/appearance', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify(state.appearance)
  }).catch(() => {});
}

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
    { key: 'jVault',  id: 'jVault',   label: 'J-Vault',  desc: 'Shack inventory + estate handoff PDF (port 8083)' },
    { key: 'jLearn',  id: 'j-learn',  label: 'J-Learn',  desc: 'Reference library web app (port 8082)' },
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
  }).join('') + `
    <div class="card">
      <div class="card-title">J-Learn</div>
      <div style="font-size:11px;color:var(--overlay0);margin-bottom:10px">In-app reference library — ~200 chapters bundled inside J-Hub. Not a separate process.</div>
      <div style="font-size:12px;color:var(--subtext0);margin-bottom:10px">
        Open the J-Learn tab from the left nav, or click below.
      </div>
      <div class="btn-row" style="margin-top:6px">
        <button class="btn btn-green btn-sm" onclick="document.querySelector('[data-tab=learn]').click()">Open J-Learn Tab</button>
      </div>
    </div>`;
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
      iaruRegion:  document.getElementById('st-iaru-region').value || 'IARU-R2',
      country:     document.getElementById('st-country').value,
      cqZone:      parseInt(document.getElementById('st-cqzone').value)||0,
      arrlSection: (document.getElementById('st-arrl').value||'').toUpperCase().trim(),
      ituZone:     parseInt(document.getElementById('st-ituzone').value)||0,
      rigModel:    document.getElementById('st-rig-model').value.trim(),
      rigAlias:    document.getElementById('st-rig-alias').value.trim(),
      rigOperator: document.getElementById('st-rig-operator').value.trim(),
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
    manageRigctld: document.getElementById('rig-manage').checked,
    rigModel:   readRigModelId(),
    serialPort: document.getElementById('rig-serial-port').value.trim(),
    baudRate:   parseInt(document.getElementById('rig-baud').value)||0,
    pollRateMs: parseInt(document.getElementById('rig-poll-ms').value)||500,
    enablePtt:  document.getElementById('rig-ptt').checked,
    // Top-level override consumed by DependencyChecker. The /api/rig POST
    // handler pulls this out of the RigSection payload before deserializing.
    hamlibBinDir: document.getElementById('rig-hamlib-bindir').value.trim(),
  };
  fetch('/api/rig', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => {
      flashMsg('rig-save-msg', 'Saved');
      state.config.rig = body;
      state.config.hamlibBinDir = body.hamlibBinDir;
      // Re-run the dependency probe so the user sees the result of a new
      // override immediately on the Dashboard's System Dependencies card.
      if (typeof pollDeps === 'function') pollDeps();
    })
    .catch(() => flashMsg('rig-save-msg', 'Error', true));
}

// ── Managed-rigctld UI helpers ────────────────────────────────────
// "Start rigctld for me" toggle hides/shows the model+port+baud trio.
// When off, the user is on the hook for starting rigctld themselves and
// only host/port matter (current external-daemon behavior).
function updateManageRigctldUI() {
  const on = document.getElementById('rig-manage').checked;
  const managed = document.getElementById('rig-managed-block');
  if (managed) managed.style.display = on ? '' : 'none';
  const hint = document.getElementById('rig-host-hint');
  if (hint) hint.textContent = on
      ? 'J-Hub will bind rigctld here. Leave as localhost:4532 unless you have a reason.'
      : 'Where your existing rigctld is listening.';
}

// Map a saved Hamlib model id back onto the preset dropdown — falls back to
// "Custom" with the numeric input pre-filled when the id isn't in our list.
function setRigModelUI(modelId) {
  const sel = document.getElementById('rig-model-preset');
  if (!sel) return;
  const target = modelId ? String(modelId) : '';
  const has = [...sel.options].some(o => o.value === target && !o.disabled);
  if (has) {
    sel.value = target;
    document.getElementById('rig-model-custom-row').style.display = 'none';
  } else if (modelId) {
    sel.value = 'custom';
    document.getElementById('rig-model-custom-row').style.display = '';
    setVal('rig-model-custom', modelId);
  } else {
    sel.value = '';
    document.getElementById('rig-model-custom-row').style.display = 'none';
  }
}

function onRigModelPresetChange() {
  const sel = document.getElementById('rig-model-preset');
  const customRow = document.getElementById('rig-model-custom-row');
  if (sel.value === 'custom') {
    customRow.style.display = '';
  } else {
    customRow.style.display = 'none';
  }
}

function readRigModelId() {
  const sel = document.getElementById('rig-model-preset');
  if (!sel) return 0;
  if (sel.value === 'custom') {
    return parseInt(document.getElementById('rig-model-custom').value) || 0;
  }
  return parseInt(sel.value) || 0;
}

function saveRotor() {
  const backend = activeSegVal('#rot-backend-seg');
  const body = {
    backend,
    model:   document.getElementById('rot-model').value.trim(),
    comPort: document.getElementById('rot-com').value.trim(),
    tcpHost: document.getElementById('rot-hamlib-host').value.trim()||'localhost',
    tcpPort: parseInt(document.getElementById('rot-hamlib-port').value)||4533,
    manageRotctld: document.getElementById('rot-manage').checked,
    rotorModel:  readRotorModelId(),
    serialPort:  document.getElementById('rot-serial-port').value.trim(),
    baudRate:    parseInt(document.getElementById('rot-baud').value)||0,
    shortPathOffset: parseFloat(document.getElementById('rot-short-offset').value)||0,
    customPreset:    parseFloat(document.getElementById('rot-custom').value)||0,
  };
  fetch('/api/rotor', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => { flashMsg('rot-save-msg', 'Saved'); state.config.rotor = body; setText('i-rot-backend', backend); })
    .catch(() => flashMsg('rot-save-msg', 'Error', true));
}

// ── Managed-rotctld UI helpers ────────────────────────────────────
// Mirror of updateManageRigctldUI / setRigModelUI: when on, j-hub spawns
// rotctld itself and the model+port+baud trio is meaningful; when off,
// only host/port matter and the user runs rotctld themselves.
function updateManageRotctldUI() {
  const el = document.getElementById('rot-manage');
  if (!el) return;
  const on = el.checked;
  const managed = document.getElementById('rot-managed-block');
  if (managed) managed.style.display = on ? '' : 'none';
  const hint = document.getElementById('rot-host-hint');
  if (hint) hint.textContent = on
      ? 'J-Hub will bind rotctld here. Leave as localhost:4533 unless you have a reason.'
      : 'Where your existing rotctld is listening.';
}

function setRotorModelUI(modelId) {
  const sel = document.getElementById('rot-model-preset');
  if (!sel) return;
  const target = modelId ? String(modelId) : '';
  const has = [...sel.options].some(o => o.value === target && !o.disabled);
  if (has) {
    sel.value = target;
    document.getElementById('rot-model-custom-row').style.display = 'none';
  } else if (modelId) {
    sel.value = 'custom';
    document.getElementById('rot-model-custom-row').style.display = '';
    setVal('rot-model-custom', modelId);
  } else {
    sel.value = '';
    document.getElementById('rot-model-custom-row').style.display = 'none';
  }
}

function onRotorModelPresetChange() {
  const sel = document.getElementById('rot-model-preset');
  const customRow = document.getElementById('rot-model-custom-row');
  customRow.style.display = sel.value === 'custom' ? '' : 'none';
}

function readRotorModelId() {
  const sel = document.getElementById('rot-model-preset');
  if (!sel) return 0;
  if (sel.value === 'custom') {
    return parseInt(document.getElementById('rot-model-custom').value) || 0;
  }
  return parseInt(sel.value) || 0;
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
    // Managed-ampctld fields mirror the rig pane. !== false treats missing
    // as "yes manage" so configs predating the feature default ON.
    setChk('amp-manage', amp.manageAmpctld !== false);
    setAmpModelUI(amp.ampModel || 0);
    setVal('amp-serial-port', amp.serialPort || '');
    setVal('amp-baud',        amp.baudRate   || 0);
    updateManageAmpctldUI();
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
    manageAmpctld: document.getElementById('amp-manage').checked,
    ampModel:   readAmpModelId(),
    serialPort: document.getElementById('amp-serial-port').value.trim(),
    baudRate:   parseInt(document.getElementById('amp-baud').value)||0,
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

// ── Managed-ampctld UI helpers ────────────────────────────────────
function updateManageAmpctldUI() {
  const el = document.getElementById('amp-manage');
  if (!el) return;
  const on = el.checked;
  const managed = document.getElementById('amp-managed-block');
  if (managed) managed.style.display = on ? '' : 'none';
  const hint = document.getElementById('amp-host-hint');
  if (hint) hint.textContent = on
      ? 'J-Hub will bind ampctld here. Leave as localhost:4531 unless you have a reason.'
      : 'Where your existing ampctld is listening.';
}

function setAmpModelUI(modelId) {
  const sel = document.getElementById('amp-model-preset');
  if (!sel) return;
  const target = modelId ? String(modelId) : '';
  const has = [...sel.options].some(o => o.value === target && !o.disabled);
  if (has) {
    sel.value = target;
    document.getElementById('amp-model-custom-row').style.display = 'none';
  } else if (modelId) {
    sel.value = 'custom';
    document.getElementById('amp-model-custom-row').style.display = '';
    setVal('amp-model-custom', modelId);
  } else {
    sel.value = '';
    document.getElementById('amp-model-custom-row').style.display = 'none';
  }
}

function onAmpModelPresetChange() {
  const sel = document.getElementById('amp-model-preset');
  const customRow = document.getElementById('amp-model-custom-row');
  customRow.style.display = sel.value === 'custom' ? '' : 'none';
}

function readAmpModelId() {
  const sel = document.getElementById('amp-model-preset');
  if (!sel) return 0;
  if (sel.value === 'custom') {
    return parseInt(document.getElementById('amp-model-custom').value) || 0;
  }
  return parseInt(sel.value) || 0;
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

// ── Antenna Workshop tab navigation (used by openLearnSection iframe handler) ───
function openAntennaWorkshop() {
  const btn = document.querySelector('[data-tab=antworkshop]');
  if (btn) btn.click();
}

function openAntennaCalc(calcId) {
  openAntennaWorkshop();
  // Switch the workshop subnav to "Calculators" if not already, then open the panel.
  setTimeout(() => {
    const tabs = document.querySelectorAll('.aw-tab');
    if (tabs.length >= 2) {
      tabs.forEach(t => t.classList.remove('active'));
      tabs[1].classList.add('active');
      document.getElementById('aw-recommender').style.display = 'none';
      document.getElementById('aw-calculators').style.display = '';
      if (typeof awCalcRenderList === 'function' && !aw.calc.listRendered) awCalcRenderList();
      if (typeof awCalcOpen === 'function') awCalcOpen(calcId);
    }
  }, 50);
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

function loadSkimmer() {
  fetch('/api/skimmer').then(r => r.json()).then(d => {
    const cfg = d.config || {};
    setChk('skimmer-enabled', !!cfg.enabled);
    setVal('skimmer-server',  cfg.server || '127.0.0.1');
    setVal('skimmer-port',    cfg.port   || 7300);
    setVal('skimmer-login',   cfg.loginCallsign || '');
    setVal('skimmer-snr',     cfg.minSnrDb != null ? cfg.minSnrDb : 5);
    buildFilterChips('skimmer-band-filters',
      ['160m','80m','60m','40m','30m','20m','17m','15m','12m','10m','6m','2m','70cm'],
      Array.from(cfg.bands || []));
    buildFilterChips('skimmer-mode-filters',
      ['CW','SSB','FT8','FT4','RTTY','PSK31','JS8'],
      Array.from(cfg.modes || []));
    const dot = document.getElementById('skimmer-dot');
    const txt = document.getElementById('skimmer-status-txt');
    if (dot) dot.className = 'dot ' + (d.connected ? 'green' : (d.running ? 'yellow' : 'gray'));
    if (txt) txt.textContent = d.connected ? 'connected'
                            : d.running   ? 'reconnecting…'
                            :               (cfg.enabled ? 'enabled, not connected' : 'disabled');
  }).catch(() => {});
}

function saveSkimmer() {
  const body = {
    enabled:       document.getElementById('skimmer-enabled').checked,
    server:        document.getElementById('skimmer-server').value.trim() || '127.0.0.1',
    port:          parseInt(document.getElementById('skimmer-port').value) || 7300,
    loginCallsign: document.getElementById('skimmer-login').value.toUpperCase().trim(),
    minSnrDb:      parseInt(document.getElementById('skimmer-snr').value),
    bands:         getCheckedChips('skimmer-band-filters'),
    modes:         getCheckedChips('skimmer-mode-filters'),
  };
  if (isNaN(body.minSnrDb)) body.minSnrDb = 5;
  fetch('/api/skimmer', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body) })
    .then(r => r.json())
    .then(() => { flashMsg('skimmer-msg', 'Saved'); setTimeout(loadSkimmer, 1500); })
    .catch(() => flashMsg('skimmer-msg', 'Error', true));
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

// ── Comm-port test ─────────────────────────────────────────
// One-shot rigctld + frequency query with the form values. Lets the
// user verify COM port + baud + model id without saving first.
function testRigPort() {
  const model = readRigModelId();
  const port  = document.getElementById('rig-serial-port').value.trim();
  const baud  = parseInt(document.getElementById('rig-baud').value) || 0;
  const msg   = document.getElementById('rig-save-msg');
  if (msg) { msg.textContent = 'Testing… (up to 6s)'; msg.style.color = 'var(--overlay0)'; }
  fetch('/api/rig/test', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ rigModel: model, serialPort: port, baudRate: baud })
  })
  .then(r => r.json())
  .then(j => {
    if (!msg) return;
    msg.textContent = j.message || (j.ok ? 'OK' : 'Failed');
    msg.style.color = j.ok ? 'var(--green)' : 'var(--red)';
    // Don't auto-clear — the diagnostic text is the point.
  })
  .catch(err => {
    if (!msg) return;
    msg.textContent = 'Test failed: ' + err.message;
    msg.style.color = 'var(--red)';
  });
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
      const el  = document.getElementById('rig-conn-status');
      const det = document.getElementById('rig-conn-detail');
      if (!el) return;
      if (!s.running) {
        el.textContent = 'Disabled'; el.className = 'rig-conn-badge off';
        state.rigError = '';
        if (det) det.style.display = 'none';
      } else if (s.connected) {
        el.textContent = 'Connected'; el.className = 'rig-conn-badge ok';
        state.rigError = '';
        if (det) det.style.display = 'none';
      } else {
        el.textContent = 'Connecting\u2026'; el.className = 'rig-conn-badge warn';
        state.rigError = s.error || '';
        if (det) {
          det.textContent = s.error ? ('\u26a0 ' + s.error)
                                    : 'Connecting to rigctld\u2026';
          det.style.display = '';
        }
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

// ── Community plug-in registry ───────────────────────────────
// Fetches the manifest, renders contests + awards as cards with
// Install buttons. Install POSTs to /api/plugins/install which
// downloads the JSON and drops it in ~/.j-log/{plugins,awards}/.

function loadPluginRegistry() {
  flashMsg('plugins-msg', 'Loading registry…');
  fetch('/api/plugins/registry').then(r => {
    if (!r.ok) throw new Error('HTTP ' + r.status);
    return r.json();
  }).then(reg => {
    renderPluginList('plugins-contest-list', 'Contests', reg.contests || [], 'contest');
    renderPluginList('plugins-award-list',   'Awards',   reg.awards   || [], 'award');
    const updated = reg.updated ? ' (updated ' + reg.updated + ')' : '';
    flashMsg('plugins-msg', `Registry v${reg.version || '?'}${updated}`);
  }).catch(e => flashMsg('plugins-msg', 'Failed: ' + e.message + ' — check network', true));
}

function renderPluginList(elementId, label, entries, type) {
  const el = document.getElementById(elementId);
  if (!el) return;
  if (!entries.length) {
    el.innerHTML = `<div style="font-size:12px;color:var(--overlay0)">No ${label.toLowerCase()} in the registry yet.</div>`;
    return;
  }
  const rows = entries.map(e => `
    <div style="border:1px solid var(--surface1);border-radius:4px;padding:10px;margin:6px 0">
      <div style="display:flex;justify-content:space-between;gap:10px;align-items:flex-start">
        <div style="flex:1;min-width:0">
          <div style="font-weight:600">${escapeHtml(e.name)} <span style="color:var(--overlay0);font-size:11px;font-weight:normal">v${escapeHtml(e.version || '?')}</span></div>
          <div style="font-size:12px;color:var(--subtext0);margin-top:2px">by ${escapeHtml(e.author || 'unknown')}${e.license ? ' · ' + escapeHtml(e.license) : ''}</div>
          <div style="font-size:12px;color:var(--subtext1);margin-top:4px">${escapeHtml(e.description || '')}</div>
          ${e.homepage ? `<div style="font-size:11px;margin-top:4px"><a href="${escapeAttr(e.homepage)}" target="_blank">${escapeHtml(e.homepage)}</a></div>` : ''}
        </div>
        <button class="btn btn-primary btn-sm" onclick='installPlugin(${JSON.stringify(type)}, ${JSON.stringify(e.downloadUrl)}, ${JSON.stringify(e.id)})'>Install</button>
      </div>
      <div id="plugin-msg-${escapeAttr(type)}-${escapeAttr(e.id)}" style="font-size:11px;color:var(--overlay0);margin-top:4px;min-height:14px"></div>
    </div>
  `).join('');
  el.innerHTML = `<div style="font-weight:600;margin-top:4px;margin-bottom:4px">${label}</div>${rows}`;
}

function installPlugin(type, downloadUrl, id) {
  const msgId = `plugin-msg-${type}-${id}`;
  const msgEl = document.getElementById(msgId);
  if (msgEl) { msgEl.textContent = 'Installing…'; msgEl.style.color = 'var(--overlay0)'; }
  fetch('/api/plugins/install', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ type, downloadUrl, filename: id })
  }).then(r => r.json()).then(r => {
    if (!msgEl) return;
    if (r.error) {
      msgEl.textContent = '✗ ' + r.error;
      msgEl.style.color = '#f38ba8';
    } else {
      msgEl.textContent = `✓ Installed → ${r.path}`;
      msgEl.style.color = '#a6e3a1';
    }
  }).catch(e => {
    if (msgEl) { msgEl.textContent = '✗ ' + e.message; msgEl.style.color = '#f38ba8'; }
  });
}

function escapeHtml(s) {
  return String(s == null ? '' : s)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}
function escapeAttr(s) { return escapeHtml(s); }

// ── Audio Setup Wizard ────────────────────────────────────────
// Probes audio devices via /api/audio/devices, runs a loopback test,
// saves the chosen pair to j-digi's settings. Shipped in Phase 2 of the
// roadmap because audio routing is the #1 setup friction for new
// operators.

function audioProbe() {
  flashMsg('audio-msg', 'Probing…');
  fetch('/api/audio/devices').then(r => r.json()).then(data => {
    const ins  = data.inputs  || [];
    const outs = data.outputs || [];
    fillAudioSelect('audio-input',  ins,  pickRigDefault(ins));
    fillAudioSelect('audio-output', outs, pickRigDefault(outs));
    document.getElementById('audio-wizard').style.display = '';
    flashMsg('audio-msg', `Found ${ins.length} input(s), ${outs.length} output(s)`);
  }).catch(() => flashMsg('audio-msg', 'Probe failed — check j-hub logs', true));
}

function fillAudioSelect(id, devices, preferredId) {
  const sel = document.getElementById(id);
  if (!sel) return;
  sel.innerHTML = '';
  // Group by category so the rig audio device is obvious to pick
  const groups = { 'rig-builtin': [], 'rig-signalink': [], 'rig-digirig': [],
                   'rig-rigblaster': [], 'rig-microham': [],
                   virtual: [], pc: [], other: [] };
  for (const d of devices) (groups[d.category] || groups.other).push(d);
  const ordered = [
    ['Rig audio (probable)', ['rig-signalink','rig-digirig','rig-rigblaster','rig-microham','rig-builtin']],
    ['Virtual cables',       ['virtual']],
    ['PC built-in',          ['pc']],
    ['Other',                ['other']],
  ];
  for (const [label, cats] of ordered) {
    const flat = cats.flatMap(c => groups[c] || []);
    if (!flat.length) continue;
    const og = document.createElement('optgroup');
    og.label = label;
    for (const d of flat) {
      const opt = document.createElement('option');
      opt.value = d.id;
      opt.textContent = d.name + (d.description && d.description !== d.name ? '  —  ' + d.description : '');
      og.appendChild(opt);
    }
    sel.appendChild(og);
  }
  if (preferredId) sel.value = preferredId;
}

function pickRigDefault(devices) {
  // Prefer SignaLink → DigiRig → rig-builtin → first device. Lets the
  // wizard pre-select what most operators will actually choose.
  const priority = ['rig-signalink','rig-digirig','rig-rigblaster','rig-microham','rig-builtin'];
  for (const cat of priority) {
    const hit = devices.find(d => d.category === cat);
    if (hit) return hit.id;
  }
  return devices.length ? devices[0].id : null;
}

function audioLoopbackTest() {
  const inEl  = document.getElementById('audio-input');
  const outEl = document.getElementById('audio-output');
  const resultEl = document.getElementById('audio-test-result');
  if (!inEl.value || !outEl.value) {
    resultEl.textContent = 'Pick input + output first.';
    resultEl.style.color = '#fab387';
    return;
  }
  resultEl.textContent = 'Testing…';
  resultEl.style.color = 'var(--overlay0)';
  fetch('/api/audio/loopback-test', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({
      outputId:    outEl.value,
      inputId:     inEl.value,
      durationMs:  1000,
      freqHz:      700
    })
  }).then(r => r.json()).then(r => {
    if (r.error) {
      resultEl.textContent = '✗ ' + r.error;
      resultEl.style.color = '#f38ba8';
      return;
    }
    const peak = r.peakDb.toFixed(1);
    const snr  = r.snrDb.toFixed(1);
    if (r.detected) {
      resultEl.textContent = `✓ Detected — peak ${peak} dB, SNR ${snr} dB`;
      resultEl.style.color = '#a6e3a1';
    } else {
      resultEl.textContent = `✗ No tone (peak ${peak} dB, SNR ${snr} dB) — check wiring`;
      resultEl.style.color = '#f38ba8';
    }
  }).catch(() => {
    resultEl.textContent = '✗ Test failed (check j-hub logs)';
    resultEl.style.color = '#f38ba8';
  });
}

function audioSaveDevices() {
  const inEl  = document.getElementById('audio-input');
  const outEl = document.getElementById('audio-output');
  if (!inEl.value || !outEl.value) {
    flashMsg('audio-msg', 'Pick input + output first', true);
    return;
  }
  fetch('/api/audio/save', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ inputId: inEl.value, outputId: outEl.value })
  }).then(r => r.json()).then(() => {
    flashMsg('audio-msg', '✓ Saved — J-Digi will pick it up on next audio engine restart');
    // Surface a matching WSJT-X hint with the human-readable device names
    const inText  = inEl.options[inEl.selectedIndex].textContent.split('  —  ')[0];
    const outText = outEl.options[outEl.selectedIndex].textContent.split('  —  ')[0];
    document.getElementById('audio-wsjtx-in').textContent  = inText;
    document.getElementById('audio-wsjtx-out').textContent = outText;
    document.getElementById('audio-wsjtx-hint').style.display = '';
  }).catch(() => flashMsg('audio-msg', 'Save failed', true));
}

function reportIssue() {
  // Open a pre-filled GitHub issue. Pulls the live status snapshot so the
  // body comes pre-populated with versions, OS, and connected modules.
  // The diagnostics zip itself must be attached manually — GitHub's URL API
  // doesn't accept file uploads.
  fetch('/api/status').then(r => r.json()).then(s => {
    const env = s.environment || {};
    const apps = (s.connectedApps || []).map(a => `- ${a.appName} v${a.version || '?'}`).join('\n')
                 || '- (none connected)';
    const title = `[j-hub v1.0.0] <one-line summary>`;
    const body =
`### Reporting from
- Module: J-Hub web UI
- J-Hub uptime: ${s.uptimeSeconds || '?'} s

### Connected modules at time of report
${apps}

### Environment
- OS: ${env.osName || ''} ${env.osVersion || ''}
- Arch: ${env.osArch || ''}
- Java: ${env.javaVersion || ''} (${env.javaVendor || ''})

### What I expected to happen


### What actually happened


### Steps to reproduce
1.
2.
3.

### Diagnostics bundle
Click **Export Diagnostics** (above) first, then drag the downloaded
ars-diag-*.zip into this issue.
`;
    const url = 'https://github.com/skip17331/WM3J-ARS-Suite/issues/new'
      + '?labels=bug'
      + '&title=' + encodeURIComponent(title)
      + '&body='  + encodeURIComponent(body);
    window.open(url, '_blank', 'noopener');
  }).catch(() => {
    // Fall back to an empty pre-fill if /api/status fails
    window.open('https://github.com/skip17331/WM3J-ARS-Suite/issues/new?labels=bug', '_blank', 'noopener');
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
  const rc = document.getElementById('jlog-rig-control-visible');
  if (rc) rc.checked = (s.rigControlVisible !== false);  // default true
  const idt = document.getElementById('jlog-id-timer-enabled');
  if (idt) idt.checked = (s.idTimerEnabled === true);    // default false
  const idm = document.getElementById('jlog-id-timer-minutes');
  if (idm) idm.value = (s.idTimerMinutes || 10);         // default 10
}

function saveJLogSettings() {
  const intOf = id => parseInt(document.getElementById(id).value, 10);
  const wxEl  = document.getElementById('jlog-show-spacewx');
  const rcEl  = document.getElementById('jlog-rig-control-visible');
  const idtEl = document.getElementById('jlog-id-timer-enabled');
  const payload = {
    fontSize: intOf('jlog-font-size') || 13,
    showSpaceWeather:    wxEl ? wxEl.checked : true,
    rigControlVisible:   rcEl ? rcEl.checked : true,
    idTimerEnabled:      idtEl ? idtEl.checked : false,
    idTimerMinutes:      Math.min(60, Math.max(1, intOf('jlog-id-timer-minutes') || 10)),
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

  setSelectVal('jdigi-ptt-method', s.pttMethod || 'VOX');
  setSelectVal('jdigi-cw-keyer',   s.cwKeyer   || 'AUDIO');
  setVal('jdigi-cw-wpm',           s.cwWpm     || 20);
  setChk('jdigi-skimmer-enabled', !!s.localSkimmerEnabled);

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
    pttMethod: document.getElementById('jdigi-ptt-method').value || 'VOX',
    cwKeyer:   document.getElementById('jdigi-cw-keyer').value   || 'AUDIO',
    cwWpm:     intOf('jdigi-cw-wpm') || 20,
    localSkimmerEnabled: document.getElementById('jdigi-skimmer-enabled').checked,
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

// ── Font-size zoom (per-machine, persisted) ─────────────────
function jhubZoomGet() {
  const v = parseFloat(localStorage.getItem('jhub-zoom'));
  return isFinite(v) && v >= 0.7 && v <= 1.6 ? v : 1.0;
}
function jhubZoomApply(v) {
  document.body.style.zoom = String(v);
  try { localStorage.setItem('jhub-zoom', String(v)); } catch (e) {}
}
function jhubZoomDelta(d) {
  let v = jhubZoomGet() + d;
  v = Math.max(0.7, Math.min(1.6, Math.round(v * 20) / 20));
  jhubZoomApply(v);
}
function jhubZoomReset() { jhubZoomApply(1.0); }

// ── Boot ────────────────────────────────────────────────────
// Restore theme and zoom before first paint to avoid flash
(function () {
  const saved = localStorage.getItem('jhub-theme') || 'dark';
  applyTheme(saved);
  state.appearance.theme = saved;
  jhubZoomApply(jhubZoomGet());
})();

populateTimezones();
loadConfig();
loadMacros();
loadAmp();
loadAntenna();
loadRbn();
loadSkimmer();
loadBackup();
loadUploaders();
// J-Learn loads itself when its tab is first opened (lazy iframe).
setInterval(loadRbn, 10000);
setInterval(loadSkimmer, 10000);
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


function escHtml(s) {
  if (s == null) return '';
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}


// ═══════════════════════════════════════════════════════════════════════
// Antenna Workshop — recommender + calculators
// ═══════════════════════════════════════════════════════════════════════
//
// Pure browser-side; no backend. Recommender questionnaire is sourced from
// J-Learn §08-01; calculator math from §08-02..§08-15 and §21.
//
// Scoring approach (§08-01):
//   - Each antenna has a profile mapping question codes to fit functions.
//   - Each question's answer feeds into fitᵢ(A, R) returning 0..10.
//   - Scores are weighted-summed; antennas with any 0 are dropped.
//   - Top results above 0.7 × top-score are returned, ranked.
// ═══════════════════════════════════════════════════════════════════════

const aw = {
  rec: { step: 0, answers: {} },
  calc: { current: null }
};

function awSwitch(which, btn) {
  document.querySelectorAll('.aw-tab').forEach(t => t.classList.remove('active'));
  btn.classList.add('active');
  document.getElementById('aw-recommender').style.display  = which === 'recommender'  ? '' : 'none';
  document.getElementById('aw-calculators').style.display  = which === 'calculators'  ? '' : 'none';
  if (which === 'calculators' && !aw.calc.listRendered) awCalcRenderList();
  // Only initialize the recommender wizard when the user explicitly clicks it.
  if (which === 'recommender' && !aw.rec.initialized) {
    aw.rec.initialized = true;
    awRecRender();
  }
}

// ─────────────────────────────────────────────────────────────────────
// Questionnaire definition (matches §08-01)
// ─────────────────────────────────────────────────────────────────────

const AW_STEPS = [
  {
    title: 'A — Your Dwelling',
    questions: [
      { id: 'A1', text: 'Building type',
        type: 'single',
        choices: [
          ['sf-detached',  'Single-family detached'],
          ['sf-attached',  'Single-family attached (row house, townhouse)'],
          ['mf-low',       'Multi-family low-rise (duplex, triplex, walk-up)'],
          ['mf-high',      'Multi-family high-rise (apartment 3+ stories)'],
          ['mobile',       'Mobile home / manufactured'],
          ['rv',           'RV / camper'],
          ['none',         'No fixed residence'],
        ]},
      { id: 'A2', text: 'Ownership',
        type: 'single',
        choices: [
          ['own',         'Own'],
          ['rent',        'Rent'],
          ['condo-hoa',   'Condo with HOA'],
          ['hoa-sf',      'HOA single-family'],
        ]},
      { id: 'A3', text: 'HOA / antenna restrictions',
        type: 'single',
        choices: [
          ['none',     'None'],
          ['light',    'Some restrictions (no visible antennas above roofline)'],
          ['strict',   'Strict (nothing visible from outside)'],
          ['unknown',  "Don't know / haven't checked"],
        ]},
      { id: 'A4', text: 'Indoor / attic option',
        type: 'single',
        choices: [
          ['attic-clear',  'Yes — usable attic, non-metal roof'],
          ['attic-metal',  'Yes — but attic is metal-clad / has foil insulation'],
          ['no',           'No attic option'],
          ['unknown',      "Don't know"],
        ]},
    ]
  },
  {
    title: 'B — Your Lot',
    questions: [
      { id: 'B1L', text: 'Lot length (ft)', type: 'number', placeholder: 'e.g. 120' },
      { id: 'B1W', text: 'Lot width (ft)',  type: 'number', placeholder: 'e.g. 80' },
      { id: 'B2', text: 'Lot orientation (long axis)',
        type: 'single',
        choices: [
          ['ns',       'North-South'],
          ['ew',       'East-West'],
          ['square',   "Roughly square / doesn't matter"],
          ['irregular','Irregular'],
        ]},
      { id: 'B3N', text: 'How many trees usable as antenna supports?',
        type: 'single',
        choices: [
          ['0', 'None'],
          ['1', '1 tree'],
          ['2', '2 trees'],
          ['3+','3 or more'],
        ]},
      { id: 'B3H', text: 'Tallest tree height (ft)', type: 'number', placeholder: 'e.g. 60' },
      { id: 'B4', text: 'Existing tower',
        type: 'single',
        choices: [
          ['none',     'None'],
          ['pushup',   'Push-up mast (~30 ft)'],
          ['self40',   'Self-supporting 40-50 ft'],
          ['guyed50',  'Guyed 50+ ft'],
          ['crankup',  'Crank-up'],
        ]},
      { id: 'B5', text: 'Could you put up a tower if you wanted?',
        type: 'single',
        choices: [
          ['approved',   'Already approved / no obstacles'],
          ['possible',   'Could be approved but not yet'],
          ['forbidden',  'Forbidden (HOA, lease, code)'],
          ['no-interest','Not interested'],
        ]},
      { id: 'B6', text: 'Roof access',
        type: 'single',
        choices: [
          ['flat',     'Yes — flat roof'],
          ['pitched',  'Yes — pitched roof'],
          ['no',       "No / can't go up there"],
        ]},
      { id: 'B7', text: 'Soil for buried radials',
        type: 'single',
        choices: [
          ['lawn',     'Lawn (good)'],
          ['drive',    'Driveway / patio'],
          ['rocks',    'Rocks / shallow soil'],
          ['salt',     'Salt marsh / coastal (excellent)'],
          ['unknown',  "Don't know"],
        ]},
    ]
  },
  {
    title: 'C — Your Goals',
    questions: [
      { id: 'C1', text: 'Bands wanted (check all that apply)',
        type: 'multi',
        choices: [
          ['160','160m'], ['80','80m'], ['40','40m'], ['30','30m'],
          ['20','20m'], ['17','17m'], ['15','15m'], ['12','12m'],
          ['10','10m'], ['6','6m'], ['2','2m'], ['70cm','70cm'],
        ]},
      { id: 'C2', text: 'Multi-band priority',
        type: 'single',
        choices: [
          ['single', 'One band done well'],
          ['few',    'A few specific bands done well'],
          ['all',    'All HF bands acceptably (with tuner)'],
        ]},
      { id: 'C3', text: 'Operating modes (check all that apply)',
        type: 'multi',
        choices: [
          ['ssb','SSB voice'], ['cw','CW'], ['ft8','FT8 / FT4'],
          ['rtty','RTTY'], ['digital','Other digital (PSK31, JS8Call)'],
          ['fm','FM voice (repeaters)'], ['dv','Digital voice (DMR/Fusion/D-STAR)'],
          ['packet','Packet / APRS'],
        ]},
      { id: 'C4', text: 'Power level',
        type: 'single',
        choices: [
          ['qrp',    'QRP (≤ 10 W)'],
          ['100',    '100 W'],
          ['500',    '500 W'],
          ['legal',  'Legal limit (1500 W)'],
        ]},
      { id: 'C5', text: 'Operating goals (check all that apply)',
        type: 'multi',
        choices: [
          ['rag',    'Local rag-chew'],
          ['nvis',   'Regional NVIS'],
          ['dx',     'DX hunting'],
          ['contest','Contesting'],
          ['emcomm', 'Emergency communications'],
          ['portable','Portable (POTA / SOTA)'],
          ['fd',     'Field Day'],
        ]},
    ]
  },
  {
    title: 'D — Constraints',
    questions: [
      { id: 'D1', text: 'Budget',
        type: 'single',
        choices: [
          ['lt50',    'Under $50'],
          ['50-200',  '$50 – $200'],
          ['200-500', '$200 – $500'],
          ['500-1500','$500 – $1500'],
          ['gt1500',  'Over $1500'],
          ['no-limit','No firm limit'],
        ]},
      { id: 'D2', text: 'Time investment',
        type: 'single',
        choices: [
          ['hours',     'A few hours'],
          ['weekend',   'A weekend'],
          ['weekends',  'Multiple weekends'],
          ['long',      'A long-term project'],
        ]},
      { id: 'D3', text: 'Skill level',
        type: 'single',
        choices: [
          ['first',    'First antenna'],
          ['couple',   'Built one or two'],
          ['exp',      'Experienced builder'],
          ['eng',      'Antenna-engineering background'],
        ]},
      { id: 'D4', text: 'Climbing comfort',
        type: 'single',
        choices: [
          ['tower',    "I'll climb a tower"],
          ['roof',     'Pitched roof OK'],
          ['ladder',   'Step ladder only'],
          ['ground',   "No heights — keep it on the ground"],
        ]},
      { id: 'D5', text: 'Stealth required',
        type: 'single',
        choices: [
          ['none',     'Visible OK'],
          ['subtle',   'Subtle preferred'],
          ['hidden',   'Must be invisible from neighbors'],
          ['stealth',  'Must be invisible always (attic / flagpole only)'],
        ]},
    ]
  },
  {
    title: 'E — Existing Infrastructure',
    questions: [
      { id: 'E2', text: 'Coax in place',
        type: 'single',
        choices: [
          ['none',     'None — buying new'],
          ['rg58',     'RG-58'],
          ['rg8x',     'RG-8X'],
          ['rg213',    'RG-213'],
          ['lmr400',   'LMR-400'],
          ['hardline', 'Hardline (Heliax, etc.)'],
          ['ladder',   'Window line / ladder line'],
          ['unknown',  "Don't know"],
        ]},
      { id: 'E3', text: 'Tuner available',
        type: 'single',
        choices: [
          ['none',      'None'],
          ['internal',  'Internal (rig only)'],
          ['ext100',    'External 100 W'],
          ['ext1500',   'External 1500 W'],
          ['remote',    'Auto + remote'],
        ]},
      { id: 'E4', text: 'Grounding system',
        type: 'single',
        choices: [
          ['none',     'None'],
          ['rod',      'Single ground rod'],
          ['array',    'Multi-rod bonded array'],
          ['counter',  'Dedicated counterpoise wire'],
        ]},
    ]
  }
];

// ─────────────────────────────────────────────────────────────────────
// Per-antenna scoring profiles
// Each profile.fit(answers) returns {score:0..10, reasons:[]}.
// Returning score=0 disqualifies the antenna entirely.
// ─────────────────────────────────────────────────────────────────────

function awBand(answers, b) { return (answers.C1 || []).includes(b); }
function awAnyBand(answers, bs) { return bs.some(b => awBand(answers, b)); }
function awGoal(answers, g) { return (answers.C5 || []).includes(g); }
function awHasModes(answers, ms) { return ms.some(m => (answers.C3 || []).includes(m)); }
function awLot(answers) {
  const L = parseFloat(answers.B1L), W = parseFloat(answers.B1W);
  return { L: isNaN(L) ? null : L, W: isNaN(W) ? null : W,
           longest: isNaN(L) ? (isNaN(W) ? null : W) : (isNaN(W) ? L : Math.max(L, W)) };
}

function awTrees(answers) {
  const n = answers.B3N;
  if (!n) return 0;
  if (n === '3+') return 3;
  return parseInt(n, 10) || 0;
}

function awHasOutdoor(answers) {
  return answers.A3 !== 'strict';
}

function awCanTower(answers) {
  if (answers.B4 && answers.B4 !== 'none') return true;
  return answers.B5 === 'approved' || answers.B5 === 'possible';
}

function longestBandToMHz(b) {
  const map = { 160:1.9, 80:3.7, 40:7.15, 30:10.13, 20:14.15, 17:18.12, 15:21.2,
                12:24.94, 10:28.5, 6:50.15, 2:146, '70cm':446 };
  return map[b] || 14.15;
}

const AW_ANTENNAS = [
  {
    id: 'flat-dipole',
    name: 'Flat Dipole',
    section: '09-02',
    summary: 'Half-wave dipole strung horizontally between two supports. Single-band, simplest, cheapest.',
    fit(a) {
      const reasons = [];
      if (!awHasOutdoor(a) && a.A4 === 'no') return { score: 0 };
      const lot = awLot(a);
      const bands = a.C1 || [];
      if (bands.length === 0) return { score: 4, reasons: ['no bands selected — generic suggestion'] };
      const longestBand = Math.max(...bands.map(b => parseInt(b, 10) || 0).filter(n => !isNaN(n)));
      const halfWaveFt = longestBand > 0 ? Math.round(468 / longestBandToMHz(longestBand)) : 65;
      if (lot.longest && lot.longest < halfWaveFt + 5) {
        return { score: 1, reasons: [`lot ${lot.longest} ft is too short for a ${longestBand}m dipole (~${halfWaveFt} ft)`] };
      }
      let s = 7;
      if (awTrees(a) >= 2) { s += 1.5; reasons.push(`two supports available (${awTrees(a)} trees)`); }
      if (a.A3 === 'none' || a.A3 === 'light') { s += 0.3; }
      if (a.D3 === 'first' || a.D3 === 'couple') { s += 0.5; reasons.push('beginner-friendly build'); }
      if (a.D1 === 'lt50' || a.D1 === '50-200') { s += 0.3; reasons.push('inexpensive (wire + insulators + balun)'); }
      if (a.C2 === 'single') { s += 0.5; reasons.push('matches single-band priority'); }
      if (a.C2 === 'all')    { s -= 1.0; reasons.push('⚠ single-band only — consider trapped or fan variant'); }
      if (a.D5 === 'stealth') { s -= 2; reasons.push('⚠ visible installation'); }
      return { score: Math.min(10, Math.max(0, s)), reasons };
    }
  },
  {
    id: 'inverted-v',
    name: 'Inverted-V Dipole',
    section: '09-03',
    summary: 'Dipole with apex at center support, ends sloping down to stakes. Needs only one tall support.',
    fit(a) {
      const reasons = [];
      if (!awHasOutdoor(a) && a.A4 === 'no') return { score: 0 };
      const bands = a.C1 || [];
      if (bands.length === 0) return { score: 4 };
      const longestBand = Math.max(...bands.map(b => parseInt(b, 10) || 0).filter(n => !isNaN(n)));
      const halfWaveFt = Math.round(468 / longestBandToMHz(longestBand));
      const lot = awLot(a);
      if (lot.longest && lot.longest < halfWaveFt * 0.8) {
        return { score: 1, reasons: [`lot ${lot.longest} ft still too short for ${longestBand}m inverted-V`] };
      }
      let s = 7.5;
      if (awTrees(a) >= 1) { s += 1.5; reasons.push(`one tall support sufficient — ${awTrees(a)} tree(s) available`); }
      if (a.B3H && parseFloat(a.B3H) >= 30) { s += 0.5; reasons.push(`apex height ${a.B3H} ft is good`); }
      if (a.D3 === 'first' || a.D3 === 'couple') { s += 0.5; reasons.push('beginner-friendly'); }
      if (a.D1 === 'lt50' || a.D1 === '50-200') { s += 0.3; reasons.push('inexpensive build'); }
      if (a.D5 === 'stealth') { s -= 1.5; reasons.push('⚠ apex still visible'); }
      return { score: Math.min(10, Math.max(0, s)), reasons };
    }
  },
  {
    id: 'fan-dipole',
    name: 'Fan Dipole',
    section: '09-04',
    summary: 'Multiple parallel dipoles fed from one feedpoint. Resonant on each band without traps.',
    fit(a) {
      const reasons = [];
      if (!awHasOutdoor(a) && a.A4 === 'no') return { score: 0 };
      const bands = (a.C1 || []).filter(b => parseInt(b) <= 80).length;
      if (bands < 2) return { score: 2, reasons: ['fan only useful for 2+ bands'] };
      let s = 7;
      if (a.C2 === 'few') { s += 1.5; reasons.push(`fits "${bands} bands done well" goal`); }
      if (a.D3 === 'couple' || a.D3 === 'exp') { s += 0.5; reasons.push('moderate build skill needed'); }
      if (a.D3 === 'first') { s -= 0.5; reasons.push('⚠ harder first build than single dipole'); }
      if (awTrees(a) >= 2) { s += 1; reasons.push('two supports available'); }
      if (a.D5 === 'stealth') { s -= 2.5; reasons.push('⚠ multiple wires visible'); }
      return { score: Math.min(10, Math.max(0, s)), reasons };
    }
  },
  {
    id: 'trapped-dipole',
    name: 'Trapped Dipole',
    section: '09-05',
    summary: 'Single wire with traps that isolate band segments. Multi-band in less space than fan.',
    fit(a) {
      const reasons = [];
      if (!awHasOutdoor(a) && a.A4 === 'no') return { score: 0 };
      const bands = (a.C1 || []).length;
      if (bands < 2) return { score: 2, reasons: ['traps only useful for 2+ bands'] };
      let s = 7;
      if (a.C2 === 'few') { s += 1; reasons.push('matches few-bands-well goal'); }
      if (a.D2 === 'weekends' || a.D2 === 'long') { s += 0.5; reasons.push('time available for trap construction'); }
      if (a.D2 === 'hours') { s -= 1; reasons.push('⚠ trap building takes a weekend+'); }
      if (a.D3 === 'first') { s -= 1.5; reasons.push('⚠ requires intermediate skill'); }
      const lot = awLot(a);
      if (lot.longest && lot.longest < 65) { s -= 1; reasons.push('⚠ short lot may not fit even trapped 40m'); }
      if (awTrees(a) >= 2) { s += 0.5; reasons.push('two supports available'); }
      if (a.C4 === 'legal') { s -= 0.5; reasons.push('⚠ legal-limit traps need careful design'); }
      return { score: Math.min(10, Math.max(0, s)), reasons };
    }
  },
  {
    id: 'ocf-dipole',
    name: 'OCF Dipole (Windom)',
    section: '09-06',
    summary: 'Dipole fed off-center (~⅓ from one end) with 4:1 or 6:1 balun. Multi-band without traps or tuner.',
    fit(a) {
      const reasons = [];
      if (!awHasOutdoor(a) && a.A4 === 'no') return { score: 0 };
      const bands = a.C1 || [];
      if (bands.length === 0) return { score: 4 };
      let s = 7;
      if (a.C2 === 'few' || a.C2 === 'all') { s += 1; reasons.push('OCF covers 80/40/20/15/10 with one wire'); }
      if (a.E3 === 'none' || a.E3 === 'internal') { s += 0.5; reasons.push('works without external tuner'); }
      if (a.D5 === 'stealth') { s -= 1.5; }
      if (awTrees(a) >= 2) { s += 0.5; reasons.push('two supports available'); }
      if (a.D3 === 'first') { s -= 0.5; reasons.push('⚠ needs balanced feed-line discipline'); }
      return { score: Math.min(10, Math.max(0, s)), reasons };
    }
  },
  {
    id: 'efhw-no-traps',
    name: 'EFHW (No Traps)',
    section: '09-07',
    summary: 'Half-wave wire fed at one end via 49:1 or 64:1 unun. Resonant on harmonics — 80m EFHW covers 40/20/15/10.',
    fit(a) {
      const reasons = [];
      if (!awHasOutdoor(a) && a.A4 === 'no') return { score: 0 };
      let s = 8;
      if (awTrees(a) >= 1) { s += 1; reasons.push(`only one support needed — ${awTrees(a)} tree(s) available`); }
      if (a.C2 === 'few' || a.C2 === 'all') { s += 1; reasons.push('harmonic coverage on 4 bands'); }
      if (a.A4 === 'attic-clear' && !awHasOutdoor(a)) { s += 1; reasons.push('attic install possible'); }
      if (a.D3 !== 'first') { s += 0.3; }
      if (a.D5 === 'stealth') { s -= 1; reasons.push('⚠ wire visible if installed outdoors'); }
      if (a.E3 === 'none') { s -= 0.3; reasons.push('⚠ tuner helps for FT8/SSB on same band'); }
      const lot = awLot(a);
      if (lot.longest && lot.longest < 70) { s -= 1; reasons.push('⚠ short lot — 80m EFHW needs ~134 ft, can be sloped'); }
      return { score: Math.min(10, Math.max(0, s)), reasons };
    }
  },
  {
    id: 'efhw-trapped',
    name: 'EFHW (Trapped)',
    section: '09-08',
    summary: 'Trapped end-fed half-wave for portable / restricted-length installations. Each band cuts the wire shorter.',
    fit(a) {
      const reasons = [];
      if (!awHasOutdoor(a) && a.A4 === 'no') return { score: 0 };
      let s = 6;
      if (awGoal(a, 'portable')) { s += 2; reasons.push('excellent for POTA / SOTA portable'); }
      const lot = awLot(a);
      if (lot.longest && lot.longest < 70) { s += 1.5; reasons.push('fits short lot via traps'); }
      if (a.D3 === 'first') { s -= 2; reasons.push('⚠ trap construction is intermediate'); }
      if (a.D2 === 'hours') { s -= 1.5; reasons.push('⚠ need a weekend to wind traps'); }
      return { score: Math.min(10, Math.max(0, s)), reasons };
    }
  },
  {
    id: 'j-pole',
    name: 'J-Pole (VHF/UHF)',
    section: '09-09',
    summary: 'Half-wave VHF/UHF radiator with quarter-wave matching stub. Easy build, omni, vertical polarization.',
    fit(a) {
      const reasons = [];
      if (!awAnyBand(a, ['2', '70cm', '6'])) return { score: 0, reasons: ['J-pole is for VHF/UHF only'] };
      let s = 7.5;
      if (awHasModes(a, ['fm', 'dv'])) { s += 1; reasons.push('matches FM repeater operating'); }
      if (awGoal(a, 'rag')) { s += 0.5; reasons.push('great for local FM rag-chew'); }
      if (a.D3 === 'first') { s += 0.5; reasons.push('beginner-friendly — copper pipe + solder'); }
      if (a.D1 === 'lt50') { s += 0.5; reasons.push('under $20 in copper'); }
      if (a.A1 === 'mf-high') { s -= 1; reasons.push('⚠ apartment installation challenging'); }
      if (a.D5 === 'stealth') { s -= 1.5; reasons.push('⚠ vertical pipe is visible'); }
      return { score: Math.min(10, Math.max(0, s)), reasons };
    }
  },
  {
    id: 'yagi',
    name: 'Yagi-Uda',
    section: '09-10',
    summary: 'Directional beam with reflector and directors. High gain, narrow beamwidth — best for DX and contesting.',
    fit(a) {
      const reasons = [];
      if (!awCanTower(a)) return { score: 0, reasons: ['Yagi requires a tower'] };
      if (a.B5 === 'forbidden') return { score: 0, reasons: ['tower forbidden'] };
      let s = 6;
      if (awGoal(a, 'dx') || awGoal(a, 'contest')) { s += 2.5; reasons.push('directional gain + F/B ratio for DX/contest'); }
      if (a.B4 && a.B4 !== 'none') { s += 1.5; reasons.push('existing tower'); }
      else if (a.B5 === 'approved') { s += 0.5; reasons.push('tower could be added'); }
      if (a.D1 === 'no-limit' || a.D1 === 'gt1500') { s += 0.5; reasons.push('budget supports commercial Yagi'); }
      if (a.D1 === 'lt50' || a.D1 === '50-200') { s -= 2; reasons.push('⚠ Yagi is expensive'); }
      if (a.D3 === 'first') { s -= 1.5; reasons.push('⚠ Yagi is not a first antenna'); }
      if (a.D4 === 'ground' || a.D4 === 'ladder') { s -= 1; reasons.push('⚠ tower work requires climbing or hire-out'); }
      if (awAnyBand(a, ['20', '15', '10'])) { s += 0.5; reasons.push('20/15/10 are classic Yagi bands'); }
      if (awAnyBand(a, ['80', '40']) && !awAnyBand(a, ['20', '15', '10'])) { s -= 1.5; reasons.push('⚠ 80/40 Yagis are huge and rare'); }
      return { score: Math.min(10, Math.max(0, s)), reasons };
    }
  },
  {
    id: 'vertical',
    name: 'Vertical Antenna',
    section: '09-11',
    summary: 'Quarter-wave / 5/8-wave / half-wave vertical with radial system. Low takeoff angle, omnidirectional.',
    fit(a) {
      const reasons = [];
      if (a.A3 === 'strict' && a.A4 === 'no') return { score: 0 };
      let s = 7;
      if (awGoal(a, 'dx')) { s += 1; reasons.push('low takeoff angle good for DX'); }
      if (a.B7 === 'lawn' || a.B7 === 'salt') { s += 1; reasons.push(`good radial soil (${a.B7})`); }
      if (a.B7 === 'rocks' || a.B7 === 'drive') { s -= 1.5; reasons.push('⚠ radials hard to install in your soil'); }
      if (a.D5 === 'subtle' || a.D5 === 'hidden') { s += 0.5; reasons.push('lower visual profile than horizontal wire'); }
      if (a.D5 === 'stealth') { s -= 1; reasons.push('⚠ still visible — flagpole hide-job needed'); }
      const lot = awLot(a);
      if (lot.longest && lot.longest >= 30) { s += 0.3; reasons.push('lot supports radial field'); }
      if (lot.longest && lot.longest < 30) { s -= 1; reasons.push('⚠ too small for full radial field'); }
      if (awGoal(a, 'nvis')) { s -= 1.5; reasons.push('⚠ vertical bad for NVIS (needs high angle)'); }
      return { score: Math.min(10, Math.max(0, s)), reasons };
    }
  },
  {
    id: 'mag-loop',
    name: 'Magnetic Loop',
    section: '09-14',
    summary: 'Small loop, ~⅛ wavelength, with high-Q tuning capacitor. Stealth-friendly; narrow bandwidth.',
    fit(a) {
      const reasons = [];
      let s = 5;
      if (a.D5 === 'stealth' || a.D5 === 'hidden') { s += 3; reasons.push('excellent stealth — tiny footprint'); }
      if (a.A1 === 'mf-high' || a.A1 === 'mf-low') { s += 2; reasons.push('apartment-friendly indoor option'); }
      if (a.A4 === 'attic-clear') { s += 1.5; reasons.push('attic install practical'); }
      if (awGoal(a, 'portable')) { s += 1; reasons.push('compact for portable'); }
      if (a.C4 === 'legal' || a.C4 === '500') { s -= 1; reasons.push('⚠ high-power mag loop needs vacuum capacitor'); }
      if (a.D1 === 'lt50') { s -= 1; reasons.push('⚠ tuning capacitor often $50-100'); }
      if (a.D3 === 'first') { s -= 1.5; reasons.push('⚠ tricky tuning, narrow BW'); }
      return { score: Math.min(10, Math.max(0, s)), reasons };
    }
  },
];

// ─────────────────────────────────────────────────────────────────────
// Wizard rendering & navigation
// ─────────────────────────────────────────────────────────────────────

function awRecRender() {
  const step = AW_STEPS[aw.rec.step];
  if (!step) return;
  const wrap = document.getElementById('aw-rec-steps');
  wrap.innerHTML =
    `<div class="aw-rec-step-title">${escHtml(step.title)}</div>` +
    step.questions.map(q => awRenderQuestion(q)).join('');
  document.getElementById('aw-rec-progress').textContent =
    `Step ${aw.rec.step + 1} of ${AW_STEPS.length}`;
  document.getElementById('aw-rec-back').disabled = aw.rec.step === 0;
  document.getElementById('aw-rec-next').textContent =
    aw.rec.step === AW_STEPS.length - 1 ? 'See Results →' : 'Next →';
}

function awRenderQuestion(q) {
  const cur = aw.rec.answers[q.id];
  if (q.type === 'number') {
    return `<div class="aw-q">
      <div class="aw-q-text">${escHtml(q.text)}</div>
      <input type="number" class="aw-q-input"
             placeholder="${escHtml(q.placeholder || '')}"
             value="${cur != null ? escHtml(cur) : ''}"
             oninput="awRecAnswerNoRefresh('${q.id}', this.value)">
    </div>`;
  }
  if (q.type === 'single') {
    return `<div class="aw-q">
      <div class="aw-q-text">${escHtml(q.text)}</div>
      <div class="aw-q-choices">
        ${q.choices.map(([v, label]) => `
          <label class="aw-q-choice ${cur === v ? 'sel' : ''}">
            <input type="radio" name="aw-${q.id}" value="${v}"
                   ${cur === v ? 'checked' : ''}
                   onchange="awRecAnswer('${q.id}', '${v}')">
            <span>${escHtml(label)}</span>
          </label>`).join('')}
        <label class="aw-q-choice ${cur === '__skip' ? 'sel' : ''}">
          <input type="radio" name="aw-${q.id}" value="__skip"
                 ${cur === '__skip' ? 'checked' : ''}
                 onchange="awRecAnswer('${q.id}', '__skip')">
          <span style="color:var(--overlay0)">Skip / don't know</span>
        </label>
      </div>
    </div>`;
  }
  if (q.type === 'multi') {
    const sel = Array.isArray(cur) ? cur : [];
    return `<div class="aw-q">
      <div class="aw-q-text">${escHtml(q.text)}</div>
      <div class="aw-q-choices aw-q-multi">
        ${q.choices.map(([v, label]) => `
          <label class="aw-q-choice ${sel.includes(v) ? 'sel' : ''}">
            <input type="checkbox" value="${v}"
                   ${sel.includes(v) ? 'checked' : ''}
                   onchange="awRecAnswerMulti('${q.id}', '${v}', this.checked)">
            <span>${escHtml(label)}</span>
          </label>`).join('')}
      </div>
    </div>`;
  }
  return '';
}

function awRecAnswer(qid, val) {
  aw.rec.answers[qid] = val;
  awRecRender();
}

function awRecAnswerNoRefresh(qid, val) {
  // For numeric inputs — don't re-render or we lose focus
  aw.rec.answers[qid] = val;
}

function awRecAnswerMulti(qid, val, checked) {
  if (!Array.isArray(aw.rec.answers[qid])) aw.rec.answers[qid] = [];
  const arr = aw.rec.answers[qid];
  const idx = arr.indexOf(val);
  if (checked && idx < 0) arr.push(val);
  if (!checked && idx >= 0) arr.splice(idx, 1);
}

function awRecBack() {
  if (aw.rec.step > 0) { aw.rec.step--; awRecRender(); }
}

function awRecNext() {
  if (aw.rec.step < AW_STEPS.length - 1) {
    aw.rec.step++;
    awRecRender();
  } else {
    awRecScore();
  }
}

function awRecRestart() {
  aw.rec = { step: 0, answers: {} };
  document.getElementById('aw-rec-results').style.display = 'none';
  awRecRender();
}

function awCleanAnswers(raw) {
  const a = {};
  for (const [k, v] of Object.entries(raw)) {
    if (v === '__skip') continue;
    if (Array.isArray(v) && v.length === 0) continue;
    if (v === '' || v == null) continue;
    a[k] = v;
  }
  return a;
}

function awRecScore() {
  const a = awCleanAnswers(aw.rec.answers);
  const ranked = AW_ANTENNAS
    .map(ant => {
      const r = ant.fit(a) || { score: 0, reasons: [] };
      return { ant, score: r.score || 0, reasons: r.reasons || [] };
    })
    .filter(r => r.score > 0)
    .sort((a, b) => b.score - a.score);

  const top = ranked.length ? ranked[0].score : 0;
  const cutoff = top * 0.7;
  const show = ranked.filter(r => r.score >= cutoff).slice(0, 5);

  const wrap = document.getElementById('aw-rec-results-list');
  if (!show.length) {
    wrap.innerHTML = '<div style="color:var(--subtext0)">No matching antennas — try answering more questions or relaxing constraints.</div>';
  } else {
    wrap.innerHTML = show.map((r, i) => `
      <div class="aw-rec-result">
        <div class="aw-rec-result-header">
          <div>
            <span class="aw-rec-rank">${i + 1}</span>
            <span class="aw-rec-name">${escHtml(r.ant.name)}</span>
            <span class="aw-rec-score">score ${r.score.toFixed(1)}</span>
          </div>
          <button class="action-btn primary" onclick="awCalcOpen('${r.ant.id}')">▶ Open in calculator</button>
        </div>
        <div class="aw-rec-result-summary">${escHtml(r.ant.summary)}</div>
        <ul class="aw-rec-reasons">
          ${r.reasons.map(reason => `<li>${escHtml(reason)}</li>`).join('')}
        </ul>
        <div class="aw-rec-result-link">
          <a href="#" onclick="document.querySelector('[data-tab=learn]').click(); openLearnSection('${r.ant.section}'); return false">
            Read §${r.ant.section} for the full design notes →
          </a>
        </div>
      </div>`).join('');
  }
  document.getElementById('aw-rec-results').style.display = '';
  document.getElementById('aw-rec-results').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ─────────────────────────────────────────────────────────────────────
// Calculator panels (initial 5 implemented; rest are "coming soon")
// ─────────────────────────────────────────────────────────────────────

function hamBandFor(mhz) {
  if (mhz >= 1.8 && mhz <= 2.0) return '160m';
  if (mhz >= 3.5 && mhz <= 4.0) return '80m';
  if (mhz >= 7.0 && mhz <= 7.3) return '40m';
  if (mhz >= 10.1 && mhz <= 10.15) return '30m';
  if (mhz >= 14.0 && mhz <= 14.35) return '20m';
  if (mhz >= 18.06 && mhz <= 18.17) return '17m';
  if (mhz >= 21.0 && mhz <= 21.45) return '15m';
  if (mhz >= 24.89 && mhz <= 24.99) return '12m';
  if (mhz >= 28.0 && mhz <= 29.7) return '10m';
  if (mhz >= 50 && mhz <= 54) return '6m';
  if (mhz >= 144 && mhz <= 148) return '2m';
  if (mhz >= 420 && mhz <= 450) return '70cm';
  return 'out of band';
}

const AW_CALCS = {
  'flat-dipole': {
    name: 'Flat Dipole',
    section: '09-02',
    inputs: [
      { id: 'freq',   label: 'Frequency (MHz)',     type: 'number', step: '0.001', default: 14.150 },
      { id: 'k',      label: 'Length factor (k)',   type: 'number', step: '0.01',  default: 468,
        hint: 'Imperial: 468 (thin wire). Use 462 for thick aluminum, 471 for very thin wire.' },
      { id: 'units',  label: 'Display units',       type: 'select', default: 'ft',
        choices: [['ft','feet (and inches)'], ['m','meters']] },
    ],
    compute(v) {
      const f = parseFloat(v.freq) || 14.150;
      const k = parseFloat(v.k) || 468;
      const ft = k / f;
      const m = ft * 0.3048;
      const half = ft / 2;
      return { rows: [
        ['Total length',     v.units === 'm' ? `${m.toFixed(2)} m`     : `${ft.toFixed(2)} ft  (${(ft*12).toFixed(1)} in)`],
        ['Each leg (half)',  v.units === 'm' ? `${(m/2).toFixed(2)} m` : `${half.toFixed(2)} ft  (${(half*12).toFixed(1)} in)`],
        ['Free-space λ/2',   v.units === 'm' ? `${(150/f).toFixed(2)} m` : `${(492/f).toFixed(2)} ft  (no end-effect)`],
        ['Expected feed Z (free space)', '~73 Ω resistive at resonance'],
        ['Practical feed Z',             '~50–73 Ω depending on height; lower height drops below 50 Ω'],
      ]};
    },
    diagram() {
      return `<pre class="aw-diag">   support ─────────────────────●─────────────────────── support
                                │
                              feed
                              │
                            coax to rig
       ←───────────  half-wave  ───────────→</pre>`;
    },
    notes: [
      'Trim shorter to raise resonant frequency, longer to lower it. ~1 cm = ~10 kHz on 20m.',
      'Mount at least λ/4 above ground for usable patterns; λ/2 or higher for DX.',
      '1:1 current balun at the feedpoint suppresses common-mode current on the coax shield.',
    ],
  },

  'inverted-v': {
    name: 'Inverted-V Dipole',
    section: '09-03',
    inputs: [
      { id: 'freq',   label: 'Frequency (MHz)',     type: 'number', step: '0.001', default: 7.150 },
      { id: 'apex',   label: 'Apex height (ft)',    type: 'number', step: '1',     default: 35 },
      { id: 'angle',  label: 'Droop angle from horizontal (deg)', type: 'number', step: '5', default: 30 },
    ],
    compute(v) {
      const f = parseFloat(v.freq) || 7.150;
      const angleDeg = parseFloat(v.angle) || 30;
      const corr = 1 - 0.03 * (angleDeg / 45);
      const flatLen = 468 / f;
      const invLen = flatLen * corr;
      const legSlope = invLen / 2;
      const apex = parseFloat(v.apex) || 35;
      const tipHeight = apex - legSlope * Math.cos((90 - angleDeg) * Math.PI / 180);
      return { rows: [
        ['Total length (corrected)', `${invLen.toFixed(2)} ft  (${(invLen*0.3048).toFixed(2)} m)`],
        ['Each leg (sloped)',        `${legSlope.toFixed(2)} ft along the wire`],
        ['Tip height above ground',  `${Math.max(0, tipHeight).toFixed(1)} ft (keep ≥ 8 ft for safety)`],
        ['Expected feed Z',          '~50 Ω at typical droop angles (lower than flat dipole)'],
        ['Match to 50 Ω coax',       '1:1 balun at apex'],
      ]};
    },
    diagram() {
      return `<pre class="aw-diag">                           ●  ← apex (mast / tree)
                          /│\\
                         / │ \\
                        /  │  \\
                       /  feed  \\
                      /    │    \\
                     /  coax to rig  \\
                    /                 \\
              stake ●─── ground ───● stake</pre>`;
    },
    notes: [
      'Droop 30–45° is typical; steeper makes the antenna more vertical (lower feed Z, higher angle).',
      '~3% length correction at 45° droop accounts for end-coupling and ground proximity at the tips.',
      'Apex must be at least λ/4 high for effective NVIS / regional coverage.',
    ],
  },

  'efhw-no-traps': {
    name: 'EFHW (No Traps)',
    section: '09-07',
    inputs: [
      { id: 'freq',   label: 'Lowest band (MHz)',  type: 'number', step: '0.01',  default: 7.150,
        hint: 'EFHW is half-wave at this frequency, harmonic-resonant on 2×, 3×, 4× of it.' },
      { id: 'unun',   label: 'Unun ratio',          type: 'select', default: '49',
        choices: [['49','49:1 — most common'], ['64','64:1 — slightly higher Z'], ['56','56:1 — alternative']] },
    ],
    compute(v) {
      const f = parseFloat(v.freq) || 7.150;
      const lenFt = 478 / f;
      const harmonics = [];
      for (const h of [1, 2, 3, 4]) {
        const fh = f * h;
        if (fh < 30) harmonics.push(`${h}× (${fh.toFixed(2)} MHz → ${hamBandFor(fh)})`);
      }
      const unun = parseInt(v.unun, 10) || 49;
      const expectedZ = unun === 49 ? '~2450 Ω' : unun === 64 ? '~3200 Ω' : '~2800 Ω';
      return { rows: [
        ['Total wire length',     `${lenFt.toFixed(1)} ft  (${(lenFt*0.3048).toFixed(2)} m)`],
        ['Counterpoise length',   `~${(lenFt * 0.05).toFixed(1)} ft  (~5% of wire — soldered to unun ground side)`],
        ['Expected end Z',        expectedZ],
        ['Unun primary:secondary', unun === 49 ? '2:14 turns' : unun === 64 ? '2:16 turns' : '2:14.7 turns (typical)'],
        ['Harmonic resonance',    harmonics.join(';  ')],
      ]};
    },
    diagram() {
      return `<pre class="aw-diag">       ┌──── 49:1 unun ────┐
       │                   │
   ────┴──●═════════════════════════════ wire (length above) → end insulator
   coax   ↑
          5% counterpoise tail</pre>`;
    },
    notes: [
      'Harmonic resonance: a 40m EFHW (~67 ft) is also resonant on 20m, 15m, 10m.',
      'Counterpoise (~5% of wire length) is critical — without it, coax shield carries common-mode current.',
      'Run the wire in a straight line for cleanest impedance; gentle slope is fine.',
    ],
  },

  'j-pole': {
    name: 'J-Pole (VHF/UHF)',
    section: '09-09',
    inputs: [
      { id: 'freq',   label: 'Center frequency (MHz)', type: 'number', step: '0.01', default: 146.000 },
      { id: 'mat',    label: 'Material',                type: 'select', default: 'copper',
        choices: [['copper','½" copper pipe — VF ≈ 0.96'], ['aluminum','Aluminum tubing — VF ≈ 0.96'], ['450ohm','450Ω ladder line — VF ≈ 0.91']] },
    ],
    compute(v) {
      const f = parseFloat(v.freq) || 146.000;
      const vf = v.mat === '450ohm' ? 0.91 : 0.96;
      const halfWaveIn = (5905.5 * vf) / f;
      const quarterIn  = halfWaveIn / 2;
      const feedFromBaseIn = quarterIn * 0.10;
      return { rows: [
        ['Half-wave radiator (long element)', `${halfWaveIn.toFixed(2)} in  (${(halfWaveIn*2.54).toFixed(1)} cm)`],
        ['Quarter-wave matching stub',         `${quarterIn.toFixed(2)} in  (${(quarterIn*2.54).toFixed(1)} cm)`],
        ['Stub spacing',                       v.mat === '450ohm' ? '450Ω ladder line spacing' : 'About 1.5 inches center-to-center'],
        ['Feed point from bottom',             `${feedFromBaseIn.toFixed(2)} in  — adjust ±¼" for SWR minimum`],
        ['Bottom of stub',                     'Connected (shorted) — tied to coax shield via short jumper'],
      ]};
    },
    diagram() {
      return `<pre class="aw-diag">     ▲  ← top of half-wave radiator (free end)
     │
     │
     │   ← long element (½λ)
     │
     │   ┌──── 1.5" spacing ───┐
     │   │                      │
     │   │ ← short stub (¼λ)    │
     │   │                      │
     ●───●─── feed point  (slide to tune)
              ↑
            coax to rig</pre>`;
    },
    notes: [
      'Tune by sliding the feedpoint up/down the stub for SWR minimum at the operating frequency.',
      'Vertical mounting; vertical polarization. Omnidirectional.',
      'No radials needed — stub provides the matching network.',
    ],
  },

  'vertical': {
    name: 'Vertical Antenna (¼λ)',
    section: '09-11',
    inputs: [
      { id: 'freq',   label: 'Frequency (MHz)', type: 'number', step: '0.01', default: 14.150 },
      { id: 'radials', label: 'Radials installed', type: 'number', step: '1', default: 16 },
    ],
    compute(v) {
      const f = parseFloat(v.freq) || 14.150;
      const ft = 234 / f;
      const radials = parseInt(v.radials, 10) || 16;
      const radialLenFt = ft;
      const totalRadialFt = radials * radialLenFt;
      const groundLossDb = radials >= 32 ? '< 0.5 dB' :
                            radials >= 16 ? '~1 dB'  :
                            radials >= 8  ? '~2 dB'  :
                            radials >= 4  ? '~3 dB'  : '~5 dB';
      return { rows: [
        ['Vertical element length',   `${ft.toFixed(2)} ft  (${(ft*12).toFixed(1)} in)`],
        ['Each radial length',         `${radialLenFt.toFixed(2)} ft  (¼λ; trim to fit your yard)`],
        ['Number of radials',          `${radials}  (32+ ideal, 16 acceptable, 4 minimum)`],
        ['Total radial wire needed',   `${totalRadialFt.toFixed(0)} ft`],
        ['Estimated ground-system loss', groundLossDb],
        ['Expected feed Z',            '~36 Ω over a perfect ground; raised radials → ~50 Ω'],
        ['Match to 50 Ω coax',         radials >= 32 ? 'Direct or 1:1 balun' : '1:1 balun + tuner if needed'],
      ]};
    },
    diagram() {
      return `<pre class="aw-diag">                  ▲  ← tip of vertical (¼λ)
                  │
                  │
                  │
                  │   feed at base
                  ●═════════════════════ ground level
                ╱─┼─╲
              ╱   │   ╲
            ╱   coax    ╲   ← buried radials (¼λ each, 16-32 ideal)
          ╱       to       ╲
        ╱         rig         ╲</pre>`;
    },
    notes: [
      '¼λ vertical needs a radial system — verticals over poor ground can be 6 dB worse than horizontal dipoles.',
      "Radials don't need to be exactly ¼λ — any length helps; longer is better.",
      'Salt-marsh QTH: 4 radials may be enough. Sand or rocks: 32+ radials minimum for usable performance.',
    ],
  },

  // ─── Fan dipole ────────────────────────────────────────────────
  'fan-dipole': {
    name: 'Fan Dipole',
    section: '09-04',
    inputs: [
      { id: 'b1', label: 'Band 1 (MHz)', type: 'number', step: '0.01', default: 7.150 },
      { id: 'b2', label: 'Band 2 (MHz)', type: 'number', step: '0.01', default: 14.150 },
      { id: 'b3', label: 'Band 3 (MHz, optional)', type: 'number', step: '0.01', default: 21.200 },
      { id: 'b4', label: 'Band 4 (MHz, optional)', type: 'number', step: '0.01', default: 28.500 },
      { id: 'sep', label: 'End-to-end separation (in)', type: 'number', step: '0.5', default: 4,
        hint: 'Gap between adjacent dipole tips. 4-6" is typical; closer = more interaction = more shortening.' },
    ],
    compute(v) {
      const sep = parseFloat(v.sep) || 4;
      // Coupling factor: ~3% shortening at 6" sep, ~5% at 4", ~8% at 2"
      const couple = Math.max(0.92, Math.min(0.98, 0.97 - 0.012 * (6 - sep)));
      const elements = [];
      for (const k of ['b1', 'b2', 'b3', 'b4']) {
        const f = parseFloat(v[k]);
        if (!f || f < 1) continue;
        const flatLen = 468 / f;
        const corrected = flatLen * couple;
        const half = corrected / 2;
        elements.push([
          `${f.toFixed(2)} MHz (${hamBandFor(f)})`,
          `${corrected.toFixed(2)} ft total · ${half.toFixed(2)} ft each leg`
        ]);
      }
      return { rows: [
        ['Coupling correction',     `×${couple.toFixed(3)}  (~${((1-couple)*100).toFixed(1)}% shortening)`],
        ['Tip separation',          `${sep.toFixed(1)} in between adjacent element tips`],
        ['Feed point',              'All elements connected at one center insulator + 1:1 balun'],
        ...elements,
        ['Match',                   '50 Ω coax + 1:1 current balun'],
      ]};
    },
    diagram() {
      return `<pre class="aw-diag">                       feed (1:1 balun)
                              │
                              │
       ●─── 20m ─────────●─────────────── 20m ───●
       ●─── 40m ────────────●───────────────── 40m ───●
       ●─── 80m ────────────────●───────────────────── 80m ───●
                              │
                            coax to rig

      All elements meet at one feedpoint. Tips ~4-6" apart.</pre>`;
    },
    notes: [
      'Trim each band independently from longest to shortest — start with the lowest-frequency element.',
      'Each band is resonant on its own; shorter elements look like high-impedance opens at lower bands.',
      'Mutual coupling between elements means you must build all of them before final trim.',
      'Wider tip-spacing reduces coupling but takes more lateral space. 4-6" is a good compromise.',
    ],
  },

  // ─── Trapped dipole ─────────────────────────────────────────────
  'trapped-dipole': {
    name: 'Trapped Dipole',
    section: '09-05',
    inputs: [
      { id: 'nbands', label: 'Number of bands', type: 'select', default: '3',
        choices: [['2','2'],['3','3'],['4','4']] },
      { id: 'b1', label: 'Band 1 — highest (MHz)', type: 'number', step: '0.01', default: 14.150 },
      { id: 'b2', label: 'Band 2 (MHz)',           type: 'number', step: '0.01', default: 7.150 },
      { id: 'b3', label: 'Band 3 (MHz)',           type: 'number', step: '0.01', default: 3.700 },
      { id: 'b4', label: 'Band 4 (MHz, if 4-band)', type: 'number', step: '0.01', default: 1.900 },
      { id: 'L_avail', label: 'Available total length (ft, tip-to-tip)', type: 'number', step: '1', default: 0,
        hint: '0 = no constraint (full-size design). Otherwise the design will be shortened to fit.' },
      { id: 'cap_pf', label: 'Trap capacitor (pF)', type: 'select', default: '100',
        choices: [['50','50'],['75','75'],['100','100'],['150','150'],['220','220'],['330','330']] },
      { id: 'power', label: 'Power (W PEP)', type: 'number', step: '50', default: 100 },
    ],
    compute(v) {
      // ── Inputs ──
      const n = parseInt(v.nbands, 10) || 3;
      const f = [];
      for (let i = 1; i <= n; i++) {
        const fi = parseFloat(v['b'+i]);
        if (fi && fi > 0) f.push(fi);
      }
      if (f.length !== n) {
        return { rows: [['Error', `Need ${n} valid band frequencies (got ${f.length}).`]] };
      }
      f.sort((a, b) => b - a);   // descending: highest first
      const L_avail = parseFloat(v.L_avail) || 0;
      const C       = parseFloat(v.cap_pf) || 100;
      const P       = parseFloat(v.power) || 100;
      const Z0      = 600;       // approximate wire characteristic impedance for loading calc
      const Qtrap   = 100;       // typical air-core trap Q
      const Qcoil   = 200;       // typical air-core loading-coil Q

      // ── Helpers ──
      function trapForC(fr_MHz, C_pF) {
        const fr = fr_MHz * 1e6;
        const Lh = 1 / (4 * Math.PI * Math.PI * fr * fr * C_pF * 1e-12);
        const XLr = 2 * Math.PI * fr * Lh;
        const Vpk = Math.sqrt(P * Qtrap * XLr);
        return { L_uH: Lh * 1e6, C_pF: C_pF, XL_fr: XLr, V_peak: Vpk };
      }
      function trapForX(fr_MHz, f_MHz, Xohms) {
        const fr = fr_MHz * 1e6, fa = f_MHz * 1e6;
        const Lh = Xohms * (1 - (fa / fr) ** 2) / (2 * Math.PI * fa);
        const Cf = 1 / (4 * Math.PI * Math.PI * fr * fr * Lh);
        const XLr = 2 * Math.PI * fr * Lh;
        const Vpk = Math.sqrt(P * Qtrap * XLr);
        return { L_uH: Lh * 1e6, C_pF: Cf * 1e12, XL_fr: XLr, V_peak: Vpk };
      }
      function capClass(V) {
        if (V < 700)  return 'silvered mica / NPO ceramic';
        if (V < 2000) return 'doorknob ceramic 1–3 kV';
        if (V < 6000) return 'vacuum capacitor 5 kV+';
        return 'vacuum capacitor 10 kV+';
      }

      // ── Full-size segment lengths (per side) ──
      // Dipole full size: 468 / f(MHz). Inner = λ/2 at highest band, divided by 2 for per-side.
      // Each outer extends total per-side length to λ/2 of the next-lower band.
      const segFull = [];
      let cumFull = 0;
      for (let i = 0; i < n; i++) {
        const fullPerSide = (468 / f[i]) / 2;
        const seg = fullPerSide - cumFull;
        segFull.push(seg);
        cumFull = fullPerSide;
      }
      const fullTipToTip = 2 * cumFull;

      // ── Determine shortening ──
      let s = 1.0;
      let shortened = false;
      if (L_avail > 0 && L_avail < fullTipToTip) {
        s = L_avail / fullTipToTip;
        shortened = true;
      }
      // Built segments (uniform shortening factor applied to all segments per side):
      const seg = segFull.map(x => x * s);

      // ── Per-band loading & trap calculations ──
      // For band i (i >= 1), trap_(i-1) is resonant at f[i-1] and acts inductive at f[i].
      // The required extra X at f[i] equals what the missing segment-i physical length would have provided.
      // δ per side = segFull[i] − seg[i]
      const traps = [];
      const Xtotal_arr = [0];  // X_loaded per band (band 0 set below; bands 1..n-1 use trap X)
      for (let i = 1; i < n; i++) {
        const lambdaB = 984 / f[i];
        const dSeg = segFull[i] - seg[i];
        const fr = f[i - 1];
        // Standard trap (resonant at fr, using user's C) already provides some loading at f[i]:
        const Lstd_h = 1 / (4 * Math.PI * Math.PI * (fr * 1e6) ** 2 * C * 1e-12);
        const Xstd_at_fi = 2 * Math.PI * (f[i] * 1e6) * Lstd_h / (1 - (f[i] / fr) ** 2);
        let trap;
        let Xtotal;
        if (shortened && dSeg > 0.05) {
          const Xextra = Z0 * Math.tan(2 * Math.PI * dSeg / lambdaB);
          Xtotal = Xstd_at_fi + Math.max(0, Xextra);
          trap = trapForX(fr, f[i], Xtotal);
        } else {
          Xtotal = Xstd_at_fi;
          trap = trapForC(fr, C);
        }
        traps.push({ ...trap, X_at_fi: Xtotal, fr_MHz: fr, fi_MHz: f[i] });
        Xtotal_arr.push(Xtotal);
      }

      // ── Inner-segment center loading coil (if inner shortened) ──
      let innerCoil_uH = 0, innerCoil_X = 0;
      if (shortened && segFull[0] - seg[0] > 0.05) {
        const lambda1 = 984 / f[0];
        const dInner = segFull[0] - seg[0];
        innerCoil_X = Z0 * Math.tan(2 * Math.PI * dInner / lambda1);
        innerCoil_uH = innerCoil_X / (2 * Math.PI * (f[0] * 1e6)) * 1e6;
        Xtotal_arr[0] = innerCoil_X;
      } else {
        Xtotal_arr[0] = 30;   // small effective antenna reactance for full-size
      }

      // ── Radiation resistance per band (rough: 73 · (effective_length / full_λ/2)²) ──
      const Rr = [];
      for (let i = 0; i < n; i++) {
        const fullAtBand = (468 / f[i]) / 2;        // per side
        const builtAtBand = seg.slice(0, i + 1).reduce((a, b) => a + b, 0);
        const ratio = Math.min(1, builtAtBand / fullAtBand);
        Rr.push(Math.max(2, 73 * ratio * ratio));
      }

      // ── Bandwidth per band ──
      // Practical loaded-antenna formula: BW(Hz) ≈ f · (Rr + X/Q) / X
      // where X = loading reactance on this band, Q = element Q (~100 for traps, ~200 for air-core coils).
      // Capped at f / Q_natural (≈ 15) — the natural Q of a thin-wire half-wave element.
      const BW_kHz = [];
      for (let i = 0; i < n; i++) {
        const X = Math.max(10, Xtotal_arr[i]);
        const Qe = (i === 0 && innerCoil_uH > 0.1) ? Qcoil : Qtrap;
        const bwHz = (f[i] * 1e6) * (Rr[i] + X / Qe) / X;
        const bwCap_kHz = (f[i] * 1000) / 15;
        BW_kHz.push(Math.min(bwHz / 1000, bwCap_kHz));
      }

      // ── Total built length ──
      const totalPerSide = seg.reduce((a, b) => a + b, 0);

      // ── Build output rows ──
      const rows = [];

      // 1. Overview
      rows.push(['── 1. Antenna Overview ──', '']);
      rows.push(['Design type', shortened ? 'Shortened trapped dipole' : 'Full-size trapped dipole']);
      rows.push(['Bands', `${n} bands: ${f.map(x => x.toFixed(3) + ' MHz').join(', ')}`]);
      if (shortened) {
        rows.push(['Why shortened',
          `Available length ${L_avail.toFixed(1)} ft is less than the full-size requirement of ${fullTipToTip.toFixed(1)} ft. ` +
          `Applying uniform shortening factor s = ${(s * 100).toFixed(1)}% to all segments and re-specing traps to provide the missing inductive loading.`]);
      } else if (L_avail > 0) {
        rows.push(['Note', `Available length ${L_avail.toFixed(1)} ft meets/exceeds full-size requirement ${fullTipToTip.toFixed(1)} ft; building full size.`]);
      } else {
        rows.push(['Note', 'No length constraint specified — designing at full size.']);
      }

      // 2. Segment Length Plan
      rows.push(['', '']);
      rows.push(['── 2. Segment Length Plan ──', '']);
      rows.push(['BUILT tip-to-tip', `${(2 * totalPerSide).toFixed(2)} ft  (${totalPerSide.toFixed(2)} ft per side)`]);
      if (shortened) {
        rows.push(['Shortening applied', `s = ${(s * 100).toFixed(1)}% of full-size  (full-size would be ${fullTipToTip.toFixed(2)} ft)`]);
      } else {
        rows.push(['Shortening applied', `none — full-size design (no length constraint, or constraint exceeds full-size)`]);
      }
      rows.push([`Inner segment, each side (λ/2 at ${f[0].toFixed(2)} MHz)`,
        `${seg[0].toFixed(2)} ft` +
        (shortened && segFull[0] - seg[0] > 0.05 ? `  (ideal ${segFull[0].toFixed(2)} ft  ← shortened)` : `  (full-size)`)]);
      for (let i = 1; i < n; i++) {
        rows.push([`Outer ${i}, each side (extends to ${f[i].toFixed(2)} MHz)`,
          `${seg[i].toFixed(2)} ft` +
          (shortened && segFull[i] - seg[i] > 0.05 ? `  (ideal ${segFull[i].toFixed(2)} ft  ← shortened)` : `  (full-size)`)]);
      }

      // 3. Loading Components
      rows.push(['', '']);
      rows.push(['── 3. Loading Components ──', '']);
      if (!shortened) {
        rows.push(['Loading needed', 'None beyond the natural inductive loading provided by the standard traps below their resonant frequency.']);
      }
      for (let i = 0; i < traps.length; i++) {
        const t = traps[i];
        rows.push([`Trap ${i + 1} — resonant at ${t.fr_MHz.toFixed(2)} MHz`,
          `L = ${t.L_uH.toFixed(2)} µH  ·  C = ${t.C_pF.toFixed(0)} pF`]);
        rows.push([`  Effect`,
          `Open-Z (isolates ${t.fr_MHz.toFixed(2)} MHz);  ~${t.X_at_fi.toFixed(0)} Ω inductive at ${t.fi_MHz.toFixed(2)} MHz (loads the shortened outer segment).`]);
        rows.push([`  Cap voltage @ ${P} W`, `${t.V_peak.toFixed(0)} V peak  →  ${capClass(t.V_peak)}`]);
      }
      if (innerCoil_uH > 0.1) {
        rows.push(['Center loading coil per leg',
          `${innerCoil_uH.toFixed(2)} µH (one in each leg at the feedpoint) — replaces missing inner-segment length on ${f[0].toFixed(2)} MHz.`]);
      }
      rows.push(['Capacitive hats',
        shortened && s < 0.55
          ? 'Recommended at wire tips to reduce required coil/trap reactance and lift radiation resistance. Each hat adds the equivalent of ~5–10% of physical length per side.'
          : 'Not required for this design; can be added at tips to slightly broaden bandwidth.']);

      // 4. Placement
      rows.push(['', '']);
      rows.push(['── 4. Trap / Coil Placement ──', '']);
      rows.push(['Rule', 'Higher-frequency trap closest to feed; each lower-frequency trap farther out. Traps mirror across the feedpoint (one set per leg).']);
      let layout = `feed`;
      if (innerCoil_uH > 0.1) layout += ` → [${innerCoil_uH.toFixed(1)} µH coil]`;
      layout += ` → ${seg[0].toFixed(1)} ft wire`;
      for (let i = 1; i < n; i++) {
        layout += `  → [Trap ${i}: ${f[i - 1].toFixed(2)} MHz]  → ${seg[i].toFixed(1)} ft wire`;
      }
      rows.push(['Per-side layout (feed → tip)', layout]);

      // 5. Feedpoint Impedance & Matching
      rows.push(['', '']);
      rows.push(['── 5. Feedpoint Impedance & Matching ──', '']);
      for (let i = 0; i < n; i++) {
        rows.push([`Estimated Z on ${f[i].toFixed(2)} MHz`,
          `~${Rr[i].toFixed(0)} Ω (full-size dipole = 73 Ω; loaded antennas drop with shortening)`]);
      }
      rows.push(['Recommended match',
        shortened
          ? '50 Ω coax + 1:1 current balun. Use an antenna tuner at the rig for SWR > 2:1 outside band centers.'
          : '50 Ω coax + 1:1 current balun directly. Tuner needed only at band edges.']);

      // 6. SWR Behavior
      rows.push(['', '']);
      rows.push(['── 6. Expected SWR Behavior ──', '']);
      if (shortened && s < 0.55) {
        rows.push(['Curve shape', 'Very sharp V-shaped minima at each band center — narrow tuning window, steep edges.']);
      } else if (shortened && s < 0.75) {
        rows.push(['Curve shape', 'Pronounced V-shaped minima; SWR < 2:1 over a smaller fraction of each band than a full-size trapped dipole.']);
      } else if (shortened) {
        rows.push(['Curve shape', 'Standard trapped-dipole V shape, slightly steeper sides than full size.']);
      } else {
        rows.push(['Curve shape', 'Typical trapped-dipole behavior: SWR ≈ 1.5:1 at center, rising to ~2:1 at band edges.']);
      }
      rows.push(['Narrowest band', `${f[n - 1].toFixed(2)} MHz (lowest band) — highest loading reactance = sharpest tuning`]);

      // 7. Bandwidth per band
      rows.push(['', '']);
      rows.push(['── 7. Expected 2:1 SWR Bandwidth ──', '']);
      rows.push(['Formula used', 'BW ≈ f · (Rr + X_load/Q) / X_load (loaded-antenna approximation; Q ≈ 100 for traps, 200 for air-core coils)']);
      for (let i = 0; i < n; i++) {
        const X = Math.max(10, Xtotal_arr[i]);
        const tag = (i === 0 && innerCoil_uH > 0.1) ? 'coil-loaded' :
                    (i === 0) ? 'unloaded' : 'trap-loaded';
        const bwStr = BW_kHz[i] < 10 ? BW_kHz[i].toFixed(1) : BW_kHz[i].toFixed(0);
        rows.push([`${f[i].toFixed(2)} MHz  (${tag})`,
          `BW ≈ ${bwStr} kHz    [Rr ≈ ${Rr[i].toFixed(0)} Ω,  X_load ≈ ${X.toFixed(0)} Ω]`]);
      }
      for (let i = 0; i < n; i++) {
        if (BW_kHz[i] < 20 && f[i] < 5) {
          rows.push(['  ⚠ ' + f[i].toFixed(2) + ' MHz BW',
            'extremely narrow — operating practical only within ±' + (BW_kHz[i] / 2).toFixed(1) + ' kHz of design frequency.']);
        }
      }

      // 8. Performance & Efficiency Opinion
      rows.push(['', '']);
      rows.push(['── 8. Performance & Efficiency Opinion ──', '']);
      let verdict;
      if (!shortened) {
        verdict = 'Full-size trapped dipole. Expect ~0.5–1.0 dB total trap loss across bands; otherwise a strong performer.';
      } else if (s >= 0.85) {
        verdict = 'Mild shortening (≤15%). Practical, near-full performance. Slight bandwidth narrowing; trap voltage manageable.';
      } else if (s >= 0.70) {
        verdict = 'Moderate shortening (15–30%). Real-world good — popular space-saving QTH antenna. Plan on doorknob caps and noticeably narrower lowest-band BW.';
      } else if (s >= 0.55) {
        verdict = 'Heavy shortening (30–45%). Marginal: efficiency suffers (~1.5–2.5 dB), bandwidth tight. Doorknob or vacuum caps required.';
      } else if (s >= 0.40) {
        verdict = 'Extreme shortening (45–60%). Poor efficiency, very narrow BW. Build only if no alternative; expect to retune frequently within bands.';
      } else {
        verdict = 'Below 40% — not recommended as a trapped dipole; the loading reactance is so dominant that radiation resistance becomes < 5 Ω. Pick a different topology.';
      }
      rows.push(['Verdict', verdict]);
      rows.push(['Radiation resistance trend',
        `Rr drops from ${Rr[0].toFixed(0)} Ω on ${f[0].toFixed(2)} MHz to ${Rr[n - 1].toFixed(0)} Ω on ${f[n - 1].toFixed(2)} MHz.`]);
      rows.push(['Trap insertion loss',
        `${traps.length} trap${traps.length > 1 ? 's' : ''} × ~0.5–1 dB each on the bands below their resonance. Cumulative on the lowest band: ~${(traps.length * 0.75).toFixed(1)} dB.`]);

      // 9. Warnings & Practical Notes
      rows.push(['', '']);
      rows.push(['── 9. Warnings & Practical Notes ──', '']);
      const warnings = [];
      if (L_avail > 0 && L_avail < segFull[0] * 2 * 0.4) {
        warnings.push(`⚠ Available length ${L_avail.toFixed(1)} ft is well below the highest-band ideal ${(segFull[0] * 2).toFixed(1)} ft — design infeasible as a dipole.`);
      }
      if (shortened && s < 0.5) {
        warnings.push(`⚠ At s = ${(s * 100).toFixed(0)}% the antenna is severely shortened. Alternatives to consider:`);
        warnings.push(`   • Loaded vertical with elevated radials (uses ~30 ft of vertical real estate, similar efficiency)`);
        warnings.push(`   • Magnetic loop antenna (5–10 ft diameter, very narrow BW but efficient on ${f[n - 1].toFixed(1)} MHz)`);
        warnings.push(`   • Drop the lowest band (${f[n - 1].toFixed(2)} MHz) and build a fuller-size design for the higher bands`);
        warnings.push(`   • Add capacitive hats at the tips to reduce required loading reactance`);
      }
      for (let i = 0; i < traps.length; i++) {
        if (traps[i].V_peak > 2000 && P < 500) {
          warnings.push(`⚠ Trap ${i + 1} needs doorknob/vacuum caps even at ${P} W power level`);
        }
      }
      if (BW_kHz[n - 1] < 20 && f[n - 1] < 5) {
        warnings.push(`⚠ Lowest-band BW < 20 kHz — you may not cover the whole CW or phone segment without retuning.`);
      }
      if (warnings.length === 0) warnings.push('No specific warnings — sound design within practical limits.');
      warnings.forEach(w => rows.push(['', w]));

      return { rows };
    },
    diagram() {
      return `<pre class="aw-diag">              feed (1:1 balun)
                     │
                     ▼
   ●═══════════[Trap1]═══════════[Trap2]═══════════● outer end
   ←─inner─→  ←──outer 1──→  ←─── outer 2 ────→
    20m         opens at        opens at
    leg         14 MHz          7 MHz

   On 20m: traps look like high-Z opens, only inner radiates
   On 40m: trap 2 opens; inner + outer-1 radiate
   On 80m: both traps open; full antenna radiates

  SHORTY VARIANT (shorten > 0):
   Outer segments are physically shorter than standard. Traps are
   re-spec'd with HIGHER inductance (lower C) so their below-resonance
   reactance does more loading work. Same f_r, bigger coil, smaller cap.</pre>`;
    },
    notes: [
      'Length-driven design: set Available Length to your space limit (tip-to-tip) and the calc fits a shortened design; leave it at 0 for full-size.',
      'Trim from highest band to lowest. Inner segment first (traps shorted), then add each outer band one at a time.',
      'Higher-frequency traps go closest to the feedpoint; lower-frequency traps farther out (one set per leg, mirrored).',
      'When the antenna is shortened, traps are re-spec\'d with higher L and lower C (same f_r) so their below-resonance reactance does more loading work.',
      'Real bandwidth on the lowest band is typically 60–80% of the calculated estimate due to trap and coil losses; widen capacitive-hat additions if you need more.',
      'Feed via 50 Ω coax with a 1:1 current balun; a rig-end tuner helps on shortened designs.',
    ],
  },

  // ─── OCF (Windom) ───────────────────────────────────────────────
  'ocf-dipole': {
    name: 'OCF Dipole (Windom)',
    section: '09-06',
    inputs: [
      { id: 'freq', label: 'Lowest band (MHz)', type: 'number', step: '0.01', default: 7.150 },
      { id: 'offset', label: 'Feed offset from center (%)', type: 'number', step: '1', default: 33,
        hint: '33% gives ~200Ω feed → 4:1 balun. 25% gives ~300Ω → 6:1 balun. Classic Windom: 36%.' },
      { id: 'balun', label: 'Balun ratio', type: 'select', default: '4',
        choices: [['4', '4:1 — 200Ω target (standard OCF)'], ['6', '6:1 — 300Ω target (Carolina Windom)'], ['9', '9:1 — 450Ω target']] },
    ],
    compute(v) {
      const f = parseFloat(v.freq) || 7.150;
      const offset = parseFloat(v.offset) || 33;
      const totalLen = 468 / f;
      const shortLeg = totalLen * (offset / 100);
      const longLeg = totalLen - shortLeg;
      const expectedZ = v.balun === '4' ? '~200 Ω' : v.balun === '6' ? '~300 Ω' : '~450 Ω';
      const ratio = v.balun === '4' ? '4:1' : v.balun === '6' ? '6:1' : '9:1';
      const harmonics = [];
      for (const h of [1, 2, 4, 8]) {
        const fh = f * h;
        if (fh < 30) harmonics.push(`${fh.toFixed(2)} MHz → ${hamBandFor(fh)}`);
      }
      return { rows: [
        ['Total length',       `${totalLen.toFixed(2)} ft  (${(totalLen*0.3048).toFixed(2)} m)`],
        ['Short leg',          `${shortLeg.toFixed(2)} ft  (${offset}% from center)`],
        ['Long leg',           `${longLeg.toFixed(2)} ft  (${(100-offset)}% from center)`],
        ['Feed-point Z (target)', expectedZ],
        ['Balun ratio',        `${ratio} current balun at the feedpoint`],
        ['Bands covered (harmonics)', harmonics.join('; ')],
        ['Common-mode choke',  'Add a 1:1 current choke at the rig end of the coax'],
      ]};
    },
    diagram() {
      return `<pre class="aw-diag">  ●──── short leg ────●─────── long leg ───────●
                       │
                     [Balun 4:1]
                       │
                     coax to rig

  Feed point is OFF-CENTER — typically 36% from one end (Carolina)
  or 33% (classic Windom). The off-center feed presents a 200-300 Ω
  impedance that's roughly band-independent across HF.</pre>`;
    },
    notes: [
      "OCF dipoles are known harmonically — an 80m OCF feeds 80/40/20/15/10 with usable SWR.",
      'The off-center feed picks an impedance plateau across the harmonic bands.',
      'A 1:1 common-mode choke at the rig end is essential — OCF feedlines radiate without it.',
      'WARC bands (30/17/12) typically need a tuner; harmonic plateau doesn\'t cover them.',
      'Total length varies between vendors — measure and trim with an analyzer for best SWR.',
    ],
  },

  // ─── EFHW trapped ───────────────────────────────────────────────
  'efhw-trapped': {
    name: 'EFHW (Trapped)',
    section: '09-08',
    inputs: [
      { id: 'nbands', label: 'Number of bands', type: 'select', default: '3',
        choices: [['2','2'],['3','3'],['4','4']] },
      { id: 'b1', label: 'Band 1 — highest (MHz)', type: 'number', step: '0.01', default: 14.150 },
      { id: 'b2', label: 'Band 2 (MHz)',           type: 'number', step: '0.01', default: 7.150 },
      { id: 'b3', label: 'Band 3 (MHz)',           type: 'number', step: '0.01', default: 3.700 },
      { id: 'b4', label: 'Band 4 (MHz, if 4-band)', type: 'number', step: '0.01', default: 1.900 },
      { id: 'L_avail', label: 'Available total wire length (ft)', type: 'number', step: '1', default: 0,
        hint: '0 = no constraint (full-size). Otherwise the wire will be shortened to fit.' },
      { id: 'cap_pf', label: 'Trap capacitor (pF)', type: 'select', default: '100',
        choices: [['50','50'],['75','75'],['100','100'],['150','150'],['220','220'],['330','330']] },
      { id: 'power', label: 'Power (W PEP)', type: 'number', step: '50', default: 100 },
      { id: 'unun', label: 'Unun ratio', type: 'select', default: '49',
        choices: [['49','49:1'], ['64','64:1']] },
    ],
    compute(v) {
      // ── Inputs ──
      const n = parseInt(v.nbands, 10) || 3;
      const f = [];
      for (let i = 1; i <= n; i++) {
        const fi = parseFloat(v['b'+i]);
        if (fi && fi > 0) f.push(fi);
      }
      if (f.length !== n) {
        return { rows: [['Error', `Need ${n} valid band frequencies (got ${f.length}).`]] };
      }
      f.sort((a, b) => b - a);
      const L_avail = parseFloat(v.L_avail) || 0;
      const C       = parseFloat(v.cap_pf) || 100;
      const P       = parseFloat(v.power) || 100;
      const Z0      = 600;
      const Qtrap   = 100;
      const Qcoil   = 200;
      // EFHW traps sit close to the high-impedance end of the wire; voltage is roughly 2.5× the dipole-equivalent.
      const efhwV   = 2.5;

      // ── Helpers ──
      function trapForC(fr_MHz, C_pF) {
        const fr = fr_MHz * 1e6;
        const Lh = 1 / (4 * Math.PI * Math.PI * fr * fr * C_pF * 1e-12);
        const XLr = 2 * Math.PI * fr * Lh;
        const Vpk = Math.sqrt(P * Qtrap * XLr) * efhwV;
        return { L_uH: Lh * 1e6, C_pF: C_pF, XL_fr: XLr, V_peak: Vpk };
      }
      function trapForX(fr_MHz, f_MHz, Xohms) {
        const fr = fr_MHz * 1e6, fa = f_MHz * 1e6;
        const Lh = Xohms * (1 - (fa / fr) ** 2) / (2 * Math.PI * fa);
        const Cf = 1 / (4 * Math.PI * Math.PI * fr * fr * Lh);
        const XLr = 2 * Math.PI * fr * Lh;
        const Vpk = Math.sqrt(P * Qtrap * XLr) * efhwV;
        return { L_uH: Lh * 1e6, C_pF: Cf * 1e12, XL_fr: XLr, V_peak: Vpk };
      }
      function capClass(V) {
        if (V < 1000) return 'silvered mica 1 kV';
        if (V < 3000) return 'doorknob ceramic 3 kV';
        if (V < 7000) return 'vacuum capacitor 5–10 kV';
        return 'vacuum capacitor 10 kV+';
      }

      // ── Full-size segment lengths ──
      // EFHW is end-fed half-wave: full wire = 478/f(MHz) ft (with end-fed correction).
      // Each outer segment extends the wire to reach 478/f for the next-lower band.
      const segFull = [];
      let cumFull = 0;
      for (let i = 0; i < n; i++) {
        const fullForBand = 478 / f[i];
        const seg = fullForBand - cumFull;
        segFull.push(seg);
        cumFull = fullForBand;
      }
      const fullTotal = cumFull;

      // ── Shortening ──
      let s = 1.0;
      let shortened = false;
      if (L_avail > 0 && L_avail < fullTotal) {
        s = L_avail / fullTotal;
        shortened = true;
      }
      const seg = segFull.map(x => x * s);

      // ── Per-band loading & trap calc ──
      const traps = [];
      const Xtotal_arr = [0];
      for (let i = 1; i < n; i++) {
        const lambdaB = 984 / f[i];
        const dSeg = segFull[i] - seg[i];
        const fr = f[i - 1];
        const Lstd_h = 1 / (4 * Math.PI * Math.PI * (fr * 1e6) ** 2 * C * 1e-12);
        const Xstd_at_fi = 2 * Math.PI * (f[i] * 1e6) * Lstd_h / (1 - (f[i] / fr) ** 2);
        let trap, Xtotal;
        if (shortened && dSeg > 0.05) {
          const Xextra = Z0 * Math.tan(2 * Math.PI * dSeg / lambdaB);
          Xtotal = Xstd_at_fi + Math.max(0, Xextra);
          trap = trapForX(fr, f[i], Xtotal);
        } else {
          Xtotal = Xstd_at_fi;
          trap = trapForC(fr, C);
        }
        traps.push({ ...trap, X_at_fi: Xtotal, fr_MHz: fr, fi_MHz: f[i] });
        Xtotal_arr.push(Xtotal);
      }

      // ── Seg-1 inline coil if seg 1 shortened more than ~10% ──
      let seg1Coil_uH = 0, seg1Coil_X = 0;
      if (shortened && segFull[0] - seg[0] > Math.max(0.05, segFull[0] * 0.10)) {
        const lambda1 = 984 / f[0];
        const dSeg1 = segFull[0] - seg[0];
        seg1Coil_X = Z0 * Math.tan(2 * Math.PI * dSeg1 / lambda1);
        seg1Coil_uH = seg1Coil_X / (2 * Math.PI * (f[0] * 1e6)) * 1e6;
        Xtotal_arr[0] = seg1Coil_X;
      } else {
        Xtotal_arr[0] = 50;
      }

      // ── Rr per band (rough): 36 · (built / 478/f)² (EFHW radiation res ~36 Ω at full size at the input through unun) ──
      const Rr = [];
      for (let i = 0; i < n; i++) {
        const fullAtBand = 478 / f[i];
        const builtAtBand = seg.slice(0, i + 1).reduce((a, b) => a + b, 0);
        const ratio = Math.min(1, builtAtBand / fullAtBand);
        Rr.push(Math.max(2, 36 * ratio * ratio));
      }

      // ── BW per band ──
      const BW_kHz = [];
      for (let i = 0; i < n; i++) {
        const X = Math.max(10, Xtotal_arr[i]);
        const Qe = (i === 0 && seg1Coil_uH > 0.1) ? Qcoil : Qtrap;
        const bwHz = (f[i] * 1e6) * (Rr[i] + X / Qe) / X;
        BW_kHz.push(bwHz / 1000);
      }

      const totalLen = seg.reduce((a, b) => a + b, 0);

      // ── Output rows ──
      const rows = [];

      // 1. Overview
      rows.push(['── 1. Antenna Overview ──', '']);
      rows.push(['Design type', shortened ? 'Shortened trapped EFHW' : 'Full-size trapped EFHW']);
      rows.push(['Bands', `${n} bands: ${f.map(x => x.toFixed(3) + ' MHz').join(', ')}`]);
      if (shortened) {
        rows.push(['Why shortened',
          `Available wire length ${L_avail.toFixed(1)} ft is less than the full-size requirement of ${fullTotal.toFixed(1)} ft. ` +
          `Applying uniform shortening s = ${(s * 100).toFixed(1)}% to all segments and re-specing traps to provide extra inductive loading.`]);
      } else if (L_avail > 0) {
        rows.push(['Note', `Available length ${L_avail.toFixed(1)} ft ≥ full-size requirement ${fullTotal.toFixed(1)} ft; building full size.`]);
      } else {
        rows.push(['Note', 'No length constraint specified — designing at full size.']);
      }

      // 2. Segment Length Plan
      rows.push(['', '']);
      rows.push(['── 2. Segment Length Plan ──', '']);
      rows.push(['BUILT total wire length', `${totalLen.toFixed(2)} ft`]);
      if (shortened) {
        rows.push(['Shortening applied', `s = ${(s * 100).toFixed(1)}% of full-size  (full-size would be ${fullTotal.toFixed(2)} ft)`]);
      } else {
        rows.push(['Shortening applied', `none — full-size design`]);
      }
      rows.push([`Seg 1 (unun → trap 1, λ/2 at ${f[0].toFixed(2)} MHz)`,
        `${seg[0].toFixed(2)} ft` +
        (shortened && segFull[0] - seg[0] > 0.05 ? `  (ideal ${segFull[0].toFixed(2)} ft  ← shortened)` : `  (full-size)`)]);
      for (let i = 1; i < n; i++) {
        const tail = (i === n - 1) ? 'end' : `trap ${i + 1}`;
        rows.push([`Seg ${i + 1} (trap ${i} → ${tail}, extends to ${f[i].toFixed(2)} MHz)`,
          `${seg[i].toFixed(2)} ft` +
          (shortened && segFull[i] - seg[i] > 0.05 ? `  (ideal ${segFull[i].toFixed(2)} ft  ← shortened)` : `  (full-size)`)]);
      }

      // 3. Loading Components
      rows.push(['', '']);
      rows.push(['── 3. Loading Components ──', '']);
      if (!shortened) {
        rows.push(['Loading needed', 'None beyond natural trap inductance below resonance.']);
      }
      for (let i = 0; i < traps.length; i++) {
        const t = traps[i];
        rows.push([`Trap ${i + 1} — resonant at ${t.fr_MHz.toFixed(2)} MHz`,
          `L = ${t.L_uH.toFixed(2)} µH  ·  C = ${t.C_pF.toFixed(0)} pF`]);
        rows.push([`  Effect`,
          `Open-Z at ${t.fr_MHz.toFixed(2)} MHz;  ~${t.X_at_fi.toFixed(0)} Ω inductive at ${t.fi_MHz.toFixed(2)} MHz (loads the shortened next segment).`]);
        rows.push([`  Cap voltage @ ${P} W (EFHW × ${efhwV})`, `${t.V_peak.toFixed(0)} V peak  →  ${capClass(t.V_peak)}`]);
      }
      if (seg1Coil_uH > 0.1) {
        rows.push(['Seg-1 inline loading coil',
          `${seg1Coil_uH.toFixed(2)} µH placed ~⅓ to ½ along seg 1 — replaces missing length on ${f[0].toFixed(2)} MHz.`]);
      }
      rows.push(['Capacitive hats',
        shortened && s < 0.55
          ? 'Recommended at the wire end to broaden lowest-band BW and reduce required trap reactance.'
          : 'Optional; can be added at the wire end to slightly broaden bandwidth.']);

      // 4. Placement
      rows.push(['', '']);
      rows.push(['── 4. Trap / Coil Placement ──', '']);
      rows.push(['Rule', 'Higher-frequency trap closest to the unun; each lower-frequency trap farther out (single wire, one trap per band boundary).']);
      let layout = `unun ⇒ ${seg[0].toFixed(1)} ft`;
      if (seg1Coil_uH > 0.1) layout += ` (incl. ${seg1Coil_uH.toFixed(1)} µH inline coil)`;
      for (let i = 1; i < n; i++) {
        const lbl = (i === n - 1) ? 'end' : '';
        layout += `  → [Trap ${i}: ${f[i - 1].toFixed(2)} MHz]  → ${seg[i].toFixed(1)} ft${lbl ? ' ⇒ ' + lbl : ''}`;
      }
      rows.push(['Layout (unun → end)', layout]);

      // 5. Feedpoint Impedance & Matching
      rows.push(['', '']);
      rows.push(['── 5. Feedpoint Impedance & Matching ──', '']);
      for (let i = 0; i < n; i++) {
        const Zend = Rr[i] * (v.unun === '49' ? 49 : 64);
        rows.push([`Estimated end Z on ${f[i].toFixed(2)} MHz`,
          `~${Rr[i].toFixed(0)} Ω at the input of the unun (full = 36 Ω);  ~${Zend.toFixed(0)} Ω at unun output`]);
      }
      rows.push(['Recommended match',
        `${v.unun}:1 unun (primary 2 turns, secondary ${v.unun === '49' ? '14' : '16'} turns on FT240-43 toroid). ` +
        (shortened ? 'Add a 1:1 common-mode choke at the rig end of the coax; tuner recommended on shortened designs.' : 'A 1:1 common-mode choke at the rig end is essential.')]);
      rows.push(['Counterpoise', `~${(segFull[0] * 0.05).toFixed(1)} ft (5% of full-size seg 1) — connect to unun ground side`]);

      // 6. SWR Behavior
      rows.push(['', '']);
      rows.push(['── 6. Expected SWR Behavior ──', '']);
      if (shortened && s < 0.55) {
        rows.push(['Curve shape', 'Very sharp V-shaped minima — narrow operating window, steep edges, may require tuner for full band coverage.']);
      } else if (shortened && s < 0.75) {
        rows.push(['Curve shape', 'Sharper minima than full-size; usable < 2:1 over ~50–70% of each band center.']);
      } else if (shortened) {
        rows.push(['Curve shape', 'Mild narrowing relative to full-size trapped EFHW; SWR < 2:1 across most of each band.']);
      } else {
        rows.push(['Curve shape', 'Standard trapped EFHW: SWR ≈ 1.5–2:1 across each band, broader than a trapped dipole.']);
      }
      rows.push(['Narrowest band', `${f[n - 1].toFixed(2)} MHz (lowest band) — highest loading reactance and lowest Rr`]);

      // 7. Bandwidth per band
      rows.push(['', '']);
      rows.push(['── 7. Expected 2:1 SWR Bandwidth ──', '']);
      rows.push(['Formula used', 'BW ≈ f · (Rr + X_load/Q) / X_load (loaded-antenna approximation; Q ≈ 100 traps, 200 air-core coils)']);
      for (let i = 0; i < n; i++) {
        const X = Math.max(10, Xtotal_arr[i]);
        const tag = (i === 0 && seg1Coil_uH > 0.1) ? 'coil-loaded' :
                    (i === 0) ? 'unloaded' : 'trap-loaded';
        const bwStr = BW_kHz[i] < 10 ? BW_kHz[i].toFixed(1) : BW_kHz[i].toFixed(0);
        rows.push([`${f[i].toFixed(2)} MHz  (${tag})`,
          `BW ≈ ${bwStr} kHz    [Rr ≈ ${Rr[i].toFixed(0)} Ω,  X_load ≈ ${X.toFixed(0)} Ω]`]);
      }
      for (let i = 0; i < n; i++) {
        if (BW_kHz[i] < 20 && f[i] < 5) {
          rows.push(['  ⚠ ' + f[i].toFixed(2) + ' MHz BW',
            'extremely narrow — practical only within ±' + (BW_kHz[i] / 2).toFixed(1) + ' kHz of design frequency.']);
        }
      }

      // 8. Performance & Efficiency Opinion
      rows.push(['', '']);
      rows.push(['── 8. Performance & Efficiency Opinion ──', '']);
      let verdict;
      if (!shortened) {
        verdict = 'Full-size trapped EFHW. Solid performer; expect ~0.5–1.5 dB total trap loss across bands. Excellent for portable / fixed multi-band.';
      } else if (s >= 0.85) {
        verdict = 'Mild shortening (≤15%). Practical, near-full performance. Reasonable trap voltages.';
      } else if (s >= 0.70) {
        verdict = 'Moderate shortening (15–30%). Practical for SOTA/POTA — wire bag drops by 1/3. Plan on doorknob+ caps for the innermost trap.';
      } else if (s >= 0.55) {
        verdict = 'Heavy shortening (30–45%). Marginal: efficiency suffers (~1.5–3 dB on lowest band), tight BW, vacuum caps likely required at full power.';
      } else if (s >= 0.40) {
        verdict = 'Extreme shortening (45–60%). Poor — very narrow BW, vacuum caps required, traps approach physical limits.';
      } else {
        verdict = 'Below 40% — not viable as an EFHW. Consider a different topology (vertical, loop, etc.).';
      }
      rows.push(['Verdict', verdict]);
      rows.push(['Trap insertion loss',
        `${traps.length} trap${traps.length > 1 ? 's' : ''} × ~0.5–1.5 dB each on the bands below their resonance. Cumulative on lowest band: ~${(traps.length * 1.0).toFixed(1)} dB.`]);
      rows.push(['Voltage warning',
        `EFHW traps see ~${efhwV}× the voltage of dipole traps at the same power. Plan capacitor type accordingly.`]);

      // 9. Warnings & Practical Notes
      rows.push(['', '']);
      rows.push(['── 9. Warnings & Practical Notes ──', '']);
      const warnings = [];
      if (L_avail > 0 && L_avail < segFull[0] * 0.4) {
        warnings.push(`⚠ Available length ${L_avail.toFixed(1)} ft is well below the highest-band ideal ${segFull[0].toFixed(1)} ft — design infeasible as an EFHW.`);
      }
      if (shortened && s < 0.5) {
        warnings.push(`⚠ At s = ${(s * 100).toFixed(0)}% the wire is severely shortened. Alternatives:`);
        warnings.push(`   • Half-size loaded vertical with elevated radials`);
        warnings.push(`   • Magnetic loop antenna (very narrow BW but efficient)`);
        warnings.push(`   • Drop the lowest band; design for higher bands at fuller size`);
        warnings.push(`   • Add capacitive end-hat to reduce required trap reactance`);
      }
      for (let i = 0; i < traps.length; i++) {
        if (traps[i].V_peak > 3000 && P < 500) {
          warnings.push(`⚠ Trap ${i + 1} needs vacuum capacitor at ${P} W (EFHW voltage is severe)`);
        }
      }
      if (BW_kHz[n - 1] < 20 && f[n - 1] < 5) {
        warnings.push(`⚠ Lowest-band BW < 20 kHz — practical operation limited to a narrow window.`);
      }
      if (warnings.length === 0) warnings.push('No specific warnings — design is within practical limits.');
      warnings.forEach(w => rows.push(['', w]));

      return { rows };
    },
    diagram() {
      return `<pre class="aw-diag">  unun
   │
   ●═══ seg 1 ═══[Trap1]═══ seg 2 ═══[Trap2]═══ seg 3 ═══●
   ↑    20m       opens     additional             additional
 5% c/p  half     14 MHz    for 40m                for 80m
 tail    wave

  On 20m: traps act as opens — only seg 1 radiates
  On 40m: trap 2 opens — seg 1 + seg 2 radiate
  On 80m: both traps open — full wire radiates

  SHORTY VARIANT (shorten > 0):
   Segments are physically shorter; traps re-spec'd with higher L
   (lower C) to provide extra inductive loading at lower bands.
   At seg-1 shortening > 10% an inline coil is added inside seg 1.</pre>`;
    },
    notes: [
      'Length-driven design: set Available Length to your wire-length budget; leave it at 0 for full-size.',
      'Build & trim from the unun outward, one band at a time. Higher-frequency traps live closer to the unun.',
      'When shortened, traps get re-spec\'d (higher L, lower C, same f_r) and a seg-1 inline coil may be added.',
      'Trap voltage rating is the dominant constraint — EFHW traps see roughly 2.5× the voltage of dipole traps at the same power. Vacuum caps are common for shorty designs > 100 W.',
      'Counterpoise tail (~5% of innermost segment) on the unun ground side is essential. A 1:1 common-mode choke at the rig end is mandatory.',
      'For shortened designs, expect to use a tuner at the rig for full-band SWR coverage.',
    ],
  },

  // ─── Yagi-Uda ──────────────────────────────────────────────────
  'yagi': {
    name: 'Yagi-Uda',
    section: '09-10',
    inputs: [
      { id: 'freq', label: 'Center frequency (MHz)', type: 'number', step: '0.01', default: 14.175 },
      { id: 'elements', label: 'Total elements (incl. DE + reflector)', type: 'select', default: '3',
        choices: [['2','2 (DE + reflector)'], ['3','3 (DE + R + 1 director)'], ['4','4 (DE + R + 2 directors)'],
                  ['5','5 (DE + R + 3 directors)'], ['6','6 (DE + R + 4 directors)']] },
    ],
    compute(v) {
      const f = parseFloat(v.freq) || 14.175;
      const n = parseInt(v.elements, 10) || 3;
      const lambdaFt = 984 / f;
      // Empirical element lengths (Rothammel / W2PV / NBS optimized averages)
      const deLen = (468 / f) * 0.97;          // driven element ~3% shorter than 1/2-wave (gamma match etc.)
      const reflLen = deLen * 1.05;            // reflector ~5% longer than DE
      const dirShort = [0.95, 0.93, 0.92, 0.91]; // directors progressively shorter
      const directors = [];
      for (let i = 0; i < n - 2; i++) {
        const len = deLen * dirShort[Math.min(i, dirShort.length - 1)];
        directors.push([`Director ${i + 1} length`, `${len.toFixed(2)} ft  (${(len*12).toFixed(1)} in)`]);
      }
      // Spacing: reflector → DE = 0.15-0.2 λ; DE → D1 = 0.10-0.15 λ; subsequent ~0.20 λ
      const spaceRefl = lambdaFt * 0.18;
      const spaceD1   = lambdaFt * 0.12;
      const spaceDir  = lambdaFt * 0.20;
      const spacings = [];
      spacings.push([`Reflector → DE spacing`, `${spaceRefl.toFixed(2)} ft`]);
      if (n >= 3) spacings.push([`DE → Director 1 spacing`, `${spaceD1.toFixed(2)} ft`]);
      for (let i = 2; i < n - 1; i++) {
        spacings.push([`Director ${i-1} → Director ${i} spacing`, `${spaceDir.toFixed(2)} ft`]);
      }
      const boomLen = spaceRefl + (n >= 3 ? spaceD1 : 0) + (n - 3) * spaceDir;
      // Approximate gain (dBi) and F/B (dB) for typical configurations
      const gainTable = { 2: 5.5, 3: 7.5, 4: 9.0, 5: 10.5, 6: 11.5 };
      const fbTable = { 2: 12, 3: 20, 4: 22, 5: 25, 6: 25 };
      return { rows: [
        ['Reflector length',       `${reflLen.toFixed(2)} ft  (${(reflLen*12).toFixed(1)} in)`],
        ['Driven element length',  `${deLen.toFixed(2)} ft  (${(deLen*12).toFixed(1)} in)`],
        ...directors,
        ['Boom length (total)',    `${Math.max(0, boomLen).toFixed(2)} ft`],
        ...spacings,
        ['Expected forward gain',  `~${gainTable[n] || 12} dBi  (~${((gainTable[n] || 12) - 2.15).toFixed(1)} dBd)`],
        ['Expected F/B ratio',     `~${fbTable[n] || 25} dB`],
        ['Expected feed Z',        '~25-35 Ω (gamma match or hairpin to 50 Ω)'],
        ['Match',                  'Gamma match, beta/hairpin match, or 4:1 transformer to 50 Ω coax'],
      ]};
    },
    diagram() {
      return `<pre class="aw-diag">    refl  DE     D1   D2   D3
     │    │      │    │    │
     │    │      │    │    │
     │    │      │    │    │     →  forward direction (toward DX)
     │    │      │    │    │
     │    │      │    │    │
     ●────●──────●────●────●  boom

         feed
          │
        coax to rig

  Reflector is the longest element; directors get progressively shorter.
  Driven element is shortest of the three classic elements.</pre>`;
    },
    notes: [
      'Element lengths are empirical — verify with NEC-2 / 4nec2 modeling for production builds.',
      'Spacing affects gain, F/B, and feed Z — close-spaced gives narrow band but slightly more gain.',
      'Driven-element length given includes ~3% shortening typical with a gamma match. Adjust for your match type.',
      'Boom must be insulated from elements (or all-aluminum with elements through the boom — both OK).',
      "Yagis typically need a balun (4:1 hairpin or matching transformer) to bring 25-35 Ω feed Z to 50 Ω.",
    ],
  },

  // ─── Magnetic loop ─────────────────────────────────────────────
  'mag-loop': {
    name: 'Magnetic Loop',
    section: '09-14',
    inputs: [
      { id: 'freq', label: 'Operating frequency (MHz)', type: 'number', step: '0.01', default: 14.175 },
      { id: 'diam', label: 'Loop diameter (ft)', type: 'number', step: '0.1', default: 3.0,
        hint: 'Larger loop = higher efficiency but lower Q. 0.05λ - 0.25λ is typical (50% range)' },
      { id: 'cond', label: 'Conductor diameter (in)', type: 'number', step: '0.125', default: 0.5,
        hint: '½" copper pipe is typical. Larger pipe = lower loss = higher efficiency.' },
      { id: 'power', label: 'Transmit power (W)', type: 'number', step: '1', default: 25 },
    ],
    compute(v) {
      const f = parseFloat(v.freq) || 14.175;
      const dFt = parseFloat(v.diam) || 3;
      const condIn = parseFloat(v.cond) || 0.5;
      const power = parseFloat(v.power) || 25;
      const dM = dFt * 0.3048;
      const condM = condIn * 0.0254;
      // Loop circumference and area
      const circumM = Math.PI * dM;
      const areaM2 = Math.PI * (dM / 2) ** 2;
      // Inductance (Wheeler approx for single-turn loop, conductor a, loop radius b)
      const a = condM / 2;
      const b = dM / 2;
      const inductanceH = 4e-7 * Math.PI * b * (Math.log(8 * b / a) - 2);
      const inductanceUH = inductanceH * 1e6;
      // Required tuning capacitance: f = 1/(2π√LC) → C = 1/(4π² f² L)
      const reqC_F = 1 / (4 * Math.PI * Math.PI * (f * 1e6) ** 2 * inductanceH);
      const reqC_pF = reqC_F * 1e12;
      // Q estimate (rough: Q ~ X_L / R_loss; R_loss dominated by conductor skin effect)
      const X_L = 2 * Math.PI * f * 1e6 * inductanceH;
      const skinR = 0.0826 * Math.sqrt(f) / (condIn / 0.039);   // empirical for copper, ohms/ft
      const R_loss = skinR * (circumM / 0.3048);                 // total
      const Q = Math.min(800, X_L / Math.max(R_loss, 0.01));
      // Voltage at capacitor: V = √(P × Q × X_L)
      const V_cap = Math.sqrt(power * Q * X_L);
      // 3-dB bandwidth at resonance
      const bw_kHz = (f * 1000) / Q;
      // Circumference fraction of wavelength (efficiency indicator)
      const lambdaM = 300 / f;
      const cFrac = circumM / lambdaM;
      const efficiency = cFrac < 0.1 ? 'poor' : cFrac < 0.2 ? 'good' : cFrac < 0.3 ? 'excellent' : 'over-size';
      return { rows: [
        ['Loop circumference',         `${circumM.toFixed(2)} m  (${(circumM/0.3048).toFixed(2)} ft)`],
        ['Circumference / wavelength', `${cFrac.toFixed(3)}  (${efficiency})`],
        ['Loop inductance',            `${inductanceUH.toFixed(2)} µH`],
        ['Tuning capacitance needed',  `${reqC_pF.toFixed(1)} pF`],
        ['Estimated Q',                `${Q.toFixed(0)}`],
        ['3-dB bandwidth',             `${bw_kHz.toFixed(1)} kHz`],
        ['Inductive reactance X_L',    `${X_L.toFixed(0)} Ω`],
        ['Voltage across capacitor',   `${V_cap.toFixed(0)} V peak  (at ${power} W)`],
        ['Capacitor voltage rating',   `≥ ${(V_cap * 1.5).toFixed(0)} V (safety margin) → vacuum cap recommended`],
      ]};
    },
    diagram() {
      return `<pre class="aw-diag">         ╭─────────────╮
        ╱               ╲
       ╱                 ╲
      │                   │
      │                   │
      │     [tuning C]    │  ← high-V variable capacitor (vacuum)
      │      ┌───┐        │
       ╲    ─┤   ├─      ╱
        ╲   ─┤   ├─     ╱
         ╰───┴───┴─────╯
              ↑
         coupling loop
         (~⅕ of main loop dia.)
              │
            coax to rig

  Tune by varying the capacitor — each band needs a different C value.
  Coupling loop adjusts feed Z to ~50 Ω.</pre>`;
    },
    notes: [
      'Mag loops are extremely narrow-banded — Q of 200-500 means 3-dB BW around 30-70 kHz on 20m.',
      'Voltages across the tuning cap exceed several kV at moderate power — vacuum capacitor required for >50 W.',
      'Efficiency drops sharply below 0.1λ circumference; aim for 0.15-0.25λ for best results.',
      'Coupling loop ~⅕ the main-loop diameter is a good starting point; trim for SWR.',
      'Loop should be 6-10 ft above ground for cleanest pattern.',
    ],
  },

  // ─── Trap design (component, not in AW_ANTENNAS) ───────────────
  'trap-design': {
    name: 'Trap Design',
    section: '09-13',
    component: true,
    inputs: [
      { id: 'freq', label: 'Trap resonant frequency (MHz)', type: 'number', step: '0.01', default: 14.150,
        hint: 'Set this to the highest band the trap should isolate (e.g. 14 MHz for an 80/40/20 trap dipole).' },
      { id: 'capPF', label: 'Capacitance choice (pF)', type: 'number', step: '1', default: 100,
        hint: 'Pick a doorknob or vacuum cap on hand. 50-200 pF is typical. Smaller C → larger L (more turns).' },
      { id: 'power', label: 'Operating power (W PEP)', type: 'number', step: '1', default: 100 },
      { id: 'formIn', label: 'Coil form diameter (in)', type: 'number', step: '0.125', default: 1.0,
        hint: 'Clear PVC pipe is common. 1" - 2" diameter, close-wound.' },
      { id: 'wireGauge', label: 'Wire gauge (AWG)', type: 'select', default: '14',
        choices: [['12','#12 (heavy, low loss)'], ['14','#14 (standard)'], ['16','#16 (lighter)']] },
    ],
    compute(v) {
      const f = parseFloat(v.freq) || 14.150;
      const capPF = parseFloat(v.capPF) || 100;
      const power = parseFloat(v.power) || 100;
      const formIn = parseFloat(v.formIn) || 1.0;
      const wireAwg = parseInt(v.wireGauge, 10) || 14;
      // Required L from f = 1/(2π√LC)
      const capF = capPF * 1e-12;
      const inductanceH = 1 / (4 * Math.PI * Math.PI * (f * 1e6) ** 2 * capF);
      const inductanceUH = inductanceH * 1e6;
      // Reactance at resonance (for voltage estimate)
      const X_L = 2 * Math.PI * f * 1e6 * inductanceH;
      // Trap Q (typical antenna trap Q is 50-150)
      const trapQ = 100;
      // Voltage across capacitor at PEP
      const V_cap = Math.sqrt(power * trapQ * X_L);
      // Coil dimensions: Wheeler formula for single-layer air-core inductor
      // L (µH) = (r² × N²) / (9r + 10ℓ)  where r = radius (in), ℓ = length (in)
      // Solve for N given r, target L; then ℓ = N × wire diameter
      const r = formIn / 2;
      const wireDia = wireAwg === 12 ? 0.0808 : wireAwg === 14 ? 0.0641 : 0.0508;  // bare AWG diameter, in
      // Iterate to find N (since ℓ depends on N)
      let N = 5, prevN = 0;
      for (let iter = 0; iter < 30 && Math.abs(N - prevN) > 0.01; iter++) {
        prevN = N;
        const ell = N * wireDia;
        N = Math.sqrt(inductanceUH * (9 * r + 10 * ell) / (r * r));
      }
      const turns = Math.ceil(N * 10) / 10;
      const coilLength = turns * wireDia;
      const wireLengthFt = (turns * Math.PI * formIn) / 12;
      return { rows: [
        ['Required inductance', `${inductanceUH.toFixed(2)} µH`],
        ['Reactance at resonance (X_L = X_C)', `${X_L.toFixed(0)} Ω`],
        ['Capacitor voltage rating needed', `≥ ${(V_cap * 1.5).toFixed(0)} V peak  (at ${power} W PEP, Q≈${trapQ})`],
        ['Capacitor type recommendation', V_cap > 2000 ? 'Vacuum or doorknob ceramic (5 kV+)' : V_cap > 500 ? 'Doorknob ceramic (3 kV+)' : 'Mica or ceramic disc (1 kV)'],
        ['Coil turns', `${turns.toFixed(1)} turns of #${wireAwg} on ${formIn}" form`],
        ['Coil length', `${coilLength.toFixed(2)} in close-wound`],
        ['Wire length', `${wireLengthFt.toFixed(1)} ft  (plus 6" leads)`],
        ['Trap Q (typical)', `~${trapQ}`],
      ]};
    },
    diagram() {
      return `<pre class="aw-diag">          C (capacitor)
            ┌─┤├─┐
            │    │
            │    │
            │    │
   wire ────┴────┴──── wire
                │
              L (coil)

   Schematic — at resonance (f₀ = 1/(2π√LC)) the trap is a high-Z open.
   Above f₀, looks capacitive (X_C dominates).
   Below f₀, looks inductive (X_L dominates).</pre>`;
    },
    notes: [
      'Pick a capacitor first (high voltage rating matters more than precision); compute coil to match.',
      'For legal-limit operation, capacitor voltage can exceed 5 kV — use vacuum capacitors.',
      'Wind the coil close-wound (turns touching) for predictable inductance from the formula.',
      'Trap loss (typically 0.3-1 dB per trap) reduces antenna efficiency — count traps in your loss budget.',
      'Verify trap resonance with a NanoVNA or dip meter BEFORE installing — much easier on the bench.',
    ],
  },

  // ─── Loading coil for shortened antennas (component) ────────────
  'loading-coil': {
    name: 'Loading Coil',
    section: '09-12',
    component: true,
    inputs: [
      { id: 'freq', label: 'Operating frequency (MHz)', type: 'number', step: '0.01', default: 7.150 },
      { id: 'physLen', label: 'Antenna physical length (ft)', type: 'number', step: '0.5', default: 33,
        hint: 'Length of the (shortened) antenna. For a dipole, this is total tip-to-tip; for a vertical, full length.' },
      { id: 'antType', label: 'Antenna type', type: 'select', default: 'dipole',
        choices: [['dipole','Dipole (half-wave)'], ['vertical','Vertical (quarter-wave)']] },
      { id: 'position', label: 'Coil position', type: 'select', default: 'center',
        choices: [['base','Base loaded (lossiest, easiest)'],
                  ['center','Center loaded (best efficiency)'],
                  ['top','Top loaded (highest efficiency, hardest physically)']] },
    ],
    compute(v) {
      const f = parseFloat(v.freq) || 7.150;
      const physFt = parseFloat(v.physLen) || 33;
      const isDipole = v.antType === 'dipole';
      const fullFt = isDipole ? 468 / f : 234 / f;
      const shortenFrac = physFt / fullFt;
      if (shortenFrac >= 0.97) {
        return { rows: [['Result', `Antenna is ${(shortenFrac*100).toFixed(0)}% of full length — no significant loading needed.`]] };
      }
      // Reactance to add — depends on position
      // Simplified model: for center-loaded shortened dipole, missing X = -j Z₀ × cot(π×physFt/fullFt × π/2) approximately
      // For quick estimate, use: X_load ≈ Z_0 × tan((π/2) × (1 - shortenFrac))
      // Position factor: base ~3×, center ~1× (best), top ~0.5× (least L needed but high stress)
      const Z0 = isDipole ? 50 : 36;  // approx feed impedance reference
      const xRatio = Math.tan((Math.PI / 2) * (1 - shortenFrac));
      let positionFactor = 1.0;
      let efficiency = 'good';
      let loss = '~1 dB';
      if (v.position === 'base') { positionFactor = 3.0; efficiency = 'poor'; loss = '~3-5 dB'; }
      if (v.position === 'top')  { positionFactor = 0.5; efficiency = 'excellent'; loss = '<0.5 dB'; }
      const X_required = Z0 * xRatio * positionFactor;
      const inductanceH = X_required / (2 * Math.PI * f * 1e6);
      const inductanceUH = inductanceH * 1e6;
      // Per-side (dipole has 2; vertical has 1)
      const perSide = isDipole ? inductanceUH / 2 : inductanceUH;
      // Suggested coil dimensions: 2" form, #14 wire, close-wound
      const formIn = 2;
      const wireDia = 0.0641;
      const r = formIn / 2;
      let N = 10, prevN = 0;
      for (let iter = 0; iter < 30 && Math.abs(N - prevN) > 0.01; iter++) {
        prevN = N;
        const ell = N * wireDia;
        N = Math.sqrt(perSide * (9 * r + 10 * ell) / (r * r));
      }
      const turns = Math.ceil(N * 10) / 10;
      const coilLength = turns * wireDia;
      return { rows: [
        ['Full-size length (no loading)', `${fullFt.toFixed(1)} ft`],
        ['Physical length (input)',       `${physFt.toFixed(1)} ft  (${(shortenFrac*100).toFixed(0)}%)`],
        ['Reactance to add',              `${X_required.toFixed(0)} Ω total`],
        ['Total loading inductance',      `${inductanceUH.toFixed(2)} µH`],
        ['Inductance per side',           `${perSide.toFixed(2)} µH ${isDipole ? '(× 2 — one in each leg)' : '(single coil)'}`],
        ['Suggested coil',                `${turns.toFixed(1)} turns of #14 on 2" form, close-wound (~${coilLength.toFixed(2)}" long)`],
        ['Position',                      `${v.position} loaded — efficiency: ${efficiency}, est. loss: ${loss}`],
        ['Practical Q',                   v.position === 'base' ? '50-100 (lossy in mobile mounts)' : '150-300 (air-core)'],
      ]};
    },
    diagram() {
      const isDipole = false;  // hard-coded diagram for vertical case
      return `<pre class="aw-diag">  Center-loaded vertical:                    Base-loaded vertical:

       │                                    │
       │  ← upper element                   │  ← full element
       │                                    │
       ●  ← loading coil                    │
       │                                    │
       │  ← lower element                   ●  ← loading coil at base
       │                                    │
   ────●────────                         ───●────────
   ground                                 ground

  Center loading is more efficient (more current flows in the
  full upper section before the loading coil reduces it).
  Base loading is easier mechanically but lossier.</pre>`;
    },
    notes: [
      'Loading restores resonance by adding reactance equal to what was lost from shortening.',
      'Position trade-off: base = simple but lossy; center = best efficiency; top = best efficiency, mechanical challenge.',
      'For a dipole, put a coil in EACH leg (half the inductance per side, both sides).',
      'Loading coil Q matters — air-core ~150-300 vs. iron core ~50-100. Higher Q = less loss.',
      'Bandwidth narrows in proportion to shortening — a 50% shortened antenna has ~1/4 the bandwidth.',
    ],
  },

  // ════════════════════════════════════════════════════════════════
  //  FORMULA CALCULATORS  — one per card in J-Learn ch 18 (Formulas).
  //  Each entry has formula:true so awCalcRenderList puts it in the
  //  "Formulas" sidebar section.
  // ════════════════════════════════════════════════════════════════

  'ohms-law': {
    name: "Ohm's Law",
    section: '17-01',
    formula: true,
    inputs: [
      { id: 'mode', label: 'Solve for', type: 'select', default: 'V',
        choices: [['V', 'Voltage (given I, R)'], ['I', 'Current (given V, R)'],
                  ['R', 'Resistance (given V, I)']] },
      { id: 'V', label: 'Voltage V (volts)',  type: 'number', step: '0.001', default: 13.8 },
      { id: 'I', label: 'Current I (amps)',   type: 'number', step: '0.001', default: 22 },
      { id: 'R', label: 'Resistance R (ohms)', type: 'number', step: '0.001', default: 0.016 },
    ],
    compute(v) {
      const V = parseFloat(v.V), I = parseFloat(v.I), R = parseFloat(v.R);
      let result, formula;
      if (v.mode === 'V') { result = I * R; formula = 'V = I × R'; }
      else if (v.mode === 'I') { result = V / R; formula = 'I = V / R'; }
      else { result = V / I; formula = 'R = V / I'; }
      const P = V * I;   // always show power as a bonus
      return { rows: [
        ['Formula',           formula],
        ['Result',            v.mode === 'V' ? `V = ${result.toFixed(3)} volts`
                            : v.mode === 'I' ? `I = ${result.toFixed(3)} amps`
                            :                  `R = ${result.toFixed(3)} ohms`],
        ['Power (V × I)',     `${P.toFixed(2)} W  (using current input values)`],
        ['Power (I²R)',       `${(I * I * R).toFixed(2)} W`],
        ['Power (V²/R)',      `${(V * V / R).toFixed(2)} W`],
      ]};
    },
    notes: [
      "Use this with all three forms: V=IR, I=V/R, R=V/I. Power forms (P=VI=I²R=V²/R) are shown as cross-checks.",
      "Voltage drop in mobile DC cables is the classic ham use — pick I and R, solve for V_drop.",
      "For AC with reactance, use Impedance (§15-04) instead.",
    ],
  },

  'power-law': {
    name: 'Power Law',
    section: '17-02',
    formula: true,
    inputs: [
      { id: 'mode', label: 'Solve for', type: 'select', default: 'P_VI',
        choices: [
          ['P_VI', 'Power from V and I'],
          ['P_IR', 'Power from I and R'],
          ['P_VR', 'Power from V and R'],
          ['V_PR', 'Voltage from P and R'],
          ['I_PR', 'Current from P and R'],
        ]},
      { id: 'V', label: 'Voltage V (volts)', type: 'number', step: '0.001', default: 70.7 },
      { id: 'I', label: 'Current I (amps)',  type: 'number', step: '0.001', default: 1.41 },
      { id: 'R', label: 'Resistance R (ohms)', type: 'number', step: '0.001', default: 50 },
      { id: 'P', label: 'Power P (watts)',     type: 'number', step: '0.01',  default: 100 },
    ],
    compute(v) {
      const V = parseFloat(v.V), I = parseFloat(v.I), R = parseFloat(v.R), P = parseFloat(v.P);
      let result, formula, label;
      if (v.mode === 'P_VI') { result = V * I;        formula = 'P = V × I';        label = 'P (W)'; }
      else if (v.mode === 'P_IR') { result = I * I * R; formula = 'P = I² × R';     label = 'P (W)'; }
      else if (v.mode === 'P_VR') { result = V * V / R; formula = 'P = V² / R';     label = 'P (W)'; }
      else if (v.mode === 'V_PR') { result = Math.sqrt(P * R); formula = 'V = √(P · R)'; label = 'V (V)'; }
      else                        { result = Math.sqrt(P / R); formula = 'I = √(P / R)'; label = 'I (A)'; }
      return { rows: [
        ['Formula', formula],
        ['Result',  `${label}: ${result.toFixed(3)}`],
        ['Cross-check (P = V·I)', `${(V * I).toFixed(2)} W`],
        ['Cross-check (P = I²R)', `${(I * I * R).toFixed(2)} W`],
        ['Cross-check (P = V²/R)', `${(V * V / R).toFixed(2)} W`],
      ]};
    },
    notes: [
      'Use RMS for AC. RMS = Peak / √2 for sine waves.',
      'For SSB, peak power (PEP) is the maximum envelope; average is much lower (~25-40 W avg for 100 W PEP voice).',
      'Resistor wattage rating: derate to 50%. A 100 W resistor running at 100 W is failing.',
    ],
  },

  'reactance': {
    name: 'Reactance (X_L, X_C)',
    section: '17-03',
    formula: true,
    inputs: [
      { id: 'mode', label: 'Type', type: 'select', default: 'L',
        choices: [['L', 'Inductive (X_L = 2πfL)'], ['C', 'Capacitive (X_C = 1/(2πfC))']] },
      { id: 'f', label: 'Frequency f (MHz)', type: 'number', step: '0.001', default: 14.150 },
      { id: 'L', label: 'Inductance L (µH)', type: 'number', step: '0.001', default: 5 },
      { id: 'C', label: 'Capacitance C (pF)', type: 'number', step: '0.1', default: 50 },
    ],
    compute(v) {
      const f = parseFloat(v.f) * 1e6;          // Hz
      const L = parseFloat(v.L) * 1e-6;          // H
      const C = parseFloat(v.C) * 1e-12;         // F
      const X_L = 2 * Math.PI * f * L;
      const X_C = 1 / (2 * Math.PI * f * C);
      const X = v.mode === 'L' ? X_L : X_C;
      const formula = v.mode === 'L' ? 'X_L = 2π · f · L' : 'X_C = 1 / (2π · f · C)';
      return { rows: [
        ['Formula', formula],
        ['Result',  `${v.mode === 'L' ? 'X_L' : 'X_C'} = ${X.toFixed(2)} Ω`],
        ['X_L (cross)', `${X_L.toFixed(2)} Ω`],
        ['X_C (cross)', `${X_C.toFixed(2)} Ω`],
        ['X_L − X_C (net X)', `${(X_L - X_C).toFixed(2)} Ω  (${X_L > X_C ? 'inductive' : X_L < X_C ? 'capacitive' : 'resonant'})`],
      ]};
    },
    notes: [
      'Convert MHz → Hz, µH → H, pF → F before plugging in. Calculator handles this for you.',
      'Reactance does NOT dissipate power — only resistance does.',
      'When X_L = X_C the circuit is resonant (see §15-05).',
    ],
  },

  'impedance': {
    name: 'Impedance |Z| / phase',
    section: '17-04',
    formula: true,
    inputs: [
      { id: 'R',  label: 'Resistance R (Ω)',  type: 'number', step: '0.1', default: 65 },
      { id: 'X',  label: 'Reactance X (Ω, signed: + = ind, − = cap)', type: 'number', step: '0.1', default: 28 },
      { id: 'Z0', label: 'Reference Z₀ (Ω)',  type: 'number', step: '1',   default: 50 },
    ],
    compute(v) {
      const R = parseFloat(v.R), X = parseFloat(v.X), Z0 = parseFloat(v.Z0);
      const magZ = Math.sqrt(R * R + X * X);
      const phase = Math.atan2(X, R) * 180 / Math.PI;
      // Reflection coefficient Γ
      const num_r = R - Z0, num_i = X;
      const den_r = R + Z0, den_i = X;
      const den_mag2 = den_r * den_r + den_i * den_i;
      const G_r = (num_r * den_r + num_i * den_i) / den_mag2;
      const G_i = (num_i * den_r - num_r * den_i) / den_mag2;
      const G_mag = Math.sqrt(G_r * G_r + G_i * G_i);
      const G_phase = Math.atan2(G_i, G_r) * 180 / Math.PI;
      const swr = G_mag >= 1 ? Infinity : (1 + G_mag) / (1 - G_mag);
      return { rows: [
        ['Z = R + jX',       `${R.toFixed(2)} + j${X.toFixed(2)} Ω`],
        ['|Z|',              `${magZ.toFixed(2)} Ω`],
        ['Phase ∠Z',         `${phase.toFixed(2)}°  (${X > 0 ? 'inductive' : X < 0 ? 'capacitive' : 'resistive'})`],
        ['Reflection |Γ|',   `${G_mag.toFixed(4)}  ∠ ${G_phase.toFixed(1)}°`],
        ['SWR (vs Z₀)',      isFinite(swr) ? swr.toFixed(2) + ':1' : '∞ (open / short)'],
      ]};
    },
    notes: [
      'X is signed: + for inductive, − for capacitive. The display tells you which.',
      "Pure resistance (X=0) gives SWR = R/Z₀ when R > Z₀, or Z₀/R when R < Z₀.",
      'See §15-13 for the Smith chart calc using these same Γ values.',
    ],
  },

  'resonance': {
    name: 'Resonant Frequency',
    section: '17-05',
    formula: true,
    inputs: [
      { id: 'mode', label: 'Solve for', type: 'select', default: 'f',
        choices: [['f','f from L and C'], ['L','L from f and C'], ['C','C from f and L']] },
      { id: 'L', label: 'Inductance L (µH)',  type: 'number', step: '0.01', default: 5 },
      { id: 'C', label: 'Capacitance C (pF)', type: 'number', step: '1',    default: 50 },
      { id: 'f', label: 'Frequency f (MHz)',  type: 'number', step: '0.01', default: 10.07 },
    ],
    compute(v) {
      const L = parseFloat(v.L) * 1e-6;
      const C = parseFloat(v.C) * 1e-12;
      const f = parseFloat(v.f) * 1e6;
      let result, formula, label;
      if (v.mode === 'f') {
        result = 1 / (2 * Math.PI * Math.sqrt(L * C)) / 1e6;
        formula = 'f = 1 / (2π · √(L · C))';
        label = 'f (MHz)';
      } else if (v.mode === 'L') {
        result = 1 / (4 * Math.PI * Math.PI * f * f * C) * 1e6;
        formula = 'L = 1 / (4π² · f² · C)';
        label = 'L (µH)';
      } else {
        result = 1 / (4 * Math.PI * Math.PI * f * f * L) * 1e12;
        formula = 'C = 1 / (4π² · f² · L)';
        label = 'C (pF)';
      }
      return { rows: [
        ['Formula', formula],
        ['Result',  `${label}: ${result.toFixed(3)}`],
        ['Quick form (MHz · µH · pF)', 'f(MHz) = 159.15 / √(L(µH)·C(pF))'],
      ]};
    },
    notes: [
      'At resonance X_L = X_C — they cancel and the circuit is purely resistive.',
      'High Q narrows the resonance peak; low Q broadens it (see §15-11).',
      'Use this to size traps (§07-13) and tank circuits.',
    ],
  },

  'wavelength': {
    name: 'Wavelength',
    section: '17-06',
    formula: true,
    inputs: [
      { id: 'f', label: 'Frequency f (MHz)', type: 'number', step: '0.001', default: 14.150 },
      { id: 'vf', label: 'Velocity factor (1.0 = free space)', type: 'number', step: '0.01', default: 1.0,
        hint: 'For coax, use the cable\'s VF (RG-58 ≈ 0.66, LMR-400 ≈ 0.85, ladder line ≈ 0.91).' },
    ],
    compute(v) {
      const f = parseFloat(v.f);
      const vf = parseFloat(v.vf);
      const lambda_m = (300 / f) * vf;
      const lambda_ft = lambda_m / 0.3048;
      const halfDipole_ft = (468 / f);    // empirical end-effect, free space wire
      const quarter_ft = halfDipole_ft / 2;
      return { rows: [
        ['Wavelength λ',         `${lambda_m.toFixed(3)} m  (${lambda_ft.toFixed(2)} ft)`],
        ['λ/2',                  `${(lambda_m / 2).toFixed(3)} m  (${(lambda_ft / 2).toFixed(2)} ft)`],
        ['λ/4',                  `${(lambda_m / 4).toFixed(3)} m  (${(lambda_ft / 4).toFixed(2)} ft)`],
        ['½-wave dipole (in air, end-effect)', `${halfDipole_ft.toFixed(2)} ft  (k=468)`],
        ['¼-wave vertical (in air)',           `${quarter_ft.toFixed(2)} ft  (k=234)`],
      ]};
    },
    notes: [
      'Free-space λ(m) = 300 / f(MHz). For coax / ladder line, multiply by VF.',
      'Half-wave dipole length = 468 / f(MHz) ft accounts for ~5% end-effect on thin wire.',
      'Quarter-wave vertical = 234 / f(MHz) ft (same correction, halved).',
    ],
  },

  'swr': {
    name: 'SWR',
    section: '17-07',
    formula: true,
    inputs: [
      { id: 'mode', label: 'Compute from', type: 'select', default: 'pwr',
        choices: [['pwr', 'Forward / reflected power'], ['z', 'Load impedance Z_L vs Z₀']] },
      { id: 'Pf', label: 'Forward power P_f (W)', type: 'number', step: '0.1', default: 100 },
      { id: 'Pr', label: 'Reflected power P_r (W)', type: 'number', step: '0.01', default: 4 },
      { id: 'R',  label: 'Load R (Ω)', type: 'number', step: '0.1', default: 75 },
      { id: 'X',  label: 'Load X (Ω, signed)', type: 'number', step: '0.1', default: 0 },
      { id: 'Z0', label: 'Z₀ (Ω)', type: 'number', step: '1', default: 50 },
    ],
    compute(v) {
      let G_mag, swr, formula;
      if (v.mode === 'pwr') {
        const Pf = parseFloat(v.Pf), Pr = parseFloat(v.Pr);
        G_mag = Math.sqrt(Pr / Pf);
        formula = '|Γ| = √(P_r / P_f);  SWR = (1 + |Γ|) / (1 − |Γ|)';
      } else {
        const R = parseFloat(v.R), X = parseFloat(v.X), Z0 = parseFloat(v.Z0);
        const num_r = R - Z0, num_i = X;
        const den_r = R + Z0, den_i = X;
        const den_mag2 = den_r * den_r + den_i * den_i;
        const G_r = (num_r * den_r + num_i * den_i) / den_mag2;
        const G_i = (num_i * den_r - num_r * den_i) / den_mag2;
        G_mag = Math.sqrt(G_r * G_r + G_i * G_i);
        formula = 'Γ = (Z_L − Z₀)/(Z_L + Z₀);  SWR = (1 + |Γ|)/(1 − |Γ|)';
      }
      swr = G_mag >= 1 ? Infinity : (1 + G_mag) / (1 - G_mag);
      // Mismatch loss in dB
      const refPct = 100 * G_mag * G_mag;
      const mismatchLoss = G_mag >= 1 ? Infinity : -10 * Math.log10(1 - G_mag * G_mag);
      return { rows: [
        ['Formula',         formula],
        ['Reflection |Γ|',  `${G_mag.toFixed(4)}`],
        ['SWR',             isFinite(swr) ? `${swr.toFixed(2)}:1` : '∞:1 (full reflection)'],
        ['Reflected power', `${refPct.toFixed(1)}%`],
        ['Mismatch loss',   isFinite(mismatchLoss) ? `${mismatchLoss.toFixed(2)} dB` : '∞'],
      ]};
    },
    notes: [
      'SWR ≤ 1.5 is excellent; ≤ 2 is good; ≤ 3 is acceptable for most rigs.',
      'Mismatch loss column: power lost vs. power available with perfect match.',
      'On a lossy feedline, SWR at the rig reads lower than at the antenna — the cable hides mismatch.',
    ],
  },

  'erp': {
    name: 'ERP / EIRP',
    section: '17-08',
    formula: true,
    inputs: [
      { id: 'P_TX',  label: 'Transmitter power P_TX (W)', type: 'number', step: '1',   default: 100 },
      { id: 'gain',  label: 'Antenna gain',                type: 'number', step: '0.1', default: 6 },
      { id: 'unit',  label: 'Gain reference',              type: 'select', default: 'dBd',
        choices: [['dBd','dBd (vs dipole)'], ['dBi','dBi (vs isotropic)'], ['lin','linear ratio']] },
      { id: 'loss',  label: 'Feedline loss (dB)',          type: 'number', step: '0.1', default: 1 },
    ],
    compute(v) {
      const P = parseFloat(v.P_TX);
      const g = parseFloat(v.gain);
      const loss_dB = parseFloat(v.loss);
      const G_dBi = v.unit === 'dBd' ? g + 2.15
                  : v.unit === 'dBi' ? g
                  :                   10 * Math.log10(g);
      const G_dBd = G_dBi - 2.15;
      const G_lin = Math.pow(10, G_dBi / 10);
      const L_lin = Math.pow(10, -loss_dB / 10);
      const EIRP = P * G_lin * L_lin;
      const ERP  = EIRP / Math.pow(10, 2.15 / 10);
      const EIRP_dBW = 10 * Math.log10(EIRP);
      const EIRP_dBm = 10 * Math.log10(EIRP * 1000);
      return { rows: [
        ['Antenna gain',    `${G_dBi.toFixed(2)} dBi  (${G_dBd.toFixed(2)} dBd, ×${G_lin.toFixed(2)})`],
        ['Feedline factor', `×${L_lin.toFixed(3)}  (lose ${(100 * (1 - L_lin)).toFixed(1)}%)`],
        ['EIRP',            `${EIRP.toFixed(1)} W  (${EIRP_dBW.toFixed(1)} dBW, ${EIRP_dBm.toFixed(1)} dBm)`],
        ['ERP (vs dipole)', `${ERP.toFixed(1)} W`],
      ]};
    },
    notes: [
      'EIRP uses dBi (isotropic) reference; ERP uses dBd (dipole). Differ by 2.15 dB.',
      'For RF-safety / FCC compliance use EIRP — see §15-14 RF Exposure.',
      "PEP is the peak; for averaged exposure (FCC), use average power ≈ PEP × duty cycle × mode factor.",
    ],
  },

  'feedline-loss': {
    name: 'Feedline Loss',
    section: '17-09',
    formula: true,
    inputs: [
      { id: 'cable', label: 'Cable type', type: 'select', default: 'lmr400',
        choices: [
          ['rg58',   'RG-58 (foam)'],
          ['rg58a',  'RG-58A (PVC)'],
          ['rg8x',   'RG-8X'],
          ['rg213',  'RG-213 (foam)'],
          ['lmr400', 'LMR-400'],
          ['lmr600', 'LMR-600'],
          ['heliax', '7/8" Heliax (LDF5-50A)'],
        ]},
      { id: 'len_ft', label: 'Length (ft)',     type: 'number', step: '1',    default: 100 },
      { id: 'f',      label: 'Frequency (MHz)', type: 'number', step: '0.01', default: 14.150 },
      { id: 'P_in',   label: 'Power into line (W)', type: 'number', step: '1', default: 100 },
    ],
    compute(v) {
      // dB / 100 ft at canonical frequencies; piecewise log-interpolated for any f.
      const TBL = {
        rg58:   { 1.8: 0.4, 7: 0.9, 14: 1.3, 28: 1.9, 50: 2.5, 144: 4.6, 432: 8.4, 1300: 16.5 },
        rg58a:  { 1.8: 0.5, 7: 1.1, 14: 1.6, 28: 2.4, 50: 3.3, 144: 6.0, 432: 11.4, 1300: 22.0 },
        rg8x:   { 1.8: 0.3, 7: 0.6, 14: 0.9, 28: 1.3, 50: 1.7, 144: 3.1, 432: 5.7, 1300: 11.0 },
        rg213:  { 1.8: 0.2, 7: 0.4, 14: 0.6, 28: 0.8, 50: 1.1, 144: 1.9, 432: 3.4, 1300: 6.8 },
        lmr400: { 1.8: 0.1, 7: 0.3, 14: 0.4, 28: 0.6, 50: 0.8, 144: 1.4, 432: 2.6, 1300: 4.8 },
        lmr600: { 1.8: 0.1, 7: 0.2, 14: 0.3, 28: 0.4, 50: 0.5, 144: 0.9, 432: 1.7, 1300: 3.2 },
        heliax: { 1.8: 0.05, 7: 0.1, 14: 0.15, 28: 0.2, 50: 0.3, 144: 0.5, 432: 1.0, 1300: 1.9 },
      };
      const row = TBL[v.cable] || TBL.lmr400;
      const f = parseFloat(v.f);
      const keys = Object.keys(row).map(Number).sort((a, b) => a - b);
      // log-interpolate between bracketing entries
      let dbPer100;
      if (f <= keys[0]) dbPer100 = row[keys[0]];
      else if (f >= keys[keys.length - 1]) dbPer100 = row[keys[keys.length - 1]];
      else {
        for (let i = 0; i < keys.length - 1; i++) {
          if (f >= keys[i] && f <= keys[i + 1]) {
            const lo = keys[i], hi = keys[i + 1];
            const lr = Math.log10(lo), hr = Math.log10(hi), fr = Math.log10(f);
            const t = (fr - lr) / (hr - lr);
            dbPer100 = row[lo] + t * (row[hi] - row[lo]);
            break;
          }
        }
      }
      const len_ft = parseFloat(v.len_ft);
      const lossDb = dbPer100 * len_ft / 100;
      const factor = Math.pow(10, -lossDb / 10);
      const P_in = parseFloat(v.P_in);
      const P_out = P_in * factor;
      const P_lost = P_in - P_out;
      return { rows: [
        ['Cable loss spec', `${dbPer100.toFixed(2)} dB / 100 ft @ ${f.toFixed(2)} MHz`],
        ['Total loss',      `${lossDb.toFixed(2)} dB`],
        ['Power factor',    `×${factor.toFixed(3)}  (${(factor * 100).toFixed(1)}% delivered)`],
        ['Power delivered', `${P_out.toFixed(1)} W`],
        ['Power lost',      `${P_lost.toFixed(1)} W (heats the cable)`],
      ]};
    },
    notes: [
      'Loss roughly doubles when frequency quadruples — VHF needs better cable than HF.',
      "Mismatch on a lossy line ADDS to matched loss; see §15-07 SWR for the penalty calc.",
      'Cable specs vary slightly by manufacturer; this calculator uses canonical values.',
    ],
  },

  'decibels': {
    name: 'Decibels',
    section: '17-10',
    formula: true,
    inputs: [
      { id: 'mode', label: 'Convert', type: 'select', default: 'lin2db',
        choices: [
          ['lin2db', 'Linear ratio → dB (power)'],
          ['db2lin', 'dB → linear ratio (power)'],
          ['lin2db_v', 'Voltage ratio → dB'],
          ['watt2dbm','Watts → dBm'],
          ['dbm2watt','dBm → Watts'],
        ]},
      { id: 'val',  label: 'Input value', type: 'number', step: 'any', default: 100 },
    ],
    compute(v) {
      const x = parseFloat(v.val);
      let result, formula, unit;
      if (v.mode === 'lin2db')   { result = 10 * Math.log10(x);             formula = 'dB = 10 · log₁₀(P₂/P₁)'; unit = 'dB'; }
      else if (v.mode === 'db2lin') { result = Math.pow(10, x / 10);         formula = 'ratio = 10^(dB/10)';      unit = '×';  }
      else if (v.mode === 'lin2db_v') { result = 20 * Math.log10(x);          formula = 'dB = 20 · log₁₀(V₂/V₁)'; unit = 'dB'; }
      else if (v.mode === 'watt2dbm') { result = 10 * Math.log10(x * 1000);   formula = 'dBm = 10 · log₁₀(W·1000)'; unit = 'dBm'; }
      else                            { result = Math.pow(10, x / 10) / 1000; formula = 'W = 10^(dBm/10) / 1000';   unit = 'W';  }
      return { rows: [
        ['Formula', formula],
        ['Result',  `${result.toFixed(3)} ${unit}`],
        ['Reference', '0 dBm = 1 mW; 30 dBm = 1 W; 60 dBm = 1 kW; +3 dB ≈ ×2; +10 dB = ×10'],
      ]};
    },
    notes: [
      'Power: 10·log; voltage / current at same impedance: 20·log.',
      'dBm reference is 1 mW; dBW reference is 1 W. Differ by 30 dB.',
      'Quick mental: 3 dB = ×2; 10 dB = ×10; combine these for any dB.',
    ],
  },

  'q-factor': {
    name: 'Q Factor',
    section: '17-11',
    formula: true,
    inputs: [
      { id: 'mode', label: 'Compute from', type: 'select', default: 'XR',
        choices: [['XR', 'X / R at resonance'], ['fbw', 'f / Δf₃dB']] },
      { id: 'X',  label: 'Reactance X (Ω at resonance)', type: 'number', step: '1', default: 314 },
      { id: 'R',  label: 'Loss resistance R (Ω)', type: 'number', step: '0.1', default: 1 },
      { id: 'f',  label: 'Center frequency f (MHz)', type: 'number', step: '0.01', default: 14.150 },
      { id: 'bw', label: '3-dB bandwidth Δf (kHz)', type: 'number', step: '1', default: 35 },
    ],
    compute(v) {
      let Q, formula;
      if (v.mode === 'XR') {
        Q = parseFloat(v.X) / parseFloat(v.R);
        formula = 'Q = X / R';
      } else {
        Q = (parseFloat(v.f) * 1000) / parseFloat(v.bw);
        formula = 'Q = f / Δf₃dB';
      }
      const f = parseFloat(v.f);
      const bw = (f * 1000) / Q;
      return { rows: [
        ['Formula', formula],
        ['Q',       Q.toFixed(1)],
        ['Implied 3-dB bandwidth', `${bw.toFixed(1)} kHz @ ${f.toFixed(2)} MHz`],
        ['Voltage rise factor at resonance (V_C / V_in)', `Q = ${Q.toFixed(1)}× (in series LC)`],
      ]};
    },
    notes: [
      'High Q (>200) = sharp resonance, narrow bandwidth, low loss.',
      'Q ≈ 100 typical for L/C tank circuits; ≈ 400-500 for mag loops; ≈ 10,000 for crystals.',
      "Voltage across the cap at resonance is Q × input voltage — sizes the cap's voltage rating.",
    ],
  },

  'bandwidth': {
    name: 'Bandwidth',
    section: '17-12',
    formula: true,
    inputs: [
      { id: 'f',  label: 'Center frequency f (MHz)', type: 'number', step: '0.01', default: 14.150 },
      { id: 'Q',  label: 'Q factor',                  type: 'number', step: '1',    default: 200 },
    ],
    compute(v) {
      const f = parseFloat(v.f);
      const Q = parseFloat(v.Q);
      const bw_kHz = (f * 1000) / Q;
      const bw_pct = 100 / Q;
      return { rows: [
        ['Formula',          'BW = f / Q'],
        ['3-dB bandwidth',   `${bw_kHz.toFixed(2)} kHz  (${bw_pct.toFixed(2)}% of f)`],
        ['Approximate SWR-2:1 BW (single-tuned antenna)', `${(bw_kHz * 1.41).toFixed(1)} kHz`],
      ]};
    },
    notes: [
      'Q = 100 antenna at 14 MHz → ~140 kHz BW; Q = 400 mag loop → ~35 kHz BW.',
      'SWR-2:1 antenna BW is approximately √2 × the 3-dB BW.',
      'CW filter BW typically 250-500 Hz; SSB filter 2.4-3 kHz; AM 6 kHz; FM 12.5-25 kHz.',
    ],
  },

  'smith-chart': {
    name: 'Smith Chart (Γ ↔ Z)',
    section: '17-13',
    formula: true,
    inputs: [
      { id: 'mode', label: 'Direction', type: 'select', default: 'z2g',
        choices: [['z2g', 'Z → Γ'], ['g2z', 'Γ → Z']] },
      { id: 'R',  label: 'R (Ω)', type: 'number', step: '0.1', default: 75 },
      { id: 'X',  label: 'X (Ω, signed)', type: 'number', step: '0.1', default: 0 },
      { id: 'Z0', label: 'Z₀ (Ω)', type: 'number', step: '1', default: 50 },
      { id: 'Gmag',   label: '|Γ|',      type: 'number', step: '0.001', default: 0.2 },
      { id: 'Gphase', label: '∠Γ (deg)', type: 'number', step: '1',     default: 0 },
    ],
    compute(v) {
      const Z0 = parseFloat(v.Z0);
      if (v.mode === 'z2g') {
        const R = parseFloat(v.R), X = parseFloat(v.X);
        const num_r = R - Z0, num_i = X;
        const den_r = R + Z0, den_i = X;
        const den_mag2 = den_r * den_r + den_i * den_i;
        const G_r = (num_r * den_r + num_i * den_i) / den_mag2;
        const G_i = (num_i * den_r - num_r * den_i) / den_mag2;
        const G_mag = Math.sqrt(G_r * G_r + G_i * G_i);
        const G_phase = Math.atan2(G_i, G_r) * 180 / Math.PI;
        const swr = G_mag >= 1 ? Infinity : (1 + G_mag) / (1 - G_mag);
        return { rows: [
          ['Formula',         'Γ = (Z − Z₀) / (Z + Z₀)'],
          ['z (normalized Z)', `${(R / Z0).toFixed(3)} + j${(X / Z0).toFixed(3)}`],
          ['|Γ|',             `${G_mag.toFixed(4)}`],
          ['∠Γ',              `${G_phase.toFixed(1)}°`],
          ['Γ (rect)',        `${G_r.toFixed(4)} + j${G_i.toFixed(4)}`],
          ['SWR',             isFinite(swr) ? `${swr.toFixed(2)}:1` : '∞:1'],
        ]};
      } else {
        const Gmag = parseFloat(v.Gmag);
        const Gphase = parseFloat(v.Gphase) * Math.PI / 180;
        const G_r = Gmag * Math.cos(Gphase);
        const G_i = Gmag * Math.sin(Gphase);
        // z = (1 + Γ) / (1 − Γ); Z = z × Z₀
        const num_r = 1 + G_r, num_i = G_i;
        const den_r = 1 - G_r, den_i = -G_i;
        const den_mag2 = den_r * den_r + den_i * den_i;
        const z_r = (num_r * den_r + num_i * den_i) / den_mag2;
        const z_i = (num_i * den_r - num_r * den_i) / den_mag2;
        const Z_r = z_r * Z0;
        const Z_i = z_i * Z0;
        const swr = Gmag >= 1 ? Infinity : (1 + Gmag) / (1 - Gmag);
        return { rows: [
          ['Formula',         'z = (1 + Γ) / (1 − Γ);  Z = z × Z₀'],
          ['z (normalized)',  `${z_r.toFixed(3)} + j${z_i.toFixed(3)}`],
          ['Z',               `${Z_r.toFixed(2)} + j${Z_i.toFixed(2)} Ω`],
          ['SWR',             isFinite(swr) ? `${swr.toFixed(2)}:1` : '∞:1'],
        ]};
      }
    },
    notes: [
      'Smith chart is a polar plot of Γ. Center = perfect match (Γ=0).',
      'Normalized impedance z = Z/Z₀. Plot at the intersection of constant-r and constant-x circles.',
      'Half-wavelength of lossless line rotates 360° around the chart; quarter-wave rotates 180°.',
    ],
  },

  'rf-exposure': {
    name: 'RF Exposure',
    section: '17-14',
    formula: true,
    inputs: [
      { id: 'P_PEP', label: 'Transmitter PEP (W)', type: 'number', step: '1', default: 100 },
      { id: 'mode',  label: 'Mode',                 type: 'select', default: 'ssb',
        choices: [
          ['ssb',     'SSB voice (k_avg ≈ 0.20)'],
          ['am',      'AM voice (k_avg ≈ 0.50)'],
          ['fm',      'FM voice (k_avg = 1.0)'],
          ['cw',      'CW (k_avg ≈ 0.40)'],
          ['ft8',     'FT8/FT4/RTTY (k_avg = 1.0)'],
          ['psk31',   'PSK31 (k_avg ≈ 0.5)'],
        ]},
      { id: 'D',     label: 'TX duty cycle (0..1)', type: 'number', step: '0.05', default: 0.5,
        hint: 'Fraction of TX time during a 6-min uncontrolled / 30-min controlled window.' },
      { id: 'L_dB',  label: 'Feedline loss (dB)',   type: 'number', step: '0.1', default: 1 },
      { id: 'G_dBi', label: 'Antenna gain (dBi)',   type: 'number', step: '0.1', default: 8.15 },
      { id: 'd_m',   label: 'Distance to evaluation point (m)', type: 'number', step: '0.1', default: 5 },
      { id: 'f',     label: 'Frequency (MHz)',      type: 'number', step: '0.01', default: 14.15 },
      { id: 'env',   label: 'Environment',          type: 'select', default: 'uncontrolled',
        choices: [['uncontrolled', 'Uncontrolled (public, neighbors)'], ['controlled', 'Controlled (operator and family)']] },
    ],
    compute(v) {
      const P_PEP = parseFloat(v.P_PEP);
      const D     = parseFloat(v.D);
      const L_dB  = parseFloat(v.L_dB);
      const G_dBi = parseFloat(v.G_dBi);
      const d     = parseFloat(v.d_m);
      const f     = parseFloat(v.f);
      const k_avg_map = { ssb: 0.20, am: 0.50, fm: 1.0, cw: 0.40, ft8: 1.0, psk31: 0.5 };
      const k_avg = k_avg_map[v.mode] ?? 1.0;
      const P_avg = P_PEP * k_avg * D;
      const P_ant = P_avg * Math.pow(10, -L_dB / 10);
      const EIRP  = P_ant * Math.pow(10, G_dBi / 10);
      // Power density with 4× ground-reflection enhancement (FCC worst case)
      const S_worst = EIRP / (Math.PI * d * d);
      // S_MPE table (W/m²) per FCC §1.1310
      let S_MPE;
      if (v.env === 'uncontrolled') {
        if      (f >= 0.3 && f <= 3.0)    S_MPE = Math.min(100, 100 / (f * f));
        else if (f > 3.0 && f <= 30)      S_MPE = 180 / (f * f);
        else if (f > 30 && f <= 300)      S_MPE = 0.2;
        else if (f > 300 && f <= 1500)    S_MPE = 0.0067 * f;
        else                              S_MPE = 1.0;
      } else {
        if      (f >= 0.3 && f <= 3.0)    S_MPE = 100;
        else if (f > 3.0 && f <= 30)      S_MPE = 900 / (f * f);
        else if (f > 30 && f <= 300)      S_MPE = 1.0;
        else if (f > 300 && f <= 1500)    S_MPE = 0.0335 * f;
        else                              S_MPE = 5.0;
      }
      const compliant = S_worst <= S_MPE;
      const margin = (S_MPE / S_worst);
      return { rows: [
        ['Average power factor (mode × duty)',  `${(k_avg * D).toFixed(3)} × ${P_PEP} W`],
        ['Avg power into feedline',              `${P_avg.toFixed(1)} W`],
        ['Power at antenna (after feedline)',    `${P_ant.toFixed(1)} W`],
        ['EIRP',                                  `${EIRP.toFixed(1)} W (${(10 * Math.log10(EIRP)).toFixed(1)} dBW)`],
        ['Power density at d (4× ground reflection)', `${S_worst.toFixed(3)} W/m²`],
        [`MPE limit (${v.env})`,                 `${S_MPE.toFixed(3)} W/m²`],
        ['Compliance',                            compliant ? `✅ COMPLIANT (margin ${margin.toFixed(2)}×)` : `❌ EXCEEDS MPE (over by ${(1 / margin).toFixed(2)}×)`],
      ]};
    },
    notes: [
      'Average power = PEP × mode-factor × duty. SSB voice averages ~10–20% of PEP.',
      'EIRP not ERP. Convert dBd → dBi by adding 2.15 if your antenna spec is in dBd.',
      "FCC averages over 6 min uncontrolled / 30 min controlled.",
      'Failed scenarios: reduce power, reduce duty cycle, increase distance, or aim antenna away.',
    ],
  },
};

function awCalcRenderList() {
  const wrap = document.getElementById('aw-calc-list-items');
  const calcKeys = Object.keys(AW_CALCS);
  const antennaIds = AW_ANTENNAS.map(a => a.id);

  // Antenna calcs follow AW_ANTENNAS order so the list mirrors the chapter sections.
  const antennaCalcs   = antennaIds.filter(id => calcKeys.includes(id));
  const remaining      = antennaIds.filter(id => !calcKeys.includes(id));
  const componentCalcs = calcKeys.filter(id => AW_CALCS[id].component);
  const formulaCalcs   = calcKeys.filter(id => AW_CALCS[id].formula);

  let html = `<div class="aw-calc-list-divider">Antennas</div>` +
    antennaCalcs.map(id => `
      <div class="aw-calc-list-item" onclick="awCalcOpen('${id}')">
        <div class="aw-calc-list-name">${escHtml(AW_CALCS[id].name)}</div>
        <div class="aw-calc-list-section">§${AW_CALCS[id].section}</div>
      </div>`).join('');

  if (componentCalcs.length) {
    html += `<div class="aw-calc-list-divider">Components</div>` +
      componentCalcs.map(id => `
        <div class="aw-calc-list-item" onclick="awCalcOpen('${id}')">
          <div class="aw-calc-list-name">${escHtml(AW_CALCS[id].name)}</div>
          <div class="aw-calc-list-section">§${AW_CALCS[id].section}</div>
        </div>`).join('');
  }

  if (formulaCalcs.length) {
    html += `<div class="aw-calc-list-divider">Formulas</div>` +
      formulaCalcs.map(id => `
        <div class="aw-calc-list-item" onclick="awCalcOpen('${id}')">
          <div class="aw-calc-list-name">${escHtml(AW_CALCS[id].name)}</div>
          <div class="aw-calc-list-section">§${AW_CALCS[id].section}</div>
        </div>`).join('');
  }

  if (remaining.length) {
    html += `<div class="aw-calc-list-divider">Coming soon</div>` +
      remaining.map(id => {
        const ant = AW_ANTENNAS.find(a => a.id === id);
        return `<div class="aw-calc-list-item disabled">
          <div class="aw-calc-list-name">${escHtml(ant.name)}</div>
          <div class="aw-calc-list-section">§${ant.section}</div>
        </div>`;
      }).join('');
  }

  wrap.innerHTML = html;
  aw.calc.listRendered = true;
}

function awCalcOpen(id) {
  if (document.getElementById('aw-calculators').style.display === 'none') {
    document.querySelectorAll('.aw-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.aw-tab')[1].classList.add('active');
    document.getElementById('aw-recommender').style.display = 'none';
    document.getElementById('aw-calculators').style.display = '';
    if (!aw.calc.listRendered) awCalcRenderList();
  }

  const calc = AW_CALCS[id];
  if (!calc) {
    // Defensive fallback — every entry in AW_ANTENNAS currently has a
    // matching AW_CALCS entry, so this branch is unreachable in normal
    // UX. Lands here only if a future antenna is added to AW_ANTENNAS
    // without a calculator companion. Keep the message factual, not
    // promissory (the old "coming in a follow-up commit" wording aged
    // badly when the follow-up shipped).
    document.getElementById('aw-calc-panel').innerHTML =
      `<div style="color:var(--peach);font-size:13px">Calculator '${escHtml(id)}' isn't defined. The recommender scores it but no AW_CALCS entry was found — add one or remove the antenna from AW_ANTENNAS.</div>`;
    return;
  }
  aw.calc.current = id;
  if (!aw.calc[id]) {
    aw.calc[id] = {};
    calc.inputs.forEach(i => aw.calc[id][i.id] = i.default);
  }
  awCalcRenderPanel();
}

function awCalcRenderPanel() {
  const id = aw.calc.current;
  const calc = AW_CALCS[id];
  if (!calc) return;
  const v = aw.calc[id];

  const inputsHtml = calc.inputs.map(i => {
    if (i.type === 'select') {
      return `<div class="aw-calc-input">
        <label>${escHtml(i.label)}</label>
        <select onchange="awCalcSet('${i.id}', this.value)">
          ${i.choices.map(([cv, cl]) => `
            <option value="${cv}" ${v[i.id] === cv ? 'selected' : ''}>${escHtml(cl)}</option>
          `).join('')}
        </select>
        ${i.hint ? `<div class="aw-calc-hint">${escHtml(i.hint)}</div>` : ''}
      </div>`;
    }
    return `<div class="aw-calc-input">
      <label>${escHtml(i.label)}</label>
      <input type="${i.type}" step="${i.step || 'any'}"
             value="${v[i.id] != null ? escHtml(v[i.id]) : ''}"
             oninput="awCalcSet('${i.id}', this.value)">
      ${i.hint ? `<div class="aw-calc-hint">${escHtml(i.hint)}</div>` : ''}
    </div>`;
  }).join('');

  document.getElementById('aw-calc-panel').innerHTML = `
    <div class="aw-calc-header">
      <div>
        <div class="aw-calc-title">${escHtml(calc.name)}</div>
        <div class="aw-calc-section">
          <a href="#" onclick="document.querySelector('[data-tab=learn]').click(); openLearnSection('${calc.section}'); return false">
            Read §${calc.section} →
          </a>
        </div>
      </div>
    </div>
    <div class="aw-calc-grid">
      <div>
        <div class="aw-calc-subhead">Inputs</div>
        ${inputsHtml}
      </div>
      <div>
        <div class="aw-calc-subhead">Outputs</div>
        <table class="aw-calc-out"><tbody id="aw-calc-out-body"></tbody></table>
      </div>
    </div>
    ${calc.diagram ? `<div class="aw-calc-subhead">Diagram</div>${calc.diagram()}` : ''}
    ${calc.notes && calc.notes.length ? `
      <div class="aw-calc-subhead">Notes</div>
      <ul class="aw-calc-notes">
        ${calc.notes.map(n => `<li>${escHtml(n)}</li>`).join('')}
      </ul>` : ''}
  `;
  // Populate the output table without rebuilding the inputs (which would steal focus).
  awCalcRenderOutput();
}

// Recompute and update only the output table — leaves input DOM intact so typing focus is preserved.
function awCalcRenderOutput() {
  const id = aw.calc.current;
  const calc = AW_CALCS[id];
  if (!calc) return;
  const tbody = document.getElementById('aw-calc-out-body');
  if (!tbody) return;
  const v = aw.calc[id];
  const result = calc.compute(v);
  tbody.innerHTML = result.rows.map(([k, val]) =>
    `<tr><td class="aw-calc-out-key">${escHtml(k)}</td>
         <td class="aw-calc-out-val">${escHtml(val)}</td></tr>`).join('');
}

function awCalcSet(field, val) {
  const id = aw.calc.current;
  if (!id) return;
  if (!aw.calc[id]) aw.calc[id] = {};
  aw.calc[id][field] = val;
  awCalcRenderOutput();
}

// Recommender wizard initializes lazily — only when user clicks the
// Recommender subnav button (see awSwitch). Don't auto-render on load.
