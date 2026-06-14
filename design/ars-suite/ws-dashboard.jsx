/* J-Hub Workspace — day-to-day operating COCKPIT (Dashboard view).
   Three columns:
     1) Full J-Log data-entry pane (callsign/DXCC lookup + all QSO fields) over
        the live log.
     2) DX Cluster — spot list with band/mode/needed filters; click a spot to
        tune the rig and pre-fill the entry.
     3) Collapsible drawer widgets — Rig, Space weather, Propagation, Weather.
   Entry, cluster and rig share one tuning state (call/freq/band/mode). */

(function () {
  if (document.getElementById('ws-dash-styles')) return;
  const s = document.createElement('style');
  s.id = 'ws-dash-styles';
  s.textContent = `
  .ck { padding:16px 20px 30px; min-width:1080px; }
  .ck-top { display:flex; align-items:center; gap:9px; margin-bottom:14px; flex-wrap:wrap; }
  .ck-prof { display:flex; align-items:center; gap:8px; padding:7px 12px 7px 9px; border-radius:8px; border:1px solid var(--border); background:var(--surface-1); cursor:pointer; transition:border-color .13s, background .13s; }
  .ck-prof:hover { border-color:var(--border-glow); background:var(--surface-2); }
  .ck-pdot { width:8px; height:8px; border-radius:3px; background:var(--h); }
  .ck-pname { font-size:12.5px; font-weight:600; }
  .ck-live { margin-left:auto; display:flex; align-items:center; gap:16px; font-size:12px; color:var(--t2); }
  .ck-live .ars-mono { color:var(--t1); }
  .ck-grid { display:grid; grid-template-columns:minmax(400px,1fr) 342px 348px; gap:14px; align-items:start; }

  .fld-lbl { font-size:9px; letter-spacing:1.1px; text-transform:uppercase; color:var(--t3); font-weight:600; }

  /* ---- entry pane ---- */
  .ep { background:var(--surface-1); border:1px solid var(--border); border-radius:var(--radius-lg); overflow:hidden; margin-bottom:14px; }
  .ep-head, .ml-head { display:flex; align-items:center; gap:11px; padding:12px 16px; border-bottom:1px solid var(--border); }
  .ep-icon, .ml-icon { width:32px; height:32px; border-radius:9px; display:flex; align-items:center; justify-content:center; background:color-mix(in oklch,var(--log) 18%, var(--surface-3)); color:var(--log); border:1px solid color-mix(in oklch,var(--log) 28%, transparent); }
  .ep-title, .ml-title { font-size:14.5px; font-weight:600; }
  .ep-sub, .ml-sub { font-size:11px; color:var(--t3); margin-top:1px; }
  .ep-open, .ml-open { margin-left:auto; font-size:12px; }

  /* callsign info strip */
  .ci { display:flex; align-items:center; gap:18px; padding:10px 16px; border-bottom:1px solid var(--border); flex-wrap:wrap; min-height:42px; background:var(--surface-2); }
  .ci-empty { color:var(--t3); font-size:12px; }
  .ci-ent { font-size:14px; font-weight:600; }
  .ci-meta { display:flex; gap:16px; font-size:11.5px; color:var(--t3); flex-wrap:wrap; }
  .ci-meta b { color:var(--t1); font-family:'JetBrains Mono', monospace; font-weight:600; margin-left:5px; }
  .ci-worked { margin-left:auto; font-size:10.5px; font-weight:700; letter-spacing:.4px; text-transform:uppercase; padding:4px 10px; border-radius:99px; white-space:nowrap; }
  .ci-worked.new { color:var(--ok); background:color-mix(in oklch,var(--ok) 14%, transparent); border:1px solid color-mix(in oklch,var(--ok) 35%, transparent); }
  .ci-worked.dup { color:var(--t2); background:var(--surface-3); border:1px solid var(--border-2); }

  .ep-grid { padding:14px 16px; display:flex; flex-wrap:wrap; gap:11px 13px; align-items:flex-end; background:color-mix(in oklch,var(--log) 5%, var(--surface-1)); }
  .ep-fld { display:flex; flex-direction:column; gap:4px; }
  .ep-fld.grow { flex:1 1 130px; min-width:110px; }
  .ep-in { font-family:'IBM Plex Sans', sans-serif; font-size:13.5px; color:var(--t1); background:var(--bg); border:1px solid var(--border-2); border-radius:6px; padding:8px 10px; width:100%; }
  .ep-in:focus { outline:none; border-color:var(--log); box-shadow:0 0 0 3px color-mix(in oklch,var(--log) 16%, transparent); }
  .ep-in.mono { font-family:'JetBrains Mono', monospace; font-weight:600; text-transform:uppercase; }
  .ep-in.call { font-size:18px; font-weight:700; letter-spacing:.5px; width:148px; }
  .ep-in.freq { width:108px; font-size:14px; }
  .ep-in.rst { width:62px; text-align:center; }
  .ep-in.grid { width:78px; }
  .ep-sel { font-family:'JetBrains Mono', monospace; font-weight:600; font-size:13px; color:var(--t1); background:var(--bg); border:1px solid var(--border-2); border-radius:6px; padding:8px 24px 8px 9px; cursor:pointer; appearance:none;
    background-image:linear-gradient(45deg,transparent 50%,var(--t3) 50%),linear-gradient(135deg,var(--t3) 50%,transparent 50%); background-position:calc(100% - 12px) 52%,calc(100% - 8px) 52%; background-size:5px 5px,5px 5px; background-repeat:no-repeat; }
  .ep-sel:focus { outline:none; border-color:var(--log); }
  .ep-logbtn { font-family:inherit; font-size:14px; font-weight:700; letter-spacing:.3px; padding:11px 22px; border-radius:7px; border:1px solid var(--log); background:var(--log); color:#1c1402; cursor:pointer; transition:filter .12s; white-space:nowrap; }
  .ep-logbtn:hover { filter:brightness(1.08); }
  .ep-clear { font-size:12px; }

  /* ---- log ---- */
  .ml { background:var(--surface-1); border:1px solid var(--border); border-radius:var(--radius-lg); overflow:hidden; }
  .ml-slim { display:flex; align-items:center; gap:10px; padding:10px 16px; border-bottom:1px solid var(--border); font-size:12px; font-weight:600; color:var(--t2); }
  .ml-tablehead, .ml-row { display:grid; grid-template-columns:58px 1fr 70px 54px 64px 56px; gap:8px; align-items:center; padding:8px 16px; }
  .ml-tablehead { font-size:9px; letter-spacing:1px; text-transform:uppercase; color:var(--t3); font-weight:600; border-bottom:1px solid var(--border); }
  .ml-list { max-height:300px; overflow:auto; }
  .ml-row { font-size:12.5px; border-bottom:1px solid var(--border); }
  .ml-row:last-child { border-bottom:none; }
  .ml-row.fresh { animation:mlflash 1.3s ease-out; }
  @keyframes mlflash { from { background:color-mix(in oklch,var(--log) 26%, transparent); } to { background:transparent; } }
  .ml-call { font-family:'JetBrains Mono', monospace; font-weight:600; color:var(--t1); }
  .ml-mono { font-family:'JetBrains Mono', monospace; color:var(--t2); }
  .ml-mode { font-size:10.5px; font-weight:600; padding:2px 7px; border-radius:5px; background:var(--surface-3); border:1px solid var(--border); color:var(--t2); justify-self:start; }

  /* ---- dx cluster ---- */
  .dx { background:var(--surface-1); border:1px solid var(--border); border-radius:var(--radius-lg); display:flex; flex-direction:column; overflow:hidden; max-height:640px; }
  .dx-head { display:flex; align-items:center; gap:10px; padding:12px 14px; border-bottom:1px solid var(--border); }
  .dx-ic { width:30px; height:30px; border-radius:8px; display:flex; align-items:center; justify-content:center; background:color-mix(in oklch,var(--map) 16%, var(--surface-3)); color:var(--map); }
  .dx-conn { margin-left:auto; display:flex; align-items:center; gap:7px; font-family:'JetBrains Mono',monospace; font-size:10.5px; color:var(--t3); }
  .dx-ctl { display:flex; gap:6px; padding:9px 12px; border-bottom:1px solid var(--border); align-items:center; flex-wrap:wrap; }
  .dx-msel { font-family:'JetBrains Mono',monospace; font-size:11px; font-weight:600; color:var(--t1); background:var(--bg); border:1px solid var(--border-2); border-radius:6px; padding:5px 22px 5px 8px; cursor:pointer; appearance:none;
    background-image:linear-gradient(45deg,transparent 50%,var(--t3) 50%),linear-gradient(135deg,var(--t3) 50%,transparent 50%); background-position:calc(100% - 11px) 52%,calc(100% - 7px) 52%; background-size:4px 4px,4px 4px; background-repeat:no-repeat; }
  .dx-chip { font-size:11px; font-weight:600; padding:6px 11px; border-radius:6px; border:1px solid var(--border-2); background:var(--surface-3); color:var(--t2); cursor:pointer; transition:all .12s; }
  .dx-chip.on { background:var(--accent-dim); border-color:var(--accent-line); color:var(--accent); }
  .dx-spotbtn { margin-left:auto; font-size:11.5px; }
  .dx-list { flex:1; overflow:auto; }
  .dx-spot { display:flex; flex-direction:column; gap:3px; padding:8px 13px; border-bottom:1px solid var(--border); cursor:pointer; position:relative; transition:background .1s; }
  .dx-spot:hover { background:var(--surface-2); }
  .dx-spot.need { background:color-mix(in oklch,var(--warn) 7%, transparent); }
  .dx-spot.need::before { content:''; position:absolute; left:0; top:0; bottom:0; width:3px; background:var(--warn); }
  .dx-r1 { display:flex; align-items:center; gap:9px; }
  .dx-freq { font-family:'JetBrains Mono',monospace; font-weight:600; font-size:13px; color:var(--accent); }
  .dx-call { font-family:'JetBrains Mono',monospace; font-weight:700; font-size:13px; color:var(--t1); }
  .dx-need { font-size:8.5px; font-weight:800; letter-spacing:.5px; color:var(--warn); border:1px solid color-mix(in oklch,var(--warn) 45%, transparent); border-radius:4px; padding:1px 5px; }
  .dx-age { margin-left:auto; font-family:'JetBrains Mono',monospace; font-size:10px; color:var(--t4); }
  .dx-r2 { display:flex; gap:8px; font-size:11px; color:var(--t3); align-items:center; }
  .dx-spotter { font-family:'JetBrains Mono',monospace; color:var(--t2); }
  .dx-empty { padding:24px; text-align:center; color:var(--t3); font-size:12px; }

  /* ---- drawer widgets ---- */
  .dw-stack { display:flex; flex-direction:column; gap:11px; }
  .dw { background:var(--surface-1); border:1px solid var(--border); border-radius:var(--radius); overflow:hidden; }
  .dw-head { width:100%; display:flex; align-items:center; gap:10px; padding:11px 13px; background:transparent; border:none; cursor:pointer; color:var(--t1); text-align:left; }
  .dw-head:hover { background:var(--surface-2); }
  .dw-ic { width:28px; height:28px; border-radius:7px; flex:0 0 auto; display:flex; align-items:center; justify-content:center; background:color-mix(in oklch,var(--h) 16%, var(--surface-3)); color:var(--h); }
  .dw-title { font-size:13px; font-weight:600; flex:1; }
  .dw-sum { font-family:'JetBrains Mono', monospace; font-size:12px; color:var(--t2); }
  .dw-caret { color:var(--t4); transition:transform .18s; display:flex; }
  .dw.open .dw-caret { transform:rotate(90deg); }
  .dw-wrap { display:grid; grid-template-rows:0fr; transition:grid-template-rows .2s ease; }
  .dw.open .dw-wrap { grid-template-rows:1fr; }
  .dw-inner { overflow:hidden; }
  .dw-body { padding:4px 14px 15px; border-top:1px solid var(--border); }

  .rg-freq { font-family:'JetBrains Mono', monospace; font-size:28px; font-weight:600; letter-spacing:-.5px; margin:10px 0 2px; }
  .rg-freq .u { font-size:12px; color:var(--accent); margin-left:5px; }
  .rg-chips { display:flex; gap:6px; margin:8px 0 12px; }
  .rg-chip { font-family:'JetBrains Mono',monospace; font-size:11.5px; font-weight:600; padding:5px 9px; border-radius:6px; background:var(--surface-3); border:1px solid var(--border); color:var(--t2); }
  .rg-freqrow { display:flex; align-items:baseline; gap:7px; margin:10px 0 2px; }
  .rg-freqin { font-family:'JetBrains Mono',monospace; font-size:27px; font-weight:600; letter-spacing:-.5px; color:var(--t1); background:var(--bg); border:1px solid var(--border); border-radius:7px; padding:4px 11px; width:198px; }
  .rg-freqin:focus { outline:none; border-color:var(--accent-line); box-shadow:0 0 0 3px var(--accent-dim); }
  .rg-u { font-size:12px; color:var(--accent); }
  .rg-modesel { font-family:'JetBrains Mono',monospace; font-size:11.5px; font-weight:600; color:var(--accent); background:var(--accent-dim); border:1px solid var(--accent-line); border-radius:6px; padding:5px 23px 5px 9px; cursor:pointer; appearance:none; background-image:linear-gradient(45deg,transparent 50%,var(--accent) 50%),linear-gradient(135deg,var(--accent) 50%,transparent 50%); background-position:calc(100% - 11px) 52%,calc(100% - 7px) 52%; background-size:4px 4px,4px 4px; background-repeat:no-repeat; }
  .rg-modesel:focus { outline:none; }
  .rg-s { display:flex; align-items:flex-end; gap:2px; height:18px; margin-bottom:3px; }
  .rg-s i { flex:1; border-radius:1px; }
  .rg-slabel { display:flex; justify-content:space-between; font-family:'JetBrains Mono',monospace; font-size:8.5px; color:var(--t4); }
  .rg-meters { display:grid; grid-template-columns:1fr 1fr; gap:10px; margin:12px 0; }
  .rg-m .k { font-size:9px; letter-spacing:1px; text-transform:uppercase; color:var(--t3); font-weight:600; }
  .rg-m .v { font-family:'JetBrains Mono',monospace; font-size:15px; font-weight:600; }
  .rg-bands { display:grid; grid-template-columns:repeat(5,1fr); gap:5px; }
  .rg-band { font-family:'JetBrains Mono',monospace; font-size:11px; font-weight:600; padding:6px 0; text-align:center; border-radius:5px; background:var(--surface-3); border:1px solid var(--border); color:var(--t2); cursor:pointer; transition:all .12s; }
  .rg-band:hover { border-color:var(--border-glow); color:var(--t1); }
  .rg-band.on { background:var(--accent-dim); border-color:var(--accent-line); color:var(--accent); }
  .rg-tune { display:flex; align-items:center; gap:6px; margin:6px 0 12px; }
  .rg-nudge { width:32px; height:30px; flex:0 0 auto; border-radius:6px; border:1px solid var(--border-2); background:var(--surface-3); color:var(--t1); cursor:pointer; display:flex; align-items:center; justify-content:center; transition:all .12s; }
  .rg-nudge:hover { border-color:var(--border-glow); background:var(--surface-4); }
  .rg-steps { display:flex; gap:4px; flex:1; }
  .rg-step { flex:1; font-family:'JetBrains Mono',monospace; font-size:11px; font-weight:600; padding:7px 0; text-align:center; border-radius:5px; background:var(--surface-3); border:1px solid var(--border); color:var(--t3); cursor:pointer; transition:all .12s; }
  .rg-step:hover { color:var(--t1); border-color:var(--border-glow); }
  .rg-step.on { background:var(--accent-dim); border-color:var(--accent-line); color:var(--accent); }
  .ro-top { display:flex; gap:14px; align-items:center; margin:10px 0 12px; }
  .ro-info { flex:1; min-width:0; }
  .ro-head { font-size:32px; font-weight:600; line-height:1; }
  .ro-dir { font-size:11.5px; color:var(--t3); margin-top:4px; letter-spacing:.5px; }
  .ro-turn { display:flex; gap:6px; margin-top:12px; }
  .ro-turn button { flex:1; font-family:inherit; font-size:11px; font-weight:600; padding:7px 0; border-radius:6px; border:1px solid var(--border-2); background:var(--surface-3); color:var(--t2); cursor:pointer; transition:all .12s; }
  .ro-turn button:hover { border-color:var(--border-glow); color:var(--t1); }
  .ro-turn button.stop { color:var(--err); border-color:color-mix(in oklch,var(--err) 35%, var(--border-2)); }
  .ro-turn button.stop:hover { background:color-mix(in oklch,var(--err) 12%, transparent); }
  .ro-slider { margin:4px 0 14px; }
  .ro-presets { display:grid; grid-template-columns:repeat(3,1fr); gap:6px; }
  .ro-preset { font-size:11px; font-weight:600; padding:8px 4px; text-align:center; border-radius:6px; background:var(--surface-3); border:1px solid var(--border); color:var(--t2); cursor:pointer; transition:all .12s; }
  .ro-preset:hover { border-color:var(--border-glow); color:var(--t1); }
  .ro-preset.on { background:color-mix(in oklch,var(--sat) 14%, transparent); border-color:color-mix(in oklch,var(--sat) 40%, transparent); color:var(--sat); }
  .ro-preset b { font-family:'JetBrains Mono',monospace; color:var(--t4); font-weight:600; margin-left:5px; }
  .ro-preset.on b { color:var(--sat); }

  .sw-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:9px; margin:10px 0 14px; }
  .sw-cell { background:var(--surface-2); border:1px solid var(--border); border-radius:7px; padding:8px 9px; }
  .sw-k { font-size:9px; letter-spacing:.8px; text-transform:uppercase; color:var(--t3); font-weight:600; }
  .sw-v { font-family:'JetBrains Mono',monospace; font-size:16px; font-weight:600; margin-top:3px; }
  .sw-bands { display:flex; flex-direction:column; gap:5px; }
  .sw-bhead, .sw-brow { display:grid; grid-template-columns:1fr 70px 70px; gap:8px; align-items:center; }
  .sw-bhead { font-size:9px; letter-spacing:1px; text-transform:uppercase; color:var(--t3); font-weight:600; }
  .sw-bname { font-family:'JetBrains Mono',monospace; font-size:12px; color:var(--t1); }
  .sw-cond { font-size:11px; font-weight:600; padding:3px 0; text-align:center; border-radius:4px; }
  .sw-cond.good { color:var(--ok); background:color-mix(in oklch,var(--ok) 14%, transparent); }
  .sw-cond.fair { color:var(--warn); background:color-mix(in oklch,var(--warn) 14%, transparent); }
  .sw-cond.poor { color:var(--err); background:color-mix(in oklch,var(--err) 14%, transparent); }
  .pr-row, .wx-row { display:flex; align-items:center; justify-content:space-between; padding:8px 0; border-bottom:1px solid var(--border); font-size:12.5px; }
  .pr-row:last-child, .wx-row:last-child { border-bottom:none; }
  .pr-k, .wx-k { color:var(--t2); }
  .pr-v, .wx-v { font-family:'JetBrains Mono',monospace; font-weight:600; color:var(--t1); }
  .wx-big { display:flex; align-items:baseline; gap:8px; margin:8px 0 12px; }
  .wx-temp { font-family:'JetBrains Mono',monospace; font-size:30px; font-weight:600; }
  .wx-cond { font-size:13px; color:var(--t2); }
  .dw-stamp { font-family:'JetBrains Mono',monospace; font-size:10px; color:var(--t4); margin-top:11px; text-align:right; }
  `;
  document.head.appendChild(s);
})();

const WS_QSOS = [
  { t: '14:42', call: 'DL8WPX', band: '20m', mode: 'FT8', rst: '-13' },
  { t: '14:31', call: 'VK9DX', band: '20m', mode: 'CW', rst: '559' },
  { t: '14:18', call: 'JA1XYZ', band: '20m', mode: 'SSB', rst: '59' },
  { t: '14:09', call: 'EA5K', band: '20m', mode: 'FT8', rst: '-08' },
  { t: '13:57', call: 'G4ABC', band: '20m', mode: 'CW', rst: '599' },
  { t: '13:44', call: 'PY2NY', band: '20m', mode: 'SSB', rst: '57' },
  { t: '13:30', call: 'VE7CC', band: '20m', mode: 'FT8', rst: '-04' },
];
const WS_SOLAR = [['SFI', '168'], ['SN', '142'], ['A-idx', '7'], ['K-idx', '2'], ['X-ray', 'B1.2'], ['304Å', '171'], ['Aurora', '1.5'], ['MUF', '28.4']];
const WS_BANDCOND = [['80m–40m', 'good', 'good'], ['30m–20m', 'good', 'fair'], ['17m–15m', 'fair', 'poor'], ['12m–10m', 'fair', 'poor']];

const WS_SPOTS = [
  { f: '14.022.0', call: 'VK9DX', mode: 'CW', spotter: 'JA1ABC', age: '1m', cmt: 'up 2', need: true },
  { f: '21.295.0', call: 'ZD7BG', mode: 'SSB', spotter: 'G4XYZ', age: '3m', cmt: '59 into EU', need: true },
  { f: '7.005.0', call: '5U5R', mode: 'CW', spotter: 'W3LPL', age: '4m', cmt: 'ATNO!', need: true },
  { f: '14.074.0', call: 'DL8WPX', mode: 'FT8', spotter: 'EA5K', age: '5m', cmt: '-13 dB' },
  { f: '18.100.0', call: 'FO5QB', mode: 'CW', spotter: 'VE7CC', age: '7m', cmt: '', need: true },
  { f: '28.495.0', call: 'PY2NY', mode: 'SSB', spotter: 'K1TTT', age: '9m', cmt: 'strong' },
  { f: '10.136.0', call: 'JA7QVI', mode: 'FT8', spotter: 'N6TR', age: '11m', cmt: '+02 dB' },
  { f: '3.798.0', call: 'TF3IRA', mode: 'SSB', spotter: 'OH2BH', age: '14m', cmt: 'gray line' },
  { f: '24.915.0', call: 'EA8RKL', mode: 'CW', spotter: 'F6BEE', age: '18m', cmt: '' },
];

const PFX = [
  [/^(K|W|N|A[A-K]|[KWN][0-9])/, 'United States', '05'],
  [/^(VE|VA|VO|VY|CY)/, 'Canada', '05'],
  [/^DL|^DK|^DJ|^DF|^DD/, 'Germany', '14'],
  [/^(G|M|2E)/, 'England', '14'],
  [/^F[0-9]/, 'France', '14'],
  [/^EA[0-9]/, 'Spain', '14'],
  [/^I[0-9]/, 'Italy', '15'],
  [/^OH/, 'Finland', '15'],
  [/^(JA|JH|JR|JE|7K)/, 'Japan', '25'],
  [/^VK/, 'Australia', '30'],
  [/^(PY|PP|PT|PR)/, 'Brazil', '11'],
  [/^EA8/, 'Canary Is.', '33'],
  [/^ZD7/, 'St. Helena', '36'],
  [/^5U/, 'Niger', '35'],
  [/^FO/, 'Fr. Polynesia', '32'],
  [/^TF/, 'Iceland', '40'],
];
const BEAR = { 'United States': ['—', 'home'], 'Canada': ['315°', '1,820 km'], 'Germany': ['48°', '6,400 km'], 'England': ['51°', '5,560 km'], 'France': ['52°', '6,100 km'], 'Spain': ['58°', '6,000 km'], 'Italy': ['46°', '7,200 km'], 'Finland': ['33°', '6,900 km'], 'Japan': ['332°', '10,900 km'], 'Australia': ['250°', '16,200 km'], 'Brazil': ['155°', '7,600 km'], 'Canary Is.': ['86°', '5,800 km'], 'St. Helena': ['110°', '9,300 km'], 'Niger': ['95°', '8,200 km'], 'Fr. Polynesia': ['265°', '9,800 km'], 'Iceland': ['38°', '4,200 km'] };

function callInfo(call) {
  const c = (call || '').toUpperCase().trim();
  if (c.length < 3) return null;
  const m = PFX.find(([re]) => re.test(c));
  const entity = m ? m[1] : 'Unknown entity';
  const cqz = m ? m[2] : '—';
  const [bearing, dist] = BEAR[entity] || ['—', '—'];
  return { entity, cqz, bearing, dist };
}
function bandFromFreq(f) {
  const m = parseFloat(f); if (isNaN(m)) return '20m';
  if (m < 2.1) return '160m'; if (m < 4.1) return '80m'; if (m < 7.4) return '40m'; if (m < 10.2) return '30m';
  if (m < 14.4) return '20m'; if (m < 18.2) return '17m'; if (m < 21.5) return '15m'; if (m < 25) return '12m';
  if (m < 29.8) return '10m'; return '6m';
}
const defaultRst = (mode) => (mode === 'CW' || mode === 'RTTY') ? '599' : (mode === 'FT8' || mode === 'FT4') ? '-10' : '59';
function freqToHz(str) { const p = String(str).replace(/[^\d.]/g, '').split('.'); const mhz = parseInt(p[0] || '0', 10) || 0; const khz = parseInt((p[1] || '').padEnd(3, '0').slice(0, 3), 10) || 0; const dhz = parseInt((p[2] || '').padEnd(2, '0').slice(0, 2), 10) || 0; return mhz * 1e6 + khz * 1e3 + dhz * 10; }
function hzToFreq(hz) { hz = Math.max(0, Math.round(hz / 10) * 10); const mhz = Math.floor(hz / 1e6); const khz = String(Math.floor((hz % 1e6) / 1e3)).padStart(3, '0'); const dhz = String(Math.round((hz % 1e3) / 10)).padStart(2, '0'); return `${mhz}.${khz}.${dhz}`; }
function azLabel(a) { const d = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW']; return d[Math.round((((a % 360) + 360) % 360) / 45) % 8]; }
const pad3 = (n) => String(((n % 360) + 360) % 360).padStart(3, '0');

function WSDrawerWidget({ title, hue, glyph, summary, defaultOpen, children }) {
  const [open, setOpen] = React.useState(!!defaultOpen);
  return (
    <div className={`dw ${open ? 'open' : ''}`} style={{ '--h': `var(--${hue})` }}>
      <button className="dw-head" onClick={() => setOpen(o => !o)}>
        <span className="dw-ic">{glyph}</span>
        <span className="dw-title">{title}</span>
        {!open && summary && <span className="dw-sum">{summary}</span>}
        <span className="dw-caret"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"><polyline points="9 6 15 12 9 18"/></svg></span>
      </button>
      <div className="dw-wrap"><div className="dw-inner"><div className="dw-body">{children}</div></div></div>
    </div>
  );
}

function DXCluster({ onTune }) {
  const [bandF, setBandF] = React.useState('All');
  const [modeF, setModeF] = React.useState('All');
  const [needOnly, setNeedOnly] = React.useState(false);
  const modeGroup = (m) => (m === 'CW') ? 'CW' : (m === 'FT8' || m === 'FT4' || m === 'RTTY' || m === 'PSK') ? 'Digi' : 'Phone';
  const spots = WS_SPOTS.filter(s =>
    (bandF === 'All' || bandFromFreq(s.f) === bandF) &&
    (modeF === 'All' || modeGroup(s.mode) === modeF) &&
    (!needOnly || s.need));
  return (
    <div className="dx">
      <div className="dx-head">
        <span className="dx-ic"><ARSGlyph id="map" size={17} stroke={1.9} /></span>
        <span style={{ fontSize: 14.5, fontWeight: 600 }}>DX Cluster</span>
        <span className="dx-conn"><span className="ars-dot on"></span>dxc.ve7cc.net</span>
      </div>
      <div className="dx-ctl">
        <select className="dx-msel" value={bandF} onChange={e => setBandF(e.target.value)}>
          {['All', '160m', '80m', '40m', '30m', '20m', '17m', '15m', '12m', '10m'].map(b => <option key={b}>{b}</option>)}
        </select>
        <select className="dx-msel" value={modeF} onChange={e => setModeF(e.target.value)}>
          {['All', 'CW', 'Phone', 'Digi'].map(m => <option key={m}>{m}</option>)}
        </select>
        <button className={`dx-chip ${needOnly ? 'on' : ''}`} onClick={() => setNeedOnly(v => !v)}>Needed</button>
        <button className="ars-btn ghost dx-spotbtn">+ Spot</button>
      </div>
      <div className="dx-list ars-scroll">
        {spots.length === 0 && <div className="dx-empty">No spots match the filter.</div>}
        {spots.map((s, i) => (
          <div key={i} className={`dx-spot ${s.need ? 'need' : ''}`} onClick={() => onTune(s)} title="Click to tune & fill entry">
            <div className="dx-r1">
              <span className="dx-freq">{s.f}</span>
              <span className="dx-call">{s.call}</span>
              {s.need && <span className="dx-need">NEED</span>}
              <span className="dx-age">{s.age}</span>
            </div>
            <div className="dx-r2">
              <span className="ml-mode">{s.mode}</span>
              <span>{bandFromFreq(s.f)}</span>
              <span className="dx-spotter">de {s.spotter}</span>
              {s.cmt && <span style={{ color: 'var(--t2)' }}>· {s.cmt}</span>}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function WSDashboard({ mods, onLaunchProfile }) {
  const S = ARS_STATION;
  const [qsos, setQsos] = React.useState(WS_QSOS);
  const [freshId, setFreshId] = React.useState(null);
  const [e, setE] = React.useState({ call: '', sent: '59', rcvd: '59', name: '', qth: '', grid: '', cmt: '', freq: S.freq, band: S.band, mode: S.mode });
  const set = (patch) => setE(p => ({ ...p, ...patch }));
  const callRef = React.useRef(null);
  const [step, setStep] = React.useState(1000);
  const [rotorAz, setRotorAz] = React.useState(S.az);
  const nudge = (dir) => { const f = hzToFreq(freqToHz(e.freq) + dir * step); set({ freq: f, band: bandFromFreq(f) }); };

  const info = callInfo(e.call);
  const dupCount = qsos.filter(q => q.call === e.call.toUpperCase().trim()).length;

  const tune = (spot) => {
    set({ call: spot.call, freq: spot.f, band: bandFromFreq(spot.f), mode: spot.mode, sent: defaultRst(spot.mode), rcvd: defaultRst(spot.mode) });
    const ci = callInfo(spot.call); if (ci) { const b = parseInt(ci.bearing, 10); if (!isNaN(b)) setRotorAz(b); }
    if (callRef.current) callRef.current.focus();
  };
  const logQso = () => {
    const c = e.call.trim().toUpperCase(); if (!c) return;
    const t = new Date().toUTCString().slice(17, 22);
    const q = { t, call: c, band: e.band, mode: e.mode, rst: e.rcvd, _id: Date.now() };
    setQsos(qs => [q, ...qs]); setFreshId(q._id);
    set({ call: '', name: '', qth: '', grid: '', cmt: '', sent: defaultRst(e.mode), rcvd: defaultRst(e.mode) });
  };
  const onModeChange = (mode) => set({ mode, sent: defaultRst(mode), rcvd: defaultRst(mode) });
  const onKey = (ev) => { if (ev.key === 'Enter') logQso(); };

  const segs = []; for (let i = 0; i < 13; i++) segs.push(<i key={i} style={{ height: `${36 + i * 5}%`, background: i < (S.smeter + 2) ? (i >= 9 ? 'var(--warn)' : 'var(--ok)') : 'var(--surface-3)' }}></i>);
  const BANDS = ['160', '80', '40', '30', '20', '17', '15', '12', '10', '6'];
  const g = (id, size = 16) => <ARSGlyph id={id} size={size} stroke={1.9} />;

  return (
    <div className="ck">
      <div className="ck-top">
        {ARS_PROFILES.map(p => (
          <button key={p.id} className="ck-prof" style={{ '--h': `var(--${p.hue})` }} onClick={() => onLaunchProfile(p)} title={p.sub}>
            <span className="ck-pdot"></span><span className="ck-pname">{p.name}</span>
          </button>
        ))}
        <div className="ck-live">
          <span>Cluster <span className="ars-dot on" style={{ display: 'inline-block', marginLeft: 2 }}></span> <span className="ars-mono">{S.spots}/hr</span></span>
          <span>UTC <span className="ars-mono">{S.utc}</span></span>
        </div>
      </div>

      <div className="ck-grid">
        {/* COLUMN 1 — entry pane + log */}
        <div>
          <div className="ep">
            <div className="ep-head">
              <div className="ep-icon">{g('log', 18)}</div>
              <div><div className="ep-title">J-Log · new QSO</div><div className="ep-sub">1,284 today · 38/hr · CQ WW DX</div></div>
              <button className="ars-btn ghost ep-open">Open full J-Log →</button>
            </div>
            <div className="ci">
              {info ? (
                <React.Fragment>
                  <span className="ci-ent">{info.entity}</span>
                  <div className="ci-meta">
                    <span>CQ<b>{info.cqz}</b></span>
                    <span>Bearing<b>{info.bearing}</b></span>
                    <span>Dist<b>{info.dist}</b></span>
                  </div>
                  <span className={`ci-worked ${dupCount ? 'dup' : 'new'}`}>{dupCount ? `Worked ${dupCount}× · new on ${e.band}` : 'New one!'}</span>
                </React.Fragment>
              ) : <span className="ci-empty">Enter a callsign for DXCC, beam heading & worked-before…</span>}
            </div>
            <div className="ep-grid">
              <div className="ep-fld"><span className="fld-lbl">Callsign</span><input ref={callRef} className="ep-in mono call" value={e.call} onChange={ev => set({ call: ev.target.value })} onKeyDown={onKey} placeholder="—" autoFocus /></div>
              <div className="ep-fld"><span className="fld-lbl">Freq (MHz)</span><input className="ep-in mono freq" value={e.freq} onChange={ev => set({ freq: ev.target.value, band: bandFromFreq(ev.target.value) })} onKeyDown={onKey} /></div>
              <div className="ep-fld"><span className="fld-lbl">Mode</span><select className="ep-sel" value={e.mode} onChange={ev => onModeChange(ev.target.value)}>{['USB', 'LSB', 'CW', 'FT8', 'RTTY'].map(m => <option key={m}>{m}</option>)}</select></div>
              <div className="ep-fld"><span className="fld-lbl">RST ↑</span><input className="ep-in mono rst" value={e.sent} onChange={ev => set({ sent: ev.target.value })} onKeyDown={onKey} /></div>
              <div className="ep-fld"><span className="fld-lbl">RST ↓</span><input className="ep-in mono rst" value={e.rcvd} onChange={ev => set({ rcvd: ev.target.value })} onKeyDown={onKey} /></div>
              <div className="ep-fld grow"><span className="fld-lbl">Name</span><input className="ep-in" value={e.name} onChange={ev => set({ name: ev.target.value })} onKeyDown={onKey} /></div>
              <div className="ep-fld grow"><span className="fld-lbl">QTH</span><input className="ep-in" value={e.qth} onChange={ev => set({ qth: ev.target.value })} onKeyDown={onKey} /></div>
              <div className="ep-fld"><span className="fld-lbl">Grid</span><input className="ep-in mono grid" value={e.grid} onChange={ev => set({ grid: ev.target.value })} onKeyDown={onKey} /></div>
              <div className="ep-fld grow" style={{ flexBasis: '100%', minWidth: 0 }}><span className="fld-lbl">Comment</span>
                <div style={{ display: 'flex', gap: 11 }}>
                  <input className="ep-in" style={{ flex: 1 }} value={e.cmt} onChange={ev => set({ cmt: ev.target.value })} onKeyDown={onKey} placeholder="notes, QSL info…" />
                  <button className="ars-btn ghost ep-clear" onClick={() => set({ call: '', name: '', qth: '', grid: '', cmt: '' })}>Clear</button>
                  <button className="ep-logbtn" onClick={logQso}>Log QSO ⏎</button>
                </div>
              </div>
            </div>
          </div>

          <div className="ml">
            <div className="ml-slim">{g('log', 15)} Today's log <span style={{ color: 'var(--t4)', fontWeight: 400 }}>· {qsos.length} shown</span></div>
            <div className="ml-tablehead"><span>UTC</span><span>Call</span><span>Freq</span><span>Band</span><span>Mode</span><span>RST</span></div>
            <div className="ml-list ars-scroll">
              {qsos.map((q, i) => (
                <div key={q._id || i} className={`ml-row ${q._id && q._id === freshId ? 'fresh' : ''}`}>
                  <span className="ml-mono">{q.t}</span>
                  <span className="ml-call">{q.call}</span>
                  <span className="ml-mono" style={{ color: 'var(--t3)' }}>{q.freq || S.freq}</span>
                  <span className="ml-mono">{q.band}</span>
                  <span className="ml-mode">{q.mode}</span>
                  <span className="ml-mono">{q.rst}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* COLUMN 2 — DX cluster */}
        <DXCluster onTune={tune} />

        {/* COLUMN 3 — drawer widgets */}
        <div className="dw-stack">
          <WSDrawerWidget title="Rig control" hue="bridge" glyph={g('sat')} defaultOpen summary={`${e.freq} ${e.mode}`}>
            <div className="rg-freqrow"><input className="rg-freqin" value={e.freq} onChange={ev => set({ freq: ev.target.value, band: bandFromFreq(ev.target.value) })} onBlur={ev => set({ freq: hzToFreq(freqToHz(ev.target.value)) })} title="Type a frequency" /><span className="rg-u">MHz</span></div>
            <div className="rg-chips"><select className="rg-modesel" value={e.mode} onChange={ev => onModeChange(ev.target.value)}>{['USB', 'LSB', 'CW', 'FT8', 'RTTY', 'AM', 'FM'].map(m => <option key={m}>{m}</option>)}</select><span className="rg-chip">{e.band}</span><span className="rg-chip">VFO A</span></div>
            <div className="rg-tune">
              <button className="rg-nudge" onClick={() => nudge(-1)} title="Tune down"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 6 9 12 15 18"/></svg></button>
              <div className="rg-steps">{[['10', 10], ['100', 100], ['1k', 1000], ['5k', 5000]].map(([l, v]) => <button key={v} className={`rg-step ${step === v ? 'on' : ''}`} onClick={() => setStep(v)}>{l}</button>)}</div>
              <button className="rg-nudge" onClick={() => nudge(1)} title="Tune up"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><polyline points="9 6 15 12 9 18"/></svg></button>
            </div>
            <div className="rg-s">{segs}</div>
            <div className="rg-slabel"><span>S1</span><span>S5</span><span>S9</span><span>+20</span><span>+40</span></div>
            <div className="rg-meters">
              <div className="rg-m"><div className="k">Power</div><div className="v">{S.power} W</div></div>
              <div className="rg-m"><div className="k">SWR</div><div className="v" style={{ color: 'var(--ok)' }}>{S.swr}:1</div></div>
            </div>
            <div className="rg-bands">{BANDS.map(b => <div key={b} className={`rg-band ${b + 'm' === e.band ? 'on' : ''}`} onClick={() => set({ band: b + 'm' })}>{b}</div>)}</div>
          </WSDrawerWidget>

          <WSDrawerWidget title="Rotor control" hue="sat" glyph={g('sat')} defaultOpen summary={`${pad3(rotorAz)}\u00b0 ${azLabel(rotorAz)}`}>
            <div className="ro-top">
              <ARSCompass az={rotorAz} size={104} />
              <div className="ro-info">
                <div className="ro-head ars-mono">{pad3(rotorAz)}<span style={{ fontSize: 14, color: 'var(--t3)' }}>°</span></div>
                <div className="ro-dir">{azLabel(rotorAz)} · short path</div>
                <div className="ro-turn">
                  <button onClick={() => setRotorAz(a => (a - 5 + 360) % 360)} title="Rotate CCW">◀ CCW</button>
                  <button className="stop" title="Stop rotation">Stop</button>
                  <button onClick={() => setRotorAz(a => (a + 5) % 360)} title="Rotate CW">CW ▶</button>
                </div>
              </div>
            </div>
            <div className="ro-slider"><input className="ws-range" type="range" min="0" max="359" value={rotorAz} onChange={ev => setRotorAz(+ev.target.value)} style={{ width: '100%' }} /></div>
            <div className="ro-presets">{[['Europe', 50], ['Africa', 95], ['S. Am', 155], ['Carib', 135], ['Asia', 330], ['Oceania', 250]].map(([l, az]) => <button key={l} className={`ro-preset ${rotorAz === az ? 'on' : ''}`} onClick={() => setRotorAz(az)}>{l}<b>{az}°</b></button>)}</div>
          </WSDrawerWidget>

          <WSDrawerWidget title="Space weather" hue="digi" glyph={g('digi')} defaultOpen summary="SFI 168 · K2">
            <div className="sw-grid">{WS_SOLAR.map(([k, v]) => <div key={k} className="sw-cell"><div className="sw-k">{k}</div><div className="sw-v">{v}</div></div>)}</div>
            <div className="sw-bands">
              <div className="sw-bhead"><span>Band</span><span style={{ textAlign: 'center' }}>Day</span><span style={{ textAlign: 'center' }}>Night</span></div>
              {WS_BANDCOND.map(([b, d, n]) => <div key={b} className="sw-brow"><span className="sw-bname">{b}</span><span className={`sw-cond ${d}`}>{d}</span><span className={`sw-cond ${n}`}>{n}</span></div>)}
            </div>
            <div className="dw-stamp">NOAA SWPC · updated 14:30Z</div>
          </WSDrawerWidget>

          <WSDrawerWidget title="Propagation" hue="map" glyph={g('map')} summary="Best 20m">
            <div className="pr-row"><span className="pr-k">Best band now</span><span className="pr-v" style={{ color: 'var(--ok)' }}>20m → EU</span></div>
            <div className="pr-row"><span className="pr-k">MUF (3000 km)</span><span className="pr-v">28.4 MHz</span></div>
            <div className="pr-row"><span className="pr-k">Gray line</span><span className="pr-v">SR 11:02Z · SS 22:48Z</span></div>
            <div className="pr-row"><span className="pr-k">Sporadic-E</span><span className="pr-v" style={{ color: 'var(--t3)' }}>none</span></div>
            <div className="pr-row"><span className="pr-k">Aurora</span><span className="pr-v" style={{ color: 'var(--ok)' }}>quiet</span></div>
          </WSDrawerWidget>

          <WSDrawerWidget title="Weather · FN20" hue="sat" glyph={g('vault')} summary="14° · NW 12">
            <div className="wx-big"><span className="wx-temp ars-mono">14°</span><span className="wx-cond">Partly cloudy · feels 12°</span></div>
            <div className="wx-row"><span className="wx-k">Wind</span><span className="wx-v">NW 12 mph · g 21</span></div>
            <div className="wx-row"><span className="wx-k">Humidity</span><span className="wx-v">48%</span></div>
            <div className="wx-row"><span className="wx-k">Pressure</span><span className="wx-v">1018 hPa ↑</span></div>
            <div className="wx-row"><span className="wx-k">Visibility</span><span className="wx-v">16 km</span></div>
          </WSDrawerWidget>
        </div>
      </div>
    </div>
  );
}

window.WSDashboard = WSDashboard;
