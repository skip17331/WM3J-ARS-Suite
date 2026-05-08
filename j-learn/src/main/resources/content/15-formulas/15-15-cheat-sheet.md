---
id: 15-15
title: Cheat Sheet
chapter: 15
section: 15
level: simple
status: draft
---

# Cheat Sheet

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

One-page quick reference. Every formula card in §15-01 through §15-14 has its own dedicated calculator in the J-Hub Antenna Workshop tab; this page is for when you just want the equation in front of you.

## Core formulas

| Quantity | Formula | Units | See |
|----------|---------|-------|-----|
| Ohm's Law | V = I × R | V, A, Ω | §15-01 |
| Power (3 forms) | P = V·I = I²R = V²/R | W | §15-02 |
| Inductive reactance | X_L = 2πfL | Ω (f in Hz, L in H) | §15-03 |
| Capacitive reactance | X_C = 1 / (2πfC) | Ω (f in Hz, C in F) | §15-03 |
| Impedance magnitude | \|Z\| = √(R² + X²) | Ω | §15-04 |
| Impedance phase | φ = arctan(X / R) | degrees | §15-04 |
| Resonant frequency | f = 1 / (2π · √(L·C)) | Hz | §15-05 |
| Quick LC resonance | f(MHz) = 159.15 / √(L(µH)·C(pF)) | MHz | §15-05 |
| Free-space wavelength | λ(m) = 300 / f(MHz) | m | §15-06 |
| Half-wave dipole | length(ft) = 468 / f(MHz) | ft (thin wire, 5% end-effect) | §15-06 |
| Quarter-wave vertical | length(ft) = 234 / f(MHz) | ft | §15-06 |
| Reflection coefficient | Γ = (Z_L − Z₀) / (Z_L + Z₀) | dimensionless complex | §15-07 |
| SWR from Γ | SWR = (1 + \|Γ\|) / (1 − \|Γ\|) | dimensionless | §15-07 |
| SWR from power | \|Γ\| = √(P_r / P_f) → SWR formula above | — | §15-07 |
| ERP / EIRP | ERP = P_TX × G × L_feedline | W | §15-08 |
| dB ↔ ratio (power) | dB = 10·log₁₀(P₂/P₁) | dB | §15-10 |
| dB ↔ ratio (voltage) | dB = 20·log₁₀(V₂/V₁) | dB | §15-10 |
| dBm ↔ W | dBm = 10·log₁₀(W × 1000) | dBm | §15-10 |
| Q factor | Q = X_L / R = f / Δf₃dB | dimensionless | §15-11 |
| Bandwidth | BW = f / Q | Hz | §15-12 |
| Power density (free space) | S = EIRP / (4π · d²) | W/m² | §15-14 |
| Power density (worst case) | S = EIRP / (π · d²) | W/m² (4× ground reflect) | §15-14 |

## Useful constants

| Constant | Value | Where it shows up |
|----------|-------|-------------------|
| Speed of light *c* | 2.998 × 10⁸ m/s | wavelength, Doppler |
| Practical *c* (3 sig fig) | 3 × 10⁸ m/s | every wavelength calc |
| π | 3.14159 | reactance, resonance |
| 2π | 6.2832 | reactance, resonance, BW |
| Free-space impedance η₀ | 377 Ω | E-field / H-field math |
| Boltzmann k | 1.381 × 10⁻²³ J/K | noise floor |
| Earth radius (mean) | 6371 km | great-circle bearing |
| 4/3 Earth radius (refracted) | 8493 km | line-of-sight at VHF/UHF |
| Reference noise floor (290 K, 1 Hz) | −174 dBm/Hz | receiver sensitivity |

## Useful approximations

| Want to estimate... | Quick rule |
|---------------------|------------|
| dB ↔ multiplier (power) | +3 dB ≈ ×2; +10 dB = ×10; combine for any value |
| dB ↔ multiplier (voltage) | +6 dB ≈ ×2; +20 dB = ×10 |
| Half-wave length, ft | 468 / f(MHz) |
| Quarter-wave length, ft | 234 / f(MHz) |
| Half-wave length, m | 142.5 / f(MHz) |
| Antenna height for clean pattern | ≥ λ/4 above ground |
| 1 dBm | 1.26 mW |
| 1 W (logbook reality check) | 30 dBm |
| 100 W | 50 dBm |
| 1.5 kW (legal limit) | 62 dBm |
| Receiver "S9" (HF) | −73 dBm = ~50 µV at 50 Ω |
| Each S unit | nominally 6 dB |

## Metric ↔ imperial conversions

### Length

| From | × | = To |
|------|---|------|
| meters | 3.281 | feet |
| feet | 0.3048 | meters |
| inches | 2.540 | cm |
| cm | 0.3937 | inches |
| miles | 1.609 | km |
| km | 0.6214 | miles |
| nautical miles | 1.852 | km |

### Wire / conductor

| From | × | = To |
|------|---|------|
| AWG 14 | — | 0.0641 in / 1.628 mm dia |
| AWG 12 | — | 0.0808 in / 2.053 mm dia |
| AWG 10 | — | 0.1019 in / 2.588 mm dia |
| AWG 8  | — | 0.1285 in / 3.264 mm dia |
| inches² | 645.16 | mm² |
| circular mils | 5.067 × 10⁻⁴ | mm² |

### Power / energy

| From | × | = To |
|------|---|------|
| watts | 0.001 | kW |
| watts | 1000 | mW |
| dBm | (formula) | mW (10^(dBm/10)) |
| BTU | 1055 | joules |
| kWh | 3.6 × 10⁶ | joules |
| amp-hours @ 12 V | 12 | watt-hours |

### Frequency / wavelength

| From | × | = To |
|------|---|------|
| Hz | 10⁻⁶ | MHz |
| MHz | 10⁶ | Hz |
| MHz | 10³ | kHz |
| GHz | 10³ | MHz |
| Wavelength λ(m) | 1 | 300 / f(MHz) |
| Frequency f(MHz) | 1 | 300 / λ(m) |

### Speed

| From | × | = To |
|------|---|------|
| mph | 1.609 | km/h |
| km/h | 0.6214 | mph |
| m/s | 2.237 | mph |
| m/s | 3.6 | km/h |
| knots | 1.852 | km/h |

### Pressure / weather

| From | × | = To |
|------|---|------|
| inches Hg | 33.864 | hPa (mb) |
| hPa | 0.02953 | inches Hg |
| mph (wind) | 0.4470 | m/s |

### Temperature

| From | Formula | = To |
|------|---------|------|
| °F | (°F − 32) × 5/9 | °C |
| °C | °C × 9/5 + 32 | °F |
| °C | °C + 273.15 | K |

### Coordinates / bearings

| | Conversion |
|--|-----------|
| Latitude / longitude (decimal) | DD = degrees + minutes/60 + seconds/3600 |
| Maidenhead grid 4-char → lat/lon | (the §06-04 satellite calculator does this) |
| Compass to azimuth | 0° = N, 90° = E, 180° = S, 270° = W |
| Bearing magnetic ↔ true | true = magnetic + magnetic-variation (positive E) |

## Quick power chart

| Power | dBm | dBW | Reference |
|-------|----:|----:|-----------|
| 1 µW | −30 | −60 | weak signal |
| 1 mW | 0 | −30 | reference dBm |
| 10 mW | 10 | −20 | low-power digital |
| 100 mW | 20 | −10 | small handheld |
| 1 W | 30 | 0 | QRP / HT typical |
| 5 W | 37 | 7 | QRP boundary |
| 25 W | 44 | 14 | mobile typical |
| 100 W | 50 | 20 | base-station typical |
| 500 W | 57 | 27 | mid-size amplifier |
| 1500 W | 62 | 32 | US legal limit |

## Quick antenna-length chart

| Band | f (mid) | ½-wave (ft) | ¼-wave (ft) |
|------|--------:|------------:|------------:|
| 160m | 1.900 | 246.3 | 123.2 |
| 80m | 3.700 | 126.5 | 63.2 |
| 40m | 7.150 | 65.5 | 32.7 |
| 30m | 10.125 | 46.2 | 23.1 |
| 20m | 14.150 | 33.1 | 16.5 |
| 17m | 18.118 | 25.8 | 12.9 |
| 15m | 21.225 | 22.0 | 11.0 |
| 12m | 24.940 | 18.8 | 9.4 |
| 10m | 28.500 | 16.4 | 8.2 |
| 6m | 50.150 | 9.3 | 4.7 |
| 2m | 146.000 | 38.4 in | 19.2 in |
| 1.25m | 222.000 | 25.3 in | 12.7 in |
| 70cm | 446.000 | 12.6 in | 6.3 in |
| 23cm | 1296.000 | 4.3 in | 2.2 in |

## Coax loss reference (dB / 100 ft, matched)

| Cable | 1.8 | 7 | 14 | 28 | 50 | 144 | 432 | 1296 |
|-------|-----|---|----|----|----|-----|-----|------|
| RG-58 (foam) | 0.4 | 0.9 | 1.3 | 1.9 | 2.5 | 4.6 | 8.4 | 16.5 |
| RG-8X | 0.3 | 0.6 | 0.9 | 1.3 | 1.7 | 3.1 | 5.7 | 11.0 |
| RG-213 (foam) | 0.2 | 0.4 | 0.6 | 0.8 | 1.1 | 1.9 | 3.4 | 6.8 |
| LMR-400 | 0.1 | 0.3 | 0.4 | 0.6 | 0.8 | 1.4 | 2.6 | 4.8 |
| LMR-600 | 0.1 | 0.2 | 0.3 | 0.4 | 0.5 | 0.9 | 1.7 | 3.2 |
| 7/8" Heliax (LDF5-50A) | 0.05 | 0.1 | 0.15 | 0.2 | 0.3 | 0.5 | 1.0 | 1.9 |

(Frequencies in MHz across the top.)

## See also

- §15-00 — Formulas overview (the chapter map)
- §15-01 through §15-14 — every formula card has a calculator in the Workshop tab
- §16-02 — Coax loss tables (more cable types, more frequencies)
- §18-00 — Band Plans (where these antenna lengths apply)
