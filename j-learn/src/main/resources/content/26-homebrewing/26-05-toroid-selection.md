---
id: 26-05
title: Toroid Selection (Powdered Iron)
chapter: 26
section: 05
level: mixed
status: draft
---

# Toroid Selection (Powdered Iron)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## Powdered iron vs ferrite — first decision

Toroidal cores fall into two families, and **picking the wrong family kills your circuit**:

| | Powdered iron | Ferrite |
|---|---------------|---------|
| Part number prefix | **T-**xx-yy (Amidon) | **FT-**xx-yy (Amidon) |
| Permeability (µ) | 1–100 | 100–10000 |
| Q at HF | **High (50–300)** | Low (5–50) |
| Loss | **Low** | High |
| Use | **Tuned circuits, filters** | **Chokes, transformers** |

The rule is simple: **powdered iron for filters and resonant circuits; ferrite for broadband chokes and transformers**. Trying to use a ferrite core for a 14 MHz LPF gives 3 dB of insertion loss instead of 0.3 dB — the filter cooks itself at 100 W. Trying to use powdered iron for a common-mode choke gets you ~50 Ω of choking impedance instead of ~3000 Ω.

This section covers **powdered iron**. Ferrite is in [§26-06](26-06-ferrite-mix-selection.md).

## The mix system — color codes

Powdered-iron toroids come in **mixes**, each tuned for a frequency range. Amidon (the dominant U.S. supplier) color-codes them:

| Mix | Color | µ_init | Best frequency | Typical amateur use |
|-----|-------|--------|----------------|---------------------|
| Mix 1 | Blue | 20 | 0.5–5 MHz | Audio chokes, LF |
| **Mix 2** | **Red** | **10** | **2–30 MHz** | **80 m / 160 m filters, antennas** |
| **Mix 3** | **Gray** | **35** | **50 kHz – 0.5 MHz** | **LF / VLF; rare in amateur** |
| **Mix 6** | **Yellow** | **8.5** | **10–50 MHz** | **40 m through 15 m filters** |
| Mix 7 | White | 9 | 1–25 MHz | Wide-band; less common |
| **Mix 10** | **Black** | **6** | **30–100 MHz** | **6 m, 2 m filters; 10 m sometimes** |
| **Mix 12** | **Green/white** | **4** | **50–200 MHz** | **VHF resonant circuits** |
| **Mix 17** | **Blue/yellow** | **4** | **20–200 MHz** | **HF high-band + VHF** |
| Mix 15 | Red/white | 25 | 0.1–2 MHz | LF chokes / filters |
| Mix 26 | Yellow/white | 75 | 30 kHz – 1 MHz | Audio, switching supplies |

The Amidon catalog has a graph of Q vs frequency for each mix. Q peaks in the mix's design range and falls off on either side. Using Mix 2 (red) at 28 MHz works (Q ≈ 100 instead of 200), but using it at 100 MHz gives Q ≈ 20 — barely a filter.

## The A_L value — turn count from inductance

Each core has an **A_L value**, which tells you how much inductance you get per turn. Two conventions exist:

- **A_L in µH per 100 turns** (Amidon classic catalog)
- **A_L in nH per turn²** (modern datasheets, more useful)

For nH/turn²: `L_nH = A_L × N²`, or `N = √(L_nH / A_L)`.

For µH/100 turns: `L_µH = A_L × (N/100)²`, or `N = 100 × √(L_µH / A_L)`.

| Core | Mix | A_L (nH/N²) | A_L (µH/100²) |
|------|-----|-------------|---------------|
| T-37-2 | 2 (red) | 4.0 | 4.0 |
| T-37-6 | 6 (yellow) | 3.0 | 3.0 |
| T-37-10 | 10 (black) | 2.5 | 2.5 |
| **T-50-2** | **2 (red)** | **5.0** | **5.0** |
| **T-50-6** | **6 (yellow)** | **4.0** | **4.0** |
| T-50-10 | 10 (black) | 3.1 | 3.1 |
| T-50-17 | 17 (blue/yellow) | 2.1 | 2.1 |
| T-68-2 | 2 (red) | 5.7 | 5.7 |
| T-68-6 | 6 (yellow) | 4.7 | 4.7 |
| T-94-2 | 2 (red) | 8.4 | 8.4 |
| T-130-2 | 2 (red) | 11.0 | 11.0 |
| T-200-2 | 2 (red) | 12.0 | 12.0 |

The T-numbering convention: **T-50-6** means powdered iron toroid, OD = 0.5 inch, Mix 6 (yellow). Sizes commonly stocked by amateurs: **T-37** (0.37" OD), **T-50** (0.50" OD), **T-68** (0.68"), **T-94** (0.94"), **T-130** (1.30"), **T-200** (2.00").

## Band-to-mix quick guide

The "default" mix for each ham band:

| Band | Filter mix | Antenna trap mix | Tank coil mix |
|------|------------|------------------|----------------|
| 160 m (1.8 MHz) | **Mix 2** (red), T-200-2 for power | Mix 2 | Mix 2 |
| 80 m (3.5 MHz) | **Mix 2** (red), T-130-2 for power | Mix 2 | Mix 2 |
| 40 m (7 MHz) | **Mix 2 or 6** (red or yellow) | Mix 2 | Mix 2 |
| 30 m (10 MHz) | **Mix 6** (yellow) | Mix 6 | Mix 6 |
| 20 m (14 MHz) | **Mix 6** (yellow) | Mix 6 | Mix 6 |
| 17 m (18 MHz) | Mix 6 or **Mix 17** | Mix 17 | Mix 17 |
| 15 m (21 MHz) | Mix 6 or **Mix 17** | Mix 17 | Mix 17 |
| 12 m (24 MHz) | **Mix 17** (blue/yellow) | Mix 17 | Mix 17 |
| 10 m (28 MHz) | **Mix 17** or Mix 10 | Mix 17 | Mix 17 |
| 6 m (50 MHz) | **Mix 10** (black) | Mix 10 | Mix 10 |
| 2 m (144 MHz) | **Mix 10 or 12** | air-core | Mix 12 / air |
| 70 cm (440 MHz) | **air-core** | air-core | air-core |

The amateur rule of thumb: **Mix 2 for 80/160 m, Mix 6 for 40/30/20 m, Mix 17 for 17/15/12/10 m, Mix 10 for 6 m and up.**

## Saturation — the high-power gotcha

Powdered iron *does not saturate* the way ferrite does, but it has a soft saturation limit set by the maximum allowable **flux density (B_max)**. Above B_max, the core's permeability drops, the inductance shifts, and the filter detunes.

The flux density formula (rule of thumb for amateur HF use):

```
B_max (gauss) = E × 10⁸ / (4.44 × A_e × N × f)

where:
  E = peak voltage across the inductor (V_peak)
  A_e = effective core cross-section (cm²)
  N = number of turns
  f = frequency (Hz)
```

Mix 2: keep B_max < 200 gauss. Mix 6: < 100 gauss. Mix 10: < 50 gauss.

The practical rule for amateur LPFs:

| Power level | Min toroid size (Mix 2/6) |
|-------------|---------------------------|
| QRP (5 W) | T-37 |
| 25 W | T-50 |
| 100 W | **T-68** for 80–40 m, **T-50** for 20 m and up |
| 500 W | **T-94** for 80–40 m, **T-68** for 20 m and up |
| 1500 W | **T-200-2** for 80–40 m, **T-130** for 20 m and up |

A T-50-2 will overheat in a 500 W LPF on 80 m. Always size up if the duty cycle is high (digital modes, RTTY) — halve the rated power for those.

## Where to buy

- **Amidon Associates** (amidoncorp.com) — the historical source, still ships
- **Kits And Parts** (kitsandparts.com, now Box Vault Industries) — popular QRP supplier
- **Mouser / Digi-Key** — stock Micrometals (the manufacturer) cores in bulk
- **eBay** — surplus toroids, sometimes mislabeled; check Q on a NanoVNA

Price (2026): T-50-6 ≈ $0.80 single, $0.40 in bulk. T-200-2 ≈ $5 single. Buy a hundred T-50-6 and a hundred T-37-6 and you're set for years of HF homebrew.

> ⚙️ **Advanced —** Some legendary amateur designs use **stacked toroids** — gluing two cores together to double A_L without doubling diameter. This works for inductors (the magnetic path is shared) but introduces a small inductance variation due to airgap between the cores. For critical filter alignment, use a single larger core (T-94 instead of two T-50s stacked) — the result is more predictable. Stacking is fine for non-critical inductors like RFC chokes.

## Common mistakes

- **Wrong mix.** A T-50-2 (red) in a 28 MHz filter has Q ≈ 60 instead of 200; the filter has 2 dB insertion loss. Always check the mix-vs-frequency table.
- **Wrong size for power.** Mentioned above — undersized toroids saturate and detune at full power.
- **Over-winding.** More turns ≠ more inductance proportional to N²; close-wound is more inductive than spread-wound on the same core because the windings couple to each other. Match the wind style of the reference design.
- **Treating ferrite cores as powdered iron.** An FT-50-43 is **not** a substitute for a T-50-2 in a filter. Verify the part number prefix before winding.

## See also

- [§26-06 — Ferrite Mix Selection](26-06-ferrite-mix-selection.md) — the broadband counterpart
- [§26-02 — Low-Pass Filters](26-02-low-pass-filters.md) — main consumer of these cores
- [§09-13 — Trap Design](../09-antenna-calc/09-13-trap-design.md) — antenna traps
- [§18-05 — Baluns & Chokes](../18-coax-connectors/18-05-baluns-chokes.md) — when *not* to use powdered iron
- [§17 — Formulas](../17-formulas/) — A_L and flux density math
