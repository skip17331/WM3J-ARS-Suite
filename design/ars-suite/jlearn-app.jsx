/* J-Learn — reference / learning library. Category browser, study-deck grid
   with progress, and a reference reader pane on the shared suite shell. */

function JLearn({ theme = 'dark' }) {
  const [cat, setCat] = React.useState('all');
  const g = (id, size = 14) => <ARSGlyph id={id} size={size} stroke={1.9} />;
  const decks = JLEARN_DECKS.filter(d => cat === 'all' || d.cat === cat);
  const A = JLEARN_ARTICLE;
  const totalDue = JLEARN_DECKS.reduce((s, d) => s + d.due, 0);
  const totalCards = JLEARN_DECKS.reduce((s, d) => s + d.cards, 0);
  const doneCards = JLEARN_DECKS.reduce((s, d) => s + d.done, 0);
  const catName = cat === 'all' ? 'All topics' : JLEARN_CATS.find(c => c.id === cat).name;

  return (
    <div className={`ars-root ${theme === 'light' ? 'ars-light' : ''}`} data-screen-label="J-Learn · Reference library">
      <div className="sx-shell">
        <SuiteDock active="learn" />
        <div className="sx-modwin" style={{ '--h': 'var(--learn)' }}>
          <div className="sx-top">
            <div className="sx-brand" style={{ '--h': 'var(--learn)' }}><div className="ic">{g('learn', 18)}</div><div><div className="nm">J-Learn</div><div className="ct">Reference & learning library</div></div></div>
            <div className="sx-stat"><div className="k">Decks</div><div className="v">{JLEARN_DECKS.length}</div></div>
            <div className="sx-stat"><div className="k">Due today</div><div className="v" style={{ color: 'var(--learn)' }}>{totalDue}</div></div>
            <div className="sx-stat"><div className="k">Mastered</div><div className="v">{Math.round(doneCards / totalCards * 100)}%</div></div>
            <div className="sx-top-sp"></div>
            <div className="sx-clock"><div className="k">Streak</div><div className="v ars-mono" style={{ fontSize: 15 }}>12 days</div></div>
          </div>

          <div className="sx-body jl2-body">
            {/* LEFT — categories */}
            <div className="jl2-cats ars-scroll">
              <div className="jl2-cats-h">Topics</div>
              <div className={`jl2-cat ${cat === 'all' ? 'on' : ''}`} onClick={() => setCat('all')}><span className="nm">All topics</span><span className="ct">{JLEARN_DECKS.length}</span></div>
              {JLEARN_CATS.map(c => (
                <div key={c.id} className={`jl2-cat ${cat === c.id ? 'on' : ''}`} onClick={() => setCat(c.id)}><span className="nm">{c.name}</span><span className="ct">{c.n}</span></div>
              ))}
            </div>

            {/* CENTER — deck grid */}
            <div className="jl2-main">
              <div className="jl2-h"><span className="t">{catName}</span><span className="meta">· {decks.length} decks · {decks.reduce((s, d) => s + d.due, 0)} cards due</span></div>
              <div className="jl2-decks ars-scroll">
                {decks.map(d => {
                  const pct = Math.round(d.done / d.cards * 100);
                  return (
                    <div key={d.id} className="jl2-deck">
                      <div className="jl2-deck-top">
                        <div className="jl2-deck-ic">{g(d.kind === 'Reference' ? 'log' : d.kind === 'Guide' ? 'map' : 'learn', 18)}</div>
                        <span className="jl2-deck-kind">{d.kind}</span>
                      </div>
                      <div className="jl2-deck-title">{d.title}</div>
                      <div className="jl2-deck-sub">{d.sub}</div>
                      <div className="jl2-prog">
                        <div className="jl2-prog-bar"><i style={{ width: `${pct}%` }}></i></div>
                        <div className="jl2-prog-lbl"><span><b>{d.done}</b>/{d.cards} cards · {pct}%</span><span>{d.due > 0 ? <span className="jl2-due">{d.due} due</span> : 'up to date'}</span></div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* RIGHT — reader + reference */}
            <div className="sx-rail ars-scroll">
              <SuiteDrawer title="Reference reader" hue="learn" glyph={g('learn')} defaultOpen summary={`${A.read} min`}>
                <div style={{ marginTop: 4 }}>
                  <div className="jl2-art-h">{A.cat}</div>
                  <div className="jl2-art-title">{A.title}</div>
                  <div className="jl2-art-meta">{A.read} min read · saved offline</div>
                  <div className="jl2-art-body">
                    {A.body.map((b, i) => {
                      if (b[0] === 'p') return <p key={i}>{b[1]}</p>;
                      if (b[0] === 'h') return <h4 key={i}>{b[1]}</h4>;
                      return <div key={i} className="jl2-qrow"><span className="q">{b[1]}</span><span className="d">{b[2]}</span></div>;
                    })}
                  </div>
                </div>
              </SuiteDrawer>

              <SuiteDrawer title="Quick reference" hue="map" glyph={g('map')} defaultOpen summary={`${JLEARN_REF.length} cards`}>
                <div className="jl2-reflist">
                  {JLEARN_REF.map((r, i) => (
                    <div key={i} className="sx-kv"><span className="k">{r.t}<div style={{ fontSize: 10.5, color: 'var(--t4)', marginTop: 1 }}>{r.tag}</div></span><span className="v" style={{ color: 'var(--t4)' }}>›</span></div>
                  ))}
                </div>
              </SuiteDrawer>

              <SuiteDrawer title="Exam prep" hue="vault" glyph={g('vault')} summary="Extra · 82%">
                <div className="sx-kv"><span className="k">Target class</span><span className="v">Amateur Extra</span></div>
                <div className="sx-kv"><span className="k">Practice score</span><span className="v" style={{ color: 'var(--ok)' }}>82% avg</span></div>
                <div className="sx-kv"><span className="k">Weak area</span><span className="v">E3 propagation</span></div>
                <div className="sx-kv"><span className="k">Exam session</span><span className="v">Jun 28</span></div>
              </SuiteDrawer>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

window.JLearn = JLearn;
