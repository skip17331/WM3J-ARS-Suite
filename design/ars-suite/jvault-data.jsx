/* J-Vault — station inventory + estate planning data. */

const JVAULT_ITEMS = [
  { id: 1, cat: 'Transceiver', name: 'Icom IC-7610', sn: '0203145', yr: 2021, val: 2800, cond: 'excellent', disp: 'Son — Alex (KC2ABC)', note: 'Primary HF rig. Manual + box in closet.' },
  { id: 2, cat: 'Transceiver', name: 'Elecraft K4', sn: 'K4-1882', yr: 2023, val: 4400, cond: 'excellent', disp: 'Club — keep on air', note: 'Backup / SO2R radio.' },
  { id: 3, cat: 'Amplifier', name: 'SPE Expert 1.3K-FA', sn: 'FA13-771', yr: 2022, val: 3200, cond: 'excellent', disp: 'Son — Alex (KC2ABC)', note: '1.3 kW solid-state. Pairs with IC-7610.' },
  { id: 4, cat: 'Antenna', name: 'Hex Beam (6-band)', sn: '—', yr: 2020, val: 650, cond: 'good', disp: 'Club — dismantle', note: 'Tower-mounted at 50 ft. Needs rigger to remove.' },
  { id: 5, cat: 'Antenna', name: 'Mosley PRO-67B', sn: '—', yr: 2019, val: 1500, cond: 'good', disp: 'Sell — estate', note: '6-element tribander. Top of tower.' },
  { id: 6, cat: 'Rotator', name: 'Yaesu G-1000DXA', sn: 'G1K-44218', yr: 2019, val: 600, cond: 'good', disp: 'Son — Alex (KC2ABC)', note: 'Controller in shack, motor on tower.' },
  { id: 7, cat: 'Tower', name: 'US Towers MA-550', sn: '—', yr: 2018, val: 3800, cond: 'good', disp: 'Sell with house', note: 'Crank-up 55 ft. Permit on file.' },
  { id: 8, cat: 'Tuner', name: 'Palstar AT2K', sn: 'AT2K-9912', yr: 2017, val: 450, cond: 'good', disp: 'Sell — estate', note: 'Manual 2 kW tuner.' },
  { id: 9, cat: 'Accessory', name: 'Winkeyer USB', sn: '—', yr: 2016, val: 120, cond: 'good', disp: 'Son — Alex (KC2ABC)', note: 'CW keyer interface.' },
  { id: 10, cat: 'Accessory', name: 'Bird 43 Wattmeter', sn: 'B43-5521', yr: 2015, val: 280, cond: 'excellent', disp: 'Friend — W3LPL', note: 'With 100W + 1kW slugs.' },
  { id: 11, cat: 'Computer', name: 'Shack PC (logging)', sn: '—', yr: 2022, val: 900, cond: 'good', disp: 'Family — wipe first', note: 'Logs backed up to J-Vault + cloud.' },
  { id: 12, cat: 'Antenna', name: 'Inverted-L (160m)', sn: '—', yr: 2021, val: 200, cond: 'fair', disp: 'Club — dismantle', note: 'Wire + radials. Low priority.' },
];

const JVAULT_DOCS = [
  { name: 'Station insurance policy', tag: 'ARRL Equipment Plan', updated: '2026-01' },
  { name: 'Tower permit & engineering', tag: 'Township file #4471', updated: '2018-06' },
  { name: 'Estate letter to family', tag: 'What to do with the station', updated: '2026-03' },
  { name: 'Trusted contact', tag: 'W3LPL · Frank · (610) 555-0148', updated: '2025-11' },
  { name: 'Equipment manuals (PDF)', tag: '14 documents', updated: '2025-09' },
];

const JVAULT_DISP = {
  'Son — Alex (KC2ABC)': 'family', 'Club — keep on air': 'club', 'Club — dismantle': 'club',
  'Sell — estate': 'sell', 'Sell with house': 'sell', 'Friend — W3LPL': 'gift', 'Family — wipe first': 'family',
};

Object.assign(window, { JVAULT_ITEMS, JVAULT_DOCS, JVAULT_DISP });
