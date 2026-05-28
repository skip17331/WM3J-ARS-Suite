---
id: 29-09
title: Linear Transponder Strategy
chapter: 29
section: 09
level: advanced
status: draft
---

# Linear Transponder Strategy

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What a linear transponder is

A linear transponder relays everything inside a passband, not just one channel. AO-7, FO-29, RS-44, and most of the CAS-series Chinese cubesats carry linear birds — typically 30 to 60 kHz wide on V/U or U/V modes. Inside that passband dozens of QSOs can run simultaneously on different sub-frequencies, all sharing one bird.

The transponder is also **inverting** on almost every modern amateur satellite: an *increase* in your uplink frequency causes a *decrease* in your downlink frequency. The sideband flips too — you uplink LSB, the downlink comes out USB (or vice versa). This matters operationally because Doppler shift on uplink and downlink moves in opposite directions, so tracking is asymmetric (see §29-08 and below).

## The "find a quiet slot" workflow

You don't call CQ on a fixed frequency on a linear bird; you find a quiet spot inside the passband and call there. The workflow looks like this:

1. **Tune across the downlink passband during the pass**, listening for the carrier hash and existing QSOs. Linear-bird signals are weak and CW or SSB — there's never a moment of dead silence; you're looking for *less* activity, not none.
2. **Pick a gap of at least 3 kHz** from the nearest QSO.
3. **Compute the corresponding uplink frequency.** For an inverting bird with center 145.950 (uplink) / 435.900 (downlink, V/U inverting), if the downlink slot is 435.880 (20 kHz lower than center), your uplink is 145.970 (20 kHz higher than center — inversion).
4. **Set both VFOs** (rig handles split — see §22-08 for the general technique, §29-08 for Doppler automation).
5. **Call short**: `"CQ satellite from WM3J"` — one shot, listen for response on the downlink.

Operating power: **10 watts EIRP or less is standard**. Linear birds have limited transponder gain budget; one strong station can suck up all the bird's RF power and squash everyone else. The classic "alligator" of satellite work is the operator transmitting 100 W into a 13-element yagi — they hear themselves crystal clear and crush every other QSO on the bird.

## The inversion + Doppler asymmetry

On an inverting linear bird with Doppler:

- During approach (bird coming toward you): downlink shifts **up** in frequency, uplink shifts **up** in frequency at the bird (so you must shift your TX **down** to land on the same point in the passband).
- During recession (bird going away): downlink shifts **down**, you must shift TX **up**.

In practice the tracking software handles this — set the transponder type to "inverting" in SatPC32 / GPredict / MacDoppler and the rig CAT follows correctly. The trap is *manual tracking*: if you simply chase the downlink with the RX VFO and key up without adjusting TX, you drift across the passband during the pass, interfering with other QSOs as you go.

> **Advanced —** The "uplink ahead, downlink behind" mnemonic only works on non-inverting birds (which are rare). On an inverting bird, the relationship is reversed: as Doppler pushes the downlink up, the uplink must be pushed down by the same delta to stay locked on the same point in the passband. Most tracking software hides this and just keeps both VFOs aligned; the operator only needs to think about it when troubleshooting a misconfigured rig.

## Tracking other operators across the passband

A useful tactical move: when you spot another operator working someone in a quiet part of the passband, listen to the cadence and frequency drift. Many semi-automated satellite stations don't perfectly compensate Doppler and drift slowly across the passband as the pass progresses. If you tag along 2–3 kHz away, you stay in the same neighborhood without QRMing them — and when they finish, the slot is yours.

Conversely, if you hear a station drifting *into* your slot, give it up gracefully — the linear bird is shared real estate, and 30 seconds of patience beats a 30-second QRM exchange.

## Common linear-bird mistakes

- **Too much power.** 10 W EIRP feels weak when you're used to terrestrial QSOs, but it's plenty through the bird. Anything more and you're crushing the transponder AGC and degrading every other QSO on the bird.
- **Wrong sideband.** Inverting birds flip USB↔LSB. If you transmit USB on an inverting V/U bird, your audio comes out LSB on the downlink — sounds like Donald Duck to the other operator. Rig's "REV" or "inverted satellite mode" handles this.
- **Drifting through the passband.** Either run proper Doppler automation or check your downlink frequency every 30 seconds and re-center.
- **Calling on the center frequency.** That's the most-crowded slot. Aim 5–10 kHz away from center for cleaner conditions.
- **Not full-duplex.** You need to hear yourself on the downlink to verify you're actually transmitting into the bird (see §29-01). A half-duplex setup means you're calling blind.

## When linear birds are best for what

- **Long-haul DX:** FO-29 and AO-7 over polar paths give you EU-from-NA contacts that an FM bird can't manage.
- **CW operation:** linear birds with 50 W EIRP at QRP power give clean CW that beats noisy FM-bird CW (FM birds don't support CW well).
- **Multi-operator chasing:** since dozens of QSOs can run simultaneously, contests like AMSAT Field Day fill the linear passband end-to-end.
- **Practice for satellite contests:** RS-44 and FO-29 are forgiving birds for learning the Doppler/inversion choreography.

## See also

- [§29-01 Full-Duplex Operation](29-01-full-duplex.md) — non-negotiable for linear bird work
- [§29-02 Transponder Etiquette](29-02-transponder-etiquette.md) — the social contract
- [§29-08 Doppler Automation](29-08-doppler-automation.md) — handle the tracking complexity
- [§29-10 Mode Identification](29-10-mode-identification.md) — V/U vs U/V vs L/U etc.
- [§22-08 Split-Frequency Operation](../22-operating-practice/22-08-split-frequency.md) — terrestrial split is the conceptual template
- [§07-02 Doppler Shift](../07-satellites/07-02-doppler-shift.md) — the underlying physics
