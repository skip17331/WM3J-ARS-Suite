/* J-Log cockpit — data + CONTEST registry.
   Each contest defines its received-exchange fields, sent exchange, serial
   use, and multiplier rule, so the entry pane reshapes per contest. */

const F = (k, l, w, d) => ({ k, l, w, d: d || '', mono: true });   // mono field
const Ft = (k, l, w) => ({ k, l, w, d: '', mono: false });          // text field

const CONTESTS = [
  { id: 'cqww', name: 'CQ WW DX', mult: 'Zones', mySent: '59 05',
    fields: [F('rst', 'RST', 54, '59'), F('zone', 'CQ Zone', 76)], multField: 'zone',
    seed: ['14', '36', '30', '25', '11', '3', '28', '15', '16', '20', '8', '33', '40'] },
  { id: 'wpx', name: 'CQ WPX', mult: 'Prefixes', serial: true, mySent: '59 #',
    fields: [F('rst', 'RST', 54, '59'), F('nr', 'Nr Rcvd', 90)], multPrefix: true,
    seed: ['DL8', 'K1', 'PY2', 'VK9', 'EA5', 'G3', 'CT1', '9M6', 'ZD7', 'JA1', 'CN2', 'VE7'] },
  { id: 'arrldx', name: 'ARRL DX — DX side', mult: 'States + VE', mySent: '59 KW',
    fields: [F('rst', 'RST', 54, '59'), Ft('spc', 'State / Prov', 110)], multField: 'spc',
    seed: ['PA', 'NY', 'CA', 'TX', 'ON', 'BC', 'FL', 'OH', 'WA', 'MA', 'IL', 'NC'] },
  { id: 'ss', name: 'ARRL Sweepstakes', mult: 'Sections (84)', serial: true, mySent: '# B WM3J 73 EPA',
    fields: [F('nr', 'Nr', 64), F('prec', 'Prec', 56), F('chk', 'Chk', 56), Ft('sect', 'Sect', 76)], multField: 'sect',
    seed: ['EPA', 'WPA', 'MDC', 'NNJ', 'SNJ', 'DE', 'EMA', 'CT', 'RI', 'VT', 'NH', 'ME', 'WNY', 'ENY'] },
  { id: 'fd', name: 'ARRL Field Day', mult: 'Sections', mySent: '2A EPA',
    fields: [Ft('class', 'Class', 70), Ft('sect', 'Section', 92)], multField: 'sect',
    seed: ['EPA', 'WPA', 'MDC', 'NNJ', 'VA', 'MD', 'OH', 'WNY'] },
  { id: 'naqp', name: 'NAQP', mult: 'States / Prov', mySent: 'Jim PA',
    fields: [Ft('name', 'Name', 110), Ft('spc', 'St / Prov', 88)], multField: 'spc',
    seed: ['PA', 'NY', 'OH', 'CA', 'TX', 'FL', 'ON', 'VA', 'NC', 'GA', 'MI'] },
  { id: 'iaru', name: 'IARU HF', mult: 'Zones + HQ', mySent: '59 08',
    fields: [F('rst', 'RST', 54, '59'), Ft('exch', 'Zone / HQ', 100)], multField: 'exch',
    seed: ['08', '14', '15', '25', '30', 'ARRL', 'RAC', 'DARC', 'NU1AW'] },
  { id: 'vhf', name: 'ARRL VHF', mult: 'Grids', mySent: 'FN20',
    fields: [F('grid', 'Grid', 100)], multField: 'grid',
    seed: ['FN20', 'FN21', 'FN10', 'FM19', 'EN90', 'FN30', 'FN31', 'FM29'] },
  { id: 'general', name: 'General / Ragchew', general: true,
    fields: [F('rst', 'RST', 54, '59'), Ft('name', 'Name', 110), Ft('qth', 'QTH', 124), F('grid', 'Grid', 86)] },
];

function wpxPrefix(call) {
  const c = (call || '').toUpperCase();
  let m = c.match(/^([A-Z0-9]*?\d)[A-Z]+$/);
  if (m) return m[1];
  m = c.match(/^([A-Z0-9]*\d)/);
  return m ? m[1] : c.slice(0, 2);
}

const JLOG_QSOS = [
  { t: '14:42', call: 'DL8WPX', band: '20', mode: 'SSB', snt: '59 05', rcv: '59 14', mult: false },
  { t: '14:41', call: 'ZD7BG', band: '20', mode: 'SSB', snt: '59 05', rcv: '59 36', mult: true },
  { t: '14:40', call: 'VK9DX', band: '20', mode: 'SSB', snt: '59 05', rcv: '59 30', mult: true },
  { t: '14:39', call: 'JA1XYZ', band: '20', mode: 'SSB', snt: '59 05', rcv: '59 25', mult: false },
  { t: '14:38', call: 'EA5K', band: '20', mode: 'SSB', snt: '59 05', rcv: '59 14', mult: false },
  { t: '14:37', call: 'G4ABC', band: '20', mode: 'SSB', snt: '59 05', rcv: '59 14', mult: false },
  { t: '14:36', call: 'PY2NY', band: '20', mode: 'SSB', snt: '59 05', rcv: '59 11', mult: false },
  { t: '14:35', call: 'VE7CC', band: '20', mode: 'SSB', snt: '59 05', rcv: '59 03', mult: false },
  { t: '14:33', call: 'CT1BOH', band: '20', mode: 'SSB', snt: '59 05', rcv: '59 14', mult: false },
  { t: '14:31', call: '9M6XRO', band: '20', mode: 'SSB', snt: '59 05', rcv: '59 28', mult: true },
];

const JLOG_BANDMAP = [
  { f: '14.182', call: 'ZD7BG', mult: true, need: true },
  { f: '14.205', call: 'VK9DX', mult: true, need: false },
  { f: '14.225', call: 'CN2AA', mult: true, need: true },
  { f: '14.250', call: 'WM3J', me: true },
  { f: '14.263', call: 'PY2NY', mult: false, need: false },
  { f: '14.285', call: '9M6XRO', mult: true, need: true },
  { f: '14.310', call: 'JA7QVI', mult: false, need: false },
  { f: '14.332', call: '5U5R', mult: true, need: true },
];

const JLOG_MASTER = ['DL8WPX', 'DL1ABC', 'DL3XYZ', 'DK5ABC', 'EA5K', 'EA8RKL', 'G4ABC', 'G3XYZ', 'VK9DX', 'VK3MO', 'JA1XYZ', 'JA7QVI', 'PY2NY', 'PT5J', 'VE7CC', 'CT1BOH', '9M6XRO', 'ZD7BG', 'CN2AA', '5U5R', 'TF3IRA', 'FO5QB', 'K1TTT', 'W3LPL', 'N6TR'];

const JLOG_MACROS = [
  ['F1', 'CQ', 'CQ TEST WM3J WM3J'],
  ['F2', 'Exch', '{call} {snt}'],
  ['F3', 'TU', 'TU WM3J'],
  ['F4', 'My Call', 'WM3J'],
  ['F5', 'His Call', '{call}'],
  ['F6', 'Repeat', 'AGN AGN'],
  ['F7', 'Fill?', 'NR?'],
  ['F8', 'QRZ', 'QRZ WM3J'],
];

const JLOG_RATE = [3, 2, 4, 5, 3, 6, 4, 5, 7, 5];

const JL_SOLAR = [['SFI', '168'], ['SN', '142'], ['A', '7'], ['K', '2'], ['X-ray', 'B1'], ['304Å', '171'], ['Aur', '1.5'], ['MUF', '28']];
const JL_BANDCOND = [['80–40m', 'good', 'good'], ['30–20m', 'good', 'fair'], ['17–15m', 'fair', 'poor'], ['12–10m', 'fair', 'poor']];
const JL_ROTOR_PRESETS = [['EU', 50], ['AF', 95], ['SA', 155], ['Carib', 135], ['AS', 330], ['OC', 250]];
const JL_DXCC_BY_PFX = [
  [/^DL|^DK|^DJ|^DF/, ['Germany', '14', '28']], [/^(G|M|2E)/, ['England', '14', '27']], [/^F/, ['France', '14', '27']],
  [/^EA[0-9]/, ['Spain', '14', '37']], [/^I/, ['Italy', '15', '28']], [/^(JA|JH|JR|7K)/, ['Japan', '25', '45']],
  [/^VK/, ['Australia', '30', '59']], [/^(PY|PP|PT)/, ['Brazil', '11', '15']], [/^VE|^VA|^VO/, ['Canada', '05', '09']],
  [/^(K|W|N|A)/, ['United States', '05', '08']], [/^ZD7/, ['St. Helena', '36', '66']], [/^9M6/, ['East Malaysia', '28', '54']],
];
function jlDxcc(call) {
  const c = (call || '').toUpperCase(); if (c.length < 2) return null;
  const m = JL_DXCC_BY_PFX.find(([re]) => re.test(c));
  return m ? { country: m[1][0], cq: m[1][1], itu: m[1][2] } : null;
}

Object.assign(window, { CONTESTS, wpxPrefix, JLOG_QSOS, JLOG_BANDMAP, JLOG_MASTER, JLOG_MACROS, JLOG_RATE, JL_SOLAR, JL_BANDCOND, JL_ROTOR_PRESETS, jlDxcc });
