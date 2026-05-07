---
id: 11-04
title: RF Feedback
chapter: 11
section: 04
level: mixed
status: draft
---

# RF Feedback

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

RF feedback happens when transmitted RF energy gets back into the radio's input or audio chain instead of going to the antenna. Common-mode current on the coax is the usual root cause. The result: garbled audio, raspy SSB, sometimes distortion that varies with how you're touching the equipment.

## Symptoms that point at RF feedback

- **Audio reports get worse at higher power levels.** Low power = OK; full power = harsh / distorted / breaks up.
- **Touching the rig case changes your SWR or audio reports.**
- **You get an RF tingle or burn touching the rig case during TX.**
- **Audio cables physically pick up RF** — touching the cable shield mid-transmission changes the audio quality.
- **Shack lights flicker, monitors flicker, computer audio glitches** during TX.
- **RFI in the house gear** (TVs, stereos) that's worse on higher frequency bands.

These are all common-mode current problems. The cure is shrinking the common-mode current.

## What's happening physically

In a clean station, RF goes:

```
Radio → coax (inside) → antenna → into the air
```

In a feedback-prone station:

```
Radio → coax (inside) → antenna 
                        ↓
                    coax (outside shield)
                        ↓
                    back to radio chassis
                        ↓
                    audio chain, PSU, control lines
                        ↓
                    re-enters microphone or audio input
                        ↓
                    re-amplified, re-transmitted, distorted
```

The RF on the outside of the coax shield finds its way back to the radio because the shield is grounded to the radio chassis. The chassis becomes "hot" with RF; that RF couples into anything connected.

## Step 1 — Confirm common-mode current is the problem

Two quick tests:

### Test A: Touch the equipment

At low power (5 W), transmit a steady carrier. Touch the rig chassis. Does the SWR meter change? If yes, common-mode current is real.

Touch the audio cable shield. Does the audio change (have someone listening)? If yes, the audio chain is picking up RF.

Don't do this at full power — RF burns are real.

### Test B: Add a temporary choke

Wind 6–8 turns of the coax around a small toroid (FT-240-43 is the workhorse) at the radio end of the coax. Operate again. If the symptoms reduce significantly, you've confirmed common-mode is the issue and you have a permanent fix path.

## Step 2 — Add chokes systematically

In rough order of payoff:

### Choke at the antenna feedpoint

The biggest impact for the smallest effort. A 1:1 current balun (or a homemade ferrite choke) installed AT the antenna's feedpoint forces equal currents into the two antenna sides and minimizes shield current induction.

### Choke at the rig end of the coax

Inside the shack, just before the coax enters the rig. This blocks any common-mode that survived the feedpoint choke from reaching the rig.

### Chokes on every cable that enters the rig

Power, mic, headphones, speaker, USB, CAT, audio-out — all of them. Each needs a couple of turns through a small ferrite (FT-114-43 or snap-on Type 31 cores).

### Choke on the AC power cord

Especially helpful if the AC ground is the path back. A snap-on choke at the rig end of the AC cord, plus another at the wall.

## Step 3 — Improve the station ground

If chokes alone don't fully solve it, the station may need a better RF ground:

- A **single-point ground** at the rig: a copper bus bar with all equipment grounded to it.
- A short, thick wire from the bus bar to a ground rod outside (single-point connection — don't ground each piece of equipment to a different rod).
- All shack equipment connected to that single bus.

This is covered in detail in §11-05.

## Step 4 — Check audio cable shielding

Cheap audio cables (especially those with foil shielding instead of braided) leak RF. Replace any cable that's part of the audio chain with a quality braided-shield cable, ideally with twisted-pair conductors inside.

For mic cables and CW key cables, **twisted pair inside a shielded jacket** is the gold standard. Each pair carries balanced audio; the shield carries no signal current and stays at chassis potential.

## Step 5 — Eliminate ground loops

Ground loops happen when a cable runs between two pieces of equipment that are each grounded separately. The two grounds aren't at the same potential (especially during TX), and current flows through the cable shield.

Symptoms: 60 Hz hum on RX (separate problem), audio buzz that varies with power, persistent low-level distortion.

Fixes:
- **Lift the audio ground** at one end of an interconnect cable using a 1:1 audio isolation transformer. Common in computer-radio interfaces.
- **Common ground all equipment** to the single-point ground bus mentioned above.
- **Use galvanically isolated USB** (a USB isolator like the one made by Coral Reef) for computer-to-radio data connections.

## When chokes don't fix it

If you've added chokes everywhere and the problem persists:

- **Check if the antenna design needs common-mode current** to function (some EFHWs use the coax shield as part of the radiator; aggressively choking it makes them worse, not better).
- **Re-route the coax away from your shack** — a coax run past the side of the house is a longer feedline on the outside than necessary.
- **Use a remote autotuner** at the antenna instead of in the shack — keeps the high-SWR section confined outside.
- **Switch antenna styles** — if you're using a poorly-fed long-wire that's begging for common-mode trouble, a balanced loop or properly choked dipole solves it.

> ⚙️ **Advanced —** The choke effectiveness can be characterized with a NanoVNA: place the choke between two short coax stubs into the test ports, sweep, and measure the common-mode insertion loss versus frequency. A good HF choke shows >30 dB CM rejection from 1.8 to 30 MHz. Single-toroid chokes typically don't reach 30 dB across the whole HF range; you need a stack of 2–3 toroids or an air-core "ugly balun" with many more turns. Field-built chokes are easier than people expect, but don't guess at adequacy — measure.

## See also

- §10-06 — feedline routing (closely related)
- §10-05 — faulty balun (a balun is a deliberate common-mode choke)
- §11-05 — grounding
- §12 — RFI (common-mode is the #1 RFI cause)
