---
id: 23-12
title: HF Portable
chapter: 23
section: 12
level: mixed
status: draft
---

# HF Portable

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

## What it is

**HF portable** is operating HF away from your home station, *on foot* — backpack, picnic table, parking lot, summit, park, parking-lot picnic table, or somewhere off-grid entirely. Distinct from mobile (vehicle-based) in that there's no engine, no alternator noise, and usually no fixed antenna mount. You carry everything in.

Portable operating is the natural home of **SOTA** (Summits On The Air), **POTA** (Parks On The Air), **IOTA** (Islands On The Air), and **NPOTA**-like activations. It's also where QRP (low-power) operating thrives — 5–10 W into a wire in a tree often outperforms 100 W into a mobile whip in a parking lot.

## The four constraints

A portable station is bounded by what you can carry. The constraints, in priority order:

1. **Weight.** Hiking 5 miles to a summit with 30 lbs of radio gear is brutal. A modern portable kit can fit in 5–10 lbs.
2. **Power.** No wall outlet. You're on battery, and battery weight scales with capacity.
3. **Time to deploy.** Setting up takes 10–60 minutes. Pack-out takes similar. Activation time is the difference.
4. **Antenna height.** No tower. The antenna's high end goes up a tree, a fishing pole, or a guy line.

These constraints shape every choice — radio, antenna, battery, accessories.

## Battery sizing

Battery capacity is the limiting factor on operating time. A typical calculation:

- **5 W transmit:** 1 A current draw on most modern rigs.
- **10 W transmit:** 2 A.
- **100 W transmit:** 20 A.
- **Receive only:** 0.3–0.5 A typical.

For a 4-hour activation at 5 W TX, 50% TX duty cycle:
- TX hours: 2 × 1 A = 2 Ah
- RX hours: 2 × 0.5 A = 1 Ah
- **Total: ~3 Ah**

A LiFePO4 4S battery (12.8 V) sized at 6 Ah weighs about 1.5 lb. That's a 4-hour activation with comfortable margin.

For 100 W operation:
- TX hours: 2 × 20 A = 40 Ah
- RX hours: 2 × 0.5 A = 1 Ah
- **Total: ~41 Ah**

A 41 Ah battery weighs 15+ lb in LiFePO4. That's why QRP dominates portable operation — 5 W gets you on the air with 1/8 the battery weight of 100 W.

Battery chemistry choices:

| Type | Weight (Ah/lb) | Cycles | Notes |
|------|----------------|--------|-------|
| Lead-acid SLA | 1.5–2 Ah/lb | 200–500 | Cheap, heavy, gets damaged below 50% DoD |
| NiMH | 3 Ah/lb | 500–1000 | Discontinued for most uses; high self-discharge |
| Li-ion (18650 packs) | 4–5 Ah/lb | 500–1000 | Light but voltage drop affects rigs |
| LiFePO4 (4S) | 3–4 Ah/lb | 2000–4000 | Best for amateur radio; flat voltage curve |
| Lithium polymer (LiPo) | 5–6 Ah/lb | 300–500 | Light but needs careful charging; fire risk |

**LiFePO4 4S** (4 cells, ~12.8 V nominal, 14.6 V max) is the de facto standard. The voltage matches what HF rigs expect, the cycle life is enormous, and the chemistry is much safer than LiPo. Brands: Bioenno, Talentcell, Powerwerx.

## Wire-antenna deployment

The QRP/portable secret is that *wires beat whips, always*. A 33-ft wire in a tree is more efficient than any mobile whip ever made. The key designs:

### End-Fed Half-Wave (EFHW)

A half-wave wire (e.g. 66 ft for 40 m) fed at one end through a 49:1 or 64:1 unun. Resonant on the design band and on the harmonics (40 / 20 / 15 / 10 m for a 40 m EFHW).

**Pros:**
- One wire covers 4 bands.
- Easy to deploy — one end up in a tree, one end at the radio.
- Highly efficient (60–80%).
- Light (200–400 g for the wire + unun).

**Cons:**
- High voltage at the open end (don't touch during TX).
- The 49:1 unun is a single point of failure.
- Some bandwidth limits per band; an internal tuner helps.

### Sloper / Inverted-V dipole

A 1/2-wave dipole strung between two trees, or as an inverted-V from a single high point with the ends sloping down.

**Pros:**
- Highly efficient (75–90%).
- Predictable pattern.
- Bandwidth wider than EFHW.

**Cons:**
- Needs two suspension points (or one high + two low).
- Per-band; multi-band requires a fan dipole or tuner.
- More fiddly to deploy than EFHW.

### Vertical with elevated radials

A 1/4-wave wire vertical with 2–4 elevated radials. The radials must be at least 4 ft above ground for the antenna to work efficiently.

**Pros:**
- Omnidirectional in azimuth.
- Lower take-off angle than dipole (favors DX).
- Decent for portable POTA/SOTA where DX is the goal.

**Cons:**
- Radials must be deployed flat or angled.
- Pure vertical needs a fiberglass mast (8–10 m for 40 m).
- Bandwidth is limited per band.

### Linked dipole

A dipole with bands switched by *unclipping* sections of wire. Adjustable for 40/20/15/10.

**Pros:**
- Per-band optimization.
- Light (50–100 g total).

**Cons:**
- Manual band changes (clip / unclip in the field).
- Can be slow to redeploy.

## Antenna height

Wire antennas in the field benefit from height. Common methods:

- **Sapling / tree branch.** Toss a weighted line over a branch, pull up the antenna end. 20–40 ft typical.
- **Fiberglass telescoping pole.** 20–33 ft poles (Chameleon, Sotabeams) weigh 1–2 lb. Set up in 2 minutes; free-standing or guyed.
- **Painter's pole / extension pole.** 15–20 ft poles from a hardware store; cheap and light.
- **Drone / kite.** Used by experimental operators for very tall antennas. Legal in most US locations but expensive and weather-dependent.

Even a 20 ft horizontal antenna is enormously better than a 5 ft mobile whip. The rule of thumb: antenna height in wavelengths matters; 40 m at 20 ft is 0.15 wavelengths (poor); at 33 ft is 0.25 wavelengths (good for short-skip).

## QRP considerations

Portable operating is dominated by QRP — running 5–10 W instead of 100 W. The math works:

- A typical home station: 100 W to a dipole at 40 ft. Total ERP: ~150 W EIRP.
- A QRP portable station: 5 W to a dipole at 30 ft. Total ERP: ~7.5 W EIRP.

The difference is 13 dB. That feels like a lot, but in modern DXing with FT8, 13 dB is the difference between -10 dB SNR and -23 dB SNR — both reliably decodable. For CW, 13 dB is the difference between an S5 signal and an S0 signal — still copyable if the other end is listening.

Conclusion: QRP works. It works less well on SSB (where the operator's ear is the decoder) and more well on CW and digital (where the computer is the decoder).

The slogan from the QRP community: "When you don't succeed at low power, try lower power" — meaning, if your 5 W contact didn't work, the problem isn't power; it's antenna or propagation. Fix those first.

## Finding RFI-clean sites

A clean site (low RF noise floor) is a luxury for HF portable operators. Compared to a typical suburban home (S5–S7 noise floor on 40 m), a rural or wilderness site can drop to S0–S1. That's a 30+ dB improvement in receive — equivalent to adding a 30 dB amplifier on the antenna.

Where to find clean sites:

- **State or national parks** away from highways and power lines. POTA references are typically chosen for parking access; the actual operating spot is often a quiet picnic table or trailhead.
- **Hilltops in rural areas.** SOTA summits, but also informal hilltops that are accessible.
- **Beaches and shorelines.** Salt water is a near-perfect RF ground; vertical antennas at the shore work spectacularly.
- **Lakes (frozen, in winter).** Ice is a fine RF ground; a vertical on the ice with radials works well.
- **Off-grid campsites.** No power lines, no neighbors with switching power supplies.

Conversely, avoid:

- **Parks near highways.** Vehicle ignition noise is constant.
- **Picnic shelters with overhead lighting on photocells.** Modern LED lights are noise factories.
- **Sites near solar installations.** Solar inverters generate broadband hash.

## Integrating with SOTA / POTA

**SOTA** (Summits On The Air) and **POTA** (Parks On The Air) are structured portable-operating programs that have exploded in popularity since 2020:

- **POTA:** activate any of ~30,000 worldwide parks. Make 10 QSOs to qualify the activation. Spotters chase activators in real time via pota.app. Almost any park works; no hiking required for many.
- **SOTA:** activate designated summits (peaks above prominence thresholds). Higher difficulty; usually requires hiking. Points awarded based on summit altitude. Worldwide ranking system; the SOTA Honor Roll is the highest-prestige portable award.
- **IOTA** (Islands On The Air): activate any of ~1,200 island groups worldwide. Often requires boats or expensive trips. Less common than POTA/SOTA but the most "DXpedition-like" of the casual programs.

A typical POTA activation:

1. Drive to the park; park at a designated area.
2. Set up wire antenna in 10–20 minutes.
3. Spot yourself on pota.app: callsign + park reference + frequency + mode.
4. Call CQ POTA: "CQ POTA WM3J at K-4567 Susquehannock State Forest."
5. Work 10+ callers (typically 30–60 minutes; pile-ups are small to medium).
6. Pack up; submit log.

The chase community is enormous — thousands of "hunters" are watching pota.app for new spots and calling immediately. A clean activation can produce 50–100 QSOs in an afternoon.

> ⚙️ **Advanced —** Many POTA/SOTA operators run **FT8** rather than SSB or CW because the software handles the rate and the exchange is automated. With 5 W and a wire, FT8 routinely gets POTA hunters from 1,000+ miles away within seconds of self-spotting. The downside: FT8 is less "operator-vs-operator" satisfying than CW or SSB, but the numbers don't lie — FT8 activations finish faster.

## A typical portable kit (1-day operation)

A lean, fast-deploy kit:

| Item | Weight |
|------|--------|
| Elecraft KX2 or Yaesu FT-891 (transceiver) | 1.5 / 4 lb |
| LiFePO4 4S 6 Ah battery | 1.5 lb |
| EFHW 40/20/15/10 wire + 49:1 unun | 0.5 lb |
| 25 ft of paracord + arborist throw weight | 0.3 lb |
| 20 ft RG-174 coax with BNC connectors | 0.4 lb |
| Logging notebook + pencil | 0.2 lb |
| iambic paddle (for CW) | 0.3 lb |
| Microphone (for SSB) | 0.2 lb |
| Tablet/phone for FT8 (if running digital) | 0.5 lb |
| First aid, water, snacks | 1–2 lb |
| **Total** | **6–11 lb** |

Pack in a 25–35 L day pack; carry to your operating spot; deploy in 15 minutes; operate for 2–4 hours.

## See also

- [§23-11 — HF Mobile](23-11-hf-mobile.md)
- [§22-05 — Pile-up Etiquette](../22-operating-practice/22-05-pile-up-etiquette.md)
- [§11 — Power Budget](../11-power-budget-erp/11-00-overview.md)
- [§06 — Antennas](../06-antennas/06-00-overview.md)
- [§30 — Operating Specialties](../30-operating-specialties/30-00-overview.md) (SOTA, POTA, IOTA depth)
