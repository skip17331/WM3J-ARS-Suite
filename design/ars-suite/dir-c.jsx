/* Direction C — "Flight Deck"
   The software-forward direction, modelled on a modern IDE/terminal. A
   command bar is the primary verb ("launch a module, jump to a setting, run a
   macro"), a persistent left module rail stays open all session, the center
   is organized around saved "workspaces" (named layouts) plus a session
   timeline, and an IDE-style status bar pins live telemetry to the bottom.
   Mostly monochrome; module hues appear only as identifying marks. */

(function () {
  if (document.getElementById('dir-c-styles')) return;
  const s = document.createElement('style');
  s.id = 'dir-c-styles';
  s.textContent = `
  .c-cmd { flex:0 0 auto; display:flex; align-items:center; gap:11px; padding:11px 16px; background:var(--surface-1); border-bottom:1px solid var(--border); }
  .c-cmdbox { flex:1; max-width:620px; margin:0 auto; display:flex; align-items:center; gap:10px; height:38px; padding:0 14px; border-radius:8px; background:var(--bg); border:1px solid var(--border-2); color:var(--t3); font-size:13px; cursor:text; }
  .c-cmdbox:hover { border-color:var(--border-glow); }
  .c-kbd { margin-left:auto; font-size:10.5px; padding:2px 7px; border-radius:4px; background:var(--surface-3); border:1px solid var(--border-2); color:var(--t2); }

  .c-body { flex:1; display:grid; grid-template-columns:218px 1fr 256px; min-height:0; }
  .c-rail { border-right:1px solid var(--border); background:var(--surface-1); padding:13px 9px; overflow:auto; }
  .c-rail-h { font-size:9.5px; letter-spacing:1.4px; text-transform:uppercase; color:var(--t3); font-weight:600; padding:0 7px; margin:4px 0 9px; display:flex; justify-content:space-between; }
  .c-mrow { display:flex; align-items:center; gap:10px; padding:8px 8px; border-radius:6px; cursor:pointer; transition:background .12s; position:relative; }
  .c-mrow:hover { background:var(--surface-3); }
  .c-mrow.run { background:var(--surface-2); }
  .c-mmark { width:9px; height:9px; border-radius:3px; flex:0 0 auto; background:var(--h); }
  .c-mrow:not(.run) .c-mmark { background:var(--surface-4); }
  .c-micon { color:var(--t2); display:flex; }
  .c-mrow.run .c-micon { color:var(--t1); }
  .c-mname { font-size:13px; font-weight:500; color:var(--t2); flex:1; }
  .c-mrow.run .c-mname { color:var(--t1); }
  .c-mdot { width:6px; height:6px; border-radius:50%; flex:0 0 auto; }
  .c-mdot.on { background:var(--ok); box-shadow:0 0 6px -1px var(--ok); }
  .c-mdot.off { background:transparent; border:1px solid var(--border-2); }

  .c-main { padding:20px 22px; overflow:auto; }
  .c-h { font-size:13px; font-weight:600; margin-bottom:13px; display:flex; align-items:center; gap:10px; }
  .c-h .ars-eyebrow { font-weight:600; }
  .c-ws { display:grid; grid-template-columns:repeat(2,1fr); gap:12px; margin-bottom:26px; }
  .c-wscard { background:var(--surface-1); border:1px solid var(--border); border-radius:var(--radius); padding:15px; cursor:pointer; transition:border-color .14s, background .14s; }
  .c-wscard:hover { border-color:var(--border-glow); background:var(--surface-2); }
  .c-wscard.active { border-color:var(--accent-line); background:color-mix(in oklch, var(--accent) 6%, var(--surface-1)); }
  .c-ws-top { display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:11px; }
  .c-ws-name { font-size:15px; font-weight:600; white-space:nowrap; }
  .c-ws-badge { font-size:9.5px; font-weight:700; letter-spacing:.6px; text-transform:uppercase; color:var(--accent); border:1px solid var(--accent-line); padding:2px 7px; border-radius:99px; white-space:nowrap; flex:0 0 auto; }
  .c-ws-mods { display:flex; flex-wrap:wrap; gap:6px; margin-bottom:13px; }
  .c-ws-chip { display:flex; align-items:center; gap:5px; font-size:11px; color:var(--t2); padding:3px 8px 3px 6px; border-radius:6px; background:var(--surface-3); border:1px solid var(--border); }
  .c-ws-chip i { width:7px; height:7px; border-radius:2px; display:block; }
  .c-ws-foot { display:flex; align-items:center; justify-content:space-between; }
  .c-ws-meta { font-size:11px; color:var(--t3); }

  .c-tl { position:relative; padding-left:18px; }
  .c-tl::before { content:''; position:absolute; left:4px; top:4px; bottom:4px; width:1px; background:var(--border); }
  .c-tlrow { position:relative; padding:7px 0; display:flex; gap:11px; }
  .c-tldot { position:absolute; left:-17px; top:11px; width:8px; height:8px; border-radius:50%; background:var(--h); border:2px solid var(--bg); }
  .c-tl-t { font-size:11px; color:var(--t3); flex:0 0 40px; padding-top:1px; }
  .c-tl-txt { font-size:12.5px; color:var(--t2); }
  .c-tl-tag { font-size:10px; color:var(--h); font-weight:600; }

  .c-side { border-left:1px solid var(--border); background:var(--surface-1); padding:15px 15px; overflow:auto; }
  .c-srow { padding:10px 0; border-bottom:1px solid var(--border); }
  .c-srow:last-child { border-bottom:none; }
  .c-sk { font-size:9.5px; letter-spacing:1.2px; text-transform:uppercase; color:var(--t3); font-weight:600; margin-bottom:4px; }
  .c-sv { font-size:18px; font-weight:600; }
  .c-sv .u { font-size:11px; color:var(--t3); margin-left:3px; }
  .c-sv2 { font-size:13px; color:var(--t2); }

  /* IDE-style status bar */
  .c-status { flex:0 0 auto; height:30px; display:flex; align-items:center; gap:0; background:var(--accent); color:#08161d; font-size:11.5px; font-weight:500; padding:0 4px; }
  .ars-light .c-status { color:#06222e; }
  .c-stat { padding:0 13px; display:flex; align-items:center; gap:7px; height:100%; }
  .c-stat + .c-stat { box-shadow:inset 1px 0 0 rgba(0,0,0,0.12); }
  .c-stat .d { width:7px; height:7px; border-radius:50%; background:#08161d; }
  .c-status-sp { flex:1; }
  `;
  document.head.appendChild(s);
})();

function ARSDirectionC({ theme = 'dark' }) {
  const [mods, setMods] = React.useState(() => ARS_MODULES.map(m => ({ ...m })));
  const [active, setActive] = React.useState('contest');
  const toggle = (id) => setMods(ms => ms.map(m => m.id === id ? { ...m, running: !m.running } : m));
  const openWs = (p) => { setActive(p.id); setMods(ms => ms.map(m => ({ ...m, running: p.mods.includes(m.id) }))); };
  const S = ARS_STATION;
  const byId = Object.fromEntries(mods.map(m => [m.id, m]));

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label="Launcher · C Flight Deck">
      <div className="ars-win">
        <div className="ars-titlebar">
          <div className="ars-tb-mark"><span className="ars-tb-dot"></span>J-Hub</div>
          <div className="ars-tb-spacer"></div>
          <div className="ars-tb-sub ars-mono">{S.call} · {S.grid}</div>
          <div className="ars-tb-ctl"><span>—</span><span>▢</span><span>✕</span></div>
        </div>

        {/* command bar */}
        <div className="c-cmd">
          <div className="c-cmdbox ars-mono">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.5" y2="16.5"/></svg>
            <span>Launch a module, jump to a setting, run a macro…</span>
            <span className="c-kbd">⌘K</span>
          </div>
        </div>

        <div className="c-body">
          {/* persistent module rail */}
          <div className="c-rail ars-scroll">
            <div className="c-rail-h"><span>Modules</span><span>{mods.filter(m => m.running).length}/{mods.length}</span></div>
            {mods.map(m => (
              <div key={m.id} className={`c-mrow ${m.running ? 'run' : ''}`} style={{ '--h': `var(--${m.hue})` }} onClick={() => toggle(m.id)}>
                <span className="c-mmark"></span>
                <span className="c-micon"><ARSGlyph id={m.id} size={17} stroke={1.8} /></span>
                <span className="c-mname">{m.name}</span>
                <span className={`c-mdot ${m.running ? 'on' : 'off'}`}></span>
              </div>
            ))}
            <div className="c-rail-h" style={{ marginTop: '18px' }}><span>J-Hub config</span></div>
            {['Station', 'Hardware', 'Data & logging', 'Macros'].map(x => (
              <div key={x} className="c-mrow"><span className="c-mmark" style={{ background: 'var(--surface-4)' }}></span><span className="c-mname">{x}</span></div>
            ))}
          </div>

          {/* center — workspaces + timeline */}
          <div className="c-main ars-scroll">
            <div className="c-h"><span className="ars-eyebrow">Workspaces</span><span style={{ color: 'var(--t4)', fontWeight: 400, fontSize: '12px' }}>saved layouts · one click launches the set</span></div>
            <div className="c-ws">
              {ARS_PROFILES.map(p => (
                <div key={p.id} className={`c-wscard ${active === p.id ? 'active' : ''}`} onClick={() => openWs(p)}>
                  <div className="c-ws-top">
                    <div className="c-ws-name">{p.name}</div>
                    {active === p.id && <span className="c-ws-badge">● active</span>}
                  </div>
                  <div className="c-ws-mods">
                    {p.mods.map(id => (
                      <span key={id} className="c-ws-chip"><i style={{ background: `var(--${byId[id].hue})` }}></i>{byId[id].name}</span>
                    ))}
                  </div>
                  <div className="c-ws-foot">
                    <span className="c-ws-meta ars-mono">{p.mods.length} modules</span>
                    <span className="ars-btn ghost" style={{ padding: '4px 10px', fontSize: '12px' }}>{active === p.id ? 'Resume ›' : 'Launch ›'}</span>
                  </div>
                </div>
              ))}
            </div>

            <div className="c-h"><span className="ars-eyebrow">Session timeline</span></div>
            <div className="c-tl">
              {ARS_ACTIVITY.map((a, i) => (
                <div key={i} className="c-tlrow" style={{ '--h': `var(--${a.hue})` }}>
                  <span className="c-tldot"></span>
                  <span className="c-tl-t ars-mono">{a.t}</span>
                  <span><span className="c-tl-txt">{a.txt}</span> <span className="c-tl-tag">{a.tag}</span></span>
                </div>
              ))}
            </div>
          </div>

          {/* right — compact station */}
          <div className="c-side ars-scroll">
            <div className="c-rail-h" style={{ padding: 0, marginBottom: '6px' }}>Station</div>
            <div className="c-srow"><div className="c-sk">Rig · IC-7610</div><div className="c-sv ars-mono">{S.freq}<span className="u">MHz</span></div><div className="c-sv2 ars-mono">{S.mode} · {S.band}</div></div>
            <div className="c-srow"><div className="c-sk">Rotor</div><div className="c-sv ars-mono">{S.rotor}° <span className="u">{S.rotorName}</span></div></div>
            <div className="c-srow"><div className="c-sk">Power / SWR</div><div className="c-sv ars-mono">{S.power}<span className="u">W</span> · {S.swr}:1</div></div>
            <div className="c-srow"><div className="c-sk">DX Cluster</div><div className="c-sv2" style={{ display: 'flex', alignItems: 'center', gap: '7px', fontWeight: 600, color: 'var(--ok)' }}><span className="ars-dot on"></span><span className="ars-mono">{S.spots} spots/hr</span></div></div>
          </div>
        </div>

        {/* status bar */}
        <div className="c-status ars-mono">
          <span className="c-stat"><span className="d"></span>{S.call}</span>
          <span className="c-stat">{S.freq} {S.mode}</span>
          <span className="c-stat">ROT {S.rotor}°</span>
          <span className="c-stat">CLUSTER ●</span>
          <span className="c-stat">{S.spots} spt/hr</span>
          <span className="c-status-sp"></span>
          <span className="c-stat">8081</span>
          <span className="c-stat">{S.utc}Z</span>
        </div>
      </div>
    </div>
  );
}

window.ARSDirectionC = ARSDirectionC;
