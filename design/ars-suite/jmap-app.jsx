/* J-Map — propagation / spot map. Azimuthal-equidistant map centered on QTH:
   range rings (distance), bearing spokes (beam heading), live gray-line
   sunlit overlay, rotor beam wedge, and DX spots at true bearing/distance,
   colored by band. Filters + a synced spot list. */

function JMap({ theme = 'dark' }) {
  const Q = JMAP_QTH;
  const [bands, setBands] = React.useState(() => new Set(JMAP_BANDS));
  const [modeF, setModeF] = React.useState('All');
  const [maxAge, setMaxAge] = React.useState(20);
  const [showGray, setShowGray] = React.useState(true);
  const [showBeam, setShowBeam] = React.useState(true);
  const [showRings, setShowRings] = React.useState(true);
  const [showCalls, setShowCalls] = React.useState(true);
  const [showBase, setShowBase] = React.useState(true);
  const [needOnly, setNeedOnly] = React.useState(false);
  const [beamAz, setBeamAz] = React.useState(50);
  const [sel, setSel] = React.useState(null);
  const [proj, setProj] = React.useState('azimuthal');
  const [freq, setFreq] = React.useState('14.250');
  const [now, setNow] = React.useState(() => new Date());

  React.useEffect(() => { const i = setInterval(() => setNow(new Date()), 1000); return () => clearInterval(i); }, []);
  const utc = now.toISOString().slice(11, 19);

  // geometry
  const VB = 620, cx = VB / 2, cy = VB / 2, R = 280;
  const spots = React.useMemo(() => JMAP_SPOTS.map(s => {
    const { az, dist } = greatCircle(Q.lat, Q.lon, s.lat, s.lon);
    return { ...s, az, dist };
  }), []);
  const shown = spots.filter(s => bands.has(s.band) && (modeF === 'All' || modeGroup(s.mode) === modeF) && s.age <= maxAge && (!needOnly || s.need));

  const toggleBand = (b) => setBands(s => { const n = new Set(s); n.has(b) ? n.delete(b) : n.add(b); return n; });
  const allBands = () => setBands(new Set(JMAP_BANDS));
  const pick = (s) => { setSel(s.call); setFreq(s.f); };
  const beamTo = (s) => setBeamAz(Math.round(s.az));
  const g = (id, size = 15) => <ARSGlyph id={id} size={size} stroke={1.9} />;

  // gray-line sunlit disc
  const ss = subSolar(now);
  const sgc = greatCircle(Q.lat, Q.lon, ss.lat, ss.lon);
  const sunPt = project(sgc.az, sgc.dist, cx, cy, R);
  const rDay = (10007 / MAXKM) * R;

  // rings & spokes
  const rings = [5000, 10000, 15000];
  const spokes = []; for (let a = 0; a < 360; a += 30) spokes.push(a);
  const beamEnd = project(beamAz, MAXKM, cx, cy, R);
  const beamL = project(beamAz - 4, MAXKM, cx, cy, R);
  const beamR = project(beamAz + 4, MAXKM, cx, cy, R);
  const dirLabel = (a) => ['N', '30', '60', 'E', '120', '150', 'S', '210', '240', 'W', '300', '330'][a / 30];

  const selSpot = shown.find(s => s.call === sel) || spots.find(s => s.call === sel);

  // ---- rectangular (equirectangular) projection ----
  const RW = 600, RH = 300, RMX = 14, RMY = 38;
  const lon2x = (lon) => RMX + ((lon + 180) / 360) * RW;
  const lat2y = (lat) => RMY + ((90 - lat) / 180) * RH;
  const decl = ss.lat, ssLon = ss.lon;
  const northLit = decl >= 0;
  // terminator latitude for a given longitude
  const termLat = (lon) => Math.atan(-Math.cos((lon - ssLon) * Math.PI / 180) / Math.tan((decl || 0.001) * Math.PI / 180)) * 180 / Math.PI;
  let termPts = [];
  for (let lon = -180; lon <= 180; lon += 5) termPts.push(`${lon2x(lon).toFixed(1)},${lat2y(termLat(lon)).toFixed(1)}`);
  const dayEdgeY = northLit ? RMY : RMY + RH;
  const dayPath = `M ${RMX},${dayEdgeY} L ${termPts.join(' L ')} L ${RMX + RW},${dayEdgeY} Z`;
  // great-circle path QTH→selected, sampled & split at the dateline
  const gcSegments = React.useMemo(() => {
    if (!selSpot) return [];
    const toXYZ = (la, lo) => { la *= Math.PI / 180; lo *= Math.PI / 180; return [Math.cos(la) * Math.cos(lo), Math.cos(la) * Math.sin(lo), Math.sin(la)]; };
    const a = toXYZ(Q.lat, Q.lon), b = toXYZ(selSpot.lat, selSpot.lon);
    const dot = Math.max(-1, Math.min(1, a[0] * b[0] + a[1] * b[1] + a[2] * b[2])), om = Math.acos(dot);
    const segs = []; let cur = []; let prevX = null;
    for (let i = 0; i <= 64; i++) {
      const t = i / 64, s1 = om < 1e-6 ? 1 - t : Math.sin((1 - t) * om) / Math.sin(om), s2 = om < 1e-6 ? t : Math.sin(t * om) / Math.sin(om);
      const v = [a[0] * s1 + b[0] * s2, a[1] * s1 + b[1] * s2, a[2] * s1 + b[2] * s2];
      const la = Math.atan2(v[2], Math.hypot(v[0], v[1])) * 180 / Math.PI, lo = Math.atan2(v[1], v[0]) * 180 / Math.PI;
      const x = lon2x(lo), y = lat2y(la);
      if (prevX !== null && Math.abs(x - prevX) > RW / 2) { segs.push(cur); cur = []; }
      cur.push(`${x.toFixed(1)},${y.toFixed(1)}`); prevX = x;
    }
    if (cur.length) segs.push(cur);
    return segs;
  }, [sel]);

  // basemap frame geometry (aspect-locked so the SVG overlay maps 1:1)
  const rectTotalW = RW + RMX * 2, rectTotalH = RH + RMY * 2;
  const azAR = 1, rectAR = rectTotalW / rectTotalH;
  const frameAR = proj === 'azimuthal' ? azAR : rectAR;
  const azInset = { left: `${(cx - R) / VB * 100}%`, top: `${(cy - R) / VB * 100}%`, width: `${2 * R / VB * 100}%`, height: `${2 * R / VB * 100}%` };
  const rectInset = { left: `${RMX / rectTotalW * 100}%`, top: `${RMY / rectTotalH * 100}%`, width: `${RW / rectTotalW * 100}%`, height: `${RH / rectTotalH * 100}%` };

  // rotor/instrument data (rotor heading is bound to the beam slider)
  const azDir = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'][Math.round((((beamAz % 360) + 360) % 360) / 45) % 8];
  const az3 = String(((beamAz % 360) + 360) % 360).padStart(3, '0');
  const ROTOR_PRESETS = [['EU', 50], ['AF', 95], ['SA', 155], ['Carib', 135], ['AS', 330], ['OC', 250]];
  const SOLAR = [['SFI', '168'], ['SN', '142'], ['A', '7'], ['K', '2'], ['X-ray', 'B1'], ['304Å', '171'], ['Aur', '1.5'], ['MUF', '28']];
  const BANDCOND = [['80–40m', 'good', 'good'], ['30–20m', 'good', 'fair'], ['17–15m', 'fair', 'poor'], ['12–10m', 'fair', 'poor']];

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label="J-Map · Propagation map">
      <div className="sx-shell">
      <SuiteDock active="map" />
      <div className="jm-win">
        {/* top bar */}
        <div className="jm-top">
          <div className="jm-brand"><div className="ic">{g('map', 18)}</div><div><div className="nm">J-Map</div><div className="ct">Propagation & spots · {Q.grid}</div></div></div>
          <div className="jm-stat"><div className="k">Spots / hr</div><div className="v">342</div></div>
          <div className="jm-stat"><div className="k">Shown</div><div className="v">{shown.length}</div></div>
          <div className="jm-stat"><div className="k">Gray line</div><div className="v" style={{ fontSize: 13 }}>SR 11:02 · SS 22:48</div></div>
          <div className="jm-stat"><div className="k">Beam</div><div className="v" style={{ color: 'var(--accent)' }}>{String(beamAz).padStart(3, '0')}°</div></div>
          <div className="jm-top-sp"></div>
          <div className="jm-clock"><div className="k">UTC</div><div className="v ars-mono">{utc}</div></div>
        </div>

        <div className="jm-body">
          {/* LEFT — filters */}
          <div className="jm-rail ars-scroll">
            <div>
              <div className="jm-sec-t">Projection</div>
              <div className="jm-seg">{[['azimuthal', 'Azimuthal'], ['rect', 'Rectangular']].map(([v, l]) => <button key={v} className={proj === v ? 'on' : ''} onClick={() => setProj(v)}>{l}</button>)}</div>
            </div>
            <div>
              <div className="jm-sec-t" style={{ display: 'flex', justifyContent: 'space-between' }}><span>Bands</span><span style={{ color: 'var(--accent)', cursor: 'pointer' }} onClick={allBands}>All</span></div>
              <div className="jm-bandgrid">
                {JMAP_BANDS.map(b => (
                  <button key={b} className={`jm-bandbtn ${bands.has(b) ? 'on' : ''}`} onClick={() => toggleBand(b)}>
                    <span className="sw" style={{ background: BAND_COLOR[b] }}></span>{b}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <div className="jm-sec-t">Mode</div>
              <div className="jm-seg">{['All', 'CW', 'Phone', 'Digi'].map(m => <button key={m} className={modeF === m ? 'on' : ''} onClick={() => setModeF(m)}>{m}</button>)}</div>
            </div>
            <div>
              <div className="jm-sec-t">Spot age ≤ {maxAge} min</div>
              <div className="jm-age"><input className="ws-range" type="range" min="2" max="20" value={maxAge} onChange={e => setMaxAge(+e.target.value)} style={{ flex: 1 }} /></div>
            </div>
            <div>
              <div className="jm-sec-t">Overlays</div>
              <div className={`jm-tog ${showGray ? 'on' : ''}`} onClick={() => setShowGray(v => !v)}><span className="lb">Gray line / sun</span><span className="jm-sw"></span></div>
              <div className={`jm-tog ${showBase ? 'on' : ''}`} onClick={() => setShowBase(v => !v)}><span className="lb">Basemap image</span><span className="jm-sw"></span></div>
              <div className={`jm-tog ${showBeam ? 'on' : ''}`} onClick={() => setShowBeam(v => !v)}><span className="lb">Beam heading</span><span className="jm-sw"></span></div>
              <div className={`jm-tog ${showRings ? 'on' : ''}`} onClick={() => setShowRings(v => !v)}><span className="lb">Range rings</span><span className="jm-sw"></span></div>
              <div className={`jm-tog ${showCalls ? 'on' : ''}`} onClick={() => setShowCalls(v => !v)}><span className="lb">Callsigns</span><span className="jm-sw"></span></div>
              <div className={`jm-tog ${needOnly ? 'on' : ''}`} onClick={() => setNeedOnly(v => !v)}><span className="lb">Needed only</span><span className="jm-sw"></span></div>
            </div>
            <div>
              <div className="jm-sec-t">Beam heading · {String(beamAz).padStart(3, '0')}°</div>
              <input className="ws-range" type="range" min="0" max="359" value={beamAz} onChange={e => setBeamAz(+e.target.value)} style={{ width: '100%' }} />
            </div>
          </div>

          {/* CENTER — map */}
          <div className="jm-map">
            <div className="jm-mapframe" style={{ aspectRatio: frameAR, width: `min(100%, calc(100cqh * ${frameAR}))` }}>
            {showBase && proj === 'azimuthal' && (
              <image-slot id="jm-base-az" class="jm-base az" shape="circle" fit="fill" placeholder="Drop an azimuthal-equidistant map centered on FN20" style={azInset}></image-slot>
            )}
            {showBase && proj === 'rect' && (
              <image-slot id="jm-base-rect" class="jm-base" shape="rect" fit="fill" src="bluemarble.jpg" placeholder="Drop an equirectangular world map (−180…180°)" style={rectInset}></image-slot>
            )}
            {proj === 'azimuthal' && (
            <svg viewBox={`0 0 ${VB} ${VB}`} preserveAspectRatio="none" width="100%" height="100%">
              <defs><clipPath id="jm-disc"><circle cx={cx} cy={cy} r={R} /></clipPath></defs>
              {/* outer horizon */}
              <circle cx={cx} cy={cy} r={R} fill={showBase ? 'none' : 'var(--surface-1)'} stroke="var(--border-2)" strokeWidth="1.5" />
              {/* sunlit overlay */}
              {showGray && (
                <g clipPath="url(#jm-disc)">
                  <circle cx={sunPt.x} cy={sunPt.y} r={rDay} fill="oklch(0.85 0.07 85 / 0.10)" stroke="oklch(0.85 0.09 85 / 0.45)" strokeWidth="1.5" strokeDasharray="3 4" />
                </g>
              )}
              {/* range rings */}
              {showRings && rings.map(km => {
                const rr = (km / MAXKM) * R;
                return <g key={km}>
                  <circle cx={cx} cy={cy} r={rr} fill="none" stroke="var(--border)" strokeWidth="1" />
                  <text className="jm-ring-lbl" x={cx + 3} y={cy - rr - 3}>{km / 1000}k km</text>
                </g>;
              })}
              {/* spokes + bearing labels */}
              {spokes.map(a => {
                const e = project(a, MAXKM, cx, cy, R);
                const lp = project(a, MAXKM * 1.06, cx, cy, R);
                return <g key={a}>
                  <line x1={cx} y1={cy} x2={e.x} y2={e.y} stroke="var(--border)" strokeWidth={a % 90 === 0 ? 1.2 : 0.7} opacity={a % 90 === 0 ? 0.9 : 0.5} />
                  <text className="jm-brg-lbl" x={lp.x} y={lp.y} textAnchor="middle" dominantBaseline="middle">{dirLabel(a)}</text>
                </g>;
              })}
              {/* beam wedge */}
              {showBeam && (
                <g>
                  <path d={`M ${cx} ${cy} L ${beamL.x} ${beamL.y} A ${R} ${R} 0 0 1 ${beamR.x} ${beamR.y} Z`} fill="var(--accent-dim)" />
                  <line x1={cx} y1={cy} x2={beamEnd.x} y2={beamEnd.y} stroke="var(--accent)" strokeWidth="2" />
                </g>
              )}
              {/* region anchors — edge direction markers */}
              {JMAP_REGIONS.map(rg => {
                const { az } = greatCircle(Q.lat, Q.lon, rg.lat, rg.lon);
                const p = project(az, MAXKM * 0.93, cx, cy, R);
                return <text key={rg.name} className="jm-region-lbl" x={p.x} y={p.y} textAnchor="middle" dominantBaseline="middle">{rg.name}</text>;
              })}
              {/* spots */}
              {shown.map(s => {
                const p = project(s.az, s.dist, cx, cy, R);
                const isSel = s.call === sel;
                return (
                  <g key={s.call} className={`jm-spot ${isSel ? 'sel' : ''}`} onClick={() => pick(s)}>
                    {isSel && <circle cx={p.x} cy={p.y} r="11" fill="none" stroke="var(--accent)" strokeWidth="1.5" />}
                    {s.need && <circle cx={p.x} cy={p.y} r="8" fill="none" stroke="var(--warn)" strokeWidth="1.3" opacity="0.8" />}
                    <circle cx={p.x} cy={p.y} r="5" fill={BAND_COLOR[s.band]} stroke="var(--surface-1)" strokeWidth="1.5" />
                    {showCalls && <text className="jm-spot-call" x={p.x + 8} y={p.y + 3.5}>{s.call}</text>}
                  </g>
                );
              })}
              {/* QTH center */}
              <circle cx={cx} cy={cy} r="4.5" fill="var(--accent)" />
              <circle cx={cx} cy={cy} r="8" fill="none" stroke="var(--accent)" strokeWidth="1.2" opacity="0.5" />
              <text className="jm-qth-lbl" x={cx} y={cy - 13} textAnchor="middle">{Q.call}</text>
            </svg>
            )}
            {proj === 'rect' && (
            <svg viewBox={`0 0 ${RW + RMX * 2} ${RH + RMY * 2}`} preserveAspectRatio="none" width="100%" height="100%">
              {/* ocean field */}
              <rect x={RMX} y={RMY} width={RW} height={RH} fill={showBase ? 'none' : 'var(--surface-1)'} stroke="var(--border-2)" strokeWidth="1.5" />
              {/* sunlit (gray line) */}
              {showGray && <path d={dayPath} fill="oklch(0.85 0.07 85 / 0.10)" />}
              {showGray && <polyline points={termPts.join(' ')} fill="none" stroke="oklch(0.85 0.09 85 / 0.5)" strokeWidth="1.5" strokeDasharray="3 4" />}
              {/* graticule */}
              {showRings && [-150, -120, -90, -60, -30, 0, 30, 60, 90, 120, 150].map(lon => (
                <line key={'x' + lon} x1={lon2x(lon)} y1={RMY} x2={lon2x(lon)} y2={RMY + RH} stroke="var(--border)" strokeWidth={lon === 0 ? 1.1 : 0.6} opacity={lon === 0 ? 0.9 : 0.5} />
              ))}
              {showRings && [-60, -30, 0, 30, 60].map(lat => (
                <line key={'y' + lat} x1={RMX} y1={lat2y(lat)} x2={RMX + RW} y2={lat2y(lat)} stroke="var(--border)" strokeWidth={lat === 0 ? 1.1 : 0.6} opacity={lat === 0 ? 0.9 : 0.5} />
              ))}
              {/* region anchors */}
              {JMAP_REGIONS.map(rg => <text key={rg.name} className="jm-region-lbl" x={lon2x(rg.lon)} y={lat2y(rg.lat)} textAnchor="middle" dominantBaseline="middle">{rg.name}</text>)}
              {/* great-circle path to selected */}
              {gcSegments.map((seg, i) => <polyline key={i} points={seg.join(' ')} fill="none" stroke="var(--accent)" strokeWidth="1.8" strokeDasharray="4 3" opacity="0.85" />)}
              {/* spots */}
              {shown.map(s => {
                const x = lon2x(s.lon), y = lat2y(s.lat), isSel = s.call === sel;
                return (
                  <g key={s.call} className={`jm-spot ${isSel ? 'sel' : ''}`} onClick={() => pick(s)}>
                    {isSel && <circle cx={x} cy={y} r="11" fill="none" stroke="var(--accent)" strokeWidth="1.5" />}
                    {s.need && <circle cx={x} cy={y} r="8" fill="none" stroke="var(--warn)" strokeWidth="1.3" opacity="0.8" />}
                    <circle cx={x} cy={y} r="5" fill={BAND_COLOR[s.band]} stroke="var(--surface-1)" strokeWidth="1.5" />
                    {showCalls && <text className="jm-spot-call" x={x + 8} y={y + 3.5}>{s.call}</text>}
                  </g>
                );
              })}
              {/* QTH */}
              <circle cx={lon2x(Q.lon)} cy={lat2y(Q.lat)} r="4.5" fill="var(--accent)" />
              <circle cx={lon2x(Q.lon)} cy={lat2y(Q.lat)} r="8" fill="none" stroke="var(--accent)" strokeWidth="1.2" opacity="0.5" />
              <text className="jm-qth-lbl" x={lon2x(Q.lon)} y={lat2y(Q.lat) - 13} textAnchor="middle">{Q.call}</text>
            </svg>
            )}
            </div>
            <div className="jm-leg">
              <div className="jm-leg-row" style={{ color: 'var(--t1)', fontWeight: 600, marginBottom: 2 }}>{proj === 'azimuthal' ? `Azimuthal · centered ${Q.grid}` : 'Rectangular · lat / lon'}</div>
              <div className="jm-leg-row"><span className="d" style={{ background: 'var(--accent)' }}></span>your station{proj === 'azimuthal' ? ' / beam' : ' / path'}</div>
              <div className="jm-leg-row"><span className="d" style={{ background: 'none', border: '1.3px solid var(--warn)' }}></span>needed DX</div>
              <div className="jm-leg-row"><span className="d" style={{ background: 'oklch(0.85 0.09 85 / 0.55)' }}></span>sunlit (gray line)</div>
            </div>
            <div className="jm-mapinfo"><b>{shown.length}</b> spots · ≤ <b>{maxAge}m</b><br />{proj === 'azimuthal' ? <React.Fragment>rings = distance<br />spokes = bearing</React.Fragment> : <React.Fragment>grid = 30° lat/lon<br />dash = great-circle</React.Fragment>}</div>
          </div>

          {/* RIGHT — drawer rail */}
          <div className="sx-rail ars-scroll">
            <SuiteDrawer title="DX Spots" hue="map" glyph={g('map', 14)} defaultOpen summary={`${shown.length} live`}>
              <div className="jm-dwlist ars-scroll">
                {[...shown].sort((a, b) => a.age - b.age).map(s => (
                  <div key={s.call} className={`jm-row ${s.call === sel ? 'sel' : ''}`} onClick={() => pick(s)}>
                    <span className="sw" style={{ background: BAND_COLOR[s.band] }}></span>
                    <span className="fq">{s.f}</span>
                    <span><span className="cl">{s.call}</span>{s.need && <span className="need">NEED</span>}<div className="rg">{s.region} · {String(Math.round(s.az)).padStart(3, '0')}°</div></span>
                    <span className="ag">{s.age}m</span>
                  </div>
                ))}
              </div>
            </SuiteDrawer>

            <SuiteDrawer title="DX station info" hue="bridge" glyph={g('vault', 14)} defaultOpen summary={selSpot ? selSpot.call : '—'}>
              {selSpot ? (
                <div style={{ marginTop: 4 }}>
                  <div className="jm-sel-top">
                    <div style={{ flex: 1 }}><div className="jm-sel-call">{selSpot.call}</div><div className="jm-sel-reg">{selSpot.region}</div></div>
                    <span className="jm-sel-band" style={{ background: `color-mix(in oklch, ${BAND_COLOR[selSpot.band]} 22%, transparent)`, color: BAND_COLOR[selSpot.band] }}>{selSpot.band}m {selSpot.mode}</span>
                  </div>
                  <div className="jm-sel-meta">
                    <div><div className="k">Freq</div><div className="v" style={{ color: 'var(--accent)' }}>{selSpot.f}</div></div>
                    <div><div className="k">Bearing</div><div className="v">{String(Math.round(selSpot.az)).padStart(3, '0')}°</div></div>
                    <div><div className="k">Distance</div><div className="v">{selSpot.dist.toLocaleString(undefined, { maximumFractionDigits: 0 })}<span style={{ fontSize: 11, color: 'var(--t3)' }}> km</span></div></div>
                  </div>
                  <div className="jm-sel-act">
                    <button className="ars-btn primary" onClick={() => setFreq(selSpot.f)} style={{ flex: 1, justifyContent: 'center' }}>Tune {selSpot.f}</button>
                    <button className="ars-btn jm-btn-beam" onClick={() => beamTo(selSpot)}>Beam {String(Math.round(selSpot.az)).padStart(3, '0')}°</button>
                  </div>
                </div>
              ) : <div className="jm-empty">Select a spot to see DXCC, beam heading & distance.</div>}
            </SuiteDrawer>

            <SuiteDrawer title="Antenna · Rotor" hue="sat" glyph={g('sat', 14)} defaultOpen summary={`${az3}° ${azDir}`}>
              <div className="jm-ro-top">
                <ARSCompass az={beamAz} size={92} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div className="jm-ro-head">{az3}<span style={{ fontSize: 13, color: 'var(--t3)' }}>°</span></div>
                  <div className="jm-ro-dir">{azDir} · short path</div>
                  <div className="jm-ro-turn">
                    <button onClick={() => setBeamAz(a => (a - 5 + 360) % 360)}>◀</button>
                    <button className="stop">Stop</button>
                    <button onClick={() => setBeamAz(a => (a + 5) % 360)}>▶</button>
                  </div>
                </div>
              </div>
              <input className="ws-range" type="range" min="0" max="359" value={beamAz} onChange={e => setBeamAz(+e.target.value)} style={{ width: '100%' }} />
              <div className="jm-ro-presets">{ROTOR_PRESETS.map(([l, az]) => <button key={l} className={`jm-ro-preset ${beamAz === az ? 'on' : ''}`} onClick={() => setBeamAz(az)}>{l}<b>{az}°</b></button>)}</div>
            </SuiteDrawer>

            <SuiteDrawer title="Propagation" hue="map" glyph={g('map', 14)} summary="20m → EU">
              <div className="jm-kv"><span className="k">Best band now</span><span className="v" style={{ color: 'var(--ok)' }}>20m → EU</span></div>
              <div className="jm-kv"><span className="k">MUF (3000 km)</span><span className="v">28.4 MHz</span></div>
              <div className="jm-kv"><span className="k">Gray line</span><span className="v">SR 11:02 · SS 22:48</span></div>
              <div className="jm-kv"><span className="k">Sporadic-E</span><span className="v" style={{ color: 'var(--t3)' }}>none</span></div>
              <div className="jm-kv"><span className="k">Aurora</span><span className="v" style={{ color: 'var(--ok)' }}>quiet</span></div>
            </SuiteDrawer>

            <SuiteDrawer title="Space weather" hue="digi" glyph={g('digi', 14)} summary="SFI 168 · K2">
              <div className="jm-swx">{SOLAR.map(([k, v]) => <div key={k} className="jm-swx-c"><div className="jm-swx-k">{k}</div><div className="jm-swx-v">{v}</div></div>)}</div>
              {BANDCOND.map(([b, d, n]) => <div key={b} className="jm-bc"><span className="bn">{b}</span><span className={`c ${d}`}>{d}</span><span className={`c ${n}`}>{n}</span></div>)}
              <div className="jm-dw-stamp">NOAA SWPC · 14:30Z</div>
            </SuiteDrawer>

            <SuiteDrawer title="Weather · FN20" hue="bridge" glyph={g('bridge', 14)} summary="14° NW12">
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, margin: '8px 0 10px' }}><span className="ars-mono" style={{ fontSize: 28, fontWeight: 600 }}>14°</span><span style={{ fontSize: 12, color: 'var(--t2)' }}>Partly cloudy · feels 12°</span></div>
              <div className="jm-kv"><span className="k">Wind</span><span className="v">NW 12 · g21</span></div>
              <div className="jm-kv"><span className="k">Humidity</span><span className="v">48%</span></div>
              <div className="jm-kv"><span className="k">Pressure</span><span className="v">1018 hPa ↑</span></div>
            </SuiteDrawer>
          </div>
        </div>
      </div>
      </div>
    </div>
  );
}

window.JMap = JMap;
