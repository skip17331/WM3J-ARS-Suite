/* J-Map — propagation / spot map data + great-circle geo helpers.
   Azimuthal-equidistant projection centered on the station QTH: every spot is
   placed at its true bearing (compass heading to point the beam) and distance
   (great-circle km) from home. Exported to window. */

const JMAP_QTH = { call: 'WM3J', grid: 'FN20', lat: 40.62, lon: -74.72 };

const D2R = Math.PI / 180, R2D = 180 / Math.PI, ER = 6371;
function greatCircle(lat1, lon1, lat2, lon2) {
  const φ1 = lat1 * D2R, φ2 = lat2 * D2R, dλ = (lon2 - lon1) * D2R;
  const y = Math.sin(dλ) * Math.cos(φ2);
  const x = Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(dλ);
  let brg = Math.atan2(y, x) * R2D; brg = (brg + 360) % 360;
  const dist = Math.acos(Math.min(1, Math.sin(φ1) * Math.sin(φ2) + Math.cos(φ1) * Math.cos(φ2) * Math.cos(dλ))) * ER;
  return { az: brg, dist };
}
// place (az,dist) on a circle of pixel radius R representing MAXKM
const MAXKM = 20015;
function project(az, dist, cx, cy, R) {
  const r = Math.min(1, dist / MAXKM) * R, a = (az - 90) * D2R;
  return { x: cx + Math.cos(a) * r, y: cy + Math.sin(a) * r };
}
// sub-solar point for the gray-line terminator
function subSolar(date) {
  const start = Date.UTC(date.getUTCFullYear(), 0, 0);
  const doy = (date - start) / 86400000;
  const decl = 23.44 * Math.sin((360 / 365) * (doy - 81) * D2R);
  const utch = date.getUTCHours() + date.getUTCMinutes() / 60 + date.getUTCSeconds() / 3600;
  const lon = -15 * (utch - 12);
  return { lat: decl, lon };
}

const JMAP_BANDS = ['160', '80', '40', '30', '20', '17', '15', '12', '10'];
const BAND_COLOR = {
  '160': 'oklch(0.70 0.13 25)', '80': 'oklch(0.76 0.13 55)', '40': 'oklch(0.80 0.13 92)',
  '30': 'oklch(0.78 0.13 140)', '20': 'oklch(0.75 0.12 200)', '17': 'oklch(0.72 0.12 245)',
  '15': 'oklch(0.70 0.13 285)', '12': 'oklch(0.70 0.13 320)', '10': 'oklch(0.68 0.14 350)',
};

// region anchors — labels placed at true bearing/distance
const JMAP_REGIONS = [
  { name: 'EUROPE', lat: 50, lon: 15 }, { name: 'AFRICA', lat: 2, lon: 22 },
  { name: 'ASIA', lat: 45, lon: 95 }, { name: 'OCEANIA', lat: -25, lon: 150 },
  { name: 'S. AMERICA', lat: -15, lon: -60 }, { name: 'W. N.A.', lat: 50, lon: -120 },
];

// DX spots — real lat/lon → bearing/distance computed in app
const JMAP_SPOTS = [
  { call: 'DL8WPX', lat: 51, lon: 10, region: 'Germany', band: '20', mode: 'SSB', f: '14.182', age: 1 },
  { call: 'G4ABC', lat: 52, lon: -1, region: 'England', band: '20', mode: 'CW', f: '14.025', age: 2 },
  { call: 'EA8RKL', lat: 28, lon: -16, region: 'Canary Is.', band: '15', mode: 'CW', f: '21.024', age: 3, mult: true, need: true },
  { call: 'CN2AA', lat: 33, lon: -6, region: 'Morocco', band: '20', mode: 'SSB', f: '14.225', age: 4, mult: true, need: true },
  { call: 'ZD7BG', lat: -16, lon: -5, region: 'St. Helena', band: '20', mode: 'SSB', f: '14.182', age: 2, mult: true, need: true },
  { call: '5U5R', lat: 17, lon: 8, region: 'Niger', band: '40', mode: 'CW', f: '7.005', age: 5, mult: true, need: true },
  { call: 'JA1XYZ', lat: 36, lon: 138, region: 'Japan', band: '20', mode: 'SSB', f: '14.250', age: 6 },
  { call: 'JA7QVI', lat: 39, lon: 140, region: 'Japan', band: '30', mode: 'FT8', f: '10.136', age: 7 },
  { call: 'VU2XYZ', lat: 13, lon: 77, region: 'India', band: '15', mode: 'SSB', f: '21.300', age: 12, mult: true, need: true },
  { call: '4X1ABC', lat: 32, lon: 34, region: 'Israel', band: '20', mode: 'CW', f: '14.030', age: 9, mult: true },
  { call: 'VK9DX', lat: -29, lon: 168, region: 'Norfolk Is.', band: '20', mode: 'CW', f: '14.022', age: 1, mult: true },
  { call: '9M6XRO', lat: 5, lon: 117, region: 'E. Malaysia', band: '20', mode: 'SSB', f: '14.285', age: 5, mult: true, need: true },
  { call: 'FO5QB', lat: -17, lon: -149, region: 'Fr. Polynesia', band: '17', mode: 'CW', f: '18.072', age: 7, mult: true, need: true },
  { call: 'ZL3ABC', lat: -43, lon: 172, region: 'New Zealand', band: '15', mode: 'SSB', f: '21.290', age: 15, mult: true },
  { call: 'PY2NY', lat: -23, lon: -46, region: 'Brazil', band: '10', mode: 'SSB', f: '28.495', age: 9 },
  { call: 'LU5ABC', lat: -34, lon: -58, region: 'Argentina', band: '15', mode: 'CW', f: '21.020', age: 11, mult: true },
  { call: 'CE3ABC', lat: -33, lon: -70, region: 'Chile', band: '20', mode: 'SSB', f: '14.200', age: 14, mult: true, need: true },
  { call: 'VE7CC', lat: 49, lon: -123, region: 'Canada', band: '20', mode: 'FT8', f: '14.074', age: 3 },
  { call: 'K1TTT', lat: 42, lon: -72, region: 'USA', band: '20', mode: 'CW', f: '14.040', age: 2 },
  { call: 'TF3IRA', lat: 64, lon: -22, region: 'Iceland', band: '40', mode: 'SSB', f: '7.150', age: 18, mult: true, need: true },
  { call: 'UA9ABC', lat: 55, lon: 83, region: 'Asiatic Russia', band: '20', mode: 'CW', f: '14.018', age: 13, mult: true, need: true },
  { call: 'ZS6ABC', lat: -26, lon: 28, region: 'South Africa', band: '15', mode: 'SSB', f: '21.250', age: 16, mult: true, need: true },
];

function modeGroup(m) { return m === 'CW' ? 'CW' : (m === 'FT8' || m === 'FT4' || m === 'RTTY' || m === 'PSK') ? 'Digi' : 'Phone'; }

Object.assign(window, { JMAP_QTH, JMAP_SPOTS, JMAP_REGIONS, JMAP_BANDS, BAND_COLOR, greatCircle, project, subSolar, MAXKM, modeGroup });
