---
id: 22-06
title: Power Minimum and Polite Operating
chapter: 22
section: 06
level: simple
status: draft
---

# Power Minimum and Polite Operating

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

§97.313(a) is one of the shortest, clearest rules in amateur radio:

> An amateur station must use the minimum transmitter power necessary to carry out the desired communications.

It is law. It is also etiquette. It is also smart engineering. This section unpacks why and how.

## What "minimum power" means

The rule doesn't say "low power" or "QRP" — it says **minimum necessary for the communication**. That's a moving target:

- **Working a local repeater 5 miles away**: 5 W is plenty.
- **Working a contest exchange to a station 1500 km away**: 100 W might be needed.
- **Calling a marginal DX station through a noisy band**: 1 kW may be necessary.
- **Working FT8 with a station at -25 dB SNR**: 50 W often beats 500 W (because the digital mode has fixed efficiency thresholds).

The rule recognizes that "minimum" is contextual. What it forbids is **gratuitous power** — running 1500 W into a contact that 100 W would close.

## Why minimum power matters

Three reasons:

### 1. Less interference

A 1500 W signal radiates to neighboring stations in adjacent rooms (intermodulation, second-order distortion in their receivers, reflections off your tower). At 100 W those problems don't exist.

The math: doubling power = +3 dB. Going from 100 W to 1500 W is +12 dB — meaningful at the receiver. A 100 W transmission produces detectable QRM 200 km away; a 1500 W transmission produces detectable QRM 800 km away.

### 2. Less RF exposure

§97.13(c) requires station evaluation for RF safety. Higher power means:

- More energy radiated near humans (you, family, neighbors).
- Larger MPE compliance distances.
- More potential for human-body heating effects.

§17-14 (RF Exposure) covers the calculations. The simple version: 100 W from a tower antenna is well below MPE limits at typical distances; 1500 W can require careful placement and warning labels.

### 3. Less fatigue on equipment

- **PA tubes / transistors** wear out faster at higher dissipation.
- **Coax** and connectors carry more heat at higher SWR + higher power.
- **Power supply** runs hotter, fans louder, reliability lower.
- **Heat in the operating room** during long sessions.

A 100 W rig running 8-hour Field Day shifts is a happy rig; a 1500 W rig in the same conditions is at 80% duty cycle of its limit and getting hot.

## In practice

### Start low, increase if needed

When calling a new contact:

1. **Start at moderate power** (50-100 W on HF SSB; 100-200 W on CW).
2. **If they don't hear you**, ask the receiving station to confirm signal strength.
3. **Increase power 3-6 dB at a time** if the report is weak (3 → 25, 25 → 100, 100 → 500 W).
4. **Stop once readability is comfortable** (R5 voice, 599 CW, decoded reliably digital).

Going straight to 1500 W is overkill for >90% of contacts.

### Calibrate to your antenna

A 100 W signal into a high-gain Yagi delivers more ERP than a 1500 W signal into a vertical with poor ground. Power isn't the only knob — antenna improvement is often more effective.

§11-04 (ERP Output) and §17-08 (ERP) cover how antenna gain interacts with power.

### Reduce when conditions improve

If you started a QSO at 200 W and propagation has improved, drop to 100 or 50 W. Many operators forget to drop power as conditions get better; they end up running too much for the link.

### QRP isn't "minimum power" automatically

QRP (5 W or less) is a sport, not a regulatory requirement. The rule is "minimum **necessary**." If you can't reach a station at 5 W and you need the contact, increase power. Running 5 W when 100 W is needed is operating at lower-than-necessary power, which is fine for QRP-as-sport but isn't required.

## Power and DX

DX work has a specific dynamic:

- **The DX station may be running 100-1500 W** depending on their station. They typically can't easily drop power.
- **Your power up to ~500 W** is reasonable for closing a marginal DX contact.
- **Above 500 W**, the marginal benefit drops sharply. You're not going to "hear" the DX station better at 1500 W vs 500 W; you're trying to be heard, and signal strength at the DX is what matters (their RX, not yours).
- **For pile-ups** the DX often runs 500-1500 W to be heard. You don't have to match — they're hearing you on their listening side, and 100-500 W from you is usually enough.

## Power and contest

Contest operating has different conventions:

- **Top-tier contesters** typically run 1500 W to maximize contacts per hour. The contests reward many contacts, and small signal-strength advantages translate to more contacts in the rate.
- **Mid-tier contesters** run 100-500 W, prioritize accuracy and rate over absolute peak power.
- **Casual contesters** run 100 W or less; the contest is fun, the score doesn't matter.

The rule still applies — contest operators should use minimum *necessary* power for the QSOs they're making. Many contest operators run 1500 W because it makes the contacts, but if 500 W also makes the contacts, 500 W is "minimum necessary."

## Power and emcomm

Emergency operations often use higher power to ensure connectivity:

- **HF traffic during a hurricane**: 100-500 W is typical. You need a strong, copyable signal even with damaged antennas.
- **VHF/UHF tactical**: 5-50 W is plenty. Repeater coverage doesn't need power; it needs antenna geometry.
- **HF NVIS**: 100-500 W to compensate for the specific antenna loss patterns.

The rule applies in emcomm but is interpreted contextually. Reliable communication is the goal; power is a means to that goal.

## Other polite-operating considerations

Power minimum is one of several "polite operating" principles. Others:

### Listen before transmitting

Covered in §22-01. Universally applicable — every band, every mode.

### Don't transmit on top of others

Frequencies are shared. Tuning into a frequency someone else is using is rude unless you're absolutely certain they've left.

### Brief transmissions

Long-winded transmissions block others. Make your point and pass back.

### Acknowledge requests

If an operator asks "WM3J, can you give me a quick break?" — acknowledge them. "Sure, go ahead." Don't ignore breaks.

### Respond to "say again" requests

If your transmission is unintelligible, repeat. Don't just say "WM3J" again louder.

### Don't QRM on purpose

Intentional interference is illegal (§97.101) and gets you reported. If you suspect another operator is QRMing you, document it (date, time, frequency, mode) and contact ARRL or the FCC.

### Be courteous in tense situations

Bands get crowded; tempers fray. Polite resolution beats public flame wars. "Please QSY 5 down" is better than yelling.

## Common power-minimum mistakes

- **Running max power "to be heard."** If the band is open, 100 W is heard. 1500 W is heard better, but the marginal benefit is small and the cost (interference, RF exposure, gear stress) is real.
- **Not adjusting for conditions.** Running 200 W during a weak band opening, not dropping to 100 W when the opening peaks. Stay attentive.
- **Treating "QRP" as a brand identity.** QRP-only operators sometimes refuse to use higher power even when needed. Use what you need; the QRP credit is for the QRP attempts, not for limiting all your operating.
- **Confusing "more power" with "better antenna."** A small antenna at 1500 W often performs worse than a good antenna at 100 W. Improve the antenna first.
- **Misreading rules.** §97.313 doesn't ban high power; it requires *minimum necessary*. If you need 1500 W, you can use 1500 W. The judgment call is whether you actually need it.

> ⚙️ **Advanced —** §97.313 has a long history. The original 1934 amateur regulations specified maximum power (2 kW input); the 1968 amendments started requiring "the minimum power necessary" along with a 2 kW PEP output limit. The 1990 reform brought the 1500 W PEP output limit and the explicit minimum-power language. The rule is enforced through the "self-policing" model — the FCC rarely cites for excessive power, but the community's social pressure (DX cluster comments, social media) is meaningful. The "Effective Radiated Power (ERP)" calculations needed for MPE compliance (§97.13(c) + §97.0) are why high-power operators are increasingly expected to know §17-08 and §17-14.

## See also

- §22-04 — Calling CQ (start at moderate power)
- §17-08 — ERP and §17-14 RF Exposure (the math)
- §08 — RF Safety
- §11-04 — ERP Output
- §15 — Noise Sources (your high-power signal is somebody else's noise)
