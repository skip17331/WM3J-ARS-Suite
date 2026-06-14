/* J-Hub Workspace — information architecture.
   The flat 20-item web nav, regrouped. Two top-level kinds:
     • MODULES  — operate (launch/stop/focus); each also has a config page.
     • J-HUB    — configure: Dashboard, Station, Hardware, Data, Intel.
   Config pages are data-driven (see WS_PAGES) so the field renderer stays small. */

// Configure-side nav. Dashboard sits alone; the rest are grouped.
const WS_NAV = [
  { id: 'dashboard', label: 'Dashboard', glyph: 'hub', solo: true },
  { id: 'station', label: 'Station', glyph: 'vault', items: [
      { id: 'identity', label: 'Identity & callsign' },
      { id: 'location', label: 'Location & grid' },
      { id: 'operators', label: 'Operators' },
  ] },
  { id: 'hardware', label: 'Hardware', glyph: 'sat', items: [
      { id: 'rig', label: 'Rig control' },
      { id: 'rotor', label: 'Rotor control' },
      { id: 'amp', label: 'Amplifier' },
      { id: 'antsw', label: 'Antenna switch' },
      { id: 'workshop', label: 'Antenna workshop' },
  ] },
  { id: 'data', label: 'Data', glyph: 'log', items: [
      { id: 'logging', label: 'Logging & data' },
      { id: 'macros', label: 'Macros' },
      { id: 'backup', label: 'Backup & sync' },
  ] },
  { id: 'intel', label: 'Intel', glyph: 'map', items: [
      { id: 'cluster', label: 'DX cluster' },
      { id: 'weather', label: 'Weather' },
  ] },
];

// Per-module config target ids (module chiclet gear → these pages).
const WS_MODULE_CFG = { log: 'cfg-log', map: 'cfg-map', bridge: 'cfg-bridge', digi: 'cfg-digi', sat: 'cfg-sat', vault: 'cfg-vault', learn: 'cfg-learn' };

/* field types: readout · text · addr · select · toggle · segmented · slider */
const WS_PAGES = {
  rig: {
    title: 'Rig control', group: 'Hardware', glyph: 'sat',
    intro: 'CAT control for your transceiver — connection, PTT, and how J-Hub follows the radio.',
    live: [ ['Status', 'CONNECTED', 'ok'], ['Frequency', '14.074.00 MHz'], ['Mode', 'USB'], ['Poll', '12 ms'] ],
    sections: [
      { title: 'Connection', fields: [
        { type: 'select', label: 'Transceiver model', value: 'Icom IC-7610', options: ['Icom IC-7610', 'Icom IC-7300', 'Yaesu FTDX10', 'Kenwood TS-890S', 'Elecraft K4'] },
        { type: 'segmented', label: 'Interface', value: 'USB', options: ['USB', 'Serial', 'Network'] },
        { type: 'select', label: 'Port', value: '/dev/cu.SLAB_USBtoUART', options: ['/dev/cu.SLAB_USBtoUART', 'COM4', 'COM5'] },
        { type: 'select', label: 'Baud rate', value: '19200', options: ['9600', '19200', '38400', '115200'] },
        { type: 'addr', label: 'CI-V address', value: '0x98', hint: 'Icom civ address (hex)' },
        { type: 'slider', label: 'Poll interval', value: 12, min: 5, max: 200, unit: 'ms' },
      ] },
      { title: 'PTT & keying', fields: [
        { type: 'segmented', label: 'PTT method', value: 'CAT', options: ['CAT', 'RTS', 'DTR', 'VOX'] },
        { type: 'select', label: 'CW keyer', value: 'Winkeyer USB', options: ['Winkeyer USB', 'Rig built-in', 'None'] },
        { type: 'slider', label: 'TX delay', value: 30, min: 0, max: 200, unit: 'ms' },
      ] },
      { title: 'Behavior', fields: [
        { type: 'toggle', label: 'Auto band-follow', value: true, hint: 'Switch antenna & amp band with the rig' },
        { type: 'toggle', label: 'Split tracking', value: true },
        { type: 'toggle', label: 'Power on with J-Hub', value: true },
        { type: 'toggle', label: 'Lock TX outside band edges', value: false },
      ] },
    ],
  },
  identity: {
    title: 'Identity & callsign', group: 'Station', glyph: 'vault',
    intro: 'Who you are on the air. Used across every module and upload service.',
    sections: [
      { title: 'Station identity', fields: [
        { type: 'text', label: 'Callsign', value: 'WM3J', mono: true },
        { type: 'text', label: 'Operator name', value: 'Jim' },
        { type: 'text', label: 'Grid square', value: 'FN20', mono: true },
        { type: 'segmented', label: 'License class', value: 'Extra', options: ['Tech', 'General', 'Extra'] },
      ] },
      { title: 'Awards & zones', fields: [
        { type: 'addr', label: 'CQ zone', value: '05' },
        { type: 'addr', label: 'ITU zone', value: '08' },
        { type: 'text', label: 'DXCC entity', value: 'United States' },
        { type: 'addr', label: 'IOTA', value: 'NA-001' },
      ] },
      { title: 'Defaults', fields: [
        { type: 'select', label: 'Default mode', value: 'USB', options: ['USB', 'LSB', 'CW', 'FT8', 'RTTY'] },
        { type: 'slider', label: 'Default power', value: 92, min: 5, max: 100, unit: 'W' },
      ] },
    ],
  },
  'cfg-log': {
    title: 'J-Log', group: 'Module settings', glyph: 'log', hue: 'log',
    intro: 'Logging engine — files, dupe checking, contest defaults and uploads.',
    live: [ ['Module', 'RUNNING', 'ok'], ['Today', '1,284 QSOs'], ['Log', 'wm3j-2026.adi'] ],
    sections: [
      { title: 'Log file', fields: [
        { type: 'select', label: 'Active log', value: 'wm3j-2026.adi', options: ['wm3j-2026.adi', 'cqww-2025.adi', 'fielday-2025.adi'] },
        { type: 'text', label: 'ADIF export path', value: '~/Documents/ARS/logs', mono: true },
        { type: 'toggle', label: 'Real-time duplicate check', value: true },
        { type: 'toggle', label: 'Auto-fill from previous QSO', value: true },
      ] },
      { title: 'Uploads', fields: [
        { type: 'toggle', label: 'LoTW auto-upload', value: true },
        { type: 'toggle', label: 'eQSL auto-upload', value: false },
        { type: 'toggle', label: 'QRZ logbook sync', value: true },
      ] },
      { title: 'Contest', fields: [
        { type: 'select', label: 'Default contest', value: 'CQ WW DX', options: ['CQ WW DX', 'ARRL DX', 'CQ WPX', 'Sweepstakes', 'None'] },
        { type: 'segmented', label: 'Entry behavior', value: 'Fast', options: ['Fast', 'Guided', 'Verbose'] },
      ] },
    ],
  },
};

// Lightweight generated stubs for nav items we don't fully spec (clean, not empty).
const WS_STUB = (title, group, glyph) => ({ title, group, glyph, stub: true, intro: 'Settings for ' + title + '. Wired into the same grouped structure — drop fields in here.' });

Object.assign(window, { WS_NAV, WS_MODULE_CFG, WS_PAGES, WS_STUB });
