---
id: 06-22
title: Fan Dipole (Parallel Dipole)
chapter: 06
section: 22
level: mixed
status: draft
---

# Fan Dipole (Parallel Dipole)

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A fan dipole is several half-wave dipoles — one per band — all joined at a single feedpoint and "fanned" outward so the wires separate as they leave the center. You feed it with one coax and one choke balun, no tuner and no traps. On any given band only the dipole cut for that band presents a low impedance; the others are so far off resonance that the feedline simply ignores them. It's the most efficient multi-band wire there is, because every band has a full-size resonant dipole of its own.

## How it works

All the dipoles share the same feedpoint, so the transmitter sees them in parallel. A 20 m signal sees the 20 m dipole at ~70 Ω (a good match) in parallel with the 40 m and 15 m dipoles, which at 14 MHz look like high, reactive impedances — effectively open circuits. The energy goes where the match is. Repeat per band.

| Property | Value |
|----------|-------|
| Bands | One full-size dipole per band — typically 40 / 20 / 15 / 10 |
| Feed | Single 1:1 current choke balun + coax |
| Tuner | Not required on the cut bands |
| Efficiency | Full dipole efficiency on every band — no trap or matching losses |
| Pattern | Each band radiates its own dipole figure-eight |

## Building and tuning one

The wires interact, so you tune from the **lowest band up** and expect iteration:

1. Cut all dipoles to the standard length (468 / f), each a little long.
2. Hang at final height with the wires fanned — spread the ends **2–4 ft apart** with spreaders so adjacent-band wires don't couple tightly.
3. Tune the **lowest band first** (trim that dipole), then the next band up, and so on.
4. Re-check the lower bands after tuning the higher ones — adjusting one wire pulls its neighbors slightly. Two or three passes converges.

> **Advanced —** The interaction is strongest between bands in a harmonic relationship — a 40 m dipole is near-resonant on 15 m (3rd harmonic), so a 40/20/15/10 fan couples 40↔15 noticeably. Wider end-spacing decouples them; so does staggering the wires in different planes (a true 3-D fan rather than a flat one). Mutual coupling is also why each added wire lowers every dipole's resonant frequency a little — always cut long and trim. EZNEC (§06-14) models the coupling well if you want to skip the iteration on the ground.

## Mechanical reality

A fan dipole is heavier and windier than any single wire — four dipoles means up to eight legs radiating from one center insulator, all needing end support or spreaders. The wires must **not tangle**: a flat fan in a single plane can slap together in wind and detune or short. Use rigid spreaders at the ends, or run the legs to separate support points so they hold their fan shape.

## When to pick a fan dipole

- You want **no-tuner, full-efficiency** operation on several bands and have the supports for a full-size multi-wire center.
- You don't want the losses of traps (§06-08) or the unun of an EFHW (§06-04).
- You have one good center support and room for the wires to spread.

## When to avoid

- Limited space or one support only — an EFHW or doublet is far easier to hang.
- You want a clean single wire — the fan is mechanically the fussiest dipole.
- You want WARC bands too — adding 30/17/12 m means three more wires and more coupling to fight.

## Common mistakes

- **Wires too close together** — tight coupling makes the bands un-tunable and shifts resonances unpredictably. Spread the ends.
- **Tuning the wrong order** — always lowest band first, then work up, then re-check.
- **One balun is fine; one wire isn't.** People forget the choke balun and get the usual feedline-radiation symptoms (§06-12).
- **Letting the legs sag together** — wind shorts or detunes them; use spreaders.

## See also

- §06-01 — Dipoles (each element is one)
- §06-08 — Traps (the other no-tuner multiband approach, with losses)
- §06-04 — EFHW (multiband from one wire instead of many)
- §06-14 — Modeling concepts (model the coupling before you climb)
- §06-12 — Baluns and chokes
