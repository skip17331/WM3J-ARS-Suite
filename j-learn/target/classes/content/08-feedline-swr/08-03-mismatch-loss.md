---
id: 08-03
title: Mismatch Loss
chapter: 08
section: 03
level: mixed
status: draft
---

# Mismatch Loss

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

When a transmission line is terminated in something other than its characteristic impedance, two things happen at the load: **some of the forward wave reflects back** toward the source, and the **forward wave that reaches the load is reduced** (because not all of it transferred). Both effects mean less power reaches the load than the transmitter delivered into the line.

In a **lossless** line the reflected wave just bounces around and eventually delivers its power back to the load — net loss from mismatch alone is small. In a **real lossy** line, the reflected wave loses energy on every traversal of the line; the longer the line and the higher the matched-line loss, the more SWR-induced extra loss you see.

This section quantifies that effect: how much *extra* loss you get from impedance mismatch, beyond the matched-line loss covered in §08-01.

## Two parts of mismatch loss

It helps to distinguish:

1. **Reflection loss** — the immediate "first-pass" loss when the forward wave hits the mismatched load. A 3:1 SWR load reflects 25% of the power back; only 75% transfers on first encounter.
2. **Extra feedline loss** — power that bounces back through the lossy feedline takes additional loss on each round trip. Most of this is what we mean by "mismatch loss" in the practical sense.

The total power loss budget for a real system is:

**P_loss = P_in × (1 − transmission_efficiency)**

Where transmission_efficiency factors in both the matched-line loss and the SWR-induced extra loss.

## The mismatch-loss table

For a transmission line with matched-line loss L_matched (in dB) and antenna SWR S, the total loss L_total is:

**L_total = L_matched + ΔL(SWR, L_matched)**

Where ΔL is the extra loss from mismatch. Approximately:

| Matched-line loss | SWR 1.5:1 | SWR 2:1 | SWR 3:1 | SWR 5:1 | SWR 10:1 |
|-------------------|----------|---------|---------|---------|----------|
| 0.5 dB | 0.05 dB | 0.1 dB | 0.25 dB | 0.5 dB | 1.0 dB |
| 1.0 dB | 0.1 dB | 0.2 dB | 0.5 dB | 1.0 dB | 2.0 dB |
| 2.0 dB | 0.2 dB | 0.4 dB | 1.0 dB | 2.0 dB | 4.0 dB |
| 3.0 dB | 0.3 dB | 0.6 dB | 1.5 dB | 3.0 dB | 6.0 dB |
| 5.0 dB | 0.5 dB | 1.0 dB | 2.5 dB | 5.0 dB | 10 dB |

(These are approximate; the exact relationship is non-linear at higher SWR + higher loss combinations.)

Pattern: **mismatch loss scales with both SWR and matched-line loss**. A short low-loss line with high SWR has only a small extra penalty. A long high-loss line with high SWR has a big penalty.

## A motivating example

**Scenario A**: 50 ft of LMR-400 to a tuned dipole at 14 MHz.
- Matched-line loss: 0.45 × 50/100 = 0.23 dB.
- SWR at antenna: 1.3:1.
- Mismatch loss: ~0.02 dB.
- Total: 0.25 dB.

**Scenario B**: 100 ft of RG-58 to a non-resonant antenna at 14 MHz.
- Matched-line loss: 1.6 × 100/100 = 1.6 dB.
- SWR at antenna: 4:1.
- Mismatch loss: ~0.7 dB.
- Total: 2.3 dB.

**Scenario C**: 200 ft of RG-8X to a marginally tuned antenna at 144 MHz.
- Matched-line loss: 4.5 × 200/100 = 9.0 dB.
- SWR at antenna: 3:1.
- Mismatch loss: ~4.5 dB.
- Total: 13.5 dB.

Scenario C is *most* of the loss is mismatch-induced. Cutting that SWR in half (3:1 → 1.5:1) would save ~4 dB at the antenna. The 9 dB matched-line loss is unavoidable without changing cable.

## Why high SWR + lossy line is so bad

The reflected wave traveling back through a lossy cable loses energy each pass. Some of it then re-reflects off the source (if not perfectly matched) and travels forward again, losing more energy. The cumulative effect is geometric.

**Intuition**: a lossless line with 10:1 SWR has 67% reflected power, but 100% of the power eventually reaches the load (the reflections just bounce). A 6 dB lossy line with 10:1 SWR has 67% reflected once, then ~25% of that returning, then ~6%, then 1%, etc. — a small fraction of the original energy ever reaches the load. The bulk has been dissipated as heat in the lossy line.

> ⚙️ **Advanced —** The exact formula for power transmitted through a line of matched-line loss α (in nepers, not dB) with reflection coefficient |Γ| at the load is:

P_load / P_in = (1 − |Γ|²) × e^(−2α) / (1 − |Γ|² × e^(−4α))

Where 1 dB = 0.115 neper. For small α (low matched-line loss) the formula reduces to P_load / P_in ≈ (1 − |Γ|²) — almost independent of line loss. For larger α the denominator term suppresses the load power further. The exact loss table in this section comes from this formula evaluated numerically.

## "Hidden" SWR — when feedline transformation deceives

A peculiar consequence of high feedline loss: **the SWR you see at the rig is lower than the SWR at the antenna**. Reasoning:

- Antenna at 10:1 SWR. The reflected wave at the antenna is loud.
- The reflected wave travels back toward the rig through lossy feedline. It loses energy each foot.
- By the time it reaches the rig, the reflected wave is much smaller.
- The rig's SWR meter sees the (small) reflected wave compared to the (full) forward wave.
- It computes a "low" SWR.

A 100 ft RG-58 at 144 MHz (6.5 dB matched loss) with a *complete short circuit* at the antenna (theoretically infinite SWR) shows about 2.6:1 SWR at the rig.

So **a "low SWR at the rig" can be a sign of a dead antenna behind a lot of cable loss**. Always measure at the antenna feedpoint to verify.

## Loss budget calculation

For a complete loss accounting, work backward from the antenna:

1. **Antenna's actual delivered power** = transmitted ERP / antenna gain (linear) − antenna efficiency loss. (See §04-13 for ground losses; some are included in the antenna's effective gain.)
2. **Power reaching the antenna feedpoint** = P_load.
3. **Power leaving the rig** = P_load × 10^(L_total/10).
4. **Transmitter rated power** = power leaving the rig (if the rig is delivering rated output).

Or forward:

1. **Transmitter rated power** = P_in (e.g., 100 W).
2. **Total feedline loss** = L_total (matched + mismatch).
3. **Power at antenna feedpoint** = P_in × 10^(−L_total/10).
4. **Effective radiated power** = power at feedpoint × antenna gain in your direction.

The math doesn't lie; reconcile the two and you have a complete picture.

## Mitigation

Three ways to reduce mismatch loss:

### 1. Reduce SWR at the antenna

Tuning the antenna brings SWR down → less reflection → less mismatch loss. Always the right starting point. See §10 for high-SWR troubleshooting.

### 2. Reduce matched-line loss

Better feedline → less power lost on each traversal of reflected energy. Going from RG-58 to LMR-400 typically halves matched-line loss.

### 3. Use an antenna tuner — but understand what it does

An antenna tuner at the rig presents a 1:1 SWR to the rig. The rig delivers full power. **But the tuner does not change the SWR on the feedline**. The high SWR is still on the feedline; the tuner just absorbs the reflected wave and re-presents 50 Ω to the rig.

A tuner therefore **doesn't reduce mismatch loss**. It only fixes the rig-folded-back-power problem. If your feedline is long and lossy, mismatch loss is still happening.

The exception: a **tuner at the antenna feedpoint** does change SWR on the feedline. Remote-base tuners (like the LDG AT-100 at the antenna mast) actually solve mismatch loss. Tuners in the shack don't.

## Common mistakes

- **Believing "the rig SWR meter says 2:1, so my feedline is fine."** Could be 4:1 at the antenna with significant feedline masking it.
- **Tuning to 1:1 at the rig and assuming all is well.** The tuner is doing real work; matched-line loss + mismatch loss + tuner loss may all be present.
- **Ignoring mismatch loss because "it's only 0.5 dB."** Half a dB cumulative is real. Across multiple bands or operating contexts it adds up.
- **Forgetting that high feedline loss + high SWR is the worst case.** A long lossy run with bad SWR at the antenna is much worse than the matched-line loss alone suggests.

## See also

- §08-00 — Chapter overview
- §08-01 — Coax loss by frequency (the matched-line baseline)
- §08-02 — SWR & reflected power
- §08-04 — Power delivered vs lost
- §04-10 — Feedline effects (impedance transformation context)
- §10 — High-SWR troubleshooting
- §19 — Coax & connectors reference (cable specs, dB/100 ft, connector ratings)
- §19-02 — Loss tables (matched-line baseline values used in this calc)
