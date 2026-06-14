/* J-Digi — digital modes decode (CW / RTTY / PSK). Waterfall + a live decode
   window, mode tabs, and Tx macros, on the shared suite shell. */

const JD_MODES = {
  CW: { speed: '28 WPM', mark: 'morse', wf: [{ x: 0.5, w: 6, i: 1 }], wfmode: 'cw',
    text: [['dim', '1438Z  '], ['rx', 'CQ CQ CQ DE DL8WPX DL8WPX DL8WPX K\n'],
      ['dim', '1439Z  '], ['me', 'DL8WPX DE WM3J WM3J K\n'],
      ['dim', '1439Z  '], ['rx', 'WM3J DE DL8WPX = GM DR OM TKS FER CALL = UR RST 599 599 = NAME HANS HANS = QTH NR MUNICH MUNICH = HW CPY? WM3J DE DL8WPX KN\n'],
      ['dim', '1440Z  '], ['me', 'DL8WPX DE WM3J = FB HANS TKS RPRT = UR 599 ALSO = NAME JIM QTH NEW JERSEY = ']] },
  RTTY: { speed: '45.45 baud', mark: 'fsk', wf: [{ x: 0.46, w: 5, i: 0.95 }, { x: 0.53, w: 5, i: 0.95 }], wfmode: 'generic',
    text: [['dim', '1442Z  '], ['rx', 'RYRYRYRYRY DE G4ABC G4ABC\n'],
      ['dim', '1442Z  '], ['rx', 'CQ CQ DE G4ABC G4ABC CQ K\n'],
      ['dim', '1443Z  '], ['me', 'G4ABC DE WM3J WM3J K\n'],
      ['dim', '1443Z  '], ['rx', 'WM3J DE G4ABC UR 599 599 OP JOHN JOHN QTH LONDON LONDON = HW? WM3J DE G4ABC K\n'],
      ['dim', '1444Z  '], ['me', 'G4ABC DE WM3J 599 599 NJ NJ TU = ']] },
  PSK31: { speed: '31.25 baud', mark: 'psk', wf: [{ x: 0.5, w: 7, i: 1 }], wfmode: 'generic',
    text: [['dim', '1446Z  '], ['rx', 'CQ CQ de EA5K EA5K EA5K pse k\n'],
      ['dim', '1446Z  '], ['me', 'EA5K de WM3J WM3J kn\n'],
      ['dim', '1447Z  '], ['rx', 'WM3J de EA5K = Gud eve = ur rst 599 = name Pedro = qth Valencia = rig is K4 running 100w = ant hexbeam = hw? WM3J de EA5K kn\n'],
      ['dim', '1448Z  '], ['me', 'EA5K de WM3J = FB Pedro tnx = ']] },
};
const JD_MACROS = [['F1', 'CQ'], ['F2', 'Call'], ['F3', 'RST'], ['F4', 'QTH'], ['F5', 'BTU'], ['F6', '73'], ['F7', 'Tune']];

function JDigi({ theme = 'dark' }) {
  const [mode, setMode] = React.useState('CW');
  const [afc, setAfc] = React.useState(true);
  const [sql, setSql] = React.useState(true);
  const [now, setNow] = React.useState(() => new Date());
  React.useEffect(() => { const i = setInterval(() => setNow(new Date()), 1000); return () => clearInterval(i); }, []);
  const utc = now.toISOString().slice(11, 19);
  const M = JD_MODES[mode];
  const g = (id, size = 14) => <ARSGlyph id={id} size={size} stroke={1.9} />;

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label="J-Digi · Digital decode">
      <div className="sx-shell">
        <SuiteDock active="digi" />
        <div className="sx-modwin" style={{ '--h': 'var(--digi)' }}>
          <div className="sx-top">
            <div className="sx-brand" style={{ '--h': 'var(--digi)' }}><div className="ic">{g('digi', 18)}</div><div><div className="nm">J-Digi</div><div className="ct">Digital modes decode</div></div></div>
            <div className="sx-stat"><div className="k">Mode</div><div className="v" style={{ color: 'var(--digi)' }}>{mode}</div></div>
            <div className="sx-stat"><div className="k">Speed</div><div className="v">{M.speed}</div></div>
            <div className="sx-stat"><div className="k">Freq</div><div className="v accent">14.070</div></div>
            <div className="sx-stat"><div className="k">AFC</div><div className="v" style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 6 }}><span className={`ars-dot ${afc ? 'on' : 'off'}`}></span>{afc ? 'lock' : 'off'}</div></div>
            <div className="sx-top-sp"></div>
            <div className="sx-clock"><div className="k">UTC</div><div className="v ars-mono">{utc}</div></div>
          </div>

          <div className="sx-body jd-body">
            <div className="jd-main">
              {/* waterfall */}
              <div>
                <div className="jd-wf-top"><span className="t">Waterfall</span><span className="meta">14.070 MHz · {mode}</span></div>
                <div className="jd-wf-box"><SuiteWaterfall mode={M.wfmode} height={110} signals={M.wf} /><div className="jd-wf-cursor" style={{ left: `${M.wf[0].x * 100}%` }}></div></div>
                <div className="jd-wf-axis"><span>0</span><span>500</span><span>1000</span><span>1500</span><span>2000</span><span>2500 Hz</span></div>
              </div>

              {/* mode tabs + controls */}
              <div className="jd-bar">
                <div className="jd-tabs">{Object.keys(JD_MODES).map(m => <button key={m} className={`jd-tab ${mode === m ? 'on' : ''}`} onClick={() => setMode(m)}>{m}</button>)}</div>
                <div className="jd-ctl">
                  <span className={`jd-pill ${afc ? 'on' : ''}`} onClick={() => setAfc(v => !v)}>AFC</span>
                  <span className={`jd-pill ${sql ? 'on' : ''}`} onClick={() => setSql(v => !v)}>SQL</span>
                  <span className="jd-pill">RxID</span>
                </div>
              </div>

              {/* decode window */}
              <div className="jd-decode ars-scroll">
                {M.text.map(([cls, t], i) => <span key={i} className={cls}>{t}</span>)}
                <span className="jd-cursor"></span>
              </div>

              {/* tx */}
              <div className="jd-tx">
                <div className="jd-tx-row">
                  <input className="jd-tx-in" defaultValue={mode === 'CW' ? 'DL8WPX DE WM3J' : 'de WM3J'} />
                  <button className="jd-send">Send ▶</button>
                </div>
                <div className="jd-macros">{JD_MACROS.map(([fk, l]) => <button key={fk} className="jd-macro"><span className="fk">{fk}</span>{l}</button>)}</div>
              </div>
            </div>

            {/* right rail */}
            <div className="sx-rail ars-scroll">
              <SuiteDrawer title="Decoder" hue="digi" glyph={g('digi')} defaultOpen summary={mode}>
                <div className="sx-kv"><span className="k">Mode</span><span className="v" style={{ color: 'var(--digi)' }}>{mode}</span></div>
                <div className="sx-kv"><span className="k">Speed</span><span className="v">{M.speed}</span></div>
                <div className="sx-kv"><span className="k">AFC</span><span className="v" style={{ color: afc ? 'var(--ok)' : 'var(--t3)' }}>{afc ? 'locked' : 'off'}</span></div>
                <div className="sx-kv"><span className="k">Squelch</span><span className="v" style={{ color: sql ? 'var(--ok)' : 'var(--t3)' }}>{sql ? 'on' : 'off'}</span></div>
                <div className="sx-kv"><span className="k">Audio in</span><span className="v">−6 dB</span></div>
                <div className="sx-kv"><span className="k">Log to</span><span className="v" style={{ color: 'var(--ok)' }}>J-Log</span></div>
              </SuiteDrawer>
              <SuiteInstruments defaultRotor={50} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

window.JDigi = JDigi;
