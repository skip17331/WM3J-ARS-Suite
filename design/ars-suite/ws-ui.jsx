/* J-Hub Workspace — UI pieces: field controls, ConfigPage renderer, Dashboard.
   Exported to window. */

function WSField({ f }) {
  const [val, setVal] = React.useState(f.value);
  const ctl = () => {
    switch (f.type) {
      case 'text':
      case 'addr':
        return <input className={`ws-input ${f.mono || f.type === 'addr' ? 'mono' : ''} ${f.type === 'addr' ? 'short' : ''}`} value={val} onChange={e => setVal(e.target.value)} />;
      case 'select':
        return (
          <select className="ws-select" value={val} onChange={e => setVal(e.target.value)}>
            {f.options.map(o => <option key={o} value={o}>{o}</option>)}
          </select>
        );
      case 'segmented':
        return (
          <div className="ws-seg">
            {f.options.map(o => <button key={o} className={val === o ? 'on' : ''} onClick={() => setVal(o)}>{o}</button>)}
          </div>
        );
      case 'toggle':
        return <div className={`ws-switch ${val ? 'on' : ''}`} onClick={() => setVal(v => !v)}><i></i></div>;
      case 'slider':
        return (
          <div className="ws-slider">
            <input className="ws-range" type="range" min={f.min} max={f.max} value={val} onChange={e => setVal(+e.target.value)} />
            <span className="ws-sval">{val}<span style={{ color: 'var(--t3)', fontWeight: 400, marginLeft: 2 }}>{f.unit}</span></span>
          </div>
        );
      default:
        return <span className="ars-mono" style={{ color: 'var(--t2)' }}>{val}</span>;
    }
  };
  return (
    <div className="ws-field">
      <div className="ws-flabel">
        <div className="t">{f.label}</div>
        {f.hint && <div className="h">{f.hint}</div>}
      </div>
      <div className="ws-fctl">{ctl()}</div>
    </div>
  );
}

function WSConfigPage({ page }) {
  return (
    <div className="ws-page">
      {page.intro && <div className="ws-pintro">{page.intro}</div>}

      {page.live && (
        <div className="ws-live">
          {page.live.map(([k, v, st], i) => (
            <div key={i} className="ws-live-cell">
              <div className="ws-live-k">{k}</div>
              <div className="ws-live-v ars-mono" style={{ color: st === 'ok' ? 'var(--ok)' : 'var(--t1)', display: 'flex', alignItems: 'center', gap: 7 }}>
                {st === 'ok' && <span className="ars-dot on"></span>}{v}
              </div>
            </div>
          ))}
        </div>
      )}

      {page.stub ? (
        <div className="ws-stub">
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M5 19l2-2M17 7l2-2"/></svg>
          <div style={{ fontSize: 13, color: 'var(--t2)', maxWidth: 360 }}>{page.intro}</div>
        </div>
      ) : (
        page.sections.map((sec, i) => (
          <div key={i} className="ws-section">
            <div className="ws-sectitle">{sec.title}</div>
            <div className="ws-fields">
              {sec.fields.map((f, j) => <WSField key={j} f={f} />)}
            </div>
          </div>
        ))
      )}

      {!page.stub && (
        <div style={{ display: 'flex', gap: 10, marginTop: 6 }}>
          <button className="ars-btn primary">Apply changes</button>
          <button className="ars-btn ghost">Revert</button>
        </div>
      )}
    </div>
  );
}

Object.assign(window, { WSField, WSConfigPage });
