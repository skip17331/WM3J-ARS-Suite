---
id: 14-03
title: Distorted Audio
chapter: 14
section: 03
level: simple
status: draft
---

# Distorted Audio

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

You can transmit. People hear you. But they describe your audio as muffled, harsh, clipped, distorted, or "having too much echo." Or you sound fine to yourself in the monitor but bad on the air. The most common station-level reason: **mic gain or compressor settings wrong**, often by a wide margin.

## The first move — get an honest report

Don't trust your own monitor; rigs almost always sound great in the monitor regardless of what they sound like on the air. Get reports from:

- **A friend on the air** — call them and ask how you sound.
- **A second receiver in the house** — a cheap SDR dongle a few rooms away, set to listen to your transmit frequency.
- **An online SDR (KiwiSDR or WebSDR)** — tune to your transmit frequency from a remote receiver. Note: pick one far enough away that you're going through real propagation, not direct local pickup.
- **Cluster spots / pskreporter** — see if your CW/digital signal is decoded correctly elsewhere.

Pin down what the audio actually sounds like before tweaking knobs.

## Common audio problems and their causes

### "You sound muffled / muddy"

- Mic too far from your mouth — try 1–2 inches from the corner of the mouth.
- Speech compressor off when you need it on (in noisy environments, compression helps clarity).
- Bass response too high — most rigs let you adjust mic EQ; rolling off below 200 Hz often helps.
- A "boomy" microphone (some studio condenser mics put out too much bass for SSB).

### "You sound harsh / tinny"

- Too much treble in the mic EQ — roll off above 3000 Hz.
- Mic too close to the mouth — proximity effect from a directional mic.
- Speech compressor set too aggressively — heavy compression makes consonants stand out unnaturally.

### "You sound like you're underwater" / "warbling"

- Audio sample rate mismatch in a software pipeline (digital modes setup with the wrong sample rate).
- USB audio glitches — USB cable is bad, or driver buffer underrun.
- An external sound card with a sticky audio buffer — try a different card or different driver.

### "Your audio is splattering / way too wide"

- Mic gain too high — you're driving the radio into ALC clipping.
- Speech compressor too high — the compressor is adding harmonics.
- Drive too high in digital modes — same problem (ALC pegged).
- Wrong audio source — feeding line-level into a mic-level input creates massive distortion.

### "I hear two voices / there's an echo"

- VOX is on and the system is hearing itself through the speaker (the speaker's audio is leaking back into the mic).
- Monitor is on with the audio routing set wrong — you're hearing yourself with delay.
- A linked repeater is delayed and you're hearing both the local and the linked transmission.

### "Your audio cuts in and out"

- Mic cable is intermittent.
- Mic PTT switch is sticky.
- VOX threshold is set too high for your speaking voice.
- Software-controlled audio is being interrupted (Discord/Zoom is stealing the mic).

### "Your audio is fine but the carrier is hot"

This is a CW issue, not SSB. The transmitter is producing audible AC hum on the carrier. Causes:
- Power supply ripple.
- Bad keying line capacitor on a homebrew transmitter.
- Old electrolytic capacitors in the transmitter itself.

## The mic gain / ALC interaction

This is where most "splattering" problems live.

The radio's PA stage has a maximum input drive level. The Automatic Level Control (ALC) reduces gain when input is too high. If mic gain is set so that you're constantly maxing the ALC:

- Audio peaks get clipped — this distorts the audio.
- Average power stays the same — increasing mic gain past the ALC threshold doesn't get you louder; it just adds distortion.

The right setting: **mic gain set so that voice peaks just barely tip the ALC into the marked range** (consult your rig's manual; usually a small marked zone at the lower end of the ALC scale). Whisper or shout — neither should cause more than a brief excursion above the marked range.

Many operators leave mic gain at "factory default" and assume it's right. It usually isn't, especially for SSB voice. Spend 5 minutes tuning it.

## The compressor

A speech processor / compressor reduces the dynamic range of your voice — quieter syllables get amplified, louder syllables get attenuated. The result is more average power on the air for the same peak.

Done right (~6–10 dB of compression), it makes you sound noticeably stronger on weak-signal paths.

Done wrong (15+ dB of compression), it sounds awful — pumping, breathing artifacts, and harmonic distortion.

For SSB DX work: 6–8 dB compression is typical.
For local rag-chewing: 0–3 dB or off.
For CW or digital: irrelevant; don't apply it.

## Microphone choice

The microphone matters more than most operators realize. A mic that's great for studio work may be wrong for HF SSB.

- **Heil mics** (HM-i series, Pro 7) — popular for ham use, designed with HF in mind, give good intelligibility.
- **The Yaesu MD-100 / MD-200, Icom HM-219 / SM-30, Kenwood MC-90** — manufacturer-branded ham mics, usually well-matched to their respective rigs.
- **Generic studio condenser** (Audio-Technica AT2020, etc.) — needs phantom power and an EQ tweak; some operators love them, some find them too "mid-range heavy."
- **Headset boom mic** — convenient for digital modes; quality varies wildly by brand.

If your audio reports are consistently bad, try a different mic before assuming the radio is broken.

## Audio chain block diagram

Visualizing helps:

```
Your voice
    ↓
Microphone (capsule)
    ↓ analog audio
Mic preamp (in the mic or in the rig) ← gain
    ↓
Speech compressor / processor          ← compression level
    ↓
Modulator
    ↓ RF
PA driver                              ← drive level
    ↓
PA finals                              ← ALC controls here
    ↓
Antenna
```

A problem at any stage shows up downstream. Walk the chain when troubleshooting.

> ⚙️ **Advanced —** Modern transceivers add several stages of optional processing inside the chain — TX equalizer, transmit DSP, second-stage compressor, RF speech clipper. Each can be misconfigured to sound great in your monitor and terrible on the air. The transmit-monitor function in many rigs samples the audio AFTER all DSP but BEFORE the final RF output, so it doesn't catch distortion introduced by ALC limiting or final-stage saturation. The only honest test is a remote receiver.

## See also

- §14-01 — no transmit (sometimes confused with very low audio)
- §14-04 — RF feedback (which can manifest as audio distortion too)
