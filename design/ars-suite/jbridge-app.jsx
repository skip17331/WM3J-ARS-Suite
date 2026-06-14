/* J-Bridge — WSJT-X / FT8 bridge. Waterfall + Band Activity / Rx Frequency
   decode tables + FT8 sequencing & Tx messages, on the shared suite shell. */

const JB_DECODES = [
  { t: '1415', db: -8, dt: '0.2', hz: 1577, msg: 'CQ DL8WPX JO62', cq: true, grid: 'JO62' },
  { t: '1415', db: -14, dt: '0.1', hz: 892, msg: 'K1ABC W2XYZ FN20' },
  { t: '1415', db: -2, dt: '0.3', hz: 1230, msg: 'CQ VK9DX RG29', cq: true, grid: 'RG29', need: true },
  { t: '1415', db: -16, dt: '-0.2', hz: 2104, msg: 'JA7QVI EA5K -11' },
  { t: '1415', db: -5, dt: '0.1', hz: 1450, msg: 'CQ DX 5U5R JN06', cq: true, grid: 'JN06', need: true },
  { t: '1415', db: -19, dt: '0.4', hz: 760, msg: 'CQ PY2NY GG66', cq: true, grid: 'GG66' },
  { t: '1430', db: -11, dt: '0.0', hz: 1577, msg: 'WM3J DL8WPX -08', me: true },
  { t: '1430', db: -3, dt: '0.2', hz: 1230, msg: 'CQ VK9DX RG29', cq: true, grid: 'RG29', need: true },
  { t: '1430', db: -22, dt: '-0.1', hz: 2360, msg: 'CQ CN2AA IM63', cq: true, grid: 'IM63', need: true },
  { t: '1430', db: -9, dt: '0.1', hz: 980, msg: 'G4ABC W1AW FN31' },
  { t: '1445', db: -12, dt: '0.2', hz: 1577, msg: 'WM3J DL8WPX R-08', me: true },
  { t: '1445', db: -7, dt: '0.3', hz: 1680, msg: 'CQ JA1XYZ PM95', cq: true, grid: 'PM95' },
];
const JB_RXFREQ = JB_DECODES.filter(d => d.hz === 1577 || d.me);

function JBridge({ theme = 'dark' }) {
  const [enableTx, setEnableTx] = React.useState(false);
  const [partner, setPartner] = React.useState(null);
  const [evenOdd, setEvenOdd] = React.useState('Even');
  const [sel, setSel] = React.useState(null);
  const [now, setNow] = React.useState(() => new Date());
  React.useEffect(() => { const i = setInterval(() => setNow(new Date()), 250); return () => clearInterval(i); }, []);
  const utc = now.toISOString().slice(11, 19);
  const epoch = Math.floor(now.getTime() / 1000);
  const inPeriod = (now.getTime() / 1000) % 15;
  const periodPct = (inPeriod / 15) * 100;
  const slotEven = Math.floor(epoch / 15) % 2 === 0;

  const g = (id, size = 14) => <ARSGlyph id={id} size={size} stroke={1.9} />;
  const call = (d) => {
    const c = d.msg.replace(/^CQ (DX )?/, '').split(' ')[0];
    setPartner({ call: c, grid: d.grid || '' }); setSel(d.msg + d.hz); setEnableTx(true);
  };
  const txMsgs = partner ? [
    ['Tx1', `${partner.call} WM3J FN20`],
    ['Tx2', `${partner.call} WM3J -08`],
    ['Tx3', `${partner.call} WM3J R-08`],
    ['Tx4', `${partner.call} WM3J RR73`],
    ['Tx5', `${partner.call} WM3J 73`],
    ['Tx6', 'CQ WM3J FN20'],
  ] : [['Tx6', 'CQ WM3J FN20']];
  const txActive = enableTx && (evenOdd === 'Even' ? slotEven : !slotEven);

  const wfSignals = [{ x: 0.21, w: 9, i: 0.9 }, { x: 0.32, w: 8, i: 0.7 }, { x: 0.45, w: 10, i: 1 }, { x: 0.58, w: 8, i: 0.6 }, { x: 0.7, w: 9, i: 0.85 }, { x: 0.82, w: 7, i: 0.5 }];

  const Row = (d, i) => (
    <div key={i} className={`jb-decrow ${d.cq ? 'cq' : ''} ${d.me ? 'me' : ''} ${sel === d.msg + d.hz ? 'sel' : ''}`} onClick={() => d.cq && call(d)}>
      <span>{d.t}</span><span className="db">{d.db}</span><span>{d.dt}</span><span>{d.hz}</span>
      <span className="msg">{d.msg}{d.need && <span className="nd"> ●</span>}</span>
    </div>
  );

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label="J-Bridge · WSJT-X">
      <div className="sx-shell">
        <SuiteDock active="bridge" />
        <div className="sx-modwin" style={{ '--h': 'var(--bridge)' }}>
          <div className="sx-top">
            <div className="sx-brand" style={{ '--h': 'var(--bridge)' }}><div className="ic">{g('bridge', 18)}</div><div><div className="nm">J-Bridge</div><div className="ct">WSJT-X bridge · FT8</div></div></div>
            <div className="sx-stat"><div className="k">Dial</div><div className="v accent">14.074</div></div>
            <div className="sx-stat"><div className="k">Mode</div><div className="v">FT8</div></div>
            <div className="sx-stat"><div className="k">Decodes</div><div className="v">{JB_DECODES.length}</div></div>
            <div className="sx-stat"><div className="k">WSJT-X</div><div className="v" style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 6 }}><span className="ars-dot on"></span>linked</div></div>
            <div className="sx-top-sp"></div>
            <div className="sx-clock"><div className="k">UTC</div><div className="v ars-mono">{utc}</div></div>
          </div>

          <div className="sx-body jb-body">
            <div className="jb-main">
              {/* waterfall */}
              <div className="jb-wf">
                <div className="jb-wf-top"><span className="t">Waterfall</span><span className="meta">200–2800 Hz · 14.074 MHz</span></div>
                <div className="jb-wf-box"><SuiteWaterfall mode="ft8" height={120} signals={wfSignals} /></div>
                <div className="jb-wf-axis"><span>200</span><span>700</span><span>1200</span><span>1700</span><span>2200</span><span>2800 Hz</span></div>
              </div>

              {/* sequence */}
              <div className="jb-seq">
                <div className="jb-period">
                  <div className="jb-period-bar"><i style={{ width: `${periodPct}%` }}></i></div>
                  <div className="jb-period-lbl"><span>15 s FT8 period · <b>{slotEven ? 'EVEN' : 'ODD'}</b> slot</span><span><b>{(15 - inPeriod).toFixed(0)}s</b> to next</span></div>
                </div>
                <div className="jb-seqctl">
                  <div className="jb-evenodd">{['Even', 'Odd'].map(x => <button key={x} className={evenOdd === x ? 'on' : ''} onClick={() => setEvenOdd(x)}>{x}</button>)}</div>
                  <div className={`jb-txtog ${enableTx ? 'on' : ''}`} onClick={() => setEnableTx(v => !v)}><span className="jb-sw"></span>Enable Tx</div>
                </div>
              </div>

              {/* decode tables */}
              <div className="jb-grid2">
                <div className="jb-panel">
                  <div className="jb-ph">Band Activity <span className="n">UTC · dB · DT · Hz</span></div>
                  <div className="jb-dec ars-scroll">{JB_DECODES.map(Row)}</div>
                </div>
                <div className="jb-panel">
                  <div className="jb-ph">Rx Frequency <span className="n">{partner ? partner.call : '—'}</span></div>
                  <div className="jb-dec ars-scroll">{JB_RXFREQ.map(Row)}</div>
                </div>
              </div>

              {/* tx control */}
              <div className="jb-tx">
                <div className="jb-tx-top">
                  <div>
                    <div className="jb-tx-call">{partner ? partner.call : 'No QSO'}</div>
                    <div className="jb-tx-state">{partner ? `Calling ${partner.call} ${partner.grid}` : 'Double-click a CQ decode to call'}</div>
                  </div>
                  <span className={`jb-tx-badge ${txActive ? 'tx' : 'rx'}`}>{txActive ? '● Transmitting' : 'Receiving'}</span>
                </div>
                <div className="jb-tx-msgs">
                  {txMsgs.map(([fk, tt], i) => (
                    <div key={fk} className={`jb-tx-msg ${i === 0 && enableTx ? 'next' : ''}`}><span className="fk">{fk}</span><span className="tt">{tt}</span></div>
                  ))}
                </div>
              </div>
            </div>

            {/* right rail */}
            <div className="sx-rail ars-scroll">
              <SuiteDrawer title="WSJT-X link" hue="bridge" glyph={g('bridge')} defaultOpen summary="linked">
                <div className="sx-kv"><span className="k">Status</span><span className="v" style={{ color: 'var(--ok)' }}>● Connected</span></div>
                <div className="sx-kv"><span className="k">Version</span><span className="v">2.7.0</span></div>
                <div className="sx-kv"><span className="k">UDP port</span><span className="v">2237</span></div>
                <div className="sx-kv"><span className="k">Rig (CAT)</span><span className="v">IC-7610</span></div>
                <div className="sx-kv"><span className="k">Auto-log QSO</span><span className="v" style={{ color: 'var(--ok)' }}>on → J-Log</span></div>
              </SuiteDrawer>
              <SuiteInstruments defaultRotor={50} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

window.JBridge = JBridge;
