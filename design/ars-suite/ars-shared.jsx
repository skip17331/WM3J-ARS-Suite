/* ARS Suite — shared data, icons, helpers. Exported to window for the
   direction components. Loaded as a babel script. */

const ARS_MODULES = [
  { id: 'log',    name: 'J-Log',    tag: 'Logging',     hue: 'log',    desc: 'Contest & general logging',  running: true,  stat: '1,284 QSOs · today', heavy: true },
  { id: 'map',    name: 'J-Map',    tag: 'Propagation', hue: 'map',    desc: 'Propagation & spot map',     running: true,  stat: '342 spots / hr' },
  { id: 'bridge', name: 'J-Bridge', tag: 'WSJT-X',      hue: 'bridge', desc: 'WSJT-X bridge',              running: true,  stat: 'FT8 · 14.074 MHz' },
  { id: 'digi',   name: 'J-Digi',   tag: 'Digital',     hue: 'digi',   desc: 'Digital modes decode',       running: false, stat: 'CW · RTTY · PSK' },
  { id: 'sat',    name: 'J-Sat',    tag: 'Satellite',   hue: 'sat',    desc: 'Satellite tracking',         running: false, stat: 'AO-91 AOS in 12m' },
  { id: 'vault',  name: 'J-Vault',  tag: 'Inventory',   hue: 'vault',  desc: 'Station inventory & estate', running: false, stat: '64 items · 3 rigs' },
  { id: 'learn',  name: 'J-Learn',  tag: 'Reference',   hue: 'learn',  desc: 'Reference & learning',       running: false, stat: '12 study decks' },
];

// "Start my usual setup" profiles — each launches a named set of modules.
const ARS_PROFILES = [
  { id: 'contest', name: 'Contest Day',   sub: 'Log · Map · Bridge · Cluster',  mods: ['log','map','bridge'], hue: 'log' },
  { id: 'dx',      name: 'Casual DX',     sub: 'Log · Map · Cluster',           mods: ['log','map'],          hue: 'map' },
  { id: 'digital', name: 'Digital',       sub: 'Bridge · Digi · Log',           mods: ['bridge','digi','log'],hue: 'digi' },
  { id: 'sat',     name: 'Satellite Pass',sub: 'Sat · Map · Log',               mods: ['sat','map','log'],    hue: 'sat' },
];

const ARS_ACTIVITY = [
  { t: '14:42', txt: 'Worked DL8WPX on 20m FT8', tag: 'J-Log',    hue: 'log' },
  { t: '14:38', txt: 'New spot — VK9DX 14.022 CW', tag: 'Cluster', hue: 'map' },
  { t: '14:31', txt: 'Rotor parked to 045° NE',     tag: 'Rotor',   hue: 'sat' },
  { t: '14:09', txt: 'AO-91 pass logged — 4 QSOs',  tag: 'J-Sat',   hue: 'sat' },
  { t: '13:55', txt: 'Bridge synced WSJT-X v2.7',   tag: 'J-Bridge',hue: 'bridge' },
];

const ARS_HEALTH = [
  { name: 'Rig — IC-7610',       via: 'USB · CI-V 19200', ok: true },
  { name: 'Rotor — Yaesu G-1000',via: 'GS-232 · COM4',    ok: true },
  { name: 'Amp — SPE 1.3K',      via: 'CAT · standby',    ok: true },
  { name: 'Ant. Switch — 6×1',   via: 'port 3 active',    ok: true },
  { name: 'DX Cluster',          via: 'dxc.ve7cc.net',    ok: true },
  { name: 'Internet / CAT time', via: 'NTP synced',       ok: false },
];

// Live station telemetry snapshot.
const ARS_STATION = {
  call: 'WM3J', grid: 'FN20',
  freq: '14.074.00', mode: 'USB', band: '20m',
  rotor: '045', rotorName: 'NE', az: 45,
  power: '92', swr: '1.2', cluster: 'CONNECTED', spots: '342',
  utc: '14:42:07', smeter: 7, // S7
};

/* ---- module glyphs — deliberately simple geometric marks --------------- */
function ARSGlyph({ id, size = 20, stroke = 2 }) {
  const p = { fill: 'none', stroke: 'currentColor', strokeWidth: stroke, strokeLinecap: 'round', strokeLinejoin: 'round' };
  const map = {
    log: (<g {...p}><line x1="5" y1="7" x2="19" y2="7"/><line x1="5" y1="12" x2="19" y2="12"/><line x1="5" y1="17" x2="14" y2="17"/></g>),
    map: (<g {...p}><circle cx="12" cy="12" r="7.5"/><line x1="12" y1="3" x2="12" y2="21"/><line x1="3.5" y1="12" x2="20.5" y2="12"/><path d="M12 4.5c3 2.4 3 12.6 0 15M12 4.5c-3 2.4-3 12.6 0 15"/></g>),
    bridge: (<g {...p}><circle cx="6" cy="12" r="2.4"/><circle cx="18" cy="12" r="2.4"/><line x1="8.4" y1="12" x2="15.6" y2="12"/><path d="M4 8.5a5 5 0 000 7M20 8.5a5 5 0 010 7"/></g>),
    digi: (<g {...p}><line x1="5" y1="12" x2="5" y2="15"/><line x1="9" y1="9" x2="9" y2="18"/><line x1="12" y1="5" x2="12" y2="19"/><line x1="15" y1="9" x2="15" y2="18"/><line x1="19" y1="12" x2="19" y2="15"/></g>),
    sat: (<g {...p}><ellipse cx="12" cy="12" rx="9" ry="4.5" transform="rotate(-30 12 12)"/><circle cx="12" cy="12" r="2.4" fill="currentColor"/></g>),
    vault: (<g {...p}><rect x="4.5" y="6" width="15" height="13" rx="2"/><path d="M9 6V4.5h6V6"/><circle cx="12" cy="12.5" r="2"/></g>),
    learn: (<g {...p}><path d="M5 5.5h7a2.5 2.5 0 012.5 2.5v11a2 2 0 00-2-2H5z"/><path d="M19 5.5h-4.5"/><path d="M14.5 8v11"/></g>),
    hub: (<g {...p}><circle cx="12" cy="12" r="2.6" fill="currentColor"/><circle cx="12" cy="12" r="7.5"/><line x1="12" y1="4.5" x2="12" y2="2.5"/><line x1="12" y1="21.5" x2="12" y2="19.5"/></g>),
  };
  return (<svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">{map[id] || map.hub}</svg>);
}

// rotor compass dial — simple SVG, JavaFX-safe (no filters)
function ARSCompass({ az = 45, size = 96 }) {
  const r = size / 2 - 8, cx = size / 2, cy = size / 2;
  const rad = (az - 90) * Math.PI / 180;
  const nx = cx + Math.cos(rad) * (r - 6), ny = cy + Math.sin(rad) * (r - 6);
  const ticks = [];
  for (let i = 0; i < 36; i++) {
    const a = (i * 10 - 90) * Math.PI / 180;
    const major = i % 9 === 0;
    const r1 = r, r2 = r - (major ? 7 : 3.5);
    ticks.push(<line key={i} x1={cx + Math.cos(a) * r1} y1={cy + Math.sin(a) * r1} x2={cx + Math.cos(a) * r2} y2={cy + Math.sin(a) * r2} stroke="var(--t4)" strokeWidth={major ? 1.6 : 1} />);
  }
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      <circle cx={cx} cy={cy} r={r} fill="none" stroke="var(--border-2)" strokeWidth="1.5" />
      {ticks}
      <line x1={cx} y1={cy} x2={nx} y2={ny} stroke="var(--sat)" strokeWidth="2.5" strokeLinecap="round" />
      <circle cx={nx} cy={ny} r="3" fill="var(--sat)" />
      <circle cx={cx} cy={cy} r="2.5" fill="var(--t3)" />
    </svg>
  );
}

// tiny band-scope sparkline (deterministic pseudo-random)
function ARSBandScope({ w = 220, h = 40, color = 'var(--accent)' }) {
  const n = 60, pts = [];
  for (let i = 0; i < n; i++) {
    const base = Math.sin(i * 0.5) * 0.2 + Math.sin(i * 0.17) * 0.25;
    const spike = (i % 11 === 0 || i % 17 === 0) ? 0.55 : 0;
    const v = Math.min(1, Math.abs(base) + spike + 0.12);
    pts.push(`${(i / (n - 1)) * w},${h - v * h}`);
  }
  return (
    <svg width={w} height={h} viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none" style={{ display: 'block' }}>
      <polyline points={pts.join(' ')} fill="none" stroke={color} strokeWidth="1.4" opacity="0.9" />
    </svg>
  );
}

Object.assign(window, { ARS_MODULES, ARS_PROFILES, ARS_ACTIVITY, ARS_HEALTH, ARS_STATION, ARSGlyph, ARSCompass, ARSBandScope });
