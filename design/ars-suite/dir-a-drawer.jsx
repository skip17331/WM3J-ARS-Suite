/* Direction A · Drawer — the chiclet module menu as a COLLAPSIBLE LEFT RAIL.
   Two ways to reveal it (both live):
     • Pin button (☰) in the title bar — locks it open and pushes content.
     • Edge-hover — push the pointer to the left edge and it flies out as an
       overlay; move away and it collapses.
   Collapse mode switch: "Mini rail" (icon chiclets stay visible) vs "Hidden"
   (drawer vanishes behind a slim handle). Main area becomes the dashboard the
   modules used to crowd. */

(function () {
  if (document.getElementById('dir-ad-styles')) return;
  const s = document.createElement('style');
  s.id = 'dir-ad-styles';
  s.textContent = `
  .ad-shell { flex:1; position:relative; min-height:0; overflow:hidden; }

  /* edge hot-zone for hover-reveal */
  .ad-edge { position:absolute; left:0; top:0; bottom:0; width:14px; z-index:6; }
  .ad-handle { position:absolute; left:0; top:50%; transform:translateY(-50%); z-index:5;
    width:18px; height:64px; border-radius:0 8px 8px 0; background:var(--surface-3);
    border:1px solid var(--border-2); border-left:none; display:flex; align-items:center; justify-content:center;
    color:var(--t3); cursor:pointer; transition:background .14s, color .14s, opacity .2s; }
  .ad-handle:hover { background:var(--surface-4); color:var(--t1); }

  /* drawer */
  .ad-drawer { position:absolute; left:0; top:0; bottom:0; z-index:7; background:var(--surface-1); width:0;
    border-right:1px solid var(--border); display:flex; flex-direction:column; overflow:hidden;
    transition:width .22s cubic-bezier(.3,.7,.3,1), box-shadow .22s; }
  .ad-drawer.is-mini { width:64px; }
  .ad-drawer.is-open { width:296px; }
  .ad-drawer.float { box-shadow:14px 0 40px -12px rgba(0,0,0,.55); }
  .ad-dhead { flex:0 0 auto; height:46px; display:flex; align-items:center; gap:10px; padding:0 12px;
    border-bottom:1px solid var(--border); }
  .ad-dhead .lbl { font-size:11px; letter-spacing:1.3px; text-transform:uppercase; color:var(--t3); font-weight:600; white-space:nowrap; flex:1; }
  .ad-pin { width:30px; height:30px; flex:0 0 auto; border-radius:7px; border:1px solid var(--border-2);
    background:var(--surface-3); color:var(--t2); display:flex; align-items:center; justify-content:center; cursor:pointer; transition:all .13s; }
  .ad-pin:hover { color:var(--t1); border-color:var(--border-glow); }
  .ad-pin.on { background:var(--accent-dim); border-color:var(--accent-line); color:var(--accent); }

  .ad-list { flex:1; overflow:hidden auto; padding:9px; display:flex; flex-direction:column; gap:7px; }

  /* chiclet — collapses to icon-only */
  .ad-chic { display:flex; align-items:center; gap:11px; padding:9px; border-radius:9px;
    background:var(--surface-2); border:1px solid var(--border); position:relative; overflow:hidden;
    cursor:pointer; transition:border-color .13s, background .13s; }
  .ad-chic:hover { border-color:var(--border-glow); }
  .ad-chic.run { background:color-mix(in oklch, var(--h) 8%, var(--surface-2)); border-color:color-mix(in oklch, var(--h) 30%, var(--border)); }
  .ad-chic.run::before { content:''; position:absolute; left:0; top:0; bottom:0; width:3px; background:var(--h); }
  .ad-ci { width:38px; height:38px; flex:0 0 auto; border-radius:9px; display:flex; align-items:center; justify-content:center;
    background:color-mix(in oklch, var(--h) 18%, var(--surface-3)); color:var(--h); border:1px solid color-mix(in oklch, var(--h) 28%, transparent); }
  .ad-cbody { flex:1; min-width:0; opacity:1; transition:opacity .16s; }
  .ad-drawer.mini-collapsed .ad-cbody { opacity:0; }
  .ad-cname { font-size:13.5px; font-weight:600; line-height:1.1; white-space:nowrap; }
  .ad-ctag { font-size:10.5px; color:var(--t3); margin-top:2px; white-space:nowrap; }
  .ad-cact { flex:0 0 auto; display:flex; align-items:center; gap:8px; }
  .ad-cdot { width:7px; height:7px; border-radius:50%; flex:0 0 auto; }
  .ad-cdot.on { background:var(--ok); box-shadow:0 0 6px -1px var(--ok); }
  .ad-cdot.off{ background:var(--surface-4); }
  /* mini-state status pip in the corner */
  .ad-cpip { position:absolute; top:6px; right:6px; width:7px; height:7px; border-radius:50%; opacity:0; transition:opacity .16s; }
  .ad-drawer.mini-collapsed .ad-cpip { opacity:1; }
  .ad-dfoot { flex:0 0 auto; padding:10px; border-top:1px solid var(--border); }

  /* main dashboard */
  .ad-main { height:100%; overflow:auto; transition:padding-left .22s cubic-bezier(.3,.7,.3,1); }
  .ad-inner { padding:18px 20px; }
  .ad-strip { display:grid; grid-template-columns:repeat(6,1fr); gap:10px; margin-bottom:16px; }
  .ad-stat { background:var(--surface-1); border:1px solid var(--border); border-radius:var(--radius); padding:11px 13px; }
  .ad-stat .k { font-size:9px; letter-spacing:1.2px; text-transform:uppercase; color:var(--t3); font-weight:600; }
  .ad-stat .v { font-size:16px; font-weight:600; margin-top:5px; }
  .ad-stat .v .u { font-size:10px; color:var(--t3); margin-left:2px; }
  .ad-sec { font-size:13px; font-weight:600; margin:6px 0 11px; display:flex; align-items:center; gap:9px; }
  .ad-profrow { display:grid; grid-template-columns:repeat(4,1fr); gap:11px; margin-bottom:22px; }
  .ad-prof { display:flex; align-items:center; gap:11px; padding:12px; border-radius:var(--radius); border:1px solid var(--border); background:var(--surface-1); cursor:pointer; transition:border-color .13s, background .13s; }
  .ad-prof:hover { border-color:var(--border-glow); background:var(--surface-2); }
  .ad-pmark { width:34px; height:34px; flex:0 0 auto; border-radius:8px; display:flex; align-items:center; justify-content:center; background:color-mix(in oklch, var(--h) 16%, var(--surface-3)); color:var(--h); }
  .ad-pname { font-size:13.5px; font-weight:600; }
  .ad-psub { font-size:10.5px; color:var(--t3); margin-top:1px; }
  .ad-cols { display:grid; grid-template-columns:1fr 1fr; gap:22px; }
  .ad-card { background:var(--surface-1); border:1px solid var(--border); border-radius:var(--radius); padding:14px 16px; }
  .ad-feed-item { display:flex; gap:10px; padding:8px 0; border-bottom:1px solid var(--border); }
  .ad-feed-item:last-child { border-bottom:none; }
  .ad-feed-t { font-size:11px; color:var(--t3); flex:0 0 38px; }
  .ad-feed-txt { font-size:12px; color:var(--t2); line-height:1.35; }
  .ad-feed-tag { font-size:10px; color:var(--h); font-weight:600; }
  .ad-hrow { display:flex; align-items:center; gap:9px; padding:7px 0; border-bottom:1px solid var(--border); }
  .ad-hrow:last-child { border-bottom:none; }
  .ad-hint { position:absolute; right:16px; bottom:14px; z-index:8; font-size:11px; color:var(--t3);
    background:var(--surface-1); border:1px solid var(--border); border-radius:99px; padding:6px 13px; display:flex; gap:8px; align-items:center; }
  .ad-hint b { color:var(--t1); font-weight:600; }
  `;
  document.head.appendChild(s);
})();

function ARSDirectionADrawer({ theme = 'dark' }) {
  const [mods, setMods] = React.useState(() => ARS_MODULES.map(m => ({ ...m })));
  const [pinned, setPinned] = React.useState(true);
  const [hovered, setHovered] = React.useState(false);
  const [mode, setMode] = React.useState('mini'); // 'mini' | 'hidden'
  const shellRef = React.useRef(null);
  const toggle = (id) => setMods(ms => ms.map(m => m.id === id ? { ...m, running: !m.running } : m));
  const launchProfile = (p) => setMods(ms => ms.map(m => p.mods.includes(m.id) ? { ...m, running: true } : m));
  const S = ARS_STATION;
  const runningCount = mods.filter(m => m.running).length;

  const open = pinned || hovered;
  const collapsedW = mode === 'mini' ? 64 : 0;
  const drawerW = open ? 296 : collapsedW;
  const footprint = pinned ? 296 : collapsedW;
  const floating = open && !pinned;
  const miniCollapsed = !open && mode === 'mini';

  // "push the pointer to the left edge" → reveal; move back past the drawer → collapse.
  React.useEffect(() => {
    const el = shellRef.current;
    if (!el) return;
    const onMove = (e) => {
      if (pinned) return;
      const x = e.clientX - el.getBoundingClientRect().left;
      if (x < 30) setHovered(true);
      else if (x > 300) setHovered(false);
    };
    const onLeave = () => setHovered(false);
    el.addEventListener('mousemove', onMove);
    el.addEventListener('mouseleave', onLeave);
    return () => { el.removeEventListener('mousemove', onMove); el.removeEventListener('mouseleave', onLeave); };
  }, [pinned]);

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label="Launcher · A Drawer">
      <div className="ars-win">
        <div className="ars-titlebar">
          <button className={`ad-pin ${pinned ? 'on' : ''}`} style={{ width: 28, height: 28 }} onClick={() => setPinned(p => !p)} title={pinned ? 'Unpin menu' : 'Pin menu open'}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><line x1="4" y1="7" x2="20" y2="7"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="17" x2="20" y2="17"/></svg>
          </button>
          <div className="ars-tb-mark"><span className="ars-tb-dot"></span>J-Hub <span className="ars-tb-sub">· {S.call}</span></div>
          <div className="ars-tb-spacer"></div>
          {/* collapse-mode segmented control */}
          <div style={{ display: 'flex', gap: 2, padding: 2, background: 'var(--surface-3)', borderRadius: 7, border: '1px solid var(--border)' }}>
            {[['mini', 'Mini rail'], ['hidden', 'Hidden']].map(([v, lbl]) => (
              <button key={v} onClick={() => setMode(v)} style={{
                fontFamily: 'inherit', fontSize: 11.5, fontWeight: 600, padding: '4px 10px', borderRadius: 5, cursor: 'pointer', border: 'none',
                background: mode === v ? 'var(--surface-1)' : 'transparent', color: mode === v ? 'var(--t1)' : 'var(--t3)',
              }}>{lbl}</button>
            ))}
          </div>
          <div className="ars-tb-ctl" style={{ marginLeft: 8 }}><span>—</span><span>▢</span><span>✕</span></div>
        </div>

        <div className="ad-shell" ref={shellRef}>
          {/* edge hover-reveal hot-zone (visual cue; reveal is driven by pointer-x) */}
          <div className="ad-edge"></div>
          {/* handle shown when fully hidden */}
          {mode === 'hidden' && !open && (
            <div className="ad-handle" onClick={() => setPinned(true)} title="Show modules">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round"><polyline points="9 6 15 12 9 18"/></svg>
            </div>
          )}

          {/* drawer */}
          <aside className={`ad-drawer ${open ? 'is-open' : (mode === 'mini' ? 'is-mini' : '')} ${floating ? 'float' : ''} ${miniCollapsed ? 'mini-collapsed' : ''}`}>
            <div className="ad-dhead">
              <button className={`ad-pin ${pinned ? 'on' : ''}`} onClick={() => setPinned(p => !p)} title={pinned ? 'Unpin' : 'Pin open'}>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><line x1="4" y1="7" x2="20" y2="7"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="17" x2="20" y2="17"/></svg>
              </button>
              <span className="lbl">Modules · {runningCount}/{mods.length}</span>
            </div>
            <div className="ad-list ars-scroll">
              {mods.map(m => (
                <div key={m.id} className={`ad-chic ${m.running ? 'run' : ''}`} style={{ '--h': `var(--${m.hue})` }} onClick={() => toggle(m.id)} title={`${m.name} — ${m.running ? 'running' : 'stopped'}`}>
                  <span className="ad-cpip" style={{ background: m.running ? 'var(--ok)' : 'var(--surface-4)' }}></span>
                  <div className="ad-ci"><ARSGlyph id={m.id} size={20} /></div>
                  <div className="ad-cbody">
                    <div className="ad-cname">{m.name}</div>
                    <div className="ad-ctag">{m.running ? <span className="ars-mono" style={{ color: 'var(--h)' }}>{m.stat}</span> : m.tag}</div>
                  </div>
                  <div className="ad-cact">
                    <span className={`ad-cdot ${m.running ? 'on' : 'off'}`}></span>
                  </div>
                </div>
              ))}
            </div>
            <div className="ad-dfoot">
              <button className="ars-btn" style={{ width: '100%', justifyContent: 'center', whiteSpace: 'nowrap' }} onClick={() => setMods(ms => ms.map(m => ({ ...m, running: true })))}>Launch all</button>
            </div>
          </aside>

          {/* main dashboard */}
          <div className="ad-main ars-scroll" style={{ paddingLeft: footprint }}>
            <div className="ad-inner">
              <div className="ad-strip">
                <div className="ad-stat"><div className="k">Rig</div><div className="v ars-mono">{S.freq}<span className="u">MHz</span></div></div>
                <div className="ad-stat"><div className="k">Mode / Band</div><div className="v ars-mono">{S.mode} {S.band}</div></div>
                <div className="ad-stat"><div className="k">Rotor</div><div className="v ars-mono">{S.rotor}°<span className="u">{S.rotorName}</span></div></div>
                <div className="ad-stat"><div className="k">Power / SWR</div><div className="v ars-mono">{S.power}<span className="u">W</span> {S.swr}</div></div>
                <div className="ad-stat"><div className="k">Cluster</div><div className="v ars-mono" style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 6 }}><span className="ars-dot on"></span>{S.spots}/hr</div></div>
                <div className="ad-stat"><div className="k">UTC</div><div className="v ars-mono">{S.utc}</div></div>
              </div>

              <div className="ad-sec">Start your setup</div>
              <div className="ad-profrow">
                {ARS_PROFILES.map(p => (
                  <button key={p.id} className="ad-prof" style={{ '--h': `var(--${p.hue})` }} onClick={() => launchProfile(p)}>
                    <div className="ad-pmark"><ARSGlyph id={p.mods[0]} size={17} /></div>
                    <div style={{ textAlign: 'left' }}><div className="ad-pname">{p.name}</div><div className="ad-psub">{p.sub}</div></div>
                  </button>
                ))}
              </div>

              <div className="ad-cols">
                <div>
                  <div className="ad-sec">Recent activity</div>
                  <div className="ad-card">
                    {ARS_ACTIVITY.map((a, i) => (
                      <div key={i} className="ad-feed-item" style={{ '--h': `var(--${a.hue})` }}>
                        <div className="ad-feed-t ars-mono">{a.t}</div>
                        <div><div className="ad-feed-txt">{a.txt}</div><div className="ad-feed-tag">{a.tag}</div></div>
                      </div>
                    ))}
                  </div>
                </div>
                <div>
                  <div className="ad-sec">Connection health</div>
                  <div className="ad-card">
                    {ARS_HEALTH.map((h, i) => (
                      <div key={i} className="ad-hrow">
                        <span className={`ars-dot ${h.ok ? 'on' : 'warn'}`}></span>
                        <div style={{ flex: 1 }}><div style={{ fontSize: 12 }}>{h.name}</div><div className="ars-mono" style={{ fontSize: 10.5, color: 'var(--t3)', marginTop: 1 }}>{h.via}</div></div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="ad-hint"><b>{pinned ? 'Pinned' : 'Auto'}</b> · ☰ pins · push the left edge to peek</div>
        </div>
      </div>
    </div>
  );
}

window.ARSDirectionADrawer = ARSDirectionADrawer;
