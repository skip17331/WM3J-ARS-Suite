---
id: 06-23
title: Sloper and Half-Sloper
chapter: 06
section: 23
level: mixed
status: published
---

# Sloper and Half-Sloper

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Two different antennas share the "sloper" name, and confusing them is the source of most disappointment. The **full sloper** is an ordinary half-wave dipole hung at a slant — predictable, well-behaved, mildly directional. The **half-sloper** is a *quarter-wave* wire sloping off a grounded tower that uses the tower as its other half — a genuinely useful low-band DX antenna, but notoriously finicky to match because the tower (and everything mounted on it) is part of the antenna.

## The full sloper

A half-wave dipole with one end up high and the other end down low, fed at the center as usual. Standard dipole length (468 / f), standard ~70 Ω feed, standard 1:1 choke.

- **Pattern:** the slant mixes horizontal and vertical polarization, tilting the pattern to favor the **down-slope direction** at lower angles than a flat dipole — useful for aiming some low-angle DX response one way.
- **Directivity:** modest, a few dB favoring the slope direction. Don't expect a beam.
- **Why use it:** when you have one tall support and the geometry of a slant fits the lot better than a flat-top, or you want a little low-angle bias toward a favored direction.
- A slope angle of **30–45° from horizontal** is the usual practical range.

It's just a dipole; feed and tune it like one (§06-10).

## The half-sloper (quarter-wave sloper)

This is the interesting one. A **single quarter-wave wire** slopes down from near the top of a **grounded** metal tower. You feed it at the top: **coax center to the sloping wire, coax shield to the tower**. The tower itself acts as the return — the "other half" of the antenna.

| Property | Typical value |
|----------|---------------|
| Sloping wire | ~quarter-wave (234 / f feet), trimmed in place |
| Tower | Grounded, ideally ~λ/8 to λ/4 tall at the operating band |
| Feed impedance | Anywhere from ~10 Ω to over 100 Ω — depends on the install |
| Bands | Single band per wire; classic on 160 / 80 / 40 m |

When it works, a half-sloper is a compact, low-angle DX antenna that reuses a tower you already have. When it *doesn't*, you cannot get the SWR down and no amount of wire trimming fixes it.

> **Advanced —** The half-sloper's match depends on tower height in wavelengths, the slope angle, the ground system at the tower base, and — critically — whatever else is on the tower (the rotator, the Yagi and its feedline, guy wires). All of it is part of the radiating/return structure. There is no reliable formula; the feedpoint impedance is set by the boundary conditions of *your* specific tower. The practical method is empirical: set the slope angle (~45°), trim the wire for minimum reactance, then vary the **attachment height down the tower** a few feet at a time to find where the resistive part lands near 50 Ω. Some towers simply never present a good match, which is why the half-sloper has both devoted fans and people who swear it's broken.

## Tuning a half-sloper

1. Attach the wire near the top, slope it at ~45° to a low support.
2. Sweep: trim the wire to move resonance, *and* move the tower attachment point up/down to move the resistance toward 50 Ω.
3. Expect to chase both at once. A 1.5:1 result is a good day; some towers won't beat 2.5:1.
4. Choke the feedline (§06-04) — common-mode current is part of why it's twitchy.

## When to pick each

- **Full sloper** — you want a dependable dipole with a slight low-angle, directional bias and a slanted geometry fits your supports.
- **Half-sloper** — you have a *grounded* tower with HF antennas on it and want a low-band DX wire almost for free, and you're willing to experiment to get the match.

## When to avoid

- No grounded tower of suitable height → the half-sloper has nothing to work against; use a vertical or inverted-L (§06-25) instead.
- You need a guaranteed, repeatable match → the half-sloper is empirical by nature.

## Common mistakes

- **Calling a half-sloper a dipole.** It's a quarter-wave against the tower — half the wire, totally different feed.
- **Expecting a formula** for the half-sloper match. There isn't one; it's trial and error per tower.
- **Ungrounded tower** under a half-sloper — the return path is undefined and it won't load.
- **No feedline choke** — worsens the already-touchy match.

## See also

- §06-10 — Dipoles (the full sloper is one)
- §06-25 — Inverted-L (the other "one support, low-band DX" wire)
- §06-12 — Verticals (the half-sloper is a tower-fed vertical relative)
- §06-28 — Shunt-fed towers (the grounded tower fed as a full vertical, not a counterpoise)
- §06-05 — Ground-plane effects (why the tower base ground matters)
- §06-04 — Baluns and chokes
