---
id: 06-32
title: VHF/UHF Antennas
chapter: 06
section: 32
level: mixed
status: published
---

# VHF/UHF Antennas

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Above 50 MHz the antenna game changes. Wavelengths shrink to inches, so a full-size antenna is small and cheap — but two things start to dominate that you could shrug off on HF: **feedline loss** (a run of RG-58 that's fine on 40 m can eat half your power at 440 MHz) and **polarization** (vertical for FM/repeaters, horizontal for weak-signal — mix them and you throw away ~20 dB). This section covers the antenna *families* you meet on 6 m, 2 m, and 70 cm; for the homebrew J-pole build and its calculator, see §09-09.

## Vertical omnidirectionals (FM / repeaters)

FM and repeater work is **vertically polarized and omnidirectional** — you want to hear and be heard in every direction. The ladder of choices, roughly by gain:

| Antenna | Gain | Notes |
|---------|------|-------|
| ¼-wave ground plane | ~unity (0 dBd) | The basic VHF vertical: a ¼λ whip over 3–4 drooping radials. Radials sloped ~45° raise the feed near 50 Ω |
| ½-wave (end-fed) | ~unity, slight edge | **Needs no ground plane** — the reason mag-mount and "no-ground-plane" mobile whips use it |
| 5/8-wave | ~3 dB over ¼λ | A base loading coil; lower take-off angle + gain; classic mobile and base vertical |
| Collinear (Diamond X-series etc.) | ~3–6 dBd | Several ½λ sections stacked in phase for omni gain — the tall white fiberglass base verticals |
| J-pole / Slim Jim | ~unity | ½λ radiator + ¼λ matching stub; the standard homebrew repeater antenna. Build & calculator in §09-09 |

> **Advanced —** Vertical "gain" is bought by squashing the pattern toward the horizon — a collinear's extra dB come from a *narrower elevation lobe*, not free energy. That's great for flat terrain and distant repeaters, but a high-gain vertical on a hilltop can actually *overshoot* a repeater in the valley below it, and its pattern is more sensitive to mounting height and nearby metal. On a hill, a unity-gain antenna sometimes outperforms a 6 dB collinear.

## Mobile antennas

- **¼-wave** — simplest; uses the vehicle body as its ground plane (center of the roof is best).
- **½-wave / 5/8-wave** — more gain and less ground-plane-dependence; NMO or mag-mount.
- **Mag-mount caveat:** the magnet doesn't make a DC ground — it **capacitively couples** to the roof sheet metal for an RF ground, which works fine above ~50 MHz. A ½-wave design sidesteps the issue by not needing a ground plane at all.
- **Dual-band (2 m / 70 cm)** whips are near-universal; the 70 cm pattern is the 3rd-harmonic behaviour of the 2 m design.

## Handheld antennas

The stock **"rubber duck"** is a heavily compromised helically-loaded short antenna — convenient, but several dB down on a real ¼-wave. The cheapest big upgrade in ham radio is swapping it for a **telescoping ¼-wave whip** or a **roll-up J-pole** thrown into a tree (§04-04): often a 3–6 dB improvement, the difference between making the repeater and not.

## Weak-signal (SSB / CW / digital) — go horizontal

6 m, 2 m, and 70 cm SSB/CW/MSK144/EME work is **horizontally polarized**, and directional. Here you want a **Yagi** (or quad / LPDA) — covered in §06-30 — pointed at the other station or the sky volume. Do **not** try to work horizontal SSB with a vertical: cross-polarization costs ~20 dB, enough to turn a workable signal into nothing. See §06-08.

## Wideband: the discone

A **discone** (a disc above a cone) is a vertical omni that works over a **huge** frequency range — often 25–1300 MHz — at roughly unity gain. It's the standard **scanner / wideband receive** antenna and can transmit across a wide span, the price being no gain. When you want one antenna to *hear everything*, this is it.

## Feedline is not an afterthought

At these frequencies coax loss is the quiet killer:

- A run of RG-58 that loses ~1 dB on 20 m can lose **3–5 dB per 100 ft at 440 MHz** — half your power or more, both ways.
- Use **low-loss coax** (LMR-400 / hardline) for any meaningful run, keep it short, and weatherproof every connector. See §18 (coax) and §11 (power budget).
- **Height and line-of-sight** dominate VHF/UHF range far more than antenna gain — get the antenna *up* and in the clear before chasing the last dB of gain.

## When to pick what

- **Repeaters / FM around town:** a ¼-wave or 5/8 vertical, or a collinear for more reach on flat ground.
- **Hilltop / SOTA portable:** a roll-up J-pole for FM, a small Yagi (§06-30) for SSB.
- **Weak-signal / EME / contests:** horizontal Yagis, stacked for gain (§06-27).
- **Scanning / wideband RX:** a discone.

## Common mistakes

- **Wrong polarization** — a vertical on horizontal SSB (or vice-versa) loses ~20 dB. Match the mode.
- **Cheap, lossy coax** on a long UHF run — the antenna can't make up what the feedline ate.
- **High-gain vertical on a hilltop** overshooting the valley repeater — sometimes unity gain wins.
- **Relying on the rubber duck** — the easiest upgrade you're not making.
- **Mag-mount on a fiberglass or plastic surface** — no metal to couple to, no RF ground; use a ½-wave or a real ground plane.

## See also

- §09-09 — J-Pole (the homebrew build and its calculator; Slim Jim, Copper Cactus variants)
- §06-30 — Beams (VHF/UHF Yagis, quads, LPDAs for weak-signal and tropo)
- §06-08 — Polarization (why vertical vs horizontal matters so much here)
- §06-12 — Verticals (the same ground-plane principles, scaled down)
- §18 — Coax & connectors (low-loss feedline matters most up here)
