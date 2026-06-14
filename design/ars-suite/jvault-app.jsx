/* J-Vault — station inventory + estate planning. Inventory table with value
   & disposition, item detail, and an estate-planning documents drawer. */

function JVault({ theme = 'dark' }) {
  const [sel, setSel] = React.useState(1);
  const [cat, setCat] = React.useState('All');
  const g = (id, size = 14) => <ARSGlyph id={id} size={size} stroke={1.9} />;
  const cats = ['All', ...Array.from(new Set(JVAULT_ITEMS.map(i => i.cat)))];
  const items = JVAULT_ITEMS.filter(i => cat === 'All' || i.cat === cat);
  const item = JVAULT_ITEMS.find(i => i.id === sel) || JVAULT_ITEMS[0];
  const total = JVAULT_ITEMS.reduce((s, i) => s + i.val, 0);
  const insured = JVAULT_ITEMS.filter(i => i.val >= 500).reduce((s, i) => s + i.val, 0);
  const assigned = JVAULT_ITEMS.filter(i => JVAULT_DISP[i.disp] !== 'sell').length;
  const fmt = (n) => '$' + n.toLocaleString();

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label="J-Vault · Inventory & estate">
      <div className="sx-shell">
        <SuiteDock active="vault" />
        <div className="sx-modwin" style={{ '--h': 'var(--vault)' }}>
          <div className="sx-top">
            <div className="sx-brand" style={{ '--h': 'var(--vault)' }}><div className="ic">{g('vault', 18)}</div><div><div className="nm">J-Vault</div><div className="ct">Station inventory & estate planning</div></div></div>
            <div className="sx-stat"><div className="k">Items</div><div className="v">{JVAULT_ITEMS.length}</div></div>
            <div className="sx-stat"><div className="k">Total value</div><div className="v" style={{ color: 'var(--vault)' }}>{fmt(total)}</div></div>
            <div className="sx-stat"><div className="k">Assigned</div><div className="v">{assigned}/{JVAULT_ITEMS.length}</div></div>
            <div className="sx-top-sp"></div>
            <div className="sx-clock"><div className="k">Last backup</div><div className="v ars-mono" style={{ fontSize: 14 }}>2026-06-12</div></div>
          </div>

          <div className="sx-body jv-body">
            <div className="jv-main">
              <div className="jv-sum">
                <div className="jv-sum-c"><div className="jv-sum-k">Total value</div><div className="jv-sum-v">{fmt(total)}</div></div>
                <div className="jv-sum-c"><div className="jv-sum-k">Insured (≥$500)</div><div className="jv-sum-v">{fmt(insured)}</div></div>
                <div className="jv-sum-c"><div className="jv-sum-k">Items</div><div className="jv-sum-v">{JVAULT_ITEMS.length}</div></div>
                <div className="jv-sum-c"><div className="jv-sum-k">Est. assigned</div><div className="jv-sum-v">{assigned}<span className="u"> / {JVAULT_ITEMS.length}</span></div></div>
              </div>

              <div className="jv-tools">
                <div className="jv-search">{g('vault', 13)} Search inventory…</div>
                {cats.map(c => <span key={c} className={`jv-chip ${cat === c ? 'on' : ''}`} onClick={() => setCat(c)}>{c}</span>)}
              </div>

              <div className="jv-table">
                <div className="jv-th"><span>Category</span><span>Item</span><span>Value</span><span>Condition</span><span>Disposition</span></div>
                <div className="jv-rows ars-scroll">
                  {items.map(i => (
                    <div key={i.id} className={`jv-tr ${i.id === sel ? 'sel' : ''}`} onClick={() => setSel(i.id)}>
                      <span className="jv-cat">{i.cat}</span>
                      <span><div className="jv-name">{i.name}</div><div className="jv-sn">{i.sn !== '—' ? 'S/N ' + i.sn : '—'} · {i.yr}</div></span>
                      <span className="jv-val">{fmt(i.val)}</span>
                      <span style={{ color: i.cond === 'excellent' ? 'var(--ok)' : i.cond === 'fair' ? 'var(--warn)' : 'var(--t2)', fontSize: 12 }}>{i.cond}</span>
                      <span className="jv-disp"><span className={`d ${JVAULT_DISP[i.disp]}`}></span>{i.disp}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* right rail */}
            <div className="sx-rail ars-scroll">
              <SuiteDrawer title="Item detail" hue="vault" glyph={g('vault')} defaultOpen summary={item.name.split(' ')[0]}>
                <div style={{ marginTop: 4 }}>
                  <div className="jv-det-name">{item.name}</div>
                  <div className="jv-det-cat">{item.cat} · {item.yr}</div>
                  <div className="jv-det-meta">
                    <div><div className="k">Value</div><div className="v" style={{ color: 'var(--vault)' }}>{fmt(item.val)}</div></div>
                    <div><div className="k">Condition</div><div className="v">{item.cond}</div></div>
                    <div><div className="k">Serial</div><div className="v">{item.sn}</div></div>
                    <div><div className="k">Year</div><div className="v">{item.yr}</div></div>
                  </div>
                  <div className="jv-det-note">{item.note}</div>
                  <div className="jv-disp-box">
                    <div className="lab">Estate disposition</div>
                    <div className="who">{item.disp}</div>
                  </div>
                </div>
              </SuiteDrawer>

              <SuiteDrawer title="Estate planning" hue="vault" glyph={g('learn')} defaultOpen summary={`${JVAULT_DOCS.length} docs`}>
                {JVAULT_DOCS.map((d, i) => (
                  <div key={i} className="jv-doc">
                    <span className="jv-doc-ic"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9"><path d="M6 2h9l5 5v15H6z"/><path d="M15 2v5h5"/></svg></span>
                    <div style={{ flex: 1, minWidth: 0 }}><div className="jv-doc-n">{d.name}</div><div className="jv-doc-t">{d.tag}</div></div>
                    <span className="jv-doc-u">{d.updated}</span>
                  </div>
                ))}
              </SuiteDrawer>

              <SuiteDrawer title="Disposition summary" hue="sat" glyph={g('sat')} summary={`${assigned} assigned`}>
                <div className="sx-kv"><span className="k"><span className="jv-disp"><span className="d family"></span></span> Family</span><span className="v">{JVAULT_ITEMS.filter(i => JVAULT_DISP[i.disp] === 'family').length} items</span></div>
                <div className="sx-kv"><span className="k"><span className="jv-disp"><span className="d club"></span></span> Club</span><span className="v">{JVAULT_ITEMS.filter(i => JVAULT_DISP[i.disp] === 'club').length} items</span></div>
                <div className="sx-kv"><span className="k"><span className="jv-disp"><span className="d gift"></span></span> Gift</span><span className="v">{JVAULT_ITEMS.filter(i => JVAULT_DISP[i.disp] === 'gift').length} items</span></div>
                <div className="sx-kv"><span className="k"><span className="jv-disp"><span className="d sell"></span></span> Sell / estate</span><span className="v">{JVAULT_ITEMS.filter(i => JVAULT_DISP[i.disp] === 'sell').length} items</span></div>
              </SuiteDrawer>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

window.JVault = JVault;
