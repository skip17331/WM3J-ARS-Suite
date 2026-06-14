/* J-Hub Workspace — the unified window.
   Left drawer is the spine: MODULES (operate: launch/stop/focus + gear→config)
   and J-HUB (configure: Dashboard + grouped settings). Main panel routes to the
   Dashboard or any config page. Drawer collapses to a mini icon-rail or hides;
   pin to lock it open, or push the pointer to the left edge to peek. */

function JHubWorkspace({ theme = 'dark' }) {
  const [mods, setMods] = React.useState(() => ARS_MODULES.map(m => ({ ...m })));
  const [route, setRoute] = React.useState('dashboard');
  const [pinned, setPinned] = React.useState(true);
  const [hovered, setHovered] = React.useState(false);
  const [mode] = React.useState('mini');
  const [openGroups, setOpenGroups] = React.useState({ hardware: true });
  const shellRef = React.useRef(null);

  const open = pinned || hovered;
  const collapsed = !open;
  const drawerCls = open ? 'is-open' : (mode === 'mini' ? 'is-mini' : '');
  const footprint = pinned ? 268 : (mode === 'mini' ? 66 : 0);

  React.useEffect(() => {
    const el = shellRef.current; if (!el) return;
    const onMove = (e) => {
      if (pinned) return;
      const x = e.clientX - el.getBoundingClientRect().left;
      if (x < 30) setHovered(true); else if (x > 270) setHovered(false);
    };
    const onLeave = () => setHovered(false);
    el.addEventListener('mousemove', onMove); el.addEventListener('mouseleave', onLeave);
    return () => { el.removeEventListener('mousemove', onMove); el.removeEventListener('mouseleave', onLeave); };
  }, [pinned]);

  const toggle = (id) => setMods(ms => ms.map(m => m.id === id ? { ...m, running: !m.running } : m));
  const launchProfile = (p) => setMods(ms => ms.map(m => p.mods.includes(m.id) ? { ...m, running: true } : m));
  const go = (r) => { setRoute(r); if (!pinned) setHovered(false); };

  // route → label/group/glyph
  const lookup = {};
  WS_NAV.forEach(g => { if (g.solo) lookup[g.id] = { label: g.label, group: null, glyph: g.glyph }; else g.items.forEach(it => lookup[it.id] = { label: it.label, group: g.label, glyph: g.glyph }); });
  mods.forEach(m => lookup['cfg-' + m.id] = { label: m.name, group: 'Module settings', glyph: m.id, hue: m.hue });

  const page = (() => {
    if (route === 'dashboard') return null;
    if (WS_PAGES[route]) return WS_PAGES[route];
    const l = lookup[route] || { label: route, group: '', glyph: 'hub' };
    return WS_STUB(l.label, l.group, l.glyph);
  })();
  const crumbGroup = page ? page.group : null;
  const crumbTitle = page ? page.title : 'Dashboard';

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label={`J-Hub Workspace · ${crumbTitle}`}>
      <div className="ars-win">
        <div className="ars-titlebar">
          <button className={`ws-pin ${pinned ? 'on' : ''}`} style={{ width: 28, height: 28 }} onClick={() => setPinned(p => !p)} title={pinned ? 'Unpin menu' : 'Pin menu open'}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><line x1="4" y1="7" x2="20" y2="7"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="17" x2="20" y2="17"/></svg>
          </button>
          <div className="ars-tb-mark"><span className="ars-tb-dot"></span>J-Hub <span className="ars-tb-sub">· {ARS_STATION.call}</span></div>
          <div className="ars-tb-spacer"></div>
          <div className="ars-tb-sub ars-mono">localhost:8081 · up 4h 12m</div>
          <div className="ars-tb-ctl"><span>—</span><span>▢</span><span>✕</span></div>
        </div>

        <div className="ws-body" ref={shellRef}>
          <div className="ws-edge"></div>
          {mode === 'hidden' && !open && (
            <div className="ws-handle" onClick={() => setPinned(true)} title="Show menu">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round"><polyline points="9 6 15 12 9 18"/></svg>
            </div>
          )}

          <aside className={`ws-drawer ${drawerCls} ${open && !pinned ? 'float' : ''} ${collapsed ? 'collapsed' : ''}`}>
            <div className="ws-dhead">
              <button className={`ws-pin ${pinned ? 'on' : ''}`} onClick={() => setPinned(p => !p)} title={pinned ? 'Unpin' : 'Pin open'}>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><line x1="4" y1="7" x2="20" y2="7"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="17" x2="20" y2="17"/></svg>
              </button>
              <span className="lbl">J-Hub</span>
            </div>

            <div className="ws-nav ars-scroll">
              {/* OPERATE */}
              <div className="ws-zone"><span>Operate</span><span className="ln"></span></div>
              {mods.map(m => (
                <div key={m.id} className={`ws-chic ${m.running ? 'run' : ''}`} style={{ '--h': `var(--${m.hue})` }} onClick={() => toggle(m.id)} title={`${m.name} — ${m.running ? 'running (click to stop)' : 'stopped (click to launch)'}`}>
                  <span className="ws-cpip" style={{ background: m.running ? 'var(--ok)' : 'var(--surface-4)' }}></span>
                  <div className="ws-ci"><ARSGlyph id={m.id} size={19} /></div>
                  <div className="ws-cbody">
                    <div className="ws-cname">{m.name}</div>
                    <div className="ws-ctag">{m.running ? <span className="ars-mono" style={{ color: 'var(--h)' }}>running</span> : m.tag}</div>
                  </div>
                  <button className="ws-cgear" title={`${m.name} settings`} onClick={(e) => { e.stopPropagation(); go('cfg-' + m.id); }}>
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="12" cy="12" r="3"/><path d="M19.4 13.5a1.6 1.6 0 00.3 1.8l.1.1a2 2 0 11-2.8 2.8l-.1-.1a1.6 1.6 0 00-2.7 1.1V21a2 2 0 11-4 0v-.1A1.6 1.6 0 005 19.4l-.1.1a2 2 0 11-2.8-2.8l.1-.1a1.6 1.6 0 00-1.1-2.7H1a2 2 0 110-4h.1A1.6 1.6 0 002.6 5l-.1-.1a2 2 0 112.8-2.8l.1.1a1.6 1.6 0 001.8.3H8a1.6 1.6 0 001-1.5V1a2 2 0 114 0v.1a1.6 1.6 0 001 1.5 1.6 1.6 0 001.8-.3l.1-.1a2 2 0 112.8 2.8l-.1.1a1.6 1.6 0 00-.3 1.8V8a1.6 1.6 0 001.5 1H23a2 2 0 110 4h-.1a1.6 1.6 0 00-1.5 1z"/></svg>
                  </button>
                </div>
              ))}

              {/* CONFIGURE */}
              <div className="ws-zone" style={{ marginTop: 16 }}><span>J-Hub</span><span className="ln"></span></div>
              {WS_NAV.map(g => g.solo ? (
                <div key={g.id} className={`ws-item ${route === g.id ? 'active' : ''}`} onClick={() => go(g.id)} title={g.label}>
                  <span className="ws-iglyph"><ARSGlyph id={g.glyph} size={18} stroke={1.8} /></span>
                  <span className="ws-ilabel">{g.label}</span>
                </div>
              ) : (
                <React.Fragment key={g.id}>
                  <div className="ws-item" onClick={() => collapsed ? setPinned(true) : setOpenGroups(o => ({ ...o, [g.id]: !o[g.id] }))} title={g.label}>
                    <span className="ws-iglyph"><ARSGlyph id={g.glyph} size={18} stroke={1.8} /></span>
                    <span className="ws-ilabel">{g.label}</span>
                    <span className={`ws-icaret ${openGroups[g.id] ? 'open' : ''}`}>
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"><polyline points="9 6 15 12 9 18"/></svg>
                    </span>
                  </div>
                  {openGroups[g.id] && !collapsed && (
                    <div className="ws-sub">
                      {g.items.map(it => (
                        <div key={it.id} className={`ws-subitem ${route === it.id ? 'active' : ''}`} onClick={() => go(it.id)}>{it.label}</div>
                      ))}
                    </div>
                  )}
                </React.Fragment>
              ))}
            </div>
          </aside>

          {/* MAIN PANEL */}
          <div className="ws-main ars-scroll" style={{ paddingLeft: footprint }}>
            <div className="ws-topbar">
              <div className="ws-crumb">
                J-Hub <span className="sep">›</span>
                {crumbGroup && <React.Fragment>{crumbGroup} <span className="sep">›</span> </React.Fragment>}
                <b>{crumbTitle}</b>
              </div>
              <div style={{ flex: 1 }}></div>
              {route === 'dashboard'
                ? <span className="ars-mono" style={{ fontSize: 11.5, color: 'var(--t3)' }}>{mods.filter(m => m.running).length}/{mods.length} modules running</span>
                : <button className="ars-btn ghost" onClick={() => go('dashboard')} style={{ fontSize: 12 }}>← Dashboard</button>}
            </div>
            {route === 'dashboard'
              ? <WSDashboard mods={mods} onLaunchProfile={launchProfile} />
              : <WSConfigPage page={page} />}
          </div>
        </div>
      </div>
    </div>
  );
}

window.JHubWorkspace = JHubWorkspace;
