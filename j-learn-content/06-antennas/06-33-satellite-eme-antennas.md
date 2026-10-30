---
id: 06-33
title: Satellite & EME Antennas
chapter: 06
section: 33
level: mixed
status: published
---

# Satellite & EME Antennas

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

Pointing an antenna at space pulls in two directions. A low-earth-orbit satellite is *close* and its signal is workable with modest gain — but it races across the whole sky in minutes and arrives with a scrambled polarization, so the antenna problem is **coverage and polarization**. The Moon is *predictable and slow*, but 250+ dB of path loss means the antenna problem is **raw gain** — as much as you can physically build. This section is the antenna-chapter view of both. The operating mechanics — Keplerian elements, tracking software, Doppler, modes, link budgets — are covered in depth in §07, §29, and §30-03, and pointed to (not repeated) below.

## Circular polarization — the satellite essential

A linearly-polarized antenna is the wrong tool for a satellite. A small spacecraft tumbles, and the ionosphere **Faraday-rotates** the polarization unpredictably on the way down — so a linear antenna sees the arriving polarization swing through every angle, with deep fades (often 20+ dB) every time it crosses perpendicular. **Circular polarization (CP)** fixes this: it accepts *any* linear polarization at a constant, gentle 3 dB penalty instead of deep nulls, giving steady copy through the pass.

- **Sense matters.** RHCP and LHCP are opposite; receive the wrong sense and you lose 20+ dB. Match the satellite's published sense — or use a switchable-sense antenna.
- **How CP is made:** crossed (turnstile) elements fed 90° out of phase, or a helix (which is circularly polarized by its geometry).

> **Advanced —** The figure of merit is **axial ratio** — the ratio of the polarization ellipse's major to minor axis. 0 dB is perfect CP; a purely linear antenna is infinite AR. A CP antenna receiving a linear signal of arbitrary orientation always captures half the power (−3 dB), which is why CP trades a fixed 3 dB for the elimination of multi-dB spin/Faraday fading. Two RHCP antennas in a link are matched; RHCP-to-LHCP is cross-polarized and deeply rejected. On 2 m EME, Faraday rotation over the round trip can leave you cross-polarized to your own echo — which is why serious 2 m stations switch H/V polarization or run CP (§30-03).

## Satellite antennas

The LEO menu, from no-effort to high-performance (the per-antenna detail lives in §29):

- **Handheld dual-band Yagi (Arrow / Elk)** — the canonical portable FM-bird antenna. Hand-aim it at the pass and twist it to track the polarization fades; 3–4 elements on 2 m, 6–7 on 70 cm. → §29-03.
- **Omni (eggbeater / turnstile, QFH)** — two crossed loops giving a hemispherical CP pattern. No rotor, no tracking, works FM birds from home; the trade is low gain that fades out at low elevation. → §29-04.
- **Helical** — circularly polarized by geometry, gain proportional to length; used standalone on 70 cm/23 cm or as a dish feed. → §29-05.
- **Crossed Yagis with switchable polarization** — the fixed-station performance answer: two Yagis 90° apart, phased for CP and switchable RHCP/LHCP, on an **az/el rotator**. → §29-06.

Downlink signals are weak, so a **mast-mounted preamp** (LNA at the antenna, before the feedline loss) is standard for any serious satellite station — §29-07.

## EME antennas — when only gain will do

EME is an exercise in gain (see the link budget in §30-03). Two paths:

- **Stacked long-Yagi arrays** — the 2 m / 70 cm workhorse. Four to eight (or more) long-boom Yagis on an H-frame, combined with a **phasing harness** (§06-27) and tracked in azimuth *and* elevation to about ±0.5°. Representative gains: **4× 17-element ≈ 22 dBi** (entry EME), **8× 22-element ≈ 26 dBi** (big station). Because Faraday rotation can leave you cross-polarized to your own echo on 2 m, many arrays use **cross-polarized (H/V switchable) Yagis** or CP.
- **Parabolic dishes** — the microwave path (23 cm and up). A **3 m dish ≈ 30 dBi at 1296 MHz**; 10 GHz EME uses 5–10 m dishes. Mechanically simpler than a giant Yagi array for the same gain, but the beam is *narrow* and pointing must be precise.

> **Advanced —** A dish needs the right **feed illumination**: a feed that under-illuminates wastes aperture (low gain), one that over-illuminates spills past the rim and picks up warm ground noise (high system temperature — fatal for EME RX). Match the feed pattern to the dish **f/D** (≈0.4–0.45 for prime-focus). At 23 cm the **septum feed** (e.g. the OK1DFC design) is popular because it produces clean CP from a single feed and lets you switch sense. Above all, on EME the **receive** side — low system noise temperature, a sub-0.5 dB mast-mounted LNA, and pointing at cold sky rather than warm horizon — matters as much as transmit gain.

## The operating side (covered in depth elsewhere)

These are *not* antenna topics, so they live in their own sections — go there for the real detail:

- **Orbits & Keplerian elements (TLEs)** — the numbers that describe a satellite's orbit: §07-03.
- **Tracking software** — SatPC32, Gpredict, MacDoppler and friends compute azimuth/elevation and Doppler from the TLEs and drive your rotor and rig: §07-04, §07-07, and automated Doppler in §29-08.
- **Doppler** — large on LEO downlinks and larger still at EME microwave frequencies: §07-02 and the correction tables in §07-08 for satellites, §30-03 for EME Doppler and libration.
- **Modes & operating** — FM vs linear, V/U·U/V mode identification, full-duplex, transponder etiquette: §07-01, §29-01, §29-02, §29-10.

## When to use what

- **Casual LEO FM** — a handheld Arrow on a pass, or an eggbeater at home for no-tracking operation.
- **Serious LEO / linear-transponder** — crossed Yagis with CP switching, az/el rotor, mast preamp.
- **EME on 2 m / 70 cm** — a stacked long-Yagi array, az/el tracking, LNA, 500 W–1.5 kW.
- **EME at 23 cm and up** — a parabolic dish with a septum or helical feed.

## Common mistakes

- **A linear antenna on satellites** — deep polarization fades; use CP.
- **Wrong CP sense** — ~20 dB down for no obvious reason.
- **No mast-mounted preamp / lossy coax** — UHF feedline loss is brutal and you simply won't hear the downlink (§18).
- **Under-built EME array** — below the link-budget threshold (§30-03) you never decode, no matter how patient you are.
- **Mispointing a narrow beam** — a big array or dish has a beamwidth of a few degrees; sloppy az/el tracking throws away the gain you built.

## See also

- §29 — Satellite Advanced (the per-antenna detail: Arrow, eggbeater, helical, polarization switching, preamps)
- §07 — Satellites (Keplerian elements, tracking, Doppler, pass prediction, footprints)
- §30-03 — EME Basics (link budget, equipment tiers, Q65, libration, Doppler)
- §06-30 — Beams (the Yagis an EME array is built from)
- §06-27 — Phasing harnesses & stacking (combining Yagis into the array)
- §06-08 — Polarization (linear vs circular, the satellite essential)
