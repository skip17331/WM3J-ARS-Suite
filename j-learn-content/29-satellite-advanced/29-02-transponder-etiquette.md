---
id: 29-02
title: Linear Transponder Etiquette
chapter: 29
section: 02
level: mixed
status: published
---

# Linear Transponder Etiquette

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A linear transponder is a shared resource — not in the FM-bird sense (one QSO at a time, take turns), but in the more nuanced sense of a 60 kHz passband where everyone is broadcasting and listening simultaneously. Your signal occupies a 2.5 kHz slot of SSB or a few hundred Hz of CW; the operator one kHz away is sharing the same transponder amplifier, the same downlink antenna, the same power budget. Bad etiquette on a linear sat doesn't ruin one QSO — it ruins every QSO inside that 60 kHz that's near you in frequency or louder than you in level.

Linear birds get **stricter etiquette than FM birds** because the consequences of bad behavior propagate. On an FM bird, an over-driving operator simply captures the bird for the duration of their transmission and the next operator's QSO starts fresh. On a linear bird, an over-driving operator robs everybody else of downlink power for as long as they're transmitting — and on a 15-minute pass with 20 simultaneous QSOs, that's a lot of damage.

## The core rules

### 1. Keep contacts brief

On FO-29 or RS-44 with a 10-12 minute pass and 60 kHz of passband, the bird can comfortably support 15-20 simultaneous QSOs. The expectation is the same as on an FM bird: callsign, grid, signal report, 73. Maybe two or three sentence exchanges if you have something specific to say. Not a ragchew.

If you want to chat, work the QSO, exchange contact info (email, Discord, club), and move the conversation off the satellite. The transponder is a connection mechanism, not a meeting place.

### 2. Listen first

Tune across the entire downlink passband before transmitting. You're looking for:

- **Active QSOs** — clusters of signals you should avoid landing on.
- **Beacons** — most linear sats have a downlink beacon (CW telemetry, usually at one end of the passband). Don't transmit there.
- **A quiet slot** — somewhere with no current activity where you can call CQ.

A linear transponder isn't a single channel. It's a passband with structure. Treat it like tuning across the 20-meter SSB band — find an empty spot before you key up.

### 3. Don't camp on one frequency

On an FM bird, the operating slot is "the bird." Anyone working the bird is on the same frequency as you. On a linear bird, your slot is wherever you decided to call CQ. If you sit there for the entire pass, you're occupying the same 2.5 kHz of passband repeatedly, displacing other operators who might want to use that slot.

Convention: work two or three QSOs from one slot, then QSY a few kHz. This rotates who's where and gives the pass dynamics. Camping at, say, 145.985 MHz downlink for an entire 10-minute pass is the linear-transponder equivalent of standing in the middle of a doorway.

### 4. Don't ragchew during a pass

Even if the bird isn't busy and you have your friend on the other end of a 12-minute pass to yourself, save the ragchew. The bird isn't your private repeater. Other operators may be lurking on the downlink waiting for a quiet moment to call CQ; if you're holding the spectrum hostage with a Q-and-A about your radio collection, you're keeping them off.

Make the contact, exchange grids/reports, sign off, QSY. If the friend is also on the next pass an hour later, talk on terrestrial 2 m or a Discord voice channel in the meantime.

## Signal-level discipline — the alligator problem

This is where linear transponder etiquette gets technical, and where most violations happen unintentionally.

**The alligator** is an operator with a big TX antenna and weak RX — "big mouth, small ears." They can blast the satellite uplink at a level that captures the transponder's automatic gain control (AGC) — but they have a weak downlink antenna and can't hear themselves at the same level. They keep cranking up the uplink power trying to make themselves louder on a downlink they can't even hear properly, and meanwhile every other operator on the bird is being suppressed by their overdrive.

The diagnostic test:

- **Your downlink signal should be about the same loudness as the satellite's downlink beacon** (the bird's built-in telemetry transmission). The beacon represents the transponder running at a known reference level. If you're significantly louder than the beacon, you're overdriving the bird.

The fix:

- **Reduce uplink power until your downlink is at beacon level or below.** Typical linear birds need 5-25 W from a modest Yagi — *not* 100 W from a big Yagi. The IC-9700 default of 50 W is too much for most linear satellite work.
- **If you can't hear yourself**, fix the receive side before you transmit. Better downlink antenna, mast preamp (§29-07), better aiming. Don't transmit blind into a transponder hoping you're on frequency.

> **Advanced —** The transponder's AGC isn't actually capturing in the FM sense — the transponder is a linear amplifier and outputs the sum of all uplink signals, weighted by their relative input power. But the **total downlink power** is capped (usually at a few watts EIRP). When one operator's uplink is 20 dB stronger than the median, that operator's downlink is correspondingly louder, and the rest are correspondingly weaker — fixed-gain bird, fixed power budget, the math works out to direct competition for downlink share. On FO-29 the practical effect is that one alligator can suppress everyone else by 6-10 dB.

## Sideband and frequency conventions

Most amateur linear transponders are **inverting** — see §29-09 for the details. The etiquette implications:

- **Run the inverted sideband.** Most operators use **LSB up / USB down**. If you run USB up on an inverting transponder, your downlink is LSB, but most operators are tuned for USB downlink — they'll hear you as scrambled audio. Worse, they'll have to retune their rig to listen to you, which is rude.
- **Doppler-correct your uplink, not your downlink.** Tuning your uplink keeps your downlink at a fixed apparent frequency. Other operators tuned to your downlink slot can follow you across the pass without retuning their radio.

These conventions exist so multiple operators can find each other reliably during a pass. Breaking them — running the wrong sideband, tuning the wrong end for Doppler — works for you but makes it harder for everyone else.

## Common etiquette failures

- **Transmitting on the bird's beacon frequency.** Cardinal sin. Easy to do if you haven't listened first.
- **Asking the other station to QSY 2 kHz mid-QSO.** Wastes pass time and crosses into another operator's slot.
- **Calling CQ on top of an existing QSO** because "the bird is busy and I want my contact." Wait for an empty slot.
- **Repeating your callsign three times when you're already in QSO.** Brief is better — you've got 8 minutes left in the pass; somebody else wants their turn.
- **Cursing or commenting when something goes wrong.** The whole bird hears you. The whole bird remembers.

## See also

- §29-01 — Full-duplex operation (prerequisite for hearing yourself)
- §29-09 — Linear transponder strategy
- §07-01 — FM vs linear satellites
- §22-05 — Pile-up etiquette (the general principles)
