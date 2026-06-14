/* J-Sat — satellite tracking data. Sky plot uses a polar az/el projection
   (zenith at center, horizon at rim). Pass geometry is illustrative. */

// satellites with current state + next pass
const JSAT_SATS = [
  { name: 'AO-91', kind: 'FM', up: '435.250', dn: '145.960', status: 'AOS', maxEl: 47, az: 96, el: 31, dir: 'ascending', next: 'now', dur: '11m', footprint: 'EU·NA' },
  { name: 'SO-50', kind: 'FM', up: '145.850', dn: '436.795', status: 'next', maxEl: 22, az: 250, el: 0, dir: '—', next: '+18m', dur: '9m', footprint: 'NA' },
  { name: 'ISS', kind: 'APRS/Voice', up: '145.990', dn: '145.800', status: 'next', maxEl: 78, az: 300, el: 0, dir: '—', next: '+42m', dur: '10m', footprint: '—' },
  { name: 'RS-44', kind: 'Linear', up: '145.935', dn: '435.610', status: 'next', maxEl: 34, az: 200, el: 0, dir: '—', next: '+1h 26m', dur: '22m', footprint: '—' },
  { name: 'AO-7', kind: 'Linear', up: '432.180', dn: '145.970', status: 'LOS', maxEl: 12, az: 305, el: 0, dir: 'set', next: '+3h 02m', dur: '14m', footprint: '—' },
  { name: 'CAS-4B', kind: 'Linear', up: '435.280', dn: '145.870', status: 'next', maxEl: 58, az: 140, el: 0, dir: '—', next: '+2h 11m', dur: '16m', footprint: '—' },
];

// current pass track (az/el samples) for the active satellite — illustrative arc
const JSAT_TRACK = [
  { az: 70, el: 0 }, { az: 80, el: 12 }, { az: 90, el: 24 }, { az: 96, el: 31 },
  { az: 110, el: 40 }, { az: 130, el: 46 }, { az: 160, el: 47 }, { az: 195, el: 42 },
  { az: 225, el: 30 }, { az: 245, el: 16 }, { az: 255, el: 0 },
];
const JSAT_NOW_INDEX = 3; // current position along the track

Object.assign(window, { JSAT_SATS, JSAT_TRACK, JSAT_NOW_INDEX });
