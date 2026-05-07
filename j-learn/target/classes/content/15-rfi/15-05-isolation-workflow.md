---
id: 15-05
title: Isolation Workflow
chapter: 15
section: 05
level: simple
status: draft
---

# Isolation Workflow

> **Advanced callout convention:** sections or paragraphs intended for Extra-class / engineering depth are marked with a blockquote starting `> ⚙️ **Advanced —**`. Simple-mode renderers can hide these; advanced-mode renderers show them inline.

A workflow for systematically isolating an RFI source. The point is to **never guess** — every step either rules out a class of source or narrows down to a smaller set. Done correctly, a stubborn RFI problem can be tracked from "I have noise on 80 m" to "this specific power adapter" in 15–60 minutes.

## Pre-work

Before you start: log the noise's character.

- What does it sound like? (See §15-03.)
- What S-meter reading does it produce on a quiet frequency on each band?
- Time of day? Weather? Recent house events (someone turned on the heater, started laundry, etc.)?

This baseline is what you compare against during the workflow.

## Step 1 — Inside or outside?

The single most useful test. Ten minutes; saves hours.

1. Set the radio to a band where you hear the noise clearly (80 m or 40 m usually works).
2. Note the S-meter reading.
3. **Turn off all the breakers in your house except the one feeding the radio.**
4. If you have a battery option for the radio, use it — fully isolate from house power.
5. Note the new reading.

| Result | What it means | Next step |
|--------|---------------|-----------|
| Noise drops to S0–S2 | Source is inside your house | Step 2 (isolate by breaker) |
| Noise unchanged | Source is outside your house | Step 4 (direction-find) |
| Noise drops some, but not all | Mixed sources | Do both; fix the inside one first |

This single test answers the most important question.

## Step 2 — Which breaker?

If the source is inside:

1. **Turn breakers back on, one at a time.**
2. **Wait 30 seconds** between breakers (let things stabilize).
3. **Note any breaker that causes a noticeable noise jump.**

You may identify one breaker as the dominant offender, or you may find several contributing.

For each offender, **proceed to step 3** with that breaker only on.

## Step 3 — Which device on that circuit?

Now you've narrowed to one circuit. List the devices on it (lights, outlets, fixed appliances). Then turn off / unplug things one at a time:

1. Pick the first device. Unplug it (or turn it off via switch).
2. Note the change.
3. Restore that device. Unplug the next.
4. Repeat for every device on the circuit.

Often one device is much louder than the others. That's your culprit.

If no single device is dominant — they're all weak contributors — you may need to take a "whole-circuit" filter approach (mains line filter, Circuit-level chokes) instead of per-device fixes.

## Step 4 — Outside source: direction-finding

If your house tested clean, the source is somewhere else. Two methods:

### Direction by signal strength

Walk around your property with a portable receiver tuned to the noise. Note where the signal is strongest. Most likely directions:

- Toward your antenna — the source might be on a path that's literally on your antenna's beam.
- Toward a power line — utility issue (chapter 17).
- Toward a neighbor's house — their problem device.

### Direction by null

Point a directional antenna (small loop antenna, fox-hunt antenna) at the source and rotate it. The deepest signal null indicates the source's direction (perpendicular to the loop).

Better than peak-finding because nulls are much sharper than peaks.

A small loop, even on a portable AM radio, gives surprisingly good directional information. §15-07 covers AM-radio sniffing in detail.

## Step 5 — At the source

Once you've narrowed to a single device or location:

### If it's your device

Apply the standard fixes:
- Unplug. Replace with a quieter equivalent.
- Add ferrite to power, signal, and audio leads.
- Check internal grounding.

### If it's a neighbor's device

This is delicate. Suggested approach:

1. **Don't accuse.** Frame it as "I'm getting some interference and I'm trying to figure out where it's coming from. Could I bring my radio over and we could see if it's something on your property?"
2. **Bring a portable AM or shortwave radio.** Listen with the neighbor present. The same buzz they may have heard on their own audio gear is now audibly correlated to a device they own.
3. **If they're cooperative**, offer to provide ferrites or a different power adapter. Many neighbors are happy to swap a $5 charger if it makes their TV stop buzzing too.
4. **If uncooperative**, you have limited options. The FCC won't intervene quickly. Consider better antennas, additional band-pass filtering at your station, and operating on bands less affected.

### If it's the utility

A power-line problem is the utility's problem, but they only act on it if you formally report and follow up. See §17-07 for the documentation procedure.

## Step 6 — Verify the fix

After any fix:

1. Re-do the original noise measurement.
2. Compare to baseline.
3. If improvement is < 6 dB, the fix wasn't sufficient. Try other sources or stronger filtering.
4. If significant improvement, log the fix and move on to other sources if multiple were identified.

## A worked example

**Symptoms:** S7 broadband hash on 80 m, not present a year ago.

1. **Test inside vs outside:** turn off all breakers except radio. Noise drops to S2. → Internal source.
2. **Find breaker:** turn breakers back on. The "garage" breaker causes a S2 → S6 jump.
3. **Find device:** in the garage, turn off / unplug each device. The new EV charger drops S6 → S2.
4. **Fix:** add 5 turns of the EV's J1772 cable through an FT-240-31 toroid. Noise drops to S3 with charger plugged in.
5. **Verify:** measurement matches the calculation; problem solved at 80 m. Spot-check other bands; confirm no new problems introduced.

Total time: 45 minutes. Cost: $8 in ferrite. The same problem could have caused months of frustration without a workflow.

## Tools that help

- **Strong flashlight** for breaker box work in the dark.
- **Portable AM radio** for direction-finding.
- **A small portable HF receiver** if your station radio is hard to move (e.g., a Tecsun PL-380, $40).
- **Notebook and pencil.** Don't trust your memory between steps.
- **Ferrites and snap-ons** to do test fixes on the spot.

## When the workflow fails

Sometimes the source is genuinely hard to pin down:

- **Multiple weak sources** that combine to produce an audible noise floor. No single fix moves the needle. Solution: improve the antenna's noise-rejection (a magnetic loop antenna, a Beverage receive antenna, an active loop), and accept that the noise floor isn't going to S2.
- **Source on a separate AC service** that you can't switch off. Coordinate with the utility or neighbor.
- **Source that varies with temperature, humidity, or time of day** in ways you can't reproduce on demand. Wait for the noise to recur and then start the workflow.

## See also

- §15-03 — sound-based identification (do this first)
- §15-06 — step-by-step elimination (the practical detail)
- §15-07 — AM radio sniffer
- §17-07 — utility documentation for power-line cases
