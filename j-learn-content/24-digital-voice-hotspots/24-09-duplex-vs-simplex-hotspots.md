---
id: 24-09
title: Duplex vs Simplex Hotspots
chapter: 24
section: 09
level: mixed
status: published
---

# Duplex vs Simplex Hotspots

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What the distinction means

A **simplex hotspot** uses one frequency for both TX and RX. The radio side is half-duplex: at any instant, the hotspot is either *transmitting* to your handheld or *listening* for your handheld's transmission, but never both. Your radio operates similarly — listening to the hotspot's transmissions, then keying to send.

A **duplex hotspot** uses two frequencies — one for TX and one for RX, separated by a standard repeater offset (typically 5 MHz on 70 cm in the US). The hotspot transmits and receives simultaneously, exactly like a real repeater. Your radio is set up with a repeater-style channel pair, with appropriate TX offset.

```
   SIMPLEX hotspot
   ───────────────
   438.800 MHz  ◀──── hotspot transmits to your radio  ◀──── network audio in
                ────▶ hotspot receives your transmission ────▶ network out

   (the hotspot's radio chip toggles direction)

   DUPLEX hotspot
   ──────────────
   438.800 MHz (RX) ◀──── your radio's TX
   433.800 MHz (TX) ────▶ your radio's RX

   (two independent radio paths, both active simultaneously)
```

## Why simplex is the cheap default

Simplex hotspots are about half the price of duplex hotspots because they need only one radio chip, one antenna, one filter chain. They're physically smaller, run cooler, draw less power.

For 90% of casual DV operating, simplex is functionally identical to duplex from the user's perspective. You key your radio, your handheld stops listening, the hotspot stops transmitting, your audio goes to the hotspot, audio gets relayed to the network, you release PTT, the hotspot starts transmitting again with whatever the network is sending you. It's just half-duplex turn-taking, no different from how a normal simplex QSO works.

For the dominant DV use case — short conversational exchanges with brief PTT presses — simplex is fine.

## When duplex actually matters

A handful of scenarios where duplex changes the experience:

### Long-form conversations and breaking in

On a simplex hotspot, your radio cannot hear network audio while you're transmitting. If you're 30 seconds into a long-winded explanation and the person on the other end says "wait, you've got that backward" — you can't hear them say it. You'll finish your sentence, release PTT, and only then discover they were trying to interrupt.

On a duplex hotspot, your radio can listen on the TX-out frequency while your radio is keyed on the TX-in frequency. If the hotspot starts emitting network audio while you're still keyed, you'll hear it. You can stop talking, drop PTT, and respond.

This matters most in:

- Multi-station roundtables on a busy TG/reflector.
- Net check-ins where the net control may need to break in to redirect.
- D-STAR callsign-routed conversations with rapid back-and-forth.

### Repeater-style operating habit

If you're already used to operating through repeaters (with the natural assumption that the repeater is always transmitting, with a courtesy tone telling you when to begin), a duplex hotspot feels familiar. A simplex hotspot doesn't — there's no courtesy tone, no "kerchunk recovery" feeling, just "is the channel quiet?" judged by what your radio's speaker is doing.

### Listening while parked

Set a duplex hotspot to monitor a TG. Walk around the house with your HT. You hear continuous audio whenever the TG is active. Your HT is in regular *receive* mode, listening to the hotspot's TX frequency. You can hear conversations without paying attention.

With a simplex hotspot, the same setup works — the hotspot transmits the TG's audio out to your HT — but if you ever key your HT, you immediately blank out everything until you stop keying. With duplex you can briefly check in mid-conversation without dropping out.

### Working multiple TGs / reflectors at once

If you've configured static talkgroups on a duplex hotspot, *any* incoming TG audio plays out the TX side, while you can independently key to a different TG. A simplex hotspot has to time-share — it can't be playing TG 91 audio while also receiving your TG 3100 transmission.

## What duplex doesn't buy you

- **More RF range.** Both are 10–20 mW. The duplexer doesn't extend coverage.
- **Fundamentally different network behavior.** From the network's perspective, simplex and duplex hotspots look identical.
- **Better audio quality** of any single QSO. Codec, BER, and network hops are unchanged.
- **More frequency stability.** Both are subject to the same TCXO drift issues.

## Cost vs benefit — a rough rule

For most casual DV users (~10 minutes of operating per day, mostly listening to a TG and occasionally checking in), **simplex is fine**. The $80–$100 you save buying simplex pays for the rest of your codeplug-programming time.

For operators who:
- Spend hours per day on networks
- Do nets or roundtables
- Want a hotspot to behave like a "real" repeater
- Are running multiple TG static subscriptions

**Duplex is worth the extra ~$50–$80.**

A typical duplex Pi-Star build costs $120–$180 vs $50–$80 simplex. OpenSpot 4 vs 4 Pro is $280 vs $330 — both nominally duplex-capable but the Pro has the better duplexer.

## Spurious emission concerns on simplex hotspots

A real concern with the cheaper simplex MMDVM HATs (Jumbospot-class, ~$25) is **out-of-band spurious emissions**. The radio chip used (typically an Si4463 or Ax5043) has marginal filtering, and harmonics or LO leakage can appear well outside the intended channel.

Some specific issues observed:

- **Harmonics on 70 cm.** A hotspot transmitting on 438 MHz can emit detectable energy at 876 MHz (2x), 1314 MHz (3x). The 3rd harmonic in particular falls in the GPS L-band (1227 MHz) and can interfere with consumer GPS receivers nearby.
- **LO leakage at non-harmonic spurs.** Some chips emit at frequencies that don't relate cleanly to the carrier — a "birdie" at 433 MHz when the carrier is at 438 MHz, for instance.
- **Phase noise creating wideband sidebands.** Cheap synthesizers produce TX phase noise that occupies more bandwidth than the modulation alone justifies.

The power levels are tiny (10 mW peak), so the *absolute* spurious levels are also tiny — usually below FCC Part 97 limits for amateur transmitters by wide margins. But:

- If your hotspot is sitting right next to a GPS-dependent device (a car nav, a weather station, your watch), interference can occur at very short range.
- If the hotspot is in a multi-radio shack, it can desense nearby receivers on bands far from the operating frequency.

Better-quality hotspots (genuine ZUMspot, OpenSpot, Pi-Star duplex HATs with proper filtering) have additional output filters that reduce harmonics to 70–80 dB below the carrier. Cheap Jumbospots may be 30–40 dB down only.

> **Advanced —** The FCC Part 97.307(d) spurious-emission limit for an amateur transmitter under 5 W is that any spurious emission must be at least 40 dB below the unmodulated carrier. A cheap MMDVM HAT transmitting at 10 mW (= +10 dBm carrier) with -40 dB spurious comes out to a spur power of -30 dBm, or 1 microwatt. That's well below the noise floor of most distant receivers, but enough to desense a co-located receiver on a different band. The fix is an external low-pass filter (cheap LPFs for 70 cm cost $10–$20) installed between the hotspot and its antenna. Most genuine ZUMspots have an LPF built in; clones often skip it.

Duplex hotspots have similar concerns, but the dual-radio designs typically include better filtering as a side effect — the receiver path needs a filter to keep TX from desensing RX, and that filter helps with general spurious cleanup too.

## A typical duplex hotspot setup

If you've decided duplex is worth it:

1. Pick a frequency pair with the standard 5 MHz offset. Example: hotspot TX 438.800 / RX 443.800 means your radio is programmed with the standard 70 cm "+5 MHz" offset, like any 70 cm repeater.
2. Configure both frequencies in the hotspot dashboard (Pi-Star: Configuration → MMDVM Display → Frequency RX/TX).
3. Set TX power 50–80 (Pi-Star units, ~5–10 mW).
4. Set your radio for duplex with +5 MHz offset, your hotspot's TX frequency as the listening freq.

You're now operating through a tiny personal repeater. Bonus: it works exactly like real repeaters in your codeplug programming — same memory format, same offsets, same CTCSS or color-code expectations. No special "this is a simplex hotspot, you need to disable offset" handling.

## See also

- [§24-07](24-07-hotspot-pistar.md) — Pi-Star supports both simplex and duplex HATs
- [§24-08](24-08-hotspot-openspot.md) — OpenSpot 4 Pro is duplex
- [§24-10](24-10-ber-explained.md) — How simplex/duplex affects BER tuning
- [§04-05](../04-repeaters-bandplans/04-05-linked-systems.md) — Real repeaters all use duplex; hotspots are just small versions
- [§22-08](../22-operating-practice/22-08-split-frequency.md) — Duplex hotspots use split, automatically
