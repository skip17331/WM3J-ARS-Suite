---
id: 07-01
title: Antenna Recommender
chapter: 07
section: 01
level: simple
status: draft
---

# Antenna Recommender

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The recommender is a **questionnaire-driven decision tool** that maps your living situation, lot, supports, and operating goals to a ranked list of antennas you can realistically build at your QTH. It's the answer to the most-asked question on every ham forum: "What antenna should I put up?"

The recommender lives in the J-Hub **Antenna Workshop** tab as an interactive wizard. This section documents *what it asks*, *why it asks*, and *how the scoring works* — so you can use the recommender confidently rather than treat it as a black box, and so the criteria can be reviewed and improved over time.

## What the recommender does (and doesn't)

**Does:**
- Ask 15–20 questions about your QTH, supports, restrictions, bands, modes, power, and goals.
- Score every antenna in §07-02 through §07-15 against the answers.
- Return a ranked top-5 list with **why** each antenna fits (and what you'd give up).
- Deep-link each suggestion to its calculator panel.

**Doesn't:**
- Replace your judgment about your specific situation. The recommender doesn't know your neighbor is hostile, your trees are dying, or your spouse drew a hard line at the rooftop.
- Account for every possible site detail. It's a starting point.
- Tell you "the best antenna" — there isn't one. Every antenna is a tradeoff between space, money, performance, and stealth.

## The questions

The questionnaire is grouped into five sections. Each question lists the answer choices and what the recommender does with the answer.

### Section A — Your dwelling

| Question | Choices | Why it matters |
|----------|---------|----------------|
| **A1. Building type** | Single-family detached / Single-family attached (row house, townhouse) / Multi-family low-rise (duplex, triplex, walk-up) / Multi-family high-rise (apartment 3+ stories) / Mobile home / RV / camper / No fixed residence | Sets the upper bound on what's physically possible — apartment dwellers can't put up a tower, mobile-home parks usually forbid permanent installations |
| **A2. Ownership** | Own / Rent / Condo with HOA / HOA single-family | Affects whether you can drill, mount permanent supports, or run conduit |
| **A3. HOA / antenna restrictions** | None / Some (no visible antennas) / Strict (FCC PRB-1 doesn't apply / nothing outside) / Unknown | Pushes the recommender toward stealth (attic, flagpole, hidden) or rules out outdoor entirely |
| **A4. Indoor / attic option** | Yes — usable attic / Yes — but attic is metal-clad / No / Don't know | If outdoor is restricted, attic dipoles / EFHWs become the best option (with the metal-clad caveat) |

### Section B — Your lot

| Question | Choices | Why it matters |
|----------|---------|----------------|
| **B1. Lot length × width** | (numeric, ft) | Hard physical bound on dipole spans, vertical radial fields, Yagi turning radius |
| **B2. Lot orientation** | N–S long axis / E–W long axis / Doesn't matter (square) / Irregular | A long lot N–S favors dipoles broadside to N–S (good for E–W DX); square lots are flexible |
| **B3. Tree count and max height** | 0 / 1 / 2 / 3+ trees, max height (ft) | Trees are free antenna supports; two trees 60 ft apart at 30+ ft enable 80m dipoles |
| **B4. Existing tower** | None / 30 ft (push-up) / 40-50 ft self-supporting / 50+ ft guyed / Crank-up | Towers enable Yagis and high-vertical installs |
| **B5. Tower potential** | Already approved / Could be approved / Forbidden / Not interested | If no tower is possible, Yagis come off the list |
| **B6. Roof access** | Yes — flat / Yes — pitched / No / Won't go up there | Roof verticals, J-poles for VHF/UHF need this |
| **B7. Buried-radial-friendly soil** | Lawn (good) / Driveway / Concrete patio / Rocks / Salt marsh / Don't know | Verticals need radials; salt marsh is gold, rocks are bad |

### Section C — Your goals

| Question | Choices | Why it matters |
|----------|---------|----------------|
| **C1. Bands wanted** | Multi-select: 160m, 80m, 40m, 30m, 20m, 17m, 15m, 12m, 10m, 6m, 2m, 70cm, higher | Drives what antenna geometries are even feasible; 160m on a 30 ft lot is essentially impossible |
| **C2. Single band or all-band priority?** | Single best band / Few bands well / All bands acceptably | A 40m EFHW gets 40/20/15/10 cleanly; a fan dipole gets specific bands well |
| **C3. Operating modes** | Multi-select: SSB / CW / FT8-FT4 / RTTY / digital / FM voice (repeaters) / digital voice (DMR/Fusion/D-STAR) / packet | Some antennas favor specific bands/modes; J-pole = FM repeaters |
| **C4. Power level** | QRP (≤10 W) / 100 W / 500 W / Legal limit (1500 W) | Trap construction, balun ratings, wire gauge all change |
| **C5. Operating goals** | Multi-select: Local rag-chew / Regional NVIS / DX hunting / Contesting / Emcomm / Portable (POTA / SOTA) / Field Day / Special events | DX wants gain + low takeoff angle; NVIS wants high takeoff; POTA wants portable |
| **C6. Geographic targets** | Local <100 mi / Regional <500 mi / Continental US / Worldwide DX / Specific entity | Affects band choice and antenna pattern preference |

### Section D — Constraints

| Question | Choices | Why it matters |
|----------|---------|----------------|
| **D1. Budget** | Under $50 / $50-200 / $200-500 / $500-1500 / Over $1500 / No firm limit | Wire dipoles are cheap; trapped EFHWs are mid; commercial Yagis are expensive |
| **D2. Time investment** | A few hours / A weekend / Multiple weekends / A long-term project | Commercial kits take an afternoon; homebrew Yagis take weekends |
| **D3. Skill level** | First antenna / Built one or two / Experienced builder / Antenna-engineering background | Calibrates what's "stretch" vs. "easy"; a Yagi is a third or fourth antenna, not a first |
| **D4. Climbing comfort** | I'll climb a tower / Pitched roof yes / Step ladder only / No heights | Affects what's installable; tower-mounted Yagis need climbers |
| **D5. Stealth required** | Visible OK / Subtle preferred / Must be invisible from neighbors / Must be invisible always | Pushes toward attic / flagpole / wire-thin / camouflaged options |

### Section E — Existing infrastructure

| Question | Choices | Why it matters |
|----------|---------|----------------|
| **E1. Existing antenna(s)** | List + bands they cover | Recommender suggests *complementary* antennas — if you already have a 20m vertical, it suggests something for 80m / 40m |
| **E2. Coax in place** | None / RG-58 / RG-8X / RG-213 / LMR-400 / Hardline / Window line / Don't know | Lossy short feedline can rule out high-VHF / UHF antennas |
| **E3. Tuner available** | None / Internal (rig only) / External 100 W / External 1500 W / Auto + remote | Tuner availability changes what antennas are viable |
| **E4. Grounding system** | None / Single rod / Multi-rod array with bonded copper / Counterpoise wire | Verticals especially benefit from a real RF ground |

## How the scoring works

Each antenna in §07-02 to §07-15 has a small **profile** that says, for each questionnaire dimension, the conditions under which it's viable / preferred / impossible. The recommender computes a score per antenna, ranks them, and explains the score.

### Per-antenna scoring profile (example: Inverted-V Dipole)

| Dimension | Inverted-V Dipole profile |
|-----------|----------------------------|
| Building | All types acceptable; row house and apartment require attic |
| Lot length | Min: 0.45 × λ; ideal: 0.55 × λ |
| Supports needed | 1 mast (apex) + 2 stake-out points |
| Tree-friendly | High — uses one tall support and two staked corners |
| Tower needed | No |
| Bands | One per dipole; multi-band needs traps or fan |
| Power | Up to legal limit with adequate balun |
| Budget | Under $50 (wire + balun + insulators) |
| Skill | First antenna friendly |
| Stealth | Moderate — visible but slim |
| Best at | Single-band, single-support, lots of compromise potential |
| Worst at | Multi-band without traps, spaces under 30 ft long |

### Scoring rules

For each antenna A and answer set R, the score is:

```
score(A, R) = Σᵢ wᵢ · fitᵢ(A, R)
```

Where:
- **wᵢ** is the weight of dimension i (lot size and bands wanted are heavily weighted; budget is moderately weighted; aesthetic preferences are lightly weighted).
- **fitᵢ(A, R)** is a 0–10 score for how well antenna A handles your answer to dimension i (0 = impossible, 5 = workable, 10 = ideal).

If any dimension scores 0 (e.g., "no outdoor antennas" + "Yagi-Uda"), the antenna is dropped entirely.

Antennas with score ≥ 0.7 × top-score appear in the result list, with explanation:

```
1. Inverted-V Dipole (40m)            score 8.4
   ✓ Two oaks on the lot are perfect supports (B3)
   ✓ Lot length 65 ft fits a 40m dipole (B1, C1)
   ✓ Wire-cheap; easy first build (D1, D3)
   ⚠ Single-band; consider fan or trapped variant for multi-band
   → Launch calculator

2. End-Fed Half-Wave 40-10m            score 7.9
   ✓ Single end-support sufficient — only need one tree (B3)
   ✓ Covers 40 / 20 / 15 / 10m harmonically (C1)
   ⚠ Requires 49:1 unun (small kit); first-build is moderate (D3)
   → Launch calculator

3. Multi-band Vertical                 score 7.5
   ⚠ Requires 8-16 buried radials (B7 — your soil is good)
   ✓ Low visual profile (D5)
   ⚠ Less favorable for high takeoff angle (C5 NVIS deprioritized)
   → Launch calculator
```

The "Why this fits" lines reference the question codes (A1, B3, C1, etc.) so the operator can trace each judgment back to their answers.

> ⚙️ **Advanced —** The scoring weights are tuneable. Defaults err toward "what works at most US suburban QTHs"; an HOA-restricted condo dweller and a 10-acre rural ham have wildly different priorities. The Antenna Workshop UI exposes a "weights" panel for advanced users to bias the recommender (e.g., set "stealth" weight to 1.0 if you must be invisible).

## Worked example — single-family suburban, 80×120 ft lot, two oaks, no tower yet

Inputs:

```
A1: Single-family detached
A2: Own
A3: HOA — light (no visible antennas above roofline)
A4: Attic available, no metal in roof
B1: 80 × 120 ft
B2: N-S long axis
B3: 2 oaks, 60 ft tall
B4: No tower
B5: Could be approved, $$$ to consider
B6: Roof access — pitched
B7: Lawn (good radial-friendly)
C1: 80, 40, 20, 15, 10
C2: Few bands well
C3: SSB + CW + FT8
C4: 100 W
C5: DX hunting + occasional rag-chew
C6: Worldwide DX
D1: $200-500 budget
D2: A weekend
D3: Built one or two
D4: Step ladder OK; pitched roof yes
D5: Subtle preferred (HOA-light)
E1: None yet
E2: 100 ft LMR-400 ready
E3: External tuner 100 W
E4: Single 8 ft rod
```

Recommender output (top 3):

```
1. End-Fed Half-Wave 80m (no traps), 134 ft @ 30 ft       score 8.6
   ✓ Single 60 ft oak support is enough (B3)
   ✓ Harmonic on 40 / 20 / 15 / 10m matches your bands (C1)
   ✓ Wire is invisible from street under roof line (D5)
   ✓ LMR-400 (E2) handles full 100 W comfortably
   ⚠ Requires 49:1 unun (~$80 kit) and tuner (✓ E3)
   ⚠ Bandwidth tight — tuner needed for FT8 vs SSB on same band
   → Launch calculator

2. Trapped Multi-band Dipole 80-40-20m, 70 ft @ 30 ft     score 8.1
   ✓ 80 ft span fits horizontally between oaks (B1)
   ✓ Resonant on 80/40/20 with no tuner — cleaner FT8 (C3)
   ⚠ Trap construction is a 1-2 weekend project (D2)
   ⚠ More visible (3 traps + apex hardware) (D5)
   → Launch calculator

3. Multi-band Vertical (Hustler 5-BTV equivalent)         score 7.4
   ✓ Low visual profile if mounted at the edge of yard (D5)
   ✓ Low takeoff angle good for DX (C5, C6)
   ⚠ Requires 16+ radials (B7 - lawn is workable)
   ⚠ Single feedline run; performance highly soil-dependent
   → Launch calculator
```

The recommender doesn't pick a single answer — it ranks options with their tradeoffs, and the operator picks based on what they value most.

## Walking-skeleton (UI implementation notes)

For the Phase 2 UI build, the wizard presents the questionnaire as a **5-step flow** (one section per step), with **"Skip" / "Don't know"** options on every question (since some operators won't have measured their lot, or won't know what coax type is in the wall). Skipped answers contribute neutrally (no positive or negative weight) to the score.

The result page renders the ranked list with scoring rationale, and each entry has a **▶ Open in calculator** button that opens that antenna's calc panel pre-filled with the recommender's suggested band(s).

Answers can be saved as a **profile** (locally in `~/.j-hub/antenna-profile.json`) and re-loaded — useful if you're evaluating multiple QTHs (current home vs. retirement home, primary vs. portable).

## See also

- §04 — Antennas (full theory for each type the recommender suggests)
- §07-02 through §07-15 — calculator cards for each antenna
- §09 — Power Budget & ERP (relevant once you've chosen and need to compute coverage)
- §10 — High-SWR troubleshooting (post-build, if results don't match prediction)
