/* Direction A — "Command Console"
   Restrained evolution of today's launcher. A calm telemetry strip on top,
   a clean color-coded module grid as the main act, and a working right rail
   (setup profiles · activity · connection health). The safe, by-the-book
   direction that an alpha user would instantly recognize as "the same app,
   but finished." */

(function () {
  if (document.getElementById('dir-a-styles')) return;
  const s = document.createElement('style');
  s.id = 'dir-a-styles';
  s.textContent = `
  .a-body { flex:1; display:grid; grid-template-columns:1fr 332px; min-height:0; }
  .a-main { padding:18px 20px; overflow:auto; }
  .a-rail { border-left:1px solid var(--border); background:var(--surface-1); padding:16px 16px 4px; overflow:auto; display:flex; flex-direction:column; gap:18px; }

  /* station strip */
  .a-strip { flex:0 0 auto; display:flex; align-items:stretch; gap:0; background:var(--surface-1); border-bottom:1px solid var(--border); padding:0 6px; }
  .a-tele { display:flex; flex-direction:column; justify-content:center; gap:3px; padding:11px 18px; position:relative; }
  .a-tele + .a-tele::before { content:''; position:absolute; left:0; top:22%; height:56%; width:1px; background:var(--border); }
  .a-tele-k { font-size:9.5px; letter-spacing:1.3px; text-transform:uppercase; color:var(--t3); font-weight:600; }
  .a-tele-v { font-size:17px; font-weight:600; color:var(--t1); line-height:1; }
  .a-tele-v .u { font-size:11px; color:var(--t3); margin-left:3px; font-weight:500; }
  .a-strip-sp { flex:1; }

  .a-sec-head { display:flex; align-items:center; gap:12px; margin-bottom:14px; }
  .a-sec-title { font-size:14px; font-weight:600; }
  .a-pill { font-size:11px; padding:3px 9px; border-radius:99px; background:var(--surface-3); color:var(--t2); border:1px solid var(--border); }
  .a-pill .ars-dot { display:inline-block; margin-right:5px; vertical-align:middle; }

  .a-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:12px; }
  .a-card { background:var(--surface-2); border:1px solid var(--border); border-radius:var(--radius); padding:14px; position:relative; overflow:hidden; transition:border-color .14s, background .14s; }
  .a-card:hover { border-color:var(--border-glow); }
  .a-card.run { background:color-mix(in oklch, var(--h) 7%, var(--surface-2)); border-color:color-mix(in oklch, var(--h) 30%, var(--border)); }
  .a-card.run::before { content:''; position:absolute; left:0; top:0; bottom:0; width:3px; background:var(--h); }
  .a-ctop { display:flex; align-items:flex-start; gap:11px; margin-bottom:11px; }
  .a-icon { width:38px; height:38px; border-radius:9px; display:flex; align-items:center; justify-content:center; flex:0 0 auto; background:color-mix(in oklch, var(--h) 18%, var(--surface-3)); color:var(--h); border:1px solid color-mix(in oklch, var(--h) 28%, transparent); }
  .a-name { font-size:14.5px; font-weight:600; line-height:1.15; }
  .a-tag { font-size:11px; color:var(--t3); margin-top:1px; }
  .a-cstat { display:flex; align-items:center; gap:6px; font-size:11.5px; color:var(--t2); min-height:16px; }
  .a-cstat .v { color:var(--h); }
  .a-cfoot { display:flex; align-items:center; justify-content:space-between; margin-top:12px; }
  .a-state { font-size:11px; font-weight:600; letter-spacing:.6px; text-transform:uppercase; color:var(--t3); display:flex; align-items:center; gap:6px; }
  .a-state.on { color:var(--ok); }

  .a-rail-h { display:flex; align-items:center; justify-content:space-between; margin-bottom:11px; }
  .a-prof { display:flex; flex-direction:column; gap:8px; }
  .a-profbtn { display:flex; align-items:center; gap:11px; text-align:left; padding:10px 11px; border-radius:var(--radius-sm); border:1px solid var(--border); background:var(--surface-2); cursor:pointer; transition:border-color .13s, background .13s; }
  .a-profbtn:hover { border-color:var(--border-glow); background:var(--surface-3); }
  .a-profmark { width:30px; height:30px; border-radius:8px; flex:0 0 auto; display:flex; align-items:center; justify-content:center; background:color-mix(in oklch, var(--h) 16%, var(--surface-3)); color:var(--h); }
  .a-profname { font-size:13px; font-weight:600; }
  .a-profsub { font-size:10.5px; color:var(--t3); margin-top:1px; }
  .a-prof-go { margin-left:auto; color:var(--t4); font-size:15px; }

  .a-feed { display:flex; flex-direction:column; gap:0; }
  .a-feed-item { display:flex; gap:10px; padding:8px 0; border-bottom:1px solid var(--border); }
  .a-feed-item:last-child { border-bottom:none; }
  .a-feed-t { font-size:11px; color:var(--t3); flex:0 0 38px; padding-top:1px; }
  .a-feed-txt { font-size:12px; color:var(--t2); line-height:1.35; }
  .a-feed-tag { font-size:10px; color:var(--h); font-weight:600; }

  .a-health { display:flex; flex-direction:column; gap:0; }
  .a-hrow { display:flex; align-items:center; gap:9px; padding:7px 0; border-bottom:1px solid var(--border); }
  .a-hrow:last-child { border-bottom:none; }
  .a-hname { font-size:12px; color:var(--t1); }
  .a-hvia { font-size:10.5px; color:var(--t3); margin-top:1px; }
  `;
  document.head.appendChild(s);
})();

function ARSDirectionA({ theme = 'dark' }) {
  const [mods, setMods] = React.useState(() => ARS_MODULES.map(m => ({ ...m })));
  const toggle = (id) => setMods(ms => ms.map(m => m.id === id ? { ...m, running: !m.running } : m));
  const launchProfile = (p) => setMods(ms => ms.map(m => p.mods.includes(m.id) ? { ...m, running: true } : m));
  const runningCount = mods.filter(m => m.running).length;
  const S = ARS_STATION;

  const Tele = ({ k, children }) => (
    <div className="a-tele"><div className="a-tele-k">{k}</div><div className="a-tele-v ars-mono">{children}</div></div>
  );

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label="Launcher · A Command Console">
      <div className="ars-win">
        <div className="ars-titlebar">
          <div className="ars-tb-mark"><span className="ars-tb-dot"></span>J-Hub <span className="ars-tb-sub">· {S.call} · {S.grid}</span></div>
          <div className="ars-tb-spacer"></div>
          <div className="ars-tb-sub ars-mono">localhost:8081 · up 4h 12m</div>
          <div className="ars-tb-ctl"><span>—</span><span>▢</span><span>✕</span></div>
        </div>

        {/* live station telemetry strip */}
        <div className="a-strip">
          <Tele k="Rig">{S.freq}<span className="u">MHz</span></Tele>
          <Tele k="Mode / Band">{S.mode} · {S.band}</Tele>
          <Tele k="Rotor">{S.rotor}°<span className="u">{S.rotorName}</span></Tele>
          <Tele k="Power / SWR">{S.power}<span className="u">W</span> · {S.swr}</Tele>
          <div className="a-tele">
            <div className="a-tele-k">Cluster</div>
            <div className="a-tele-v" style={{ fontSize: '13px', display: 'flex', alignItems: 'center', gap: '7px' }}>
              <span className="ars-dot on"></span><span className="ars-mono">{S.spots} spt/hr</span>
            </div>
          </div>
          <div className="a-strip-sp"></div>
          <Tele k="UTC">{S.utc}</Tele>
        </div>

        <div className="a-body">
          {/* main module grid */}
          <div className="a-main ars-scroll">
            <div className="a-sec-head">
              <div className="a-sec-title">Modules</div>
              <span className="a-pill"><span className="ars-dot on"></span>{runningCount} running</span>
              <span className="a-pill">{mods.length - runningCount} stopped</span>
              <div style={{ flex: 1 }}></div>
              <button className="ars-btn ghost" onClick={() => setMods(ms => ms.map(m => ({ ...m, running: true })))}>Launch all</button>
            </div>
            <div className="a-grid">
              {mods.map(m => (
                <div key={m.id} className={`a-card ${m.running ? 'run' : ''}`} style={{ '--h': `var(--${m.hue})` }}>
                  <div className="a-ctop">
                    <div className="a-icon"><ARSGlyph id={m.id} size={20} /></div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div className="a-name">{m.name}</div>
                      <div className="a-tag">{m.tag}</div>
                    </div>
                    <span className={`ars-dot ${m.running ? 'on' : 'off'}`}></span>
                  </div>
                  <div className="a-cstat">{m.running ? <span className="v ars-mono">{m.stat}</span> : <span style={{ color: 'var(--t3)' }}>{m.desc}</span>}</div>
                  <div className="a-cfoot">
                    <span className={`a-state ${m.running ? 'on' : ''}`}>{m.running ? 'Running' : 'Stopped'}</span>
                    <button className={`ars-btn ${m.running ? 'danger' : 'primary'}`} onClick={() => toggle(m.id)} style={{ padding: '6px 14px' }}>
                      {m.running ? 'Stop' : 'Launch'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* right rail */}
          <div className="a-rail ars-scroll">
            <div>
              <div className="a-rail-h"><div className="a-sec-title" style={{ fontSize: '13px' }}>Start your setup</div></div>
              <div className="a-prof">
                {ARS_PROFILES.map(p => (
                  <button key={p.id} className="a-profbtn" style={{ '--h': `var(--${p.hue})` }} onClick={() => launchProfile(p)}>
                    <div className="a-profmark"><ARSGlyph id={p.mods[0]} size={16} /></div>
                    <div><div className="a-profname">{p.name}</div><div className="a-profsub">{p.sub}</div></div>
                    <span className="a-prof-go">›</span>
                  </button>
                ))}
              </div>
            </div>

            <div>
              <div className="a-rail-h"><div className="a-sec-title" style={{ fontSize: '13px' }}>Recent activity</div></div>
              <div className="a-feed">
                {ARS_ACTIVITY.map((a, i) => (
                  <div key={i} className="a-feed-item" style={{ '--h': `var(--${a.hue})` }}>
                    <div className="a-feed-t ars-mono">{a.t}</div>
                    <div><div className="a-feed-txt">{a.txt}</div><div className="a-feed-tag">{a.tag}</div></div>
                  </div>
                ))}
              </div>
            </div>

            <div>
              <div className="a-rail-h"><div className="a-sec-title" style={{ fontSize: '13px' }}>Connection health</div></div>
              <div className="a-health">
                {ARS_HEALTH.map((h, i) => (
                  <div key={i} className="a-hrow">
                    <span className={`ars-dot ${h.ok ? 'on' : 'warn'}`}></span>
                    <div style={{ flex: 1 }}><div className="a-hname">{h.name}</div><div className="a-hvia ars-mono">{h.via}</div></div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

window.ARSDirectionA = ARSDirectionA;
