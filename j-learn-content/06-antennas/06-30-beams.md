---
id: 06-30
title: Beams — Yagis, Quads & Log-Periodics
chapter: 06
section: 30
level: mixed
status: published
---

# Beams — Yagis, Quads & Log-Periodics

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A beam concentrates your power in one direction instead of spraying it everywhere. Where the phased arrays of §06-29 steer a pattern by driving every element, a beam gets its directivity mostly from **parasitic** elements — undriven conductors that re-radiate the field with a phase that reinforces forward and cancels backward. Point it at the DX and you gain both ways: more signal out *and* a quieter receive pattern, with the back and sides rejected.

## How a parasitic beam works

One **driven element** (a dipole) is fed; the others are parasitic, coupled only through the field:

- A **reflector** — slightly *longer* (~5%) than the driven element, placed *behind* it — looks inductive and re-radiates with a phase that reinforces forward.
- One or more **directors** — slightly *shorter*, placed *in front* — look capacitive and pull the pattern forward.

The result is a unidirectional pattern with forward gain and a front-to-back ratio. This is the **Yagi-Uda**, by far the most common HF/VHF beam.

## Gain and the boom-length rule

Gain is set mainly by **boom length in wavelengths**, not raw element count:

| Yagi | Typical gain | Front-to-back |
|------|--------------|----------------|
| 2-element (driven + reflector) | ~3–4 dBd (5–6 dBi) | 10–15 dB |
| 3-element | ~5–6 dBd (7–8 dBi) | 15–25 dB |
| Long-boom (5–6+ el) | 8–10+ dBd | 20–30 dB |

Adding directors on a longer boom keeps adding gain, but with diminishing returns — roughly **+2.5 dB per doubling of boom length**. You cannot maximize gain, front-to-back, *and* SWR bandwidth at once; every Yagi design is a compromise among the three.

> **Advanced —** Parasitic coupling drops the driven element's feedpoint impedance well below a free-space dipole's 70 Ω — a close-spaced monoband Yagi driven element can sit at **15–25 Ω**. That's why the driven element is rarely fed directly: it uses a **gamma match**, a **hairpin/beta match**, or a **folded-dipole driven element** (§06-18, whose 4× step-up brings a 12–15 Ω driven element back toward 50 Ω). Matching and resonance are separate adjustments — a Yagi can be resonant and still show 2:1 because the resistive part isn't 50 Ω. See §06-03.

## Quads

A **quad** replaces the Yagi's linear elements with full-wave **loops** (§06-15). For a given boom length a quad yields roughly **1–2 dB more gain** than a Yagi, runs a little **quieter** (the DC-grounded loop sheds precipitation static), and often holds a better front-to-back. The cost is bulk and mechanical complexity — spreaders, more wind sail, harder to rotate and survive storms. A 2-element quad is roughly a 3-element Yagi.

## Log-periodic dipole arrays (LPDA)

An **LPDA** is *all* elements driven, through a phase-reversing feedline run along the boom, with element lengths and spacings scaled by a constant ratio. That makes it **broadband** — one antenna covering, say, 13–30 MHz continuously, including the WARC bands. The trade is size and gain: only two or three elements are active at any one frequency, so an LPDA gives **~6–7 dBi** — less than a monoband Yagi of similar size — and needs a balun at its feedpoint. The choice when you want *one* rotatable antenna across a wide range and will trade some gain for coverage.

## Multiband Yagis

- **Trapped tribanders** — traps (§06-17) electrically shorten elements so one boom works 20/15/10 m. Convenient; traps add loss and narrow bandwidth.
- **Interlaced monobanders** — separate full-size element sets on one boom. More gain and bandwidth, heavier and pricier.
- For stacking two beams for more gain and a lower take-off angle, see §06-27.

## Practical reality

- **Height beats elements** on HF. A modest tribander up high outperforms a big array down low; see §06-05.
- **Wind load and rotator torque** scale fast with boom length and element count — spec the rotator and mast for the antenna, not the other way around.
- **SWR bandwidth** is narrow on long-boom monobanders; tune for your busiest part of the band.

## When to pick a beam

- You want gain and directivity on the high bands (20–10 m, 6 m, VHF) and can put up a rotatable structure.
- DXing or contesting where front-to-back and forward gain win QSOs.

## When to avoid

- Low bands (160/80 m), where a full-size beam is impractically large — use verticals/phased arrays (§06-12, §06-29) instead.
- No rotator/tower budget, or HOA limits — a wire antenna is the realistic path.

## Common mistakes

- **Feeding the driven element directly** and fighting 2:1 SWR — it's a low-impedance element; use the gamma/hairpin/folded-dipole match.
- **No common-mode choke** at the feedpoint — pattern distortion and feedline radiation (§06-04).
- **Mounting too low** — kills the height advantage that matters more than an extra element.
- **Chasing gain, F/B, and bandwidth together** — pick the two that matter for your operating.

## See also

- §06-29 — Phased arrays (the *driven* route to directivity; contrast with parasitic)
- §06-18 — Folded dipole (the classic Yagi driven element)
- §06-15 — Full-wave loops (the quad's elements)
- §06-03 — Impedance transformation (gamma / hairpin / beta matches)
- §06-27 — Phasing harnesses & stacking (stacking beams for more gain)
- §06-33 — Satellite & EME antennas (stacked-Yagi arrays for moonbounce)
- §06-05 — Ground-plane effects (why height wins)
