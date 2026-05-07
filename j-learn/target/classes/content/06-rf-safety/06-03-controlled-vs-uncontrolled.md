---
id: 06-03
title: Controlled vs Uncontrolled
chapter: 06
section: 03
level: simple
status: draft
---

# Controlled vs Uncontrolled

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

The FCC's MPE limits come in **two tiers**: a higher (more permissive) limit for "controlled" environments, and a lower (stricter) limit — typically 5× tighter in the VHF range — for "uncontrolled" environments. Almost every amateur RF safety question reduces to: **for each location near my station, which limit applies?**

Get the classification wrong and you may design a "compliant" station that is actually exposing your neighbor's kids to 5× the legal limit. The good news: the rules for classification are clear and not very subjective.

## The two categories

**Controlled environment**: a place where the people present:
- Are aware of the potential for RF exposure, and
- Can take action to limit their exposure (move away, reduce power, etc.).

This includes the operator at the station and any visitors who have been informed of the RF and shown how to move out of the field. It does **not** include casually-present family members, bystanders, or neighbors.

**Uncontrolled environment**: anywhere the people present:
- Are not aware of the RF, **or**
- Cannot easily move to limit their exposure.

This includes essentially everyone except the operator and trained personnel: family, neighbors, the public, pets (since they cannot make informed decisions). Most spaces near most amateur installations are uncontrolled.

## The two MPE limits, side by side

| Frequency range | Power density limit (mW/cm²) — controlled | Power density limit (mW/cm²) — uncontrolled | Ratio |
|----------------|------------------------------------------|---------------------------------------------|-------|
| 0.3–3.0 MHz | 100 | 100 | 1× (no difference) |
| 3–30 MHz | 900 / f² | 180 / f² | 5× |
| 30–300 MHz | 1.0 | 0.2 | 5× |
| 300–1500 MHz | f / 300 | f / 1500 | 5× |
| 1500–100,000 MHz | 5.0 | 1.0 | 5× |

Below 3 MHz, the limits are the same; they assume that there is no biological need for tighter rules at very low frequencies. Above 3 MHz, **uncontrolled limits are 5× lower**, reflecting that the public should be afforded a larger margin of safety than informed/trained personnel.

Translated: at 14 MHz, the controlled limit is 4.6 mW/cm²; uncontrolled is 0.92 mW/cm². At 144 MHz: controlled 1.0; uncontrolled 0.2.

## Which limit applies where in your station

| Location | Category | Why |
|----------|----------|-----|
| Operator at the radio (you) | Controlled | You're aware of the RF and can stop transmitting |
| Operator's chair, with the rig running unattended (e.g., AFSK/FT8 sessions) | Uncontrolled | If you're not there, you can't react |
| Family members elsewhere in the house | Uncontrolled | They're not aware of TX timing |
| Family playroom / kid's bedroom | Uncontrolled | Particularly tight standard for children |
| Yard / patio | Uncontrolled | Anyone can be there |
| Public sidewalk past your fence | Uncontrolled | Definitionally |
| Neighbor's yard or house | Uncontrolled | They're not your operators |
| Tower base during climb (you, with hardhat and awareness) | Controlled | Trained personnel; transmit-lock procedures |
| Climbing buddy on the ground while you're on the tower | Controlled (if briefed) | Same |
| Roof access from neighbor's tree | Uncontrolled | Pretend a stranger could be there |

The general principle: **default to uncontrolled** for any location you don't have explicit control over, and you'll rarely be wrong.

## The "trained operator" question

The controlled-environment limit applies to:

- The licensee at the station.
- Family members **who have been formally trained** in RF awareness (knowing what TX means, how to leave the area, how to identify high-field zones).
- Other amateur radio operators.
- Personnel at multi-operator installations (contest stations, club stations) who have been briefed.

It does **not** apply to:

- Casual house guests.
- Family members who haven't been specifically trained.
- Pets (no possible training).
- Children, ever.

In practice, **most family members aren't formally "trained" in RF awareness**, even if they vaguely know "Dad's transmitting now, the lights flicker." The conservative interpretation: family rooms where untrained occupants spend significant time should be evaluated as uncontrolled.

If you train a family member explicitly — they understand the concept of MPE, know how to leave the field area, know the visual or audio cues that you're transmitting at high power — you can argue their living space is "controlled." Document the training. Be honest about whether they really understand it.

## Why this matters in practice

Almost every amateur station has at least one location where the limit changes from controlled to uncontrolled. That's where the math gets tight.

**Example case**: 100 W to a 6-dBi vertical at the corner of the yard. EIRP = 400 W. At 20 m (14 MHz):

- 30 ft from the antenna (operator chair, but only when at the rig): **controlled limit applies = 4.6 mW/cm². Power density = 0.0011 mW/cm². Compliant easily.**
- 10 ft from the antenna (where the patio chair sits): **uncontrolled = 0.92 mW/cm². Power density = 0.010 mW/cm². Compliant.**
- 5 ft from the antenna (next to where the family pet sleeps in summer): **uncontrolled = 0.92. Power density = 0.041. Compliant.**

In this case, no problem. But change to a 1500 W amplifier at 6 dBi gain (EIRP = 6000 W), and the patio at 10 ft is at 0.15 mW/cm² — still compliant on 20 m, but on 6 m where the uncontrolled limit drops to 0.2 mW/cm²:

Actually let's redo with 6 m: 1500 W into a 6 dBi vertical at 50 MHz, distance 10 ft = 3 m. Power density = 0.000064 × 6000 / 10² = 3.84 mW/cm². **Way over the 0.2 uncontrolled limit at 6 m**. Non-compliant.

## What "controlled" gets you

For yourself at the operating position, applying the controlled limit gives you 5× more power-density headroom than would apply to a bystander. Practical consequences:

- The operating position can be closer to the antenna than other rooms can.
- Tower work is feasible at moderate transmit power if the climber is "controlled" (you, with TX-lockout enforced when on the tower).
- Mobile installs can have the antenna closer to the driver because the driver is in a controlled environment (knows the rig's TX state).

But for bystander evaluations — your spouse's office, your kids' bedroom, your neighbor's deck — **uncontrolled is the only honest classification**, and the 5× tighter limit is what you must meet.

## Mitigation when you can't meet uncontrolled

If your installation can't meet the uncontrolled MPE limit at some bystander location, you have these options:

1. **Reduce power** in those bands or modes.
2. **Move the antenna** further from the bystander location, or higher.
3. **Beam direction control**: avoid pointing the antenna at the problem area during high-power TX.
4. **Operating-position control**: only transmit when the bystander area is unoccupied (impractical for casual operating, fine for contests).
5. **Restrict access**: fence off the area, post warning signs, document. Now it's at least *less* uncontrolled.
6. **Reclassify as controlled**: explicitly train the family member who occupies the location, document the training. Their occupied area becomes controlled.

Most amateur installations meet uncontrolled limits naturally. When they don't, mitigation is usually straightforward.

> ⚙️ **Advanced —** The 5× factor between controlled and uncontrolled limits comes from the safety-factor scheme in IEEE C95.1: occupational SAR limit 0.4 W/kg, public SAR limit 0.08 W/kg — a 5× ratio. The MPE limits in V/m or mW/cm² are derived to keep an "average adult body" below those SAR thresholds at each frequency, with frequency-dependent body-resonance corrections. The IEEE rationale for the 5× public-vs-occupational factor is that the public includes all body types and ages (more biological variation than a screened occupational population), and the public cannot adjust their exposure based on knowledge.

## Common mistakes

- **Treating the family room as controlled because "they know I'm a ham."** Knowing you have a hobby is not RF awareness training. Use uncontrolled.
- **Skipping the calculation for the neighbor's yard "because that's not my property."** The MPE rules don't care whose property; they care where humans might be exposed.
- **Pretending pets don't count.** They do — same as people, evaluated uncontrolled.
- **Computing only for the operating position.** The bystander locations are usually the binding constraint.
- **Forgetting that controlled means "can take action."** A controlled environment requires ability to *act* on the exposure information, which a sleeping person or someone in a different room cannot do.

## See also

- §06-01 — FCC rules
- §06-02 — MPE limits (the actual numbers per category)
- §06-04 — Duty cycle (time-averaging within the categories)
- §06-06 — Safe antenna placement (where category boundaries fall)
- §10 — RF Exposure Calculator
