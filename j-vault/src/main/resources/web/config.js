// J-Vault — standalone inventory + estate handoff app.
// Lifted from j-hub config.js (inventory + estate sections only).

// ===== Local station info (stored in browser localStorage) =====
function getStation() {
  return {
    name:     localStorage.getItem('jvault.station.name')     || '',
    callsign: localStorage.getItem('jvault.station.callsign') || '',
  };
}
function saveStation() {
  localStorage.setItem('jvault.station.name',
    (document.getElementById('station-name').value || '').trim());
  localStorage.setItem('jvault.station.callsign',
    (document.getElementById('station-callsign').value || '').trim());
}
function loadStation() {
  const s = getStation();
  document.getElementById('station-name').value = s.name;
  document.getElementById('station-callsign').value = s.callsign;
}

// ===== Application state =====
const state = {};


state.inventory = { items: [], types: [], contacts: [] };



const INV_TYPE_HINTS = {
  'radio':       "For radios: Manufacturer/Model are obvious; Serial # is critical for warranty/recall lookup. Capture firmware version in Notes if you track that.",
  'transceiver': "For radios: Manufacturer/Model are obvious; Serial # is critical for warranty/recall lookup. Capture firmware version in Notes if you track that.",
  'amplifier':   "For amplifiers: record tube/transistor type and any matching tuners or power supplies in Notes. Estimated value is volatile — vintage tube amps especially.",
  'antenna':     "For antennas: Model = the design (e.g. 'Hex Beam' or 'EFHW 80-10'); record band coverage and feedline length in Notes. Photograph it during a spring inspection.",
  'tuner':       "For tuners: note manual vs auto, max power, and which radio it's paired with. Internal tuners in modern rigs don't need their own entry.",
  'coax run':    "For coax runs: Model = type + length, e.g. 'LMR-400 / 75 ft'. Date Acquired = installation date (sets the 7–15 yr replacement clock — see §16-04).",
  'coax':        "For coax runs: Model = type + length, e.g. 'LMR-400 / 75 ft'. Date Acquired = installation date (sets the 7–15 yr replacement clock — see §16-04).",
  'feedline':    "For coax runs: Model = type + length, e.g. 'LMR-400 / 75 ft'. Date Acquired = installation date (sets the 7–15 yr replacement clock — see §16-04).",
  'accessory':   "For accessories (mics, keys, headphones, meters): Manufacturer/Model + a note about which radio they're typically used with.",
  'computer':    "For computers / SDRs: capture OS, key software (WSJT-X version, JS8Call version) in Notes — useful when reproducing setups.",
  'tower':       "For towers: model and section count (Rohn 25G ×6, etc.); Notes should include guy material, anchor type, install date. See §16-05.",
  'rotator':     "For rotators: capture controller model separately if cabled. Note maintenance history (last lube date) in Notes.",
  'handheld':    "For HTs: include the battery type and any spare batteries / chargers in Notes. Serial # is on the battery contact area or under the battery.",
  'receiver':    "For receivers: serial # location varies by manufacturer. Note any specialty mods (filters, IF taps) in Notes.",
  'battery':     "For batteries: Notes should include AH rating, install date, and last capacity test (see §16-01). Replace at ~80% of original capacity.",
  'balun':       "For baluns / ununs: ratio (1:1, 4:1, 9:1) and power rating in Notes; identify which antenna it serves.",
  'antenna switch': "For antenna switches: number of ports and which antennas are on which ports in Notes; record any rotator or sequencer bridging.",
  'power supply':"For power supplies: max current, rig(s) it powers, and whether it's 1-row or rack-mounted (helps the estate handoff describe what to look for).",
  'tool':        "For tools (NanoVNA, antenna analyzer, soldering iron): brand+model usually enough. Date Acquired matters for warranty.",
  'test equipment': "For test gear: include calibration date or last self-cal in Notes. Some test gear loses value rapidly when it goes out of cal.",
  'book':        "For books / reference: edition / year important. ARRL Handbook editions vary in worth. Notes for autographs / dedications.",
};

function inventoryTypeHint(typeName) {
  if (!typeName) return null;
  const key = String(typeName).toLowerCase().trim();
  if (INV_TYPE_HINTS[key]) return INV_TYPE_HINTS[key];
  // partial match — try longer keys first so "antenna switch" beats "antenna".
  const sortedKeys = Object.keys(INV_TYPE_HINTS).sort((a, b) => b.length - a.length);
  for (const k of sortedKeys) {
    if (key.startsWith(k) || key.includes(k)) return INV_TYPE_HINTS[k];
  }
  return null;
}

function updateTypeHint() {
  const sel = document.getElementById('inv-item-type');
  const banner = document.getElementById('inv-type-hint');
  const text = document.getElementById('inv-type-hint-text');
  if (!sel || !banner || !text) return;
  const typeId = parseInt(sel.value, 10);
  const t = state.inventory.types.find(x => x.id === typeId);
  const hint = inventoryTypeHint(t && t.name);
  if (hint) {
    text.textContent = ' ' + hint;
    banner.classList.add('active');
  } else {
    banner.classList.remove('active');
  }
}

// Show/hide the Getting-Started panel; persist user's choice.
function toggleInventoryHelp() {
  const body = document.getElementById('inv-help-body');
  const btn  = document.getElementById('inv-help-toggle');
  if (!body || !btn) return;
  const hidden = body.style.display === 'none';
  body.style.display = hidden ? '' : 'none';
  btn.textContent = hidden ? 'Hide' : 'Show';
  try { localStorage.setItem('jhub.inv.help.hidden', hidden ? '0' : '1'); } catch (e) {}
}

function applyInventoryHelpPref() {
  let hidden = false;
  try { hidden = localStorage.getItem('jhub.inv.help.hidden') === '1'; } catch (e) {}
  if (hidden) {
    const body = document.getElementById('inv-help-body');
    const btn  = document.getElementById('inv-help-toggle');
    if (body) body.style.display = 'none';
    if (btn)  btn.textContent = 'Show';
  }
}

// Switch to the J-Learn tab and open a specific section.
function openLearnFromInventory(id) { /* no Learn tab in J-Vault standalone */ }

// Inventory completeness pills (shown under the stats line).
function renderInventoryHealth() {
  const el = document.getElementById('inv-health');
  if (!el) return;
  const items = state.inventory.items || [];
  if (items.length === 0) { el.innerHTML = ''; return; }
  const noSerial = items.filter(it => !it.serial_number || !String(it.serial_number).trim()).length;
  const noValue  = items.filter(it => it.estimated_value == null || it.estimated_value === '').length;
  const noDate   = items.filter(it => !it.date_acquired).length;
  const repair   = items.filter(it => it.disposition === 'repairable').length;
  const broken   = items.filter(it => it.disposition === 'not_repairable').length;
  const pills = [];
  pills.push(`<span class="pill">${items.length} item${items.length === 1 ? '' : 's'}</span>`);
  if (noSerial)
    pills.push(`<span class="pill ${noSerial > items.length / 2 ? 'bad' : 'warn'}">${noSerial} missing serial #</span>`);
  if (noValue)
    pills.push(`<span class="pill ${noValue > items.length / 2 ? 'bad' : 'warn'}">${noValue} missing value</span>`);
  if (noDate)
    pills.push(`<span class="pill warn">${noDate} missing date</span>`);
  if (repair)
    pills.push(`<span class="pill warn">${repair} repairable</span>`);
  if (broken)
    pills.push(`<span class="pill bad">${broken} not repairable</span>`);
  if (noSerial === 0 && noValue === 0 && noDate === 0)
    pills.push(`<span class="pill good">✓ all fields populated</span>`);
  el.innerHTML = pills.join('');
}


function openEstateModal() {
  const modal = document.getElementById('inv-estate-modal');
  if (!modal) return;
  // Pre-fill from station config.
  const st = getStation();
  const callField = document.getElementById('inv-estate-callsign');
  const nameField = document.getElementById('inv-estate-name');
  const dateField = document.getElementById('inv-estate-date');
  if (callField && !callField.value) callField.value = st.callsign || '';
  if (nameField && !nameField.value) nameField.value = st.name || st.fullName || '';
  if (dateField && !dateField.value) {
    dateField.value = new Date().toISOString().slice(0, 10);
  }
  modal.style.display = 'flex';
}
function closeEstateModal() {
  document.getElementById('inv-estate-modal').style.display = 'none';
}

function generateEstateDocument(ev) {
  if (ev) ev.preventDefault();
  // Read radio button values (one per section, value = "include" or "exclude").
  function radioOn(name) {
    const el = document.querySelector(`input[name="${name}"]:checked`);
    return el ? el.value === 'include' : true;
  }
  const opts = {
    name: (document.getElementById('inv-estate-name').value || '').trim(),
    callsign: (document.getElementById('inv-estate-callsign').value || '').trim(),
    date: document.getElementById('inv-estate-date').value || new Date().toISOString().slice(0, 10),
    note: (document.getElementById('inv-estate-note').value || '').trim(),
    disposition: document.getElementById('inv-estate-disposition').value,
    sections: {
      contacts:  radioOn('estate-contacts'),
      inventory: radioOn('estate-inventory'),
      summary:   radioOn('estate-summary'),
      sale:      radioOn('estate-sale'),
      steps:     radioOn('estate-steps'),
      glossary:  radioOn('estate-glossary'),
    },
  };

  // Filter items by disposition selection.
  let items = state.inventory.items.slice();
  if (opts.disposition === 'working') {
    items = items.filter(it => it.disposition === 'working');
  } else if (opts.disposition === 'sellable') {
    items = items.filter(it => it.disposition === 'working' || it.disposition === 'repairable');
  }
  const contacts = (state.inventory.contacts || []).slice()
    .sort((a, b) => (a.priority || 999) - (b.priority || 999));

  if (!window.jspdf || !window.jspdf.jsPDF) {
    alert('PDF library failed to load. Refresh the page and try again.');
    return;
  }

  const filename = `estate-handoff-${(opts.callsign || 'station').replace(/[^a-zA-Z0-9_-]/g, '')}-${opts.date}.pdf`;
  buildEstatePdf(opts, items, contacts).save(filename);
  closeEstateModal();
}

function estateMoney(n) {
  if (n == null || n === '') return '-';
  const v = Number(n);
  if (isNaN(v)) return '-';
  return '$' + v.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}
function estateText(s) {
  // jsPDF accepts strings directly; convert null/undefined to empty.
  if (s == null) return '';
  return String(s);
}

function buildEstatePdf(opts, items, contacts) {
  const { jsPDF } = window.jspdf;
  const doc = new jsPDF({ unit: 'pt', format: 'letter' });
  const PAGE_W = doc.internal.pageSize.getWidth();   // 612 pt
  const PAGE_H = doc.internal.pageSize.getHeight();  // 792 pt
  const MARGIN_X = 54;   // 0.75 in
  const MARGIN_TOP = 54;
  const MARGIN_BOT = 60;
  const CONTENT_W = PAGE_W - MARGIN_X * 2;

  const dispLabel = {
    working: 'Working',
    repairable: 'Repairable',
    not_repairable: 'Not Repairable',
  };

  // Group inventory by type for the printout.
  const byType = {};
  for (const it of items) {
    const k = it.type_name || 'Other';
    if (!byType[k]) byType[k] = [];
    byType[k].push(it);
  }
  const typeOrder = Object.keys(byType).sort();

  // Compute summary stats.
  const totalValue = items.reduce((s, it) => s + (Number(it.estimated_value) || 0), 0);
  const totalCost  = items.reduce((s, it) => s + (Number(it.purchase_price)  || 0), 0);
  const dispCounts = items.reduce((acc, it) => {
    const k = it.disposition || 'unknown';
    acc[k] = (acc[k] || 0) + 1; return acc;
  }, {});
  const headline = items
    .filter(it => it.estimated_value != null && it.estimated_value !== '')
    .sort((a, b) => (Number(b.estimated_value) || 0) - (Number(a.estimated_value) || 0))
    .slice(0, 5);

  // ---- helpers ----
  let cursorY = MARGIN_TOP;

  function pageFooter(pageNum, totalPages) {
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.setTextColor(140);
    const label = `${opts.callsign || 'Estate Handoff'}  ·  page ${pageNum} of ${totalPages}`;
    doc.text(label, PAGE_W / 2, PAGE_H - 28, { align: 'center' });
    doc.setTextColor(0);
  }

  function ensureSpace(needed) {
    if (cursorY + needed > PAGE_H - MARGIN_BOT) {
      doc.addPage();
      cursorY = MARGIN_TOP;
    }
  }

  function newPage() {
    doc.addPage();
    cursorY = MARGIN_TOP;
  }

  function h1(text) {
    ensureSpace(40);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(18);
    doc.setTextColor(40);
    doc.text(text, MARGIN_X, cursorY);
    cursorY += 6;
    doc.setDrawColor(80);
    doc.setLineWidth(1);
    doc.line(MARGIN_X, cursorY, PAGE_W - MARGIN_X, cursorY);
    cursorY += 18;
    doc.setTextColor(0);
  }

  function h2(text) {
    ensureSpace(26);
    cursorY += 6;
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(13);
    doc.setTextColor(50);
    doc.text(text, MARGIN_X, cursorY);
    cursorY += 16;
    doc.setTextColor(0);
  }

  function paragraph(text, opts2) {
    opts2 = opts2 || {};
    doc.setFont('times', 'normal');
    doc.setFontSize(opts2.size || 11);
    doc.setTextColor(opts2.color != null ? opts2.color : 30);
    const lines = doc.splitTextToSize(text, CONTENT_W);
    for (const line of lines) {
      ensureSpace(14);
      doc.text(line, MARGIN_X, cursorY);
      cursorY += 14;
    }
    cursorY += 4;
    doc.setTextColor(0);
  }

  function bulletList(items) {
    doc.setFont('times', 'normal');
    doc.setFontSize(11);
    doc.setTextColor(30);
    for (const item of items) {
      const lines = doc.splitTextToSize(item, CONTENT_W - 14);
      ensureSpace(14 * lines.length + 2);
      doc.text('•', MARGIN_X, cursorY);
      for (let i = 0; i < lines.length; i++) {
        doc.text(lines[i], MARGIN_X + 14, cursorY);
        cursorY += 14;
      }
      cursorY += 2;
    }
    cursorY += 4;
    doc.setTextColor(0);
  }

  function numberedList(items) {
    doc.setFont('times', 'normal');
    doc.setFontSize(11);
    doc.setTextColor(30);
    for (let i = 0; i < items.length; i++) {
      const num = (i + 1) + '.';
      const lines = doc.splitTextToSize(items[i], CONTENT_W - 22);
      ensureSpace(14 * lines.length + 4);
      doc.text(num, MARGIN_X, cursorY);
      for (let j = 0; j < lines.length; j++) {
        doc.text(lines[j], MARGIN_X + 22, cursorY);
        cursorY += 14;
      }
      cursorY += 4;
    }
    cursorY += 4;
    doc.setTextColor(0);
  }

  function autotable(head, body, opts2) {
    opts2 = opts2 || {};
    doc.autoTable({
      head: [head],
      body: body,
      startY: cursorY,
      margin: { left: MARGIN_X, right: MARGIN_X },
      styles: { font: 'helvetica', fontSize: 9, cellPadding: 4, overflow: 'linebreak', valign: 'top' },
      headStyles: { fillColor: [225, 225, 225], textColor: 30, fontStyle: 'bold' },
      alternateRowStyles: { fillColor: [248, 248, 248] },
      columnStyles: opts2.columnStyles || {},
      didDrawPage: () => { /* footers added later */ },
    });
    cursorY = doc.lastAutoTable.finalY + 12;
  }

  // ---- Cover page ----
  // No margins here — center vertically on the cover.
  {
    const cy = PAGE_H / 2 - 100;
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(28);
    doc.setTextColor(50);
    doc.text('Amateur Radio Station', PAGE_W / 2, cy, { align: 'center' });
    doc.text('Estate Handoff', PAGE_W / 2, cy + 36, { align: 'center' });

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(54);
    doc.setTextColor(60);
    doc.text(estateText(opts.callsign || ''), PAGE_W / 2, cy + 110, { align: 'center' });

    doc.setFont('times', 'italic');
    doc.setFontSize(16);
    doc.setTextColor(80);
    doc.text(estateText(opts.name || ''), PAGE_W / 2, cy + 140, { align: 'center' });

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    doc.setTextColor(120);
    doc.text(`Document date: ${estateText(opts.date)}`, PAGE_W / 2, cy + 170, { align: 'center' });

    if (opts.note) {
      const noteWidth = CONTENT_W - 80;
      const lines = doc.splitTextToSize(opts.note, noteWidth);
      let y = cy + 220;
      doc.setFont('times', 'normal');
      doc.setFontSize(11);
      doc.setTextColor(40);
      // background box
      const boxX = MARGIN_X + 40;
      const boxH = lines.length * 14 + 24;
      doc.setFillColor(245, 245, 245);
      doc.setDrawColor(180);
      doc.rect(boxX, y - 14, noteWidth + 20, boxH, 'FD');
      // left-edge accent
      doc.setFillColor(120);
      doc.rect(boxX, y - 14, 3, boxH, 'F');
      for (const line of lines) {
        doc.text(line, boxX + 14, y);
        y += 14;
      }
      doc.setTextColor(0);
    }

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.setTextColor(150);
    doc.text('Prepared with WM3J ARS Suite — J-Hub Inventory  ·  See J-Learn §15 Estate / SK',
             PAGE_W / 2, PAGE_H - 50, { align: 'center' });
    doc.setTextColor(0);
  }

  // ---- "What this document is" orientation page (always) ----
  newPage();
  h1('What this document is');
  paragraph(`This is a record of the amateur radio station belonging to ${opts.name || opts.callsign || 'the operator'}. It exists so that, if the operator can no longer manage the station, family members can:`);
  bulletList([
    'Identify the equipment and roughly what each piece is worth.',
    'Reach trusted friends in the amateur-radio community who can help.',
    'Make informed decisions about selling, donating, or keeping items.',
    'Avoid common scams that target estate sales of technical equipment.',
  ]);
  paragraph("You don't need to know anything about radio to use this document. The instructions in the back of the booklet walk through each step in plain language. The first-call list near the front contains people who can help — start with the name listed as priority 1.");
  paragraph("Take your time. Nothing here is urgent. Most operators have built this collection over many years; there's no rush in deciding what to do with it.");

  // ---- First-call contacts ----
  if (opts.sections.contacts) {
    newPage();
    h1('First-call contacts');
    paragraph("Phone the people on this list in priority order (lowest number first). They've agreed in advance to help — or are friends and club leaders who will know the operator and the equipment.");
    if (contacts.length === 0) {
      paragraph('No contacts have been added to the inventory yet.', { color: 120 });
    } else {
      autotable(
        ['Pri', 'Name', 'Callsign', 'Phone', 'Email', 'Relationship', 'Items they wanted'],
        contacts.map(c => [
          estateText(c.priority),
          estateText(c.name),
          estateText(c.callsign),
          estateText(c.phone),
          estateText(c.email),
          estateText(c.relationship),
          estateText(c.items_wanted),
        ]),
        {
          columnStyles: {
            0: { cellWidth: 28, halign: 'right' },
            6: { cellWidth: 'auto' },
          },
        }
      );
    }
  }

  // ---- Value summary ----
  if (opts.sections.summary && items.length > 0) {
    newPage();
    h1('Value summary');
    autotable(
      ['Metric', 'Value'],
      [
        ['Total items', String(items.length)],
        ['Total estimated value', estateMoney(totalValue)],
        ['Original purchase total', estateMoney(totalCost)],
        ...Object.keys(dispCounts).map(k => [dispLabel[k] || k, dispCounts[k] + ' items']),
      ],
      { columnStyles: { 0: { cellWidth: 200, fontStyle: 'bold' } } }
    );
    if (headline.length > 0) {
      h2('Highest-value items');
      paragraph('These are the items most worth careful handling. Get a second opinion from someone on the first-call list before selling these.');
      autotable(
        ['Item', 'Manufacturer', 'Model', 'Serial', 'Value'],
        headline.map(it => [
          estateText(it.type_name),
          estateText(it.manufacturer),
          estateText(it.model),
          estateText(it.serial_number),
          estateMoney(it.estimated_value),
        ]),
        { columnStyles: { 4: { halign: 'right' } } }
      );
    }
  }

  // ---- Equipment inventory ----
  if (opts.sections.inventory) {
    newPage();
    h1('Equipment inventory');
    if (items.length === 0) {
      paragraph('No items match the selected filter. The operator may not have added items yet, or the disposition filter may be excluding everything.', { color: 120 });
    } else {
      paragraph("All items in the station, grouped by type. Serial numbers are the most important field for insurance claims and recall checks. Estimated values are the operator's best guess and may be high or low — verify with a knowledgeable friend before pricing.");
      for (const t of typeOrder) {
        const list = byType[t];
        h2(`${t}  (${list.length})`);
        autotable(
          ['Mfr', 'Model', 'Serial', 'Date', 'Value', 'Disp.', 'Location', 'Notes'],
          list.map(it => [
            estateText(it.manufacturer),
            estateText(it.model),
            estateText(it.serial_number),
            estateText(it.date_acquired),
            estateMoney(it.estimated_value),
            estateText(dispLabel[it.disposition] || it.disposition || ''),
            estateText(it.install_status === 'storage' ? (it.storage_location || 'Storage') : 'Installed'),
            estateText(it.notes),
          ]),
          {
            columnStyles: {
              4: { halign: 'right', cellWidth: 50 },
              7: { cellWidth: 'auto', fontStyle: 'italic', textColor: 80 },
            },
          }
        );
      }
    }
  }

  // ---- Sale recommendations ----
  if (opts.sections.sale) {
    newPage();
    h1('Where to sell');
    paragraph('The amateur-radio market has a small number of well-known dealers and online communities where used equipment trades regularly. None of these are "the best" — get quotes from two or three before committing.');

    h2('Major US dealers');
    autotable(
      ['Dealer', 'Strength', 'Notes'],
      [
        ['Ham Radio Outlet (HRO)', 'Largest US dealer; trade-ins on most modern gear',
         'Quotes a trade-in value, usually 50-70% of "blue book" retail. Several store locations + online (hamradio.com).'],
        ['DX Engineering', 'Antennas, accessories, kits',
         'Less frequent on used radios; will sometimes buy estate antennas (dxengineering.com).'],
        ['R&L Electronics', 'Mid-sized full-service dealer',
         'Hamilton, Ohio. Will travel for large estates (randl.com).'],
        ['Universal Radio', 'Vintage-friendly; specialty receivers',
         'Reynoldsburg, OH. Especially good for older Collins, Drake, Heath, Hallicrafters (universal-radio.com).'],
        ['GigaParts', 'Modern transceivers, antennas',
         'Will buy from estates; often-fair quotes (gigaparts.com).'],
      ],
      {
        columnStyles: {
          0: { cellWidth: 110, fontStyle: 'bold' },
          1: { cellWidth: 130 },
          2: { cellWidth: 'auto' },
        },
      }
    );

    h2('Online communities & classified sites');
    bulletList([
      'QRZ.com classifieds — large amateur swap board; free with QRZ account.',
      'eHam classifieds — second-largest amateur swap site (eham.net).',
      'QTH.com classifieds — long-running, trusted, free.',
      'eBay — broadest reach but more scams; use established sellers\' methods (returns enabled, escrow).',
      'Local club hamfests — see Step-by-step instructions for how to find them.',
    ]);

    h2('What to expect');
    bulletList([
      'Modern transceivers (last 5-10 years): typically sell for 50-70% of new retail.',
      'Older transceivers (10-20 years): 30-50% of new retail; less if cosmetically worn.',
      'Antennas and towers: hard to sell remotely; usually local pickup. Towers especially — buyer brings the climbing crew.',
      'Vintage gear (40+ years): collector market; values can be surprising — high or low. Get an expert opinion before pricing.',
      'Coax and consumables: very low resale; most estates donate these to clubs.',
    ]);

    h2('Watch out for');
    bulletList([
      'Lowball "I\'ll take everything for $500" offers. Polite but unequivocal "no thank you" until you have at least one second opinion.',
      'Wire transfers or gift cards. No legitimate dealer pays in gift cards. Treat any such request as a scam.',
      '"Shipping fee" buyers. Buyer says they\'ll send a check that includes shipping; you forward the shipping to a third party. The check bounces; you\'re out the shipping money.',
    ]);
  }

  // ---- Step-by-step instructions ----
  if (opts.sections.steps) {
    newPage();
    h1('Step-by-step instructions');
    paragraph('If you\'re reading this and wondering "what now?" — this is the order to do things. None are urgent. Most can take days or weeks.');
    numberedList([
      'Don\'t touch the antennas or tower yet. Outdoor structures need professional removal if they\'re going down. The radios indoors are stable; nothing will degrade in the short term.',
      'Phone the priority-1 contact from the first-call list. They\'ll know how to help.',
      'Decide whether to keep, sell, or donate. The first-call contact can help you think this through. Some families keep one radio as a memento; some sell everything; some donate to a local club.',
      'If selling: get quotes from two or three places. The dealer list above is a good starting point; the first-call contact may know better local options.',
      'If donating: local amateur-radio clubs often accept estate equipment. ARRL (arrl.org) lists clubs by state; a quick web search for "[your city] amateur radio club" finds the active local clubs.',
      'If keeping: the priority-1 contact can recommend storage. Outdoor antennas should come down within a few months to avoid neighbor complaints and weather damage.',
      'For the tower (if any): hire a professional tower-removal service. Don\'t attempt removal without the right equipment. The first-call contact can recommend someone local.',
      'For the callsign: the FCC license is in the operator\'s name. It can be retired by filing form 605 (license cancellation), or transferred to a family member who holds an amateur license. The first-call contact can help with the paperwork.',
      'Insurance and warranty: serial numbers are listed in the inventory. If items were covered under a homeowner\'s policy, the inventory + serial numbers + this document are your claim packet. Photograph any visible damage.',
      'Logbook and personal records: the operator may have decades of QSO logs (records of conversations). These often have sentimental value to the amateur community; check with the first-call contact about ARRL\'s logbook archive program before discarding.',
      'Take photographs of the station as it is, before any disassembly. Good for insurance and good for preserving a record.',
    ]);
  }

  // ---- Glossary ----
  if (opts.sections.glossary) {
    newPage();
    h1('Glossary for non-hams');
    paragraph('Plain-language definitions of terms used in this document.');
    const glossary = [
      ['Amateur radio (ham radio)', 'A licensed hobby in which people use radio equipment to communicate worldwide. Regulated by the FCC in the US.'],
      ['Callsign', 'A unique identifier issued by the FCC, like a license plate for the operator. Examples: WM3J, K1ABC. The operator\'s callsign is on the cover of this document.'],
      ['Transceiver / radio', 'The main radio that transmits and receives. The most valuable single item in most stations.'],
      ['Antenna', 'A wire, rod, or beam structure that connects to the radio and converts electrical signals into radio waves (and back). Often outdoors and visible.'],
      ['Coax (coaxial cable)', 'The thick black cable that connects the radio to the antenna. Looks like TV cable but heavier.'],
      ['Tower', 'A vertical metal structure (usually 30-80 feet) that supports antennas. Self-supporting or guyed (held up by wires).'],
      ['Tuner', 'A box that adjusts the radio\'s connection to the antenna. About the size of a desktop radio.'],
      ['Amplifier', 'A box that increases the radio\'s transmit power. Often heavy and uses a lot of electricity. Vacuum-tube amplifiers can be valuable.'],
      ['Repeater', 'A relay station that extends the range of small radios. Not in a typical home station; mentioned in case the operator\'s records reference a "club repeater."'],
      ['HF / VHF / UHF', 'Different ranges of radio frequencies. HF reaches around the world; VHF/UHF is local. Different antennas are used for each.'],
      ['QSL card', 'A postcard confirming a radio contact. Some operators have shoeboxes of these going back decades. Sentimental value, modest collector market for unusual ones.'],
      ['Logbook', 'A paper or electronic record of every contact made. Historical / sentimental value; ARRL archives complete logbooks.'],
      ['ARRL', 'American Radio Relay League — the national amateur-radio organization. Membership-based; provides licensing assistance, magazines, and ham-related services. arrl.org.'],
      ['FCC', 'Federal Communications Commission — issues amateur licenses. Has a form (605) to cancel a license.'],
      ['Hamfest', 'An amateur-radio swap meet, usually one weekend per year per region. ARRL.org lists upcoming hamfests by date.'],
      ['Silent Key (SK)', 'The amateur-radio community\'s term for a deceased operator. The expression "now SK" appears on QSL cards, social media, and club newsletters.'],
    ];
    autotable(
      ['Term', 'Definition'],
      glossary,
      {
        columnStyles: {
          0: { cellWidth: 130, fontStyle: 'bold' },
          1: { cellWidth: 'auto' },
        },
      }
    );
  }

  // ---- Page footers ----
  const totalPages = doc.internal.getNumberOfPages();
  for (let i = 1; i <= totalPages; i++) {
    doc.setPage(i);
    if (i === 1) continue; // skip footer on cover
    pageFooter(i, totalPages);
  }

  return doc;
}


function loadInventoryAll() {
  Promise.all([
    fetch('/api/inventory/types').then(r => r.json()),
    fetch('/api/inventory/items').then(r => r.json()),
    fetch('/api/inventory/contacts').then(r => r.json()),
  ]).then(([types, items, contacts]) => {
    state.inventory.types = types || [];
    state.inventory.items = items || [];
    state.inventory.contacts = contacts || [];
    populateTypeSelectors();
    renderInventoryTable();
    renderContactsTable();
    applyInventoryHelpPref();
  }).catch(err => console.error('inventory load failed', err));
}

function populateTypeSelectors() {
  const typeFilter = document.getElementById('inv-type-filter');
  const typeForm   = document.getElementById('inv-item-type');
  if (!typeFilter || !typeForm) return;
  // Filter dropdown (preserves the "All Types" first option).
  const cur = typeFilter.value;
  typeFilter.innerHTML = '<option value="">All Types</option>'
    + state.inventory.types.map(t => `<option value="${t.id}">${escHtml(t.name)}</option>`).join('');
  typeFilter.value = cur;
  // Form dropdown.
  typeForm.innerHTML = state.inventory.types.map(t =>
    `<option value="${t.id}">${escHtml(t.name)}</option>`).join('');
}

function renderInventoryTable() {
  const tbody = document.getElementById('inv-tbody');
  if (!tbody) return;
  const search    = (document.getElementById('inv-search')?.value || '').toLowerCase();
  const typeFilter = document.getElementById('inv-type-filter')?.value || '';
  const dispFilter = document.getElementById('inv-disposition-filter')?.value || '';
  const instFilter = document.getElementById('inv-install-filter')?.value || '';

  const items = state.inventory.items.filter(it => {
    if (typeFilter && String(it.type_id) !== typeFilter) return false;
    if (dispFilter && it.disposition !== dispFilter) return false;
    if (instFilter && it.install_status !== instFilter) return false;
    if (search) {
      const hay = [it.manufacturer, it.model, it.serial_number, it.notes,
                   it.storage_location, it.type_name].filter(Boolean).join(' ').toLowerCase();
      if (!hay.includes(search)) return false;
    }
    return true;
  });

  // Stats
  const stats = document.getElementById('inv-stats');
  if (stats) {
    const totalValue = items.reduce((sum, it) =>
      sum + (Number(it.estimated_value) || 0), 0);
    const totalCost  = items.reduce((sum, it) =>
      sum + (Number(it.purchase_price)  || 0), 0);
    stats.textContent = `${items.length} item${items.length === 1 ? '' : 's'} `
      + ` ·  Estimated value: $${totalValue.toFixed(2)}`
      + ` ·  Original cost: $${totalCost.toFixed(2)}`;
  }
  renderInventoryHealth();

  if (items.length === 0) {
    tbody.innerHTML = '<tr><td colspan="10" style="text-align:center;color:var(--subtext0);padding:20px">'
      + (state.inventory.items.length === 0
         ? 'No items yet. Click <b>+ Add Item</b> to start your inventory.'
         : 'No items match the current filters.')
      + '</td></tr>';
    return;
  }

  tbody.innerHTML = items.map(it => {
    const valueText = it.estimated_value != null
      ? '$' + Number(it.estimated_value).toFixed(2) : '—';
    const dispLabel = ({
      working:        'Working',
      repairable:     'Repairable',
      not_repairable: 'Not Repairable',
    })[it.disposition] || it.disposition || '';
    const installLabel = it.install_status === 'storage' ? 'Storage' : 'Installed';
    return `<tr>
      <td>${escHtml(it.type_name || '')}</td>
      <td>${escHtml(it.manufacturer || '')}</td>
      <td>${escHtml(it.model || '')}</td>
      <td><code style="font-size:11px">${escHtml(it.serial_number || '')}</code></td>
      <td>${escHtml(it.date_acquired || '')}</td>
      <td style="text-align:right">${valueText}</td>
      <td><span class="inv-disp-${it.disposition || ''}">${escHtml(dispLabel)}</span></td>
      <td>${escHtml(installLabel)}</td>
      <td>${escHtml(it.storage_location || '')}</td>
      <td class="inv-actions">
        <button onclick="openItemModal(${it.id})">Edit</button>
        <button class="del" onclick="deleteItem(${it.id})">Del</button>
      </td>
    </tr>`;
  }).join('');
}

function renderContactsTable() {
  const tbody = document.getElementById('inv-contacts-tbody');
  if (!tbody) return;
  if (state.inventory.contacts.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--subtext0);padding:20px">'
      + 'No contacts yet. Click <b>+ Add Contact</b> to add a friend, club leader, or dealer.'
      + '</td></tr>';
    return;
  }
  tbody.innerHTML = state.inventory.contacts.map(c => `<tr>
    <td>${c.priority || ''}</td>
    <td>${escHtml(c.name || '')}</td>
    <td>${escHtml(c.callsign || '')}</td>
    <td>${escHtml(c.phone || '')}</td>
    <td>${escHtml(c.email || '')}</td>
    <td>${escHtml(c.relationship || '')}</td>
    <td style="max-width:300px;white-space:normal">${escHtml(c.items_wanted || '')}</td>
    <td class="inv-actions">
      <button onclick="openContactModal(${c.id})">Edit</button>
      <button class="del" onclick="deleteContact(${c.id})">Del</button>
    </td>
  </tr>`).join('');
}

// ----- Item modal -----
function openItemModal(id) {
  const modal = document.getElementById('inv-item-modal');
  const title = document.getElementById('inv-item-modal-title');
  document.getElementById('inv-item-form').reset();
  document.getElementById('inv-item-id').value = '';
  if (id) {
    const it = state.inventory.items.find(x => x.id === id);
    if (!it) return;
    title.textContent = 'Edit Item — ' + (it.manufacturer || '') + ' ' + (it.model || '');
    document.getElementById('inv-item-id').value          = it.id;
    document.getElementById('inv-item-type').value        = it.type_id;
    document.getElementById('inv-item-manufacturer').value= it.manufacturer || '';
    document.getElementById('inv-item-model').value       = it.model || '';
    document.getElementById('inv-item-serial').value      = it.serial_number || '';
    document.getElementById('inv-item-date').value        = it.date_acquired || '';
    document.getElementById('inv-item-price').value       = it.purchase_price != null ? it.purchase_price : '';
    document.getElementById('inv-item-value').value       = it.estimated_value != null ? it.estimated_value : '';
    document.getElementById('inv-item-disposition').value = it.disposition || 'working';
    document.getElementById('inv-item-install').value     = it.install_status || 'installed';
    document.getElementById('inv-item-storage').value     = it.storage_location || '';
    document.getElementById('inv-item-notes').value       = it.notes || '';
  } else {
    title.textContent = 'Add Item';
    // Default to "today" for the date field if blank.
    const d = new Date();
    document.getElementById('inv-item-date').value =
      d.toISOString().slice(0, 10);
  }
  toggleStorageLocation();
  updateTypeHint();
  modal.style.display = 'flex';
}

function closeItemModal() {
  document.getElementById('inv-item-modal').style.display = 'none';
}

function toggleStorageLocation() {
  const inst = document.getElementById('inv-item-install')?.value;
  const row = document.getElementById('inv-item-storage-row');
  if (row) row.style.display = inst === 'storage' ? '' : 'none';
}

function saveItem(ev) {
  ev.preventDefault();
  const id = document.getElementById('inv-item-id').value;
  const body = {
    type_id:          parseInt(document.getElementById('inv-item-type').value, 10),
    manufacturer:     document.getElementById('inv-item-manufacturer').value,
    model:            document.getElementById('inv-item-model').value,
    serial_number:    document.getElementById('inv-item-serial').value,
    date_acquired:    document.getElementById('inv-item-date').value,
    purchase_price:   document.getElementById('inv-item-price').value,
    estimated_value:  document.getElementById('inv-item-value').value,
    disposition:      document.getElementById('inv-item-disposition').value,
    install_status:   document.getElementById('inv-item-install').value,
    storage_location: document.getElementById('inv-item-storage').value,
    notes:            document.getElementById('inv-item-notes').value,
  };
  const url    = id ? '/api/inventory/items/' + id : '/api/inventory/items';
  const method = id ? 'PUT' : 'POST';
  fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then(r => r.json())
    .then(data => {
      if (data.error) { alert('Save failed: ' + data.error); return; }
      closeItemModal();
      loadInventoryAll();
    })
    .catch(err => alert('Save failed: ' + err));
}

function deleteItem(id) {
  const it = state.inventory.items.find(x => x.id === id);
  if (!it) return;
  const label = (it.manufacturer || '') + ' ' + (it.model || '');
  if (!confirm('Delete "' + label.trim() + '"? This cannot be undone.')) return;
  fetch('/api/inventory/items/' + id, { method: 'DELETE' })
    .then(r => r.json())
    .then(() => loadInventoryAll())
    .catch(err => alert('Delete failed: ' + err));
}

function exportInventoryCsv() {
  window.location.href = '/api/inventory/export.csv';
}

// ----- Contact modal -----
function openContactModal(id) {
  const modal = document.getElementById('inv-contact-modal');
  const title = document.getElementById('inv-contact-modal-title');
  document.getElementById('inv-contact-form').reset();
  document.getElementById('inv-contact-id').value = '';
  if (id) {
    const c = state.inventory.contacts.find(x => x.id === id);
    if (!c) return;
    title.textContent = 'Edit Contact — ' + (c.name || '');
    document.getElementById('inv-contact-id').value           = c.id;
    document.getElementById('inv-contact-name').value         = c.name || '';
    document.getElementById('inv-contact-callsign').value     = c.callsign || '';
    document.getElementById('inv-contact-phone').value        = c.phone || '';
    document.getElementById('inv-contact-email').value        = c.email || '';
    document.getElementById('inv-contact-relationship').value = c.relationship || '';
    document.getElementById('inv-contact-priority').value     = c.priority != null ? c.priority : 100;
    document.getElementById('inv-contact-items').value        = c.items_wanted || '';
    document.getElementById('inv-contact-notes').value        = c.notes || '';
  } else {
    title.textContent = 'Add Contact';
    document.getElementById('inv-contact-priority').value = 100;
  }
  modal.style.display = 'flex';
}

function closeContactModal() {
  document.getElementById('inv-contact-modal').style.display = 'none';
}

function saveContact(ev) {
  ev.preventDefault();
  const id = document.getElementById('inv-contact-id').value;
  const body = {
    name:         document.getElementById('inv-contact-name').value,
    callsign:     document.getElementById('inv-contact-callsign').value,
    phone:        document.getElementById('inv-contact-phone').value,
    email:        document.getElementById('inv-contact-email').value,
    relationship: document.getElementById('inv-contact-relationship').value,
    priority:     parseInt(document.getElementById('inv-contact-priority').value, 10) || 100,
    items_wanted: document.getElementById('inv-contact-items').value,
    notes:        document.getElementById('inv-contact-notes').value,
  };
  const url    = id ? '/api/inventory/contacts/' + id : '/api/inventory/contacts';
  const method = id ? 'PUT' : 'POST';
  fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then(r => r.json())
    .then(data => {
      if (data.error) { alert('Save failed: ' + data.error); return; }
      closeContactModal();
      loadInventoryAll();
    })
    .catch(err => alert('Save failed: ' + err));
}

function deleteContact(id) {
  const c = state.inventory.contacts.find(x => x.id === id);
  if (!c) return;
  if (!confirm('Delete contact "' + (c.name || '') + '"?')) return;
  fetch('/api/inventory/contacts/' + id, { method: 'DELETE' })
    .then(r => r.json())
    .then(() => loadInventoryAll())
    .catch(err => alert('Delete failed: ' + err));
}

function escHtml(s) {
  if (s == null) return '';
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ===== UI prefs (theme + text size) =====
//
// Persisted in localStorage so the choice survives across reloads. The slider
// drives a CSS `zoom` on #app via the --vault-scale variable; the dropdown
// flips :root[data-theme] between mocha (dark) and latte (light).

function applyVaultScale(pct) {
  let v = parseInt(pct, 10);
  if (!isFinite(v) || v < 80 || v > 150) v = 100;
  document.documentElement.style.setProperty('--vault-scale', (v / 100).toString());
  const slider = document.getElementById('jv-scale');
  const label  = document.getElementById('jv-scale-val');
  if (slider) slider.value = v;
  if (label)  label.textContent = v;
  try { localStorage.setItem('jvault.ui.scale', String(v)); } catch (e) {}
}

function applyVaultTheme(name) {
  const theme = (name === 'latte') ? 'latte' : 'mocha';
  document.documentElement.setAttribute('data-theme', theme);
  const sel = document.getElementById('jv-theme');
  if (sel) sel.value = theme;
  try { localStorage.setItem('jvault.ui.theme', theme); } catch (e) {}
}

function resetVaultPrefs() {
  applyVaultScale(100);
  applyVaultTheme('mocha');
}

function reportVaultIssue() {
  const title = '[j-vault v1.5.0] <one-line summary>';
  const body =
`### Module
- Name: j-vault
- Version: 1.5.0
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

function loadVaultPrefs() {
  let scale = 100, theme = 'mocha';
  try {
    scale = parseInt(localStorage.getItem('jvault.ui.scale') || '100', 10);
    theme = localStorage.getItem('jvault.ui.theme') || 'mocha';
  } catch (e) {}
  applyVaultScale(scale);
  applyVaultTheme(theme);
}

// ===== Init =====
document.addEventListener('DOMContentLoaded', () => {
  loadVaultPrefs();
  loadStation();
  loadInventoryAll();
});

