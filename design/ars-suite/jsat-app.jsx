/* J-Sat — satellite tracking. Polar sky plot (zenith center, horizon rim),
   satellite pass list, live az/el telemetry, and Doppler-corrected VFOs. */

function JSat({ theme = 'dark' }) {
  const [sel, setSel] = React.useState('AO-91');
  const [now, setNow] = React.useState(() => new Date());
  React.useEffect(() => { const i = setInterval(() => setNow(new Date()), 1000); return () => clearInterval(i); }, []);
  const utc = now.toISOString().slice(11, 19);
  const g = (id, size = 14) => <ARSGlyph id={id} size={size} stroke={1.9} />;
  const S = JSAT_SATS.find(s => s.name === sel) || JSAT_SATS[0];
  const active = JSAT_SATS.find(s => s.status === 'AOS');

  // polar sky plot: el 90 (zenith) at center, el 0 (horizon) at rim
  const VB = 460, cx = VB / 2, cy = VB / 2, R = 196;
  const proj = (az, el) => { const r = ((90 - el) / 90) * R, a = (az - 90) * Math.PI / 180; return { x: cx + Math.cos(a) * r, y: cy + Math.sin(a) * r }; };
  const elRings = [0, 30, 60];
  const cardinals = [['N', 0], ['E', 90], ['S', 180], ['W', 270]];
  const trackPts = JSAT_TRACK.map(p => proj(p.az, p.el));
  const trackStr = trackPts.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
  const nowPt = trackPts[JSAT_NOW_INDEX];

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label="J-Sat · Satellite tracking">
      <div className="sx-shell">
        <SuiteDock active="sat" />
        <div className="sx-modwin" style={{ '--h': 'var(--sat)' }}>
          <div className="sx-top">
            <div className="sx-brand" style={{ '--h': 'var(--sat)' }}><div className="ic">{g('sat', 18)}</div><div><div className="nm">J-Sat</div><div className="ct">Satellite tracking · {active ? active.name : 'idle'}</div></div></div>
            <div className="sx-stat"><div className="k">Tracking</div><div className="v" style={{ color: 'var(--sat)' }}>{S.name}</div></div>
            <div className="sx-stat"><div className="k">Az / El</div><div className="v">{String(S.az).padStart(3, '0')}° / {S.el}°</div></div>
            <div className="sx-stat"><div className="k">Next AOS</div><div className="v">{active ? 'NOW' : S.next}</div></div>
            <div className="sx-stat"><div className="k">Rotor</div><div className="v" style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 6 }}><span className="ars-dot on"></span>auto-track</div></div>
            <div className="sx-top-sp"></div>
            <div className="sx-clock"><div className="k">UTC</div><div className="v ars-mono">{utc}</div></div>
          </div>

          <div className="sx-body js-body">
            {/* LEFT — satellite list */}
            <div className="js-satcol">
              <div className="js-sat-h">{g('sat')} Satellites · {JSAT_SATS.length}</div>
              <div className="js-satlist ars-scroll">
                {JSAT_SATS.map(s => (
                  <div key={s.name} className={`js-sat ${s.name === sel ? 'sel' : ''}`} onClick={() => setSel(s.name)}>
                    <div className="js-sat-top">
                      <span className="js-sat-name">{s.name}</span><span className="js-sat-kind">{s.kind}</span>
                      <span className={`js-sat-badge ${s.status === 'AOS' ? 'aos' : s.status === 'LOS' ? 'los' : 'next'}`}>{s.status === 'AOS' ? '● AOS' : s.status === 'LOS' ? 'LOS' : s.next}</span>
                    </div>
                    <div className="js-sat-meta"><span>Max el<b>{s.maxEl}°</b></span><span>Dur<b>{s.dur}</b></span><span>Dn<b>{s.dn}</b></span></div>
                  </div>
                ))}
              </div>
            </div>

            {/* CENTER — sky plot + telemetry */}
            <div className="js-sky">
              <div className="js-sky-head">
                <div className="js-sky-title">{S.name}</div>
                <div className="js-sky-sub">{S.kind} · {S.dir} · max el {S.maxEl}°</div>
              </div>
              <div className="js-sky-wrap">
                <svg className="js-sky-svg" viewBox={`0 0 ${VB} ${VB}`} style={{ width: 'min(100%, calc(100cqh - 4px))', aspectRatio: 1 }}>
                  <circle cx={cx} cy={cy} r={R} fill="var(--surface-1)" stroke="var(--border-2)" strokeWidth="1.5" />
                  {elRings.map(el => { const rr = ((90 - el) / 90) * R; return <g key={el}><circle cx={cx} cy={cy} r={rr} fill="none" stroke="var(--border)" strokeWidth="1" /><text className="js-elr-lbl" x={cx + 3} y={cy - rr - 3}>{el}°</text></g>; })}
                  {[0, 45, 90, 135].map(a => { const p1 = proj(a, 0), p2 = proj(a + 180, 0); return <line key={a} x1={p1.x} y1={p1.y} x2={p2.x} y2={p2.y} stroke="var(--border)" strokeWidth="0.7" opacity="0.5" />; })}
                  {cardinals.map(([l, az]) => { const p = proj(az, -6); return <text key={l} className="js-card-lbl" x={p.x} y={p.y} textAnchor="middle" dominantBaseline="middle">{l}</text>; })}
                  {/* pass track */}
                  <polyline points={trackStr} fill="none" stroke="var(--sat)" strokeWidth="2" strokeDasharray="4 3" opacity="0.5" />
                  <circle cx={trackPts[0].x} cy={trackPts[0].y} r="3.5" fill="none" stroke="var(--sat)" strokeWidth="1.5" />
                  <circle cx={trackPts[trackPts.length - 1].x} cy={trackPts[trackPts.length - 1].y} r="3.5" fill="var(--surface-3)" stroke="var(--t3)" strokeWidth="1.5" />
                  {/* current position */}
                  <circle cx={nowPt.x} cy={nowPt.y} r="11" fill="none" stroke="var(--sat)" strokeWidth="1.5" opacity="0.5" />
                  <circle cx={nowPt.x} cy={nowPt.y} r="6" fill="var(--sat)" stroke="var(--surface-1)" strokeWidth="1.5" />
                  <text className="js-sat-dot-lbl" x={nowPt.x + 11} y={nowPt.y + 4}>{S.name}</text>
                  <text className="js-elr-lbl" x={cx} y={cy - 1} textAnchor="middle" style={{ fontSize: 8 }}>zenith</text>
                </svg>
              </div>
              <div className="js-tele">
                <div className="js-tele-c"><div className="js-tele-k">Azimuth</div><div className="js-tele-v">{String(S.az).padStart(3, '0')}<span className="u">°</span></div></div>
                <div className="js-tele-c"><div className="js-tele-k">Elevation</div><div className="js-tele-v">{S.el}<span className="u">°</span></div></div>
                <div className="js-tele-c"><div className="js-tele-k">Range</div><div className="js-tele-v">1,842<span className="u">km</span></div></div>
                <div className="js-tele-c"><div className="js-tele-k">Range rate</div><div className="js-tele-v" style={{ color: 'var(--sat)' }}>−5.2<span className="u">km/s</span></div></div>
                <div className="js-tele-c"><div className="js-tele-k">Phase</div><div className="js-tele-v">112</div></div>
              </div>
            </div>

            {/* RIGHT — VFO + instruments */}
            <div className="sx-rail ars-scroll">
              <SuiteDrawer title="Radio · Doppler" hue="sat" glyph={g('bridge')} defaultOpen summary={S.dn}>
                <div className="js-vfo">
                  <div className="js-vfo-row dn"><div><div className="lab">Downlink</div><div className="js-doppler">{S.dn} +1.2 kHz</div></div><div className="frq">{(parseFloat(S.dn) + 0.0012).toFixed(4)}</div></div>
                  <div className="js-vfo-row up"><div><div className="lab">Uplink</div><div className="js-doppler">{S.up} −0.4 kHz</div></div><div className="frq">{(parseFloat(S.up) - 0.0004).toFixed(4)}</div></div>
                </div>
                <div className="sx-kv" style={{ marginTop: 10 }}><span className="k">Mode</span><span className="v">{S.kind}</span></div>
                <div className="sx-kv"><span className="k">Doppler track</span><span className="v" style={{ color: 'var(--ok)' }}>full · auto</span></div>
                <div className="sx-kv"><span className="k">TX inhibit &lt; 5° el</span><span className="v" style={{ color: 'var(--ok)' }}>on</span></div>
              </SuiteDrawer>
              <SuiteDrawer title="Next passes" hue="map" glyph={g('map')} defaultOpen summary={`${JSAT_SATS.filter(s => s.status !== 'LOS').length} today`}>
                {JSAT_SATS.filter(s => s.status !== 'AOS').slice(0, 5).map(s => (
                  <div key={s.name} className="sx-kv"><span className="k">{s.name}<span style={{ color: 'var(--t4)', marginLeft: 6 }}>el {s.maxEl}°</span></span><span className="v">{s.next}</span></div>
                ))}
              </SuiteDrawer>
              <SuiteInstruments defaultRotor={S.az} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

window.JSat = JSat;
