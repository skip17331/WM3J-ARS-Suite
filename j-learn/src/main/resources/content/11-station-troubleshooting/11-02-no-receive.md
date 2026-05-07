---
id: 11-02
title: No Receive
chapter: 11
section: 02
level: simple
status: draft
---

# No Receive

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The radio is on. The display lights up. But you don't hear anything — no static, no noise floor, nothing. Or you hear noise but no signals. Or you hear signals but they're impossibly weak.

The good news: receive problems are usually easier to diagnose than transmit problems because they don't risk damaging anything if you keep poking.

## Step 1 — Is there ANY noise at all?

Tune to a known-busy frequency (40 m around 7.200 MHz at night, or 20 m around 14.250 MHz during the day). Turn squelch off. Turn AF gain to 50%.

- **Total silence, no hiss:** Audio chain dead. Likely speaker, headphones, mute, or AF gain at zero.
- **Hiss but no signals:** RF chain dead from antenna to demodulator. Continue.
- **Hiss and a few weak signals:** Receiver mostly works but is much less sensitive than usual. Continue.
- **Normal noise floor and signals:** Receiver works fine. The problem is somewhere else (band conditions, antenna pointing the wrong direction).

## Step 2 — Audio chain check (silence case)

If you hear *nothing*:

- **AF gain at zero?** Turn it up.
- **Mute / squelch on?** Turn squelch fully counter-clockwise (open).
- **Headphones plugged in but volume up?** Some rigs mute the speaker when headphones are present.
- **External speaker selected but not connected?** Some rigs let you route audio to an external speaker port; if nothing's connected there, no audio.
- **AF mute on?** Some software-controlled rigs have an AF mute that's separate from squelch.
- **DSP / digital audio routing wrong?** If you're listening through a sound card (digital modes setup), the routing in the OS may be sending audio to the wrong device.

If still silent, listen at the headphones jack with a known-good set of phones. If the jack works but the speaker doesn't, the internal speaker amplifier is bad.

## Step 3 — RF chain check (hiss but no signals)

If you hear hiss but no signals:

- **Wrong band selected?** Modes that look the same on the display can be subtly different (some rigs let you set CW mode while displaying USB; the radio is then rejecting the signal you expect).
- **Antenna selector on the wrong port?** Same as TX — check ANT 1 vs ANT 2.
- **Attenuator engaged?** Many rigs have a 10–20 dB front-end attenuator for strong-signal environments. If it's stuck on, all signals are 10–20 dB weaker than they should be.
- **Preamp off when needed?** On 6 m and 2 m at quiet locations, the preamp adds 10–15 dB of front-end gain. Without it, signals you expect to hear may be below the noise floor.
- **NB (noise blanker) too aggressive?** A noise blanker set wrong can chop signals into chunks that don't decode.
- **DSP filter too narrow?** A filter set to 50 Hz width receives only signals exactly on frequency.
- **RIT or XIT on?** Receiver Incremental Tuning shifts the RX frequency away from the displayed value. Check the RIT control.

Try **listening to broadcast stations on a non-amateur frequency** (BBC at 9.410 or 12.095 MHz, WWV at 5.000 / 10.000 / 15.000 MHz). If you can hear those, the receiver works; the problem is band conditions or antenna direction.

## Step 4 — Antenna chain check

If the receiver works on broadcast frequencies but not on amateur:

- **Antenna selector?** (Mentioned above; worth checking again.)
- **Lightning arrestor faulty?** Disconnect at the entry point and bypass to test.
- **Coax bad?** Test with a known-good antenna or dummy load. If signals appear with a dummy load (you'll hear noise but no real signals), the coax is suspect.
- **Antenna fallen down?** Walk outside and look. A dipole leg can come loose from a tree branch and not be obvious from the ground.
- **Wrong polarization?** A horizontal antenna receives a vertically-polarized signal 20+ dB weaker than the same signal on a vertical antenna. If you put up a new antenna and switched from horizontal to vertical, expect a different signal landscape.

## Step 5 — Common cause: audio routing for digital modes

Operators running digital modes often route audio through a sound card. If anything in that path is wrong:

- The radio's audio is muted to the speaker (rig set to "USB AF" only).
- The sound card is muted in the OS mixer.
- The wrong sound card device is selected.
- The PTT-via-USB has the audio routing set to the wrong device.

If you set up for FT8 last week and now you can't hear anything on SSB, this is probably the cause. Reset the radio's audio routing to "speaker on" / "front jack" / however your rig defaults.

## Step 6 — Receiver desensitization (RX intermod)

Sometimes a strong nearby station overloads your receiver's front end, blocking everything in a wide range. This is more common on VHF/UHF near commercial transmitters but can happen on HF too.

Symptoms: receiver works fine when you turn on the attenuator (which proves the front end is overloaded). Restoring full sensitivity by turning the attenuator off makes it deaf again because of the overload.

Fix:
- Use the attenuator deliberately when the cause is unavoidable.
- Add a band-pass filter (a tunable preselector or a fixed BPF) ahead of the receiver to reject the offending signal.
- For VHF/UHF, add a notch filter for the specific offender (e.g., a paging system at 154 MHz that desenses your 144 MHz receiver).

## Step 7 — Is the receiver actually working?

If everything else checks out:

- Connect the radio to a friend's antenna or take it to a club station. If it receives there, your station has the problem.
- Connect a signal generator (or a low-power transmitter on an isolated band) at the antenna jack. If the receiver hears the signal at the expected level, the receiver works.
- Bring the radio to a service shop if you've ruled out everything outside the radio.

## When the receiver intermittently dies

Intermittent receive problems are the worst:

- **Cold solder joint on a connector** — temperature-sensitive, fails after warm-up.
- **Power supply ripple** — adds noise to the RX, sometimes makes weak signals undetectable.
- **CAT / USB cable shorting** — disconnect everything except the antenna and try again.
- **Internal corrosion** on the antenna BNC inside the radio — clean with isopropyl.

Document the conditions when it fails (weather, time, what you were doing) — pattern recognition is the path to a repair.

## See also

- §11-01 — no transmit (related diagnostic flow)
- §11-04 — RF feedback (sometimes shows up as receiver weirdness)
- §13, §14 — noise sources (cause of "I hear noise but no signals")
- §10 — high SWR (the antenna problem you might have been blaming)
