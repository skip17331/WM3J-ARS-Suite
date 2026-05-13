// J-Learn standalone web app.
//
// Runs unmodified in two contexts:
//   1. Direct browser visit to http://localhost:8082/  — fully standalone.
//   2. Inside a J-Hub iframe — parent posts {type:'jlearn-open', id:'NN-NN'}
//      to navigate; cross-module banner buttons postMessage back to parent.

const state = {
  manifest: [],
  byId: {},
  currentId: null,
};

const inIframe = window.parent && window.parent !== window;

// ---------- text size ---------------------------------------------------

function applyJLearnTextSize(pct) {
  let v = parseInt(pct, 10);
  if (!isFinite(v) || v < 80 || v > 180) v = 100;
  // Scale both the reading area and the sidebar TOC. Topbar (chrome) is
  // intentionally excluded so the slider/dropdown stay a stable size.
  const viewer  = document.getElementById('jl-viewer');
  const sidebar = document.querySelector('.sidebar');
  if (viewer)  viewer.style.fontSize  = v + '%';
  if (sidebar) sidebar.style.fontSize = v + '%';
  document.getElementById('jl-text-size').value = v;
  document.getElementById('jl-text-size-val').textContent = v;
  try { localStorage.setItem('jhub.jlearn.textSize', String(v)); } catch (e) {}
}

function resetJLearnTextSize() { applyJLearnTextSize(100); }

function applyJLearnTextSizePref() {
  let v = 100;
  try { v = parseInt(localStorage.getItem('jhub.jlearn.textSize') || '100', 10); }
  catch (e) {}
  applyJLearnTextSize(v);
}

// ---------- theme ------------------------------------------------------

function applyJLearnTheme(name) {
  const theme = (name === 'latte') ? 'latte' : 'mocha';
  document.documentElement.setAttribute('data-theme', theme);
  const sel = document.getElementById('jl-theme');
  if (sel) sel.value = theme;
  try { localStorage.setItem('jhub.jlearn.theme', theme); } catch (e) {}
}

function applyJLearnThemePref() {
  let theme = 'mocha';
  try { theme = localStorage.getItem('jhub.jlearn.theme') || 'mocha'; }
  catch (e) {}
  applyJLearnTheme(theme);
}

function resetJLearnPrefs() {
  applyJLearnTextSize(100);
  applyJLearnTheme('mocha');
}

function reportJLearnIssue() {
  const sectionId = (state && state.current) ? state.current : '(none)';
  const title = '[j-learn v1.0.0] <one-line summary>';
  const body =
`### Module
- Name: j-learn
- Version: 1.0.0
- Current section: ${sectionId}
- Browser: ${navigator.userAgent}

### What I expected to happen


### What actually happened


### Steps to reproduce
1.
2.
3.
`;
  const url = 'https://github.com/skip17331/WM3J-ARS-Suite/issues/new'
    + '?labels=bug'
    + '&title=' + encodeURIComponent(title)
    + '&body='  + encodeURIComponent(body);
  window.open(url, '_blank', 'noopener');
}

// ---------- manifest + sidebar ------------------------------------------

function loadLearn() {
  fetch('/api/jlearn/manifest').then(r => r.json()).then(list => {
    state.manifest = list || [];
    state.byId = {};
    for (const e of state.manifest) state.byId[e.id] = e;
    renderLearnToc();

    // Resolve initial section: ?section= URL > postMessage from parent > localStorage > nothing.
    const fromUrl = new URLSearchParams(location.search).get('section');
    if (fromUrl && state.byId[fromUrl]) { openLearnSection(fromUrl); return; }
    const last = localStorage.getItem('jl-last');
    if (last && state.byId[last]) openLearnSection(last);
  }).catch(err => {
    document.getElementById('jl-viewer').innerHTML =
      '<div style="color:var(--red);padding:24px">Failed to load manifest: ' + esc(err.message) + '</div>';
  });
}

function renderLearnToc() {
  const wrap = document.getElementById('jl-toc');
  if (!wrap) return;
  const filter = (document.getElementById('jl-search')?.value || '').toLowerCase();

  const visible = state.manifest.filter(e => {
    if (!filter) return true;
    return e.title.toLowerCase().includes(filter) || e.id.includes(filter);
  });

  const byChapter = {};
  for (const e of visible) {
    (byChapter[e.chapter] = byChapter[e.chapter] || []).push(e);
  }
  const chapters = Object.keys(byChapter).sort();
  wrap.innerHTML = chapters.map(ch => {
    const overview = byChapter[ch].find(e => e.section === '00');
    const sections = byChapter[ch].filter(e => e.section !== '00');
    const chapterTitle = overview ? overview.title.replace(/ — Overview$/, '') : 'Chapter ' + ch;
    const head = `<div class="chapter-title" data-id="${overview ? overview.id : (sections[0] && sections[0].id) || ''}">${ch} · ${esc(chapterTitle)}</div>`;
    const items = sections.map(s =>
      `<div class="section-link" data-id="${s.id}">${s.section} · ${esc(s.title)}` +
      (s.level === 'advanced' ? ' <span class="adv-marker">⚙️</span>' : '') +
      '</div>'
    ).join('');
    return `<div class="chapter">${head}${items}</div>`;
  }).join('');

  wrap.querySelectorAll('[data-id]').forEach(el => {
    el.addEventListener('click', () => openLearnSection(el.dataset.id));
  });
}

function filterLearnToc() { renderLearnToc(); }

// ---------- content load + render ---------------------------------------

function openLearnSection(id) {
  if (!id) return;
  state.currentId = id;
  try { localStorage.setItem('jl-last', id); } catch (e) {}
  fetch('/api/jlearn/content?id=' + encodeURIComponent(id))
    .then(r => r.text())
    .then(md => renderLearnContent(md))
    .catch(err => {
      document.getElementById('jl-viewer').innerHTML =
        '<div style="color:var(--red)">Failed to load section: ' + esc(err.message) + '</div>';
    });
}

function renderLearnContent(md) {
  if (md == null) {
    if (state.currentId) openLearnSection(state.currentId);
    return;
  }
  const banner = renderLearnBanner(state.currentId);
  document.getElementById('jl-viewer').innerHTML = banner + mdToHtml(stripFrontMatter(md));
  document.getElementById('jl-viewer').scrollTop = 0;
}

function renderLearnBanner(id) {
  if (!id) return '';
  if (id.startsWith('05-')) {
    return banner('🎧', 'Morse Code Trainer',
      'Standalone JavaFX practice app: letter / group / QSO drills, real-time decoder, optional Arduino or Pi Zero keyer.',
      '▶ Launch Trainer',
      'launch-morse');
  }
  if (id.startsWith('17-')) {
    const calcId = ({
      '17-01': 'ohms-law',     '17-02': 'power-law',     '17-03': 'reactance',
      '17-04': 'impedance',    '17-05': 'resonance',     '17-06': 'wavelength',
      '17-07': 'swr',          '17-08': 'erp',           '17-09': 'feedline-loss',
      '17-10': 'decibels',     '17-11': 'q-factor',      '17-12': 'bandwidth',
      '17-13': 'smith-chart',  '17-14': 'rf-exposure',
    })[id];
    return banner('📐', 'Formula Calculator',
      calcId
        ? "Run this formula's calculator with live inputs and outputs in the J-Hub Antenna Workshop tab."
        : "Pick a formula calculator from the Workshop's Formulas section.",
      calcId ? '▶ Open in Workshop' : '▶ Open Workshop',
      calcId ? ('open-calc:' + calcId) : 'open-workshop');
  }
  if (id.startsWith('09-')) {
    const calcId = ({
      '09-02': 'flat-dipole',     '09-03': 'inverted-v',     '09-04': 'fan-dipole',
      '09-05': 'trapped-dipole',  '09-06': 'ocf-dipole',     '09-07': 'efhw-no-traps',
      '09-08': 'efhw-trapped',    '09-09': 'j-pole',         '09-10': 'yagi',
      '09-11': 'vertical',        '09-12': 'loading-coil',   '09-13': 'trap-design',
      '09-14': 'mag-loop',
    })[id];
    return banner('📡', 'Antenna Workshop',
      calcId
        ? "Run this antenna's calculator with live inputs and outputs in the J-Hub Antenna Workshop tab."
        : 'Pick an antenna or component calculator, or run the recommender wizard to find what fits your QTH.',
      calcId ? '▶ Open in Workshop' : '▶ Open Workshop',
      calcId ? ('open-calc:' + calcId) : 'open-workshop');
  }
  return '';
}

function banner(icon, title, sub, btnText, action) {
  const id = 'b' + Math.random().toString(36).slice(2, 8);
  setTimeout(() => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('click', () => bannerAction(action));
  }, 0);
  return `<div class="cross-banner">
    <span class="ico">${icon}</span>
    <div class="body">
      <div class="title">${esc(title)}</div>
      <div class="sub">${esc(sub)}</div>
    </div>
    <button id="${id}">${esc(btnText)}</button>
  </div>`;
}

// Cross-module action dispatch.
//
// Inside the J-Hub iframe → postMessage to parent.
// Standalone (direct browser) → fall back to opening the j-hub URL,
//   which only works if the user is also running j-hub. We hint that.
function bannerAction(action) {
  if (inIframe) {
    window.parent.postMessage({ type: 'jlearn-action', action }, '*');
    return;
  }
  // Standalone fallback.
  if (action === 'launch-morse') {
    // /api/morsetrainer/launch is POST-only — window.open issues a GET and
    // gets a 405. Use fetch + POST and surface the result; the morse-trainer
    // is a desktop JavaFX app so there's nothing useful to open in a new tab
    // anyway.
    fetch('http://localhost:8081/api/morsetrainer/launch', { method: 'POST' })
      .then(r => r.json().catch(() => ({})).then(body => ({ ok: r.ok, body })))
      .then(({ ok, body }) => {
        if (ok) return;
        const msg = (body && body.error) ? body.error : 'launch failed';
        alert('Could not launch Morse Trainer: ' + msg
              + '\n\nIs J-Hub running on port 8081?');
      })
      .catch(() => {
        alert('Could not reach J-Hub on port 8081.'
              + '\n\nMorse Trainer is launched by J-Hub — start J-Hub first.');
      });
    return;
  }
  if (action === 'open-workshop' || action.startsWith('open-calc:')) {
    const calcId = action.startsWith('open-calc:') ? action.substring(10) : '';
    const url = 'http://localhost:8081/#antworkshop' + (calcId ? '?calc=' + encodeURIComponent(calcId) : '');
    window.open(url, '_blank');
  }
}

// Parent-driven navigation when embedded as an iframe inside J-Hub.
window.addEventListener('message', ev => {
  const m = ev.data || {};
  if (m.type === 'jlearn-open' && m.id) openLearnSection(m.id);
});

// ---------- markdown rendering ------------------------------------------

function stripFrontMatter(md) {
  if (!md.startsWith('---')) return md;
  const end = md.indexOf('\n---', 3);
  if (end < 0) return md;
  return md.substring(end + 4).replace(/^\s*\n/, '');
}

function esc(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// Split a table row on unescaped pipes. `\|` inside a cell is preserved
// (used in content like `\|Γ\|` for absolute-value notation).
function parseTableRow(line) {
  const trimmed = line.trim().replace(/^\|/, '').replace(/\|$/, '');
  return trimmed
    .replace(/\\\|/g, '\x00')
    .split('|')
    .map(c => c.replace(/\x00/g, '|').trim());
}

// GFM-style alignment from the `---` separator row: `:---` left, `---:` right,
// `:---:` center. Returns '' for cells with no explicit alignment.
function parseTableAlign(sep, count) {
  const cells = sep.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(s => s.trim());
  const out = [];
  for (let k = 0; k < count; k++) {
    const c = cells[k] || '';
    const left = c.startsWith(':'), right = c.endsWith(':');
    out.push(left && right ? 'center' : right ? 'right' : left ? 'left' : '');
  }
  return out;
}

function mdToHtml(md) {
  const lines = md.split(/\r?\n/);
  const out = [];
  let i = 0;
  const inline = s => esc(s)
    .replace(/`([^`]+)`/g,                '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g,          '<strong>$1</strong>')
    .replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '<em>$1</em>')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g,  '<a href="$2" target="_blank">$1</a>');

  while (i < lines.length) {
    const line = lines[i];
    if (line.startsWith('```')) {
      const buf = [];
      i++;
      while (i < lines.length && !lines[i].startsWith('```')) { buf.push(esc(lines[i])); i++; }
      i++;
      out.push('<pre><code>' + buf.join('\n') + '</code></pre>');
      continue;
    }
    let m;
    if ((m = line.match(/^(#{1,4})\s+(.*)$/))) {
      const level = m[1].length;
      out.push('<h' + level + '>' + inline(m[2]) + '</h' + level + '>');
      i++; continue;
    }
    if (line.startsWith('>')) {
      const buf = [];
      while (i < lines.length && lines[i].startsWith('>')) {
        buf.push(lines[i].replace(/^>\s?/, ''));
        i++;
      }
      const text = buf.join(' ');
      // The "Advanced — …" callout still gets the .advanced class so it
      // renders distinctly (peach accent), but it is no longer hidden —
      // readers can skip or skim it as they choose.
      const isAdvanced = /^⚙️\s+\*\*Advanced\s*—/.test(text.trim());
      out.push('<blockquote' + (isAdvanced ? ' class="advanced"' : '') + '>' + inline(text) + '</blockquote>');
      continue;
    }
    if (/^\s*[-*]\s+/.test(line)) {
      const items = [];
      while (i < lines.length && /^\s*[-*]\s+/.test(lines[i])) {
        items.push('<li>' + inline(lines[i].replace(/^\s*[-*]\s+/, '')) + '</li>');
        i++;
      }
      out.push('<ul>' + items.join('') + '</ul>');
      continue;
    }
    // GFM table: header row (starts with |), then separator row of dashes/colons.
    if (line.trim().startsWith('|') && i + 1 < lines.length
        && /^\s*\|?[\s\-:|]+\|?\s*$/.test(lines[i + 1])
        && /-{3,}/.test(lines[i + 1])) {
      const headers = parseTableRow(line);
      const aligns  = parseTableAlign(lines[i + 1], headers.length);
      i += 2;
      const rows = [];
      while (i < lines.length && lines[i].trim().startsWith('|')) {
        rows.push(parseTableRow(lines[i]));
        i++;
      }
      const styleFor = idx => aligns[idx] ? ' style="text-align:' + aligns[idx] + '"' : '';
      const thead = '<thead><tr>' + headers.map((h, idx) =>
        '<th' + styleFor(idx) + '>' + inline(h) + '</th>').join('') + '</tr></thead>';
      const tbody = '<tbody>' + rows.map(r =>
        '<tr>' + r.map((c, idx) =>
          '<td' + styleFor(idx) + '>' + inline(c) + '</td>').join('') + '</tr>').join('') + '</tbody>';
      out.push('<table>' + thead + tbody + '</table>');
      continue;
    }
    if (line.trim() === '') { i++; continue; }
    const para = [];
    while (i < lines.length && lines[i].trim() !== ''
        && !lines[i].startsWith('#')
        && !lines[i].startsWith('>')
        && !lines[i].startsWith('```')
        && !/^\s*[-*]\s+/.test(lines[i])
        && !lines[i].trim().startsWith('|')) {
      para.push(lines[i]);
      i++;
    }
    if (para.length) {
      let html = inline(para.join(' '));
      html = html.replace(/&lt;!--\s*TODO:?\s*content\s*--&gt;/g,
        '<span style="font-style:italic;color:var(--overlay0);font-size:12px">(content not yet written)</span>');
      out.push('<p>' + html + '</p>');
    }
  }
  return out.join('\n');
}

// ---------- bootstrap ---------------------------------------------------

document.addEventListener('DOMContentLoaded', () => {
  applyJLearnTextSizePref();
  applyJLearnThemePref();
  loadLearn();
});
