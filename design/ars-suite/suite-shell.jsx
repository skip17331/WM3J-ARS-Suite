/* ARS Suite — shared shell: a left module-launcher DOCK and a reusable
   collapsible DRAWER, used across modules (J-Map, J-Log, …). Requires
   ARS_MODULES + ARSGlyph from ars-shared.jsx. Exported to window. */

const SX_FILES = { hub: 'J-Hub Workspace.html', log: 'J-Log Cockpit.html', map: 'J-Map.html', digi: 'J-Digi.html', bridge: 'J-Bridge.html', sat: 'J-Sat.html', vault: 'J-Vault.html', learn: 'J-Learn.html' };

function SuiteDock({ active }) {
  const g = (id, size = 20) => <ARSGlyph id={id} size={size} stroke={1.9} />;
  const item = (id, name, hue, running) => {
    const file = SX_FILES[id];
    const isActive = active === id;
    const cls = `sx-dock-item ${isActive ? 'active' : ''} ${file ? '' : 'disabled'}`;
    const inner = (
      <React.Fragment>
        <span className="sx-dock-ic" style={{ '--h': `var(--${hue})`, opacity: running || isActive ? 1 : 0.62 }}>{g(id)}</span>
        <span className="sx-dock-name">{name}</span>
        <span className={`sx-dock-dot ${running ? 'on' : 'off'}`}></span>
      </React.Fragment>
    );
    return file && !isActive
      ? <a key={id} className={cls} href={file} style={{ '--h': `var(--${hue})` }} title={`Open ${name}`}>{inner}</a>
      : <div key={id} className={cls} style={{ '--h': `var(--${hue})` }} title={file ? name : `${name} — not built yet`}>{inner}</div>;
  };
  return (
    <div className="sx-dock">
      {item('hub', 'J-Hub', 'hub', true)}
      <div className="sx-dock-sec">Modules</div>
      <div className="sx-dock-list">
        {ARS_MODULES.map(m => item(m.id, m.name, m.hue, m.running))}
      </div>
      <div className="sx-dock-sp"></div>
      <div className="sx-dock-foot">
        <a className="sx-dock-item" href={SX_FILES.hub} title="Station settings">
          <span className="sx-dock-ic" style={{ '--h': 'var(--t3)', background: 'var(--surface-3)', color: 'var(--t2)', borderColor: 'var(--border-2)' }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 1 1-4 0v-.1A1.6 1.6 0 0 0 7 19.4a1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0-1.1-2.7H1a2 2 0 1 1 0-4h.1A1.6 1.6 0 0 0 2.6 7a1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H7a1.6 1.6 0 0 0 1-1.5V1a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 2.7 1.1l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V7a1.6 1.6 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z"/></svg>
          </span>
          <span className="sx-dock-name">Station settings</span>
        </a>
      </div>
    </div>
  );
}

function SuiteDrawer({ title, hue = 'accent', glyph, summary, defaultOpen, children, accent }) {
  const [open, setOpen] = React.useState(!!defaultOpen);
  return (
    <div className={`sx-dw ${open ? 'open' : ''}`} style={{ '--h': `var(--${hue})` }}>
      <button className="sx-dw-head" onClick={() => setOpen(o => !o)}>
        {glyph && <span className="sx-dw-ic">{glyph}</span>}
        <span className="sx-dw-t">{title}</span>
        {!open && summary && <span className="sx-dw-sum">{summary}</span>}
        <span className="sx-dw-cv"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"><polyline points="9 6 15 12 9 18"/></svg></span>
      </button>
      <div className="sx-dw-wrap"><div className="sx-dw-inner"><div className="sx-dw-body">{children}</div></div></div>
    </div>
  );
}

/* shared instrument data */
const SX_ROTOR_PRESETS = [['EU', 50], ['AF', 95], ['SA', 155], ['Carib', 135], ['AS', 330], ['OC', 250]];
const SX_SOLAR = [['SFI', '168'], ['SN', '142'], ['A', '7'], ['K', '2'], ['X-ray', 'B1'], ['304Å', '171'], ['Aur', '1.5'], ['MUF', '28']];
const SX_BANDCOND = [['80–40m', 'good', 'good'], ['30–20m', 'good', 'fair'], ['17–15m', 'fair', 'poor'], ['12–10m', 'fair', 'poor']];

/* Reusable instrument drawers (rotor · propagation · space wx · weather).
   Manages its own rotor heading. Drop into any module's <div className="sx-rail">. */
function SuiteInstruments({ defaultRotor = 50 }) {
  const [az, setAz] = React.useState(defaultRotor);
  const g = (id, size = 14) => <ARSGlyph id={id} size={size} stroke={1.9} />;
  const dir = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'][Math.round((((az % 360) + 360) % 360) / 45) % 8];
  const az3 = String(((az % 360) + 360) % 360).padStart(3, '0');
  return (
    <React.Fragment>
      <SuiteDrawer title="Antenna · Rotor" hue="sat" glyph={g('sat')} summary={`${az3}° ${dir}`}>
        <div className="sx-ro-top">
          <ARSCompass az={az} size={88} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="sx-ro-head">{az3}<span style={{ fontSize: 13, color: 'var(--t3)' }}>°</span></div>
            <div className="sx-ro-dir">{dir} · short path</div>
            <div className="sx-ro-turn">
              <button onClick={() => setAz(a => (a - 5 + 360) % 360)}>◀</button>
              <button className="stop">Stop</button>
              <button onClick={() => setAz(a => (a + 5) % 360)}>▶</button>
            </div>
          </div>
        </div>
        <input className="ws-range" type="range" min="0" max="359" value={az} onChange={e => setAz(+e.target.value)} style={{ width: '100%' }} />
        <div className="sx-ro-presets">{SX_ROTOR_PRESETS.map(([l, a]) => <button key={l} className={`sx-ro-preset ${az === a ? 'on' : ''}`} onClick={() => setAz(a)}>{l}<b>{a}°</b></button>)}</div>
      </SuiteDrawer>
      <SuiteDrawer title="Propagation" hue="map" glyph={g('map')} summary="20m → EU">
        <div className="sx-kv"><span className="k">Best band now</span><span className="v" style={{ color: 'var(--ok)' }}>20m → EU</span></div>
        <div className="sx-kv"><span className="k">MUF (3000 km)</span><span className="v">28.4 MHz</span></div>
        <div className="sx-kv"><span className="k">Gray line</span><span className="v">SR 11:02 · SS 22:48</span></div>
        <div className="sx-kv"><span className="k">Aurora</span><span className="v" style={{ color: 'var(--ok)' }}>quiet</span></div>
      </SuiteDrawer>
      <SuiteDrawer title="Space weather" hue="digi" glyph={g('digi')} summary="SFI 168 · K2">
        <div className="sx-swx">{SX_SOLAR.map(([k, v]) => <div key={k} className="sx-swx-c"><div className="sx-swx-k">{k}</div><div className="sx-swx-v">{v}</div></div>)}</div>
        {SX_BANDCOND.map(([b, d, n]) => <div key={b} className="sx-bc"><span className="bn">{b}</span><span className={`c ${d}`}>{d}</span><span className={`c ${n}`}>{n}</span></div>)}
        <div className="sx-dw-stamp">NOAA SWPC · 14:30Z</div>
      </SuiteDrawer>
      <SuiteDrawer title="Weather · FN20" hue="bridge" glyph={g('bridge')} summary="14° NW12">
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, margin: '8px 0 10px' }}><span className="ars-mono" style={{ fontSize: 28, fontWeight: 600 }}>14°</span><span style={{ fontSize: 12, color: 'var(--t2)' }}>Partly cloudy · feels 12°</span></div>
        <div className="sx-kv"><span className="k">Wind</span><span className="v">NW 12 · g21</span></div>
        <div className="sx-kv"><span className="k">Humidity</span><span className="v">48%</span></div>
        <div className="sx-kv"><span className="k">Pressure</span><span className="v">1018 hPa ↑</span></div>
      </SuiteDrawer>
    </React.Fragment>
  );
}

/* Reusable spectrogram/waterfall on a canvas. mode tweaks the synthetic signal. */
function SuiteWaterfall({ height = 150, signals = [], mode = 'generic' }) {
  const ref = React.useRef(null);
  React.useEffect(() => {
    const cv = ref.current; if (!cv) return;
    const W = cv.width, H = cv.height, ctx = cv.getContext('2d');
    const rnd = (s) => { let x = Math.sin(s * 12.9898) * 43758.5453; return x - Math.floor(x); };
    const img = ctx.createImageData(W, H);
    for (let y = 0; y < H; y++) {
      for (let x = 0; x < W; x++) {
        let v = rnd(x * 0.7 + y * 3.1) * 0.18; // noise floor
        for (const s of signals) {
          const d = Math.abs(x - s.x * W);
          if (d < s.w) { const fall = 1 - d / s.w; const tmod = (mode === 'ft8') ? (0.6 + 0.4 * Math.sin((y / H) * Math.PI * 2 + s.x * 9)) : (mode === 'cw' ? ((Math.floor(y / 7) + Math.floor(s.x * 30)) % 2 ? 1 : 0.15) : 0.9); v = Math.max(v, fall * s.i * tmod); }
        }
        const i = (y * W + x) * 4;
        // dark-blue → cyan → amber intensity ramp
        const t = Math.min(1, v);
        img.data[i] = 20 + t * 235; img.data[i + 1] = 30 + t * 180; img.data[i + 2] = 60 + (1 - t) * 90 + t * 40; img.data[i + 3] = 255;
      }
    }
    ctx.putImageData(img, 0, 0);
  }, []);
  return <canvas ref={ref} width="760" height={height} className="sx-wf-canvas"></canvas>;
}

Object.assign(window, { SuiteDock, SuiteDrawer, SuiteInstruments, SuiteWaterfall });