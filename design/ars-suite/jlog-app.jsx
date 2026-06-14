/* J-Log — contest logging cockpit, contest-driven entry.
   Pick a contest → the entry pane reshapes to that contest's exchange fields,
   sent exchange, serial use, and multiplier rule. */

function JLDrawer({ title, hue, glyph, summary, defaultOpen, children }) {
  const [open, setOpen] = React.useState(!!defaultOpen);
  return (
    <div className={`jl-dw ${open ? 'open' : ''}`} style={{ '--h': `var(--${hue})` }}>
      <button className="jl-dw-head" onClick={() => setOpen(o => !o)}>
        <span className="jl-dw-ic">{glyph}</span>
        <span className="jl-dw-t">{title}</span>
        {!open && summary && <span className="jl-dw-sum">{summary}</span>}
        <span className="jl-dw-cv"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"><polyline points="9 6 15 12 9 18"/></svg></span>
      </button>
      <div className="jl-dw-wrap"><div className="jl-dw-inner"><div className="jl-dw-body">{children}</div></div></div>
    </div>
  );
}

function JLogCockpit({ theme = 'dark' }) {
  const [contestId, setContestId] = React.useState('cqww');
  const contest = CONTESTS.find(c => c.id === contestId);
  const general = !!contest.general;
  const defaults = (c) => Object.fromEntries(c.fields.map(f => [f.k, f.d]));

  const [call, setCall] = React.useState('');
  const [rcv, setRcv] = React.useState(() => defaults(contest));
  const [mults, setMults] = React.useState(() => new Set(contest.seed || []));
  const [serial, setSerial] = React.useState(1285);
  const [qCount, setQCount] = React.useState(1284);
  const [runSp, setRunSp] = React.useState('Run');
  const [opMode, setOpMode] = React.useState('SSB');
  const [freq, setFreq] = React.useState('14.250');
  const [band] = React.useState('20');
  const [rotorAz, setRotorAz] = React.useState(45);
  const [showDet, setShowDet] = React.useState(false);
  const [det, setDet] = React.useState({});
  const setD = (k, v) => setDet(d => ({ ...d, [k]: v }));
  const [qsos, setQsos] = React.useState(JLOG_QSOS);
  const [freshId, setFreshId] = React.useState(null);
  const [freshMult, setFreshMult] = React.useState(null);
  const [lastMacro, setLastMacro] = React.useState(null);
  const [now, setNow] = React.useState(() => new Date());
  const callRef = React.useRef(null);

  React.useEffect(() => { const i = setInterval(() => setNow(new Date()), 1000); return () => clearInterval(i); }, []);
  const utc = now.toISOString().slice(11, 19);

  const switchContest = (id) => {
    const c = CONTESTS.find(x => x.id === id);
    setContestId(id); setRcv(defaults(c)); setMults(new Set(c.seed || [])); setCall('');
    setTimeout(() => callRef.current && callRef.current.focus(), 0);
  };

  const callU = call.toUpperCase().trim();
  const dx = jlDxcc(callU);
  const sentDisplay = contest.serial ? (contest.mySent || '').replace('#', String(serial)) : contest.mySent;
  const dupe = qsos.some(q => q.call === callU);
  const multVal = () => {
    if (contest.multPrefix) return callU ? wpxPrefix(callU) : null;
    if (contest.multField) { const v = (rcv[contest.multField] || '').toUpperCase().trim(); return v || null; }
    return null;
  };
  const mv = multVal();
  const newMult = !!mv && !mults.has(mv);
  const MULTCALLS = new Set(JLOG_BANDMAP.filter(s => s.mult && !s.me).map(s => s.call));
  const partial = callU ? JLOG_MASTER.filter(c => c.startsWith(callU)).slice(0, 9) : [];
  const status = dupe ? ['dupe', 'DUPE — worked'] : (!general && newMult) ? ['mult', `NEW MULT · ${mv}`] : callU ? ['new', general ? 'NEW QSO' : 'NEW · 3 pts'] : null;

  const focusCall = () => callRef.current && callRef.current.focus();
  const logQso = () => {
    if (!callU) return;
    const rcvStr = contest.fields.map(f => rcv[f.k]).filter(Boolean).join(' ') || '—';
    const isNew = !general && newMult;
    const q = { t: utc.slice(0, 5), call: callU, band, mode: opMode, snt: general ? '59' : sentDisplay, rcv: rcvStr, mult: isNew, _id: Date.now() };
    setQsos(qs => [q, ...qs]); setFreshId(q._id); setQCount(n => n + 1);
    if (contest.serial) setSerial(s => s + 1);
    if (isNew) { setMults(m => new Set([...m, mv])); setFreshMult(mv); setTimeout(() => setFreshMult(null), 1400); }
    setRcv(defaults(contest)); setCall(''); setDet({}); focusCall();
  };
  const tuneSpot = (s) => { if (s.me) return; setFreq(s.f); setCall(s.call); focusCall(); };
  const fireMacro = (m) => { setLastMacro(m[0]); setTimeout(() => setLastMacro(null), 900); };
  const onKey = (e) => { if (e.key === 'Enter') logQso(); };
  const g = (id, size = 15) => <ARSGlyph id={id} size={size} stroke={1.9} />;

  const BM_MIN = 14.150, BM_MAX = 14.350;
  const bmTop = (f) => `${((parseFloat(f) - BM_MIN) / (BM_MAX - BM_MIN)) * 100}%`;
  const ticks = ['14.150', '14.200', '14.250', '14.300', '14.350'];
  const clusterRows = JLOG_BANDMAP.filter(s => !s.me).map((s, i) => ({ ...s, age: `${i + 1}m` }));
  const azDir = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'][Math.round((((rotorAz % 360) + 360) % 360) / 45) % 8];
  const az3 = String(((rotorAz % 360) + 360) % 360).padStart(3, '0');

  const scoreChips = general
    ? [['QSOs', qCount.toLocaleString()], ['Rate', '38', '/hr'], ['Best hr', '94']]
    : [['QSOs', qCount.toLocaleString()], ['Mults', String(mults.size)], ['Score', (qCount * mults.size).toLocaleString()]];

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label={`J-Log · ${contest.name}`}>
      <div className="sx-shell">
      <SuiteDock active="log" />
      <div className="jl-win">
        {/* scoreboard */}
        <div className="jl-top">
          <div className="jl-brand">
            <div className="ic">{g('log', 19)}</div>
            <div><div className="nm">J-Log</div><div className="ct">{contest.name}{!general && contest.mult ? ` · ${contest.mult}` : ''}</div></div>
          </div>
          <div className="jl-scorebar">
            {scoreChips.map(([k, v, of], i) => (
              <div key={k} className={`jl-sc ${k === 'Score' || k === 'Rate' ? 'hi' : ''}`}>
                <div className="k">{k}</div><div className="v">{v}{of && <span className="of"> {of}</span>}</div>
              </div>
            ))}
          </div>
          <div className="jl-clock"><div className="k">UTC</div><div className="v ars-mono">{utc}</div></div>
          <select className="jl-csel" value={contestId} onChange={e => switchContest(e.target.value)}>
            {CONTESTS.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>

        <div className="jl-body">
          {/* PRIMARY — entry + log */}
          <div className="jl-col">
            <div className="jl-entry">
              <div className="jl-entry-top">
                <span className="jl-ctitle">{general ? 'QSO entry' : 'Exchange'}</span>
                <div className="jl-runsp">
                  {['Run', 'S&P'].map(x => <button key={x} className={runSp === x ? 'on' : ''} onClick={() => setRunSp(x)}>{x}</button>)}
                </div>
                {contest.serial && <div className="jl-serial"><span className="k">Nr Snt</span><span className="v">{String(serial).padStart(4, '0')}</span></div>}
                <span className="jl-emeta">{general ? 'Ragchew — no scoring' : <React.Fragment>Mult: <b>{contest.mult}</b></React.Fragment>}</span>
              </div>
              <div className="jl-erow">
                <div className="jl-ef"><span className="lbl">Callsign</span><input ref={callRef} className="jl-ein call" value={call} onChange={e => setCall(e.target.value)} onKeyDown={onKey} placeholder="—" autoFocus /></div>
                {!general && <div className="jl-snt-disp"><span className="lbl">Snt</span><span className="val">{sentDisplay}</span></div>}
                {contest.fields.map(f => (
                  <div className="jl-ef" key={f.k}>
                    <span className="lbl">{f.l}</span>
                    <input className={`jl-fin ${f.mono ? '' : 'text'}`} style={{ width: f.w }} value={rcv[f.k] || ''} onChange={e => setRcv(r => ({ ...r, [f.k]: e.target.value }))} onKeyDown={onKey} />
                  </div>
                ))}
                <div className="jl-elog"><button className="jl-elogbtn" onClick={logQso}>Log ⏎</button></div>
              </div>
              <div className="jl-under">
                <div className="jl-cp">
                  {partial.length ? partial.map(c => (
                    <span key={c} className={`jl-cp-item ${qsos.some(q => q.call === c) ? 'dupe' : ''} ${MULTCALLS.has(c) ? 'mult' : ''}`} onClick={() => { setCall(c); focusCall(); }}>{c}</span>
                  )) : <span className="jl-cp-empty">Check-partial — start typing a callsign…</span>}
                </div>
                {status && <span className={`jl-status ${status[0]}`}>{status[1]}</span>}
              </div>

              <button className={`jl-details-tog ${showDet ? 'open' : ''}`} onClick={() => setShowDet(s => !s)}>
                <span className="cv"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><polyline points="9 6 15 12 9 18"/></svg></span>
                Full QSO details {dx && <span style={{ color: 'var(--t4)', fontWeight: 500 }}>· {dx.country}</span>}
              </button>
              <div className={`jl-details-wrap ${showDet ? 'open' : ''}`}>
                <div className="jl-details-inner">
                  <div className="jl-detgrid">
                    <div className="jl-detf"><span className="lbl">Name</span><input value={det.name || ''} onChange={e => setD('name', e.target.value)} /></div>
                    <div className="jl-detf"><span className="lbl">QTH</span><input value={det.qth || ''} onChange={e => setD('qth', e.target.value)} /></div>
                    <div className="jl-detf"><span className="lbl">State / Prov</span><input className="mono" value={det.state || ''} onChange={e => setD('state', e.target.value)} /></div>
                    <div className="jl-detf"><span className="lbl">County</span><input value={det.county || ''} onChange={e => setD('county', e.target.value)} /></div>
                    <div className="jl-detf"><span className="lbl">Grid</span><input className="mono" value={det.grid || ''} onChange={e => setD('grid', e.target.value)} /></div>
                    <div className="jl-detf auto"><span className="lbl">DXCC country</span><input value={dx ? dx.country : ''} placeholder="auto" readOnly /></div>
                    <div className="jl-detf auto"><span className="lbl">CQ zone</span><input className="mono" value={dx ? dx.cq : ''} placeholder="auto" readOnly /></div>
                    <div className="jl-detf auto"><span className="lbl">ITU zone</span><input className="mono" value={dx ? dx.itu : ''} placeholder="auto" readOnly /></div>
                    <div className="jl-detf"><span className="lbl">IOTA</span><input className="mono" value={det.iota || ''} onChange={e => setD('iota', e.target.value)} /></div>
                    <div className="jl-detf"><span className="lbl">Operator</span><input className="mono" value={det.op || 'WM3J'} onChange={e => setD('op', e.target.value)} /></div>
                    <div className="jl-detf"><span className="lbl">TX power (W)</span><input className="mono" value={det.pwr || '100'} onChange={e => setD('pwr', e.target.value)} /></div>
                    <div className="jl-detf"><span className="lbl">QSL via</span><input value={det.qsl || ''} onChange={e => setD('qsl', e.target.value)} placeholder="LoTW / direct…" /></div>
                    <div className="jl-detf"><span className="lbl">My antenna</span><input value={det.ant || 'Hex beam'} onChange={e => setD('ant', e.target.value)} /></div>
                    <div className="jl-detf"><span className="lbl">Prop mode</span>
                      <select value={det.prop || ''} onChange={e => setD('prop', e.target.value)}>
                        <option value="">—</option><option>Ionospheric</option><option>Sporadic-E</option><option>Meteor scatter</option><option>EME</option><option>Satellite</option><option>Tropo</option>
                      </select>
                    </div>
                    <div className="jl-detf"><span className="lbl">POTA / SOTA ref</span><input className="mono" value={det.park || ''} onChange={e => setD('park', e.target.value)} placeholder="K-1234" /></div>
                    <div className="jl-detf" style={{ gridColumn: '1 / -1' }}><span className="lbl">Comment</span><input value={det.cmt || ''} onChange={e => setD('cmt', e.target.value)} placeholder="notes, QSL info, rig…" /></div>
                  </div>
                </div>
              </div>
            </div>

            <div className="jl-loghd">{g('log', 14)} {general ? "Today's log" : 'Contest log'} <span style={{ color: 'var(--t4)', fontWeight: 400, letterSpacing: 0, textTransform: 'none' }}>· {qsos.length} QSOs shown</span></div>
            <div className="jl-th"><span>UTC</span><span>Call</span><span>Bnd</span><span>Mode</span><span>Snt</span><span>Rcv</span><span style={{ justifySelf: 'center' }}>M</span></div>
            <div className="jl-list ars-scroll">
              {qsos.map((q, i) => (
                <div key={q._id || i} className={`jl-tr ${q._id && q._id === freshId ? 'fresh' : ''}`}>
                  <span className="mono">{q.t}</span>
                  <span className="callc">{q.call}</span>
                  <span className="mono">{q.band}</span>
                  <span className="modec">{q.mode}</span>
                  <span className="mono">{q.snt}</span>
                  <span className="mono">{q.rcv}</span>
                  <span className={`jl-multdot ${q.mult ? 'on' : ''}`}></span>
                </div>
              ))}
            </div>
          </div>

          {/* SUPPORT col 2 — band map */}
          <div className="jl-col sep">
            <div className="jl-pane grow">
              <div className="jl-ph" style={{ '--h': 'var(--map)' }}><span className="ic">{g('map', 14)}</span><span className="t">Band map</span><span className="meta">{band}m · {opMode}</span></div>
              <div className="jl-bm ars-scroll">
                <div className="jl-bm-scale">
                  {ticks.map(t => <div key={t} className="jl-bm-tick" style={{ top: bmTop(t) }}><span className="fq">{t}</span><span className="ln"></span></div>)}
                  {JLOG_BANDMAP.map((s, i) => s.me ? (
                    <React.Fragment key={i}>
                      <div className="jl-bm-me-line" style={{ top: bmTop(s.f) }}></div>
                      <div className="jl-bm-spot me" style={{ top: bmTop(s.f) }}><span className="dot"></span><span className="cl">{s.call} ◄</span></div>
                    </React.Fragment>
                  ) : (
                    <div key={i} className={`jl-bm-spot ${s.mult ? 'mult' : ''} ${s.need ? 'need' : ''}`} style={{ top: bmTop(s.f) }} onClick={() => tuneSpot(s)} title={`Tune ${s.f}`}>
                      <span className="dot"></span><span className="cl">{s.call}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* SUPPORT col 3 — drawer rail */}
          <div className="jl-col sep">
            <div className="jl-rail ars-scroll">
              {general ? (
                <JLDrawer title="QSO notes" hue="learn" glyph={g('learn', 14)} defaultOpen>
                  <textarea style={{ width: '100%', minHeight: 96, resize: 'vertical', fontFamily: 'IBM Plex Sans, sans-serif', fontSize: 13, color: 'var(--t1)', background: 'var(--bg)', border: '1px solid var(--border-2)', borderRadius: 8, padding: 10, marginTop: 8 }} placeholder="Rag-chew notes…" defaultValue={"Nice chat — 100W to a hex beam.\nWX sunny, 14°C. QSL via LoTW."}></textarea>
                </JLDrawer>
              ) : (
                <React.Fragment>
                  <JLDrawer title={contest.mult} hue="log" glyph={g('vault', 14)} defaultOpen summary={`${mults.size} wkd`}>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 5, marginTop: 9, maxHeight: 132, overflow: 'auto' }} className="ars-scroll">
                      {[...mults].map(m => <span key={m} className={`jl-mchip ${freshMult === m ? 'fresh' : ''}`}>{m}</span>)}
                    </div>
                  </JLDrawer>
                  <JLDrawer title="Rate" hue="accent" glyph={g('digi', 14)} defaultOpen summary="38/hr">
                    <div className="jl-rate" style={{ paddingLeft: 0, paddingRight: 0 }}>{JLOG_RATE.map((r, i) => <i key={i} style={{ height: `${r / 7 * 100}%` }}></i>)}</div>
                    <div className="jl-rate-foot" style={{ paddingLeft: 0, paddingRight: 0 }}><span>last 10 min</span><span><b>52</b> Q · <b>9</b> mult/hr</span></div>
                  </JLDrawer>
                </React.Fragment>
              )}

              <JLDrawer title="DX Cluster" hue="map" glyph={g('map', 14)} defaultOpen summary="live">
                <div style={{ margin: '4px -13px 0' }}>
                  {clusterRows.map((s, i) => (
                    <div key={i} className="jl-cluspot" onClick={() => tuneSpot(s)}>
                      <span className="fq">{s.f}</span><span className="cl" style={s.mult ? { color: 'var(--warn)' } : {}}>{s.call}</span>
                      {s.mult && <span style={{ fontSize: 9, fontWeight: 800, color: 'var(--warn)' }}>MULT</span>}
                      <span className="age">{s.age}</span>
                    </div>
                  ))}
                </div>
              </JLDrawer>

              <JLDrawer title="Antenna · Rotor" hue="sat" glyph={g('sat', 14)} summary={`${az3}° ${azDir}`}>
                <div className="jl-ro-top">
                  <ARSCompass az={rotorAz} size={92} />
                  <div className="jl-ro-info">
                    <div className="jl-ro-head">{az3}<span style={{ fontSize: 13, color: 'var(--t3)' }}>°</span></div>
                    <div className="jl-ro-dir">{azDir} · short path</div>
                    <div className="jl-ro-turn">
                      <button onClick={() => setRotorAz(a => (a - 5 + 360) % 360)}>◀</button>
                      <button className="stop">Stop</button>
                      <button onClick={() => setRotorAz(a => (a + 5) % 360)}>▶</button>
                    </div>
                  </div>
                </div>
                <input className="ws-range" type="range" min="0" max="359" value={rotorAz} onChange={e => setRotorAz(+e.target.value)} style={{ width: '100%' }} />
                <div className="jl-ro-presets">{JL_ROTOR_PRESETS.map(([l, az]) => <button key={l} className={`jl-ro-preset ${rotorAz === az ? 'on' : ''}`} onClick={() => setRotorAz(az)}>{l}<b>{az}°</b></button>)}</div>
              </JLDrawer>

              <JLDrawer title="Propagation" hue="map" glyph={g('map', 14)} summary="20m → EU">
                <div className="jl-kv"><span className="k">Best band now</span><span className="v" style={{ color: 'var(--ok)' }}>20m → EU</span></div>
                <div className="jl-kv"><span className="k">MUF (3000 km)</span><span className="v">28.4 MHz</span></div>
                <div className="jl-kv"><span className="k">Gray line</span><span className="v">SR 11:02 · SS 22:48</span></div>
                <div className="jl-kv"><span className="k">Sporadic-E</span><span className="v" style={{ color: 'var(--t3)' }}>none</span></div>
                <div className="jl-kv"><span className="k">Aurora</span><span className="v" style={{ color: 'var(--ok)' }}>quiet</span></div>
              </JLDrawer>

              <JLDrawer title="Space weather" hue="digi" glyph={g('digi', 14)} summary="SFI 168 · K2">
                <div className="jl-sw-grid">{JL_SOLAR.map(([k, v]) => <div key={k} className="jl-sw-cell"><div className="jl-sw-k">{k}</div><div className="jl-sw-v">{v}</div></div>)}</div>
                <div className="jl-sw-bands">
                  {JL_BANDCOND.map(([b, d, n]) => <div key={b} className="jl-sw-brow"><span className="jl-sw-bn">{b}</span><span className={`jl-sw-c ${d}`}>{d}</span><span className={`jl-sw-c ${n}`}>{n}</span></div>)}
                </div>
                <div className="jl-dw-stamp">NOAA SWPC · 14:30Z</div>
              </JLDrawer>

              <JLDrawer title="Weather · FN20" hue="bridge" glyph={g('bridge', 14)} summary="14° NW12">
                <div className="jl-wx-big"><span className="jl-wx-temp">14°</span><span className="jl-wx-cond">Partly cloudy · feels 12°</span></div>
                <div className="jl-kv"><span className="k">Wind</span><span className="v">NW 12 · g21</span></div>
                <div className="jl-kv"><span className="k">Humidity</span><span className="v">48%</span></div>
                <div className="jl-kv"><span className="k">Pressure</span><span className="v">1018 hPa ↑</span></div>
                <div className="jl-kv"><span className="k">Visibility</span><span className="v">16 km</span></div>
              </JLDrawer>
            </div>
          </div>
        </div>

        {/* bottom — rig + macros */}
        <div className="jl-bottom">
          <div className="jl-rig">
            <div className="fq ars-mono">{freq}<span className="u">MHz</span></div>
            <div className="mb"><span className="chip accent">{opMode}</span><span className="chip">{band}m</span></div>
          </div>
          <div className="jl-macros">
            {JLOG_MACROS.map(m => (
              <button key={m[0]} className="jl-macro" onClick={() => fireMacro(m)} style={lastMacro === m[0] ? { borderColor: 'var(--accent)', background: 'var(--accent-dim)' } : {}}>
                <span className="fk">{m[0]}</span><span className="ml">{m[1]}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
      </div>
    </div>
  );
}

window.JLogCockpit = JLogCockpit;
