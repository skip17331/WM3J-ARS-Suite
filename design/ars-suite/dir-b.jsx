/* Direction B — "Mission Control"
   The expressive instrument-panel direction. A live radio "faceplate" is the
   hero: oversized frequency, S-meter, band scope, a rotor compass dial and
   power/SWR wells — the at-a-glance station state a contester glances at all
   night. Modules sit below as a grouped dock (Operating / Tools) with a hue
   glow when live. A single "Station power" control runs the usual setup. */

(function () {
  if (document.getElementById('dir-b-styles')) return;
  const s = document.createElement('style');
  s.id = 'dir-b-styles';
  s.textContent = `
  .b-wrap { flex:1; display:flex; flex-direction:column; min-height:0; background:radial-gradient(120% 90% at 50% -10%, var(--surface-1), var(--bg) 70%); }
  .b-hero { flex:0 0 auto; display:grid; grid-template-columns:1.5fr 0.9fr 1fr; gap:14px; padding:18px 20px; }
  .b-well { background:var(--surface-1); border:1px solid var(--border); border-radius:var(--radius-lg); padding:15px 17px; position:relative; box-shadow:inset 0 1px 0 rgba(255,255,255,0.03); }
  .b-well-k { font-size:9.5px; letter-spacing:1.5px; text-transform:uppercase; color:var(--t3); font-weight:600; margin-bottom:9px; display:flex; align-items:center; gap:7px; }

  .b-freq { font-size:46px; font-weight:600; line-height:0.9; letter-spacing:-0.5px; color:var(--t1); }
  .b-freq .u { font-size:17px; color:var(--accent); margin-left:8px; font-weight:500; }
  .b-modeband { display:flex; gap:8px; margin:12px 0 14px; }
  .b-chip { font-size:12px; font-weight:600; padding:4px 11px; border-radius:6px; background:var(--surface-3); color:var(--t1); border:1px solid var(--border-2); }
  .b-chip.accent { background:var(--accent-dim); color:var(--accent); border-color:var(--accent-line); }

  /* S-meter */
  .b-smeter { display:flex; align-items:flex-end; gap:3px; height:26px; margin-top:4px; }
  .b-seg { flex:1; border-radius:2px; background:var(--surface-3); }
  .b-seg.lit { background:var(--ok); }
  .b-seg.lit.hi { background:var(--warn); }
  .b-slabels { display:flex; justify-content:space-between; font-size:9px; color:var(--t4); margin-top:5px; }

  .b-compass { display:flex; flex-direction:column; align-items:center; justify-content:center; }
  .b-az { font-size:30px; font-weight:600; margin-top:8px; line-height:1; }
  .b-az .u { font-size:13px; color:var(--t3); margin-left:4px; }
  .b-azsub { font-size:11px; color:var(--t3); margin-top:3px; letter-spacing:1px; }

  .b-gauges { display:flex; flex-direction:column; gap:13px; }
  .b-gauge { }
  .b-gauge-top { display:flex; justify-content:space-between; align-items:baseline; margin-bottom:6px; }
  .b-gauge-v { font-size:18px; font-weight:600; }
  .b-gauge-v .u { font-size:11px; color:var(--t3); margin-left:2px; }
  .b-bar { height:6px; border-radius:99px; background:var(--surface-3); overflow:hidden; }
  .b-bar > i { display:block; height:100%; border-radius:99px; }
  .b-power-line { display:flex; align-items:center; justify-content:space-between; margin-top:auto; padding-top:13px; }

  .b-pwr { display:flex; align-items:center; gap:11px; }
  .b-pwrbtn { width:42px; height:42px; border-radius:50%; border:2px solid var(--ok); background:color-mix(in oklch,var(--ok) 16%, transparent); color:var(--ok); display:flex; align-items:center; justify-content:center; cursor:pointer; flex:0 0 auto; transition:all .15s; }
  .b-pwrbtn.off { border-color:var(--border-2); background:var(--surface-3); color:var(--t3); }
  .b-pwrbtn:hover { filter:brightness(1.15); }
  .b-pwr-state { font-size:14px; font-weight:600; }
  .b-pwr-sub { font-size:10.5px; color:var(--t3); }

  /* module dock */
  .b-dock { flex:1; overflow:auto; padding:4px 20px 18px; }
  .b-group { margin-bottom:6px; }
  .b-glabel { display:flex; align-items:center; gap:10px; margin:14px 0 11px; }
  .b-glabel .ln { flex:1; height:1px; background:var(--border); }
  .b-tiles { display:grid; grid-template-columns:repeat(4,1fr); gap:13px; }
  .b-tile { background:var(--surface-1); border:1px solid var(--border); border-radius:var(--radius); padding:15px; cursor:pointer; position:relative; transition:transform .14s, border-color .14s, box-shadow .14s; overflow:hidden; }
  .b-tile:hover { transform:translateY(-2px); border-color:var(--border-glow); }
  .b-tile.run { border-color:color-mix(in oklch,var(--h) 42%, var(--border)); box-shadow:0 0 0 1px color-mix(in oklch,var(--h) 30%, transparent), 0 8px 26px -12px var(--h); background:color-mix(in oklch,var(--h) 8%, var(--surface-1)); }
  .b-tglow { position:absolute; inset:0; opacity:0; background:radial-gradient(80% 60% at 18% 0%, color-mix(in oklch,var(--h) 22%, transparent), transparent 70%); pointer-events:none; }
  .b-tile.run .b-tglow { opacity:1; }
  .b-tile-top { display:flex; align-items:center; justify-content:space-between; margin-bottom:13px; }
  .b-ticon { width:44px; height:44px; border-radius:11px; display:flex; align-items:center; justify-content:center; background:color-mix(in oklch,var(--h) 16%, var(--surface-3)); color:var(--h); border:1px solid color-mix(in oklch,var(--h) 26%, transparent); }
  .b-tname { font-size:15px; font-weight:600; position:relative; }
  .b-ttag { font-size:11px; color:var(--t3); margin-top:1px; position:relative; }
  .b-tstat { font-size:11px; margin-top:10px; min-height:15px; position:relative; }
  .b-tstat.run { color:var(--h); }
  .b-tstat.stop { color:var(--t3); }
  .b-tstate { position:absolute; top:15px; right:15px; font-size:9.5px; font-weight:700; letter-spacing:.8px; text-transform:uppercase; color:var(--t4); }
  .b-tstate.on { color:var(--ok); }
  `;
  document.head.appendChild(s);
})();

function ARSDirectionB({ theme = 'dark' }) {
  const [mods, setMods] = React.useState(() => ARS_MODULES.map(m => ({ ...m })));
  const [power, setPower] = React.useState(true);
  const toggle = (id) => setMods(ms => ms.map(m => m.id === id ? { ...m, running: !m.running } : m));
  const togglePower = () => {
    const next = !power; setPower(next);
    setMods(ms => ms.map(m => ['log', 'map', 'bridge'].includes(m.id) ? { ...m, running: next } : m));
  };
  const S = ARS_STATION;
  const operating = mods.filter(m => ['log', 'map', 'bridge', 'sat'].includes(m.id));
  const tools = mods.filter(m => ['digi', 'vault', 'learn'].includes(m.id));

  const segs = [];
  for (let i = 0; i < 13; i++) segs.push(<div key={i} className={`b-seg ${i < (S.smeter + 2) ? 'lit' : ''} ${i >= 9 ? 'hi' : ''}`} style={{ height: `${40 + i * 4.5}%` }}></div>);

  const Tile = (m) => (
    <div key={m.id} className={`b-tile ${m.running ? 'run' : ''}`} style={{ '--h': `var(--${m.hue})` }} onClick={() => toggle(m.id)}>
      <div className="b-tglow"></div>
      <div className="b-tstate" style={{}}>
        <span className={m.running ? 'on' : ''}>{m.running ? '● live' : '○ off'}</span>
      </div>
      <div className="b-tile-top"><div className="b-ticon"><ARSGlyph id={m.id} size={23} /></div></div>
      <div className="b-tname">{m.name}</div>
      <div className="b-ttag">{m.desc}</div>
      <div className={`b-tstat ${m.running ? 'run' : 'stop'} ars-mono`}>{m.running ? m.stat : 'Click to launch'}</div>
    </div>
  );

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label="Launcher · B Mission Control">
      <div className="ars-win">
        <div className="ars-titlebar">
          <div className="ars-tb-mark"><span className="ars-tb-dot"></span>J-Hub <span className="ars-tb-sub">Mission Control</span></div>
          <div className="ars-tb-spacer"></div>
          <div className="ars-tb-sub ars-mono">{S.call} · {S.grid} · {S.utc}Z</div>
          <div className="ars-tb-ctl"><span>—</span><span>▢</span><span>✕</span></div>
        </div>

        <div className="b-wrap ars-scroll">
          {/* faceplate hero */}
          <div className="b-hero">
            {/* freq + S-meter + scope */}
            <div className="b-well" style={{ display: 'flex', flexDirection: 'column' }}>
              <div className="b-well-k">Transceiver · IC-7610</div>
              <div className="b-freq ars-mono">{S.freq}<span className="u">MHz</span></div>
              <div className="b-modeband"><span className="b-chip accent">{S.mode}</span><span className="b-chip">{S.band}</span><span className="b-chip">SPLIT OFF</span></div>
              <div style={{ marginTop: 'auto' }}>
                <div style={{ borderTop: '1px solid var(--border)', paddingTop: '11px' }}>
                  <div className="b-well-k" style={{ marginBottom: '7px' }}>S-Meter · Band activity</div>
                  <div className="b-smeter">{segs}</div>
                  <div className="b-slabels ars-mono"><span>S1</span><span>S3</span><span>S5</span><span>S7</span><span>S9</span><span>+20</span><span>+40</span></div>
                  <div style={{ marginTop: '10px' }}><ARSBandScope w={360} h={32} /></div>
                </div>
              </div>
            </div>

            {/* compass */}
            <div className="b-well b-compass">
              <div className="b-well-k" style={{ alignSelf: 'flex-start' }}>Rotor · Yaesu G-1000</div>
              <ARSCompass az={S.az} size={120} />
              <div className="b-az ars-mono">{S.rotor}<span className="u">°</span></div>
              <div className="b-azsub">{S.rotorName} · TRACKING</div>
            </div>

            {/* gauges + power */}
            <div className="b-well" style={{ display: 'flex', flexDirection: 'column' }}>
              <div className="b-well-k">Output · Antenna</div>
              <div className="b-gauges">
                <div className="b-gauge">
                  <div className="b-gauge-top"><span className="ars-eyebrow" style={{ letterSpacing: '1px' }}>Power</span><span className="b-gauge-v ars-mono">{S.power}<span className="u">W</span></span></div>
                  <div className="b-bar"><i style={{ width: '62%', background: 'var(--accent)' }}></i></div>
                </div>
                <div className="b-gauge">
                  <div className="b-gauge-top"><span className="ars-eyebrow" style={{ letterSpacing: '1px' }}>SWR</span><span className="b-gauge-v ars-mono">{S.swr}:1</span></div>
                  <div className="b-bar"><i style={{ width: '16%', background: 'var(--ok)' }}></i></div>
                </div>
                <div className="b-gauge">
                  <div className="b-gauge-top"><span className="ars-eyebrow" style={{ letterSpacing: '1px' }}>Cluster</span><span className="b-gauge-v ars-mono" style={{ fontSize: '14px', color: 'var(--ok)' }}>● {S.spots}/hr</span></div>
                  <div className="b-bar"><i style={{ width: '78%', background: 'var(--map)' }}></i></div>
                </div>
              </div>
              <div className="b-power-line">
                <div className="b-pwr">
                  <button className={`b-pwrbtn ${power ? '' : 'off'}`} onClick={togglePower} title="Power on usual setup">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"><path d="M12 3v9"/><path d="M6.5 7a8 8 0 1011 0"/></svg>
                  </button>
                  <div><div className="b-pwr-state">{power ? 'ON AIR' : 'STANDBY'}</div><div className="b-pwr-sub">Contest Day setup</div></div>
                </div>
              </div>
            </div>
          </div>

          {/* module dock */}
          <div className="b-dock">
            <div className="b-group">
              <div className="b-glabel"><span className="ars-eyebrow">Operating</span><span className="ln"></span></div>
              <div className="b-tiles">{operating.map(Tile)}</div>
            </div>
            <div className="b-group">
              <div className="b-glabel"><span className="ars-eyebrow">Tools & reference</span><span className="ln"></span></div>
              <div className="b-tiles">{tools.map(Tile)}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

window.ARSDirectionB = ARSDirectionB;
