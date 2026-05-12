---
id: 23-11
title: HF Mobile
chapter: 23
section: 11
level: mixed
status: draft
---

# HF Mobile

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

**HF mobile** is operating HF (1.8–30 MHz) from a vehicle — usually a car or truck, sometimes an RV. The challenges are physical (the car is a moving, electrically noisy, antenna-hostile environment) and operational (you're driving, so multitasking is constrained).

Done well, HF mobile is genuinely fun and can be remarkably effective. Many operators have worked DXCC entirely from their car commute. Done poorly, it's frustrating — bad antennas, alternator whine, RFI everywhere, and signals nobody can hear.

This section covers the setup choices, the common mistakes, and the operating-while-driving practice.

## The antenna problem

A full-size HF antenna is enormous — a 40 m half-wave dipole is 66 feet end-to-end; a 1/4 wave vertical for 80 m is 65 feet tall. A car is roughly 15 feet long. You cannot fit a full-size HF antenna on a car. Every HF mobile antenna is therefore *shortened*, with the tradeoffs that implies:

- **Lower efficiency** (5–30% typical, vs 80–95% for a full-size antenna).
- **Narrower bandwidth** (often <50 kHz at 2:1 SWR).
- **Higher Q** (sharp tuning peak; you must retune often).
- **More near-field coupling to the car body**, making the car itself part of the radiating system.

The "rate" you can achieve on HF mobile is typically 1/3 to 1/2 of the same operator on a home station. You're not going to win contests from the car; you're going to make some contacts and enjoy the commute.

## Antenna choices

### Screwdriver antennas

A **screwdriver antenna** is a single physical whip with a motorized tuning section — a coil moves up or down inside a fiberglass housing, changing the inductance and tuning the resonant frequency. Examples: Tarheel, Scorpion, High Sierra, Hustler 6-BTV variant.

**Pros:**
- Tunes continuously from 80 m through 6 m or 10 m.
- Higher efficiency than fixed-coil antennas (15–30% on 40 m).
- Single antenna covers all HF bands.
- Quick band changes — push a button, the motor retunes.

**Cons:**
- Expensive ($300–800+).
- Physically large (5–10 feet tall plus the base).
- Motor and gearing fail eventually; weather sealing matters.
- Requires a controller in the cab and a control cable.

**Best for:** dedicated HF mobile operators who want a permanent, capable installation.

### Hamstick / Hustler / Texas Bug Catcher (fixed-coil resonators)

A **fixed-coil resonator** is a whip with a fixed loading coil for a single band. To change bands, you swap the resonator (or rotate a turret).

**Pros:**
- Inexpensive ($30–80 per band).
- Simple, reliable, no moving parts.
- Lightweight.

**Cons:**
- Lower efficiency than screwdriver (5–15% on 40 m).
- Band changes require physically swapping the resonator (stop the car, walk around back, screw a new one in).
- Need separate resonator per band.

**Best for:** budget-conscious operators, single-band specialists, or those who'll mostly operate one band on a given trip.

### Mag-mount whip

A **magnetic-base mount** holds an antenna to a flat steel surface via strong magnets. Usually used with shortened whips on 10/20/40.

**Pros:**
- No drilling.
- Movable between vehicles.
- Cheap.

**Cons:**
- Very low efficiency for HF (the mag-mount couples to the body via capacitance, not a galvanic ground).
- Will scratch paint.
- Will blow off at highway speed if the antenna is too tall.
- Often broken at HF on cars with composite roofs (more common now).

**Best for:** temporary setups, renters, or "test it before drilling holes" experiments.

### Mono-band wire on a fishing pole or trailer hitch

For stationary operation (parked at a rest stop, in a parking lot), running a wire from the vehicle to a tree or a fiberglass mast is far better than any mobile whip. The wire is full-size and high-efficiency. Some operators carry a 30 ft telescoping pole and a wire for parked operating.

## Grounding — the big issue

Mobile antennas need a counterpoise. For a vertical, that's the car body — provided the body is well-bonded together. Most cars are not, by amateur radio standards. You need to add bonding straps:

- **Engine block to chassis.**
- **Exhaust system to chassis** (especially the muffler and tailpipe).
- **Hood to chassis** (the hood is hinged but often isolated; a 6" copper braid strap from hood corner to fender bonds it).
- **Trunk lid to body** (same problem, same solution).
- **Battery negative to chassis** (usually already there, but verify).
- **Antenna mount base to chassis** (the mount itself needs RF-quality ground; not just a sheet-metal screw).

A typical kit: 6 to 12 lengths of flat copper braid (1/2" wide), each 6–12 inches long, with crimped lugs at each end. Bolt one end to each surface and the other to the chassis at a clean (paint-stripped) attachment point. Resistance from antenna mount to battery negative should be <0.05 ohm.

Why does this matter? An ungrounded antenna couples to the body capacitively only. RF currents go everywhere — through the radio, through the audio system, through the airbag computer. SWR is unstable, the radio interferes with the car's electronics, and the antenna radiates poorly.

Properly grounded, the body becomes a real RF counterpoise, the SWR is stable, and the antenna radiates efficiently.

> ⚙️ **Advanced —** Modern cars (post-2010 especially) have **CAN-bus** networks and increasing amounts of computer control. RF currents from a poorly-grounded antenna can disrupt CAN messages, causing warning lights, transmission shifts, even engine stalls. The aluminum and composite body panels on modern cars also reduce the natural counterpoise. The cure is more aggressive bonding *and* ferrite chokes on every RF-carrying lead near sensitive electronics. The Alpha Delta DX-A or Bencher YA-1 RF chokes are common; some operators wrap the radio's DC leads through ferrites at the radio end.

## Alternator noise

The vehicle's alternator generates broadband electrical noise that radiates and conducts into the radio. Symptoms: a whine that changes pitch with engine RPM, a buzz at higher harmonics, S9+ noise floor on certain bands.

Mitigations, in order of effectiveness:

1. **Route the radio's DC power directly to the battery**, not through the car's wiring. Use heavy gauge (10 AWG or thicker for 100 W rigs) with an inline fuse close to the battery.
2. **Add ferrite chokes on the DC leads** at the battery end, the radio end, and along the run.
3. **Add a noise filter** (e.g. Astron line filter, or homebrew capacitors + chokes) on the DC line.
4. **Check the alternator brushes**. If the noise is severe and recent, the brushes may be worn. Replacement is $50–150 at a mechanic.
5. **Bond the alternator case** to the chassis. Some vehicles do this poorly; a heavy strap from the alternator mounting bolt to the engine block helps.

Some noise will remain. The S-meter floor in a moving car is typically S3–S5, where a home station might be S0–S2. Expect it.

## RF noise from other car systems

Beyond the alternator, modern cars are noise factories:

- **Ignition system** — broadband noise from spark plugs.
- **Fuel injectors** — short pulses, often 20–50 ms apart.
- **CAN-bus** — periodic noise at multiples of 50/125/500 kHz.
- **LED headlights / running lights** — switching power supplies generating wideband hash.
- **Tire pressure monitors (TPMS)** — 315 / 433 MHz transmitters (high band but harmonics can land in HF).
- **Inverter for charging laptop/phone** — switching-mode supplies create wideband hash.

The cure is one ferrite at a time. Trace each noise to its source by switching things off one at a time; add ferrites or move cables; iterate.

## Common HF mobile mistakes

- **No grounding straps.** SWR all over the place, RFI into the car's electronics, poor radiation. Add the straps before troubleshooting anything else.
- **Antenna too short or too lossy.** A 4-ft Hamstick on 80 m is 1% efficient. You won't be heard. Use a screwdriver or expect to be QRP-grade weak.
- **Antenna mount on a bad spot.** Trunk-lid mounts are convenient but the lid is poorly bonded. Front-bumper mounts give the antenna a clear field but no counterpoise on the front of the car. Best: behind the trunk, on the rear bumper bracket or a hitch mount, with strong bonding.
- **Running 100 W on a poor antenna.** You'll dump 80–95% of that into heat in the loading coil. The coil overheats; the SWR shifts as the coil warms; eventually the coil melts. Run lower power (25–50 W) on inefficient antennas.
- **Forgetting to retune after band change.** Screwdriver antennas need retune for each band; some operators set the rig to a band and forget the antenna's still on 40. SWR goes to 5:1; transmitter folds back.
- **Operating while the car is in motion in adverse conditions.** Heavy rain, snow, ice, or fog requires both hands and full attention. Don't operate; pull over.

## Operating in motion vs parked

### In motion

The mobile-while-driving question: legal in most jurisdictions, but requires care.

**Practical guidelines:**
- **No microphone in hand.** Use a boom mic on a headset or a steering-wheel-mount mic.
- **No log-keeping in motion.** Use a voice recorder ("WM3J working K1ABC, FN42, 14:32 UTC") and transcribe at the next stop.
- **No band-changing in motion** if it requires complex menu navigation. Pre-program frequencies and band-change buttons.
- **Pull over for pile-ups.** Active calling, tuning, and pile-up management require full attention. Find a parking lot.
- **Keep the rig power moderate.** 25–50 W is plenty; running 100 W in a car with bad bonding can cause RFI into the car's own electronics.

Some operators run **HF mobile while driving** for casual QSOs — local nets, brief contacts with people they know, FT8 (the software handles everything). Others do **HF parked** only — they drive to a quiet spot (a hilltop, a parking lot), set up the antenna, operate for 30 minutes to several hours, and pack up.

### Parked

Parked is the high-quality way to do HF mobile. With the engine off (no alternator noise), a properly-bonded vehicle, and a screwdriver antenna at a quiet location:

- **Noise floor drops** by 6–20 dB compared to in-motion.
- **You can run full power** (100 W or more) without worrying about driving distractions.
- **You can chase pile-ups effectively** — your hands are free, your log is open.
- **You can deploy a wire antenna** between the car and a nearby tree for a huge efficiency improvement.

POTA (Parks On The Air) and SOTA (Summits On The Air) operators are essentially "HF mobile, parked, optimized." See [§23-12](23-12-hf-portable.md) for the portable-station depth.

## Radio choice

Modern HF mobile rigs:

- **Yaesu FT-891** — popular, 100 W, compact, good DSP.
- **Icom IC-7100** — touchscreen, all-mode/all-band including VHF/UHF, separable controls/display.
- **Yaesu FT-857D / FT-857** — discontinued but still common, classic HF mobile rig.
- **Icom IC-7300** — bigger than dedicated mobile rigs but possible with a sturdy mount.
- **Elecraft KX3 + KXPA100** — high-end portable that doubles as mobile.

The display/control head should be separable from the main body, so you can mount the chunky transmitter in the trunk and the small controls within reach of the driver. Yaesu's FT-857 and Icom's IC-7100 are the classic separable-head designs.

## See also

- [§22-05 — Pile-up Etiquette](../22-operating-practice/22-05-pile-up-etiquette.md)
- [§23-12 — HF Portable](23-12-hf-portable.md)
- [§14 — RFI](../14-rfi/14-00-overview.md)
- [§17-08 — ERP / EIRP](../17-formulas/17-08-erp-eirp.md)
- [§10 — Feedline & SWR](../10-feedline-swr/10-00-overview.md)
